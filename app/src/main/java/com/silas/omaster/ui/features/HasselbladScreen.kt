package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.ui.components.AnalysisStatus
import com.silas.omaster.ui.components.AnalysisStep
import com.silas.omaster.ui.components.ApertureState
import com.silas.omaster.ui.components.defaultAnalysisSteps
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.HasselbladOrangeLight
import com.silas.omaster.util.formatSigned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "HasselbladEye"

private data class SceneMode(
    val id: String,
    val name: String,
    val description: String,
    val category: SceneCategory,
    val confidence: Float,
    val params: Map<String, Int>,
    val icon: String
)

data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Long,
    val params: Map<String, Int>
)

enum class HasselbladEyeStage { SETUP, ANALYZING, RESULTS, PREVIEW, DONE }

data class AnalysisResult(
    val sceneProfile: SceneProfile,
    val recommendedFilms: List<FilmPreset>,
    val masterTips: List<String>,
    val suggestedColorMode: String,
    val paramAdjustments: Map<String, Int>
)

private val allSceneModes: List<SceneMode> = listOf(
    SceneMode("scene-portrait", "人像大师", "肤色自然 · 背景虚化 · 柔和色调", SceneCategory.PORTRAIT, 0.92f,
        mapOf("tone" to -3, "saturation" to 10, "contrast" to -15, "colorTemp" to 5, "vignette" to 8), "👤"),
    SceneMode("scene-landscape", "风景增强", "天空湛蓝 · 植被浓郁 · 层次分明", SceneCategory.LANDSCAPE, 0.88f,
        mapOf("tone" to 5, "saturation" to 15, "contrast" to 12, "clarity" to 10, "highlights" to -5), "🏔️"),
    SceneMode("scene-night", "夜景星空", "降噪保留细节 · 暗部纯净 · 星芒锐利", SceneCategory.NIGHT, 0.85f,
        mapOf("tone" to -15, "contrast" to 25, "colorTemp" to -5, "sharpness" to 8, "clarity" to 5), "🌃"),
    SceneMode("scene-food", "美食胶片", "色彩鲜艳 · 质感润泽 · 食欲诱惑", SceneCategory.FOOD, 0.87f,
        mapOf("tone" to -5, "saturation" to 15, "colorTemp" to 10, "sharpness" to 5), "🍜"),
    SceneMode("scene-urban", "城市街拍", "对比鲜明 · 质感硬朗 · 电影感强", SceneCategory.URBAN, 0.83f,
        mapOf("tone" to -5, "saturation" to 5, "contrast" to 18, "cyanMagenta" to -8), "🏢"),
    SceneMode("scene-still", "静物小品", "色彩细腻 · 质感丰富 · 主体突出", SceneCategory.STILL_LIFE, 0.86f,
        mapOf("saturation" to 15, "tone" to 5, "sharpness" to 10, "clarity" to 8), "📦"),
    SceneMode("scene-macro", "微距世界", "细节锐利 · 色彩鲜艳 · 背景虚化", SceneCategory.MACRO, 0.84f,
        mapOf("sharpness" to 25, "contrast" to 15, "saturation" to 10, "clarity" to 10), "🔍"),
    SceneMode("scene-event", "活动纪实", "抓拍精彩 · 动态丰富 · 真实感人", SceneCategory.EVENT, 0.82f,
        mapOf("tone" to 5, "saturation" to 10, "colorTemp" to 10, "sharpness" to 5), "🎉"),
    SceneMode("scene-natural", "自然色彩", "HNCS 3.0 哈苏自然色彩科学（默认推荐）", SceneCategory.UNKNOWN, 0.0f,
        emptyMap(), "✨")
)

private val allColorModes: List<ColorMode> = listOf(
    ColorMode("natural", "自然色彩", "HNCS 3.0 自然色彩，哈苏色彩克制哲学", 0xFFFF6B35,
        mapOf("saturation" to 0, "contrast" to 5, "warmth" to 0, "clarity" to 0, "highlights" to 0)),
    ColorMode("portrait", "人像肤色", "自然美化肤色，保留细节，柔和背景", 0xFFFF6B9D,
        mapOf("saturation" to 5, "contrast" to 8, "warmth" to 3, "tone" to 3, "clarity" to 0)),
    ColorMode("landscape", "风景色彩", "天空湛蓝，植被浓郁，层次分明", 0xFF4CAF50,
        mapOf("saturation" to 12, "contrast" to 10, "warmth" to 5, "clarity" to 10, "tone" to 5)),
    ColorMode("classic", "经典胶片", "复古胶片色彩质感，棕调浓郁", 0xFF9C27B0,
        mapOf("saturation" to 8, "contrast" to 15, "warmth" to 8, "clarity" to 0, "tone" to -3, "vignette" to 10)),
    ColorMode("bw", "哈苏黑白", "经典黑白摄影，高对比，暗部丰富", 0xFF212121,
        mapOf("saturation" to -30, "contrast" to 20, "clarity" to 15, "shadows" to -5, "highlights" to -10)),
    ColorMode("vivid", "鲜艳色彩", "鲜艳饱满的色彩表现，视觉冲击力强", 0xFFFF9800,
        mapOf("saturation" to 20, "contrast" to 10, "warmth" to 0, "clarity" to 5)),
    ColorMode("film-portra", "Portra 400", "柯达 Portra 400 胶片，温暖柔和人像胶片", 0xFFF4A460,
        mapOf("saturation" to 8, "contrast" to 8, "warmth" to 10, "tone" to 5)),
    ColorMode("film-cc", "CC 经典负片", "柯达 ColorPlus 200 风格，温暖怀旧色彩", 0xFFE4B060,
        mapOf("saturation" to 10, "contrast" to 12, "warmth" to 8, "tone" to 3, "vignette" to 6)),
    ColorMode("film-bw", "TX400 黑白", "Kodak Tri-X 400，高对比颗粒感黑白", 0xFF333333,
        mapOf("saturation" to -30, "contrast" to 25, "clarity" to 18, "tone" to -5))
)

private fun paramsToMap(params: HasselbladParams): Map<String, Int> = mapOf(
    "tone" to params.tone,
    "saturation" to params.saturation,
    "contrast" to params.contrast,
    "colorTemp" to params.colorTemp,
    "sharpness" to params.sharpness,
    "vignette" to params.vignette,
    "cyanMagenta" to params.cyanMagenta,
    "highlights" to params.highlights,
    "shadows" to params.shadows,
    "clarity" to params.clarity
)

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

    val stage by viewModel.stage.collectAsState()
    val params by viewModel.params.collectAsState()
    val isParamsLocked by viewModel.isParamsLocked.collectAsState()
    val selectedModeId by viewModel.selectedModeId.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val apertureState by viewModel.apertureState.collectAsState()
    val originalBitmap by viewModel.originalBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val analysisSteps by viewModel.analysisSteps.collectAsState()
    val analysisMessage by viewModel.analysisMessage.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var recentShots by remember { mutableStateOf<List<Uri>>(emptyList()) }

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
                        viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
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
                    viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    fun onParamChanged(key: String, value: Int) {
        viewModel.updateParam(key, value)
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

    fun onResetParams() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.resetToRecommended()
    }

    fun onToggleLock() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setParamsLocked(!isParamsLocked)
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
                            HasselbladEyeStage.SETUP -> "哈苏之眼"
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
                    sceneModes = allSceneModes,
                    colorModes = allColorModes,
                    selectedModeId = selectedModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    recentShots = recentShots,
                    onSceneModeSelected = ::onSceneModeSelected,
                    onColorModeSelected = ::onColorModeSelected,
                    onParamChanged = ::onParamChanged,
                    onParamsLockedChanged = { onToggleLock() },
                    onLaunchCamera = ::launchCamera,
                    onPickFromGallery = ::onPickFromGallery,
                    onRecentShotClick = { uri ->
                        scope.launch {
                            val bitmap = loadBitmapFromUri(context, uri)
                            if (bitmap != null) {
                                viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
                            } else {
                                Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onResetParams = ::onResetParams
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
                    sceneModes = allSceneModes,
                    colorModes = allColorModes,
                    selectedModeId = selectedModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    originalBitmap = originalBitmap,
                    result = analysisResult,
                    onSceneModeSelected = ::onSceneModeSelected,
                    onColorModeSelected = ::onColorModeSelected,
                    onParamChanged = ::onParamChanged,
                    onParamsLockedChanged = { onToggleLock() },
                    onPreviewEffect = ::onPreviewEffect,
                    onResetParams = ::onResetParams
                )

                HasselbladEyeStage.PREVIEW -> PreviewContent(
                    originalBitmap = originalBitmap,
                    previewBitmap = previewBitmap,
                    params = params,
                    exportFormat = exportFormat,
                    isSaving = isSaving,
                    onExportFormatChanged = { viewModel.setExportFormat(it) },
                    onSave = ::onSaveImage,
                    onShare = ::onShareImage,
                    onRetake = ::onRetake
                )

                HasselbladEyeStage.DONE -> DoneContent(
                    onShare = ::onShareImage,
                    onRetake = ::onRetake,
                    onBack = onBack
                )
            }
        }
    }
}

