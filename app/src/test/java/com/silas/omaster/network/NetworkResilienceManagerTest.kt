package com.silas.omaster.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NetworkResilienceManager 单元测试
 *
 * 验证网络韧性管理器的初始化、网络状态监控、Flow 暴露和查询方法。
 * 使用 MockK 模拟 Android 依赖（Context、ConnectivityManager）。
 */
class NetworkResilienceManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAppContext = mockk(relaxed = true)
        mockConnectivityManager = mockk(relaxed = true)

        every { mockContext.applicationContext } returns mockAppContext
        every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        // Mock registerNetworkCallback to accept any request and callback
        every {
            mockConnectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                any<ConnectivityManager.NetworkCallback>()
            )
        } just runs

        // Reset singleton state before each test
        resetManagerState()
    }

    @After
    fun tearDown() {
        resetManagerState()
    }

    /**
     * 通过反射重置 NetworkResilienceManager 的单例状态
     */
    private fun resetManagerState() {
        setPrivateField("isInitialized", false)
        setPrivateField("connectivityManager", null)
        setPrivateField("_networkState", MutableStateFlow(NetworkResilienceManager.NetworkState.Unknown))
        setPrivateField("_isOnline", MutableStateFlow(false))
    }

    // ==================================================================
    // 1. init() 测试
    // ==================================================================

    @Test
    fun `init - 使用 context 初始化不应抛出异常`() {
        try {
            NetworkResilienceManager.init(mockContext)
        } catch (e: Exception) {
            org.junit.Assert.fail("init() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `init - 初始化后 isInitialized 应为 true`() {
        NetworkResilienceManager.init(mockContext)
        val initialized = getPrivateField<Boolean>("isInitialized")
        assertTrue("isInitialized should be true after init", initialized)
    }

    @Test
    fun `init - 应获取 ConnectivityManager 系统服务`() {
        NetworkResilienceManager.init(mockContext)
        verify(exactly = 1) {
            mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        }
    }

    @Test
    fun `init - 应注册网络回调`() {
        NetworkResilienceManager.init(mockContext)
        verify(atLeast = 1) {
            mockConnectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                any<ConnectivityManager.NetworkCallback>()
            )
        }
    }

    @Test
    fun `init - 重复初始化应安全（幂等）`() {
        NetworkResilienceManager.init(mockContext)
        try {
            NetworkResilienceManager.init(mockContext)
        } catch (e: Exception) {
            org.junit.Assert.fail("Duplicate init() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `init - 重复初始化不应再次注册网络回调`() {
        NetworkResilienceManager.init(mockContext)
        NetworkResilienceManager.init(mockContext)
        // 只应注册一次
        verify(exactly = 1) {
            mockConnectivityManager.registerNetworkCallback(
                any<NetworkRequest>(),
                any<ConnectivityManager.NetworkCallback>()
            )
        }
    }

    // ==================================================================
    // 2. networkState 初始值测试
    // ==================================================================

    @Test
    fun `networkState 初始值应为 Unknown`() {
        assertEquals(
            "networkState initial value should be Unknown",
            NetworkResilienceManager.NetworkState.Unknown,
            NetworkResilienceManager.networkState.value
        )
    }

    @Test
    fun `networkState 在 init 后不应为 Unknown（取决于网络状态）`() {
        // 当没有 active network 时，init 会调用 updateNetworkState
        // connectivityManager.activeNetwork 为 null（mock relaxed 返回 null）
        // 所以 state 应该变为 Unavailable
        NetworkResilienceManager.init(mockContext)
        val state = NetworkResilienceManager.networkState.value
        // 由于 mock activeNetwork 为 null，应变为 Unavailable
        assertTrue(
            "networkState after init should be Available, Metered, or Unavailable (not Unknown)",
            state != NetworkResilienceManager.NetworkState.Unknown
        )
    }

    // ==================================================================
    // 3. isOnline 初始值测试
    // ==================================================================

    @Test
    fun `isOnline 初始值应为 false`() {
        assertFalse(
            "isOnline initial value should be false",
            NetworkResilienceManager.isOnline.value
        )
    }

    // ==================================================================
    // 4. observeNetworkState() 测试
    // ==================================================================

    @Test
    fun `observeNetworkState 应返回一个 Flow`() {
        val flow = NetworkResilienceManager.observeNetworkState()
        assertNotNull("observeNetworkState() should return a non-null Flow", flow)
    }

    @Test
    fun `observeNetworkState 初始发射值应为 Unknown`() = runTest {
        val flow = NetworkResilienceManager.observeNetworkState()
        val firstValue = flow.first()
        assertEquals(
            "observeNetworkState first emission should be Unknown",
            NetworkResilienceManager.NetworkState.Unknown,
            firstValue
        )
    }

    // ==================================================================
    // 5. isCurrentlyOnline() 测试
    // ==================================================================

    @Test
    fun `isCurrentlyOnline - 初始状态应返回 false`() {
        assertFalse(
            "isCurrentlyOnline() should return false before init",
            NetworkResilienceManager.isCurrentlyOnline()
        )
    }

    @Test
    fun `isCurrentlyOnline - init 后应反映真实网络状态`() {
        // 没有 active network 的 mock，应返回 false
        NetworkResilienceManager.init(mockContext)
        assertFalse(
            "isCurrentlyOnline() should return false when no active network",
            NetworkResilienceManager.isCurrentlyOnline()
        )
    }

    // ==================================================================
    // 6. isMeteredNetwork() 测试
    // ==================================================================

    @Test
    fun `isMeteredNetwork - 初始状态应返回 false`() {
        assertFalse(
            "isMeteredNetwork() should return false initially",
            NetworkResilienceManager.isMeteredNetwork()
        )
    }

    @Test
    fun `isMeteredNetwork - init 后无网络时应返回 false`() {
        // 没有 active network，state 变为 Unavailable，不是 Metered
        NetworkResilienceManager.init(mockContext)
        assertFalse(
            "isMeteredNetwork() should return false when network is unavailable",
            NetworkResilienceManager.isMeteredNetwork()
        )
    }

    // ==================================================================
    // 7. NetworkState 枚举值测试
    // ==================================================================

    @Test
    fun `NetworkState 枚举应包含所有预期值`() {
        val values = NetworkResilienceManager.NetworkState.values()
        assertEquals("NetworkState should have 4 values", 4, values.size)
        assertTrue(
            "Should contain Unknown",
            values.contains(NetworkResilienceManager.NetworkState.Unknown)
        )
        assertTrue(
            "Should contain Available",
            values.contains(NetworkResilienceManager.NetworkState.Available)
        )
        assertTrue(
            "Should contain Metered",
            values.contains(NetworkResilienceManager.NetworkState.Metered)
        )
        assertTrue(
            "Should contain Unavailable",
            values.contains(NetworkResilienceManager.NetworkState.Unavailable)
        )
    }

    // ==================================================================
    // 辅助方法：通过反射访问私有字段
    // ==================================================================

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateField(fieldName: String): T {
        val field = NetworkResilienceManager::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(NetworkResilienceManager) as T
    }

    private fun setPrivateField(fieldName: String, value: Any?) {
        val field = NetworkResilienceManager::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(NetworkResilienceManager, value)
    }
}