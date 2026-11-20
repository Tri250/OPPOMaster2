package com.silas.omaster.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.sqrt

/**
 * 【v1.7.8】POP 即拍全分辨率画质管线测试（纯 JVM，不依赖 android.graphics）。
 *
 * 覆盖 [PresetPixelEffects] 新增的条带处理内核：
 * ① 任意分带数量结果一致（重叠行消除接缝）；
 * ② 4x5 色彩矩阵与参考实现一致、越界钳位、单位矩阵直通；
 * ③ 保边降噪压平坦区噪点、边缘保留；
 * ④ 亮度域 unsharp 增强边缘梯度、不放大平坦区噪点；
 * ⑤ 暗角中心不变、四角按公式压暗；
 * ⑥ 内存守卫 [PresetPixelEffects.chooseStyleMaxSide] 各堆规格行为；
 * ⑦ 高斯模糊均匀图不变、阶梯被平滑。
 */
class PopImageQualityTest {

    private fun rgb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun img(w: Int, h: Int, f: (x: Int, y: Int) -> Int): IntArray =
        IntArray(w * h).also { px -> for (y in 0 until h) for (x in 0 until w) px[y * w + x] = f(x, y) }

    private fun lum(px: Int) = ((px shr 16) and 0xFF) * 54 + ((px shr 8) and 0xFF) * 183 + (px and 0xFF) * 19 shr 8

    private fun ch(px: Int, shift: Int) = (px shr shift) and 0xFF

    private fun edgeW(residual: Float): Float {
        val t = ((residual - 6f) / 24f).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 参考实现：整带一步处理（与实现同公式，独立书写用于交叉验证） */
    private fun referenceProcess(
        src: IntArray, w: Int, h: Int,
        matrix: FloatArray?, sharpen: Float, denoise: Float, vignette: Float
    ): IntArray {
        // base = 矩阵后的原始像素（邻域运算只读 base，保证与实现一致：读未被污染的源）
        val base = src.copyOf()
        if (matrix != null) PresetPixelEffects.applyMatrixInto(base, w, h, matrix)
        val out = base.copyOf()
        val lumArr = IntArray(w * h) { lum(base[it]) }
        val blur = IntArray(w * h)
        PresetPixelEffects.gaussianBlurInto(lumArr, blur, w, h, 2)
        val cx = (w - 1) / 2f; val cy = (h - 1) / 2f
        val maxSide = maxOf(w, h).toFloat(); val vr = maxSide * 0.75f
        val dark = (0.62f * vignette).coerceIn(0f, 0.62f)
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val d = Math.abs(lumArr[i] - blur[i]).toFloat()
            val ew = edgeW(d)
            var c = out[i]
            if (denoise > 0.01f && ew < 0.9f) {
                val a = denoise * 0.85f * (1f - ew)
                if (a > 0.001f) {
                val l = if (x > 0) base[i - 1] else c
                val r = if (x < w - 1) base[i + 1] else c
                val u = if (y > 0) base[i - w] else c
                val dn = if (y < h - 1) base[i + w] else c
                val mr = (ch(l, 16) + ch(r, 16) + ch(u, 16) + ch(dn, 16)) / 4f
                val mg = (ch(l, 8) + ch(r, 8) + ch(u, 8) + ch(dn, 8)) / 4f
                val mb = (ch(l, 0) + ch(r, 0) + ch(u, 0) + ch(dn, 0)) / 4f
                val cr = ((c shr 16) and 0xFF).toFloat(); val cg = ((c shr 8) and 0xFF).toFloat(); val cb = (c and 0xFF).toFloat()
                c = (0xFF shl 24) or
                    ((cr + (mr - cr) * a).toInt().coerceIn(0, 255) shl 16) or
                    ((cg + (mg - cg) * a).toInt().coerceIn(0, 255) shl 8) or
                    ((cb + (mb - cb) * a).toInt().coerceIn(0, 255))
                }
            }
            if (sharpen > 0.02f) {
                val eff = sharpen * 1.6f * ew
                val delta = (lumArr[i] - blur[i]).toFloat() * eff
                if (delta > 0.5f || delta < -0.5f) {
                c = (0xFF shl 24) or
                    ((((c shr 16) and 0xFF) + delta).toInt().coerceIn(0, 255) shl 16) or
                    ((((c shr 8) and 0xFF) + delta).toInt().coerceIn(0, 255) shl 8) or
                    (((c and 0xFF) + delta).toInt().coerceIn(0, 255))
                }
            }
            if (vignette > 0.02f) {
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                val t = ((dist / vr - 0.55f) / 0.45f).coerceIn(0f, 1f)
                if (t > 0.001f) {
                    val f = 1f - dark * t
                    c = (0xFF shl 24) or
                        ((((c shr 16) and 0xFF) * f).toInt().coerceIn(0, 255) shl 16) or
                        ((((c shr 8) and 0xFF) * f).toInt().coerceIn(0, 255) shl 8) or
                        (((c and 0xFF) * f).toInt().coerceIn(0, 255))
                }
            }
            out[i] = c
        }
        return out
    }

