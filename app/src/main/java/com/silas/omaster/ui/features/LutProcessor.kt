package com.silas.omaster.ui.features

import android.graphics.Color
import kotlin.math.cbrt

/**
 * LUT 处理器
 *
 * 对齐 PixelFruit LutProcessor.js
 * - .cube 文件解析
 * - 三线性插值（Trilinear Interpolation）
 * - LUT 数据管理
 */
class LutProcessor {

    /**
     * 3D LUT 数据结构
     *
     * @param size LUT 尺寸（通常 17/33/65）
     * @param data size³ × 3 的 RGB 数据，索引顺序：b + g×size + r×size²
     */
    data class Lut3D(
        val size: Int,
        val data: FloatArray
    ) {
        fun getR(r: Int, g: Int, b: Int): Float = data[(b * size * size + g * size + r) * 3]
        fun getG(r: Int, g: Int, b: Int): Float = data[(b * size * size + g * size + r) * 3 + 1]
        fun getB(r: Int, g: Int, b: Int): Float = data[(b * size * size + g * size + r) * 3 + 2]
    }

    /**
     * 解析 .cube 文件内容
     *
     * 支持两种值域：
     * - 0-255 整数（自动归一化到 0-1）
     * - 0-1 浮点数（直接使用）
     */
    fun parseCube(content: String): Lut3D {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

        var size = 0
        val dataList = mutableListOf<Triple<Float, Float, Float>>()
        var maxValue = 1f

        for (line in lines) {
            when {
                line.startsWith("LUT_3D_SIZE") -> {
                    size = line.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull() ?: 0
                }
                line.startsWith("DOMAIN_MIN") || line.startsWith("DOMAIN_MAX") -> {
                    // 可选：解析域范围
                }
                line.matches(Regex("^\\d+\\s+\\d+\\s+\\d+\$") ) -> {
                    // 0-255 整数格式
                    val parts = line.split(Regex("\\s+"))
                    dataList.add(Triple(
                        parts[0].toFloat() / 255f,
                        parts[1].toFloat() / 255f,
                        parts[2].toFloat() / 255f
                    ))
                    maxValue = 255f
                }
                line.matches(Regex("^\\d*\\.?\\d+\\s+\\d*\\.?\\d+\\s+\\d*\\.?\\d+\$") ) -> {
                    // 0-1 浮点数格式
                    val parts = line.split(Regex("\\s+"))
                    dataList.add(Triple(
                        parts[0].toFloat(),
                        parts[1].toFloat(),
                        parts[2].toFloat()
                    ))
                }
            }
        }

        // 自动推断尺寸
        if (size == 0 && dataList.isNotEmpty()) {
            size = cbrt(dataList.size.toFloat()).toInt()
        }

        // 如果数据值域大于1，归一化
        if (maxValue > 1f) {
            val normalized = dataList.map { (r, g, b) ->
                Triple(r / maxValue, g / maxValue, b / maxValue)
            }
            dataList.clear()
            dataList.addAll(normalized)
        }

        val data = FloatArray(dataList.size * 3)
        dataList.forEachIndexed { i, (r, g, b) ->
            data[i * 3] = r.coerceIn(0f, 1f)
            data[i * 3 + 1] = g.coerceIn(0f, 1f)
            data[i * 3 + 2] = b.coerceIn(0f, 1f)
        }

        return Lut3D(size, data)
    }

    /**
     * 三线性插值应用 LUT
     *
     * 算法步骤：
     * 1. 将输入 RGB 缩放到 LUT 索引空间
     * 2. 找到包围输入点的 8 个顶点
     * 3. 计算插值权重
     * 4. 沿 R/G/B 三个维度依次线性插值
     * 5. 混合原始值与 LUT 结果
     */
    fun applyLut(pixels: IntArray, lut: Lut3D, intensity: Float) {
        if (intensity <= 0f || lut.size <= 0) return
        val size = lut.size
        val sizeMinusOne = size - 1

        for (i in pixels.indices) {
            val a = Color.alpha(pixels[i])
            val rIn = Color.red(pixels[i]) / 255f
            val gIn = Color.green(pixels[i]) / 255f
            val bIn = Color.blue(pixels[i]) / 255f

            // 缩放到 LUT 索引空间
            val rScaled = rIn * sizeMinusOne
            val gScaled = gIn * sizeMinusOne
            val bScaled = bIn * sizeMinusOne

            // 包围盒顶点
            val r0 = rScaled.toInt().coerceIn(0, sizeMinusOne)
            val g0 = gScaled.toInt().coerceIn(0, sizeMinusOne)
            val b0 = bScaled.toInt().coerceIn(0, sizeMinusOne)
            val r1 = (r0 + 1).coerceAtMost(sizeMinusOne)
            val g1 = (g0 + 1).coerceAtMost(sizeMinusOne)
            val b1 = (b0 + 1).coerceAtMost(sizeMinusOne)

            // 插值权重
            val dr = rScaled - r0
            val dg = gScaled - g0
            val db = bScaled - b0

            // 8 个顶点采样
            val c000 = getColor(lut, r0, g0, b0)
            val c001 = getColor(lut, r0, g0, b1)
            val c010 = getColor(lut, r0, g1, b0)
            val c011 = getColor(lut, r0, g1, b1)
            val c100 = getColor(lut, r1, g0, b0)
            val c101 = getColor(lut, r1, g0, b1)
            val c110 = getColor(lut, r1, g1, b0)
            val c111 = getColor(lut, r1, g1, b1)

            // 三线性插值
            val c00 = lerp(c000, c100, dr)
            val c01 = lerp(c001, c101, dr)
            val c10 = lerp(c010, c110, dr)
            val c11 = lerp(c011, c111, dr)

            val c0 = lerp(c00, c10, dg)
            val c1 = lerp(c01, c11, dg)

            val final = lerp(c0, c1, db)

            // 混合原始值与 LUT 结果
            val outR = (rIn * (1f - intensity) + final.first * intensity) * 255f
            val outG = (gIn * (1f - intensity) + final.second * intensity) * 255f
            val outB = (bIn * (1f - intensity) + final.third * intensity) * 255f

            pixels[i] = Color.argb(
                a,
                outR.toInt().coerceIn(0, 255),
                outG.toInt().coerceIn(0, 255),
                outB.toInt().coerceIn(0, 255)
            )
        }
    }

    private fun getColor(lut: Lut3D, r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        return Triple(lut.getR(r, g, b), lut.getG(r, g, b), lut.getB(r, g, b))
    }

    private fun lerp(
        a: Triple<Float, Float, Float>,
        b: Triple<Float, Float, Float>,
        t: Float
    ): Triple<Float, Float, Float> {
        return Triple(
            a.first + (b.first - a.first) * t,
            a.second + (b.second - a.second) * t,
            a.third + (b.third - a.third) * t
        )
    }
}
