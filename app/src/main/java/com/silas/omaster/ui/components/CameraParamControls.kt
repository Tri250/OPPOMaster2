package com.silas.omaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.model.CameraParams
import com.silas.omaster.model.ColorStyle
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlin.math.roundToInt

/**
 * 相机参数控制组件
 * 包含ISO、快门速度、白平衡、曝光补偿等参数的调整
 */
@Composable
fun CameraParamControls(
    params: CameraParams,
    onParamsChanged: (CameraParams) -> Unit,
    modifier: Modifier = Modifier,
    isManualMode: Boolean = true
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ISO控制
        IsoControl(
            iso = params.iso,
            onIsoChanged = { newIso ->
                onParamsChanged(params.copy(iso = newIso))
            },
            enabled = isManualMode
        )

        // 快门速度控制
        ShutterSpeedControl(
            shutterSpeed = params.shutter,
            onShutterSpeedChanged = { newShutter ->
                onParamsChanged(params.copy(shutter = newShutter))
            },
            enabled = isManualMode
        )

        // 白平衡控制
        WhiteBalanceControl(
            whiteBalance = params.wb,
            colorTemperature = params.colorTemperature,
            onWhiteBalanceChanged = { wb, temp ->
                onParamsChanged(params.copy(wb = wb, colorTemperature = temp))
            },
            enabled = isManualMode
        )

        // 曝光补偿控制
        ExposureControl(
            exposure = params.ev,
            onExposureChanged = { newEv ->
                onParamsChanged(params.copy(ev = newEv))
            }
        )

        HorizontalDivider()

        // 其他选项
        AdditionalOptions(
            params = params,
            onParamsChanged = onParamsChanged
        )
    }
}

/**
 * ISO控制组件
 */
