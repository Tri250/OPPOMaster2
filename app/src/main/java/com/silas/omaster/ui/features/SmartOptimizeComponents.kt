package com.silas.omaster.ui.features

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

// ==================== 色调曲线编辑器 ====================

@Composable
fun ToneCurveEditor(
    points: List<CurvePoint>,
    onPointsChanged: (List<CurvePoint>) -> Unit,
    modifier: Modifier = Modifier,
    channelLabel: String = "RGB",
    channelColor: Color = Color.White
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "色调曲线 - $channelLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, Color(0xFF2A2A4E), RoundedCornerShape(8.dp))
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val newX = (offset.x / w).coerceIn(0f, 1f)
                        val newY = 1f - (offset.y / h).coerceIn(0f, 1f)
                        val newPoints = (points + CurvePoint(newX, newY))
                            .sortedBy { it.x }
                        onPointsChanged(newPoints)
                    }
                }
                .pointerInput(points) {
                    detectDragGestures { change, _ ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val newX = (change.position.x / w).coerceIn(0f, 1f)
                        val newY = 1f - (change.position.y / h).coerceIn(0f, 1f)
                        val nearestIdx = points.indices.minByOrNull {
                            abs(points[it].x - newX)
                        } ?: return@detectDragGestures
                        val updated = points.toMutableList()
                        updated[nearestIdx] = CurvePoint(
                            updated[nearestIdx].x,
                            newY.coerceIn(0f, 1f)
                        )
                        onPointsChanged(updated.sortedBy { it.x })
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 网格
                val gridColor = Color(0xFF2A2A4E)
                for (i in 1..3) {
                    val x = w * i / 4f
                    val y = h * i / 4f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // 对角线
                drawLine(Color(0xFF3A3A5E), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                // 曲线
                if (points.size >= 2) {
                    val path = Path()
                    val sorted = points.sortedBy { it.x }
                    path.moveTo(0f, h)
                    sorted.forEach { pt ->
                        path.lineTo(pt.x * w, (1f - pt.y) * h)
                    }
                    path.lineTo(w, h * 0f)

                    drawPath(
                        path = path,
                        color = channelColor.copy(alpha = 0.3f)
                    )

                    // 曲线线
                    for (i in 0 until sorted.size - 1) {
                        drawLine(
                            color = channelColor,
                            start = Offset(sorted[i].x * w, (1f - sorted[i].y) * h),
                            end = Offset(sorted[i + 1].x * w, (1f - sorted[i + 1].y) * h),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 控制点
                    sorted.forEach { pt ->
                        drawCircle(
                            color = channelColor,
                            radius = 6f,
                            center = Offset(pt.x * w, (1f - pt.y) * h)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(pt.x * w, (1f - pt.y) * h)
                        )
                    }
                }
            }
        }

        // 重置按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                onPointsChanged(listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)))
            }) {
                Text("重置", fontSize = 12.sp)
            }
            TextButton(onClick = {
                onPointsChanged(listOf(
                    CurvePoint(0f, 0f), CurvePoint(0.25f, 0.25f),
                    CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.75f),
                    CurvePoint(1f, 1f)
                ))
            }) {
                Text("线性", fontSize = 12.sp)
            }
            TextButton(onClick = {
                onPointsChanged(listOf(
                    CurvePoint(0f, 0f), CurvePoint(0.25f, 0.15f),
                    CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.85f),
                    CurvePoint(1f, 1f)
                ))
            }) {
                Text("S曲线", fontSize = 12.sp)
            }
        }
    }
}

// ==================== 参数曲线面板 ====================

