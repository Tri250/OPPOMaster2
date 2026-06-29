package com.silas.omaster.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 色彩范围蒙版引擎
 *
 * 参照 RapidRAW 的 Color Range Mask 功能。
 * RapidRAW 支持：选择特定颜色范围的像素创建蒙版，
 * 结合径向/线性/画笔蒙版使用，精确控制调整范围。
 *
 * 实现：
 * 1. 基于色相（Hue）范围选择
 * 2. 基于亮度（Luminance）范围选择
 * 3. 基于饱和度（Saturation）范围选择
 * 4. 吸管工具：从图片中取色并自动扩展相似范围
 * 5. 蒙版模糊/羽化
 *
 * 操作链路：
 * 1. 用户选择"色彩范围"蒙版类型
 * 2. 吸管取色或手动设定色相/亮度/饱和度范围
 * 3. 引擎计算满足条件的像素蒙版
 * 4. 蒙版叠加到图片上，仅对选中区域应用调整
 */
class ColorRangeMaskEngine {

    /** 色彩范围参数 */
    data class ColorRangeParams(
        val hueCenter: Float = 0f,          // 色相中心 [0, 360]
        val hueRange: Float = 30f,          // 色相范围 [0, 180]
        val luminanceMin: Float = 0f,       // 亮度下限 [0, 1]
        val luminanceMax: Float = 1f,       // 亮度上限 [0, 1]
        val saturationMin: Float = 0f,      // 饱和度下限 [0, 1]
        val saturationMax: Float = 1f,      // 饱和度上限 [0, 1]
        val feather: Float = 0.2f,          // 羽化程度 [0, 1]
        val invert: Boolean = false          // 反转蒙版
    )

    /** 预设色彩范围 */
    enum class PresetColorRange(
        val label: String,
        val hueCenter: Float,
        val hueRange: Float
    ) {
        RED("红色", 0f, 25f),
        ORANGE("橙色", 30f, 20f),
        YELLOW("黄色", 60f, 25f),
        GREEN("绿色", 120f, 35f),
        CYAN("青色", 180f, 25f),
        BLUE("蓝色", 240f, 30f),
        PURPLE("紫色", 300f, 30f),
        SKIN("肤色", 25f, 20f),
        WARM("暖色", 30f, 60f),
        COOL("冷色", 210f, 60f)
    }

    /**
     * 从图片中取色，生成对应的色彩范围参数
     * @param bitmap 源图片
     * @param x 取色点 x
     * @param y 取色点 y
     * @param tolerance 容差 [0, 1]
     */
    fun pickColor(
        bitmap: Bitmap, x: Int, y: Int, tolerance: Float = 0.3f
    ): ColorRangeParams {
        val pixel = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
        val r = Color.red(pixel) / 255f
        val g = Color.green(pixel) / 255f
        val b = Color.blue(pixel) / 255f

        val hsl = rgb2hsl(r, g, b)
        val hueDeg = hsl[0] * 360f

        return ColorRangeParams(
            hueCenter = hueDeg,
            hueRange = tolerance * 180f,
            luminanceMin = max(0f, hsl[2] - tolerance),
            luminanceMax = min(1f, hsl[2] + tolerance),
            saturationMin = max(0f, hsl[1] - tolerance * 0.5f),
            saturationMax = min(1f, hsl[1] + tolerance * 0.5f),
            feather = tolerance * 0.5f
        )
    }

    /**
     * 计算色彩范围蒙版
     * @param bitmap 源图片
     * @param params 色彩范围参数
     * @return 蒙版 Bitmap（灰度，白色=选中，黑色=未选中）
     */
    fun computeMask(bitmap: Bitmap, params: ColorRangeParams): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val mask = FloatArray(w * h)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            val hsl = rgb2hsl(r, g, b)
            val hueDeg = hsl[0] * 360f
            val sat = hsl[1]
            val lum = hsl[2]

