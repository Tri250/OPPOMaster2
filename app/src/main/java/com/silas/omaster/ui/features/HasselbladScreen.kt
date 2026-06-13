package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import kotlinx.coroutines.launch

/**
 * 哈苏色彩科学页面
 * HNCS 3.0 自然色彩解决方案
 *
 * 完整设计规范（与Web端对齐）：
 * - Hero Section: HNCS 3.0介绍
 * - 色彩模式选择（6种模式）
 * - 精细调节参数
 * - 核心特性展示
 * - 重置和应用按钮
 */

/**
 * 色彩模式定义
 */
data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val icon: androidx.compose.material.icons.Icon,
    val params: Map<String, Int>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 色彩模式列表（与Web端完全对齐）
    val colorModes = listOf(
        ColorMode(
            "natural",
            "哈苏自然色彩",
            "HNCS 3.0 自然色彩解决方案",
            HasselbladOrange,
            Icons.Default.Visibility,
            mapOf("saturation" to 0, "contrast" to 5, "warmth" to 0, "vibrance" to 5, "clarity" to 0)
        ),
        ColorMode(
            "portrait",
            "人像肤色优化",
            "自然美化肤色，保留细节",
            Color(0xFFFF6B9D),
            Icons.Default.Face,
            mapOf("saturation" to 5, "contrast" to 8, "warmth" to 3, "vibrance" to 0, "clarity" to 0)
        ),
        ColorMode(
            "landscape",
            "风景色彩增强",
            "增强风景色彩层次",
            Color(0xFF4ECDC4),
            Icons.Default.Landscape,
            mapOf("saturation" to 12, "contrast" to 10, "warmth" to 5, "vibrance" to 10, "clarity" to 10)
        ),
        ColorMode(
            "classic",
            "哈苏经典胶片",
            "复古胶片色彩质感",
            Color(0xFF9C27B0),
            Icons.Default.AutoAwesome,
            mapOf("saturation" to 8, "contrast" to 15, "warmth" to 8, "vibrance" to 5, "clarity" to 5)
        ),
        ColorMode(
            "bw",
            "哈苏黑白",
            "经典黑白摄影风格",
            Color(0xFF808080),
            Icons.Default.DarkMode,
            mapOf("saturation" to -100, "contrast" to 20, "warmth" to 0, "vibrance" to 0, "clarity" to 15)
        ),
        ColorMode(
            "vivid",
            "鲜艳色彩",
            "鲜艳饱满的色彩表现",
            Color(0xFFFF9800),
            Icons.Default.Palette,
            mapOf("saturation" to 20, "contrast" to 10, "warmth" to 0, "vibrance" to 15, "clarity" to 0)
        )
    )

    // 状态
    var selectedMode by remember { mutableStateOf("natural") }
    val params = remember {
        mutableStateMapOf(
            "saturation" to 0,
            "contrast" to 5,
            "warmth" to 0,
            "vibrance" to 5,
            "clarity" to 0,
        )
    }
    var isApplied by remember { mutableStateOf(false) }

    // 选择模式时更新参数
    fun selectMode(modeId: String) {
        val mode = colorModes.find { it.id == modeId }
        if (mode != null) {
            selectedMode = modeId
            mode.params.forEach { (key, value) ->
                params[key] = value
            }
        }
    }

    // 重置参数
    fun resetParams() {
        val mode = colorModes.find { it.id == selectedMode }
        if (mode != null) {
            mode.params.forEach { (key, value) ->
                params[key] = value
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("哈苏色彩科学", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
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
            // Hero Section - HNCS 3.0介绍
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(HasselbladOrange, Color(0xFFFF8A50))
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Camera, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "HNCS 3.0",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "哈苏自然色彩解决方案",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "还原真实色彩，呈现自然之美",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 色彩模式选择
            item {
                Text(
                    text = "色彩模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorModes.forEach { mode ->
                        ColorModeCard(
                            mode = mode,
                            isSelected = selectedMode == mode.id,
                            onClick = {
                                haptic.perform(HapticFeedbackType.SegmentTick)
                                selectMode(mode.id)
                            }
                        )
                    }
                }
            }

            // 精细调节
            item {
                Text(
                    text = "精细调节",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf(
                            "saturation" to "饱和度",
                            "contrast" to "对比度",
                            "warmth" to "色温",
                            "vibrance" to "鲜艳度",
                            "clarity" to "清晰度",
                        ).forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.width(60.dp)
                                )
                                Slider(
                                    value = (params[key] ?: 0).toFloat(),
                                    onValueChange = { params[key] = it.toInt() },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = HasselbladOrange,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f),
                                        thumbColor = HasselbladOrange
                                    )
                                )
                                Text(
                                    text = "${params[key] ?: 0}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HasselbladOrange,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }

            // 核心特性
            item {
                Text(
                    text = "核心特性",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureItem(
                        icon = Icons.Default.Face,
                        iconBgColor = Color(0xFFFF6B9D),
                        title = "自然肤色还原",
                        description = "智能识别肤色区域，自然美化不偏色"
                    )
                    FeatureItem(
                        icon = Icons.Default.Layers,
                        iconBgColor = Color(0xFF4ECDC4),
                        title = "色彩层次增强",
                        description = "智能增强色彩过渡，层次更丰富"
                    )
                    FeatureItem(
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = Color(0xFF9C27B0),
                        title = "16-bit 色彩深度",
                        description = "超高色彩精度，细节分毫毕现"
                    )
                }
            }

            // 底部操作按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            resetParams()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置", color = Color.White)
                    }
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            isApplied = true
                            scope.launch {
                                kotlinx.coroutines.delay(2000)
                                isApplied = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApplied) Color(0xFF4CAF50) else HasselbladOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isApplied) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("已应用", color = Color.White)
                        } else {
                            Icon(Icons.Default.Palette, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("应用色彩", color = Color.White)
                        }
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 色彩模式卡片
 */
@Composable
private fun ColorModeCard(
    mode: ColorMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
            ),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, HasselbladOrange)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(mode.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(mode.icon, null, tint = mode.color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) HasselbladOrange else Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 特性项
 */
@Composable
private fun FeatureItem(
    icon: androidx.compose.material.icons.Icon,
    iconBgColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconBgColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}