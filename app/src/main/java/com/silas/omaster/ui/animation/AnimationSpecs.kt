package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * 全局动画配置 - Web风格
 * 融合Web展示页面的流畅动画效果
 */
object AnimationSpecs {

    // ========== Web风格动画时长 ==========
    // 快速响应 - 按钮点击、图标变化
    val FastTween = tween<Float>(
        durationMillis = 100,
        easing = FastOutSlowInEasing
    )

    // 标准过渡 - 页面切换、内容显示
    val NormalTween = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // 慢速强调 - 卡片入场、重要提示
    val SlowTween = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // Web风格淡入 - 页面内容入场
    val WebFadeIn = tween<Float>(
        durationMillis = 400,
        easing = LinearOutSlowInEasing
    )

    // Web风格滑入 - 从下方滑入
    val WebSlideIn = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // Web风格缩放 - 悬停效果
    val WebScaleHover = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    // Web风格按压 - 点击反馈
    val WebPressScale = tween<Float>(
        durationMillis = 100,
        easing = LinearEasing
    )

    // ========== 弹性动画 ==========
    // 列表项入场 - 轻量级
    val ListItemSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.01f
    )

    // 卡片弹性 - 微弹性
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.001f
    )

    // 按钮弹性 - 明显弹性
    val ButtonSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )

    // ========== 淡入淡出 ==========
    val FadeInSpec = tween<Float>(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )

    val FadeOutSpec = tween<Float>(
        durationMillis = 150,
        easing = FastOutLinearInEasing
    )

    // ========== 滑动动画 ==========
    val SlideSpec = tween<Int>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // ========== 缩放动画 ==========
    val ScaleSpec = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    // ========== 错开延迟 ==========
    // Web风格：更流畅的错开效果
    const val StaggerDelayMillis = 30
    const val MaxStaggerDelayMillis = 200

    // 自动播放间隔
    const val AutoPlayIntervalMillis = 3000L

    // 页面切换时长
    const val PageTransitionMillis = 300

    // ========== Web风格动画曲线 ==========
    // 模拟framer-motion的easeOut曲线
    val WebEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // 模拟framer-motion的easeInOut曲线
    val WebEaseInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    // ========== Web风格入场动画组合 ==========
    // 卡片入场：淡入 + 缩放 + 上移
    fun webCardEnterAnimation(
        alpha: Animatable<Float>,
        scale: Animatable<Float>,
        translationY: Animatable<Float>
    ) {
        alpha.animateTo(1f, WebFadeIn)
        scale.animateTo(1f, tween(400, easing = WebEaseOut))
        translationY.animateTo(0f, tween(400, easing = WebEaseOut))
    }
}

/**
 * 记住动画状态的便捷函数
 */
@Composable
fun rememberAnimatable(initialValue: Float = 0f): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(initialValue) }
}

/**
 * 计算列表项错开延迟 - Web风格
 */
fun calculateStaggerDelay(index: Int, visibleStartIndex: Int): Int {
    val relativeIndex = (index - visibleStartIndex).coerceAtLeast(0)
    return (relativeIndex * AnimationSpecs.StaggerDelayMillis)
        .coerceAtMost(AnimationSpecs.MaxStaggerDelayMillis)
}

/**
 * 列表项动画放置规格
 */
val ListItemPlacementSpec: SpringSpec<IntOffset> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/**
 * 列表项淡入规格 - Web风格
 */
val ListItemFadeInSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = 0.01f
)

/**
 * Web风格悬停动画状态
 */
@Composable
fun rememberHoverState(): HoverAnimationState {
    return remember { HoverAnimationState() }
}

class HoverAnimationState {
    var isHovered by mutableStateOf(false)
    val scale: Float by mutableStateOf(if (isHovered) 1.05f else 1f)
}

/**
 * Web风格按压动画状态
 */
@Composable
fun rememberPressState(): PressAnimationState {
    return remember { PressAnimationState() }
}

class PressAnimationState {
    var isPressed by mutableStateOf(false)
    val scale: Float by mutableStateOf(if (isPressed) 0.95f else 1f)
}

// ========== 导入mutableStateOf ==========
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue