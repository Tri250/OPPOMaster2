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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
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
    onNavigateToSceneRecognition: () -> Unit,
    onNavigateToAIFineTune: () -> Unit,
    onNavigateToWatermarkEditor: () -> Unit,
    onNavigateToSmartOptimize: () -> Unit,
    onNavigateToPresetManager: () -> Unit,
    onNavigateToParamAdjustment: () -> Unit,
    onNavigateToLUTShare: () -> Unit,
    onNavigateToHasselbladColor: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // 功能开关状态
    var aiSceneEnabled by remember { mutableStateOf(settingsManager.isAISceneRecognitionEnabled) }
    var aiFineTuneEnabled by remember { mutableStateOf(settingsManager.isAIFineTuneEnabled) }
    var smartOptimizeEnabled by remember { mutableStateOf(settingsManager.isSmartOptimizeEnabled) }
    var watermarkEnabled by remember { mutableStateOf(settingsManager.isWatermarkEditorEnabled) }
    var hasselbladEnabled by remember { mutableStateOf(settingsManager.isHasselbladColorEnabled) }
    var cloudSyncEnabled by remember { mutableStateOf(settingsManager.isCloudSyncEnabled) }

    // 定义所有功能数据 - 同步Web端features数组
    val allFeatures = remember {
        listOf(
            // AI智能功能 (前4个)
            FeatureData(
                id = "ai-scene",
                title = "哈苏之眼",
                subtitle = "智能识别50+拍摄场景，自动推荐最佳参数",
                icon = Icons.Default.CameraAlt,
                gradientColors = listOf(Color(0xFFFF6B35), Color(0xFFFF8C42)),
                description = FeatureDescription(
                    desc = "支持36+拍摄场景智能识别",
                    tips = listOf("人像", "风景", "夜景", "美食", "建筑", "自然")
                )
            ),
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
            FeatureData(
                id = "watermark",
                title = "水印编辑器",
                subtitle = "14+专业水印模板，品牌认证水印",
                icon = Icons.Default.WaterDrop,
                gradientColors = listOf(Color(0xFF006064), Color(0xFF00838F)),
                description = FeatureDescription(
                    desc = "14+专业水印模板，品牌认证水印",
                    tips = listOf("标准", "极简", "详细", "品牌")
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
            // 品牌特色 (3个)
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
                title = "哈苏色彩科学",
                subtitle = "HNCS 3.0 自然色彩解决方案",
                icon = Icons.Default.Image,
                gradientColors = listOf(Color(0xFFCC5500), Color(0xFFE86A17)),
                description = FeatureDescription(
                    desc = "HNCS 3.0 自然色彩解决方案",
                    tips = listOf("自然色彩", "肤色优化", "风景增强", "黑白胶片")
                )
            ),
            FeatureData(
                id = "cloud-sync",
                title = "云同步",
                subtitle = "多平台云同步，数据永不丢失",
                icon = Icons.Default.Cloud,
                gradientColors = listOf(Color(0xFF1A237E), Color(0xFF303F9F)),
                description = FeatureDescription(
                    desc = "多平台云同步，数据永不丢失",
                    tips = listOf("OPPO", "realme", "vivo", "荣耀")
                )
            )
        )
    }

    // 功能分类 - 同步Web端
    val aiFeatures = allFeatures.slice(0..3)
    val toolFeatures = allFeatures.slice(4..5)
    val brandFeatures = allFeatures.slice(6..8)

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
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 页面标题 - 同步Web端
        item {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                Text(
                    text = "核心功能",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "点击进入功能操作界面",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
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
                    "ai-scene" -> aiSceneEnabled
                    "ai-fine-tune" -> aiFineTuneEnabled
                    "smart-optimize" -> smartOptimizeEnabled
                    "watermark" -> watermarkEnabled
                    else -> true
                }
                
                FeatureCard(
                    feature = feature,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        haptic.perform(HapticFeedbackType.LongPress)
                        when (feature.id) {
                            "ai-scene" -> {
                                aiSceneEnabled = enabled
                                settingsManager.isAISceneRecognitionEnabled = enabled
                            }
                            "ai-fine-tune" -> {
                                aiFineTuneEnabled = enabled
                                settingsManager.isAIFineTuneEnabled = enabled
                            }
                            "smart-optimize" -> {
                                smartOptimizeEnabled = enabled
                                settingsManager.isSmartOptimizeEnabled = enabled
                            }
                            "watermark" -> {
                                watermarkEnabled = enabled
                                settingsManager.isWatermarkEditorEnabled = enabled
                            }
                        }
                    },
                    onClick = {
                        when (feature.id) {
                            "ai-scene" -> onNavigateToSceneRecognition()
                            "ai-fine-tune" -> onNavigateToAIFineTune()
                            "smart-optimize" -> onNavigateToSmartOptimize()
                            "watermark" -> onNavigateToWatermarkEditor()
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
                    "cloud-sync" -> cloudSyncEnabled
                    else -> true
                }
                
                FeatureCard(
                    feature = feature,
                    isEnabled = isEnabled,
                    onToggle = { enabled ->
                        haptic.perform(HapticFeedbackType.LongPress)
                        when (feature.id) {
                            "hasselblad" -> {
                                hasselbladEnabled = enabled
                                settingsManager.isHasselbladColorEnabled = enabled
                            }
                            "cloud-sync" -> {
                                cloudSyncEnabled = enabled
                                settingsManager.isCloudSyncEnabled = enabled
                            }
                        }
                    },
                    onClick = {
                        when (feature.id) {
                            "lut-share" -> onNavigateToLUTShare()
                            "hasselblad" -> onNavigateToHasselbladColor()
                            "cloud-sync" -> onNavigateToCloudSync()
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
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 标题和描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        // 数量标签
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
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
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // 右侧按钮区域
                    if (feature.showToggle) {
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
                        // 进入按钮 - 同步Web端箭头样式
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 标题和副标题
                Text(
                    text = feature.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = feature.subtitle,
                    fontSize = 12.sp,
                    color = if (isEnabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // 描述和标签 - 同步Web端样式
                if (isEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 描述文字
                    Text(
                        text = feature.description.desc,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
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
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tip,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
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