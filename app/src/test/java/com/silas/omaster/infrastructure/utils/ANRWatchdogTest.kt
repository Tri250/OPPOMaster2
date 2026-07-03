package com.silas.omaster.infrastructure.utils

import android.os.Handler
import android.os.Looper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ANRWatchdog 单元测试
 *
 * 验证 ANR 看门狗的安装、卸载、状态管理和幂等性。
 * 使用 MockK 模拟 Android 依赖（Handler、Looper、Sentry）。
 */
class ANRWatchdogTest {

    private lateinit var mockHandler: Handler
    private lateinit var mockLooper: Looper

    @Before
    fun setUp() {
        // Mock Looper.getMainLooper() to return a mock Looper
        mockLooper = mockk(relaxed = true)
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockLooper

        // Mock Sentry to avoid real Sentry calls
        mockkObject(Sentry)
        every { Sentry.captureMessage(any(), any()) } just runs

        // Replace the private handler field with a mock
        mockHandler = mockk(relaxed = true)
        every { mockHandler.post(any()) } returns true
        every { mockHandler.removeCallbacksAndMessages(any()) } just runs
        setPrivateField("handler", mockHandler)

        // Replace the scope with a TestDispatcher-based scope
        val testDispatcher = StandardTestDispatcher()
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        setPrivateField("scope", testScope)

        // Reset running state before each test
        setPrivateField("running", false)
        setPrivateField("lastTickTime", 0L)
        setPrivateField("installTime", 0L)
        setPrivateField("_anrCount", kotlinx.coroutines.flow.MutableStateFlow(0))
    }

    @After
    fun tearDown() {
        unmockkObject(Sentry)
        unmockkStatic(Looper::class)
    }

    // ==================================================================
    // 1. install() 测试
    // ==================================================================

    @Test
    fun `install - 不应抛出异常`() {
        try {
            ANRWatchdog.install()
            // 如果执行到这里说明没有抛出异常
        } catch (e: Exception) {
            org.junit.Assert.fail("install() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `install - 安装后 running 状态应为 true`() {
        ANRWatchdog.install()
        val running = getPrivateField<Boolean>("running")
        assertTrue("install() should set running to true", running)
    }

    @Test
    fun `install - 安装后 installTime 应大于 0`() {
        ANRWatchdog.install()
        val installTime = getPrivateField<Long>("installTime")
        assertTrue("installTime should be > 0 after install", installTime > 0)
    }

    @Test
    fun `install - 安装后 lastTickTime 应等于 installTime`() {
        ANRWatchdog.install()
        val installTime = getPrivateField<Long>("installTime")
        val lastTickTime = getPrivateField<Long>("lastTickTime")
        assertEquals("lastTickTime should equal installTime after install", installTime, lastTickTime)
    }

    // ==================================================================
    // 2. 重复 install() 幂等性测试
    // ==================================================================

    @Test
    fun `重复install - 不应抛出异常`() {
        ANRWatchdog.install()
        try {
            ANRWatchdog.install()
        } catch (e: Exception) {
            org.junit.Assert.fail("Duplicate install() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `重复install - running 状态应保持 true`() {
        ANRWatchdog.install()
        ANRWatchdog.install()
        val running = getPrivateField<Boolean>("running")
        assertTrue("running should remain true after duplicate install", running)
    }

    @Test
    fun `重复install - installTime 不应被覆盖`() {
        ANRWatchdog.install()
        val firstInstallTime = getPrivateField<Long>("installTime")

        // 稍作等待确保时间差
        Thread.sleep(5)

        ANRWatchdog.install()
        val secondInstallTime = getPrivateField<Long>("installTime")

        assertEquals(
            "installTime should not be overwritten by duplicate install",
            firstInstallTime,
            secondInstallTime
        )
    }

    // ==================================================================
    // 3. uninstall() 测试
    // ==================================================================

    @Test
    fun `uninstall - 不应抛出异常`() {
        ANRWatchdog.install()
        try {
            ANRWatchdog.uninstall()
        } catch (e: Exception) {
            org.junit.Assert.fail("uninstall() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `uninstall - 应设置 running 为 false`() {
        ANRWatchdog.install()
        ANRWatchdog.uninstall()
        val running = getPrivateField<Boolean>("running")
        assertFalse("uninstall() should set running to false", running)
    }

    @Test
    fun `uninstall - 应调用 handler 的 removeCallbacksAndMessages`() {
        ANRWatchdog.install()
        ANRWatchdog.uninstall()
        verify(atLeast = 1) { mockHandler.removeCallbacksAndMessages(null) }
    }

    // ==================================================================
    // 4. uninstall() before install() 安全性测试
    // ==================================================================

    @Test
    fun `uninstall在install之前调用 - 不应抛出异常`() {
        try {
            ANRWatchdog.uninstall()
        } catch (e: Exception) {
            org.junit.Assert.fail("uninstall() before install() should not throw, but got: ${e.message}")
        }
    }

    @Test
    fun `uninstall在install之前调用 - running 应保持 false`() {
        ANRWatchdog.uninstall()
        val running = getPrivateField<Boolean>("running")
        assertFalse("running should remain false", running)
    }

    @Test
    fun `uninstall在install之前调用 - 不应影响后续 install`() {
        ANRWatchdog.uninstall()
        ANRWatchdog.install()
        val running = getPrivateField<Boolean>("running")
        assertTrue("install should still work after uninstall-before-install", running)
    }

    // ==================================================================
    // 5. anrCount 初始值测试
    // ==================================================================

    @Test
    fun `anrCount 初始值应为 0`() {
        // 重置 _anrCount 确保干净状态
        setPrivateField("_anrCount", kotlinx.coroutines.flow.MutableStateFlow(0))
        assertEquals("anrCount should start at 0", 0, ANRWatchdog.anrCount.value)
    }

    @Test
    fun `anrCount - 安装后仍为 0`() {
        setPrivateField("_anrCount", kotlinx.coroutines.flow.MutableStateFlow(0))
        ANRWatchdog.install()
        assertEquals("anrCount should remain 0 after install", 0, ANRWatchdog.anrCount.value)
    }

    @Test
    fun `anrCount - 卸载后仍为 0（无ANR触发）`() {
        setPrivateField("_anrCount", kotlinx.coroutines.flow.MutableStateFlow(0))
        ANRWatchdog.install()
        ANRWatchdog.uninstall()
        assertEquals("anrCount should remain 0 after uninstall when no ANR detected", 0, ANRWatchdog.anrCount.value)
    }

    // ==================================================================
    // 辅助方法：通过反射访问私有字段
    // ==================================================================

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPrivateField(fieldName: String): T {
        val field = ANRWatchdog::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(ANRWatchdog) as T
    }

    private fun setPrivateField(fieldName: String, value: Any?) {
        val field = ANRWatchdog::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(ANRWatchdog, value)
    }
}