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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
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
import androidx.compose.runtime.mutableStateListOf
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
import com.silas.omaster.ui.theme.SurfaceElevated
import com.silas.omaster.ui.theme.WarningYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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

    // 图片状态
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var optimizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
    val tabs = listOf("调色", "细节", "滤镜", "AI")

    // 优化进度
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationStep by remember { mutableStateOf(0) }
    var optimizationTotalSteps by remember { mutableStateOf(0) }
    var optimizationProgress by remember { mutableFloatStateOf(0f) }
    var optimizationCurrentName by remember { mutableStateOf("") }
    val optimizedOptions = remember { mutableStateListOf<String>() }

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
                    previewMode = "before"
                    analysisResult = null
                    // 自动AI场景识别
                    analyzeImage(loadedBitmap, inferenceEngine, scope) { result ->
                        analysisResult = result
                    }
                } else {
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 从assets加载示例预览图
    LaunchedEffect(Unit) {
        try {
            context.assets.open("images/placeholder.webp").use { stream ->
                originalBitmap = BitmapFactory.decodeStream(stream)
            }
        } catch (_: IOException) { }
    }

    LaunchedEffect(saveError) {
        saveError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            saveError = null
        }
    }

    // 参数ID映射
    val paramIdToName = mapOf(
        "brightness" to "亮度", "exposure" to "曝光", "saturation" to "饱和度",
        "contrast" to "对比度", "highlights" to "高光", "shadows" to "阴影",
        "whites" to "白场", "color" to "色调", "sharpness" to "锐化",
        "denoise" to "降噪", "face" to "面部美化"
    )

    // 执行优化工作流（对齐 PixelFruit applyAdjustmentsToCachedData 顺序）
    fun runOptimizeWorkflow() {
        val activeParams = params
        val selectedIds = buildList {
            if (activeParams.brightness != 1.0f || activeParams.exposure != 0f ||
                activeParams.saturation != 100f || activeParams.contrast != 0f ||
                activeParams.highlights != 0f || activeParams.shadows != 0f ||
                activeParams.whites != 100f || activeParams.redTint != 0f ||
                activeParams.greenTint != 0f || activeParams.blueTint != 0f
            ) add("color")
            if (activeParams.noiseReduction > 0f) add("denoise")
            if (activeParams.sharpness > 0f) add("sharpness")
            if (activeParams.faceBrightening > 0f) add("face")
        }
        if (selectedIds.isEmpty()) return

        scope.launch {
            isOptimizing = true
            optimizedOptions.clear()
            optimizationTotalSteps = selectedIds.size
            optimizationStep = 0
            optimizationProgress = 0f

            val source = originalBitmap ?: run { isOptimizing = false; return@launch }
            val mutableSource = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)

            try {
                var workingBitmap: Bitmap = mutableSource
                for ((index, id) in selectedIds.withIndex()) {
                    optimizationStep = index + 1
                    optimizationCurrentName = paramIdToName[id] ?: id
                    optimizationProgress = index.toFloat() / selectedIds.size

                    val strength = when (id) {
                        "color" -> 1.0f
                        "denoise" -> activeParams.noiseReduction / 100f
                        "sharpness" -> activeParams.sharpness / 100f
                        "face" -> activeParams.faceBrightening / 100f
                        else -> 0.5f
                    }
                    val prevBitmap = workingBitmap
                    workingBitmap = withContext(Dispatchers.Default) {
                        inferenceEngine.applyOptimization(workingBitmap, id, strength)
                    }
                    if (prevBitmap !== mutableSource && prevBitmap !== source && !prevBitmap.isRecycled) {
                        prevBitmap.recycle()
                    }
                    optimizationProgress = (index + 1).toFloat() / selectedIds.size
                    optimizedOptions.add(id)
                }
                optimizedBitmap = workingBitmap
                previewMode = "after"
                Toast.makeText(context, "优化完成", Toast.LENGTH_SHORT).show()
            } catch (e: OutOfMemoryError) {
                Toast.makeText(context, "内存不足，请选择较小图片", Toast.LENGTH_LONG).show()
                optimizedOptions.clear()
            } catch (e: Exception) {
                Toast.makeText(context, "优化失败：${e.message}", Toast.LENGTH_LONG).show()
                optimizedOptions.clear()
            } finally {
                isOptimizing = false
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
                        Text("正在优化 $optimizationStep/$optimizationTotalSteps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
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
            previewMode = previewMode,
            isAnalyzing = isAnalyzing,
            onPickImage = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )

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
                                2 -> Icons.Default.FilterAlt
                                else -> Icons.Default.AutoAwesome
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
                0 -> item { ColorAdjustPanel(params = params, onParamsChange = { params = it }) }
                1 -> item { DetailPanel(params = params, onParamsChange = { params = it }) }
                2 -> item { FilterPresetPanel(onApplyPreset = { params = it }) }
                3 -> item { AIPanel(
                    params = params,
                    onParamsChange = { params = it },
                    originalBitmap = originalBitmap,
                    inferenceEngine = inferenceEngine,
                    scope = scope,
                    context = context
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
                    optimizedOptions.clear()
                    optimizedBitmap = null
                    previewMode = "before"
                    analysisResult = null
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
    context: Context
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
                    analyzeImage(bitmap, inferenceEngine, scope) { result ->
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
                    }
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
                    Text(it.name, fontSize = 11.sp, color = HasselbladOrange, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            val displayBitmap = if (previewMode == "after") optimizedBitmap ?: originalBitmap else originalBitmap
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
                when { previewMode == "compare" -> "前后对比"; previewMode == "after" && optimizedBitmap != null -> "优化后"; else -> "原图" },
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
                Text("AI 场景识别中...", fontSize = 11.sp, color = HasselbladOrange)
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

private fun analyzeImage(bitmap: Bitmap, engine: MasterInferenceEngine, scope: CoroutineScope, onResult: (SceneProfile) -> Unit) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = engine.analyzeImage(bitmap)
            withContext(Dispatchers.Main) { onResult(result) }
        } catch (_: Exception) { }
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
                Text("原图", color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                Text("优化后", color = HasselbladOrange, fontSize = 11.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
            }
        }
    }
}
