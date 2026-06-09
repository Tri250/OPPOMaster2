package com.silas.omaster.ui.features

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*

/**
 * 参数精细调节页面
 * 
 * 功能：
 * - ISO 调节
 * - 快门速度调节
 * - 光圈调节
 * - 白平衡调节
 * - 焦距调节
 * - 曝光补偿
 * 
 * 对齐 Web 端 ParamAdjustPage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamAdjustScreen(
    onBack: () -> Unit,
    onApply: (CameraParams) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // 相机参数状态
    var iso by remember { mutableIntStateOf(100) }
    var shutterSpeed by remember { mutableFloatStateOf(125f) }
    var aperture by remember { mutableFloatStateOf(2.8f) }
    var whiteBalance by remember { mutableIntStateOf(5500) }
    var focalLength by remember { mutableIntStateOf(23) }
    var exposureCompensation by remember { mutableFloatStateOf(0f) }
    
    // 预设模式
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    
    // ISO 选项
    val isoOptions = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)
    
    // 快门速度选项
    val shutterOptions = listOf("1/4000", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1s", "2s", "4s", "8s", "15s", "30s")
    
    // 光圈选项
    val apertureOptions = listOf(1.4f, 1.8f, 2.0f, 2.8f, 4.0f, 5.6f, 8.0f, 11f, 16f, 22f)
    
    // 白平衡选项
    val wbOptions = listOf(2800, 3200, 4000, 5000, 5500, 6000, 6500, 7000, 8000, 9000)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("参数精细调节", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 重置按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    iso = 100
                    shutterSpeed = 125f
                    aperture = 2.8f
                    whiteBalance = 5500
                    focalLength = 23
                    exposureCompensation = 0f
                    selectedPreset = null
                }) {
                    Icon(Icons.Default.Refresh, "重置", tint = Color.White)
                }
                // 应用按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onApply(CameraParams(
                        iso = iso,
                        shutterSpeed = shutterSpeed,
                        aperture = aperture,
                        whiteBalance = whiteBalance,
                        focalLength = focalLength,
                        exposureCompensation = exposureCompensation
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

        // 快速预设
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ParamPresetChip(
                label = "人像",
                selected = selectedPreset == "portrait",
                onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    selectedPreset = "portrait"
                    iso = 100
                    shutterSpeed = 125f
                    aperture = 1.8f
                    whiteBalance = 5500
                }
            )
            ParamPresetChip(
                label = "风景",
                selected = selectedPreset == "landscape",
                onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    selectedPreset = "landscape"
                    iso = 100
                    shutterSpeed = 250f
                    aperture = 8.0f
                    whiteBalance = 5500
                }
            )
            ParamPresetChip(
                label = "夜景",
                selected = selectedPreset == "night",
                onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    selectedPreset = "night"
                    iso = 800
                    shutterSpeed = 30f
                    aperture = 2.8f
                    whiteBalance = 3200
                }
            )
            ParamPresetChip(
                label = "运动",
                selected = selectedPreset == "sports",
                onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    selectedPreset = "sports"
                    iso = 400
                    shutterSpeed = 1000f
                    aperture = 4.0f
                    whiteBalance = 5500
                }
            )
        }

        // 参数调节列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ISO 调节
            item {
                ParamSliderCard(
                    title = "ISO",
                    subtitle = "感光度，影响噪点和曝光",
                    value = iso,
                    valueRange = 50..6400,
                    options = isoOptions,
                    onValueChange = { iso = it },
                    unit = "",
                    color = Color(0xFF9C27B0)
                )
            }

            // 快门速度调节
            item {
                ParamSliderCard(
                    title = "快门",
                    subtitle = "曝光时间，影响运动模糊",
                    value = shutterSpeed.toInt(),
                    valueRange = 1..30000,
                    displayValue = formatShutterSpeed(shutterSpeed),
                    onValueChange = { shutterSpeed = it.toFloat() },
                    unit = "",
                    color = Color(0xFF2196F3)
                )
            }

            // 光圈调节
            item {
                ParamSliderCard(
                    title = "光圈",
                    subtitle = "景深控制，影响背景虚化",
                    value = aperture,
                    valueRange = 1.4f..22f,
                    options = apertureOptions.map { it.toString() },
                    onValueChange = { aperture = it },
                    unit = "f/",
                    color = HasselbladOrange
                )
            }

            // 白平衡调节
            item {
                ParamSliderCard(
                    title = "白平衡",
                    subtitle = "色温调节，影响色彩冷暖",
                    value = whiteBalance,
                    valueRange = 2800..9000,
                    options = wbOptions.map { "${it}K" },
                    onValueChange = { whiteBalance = it },
                    unit = "K",
                    color = Color(0xFFFFEB3B)
                )
            }

            // 焦距调节
            item {
                ParamSliderCard(
                    title = "焦距",
                    subtitle = "镜头焦距，影响视角和压缩感",
                    value = focalLength,
                    valueRange = 8..400,
                    onValueChange = { focalLength = it },
                    unit = "mm",
                    color = Color(0xFFE91E63)
                )
            }

            // 曝光补偿
            item {
                ParamSliderCard(
                    title = "曝光补偿",
                    subtitle = "手动调整曝光量",
                    value = exposureCompensation,
                    valueRange = -3f..3f,
                    onValueChange = { exposureCompensation = it },
                    unit = "EV",
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // 当前参数摘要
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ParamSummaryItem("ISO", iso.toString())
                ParamSummaryItem("快门", formatShutterSpeed(shutterSpeed))
                ParamSummaryItem("光圈", "f/$aperture")
                ParamSummaryItem("WB", "${whiteBalance}K")
            }
        }
    }
}

@Composable
private fun ParamPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HasselbladOrange else Color(0xFF2A2A2A))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ParamSliderCard(
    title: String,
    subtitle: String,
    value: Any,
    valueRange: Any,
    displayValue: String? = null,
    options: List<String>? = null,
    onValueChange: (Any) -> Unit,
    unit: String,
    color: Color
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = displayValue ?: when (value) {
                        is Int -> "$value$unit"
                        is Float -> "${if (value == value.toInt().toFloat()) value.toInt() else value}$unit"
                        else -> "$value$unit"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 滑块
            when (value) {
                is Int -> {
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { 
                            haptic.perform(HapticFeedbackType.TextHandleMove)
                            onValueChange(it.toInt()) 
                        },
                        valueRange = when (valueRange) {
                            is IntRange -> valueRange.first.toFloat()..valueRange.last.toFloat()
                            else -> 0f..100f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color
                        )
                    )
                }
                is Float -> {
                    Slider(
                        value = value,
                        onValueChange = { 
                            haptic.perform(HapticFeedbackType.TextHandleMove)
                            onValueChange(it) 
                        },
                        valueRange = when (valueRange) {
                            is ClosedFloatingPointRange<Float> -> valueRange
                            else -> 0f..100f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color
                        )
                    )
                }
            }

            // 快速选项（如果有）
            if (options != null && options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.take(6).forEach { option ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A1A1A))
                                .clickable {
                                    haptic.perform(HapticFeedbackType.Select)
                                    // 解析选项值
                                    val parsedValue = when (value) {
                                        is Int -> option.filter { it.isDigit() }.toIntOrNull() ?: value
                                        is Float -> option.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: value
                                        else -> value
                                    }
                                    onValueChange(parsedValue)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HasselbladOrange
        )
    }
}

/**
 * 格式化快门速度
 */
private fun formatShutterSpeed(speed: Float): String {
    return when {
        speed >= 1000 -> "1/${(speed / 1000).toInt()}s"
        speed >= 1 -> "${speed.toInt()}s"
        else -> "1/${speed.toInt()}s"
    }
}

/**
 * 相机参数数据类
 */
data class CameraParams(
    val iso: Int = 100,
    val shutterSpeed: Float = 125f,
    val aperture: Float = 2.8f,
    val whiteBalance: Int = 5500,
    val focalLength: Int = 23,
    val exposureCompensation: Float = 0f
)