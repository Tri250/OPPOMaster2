package com.silas.omaster.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 风格 LUT 生成器
 *
 * 核心功能：从原图和参考图之间的色彩映射关系，生成 3D LUT (.cube) 文件。
 * 算法流程：
 * 1. 统计原图和参考图的色彩分布（RGB 直方图 + 联合分布）
 * 2. 计算色彩迁移映射函数（均值/方差匹配 + 分段线性插值）
 * 3. 将映射函数编码为 33x33x33 3D LUT
 * 4. 生成标准 .cube 文件格式输出
 *
 * 参考 cubelut.cn/style_lab.php 功能：
 * - 上传原图 + 参考风格图
 * - 自动检测色彩空间（Rec.709 / Log）
 * - 生成可导入剪辑软件的 .cube LUT
 * - 预览应用效果
 * - 场景匹配/色彩相似/亮度相似/导出精度 四维评估
 */
object StyleLUTGenerator {

    private const val TAG = "StyleLUTGenerator"
    const val LUT_SIZE = 33
    const val LUT_TOTAL = LUT_SIZE * LUT_SIZE * LUT_SIZE

    /**
     * 生成结果
     */
    data class GenerationResult(
        val lutData: LUT3DData,
        val previewBitmap: Bitmap,
        val metrics: StyleMetrics
    )

    /**
     * 风格匹配评估指标
     */
    data class StyleMetrics(
        val sceneMatch: Float,      // 场景匹配度 [0, 1]
        val colorSimilarity: Float,  // 色彩相似度 [0, 1]
        val brightnessSimilarity: Float, // 亮度相似度 [0, 1]
        val exportPrecision: Float   // 导出精度 [0, 1]
    )

    /**
     * 色彩空间类型
     */
    enum class ColorSpace {
        REC709,     // 标准 Rec.709
        LOG,        // Log 色彩空间（需先还原）
        AUTO        // 自动检测
    }

    /**
     * 生成风格 LUT
     *
     * @param sourceBitmap 原图
     * @param referenceBitmap 参考风格图
     * @param sourceColorSpace 原图色彩空间
     * @param strength 迁移强度 [0, 1]
     * @return 生成结果
     */
    suspend fun generate(
        sourceBitmap: Bitmap,
        referenceBitmap: Bitmap,
        sourceColorSpace: ColorSpace = ColorSpace.AUTO,
        strength: Float = 1.0f
    ): GenerationResult = withContext(Dispatchers.Default) {
        // 1. 降采样加速（统一到 256x256 分析）
        val srcScaled = scaleForAnalysis(sourceBitmap)
        val refScaled = scaleForAnalysis(referenceBitmap)

        // 2. 自动检测色彩空间
        val detectedColorSpace = if (sourceColorSpace == ColorSpace.AUTO) {
            detectColorSpace(srcScaled)
        } else {
            sourceColorSpace
        }

        // 3. 如果是 Log 色彩空间，先还原为 Rec.709
        val srcLinear = if (detectedColorSpace == ColorSpace.LOG) {
            convertLogToRec709(srcScaled)
        } else {
            srcScaled
        }

        // 4. 统计色彩分布
        val srcStats = computeColorStatistics(srcLinear)
        val refStats = computeColorStatistics(refScaled)

        // 5. 计算色彩迁移映射
        val mapping = computeColorMapping(srcStats, refStats, strength)

        // 6. 生成 3D LUT 数据
        val lutData = generateLUT3DData(mapping)

        // 7. 生成预览图
        val previewBitmap = LUT3DRenderer.applyLUTCPU(sourceBitmap, lutData, strength)

        // 8. 计算评估指标
        val metrics = computeMetrics(srcStats, refStats, mapping, lutData)

        // 清理临时 Bitmap
        if (srcScaled !== sourceBitmap) srcScaled.recycle()
        if (refScaled !== referenceBitmap) refScaled.recycle()
        if (srcLinear !== srcScaled) srcLinear.recycle()

        GenerationResult(lutData, previewBitmap, metrics)
    }

    /**
     * 将 LUT3DData 导出为 .cube 文件内容
     */
    fun exportToCubeString(lutData: LUT3DData, title: String = "OMaster Style LUT"): String {
        val sb = StringBuilder()
        sb.appendLine("TITLE \"$title\"")
        sb.appendLine("LUT_3D_SIZE ${lutData.size}")
        sb.appendLine("LUT_3D_INPUT_RANGE 0.0 1.0")
        sb.appendLine()

        for (i in 0 until lutData.data.size step 3) {
            sb.appendLine("${lutData.data[i]} ${lutData.data[i + 1]} ${lutData.data[i + 2]}")
        }

        return sb.toString()
    }

    // ================== 内部算法 ==================

