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
}
