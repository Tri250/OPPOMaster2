package com.silas.omaster.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 直方图计算器
 *
 * 功能：
 * - RGB 3 通道直方图
 * - 亮度 (Luminance) 直方图
 * - 实时更新（16ms 内完成计算）
 * - 支持点击定位高光/中间调/阴影
 *
 * 用于：
 * - 参数调节预览区叠加显示
 * - 曝光分布判断
 * - 参数可视化（曝光补偿旁显示直方图）
 */
object HistogramCalculator {

    /** 直方图桶数（256 级灰度） */
    const val BUCKET_COUNT = 256

    /**
     * 直方图数据
     * @property red 红色通道直方图 [0..255]
     * @property green 绿色通道直方图
     * @property blue 蓝色通道直方图
     * @property luminance 亮度直方图
     * @property totalPixels 总像素数
     * @property meanBrightness 平均亮度
     * @property stdDevBrightness 亮度标准差
     */
    data class HistogramData(
        val red: IntArray = IntArray(BUCKET_COUNT),
        val green: IntArray = IntArray(BUCKET_COUNT),
        val blue: IntArray = IntArray(BUCKET_COUNT),
        val luminance: IntArray = IntArray(BUCKET_COUNT),
        val totalPixels: Int = 0,
        val meanBrightness: Float = 0f,
        val stdDevBrightness: Float = 0f
    ) {
        /** 归一化直方图（0..1） */
        fun normalized(): NormalizedHistogram {
            val maxVal = maxOf(
                red.maxOrNull() ?: 1,
                green.maxOrNull() ?: 1,
                blue.maxOrNull() ?: 1,
                luminance.maxOrNull() ?: 1
            ).coerceAtLeast(1)

            return NormalizedHistogram(
                red = red.map { it.toFloat() / maxVal }.toFloatArray(),
                green = green.map { it.toFloat() / maxVal }.toFloatArray(),
                blue = blue.map { it.toFloat() / maxVal }.toFloatArray(),
                luminance = luminance.map { it.toFloat() / maxVal }.toFloatArray(),
                totalPixels = totalPixels,
                meanBrightness = meanBrightness,
                stdDevBrightness = stdDevBrightness
            )
        }

        /**
         * 获取指定亮度范围的像素比例
         * @param low 亮度下限 [0..255]
         * @param high 亮度上限 [0..255]
         * @return 像素比例 [0..1]
         */
        fun getLuminanceRatio(low: Int, high: Int): Float {
            if (totalPixels == 0) return 0f
            val count = luminance.sliceArray(low..high).sum()
            return count.toFloat() / totalPixels
        }

        /**
         * 分析曝光分布
         * @return ExposureAnalysis（欠曝/正常/过曝比例）
         */
        fun analyzeExposure(): ExposureAnalysis {
            if (totalPixels == 0) return ExposureAnalysis()

            // 欠曝：< 50
            val underexposed = luminance.sliceArray(0..50).sum()
            // 正常：50..205
            val normal = luminance.sliceArray(51..205).sum()
            // 过曝：> 205
            val overexposed = luminance.sliceArray(206..255).sum()

            return ExposureAnalysis(
                underexposedRatio = underexposed.toFloat() / totalPixels,
                normalRatio = normal.toFloat() / totalPixels,
                overexposedRatio = overexposed.toFloat() / totalPixels,
                meanBrightness = meanBrightness,
                stdDevBrightness = stdDevBrightness
            )
        }

        companion object {
            val EMPTY = HistogramData()
        }
    }

    /**
     * 归一化直方图（用于绘制）
     */
    data class NormalizedHistogram(
        val red: FloatArray,
        val green: FloatArray,
        val blue: FloatArray,
        val luminance: FloatArray,
        val totalPixels: Int,
        val meanBrightness: Float,
        val stdDevBrightness: Float
    )

    /**
     * 曝光分析结果
     */
    data class ExposureAnalysis(
        val underexposedRatio: Float = 0f,   // 欠曝比例 [0..1]
        val normalRatio: Float = 0f,         // 正常比例 [0..1]
        val overexposedRatio: Float = 0f,    // 过曝比例 [0..1]
        val meanBrightness: Float = 0f,      // 平均亮度 [0..255]
        val stdDevBrightness: Float = 0f     // 亮度标准差
    ) {
        /** 判断是否需要调整曝光 */
        val needsExposureAdjustment: Boolean
            get() = underexposedRatio > 0.3f || overexposedRatio > 0.3f

        /** 建议的曝光补偿 */
        val suggestedExposureCompensation: Float
            get() = when {
                underexposedRatio > 0.4f -> (underexposedRatio - 0.3f) * 100f
                overexposedRatio > 0.4f -> -(overexposedRatio - 0.3f) * 100f
                else -> 0f
            }
    }

    /**
     * 计算直方图
     * @param bitmap 输入图像
     * @param sampleRate 采样率（1=全采样，2=每2像素采样，4=每4像素采样）
     * @return HistogramData
     */
    suspend fun calculate(
        bitmap: Bitmap,
        sampleRate: Int = 2
    ): HistogramData = withContext(Dispatchers.Default) {
        if (bitmap.isRecycled) return@withContext HistogramData.EMPTY

        val width = bitmap.width
        val height = bitmap.height
        val step = sampleRate.coerceAtLeast(1)

        val red = IntArray(BUCKET_COUNT)
        val green = IntArray(BUCKET_COUNT)
        val blue = IntArray(BUCKET_COUNT)
        val luminance = IntArray(BUCKET_COUNT)

        var totalLum = 0L
        var totalLumSq = 0L
        var pixelCount = 0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val color = pixels[y * width + x]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                // RGB 直方图
                red[r]++
                green[g]++
                blue[b]++

                // 亮度直方图（使用 Rec.709 系数）
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)
                luminance[lum]++

                // 统计数据
                totalLum += lum
                totalLumSq += lum * lum
                pixelCount++
            }
        }

        val meanBrightness = if (pixelCount > 0) totalLum.toFloat() / pixelCount else 0f
        val variance = if (pixelCount > 0) {
            totalLumSq.toFloat() / pixelCount - meanBrightness * meanBrightness
        } else 0f
        val stdDevBrightness = sqrt(max(0f, variance))

        HistogramData(
            red = red,
            green = green,
            blue = blue,
            luminance = luminance,
            totalPixels = pixelCount,
            meanBrightness = meanBrightness,
            stdDevBrightness = stdDevBrightness
        )
    }

    /**
     * 快速计算（仅亮度直方图，用于实时预览）
     */
    fun calculateLuminanceFast(bitmap: Bitmap): IntArray {
        if (bitmap.isRecycled) return IntArray(BUCKET_COUNT)

        val width = bitmap.width
        val height = bitmap.height
        val luminance = IntArray(BUCKET_COUNT)

        // 4x 采样加速
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val color = bitmap.getPixel(x, y)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)
                luminance[lum]++
            }
        }

        return luminance
    }

    /**
     * 从直方图计算对比度
     */
    fun calculateContrast(histogram: IntArray): Float {
        val total = histogram.sum().toFloat()
        if (total == 0f) return 0f

        // 计算均值
        var mean = 0f
        for (i in histogram.indices) {
            mean += i * histogram[i]
        }
        mean /= total

        // 计算标准差
        var variance = 0f
        for (i in histogram.indices) {
            val diff = i - mean
            variance += diff * diff * histogram[i]
        }
        variance /= total

        // 归一化对比度 [0, 1]
        return sqrt(variance) / 128f
    }

    /**
     * 从直方图计算动态范围
     * @return (黑点, 白点) [0..255]
     */
    fun calculateDynamicRange(histogram: IntArray, threshold: Float = 0.01f): Pair<Int, Int> {
        val total = histogram.sum().toFloat()
        if (total == 0f) return Pair(0, 255)

        val thresholdCount = (total * threshold).toInt()

        // 找黑点（从左向右累积，超过阈值）
        var blackPoint = 0
        var cumulative = 0
        for (i in histogram.indices) {
            cumulative += histogram[i]
            if (cumulative > thresholdCount) {
                blackPoint = i
                break
            }
        }

        // 找白点（从右向左累积，超过阈值）
        var whitePoint = 255
        cumulative = 0
        for (i in histogram.indices.reversed()) {
            cumulative += histogram[i]
            if (cumulative > thresholdCount) {
                whitePoint = i
                break
            }
        }

        return Pair(blackPoint, whitePoint)
    }
}
