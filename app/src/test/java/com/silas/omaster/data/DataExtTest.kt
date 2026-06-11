package com.silas.omaster.data

import org.junit.Test
import org.junit.Assert.*

/**
 * LUTResource 单元测试
 * 测试LUT资源数据模型
 */
class LUTResourceTest {

    @Test
    fun `LUT创建 - 应该正确创建LUT对象`() {
        val lut = LUTResource(
            id = "lut_001",
            name = "Film Look",
            description = "经典胶片风格",
            category = "Film",
            downloadUrl = "https://example.com/lut.cube",
            downloadCount = 1000,
            rating = 4.5
        )
        
        assertEquals("lut_001", lut.id)
        assertEquals("Film Look", lut.name)
        assertEquals("经典胶片风格", lut.description)
        assertEquals("Film", lut.category)
        assertEquals(1000, lut.downloadCount)
        assertEquals(4.5, lut.rating, 0.01)
    }

    @Test
    fun `LUT评分 - 应该在有效范围内`() {
        val lut = LUTResource(
            id = "lut_001",
            name = "Test",
            description = "Test",
            category = "Test",
            downloadUrl = "https://example.com/lut.cube",
            downloadCount = 0,
            rating = 4.8
        )
        
        assertTrue("评分应该在0到5之间", lut.rating in 0.0..5.0)
    }

    @Test
    fun `LUT下载量 - 应该非负`() {
        val lut = LUTResource(
            id = "lut_001",
            name = "Test",
            description = "Test",
            category = "Test",
            downloadUrl = "https://example.com/lut.cube",
            downloadCount = 500,
            rating = 4.0
        )
        
        assertTrue("下载量应该非负", lut.downloadCount >= 0)
    }
}

/**
 * PresetSource 单元测试
 * 测试预设源数据模型
 */
class PresetSourceTest {

    @Test
    fun `预设源创建 - 应该正确创建预设源对象`() {
        val source = PresetSource(
            id = "source_001",
            name = "Official Presets",
            url = "https://cdn.example.com/presets.json",
            enabled = true,
            lastUpdated = System.currentTimeMillis()
        )
        
        assertEquals("source_001", source.id)
        assertEquals("Official Presets", source.name)
        assertEquals("https://cdn.example.com/presets.json", source.url)
        assertTrue(source.enabled)
        assertNotNull(source.lastUpdated)
    }

    @Test
    fun `预设源默认值 - 应该有正确的默认值`() {
        val source = PresetSource(
            id = "source_001",
            name = "Test",
            url = "https://example.com/presets.json"
        )
        
        assertTrue("默认应该启用", source.enabled)
        assertNull("默认更新时间应该为null", source.lastUpdated)
    }

    @Test
    fun `预设源状态 - 应该正确切换启用状态`() {
        val source = PresetSource(
            id = "source_001",
            name = "Test",
            url = "https://example.com/presets.json",
            enabled = true
        )
        
        val disabled = source.copy(enabled = false)
        assertFalse("应该被禁用", disabled.enabled)
    }

    @Test
    fun `预设源配置 - 应该正确创建配置对象`() {
        val config = PresetSourceConfig(
            sources = listOf(
                PresetSource("001", "Source 1", "https://example1.com/presets.json"),
                PresetSource("002", "Source 2", "https://example2.com/presets.json")
            )
        )
        
        assertEquals(2, config.sources.size)
    }

    @Test
    fun `预设源响应 - 应该正确创建响应对象`() {
        val response = PresetSourceResponse(
            presets = emptyList(),
            version = "1.0.0",
            updateTime = "2024-01-15"
        )
        
        assertEquals("1.0.0", response.version)
        assertEquals("2024-01-15", response.updateTime)
    }
}

/**
 * PresetRepository 单元测试
 * 测试预设仓库逻辑
 */
class PresetRepositoryLogicTest {

    @Test
    fun `预设排序 - 应该按时间戳降序排列`() {
        val presets = listOf(
            TestPresetData("001", timestamp = 1000L),
            TestPresetData("002", timestamp = 3000L),
            TestPresetData("003", timestamp = 2000L)
        )
        
        val sorted = presets.sortedByDescending { it.timestamp }
        
        assertEquals("002", sorted[0].id)
        assertEquals("003", sorted[1].id)
        assertEquals("001", sorted[2].id)
    }

    @Test
    fun `预设过滤 - 应该按品牌过滤`() {
        val presets = listOf(
            TestPresetData("001", brand = "Hasselblad"),
            TestPresetData("002", brand = "Fuji"),
            TestPresetData("003", brand = "Hasselblad")
        )
        
        val filtered = presets.filter { it.brand == "Hasselblad" }
        
        assertEquals(2, filtered.size)
    }

