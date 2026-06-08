package com.silas.omaster.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.silas.omaster.R
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.animation.ListItemFadeInSpec
import com.silas.omaster.ui.animation.ListItemPlacementSpec
import com.silas.omaster.ui.animation.calculateStaggerDelay
import com.silas.omaster.ui.animation.HoverEffects
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.components.GlassCard
import com.silas.omaster.ui.components.GlassChip
import com.silas.omaster.ui.components.GlassBadge
import com.silas.omaster.ui.components.HncsBadge
import com.silas.omaster.ui.components.NewBadge
import com.silas.omaster.ui.components.GlassIconContainer
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.*
import com.silas.omaster.ui.utils.rememberScreenSizeInfo
import com.silas.omaster.ui.utils.DeviceType
import com.silas.omaster.ui.utils.responsivePadding
import com.silas.omaster.ui.utils.responsiveSpacing
import com.silas.omaster.ui.utils.responsiveCornerRadius
import com.silas.omaster.util.hapticClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border

/**
 * ============================================
 * 首页 - 小O帮帮
 * ColorOS 16 液态玻璃风格
 * 多设备分辨率适配
 * ============================================
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (MasterPreset) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSceneRecognition: () -> Unit = {},
    onNavigateToAIFineTune: () -> Unit = {},
    onNavigateToWatermarkEditor: () -> Unit = {},
    onNavigateToSmartOptimize: () -> Unit = {},
    onNavigateToPresetManager: () -> Unit = {},
    onNavigateToParamAdjustment: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository)
    )
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // 响应式布局参数
    val screenSizeInfo = rememberScreenSizeInfo()
    val padding = responsivePadding()
    val spacing = responsiveSpacing()
    val cornerRadius = responsiveCornerRadius()

    val allPresets by viewModel.allPresets.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    
    // 当 refreshTrigger 变化时刷新数据
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.refresh()
        }
    }

    // 读取默认启动 Tab 设置
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val defaultStartTab = remember { settingsManager.defaultStartTab }
    
    val pagerState = rememberPagerState(initialPage = defaultStartTab, pageCount = { 3 })
    
    // 初始化时同步默认 Tab
    LaunchedEffect(Unit) {
        if (selectedTab != defaultStartTab) {
            viewModel.selectTab(defaultStartTab)
        }
    }

    // 全局悬浮窗控制器
    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }

    // 当预设列表或选中的 Tab 变化时，更新到全局控制器
    LaunchedEffect(allPresets, favorites, customPresets, selectedTab) {
        val currentList = when (selectedTab) {
            0 -> allPresets
            1 -> favorites
            2 -> customPresets
            else -> allPresets
        }
        floatingWindowController.setPresetList(currentList)
    }

    // 删除确认对话框状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<String?>(null) }

    // 同步 Tab 和 Pager 的状态
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(
                selectedTab,
                animationSpec = AnimationSpecs.LiquidSlideSpec
            )
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 液态玻璃标题栏
            GlassHeader(
                padding = padding,
                screenSizeInfo = screenSizeInfo
            )

            // 功能入口卡片区域 - 液态玻璃风格
            FeatureEntryRow(
                onNavigateToSceneRecognition = onNavigateToSceneRecognition,
                onNavigateToAIFineTune = onNavigateToAIFineTune,
                onNavigateToWatermarkEditor = onNavigateToWatermarkEditor,
                onNavigateToSmartOptimize = onNavigateToSmartOptimize,
                onNavigateToPresetManager = onNavigateToPresetManager,
                onNavigateToParamAdjustment = onNavigateToParamAdjustment,
                padding = padding,
                spacing = spacing,
                screenSizeInfo = screenSizeInfo
            )

            // Tab 切换栏 - 液态玻璃风格
            GlassTabRow(
                selectedTab = selectedTab,
                pagerState = pagerState,
                allPresetsSize = allPresets.size,
                favoritesSize = favorites.size,
                customPresetsSize = customPresets.size,
                onTabSelected = { index ->
                    haptic.perform(HapticFeedbackType.ToggleOn)
                    scope.launch {
                        pagerState.scrollToPage(index)
                    }
                    viewModel.selectTab(index)
                },
                padding = padding
            )

            // 可滑动的页面内容
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                androidx.compose.runtime.key(page) {
                    when (page) {
                        0 -> PresetGrid(
                            presets = allPresets,
                            tabIndex = 0,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeletePreset = {
                                presetToDelete = it
                                showDeleteConfirm = true
                            },
                            onScrollStateChanged = onScrollStateChanged,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            padding = padding,
                            spacing = spacing,
                            screenSizeInfo = screenSizeInfo
                        )
                        1 -> PresetGrid(
                            presets = favorites,
                            tabIndex = 1,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeletePreset = {
                                presetToDelete = it
                                showDeleteConfirm = true
                            },
                            onScrollStateChanged = onScrollStateChanged,
                            showLoadingTip = false,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            padding = padding,
                            spacing = spacing,
                            screenSizeInfo = screenSizeInfo
                        )
                        2 -> PresetGrid(
                            presets = customPresets,
                            tabIndex = 2,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeletePreset = {
                                presetToDelete = it
                                showDeleteConfirm = true
                            },
                            showLoadingTip = false,
                            showTopHint = false,
                            onScrollStateChanged = onScrollStateChanged,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            padding = padding,
                            spacing = spacing,
                            screenSizeInfo = screenSizeInfo
                        )
                    }
                }
            }
        }

        // 悬浮添加按钮 - 液态玻璃风格
        if (selectedTab == 2) {
            GlassFloatingActionButton(
                onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onNavigateToCreate()
                },
                screenSizeInfo = screenSizeInfo
            )
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                presetToDelete = null
            },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = { Text(stringResource(R.string.delete_preset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        val id = presetToDelete
                        if (id != null) {
                            viewModel.deleteCustomPreset(id)
                        }
                        showDeleteConfirm = false
                        presetToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = HasselbladOrange)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        showDeleteConfirm = false
                        presetToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 液态玻璃标题栏
 */
