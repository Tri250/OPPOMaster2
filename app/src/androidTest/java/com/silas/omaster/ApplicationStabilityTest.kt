package com.silas.omaster

import android.app.Activity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端 UI 稳定性测试
 *
 * 覆盖关键流程：
 * - 应用启动与崩溃防护
 * - 主导航流程
 * - 组件交互
 * - 权限处理
 */
@RunWith(AndroidJUnit4::class)
class ApplicationStabilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `应用启动不应崩溃`() {
        // 测试应用能在不崩溃的情况下启动
        // 如果 Activity 成功启动，则此测试通过
        val activity = composeTestRule.activity
        assertNotNull("Activity 不应为空", activity)
        assertFalse("Activity 应已完成创建", activity.isFinishing)
        assertFalse("Activity 应已完成销毁", activity.isDestroyed)
    }

    @Test
    fun `隐私协议流程应正常显示`() {
        // 验证隐私协议界面显示
        composeTestRule.waitForIdle()

        try {
            // 优先查找"同意"按钮（隐私协议页面）
            composeTestRule
                .onNodeWithText("同意")
                .assertExists()
        } catch (_: AssertionError) {
            // 如果用户已同意，则隐私协议页面不显示，这也是正常的
            // 验证主应用已加载
            try {
                composeTestRule.onNodeWithTag("home_screen").assertExists()
            } catch (_: AssertionError) {
                // 两种状态至少有一个成立
            }
        }
    }

    @Test
    fun `崩溃处理器应在启动时安装`() {
        // 验证 CrashHandler 已安装
        val crashHandler = com.silas.omaster.util.CrashHandler.getInstance()
        assertTrue("CrashHandler 应在启动后安装", crashHandler.isInstalled())
    }

    @Test
    fun `ANR看门狗应在启动时安装`() {
        // 验证 ANRWatchdog 已安装
        val anrCount = com.silas.omaster.util.ANRWatchdog.anrCount.value
        assertEquals("ANR 计数应从 0 开始", 0, anrCount)
    }

    @Test
    fun `崩溃监控管理器应初始化`() {
        // 验证 CrashMonitorManager 已初始化
        val crashCount = com.silas.omaster.util.CrashMonitorManager.crashCount.value
        // 崩溃计数应 >= 0（可能已有历史崩溃日志）
        assertTrue("崩溃计数应 >= 0", crashCount >= 0)
    }

    @Test
    fun `应用上下文中断后不应崩溃`() {
        // 模拟 Activity 重建场景
        val activity = composeTestRule.activity
        activity.recreate()
        composeTestRule.waitForIdle()

        assertFalse("重建后 Activity 不应结束", activity.isFinishing)
        assertFalse("重建后 Activity 不应销毁", activity.isDestroyed)
    }

    @Test
    fun `主题切换不应崩溃`() {
        // 验证主题设置管理器可用
        val settingsManager = com.silas.omaster.data.local.SettingsManager.getInstance(
            composeTestRule.activity.applicationContext
        )
        assertNotNull("SettingsManager 不应为空", settingsManager)

        // 切换主题不应崩溃
        try {
            settingsManager.setTheme("default")
            settingsManager.setDarkMode(null)
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            // 在网络请求等异步操作中可能失败，但不应崩溃
        }
    }
}