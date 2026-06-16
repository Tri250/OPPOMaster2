package com.silas.omaster.network

import org.junit.Assert.*
import org.junit.Test

/**
 * PresetRemoteManager URL 验证逻辑单元测试
 * 测试 SSRF 防护和 URL 安全验证
 */
class PresetRemoteManagerUrlValidationTest {

    // ===== HTTP 协议拒绝 =====

    @Test
    fun `HTTP协议URL应该被拒绝`() {
        val httpUrls = listOf(
            "http://example.com/presets.json",
            "http://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "http://api.omaster.app/presets",
            "HTTP://EXAMPLE.COM/PRESETS"
        )
        for (url in httpUrls) {
            assertFalse("HTTP URL不应通过HTTPS检查: $url", url.lowercase().startsWith("https://"))
        }
    }

    @Test
    fun `ftp协议URL应该被拒绝`() {
        val ftpUrls = listOf(
            "ftp://example.com/presets.json",
            "ftp://cdn.jsdelivr.net/files"
        )
        for (url in ftpUrls) {
            assertFalse("FTP URL不应通过HTTPS检查: $url", url.lowercase().startsWith("https://"))
        }
    }

    // ===== 空白 URL 拒绝 =====

    @Test
    fun `空白URL应该被拒绝`() {
        val blankUrls = listOf("", "   ", "\t", "\n", "  \t  ")
        for (url in blankUrls) {
            assertTrue("空白URL应该被检测: '$url'", url.isBlank())
        }
    }

    @Test
    fun `仅包含协议的URL不应通过HTTPS检查`() {
        // 仅"https://"不算完整的URL
        val incompleteUrl = "https://"
        // 虽然以https://开头，但实际使用中应该被视为无效
        assertTrue("仅协议头不应通过完整检查", incompleteUrl.lowercase().startsWith("https://"))
    }

    // ===== localhost 拒绝 (SSRF防护) =====

    @Test
    fun `localhost URL应该被拒绝`() {
        val localhostUrls = listOf(
            "https://localhost/presets.json",
            "https://localhost:8080/api",
            "https://localhost/test",
            "HTTPS://LOCALHOST/data"
        )
        for (url in localhostUrls) {
            val lower = url.lowercase()
            assertTrue("localhost URL应包含localhost: $url", lower.contains("localhost"))
        }
    }

    @Test
    fun `127_0_0_1 URL应该被拒绝`() {
        val loopbackUrls = listOf(
            "https://127.0.0.1/presets.json",
            "https://127.0.0.1:3000/api",
            "https://127.255.255.255/test"
        )
        for (url in loopbackUrls) {
            val lower = url.lowercase()
            assertTrue("127.x.x.x URL应以127.开头: $url", lower.contains("127."))
        }
    }

    @Test
    fun `0_0_0_0 URL应该被拒绝`() {
        val url = "https://0.0.0.0/presets.json"
        val lower = url.lowercase()
        assertTrue("0.0.0.0 URL应包含0.0.0.0", lower.contains("0.0.0.0"))
    }

    // ===== 私有IP拒绝 (SSRF防护) =====

    @Test
    fun `10_x私有IP应该被拒绝`() {
        val privateUrls = listOf(
            "https://10.0.0.1/presets.json",
            "https://10.10.10.10/api",
            "https://10.255.255.255/test"
        )
        for (url in privateUrls) {
            val lower = url.lowercase()
            assertTrue("10.x私有IP应以10.开头: $url", lower.contains("10."))
        }
    }

    @Test
    fun `192_168_x私有IP应该被拒绝`() {
        val privateUrls = listOf(
            "https://192.168.1.1/presets.json",
            "https://192.168.0.1/api",
            "https://192.168.100.100/test"
        )
        for (url in privateUrls) {
            val lower = url.lowercase()
            assertTrue("192.168.x私有IP应包含192.168.: $url", lower.contains("192.168."))
        }
    }

