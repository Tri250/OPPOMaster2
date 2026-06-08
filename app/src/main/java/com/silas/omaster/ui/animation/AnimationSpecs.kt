package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * ============================================
 * 小O帮帮 动画系统 - ColorOS 16 规范
 * 2026 企业级设计标准
 * ============================================
 */

object AnimationSpecs {

    /**
     * ============================================
     * 动画时长 - ColorOS 16 规范
     * ============================================
     */
    
    /** 瞬间动画 - 用于即时反馈 */
    const val DurationInstant = 50
    
    /** 快速动画 - 用于微交互 */
    const val DurationFast = 100
    
    /** 标准动画 - 用于一般过渡 */
    const val DurationNormal = 200
    
    /** 慢速动画 - 用于强调效果 */
    const val DurationSlow = 300
    
    /** 更慢动画 - 用于大型元素 */
    const val DurationSlower = 400
    
    /** 最慢动画 - 用于页面切换 */
    const val DurationSlowest = 500

    /**
     * ============================================
     * 动画曲线 - ColorOS 16 弹性曲线
     * ============================================
     */
    
    /** 线性曲线 */
    val EaseLinear = LinearEasing
    
    /** 入场曲线 */
    val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    
    /** 出场曲线 */
    val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    
    /** 入场出场曲线 */
    val EaseInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    
    /** 弹性效果 - ColorOS 16 标准弹性 */
    val EaseSpring = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)
    
    /** 柔和弹性 */
    val EaseSpringSoft = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    
    /** 强弹性 */
    val EaseSpringBouncy = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)
    
    /** 平滑过渡 */
    val EaseSmooth = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    
    /** 液态流动 - ColorOS 16 核心曲线 */
    val EaseLiquid = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /**
     * ============================================
     * Tween 动画规格
     * ============================================
     */
    
    /** 快速动画 */
    val FastTween = tween<Float>(
        durationMillis = DurationFast,
        easing = EaseOut
    )
    
    /** 标准动画 */
    val NormalTween = tween<Float>(
        durationMillis = DurationNormal,
        easing = EaseInOut
    )
    
    /** 慢速动画 */
    val SlowTween = tween<Float>(
        durationMillis = DurationSlow,
        easing = EaseLiquid
    )
    
    /** 液态动画 */
    val LiquidTween = tween<Float>(
        durationMillis = DurationSlow,
        easing = EaseLiquid
    )
    
    /** 弹性动画 */
    val SpringTween = tween<Float>(
        durationMillis = DurationSlow,
        easing = EaseSpring
    )
    
    /** 柔和弹性动画 */
    val SpringSoftTween = tween<Float>(
        durationMillis = DurationSlow,
        easing = EaseSpringSoft
    )
    
    /** 平滑动画 */
    val SmoothTween = tween<Float>(
        durationMillis = DurationNormal,
        easing = EaseSmooth
    )

    /**
     * ============================================
     * Spring 动画规格
     * ============================================
     */
    
    /** 列表项弹性 */
    val ListItemSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.01f
    )
    
    /** 卡片弹性 */
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )
    
    /** 液态弹性 */
    val LiquidSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = 0.001f
    )
    
    /** 强弹性 */
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )

    /**
     * ============================================
     * 特定动画规格
     * ============================================
     */
    
    /** 淡入动画 */
    val FadeInSpec = tween<Float>(
        durationMillis = DurationNormal,
        easing = EaseLiquid
    )
    
    /** 淡出动画 */
    val FadeOutSpec = tween<Float>(
        durationMillis = DurationFast,
        easing = EaseIn
    )
    
    /** 滑动动画 */
    val SlideSpec = tween<Int>(
        durationMillis = DurationSlow,
        easing = EaseLiquid
    )
    
    /** 缩放动画 */
    val ScaleSpec = tween<Float>(
        durationMillis = DurationNormal,
        easing = EaseSpringSoft
    )
    
    /** 液态滑动动画 */
    val LiquidSlideSpec = tween<IntOffset>(
        durationMillis = DurationSlow,
        easing = EaseLiquid
    )

    /**
     * ============================================
     * 列表项动画参数
     * ============================================
     */
    
    /** 列表项错开延迟基准值 */
    const val StaggerDelayMillis = 20
    
    /** 列表项最大延迟 */
    const val MaxStaggerDelayMillis = 150
    
    /** 自动播放间隔 */
    const val AutoPlayIntervalMillis = 3000L
    
    /** 页面切换动画时长 */
    const val PageTransitionMillis = DurationSlow
    
    /** 液态呼吸动画周期 */
    const val LiquidBreatheDuration = 3000
    
    /** 液态脉冲动画周期 */
    const val LiquidPulseDuration = 1500
    
    /** 液态浮动动画周期 */
    const val LiquidFloatDuration = 2000
}

/**
 * ============================================
 * 液态动画效果
 * ============================================
 */

/** 液态流动动画 */
@Composable
fun liquidFlowAnimation(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}

/** 弹性进入动画 */
@Composable
fun springInAnimation(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}

/** 液态淡入动画 */
@Composable
fun liquidFadeAnimation(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}

/**
 * 记住动画状态的便捷函数
 */
@Composable
fun rememberAnimatable(initialValue: Float = 0f): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(initialValue) }
}

/**
 * 计算列表项错开延迟
 * @param index 列表项索引
 * @param visibleStartIndex 可见区域起始索引
 * @return 延迟毫秒数
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
 * 列表项淡入规格
 */
val ListItemFadeInSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = 0.01f
)

/**
 * 液态卡片放置规格
 */
val LiquidCardPlacementSpec: SpringSpec<IntOffset> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/**
 * ============================================
 * 悬停效果参数
 * ============================================
 */
object HoverEffects {
    /** 悬停缩放比例 */
    const val ScaleMultiplier = 1.02f
    
    /** 悬停位移 */
    const val TranslationY = -2f
    
    /** 按压缩放比例 */
    const val PressScaleMultiplier = 0.98f
    
    /** 强按压缩放比例 */
    const val PressScaleStrong = 0.95f
}

/**
 * ============================================
 * 液态玻璃效果参数
 * ============================================
 */
object GlassEffects {
    /** 标准模糊半径 */
    const val BlurStandard = 20f
    
    /** 重度模糊半径 */
    const val BlurHeavy = 40f
    
    /** 饱和度增强 */
    const val SaturationBoost = 1.8f
    
    /** 亮度增强 */
    const val BrightnessBoost = 1.05f
}