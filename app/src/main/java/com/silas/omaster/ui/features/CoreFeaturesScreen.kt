package com.silas.omaster.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform

/**
 * 核心功能页面
 * 整合所有AI功能和高级特性
 */
@Composable
fun CoreFeaturesScreen(
    onNavigateToSceneRecognition: () -> Unit = {},
    onNavigateToAIFineTune: () -> Unit = {},
    onNavigateToWatermarkEditor: () -> Unit = {},
    onNavigateToSmartOptimize: () -> Unit = {},
    onNavigateToPresetManager: () -> Unit = {},
    onNavigateToParamAdjustment: () -> Unit = {},
    onNavigateToHasselbladColor: () -> Unit = {},
    onNavigateToCloudSync: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // 功能开关状态
    var aiSceneEnabled by remember { mutableStateOf(settingsManager.isAISceneRecognitionEnabled) }
    var aiFineTuneEnabled by remember { mutableStateOf(settingsManager.isAIFineTuneEnabled) }
    var watermarkEnabled by remember { mutableStateOf(settingsManager.isWatermarkEditorEnabled) }
    var hasselbladEnabled by remember { mutableStateOf(settingsManager.isHasselbladColorEnabled) }
    var cloudSyncEnabled by remember { mutableStateOf(settingsManager.isCloudSyncEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AI驱动的专业摄影体验",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // AI功能区域标题
        SectionTitle(title = "AI 智能功能", icon = Icons.Default.AutoAwesome)

        // AI功能网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(280.dp),
            userScrollEnabled = false
        ) {
            item {
                FeatureCard(
                    title = "AI场景识别",
                    subtitle = "35+场景智能识别",
                    icon = Icons.Default.CameraAlt,
                    iconColor = Color(0xFF4CAF50),
                    gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)),
                    isEnabled = aiSceneEnabled,
                    onToggle = { enabled ->
                        aiSceneEnabled = enabled
                        settingsManager.isAISceneRecognitionEnabled = enabled
                        haptic.perform(HapticFeedbackType.ToggleOn)
                    },
                    onClick = onNavigateToSceneRecognition
                )
            }

            item {
                FeatureCard(
                    title = "AI微调",
                    subtitle = "色彩风格智能调整",
                    icon = Icons.Default.ColorLens,
                    iconColor = Color(0xFF9C27B0),
                    gradientColors = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A)),
                    isEnabled = aiFineTuneEnabled,
                    onToggle = { enabled ->
                        aiFineTuneEnabled = enabled
                        settingsManager.isAIFineTuneEnabled = enabled
                        haptic.perform(HapticFeedbackType.ToggleOn)
                    },
                    onClick = onNavigateToAIFineTune
                )
            }

            item {
                FeatureCard(
                    title = "智能优化",
                    subtitle = "一键HDR/降噪/锐化",
                    icon = Icons.Default.MotionPhotosAuto,
                    iconColor = Color(0xFF2196F3),
                    gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0)),
                    isEnabled = aiFineTuneEnabled,
                    onToggle = { enabled ->
                        aiFineTuneEnabled = enabled
                        settingsManager.isAIFineTuneEnabled = enabled
                        haptic.perform(HapticFeedbackType.ToggleOn)
                    },
                    onClick = onNavigateToSmartOptimize
                )
            }

            item {
                FeatureCard(
                    title = "水印编辑",
                    subtitle = "12+专业水印模板",
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF00BCD4),
                    gradientColors = listOf(Color(0xFF006064), Color(0xFF00838F)),
                    isEnabled = watermarkEnabled,
                    onToggle = { enabled ->
                        watermarkEnabled = enabled
                        settingsManager.isWatermarkEditorEnabled = enabled
                        haptic.perform(HapticFeedbackType.ToggleOn)
                    },
                    onClick = onNavigateToWatermarkEditor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 专业功能区域标题
        SectionTitle(title = "专业功能", icon = Icons.Default.SettingsSuggest)

        // 专业功能列表
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 预设管理
            FeatureListItem(
                title = "预设管理",
                subtitle = "云端预设库管理",
                icon = Icons.Default.PhotoFilter,
                iconColor = Color(0xFFFF9800),
                onClick = onNavigateToPresetManager
            )

            // 参数精细调节
            FeatureListItem(
                title = "参数精细调节",
                subtitle = "ISO/快门/光圈精确控制",
                icon = Icons.Default.Brush,
                iconColor = Color(0xFFE91E63),
                onClick = onNavigateToParamAdjustment
            )

            // 哈苏色彩科学
            FeatureListItem(
                title = "哈苏色彩科学",
                subtitle = "HNCS 3.0 自然色彩",
                icon = Icons.Default.CameraAlt,
                iconColor = Color(0xFFFFB347),
                isEnabled = hasselbladEnabled,
                onToggle = { enabled ->
                    hasselbladEnabled = enabled
                    settingsManager.isHasselbladColorEnabled = enabled
                    haptic.perform(HapticFeedbackType.ToggleOn)
                },
                onClick = onNavigateToHasselbladColor
            )

            // 云同步
            FeatureListItem(
                title = "云同步",
                subtitle = "预设数据云端备份",
                icon = Icons.Default.AutoAwesome,
                iconColor = Color(0xFF3F51B5),
                isEnabled = cloudSyncEnabled,
                onToggle = { enabled ->
                    cloudSyncEnabled = enabled
                    settingsManager.isCloudSyncEnabled = enabled
                    haptic.perform(HapticFeedbackType.ToggleOn)
                },
                onClick = onNavigateToCloudSync
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    gradientColors: List<Color>,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
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
            .aspectRatio(1f)
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
                .fillMaxSize()
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：图标和开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(40.dp, 24.dp)
                    )
                }

                // 底部：文字
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isEnabled: Boolean? = null,
    onToggle: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 开关（如果有）
            if (isEnabled != null && onToggle != null) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
