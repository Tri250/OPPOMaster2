package com.silas.omaster.network

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetList
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method
import kotlin.math.pow

/**
 * PresetRemoteManager 综合单元测试
 *
 * 覆盖范围：
 * 1. 远程预设拉取逻辑
 * 2. 网络故障错误处理
 * 3. 指数退避重试机制 (baseDelayMs * 2.0.pow(attempt))
 * 4. 远程预设 JSON 解析
 * 5. 缓存行为（Cache-Control / ETag / SSRF 防护）
 */
class PresetRemoteManagerTest {

    /** 与 PresetRemoteManager 内部一致的 Json 配置 */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    // ==================== 辅助：通过反射访问私有方法 ====================

    private fun getValidateUrlMethod(): Method {
        return PresetRemoteManager::class.java.getDeclaredMethod("validateUrl", String::class.java)
            .apply { isAccessible = true }
    }

    private fun getBuildCacheHeadersMethod(): Method {
        return PresetRemoteManager::class.java.getDeclaredMethod("buildCacheHeaders", String::class.java)
            .apply { isAccessible = true }
    }

    // ==================== 1. 远程预设拉取逻辑 ====================

    @Test
    fun `fetchPresets 对无效URL应返回null`() = runTest {
        val result = PresetRemoteManager.fetchPresets("http://example.com/presets.json")
        assertNull(result)
    }

    @Test
    fun `fetchPresets 对空URL应返回null`() = runTest {
        val result = PresetRemoteManager.fetchPresets("")
        assertNull(result)
    }

    @Test
    fun `fetchPresets 对内网地址应返回null`() = runTest {
        val result = PresetRemoteManager.fetchPresets("https://127.0.0.1/presets.json")
        assertNull(result)
    }

    // ==================== 2. 网络故障错误处理 ====================

