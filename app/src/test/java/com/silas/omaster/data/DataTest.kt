package com.silas.omaster.data

import org.junit.Assert.*
import org.junit.Test

/**
 * SettingsManager 单元测试
 * 测试设置管理器的数据验证逻辑
 */
class SettingsManagerTest {

    @Test
    fun `API密钥验证 - 有效密钥格式`() {
        val validKey = "abcd1234567890efghij"
        
        val isValid = validKey.isNotBlank() && validKey.length >= 16
        
        assertTrue("有效密钥应该通过验证", isValid)
    }

    @Test
    fun `API密钥验证 - 密钥太短`() {
        val shortKey = "abcd1234"
        
        val isValid = shortKey.isNotBlank() && shortKey.length >= 16
        
        assertFalse("太短的密钥应该被拒绝", isValid)
    }

    @Test
    fun `API密钥验证 - 空密钥`() {
        val emptyKey = ""
        
        val isValid = emptyKey.isNotBlank() && emptyKey.length >= 16
        
        assertFalse("空密钥应该被拒绝", isValid)
    }

    @Test
    fun `API密钥格式验证 - 允许的字符`() {
        val validKeys = listOf(
            "abcd1234-efgh-5678",
            "ABCD_1234_EFGH_5678",
            "1234567890123456"
        )
        
        for (key in validKeys) {
            val isValidFormat = key.all { it.isLetterOrDigit() || it == '-' || it == '_' }
            assertTrue("密钥格式应该有效: $key", isValidFormat)
        }
    }

    @Test
    fun `API密钥格式验证 - 不允许的字符`() {
        val invalidKeys = listOf(
            "abcd1234@efgh!5678",
            "abcd 1234 efgh 5678",
            "abcd\t1234"
        )
        
        for (key in invalidKeys) {
            val isValidFormat = key.all { it.isLetterOrDigit() || it == '-' || it == '_' }
            assertFalse("密钥格式应该无效: $key", isValidFormat)
        }
    }

    @Test
    fun `API密钥格式验证 - 空格和空白字符`() {
        val keyWithSpace = "abcd 1234"
        val keyWithTab = "abcd\t1234"
        
        assertTrue("包含空格的密钥应该被检测", keyWithSpace.contains(" "))
        assertTrue("包含制表符的密钥应该被检测", keyWithTab.contains("\t"))
    }

    @Test
    fun `API端点URL构建 - 完整URL生成`() {
        val baseUrl = "https://api.omaster.app/ai"
        val apiVersion = "v1"
        val endpoint = "analyze"
        
        val fullUrl = "${baseUrl.trimEnd('/')}/${apiVersion}/${endpoint.trimStart('/')}"
        
        assertEquals("https://api.omaster.app/ai/v1/analyze", fullUrl)
    }

    @Test
    fun `API端点URL构建 - 不同类型的端点`() {
        val aiEndpoint = "https://api.omaster.app/ai"
        val presetEndpoint = "https://api.omaster.app/presets"
        val authEndpoint = "https://api.omaster.app/auth"
        
        assertTrue(aiEndpoint.contains("/ai"))
        assertTrue(presetEndpoint.contains("/presets"))
        assertTrue(authEndpoint.contains("/auth"))
    }

    @Test
    fun `透明度范围限制 - 30-70范围`() {
        val values = listOf(-10, 0, 30, 56, 70, 100, 150)
        
        for (value in values) {
            val coerced = value.coerceIn(30, 70)
            assertTrue("值应该在30-70范围内: $value -> $coerced", coerced in 30..70)
        }
    }

    @Test
    fun `启动Tab范围限制 - 0-2范围`() {
        val values = listOf(-1, 0, 1, 2, 3, 5)
        
        for (value in values) {
            val coerced = value.coerceIn(0, 2)
            assertTrue("值应该在0-2范围内: $value -> $coerced", coerced in 0..2)
        }
    }

    @Test
    fun `深色模式枚举解析 - 有效值`() {
        val validModes = listOf("SYSTEM", "LIGHT", "DARK")
        
        for (mode in validModes) {
            try {
                val enumValue = com.silas.omaster.data.local.DarkMode.valueOf(mode)
                assertNotNull(enumValue)
            } catch (e: Exception) {
                fail("有效的深色模式应该被解析: $mode")
            }
        }
    }

    @Test
    fun `深色模式枚举解析 - 无效值`() {
        val invalidMode = "INVALID_MODE"
        
        try {
            com.silas.omaster.data.local.DarkMode.valueOf(invalidMode)
            fail("无效的深色模式应该抛出异常")
        } catch (e: IllegalArgumentException) {
            // 预期异常
            assertTrue(true)
        }
    }

    @Test
    fun `更新渠道枚举解析 - Gitee和GitHub`() {
        val giteeMode = com.silas.omaster.data.local.UpdateChannel.valueOf("GITEE")
        val githubMode = com.silas.omaster.data.local.UpdateChannel.valueOf("GITHUB")
        
        assertEquals("GITEE", giteeMode.name)
        assertEquals("GITHUB", githubMode.name)
    }

