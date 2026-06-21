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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.ui.components.AnalysisStatus
import com.silas.omaster.ui.components.AnalysisStep
import com.silas.omaster.ui.components.ApertureState
import com.silas.omaster.ui.components.defaultAnalysisSteps
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.HasselbladOrangeLight
import com.silas.omaster.util.formatSigned
import com.silas.omaster.util.hapticClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "HasselbladEye"

enum class HasselbladEyeStage { SETUP, ANALYZING, RESULTS, PREVIEW, DONE }

data class AnalysisResult(
    val sceneProfile: SceneProfile,
    val alternativeScenes: List<SceneProfile>,
    val recommendedFilms: List<FilmPreset>,
    val masterTips: List<String>,
    val suggestedColorMode: String,
    val paramAdjustments: Map<String, Int>
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
    val selectedSceneModeId by viewModel.selectedSceneModeId.collectAsState()
    val selectedColorModeId by viewModel.selectedColorModeId.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val apertureState by viewModel.apertureState.collectAsState()
    val originalBitmap by viewModel.originalBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val thumbnailPreview by viewModel.thumbnailPreview.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val lastSavedUri by viewModel.lastSavedUri.collectAsState()
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
        viewModel.updateSelectedSceneMode(sceneMode.id, sceneMode.params)
        if (isParamsLocked) {
            Toast.makeText(context, "参数已锁定：场景模式仅作参考，未覆盖当前参数", Toast.LENGTH_SHORT).show()
        }
    }

    fun onColorModeSelected(colorMode: ColorMode) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateSelectedColorMode(colorMode.id, colorMode.params)
        if (isParamsLocked) {
            Toast.makeText(context, "参数已锁定：色彩模式仅作参考，未覆盖当前参数", Toast.LENGTH_SHORT).show()
        }
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
                recentShots = recentShots,
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
                }
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
                    selectedSceneModeId = selectedSceneModeId,
                    selectedColorModeId = selectedColorModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    originalBitmap = originalBitmap,
                    thumbnailPreview = thumbnailPreview,
                    result = analysisResult,
                    onSceneModeSelected = ::onSceneModeSelected,
                    onColorModeSelected = ::onColorModeSelected,
                    onParamChanged = ::onParamChanged,
                    onParamsLockedChanged = { onToggleLock() },
                    onPreviewEffect = ::onPreviewEffect,
                    onResetParams = ::onResetParams,
                    onFilmClick = { film ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyFilmPreset(film.id)
                    },
                    onFineGrainedSceneSelected = { sceneProfile ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyFineGrainedScene(sceneProfile)
                        if (isParamsLocked) {
                            Toast.makeText(context, "参数已锁定：仅切换参考模式，未覆盖当前参数", Toast.LENGTH_SHORT).show()
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
                    onSave = ::onSaveImage,
                    onShare = ::onShareImage,
                    onRetake = ::onRetake
                )

                HasselbladEyeStage.DONE -> DoneContent(
                    onShare = ::onShareImage,
                    onViewImage = {
                        val uri = lastSavedUri
                        if (uri != null) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/*")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开相册", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "图片未保存", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditAnother = {
                        viewModel.clear()
                    },
                    onRetake = ::onRetake,
                    onBack = onBack
                )
            }
        }
    }
}

// ==================== 辅助组件：GlassCard / ColorOS 16 动效 ====================

/**
 * P2-14：ColorOS 16 风格的入场动效修饰符
 * 给卡片添加淡入 + 轻微上滑的弹簧入场效果。
 */
@Composable
private fun Modifier.colorOsEntrance(delayMillis: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "entrance_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "entrance_offset"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // P2-14：ColorOS 16 液态玻璃近似效果（多层渐变 + 高光描边）
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val highlightBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            0.5.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .background(highlightBrush)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// ==================== SETUP 阶段 ====================

@Composable
private fun SetupContent(
    recentShots: List<Uri>,
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRecentShotClick: (Uri) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard() }

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
                Column {
                    Text(
                        text = "锁定当前参数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "开启后切换场景/色彩模式不会覆盖已调参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
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
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { shadowElevation = 20.dp.toPx() },
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
    selectedSceneModeId: String,
    selectedColorModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    originalBitmap: Bitmap?,
    thumbnailPreview: Bitmap?,
    result: AnalysisResult?,
    onSceneModeSelected: (SceneMode) -> Unit,
    onColorModeSelected: (ColorMode) -> Unit,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onPreviewEffect: () -> Unit,
    onResetParams: () -> Unit,
    onFilmClick: (FilmPreset) -> Unit,
    onFineGrainedSceneSelected: (SceneProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (result != null) {
            item {
                ResultPhotoCard(bitmap = originalBitmap, preview = thumbnailPreview)
            }

            item {
                SceneRecognitionCard(profile = result.sceneProfile)
            }

            // P1-10：展示 Top-3 备选场景，支持一键切换
            if (result.alternativeScenes.isNotEmpty()) {
                item {
                    SectionTitle(title = "备选场景")
                }
                item {
                    AlternativeScenesRow(
                        alternatives = result.alternativeScenes,
                        onSceneSelected = onFineGrainedSceneSelected
                    )
                }
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
                RecommendedFilmsRow(
                    films = result.recommendedFilms,
                    onFilmClick = onFilmClick
                )
            }

            item {
                SectionTitle(title = "场景模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sceneModes) { scene ->
                        SceneModeChip(
                            sceneMode = scene,
                            isSelected = selectedSceneModeId == scene.id,
                            onClick = { onSceneModeSelected(scene) }
                        )
                    }
                }
            }

            // P1-10：当前粗粒度模式展开 2-4 个细分子模式
            item {
                SubSceneModesPanel(
                    selectedSceneModeId = selectedSceneModeId,
                    currentSceneId = result.sceneProfile.id,
                    onSceneSelected = onFineGrainedSceneSelected
                )
            }

            item {
                SectionTitle(title = "色彩模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorModes) { mode ->
                        ColorModeChip(
                            mode = mode,
                            isSelected = selectedColorModeId == mode.id,
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
private fun ResultPhotoCard(bitmap: Bitmap?, preview: Bitmap?) {
    val displayBitmap = preview ?: bitmap
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (preview != null) "实时预览" else "原图预览",
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
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = if (preview != null) "实时预览" else "原图",
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
private fun RecommendedFilmsRow(
    films: List<FilmPreset>,
    onFilmClick: (FilmPreset) -> Unit
) {
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
            FilmCard(film = film, onClick = { onFilmClick(film) })
        }
    }
}

@Composable
private fun FilmCard(
    film: FilmPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .hapticClickable(onClick = onClick),
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

// P1-10：备选场景横向列表
@Composable
private fun AlternativeScenesRow(
    alternatives: List<SceneProfile>,
    onSceneSelected: (SceneProfile) -> Unit
) {
    val top3 = alternatives.take(3)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(top3) { scene ->
            AlternativeSceneCard(scene = scene, onClick = { onSceneSelected(scene) })
        }
    }
}

@Composable
private fun AlternativeSceneCard(
    scene: SceneProfile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = scene.category.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scene.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "置信度 ${(scene.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = HasselbladOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scene.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// P1-10：当前粗粒度模式下的细分子模式面板
@Composable
private fun SubSceneModesPanel(
    selectedSceneModeId: String,
    currentSceneId: String,
    onSceneSelected: (SceneProfile) -> Unit
) {
    val subScenes = getSubSceneProfiles(selectedSceneModeId)
    if (subScenes.isEmpty()) return

    // 优先展示 2-4 个细分子模式，当前识别到的排最前
    val sorted = subScenes.sortedByDescending { it.id == currentSceneId }
    val displayScenes = sorted.take(4)

    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "细分子模式",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(displayScenes) { scene ->
                    val isCurrent = scene.id == currentSceneId
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onSceneSelected(scene) },
                        label = {
                            Column {
                                Text(
                                    text = scene.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = scene.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                            selected = isCurrent
                        )
                    )
                }
            }
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
    val haptic = LocalHapticFeedback.current
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
                            detectDragGestures(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ) { _, dragAmount ->
                                splitFraction = (splitFraction + dragAmount.x / maxW).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    // 底层：处理后图（右侧）
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (processed != null) {
                            Image(
                                bitmap = processed.asImageBitmap(),
                                contentDescription = "哈苏处理",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 上层裁剪：原图（左侧）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(splitFraction)
                            .fillMaxHeight()
                    ) {
                        if (original != null) {
                            Image(
                                bitmap = original.asImageBitmap(),
                                contentDescription = "原图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .offset(x = with(LocalDensity.current) { (maxW * splitFraction - 1.5f).toDp() })
                            .background(HasselbladOrange)
                    )

                    // 拖动圆点 + 百分比
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { (maxW * splitFraction - 16).toDp() },
                                y = with(LocalDensity.current) { (maxH / 2 - 16).toDp() }
                            )
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(splitFraction * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 标签：左侧原图，右侧哈苏
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "原图 ORIGINAL", color = Color.White, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HasselbladOrange.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "哈苏 HASSELBLAD", color = Color.White, fontSize = 11.sp)
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
    onViewImage: () -> Unit,
    onEditAnother: () -> Unit,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewImage,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "查看图片", color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onEditAnother,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "再调一张", color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetake,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "重新拍摄", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
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
    maxDimension: Int = 2048
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