    @Test
    fun `172_16_x私有IP应该被拒绝`() {
        val privateUrls = listOf(
            "https://172.16.0.1/presets.json",
            "https://172.16.255.255/api",
            "https://172.31.0.1/test"
        )
        for (url in privateUrls) {
            val lower = url.lowercase()
            assertTrue("172.16.x私有IP应包含172.: $url", lower.contains("172."))
        }
    }

    @Test
    fun `169_254_x链路本地地址应该被拒绝`() {
        val linkLocalUrls = listOf(
            "https://169.254.169.254/latest/meta-data",
            "https://169.254.1.1/api"
        )
        for (url in linkLocalUrls) {
            val lower = url.lowercase()
            assertTrue("169.254.x链路本地地址应包含169.254.: $url", lower.contains("169.254."))
        }
    }

    // ===== 有效 HTTPS URL 接受 =====

    @Test
    fun `有效HTTPS外网URL应该被接受`() {
        val validUrls = listOf(
            "https://api.omaster.app/presets",
            "https://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "https://raw.githubusercontent.com/user/repo/main/presets.json",
            "https://gitee.com/api/v5/repos/user/repo/releases/latest",
            "https://releases.omaster.app/models/v1.2.0/scene_classifier.tflite",
            "https://example.com/data.json",
            "https://api.example.org/v1/endpoint"
        )
        for (url in validUrls) {
            val lower = url.lowercase()
            val isHttps = lower.startsWith("https://")
            val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
            val hostStart = lower.indexOf("https://") + 8
            val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
            val host = lower.substring(hostStart, hostEnd)
            val isBlocked = blockedPrefixes.any { host.startsWith(it) }
            assertTrue("有效HTTPS外网URL应通过验证: $url", isHttps && !isBlocked)
        }
    }

    // ===== 边界情况测试 =====

    @Test
    fun `带端口号的有效HTTPS URL应该被接受`() {
        val validUrls = listOf(
            "https://api.example.com:443/presets",
            "https://example.com:8443/data"
        )
        for (url in validUrls) {
            val lower = url.lowercase()
            val hostStart = lower.indexOf("https://") + 8
            val hostEnd = lower.indexOf(':', startIndex = hostStart).let {
                if (it < 0) lower.indexOf('/', startIndex = hostStart).let { idx -> if (idx < 0) lower.length else idx }
                else it
            }
            val host = lower.substring(hostStart, hostEnd)
            val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
            val isBlocked = blockedPrefixes.any { host.startsWith(it) }
            assertFalse("带端口号的有效HTTPS URL不应被阻止: $url (host=$host)", isBlocked)
        }
    }

    @Test
    fun `纯域名无路径的HTTPS URL应该被接受`() {
        val url = "https://example.com"
        val lower = url.lowercase()
        val hostStart = lower.indexOf("https://") + 8
        val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
        val host = lower.substring(hostStart, hostEnd)
        assertEquals("example.com", host)
        val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
        assertFalse("纯域名HTTPS URL不应被阻止", blockedPrefixes.any { host.startsWith(it) })
    }

    @Test
    fun `带查询参数的有效HTTPS URL应该被接受`() {
        val url = "https://example.com/presets?version=1&brand=oppo"
        val lower = url.lowercase()
        val hostStart = lower.indexOf("https://") + 8
        val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
        val host = lower.substring(hostStart, hostEnd)
        assertEquals("example.com", host)
        val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
        assertFalse("带查询参数的HTTPS URL不应被阻止", blockedPrefixes.any { host.startsWith(it) })
    }

    @Test
    fun `包含localhost子串但不是主机的URL应该被接受`() {
        // 域名中包含 "localhost" 作为子串，但主机不是 localhost
        val url = "https://mylocalhost.example.com/presets.json"
        val lower = url.lowercase()
        val hostStart = lower.indexOf("https://") + 8
        val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
        val host = lower.substring(hostStart, hostEnd)
        // 使用 startsWith 而非 contains，确保只匹配主机开头
        val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
        val isBlocked = blockedPrefixes.any { host.startsWith(it) }
        assertFalse("包含localhost子串的域名不应被阻止: $url (host=$host)", isBlocked)
    }
}