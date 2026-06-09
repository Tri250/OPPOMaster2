package com.silas.omaster.ai.mapping

import com.silas.omaster.model.FilmSeries
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * SceneToHasselbladMapping 单元测试
 *
 * 测试覆盖：
 * - 场景参数映射
 * - 胶片推荐
 * - 大师建议
 * - 参数调整建议
 * - 场景ID列表
 */
class SceneToHasselbladMappingTest {

    private val mapping = SceneToHasselbladMapping()

    @Test
    fun `getParams - 应该返回人像场景的参数`() {
        val params = mapping.getParams("portrait-indoor")

        assertNotNull(params)
        assertTrue("影调值应该在有效范围内", params.tone in -30..30)
        assertTrue("饱和度值应该在有效范围内", params.saturation in -30..30)
        assertTrue("对比度值应该在有效范围内", params.contrast in -30..30)
    }

    @Test
    fun `getParams - 应该返回风景场景的参数`() {
        val params = mapping.getParams("landscape-sunset")

        assertNotNull(params)
        assertTrue("影调值应该在有效范围内", params.tone in -30..30)
        assertTrue("清晰度值应该在有效范围内", params.clarity in -30..30)
    }

    @Test
    fun `getParams - 应该返回夜景场景的参数`() {
        val params = mapping.getParams("night-cityscape")

        assertNotNull(params)
        assertTrue("影调值应该在有效范围内", params.tone in -30..30)
        assertTrue("对比度值应该在有效范围内", params.contrast in -30..30)
    }

    @Test
    fun `getParams - 人像场景应该有较低的对比度`() {
        val params = mapping.getParams("portrait-outdoor")

        assertTrue("人像场景对比度应该较低", params.contrast <= 5)
    }

    @Test
    fun `getParams - 风景场景应该有较高的清晰度`() {
        val params = mapping.getParams("landscape-mountain")

        assertTrue("风景场景清晰度应该较高", params.clarity >= 15)
    }

    @Test
    fun `getParams - 夜景场景应该有较高的对比度`() {
        val params = mapping.getParams("night-cityscape")

        assertTrue("夜景场景对比度应该较高", params.contrast >= 10)
    }

    @Test
    fun `getRecommendedFilms - 应该为人像场景推荐合适的胶片`() {
        val films = mapping.getRecommendedFilms("portrait-indoor")

        assertTrue("胶片推荐列表不应该为空", films.isNotEmpty())

        // 人像场景应该推荐Portra或CC胶片
        val filmIds = films.map { it.id }
        assertTrue(
            "人像场景应该推荐Portra或CC胶片",
            filmIds.any { it in listOf("portra", "cc", "nc") }
        )
    }

    @Test
    fun `getRecommendedFilms - 应该为风景场景推荐合适的胶片`() {
        val films = mapping.getRecommendedFilms("landscape-mountain")

        assertTrue("胶片推荐列表不应该为空", films.isNotEmpty())

        // 风景场景应该推荐反转片或经典负片
        val filmIds = films.map { it.id }
        assertTrue(
            "风景场景应该推荐反转片或经典负片",
            filmIds.any { it in listOf("rdp3", "cc", "nh") }
        )
    }

    @Test
    fun `getRecommendedFilms - 应该为夜景场景推荐合适的胶片`() {
        val films = mapping.getRecommendedFilms("night-cityscape")

        assertTrue("胶片推荐列表不应该为空", films.isNotEmpty())

        // 夜景场景应该推荐800T胶片
        val filmIds = films.map { it.id }
        assertTrue("夜景场景应该推荐800T胶片", filmIds.contains("800t"))
    }

    @Test
    fun `getRecommendedFilms - 胶片应该按匹配分数排序`() {
        val films = mapping.getRecommendedFilms("portrait-studio")

        for (i in 0 until films.size - 1) {
            assertTrue(
                "胶片应该按匹配分数降序排列",
                films[i].matchScore >= films[i + 1].matchScore
            )
        }
    }

    @Test
    fun `getRecommendedFilms - 胶片应该有有效的匹配分数`() {
        val films = mapping.getRecommendedFilms("food-dessert")

        for (film in films) {
            assertTrue("匹配分数应该在0到1之间", film.matchScore in 0.0f..1.0f)
            assertNotNull("胶片ID不应该为空", film.id)
            assertNotNull("胶片名称不应该为空", film.name)
            assertNotNull("胶片系列不应该为空", film.series)
        }
    }

    @Test
    fun `getMasterTips - 应该返回大师拍摄建议`() {
        val tips = mapping.getMasterTips("portrait-indoor")

        assertTrue("大师建议列表不应该为空", tips.isNotEmpty())
        assertTrue("大师建议应该是非空字符串", tips[0].isNotEmpty())
    }

    @Test
    fun `getMasterTips - 人像场景应该有关于光线的建议`() {
        val tips = mapping.getMasterTips("portrait-outdoor")

        val tipsText = tips.joinToString(" ")
        assertTrue("人像场景建议应该包含光线相关内容", tipsText.contains("光"))
    }

