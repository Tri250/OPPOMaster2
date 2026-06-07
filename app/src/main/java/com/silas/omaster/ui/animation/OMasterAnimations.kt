package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * =====================================================
 * OMaster 设计系统 v2.0 - 动画系统
 * =====================================================
 * 动画风格：Framer Motion风格，流畅优雅
 * 
 * 动画库：Framer Motion
 * 滚动：平滑滚动 scroll-behavior: smooth
 * 交互：悬停效果、过渡动画
 */

// ==================== 动画时长 ====================

object AnimationDurations {
    /** 快速动画 - 100ms */
    const val FAST = 100
    
    /** 正常动画 - 200ms */
    const val NORMAL = 200
    
    /** 慢速动画 - 300ms */
    const val SLOW = 300
    
    /** 页面进入动画 - 500ms */
    const val PAGE_ENTER = 500
    
    /** 卡片悬停 - 300ms */
    const val CARD_HOVER = 300
    
    /** 图片缩放 - 400ms */
    const val IMAGE_SCALE = 400
    
    /** 按钮点击 - 100ms */
    const val BUTTON_TAP = 100
}

// ==================== 动画曲线 ====================

object AnimationEasings {
    /** 标准缓动 - ease-out */
    val Standard = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /** 进入缓动 */
    val Enter = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    /** 退出缓动 */
    val Exit = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    /** 弹性缓动 - spring-like */
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    
    /** 平滑缓动 */
    val Smooth = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    /** 线性 */
    val Linear = LinearEasing
}

// ==================== 动画规格 ====================

@Composable
fun <T> tweenFast(): TweenSpec<T> = remember {
    tween(
        durationMillis = AnimationDurations.FAST,
        easing = AnimationEasings.Standard
    )
}

@Composable
fun <T> tweenNormal(): TweenSpec<T> = remember {
    tween(
        durationMillis = AnimationDurations.NORMAL,
        easing = AnimationEasings.Smooth
    )
}

@Composable
fun <T> tweenSlow(): TweenSpec<T> = remember {
    tween(
        durationMillis = AnimationDurations.SLOW,
        easing = AnimationEasings.Smooth
    )
}

@Composable
fun <T> springBounce(): SpringSpec<T> = remember {
    spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
}

// ==================== 按钮动画 ====================

@Composable
fun tapScaleSpec(): TweenSpec<Float> = remember {
    tween(
        durationMillis = AnimationDurations.BUTTON_TAP,
        easing = AnimationEasings.Standard
    )
}

@Composable
fun hoverYSpec(): TweenSpec<Float> = remember {
    tween(
        durationMillis = AnimationDurations.CARD_HOVER,
        easing = AnimationEasings.Smooth
    )
}

@Composable
fun hoverElevationSpec(): TweenSpec<Float> = remember {
    tween(
        durationMillis = AnimationDurations.CARD_HOVER,
        easing = AnimationEasings.Smooth
    )
}

// ==================== Modifier扩展 ====================

/**
 * 按钮按压效果
 * scale(0.96) on press
 */
fun Modifier.pressEffect(
    scale: Float = 0.96f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = tween(
            durationMillis = AnimationDurations.BUTTON_TAP,
            easing = AnimationEasings.Standard
        ),
        label = "press_scale"
    )
    
    this
        .graphicsLayer {
            this.scaleX = animatedScale
            this.scaleY = animatedScale
            this.transformOrigin = TransformOrigin.Center
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown()
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * 悬停上移效果
 * translateY(-4px) on hover
 */
fun Modifier.hoverLift(
    liftAmount: Float = -4f
): Modifier = composed {
    var isHovered by remember { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (isHovered) liftAmount else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurations.CARD_HOVER,
            easing = AnimationEasings.Smooth
        ),
        label = "hover_offset"
    )
    
    this
        .graphicsLayer {
            this.translationY = animatedOffset
        }
}

/**
 * 进入动画 - Fade In + Y位移
 * framer-motion: initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
 */
fun Modifier.fadeInUp(
    delayMillis: Int = 0
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurations.PAGE_ENTER,
            delayMillis = delayMillis,
            easing = AnimationEasings.Enter
        ),
        label = "fade_alpha"
    )
    
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(
            durationMillis = AnimationDurations.PAGE_ENTER,
            delayMillis = delayMillis,
            easing = AnimationEasings.Enter
        ),
        label = "fade_offset"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    this.graphicsLayer {
        this.alpha = animatedAlpha
        this.translationY = animatedOffset
    }
}

/**
 * 缩放进入动画
 * framer-motion: initial={{ opacity: 0, scale: 0.9 }}
 */
fun Modifier.scaleIn(
    delayMillis: Int = 0
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurations.NORMAL,
            delayMillis = delayMillis,
            easing = AnimationEasings.Enter
        ),
        label = "scale_alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(
            durationMillis = AnimationDurations.NORMAL,
            delayMillis = delayMillis,
            easing = AnimationEasings.Bounce
        ),
        label = "scale_scale"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    this.graphicsLayer {
        this.alpha = animatedAlpha
        this.scaleX = animatedScale
        this.scaleY = animatedScale
        this.transformOrigin = TransformOrigin.Center
    }
}

/**
 * 交错动画延迟计算
 * index * 100ms
 */
fun staggerDelay(index: Int, baseDelay: Int = 100): Int {
    return index * baseDelay
}

// ==================== 页面过渡动画 ====================

/**
 * 页面进入动画规格
 */
@Composable
fun pageEnterSpec(): TweenSpec<Float> = remember {
    tween(
        durationMillis = AnimationDurations.PAGE_ENTER,
        easing = AnimationEasings.Enter
    )
}

/**
 * 列表项进入动画
 */
@Composable
fun listItemEnterSpec(index: Int): TweenSpec<Float> = remember(index) {
    tween(
        durationMillis = AnimationDurations.NORMAL,
        delayMillis = staggerDelay(index, 50),
        easing = AnimationEasings.Enter
    )
}

// ==================== 手势动画 ====================

/**
 * 滑动返回手势动画
 */
@Composable
fun swipeBackSpec(): SpringSpec<Float> = remember {
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

// ==================== 滚动动画 ====================

/**
 * 平滑滚动行为
 * 在LazyColumn/LazyRow中使用
 */
@Composable
fun smoothScrollSpec(): FlingBehavior {
    return rememberSplineBasedDecay()
}
