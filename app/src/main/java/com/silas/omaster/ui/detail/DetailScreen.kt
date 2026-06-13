package com.silas.omaster.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.local.FloatingWindowGuideManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.DescriptionCard
import com.silas.omaster.ui.components.FloatingWindowGuideDialog
import com.silas.omaster.ui.components.ImageGallery
import com.silas.omaster.ui.components.ModeBadge
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.ParameterCard
import com.silas.omaster.ui.components.SectionTitle
import com.silas.omaster.ui.components.ShootingTipsDetailCard
import com.silas.omaster.ui.components.PresetStatsCard
import com.silas.omaster.ui.components.UserCommentsCard
import com.silas.omaster.ui.components.UserComment
import com.silas.omaster.ui.components.RelatedPresetsCard
import com.silas.omaster.ui.components.RelatedPreset
import com.silas.omaster.ui.components.ApplyPresetButton
import com.silas.omaster.ui.components.FavoriteButton
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.service.FloatingWindowService
import androidx.compose.ui.res.stringResource
import com.silas.omaster.R
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.formatSigned
import com.silas.omaster.util.hapticClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

import com.silas.omaster.model.PresetSection

@Composable
fun DetailScreen(
    presetId: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    
    // 使用 presetId 作为 key，确保每个预设有独立的 ViewModel
    val viewModel: DetailViewModel = viewModel(
        key = presetId,
        factory = DetailViewModelFactory(repository)
    )

    // 加载预设数据
    LaunchedEffect(presetId) {
        viewModel.loadPreset(presetId)
    }
    
    // 当 refreshTrigger 变化时重新加载数据（用于编辑后刷新）
    // 使用 snapshotFlow 确保持续监听，即使页面不可见时也能捕获变化
    var lastRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
    LaunchedEffect(Unit) {
        snapshotFlow { refreshTrigger }
            .collect { newValue ->
                if (newValue != lastRefreshTrigger && newValue > 0) {
                    lastRefreshTrigger = newValue
                    viewModel.loadPreset(presetId)
                }
            }
    }

    val preset by viewModel.preset.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    // 悬浮窗引导对话框状态
    var showFloatingWindowGuide by remember { mutableStateOf(false) }
    val guideManager = remember { FloatingWindowGuideManager.getInstance(context) }

    // 悬浮窗控制器（全局单例，已在 MainActivity 中注册）
    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }

    // 当前显示的预设（用于悬浮窗切换时更新 UI）
    val floatingPreset by floatingWindowController.currentPreset.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OMasterTopAppBar(
            title = preset?.let { PresetI18n.getLocalizedPresetName(it.name) } ?: stringResource(R.string.detail_title),
            subtitle = preset?.author,
            onBack = {
                onBack()
            },
            actions = {
                // 悬浮窗按钮
                IconButton(
                    onClick = {
                        preset?.let { p ->
                            val isFirstTime = guideManager.isFirstTimeUseFloatingWindow()
                            android.util.Log.d("DetailScreen", "悬浮窗按钮点击，是否首次使用: $isFirstTime")
                            // 检查是否是首次使用悬浮窗
                            if (isFirstTime) {
                                showFloatingWindowGuide = true
                                guideManager.markGuideShown()
                                android.util.Log.d("DetailScreen", "显示悬浮窗引导对话框")
                            } else {
                                // 非首次使用，直接处理悬浮窗逻辑（预设列表已在 HomeScreen 中设置）
                                handleFloatingWindowClick(context, p)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.floating_window),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 编辑按钮（仅自定义预设显示）
                if (preset?.isCustom == true && onEdit != null) {
                    IconButton(
                        onClick = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            preset?.id?.let { presetId ->
                                onEdit(presetId)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = Color.White
                        )
                    }
                }

                // 收藏按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.toggleFavorite()
                }) {
                    Icon(
                        imageVector = if (isFavorite)
                            Icons.Filled.Favorite
                        else
                            Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.preset_favorited) else stringResource(R.string.preset_favorite),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (preset == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.detail_load_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                val scrollState = rememberScrollState()

                // 滚动到顶/底部震感
                var lastScrollValue by remember { mutableIntStateOf(0) }
                var hasHapticAtTop by remember { mutableStateOf(false) }
                var hasHapticAtBottom by remember { mutableStateOf(false) }

                LaunchedEffect(scrollState.value) {
                    val currentValue = scrollState.value
                    val maxValue = scrollState.maxValue

                    if (currentValue == 0 && !hasHapticAtTop) {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        hasHapticAtTop = true
                        hasHapticAtBottom = false
                    } else if (maxValue > 0 && currentValue >= maxValue && !hasHapticAtBottom) {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        hasHapticAtBottom = true
                        hasHapticAtTop = false
                    } else if (currentValue > 0 && currentValue < maxValue) {
                        hasHapticAtTop = false
                        hasHapticAtBottom = false
                    }
                    lastScrollValue = currentValue
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // 图片画廊（支持自动轮播和手动切换）
                    preset?.let {
                        ImageGallery(
                            images = it.allImages,
                            modifier = Modifier.fillMaxWidth(),
                            autoPlayInterval = 3000L
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 预设信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = PresetI18n.getLocalizedPresetName(it.name),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (it.isHncs) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "👑 HNCS",
                                    color = HasselbladOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "@${it.author}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 标签
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            it.tags?.forEach { tag ->
                                Text(
                                    text = "#$tag",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 统计数据（使用预设真实数据）
                        PresetStatsCard(
                            downloads = it.downloads ?: 0,
                            rating = it.rating ?: 4.5f,
                            ratingCount = it.ratingCount ?: 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 拍摄建议
                        ShootingTipsDetailCard(
                            environment = "日间户外或充足自然光",
                            scenes = "街拍、人像、风景、建筑",
                            points = "适合追求经典胶片质感，建议使用黄金时刻拍摄",
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 动态参数展示
                        DynamicParameters(
                            sections = it.getDisplaySections(context),
                            presetName = it.name
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 关联推荐
                        RelatedPresetsCard(
                            presets = listOf(
                                RelatedPreset("r1", "电影胶片", ""),
                                RelatedPreset("r2", "复古人像", ""),
                                RelatedPreset("r3", "清新风景", ""),
                                RelatedPreset("r4", "黑白经典", "")
                            ),
                            onSelect = { id -> },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 用户评价
                        UserCommentsCard(
                            comments = listOf(
                                UserComment("c1", "摄影爱好者", "非常好用的预设！色彩还原很准确", 5),
                                UserComment("c2", "专业摄影师", "配合哈苏大师模式使用效果绝佳", 5)
                            ),
                            onViewAll = { },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 底部操作按钮（对齐用户规范）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 收藏按钮
                            FavoriteButton(
                                isFavorite = isFavorite,
                                onToggle = { viewModel.toggleFavorite() },
                                modifier = Modifier.weight(1f)
                            )
                            
                            // 应用按钮（带动画反馈）
                            ApplyPresetButton(
                                onApply = { },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 悬浮窗引导对话框 - 放在最外层确保显示在最上层
    if (showFloatingWindowGuide) {
        FloatingWindowGuideDialog(
            onDismiss = {
                showFloatingWindowGuide = false
                // 用户选择"以后再说"，只是关闭对话框，不执行任何操作
            },
            onGoToSettings = {
                showFloatingWindowGuide = false
                // 用户点击"去开启权限"，跳转到权限设置
                preset?.let { p ->
                    handleFloatingWindowClick(context, p)
                }
            }
        )
    }
}

/**
 * 处理悬浮窗按钮点击逻辑
 */
private fun handleFloatingWindowClick(
    context: android.content.Context,
    preset: MasterPreset
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(context)) {
            // 请求悬浮窗权限
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } else {
            // 使用全局控制器显示悬浮窗（预设列表已在 HomeScreen 中设置）
            FloatingWindowController.getInstance(context).showFloatingWindow(preset)
        }
    } else {
        FloatingWindowController.getInstance(context).showFloatingWindow(preset)
    }
}

@Composable
private fun DynamicParameters(
    sections: List<PresetSection>,
    presetName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Title
                section.title?.let { title ->
                    if (title.isNotEmpty()) {
                        SectionTitle(title = PresetI18n.resolveStringComposable(title))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Items
                val items = section.items
                var i = 0
                while (i < items.size) {
                    val item = items[i]
                    if (item.span == 2) {
                        // Full width
                        ParameterCard(
                            label = PresetI18n.resolveStringComposable(item.label),
                            value = PresetI18n.resolveValue(item.value),
                            modifier = Modifier.fillMaxWidth()
                        )
                        i++
                    } else {
                        // Half width
                        // Check next item
                        if (i + 1 < items.size && items[i + 1].span == 1) {
                            val nextItem = items[i + 1]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ParameterCard(
                                    label = PresetI18n.resolveStringComposable(item.label),
                                    value = PresetI18n.resolveValue(item.value),
                                    modifier = Modifier.weight(1f)
                                )
                                ParameterCard(
                                    label = PresetI18n.resolveStringComposable(nextItem.label),
                                    value = PresetI18n.resolveValue(nextItem.value),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            i += 2
                        } else {
                            // Only one half-width item left or next is full width
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ParameterCard(
                                    label = PresetI18n.resolveStringComposable(item.label),
                                    value = PresetI18n.resolveValue(item.value),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            i++
                        }
                    }
                }
            }
        }
    }
}


