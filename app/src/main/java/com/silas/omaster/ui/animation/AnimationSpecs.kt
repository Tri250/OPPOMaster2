package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import com.silas.omaster.ui.theme.ColorOS16Palette
import com.silas.omaster.ui.theme.LiquidGlassConfig

/**
 * 全局动画配置
 * 统一管理应用内所有动画规格，确保一致性和性能
 *
 * ColorOS 16 动画规范：
 * - 使用 Spring 物理动画替代固定时长 Tween
 * - 弹性阻尼比 0.75（中等弹性，柔和自然）
 * - 刚度系数 400（中等偏低，流畅不突兀）
 * - 过渡动画时长 300-400ms
 * - 微交互 150-200ms
 *
 * 注意：Spring 动画在低端设备上应使用较低的 stiffness 值以避免卡顿。
 * 使用 adaptiveSpringSpec() 可根据设备性能等级自动选择合适的动画参数。
 */
object AnimationSpecs {

    /**
     * 快速动画 - 用于微交互（按钮点击、图标变化等）
     * 时长：150ms
     */
    val FastTween = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )

    /**
     * 标准动画 - 用于一般过渡（页面切换、内容显示等）
     * 时长：250ms
     */
    val NormalTween = tween<Float>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    /**
     * 慢速动画 - 用于强调动画（卡片入场、重要提示等）
     * 时长：400ms
     */
    val SlowTween = tween<Float>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    /**
     * 列表项入场动画 - 轻量级，适合大量列表项
     * 使用较硬的 spring 减少计算量
     */
    val ListItemSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.01f
    )

    /**
     * 卡片弹性动画 - 用于卡片等需要弹性的元素
     * 优化：提高刚度，减少拖沓感
     */
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = 0.001f
    )

    /**
     * 淡入动画规格
     */
    val FadeInSpec = tween<Float>(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )

    /**
     * 淡出动画规格
     */
    val FadeOutSpec = tween<Float>(
        durationMillis = 150,
        easing = FastOutLinearInEasing
    )

    /**
     * 滑动动画规格
     */
    val SlideSpec = tween<Int>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    /**
     * 缩放动画规格
     */
    val ScaleSpec = tween<Float>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    /**
     * 列表项错开延迟基准值
     * 优化：从 50ms 减少到 20ms，提升加载流畅度
     */
    const val StaggerDelayMillis = 20

    /**
     * 列表项最大延迟
     * 优化：从 300ms 减少到 150ms，提升加载流畅度
     */
    const val MaxStaggerDelayMillis = 150

    /**
     * 自动播放间隔
     */
    const val AutoPlayIntervalMillis = 3000L

    /**
     * 页面切换动画时长
     */
    const val PageTransitionMillis = 250

    // ==================== ColorOS 16 Spring 动画规格 ====================

    /**
     * ColorOS 16 标准弹性动画
     * 阻尼比 0.75，刚度 400 - 柔和自然的弹性效果
     * 适用于：页面切换、卡片展开、模态弹出
     */
    val ColorOS16StandardSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 400f,
        visibilityThreshold = 0.001f
    )

    /**
     * ColorOS 16 柔和弹性动画
     * 阻尼比 0.85，刚度 300 - 更柔和的过渡
     * 适用于：底部抽屉、滑出面板
     */
    val ColorOS16GentleSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 300f,
        visibilityThreshold = 0.001f
    )

    /**
     * ColorOS 16 活泼弹性动画
     * 阻尼比 0.6，刚度 500 - 更有弹性的反馈
     * 适用于：按钮按压、图标弹跳、开关切换
     */
    val ColorOS16BouncySpring = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 500f,
        visibilityThreshold = 0.001f
    )

    /**
     * ColorOS 16 微交互弹性动画
     * 阻尼比 0.7，刚度 600 - 快速响应的微交互
     * 适用于：涟漪效果、焦点变化、选中状态
     */
    val ColorOS16MicroSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 600f,
        visibilityThreshold = 0.01f
    )

    /**
     * ColorOS 16 液态玻璃过渡动画
     * 阻尼比 0.8，刚度 350 - 流体般的过渡效果
     * 适用于：液态玻璃效果出现/消失、模糊度变化
     */
    val ColorOS16LiquidSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 350f,
        visibilityThreshold = 0.001f
    )

    /**
     * ColorOS 16 页面切换动画
     * 时长 350ms，使用 Emphasized 缓动
     */
    val ColorOS16PageTransition = tween<Float>(
        durationMillis = 350,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f) // Emphasized easing
    )

    /**
     * ColorOS 16 内容入场动画
     * 时长 300ms，使用 EmphasizedAccelerate 缓动
     */
    val ColorOS16ContentEnter = tween<Float>(
        durationMillis = 300,
        easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f) // EmphasizedAccelerate
    )

    /**
     * ColorOS 16 内容退场动画
     * 时长 200ms，使用 EmphasizedDecelerate 缓动
     */
    val ColorOS16ContentExit = tween<Float>(
        durationMillis = 200,
        easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f) // EmphasizedDecelerate
    )
}

