package com.silas.omaster.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Layer 4: 大师洞察层 - 端到端大师工作流
 * 
 * 场景→胶片→参数的端到端工作流
 * 这是「哈苏大师之眼」区别于普通场景识别的最核心差异
 * 
 * 流程：
 * 1. 哈苏之眼（颜色直方图 + EXIF + 人脸检测）
 * 2. 智能胶片推荐（场景→胶片映射表）
 * 3. 哈苏参数优化（HasselbladParams 映射）
 * 4. 大师拍摄建议（构图、光线、焦段建议）
 * 5. 配方保存/分享（场景+胶片+参数 = 可分享配方）
 */

/**
 * 工作流步骤
 */
data class WorkflowStep(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val status: WorkflowStatus = WorkflowStatus.PENDING,
    val result: String? = null
)

enum class WorkflowStatus {
    PENDING, PROCESSING, COMPLETED
}

/**
 * 工作流结果
 */
data class WorkflowResult(
    val sceneId: String,
    val sceneName: String,
    val confidence: Float,
    val recommendedFilm: String,
    val hasselbladParams: Map<String, Int>,
    val masterTips: List<String>
)

/**
 * 大师工作流组件
 */
@Composable
fun MasterWorkflow(
    imageUrl: String? = null,
    onComplete: (WorkflowResult) -> Unit = {}
) {
    val defaultSteps = remember {
        listOf(
            WorkflowStep(
                id = "scene",
                icon = Icons.Default.Camera,
                title = "哈苏之眼",
                description = "颜色直方图 + EXIF + 人脸检测"
            ),
            WorkflowStep(
                id = "film",
                icon = Icons.Default.Movie,
                title = "智能胶片推荐",
                description = "场景→胶片映射表"
            ),
            WorkflowStep(
                id = "params",
                icon = Icons.Default.Tune,
                title = "哈苏参数优化",
                description = "HNCS 色彩科学"
            ),
            WorkflowStep(
                id = "tips",
                icon = Icons.Default.Lightbulb,
                title = "大师拍摄建议",
                description = "哈苏大师赛级别指导"
            ),
            WorkflowStep(
                id = "save",
                icon = Icons.Default.Share,
                title = "配方保存/分享",
                description = "可分享的胶片配方"
            )
        )
    }

    var steps by remember { mutableStateOf(defaultSteps) }
    var currentStepIndex by remember { mutableIntStateOf(-1) }
    var isComplete by remember { mutableStateOf(false) }
    var workflowResult by remember { mutableStateOf<WorkflowResult?>(null) }

    val scope = rememberCoroutineScope()

    // 启动工作流
    LaunchedEffect(imageUrl) {
        for (i in steps.indices) {
            currentStepIndex = i

            // 更新当前步骤为处理中
            steps = steps.mapIndexed { idx, step ->
                if (idx == i) step.copy(status = WorkflowStatus.PROCESSING)
                else step
            }

            // 模拟处理时间
            delay(800 + (Math.random() * 400).toLong())

            // 更新步骤结果
            val result = getStepResult(i)
            steps = steps.mapIndexed { idx, step ->
                if (idx == i) step.copy(status = WorkflowStatus.COMPLETED, result = result)
                else step
            }
        }

        // 工作流完成
        currentStepIndex = -1
        isComplete = true

        // 构建最终结果
        workflowResult = WorkflowResult(
            sceneId = "landscape-sunset",
            sceneName = "日落",
            confidence = 0.92f,
            recommendedFilm = "RDP3 正片",
            hasselbladParams = mapOf(
                "tone" to -5,
                "saturation" to 25,
                "contrast" to 10,
                "colorTemp" to 20,
                "sharpness" to 12,
                "vignette" to 0,
                "cyanMagenta" to 5
            ),
            masterTips = listOf(
                "黄金时刻出片率最高",
                "利用前景增加层次感",
                "试试 XPAN 宽幅模式",
                "浓郁胶片让色彩更鲜活"
            )
        )

        onComplete(workflowResult!!)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 顶部标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "哈苏大师工作流",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "场景 → 胶片 → 参数 → 建议 → 配方",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 工作流步骤
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, step ->
                WorkflowStepItem(
                    step = step,
                    index = index,
                    isLast = index == steps.lastIndex
                )
            }
        }

        // 完成状态
        AnimatedVisibility(
            visible = isComplete && workflowResult != null,
            enter = fadeIn() + slideInVertically()
        ) {
            workflowResult?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HasselbladOrange.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, HasselbladOrange.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "工作流完成",
                                color = HasselbladOrange,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 结果摘要
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "场景：",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = result.sceneName,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = " (${(result.confidence * 100).toInt()}%)",
                                color = HasselbladOrange,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "胶片：",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = result.recommendedFilm,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "参数：",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "影调${result.hasselbladParams["tone"]} 饱和度+${result.hasselbladParams["saturation"]}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
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
 * 工作流步骤项
 */
@Composable
private fun WorkflowStepItem(
    step: WorkflowStep,
    index: Int,
    isLast: Boolean
) {
    val backgroundColor = when (step.status) {
        WorkflowStatus.COMPLETED -> HasselbladOrange.copy(alpha = 0.1f)
        WorkflowStatus.PROCESSING -> Color.White.copy(alpha = 0.05f)
        WorkflowStatus.PENDING -> Color.White.copy(alpha = 0.05f)
    }

    val borderColor = when (step.status) {
        WorkflowStatus.COMPLETED -> HasselbladOrange.copy(alpha = 0.3f)
        WorkflowStatus.PROCESSING -> HasselbladOrange.copy(alpha = 0.5f)
        WorkflowStatus.PENDING -> Color.White.copy(alpha = 0.05f)
    }

    val iconColor = when (step.status) {
        WorkflowStatus.COMPLETED -> HasselbladOrange
        WorkflowStatus.PROCESSING -> Color.White
        WorkflowStatus.PENDING -> Color.White.copy(alpha = 0.5f)
    }

    val textColor = when (step.status) {
        WorkflowStatus.COMPLETED -> HasselbladOrange
        WorkflowStatus.PROCESSING -> Color.White
        WorkflowStatus.PENDING -> Color.White.copy(alpha = 0.5f)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 连接线
        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 32.dp)
                    .width(1.dp)
                    .height(12.dp)
                    .background(
                        if (step.status == WorkflowStatus.COMPLETED) HasselbladOrange
                        else Color.White.copy(alpha = 0.1f)
                    )
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 步骤序号/状态图标
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (step.status) {
                                WorkflowStatus.COMPLETED -> HasselbladOrange
                                WorkflowStatus.PROCESSING -> HasselbladOrange.copy(alpha = 0.3f)
                                WorkflowStatus.PENDING -> Color.White.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (step.status) {
                        WorkflowStatus.COMPLETED -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        WorkflowStatus.PROCESSING -> CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.size(16.dp),
                            color = HasselbladOrange,
                            strokeWidth = 2.dp,
                            trackColor = HasselbladOrange.copy(alpha = 0.3f)
                        )
                        WorkflowStatus.PENDING -> Text(
                            text = "${index + 1}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 步骤内容
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step.title,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = step.description,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )

                    // 步骤结果
                    step.result?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            color = HasselbladOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 获取步骤结果
 */
private fun getStepResult(stepIndex: Int): String {
    return when (stepIndex) {
        0 -> "日落 · 置信度 92%"
        1 -> "RDP3 正片 (93%匹配)"
        2 -> "影调-5 饱和度+25 色温+20"
        3 -> "4 条大师建议"
        4 -> "配方已生成"
        else -> ""
    }
}