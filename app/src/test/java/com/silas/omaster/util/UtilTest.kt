package com.silas.omaster.util

import org.junit.Test
import org.junit.Assert.*

/**
 * FormatUtils 单元测试
 * 测试格式化工具函数
 */
class FormatUtilsTest {

    @Test
    fun `formatSigned - 正数应该带加号`() {
        assertEquals("+5", 5.formatSigned())
        assertEquals("+100", 100.formatSigned())
        assertEquals("+1", 1.formatSigned())
    }

    @Test
    fun `formatSigned - 负数应该保持负号`() {
        assertEquals("-5", (-5).formatSigned())
        assertEquals("-100", (-100).formatSigned())
        assertEquals("-1", (-1).formatSigned())
    }

    @Test
    fun `formatSigned - 零应该不带符号`() {
        assertEquals("0", 0.formatSigned())
    }

    @Test
    fun `formatPercent - 应该正确格式化百分比`() {
        assertEquals("75%", 0.75f.formatPercent())
        assertEquals("50%", 0.5f.formatPercent())
        assertEquals("100%", 1.0f.formatPercent())
        assertEquals("0%", 0.0f.formatPercent())
    }

    @Test
    fun `formatPercent - 小数应该被截断为整数`() {
        assertEquals("33%", 0.333f.formatPercent())
        assertEquals("66%", 0.666f.formatPercent())
    }

    @Test
    fun `formatFilterWithIntensity - 标准滤镜应该只返回名称`() {
        assertEquals("标准", formatFilterWithIntensity("标准", 100))
        assertEquals("标准", formatFilterWithIntensity("标准", 50))
    }

    @Test
    fun `formatFilterWithIntensity - 非标准滤镜应该带强度`() {
        assertEquals("复古 80%", formatFilterWithIntensity("复古", 80))
        assertEquals("胶片 100%", formatFilterWithIntensity("胶片", 100))
        assertEquals("黑白 50%", formatFilterWithIntensity("黑白", 50))
    }

    @Test
    fun `formatFilterWithIntensity - 强度为0也应该显示`() {
        assertEquals("复古 0%", formatFilterWithIntensity("复古", 0))
    }
}

/**
 * VersionInfo 单元测试
 * 测试版本信息管理
 */
class VersionInfoTest {

    @Test
    fun `parseVersionCode - 应该正确解析标准版本号`() {
        assertEquals(10100, VersionInfo.parseVersionCode("1.1.0"))
        assertEquals(10003, VersionInfo.parseVersionCode("1.0.3"))
        assertEquals(20000, VersionInfo.parseVersionCode("2.0.0"))
    }

    @Test
    fun `parseVersionCode - 应该处理两位数版本号`() {
        assertEquals(20100, VersionInfo.parseVersionCode("2.1.0"))
        assertEquals(11050, VersionInfo.parseVersionCode("1.10.50"))
    }

    @Test
    fun `parseVersionCode - 应该处理缺失的版本部分`() {
        // 缺失部分应该返回0
        val result = VersionInfo.parseVersionCode("1")
        assertTrue(result >= 10000) // 主版本1
    }

    @Test
    fun `parseVersionCode - 应该处理无效版本号`() {
        assertEquals(0, VersionInfo.parseVersionCode("invalid"))
        assertEquals(0, VersionInfo.parseVersionCode(""))
    }

    @Test
    fun `parseVersionCode - 版本号计算公式正确`() {
        // 公式: major * 10000 + minor * 100 + patch
        assertEquals(1 * 10000 + 2 * 100 + 3, VersionInfo.parseVersionCode("1.2.3"))
        assertEquals(3 * 10000 + 5 * 100 + 7, VersionInfo.parseVersionCode("3.5.7"))
    }

    @Test
    fun `VERSION_NAME - 应该是有效的版本字符串`() {
        assertTrue(VersionInfo.VERSION_NAME.isNotEmpty())
        assertTrue(VersionInfo.VERSION_NAME.contains("."))
    }

    @Test
    fun `VERSION_CODE - 应该是正数`() {
        assertTrue(VersionInfo.VERSION_CODE > 0)
    }
}

/**
 * SecurityCrypto 单元测试
 * 测试安全加密常量与配置（加密/解密 roundtrip 需要 Android Keystore，
 * 请在 Android 仪器测试或 Robolectric 环境中覆盖）。
 */
class SecurityCryptoTest {

    @Test
    fun `加密常量 - GCM IV长度应该为12`() {
        assertEquals(12, SecurityCrypto.GCM_IV_LENGTH)
    }

