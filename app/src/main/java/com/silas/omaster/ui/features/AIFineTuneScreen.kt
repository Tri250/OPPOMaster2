package com.silas.omaster.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.ui.theme.CyanAccent
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/**
 * AI 微调功能页面
 * 修复后：统一使用 AIFineTuneViewModel 管理状态，接入 GPU 实时预览，
 * HSL/曲线/智能优化真实生效，导出与预览一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIFineTuneScreen(
    bitmap: Bitmap? = null,
    onBack: () -> Unit,
    onApply: (RenderParameters) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aiManager = remember { AIFineTuneManager.getInstance(context) }
    val viewModel: AIFineTuneViewModel = viewModel(
        factory = AIFineTuneViewModelFactory(aiManager)
    )

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 状态收集
    val activeTab by viewModel.activeTab.collectAsState()
    val currentParams by viewModel.currentParams.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val sourceBitmap by viewModel.sourceBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val isLoadingImage by viewModel.isLoadingImage.collectAsState()
    val imageLoadError by viewModel.imageLoadError.collectAsState()
    val lockedParams by viewModel.lockedParams.collectAsState()
    val showCompare by viewModel.showCompare.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val inferenceStage by viewModel.inferenceStage.collectAsState()
    val inferenceProgress by viewModel.inferenceProgress.collectAsState()
    val inferenceMessage by viewModel.inferenceMessage.collectAsState()
    val showSuccess by viewModel.showSuccess.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val selectedStyleId by viewModel.selectedStyleId.collectAsState()
    val selectedOptimizations by viewModel.selectedOptimizations.collectAsState()
    val hslValues by viewModel.hslValues.collectAsState()
    val selectedHslId by viewModel.selectedHslId.collectAsState()
    val curveChannel by viewModel.curveChannel.collectAsState()
            val curvePoints by viewModel.curvePoints.collectAsState()
            val canUndo by viewModel.canUndo.collectAsState()
            val canRedo by viewModel.canRedo.collectAsState()
            val localAdjustments by viewModel.localAdjustments.collectAsState()
            val selectedLocalAdjId by viewModel.selectedLocalAdjId.collectAsState()
            val showMaskOverlay by viewModel.showMaskOverlay.collectAsState()
            val historySnapshots by viewModel.historySnapshots.collectAsState()

    // 初始化传入的 bitmap
    LaunchedEffect(bitmap) {
        bitmap?.let { viewModel.setBitmap(context, it) }
    }

    // 参数变化时刷新 GPU 预览
    LaunchedEffect(currentParams) {
        if (sourceBitmap != null) {
            viewModel.refreshPreview(context)
        }
    }

    // 屏幕销毁时取消 AI 推理协程，防止闪退
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelInference()
        }
    }

    // 图片选择器：使用 PickVisualMedia 符合 Android 16 隐私最佳实践
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadImage(context, it) }
    }

    // 相机拍照
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { viewModel.loadImage(context, it) }
        }
    }

    // 权限：Android 13+ PickVisualMedia 无需 READ_MEDIA_IMAGES；低版本仍需 READ_EXTERNAL_STORAGE
    // Android 14+ (API 34+) 还需要 READ_MEDIA_VISUAL_USER_SELECTED 实现部分图片访问
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Android 14+ 多权限请求（READ_MEDIA_IMAGES + READ_MEDIA_VISUAL_USER_SELECTED）
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val imagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        val visualSelectedGranted = permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        if (imagesGranted || visualSelectedGranted) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    fun launchGalleryPicker() {
        // Android 14+ (API 34+) 使用 Photo Picker 不需要存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else if (ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // Android 14+ 同时请求 READ_MEDIA_IMAGES 和 READ_MEDIA_VISUAL_USER_SELECTED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                multiPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                )
            } else {
                permissionLauncher.launch(storagePermission)
            }
        }
    }

    // Toast 提示
    LaunchedEffect(imageLoadError) {
        imageLoadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            Toast.makeText(context, "AI 微调完成", Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 微调",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(Icons.Default.Undo, contentDescription = "撤销", tint = if (canUndo) HasselbladOrange else Color.Gray)
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "重做", tint = if (canRedo) HasselbladOrange else Color.Gray)
                    }
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重置", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomActionBar(
                onGallery = { launchGalleryPicker() },
                onCamera = {
                    val file = File(context.cacheDir, "ai_tune_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    tempPhotoUri = uri
                    cameraLauncher.launch(uri)
                },
                onAI = { viewModel.performAIInference(sourceBitmap) },
                onExport = {
                    scope.launch {
                        val success = viewModel.exportImage(context)
                        Toast.makeText(context, if (success) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
                    }
                },
                onApply = { onApply(viewModel.getFinalParams()) },
                hasImage = sourceBitmap != null,
                isProcessing = isProcessing
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (sourceBitmap != null) {
                    ImagePreview(
                        sourceBitmap = sourceBitmap,
                        previewBitmap = previewBitmap,
                        showCompare = showCompare,
                        onCompareToggle = { viewModel.toggleCompare() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyImagePlaceholder(
                        onGallery = { launchGalleryPicker() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isProcessing || inferenceStage == InferenceStage.ERROR) {
                    AIProgressOverlay(
                        stage = inferenceStage,
                        progress = inferenceProgress,
                        message = inferenceMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Tab 切换
            val tabs = listOf(
                "basic" to "基础",
                "style" to "风格",
                "smart" to "智能",
                "hsl" to "HSL",
                "curve" to "曲线",
                "local" to "局部",
                "lens" to "镜头",
                "scope" to "示波",
                "history" to "历史"
            )
            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = HasselbladOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { (id, label) ->
                    Tab(
                        selected = activeTab == id,
                        onClick = { viewModel.setTab(id) },
                        text = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            // 参数面板
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (activeTab) {
                    "basic" -> BasicParamsPanel(
                        params = currentParams,
                        locked = lockedParams,
                        onParamChange = { key, value ->
                            viewModel.updateParam(key, value)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onLockToggle = { viewModel.toggleParamLock(it) }
                    )
                    "style" -> StylePanel(
                        styles = COLOR_STYLES,
                        selectedId = selectedStyleId,
                        onSelect = { viewModel.selectStyle(it) }
                    )
                    "smart" -> SmartPanel(
                        optimizations = SMART_OPTIMIZATIONS,
                        selected = selectedOptimizations,
                        onToggle = { viewModel.toggleOptimization(it) }
                    )
                    "hsl" -> HSLPanel(
                        hslValues = hslValues,
                        selectedId = selectedHslId,
                        onSelect = { viewModel.selectHsl(it) },
                        onValueChange = { id, type, value ->
                            viewModel.updateHSL(id, type, value)
                        }
                    )
                    "curve" -> CurvePanel(
                        channel = curveChannel,
                        points = curvePoints[curveChannel] ?: emptyList(),
                        onChannelChange = { viewModel.setCurveChannel(it) },
                        onPointsChange = { viewModel.updateCurvePoints(curveChannel, it) },
                        onPreset = { viewModel.applyCurvePreset(it) }
                    )
                    "local" -> LocalAdjustPanel(
                        adjustments = localAdjustments,
                        selectedId = selectedLocalAdjId,
                        onSelect = { viewModel.selectLocalAdjustment(it) },
                        onAdd = { type ->
                            val adj = com.silas.omaster.renderer.LocalAdjustment(
                                maskType = type,
                                name = when (type) {
                                    com.silas.omaster.renderer.MaskType.BRUSH -> "画笔蒙版"
                                    com.silas.omaster.renderer.MaskType.RADIAL -> "径向渐变"
                                    com.silas.omaster.renderer.MaskType.LINEAR -> "线性渐变"
                                }
                            )
                            viewModel.addLocalAdjustment(adj)
                        },
                        onUpdate = { id, block -> viewModel.updateLocalAdjustment(id, block) },
                        onRemove = { viewModel.removeLocalAdjustment(it) },
                        onToggleMaskOverlay = { viewModel.toggleMaskOverlay() },
                        showMaskOverlay = showMaskOverlay,
                        onColorRange = { viewModel.applyColorRangeAsLocalAdjustment() },
                        onAISubject = { viewModel.detectSubject() },
                        isDetectingSubject = viewModel.isDetectingSubject.collectAsState().value
                    )
                    "lens" -> LensCorrectionPanel(
                        lensProfiles = com.silas.omaster.engine.LensCorrectionEngine().lensProfiles,
                        currentParams = viewModel.lensCorrectionParams.collectAsState().value,
                        onUpdateParams = { viewModel.updateLensCorrection(it) },
                        onApply = { viewModel.applyLensCorrection() },
                        onAutoCorrect = { viewModel.applyLensAutoCorrect() }
                    )
                    "scope" -> ScopePanel(
                        scopeType = viewModel.scopeType.collectAsState().value,
                        onScopeTypeChange = { viewModel.setScopeType(it) },
                        onToggle = { viewModel.toggleScope() },
                        isShowing = viewModel.showScope.collectAsState().value
                    )
                    "history" -> EditHistoryPanel(
                        snapshots = historySnapshots,
                        currentIndex = viewModel.currentHistoryIndex.collectAsState().value,
                        onJump = { viewModel.jumpToHistory(it) },
                        onCreateBranch = { viewModel.createBranch(it) }
                    )
                }
            }
        }
    }
}

// ==================== 预览区域 ====================

@Composable
private fun ImagePreview(
    sourceBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    showCompare: Boolean,
    onCompareToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        // 原图（作为底层）
        sourceBitmap?.let { bitmap ->
            AsyncImage(
                model = bitmap,
                contentDescription = "原图",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 效果预览（覆盖在原图上）
        if (!showCompare && previewBitmap != null) {
            AsyncImage(
                model = previewBitmap,
                contentDescription = "AI 微调预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 对比按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (showCompare) HasselbladOrange else Color.Black.copy(alpha = 0.6f))
                .clickable { onCompareToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (showCompare) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "对比",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showCompare) {
            Text(
                text = "原图",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyImagePlaceholder(
    onGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onGallery() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("点击选择图片", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            Text("支持相册与拍照", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

// ==================== 基础参数面板 ====================

@Composable
private fun BasicParamsPanel(
    params: RenderParameters,
    locked: Set<String>,
    onParamChange: (String, Float) -> Unit,
    onLockToggle: (String) -> Unit
) {
    val basicParams = listOf(
        Triple("exposure", "曝光", -100f..100f),
        Triple("brightness", "亮度", -100f..100f),
        Triple("contrast", "对比度", -100f..100f),
        Triple("saturation", "饱和度", -100f..100f),
        Triple("vibrance", "鲜艳度", -100f..100f),
        Triple("warmth", "色温", -100f..100f),
        Triple("tint", "色调", -100f..100f),
        Triple("highlights", "高光", -100f..100f),
        Triple("shadows", "阴影", -100f..100f),
        Triple("whites", "白色", -100f..100f),
        Triple("blacks", "黑色", -100f..100f),
        Triple("texture", "纹理", -100f..100f),
        Triple("clarity", "清晰度", 0f..100f),
        Triple("sharpness", "锐度", 0f..100f),
        Triple("dehaze", "去霾", 0f..100f),
        Triple("denoise", "降噪", 0f..100f),
        Triple("grain", "颗粒", 0f..100f),
        Triple("fade", "褪色", 0f..100f),
        Triple("skinSmooth", "肤色平滑", 0f..100f),
        Triple("vignette", "暗角", 0f..100f)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(basicParams) { (key, name, range) ->
            val value = when (key) {
                "exposure" -> params.exposure
                "brightness" -> params.brightness
                "contrast" -> params.contrast
                "saturation" -> params.saturation
                "vibrance" -> params.vibrance
                "warmth" -> params.warmth
                "highlights" -> params.highlights
                "shadows" -> params.shadows
                "whites" -> params.whites
                "blacks" -> params.blacks
                "texture" -> params.texture
                "clarity" -> params.clarity
                "sharpness" -> params.sharpness
                "dehaze" -> params.dehaze
                "denoise" -> params.denoise
                "grain" -> params.grain
                "fade" -> params.fade
                "skinSmooth" -> params.skinSmooth
                "tint" -> params.tint
                "vignette" -> params.vignette
                else -> 0f
            }
            ParamSliderRow(
                key = key,
                name = name,
                value = value,
                range = range,
                locked = locked.contains(key),
                onValueChange = onParamChange,
                onLockToggle = onLockToggle
            )
        }
    }
}

@Composable
private fun ParamSliderRow(
    key: String,
    name: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    locked: Boolean,
    onValueChange: (String, Float) -> Unit,
    onLockToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = value,
            onValueChange = { onValueChange(key, it) },
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = if (locked) Color.Gray else HasselbladOrange,
                thumbColor = if (locked) Color.Gray else HasselbladOrange,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            enabled = !locked
        )
        Text(
            text = "${value.roundToInt()}",
            color = if (locked) Color.Gray else MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
        IconButton(
            onClick = { onLockToggle(key) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) "已锁定" else "未锁定",
                tint = if (locked) HasselbladOrange else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==================== 风格面板 ====================

@Composable
private fun StylePanel(
    styles: List<ColorStylePreset>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(styles) { style ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedId == style.id) style.color.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (selectedId == style.id) 2.dp else 0.dp,
                        color = if (selectedId == style.id) style.color else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(style.id) }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(style.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(style.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
            }
        }
    }
}

// ==================== 智能优化面板 ====================

@Composable
private fun SmartPanel(
    optimizations: List<SmartOptimization>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(optimizations) { opt ->
            val isSelected = selected.contains(opt.id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) opt.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) opt.color else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggle(opt.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(opt.icon, contentDescription = null, tint = opt.color, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(opt.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        if (opt.isPro) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRO", color = HasselbladOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(opt.description, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle(opt.id) },
                    colors = CheckboxDefaults.colors(checkedColor = opt.color, uncheckedColor = Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}

// ==================== HSL 面板 ====================

@Composable
private fun HSLPanel(
    hslValues: List<HSLValue>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onValueChange: (String, String, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 色块选择
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(hslValues) { hsl ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(hsl.color)
                        .border(
                            width = if (selectedId == hsl.id) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hsl.id) }
                )
            }
        }

        val selected = hslValues.find { it.id == selectedId } ?: return
        Spacer(modifier = Modifier.height(8.dp))
        HSLSliderRow(label = "色相", value = selected.hue.toFloat(), range = -180f..180f, onValueChange = {
            onValueChange(selected.id, "hue", it.roundToInt())
        })
        HSLSliderRow(label = "饱和度", value = selected.saturation.toFloat(), range = -100f..100f, onValueChange = {
            onValueChange(selected.id, "saturation", it.roundToInt())
        })
        HSLSliderRow(label = "明度", value = selected.luminance.toFloat(), range = -100f..100f, onValueChange = {
            onValueChange(selected.id, "luminance", it.roundToInt())
        })
    }
}

@Composable
private fun HSLSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.width(56.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(activeTrackColor = HasselbladOrange, thumbColor = HasselbladOrange, inactiveTrackColor = Color.White.copy(alpha = 0.15f))
        )
        Text("${value.roundToInt()}", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

// ==================== 曲线面板 ====================

@Composable
private fun CurvePanel(
    channel: String,
    points: List<CurvePoint>,
    onChannelChange: (String) -> Unit,
    onPointsChange: (List<CurvePoint>) -> Unit,
    onPreset: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 通道选择
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("rgb" to "RGB", "red" to "红", "green" to "绿", "blue" to "蓝").forEach { (id, label) ->
                FilterChip(
                    selected = channel == id,
                    onClick = { onChannelChange(id) },
                    label = { Text(label, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HasselbladOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 曲线画布
        CurveEditor(
            points = points,
            channelColor = when (channel) {
                "red" -> Color.Red
                "green" -> Color.Green
                "blue" -> Color.Blue
                else -> Color.White
            },
            onPointsChange = onPointsChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 预设
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("linear" to "线性", "sCurve" to "S曲线", "highContrast" to "高对比", "soft" to "柔和", "invert" to "反相").forEach { (id, label) ->
                OutlinedButton(
                    onClick = { onPreset(id) },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(label, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CurveEditor(
    points: List<CurvePoint>,
    channelColor: Color,
    onPointsChange: (List<CurvePoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    var controlPoints by remember(points) { mutableStateOf(points) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val size = this.size
                            val x = (offset.x / size.width).coerceIn(0f, 1f)
                            val y = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            val index = findNearestPoint(controlPoints, x, y)
                            if (index != null) {
                                draggedIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDrag = { change, _ ->
                            val size = this.size
                            draggedIndex?.let { index ->
                                val x = (change.position.x / size.width).coerceIn(0f, 1f)
                                val y = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                val newPoints = controlPoints.toMutableList()
                                newPoints[index] = CurvePoint(x, y)
                                controlPoints = newPoints.sortedBy { it.x }
                                onPointsChange(controlPoints)
                            }
                            change.consume()
                        },
                        onDragEnd = { draggedIndex = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            // 背景网格
            val gridColor = Color.White.copy(alpha = 0.1f)
            for (i in 1..3) {
                val x = w * i / 4
                drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                val y = h * i / 4
                drawLine(gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            }
            drawLine(Color.White.copy(alpha = 0.3f), start = Offset(0f, h), end = Offset(w, 0f), strokeWidth = 1.5f)

            // 曲线
            if (controlPoints.size >= 2) {
                val path = Path().apply {
                    val first = controlPoints.first()
                    moveTo(first.x * w, h - first.y * h)
                    for (i in 0 until controlPoints.size - 1) {
                        val p0 = controlPoints[i]
                        val p1 = controlPoints[i + 1]
                        lineTo(p1.x * w, h - p1.y * h)
                    }
                }
                drawPath(path, channelColor, style = Stroke(width = 3f))
            }

            // 控制点
            controlPoints.forEach { point ->
                val cx = point.x * w
                val cy = h - point.y * h
                drawCircle(Color.White, radius = 8f, center = Offset(cx, cy))
                drawCircle(channelColor, radius = 5f, center = Offset(cx, cy))
            }
        }
    }
}

private fun findNearestPoint(points: List<CurvePoint>, x: Float, y: Float): Int? {
    if (points.isEmpty()) return null
    var bestIndex: Int? = null
    var bestDist = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val dx = point.x - x
        val dy = point.y - y
        val dist = dx * dx + dy * dy
        if (dist < bestDist && dist < 0.02f) {
            bestDist = dist
            bestIndex = index
        }
    }
    return bestIndex
}

// ==================== 底部操作栏 ====================

@Composable
private fun BottomActionBar(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onAI: () -> Unit,
    onExport: () -> Unit,
    onApply: () -> Unit,
    hasImage: Boolean,
    isProcessing: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(onClick = onGallery, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "相册", tint = MaterialTheme.colorScheme.onBackground)
                }
                OutlinedIconButton(onClick = onCamera, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "拍照", tint = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = onAI,
                    enabled = hasImage && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI 微调", fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = onExport,
                    enabled = hasImage && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导出", fontSize = 14.sp)
                }

                Button(
                    onClick = onApply,
                    enabled = hasImage && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("应用", fontSize = 14.sp)
                }
            }
        }
    }
}

// ==================== AI 进度浮层 ====================

@Composable
private fun AIProgressOverlay(
    stage: InferenceStage,
    progress: Float,
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = stage == InferenceStage.ANALYZING || stage == InferenceStage.DETECTING_SUBJECT ||
                stage == InferenceStage.COMPUTING_PARAMS || stage == InferenceStage.APPLYING_AI ||
                stage == InferenceStage.COMPLETED || stage == InferenceStage.ERROR,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (stage == InferenceStage.COMPLETED) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                    } else if (stage == InferenceStage.ERROR) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(48.dp))
                    } else {
                        CircularProgressIndicator(color = HasselbladOrange, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(message, color = Color.White, fontSize = 14.sp)
                    if (stage != InferenceStage.COMPLETED && stage != InferenceStage.ERROR) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(0.7f),
                            color = HasselbladOrange,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 局部调整面板 ====================

@Composable
private fun LocalAdjustPanel(
    adjustments: List<com.silas.omaster.renderer.LocalAdjustment>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onAdd: (com.silas.omaster.renderer.MaskType) -> Unit,
    onUpdate: (String, (com.silas.omaster.renderer.LocalAdjustment) -> com.silas.omaster.renderer.LocalAdjustment) -> Unit,
    onRemove: (String) -> Unit,
    onToggleMaskOverlay: () -> Unit,
    showMaskOverlay: Boolean,
    onColorRange: () -> Unit = {},
    onAISubject: () -> Unit = {},
    isDetectingSubject: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 工具栏：添加蒙版按钮 + 蒙版叠加开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaskToolButton("画笔", Icons.Default.Edit) { onAdd(com.silas.omaster.renderer.MaskType.BRUSH) }
            MaskToolButton("径向", Icons.Default.RadioButtonUnchecked) { onAdd(com.silas.omaster.renderer.MaskType.RADIAL) }
            MaskToolButton("线性", Icons.Default.LinearScale) { onAdd(com.silas.omaster.renderer.MaskType.LINEAR) }
            MaskToolButton("色彩", Icons.Default.Palette) { onColorRange() }
            MaskToolButton("AI主体", Icons.Default.Person2) { onAISubject() }
            IconButton(onClick = onToggleMaskOverlay) {
                Icon(
                    imageVector = if (showMaskOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "蒙版叠加",
                    tint = if (showMaskOverlay) HasselbladOrange else Color.Gray
                )
            }
        }

        if (isDetectingSubject) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = HasselbladOrange,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }

        if (adjustments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("点击上方工具添加局部调整", color = Color.Gray, fontSize = 14.sp)
            }
            return
        }

        // 已添加的局部调整列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(adjustments) { adj ->
                val isSelected = adj.id == selectedId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onSelect(if (isSelected) null else adj.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else Color(0xFF1A1A1A)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(adj.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { onRemove(adj.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LocalParamSliders(adj) { updated ->
                                onUpdate(adj.id, updated)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaskToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = HasselbladOrange)
        }
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun LocalParamSliders(
    adj: com.silas.omaster.renderer.LocalAdjustment,
    onChange: (com.silas.omaster.renderer.LocalAdjustment) -> Unit
) {
    val params = listOf(
        "exposure" to "曝光" to adj.exposure,
        "brightness" to "亮度" to adj.brightness,
        "contrast" to "对比度" to adj.contrast,
        "saturation" to "饱和度" to adj.saturation,
        "warmth" to "色温" to adj.warmth,
        "tint" to "色调" to adj.tint,
        "highlights" to "高光" to adj.highlights,
        "shadows" to "阴影" to adj.shadows,
        "clarity" to "清晰度" to adj.clarity,
        "sharpness" to "锐度" to adj.sharpness
    )

    Column {
        params.forEach { (pair, value) ->
            val (key, name) = pair
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(48.dp))
                Slider(
                    value = value,
                    onValueChange = { newVal ->
                        val updated = when (key) {
                            "exposure" -> adj.copy(exposure = newVal)
                            "brightness" -> adj.copy(brightness = newVal)
                            "contrast" -> adj.copy(contrast = newVal)
                            "saturation" -> adj.copy(saturation = newVal)
                            "warmth" -> adj.copy(warmth = newVal)
                            "tint" -> adj.copy(tint = newVal)
                            "highlights" -> adj.copy(highlights = newVal)
                            "shadows" -> adj.copy(shadows = newVal)
                            "clarity" -> adj.copy(clarity = newVal)
                            "sharpness" -> adj.copy(sharpness = newVal)
                            else -> adj
                        }
                        onChange(updated)
                    },
                    valueRange = -100f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = HasselbladOrange,
                        activeTrackColor = HasselbladOrange,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

// ==================== 通用组件 ====================

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = HasselbladOrange,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

// ==================== 镜头校正面板 ====================

@Composable
private fun LensCorrectionPanel(
    lensProfiles: List<com.silas.omaster.engine.LensCorrectionEngine.LensProfile>,
    currentParams: com.silas.omaster.engine.LensCorrectionEngine.CorrectionParams,
    onUpdateParams: (com.silas.omaster.engine.LensCorrectionEngine.CorrectionParams) -> Unit,
    onApply: () -> Unit,
    onAutoCorrect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 一键自动校正
        Button(
            onClick = onAutoCorrect,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("自动镜头校正")
        }

        // 镜头预设
        SectionLabel("镜头预设")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lensProfiles) { profile ->
                Button(
                    onClick = {
                        val engine = com.silas.omaster.engine.LensCorrectionEngine()
                        onUpdateParams(engine.paramsFromProfile(profile))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Text("${profile.manufacturer} ${profile.focalLength}", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // 色差校正
        SectionLabel("色差校正（Chromatic Aberration）")
        LensSlider("R水平偏移", currentParams.caRedOffset, -5f..5f) {
            onUpdateParams(currentParams.copy(caRedOffset = it))
        }
        LensSlider("R垂直偏移", currentParams.caRedOffsetV, -5f..5f) {
            onUpdateParams(currentParams.copy(caRedOffsetV = it))
        }
        LensSlider("B水平偏移", currentParams.caBlueOffset, -5f..5f) {
            onUpdateParams(currentParams.copy(caBlueOffset = it))
        }
        LensSlider("B垂直偏移", currentParams.caBlueOffsetV, -5f..5f) {
            onUpdateParams(currentParams.copy(caBlueOffsetV = it))
        }

        // 暗角校正
        SectionLabel("暗角校正")
        LensSlider("校正量", currentParams.vignetteAmount, 0f..100f) {
            onUpdateParams(currentParams.copy(vignetteAmount = it))
        }
        LensSlider("半径", currentParams.vignetteRadius, 0f..100f) {
            onUpdateParams(currentParams.copy(vignetteRadius = it))
        }

        // 畸变校正
        SectionLabel("畸变校正")
        LensSlider("畸变系数", currentParams.distortion, -0.1f..0.1f) {
            onUpdateParams(currentParams.copy(distortion = it))
        }

        // 应用按钮
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("应用镜头校正")
        }
    }
}

@Composable
private fun LensSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
        )
        Text(String.format("%.2f", value), color = HasselbladOrange, fontSize = 11.sp, modifier = Modifier.width(48.dp))
    }
}

// ==================== 示波器面板 ====================

@Composable
private fun ScopePanel(
    scopeType: String,
    onScopeTypeChange: (String) -> Unit,
    onToggle: () -> Unit,
    isShowing: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 示波器开关
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("实时示波器", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = isShowing,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = HasselbladOrange)
            )
        }

        // 示波器类型切换
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("waveform" to "波形", "vectorscope" to "矢量", "parade" to "RGB").forEach { (id, label) ->
                Button(
                    onClick = { onScopeTypeChange(id) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scopeType == id) HasselbladOrange else Color(0xFF2A2A2A)
                    )
                ) { Text(label, fontSize = 12.sp) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 示波器显示区域
        if (isShowing) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp).height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (scopeType) {
                        "waveform" -> WaveformScopeView()
                        "vectorscope" -> VectorscopeScopeView()
                        "parade" -> ParadeScopeView()
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("开启示波器查看图片色彩分布", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun WaveformScopeView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // 绘制波形网格
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(Color.White.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(w, y))
        }
        // 标注亮度级
        drawLine(Color.Green.copy(alpha = 0.1f), androidx.compose.ui.geometry.Offset(0f, h * 0.75f), androidx.compose.ui.geometry.Offset(w, h * 0.75f))
        // 示波器标签
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 20f }
            drawText("100%", 8f, 30f, paint)
            drawText("50%", 8f, h.toInt() / 2 + 6, paint)
            drawText("0%", 8f, h.toInt() - 8, paint)
        }
    }
}

@Composable
private fun VectorscopeScopeView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = min(cx, cy) * 0.85f
        // 绘制矢量图圆环和肤色线
        for (i in 1..3) {
            drawCircle(Color.White.copy(alpha = 0.1f), radius = r * i / 3f, center = androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke())
        }
        // 肤色线（I线，约33°）
        val skinAngle = Math.toRadians(33.0)
        drawLine(
            Color.Yellow.copy(alpha = 0.3f),
            androidx.compose.ui.geometry.Offset(cx, cy),
            androidx.compose.ui.geometry.Offset(cx + r * kotlin.math.cos(skinAngle).toFloat(), cy - r * kotlin.math.sin(skinAngle).toFloat())
        )
        // RGB 标注
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 18f }
            drawText("R", cx + r + 8, cy + 6, paint)
            drawText("B", cx - 8, cy - r - 8, paint)
        }
    }
}

@Composable
private fun ParadeScopeView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sectionW = w / 3f
        val labels = listOf("R", "G", "B")
        val colors = listOf(Color.Red.copy(alpha = 0.6f), Color.Green.copy(alpha = 0.6f), Color.Blue.copy(alpha = 0.6f))

        labels.forEachIndexed { i, label ->
            val offsetX = sectionW * i
            // 分隔线
            if (i > 0) {
                drawLine(Color.White.copy(alpha = 0.2f), androidx.compose.ui.geometry.Offset(offsetX, 0f), androidx.compose.ui.geometry.Offset(offsetX, h))
            }
            // 网格
            for (j in 0..4) {
                val y = h * j / 4f
                drawLine(Color.White.copy(alpha = 0.1f), androidx.compose.ui.geometry.Offset(offsetX, y), androidx.compose.ui.geometry.Offset(offsetX + sectionW, y))
            }
            // 标注
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply { this.color = colors[i].toArgb(); textSize = 22f; isFakeBoldText = true }
                drawText(label, offsetX + sectionW / 2 - 6, 30f, paint)
            }
        }
    }
}

// ==================== 编辑历史面板 ====================

@Composable
private fun EditHistoryPanel(
    snapshots: List<com.silas.omaster.manager.EditSnapshot>,
    currentIndex: Int,
    onJump: (String) -> Unit,
    onCreateBranch: (String) -> Unit
) {
    var showBranchDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("编辑步骤 (${snapshots.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = { showBranchDialog = true }) {
                Text("创建分支", color = HasselbladOrange, fontSize = 12.sp)
            }
        }

        if (snapshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无编辑历史", color = Color.Gray, fontSize = 14.sp)
            }
            return
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(snapshots) { index, snapshot ->
                val isCurrent = index == currentIndex
                val timeText = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(snapshot.timestamp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) HasselbladOrange.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onJump(snapshot.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}.",
                        color = if (isCurrent) HasselbladOrange else Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(snapshot.name, color = Color.White, fontSize = 13.sp)
                        Text(timeText, color = Color.Gray, fontSize = 10.sp)
                    }
                    if (isCurrent) {
                        Text("当前", color = HasselbladOrange, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text("创建分支", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("分支名称", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (branchName.isNotBlank()) {
                        onCreateBranch(branchName)
                        showBranchDialog = false
                    }
                }) {
                    Text("确定", color = HasselbladOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBranchDialog = false }) {
                    Text("取消", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}
