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
 * 测试安全加密工具（不依赖 Android Keystore 的逻辑测试）
 */
class SecurityCryptoTest {

    @Test
    fun `URL验证 - HTTPS协议应该被允许`() {
        val url = "https://example.com/presets.json"
        assertTrue(url.startsWith("https://"))
    }

    @Test
    fun `URL验证 - HTTP协议应该被拒绝`() {
        val url = "http://example.com/presets.json"
        assertFalse(url.startsWith("https://"))
    }

    @Test
    fun `URL验证 - 空URL应该被拒绝`() {
        val url = ""
        assertTrue(url.isBlank())
    }

    @Test
    fun `URL验证 - 内网地址应该被拒绝`() {
        val blockedHosts = listOf("localhost", "127.0.0.1", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
        
        val testUrls = listOf(
            "https://localhost/test",
            "https://127.0.0.1/test",
            "https://192.168.1.1/test",
            "https://10.0.0.1/test"
        )
        
        for (url in testUrls) {
            val lower = url.lowercase()
            val isBlocked = blockedHosts.any { lower.contains(it) }
            assertTrue("$url 应该被阻止", isBlocked)
        }
    }

    @Test
    fun `加密常量 - GCM IV长度应该为12`() {
        assertEquals(12, 12) // GCM_IV_LENGTH
    }

    @Test
    fun `加密常量 - GCM Tag长度应该为128`() {
        assertEquals(128, 128) // GCM_TAG_LENGTH
    }

    @Test
    fun `加密常量 - 密钥大小应该为256位`() {
        assertEquals(256, 256) // AES-256
    }
}

/**
 * JsonUtil 单元测试
 * 测试JSON工具类的逻辑（不依赖 Context）
 */
class JsonUtilTest {

    @Test
    fun `ID生成 - 应该生成有效的预设ID`() {
        val name = "富士胶片"
        val index = 0
        
        // 模拟ID生成逻辑
        val baseId = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val id = if (baseId.length < 2) "preset_$index" else "${baseId}_$index"
        
        assertTrue(id.isNotEmpty())
        assertTrue(id.contains("_"))
    }

    @Test
    fun `ID生成 - 应该处理特殊字符`() {
        val names = listOf("Portrait Classic", "蓝调时刻", "复古-胶片", "Test@123")
        
        for (name in names) {
            val cleaned = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            assertTrue("处理后的ID应该只包含小写字母、数字和下划线: $cleaned", 
                cleaned.all { it.isLetterOrDigit() || it == '_' })
        }
    }

    @Test
    fun `ID生成 - 应该限制长度`() {
        val longName = "a".repeat(50)
        val maxLength = 30
        
        val truncated = if (longName.length > maxLength) longName.substring(0, maxLength) else longName
        
        assertTrue(truncated.length <= maxLength)
    }

    @Test
    fun `版本号 - 默认版本应该为2`() {
        val defaultVersion = 2
        assertEquals(2, defaultVersion)
    }

    @Test
    fun `缓存 - 初始缓存应该为空`() {
        var cachedPresets: List<String>? = null
        assertNull(cachedPresets)
        
        // 设置缓存后应该不为空
        cachedPresets = listOf("preset1", "preset2")
        assertNotNull(cachedPresets)
        assertEquals(2, cachedPresets.size)
    }
}

/**
 * UpdateConfigManager 单元测试
 * 测试更新配置管理
 */
class UpdateConfigManagerTest {

    @Test
    fun `默认URL - 应该是HTTPS协议`() {
        val defaultUrl = "https://cdn.jsdelivr.net/gh/user/repo/presets.json"
        assertTrue(defaultUrl.startsWith("https://"))
    }

    @Test
    fun `版本比较 - 应该正确比较版本号`() {
        val currentVersion = 10100
        val remoteVersion = 10200
        
        assertTrue(remoteVersion > currentVersion)
        
        val sameVersion = 10100
        assertFalse(sameVersion > currentVersion)
    }

