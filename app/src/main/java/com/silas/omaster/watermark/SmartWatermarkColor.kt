package com.silas.omaster.watermark

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 智能水印颜色适配系统
 * 
 * 核心功能：
 * - 自动分析水印区域亮度，推荐最佳文字颜色
 * - 白色水印在浅色照片上不可见 → 自动切换黑色
 * - 黑色水印在深色照片上不可见 → 自动切换白色
 * - 中间调场景推荐启用阴影增强可读性
 * 
 * 算法原理：
 * - 采样水印所在区域（占画面 10-20%）
 * - 使用标准亮度公式：L = 0.299R + 0.587G + 0.114B
 * - 亮度 > 0.7 → 亮区，推荐深色文字
 * - 亮度 < 0.3 → 暗区，推荐白色文字
 * - 亮度 0.3-0.7 → 中间调，白色 + 阴影
 */
object SmartWatermarkColor {

    // 亮度阈值常量
    private const val BRIGHT_THRESHOLD = 0.7f    // 亮区阈值
    private const val DARK_THRESHOLD = 0.3f      // 暗区阈值
    private const val SAMPLE_RATIO = 0.2f        // 采样区域占比

    /**
     * 颜色推荐结果
     */
    data class ColorRecommendation(
        val textColor: Int,              // 推荐文字颜色
        val colorName: String,           // 颜色名称（中文）
        val shadowEnabled: Boolean,      // 是否启用阴影
        val shadowBlur: Float,           // 阴影模糊度
        val shadowColor: Int,            // 阴影颜色
        val contrastRatio: Float,        // 对比度比率
        val luminance: Float,            // 区域亮度值
        val toneType: ToneType           // 色调类型
    )

    /**
     * 色调类型枚举
     */
    enum class ToneType {
        BRIGHT,      // 高调（亮区）
        DARK,        // 低调（暗区）
        MIDTONE,     // 中间调
        WARM,        // 暖色调
        COOL,        // 冷色调
        NEUTRAL      // 中性色调
    }

    /**
     * 分析预览图水印区域的亮度，推荐最佳文字颜色
     * 
     * @param bitmap 预览图片
     * @param position 水印位置
     * @return 颜色推荐结果
     */
    fun recommendColor(bitmap: Bitmap, position: WatermarkPosition): ColorRecommendation {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)
        val toneAnalysis = analyzeTone(region)

        // 根据亮度推荐颜色
        val textColor = when {
            avgLuminance > BRIGHT_THRESHOLD -> Color.BLACK
            avgLuminance < DARK_THRESHOLD -> Color.WHITE
            else -> Color.WHITE
        }

        // 颜色名称
        val colorName = if (textColor == Color.BLACK) "黑色" else "白色"

        // 是否启用阴影（中间调场景必须启用）
        val shadowEnabled = avgLuminance >= DARK_THRESHOLD && avgLuminance <= BRIGHT_THRESHOLD

        // 阴影模糊度（亮度越接近中间值，阴影越强）
        val shadowBlur = if (shadowEnabled) {
            val distanceFromMiddle = abs(avgLuminance - 0.5f)
            (6f - distanceFromMiddle * 8f).coerceIn(2f, 8f)
        } else {
            0f
        }

        // 阴影颜色（与文字颜色相反）
        val shadowColor = if (textColor == Color.BLACK) Color.WHITE else Color.BLACK

        // 计算对比度比率
        val contrastRatio = calculateContrastRatio(avgLuminance, textColor)

        // 色调类型
        val toneType = when {
            avgLuminance > BRIGHT_THRESHOLD -> ToneType.BRIGHT
            avgLuminance < DARK_THRESHOLD -> ToneType.DARK
            toneAnalysis.isWarm -> ToneType.WARM
            toneAnalysis.isCool -> ToneType.COOL
            else -> ToneType.MIDTONE
        }