// ==================== 辅助组件：GlassCard ====================

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ==================== SETUP 阶段 ====================

@Composable
private fun SetupContent(
    sceneModes: List<SceneMode>,
    colorModes: List<ColorMode>,
    selectedModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    recentShots: List<Uri>,
    onSceneModeSelected: (SceneMode) -> Unit,
    onColorModeSelected: (ColorMode) -> Unit,
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
        item { HeroCard() }

        item {
            SectionTitle(title = "场景模式")
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sceneModes) { scene ->
                    SceneModeChip(
                        sceneMode = scene,
                        isSelected = selectedModeId == scene.id,
                        onClick = { onSceneModeSelected(scene) }
                    )
                }
            }
        }

        item {
            SectionTitle(title = "色彩模式")
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        item {
            SectionTitle(title = "大师参数调节")
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
            SectionTitle(title = "哈苏之眼")
        }

        item {
            ShutterCard(
                onLaunchCamera = onLaunchCamera,
                onPickFromGallery = onPickFromGallery
            )
        }

        if (recentShots.isNotEmpty()) {
            item {
                SectionTitle(title = "最近拍摄")
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
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(HasselbladOrange, HasselbladOrangeLight)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
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
                        text = "哈苏之眼 · HNCS 3.0",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OPPO Find X9 系列 · 哈苏大师影像",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "真实场景识别 · 哈苏大师参数 · HNCS 3.0 自然色彩科学",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneModeChip(
    sceneMode: SceneMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = sceneMode.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sceneMode.name)
                }
                if (sceneMode.confidence > 0f) {
                    Text(
                        text = "${(sceneMode.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = HasselbladOrange,
            enabled = true,
            selected = isSelected
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorModeChip(
    mode: ColorMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(mode.color))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(mode.name)
                }
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) HasselbladOrange.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange
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

        ParamSlider(label = "影调", value = params.tone, enabled = !isParamsLocked, onValueChange = { onParamChanged("tone", it) })
        ParamSlider(label = "饱和度", value = params.saturation, enabled = !isParamsLocked, onValueChange = { onParamChanged("saturation", it) })
        ParamSlider(label = "对比度", value = params.contrast, enabled = !isParamsLocked, onValueChange = { onParamChanged("contrast", it) })
        ParamSlider(label = "色温", value = params.colorTemp, enabled = !isParamsLocked, onValueChange = { onParamChanged("colorTemp", it) })
        ParamSlider(label = "锐度", value = params.sharpness, enabled = !isParamsLocked, onValueChange = { onParamChanged("sharpness", it) })
        ParamSlider(label = "暗角", value = params.vignette, enabled = !isParamsLocked, onValueChange = { onParamChanged("vignette", it) })
        ParamSlider(label = "青品调", value = params.cyanMagenta, enabled = !isParamsLocked, onValueChange = { onParamChanged("cyanMagenta", it) })
        ParamSlider(label = "高光", value = params.highlights, enabled = !isParamsLocked, onValueChange = { onParamChanged("highlights", it) })
        ParamSlider(label = "阴影", value = params.shadows, enabled = !isParamsLocked, onValueChange = { onParamChanged("shadows", it) })
        ParamSlider(label = "清晰度", value = params.clarity, range = 0f..30f, enabled = !isParamsLocked, onValueChange = { onParamChanged("clarity", it) })

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "柔光模式",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(SoftLightMode.NONE, SoftLightMode.SOFT, SoftLightMode.DREAMY).forEach { softMode ->
                val isSoftSelected = params.softLight == softMode
                val label = when (softMode) {
                    SoftLightMode.NONE -> "无"
                    SoftLightMode.SOFT -> "柔"
                    SoftLightMode.DREAMY -> "梦幻"
                }
                FilterChip(
                    selected = isSoftSelected,
                    onClick = { if (!isParamsLocked) onParamChanged("softLight", softMode.ordinal) },
                    enabled = !isParamsLocked,
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                        selectedLabelColor = HasselbladOrange
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = HasselbladOrange,
                        enabled = !isParamsLocked,
                        selected = isSoftSelected
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onResetParams,
            enabled = !isParamsLocked,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, HasselbladOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "重置为推荐参数", color = HasselbladOrange)
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float> = -30f..30f,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择一张照片，让哈苏之眼为你分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "从相册选择", color = HasselbladOrange)
            }
        }
    }
}

