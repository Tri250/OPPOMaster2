package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.silas.omaster.util.formatSigned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * 哈苏色彩科学页面 - 哈苏之眼完整流程
 * HNCS 3.0 自然色彩解决方案
 *
 * 完整流程：
 * 1. 用户选择场景/色彩模式并调节参数
 * 2. 点击「拍照」按钮启动相机
 * 3. 拍照后展示光圈分析动画
 * 4. AI 分析照片并给出推荐
 * 5. 预览应用哈苏色彩科学后的效果
 * 6. 确认保存最终图像
 */

/**
 * 色彩模式定义
 */
data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val icon: ImageVector,
    val params: Map<String, Int>
)

/**
 * 哈苏之眼工作流阶段
 */
enum class HasselbladEyeStage {
    /** 初始状态：选择色彩模式 + 调节参数 */
    SETUP,
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
    val paramAdjustments: Map<String, Int>
)

/**
 * 场景模式定义（SETUP 阶段横向选择器）
 */
private data class SceneMode(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: SceneCategory,
    val params: Map<String, Int>
)

private const val TAG = "HasselbladScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel: HasselbladEyeViewModel = viewModel()
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }

    // ===== 色彩模式列表（与Web端完全对齐） =====
    val colorModes = remember {
        listOf(
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
                Color(0xFF9C27B0), Icons.Default.Palette,
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
    }

    // ===== 场景模式列表（SETUP 阶段横向选择器） =====
    val sceneModes = remember {
        listOf(
            SceneMode(
                "scene-portrait", "人像", Icons.Default.Face, SceneCategory.PORTRAIT,
                paramsToMap(HasselbladParams(tone = -3, saturation = 10, contrast = -15))
            ),
            SceneMode(
                "scene-landscape", "风景", Icons.Default.Landscape, SceneCategory.LANDSCAPE,
                paramsToMap(HasselbladParams(tone = 5, saturation = 15, contrast = 12))
            ),
            SceneMode(
                "scene-night", "夜景", Icons.Default.DarkMode, SceneCategory.NIGHT,
                paramsToMap(HasselbladParams(tone = -15, contrast = 25, colorTemp = -5))
            ),
            SceneMode(
                "scene-food", "美食", Icons.Default.Restaurant, SceneCategory.FOOD,
                paramsToMap(HasselbladParams(tone = -5, saturation = 15, colorTemp = 10))
            ),
            SceneMode(
                "scene-street", "街拍", Icons.Default.LocationCity, SceneCategory.URBAN,
                paramsToMap(HasselbladParams(tone = -5, saturation = 5, contrast = 18))
            ),
            SceneMode(
                "scene-still", "静物", Icons.Default.LocalFlorist, SceneCategory.STILL_LIFE,
                paramsToMap(HasselbladParams(saturation = 15, tone = 5, sharpness = 10))
            ),
            SceneMode(
                "scene-macro", "微距", Icons.Default.CenterFocusWeak, SceneCategory.MACRO,
                paramsToMap(HasselbladParams(sharpness = 25, contrast = 15, saturation = 10))
            ),
            SceneMode(
                "scene-event", "活动", Icons.Default.Celebration, SceneCategory.EVENT,
                paramsToMap(HasselbladParams(tone = 5, saturation = 10, colorTemp = 10))
            )
        )
    }

    // ===== 观察 ViewModel 状态 =====
    val stage by viewModel.stage.collectAsState()
    val params by viewModel.params.collectAsState()
    val isParamsLocked by viewModel.isParamsLocked.collectAsState()
    val selectedModeId by viewModel.selectedModeId.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val analysisMessage by viewModel.analysisMessage.collectAsState()
    val analysisSteps by viewModel.analysisSteps.collectAsState()
    val apertureState by viewModel.apertureState.collectAsState()
    val originalBitmap by viewModel.originalBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val thumbnailPreview by viewModel.thumbnailPreview.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()

    // ===== 本地状态 =====
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var recentShots by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // ===== 权限与启动器 =====
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能使用哈苏之眼", Toast.LENGTH_LONG).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                scope.launch {
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        recentShots = (listOf(uri) + recentShots).take(3)
                        viewModel.startAnalysis(bitmap, inferenceEngine, colorModes)
                    } else {
                        Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val bitmap = loadBitmapFromUri(context, it)
                if (bitmap != null) {
                    recentShots = (listOf(it) + recentShots).take(3)
                    viewModel.startAnalysis(bitmap, inferenceEngine, colorModes)
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ===== 辅助函数 =====
    fun launchCamera() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val hasPermission = ContextCompat.checkSelfPermission(
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

    fun onPickFromGallery() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        galleryLauncher.launch("image/*")
    }

    fun onSceneModeSelected(sceneMode: SceneMode) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateSelectedMode(sceneMode.id, sceneMode.params)
    }

    fun onColorModeSelected(colorMode: ColorMode) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateSelectedMode(colorMode.id, colorMode.params)
    }

    fun onParamChanged(key: String, value: Int, triggerPreview: Boolean = false) {
        viewModel.updateParam(key, value)
        val bitmap = originalBitmap
        if (triggerPreview && bitmap != null) {
            viewModel.updatePreviewAsync(bitmap, emptyMap())
        }
    }

    fun onPreviewEffect() {
        val source = originalBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.generateFullPreview(source, emptyMap())
    }

    fun onSaveImage() {
        val bitmap = previewBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.saveImage(context, bitmap, exportFormat)
    }

    fun onShareImage() {
        val bitmap = previewBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.shareImage(context, bitmap)
    }

    fun onRetake() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.clear()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (stage) {
                            HasselbladEyeStage.SETUP -> "哈苏色彩科学"
                            HasselbladEyeStage.ANALYZING -> "哈苏之眼 · 分析中"
                            HasselbladEyeStage.RESULTS -> "哈苏之眼 · 分析结果"
                            HasselbladEyeStage.PREVIEW -> "哈苏之眼 · 预览"
                            HasselbladEyeStage.DONE -> "哈苏之眼 · 已完成"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (stage) {
                            HasselbladEyeStage.RESULTS,
                            HasselbladEyeStage.PREVIEW,
                            HasselbladEyeStage.DONE -> onRetake()
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)) togetherWith
                    fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f))
            },
            label = "hasselblad_stage",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentStage: HasselbladEyeStage ->
            when (currentStage) {
                HasselbladEyeStage.SETUP -> SetupContent(
                    colorModes = colorModes,
                    sceneModes = sceneModes,
                    selectedModeId = selectedModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    recentShots = recentShots,
                    onSceneModeSelected = { onSceneModeSelected(it) },
                    onParamChanged = { key, value -> onParamChanged(key, value) },
                    onParamsLockedChanged = { viewModel.setParamsLocked(it) },
                    onLaunchCamera = { launchCamera() },
                    onPickFromGallery = { onPickFromGallery() },
                    onRecentShotClick = { uri ->
                        scope.launch {
                            val bitmap = loadBitmapFromUri(context, uri)
                            if (bitmap != null) {
                                viewModel.startAnalysis(bitmap, inferenceEngine, colorModes)
                            } else {
                                Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onResetParams = { viewModel.resetToRecommended() }
                )

                HasselbladEyeStage.ANALYZING -> AnalyzingContent(
                    apertureState = apertureState,
                    progress = analysisProgress,
                    message = analysisMessage,
                    steps = analysisSteps,
                    onCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.cancelAnalysis()
                    }
                )

                HasselbladEyeStage.RESULTS -> ResultsContent(
                    result = analysisResult,
                    error = analysisError,
                    originalBitmap = originalBitmap,
                    thumbnailPreview = thumbnailPreview,
                    colorModes = colorModes,
                    selectedModeId = selectedModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    onColorModeSelected = { onColorModeSelected(it) },
                    onParamChanged = { key, value ->
                        onParamChanged(key, value, triggerPreview = true)
                    },
                    onParamsLockedChanged = { viewModel.setParamsLocked(it) },
                    onPreviewEffect = { onPreviewEffect() },
                    onRetake = { onRetake() },
                    onGenerateThumbnail = {
                        val bitmap = originalBitmap
                        if (bitmap != null) {
                            viewModel.updatePreviewAsync(bitmap, emptyMap())
                        }
                    }
                )

                HasselbladEyeStage.PREVIEW -> PreviewContent(
                    originalBitmap = originalBitmap,
                    previewBitmap = previewBitmap,
                    params = params,
                    exportFormat = exportFormat,
                    isSaving = isSaving,
                    onExportFormatChanged = { viewModel.setExportFormat(it) },
                    onSave = { onSaveImage() },
                    onShare = { onShareImage() },
                    onRetake = { onRetake() }
                )

                HasselbladEyeStage.DONE -> DoneContent(
                    result = analysisResult,
                    onCompare = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setStage(HasselbladEyeStage.PREVIEW)
                    },
                    onShare = { onShareImage() },
                    onNewPhoto = { onRetake() },
                    onBack = onBack
                )
            }
        }
    }
}

