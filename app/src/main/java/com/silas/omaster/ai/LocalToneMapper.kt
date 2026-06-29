package com.silas.omaster.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 局部色调映射器
 *
 * 使用多尺度局部对比度方法替代全局高光/阴影调整。
 *
 * 算法原理：
 * 1. 将图像分解为基础层（低频）和细节层（高频）
 * 2. 对基础层应用色调压缩（压缩高光、提升阴影）
 * 3. 保持细节层不变（保留局部对比度）
 * 4. 重建图像
 *
 * 相比全局调整的优势：
 * - 局部自适应：不同区域有不同的色调映射
 * - 保留更多细节：不会因全局调整丢失中间调细节
 * - 更自然的HDR效果：避免全局HDR的晕影效应
 */
class LocalToneMapper {

    data class ToneMapParams(
        val shadowBoost: Float = 0.3f,      // 阴影提升强度 (0-1)
        val highlightRecovery: Float = 0.3f, // 高光恢复强度 (0-1)
        val localContrast: Float = 0.5f,     // 局部对比度 (0-1)
        val radius: Int = 8,                  // 基础层模糊半径
        val detailStrength: Float = 1.0f      // 细节增强强度 (0-2)
    )

    /**
     * 应用局部色调映射
     */
    fun applyToneMap(bitmap: Bitmap, params: ToneMapParams = ToneMapParams()): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Step 1: Convert to luminance
        val luminance = computeLuminance(bitmap)

        // Step 2: Compute base layer (gaussian blur of luminance)
        val baseLayer = gaussianBlur(luminance, width, height, params.radius)

        // Step 3: Compute detail layer (luminance - base)
        val detailLayer = computeDetailLayer(luminance, baseLayer)

        // Step 4: Apply tone compression to base layer
        val compressedBase = compressBaseLayer(baseLayer, params)

        // Step 5: Enhance detail layer
        val enhancedDetail = enhanceDetail(detailLayer, params.detailStrength)

        // Step 6: Reconstruct: result = compressedBase + enhancedDetail
        val result = reconstruct(bitmap, compressedBase, enhancedDetail, luminance, params)

        return result
    }

    /**
     * Compute luminance from bitmap using ITU-R BT.601 weights
     */
    private fun computeLuminance(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val luminance = FloatArray(width * height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f
            luminance[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        return luminance
    }

    /**
     * Separable Gaussian blur: horizontal pass then vertical pass
     * Uses a discrete Gaussian kernel approximated by binomial coefficients
     */
    private fun gaussianBlur(data: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        // Compute 1D Gaussian kernel
        val kernelSize = radius * 2 + 1
        val kernel = FloatArray(kernelSize)
        val sigma = radius / 3.0f
        var sum = 0f
        for (i in 0 until kernelSize) {
            val x = i - radius
            kernel[i] = kotlin.math.exp(-(x * x) / (2f * sigma * sigma))
            sum += kernel[i]
        }
        // Normalize kernel
        for (i in 0 until kernelSize) {
            kernel[i] /= sum
        }

        // Horizontal pass
        val temp = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var value = 0f
                for (k in 0 until kernelSize) {
                    val sx = (x + k - radius).coerceIn(0, width - 1)
                    value += data[y * width + sx] * kernel[k]
                }
                temp[y * width + x] = value
            }
        }

        // Vertical pass
        val result = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var value = 0f
                for (k in 0 until kernelSize) {
                    val sy = (y + k - radius).coerceIn(0, height - 1)
                    value += temp[sy * width + x] * kernel[k]
                }
                result[y * width + x] = value
            }
        }

        return result
    }

    /**
     * Detail layer = luminance - base layer
     * Represents high-frequency local contrast information
     */
    private fun computeDetailLayer(luminance: FloatArray, base: FloatArray): FloatArray {
        val detail = FloatArray(luminance.size)
        for (i in luminance.indices) {
            detail[i] = luminance[i] - base[i]
        }
        return detail
    }

    /**
     * Compress the base layer:
     * - Shadows: boosted via gamma < 1 (brightens dark areas)
     * - Highlights: compressed via log-like curve (prevents clipping)
     *
     * The compression is applied based on the luminance level:
     * - Dark regions (below midtone) get a gamma boost for shadow recovery
     * - Bright regions (above midtone) get a log compression for highlight recovery
     */
    private fun compressBaseLayer(base: FloatArray, params: ToneMapParams): FloatArray {
        val compressed = FloatArray(base.size)
        val shadowGamma = 1f - params.shadowBoost * 0.6f  // gamma range: 1.0 → 0.4
        val highlightCompression = params.highlightRecovery

        for (i in base.indices) {
            val v = base[i].coerceIn(0f, 1f)
            val result = if (v <= 0.5f) {
                // Shadow region: apply gamma < 1 to brighten
                // Gamma power brightens dark values
                val gammaCorrected = if (v > 0f) {
                    val normalized = v / 0.5f  // normalize to [0,1] within shadow range
                    val gammaVal = kotlin.math.pow(normalized.toDouble(), shadowGamma.toDouble()).toFloat()
                    gammaVal * 0.5f  // map back to [0, 0.5]
                } else {
                    0f
                }
                // Blend between original and gamma-corrected based on shadowBoost
                v + (gammaCorrected - v) * params.shadowBoost
            } else {
                // Highlight region: apply log-like compression
                // Reinhard-style tone mapping: v / (1 + v) normalized to [0.5, 1.0]
                val normalized = (v - 0.5f) / 0.5f  // normalize to [0,1] within highlight range
                val compressed = normalized / (1f + normalized * highlightCompression * 2f)
                val mapped = compressed * 0.5f + 0.5f  // map back to [0.5, 1.0]
                // Blend between original and compressed based on highlightRecovery
                v + (mapped - v) * highlightCompression
            }
            compressed[i] = result.coerceIn(0f, 1f)
        }
        return compressed
    }

    /**
     * Enhance detail layer by multiplying with strength factor
     * Strength = 1.0 preserves original detail, > 1.0 enhances, < 1.0 suppresses
     */
    private fun enhanceDetail(detail: FloatArray, strength: Float): FloatArray {
        val enhanced = FloatArray(detail.size)
        for (i in detail.indices) {
            enhanced[i] = detail[i] * strength
        }
        return enhanced
    }

    /**
     * Reconstruct the final image:
     * - New luminance = compressedBase + enhancedDetail
     * - Adjust each pixel's color based on the ratio of new luminance to old luminance
     * - This preserves the original color ratios while adjusting overall brightness
     */
    private fun reconstruct(
        bitmap: Bitmap,
        base: FloatArray,
        detail: FloatArray,
        originalLuminance: FloatArray,
        params: ToneMapParams
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val a = Color.alpha(pixel)

            val origLum = originalLuminance[i]
            // New luminance from compressed base + enhanced detail
            val newLum = (base[i] + detail[i]).coerceIn(0f, 1f)

            // Compute scaling ratio: how much to brighten/darken this pixel
            // Add small epsilon to avoid division by zero
            val ratio = if (origLum > 0.001f) {
                newLum / origLum
            } else {
                // For very dark pixels, use a gentle boost based on shadow parameter
                1f + params.shadowBoost * 0.5f
            }

            // Apply ratio to each channel, preserving color ratios
            val newR = (r * ratio).roundToInt().coerceIn(0, 255)
            val newG = (g * ratio).roundToInt().coerceIn(0, 255)
            val newB = (b * ratio).roundToInt().coerceIn(0, 255)

            dstPixels[i] = Color.argb(a, newR, newG, newB)
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
}
