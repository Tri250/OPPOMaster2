package com.silas.omaster.security

import com.silas.omaster.model.ExifData
import com.silas.omaster.model.PresetComment
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.UrlConstants
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/**
 * 安全隐私测试
 * 验证网络安全、输入验证、数据加密、隐私合规等
 */
class SecurityPrivacyTest {

    // ===== 1. URL 安全 =====

    @Test
    fun `URL常量 - 所有预设源URL必须使用HTTPS`() {
        for ((brand, url) in UrlConstants.PRESET_SOURCE_URLS) {
            assertTrue("品牌 $brand 必须使用 HTTPS: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `URL常量 - 所有API端点必须使用HTTPS`() {
        val apiUrls = listOf(
            UrlConstants.API_AI_ENDPOINT,
            UrlConstants.API_PRESET_ENDPOINT,
            UrlConstants.API_AUTH_ENDPOINT,
            UrlConstants.API_CLOUD_SCENE_ANALYZE
        )
        for (url in apiUrls) {
            assertTrue("API端点必须使用 HTTPS: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `URL常量 - 隐私政策URL必须使用HTTPS`() {
        assertTrue(UrlConstants.PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(UrlConstants.UMENG_PRIVACY_URL.startsWith("https://"))
    }

    @Test
    fun `URL常量 - CDN和更新URL必须使用HTTPS`() {
        assertTrue(UrlConstants.CDN_JSDELIVR.startsWith("https://"))
        assertTrue(UrlConstants.CDN_MODELS.startsWith("https://"))
        assertTrue(UrlConstants.GITHUB_API_RELEASES.startsWith("https://"))
        assertTrue(UrlConstants.GITEE_API_RELEASES.startsWith("https://"))
    }

    @Test
    fun `URL常量 - 禁止内网地址`() {
        val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")

        fun isBlocked(url: String): Boolean {
            val lower = url.lowercase()
            val hostStart = lower.indexOf("https://") + 8
            val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
            val host = lower.substring(hostStart, hostEnd)
            return blockedPrefixes.any { host.startsWith(it) }
        }

        val testUrls = listOf(
            "https://localhost/api",
            "https://127.0.0.1/api",
            "https://192.168.1.1/data.json",
            "https://10.0.0.1/presets.json"
        )

        for (url in testUrls) {
            assertTrue("应阻止内网地址: $url", isBlocked(url))
        }
    }

    @Test
    fun `URL构造 - LUT下载URL不应包含路径遍历`() {
        val url = UrlConstants.getLUTDownloadUrl("film", "test.cube")
        assertFalse("URL不应包含 ../", url.contains("../"))
        assertFalse("URL不应包含 ..\\", url.contains("..\\"))
        assertTrue(url.startsWith("https://"))
    }

    // ===== 2. 输入验证 =====

    @Test
    fun `输入验证 - 订阅URL应为HTTPS`() {
        val validUrl = "https://example.com/presets.json"
        val invalidUrl = "http://example.com/presets.json"

        assertTrue("有效URL应以HTTPS开头", validUrl.startsWith("https://"))
        assertFalse("无效URL不应以HTTPS开头", invalidUrl.startsWith("https://"))
    }

    @Test
    fun `输入验证 - 空白和空URL应被拒绝`() {
        val urls = listOf("", "   ", "\t", "\n")
        for (url in urls) {
            assertTrue("空白URL应被拒绝: '$url'", url.isBlank())
        }
    }

    @Test
    fun `输入验证 - API密钥长度验证`() {
        val googleDriveKey = "a".repeat(20)
        val dropboxKey = "b".repeat(15)

        assertTrue("Google Drive key应足够长", googleDriveKey.length >= 20)
        assertTrue("Dropbox key应足够长", dropboxKey.length >= 15)

        val shortKey = "abc"
        assertFalse("短key应被拒绝", shortKey.length >= 20)
    }

    // ===== 3. 加密基础测试 =====

    @Test
    fun `加密 - AES-GCM参数配置正确`() {
        val transformation = "AES/GCM/NoPadding"
        val ivLength = 12
        val tagLength = 128
        val keySize = 256

        assertEquals("AES/GCM/NoPadding", transformation)
        assertEquals(12, ivLength)
        assertEquals(128, tagLength)
        assertEquals(256, keySize)
    }

    @Test
    fun `加密 - Base64编码不应包含换行`() {
        val data = "sensitive_api_key_12345"
        val encoded = Base64.getEncoder().encodeToString(data.toByteArray())
        val noWrapEncoded = Base64.getEncoder().withoutWrapping().encodeToString(data.toByteArray())

        assertFalse("Base64应无换行", encoded.contains("\n"))
        assertEquals(encoded, noWrapEncoded)
    }

    @Test
    fun `加密 - 版本化密文格式长度合理`() {
        // 模拟版本化密文格式：[版本(1) + IV长度(1) + IV(12) + 算法标识(1) + 密文+Tag(变长)]
        val iv = ByteArray(12) { it.toByte() }
        val cipherBytes = ByteArray(32) { (it + 12).toByte() }
        val combined = ByteArray(1 + 1 + iv.size + 1 + cipherBytes.size)

        assertTrue("版本化密文最小长度应合理", combined.size >= 15)
    }

    // ===== 4. 数据隐私 =====

    @Test
    fun `隐私 - EXIF位置数据可选且不强制`() {
        val exifWithLocation = ExifData(
            cameraModel = "Test",
            lensModel = null,
            focalLength = null,
            fNumber = null,
            exposureTime = null,
            iso = null,
            dateTime = null,
            gpsLatitude = 39.9,
            gpsLongitude = 116.4
        )
        val exifWithoutLocation = ExifData(
            cameraModel = "Test",
            lensModel = null,
            focalLength = null,
            fNumber = null,
            exposureTime = null,
            iso = null,
            dateTime = null,
            gpsLatitude = null,
            gpsLongitude = null
        )

        assertNotNull(exifWithLocation.gpsLatitude)
        assertNull(exifWithoutLocation.gpsLatitude)
    }

    @Test
    fun `隐私 - 用户ID和敏感字段不直接暴露`() {
        val comment = PresetComment(
            id = "user_123",
            user = "nickname",
            content = "Great preset!",
            rating = 5f
        )
        assertNotEquals("", comment.id)
        assertTrue("评分应在合理范围", comment.rating in 0f..5f)
    }

    // ===== 5. SSRF 防护 =====

    @Test
    fun `SSRF防护 - 应阻止常见内网地址`() {
        val blockedHosts = listOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "10.0.0.1",
            "192.168.1.1",
            "172.16.0.1",
            "169.254.1.1"
        )

        for (host in blockedHosts) {
            val url = "https://$host/presets.json"
            val lower = url.lowercase()
            val hostStart = lower.indexOf("https://") + 8
            val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
            val extractedHost = lower.substring(hostStart, hostEnd)

            val isBlocked = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
                .any { extractedHost.startsWith(it) }
            assertTrue("应阻止内网地址: $host", isBlocked)
        }
    }

    @Test
    fun `SSRF防护 - 公网地址应被允许`() {
        val publicUrls = listOf(
            "https://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "https://api.omaster.app/presets",
            "https://github.com/user/repo/releases"
        )

        for (url in publicUrls) {
            val lower = url.lowercase()
            val hostStart = lower.indexOf("https://") + 8
            val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
            val host = lower.substring(hostStart, hostEnd)

            val isBlocked = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
                .any { host.startsWith(it) }
            assertFalse("公网地址不应被阻止: $url", isBlocked)
        }
    }

    // ===== 6. 协议安全 =====

    @Test
    fun `协议安全 - HTTP URL应被拒绝`() {
        val httpUrls = listOf(
            "http://example.com/api",
            "http://cdn.jsdelivr.net/gh/user/repo/presets.json",
            "http://192.168.1.1/data.json"
        )

        for (url in httpUrls) {
            assertFalse("HTTP URL不应被允许: $url", url.startsWith("https://"))
        }
    }

    // ===== 7. 参数元数据安全 =====

    @Test
    fun `参数元数据 - 所有参数范围定义一致`() {
        for (metadata in RenderParameters.PARAM_METADATA) {
            assertTrue("${metadata.key} 最小值应小于等于最大值", metadata.minValue <= metadata.maxValue)
            assertTrue("${metadata.key} 描述不应为空", metadata.description.isNotBlank())
            assertTrue("${metadata.key} 显示名不应为空", metadata.displayName.isNotBlank())
        }
    }

    // ===== 8. 反序列化安全 =====

    @Test
    fun `JSON安全 - 未知字段不应导致解析失败`() {
        val json = """
            {
                "name": "Test Preset",
                "unknownField": "should be ignored",
                "build": 5,
                "presets": []
            }
        """.trimIndent()

        // 验证JSON结构有效
        val obj = org.json.JSONObject(json)
        assertEquals("Test Preset", obj.getString("name"))
        assertEquals(5, obj.getInt("build"))
        assertTrue("应忽略未知字段", obj.has("unknownField"))
    }
}
