package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 夜景模式参数配置
 *
 * @param targetFrames 合成总帧数，默认 7 帧，实际会被钳制在 [5, 9]
 * @param maxIso ISO 上限，用于估算夜景可接受的最大数字增益
 * @param exposureTimeNs 曝光时间偏好（单位：纳秒），null 表示自动估算
 * @param stabilizationEnabled 是否启用手持对齐（基于特征块的平移估计）
 */
data class NightParams(
    val targetFrames: Int = 7,
    val maxIso: Int = 6400,
    val exposureTimeNs: Long? = null,
    val stabilizationEnabled: Boolean = true
)

/**
 * 夜景模式管理器
 *
 * 负责夜景（NIGHT）模式下的多帧采集、对齐、时域/空域降噪与长曝光亮度增强。
 * 所有耗时处理均在协程后台线程执行，相机回调线程只负责快速投递 Bitmap 副本。
 *
 * 实现要点：
 * - 帧缓冲区 5~9 帧，按时间戳顺序存放。
 * - 手持场景使用灰度缩略图块匹配估计帧间平移，对齐后合成。
 * - 时域降噪：带对齐补偿的加权平均，权重由帧间相似度决定。
 * - 空域降噪：双边滤波，在平坦区域平滑噪点并保留边缘。
 * - 长曝光效果：根据场景平均亮度动态调整合成帧数与曝光增益。
 * - 全链路回收中间 Bitmap，避免多帧场景下内存泄漏。
 */
class NightModeManager(private val context: Context) {

    companion object {
        private const val TAG = "NightModeManager"

        /** 最小/最大合成帧数 */
        private const val MIN_FRAMES = 5
        private const val MAX_FRAMES = 9

        /** 对齐搜索最大平移（缩略图像素） */
        private const val ALIGNMENT_MAX_SHIFT = 8

        /** 对齐参考块占缩略图的比例 */
        private const val ALIGNMENT_BLOCK_RATIO = 0.5f

        /** 缩略图长边上限，用于加速对齐 */
        private const val ALIGNMENT_THUMB_MAX_SIZE = 320

        /** 暗场/亮场亮度阈值（0~255） */
        private const val DARK_LUMINANCE_THRESHOLD = 30f
        private const val BRIGHT_LUMINANCE_THRESHOLD = 120f
        private const val TARGET_LUMINANCE = 80f

        /** 最大曝光增益 */
        private const val MAX_GAIN = 4.0f

        /** 双边滤波空间/亮度 sigma */
        private const val BILATERAL_SPATIAL_SIGMA = 2.0f
        private const val BILATERAL_INTENSITY_SIGMA = 25f

        /** 单帧等待超时（ms） */
        private const val CAPTURE_TIMEOUT_MS = 12000L

        /** 默认单帧间隔（ms），用于剩余时间估算 */
        private const val DEFAULT_FRAME_INTERVAL_MS = 150L
    }

    /** 后台协程作用域 */
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 当前夜景参数 */
    @Volatile
    private var currentParams = NightParams()

    /** 当前状态流 */
    private val _state = MutableStateFlow(
        NightModeState(
            isCapturing = false,
            capturedFrames = 0,
            totalFrames = currentParams.targetFrames,
            estimatedRemainingMs = 0L
        )
    )
    val state: StateFlow<NightModeState> = _state.asStateFlow()

    /** 帧缓冲与并发锁 */
    private val frameLock = Object()
    private val pendingFrames = ArrayDeque<CapturedFrame>()

    /** 采集状态 */
    @Volatile
    private var isCapturing = false

    @Volatile
    private var captureCancelled = false

    /** 当前 captureAndProcess 的协程任务，用于 cancelCapture() */
    private var captureJob: kotlinx.coroutines.Job? = null

    /** 内部帧数据结构 */
    private data class CapturedFrame(
        val bitmap: Bitmap,
        val thumbnailGray: IntArray,
        val thumbWidth: Int,
        val thumbHeight: Int,
        val timestampNs: Long,
        val alignmentShift: Pair<Int, Int> = Pair(0, 0)
    )

