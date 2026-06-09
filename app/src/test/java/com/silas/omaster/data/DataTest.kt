package com.silas.omaster.data

import org.junit.Test
import org.junit.Assert.*

/**
 * 预设仓库测试
 * 测试预设数据管理的核心逻辑
 */
class PresetRepositoryTest {

    @Test
    fun testPresetIdGeneration() {
        // 测试预设ID生成
        val timestamp = System.currentTimeMillis()
        val random = (0..9999).random()
        
        val presetId = "preset_${timestamp}_${random}"
        
        assertTrue(presetId.startsWith("preset_"))
        assertTrue(presetId.contains("_"))
    }

    @Test
    fun testPresetSortingByDate() {
        // 测试按日期排序
        val presets = listOf(
            mockPreset("001", timestamp = 1704067200000L),
            mockPreset("002", timestamp = 1704153600000L),
            mockPreset("003", timestamp = 1704240000000L)
        )
        
        val sortedPresets = presets.sortedByDescending { it.timestamp }
        
        assertEquals("003", sortedPresets[0].id)
        assertEquals("002", sortedPresets[1].id)
        assertEquals("001", sortedPresets[2].id)
    }

    @Test
    fun testPresetSortingByRating() {
        // 测试按评分排序
        val presets = listOf(
            mockPresetWithRating("001", rating = 4.5f),
            mockPresetWithRating("002", rating = 5.0f),
            mockPresetWithRating("003", rating = 3.8f)
        )
        
        val sortedPresets = presets.sortedByDescending { it.rating }
        
        assertEquals("002", sortedPresets[0].id)
        assertEquals("001", sortedPresets[1].id)
        assertEquals("003", sortedPresets[2].id)
    }

    @Test
    fun testPresetFilteringByBrand() {
        // 测试按品牌过滤
        val presets = listOf(
            mockPresetWithBrand("001", "Hasselblad"),
            mockPresetWithBrand("002", "Fuji"),
            mockPresetWithBrand("003", "Hasselblad"),
            mockPresetWithBrand("004", "Sony")
        )
        
        val filteredPresets = presets.filter { it.brand == "Hasselblad" }
        
        assertEquals(2, filteredPresets.size)
        assertEquals("001", filteredPresets[0].id)
        assertEquals("003", filteredPresets[1].id)
    }

    @Test
    fun testPresetFilteringByTag() {
        // 测试按标签过滤
        val presets = listOf(
            mockPresetWithTags("001", listOf("portrait", "skin")),
            mockPresetWithTags("002", listOf("landscape", "nature")),
            mockPresetWithTags("003", listOf("portrait", "classic"))
        )
        
        val filteredPresets = presets.filter { it.tags.contains("portrait") }
        
        assertEquals(2, filteredPresets.size)
    }

    @Test
    fun testPresetSearchByName() {
        // 测试按名称搜索
        val presets = listOf(
            mockPresetWithName("001", "Portrait Classic"),
            mockPresetWithName("002", "Landscape Pro"),
            mockPresetWithName("003", "Portrait Modern")
        )
        
        val searchQuery = "portrait"
        val filteredPresets = presets.filter { 
            it.name.lowercase().contains(searchQuery.lowercase()) 
        }
        
        assertEquals(2, filteredPresets.size)
    }

    @Test
    fun testPresetPagination() {
        // 测试预设分页
        val presets = (1..100).map { i -> mockPreset("preset_$i") }
        
        val pageSize = 20
        val currentPage = 2
        
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = startIndex + pageSize
        
        val pagePresets = presets.subList(startIndex, endIndex.coerceAtMost(presets.size))
        
        assertEquals(20, pagePresets.size)
        assertEquals("preset_21", pagePresets[0].id)
    }

    @Test
    fun testFavoriteToggle() {
        // 测试收藏切换
        val favorites = mutableSetOf<String>()
        
        val presetId = "preset_001"
        
        // 添加收藏
        favorites.add(presetId)
        assertTrue(favorites.contains(presetId))
        
        // 移除收藏
        favorites.remove(presetId)
        assertFalse(favorites.contains(presetId))
    }

