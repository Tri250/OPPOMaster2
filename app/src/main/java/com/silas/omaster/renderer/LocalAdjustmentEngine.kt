package com.silas.omaster.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.hypot

/**
 * 蒙版类型
 */
enum class MaskType {
    BRUSH,      // 画笔蒙版
    RADIAL,     // 径向渐变蒙版
    LINEAR      // 线性渐变蒙版
}

/**
 * 单个局部调整项
 * 记录蒙版形状、位置、以及该蒙版区域内的参数调整
 */
data class LocalAdjustment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val maskType: MaskType,
    val name: String = "局部调整",

    // 蒙版参数
    val centerX: Float = 0.5f,      // 归一化中心X [0,1]
    val centerY: Float = 0.5f,      // 归一化中心Y [0,1]
    val radius: Float = 0.3f,       // 归一化半径 [0,1]
    val feather: Float = 0.5f,      // 羽化程度 [0,1]
    val angle: Float = 0f,          // 角度（度），用于线性渐变

    // 该蒙版区域内的参数调整（与全局参数叠加）
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val clarity: Float = 0f,
    val sharpness: Float = 0f,
    val dehaze: Float = 0f,
    val vignette: Float = 0f
)

/**
 * 局部调整引擎
 * 负责生成蒙版 Bitmap 并将局部调整应用到图像
 *
 * 操作链路：
 * 1. 用户选择蒙版类型（画笔/径向/线性）
 * 2. 在图片上绘制/放置蒙版
 * 3. 调整仅影响该蒙版区域的参数
 * 4. 引擎将蒙版叠加到原图，与全局参数混合
 */
class LocalAdjustmentEngine {

    companion object {
        private const val MASK_BITMAP_SIZE = 512
    }