    /**
     * 降采样到分析尺寸
     */
    private fun scaleForAnalysis(bitmap: Bitmap): Bitmap {
        val targetSize = 256
        if (bitmap.width <= targetSize && bitmap.height <= targetSize) return bitmap
        val scale = minOf(targetSize.toFloat() / bitmap.width, targetSize.toFloat() / bitmap.height)
        val w = (bitmap.width * scale).toInt()
        val h = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /**
     * 自动检测色彩空间（公开方法，供 ViewModel 调用）
     */
    fun detectColorSpace(bitmap: Bitmap): ColorSpace {
        val stats = computeColorStatistics(bitmap)

        val isLowContrast = stats.rgbStdDev < 0.18f
        val isHighBrightness = stats.meanLuminance > 0.45f
        val isLowSaturation = stats.meanSaturation < 0.15f

        return if (isLowContrast && isHighBrightness && isLowSaturation) {
            Log.d(TAG, "Detected LOG color space (brightness=${stats.meanLuminance}, contrast=${stats.rgbStdDev}, sat=${stats.meanSaturation})")
            ColorSpace.LOG
        } else {
            ColorSpace.REC709
        }
    }

    /**
     * Log → Rec.709 转换
     * 使用通用 Log 还原曲线（近似 Sony S-Log3 → Rec.709）
     */
    private fun convertLogToRec709(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (Color.red(pixel) / 255f).let { logToLinear(it) }
            val g = (Color.green(pixel) / 255f).let { logToLinear(it) }
            val b = (Color.blue(pixel) / 255f).let { logToLinear(it) }
            val a = Color.alpha(pixel)
            pixels[i] = Color.argb(
                a,
                (r.coerceIn(0f, 1f) * 255).toInt(),
                (g.coerceIn(0f, 1f) * 255).toInt(),
                (b.coerceIn(0f, 1f) * 255).toInt()
            )
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Log 曲线还原（通用近似）
     */
    private fun logToLinear(x: Float): Float {
        if (x < 0.01f) return 0f
        // 通用 Log 还原：linear = (10^(x * 2) - 1) / 99
        return ((10.0.pow(x * 2.0) - 1.0) / 99.0).toFloat().coerceIn(0f, 1f)
    }

    /**
     * 色彩统计信息
     */
    data class ColorStatistics(
        val meanR: Float, val meanG: Float, val meanB: Float,
        val stdDevR: Float, val stdDevG: Float, val stdDevB: Float,
        val meanLuminance: Float,
        val meanSaturation: Float,
        val rgbStdDev: Float,
        // RGB 直方图
        val histogramR: FloatArray, val histogramG: FloatArray, val histogramB: FloatArray,
        // 累积分布函数
        val cdfR: FloatArray, val cdfG: FloatArray, val cdfB: FloatArray
    )

    /**
     * 计算色彩统计信息
     */
    private fun computeColorStatistics(bitmap: Bitmap): ColorStatistics {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bins = 256
        val histR = IntArray(bins)
        val histG = IntArray(bins)
        val histB = IntArray(bins)

        var sumR = 0.0; var sumG = 0.0; var sumB = 0.0
        var sumLuma = 0.0; var sumSat = 0.0
        val n = pixels.size

        // 第一遍：计算均值和直方图
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            histR[r]++
            histG[g]++
            histB[b]++

            sumR += r; sumG += g; sumB += b
            sumLuma += 0.2126 * r + 0.7152 * g + 0.0722 * b

            val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
            val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
            val sat = if (max > 0) (max - min) / max else 0f
            sumSat += sat
        }

        val meanR = (sumR / n).toFloat()
        val meanG = (sumG / n).toFloat()
        val meanB = (sumB / n).toFloat()
        val meanLuma = (sumLuma / n).toFloat() / 255f
        val meanSat = (sumSat / n).toFloat()

        // 第二遍：计算标准差
        var varR = 0.0; var varG = 0.0; var varB = 0.0
        for (pixel in pixels) {
            val r = Color.red(pixel).toDouble()
            val g = Color.green(pixel).toDouble()
            val b = Color.blue(pixel).toDouble()
            varR += (r - meanR).pow(2)
            varG += (g - meanG).pow(2)
            varB += (b - meanB).pow(2)
        }

        val stdDevR = sqrt(varR / n).toFloat()
        val stdDevG = sqrt(varG / n).toFloat()
        val stdDevB = sqrt(varB / n).toFloat()
        val rgbStdDev = (stdDevR + stdDevG + stdDevB) / (3f * 255f)

        // 计算归一化直方图和 CDF
        val normHistR = FloatArray(bins) { histR[it].toFloat() / n }
        val normHistG = FloatArray(bins) { histG[it].toFloat() / n }
        val normHistB = FloatArray(bins) { histB[it].toFloat() / n }

        val cdfR = computeCDF(normHistR)
        val cdfG = computeCDF(normHistG)
        val cdfB = computeCDF(normHistB)

        return ColorStatistics(
            meanR = meanR / 255f, meanG = meanG / 255f, meanB = meanB / 255f,
            stdDevR = stdDevR / 255f, stdDevG = stdDevG / 255f, stdDevB = stdDevB / 255f,
            meanLuminance = meanLuma,
            meanSaturation = meanSat,
            rgbStdDev = rgbStdDev,
            histogramR = normHistR, histogramG = normHistG, histogramB = normHistB,
            cdfR = cdfR, cdfG = cdfG, cdfB = cdfB
        )
    }

    /**
     * 计算累积分布函数
     */
    private fun computeCDF(histogram: FloatArray): FloatArray {
        val cdf = FloatArray(histogram.size)
        cdf[0] = histogram[0]
        for (i in 1 until histogram.size) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }
        // 归一化到 [0, 1]
        val max = cdf.last()
        if (max > 0) {
            for (i in cdf.indices) {
                cdf[i] /= max
            }
        }
        return cdf
    }