@Composable
fun ParametricCurvePanel(
    curve: ParametricCurveData,
    onCurveChanged: (ParametricCurveData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("参数曲线", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp))

        LabeledSlider("高光", curve.highlights, -100f, 100f, onValueChange = {
            onCurveChanged(curve.copy(highlights = it))
        })
        LabeledSlider("亮调", curve.lights, -100f, 100f, onValueChange = {
            onCurveChanged(curve.copy(lights = it))
        })
        LabeledSlider("暗调", curve.darks, -100f, 100f, onValueChange = {
            onCurveChanged(curve.copy(darks = it))
        })
        LabeledSlider("阴影", curve.shadows, -100f, 100f, onValueChange = {
            onCurveChanged(curve.copy(shadows = it))
        })
    }
}

// ==================== 色彩分级色轮 ====================

@Composable
fun ColorWheelPanel(
    wheel: ColorWheelValue,
    onWheelChanged: (ColorWheelValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    wheelSize: Float = 120f
) {
    val density = LocalDensity.current
    val sizePx = with(density) { wheelSize.dp.toPx() }
    val maxRadiusPx = sizePx / 2f - 10f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Box(
            modifier = Modifier
                .size(wheelSize.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val cx = sizePx / 2f
                        val cy = sizePx / 2f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val hue = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f)
                            .let { if (it < 0f) it + 360f else it }
                        val sat = (dist / maxRadiusPx * 100f).coerceIn(0f, 100f)
                        onWheelChanged(wheel.copy(hue = hue, saturation = sat))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cx = sizePx / 2f
                        val cy = sizePx / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = sqrt(dx * dx + dy * dy)
                        val hue = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f)
                            .let { if (it < 0f) it + 360f else it }
                        val sat = (dist / maxRadiusPx * 100f).coerceIn(0f, 100f)
                        onWheelChanged(wheel.copy(hue = hue, saturation = sat))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 10f

                // 色轮背景
                for (angle in 0..359 step 3) {
                    val rad = Math.toRadians(angle.toDouble())
                    val hueColor = Color.hsl(angle.toFloat(), 1f, 0.5f)
                    drawArc(
                        color = hueColor,
                        startAngle = angle.toFloat() - 1.5f,
                        sweepAngle = 3f,
                        useCenter = true,
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                }

                // 中心白色渐变
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                        center = center,
                        radius = radius * 0.7f
                    ),
                    radius = radius * 0.7f,
                    center = center
                )

                // 当前选择点
                if (wheel.saturation > 0.01f) {
                    val hueRad = Math.toRadians(wheel.hue.toDouble())
                    val r = radius * (wheel.saturation / 100f)
                    val px = center.x + r * sin(hueRad).toFloat()
                    val py = center.y - r * cos(hueRad).toFloat()

                    drawCircle(Color.White, 5f, Offset(px, py))
                    drawCircle(Color.Black, 7f, Offset(px, py), style = Stroke(2f))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 亮度滑块
        Slider(
            value = wheel.luminance,
            onValueChange = { onWheelChanged(wheel.copy(luminance = it)) },
            valueRange = -100f..100f,
            modifier = Modifier.width(wheelSize.dp)
        )

        // 重置
        TextButton(
            onClick = { onWheelChanged(ColorWheelValue()) },
            contentPadding = PaddingValues(4.dp)
        ) {
            Text("重置", fontSize = 10.sp)
        }
    }
}

// ==================== HSL 面板 ====================

@Composable
fun HSLPanel(
    hsl: HSLAdjustments,
    onHSLChanged: (HSLAdjustments) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        "红" to Color(0xFFFF4444),
        "橙" to Color(0xFFFF8800),
        "黄" to Color(0xFFFFCC00),
        "绿" to Color(0xFF44CC44),
        "青" to Color(0xFF44CCCC),
        "蓝" to Color(0xFF4444FF),
        "紫" to Color(0xFF8844CC),
        "品红" to Color(0xFFFF44CC)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // 通道选择器
        var selectedChannel by remember { mutableIntStateOf(0) }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { (name, color) ->
                val idx = colors.indexOf(name to color)
                val isSelected = idx == selectedChannel
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedChannel = idx },
                    label = { Text(name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.3f),
                        selectedLabelColor = color
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 获取当前通道的值
        val (hue, sat, lum) = getHSLChannelValues(hsl, selectedChannel)
        val (hueChanged, satChanged, lumChanged) = makeHSLChannelUpdaters(hsl, onHSLChanged, selectedChannel)

        LabeledSlider("色相", hue, -100f, 100f, hueChanged)
        LabeledSlider("饱和度", sat, -100f, 100f, satChanged)
        LabeledSlider("明度", lum, -100f, 100f, lumChanged)
    }
}

