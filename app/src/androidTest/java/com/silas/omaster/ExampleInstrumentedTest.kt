package com.silas.omaster

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.silas.omaster", appContext.packageName)
    }
}

/**
 * UI 界面集成测试（需在 Android 设备/模拟器运行）
 *
 * 验证 Compose 界面基本渲染能力。
 * 由于 UI 组件大量依赖 ViewModel 与 Application 上下文，
 * 实际业务 UI 测试建议在具备设备环境后逐步补充。
 */
@RunWith(AndroidJUnit4::class)
class UiIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composeBasicTextRendering() {
        composeTestRule.setContent {
            androidx.compose.material3.Text(text = "OMaster")
        }

        composeTestRule.onNodeWithText("OMaster").assertIsDisplayed()
    }
}