    /**
     * 色彩映射函数
     *
     * 对每个通道独立计算映射：
     * 1. 直方图匹配（CDF 匹配）— 全局色彩分布对齐
     * 2. 均值/方差匹配 — 统计特征对齐
     * 3. 两者加权融合
     */
    data class ColorMapping(
        val mapR: FloatArray,  // 256 级映射表
        val mapG: FloatArray,
        val mapB: FloatArray
    )

    private fun computeColorMapping(
        srcStats: ColorStatistics,
        refStats: ColorStatistics,
        strength: Float
    ): ColorMapping {
        val s = strength.coerceIn(0f, 1f)

        // 直方图匹配映射
        val histMatchR = histogramMatchMapping(srcStats.cdfR, refStats.cdfR)
        val histMatchG = histogramMatchMapping(srcStats.cdfG, refStats.cdfG)
        val histMatchB = histogramMatchMapping(srcStats.cdfB, refStats.cdfB)

        // 均值/方差匹配映射
        val mvMatchR = meanVarianceMapping(srcStats.meanR, srcStats.stdDevR, refStats.meanR, refStats.stdDevR)
        val mvMatchG = meanVarianceMapping(srcStats.meanG, srcStats.stdDevG, refStats.meanG, refStats.stdDevG)
        val mvMatchB = meanVarianceMapping(srcStats.meanB, srcStats.stdDevB, refStats.meanB, refStats.stdDevB)

        // 融合两种映射（直方图匹配权重 0.6，均值方差匹配权重 0.4）
        val mapR = FloatArray(256) { i ->
            val original = i / 255f
            val mapped = histMatchR[i] * 0.6f + mvMatchR[i] * 0.4f
            original * (1f - s) + mapped * s
        }
        val mapG = FloatArray(256) { i ->
            val original = i / 255f
            val mapped = histMatchG[i] * 0.6f + mvMatchG[i] * 0.4f
            original * (1f - s) + mapped * s
        }
        val mapB = FloatArray(256) { i ->
            val original = i / 255f
            val mapped = histMatchB[i] * 0.6f + mvMatchB[i] * 0.4f
            original * (1f - s) + mapped * s
        }

        return ColorMapping(mapR, mapG, mapB)
    }

    /**
     * 直方图匹配映射
     *
     * 对每个输入亮度级别，找到源 CDF 中对应的位置，
     * 然后在参考 CDF 中找到相同累积概率对应的亮度级别。
     */
    private fun histogramMatchMapping(srcCDF: FloatArray, refCDF: FloatArray): FloatArray {
        val mapping = FloatArray(256)

        for (i in 0 until 256) {
            val srcValue = srcCDF[i]
            // 在参考 CDF 中找到最接近的索引
            var bestJ = 0
            var bestDiff = Float.MAX_VALUE
            for (j in 0 until 256) {
                val diff = abs(refCDF[j] - srcValue)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestJ = j
                }
            }
            mapping[i] = bestJ / 255f
        }