private fun getHSLChannelValues(hsl: HSLAdjustments, channel: Int): Triple<Float, Float, Float> = when (channel) {
    0 -> Triple(hsl.redHue, hsl.redSaturation, hsl.redLuminance)
    1 -> Triple(hsl.orangeHue, hsl.orangeSaturation, hsl.orangeLuminance)
    2 -> Triple(hsl.yellowHue, hsl.yellowSaturation, hsl.yellowLuminance)
    3 -> Triple(hsl.greenHue, hsl.greenSaturation, hsl.greenLuminance)
    4 -> Triple(hsl.aquaHue, hsl.aquaSaturation, hsl.aquaLuminance)
    5 -> Triple(hsl.blueHue, hsl.blueSaturation, hsl.blueLuminance)
    6 -> Triple(hsl.purpleHue, hsl.purpleSaturation, hsl.purpleLuminance)
    7 -> Triple(hsl.magentaHue, hsl.magentaSaturation, hsl.magentaLuminance)
    else -> Triple(0f, 0f, 0f)
}

private fun makeHSLChannelUpdaters(
    hsl: HSLAdjustments,
    onChanged: (HSLAdjustments) -> Unit,
    channel: Int
): Triple<(Float) -> Unit, (Float) -> Unit, (Float) -> Unit> {
    fun update(h: Float? = null, s: Float? = null, l: Float? = null) {
        onChanged(when (channel) {
            0 -> hsl.copy(redHue = h ?: hsl.redHue, redSaturation = s ?: hsl.redSaturation, redLuminance = l ?: hsl.redLuminance)
            1 -> hsl.copy(orangeHue = h ?: hsl.orangeHue, orangeSaturation = s ?: hsl.orangeSaturation, orangeLuminance = l ?: hsl.orangeLuminance)
            2 -> hsl.copy(yellowHue = h ?: hsl.yellowHue, yellowSaturation = s ?: hsl.yellowSaturation, yellowLuminance = l ?: hsl.yellowLuminance)
            3 -> hsl.copy(greenHue = h ?: hsl.greenHue, greenSaturation = s ?: hsl.greenSaturation, greenLuminance = l ?: hsl.greenLuminance)
            4 -> hsl.copy(aquaHue = h ?: hsl.aquaHue, aquaSaturation = s ?: hsl.aquaSaturation, aquaLuminance = l ?: hsl.aquaLuminance)
            5 -> hsl.copy(blueHue = h ?: hsl.blueHue, blueSaturation = s ?: hsl.blueSaturation, blueLuminance = l ?: hsl.blueLuminance)
            6 -> hsl.copy(purpleHue = h ?: hsl.purpleHue, purpleSaturation = s ?: hsl.purpleSaturation, purpleLuminance = l ?: hsl.purpleLuminance)
            7 -> hsl.copy(magentaHue = h ?: hsl.magentaHue, magentaSaturation = s ?: hsl.magentaSaturation, magentaLuminance = l ?: hsl.magentaLuminance)
            else -> hsl
        })
    }
    return Triple(
        { h -> update(h = h) },
        { s -> update(s = s) },
        { l -> update(l = l) }
    )
}

// ==================== 直方图视图 ====================

