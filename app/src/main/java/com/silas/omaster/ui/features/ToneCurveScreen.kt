package com.silas.omaster.ui.features

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.curve.ToneCurveManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform

/**
 * 色调曲线页面
 * 同步 Web 设计：RGB/R/G/B 通道曲线
 * 支持点击添加控制点、拖动调整
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneCurveScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ToneCurveManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    val curves by manager.curves.collectAsState()
    val selectedChannel by manager.selectedChannel.collectAsState()

    val currentCurve = curves[selectedChannel] ?: ToneCurveManager.Curve(selectedChannel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.curve_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.curve_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    manager.resetCurve(selectedChannel)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.curve_reset), tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提示
            item {
                Text(
                    text = stringResource(R.string.curve_drag_hint),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 通道选择
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ToneCurveManager.CurveChannel.entries) { channel ->
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                manager.selectChannel(channel)
                            },
                            label = { Text(channel.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                                selectedLabelColor = HasselbladOrange
                            )
                        )
                    }
                }
            }

            // 曲线画布
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedChannel.displayName + " 通道",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CurveCanvas(
                            controlPoints = currentCurve.controlPoints,
                            curveColor = when (selectedChannel) {
                                ToneCurveManager.CurveChannel.RGB -> Color.White
                                ToneCurveManager.CurveChannel.RED -> Color(0xFFE53935)
                                ToneCurveManager.CurveChannel.GREEN -> Color(0xFF43A047)
                                ToneCurveManager.CurveChannel.BLUE -> Color(0xFF1E88E5)
                            },
                            onPointMove = { index, newPoint ->
                                manager.moveControlPoint(selectedChannel, index, newPoint)
                            },
                            onAddPoint = { point ->
                                manager.addControlPoint(selectedChannel, point)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 显示当前控制点数量
                        Text(
                            text = "${currentCurve.controlPoints.size} 个控制点",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 预设
            item {
                Text(
                    text = stringResource(R.string.curve_presets),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(manager.curvePresets) { preset ->
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable {
                                    haptic.perform(HapticFeedbackType.Confirm)
                                    manager.applyPreset(preset)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = preset.name,
                                    color = HasselbladOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.description,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // 操作按钮
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            manager.resetAll()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.curve_reset))
                    }
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            onBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Text(stringResource(R.string.hsl_apply), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurveCanvas(
    controlPoints: List<ToneCurveManager.ControlPoint>,
    curveColor: Color,
    onPointMove: (Int, ToneCurveManager.ControlPoint) -> Unit,
    onAddPoint: (ToneCurveManager.ControlPoint) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .pointerInput(controlPoints) {
                detectTapGestures(
                    onTap = { offset ->
                        // 点击时添加控制点
                        val size = this.size
                        val x = (offset.x / size.width).coerceIn(0f, 1f)
                        val y = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        onAddPoint(ToneCurveManager.ControlPoint(x, y))
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controlPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // 找到最近的控制点
                            val size = this.size
                            val posX = offset.x / size.width
                            val posY = 1f - offset.y / size.height
                            val closestIndex = controlPoints.indices.minByOrNull { i ->
                                val p = controlPoints[i]
                                val dx = p.x - posX
                                val dy = p.y - posY
                                dx * dx + dy * dy
                            } ?: -1
                            // 暂存 - 通过 onPointMove 立即响应
                            if (closestIndex >= 0) {
                                val p = controlPoints[closestIndex]
                                if (kotlin.math.abs(p.x - posX) < 0.1f && kotlin.math.abs(p.y - posY) < 0.1f) {
                                    activePointIndex = closestIndex
                                }
                            }
                        }
                    ) { change, _ ->
                        val size = this.size
                        val posX = (change.position.x / size.width).coerceIn(0f, 1f)
                        val posY = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        if (activePointIndex >= 0) {
                            onPointMove(activePointIndex, ToneCurveManager.ControlPoint(posX, posY))
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padding = 20f

            // 网格线
            for (i in 0..4) {
                val pos = padding + (w - 2 * padding) * i / 4
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(pos, padding),
                    end = Offset(pos, h - padding),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(padding, pos),
                    end = Offset(w - padding, pos),
                    strokeWidth = 1f
                )
            }

            // 对角线
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(padding, h - padding),
                end = Offset(w - padding, padding),
                strokeWidth = 1.5f
            )

            // 曲线
            if (controlPoints.size >= 2) {
                val path = Path()
                val sorted = controlPoints.sortedBy { it.x }
                sorted.forEachIndexed { index, p ->
                    val x = padding + (w - 2 * padding) * p.x
                    val y = h - padding - (h - 2 * padding) * p.y
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prev = sorted[index - 1]
                        val prevX = padding + (w - 2 * padding) * prev.x
                        val prevY = h - padding - (h - 2 * padding) * prev.y
                        // 平滑曲线
                        val midX = (prevX + x) / 2
                        path.cubicTo(midX, prevY, midX, y, x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // 控制点
            controlPoints.forEach { p ->
                val x = padding + (w - 2 * padding) * p.x
                val y = h - padding - (h - 2 * padding) * p.y
                drawCircle(
                    color = curveColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

// 活跃控制点索引（简化版拖动）
private var activePointIndex: Int = -1