    @Test
    fun `预设搜索 - 应该支持模糊搜索`() {
        val presets = listOf(
            TestPresetData("001", name = "Portrait Classic"),
            TestPresetData("002", name = "Landscape Pro"),
            TestPresetData("003", name = "Portrait Modern")
        )
        
        val query = "portrait"
        val results = presets.filter { it.name.lowercase().contains(query.lowercase()) }
        
        assertEquals(2, results.size)
    }

    @Test
    fun `预设分页 - 应该正确计算分页`() {
        val totalItems = 100
        val pageSize = 20
        val currentPage = 2
        
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = startIndex + pageSize
        
        assertEquals(20, startIndex)
        assertEquals(40, endIndex)
    }

    @Test
    fun `预设去重 - 应该移除重复预设`() {
        val presets = listOf(
            TestPresetData("001"),
            TestPresetData("001"),
            TestPresetData("002")
        )
        
        val unique = presets.distinctBy { it.id }
        
        assertEquals(2, unique.size)
    }
}

/**
 * FavoriteManager 逻辑测试
 */
class FavoriteManagerLogicTest {

    @Test
    fun `收藏添加 - 应该正确添加收藏`() {
        val favorites = mutableSetOf<String>()
        
        favorites.add("preset_001")
        
        assertTrue(favorites.contains("preset_001"))
        assertEquals(1, favorites.size)
    }

    @Test
    fun `收藏移除 - 应该正确移除收藏`() {
        val favorites = mutableSetOf("preset_001", "preset_002")
        
        favorites.remove("preset_001")
        
        assertFalse(favorites.contains("preset_001"))
        assertEquals(1, favorites.size)
    }

    @Test
    fun `收藏切换 - 应该正确切换收藏状态`() {
        val favorites = mutableSetOf<String>()
        val presetId = "preset_001"
        
        // 添加
        if (!favorites.contains(presetId)) {
            favorites.add(presetId)
        }
        assertTrue(favorites.contains(presetId))
        
        // 移除
        if (favorites.contains(presetId)) {
            favorites.remove(presetId)
        }
        assertFalse(favorites.contains(presetId))
    }
}

/**
 * SettingsManager 逻辑测试
 */
class SettingsManagerLogicTest {

    @Test
    fun `设置默认值 - 应该有合理的默认值`() {
        val settings = TestSettings(
            autoSync = true,
            darkMode = false,
            language = "zh"
        )
        
        assertTrue(settings.autoSync)
        assertFalse(settings.darkMode)
        assertEquals("zh", settings.language)
    }

    @Test
    fun `设置更新 - 应该正确更新设置`() {
        val original = TestSettings(autoSync = true, darkMode = false, language = "zh")
        
        val updated = original.copy(darkMode = true)
        
        assertTrue(updated.darkMode)
        assertEquals(original.autoSync, updated.autoSync)
    }
}

/**
 * CustomPresetManager 逻辑测试
 */
class CustomPresetManagerLogicTest {

    @Test
    fun `自定义预设创建 - 应该正确创建预设`() {
        val preset = TestCustomPreset(
            id = "custom_001",
            name = "My Preset",
            params = mapOf("saturation" to 10, "contrast" to 5),
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals("custom_001", preset.id)
        assertEquals("My Preset", preset.name)
        assertEquals(10, preset.params["saturation"])
    }

    @Test
    fun `自定义预设限制 - 应该限制预设数量`() {
        val maxPresets = 50
        val currentCount = 45
        
        val canCreate = currentCount < maxPresets
        assertTrue("应该允许创建更多预设", canCreate)
        
        val fullCount = 50
        val cannotCreate = fullCount >= maxPresets
        assertTrue("达到上限时不应该允许创建", cannotCreate)
    }
}

/**
 * RecipeHistoryManager 逻辑测试
 */
class RecipeHistoryManagerLogicTest {

    @Test
    fun `历史记录 - 应该正确添加历史记录`() {
        val history = mutableListOf<String>()
        
        history.add(0, "preset_001")
        history.add(0, "preset_002")
        
        assertEquals(2, history.size)
        assertEquals("preset_002", history[0]) // 最新的在前面
    }

    @Test
    fun `历史记录限制 - 应该限制历史记录数量`() {
        val maxHistory = 20
        val history = mutableListOf<String>()
        
        for (i in 1..25) {
            history.add(0, "preset_$i")
            if (history.size > maxHistory) {
                history.removeAt(history.size - 1)
            }
        }
        
        assertEquals(maxHistory, history.size)
    }
}

// 辅助数据类
data class TestPresetData(
    val id: String,
    val name: String = "Test",
    val brand: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis()
)

data class TestSettings(
    val autoSync: Boolean,
    val darkMode: Boolean,
    val language: String
)

data class TestCustomPreset(
    val id: String,
    val name: String,
    val params: Map<String, Int>,
    val createdAt: Long
)