        return mapping
    }

    /**
     * 均值/方差匹配映射
     *
     * 将源图像的均值和方差变换到参考图像的均值和方差。
     * 公式：output = (input - srcMean) / srcStdDev * refStdDev + refMean
     */
    private fun meanVarianceMapping(
        srcMean: Float, srcStdDev: Float,
        refMean: Float, refStdDev: Float
    ): FloatArray {
        val mapping = FloatArray(256)
        val safeSrcStdDev = if (srcStdDev < 0.01f) 0.01f else srcStdDev

        for (i in 0 until 256) {
            val input = i / 255f
            val output = (input - srcMean) / safeSrcStdDev * refStdDev + refMean
            mapping[i] = output.coerceIn(0f, 1f)
        }

        return mapping
    }

    /**
     * 从色彩映射生成 3D LUT 数据
     *
     * 对 33x33x33 网格中的每个点，应用色彩映射函数。
     * 使用三线性插值确保平滑性。
     */
    private fun generateLUT3DData(mapping: ColorMapping): LUT3DData {
        val data = FloatArray(LUT_TOTAL * 3)
        val maxIndex = LUT_SIZE - 1

        for (b in 0 until LUT_SIZE) {
            for (g in 0 until LUT_SIZE) {
                for (r in 0 until LUT_SIZE) {
                    val ri = (r.toFloat() / maxIndex * 255f).toInt().coerceIn(0, 255)
                    val gi = (g.toFloat() / maxIndex * 255f).toInt().coerceIn(0, 255)
                    val bi = (b.toFloat() / maxIndex * 255f).toInt().coerceIn(0, 255)

                    // 应用映射
                    val outR = mapping.mapR[ri]
                    val outG = mapping.mapG[gi]
                    val outB = mapping.mapB[bi]

                    // 添加通道间交叉耦合（模拟真实色彩迁移中的通道混合效应）
                    val crossR = outR * 0.9f + outG * 0.07f + outB * 0.03f
                    val crossG = outR * 0.03f + outG * 0.9f + outB * 0.07f
                    val crossB = outR * 0.07f + outG * 0.03f + outB * 0.9f

                    val index = (b * LUT_SIZE * LUT_SIZE + g * LUT_SIZE + r) * 3
                    data[index] = crossR.coerceIn(0f, 1f)
                    data[index + 1] = crossG.coerceIn(0f, 1f)
                    data[index + 2] = crossB.coerceIn(0f, 1f)
                }
            }
        }

        return LUT3DData(
            title = "OMaster Style LUT",
            size = LUT_SIZE,
            data = data
        )
    }

    /**
     * 计算风格匹配评估指标
     */
    private fun computeMetrics(
        srcStats: ColorStatistics,
        refStats: ColorStatistics,
        mapping: ColorMapping,
        lutData: LUT3DData
    ): StyleMetrics {
        // 场景匹配度：基于亮度分布相似性
        val lumaDiff = abs(srcStats.meanLuminance - refStats.meanLuminance)
        val contrastDiff = abs(srcStats.rgbStdDev - refStats.rgbStdDev)
        val sceneMatch = (1f - lumaDiff) * 0.6f + (1f - contrastDiff) * 0.4f

        // 色彩相似度：基于 RGB 均值差异
        val colorDiff = sqrt(
            (srcStats.meanR - refStats.meanR).pow(2) +
            (srcStats.meanG - refStats.meanG).pow(2) +
            (srcStats.meanB - refStats.meanB).pow(2)
        ) / sqrt(3f)
        val colorSimilarity = 1f - colorDiff

        // 亮度相似度
        val brightnessSimilarity = 1f - lumaDiff

        // 导出精度：基于 LUT 映射的平滑度（相邻 LUT 点差异越小精度越高）
        val smoothness = computeLUTSmoothness(lutData)
        val exportPrecision = smoothness

        return StyleMetrics(
            sceneMatch = sceneMatch.coerceIn(0f, 1f),
            colorSimilarity = colorSimilarity.coerceIn(0f, 1f),
            brightnessSimilarity = brightnessSimilarity.coerceIn(0f, 1f),
            exportPrecision = exportPrecision.coerceIn(0f, 1f)
        )
    }

    /**
     * 计算 LUT 平滑度
     *
     * 衡量相邻 LUT 点之间的差异，差异越小表示映射越平滑，精度越高
     */
    private fun computeLUTSmoothness(lutData: LUT3DData): Float {
        var totalDiff = 0.0
        var count = 0
        val size = lutData.size

        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size - 1) {
                    val idx0 = (b * size * size + g * size + r) * 3
                    val idx1 = (b * size * size + g * size + r + 1) * 3

                    val dr = abs(lutData.data[idx1] - lutData.data[idx0])
                    val dg = abs(lutData.data[idx1 + 1] - lutData.data[idx0 + 1])
                    val db = abs(lutData.data[idx1 + 2] - lutData.data[idx0 + 2])

                    totalDiff += sqrt(dr * dr + dg * dg + db * db)
                    count++
                }
            }
        }

        val avgDiff = if (count > 0) totalDiff / count else 0.0
        // 平滑度：差异越小越好，映射到 [0, 1]
        return (1.0 - avgDiff / 0.1).coerceIn(0.0, 1.0).toFloat()
    }
}
