package com.silas.omaster.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * OMaster 2026 动画效果规范
 * 行业最高水平动画设计，流畅自然的用户体验
 */

// ========== 动画时长规范 (Material3标准) ==========

/**
 * 动画时长定义 - 符合Material3规范
 */
object AnimationDuration {
    // 快速动画（按钮点击、状态切换）
    const val FAST = 150

    // 标准动画（页面切换、卡片展开）
    const val STANDARD = 300

    // 慢速动画（复杂过渡、详细信息展示）
    const val SLOW = 500

    // 入场动画（页面首次加载）
    const val ENTER = 400

    // 退场动画（页面关闭）
    const val EXIT = 200

    // AI识别动画（场景识别过程）
    const val AI_RECOGNITION = 2000

    // 扫描动画周期
    const val SCAN_CYCLE = 3000
}

// ========== 动画缓动规范 ==========

/**
 * 缓动曲线定义 - 符合Material3规范
 */
object AnimationEasing {
    // 标准缓动（大部分场景）
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    // 加速缓动（元素离开）
    val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    // 减速缓动（元素进入）
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    // 强调缓动（重要元素）
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // 线性缓动（匀速动画）
    val LinearEasing = LinearEasing
}

// ========== 动画规格定义 ==========

/**
 * 预定义动画规格
 */
object AnimationSpecs {
    // 快速淡入淡出
    val fastFade = tween<Float>(
        durationMillis = AnimationDuration.FAST,
        easing = AnimationEasing.StandardEasing
    )

    // 标准淡入淡出
    val standardFade = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = AnimationEasing.StandardEasing
    )

    // 慢速淡入淡出
    val slowFade = tween<Float>(
        durationMillis = AnimationDuration.SLOW,
        easing = AnimationEasing.DecelerateEasing
    )

    // 入场动画
    val enterFade = tween<Float>(
        durationMillis = AnimationDuration.ENTER,
        easing = AnimationEasing.DecelerateEasing
    )

    // 退场动画
    val exitFade = tween<Float>(
        durationMillis = AnimationDuration.EXIT,
        easing = AnimationEasing.AccelerateEasing
    )

    // 滑动动画
    val slideIn = tween<IntOffset>(
        durationMillis = AnimationDuration.STANDARD,
        easing = AnimationEasing.DecelerateEasing
    )

    val slideOut = tween<IntOffset>(
        durationMillis = AnimationDuration.EXIT,
        easing = AnimationEasing.AccelerateEasing
    )

    // 缩放动画
    val scaleIn = tween<Float>(
        durationMillis = AnimationDuration.STANDARD,
        easing = AnimationEasing.DecelerateEasing
    )

    val scaleOut = tween<Float>(
        durationMillis = AnimationDuration.EXIT,
        easing = AnimationEasing.AccelerateEasing
    )

    // AI识别扫描动画
    val aiScanRotation = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = AnimationDuration.SCAN_CYCLE,
            easing = AnimationEasing.LinearEasing
        ),
        repeatMode = RepeatMode.Restart
    )

    // 脉冲动画
    val pulse = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = 1000,
            easing = AnimationEasing.StandardEasing
        ),
        repeatMode = RepeatMode.Reverse
    )

    // 弹跳动画
    val bounce = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // 强弹跳动画
    val strongBounce = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

// ========== 入场动画组合 ==========

/**
 * 页面入场动画 - 从底部滑入 + 淡入
 */
fun Modifier.omasterEnterAnimation(): Modifier = this
    .animateEnterExit(
        enter = slideInVertically(
            animationSpec = AnimationSpecs.slideIn,
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = AnimationSpecs.enterFade),
        exit = slideOutVertically(
            animationSpec = AnimationSpecs.slideOut,
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = AnimationSpecs.exitFade)
    )

/**
 * 卡片入场动画 - 缩放 + 淡入
 */
fun Modifier.cardEnterAnimation(): Modifier = this
    .animateEnterExit(
        enter = scaleIn(
            animationSpec = AnimationSpecs.scaleIn,
            initialScale = 0.8f
        ) + fadeIn(animationSpec = AnimationSpecs.standardFade),
        exit = scaleOut(
            animationSpec = AnimationSpecs.scaleOut,
            targetScale = 0.8f
        ) + fadeOut(animationSpec = AnimationSpecs.exitFade)
    )

