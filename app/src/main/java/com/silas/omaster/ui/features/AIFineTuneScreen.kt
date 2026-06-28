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
import com.silas.omaster.ai.AIMaskManager
import com.silas.omaster.data.local.EditRecipeClipboard
import com.silas.omaster.data.local.NonDestructiveRecipeManager
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
    val recipeManager = remember { NonDestructiveRecipeManager.getInstance(context) }
    val viewModel: AIFineTuneViewModel = viewModel(
        factory = AIFineTuneViewModelFactory(aiManager, recipeManager)
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

    // P0-2: AI 局部遮罩状态
    val isGeneratingMask by viewModel.isGeneratingMask.collectAsState()
    val activeMaskType by viewModel.activeMaskType.collectAsState()
    val maskOpacity by viewModel.maskOpacity.collectAsState()

    // P0-1: 非破坏性配方恢复状态
    val hasPendingRecipe by viewModel.hasPendingRecipe.collectAsState()
    val pendingRecipeLabel by viewModel.pendingRecipeLabel.collectAsState()
    val recipeSaved by viewModel.recipeSaved.collectAsState()

    // P0-4: 编辑历史时间轴
    val showHistoryTimeline by viewModel.showHistoryTimeline.collectAsState()
    val historyNodes = remember { viewModel.getHistoryNodes() }

    // P1-2: Filmstrip 最近图片
    val recentImageUris by viewModel.recentImageUris.collectAsState()

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
    LaunchedEffect(recipeSaved) {
        if (recipeSaved) {
            Toast.makeText(context, "配方已保存", Toast.LENGTH_SHORT).show()
        }
    }

    // P0-1: 配方恢复对话框
    if (hasPendingRecipe) {
        RecipeRestoreDialog(
            recipeLabel = pendingRecipeLabel ?: "上次编辑",
            onRestore = {
                scope.launch {
                    val hash = recipeManager.activeRecipes.value.keys.firstOrNull() ?: return@launch
                    val recipe = recipeManager.loadLatestRecipe(hash)
                    recipe?.let { viewModel.applyRecipe(context, it) }
                }
            },
            onDismiss = { viewModel.dismissPendingRecipe() }
        )
    }

    // P0-4: 编辑历史时间轴 BottomSheet
    if (showHistoryTimeline) {
        HistoryTimelineSheet(
            nodes = historyNodes,
            currentIndex = historyNodes.size - 1,
            onNodeClick = { node -> viewModel.jumpToHistoryNode(context, node.index) },
            onDismiss = { viewModel.dismissHistoryTimeline() }
        )
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
                    // P0-1: 保存配方按钮
                    IconButton(onClick = { viewModel.saveCurrentRecipe("AI微调") }) {
                        Icon(Icons.Default.Save, contentDescription = "保存配方", tint = HasselbladOrange)
                    }
                    // P1-1: 复制设置按钮
                    IconButton(onClick = { viewModel.copySettings() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制设置", tint = HasselbladOrange)
                    }
                    // P1-1: 粘贴设置按钮（仅当剪贴板有内容时可用）
                    val hasClipboard by EditRecipeClipboard.hasClipboard.collectAsState()
                    IconButton(
                        onClick = { viewModel.pasteSettings(context) },
                        enabled = hasClipboard
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "粘贴设置",
                            tint = if (hasClipboard) HasselbladOrange else Color.Gray
                        )
                    }
                    // P0-4: 历史时间轴按钮
                    IconButton(onClick = { viewModel.toggleHistoryTimeline() }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录", tint = HasselbladOrange)
                    }
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
                        // P0-1: 使用非破坏性渲染导出
                        viewModel.exportRenderedImage(context)
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

                // P1-2: Filmstrip 底部快切栏
                if (recentImageUris.isNotEmpty()) {
                    FilmstripBar(
                        uris = recentImageUris,
                        selectedUri = selectedImageUri,
                        onSelect = { uri -> viewModel.loadImage(context, uri) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Tab 切换
            TabRow(
                selectedTabIndex = listOf("basic", "style", "smart", "hsl", "curve", "local").indexOf(activeTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = HasselbladOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "basic" to "基础",
                    "style" to "风格",
                    "smart" to "智能",
                    "hsl" to "HSL",
                    "curve" to "曲线",
                    "local" to "局部"
                ).forEach { (id, label) ->
                    Tab(
                        selected = activeTab == id,
                        onClick = { viewModel.setTab(id) },
                        text = { Text(label, fontSize = 13.sp) }
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
                    "local" -> LocalMaskPanel(
                        isGeneratingMask = isGeneratingMask,
                        activeMaskType = activeMaskType,
                        maskOpacity = maskOpacity,
                        onGenerateMask = { type -> viewModel.generateMask(context, type) },
                        onClearMask = { viewModel.clearMask(context) },
                        onOpacityChange = { viewModel.updateMaskOpacity(context, it) }
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
        Triple("highlights", "高光", -100f..100f),
        Triple("shadows", "阴影", -100f..100f),
        Triple("whites", "白色", -100f..100f),
        Triple("blacks", "黑色", -100f..100f),
        Triple("texture", "纹理", -100f..100f),
        Triple("clarity", "清晰度", 0f..100f),
        Triple("sharpness", "锐度", 0f..100f),
        Triple("dehaze", "去霾", 0f..100f),
        Triple("vignette", "暗角", 0f..100f),
        Triple("denoise", "降噪", 0f..100f),
        Triple("grain", "颗粒", 0f..100f),
        Triple("fade", "褪色", 0f..100f),
        Triple("skinSmooth", "肤色平滑", 0f..100f)
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
                "vignette" -> params.vignette
                "denoise" -> params.denoise
                "grain" -> params.grain
                "fade" -> params.fade
                "skinSmooth" -> params.skinSmooth
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

// ==================== P0-2: AI 局部遮罩面板 ====================

/**
 * 局部遮罩面板 — 纯端侧 AI 分割，实现人像/背景/天空的局部调整
 *
 * 产品经理交互审查：
 * - 用户心智："我只想提亮人脸""我只想调色天空"，不应需要手动抠图
 * - 操作链路：点击「局部」Tab → 选择遮罩类型 → 系统自动分割 → 参数调整仅影响选中区域
 * - 反馈设计：分割中显示进度指示，分割完成后预览实时更新，遮罩强度可滑动调节
 */
@Composable
private fun LocalMaskPanel(
    isGeneratingMask: Boolean,
    activeMaskType: AIMaskManager.MaskType?,
    maskOpacity: Float,
    onGenerateMask: (AIMaskManager.MaskType) -> Unit,
    onClearMask: () -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "AI 局部调整",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "选择要调整的区域，参数将仅影响选中范围",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // 遮罩类型选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaskTypeButton(
                label = "人像",
                icon = Icons.Default.Face,
                isSelected = activeMaskType == AIMaskManager.MaskType.SUBJECT,
                isLoading = isGeneratingMask && activeMaskType == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGenerateMask(AIMaskManager.MaskType.SUBJECT)
                },
                modifier = Modifier.weight(1f)
            )
            MaskTypeButton(
                label = "背景",
                icon = Icons.Default.Landscape,
                isSelected = activeMaskType == AIMaskManager.MaskType.BACKGROUND,
                isLoading = isGeneratingMask && activeMaskType == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGenerateMask(AIMaskManager.MaskType.BACKGROUND)
                },
                modifier = Modifier.weight(1f)
            )
            MaskTypeButton(
                label = "天空",
                icon = Icons.Default.WbCloudy,
                isSelected = activeMaskType == AIMaskManager.MaskType.SKY,
                isLoading = isGeneratingMask && activeMaskType == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGenerateMask(AIMaskManager.MaskType.SKY)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 分割中指示
        if (isGeneratingMask) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HasselbladOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 分割中...", style = MaterialTheme.typography.bodySmall, color = HasselbladOrange)
            }
        }

        // 当前遮罩状态与清除
        if (activeMaskType != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (activeMaskType) {
                                AIMaskManager.MaskType.SUBJECT -> Icons.Default.Face
                                AIMaskManager.MaskType.BACKGROUND -> Icons.Default.Landscape
                                AIMaskManager.MaskType.SKY -> Icons.Default.WbCloudy
                            },
                            contentDescription = null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (activeMaskType) {
                                AIMaskManager.MaskType.SUBJECT -> "人像区域已选中"
                                AIMaskManager.MaskType.BACKGROUND -> "背景区域已选中"
                                AIMaskManager.MaskType.SKY -> "天空区域已选中"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = HasselbladOrange
                        )
                    }
                    TextButton(onClick = onClearMask) {
                        Text("清除", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 遮罩强度滑动条
            Text("遮罩强度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Slider(
                value = maskOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )
        }
    }
}

@Composable
private fun MaskTypeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HasselbladOrange) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = HasselbladOrange)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// ==================== P0-4: 编辑历史时间轴 BottomSheet ====================

/**
 * 历史时间轴 BottomSheet — 可视化编辑历史，支持任意节点回退
 *
 * 产品经理交互审查：
 * - 用户心智：用户需要知道"我刚才做了什么"，并能一键回到某个状态
 * - 操作链路：点击顶部「历史」图标 → 底部弹出时间轴 → 点击任意节点立即回退 → 预览实时更新
 * - 视觉设计：当前状态高亮（哈苏橙），历史节点用时间线连接，操作标签清晰可读
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTimelineSheet(
    nodes: List<AIFineTuneViewModel.HistoryNode>,
    currentIndex: Int,
    onNodeClick: (AIFineTuneViewModel.HistoryNode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "编辑历史",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "点击任意节点可回退到该状态",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(nodes.size) { index ->
                    val node = nodes[index]
                    val isCurrent = index == currentIndex
                    val isLast = index == nodes.size - 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNodeClick(node) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 时间线节点
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isCurrent) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .border(
                                        width = if (isCurrent) 2.dp else 0.dp,
                                        color = if (isCurrent) HasselbladOrange else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            if (!isLast) {
                                Spacer(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 节点信息
                        Column {
                            Text(
                                node.actionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) HasselbladOrange else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "步骤 ${node.index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (isCurrent) {
                            Text(
                                "当前",
                                style = MaterialTheme.typography.labelSmall,
                                color = HasselbladOrange,
                                modifier = Modifier
                                    .background(HasselbladOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Text("关闭")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== P1-2: Filmstrip 底部快切栏 ====================

/**
 * Filmstrip 底部快切栏 — 连续修图工作流
 *
 * 产品经理交互审查：
 * - 用户心智：专业用户一次选多张图连续修图，不应反复跳出选图
 * - 操作链路：加载图片 → 底部出现缩略图栏 → 点击其他缩略图直接切换 → 保留当前参数可快速复用
 * - 视觉设计：半透明背景悬浮于预览区底部，当前选中图高亮（哈苏橙边框），高度 56dp 不遮挡主体
 */
@Composable
private fun FilmstripBar(
    uris: List<Uri>,
    selectedUri: Uri?,
    onSelect: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(uris.size) { index ->
                val uri = uris[index]
                val isSelected = uri == selectedUri
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                        .then(
                            if (isSelected) Modifier.border(2.dp, HasselbladOrange, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable { onSelect(uri) }
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "缩略图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
