package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.model.FilmPreset
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.ui.components.AnalysisStatus
import com.silas.omaster.ui.components.AnalysisStep
import com.silas.omaster.ui.components.ApertureState
import com.silas.omaster.ui.components.defaultAnalysisSteps
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.HasselbladOrangeLight
import com.silas.omaster.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 哈苏之眼 (Hasselblad Eye) - HNCS 3.0 自然色彩解决方案
 *
 * 产品流程：
 * 1. READY    → 品牌展示 + 拍照/选图入口
 * 2. ANALYZING → 光圈动画 + AI 场景分析
 * 3. RESULTS  → 场景画像 + 推荐色彩模式 + 哈苏参数 + 胶片 + 大师建议
 * 4. PREVIEW  → 应用哈苏色彩科学前后对比
 * 5. DONE     → 保存完成
 */

/**
 * 哈苏之眼工作流阶段
 */
enum class HasselbladEyeStage {
    /** 初始状态：显示拍照/选图按钮 */
    READY,
    /** 分析中：光圈动画 + AI推理 */
    ANALYZING,
    /** 展示分析结果和推荐 */
    RESULTS,
    /** 预览应用色彩后的效果 */
    PREVIEW,
    /** 已完成保存 */
    DONE
}

/**
 * AI 分析结果
 */
data class AnalysisResult(
    val sceneProfile: SceneProfile,
    val recommendedFilms: List<FilmPreset>,
    val masterTips: List<String>,
    val suggestedColorMode: String,
    val suggestedColorModeId: String,
    val paramAdjustments: Map<String, Int>
)

/**
 * 色彩模式定义（用于结果页自动推荐 + 可切换）
 */
@androidx.compose.runtime.Immutable
data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val icon: ImageVector,
    val params: Map<String, Int>
)

/** 可选色彩模式列表 */
val colorModes = listOf(
    ColorMode(
        "natural", "哈苏自然色彩", "HNCS 3.0 自然色彩解决方案",
        HasselbladOrange, Icons.Default.Visibility,
        mapOf("saturation" to 0, "contrast" to 5, "warmth" to 0, "vibrance" to 5, "clarity" to 0)
    ),
    ColorMode(
        "portrait", "人像肤色优化", "自然美化肤色，保留细节",
        Color(0xFFFF6B9D), Icons.Default.Face,
        mapOf("saturation" to 5, "contrast" to 8, "warmth" to 3, "vibrance" to 0, "skinTone" to 10, "clarity" to 0)
    ),
    ColorMode(
        "landscape", "风景色彩增强", "增强风景色彩层次",
        Color(0xFF4ECDC4), Icons.Default.Landscape,
        mapOf("saturation" to 12, "contrast" to 10, "warmth" to 5, "vibrance" to 0, "clarity" to 10)
    ),
    ColorMode(
        "classic", "哈苏经典胶片", "复古胶片色彩质感",
        Color(0xFF9C27B0), Icons.Default.AutoAwesome,
        mapOf("saturation" to 8, "contrast" to 15, "warmth" to 8, "vibrance" to 0, "grain" to 5, "clarity" to 0)
    ),
    ColorMode(
        "bw", "哈苏黑白", "经典黑白摄影风格",
        Color(0xFF808080), Icons.Default.DarkMode,
        mapOf("saturation" to -100, "contrast" to 20, "warmth" to 0, "vibrance" to 0, "clarity" to 15)
    ),
    ColorMode(
        "vivid", "鲜艳色彩", "鲜艳饱满的色彩表现",
        Color(0xFFFF9800), Icons.Default.Palette,
        mapOf("saturation" to 20, "contrast" to 10, "warmth" to 0, "vibrance" to 15, "clarity" to 0)
    )
)

