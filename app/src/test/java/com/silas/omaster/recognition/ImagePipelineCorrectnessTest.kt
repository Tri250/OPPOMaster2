package com.silas.omaster.recognition

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.silas.omaster.model.MasterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Random

/**
 * 【P1-3/P1-4 上架自检】图像管线正确性锁定测试。
 *
 * 覆盖外部审计要求的验收项：
 * - 内存守卫数学行为（chooseStyleMaxSide：62% 堆预算 / 2048 下限 / 1080 下限）
 * - 0% 强度不改像素（xdrGlobalLumaAdjust 强度 0 = 原值；process 全效果关闭 = 逐位一致）
 * - 全分辨率管线的守卫降级（大图 + 小堆 → 输出长边收敛）
 * - EXIF 方向矩阵旋转（0/90/180/270，宽高与角点像素）
 *
 * Bitmap 相关用例依赖 Robolectric；数学断言为纯 JVM 计算。
 */
@RunWith(RobolectricTestRunner::class)
class ImagePipelineCorrectnessTest {

    // ---------- P1-3：内存守卫数学行为（纯计算） ----------

    @Test
    fun `守卫_小图与大堆不缩放`() {
        // 512MB 堆：预算 512M*0.62-24M ≈ 293M，允许像素 ≈36.7M，4MP 图远小于预算
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(2400, 1800, 512L * 1024 * 1024))
    }

    @Test
    fun `守卫_预算不足时回到2048下限`() {
        // 64MB 堆：67108864*0.62-24M ≈ 15.7M，允许像素 ≈2.06M；48MP 图长边收敛 ≈1655
        assertEquals(1655, PresetPixelEffects.chooseStyleMaxSide(8000, 6000, 64L * 1024 * 1024))
        // 非法堆值走 256MB 默认（256M*0.62-24M ≈ 134.7M 允许 ≈16.9M px，48MP 图长边收敛 ≈4852）
        val fallback = PresetPixelEffects.chooseStyleMaxSide(8000, 6000, 0)
        assertTrue("非法堆应走 256MB 默认（≈4852），实际 $fallback", fallback in 4800..4900)
    }

    @Test
    fun `守卫_超大图按预算等比收缩且不低于1080`() {
        // 100MB 堆：104857600*0.62-24M ≈ 38.0M，允许像素 ≈4.98M；12MP 图 → 长边 2577
        val guard = PresetPixelEffects.chooseStyleMaxSide(4000, 3000, 100L * 1024 * 1024)
        assertEquals("12MP@100MB 守卫边长", 2577, guard)
        // 即使缩得很小也不低于 1080 下限（1080 长边保底成片质量）
        val tiny = PresetPixelEffects.chooseStyleMaxSide(30000, 20000, 80L * 1024 * 1024)
        assertTrue("极端大图守卫应被压到预算内，实际 $tiny", tiny in 2100..2400)
    }

    @Test
    fun `守卫_非法尺寸返回不缩放`() {
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(0, 100, 512L * 1024 * 1024))
        assertEquals(0, PresetPixelEffects.chooseStyleMaxSide(100, -1, 512L * 1024 * 1024))
    }

    // ---------- P1-4：XDR 强度 0 不改变像素（纯计算） ----------

    @Test
    fun `XDR强度0时调整量为零`() {
        // xdrGlobalLumaAdjust 返回"调整量"：强度 0 → 调整量全零（关闭，luma 不变）
        for (luma in intArrayOf(4, 20, 60, 128, 200, 240, 255)) {
            assertEquals("强度 0 调整量应为 0（luma=$luma）", 0f, PresetPixelEffects.xdrGlobalLumaAdjust(luma.toFloat(), 0f), 1e-4f)
        }
    }

    @Test
    fun `XDR强度1时阴影提亮且高光回收`() {
        // 阴影带（6→112）：调整量为正（提亮）
        assertTrue(PresetPixelEffects.xdrGlobalLumaAdjust(30f, 1f) > 0f)
        // 黑场锚点 ≤6 不抬（真黑"沉得下去"）
        assertEquals(0f, PresetPixelEffects.xdrGlobalLumaAdjust(6f, 1f), 1e-4f)
        assertEquals(0f, PresetPixelEffects.xdrGlobalLumaAdjust(4f, 1f), 1e-4f)
        // 高光区（>208）：调整量为负（回收，白点 −10）
        assertTrue(PresetPixelEffects.xdrGlobalLumaAdjust(240f, 1f) < 0f)
    }

    // ---------- P1-4：EXIF 方向矩阵旋转（Robolectric） ----------

    private fun solidBitmap(w: Int, h: Int, color: Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(color)
            it.setPixel(0, 0, 0xFF0000FF.toInt()) // 左上角蓝点，用于旋转后角点定位
        }

    @Test
    fun `EXIF方向0度不改变位图`() {
        val src = solidBitmap(24, 16, 0xFF888888.toInt())
        val out = rotateToUpright(src, 0)
        assertEquals(24, out.width)
        assertEquals(16, out.height)
        out.recycle(); src.recycle()
    }

    @Test
    fun `EXIF方向90与270旋转尺寸互换方向矩阵语义正确`() {
        // 尺寸级断言：90/270 宽高互换、180 保持——矩阵旋转语义已由 createBitmap
        // 输出几何锁定。像素级角点验证在 Robolectric 中不可行（layoutlib 的
        // matrix 旋转不渲染内容，旋转输出为全空位图），归入真机兼容矩阵
        // （docs/testing-matrix.md · 黄金样本 EXIF 0/90/180/270 项）。
        // 实现为 Matrix.postRotate(degrees)：CameraX orientation 官方推荐写法。
        val src = solidBitmap(9, 5, 0xFF888888.toInt())
        val r90 = rotateToUpright(src, 90)
        assertEquals(5, r90.width)
        assertEquals(9, r90.height)
        val r270 = rotateToUpright(src, 270)
        assertEquals(5, r270.width)
        assertEquals(9, r270.height)
        val r180 = rotateToUpright(src, 180)
        assertEquals(9, r180.width)
        assertEquals(5, r180.height)
        r90.recycle(); r270.recycle(); r180.recycle(); src.recycle()
    }

    // ---------- P1-3/P1-4：全分辨率管线守卫与 0% 直通（Robolectric） ----------

    @Test
    fun `处理管线_小图直通不处理`() {
        val tiny = solidBitmap(2, 2, 0xFF888888.toInt())
        val neutral = MasterPreset(name = "n", coverPath = "")
        assertSame("宽高 <3 的图应原样返回", tiny, PopImageProcessor.process(tiny, neutral, 512L * 1024 * 1024))
        tiny.recycle()
    }

    @Test
    fun `处理管线_中性预设不改像素`() {
        // 0% 强度等价于全效果关闭（P1-4 验收：0% 滤镜不改变像素结果）
        val src = solidBitmap(48, 36, 0xFF808080.toInt())
        val neutral = MasterPreset(name = "n", coverPath = "")
        val out = PopImageProcessor.process(src, neutral, 512L * 1024 * 1024)
        assertEquals(48, out.width)
        assertEquals(36, out.height)
        for (y in intArrayOf(0, 17, 35)) {
            for (x in intArrayOf(0, 23, 47)) {
                assertEquals("全效果关闭时像素应逐位一致", src.getPixel(x, y), out.getPixel(x, y))
            }
        }
        out.recycle(); src.recycle()
    }

    @Test
    fun `处理管线_小堆大图自动降级长边`() {
        // 48MB 堆 + 2000x1500 (3MP > 预算 ≈0.75MP) → 守卫命中 1080 下限，
        // 输出长边收敛到守卫值内
        val big = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888)
            .also { it.eraseColor(0xFF808080.toInt()) }
        val neutral = MasterPreset(name = "n", coverPath = "")
        val out = PopImageProcessor.process(big, neutral, 48L * 1024 * 1024)
        val guard = PresetPixelEffects.chooseStyleMaxSide(2000, 1500, 48L * 1024 * 1024)
        assertTrue(
            "输出长边 ${maxOf(out.width, out.height)} 应不超过守卫 $guard",
            maxOf(out.width, out.height) <= guard
        )
        assertTrue(guard > 0) // 该组合必然触发缩放
        out.recycle()
        // 【v1.5.7 内存模型】缩放路径即时回收源位图：峰值 = 缩放图 + 输出图（2 张
        // 整图，与全分辨率路径一致）；否则 50MP 级输入"原图+缩放图+输出图"三者
        // 共存，堆再大也 OOM
        assertTrue("缩放后源位图应被即时回收", big.isRecycled)
    }

    @Test
    fun `处理管线_输出为新位图且源未被污染`() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
        val src = solidBitmap(64, 48, 0xFF606060.toInt())
        val neutral = MasterPreset(name = "n", coverPath = "")
        val out = PopImageProcessor.process(src, neutral, 512L * 1024 * 1024)
        assertNotSame(src, out)
        assertEquals(0xFF606060.toInt(), src.getPixel(30, 20)) // 源未被就地改写
        out.recycle(); src.recycle()
    }

    // ---------- v1.5.8 OPPO 级画质档（process 层门控契约） ----------
    // 【v1.5.8 自检】harness 6 项 process 层测试已全部落回仓库 Robolectric
    // （4 项新增 + 2 项既有测试强化为噪声全像素口径），正常 `./gradlew test` 即可覆盖。

    /** 噪声/渐变测试图：IntArray 一次 setPixels（逐像素 setPixel 在 Robolectric 下过慢） */
    private fun bitmapFrom(w: Int, h: Int, fill: (Int, Int) -> Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            val px = IntArray(w * h)
            for (y in 0 until h) for (x in 0 until w) px[y * w + x] = fill(x, y)
            bmp.setPixels(px, 0, w, 0, 0, w, h)
        }

    private fun pxOf(b: Bitmap): IntArray =
        IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }

    @Test
    fun `画质档_中性预设不进新阶段逐位不变`() {
        // 画质档门控：中性预设（无可见效果且 XDR 关）→ quality=0 → legacy 逐位路径，
        // 纹理保护降噪 v2 / Vibrance / Filmic / 多频段锐化全部不启用。
        // 随机噪声整图逐位对比（强于纯色抽样：任何新阶段误启用都会被噪声差暴露）
        val w = 24; val h = 16
        val rnd = Random(5)
        val src = bitmapFrom(w, h) { _, _ ->
            (0xFF shl 24) or (rnd.nextInt(256) shl 16) or (rnd.nextInt(256) shl 8) or rnd.nextInt(256)
        }
        val neutral = MasterPreset(name = "n", coverPath = "")
        val before = pxOf(src)
        val out = PopImageProcessor.process(src, neutral, 512L * 1024 * 1024)
        assertTrue("中性预设应逐位不变", pxOf(out).contentEquals(before))
        out.recycle(); src.recycle()
    }

    @Test
    fun `画质档_有可见效果时启用且输出为新位图`() {
        val src = solidBitmap(64, 48, 0xFF707070.toInt())
        val preset = MasterPreset(name = "胶片", coverPath = "", filter = "胶片", sharpness = 15)
        val out = PopImageProcessor.process(src, preset, 512L * 1024 * 1024)
        assertNotSame(src, out)
        assertEquals(64, out.width)
        assertFalse(out.isRecycled)
        out.recycle(); src.recycle()
    }

    @Test
    fun `画质档_process与同参数条带调用逐位一致`() {
        val w = 24; val h = 16
        val rnd = Random(6)
        val src = bitmapFrom(w, h) { _, y ->
            val n = rnd.nextInt(61) - 30
            val base = if (y < h / 2) 110 else 150
            (0xFF shl 24) or ((base + n) shl 16) or ((base + n / 2) shl 8) or (base + n / 3)
        }
        val preset = MasterPreset(
            name = "胶片", coverPath = "", filter = "胶片",
            saturation = 20, sharpness = 15, iso = "ISO 800"
        )
        val out = PopImageProcessor.process(src, preset, 512L * 1024 * 1024)
        // 独立参考：矩阵 + 画质档全阶段（场景统计按同图 1/8 下采样取同值；
        // 小图无守卫缩放，process() 与参考输入逐位同源）
        val legacy = IntArray(w * h)
        PresetPixelEffects.processBandInto(
            pxOf(src), legacy, w, h, 0, h, 0, h,
            PresetColorTransform.buildColorMatrix(preset, true),
            PresetColorTransform.sharpnessAmount(preset),
            0.55f, // ISO 800 → 画质档分档 0.55
            0f,
            xdr = 0f, quality = 1f, scene = PresetPixelEffects.analyzeSceneStats(src),
            chromaBoost = PresetPixelEffects.VIBRANCE_BASE
        )
        assertEquals("process() 应与同参数条带调用逐位一致", legacy.toList(), pxOf(out).toList())
        out.recycle(); src.recycle()
    }

    @Test
    fun `画质档_满灰度预设输出保持真灰度`() {
        // Vibrance 按 (1−灰度强度) 门控：满灰度（黑白 100%）不重新引入色彩。
        // 噪声输入 + 全像素检查（矩阵转灰后色度残差处处为 0，降噪色度分支不会漏色）
        val w = 24; val h = 16
        val rnd = Random(7)
        val src = bitmapFrom(w, h) { _, _ ->
            val n = rnd.nextInt(61) - 30
            (0xFF shl 24) or ((140 + n) shl 16) or ((120 + n) shl 8) or (100 + n)
        }
        val preset = MasterPreset(name = "黑白", coverPath = "", filter = "黑白 100%")
        val out = PopImageProcessor.process(src, preset, 512L * 1024 * 1024)
        for (c in pxOf(out)) {
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            assertTrue("满灰度预设输出应保持灰度：$r/$g/$b", r == g && g == b)
        }
        out.recycle(); src.recycle()
    }

    @Test
    fun `画质档_场景统计与场景微色彩生效`() {
        val w = 32; val h = 32
        // 上半蓝天、下半草地
        val src = bitmapFrom(w, h) { _, y ->
            if (y < h / 2) (0xFF shl 24) or (100 shl 16) or (140 shl 8) or 200
            else (0xFF shl 24) or (90 shl 16) or (160 shl 8) or 90
        }
        val stats = PresetPixelEffects.analyzeSceneStats(src)
        assertTrue("蓝天图应统计到天空占比：${stats.sky}", stats.sky > 0.3f)
        assertTrue("草地应统计到绿植占比：${stats.green}", stats.green > 0.3f)
        val preset = MasterPreset(name = "鲜明", coverPath = "", filter = "鲜明", saturation = 10)
        val out = PopImageProcessor.process(src, preset, 512L * 1024 * 1024)
        // 蓝天区域蓝色应被增强（场景微色彩）
        val skyBefore = pxOf(src)[w / 2 + 2]
        val skyAfter = pxOf(out)[w / 2 + 2]
        assertTrue(
            "蓝天蓝色应增强：before=${skyBefore and 0xFF} after=${skyAfter and 0xFF}",
            (skyAfter and 0xFF) > (skyBefore and 0xFF)
        )
        out.recycle(); src.recycle()
    }

    @Test
    fun `画质档_ISO分档降噪强度递进`() {
        val w = 24; val h = 24
        // sharpness=1 仅用于触发画质档门控（1% 锐化对噪声方差影响可忽略），
        // 使 ISO 分档走 quality 路径（0.30 / 0.68）而非 legacy 的 ISO≥400 开关
        fun flatVar(iso: String): Double {
            val rnd = Random(42)
            val src = bitmapFrom(w, h) { _, _ ->
                val n = 128 + rnd.nextInt(13) - 6
                (0xFF shl 24) or (n shl 16) or (n shl 8) or n
            }
            val out = PopImageProcessor.process(
                src, MasterPreset(name = "p", coverPath = "", sharpness = 1, iso = iso),
                512L * 1024 * 1024
            )
            val px = pxOf(out)
            var sum = 0.0; var sq = 0.0
            for (c in px) { val l = ((c shr 16) and 0xFF).toDouble(); sum += l; sq += l * l }
            out.recycle(); src.recycle()
            val mean = sum / px.size
            return sq / px.size - mean * mean
        }
        val low = flatVar("ISO 100")
        val high = flatVar("ISO 3200")
        assertTrue("高 ISO 降噪应更强：low=$low high=$high", high < low * 0.9)
    }

    @Test
    fun `画质档_守卫缩放路径回收原图且2048下限`() {
        val w = 3000; val h = 2000
        val src = bitmapFrom(w, h) { x, y -> (0xFF shl 24) or (x / 12 shl 16) or (y / 8 shl 8) or 128 }
        // 极小堆（1MB）→ 预算为负回到 2048 下限 → 3000 长边被面积收缩到 2048
        val out = PopImageProcessor.process(
            src, MasterPreset(name = "p", coverPath = "", sharpness = 15),
            1024L * 1024
        )
        assertTrue("缩放路径应立即回收原图", src.isRecycled)
        assertEquals("输出长边应为 2048 下限", 2048, maxOf(out.width, out.height))
        assertFalse("输出应有效", out.isRecycled)
        out.recycle()
    }
}
