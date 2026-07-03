package com.silas.omaster.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.ui.theme.OMasterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GlassButton 组件 UI 测试
 * 验证玻璃态按钮的显示和交互
 */
@RunWith(AndroidJUnit4::class)
class GlassButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GlassButton 应正确显示文字`() {
        composeTestRule.setContent {
            OMasterTheme {
                GlassButton(
                    text = "测试按钮",
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("测试按钮").assertIsDisplayed()
    }

    @Test
    fun `GlassButton 点击应触发回调`() {
        var clicked = false

        composeTestRule.setContent {
            OMasterTheme {
                GlassButton(
                    text = "点击测试",
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("点击测试").performClick()
        assert(clicked)
    }

    @Test
    fun `GlassSecondaryButton 应正确显示`() {
        composeTestRule.setContent {
            OMasterTheme {
                GlassSecondaryButton(
                    text = "次按钮",
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("次按钮").assertIsDisplayed()
    }

    @Test
    fun `GlassChip 应正确显示选中状态`() {
        composeTestRule.setContent {
            OMasterTheme {
                GlassChip(
                    text = "已选中",
                    selected = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("已选中").assertIsDisplayed()
    }

    @Test
    fun `GlassChip 应正确显示未选中状态`() {
        composeTestRule.setContent {
            OMasterTheme {
                GlassChip(
                    text = "未选中",
                    selected = false,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("未选中").assertIsDisplayed()
    }
}
