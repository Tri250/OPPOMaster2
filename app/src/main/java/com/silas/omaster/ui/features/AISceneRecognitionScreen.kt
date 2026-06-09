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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.silas.omaster.ai.SceneRecognitionManager
import com.silas.omaster.ai.SceneType
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import kotlinx.coroutines.launch

/**
 * 哈苏之眼功能页面
 * 支持50+精细场景类型识别
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISceneRecognitionScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sceneManager = remember { SceneRecognitionManager.getInstance() }

    val sceneTypes by sceneManager.sceneTypes.collectAsState()
    val recognizedScenes by sceneManager.recognizedScenes.collectAsState()
    val isRecognizing by sceneManager.isRecognizing.collectAsState()
    var selectedSceneId by remember { mutableStateOf<String?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val groupedScenes = sceneTypes.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("哈苏之眼", fontWeight = FontWeight.Bold) },
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
            // 一键识别按钮
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
                            text = "智能场景识别",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                        Text(
                            text = "识别50+拍摄场景，自动推荐最佳参数",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                scope.launch {
                                    sceneManager.performRecognition()
                                }
                            },
                            enabled = !isRecognizing,
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            if (isRecognizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始识别")
                            }
                        }
                    }
                }
            }

            // 已识别场景
            if (recognizedScenes.isNotEmpty()) {
                item {
                    Text(
                        text = "识别结果",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                items(recognizedScenes) { scene ->
                    RecognizedSceneCard(
                        scene = scene,
                        onClick = {
                            selectedSceneId = scene.id
                            showDetailDialog = true
                        }
                    )
                }
            }

            // 场景分类列表
            groupedScenes.forEach { (category, scenes) ->
                item {
                    Text(
                        text = getCategoryName(category),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                items(scenes) { scene ->
                    SceneTypeCard(
                        scene = scene,
                        isRecognized = recognizedScenes.any { it.id == scene.id },
                        onClick = {
                            haptic.perform(HapticFeedbackType.Select)
                            selectedSceneId = scene.id
                            showDetailDialog = true
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

    // 场景详情弹窗
    if (showDetailDialog && selectedSceneId != null) {
        val scene = sceneTypes.find { it.id == selectedSceneId }
        if (scene != null) {
            SceneDetailDialog(
                scene = scene,
                onApply = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    scope.launch {
                        sceneManager.applySceneParams(scene.id)
                    }
                    showDetailDialog = false
                },
                onDismiss = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    showDetailDialog = false
                }
            )
        }
    }
}

private fun getCategoryName(category: String): String {
    return when (category) {
        "portrait" -> "人像系列"
        "landscape" -> "风景系列"
        "food" -> "美食系列"
        "night" -> "夜景系列"
        "macro" -> "微距系列"
        "action" -> "运动系列"
        else -> category
    }
}

@Composable
private fun RecognizedSceneCard(
    scene: SceneType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HasselbladOrange.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = HasselbladOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = HasselbladOrange
                )
                Text(
                    text = "${scene.confidence}% 匹配度",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun SceneTypeCard(
    scene: SceneType,
    isRecognized: Boolean,
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
            if (isRecognized) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = scene.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun SceneDetailDialog(
    scene: SceneType,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = scene.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "推荐参数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                val params = listOf(
                    "饱和度" to scene.params.saturation,
                    "对比度" to scene.params.contrast,
                    "色温" to scene.params.warmth,
                    "锐度" to scene.params.sharpness
                )

                params.forEach { (name, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = value.toString(),
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("应用参数")
                    }
                }
            }
        }
    }
}
