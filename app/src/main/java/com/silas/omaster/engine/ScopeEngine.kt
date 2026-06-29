package com.silas.omaster.engine

import android.graphics.Bitmap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 示波器引擎
 *
 * 参照 AlcedoStudio 的 Waveform Scope 和 RapidRAW 的直方图/波形/矢量示波器。
 * AlcedoStudio 提供：Live Waveform scope（亮度波形图）
 * RapidRAW 提供：Histogram / Waveform / Vectorscope
 *
 * 实现三种专业示波器：
 * 1. Waveform（波形图）：水平方向=图片列，垂直方向=亮度级，亮度=像素密度
 * 2. Vectorscope（矢量图）：色相→角度，饱和度→半径
 * 3. Parade（三通道波形）：RGB 三通道并排波形
 *
 * 操作链路：
 * 1. 用户在预览区切换示波器类型
 * 2. 引擎分析当前 Bitmap 生成示波器数据
 * 3. UI 组件根据数据绘制示波器
 */
class ScopeEngine {

    /** 波形图数据 */
    data class WaveformData(
        val red: FloatArray,      // [width] 每列的R亮度分布 [0,1]
        val green: FloatArray,    // [width] 每列的G亮度分布 [0,1]
        val blue: FloatArray,     // [width] 每列的B亮度分布 [0,1]
        val luminance: FloatArray // [width] 每列的亮度分布 [0,1]
    )

    /** 2D 波形图数据（完整分布） */
    data class Waveform2DData(
        val columns: Int,         // 列数（缩放到合理范围）
        val rows: Int,            // 行数（256级）
        val red: Array<FloatArray>,      // [columns][rows] 归一化密度
        val green: Array<FloatArray>,    // [columns][rows]
        val blue: Array<FloatArray>,     // [columns][rows]
        val luminance: Array<FloatArray> // [columns][rows]
    )

    /** 矢量图数据 */
    data class VectorscopeData(
        val points: List<Pair<Float, Float>>,  // (angle, radius) 角度/半径
        val avgHue: Float,                      // 平均色相
        val avgSaturation: Float,               // 平均饱和度
        val skinLine: Float                     // 肤色线角度
    )

    /** Parade 数据 */
    data class ParadeData(
        val red: Waveform2DData,
        val green: Waveform2DData,
        val blue: Waveform2DData
    )

    /**
     * 计算 2D 波形图数据
     * @param bitmap 输入图像
     * @param targetColumns 目标列数（通常=显示宽度）
     */
    fun computeWaveform2D(bitmap: Bitmap, targetColumns: Int = 256): Waveform2DData {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val rows = 256
        val cols = min(targetColumns, w)
        val colStep = w.toFloat() / cols

        val lum = Array(cols) { FloatArray(rows) }
        val r = Array(cols) { FloatArray(rows) }
        val g = Array(cols) { FloatArray(rows) }
        val b = Array(cols) { FloatArray(rows) }

        // 统计每列的亮度分布
        for (col in 0 until cols) {
            val startX = (col * colStep).toInt()
            val endX = min(w, ((col + 1) * colStep).toInt())
            val lumCounts = FloatArray(rows)
            val rCounts = FloatArray(rows)
            val gCounts = FloatArray(rows)
            val bCounts = FloatArray(rows)

            for (y in 0 until h step 2) {  // step 2 加速
                for (x in startX until endX) {
                    val p = pixels[y * w + x]
                    val rv = (p shr 16) and 0xFF
                    val gv = (p shr 8) and 0xFF
                    val bv = p and 0xFF
                    val lv = (0.299f * rv + 0.587f * gv + 0.114f * bv).toInt().coerceIn(0, 255)

                    lumCounts[lv]++
                    rCounts[rv]++
                    gCounts[gv]++
                    bCounts[bv]++
                }
            }

            // 归一化
            val maxLum = lumCounts.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val maxR = rCounts.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val maxG = gCounts.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val maxB = bCounts.maxOrNull()?.coerceAtLeast(1f) ?: 1f

            for (row in 0 until rows) {
                lum[col][row] = lumCounts[row] / maxLum
                r[col][row] = rCounts[row] / maxR
                g[col][row] = gCounts[row] / maxG
                b[col][row] = bCounts[row] / maxB
            }
        }

        return Waveform2DData(cols, rows, r, g, b, lum)
    }

