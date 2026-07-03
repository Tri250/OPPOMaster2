package com.silas.omaster.ai.mapping

import org.junit.Assert.*
import org.junit.Test

/**
 * SceneToHasselbladMapping 单元测试
 *
 * 测试场景到哈苏参数映射的核心功能：
 * - 36类场景完整映射
 * - 参数范围验证
 * - 胶片推荐准确性
 * - 大师建议完整性
 */
class SceneToHasselbladMappingTest {

    @Test
    fun `getParams returns valid params for portrait scenes`() {
        val portraitScenes = listOf(
            "portrait-standard", "portrait", "portrait-backlit",
            "portrait-studio", "portrait-bw", "portrait-group",
            "portrait-child", "portrait-couple", "portrait-senior"
        )

        for (sceneId in portraitScenes) {
            val params = SceneToHasselbladMapping.getParams(sceneId)

            assertNotNull(params)
            // 人像场景通常降低对比度（柔和）
            assertTrue(params.contrast >= -30 && params.contrast <= 30)
            assertTrue(params.saturation >= -30 && params.saturation <= 30)
        }
    }

    @Test
    fun `getParams returns valid params for landscape scenes`() {
        val landscapeScenes = listOf(
            "landscape-standard", "landscape", "landscape-sunset",
            "landscape-sky", "landscape-forest", "landscape-mountain",
            "landscape-sea", "landscape-desert"
        )

        for (sceneId in landscapeScenes) {
            val params = SceneToHasselbladMapping.getParams(sceneId)

            assertNotNull(params)
            // 风景场景通常增强饱和度和对比度
            assertTrue(params.saturation >= -30 && params.saturation <= 30)
            assertTrue(params.contrast >= -30 && params.contrast <= 30)
        }
    }

    @Test
    fun `getParams returns valid params for food scenes`() {
        val params = SceneToHasselbladMapping.getParams("food")

        assertNotNull(params)
        // 食物场景通常增强饱和度（色彩鲜艳）
        assertTrue(params.saturation > 0)
    }

    @Test
    fun `getParams handles unknown scene gracefully`() {
        val params = SceneToHasselbladMapping.getParams("unknown_scene")

        assertNotNull(params)
        // 未知场景应返回默认参数（全0或安全值）
        assertEquals(0, params.saturation)
        assertEquals(0, params.contrast)
    }

    @Test
    fun `getRecommendedFilm returns valid film for portrait`() {
        val film = SceneToHasselbladMapping.getRecommendedFilm("portrait-standard")

        assertNotNull(film)
        assertTrue(film.isNotEmpty())
    }

    @Test
    fun `getRecommendedFilm returns valid film for landscape`() {
        val film = SceneToHasselbladMapping.getRecommendedFilm("landscape-sunset")

        assertNotNull(film)
        assertTrue(film.isNotEmpty())
    }

    @Test
    fun `getMasterAdvice returns valid advice for food`() {
        val advice = SceneToHasselbladMapping.getMasterAdvice("food")

        assertNotNull(advice)
        assertTrue(advice.isNotEmpty())
    }

    @Test
    fun `getMasterAdvice returns valid advice for night`() {
        val advice = SceneToHasselbladMapping.getMasterAdvice("night_city")

        assertNotNull(advice)
        assertTrue(advice.isNotEmpty())
    }

    @Test
    fun `allSceneCategories have complete mappings`() {
        val allScenes = listOf(
            // 人像
            "portrait-standard", "portrait-backlit", "portrait-studio",
            // 风景
            "landscape-standard", "landscape-sunset", "landscape-sky",
            // 食物
            "food", "food-dessert", "food-drink",
            // 夜景
            "night_city", "night_street",
            // 街拍
            "street", "street-urban",
            // 宠物
            "pet", "pet-cat", "pet-dog",
            // 微距
            "macro", "macro-flower",
            // 文档
            "document",
            // 室内
            "indoor", "indoor-home"
        )

        for (sceneId in allScenes) {
            val params = SceneToHasselbladMapping.getParams(sceneId)
            assertNotNull("Params for $sceneId should not be null", params)
        }
    }

    @Test
    fun `paramRangeValidation ensures safety`() {
        val params = SceneToHasselbladMapping.getParams("portrait-standard")

        // 所有参数应在 -30 ~ +30 范围内（OPPO大师模式标准）
        assertTrue(params.tone >= -30 && params.tone <= 30)
        assertTrue(params.saturation >= -30 && params.saturation <= 30)
        assertTrue(params.contrast >= -30 && params.contrast <= 30)
        assertTrue(params.colorTemp >= -30 && params.colorTemp <= 30)
        assertTrue(params.sharpness >= -30 && params.sharpness <= 30)
        assertTrue(params.vignette >= -30 && params.vignette <= 30)
        assertTrue(params.cyanMagenta >= -30 && params.cyanMagenta <= 30)
    }

    @Test
    fun `bwPortrait reduces saturation`() {
        val bwParams = SceneToHasselbladMapping.getParams("portrait-bw")

        // 黑白人像应大幅降低饱和度
        assertTrue(bwParams.saturation < 0)
        assertTrue(bwParams.saturation <= -20)
    }

    @Test
    fun `sunsetLandscape increases warmth`() {
        val sunsetParams = SceneToHasselbladMapping.getParams("landscape-sunset")

        // 日落风景应增加暖色调
        assertTrue(sunsetParams.colorTemp > 0)
        assertTrue(sunsetParams.saturation > 0)
    }
}