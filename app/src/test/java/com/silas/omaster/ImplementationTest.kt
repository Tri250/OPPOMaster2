package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.*
import org.junit.Assert.*
import org.junit.Test

/**
 * 实现测试
 * 验证功能模块是真实实现而非模拟/空实现
 */
class ImplementationTest {

    // ===== 1. 渲染参数真实实现 =====

    @Test
    fun `实现 - RenderParameters包含18个参数`() {
        assertEquals(18, RenderParameters.PARAM_METADATA.size)
        val uniforms = RenderParameters.DEFAULT.toShaderUniforms()
        assertEquals(18, uniforms.size)
    }

    @Test
    fun `实现 - RenderParameters的toMap与fromMap真实互转`() {
        val params = RenderParameters(
            saturation = 10f,
            contrast = -10f,
            brightness = 5f,
            sharpness = 20f
        )
        val map = params.toMap()
        val restored = RenderParameters.fromMap(map)
        assertEquals(params, restored)
        assertTrue(map.isNotEmpty())
    }

    @Test
    fun `实现 - hasAnyAdjustment真实判断非零参数`() {
        assertFalse(RenderParameters.DEFAULT.hasAnyAdjustment())
        assertTrue(RenderParameters(saturation = 1f).hasAnyAdjustment())
        assertTrue(RenderParameters(sharpness = 1f).hasAnyAdjustment())
    }

    // ===== 2. 场景映射真实实现 =====

    @Test
    fun `实现 - 场景映射覆盖主要场景`() {
        val scenes = listOf(
            "portrait", "landscape", "night", "food", "street",
            "macro-insect", "still-flower", "event-wedding"
        )
        for (sceneId in scenes) {
            val params = SceneToHasselbladMapping.getParams(sceneId)
            val films = SceneToHasselbladMapping.getRecommendedFilms(sceneId)
            val tips = SceneToHasselbladMapping.getMasterTips(sceneId)

            assertNotNull("$sceneId 参数不应为null", params)
            assertTrue("$sceneId 胶片推荐不应为空", films.isNotEmpty())
            assertEquals("$sceneId 大师建议应为4条", 4, tips.size)
        }
    }

    @Test
    fun `实现 - 场景映射提供差异化建议`() {
        val sunsetTips = SceneToHasselbladMapping.getMasterTips("landscape-sunset")
        val nightTips = SceneToHasselbladMapping.getMasterTips("night-city")

        assertNotEquals("日落和夜景的构图建议应不同", sunsetTips[0], nightTips[0])
        assertNotEquals("日落和夜景的光线建议应不同", sunsetTips[1], nightTips[1])
    }

    @Test
    fun `实现 - 参数调整建议真实计算差异`() {
        val current = HasselbladParams(tone = 0, saturation = 0)
        val advice = SceneToHasselbladMapping.getParamAdjustmentAdvice(current, "portrait-standard")
        assertTrue("应存在参数差异建议", advice.isNotEmpty())
    }

    // ===== 3. 胶片预设真实实现 =====

    @Test
    fun `实现 - 9款原生胶片全部真实定义`() {
        assertEquals(9, FilmPresets.allFilms.size)
        val ids = FilmPresets.allFilms.map { it.id }.toSet()
        val expected = setOf("cc", "nc", "nh", "portra", "rdp3", "800t", "tx400", "ccd_cool", "ccd_warm")
        assertEquals(expected, ids)
    }

    @Test
    fun `实现 - 场景预设50个以上真实场景`() {
        assertTrue("场景数量应>=50", ScenePresets.allScenes.size >= 50)
    }

    // ===== 4. URL 常量真实实现 =====

    @Test
    fun `实现 - URL常量集中管理所有外部URL`() {
        assertTrue(UrlConstants.PRESET_SOURCE_URLS.isNotEmpty())
        assertTrue(UrlConstants.PRESET_SOURCE_INFO_LIST.isNotEmpty())
        assertTrue(UrlConstants.LUT_BASE_PATH.startsWith("https://"))
        assertTrue(UrlConstants.SAMPLES_BASE_PATH.startsWith("https://"))
    }

