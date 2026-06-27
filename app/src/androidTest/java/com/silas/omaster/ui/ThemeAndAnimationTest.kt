package com.silas.omaster.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.animation.liquidGlassEffect
import com.silas.omaster.ui.theme.OMasterTheme
import com.silas.omaster.ui.theme.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI/UX、字体字号、动画规格、液态玻璃效果验收测试
 *
 * 覆盖：
 * 1. Material3 Typography 各层级字号、行高符合 ColorOS 16 / OMaster 规范
 * 2. 动画规格常量范围合理（Spring 刚度、Tween 时长）
 * 3. 液态玻璃 Modifier 可正常组合且不崩溃
 * 4. 主题切换后颜色正确
 */
@RunWith(AndroidJUnit4::class)
class ThemeAndAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typography_fontSizes_matchSpec() {
        // ColorOS 16 规范校验
        assertEquals(28f, Typography.displayLarge.fontSize.value, 0f)
        assertEquals(36f, Typography.displayLarge.lineHeight.value, 0f)

        assertEquals(22f, Typography.displayMedium.fontSize.value, 0f)
        assertEquals(30f, Typography.displayMedium.lineHeight.value, 0f)

        assertEquals(16f, Typography.bodyLarge.fontSize.value, 0f)
        assertEquals(24f, Typography.bodyLarge.lineHeight.value, 0f)

        assertEquals(14f, Typography.bodyMedium.fontSize.value, 0f)
        assertEquals(22f, Typography.bodyMedium.lineHeight.value, 0f)

        assertEquals(12f, Typography.bodySmall.fontSize.value, 0f)
        assertEquals(18f, Typography.bodySmall.lineHeight.value, 0f)

        assertEquals(11f, Typography.labelSmall.fontSize.value, 0f)
        assertEquals(16f, Typography.labelSmall.lineHeight.value, 0f)
    }

    @Test
    fun typography_labelSmall_hasMinimumLineHeight() {
        // 11sp 标签行高不应小于 16sp（可读性验收）
        assertTrue(Typography.labelSmall.lineHeight.value >= 16f)
    }

    @Test
    fun animationSpecs_durationsWithinReasonableRange() {
        // 微交互 150ms
        assertEquals(150, AnimationSpecs.FastTween.durationMillis)
        // 标准过渡 250ms
        assertEquals(250, AnimationSpecs.NormalTween.durationMillis)
        // 慢速强调 400ms
        assertEquals(400, AnimationSpecs.SlowTween.durationMillis)
        // 页面切换不应超过 500ms
        assertTrue(AnimationSpecs.PageTransitionMillis <= 500)
        // 错开延迟 20ms，最大 150ms
        assertEquals(20, AnimationSpecs.StaggerDelayMillis)
        assertEquals(150, AnimationSpecs.MaxStaggerDelayMillis)
    }

    @Test
    fun animationSpecs_springStiffnessNotZero() {
        listOf(
            AnimationSpecs.ListItemSpring,
            AnimationSpecs.CardSpring,
            AnimationSpecs.ColorOS16StandardSpring,
            AnimationSpecs.ColorOS16LiquidSpring
        ).forEach { spec ->
            assertTrue("Spring 刚度必须大于 0", spec.stiffness > 0f)
        }
    }

    @Test
    fun liquidGlassEffect_modifierComposes() {
        composeTestRule.setContent {
            OMasterTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .liquidGlassEffect()
                ) {
                    // 仅验证 Modifier 组合不崩溃
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun theme_appliesDarkBackgroundColor() {
        composeTestRule.setContent {
            OMasterTheme(darkMode = com.silas.omaster.data.local.DarkMode.DARK) {
                androidx.compose.foundation.layout.Box {
                    assertEquals(
                        android.graphics.Color.parseColor("#FF0A0A0A"),
                        MaterialTheme.colorScheme.background.value.toInt()
                    )
                }
            }
        }
    }
}
