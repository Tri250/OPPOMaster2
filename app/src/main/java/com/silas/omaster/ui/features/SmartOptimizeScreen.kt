package com.silas.omaster.ui.features

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.WarningYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 智能优化页面 — 基于 PixelFruit 架构重构
 *
 * 核心架构对齐 PixelFruit (gitee.com/ji_annn/PixelFruit):
 * - 14 参数调色体系（亮度/曝光/饱和度/对比度/高光/阴影/白场/RGB色调/锐化/降噪/面部美白）
 * - 处理管线顺序：颜色调整 → 降噪 → 锐化 → 面部美化 → LUT
 * - 滤镜预设 = 参数快照
 * - AI 调色 = VL 模型 → JSON 参数
 *
 * UI/UX: 哈苏橙主题 + OPPO Find 交互
 * - 底部 Tab 切换调色/细节/滤镜/AI 面板
 * - 大圆角卡片 + 渐变色强调
 * - 触觉反馈 + 按压缩放动画
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOptimizeScreen(
    onBack: () -> Unit,
    onApply: (OptimizeParams) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }
    val pixelFruitEngine = remember { PixelFruitEngine() }
    val histogramAnalyzer = remember { HistogramAnalyzer() }

    // 图片状态
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var optimizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 预览模式
    var previewMode by remember { mutableStateOf("before") }

    // AI 场景识别
    var analysisResult by remember { mutableStateOf<SceneProfile?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // PixelFruit 14参数体系
    var params by remember { mutableStateOf(PixelFruitParams()) }

    // 编辑Tab
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("调色", "细节", "裁剪", "滤镜", "AI", "镜头", "RAW")

    // 裁剪旋转状态
    var cropRotateEngine by remember { mutableStateOf(com.silas.omaster.engine.CropRotateEngine()) }
    var cropRect by remember { mutableStateOf<android.graphics.RectF?>(null) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var cropApplied by remember { mutableStateOf(false) }

    // 优化进度
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationProgress by remember { mutableFloatStateOf(0f) }
    var optimizationCurrentName by remember { mutableStateOf("") }
    // 直方图
    var histogram by remember { mutableStateOf<HistogramAnalyzer.HistogramResult?>(null) }
    var showHistogram by remember { mutableStateOf(true) }

    // 实时预览防抖
    var previewJob by remember { mutableStateOf<Job?>(null) }

    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val loadedBitmap = loadSampledBitmap(context, it, 2048)
                if (loadedBitmap != null) {
                    originalBitmap = loadedBitmap
                    optimizedBitmap = null
                    previewBitmap = null
                    previewMode = "before"
                    analysisResult = null
                    // 直方图分析
                    histogram = histogramAnalyzer.analyze(loadedBitmap)
                    // 自动AI场景识别
                    isAnalyzing = true
                    analyzeImage(
                        loadedBitmap,
                        inferenceEngine,
                        scope,
                        onResult = { result ->
                            analysisResult = result
                            isAnalyzing = false
                        },
                        onError = { isAnalyzing = false }
                    )
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(saveError) {
        saveError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            saveError = null
        }
    }

    // 执行优化工作流（对齐 PixelFruit applyAdjustmentsToCachedData 顺序）
    fun runOptimizeWorkflow() {
        if (params.changedParamCount() == 0) return
        val source = originalBitmap ?: return

        scope.launch {
            isOptimizing = true
            optimizationProgress = 0f

            try {
                val result = pixelFruitEngine.process(
                    bitmap = source,
                    params = params,
                    onProgress = { step, progress ->
                        optimizationCurrentName = step
                        optimizationProgress = progress
                    }
                )
                optimizedBitmap = result
                previewMode = "after"
                // 更新处理后直方图
                histogram = histogramAnalyzer.analyze(result)
                Toast.makeText(context, "PixelFruit 优化完成", Toast.LENGTH_SHORT).show()
            } catch (e: OutOfMemoryError) {
                Toast.makeText(context, "内存不足，请选择较小图片", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "优化失败：${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isOptimizing = false
            }
        }
    }

    // 实时预览：参数变化时自动更新预览
    fun updatePreview() {
        val source = originalBitmap ?: return
        previewJob?.cancel()
        previewJob = scope.launch {
            kotlinx.coroutines.delay(150) // 150ms 防抖
            if (!isActive) return@launch
            try {
                val preview = pixelFruitEngine.processPreview(source, params)
                previewBitmap = preview
                if (previewMode == "before") previewMode = "preview"
            } catch (e: Exception) {
                android.util.Log.w("SmartOptimizeScreen", "实时预览更新失败", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ===== 顶部导航栏 =====
        SmartOptimizeTopBar(
            previewMode = previewMode,
            onPreviewToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                previewMode = when (previewMode) {
                    "before" -> "after"
                    "after" -> "compare"
                    else -> "before"
                }
            },
            onSave = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val bitmap = optimizedBitmap ?: originalBitmap ?: return@SmartOptimizeTopBar
                scope.launch {
                    isSaving = true
                    try {
                        val uri = withContext(Dispatchers.IO) { saveBitmapToGallery(context, bitmap, "PixelFruit") }
                        if (uri != null) Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                        else saveError = "保存失败"
                    } catch (e: Exception) { saveError = "保存失败：${e.message}" }
                    finally { isSaving = false }
                }
            },
            onShare = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val bitmap = optimizedBitmap ?: originalBitmap ?: return@SmartOptimizeTopBar
                scope.launch {
                    try {
                        val uri = withContext(Dispatchers.IO) { saveBitmapToCache(context, bitmap, "share_${System.currentTimeMillis()}.jpg") }
                        if (uri != null) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "分享优化照片").apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                        }
                    } catch (e: Exception) { saveError = "分享失败：${e.message}" }
                }
            },
            onBack = {
                if (optimizedBitmap != null) {
                    onApply(OptimizeParams(
                        hdrEnabled = params.highlights != 0f,
                        hdrStrength = params.highlights + 50f,
                        noiseReductionEnabled = params.noiseReduction > 0,
                        noiseReductionStrength = params.noiseReduction,
                        sharpenEnabled = params.sharpness > 0,
                        sharpenStrength = params.sharpness,
                        exposureAdjustment = params.exposure * 25f,
                        colorCorrectionEnabled = params.saturation != 100f,
                        colorCorrectionStrength = params.saturation - 100f
                    ))
                } else {
                    onBack()
                }
            },
            isSaving = isSaving,
            hasImage = originalBitmap != null,
            analysisResult = analysisResult
        )

        // ===== 优化进度条 =====
        AnimatedVisibility(visible = isOptimizing) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HasselbladOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PixelFruit 处理中", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${(optimizationProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { optimizationProgress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = HasselbladOrange,
                        trackColor = HasselbladOrange.copy(alpha = 0.2f)
                    )
                    if (optimizationCurrentName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("正在处理: $optimizationCurrentName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // ===== 图像预览区 =====
        ImagePreviewArea(
            originalBitmap = originalBitmap,
            optimizedBitmap = optimizedBitmap,
            previewBitmap = previewBitmap,
            previewMode = previewMode,
            isAnalyzing = isAnalyzing,
            onPickImage = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )

        // ===== 直方图 =====
        if (showHistogram && histogram != null) {
            HistogramView(
                histogram = histogram,
                mode = HistogramMode.RGB,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ===== Tab 切换栏 =====
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = HasselbladOrange,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = HasselbladOrange,
                    shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = index
                    },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    },
                    icon = {
                        Icon(
                            when (index) {
                                0 -> Icons.Default.Palette
                                1 -> Icons.Default.Tune
                                2 -> Icons.Default.CropFree
                                3 -> Icons.Default.FilterAlt
                                4 -> Icons.Default.AutoAwesome
                                5 -> Icons.Default.CameraAlt
                                else -> Icons.Default.Image
                            },
                            contentDescription = title,
                            modifier = Modifier.size(20.dp),
                            tint = if (selectedTab == index) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                )
            }
        }

        // ===== Tab 内容区 =====
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTab) {
                0 -> item { ColorAdjustPanel(params = params, onParamsChange = { params = it; updatePreview() }) }
                1 -> item { DetailPanel(params = params, onParamsChange = { params = it; updatePreview() }) }
                2 -> item {
                    val src = originalBitmap
                    if (src != null) {
                        CropRotatePanel(
                            sourceBitmap = src,
                            cropRect = cropRect,
                            rotationDegrees = rotationDegrees,
                            flipH = flipH,
                            flipV = flipV,
                            onCropRectChange = { cropRect = it },
                            onRotationChange = { rotationDegrees = it },
                            onFlipH = { flipH = !flipH },
                            onFlipV = { flipV = !flipV },
                            onApply = {
                                val result = cropRotateEngine.cropAndRotate(src, cropRect, rotationDegrees, flipH, flipV)
                                originalBitmap = result
                                optimizedBitmap = null
                                previewBitmap = null
                                previewMode = "before"
                                cropApplied = true
                                Toast.makeText(context, "裁剪旋转已应用", Toast.LENGTH_SHORT).show()
                            },
                            onReset = {
                                cropRect = null
                                rotationDegrees = 0f
                                flipH = false
                                flipV = false
                                cropApplied = false
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("请先选择图片", color = Color.Gray)
                        }
                    }
                }
                3 -> item { FilterPresetPanel(onApplyPreset = { params = it; updatePreview() }) }
                4 -> item { AIPanel(
                    params = params,
                    onParamsChange = { params = it; updatePreview() },
                    originalBitmap = originalBitmap,
                    inferenceEngine = inferenceEngine,
                    scope = scope,
                    context = context,
                    onAnalysisResult = { analysisResult = it },
                    setIsAnalyzing = { isAnalyzing = it }
                ) }
                5 -> item { LensCorrectionSmartPanel(
                    onAutoCorrect = {
                        originalBitmap?.let { bmp ->
                            scope.launch {
                                val corrected = withContext(Dispatchers.IO) {
                                    com.silas.omaster.engine.LensCorrectionEngine().autoCorrect(bmp)
                                }
                                originalBitmap = corrected
                                optimizedBitmap = null
                                previewBitmap = null
                                updatePreview()
                                Toast.makeText(context, "镜头校正已应用", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onApplyCA = { rOff, bOff ->
                        originalBitmap?.let { bmp ->
                            scope.launch {
                                val corrected = withContext(Dispatchers.IO) {
                                    val engine = com.silas.omaster.engine.LensCorrectionEngine()
                                    val p = com.silas.omaster.engine.LensCorrectionEngine.CorrectionParams(caRedOffset = rOff, caBlueOffset = bOff)
                                    engine.correctChromaticAberration(bmp, p)
                                }
                                originalBitmap = corrected
                                updatePreview()
                            }
                        }
                    }
                ) }
                6 -> item { RawFilePanel(
                    isLoading = false,
                    onLoadRaw = { path ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                com.silas.omaster.engine.RawDecodeEngine().decodeRaw(path)
                            }
                            if (result != null) {
                                originalBitmap = result.bitmap
                                optimizedBitmap = null
                                previewBitmap = null
                                previewMode = "before"
                                Toast.makeText(context, "RAW文件已加载: ${result.metadata.cameraModel}", Toast.LENGTH_SHORT).show()
                                updatePreview()
                            } else {
                                Toast.makeText(context, "RAW文件解码失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ===== 底部操作栏 =====
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    params = PixelFruitParams()
                    optimizedBitmap = null
                    previewBitmap = null
                    previewMode = "before"
                    analysisResult = null
                    previewJob?.cancel()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置")
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val bitmap = optimizedBitmap ?: originalBitmap ?: return@Button
                    scope.launch {
                        isSaving = true
                        try {
                            val uri = withContext(Dispatchers.IO) { saveBitmapToGallery(context, bitmap, "PixelFruit") }
                            if (uri != null) Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                            else saveError = "保存失败"
                        } catch (e: Exception) { saveError = "保存失败" }
                        finally { isSaving = false }
                    }
                },
                enabled = !isSaving && (optimizedBitmap != null || originalBitmap != null),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSaving) "保存中..." else "保存")
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    runOptimizeWorkflow()
                },
                enabled = !isOptimizing && params.changedParamCount() > 0,
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isOptimizing) "优化中..." else "智能优化")
            }
        }
    }
}

// ==================== 子面板组件 ====================

/**
 * 调色面板 — 对齐 PixelFruit color.js 的 10 个参数
 */
@Composable
private fun ColorAdjustPanel(
    params: PixelFruitParams,
    onParamsChange: (PixelFruitParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("光影调整")
        ParamSlider("亮度", params.brightness, 0.1f..4.0f, HasselbladOrange) { onParamsChange(params.copy(brightness = it)) }
        ParamSlider("曝光", params.exposure, -2f..2f, Color(0xFFFF9800)) { onParamsChange(params.copy(exposure = it)) }
        ParamSlider("对比度", params.contrast, -50f..50f, Color(0xFFE91E63)) { onParamsChange(params.copy(contrast = it)) }
        ParamSlider("饱和度", params.saturation, 0f..300f, Color(0xFF9C27B0)) { onParamsChange(params.copy(saturation = it)) }
        ParamSlider("高光", params.highlights, -50f..50f, WarningYellow) { onParamsChange(params.copy(highlights = it)) }
        ParamSlider("阴影", params.shadows, -50f..50f, Color(0xFF42A5F5)) { onParamsChange(params.copy(shadows = it)) }
        ParamSlider("白场", params.whites, 0f..200f, Color.White.copy(alpha = 0.8f)) { onParamsChange(params.copy(whites = it)) }

        SectionLabel("色调偏移")
        ParamSlider("红色调", params.redTint, -100f..100f, Color(0xFFF44336)) { onParamsChange(params.copy(redTint = it)) }
        ParamSlider("绿色调", params.greenTint, -100f..100f, Color(0xFF4CAF50)) { onParamsChange(params.copy(greenTint = it)) }
        ParamSlider("蓝色调", params.blueTint, -100f..100f, Color(0xFF2196F3)) { onParamsChange(params.copy(blueTint = it)) }
    }
}

/**
 * 细节处理面板 — 对齐 PixelFruit Details.js
 */
@Composable
private fun DetailPanel(
    params: PixelFruitParams,
    onParamsChange: (PixelFruitParams) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("锐化与降噪")
        ParamSlider("锐化", params.sharpness, 0f..100f, Color(0xFF9C27B0)) { onParamsChange(params.copy(sharpness = it)) }
        ParamSlider("降噪", params.noiseReduction, 0f..100f, Color(0xFF2196F3)) { onParamsChange(params.copy(noiseReduction = it)) }

        SectionLabel("面部美化")
        ParamSlider("面部美白", params.faceBrightening, 0f..100f, Color(0xFFFFAB91)) { onParamsChange(params.copy(faceBrightening = it)) }
        ParamSlider("过渡平滑", params.faceSmoothness, 0f..100f, Color(0xFF80DEEA)) { onParamsChange(params.copy(faceSmoothness = it)) }
    }
}

