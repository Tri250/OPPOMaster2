package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SoftLightMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 哈苏色彩科学处理引擎
 *
 * 纯函数集合，不依赖 Compose Runtime。
 * 所有耗时操作应在 Dispatchers.Default 中执行（调用方保证）。
 * 输入 Bitmap 不会被修改，始终返回新的 Bitmap。
 *
 * 处理管线：
 * 1. 必要时按质量缩放，降低内存压力
 * 2. ColorMatrix 基础调整（饱和度、对比度、色温、影调、青品调、黑白）
 * 3. 高光 / 阴影 / 鲜艳度（逐像素单通道）
 * 4. 清晰度（Unsharp Mask，中等半径）
 * 5. 柔光效果（高斯模糊叠加）
 * 6. 锐度（Unsharp Mask，小半径）
 * 7. 暗角（径向渐变叠加）
 * 8. 胶片颗粒（噪声 Overlay 混合）
 * 9. 哈苏水印
 */

/**
 * 主入口：对 [source] 应用完整的哈苏色彩科学处理流程。
 *
 * @param source 原图，不会被修改
 * @param hasselbladParams 哈苏大师参数（核心参数，范围 -30 ~ +30）
 * @param colorModeParams 色彩模式增量参数（范围 -100 ~ +100），会与 [hasselbladParams] 叠加
 * @param exportQuality JPEG 导出质量 0-100，默认 95；也作为大图片处理尺寸的提示
 * @return 处理后的新 Bitmap
 */
fun applyHasselbladColorEngine(
    source: Bitmap,
    hasselbladParams: HasselbladParams,
    colorModeParams: Map<String, Int> = emptyMap(),
    exportQuality: Int = HasselbladColorEngine.DEFAULT_EXPORT_QUALITY
): Bitmap = HasselbladColorEngine.apply(source, hasselbladParams, colorModeParams, exportQuality)

/**
 * 在 [bitmap] 左下角添加 "HNCS 3.0 · Hasselblad Natural Color" 水印。
 * 输入不会被修改，返回新 Bitmap。
 */
fun addHasselbladWatermark(bitmap: Bitmap): Bitmap =
    HasselbladColorEngine.addHasselbladWatermark(bitmap)

/**
 * 对 [bitmap] 应用哈苏暗角效果。
 * 输入不会被修改，返回新 Bitmap。
 */
fun applyHasselbladColorEngineVignette(bitmap: Bitmap, vignette: Int): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    HasselbladColorEngine.applyVignette(result, vignette.toFloat())
    return result
}

/**
 * 将 [bitmap] 按比例 [ratio]（宽/高）居中裁剪。
 * 输入不会被修改，返回新 Bitmap。
 */
fun cropToAspectRatio(bitmap: Bitmap, ratio: Float): Bitmap =
    HasselbladColorEngine.cropToAspectRatio(bitmap, ratio)

/**
 * 获取 [bitmap] 的主色调。
 * 返回 ARGB 颜色值 Int。
 */
fun getDominantColor(bitmap: Bitmap): Int = HasselbladColorEngine.getDominantColor(bitmap)

object HasselbladColorEngine {

    const val DEFAULT_EXPORT_QUALITY = 95
    const val MAX_PROCESS_DIMENSION_HIGH = 4096
    const val MAX_PROCESS_DIMENSION_MEDIUM = 3072
    const val MAX_PROCESS_DIMENSION_LOW = 2048

    private const val WATERMARK_TEXT = "HNCS 3.0 · Hasselblad Natural Color"

    fun apply(
        source: Bitmap,
        hasselbladParams: HasselbladParams,
        colorModeParams: Map<String, Int> = emptyMap(),
        exportQuality: Int = DEFAULT_EXPORT_QUALITY
    ): Bitmap {
        require(exportQuality in 0..100) { "exportQuality must be in 0..100" }

        val params = mergeParams(hasselbladParams, colorModeParams)
        var working = prepareSource(source, exportQuality)

        // 1. 基础 ColorMatrix（饱和度、对比度、色温、影调、青品调、黑白）
        working = applyColorMatrix(working, buildColorMatrix(params))

        // 2. 高光 / 阴影 / 鲜艳度（逐像素单通道）
        applyHighlightsShadowsAndVibrance(working, params.highlights, params.shadows, params.vibrance.toFloat())

        // 3. 清晰度（中等半径 Unsharp Mask）
        if (params.clarity > 0.005f) {
            val radius = (3f + params.clarity * 5f).roundToInt().coerceIn(3, 8)
            val amount = params.clarity * 0.8f
            applyUnsharpMask(working, radius, amount)
        }

        // 4. 柔光
        if (params.softLight != SoftLightMode.NONE) {
            applySoftLight(working, params.softLight)
        }

        // 5. 锐度（小半径 Unsharp Mask）
        if (params.sharpness > 0.005f) {
            applyUnsharpMask(working, radius = 1, amount = params.sharpness * 1.2f)
        }

        // 6. 暗角
        if (params.vignette > 0.005f) {
            applyVignette(working, params.vignette)
        }

        // 7. 胶片颗粒
        if (params.grain > 0) {
            applyGrain(working, params.grain.toFloat())
        }

        // 8. 水印
        drawHasselbladWatermark(working)

        return working
    }

