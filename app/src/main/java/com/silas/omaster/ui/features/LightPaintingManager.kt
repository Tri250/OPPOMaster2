package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.hardware.camera2.CaptureRequest
import kotlinx.coroutines.cancel
import com.silas.omaster.renderer.BitmapPool
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.Executors

/**
 * 光绘场景模式
 *
 * - TRAFFIC：车流光轨，使用“最大值保留”混合，让车灯轨迹持续叠加、越来越亮。
 * - WATER：水流/瀑布长曝光，使用“加权平均”平滑混合，营造丝绸般的流动感。
 * - FREESTYLE：自由创作，使用中等强度加权混合，兼顾光轨保留与环境自然过渡。
 */
enum class LightPaintingSceneMode {
    TRAFFIC,
    WATER,
    FREESTYLE
}

/**
 * 光绘手动曝光参数
 *
 * @param iso ISO 感光度，null 表示由相机自动决定。
 * @param shutterMs 快门时间，单位毫秒，null 表示由相机自动决定。
 * @param exposureCompensation 曝光补偿索引（例如 CameraX 的 EV 索引），0 为不补偿。
 */
data class LightPaintingExposureParams(
    val iso: Int? = null,
    val shutterMs: Long? = null,
    val exposureCompensation: Int = 0
)

/**
 * 光绘模式核心管理器
 *
 * 职责：
 * 1. 在独立后台线程中完成像素级光轨累积，不阻塞相机回调线程。
 * 2. 维护可复用的 Bitmap 缓冲与对象池，避免每帧重复分配大内存。
 * 3. 根据场景自动选择混合策略：车流最大值、水流平均值。
 * 4. 对外暴露 [LightPaintingState] 状态流，供 UI 实时展示录制时长与累积帧。
 * 5. 录制结束后输出最终累积帧 Bitmap，调用方拥有该 Bitmap 的处置权。
 *
 * 线程模型：
 * - [submitFrame] 在相机线程调用，仅把帧投递到容量为 1 的 Channel，不会等待处理。
 * - 单线程 CoroutineDispatcher 负责串行执行像素混合、状态更新与 Bitmap 回收。
 * - [accumulator] 等核心缓冲在 [lock] 保护下访问，避免并发读写与回收冲突。
 */
class LightPaintingManager(context: Context) {

    companion object {
        private const val TAG = "LightPaintingManager"

        /** 像素处理的最大边长，超过则等比缩放，降低内存与计算压力 */
        private const val MAX_PROCESSING_DIMENSION = 1280

        /** 水流场景加权混合的基准权重，越小越平滑 */
        private const val WATER_BLEND_ALPHA = 0.08f

        /** 自由创作场景加权混合的基准权重 */
        private const val FREESTYLE_BLEND_ALPHA = 0.18f

        /** 像素分块大小，避免单帧处理耗时过长，便于协程取消 */
        private const val PIXEL_CHUNK_SIZE = 200_000
    }

