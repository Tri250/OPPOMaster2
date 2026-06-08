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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * 哈苏色彩科学页面
 * HNCS 3.0 自然色彩解决方案
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val colorModes = listOf(
        ColorMode("natural", "哈苏自然色彩", "HNCS 3.0 自然色彩解决方案", Color(0xFF4CAF50)),
        ColorMode("portrait", "人像肤色优化", "自然美化肤色，保留细节", Color(0xFFFF6B9D)),
        ColorMode("landscape", "风景色彩增强", "增强风景色彩层次", Color(0xFF4ECDC4)),
        ColorMode("classic", "哈苏经典胶片", "复古胶片色彩质感", Color(0xFF9C27B0)),
        ColorMode("bw", "哈苏黑白", "经典黑白摄影风格", Color(0xFF808080)),
        ColorMode("vivid", "鲜艳色彩", "鲜艳饱满的色彩表现", Color(0xFFFF9800)),
    )

    val selectedMode = remember { mutableStateOf("natural") }
    val params = remember {
        mutableStateMapOf(
            "saturation" to 0,
            "contrast" to 5,
            "warmth" to 0,
            "vibrance" to 5,
            "clarity" to 0,
        )
    }
    val isApplied = remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.ArrowBack, "返回")
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
            // Hero Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HNCS 3.0",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                        Text(
                            text = "哈苏自然色彩解决方案",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "还原真实色彩，呈现自然之美",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 色彩模式
            item {
                Text(
                    text = "色彩模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            items(colorModes) { mode ->
                ColorModeCard(
                    mode = mode,
                    isSelected = selectedMode.value == mode.id,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Select)
                        selectedMode.value = mode.id
                    }
                )
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
                                    value = params[key]?.toFloat() ?: 0f,
                                    onValueChange = { params[key] = it.toInt() },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = HasselbladOrange,
                                        inactiveTrackColor = Color.Gray,
                                        thumbColor = HasselbladOrange
                                    )
                                )
                                Text(
                                    text = params[key].toString(),
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
                        icon = Icons.Default.BrightnessHigh,
                        title = "自然肤色还原",
                        description = "智能识别肤色区域，自然美化不偏色"
                    )
                    FeatureItem(
                        icon = Icons.Default.Layers,
                        title = "色彩层次增强",
                        description = "智能增强色彩过渡，层次更丰富"
                    )
                    FeatureItem(
                        icon = Icons.Default.Star,
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
                            params["saturation"] = 0
                            params["contrast"] = 5
                            params["warmth"] = 0
                            params["vibrance"] = 5
                            params["clarity"] = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置", color = Color.White)
                    }
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            isApplied.value = true
                            kotlinx.coroutines.delay(2000)
                            isApplied.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isApplied.value) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("已应用")
                        } else {
                            Icon(Icons.Default.Palette, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("应用色彩")
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

data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Color
)

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
            .background(if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) HasselbladOrange else Color.White
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.material.icons.Icon,
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
                    .background(HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HasselbladOrange)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
