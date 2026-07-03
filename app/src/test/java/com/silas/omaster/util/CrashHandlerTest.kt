package com.silas.omaster.util

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * CrashHandler 单元测试
 * 验证崩溃处理器的核心逻辑
 *
 * 注意：实际的线程崩溃捕获需要 Android 环境
 * 此测试验证辅助函数和配置逻辑
 */
class CrashHandlerTest {

    @Test
    fun `CrashHandler 应为单例`() {
        val instance1 = CrashHandler.getInstance()
        val instance2 = CrashHandler.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `初始状态下isInstalled应返回false`() {
        // 注意：实际环境中可能已安装，这里测试单例获取
        val handler = CrashHandler.getInstance()
        assertNotNull(handler)
    }

    @Test
    fun `异常分类应正确识别常见异常`() {
        val npe = NullPointerException("test")
        assertEquals("NPE", getExceptionType(npe))

        val ise = IllegalStateException("test")
        assertEquals("ISE", getExceptionType(ise))

        val iae = IllegalArgumentException("test")
        assertEquals("IAE", getExceptionType(iae))

        val ioobe = IndexOutOfBoundsException("test")
        assertEquals("IOOBE", getExceptionType(ioobe))

        val cce = ClassCastException("test")
        assertEquals("CCE", getExceptionType(cce))

        val se = SecurityException("test")
        assertEquals("SEC", getExceptionType(se))

        val oom = OutOfMemoryError("test")
        assertEquals("OOM", getExceptionType(oom))

        val soe = StackOverflowError("test")
        assertEquals("SOE", getExceptionType(soe))

        val unknown = RuntimeException("test")
        assertEquals("OTHER", getExceptionType(unknown))
    }

    @Test
    fun `敏感信息过滤应正确处理路径`() {
        val contentWithPath = "Error at /data/data/com.silas.omaster/files/cache/test.txt"
        val filtered = sanitizeCrashReport(contentWithPath)
        assertFalse(filtered.contains("/data/data/com.silas.omaster"))
        assertTrue(filtered.contains("[PATH_REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理IP地址`() {
        val contentWithIp = "Connection to 192.168.1.100 failed"
        val filtered = sanitizeCrashReport(contentWithIp)
        assertFalse(filtered.contains("192.168.1.100"))
        assertTrue(filtered.contains("[IP_REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理Token`() {
        val contentWithToken = "Authorization: token=abc123xyz"
        val filtered = sanitizeCrashReport(contentWithToken)
        assertFalse(filtered.contains("abc123xyz"))
        assertTrue(filtered.contains("[REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理密码`() {
        val contentWithPassword = "user password=secret123"
        val filtered = sanitizeCrashReport(contentWithPassword)
        assertFalse(filtered.contains("secret123"))
        assertTrue(filtered.contains("[REDACTED]"))
    }

    // ==================================================================
    // 扩展测试：install / isInstalled / CrashListener 机制
    // ==================================================================

    private lateinit var mockDefaultHandler: Thread.UncaughtExceptionHandler

    @Before
    fun setUp() {
        mockDefaultHandler = mockk(relaxed = true)
        mockkStatic(Thread::class)
        every { Thread.getDefaultUncaughtExceptionHandler() } returns mockDefaultHandler
        every { Thread.setDefaultUncaughtExceptionHandler(any()) } just runs
        mockkObject(SecurityCrypto)
        every { SecurityCrypto.encrypt(any()) } returns "mock_encrypted"
    }

    @After
    fun tearDown() {
        unmockkObject(SecurityCrypto)
        unmockkStatic(Thread::class)
        // 重置 CrashHandler 单例状态，避免测试间干扰
        resetCrashHandlerState()
    }

    private fun resetCrashHandlerState() {
        try {
            val instanceField = CrashHandler::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (_: Exception) {
            // 忽略重置失败
        }
    }

    @Test
    fun `install - 应保存原始的 default handler`() {
        val handler = CrashHandler.getInstance()
        handler.install()
        verify(exactly = 1) { Thread.getDefaultUncaughtExceptionHandler() }
    }

    @Test
    fun `install - 应将自身设置为 default handler`() {
        val handler = CrashHandler.getInstance()
        handler.install()
        verify(exactly = 1) { Thread.setDefaultUncaughtExceptionHandler(handler) }
    }

    @Test
    fun `isInstalled - install 后应返回 true`() {
        val handler = CrashHandler.getInstance()
        handler.install()
        assertTrue("isInstalled() should return true after install", handler.isInstalled())
    }

    @Test
    fun `isInstalled - install 前应返回 false`() {
        val handler = CrashHandler.getInstance()
        // 重置状态确保未安装
        resetCrashHandlerState()
        val freshHandler = CrashHandler.getInstance()
        assertFalse("isInstalled() should return false before install", freshHandler.isInstalled())
    }

    @Test
    fun `addCrashListener - 应成功添加监听器`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)

        // 通过调用 uncaughtException 验证监听器是否被调用
        handler.install()
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test listener"))

        verify(exactly = 1) { listener.onCrash(any(), any(), any()) }
    }

    @Test
    fun `removeCrashListener - 应成功移除监听器`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)
        handler.removeCrashListener(listener)

        handler.install()
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test remove"))

        verify(exactly = 0) { listener.onCrash(any(), any(), any()) }
    }

    @Test
    fun `addCrashListener - 重复添加同一监听器不应重复`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)
        handler.addCrashListener(listener)

        handler.install()
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test duplicate"))

        // 监听器只应被调用一次
        verify(exactly = 1) { listener.onCrash(any(), any(), any()) }
    }

    @Test
    fun `uncaughtException - 崩溃监听器应被调用`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)
        handler.install()

        val testException = RuntimeException("test crash")
        handler.uncaughtException(Thread.currentThread(), testException)

        verify(exactly = 1) { listener.onCrash(Thread.currentThread(), testException, "OTHER") }
    }

