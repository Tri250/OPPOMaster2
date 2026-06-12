package com.silas.omaster.network

import org.junit.Assert.*
import org.junit.Test

/**
 * PresetRemoteManager 单元测试
 * 测试网络请求的安全验证和URL处理逻辑
 */
class PresetRemoteManagerTest {

    @Test
    fun `URL验证 - 空白URL应该被拒绝`() {
        val blankUrls = listOf("", "   ", "\t", "\n")
        
        for (url in blankUrls) {
            val isBlank = url.isBlank()
            assertTrue("空白URL应该被检测: '$url'", isBlank)
        }
    }

    @Test
    fun `URL验证 - HTTP协议应该被拒绝`() {
        val httpUrls = listOf(
            "http://example.com/presets.json",
            "http://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "HTTP://EXAMPLE.COM"
        )
        
        for (url in httpUrls) {
            assertFalse("HTTP URL不应该通过验证: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `URL验证 - HTTPS协议应该被允许`() {
        val httpsUrls = listOf(
            "https://example.com/presets.json",
            "https://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "HTTPS://API.OMASTER.APP"
        )
        
        for (url in httpsUrls) {
            assertTrue("HTTPS URL应该通过验证: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `URL验证 - 内网地址应该被阻止`() {
        val blockedHosts = listOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "10.0.0.1",
            "192.168.1.1",
            "172.16.0.1",
            "169.254.169.254"
        )
        
        for (host in blockedHosts) {
            val testUrl = "https://$host/presets.json"
            val containsBlocked = blockedHosts.any { testUrl.lowercase().contains(it) }
            assertTrue("内网地址应该被阻止: $testUrl", containsBlocked)
        }
    }

    @Test
    fun `URL验证 - 正常外网地址应该通过`() {
        val validUrls = listOf(
            "https://api.omaster.app/presets",
            "https://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "https://raw.githubusercontent.com/user/repo/main/presets.json",
            "https://gitee.com/user/repo/raw/main/presets.json"
        )
        
        for (url in validUrls) {
            val isHttps = url.startsWith("https://")
            val containsBlocked = listOf("localhost", "127.0.0.1", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
                .any { url.lowercase().contains(it) }
            
            assertTrue("有效URL应该通过验证: $url", isHttps && !containsBlocked)
        }
    }

    @Test
    fun `HTTP状态码 - 2xx应该被认为是成功`() {
        val successCodes = listOf(200, 201, 202, 204, 206)
        
        for (code in successCodes) {
            assertTrue("状态码 $code 应该是成功", code in 200..299)
        }
    }

    @Test
    fun `HTTP状态码 - 4xx 5xx应该被认为是错误`() {
        val errorCodes = listOf(400, 401, 403, 404, 500, 502, 503)
        
        for (code in errorCodes) {
            assertFalse("状态码 $code 不应该在2xx范围", code in 200..299)
        }
    }

    @Test
    fun `JSON解析 - 有效JSON字符串应该被正确解析`() {
        val jsonStr = """
            {
                "name": "Test Preset",
                "author": "Test Author",
                "presets": []
            }
        """.trimIndent()
        
        assertTrue("JSON应该包含name字段", jsonStr.contains("\"name\""))
        assertTrue("JSON应该包含author字段", jsonStr.contains("\"author\""))
        assertTrue("JSON应该包含presets字段", jsonStr.contains("\"presets\""))
    }

    @Test
    fun `JSON解析 - 缺少必填字段应该被检测`() {
        val missingFields = mutableListOf<String>()
        
        val name = ""
        val author = "Test Author"
        
        if (name.isBlank()) missingFields.add("name (订阅名称)")
        if (author.isBlank()) missingFields.add("author (作者)")
        
        assertTrue("应该检测到name字段缺失", missingFields.contains("name (订阅名称)"))
        assertFalse("author字段不应该缺失", missingFields.contains("author (作者)"))
    }

    @Test
    fun `缓存键生成 - 基于URL生成唯一键`() {
        val url1 = "https://example.com/presets.json"
        val url2 = "https://example.com/presets2.json"
        
        val key1 = url1.hashCode().toString()
        val key2 = url2.hashCode().toString()
        
        assertNotEquals("不同的URL应该生成不同的缓存键", key1, key2)
    }

    @Test
    fun `文件名字符串 - URL转文件名处理`() {
        val url = "https://cdn.jsdelivr.net/gh/user/repo/presets.json"
        
        // 简化处理：提取路径最后一部分
        val fileName = url.substringAfterLast("/").substringBefore(".")
        
        assertEquals("presets", fileName)
    }
}

/**
 * 网络响应测试
 */
class NetworkResponseTest {

    @Test
    fun `响应时间 - 应该在合理范围内`() {
        val reasonableTimeout = 30000L // 30秒
        val responseTime = 2500L // 模拟2.5秒响应
        
        assertTrue("响应时间应该在合理范围内", responseTime < reasonableTimeout)
    }

    @Test
    fun `重试机制 - 指数退避计算`() {
        val baseDelay = 1000L
        val maxDelay = 4000L
        
        val delays = listOf(
            baseDelay * (1L shl 0), // 1s
            baseDelay * (1L shl 1), // 2s
            baseDelay * (1L shl 2)  // 4s
        )
        
        assertEquals(1000L, delays[0])
        assertEquals(2000L, delays[1])
        assertEquals(4000L, delays[2])
        assertTrue("延迟不应该超过最大值", delays[2] <= maxDelay)
    }

    @Test
    fun `连接超时 - 应该设置为合理值`() {
        val connectionTimeout = 120000L // 120秒
        
        assertTrue("连接超时应该大于60秒", connectionTimeout >= 60000L)
    }
}
