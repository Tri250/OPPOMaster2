package com.silas.omaster.ui.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.silas.omaster.R
import com.silas.omaster.model.AiAdjustmentParams
import com.silas.omaster.model.SceneType
import com.silas.omaster.service.AiService
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.launch

/**
 * AI功能屏幕 - 提供AI场景识别和AI智能优化功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
            title = "AI 智能助手",
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
            // 图片选择卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "选择图片",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "已选图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击下方按钮选择图片",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从相册选择图片")
                    }
                }
            }

            // AI场景识别卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 场景识别",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "智能识别图片场景类型，自动推荐最佳相机参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (detectedScene != null) {
                        SceneResultCard(scene = detectedScene!!)
                    } else {
                        Button(
                            onClick = {
                                if (selectedImageUri == null) {
                                    errorMessage = "请先选择图片"
                                    return@Button
                                }
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
                            },
                            enabled = selectedImageUri != null && !isAnalyzing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("分析中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始 AI 场景识别")
                            }
                        }
                    }
                }
            }

            // AI智能优化卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 智能优化",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "基于图像特征自动计算最优调整参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (adjustments != null) {
                        AdjustmentResultCard(adjustments = adjustments!!)
                    } else {
                        Button(
                            onClick = {
                                if (selectedImageUri == null) {
                                    errorMessage = "请先选择图片"
                                    return@Button
                                }
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
                            },
                            enabled = selectedImageUri != null && !isOptimizing,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("优化中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始 AI 智能优化")
                            }
                        }
                    }
                }
            }

            // 错误提示
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

            // 重置按钮
            if (selectedImageUri != null) {
                OutlinedButton(
                    onClick = {
                        selectedImageUri = null
                        detectedScene = null
                        adjustments = null
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SceneResultCard(scene: SceneType) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "识别结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = HasselbladOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getSceneDisplayName(scene),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getSceneDescription(scene),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AdjustmentResultCard(adjustments: AiAdjustmentParams) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "优化参数",
                style = MaterialTheme.typography.labelSmall,
                color = HasselbladOrange
            )
            Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun AdjustmentRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = String.format("%+.1f", value),
            style = MaterialTheme.typography.bodySmall,
            color = HasselbladOrange,
            fontWeight = FontWeight.Bold
        )
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
    SceneType.PORTRAIT -> "建议使用人像模式，ISO 100，f/1.8 大光圈"
    SceneType.LANDSCAPE -> "建议使用风景模式，ISO 64，f/8.0 小光圈"
    SceneType.NIGHT -> "建议使用夜景模式，ISO 3200，光学防抖"
    SceneType.FOOD -> "建议使用美食模式，提高饱和度和暖度"
    SceneType.SUNSET -> "建议使用日落模式，暖色调突出"
    SceneType.MACRO -> "建议使用微距模式，高锐度细节增强"
    SceneType.CITYSCAPE -> "建议使用 HDR 模式，增强建筑细节"
    else -> "已识别场景，可应用推荐参数"
}