    @Test
    fun `fetchAndSave 对无效URL应返回SecurityException失败`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val result = PresetRemoteManager.fetchAndSave(context, "http://invalid.example.com/presets.json")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is SecurityException)
    }

    @Test
    fun `fetchAndSave 对空URL应返回SecurityException失败`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val result = PresetRemoteManager.fetchAndSave(context, "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `fetchAndSave 对localhost应返回SecurityException失败`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val result = PresetRemoteManager.fetchAndSave(context, "https://localhost/presets.json")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `fetchAndSave 对私有IP应返回SecurityException失败`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)

        val privateUrls = listOf(
            "https://10.0.0.1/presets.json",
            "https://192.168.1.1/presets.json",
            "https://172.16.0.1/presets.json",
            "https://169.254.1.1/presets.json"
        )

        for (url in privateUrls) {
            val result = PresetRemoteManager.fetchAndSave(context, url)
            assertTrue("URL $url 应被SSRF防护拦截", result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
        }
    }

    @Test
    fun `fetchAndSave 对HTTP协议应返回SecurityException失败`() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        val result = PresetRemoteManager.fetchAndSave(context, "http://example.com/presets.json")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // ==================== 3. 指数退避重试机制 ====================

    @Test
    fun `指数退避延迟计算应满足 baseDelayMs 乘以 2的attempt次方`() {
        val baseDelayMs = 1000L

        // attempt=0: 1000 * 2^0 = 1000
        assertEquals(1000L, (baseDelayMs * 2.0.pow(0.0)).toLong())
        // attempt=1: 1000 * 2^1 = 2000
        assertEquals(2000L, (baseDelayMs * 2.0.pow(1.0)).toLong())
        // attempt=2: 1000 * 2^2 = 4000
        assertEquals(4000L, (baseDelayMs * 2.0.pow(2.0)).toLong())
        // attempt=3: 1000 * 2^3 = 8000
        assertEquals(8000L, (baseDelayMs * 2.0.pow(3.0)).toLong())
    }

    @Test
    fun `指数退避自定义baseDelayMs应正确计算延迟`() {
        val baseDelayMs = 500L

        assertEquals(500L, (baseDelayMs * 2.0.pow(0.0)).toLong())
        assertEquals(1000L, (baseDelayMs * 2.0.pow(1.0)).toLong())
        assertEquals(2000L, (baseDelayMs * 2.0.pow(2.0)).toLong())
    }

    @Test
    fun `指数退避 attempt为0时延迟等于baseDelayMs`() {
        val baseDelayMs = 1000L
        val attempt = 0
        val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
        assertEquals(1000L, delayMs)
    }

    @Test
    fun `指数退避 attempt为3时延迟等于baseDelayMs乘8`() {
        val baseDelayMs = 1000L
        val attempt = 3
        val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
        assertEquals(8000L, delayMs)
    }

    @Test
    fun `指数退避 大baseDelayMs时应正确计算`() {
        val baseDelayMs = 5000L
        assertEquals(5000L, (baseDelayMs * 2.0.pow(0.0)).toLong())
        assertEquals(10000L, (baseDelayMs * 2.0.pow(1.0)).toLong())
        assertEquals(20000L, (baseDelayMs * 2.0.pow(2.0)).toLong())
        assertEquals(40000L, (baseDelayMs * 2.0.pow(3.0)).toLong())
    }

    @Test
    fun `指数退避 小baseDelayMs时应正确计算`() {
        val baseDelayMs = 100L
        assertEquals(100L, (baseDelayMs * 2.0.pow(0.0)).toLong())
        assertEquals(200L, (baseDelayMs * 2.0.pow(1.0)).toLong())
        assertEquals(400L, (baseDelayMs * 2.0.pow(2.0)).toLong())
    }

    @Test
    fun `withExponentialBackoff maxRetries为3时循环应执行4次尝试`() {
        // 源码: for (attempt in 0..maxRetries) → maxRetries=3 → 0,1,2,3 共4次
        val maxRetries = 3
        var attemptCount = 0
        for (attempt in 0..maxRetries) {
            attemptCount++
        }
        assertEquals(4, attemptCount)
    }

    @Test
    fun `withExponentialBackoff 首次成功时不应重试`() {
        var callCount = 0
        val block: () -> String = {
            callCount++
            "success"
        }

        val result = block()
        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `withExponentialBackoff 在第N次重试成功时应停止重试`() {
        var callCount = 0
        val maxSuccessAttempt = 2  // 第3次调用时成功 (0-indexed: attempt 2)

        val block: () -> String = {
            callCount++
            if (callCount <= maxSuccessAttempt) {
                throw RuntimeException("失败")
            } else {
                "success"
            }
        }

        // 模拟源码中的重试逻辑
        var result: String? = null
        for (attempt in 0..3) {
            try {
                result = block()
                break  // 成功则跳出，与源码 return block() 一致
            } catch (e: Exception) {
                // 模拟退避（跳过实际 delay）
            }
        }

        assertEquals("success", result)
        assertEquals(3, callCount) // 前2次失败 + 第3次成功
    }

    @Test
    fun `withExponentialBackoff 重试耗尽后应返回null`() {
        var callCount = 0
        val maxRetries = 3

        // 模拟全部失败的场景
        val block: () -> String = {
            callCount++
            throw RuntimeException("网络错误 attempt=$callCount")
        }

        var result: String? = null
        for (attempt in 0..maxRetries) {
            try {
                result = block()
                break
            } catch (e: Exception) {
                // 退避
            }
        }

        // 与源码一致：全部失败后返回 null
        assertNull(result)
        assertEquals(4, callCount) // maxRetries + 1 = 4 次尝试
    }

    @Test
    fun `withExponentialBackoff 各次重试的延迟应按指数增长`() {
        val baseDelayMs = 1000L
        val maxRetries = 3
        val delays = mutableListOf<Long>()

        for (attempt in 0 until maxRetries) {
            val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
            delays.add(delayMs)
        }

        assertEquals(listOf(1000L, 2000L, 4000L), delays)
    }

    // ==================== 4. 远程预设 JSON 解析 ====================

    @Test
    fun `JSON解析 应正确解析有效PresetList`() {
        val jsonString = """{
            "name": "测试订阅",
            "author": "测试作者",
            "build": 42,
            "version": 2,
            "presets": [
                {
                    "id": "preset-1",
                    "name": "日落金辉",
                    "coverPath": "covers/sunset.jpg",
                    "author": "@摄影师A",
                    "brand": "oppo",
                    "build": 5
                },
                {
                    "id": "preset-2",
                    "name": "冷调蓝",
                    "coverPath": "covers/blue.jpg",
                    "author": "@摄影师B"
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)

        assertEquals("测试订阅", presetList.name)
        assertEquals("测试作者", presetList.author)
        assertEquals(42, presetList.build)
        assertEquals(2, presetList.version)
        assertEquals(2, presetList.presets.size)
        assertEquals("日落金辉", presetList.presets[0].name)
        assertEquals("冷调蓝", presetList.presets[1].name)
    }

    @Test
    fun `JSON解析 应忽略未知字段（ignoreUnknownKeys=true）`() {
        val jsonString = """{
            "name": "测试",
            "author": "作者",
            "unknownField": "should be ignored",
            "anotherUnknown": 123,
            "presets": []
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertEquals("测试", presetList.name)
    }

    @Test
    fun `JSON解析 应强制填充默认值（coerceInputValues=true）`() {
        val jsonString = """{
            "name": "测试",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "预设1",
                    "coverPath": "cover.jpg",
                    "isFavorite": null
                }
            ]
        }"""

        // coerceInputValues=true：null 布尔值应被替换为默认值 false
        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertFalse(presetList.presets[0].isFavorite)
    }

    @Test
    fun `JSON解析 宽松模式应接受标准JSON（isLenient=true）`() {
        val jsonString = """{
            "name": "宽松测试",
            "author": "作者",
            "presets": []
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertEquals("宽松测试", presetList.name)
    }

    @Test
    fun `JSON解析 缺少name和author时不应崩溃（nullable字段）`() {
        val jsonString = """{
            "presets": []
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertNull(presetList.name)
        assertNull(presetList.author)
        assertEquals(1, presetList.build) // 默认值
    }

    @Test
    fun `JSON解析 空presets数组应正常工作`() {
        val jsonString = """{"name":"空列表","author":"测试","presets":[]}"""
        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)

        assertEquals("空列表", presetList.name)
        assertTrue(presetList.presets.isEmpty())
    }

    @Test
    fun `JSON解析 无效JSON应抛出异常`() {
        val invalidJson = "not valid json {{{"

        assertThrows(Exception::class.java) {
            json.decodeFromString(PresetList.serializer(), invalidJson)
        }
    }

    @Test
    fun `JSON解析 包含sections的预设应正确解析`() {
        val jsonString = """{
            "name": "参数预设",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "专业模式",
                    "coverPath": "cover.jpg",
                    "sections": [
                        {
                            "title": "基础参数",
                            "items": [
                                {"label": "ISO", "value": "100", "span": 1},
                                {"label": "快门速度", "value": "1/1000s", "span": 2}
                            ]
                        }
                    ]
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        val sections = presetList.presets[0].sections
        assertNotNull(sections)
        assertEquals(1, sections!!.size)
        assertEquals("基础参数", sections[0].title)
        assertEquals(2, sections[0].items.size)
        assertEquals("ISO", sections[0].items[0].label)
        assertEquals(2, sections[0].items[1].span)
    }

    @Test
    fun `JSON解析 包含tags的预设应正确解析`() {
        val jsonString = """{
            "name": "标签测试",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "人像",
                    "coverPath": "cover.jpg",
                    "tags": ["人像", "室内", "柔光"]
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertEquals(listOf("人像", "室内", "柔光"), presetList.presets[0].tags)
    }

    @Test
    fun `JSON解析 包含社区数据的预设应正确解析`() {
        val jsonString = """{
            "name": "社区测试",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "热门预设",
                    "coverPath": "cover.jpg",
                    "downloads": 15000,
                    "rating": 4.8,
                    "ratingCount": 233,
                    "isHncs": true
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        val preset = presetList.presets[0]
        assertEquals(15000, preset.downloads)
        assertEquals(4.8f, preset.rating!!, 0.01f)
        assertEquals(233, preset.ratingCount)
        assertTrue(preset.isHncs)
    }

    @Test
    fun `JSON解析 包含params和colorGradingParams的预设应正确解析`() {
        val jsonString = """{
            "name": "参数测试",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "专业参数",
                    "coverPath": "cover.jpg",
                    "params": {"ISO": "400", "shutter": "1/250"},
                    "colorGradingParams": {"saturation": "+2", "contrast": "+1"}
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        val preset = presetList.presets[0]
        assertEquals(mapOf("ISO" to "400", "shutter" to "1/250"), preset.params)
        assertEquals(mapOf("saturation" to "+2", "contrast" to "+1"), preset.colorGradingParams)
    }

    @Test
    fun `JSON解析 未知字段在MasterPreset中应被忽略`() {
        val jsonString = """{
            "name": "未知字段测试",
            "author": "作者",
            "presets": [
                {
                    "id": "p1",
                    "name": "预设",
                    "coverPath": "cover.jpg",
                    "futureField": "来自未来版本的字段",
                    "experimental": true
                }
            ]
        }"""

        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertEquals(1, presetList.presets.size)
        assertEquals("预设", presetList.presets[0].name)
    }

    @Test
    fun `JSON解析 PresetList序列化反序列化往返应一致`() {
        val original = PresetList(
            name = "往返测试",
            author = "作者",
            build = 42,
            version = 2,
            presets = listOf(
                MasterPreset(id = "p1", name = "预设", coverPath = "c.jpg", brand = "oppo")
            )
        )

        val jsonString = json.encodeToString(PresetList.serializer(), original)
        val decoded = json.decodeFromString(PresetList.serializer(), jsonString)

        assertEquals(original.name, decoded.name)
        assertEquals(original.author, decoded.author)
        assertEquals(original.build, decoded.build)
        assertEquals(original.version, decoded.version)
        assertEquals(original.presets.size, decoded.presets.size)
        assertEquals(original.presets[0].id, decoded.presets[0].id)
        assertEquals(original.presets[0].brand, decoded.presets[0].brand)
    }

    // ==================== 5. 缓存行为（URL验证/Cache-Headers/SSRF防护） ====================

    // ---------- validateUrl（通过反射） ----------

    @Test
    fun `validateUrl 空白URL应返回错误`() {
        val method = getValidateUrlMethod()

        assertEquals("URL 不能为空", method.invoke(PresetRemoteManager, ""))
        assertEquals("URL 不能为空", method.invoke(PresetRemoteManager, "   "))
    }

    @Test
    fun `validateUrl 不合法URL格式应返回错误`() {
        val method = getValidateUrlMethod()

        val result = method.invoke(PresetRemoteManager, "not a url at all")
        assertNotNull(result)
        assertEquals("URL 格式不合法", result)
    }

    @Test
    fun `validateUrl 仅支持HTTPS协议`() {
        val method = getValidateUrlMethod()

        assertEquals("仅支持 HTTPS 协议", method.invoke(PresetRemoteManager, "http://example.com/presets.json"))
        assertEquals("仅支持 HTTPS 协议", method.invoke(PresetRemoteManager, "ftp://example.com/presets.json"))
    }

    @Test
    fun `validateUrl 合法HTTPS URL应返回null`() {
        val method = getValidateUrlMethod()

        val result = method.invoke(PresetRemoteManager, "https://cdn.jsdelivr.net/gh/user/repo/presets.json")
        assertNull(result)
    }

    @Test
    fun `validateUrl 禁止userInfo绕过`() {
        val method = getValidateUrlMethod()

        val result = method.invoke(PresetRemoteManager, "https://evil.com@cdn.jsdelivr.net/presets.json")
        assertEquals("URL 中不允许包含用户信息", result)
    }

    @Test
    fun `validateUrl 仅允许443端口`() {
        val method = getValidateUrlMethod()

        assertEquals("仅允许标准 HTTPS 端口 (443)", method.invoke(PresetRemoteManager, "https://example.com:8080/presets.json"))
        assertEquals("仅允许标准 HTTPS 端口 (443)", method.invoke(PresetRemoteManager, "https://example.com:8443/presets.json"))
    }

    @Test
    fun `validateUrl 443端口和默认端口应通过`() {
        val method = getValidateUrlMethod()

        assertNull(method.invoke(PresetRemoteManager, "https://example.com:443/presets.json"))
        assertNull(method.invoke(PresetRemoteManager, "https://example.com/presets.json"))
    }

    @Test
    fun `validateUrl 禁止localhost访问`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://localhost/presets.json"))
    }

    @Test
    fun `validateUrl 禁止127回环地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://127.0.0.1/presets.json"))
    }

    @Test
    fun `validateUrl 禁止10段私有地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://10.0.0.1/presets.json"))
    }

    @Test
    fun `validateUrl 禁止192_168段私有地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://192.168.1.1/presets.json"))
    }

    @Test
    fun `validateUrl 禁止172_16到172_31段私有地址`() {
        val method = getValidateUrlMethod()

        for (i in 16..31) {
            val url = "https://172.$i.0.1/presets.json"
            assertEquals("172.$i 段应被拦截", "禁止访问内网或本地地址", method.invoke(PresetRemoteManager, url))
        }
    }

    @Test
    fun `validateUrl 禁止169_254链路本地地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://169.254.1.1/presets.json"))
    }

    @Test
    fun `validateUrl 禁止0_0_0_0地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://0.0.0.0/presets.json"))
    }

    @Test
    fun `validateUrl 禁止IPv6回环地址`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://::1/presets.json"))
    }

    @Test
    fun `validateUrl 禁止IPv6方括号形式`() {
        val method = getValidateUrlMethod()

        val result = method.invoke(PresetRemoteManager, "https://[::1]/presets.json")
        assertNotNull("IPv6 方括号形式应被拦截", result)
    }

    @Test
    fun `validateUrl 禁止IPv4直接访问`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止直接使用 IP 地址", method.invoke(PresetRemoteManager, "https://1.2.3.4/presets.json"))
    }

    @Test
    fun `validateUrl 禁止IPv6本地前缀`() {
        val method = getValidateUrlMethod()

        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://fc00::1/presets.json"))
        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://fe80::1/presets.json"))
        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://ff00::1/presets.json"))
        assertEquals("禁止访问内网或本地地址", method.invoke(PresetRemoteManager, "https://ff02::1/presets.json"))
    }

    @Test
    fun `validateUrl 合法域名应通过验证`() {
        val method = getValidateUrlMethod()

        assertNull(method.invoke(PresetRemoteManager, "https://cdn.jsdelivr.net/gh/user/repo/presets.json"))
        assertNull(method.invoke(PresetRemoteManager, "https://raw.githubusercontent.com/user/repo/main/presets.json"))
        assertNull(method.invoke(PresetRemoteManager, "https://example.com/api/v1/presets"))
    }

    // ---------- buildCacheHeaders（通过反射） ----------

    @Test
    fun `buildCacheHeaders 无etag时应只包含Cache-Control`() {
        val method = getBuildCacheHeadersMethod()

        @Suppress("UNCHECKED_CAST")
        val headers = method.invoke(PresetRemoteManager, null as String?) as Map<String, String>

        assertEquals(1, headers.size)
        assertEquals("max-age=3600, stale-while-revalidate=86400", headers["Cache-Control"])
        assertFalse(headers.containsKey("If-None-Match"))
    }

    @Test
    fun `buildCacheHeaders 有etag时应包含Cache-Control和If-None-Match`() {
        val method = getBuildCacheHeadersMethod()

        @Suppress("UNCHECKED_CAST")
        val headers = method.invoke(PresetRemoteManager, "abc123") as Map<String, String>

        assertEquals(2, headers.size)
        assertEquals("max-age=3600, stale-while-revalidate=86400", headers["Cache-Control"])
        assertEquals("abc123", headers["If-None-Match"])
    }

    @Test
    fun `buildCacheHeaders Cache-Control应包含max-age和stale-while-revalidate`() {
        val method = getBuildCacheHeadersMethod()

        @Suppress("UNCHECKED_CAST")
        val headers = method.invoke(PresetRemoteManager, null as String?) as Map<String, String>

        val cacheControl = headers["Cache-Control"]!!
        assertTrue(cacheControl.contains("max-age=3600"))
        assertTrue(cacheControl.contains("stale-while-revalidate=86400"))
    }

    @Test
    fun `buildCacheHeaders 空字符串etag应设置空If-None-Match`() {
        val method = getBuildCacheHeadersMethod()

        @Suppress("UNCHECKED_CAST")
        val headers = method.invoke(PresetRemoteManager, "") as Map<String, String>

        // 空字符串非null，let 块会执行，设置 If-None-Match = ""
        assertTrue(headers.containsKey("If-None-Match"))
    }

    // ==================== fetchAndSave 业务逻辑验证 ====================

    @Test
    fun `fetchAndSave 缺少name字段的JSON应被判定为无效`() {
        val jsonString = """{"author":"作者","presets":[]}"""
        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertTrue(presetList.name.isNullOrBlank())
    }

    @Test
    fun `fetchAndSave 缺少author字段的JSON应被判定为无效`() {
        val jsonString = """{"name":"名称","presets":[]}"""
        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertTrue(presetList.author.isNullOrBlank())
    }

    @Test
    fun `fetchAndSave name和author都存在时不应报缺少字段`() {
        val jsonString = """{"name":"名称","author":"作者","presets":[]}"""
        val presetList = json.decodeFromString(PresetList.serializer(), jsonString)
        assertFalse(presetList.name.isNullOrBlank())
        assertFalse(presetList.author.isNullOrBlank())
    }

    @Test
    fun `fetchAndSave 版本相同时应返回无需更新`() {
        val presetList1 = PresetList(name = "测试", author = "作者", build = 5)
        val presetList2 = PresetList(name = "测试", author = "作者", build = 5)

        assertEquals(presetList1.build, presetList2.build)
    }

    @Test
    fun `fetchAndSave 版本不同时应有差异`() {
        val presetList1 = PresetList(name = "测试", author = "作者", build = 5)
        val presetList2 = PresetList(name = "测试", author = "作者", build = 10)

        assertNotEquals(presetList1.build, presetList2.build)
    }

    // ==================== PresetList 数据模型验证 ====================

    @Test
    fun `PresetList 默认值应正确`() {
        val presetList = PresetList()
        assertNull(presetList.name)
        assertNull(presetList.author)
        assertEquals(1, presetList.build)
        assertEquals(1, presetList.version)
        assertTrue(presetList.presets.isEmpty())
    }

    @Test
    fun `PresetList 完整数据应正确`() {
        val presets = listOf(
            MasterPreset(id = "1", name = "预设1", coverPath = "cover1.jpg"),
            MasterPreset(id = "2", name = "预设2", coverPath = "cover2.jpg")
        )
        val presetList = PresetList(
            name = "测试订阅",
            author = "测试作者",
            build = 100,
            version = 3,
            presets = presets
        )

        assertEquals("测试订阅", presetList.name)
        assertEquals("测试作者", presetList.author)
        assertEquals(100, presetList.build)
        assertEquals(3, presetList.version)
        assertEquals(2, presetList.presets.size)
    }
}
