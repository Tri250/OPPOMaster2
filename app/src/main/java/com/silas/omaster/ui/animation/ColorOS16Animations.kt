package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * =====================================================
 * ColorOS 16 专业摄影设计规范 - 动画系统
 * =====================================================
 * 设计标准：Aquatic Design 水感动效
 * 动画曲线：基于Material Design 3 + ColorOS优化
 * 目标：流畅、自然、有生命力的交互体验
 */

object ColorOS16Animations {

    // ==================== 动画时长规范 ====================
    /**
     * 动画时长层级
     * 根据元素重要性和交互类型分配不同时长
     */
    object Duration {
        const val INSTANT = 50L      // 即时反馈
        const val FAST = 150L        // 快速反馈（按钮点击）
        const val NORMAL = 300L      // 标准过渡（页面切换）
        const val SLOW = 400L        // 慢速强调（卡片展开）
        const val DELIBERATE = 500L  // 刻意强调（重要提示）
    }

    // ==================== 动画曲线规范 ====================
    /**
     * ColorOS 16标准动画曲线
     * 模拟水流的自然物理特性
     */
    object Easing {
        /**
         * 标准曲线 - 大多数动画使用
         * cubic-bezier(0.4, 0.0, 0.2, 1)
         */
        val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

        /**
         * 减速曲线 - 元素入场
         * cubic-bezier(0.0, 0.0, 0.2, 1)
         */
        val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

        /**
         * 加速曲线 - 元素退场
         * cubic-bezier(0.4, 0.0, 1, 1)
         */
        val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

        /**
         * 弹性曲线 - 强调反馈
         * cubic-bezier(0.34, 1.56, 0.64, 1)
         */
        val Spring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

        /**
         * 线性 - 持续动画
         */
        val Linear = LinearEasing
    }

    // ==================== 页面过渡动画 ====================
    /**
     * 页面切换动画规格
     */
    val PageTransition = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Standard
    )

    /**
     * 页面内容入场
     */
    val PageContentEnter = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Decelerate
    )

    // ==================== 卡片动画 ====================
    /**
     * 卡片入场动画
     * 从下方滑入 + 淡入
     */
    val CardEnter = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Decelerate
    )

    /**
     * 卡片悬停效果
     * 轻微上浮 + 阴影增强
     */
    val CardHover = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    /**
     * 卡片点击反馈
     * 轻微缩放
     */
    val CardPress = tween<Float>(
        durationMillis = Duration.INSTANT.toInt(),
        easing = Easing.Standard
    )

    // ==================== 按钮动画 ====================
    /**
     * 按钮悬停
     */
    val ButtonHover = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    /**
     * 按钮点击
     * scale 0.95效果
     */
    val ButtonPress = tween<Float>(
        durationMillis = Duration.INSTANT.toInt(),
        easing = Easing.Standard
    )

    /**
     * 按钮释放
     */
    val ButtonRelease = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    // ==================== 列表动画 ====================
    /**
     * 列表项入场错开延迟
     */
    const val StaggerDelay = 50L
    const val MaxStaggerDelay = 300L

    /**
     * 计算列表项延迟
     */
    fun calculateStaggerDelay(index: Int, startIndex: Int = 0): Int {
        val relativeIndex = (index - startIndex).coerceAtLeast(0)
        return (relativeIndex * StaggerDelay).coerceAtMost(MaxStaggerDelay).toInt()
    }

    /**
     * 列表项入场
     */
    val ListItemEnter = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Decelerate
    )

    /**
     * 列表项放置动画（用于LazyColumn）
     */
    val ListItemPlacement = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    // ==================== 图标动画 ====================
    /**
     * 图标切换
     */
    val IconChange = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    /**
     * 图标悬停缩放
     */
    val IconHover = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    // ==================== 文字动画 ====================
    /**
     * 文字淡入
     */
    val TextFadeIn = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Decelerate
    )

    /**
     * 数字变化
     */
    val NumberChange = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Spring
    )

    // ==================== 特殊效果动画 ====================
    /**
     * 脉冲效果（用于HNCS徽章等）
     */
    val Pulse = tween<Float>(
        durationMillis = 2000,
        easing = Easing.Linear
    )

    /**
     * 闪光效果（用于新内容提示）
     */
    val Shimmer = tween<Float>(
        durationMillis = 1500,
        easing = Easing.Linear
    )

    /**
     * 呼吸效果（用于焦点提示）
     */
    val Breathe = tween<Float>(
        durationMillis = 3000,
        easing = Easing.Standard
    )

    // ==================== 导航动画 ====================
    /**
     * 底部导航栏显示/隐藏
     */
    val BottomNavSlide = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Standard
    )

    /**
     * 导航项切换
     */
    val NavItemSwitch = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    // ==================== 弹窗/面板动画 ====================
    /**
     * 底部面板滑入
     */
    val BottomSheetEnter = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Decelerate
    )

    /**
     * 对话框显示
     */
    val DialogEnter = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Decelerate
    )

    /**
     * 遮罩淡入
     */
    val ScrimFade = tween<Float>(
        durationMillis = Duration.NORMAL.toInt(),
        easing = Easing.Standard
    )

    // ==================== 摄影相关动画 ====================
    /**
     * 快门按钮按下
     */
    val ShutterPress = tween<Float>(
        durationMillis = Duration.INSTANT.toInt(),
        easing = Easing.Standard
    )

    /**
     * 参数调节滑块
     */
    val SliderMove = tween<Float>(
        durationMillis = Duration.FAST.toInt(),
        easing = Easing.Spring
    )

    /**
     * 预览图缩放
     */
    val PreviewZoom = tween<Float>(
        durationMillis = Duration.SLOW.toInt(),
        easing = Easing.Decelerate
    )
}