@Composable
fun IsoControl(
    iso: Int,
    onIsoChanged: (Int) -> Unit,
    enabled: Boolean = true
) {
    val isoOptions = remember {
        listOf(50, 100, 200, 400, 800, 1600, 3200, 6400, 12800)
    }
    var expanded by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(iso.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "ISO 感光度",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ISO 感光度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    Text(
                        text = iso.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = enabled) {
                            expanded = true
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        isoOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toString()) },
                                onClick = {
                                    onIsoChanged(option)
                                    sliderValue = option.toFloat()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = {
                    if (enabled) {
                        val newIso = it.roundToInt().coerceIn(50, 12800)
                        sliderValue = newIso.toFloat()
                        onIsoChanged(newIso)
                    }
                },
                valueRange = 50f..12800f,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    activeTrackColor = HasselbladOrange,
                    thumbColor = HasselbladOrange
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "50",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "12800",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 快门速度控制组件
 */
@Composable
fun ShutterSpeedControl(
    shutterSpeed: String,
    onShutterSpeedChanged: (String) -> Unit,
    enabled: Boolean = true
) {
    val shutterOptions = remember {
        listOf(
            "1/8000", "1/4000", "1/2000", "1/1000", "1/500",
            "1/250", "1/125", "1/60", "1/30", "1/15",
            "1/8", "1/4", "1/2", "1", "2", "4", "8", "15", "30"
        )
    }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "快门速度",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "快门速度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    Text(
                        text = if (shutterSpeed.contains('/')) shutterSpeed else "${shutterSpeed}s",
                        style = MaterialTheme.typography.headlineSmall,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = enabled) {
                            expanded = true
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        shutterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (option.contains('/')) option else "${option}s"
                                    )
                                },
                                onClick = {
                                    onShutterSpeedChanged(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isLongExposure = try {
                val parts = shutterSpeed.split('/')
                parts.size == 1 && shutterSpeed.toIntOrNull()?.let { it >= 1 } ?: false
            } catch (e: Exception) {
                false
            }

            AnimatedVisibility(
                visible = isLongExposure,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            HasselbladOrange.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚠️ 长曝光拍摄，请保持设备稳定",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
        }
    }
}

/**
 * 白平衡控制组件
 */
@Composable
fun WhiteBalanceControl(
    whiteBalance: String,
    colorTemperature: Int,
    onWhiteBalanceChanged: (String, Int) -> Unit,
    enabled: Boolean = true
) {
    val wbPresets = remember {
        listOf(
            "Auto" to 5500,
            "日光" to 5500,
            "阴天" to 6500,
            "荧光灯" to 4000,
            "白炽灯" to 3200
        )
    }
    var selectedPreset by remember { mutableStateOf(whiteBalance) }
    var showCustomTemp by remember { mutableStateOf(whiteBalance == "Custom") }
    var tempSliderValue by remember { mutableFloatStateOf(colorTemperature.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "白平衡",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "白平衡",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wbPresets.forEach { (preset, temp) ->
                    val isSelected = selectedPreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) HasselbladOrange
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            .clickable(enabled = enabled) {
                                selectedPreset = preset
                                showCustomTemp = false
                                onWhiteBalanceChanged(preset, temp)
                                tempSliderValue = temp.toFloat()
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 自定义色温
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        showCustomTemp = !showCustomTemp
                        if (showCustomTemp) {
                            selectedPreset = "Custom"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCustomTemp) HasselbladOrange
                        else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("自定义色温")
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (showCustomTemp) {
                    Text(
                        text = "${tempSliderValue.roundToInt()}K",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showCustomTemp,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = tempSliderValue,
                        onValueChange = {
                            if (enabled) {
                                tempSliderValue = it
                                val temp = it.roundToInt().coerceIn(2000, 10000)
                                onWhiteBalanceChanged("Custom", temp)
                            }
                        },
                        valueRange = 2000f..10000f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            activeTrackColor = HasselbladOrange,
                            thumbColor = HasselbladOrange
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "2000K (暖)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "10000K (冷)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 曝光补偿控制组件
 */
@Composable
fun ExposureControl(
    exposure: String,
    onExposureChanged: (String) -> Unit
) {
    val evValue = remember(exposure) {
        exposure.replace("+", "").replace("-", "").toFloatOrNull() ?: 0f
    }
    var sliderValue by remember { mutableFloatStateOf(evValue) }
    var isNegative by remember { mutableStateOf(exposure.startsWith("-")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "曝光补偿",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "曝光补偿",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isNegative) "-" else "+",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            isNegative = !isNegative
                            val sign = if (isNegative) "-" else "+"
                            val value = String.format("%.1f", sliderValue)
                            val newEv = if (value == "0.0") "0" else "$sign$value"
                            onExposureChanged(newEv)
                        }
                    )
                    Text(
                        text = "${String.format("%.1f", sliderValue)} EV",
                        style = MaterialTheme.typography.headlineSmall,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    val sign = if (isNegative) "-" else "+"
                    val value = String.format("%.1f", it)
                    val newEv = if (value == "0.0") "0" else "$sign$value"
                    onExposureChanged(newEv)
                },
                valueRange = 0f..3f,
                steps = 29,
                colors = SliderDefaults.colors(
                    activeTrackColor = HasselbladOrange,
                    thumbColor = HasselbladOrange
                )
            )
        }
    }
}

/**
 * 附加选项组件
 */
@Composable
fun AdditionalOptions(
    params: CameraParams,
    onParamsChanged: (CameraParams) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "更多选项",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // HDR开关
            SettingSwitch(
                title = "HDR",
                description = "提高动态范围",
                checked = params.hdr,
                onCheckedChange = {
                    onParamsChanged(params.copy(hdr = it))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 夜景模式
            SettingSwitch(
                title = "夜景模式",
                description = "哈苏夜景算法优化",
                checked = params.nightMode,
                onCheckedChange = {
                    onParamsChanged(params.copy(nightMode = it))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // AI优化
            SettingSwitch(
                title = "AI 影像引擎",
                description = "智能场景识别与优化",
                checked = params.aiOptimization,
                onCheckedChange = {
                    onParamsChanged(params.copy(aiOptimization = it))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // RAW格式
            SettingSwitch(
                title = "RAW 格式",
                description = "保存无损原始影像数据",
                checked = params.rawCapture,
                onCheckedChange = {
                    onParamsChanged(params.copy(rawCapture = it))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 色彩风格选择
            Text(
                text = "色彩风格",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ColorStyle.Natural,
                    ColorStyle.Vivid,
                    ColorStyle.Cinematic,
                    ColorStyle.BlackWhite
                ).forEach { style ->
                    val isSelected = params.colorStyle == style.name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = if (isSelected) HasselbladOrange
                                else Color.Transparent
                            )
                            .background(
                                if (isSelected) HasselbladOrange.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                            .clickable {
                                onParamsChanged(
                                    params.copy(
                                        colorStyle = style.name,
                                        colorProfile = style.displayName
                                    )
                                )
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置开关组件
 */
@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HasselbladOrange,
                checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f)
            )
        )
    }
}