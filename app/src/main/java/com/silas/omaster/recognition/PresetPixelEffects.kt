package com.silas.omaster.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.hypot

/**
 * 预设的像素级效果（ColorMatrix 无法表达的部分）：锐化与暗角。
 * 强度参数由 [PresetColorTransform] 纯函数解析，本类只做 Bitmap 像素操作。
 */
object PresetPixelEffects {

    /** 3x3 unsharp 核锐化，amount 0..1；≤0.02 直接返回原图 */
    fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        val a = amount.coerceIn(0f, 1f) * 0.8f
        if (a <= 0.016f) return src
        val w = src.width; val h = src.height
        if (w < 3 || h < 3) return src

        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        val center = 1f + 4f * a
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                val c = px[i]; val l = px[i - 1]; val r = px[i + 1]
                val u = px[i - w]; val d = px[i + w]
                out[i] = 0xFF000000.toInt() or
                    channel(c, l, r, u, d, center, a, 16) or
                    channel(c, l, r, u, d, center, a, 8) or
                    channel(c, l, r, u, d, center, a, 0)
            }
        }
        // 边界 1px 保留原值
        for (x in 0 until w) { out[x] = px[x]; out[(h - 1) * w + x] = px[(h - 1) * w + x] }
        for (y in 0 until h) { out[y * w] = px[y * w]; out[y * w + w - 1] = px[y * w + w - 1] }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun channel(
        c: Int, l: Int, r: Int, u: Int, d: Int,
        center: Float, a: Float, shift: Int
    ): Int {
        val v = ((c shr shift and 0xFF) * center -
            ((l shr shift and 0xFF) + (r shr shift and 0xFF) +
                (u shr shift and 0xFF) + (d shr shift and 0xFF)) * a)
            .toInt().coerceIn(0, 255)
        return v shl shift
    }

    /** 径向暗角，strength 0..1；≤0 直接返回原图 */
    fun applyVignette(src: Bitmap, strength: Float): Bitmap {
        if (strength <= 0f) return src
        val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
        val cx = out.width / 2f; val cy = out.height / 2f
        val radius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0xFF000000.toInt()),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP
            )
            alpha = (strength * 170f).toInt().coerceIn(1, 255)
        }
        Canvas(out).drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), paint)
        return out
    }
}
