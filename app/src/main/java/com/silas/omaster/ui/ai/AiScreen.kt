package com.silas.omaster.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silas.omaster.R
import com.silas.omaster.model.AiAdjustmentParams
import com.silas.omaster.model.SceneType
import com.silas.omaster.service.AiService
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.hapticClickable
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

/**
 * AI功能屏幕 - 提供AI场景识别和AI智能优化功能
 * 优化：添加动画效果、改进视觉层次、增强触摸反馈
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val aiService = remember { AiService.getInstance(context) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedScene by remember { mutableStateOf<SceneType?>(null) }
    var adjustments by remember { mutableStateOf<AiAdjustmentParams?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        detectedScene = null
        adjustments = null
        errorMessage = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OMasterTopAppBar(
            title = stringResource(R.string.ai_feature_title),
            subtitle = "场景识别 · 智能优化",
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图片选择卡片 - 带渐变背景
            ImageSelectionCard(
                selectedImageUri = selectedImageUri,
                onSelectImage = { imagePickerLauncher.launch("image/*") }
            )

            // AI场景识别卡片 - 带动画效果
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SceneDetectionCard(
                    detectedScene = detectedScene,
                    isAnalyzing = isAnalyzing,
                    onAnalyze = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        isAnalyzing = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val scene = aiService.detectScene(selectedImageUri.toString())
                                detectedScene = scene
                            } catch (e: Exception) {
                                errorMessage = "识别失败: ${e.message}"
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    }
                )
            }

            // AI智能优化卡片
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OptimizationCard(
                    adjustments = adjustments,
                    isOptimizing = isOptimizing,
                    onOptimize = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        isOptimizing = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val result = aiService.fineTuneImage(
                                    imageUri = selectedImageUri.toString(),
                                    preset = null
                                )
                                adjustments = result
                            } catch (e: Exception) {
                                errorMessage = "优化失败: ${e.message}"
                            } finally {
                                isOptimizing = false
                            }
                        }
                    }
                )
            }

            // 错误提示
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 重置按钮
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        selectedImageUri = null
                        detectedScene = null
                        adjustments = null
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重新选择")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ImageSelectionCard(
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题带图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "选择图片",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 图片预览区域 - 带渐变边框效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (selectedImageUri != null) {
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "已选图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                    startY = 150f
                                )
                            )
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(HasselbladOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Camera,
                                contentDescription = null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "点击选择图片开始分析",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 选择按钮 - 带触摸反馈
            Button(
                onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                    onSelectImage()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImageUri != null) "更换图片" else "从相册选择",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SceneDetectionCard(
    detectedScene: SceneType?,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题区域
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI 场景识别",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.ai_scene_detection_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 结果或按钮
            if (detectedScene != null) {
                SceneResultContent(scene = detectedScene)
            } else {
                Button(
                    onClick = onAnalyze,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("分析中...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始场景识别")
                    }
                }
            }
        }
    }
}

@Composable
private fun OptimizationCard(
    adjustments: AiAdjustmentParams?,
    isOptimizing: Boolean,
    onOptimize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 标题区域
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI 智能优化",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.ai_optimization_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 结果或按钮
            if (adjustments != null) {
                AdjustmentResultContent(adjustments = adjustments)
            } else {
                Button(
                    onClick = onOptimize,
                    enabled = !isOptimizing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isOptimizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("优化中...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始智能优化")
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneResultContent(scene: SceneType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrange.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "识别成功",
                        style = MaterialTheme.typography.labelMedium,
                        color = HasselbladOrange
                    )
                    Text(
                        text = getSceneDisplayName(scene),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getSceneDescription(scene),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AdjustmentResultContent(adjustments: AiAdjustmentParams) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrange.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "优化完成",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 参数网格
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdjustmentRow("亮度", adjustments.brightness)
                AdjustmentRow("对比度", adjustments.contrast)
                AdjustmentRow("饱和度", adjustments.saturation)
                AdjustmentRow("暖度", adjustments.warmth)
                AdjustmentRow("高光", adjustments.highlights)
                AdjustmentRow("阴影", adjustments.shadows)
                AdjustmentRow("清晰度", adjustments.clarity)
                AdjustmentRow("暗角", adjustments.vignette)
            }
        }
    }
}

@Composable
private fun AdjustmentRow(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (value > 0) HasselbladOrange.copy(alpha = 0.2f)
                    else if (value < 0) Color.Red.copy(alpha = 0.2f)
                    else Color.Gray.copy(alpha = 0.2f)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = String.format("%+.1f", value),
                style = MaterialTheme.typography.labelMedium,
                color = if (value > 0) HasselbladOrange
                else if (value < 0) Color(0xFFFF6B6B)
                else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getSceneDisplayName(scene: SceneType): String = when (scene) {
    SceneType.PORTRAIT -> "人像"
    SceneType.LANDSCAPE -> "风景"
    SceneType.NIGHT -> "夜景"
    SceneType.STARRY_NIGHT -> "星空"
    SceneType.NIGHT_PORTRAIT -> "夜景人像"
    SceneType.FOOD -> "美食"
    SceneType.SUNSET -> "日落"
    SceneType.FLOWER -> "花卉"
    SceneType.MACRO -> "微距"
    SceneType.INSECT -> "昆虫"
    SceneType.MOTION -> "运动"
    SceneType.CITYSCAPE -> "城市建筑"
    SceneType.INDOOR_WARM -> "室内暖光"
    SceneType.STILL_LIFE -> "静物"
    SceneType.RAINY_FOGGY -> "雨雾"
    SceneType.MIXED_FOOD -> "混合美食"
    SceneType.MIXED_LANDSCAPE -> "混合风景"
    SceneType.FLOWERS_SUNSET -> "花卉日落"
    SceneType.OBJECT_DETAIL -> "物品细节"
    SceneType.TOO_DARK -> "过暗"
    SceneType.TOO_BRIGHT -> "过亮"
    SceneType.TOO_BLURRY -> "过模糊"
    SceneType.UNKNOWN -> "未知场景"
}

private fun getSceneDescription(scene: SceneType): String = when (scene) {
    SceneType.PORTRAIT -> "建议使用人像模式，ISO 100，f/1.8 大光圈，突出主体"
    SceneType.LANDSCAPE -> "建议使用风景模式，ISO 64，f/8.0 小光圈，增强景深"
    SceneType.NIGHT -> "建议使用夜景模式，ISO 3200，开启光学防抖"
    SceneType.FOOD -> "建议使用美食模式，提高饱和度和暖度，突出食欲感"
    SceneType.SUNSET -> "建议使用日落模式，暖色调突出黄金时刻氛围"
    SceneType.MACRO -> "建议使用微距模式，高锐度细节增强"
    SceneType.CITYSCAPE -> "建议使用 HDR 模式，增强建筑细节和动态范围"
    else -> "已识别场景，可应用推荐参数获得最佳效果"
}