/**
 * 滤镜预设面板 — 对齐 PixelFruit Filter.js 7 个内置预设
 */
@Composable
private fun FilterPresetPanel(
    onApplyPreset: (PixelFruitParams) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("内置预设")
        BuiltInPresets.presets.forEach { preset ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "preset_scale"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(interactionSource = interactionSource, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApplyPreset(preset.params)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(HasselbladOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, preset.name, tint = HasselbladOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text(preset.description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * AI 一键调色面板 — 对齐 PixelFruit Ai.js + 通义千问 VL
 */
@Composable
private fun AIPanel(
    params: PixelFruitParams,
    onParamsChange: (PixelFruitParams) -> Unit,
    originalBitmap: Bitmap?,
    inferenceEngine: MasterInferenceEngine,
    scope: CoroutineScope,
    context: Context,
    onAnalysisResult: (com.silas.omaster.model.SceneProfile) -> Unit,
    setIsAnalyzing: (Boolean) -> Unit
) {
    var isAILoading by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // AI 调色卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(HasselbladOrange.copy(alpha = 0.9f), WarningYellow.copy(alpha = 0.8f))
                    )
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val bitmap = originalBitmap ?: return@clickable
                    isAILoading = true
                    setIsAnalyzing(true)
                    analyzeImage(
                        bitmap,
                        inferenceEngine,
                        scope,
                        onResult = { result ->
                            onAnalysisResult(result)
                            // 根据AI场景推荐参数（对齐 PixelFruit applyAutoRecommendations）
                            val recommended = when (result.category) {
                                com.silas.omaster.model.SceneCategory.PORTRAIT -> PixelFruitParams(brightness = 1.1f, exposure = 0.3f, saturation = 120f, faceBrightening = 40f, faceSmoothness = 70f)
                                com.silas.omaster.model.SceneCategory.LANDSCAPE -> PixelFruitParams(saturation = 125f, contrast = 10f, sharpness = 20f, shadows = 5f)
                                com.silas.omaster.model.SceneCategory.NIGHT -> PixelFruitParams(exposure = 0.4f, noiseReduction = 35f, saturation = 85f, highlights = -15f)
                                com.silas.omaster.model.SceneCategory.FOOD -> PixelFruitParams(brightness = 1.15f, saturation = 140f, contrast = 8f, sharpness = 15f)
                                com.silas.omaster.model.SceneCategory.URBAN -> PixelFruitParams(contrast = 12f, saturation = 115f, sharpness = 25f, shadows = 3f)
                                else -> PixelFruitParams(brightness = 1.05f, saturation = 110f, contrast = 5f)
                            }
                            onParamsChange(recommended)
                            isAILoading = false
                            setIsAnalyzing(false)
                        },
                        onError = {
                            isAILoading = false
                            setIsAnalyzing(false)
                        }
                    )
                }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAILoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI 一键调色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("智能识别场景，自动推荐最佳参数", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                }
                Icon(Icons.Default.AutoFixHigh, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }

        // 当前参数摘要
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, null, tint = HasselbladOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("当前参数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${params.changedParamCount()} 项已调整", style = MaterialTheme.typography.labelMedium, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
                }
                if (params.changedParamCount() == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("所有参数为默认值，点击上方 AI 按钮开始", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// ==================== 基础UI组件 ====================

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

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(56.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
        Text(
            String.format("%.1f", value),
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun SmartOptimizeTopBar(
    previewMode: String,
    onPreviewToggle: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    isSaving: Boolean,
    hasImage: Boolean,
    analysisResult: SceneProfile?
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("智能优化", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PixelFruit", style = MaterialTheme.typography.labelSmall, color = HasselbladOrange.copy(alpha = 0.7f))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
            }
        },
        actions = {
            analysisResult?.let {
                Row(
                    modifier = Modifier.background(HasselbladOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(it.name, style = MaterialTheme.typography.labelSmall, color = HasselbladOrange, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onPreviewToggle) {
                Icon(
                    when (previewMode) { "after" -> Icons.Default.Visibility; "compare" -> Icons.Default.Compare; else -> Icons.Default.Image },
                    "预览",
                    tint = if (previewMode != "before") HasselbladOrange else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onSave, enabled = !isSaving && hasImage) {
                Icon(Icons.Default.Save, "保存", tint = if (isSaving) Color.Gray else HasselbladOrange)
            }
            IconButton(onClick = onShare, enabled = hasImage) {
                Icon(Icons.Default.Share, "分享", tint = MaterialTheme.colorScheme.onBackground)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun ImagePreviewArea(
    originalBitmap: Bitmap?,
    optimizedBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    previewMode: String,
    isAnalyzing: Boolean,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF0D0D0D))
    ) {
        if (previewMode == "compare" && originalBitmap != null && optimizedBitmap != null) {
            BeforeAfterCompareView(beforeBitmap = originalBitmap, afterBitmap = optimizedBitmap, modifier = Modifier.fillMaxSize())
        } else {
            val displayBitmap = when (previewMode) {
                "after" -> optimizedBitmap ?: originalBitmap
                "preview" -> previewBitmap ?: originalBitmap
                else -> originalBitmap
            }
            displayBitmap?.let { bitmap ->
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "预览", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: run {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择图片开始优化", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.4f))
                }
            }
        }
        // 状态标签
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when {
                    previewMode == "compare" -> "前后对比"
                    previewMode == "preview" -> "实时预览"
                    previewMode == "after" && optimizedBitmap != null -> "优化后"
                    else -> "原图"
                },
                color = if (previewMode != "before") HasselbladOrange else Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
        // 选图按钮
        IconButton(
            onClick = onPickImage,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, "选择图片", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        // AI分析中
        if (isAnalyzing) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(HasselbladOrange.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = HasselbladOrange)
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI 场景识别中...", style = MaterialTheme.typography.labelSmall, color = HasselbladOrange)
            }
        }
    }
}

// ==================== 辅助函数 ====================

private fun loadSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        val sampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        decoded?.let { applyExifOrientation(context, uri, it) }
    } catch (_: Exception) { null } catch (_: OutOfMemoryError) { null }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight; val width = options.outWidth
    var inSampleSize = 1
    if (height <= 0 || width <= 0) return inSampleSize
    while (height / (inSampleSize * 2) >= reqHeight && width / (inSampleSize * 2) >= reqWidth) { inSampleSize *= 2 }
    return inSampleSize
}

private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = android.media.ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    } catch (_: Exception) { bitmap }
}

private fun analyzeImage(
    bitmap: Bitmap,
    engine: MasterInferenceEngine,
    scope: CoroutineScope,
    onResult: (SceneProfile) -> Unit,
    onError: (() -> Unit)? = null
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = engine.analyzeImage(bitmap)
            withContext(Dispatchers.Main) { onResult(result) }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) { onError?.invoke() }
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, tag: String): Uri? {
    return try {
        val filename = "${tag}_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OMaster/PixelFruit")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.also {
            context.contentResolver.openOutputStream(it)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            MediaScannerConnection.scanFile(context, arrayOf(it.toString()), arrayOf("image/jpeg"), null)
        }
    } catch (_: Exception) { null }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): Uri? {
    return try {
        val cacheDir = java.io.File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val file = java.io.File(cacheDir, filename)
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) { null }
}

// ==================== 裁剪旋转面板 ====================

@Composable
private fun CropRotatePanel(
    sourceBitmap: Bitmap,
    cropRect: android.graphics.RectF?,
    rotationDegrees: Float,
    flipH: Boolean,
    flipV: Boolean,
    onCropRectChange: (android.graphics.RectF?) -> Unit,
    onRotationChange: (Float) -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val engine = remember { com.silas.omaster.engine.CropRotateEngine() }
    var selectedRatio by remember { mutableStateOf<com.silas.omaster.engine.CropAspectRatio>(com.silas.omaster.engine.CropAspectRatio.FREE) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("裁剪比例")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(com.silas.omaster.engine.CropAspectRatio.entries.toList()) { ratio ->
                val selected = selectedRatio == ratio
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRatio = ratio
                        if (ratio != com.silas.omaster.engine.CropAspectRatio.FREE) {
                            val rect = engine.calculateInitialCropRect(sourceBitmap.width, sourceBitmap.height, ratio.ratio)
                            onCropRectChange(rect)
                        } else {
                            onCropRectChange(null)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) HasselbladOrange else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(ratio.label, fontSize = 12.sp)
                }
            }
        }

        SectionLabel("旋转与翻转")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRotationChange((rotationDegrees - 90f).mod(360f))
            }, shape = RoundedCornerShape(10.dp)) { Text("左转90", fontSize = 12.sp) }
            OutlinedButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRotationChange((rotationDegrees + 90f).mod(360f))
            }, shape = RoundedCornerShape(10.dp)) { Text("右转90", fontSize = 12.sp) }
            OutlinedButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFlipH()
            }, shape = RoundedCornerShape(10.dp)) { Text("水平翻转", fontSize = 12.sp) }
            OutlinedButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFlipV()
            }, shape = RoundedCornerShape(10.dp)) { Text("垂直翻转", fontSize = 12.sp) }
        }

        SectionLabel("自由旋转")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("角度", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = rotationDegrees,
                onValueChange = onRotationChange,
                valueRange = -180f..180f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )
            Text("${rotationDegrees.toInt()}°", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置")
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply()
                },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("应用裁剪")
            }
        }
    }
}