private val colorModesMap = colorModes.associateBy { it.id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }

    // 核心状态
    var stage by remember { mutableStateOf(HasselbladEyeStage.READY) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    // 分析相关
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }

    // 光圈动画状态
    var apertureState by remember { mutableStateOf(ApertureState.CLOSED) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var analysisSteps by remember { mutableStateOf(defaultAnalysisSteps()) }
    var analysisMessage by remember { mutableStateOf("正在读取光影信息...") }

    // 预览相关
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // 结果页选中的色彩模式（AI 推荐后可切换）
    var selectedColorModeId by remember { mutableStateOf("natural") }

    // 相机
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 相机权限
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能使用哈苏之眼", Toast.LENGTH_LONG).show()
        }
    }

    // 相机拍照启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            scope.launch {
                val bitmap = loadBitmapFromUriAsync(context, cameraImageUri!!)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    capturedUri = cameraImageUri
                    stage = HasselbladEyeStage.ANALYZING
                    startAnalysis(
                        bitmap = bitmap,
                        inferenceEngine = inferenceEngine,
                        onProgressUpdate = { progress, message, steps ->
                            analysisProgress = progress
                            analysisMessage = message
                            analysisSteps = steps
                            apertureState = when {
                                progress < 15f -> ApertureState.ROTATING
                                progress < 100f -> ApertureState.OPENING
                                else -> ApertureState.OPEN
                            }
                        },
                        onComplete = { result ->
                            analysisResult = result
                            selectedColorModeId = result.suggestedColorModeId
                            stage = HasselbladEyeStage.RESULTS
                        },
                        onError = { error ->
                            analysisError = error
                            stage = HasselbladEyeStage.RESULTS
                        },
                        scope = scope
                    )
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 相册选择启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUriAsync(context, it)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    capturedUri = it
                    stage = HasselbladEyeStage.ANALYZING
                    startAnalysis(
                        bitmap = bitmap,
                        inferenceEngine = inferenceEngine,
                        onProgressUpdate = { progress, message, steps ->
                            analysisProgress = progress
                            analysisMessage = message
                            analysisSteps = steps
                            apertureState = when {
                                progress < 15f -> ApertureState.ROTATING
                                progress < 100f -> ApertureState.OPENING
                                else -> ApertureState.OPEN
                            }
                        },
                        onComplete = { result ->
                            analysisResult = result
                            selectedColorModeId = result.suggestedColorModeId
                            stage = HasselbladEyeStage.RESULTS
                        },
                        onError = { error ->
                            analysisError = error
                            stage = HasselbladEyeStage.RESULTS
                        },
                        scope = scope
                    )
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 启动相机
    fun launchCamera() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val photoUri = createTempImageUri(context)
            if (photoUri != Uri.EMPTY) {
                cameraImageUri = photoUri
                cameraLauncher.launch(photoUri)
            } else {
                Toast.makeText(context, "无法创建相机临时文件", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // 重置到初始状态
    fun resetToReady() {
        stage = HasselbladEyeStage.READY
        capturedBitmap = null
        capturedUri = null
        analysisResult = null
        analysisError = null
        previewBitmap = null
        apertureState = ApertureState.CLOSED
        analysisProgress = 0f
        analysisSteps = defaultAnalysisSteps()
        analysisMessage = "正在读取光影信息..."
        selectedColorModeId = "natural"
    }

    // 主布局
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TopAppBar(
            title = {
                Text(
                    when (stage) {
                        HasselbladEyeStage.READY -> "哈苏之眼"
                        HasselbladEyeStage.ANALYZING -> "哈苏之眼 · 分析中"
                        HasselbladEyeStage.RESULTS -> "哈苏之眼 · 分析结果"
                        HasselbladEyeStage.PREVIEW -> "哈苏之眼 · 预览"
                        HasselbladEyeStage.DONE -> "哈苏之眼 · 已完成"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    when (stage) {
                        HasselbladEyeStage.RESULTS -> {
                            stage = HasselbladEyeStage.READY
                            capturedBitmap = null
                            analysisResult = null
                            analysisError = null
                        }
                        HasselbladEyeStage.PREVIEW -> {
                            stage = HasselbladEyeStage.RESULTS
                            previewBitmap = null
                        }
                        HasselbladEyeStage.DONE -> resetToReady()
                        else -> onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "hasselblad_stage"
        ) { currentStage ->
            when (currentStage) {
                HasselbladEyeStage.READY -> ReadyContent(
                    onLaunchCamera = { launchCamera() },
                    onPickFromGallery = { galleryLauncher.launch("image/*") }
                )

                HasselbladEyeStage.ANALYZING -> AnalyzingContent(
                    bitmap = capturedBitmap,
                    apertureState = apertureState,
                    progress = analysisProgress,
                    message = analysisMessage,
                    steps = analysisSteps
                )

                HasselbladEyeStage.RESULTS -> ResultsContent(
                    result = analysisResult,
                    error = analysisError,
                    capturedBitmap = capturedBitmap,
                    selectedColorModeId = selectedColorModeId,
                    onSelectColorMode = { selectedColorModeId = it },
                    onApplyAndPreview = {
                        val bmp = capturedBitmap
                        val result = analysisResult
                        if (bmp != null && result != null) {
                            scope.launch {
                                previewBitmap = withContext(Dispatchers.Default) {
                                    val modeParams = colorModesMap[selectedColorModeId]?.params ?: emptyMap()
                                    applyHasselbladColorScience(bmp, result.sceneProfile.hasselbladParams, modeParams)
                                }
                                stage = HasselbladEyeStage.PREVIEW
                            }
                        }
                    },
                    onRetake = { resetToReady() }
                )

                HasselbladEyeStage.PREVIEW -> PreviewContent(
                    originalBitmap = capturedBitmap,
                    previewBitmap = previewBitmap,
                    isSaving = isSaving,
                    onConfirm = {
                        previewBitmap?.let { bmp ->
                            isSaving = true
                            scope.launch {
                                val success = saveImageToGallery(context, bmp)
                                isSaving = false
                                if (success) {
                                    stage = HasselbladEyeStage.DONE
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onRetake = { resetToReady() }
                )

                HasselbladEyeStage.DONE -> DoneContent(
                    onNewPhoto = { resetToReady() },
                    onBack = onBack
                )
            }
        }
    }
}

// ==================== Stage Composables ====================

/**
 * READY 阶段：品牌展示 + 拍照/选图入口
 *
 * 产品经理UX优化要点：
 * 1. 首屏聚焦：大圆形快门按钮作为绝对视觉中心，符合相机应用用户心智模型
 * 2. 品牌信任：HNCS 3.0品牌卡片强化专业影像认知
 * 3. 操作分流：拍照/相册双入口，满足不同场景启动路径
 * 4. 特性教育：3个核心卖点卡片，降低用户认知门槛
 * 5. 液态玻璃：所有卡片采用半透明+模糊材质，符合ColorOS 16规范
 * 6. 工作流引导：新增 4 步流程指示器，明确告知用户后续步骤
 */
@Composable
private fun ReadyContent(
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HNCS 3.0 品牌卡片 - 液态玻璃材质
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    HasselbladOrange.copy(alpha = 0.95f),
                                    HasselbladOrangeLight.copy(alpha = 0.85f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Camera, null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "哈苏之眼",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "HNCS 3.0 自然色彩解决方案",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                letterSpacing = 0.25.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "AI场景识别 · 色彩科学 · 大师参数",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            }
        }

        // 工作流指示器 - 新增 4 步流程引导
        item {
            WorkflowIndicator(
                steps = listOf("拍照", "AI分析", "选择风格", "保存")
            )
        }

        // 大圆形拍照按钮 - 视觉焦点
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 外圈呼吸光环动画
                    val infiniteTransition = rememberInfiniteTransition(label = "shutter_breath")
                    val breathScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "breath"
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(breathScale)
                            .clip(CircleShape)
                            .background(HasselbladOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // 中圈
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.2f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onLaunchCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            // 内圈快门按钮
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            listOf(HasselbladOrangeLight, HasselbladOrange)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "点击拍照，AI即刻分析场景",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        letterSpacing = 0.25.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 从相册选择 - 次级操作
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HasselbladOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "从相册选择",
                            color = HasselbladOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.25.sp
                        )
                    }
                }
            }
        }

        // 特性亮点 - 教育用户核心价值
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureHighlightItem(
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = HasselbladOrange,
                    title = "AI场景识别",
                    description = "智能识别拍摄场景，自动推荐最佳色彩方案"
                )
                FeatureHighlightItem(
                    icon = Icons.Default.Palette,
                    iconBgColor = Color(0xFF2196F3),
                    title = "哈苏色彩科学",
                    description = "HNCS 3.0 自然色彩还原，大师级调色参数"
                )
                FeatureHighlightItem(
                    icon = Icons.Default.Movie,
                    iconBgColor = Color(0xFF9C27B0),
                    title = "大师参数推荐",
                    description = "场景匹配胶片风格与拍摄建议"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 4 步工作流指示器
 * 让用户对整个流程有清晰预期，减少认知负担
 */
@Composable
private fun WorkflowIndicator(steps: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = HasselbladOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = step,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(1.5.dp)
                            .background(HasselbladOrange.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

/**
 * ANALYZING 阶段：光圈动画 + 分析进度
 *
 * 产品经理UX优化要点：
 * 1. 沉浸式体验：全屏居中布局，减少干扰元素
 * 2. 实时反馈：进度条+步骤列表双通道信息传递
 * 3. 品牌感知：底部HNCS标识强化专业认知
 * 4. 动画细节：光圈叶片旋转+中心点呼吸，模拟真实相机快门
 */
@Composable
private fun AnalyzingContent(
    bitmap: Bitmap?,
    apertureState: ApertureState,
    progress: Float,
    message: String,
    steps: List<AnalysisStep>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 光圈动画区域
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // 外圈轨道
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(Color.Transparent)
                    .then(
                        Modifier.drawBehind {
                            drawCircle(
                                color = HasselbladOrange.copy(alpha = 0.25f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2.5f.dp.toPx())
                            )
                        }
                    )
            )

            // 光圈叶片
            ApertureBladesAnimated(state = apertureState)

            // 中心点呼吸动画
            val infiniteTransition = rememberInfiniteTransition(label = "center_pulse")
            val centerScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            val centerSize = when (apertureState) {
                ApertureState.OPEN -> 10.dp
                else -> 6.dp
            }
            Box(
                modifier = Modifier
                    .size(centerSize * centerScale)
                    .clip(CircleShape)
                    .background(HasselbladOrange)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 当前状态文字
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.25.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 哈苏橙渐变进度条
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(HasselbladOrange, HasselbladOrangeLight, Color(0xFFFFB366))
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分析进度",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 12.sp,
                letterSpacing = 0.4.sp
            )
            Text(
                text = "${progress.toInt()}%",
                color = HasselbladOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 分析步骤列表
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            steps.forEach { step ->
                AnalysisStepItem(step = step)
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        // 底部品牌标识
        Text(
            text = "HNCS · HASSELBLAD NATURAL COLOR SOLUTION",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * RESULTS 阶段：场景画像 + 推荐色彩模式 + 哈苏参数 + 胶片 + 大师建议
 *
 * 产品经理UX优化要点：
 * 1. 信息层级：缩略图→场景识别→色彩模式→参数→胶片→建议，由总到分
 * 2. 视觉锚点：置信度百分比标签作为场景识别的视觉焦点
 * 3. 操作显性：底部双按钮固定，避免用户迷失
 * 4. 色彩模式：AI推荐标签+选中态高亮，降低选择成本
 * 5. 参数可视化：数值直接展示，专业用户可快速确认
 */
@Composable
private fun ResultsContent(
    result: AnalysisResult?,
    error: String?,
    capturedBitmap: Bitmap?,
    selectedColorModeId: String,
    onSelectColorMode: (String) -> Unit,
    onApplyAndPreview: () -> Unit,
    onRetake: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error, null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "分析失败",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F).copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (result != null) {
            val profile = result.sceneProfile

            // 缩略图 - 顶部视觉锚点
            item {
                if (capturedBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedBitmap.asImageBitmap(),
                            contentDescription = "拍摄的照片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 场景识别结果 - 核心信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HasselbladOrange.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.5.dp, HasselbladOrange.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility, null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "场景识别",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HasselbladOrange,
                                letterSpacing = 0.25.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = HasselbladOrange)
                            ) {
                                Text(
                                    "${(profile.confidence * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            profile.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = HasselbladOrange.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            profile.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 推荐色彩模式（AI 自动推荐，可切换）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette, null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "推荐色彩模式",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "AI 推荐",
                                color = HasselbladOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier
                                    .background(
                                        HasselbladOrange.copy(alpha = 0.12f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // 色彩模式选择列表
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            colorModes.forEach { mode ->
                                ColorModeOption(
                                    mode = mode,
                                    isSelected = selectedColorModeId == mode.id,
                                    isRecommended = mode.id == result.suggestedColorModeId,
                                    onClick = { onSelectColorMode(mode.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 哈苏参数推荐
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune, null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "哈苏大师参数",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        val hp = profile.hasselbladParams
                        listOf(
                            "影调" to hp.tone,
                            "饱和度" to hp.saturation,
                            "对比度" to hp.contrast,
                            "色温" to hp.colorTemp,
                            "锐度" to hp.sharpness,
                            "暗角" to hp.vignette,
                            "清晰度" to hp.clarity
                        ).forEach { (name, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    hp.formatParamValue(value),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HasselbladOrange
                                )
                            }
                        }
                    }
                }
            }

            // 推荐胶片
            if (result.recommendedFilms.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Movie, null,
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "推荐胶片风格",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            result.recommendedFilms.take(3).forEach { film ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            film.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (film.description.isNotEmpty()) {
                                            Text(
                                                film.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = HasselbladOrange.copy(alpha = 0.12f)
                                        )
                                    ) {
                                        Text(
                                            "${(film.matchScore * 100).toInt()}%",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = HasselbladOrange,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 拍摄建议
            if (result.masterTips.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lightbulb, null,
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "大师拍摄建议",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            result.masterTips.forEach { tip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        "•",
                                        color = HasselbladOrange,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(18.dp)
                                    )
                                    Text(
                                        tip,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 操作按钮 - 底部固定双操作
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新拍照", fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onApplyAndPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("应用并预览", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * PREVIEW 阶段：原图/效果对比 + 保存按钮
 *
 * 产品经理UX优化要点：
 * 1. 对比切换：Tab切换原图/效果，满足用户对比需求
 * 2. 水印品牌：底部HNCS水印强化品牌认知
 * 3. 加载状态：处理中显示进度，避免用户焦虑
 * 4. 操作明确：保存按钮为主色突出，重新拍照为次级操作
 */
@Composable
private fun PreviewContent(
    originalBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 预览标签 - 可切换对比
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = HasselbladOrange,
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("哈苏色彩效果", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("原图对比", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
            )
        }

        // 预览图片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (selectedTab == 0) previewBitmap else originalBitmap

                if (displayBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = if (selectedTab == 0) "哈苏色彩预览" else "原图",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HasselbladOrange, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "正在应用哈苏色彩科学...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }

                // 哈苏水印
                if (displayBitmap != null && selectedTab == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                            .background(
                                Color.Black.copy(alpha = 0.55f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "HNCS 3.0 · Hasselblad Natural Color",
                            color = Color.White,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 原图标签
                if (displayBitmap != null && selectedTab == 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "原图",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Replay, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("重新拍照", fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = !isSaving && previewBitmap != null
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存中...", color = Color.White, fontWeight = FontWeight.Medium)
                } else {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存到相册", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * DONE 阶段：保存成功提示
 *
 * 产品经理UX优化要点：
 * 1. 正向反馈：大图标+成功文案，强化用户成就感
 * 2. 品牌收尾：OMaster/Hasselblad标识，强化品牌记忆
 * 3. 操作分流：继续拍照（同场景复用）/返回（退出流程）
 * 4. 动画入场：成功图标缩放动画，增强仪式感
 */
@Composable
private fun DoneContent(
    onNewPhoto: () -> Unit,
    onBack: () -> Unit
) {
    val successScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        successScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(successScale.value)
                .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = SuccessGreen,
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "已保存到相册",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "哈苏色彩科学已成功应用到你的照片",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "OMaster / Hasselblad",
            style = MaterialTheme.typography.bodySmall,
            color = HasselbladOrange,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNewPhoto,
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("继续拍照", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text("返回", fontWeight = FontWeight.Medium)
        }
    }
}

// ==================== Shared UI Components ====================

/**
 * 色彩模式选项（用于 Results 页面）
 */
@Composable
private fun ColorModeOption(
    mode: ColorMode,
    isSelected: Boolean,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, HasselbladOrange)
            else BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(mode.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(mode.icon, null, tint = mode.color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mode.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "推荐",
                            color = HasselbladOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    HasselbladOrange.copy(alpha = 0.15f),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check, null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 特性亮点项（READY 阶段）
 */
@Composable
private fun FeatureHighlightItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    description: String
) {
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
                    .background(iconBgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBgColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 光圈叶片动画
 */
@Composable
private fun ApertureBladesAnimated(state: ApertureState) {
    val infiniteTransition = rememberInfiniteTransition(label = "aperture")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == ApertureState.ROTATING) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val bladeCount = 8
    val openingFactor = when (state) {
        ApertureState.OPEN -> 0.3f
        ApertureState.OPENING -> 0.5f
        ApertureState.ROTATING -> 0.8f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .size(128.dp)
            .then(
                if (state == ApertureState.ROTATING) Modifier.rotate(rotation) else Modifier
            )
    ) {
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
        AnalysisStatus.PROCESSING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        AnalysisStatus.PENDING -> Color.Transparent
    }

    val textColor = when (step.status) {
        AnalysisStatus.COMPLETED -> HasselbladOrange
        AnalysisStatus.PROCESSING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        AnalysisStatus.PENDING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (step.status) {
            AnalysisStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.Done,
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
                val pendingColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .then(
                            Modifier.drawBehind {
                                drawCircle(
                                    color = pendingColor,
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        )
                )
            }
        }

        Text(
            text = step.name,
            color = textColor,
            fontSize = 14.sp
        )

        if (step.status == AnalysisStatus.COMPLETED) {
            Text(
                text = "完成",
                color = HasselbladOrange.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// ==================== Core Processing Functions ====================

/**
 * 应用哈苏色彩科学（核心接口）
 * 将场景分析得到的 HasselbladParams 与用户选择的色彩模式风格叠加
 *
 * 产品经理技术规范：
 * - 参数归一化：所有输入参数必须 coerceIn 到合理范围，避免极端值导致图像损坏
 * - 性能优化：单次 Bitmap 分配 + 离屏 Canvas，避免内存抖动
 * - 顺序固定：先饱和度→对比度→色温→影调→清晰度，叠加顺序与用户感知一致
 */
fun applyHasselbladColorScience(
    source: Bitmap,
    hasselbladParams: HasselbladParams,
    colorModeParams: Map<String, Int> = emptyMap()
): Bitmap {
    val saturation = ((hasselbladParams.saturation + (colorModeParams["saturation"] ?: 0)) / 100f).coerceIn(-1f, 1f)
    val contrast = 1f + ((hasselbladParams.contrast + (colorModeParams["contrast"] ?: 0)) / 100f).coerceIn(-0.5f, 0.5f)
    val warmth = ((hasselbladParams.colorTemp + (colorModeParams["warmth"] ?: 0)) / 100f).coerceIn(-0.5f, 0.5f)
    val tone = (hasselbladParams.tone / 100f).coerceIn(-0.3f, 0.3f)
    val clarity = ((hasselbladParams.clarity + (colorModeParams["clarity"] ?: 0)) / 100f).coerceIn(-0.5f, 0.5f)
    val vibrance = (colorModeParams["vibrance"] ?: 0) / 100f

    val matrix = ColorMatrix().apply {
        // 饱和度：先自然饱和度
        setSaturation((1f + saturation).coerceIn(0f, 2f))
        // 对比度 + 影调（亮度偏移）+ 清晰度（中间调对比增强）
        val postMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, tone * 25f + clarity * 10f,
            0f, contrast, 0f, 0f, tone * 10f + clarity * 5f,
            0f, 0f, contrast, 0f, -warmth * 15f - clarity * 5f,
            0f, 0f, 0f, 1f, 0f
        ))
        setConcat(this, postMatrix)
    }

    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(matrix)
        isFilterBitmap = true
    }
    canvas.drawBitmap(source, 0f, 0f, paint)

    // Vibrance 效果（vibrance 比 saturation 更柔和，提升欠饱和区域的鲜艳度）
    if (vibrance > 0f || vibrance < 0f) {
        val vibrancePaint = Paint().apply { isFilterBitmap = true }
        // 通过额外的半透明叠加来模拟 vibrance
        val overlayAlpha = (kotlin.math.abs(vibrance) * 50f).toInt().coerceIn(0, 80)
        val overlayColor = if (vibrance > 0f) {
            android.graphics.Color.argb(overlayAlpha, 255, 200, 150) // 暖色增强
        } else {
            android.graphics.Color.argb(overlayAlpha, 180, 200, 255) // 冷色降低
        }
        vibrancePaint.color = overlayColor
        canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vibrancePaint)
    }

    // 暗角效果（HNCS 标志性特征）
    if (hasselbladParams.vignette > 0) {
        val vignettePaint = Paint().apply { isFilterBitmap = true }
        val cx = output.width / 2f
        val cy = output.height / 2f
        val radius = maxOf(cx, cy)
        val shader = android.graphics.RadialGradient(
            cx, cy, radius,
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.argb(
                (hasselbladParams.vignette * 8.5f).toInt().coerceIn(0, 255), 0, 0, 0
            ),
            android.graphics.Shader.TileMode.CLAMP
        )
        vignettePaint.shader = shader
        canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vignettePaint)
    }

    return output
}

/**
 * 保存图片到相册
 */
private suspend fun saveImageToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val filename = "Hasselblad_${System.currentTimeMillis()}.jpg"
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/OMaster/Hasselblad")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("HasselbladScreen", "Save failed", e)
            false
        }
    }
}

/**
 * 启动分析流程
 */
private fun startAnalysis(
    bitmap: Bitmap,
    inferenceEngine: MasterInferenceEngine,
    onProgressUpdate: (Float, String, List<AnalysisStep>) -> Unit,
    onComplete: (AnalysisResult) -> Unit,
    onError: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        try {
            val steps = defaultAnalysisSteps().toMutableList()

            // Phase 1: 初始化
            onProgressUpdate(5f, "正在读取光影信息...", steps.map { it.copy() })
            delay(200)

            // Phase 2: 旋转准备
            onProgressUpdate(15f, "准备分析引擎...", steps.map { it.copy() })
            delay(300)

            // Step 1: 色彩分析
            steps[0] = steps[0].copy(status = AnalysisStatus.PROCESSING)
            onProgressUpdate(20f, "色彩分析中...", steps.map { it })

            val profile = withContext(Dispatchers.Default) {
                inferenceEngine.analyzeImage(bitmap, imagePath = null)
            }

            steps[0] = steps[0].copy(status = AnalysisStatus.COMPLETED)
            val meanLuma = profile.histogramData?.meanLuminance?.toInt() ?: 0
            onProgressUpdate(40f, "色彩分析完成 · 平均亮度 $meanLuma", steps.map { it })

            // Step 2: 光影结构分析
            steps[1] = steps[1].copy(status = AnalysisStatus.PROCESSING)
            onProgressUpdate(55f, "光影结构分析中...", steps.map { it })

            val shadowClip = profile.histogramData?.shadowClipping == true
            val highlightClip = profile.histogramData?.highlightClipping == true
            delay(100)

            steps[1] = steps[1].copy(status = AnalysisStatus.COMPLETED)
            val lightMsg = if (shadowClip || highlightClip) {
                "光影分析完成 · 检测到${if (shadowClip) "阴影" else ""}${if (shadowClip && highlightClip) "/" else ""}${if (highlightClip) "高光" else ""}裁剪"
            } else {
                "光影分析完成 · 动态范围正常"
            }
            onProgressUpdate(70f, lightMsg, steps.map { it })

            // Step 3: 场景匹配
            steps[2] = steps[2].copy(status = AnalysisStatus.PROCESSING)
            onProgressUpdate(80f, "场景匹配中...", steps.map { it })

            val faceCount = profile.faceData?.faces?.size ?: 0
            delay(100)

            steps[2] = steps[2].copy(status = AnalysisStatus.COMPLETED)
            onProgressUpdate(90f, "场景匹配完成 · ${profile.name} (${(profile.confidence * 100).toInt()}%)", steps.map { it })

            // Step 4: 胶片推荐
            steps[3] = steps[3].copy(status = AnalysisStatus.PROCESSING)
            onProgressUpdate(93f, "胶片推荐中...", steps.map { it })

            val films = if (profile.recommendedFilm.isNotEmpty()) {
                profile.recommendedFilm
            } else {
                inferenceEngine.getRecommendedFilms(profile.id)
            }

            steps[3] = steps[3].copy(status = AnalysisStatus.COMPLETED)
            val topFilm = films.firstOrNull()
            onProgressUpdate(96f, "胶片推荐完成 · ${topFilm?.name ?: "CC 经典负片"}", steps.map { it })

            // Step 5: 参数优化
            steps[4] = steps[4].copy(status = AnalysisStatus.PROCESSING)
            onProgressUpdate(98f, "哈苏参数优化中...", steps.map { it })

            val masterTips = if (profile.masterTips.isNotEmpty()) {
                profile.masterTips
            } else {
                inferenceEngine.getMasterTips(profile.id)
            }

            // 根据场景推荐色彩模式
            val suggestedColorMode = suggestColorMode(profile)
            val suggestedColorModeId = suggestColorModeId(profile)

            // 参数调整建议
            val paramAdjustments = mapParamAdjustments(profile.hasselbladParams)

            steps[4] = steps[4].copy(status = AnalysisStatus.COMPLETED)
            onProgressUpdate(100f, "哈苏之眼已睁开 · 人脸数 $faceCount", steps.map { it })

            delay(300)

            onComplete(
                AnalysisResult(
                    sceneProfile = profile,
                    recommendedFilms = films,
                    masterTips = masterTips,
                    suggestedColorMode = suggestedColorMode,
                    suggestedColorModeId = suggestedColorModeId,
                    paramAdjustments = paramAdjustments
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("HasselbladScreen", "Analysis failed", e)
            onError("分析失败: ${e.message ?: "未知错误"}")
        }
    }
}

/**
 * 根据场景推荐色彩模式名称
 */
private fun suggestColorMode(profile: SceneProfile): String {
    val category = profile.category.name
    val modeMapping = mapOf(
        "PORTRAIT" to "人像肤色优化",
        "LANDSCAPE" to "风景色彩增强",
        "NIGHT" to "哈苏经典胶片",
        "FOOD" to "鲜艳色彩",
        "URBAN" to "哈苏经典胶片",
        "STILL_LIFE" to "哈苏自然色彩",
        "MACRO" to "鲜艳色彩",
        "EVENT" to "哈苏自然色彩",
        "UNKNOWN" to "哈苏自然色彩"
    )
    return modeMapping[category] ?: "哈苏自然色彩"
}

/**
 * 根据场景推荐色彩模式 ID
 */
private fun suggestColorModeId(profile: SceneProfile): String {
    val category = profile.category
    return when (category) {
        SceneCategory.PORTRAIT -> "portrait"
        SceneCategory.LANDSCAPE -> "landscape"
        SceneCategory.NIGHT -> "classic"
        SceneCategory.FOOD -> "vivid"
        SceneCategory.URBAN -> "classic"
        SceneCategory.STILL_LIFE -> "natural"
        SceneCategory.MACRO -> "vivid"
        SceneCategory.EVENT -> "natural"
        SceneCategory.UNKNOWN -> "natural"
    }
}

/**
 * 映射参数调整建议
 */
private fun mapParamAdjustments(params: HasselbladParams): Map<String, Int> {
    return mapOf(
        "saturation" to (params.saturation * 3.3f).toInt().coerceIn(-100, 100),
        "contrast" to (params.contrast * 3.3f).toInt().coerceIn(-100, 100),
        "warmth" to (params.colorTemp * 3.3f).toInt().coerceIn(-100, 100),
        "vibrance" to (params.saturation * 2f).toInt().coerceIn(-100, 100),
        "clarity" to (params.clarity * 3.3f).toInt().coerceIn(-100, 100)
    )
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
            "hasselblad_camera_${System.currentTimeMillis()}",
            ".jpg",
            cameraDir
        )
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        android.util.Log.e("HasselbladScreen", "创建相机临时文件失败", e)
        Uri.EMPTY
    }
}

/**
 * 计算 BitmapFactory 采样率
 */
private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= maxSize || h / 2 >= maxSize) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
}

/**
 * 异步解码并按需缩放
 */
private suspend fun loadBitmapFromUriAsync(
    context: android.content.Context,
    uri: Uri,
    maxSize: Int = 2048
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            val sample = calculateInSampleSize(opts.outWidth, opts.outHeight, maxSize)
            val realOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { input2 ->
                BitmapFactory.decodeStream(input2, null, realOpts)
            }
        }
    } catch (e: Throwable) {
        android.util.Log.e("HasselbladScreen", "decodeStream failed: ${e.message}", e)
        null
    }
}
