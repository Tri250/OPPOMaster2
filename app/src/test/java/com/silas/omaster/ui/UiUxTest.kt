package com.silas.omaster.ui

import com.silas.omaster.model.*
import com.silas.omaster.util.formatFilterWithIntensity
import com.silas.omaster.util.formatPercent
import com.silas.omaster.util.formatSigned
import org.junit.Assert.*
import org.junit.Test

/**
 * UI/UX 测试
 * 验证界面显示、文案、颜色、图标、交互反馈等
 */
class UiUxTest {

    // ===== 1. 场景类别 UI 信息 =====

    @Test
    fun `UI - 所有场景类别都有中文显示名和图标`() {
        for (category in SceneCategory.entries) {
            assertTrue("${category.name} 应有中文显示名", category.displayName.isNotBlank())
            assertTrue("${category.name} 应有emoji图标", category.icon.isNotBlank())
            assertTrue("${category.name} 颜色不应为0", category.color != 0L)
        }
    }

    @Test
    fun `UI - 场景类别颜色应为有效ARGB`() {
        for (category in SceneCategory.entries) {
            val color = category.color
            // ARGB 颜色应为 8 字节（0xAARRGGBB），高字节不透明
            assertTrue("${category.name} 颜色高字节应为FF", color shr 24 == 0xFFL)
        }
    }

    // ===== 2. 胶片预设显示信息 =====

    @Test
    fun `UI - 所有胶片都有名称和描述`() {
        for (film in FilmPresets.allFilms) {
            assertTrue("${film.id} 应有名称", film.name.isNotBlank())
            assertTrue("${film.id} 应有描述", film.description.isNotBlank())
        }
    }

    @Test
    fun `UI - 胶片匹配度应在0到1之间`() {
        for (film in FilmPresets.allFilms) {
            assertTrue("${film.id} 匹配度应在[0,1]", film.matchScore in 0f..1f)
        }
    }

    @Test
    fun `UI - 胶片扩展字段不为空`() {
        for (film in FilmPresets.allFilms) {
            assertTrue("${film.id} 色彩风格不应为空", film.colorStyle.isNotBlank())
            assertTrue("${film.id} 颗粒级别不应为空", film.grainLevel.isNotBlank())
            assertTrue("${film.id} 对比度级别不应为空", film.contrastLevel.isNotBlank())
            assertTrue("${film.id} 适用场景不应为空", film.bestFor.isNotBlank())
        }
    }

    // ===== 3. 场景预设 UI 信息 =====

    @Test
    fun `UI - 所有场景都有中文名称和描述`() {
        for (scene in ScenePresets.allScenes) {
            assertTrue("${scene.id} 名称应为中文或有效文本", scene.name.isNotBlank())
            assertTrue("${scene.id} 描述应为中文或有效文本", scene.description.isNotBlank())
        }
    }

    @Test
    fun `UI - 所有场景都有至少3条大师建议`() {
        for (scene in ScenePresets.allScenes) {
            assertTrue("${scene.id} 建议数量应>=3", scene.masterTips.size >= 3)
        }
    }

    // ===== 4. 格式化显示 =====

    @Test
    fun `UI - 参数符号显示符合用户习惯`() {
        assertEquals("+5", 5.formatSigned())
        assertEquals("-5", (-5).formatSigned())
        assertEquals("0", 0.formatSigned())
    }

    @Test
    fun `UI - 百分比显示为整数`() {
        assertEquals("0%", 0f.formatPercent())
        assertEquals("50%", 0.5f.formatPercent())
        assertEquals("100%", 1f.formatPercent())
        assertEquals("33%", 0.333f.formatPercent())
    }

    @Test
    fun `UI - 滤镜强度显示正确`() {
        assertEquals("标准", formatFilterWithIntensity("标准", 100))
        assertEquals("复古 80%", formatFilterWithIntensity("复古", 80))
        assertEquals("黑白 0%", formatFilterWithIntensity("黑白", 0))
    }

    // ===== 5. 哈苏参数显示 =====

    @Test
    fun `UI - 哈苏参数格式化带符号`() {
        val params = HasselbladParams()
        assertEquals("+15", params.formatParamValue(15))
        assertEquals("-15", params.formatParamValue(-15))
        assertEquals("0", params.formatParamValue(0))
    }

    @Test
    fun `UI - 柔光模式有中文显示名`() {
        for (mode in SoftLightMode.entries) {
            assertTrue("${mode.name} 应有中文显示名", mode.displayName.isNotBlank())
            assertTrue("${mode.name} 应有描述", mode.description.isNotBlank())
        }
    }

    // ===== 6. 胶片系列显示名 =====

    @Test
    fun `UI - 胶片系列有中文显示名`() {
        for (series in FilmSeries.entries) {
            assertTrue("${series.name} 应有中文显示名", series.displayName.isNotBlank())
        }
    }

    // ===== 7. 预设显示信息 =====

    @Test
    fun `UI - 大师预设默认作者不为空`() {
        val preset = MasterPreset(name = "测试", coverPath = "cover.jpg")
        assertTrue(preset.author.isNotBlank())
    }

    @Test
    fun `UI - 大师预设所有图片列表非空`() {
        val preset = MasterPreset(
            name = "测试",
            coverPath = "cover.jpg",
            galleryImages = listOf("1.jpg", "2.jpg")
        )
        assertTrue(preset.allImages.isNotEmpty())
        assertEquals("cover.jpg", preset.allImages.first())
    }

    // ===== 8. 场景画像显示 =====

    @Test
    fun `UI - 场景画像信息完整`() {
        val scene = ScenePresets.getSceneById("portrait-standard")
        assertNotNull(scene)
        scene!!.let {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.description.isNotBlank())
            assertTrue(it.recommendedFilm.isNotEmpty())
            assertTrue(it.masterTips.isNotEmpty())
            assertTrue(it.category.displayName.isNotBlank())
        }
    }

    // ===== 9. 主题色一致性 =====

    @Test
    fun `UI - 人像类别使用哈苏橙色`() {
        assertEquals(0xFFFF6B35, SceneCategory.PORTRAIT.color)
    }

    @Test
    fun `UI - 风景类别使用自然绿色`() {
        assertEquals(0xFF4CAF50, SceneCategory.LANDSCAPE.color)
    }

    @Test
    fun `UI - 夜景类别使用夜空蓝色`() {
        assertEquals(0xFF2196F3, SceneCategory.NIGHT.color)
    }
}
