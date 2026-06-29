package com.silas.omaster.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 高级降噪器
 *
 * 升级替代 BoxBlur，采用多遍双边滤波算法：
 * - 空间权重：距离越远权重越低（高斯衰减）
 * - 色彩权重：颜色差异越大权重越低（保留边缘）
 * - 多遍迭代提升降噪效果同时保留细节
 *
 * 相比简单 BoxBlur 的优势：
 * 1. 保留边缘细节（颜色差异大的像素不被混合）
 * 2. 多遍迭代可达到更好的降噪效果
 * 3. 可调节空间sigma和色彩sigma参数
 */
class AdvancedDenoiser {

    data class DenoiseParams(
        val spatialSigma: Float = 2.0f,     // 空间高斯sigma，控制模糊范围
        val colorSigma: Float = 30.0f,       // 色彩高斯sigma，控制边缘保护
        val iterations: Int = 3,             // 迭代次数
        val radius: Int = 3,                 // 滤波半径
        val luminanceOnly: Boolean = false,   // 仅亮度通道降噪（保留色彩噪点特征）
        val strength: Float = 1.0f           // 降噪强度 0-1
    )

    companion object {
        private const val MAX_PROCESSING_SIZE = 1024

        // 预计算的色彩权重查找表大小
        private const val COLOR_LUT_SIZE = 256

        // 色彩差异最大值（RGB三通道差平方和的开方上限）
        private const val COLOR_DIFF_SCALE = 441.0f // sqrt(255^2 * 3)
    }

    /**
     * 降噪处理
     */
    fun denoise(bitmap: Bitmap, params: DenoiseParams = DenoiseParams()): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 对于大图，降采样处理后再上采样
        val shouldDownsample = width > MAX_PROCESSING_SIZE || height > MAX_PROCESSING_SIZE
        val workingBitmap = if (shouldDownsample) {
            val scale = MAX_PROCESSING_SIZE.toFloat() / maxOf(width, height)
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        // 预计算空间核
        val spatialKernel = generateSpatialKernel(params.radius, params.spatialSigma)

        // 预计算色彩权重查找表（避免每次计算 exp）
        val colorWeightLut = precomputeColorWeightLut(params.colorSigma)

        var current = workingBitmap

        // 多遍双边滤波
        for (i in 0 until params.iterations) {
            current = bilateralFilterPass(current, params, spatialKernel, colorWeightLut)
        }

        // 与原图按强度混合
        val result = if (shouldDownsample) {
            val blended = blendWithOriginal(workingBitmap, current, params.strength, params.luminanceOnly)
            // 上采样回原始尺寸
            val upsampled = Bitmap.createScaledBitmap(blended, width, height, true)
            if (blended !== workingBitmap) {
                blended.recycle()
            }
            upsampled
        } else {
            blendWithOriginal(bitmap, current, params.strength, params.luminanceOnly)
        }

        // 清理临时 Bitmap
        if (shouldDownsample && workingBitmap !== bitmap) {
            workingBitmap.recycle()
        }
        // 清理最后一遍迭代产生的 Bitmap（如果不是原图也不是最终结果）
        if (current !== bitmap && current !== result && current !== workingBitmap) {
            current.recycle()
        }

        return result
    }

