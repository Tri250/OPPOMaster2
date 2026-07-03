package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.model.Subscription
import com.silas.omaster.model.SubscriptionList
import com.silas.omaster.infrastructure.security.SecurityCrypto
import com.silas.omaster.infrastructure.utils.UpdateConfigManager
import com.silas.omaster.infrastructure.utils.UrlConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Constructor

/**
 * SubscriptionManager 单元测试
 *
 * 覆盖：
 * - 默认订阅创建（首次使用）
 * - 添加/移除订阅
 * - URL 验证（HTTPS 强制校验）
 * - 加密/解密订阅数据（SecurityCrypto）
 * - 订阅列表状态管理
 */
class SubscriptionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAppContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.applicationContext } returns mockAppContext
        every { mockAppContext.getSharedPreferences("omaster_subscriptions", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } answers { nothing }

        mockkObject(SecurityCrypto)
    }

    @After
    fun tearDown() {
        unmockkObject(SecurityCrypto)
        // 清除单例，避免测试间干扰
        val field = SubscriptionManager::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }

    /**
     * 通过反射创建 SubscriptionManager 实例（绕过私有构造函数和单例）
     */
    private fun createManager(): SubscriptionManager {
        val constructor: Constructor<SubscriptionManager> =
            SubscriptionManager::class.java.getDeclaredConstructor(Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(mockContext)
    }

    // ============================================================
    // 1. 默认订阅创建（首次使用）
    // ============================================================

    @Test
    fun `首次使用 - 无存储数据时应创建4个默认订阅`() {
        // 首次使用时 encrypted 和 plaintext 都为 null
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null

        // 模拟加密成功
        every { SecurityCrypto.encrypt(any()) } returns "encrypted_dummy"
        every { SecurityCrypto.decrypt(any()) } returns null

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 应创建 4 个默认订阅
        assertEquals(4, subs.size)
        assertEquals(UrlConstants.PRESET_OPPO, subs[0].url)
        assertEquals(UrlConstants.PRESET_REALME, subs[1].url)
        assertEquals(UrlConstants.PRESET_VIVO, subs[2].url)
        assertEquals(UrlConstants.PRESET_HONOR, subs[3].url)
    }

    @Test
    fun `首次使用 - 默认订阅均启用`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "encrypted_dummy"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        assertTrue(subs.all { it.isEnabled })
    }

    @Test
    fun `首次使用 - 默认订阅应通过加密方式保存`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "encrypted_result"

        createManager()

        // 验证加密存储被调用
        verify { SecurityCrypto.encrypt(any()) }
        // 验证加密后的数据写入 encrypted key
        verify { mockEditor.putString("subscriptions_list_enc", "encrypted_result") }
        // 验证同时清除了旧明文 key
        verify { mockEditor.remove("subscriptions_list") }
    }

    @Test
    fun `首次使用 - 加密失败时回退到明文存储`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns null

        createManager()

        // 加密失败时回退明文
        verify { mockEditor.putString("subscriptions_list", any()) }
    }

    // ============================================================
    // 2. 添加/移除订阅
    // ============================================================

    @Test
    fun `addSubscription - 成功添加HTTPS订阅`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.addSubscription("https://example.com/presets.json", "测试订阅", "@Tester", 2)

        val subs = manager.subscriptionsFlow.value
        assertEquals(initialSize + 1, subs.size)
        val added = subs.last()
        assertEquals("https://example.com/presets.json", added.url)
        assertEquals("测试订阅", added.name)
        assertEquals("@Tester", added.author)
        assertEquals(2, added.build)
    }

    @Test
    fun `addSubscription - 不添加重复URL的订阅`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        // OPPO URL 已在默认订阅中
        manager.addSubscription(UrlConstants.PRESET_OPPO, "重复订阅")

        // 数量不应增加
        assertEquals(4, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `removeSubscription - 成功移除订阅`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"
        every { mockAppContext.filesDir } returns mockk(relaxed = true)

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.removeSubscription(UrlConstants.PRESET_OPPO)

        val subs = manager.subscriptionsFlow.value
        assertEquals(initialSize - 1, subs.size)
        assertFalse(subs.any { it.url == UrlConstants.PRESET_OPPO })
    }

    @Test
    fun `removeSubscription - 移除不存在的URL不影响列表`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.removeSubscription("https://nonexistent.example.com/presets.json")

        assertEquals(initialSize, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `addSubscription 后 removeSubscription 列表应恢复原状`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"
        every { mockAppContext.filesDir } returns mockk(relaxed = true)

        val manager = createManager()
        val originalSize = manager.subscriptionsFlow.value.size
        val testUrl = "https://example.com/test.json"

        manager.addSubscription(testUrl, "临时订阅")
        assertEquals(originalSize + 1, manager.subscriptionsFlow.value.size)

        manager.removeSubscription(testUrl)
        assertEquals(originalSize, manager.subscriptionsFlow.value.size)
    }

    // ============================================================
    // 3. URL 验证（HTTPS 强制校验）
    // ============================================================

    @Test
    fun `addSubscription - HTTP URL应被拒绝`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.addSubscription("http://example.com/presets.json", "不安全订阅")

        assertEquals(initialSize, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `addSubscription - 空URL应被拒绝`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.addSubscription("", "空URL订阅")

        assertEquals(initialSize, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `addSubscription - 纯空白URL应被拒绝`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.addSubscription("   ", "空白URL订阅")

        assertEquals(initialSize, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `addSubscription - ftp URL应被拒绝`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val initialSize = manager.subscriptionsFlow.value.size

        manager.addSubscription("ftp://example.com/presets.json", "FTP订阅")

        assertEquals(initialSize, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `addSubscription - HTTPS大小写不敏感应通过`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        // HTTPS 大写应通过校验（代码使用 lowercase().startsWith("https://")）
        manager.addSubscription("HTTPS://example.com/upper.json", "大写HTTPS")

        assertTrue(manager.subscriptionsFlow.value.any { it.url == "HTTPS://example.com/upper.json" })
    }

    // ============================================================
    // 4. 加密/解密订阅数据（SecurityCrypto）
    // ============================================================

    @Test
    fun `加密存储成功时 - 应写入encrypted key并删除明文key`() {
        val testSubs = SubscriptionList(
            listOf(Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设"))
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), testSubs)

        // 模拟已有加密数据
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns "existing_encrypted"
        every { SecurityCrypto.decrypt("existing_encrypted") } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "new_encrypted_value"

        val manager = createManager()

        // 触发一次保存
        manager.addSubscription("https://example.com/new.json", "新订阅")

        verify { mockEditor.putString("subscriptions_list_enc", "new_encrypted_value") }
        verify { mockEditor.remove("subscriptions_list") }
    }

    @Test
    fun `加密存储失败时 - 应回退到明文存储`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } throws SecurityException("Keystore unavailable")

        val manager = createManager()

        // 首次使用时加密失败，应回退明文
        verify { mockEditor.putString("subscriptions_list", any()) }
    }

    @Test
    fun `读取加密数据成功时 - 应优先使用加密数据`() {
        val testSubs = SubscriptionList(
            listOf(
                Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_VIVO, name = "vivo 蔡司自然色彩预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_HONOR, name = "荣耀 Magic 影像预设", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), testSubs)

        // 同时存在加密数据和明文数据
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns "encrypted_data"
        every { mockPrefs.getString("subscriptions_list", null) } returns "plaintext_data"
        every { SecurityCrypto.decrypt("encrypted_data") } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 应使用加密数据（4个默认订阅）
        assertEquals(4, subs.size)
        assertEquals("官方内置预设", subs[0].name)
    }

    @Test
    fun `读取加密数据失败时 - 应回退到明文数据`() {
        val testSubs = SubscriptionList(
            listOf(
                Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_VIVO, name = "vivo 蔡司自然色彩预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_HONOR, name = "荣耀 Magic 影像预设", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), testSubs)

        // 加密数据解密失败
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns "bad_encrypted"
        every { SecurityCrypto.decrypt("bad_encrypted") } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 回退到明文数据
        assertEquals(4, subs.size)
        assertEquals("官方内置预设", subs[0].name)
    }

    @Test
    fun `解密抛出异常时 - 应回退到明文数据`() {
        val testSubs = SubscriptionList(
            listOf(
                Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_VIVO, name = "vivo 蔡司自然色彩预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_HONOR, name = "荣耀 Magic 影像预设", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), testSubs)

        every { mockPrefs.getString("subscriptions_list_enc", null) } returns "corrupted"
        every { SecurityCrypto.decrypt("corrupted") } throws RuntimeException("Decryption error")
        every { mockPrefs.getString("subscriptions_list", null) } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        assertEquals(4, subs.size)
    }

    // ============================================================
    // 5. 订阅列表状态管理
    // ============================================================

    @Test
    fun `toggleSubscription - 应切换订阅启用状态`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        // 初始状态：所有默认订阅均启用
        assertTrue(manager.subscriptionsFlow.value[0].isEnabled)

        // 切换第一个订阅
        manager.toggleSubscription(UrlConstants.PRESET_OPPO)
        assertFalse(manager.subscriptionsFlow.value[0].isEnabled)

        // 再次切换应恢复启用
        manager.toggleSubscription(UrlConstants.PRESET_OPPO)
        assertTrue(manager.subscriptionsFlow.value[0].isEnabled)
    }

    @Test
    fun `toggleSubscription - 仅切换目标订阅，其他不受影响`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        manager.toggleSubscription(UrlConstants.PRESET_OPPO)

        val subs = manager.subscriptionsFlow.value
        assertFalse(subs[0].isEnabled)
        // 其他订阅仍为启用
        assertTrue(subs[1].isEnabled)
        assertTrue(subs[2].isEnabled)
        assertTrue(subs[3].isEnabled)
    }

    @Test
    fun `updateSubscriptionStatus - 应更新预设数量和更新时间`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        manager.updateSubscriptionStatus(
            url = UrlConstants.PRESET_OPPO,
            presetCount = 42,
            lastUpdateTime = 1700000000000L
        )

        val sub = manager.subscriptionsFlow.value.first { it.url == UrlConstants.PRESET_OPPO }
        assertEquals(42, sub.presetCount)
        assertEquals(1700000000000L, sub.lastUpdateTime)
    }

    @Test
    fun `updateSubscriptionStatus - 可选择性更新名称和作者`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        manager.updateSubscriptionStatus(
            url = UrlConstants.PRESET_OPPO,
            presetCount = 10,
            lastUpdateTime = 1000L,
            name = "新名称",
            author = "@新作者",
            build = 5
        )

        val sub = manager.subscriptionsFlow.value.first { it.url == UrlConstants.PRESET_OPPO }
        assertEquals("新名称", sub.name)
        assertEquals("@新作者", sub.author)
        assertEquals(5, sub.build)
    }

    @Test
    fun `updateSubscriptionStatus - name为null时保留原名称`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val originalName = manager.subscriptionsFlow.value.first { it.url == UrlConstants.PRESET_OPPO }.name

        manager.updateSubscriptionStatus(
            url = UrlConstants.PRESET_OPPO,
            presetCount = 5,
            lastUpdateTime = 100L,
            name = null,
            author = null
        )

        val sub = manager.subscriptionsFlow.value.first { it.url == UrlConstants.PRESET_OPPO }
        assertEquals(originalName, sub.name)
    }

    @Test
    fun `updateSubscriptionUrl - 应更新订阅URL`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"
        every { mockAppContext.filesDir } returns mockk(relaxed = true)

        val manager = createManager()
        val oldUrl = UrlConstants.PRESET_OPPO
        val newUrl = "https://cdn.jsdelivr.net/gh/new/repo/presets.json"

        manager.updateSubscriptionUrl(oldUrl, newUrl)

        val subs = manager.subscriptionsFlow.value
        assertFalse(subs.any { it.url == oldUrl })
        assertTrue(subs.any { it.url == newUrl })
    }

    @Test
    fun `updateSubscriptionUrl - oldUrl等于newUrl时不做操作`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val url = UrlConstants.PRESET_OPPO

        manager.updateSubscriptionUrl(url, url)

        // URL 不变，列表不变
        assertTrue(manager.subscriptionsFlow.value.any { it.url == url })
    }

    @Test
    fun `subscriptionsFlow - 应反映实时状态变化`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"
        every { mockAppContext.filesDir } returns mockk(relaxed = true)

        val manager = createManager()

        // 初始 4 个
        assertEquals(4, manager.subscriptionsFlow.value.size)

        // 添加 1 个
        manager.addSubscription("https://example.com/a.json", "A")
        assertEquals(5, manager.subscriptionsFlow.value.size)

        // 移除 1 个
        manager.removeSubscription("https://example.com/a.json")
        assertEquals(4, manager.subscriptionsFlow.value.size)
    }

    @Test
    fun `getFileNameForUrl - 应返回基于URL哈希的文件名`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val url = "https://example.com/test.json"
        val fileName = manager.getFileNameForUrl(url)

        val expectedHash = url.hashCode().toString(16)
        assertEquals("sub_$expectedHash.json", fileName)
    }

    @Test
    fun `getFileNameForUrl - 不同URL应生成不同文件名`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns null
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()

        val name1 = manager.getFileNameForUrl("https://a.com/presets.json")
        val name2 = manager.getFileNameForUrl("https://b.com/presets.json")

        assertTrue(name1 != name2)
    }

    // ============================================================
    // 迁移逻辑测试
    // ============================================================

    @Test
    fun `迁移 - 官方内置预设URL过期时应自动更新`() {
        val oldUrl = "https://old-cdn.example.com/presets.json"
        val storedSubs = SubscriptionList(
            listOf(
                Subscription(url = oldUrl, name = "官方内置预设", author = "@OMaster", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_VIVO, name = "vivo 蔡司自然色彩预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_HONOR, name = "荣耀 Magic 影像预设", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), storedSubs)

        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // "官方内置预设"的 URL 应已迁移为 DEFAULT_PRESET_URL
        val officialSub = subs.first { it.name == "官方内置预设" }
        assertEquals(UpdateConfigManager.DEFAULT_PRESET_URL, officialSub.url)
    }

    @Test
    fun `迁移 - 缺失默认订阅源时应自动补齐`() {
        // 只存储了 2 个默认订阅
        val storedSubs = SubscriptionList(
            listOf(
                Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), storedSubs)

        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 应补齐到 4 个
        assertEquals(4, subs.size)
        assertTrue(subs.any { it.url == UrlConstants.PRESET_VIVO })
        assertTrue(subs.any { it.url == UrlConstants.PRESET_HONOR })
    }

    @Test
    fun `迁移 - 用户自定义订阅应保留`() {
        val customUrl = "https://custom.example.com/my-presets.json"
        val storedSubs = SubscriptionList(
            listOf(
                Subscription(url = UrlConstants.PRESET_OPPO, name = "官方内置预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_REALME, name = "realme GT 大师模式预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_VIVO, name = "vivo 蔡司自然色彩预设", isEnabled = true),
                Subscription(url = UrlConstants.PRESET_HONOR, name = "荣耀 Magic 影像预设", isEnabled = true),
                Subscription(url = customUrl, name = "我的自定义", author = "@Me", isEnabled = true)
            )
        )
        val testJson = json.encodeToString(SubscriptionList.serializer(), storedSubs)

        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns testJson
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 4 个默认 + 1 个自定义 = 5
        assertEquals(5, subs.size)
        assertTrue(subs.any { it.url == customUrl && it.name == "我的自定义" })
    }

    @Test
    fun `迁移 - JSON解析失败时回退到默认订阅`() {
        every { mockPrefs.getString("subscriptions_list_enc", null) } returns null
        every { mockPrefs.getString("subscriptions_list", null) } returns "{invalid_json"
        every { SecurityCrypto.encrypt(any()) } returns "enc"

        val manager = createManager()
        val subs = manager.subscriptionsFlow.value

        // 回退到 4 个默认订阅
        assertEquals(4, subs.size)
    }
}
