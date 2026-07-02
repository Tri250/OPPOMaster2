package com.silas.omaster.ui.features

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.perform

/**
 * 功能描述数据类
 */
data class FeatureDescription(
    val desc: String,
    val tips: List<String>
)

/**
 * 功能数据类
 */
data class FeatureData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val description: FeatureDescription,
    val showToggle: Boolean = true
)

/**
 * 核心功能页面 - 同步Web端设计
 * 卡片式布局，三个功能分类区域
 */
@Composable
fun CoreFeaturesScreen(
    onNavigateToAIFineTune: () -> Unit,
    onNavigateToSmartOptimize: () -> Unit,
    onNavigateToPresetManager: () -> Unit,
    onNavigateToParamAdjustment: () -> Unit,
    onNavigateToLUTShare: () -> Unit,
    onNavigateToHasselbladColor: () -> Unit,
    onNavigateToSceneAnalysisReport: () -> Unit = {},
    onNavigateToXingYingJi: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // 功能开关状态
    var aiFineTuneEnabled by remember { mutableStateOf(settingsManager.isAIFineTuneEnabled) }
    var smartOptimizeEnabled by remember { mutableStateOf(settingsManager.isSmartOptimizeEnabled) }
    var hasselbladEnabled by remember { mutableStateOf(settingsManager.isHasselbladColorEnabled) }

    // 定义所有功能数据 - 同步Web端features数组
    val allFeatures = remember {
        listOf(
            // AI智能功能 (2个：去掉原哈苏之眼与水印编辑器)
            FeatureData(
                id = "ai-fine-tune",
                title = "AI 微调",
                subtitle = "一键智能微调，色彩风格精准控制",
                icon = Icons.Default.ColorLens,
                gradientColors = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A)),
                description = FeatureDescription(
                    desc = "一键智能微调，精准控制色彩风格",
                    tips = listOf("饱和度", "对比度", "亮度", "色温", "锐度")
                )
            ),
            FeatureData(
                id = "smart-optimize",
                title = "智能优化",
                subtitle = "一键HDR增强、降噪、锐化优化",
                icon = Icons.Default.Memory,
                gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0)),
                description = FeatureDescription(
                    desc = "HDR增强、智能降噪、锐化增强",
                    tips = listOf("HDR增强", "智能降噪", "锐化")
                )
            ),
            // 专业工具 (2个)
            FeatureData(
                id = "param-adjust",
                title = "参数精细调节",
                subtitle = "ISO、快门、光圈、白平衡精确控制",
                icon = Icons.Default.Tune,
                gradientColors = listOf(Color(0xFF880E4F), Color(0xFFAD1457)),
                description = FeatureDescription(
                    desc = "ISO、快门、光圈、白平衡精确控制",
                    tips = listOf("ISO 50-12800", "快门 1/1000s-30s", "光圈 f/1.4-f/22")
                ),
                showToggle = false
            ),
            FeatureData(
                id = "preset-manager",
                title = "预设管理",
                subtitle = "云端预设库，收藏、创建、分享",
                icon = Icons.Default.PhotoFilter,
                gradientColors = listOf(Color(0xFFFF6B35), Color(0xFFFF8C42)),
                description = FeatureDescription(
                    desc = "云端预设库，收藏、创建、分享",
                    tips = listOf("云端同步", "本地管理", "批量操作")
                ),
                showToggle = false
            ),
            // 品牌特色 (4个：原哈苏色彩科学改名为哈苏之眼)
            FeatureData(
                id = "lut-share",
                title = "LUT 资源分享",
                subtitle = "20+专业 LUT 滤镜，一键下载使用",
                icon = Icons.Default.Palette,
                gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA)),
                description = FeatureDescription(
                    desc = "20+专业 LUT 滤镜，一键下载使用",
                    tips = listOf("电影色调", "胶片风格", "日系清新", "欧美复古")
                ),
                showToggle = false
            ),
            FeatureData(
                id = "hasselblad",
                title = "哈苏之眼",
                subtitle = "OPPO Find X9 系列 · HNCS 3.0 哈苏大师体验",
                icon = Icons.Default.CameraAlt,
                gradientColors = listOf(Color(0xFFCC5500), Color(0xFFE86A17)),
                description = FeatureDescription(
                    desc = "真实场景识别，哈苏大师参数推荐，HNCS 3.0 自然色彩",
                    tips = listOf("人像大师", "风景增强", "夜景星空", "美食胶片", "建筑几何")
                )
            ),
            FeatureData(
                id = "scene-report",
                title = "大师洞察",
                subtitle = "拍摄数据看板 · 胶片风格排行 · 大师建议",
                icon = Icons.Default.Analytics,
                gradientColors = listOf(Color(0xFF00695C), Color(0xFF00897B)),
                description = FeatureDescription(
                    desc = "基于哈苏之眼配方历史，生成场景分布与拍摄习惯洞察",
                    tips = listOf("场景分布", "胶片排行", "连续拍摄", "大师建议")
                ),
                showToggle = false
            ),
            // 记忆库（TrailSnap Android 原生版）
            FeatureData(
                id = "xingyingji",
                title = "行影集",
                subtitle = "TrailSnap · 照片回忆 · 票据 · 足迹",
                icon = Icons.Default.Collections,
                gradientColors = listOf(Color(0xFFFF6B35), Color(0xFFFFA726)),
                description = FeatureDescription(
                    desc = "把旅行从“拍过”变成“可回味、可分享、可沉淀”",
                    tips = listOf("时间轴", "足迹地图", "行程票据", "年度报告")
                ),
                showToggle = false
            )
        )
    }

    // 功能分类 - 同步Web端
    val aiFeatures = allFeatures.slice(0..1)
    val toolFeatures = allFeatures.slice(2..3)
    val brandFeatures = allFeatures.slice(4..6)
    val memoryFeatures = allFeatures.slice(7..7)

    val listState = rememberLazyListState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            val isUp = currentIndex < previousIndex ||
                (currentIndex == previousIndex && currentOffset <= previousOffset)
            previousIndex = currentIndex
            previousOffset = currentOffset
            onScrollStateChanged(isUp)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 页面标题 - 同步Web端
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                Text(
                    text = "核心功能",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "点击进入功能操作界面",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // AI智能功能区域
        item {
            SectionHeader(
                title = "AI 智能功能",
                description = "智能识别与自动优化",
                icon = Icons.Default.AutoAwesome,
                count = aiFeatures.size
            )
        }

        aiFeatures.forEach { feature ->
            item {
                val isEnabled = when (feature.id) {
                    "ai-fine-tune" -> aiFineTuneEnabled
                    "smart-optimize" -> smartOptimizeEnabled
                    else -> true
                }

                FeatureCard(
                    feature = feature,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (feature.id) {
                            "ai-fine-tune" -> {
                                aiFineTuneEnabled = enabled
                                settingsManager.isAIFineTuneEnabled = enabled
                            }
                            "smart-optimize" -> {
                                smartOptimizeEnabled = enabled
                                settingsManager.isSmartOptimizeEnabled = enabled
                            }
                        }
                    },
                    onClick = {
                        when (feature.id) {
                            "ai-fine-tune" -> onNavigateToAIFineTune()
                            "smart-optimize" -> onNavigateToSmartOptimize()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 专业工具区域
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "专业工具",
                description = "精细调节与创作工具",
                icon = Icons.Default.Settings,
                count = toolFeatures.size
            )
        }

        toolFeatures.forEach { feature ->
            item {
                FeatureCard(
                    feature = feature,
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        when (feature.id) {
                            "param-adjust" -> onNavigateToParamAdjustment()
                            "preset-manager" -> onNavigateToPresetManager()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 品牌特色区域
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "品牌特色",
                description = "哈苏影像系统专属功能",
                icon = Icons.Default.Brush,
                count = brandFeatures.size
            )
        }

        brandFeatures.forEach { feature ->
            item {
                val isEnabled = when (feature.id) {
                    "hasselblad" -> hasselbladEnabled
                    else -> true
                }

                FeatureCard(
                    feature = feature,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (feature.id) {
                            "hasselblad" -> {
                                hasselbladEnabled = enabled
                                settingsManager.isHasselbladColorEnabled = enabled
                            }
                        }
                    },
                    onClick = {
                        when (feature.id) {
                            "lut-share" -> onNavigateToLUTShare()
                            "hasselblad" -> onNavigateToHasselbladColor()
                            "scene-report" -> onNavigateToSceneAnalysisReport()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 记忆库区域（行影集）
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "记忆库",
                description = "照片回忆与旅行沉淀",
                icon = Icons.Default.Collections,
                count = memoryFeatures.size
            )
        }

        memoryFeatures.forEach { feature ->
            item {
                FeatureCard(
                    feature = feature,
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        if (feature.id == "xingyingji") {
                            onNavigateToXingYingJi()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 分类标题 - 同步Web端SectionHeader样式
 */
@Composable
private fun SectionHeader(
    title: String,
    description: String,
    icon: ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HasselbladOrange.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "功能图标",
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 标题和描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        
        // 数量标签
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * 功能卡片 - 同步Web端FeatureCard样式
 * 渐变背景、图标位置、文字布局、描述和标签
 */
@Composable
private fun FeatureCard(
    feature: FeatureData,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
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
                        colors = if (isEnabled) feature.gradientColors else listOf(
                            Color(0xFF2A2A2A),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // 顶部：图标和箭头按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 图标容器 - 同步Web端样式
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = "功能图标",
                            tint = if (isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 右侧按钮区域
                    if (feature.showToggle) {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                checkedTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    } else {
                        // 进入按钮 - 同步Web端箭头样式
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "功能图标",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 标题和副标题
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // 描述和标签 - 同步Web端样式
                if (isEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 描述文字
                    Text(
                        text = feature.description.desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    
                    // 标签列表 - 同步Web端tips样式
                    if (feature.description.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            feature.description.tips.take(4).forEach { tip ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}