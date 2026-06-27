package com.silas.omaster.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.MainActivity
import com.silas.omaster.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 核心操作链路 UI 测试
 *
 * 覆盖：
 * 1. 应用冷启动后隐私政策/欢迎流程可交互
 * 2. 同意隐私政策后进入首页
 * 3. 底部导航栏四个主 Tab 可正常切换
 * 4. 不同意隐私政策仍可进入主界面（本地功能可用）
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunch_showsWelcomeDialog() {
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.welcome_dialog_title)
        ).assertIsDisplayed()
    }

    @Test
    fun disagreePrivacyPolicy_navigatesToHome() {
        val disagreeText = composeTestRule.activity.getString(R.string.disagree)
        composeTestRule.onNodeWithText(disagreeText).performClick()

        composeTestRule.waitForIdle()

        // 进入主界面后应展示首页标题或底部导航首页项
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.nav_home)
        ).assertIsDisplayed()
    }

    @Test
    fun agreePrivacyPolicy_navigatesToHome() {
        // 勾选同意
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.privacy_policy)
        ).performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.agree_and_start)
        ).performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.nav_home)
        ).assertIsDisplayed()
    }

    @Test
    fun bottomNav_switchesBetweenMainTabs() {
        // 先进入主界面
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.disagree)
        ).performClick()
        composeTestRule.waitForIdle()

        val tabs = listOf(
            R.string.nav_home,
            R.string.nav_featured,
            R.string.nav_core_features,
            R.string.nav_about
        )

        tabs.forEach { stringRes ->
            val label = composeTestRule.activity.getString(stringRes)
            composeTestRule.onNodeWithText(label).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }
}
