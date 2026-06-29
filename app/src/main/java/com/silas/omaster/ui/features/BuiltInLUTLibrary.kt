package com.silas.omaster.ui.features

import kotlin.math.cos
import kotlin.math.max

/**
 * 内置 3D LUT 库 — 对齐 AlcedoStudio 打包的 Kodak/Fuji/Agfa/Ilford 胶片模拟 LUT
 * 与 RapidRAW 的 LUT 应用链路。
 *
 * 为 LUT 面板中列出的每个内置 LUT 程序化生成 [LutProcessor.Lut3D]（尺寸 17），
 * 供 [SmartOptimizeEngine.applyLUT] 通过 [LutProcessor] 进行三线性插值应用，
 * 从而让“LUT”Tab 的选择真实作用于图像，而非占位。
 *
 * 每个 LUT 由一个 (r,g,b) -> (r,g,b) 的色彩变换函数定义，输入输出均为 [0,1]。
 */
object BuiltInLUTLibrary {

    const val LUT_SIZE = 17

    private val cache = HashMap<String, LutProcessor.Lut3D>()

    /** 返回指定名称的内置 LUT；未知名称返回 null。 */
    fun get(name: String): LutProcessor.Lut3D? {
        if (name.isBlank()) return null
        cache[name]?.let { return it }
        val transform = transforms[name] ?: return null
        val lut = buildLUT(transform)
        cache[name] = lut
        return lut
    }

    /** 所有可用的内置 LUT 名称（与 LUT 面板列表一致）。 */
    val names: List<String> = transforms.keys.toList()

