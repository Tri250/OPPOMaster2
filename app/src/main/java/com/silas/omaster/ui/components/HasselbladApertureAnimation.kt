package com.silas.omaster.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.delay

/**
 * Layer 3: 大师呈现层 - 哈苏光圈叶片分析动画
 * 
 * 「哈苏大师之眼睁开」设计
 * - 光圈叶片动画：从闭合→旋转→张开
 * - 哈苏橙渐变进度条
 * - 逐步揭示分析步骤
 * - 营造「大师正在观察你的画面」的专业仪式感
 */

/**
 * 分析步骤状态
 */
data class AnalysisStep(
    val id: String,
    val name: String,
    val status: AnalysisStatus = AnalysisStatus.PENDING
)

enum class AnalysisStatus {
    PENDING, PROCESSING, COMPLETED
}

/**
 * 光圈状态
 */
enum class ApertureState {
    CLOSED, ROTATING, OPENING, OPEN
}

/**
 * 哈苏光圈叶片动画组件
 */
@Composable
fun HasselbladApertureAnimation(
    steps: List<AnalysisStep> = defaultAnalysisSteps(),
    onComplete: () -> Unit = {}
) {
    var apertureState by remember { mutableStateOf(ApertureState.CLOSED) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentSteps by remember { mutableStateOf(steps) }
    var currentMessage by remember { mutableStateOf("正在读取光影信息...") }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // 模拟分析过程
    LaunchedEffect(Unit) {
        // Phase 1: 闭合状态 - 初始化
        apertureState = ApertureState.CLOSED
        currentMessage = "正在读取光影信息..."
        delay(500)

        // Phase 2: 旋转状态 - 开始分析
        apertureState = ApertureState.ROTATING
        progress = 10f
        // 旋转动画
        repeat(8) {
            rotationAngle += 22.5f
            delay(100)
        }
        delay(300)

        // Phase 3: 逐步张开 - 分析各步骤
        apertureState = ApertureState.OPENING

        for (i in steps.indices) {
            val stepProgress = 20f + (i * 16f)
            progress = stepProgress

            // 更新当前步骤状态
            currentSteps = currentSteps.mapIndexed { idx, step ->
                if (idx < i) step.copy(status = AnalysisStatus.COMPLETED)
                else if (idx == i) step.copy(status = AnalysisStatus.PROCESSING)
                else step
            }

            currentMessage = "${steps[i].name}中..."
            delay(1200)

            // 标记完成
            currentSteps = currentSteps.mapIndexed { idx, step ->
                if (idx <= i) step.copy(status = AnalysisStatus.COMPLETED)
                else step
            }
        }

        // Phase 4: 完全张开 - 分析完成
        apertureState = ApertureState.OPEN
        progress = 100f
        currentMessage = "哈苏之眼已睁开"
        delay(500)

        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 光圈动画
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            // 外圈
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .then(
                        Modifier.drawBehind {
                            drawCircle(
                                color = HasselbladOrange.copy(alpha = 0.3f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    )
            )

            // 光圈叶片
            ApertureBlades(
                state = apertureState,
                rotation = rotationAngle
            )

            // 中心点
            val centerSize = when (apertureState) {
                ApertureState.OPEN -> 8.dp
                else -> 4.dp
            }
            Box(
                modifier = Modifier
                    .size(centerSize)
                    .clip(CircleShape)
                    .background(HasselbladOrange)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 当前状态文字
        Text(
            text = currentMessage,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 哈苏橙渐变进度条
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                HasselbladOrange,
                                Color(0xFFFF8A50),
                                Color(0xFFFFB366)
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分析进度",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
            Text(
                text = "${progress.toInt()}%",
                color = HasselbladOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 分析步骤列表
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentSteps.forEach { step ->
                AnalysisStepItem(step)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 底部品牌标识
        Text(
            text = "HNCS · HASSELBLAD NATURAL COLOR SOLUTION",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}

/**
 * 光圈叶片绘制
 */
@Composable
private fun ApertureBlades(
    state: ApertureState,
    rotation: Float
) {
    val bladeCount = 8
    val openingFactor = when (state) {
        ApertureState.OPEN -> 0.3f
        ApertureState.OPENING -> 0.5f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .size(128.dp)
            .rotate(rotation)
    ) {
        // 使用Canvas绘制叶片
        // 简化实现：使用多个三角形形状
        for (i in 0 until bladeCount) {
            val angle = (i * 45f)
            val alpha = 0.6f - (i * 0.05f)

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .rotate(angle)
                    .then(
                        Modifier.drawBehind {
                            val bladeWidth = 20.dp.toPx() * openingFactor
                            val bladeLength = 40.dp.toPx() * openingFactor

                            // 绘制叶片形状
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width / 2, size.height / 2)
                                    lineTo(
                                        size.width / 2 + bladeLength,
                                        size.height / 2 - bladeWidth / 2
                                    )
                                    lineTo(
                                        size.width / 2 + bladeLength,
                                        size.height / 2 + bladeWidth / 2
                                    )
                                    close()
                                },
                                color = HasselbladOrange.copy(alpha = alpha)
                            )
                        }
                    )
            )
        }
    }
}

/**
 * 分析步骤项
 */
@Composable
private fun AnalysisStepItem(step: AnalysisStep) {
    val backgroundColor = when (step.status) {
        AnalysisStatus.COMPLETED -> HasselbladOrange.copy(alpha = 0.1f)
        AnalysisStatus.PROCESSING -> Color.White.copy(alpha = 0.05f)
        AnalysisStatus.PENDING -> Color.Transparent
    }

    val textColor = when (step.status) {
        AnalysisStatus.COMPLETED -> HasselbladOrange
        AnalysisStatus.PROCESSING -> Color.White.copy(alpha = 0.8f)
        AnalysisStatus.PENDING -> Color.White.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 状态图标
        when (step.status) {
            AnalysisStatus.COMPLETED -> {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "完成",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnalysisStatus.PROCESSING -> {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(16.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp,
                    trackColor = HasselbladOrange.copy(alpha = 0.3f)
                )
            }
            AnalysisStatus.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .then(
                            Modifier.drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.2f),
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        )
                )
            }
        }

        // 步骤名称
        Text(
            text = step.name,
            color = textColor,
            fontSize = 14.sp
        )

        // 完成标记
        if (step.status == AnalysisStatus.COMPLETED) {
            Text(
                text = "完成",
                color = HasselbladOrange.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 默认分析步骤
 */
fun defaultAnalysisSteps(): List<AnalysisStep> = listOf(
    AnalysisStep("color", "色彩分析"),
    AnalysisStep("light", "光影结构分析"),
    AnalysisStep("scene", "场景匹配"),
    AnalysisStep("film", "胶片推荐"),
    AnalysisStep("params", "哈苏参数优化")
)