        return ColorRecommendation(
            textColor = textColor,
            colorName = colorName,
            shadowEnabled = shadowEnabled,
            shadowBlur = shadowBlur,
            shadowColor = shadowColor,
            contrastRatio = contrastRatio,
            luminance = avgLuminance,
            toneType = toneType
        )
    }

    /**
     * 快速推荐颜色（仅返回颜色值）
     */
    fun recommendColorFast(bitmap: Bitmap, position: WatermarkPosition): Int {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        return when {
            avgLuminance > BRIGHT_THRESHOLD -> Color.BLACK
            avgLuminance < DARK_THRESHOLD -> Color.WHITE
            else -> Color.WHITE
        }
    }

    /**
     * 推荐是否启用阴影
     */
    fun recommendShadow(bitmap: Bitmap, position: WatermarkPosition): Boolean {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        // 中间调场景建议启用阴影
        return avgLuminance >= DARK_THRESHOLD && avgLuminance <= BRIGHT_THRESHOLD
    }

    /**
     * 推荐阴影参数
     */
    fun recommendShadowParams(bitmap: Bitmap, position: WatermarkPosition): ShadowParams {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        val enabled = avgLuminance >= DARK_THRESHOLD && avgLuminance <= BRIGHT_THRESHOLD
        val blur = if (enabled) {
            val distanceFromMiddle = abs(avgLuminance - 0.5f)
            (6f - distanceFromMiddle * 8f).coerceIn(2f, 8f)
        } else {
            0f
        }

        val color = if (avgLuminance > 0.5f) Color.BLACK else Color.WHITE

        return ShadowParams(
            enabled = enabled,
            blur = blur,
            offsetX = 0f,
            offsetY = 1f,
            color = color
        )
    }

    /**
     * 阴影参数
     */
    data class ShadowParams(
        val enabled: Boolean,
        val blur: Float,
        val offsetX: Float,
        val offsetY: Float,
        val color: Int
    )

    /**
     * 采样水印所在区域（占画面 20%）
     */
    private fun sampleRegion(bitmap: Bitmap, position: WatermarkPosition): IntArray {
        val w = (bitmap.width * SAMPLE_RATIO).toInt()
        val h = (bitmap.height * SAMPLE_RATIO).toInt()

        val (x, y) = when (position) {
            WatermarkPosition.BOTTOM_LEFT -> Pair(0, bitmap.height - h)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(bitmap.width - w, bitmap.height - h)
            WatermarkPosition.BOTTOM -> Pair((bitmap.width - w) / 2, bitmap.height - h)
            WatermarkPosition.TOP_LEFT -> Pair(0, 0)
            WatermarkPosition.TOP_CENTER -> Pair((bitmap.width - w) / 2, 0)
            WatermarkPosition.TOP_RIGHT -> Pair(bitmap.width - w, 0)
            WatermarkPosition.CENTER_LEFT -> Pair(0, (bitmap.height - h) / 2)
            WatermarkPosition.CENTER -> Pair((bitmap.width - w) / 2, (bitmap.height - h) / 2)
            WatermarkPosition.CENTER_RIGHT -> Pair(bitmap.width - w, (bitmap.height - h) / 2)
            WatermarkPosition.CENTER_BOTTOM -> Pair((bitmap.width - w) / 2, bitmap.height - h)
            WatermarkPosition.CUSTOM -> Pair(0, bitmap.height - h) // 默认左下
        }

        // 确保采样区域在图片范围内
        val safeX = x.coerceIn(0, bitmap.width - w)
        val safeY = y.coerceIn(0, bitmap.height - h)
        val safeW = w.coerceAtLeast(1).coerceAtMost(bitmap.width - safeX)
        val safeH = h.coerceAtLeast(1).coerceAtMost(bitmap.height - safeY)

        val pixels = IntArray(safeW * safeH)
        bitmap.getPixels(pixels, 0, safeW, safeX, safeY, safeW, safeH)
        return pixels
    }

    /**
     * 计算亮度平均值
     * 使用标准亮度公式：L = 0.299R + 0.587G + 0.114B
     */
    private fun calculateLuminance(pixels: IntArray): Float {
        if (pixels.isEmpty()) return 0.5f

        var totalLum = 0f
        for (pixel in pixels) {
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f
            totalLum += 0.299f * r + 0.587f * g + 0.114f * b
        }
        return totalLum / pixels.size
    }

    /**
     * 分析色调（暖/冷/中性）
     */
    private fun analyzeTone(pixels: IntArray): ToneAnalysis {
        if (pixels.isEmpty()) return ToneAnalysis(0.5f, 0.5f, 0.5f, false, false, true)

        var totalR = 0f
        var totalG = 0f
        var totalB = 0f

        for (pixel in pixels) {
            totalR += Color.red(pixel) / 255f
            totalG += Color.green(pixel) / 255f
            totalB += Color.blue(pixel) / 255f
        }

        val avgR = totalR / pixels.size
        val avgG = totalG / pixels.size
        val avgB = totalB / pixels.size

        // 判断色调
        val isWarm = avgR > avgB + 0.1f
        val isCool = avgB > avgR + 0.1f
        val isNeutral = !isWarm && !isCool

        return ToneAnalysis(avgR, avgG, avgB, isWarm, isCool, isNeutral)
    }

    /**
     * 色调分析结果
     */
    data class ToneAnalysis(
        val avgR: Float,
        val avgG: Float,
        val avgB: Float,
        val isWarm: Boolean,
        val isCool: Boolean,
        val isNeutral: Boolean
    )

    /**
     * 计算对比度比率
     * WCAG 2.0 标准：最小对比度 4.5:1
     */
    private fun calculateContrastRatio(luminance: Float, textColor: Int): Float {
        val textLuminance = if (textColor == Color.BLACK) 0f else 1f
        val lighter = max(luminance, textLuminance)
        val darker = min(luminance, textLuminance)

        // 对比度公式：(L1 + 0.05) / (L2 + 0.05)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * 分析图片整体色调
     */
    fun analyzeOverallTone(bitmap: Bitmap): OverallToneAnalysis {
        val stepX = max(1, bitmap.width / 50)
        val stepY = max(1, bitmap.height / 50)

        var totalBrightness = 0f
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var sampleCount = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                val brightness = 0.299f * r + 0.587f * g + 0.114f * b

                totalBrightness += brightness
                totalR += r
                totalG += g
                totalB += b
                sampleCount++
            }
        }

        val avgBrightness = if (sampleCount > 0) totalBrightness / sampleCount else 0.5f
        val avgR = if (sampleCount > 0) totalR / sampleCount else 0.5f
        val avgG = if (sampleCount > 0) totalG / sampleCount else 0.5f
        val avgB = if (sampleCount > 0) totalB / sampleCount else 0.5f

        // 判断色调
        val isWarm = avgR > avgB + 0.1f
        val isCool = avgB > avgR + 0.1f
        val isNeutral = !isWarm && !isCool

        // 推荐颜色
        val recommendedColor = when {
            avgBrightness > 0.6f -> Color.BLACK
            avgBrightness < 0.4f -> Color.WHITE
            else -> Color.WHITE
        }

        return OverallToneAnalysis(
            avgBrightness = avgBrightness,
            avgR = avgR,
            avgG = avgG,
            avgB = avgB,
            isWarm = isWarm,
            isCool = isCool,
            isNeutral = isNeutral,
            recommendedColor = recommendedColor,
            recommendedColorName = if (recommendedColor == Color.BLACK) "黑色" else "白色"
        )
    }

    /**
     * 整体色调分析结果
     */
    data class OverallToneAnalysis(
        val avgBrightness: Float,      // 平均亮度 (0-1)
        val avgR: Float,               // 平均红色分量
        val avgG: Float,               // 平均绿色分量
        val avgB: Float,               // 平均蓝色分量
        val isWarm: Boolean,           // 是否暖色调
        val isCool: Boolean,           // 是否冷色调
        val isNeutral: Boolean,        // 是否中性色调
        val recommendedColor: Int,     // 推荐水印颜色
        val recommendedColorName: String // 推荐颜色名称
    )

    /**
     * 获取对比度等级描述
     */
    fun getContrastLevel(contrastRatio: Float): String {
        return when {
            contrastRatio >= 7f -> "优秀 (AAA级)"
            contrastRatio >= 4.5f -> "良好 (AA级)"
            contrastRatio >= 3f -> "一般 (AA-级)"
            else -> "不足 (需调整)"
        }
    }

    /**
     * 检查对比度是否满足可读性要求
     */
    fun isReadable(contrastRatio: Float): Boolean {
        return contrastRatio >= 4.5f // WCAG AA 标准
    }
}