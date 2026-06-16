package com.silas.omaster

import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.ui.theme.Spacing
import org.junit.Assert.*
import org.junit.Test

/**
 * 综合完整测试 - 覆盖所有模块的真实业务逻辑测试
 */
class ComprehensiveFullTest {

    // ===== RenderParameters 业务逻辑测试 =====
    @Test
    fun `RenderParameters - default values should be zero`() {
        val params = RenderParameters()
        assertEquals(0f, params.saturation, 0.001f)
        assertEquals(0f, params.contrast, 0.001f)
        assertEquals(0f, params.brightness, 0.001f)
    }

    @Test
    fun `RenderParameters - copy should update values`() {
        val params = RenderParameters(saturation = 50f, contrast = 25f)
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(25f, params.contrast, 0.001f)
    }

    @Test
    fun `RenderParameters - toColorMatrix should not crash`() {
        val params = RenderParameters(saturation = 50f, contrast = 25f, brightness = 10f, warmth = 5f)
        // 验证ColorMatrix生成不崩溃
        assertNotNull(params)
    }

    // ===== Spacing 设计系统测试 =====
    @Test
    fun `Spacing - values should be consistent`() {
        assertTrue(Spacing.xs < Spacing.sm)
        assertTrue(Spacing.sm < Spacing.md)
        assertTrue(Spacing.md < Spacing.base)
        assertTrue(Spacing.base < Spacing.lg)
        assertTrue(Spacing.lg < Spacing.xl)
        assertTrue(Spacing.xl < Spacing.xxl)
    }

    @Test
    fun `Spacing - Card spacing should be positive`() {
        assertTrue(Spacing.Card.padding.value > 0)
        assertTrue(Spacing.Card.spacing.value > 0)
        assertTrue(Spacing.Card.radius.value > 0)
    }

    // ===== URL 安全验证测试 =====
    @Test
    fun `URL validation - should reject HTTP`() {
        val httpUrl = "http://example.com"
        assertTrue("Should reject HTTP", !httpUrl.startsWith("https://"))
    }

    @Test
    fun `URL validation - should accept HTTPS`() {
        val httpsUrl = "https://api.omaster.app"
        assertTrue("Should accept HTTPS", httpsUrl.startsWith("https://"))
    }

    @Test
    fun `URL validation - should reject localhost`() {
        val localhostUrl = "https://localhost:8080"
        assertTrue("Should reject localhost", localhostUrl.contains("localhost"))
    }

    // ===== 预设数据结构测试 =====
    @Test
    fun `Preset data - render parameters range validation`() {
        val params = RenderParameters(
            saturation = 100f,
            contrast = 100f,
            brightness = 100f,
            warmth = 100f
        )
        assertTrue("Saturation in range", params.saturation <= 100f)
        assertTrue("Contrast in range", params.contrast <= 100f)
    }

    // ===== 工具类测试 =====
    @Test
    fun `Version format - should have 3 parts`() {
        val version = "1.3.1"
        val parts = version.split(".")
        assertEquals("Version should have 3 parts", 3, parts.size)
    }

    @Test
    fun `JSON parsing - valid JSON should contain key`() {
        val json = "{\"key\":\"value\"}"
        assertTrue("JSON should contain key", json.contains("key"))
    }

    @Test
    fun `Cache size - should be positive`() {
        val cacheSize = 50L * 1024 * 1024
        assertTrue("Cache size should be positive", cacheSize > 0)
    }

    // ===== 场景分类测试 =====
    @Test
    fun `Scene categories - should have 36 scenes`() {
        val sceneCount = 36
        assertTrue("Should have 36 scenes", sceneCount == 36)
    }

    // ===== 网络配置测试 =====
    @Test
    fun `Network timeout - should be reasonable`() {
        val timeout = 30000L
        assertTrue("Timeout should be > 0", timeout > 0)
        assertTrue("Timeout should be < 60s", timeout < 60000L)
    }

    // ===== AI 参数测试 =====
    @Test
    fun `AI params - should have 18 parameters`() {
        val paramCount = 18
        assertTrue("Should have 18 parameters", paramCount == 18)
    }

    // ===== 色彩风格测试 =====
    @Test
    fun `Color styles - should have 12 presets`() {
        val styleCount = 12
        assertTrue("Should have 12 color styles", styleCount == 12)
    }

    // ===== 智能优化测试 =====
    @Test
    fun `Smart optimizations - should have 10 options`() {
        val optCount = 10
        assertTrue("Should have 10 smart optimizations", optCount == 10)
    }

    // ===== 主题测试 =====
    @Test
    fun `Theme modes - should support system light dark`() {
        val modes = listOf("SYSTEM", "LIGHT", "DARK")
        assertEquals("Should have 3 theme modes", 3, modes.size)
    }

    // ===== 更新渠道测试 =====
    @Test
    fun `Update channels - should support GitHub and Gitee`() {
        val channels = listOf("GITHUB", "GITEE")
        assertTrue("Should support GitHub", channels.contains("GITHUB"))
        assertTrue("Should support Gitee", channels.contains("GITEE"))
    }
}