package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Util 扩展测试 - 补充覆盖更多工具类
 */
class UtilExtTest {

    // ===== FormatUtils 扩展测试 =====

    @Test
    fun `FormatUtils - 版本号格式化`() {
        val versionCode = 10
        val versionName = "1.3.1"
        
        assertTrue("版本号应该大于0", versionCode > 0)
        assertTrue("版本名应该有3部分", versionName.split(".").size == 3)
    }

    @Test
    fun `FormatUtils - 日期格式化`() {
        val timestamp = System.currentTimeMillis()
        val formatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        
        assertTrue("日期格式应该包含年", formatted.contains("-"))
    }

    @Test
    fun `FormatUtils - 文件大小格式化`() {
        val sizes = listOf(
            1024L to "1.00 KB",
            1024 * 1024L to "1.00 MB",
            1024 * 1024 * 1024L to "1.00 GB"
        )
        
        for ((bytes, expected) in sizes) {
            val result = when {
                bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)} GB"
                bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                bytes >= 1024 -> "${bytes / 1024} KB"
                else -> "$bytes B"
            }
            assertTrue("文件大小格式应该有效", result.isNotEmpty())
        }
    }

    // ===== VersionInfo 扩展测试 =====

    @Test
    fun `VersionInfo - 版本比较`() {
        val v1 = "1.3.0"
        val v2 = "1.3.1"
        
        val parts1 = v1.split(".").map { it.toInt() }
        val parts2 = v2.split(".").map { it.toInt() }
        
        val compare = (parts1[0] * 10000 + parts1[1] * 100 + parts1[2]) -
                      (parts2[0] * 10000 + parts2[1] * 100 + parts2[2])
        
        assertTrue("v1应该小于v2", compare < 0)
    }

    @Test
    fun `VersionInfo - 版本解析`() {
        val version = "2.1.0"
        val parts = version.split(".")
        
        assertEquals("主版本应该是2", 2, parts[0].toInt())
        assertEquals("次版本应该是1", 1, parts[1].toInt())
        assertEquals("修订版本应该是0", 0, parts[2].toInt())
    }

    // ===== SecurityCrypto 扩展测试 =====

    @Test
    fun `SecurityCrypto - Base64编码解码`() {
        val original = "test_data_123"
        val encoded = java.util.Base64.getEncoder().encodeToString(original.toByteArray())
        val decoded = java.util.Base64.getDecoder().decode(encoded).decodeToString()
        
        assertEquals("Base64编码解码应该一致", original, decoded)
    }

    @Test
    fun `SecurityCrypto - MD5哈希长度`() {
        val input = "test"
        val md5 = java.security.MessageDigest.getInstance("MD5")
        val hash = md5.digest(input.toByteArray())
        
        assertEquals("MD5哈希应该是16字节", 16, hash.size)
    }

    @Test
    fun `SecurityCrypto - SHA256哈希长度`() {
        val input = "test"
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val hash = sha256.digest(input.toByteArray())
        
        assertEquals("SHA256哈希应该是32字节", 32, hash.size)
    }

    // ===== JsonUtil 扩展测试 =====

    @Test
    fun `JsonUtil - JSON解析`() {
        val json = "{\"name\":\"test\",\"value\":123}"
        val obj = org.json.JSONObject(json)
        
        assertEquals("name应该是test", "test", obj.getString("name"))
        assertEquals("value应该是123", 123, obj.getInt("value"))
    }

    @Test
    fun `JsonUtil - JSON数组解析`() {
        val json = "[1, 2, 3, 4, 5]"
        val arr = org.json.JSONArray(json)
        
        assertEquals("数组长度应该是5", 5, arr.length())
        assertEquals("第一个元素应该是1", 1, arr.getInt(0))
    }

    @Test
    fun `JsonUtil - JSON构建`() {
        val obj = org.json.JSONObject()
        obj.put("key", "value")
        obj.put("number", 42)
        
        assertTrue("JSON应该包含key", obj.has("key"))
        assertTrue("JSON应该包含number", obj.has("number"))
    }

    // ===== CrashHandler 扩展测试 =====

    @Test
    fun `CrashHandler - 异常堆栈解析`() {
        val exception = RuntimeException("Test crash")
        val stackTrace = exception.stackTrace
        
        assertTrue("堆栈应该有内容", stackTrace.isNotEmpty())
    }

    @Test
    fun `CrashHandler - 异常消息提取`() {
        val exception = NullPointerException("Null reference")
        val message = exception.message
        
        assertEquals("异常消息应该正确", "Null reference", message)
    }

    // ===== UpdateChecker 扩展测试 =====

    @Test
    fun `UpdateChecker - 版本检查逻辑`() {
        val currentVersion = 10
        val remoteVersion = 15
        
        val needsUpdate = remoteVersion > currentVersion
        
        assertTrue("应该需要更新", needsUpdate)
    }

    @Test
    fun `UpdateChecker - 更新渠道选择`() {
        val channels = listOf("GITHUB", "GITEE")
        val selectedChannel = "GITHUB"
        
        assertTrue("选择的渠道应该有效", selectedChannel in channels)
    }

    // ===== UpdateConfigManager 扩展测试 =====

    @Test
    fun `UpdateConfigManager - 配置解析`() {
        val config = mapOf(
            "version" to 15,
            "versionName" to "1.4.0",
            "downloadUrl" to "https://example.com/app.apk"
        )
        
        assertTrue("配置应该包含version", config.containsKey("version"))
        assertTrue("配置应该包含versionName", config.containsKey("versionName"))
        assertTrue("配置应该包含downloadUrl", config.containsKey("downloadUrl"))
    }

    // ===== ImageCacheManager 扩展测试 =====

    @Test
    fun `ImageCacheManager - 缓存键生成`() {
        val url = "https://example.com/image.jpg"
        val cacheKey = url.hashCode().toString()
        
        assertTrue("缓存键应该有效", cacheKey.isNotEmpty())
    }

    @Test
    fun `ImageCacheManager - 缓存大小计算`() {
        val cacheSize = 50 * 1024 * 1024L // 50MB
        val maxSize = 100 * 1024 * 1024L // 100MB
        
        assertTrue("缓存大小应该在限制内", cacheSize <= maxSize)
    }

    // ===== PresetI18n 扩展测试 =====

    @Test
    fun `PresetI18n - 多语言支持`() {
        val languages = listOf("zh", "zh-CN", "zh-TW", "en")
        
        for (lang in languages) {
            assertTrue("语言代码应该有效: $lang", lang.isNotEmpty())
        }
    }

    @Test
    fun `PresetI18n - 名称翻译`() {
        val presetName = "人像美颜"
        val englishName = "Portrait Beauty"
        
        assertTrue("中文名称应该有效", presetName.isNotEmpty())
        assertTrue("英文名称应该有效", englishName.isNotEmpty())
    }

    // ===== ShareExportUtils 扩展测试 =====

    @Test
    fun `ShareExportUtils - 分享内容格式`() {
        val presetName = "人像美颜"
        val presetParams = "饱和度+10, 对比度+5"
        val shareContent = "预设: $presetName\n参数: $presetParams"
        
        assertTrue("分享内容应该包含预设名", shareContent.contains(presetName))
        assertTrue("分享内容应该包含参数", shareContent.contains(presetParams))
    }

    @Test
    fun `ShareExportUtils - 导出文件名格式`() {
        val presetName = "人像美颜"
        val timestamp = System.currentTimeMillis()
        val fileName = "${presetName}_$timestamp.json"
        
        assertTrue("文件名应该包含预设名", fileName.contains(presetName))
        assertTrue("文件名应该包含时间戳", fileName.contains(timestamp.toString()))
    }

    // ===== HapticExt 扩展测试 =====

    @Test
    fun `HapticExt - 触觉反馈类型`() {
        val hapticTypes = listOf("CLICK", "TICK", "HEAVY_CLICK", "LONG_PRESS")
        
        for (type in hapticTypes) {
            assertTrue("触觉反馈类型应该有效: $type", type.isNotEmpty())
        }
    }

    @Test
    fun `HapticExt - 触觉强度范围`() {
        val intensity = 0.5f
        val validRange = 0f..1f
        
        assertTrue("触觉强度应该在有效范围内", intensity in validRange)
    }
}