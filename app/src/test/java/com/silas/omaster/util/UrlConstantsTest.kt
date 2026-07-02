package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * UrlConstants 单元测试
 * 测试所有 URL 常量的安全性和正确性
 */
class UrlConstantsTest {

    // ===== 预设 URL HTTPS 验证 =====

    @Test
    fun `所有预设URL应该以HTTPS开头`() {
        val presetUrls = listOf(
            UrlConstants.PRESET_OPPO,
            UrlConstants.PRESET_REALME,
            UrlConstants.PRESET_VIVO,
            UrlConstants.PRESET_HONOR
        )
        for (url in presetUrls) {
            assertTrue("预设URL必须以https://开头: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `所有API端点应该以HTTPS开头`() {
        val apiUrls = listOf(
            UrlConstants.API_AI_ENDPOINT,
            UrlConstants.API_PRESET_ENDPOINT,
            UrlConstants.API_AUTH_ENDPOINT,
            UrlConstants.API_CLOUD_SCENE_ANALYZE
        )
        for (url in apiUrls) {
            assertTrue("API端点必须以https://开头: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `CDN基础URL应该以HTTPS开头`() {
        assertTrue("CDN JSDELIVR必须以https://开头", UrlConstants.CDN_JSDELIVR.startsWith("https://"))
        assertTrue("CDN MODELS必须以https://开头", UrlConstants.CDN_MODELS.startsWith("https://"))
    }

    @Test
    fun `更新相关URL应该以HTTPS开头`() {
        assertTrue("GitHub API URL必须以https://开头", UrlConstants.GITHUB_API_RELEASES.startsWith("https://"))
        assertTrue("Gitee API URL必须以https://开头", UrlConstants.GITEE_API_RELEASES.startsWith("https://"))
    }

    @Test
    fun `隐私政策URL应该以HTTPS开头`() {
        assertTrue("隐私政策URL必须以https://开头", UrlConstants.PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue("友盟隐私URL必须以https://开头", UrlConstants.UMENG_PRIVACY_URL.startsWith("https://"))
    }

    // ===== PRESET_SOURCE_URLS 验证 =====

    @Test
    fun `PRESET_SOURCE_URLS应该包含4个品牌`() {
        assertEquals(4, UrlConstants.PRESET_SOURCE_URLS.size)
    }

    @Test
    fun `PRESET_SOURCE_URLS应该包含所有品牌键`() {
        val expectedKeys = setOf("oppo", "realme", "vivo", "honor")
        assertEquals(expectedKeys, UrlConstants.PRESET_SOURCE_URLS.keys)
    }

    @Test
    fun `PRESET_SOURCE_URLS中每个品牌URL应该以HTTPS开头`() {
        for ((brand, url) in UrlConstants.PRESET_SOURCE_URLS) {
            assertTrue("品牌 $brand 的URL必须以https://开头: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `PRESET_SOURCE_URLS映射值应该与对应常量一致`() {
        assertEquals(UrlConstants.PRESET_OPPO, UrlConstants.PRESET_SOURCE_URLS["oppo"])
        assertEquals(UrlConstants.PRESET_REALME, UrlConstants.PRESET_SOURCE_URLS["realme"])
        assertEquals(UrlConstants.PRESET_VIVO, UrlConstants.PRESET_SOURCE_URLS["vivo"])
        assertEquals(UrlConstants.PRESET_HONOR, UrlConstants.PRESET_SOURCE_URLS["honor"])
    }

    // ===== PRESET_SOURCE_INFO_LIST 验证 =====

    @Test
    fun `PRESET_SOURCE_INFO_LIST应该包含4个信息条目`() {
        assertEquals(4, UrlConstants.PRESET_SOURCE_INFO_LIST.size)
    }

    @Test
    fun `PRESET_SOURCE_INFO_LIST中每个信息的URL应该以HTTPS开头`() {
        for (info in UrlConstants.PRESET_SOURCE_INFO_LIST) {
            assertTrue("${info.brand}的URL必须以https://开头", info.url.startsWith("https://"))
        }
    }

    @Test
    fun `PRESET_SOURCE_INFO_LIST中品牌名不应为空`() {
        for (info in UrlConstants.PRESET_SOURCE_INFO_LIST) {
            assertTrue("品牌名不应为空: ${info.brand}", info.brand.isNotBlank())
            assertTrue("显示名不应为空: ${info.brand}", info.displayName.isNotBlank())
        }
    }

    // ===== getLUTDownloadUrl 验证 =====

    @Test
    fun `getLUTDownloadUrl应该返回正确的URL格式`() {
        val url = UrlConstants.getLUTDownloadUrl("film", "classic.cube")
        assertTrue("LUT下载URL必须以https://开头", url.startsWith("https://"))
        assertTrue("LUT下载URL应包含category", url.contains("film"))
        assertTrue("LUT下载URL应包含fileName", url.contains("classic.cube"))
        assertTrue("LUT下载URL应基于LUT_BASE_PATH", url.startsWith(UrlConstants.LUT_BASE_PATH))
    }

    @Test
    fun `getLUTDownloadUrl应该正确处理不同分类`() {
        val categories = listOf("film", "vintage", "portrait", "landscape")
        for (category in categories) {
            val url = UrlConstants.getLUTDownloadUrl(category, "test.cube")
            assertTrue("URL应包含category $category: $url", url.contains("/$category/"))
        }
    }

    @Test
    fun `getLUTDownloadUrl应该正确处理不同文件名`() {
        val fileNames = listOf("portra.cube", "classic.png", "vintage.lut")
        for (fileName in fileNames) {
            val url = UrlConstants.getLUTDownloadUrl("film", fileName)
            assertTrue("URL应以文件名结尾: $url", url.endsWith(fileName))
        }
    }

    // ===== getSampleImageUrl 验证 =====

    @Test
    fun `getSampleImageUrl应该返回正确的URL格式`() {
        val url = UrlConstants.getSampleImageUrl("sample1.jpg")
        assertTrue("示例图片URL必须以https://开头", url.startsWith("https://"))
        assertTrue("示例图片URL应包含文件名", url.contains("sample1.jpg"))
        assertTrue("示例图片URL应基于SAMPLES_BASE_PATH", url.startsWith(UrlConstants.SAMPLES_BASE_PATH))
    }

    @Test
    fun `getSampleImageUrl应该正确处理不同文件名`() {
        val fileNames = listOf("portrait.jpg", "landscape.png", "macro.webp")
        for (fileName in fileNames) {
            val url = UrlConstants.getSampleImageUrl(fileName)
            assertTrue("URL应以文件名结尾: $url", url.endsWith(fileName))
        }
    }

    // ===== LUT_BASE_PATH 和 SAMPLES_BASE_PATH 验证 =====

    @Test
    fun `LUT_BASE_PATH应该以HTTPS开头`() {
        assertTrue("LUT_BASE_PATH必须以https://开头", UrlConstants.LUT_BASE_PATH.startsWith("https://"))
    }

    @Test
    fun `SAMPLES_BASE_PATH应该以HTTPS开头`() {
        assertTrue("SAMPLES_BASE_PATH必须以https://开头", UrlConstants.SAMPLES_BASE_PATH.startsWith("https://"))
    }
}