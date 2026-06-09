package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 哈苏 Before/After 滑杆对比组件
 * 关键交互——让用户直观看到 AI 优化前后的差异
 *
 * 设计规范：
 * - 哈苏橙滑杆轨道 + 白色手柄
 * - 滑杆手柄带哈苏橙边框
 * - 拖拽交互流畅
 */
@Composable
fun HasselbladCompareSlider(
    originalImageUrl: String,
    processedImageUrl: String,
    modifier: Modifier = Modifier,
    onPositionChange: (Float) -> Unit = {}
) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp))
            .background(HasselbladTheme.PureBlack)
    ) {
        // After 图层（全图显示）
        AsyncImage(
            model = processedImageUrl,
            contentDescription = "哈苏优化后",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Before 图层（左侧裁剪）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    // 使用 translationX 实现裁剪效果
                    translationX = -(size.width * (1f - sliderPosition))
                    // 只显示左侧部分
                    alpha = if (sliderPosition > 0.01f) 1f else 0f
                }
        ) {
            AsyncImage(
                model = originalImageUrl,
                contentDescription = "原始照片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 滑杆线
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .width(1.dp)
                .offset { IntOffset(x = (sliderPosition * (size.width - 1.dp.toPx())).toInt(), y = 0) }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HasselbladTheme.HasselbladOrange.copy(alpha = 0.3f),
                            HasselbladTheme.HasselbladOrange,
                            HasselbladTheme.HasselbladOrange.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            // 滑杆手柄
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .shadow(8.dp, CircleShape)
                    .background(HasselbladTheme.SliderThumb, CircleShape)
                    .padding(2.dp)
                    .background(HasselbladTheme.HasselbladOrange, CircleShape)
                    .padding(4.dp)
                    .background(HasselbladTheme.SliderThumb, CircleShape)
            ) {
                // 手柄图标（左右箭头）
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◀",
                        color = HasselbladTheme.HasselbladOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "▶",
                        color = HasselbladTheme.HasselbladOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 拖拽区域（整个组件）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newPosition = sliderPosition + (dragAmount.x / size.width)
                            sliderPosition = newPosition.coerceIn(0.05f, 0.95f)
                            onPositionChange(sliderPosition)
                        }
                    )
                }
        )

        // 标签显示
        if (!isDragging) {
            // Before 标签
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .graphicsLayer {
                        alpha = if (sliderPosition > 0.15f) 1f else 0f
                    },
                shape = RoundedCornerShape(8.dp),
                color = HasselbladTheme.CardBackground.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "Before",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = HasselbladTheme.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // After 标签
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .graphicsLayer {
                        alpha = if (sliderPosition < 0.85f) 1f else 0f
                    },
                shape = RoundedCornerShape(8.dp),
                color = HasselbladTheme.CardBackground.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "After",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = HasselbladTheme.HasselbladOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 简化版对比滑杆（使用两张图片直接对比）
 */
@Composable
fun HasselbladSimpleCompareSlider(
    originalImageRes: String,
    processedImageRes: String,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        val widthPx = constraints.maxWidth

        // After 图层（全图）
        AsyncImage(
            model = processedImageRes,
            contentDescription = "处理后",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Before 图层（左侧裁剪）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width((widthPx * sliderPosition).dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(topStart = 16.dp))
        ) {
            AsyncImage(
                model = originalImageRes,
                contentDescription = "原始",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 滑杆手柄
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(x = ((sliderPosition - 0.015f) * widthPx).toInt(), y = 0) }
                .fillMaxHeight()
                .width(3.dp)
                .background(HasselbladTheme.HasselbladOrange)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / widthPx
                        sliderPosition = (sliderPosition + delta).coerceIn(0.05f, 0.95f)
                    }
                }
        ) {
            // 手柄圆形
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, HasselbladTheme.HasselbladOrange, CircleShape)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("◀", color = HasselbladTheme.HasselbladOrange, fontSize = 8.sp)
                    Text("▶", color = HasselbladTheme.HasselbladOrange, fontSize = 8.sp)
                }
            }
        }

        // 标签
        if (sliderPosition > 0.2f) {
            Text(
                "Before",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = Color.White,
                fontSize = 11.sp
            )
        }
        if (sliderPosition < 0.8f) {
            Text(
                "After",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = HasselbladTheme.HasselbladOrange,
                fontSize = 11.sp
            )
        }
    }
}

// 辅助函数
private fun IntOffset(x: Int, y: Int): Offset = Offset(x.toFloat(), y.toFloat())

@Composable
private fun Modifier.offset(offset: () -> Offset): Modifier {
    val density = LocalDensity.current
    return this.then(
        Modifier.offset {
            val o = offset()
            IntOffset(x = o.x.toInt(), y = o.y.toInt())
        }
    )
}

private fun IntOffset(x: Int, y: Int): androidx.compose.ui.unit.IntOffset {
    return androidx.compose.ui.unit.IntOffset(x, y)
}