package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset

/**
 * OMaster Web风格动画系统
 * 融合Web端framer-motion动画效果
 * 参考：https://github.com/Tri250/OPPOMaster Web端组件
 */

object WebAnimationSpecs {

    // ========== framer-motion风格动画时长 ==========
    // Hero区域入场动画
    val HeroFadeIn = tween<Float>(
        durationMillis = 800,
        easing = LinearOutSlowInEasing
    )

    // Hero区域Y轴移动
    val HeroSlideUp = tween<Float>(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )

    // Features卡片入场
    val FeatureCardEnter = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // PresetCard入场动画
    val PresetCardEnter = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // PresetCard缩放动画
    val PresetCardScale = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // 悬停效果 - Web whileHover={{ y: -8 }}
    val HoverYOffset = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // 悬停缩放 - Web whileHover={{ scale: 1.05 }}
    val HoverScale = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    // 按压效果 - Web whileTap={{ scale: 0.95 }}
    val PressScale = tween<Float>(
        durationMillis = 100,
        easing = LinearEasing
    )

    // 图标悬停缩放 - Web group-hover:scale-110
    val IconHoverScale = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // 封面图悬停缩放 - Web group-hover:scale-105 duration-500
    val CoverHoverScale = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )

    // ========== 弹性动画 - Web风格 ==========
    // 列表项入场
    val ListItemSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.01f
    )

    // 卡片弹性
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.001f
    )

    // 按钮弹性
    val ButtonSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )

    // ========== 淡入淡出 ==========
    val FadeIn = tween<Float>(
        durationMillis = 400,
        easing = LinearOutSlowInEasing
    )

    val FadeOut = tween<Float>(
        durationMillis = 300,
        easing = FastOutLinearInEasing
    )

    // ========== 滑动动画 ==========
    val SlideInFromBottom = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    val SlideInFromTop = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    // ========== 错开延迟 - Web delay: index * 0.1 ==========
    const val StaggerDelayBase = 100  // 100ms = 0.1s
    const val MaxStaggerDelay = 500   // 最大500ms

    // 计算错开延迟
    fun calculateStaggerDelay(index: Int): Int {
        return (index * StaggerDelayBase).coerceAtMost(MaxStaggerDelay)
    }

    // ========== Web风格贝塞尔曲线 ==========
    // 模拟CSS ease-out
    val WebEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // 模拟CSS ease-in-out
    val WebEaseInOut = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    // 模拟framer-motion默认曲线
    val FramerMotionDefault = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    // ========== 页面切换动画时长 ==========
    const val PageTransitionDuration = 300

    // 导航栏动画时长
    const val NavBarAnimationDuration = 300

    // 自动播放间隔
    const val AutoPlayInterval = 3000L
}

/**
 * Web风格悬停动画状态
 * 模拟framer-motion whileHover效果
 */
@Composable
fun rememberWebHoverState(): WebHoverAnimationState {
    return remember { WebHoverAnimationState() }
}

class WebHoverAnimationState {
    var isHovered by mutableStateOf(false)

    // whileHover={{ y: -8 }}
    val yOffset: Float by derivedStateOf {
        if (isHovered) -8f else 0f
    }

    // whileHover={{ scale: 1.05 }}
    val scale: Float by derivedStateOf {
        if (isHovered) 1.05f else 1f
    }
}

/**
 * Web风格按压动画状态
 * 模拟framer-motion whileTap效果
 */
@Composable
fun rememberWebPressState(): WebPressAnimationState {
    return remember { WebPressAnimationState() }
}

class WebPressAnimationState {
    var isPressed by mutableStateOf(false)

    // whileTap={{ scale: 0.95 }}
    val scale: Float by derivedStateOf {
        if (isPressed) 0.95f else 1f
    }
}

/**
 * Web风格入场动画状态
 * 模拟framer-motion initial/animate效果
 */
@Composable
fun rememberWebEnterState(): WebEnterAnimationState {
    return remember { WebEnterAnimationState() }
}

class WebEnterAnimationState {
    // initial={{ opacity: 0, y: 30 }}
    var hasEntered by mutableStateOf(false)

    // animate={{ opacity: 1, y: 0 }}
    val opacity: Float by derivedStateOf {
        if (hasEntered) 1f else 0f
    }

    val yOffset: Float by derivedStateOf {
        if (hasEntered) 0f else 30f
    }

    // initial={{ opacity: 0, scale: 0.9 }}
    val scale: Float by derivedStateOf {
        if (hasEntered) 1f else 0.9f
    }

    fun enter() {
        hasEntered = true
    }
}

/**
 * 卡片入场动画规格
 * 模拟PresetCard的入场效果
 */
object CardEnterAnimation {
    // initial={{ opacity: 0, scale: 0.9 }}
    val initialOpacity = 0f
    val initialScale = 0.9f

    // whileInView={{ opacity: 1, scale: 1 }}
    val targetOpacity = 1f
    val targetScale = 1f

    // transition={{ delay: index * 0.1 }}
    fun getDelay(index: Int): Int {
        return WebAnimationSpecs.calculateStaggerDelay(index)
    }
}

/**
 * Hero区域入场动画规格
 * 模拟Hero组件的入场效果
 */
object HeroEnterAnimation {
    // initial={{ opacity: 0, y: 30 }}
    val initialOpacity = 0f
    val initialYOffset = 30f

    // animate={{ opacity: 1, y: 0 }}
    val targetOpacity = 1f
    val targetYOffset = 0f

    // transition={{ duration: 0.8 }}
    val duration = 800
}

/**
 * 统计数据入场动画规格
 * 模拟统计数据错开入场效果
 */
object StatsEnterAnimation {
    // transition={{ delay: 0.3 + index * 0.1 }}
    fun getDelay(index: Int): Int {
        return 300 + WebAnimationSpecs.calculateStaggerDelay(index)
    }
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
val ListItemFadeInSpec: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = 0.01f
)

// ========== 导入derivedStateOf ==========
import androidx.compose.runtime.derivedStateOf