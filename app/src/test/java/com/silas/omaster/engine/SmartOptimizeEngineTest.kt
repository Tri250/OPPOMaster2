package com.silas.omaster.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * SmartOptimizeEngine 单元测试
 *
 * 测试智能优化引擎的核心功能：
 * - 参数优化算法
 * - 场景适配
 * - 优化强度控制
 */
class SmartOptimizeEngineTest {

    @Test
    fun `optimizeParams returns valid params for default input`() {
        val engine = SmartOptimizeEngine()
        val params = engine.optimizeParams(
            brightness = 0.5f,
            contrast = 0.5f,
            saturation = 0.5f,
            strength = 0.5f
        )

        assertNotNull(params)
        assertTrue(params.brightness >= -50f && params.brightness <= 50f)
        assertTrue(params.contrast >= -100f && params.contrast <= 100f)
        assertTrue(params.saturation >= -100f && params.saturation <= 100f)
    }

    @Test
    fun `optimizeParams increases contrast for low contrast input`() {
        val engine = SmartOptimizeEngine()
        val params = engine.optimizeParams(
            brightness = 0.5f,
            contrast = 0.2f, // 低对比度输入
            saturation = 0.5f,
            strength = 0.7f
        )

        // 低对比度输入应增加对比度
        assertTrue(params.contrast > 0f)
    }

    @Test
    fun `optimizeParams decreases saturation for high saturation input`() {
        val engine = SmartOptimizeEngine()
        val params = engine.optimizeParams(
            brightness = 0.5f,
            contrast = 0.5f,
            saturation = 0.9f, // 高饱和度输入
            strength = 0.5f
        )

        // 高饱和度输入应降低饱和度
        assertTrue(params.saturation < 0f)
    }

    @Test
    fun `optimizeParams respects strength parameter`() {
        val engine = SmartOptimizeEngine()

        val weakParams = engine.optimizeParams(0.5f, 0.5f, 0.5f, strength = 0.2f)
        val strongParams = engine.optimizeParams(0.5f, 0.5f, 0.5f, strength = 0.8f)

        // 强优化应产生更大的参数变化
        val weakChange = kotlin.math.abs(weakParams.contrast) + kotlin.math.abs(weakParams.saturation)
        val strongChange = kotlin.math.abs(strongParams.contrast) + kotlin.math.abs(strongParams.saturation)

        assertTrue(strongChange > weakChange)
    }

    @Test
    fun `adaptToScene returns valid params for portrait`() {
        val engine = SmartOptimizeEngine()
        val params = engine.adaptToScene(
            sceneType = "portrait",
            baseParams = SmartOptimizeParams(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )

        assertNotNull(params)
        // 人像场景通常优化肤色（温暖色调）
        assertTrue(params.warmth >= 0f)
    }

    @Test
    fun `adaptToScene returns valid params for landscape`() {
        val engine = SmartOptimizeEngine()
        val params = engine.adaptToScene(
            sceneType = "landscape",
            baseParams = SmartOptimizeParams(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )

        assertNotNull(params)
        // 风景场景通常增强饱和度和对比度
        assertTrue(params.saturation > 0f || params.contrast > 0f)
    }

    @Test
    fun `adaptToScene handles unknown scene`() {
        val engine = SmartOptimizeEngine()
        val baseParams = SmartOptimizeParams(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val params = engine.adaptToScene("unknown_scene", baseParams)

        // 未知场景应返回基础参数（不崩溃）
        assertNotNull(params)
        assertEquals(baseParams, params)
    }
}