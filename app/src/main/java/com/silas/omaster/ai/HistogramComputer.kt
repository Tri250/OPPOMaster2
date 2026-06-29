package com.silas.omaster.ai

import android.graphics.Bitmap

/**
 * 直方图计算器
 *
 * 高效计算图像的四通道直方图
 * 供直方图UI和自动调整算法使用
 */
class HistogramComputer {

    data class Histograms(
        val luminance: IntArray = IntArray(256),
        val red: IntArray = IntArray(256),
        val green: IntArray = IntArray(256),
        val blue: IntArray = IntArray(256)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Histograms) return false
            return luminance.contentEquals(other.luminance) &&
                    red.contentEquals(other.red) &&
                    green.contentEquals(other.green) &&
                    blue.contentEquals(other.blue)
        }

        override fun hashCode(): Int {
            var result = luminance.contentHashCode()
            result = 31 * result + red.contentHashCode()
            result = 31 * result + green.contentHashCode()
            result = 31 * result + blue.contentHashCode()
            return result
        }
    }

    /**
     * 从 Bitmap 计算直方图
     * 使用动态采样步长提高效率
     *
     * Images > 2000px: 6x step
     * Images > 1000px: 4x step
     * Otherwise: 2x step
     */
    fun compute(bitmap: Bitmap): Histograms {
        val step = when {
            bitmap.width > 2000 || bitmap.height > 2000 -> 6
            bitmap.width > 1000 || bitmap.height > 1000 -> 4
            else -> 2
        }
        return computeWithStep(bitmap, step)
    }

    /**
     * 从渲染后的 Bitmap 快速计算（用于实时更新）
     * Always use 4x step for faster computation
     */
    fun computeFast(bitmap: Bitmap): Histograms {
        return computeWithStep(bitmap, 4)
    }

    private fun computeWithStep(bitmap: Bitmap, step: Int): Histograms {
        val histograms = Histograms()
        val width = bitmap.width
        val height = bitmap.height

        // Use batch pixel reading with row arrays for efficiency
        val rowPixels = IntArray(width)

        for (y in 0 until height step step) {
            // Read one row at a time to balance memory and efficiency
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, height - y.coerceAtLeast(0))

            for (x in 0 until width step step) {
                val pixel = rowPixels[x]
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                // ITU-R BT.601 luminance weights
                val l = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

                histograms.red[r]++
                histograms.green[g]++
                histograms.blue[b]++
                histograms.luminance[l]++
            }
        }

        return histograms
    }
}
