package com.silas.omaster.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 骨架屏闪烁动画组件集
 * 用于列表、卡片、详情等加载状态
 */

/**
 * 闪烁动画渐变刷
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.3f)
        ),
        start = Offset(translateAnim - 200, translateAnim - 200),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * 骨架屏占位块
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush())
    )
}

/**
 * 骨架屏预设卡片（模拟 PresetCard 布局）
 */
@Composable
fun ShimmerPresetCard(
    imageHeight: Int = 200,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // 图片占位
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight.dp),
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 标题占位
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp),
            cornerRadius = 4.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 作者占位
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp),
            cornerRadius = 4.dp
        )
    }
}

/**
 * 骨架屏预设网格（双列瀑布流）
 */
@Composable
fun ShimmerPresetGrid(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        )
    ) {
        items(itemCount) { index ->
            val imageHeight = when (index % 3) {
                0 -> 220
                1 -> 180
                else -> 260
            }
            ShimmerPresetCard(imageHeight = imageHeight)
        }
    }
}

/**
 * 骨架屏详情页
 */
@Composable
fun ShimmerDetailPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 图片占位
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            cornerRadius = 16.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标题占位
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp),
            cornerRadius = 4.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 作者 + 标签占位
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp),
                cornerRadius = 10.dp
            )
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(20.dp),
                cornerRadius = 10.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 参数区占位
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 4.dp),
                cornerRadius = 8.dp
            )
        }
    }
}