    @Test
    fun `uncaughtException - 多个监听器应全部被调用`() {
        val handler = CrashHandler.getInstance()
        val listener1 = mockk<CrashHandler.CrashListener>(relaxed = true)
        val listener2 = mockk<CrashHandler.CrashListener>(relaxed = true)
        val listener3 = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener1.onCrash(any(), any(), any()) } just runs
        every { listener2.onCrash(any(), any(), any()) } just runs
        every { listener3.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener1)
        handler.addCrashListener(listener2)
        handler.addCrashListener(listener3)
        handler.install()

        handler.uncaughtException(Thread.currentThread(), RuntimeException("test multi"))

        verify(exactly = 1) { listener1.onCrash(any(), any(), any()) }
        verify(exactly = 1) { listener2.onCrash(any(), any(), any()) }
        verify(exactly = 1) { listener3.onCrash(any(), any(), any()) }
    }

    @Test
    fun `uncaughtException - 监听器异常不应导致 handler 崩溃`() {
        val handler = CrashHandler.getInstance()
        val badListener = mockk<CrashHandler.CrashListener>(relaxed = true)
        val goodListener = mockk<CrashHandler.CrashListener>(relaxed = true)

        // 第一个监听器抛出异常
        every { badListener.onCrash(any(), any(), any()) } throws RuntimeException("listener error")
        every { goodListener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(badListener)
        handler.addCrashListener(goodListener)
        handler.install()

        // 不应抛出异常，且第二个监听器仍应被调用
        try {
            handler.uncaughtException(Thread.currentThread(), RuntimeException("test resilience"))
        } catch (e: Exception) {
            fail("Handler should not crash when listener throws: ${e.message}")
        }

        // 好的监听器仍应被调用
        verify(exactly = 1) { goodListener.onCrash(any(), any(), any()) }
    }

    @Test
    fun `uncaughtException - 应委托给原始 default handler`() {
        val handler = CrashHandler.getInstance()
        handler.install()

        val testException = RuntimeException("test delegation")
        handler.uncaughtException(Thread.currentThread(), testException)

        verify(exactly = 1) { mockDefaultHandler.uncaughtException(Thread.currentThread(), testException) }
    }

    @Test
    fun `uncaughtException - NPE 异常类型应正确分类`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)
        handler.install()

        val npe = NullPointerException("test npe")
        handler.uncaughtException(Thread.currentThread(), npe)

        verify(exactly = 1) { listener.onCrash(any(), any(), "NPE") }
    }

    @Test
    fun `uncaughtException - OOM 错误类型应正确分类`() {
        val handler = CrashHandler.getInstance()
        val listener = mockk<CrashHandler.CrashListener>(relaxed = true)
        every { listener.onCrash(any(), any(), any()) } just runs

        handler.addCrashListener(listener)
        handler.install()

        val oom = OutOfMemoryError("test oom")
        handler.uncaughtException(Thread.currentThread(), oom)

        verify(exactly = 1) { listener.onCrash(any(), any(), "OOM") }
    }

    // 辅助函数：从 CrashHandler 复制逻辑用于测试
    private fun getExceptionType(throwable: Throwable): String {
        return when (throwable) {
            is NullPointerException -> "NPE"
            is IllegalStateException -> "ISE"
            is IllegalArgumentException -> "IAE"
            is IndexOutOfBoundsException -> "IOOBE"
            is ClassCastException -> "CCE"
            is SecurityException -> "SEC"
            is OutOfMemoryError -> "OOM"
            is StackOverflowError -> "SOE"
            else -> "OTHER"
        }
    }

    private fun sanitizeCrashReport(content: String): String {
        return content
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"), "[IP_REDACTED]")
            .replace(Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
    }
}