    fun addHasselbladWatermark(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        drawHasselbladWatermark(result)
        return result
    }

    fun cropToAspectRatio(bitmap: Bitmap, ratio: Float): Bitmap {
        require(ratio > 0f) { "ratio must be positive" }

        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (x, y, width, height) = when {
            currentRatio > ratio -> {
                val newWidth = (bitmap.height * ratio).roundToInt()
                val xOffset = (bitmap.width - newWidth) / 2
                Quad(xOffset, 0, newWidth, bitmap.height)
            }
            currentRatio < ratio -> {
                val newHeight = (bitmap.width / ratio).roundToInt()
                val yOffset = (bitmap.height - newHeight) / 2
                Quad(0, yOffset, bitmap.width, newHeight)
            }
            else -> return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val cropped = Bitmap.createBitmap(bitmap, x, y, width, height)
        return cropped.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun getDominantColor(bitmap: Bitmap): Int {
        val targetSize = 64
        val scaled = if (bitmap.width == targetSize && bitmap.height == targetSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        }

        val pixels = IntArray(targetSize * targetSize)
        scaled.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
        if (scaled !== bitmap) scaled.recycle()

        val bucketSize = 24
        val buckets = HashMap<Int, Int>()

        for (color in pixels) {
            val r = (color.red / bucketSize) * bucketSize
            val g = (color.green / bucketSize) * bucketSize
            val b = (color.blue / bucketSize) * bucketSize
            val key = (r shl 16) or (g shl 8) or b
            buckets[key] = (buckets[key] ?: 0) + 1
        }

        val dominant = buckets.maxByOrNull { it.value }?.key ?: return Color.GRAY
        val r = (dominant ushr 16) and 0xFF
        val g = (dominant ushr 8) and 0xFF
        val b = dominant and 0xFF
        return Color.rgb(r, g, b)
    }

    // ==================== 内部参数模型 ====================

    private data class ProcessedParams(
        val saturation: Float,
        val contrast: Float,
        val colorTemp: Float,
        val tone: Float,
        val clarity: Float,
        val cyanMagenta: Float,
        val highlights: Float,
        val shadows: Float,
        val sharpness: Float,
        val vignette: Float,
        val vibrance: Float,
        val grain: Float,
        val softLight: SoftLightMode
    )

    private data class Quad(val x: Int, val y: Int, val width: Int, val height: Int)

    private fun mergeParams(hp: HasselbladParams, cmp: Map<String, Int>): ProcessedParams {
        // HasselbladParams 原始范围为 -30 ~ +30（清晰度/锐度/暗角为 0 ~ +30），
        // colorModeParams 来自 UI 滑块，范围为 -100 ~ +100。
        // 两者先归一化到 -1 ~ 1（或 0 ~ 1）再累加，最后钳制。

        val saturation = normalizeSigned(hp.saturation, 30) + normalizeSigned(cmp["saturation"] ?: 0, 100)
        val contrast = normalizeSigned(hp.contrast, 30) + normalizeSigned(cmp["contrast"] ?: 0, 100)
        val colorTemp = normalizeSigned(hp.colorTemp, 30) + normalizeSigned(cmp["warmth"] ?: 0, 100)
        val tone = normalizeSigned(hp.tone, 30) + normalizeSigned(cmp["tone"] ?: 0, 100)
        val clarity = normalizeUnsigned(hp.clarity, 30) + normalizeUnsigned(cmp["clarity"] ?: 0, 100)
        val cyanMagenta = normalizeSigned(hp.cyanMagenta, 30) + normalizeSigned(cmp["cyanMagenta"] ?: 0, 100)
        val highlights = normalizeSigned(hp.highlights, 30) + normalizeSigned(cmp["highlights"] ?: 0, 100)
        val shadows = normalizeSigned(hp.shadows, 30) + normalizeSigned(cmp["shadows"] ?: 0, 100)
        val sharpness = normalizeUnsigned(hp.sharpness, 30) + normalizeUnsigned(cmp["sharpness"] ?: 0, 100)
        val vignette = normalizeUnsigned(hp.vignette, 30) + normalizeUnsigned(cmp["vignette"] ?: 0, 100)

        return ProcessedParams(
            saturation = saturation.coerceIn(-1f, 1f),
            contrast = contrast.coerceIn(-1f, 1f),
            colorTemp = colorTemp.coerceIn(-1f, 1f),
            tone = tone.coerceIn(-1f, 1f),
            clarity = clarity.coerceIn(0f, 1f),
            cyanMagenta = cyanMagenta.coerceIn(-1f, 1f),
            highlights = highlights.coerceIn(-1f, 1f),
            shadows = shadows.coerceIn(-1f, 1f),
            sharpness = sharpness.coerceIn(0f, 1f),
            vignette = vignette.coerceIn(0f, 1f),
            vibrance = normalizeUnsigned(cmp["vibrance"] ?: 0, 100).coerceIn(0f, 1f),
            grain = normalizeUnsigned(cmp["grain"] ?: 0, 100).coerceIn(0f, 1f),
            softLight = hp.softLight
        )
    }

    private fun normalizeSigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(-1f, 1f)

    private fun normalizeUnsigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    // ==================== 图像预处理 ====================

    private fun prepareSource(source: Bitmap, exportQuality: Int): Bitmap {
        val maxDimension = maxProcessDimension(exportQuality)
        val maxDim = max(source.width, source.height)

        return if (maxDim > maxDimension) {
            val scale = maxDimension.toFloat() / maxDim.toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).roundToInt(),
                (source.height * scale).roundToInt(),
                true
            )
        } else if (source.config == Bitmap.Config.ARGB_8888 && source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun maxProcessDimension(quality: Int): Int = when {
        quality >= 90 -> MAX_PROCESS_DIMENSION_HIGH
        quality >= 70 -> MAX_PROCESS_DIMENSION_MEDIUM
        else -> MAX_PROCESS_DIMENSION_LOW
    }

    // ==================== 色彩矩阵 ====================

    private fun buildColorMatrix(params: ProcessedParams): ColorMatrix {
        val matrix = ColorMatrix()

        if (params.saturation <= -0.95f) {
            // 接近 -100 饱和度：执行基于亮度的专业黑白转换
            val bwMatrix = ColorMatrix(floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.set(bwMatrix)
        } else {
            matrix.setSaturation(1f + params.saturation)
        }

        val contrastValue = 1f + params.contrast
        val postMatrix = ColorMatrix(floatArrayOf(
            // R: 对比度 + 影调偏移 + 青品调（正值偏品，负值偏青）
            contrastValue, 0f, 0f, 0f, params.tone * 25f + params.cyanMagenta * 25f,
            // G: 对比度 + 影调偏移 - 青品调
            0f, contrastValue, 0f, 0f, params.tone * 10f - params.cyanMagenta * 20f,
            // B: 对比度 + 色温（正值减蓝偏暖，负值加蓝偏冷）+ 青品调
            0f, 0f, contrastValue, 0f, -params.colorTemp * 15f + params.cyanMagenta * 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.setConcat(matrix, postMatrix)

        return matrix
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val output = if (source.config == Bitmap.Config.ARGB_8888 && source.isMutable) {
            source
        } else {
            Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    // ==================== 高光 / 阴影 / 鲜艳度 ====================

    private fun applyHighlightsShadowsAndVibrance(
        bitmap: Bitmap,
        highlights: Float,
        shadows: Float,
        vibrance: Float
    ) {
        if (highlights == 0f && shadows == 0f && vibrance <= 0f) return
        if (!bitmap.isMutable) return

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            var r = color.red.toFloat()
            var g = color.green.toFloat()
            var b = color.blue.toFloat()

            // 高光 / 阴影：以 128 为界分别提亮或压暗
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b
            when {
                luminance > 128f && highlights != 0f -> {
                    val t = (luminance - 128f) / 127f
                    val factor = 1f + highlights * t * 0.6f
                    r *= factor
                    g *= factor
                    b *= factor
                }
                luminance < 128f && shadows != 0f -> {
                    val t = (128f - luminance) / 128f
                    val factor = 1f + shadows * t * 0.6f
                    r *= factor
                    g *= factor
                    b *= factor
                }
            }

            // 鲜艳度：只提升低饱和度像素，高饱和像素保持稳定
            if (vibrance > 0f) {
                val avg = (r + g + b) / 3f
                val maxDelta = maxOf(abs(r - avg), abs(g - avg), abs(b - avg))
                val saturation = (maxDelta / 128f).coerceIn(0f, 1f)
                val factor = 1f + vibrance * (1f - saturation) * 1.5f
                r = avg + (r - avg) * factor
                g = avg + (g - avg) * factor
                b = avg + (b - avg) * factor
            }

            pixels[i] = argb(
                color.alpha,
                r.roundToInt().coerceIn(0, 255),
                g.roundToInt().coerceIn(0, 255),
                b.roundToInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    // ==================== Unsharp Mask（清晰度 / 锐度） ====================

    private fun applyUnsharpMask(bitmap: Bitmap, radius: Int, amount: Float) {
        val clampedAmount = amount.coerceIn(0f, 5f)
        if (clampedAmount <= 0.005f || radius <= 0) return

        val blurred = boxBlur(bitmap, radius)
        val width = bitmap.width
        val height = bitmap.height
        val original = IntArray(width * height)
        val blurredPixels = IntArray(width * height)

        bitmap.getPixels(original, 0, width, 0, 0, width, height)
        blurred.getPixels(blurredPixels, 0, width, 0, 0, width, height)
        blurred.recycle()

        for (i in original.indices) {
            val o = original[i]
            val b = blurredPixels[i]
            val r = (o.red + clampedAmount * (o.red - b.red)).roundToInt().coerceIn(0, 255)
            val g = (o.green + clampedAmount * (o.green - b.green)).roundToInt().coerceIn(0, 255)
            val bl = (o.blue + clampedAmount * (o.blue - b.blue)).roundToInt().coerceIn(0, 255)
            original[i] = argb(o.alpha, r, g, bl)
        }

        bitmap.setPixels(original, 0, width, 0, 0, width, height)
    }

    // ==================== 柔光 ====================

    private fun applySoftLight(bitmap: Bitmap, mode: SoftLightMode) {
        if (mode == SoftLightMode.NONE) return

        val radius = when (mode) {
            SoftLightMode.SOFT -> 8
            SoftLightMode.DREAMY -> 16
            else -> return
        }
        val opacity = when (mode) {
            SoftLightMode.SOFT -> 0.30f
            SoftLightMode.DREAMY -> 0.50f
            else -> 0f
        }

        val blurred = boxBlur(bitmap, radius)
        val width = bitmap.width
        val height = bitmap.height
        val base = IntArray(width * height)
        val blend = IntArray(width * height)

        bitmap.getPixels(base, 0, width, 0, 0, width, height)
        blurred.getPixels(blend, 0, width, 0, 0, width, height)
        blurred.recycle()

        for (i in base.indices) {
            val baseColor = base[i]
            val blendColor = blend[i]

            val r = blendSoftLight(baseColor.red, blendColor.red, opacity)
            val g = blendSoftLight(baseColor.green, blendColor.green, opacity)
            var b = blendSoftLight(baseColor.blue, blendColor.blue, opacity)

            // DREAMY 额外暖色偏移
            val finalR = if (mode == SoftLightMode.DREAMY) {
                (r + opacity * 12f).toInt().coerceIn(0, 255)
            } else r
            val finalB = if (mode == SoftLightMode.DREAMY) {
                (b - opacity * 8f).toInt().coerceIn(0, 255)
            } else b

            base[i] = argb(baseColor.alpha, finalR, g, finalB)
        }

        bitmap.setPixels(base, 0, width, 0, 0, width, height)
    }

    private fun blendSoftLight(base: Int, blend: Int, opacity: Float): Int {
        val b = base / 255f
        val s = blend / 255f
        val blended = if (s < 0.5f) {
            b - (1f - 2f * s) * b * (1f - b)
        } else {
            b + (2f * s - 1f) * (sqrt(b) - b)
        }
        val result = base + opacity * (blended * 255f - base)
        return result.roundToInt().coerceIn(0, 255)
    }

    // ==================== 暗角 ====================

    internal fun applyVignette(bitmap: Bitmap, vignette: Float) {
        val canvas = Canvas(bitmap)
        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val radius = maxOf(cx, cy) * 1.15f
        val alpha = (vignette * 200f).toInt().coerceIn(0, 255)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                Color.TRANSPARENT,
                Color.argb(alpha, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }

    // ==================== 胶片颗粒 ====================

    private fun applyGrain(bitmap: Bitmap, grainLevel: Float) {
        if (grainLevel <= 0.005f) return

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val random = Random(System.nanoTime())
        val intensity = grainLevel * 64f

        for (i in pixels.indices) {
            val color = pixels[i]
            val noise = 128 + (random.nextFloat() - 0.5f) * 2f * intensity
            val noiseValue = noise.toInt().coerceIn(0, 255)

            val r = overlayBlend(color.red, noiseValue)
            val g = overlayBlend(color.green, noiseValue)
            val b = overlayBlend(color.blue, noiseValue)

            pixels[i] = argb(color.alpha, r, g, b)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun overlayBlend(base: Int, blend: Int): Int {
        return if (base < 128) {
            (2 * base * blend / 255).coerceIn(0, 255)
        } else {
            (255 - 2 * (255 - base) * (255 - blend) / 255).coerceIn(0, 255)
        }
    }

    // ==================== 水印 ====================

    private fun drawHasselbladWatermark(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val minEdge = min(bitmap.width, bitmap.height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = minEdge * 0.025f
            alpha = 200
            setShadowLayer(3f, 0f, 1f, Color.BLACK)
        }

        val padding = minEdge * 0.03f
        canvas.drawText(WATERMARK_TEXT, padding, bitmap.height - padding, paint)
    }

    // ==================== 快速盒式模糊 ====================

    private fun boxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        require(radius > 0) { "radius must be positive" }

        val width = bitmap.width
        val height = bitmap.height
        val input = IntArray(width * height)
        val temp = IntArray(width * height)

        bitmap.getPixels(input, 0, width, 0, 0, width, height)

        // 水平方向滑动窗口（float 累加，避免整数除法精度损失）
        val windowSize = radius * 2 + 1
        for (y in 0 until height) {
            val rowStart = y * width
            var sumA = 0f
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f

            for (dx in -radius..radius) {
                val px = dx.coerceIn(0, width - 1)
                val c = input[rowStart + px]
                sumA += c.alpha
                sumR += c.red
                sumG += c.green
                sumB += c.blue
            }
            temp[rowStart] = argb(
                (sumA / windowSize).roundToInt(),
                (sumR / windowSize).roundToInt(),
                (sumG / windowSize).roundToInt(),
                (sumB / windowSize).roundToInt()
            )

            for (x in 1 until width) {
                val leftX = (x - radius - 1).coerceIn(0, width - 1)
                val rightX = (x + radius).coerceIn(0, width - 1)
                val leftC = input[rowStart + leftX]
                val rightC = input[rowStart + rightX]

                sumA += rightC.alpha - leftC.alpha
                sumR += rightC.red - leftC.red
                sumG += rightC.green - leftC.green
                sumB += rightC.blue - leftC.blue

                temp[rowStart + x] = argb(
                    (sumA / windowSize).roundToInt(),
                    (sumR / windowSize).roundToInt(),
                    (sumG / windowSize).roundToInt(),
                    (sumB / windowSize).roundToInt()
                )
            }
        }

        val output = IntArray(width * height)

        // 垂直方向滑动窗口（float 累加，避免整数除法精度损失）
        for (x in 0 until width) {
            var sumA = 0f
            var sumR = 0f
            var sumG = 0f
            var sumB = 0f

            for (dy in -radius..radius) {
                val py = dy.coerceIn(0, height - 1)
                val c = temp[py * width + x]
                sumA += c.alpha
                sumR += c.red
                sumG += c.green
                sumB += c.blue
            }
            output[x] = argb(
                (sumA / windowSize).roundToInt(),
                (sumR / windowSize).roundToInt(),
                (sumG / windowSize).roundToInt(),
                (sumB / windowSize).roundToInt()
            )

            for (y in 1 until height) {
                val topY = (y - radius - 1).coerceIn(0, height - 1)
                val bottomY = (y + radius).coerceIn(0, height - 1)
                val topC = temp[topY * width + x]
                val bottomC = temp[bottomY * width + x]

                sumA += bottomC.alpha - topC.alpha
                sumR += bottomC.red - topC.red
                sumG += bottomC.green - topC.green
                sumB += bottomC.blue - topC.blue

                output[y * width + x] = argb(
                    (sumA / windowSize).roundToInt(),
                    (sumR / windowSize).roundToInt(),
                    (sumG / windowSize).roundToInt(),
                    (sumB / windowSize).roundToInt()
                )
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(output, 0, width, 0, 0, width, height)
        }
    }

    // ==================== 颜色工具 ====================

    private inline val Int.alpha: Int get() = (this ushr 24) and 0xFF
    private inline val Int.red: Int get() = (this ushr 16) and 0xFF
    private inline val Int.green: Int get() = (this ushr 8) and 0xFF
    private inline val Int.blue: Int get() = this and 0xFF

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return ((a and 0xFF) shl 24) or
                ((r and 0xFF) shl 16) or
                ((g and 0xFF) shl 8) or
                (b and 0xFF)
    }
}