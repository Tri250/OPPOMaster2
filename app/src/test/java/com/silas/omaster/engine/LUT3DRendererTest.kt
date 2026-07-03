package com.silas.omaster.engine

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/**
 * LUT3DRenderer 单元测试
 *
 * 测试3D LUT渲染器的核心功能：
 * - LUT 应用
 * - 颜色转换
 * - 性能优化
 */
class LUT3DRendererTest {

    @Test
    fun `applyLUT returns valid bitmap`() {
        val renderer = LUT3DRenderer()
        val inputBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        inputBitmap.eraseColor(android.graphics.Color.RED)

        // 创建模拟 LUT 数据（33x33x33）
        val lutData = FloatArray(33 * 33 * 33 * 3) { i ->
            when {
                i % 3 == 0 -> 1.0f // R
                i % 3 == 1 -> 0.0f // G
                else -> 0.0f       // B
            }
        }

        val outputBitmap = renderer.applyLUT(inputBitmap, lutData, 33)

        assertNotNull(outputBitmap)
        assertTrue(outputBitmap.width == inputBitmap.width)
        assertTrue(outputBitmap.height == inputBitmap.height)

        inputBitmap.recycle()
        outputBitmap.recycle()
    }

    @Test
    fun `identity LUT preserves colors`() {
        val renderer = LUT3DRenderer()
        val inputBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        inputBitmap.eraseColor(android.graphics.Color.valueOf(0.5f, 0.6f, 0.7f))

        // 创建单位 LUT（无颜色变化）
        val identityLUT = FloatArray(33 * 33 * 33 * 3) { i ->
            val lutIndex = i / 3
            val dimension = lutIndex / (33 * 33)
            val remainder = lutIndex % (33 * 33)
            val row = remainder / 33
            val col = remainder % 33

            // 单位 LUT：输出颜色 = 输入颜色
            when (i % 3) {
                0 -> col / 32.0f  // R
                1 -> row / 32.0f  // G
                2 -> dimension / 32.0f // B
                else -> 0.0f
            }
        }

        val outputBitmap = renderer.applyLUT(inputBitmap, identityLUT, 33)

        // 单位 LUT 应保持原始颜色
        val inputPixel = inputBitmap.getPixel(25, 25)
        val outputPixel = outputBitmap.getPixel(25, 25)

        assertEquals(inputPixel, outputPixel)

        inputBitmap.recycle()
        outputBitmap.recycle()
    }

    @Test
    fun `lutSize validation`() {
        val renderer = LUT3DRenderer()

        // 验证支持的 LUT 尺寸
        assertTrue(renderer.isValidLUTSize(17))
        assertTrue(renderer.isValidLUTSize(33))
        assertTrue(renderer.isValidLUTSize(65))

        // 验证不支持的 LUT 尺寸
        assertFalse(renderer.isValidLUTSize(16))
        assertFalse(renderer.isValidLUTSize(32))
        assertFalse(renderer.isValidLUTSize(0))
    }

    @Test
    fun `lutDataSize validation`() {
        val renderer = LUT3DRenderer()

        // 33^3 * 3 = 35937
        val validSize = 33 * 33 * 33 * 3
        assertTrue(renderer.isValidLUTDataSize(validSize, 33))

        assertFalse(renderer.isValidLUTDataSize(validSize - 1, 33))
        assertFalse(renderer.isValidLUTDataSize(validSize + 1, 33))
    }
}