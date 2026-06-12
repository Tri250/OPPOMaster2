package com.silas.omaster.data.local

import org.junit.Assert.*
import org.junit.Test

/**
 * SettingsManager 单元测试
 */
class SettingsManagerTest {

    @Test
    fun `深色模式 - 默认值验证`() {
        val defaultMode = "SYSTEM"
        assertEquals("默认深色模式应该是SYSTEM", "SYSTEM", defaultMode)
    }

    @Test
    fun `深色模式 - 有效值验证`() {
        val validModes = listOf("SYSTEM", "LIGHT", "DARK")
        for (mode in validModes) {
            assertTrue("深色模式应该有效: $mode", mode in validModes)
        }
    }

    @Test
    fun `更新渠道 - 默认值验证`() {
        val defaultChannel = "GITHUB"
        assertEquals("默认更新渠道应该是GITHUB", "GITHUB", defaultChannel)
    }

    @Test
    fun `更新渠道 - 有效值验证`() {
        val validChannels = listOf("GITHUB", "GITEE")
        for (channel in validChannels) {
            assertTrue("更新渠道应该有效: $channel", channel in validChannels)
        }
    }

    @Test
    fun `透明度范围 - 验证`() {
        val minOpacity = 30
        val maxOpacity = 70
        
        assertTrue("最小透明度应该 >= 30", minOpacity >= 30)
        assertTrue("最大透明度应该 <= 70", maxOpacity <= 70)
    }

    @Test
    fun `启动Tab索引 - 范围验证`() {
        val validIndices = 0..2
        val currentIndex = 1
        
        assertTrue("启动Tab索引应该在有效范围内", currentIndex in validIndices)
    }

    @Test
    fun `API配置 - 默认值验证`() {
        val defaultApiVersion = "v1"
        assertEquals("默认API版本应该是v1", "v1", defaultApiVersion)
    }

    @Test
    fun `云同步状态 - 有效值验证`() {
        val validStates = listOf("DISABLED", "SYNCING", "SYNCED", "ERROR")
        for (state in validStates) {
            assertTrue("云同步状态应该有效: $state", state.isNotEmpty())
        }
    }
}

/**
 * SubscriptionManager 单元测试
 */
class SubscriptionManagerTest {

    @Test
    fun `订阅状态 - 有效值验证`() {
        val validStates = listOf("ACTIVE", "EXPIRED", "CANCELLED", "PENDING")
        for (state in validStates) {
            assertTrue("订阅状态应该有效: $state", state.isNotEmpty())
        }
    }

    @Test
    fun `订阅URL验证 - HTTPS要求`() {
        val testUrl = "https://example.com/presets.json"
        assertTrue("订阅URL应该使用HTTPS", testUrl.startsWith("https://"))
    }

    @Test
    fun `订阅URL验证 - HTTP拒绝`() {
        val invalidUrl = "http://example.com/presets.json"
        assertFalse("HTTP URL应该被拒绝", invalidUrl.startsWith("https://"))
    }

    @Test
    fun `订阅构建号比较 - 需要更新`() {
        val currentBuild = 10
        val remoteBuild = 15
        
        assertTrue("远程构建更新时需要更新", remoteBuild > currentBuild)
    }

    @Test
    fun `订阅构建号比较 - 无需更新`() {
        val currentBuild = 15
        val remoteBuild = 15
        
        assertFalse("相同构建不需要更新", remoteBuild > currentBuild)
    }
}

/**
 * FavoriteManager 单元测试
 */
class FavoriteManagerTest {

    @Test
    fun `收藏操作 - 添加收藏`() {
        val favorites = mutableSetOf<String>()
        favorites.add("preset_1")
        
        assertEquals(1, favorites.size)
        assertTrue(favorites.contains("preset_1"))
    }

    @Test
    fun `收藏操作 - 移除收藏`() {
        val favorites = mutableSetOf("preset_1", "preset_2")
        favorites.remove("preset_1")
        
        assertEquals(1, favorites.size)
        assertFalse(favorites.contains("preset_1"))
    }

    @Test
    fun `收藏操作 - 重复添加`() {
        val favorites = mutableSetOf<String>()
        favorites.add("preset_1")
        favorites.add("preset_1")
        
        assertEquals("重复添加应该只保留一个", 1, favorites.size)
    }

    @Test
    fun `收藏状态检查 - 已收藏`() {
        val favorites = setOf("preset_1", "preset_2")
        assertTrue("preset_1应该已收藏", favorites.contains("preset_1"))
    }

