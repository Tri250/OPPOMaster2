package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.ai.AISuggestionResult
import com.silas.omaster.ai.ColorStyle
import com.silas.omaster.ai.SmartPreset
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

/**
 * AI精调功能页面
 * FT-001/002/003: 一键AI微调、单参数采纳、二次编辑
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIFineTuneScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aiManager = remember { AIFineTuneManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentAdjustments by aiManager.currentAdjustments.collectAsState()
    val isProcessing by aiManager.isProcessing.collectAsState()
    val suggestedParams by aiManager.suggestedParams.collectAsState()
    val manuallyModifiedFields by aiManager.manuallyModifiedFields.collectAsState()
    val errorState by aiManager.errorState.collectAsState()

    var selectedStyleId by remember { mutableStateOf<String?>(currentAdjustments.selectedStyleId) }
    var showAISuggestionDialog by remember { mutableStateOf(false) }
    var selectedPresetId by remember { mutableStateOf("fresh_cc") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = {
                Column {
                    Text("AI精调", fontWeight = FontWeight.Bold)
                    Text(
                        "智能影像精调系统",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
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
            // 一键AI微调按钮
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "一键 AI 微调",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                        Text(
                            text = "基于基础预设智能生成建议参数",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                scope.launch {
                                    val result = aiManager.generateAISuggestion(selectedPresetId)
                                    if (result is AISuggestionResult.Success) {
                                        showAISuggestionDialog = true
                                    }
                                }
                            },
                            enabled = !isProcessing && settingsManager.isAIFineTuneEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始微调")
                            }
                        }
                    }
                }
            }

            // 色彩风格选择
            item {
                Text(
                    text = "色彩风格",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            items(aiManager.colorStyles) { style ->
                ColorStyleItem(
                    style = style,
                    isSelected = selectedStyleId == style.id,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Select)
                        selectedStyleId = style.id
                        scope.launch {
                            aiManager.applyColorStyle(style.id)
                        }
                    }
                )
            }

            // 智能优化预设
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "智能优化",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            items(aiManager.smartPresets) { preset ->
                SmartPresetItem(
                    preset = preset,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        scope.launch {
                            aiManager.applySmartPreset(preset.id)
                        }
                    }
                )
            }

            // 当前参数显示
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "当前参数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                CurrentParamsCard(
                    params = currentAdjustments,
                    manuallyModifiedFields = manuallyModifiedFields,
                    onReset = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        aiManager.resetAdjustments()
                    }
                )
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // AI建议弹窗
    if (showAISuggestionDialog && suggestedParams != null) {
        AISuggestionDialog(
            suggestion = suggestedParams!!,
            onApplyAll = {
                haptic.perform(HapticFeedbackType.Confirm)
                aiManager.applySelectedSuggestions(
                    suggestedParams!!.suggestions.map { it.field }.toSet()
                )
                showAISuggestionDialog = false
            },
            onApplySelected = { selectedFields ->
                haptic.perform(HapticFeedbackType.Confirm)
                aiManager.applySelectedSuggestions(selectedFields)
                showAISuggestionDialog = false
            },
            onDismiss = {
                haptic.perform(HapticFeedbackType.ToggleOff)
                aiManager.clearSuggestion()
                showAISuggestionDialog = false
            }
        )
    }
}

@Composable
private fun ColorStyleItem(
    style: ColorStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = style.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) HasselbladOrange else Color.White
                )
                Text(
                    text = style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SmartPresetItem(
    preset: SmartPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.icon,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CurrentParamsCard(
    params: com.silas.omaster.ai.AdjustmentParams,
    manuallyModifiedFields: Set<String>,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "参数值",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                TextButton(onClick = onReset) {
                    Text("重置", color = HasselbladOrange)
                }
            }

            val paramList = listOf(
                "饱和度" to params.saturation,
                "对比度" to params.contrast,
                "亮度" to params.brightness,
                "冷暖" to params.warmth,
                "清晰度" to params.clarity,
                "锐度" to params.sharpness
            )

            paramList.forEach { (name, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        // AI标签或手动修改标记
                        if (manuallyModifiedFields.contains(name.lowercase())) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (value != 0) HasselbladOrange else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun AISuggestionDialog(
    suggestion: com.silas.omaster.ai.AISuggestion,
    onApplyAll: () -> Unit,
    onApplySelected: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedFields = remember { mutableStateListOf<String>() }

    LaunchedEffect(suggestion) {
        selectedFields.clear()
        selectedFields.addAll(suggestion.suggestions.filter { it.isSelected }.map { it.field })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI 建议参数",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "来自: ${suggestion.basePresetName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladOrange,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (suggestion.isOfflineMode) {
                    Text(
                        text = "(本地优化模式)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                suggestion.suggestions.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedFields.contains(s.field)) HasselbladOrange.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFields.contains(s.field),
                            onCheckedChange = { checked ->
                                if (checked) selectedFields.add(s.field)
                                else selectedFields.remove(s.field)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = HasselbladOrange,
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${s.currentValue} → ${s.suggestedValue}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onApplySelected(selectedFields.toSet()) },
                        enabled = selectedFields.size == 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("应用选中")
                    }
                    Button(
                        onClick = onApplyAll,
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("应用全部")
                    }
                }
            }
        }
    }
}