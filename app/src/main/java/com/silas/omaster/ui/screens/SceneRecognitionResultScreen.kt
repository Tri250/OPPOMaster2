package com.silas.omaster.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import com.silas.omaster.model.*
import kotlin.math.roundToInt
import com.silas.omaster.ui.components.FilmRecommendationStrip
import com.silas.omaster.ui.theme.HasselbladColors
import com.silas.omaster.ui.theme.*
import java.io.File

/**
 * 场景识别结果页面
 * 
 * 功能：
 * - Before/After 对比滑块
 * - 置信度可视化
 * - 胶片推荐卡片
 * - 哈苏大师参数展示
 * - 大师拍摄建议
 * - 分享、保存、导出、一键优化功能
 * 
 * 对齐 Web 端 SceneRecognitionResult.tsx
 */
@Composable
fun SceneRecognitionResultScreen(
    sceneProfile: SceneProfile,
    originalBitmap: Bitmap?,
    processedBitmap: Bitmap?,
    onShare: () -> Unit = {},
    onSave: () -> Unit = {},
    onExport: () -> Unit = {},
    onOptimize: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    var selectedFilmId by remember { mutableStateOf(sceneProfile.recommendedFilm.firstOrNull()?.id) }
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var showFilmDetail by remember { mutableStateOf<FilmPreset?>(null) }
    
    val selectedFilm = sceneProfile.recommendedFilm.find { it.id == selectedFilmId }
    
    Scaffold(
        containerColor = HasselbladColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 出片",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = HasselbladColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, null, tint = HasselbladColors.TextSecondary)
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, null, tint = HasselbladColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HasselbladColors.Surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 场景信息头部
            item {
                SceneInfoHeader(sceneProfile = sceneProfile)
            }
            
            // Before/After 对比滑块
            item {
                if (originalBitmap != null && processedBitmap != null) {
                    BeforeAfterCompareSlider(
                        beforeBitmap = originalBitmap,
                        afterBitmap = processedBitmap,
                        sliderPosition = sliderPosition,
                        onSliderChange = { position ->
                            sliderPosition = position
                            haptic.perform(HapticFeedbackType.TextHandleMove)
                        }
                    )
                } else {
                    // 仅显示原图
                    if (originalBitmap != null) {
                        ImagePreviewCard(
                            bitmap = originalBitmap,
                            label = "原始照片"
                        )
                    }
                }
            }
            
            // 置信度可视化
            item {
                ConfidenceVisualization(
                    confidence = sceneProfile.confidence,
                    category = sceneProfile.category
                )
            }
            
            // 胶片推荐
            item {
                FilmRecommendationStrip(
                    films = sceneProfile.recommendedFilm,
                    selectedId = selectedFilmId,
                    onSelect = { id ->
                        selectedFilmId = id
                        haptic.perform(HapticFeedbackType.LongPress)
                    }
                )
            }
            
            // 哈苏大师参数
            item {
                HasselbladParamsDisplay(
                    params = sceneProfile.hasselbladParams,
                    selectedFilm = selectedFilm
                )
            }
            
            // 大师拍摄建议
            item {
                MasterTipsSection(
                    tips = sceneProfile.masterTips,
                    category = sceneProfile.category
                )
            }
            
            // 操作按钮
            item {
                ActionButtonsRow(
                    onShare = onShare,
                    onSave = onSave,
                    onExport = onExport,
                    onOptimize = onOptimize
                )
            }
        }
        
        // 胶片详情弹窗
        val filmDetail = showFilmDetail
        if (filmDetail != null) {
            FilmDetailDialog(
                film = filmDetail,
                onDismiss = { showFilmDetail = null },
                onApply = {
                    selectedFilmId = showFilmDetail?.id
                    showFilmDetail = null
                    haptic.perform(HapticFeedbackType.LongPress)
                }
            )
        }
    }
}

/**
 * 场景信息头部
 */
