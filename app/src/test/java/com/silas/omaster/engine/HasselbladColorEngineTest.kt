package com.silas.omaster.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * HasselbladColorEngine 单元测试
 *
 * 测试哈苏色彩引擎的核心功能：
 * - 色彩矩阵生成
 * - 参数验证
 * - 预设加载
 */
class HasselbladColorEngineTest {

    @Test
    fun `generateColorMatrix returns valid matrix for default params`() {
        val params = HasselbladParams(
            saturation = 0f,
            contrast = 0f,
            warmth = 0f,
            brightness = 0f
        )

        val matrix = HasselbladColorEngine.generateColorMatrix(params)

        // 默认参数应生成单位矩阵（无色彩变化）
        assertNotNull(matrix)
        assertEquals(5, matrix.length)
        assertEquals(5, matrix[0].length)

        // 验证单位矩阵特性
        assertEquals(1.0f, matrix[0][0], 0.01f) // R
        assertEquals(1.0f, matrix[1][1], 0.01f) // G
        assertEquals(1.0f, matrix[2][2], 0.01f) // B
        assertEquals(1.0f, matrix[3][3], 0.01f) // A
    }

    @Test
    fun `generateColorMatrix increases saturation`() {
        val params = HasselbladParams(
            saturation = 50f, // 增加饱和度
            contrast = 0f,
            warmth = 0f,
            brightness = 0f
        )

        val matrix = HasselbladColorEngine.generateColorMatrix(params)

        // 验证饱和度矩阵影响 RGB 通道
        assertTrue(matrix[0][0] > 1.0f) // R 增强红色
        assertTrue(matrix[1][1] > 1.0f) // G 增强绿色
        assertTrue(matrix[2][2] > 1.0f) // B 增强蓝色
    }

    @Test
    fun `generateColorMatrix adjusts warmth`() {
        val warmParams = HasselbladParams(
            saturation = 0f,
            contrast = 0f,
            warmth = 30f, // 暖色调
            brightness = 0f
        )

        val coolParams = HasselbladParams(
            saturation = 0f,
            contrast = 0f,
            warmth = -30f, // 冷色调
            brightness = 0f
        )

        val warmMatrix = HasselbladColorEngine.generateColorMatrix(warmParams)
        val coolMatrix = HasselbladColorEngine.generateColorMatrix(coolParams)

        // 暖色调应增强红色，冷色调应增强蓝色
        assertTrue(warmMatrix[0][0] > coolMatrix[0][0])
        assertTrue(warmMatrix[2][2] < coolMatrix[2][2])
    }

    @Test
    fun `generateColorMatrix adjusts contrast`() {
        val highContrast = HasselbladParams(
            saturation = 0f,
            contrast = 50f,
            warmth = 0f,
            brightness = 0f
        )

        val matrix = HasselbladColorEngine.generateColorMatrix(highContrast)

        // 高对比度应影响 RGB 偏移
        assertNotNull(matrix)
        assertTrue(matrix[4][0] != 0f || matrix[4][1] != 0f || matrix[4][2] != 0f)
    }

    @Test
    fun `clampParams ensures valid ranges`() {
        val outOfRangeParams = HasselbladParams(
            saturation = 150f, // 超出范围
            contrast = -120f,  // 超出范围
            warmth = 200f,     // 超出范围
            brightness = -50f  // 超出范围
        )

        val clamped = HasselbladColorEngine.clampParams(outOfRangeParams)

        // 验证参数被限制在有效范围内
        assertTrue(clamped.saturation >= -100f && clamped.saturation <= 100f)
        assertTrue(clamped.contrast >= -100f && clamped.contrast <= 100f)
        assertTrue(clamped.warmth >= -100f && clamped.warmth <= 100f)
        assertTrue(clamped.brightness >= -50f && clamped.brightness <= 50f)
    }
}