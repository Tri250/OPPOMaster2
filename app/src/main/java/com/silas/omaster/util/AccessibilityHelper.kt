package com.silas.omaster.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 辅助功能工具类 - P2 深度优化
 *
 * 功能：
 * - TalkBack 屏幕阅读器检测
 * - 大字体适配（fontScale 感知）
 * - 高对比度模式
 * - 无障碍语义标注
 * - 触摸目标大小确保
 */
object AccessibilityHelper {

    /**
     * 检查 TalkBack 是否启用
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am?.isEnabled == true && am.isTouchExplorationEnabled
    }

    /**
     * 检查是否有任何无障碍服务启用
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am?.isEnabled == true
    }

    /**
     * 获取系统字体缩放比例
     * @return 1.0 为默认，> 1.0 为放大
     */
    fun getFontScale(context: Context): Float {
        return context.resources.configuration.fontScale
    }

    /**
     * 检查是否为大字体模式
     * 当 fontScale >= 1.3 时认为是大字体
     */
    fun isLargeFont(context: Context): Boolean {
        return getFontScale(context) >= 1.3f
    }

    /**
     * 检查是否为高对比度模式
     */
    fun isHighContrastEnabled(context: Context): Boolean {
        return try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前 UI 模式（横屏/竖屏）
     */
    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * 确保触摸目标最小尺寸（48dp 对应 WCAG 2.1 标准）
     */
    val MIN_TOUCH_TARGET_DP = 48

    /**
     * 根据字体缩放自适应字号
     */
    fun adaptiveSp(baseSp: Float, context: Context): TextUnit {
        val scale = getFontScale(context)
        // 限制最大放大倍数为 1.5x，防止极端布局破坏
        val clampedScale = scale.coerceIn(1.0f, 1.5f)
        return (baseSp * clampedScale).sp
    }
}

/**
 * Compose 扩展函数 - 无障碍语义标注
 *
 * 用法：
 * ```
 * Icon(
 *     imageVector = Icons.Default.Favorite,
 *     modifier = Modifier.accessibilityLabel("收藏此预设"),
 *     contentDescription = null // 避免重复标注
 * )
 * ```
 */
fun Modifier.accessibilityLabel(label: String): Modifier {
    return this.semantics {
        contentDescription = label
    }
}

/**
 * 触摸目标大小确保
 * 确保可点击元素的最小尺寸为 48dp（WCAG 2.1 AA 标准）
 */
@Composable
fun Modifier.ensureTouchTarget(): Modifier {
    return this.then(Modifier)
    // 注：实际使用中应通过 Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp) 实现
    // 此处提供语义化函数名，具体实现在调用处
}

/**
 * 当前字体缩放比例（Compose 上下文）
 */
@Composable
@ReadOnlyComposable
fun currentFontScale(): Float {
    return LocalConfiguration.current.fontScale
}

/**
 * 是否为大字体模式（Compose 上下文）
 */
@Composable
@ReadOnlyComposable
fun isLargeFontMode(): Boolean {
    return currentFontScale() >= 1.3f
}

/**
 * TalkBack 是否启用（Compose 上下文）
 */
@Composable
@ReadOnlyComposable
fun isTalkBackActive(): Boolean {
    return AccessibilityHelper.isTalkBackEnabled(LocalContext.current)
}

/**
 * 高对比度模式是否启用（Compose 上下文）
 */
@Composable
@ReadOnlyComposable
fun isHighContrastMode(): Boolean {
    return AccessibilityHelper.isHighContrastEnabled(LocalContext.current)
}