    /**
     * 计算矢量图数据
     * 将像素映射到 Cb-Cr 色度平面（类似视频矢量图）
     */
    fun computeVectorscope(bitmap: Bitmap, maxPoints: Int = 5000): VectorscopeData {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val points = mutableListOf<Pair<Float, Float>>()
        val sampleStep = max(1, sqrt((w * h).toFloat() / maxPoints).toInt())

        var hueSum = 0f
        var satSum = 0f
        var sampleCount = 0

        for (y in 0 until h step sampleStep) {
            for (x in 0 until w step sampleStep) {
                val p = pixels[y * w + x]
                val rv = ((p shr 16) and 0xFF) / 255f
                val gv = ((p shr 8) and 0xFF) / 255f
                val bv = (p and 0xFF) / 255f

                // RGB → YCbCr
                val yy = 0.299f * rv + 0.587f * gv + 0.114f * bv
                val cb = (bv - yy) * 0.565f
                val cr = (rv - yy) * 0.713f

                // 映射为角度和半径
                val angle = atan2(cb, cr)
                val radius = sqrt(cb * cb + cr * cr)

                points.add(Pair(angle, radius.coerceIn(0f, 1f)))

                // 累计统计
                val hsl = rgb2hsl(rv, gv, bv)
                hueSum += hsl[0]
                satSum += hsl[1]
                sampleCount++
            }
        }

        return VectorscopeData(
            points = points,
            avgHue = if (sampleCount > 0) hueSum / sampleCount else 0f,
            avgSaturation = if (sampleCount > 0) satSum / sampleCount else 0f,
            skinLine = 0.174f  // ~33° 肤色线（Rec.709 标准 I 线）
        )
    }

    /**
     * 计算 Parade 示波器数据（RGB 三通道独立波形）
     */
    fun computeParade(bitmap: Bitmap, targetColumns: Int = 85): ParadeData {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val rows = 256
        val cols = min(targetColumns, w)
        val colStep = w.toFloat() / cols

        fun computeChannel(getChannel: (Int) -> Int): Array<FloatArray> {
            val data = Array(cols) { FloatArray(rows) }
            for (col in 0 until cols) {
                val startX = (col * colStep).toInt()
                val endX = min(w, ((col + 1) * colStep).toInt())
                val counts = FloatArray(rows)

                for (y in 0 until h step 2) {
                    for (x in startX until endX) {
                        val p = pixels[y * w + x]
                        counts[getChannel(p).coerceIn(0, 255)]++
                    }
                }
                val maxCount = counts.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                for (row in 0 until rows) {
                    data[col][row] = counts[row] / maxCount
                }
            }
            return data
        }

        return ParadeData(
            red = Waveform2DData(cols, rows, computeChannel { (it shr 16) and 0xFF },
                Array(cols) { FloatArray(rows) }, Array(cols) { FloatArray(rows) }, Array(cols) { FloatArray(rows) }),
            green = Waveform2DData(cols, rows, Array(cols) { FloatArray(rows) },
                computeChannel { (it shr 8) and 0xFF }, Array(cols) { FloatArray(rows) }, Array(cols) { FloatArray(rows) }),
            blue = Waveform2DData(cols, rows, Array(cols) { FloatArray(rows) },
                Array(cols) { FloatArray(rows) }, computeChannel { it and 0xFF }, Array(cols) { FloatArray(rows) })
        )
    }

    private fun rgb2hsl(r: Float, g: Float, b: Float): FloatArray {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h = 0f; var s = 0f
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
}
