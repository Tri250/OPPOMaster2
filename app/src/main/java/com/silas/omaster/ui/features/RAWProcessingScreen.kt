package com.silas.omaster.ui.features

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.raw.RAWProcessingManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

/**
 * RAW 处理页面
 * 同步 Web 设计：专业 RAW 调节面板
 * 支持曝光、白平衡、色调、降噪等专业参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RAWProcessingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { RAWProcessingManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val state by manager.state.collectAsState()
    val params by manager.params.collectAsState()
    val rawInfo by manager.rawInfo.collectAsState()

    // RAW 文件选择器
    val rawPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                manager.loadRAW(it)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.raw_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.raw_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 选择 RAW 文件
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            rawPicker.launch(arrayOf(
                                "image/dng", "image/x-adobe-dng",
                                "image/cr2", "image/nef", "image/arw", "image/raf",
                                "image/orf", "image/rw2", "image/pef", "image/srw", "image/nrw"
                            ))
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = HasselbladOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.raw_select_file),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.raw_supported_formats),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // RAW 信息
            rawInfo?.let { info ->
                item {
                    RAWInfoCard(info = info)
                }
            }

            // 处理状态
            item {
                when (val s = state) {
                    is RAWProcessingManager.ProcessingState.Loading -> StatusCard("加载中…", HasselbladOrange)
                    is RAWProcessingManager.ProcessingState.Decoding -> StatusCard("解码中 ${(s.progress * 100).toInt()}%", HasselbladOrange)
                    is RAWProcessingManager.ProcessingState.Processing -> StatusCard("处理中: ${s.stage}", HasselbladOrange)
                    is RAWProcessingManager.ProcessingState.Error -> StatusCard(s.message, Color.Red)
                    else -> {}
                }
            }

            // 曝光调节
            item {
                Text(
                    text = stringResource(R.string.raw_exposure),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                ExposureCard(
                    value = params.exposure,
                    onValueChange = { manager.setParams(params.copy(exposure = it)) }
                )
            }

            // 白平衡
            item {
                Text(
                    text = stringResource(R.string.raw_white_balance),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                WhiteBalanceCard(
                    whiteBalance = params.whiteBalance,
                    temperature = params.temperature,
                    tint = params.tint,
                    onWhiteBalanceChange = { wb ->
                        manager.setParams(params.copy(
                            whiteBalance = wb,
                            temperature = wb.temperature,
                            tint = wb.tint
                        ))
                    },
                    onTemperatureChange = {
                        manager.setParams(params.copy(temperature = it))
                    },
                    onTintChange = {
                        manager.setParams(params.copy(tint = it))
                    }
                )
            }

            // 色调调节
            item {
                Text(
                    text = "色调",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                ToneAdjustCard(
                    highlights = params.highlights,
                    shadows = params.shadows,
                    whites = params.whites,
                    blacks = params.blacks,
                    onHighlightsChange = { manager.setParams(params.copy(highlights = it)) },
                    onShadowsChange = { manager.setParams(params.copy(shadows = it)) },
                    onWhitesChange = { manager.setParams(params.copy(whites = it)) },
                    onBlacksChange = { manager.setParams(params.copy(blacks = it)) }
                )
            }

            // 效果
            item {
                Text(
                    text = "效果",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                EffectsCard(
                    clarity = params.clarity,
                    vibrance = params.vibrance,
                    saturation = params.saturation,
                    sharpness = params.sharpness,
                    noiseReduction = params.noiseReduction,
                    onClarityChange = { manager.setParams(params.copy(clarity = it)) },
                    onVibranceChange = { manager.setParams(params.copy(vibrance = it)) },
                    onSaturationChange = { manager.setParams(params.copy(saturation = it)) },
                    onSharpnessChange = { manager.setParams(params.copy(sharpness = it)) },
                    onNoiseReductionChange = { manager.setParams(params.copy(noiseReduction = it)) }
                )
            }

            // 导出按钮
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            scope.launch {
                                manager.processRAW(params)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.raw_process), color = Color.White)
                    }
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            scope.launch {
                                manager.exportAsDNG(params)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = HasselbladOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.raw_export), color = HasselbladOrange)
                    }
                }
            }
        }
    }
}

@Composable
private fun RAWInfoCard(info: RAWProcessingManager.RAWInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RAW 信息",
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(R.string.raw_format, info.width.toString() + "x" + info.height.toString()))
            info.iso?.let { InfoRow("ISO: $it") }
            info.shutterSpeed?.let { InfoRow("快门: $it") }
            info.aperture?.let { InfoRow("光圈: $it") }
            info.cameraModel?.let { InfoRow("相机: $it") }
            info.lensModel?.let { InfoRow("镜头: $it") }
        }
    }
}

@Composable
private fun InfoRow(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun StatusCard(text: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExposureCard(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.raw_exposure),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = String.format("%+.1f EV", value),
                    color = HasselbladOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = -3f..3f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun WhiteBalanceCard(
    whiteBalance: RAWProcessingManager.WhiteBalance,
    temperature: Int,
    tint: Int,
    onWhiteBalanceChange: (RAWProcessingManager.WhiteBalance) -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onTintChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 白平衡预设
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(RAWProcessingManager.WhiteBalance.entries) { wb ->
                    FilterChip(
                        selected = whiteBalance == wb,
                        onClick = {
                            onWhiteBalanceChange(wb)
                            onTemperatureChange(wb.temperature)
                            onTintChange(wb.tint)
                        },
                        label = { Text(wb.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                            selectedLabelColor = HasselbladOrange
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 色温
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.raw_temperature), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("${temperature}K", color = HasselbladOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = temperature.toFloat(),
                onValueChange = { onTemperatureChange(it.toInt()) },
                valueRange = 2000f..10000f,
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )
            // 色调
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.raw_tint), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(tint.toString(), color = HasselbladOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = tint.toFloat(),
                onValueChange = { onTintChange(it.toInt()) },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )
        }
    }
}

@Composable
private fun ToneAdjustCard(
    highlights: Int,
    shadows: Int,
    whites: Int,
    blacks: Int,
    onHighlightsChange: (Int) -> Unit,
    onShadowsChange: (Int) -> Unit,
    onWhitesChange: (Int) -> Unit,
    onBlacksChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ParamSlider(stringResource(R.string.raw_highlights), highlights.toFloat(), onValueChange = { onHighlightsChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_shadows), shadows.toFloat(), onValueChange = { onShadowsChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_whites), whites.toFloat(), onValueChange = { onWhitesChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_blacks), blacks.toFloat(), onValueChange = { onBlacksChange(it.toInt()) })
        }
    }
}

@Composable
private fun EffectsCard(
    clarity: Int,
    vibrance: Int,
    saturation: Int,
    sharpness: Int,
    noiseReduction: RAWProcessingManager.NoiseReduction,
    onClarityChange: (Int) -> Unit,
    onVibranceChange: (Int) -> Unit,
    onSaturationChange: (Int) -> Unit,
    onSharpnessChange: (Int) -> Unit,
    onNoiseReductionChange: (RAWProcessingManager.NoiseReduction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ParamSlider(stringResource(R.string.raw_clarity), clarity.toFloat(), valueRange = 0f..100f, onValueChange = { onClarityChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_vibrance), vibrance.toFloat(), onValueChange = { onVibranceChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_saturation_raw), saturation.toFloat(), onValueChange = { onSaturationChange(it.toInt()) })
            ParamSlider(stringResource(R.string.raw_sharpness_raw), sharpness.toFloat(), valueRange = 0f..100f, onValueChange = { onSharpnessChange(it.toInt()) })
            Spacer(modifier = Modifier.height(8.dp))
            // 降噪模式
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(RAWProcessingManager.NoiseReduction.entries) { nr ->
                    FilterChip(
                        selected = noiseReduction == nr,
                        onClick = { onNoiseReductionChange(nr) },
                        label = { Text(nr.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                            selectedLabelColor = HasselbladOrange
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = value.toInt().toString(),
                color = HasselbladOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