    private val satMatrix = floatArrayOf(
        1.3f, 0f, 0f, 0f, -12f,
        0f, 1.3f, 0f, 0f, -12f,
        0f, 0f, 1.3f, 0f, -12f,
        0f, 0f, 0f, 1f, 0f
    )

    // ---------- ① 条带无接缝 ----------

    @Test
    fun `任意分带数量处理结果一致`() {
        val w = 40; val h = 31
        val rnd = Random(42)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val m = satMatrix; val s = 0.5f; val dn = 0.55f; val v = 0.6f

        // 整带一步
        val outFull = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), outFull, w, h, 0, h, 0, h, m, s, dn, v
        )

        // 多带（中心行 7 行一带，重叠 2 行）
        val outMulti = IntArray(w * h)
        var y = 0
        while (y < h) {
            val y1 = minOf(h, y + 7)
            val top = (y - 2).coerceAtLeast(0)
            val bot = (y1 + 2).coerceAtMost(h)
            val padTop = y - top
            val bandH = bot - top
            val band = IntArray(w * bandH) { i ->
                val bx = i % w; val by = top + i / w
                src[by * w + bx]
            }
            val out = IntArray(w * bandH)
            PresetPixelEffects.processBandInto(
                band, out, w, h, top, bandH, padTop, y1 - y, m, s, dn, v
            )
            for (ry in y until y1) {
                System.arraycopy(out, (ry - top) * w, outMulti, ry * w, w)
            }
            y = y1
        }
        assertTrue(
            "多带结果应与整带一致（无接缝），首个差异 idx=" +
                outFull.indices.firstOrNull { outFull[it] != outMulti[it] },
            outFull.contentEquals(outMulti)
        )
    }

    @Test
    fun `条带写回偏移必须跳过带顶重叠行`() {
        // 复刻生产 processBitmapBand 的完整调度：抽取带（含重叠）→ processBandInto
        // → 按 bandWriteSrcOffset 写回中心行。v1.7.9 曾把写回源偏移写成 0，
        // 导致除首带外每带把 padTop=2 行未完成降噪/锐化/暗角的重叠行写进成片
        // 并整体错位 → 成片每 192 行出现横向"线条"。
        val w = 24; val h = 600
        val rnd = Random(1234)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val m = satMatrix; val s = 0.5f; val dn = 0.55f; val v = 0.6f
        val radius = 2

        // 偏移公式钉死：padTop 行重叠必须整体跳过
        assertEquals(radius * w, PresetPixelEffects.bandWriteSrcOffset(radius, w))
        assertEquals(0, PresetPixelEffects.bandWriteSrcOffset(0, w))

        // 整带一步
        val outFull = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), outFull, w, h, 0, h, 0, h, m, s, dn, v
        )

        // 192 行条带（真实 BAND_ROWS）多带调度
        val outMulti = IntArray(w * h)
        var y = 0
        while (y < h) {
            val y1 = minOf(h, y + 192)
            val top = (y - radius).coerceAtLeast(0)
            val bot = (y1 + radius).coerceAtMost(h)
            val padTop = y - top
            val bandH = bot - top
            val band = IntArray(w * bandH) { i ->
                val bx = i % w; val by = top + i / w
                src[by * w + bx]
            }
            val out = IntArray(w * bandH)
            PresetPixelEffects.processBandInto(
                band, out, w, h, top, bandH, padTop, y1 - y, m, s, dn, v
            )
            System.arraycopy(
                out, PresetPixelEffects.bandWriteSrcOffset(padTop, w),
                outMulti, y * w, (y1 - y) * w
            )
            y = y1
        }
        assertTrue(
            "多带写回应与整带一致（无接缝/线条），首个差异 idx=" +
                outFull.indices.firstOrNull { outFull[it] != outMulti[it] },
            outFull.contentEquals(outMulti)
        )
    }

    @Test
    fun `条带处理与独立参考实现一致`() {
        val w = 24; val h = 17
        val rnd = Random(7)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, satMatrix, 0.5f, 0.55f, 0.6f
        )
        val ref = referenceProcess(src, w, h, satMatrix, 0.5f, 0.55f, 0.6f)
        assertTrue("实现应与参考一致", out.contentEquals(ref))
    }

    // ---------- ② 色彩矩阵 ----------

    @Test
    fun `色彩矩阵与参考公式一致`() {
        val w = 8; val h = 8
        val rnd = Random(11)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val px = src.copyOf()
        PresetPixelEffects.applyMatrixInto(px, w, h, satMatrix)
        for (i in 0 until w * h) {
            val c = src[i]
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val er = (1.3f * r - 12f).toInt().coerceIn(0, 255)
            val eg = (1.3f * g - 12f).toInt().coerceIn(0, 255)
            val eb = (1.3f * b - 12f).toInt().coerceIn(0, 255)
            assertEquals(
                "px[$i] r", (0xFF shl 24) or (er shl 16) or (eg shl 8) or eb, px[i]
            )
        }
    }

    @Test
    fun `矩阵越界值被钳位到0到255`() {
        val w = 2; val h = 1
        val src = intArrayOf(rgb(240, 240, 240), rgb(2, 2, 2))
        val px = src.copyOf()
        PresetPixelEffects.applyMatrixInto(px, w, h, satMatrix)
        // 1.3*240-12 = 300 → 钳位 255
        assertEquals(255, (px[0] shr 16) and 0xFF)
        // 1.3*2-12 = -9.4 → 钳位 0
        assertEquals(0, (px[1] shr 16) and 0xFF)
    }

    @Test
    fun `单位矩阵直通不改写`() {
        val w = 6; val h = 5
        val rnd = Random(3)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val out = IntArray(w * h)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, identity, 0f, 0f, 0f
        )
        assertTrue(out.contentEquals(src))
    }

    @Test
    fun `全关闭参数直通不改写`() {
        val w = 6; val h = 5
        val rnd = Random(5)
        val src = IntArray(w * h) { rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)) }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0f, 0f, 0f
        )
        assertTrue(out.contentEquals(src))
    }

    // ---------- ③ 保边降噪 ----------

    @Test
    fun `降噪压平坦区噪点且保边缘`() {
        val w = 16; val h = 12
        val rnd = Random(99)
        // 左半：±6 噪点平坦区；右半：80/200 阶梯边缘
        val src = img(w, h) { x, y ->
            when {
                x < w / 2 -> {
                    val n = 128 + if (rnd.nextBoolean()) 6 else -6
                    rgb(n, n, n)
                }
                x < w / 2 + 2 -> rgb(80, 80, 80)
                else -> rgb(200, 200, 200)
            }
        }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0f, 0.55f, 0f
        )
        fun variance(px: IntArray, x0: Int, x1: Int): Double {
            var sum = 0.0; var sq = 0.0; var n = 0
            for (y in 0 until h) for (x in x0 until x1) {
                val l = lum(px[y * w + x]).toDouble(); sum += l; sq += l * l; n++
            }
            val mean = sum / n
            return sq / n - mean * mean
        }
        val varBefore = variance(src, 2, w / 2 - 2)
        val varAfter = variance(out, 2, w / 2 - 2)
        assertTrue("平坦区噪点方差应显著下降：before=$varBefore after=$varAfter", varAfter < varBefore * 0.75)

        // 边缘（第 w/2-1 与 w/2+2 列之间）梯度保留 ≥90%
        val gradBefore = lum(src[3 * w + w / 2 + 2]) - lum(src[3 * w + w / 2 - 1])
        val gradAfter = lum(out[3 * w + w / 2 + 2]) - lum(out[3 * w + w / 2 - 1])
        assertTrue(
            "边缘梯度应保留：before=$gradBefore after=$gradAfter",
            gradAfter >= gradBefore * 0.9
        )
    }

    // ---------- ④ 亮度域 unsharp ----------

    @Test
    fun `锐化增强边缘梯度且两侧外扩`() {
        val w = 8; val h = 8
        val src = img(w, h) { x, _ -> if (x < w / 2) rgb(80, 80, 80) else rgb(200, 200, 200) }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0.5f, 0f, 0f
        )
        val left = 3 * w + 3
        val right = 3 * w + 4
        val gradBefore = lum(src[right]) - lum(src[left])
        val gradAfter = lum(out[right]) - lum(out[left])
        assertTrue("边缘梯度应增强：before=$gradBefore after=$gradAfter", gradAfter > gradBefore)
        assertTrue("暗侧应更暗", lum(out[left]) < lum(src[left]))
        assertTrue("亮侧应更亮", lum(out[right]) > lum(src[right]))
    }

    @Test
    fun `锐化不放大平坦区噪点`() {
        val w = 32; val h = 32
        val rnd = Random(2024)
        val src = IntArray(w * h) {
            val n = 128 + rnd.nextInt(13) - 6
            rgb(n, n, n)
        }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0.5f, 0f, 0f
        )
        fun variance(px: IntArray): Double {
            var sum = 0.0; var sq = 0.0
            for (p in px) { val l = lum(p).toDouble(); sum += l; sq += l * l }
            val mean = sum / px.size
            return sq / px.size - mean * mean
        }
        val before = variance(src)
        val after = variance(out)
        assertTrue("平坦噪点不应被放大：before=$before after=$after", after < before * 1.15)
    }

    @Test
    fun `降噪加锐化组合净降噪`() {
        val w = 32; val h = 32
        val rnd = Random(777)
        val src = IntArray(w * h) {
            val n = 128 + rnd.nextInt(13) - 6
            rgb(n, n, n)
        }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0.5f, 0.55f, 0f
        )
        fun variance(px: IntArray): Double {
            var sum = 0.0; var sq = 0.0
            for (p in px) { val l = lum(p).toDouble(); sum += l; sq += l * l }
            val mean = sum / px.size
            return sq / px.size - mean * mean
        }
        assertTrue(variance(out) < variance(src) * 0.75)
    }

    // ---------- ⑤ 暗角 ----------

    @Test
    fun `暗角中心不变四角按公式压暗`() {
        val w = 60; val h = 40
        val src = img(w, h) { _, _ -> rgb(200, 200, 200) }
        val out = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            src.copyOf(), out, w, h, 0, h, 0, h, null, 0f, 0f, 1.0f
        )
        val center = out[(h / 2) * w + w / 2]
        assertEquals("中心应不变", src[(h / 2) * w + w / 2], center)

        val corner = out[0]
        val dx = 0 - (w - 1) / 2f
        val dy = 0 - (h - 1) / 2f
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val t = ((dist / (maxOf(w, h) * 0.75f) - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val expected = (200 * (1f - 0.62f * t)).toInt().coerceIn(0, 255)
        assertTrue("四角应按公式压暗：expected=$expected actual=${corner and 0xFF}",
            Math.abs(expected - (corner and 0xFF)) <= 2)
        assertTrue("四角应暗于中心", (corner and 0xFF) < 200)
    }

    // ---------- ⑥ 内存守卫 ----------

    @Test
    fun `内存守卫大堆全分辨率不降采样`() {
        // 512MB 堆：预算 198MB → 允许 24.8MP ≥ 12.2MP
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(4032, 3024, 512L * 1024 * 1024))
    }

    @Test
    fun `内存守卫小堆按面积收缩`() {
        // 128MB 堆：预算 25.6MB → 允许 3.2MP，12.2MP 需缩到 ~2066px
        val maxSide = PresetPixelEffects.chooseStyleMaxSide(4032, 3024, 128L * 1024 * 1024)
        assertTrue("应返回正上限：$maxSide", maxSide in 1500..2600)
    }

    @Test
    fun `内存守卫108MP极端分辨率收缩`() {
        val maxSide = PresetPixelEffects.chooseStyleMaxSide(12000, 9000, 512L * 1024 * 1024)
        assertTrue("108MP 应收缩到 ~5700px：$maxSide", maxSide in 5000..6500)
    }

    @Test
    fun `内存守卫未知堆按256MB保守估计`() {
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(100, 100, 0))
        assertTrue(PresetPixelEffects.chooseStyleMaxSide(8000, 6000, 0) > 0)
    }

    @Test
    fun `内存守卫零尺寸安全返回`() {
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(0, 0, 512L * 1024 * 1024))
    }

    // ---------- ⑦ 高斯模糊 ----------

    @Test
    fun `高斯模糊均匀图不变`() {
        val w = 12; val h = 9
        val src = IntArray(w * h) { 128 }
        val dst = IntArray(w * h)
        PresetPixelEffects.gaussianBlurInto(src, dst, w, h, 2)
        assertTrue(dst.all { it == 128 })
    }

    @Test
    fun `高斯模糊平滑阶梯`() {
        val w = 9; val h = 3
        val src = IntArray(w * h) { i -> if (i % w < 4) 50 else 200 }
        val dst = IntArray(w * h)
        PresetPixelEffects.gaussianBlurInto(src, dst, w, h, 2)
        // 阶梯处（第 3/4 列之间）应出现过渡值
        val mid = dst[3]
        assertTrue("阶梯应被平滑出过渡值：$mid", mid in 51..199)
    }
}