/**
 * 记住动画状态的便捷函数
 * 避免在重组时重复创建 Animatable
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
 * 用于 LazyColumn/LazyRow 的 animateItemPlacement
 */
val ListItemPlacementSpec: SpringSpec<IntOffset> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/**
 * 列表项淡入规格
 * 用于 LazyColumn/LazyRow 的 animateItemFadeIn
 */
val ListItemFadeInSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = 0.01f
)

/**
 * 设备性能等级
 */
enum class PerformanceTier {
    LOW, MEDIUM, HIGH
}

/**
 * 根据设备性能等级返回自适应的 Spring 动画规格。
 * 低端设备使用较低的 stiffness 以减少动画计算压力，避免卡顿；
 * 高端设备使用标准参数以获得更流畅的弹性效果。
 *
 * @param dampingRatio 阻尼比，默认中等弹性
 * @param visibilityThreshold 可见性阈值
 * @return 根据设备性能调整后的 SpringSpec
 */
@Composable
fun adaptiveSpringSpec(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    visibilityThreshold: Float = 0.01f
): SpringSpec<Float> {
    val context = LocalContext.current
    val tier = remember {
        val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
        val isLowRam = activityManager?.isLowRamDevice == true
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalMemGb = memInfo.totalMem / (1024 * 1024 * 1024)
        when {
            isLowRam || totalMemGb < 3 -> PerformanceTier.LOW
            totalMemGb < 6 -> PerformanceTier.MEDIUM
            else -> PerformanceTier.HIGH
        }
    }
    val stiffness = when (tier) {
        PerformanceTier.LOW -> Spring.StiffnessHigh
        PerformanceTier.MEDIUM -> Spring.StiffnessMedium
        PerformanceTier.HIGH -> Spring.StiffnessMediumLow
    }
    return spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )
}

// ==================== 入场动画 Modifier ====================

/**
 * 淡入 + 缩放入场动画
 * 适用于卡片、按钮等元素的出现
 *
 * @param delayMillis 延迟时间
 * @param initialScale 初始缩放
 */
@Composable
fun Modifier.animateFadeInScale(
    visible: Boolean = true,
    delayMillis: Int = 0,
    initialScale: Float = 0.9f
): Modifier {
    val animatedAlpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = delayMillis,
            easing = LinearOutSlowInEasing
        ),
        label = "fade_in_alpha"
    )
    val animatedScale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else initialScale,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f,
            visibilityThreshold = 0.001f
        ),
        label = "fade_in_scale"
    )
    return this
        .graphicsLayer {
            alpha = animatedAlpha.value
            scaleX = animatedScale.value
            scaleY = animatedScale.value
        }
}

/**
 * 向上滑入 + 淡入动画
 * 适用于列表项、底部面板等
 *
 * @param delayMillis 延迟时间
 * @param translationY 初始位移
 */