@Composable
private fun GlassHeader(
    padding: androidx.compose.ui.unit.Dp,
    screenSizeInfo: com.silas.omaster.ui.utils.ScreenSizeInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = padding, vertical = 8.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NearBlack.copy(alpha = 0.9f),
                        NearBlack.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            // 哈苏大师徽章
            GlassBadge(
                text = "哈苏大师",
                color = HasselbladOrange,
                modifier = Modifier
            )
        }
    }
}

/**
 * 液态玻璃Tab栏
 */
@Composable
private fun GlassTabRow(
    selectedTab: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    allPresetsSize: Int,
    favoritesSize: Int,
    customPresetsSize: Int,
    onTabSelected: (Int) -> Unit,
    padding: androidx.compose.ui.unit.Dp
) {
    val haptic = LocalHapticFeedback.current
    
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = PureBlack,
        contentColor = HasselbladOrange,
        edgePadding = padding,
        modifier = Modifier.height(44.dp),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTab])
                    .height(3.dp)
                    .shadow(4.dp, RoundedCornerShape(50), ambientColor = HasselbladOrange.copy(alpha = 0.4f)),
                color = HasselbladOrange
            )
        },
        divider = {}
    ) {
        val tabs = listOf(
            stringResource(R.string.tab_all) to allPresetsSize,
            stringResource(R.string.tab_favorites) to favoritesSize,
            stringResource(R.string.tab_my) to customPresetsSize
        )
        
        tabs.forEachIndexed { index, (title, count) ->
            val isSelected = selectedTab == index
            
            val textColor by animateColorAsState(
                targetValue = if (isSelected) HasselbladOrange else TextTertiary,
                animationSpec = AnimationSpecs.FastTween,
                label = "tab_text_color"
            )
            
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (count > 0) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                color = if (isSelected) HasselbladOrange.copy(alpha = 0.8f) else TextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

/**
 * 液态玻璃悬浮按钮
 */
@Composable
private fun GlassFloatingActionButton(
    onClick: () -> Unit,
    screenSizeInfo: com.silas.omaster.ui.utils.ScreenSizeInfo
) {
    val buttonSize = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 56.dp
        DeviceType.TABLET -> 64.dp
        DeviceType.DESKTOP -> 72.dp
        else -> 56.dp
    }
    
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) HoverEffects.PressScaleMultiplier else 1f,
        animationSpec = AnimationSpecs.SpringSoftTween,
        label = "fab_scale"
    )
    
    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
        },
        containerColor = HasselbladOrange,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(20.dp),
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 24.dp, bottom = 100.dp)
            .size(buttonSize)
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = HasselbladOrange.copy(alpha = 0.3f))
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.create_preset),
            modifier = Modifier.size(buttonSize * 0.5f)
        )
    }
}

