package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.RectData
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.renderer.RenderParameters
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 特化拍摄模式管理器（美食 / 街拍 / 宠物）
 *
 * 职责：
 * 1. 根据 [CaptureMode] 返回专属配置 [SpecializedModeConfig] 与哈苏大师参数 [HasselbladParams]。
 * 2. 将哈苏参数映射到 GPU 渲染管线可用的 [RenderParameters]。
 * 3. 维护循环帧缓冲区，为街拍 / 宠物模式提供 0 延迟快门（预缓存 3-5 帧）。
 * 4. 使用 ML Kit Object Detection 在后台线程检测宠物/物体，输出追焦提示与主体包围框。
 * 5. 正确回收 Bitmap，避免内存泄漏；使用协程处理异步检测与帧缓存。
 *
 * 线程安全：
 * - 帧缓冲区使用 [bufferLock] 同步。
 * - ObjectDetector 采用单例，引用计数管理生命周期。
 * - 检测任务运行在 [managerScope]（Dispatchers.Default + SupervisorJob）。
 */
class SpecializedModeManager(context: Context) {

    companion object {
        private const val TAG = "SpecializedModeManager"

        /** 循环帧缓冲区容量：街拍 / 宠物模式预缓存 3-5 帧 */
        private const val FRAME_BUFFER_SIZE = 5

        /** 宠物相关关键词（ML Kit 返回英文标签，兼容大小写） */
        private val PET_KEYWORDS = listOf(
            "cat", "dog", "animal", "pet", "kitten", "puppy", "feline", "canine"
        )

        /** 街拍感兴趣关键词（人物 / 交通工具等动态主体） */
        private val STREET_SUBJECT_KEYWORDS = listOf(
            "person", "people", "vehicle", "car", "bus", "train", "bicycle",
            "motorcycle", "scooter", "traffic light"
        )

        // ===== ObjectDetector 单例与引用计数 =====
        @Volatile
        private var objectDetectorInstance: ObjectDetector? = null
        private val detectorLock = Any()
        private var detectorRefCount = 0

        /**
         * 获取或创建 ML Kit ObjectDetector 单例。
         * 使用 STREAM_MODE 适合连续视频帧，开启分类以获取标签。
         */
        private fun getObjectDetector(): ObjectDetector {
            return objectDetectorInstance ?: synchronized(detectorLock) {
                objectDetectorInstance ?: ObjectDetection.getClient(
                    ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .build()
                ).also {
                    objectDetectorInstance = it
                    Log.d(TAG, "ObjectDetector 单例已创建")
                }
            }
        }

        /**
         * 增加 ObjectDetector 引用计数。
         */
        private fun retainObjectDetector(): ObjectDetector {
            val detector = getObjectDetector()
            synchronized(detectorLock) { detectorRefCount++ }
            return detector
        }

        /**
         * 减少引用计数；当计数归零时关闭并释放 ObjectDetector。
         */
        private fun releaseObjectDetector() {
            synchronized(detectorLock) {
                detectorRefCount = (detectorRefCount - 1).coerceAtLeast(0)
                if (detectorRefCount == 0) {
                    try {
                        objectDetectorInstance?.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "释放 ObjectDetector 失败", e)
                    } finally {
                        objectDetectorInstance = null
                    }
                    Log.d(TAG, "ObjectDetector 单例已释放")
                }
            }
        }
    }

    /** 后台协程作用域：处理帧缓存、参数计算、ML Kit 检测 */
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 释放标记 */
    private val isReleased = AtomicBoolean(false)

    /** 当前激活的特化模式；影响检测策略与 0 延迟选帧逻辑 */
    @Volatile
    private var activeMode: CaptureMode = CaptureMode.AI_AUTO

    /** 单例 ObjectDetector（引用计数已在 init 中 +1） */
    private val objectDetector: ObjectDetector = retainObjectDetector()

    // ===== 帧缓冲区 =====
    private val bufferLock = Any()
    private val frameBuffer = ArrayDeque<FrameBufferEntry>(FRAME_BUFFER_SIZE)

    // ===== 追踪结果（追焦提示、主体包围框） =====
    private val _trackingResult = MutableStateFlow<TrackingResult?>(null)
    val trackingResult: StateFlow<TrackingResult?> = _trackingResult.asStateFlow()

    init {
        Log.d(TAG, "SpecializedModeManager 已初始化")
    }

    /**
     * 设置当前激活的特化模式。
     * CameraXManager 在切换拍摄模式时应调用此方法，以便管理器调整检测与选帧策略。
     */
    fun setActiveMode(mode: CaptureMode) {
        activeMode = mode
        if (!isSpecializedMode(mode)) {
            clearFrameBuffer()
        }
        Log.d(TAG, "激活模式: $mode")
    }

    /**
     * 根据 [CaptureMode] 返回特化模式配置。
     *
     * 美食模式：强化暖色调与饱和度，推荐俯拍 / 45° 构图。
     * 街拍模式：启用 0 延迟快门，电影质感低饱和高对比冷调。
     * 宠物模式：启用 0 延迟快门，快速快门追焦，适中饱和度。
     */
    fun getConfig(mode: CaptureMode): SpecializedModeConfig = when (mode) {
        CaptureMode.FOOD -> SpecializedModeConfig(
            mode = CaptureMode.FOOD,
            shutterLagElimination = false,
            burstEnabled = true,
            preferredColorModeId = "ccd_warm",
            sceneParams = mapOf(
                "saturation" to 20,
                "colorTemp" to 15,
                "sharpness" to 20,
                "vignette" to -10,
                "warmth" to 20,
                "compositionHint" to 45 // 45° 提示，仅作语义标记
            )
        )
        CaptureMode.STREET -> SpecializedModeConfig(
            mode = CaptureMode.STREET,
            shutterLagElimination = true,
            burstEnabled = true,
            preferredColorModeId = "tx400",
            sceneParams = mapOf(
                "shutterSpeedNs" to 2_000_000, // 约 1/500s
                "iso" to 400,
                "saturation" to 5,
                "contrast" to 20,
                "sharpness" to 25,
                "colorTemp" to -5
            )
        )
        CaptureMode.PET -> SpecializedModeConfig(
            mode = CaptureMode.PET,
            shutterLagElimination = true,
            burstEnabled = true,
            preferredColorModeId = "portra",
            sceneParams = mapOf(
                "shutterSpeedNs" to 1_000_000, // 约 1/1000s，冻结运动
                "iso" to 800,
                "saturation" to 12,
                "contrast" to 10,
                "sharpness" to 30,
                "colorTemp" to 0
            )
        )
        else -> SpecializedModeConfig(
            mode = mode,
            shutterLagElimination = false,
            burstEnabled = false,
            preferredColorModeId = "cc",
            sceneParams = emptyMap()
        )
    }

    /**
     * 根据 [CaptureMode] 返回哈苏大师推荐参数。
     *
     * - 美食：使用 SceneToHasselbladMapping 的 food-restaurant 参数。
     * - 街拍：使用 urban-street 映射，增强电影质感。
     * - 宠物：以 event-sports 映射为基准，调整为适合宠物抓拍的参数。
     */
    fun getRecommendedParams(mode: CaptureMode): HasselbladParams = when (mode) {
        CaptureMode.FOOD -> SceneToHasselbladMapping.getParams("food-restaurant").let {
            it.copy(
                saturation = (it.saturation + 5).coerceIn(-30, 30),
                colorTemp = (it.colorTemp + 5).coerceIn(-30, 30),
                sharpness = (it.sharpness + 5).coerceIn(-30, 30),
                vignette = 10, // 暗角增强食欲氛围
                softLight = SoftLightMode.SOFT
            )
        }
        CaptureMode.STREET -> SceneToHasselbladMapping.getParams("urban-street").let {
            it.copy(
                saturation = (it.saturation - 5).coerceIn(-30, 30),
                contrast = (it.contrast + 5).coerceIn(-30, 30),
                colorTemp = (it.colorTemp - 5).coerceIn(-30, 30),
                sharpness = (it.sharpness + 5).coerceIn(-30, 30)
            )
        }
        CaptureMode.PET -> {
            val base = SceneToHasselbladMapping.getParams("event-sports")
            base.copy(
                saturation = (base.saturation + 5).coerceIn(-30, 30),
                sharpness = (base.sharpness + 10).coerceIn(-30, 30),
                contrast = (base.contrast - 5).coerceIn(-30, 30),
                colorTemp = -5,
                vignette = 0
            )
        }
        else -> HasselbladParams()
    }

    /**
     * 将 [CaptureMode] 映射为 GPU 渲染管线使用的 [RenderParameters]。
     */
    fun getRenderParameters(mode: CaptureMode): RenderParameters {
        return hasselbladParamsToRenderParameters(getRecommendedParams(mode))
    }

    /**
     * 提交实时预览帧。
     *
     * 调用方将 [bitmap] 所有权移交给本方法，本方法负责后续处理与回收。
     * 在街拍 / 宠物模式下，会异步触发 ML Kit 物体检测并更新 [trackingResult]。
     */
    fun submitPreviewFrame(bitmap: Bitmap) {
        if (isReleased.get() || bitmap.isRecycled || !isSpecializedMode(activeMode)) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }

        val entry = FrameBufferEntry(bitmap, SystemClock.elapsedRealtimeNanos())
        addToBuffer(entry)

        // 仅在需要追焦的特化模式下跑物体检测
        if (activeMode == CaptureMode.PET || activeMode == CaptureMode.STREET) {
            detectObjectsAsync(entry)
        }
    }

    /**
     * 0 延迟快门：从循环帧缓冲区中返回最近的一帧或最佳帧。
     *
     * - 街拍 / 宠物：基于检测到的主体面积与中心位置打分，选择最佳帧。
     * - 美食 / 其他：直接返回最新帧。
     *
     * 返回的 Bitmap 为缓冲区帧的副本，调用方负责回收。
     */
    fun captureZeroLag(): Bitmap? {
        if (isReleased.get()) return null

        return synchronized(bufferLock) {
            if (frameBuffer.isEmpty()) return@synchronized null

            val bestEntry = when (activeMode) {
                CaptureMode.PET, CaptureMode.STREET -> {
                    frameBuffer.maxByOrNull { scoreFrame(it, activeMode) }
                        ?: frameBuffer.last()
                }
                else -> frameBuffer.last()
            }

            if (bestEntry.bitmap.isRecycled) null else try {
                bestEntry.bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } catch (e: Exception) {
                Log.e(TAG, "0 延迟快门拷贝失败", e)
                null
            }
        }
    }

    /**
     * 清空帧缓冲区并回收所有 Bitmap。
     */
    fun clearFrameBuffer() {
        synchronized(bufferLock) {
            while (frameBuffer.isNotEmpty()) {
                frameBuffer.removeFirst().safeRecycle()
            }
        }
        Log.d(TAG, "帧缓冲区已清空")
    }

    /**
     * 释放资源：清空帧缓冲、取消协程、释放 ObjectDetector 引用。
     */
    fun release() {
        if (!isReleased.compareAndSet(false, true)) return

        clearFrameBuffer()
        managerScope.cancel()
        releaseObjectDetector()

        Log.d(TAG, "SpecializedModeManager 已释放")
    }

    // ==================== 内部方法 ====================

    private fun isSpecializedMode(mode: CaptureMode): Boolean {
        return mode == CaptureMode.FOOD || mode == CaptureMode.STREET || mode == CaptureMode.PET
    }

    /**
     * 将帧加入循环缓冲区；超过容量时移除最旧帧并安全回收。
     */
    private fun addToBuffer(entry: FrameBufferEntry) {
        synchronized(bufferLock) {
            if (isReleased.get()) {
                entry.safeRecycle()
                return
            }
            while (frameBuffer.size >= FRAME_BUFFER_SIZE) {
                frameBuffer.removeFirst().safeRecycle()
            }
            frameBuffer.addLast(entry)
        }
    }

    /**
     * 异步执行 ML Kit 物体检测，并更新当前帧的检测元数据。
     */
    private fun detectObjectsAsync(entry: FrameBufferEntry) {
        managerScope.launch {
            if (entry.markedForRecycle.get() || entry.bitmap.isRecycled) return@launch
            entry.detectionPending.set(true)

            try {
                val inputImage = InputImage.fromBitmap(entry.bitmap, 0)
                val detectedObjects = withContext(Dispatchers.IO) {
                    objectDetector.process(inputImage).await()
                }

                if (entry.markedForRecycle.get() || entry.bitmap.isRecycled) return@launch

                val width = entry.bitmap.width.toFloat()
                val height = entry.bitmap.height.toFloat()

                entry.detectedObjects = detectedObjects.map { obj ->
                    val box = obj.boundingBox
                    DetectedObjectInfo(
                        boundingBox = RectData(
                            left = box.left / width,
                            top = box.top / height,
                            right = box.right / width,
                            bottom = box.bottom / height
                        ),
                        labels = obj.labels.map { it.text },
                        confidence = obj.labels.maxOfOrNull { it.confidence }
                            ?: obj.trackingId?.toFloat()?.coerceIn(0f, 1f)
                            ?: 0.5f
                    )
                }

                updateTrackingResult(entry)
            } catch (e: CancellationException) {
                // 释放或模式切换导致的取消，无需记录为错误
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "物体检测失败", e)
            } finally {
                entry.onDetectionFinished()
            }
        }
    }

    /**
     * 更新 [trackingResult]，输出追焦提示与最佳主体包围框。
     */
    private fun updateTrackingResult(entry: FrameBufferEntry) {
        val objects = entry.detectedObjects
        if (objects.isEmpty()) {
            _trackingResult.value = TrackingResult(
                mode = activeMode,
                subjectBounds = null,
                subjectCount = 0,
                hint = "正在寻找主体…",
                confidence = 0f
            )
            return
        }

        val candidates = when (activeMode) {
            CaptureMode.PET -> {
                val pets = objects.filter { isPetObject(it) }
                if (pets.isNotEmpty()) pets else objects
            }
            CaptureMode.STREET -> objects.filter { isStreetSubject(it) }.ifEmpty { objects }
            else -> objects
        }

        val best = candidates.maxByOrNull { objectArea(it) } ?: candidates.first()
        val hint = when (activeMode) {
            CaptureMode.PET -> if (isPetObject(best)) "已锁定宠物，保持追焦" else "已锁定主体，请准备抓拍"
            CaptureMode.STREET -> "已锁定街景主体，等待决定性瞬间"
            else -> "主体已锁定"
        }

        _trackingResult.value = TrackingResult(
            mode = activeMode,
            subjectBounds = best.boundingBox,
            subjectCount = candidates.size,
            hint = hint,
            confidence = best.confidence
        )
    }

    private fun isPetObject(obj: DetectedObjectInfo): Boolean {
        return obj.labels.any { label ->
            PET_KEYWORDS.any { keyword -> label.contains(keyword, ignoreCase = true) }
        }
    }

    private fun isStreetSubject(obj: DetectedObjectInfo): Boolean {
        return obj.labels.any { label ->
            STREET_SUBJECT_KEYWORDS.any { keyword -> label.contains(keyword, ignoreCase = true) }
        }
    }

    private fun objectArea(obj: DetectedObjectInfo): Float {
        return (obj.boundingBox.right - obj.boundingBox.left) *
            (obj.boundingBox.bottom - obj.boundingBox.top)
    }

    /**
     * 为缓冲区中的某一帧打分，用于 0 延迟快门的“最佳帧”选择。
     * 街拍 / 宠物均偏好：主体面积大、位于画面中心、置信度高。
     */
    private fun scoreFrame(entry: FrameBufferEntry, mode: CaptureMode): Float {
        val width = entry.bitmap.width.toFloat()
        val height = entry.bitmap.height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = kotlin.math.hypot(centerX, centerY)

        val objects = entry.detectedObjects
        if (objects.isEmpty()) return 0f

        return when (mode) {
            CaptureMode.PET -> {
                val pets = objects.filter { isPetObject(it) }
                val candidates = if (pets.isNotEmpty()) pets else objects
                candidates.maxOfOrNull { obj ->
                    val area = objectArea(obj) * width * height
                    val centerScore = centerScore(obj, centerX, centerY, maxDist)
                    area * centerScore * obj.confidence
                } ?: 0f
            }
            CaptureMode.STREET -> {
                val subjects = objects.filter { isStreetSubject(it) }.ifEmpty { objects }
                val areaScore = subjects.sumOf { obj ->
                    (objectArea(obj) * width * height *
                        centerScore(obj, centerX, centerY, maxDist) *
                        obj.confidence).toDouble()
                }.toFloat()
                areaScore + subjects.size * 0.05f
            }
            else -> 0f
        }
    }

    private fun centerScore(
        obj: DetectedObjectInfo,
        centerX: Float,
        centerY: Float,
        maxDist: Float
    ): Float {
        val boxCenterX = (obj.boundingBox.left + obj.boundingBox.right) / 2f * (centerX * 2f)
        val boxCenterY = (obj.boundingBox.top + obj.boundingBox.bottom) / 2f * (centerY * 2f)
        val dist = kotlin.math.hypot(boxCenterX - centerX, boxCenterY - centerY)
        return 1f - (dist / maxDist).coerceIn(0f, 1f)
    }

    /**
     * 将哈苏大师参数转换为 [RenderParameters]，供 GPU 渲染管线使用。
     * 映射规则与 CameraXManager 保持一致，确保取景器与成片效果统一。
     */
    private fun hasselbladParamsToRenderParameters(params: HasselbladParams): RenderParameters {
        return RenderParameters(
            saturation = params.saturation * 3.3f,
            contrast = params.contrast * 3.3f,
            brightness = params.tone * 1.5f,
            warmth = params.colorTemp * 3.3f,
            sharpness = params.sharpness.coerceAtLeast(0) * 3.3f,
            clarity = params.clarity * 3.3f,
            highlights = params.highlights * 3.3f,
            shadows = params.shadows * 3.3f,
            vibrance = params.saturation.coerceAtLeast(0) * 2f,
            fade = if (params.tone < 0) (-params.tone) * 1.5f else 0f,
            denoise = 0f,
            skinSmooth = 0f,
            exposure = 0f,
            texture = 0f,
            grain = 0f,
            dehaze = 0f,
            whites = 0f,
            blacks = 0f
        )
    }

    // ==================== 内部数据类 ====================

    /**
     * 帧缓冲条目。
     *
     * 通过 [detectionPending] 与 [markedForRecycle] 协调异步检测与 Bitmap 回收，
     * 防止检测过程中 Bitmap 被回收导致崩溃或内存泄漏。
     */
    private class FrameBufferEntry(
        val bitmap: Bitmap,
        val timestampNs: Long
    ) {
        @Volatile
        var detectedObjects: List<DetectedObjectInfo> = emptyList()

        val detectionPending = AtomicBoolean(false)
        val markedForRecycle = AtomicBoolean(false)

        /**
         * 安全回收：若检测仍在进行则标记延迟回收，检测结束后再真正回收。
         */
        fun safeRecycle() {
            if (markedForRecycle.compareAndSet(false, true)) {
                if (!detectionPending.get() && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }

        /**
         * 检测完成回调：若已被标记回收，则执行真正回收。
         */
        fun onDetectionFinished() {
            detectionPending.set(false)
            if (markedForRecycle.get() && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * 检测到的物体信息（坐标已归一化到 0-1）。
     */
    private data class DetectedObjectInfo(
        val boundingBox: RectData,
        val labels: List<String>,
        val confidence: Float
    )

    /**
     * 追焦 / 构图提示结果。
     */
    data class TrackingResult(
        val mode: CaptureMode,
        val subjectBounds: RectData?,
        val subjectCount: Int,
        val hint: String,
        val confidence: Float
    )
}
