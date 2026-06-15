package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform

/**
 * 功能中心页面 - 与Web端布局完全一致
 */
@Composable
fun FeaturesScreen(
    onNavigateToAIScene: () -> Unit = {},
    onNavigateToAIFineTune: () -> Unit = {},
    onNavigateToSmartOptimize: () -> Unit = {},
    onNavigateToWatermark: () -> Unit = {},
    onNavigateToParamAdjust: () -> Unit = {},
    onNavigateToPresetManager: () -> Unit = {},
    onNavigateToLUTShare: () -> Unit = {},
    onNavigateToHasselblad: () -> Unit = {},
    onNavigateToCloudSync: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(scrollState)
    ) {
        // Header - 与Web端一致
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.features_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.features_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // AI 智能功能区域
        FeatureSection(
            title = stringResource(R.string.section_ai_features),
            description = stringResource(R.string.section_ai_desc),
            icon = Icons.Default.Sparkles,
            count = 4
        ) {
            // AI 场景识别
            FeatureCard(
                title = stringResource(R.string.feature_ai_scene),
                subtitle = stringResource(R.string.feature_ai_scene_subtitle),
                description = stringResource(R.string.feature_ai_scene_desc),
                tips = listOf("人像", "风景", "夜景", "美食", "建筑", "自然"),
                gradientColors = listOf(
                    Color(0xFF3B82F6), // blue-500
                    Color(0xFF06B6D4)  // cyan-500
                ),
                icon = "scan",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToAIScene()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI 智能调参
            FeatureCard(
                title = stringResource(R.string.feature_ai_fine_tune),
                subtitle = stringResource(R.string.feature_ai_fine_tune_subtitle),
                description = stringResource(R.string.feature_ai_fine_tune_desc),
                tips = listOf("饱和度", "对比度", "亮度", "色温", "锐度"),
                gradientColors = listOf(
                    Color(0xFF8B5CF6), // purple-500
                    Color(0xFFEC4899)  // pink-500
                ),
                icon = "wand",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToAIFineTune()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 智能优化
            FeatureCard(
                title = stringResource(R.string.feature_smart_optimize),
                subtitle = stringResource(R.string.feature_smart_optimize_subtitle),
                description = stringResource(R.string.feature_smart_optimize_desc),
                tips = listOf("HDR增强", "智能降噪", "锐化"),
                gradientColors = listOf(
                    Color(0xFF10B981), // emerald-500
                    Color(0xFF14B8A6)  // teal-500
                ),
                icon = "cpu",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToSmartOptimize()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 水印编辑
            FeatureCard(
                title = stringResource(R.string.feature_watermark),
                subtitle = stringResource(R.string.feature_watermark_subtitle),
                description = stringResource(R.string.feature_watermark_desc),
                tips = listOf("标准", "极简", "详细", "品牌"),
                gradientColors = listOf(
                    Color(0xFFF59E0B), // amber-500
                    Color(0xFFF97316)  // orange-500
                ),
                icon = "droplets",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToWatermark()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 专业工具区域
        FeatureSection(
            title = stringResource(R.string.section_tools),
            description = stringResource(R.string.section_tools_desc),
            icon = Icons.Default.Settings,
            count = 2
        ) {
            // 参数调节
            FeatureCard(
                title = stringResource(R.string.feature_param_adjust),
                subtitle = stringResource(R.string.feature_param_adjust_subtitle),
                description = stringResource(R.string.feature_param_adjust_desc),
                tips = listOf("ISO 50-12800", "快门 1/1000s-30s", "光圈 f/1.4-f/22"),
                gradientColors = listOf(
                    Color(0xFF6366F1), // indigo-500
                    Color(0xFF8B5CF6)  // violet-500
                ),
                icon = "sliders",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToParamAdjust()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 预设管理
            FeatureCard(
                title = stringResource(R.string.feature_preset_manager),
                subtitle = stringResource(R.string.feature_preset_manager_subtitle),
                description = stringResource(R.string.feature_preset_manager_desc),
                tips = listOf("云端同步", "本地管理", "批量操作"),
                gradientColors = listOf(
                    Color(0xFF0EA5E9), // sky-500
                    Color(0xFF3B82F6)  // blue-500
                ),
                icon = "images",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToPresetManager()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 品牌特色区域
        FeatureSection(
            title = stringResource(R.string.section_brand),
            description = stringResource(R.string.section_brand_desc),
            icon = Icons.Default.Brush,
            count = 2
        ) {
            // LUT 分享
            FeatureCard(
                title = stringResource(R.string.feature_lut_share),
                subtitle = stringResource(R.string.feature_lut_share_subtitle),
                description = stringResource(R.string.feature_lut_share_desc),
                tips = listOf("电影色调", "胶片风格", "日系清新", "欧美复古"),
                gradientColors = listOf(
                    Color(0xFFF43F5E), // rose-500
                    Color(0xFFFB7185)  // rose-400
                ),
                icon = "share",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToLUTShare()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 哈苏影像
            FeatureCard(
                title = stringResource(R.string.feature_hasselblad),
                subtitle = stringResource(R.string.feature_hasselblad_subtitle),
                description = stringResource(R.string.feature_hasselblad_desc),
                tips = listOf("自然色彩", "肤色优化", "风景增强", "黑白胶片"),
                gradientColors = listOf(
                    Color(0xFFFF6B35), // Hasselblad Orange
                    Color(0xFFFF9800)  // amber
                ),
                icon = "aperture",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToHasselblad()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 云同步
            FeatureCard(
                title = stringResource(R.string.feature_cloud_sync),
                subtitle = stringResource(R.string.feature_cloud_sync_subtitle),
                description = stringResource(R.string.feature_cloud_sync_desc),
                tips = listOf("OPPO", "realme", "vivo", "荣耀"),
                gradientColors = listOf(
                    Color(0xFF0EA5E9), // sky-500
                    Color(0xFF38BDF8)  // sky-400
                ),
                icon = "cloud",
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToCloudSync()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 功能区域标题 - 与Web端一致
 */
@Composable
private fun FeatureSection(
    title: String,
    description: String,
    icon: ImageVector,
    count: Int,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF6B35).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(20.dp)
                    )
                }

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

            // Count Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        content()
    }
}

/**
 * 功能卡片 - 与Web端完全一致
 */
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    description: String,
    tips: List<String>,
    gradientColors: List<Color>,
    icon: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    FeatureIcon(iconName = icon)
                }

                // Arrow Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Transparent)
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Subtitle
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Description & Tips
            Column(
                modifier = Modifier.padding(top = 12.dp)
            ) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tips.take(4).forEach { tip ->
                        TipBadge(text = tip)
                    }
                }
            }
        }
    }
}

/**
 * 功能图标 - 简化版
 */
@Composable
private fun FeatureIcon(iconName: String) {
    // 使用文本emoji代替图标，与Web端风格一致
    val emoji = when (iconName) {
        "scan" -> "🔍"
        "wand" -> "✨"
        "cpu" -> "⚡"
        "droplets" -> "💧"
        "sliders" -> "⚙️"
        "images" -> "🖼️"
        "share" -> "📤"
        "aperture" -> "📷"
        "cloud" -> "☁️"
        else -> "✨"
    }

    Text(
        text = emoji,
        fontSize = 24.sp
    )
}

/**
 * 提示标签 - 与Web端一致
 */
@Composable
private fun TipBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}
