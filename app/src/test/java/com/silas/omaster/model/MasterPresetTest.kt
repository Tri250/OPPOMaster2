package com.silas.omaster.model

import org.junit.Assert.*
import org.junit.Test

/**
 * MasterPreset 数据模型单元测试
 * 验证预设数据模型的正确性和边界条件
 */
class MasterPresetTest {

    @Test
    fun `MasterPreset 默认值应正确`() {
        val preset = MasterPreset(
            id = "test-001",
            name = "测试预设",
            author = "TestAuthor"
        )

        assertEquals("test-001", preset.id)
        assertEquals("测试预设", preset.name)
        assertEquals("TestAuthor", preset.author)
        assertFalse(preset.isFavorite)
        assertEquals(0, preset.viewCount)
        assertEquals(0, preset.rating)
        assertTrue(preset.tags.isEmpty())
    }

    @Test
    fun `PresetSection 应正确存储参数`() {
        val section = PresetSection(
            name = "基础参数",
            items = listOf(
                PresetItem(label = "ISO", value = "100"),
                PresetItem(label = "快门", value = "1/1000s"),
                PresetItem(label = "曝光补偿", value = "+0.3")
            )
        )

        assertEquals("基础参数", section.name)
        assertEquals(3, section.items.size)
        assertEquals("ISO", section.items[0].label)
        assertEquals("100", section.items[0].value)
    }

    @Test
    fun `HasselbladParams 应正确计算曝光值`() {
        val params = HasselbladParams(
            iso = 100,
            shutterSpeed = "1/1000",
            aperture = 2.8f,
            exposureCompensation = 0.0f
        )

        assertEquals(100, params.iso)
        assertEquals("1/1000", params.shutterSpeed)
        assertEquals(2.8f, params.aperture, 0.01f)
    }

    @Test
    fun `SceneCategory 枚举应完整`() {
        val categories = SceneCategory.values()
        assertTrue(categories.size >= 10)
        assertTrue(SceneCategory.PORTRAIT in categories)
        assertTrue(SceneCategory.LANDSCAPE in categories)
        assertTrue(SceneCategory.NIGHT in categories)
    }

    @Test
    fun `FilmSeries 枚举应完整`() {
        val series = FilmSeries.values()
        assertTrue(series.isNotEmpty())
    }

    @Test
    fun `PresetList 应正确统计预设数量`() {
        val presets = listOf(
            MasterPreset(id = "1", name = "预设1", author = "A"),
            MasterPreset(id = "2", name = "预设2", author = "B"),
            MasterPreset(id = "3", name = "预设3", author = "C", isFavorite = true)
        )
        val presetList = PresetList(presets = presets)

        assertEquals(3, presetList.presets.size)
        assertEquals(1, presetList.presets.count { it.isFavorite })
    }

    @Test
    fun `MasterPreset 相等性应基于id`() {
        val preset1 = MasterPreset(id = "same-id", name = "名称1", author = "作者1")
        val preset2 = MasterPreset(id = "same-id", name = "名称2", author = "作者2")

        assertEquals(preset1.id, preset2.id)
    }
}
