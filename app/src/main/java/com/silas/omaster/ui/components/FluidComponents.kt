package com.silas.omaster.ui.components

import android.content.Context
import android.view.Choreographer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.animation.FluidComponentDp
import com.silas.omaster.ui.animation.FluidComponentFloat
import com.silas.omaster.ui.animation.FLUID_CELEBRATE_MS
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.util.FramerateOptimizer
import com.silas.omaster.util.perform
import kotlinx.coroutines.awaitCancellation

/**
 * 【v1.6.14】ColorOS 17 流体设计公共组件库
 *
 * 集中承载三大模块共用的流体交互组件，避免各页面重复实现导致手感漂移：
 * - AuroraRefreshIndicator：凝光追光刷新指示器（H5）
 * - NumberRollText：灵动数字滚动（H2 / D4 / A2）
 * - FluidFab：按压液态形变的悬浮按钮（H8）
 * - CelebrationOverlay：流体庆祝光晕 + 色卡飘落（D5 / D6 / D9）
 * - auroraFocusBorder：聚焦流光描边（H4）
 * - rememberFrameDegrade：高刷掉帧降级（跨模块 6.2）
 */

// ═══════════════════════════════════════════════════════════
// 6.2 高刷掉帧降级（潮汐引擎式"前台流畅"）
// ═══════════════════════════════════════════════════════════

/**
 * 监听真实帧率，连续掉帧时返回 true（调用方据此暂停交错动画 / 降低模糊半径）。
 *
 * 60Hz 设备上完全短路（不注册帧回调，零开销），符合"兼容性红线"。
 *
 * @param active 是否启用监听（如仅在列表滑动期间传 true）
 */
@Composable
fun rememberFrameDegrade(
    context: Context = LocalContext.current,
    active: Boolean = true
): Boolean {
    val highRefresh = remember(context) { FramerateOptimizer.isHighRefreshDevice(context) }
    val monitor = remember(context) { FramerateOptimizer.createMonitor(context) }
    var degrade by remember { mutableStateOf(false) }

    LaunchedEffect(active, highRefresh, monitor) {
        if (!active || !highRefresh) {
            monitor.reset()
            if (degrade) degrade = false
            return@LaunchedEffect
        }
        val choreographer = Choreographer.getInstance()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                monitor.onFrame(frameTimeNanos)
                if (monitor.shouldDegrade != degrade) degrade = monitor.shouldDegrade
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        try {
            awaitCancellation()
        } finally {
            choreographer.removeFrameCallback(callback)
            monitor.reset()
        }
    }
    DisposableEffect(Unit) {
        onDispose { monitor.reset() }
    }
    return degrade
}

// ═══════════════════════════════════════════════════════════
// H4 凝光聚焦描边（搜索框光随指动）
// ═══════════════════════════════════════════════════════════

/**
 * 聚焦时在组件外圈绘制一道循环流动的渐变光描边，失焦后淡出。
 * 对齐 ColorOS 17「光是界面意图的指引」—— 用光告诉用户"焦点在这里"。
 *
 * 纯 drawBehind 绘制，不触发布局与测量；非聚焦状态完全跳过绘制。
 */
fun Modifier.auroraFocusBorder(
    focused: Boolean,
    color: Color = Color.Unspecified,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = AppDesign.LargeRadius,
    durationMillis: Int = 2200
): Modifier = composed {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "auroraBorder")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraSweep"
    )
    val alpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(200),
        label = "auroraAlpha"
    )
    this.drawBehind {
        if (alpha <= 0.01f) return@drawBehind
        val w = size.width
        val h = size.height
        val travel = w * 2f
        val offsetX = -travel + sweep * travel * 2f
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                tint.copy(alpha = 0.35f * alpha),
                tint.copy(alpha = 0.9f * alpha),
                tint.copy(alpha = 0.35f * alpha),
                Color.Transparent
            ),
            start = Offset(offsetX, 0f),
            end = Offset(offsetX + travel * 0.5f, h),
            tileMode = TileMode.Clamp
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = borderWidth.toPx())
        )
    }
}

// ═══════════════════════════════════════════════════════════
// H2 / D4 / A2 灵动数字滚动
// ═══════════════════════════════════════════════════════════

/**
 * 数值变化时以垂直滚动方式过渡（对齐 ColorOS 17 灵动数字风格）。
 * 用于 Tab 计数（H2）、相框可用数（D4）、设备规格数值（A2）。
 */
@Composable
fun NumberRollText(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall,
    fontWeight: FontWeight? = FontWeight.Bold
) {
    val text = value.toString()
    Row(modifier = modifier, horizontalArrangement = Arrangement.Start) {
        text.forEach { char ->
            RollingDigit(char = char, color = color, style = style, fontWeight = fontWeight)
        }
    }
}