@Composable
fun Modifier.animateSlideUpFadeIn(
    visible: Boolean = true,
    delayMillis: Int = 0,
    translationY: Dp = 20.dp
): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val animatedAlpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = delayMillis,
            easing = LinearOutSlowInEasing
        ),
        label = "slide_up_alpha"
    )
    val animatedTranslationY = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else with(density) { translationY.toPx() },
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f,
            visibilityThreshold = 0.5f
        ),
        label = "slide_up_translation"
    )
    return this
        .graphicsLayer {
            alpha = animatedAlpha.value
            this.translationY = animatedTranslationY.value
        }
}

/**
 * 呼吸动画效果
 * 适用于需要吸引注意力的元素（如新功能提示、重要按钮）
 *
 * @param minScale 最小缩放
 * @param maxScale 最大缩放
 * @param durationMillis 周期时长
 */
@Composable
fun Modifier.animateBreathe(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    durationMillis: Int = 2000
): Modifier {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )
    return if (enabled) {
        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else {
        this
    }
}

/**
 * 脉冲动画（光晕效果）
 * 适用于需要强调的元素
 *
 * @param color 脉冲颜色
 * @param maxAlpha 最大透明度
 */
@Composable
fun Modifier.animatePulse(
    enabled: Boolean = true,
    color: Color = com.silas.omaster.ui.theme.HasselbladOrange,
    maxAlpha: Float = 0.3f
): Modifier {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = maxAlpha,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    return this
        .graphicsLayer {
            if (enabled) {
                this.shadowElevation = 0f
            }
        }
        .drawBehind {
            if (enabled) {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = size.minDimension * scale / 2
                )
            }
        }
}

// ==================== 页面切换动画 ====================

/**
 * 页面进入动画 - 从右滑入 + 淡入
 */
val PageEnterTransition = androidx.compose.animation.slideInHorizontally(
    initialOffsetX = { it / 3 },
    animationSpec = tween(300, easing = FastOutSlowInEasing)
) + androidx.compose.animation.fadeIn(
    animationSpec = tween(200, easing = LinearOutSlowInEasing)
)

/**
 * 页面退出动画 - 向左滑出 + 淡出
 */
val PageExitTransition = androidx.compose.animation.slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = tween(250, easing = FastOutLinearInEasing)
) + androidx.compose.animation.fadeOut(
    animationSpec = tween(150, easing = FastOutLinearInEasing)
)

/**
 * 页面弹出进入动画 - 从底部滑入 + 淡入
 */
val BottomSheetEnterTransition = androidx.compose.animation.slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
) + androidx.compose.animation.fadeIn(
    animationSpec = tween(200)
)

/**
 * 页面弹出退出动画 - 向下滑出 + 淡出
 */
val BottomSheetExitTransition = androidx.compose.animation.slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(250, easing = FastOutLinearInEasing)
) + androidx.compose.animation.fadeOut(
    animationSpec = tween(150)
)

// ==================== ColorOS 16 液态玻璃效果 Modifier ====================

/**
 * ColorOS 16 液态玻璃效果 Modifier
 * 为 Composable 添加液态玻璃视觉效果（模糊 + 半透明 + 边框 + 高光）
 *
 * 注意：模糊效果需要 API 31+ (Android 12+)，低版本设备自动降级为半透明效果
 *
 * @param cornerRadius 圆角半径，默认 24.dp
 * @param backgroundColor 背景色，默认使用深色玻璃色
 * @param borderColor 边框色，默认使用玻璃边框色
 */
@Deprecated("使用 LiquidGlassComponents.kt 中的 liquidGlass Modifier")
@Composable
fun Modifier.liquidGlassEffect(
    cornerRadius: Dp = Dp(LiquidGlassConfig.CornerRadius),
    backgroundColor: Color = ColorOS16Palette.GlassDarkSurface,
    borderColor: Color = ColorOS16Palette.GlassDarkBorder
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(
        width = Dp(LiquidGlassConfig.BorderWidth),
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )
