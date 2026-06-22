package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.VersionInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * 兼容性测试
 * 验证版本兼容性、数据格式兼容性、API兼容性和平台兼容性
 */
class CompatibilityTest {

    // ===== 1. 版本号解析兼容性 =====

    @Test
    fun `版本解析 - 标准三段式版本号`() {
        assertEquals(10100, VersionInfo.parseVersionCode("1.1.0"))
        assertEquals(10900, VersionInfo.parseVersionCode("1.9.0"))
        assertEquals(20000, VersionInfo.parseVersionCode("2.0.0"))
    }

    @Test
    fun `版本解析 - 带v前缀的版本号`() {
        assertEquals(10100, VersionInfo.parseVersionCode("v1.1.0"))
        assertEquals(10900, VersionInfo.parseVersionCode("v1.9.0"))
    }

    @Test
    fun `版本解析 - 预发布版本号应小于正式版`() {
        val stable = VersionInfo.parseVersionCode("2.0.0")
        val beta = VersionInfo.parseVersionCode("2.0.0-beta1")
        val alpha = VersionInfo.parseVersionCode("2.0.0-alpha1")
        val rc = VersionInfo.parseVersionCode("2.0.0-rc1")

        assertTrue("正式版应大于beta", stable > beta)
        assertTrue("beta应大于alpha", beta > alpha)
        assertTrue("rc应大于beta", rc > beta)
        assertTrue("alpha应为负数", alpha < 0)
    }

    @Test
    fun `版本解析 - 两位数版本号`() {
        assertEquals(11000, VersionInfo.parseVersionCode("1.10.0"))
        assertEquals(11050, VersionInfo.parseVersionCode("1.10.50"))
        assertEquals(20100, VersionInfo.parseVersionCode("2.1.0"))
    }

    @Test
    fun `版本解析 - 无效版本号应返回0`() {
        assertEquals(0, VersionInfo.parseVersionCode(""))
        assertEquals(0, VersionInfo.parseVersionCode("invalid"))
        assertEquals(0, VersionInfo.parseVersionCode("not.a.version"))
    }

    @Test
    fun `版本解析 - 版本比较顺序正确`() {
        val versions = listOf(
            "1.0.0-alpha1",
            "1.0.0-beta1",
            "1.0.0-rc1",
            "1.0.0",
            "1.0.1",
            "1.1.0",
            "1.9.0",
            "1.10.0",
            "2.0.0"
        )
        val codes = versions.map { VersionInfo.parseVersionCode(it) }
        for (i in 0 until codes.size - 1) {
            assertTrue("${versions[i]} 应小于 ${versions[i + 1]}", codes[i] < codes[i + 1])
        }
    }

    // ===== 2. 数据格式兼容性 =====

    @Test
    fun `渲染参数 - fromIntMap兼容整数输入`() {
        val map = mapOf(
            "saturation" to 50,
            "contrast" to -30,
            "sharpness" to 80
        )
        val params = RenderParameters.fromIntMap(map)
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(-30f, params.contrast, 0.001f)
        assertEquals(80f, params.sharpness, 0.001f)
    }

    @Test
    fun `渲染参数 - toMap与fromMap互为可逆`() {
        val original = RenderParameters(
            saturation = 25f,
            contrast = -15f,
            brightness = 10f,
            sharpness = 50f,
            grain = 20f
        )
        val map = original.toMap()
        val restored = RenderParameters.fromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun `大师预设 - 默认字段兼容性`() {
        val preset = MasterPreset(name = "测试预设", coverPath = "cover.jpg")
        assertEquals("@OPPO影像", preset.author)
        assertFalse(preset.isFavorite)
        assertFalse(preset.isCustom)
        assertFalse(preset.isNew)
        assertEquals(1, preset.build)
        assertEquals(0, preset.createdAt)
    }

    // ===== 3. 场景映射兼容性 =====

    @Test
    fun `场景映射 - 新旧ID别名应返回一致结果`() {
        val portraitParams = SceneToHasselbladMapping.getParams("portrait")
        val portraitStandardParams = SceneToHasselbladMapping.getParams("portrait-standard")
        assertEquals(portraitParams, portraitStandardParams)

        val landscapeParams = SceneToHasselbladMapping.getParams("landscape")
        val landscapeStandardParams = SceneToHasselbladMapping.getParams("landscape-standard")
        assertEquals(landscapeParams, landscapeStandardParams)

        val nightParams = SceneToHasselbladMapping.getParams("night")
        val nightCityParams = SceneToHasselbladMapping.getParams("night-city")
        assertEquals(nightParams, nightCityParams)
    }

    @Test
    fun `场景映射 - 城市街拍ID别名兼容`() {
        val streetParams = SceneToHasselbladMapping.getParams("street")
        val urbanStreetParams = SceneToHasselbladMapping.getParams("urban-street")
        assertEquals(streetParams, urbanStreetParams)

        val architectureParams = SceneToHasselbladMapping.getParams("architecture")
        val urbanArchitectureParams = SceneToHasselbladMapping.getParams("urban-architecture")
        assertEquals(architectureParams, urbanArchitectureParams)
    }

    // ===== 4. 数据模型兼容性 =====

    @Test
    fun `订阅模型 - 字段缺失时使用默认值`() {
        val sub = Subscription(url = "https://example.com/presets.json")
        assertEquals("", sub.name)
        assertEquals("", sub.author)
        assertEquals(1, sub.build)
        assertTrue(sub.isEnabled)
        assertEquals(0, sub.presetCount)
    }

    @Test
    fun `预设列表 - 空JSON兼容默认值`() {
        val list = PresetList()
        assertTrue(list.presets.isEmpty())
        assertEquals(1, list.build)
        assertEquals(1, list.version)
    }

    // ===== 5. 范围兼容性 =====

    @Test
    fun `渲染参数 - 正负范围参数能正确归一化`() {
        val params = RenderParameters(
            saturation = -100f,
            contrast = 100f,
            brightness = -50f,
            warmth = 50f
        )
        val uniforms = params.toShaderUniforms()
        assertEquals(-1f, uniforms[0], 0.001f)
        assertEquals(1f, uniforms[1], 0.001f)
        assertEquals(-0.5f, uniforms[2], 0.001f)
        assertEquals(0.5f, uniforms[3], 0.001f)
    }

    @Test
    fun `渲染参数 - 非负范围参数能正确归一化`() {
        val params = RenderParameters(
            sharpness = 0f,
            clarity = 50f,
            grain = 100f,
            fade = 25f
        )
        val uniforms = params.toShaderUniforms()
        assertEquals(0f, uniforms[4], 0.001f)
        assertEquals(0.5f, uniforms[5], 0.001f)
        assertEquals(1f, uniforms[11], 0.001f)
        assertEquals(0.25f, uniforms[12], 0.001f)
    }

    // ===== 6. 枚举兼容性 =====

    @Test
    fun `柔光模式 - 所有模式名称不为空`() {
        for (mode in SoftLightMode.entries) {
            assertTrue(mode.displayName.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }

    @Test
    fun `胶片系列 - 所有系列包含胶片`() {
        for (series in FilmSeries.entries) {
            assertTrue("${series.displayName} 应包含胶片", series.films.isNotEmpty())
        }
    }

    @Test
    fun `场景类别 - 所有类别都有显示信息`() {
        for (category in SceneCategory.entries) {
            assertTrue("${category.name} 应有显示名", category.displayName.isNotBlank())
            assertTrue("${category.name} 应有图标", category.icon.isNotBlank())
            assertTrue("${category.name} 应有颜色", category.color != 0L)
        }
    }
}