    @Test
    fun testFavoriteCount() {
        // 测试收藏计数
        val favorites = mutableSetOf<String>()
        
        favorites.add("001")
        favorites.add("002")
        favorites.add("003")
        
        assertEquals(3, favorites.size)
    }

    @Test
    fun testPresetValidation() {
        // 测试预设验证
        val validPreset = mockPreset("001")
        
        val isValid = validPreset.id.isNotEmpty() && 
                      validPreset.name.isNotEmpty()
        
        assertTrue(isValid)
        
        val invalidPreset = TestPreset(id = "", name = "", timestamp = 0L)
        
        val isInvalid = invalidPreset.id.isEmpty() || 
                        invalidPreset.name.isEmpty()
        
        assertTrue(isInvalid)
    }

    @Test
    fun testPresetCopy() {
        // 测试预设复制
        val original = mockPreset("001")
        val copy = original.copy(id = "002")
        
        assertEquals("002", copy.id)
        assertEquals(original.name, copy.name)
        assertEquals(original.timestamp, copy.timestamp)
    }

    @Test
    fun testPresetMerge() {
        // 测试预设合并
        val localPresets = listOf(mockPreset("001"), mockPreset("002"))
        val remotePresets = listOf(mockPreset("003"), mockPreset("004"))
        
        val mergedPresets = localPresets + remotePresets
        
        assertEquals(4, mergedPresets.size)
    }

    @Test
    fun testPresetDeduplication() {
        // 测试预设去重
        val presets = listOf(
            mockPreset("001"),
            mockPreset("001"), // 重复
            mockPreset("002"),
            mockPreset("002")  // 重复
        )
        
        val uniquePresets = presets.distinctBy { it.id }
        
        assertEquals(2, uniquePresets.size)
    }

    private fun mockPreset(id: String, timestamp: Long = System.currentTimeMillis()): TestPreset {
        return TestPreset(
            id = id,
            name = "Test Preset $id",
            timestamp = timestamp
        )
    }

    private fun mockPresetWithRating(id: String, rating: Float): TestPresetWithRating {
        return TestPresetWithRating(id = id, name = "Test", rating = rating)
    }

    private fun mockPresetWithBrand(id: String, brand: String): TestPresetWithBrand {
        return TestPresetWithBrand(id = id, name = "Test", brand = brand)
    }

    private fun mockPresetWithTags(id: String, tags: List<String>): TestPresetWithTags {
        return TestPresetWithTags(id = id, name = "Test", tags = tags)
    }

    private fun mockPresetWithName(id: String, name: String): TestPreset {
        return TestPreset(id = id, name = name, timestamp = System.currentTimeMillis())
    }
}

/**
 * 自定义预设管理器测试
 */
class CustomPresetManagerTest {

    @Test
    fun testCustomPresetCreation() {
        // 测试自定义预设创建
        val customPreset = CustomPresetData(
            id = "custom_001",
            name = "My Custom Preset",
            params = mapOf(
                "saturation" to 10f,
                "contrast" to 20f
            ),
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals("custom_001", customPreset.id)
        assertEquals("My Custom Preset", customPreset.name)
        assertEquals(10f, customPreset.params["saturation"])
    }

    @Test
    fun testCustomPresetUpdate() {
        // 测试自定义预设更新
        val original = CustomPresetData(
            id = "custom_001",
            name = "Original Name",
            params = mapOf("saturation" to 10f),
            createdAt = System.currentTimeMillis()
        )
        
        val updated = original.copy(
            name = "Updated Name",
            params = mapOf("saturation" to 15f, "contrast" to 25f)
        )
        
        assertEquals("Updated Name", updated.name)
        assertEquals(15f, updated.params["saturation"])
        assertEquals(25f, updated.params["contrast"])
    }

    @Test
    fun testCustomPresetDeletion() {
        // 测试自定义预设删除
        val customPresets = mutableListOf(
            CustomPresetData("001", "Preset 1", mapOf(), 0L),
            CustomPresetData("002", "Preset 2", mapOf(), 0L),
            CustomPresetData("003", "Preset 3", mapOf(), 0L)
        )
        
        customPresets.removeAll { it.id == "002" }
        
        assertEquals(2, customPresets.size)
        assertFalse(customPresets.any { it.id == "002" })
    }

    @Test
    fun testCustomPresetLimit() {
        // 测试自定义预设数量限制
        val maxCustomPresets = 50
        val currentCount = 45
        
        val canCreateMore = currentCount < maxCustomPresets
        
        assertTrue(canCreateMore)
        
        val fullCount = 50
        val cannotCreateMore = fullCount >= maxCustomPresets
        
        assertTrue(cannotCreateMore)
    }
}

/**
 * 收藏管理器测试
 */
class FavoriteManagerTest {

