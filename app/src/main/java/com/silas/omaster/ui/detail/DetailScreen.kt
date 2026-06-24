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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.local.FloatingWindowGuideManager
import com.silas.omaster.data.local.HistoryManager
import com.silas.omaster.camera.OPPOCameraManager
import com.silas.omaster.camera.CameraApplyResult
import com.silas.omaster.camera.ApplyMethod
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetSection
import com.silas.omaster.ui.components.FloatingWindowGuideDialog
import com.silas.omaster.ui.components.ImageGallery
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
import com.silas.omaster.R
import com.silas.omaster.util.PresetI18n
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform
import com.silas.omaster.ui.theme.HasselbladOrange

@Composable
fun DetailScreen(
    presetId: String,
    onBack: () -> Unit,
    onEdit: ((String) -> Unit)? = null,
    refreshTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // 支持在详情页内切换关联推荐预设
    var currentPresetId by remember { mutableStateOf(presetId) }

    // 使用 presetId 作为 key，确保每个预设有独立的 ViewModel
    val viewModel: DetailViewModel = viewModel(
        key = presetId,
        factory = DetailViewModelFactory(repository)
    )

    // 加载预设数据
    LaunchedEffect(currentPresetId) {
        viewModel.loadPreset(currentPresetId)
    }

    // 当 refreshTrigger 变化时重新加载数据（用于编辑后刷新）
    // 使用 snapshotFlow 确保持续监听，即使页面不可见时也能捕获变化
    var lastRefreshTrigger by remember { mutableIntStateOf(refreshTrigger) }
    LaunchedEffect(Unit) {
        snapshotFlow { refreshTrigger }
            .collect { newValue ->
                if (newValue != lastRefreshTrigger && newValue > 0) {
                    lastRefreshTrigger = newValue
                    viewModel.loadPreset(currentPresetId)
                }
            }
    }

    val preset by viewModel.preset.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    // 记录浏览历史（必须在 preset 声明之后）
    val historyManager = remember { HistoryManager.getInstance(context) }
    LaunchedEffect(preset) {
        preset?.let { p ->
            p.id?.let { id ->
                historyManager.record(id, p.name, "viewed")
            }
        }
    }

    // 悬浮窗引导对话框状态
    var showFloatingWindowGuide by remember { mutableStateOf(false) }
    val guideManager = remember { FloatingWindowGuideManager.getInstance(context) }

    // 悬浮窗控制器（全局单例，已在 MainActivity 中注册）
    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }

    // 对比视图状态
    var isComparing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OMasterTopAppBar(
            title = preset?.let { PresetI18n.getLocalizedPresetName(it.name) } ?: stringResource(R.string.detail_title),
            subtitle = preset?.author,
            onBack = {
                onBack()
            },
            actions = {
                // 分享按钮
                IconButton(
                    onClick = {
                        preset?.let { p ->
                            sharePreset(context, p)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_preset),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

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
                        tint = HasselbladOrange
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
                        tint = MaterialTheme.colorScheme.onBackground
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
                        tint = if (isFavorite) HasselbladOrange else MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                val scrollState = rememberScrollState()

                // 滚动到顶/底部震感
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
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // 图片画廊（支持自动轮播和手动切换）
                    preset?.let {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ImageGallery(
                                images = it.allImages,
                                modifier = Modifier.fillMaxWidth(),
                                autoPlayInterval = 3000L
                            )
                            
                            // 对比视图切换按钮
                            androidx.compose.material3.FilledTonalButton(
                                onClick = {
                                    haptic.perform(HapticFeedbackType.LongPress)
                                    isComparing = !isComparing
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Compare,
                                    contentDescription = "对比",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isComparing) "原图" else "对比",
                                    fontSize = 12.sp
                                )
                            }

                            // 对比模式：半透明遮罩 + 标签
                            if (isComparing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = "原始效果",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.Black.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        androidx.compose.material3.Text(
                                            text = "对比模式 - 点击按钮切换回预设效果",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 预设信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                            text = PresetI18n.getLocalizedPresetName(it.name),
                            color = MaterialTheme.colorScheme.onBackground,
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 统计数据（使用预设真实数据，无数据时不显示）
                        if (it.rating != null || it.downloads != null) {
                            PresetStatsCard(
                                downloads = it.downloads ?: 0,
                                rating = it.rating ?: 0f,
                                ratingCount = it.ratingCount ?: 0,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // 拍摄建议（从预设描述中读取真实数据）
                        val desc = it.description
                        val tips = it.shootingTips
                        if (desc != null || tips != null) {
                            ShootingTipsDetailCard(
                                environment = desc?.title ?: tips ?: "",
                                scenes = desc?.content ?: "",
                                points = tips ?: "",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // 动态参数展示
                        DynamicParameters(
                            sections = it.getDisplaySections(context),
                            presetName = it.name
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 关联推荐（从仓库获取同品牌/同标签的真实预设）
                        val relatedPresets = remember(it.id, it.brand, it.tags) {
                            repository.getRelatedPresets(it.id, it.brand, it.tags, limit = 4)
                        }
                        if (relatedPresets.isNotEmpty()) {
                            RelatedPresetsCard(
                                presets = relatedPresets.map { rp ->
                                    RelatedPreset(rp.id ?: "", rp.name, rp.coverPath)
                                },
                                onSelect = { id ->
                                    if (id.isNotBlank()) {
                                        currentPresetId = id
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // 用户评价（从预设评论数据中读取真实评论）
                        val comments = it.comments
                        if (!comments.isNullOrEmpty()) {
                            UserCommentsCard(
                                comments = comments.map { c ->
                                    UserComment(c.id, c.user, c.content, c.rating.toInt())
                                },
                                onViewAll = { },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
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
                                onApply = {
                                    preset?.let { p ->
                                        applyPresetParameters(context, p)
                                        handleFloatingWindowClick(context, p)
                                    }
                                },
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

/**
 * 将预设参数通过 OPPOCameraManager 应用到 OPPO 相机大师模式。
 * 按优先级依次尝试：ContentProvider → System Settings → Camera Intent → Clipboard
 */
private fun applyPresetParameters(context: android.content.Context, preset: MasterPreset) {
    val cameraManager = OPPOCameraManager.getInstance(context)
    val result = cameraManager.applyPreset(preset)

    when (result) {
        is CameraApplyResult.Success -> {
            val methodLabel = when (result.method) {
                ApplyMethod.CONTENT_PROVIDER -> "OPPO 大师模式"
                ApplyMethod.SYSTEM_SETTINGS -> "系统设置"
                ApplyMethod.CAMERA_INTENT -> "相机 Intent"
                ApplyMethod.CLIPBOARD_FALLBACK -> "剪贴板"
            }
            Toast.makeText(context, "已通过 $methodLabel 应用预设：${preset.name}", Toast.LENGTH_SHORT).show()
        }
        is CameraApplyResult.PartialSuccess -> {
            Toast.makeText(context, "部分参数已应用：${preset.name}（${result.failedParams.joinToString()} 未生效）", Toast.LENGTH_LONG).show()
        }
        is CameraApplyResult.Failed -> {
            Toast.makeText(context, "应用失败：${result.reason}\n${result.suggestion}", Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * 解析快门速度字符串（如 "1/500s" -> 500.0f，"1/60s" -> 60.0f）
 */
private fun parseShutterSpeed(shutter: String?): Float {
    if (shutter.isNullOrBlank()) return 125f
    val cleaned = shutter.replace(Regex("[^0-9/]"), "")
    return if (cleaned.contains("/")) {
        cleaned.substringAfter("/").toFloatOrNull() ?: 125f
    } else {
        cleaned.toFloatOrNull() ?: 125f
    }
}

/**
 * 分享预设参数
 */
private fun sharePreset(context: android.content.Context, preset: MasterPreset) {
    val paramsBuilder = StringBuilder()
    preset.getDisplaySections(context).forEach { section ->
        section.title?.let { title ->
            if (title.isNotEmpty()) {
                paramsBuilder.appendLine("【${com.silas.omaster.util.PresetI18n.resolveString(context, title)}】")
            }
        }
        section.items.forEach { item ->
            val label = com.silas.omaster.util.PresetI18n.resolveString(context, item.label)
            val value = com.silas.omaster.util.PresetI18n.resolveValue(context, item.value)
            paramsBuilder.appendLine("  $label: $value")
        }
    }

    val shareText = buildString {
        appendLine("🎨 ${preset.name} by ${preset.author}")
        appendLine("📱 OMaster 大师模式预设")
        appendLine()
        if (paramsBuilder.isNotEmpty()) {
            appendLine("参数:")
            append(paramsBuilder.toString())
            appendLine()
        }
        append("下载 OMaster 体验更多预设")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "分享预设"))
    } catch (e: Exception) {
        Toast.makeText(context, "未找到可分享的应用", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DynamicParameters(
    sections: List<PresetSection>,
    presetName: String
) {
    // 如果没有参数数据，显示提示
    if (sections.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无调色参数",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        return
    }

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


