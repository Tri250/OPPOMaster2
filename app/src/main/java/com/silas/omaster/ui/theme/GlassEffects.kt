package com.silas.omaster.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 【v1.7.7】OPPO Reno17 系列风格 · 玻璃动效体系
 *
 * 设计语言对齐 Reno17：
 * - 极光背景（Aurora）：深空底上三团低透明度蓝/紫/青光晕缓慢漂移呼吸，
 *   玻璃卡片半透明，透出极光呈现 Reno17 玻璃后盖的通透层次；
 * - 玻璃面（Glass Surface）：半透明填充 + 蓝调棱边。
 *
 * 性能约束：全部为纯绘制（Canvas/drawCircle/drawRect），无着色器逐帧重建、
 * 无布局测量参与动画；光晕用多层实心圆叠加近似柔光，避免每帧重建 Brush。
 */

/** 极光动画周期（ms）：一次完整呼吸 */
private const val AURORA_CYCLE_MS = 16000

/**
 * Reno17 极光背景：绘制在内容下层，为玻璃卡片提供"透底"。
 *
 * 用法：
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     Reno17AuroraBackground()
 *     // 页面内容
 * }
 * ```
 */
@Composable
fun Reno17AuroraBackground(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background == PureBlack
    val transition = rememberInfiniteTransition(label = "reno17Aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AURORA_CYCLE_MS, easing = LinearEasing)
        ),
        label = "reno17AuroraPhase"
    )

    // 深色：极光完全可见；浅色：收敛为淡雅薄雾（低透明度、同色系）
    val alphaScale = if (isDark) 1f else 0.35f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val angle = phase * 2f * Math.PI.toFloat()

        // 星光蓝光晕（左上，缓慢横向漂移）
        drawAuroraBlob(
            center = Offset(w * 0.18f + sin(angle) * w * 0.06f, h * 0.12f + cos(angle) * h * 0.03f),
            radius = w * 0.62f,
            color = Reno17Blue.copy(alpha = 0.20f * alphaScale)
        )
        // 极光紫光晕（右侧，反相呼吸）
        drawAuroraBlob(
            center = Offset(w * 0.88f + cos(angle) * w * 0.05f, h * 0.42f + sin(angle) * h * 0.04f),
            radius = w * 0.55f,
            color = Reno17Violet.copy(alpha = 0.16f * alphaScale)
        )
        // 极光青光晕（左下）
        drawAuroraBlob(
            center = Offset(w * 0.30f + cos(angle * 0.8f) * w * 0.05f, h * 0.92f + sin(angle * 0.8f) * h * 0.02f),
            radius = w * 0.58f,
            color = Reno17Aqua.copy(alpha = 0.12f * alphaScale)
        )

        // 顶部一道细高光带（Reno17 玻璃边缘反光）
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f * alphaScale),
                    Color.Transparent
                )
            ),
            size = Size(w, h * 0.22f)
        )
    }
}

/** 单个极光光晕：多层递减透明度实心圆叠加，近似高斯柔光（无着色器开销） */
private fun DrawScope.drawAuroraBlob(center: Offset, radius: Float, color: Color) {
    if (radius <= 0f) return
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(color = color.copy(alpha = color.alpha * 0.55f), radius = radius * 0.72f, center = center)
    drawCircle(color = color.copy(alpha = color.alpha * 0.30f), radius = radius * 0.48f, center = center)
}

/**
 * Reno17 玻璃面：半透明填充 + 蓝调棱边（与 [themedCardBackground] / [themedBorderLight] 同源）。
 *
 * 适用于 Card 之外的裸容器（Box / Column / Surface）。
 * Card 请使用 `CardDefaults.cardColors(containerColor = themedCardBackground())`
 * 配合 `.border(1.dp, themedBorderLight(), shape)`（全站卡片已统一）。
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    fillColor: Color = themedCardBackground(),
    borderColor: Color = themedBorderLight()
): Modifier = this
    .background(color = fillColor, shape = shape)
    .border(width = 1.dp, color = borderColor, shape = shape)

/**
 * Reno17 玻璃扫光：一道斜向高光缓慢掠过玻璃面（Reno17 背板流光质感）。
 *
 * 仅用于少量"主角"表面（关于页标题区、首页头卡等），避免全屏扫光造成视觉噪音与功耗。
 * 纯绘制（drawWithContent），不参与布局测量。
 */
@Composable
fun Modifier.glassSheen(
    shape: Shape,
    cycleMillis: Int = 5200,
    highlightColor: Color = Color.White.copy(alpha = 0.12f),
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "reno17Sheen")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMillis, easing = LinearEasing)
        ),
        label = "reno17SheenPhase"
    )
    return this
        .clip(shape)
        .drawWithContent {
            drawContent()
            val bandWidth = size.width * 0.45f
            val startX = -bandWidth + t * (size.width + bandWidth * 2f)
            val brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
                start = Offset(startX, 0f),
                end = Offset(startX + bandWidth, size.height)
            )
            drawRect(brush = brush)
        }
}