    private fun buildLUT(transform: (Float, Float, Float) -> FloatArray): LutProcessor.Lut3D {
        val size = LUT_SIZE
        val data = FloatArray(size * size * size * 3)
        val step = 1f / (size - 1)
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rn = r * step
                    val gn = g * step
                    val bn = b * step
                    val out = transform(rn, gn, bn)
                    val idx = (b * size * size + g * size + r) * 3
                    data[idx] = out[0].coerceIn(0f, 1f)
                    data[idx + 1] = out[1].coerceIn(0f, 1f)
                    data[idx + 2] = out[2].coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    // ==================== 色彩变换基元 ====================

    private fun contrast(x: Float, c: Float): Float = (0.5f + (x - 0.5f) * c).coerceIn(0f, 1f)

    private fun lift(x: Float, amount: Float): Float = (x + amount * (1f - x)).coerceIn(0f, 1f)

    private fun sat(r: Float, g: Float, b: Float, s: Float): FloatArray {
        val gray = 0.299f * r + 0.587f * g + 0.114f * b
        return floatArrayOf(
            (gray + s * (r - gray)).coerceIn(0f, 1f),
            (gray + s * (g - gray)).coerceIn(0f, 1f),
            (gray + s * (b - gray)).coerceIn(0f, 1f)
        )
    }

    private fun tint(r: Float, g: Float, b: Float, hueDeg: Float, amount: Float): FloatArray {
        val rad = Math.toRadians(hueDeg.toDouble()).toFloat()
        val dr = cos(rad) * amount
        val dg = cos(rad - 2.094f) * amount
        val db = cos(rad + 2.094f) * amount
        return floatArrayOf(
            (r + dr).coerceIn(0f, 1f),
            (g + dg).coerceIn(0f, 1f),
            (b + db).coerceIn(0f, 1f)
        )
    }

    // ==================== 命名 LUT 变换 ====================

    private val transforms: Map<String, (Float, Float, Float) -> FloatArray> = mapOf(

        "kodak_portra" to { r, g, b ->
            // Portra: 暖调、低对比、提亮暗部、肤色偏暖
            var rr = contrast(r, 0.92f)
            var gg = contrast(g, 0.92f)
            var bb = contrast(b, 0.9f)
            rr = lift(rr, 0.06f); gg = lift(gg, 0.05f); bb = lift(bb, 0.07f)
            val s = sat(rr, gg, bb, 0.92f)
            tint(s[0], s[1], s[2], 30f, 0.04f)
        },

        "fuji_velvia" to { r, g, b ->
            // Velvia: 高饱和、高对比、冷暗部
            var rr = contrast(r, 1.18f)
            var gg = contrast(g, 1.2f)
            var bb = contrast(b, 1.16f)
            val s = sat(rr, gg, bb, 1.35f)
            tint(s[0], s[1], s[2], 210f, 0.03f)
        },

        "cine_2383" to { r, g, b ->
            // Kodak 2383 电影打印：青色暗部 + 橙色高光 + 中高对比
            val rr = contrast(r, 1.12f)
            val gg = contrast(g, 1.1f)
            val bb = contrast(b, 1.08f)
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val shadowT = max(0f, 0.4f - lum) / 0.4f
            val highT = max(0f, lum - 0.6f) / 0.4f
            floatArrayOf(
                (rr + 0.05f * highT - 0.02f * shadowT).coerceIn(0f, 1f),
                (gg + 0.01f * highT - 0.03f * shadowT).coerceIn(0f, 1f),
                (bb - 0.04f * highT + 0.06f * shadowT).coerceIn(0f, 1f)
            )
        },

        "cine_arri" to { r, g, b ->
            // Arri Alexa：自然、轻微 ACES 色调映射、柔和对比
            val rr = contrast(r, 1.04f)
            val gg = contrast(g, 1.04f)
            val bb = contrast(b, 1.04f)
            sat(rr, gg, bb, 0.97f)
        },

        "cine_teal" to { r, g, b ->
            // Teal & Orange：青色阴影 + 橙色高光
            val rr = contrast(r, 1.1f)
            val gg = contrast(g, 1.08f)
            val bb = contrast(b, 1.06f)
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val shadowT = max(0f, 0.5f - lum) / 0.5f
            val highT = max(0f, lum - 0.5f) / 0.5f
            floatArrayOf(
                (rr + 0.08f * highT - 0.02f * shadowT).coerceIn(0f, 1f),
                (gg + 0.02f * highT - 0.04f * shadowT).coerceIn(0f, 1f),
                (bb - 0.06f * highT + 0.1f * shadowT).coerceIn(0f, 1f)
            )
        },

        "cine_bleach" to { r, g, b ->
            // Bleach Bypass：高对比 + 低饱和
            val rr = contrast(r, 1.35f)
            val gg = contrast(g, 1.35f)
            val bb = contrast(b, 1.35f)
            sat(rr, gg, bb, 0.55f)
        },

        "agfa_vista" to { r, g, b ->
            // Agfa Vista：浓郁红色、暖调
            var rr = contrast(r, 1.08f)
            var gg = contrast(g, 1.06f)
            var bb = contrast(b, 1.04f)
            rr = lift(rr, 0.02f)
            val s = sat(rr, gg, bb, 1.12f)
            tint(s[0], s[1], s[2], 20f, 0.05f)
        },

        "ilford_hp5" to { r, g, b ->
            // HP5 黑白：中对比、轻微暖调
            val lum = (0.299f * r + 0.587f * g + 0.114f * b)
            val mono = contrast(lum, 1.18f)
            val warm = mono + 0.02f
            floatArrayOf(warm.coerceIn(0f, 1f), mono.coerceIn(0f, 1f), (mono - 0.02f).coerceIn(0f, 1f))
        },

        "cine_16mm" to { r, g, b ->
            // 16mm：褪色、暖调、低对比
            var rr = contrast(r, 0.92f)
            var gg = contrast(g, 0.92f)
            var bb = contrast(b, 0.9f)
            rr = lift(rr, 0.1f); gg = lift(gg, 0.09f); bb = lift(bb, 0.08f)
            val s = sat(rr, gg, bb, 0.85f)
            tint(s[0], s[1], s[2], 35f, 0.06f)
        },

        "vintage_fade" to { r, g, b ->
            // 复古褪色：提亮暗部、降饱和、暖调
            var rr = lift(r, 0.14f)
            var gg = lift(g, 0.13f)
            var bb = lift(b, 0.12f)
            rr = contrast(rr, 0.88f); gg = contrast(gg, 0.88f); bb = contrast(bb, 0.88f)
            val s = sat(rr, gg, bb, 0.8f)
            tint(s[0], s[1], s[2], 30f, 0.08f)
        }
    )
}
