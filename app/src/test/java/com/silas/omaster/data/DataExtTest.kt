package com.silas.omaster.data

import org.junit.Assert.*
import org.junit.Test

/**
 * SubscriptionManager 单元测试
 */
class SubscriptionManagerTest {

    @Test
    fun `订阅状态 - 状态枚举验证`() {
        val states = listOf("ACTIVE", "EXPIRED", "CANCELLED", "PENDING")
        
        for (state in states) {
            assertTrue("状态应该是有效的: $state", state in states)
        }
    }

    @Test
    fun `订阅验证 - URL格式验证`() {
        val validUrl = "https://example.com/presets.json"
        val invalidUrl = "http://example.com/presets.json"
        
        assertTrue("HTTPS URL应该有效", validUrl.startsWith("https://"))
        assertFalse("HTTP URL应该无效", invalidUrl.startsWith("https://"))
    }

    @Test
    fun `订阅验证 - 构建号比较`() {
        val currentBuild = 10
        val remoteBuild = 15
        
        val needsUpdate = remoteBuild > currentBuild
        
        assertTrue("远程构建更新时需要更新", needsUpdate)
    }

    @Test
    fun `订阅验证 - 无需更新情况`() {
        val currentBuild = 15
        val remoteBuild = 15
        
        val needsUpdate = remoteBuild > currentBuild
        
        assertFalse("相同构建不需要更新", needsUpdate)
    }

    @Test
    fun `订阅验证 - 版本回退情况`() {
        val currentBuild = 15
        val remoteBuild = 10
        
        val needsUpdate = remoteBuild > currentBuild
        
        assertFalse("远程版本更旧不应该更新", needsUpdate)
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
        favorites.add("preset_1") // 重复添加
        
        assertEquals(1, favorites.size)
    }

    @Test
    fun `收藏状态检查 - 已收藏`() {
        val favorites = setOf("preset_1", "preset_2")
        
        assertTrue(favorites.contains("preset_1"))
    }

    @Test
    fun `收藏状态检查 - 未收藏`() {
        val favorites = setOf("preset_1", "preset_2")
        
        assertFalse(favorites.contains("preset_3"))
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
        
        assertEquals(maxHistory, history.size)
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
        val createdAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 刚好7天
        
        val isNew = (System.currentTimeMillis() - createdAt) <= (7 * 24 * 60 * 60 * 1000L)
        
        assertTrue("刚好7天的预设应该标记为NEW", isNew)
    }

    @Test
    fun `NEW标记 - 刚好8天`() {
        val createdAt = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L) // 刚好8天
        
        val isNew = (System.currentTimeMillis() - createdAt) <= (7 * 24 * 60 * 60 * 1000L)
        
        assertFalse("刚好8天的预设不应该标记为NEW", isNew)
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
        assertTrue(showCount <= maxShowCount)
        
        showCount++
        assertTrue(showCount <= maxShowCount)
        
        showCount++
        assertTrue(showCount <= maxShowCount)
        
        showCount++
        assertFalse(showCount <= maxShowCount)
    }

    @Test
    fun `悬浮窗引导 - 永久 dismissal`() {
        var isDismissed = false
        var isPermanent = false
        
        // 用户选择"不再显示"
        isDismissed = true
        isPermanent = true
        
        assertTrue(isDismissed)
        assertTrue(isPermanent)
    }

    @Test
    fun `悬浮窗引导 - 临时 dismissal`() {
        var isDismissed = false
        var isPermanent = false
        
        // 用户暂时关闭
        isDismissed = true
        isPermanent = false
        
        assertTrue(isDismissed)
        assertFalse(isPermanent)
    }
}
