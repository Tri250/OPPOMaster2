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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
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
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.SurfaceElevated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 智能优化页面
 *
 * 功能：
 * - AI 场景识别自动推荐优化项
 * - HDR 增强 / 降噪 / 锐化 / 曝光 / 色彩校正（强度可调）
 * - 综合优化一键处理 + 进度流程
 * - 前后拖拽对比预览
 * - 保存到相册 / 分享
 *
 * 对齐 Web 端 SmartOptimizePage.tsx
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

    // AI 推理引擎实例
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }

    // 原图与优化后图片状态
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var optimizedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 预览模式："before"/"after"/"compare"
    var previewMode by remember { mutableStateOf("before") }

    // AI 场景识别结果
    var analysisResult by remember { mutableStateOf<SceneProfile?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // 优化参数状态（需在 imagePickerLauncher 之前声明）
    var hdrEnabled by remember { mutableStateOf(false) }
    var hdrStrength by remember { mutableFloatStateOf(50f) }

    var noiseReductionEnabled by remember { mutableStateOf(false) }
    var noiseReductionStrength by remember { mutableFloatStateOf(30f) }

    var sharpenEnabled by remember { mutableStateOf(true) }
    var sharpenStrength by remember { mutableFloatStateOf(25f) }

    var exposureAuto by remember { mutableStateOf(false) }
    var exposureAdjustment by remember { mutableFloatStateOf(0f) }

    var colorCorrectionEnabled by remember { mutableStateOf(true) }
    var colorCorrectionStrength by remember { mutableFloatStateOf(20f) }

    // 图片选择器（使用 PickVisualMedia 符合 Android 16 隐私最佳实践）
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                // 降采样加载，防止大图 OOM（限制 2048px）
                val loadedBitmap = loadSampledBitmap(context, it, 2048)
                if (loadedBitmap != null) {
                    originalBitmap = loadedBitmap
                    optimizedBitmap = null
                    previewMode = "before"
                    analysisResult = null
                    // 自动触发 AI 场景识别
                    analyzeImage(loadedBitmap, inferenceEngine, scope) { result ->
                        analysisResult = result
                        // 根据场景自动推荐优化项
                        applyAutoRecommendations(result) { hdr, denoise, sharpen, exposure, color,
                            hdrS, denoiseS, sharpenS, exposureS, colorS ->
                            hdrEnabled = hdr
                            noiseReductionEnabled = denoise
                            sharpenEnabled = sharpen
                            exposureAuto = exposure
                            colorCorrectionEnabled = color
                            hdrStrength = hdrS
                            noiseReductionStrength = denoiseS
                            sharpenStrength = sharpenS
                            exposureAdjustment = exposureS
                            colorCorrectionStrength = colorS
                        }
                    }
                } else {
                    Toast.makeText(context, "图片加载失败，请选择其他图片", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 优化进度
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationStep by remember { mutableStateOf(0) }
    var optimizationTotalSteps by remember { mutableStateOf(0) }
    var optimizationProgress by remember { mutableFloatStateOf(0f) }
    var optimizationCurrentName by remember { mutableStateOf("") }
    var optimizedOptions = remember { mutableStateListOf<String>() }

    // 保存/分享状态
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // 从 assets 加载示例预览图
    LaunchedEffect(Unit) {
        try {
            context.assets.open("images/placeholder.webp").use { stream ->
                originalBitmap = BitmapFactory.decodeStream(stream)
            }
        } catch (e: IOException) {
            // 资源不存在时保持空，将显示占位提示
        }
    }

    // 已选中的优化项（用于进度流程）
    val selectedOptimizeIds = remember(hdrEnabled, noiseReductionEnabled, sharpenEnabled, exposureAuto, colorCorrectionEnabled) {
        buildList {
            if (hdrEnabled) add("hdr")
            if (noiseReductionEnabled) add("denoise")
            if (sharpenEnabled) add("sharpen")
            if (exposureAuto) add("exposure")
            if (colorCorrectionEnabled) add("color")
        }
    }

    // 强度映射：滑块值(0~100) → 算法强度(0.0~1.0)
    val optimizeIdToStrength: Map<String, Float> = mapOf(
        "hdr" to hdrStrength / 100f,
        "denoise" to noiseReductionStrength / 100f,
        "sharpen" to sharpenStrength / 100f,
        "exposure" to (exposureAdjustment + 50f) / 100f, // -50~50 → 0~1
        "color" to colorCorrectionStrength / 100f
    )

    val optimizeIdToName = mapOf(
        "hdr" to "HDR增强",
        "denoise" to "智能降噪",
        "sharpen" to "锐化增强",
        "exposure" to "自动曝光",
        "color" to "色彩校正"
    )

    // 保存错误 Toast
    LaunchedEffect(saveError) {
        saveError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            saveError = null
        }
    }

    // 顺序执行优化流程（对齐Web端handleOptimize + processStep）
    fun runOptimizeWorkflow() {
        if (selectedOptimizeIds.isEmpty()) return
        scope.launch {
            isOptimizing = true
            optimizedOptions.clear()
            optimizationTotalSteps = selectedOptimizeIds.size
            optimizationStep = 0
            optimizationProgress = 0f

            val source = originalBitmap ?: run {
                isOptimizing = false
                return@launch
            }
            // 确保源图可变（部分场景下解码的 bitmap 是 immutable，后续 Canvas 绘制需要 mutable）
            val mutableSource = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)

            try {
                var workingBitmap: Bitmap = mutableSource
                for ((index, id) in selectedOptimizeIds.withIndex()) {
                    optimizationStep = index + 1
                    optimizationCurrentName = optimizeIdToName[id] ?: id
                    optimizationProgress = (index.toFloat()) / selectedOptimizeIds.size

                    // 调用 AI 推理引擎执行真实优化处理，传入强度参数
                    val strength = optimizeIdToStrength[id] ?: 0.5f
                    val prevBitmap = workingBitmap
                    workingBitmap = withContext(Dispatchers.Default) {
                        inferenceEngine.applyOptimization(workingBitmap, id, strength)
                    }
                    // 回收中间 Bitmap（非原图），防止内存泄漏
                    if (prevBitmap !== mutableSource && prevBitmap !== source && !prevBitmap.isRecycled) {
                        prevBitmap.recycle()
                    }

                    optimizationProgress = (index + 1).toFloat() / selectedOptimizeIds.size
                    optimizedOptions.add(id)
                }
                optimizedBitmap = workingBitmap
                previewMode = "after"
                // 优化完成提示，不自动跳转——让用户查看结果、保存或手动应用参数
                Toast.makeText(context, "优化完成，可保存图片或返回应用参数", Toast.LENGTH_SHORT).show()
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("SmartOptimize", "Optimization OOM", e)
                Toast.makeText(context, "内存不足，请选择较小图片或减少优化项", Toast.LENGTH_LONG).show()
                // OOM 时清理中间 bitmap 引用，帮助 GC 回收
                if (mutableSource !== source && !mutableSource.isRecycled) mutableSource.recycle()
                optimizedOptions.clear()
            } catch (e: Exception) {
                android.util.Log.e("SmartOptimize", "Optimization failed", e)
                Toast.makeText(context, "优化失败：${e.message}，请重试", Toast.LENGTH_LONG).show()
                if (mutableSource !== source && !mutableSource.isRecycled) mutableSource.recycle()
                optimizedOptions.clear()
            } finally {
                isOptimizing = false
            }
        }
    }

    // 保存图片到相册
    fun saveToGallery() {
        val bitmap = optimizedBitmap ?: originalBitmap ?: return
        scope.launch {
            isSaving = true
            try {
                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToGallery(context, bitmap, "SmartOptimize")
                }
                if (uri != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    saveError = "保存失败，请检查存储权限或可用空间"
                }
            } catch (e: Exception) {
                saveError = "保存失败：${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    // 分享图片
    fun shareOptimizedImage() {
        val bitmap = optimizedBitmap ?: originalBitmap ?: return
        scope.launch {
            try {
                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(context, bitmap, "share_smart_optimize_${System.currentTimeMillis()}.jpg")
                }
                if (uri != null) {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = android.content.Intent.createChooser(shareIntent, "分享优化照片")
                        .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(chooser)
                } else {
                    saveError = "分享失败，无法创建分享文件"
                }
            } catch (e: Exception) {
                saveError = "分享失败：${e.message}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TopAppBar(
            title = { Text("智能优化", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // 如果已有优化结果，返回时自动将参数应用到设置
                    if (optimizedBitmap != null) {
                        onApply(OptimizeParams(
                            hdrEnabled = hdrEnabled,
                            hdrStrength = hdrStrength,
                            noiseReductionEnabled = noiseReductionEnabled,
                            noiseReductionStrength = noiseReductionStrength,
                            sharpenEnabled = sharpenEnabled,
                            sharpenStrength = sharpenStrength,
                            exposureAdjustment = exposureAdjustment,
                            colorCorrectionEnabled = colorCorrectionEnabled,
                            colorCorrectionStrength = colorCorrectionStrength
                        ))
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            actions = {
                // AI 场景识别结果徽标
                analysisResult?.let { result ->
                    Row(
                        modifier = Modifier
                            .background(
                                HasselbladOrange.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome, null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = result.name,
                            fontSize = 11.sp,
                            color = HasselbladOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 预览切换
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    previewMode = when (previewMode) {
                        "before" -> "after"
                        "after" -> "compare"
                        else -> "before"
                    }
                }) {
                    Icon(
                        when (previewMode) {
                            "after" -> Icons.Default.Visibility
                            "compare" -> Icons.Default.Compare
                            else -> Icons.Default.Image
                        },
                        "预览",
                        tint = if (previewMode != "before") HasselbladOrange else MaterialTheme.colorScheme.onBackground
                    )
                }
                // 保存按钮
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saveToGallery()
                    },
                    enabled = !isSaving && (optimizedBitmap != null || originalBitmap != null)
                ) {
                    Icon(Icons.Default.Save, "保存", tint = if (isSaving) Color.Gray else HasselbladOrange)
                }
                // 分享按钮
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareOptimizedImage()
                    },
                    enabled = optimizedBitmap != null || originalBitmap != null
                ) {
                    Icon(Icons.Default.Share, "分享", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // 优化进度条
        AnimatedVisibility(visible = isOptimizing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在优化 $optimizationStep/$optimizationTotalSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${(optimizationProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = HasselbladOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { optimizationProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = HasselbladOrange,
                        trackColor = HasselbladOrange.copy(alpha = 0.2f)
                    )
                    if (optimizationCurrentName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "正在处理: $optimizationCurrentName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF1A1A1A))
        ) {
            if (previewMode == "compare" && originalBitmap != null && optimizedBitmap != null) {
                // 前后拖拽对比
                BeforeAfterCompareView(
                    beforeBitmap = originalBitmap,
                    afterBitmap = optimizedBitmap,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val displayBitmap = if (previewMode == "after") optimizedBitmap ?: originalBitmap else originalBitmap
                displayBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = if (previewMode == "after") "优化后预览" else "原图预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Image, null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选择图片开始优化",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 顶部状态徽标
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        previewMode == "compare" -> "前后对比"
                        previewMode == "after" && optimizedBitmap != null -> "优化后"
                        else -> "原图"
                    },
                    color = if (previewMode != "before") HasselbladOrange else Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // 选择图片按钮
            IconButton(
                onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "选择图片",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // AI 分析中指示器
            if (isAnalyzing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            HasselbladOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = HasselbladOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI 场景识别中...",
                        fontSize = 11.sp,
                        color = HasselbladOrange
                    )
                }
            }
        }

        // 优化选项列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI 推荐提示
            if (analysisResult != null && !isOptimizing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = HasselbladOrange.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome, null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI 推荐：${analysisResult?.name ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HasselbladOrange
                                )
                                Text(
                                    text = "已根据场景自动推荐优化项与强度",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 已完成的优化项
            if (optimizedOptions.isNotEmpty() && !isOptimizing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "已完成 ${optimizedOptions.size} 项优化",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = optimizedOptions.joinToString(" · ") { optimizeIdToName[it] ?: it },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // 综合优化
            item {
                CompositeOptimizeCard(
                    enabled = hdrEnabled && noiseReductionEnabled && sharpenEnabled && colorCorrectionEnabled,
                    onEnable = {
                        hdrEnabled = true
                        noiseReductionEnabled = true
                        sharpenEnabled = true
                        exposureAuto = true
                        colorCorrectionEnabled = true
                    },
                    onDisable = {
                        hdrEnabled = false
                        noiseReductionEnabled = false
                        sharpenEnabled = false
                        exposureAuto = false
                        colorCorrectionEnabled = false
                    },
                    onStartOptimize = { runOptimizeWorkflow() },
                    isOptimizing = isOptimizing,
                    selectedCount = selectedOptimizeIds.size
                )
            }

            // HDR 增强
            item {
                OptimizeOptionCard(
                    title = "HDR增强",
                    description = "提升动态范围，保留更多细节",
                    icon = Icons.Default.AutoFixHigh,
                    enabled = hdrEnabled,
                    onToggle = { hdrEnabled = it },
                    strength = hdrStrength,
                    onStrengthChange = { hdrStrength = it },
                    color = Color(0xFFFF9800),
                    isOptimized = optimizedOptions.contains("hdr"),
                    isProcessing = isOptimizing && optimizationCurrentName == "HDR增强"
                )
            }

            // 智能降噪
            item {
                OptimizeOptionCard(
                    title = "智能降噪",
                    description = "AI识别并消除噪点，O(n) boxBlur算法",
                    icon = Icons.Default.FilterAlt,
                    enabled = noiseReductionEnabled,
                    onToggle = { noiseReductionEnabled = it },
                    strength = noiseReductionStrength,
                    onStrengthChange = { noiseReductionStrength = it },
                    color = Color(0xFF2196F3),
                    isOptimized = optimizedOptions.contains("denoise"),
                    isProcessing = isOptimizing && optimizationCurrentName == "智能降噪"
                )
            }

            // 锐化增强
            item {
                OptimizeOptionCard(
                    title = "锐化增强",
                    description = "Unsharp Mask 提升画面清晰度和质感",
                    icon = Icons.Default.Tune,
                    enabled = sharpenEnabled,
                    onToggle = { sharpenEnabled = it },
                    strength = sharpenStrength,
                    onStrengthChange = { sharpenStrength = it },
                    color = Color(0xFF9C27B0),
                    isOptimized = optimizedOptions.contains("sharpen"),
                    isProcessing = isOptimizing && optimizationCurrentName == "锐化增强"
                )
            }

            // 自动曝光
            item {
                OptimizeOptionCard(
                    title = "自动曝光",
                    description = "智能调整曝光，优化亮度",
                    icon = Icons.Default.LightMode,
                    enabled = exposureAuto,
                    onToggle = { exposureAuto = it },
                    strength = exposureAdjustment,
                    strengthRange = -50f..50f,
                    onStrengthChange = { exposureAdjustment = it },
                    color = Color(0xFFFFEB3B),
                    isOptimized = optimizedOptions.contains("exposure"),
                    isProcessing = isOptimizing && optimizationCurrentName == "自动曝光"
                )
            }

            // 智能色彩校正
            item {
                OptimizeOptionCard(
                    title = "智能色彩校正",
                    description = "HNCS自然色彩校正，还原真实色彩",
                    icon = Icons.Default.Palette,
                    enabled = colorCorrectionEnabled,
                    onToggle = { colorCorrectionEnabled = it },
                    strength = colorCorrectionStrength,
                    onStrengthChange = { colorCorrectionStrength = it },
                    color = HasselbladOrange,
                    isOptimized = optimizedOptions.contains("color"),
                    isProcessing = isOptimizing && optimizationCurrentName == "色彩校正"
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 重置按钮
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hdrEnabled = false; hdrStrength = 50f
                    noiseReductionEnabled = false; noiseReductionStrength = 30f
                    sharpenEnabled = true; sharpenStrength = 25f
                    exposureAuto = false; exposureAdjustment = 0f
                    colorCorrectionEnabled = true; colorCorrectionStrength = 20f
                    optimizedOptions.clear()
                    optimizedBitmap = null
                    previewMode = "before"
                    analysisResult = null
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置")
            }

            // 保存按钮
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    saveToGallery()
                },
                enabled = !isSaving && (optimizedBitmap != null || originalBitmap != null),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSaving) "保存中..." else "保存")
            }

            // 开始优化按钮
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    runOptimizeWorkflow()
                },
                enabled = !isOptimizing && selectedOptimizeIds.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isOptimizing) "优化中..." else "智能优化")
            }
        }
    }
}

// ==================== 辅助函数 ====================

/**
 * 降采样加载图片，防止大图 OOM
 * @param maxDimension 最大边长（像素）
 */
private fun loadSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        // 第一步：仅解码尺寸
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        // 计算降采样倍数
        val sampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension)
        // 第二步：降采样解码
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { s2 ->
            BitmapFactory.decodeStream(s2, null, decodeOptions)
        }
        // 应用 EXIF 方向旋转，确保图片显示正确朝向
        decoded?.let { applyExifOrientation(context, uri, it) }
    } catch (e: Exception) {
        android.util.Log.e("SmartOptimize", "loadSampledBitmap failed", e)
        null
    } catch (e: OutOfMemoryError) {
        android.util.Log.e("SmartOptimize", "loadSampledBitmap OOM", e)
        null
    }
}

/**
 * 计算 BitmapFactory 降采样倍数
 *
 * 修复：原实现使用 halfHeight/halfWidth 与 reqHeight/reqWidth 比较，
 * 导致当图片尺寸在 [reqDim, reqDim*2] 区间时 inSampleSize 仍为 1，
 * 全分辨率图片被加载到内存，后续图像处理时引发 OOM 闪退。
 *
 * 正确逻辑：持续将 inSampleSize 翻倍，直到解码后尺寸不超过目标尺寸。
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height <= 0 || width <= 0) return inSampleSize
    while (height / (inSampleSize * 2) >= reqHeight && width / (inSampleSize * 2) >= reqWidth) {
        inSampleSize *= 2
    }
    return inSampleSize
}

/**
 * 根据 EXIF 方向标签旋转 Bitmap，确保图片朝向正确
 */
private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = android.media.ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    } catch (e: Exception) {
        bitmap
    }
}

