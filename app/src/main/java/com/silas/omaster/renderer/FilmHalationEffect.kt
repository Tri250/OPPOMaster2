package com.silas.omaster.renderer

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 胶片光晕效应（Halation）
 *
 * 模拟真实胶片中的光晕现象：高光区域会产生红色/暖色光晕溢出，
 * 这是因为强光穿透胶片乳剂层后在基底反射回来形成的。
 *
 * 算法：
 * 1. 提取高光区域（亮度 > 阈值的像素）
 * 2. 将高光区域转换为暖红色（模拟银盐基底反射）
 * 3. 对高光区域进行大半径高斯模糊（模拟光晕扩散）
 * 4. 将模糊后的光晕叠加回原图（Screen 混合模式）
 * 5. 支持强度控制
 */
class FilmHalationEffect {

    data class HalationParams(
        val threshold: Float = 0.7f,       // 高光阈值 (0-1)
        val radius: Int = 15,               // 光晕扩散半径
        val intensity: Float = 0.3f,        // 光晕强度 (0-1)
        val warmth: Float = 0.6f,           // 光晕暖色程度 (0-1), 1=纯红, 0=原色
        val bloomAmount: Float = 0.2f       // 额外泛光 (0-1)
    )

    companion object {
        private const val GLOW_DOWNSAMPLE_THRESHOLD = 1024
        private const val GLOW_SCALE = 0.25f
    }

    /**
     * 应用胶片光晕效果
     */
    fun applyHalation(bitmap: Bitmap, params: HalationParams): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 对于大图，在降采样后的光晕层上处理以提升性能
        val shouldDownsampleGlow = width > GLOW_DOWNSAMPLE_THRESHOLD || height > GLOW_DOWNSAMPLE_THRESHOLD
        val glowWidth = if (shouldDownsampleGlow) max(1, (width * GLOW_SCALE).toInt()) else width
        val glowHeight = if (shouldDownsampleGlow) max(1, (height * GLOW_SCALE).toInt()) else height

        // 降采样原图用于光晕计算
        val workingBitmap = if (shouldDownsampleGlow) {
            Bitmap.createScaledBitmap(bitmap, glowWidth, glowHeight, true)
        } else {
            bitmap
        }

        // Step 1: Extract highlights
        val highlights = extractHighlights(workingBitmap, params.threshold)

        // Step 2: Convert highlights to warm red glow
        val warmGlow = convertToWarmGlow(highlights, params.warmth, glowWidth, glowHeight)

        // Step 3: Apply gaussian blur for diffusion
        val scaledRadius = if (shouldDownsampleGlow) {
            max(1, (params.radius * GLOW_SCALE).toInt())
        } else {
            params.radius
        }
        val blurredGlow = gaussianBlur(warmGlow, glowWidth, glowHeight, scaledRadius)

        // Step 4: Apply bloom (brighter areas get more glow)
        val bloomGlow = applyBloom(blurredGlow, glowWidth, glowHeight, params.bloomAmount)

        // Step 5: Upsample glow back to full resolution if needed
        val fullResGlow = if (shouldDownsampleGlow) {
            upsampleGlow(bloomGlow, glowWidth, glowHeight, width, height)
        } else {
            bloomGlow
        }

        // Step 6: Composite with Screen blend mode
        val result = compositeScreen(bitmap, fullResGlow, width, height, params.intensity)

        // 清理临时 Bitmap
        if (shouldDownsampleGlow && workingBitmap !== bitmap) {
            workingBitmap.recycle()
        }