    /**
     * 设置夜景参数。
     * 会在合法范围内修正 targetFrames 与 maxIso。
     */
    fun setNightParams(params: NightParams) {
        // 采集过程中不实时修改参数，避免与等待逻辑中的 targetFrames 不一致
        if (isCapturing) return
        currentParams = params.copy(
            targetFrames = params.targetFrames.coerceIn(MIN_FRAMES, MAX_FRAMES),
            maxIso = params.maxIso.coerceAtLeast(100)
        )
        updateState(
            totalFrames = currentParams.targetFrames,
            estimatedRemainingMs = 0L
        )
    }

    /**
     * 提交一帧到夜景缓冲区。
     *
     * 本方法轻量、非阻塞：仅复制 Bitmap 并加入队列，实际处理在 [captureAndProcess] 中异步完成。
     *
     * @param bitmap 输入帧（调用方仍拥有所有权）
     * @return 是否成功接收该帧
     */
    fun submitFrame(bitmap: Bitmap): Boolean {
        if (!isCapturing || captureCancelled) return false
        if (bitmap.isRecycled) return false

        synchronized(frameLock) {
            if (!isCapturing || captureCancelled) return false
            if (pendingFrames.size >= currentParams.targetFrames) {
                // 缓冲区已满，拒绝新帧，避免内存无限增长
                return false
            }
        }

        // 复制 Bitmap 到管理器内部所有权
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return false
        val (thumbGray, thumbW, thumbH) = createGrayThumbnail(copy)
        val frame = CapturedFrame(
            bitmap = copy,
            thumbnailGray = thumbGray,
            thumbWidth = thumbW,
            thumbHeight = thumbH,
            timestampNs = SystemClock.elapsedRealtimeNanos()
        )

        synchronized(frameLock) {
            if (!isCapturing || captureCancelled) {
                copy.recycle()
                return false
            }
            pendingFrames.add(frame)
            val count = pendingFrames.size
            val total = currentParams.targetFrames
            val avgInterval = computeAverageIntervalMs(pendingFrames)
            val remainingMs = ((total - count) * avgInterval).coerceAtLeast(0L)
            updateState(
                capturedFrames = count,
                totalFrames = total,
                estimatedRemainingMs = remainingMs
            )
        }
        return true
    }

    /**
     * 启动夜景采集与处理，完成后返回合成 Bitmap。
     *
     * @param targetFrames 本次采集目标帧数，默认 7
     * @param onStateUpdate 状态变化回调（可选），与 StateFlow 同步触发
     * @return 处理后的 Bitmap（调用方负责回收），失败或取消返回 null
     */
    suspend fun captureAndProcess(
        targetFrames: Int = 7,
        onStateUpdate: ((NightModeState) -> Unit)? = null
    ): Bitmap? = coroutineScope {
        val deferred = async(Dispatchers.Default) {
            doCaptureAndProcess(targetFrames, onStateUpdate)
        }
        captureJob = deferred
        try {
            deferred.await()
        } finally {
            captureJob = null
        }
    }

    /**
     * 取消当前采集/处理流程，并清空已缓冲帧。
     */
    fun cancelCapture() {
        captureCancelled = true
        isCapturing = false
        clearFrames()
        captureJob?.cancel()
        updateState(
            isCapturing = false,
            capturedFrames = 0,
            totalFrames = currentParams.targetFrames,
            estimatedRemainingMs = 0L
        )
    }

    /**
     * 释放夜景管理器资源。
     * 取消可能正在运行的任务并清空帧缓冲。
     */
    fun release() {
        cancelCapture()
        managerScope.cancel()
    }

    // ==================== 核心采集与处理流程 ====================

