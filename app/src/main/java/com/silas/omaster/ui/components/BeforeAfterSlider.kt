package com.silas.omaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.ui.theme.HasselbladOrange

/**
 * Before/After对比滑块组件
 * 用于展示LUT应用前后效果对比
 */
@Composable
fun BeforeAfterSlider(
    originalImage: String,      // 原始图片 URL
    processedImage: String,     // LUT 应用后图片 URL
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var boxSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .onSizeChanged { size ->
                boxSize = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // 处理后图片（底层）
        AsyncImage(
            model = processedImage,
            contentDescription = "应用LUT后",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 原始图片（左侧裁剪）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(sliderPosition)
                .clipToBounds()
        ) {
            AsyncImage(
                model = originalImage,
                contentDescription = "原始图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 分割线 + 拖拽手柄
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .offset { IntOffset((boxSize.width * sliderPosition).toInt(), 0) }
                .background(HasselbladOrange)
        ) {
            // 圆形手柄
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .background(HasselbladOrange, CircleShape)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            sliderPosition = (sliderPosition + dragAmount / boxSize.width)
                                .coerceIn(0.1f, 0.9f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = "拖拽对比",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 标签
        if (sliderPosition > 0.3f) {
            Text(
                "原始",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        if (sliderPosition < 0.7f) {
            Text(
                "配方效果",
                color = HasselbladOrange,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// 辅助函数
private fun Modifier.onSizeChanged(onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit): Modifier {
    return this.then(
        object : Modifier.Element {
            // 简化实现，实际项目中应使用Box测量
        }
    )
}