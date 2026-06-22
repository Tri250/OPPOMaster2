package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.UndoRedoManager
import com.silas.omaster.util.formatFilterWithIntensity
import com.silas.omaster.util.formatPercent
import com.silas.omaster.util.formatSigned
import org.junit.Assert.*
import org.junit.Test

/**
 * 功能测试
 * 验证各核心功能模块的正确行为
 */
class FunctionalTest {

    // ===== 1. 渲染参数功能 =====

    @Test
    fun `渲染参数 - 默认值应为零`() {
        val params = RenderParameters.DEFAULT
        assertFalse(params.hasAnyAdjustment())
        assertEquals(0, params.nonZeroCount())
    }

    @Test
    fun `渲染参数 - fromMap应正确解析所有字段`() {
        val map = mapOf(
            "saturation" to 50f,
            "contrast" to -30f,
            "brightness" to 20f,
            "sharpness" to 80f,
            "grain" to 40f
        )
        val params = RenderParameters.fromMap(map)
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(-30f, params.contrast, 0.001f)
        assertEquals(20f, params.brightness, 0.001f)
        assertEquals(80f, params.sharpness, 0.001f)
        assertEquals(40f, params.grain, 0.001f)
        // 未提供字段应为0
        assertEquals(0f, params.clarity, 0.001f)
    }

    @Test
    fun `渲染参数 - toShaderUniforms应正确归一化`() {
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
    fun `渲染参数 - merge应让非零参数覆盖目标`() {
        val base = RenderParameters(saturation = 10f, contrast = 20f, brightness = 5f)
        val overlay = RenderParameters(saturation = 30f)
        val merged = overlay.merge(base)
        assertEquals(30f, merged.saturation, 0.001f)
        assertEquals(20f, merged.contrast, 0.001f)
        assertEquals(5f, merged.brightness, 0.001f)
    }

    @Test
    fun `渲染参数 - lerp应在两端正确插值`() {
        val start = RenderParameters(saturation = 0f, brightness = 0f)
        val end = RenderParameters(saturation = 100f, brightness = 100f)

        val mid = start.lerp(end, 0.5f)
        assertEquals(50f, mid.saturation, 0.001f)
        assertEquals(50f, mid.brightness, 0.001f)

        val begin = start.lerp(end, 0f)
        assertEquals(0f, begin.saturation, 0.001f)

        val finish = start.lerp(end, 1f)
        assertEquals(100f, finish.saturation, 0.001f)
    }

    @Test
    fun `渲染参数 - lerp应钳制t到0到1范围`() {
        val start = RenderParameters(saturation = 0f)
        val end = RenderParameters(saturation = 100f)

        val over = start.lerp(end, 2f)
        assertEquals(100f, over.saturation, 0.001f)

        val under = start.lerp(end, -1f)
        assertEquals(0f, under.saturation, 0.001f)
    }

    // ===== 2. 撤销重做功能 =====

    @Test
    fun `撤销重做 - 基本undo redo流程`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(1)
        manager.pushState(2)
        manager.pushState(3)

        var current = 4
        current = manager.undo(current) ?: current
        assertEquals(3, current)

        current = manager.undo(current) ?: current
        assertEquals(2, current)

        current = manager.redo(current) ?: current
        assertEquals(3, current)
    }

    @Test
    fun `撤销重做 - 新操作后重做栈应清空`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(1)
        manager.pushState(2)

        var current = 3
        current = manager.undo(current) ?: current
        assertEquals(2, current)