/**
 * 功能入口卡片行 - 液态玻璃风格
 */
@Composable
private fun FeatureEntryRow(
    onNavigateToSceneRecognition: () -> Unit,
    onNavigateToAIFineTune: () -> Unit,
    onNavigateToWatermarkEditor: () -> Unit,
    onNavigateToSmartOptimize: () -> Unit,
    onNavigateToPresetManager: () -> Unit,
    onNavigateToParamAdjustment: () -> Unit,
    padding: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    screenSizeInfo: com.silas.omaster.ui.utils.ScreenSizeInfo
) {
    val haptic = LocalHapticFeedback.current
    
    // 根据设备类型调整卡片数量和布局
    val cardCount = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 6
        DeviceType.TABLET -> 6
        DeviceType.DESKTOP -> 6
        else -> 6
    }
    
    val cardHeight = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 56.dp
        DeviceType.TABLET -> 64.dp
        DeviceType.DESKTOP -> 72.dp
        else -> 56.dp
    }
    
    val iconSize = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 20.dp
        DeviceType.TABLET -> 24.dp
        DeviceType.DESKTOP -> 28.dp
        else -> 20.dp
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing * 0.5f)
    ) {
        // AI场景识别
        GlassFeatureCard(
            title = "AI场景",
            icon = Icons.Default.CameraAlt,
            color = FeatureSceneGreen,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToSceneRecognition()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
        
        // AI微调
        GlassFeatureCard(
            title = "AI微调",
            icon = Icons.Default.ColorLens,
            color = FeatureAIPurple,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToAIFineTune()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
        
        // 水印编辑
        GlassFeatureCard(
            title = "水印",
            icon = Icons.Default.WaterDrop,
            color = FeatureWatermarkCyan,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToWatermarkEditor()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
        
        // 智能优化
        GlassFeatureCard(
            title = "优化",
            icon = Icons.Default.Memory,
            color = FeatureSyncBlue,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToSmartOptimize()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
        
        // 预设管理
        GlassFeatureCard(
            title = "预设",
            icon = Icons.Default.PhotoFilter,
            color = FeaturePresetOrange,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToPresetManager()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
        
        // 参数调节
        GlassFeatureCard(
            title = "参数",
            icon = Icons.Default.Tune,
            color = FeatureThemePink,
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToParamAdjustment()
            },
            height = cardHeight,
            iconSize = iconSize,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 液态玻璃功能入口卡片
 */
@Composable
private fun GlassFeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) HoverEffects.ScaleMultiplier else 1f,
        animationSpec = AnimationSpecs.SpringSoftTween,
        label = "feature_card_scale"
    )
    
    Card(
        modifier = modifier
            .height(height)
            .scale(scale)
            .hapticClickable { 
                isHovered = true
                onClick() 
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 预设网格 - 液态玻璃风格 + 响应式布局
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PresetGrid(
    presets: List<MasterPreset>,
    tabIndex: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {},
    showLoadingTip: Boolean = true,
    showTopHint: Boolean = false,
    onRefresh: (onComplete: () -> Unit) -> Unit = {},
    padding: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    screenSizeInfo: com.silas.omaster.ui.utils.ScreenSizeInfo
) {
    val listState = rememberLazyStaggeredGridState()
    val haptic = LocalHapticFeedback.current

    // Pull-to-refresh state
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        onRefresh { refreshing = false }
    })

    // 滚动状态检测
    var isScrollingUp by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            val isUp = currentIndex < previousIndex ||
                       (currentIndex == previousIndex && currentOffset <= previousScrollOffset)
            isScrollingUp = isUp
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
            onScrollStateChanged(isUp)
        }
    }

    // 响应式列数
    val columns = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 2
        DeviceType.MOBILE_LARGE -> 2
        DeviceType.TABLET -> 3
        DeviceType.TABLET_LARGE -> 4
        DeviceType.DESKTOP -> 6
        else -> 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (presets.isEmpty()) {
            EmptyState(tabIndex)
        } else {
            val visibleStartIndex by remember {
                derivedStateOf {
                    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                state = listState,
                contentPadding = PaddingValues(
                    start = padding,
                    end = padding,
                    top = 8.dp,
                    bottom = 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
                modifier = Modifier.fillMaxSize()
            ) {
                if (showTopHint) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GlassLoadingTip()
                    }
                }

                itemsIndexed(
                    items = presets,
                    key = { index, preset -> preset.id?.let { "${it}_$index" } ?: "preset_$index" }
                ) { index, preset ->
                    val imageHeight = remember(index) {
                        when (index % 3) {
                            0 -> 220
                            1 -> 180
                            else -> 260
                        }
                    }

                    if (preset.id != null) {
                        val delayMillis = if (visibleStartIndex == 0) {
                            calculateStaggerDelay(index, visibleStartIndex)
                        } else {
                            0
                        }

                        GlassPresetCardItem(
                            preset = preset,
                            index = index,
                            tabIndex = tabIndex,
                            imageHeight = imageHeight,
                            delayMillis = delayMillis,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = onToggleFavorite,
                            onDeletePreset = onDeletePreset,
                            modifier = Modifier.animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = ListItemPlacementSpec
                            )
                        )
                    }
                }

                if (showLoadingTip) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GlassLoadingTip()
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = HasselbladOrange
        )
    }
}

/**
 * 液态玻璃预设卡片项
 */
@Composable
private fun GlassPresetCardItem(
    preset: MasterPreset,
    index: Int,
    tabIndex: Int,
    imageHeight: Int,
    delayMillis: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember(preset.id, tabIndex) { Animatable(0f) }

    LaunchedEffect(preset.id, tabIndex) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = AnimationSpecs.CardSpring
        )
    }

    val alpha = animatedProgress.value
    val scale = 0.85f + (0.15f * animatedProgress.value)
    val translationY = (1f - animatedProgress.value) * 30f

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = translationY
                this.shadowElevation = if (alpha > 0.9f) 4f else 0f
            }
    ) {
        PresetCard(
            preset = preset,
            onClick = { onNavigateToDetail(preset) },
            onFavoriteClick = { onToggleFavorite(preset.id!!) },
            onDeleteClick = { onDeletePreset(preset.id!!) },
            showFavoriteButton = true,
            showDeleteButton = tabIndex == 2,
            imageHeight = imageHeight
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(tabIndex: Int) {
    val message = when (tabIndex) {
        0 -> stringResource(R.string.empty_no_presets)
        1 -> stringResource(R.string.empty_no_favorites)
        2 -> stringResource(R.string.empty_no_custom)
        else -> stringResource(R.string.empty_no_data)
    }

    val subMessage = when (tabIndex) {
        0 -> stringResource(R.string.empty_hint_add_presets)
        1 -> stringResource(R.string.empty_hint_favorite)
        2 -> stringResource(R.string.empty_hint_create)
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            if (subMessage.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HasselbladOrange.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 液态玻璃加载提示
 */
@Composable
private fun GlassLoadingTip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 装饰线条 - 液态玻璃风格
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                HasselbladOrange.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Text(
                text = stringResource(R.string.load_more_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = HasselbladOrange.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                HasselbladOrange.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}