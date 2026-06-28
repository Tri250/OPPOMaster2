package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * P0-2: AI 局部遮罩管理器
 *
 * 纯端侧实现，基于 ML Kit Selfie Segmentation 进行人像分割。
 * 支持三种遮罩模式：
 * - SUBJECT（主体/人像）：自动识别人像区域
 * - BACKGROUND（背景）：反向选择，即非人像区域
 * - SKY（天空）：基于色彩阈值启发式分割（简化版，后续可替换为 TFLite 天空模型）
 *
 * 产品经理交互审查：
 * - 用户心智：用户想"只调亮人脸"或"只虚化背景"，不应需要手动抠图
 * - 操作链路：点击「局部」Tab → 选择遮罩类型（人像/背景/天空）→ 调整参数仅影响选中区域
 * - 性能预期：分割推理 100-300ms，GPU 实时预览叠加遮罩无感知延迟
 */
class AIMaskManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    // ML Kit Selfie Segmenter（人像分割）
    private val selfieSegmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        Segmentation.getClient(options)
    }

    /**
     * 遮罩类型
     */
    enum class MaskType {
        SUBJECT,    // 主体（人像）
        BACKGROUND, // 背景（反向）
        SKY         // 天空（启发式）
    }

    /**
     * 遮罩结果
     * @param maskBitmap 单通道灰度图（0-255），白色表示选中区域
     * @param type 遮罩类型
     * @param width 原始图片宽度
     * @param height 原始图片高度
     */
    data class MaskResult(
        val maskBitmap: Bitmap,
        val type: MaskType,
        val width: Int,
        val height: Int
    )

    /**
     * 生成人像遮罩（主体或背景）
     *
     * @param bitmap 输入图片
     * @param type SUBJECT 或 BACKGROUND
     * @return 遮罩灰度图
     */
    suspend fun generatePortraitMask(bitmap: Bitmap, type: MaskType = MaskType.SUBJECT): MaskResult? =
        withContext(Dispatchers.Default) {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val mask = suspendCancellableCoroutine<SegmentationMask> { continuation ->
                    selfieSegmenter.process(inputImage)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                        .addOnCanceledListener { continuation.cancel() }
                }

                val maskWidth = mask.width
                val maskHeight = mask.height
                val buffer = mask.buffer

                // 创建灰度遮罩图
                val maskBitmap = createBitmap(bitmap.width, bitmap.height)
                val pixels = IntArray(bitmap.width * bitmap.height)

                // ML Kit 返回的 mask 尺寸可能与原图不同，需要缩放映射
                val scaleX = maskWidth.toFloat() / bitmap.width
                val scaleY = maskHeight.toFloat() / bitmap.height

                for (y in 0 until bitmap.height) {
                    for (x in 0 until bitmap.width) {
                        val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                        val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                        val idx = maskY * maskWidth + maskX
                        val confidence = buffer.get(idx)

                        // 置信度 > 0.5 视为人像区域
                        val isSubject = confidence > 0.5f
                        val value = when (type) {
                            MaskType.SUBJECT -> if (isSubject) 255 else 0
                            MaskType.BACKGROUND -> if (isSubject) 0 else 255
                            MaskType.SKY -> 0 // 天空模式走下方分支
                        }
                        pixels[y * bitmap.width + x] = Color.argb(255, value, value, value)
                    }
                }
                maskBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                buffer.clear()
                MaskResult(maskBitmap, type, bitmap.width, bitmap.height)
            } catch (e: Exception) {
                Log.e("AIMaskManager", "人像分割失败", e)
                null
            }
        }

    /**
     * 生成天空遮罩（启发式算法，纯端侧无需模型）
     *
     * 基于像素颜色特征：天空通常位于图片上半部分，颜色偏蓝/青，饱和度适中。
     * 这是一个简化版实现，后续可升级为 TFLite 天空分割模型。
     */
    suspend fun generateSkyMask(bitmap: Bitmap): MaskResult? = withContext(Dispatchers.Default) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val maskPixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    // 天空启发式条件：
                    // 1. 位于图片上半部分权重更高
                    // 2. 蓝色通道显著高于红色
                    // 3. 亮度足够高（非夜晚）
                    // 4. 饱和度不能过高（排除霓虹灯等）
                    val brightness = (r + g + b) / 3f
                    val blueDominance = b - r
                    val topWeight = 1f - (y.toFloat() / height * 0.6f) // 上半部分权重高
                    val saturation = if (brightness > 0) (maxOf(r, g, b) - minOf(r, g, b)) / brightness else 0f

                    val score = (blueDominance / 255f) * topWeight * (brightness / 255f) * (1f - saturation * 0.3f)
                    val isSky = score > 0.15f && brightness > 60 && b > r + 10

                    val value = if (isSky) 255 else 0
                    maskPixels[y * width + x] = Color.argb(255, value, value, value)
                }
            }

            val maskBitmap = createBitmap(width, height)
            maskBitmap.setPixels(maskPixels, 0, width, 0, 0, width, height)
            MaskResult(maskBitmap, MaskType.SKY, width, height)
        } catch (e: Exception) {
            Log.e("AIMaskManager", "天空分割失败", e)
            null
        }
    }

    /**
     * 将遮罩应用到 Bitmap，仅保留选中区域，其余透明
     *
     * @param source 原图
     * @param maskResult 遮罩结果
     * @return 带 Alpha 的 Bitmap（选中区域保留，未选中区域透明）
     */
    fun applyMaskToBitmap(source: Bitmap, maskResult: MaskResult): Bitmap {
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        // 使用遮罩作为 PorterDuff 模式裁剪
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskResult.maskBitmap, 0f, 0f, paint)
        return result
    }

    /**
     * 生成遮罩预览图（半透明红色叠加，用于 UI 展示）
     */
    fun createMaskPreview(source: Bitmap, maskResult: MaskResult, overlayColor: Int = Color.argb(80, 255, 100, 0)): Bitmap {
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val paint = Paint().apply {
            color = overlayColor
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawBitmap(maskResult.maskBitmap, 0f, 0f, paint)
        return result
    }

    companion object {
        @Volatile
        private var INSTANCE: AIMaskManager? = null

        fun getInstance(context: Context): AIMaskManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIMaskManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
