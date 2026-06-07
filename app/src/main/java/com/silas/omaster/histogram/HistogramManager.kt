package com.silas.omaster.histogram

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 直方图分析管理器
 * 参数调节参考
 * 
 * 支持：
 * - RGB 直方图
 * - 亮度直方图
 * - R/G/B 独立通道直方图
 * - 直方图统计信息
 */
class HistogramManager {

    // 直方图数据
    data class HistogramData(
        val red: IntArray = IntArray(256),
        val green: IntArray = IntArray(256),
        val blue: IntArray = IntArray(256),
        val luminance: IntArray = IntArray(256),
        val rgb: IntArray = IntArray(256)
    ) {
        // 归一化到 0-1
        fun normalized(maxHeight: Int = 100): NormalizedHistogram {
            val rMax = red.maxOrNull() ?: 1
            val gMax = green.maxOrNull() ?: 1
            val bMax = blue.maxOrNull() ?: 1
            val lMax = luminance.maxOrNull() ?: 1
            val rgbMax = rgb.maxOrNull() ?: 1

            return NormalizedHistogram(
                red = red.map { it.toFloat() / rMax * maxHeight }.toFloatArray(),
                green = green.map { it.toFloat() / gMax * maxHeight }.toFloatArray(),
                blue = blue.map { it.toFloat() / bMax * maxHeight }.toFloatArray(),
                luminance = luminance.map { it.toFloat() / lMax * maxHeight }.toFloatArray(),
                rgb = rgb.map { it.toFloat() / rgbMax * maxHeight }.toFloatArray()
            )
        }
    }

    // 归一化直方图
    data class NormalizedHistogram(
        val red: FloatArray,
        val green: FloatArray,
        val blue: FloatArray,
        val luminance: FloatArray,
        val rgb: FloatArray
    )

    // 直方图统计信息
    data class HistogramStats(
        val mean: Float,           // 平均值
        val median: Int,           // 中位数
        val mode: Int,             // 众数
        val stdDev: Float,         // 标准差
        val min: Int,              // 最小值
        val max: Int,              // 最大值
        val dynamicRange: Float,   // 动态范围
        val contrast: Float,       // 对比度评估
        val exposure: Float        // 曝光评估
    )

    // 直方图通道
    enum class HistogramChannel(val displayName: String) {
        RGB("RGB"),
        RED("红色"),
        GREEN("绿色"),
        BLUE("蓝色"),
        LUMINANCE("亮度")
    }

    /**
     * 从 Bitmap 计算直方图
     */
    suspend fun calculate(bitmap: Bitmap): HistogramData = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val data = HistogramData()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                data.red[r]++
                data.green[g]++
                data.blue[b]++

                // 亮度 (Rec.709)
                val l = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                data.luminance[l]++

