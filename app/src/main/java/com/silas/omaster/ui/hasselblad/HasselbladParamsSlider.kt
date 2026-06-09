package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.model.HasselbladParams
import com.silas.omaster.ai.model.SoftLightMode
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 哈苏参数滑块组件
 * 显示所有哈苏大师参数的可视化滑块
 *
 * 设计规范：
 * - 哈苏橙轨道 + 白色滑块
 * - 参数范围 -30 ~ +30
 * - 大师模式交互语言
 */
@Composable
fun HasselbladParamsSliderCard(
    params: HasselbladParams,
    onParamsChange: (HasselbladParams) -> Unit = {},
    isEditable: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp))
            .background(HasselbladTheme.CardBackground)
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎛️",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "哈苏大师参数",
                color = HasselbladTheme.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 影调滑块
        HasselbladParamSlider(
            label = "影调",
            value = params.tone,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(tone = newValue))
            },
            isEditable = isEditable
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 饱和度滑块
        HasselbladParamSlider(
            label = "饱和度",
            value = params.saturation,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(saturation = newValue))
            },
            isEditable = isEditable
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 对比度滑块
        HasselbladParamSlider(
            label = "对比度",
            value = params.contrast,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(contrast = newValue))
            },
            isEditable = isEditable
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 色温滑块
        HasselbladParamSlider(
            label = "色温",
            value = params.colorTemp,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(colorTemp = newValue))
            },
            isEditable = isEditable,
            suffix = if (params.colorTemp < 0) "冷" else if (params.colorTemp > 0) "暖" else ""
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 锐度滑块
        HasselbladParamSlider(
            label = "锐度",
            value = params.sharpness,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(sharpness = newValue))
            },
            isEditable = isEditable
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 暗角滑块
        HasselbladParamSlider(
            label = "暗角",
            value = params.vignette,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(vignette = newValue))
            },
            isEditable = isEditable,
            suffix = if (params.vignette > 0) "开" else if (params.vignette < 0) "减" else ""
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 青品调滑块
        HasselbladParamSlider(
            label = "青品调",
            value = params.cyanMagenta,
            valueRange = -30..30,
            onValueChange = { newValue ->
                if (isEditable) onParamsChange(params.copy(cyanMagenta = newValue))
            },
            isEditable = isEditable,
            suffix = if (params.cyanMagenta < 0) "青" else if (params.cyanMagenta > 0) "品" else ""
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 柔光模式选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "柔光",
                color = HasselbladTheme.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.width(60.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoftLightMode.entries.forEach { mode ->
                    SoftLightButton(
                        mode = mode,
                        isSelected = params.softLight == mode,
                        onClick = {
                            if (isEditable) onParamsChange(params.copy(softLight = mode))
                        },
                        enabled = isEditable
                    )
                }
            }
        }
    }
}

/**
 * 单个参数滑块
 */
@Composable
fun HasselbladParamSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    isEditable: Boolean,
    suffix: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HasselbladTheme.ParamSliderHeight.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 参数名称
        Text(
            text = label,
            color = HasselbladTheme.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(60.dp)
        )

        // 参数值显示
        Text(
            text = formatParamValue(value, suffix),
            color = if (value != 0) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(50.dp)
        )

        // 滑块
        if (isEditable) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladTheme.SliderThumb,
                    activeTrackColor = HasselbladTheme.HasselbladOrange,
                    inactiveTrackColor = HasselbladTheme.SliderTrack,
                    activeTickColor = HasselbladTheme.HasselbladOrange.copy(alpha = 0.5f),
                    inactiveTickColor = HasselbladTheme.SliderTrack.copy(alpha = 0.5f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HasselbladTheme.SliderThumb)
                            .padding(2.dp)
                            .background(HasselbladTheme.HasselbladOrange, RoundedCornerShape(2.dp))
                    )
                }
            )
        } else {
            // 不可编辑时显示静态进度条
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HasselbladTheme.SliderTrack)
            ) {
                // 显示当前位置
                val normalizedValue = (value - valueRange.first) / (valueRange.last - valueRange.first).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(normalizedValue)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HasselbladTheme.HasselbladOrange)
                )
            }
        }
    }
}

/**
 * 柔光模式按钮
 */
@Composable
fun SoftLightButton(
    mode: SoftLightMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) HasselbladTheme.HasselbladOrange.copy(alpha = 0.2f) else HasselbladTheme.CardBackgroundHighlight,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HasselbladTheme.HasselbladOrange) else null,
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text = mode.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextTertiary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * 格式化参数值显示
 */
private fun formatParamValue(value: Int, suffix: String): String {
    val sign = if (value > 0) "+" else ""
    return "$sign$value $suffix".trim()
}

/**
 * 简化版参数显示（只读）
 */
@Composable
fun HasselbladParamsDisplaySimple(
    params: HasselbladParams,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HasselbladTheme.CardBackground)
            .padding(12.dp)
    ) {
        // 只显示非零参数
        val nonZeroParams = mutableListOf<Pair<String, String>>()

        if (params.tone != 0) nonZeroParams.add("影调" to formatParamValue(params.tone, ""))
        if (params.saturation != 0) nonZeroParams.add("饱和度" to formatParamValue(params.saturation, ""))
        if (params.contrast != 0) nonZeroParams.add("对比度" to formatParamValue(params.contrast, ""))
        if (params.colorTemp != 0) nonZeroParams.add("色温" to formatParamValue(params.colorTemp, ""))
        if (params.sharpness != 0) nonZeroParams.add("锐度" to formatParamValue(params.sharpness, ""))
        if (params.vignette != 0) nonZeroParams.add("暗角" to formatParamValue(params.vignette, ""))
        if (params.cyanMagenta != 0) nonZeroParams.add("青品调" to formatParamValue(params.cyanMagenta, ""))
        if (params.softLight != SoftLightMode.NONE) nonZeroParams.add("柔光" to params.softLight.displayName)

        if (nonZeroParams.isEmpty()) {
            Text(
                text = "参数：中性（无调整）",
                color = HasselbladTheme.TextTertiary,
                fontSize = 12.sp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nonZeroParams.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HasselbladTheme.CardBackgroundHighlight
                    ) {
                        Text(
                            text = "$label $value",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = HasselbladTheme.HasselbladOrange,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}