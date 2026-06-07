package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * =====================================================
 * OMaster 设计系统 - 动画效果
 * =====================================================
 * 动画风格：framer-motion风格，流畅优雅
 * 参考：https://github.com/Tri250/OPPOMaster
 */

// ==================== 动画时长 ====================

object AnimationDuration {
    /** 快速动画 - 100ms */
    const val Fast = 100
    
    /** 正常动画 - 200ms */
    const val Normal = 200
    
    /** 慢速动画 - 300ms */
    const val Slow = 300
    
    /** 页面进入动画 - 800ms */
    const val PageEnter = 800
    
    /** 卡片悬停 - 300ms */
    const val CardHover = 300
    
    /** 图片缩放 - 500ms */
    const val ImageScale = 500
    
    /** 按钮点击 - 100ms */
    const val ButtonTap = 100
}

// ==================== 动画曲线 ====================

object AnimationEasing {
    /** 标准缓动 - ease-out */
    val Standard = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /** 进入缓动 - ease-out */
    val Enter = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /** 退出缓动 - ease-in */
    val Exit = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    /** 弹性缓动 - spring-like */
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    
    /** 平滑缓动 - linear-ish */
    val Smooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
}

// ==================== 预设动画规格 ====================

/**
 * Fade In + Y位移 - 用于页面进入
 * framer-motion: initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
 */
@Composable
fun fadeInUpSpec(): FiniteAnimationSpec<Float> {
    return remember {
        tween<Float>(
            durationMillis = AnimationDuration.PageEnter,
            easing = AnimationEasing.Enter
        )
    }
}

/**
 * Scale In - 用于卡片进入
 * framer-motion: initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}
 */
@Composable
fun scaleInSpec(): FiniteAnimationSpec<Float> {
    return remember {
        tween<Float>(
            durationMillis = AnimationDuration.Slow,
            easing = AnimationEasing.Bounce
        )
    }
}

/**
 * Hover Y位移 - 用于卡片悬停
 * framer-motion: whileHover={{ y: -8 }}
 */
@Composable
fun hoverYSpec(): FiniteAnimationSpec<Float> {
    return remember {
        tween<Float>(
            durationMillis = AnimationDuration.CardHover,
            easing = AnimationEasing.Smooth
        )
    }
}

/**
 * 按钮点击缩放
 * framer-motion: whileTap={{ scale: 0.95 }}
 */
@Composable
fun tapScaleSpec(): FiniteAnimationSpec<Float> {
    return remember {
        tween<Float>(
            durationMillis = AnimationDuration.ButtonTap,
            easing = AnimationEasing.Standard
        )
    }
}

/**
 * 图片悬停缩放
 * framer-motion: group-hover:scale-105 transition-transform duration-500
 */
@Composable
fun imageHoverScaleSpec(): FiniteAnimationSpec<Float> {
    return remember {
        tween<Float>(
            durationMillis = AnimationDuration.ImageScale,
            easing = AnimationEasing.Smooth
        )
    }
}

// ==================== 动画状态 ====================

/**
 * 进入动画状态
 * opacity: 0 -> 1, y: 20 -> 0
 */
data class EnterAnimationState(
    val opacity: Float = 0f,
    val offsetY: Float = 20f
)

/**
 * 卡片悬停状态
 * y: 0 -> -8
 */
data class HoverAnimationState(
    val offsetY: Float = 0f
)

/**
 * 按钮点击状态
 * scale: 1 -> 0.95
 */
data class TapAnimationState(
    val scale: Float = 1f
)

// ==================== 动画扩展函数 ====================

/**
 * 卡片悬停效果
 * translateY(-8px) on hover
 */
fun Modifier.cardHoverAnimation(hovered: Boolean): Modifier {
    return graphicsLayer {
        val targetY = if (hovered) -8f else 0f
        translationY = targetY
    }
}

/**
 * 按钮点击效果
 * scale(0.95) on tap
 */
fun Modifier.buttonTapAnimation(pressed: Boolean): Modifier {
    return graphicsLayer {
        val targetScale = if (pressed) 0.95f else 1f
        scaleX = targetScale
        scaleY = targetScale
        transformOrigin = TransformOrigin.Center
    }
}

/**
 * 图片悬停缩放
 * scale(1.05) on hover
 */
fun Modifier.imageHoverAnimation(hovered: Boolean): Modifier {
    return graphicsLayer {
        val targetScale = if (hovered) 1.05f else 1f
        scaleX = targetScale
        scaleY = targetScale
        transformOrigin = TransformOrigin.Center
    }
}

/**
 * 进入动画效果
 * opacity + y translation
 */
fun Modifier.enterAnimation(progress: Float): Modifier {
    return graphicsLayer {
        alpha = progress
        translationY = 20f * (1f - progress)
    }
}

/**
 * 缩放进入效果
 * opacity + scale
 */
fun Modifier.scaleEnterAnimation(progress: Float): Modifier {
    return graphicsLayer {
        alpha = progress
        val scale = 0.9f + 0.1f * progress
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin.Center
    }
}

// ==================== 动画Transition ====================

/**
 * 创建进入动画Transition
 */
@Composable
fun enterTransition(): Transition<EnterAnimationState> {
    return updateTransition(
        targetState = EnterAnimationState(opacity = 1f, offsetY = 0f),
        label = "enter"
    )
}

/**
 * 创建悬停动画Transition
 */
@Composable
fun hoverTransition(hovered: Boolean): Transition<HoverAnimationState> {
    return updateTransition(
        targetState = HoverAnimationState(offsetY = if (hovered) -8f else 0f),
        label = "hover"
    )
}

/**
 * 创建点击动画Transition
 */
@Composable
fun tapTransition(pressed: Boolean): Transition<TapAnimationState> {
    return updateTransition(
        targetState = TapAnimationState(scale = if (pressed) 0.95f else 1f),
        label = "tap"
    )
}

// ==================== 延迟动画 ====================

/**
 * 计算列表项动画延迟
 * index * 0.1s
 */
fun calculateStaggerDelay(index: Int, baseDelay: Int = 100): Int {
    return baseDelay * index
}

/**
 * 创建延迟动画规格
 */
@Composable
fun delayedAnimationSpec(delayMs: Int): FiniteAnimationSpec<Float> {
    return remember(delayMs) {
        tween<Float>(
            durationMillis = AnimationDuration.Slow,
            delayMillis = delayMs,
            easing = AnimationEasing.Enter
        )
    }
}