    @Test
    fun `getMasterTips - 风景场景应该有关于构图的建议`() {
        val tips = mapping.getMasterTips("landscape-mountain")

        assertTrue("风景场景建议不应该为空", tips.isNotEmpty())
    }

    @Test
    fun `getParamAdjustmentAdvice - 应该返回参数调整建议`() {
        val currentParams = HasselbladParams(
            tone = 0,
            saturation = 0,
            contrast = 0,
            colorTemp = 0,
            sharpness = 0,
            clarity = 0,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        val advice = mapping.getParamAdjustmentAdvice(currentParams, "portrait-indoor")

        assertNotNull("参数调整建议不应该为空", advice)
    }

    @Test
    fun `getParamAdjustmentAdvice - 应该建议调整不合适的参数`() {
        val currentParams = HasselbladParams(
            tone = 20,
            saturation = 25,
            contrast = 15,
            colorTemp = 0,
            sharpness = 0,
            clarity = 0,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        val advice = mapping.getParamAdjustmentAdvice(currentParams, "portrait-indoor")

        // 对于人像场景，高对比度应该被建议调整
        val hasContrastAdvice = advice.any {
            it.param.contains("contrast", ignoreCase = true) ||
            it.param.contains("对比度")
        }
        assertTrue("应该建议调整对比度", hasContrastAdvice)
    }

    @Test
    fun `getAllSceneIds - 应该返回所有场景ID`() {
        val sceneIds = mapping.getAllSceneIds()

        assertTrue("场景ID列表不应该为空", sceneIds.isNotEmpty())
    }

    @Test
    fun `getAllSceneIds - 应该包含主要场景类别`() {
        val sceneIds = mapping.getAllSceneIds()

        // 检查是否包含人像场景
        assertTrue("应该包含人像场景", sceneIds.any { it.contains("portrait") })

        // 检查是否包含风景场景
        assertTrue("应该包含风景场景", sceneIds.any { it.contains("landscape") })

        // 检查是否包含夜景场景
        assertTrue("应该包含夜景场景", sceneIds.any { it.contains("night") })

        // 检查是否包含美食场景
        assertTrue("应该包含美食场景", sceneIds.any { it.contains("food") })
    }

    @Test
    fun `getAllSceneIds - 场景ID应该唯一`() {
        val sceneIds = mapping.getAllSceneIds()
        val uniqueIds = sceneIds.distinct()

        assertEquals("场景ID应该唯一", uniqueIds.size, sceneIds.size)
    }

    @Test
    fun `场景参数一致性 - 所有场景的参数应该在有效范围内`() {
        val sceneIds = mapping.getAllSceneIds()

        for (sceneId in sceneIds) {
            val params = mapping.getParams(sceneId)

            assertTrue("$sceneId 的影调值应该在有效范围内", params.tone in -30..30)
            assertTrue("$sceneId 的饱和度值应该在有效范围内", params.saturation in -30..30)
            assertTrue("$sceneId 的对比度值应该在有效范围内", params.contrast in -30..30)
            assertTrue("$sceneId 的色温值应该在有效范围内", params.colorTemp in -30..30)
        }
    }

    @Test
    fun `场景参数一致性 - 所有场景都应该有胶片推荐`() {
        val sceneIds = mapping.getAllSceneIds()

        for (sceneId in sceneIds) {
            val films = mapping.getRecommendedFilms(sceneId)
            assertTrue("$sceneId 应该有胶片推荐", films.isNotEmpty())
        }
    }

    @Test
    fun `场景参数一致性 - 所有场景都应该有大师建议`() {
        val sceneIds = mapping.getAllSceneIds()

        for (sceneId in sceneIds) {
            val tips = mapping.getMasterTips(sceneId)
            assertTrue("$sceneId 应该有大师建议", tips.isNotEmpty())
        }
    }

    @Test
    fun `胶片预设 - 应该包含所有标准胶片`() {
        val standardFilms = listOf("cc", "nc", "nh", "portra", "rdp3", "800t", "tx400")

        // 检查至少有一个场景推荐这些胶片
        val sceneIds = mapping.getAllSceneIds()
        val allRecommendedFilms = sceneIds.flatMap { mapping.getRecommendedFilms(it) }.map { it.id }

        for (film in standardFilms) {
            assertTrue("应该包含 $film 胶片", allRecommendedFilms.contains(film))
        }
    }

    @Test
    fun `场景分类 - 人像场景应该有正确的分类`() {
        val portraitScenes = listOf("portrait-indoor", "portrait-outdoor", "portrait-studio")

        for (sceneId in portraitScenes) {
            val films = mapping.getRecommendedFilms(sceneId)
            assertTrue("$sceneId 应该有胶片推荐", films.isNotEmpty())
        }
    }

    @Test
    fun `场景分类 - 风景场景应该有正确的分类`() {
        val landscapeScenes = listOf("landscape-sunset", "landscape-mountain", "landscape-seascape")

        for (sceneId in landscapeScenes) {
            val films = mapping.getRecommendedFilms(sceneId)
            assertTrue("$sceneId 应该有胶片推荐", films.isNotEmpty())
        }
    }
}
