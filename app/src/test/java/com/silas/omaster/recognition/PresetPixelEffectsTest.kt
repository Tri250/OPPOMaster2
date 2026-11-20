package com.silas.omaster.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PresetPixelEffects 锐化内核测试（纯 JVM，不依赖 android.graphics）。
 *
 * 验证抽出的纯函数 [PresetPixelEffects.sharpenPixelsInto]：
 * 边界像素保护、边缘梯度增强、均匀图不变、极小强度直拷。
 */
class PresetPixelEffectsTest {

    private fun rgb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun img(w: Int, h: Int, f: (x: Int, y: Int) -> Int): IntArray =
        IntArray(w * h).also { px -> for (y in 0 until h) for (x in 0 until w) px[y * w + x] = f(x, y) }

    @Test
    fun `极小强度仅拷贝不改写`() {
        val src = img(4, 4) { x, _ -> rgb(x * 60, 100, 200) }
        val dst = IntArray(src.size)
        PresetPixelEffects.sharpenPixelsInto(src, dst, 4, 4, 0.01f)
        assertTrue(dst.contentEquals(src))
    }

    @Test
    fun `边界像素保持原值`() {
        val w = 5; val h = 5
        val src = img(w, h) { x, y -> rgb(if (x < w / 2) 0 else 255, y * 50, 128) }
        val dst = IntArray(src.size)
        PresetPixelEffects.sharpenPixelsInto(src, dst, w, h, 0.5f)
        for (x in 0 until w) {
            assertEquals(src[x], dst[x])                       // 上边
            assertEquals(src[(h - 1) * w + x], dst[(h - 1) * w + x]) // 下边
        }
        for (y in 0 until h) {
            assertEquals(src[y * w], dst[y * w])               // 左边
            assertEquals(src[y * w + w - 1], dst[y * w + w - 1]) // 右边
        }
    }

    @Test
    fun `黑白竖边缘梯度被增强`() {
        val w = 4; val h = 4
        // 中灰/亮灰竖半边（避开 0/255 钳位，梯度变化可测）
        val src = img(w, h) { x, _ -> if (x < w / 2) rgb(80, 80, 80) else rgb(180, 180, 180) }
        val dst = IntArray(src.size)
        PresetPixelEffects.sharpenPixelsInto(src, dst, w, h, 0.5f)
        fun lum(px: IntArray, i: Int) = (px[i] shr 16) and 0xFF
        // 取内部行（避开边界像素：边界按设计保持原值）
        val left = 1 * w + 1
        val right = 1 * w + 2
        val stepBefore = lum(src, right) - lum(src, left)
        val stepAfter = lum(dst, right) - lum(dst, left)
        assertTrue("边缘梯度应增强：before=$stepBefore after=$stepAfter", stepAfter > stepBefore)
        // 暗侧更暗、亮侧更亮
        assertTrue(lum(dst, left) < lum(src, left))
        assertTrue(lum(dst, right) > lum(src, right))
    }

    @Test
    fun `均匀图像锐化后不变`() {
        val src = img(6, 6) { _, _ -> rgb(90, 120, 150) }
        val dst = IntArray(src.size)
        PresetPixelEffects.sharpenPixelsInto(src, dst, 6, 6, 0.8f)
        assertTrue(dst.contentEquals(src))
    }

    @Test
    fun `锐化强度进入颜色矩阵之外的效果判定`() {
        // sharpnessAmount：与 PresetColorTransform 的换算保持一致（锐度 30 → 0.3）
        val preset = com.silas.omaster.model.MasterPreset(
            coverPath = "", name = "锐度", sharpness = 30
        )
        assertEquals(0.3f, PresetColorTransform.sharpnessAmount(preset), 1e-4f)
        assertNotEquals(0f, PresetColorTransform.sharpnessAmount(preset), 1e-4f)
    }
}