/**
 * AI 场景识别：分析图片并返回结果
 */
private fun analyzeImage(
    bitmap: Bitmap,
    engine: MasterInferenceEngine,
    scope: CoroutineScope,
    onResult: (SceneProfile) -> Unit
) {
    scope.launch(Dispatchers.Default) {
        try {
            val result = engine.analyzeImage(bitmap)
            withContext(Dispatchers.Main) { onResult(result) }
        } catch (e: Exception) {
            android.util.Log.e("SmartOptimize", "AI analysis failed", e)
        }
    }
}

/**
 * 根据 AI 场景识别结果自动推荐优化项与强度
 */
private fun applyAutoRecommendations(
    result: SceneProfile,
    apply: (hdr: Boolean, denoise: Boolean, sharpen: Boolean, exposure: Boolean, color: Boolean,
            hdrS: Float, denoiseS: Float, sharpenS: Float, exposureS: Float, colorS: Float) -> Unit
) {
    val category = result.category
    when (category) {
        SceneCategory.PORTRAIT -> apply(true, false, true, true, true, 30f, 0f, 20f, 10f, 40f)
        SceneCategory.LANDSCAPE -> apply(true, false, true, false, true, 60f, 0f, 30f, 0f, 35f)
        SceneCategory.NIGHT -> apply(true, true, false, true, false, 40f, 50f, 0f, 30f, 0f)
        SceneCategory.FOOD -> apply(false, false, true, true, true, 0f, 0f, 15f, 15f, 50f)
        SceneCategory.URBAN -> apply(true, false, true, false, true, 40f, 0f, 35f, 0f, 25f)
        SceneCategory.STILL_LIFE -> apply(false, false, true, true, true, 0f, 0f, 25f, 10f, 30f)
        SceneCategory.MACRO -> apply(false, true, true, false, true, 0f, 20f, 40f, 0f, 20f)
        SceneCategory.EVENT -> apply(true, false, true, true, true, 35f, 0f, 20f, 20f, 30f)
        else -> apply(true, false, true, false, true, 50f, 0f, 25f, 0f, 20f)
    }
}