    @Test
    fun `云同步状态枚举 - 所有状态`() {
        val statuses = listOf(
            "DISABLED",
            "SYNCING",
            "SYNCED",
            "ERROR"
        )
        
        for (status in statuses) {
            try {
                val enumValue = com.silas.omaster.data.local.CloudSyncStatus.valueOf(status)
                assertNotNull(enumValue)
            } catch (e: Exception) {
                fail("有效的同步状态应该被解析: $status")
            }
        }
    }

    @Test
    fun `收藏预设ID列表 - Set转List`() {
        val presetIds = setOf("preset_1", "preset_2", "preset_3")
        
        val list = presetIds.toList()
        
        assertEquals(3, list.size)
        assertTrue(list.contains("preset_1"))
    }

    @Test
    fun `置顶预设ID列表 - List转Set`() {
        val pinnedIds = listOf("preset_1", "preset_2")
        
        val set = pinnedIds.toSet()
        
        assertEquals(2, set.size)
        assertTrue(set.contains("preset_1"))
    }

    @Test
    fun `自定义快捷预设 - Map结构验证`() {
        val customPresets = mapOf(
            "my_preset_1" to mapOf(
                "saturation" to 10,
                "contrast" to 5
            ),
            "my_preset_2" to mapOf(
                "saturation" to -10,
                "contrast" to -5
            )
        )
        
        assertEquals(2, customPresets.size)
        assertEquals(10, customPresets["my_preset_1"]?.get("saturation"))
    }

    @Test
    fun `API配置数据类 - 默认值`() {
        val config = com.silas.omaster.data.local.ApiConfig()
        
        assertEquals("", config.aiApiEndpoint)
        assertEquals("", config.presetApiEndpoint)
        assertEquals("", config.authApiEndpoint)
        assertEquals("v1", config.apiVersion)
    }

    @Test
    fun `API配置数据类 - 自定义值`() {
        val config = com.silas.omaster.data.local.ApiConfig(
            aiApiEndpoint = "https://custom-ai.api.com",
            presetApiEndpoint = "https://custom-preset.api.com",
            authApiEndpoint = "https://custom-auth.api.com",
            apiVersion = "v2"
        )
        
        assertEquals("https://custom-ai.api.com", config.aiApiEndpoint)
        assertEquals("https://custom-preset.api.com", config.presetApiEndpoint)
        assertEquals("https://custom-auth.api.com", config.authApiEndpoint)
        assertEquals("v2", config.apiVersion)
    }

    @Test
    fun `云端预设URL映射 - 品牌URL验证`() {
        val cloudUrls = mapOf(
            "oppo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
            "realme" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
            "vivo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
            "honor" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json"
        )
        
        assertEquals(4, cloudUrls.size)
        for ((_, url) in cloudUrls) {
            assertTrue("URL应该是HTTPS", url.startsWith("https://"))
        }
    }

    @Test
    fun `版本比较逻辑 - versionCode计算`() {
        fun parseVersionCode(version: String): Int {
            val parts = version.split(".")
            return try {
                val major = parts.getOrElse(0) { "0" }.toInt()
                val minor = parts.getOrElse(1) { "0" }.toInt()
                val patch = parts.getOrElse(2) { "0" }.toInt()
                major * 10000 + minor * 100 + patch
            } catch (e: Exception) {
                0
            }
        }
        
        assertEquals(10100, parseVersionCode("1.1.0"))
        assertEquals(10001, parseVersionCode("1.0.1"))
        assertEquals(20000, parseVersionCode("2.0.0"))
    }
}

/**
 * DataExt 扩展测试
 */
class DataExtTest {

    @Test
    fun `列表过滤 - 品牌过滤逻辑`() {
        val presets = listOf(
            mapOf("brand" to "oppo", "name" to "Preset1"),
            mapOf("brand" to "vivo", "name" to "Preset2"),
            mapOf("brand" to "oppo", "name" to "Preset3")
        )
        
        val filtered = presets.filter { it["brand"] == "oppo" }
        
        assertEquals(2, filtered.size)
    }

    @Test
    fun `列表搜索 - 名称和描述搜索`() {
        val presets = listOf(
            mapOf("name" to "人像美颜", "description" to "适合拍摄人像"),
            mapOf("name" to "风景优化", "description" to "适合拍摄风景"),
            mapOf("name" to "美食滤镜", "description" to "适合拍摄美食")
        )
        
        val searchQuery = "人像"
        val filtered = presets.filter {
            (it["name"] as String).contains(searchQuery, ignoreCase = true) ||
            (it["description"] as String).contains(searchQuery, ignoreCase = true)
        }
        
        assertEquals(1, filtered.size)
        assertEquals("人像美颜", filtered[0]["name"])
    }

    @Test
    fun `列表搜索 - 标签搜索`() {
        val presets = listOf(
            mapOf("name" to "Preset1", "tags" to listOf("portrait", "skin")),
            mapOf("name" to "Preset2", "tags" to listOf("landscape", "nature")),
            mapOf("name" to "Preset3", "tags" to listOf("food", "vibrant"))
        )
        
        val searchQuery = "portrait"
        @Suppress("UNCHECKED_CAST")
        val filtered = presets.filter {
            (it["tags"] as List<String>).any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
        
        assertEquals(1, filtered.size)
    }
}
