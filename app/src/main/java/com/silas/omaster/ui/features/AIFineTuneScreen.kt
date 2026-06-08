package com.silas.omaster.ui.features

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.ai.AISuggestionResult
import com.silas.omaster.ai.AdjustmentParams
import com.silas.omaster.ai.AdjustmentType
import com.silas.omaster.ai.ColorStyle
import com.silas.omaster.ai.SmartPreset
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI微调功能页面
 * FT-001/002/003: 一键AI微调、单参数采纳、二次编辑
 * 精细参数滑块 / 实时预览对比 / 参数重置 / 参数保存 / 已保存参数列表
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

    // 精细参数滑块状态
    var saturation by remember { mutableFloatStateOf(currentAdjustments.saturation.toFloat()) }
    var contrast by remember { mutableFloatStateOf(currentAdjustments.contrast.toFloat()) }
    var warmth by remember { mutableFloatStateOf(currentAdjustments.warmth.toFloat()) }
    var sharpness by remember { mutableFloatStateOf(currentAdjustments.sharpness.toFloat()) }

    // 同步外部参数变化到滑块
    LaunchedEffect(currentAdjustments) {
        saturation = currentAdjustments.saturation.toFloat()
        contrast = currentAdjustments.contrast.toFloat()
        warmth = currentAdjustments.warmth.toFloat()
        sharpness = currentAdjustments.sharpness.toFloat()
    }

    // 预览对比分隔线位置 (0f..1f)
    var comparisonSplit by remember { mutableFloatStateOf(0.5f) }

    // 已保存参数列表
    var savedParamsList by remember { mutableStateOf(loadSavedParams(context)) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveParamName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("AI 微调", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                // 重置参数按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    aiManager.resetAdjustments()
                    saturation = 0f
                    contrast = 0f
                    warmth = 0f
                    sharpness = 0f
                }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "重置参数",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                // 保存参数按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    showSaveDialog = true
                }) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "保存参数",
                        tint = HasselbladOrange
                    )
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

            // 实时预览对比视图
            item {
                PreviewComparisonCard(
                    currentParams = currentAdjustments,
                    splitPosition = comparisonSplit,
                    onSplitChange = { comparisonSplit = it }
                )
            }

            // 精细参数滑块
            item {
                FineTuneSlidersCard(
                    saturation = saturation,
                    contrast = contrast,
                    warmth = warmth,
                    sharpness = sharpness,
                    onSaturationChange = { value ->
                        saturation = value
                        aiManager.manuallyAdjustParam(AdjustmentType.SATURATION, value.toInt())
                    },
                    onContrastChange = { value ->
                        contrast = value
                        aiManager.manuallyAdjustParam(AdjustmentType.CONTRAST, value.toInt())
                    },
                    onWarmthChange = { value ->
                        warmth = value
                        aiManager.manuallyAdjustParam(AdjustmentType.WARMTH, value.toInt())
                    },
                    onSharpnessChange = { value ->
                        sharpness = value
                        aiManager.manuallyAdjustParam(AdjustmentType.SHARPNESS, value.toInt())
                    },
                    onReset = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        aiManager.resetAdjustments()
                        saturation = 0f
                        contrast = 0f
                        warmth = 0f
                        sharpness = 0f
                    }
                )
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
                        saturation = 0f
                        contrast = 0f
                        warmth = 0f
                        sharpness = 0f
                    }
                )
            }

            // 已保存参数列表
            if (savedParamsList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已保存参数",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        TextButton(onClick = {
                            clearAllSavedParams(context)
                            savedParamsList = emptyList()
                        }) {
                            Text("清空全部", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }

                items(savedParamsList) { savedParam ->
                    SavedParamItem(
                        savedParam = savedParam,
                        onApply = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            aiManager.adjustParam(AdjustmentType.SATURATION, savedParam.saturation)
                            aiManager.adjustParam(AdjustmentType.CONTRAST, savedParam.contrast)
                            aiManager.adjustParam(AdjustmentType.WARMTH, savedParam.warmth)
                            aiManager.adjustParam(AdjustmentType.SHARPNESS, savedParam.sharpness)
                            saturation = savedParam.saturation.toFloat()
                            contrast = savedParam.contrast.toFloat()
                            warmth = savedParam.warmth.toFloat()
                            sharpness = savedParam.sharpness.toFloat()
                        },
                        onDelete = {
                            haptic.perform(HapticFeedbackType.ToggleOff)
                            deleteSavedParam(context, savedParam.id)
                            savedParamsList = loadSavedParams(context)
                        }
                    )
                }
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

    // 保存参数弹窗
    if (showSaveDialog) {
        SaveParamDialog(
            currentName = saveParamName,
            onNameChange = { saveParamName = it },
            onConfirm = {
                if (saveParamName.isNotBlank()) {
                    haptic.perform(HapticFeedbackType.Confirm)
                    saveParamsToPrefs(
                        context = context,
                        name = saveParamName,
                        params = currentAdjustments
                    )
                    savedParamsList = loadSavedParams(context)
                    saveParamName = ""
                    showSaveDialog = false
                }
            },
            onDismiss = {
                haptic.perform(HapticFeedbackType.ToggleOff)
                saveParamName = ""
                showSaveDialog = false
            }
        )
    }
}