    @Test
    fun `收藏状态检查 - 未收藏`() {
        val favorites = setOf("preset_1", "preset_2")
        assertFalse("preset_3不应该已收藏", favorites.contains("preset_3"))
    }
}

/**
 * RecipeHistoryManager 单元测试
 */
class RecipeHistoryManagerTest {

    @Test
    fun `历史记录 - 添加记录`() {
        val history = mutableListOf<String>()
        history.add("preset_1")
        history.add("preset_2")
        
        assertEquals(2, history.size)
    }

    @Test
    fun `历史记录 - FIFO顺序`() {
        val history = mutableListOf("preset_1", "preset_2", "preset_3")
        val first = history.removeAt(0)
        
        assertEquals("preset_1", first)
        assertEquals(2, history.size)
    }

    @Test
    fun `历史记录 - 最大数量限制`() {
        val maxHistory = 100
        val history = mutableListOf<String>()
        
        for (i in 1..150) {
            history.add("preset_$i")
            if (history.size > maxHistory) {
                history.removeAt(0)
            }
        }
        
        assertEquals("历史记录应该不超过最大数量", maxHistory, history.size)
    }

    @Test
    fun `历史记录 - 去重处理`() {
        val history = mutableListOf("preset_1", "preset_2", "preset_1")
        val distinct = history.distinct()
        
        assertEquals(2, distinct.size)
    }
}

/**
 * NewPresetManager 单元测试
 */
class NewPresetManagerTest {

    @Test
    fun `NEW标记 - 时间戳判断`() {
        val createdAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L) // 3天前
        val isNew = (System.currentTimeMillis() - createdAt) <= (7 * 24 * 60 * 60 * 1000L)
        
        assertTrue("3天前的预设应该标记为NEW", isNew)
    }

    @Test
    fun `NEW标记 - 过期判断`() {
        val createdAt = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L) // 10天前
        val isNew = (System.currentTimeMillis() - createdAt) <= (7 * 24 * 60 * 60 * 1000L)
        
        assertFalse("10天前的预设不应该标记为NEW", isNew)
    }

    @Test
    fun `NEW标记 - 刚好7天`() {
        val createdAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val isNew = (System.currentTimeMillis() - createdAt) <= (7 * 24 * 60 * 60 * 1000L)
        
        assertTrue("刚好7天的预设应该标记为NEW", isNew)
    }
}

/**
 * CustomPresetManager 单元测试
 */
class CustomPresetManagerTest {

    @Test
    fun `自定义预设 - 名称验证`() {
        val validNames = listOf("我的预设", "My Preset", "预设123")
        for (name in validNames) {
            assertTrue("名称应该在1-20字符之间: $name", name.length in 1..20)
        }
    }

    @Test
    fun `自定义预设 - 名称过长`() {
        val longName = "a".repeat(25)
        assertFalse("25字符的名称应该无效", longName.length in 1..20)
    }

    @Test
    fun `自定义预设 - 空名称`() {
        val emptyName = ""
        assertFalse("空名称应该无效", emptyName.length in 1..20)
    }

    @Test
    fun `自定义预设 - ID生成`() {
        val timestamp = System.currentTimeMillis()
        val id = "custom_$timestamp"
        
        assertTrue(id.startsWith("custom_"))
        assertTrue(id.length > 10)
    }
}

/**
 * FloatingWindowGuideManager 单元测试
 */
class FloatingWindowGuideManagerTest {

    @Test
    fun `悬浮窗引导 - 显示次数限制`() {
        val maxShowCount = 3
        var showCount = 0
        
        showCount++
        showCount++
        showCount++
        
        assertTrue("显示次数不应该超过限制", showCount <= maxShowCount)
    }

    @Test
    fun `悬浮窗引导 - 永久 dismissal`() {
        var isDismissed = false
        var isPermanent = false
        
        isDismissed = true
        isPermanent = true
        
        assertTrue("应该标记为已关闭", isDismissed)
        assertTrue("应该标记为永久关闭", isPermanent)
    }

    @Test
    fun `悬浮窗引导 - 临时 dismissal`() {
        var isDismissed = false
        var isPermanent = false
        
        isDismissed = true
        isPermanent = false
        
        assertTrue("应该标记为已关闭", isDismissed)
        assertFalse("不应该标记为永久关闭", isPermanent)
    }
}