@Composable
fun HistogramView(
    histogram: HistogramFullResult?,
    modifier: Modifier = Modifier,
    mode: String = "RGB"
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, Color(0xFF2A2A4E), RoundedCornerShape(6.dp))
        ) {
            if (histogram != null) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val w = size.width
                    val h = size.height
                    val maxVal = maxOf(
                        histogram.red.maxOrNull() ?: 1,
                        histogram.green.maxOrNull() ?: 1,
                        histogram.blue.maxOrNull() ?: 1,
                        histogram.luma.maxOrNull() ?: 1
                    ).toFloat().coerceAtLeast(1f)

                    val barWidth = w / 256f

                    when (mode) {
                        "RGB" -> {
                            for (i in 0..255) {
                                val rH = (histogram.red[i] / maxVal * h)
                                val gH = (histogram.green[i] / maxVal * h)
                                val bH = (histogram.blue[i] / maxVal * h)

                                drawLine(Color.Red.copy(alpha = 0.6f),
                                    Offset(i * barWidth, h), Offset(i * barWidth, h - rH), barWidth)
                                drawLine(Color.Green.copy(alpha = 0.6f),
                                    Offset(i * barWidth, h), Offset(i * barWidth, h - gH), barWidth)
                                drawLine(Color.Blue.copy(alpha = 0.6f),
                                    Offset(i * barWidth, h), Offset(i * barWidth, h - bH), barWidth)
                            }
                        }
                        "LUMINANCE" -> {
                            for (i in 0..255) {
                                val lH = (histogram.luma[i] / maxVal * h)
                                drawLine(Color.White.copy(alpha = 0.7f),
                                    Offset(i * barWidth, h), Offset(i * barWidth, h - lH), barWidth)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("加载图像后显示直方图", fontSize = 11.sp,
                        color = Color(0xFF666688))
                }
            }
        }

        // 统计信息
        if (histogram != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("平均", "%.0f".format(histogram.meanLuminance * 255))
                StatChip("动态", "%.1f EV".format(histogram.dynamicRange))
                if (histogram.shadowClipping) StatChip("暗部剪切", "⚠", Color(0xFFFF8800))
                if (histogram.highlightClipping) StatChip("高光剪切", "⚠", Color(0xFFFF4444))
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color = Color(0xFF8888AA)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        Text(label, fontSize = 9.sp, color = Color(0xFF666688))
    }
}

// ==================== 波形监视器 ====================

