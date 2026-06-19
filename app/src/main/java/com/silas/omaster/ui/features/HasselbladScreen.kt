package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.model.FilmPreset
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.ui.components.MasterWorkflow
import com.silas.omaster.ui.components.WorkflowResult
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 哈苏之眼 - 相机拍摄 + AI大师分析
 *
 * 功能：
 * - 系统相机拍摄（支持前后翻转）
 * - AI 场景识别分析
 * - 哈苏大师工作流（场景→胶片→参数→建议）
 * - 分析结果展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }

    // 相机拍照
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisSteps by remember { mutableStateOf<List<HasselbladAnalysisStep>>(emptyList()) }
    var currentStepIndex by remember { mutableIntStateOf(-1) }
    var sceneProfile by remember { mutableStateOf<SceneProfile?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var showWorkflow by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(photoUri!!)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    runAnalysis(bitmap, inferenceEngine, scope) { steps, idx, profile, error ->
                        analysisSteps = steps
                        currentStepIndex = idx
                        sceneProfile = profile
                        analysisError = error
                    }
                } else {
                    analysisError = "图片加载失败"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("哈苏之眼", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (capturedBitmap == null) {
            // ===== 拍摄模式 =====
            CameraCaptureUI(
                onCapture = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val uri = createTempImageUri(context)
                    if (uri != Uri.EMPTY) {
                        photoUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        Toast.makeText(context, "无法创建相机临时文件", Toast.LENGTH_SHORT).show()
                    }
                },
                onBack = onBack
            )
        } else {
            // ===== 分析结果模式 =====
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 拍摄的照片预览
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            capturedBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "拍摄照片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // 重新拍摄按钮
                            IconButton(
                                onClick = {
                                    capturedBitmap = null
                                    sceneProfile = null
                                    analysisError = null
                                    analysisSteps = emptyList()
                                    currentStepIndex = -1
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    "重新拍摄",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 分析进行中
                if (isAnalyzing) {
                    item {
                        AnalyzingCard(analysisSteps, currentStepIndex)
                    }
                }

                // 分析错误
                analysisError?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF5252).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFFF5252))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("分析失败", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                    Text(error, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 分析结果
                sceneProfile?.let { profile ->
                    item {
                        SceneAnalysisCard(profile)
                    }

                    // 推荐胶片
                    item {
                        Text("推荐胶片", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                    }

                    items(profile.recommendedFilm) { film ->
                        FilmRecommendationCard(film)
                    }

                    // 哈苏参数
                    item {
                        Text("哈苏参数建议", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        HasselbladParamsCard(profile.hasselbladParams)
                    }

                    // 大师拍摄建议
                    if (profile.masterTips.isNotEmpty()) {
                        item {
                            Text("大师拍摄建议", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                        items(profile.masterTips) { tip ->
                            MasterTipCard(tip)
                        }
                    }
                }

                // 底部按钮
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedBitmap = null
                                sceneProfile = null
                                analysisError = null
                                analysisSteps = emptyList()
                                currentStepIndex = -1
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重新拍摄")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showWorkflow = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                            enabled = sceneProfile != null
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("完整工作流")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // 完整工作流弹窗
    if (showWorkflow && capturedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showWorkflow = false },
            title = { Text("哈苏大师工作流") },
            text = {
                MasterWorkflow(
                    bitmap = capturedBitmap,
                    onComplete = { result ->
                        // 工作流完成，关闭弹窗
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showWorkflow = false }) {
                    Text("关闭")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * 拍摄界面
 */
@Composable
private fun CameraCaptureUI(
    onCapture: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            HasselbladOrange.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 哈苏之眼图标
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                HasselbladOrange.copy(alpha = 0.3f),
                                HasselbladOrange.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(HasselbladOrange.copy(alpha = 0.15f))
                        .border(2.dp, HasselbladOrange.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "哈苏之眼",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "拍摄照片，AI 大师为您分析场景\n推荐最佳胶片与哈苏参数",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 拍摄按钮
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange)
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "点击拍摄",
                fontSize = 14.sp,
                color = HasselbladOrange,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "支持前后摄像头翻转 · AI 场景识别 · 大师胶片推荐",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * 分析进行中卡片
 */
@Composable
private fun AnalyzingCard(
    steps: List<HasselbladAnalysisStep>,
    currentStepIndex: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "AI 正在分析...",
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when {
                            index < currentStepIndex -> Icons.Default.CheckCircle
                            index == currentStepIndex -> Icons.Default.Sync
                            else -> Icons.Default.RadioButtonUnchecked
                        },
                        null,
                        tint = when {
                            index < currentStepIndex -> SuccessGreen
                            index == currentStepIndex -> HasselbladOrange
                            else -> Color.Gray
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        step.name,
                        fontSize = 13.sp,
                        color = when {
                            index < currentStepIndex -> SuccessGreen
                            index == currentStepIndex -> HasselbladOrange
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}

/**
 * 场景分析结果卡片
 */
@Composable
private fun SceneAnalysisCard(profile: SceneProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(HasselbladOrange, Color(0xFFFF8A50))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "场景识别",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    profile.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "置信度: ${(profile.confidence * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                if (profile.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        profile.description,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * 胶片推荐卡片
 */
@Composable
private fun FilmRecommendationCard(film: FilmPreset) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HasselbladOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Movie, null, tint = HasselbladOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    film.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
                if (film.description.isNotEmpty()) {
                    Text(
                        film.description,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${(film.matchScore * 100).toInt()}%",
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 哈苏参数卡片
 */
@Composable
private fun HasselbladParamsCard(params: HasselbladParams) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            listOf(
                "影调" to params.tone.toString(),
                "饱和度" to params.saturation.toString(),
                "色温" to params.colorTemp.toString(),
                "对比度" to params.contrast.toString(),
                "锐度" to params.sharpness.toString()
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                    Text(value, color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 大师建议卡片
 */
@Composable
private fun MasterTipCard(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                tip,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 哈苏分析步骤
 */
private data class HasselbladAnalysisStep(
    val name: String,
    val icon: ImageVector
)

/**
 * 执行 AI 分析工作流
 */
private fun runAnalysis(
    bitmap: Bitmap,
    inferenceEngine: MasterInferenceEngine,
    scope: kotlinx.coroutines.CoroutineScope,
    onUpdate: (List<HasselbladAnalysisStep>, Int, SceneProfile?, String?) -> Unit
) {
    val steps = listOf(
        HasselbladAnalysisStep("颜色直方图分析", Icons.Default.Palette),
        HasselbladAnalysisStep("EXIF 元数据提取", Icons.Default.Info),
        HasselbladAnalysisStep("人脸检测", Icons.Default.Face),
        HasselbladAnalysisStep("场景识别", Icons.Default.Camera),
        HasselbladAnalysisStep("胶片推荐", Icons.Default.Movie),
        HasselbladAnalysisStep("哈苏参数优化", Icons.Default.Tune),
        HasselbladAnalysisStep("大师拍摄建议", Icons.Default.Lightbulb)
    )

    scope.launch {
        onUpdate(steps, 0, null, null)
        delay(300)

        try {
            onUpdate(steps, 1, null, null)
            delay(200)

            onUpdate(steps, 2, null, null)
            delay(200)

            onUpdate(steps, 3, null, null)

            val profile = withContext(Dispatchers.Default) {
                inferenceEngine.analyzeImage(bitmap)
            }

            onUpdate(steps, 4, profile, null)
            delay(200)

            onUpdate(steps, 5, profile, null)
            delay(200)

            onUpdate(steps, 6, profile, null)
            delay(200)

            onUpdate(steps, steps.size, profile, null)
        } catch (e: Exception) {
            onUpdate(steps, -1, null, e.message ?: "分析失败")
        }
    }
}

/**
 * 创建临时图片Uri用于相机拍照保存
 */
private fun createTempImageUri(context: android.content.Context): Uri {
    return try {
        val cameraDir = File(context.cacheDir, "camera").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File.createTempFile(
            "omaster_eye_${System.currentTimeMillis()}",
            ".jpg",
            cameraDir
        )
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        android.util.Log.e("HasselbladScreen", "创建相机临时文件失败", e)
        Uri.EMPTY
    }
}