                // RGB 复合
                val avg = (r + g + b) / 3
                data.rgb[avg]++
            }
        }

        data
    }

    /**
     * 计算直方图统计信息
     */
    fun calculateStats(data: HistogramData, channel: HistogramChannel = HistogramChannel.LUMINANCE): HistogramStats {
        val histogram = when (channel) {
            HistogramChannel.RGB -> data.rgb
            HistogramChannel.RED -> data.red
            HistogramChannel.GREEN -> data.green
            HistogramChannel.BLUE -> data.blue
            HistogramChannel.LUMINANCE -> data.luminance
        }

        val total = histogram.sum()
        if (total == 0) {
            return HistogramStats(0f, 128, 128, 0f, 0, 255, 0f, 0f, 0f)
        }

        // 平均值
        var sum = 0.0
        for (i in 0..255) {
            sum += i * histogram[i]
        }
        val mean = (sum / total).toFloat()

        // 中位数
        var cumulative = 0
        var median = 128
        for (i in 0..255) {
            cumulative += histogram[i]
            if (cumulative >= total / 2) {
                median = i
                break
            }
        }

        // 众数
        var mode = 128
        var maxCount = 0
        for (i in 0..255) {
            if (histogram[i] > maxCount) {
                maxCount = histogram[i]
                mode = i
            }
        }

        // 标准差
        var variance = 0.0
        for (i in 0..255) {
            variance += (i - mean) * (i - mean) * histogram[i]
        }
        variance /= total
        val stdDev = sqrt(variance).toFloat()

        // 最小值/最大值（有效范围）
        var min = 0
        for (i in 0..255) {
            if (histogram[i] > 0) {
                min = i
                break
            }
        }
        var max = 255
        for (i in 255 downTo 0) {
            if (histogram[i] > 0) {
                max = i
                break
            }
        }

        // 动态范围
        val dynamicRange = (max - min) / 255f

        // 对比度评估
        val contrast = stdDev / 128f

        // 曝光评估
        val exposure = (mean - 128) / 128f

        return HistogramStats(
            mean = mean,
            median = median,
            mode = mode,
            stdDev = stdDev,
            min = min,
            max = max,
            dynamicRange = dynamicRange,
            contrast = contrast,
            exposure = exposure
        )
    }

    /**
     * 检测曝光问题
     */
    fun detectExposureIssues(data: HistogramData): List<ExposureIssue> {
        val issues = mutableListOf<ExposureIssue>()
        val stats = calculateStats(data, HistogramChannel.LUMINANCE)

        // 过曝检测
        val highlightClipping = data.luminance.sliceArray(240..255).sum()
        val totalPixels = data.luminance.sum()
        if (highlightClipping > totalPixels * 0.01) {
            issues.add(ExposureIssue.Overexposed(
                percentage = highlightClipping.toFloat() / totalPixels * 100,
                severity = if (highlightClipping > totalPixels * 0.05) Severity.High else Severity.Medium
            ))
        }

        // 欠曝检测
        val shadowClipping = data.luminance.sliceArray(0..15).sum()
        if (shadowClipping > totalPixels * 0.01) {
            issues.add(ExposureIssue.Underexposed(
                percentage = shadowClipping.toFloat() / totalPixels * 100,
                severity = if (shadowClipping > totalPixels * 0.05) Severity.High else Severity.Medium
            ))
        }

        // 低对比度检测
        if (stats.contrast < 0.3f) {
            issues.add(ExposureIssue.LowContrast(
                severity = if (stats.contrast < 0.2f) Severity.High else Severity.Medium
            ))
        }

        // 高对比度检测
        if (stats.contrast > 0.8f) {
            issues.add(ExposureIssue.HighContrast(Severity.Low))
        }

        // 偏色检测
        val redStats = calculateStats(data, HistogramChannel.RED)
        val greenStats = calculateStats(data, HistogramChannel.GREEN)
        val blueStats = calculateStats(data, HistogramChannel.BLUE)

        val redShift = redStats.mean - stats.mean
        val greenShift = greenStats.mean - stats.mean
        val blueShift = blueStats.mean - stats.mean

        if (redShift > 15 && blueShift < -15) {
            issues.add(ExposureIssue.ColorCast(
                type = ColorCastType.Warm,
                severity = if (redShift > 30) Severity.High else Severity.Medium
            ))
        } else if (blueShift > 15 && redShift < -15) {
            issues.add(ExposureIssue.ColorCast(
                type = ColorCastType.Cool,
                severity = if (blueShift > 30) Severity.High else Severity.Medium
            ))
        } else if (greenShift > 15) {
            issues.add(ExposureIssue.ColorCast(
                type = ColorCastType.Green,
                severity = Severity.Medium
            ))
        } else if (redShift > 15) {
            issues.add(ExposureIssue.ColorCast(
                type = ColorCastType.Magenta,
                severity = Severity.Medium
            ))
        }

        return issues
    }

    /**
     * 生成直方图均衡化 LUT
     */
    fun generateEqualizationLUT(data: HistogramData): IntArray {
        val lut = IntArray(256)
        val histogram = data.luminance
        val total = histogram.sum()

        if (total == 0) return lut

        // 累积分布函数
        val cdf = IntArray(256)
        cdf[0] = histogram[0]
        for (i in 1..255) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }

        // 找到最小非零 CDF 值
        var cdfMin = 0
        for (i in 0..255) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i]
                break
            }
        }

        // 均衡化
        for (i in 0..255) {
            lut[i] = ((cdf[i] - cdfMin).toFloat() / (total - cdfMin) * 255).toInt().coerceIn(0, 255)
        }

        return lut
    }

    companion object {
        fun getInstance() = HistogramManager()
    }
}

// 曝光问题类型
sealed class ExposureIssue {
    data class Overexposed(val percentage: Float, val severity: Severity) : ExposureIssue()
    data class Underexposed(val percentage: Float, val severity: Severity) : ExposureIssue()
    data class LowContrast(val severity: Severity) : ExposureIssue()
    data class HighContrast(val severity: Severity) : ExposureIssue()
    data class ColorCast(val type: ColorCastType, val severity: Severity) : ExposureIssue()
}

enum class Severity { Low, Medium, High }
enum class ColorCastType { Warm, Cool, Green, Magenta }