// ==================== 精细参数滑块组件 ====================

@Composable
private fun FineTuneSlidersCard(
    saturation: Float,
    contrast: Float,
    warmth: Float,
    sharpness: Float,
    onSaturationChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onWarmthChange: (Float) -> Unit,
    onSharpnessChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "精细调节",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                TextButton(onClick = onReset) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HasselbladOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置参数", color = HasselbladOrange, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ParamSlider(
                label = "饱和度",
                icon = Icons.Default.Palette,
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = -100f..100f
            )

            ParamSlider(
                label = "对比度",
                icon = Icons.Default.Contrast,
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = -100f..100f
            )

            ParamSlider(
                label = "色温",
                icon = Icons.Default.Thermostat,
                value = warmth,
                onValueChange = onWarmthChange,
                valueRange = -100f..100f
            )

            ParamSlider(
                label = "锐度",
                icon = Icons.Default.Grain,
                value = sharpness,
                onValueChange = onSharpnessChange,
                valueRange = -100f..100f
            )
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f
) {
    val isNonDefault = value != 0f
    val displayValue = value.toInt()

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = if (isNonDefault) HasselbladOrange else Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Text(
                text = if (displayValue > 0) "+$displayValue" else displayValue.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    displayValue > 0 -> HasselbladOrange
                    displayValue < 0 -> Color(0xFF64B5F6)
                    else -> Color.White.copy(alpha = 0.4f)
                }
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = if (isNonDefault) HasselbladOrange else Color.White.copy(alpha = 0.8f),
                activeTrackColor = if (isNonDefault) HasselbladOrange else Color.White.copy(alpha = 0.6f),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== 实时预览对比组件 ====================

@Composable
private fun PreviewComparisonCard(
    currentParams: AdjustmentParams,
    splitPosition: Float,
    onSplitChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预览对比",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row {
                    Text(
                        text = "原图",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "↔",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI微调",
                        style = MaterialTheme.typography.labelSmall,
                        color = HasselbladOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 对比预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D0D0D))
            ) {
                // 原图侧（左侧）
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(splitPosition)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E),
                                    Color(0xFF0F3460)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "原图",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // AI微调侧（右侧）
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(1f - splitPosition)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    applyTintToColor(Color(0xFF1A1A2E), currentParams),
                                    applyTintToColor(Color(0xFF16213E), currentParams),
                                    applyTintToColor(Color(0xFF0F3460), currentParams)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = HasselbladOrange.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI微调",
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange.copy(alpha = 0.7f)
                        )
                    }
                }

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.White.copy(alpha = 0.8f))
                        .align(Alignment.Center)
                        .offset(x = (splitPosition - 0.5f) * 300.dp) // approximate offset
                )

                // 拖动手柄
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        .align(Alignment.Center)
                        .offset(x = (splitPosition - 0.5f) * 300.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val newSplit = (splitPosition + dragAmount / size.width)
                                    .coerceIn(0.05f, 0.95f)
                                onSplitChange(newSplit)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp, 48.dp)
                            .background(
                                Color.White.copy(alpha = 0.9f),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = "拖动对比",
                            modifier = Modifier.size(16.dp),
                            tint = PureBlack
                        )
                    }
                }

                // 参数叠加显示
                if (currentParams.saturation != 0 || currentParams.contrast != 0 ||
                    currentParams.warmth != 0 || currentParams.sharpness != 0
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val paramText = buildString {
                            if (currentParams.saturation != 0) append("饱和度${formatSignedInt(currentParams.saturation)} ")
                            if (currentParams.contrast != 0) append("对比度${formatSignedInt(currentParams.contrast)} ")
                            if (currentParams.warmth != 0) append("色温${formatSignedInt(currentParams.warmth)} ")
                            if (currentParams.sharpness != 0) append("锐度${formatSignedInt(currentParams.sharpness)}")
                        }.trim()
                        Text(
                            text = paramText,
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "← 拖动分隔线对比原图与AI微调效果 →",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 根据参数对颜色进行微调，模拟预览效果
 */
private fun applyTintToColor(base: Color, params: AdjustmentParams): Color {
    val saturationShift = params.saturation / 400f
    val warmthShift = params.warmth / 300f
    val contrastShift = params.contrast / 500f

    val r = (base.red + warmthShift + contrastShift).coerceIn(0f, 1f)
    val g = (base.green + contrastShift).coerceIn(0f, 1f)
    val b = (base.blue - warmthShift + contrastShift).coerceIn(0f, 1f)

    val gray = 0.299f * r + 0.587f * g + 0.114f * b
    val satFactor = 1f + saturationShift
    val finalR = (gray + (r - gray) * satFactor).coerceIn(0f, 1f)
    val finalG = (gray + (g - gray) * satFactor).coerceIn(0f, 1f)
    val finalB = (gray + (b - gray) * satFactor).coerceIn(0f, 1f)

    return Color(finalR, finalG, finalB, base.alpha)
}

private fun formatSignedInt(value: Int): String = if (value > 0) "+$value" else value.toString()

// ==================== 保存参数弹窗 ====================

@Composable
private fun SaveParamDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "保存参数方案",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    label = { Text("方案名称") },
                    placeholder = { Text("例如：我的风景参数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = HasselbladOrange,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = HasselbladOrange
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = currentName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                }
            }
        }
    }
}