@Composable
private fun SceneInfoHeader(
    sceneProfile: SceneProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(sceneProfile.color).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 场景图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(sceneProfile.color).copy(alpha = 0.3f))
            ) {
                Text(
                    text = sceneProfile.category.icon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 场景信息
            Column {
                Text(
                    text = sceneProfile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladColors.TextPrimary
                )
                Text(
                    text = sceneProfile.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(sceneProfile.color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sceneProfile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladColors.TextSecondary,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Before/After 对比滑块
 */
@Composable
private fun BeforeAfterCompareSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    sliderPosition: Float,
    onSliderChange: (Float) -> Unit
) {
    var containerWidthPx by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "效果对比",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladColors.TextPrimary
                    )
                Row {
                    Text(
                        text = "原图",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladColors.TextTertiary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "优化后",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 对比滑块
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .onSizeChanged { containerWidthPx = it.width.toFloat() }
            ) {
                // After 图片（底层）
                Image(
                    bitmap = android.graphics.Bitmap.createBitmap(afterBitmap).asImageBitmap(),
                    contentDescription = "优化后",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Before 图片（裁剪显示）
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(sliderPosition)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                ) {
                    Image(
                        bitmap = android.graphics.Bitmap.createBitmap(beforeBitmap).asImageBitmap(),
                        contentDescription = "原图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 原图标签
                    Surface(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(4.dp),
                        color = HasselbladColors.BackgroundSemiTransparent
                    ) {
                        Text(
                            text = "原图",
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // 分割线和滑块
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .offset { IntOffset((sliderPosition * containerWidthPx - 2.dp.toPx()).roundToInt(), 0) }
                        .background(HasselbladColors.TextPrimary)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                val newPosition = sliderPosition + dragAmount / containerWidthPx
                                onSliderChange(newPosition.coerceIn(0f, 1f))
                            }
                        }
                ) {
                    // 滑块手柄
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                            .offset { IntOffset(0, 0) }
                            .clip(RoundedCornerShape(20.dp))
                            .background(HasselbladColors.TextPrimary)
                            .border(2.dp, HasselbladOrange, RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            Icons.Default.CompareArrows,
                            null,
                            tint = HasselbladOrange,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                
                // 优化后标签
                Surface(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(4.dp),
                    color = HasselbladOrange.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "优化后",
                        style = MaterialTheme.typography.labelSmall,
                        color = HasselbladColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 滑块提示
            Text(
                text = "← 拖动滑块对比效果 →",
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 图片预览卡片
 */
@Composable
private fun ImagePreviewCard(
    bitmap: Bitmap,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = HasselbladColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * 置信度可视化
 */
@Composable
private fun ConfidenceVisualization(
    confidence: Float,
    category: SceneCategory
) {
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "confidence"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "识别置信度",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladColors.TextPrimary
                    )
                }
                
                Text(
                    text = "${(animatedConfidence * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        animatedConfidence >= 0.9f -> HasselbladGreen
                        animatedConfidence >= 0.7f -> HasselbladOrange
                        else -> HasselbladColors.TextSecondary
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 置信度进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HasselbladColors.Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedConfidence)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                animatedConfidence >= 0.9f -> HasselbladGreen
                                animatedConfidence >= 0.7f -> HasselbladOrange
                                else -> HasselbladColors.TextTertiary
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 置信度说明
            Text(
                text = when {
                    confidence >= 0.9f -> "高置信度：场景特征明显，推荐参数精准"
                    confidence >= 0.7f -> "中等置信度：场景特征可识别，参数可参考"
                    else -> "低置信度：场景特征不明显，建议手动调整"
                },
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladColors.TextSecondary
            )
        }
    }
}

/**
 * 哈苏大师参数展示
 */
@Composable
private fun HasselbladParamsDisplay(
    params: HasselbladParams,
    selectedFilm: FilmPreset?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "哈苏大师参数",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladColors.TextPrimary
                    )
                }
                
                if (selectedFilm != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HasselbladOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = selectedFilm.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 参数网格
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 影调
                ParamRow(
                    label = "影调",
                    value = params.tone,
                    description = if (params.tone < 0) "暗调/电影感" else if (params.tone > 0) "明亮/通透" else "标准"
                )
                
                // 饱和度
                ParamRow(
                    label = "饱和度",
                    value = params.saturation,
                    description = if (params.saturation < 0) "低饱和/克制" else if (params.saturation > 0) "高饱和/鲜艳" else "自然"
                )
                
                // 对比度
                ParamRow(
                    label = "对比度",
                    value = params.contrast,
                    description = if (params.contrast < 0) "柔和对比" else if (params.contrast > 0) "强烈对比" else "标准"
                )
                
                // 色温
                ParamRow(
                    label = "色温",
                    value = params.colorTemp,
                    description = if (params.colorTemp < 0) "冷调" else if (params.colorTemp > 0) "暖调" else "中性"
                )
                
                // 锐度
                ParamRow(
                    label = "锐度",
                    value = params.sharpness,
                    description = if (params.sharpness < 0) "柔和" else if (params.sharpness > 0) "锐利" else "自然"
                )
                
                // 暗角
                ParamRow(
                    label = "暗角",
                    value = params.vignette,
                    description = if (params.vignette > 0) "胶片感暗角" else if (params.vignette < 0) "明亮边缘" else "无暗角"
                )
                
                // 青品调
                ParamRow(
                    label = "青品调",
                    value = params.cyanMagenta,
                    description = if (params.cyanMagenta < 0) "偏青/电影感" else if (params.cyanMagenta > 0) "偏品/复古" else "中性"
                )
                
                // 柔光模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "柔光模式",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladColors.TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (params.softLight != SoftLightMode.NONE) HasselbladOrange.copy(alpha = 0.2f) else HasselbladColors.Surface
                    ) {
                        Text(
                            text = params.softLight.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (params.softLight != SoftLightMode.NONE) HasselbladOrange else HasselbladColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // HNCS 理念提示
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = HasselbladOrange.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HNCS理念：克制使用饱和度，±15为舒适区",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 参数行
 */
@Composable
private fun ParamRow(
    label: String,
    value: Int,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladColors.TextSecondary
                    )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 数值
            Text(
                text = if (value >= 0) "+$value" else "$value",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    value > 15 -> HasselbladOrange
                    value < -15 -> Color(0xFF2196F3)
                    value != 0 -> HasselbladColors.TextPrimary
                    else -> HasselbladColors.TextTertiary
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 描述
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HasselbladColors.Surface
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = HasselbladColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * 大师拍摄建议
 */
@Composable
private fun MasterTipsSection(
    tips: List<String>,
    category: SceneCategory
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                        text = "大师建议",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladColors.TextPrimary
                    )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 建议列表
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tips.forEachIndexed { index, tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 序号
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HasselbladOrange.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HasselbladOrange,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 建议
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = HasselbladColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // 场景专属提示
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(category.color).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${category.displayName}场景专属建议：${getCategoryTip(category)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 获取场景专属提示
 */
private fun getCategoryTip(category: SceneCategory): String {
    return when (category) {
        SceneCategory.PORTRAIT -> "关注眼神光和肤色调优"
        SceneCategory.LANDSCAPE -> "注意层次感和色彩分离"
        SceneCategory.NIGHT -> "控制噪点和光源渲染"
        SceneCategory.FOOD -> "强调质感和色彩饱和"
        SceneCategory.URBAN -> "突出建筑线条和氛围"
        SceneCategory.STILL_LIFE -> "注重光影细节和构图"
        SceneCategory.MACRO -> "精确聚焦和景深控制"
        SceneCategory.EVENT -> "捕捉瞬间和氛围表达"
    }
}

/**
 * 操作按钮行
 */
@Composable
private fun ActionButtonsRow(
    onShare: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onOptimize: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 分享
        OutlinedButton(
            onClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                onShare()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HasselbladColors.TextPrimary)
        ) {
            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("分享配方")
        }

        // 保存
        OutlinedButton(
            onClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                onSave()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HasselbladColors.TextPrimary)
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("保存配方")
        }

        // 导出
        OutlinedButton(
            onClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                onExport()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HasselbladColors.TextPrimary)
        ) {
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("导出")
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // 一键优化按钮
    Button(
        onClick = {
            haptic.perform(HapticFeedbackType.LongPress)
            onOptimize()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
    ) {
        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "一键哈苏优化",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 胶片详情弹窗（复用已有组件）
 */
@Composable
private fun FilmDetailDialog(
    film: FilmPreset,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    com.silas.omaster.ui.components.FilmDetailDialog(
        film = film,
        onDismiss = onDismiss,
        onApply = onApply
    )
}