@Composable
private fun RecentShotsRow(
    recentShots: List<Uri>,
    onShotClick: (Uri) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recentShots) { uri ->
            RecentShotThumbnail(uri = uri, onClick = { onShotClick(uri) })
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

// ==================== ANALYZING 阶段 ====================

@Composable
private fun AnalyzingContent(
    apertureState: ApertureState,
    progress: Float,
    message: String,
    steps: List<AnalysisStep>,
    onCancel: () -> Unit
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val infiniteRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 2000),
        label = "aperture_rotation"
    )

    LaunchedEffect(Unit) {
        while (true) {
            rotationAngle += 360f
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .drawBehind {
                        drawCircle(
                            color = HasselbladOrange.copy(alpha = 0.3f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )

            val bladeCount = 8
            val openingFactor = when (apertureState) {
                ApertureState.OPEN -> 0.3f
                ApertureState.OPENING -> 0.5f
                else -> 1f
            }

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .rotate(infiniteRotation)
            ) {
                for (i in 0 until bladeCount) {
                    val angle = (i * 45f)
                    val alpha = 0.6f - (i * 0.05f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .rotate(angle)
                            .drawBehind {
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
                }
            }

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

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            steps.forEach { step ->
                AnalysisStepRow(step = step)
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

@Composable
private fun AnalysisStepRow(step: AnalysisStep) {
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
                val pendingRingColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .drawBehind {
                            drawCircle(
                                color = pendingRingColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                )
            }
        }
        Text(text = step.name, color = textColor, fontSize = 14.sp)
        if (step.status == AnalysisStatus.COMPLETED) {
            Text(
                text = "完成",
                color = HasselbladOrange.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// ==================== RESULTS 阶段 ====================

@Composable
private fun ResultsContent(
    sceneModes: List<SceneMode>,
    colorModes: List<ColorMode>,
    selectedModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    originalBitmap: Bitmap?,
    result: AnalysisResult?,
    onSceneModeSelected: (SceneMode) -> Unit,
    onColorModeSelected: (ColorMode) -> Unit,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onPreviewEffect: () -> Unit,
    onResetParams: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (result != null) {
            item {
                ResultPhotoCard(bitmap = originalBitmap)
            }

            item {
                SceneRecognitionCard(profile = result.sceneProfile)
            }

            if (result.masterTips.isNotEmpty()) {
                item {
                    SectionTitle(title = "哈苏大师建议")
                }
                item {
                    MasterTipsCard(tips = result.masterTips)
                }
            }

            item {
                SectionTitle(title = "推荐胶片")
            }
            item {
                RecommendedFilmsRow(films = result.recommendedFilms)
            }

            item {
                SectionTitle(title = "场景模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sceneModes) { scene ->
                        SceneModeChip(
                            sceneMode = scene,
                            isSelected = selectedModeId == scene.id,
                            onClick = { onSceneModeSelected(scene) }
                        )
                    }
                }
            }

            item {
                SectionTitle(title = "色彩模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorModes) { mode ->
                        ColorModeChip(
                            mode = mode,
                            isSelected = selectedModeId == mode.id,
                            onClick = { onColorModeSelected(mode) }
                        )
                    }
                }
            }

            item {
                SectionTitle(title = "哈苏大师参数")
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
                Button(
                    onClick = onPreviewEffect,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "生成预览图",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无分析结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ResultPhotoCard(bitmap: Bitmap?) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "原图预览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "原图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "暂无图片",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneRecognitionCard(profile: SceneProfile) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = profile.category.icon, fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "识别置信度 ${(profile.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MasterTipsCard(tips: List<String>) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tips.forEachIndexed { index, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedFilmsRow(films: List<FilmPreset>) {
    if (films.isEmpty()) {
        GlassCard {
            Text(
                text = "暂无推荐胶片",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(films) { film ->
            FilmCard(film = film)
        }
    }
}

@Composable
private fun FilmCard(film: FilmPreset) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = film.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${(film.matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "色彩风格：${film.colorStyle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = "颗粒感：${film.grainLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = "对比度：${film.contrastLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// ==================== PREVIEW 阶段 ====================

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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BeforeAfterCompareCard(
                original = originalBitmap,
                processed = previewBitmap
            )
        }

        item {
            SectionTitle(title = "参数摘要")
        }
        item {
            ParamsSummaryCard(params = params)
        }

        item {
            SectionTitle(title = "导出格式")
        }
        item {
            ExportFormatSelector(
                currentFormat = exportFormat,
                onFormatChanged = onExportFormatChanged
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Button(
                onClick = onSave,
                enabled = !isSaving && previewBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "保存中...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "保存到相册",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    enabled = previewBitmap != null,
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "分享", color = HasselbladOrange)
                }
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "重新拍摄")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun BeforeAfterCompareCard(original: Bitmap?, processed: Bitmap?) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "前后对比 · 滑动查看差异",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val maxW = constraints.maxWidth
                val maxH = constraints.maxHeight
                var splitFraction by remember { mutableFloatStateOf(0.5f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                splitFraction = (splitFraction + dragAmount.x / maxW).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (original != null) {
                            Image(
                                bitmap = original.asImageBitmap(),
                                contentDescription = "原图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(splitFraction)
                            .fillMaxHeight()
                    ) {
                        if (processed != null) {
                            Image(
                                bitmap = processed.asImageBitmap(),
                                contentDescription = "处理后",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .offset(x = (maxW * splitFraction - 1.5f).dp)
                            .background(HasselbladOrange)
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(
                                x = (maxW * splitFraction - 16).dp,
                                y = (maxH / 2 - 16).dp
                            )
                            .clip(CircleShape)
                            .background(HasselbladOrange)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⇄", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "处理后", color = Color.White, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "原图", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamsSummaryCard(params: HasselbladParams) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val paramPairs = listOf(
                "影调" to params.tone,
                "饱和度" to params.saturation,
                "对比度" to params.contrast,
                "色温" to params.colorTemp,
                "锐度" to params.sharpness,
                "暗角" to params.vignette,
                "青品调" to params.cyanMagenta,
                "高光" to params.highlights,
                "阴影" to params.shadows,
                "清晰度" to params.clarity
            )
            paramPairs.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) ->
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = value.formatSigned(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HasselbladOrange
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "柔光模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (params.softLight) {
                            SoftLightMode.NONE -> "无"
                            SoftLightMode.SOFT -> "柔"
                            SoftLightMode.DREAMY -> "梦幻"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatSelector(
    currentFormat: HasselbladEyeViewModel.ExportFormat,
    onFormatChanged: (HasselbladEyeViewModel.ExportFormat) -> Unit
) {
    val formats = listOf(
        HasselbladEyeViewModel.ExportFormat.JPEG to "JPEG",
        HasselbladEyeViewModel.ExportFormat.PNG to "PNG",
        HasselbladEyeViewModel.ExportFormat.WEBP to "WEBP"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(formats) { (format, label) ->
            FilterChip(
                selected = currentFormat == format,
                onClick = { onFormatChanged(format) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                    selectedLabelColor = HasselbladOrange
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = HasselbladOrange,
                    enabled = true,
                    selected = currentFormat == format
                )
            )
        }
    }
}

// ==================== DONE 阶段 ====================

@Composable
private fun DoneContent(
    onShare: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(HasselbladOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "完成",
                tint = HasselbladOrange,
                modifier = Modifier.size(48.dp)
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
            text = "图片已保存至 Pictures/OMaster/Hasselblad/ 目录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "分享图片", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetake,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, HasselbladOrange)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "重新拍摄", color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text(text = "返回")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HNCS 3.0",
                color = HasselbladOrange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HASSELBLAD NATURAL COLOR SOLUTION",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==================== 文件工具函数 ====================

private suspend fun loadBitmapFromUri(
    context: android.content.Context,
    uri: Uri,
    maxDimension: Int = 0
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }
        if (maxDimension > 0 && options.outWidth > 0 && options.outHeight > 0) {
            var inSampleSize = 1
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
            options.inSampleSize = inSampleSize
        }
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        null
    }
}

private fun createTempImageUri(context: android.content.Context): Uri {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Hasselblad").apply {
            if (!exists()) mkdirs()
        }
        val imageFile = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        Uri.EMPTY
    }
}