/**
 * 保存 Bitmap 到系统相册
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, tag: String): Uri? {
    return try {
        val filename = "${tag}_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/OMaster/SmartOptimize"
                )
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )
        uri?.also {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            MediaScannerConnection.scanFile(context, arrayOf(it.toString()), arrayOf("image/jpeg"), null)
        }
    } catch (e: Exception) {
        android.util.Log.e("SmartOptimize", "Save to gallery failed", e)
        null
    }
}

/**
 * 保存 Bitmap 到缓存目录（用于分享）
 */
private fun saveBitmapToCache(context: Context, bitmap: Bitmap, filename: String): Uri? {
    return try {
        val cacheDir = java.io.File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val file = java.io.File(cacheDir, filename)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        android.util.Log.e("SmartOptimize", "Save to cache failed", e)
        null
    }
}

/**
 * 前后拖拽对比视图
 */
@Composable
private fun BeforeAfterCompareView(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var dividerOffset by remember { mutableFloatStateOf(0.5f) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    Box(modifier = modifier) {
        // 优化后（底层）
        Image(
            bitmap = afterBitmap.asImageBitmap(),
            contentDescription = "优化后",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 原图（上层，通过 clip 裁剪）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val delta = dragAmount / size.width
                        dividerOffset = (dividerOffset + delta).coerceIn(0f, 1f)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dividerOffset)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(0.dp))
            ) {
                Image(
                    bitmap = beforeBitmap.asImageBitmap(),
                    contentDescription = "原图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // 分割线
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (dividerOffset * screenWidthDp).dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
            // 标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("原图", color = Color.White, fontSize = 11.sp,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
                Text("优化后", color = HasselbladOrange, fontSize = 11.sp,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp))
            }
        }
    }
}

