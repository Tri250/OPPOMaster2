package com.silas.omaster.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.PillNavBar
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.theme.OMasterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 关键 UI 组件测试
 *
 * 覆盖：
 * 1. PillNavBar 显示四个导航项、选中态、点击反馈
 * 2. PresetCard 显示预设信息、收藏按钮可点击、contentDescription 完整
 * 3. 收藏/取消收藏状态切换
 */
@RunWith(AndroidJUnit4::class)
class ComponentsUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pillNavBar_displaysAllItems() {
        composeTestRule.setContent {
            OMasterTheme {
                PillNavBar(
                    visible = true,
                    currentRoute = "home",
                    onNavigate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("精选推荐").assertIsDisplayed()
        composeTestRule.onNodeWithText("核心功能").assertIsDisplayed()
        composeTestRule.onNodeWithText("关于").assertIsDisplayed()
    }

    @Test
    fun pillNavBar_clickItem_triggersNavigation() {
        var clickedRoute: String? = null

        composeTestRule.setContent {
            OMasterTheme {
                PillNavBar(
                    visible = true,
                    currentRoute = "home",
                    onNavigate = { clickedRoute = it }
                )
            }
        }

        composeTestRule.onNodeWithText("关于").performClick()
        assert(clickedRoute == "about")
    }

    @Test
    fun presetCard_displaysPresetInfo_andFavoriteClickable() {
        val preset = MasterPreset(
            id = "test-preset-1",
            name = "测试预设",
            coverPath = "",
            author = "测试作者",
            isFavorite = false
        )

        composeTestRule.setContent {
            OMasterTheme {
                PresetCard(
                    preset = preset,
                    onClick = {},
                    onFavoriteClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("测试预设").assertIsDisplayed()
        composeTestRule.onNodeWithText("测试作者").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("收藏").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun presetCard_favoriteClick_togglesState() {
        var favoriteClicked = false
        val preset = MasterPreset(
            id = "test-preset-2",
            name = "可收藏预设",
            coverPath = "",
            author = "作者",
            isFavorite = false
        )

        composeTestRule.setContent {
            OMasterTheme {
                PresetCard(
                    preset = preset,
                    onClick = {},
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("收藏").performClick()
        assert(favoriteClicked)
    }
}