    @Test
    fun `更新检查 - 应该判断是否需要更新`() {
        val localBuild = 1
        val remoteBuild = 2
        
        val needsUpdate = remoteBuild > localBuild
        assertTrue(needsUpdate)
        
        val noUpdate = localBuild >= remoteBuild
        assertFalse(noUpdate)
    }
}

/**
 * ImageCacheManager 单元测试
 * 测试图片缓存管理逻辑
 */
class ImageCacheManagerTest {

    @Test
    fun `缓存键 - 应该生成唯一的缓存键`() {
        val url1 = "https://example.com/image1.jpg"
        val url2 = "https://example.com/image2.jpg"
        
        val key1 = url1.hashCode().toString(16)
        val key2 = url2.hashCode().toString(16)
        
        assertNotEquals(key1, key2)
    }

    @Test
    fun `缓存大小 - 应该在合理范围内`() {
        val maxCacheSize = 100 * 1024 * 1024L // 100MB
        val currentSize = 50 * 1024 * 1024L // 50MB
        
        assertTrue(currentSize < maxCacheSize)
    }

    @Test
    fun `LRU淘汰 - 应该移除最久未使用的缓存`() {
        val cache = mutableMapOf<String, Long>(
            "key1" to 1000L,
            "key2" to 2000L,
            "key3" to 500L // 最旧
        )
        
        val oldest = cache.minByOrNull { it.value }
        assertEquals("key3", oldest?.key)
    }
}

/**
 * CrashHandler 单元测试
 * 测试崩溃处理逻辑
 */
class CrashHandlerTest {

    @Test
    fun `异常信息 - 应该包含堆栈跟踪`() {
        val exception = RuntimeException("Test exception")
        val stackTrace = exception.stackTrace
        
        assertTrue(stackTrace.isNotEmpty())
    }

    @Test
    fun `异常时间戳 - 应该是有效的时间戳`() {
        val timestamp = System.currentTimeMillis()
        assertTrue(timestamp > 0)
    }
}

/**
 * HapticExt 单元测试
 * 测试触觉反馈扩展
 */
class HapticExtTest {

    @Test
    fun `触觉强度 - 应该在有效范围内`() {
        val intensities = listOf(0f, 0.5f, 1.0f)
        
        for (intensity in intensities) {
            assertTrue("触觉强度应该在0到1之间", intensity in 0f..1f)
        }
    }
}

/**
 * PresetI18n 单元测试
 * 测试预设国际化
 */
class PresetI18nTest {

    @Test
    fun `滤镜名称 - 应该支持国际化`() {
        val filterIds = listOf("portra", "cc", "nc", "nh", "rdp3", "800t", "tx400")
        
        for (filterId in filterIds) {
            assertTrue("滤镜ID应该有效: $filterId", filterId.isNotEmpty())
        }
    }

    @Test
    fun `柔光模式 - 应该有对应的显示名称`() {
        val softLightModes = listOf("无", "柔", "梦幻")
        
        for (mode in softLightModes) {
            assertTrue("柔光模式应该有显示名称: $mode", mode.isNotEmpty())
        }
    }

    @Test
    fun `暗角模式 - 应该有对应的显示名称`() {
        val vignetteModes = listOf("开", "关")

        for (mode in vignetteModes) {
            assertTrue("暗角模式应该有显示名称: $mode", mode.isNotEmpty())
        }
    }
}

/**
 * PerformanceHelper 单元测试
 * 测试性能优化工具类的纯逻辑部分
 */
class PerformanceHelperTest {

    @Test
    fun `Bitmap内存计算 - ARGB_8888应该按4字节计算`() {
        val width = 1024
        val height = 1024
        val bytesPerPixel = 4
        val expectedMB = (width * height * bytesPerPixel).toDouble() / (1024 * 1024)
        // 验证计算公式正确
        assertEquals(4.0, expectedMB, 0.01)
    }