    /**
     * 单遍双边滤波
     *
     * 对每个像素，在其邻域内计算加权平均：
     * weight = spatialWeight * colorWeight
     * spatialWeight = exp(-dist² / (2 * spatialSigma²))
     * colorWeight = exp(-colorDiff² / (2 * colorSigma²))
     *
     * @param bitmap 输入 Bitmap
     * @param params 降噪参数
     * @param spatialKernel 预计算的空间高斯核
     * @param colorWeightLut 色彩权重查找表
     * @return 滤波后的 Bitmap
     */
    private fun bilateralFilterPass(
        bitmap: Bitmap,
        params: DenoiseParams,
        spatialKernel: Array<FloatArray>,
        colorWeightLut: FloatArray
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height

        // 批量读取所有像素
        val pixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(pixelCount)
        val radius = params.radius.coerceAtMost(5) // 限制最大半径避免过慢

        // 预计算色彩 sigma 的倒数用于查找表索引
        val twoColorSigmaSq = 2.0f * params.colorSigma * params.colorSigma

        for (y in 0 until height) {
            for (x in 0 until width) {
                val centerIdx = y * width + x
                val centerPixel = pixels[centerIdx]
                val cR = Color.red(centerPixel)
                val cG = Color.green(centerPixel)
                val cB = Color.blue(centerPixel)
                val cA = Color.alpha(centerPixel)

                var sumR = 0.0f
                var sumG = 0.0f
                var sumB = 0.0f
                var totalWeight = 0.0f

                // 遍历邻域
                for (ky in -radius..radius) {
                    val ny = (y + ky).coerceIn(0, height - 1)
                    for (kx in -radius..radius) {
                        val nx = (x + kx).coerceIn(0, width - 1)

                        // 空间权重（从预计算核读取）
                        val spatialW = spatialKernel[ky + radius][kx + radius]

                        // 色彩权重
                        val neighborIdx = ny * width + nx
                        val nPixel = pixels[neighborIdx]
                        val nR = Color.red(nPixel)
                        val nG = Color.green(nPixel)
                        val nB = Color.blue(nPixel)

                        val diffR = nR - cR
                        val diffG = nG - cG
                        val diffB = nB - cB
                        val colorDiffSq = diffR * diffR + diffG * diffG + diffB * diffB

                        // 通过查找表获取色彩权重
                        val lutIndex = (colorDiffSq * (COLOR_LUT_SIZE - 1).toFloat() /
                                (COLOR_DIFF_SCALE * COLOR_DIFF_SCALE)).toInt()
                            .coerceIn(0, COLOR_LUT_SIZE - 1)
                        val colorW = colorWeightLut[lutIndex]

                        val weight = spatialW * colorW

                        sumR += nR * weight
                        sumG += nG * weight
                        sumB += nB * weight
                        totalWeight += weight
                    }
                }

                // 归一化
                if (totalWeight > 0f) {
                    resultPixels[centerIdx] = Color.argb(
                        cA,
                        (sumR / totalWeight + 0.5f).toInt().coerceIn(0, 255),
                        (sumG / totalWeight + 0.5f).toInt().coerceIn(0, 255),
                        (sumB / totalWeight + 0.5f).toInt().coerceIn(0, 255)
                    )
                } else {
                    resultPixels[centerIdx] = centerPixel
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 生成空间高斯核
     *
     * 2D 高斯核，大小为 (2*radius+1) x (2*radius+1)
     * kernel[i][j] = exp(-((i-r)² + (j-r)²) / (2*sigma²))
     *
     * @param radius 滤波半径
     * @param sigma 高斯标准差
     * @return 归一化的 2D 高斯核
     */
    private fun generateSpatialKernel(radius: Int, sigma: Float): Array<FloatArray> {
        val size = 2 * radius + 1
        val kernel = Array(size) { FloatArray(size) }
        val twoSigmaSq = 2.0f * sigma * sigma
        var sum = 0.0f

        for (i in 0 until size) {
            for (j in 0 until size) {
                val dx = i - radius
                val dy = j - radius
                val distSq = (dx * dx + dy * dy).toFloat()
                kernel[i][j] = exp(-distSq / twoSigmaSq)
                sum += kernel[i][j]
            }
        }

        // 归一化
        for (i in 0 until size) {
            for (j in 0 until size) {
                kernel[i][j] /= sum
            }
        }

        return kernel
    }

    /**
     * 预计算色彩权重查找表
     *
     * 将色彩差异平方映射到 [0, COLOR_LUT_SIZE-1] 的索引，
     * 存储对应的 exp(-colorDiffSq / (2 * colorSigma²)) 值。
     *
     * @param colorSigma 色彩高斯 sigma
     * @return 查找表数组，索引越大权重越小
     */
    private fun precomputeColorWeightLut(colorSigma: Float): FloatArray {
        val lut = FloatArray(COLOR_LUT_SIZE)
        val twoSigmaSq = 2.0f * colorSigma * colorSigma

        for (i in 0 until COLOR_LUT_SIZE) {
            // 将索引映射回色彩差异平方值
            val colorDiffSq = i.toFloat() / (COLOR_LUT_SIZE - 1).toFloat() *
                    COLOR_DIFF_SCALE * COLOR_DIFF_SCALE
            lut[i] = exp(-colorDiffSq / twoSigmaSq)
        }

        return lut
    }

    /**
     * 与原图混合
     *
     * 支持两种混合模式：
     * - 全通道混合：result = original * (1-strength) + denoised * strength
     * - 仅亮度通道混合：只对亮度分量做插值，保留原始色彩特征
     *
     * @param original 原始 Bitmap
     * @param denoised 降噪后的 Bitmap
     * @param strength 降噪强度 [0, 1]
     * @param luminanceOnly 是否仅亮度通道降噪
     * @return 混合后的 Bitmap
     */
    private fun blendWithOriginal(
        original: Bitmap,
        denoised: Bitmap,
        strength: Float,
        luminanceOnly: Boolean
    ): Bitmap {
        val width = original.width
        val height = original.height
        val pixelCount = width * height

        val origPixels = IntArray(pixelCount)
        val denoisedPixels = IntArray(pixelCount)
        original.getPixels(origPixels, 0, width, 0, 0, width, height)
        denoised.getPixels(denoisedPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(pixelCount)

        if (luminanceOnly) {
            // 仅亮度通道降噪
            for (i in 0 until pixelCount) {
                val op = origPixels[i]
                val dp = denoisedPixels[i]

                val oR = Color.red(op)
                val oG = Color.green(op)
                val oB = Color.blue(op)
                val oA = Color.alpha(op)

                val dR = Color.red(dp)
                val dG = Color.green(dp)
                val dB = Color.blue(dp)

                // 计算原图和降噪图的亮度
                val origLum = 0.299f * oR + 0.587f * oG + 0.114f * oB
                val denoisedLum = 0.299f * dR + 0.587f * dG + 0.114f * dB

                // 混合后的亮度
                val blendedLum = origLum + (denoisedLum - origLum) * strength

                // 计算亮度调整比例
                val lumRatio = if (origLum > 0.5f) {
                    blendedLum / origLum
                } else {
                    // 暗区避免除以零，直接用差值
                    val lumDiff = blendedLum - origLum
                    1f + lumDiff.coerceIn(-128f, 128f) / 255f
                }

                resultPixels[i] = Color.argb(
                    oA,
                    (oR * lumRatio).toInt().coerceIn(0, 255),
                    (oG * lumRatio).toInt().coerceIn(0, 255),
                    (oB * lumRatio).toInt().coerceIn(0, 255)
                )
            }
        } else {
            // 全通道线性混合
            val invStrength = 1f - strength
            for (i in 0 until pixelCount) {
                val op = origPixels[i]
                val dp = denoisedPixels[i]

                val oR = Color.red(op)
                val oG = Color.green(op)
                val oB = Color.blue(op)
                val oA = Color.alpha(op)

                val dR = Color.red(dp)
                val dG = Color.green(dp)
                val dB = Color.blue(dp)

                resultPixels[i] = Color.argb(
                    oA,
                    (oR * invStrength + dR * strength + 0.5f).toInt().coerceIn(0, 255),
                    (oG * invStrength + dG * strength + 0.5f).toInt().coerceIn(0, 255),
                    (oB * invStrength + dB * strength + 0.5f).toInt().coerceIn(0, 255)
                )
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
}
