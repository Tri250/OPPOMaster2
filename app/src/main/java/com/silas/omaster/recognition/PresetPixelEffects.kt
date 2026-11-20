package com.silas.omaster.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * 预设像素级效果（锐化 / 暗角）。
 *
 * 拍后取景与成片共用；性能敏感，均为纯像素运算。
 * 内存策略：输入为可变 Bitmap 时**原地**处理（不额外分配整图），
 * 与 POP 成片管线的降采样（2048px）配合，把 12MP 原图的套色峰值内存
 * 从 ~300MB 压到 ~75MB。
 */
object PresetPixelEffects {

    /** 中心权重 1、四邻 0.25/8…的 3x3 unsharp 近似：dst = src + amount * (src - blur)。src 与 dst 不可为同一数组。 */
    fun sharpenPixelsInto(src: IntArray, dst: IntArray, w: Int, h: Int, amount: Float) {
        System.arraycopy(src, 0, dst, 0, src.size)
        // amount 极小时无感，直接拷贝
        if (amount <= 0.016f) return
        val a = (amount * 1.2f).coerceAtMost(1.0f)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                val c = src[i]
                val cr = (c shr 16) and 0xFF
                val cg = (c shr 8) and 0xFF
                val cb = c and 0xFF
                // 四邻平均（blur 近似）
                val l = src[i - 1]; val r = src[i + 1]; val u = src[i - w]; val d = src[i + w]
                val lr = (l shr 16) and 0xFF; val rr = (r shr 16) and 0xFF
                val ur = (u shr 16) and 0xFF; val dr = (d shr 16) and 0xFF
                val lg = (l shr 8) and 0xFF; val rg = (r shr 8) and 0xFF
                val ug = (u shr 8) and 0xFF; val dg = (d shr 8) and 0xFF
                val lb = l and 0xFF; val rb = r and 0xFF
                val ub = u and 0xFF; val db = d and 0xFF
                val blurR = (lr + rr + ur + dr + 2) / 4
                val blurG = (lg + rg + ug + dg + 2) / 4
                val blurB = (lb + rb + ub + db + 2) / 4
                val nr = (cr + a * (cr - blurR)).toInt().coerceIn(0, 255)
                val ng = (cg + a * (cg - blurG)).toInt().coerceIn(0, 255)
                val nb = (cb + a * (cb - blurB)).toInt().coerceIn(0, 255)
                dst[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
    }

    /**
     * 3x3 unsharp 锐化。输入为可变 Bitmap 时原地处理并返回同一实例；不可变时返回新 Bitmap。
     * amount <= 0.016 视为无感，直接返回原图。
     */
    fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        if (amount <= 0.016f) return src
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src

        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // 注意：src/dst 不可别名——锐化要读"原始邻域"，必须用独立输出数组
        val out = IntArray(w * h)
        sharpenPixelsInto(px, out, w, h, amount)

        if (src.isMutable) {
            src.setPixels(out, 0, w, 0, 0, w, h)
            return src
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * 径向暗角（四角压暗、中心不变）。输入为可变 Bitmap 时原地绘制；不可变时返回新 Bitmap。
     * strength <= 0.02 视为无感，直接返回原图。
     */
    fun applyVignette(src: Bitmap, strength: Float): Bitmap {
        if (strength <= 0.02f) return src
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val target = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
        val radius = maxOf(w, h) * 0.75f
        val dark = (0.62f * strength).coerceIn(0f, 0.62f)
        val canvas = Canvas(target)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(0x00000000, 0x00000000, (dark * 255).toInt() shl 24),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return target
    }
}