// ==================== 阶段1：SETUP ====================

@Composable
private fun SetupContent(
    colorModes: List<ColorMode>,
    sceneModes: List<SceneMode>,
    selectedModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    recentShots: List<Uri>,
    onSceneModeSelected: (SceneMode) -> Unit,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRecentShotClick: (Uri) -> Unit,
    onResetParams: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HncsHeroCard() }

        item {
            Text(
                text = "场景模式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(sceneModes) { sceneMode ->
                    SceneModeChip(
                        sceneMode = sceneMode,
                        isSelected = selectedModeId == sceneMode.id,
                        onClick = { onSceneModeSelected(sceneMode) }
                    )
                }
            }
        }

        item {
            Text(
                text = "参数精细调节",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            ParamsPanel(
                params = params,
                isParamsLocked = isParamsLocked,
                onParamChanged = onParamChanged,
                onParamsLockedChanged = onParamsLockedChanged,
                onResetParams = onResetParams
            )
        }

        item {
            Text(
                text = "色彩模式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(colorModes) { mode ->
                    ColorModeChip(
                        mode = mode,
                        isSelected = selectedModeId == mode.id,
                        onClick = { onSceneModeSelected(SceneMode(mode.id, mode.name, mode.icon, SceneCategory.UNKNOWN, mode.params)) }
                    )
                }
            }
        }

        item {
            Text(
                text = "哈苏之眼",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            ShutterCard(
                onLaunchCamera = onLaunchCamera,
                onPickFromGallery = onPickFromGallery
            )
        }

        if (recentShots.isNotEmpty()) {
            item {
                Text(
                    text = "最近拍摄",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                RecentShotsRow(
                    recentShots = recentShots,
                    onShotClick = onRecentShotClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun HncsHeroCard() {
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
                    Brush.horizontalGradient(
                        listOf(HasselbladOrange, HasselbladOrangeLight)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
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
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "HNCS 3.0",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "哈苏自然色彩解决方案",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "拍照 → AI分析 → 色彩推荐 → 预览保存",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneModeChip(
    sceneMode: SceneMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(sceneMode.name) },
        leadingIcon = {
            Icon(
                imageVector = sceneMode.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange,
            selectedLeadingIconColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = HasselbladOrange,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun ColorModeChip(
    mode: ColorMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(mode.name) },
        leadingIcon = {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = if (isSelected) HasselbladOrange else mode.color,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange,
            selectedLeadingIconColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = HasselbladOrange,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun ParamsPanel(
    params: HasselbladParams,
    isParamsLocked: Boolean,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onResetParams: () -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isParamsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isParamsLocked) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "锁定参数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = isParamsLocked,
                    onCheckedChange = onParamsLockedChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HasselbladOrange,
                        checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ParamSlider(
                label = "饱和度",
                value = params.saturation,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("saturation", it) }
            )
            ParamSlider(
                label = "对比度",
                value = params.contrast,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("contrast", it) }
            )
            ParamSlider(
                label = "色温",
                value = params.colorTemp,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("colorTemp", it) }
            )
            ParamSlider(
                label = "影调",
                value = params.tone,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("tone", it) }
            )
            ParamSlider(
                label = "锐度",
                value = params.sharpness,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("sharpness", it) }
            )
            ParamSlider(
                label = "暗角",
                value = params.vignette,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("vignette", it) }
            )
            ParamSlider(
                label = "青品调",
                value = params.cyanMagenta,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("cyanMagenta", it) }
            )
            ParamSlider(
                label = "高光",
                value = params.highlights,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("highlights", it) }
            )
            ParamSlider(
                label = "阴影",
                value = params.shadows,
                range = -30f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("shadows", it) }
            )
            ParamSlider(
                label = "清晰度",
                value = params.clarity,
                range = 0f..30f,
                enabled = !isParamsLocked,
                onValueChange = { onParamChanged("clarity", it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onResetParams,
                enabled = !isParamsLocked,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "重置参数",
                    color = HasselbladOrange
                )
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (enabled) HasselbladOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = value.formatSigned(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                thumbColor = HasselbladOrange,
                disabledActiveTrackColor = HasselbladOrange.copy(alpha = 0.3f),
                disabledThumbColor = HasselbladOrange.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ShutterCard(
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择一张照片，让哈苏之眼为你分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 大圆形快门按钮
            Button(
                onClick = onLaunchCamera,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = CircleShape,
                modifier = Modifier.size(88.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击拍照",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onPickFromGallery,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "从相册选择",
                    color = HasselbladOrange,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun RecentShotsRow(
    recentShots: List<Uri>,
    onShotClick: (Uri) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(recentShots) { uri ->
            RecentShotThumbnail(
                uri = uri,
                onClick = { onShotClick(uri) }
            )
        }
    }
}

@Composable
private fun RecentShotThumbnail(
    uri: Uri,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        thumbnail = withContext(Dispatchers.IO) {
            loadBitmapFromUri(context, uri, maxDimension = 200)
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(72.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "最近拍摄",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ==================== 阶段2：ANALYZING ====================

@Composable
private fun AnalyzingContent(
    apertureState: ApertureState,
    progress: Float,
    message: String,
    steps: List<AnalysisStep>,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 光圈动画
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
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

            ApertureBladesAnimated(state = apertureState)

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

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 哈苏橙渐变进度条
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(HasselbladOrange, HasselbladOrangeLight, Color(0xFFFFB366))
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分析进度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Text(
                text = "${progress.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 分析步骤列表
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            steps.forEach { step ->
                AnalysisStepItem(step = step)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "取消分析")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HNCS · HASSELBLAD NATURAL COLOR SOLUTION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            letterSpacing = 2.sp
        )
    }
}

// ==================== 阶段3：RESULTS ====================

@Composable
private fun ResultsContent(
    result: AnalysisResult?,
    error: String?,
    originalBitmap: Bitmap?,
    thumbnailPreview: Bitmap?,
    colorModes: List<ColorMode>,
    selectedModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    onColorModeSelected: (ColorMode) -> Unit,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onPreviewEffect: () -> Unit,
    onRetake: () -> Unit,
    onGenerateThumbnail: () -> Unit
) {
    // 进入结果页时生成缩略图
    LaunchedEffect(Unit) {
        onGenerateThumbnail()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            item { AnalysisErrorCard(error = error) }
        } else if (result != null) {
            val profile = result.sceneProfile

            // 缩略图预览
            item {
                ThumbnailPreviewCard(
                    originalBitmap = originalBitmap,
                    thumbnailPreview = thumbnailPreview
                )
            }

            // 场景识别卡片
            item {
                SceneRecognitionCard(profile = profile)
            }

            // 横向色彩模式选择器
            item {
                Text(
                    text = "推荐色彩模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(colorModes) { mode ->
                        ColorModeChip(
                            mode = mode,
                            isSelected = selectedModeId == mode.id,
                            onClick = { onColorModeSelected(mode) }
                        )
                    }
                }
            }

            // 哈苏大师参数 - 可调 Slider
            item {
                Text(
                    text = "哈苏大师参数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                ParamsPanel(
                    params = params,
                    isParamsLocked = isParamsLocked,
                    onParamChanged = onParamChanged,
                    onParamsLockedChanged = onParamsLockedChanged,
                    onResetParams = { }
                )
            }

            // 推荐胶片风格
            if (result.recommendedFilms.isNotEmpty()) {
                item {
                    FilmRecommendationCard(films = result.recommendedFilms)
                }
            }

            // 大师拍摄建议
            if (result.masterTips.isNotEmpty()) {
                item {
                    MasterTipsCard(tips = result.masterTips)
                }
            }
        }

        // 操作按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "重新拍照")
                }
                Button(
                    onClick = onPreviewEffect,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "预览效果", color = Color.White)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ThumbnailPreviewCard(
    originalBitmap: Bitmap?,
    thumbnailPreview: Bitmap?
) {
    GlassCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (originalBitmap != null) {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "原图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (thumbnailPreview != null) {
                Image(
                    bitmap = thumbnailPreview.asImageBitmap(),
                    contentDescription = "实时预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (originalBitmap == null && thumbnailPreview == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HasselbladOrange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "正在生成预览...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneRecognitionCard(profile: SceneProfile) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "场景识别",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange)
                ) {
                    Text(
                        text = "${(profile.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FilmRecommendationCard(films: List<FilmPreset>) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "推荐胶片风格",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            films.take(3).forEach { film ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = film.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (film.description.isNotEmpty()) {
                            Text(
                                text = film.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = HasselbladOrange.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            text = "${(film.matchScore * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = HasselbladOrange,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterTipsCard(tips: List<String>) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "大师拍摄建议",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            tips.forEach { tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        color = HasselbladOrange,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(16.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "分析失败",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD32F2F).copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== 阶段4：PREVIEW ====================

@Composable
private fun PreviewContent(
    originalBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    params: HasselbladParams,
    exportFormat: HasselbladEyeViewModel.ExportFormat,
    isSaving: Boolean,
    onExportFormatChanged: (HasselbladEyeViewModel.ExportFormat) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Before/After 滑动对比
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (originalBitmap != null && previewBitmap != null) {
                    BeforeAfterCompare(
                        originalBitmap = originalBitmap,
                        previewBitmap = previewBitmap
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HasselbladOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在应用哈苏色彩科学...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // 参数摘要
        ParamSummaryCard(params = params)

        // 导出格式选择
        ExportFormatSelector(
            selected = exportFormat,
            onSelected = onExportFormatChanged
        )

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "重新拍照")
            }

            OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "分享", color = HasselbladOrange)
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "保存中...", color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "保存", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterCompare(
    originalBitmap: Bitmap,
    previewBitmap: Bitmap
) {
    var dividerPosition by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newPos = change.position.x / size.width
                    dividerPosition = newPos.coerceIn(0.05f, 0.95f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth

        // 右侧效果图（全图）
        Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = "效果图",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 左侧原图（按 dividerPosition 裁剪）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(width * dividerPosition)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        ) {
            Image(
                bitmap = originalBitmap.asImageBitmap(),
                contentDescription = "原图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset(x = width * dividerPosition - 1.dp)
                .background(HasselbladOrange)
                .align(Alignment.CenterStart)
        )

        // 标签
        Text(
            text = "原图",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "哈苏效果",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(HasselbladOrange.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ParamSummaryCard(params: HasselbladParams) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "参数摘要",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ParamSummaryItem("影调", params.tone.formatSigned())
                ParamSummaryItem("饱和度", params.saturation.formatSigned())
                ParamSummaryItem("对比度", params.contrast.formatSigned())
                ParamSummaryItem("色温", params.colorTemp.formatSigned())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ParamSummaryItem("锐度", params.sharpness.formatSigned())
                ParamSummaryItem("暗角", params.vignette.formatSigned())
                ParamSummaryItem("清晰度", "${params.clarity}")
                ParamSummaryItem("青品调", params.cyanMagenta.formatSigned())
            }
        }
    }
}

@Composable
private fun ParamSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = HasselbladOrange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ExportFormatSelector(
    selected: HasselbladEyeViewModel.ExportFormat,
    onSelected: (HasselbladEyeViewModel.ExportFormat) -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "导出格式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HasselbladEyeViewModel.ExportFormat.entries.forEach { format ->
                    val label = when (format) {
                        HasselbladEyeViewModel.ExportFormat.JPEG -> "JPEG"
                        HasselbladEyeViewModel.ExportFormat.PNG -> "PNG"
                        HasselbladEyeViewModel.ExportFormat.WEBP -> "WEBP"
                    }
                    FilterChip(
                        selected = selected == format,
                        onClick = { onSelected(format) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                            selectedLabelColor = HasselbladOrange
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = HasselbladOrange,
                            enabled = true,
                            selected = selected == format
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== 阶段5：DONE ====================

@Composable
private fun DoneContent(
    result: AnalysisResult?,
    onCompare: () -> Unit,
    onShare: () -> Unit,
    onNewPhoto: () -> Unit,
    onBack: () -> Unit
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }

    val successScale by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "success_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 成功动画图标
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = successScale, scaleY = successScale)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "已保存到相册",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "哈苏色彩科学已成功应用到你的照片",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "OMaster/Hasselblad",
            style = MaterialTheme.typography.bodySmall,
            color = HasselbladOrange,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // EXIF 信息卡片
        ExifInfoCard(result = result)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCompare,
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Compare,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "对比",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onShare,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.dp, HasselbladOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = HasselbladOrange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "分享", color = HasselbladOrange)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNewPhoto,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "继续拍照")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text(text = "返回")
        }
    }
}

@Composable
private fun ExifInfoCard(result: AnalysisResult?) {
    val exif = result?.sceneProfile?.exifData
    val cameraParams = result?.sceneProfile?.cameraParams

    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXIF 信息",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val items = listOf(
                "ISO" to (exif?.iso?.toString() ?: cameraParams?.iso?.toString() ?: "--"),
                "快门" to (exif?.exposureTime ?: cameraParams?.shutterSpeed ?: "--"),
                "光圈" to (exif?.fNumber?.let { "f/$it" } ?: cameraParams?.aperture?.let { "f/$it" } ?: "--"),
                "焦距" to (exif?.focalLength?.let { "${it}mm" } ?: cameraParams?.focalLength?.let { "${it}mm" } ?: "--"),
                "拍摄时间" to (exif?.dateTime ?: "--")
            )

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// ==================== 通用组件 ====================

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        content()
    }
}

// ==================== 光圈与分析步骤组件 ====================

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
                        .clip(CircleShape)
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        if (step.status == AnalysisStatus.COMPLETED) {
            Text(
                text = "完成",
                color = HasselbladOrange.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ==================== 业务辅助函数 ====================

/**
 * 根据场景推荐色彩模式
 */
private fun suggestColorMode(profile: SceneProfile, colorModes: List<ColorMode>): String {
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
    val displayName = modeMapping[profile.category.name] ?: "哈苏自然色彩"
    return colorModes.find { it.name == displayName }?.name ?: displayName
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
 * 使用应用私有缓存目录 + FileProvider
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
        Log.e(TAG, "创建相机临时文件失败", e)
        Uri.EMPTY
    }
}

/**
 * 从 Uri 加载 Bitmap，并按 maxDimension 缩放
 */
private suspend fun loadBitmapFromUri(
    context: android.content.Context,
    uri: Uri,
    maxDimension: Int = 2048
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)

            val scale = maxOf(options.outWidth, options.outHeight).toFloat() / maxDimension
            val sampleSize = if (scale > 1) {
                scale.toInt().coerceAtLeast(2).takeHighestOneBit()
            } else 1

            context.contentResolver.openInputStream(uri)?.use { input2 ->
                BitmapFactory.decodeStream(
                    input2,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "loadBitmapFromUri failed", e)
        null
    }
}

/**
 * 将 HasselbladParams 转为参数 Map，供 ViewModel 使用
 */
private fun paramsToMap(params: HasselbladParams): Map<String, Int> = mapOf(
    "tone" to params.tone,
    "saturation" to params.saturation,
    "contrast" to params.contrast,
    "colorTemp" to params.colorTemp,
    "sharpness" to params.sharpness,
    "vignette" to params.vignette,
    "cyanMagenta" to params.cyanMagenta,
    "softLight" to params.softLight.ordinal,
    "highlights" to params.highlights,
    "shadows" to params.shadows,
    "clarity" to params.clarity
)
