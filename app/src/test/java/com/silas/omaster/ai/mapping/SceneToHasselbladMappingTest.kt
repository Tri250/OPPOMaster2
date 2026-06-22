package com.silas.omaster.ai.mapping

import com.silas.omaster.model.SoftLightMode
import org.junit.Assert.*
import org.junit.Test

/**
 * SceneToHasselbladMapping 单元测试
 * 验证场景到哈苏参数的映射逻辑
 */
class SceneToHasselbladMappingTest {

    @Test
    fun `人像场景 - 应返回人像优化参数`() {
        val params = SceneToHasselbladMapping.getParams("portrait")
        assertTrue("人像饱和度应偏低或适中", params.saturation in -20..20)
        assertTrue("人像应带柔光或梦幻效果", params.softLight != SoftLightMode.NONE || params.sceneRelatedToPortrait())
    }

    @Test
    fun `风景场景 - 应提升饱和度和锐度`() {
        val params = SceneToHasselbladMapping.getParams("landscape")
        assertTrue("风景饱和度应提升", params.saturation >= 10)
        assertTrue("风景锐度应提升", params.sharpness >= 10)
    }

    @Test
    fun `夜景场景 - 应增强对比度和暗角`() {
        val params = SceneToHasselbladMapping.getParams("night")
        assertTrue("夜景对比度应增强", params.contrast >= 10)
        assertTrue("夜景暗角应加深", params.vignette >= 10)
    }

    @Test
    fun `未知场景 - 应返回默认安全参数`() {
        val params = SceneToHasselbladMapping.getParams("unknown-scene")
        assertEquals(0, params.tone)
        assertEquals(0, params.saturation)
        assertEquals(0, params.contrast)
        assertEquals(SoftLightMode.NONE, params.softLight)
    }

    @Test
    fun `黑白人像 - 应大幅降低饱和度`() {
        val params = SceneToHasselbladMapping.getParams("portrait-bw")
        assertTrue("黑白人像饱和度应极低", params.saturation <= -20)
    }

    @Test
    fun `日落风景 - 应偏暖色调`() {
        val params = SceneToHasselbladMapping.getParams("landscape-sunset")
        assertTrue("日落色温应偏暖", params.colorTemp > 0)
        assertTrue("日落饱和度应提升", params.saturation >= 15)
    }

    @Test
    fun `雪景 - 应保持低饱和偏冷`() {
        val params = SceneToHasselbladMapping.getParams("landscape-snow")
        assertTrue("雪景饱和度应降低或微调", params.saturation <= 5)
    }

    @Test
    fun `参数范围 - 所有返回值应在安全区间`() {
        val scenes = listOf(
            "portrait", "landscape", "night", "food", "street",
            "portrait-backlit", "landscape-sunset", "night-city"
        )
        for (scene in scenes) {
            val params = SceneToHasselbladMapping.getParams(scene)
            assertTrue("$scene 影调超出范围", params.tone in -30..30)
            assertTrue("$scene 饱和度超出范围", params.saturation in -30..30)
            assertTrue("$scene 对比度超出范围", params.contrast in -30..30)
            assertTrue("$scene 色温超出范围", params.colorTemp in -30..30)
            assertTrue("$scene 锐度超出范围", params.sharpness in -30..30)
            assertTrue("$scene 暗角超出范围", params.vignette in -30..30)
        }
    }

    private fun com.silas.omaster.model.HasselbladParams.sceneRelatedToPortrait(): Boolean {
        // 人像场景通常使用柔光或轻微暗角
        return softLight != SoftLightMode.NONE || vignette > 0
    }
}