@Composable
private fun BeforeAfterCompareView(beforeBitmap: Bitmap, afterBitmap: Bitmap, modifier: Modifier = Modifier) {
    var dividerOffset by remember { mutableFloatStateOf(0.5f) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    Box(modifier = modifier) {
        Image(bitmap = afterBitmap.asImageBitmap(), contentDescription = "优化后", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectHorizontalDragGestures { _, dragAmount -> dividerOffset = (dividerOffset + dragAmount / size.width).coerceIn(0f, 1f) } }) {
            Box(modifier = Modifier.fillMaxWidth(dividerOffset).fillMaxHeight()) {
                Image(bitmap = beforeBitmap.asImageBitmap(), contentDescription = "原图", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(modifier = Modifier.align(Alignment.CenterStart).offset(x = (dividerOffset * screenWidthDp).dp).width(2.dp).fillMaxHeight().background(Color.White))
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("原图", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                Text("优化后", color = HasselbladOrange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
            }
        }
    }
}

// ==================== 镜头校正面板（SmartOptimize 专用） ====================

@Composable
private fun LensCorrectionSmartPanel(
    onAutoCorrect: () -> Unit,
    onApplyCA: (redOffset: Float, blueOffset: Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val engine = remember { com.silas.omaster.engine.LensCorrectionEngine() }

    // 镜头校正参数
    var caRedOffset by remember { mutableFloatStateOf(0f) }
    var caBlueOffset by remember { mutableFloatStateOf(0f) }
    var vignetteAmount by remember { mutableFloatStateOf(0f) }
    var vignetteRadius by remember { mutableFloatStateOf(50f) }
    var distortion by remember { mutableFloatStateOf(0f) }
    var selectedProfileIdx by remember { mutableStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 一键自动校正
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAutoCorrect()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("自动镜头校正")
        }

        // 镜头预设选择
        SectionLabel("镜头预设")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(engine.lensProfiles.size) { index ->
                val profile = engine.lensProfiles[index]
                val selected = selectedProfileIdx == index
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedProfileIdx = index
                        val params = engine.paramsFromProfile(profile)
                        caRedOffset = params.caRedOffset
                        caBlueOffset = params.caBlueOffset
                        vignetteAmount = params.vignetteAmount
                        distortion = params.distortion
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) HasselbladOrange else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("${profile.focalLength}", fontSize = 12.sp)
                }
            }
        }

        // 色差校正
        SectionLabel("色差校正")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R偏移", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = caRedOffset, onValueChange = { caRedOffset = it },
                valueRange = -5f..5f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
            )
            Text(String.format("%.1f", caRedOffset), modifier = Modifier.width(48.dp), fontSize = 13.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B偏移", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = caBlueOffset, onValueChange = { caBlueOffset = it },
                valueRange = -5f..5f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
            )
            Text(String.format("%.1f", caBlueOffset), modifier = Modifier.width(48.dp), fontSize = 13.sp, color = Color.Blue, fontWeight = FontWeight.SemiBold)
        }

        // 暗角校正
        SectionLabel("暗角校正")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("校正量", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = vignetteAmount, onValueChange = { vignetteAmount = it },
                valueRange = 0f..100f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )
            Text("${vignetteAmount.toInt()}", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
        }

        // 畸变校正
        SectionLabel("畸变校正")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("系数", modifier = Modifier.width(48.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = distortion, onValueChange = { distortion = it },
                valueRange = -0.1f..0.1f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )
            Text(String.format("%.3f", distortion), modifier = Modifier.width(48.dp), fontSize = 13.sp, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
        }

        // 应用按钮
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    caRedOffset = 0f; caBlueOffset = 0f
                    vignetteAmount = 0f; distortion = 0f
                    selectedProfileIdx = -1
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("重置")
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApplyCA(caRedOffset, caBlueOffset)
                },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("应用校正")
            }
        }
    }
}