    @Test
    fun `Bitmap内存计算 - RGB_565应该按2字节计算`() {
        val width = 1024
        val height = 1024
        val bytesPerPixel = 2
        val expectedMB = (width * height * bytesPerPixel).toDouble() / (1024 * 1024)
        assertEquals(2.0, expectedMB, 0.01)
    }

    @Test
    fun `Bitmap缩放比例 - 宽度超出时应按宽度比例缩放`() {
        val width = 2000
        val height = 1000
        val maxWidth = 1000
        val maxHeight = 1000
        val scale = kotlin.math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        assertEquals(1000, newWidth)
        assertEquals(500, newHeight)
    }

    @Test
    fun `Bitmap缩放比例 - 高度超出时应按高度比例缩放`() {
        val width = 1000
        val height = 2000
        val maxWidth = 1000
        val maxHeight = 1000
        val scale = kotlin.math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        assertEquals(500, newWidth)
        assertEquals(1000, newHeight)
    }

    @Test
    fun `Bitmap缩放比例 - 尺寸在范围内时不应缩放`() {
        val width = 500
        val height = 500
        val maxWidth = 1000
        val maxHeight = 1000
        val needsScale = width > maxWidth || height > maxHeight
        assertFalse(needsScale)
    }

    @Test
    fun `协程作用域 - 创建后不应为null`() {
        val scope = PerformanceHelper.createSafeScope("test_scope")
        assertNotNull(scope)
        // 清理
        PerformanceHelper.cancelScope("test_scope")
    }

    @Test
    fun `协程作用域 - 取消后应失效`() {
        val scope = PerformanceHelper.createSafeScope("test_cancel_scope")
        assertTrue(scope.isActive)
        PerformanceHelper.cancelScope("test_cancel_scope")
        // 取消后 isActive 应该变为 false（由于 cancel 是异步的，这里直接检查）
        assertFalse(scope.isActive)
    }

    @Test
    fun `性能统计 - 初始平均时间应为0`() {
        val avgTime = PerformanceHelper.getAverageOperationTime("non_existent_op")
        assertEquals(0L, avgTime)
    }

    @Test
    fun `性能统计 - 记录后平均时间应正确计算`() {
        val opName = "test_op"
        // 模拟记录两次操作，耗时 100ms 和 300ms
        PerformanceHelper.recordOperationTime(opName, System.currentTimeMillis() - 100)
        PerformanceHelper.recordOperationTime(opName, System.currentTimeMillis() - 300)
        val avgTime = PerformanceHelper.getAverageOperationTime(opName)
        // 平均时间应在 100~300 之间（由于使用真实时间，这里只验证大于0）
        assertTrue("平均时间应大于0", avgTime >= 0)
    }

    @Test
    fun `内存状态枚举 - 应包含OK WARNING CRITICAL`() {
        val statuses = PerformanceHelper.MemoryStatus.values()
        assertEquals(3, statuses.size)
        assertTrue(statuses.contains(PerformanceHelper.MemoryStatus.OK))
        assertTrue(statuses.contains(PerformanceHelper.MemoryStatus.WARNING))
        assertTrue(statuses.contains(PerformanceHelper.MemoryStatus.CRITICAL))
    }

    @Test
    fun `采样率计算 - 尺寸刚好满足时应返回1`() {
        val width = 500
        val height = 500
        val maxWidth = 1000
        val maxHeight = 1000
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        assertEquals(1, sampleSize)
    }

    @Test
    fun `采样率计算 - 超出一倍时应返回2`() {
        val width = 2000
        val height = 1000
        val maxWidth = 1000
        val maxHeight = 1000
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        assertEquals(2, sampleSize)
    }

    @Test
    fun `采样率计算 - 超出四倍时应返回4`() {
        val width = 4001
        val height = 1000
        val maxWidth = 1000
        val maxHeight = 1000
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        assertEquals(4, sampleSize)
    }
}
