package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * 全局动画配置
 * 统一管理应用内所有动画规格，确保一致性和性能
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

// ═══════════════════════════════════════════════════════════
// 【v1.6.13】ColorOS 17 液态按压反馈（Fluid Press）
// 按下轻微缩小、松手低阻尼回弹，对齐 ColorOS 17 灵动交互手感；
// 纯 graphicsLayer 缩放，硬件加速，不触发布局与测量。
// ═══════════════════════════════════════════════════════════

/** 液态按压弹簧：按下跟手，松手细腻回弹 */
val FluidPressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh,
    visibilityThreshold = 0.0001f
)

/**
 * ColorOS 17 液态按压缩放：配合 Card(onClick, interactionSource) 使用。
 * 按下缩小至 [pressedScale]，松手以 FluidPressSpring 回弹。
 */
@Composable
fun Modifier.fluidPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = FluidPressSpring,
        label = "fluidPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ═══════════════════════════════════════════════════════════
// 【v1.6.14】ColorOS 17 流体设计三级参数体系（Fluid Design System）
//
// 背景：v1.6.13 及之前各页面动效参数各自为政（PillNavBar 用 StiffnessLow、
// ColorWalkCard 用 tween(400)、MainActivity 用 0.92f/380f），同类交互手感不一致。
// v1.6.14 起全 App 动效必须引用本体系，禁止文件内私有 spring/tween 硬编码。
//
// L1 微交互：按压 / Toggle / Chip      —— 跟手、回弹利落
// L2 组件过渡：面板滑入 / 入场 / 展开   —— 0.92f 低刚度，细腻不夸张
// L3 页面转场：路由切换                 —— 与 MainActivity.fluidSlideSpec 参数同源
// ═══════════════════════════════════════════════════════════

/** L1 微交互弹簧（Float）—— 按压、Toggle、Chip */
val FluidPressFloat = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessHigh,
    visibilityThreshold = 0.0001f
)

/** L2 组件过渡弹簧（Float）—— 面板滑入、Banner 入场、展开收起 */
val FluidComponentFloat = spring<Float>(
    dampingRatio = 0.92f,
    stiffness = 380f,
    visibilityThreshold = 0.0001f
)

/** L2 组件过渡弹簧（IntOffset）—— 面板/胶囊位移动画 */
val FluidComponentIntOffset = spring<IntOffset>(
    dampingRatio = 0.92f,
    stiffness = 380f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/** L2 组件过渡弹簧（Dp）—— 宽度 / 高度 / 圆角形变（流体 FAB、展开菜单） */
val FluidComponentDp = spring<Dp>(
    dampingRatio = 0.92f,
    stiffness = 380f,
    visibilityThreshold = 0.01f
)

/** L3 页面转场弹簧（Float）—— 路由切换淡入淡出 */
val FluidPageFloat = spring<Float>(
    dampingRatio = 0.92f,
    stiffness = 380f,
    visibilityThreshold = 0.0001f
)

/** L3 页面转场弹簧（IntOffset）—— 路由切换滑动 */
val FluidPageIntOffset = spring<IntOffset>(
    dampingRatio = 0.92f,
    stiffness = 380f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

/**
 * 流体设计标准时长（毫秒）—— 与 FramerateOptimizer 配合做高刷自适应。
 * 所有 tween 类动画应以这三个常量为基准，避免散落的魔法数字。
 */
const val FLUID_MICRO_MS = 150      // 微交互：高亮、闪烁、指示点
const val FLUID_COMPONENT_MS = 250  // 组件：展开、收起、滑入
const val FLUID_PAGE_MS = 300       // 页面：转场、切换

/**
 * 流体庆祝动画时长（毫秒）—— 打卡完成 / 挑战完成 / 里程碑达成
 */
const val FLUID_CELEBRATE_MS = 600

/**
 * 列表项错开延迟（毫秒）—— 列表入场、标签云、特性列表统一使用
 */
const val FLUID_STAGGER_MS = 40
