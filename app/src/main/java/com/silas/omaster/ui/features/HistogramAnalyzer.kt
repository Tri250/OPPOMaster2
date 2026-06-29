package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Color

/**
 * 直方图分析器
 *
 * 对齐 PixelFruit HistogramManager.js
 * - RGB/L 四通道直方图统计
 * - 均值亮度计算
 * - 阴影/高光裁剪检测
 */
class HistogramAnalyzer {

    data class HistogramResult(
        val luminance: IntArray,      // 256 levels
        val red: IntArray,            // 256 levels
        val green: IntArray,          // 256 levels
        val blue: IntArray,           // 256 levels
        val meanLuminance: Float,
        val shadowClipping: Boolean,
        val highlightClipping: Boolean
    )

    /**
     * 分析 Bitmap 的直方图
     *
     * @param bitmap 输入图像（会被采样以提升性能）
     * @param sampleStep 采样步长，默认 4（每 4x4 像素采样一个）
     */
    fun analyze(bitmap: Bitmap, sampleStep: Int = 4): HistogramResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val lum = IntArray(256)
        val r = IntArray(256)
        val g = IntArray(256)
        val b = IntArray(256)

        var totalLum = 0L
        var sampledCount = 0
        var shadowPixels = 0
        var highlightPixels = 0

        // 采样统计（提升性能）
        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val i = y * width + x
                if (i >= pixels.size) continue
                val pixel = pixels[i]
                val rv = Color.red(pixel)
                val gv = Color.green(pixel)
                val bv = Color.blue(pixel)
                val lv = (0.299f * rv + 0.587f * gv + 0.114f * bv).toInt()

                r[rv]++
                g[gv]++
                b[bv]++
                lum[lv.coerceIn(0, 255)]++
                totalLum += lv
                sampledCount++

                if (lv < 10) shadowPixels++
                if (lv > 245) highlightPixels++
            }
        }

        val pixelCount = sampledCount.toFloat().coerceAtLeast(1f)
        val meanLum = totalLum / pixelCount
        val shadowClipping = shadowPixels / pixelCount > 0.01f
        val highlightClipping = highlightPixels / pixelCount > 0.01f

        return HistogramResult(lum, r, g, b, meanLum, shadowClipping, highlightClipping)
    }

    /**
     * 快速分析：仅亮度通道
     */
    fun analyzeLuminanceOnly(bitmap: Bitmap, sampleStep: Int = 4): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val lum = IntArray(256)
        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val i = y * width + x
                if (i >= pixels.size) continue
                val pixel = pixels[i]
                val lv = (0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)).toInt()
                lum[lv.coerceIn(0, 255)]++
            }
        }
        return lum
    }

    /**
     * 完整分析：返回 [HistogramFullResult]，供智能优化的示波器叠加层使用。
     *
     * 包含 RGB/L 四通道直方图、平均/中位亮度、动态范围、过曝/欠曝倾向与裁剪检测。
     */
    fun analyzeFull(bitmap: Bitmap, sampleStep: Int = 4): HistogramFullResult {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val lum = IntArray(256)
        val r = IntArray(256)
        val g = IntArray(256)
        val b = IntArray(256)

        var totalLum = 0L
        var sampledCount = 0
        var shadowPixels = 0
        var highlightPixels = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val i = y * width + x
                if (i >= pixels.size) continue
                val pixel = pixels[i]
                val rv = Color.red(pixel)
                val gv = Color.green(pixel)
                val bv = Color.blue(pixel)
                val lv = (0.299f * rv + 0.587f * gv + 0.114f * bv).toInt().coerceIn(0, 255)

                r[rv]++; g[gv]++; b[bv]++; lum[lv]++
                totalLum += lv
                sampledCount++
                if (lv < 10) shadowPixels++
                if (lv > 245) highlightPixels++
            }
        }

        val count = sampledCount.toFloat().coerceAtLeast(1f)
        val meanLum = (totalLum / count) / 255f

        // 中位亮度
        var medianLum = 0.5f
        val half = sampledCount / 2
        var acc = 0
        for (v in 0..255) {
            acc += lum[v]
            if (acc >= half) { medianLum = v / 255f; break }
        }

        // 动态范围（EV）：基于 1%~99% 分位
        val lo = percentile(lum, sampledCount, 0.01) / 255f
        val hi = percentile(lum, sampledCount, 0.99) / 255f
        val dynamicRange = if (hi > lo + 0.001f) {
            (kotlin.math.log((hi / (lo.coerceAtLeast(0.001f))).toDouble()) / kotlin.math.log(2.0)).toFloat()
        } else 0f

        val shadowClipping = shadowPixels / count > 0.01f
        val highlightClipping = highlightPixels / count > 0.01f
        // 正=过曝倾向, 负=欠曝倾向
        val exposureBias = (meanLum - 0.5f) * 4f

        return HistogramFullResult(
            luminance = lum, red = r, green = g, blue = b,
            meanLuminance = meanLum, medianLuminance = medianLum,
            shadowClipping = shadowClipping, highlightClipping = highlightClipping,
            dynamicRange = dynamicRange, exposureBias = exposureBias
        )
    }

    /**
     * 波形监视器：将图像降采样为 [columns] 列，每列统计 256 级亮度的分布。
     */
    fun analyzeWaveform(bitmap: Bitmap, columns: Int = 128): WaveformData {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val scanlines = ArrayList<FloatArray>(columns)
        var maxVal = 1f
        val colWidth = (width.toFloat() / columns).coerceAtLeast(1f).toInt()

        for (c in 0 until columns) {
            val hist = FloatArray(256)
            val xStart = c * colWidth
            val xEnd = (xStart + colWidth).coerceAtMost(width)
            var samples = 0
            var y = 0
            while (y < height) {
                var x = xStart
                while (x < xEnd) {
                    val pixel = pixels[y * width + x]
                    val lv = (0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)).toInt().coerceIn(0, 255)
                    hist[lv]++
                    samples++
                    x++
                }
                y += 4 // 行采样步长
            }
            if (samples > 0) {
                for (i in 0..255) hist[i] /= samples
            }
            val colMax = hist.maxOrNull() ?: 0f
            if (colMax > maxVal) maxVal = colMax
            scanlines.add(hist)
        }

        return WaveformData(scanlines = scanlines, maxValue = maxVal, isParade = false)
    }

    private fun percentile(hist: IntArray, total: Int, p: Float): Int {
        val target = (total * p).toInt()
        var acc = 0
        for (v in 0..255) {
            acc += hist[v]
            if (acc >= target) return v
        }
        return 255
    }
}