    @Test
    fun `实现 - URL构造方法真实工作`() {
        val lutUrl = UrlConstants.getLUTDownloadUrl("film", "portra.cube")
        val sampleUrl = UrlConstants.getSampleImageUrl("portrait.jpg")

        assertTrue(lutUrl.startsWith("https://"))
        assertTrue(lutUrl.endsWith("portra.cube"))
        assertTrue(sampleUrl.startsWith("https://"))
        assertTrue(sampleUrl.endsWith("portrait.jpg"))
    }

    // ===== 5. 版本信息真实实现 =====

    @Test
    fun `实现 - VersionInfo从BuildConfig读取真实版本`() {
        assertTrue(VersionInfo.VERSION_NAME.isNotBlank())
        assertTrue(VersionInfo.VERSION_CODE > 0)
    }

    @Test
    fun `实现 - 版本解析支持预发布版本`() {
        val beta = VersionInfo.parseVersionCode("2.0.0-beta1")
        val stable = VersionInfo.parseVersionCode("2.0.0")
        assertTrue(beta < 0)
        assertTrue(stable > beta)
    }

    // ===== 6. 撤销重做真实实现 =====

    @Test
    fun `实现 - UndoRedoManager真实维护两个栈`() {
        val manager = UndoRedoManager<Int>()
        assertEquals(0, manager.undoCount())
        assertEquals(0, manager.redoCount())

        manager.pushState(1)
        assertEquals(1, manager.undoCount())
        assertEquals(0, manager.redoCount())

        manager.undo(2)
        assertEquals(0, manager.undoCount())
        assertEquals(1, manager.redoCount())
    }

    // ===== 7. 数据模型真实字段 =====

    @Test
    fun `实现 - MasterPreset包含云同步字段`() {
        val preset = MasterPreset(
            name = "测试",
            coverPath = "cover.jpg",
            brand = "oppo",
            version = 1,
            build = 2,
            params = mapOf("ISO" to "100"),
            colorGradingParams = mapOf("saturation" to "+10")
        )
        assertEquals("oppo", preset.brand)
        assertEquals(2, preset.build)
        assertNotNull(preset.params)
        assertNotNull(preset.colorGradingParams)
    }

    @Test
    fun `实现 - Subscription包含完整元数据`() {
        val sub = Subscription(
            url = "https://example.com/presets.json",
            name = "测试",
            author = "作者",
            build = 3,
            presetCount = 10,
            lastUpdateTime = 1234567890
        )
        assertEquals("测试", sub.name)
        assertEquals("作者", sub.author)
        assertEquals(3, sub.build)
        assertEquals(10, sub.presetCount)
        assertEquals(1234567890, sub.lastUpdateTime)
    }

    // ===== 8. 格式化工具真实实现 =====

    @Test
    fun `实现 - FormatUtils真实格式化输出`() {
        assertEquals("+5", 5.formatSigned())
        assertEquals("-5", (-5).formatSigned())
        assertEquals("75%", 0.75f.formatPercent())
        assertEquals("复古 80%", formatFilterWithIntensity("复古", 80))
    }

    // ===== 9. 哈苏参数真实实现 =====

    @Test
    fun `实现 - HasselbladParams包含完整参数`() {
        val params = HasselbladParams(
            tone = 5,
            saturation = 10,
            contrast = -5,
            colorTemp = 3,
            sharpness = 8,
            vignette = 10,
            cyanMagenta = -2,
            softLight = SoftLightMode.SOFT,
            highlights = 5,
            shadows = -5,
            clarity = 10
        )
        assertEquals(5, params.tone)
        assertEquals(10, params.saturation)
        assertEquals(-5, params.contrast)
        assertEquals(SoftLightMode.SOFT, params.softLight)
    }

    // ===== 10. 无空实现检查 =====

    @Test
    fun `实现 - 核心功能方法返回有意义结果`() {
        // RenderParameters
        assertNotNull(RenderParameters.DEFAULT.toShaderUniforms())
        assertTrue(RenderParameters(saturation = 10f).nonZeroCount() > 0)

        // SceneToHasselbladMapping
        assertTrue(SceneToHasselbladMapping.getRecommendedFilms("portrait").isNotEmpty())
        assertTrue(SceneToHasselbladMapping.getMasterTips("landscape").isNotEmpty())

        // FilmPresets
        assertNotNull(FilmPresets.getFilmById("portra"))
        assertTrue(FilmPresets.getFilmsBySeries(FilmSeries.EMOTION).isNotEmpty())
    }
}