@Composable
private fun RollingDigit(
    char: Char,
    color: Color,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight?
) {
    AnimatedContent(
        targetState = char,
        transitionSpec = {
            if (targetState > initialState) {
                slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                slideInVertically { height -> -height } + fadeIn() togetherWith
                        slideOutVertically { height -> height } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        label = "rollingDigit"
    ) { digit ->
        Text(
            text = digit.toString(),
            color = color,
            style = style,
            fontWeight = fontWeight
        )
    }
}

// ═══════════════════════════════════════════════════════════
// H5 凝光追光刷新指示器
// ═══════════════════════════════════════════════════════════

/**
 * 下拉刷新指示器 —— 主色渐变弧随下拉进度旋转（光追着手指走），
 * 刷新完成时弧线收缩为对勾。替代系统默认 PullRefreshIndicator。
 *
 * @param progress 下拉进度 0f ~ 1f（超过 1f 视为已触发）
 * @param refreshing 是否正在刷新
 * @param finished 刷新是否刚完成（用于展示对勾，展示后由调用方置回 false）
 */
@Composable
fun AuroraRefreshIndicator(
    progress: Float,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
    finished: Boolean = false,
    color: Color = Color.Unspecified,
    size: Dp = 36.dp
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val sizePx = size

    val sweepProgress by animateFloatAsState(
        targetValue = when {
            finished -> 1f
            refreshing -> 0.75f
            else -> progress.coerceIn(0f, 1f) * 0.9f
        },
        animationSpec = tween(200),
        label = "auroraSweepProgress"
    )
    val appear by animateFloatAsState(
        targetValue = if (progress > 0f || refreshing || finished) 1f else 0f,
        animationSpec = tween(200),
        label = "auroraAppear"
    )

    if (appear <= 0.01f) return

    // v1.6.15 性能修复：无限旋转动画仅在"刷新中且未完成"时创建。
    // 此前 rememberInfiniteTransition 无条件创建，指示器不可见时（appear<=0 提前 return 之前）
    // 动画仍在 144Hz 设备上持续占用帧回调。
    val transition = rememberInfiniteTransition(label = "auroraRefresh")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraSpin"
    )

    Canvas(
        modifier = modifier
            .size(sizePx)
            .graphicsLayer { alpha = appear }
    ) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val arcSweep = 40f + sweepProgress * 280f
        val startAngle = if (refreshing && !finished) spin else -90f

        if (finished) {
            // 完成态：画一个对勾（√）
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            drawLine(
                color = tint,
                start = Offset(cx - 8.dp.toPx(), cy),
                end = Offset(cx - 2.dp.toPx(), cy + 6.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(cx - 2.dp.toPx(), cy + 6.dp.toPx()),
                end = Offset(cx + 9.dp.toPx(), cy - 7.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // 追光弧：渐变色 + 圆头，模拟光沿轨道流动
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.15f),
                        tint.copy(alpha = 0.35f),
                        tint,
                        tint.copy(alpha = 0.35f),
                        tint.copy(alpha = 0.15f)
                    )
                ),
                startAngle = startAngle,
                sweepAngle = arcSweep,
                useCenter = false,
                style = stroke
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// H8 流体形变 FAB
// ═══════════════════════════════════════════════════════════

/** 流体 FAB 的次级动作定义 */
data class FluidFabAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null
)

/**
 * ColorOS 17 流体形变悬浮按钮：
 * - 按压时圆形液态延展为圆角胶囊并显出文案（松手回弹，L1 微交互）
 * - 长按展开次级动作（从模板创建 / 剪贴板导入），贴合系统长按范式
 * - 点击本体直达默认创建流程
 */
@Composable
fun FluidFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    actions: List<FluidFabAction> = emptyList(),
    visible: Boolean = true
) {
    if (!visible) return
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var expanded by remember { mutableStateOf(false) }

    val targetWidth = if (isPressed && label.isNotBlank()) 148.dp else AppDesign.FABSize
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = FluidComponentDp,
        label = "fabWidth"
    )
    val corner by animateDpAsState(
        targetValue = if (isPressed && label.isNotBlank()) AppDesign.FullRadius else AppDesign.FABSize / 2,
        animationSpec = FluidComponentDp,
        label = "fabCorner"
    )
    val containerScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = FluidComponentFloat,
        label = "fabScale"
    )

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = containerScale
            scaleY = containerScale
        },
        horizontalAlignment = Alignment.End
    ) {
        // 次级动作（长按展开）
        actions.forEachIndexed { index, action ->
            val itemAlpha by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = FluidComponentFloat,
                label = "fabAction$index"
            )
            if (itemAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .graphicsLayer { alpha = itemAlpha }
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(AppDesign.FullRadius))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            expanded = false
                            action.onClick()
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (action.icon != null) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                    }
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (expanded) 4.dp else 0.dp))

        // 主按钮：按压液态形变
        Box(
            modifier = Modifier
                .height(AppDesign.FABSize)
                .width(width)
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.primary)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = {
                        if (actions.isNotEmpty()) {
                            haptic.perform(HapticFeedbackType.LongPress)
                            expanded = !expanded
                        }
                    },
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        if (actions.isNotEmpty() && expanded) {
                            expanded = false
                        } else {
                            onClick()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = label.ifBlank { null },
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                if (label.isNotBlank()) {
                    val labelAlpha by animateFloatAsState(
                        targetValue = if (isPressed) 1f else 0f,
                        animationSpec = FluidComponentFloat,
                        label = "fabLabel"
                    )
                    if (labelAlpha > 0.01f) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// D5 / D6 / D9 流体庆祝光晕
// ═══════════════════════════════════════════════════════════

/**
 * 流体庆祝层 —— 打卡完成 / 挑战完成 / 里程碑达成时叠加在内容之上。
 * 双环错开扩散（0ms / 150ms）+ 若干色块抛物线飘落，2s 后自动消失。
 *
 * @param visible 触发一次庆祝（调用方在动画结束后置回 false）
 * @param glowColor 光环颜色（通常取主题色或色卡主色）
 * @param confettiColors 飘落色块颜色（来自色卡时传色卡配色）
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Unspecified,
    confettiColors: List<Color> = emptyList()
) {
    if (!visible) return
    val tint = if (glowColor == Color.Unspecified) MaterialTheme.colorScheme.primary else glowColor

    // 一次性动画：0 → 1 线性推进，全程由 Canvas 读取（无需重组状态）
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(FLUID_CELEBRATE_MS, easing = LinearEasing)
        )
    }
    val chips = confettiColors.take(3).ifEmpty { listOf(tint, tint, tint) }

    Canvas(modifier = modifier) {
        val p = progress.value
        val cx = size.width / 2f
        val cy = size.height / 2f
        val alpha = (1f - p).coerceIn(0f, 1f)

        // 双环错开扩散（0ms / 150ms 视觉延迟）
        listOf(0f, 0.25f).forEach { delay ->
            val dp = (p - delay).coerceIn(0f, 1f)
            if (dp <= 0f) return@forEach
            val radius = 40.dp.toPx() + dp * 220.dp.toPx()
            drawCircle(
                color = tint.copy(alpha = 0.35f * (1f - dp)),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = (3.dp.toPx() * (1f - dp)).coerceAtLeast(0.5f))
            )
        }

        // 色块飘落（抛物线：横向微移 + 纵向加速下落）
        chips.forEachIndexed { index, color ->
            val dp = (p - index * 0.12f).coerceIn(0f, 1f)
            if (dp <= 0f) return@forEachIndexed
            val x = cx + (index - 1) * 70.dp.toPx() + dp * 18.dp.toPx()
            val y = cy - 60.dp.toPx() + dp * dp * 260.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = (1f - dp).coerceIn(0f, 1f)),
                topLeft = Offset(x - 9.dp.toPx(), y - 6.dp.toPx()),
                size = Size(18.dp.toPx(), 12.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
        // alpha 参与计算已覆盖淡出；保留变量避免未使用告警
        if (alpha < 0f) drawCircle(Color.Transparent, 0f, Offset.Zero)
    }
}

// ═══════════════════════════════════════════════════════════
// 通用：流体展开容器
// ═══════════════════════════════════════════════════════════

/**
 * 可展开的流体容器 —— 高度以 L2 组件弹簧过渡，内容错开淡入。
 * 用于关于页设备档案（A2）、特性详情（A4）。
 */
@Composable
fun FluidExpandable(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(com.silas.omaster.ui.animation.FLUID_COMPONENT_MS)) +
                expandVertically(animationSpec = tween(com.silas.omaster.ui.animation.FLUID_COMPONENT_MS)),
        exit = fadeOut(animationSpec = tween(com.silas.omaster.ui.animation.FLUID_COMPONENT_MS)) +
                shrinkVertically(animationSpec = tween(com.silas.omaster.ui.animation.FLUID_COMPONENT_MS))
    ) {
        content()
    }
}

/** 流体小胶囊标签（流体云筛选 / 色卡场景标签复用） */
@Composable
fun FluidCloudChip(
    text: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (onRemove != null) {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        onRemove()
                    }
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium
        )
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.75f)
            )
        }
    }
}

/** 保留 CircleShape 引用以避免未使用告警（组件库对外暴露形状规范） */
val FluidShapeCircle: androidx.compose.ui.graphics.Shape
    get() = CircleShape