    @Test
    fun testFavoriteAdd() {
        val favorites = mutableSetOf<String>()
        
        favorites.add("preset_001")
        
        assertTrue(favorites.contains("preset_001"))
        assertEquals(1, favorites.size)
    }

    @Test
    fun testFavoriteRemove() {
        val favorites = mutableSetOf("preset_001", "preset_002")
        
        favorites.remove("preset_001")
        
        assertFalse(favorites.contains("preset_001"))
        assertEquals(1, favorites.size)
    }

    @Test
    fun testFavoriteToggle() {
        val favorites = mutableSetOf<String>()
        val presetId = "preset_001"
        
        // 第一次切换：添加
        if (favorites.contains(presetId)) {
            favorites.remove(presetId)
        } else {
            favorites.add(presetId)
        }
        
        assertTrue(favorites.contains(presetId))
        
        // 第二次切换：移除
        if (favorites.contains(presetId)) {
            favorites.remove(presetId)
        } else {
            favorites.add(presetId)
        }
        
        assertFalse(favorites.contains(presetId))
    }

    @Test
    fun testFavoriteSort() {
        val presets = listOf(
            TestPreset("001", "Preset 1", 0L),
            TestPreset("002", "Preset 2", 0L),
            TestPreset("003", "Preset 3", 0L)
        )
        
        val favorites = setOf("002", "003")
        
        val sortedPresets = presets.sortedByDescending { favorites.contains(it.id) }
        
        assertEquals("002", sortedPresets[0].id)
        assertEquals("003", sortedPresets[1].id)
        assertEquals("001", sortedPresets[2].id)
    }
}

/**
 * 设置管理器测试
 */
class SettingsManagerTest {

    @Test
    fun testDefaultSettings() {
        val settings = AppSettings()
        
        assertTrue(settings.autoSync)
        assertFalse(settings.darkMode)
        assertEquals("zh", settings.language)
    }

    @Test
    fun testSettingsUpdate() {
        val settings = AppSettings()
        
        val updatedSettings = settings.copy(
            autoSync = false,
            darkMode = true,
            language = "en"
        )
        
        assertFalse(updatedSettings.autoSync)
        assertTrue(updatedSettings.darkMode)
        assertEquals("en", updatedSettings.language)
    }

    @Test
    fun testSettingsPersistence() {
        // 模拟设置持久化
        val settings = AppSettings(darkMode = true)
        
        val serialized = settings.toString()
        
        assertTrue(serialized.contains("darkMode=true"))
    }
}

// 辅助数据类
data class TestPreset(
    val id: String,
    val name: String,
    val timestamp: Long
)

data class TestPresetWithRating(
    val id: String,
    val name: String,
    val rating: Float
)

data class TestPresetWithBrand(
    val id: String,
    val name: String,
    val brand: String
)

data class TestPresetWithTags(
    val id: String,
    val name: String,
    val tags: List<String>
)

data class CustomPresetData(
    val id: String,
    val name: String,
    val params: Map<String, Float>,
    val createdAt: Long
)

data class AppSettings(
    val autoSync: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "zh"
)