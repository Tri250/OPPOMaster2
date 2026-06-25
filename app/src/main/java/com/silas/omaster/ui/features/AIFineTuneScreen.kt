package com.silas.omaster.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.theme.ColorOS16Palette
import com.silas.omaster.ui.theme.CyanAccent
import com.silas.omaster.ui.theme.DarkGray
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

    // 图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
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

    // 权限
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch("image/*")
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
                onGallery = {
                    if (ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) {
                        galleryLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(storagePermission)
                    }
                },
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
                        onGallery = {
                            if (ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) {
                                galleryLauncher.launch("image/*")
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
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
            TabRow(
                selectedTabIndex = listOf("basic", "style", "smart", "hsl", "curve").indexOf(activeTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = HasselbladOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "basic" to "基础",
                    "style" to "风格",
                    "smart" to "智能",
                    "hsl" to "HSL",
                    "curve" to "曲线"
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
            .background(DarkGray)
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
            .background(DarkGray)
            .clickable { onGallery() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "添加图片",
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
        Triple("denoise", "降噪", 0f..100f),
        Triple("grain", "颗粒", 0f..100f),
        Triple("fade", "褪色", 0f..100f),
        Triple("skinSmooth", "肤色平滑", 0f..100f)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "基础参数列表" },
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "风格选择" },
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
                    Icon(style.icon, contentDescription = style.name, tint = style.color, modifier = Modifier.size(24.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "智能优化选项" },
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
                Icon(opt.icon, contentDescription = opt.name, tint = opt.color, modifier = Modifier.size(28.dp))
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
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "HSL色块选择" }
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
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI微调", modifier = Modifier.size(18.dp))
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
                    Icon(Icons.Default.Save, contentDescription = "保存", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导出", fontSize = 14.sp)
                }

                Button(
                    onClick = onApply,
                    enabled = hasImage && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "应用", modifier = Modifier.size(18.dp))
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
        enter = fadeIn(animationSpec = AnimationSpecs.ColorOS16ContentEnter),
        exit = fadeOut(animationSpec = AnimationSpecs.ColorOS16ContentExit)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (stage == InferenceStage.COMPLETED) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "完成", tint = SuccessGreen, modifier = Modifier.size(48.dp))
                    } else if (stage == InferenceStage.ERROR) {
                        Icon(Icons.Default.Error, contentDescription = "错误", tint = WarningYellow, modifier = Modifier.size(48.dp))
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
