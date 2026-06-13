package com.silas.omaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.ai.AISuggestion
import com.silas.omaster.ai.ParamComparison
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.perform
import kotlinx.coroutines.delay

/**
 * FT-001/FT-002/FT-003: AI微调弹窗
 * 显示建议参数对比表，提供"应用""单选""取消"三个按钮
 */
@Composable
fun AIFineTuneDialog(
    aiFineTuneManager: AIFineTuneManager,
    basePresetId: String,
    onDismiss: () -> Unit,
    onApplyAll: () -> Unit,
    onApplySelected: (Set<String>) -> Unit,
    onResetToAI: () -> Unit
) {
    val isProcessing by aiFineTuneManager.isProcessing.collectAsState()
    val suggestion by aiFineTuneManager.suggestedParams.collectAsState()
    val haptic = LocalHapticFeedback.current

    // 选中的参数列表
    val selectedFields = remember { mutableStateListOf<String>() }

    // 加载状态
    var loadingProgress by remember { mutableStateOf(0f) }
    var loadingText by remember { mutableStateOf("正在分析...") }

    // 初始化加载
    LaunchedEffect(basePresetId) {
        loadingProgress = 0f
        for (i in 1..10) {
            delay(150)
            loadingProgress = i / 10f
            loadingText = when (i) {
                in 1..3 -> "分析色彩特征..."
                in 4..6 -> "对比预设参数..."
                in 7..9 -> "生成优化建议..."
                else -> "完成"
            }
        }
        aiFineTuneManager.generateAISuggestion(basePresetId)
    }

    // 更新选中列表
    LaunchedEffect(suggestion) {
        suggestion?.let {
            selectedFields.clear()
            selectedFields.addAll(it.suggestions.filter { s -> s.isSelected }.map { s -> s.field })
        }
    }

    AlertDialog(
        onDismissRequest = {
            haptic.perform(HapticFeedbackType.ToggleOff)
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 微调",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
        },
        text = {
            Column {
                // 加载状态
                if (isProcessing || suggestion == null) {
                    LoadingSection(progress = loadingProgress, text = loadingText)
                } else {
                    // 建议信息
                    suggestion?.let { s ->
                        SuggestionHeader(suggestion = s)
                        Spacer(modifier = Modifier.height(12.dp))
                        // 参数对比表
                        ParamComparisonTable(
                            suggestions = aiFineTuneManager.getParamComparison(),
                            selectedFields = selectedFields,
                            onToggle = { field ->
                                haptic.perform(HapticFeedbackType.SegmentTick)
                                if (selectedFields.contains(field)) {
                                    selectedFields.remove(field)
                                } else {
                                    selectedFields.add(field)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 单选按钮
                OutlinedButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        if (selectedFields.size == 1) {
                            onApplySelected(selectedFields.toSet())
                        }
                    },
                    enabled = selectedFields.size == 1
                ) {
                    Text("应用选中")
                }

                // 全部应用按钮
                Button(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        onApplyAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrange
                    )
                ) {
                    Text("应用全部")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.perform(HapticFeedbackType.ToggleOff)
                onDismiss()
            }) {
                Text("取消")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

/**
 * 加载状态显示
 */
@Composable
private fun LoadingSection(progress: Float, text: String) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(64.dp),
            color = HasselbladOrange,
            strokeWidth = 4.dp,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = HasselbladOrange,
            trackColor = Color.Gray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "基于基础预设派生建议参数",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

/**
 * 建议头部信息
 */
@Composable
private fun SuggestionHeader(suggestion: AISuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HasselbladOrange.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = HasselbladOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "来自: ${suggestion.basePresetName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "${suggestion.suggestions.size} 项参数建议",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

/**
 * FT-001: 参数对比表
 */
@Composable
private fun ParamComparisonTable(
    suggestions: List<ParamComparison>,
    selectedFields: MutableList<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "建议参数",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )

        suggestions.forEach { param ->
            ParamComparisonRow(
                param = param,
                isSelected = selectedFields.contains(param.field),
                onToggle = { onToggle(param.field) }
            )
        }
    }
}

/**
 * FT-002: 单个参数对比行
 */
@Composable
private fun ParamComparisonRow(
    param: ParamComparison,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor by animateFloatAsState(
        targetValue = if (isSelected) HasselbladOrange else Color.Transparent,
        label = "border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .background(if (isSelected) HasselbladOrange.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中状态
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) HasselbladOrange else Color.Transparent)
                .border(2.dp, if (isSelected) HasselbladOrange else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 参数名
        Text(
            text = param.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        // 对比值
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${param.currentValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(12.dp)
                )
                Text(
                    text = "${param.suggestedValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )
            }

            // 差异显示
            val diffText = when {
                param.difference > 0 -> "+${param.difference}"
                param.difference < 0 -> "${param.difference}"
                else -> "0"
            }
            val diffColor = when {
                param.difference > 0 -> Color.Green
                param.difference < 0 -> Color.Red
                else -> Color.Gray
            }
            Text(
                text = diffText,
                style = MaterialTheme.typography.bodySmall,
                color = diffColor
            )
        }
    }
}

/**
 * FT-002: 参数更新高亮提示
 */
@Composable
fun ParamUpdateHighlight(
    field: String,
    newValue: Int,
    onDismiss: () -> Unit
) {
    LaunchedEffect(field) {
        delay(500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(HasselbladOrange.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$field: $newValue",
            style = MaterialTheme.typography.bodySmall,
            color = HasselbladOrange
        )
    }
}
