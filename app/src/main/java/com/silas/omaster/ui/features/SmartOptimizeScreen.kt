package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*
import com.silas.omaster.util.HapticFeedbackTypeCompat
import com.silas.omaster.util.perform
import java.io.IOException

/**
 * 智能优化页面
 * 
 * 功能：
 * - HDR 增强
 * - 降噪处理
 * - 锐化优化
 * - 自动曝光调整
 * - 智能色彩校正
 * 
 * 对齐 Web 端 SmartOptimizePage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOptimizeScreen(
    onBack: () -> Unit,
    onApply: (OptimizeParams) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("智能优化", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 预览切换
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Select)
                    previewMode = if (previewMode == "before") "after" else "before"
                }) {
                    Icon(
                        if (previewMode == "after") Icons.Default.Visibility else Icons.Default.Compare,
                        "预览",
                        tint = if (previewMode == "after") HasselbladOrange else Color.White
                    )
                }
                // 应用按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
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
                }) {
                    Icon(Icons.Default.Check, "应用", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // 预览区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF1A1A1A))
        ) {
            if (previewBitmap != null) {
                val bitmap = previewBitmap!!
                // 显示预览图片（带优化后效果模拟滤镜）
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = if (previewMode == "after") "优化后预览" else "原图预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (previewMode == "after") {
                        // 模拟优化后效果：轻微提亮+暖色调
                        ColorFilter.colorMatrix(
                            androidx.compose.ui.graphics.ColorMatrix(
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (previewMode == "after") "优化后" else "原图",
                        color = if (previewMode == "after") HasselbladOrange else Color.White,
                        fontSize = 11.sp
                    )
                }
            } else {
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
            // HDR 增强
            item {
                OptimizeOptionCard(
                    title = "HDR 增强",
                    description = "扩展动态范围，增强明暗细节",
                    icon = Icons.Default.AutoFixHigh,
                    enabled = hdrEnabled,
                    onToggle = { hdrEnabled = it },
                    strength = hdrStrength,
                    onStrengthChange = { hdrStrength = it },
                    color = HasselbladOrange
                )
            }

            // 降噪处理
            item {
                OptimizeOptionCard(
                    title = "降噪处理",
                    description = "智能降噪，保留细节纹理",
                    icon = Icons.Default.FilterAlt,
                    enabled = noiseReductionEnabled,
                    onToggle = { noiseReductionEnabled = it },
                    strength = noiseReductionStrength,
                    onStrengthChange = { noiseReductionStrength = it },
                    color = Color(0xFF2196F3)
                )
            }

            // 锐化优化
            item {
                OptimizeOptionCard(
                    title = "锐化优化",
                    description = "增强边缘清晰度，提升质感",
                    icon = Icons.Default.Tune,
                    enabled = sharpenEnabled,
                    onToggle = { sharpenEnabled = it },
                    strength = sharpenStrength,
                    onStrengthChange = { sharpenStrength = it },
                    color = Color(0xFFE91E63)
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
                    color = Color(0xFFFFEB3B)
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
                    color = HasselbladOrange
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
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
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
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重置")
            }

            // 一键优化按钮
            Button(
                onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
                    // 启用所有优化
                    hdrEnabled = true
                    noiseReductionEnabled = true
                    sharpenEnabled = true
                    colorCorrectionEnabled = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("一键优化")
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
    color: Color
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.1f) else Color(0xFF2A2A2A)
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
                        tint = if (enabled) color else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) color else Color.White
                        )
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
                        haptic.perform(HapticFeedbackTypeCompat.Confirm)
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