/**
 * 弹窗入场动画 - 从底部滑入 + 强弹跳
 */
fun Modifier.sheetEnterAnimation(): Modifier = this
    .animateEnterExit(
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = AnimationSpecs.enterFade)
    )

// ========== 特殊动画效果 ==========

/**
 * AI识别扫描动画 - 旋转 + 脉冲
 */
@Composable
fun aiRecognitionAnimation(): AnimatedVisibilityScope.() -> Modifier = {
    Modifier
        .graphicsLayer {
            rotationZ = animateFloatAsState(
                targetValue = 360f,
                animationSpec = AnimationSpecs.aiScanRotation,
                label = "ai_rotation"
            ).value
        }
        .alpha(
            animateFloatAsState(
                targetValue = 1f,
                animationSpec = AnimationSpecs.pulse,
                label = "ai_alpha"
            ).value
        )
}

/**
 * 成功动画 - 缩放弹跳
 */
@Composable
fun successAnimation(trigger: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = AnimationSpecs.bounce,
        label = "success_scale"
    ).value
}

/**
 * 点击反馈动画 - 缩放
 */
@Composable
fun clickScaleAnimation(isPressed: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationSpecs.fastFade,
        label = "click_scale"
    ).value
}

/**
 * 参数滑块动画 - 平滑过渡
 */
@Composable
fun paramSliderAnimation(targetValue: Float): Float {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = AnimationDuration.FAST,
            easing = AnimationEasing.StandardEasing
        ),
        label = "param_value"
    ).value
}

/**
 * 场景识别置信度动画 - 数字滚动效果
 */
@Composable
fun confidenceAnimation(targetConfidence: Int): Int {
    val animatedValue = animateFloatAsState(
        targetValue = targetConfidence.toFloat(),
        animationSpec = tween(
            durationMillis = AnimationDuration.STANDARD,
            easing = AnimationEasing.DecelerateEasing
        ),
        label = "confidence"
    ).value
    return animatedValue.roundToInt()
}

// ========== 过渡动画组合 ==========

/**
 * 内容切换动画 - 淡入淡出 + 滑动
 */
fun contentTransition(): ContentTransform = ContentTransform(
    enter = fadeIn(animationSpec = AnimationSpecs.standardFade) +
            slideInHorizontally(animationSpec = AnimationSpecs.slideIn) { it / 2 },
    exit = fadeOut(animationSpec = AnimationSpecs.exitFade) +
            slideOutHorizontally(animationSpec = AnimationSpecs.slideOut) { -it / 2 },
    targetContentZIndex = 1f
)

/**
 * 列表项动画 - 交错入场
 */
fun listItemAnimation(index: Int): EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = AnimationDuration.STANDARD,
        delayMillis = index * 50, // 交错延迟
        easing = AnimationEasing.DecelerateEasing
    )
) + slideInVertically(
    animationSpec = tween(
        durationMillis = AnimationDuration.STANDARD,
        delayMillis = index * 50,
        easing = AnimationEasing.DecelerateEasing
    ),
    initialOffsetY = { 40 }
)

// ========== 预设动画组合 ==========

/**
 * 预设卡片展开动画
 */
fun presetExpandAnimation(): ContentTransform = ContentTransform(
    enter = scaleIn(
        animationSpec = AnimationSpecs.scaleIn,
        initialScale = 0.9f
    ) + fadeIn(animationSpec = AnimationSpecs.standardFade),
    exit = scaleOut(
        animationSpec = AnimationSpecs.scaleOut,
        targetScale = 0.9f
    ) + fadeOut(animationSpec = AnimationSpecs.exitFade)
)

/**
 * 水印预览动画
 */
fun watermarkPreviewAnimation(): EnterTransition = fadeIn(
    animationSpec = AnimationSpecs.slowFade
) + scaleIn(
    animationSpec = AnimationSpecs.scaleIn,
    initialScale = 0.95f
)

/**
 * 参数调节动画
 */
fun paramAdjustAnimation(): EnterTransition = slideInHorizontally(
    animationSpec = AnimationSpecs.slideIn
) { -it } + fadeIn(animationSpec = AnimationSpecs.standardFade)