// ==================== 动画状态管理 ====================

/**
 * 按钮动画状态
 */
class ButtonAnimationState {
    var isPressed by mutableStateOf(false)
    var isHovered by mutableStateOf(false)

    val scale: Float by derivedStateOf {
        when {
            isPressed -> 0.95f
            isHovered -> 1.02f
            else -> 1.0f
        }
    }

    val alpha: Float by derivedStateOf {
        if (isPressed) 0.8f else 1.0f
    }
}

/**
 * 卡片动画状态
 */
class CardAnimationState {
    var isHovered by mutableStateOf(false)
    var isPressed by mutableStateOf(false)

    val scale: Float by derivedStateOf {
        when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1.0f
        }
    }

    val elevation: Float by derivedStateOf {
        when {
            isPressed -> 2f
            isHovered -> 8f
            else -> 4f
        }
    }

    val translationY: Float by derivedStateOf {
        if (isHovered) -4f else 0f
    }
}

/**
 * 列表项动画状态
 */
class ListItemAnimationState {
    var isVisible by mutableStateOf(false)
    var index by mutableStateOf(0)

    val delay: Int
        get() = ColorOS16Animations.calculateStaggerDelay(index)
}

/**
 * 页面动画状态
 */
class PageAnimationState {
    var isEntering by mutableStateOf(true)

    val enterProgress: Float by derivedStateOf {
        if (isEntering) 1f else 0f
    }
}

// ==================== 便捷函数 ====================

/**
 * 记住按钮动画状态
 */
@Composable
fun rememberButtonAnimationState(): ButtonAnimationState {
    return remember { ButtonAnimationState() }
}

/**
 * 记住卡片动画状态
 */
@Composable
fun rememberCardAnimationState(): CardAnimationState {
    return remember { CardAnimationState() }
}

/**
 * 记住列表项动画状态
 */
@Composable
fun rememberListItemAnimationState(index: Int = 0): ListItemAnimationState {
    return remember(index) {
        ListItemAnimationState().apply { this.index = index }
    }
}

/**
 * 记住页面动画状态
 */
@Composable
fun rememberPageAnimationState(): PageAnimationState {
    return remember { PageAnimationState() }
}

// ==================== 导入 ====================
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