        manager.pushState(4)
        assertFalse(manager.canRedo())
    }

    @Test
    fun `撤销重做 - 空栈时undo redo应返回null`() {
        val manager = UndoRedoManager<String>()
        assertNull(manager.undo("current"))
        assertNull(manager.redo("current"))
    }

    // ===== 3. 场景映射功能 =====

    @Test
    fun `场景映射 - 所有人像场景应返回合理参数`() {
        val portraitScenes = listOf(
            "portrait", "portrait-standard", "portrait-backlit", "portrait-studio",
            "portrait-bw", "portrait-group", "portrait-child", "portrait-couple", "portrait-senior"
        )
        for (sceneId in portraitScenes) {
            val params = SceneToHasselbladMapping.getParams(sceneId)
            assertTrue("$sceneId 影调应在范围内", params.tone in -30..30)
            assertTrue("$sceneId 饱和度应在范围内", params.saturation in -30..30)
            assertTrue("$sceneId 对比度应在范围内", params.contrast in -30..30)
        }
    }

    @Test
    fun `场景映射 - 日落风景应偏暖色调`() {
        val params = SceneToHasselbladMapping.getParams("landscape-sunset")
        assertTrue("日落色温应偏暖", params.colorTemp > 0)
        assertTrue("日落饱和度应提升", params.saturation >= 15)
    }

    @Test
    fun `场景映射 - 黑白人像饱和度应大幅降低`() {
        val params = SceneToHasselbladMapping.getParams("portrait-bw")
        assertTrue("黑白人像饱和度应很低", params.saturation <= -20)
    }

    @Test
    fun `场景映射 - 推荐胶片应非空`() {
        val scenes = listOf("portrait", "landscape", "night", "food", "street", "macro", "still-flower", "event-wedding")
        for (sceneId in scenes) {
            val films = SceneToHasselbladMapping.getRecommendedFilms(sceneId)
            assertTrue("$sceneId 应有胶片推荐", films.isNotEmpty())
            assertTrue("$sceneId 首条胶片匹配度应较高", films.first().matchScore >= 0.5f)
        }
    }

    @Test
    fun `场景映射 - 大师建议应包含4条`() {
        val tips = SceneToHasselbladMapping.getMasterTips("landscape-sunset")
        assertEquals(4, tips.size)
        for (tip in tips) {
            assertTrue("建议不应为空", tip.isNotBlank())
        }
    }

    @Test
    fun `场景映射 - 未知场景应返回默认参数`() {
        val params = SceneToHasselbladMapping.getParams("unknown-scene")
        assertEquals(HasselbladParams(), params)
    }

    // ===== 4. 格式化工具功能 =====

    @Test
    fun `格式化 - 正负数符号`() {
        assertEquals("+5", 5.formatSigned())
        assertEquals("-5", (-5).formatSigned())
        assertEquals("0", 0.formatSigned())
    }

    @Test
    fun `格式化 - 百分比`() {
        assertEquals("75%", 0.75f.formatPercent())
        assertEquals("0%", 0f.formatPercent())
        assertEquals("100%", 1f.formatPercent())
    }

    @Test
    fun `格式化 - 滤镜强度`() {
        assertEquals("标准", formatFilterWithIntensity("标准", 80))
        assertEquals("复古 80%", formatFilterWithIntensity("复古", 80))
        assertEquals("胶片 0%", formatFilterWithIntensity("胶片", 0))
    }

    // ===== 5. 胶片预设功能 =====

    @Test
    fun `胶片预设 - 9款原生胶片应都存在`() {
        val filmIds = listOf("cc", "nc", "nh", "portra", "rdp3", "800t", "tx400", "ccd_cool", "ccd_warm")
        for (id in filmIds) {
            val film = FilmPresets.getFilmById(id)
            assertNotNull("胶片 $id 应存在", film)
            assertTrue("$id 应有名称", film!!.name.isNotBlank())
        }
    }

    @Test
    fun `胶片预设 - 按系列分组应完整`() {
        val classic = FilmPresets.getFilmsBySeries(FilmSeries.CLASSIC)
        val emotion = FilmPresets.getFilmsBySeries(FilmSeries.EMOTION)
        val structure = FilmPresets.getFilmsBySeries(FilmSeries.STRUCTURE)
        val digital = FilmPresets.getFilmsBySeries(FilmSeries.DIGITAL)

        assertEquals(3, classic.size)
        assertEquals(2, emotion.size)
        assertEquals(2, structure.size)
        assertEquals(2, digital.size)
    }

    // ===== 6. 场景预设功能 =====

    @Test
    fun `场景预设 - 数量应大于50`() {
        assertTrue("场景数量应大于50", ScenePresets.allScenes.size >= 50)
    }

    @Test
    fun `场景预设 - 每个场景ID唯一`() {
        val ids = ScenePresets.allScenes.map { it.id }
        assertEquals("场景ID应唯一", ids.size, ids.toSet().size)
    }

    @Test
    fun `场景预设 - 每个场景都有名称和描述`() {
        for (scene in ScenePresets.allScenes) {
            assertTrue("${scene.id} 名称不应为空", scene.name.isNotBlank())
            assertTrue("${scene.id} 描述不应为空", scene.description.isNotBlank())
            assertTrue("${scene.id} 应有胶片推荐", scene.recommendedFilm.isNotEmpty())
            assertTrue("${scene.id} 应有大师建议", scene.masterTips.isNotEmpty())
        }
    }

    @Test
    fun `场景预设 - 按类别查询应返回对应场景`() {
        val portraits = ScenePresets.getScenesByCategory(SceneCategory.PORTRAIT)
        assertTrue("人像场景应存在", portraits.isNotEmpty())
        assertTrue("所有返回场景应为人像类别", portraits.all { it.category == SceneCategory.PORTRAIT })
    }

    @Test
    fun `场景预设 - 通过ID查询应返回正确场景`() {
        val scene = ScenePresets.getSceneById("landscape-sunset")
        assertNotNull(scene)
        assertEquals("日落", scene!!.name)
        assertEquals(SceneCategory.LANDSCAPE, scene.category)
    }

    // ===== 7. 哈苏参数范围验证 =====

    @Test
    fun `哈苏参数 - 默认参数应为中性`() {
        val params = HasselbladParams()
        assertEquals(0, params.tone)
        assertEquals(0, params.saturation)
        assertEquals(0, params.contrast)
        assertEquals(SoftLightMode.NONE, params.softLight)
    }

    @Test
    fun `哈苏参数 - formatParamValue应正确显示符号`() {
        val params = HasselbladParams()
        assertEquals("+5", params.formatParamValue(5))
        assertEquals("-5", params.formatParamValue(-5))
        assertEquals("0", params.formatParamValue(0))
    }

    // ===== 8. 数据模型基本行为 =====

    @Test
    fun `订阅模型 - 默认值应正确`() {
        val sub = Subscription(url = "https://example.com/test.json")
        assertTrue(sub.isEnabled)
        assertEquals(1, sub.build)
        assertEquals(0, sub.presetCount)
        assertEquals(0, sub.lastUpdateTime)
    }

    @Test
    fun `预设列表 - 默认应为空列表`() {
        val list = PresetList()
        assertTrue(list.presets.isEmpty())
        assertEquals(1, list.build)
    }

    @Test
    fun `大师预设 - allImages应包含封面和图库`() {
        val preset = MasterPreset(
            name = "测试预设",
            coverPath = "cover.jpg",
            galleryImages = listOf("g1.jpg", "g2.jpg")
        )
        assertEquals(3, preset.allImages.size)
        assertEquals("cover.jpg", preset.allImages[0])
        assertEquals("g1.jpg", preset.allImages[1])
        assertEquals("g2.jpg", preset.allImages[2])
    }

    @Test
    fun `大师预设 - 空图库时allImages只包含封面`() {
        val preset = MasterPreset(
            name = "测试预设",
            coverPath = "cover.jpg",
            galleryImages = emptyList()
        )
        assertEquals(1, preset.allImages.size)
        assertEquals("cover.jpg", preset.allImages[0])
    }
}