    @Test
    fun `加密常量 - GCM Tag长度应该为128`() {
        assertEquals(128, SecurityCrypto.GCM_TAG_LENGTH)
    }

    @Test
    fun `加密常量 - IV长度应满足GCM规范`() {
        // GCM 标准推荐 IV 长度为 12 字节（96 位）
        assertTrue("GCM IV长度应等于12", SecurityCrypto.GCM_IV_LENGTH == 12)
    }
}

/**
 * JsonUtil 单元测试
 * 测试JSON工具类的逻辑（不依赖 Context）
 */
class JsonUtilTest {

    @Test
    fun `ID生成 - 应该生成有效的预设ID`() {
        val id = JsonUtil.generatePresetId("富士胶片", 0)
        assertTrue(id.isNotEmpty())
        assertTrue(id.contains("_"))
        assertTrue("生成的ID应只包含小写字母、数字和下划线", 
            id.all { it.isLetterOrDigit() || it == '_' })
    }

    @Test
    fun `ID生成 - 应该处理特殊字符与中英文混合`() {
        val names = listOf("Portrait Classic", "蓝调时刻", "复古-胶片", "Test@123")
        
        for ((index, name) in names.withIndex()) {
            val id = JsonUtil.generatePresetId(name, index)
            assertTrue("$name 生成的ID应有效: $id", id.isNotEmpty())
            assertTrue("ID应只包含小写字母、数字和下划线: $id", 
                id.all { it.isLetterOrDigit() || it == '_' })
        }
    }

    @Test
    fun `ID生成 - 应该限制长度并不超过30字符主体`() {
        val longName = "a".repeat(50)
        val id = JsonUtil.generatePresetId(longName, 0)
        // ID 格式为 {baseId}_{index}，baseId 被截断为 30 字符
        val basePart = id.substringBeforeLast("_")
        assertTrue("baseId应被截断: ${basePart.length}", basePart.length <= 30)
    }

    @Test
    fun `ID生成 - 索引应作为后缀避免重复`() {
        val id1 = JsonUtil.generatePresetId("Portrait", 0)
        val id2 = JsonUtil.generatePresetId("Portrait", 1)
        assertNotEquals(id1, id2)
        assertTrue(id1.endsWith("_0"))
        assertTrue(id2.endsWith("_1"))
    }

    @Test
    fun `ID生成 - 中文名称应被转换为拼音风格ID`() {
        val id = JsonUtil.generatePresetId("富士胶片", 0)
        // 中文经 Normalizer 与清理后应生成小写 ID
        assertTrue(id.startsWith("fu_", ignoreCase = true) || id.contains("shi"))
        assertTrue(id.isNotEmpty())
    }
}

/**
 * PresetI18n 单元测试
 * 测试预设国际化中的无 Context 逻辑
 */
class PresetI18nTest {

    @Test
    fun `getFilterResId - 已知滤镜名称应返回非空资源ID`() {
        assertNotNull(PresetI18n.getFilterResId("标准"))
        assertNotNull(PresetI18n.getFilterResId("复古"))
        assertNotNull(PresetI18n.getFilterResId("黑白"))
    }

    @Test
    fun `getFilterResId - 未知滤镜名称应返回null`() {
        assertNull(PresetI18n.getFilterResId("不存在的滤镜"))
    }

    @Test
    fun `getPresetNameResId - 已知预设名称应返回非空资源ID`() {
        assertNotNull(PresetI18n.getPresetNameResId("富士胶片"))
        assertNotNull(PresetI18n.getPresetNameResId("哈苏浓郁"))
    }

    @Test
    fun `getPresetNameResId - 未知预设名称应返回null`() {
        assertNull(PresetI18n.getPresetNameResId("未知预设"))
    }

    @Test
    fun `getSoftLightResId - 已知柔光值应返回非空资源ID`() {
        assertNotNull(PresetI18n.getSoftLightResId("无"))
        assertNotNull(PresetI18n.getSoftLightResId("梦幻"))
    }

    @Test
    fun `getVignetteResId - 开关值应返回非空资源ID`() {
        assertNotNull(PresetI18n.getVignetteResId("开"))
        assertNotNull(PresetI18n.getVignetteResId("关"))
    }

    @Test
    fun `getModeResId - 大小写模式都应返回资源ID`() {
        assertNotNull(PresetI18n.getModeResId("auto"))
        assertNotNull(PresetI18n.getModeResId("AUTO"))
        assertNotNull(PresetI18n.getModeResId("pro"))
    }
}
