package com.silas.omaster.ui.features

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.HapticFeedbackType as HapticType
import com.silas.omaster.util.perform

/**
 * 核心功能页面 - 重新设计排版
 * 6大核心功能入口，清晰分类，专业布局
 */
@Composable
fun CoreFeaturesScreen(
    onNavigateToSceneRecognition: () -> Unit,
    onNavigateToAIFineTune: () -> Unit,
    onNavigateToWatermarkEditor: () -> Unit,
    onNavigateToSmartOptimize: () -> Unit,
    onNavigateToParamAdjustment: () -> Unit,
    onNavigateToHasselbladColor: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // 功能开关状态
    var aiSceneEnabled by remember { mutableStateOf(settingsManager.isAISceneRecognitionEnabled) }
    var aiFineTuneEnabled by remember { mutableStateOf(settingsManager.isAIFineTuneEnabled) }
    var watermarkEnabled by remember { mutableStateOf(settingsManager.isWatermarkEditorEnabled) }
    var smartOptimizeEnabled by remember { mutableStateOf(settingsManager.isSmartOptimizeEnabled) }
    var hasselbladEnabled by remember { mutableStateOf(settingsManager.isHasselbladColorEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp)
    ) {
        // 页面标题
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AI驱动的专业影像体验",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // AI智能功能区域
        item {
            SectionHeader(
                title = "AI 智能功能",
                icon = Icons.Default.AutoAwesome,
                description = "智能识别与自动优化"
            )
        }

        // AI场景识别（相机实时识别）
        item {
            FeatureCard(
                title = "AI场景识别",
                subtitle = "打开相机实时识别场景，推荐哈苏大师参数",
                icon = Icons.Default.CameraAlt,
                iconColor = Color(0xFFFF6B35),
                gradientColors = listOf(Color(0xFFE65100), Color(0xFFFF6B35)),
                isEnabled = aiSceneEnabled,
                showToggle = false,
                onToggle = { enabled ->
                    haptic.perform(HapticType.ToggleOn)
                    aiSceneEnabled = enabled
                    settingsManager.isAISceneRecognitionEnabled = enabled
                },
                onClick = onNavigateToSceneRecognition
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // AI微调
        item {
            FeatureCard(
                title = "AI 微调",
                subtitle = "一键智能微调，色彩风格精准控制",
                icon = Icons.Default.ColorLens,
                iconColor = Color(0xFF9C27B0),
                gradientColors = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A)),
                isEnabled = aiFineTuneEnabled,
                onToggle = { enabled ->
                    haptic.perform(HapticType.ToggleOn)
                    aiFineTuneEnabled = enabled
                    settingsManager.isAIFineTuneEnabled = enabled
                },
                onClick = onNavigateToAIFineTune
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 智能优化
        item {
            FeatureCard(
                title = "智能优化",
                subtitle = "一键HDR增强、降噪、锐化优化",
                icon = Icons.Default.Memory,
                iconColor = Color(0xFF2196F3),
                gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0)),
                isEnabled = smartOptimizeEnabled,
                onToggle = { enabled ->
                    haptic.perform(HapticType.ToggleOn)
                    smartOptimizeEnabled = enabled
                    settingsManager.isSmartOptimizeEnabled = enabled
                },
                onClick = onNavigateToSmartOptimize
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 专业工具区域
        item {
            SectionHeader(
                title = "专业工具",
                icon = Icons.Default.SettingsSuggest,
                description = "精细调节与创作工具"
            )
        }

        // 水印编辑器
        item {
            FeatureCard(
                title = "水印编辑器",
                subtitle = "14+专业水印模板，品牌认证水印",
                icon = Icons.Default.WaterDrop,
                iconColor = Color(0xFF00BCD4),
                gradientColors = listOf(Color(0xFF006064), Color(0xFF00838F)),
                isEnabled = watermarkEnabled,
                onToggle = { enabled ->
                    haptic.perform(HapticType.ToggleOn)
                    watermarkEnabled = enabled
                    settingsManager.isWatermarkEditorEnabled = enabled
                },
                onClick = onNavigateToWatermarkEditor
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 参数精细调节
        item {
            FeatureCard(
                title = "参数精细调节",
                subtitle = "ISO、快门、光圈、白平衡精确控制",
                icon = Icons.Default.Tune,
                iconColor = Color(0xFFE91E63),
                gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457)),
                isEnabled = true,
                showToggle = false,
                onClick = onNavigateToParamAdjustment
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 品牌特色区域
        item {
            SectionHeader(
                title = "品牌特色",
                icon = Icons.Default.Brush,
                description = "哈苏影像系统专属功能"
            )
        }

        // 哈苏色彩科学
        item {
            FeatureCard(
                title = "哈苏色彩科学",
                subtitle = "HNCS 3.0 自然色彩解决方案",
                icon = Icons.Default.Image,
                iconColor = HasselbladOrange,
                gradientColors = listOf(Color(0xFFCC5500), Color(0xFFE86A17)),
                isEnabled = hasselbladEnabled,
                onToggle = { enabled ->
                    haptic.perform(HapticType.ToggleOn)
                    hasselbladEnabled = enabled
                    settingsManager.isHasselbladColorEnabled = enabled
                },
                onClick = onNavigateToHasselbladColor
            )
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(HasselbladOrange.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    gradientColors: List<Color>,
    isEnabled: Boolean,
    showToggle: Boolean = true,
    onToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isEnabled) gradientColors else listOf(
                            Color(0xFF2A2A2A),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 文字
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                        if (isEnabled && showToggle) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 开关
                if (showToggle) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    // 进入按钮
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Brush,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}