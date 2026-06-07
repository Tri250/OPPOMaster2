package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * =====================================================
 * OMaster 精致高端动画系统
 * =====================================================
 * 设计理念：流畅、精致、有深度
 * 动画曲线：细腻、优雅、自然
 * 目标：行业顶尖的交互动画体验
 */

object PremiumAnimations {

    // ==================== 动画时长规范 ====================
    object Duration {
        const val INSTANT = 30L      // 即时反馈
        const val FAST = 100L       // 快速响应
        const val NORMAL = 200L      // 标准过渡
        const val SLOW = 300L        // 慢速强调
        const val DELIBERATE = 400L  // 刻意强调
        const val EXQUISITE = 500L   // 精致延时
    }

    // ==================== 精致动画曲线 ====================
    object Easing {
        /**
         * 轻盈曲线 - 优雅入场
         * 用于：元素淡入、滑入
         */
        val Elegant = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

        /**
         * 流畅曲线 - 自然过渡
         * 用于：页面切换、内容变化
         */
        val Smooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

        /**
         * 弹性曲线 - 精致反馈
         * 用于：按钮点击、卡片悬停
         */
        val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

        /**
         * 轻盈曲线 - 快速响应
         * 用于：小元素变化
         */
        val Light = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

        /**
         * 线性 - 持续动画
         */
        val Linear = LinearEasing
    }

    // ==================== 入场动画 ====================
    
    /**
     * 精致淡入 - 柔和优雅
     */
    val ElegantFadeIn = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Elegant
    )

    /**
     * 精致滑入 - 从下方优雅升起
     */
    val ElegantSlideUp = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Elegant
    )

    /**
     * 精致缩放 - 优雅绽放
     */
    val ElegantScale = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Elegant
    )

    // ==================== 交互动画 ====================

    /**
     * 卡片悬停 - 精致上浮
     * 效果：translateY -2dp
     */
    val CardHover = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Bounce
    )

    /**
     * 卡片按压 - 精致反馈
     * 效果：scale 0.98
     */
    val CardPress = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Light
    )

    /**
     * 按钮按压 - 即时响应
     * 效果：scale 0.96
     */
    val ButtonPress = tween<Float>(
        durationMillis = Duration.INSTANT.toInt(),
        easing = Easing.Smooth
    )

    /**
     * 按钮释放 - 弹性恢复
     */
    val ButtonRelease = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Bounce
    )

    /**
     * 图标缩放 - 精致变化
     */
    val IconScale = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Bounce
    )

    // ==================== 列表动画 ====================

    /**
     * 列表项错开延迟 - 精致节奏
     */
    const val StaggerDelay = 30L
    const val MaxStaggerDelay = 200L

    /**
     * 计算错开延迟
     */
    fun calculateStaggerDelay(index: Int, startIndex: Int = 0): Int {
        val relativeIndex = (index - startIndex).coerceAtLeast(0)
        return (relativeIndex * StaggerDelay).coerceAtMost(MaxStaggerDelay).toInt()
    }

    /**
     * 列表项入场 - 精致依次出现
     */
    val ListItemEnter = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Elegant
    )

    /**
     * 列表项放置 - 精致移动
     */
    val ListItemPlacement = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    // ==================== 特殊效果动画 ====================

    /**
     * 脉冲效果 - 精致呼吸
     * 用于：认证徽章、焦点提示
     */
    val GentlePulse = tween<Float>(
        durationMillis = 2500,
        easing = Easing.Smooth
    )

    /**
     * 光晕效果 - 精致闪耀
     * 用于：新内容提示
     */
    val Shimmer = tween<Float>(
        durationMillis = 2000,
        easing = Easing.Linear
    )

    /**
     * 渐变效果 - 精致流动
     * 用于：背景渐变
     */
    val GradientFlow = tween<Float>(
        durationMillis = 3000,
        easing = Easing.Smooth
    )

    // ==================== 页面过渡动画 ====================

    /**
     * 页面切换 - 流畅自然
     */
    val PageTransition = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Smooth
    )

    /**
     * 页面内容 - 精致入场
     */
    val PageContentEnter = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Elegant
    )

    // ==================== 导航动画 ====================

    /**
     * 底部导航 - 流畅滑入
     */
    val BottomNavSlide = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Elegant
    )

    /**
     * 导航项切换 - 精致变化
     */
    val NavItemSwitch = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Elegant
    )
}

// ==================== 动画状态管理 ====================

/**
 * 精致按钮动画状态
 */
class PremiumButtonState {
    var isPressed by androidx.compose.runtime.mutableStateOf(false)
    var isHovered by androidx.compose.runtime.mutableStateOf(false)

    val scale: Float
        get() = when {
            isPressed -> 0.96f
            isHovered -> 1.02f
            else -> 1f
        }

    val alpha: Float
        get() = if (isPressed) 0.85f else 1f
}

/**
 * 精致卡片动画状态
 */
class PremiumCardState {
    var isHovered by androidx.compose.runtime.mutableStateOf(false)
    var isPressed by androidx.compose.runtime.mutableStateOf(false)

    val scale: Float
        get() = when {
            isPressed -> 0.98f
            isHovered -> 1.01f
            else -> 1f
        }

    val elevation: Float
        get() = when {
            isPressed -> 2f
            isHovered -> 6f
            else -> 4f
        }

    val translationY: Float
        get() = if (isHovered) -2f else 0f
}

/**
 * 精致图标动画状态
 */
class PremiumIconState {
    var isSelected by androidx.compose.runtime.mutableStateOf(false)

    val scale: Float
        get() = if (isSelected) 1.1f else 1f

    val alpha: Float
        get() = if (isSelected) 1f else 0.7f
}

// ==================== 便捷函数 ====================

@Composable
fun rememberPremiumButtonState(): PremiumButtonState {
    return remember { PremiumButtonState() }
}

@Composable
fun rememberPremiumCardState(): PremiumCardState {
    return remember { PremiumCardState() }
}

@Composable
fun rememberPremiumIconState(): PremiumIconState {
    return remember { PremiumIconState() }
}
