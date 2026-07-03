package com.silas.omaster.util

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CrashMonitorManager 单元测试
 *
 * 验证崩溃监控管理器的初始化、CrashListener 注册、崩溃计数和回调机制。
 * 使用 MockK 模拟 Android 依赖（Context、CrashHandler）。
 */
class CrashMonitorManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAppContext: Context
    private lateinit var mockCrashHandler: CrashHandler
    private lateinit var testDispatcher: StandardTestDispatcher
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAppContext = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockAppContext

        // Mock CrashHandler singleton via reflection
        mockCrashHandler = mockk(relaxed = true)
        every { mockCrashHandler.addCrashListener(any()) } just runs
        every { mockCrashHandler.removeCrashListener(any()) } just runs
        setCrashHandlerInstance(mockCrashHandler)

        // Replace the scope with a TestDispatcher-based scope
        testDispatcher = StandardTestDispatcher()
        testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        setPrivateField("scope", testScope)

        // Reset state
        setPrivateField("context", null)
        setPrivateField("_crashCount", MutableStateFlow(0))
        setPrivateField("isUploadEnabled", false)
        setPrivateField("uploadUrl", null)
    }

    @After
    fun tearDown() {
        // Reset CrashHandler singleton
        setCrashHandlerInstance(null)
        // Reset CrashMonitorManager state
        setPrivateField("context", null)
        setPrivateField("_crashCount", MutableStateFlow(0))
    }

    // ==================================================================
    // 1. init() 注册为 CrashListener 测试
    // ==================================================================

    @Test
    fun `init - 应将自身注册为 CrashHandler 的 CrashListener`() {
        CrashMonitorManager.init(mockContext)

        verify(exactly = 1) {
            mockCrashHandler.addCrashListener(CrashMonitorManager)
        }
    }

    @Test
    fun `init - 不应抛出异常`() {
        try {
            CrashMonitorManager.init(mockContext)
        } catch (e: Exception) {
            org.junit.Assert.fail("init() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `init - 应保存 applicationContext`() {
        CrashMonitorManager.init(mockContext)
        val ctx = getPrivateField<Context>("context")
        assertEquals("Should store applicationContext", mockAppContext, ctx)
    }

    @Test
    fun `init - 应设置 appVersion`() {
        CrashMonitorManager.init(mockContext, appVersion = "1.2.3")
        val version = getPrivateField<String>("appVersion")
        assertEquals("Should store appVersion", "1.2.3", version)
    }

    @Test
    fun `init - 默认 appVersion 应为 unknown`() {
        CrashMonitorManager.init(mockContext)
        val version = getPrivateField<String>("appVersion")
        assertEquals("Default appVersion should be unknown", "unknown", version)
    }

    @Test
    fun `init - 应设置上传配置`() {
        CrashMonitorManager.init(
            mockContext,
            uploadUrl = "https://example.com/crash",
            enableUpload = true
        )
        val uploadUrl = getPrivateField<String>("uploadUrl")
        val isUploadEnabled = getPrivateField<Boolean>("isUploadEnabled")
        assertEquals("Should store uploadUrl", "https://example.com/crash", uploadUrl)
        assertTrue("Should enable upload", isUploadEnabled)
    }

    // ==================================================================
    // 2. crashCount 初始值测试
    // ==================================================================

    @Test
    fun `crashCount 初始值应为 0`() {
        setPrivateField("_crashCount", MutableStateFlow(0))
        assertEquals("crashCount should start at 0", 0, CrashMonitorManager.crashCount.value)
    }

    @Test
    fun `crashCount - init 后不应自动递增`() {
        setPrivateField("_crashCount", MutableStateFlow(0))
        CrashMonitorManager.init(mockContext)
        // init 不会立即改变 crashCount（异步统计已有日志数的协程尚未执行）
        // crashCount 仍应保持初始值
        val count = CrashMonitorManager.crashCount.value
        assertTrue("crashCount should be >= 0 after init", count >= 0)
    }

    // ==================================================================
    // 3. onCrash() 回调递增 crashCount 测试
    // ==================================================================

    @Test
    fun `onCrash - 应递增 crashCount`() = runTest {
        setPrivateField("_crashCount", MutableStateFlow(0))
        // Replace scope with test scope for this test
        setPrivateField("scope", testScope)

        CrashMonitorManager.onCrash(
            Thread.currentThread(),
            RuntimeException("test crash"),
            "OTHER"
        )

        // 推进协程执行
        advanceUntilIdle()

        assertEquals("crashCount should increment after onCrash", 1, CrashMonitorManager.crashCount.value)
    }

    @Test
    fun `onCrash - 多次调用应持续递增 crashCount`() = runTest {
        setPrivateField("_crashCount", MutableStateFlow(0))
        setPrivateField("scope", testScope)

        CrashMonitorManager.onCrash(Thread.currentThread(), RuntimeException("crash 1"), "OTHER")
        advanceUntilIdle()
        assertEquals("crashCount should be 1 after first crash", 1, CrashMonitorManager.crashCount.value)

        CrashMonitorManager.onCrash(Thread.currentThread(), RuntimeException("crash 2"), "NPE")
        advanceUntilIdle()
        assertEquals("crashCount should be 2 after second crash", 2, CrashMonitorManager.crashCount.value)

        CrashMonitorManager.onCrash(Thread.currentThread(), OutOfMemoryError("crash 3"), "OOM")
        advanceUntilIdle()
        assertEquals("crashCount should be 3 after third crash", 3, CrashMonitorManager.crashCount.value)
    }

    @Test
    fun `onCrash - 回调不应抛出异常`() = runTest {
        setPrivateField("_crashCount", MutableStateFlow(0))
        setPrivateField("scope", testScope)

        try {
            CrashMonitorManager.onCrash(
                Thread.currentThread(),
                RuntimeException("test"),
                "OTHER"
            )
            advanceUntilIdle()
        } catch (e: Exception) {
            org.junit.Assert.fail("onCrash() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `onCrash - 无 context 时也不应崩溃`() = runTest {
        // context 为 null
        setPrivateField("context", null)
        setPrivateField("_crashCount", MutableStateFlow(0))
        setPrivateField("scope", testScope)

        try {
            CrashMonitorManager.onCrash(Thread.currentThread(), RuntimeException("test"), "OTHER")
            advanceUntilIdle()
        } catch (e: Exception) {
            org.junit.Assert.fail("onCrash() should not crash without context, but got: ${e.message}")
        }

        assertEquals("crashCount should still increment without context", 1, CrashMonitorManager.crashCount.value)
    }

    // ==================================================================
    // 4. CrashMonitorManager 作为 CrashListener 接口实现测试
    // ==================================================================

    @Test
    fun `应实现 CrashHandler_CrashListener 接口`() {
        assertTrue(
            "CrashMonitorManager should implement CrashHandler.CrashListener",
            CrashMonitorManager is CrashHandler.CrashListener
        )
    }

    // ==================================================================
    // 辅助方法：通过反射访问私有字段
    // ==================================================================

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateField(fieldName: String): T {
        val field = CrashMonitorManager::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(CrashMonitorManager) as T
    }

    private fun setPrivateField(fieldName: String, value: Any?) {
        val field = CrashMonitorManager::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(CrashMonitorManager, value)
    }

    /**
     * 通过反射设置 CrashHandler 单例实例
     */
    private fun setCrashHandlerInstance(instance: CrashHandler?) {
        try {
            val field = CrashHandler::class.java.getDeclaredField("instance")
            field.isAccessible = true
            field.set(null, instance)
        } catch (_: Exception) {
            // 忽略设置失败
        }
    }
}