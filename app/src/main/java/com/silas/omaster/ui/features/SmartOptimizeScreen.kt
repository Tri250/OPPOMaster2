package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.SurfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 智能优化页面
 *
 * 功能：
 * - HDR 增强
 * - 降噪处理
 * - 锐化优化
 * - 自动曝光调整
 * - 智能色彩校正
 * - 综合优化（一键全部 + 进度流程）
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

    // 预览图片状态
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 优化参数状态
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

    // 预览模式
    var previewMode by remember { mutableStateOf("before") }

    // 优化进度
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationStep by remember { mutableStateOf(0) }
    var optimizationTotalSteps by remember { mutableStateOf(0) }
    var optimizationProgress by remember { mutableFloatStateOf(0f) }
    var optimizationCurrentName by remember { mutableStateOf("") }
    val optimizedOptions = remember { mutableListOf<String>() }

    // 从 assets 加载示例预览图
    LaunchedEffect(Unit) {
        try {
            context.assets.open("images/placeholder.webp").use { stream ->
                previewBitmap = BitmapFactory.decodeStream(stream)
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

    val optimizeIdToName = mapOf(
        "hdr" to "HDR增强",
        "denoise" to "智能降噪",
        "sharpen" to "锐化增强",
        "exposure" to "自动曝光",
        "color" to "色彩校正"
    )

    // 顺序执行优化流程（对齐Web端handleOptimize + processStep）
    // 使用 AI 推理引擎执行真实优化处理
    fun runOptimizeWorkflow() {
        if (selectedOptimizeIds.isEmpty()) return
        scope.launch {
            isOptimizing = true
            optimizedOptions.clear()
            optimizationTotalSteps = selectedOptimizeIds.size
            optimizationStep = 0
            optimizationProgress = 0f

            try {
                for ((index, id) in selectedOptimizeIds.withIndex()) {
                    optimizationStep = index + 1
                    optimizationCurrentName = optimizeIdToName[id] ?: id
                    optimizationProgress = (index.toFloat()) / selectedOptimizeIds.size

                    // 调用 AI 推理引擎执行真实优化处理
                    val bitmap = previewBitmap
                    if (bitmap != null) {
                        withContext(Dispatchers.Default) {
                            inferenceEngine.applyOptimization(bitmap, id)
                        }
                    }

                    optimizationProgress = (index + 1).toFloat() / selectedOptimizeIds.size
                    optimizedOptions.add(id)
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartOptimize", "Optimization failed", e)
                return@launch
            } finally {
                isOptimizing = false
            }
            // 完成 - 应用参数
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
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TopAppBar(
            title = { Text("智能优化", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 预览切换
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    previewMode = if (previewMode == "before") "after" else "before"
                }) {
                    Icon(
                        if (previewMode == "after") Icons.Default.Visibility else Icons.Default.Compare,
                        "预览",
                        tint = if (previewMode == "after") HasselbladOrange else Color.White
                    )
                }
                // 应用按钮
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        runOptimizeWorkflow()
                    },
                    enabled = !isOptimizing
                ) {
                    Icon(Icons.Default.Check, "应用", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // 优化进度条（与Web端isOptimizing + processStep对齐）
        AnimatedVisibility(visible = isOptimizing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
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
                            color = Color.White
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
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(DarkGray)
        ) {
            previewBitmap?.let { bitmap ->
                // 显示预览图片（带优化后效果模拟滤镜）
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = if (previewMode == "after") "优化后预览" else "原图预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (previewMode == "after") {
                        // 模拟优化后效果：轻微提亮+暖色调
                        ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    1.05f, 0f, 0f, 0f, 10f,
                                    0f, 1.02f, 0f, 0f, 6f,
                                    0f, 0f, 0.98f, 0f, -2f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    } else null
                )

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
                        text = if (previewMode == "after") "优化后" else "原图",
                        color = if (previewMode == "after") HasselbladOrange else Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } ?: run {
                // 图片未加载时显示占位提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (previewMode == "after") "优化后预览" else "原图预览",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
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
            // 已完成的优化项（与Web端optimizedOptions对齐）
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
                                    Icons.Default.CheckCircle,
                                    null,
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
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // 综合优化（对齐Web端'id: enhance'选项）
            item {
                CompositeOptimizeCard(
                    enabled = hdrEnabled && noiseReductionEnabled && sharpenEnabled && colorCorrectionEnabled,
                    onEnable = {
                        // 一键启用所有选项
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
                    description = "AI识别并消除噪点",
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
                    description = "提升画面清晰度和质感",
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
        }

        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 重置按钮
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    hdrEnabled = false
                    hdrStrength = 50f
                    noiseReductionEnabled = false
                    noiseReductionStrength = 30f
                    sharpenEnabled = true
                    sharpenStrength = 25f
                    exposureAuto = false
                    exposureAdjustment = 0f
                    colorCorrectionEnabled = true
                    colorCorrectionStrength = 20f
                    optimizedOptions.clear()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置")
            }

            // 开始优化按钮（顺序处理所选项）
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
                Text(if (isOptimizing) "优化中..." else "开始智能优化")
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
                            else Color.White.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        null,
                        tint = if (enabled) SuccessGreen else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "综合优化",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) SuccessGreen else Color.White
                    )
                    Text(
                        text = "一键优化全部参数 · 已选 $selectedCount 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (it) onEnable() else onDisable()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
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
                        tint = if (enabled || isProcessing) color else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (enabled || isProcessing) color else Color.White
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
                            color = Color.White.copy(alpha = 0.6f)
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
                        checkedThumbColor = Color.White,
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
                        color = Color.White.copy(alpha = 0.7f),
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