@Composable
fun WaveformView(
    waveform: WaveformData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111122))
            .border(1.dp, Color(0xFF2A2A4E), RoundedCornerShape(6.dp))
    ) {
        if (waveform != null) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val w = size.width
                val h = size.height
                val maxVal = waveform.maxValue.coerceAtLeast(1f)

                waveform.scanlines.forEachIndexed { col, scanline ->
                    val x = col.toFloat() / waveform.scanlines.size * w
                    for (row in 0..255) {
                        val y = (1f - row / 255f) * h
                        val valNorm = scanline[row] / maxVal
                        if (valNorm > 0.01f) {
                            val alpha = (valNorm * 0.8f).coerceIn(0f, 1f)
                            drawCircle(
                                Color(0xFF44CC88).copy(alpha = alpha),
                                radius = 1.5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("波形监视器", fontSize = 11.sp, color = Color(0xFF666688))
            }
        }
    }
}

// ==================== 通用带标签滑块 ====================

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    rangeStart: Float,
    rangeEnd: Float,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { "%.0f".format(it) },
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(48.dp),
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = { onValueChangeFinished?.invoke() },
            valueRange = rangeStart..rangeEnd,
            enabled = enabled,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Text(
            text = valueFormatter(value),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

// ==================== 预设卡片 ====================

@Composable
fun PresetCard(
    preset: SmartOptimizePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 预设缩略图占位
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (preset.category) {
                            PresetCategory.FILM -> Color(0xFFFF6B35)
                            PresetCategory.CINEMATIC -> Color(0xFF4A90D9)
                            PresetCategory.MONOCHROME -> Color(0xFF666666)
                            PresetCategory.LANDSCAPE -> Color(0xFF4CAF50)
                            PresetCategory.PORTRAIT -> Color(0xFFE91E63)
                            PresetCategory.NIGHT -> Color(0xFF3F51B5)
                            PresetCategory.VINTAGE -> Color(0xFF9C27B0)
                            PresetCategory.MOOD -> Color(0xFFFF9800)
                            PresetCategory.HDR -> Color(0xFF00BCD4)
                            else -> Color(0xFF607D8B)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.category.label,
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = preset.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ==================== 编辑历史项 ====================

@Composable
fun EditHistoryItem(
    entry: EditHistoryEntry,
    isCurrent: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .clickable(onClick = onRestore)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.label.contains("检查点")) Icons.Filled.Bookmark
            else Icons.Filled.History,
            contentDescription = null,
            tint = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.label.ifEmpty { "编辑 #${entry.id.take(6)}" },
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = formatTimestamp(entry.timestamp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${entry.params.changedParamCount()} 参数",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

// ==================== 色彩科学选择器 ====================

@Composable
fun ColorScienceSelector(
    currentMode: String,
    displayColorSpace: String,
    eotf: String,
    peakLuminance: Float,
    onColorScienceChanged: (String) -> Unit,
    onDisplayColorSpaceChanged: (String) -> Unit,
    onEOTFChanged: (String) -> Unit,
    onPeakLuminanceChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("色彩科学", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp))

        // 色彩科学模式
        Text("色彩管线", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorScienceMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode.name,
                    onClick = { onColorScienceChanged(mode.name) },
                    label = { Text(mode.label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 显示色彩空间
        Text("显示色彩空间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DisplayColorSpace.entries.forEach { space ->
                FilterChip(
                    selected = displayColorSpace == space.name,
                    onClick = { onDisplayColorSpaceChanged(space.name) },
                    label = { Text(space.label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // EOTF
        Text("传输函数 (EOTF)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EOTF.entries.forEach { eotfEntry ->
                FilterChip(
                    selected = eotf == eotfEntry.name,
                    onClick = { onEOTFChanged(eotfEntry.name) },
                    label = { Text(eotfEntry.label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LabeledSlider("峰值亮度", peakLuminance, 80f, 4000f,
            onPeakLuminanceChanged, { "${it.toInt()} nits" })
    }
}

// ==================== 导出配置面板 ====================

@Composable
fun ExportConfigPanel(
    config: ExportConfig,
    onConfigChanged: (ExportConfig) -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("导出设置", style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp))

        // 格式
        Text("格式", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportFormat.entries.forEach { fmt ->
                FilterChip(
                    selected = config.format == fmt.value,
                    onClick = { onConfigChanged(config.copy(format = fmt.value)) },
                    label = { Text(fmt.label, fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        LabeledSlider("质量", config.quality.toFloat(), 10f, 100f,
            { onConfigChanged(config.copy(quality = it.toInt())) },
            { "${it.toInt()}%" })

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = config.metadata,
                onCheckedChange = { onConfigChanged(config.copy(metadata = it)) }
            )
            Text("包含元数据", fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("导出图像")
        }
    }
}

// ==================== 集中处理进度条 ====================

@Composable
fun ProcessingProgressBar(
    stage: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ==================== 参数有效指示器 ====================

@Composable
fun ParameterDot(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

// ==================== 比较视图 ====================

@Composable
fun BeforeAfterToggle(
    showBefore: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
    ) {
        FilterChip(
            selected = !showBefore,
            onClick = { onToggle(false) },
            label = { Text("之后", fontSize = 12.sp) },
            modifier = Modifier.height(32.dp)
        )
        FilterChip(
            selected = showBefore,
            onClick = { onToggle(true) },
            label = { Text("之前", fontSize = 12.sp) },
            modifier = Modifier.height(32.dp)
        )
    }
}