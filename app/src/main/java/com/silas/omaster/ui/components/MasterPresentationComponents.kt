package com.silas.omaster.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.silas.omaster.model.*
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Layer 3: 大师呈现 (Master Presentation)
 * UI组件集合
 *
 * 功能：
 * - Before/After 滑杆对比
 * - 哈苏橙色强调色系统
 * - HNCS 水印
 * - 胶片切换预览
 * - XPAN 宽幅提示
 */

/**
 * Before/After 对比滑杆组件
 */
@Composable
fun BeforeAfterSlider(
    beforeImage: Any,
    afterImage: Any,
    modifier: Modifier = Modifier,
    initialPosition: Float = 0.5f
) {
    var sliderPosition by remember { mutableFloatStateOf(initialPosition) }
    var componentWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { size ->
                componentWidth = size.width.toFloat()
            }
    ) {
        // After 图片（底层）
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(afterImage)
                .crossfade(true)
                .build(),
            contentDescription = "After",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Before 图片（上层，根据滑杆位置裁剪）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(LocalDensity.current) { (componentWidth * sliderPosition).toDp() })
                .clipToBounds()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(beforeImage)
                    .crossfade(true)
                    .build(),
                contentDescription = "Before",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 滑杆分隔线
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White)
                .offset(x = with(LocalDensity.current) { (componentWidth * sliderPosition).toDp() - 1.dp })
        )

        // 滑杆手柄
        Box(
            modifier = Modifier
                .size(48.dp)
                .offset(
                    x = with(LocalDensity.current) { (componentWidth * sliderPosition).toDp() - 24.dp },
                    y = with(LocalDensity.current) { ((componentWidth * 3 / 4) / 2).toDp() - 24.dp }
                )
                .background(Color.White, CircleShape)
                .border(2.dp, HasselbladOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Compare,
                contentDescription = "Compare",
                tint = HasselbladOrange,
                modifier = Modifier.size(24.dp)
            )
        }

        // 拖拽区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPosition = change.position.x / componentWidth
                        sliderPosition = newPosition.coerceIn(0f, 1f)
                    }
                }
        )

        // 标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "原图",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = "大师调色",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .background(HasselbladOrange.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * HNCS 水印组件
 */
@Composable
fun HNCSWatermark(
    modifier: Modifier = Modifier,
    style: WatermarkStyle = WatermarkStyle.DEFAULT
) {
    val watermarkText = when (style) {
        WatermarkStyle.DEFAULT -> "HNCS"
        WatermarkStyle.FULL -> "HASSELBLAD\nNATURAL COLOR SOLUTION"
        WatermarkStyle.MINIMAL -> "H"
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 哈苏橙色装饰线
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(HasselbladOrange)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = watermarkText,
                style = TextStyle(
                    fontSize = if (style == WatermarkStyle.FULL) 8.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    lineHeight = 10.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class WatermarkStyle {
    DEFAULT,    // HNCS
    FULL,       // HASSELBLAD NATURAL COLOR SOLUTION
    MINIMAL     // H
}

/**
 * 胶片风格切换预览条
 */
@Composable
fun FilmRecipePreviewBar(
    selectedFilm: FilmStock,
    onFilmSelected: (FilmStock) -> Unit,
    modifier: Modifier = Modifier
) {
    val films = FilmStock.entries.toTypedArray()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "胶片配方",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(films.size) { index ->
                val film = films[index]
                FilmRecipeItem(
                    film = film,
                    isSelected = film == selectedFilm,
                    onClick = { onFilmSelected(film) }
                )
            }
        }
    }
}

@Composable
private fun FilmRecipeItem(
    film: FilmStock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.2f)
    val backgroundColor = if (isSelected) HasselbladOrange.copy(alpha = 0.1f) else DarkGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        // 胶片品牌标识
        Text(
            text = film.brand.firstOrNull()?.toString() ?: "",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ISO 值
        Text(
            text = "ISO ${film.iso}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 胶片名称
        Text(
            text = film.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) HasselbladOrange else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        // 选中指示器
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(HasselbladOrange, CircleShape)
            )
        }
    }
}

/**
 * XPAN 宽幅模式提示
 */
@Composable
fun XPANAspectRatioIndicator(
    isXPAN: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isXPAN) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // XPAN 图标（简化版）
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(8.dp)
                    .border(1.dp, HasselbladOrange)
            )
            Text(
                text = "XPAN 65:24",
                style = MaterialTheme.typography.labelSmall,
                color = HasselbladOrange,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 哈苏橙色强调按钮
 */
@Composable
fun HasselbladButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HasselbladOrange,
            disabledContainerColor = HasselbladOrange.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 大师参数卡片
 */
@Composable
fun MasterParamCard(
    title: String,
    value: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = HasselbladOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 直方图可视化组件
 */
@Composable
fun HistogramVisualizer(
    histogramData: HistogramData,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    val luminanceData = histogramData.luminance
    val maxValue = luminanceData.maxOrNull()?.toFloat() ?: 1f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / 256f

            luminanceData.forEachIndexed { index, value ->
                val barHeight = (value / maxValue) * size.height
                drawRect(
                    color = HasselbladOrange.copy(alpha = 0.6f),
                    topLeft = Offset(index * barWidth, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }

            // 绘制阴影/高光裁剪警告
            if (histogramData.shadowClipping) {
                drawRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width * 0.1f, size.height)
                )
            }
            if (histogramData.highlightClipping) {
                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(size.width * 0.9f, 0f),
                    size = Size(size.width * 0.1f, size.height)
                )
            }
        }
    }
}

/**
 * 场景标签组件
 */
@Composable
fun SceneHierarchyBadge(
    sceneHierarchy: SceneHierarchy,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 一级场景
        SceneBadge(
            text = sceneHierarchy.primary.displayName,
            icon = sceneHierarchy.primary.icon,
            isPrimary = true
        )

        // 二级场景
        SceneBadge(
            text = sceneHierarchy.secondary.displayName,
            isPrimary = false
        )

        // 三级场景
        SceneBadge(
            text = sceneHierarchy.fine.displayName,
            isPrimary = false,
            isFine = true
        )
    }
}

@Composable
private fun SceneBadge(
    text: String,
    icon: String? = null,
    isPrimary: Boolean = false,
    isFine: Boolean = false
) {
    val backgroundColor = when {
        isPrimary -> HasselbladOrange.copy(alpha = 0.2f)
        isFine -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    val textColor = when {
        isPrimary -> HasselbladOrange
        else -> Color.White.copy(alpha = 0.8f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        icon?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 置信度指示器
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        confidence >= 0.9f -> Color(0xFF4CAF50) // 绿色
        confidence >= 0.7f -> HasselbladOrange
        else -> Color(0xFFFF9800) // 橙色
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // 进度条背景
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { (60.dp * confidence).coerceIn(0.dp, 60.dp) })
                    .background(color, RoundedCornerShape(2.dp))
            )
        }

        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