    /**
     * 在后台线程执行一次完整的夜景采集与合成。
     */
    private suspend fun doCaptureAndProcess(
        targetFrames: Int,
        onStateUpdate: ((NightModeState) -> Unit)?
    ): Bitmap? {
        if (!startCapture()) {
            Log.w(TAG, "已有夜景采集任务在运行，忽略本次调用")
            return null
        }

        val processingFrames = mutableListOf<CapturedFrame>()
        var resultBitmap: Bitmap? = null

        try {
            // 初始化采集状态
            val initialTotal = targetFrames.coerceIn(MIN_FRAMES, MAX_FRAMES)
            currentParams = currentParams.copy(targetFrames = initialTotal)
            clearFrames()
            updateState(
                isCapturing = true,
                capturedFrames = 0,
                totalFrames = initialTotal,
                estimatedRemainingMs = estimateInitialRemainingMs(initialTotal)
            )
            onStateUpdate?.invoke(_state.value)

            // 等待帧缓冲填充
            val frames = waitForFrames(onStateUpdate)
            if (frames == null) {
                Log.w(TAG, "夜景采集被取消或帧数不足")
                return null
            }
            processingFrames.addAll(frames)

            // 阶段状态：开始处理
            updateState(
                estimatedRemainingMs = 500L, // 处理大约耗时，后续可细化
                totalFrames = frames.size
            )
            onStateUpdate?.invoke(_state.value)

            // 1. 对齐
            val alignedFrames = alignFrames(frames)

            // 2. 计算帧权重（基于对齐后的相似度）
            val weights = computeFrameWeights(alignedFrames)

            // 3. 时域合成（加权平均）
            val width = frames[0].bitmap.width
            val height = frames[0].bitmap.height
            val merged = mergeFrames(alignedFrames, weights, width, height)

            // 4. 空域降噪：双边滤波
            val denoised = applyBilateralFilter(merged, BILATERAL_SPATIAL_SIGMA, BILATERAL_INTENSITY_SIGMA)
            merged.recycle()

            // 5. 长曝光亮度增强：根据场景亮度动态增益
            val meanLuminance = computeAverageLuminance(denoised)
            val maxGain = computeMaxGain()
            val gain = if (meanLuminance < TARGET_LUMINANCE) {
                (TARGET_LUMINANCE / meanLuminance.coerceAtLeast(1f)).coerceIn(1f, maxGain)
            } else {
                1f
            }
            resultBitmap = applyExposureGain(denoised, gain)

            // 完成状态
            updateState(
                isCapturing = false,
                capturedFrames = frames.size,
                totalFrames = frames.size,
                estimatedRemainingMs = 0L
            )
            onStateUpdate?.invoke(_state.value)

            Log.d(
                TAG,
                "夜景合成完成: ${frames.size} 帧, 平均亮度=${"%.1f".format(meanLuminance)}, 增益=${"%.2f".format(gain)}"
            )
        } catch (e: CancellationException) {
            // 取消时直接向上传播，由 finally 回收资源
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "夜景处理失败", e)
            resultBitmap = null
        } finally {
            isCapturing = false
            processingFrames.forEach { it.bitmap.recycle() }
            // 处理过程中如果 resultBitmap 为 null，确保没有残留缓冲
            if (resultBitmap == null) {
                synchronized(frameLock) {
                    pendingFrames.forEach { it.bitmap.recycle() }
                    pendingFrames.clear()
                }
            }
        }