/**
 * 综合优化卡片（对齐 Web 端"综合优化"选项）
 * 一键启用所有优化项并触发顺序处理流程
 */
@Composable
private fun CompositeOptimizeCard(
    enabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onStartOptimize: () -> Unit,
    isOptimizing: Boolean,
    selectedCount: Int
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) SuccessGreen.copy(alpha = 0.15f) else SurfaceElevated
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (enabled) SuccessGreen.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        null,
                        tint = if (enabled) SuccessGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "综合优化",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) SuccessGreen else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "一键优化全部参数 · 已选 $selectedCount 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (it) onEnable() else onDisable()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                        checkedTrackColor = SuccessGreen
                    )
                )
            }
            if (enabled && selectedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartOptimize()
                    },
                    enabled = !isOptimizing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SuccessGreen
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("一键顺序处理")
                }
            }
        }
    }
}

@Composable
private fun OptimizeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    strength: Float,
    strengthRange: ClosedFloatingPointRange<Float> = 0f..100f,
    onStrengthChange: (Float) -> Unit,
    color: Color,
    isOptimized: Boolean = false,
    isProcessing: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isProcessing -> color.copy(alpha = 0.15f)
                enabled -> color.copy(alpha = 0.1f)
                else -> SurfaceElevated
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        null,
                        tint = if (enabled || isProcessing) color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (enabled || isProcessing) color else MaterialTheme.colorScheme.onBackground
                            )
                            if (isProcessing) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = color
                                )
                            } else if (isOptimized) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                        checkedTrackColor = color
                    )
                )
            }

            // 强度滑块（仅启用时显示）
            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "强度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.width(50.dp)
                    )

                    Slider(
                        value = strength,
                        onValueChange = onStrengthChange,
                        valueRange = strengthRange,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color
                        )
                    )

                    Text(
                        text = "${strength.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        modifier = Modifier.width(50.dp)
                    )
                }
            }
        }
    }
}

/**
 * 优化参数数据类
 */
data class OptimizeParams(
    val hdrEnabled: Boolean = false,
    val hdrStrength: Float = 50f,
    val noiseReductionEnabled: Boolean = false,
    val noiseReductionStrength: Float = 30f,
    val sharpenEnabled: Boolean = true,
    val sharpenStrength: Float = 25f,
    val exposureAdjustment: Float = 0f,
    val colorCorrectionEnabled: Boolean = true,
    val colorCorrectionStrength: Float = 20f
)