            // 计算色相匹配度（考虑色相环的循环性）
            val hueDist = circularDistance(hueDeg, params.hueCenter)
            val hueMatch = if (hueDist <= params.hueRange) {
                1f
            } else if (hueDist <= params.hueRange + params.feather * 180f) {
                1f - (hueDist - params.hueRange) / (params.feather * 180f)
            } else {
                0f
            }

            // 计算亮度匹配度
            val lumMatch = if (lum in params.luminanceMin..params.luminanceMax) {
                1f
            } else {
                val featherLum = params.feather * 0.3f
                if (lum < params.luminanceMin) {
                    ((lum - params.luminanceMin + featherLum) / featherLum).coerceIn(0f, 1f)
                } else {
                    ((params.luminanceMax + featherLum - lum) / featherLum).coerceIn(0f, 1f)
                }
            }

            // 计算饱和度匹配度
            val satMatch = if (sat in params.saturationMin..params.saturationMax) {
                1f
            } else {
                val featherSat = params.feather * 0.3f
                if (sat < params.saturationMin) {
                    ((sat - params.saturationMin + featherSat) / featherSat).coerceIn(0f, 1f)
                } else {
                    ((params.saturationMax + featherSat - sat) / featherSat).coerceIn(0f, 1f)
                }
            }

            // 综合匹配度（色相权重最高）
            var match = hueMatch * 0.6f + lumMatch * 0.25f + satMatch * 0.15f
            if (params.invert) match = 1f - match

            mask[i] = match
        }

        // 转换为灰度 Bitmap（使用 ARGB_8888，alpha 通道存储蒙版值）
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskPixels = IntArray(w * h)
        for (i in mask.indices) {
            val alpha = (mask[i] * 255f).toInt().coerceIn(0, 255)
            maskPixels[i] = (alpha shl 24) or (alpha shl 16) or (alpha shl 8) or alpha
        }
        maskBitmap.setPixels(maskPixels, 0, w, 0, 0, w, h)
        return maskBitmap
    }

    /**
     * 计算蒙版并返回 FloatArray（用于后续与局部调整叠加）
     */
    fun computeMaskFloat(bitmap: Bitmap, params: ColorRangeParams): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val mask = FloatArray(w * h)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            val hsl = rgb2hsl(r, g, b)
            val hueDeg = hsl[0] * 360f

            val hueDist = circularDistance(hueDeg, params.hueCenter)
            val hueMatch = if (hueDist <= params.hueRange) 1f
            else if (hueDist <= params.hueRange + params.feather * 180f)
                1f - (hueDist - params.hueRange) / (params.feather * 180f)
            else 0f

            val lumMatch = if (hsl[2] in params.luminanceMin..params.luminanceMax) 1f else 0f
            val satMatch = if (hsl[1] in params.saturationMin..params.saturationMax) 1f else 0f

            var match = hueMatch * 0.6f + lumMatch * 0.25f + satMatch * 0.15f
            if (params.invert) match = 1f - match
            mask[i] = match
        }

        return mask
    }

    /**
     * 从预设生成参数
     */
    fun paramsFromPreset(preset: PresetColorRange): ColorRangeParams {
        return ColorRangeParams(
            hueCenter = preset.hueCenter,
            hueRange = preset.hueRange,
            feather = 0.15f
        )
    }

    /**
     * 色相环距离（考虑 0°/360° 循环）
     */
    private fun circularDistance(a: Float, b: Float): Float {
        val d = abs(a - b)
        return min(d, 360f - d)
    }

    private fun rgb2hsl(r: Float, g: Float, b: Float): FloatArray {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h = 0f; var s = 0f
        if (delta > 0.0001f) {
            s = if (l < 0.5f) delta / (maxC + minC) else delta / (2f - maxC - minC)
            h = when {
                r >= maxC -> (g - b) / delta
                g >= maxC -> 2f + (b - r) / delta
                else -> 4f + (r - g) / delta
            }
            h /= 6f
            if (h < 0f) h += 1f
        }
        return floatArrayOf(h, s, l)
    }
}
