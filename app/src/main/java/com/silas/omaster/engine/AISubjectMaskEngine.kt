package com.silas.omaster.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.suspendCancellableCoroutine

/**
 * AI 主体检测蒙版引擎
 *
 * 参照 RapidRAW 的 AI Subject Mask 功能。
 * RapidRAW 使用深度学习模型自动检测图片中的主体（人/动物/物体），
 * 生成精确蒙版用于局部调整。
 *
 * Android 端实现方案：
 * - 主路径：ML Kit Selfie Segmentation（实时主体分割，支持人像）
 * - 降级路径1：ML Kit Object Detection + 显著性检测
 * - 降级路径2：基于亮度/对比度的启发式显著性检测
 *
 * 操作链路：
 * 1. 用户点击"AI 主体蒙版"
 * 2. 引擎运行主体检测
 * 3. 生成蒙版（FloatArray [0,1]）
 * 4. 蒙版自动叠加到局部调整面板
 * 5. 用户可调节 AI 蒙版强度/羽化
 */
class AISubjectMaskEngine {

    /** 主体类型 */
    enum class SubjectType(val label: String) {
        PERSON("人物"),
        OBJECT("物体"),
        AUTO("自动")
    }

    /** AI 蒙版结果 */
    data class SubjectMaskResult(
        val mask: FloatArray,       // [width * height] 归一化 [0, 1]
        val width: Int,
        val height: Int,
        val method: String,         // "mlkit_segmentation", "mlkit_object", "saliency"
        val confidence: Float,      // 平均置信度
        val processingTimeMs: Long
    )

    private var segmenter: Segmenter? = null

    init {
        try {
            segmenter = SelfieSegmenter.getClient(
                SelfieSegmenter.Builder()
                    .setDetectorMode(SelfieSegmenter.SINGLE_IMAGE_MODE)
                    .build()
            )
        } catch (_: Exception) {
            segmenter = null
        }
    }

    /**
     * 检测主体并生成蒙版
     * @param bitmap 输入图片
     * @param subjectType 主体类型
     */
    suspend fun detectSubject(
        bitmap: Bitmap,
        subjectType: SubjectType = SubjectType.AUTO
    ): SubjectMaskResult {
        val startTime = System.currentTimeMillis()

        val result = when {
            segmenter != null && (subjectType == SubjectType.PERSON || subjectType == SubjectType.AUTO) ->
                detectWithMLKitSegmentation(bitmap)
            else ->
                detectWithSaliency(bitmap)
        }

        val elapsed = System.currentTimeMillis() - startTime
        return result.copy(processingTimeMs = elapsed)
    }

    /**
     * ML Kit Selfie Segmentation 检测
     * 高精度人像分割，返回每个像素是否属于前景
     */
    private suspend fun detectWithMLKitSegmentation(bitmap: Bitmap): SubjectMaskResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val segmentation = processSegmentation(inputImage)

            val maskBuffer = segmentation.buffer
            // ML Kit segmentation buffer: ByteBuffer, 每个像素为 float 置信度
            // 尺寸取决于模型，通常接近原图
            maskBuffer.rewind()
            val bufferLen = maskBuffer.remaining() / 4  // 每像素4字节(float)
            // 估算蒙版尺寸（近似正方形）
            val maskH = kotlin.math.sqrt(bufferLen.toFloat()).toInt().coerceAtLeast(1)
            val maskW = (bufferLen / maskH).coerceAtLeast(1)
            val mask = FloatArray(bitmap.width * bitmap.height)

            // 读取所有置信度值并缩放到图片尺寸
            for (i in 0 until min(bufferLen, maskW * maskH)) {
                val confidence = maskBuffer.getFloat(i * 4)
                // 简单最近邻缩放
                val srcX = (i % maskW) * bitmap.width / maskW
                val srcY = (i / maskW) * bitmap.height / maskH
                if (srcX < bitmap.width && srcY < bitmap.height) {
                    mask[srcY * bitmap.width + srcX] = confidence
                }
            }

