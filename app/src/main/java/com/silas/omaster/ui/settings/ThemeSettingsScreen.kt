package com.silas.omaster.ui.settings

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*

/**
 * 主题设置页面
 * 
 * 功能：
 * - 主题颜色选择
 * - 品牌风格切换
 * - 自定义主题配置
 * 
 * 对齐 Web 端 ThemeSettingsPage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onApply: (ThemeSettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // 主题选项
    var selectedTheme by remember { mutableStateOf("hasselblad") }
    val themes = listOf(
        ThemeOption("hasselblad", "哈苏", HasselbladOrange, "专业哈苏品牌风格"),
        ThemeOption("oppo", "OPPO", Color(0xFF1BA784), "OPPO 品牌绿色"),
        ThemeOption("vivo", "vivo", Color(0xFF415FFF), "vivo 品牌蓝色"),
        ThemeOption("realme", "realme", Color(0xFFFFC30D), "realme 品牌金色"),
        ThemeOption("honor", "荣耀", Color(0xFF0091FF), "荣耀品牌蓝色"),
        ThemeOption("xiaomi", "小米", Color(0xFFFF6900), "小米品牌橙色")
    )
    
    // 自定义颜色
    var customColor by remember { mutableStateOf(HasselbladOrange) }
    var showCustomColorPicker by remember { mutableStateOf(false) }
    
    // 深色模式
    var darkMode by remember { mutableStateOf("system") }
    val darkModeOptions = listOf("system", "light", "dark")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("主题设置", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply(ThemeSettings(
                        theme = selectedTheme,
                        customColor = customColor,
                        darkMode = darkMode
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

        // 主题选择
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "品牌主题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 主题网格
            themes.forEach { theme ->
                ThemeOptionCard(
                    theme = theme,
                    selected = selectedTheme == theme.id,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTheme = theme.id
                        customColor = theme.color
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.White.copy(alpha = 0.1f)
        )

        // 深色模式设置
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "深色模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                darkModeOptions.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (darkMode == mode) HasselbladOrange
                                else DarkGray
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                darkMode = mode
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                when (mode) {
                                    "system" -> Icons.Default.Settings
                                    "light" -> Icons.Default.LightMode
                                    "dark" -> Icons.Default.DarkMode
                                    else -> Icons.Default.Settings
                                },
                                null,
                                tint = if (darkMode == mode) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (mode) {
                                    "system" -> "跟随系统"
                                    "light" -> "浅色"
                                    "dark" -> "深色"
                                    else -> mode
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (darkMode == mode) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.White.copy(alpha = 0.1f)
        )

        // 自定义颜色
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "自定义强调色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 颜色预设
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colorPresets = listOf(
                    HasselbladOrange,
                    Color(0xFF1BA784),
                    Color(0xFF415FFF),
                    Color(0xFFFFC30D),
                    Color(0xFF0091FF),
                    Color(0xFFFF6900),
                    Color(0xFFE91E63),
                    Color(0xFF9C27B0)
                )
                
                colorPresets.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                2.dp,
                                if (customColor == color) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                customColor = color
                            }
                    )
                }
            }
        }

        // 预览区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "主题预览",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 预览按钮
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = customColor)
                ) {
                    Text("强调色按钮")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {},
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = customColor),
                    border = BorderStroke(1.dp, customColor)
                ) {
                    Text("强调色边框")
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    theme: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) theme.color.copy(alpha = 0.2f) else DarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.color)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) theme.color else Color.White
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = theme.color
                )
            }
        }
    }
}

data class ThemeOption(
    val id: String,
    val name: String,
    val color: Color,
    val description: String
)

data class ThemeSettings(
    val theme: String = "hasselblad",
    val customColor: Color = HasselbladOrange,
    val darkMode: String = "system"
)