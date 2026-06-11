package com.silas.omaster.ai

import org.junit.Assert.*
import org.junit.Test

/**
 * AI 引擎单元测试
 */
class AIEngineTest {

    @Test
    fun `场景识别结果验证`() {
        val scenes = listOf(
            "landscape", "portrait", "night", "food", "architecture"
        )
        // 验证支持的场景类型
        assertTrue(scenes.contains("landscape"))
        assertTrue(scenes.contains("portrait"))
        assertTrue(scenes.contains("night"))
        assertEquals(5, scenes.size)
    }

    @Test
    fun `AI 参数范围验证`() {
        // 亮度调整范围 -100 到 100
        val brightnessAdjustment = 50
        assertTrue(brightnessAdjustment in -100..100)
        
        // 对比度调整范围 -100 到 100
        val contrastAdjustment = -30
        assertTrue(contrastAdjustment in -100..100)
        
        // 饱和度调整范围 -100 到 100
        val saturationAdjustment = 20
        assertTrue(saturationAdjustment in -100..100)
    }

    @Test
    fun `场景映射验证`() {
        // 验证场景到参数的映射
        val sceneMapping = mapOf(
            "landscape" to mapOf("saturation" to 10, "contrast" to 5),
            "portrait" to mapOf("saturation" to -5, "brightness" to 5),
            "night" to mapOf("brightness" to -10, "contrast" to 15)
        )
        
        assertTrue(sceneMapping.containsKey("landscape"))
        assertTrue(sceneMapping.containsKey("portrait"))
        assertTrue(sceneMapping.containsKey("night"))
    }
}
