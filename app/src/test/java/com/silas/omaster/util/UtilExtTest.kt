package com.silas.omaster.util

import com.silas.omaster.data.lut.LUT3DData
import org.junit.Assert.*
import org.junit.Test

/**
 * Util 扩展测试 - 补充覆盖更多工具类的边界场景与纯计算逻辑
 *
 * 注意：本文件仅测试不依赖 Android 运行时/Keystore/SharedPreferences 的逻辑，
 * 依赖 Android 上下文的逻辑请使用 Android 仪器测试或 Robolectric 覆盖。
 */
class UtilExtTest {

    // ===== FormatUtils 边界测试 =====

    @Test
    fun `formatSigned - 大整数应正确格式化`() {
        assertEquals("+99999", 99999.formatSigned())
        assertEquals("-99999", (-99999).formatSigned())
    }

    @Test
    fun `formatPercent - 边界值应正确格式化`() {
        assertEquals("0%", 0.0f.formatPercent())
        assertEquals("100%", 1.0f.formatPercent())
        assertEquals("50%", 0.5f.formatPercent())
        assertEquals("33%", (1.0f / 3.0f).formatPercent())
    }

    @Test
    fun `formatFilterWithIntensity - 标准滤镜无论强度都返回原名称`() {
        assertEquals("标准", formatFilterWithIntensity("标准", 0))
        assertEquals("标准", formatFilterWithIntensity("标准", 100))
        assertEquals("标准", formatFilterWithIntensity("标准", 50))
    }

    @Test
    fun `formatFilterWithIntensity - 非标准滤镜应附带强度`() {
        assertEquals("胶片 100%", formatFilterWithIntensity("胶片", 100))
        assertEquals("黑白 0%", formatFilterWithIntensity("黑白", 0))
    }

    // ===== VersionInfo 预发布版本测试 =====

    @Test
    fun `parseVersionCode - 预发布版本应解析为负数`() {
        val beta = VersionInfo.parseVersionCode("1.0.0-beta1")
        val release = VersionInfo.parseVersionCode("1.0.0")
        assertTrue("正式版应大于预发布版", release > beta)
        assertTrue("预发布版应为负数", beta < 0)
    }

    @Test
    fun `parseVersionCode - 不同预发布阶段应正确排序`() {
        val alpha = VersionInfo.parseVersionCode("1.0.0-alpha1")
        val beta = VersionInfo.parseVersionCode("1.0.0-beta1")
        val rc = VersionInfo.parseVersionCode("1.0.0-rc1")
        assertTrue("alpha < beta", alpha < beta)
        assertTrue("beta < rc", beta < rc)
    }

    @Test
    fun `parseVersionCode - 应处理build元数据`() {
        val result = VersionInfo.parseVersionCode("1.2.3+build123")
        assertEquals(10203, result)
    }

    @Test
    fun `parseVersionCode - 应处理带v前缀的版本号`() {
        assertEquals(20100, VersionInfo.parseVersionCode("v2.1.0"))
    }

    // ===== LUT3DData 纯数学测试 =====

    @Test
    fun `LUT3DData - 2尺寸LUT应正确采样角点`() {
        // 2x2x2 LUT: 0,0,0 -> (0,0,0); 1,1,1 -> (1,1,1)
        val data = FloatArray(2 * 2 * 2 * 3) { i ->
            when (i % 3) {
                0 -> ((i / 3) and 1).toFloat()
                1 -> (((i / 3) shr 1) and 1).toFloat()
                else -> (((i / 3) shr 2) and 1).toFloat()
            }
        }
        val lut = LUT3DData("test", 2, data)

        val black = lut.get(0, 0, 0)
        assertEquals(0.0f, black[0], 0.0001f)
        assertEquals(0.0f, black[1], 0.0001f)
        assertEquals(0.0f, black[2], 0.0001f)

        val white = lut.get(1, 1, 1)
        assertEquals(1.0f, white[0], 0.0001f)
        assertEquals(1.0f, white[1], 0.0001f)
        assertEquals(1.0f, white[2], 0.0001f)
    }

    @Test
    fun `LUT3DData - 三线性插值在角点应返回角点值`() {
        val data = FloatArray(2 * 2 * 2 * 3) { i ->
            when (i % 3) {
                0 -> ((i / 3) and 1).toFloat()
                1 -> (((i / 3) shr 1) and 1).toFloat()
                else -> (((i / 3) shr 2) and 1).toFloat()
            }
        }
        val lut = LUT3DData("test", 2, data)

        val black = lut.sampleTrilinear(0f, 0f, 0f)
        assertEquals(0.0f, black[0], 0.001f)
        assertEquals(0.0f, black[1], 0.001f)
        assertEquals(0.0f, black[2], 0.001f)

        val white = lut.sampleTrilinear(1f, 1f, 1f)
        assertEquals(1.0f, white[0], 0.001f)
        assertEquals(1.0f, white[1], 0.001f)
        assertEquals(1.0f, white[2], 0.001f)
    }

    @Test
    fun `LUT3DData - 三线性插值在边界外应被钳制`() {
        val data = FloatArray(2 * 2 * 2 * 3) { 0.5f }
        val lut = LUT3DData("test", 2, data)

        val result = lut.sampleTrilinear(-0.5f, 1.5f, 0.5f)
        assertEquals(0.5f, result[0], 0.001f)
        assertEquals(0.5f, result[1], 0.001f)
        assertEquals(0.5f, result[2], 0.001f)
    }

    @Test
    fun `LUT3DData - 相等性应比较内容`() {
        val data1 = FloatArray(2 * 2 * 2 * 3) { 0.5f }
        val data2 = data1.copyOf()
        val lut1 = LUT3DData("same", 2, data1)
        val lut2 = LUT3DData("same", 2, data2)
        assertEquals(lut1, lut2)
        assertEquals(lut1.hashCode(), lut2.hashCode())
    }

    // ===== UrlConstants 边界测试 =====

    @Test
    fun `UrlConstants - LUT下载URL不应包含路径多余斜杠`() {
        val url = UrlConstants.getLUTDownloadUrl("film", "classic.cube")
        // LUT_BASE_PATH 本身以 https:// 开头，路径部分（去掉 scheme 后）不应有连续斜杠
        val pathPart = url.removePrefix("https://")
        assertFalse("路径部分不应包含多余斜杠: $url", pathPart.contains("//"))
        assertTrue("URL必须以https://开头", url.startsWith("https://"))
    }

    @Test
    fun `UrlConstants - 所有预设URL必须在已知域名下`() {
        val urls = UrlConstants.PRESET_SOURCE_URLS.values
        for (url in urls) {
            assertTrue("$url 应使用 jsDelivr CDN", url.contains("cdn.jsdelivr.net"))
        }
    }

    // ===== PresetI18n 边界测试 =====

    @Test
    fun `PresetI18n - 大小写不敏感的模式查找`() {
        assertNotNull(PresetI18n.getModeResId("AUTO"))
        assertNotNull(PresetI18n.getModeResId("auto"))
        assertNotNull(PresetI18n.getModeResId("Auto"))
    }

    @Test
    fun `PresetI18n - 空字符串应返回null`() {
        assertNull(PresetI18n.getFilterResId(""))
        assertNull(PresetI18n.getPresetNameResId(""))
    }
}