    /** 光绘处理专用单线程调度器 */
    private val processingExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "LightPaintingWorker").apply { isDaemon = true }
    }
    private val processingDispatcher = processingExecutor.asCoroutineDispatcher()
    private val processingScope = CoroutineScope(processingDispatcher + SupervisorJob())

    /**
     * 帧输入队列，容量为 1 并按 DROP_OLDEST 丢弃旧帧。
     * 这样相机线程永远不会被阻塞，后台线程始终只处理最新一帧。
     */
    private val frameChannel = Channel<Bitmap>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Bitmap 对象池，用于复用缩放后的输入缓冲 */
    private val bitmapPool = BitmapPool(maxPoolSize = 4)

    /** 保护 accumulator、录制状态等核心状态 */
    private val lock = Any()

    @Volatile
    private var isRecording = false

    @Volatile
    private var isReleased = false

    /** 录制开始时间，用于计算 elapsedMs */
    private var recordingStartTime = 0L

    /** 当前累积帧，所有混合操作都在该缓冲上进行 */
    private var accumulator: Bitmap? = null

    /** 已处理帧数，用于部分混合算法的权重计算 */
    private var processedFrames = 0

    /** 当前场景模式 */
    @Volatile
    private var sceneMode: LightPaintingSceneMode = LightPaintingSceneMode.FREESTYLE

    /** 当前手动曝光参数 */
    @Volatile
    private var exposureParams: LightPaintingExposureParams = LightPaintingExposureParams()

    @Volatile
    private var boundCamera: Camera? = null

    private val _state = MutableStateFlow(LightPaintingState(false, 0L, null))
    val state: StateFlow<LightPaintingState> = _state.asStateFlow()

    init {
        startFrameConsumer()
    }

    /**
     * 启动后台帧消费协程。
     * 从 [frameChannel] 串行读取帧并处理，确保像素混合与 Bitmap 回收都在同一线程。
     */
    private fun startFrameConsumer() {
        processingScope.launch {
            try {
                for (frame in frameChannel) {
                    if (isReleased) {
                        safeRecycle(frame)
                        continue
                    }
                    processFrameInternal(frame)
                }
            } catch (_: CancellationException) {
                // release() 取消作用域，属正常流程
            } catch (e: Exception) {
                Log.e(TAG, "光绘帧消费循环异常", e)
            }
        }
    }

    /**
     * 开始录制并清空之前的累积结果。
     * 调用后 [submitFrame] 投递的帧才会被真正混合。
     */
    fun startRecording() {
        if (isReleased) return
        synchronized(lock) {
            // 丢弃尚未处理的旧帧，避免把预览阶段的画面带入录制
            drainPendingFrames()
            resetAccumulatorLocked()
            isRecording = true
            processedFrames = 0
            recordingStartTime = SystemClock.elapsedRealtime()
            _state.value = LightPaintingState(true, 0L, null)
        }
    }

    /**
     * 停止录制并返回最终累积帧。
     *
     * @return 最终光绘结果 Bitmap，调用方拥有该 Bitmap 并负责 [Bitmap.recycle]；
     *         若未开始录制或尚未收到任何帧则返回 null。
     */
    fun stopRecording(): Bitmap? {
        if (isReleased) return null
        synchronized(lock) {
            isRecording = false
            drainPendingFrames()
            val result = accumulator?.copy(Bitmap.Config.ARGB_8888, false)
            val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
            _state.value = LightPaintingState(false, elapsed, accumulator)
            return result
        }
    }

    /**
     * 重置所有状态，清空累积帧与录制时长。
     * 不会修改场景模式与曝光参数设置。
     */
    fun reset() {
        if (isReleased) return
        synchronized(lock) {
            isRecording = false
            drainPendingFrames()
            resetAccumulatorLocked()
            processedFrames = 0
            recordingStartTime = 0L
            _state.value = LightPaintingState(false, 0L, null)
        }
    }

    /**
     * 提交一帧相机图像供光绘累积。
     *
     * 重要契约：
     * - 只有在 [isRecording] 为 true 时，本方法才会接管 [bitmap] 的所有权并在处理完成后回收。
     * - 未录制时返回 null，调用方需自行管理 [bitmap] 生命周期。
     * - 消费端繁忙时，旧帧会被显式回收，新帧继续投递，相机线程永不阻塞。
     * - 返回值是 manager 当前持有的累积帧引用，仅供读取/展示，调用方不应回收或修改。
     *
     * @param bitmap 输入帧，建议尺寸不超过 [MAX_PROCESSING_DIMENSION]。
     * @return 当前累积帧预览，若未录制则返回 null。
     */
    fun submitFrame(bitmap: Bitmap): Bitmap? {
        if (isReleased || !isRecording || bitmap.isRecycled) {
            return null
        }

        // 容量为 1 的 Channel 在消费端繁忙时会保留最新一帧；
        // 先尝试清掉上一帧并回收，避免 Bitmap 在 Channel 中堆积。
        frameChannel.tryReceive().getOrNull()?.let { safeRecycle(it) }
        val sent = frameChannel.trySend(bitmap).isSuccess
        if (!sent) {
            // Channel 已关闭（release 后），回收当前帧。
            safeRecycle(bitmap)
        }
        return _state.value.accumulatedFrame
    }

    /**
     * 设置手动曝光参数。
     * 参数会保存在状态内，并在混合新帧时按曝光补偿微调亮度。
     */
    fun setExposureParams(
        iso: Int?,
        shutterMs: Long?,
        exposureCompensation: Int = 0
    ) {
        exposureParams = LightPaintingExposureParams(iso, shutterMs, exposureCompensation)
        applyExposureToCamera()
    }

    fun bindCamera(camera: Camera) {
        boundCamera = camera
        applyExposureToCamera()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun applyExposureToCamera() {
        val cam = boundCamera ?: return
        val params = exposureParams
        val iso = params.iso
        val shutterMs = params.shutterMs

        if (iso == null && shutterMs == null) return

        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val builder = CaptureRequestOptions.Builder()
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
            )
            iso?.let {
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it.coerceIn(100, 12800))
            }
            shutterMs?.let {
                val shutterNs = (it * 1_000_000L).coerceIn(1_000L, 30_000_000_000L)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterNs)
            }
            camera2Control.setCaptureRequestOptions(builder.build())
        } catch (e: Exception) {
            android.util.Log.w(TAG, "光绘曝光参数下发失败: ${e.message}")
        }
    }

    /**
     * 设置光绘场景模式，模式会决定下一帧使用的混合策略。
     * 切换模式不会清空已累积的帧；如需重新开始请调用 [reset]。
     */
    fun setSceneMode(mode: LightPaintingSceneMode) {
        sceneMode = mode
    }

    /**
     * 释放所有资源：停止后台线程、关闭 Channel、回收 Bitmap 与对象池。
     * 释放后不可再使用本实例。
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        isRecording = false

        // 先回收队列中的滞留帧，避免 Bitmap 泄漏
        drainPendingFrames()
        frameChannel.close()
        processingScope.cancel()
        processingExecutor.shutdown()

        synchronized(lock) {
            accumulator?.let { safeRecycle(it) }
            accumulator = null
            bitmapPool.clear()
            _state.value = LightPaintingState(false, 0L, null)
        }
    }

    /**
     * 单帧处理入口，运行在后台线程。
     */
    private suspend fun processFrameInternal(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        synchronized(lock) {
            if (!isRecording || isReleased) {
                safeRecycle(bitmap)
                return
            }

            val targetSize = calculateTargetSize(bitmap.width, bitmap.height)

            // 首次收到帧或尺寸变化时，用第一帧初始化累积缓冲
            if (accumulator == null ||
                accumulator?.width != targetSize.width ||
                accumulator?.height != targetSize.height
            ) {
                accumulator?.let { safeRecycle(it) }
                // 创建可变的累积缓冲，并绘制第一帧作为初始内容
                val initial = Bitmap.createBitmap(
                    targetSize.width,
                    targetSize.height,
                    Bitmap.Config.ARGB_8888
                )
                drawBitmapScaled(bitmap, initial)
                accumulator = initial
                processedFrames = 1
                updateStateLocked()
                safeRecycle(bitmap)
                return
            }
        }

        val acc: Bitmap
        val src: Bitmap
        synchronized(lock) {
            acc = accumulator ?: return
            val targetSize = Size(acc.width, acc.height)
            // 从对象池获取与累积帧同尺寸的缓冲，避免为每帧新分配
            src = bitmapPool.obtain(targetSize.width, targetSize.height, Bitmap.Config.ARGB_8888)
        }

        try {
            drawBitmapScaled(bitmap, src)
            blendFrame(acc, src)
            synchronized(lock) {
                if (isRecording) {
                    processedFrames++
                    updateStateLocked()
                }
            }
        } finally {
            bitmapPool.recycle(src)
            safeRecycle(bitmap)
        }
    }

    /**
     * 将 [source] 等比缩放绘制到 [target]（尺寸已匹配）。
     */
    private fun drawBitmapScaled(source: Bitmap, target: Bitmap) {
        target.eraseColor(0)
        val canvas = Canvas(target)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(
            source,
            null,
            RectF(0f, 0f, target.width.toFloat(), target.height.toFloat()),
            paint
        )
    }

    /**
     * 像素级混合：将 [src] 混合到 [accumulator] 中。
     * 按 [sceneMode] 选择最大值保留或加权平均。
     */
    private suspend fun blendFrame(accumulator: Bitmap, src: Bitmap) {
        val width = accumulator.width
        val height = accumulator.height
        val totalPixels = width * height

        val accPixels = IntArray(totalPixels)
        val srcPixels = IntArray(totalPixels)

        accumulator.getPixels(accPixels, 0, width, 0, 0, width, height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val ev = exposureParams.exposureCompensation
        val evFactor = if (ev != 0) 2.0.pow(ev / 6.0).toFloat() else 1f

        val mode = sceneMode
        val alpha = when (mode) {
            LightPaintingSceneMode.WATER -> WATER_BLEND_ALPHA
            LightPaintingSceneMode.FREESTYLE -> FREESTYLE_BLEND_ALPHA
            LightPaintingSceneMode.TRAFFIC -> 0f
        }

        // 分块处理，便于协程取消与避免 ANR
        var offset = 0
        while (offset < totalPixels) {
            yield()
            val end = min(offset + PIXEL_CHUNK_SIZE, totalPixels)
            for (i in offset until end) {
                val srcPixel = srcPixels[i]
                val accPixel = accPixels[i]

                var srcR = (srcPixel shr 16) and 0xFF
                var srcG = (srcPixel shr 8) and 0xFF
                var srcB = srcPixel and 0xFF

                if (evFactor != 1f) {
                    srcR = (srcR * evFactor).toInt().coerceIn(0, 255)
                    srcG = (srcG * evFactor).toInt().coerceIn(0, 255)
                    srcB = (srcB * evFactor).toInt().coerceIn(0, 255)
                }

                val accR = (accPixel shr 16) and 0xFF
                val accG = (accPixel shr 8) and 0xFF
                val accB = accPixel and 0xFF
                val accA = (accPixel shr 24) and 0xFF

                val outR: Int
                val outG: Int
                val outB: Int
                when (mode) {
                    LightPaintingSceneMode.TRAFFIC -> {
                        // 最大值保留：车灯轨迹逐帧提亮
                        outR = max(accR, srcR)
                        outG = max(accG, srcG)
                        outB = max(accB, srcB)
                    }
                    LightPaintingSceneMode.WATER,
                    LightPaintingSceneMode.FREESTYLE -> {
                        // 加权平均平滑：旧帧权重高，新帧权重低，形成流动丝绸感
                        outR = (accR * (1f - alpha) + srcR * alpha).toInt()
                        outG = (accG * (1f - alpha) + srcG * alpha).toInt()
                        outB = (accB * (1f - alpha) + srcB * alpha).toInt()
                    }
                }

                // 光绘结果通常保持不透明，alpha 取已有最大值
                val outA = max(accA, (srcPixel shr 24) and 0xFF).coerceAtMost(255)
                accPixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
            offset = end
        }

        accumulator.setPixels(accPixels, 0, width, 0, 0, width, height)
    }

    /**
     * 更新 [LightPaintingState]，始终对 accumulator 做一份拷贝后再暴露给 UI，
     * 避免 UI 线程与后台线程同时操作同一张 Bitmap 引发撕裂或回收冲突。
     */
    private fun updateStateLocked() {
        val acc = accumulator ?: return
        val elapsed = if (recordingStartTime > 0L) {
            SystemClock.elapsedRealtime() - recordingStartTime
        } else {
            0L
        }
        val displayCopy = acc.copy(Bitmap.Config.ARGB_8888, false)
        _state.value = LightPaintingState(isRecording, elapsed, displayCopy)
    }

    /**
     * 计算目标处理尺寸，限制最大边长。
     */
    private fun calculateTargetSize(width: Int, height: Int): Size {
        val maxDim = max(width, height)
        if (maxDim <= MAX_PROCESSING_DIMENSION) {
            return Size(width, height)
        }
        val scale = MAX_PROCESSING_DIMENSION.toFloat() / maxDim
        return Size(
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1)
        )
    }

    /**
     * 清空累积缓冲并回收旧 Bitmap。
     */
    private fun resetAccumulatorLocked() {
        accumulator?.let { safeRecycle(it) }
        accumulator = null
        processedFrames = 0
    }

    /**
     * 清空帧输入队列中尚未处理的 Bitmap 并回收。
     */
    private fun drainPendingFrames() {
        while (true) {
            val frame = frameChannel.tryReceive().getOrNull() ?: break
            safeRecycle(frame)
        }
    }

    /**
     * 安全回收 Bitmap，避免重复回收导致崩溃。
     */
    private fun safeRecycle(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