        return result
    }

    /**
     * 提取高光区域
     *
     * 遍历每个像素，计算亮度值。亮度超过阈值的像素保留其 RGB 值（归一化 0-1），
     * 低于阈值的像素设为 0。
     *
     * @return FloatArray，长度为 width * height * 3，每像素 3 个 float (R,G,B)
     */
    private fun extractHighlights(bitmap: Bitmap, threshold: Float): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val highlights = FloatArray(pixelCount * 3)

        for (i in 0 until pixelCount) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f

            // 计算 ITU-R BT.709 亮度
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b

            if (luminance > threshold) {
                // 柔和过渡：用 smoothstep 做软阈值
                val t = (luminance - threshold) / (1f - threshold).coerceAtLeast(0.01f)
                val softT = t * t * (3f - 2f * t) // smoothstep

                val idx = i * 3
                highlights[idx] = r * softT
                highlights[idx + 1] = g * softT
                highlights[idx + 2] = b * softT
            }
            // 低于阈值的保持 0（FloatArray 默认值）
        }

        return highlights
    }

    /**
     * 将高光转换为暖红色光晕
     *
     * 胶片光晕特征：高光溢出偏红/暖色。
     * warmth=1 时为纯红色光晕，warmth=0 时保留原色。
     *
     * @param warmth 暖色程度 [0, 1]
     */
    private fun convertToWarmGlow(highlights: FloatArray, warmth: Float, width: Int, height: Int): FloatArray {
        val pixelCount = width * height
        val glow = FloatArray(pixelCount * 3)

        // 暖红色目标: R=1.0, G=0.3, B=0.1 (典型的胶片光晕色)
        val warmR = 1.0f
        val warmG = 0.3f
        val warmB = 0.1f

        for (i in 0 until pixelCount) {
            val idx = i * 3
            val hR = highlights[idx]
            val hG = highlights[idx + 1]
            val hB = highlights[idx + 2]

            // 用亮度作为光晕强度
            val luminance = 0.2126f * hR + 0.7152f * hG + 0.0722f * hB

            // 在原色和暖红色之间插值
            glow[idx] = hR * (1f - warmth) + warmR * luminance * warmth
            glow[idx + 1] = hG * (1f - warmth) + warmG * luminance * warmth
            glow[idx + 2] = hB * (1f - warmth) + warmB * luminance * warmth
        }

        return glow
    }

    /**
     * 可分离高斯模糊
     *
     * 分两步执行：先水平方向，再垂直方向。
     * 使用 1D 高斯核，大小为 2*radius+1。
     *
     * @param data 输入数据，FloatArray，每像素 3 个 float (R,G,B)
     * @param width 图像宽度
     * @param height 图像高度
     * @param radius 模糊半径
     * @return 模糊后的 FloatArray
     */
    private fun gaussianBlur(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        if (radius <= 0) return data.copyOf()

        // 生成 1D 高斯核
        val kernelSize = 2 * radius + 1
        val kernel = FloatArray(kernelSize)
        val sigma = radius / 3.0f // 经验值：sigma ≈ radius/3
        val twoSigmaSq = 2.0f * sigma * sigma
        var sum = 0.0f

        for (i in 0 until kernelSize) {
            val x = i - radius
            kernel[i] = exp(-(x * x).toFloat() / twoSigmaSq)
            sum += kernel[i]
        }

        // 归一化
        for (i in 0 until kernelSize) {
            kernel[i] /= sum
        }

        // 水平方向模糊
        val temp = FloatArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0.0f
                var g = 0.0f
                var b = 0.0f

                for (k in 0 until kernelSize) {
                    val sx = (x + k - radius).coerceIn(0, width - 1)
                    val srcIdx = (y * width + sx) * 3
                    val w = kernel[k]
                    r += data[srcIdx] * w
                    g += data[srcIdx + 1] * w
                    b += data[srcIdx + 2] * w
                }

                val dstIdx = (y * width + x) * 3
                temp[dstIdx] = r
                temp[dstIdx + 1] = g
                temp[dstIdx + 2] = b
            }
        }

        // 垂直方向模糊
        val result = FloatArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0.0f
                var g = 0.0f
                var b = 0.0f

                for (k in 0 until kernelSize) {
                    val sy = (y + k - radius).coerceIn(0, height - 1)
                    val srcIdx = (sy * width + x) * 3
                    val w = kernel[k]
                    r += temp[srcIdx] * w
                    g += temp[srcIdx + 1] * w
                    b += temp[srcIdx + 2] * w
                }

                val dstIdx = (y * width + x) * 3
                result[dstIdx] = r
                result[dstIdx + 1] = g
                result[dstIdx + 2] = b
            }
        }

        return result
    }

    /**
     * 泛光增强
     *
     * 对已经模糊的光晕层再做一次亮度增强，越亮的区域增加越多。
     * 使用幂函数曲线：result = value * (1 + amount * value^0.5)
     *
     * @param data 输入光晕数据
     * @param amount 泛光量 [0, 1]
     */
    private fun applyBloom(data: FloatArray, width: Int, height: Int, amount: Float): FloatArray {
        if (amount <= 0f) return data

        val result = FloatArray(data.size)
        val pixelCount = width * height

        for (i in 0 until pixelCount) {
            val idx = i * 3
            val r = data[idx]
            val g = data[idx + 1]
            val b = data[idx + 2]

            // 泛光：亮度越高，增幅越大
            val boost = 1f + amount * 2f
            result[idx] = min(1f, r * boost * (1f + amount * r.pow(0.5f)))
            result[idx + 1] = min(1f, g * boost * (1f + amount * g.pow(0.5f)))
            result[idx + 2] = min(1f, b * boost * (1f + amount * b.pow(0.5f)))
        }

        return result
    }

    /**
     * 将降采样的光晕层上采样到全分辨率
     *
     * 使用双线性插值进行上采样，保证光晕边缘平滑。
     *
     * @param glow 降采样后的光晕数据
     * @param smallWidth 降采样宽度
     * @param smallHeight 降采样高度
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return 全分辨率光晕 FloatArray
     */
    private fun upsampleGlow(
        glow: FloatArray,
        smallWidth: Int,
        smallHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        val result = FloatArray(targetWidth * targetHeight * 3)
        val xRatio = (smallWidth - 1).toFloat() / (targetWidth - 1).coerceAtLeast(1)
        val yRatio = (smallHeight - 1).toFloat() / (targetHeight - 1).coerceAtLeast(1)

        for (y in 0 until targetHeight) {
            val srcYf = y * yRatio
            val srcY0 = srcYf.toInt().coerceIn(0, smallHeight - 1)
            val srcY1 = (srcY0 + 1).coerceIn(0, smallHeight - 1)
            val fy = srcYf - srcY0

            for (x in 0 until targetWidth) {
                val srcXf = x * xRatio
                val srcX0 = srcXf.toInt().coerceIn(0, smallWidth - 1)
                val srcX1 = (srcX0 + 1).coerceIn(0, smallWidth - 1)
                val fx = srcXf - srcX0

                // 四个邻近点的索引
                val i00 = (srcY0 * smallWidth + srcX0) * 3
                val i10 = (srcY0 * smallWidth + srcX1) * 3
                val i01 = (srcY1 * smallWidth + srcX0) * 3
                val i11 = (srcY1 * smallWidth + srcX1) * 3

                // 双线性插值
                val dstIdx = (y * targetWidth + x) * 3
                for (c in 0..2) {
                    val v00 = glow[i00 + c]
                    val v10 = glow[i10 + c]
                    val v01 = glow[i01 + c]
                    val v11 = glow[i11 + c]
                    val top = v00 * (1f - fx) + v10 * fx
                    val bottom = v01 * (1f - fx) + v11 * fx
                    result[dstIdx + c] = top * (1f - fy) + bottom * fy
                }
            }
        }

        return result
    }

    /**
     * Screen 混合模式合成
     *
     * Screen 混合公式：result = 1 - (1 - base) * (1 - glow)
     * 等价于：result = base + glow - base * glow
     *
     * 通过 intensity 参数控制光晕叠加强度：
     * final = base + (screenResult - base) * intensity
     *
     * @param bitmap 原始 Bitmap
     * @param glow 光晕数据，FloatArray，每像素 3 float (R,G,B 0-1)
     * @param width 图像宽度
     * @param height 图像高度
     * @param intensity 光晕强度 [0, 1]
     * @return 合成后的 Bitmap
     */
    private fun compositeScreen(
        bitmap: Bitmap,
        glow: FloatArray,
        width: Int,
        height: Int,
        intensity: Float
    ): Bitmap {
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val resultPixels = IntArray(pixelCount)

        for (i in 0 until pixelCount) {
            val pixel = pixels[i]
            val baseR = Color.red(pixel) / 255f
            val baseG = Color.green(pixel) / 255f
            val baseB = Color.blue(pixel) / 255f
            val alpha = Color.alpha(pixel)

            val idx = i * 3
            val glowR = glow[idx]
            val glowG = glow[idx + 1]
            val glowB = glow[idx + 2]

            // Screen 混合: 1 - (1-base)*(1-glow)
            val screenR = 1f - (1f - baseR) * (1f - glowR)
            val screenG = 1f - (1f - baseG) * (1f - glowG)
            val screenB = 1f - (1f - baseB) * (1f - glowB)

            // 用 intensity 线性混合：base + (screen - base) * intensity
            val finalR = baseR + (screenR - baseR) * intensity
            val finalG = baseG + (screenG - baseG) * intensity
            val finalB = baseB + (screenB - baseB) * intensity

            resultPixels[i] = Color.argb(
                alpha,
                (finalR.coerceIn(0f, 1f) * 255f + 0.5f).toInt(),
                (finalG.coerceIn(0f, 1f) * 255f + 0.5f).toInt(),
                (finalB.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            )
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
}