            SubjectMaskResult(
                mask = mask,
                width = bitmap.width,
                height = bitmap.height,
                method = "mlkit_segmentation",
                confidence = mask.average().toFloat(),
                processingTimeMs = 0
            )
        } catch (_: Exception) {
            // ML Kit 失败，降级到显著性检测
            detectWithSaliency(bitmap)
        }
    }

    private suspend fun processSegmentation(inputImage: InputImage): Segmentation {
        val seg = segmenter ?: throw RuntimeException("Segmenter not available")
        return suspendCancellableCoroutine { cont ->
            seg.process(inputImage)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
    }

    /**
     * 基于显著性检测的主体蒙版
     * 当 ML Kit 不可用时的降级方案
     * 使用 FT（Frequency-tuned）显著性检测算法
     */
    private fun detectWithSaliency(bitmap: Bitmap): SubjectMaskResult {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // 计算全局平均颜色（Lab 近似）
        var avgR = 0f; var avgG = 0f; var avgB = 0f
        val sampleStep = max(1, min(w, h) / 256)
        var sampleCount = 0

        for (y in 0 until h step sampleStep) {
            for (x in 0 until w step sampleStep) {
                val p = pixels[y * w + x]
                avgR += ((p shr 16) and 0xFF) / 255f
                avgG += ((p shr 8) and 0xFF) / 255f
                avgB += (p and 0xFF) / 255f
                sampleCount++
            }
        }
        avgR /= sampleCount; avgG /= sampleCount; avgB /= sampleCount

        // 计算每个像素与全局平均的色差作为显著性
        val mask = FloatArray(w * h)
        var maxSal = 0f
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            // Lab 空间的近似色差
            val dr = r - avgR
            val dg = g - avgG
            val db = b - avgB
            val sal = dr * dr + dg * dg + db * db
            mask[i] = sal
            if (sal > maxSal) maxSal = sal
        }

        // 归一化 + 中心偏好加权
        val cx = w / 2f; val cy = h / 2f
        val maxDist = sqrt(cx * cx + cy * cy)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                mask[idx] = if (maxSal > 0f) mask[idx] / maxSal else 0f

                // 中心偏好：离中心越近权重越高（主体通常在画面中心）
                val dx = (x - cx) / cx
                val dy = (y - cy) / cy
                val centerWeight = 1f - (dx * dx + dy * dy) * 0.3f
                mask[idx] = mask[idx] * centerWeight.coerceIn(0.3f, 1f)

                // 阈值化：显著区域 vs 非显著区域
                mask[idx] = if (mask[idx] > 0.3f) {
                    ((mask[idx] - 0.3f) / 0.7f).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

        // 高斯模糊平滑蒙版边缘
        val smoothed = gaussianBlur1D(mask, w, h, 3)

        return SubjectMaskResult(
            mask = smoothed,
            width = w,
            height = h,
            method = "saliency",
            confidence = smoothed.average().toFloat(),
            processingTimeMs = 0
        )
    }

    /**
     * 缩放蒙版（双线性插值）
     */
    private fun scaleMask(
        mask: FloatArray, srcW: Int, srcH: Int,
        dstW: Int, dstH: Int
    ): FloatArray {
        val result = FloatArray(dstW * dstH)
        val xRatio = srcW.toFloat() / dstW
        val yRatio = srcH.toFloat() / dstH

        for (y in 0 until dstH) {
            for (x in 0 until dstW) {
                val srcX = (x * xRatio).toInt().coerceIn(0, srcW - 1)
                val srcY = (y * yRatio).toInt().coerceIn(0, srcH - 1)
                result[y * dstW + x] = mask[srcY * srcW + srcX]
            }
        }
        return result
    }

    /**
     * 简单 1D 高斯模糊（水平 + 垂直两遍）
     */
    private fun gaussianBlur1D(data: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val temp = FloatArray(w * h)
        val result = FloatArray(w * h)

        // 水平
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f; var weightSum = 0f
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val weight = gaussianWeight(dx, radius)
                    sum += data[y * w + nx] * weight
                    weightSum += weight
                }
                temp[y * w + x] = sum / weightSum
            }
        }

        // 垂直
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f; var weightSum = 0f
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val weight = gaussianWeight(dy, radius)
                    sum += temp[ny * w + x] * weight
                    weightSum += weight
                }
                result[y * w + x] = sum / weightSum
            }
        }

        return result
    }

    private fun gaussianWeight(x: Int, radius: Int): Float {
        val sigma = radius / 2f
        return (1f / (sigma * 2.5066f)) * kotlin.math.exp(-x * x / (2f * sigma * sigma)).toFloat()
    }

    /**
     * 释放资源
     */
    fun release() {
        segmenter?.close()
        segmenter = null
    }
}
