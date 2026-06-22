package com.silas.omaster.renderer

import org.junit.Assert.*
import org.junit.Test

/**
 * RenderParameters 单元测试
 * 覆盖参数转换、合并、插值等核心逻辑
 */
class RenderParametersTest {

    @Test
    fun `默认参数 - 所有字段应为0`() {
        val params = RenderParameters.DEFAULT
        assertEquals(0f, params.saturation, 0.001f)
        assertEquals(0f, params.contrast, 0.001f)
        assertEquals(0f, params.brightness, 0.001f)
        assertEquals(0f, params.sharpness, 0.001f)
        assertFalse(params.hasAnyAdjustment())
    }

    @Test
    fun `hasAnyAdjustment - 任一参数非零应返回true`() {
        assertTrue(RenderParameters(brightness = 10f).hasAnyAdjustment())
        assertTrue(RenderParameters(saturation = -5f).hasAnyAdjustment())
        assertTrue(RenderParameters(sharpness = 1f).hasAnyAdjustment())
    }

    @Test
    fun `hasAnyAdjustment - 全零参数应返回false`() {
        assertFalse(RenderParameters().hasAnyAdjustment())
    }

    @Test
    fun `toMap与fromMap - 应互为逆操作`() {
        val params = RenderParameters(
            saturation = 10f,
            contrast = -20f,
            brightness = 15f,
            sharpness = 25f,
            skinSmooth = 30f
        )
        val map = params.toMap()
        val restored = RenderParameters.fromMap(map)
        assertEquals(params, restored)
    }

    @Test
    fun `fromMap - 缺失字段应默认为0`() {
        val map = mapOf("saturation" to 50f)
        val params = RenderParameters.fromMap(map)
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(0f, params.contrast, 0.001f)
        assertEquals(0f, params.brightness, 0.001f)
    }

    @Test
    fun `fromIntMap - 整数值应正确转换为Float`() {
        val map = mapOf(
            "saturation" to 10,
            "contrast" to -20,
            "sharpness" to 30
        )
        val params = RenderParameters.fromIntMap(map)
        assertEquals(10f, params.saturation, 0.001f)
        assertEquals(-20f, params.contrast, 0.001f)
        assertEquals(30f, params.sharpness, 0.001f)
    }

    @Test
    fun `toShaderUniforms - 应归一化到-1到1范围`() {
        val params = RenderParameters(
            saturation = 100f,
            contrast = -100f,
            brightness = 50f,
            sharpness = 100f
        )
        val uniforms = params.toShaderUniforms()
        assertEquals(1f, uniforms[0], 0.001f)   // saturation
        assertEquals(-1f, uniforms[1], 0.001f)  // contrast
        assertEquals(0.5f, uniforms[2], 0.001f) // brightness
        assertEquals(1f, uniforms[4], 0.001f)   // sharpness
    }

    @Test
    fun `toShaderUniforms - 数组长度应为18`() {
        val uniforms = RenderParameters().toShaderUniforms()
        assertEquals(18, uniforms.size)
    }

    @Test
    fun `nonZeroCount - 应正确统计非零参数`() {
        assertEquals(0, RenderParameters().nonZeroCount())
        assertEquals(1, RenderParameters(brightness = 10f).nonZeroCount())
        assertEquals(3, RenderParameters(
            saturation = 10f,
            contrast = -5f,
            sharpness = 20f
        ).nonZeroCount())
    }

    @Test
    fun `merge - 当前非零参数应覆盖目标参数`() {
        val base = RenderParameters(saturation = 10f, contrast = 20f, brightness = 5f)
        val overlay = RenderParameters(saturation = 30f)
        val merged = overlay.merge(base)
        assertEquals(30f, merged.saturation, 0.001f)
        assertEquals(20f, merged.contrast, 0.001f)
        assertEquals(5f, merged.brightness, 0.001f)
    }

    @Test
    fun `lerp - 应在两个参数间正确插值`() {
        val start = RenderParameters(brightness = 0f)
        val end = RenderParameters(brightness = 100f)
        val mid = start.lerp(end, 0.5f)
        assertEquals(50f, mid.brightness, 0.001f)
    }

    @Test
    fun `lerp - t应被限制在0到1之间`() {
        val start = RenderParameters(brightness = 0f)
        val end = RenderParameters(brightness = 100f)
        assertEquals(0f, start.lerp(end, -1f).brightness, 0.001f)
        assertEquals(100f, start.lerp(end, 2f).brightness, 0.001f)
    }

    @Test
    fun `参数元数据 - 应包含18个参数定义`() {
        assertEquals(18, RenderParameters.PARAM_METADATA.size)
        val keys = RenderParameters.PARAM_METADATA.map { it.key }.toSet()
        assertTrue(keys.contains("saturation"))
        assertTrue(keys.contains("sharpness"))
        assertTrue(keys.contains("skinSmooth"))
    }
}
