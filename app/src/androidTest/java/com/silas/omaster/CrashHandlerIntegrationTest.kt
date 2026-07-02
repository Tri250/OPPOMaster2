package com.silas.omaster

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.util.CrashHandler
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 崩溃处理集成测试
 *
 * 验证 CrashHandler 与 CrashMonitorManager 的协作：
 * - 崩溃监听器正确注册和回调
 * - 双重注册冲突已解决
 * - 崩溃上报链路完整
 */
@RunWith(AndroidJUnit4::class)
class CrashHandlerIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `CrashHandler 应为唯一未捕获异常处理器`() {
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull("应有默认异常处理器", handler)
        assertTrue(
            "默认异常处理器应为 CrashHandler",
            handler is CrashHandler
        )
    }

    @Test
    fun `CrashHandler 监听器应正确回调`() {
        val latch = CountDownLatch(1)
        val crashHandler = CrashHandler.getInstance()

        val listener = object : CrashHandler.CrashListener {
            override fun onCrash(thread: Thread, throwable: Throwable, exceptionType: String) {
                assertEquals("OTHER", exceptionType)
                latch.countDown()
            }
        }

        crashHandler.addCrashListener(listener)

        // 模拟异常
        crashHandler.uncaughtException(
            Thread.currentThread(),
            RuntimeException("测试崩溃")
        )

        // 等待回调（最多 3 秒）
        val callbackInvoked = latch.await(3, TimeUnit.SECONDS)
        assertTrue("崩溃监听器应在 3 秒内回调", callbackInvoked)

        crashHandler.removeCrashListener(listener)
    }

    @Test
    fun `CrashMonitorManager 应作为监听器注册`() {
        val crashHandler = CrashHandler.getInstance()
        val crashMonitor = com.silas.omaster.util.CrashMonitorManager

        // 验证 CrashMonitorManager 实现了 CrashListener 接口
        assertTrue(
            "CrashMonitorManager 应实现 CrashListener",
            crashMonitor is CrashHandler.CrashListener
        )

        // 通过反射检查是否已注册为监听器
        val field = CrashHandler::class.java.getDeclaredField("crashListeners")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val listeners = field.get(crashHandler) as? List<*>
        assertNotNull("监听器列表不应为空", listeners)
        assertTrue(
            "CrashMonitorManager 应在监听器列表中",
            listeners.any { it === crashMonitor }
        )
    }

    @Test
    fun `多个监听器应全部被调用`() {
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        val crashHandler = CrashHandler.getInstance()

        val listener1 = object : CrashHandler.CrashListener {
            override fun onCrash(thread: Thread, throwable: Throwable, exceptionType: String) {
                latch1.countDown()
            }
        }
        val listener2 = object : CrashHandler.CrashListener {
            override fun onCrash(thread: Thread, throwable: Throwable, exceptionType: String) {
                latch2.countDown()
            }
        }

        crashHandler.addCrashListener(listener1)
        crashHandler.addCrashListener(listener2)

        crashHandler.uncaughtException(
            Thread.currentThread(),
            IllegalArgumentException("测试多监听器")
        )

        val allInvoked = latch1.await(3, TimeUnit.SECONDS) &&
                latch2.await(3, TimeUnit.SECONDS)
        assertTrue("所有崩溃监听器应被调用", allInvoked)

        crashHandler.removeCrashListener(listener1)
        crashHandler.removeCrashListener(listener2)
    }
}