// ==================== RAW 文件面板 ====================

@Composable
private fun RawFilePanel(
    isLoading: Boolean,
    onLoadRaw: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val engine = remember { com.silas.omaster.engine.RawDecodeEngine() }

    var rawFilePath by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(com.silas.omaster.engine.RawDecodeEngine.RawFormat.DNG) }
    var decodedMetadata by remember { mutableStateOf<com.silas.omaster.engine.RawDecodeEngine.RawMetadata?>(null) }

    // RAW 文件选择器
    val rawPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // 复制到缓存文件（RAW 解码需要文件路径）
                val inputStream = context.contentResolver.openInputStream(it)
                val fileName = it.lastPathSegment ?: "raw_file"
                val cacheFile = java.io.File(context.cacheDir, "raw_temp_${System.currentTimeMillis()}_${fileName}")
                inputStream?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                rawFilePath = cacheFile.absolutePath
                onLoadRaw(cacheFile.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(context, "RAW文件读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedY(10.dp)) {
        // RAW 文件说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, null, tint = HasselbladOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RAW 文件解码", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "支持 DNG / ARW / NEF / CR2 / ORF / RAF / RW2 / PEP / SRW 格式，保留完整传感器数据，14-bit 高精度解码",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // 格式选择
        SectionLabel("RAW 格式")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(com.silas.omaster.engine.RawDecodeEngine.RawFormat.entries.dropLast(1)) { format ->
                val selected = selectedFormat == format
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFormat = format
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) HasselbladOrange else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(format.extension.uppercase(), fontSize = 12.sp)
                }
            }
        }

        // 选择 RAW 文件按钮
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                rawPickerLauncher.launch(arrayOf("image/*", "application/octet-stream"))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("解码中...")
            } else {
                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("选择 RAW 文件")
            }
        }

        // 已解码的元数据展示
        decodedMetadata?.let { meta ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel("文件信息")
                    RawMetaRow("格式", meta.format.name)
                    RawMetaRow("相机", meta.cameraModel.ifEmpty { "未知" })
                    RawMetaRow("尺寸", "${meta.width} x ${meta.height}")
                    RawMetaRow("位深", "${meta.bitsPerPixel}-bit")
                    RawMetaRow("ISO", meta.iso.toString())
                    RawMetaRow("快门", meta.shutterSpeed)
                    RawMetaRow("光圈", meta.aperture)
                    RawMetaRow("焦距", meta.focalLength)
                }
            }
        }
    }
}

@Composable
private fun RawMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
    }
}