        return resultBitmap
    }

    /**
     * 等待帧缓冲达到目标数量或超时。
     * 接收第一帧后会根据场景亮度动态调整目标帧数。
     */
    private suspend fun waitForFrames(
        onStateUpdate: ((NightModeState) -> Unit)?
    ): List<CapturedFrame>? {
        val startTime = SystemClock.elapsedRealtime()
        var adjustedTotal = currentParams.targetFrames
        var firstFrameAnalyzed = false

        while (true) {
            if (captureCancelled) return null

            val (count, total) = synchronized(frameLock) {
                pendingFrames.size to currentParams.targetFrames
            }

            // 根据首帧亮度动态调整总帧数
            if (!firstFrameAnalyzed && count >= 1) {
                val firstFrame = synchronized(frameLock) { pendingFrames.first() }
                adjustedTotal = adjustCaptureParams(firstFrame, total)
                currentParams = currentParams.copy(targetFrames = adjustedTotal)
                firstFrameAnalyzed = true
            }

            if (count >= adjustedTotal) break

            val elapsed = SystemClock.elapsedRealtime() - startTime
            if (elapsed > CAPTURE_TIMEOUT_MS) {
                if (count < MIN_FRAMES) {
                    Log.w(TAG, "夜景采集超时，可用帧 $count 不足 $MIN_FRAMES")
                    return null
                }
                Log.w(TAG, "夜景采集超时，使用现有 $count 帧继续处理")
                break
            }

            val framesSnapshot = synchronized(frameLock) { pendingFrames.toList() }
            val avgInterval = computeAverageIntervalMs(framesSnapshot)
            val remainingFrames = (adjustedTotal - count).coerceAtLeast(0)
            updateState(
                capturedFrames = count,
                totalFrames = adjustedTotal,
                estimatedRemainingMs = remainingFrames * avgInterval
            )
            onStateUpdate?.invoke(_state.value)

            delay(10)
        }

        // 取出所有缓冲帧并清空队列
        return synchronized(frameLock) {
            val list = pendingFrames.toList()
            pendingFrames.clear()
            list
        }
    }

    // ==================== 对齐 ====================

    /**
     * 对所有帧进行平移对齐，返回带有 alignmentShift 的新帧列表。
     * 若 stabilizationEnabled 为 false，则所有帧偏移置零。
     */
    private fun alignFrames(frames: List<CapturedFrame>): List<CapturedFrame> {
        if (frames.isEmpty()) return frames
        if (!currentParams.stabilizationEnabled) {
            return frames.map { it.copy(alignmentShift = Pair(0, 0)) }
        }

        val ref = frames[0]
        return frames.mapIndexed { index, frame ->
            if (index == 0) {
                frame.copy(alignmentShift = Pair(0, 0))
            } else {
                val shift = estimateAlignment(ref, frame)
                frame.copy(alignmentShift = shift)
            }
        }
    }

    /**
     * 基于灰度缩略图中心块 SAD 估计两帧之间的平移量。
     * 返回的是相对于原始分辨率的 (dx, dy)。
     */
    private fun estimateAlignment(ref: CapturedFrame, current: CapturedFrame): Pair<Int, Int> {
        val w = ref.thumbWidth
        val h = ref.thumbHeight
        val blockW = (w * ALIGNMENT_BLOCK_RATIO).toInt().coerceAtLeast(8)
        val blockH = (h * ALIGNMENT_BLOCK_RATIO).toInt().coerceAtLeast(8)
        val bx0 = (w - blockW) / 2
        val by0 = (h - blockH) / 2

        var bestDx = 0
        var bestDy = 0
        var minSad = Long.MAX_VALUE

        for (dy in -ALIGNMENT_MAX_SHIFT..ALIGNMENT_MAX_SHIFT) {
            for (dx in -ALIGNMENT_MAX_SHIFT..ALIGNMENT_MAX_SHIFT) {
                var sad = 0L
                for (y in 0 until blockH) {
                    val refY = by0 + y
                    val curY = (by0 + y + dy).coerceIn(0, h - 1)
                    for (x in 0 until blockW) {
                        val refX = bx0 + x
                        val curX = (bx0 + x + dx).coerceIn(0, w - 1)
                        val diff = ref.thumbnailGray[refY * w + refX] -
                                current.thumbnailGray[curY * w + curX]
                        sad += abs(diff)
                    }
                }
                if (sad < minSad) {
                    minSad = sad
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        // 将缩略图像素偏移映射到原始分辨率
        val scaleX = current.bitmap.width.toFloat() / w
        val scaleY = current.bitmap.height.toFloat() / h
        return Pair((bestDx * scaleX).toInt(), (bestDy * scaleY).toInt())
    }

    // ==================== 时域合成 ====================

    /**
     * 根据对齐后的帧间相似度计算每帧权重。
     * 与参考帧越相似，权重越高；相似度过低时给予保底权重避免丢弃。
     */
    private fun computeFrameWeights(frames: List<CapturedFrame>): FloatArray {
        if (frames.isEmpty()) return floatArrayOf()
        if (frames.size == 1) return floatArrayOf(1f)

        val ref = frames[0]
        val w = ref.thumbWidth
        val h = ref.thumbHeight
        val blockW = (w * ALIGNMENT_BLOCK_RATIO).toInt().coerceAtLeast(8)
        val blockH = (h * ALIGNMENT_BLOCK_RATIO).toInt().coerceAtLeast(8)
        val bx0 = (w - blockW) / 2
        val by0 = (h - blockH) / 2

        val weights = FloatArray(frames.size)
        weights[0] = 1f

        for (i in 1 until frames.size) {
            val frame = frames[i]
            // 将原始分辨率偏移映射回缩略图
            val scaleX = w.toFloat() / frame.bitmap.width
            val scaleY = h.toFloat() / frame.bitmap.height
            val tdx = (frame.alignmentShift.first * scaleX).toInt()
            val tdy = (frame.alignmentShift.second * scaleY).toInt()

            var sad = 0L
            for (y in 0 until blockH) {
                val refY = by0 + y
                val curY = (by0 + y + tdy).coerceIn(0, h - 1)
                for (x in 0 until blockW) {
                    val refX = bx0 + x
                    val curX = (bx0 + x + tdx).coerceIn(0, w - 1)
                    val diff = ref.thumbnailGray[refY * w + refX] -
                            frame.thumbnailGray[curY * w + curX]
                    sad += abs(diff)
                }
            }

            val maxSad = blockW * blockH * 255f
            val similarity = 1f - (sad / maxSad)
            weights[i] = max(0.2f, similarity)
        }

        // 归一化
        val sum = weights.sum()
        return weights.map { it / sum }.toFloatArray()
    }

    /**
     * 按权重合并对齐后的多帧，实现时域降噪。
     * 每帧按 estimatedAlignment 结果进行坐标偏移后参与加权累加。
     */
    private fun mergeFrames(
        frames: List<CapturedFrame>,
        weights: FloatArray,
        width: Int,
        height: Int
    ): Bitmap {
        val rAcc = FloatArray(width * height)
        val gAcc = FloatArray(width * height)
        val bAcc = FloatArray(width * height)
        val wAcc = FloatArray(width * height)

        frames.forEachIndexed { index, frame ->
            val pixels = IntArray(width * height)
            frame.bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val (shiftX, shiftY) = frame.alignmentShift
            val weight = weights[index]

            for (y in 0 until height) {
                val sy = (y + shiftY).coerceIn(0, height - 1)
                for (x in 0 until width) {
                    val sx = (x + shiftX).coerceIn(0, width - 1)
                    val p = pixels[sy * width + sx]
                    val i = y * width + x
                    rAcc[i] += ((p shr 16) and 0xFF) * weight
                    gAcc[i] += ((p shr 8) and 0xFF) * weight
                    bAcc[i] += (p and 0xFF) * weight
                    wAcc[i] += weight
                }
            }
        }

        val output = IntArray(width * height)
        for (i in output.indices) {
            val totalWeight = wAcc[i]
            if (totalWeight > 0f) {
                val r = (rAcc[i] / totalWeight).toInt().coerceIn(0, 255)
                val g = (gAcc[i] / totalWeight).toInt().coerceIn(0, 255)
                val b = (bAcc[i] / totalWeight).toInt().coerceIn(0, 255)
                output[i] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            } else {
                output[i] = 0xFF000000.toInt()
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(output, 0, width, 0, 0, width, height)
        return bitmap
    }

    // ==================== 空域降噪 ====================

    /**
     * 双边滤波：空间距离近且颜色相似的像素贡献更大，从而保护边缘。
     */
    private fun applyBilateralFilter(
        input: Bitmap,
        sigmaS: Float,
        sigmaI: Float
    ): Bitmap {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val output = IntArray(width * height)
        val radius = (sigmaS * 2f).toInt().coerceAtLeast(1)
        val invSigmaSSq = 1f / (2f * sigmaS * sigmaS)
        val invSigmaISq = 1f / (2f * sigmaI * sigmaI)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val center = pixels[i]
                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var sumW = 0f

                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val j = ny * width + nx
                        val p = pixels[j]
                        val pR = (p shr 16) and 0xFF
                        val pG = (p shr 8) and 0xFF
                        val pB = p and 0xFF

                        val dI = (pR - cR) * (pR - cR) +
                                (pG - cG) * (pG - cG) +
                                (pB - cB) * (pB - cB)
                        val dS = (dx * dx + dy * dy).toFloat()
                        val w = exp(-dS * invSigmaSSq - dI * invSigmaISq).toFloat()

                        sumR += pR * w
                        sumG += pG * w
                        sumB += pB * w
                        sumW += w
                    }
                }

                val r = (sumR / sumW).toInt().coerceIn(0, 255)
                val g = (sumG / sumW).toInt().coerceIn(0, 255)
                val b = (sumB / sumW).toInt().coerceIn(0, 255)
                output[i] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    // ==================== 长曝光亮度增强 ====================

    /**
     * 根据 ISO 上限计算允许的最大数字增益。
     */
    private fun computeMaxGain(): Float {
        val isoGain = currentParams.maxIso / 800f
        return isoGain.coerceIn(1f, MAX_GAIN)
    }

    /**
     * 对图像应用曝光增益。若增益接近 1，则直接返回原图避免额外拷贝。
     * 当 gain > 1 时会回收输入 Bitmap 并返回新 Bitmap。
     */
    private fun applyExposureGain(input: Bitmap, gain: Float): Bitmap {
        if (gain <= 1.01f) return input

        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) * gain
            val g = ((p shr 8) and 0xFF) * gain
            val b = (p and 0xFF) * gain
            pixels[i] = 0xFF000000.toInt() or
                    (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or
                    b.toInt().coerceIn(0, 255)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        input.recycle()
        return result
    }

    // ==================== 场景亮度与动态调整 ====================

    /**
     * 根据首帧亮度动态调整目标帧数：越暗采集越多帧，越亮减少帧数。
     */
    private fun adjustCaptureParams(firstFrame: CapturedFrame, baseFrames: Int): Int {
        val luminance = computeAverageLuminance(firstFrame.bitmap)
        return when {
            luminance < DARK_LUMINANCE_THRESHOLD -> (baseFrames + 2).coerceAtMost(MAX_FRAMES)
            luminance < DARK_LUMINANCE_THRESHOLD * 1.7f -> (baseFrames + 1).coerceAtMost(MAX_FRAMES)
            luminance > BRIGHT_LUMINANCE_THRESHOLD -> (baseFrames - 2).coerceAtLeast(MIN_FRAMES)
            luminance > BRIGHT_LUMINANCE_THRESHOLD * 0.7f -> (baseFrames - 1).coerceAtLeast(MIN_FRAMES)
            else -> baseFrames
        }
    }

    /**
     * 计算 Bitmap 的平均亮度（缩放到 64x64 加速）。
     */
    private fun computeAverageLuminance(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        scaled.recycle()

        var sum = 0L
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }
        return sum.toFloat() / pixels.size
    }

    // ==================== 工具方法 ====================

    /**
     * 创建灰度缩略图，用于快速对齐。
     * 返回三元组：(灰度像素数组, 缩略图宽度, 缩略图高度)
     */
    private fun createGrayThumbnail(bitmap: Bitmap): Triple<IntArray, Int, Int> {
        val maxSide = max(bitmap.width, bitmap.height)
        val scale = ALIGNMENT_THUMB_MAX_SIZE.toFloat() / maxSide
        val w = max((bitmap.width * scale).toInt(), 1)
        val h = max((bitmap.height * scale).toInt(), 1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        scaled.recycle()

        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
        }
        return Triple(gray, w, h)
    }

    /**
     * 估算剩余时间：优先使用历史帧间隔，否则使用 exposureTimeNs，最后使用默认值。
     */
    private fun computeAverageIntervalMs(frames: List<CapturedFrame>): Long {
        if (frames.size >= 2) {
            var totalDiff = 0L
            for (i in 1 until frames.size) {
                totalDiff += (frames[i].timestampNs - frames[i - 1].timestampNs) / 1_000_000L
            }
            return (totalDiff / (frames.size - 1)).coerceAtLeast(30L)
        }
        return currentParams.exposureTimeNs?.div(1_000_000L)?.coerceAtLeast(30L)
            ?: DEFAULT_FRAME_INTERVAL_MS
    }

    private fun estimateInitialRemainingMs(totalFrames: Int): Long {
        val interval = currentParams.exposureTimeNs?.div(1_000_000L)?.coerceAtLeast(30L)
            ?: DEFAULT_FRAME_INTERVAL_MS
        return totalFrames * interval
    }

    /**
     * 安全启动采集，避免并发执行。
     */
    private fun startCapture(): Boolean {
        synchronized(frameLock) {
            if (isCapturing) return false
            isCapturing = true
            captureCancelled = false
            return true
        }
    }

    /**
     * 清空帧缓冲并回收 Bitmap。
     */
    private fun clearFrames() {
        synchronized(frameLock) {
            pendingFrames.forEach { it.bitmap.recycle() }
            pendingFrames.clear()
        }
    }

    /**
     * 更新 StateFlow，未指定字段保持当前值。
     */
    private fun updateState(
        isCapturing: Boolean? = null,
        capturedFrames: Int? = null,
        totalFrames: Int? = null,
        estimatedRemainingMs: Long? = null
    ) {
        val current = _state.value
        _state.value = current.copy(
            isCapturing = isCapturing ?: current.isCapturing,
            capturedFrames = capturedFrames ?: current.capturedFrames,
            totalFrames = totalFrames ?: current.totalFrames,
            estimatedRemainingMs = estimatedRemainingMs ?: current.estimatedRemainingMs
        )
    }
}
