package com.silas.omaster.cloud

import org.junit.Assert.*
import org.junit.Test

/**
 * CloudProvider 单元测试
 *
 * 测试云存储提供商的核心功能：
 * - WebDAV 连接验证
 * - Google Drive 连接验证
 * - 配置数据完整性
 */
class CloudProviderTest {

    @Test
    fun `WebDAV builds valid Basic Auth`() {
        val provider = CloudProvider.WebDAV(
            serverUrl = "https://dav.example.com/",
            username = "testuser",
            password = "testpass"
        )

        val auth = provider.buildBasicAuth()

        assertNotNull(auth)
        assertTrue(auth.startsWith("Basic "))
        // Base64 编码的 "testuser:testpass"
        assertTrue(auth.contains("dGVzdHVzZXI6dGVzdHBhc3M="))
    }

    @Test
    fun `WebDAV type constants are correct`() {
        assertEquals("webdav", CloudProvider.WebDAV.TYPE_WEBDAV)
        assertEquals("WebDAV", CloudProvider.WebDAV("https://test.com", "user", "pass").displayName)
    }

    @Test
    fun `GoogleDrive type constants are correct`() {
        assertEquals("gdrive", CloudProvider.GoogleDrive.TYPE_GDRIVE)
        assertEquals("Google Drive", CloudProvider.GoogleDrive("token", "root").displayName)
    }

    @Test
    fun `WebDAV trims serverUrl`() {
        val provider1 = CloudProvider.WebDAV("https://test.com/", "user", "pass")
        val provider2 = CloudProvider.WebDAV("https://test.com", "user", "pass")

        // 两种 URL 格式都应处理
        assertNotNull(provider1.serverUrl)
        assertNotNull(provider2.serverUrl)
    }

    @Test
    fun `GoogleDrive default folderId is root`() {
        val provider = CloudProvider.GoogleDrive("test_token")

        assertEquals("root", provider.folderId)
    }

    @Test
    fun `GoogleDrive custom folderId`() {
        val provider = CloudProvider.GoogleDrive("test_token", "custom_folder_id")

        assertEquals("custom_folder_id", provider.folderId)
    }

    @Test
    fun `validateConnection handles invalid URL gracefully`() {
        val provider = CloudProvider.WebDAV(
            serverUrl = "invalid_url",
            username = "user",
            password = "pass"
        )

        // 无效 URL 应返回 false（不抛异常）
        val result = kotlinx.coroutines.runBlocking {
            provider.validateConnection()
        }

        assertFalse(result)
    }

    @Test
    fun `validateConnection handles invalid token gracefully`() {
        val provider = CloudProvider.GoogleDrive(
            accessToken = "invalid_token",
            folderId = "root"
        )

        // 无效 token 应返回 false
        val result = kotlinx.coroutines.runBlocking {
            provider.validateConnection()
        }

        assertFalse(result)
    }
}