    /**
     * 生成蒙版灰度图（白色=完全应用，黑色=不应用，灰度=过渡）
     * @param width 目标宽度
     * @param height 目标高度
     * @param adjustment 局部调整参数
     * @return 蒙版 Bitmap（单通道灰度，可当作 Alpha 使用）
     */
    fun generateMaskBitmap(width: Int, height: Int, adjustment: LocalAdjustment): Bitmap {
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(maskBitmap)

        when (adjustment.maskType) {
            MaskType.BRUSH -> {
                // 画笔蒙版：使用Path绘制（这里用圆点序列模拟）
                // 实际使用时应由用户绘制路径存储在 adjustment 中
                // 简化版：在中心绘制一个圆形笔刷示例
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    style = Paint.Style.FILL
                }
                val cx = adjustment.centerX * width
                val cy = adjustment.centerY * height
                val r = adjustment.radius * hypot(width.toFloat(), height.toFloat())
                canvas.drawCircle(cx, cy, r, paint)
            }

            MaskType.RADIAL -> {
                // 径向渐变蒙版：中心白→边缘黑
                val cx = adjustment.centerX * width
                val cy = adjustment.centerY * height
                val maxR = hypot(width.toFloat(), height.toFloat())
                val r = adjustment.radius * maxR
                val featherR = r + adjustment.feather * maxR * 0.5f

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val gradient = RadialGradient(
                    cx, cy, featherR,
                    intArrayOf(
                        android.graphics.Color.WHITE,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, (r / featherR).coerceIn(0f, 1f), 1f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            MaskType.LINEAR -> {
                // 线性渐变蒙版：沿角度方向白→黑
                val angleRad = Math.toRadians(adjustment.angle.toDouble())
                val dx = kotlin.math.cos(angleRad).toFloat()
                val dy = kotlin.math.sin(angleRad).toFloat()

                val cx = adjustment.centerX * width
                val cy = adjustment.centerY * height
                val maxR = hypot(width.toFloat(), height.toFloat())
                val bandWidth = adjustment.radius * maxR
                val featherWidth = bandWidth + adjustment.feather * maxR * 0.5f

                // 垂直于渐变方向的向量
                val perpX = -dy
                val perpY = dx

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                // 使用线性渐变，旋转到指定角度
                val gradient = android.graphics.LinearGradient(
                    cx - perpX * featherWidth,
                    cy - perpY * featherWidth,
                    cx + perpX * featherWidth,
                    cy + perpY * featherWidth,
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.WHITE,
                        android.graphics.Color.TRANSPARENT
                    ),
                    floatArrayOf(
                        0f,
                        (featherWidth - bandWidth) / (2 * featherWidth).coerceAtLeast(0.001f),
                        (featherWidth + bandWidth) / (2 * featherWidth).coerceAtLeast(0.001f),
                        1f
                    ),
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        return maskBitmap
    }

    /**
     * 将局部调整应用到 Bitmap（CPU 路径）
     * @param bitmap 当前工作图（会被修改）
     * @param adjustment 局部调整项
     * @return 处理后的 Bitmap（可能是新对象或原对象）
     */
    fun applyLocalAdjustment(bitmap: Bitmap, adjustment: LocalAdjustment): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 生成蒙版
        val maskBitmap = generateMaskBitmap(width, height, adjustment)
        val maskPixels = IntArray(width * height)
        maskBitmap.getPixels(maskPixels, 0, width, 0, 0, width, height)
        maskBitmap.recycle()

        // 读取像素
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 归一化局部参数
        val localExposure = adjustment.exposure / 100f
        val localBrightness = adjustment.brightness / 100f
        val localContrast = adjustment.contrast / 100f
        val localSaturation = adjustment.saturation / 100f
        val localWarmth = adjustment.warmth / 100f
        val localTint = adjustment.tint / 100f
        val localHighlights = adjustment.highlights / 100f
        val localShadows = adjustment.shadows / 100f
        val localClarity = adjustment.clarity / 100f
        val localDehaze = adjustment.dehaze / 100f

        for (i in pixels.indices) {
            val maskAlpha = (maskPixels[i] ushr 24) and 0xFF
            if (maskAlpha == 0) continue

            val mask = maskAlpha / 255f
            val pixel = pixels[i]
            val a = (pixel ushr 24) and 0xFF
            var r = ((pixel ushr 16) and 0xFF) / 255f
            var g = ((pixel ushr 8) and 0xFF) / 255f
            var b = (pixel and 0xFF) / 255f

            // 曝光
            if (kotlin.math.abs(localExposure) > 0.01f) {
                val factor = kotlin.math.pow(2.0, localExposure.toDouble()).toFloat()
                r *= factor; g *= factor; b *= factor
            }

            // 亮度
            if (kotlin.math.abs(localBrightness) > 0.01f) {
                val offset = localBrightness * 0.5f
                r += offset; g += offset; b += offset
            }

            // 对比度
            if (kotlin.math.abs(localContrast) > 0.01f) {
                val factor = 1f + localContrast
                r = 0.5f + (r - 0.5f) * factor
                g = 0.5f + (g - 0.5f) * factor
                b = 0.5f + (b - 0.5f) * factor
            }

            // 饱和度
            if (kotlin.math.abs(localSaturation) > 0.01f) {
                val hsl = rgb2hsl(r, g, b)
                hsl[1] = (hsl[1] + localSaturation).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // 色温
            if (kotlin.math.abs(localWarmth) > 0.01f) {
                r += localWarmth * 0.1f * mask
                b -= localWarmth * 0.1f * mask
            }

            // 色调（Tint）
            if (kotlin.math.abs(localTint) > 0.01f) {
                g += localTint * 0.1f * mask
                b -= localTint * 0.1f * mask
            }

            // 高光
            if (kotlin.math.abs(localHighlights) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val highlightMask = smoothstep(0.5f, 1.0f, lum) * mask
                val adjR = r * (1f + localHighlights * highlightMask)
                val adjG = g * (1f + localHighlights * highlightMask)
                val adjB = b * (1f + localHighlights * highlightMask)
                r = mix(r, adjR, highlightMask)
                g = mix(g, adjG, highlightMask)
                b = mix(b, adjB, highlightMask)
            }

            // 阴影
            if (kotlin.math.abs(localShadows) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val shadowMask = smoothstep(0.5f, 0.0f, lum) * mask
                val adjR = r + localShadows * shadowMask * 0.3f
                val adjG = g + localShadows * shadowMask * 0.3f
                val adjB = b + localShadows * shadowMask * 0.3f
                r = mix(r, adjR, shadowMask)
                g = mix(g, adjG, shadowMask)
                b = mix(b, adjB, shadowMask)
            }

            // 清晰度
            if (localClarity > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val adaptiveStrength = localClarity * (1f - kotlin.math.abs(lum - 0.5f) * 0.5f) * mask
                val newR = 0.5f + (r - 0.5f) * (1f + adaptiveStrength * 2f)
                val newG = 0.5f + (g - 0.5f) * (1f + adaptiveStrength * 2f)
                val newB = 0.5f + (b - 0.5f) * (1f + adaptiveStrength * 2f)
                r = mix(r, newR, mask)
                g = mix(g, newG, mask)
                b = mix(b, newB, mask)
            }

            // 去霾
            if (localDehaze > 0.01f) {
                val hsl = rgb2hsl(r, g, b)
                val fogLevel = hsl[2] * (1f - hsl[1])
                val ds = localDehaze * fogLevel * mask
                r = 0.5f + (r - 0.5f) * (1f + ds)
                g = 0.5f + (g - 0.5f) * (1f + ds)
                b = 0.5f + (b - 0.5f) * (1f + ds)
            }

            // 钳制并写回
            pixels[i] = (a shl 24) or
                    ((r.coerceIn(0f, 1f) * 255f).toInt() shl 16) or
                    ((g.coerceIn(0f, 1f) * 255f).toInt() shl 8) or
                    (b.coerceIn(0f, 1f) * 255f).toInt()
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 应用所有局部调整（按顺序叠加）
     */
    fun applyAllLocalAdjustments(bitmap: Bitmap, adjustments: List<LocalAdjustment>): Bitmap {
        var result = bitmap
        for (adj in adjustments) {
            result = applyLocalAdjustment(result, adj)
        }
        return result
    }

    // ==================== 辅助函数（与 CPURenderer 一致）====================

    private fun rgb2hsl(r: Float, g: Float, b: Float): FloatArray {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h = 0f
        var s = 0f
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

    private fun hsl2rgb(h: Float, s: Float, l: Float): FloatArray {
        if (s < 0.0001f) return floatArrayOf(l, l, l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        return floatArrayOf(hue2rgb(p, q, h + 1f / 3f), hue2rgb(p, q, h), hue2rgb(p, q, h - 1f / 3f))
    }

    private fun hue2rgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 1f / 2f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
