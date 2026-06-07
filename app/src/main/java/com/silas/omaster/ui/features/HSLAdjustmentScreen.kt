package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.silas.omaster.param.HSLAdjustmentManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform

/**
 * HSL 调节页面
 * 同步 Web 设计：8 色独立调节面板
 * 支持色相/饱和度/明度三轴调节
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HSLAdjustmentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { HSLAdjustmentManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    val adjustments by manager.hslAdjustments.collectAsState()
    val selectedChannel by manager.selectedChannel.collectAsState()

    // 当前选中通道的调整值
    val currentAdjustment = adjustments[selectedChannel] ?: HSLAdjustmentManager.HSLAdjustment()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.hsl_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.hsl_subtitle),
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
            actions = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    manager.resetAll()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.hsl_reset_all), tint = HasselbladOrange)
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
            // 提示
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Text(
                        text = stringResource(R.string.hsl_adjust_hint),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // 颜色通道选择
            item {
                Text(
                    text = stringResource(R.string.hsl_color_wheel),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(HSLAdjustmentManager.ColorChannel.entries) { channel ->
                        ChannelChip(
                            channel = channel,
                            isSelected = channel == selectedChannel,
                            hasAdjustment = !(adjustments[channel]?.isZero() ?: true),
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                manager.selectChannel(channel)
                            }
                        )
                    }
                }
            }

            // 当前通道滑块
            item {
                ChannelAdjustmentCard(
                    channel = selectedChannel,
                    adjustment = currentAdjustment,
                    onHueChange = { manager.adjustHue(selectedChannel, it.toInt()) },
                    onSaturationChange = { manager.adjustSaturation(selectedChannel, it.toInt()) },
                    onLightnessChange = { manager.adjustLightness(selectedChannel, it.toInt()) },
                    onReset = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        manager.resetChannel(selectedChannel)
                    }
                )
            }

            // HSL 预设
            item {
                Text(
                    text = stringResource(R.string.curve_presets),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(manager.hslPresets) { preset ->
                        PresetChip(
                            name = preset.name,
                            description = preset.description,
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                manager.applyPreset(preset)
                            }
                        )
                    }
                }
            }

            // 操作按钮
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            manager.resetAll()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hsl_reset_all))
                    }
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            // 应用调节的回调点
                            onBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Text(stringResource(R.string.hsl_apply), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(
    channel: HSLAdjustmentManager.ColorChannel,
    isSelected: Boolean,
    hasAdjustment: Boolean,
    onClick: () -> Unit
) {
    val channelColor = getChannelColor(channel)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) HasselbladOrange else channelColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = channel.icon,
                fontSize = 18.sp
            )
            Text(
                text = channel.displayName,
                fontSize = 9.sp,
                color = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (hasAdjustment) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange)
            )
        }
    }
}

@Composable
private fun ChannelAdjustmentCard(
    channel: HSLAdjustmentManager.ColorChannel,
    adjustment: HSLAdjustmentManager.HSLAdjustment,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onLightnessChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val channelColor = getChannelColor(channel)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 通道头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(channelColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = channel.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                OutlinedButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hsl_reset),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 色相滑块
            HSLSliderRow(
                label = stringResource(R.string.hsl_hue),
                value = adjustment.hue.toFloat(),
                valueRange = HSLAdjustmentManager.HUE_MIN.toFloat()..HSLAdjustmentManager.HUE_MAX.toFloat(),
                centerValue = 0f,
                color = channelColor,
                onValueChange = onHueChange
            )

            // 饱和度滑块
            HSLSliderRow(
                label = stringResource(R.string.hsl_saturation),
                value = adjustment.saturation.toFloat(),
                valueRange = HSLAdjustmentManager.SATURATION_MIN.toFloat()..HSLAdjustmentManager.SATURATION_MAX.toFloat(),
                centerValue = 0f,
                color = channelColor,
                onValueChange = onSaturationChange
            )

            // 明度滑块
            HSLSliderRow(
                label = stringResource(R.string.hsl_lightness),
                value = adjustment.lightness.toFloat(),
                valueRange = HSLAdjustmentManager.LIGHTNESS_MIN.toFloat()..HSLAdjustmentManager.LIGHTNESS_MAX.toFloat(),
                centerValue = 0f,
                color = channelColor,
                onValueChange = onLightnessChange
            )
        }
    }
}

@Composable
private fun HSLSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    centerValue: Float,
    color: Color,
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
                fontSize = 13.sp
            )
            Text(
                text = "${value.toInt()}",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun PresetChip(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = name,
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 2
            )
        }
    }
}

private fun getChannelColor(channel: HSLAdjustmentManager.ColorChannel): Color {
    return when (channel) {
        HSLAdjustmentManager.ColorChannel.RED -> Color(0xFFE53935)
        HSLAdjustmentManager.ColorChannel.ORANGE -> Color(0xFFFB8C00)
        HSLAdjustmentManager.ColorChannel.YELLOW -> Color(0xFFFDD835)
        HSLAdjustmentManager.ColorChannel.GREEN -> Color(0xFF43A047)
        HSLAdjustmentManager.ColorChannel.CYAN -> Color(0xFF00ACC1)
        HSLAdjustmentManager.ColorChannel.BLUE -> Color(0xFF1E88E5)
        HSLAdjustmentManager.ColorChannel.PURPLE -> Color(0xFF8E24AA)
        HSLAdjustmentManager.ColorChannel.MAGENTA -> Color(0xFFD81B60)
    }
}