// ==================== 已保存参数项 ====================

data class SavedParamEntry(
    val id: String,
    val name: String,
    val saturation: Int,
    val contrast: Int,
    val warmth: Int,
    val sharpness: Int,
    val createdAt: Long
)

@Composable
private fun SavedParamItem(
    savedParam: SavedParamEntry,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savedParam.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    ParamChip("饱和度${formatSignedInt(savedParam.saturation)}", savedParam.saturation != 0)
                    Spacer(modifier = Modifier.width(6.dp))
                    ParamChip("对比度${formatSignedInt(savedParam.contrast)}", savedParam.contrast != 0)
                    Spacer(modifier = Modifier.width(6.dp))
                    ParamChip("色温${formatSignedInt(savedParam.warmth)}", savedParam.warmth != 0)
                    Spacer(modifier = Modifier.width(6.dp))
                    ParamChip("锐度${formatSignedInt(savedParam.sharpness)}", savedParam.sharpness != 0)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 应用按钮
            IconButton(
                onClick = onApply,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "应用",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ParamChip(text: String, active: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) HasselbladOrange else Color.White.copy(alpha = 0.3f)
    )
}

// ==================== SharedPreferences 存取 ====================

private const val PREFS_SAVED_PARAMS = "ai_fine_tune_saved_params"
private const val KEY_SAVED_PARAMS_JSON = "saved_params_json"

private fun saveParamsToPrefs(context: Context, name: String, params: AdjustmentParams) {
    val prefs = context.getSharedPreferences(PREFS_SAVED_PARAMS, Context.MODE_PRIVATE)
    val existingJson = prefs.getString(KEY_SAVED_PARAMS_JSON, "[]") ?: "[]"
    val jsonArray = JSONArray(existingJson)

    val newEntry = JSONObject().apply {
        put("id", System.currentTimeMillis().toString())
        put("name", name)
        put("saturation", params.saturation)
        put("contrast", params.contrast)
        put("warmth", params.warmth)
        put("sharpness", params.sharpness)
        put("createdAt", System.currentTimeMillis())
    }

    jsonArray.put(newEntry)
    prefs.edit().putString(KEY_SAVED_PARAMS_JSON, jsonArray.toString()).apply()
}

private fun loadSavedParams(context: Context): List<SavedParamEntry> {
    val prefs = context.getSharedPreferences(PREFS_SAVED_PARAMS, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_SAVED_PARAMS_JSON, "[]") ?: "[]"
    val jsonArray = JSONArray(jsonStr)
    val result = mutableListOf<SavedParamEntry>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        result.add(
            SavedParamEntry(
                id = obj.getString("id"),
                name = obj.getString("name"),
                saturation = obj.getInt("saturation"),
                contrast = obj.getInt("contrast"),
                warmth = obj.getInt("warmth"),
                sharpness = obj.getInt("sharpness"),
                createdAt = obj.getLong("createdAt")
            )
        )
    }

    return result.sortedByDescending { it.createdAt }
}

private fun deleteSavedParam(context: Context, id: String) {
    val prefs = context.getSharedPreferences(PREFS_SAVED_PARAMS, Context.MODE_PRIVATE)
    val existingJson = prefs.getString(KEY_SAVED_PARAMS_JSON, "[]") ?: "[]"
    val jsonArray = JSONArray(existingJson)
    val newArray = JSONArray()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        if (obj.getString("id") != id) {
            newArray.put(obj)
        }
    }

    prefs.edit().putString(KEY_SAVED_PARAMS_JSON, newArray.toString()).apply()
}

private fun clearAllSavedParams(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_SAVED_PARAMS, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_SAVED_PARAMS_JSON).apply()
}

// ==================== 原有组件 ====================

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
    params: AdjustmentParams,
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
