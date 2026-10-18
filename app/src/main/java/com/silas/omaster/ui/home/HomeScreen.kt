package com.silas.omaster.ui.home

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.R
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.animation.ListItemFadeInSpec
import com.silas.omaster.ui.animation.ListItemPlacementSpec
import com.silas.omaster.ui.animation.calculateStaggerDelay
import com.silas.omaster.ui.components.HomeTabRow
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.util.DeviceDetector
import com.silas.omaster.util.hapticClickable
import com.silas.omaster.util.perform
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (MasterPreset) -> Unit,
    onNavigateToCreate: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    refreshTrigger: Int = 0,
    usePremiumGlass: Boolean = true
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository, context.applicationContext as Application)
    )
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val allPresets by viewModel.allPresets.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    // ═══════════════════════════════════════════════════════════
    // FD-10 / v1.6：多维筛选 + 搜索 + 排序 状态
    // Tab 0（全部预设）使用 filteredPresets；Tab 1/2 保持原样
    // ═══════════════════════════════════════════════════════════
    val filteredPresets by viewModel.filteredPresets.collectAsState()
    val availableFilters by viewModel.availableFilters.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val showNewOnly by viewModel.showNewOnly.collectAsState()
    // 当 refreshTrigger 变化时刷新数据
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.refresh()
        }
    }

    // 读取默认启动 Tab 设置
    val config = remember { ConfigCenter.getInstance(context) }
    val defaultStartTab by config.defaultStartTabFlow.collectAsState()

    val pagerState = rememberPagerState(initialPage = defaultStartTab, pageCount = { 3 })

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
    
    // 提取 usePremiumGlass 为局部变量，便于在 lambda 中使用
    val usePremiumGlassLocal = usePremiumGlass

    // 同步 Tab 和 Pager 的状态
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            // 禁用动画，直接跳转，避免动画过程中的中间状态导致页面卡住
            pagerState.scrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    // 当从其他页面返回主页时，直接跳转到默认启动页，无动画
    LaunchedEffect(Unit) {
        if (selectedTab != defaultStartTab) {
            // 先同步 ViewModel 状态
            viewModel.selectTab(defaultStartTab)
            // 直接跳转，无动画
            pagerState.scrollToPage(defaultStartTab)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themedBackground())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OMasterTopAppBar(
                title = stringResource(R.string.app_name),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )

            HomeTabRow(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    scope.launch {
                        pagerState.scrollToPage(index)
                    }
                    viewModel.selectTab(index)
                },
                tabs = listOf(
                    stringResource(R.string.tab_all) to allPresets.size,
                    stringResource(R.string.tab_favorites) to favorites.size,
                    stringResource(R.string.tab_my) to customPresets.size
                )
            )

            // ═══════════════════════════════════════════════════════════
            // FD-04 / MU-21 v1.6：机型感知精选 Banner
            // 当远程订阅里有预设声明 deviceProfile 并匹配当前设备时显示。
            // 例如 Find X10 用户看到"为 Find X10 · 哈苏 2亿像素 精选"。
            // 无匹配预设时 Banner 隐藏（不占位）。
            // v1.6.10：哈苏影像 / ColorOS 17 适配徽章 + 真实封面图 + 多语言
            // ═══════════════════════════════════════════════════════════
            val recommended by viewModel.deviceRecommendedPresets.collectAsState()
            val deviceDp = DeviceDetector.getCurrentDevice(LocalContext.current)
            // 精选展示条件：预设里至少有 3 个是当前设备专属（有 deviceProfile 匹配）
            val deviceSpecificCount = recommended.count { preset ->
                preset.deviceProfile != null || !preset.deviceSupport.isNullOrEmpty()
            }
            if (deviceSpecificCount >= 3) {
                DeviceRecommendedBanner(
                    deviceDisplayName = viewModel.deviceDisplayName,
                    is200Mp = deviceDp.has200Mp,
                    hasProImaging = deviceDp.isProImaging,
                    colorOsVersion = deviceDp.colorOsVersion,
                    presets = recommended,
                    onPresetClick = onNavigateToDetail,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ═══════════════════════════════════════════════════════════
            // FD-10 / v1.6：首页多维筛选栏
            // 搜索 + 风格 ChipGroup（多选 OR）+ 排序下拉 + 收藏/新预设 Toggle
            // 只在 Tab 0（全部预设）显示；Tab 切换时状态保留
            // ═══════════════════════════════════════════════════════════
            if (selectedTab == 0) {
                SearchFilterBar(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    availableFilters = availableFilters,
                    selectedFilters = selectedFilters,
                    onToggleFilter = { viewModel.toggleFilter(it) },
                    sortOrder = sortOrder,
                    onSortChange = { viewModel.setSortOrder(it) },
                    showFavoritesOnly = showFavoritesOnly,
                    onToggleFavoritesOnly = { viewModel.toggleShowFavoritesOnly() },
                    showNewOnly = showNewOnly,
                    onToggleNewOnly = { viewModel.toggleShowNewOnly() },
                    presetCount = filteredPresets.size,
                    onClearAll = { viewModel.clearAllFilters() }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // 使用 key 确保切换页面时重新创建组件，触发动画
                androidx.compose.runtime.key(page) {
                    when (page) {
                        0 -> PresetGrid(
                            presets = filteredPresets,
                            tabIndex = 0,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeletePreset = {
                                presetToDelete = it
                                showDeleteConfirm = true
                            },
                            onScrollStateChanged = onScrollStateChanged,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            usePremiumGlass = usePremiumGlass
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
                            usePremiumGlass = usePremiumGlass
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
                            usePremiumGlass = usePremiumGlass
                        )
                    }
                }
            }
        }

        // 悬浮添加按钮（只在"我的"Tab显示）
        if (selectedTab == 2) {
            FloatingActionButton(
                onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = AppDesign.ButtonShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AppDesign.ScreenPadding, bottom = 100.dp)
                    .size(AppDesign.FABSize + 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_preset),
                    modifier = Modifier.size(AppDesign.FABSize / 2 + 4.dp)
                )
            }
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
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.primary)
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

// ═══════════════════════════════════════════════════════════
// FD-04 / MU-21 v1.6：机型感知精选 Banner（可横滑）
// v1.6.10 改善（Find X10 / ColorOS 17 对齐）：
// 1) 特色徽章多语言化，新增 "ColorOS 17 适配" 徽章（colorOsVersion ≥ 17 时显示）
// 2) 专业影像机型（哈苏）显示主题色高亮徽章
// 3) 预设缩略图改用 PresetImage 真实封面（本地缓存优先，替代纯色块）
// 4) 卡片圆角对齐 ColorOS 大圆角语言（20dp）
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceRecommendedBanner(
    deviceDisplayName: String,
    is200Mp: Boolean,
    hasProImaging: Boolean,
    colorOsVersion: Int,
    presets: List<MasterPreset>,
    onPresetClick: (MasterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipTexts = buildList {
        if (hasProImaging) add(stringResource(R.string.home_banner_chip_pro_imaging))
        if (is200Mp) add(stringResource(R.string.home_banner_chip_200mp))
        if (colorOsVersion >= 17) add(stringResource(R.string.home_banner_chip_coloros, colorOsVersion))
        add(stringResource(R.string.home_banner_chip_curated))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题行：设备名 + 哈苏高亮标识
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_banner_title_for_device, deviceDisplayName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (hasProImaging) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "H",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 特色标签
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chipTexts.take(4).forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = chip,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 横滑预设列表（真实封面，本地缓存优先）
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets.take(10)) { preset ->
                    RecommendedPresetChip(
                        preset = preset,
                        onClick = { onPresetClick(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedPresetChip(
    preset: MasterPreset,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .width(96.dp)
                .padding(8.dp)
        ) {
            // 预设真实封面（与主页网格一致的加载策略）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier.fillMaxSize(),
                    showDownloadIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            preset.filter?.let { filter ->
                Text(
                    text = filter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

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
    onRefresh: (onComplete: (HomeViewModel.RefreshResult) -> Unit) -> Unit = {},
    usePremiumGlass: Boolean = true
) {
    val listState = rememberLazyStaggeredGridState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Pull-to-refresh state
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        onRefresh { result ->
            refreshing = false
            // 显示刷新结果 Toast
            val message = when (result) {
                is HomeViewModel.RefreshResult.Success -> "成功更新 ${result.count} 个订阅"
                is HomeViewModel.RefreshResult.PartialUpdate -> "成功更新 ${result.updated} 个，${result.upToDate} 个已是最新"
                is HomeViewModel.RefreshResult.UpToDate -> "所有订阅均已是最新"
                is HomeViewModel.RefreshResult.Failed -> "更新失败，请检查网络"
                is HomeViewModel.RefreshResult.NoSubscriptions -> ""
            }
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    })

    // Remove the problematic LaunchedEffect
    // LaunchedEffect(presets) {
    //    if (refreshing) refreshing = false
    // }

    // 修复：使用 snapshotFlow 安全地检测滚动方向
    // 避免在 derivedStateOf 中修改外部状态
    var isScrollingUp by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var hasHapticAtTop by remember { mutableStateOf(false) }
    var hasHapticAtBottom by remember { mutableStateOf(false) }
    // 标记是否是首次收集，避免初始化时的错误判断
    var isInitialCollection by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            // 首次收集时，仅记录状态，不触发回调
            if (isInitialCollection) {
                isInitialCollection = false
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
                // 页面重建时默认显示导航栏（无论是否在顶部）
                isScrollingUp = true
                onScrollStateChanged(true)
                return@collect
            }

            val isUp = currentIndex < previousIndex ||
                       (currentIndex == previousIndex && currentOffset <= previousScrollOffset)
            isScrollingUp = isUp
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
            onScrollStateChanged(isUp)

            // 滚动到顶部或底部时触发震感
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            if (currentIndex == 0 && !hasHapticAtTop) {
                // 滚动到顶部
                haptic.perform(HapticFeedbackType.TextHandleMove)
                hasHapticAtTop = true
                hasHapticAtBottom = false
            } else if (lastVisibleItem >= totalItems - 1 && totalItems > 0 && !hasHapticAtBottom) {
                // 滚动到底部（最后一个可见 item 是最后一个 item）
                haptic.perform(HapticFeedbackType.TextHandleMove)
                hasHapticAtBottom = true
                hasHapticAtTop = false
            } else if (currentIndex > 0 && lastVisibleItem < totalItems - 1) {
                // 在中间位置，重置状态
                hasHapticAtTop = false
                hasHapticAtBottom = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (presets.isEmpty()) {
            // 使用 LazyColumn 确保即使为空也能触发下拉刷新
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    EmptyState(tabIndex)
                }
            }
        } else {
            // 缓存可见区域起始索引，避免每次重组都计算
            val visibleStartIndex by remember {
                derivedStateOf {
                    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,  // 优化：顶部边距增加，与 Tab 栏呼吸感更好
                    bottom = 80.dp  // 优化：底部边距减少，更紧凑
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                if (showTopHint) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.feature_coming_soon),
                                style = MaterialTheme.typography.bodyMedium,
                                color = themedTextSecondary()
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = presets,
                    key = { index, preset -> preset.id?.let { "${it}_$index" } ?: "preset_$index" }
                ) { index, preset ->
                    // 优化：更 subtle 的高度差，视觉更和谐
                    val imageHeight = remember(index) {
                        when (index % 4) {
                            0 -> 200  // 标准
                            1 -> 170  // 稍矮
                            2 -> 230  // 稍高
                            else -> 190  // 接近标准
                        }
                    }

                    if (preset.id != null) {
                        // 使用缓存的 visibleStartIndex 计算延迟
                        // 优化：只有在列表顶部时才应用交错延迟，滚动时立即显示，避免卡顿感
                        val delayMillis = if (visibleStartIndex == 0) {
                            calculateStaggerDelay(index, visibleStartIndex)
                        } else {
                            0
                        }

                        PresetCardItem(
                            preset = preset,
                            index = index,
                            tabIndex = tabIndex,
                            imageHeight = imageHeight,
                            delayMillis = delayMillis,
                            onNavigateToDetail = onNavigateToDetail,
                            onToggleFavorite = onToggleFavorite,
                            onDeletePreset = onDeletePreset,
                            usePremiumGlass = usePremiumGlass,
                            modifier = Modifier.animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = ListItemPlacementSpec
                            )
                        )
                    }
                }

                // 底部提示（仅在全部预设页面显示）
                if (showLoadingTip) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LoadingMoreTip()
                    }
                }
            }
        }

        // Pull refresh indicator overlay
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PresetCardItem(
    preset: MasterPreset,
    index: Int,
    tabIndex: Int,
    imageHeight: Int,
    delayMillis: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    usePremiumGlass: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 使用统一的动画状态管理，减少 Animatable 实例
    val animatedProgress = remember(preset.id, tabIndex) { Animatable(0f) }

    LaunchedEffect(preset.id, tabIndex) {
        // 添加延迟，实现错开动画效果
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = AnimationSpecs.CardSpring
        )
    }

    // 使用 graphicsLayer 进行硬件加速友好的变换
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
            }
    ) {
        PresetCard(
            preset = preset,
            onClick = { onNavigateToDetail(preset) },
            onFavoriteClick = { onToggleFavorite(preset.id!!) },
            onDeleteClick = { onDeletePreset(preset.id!!) },
            showFavoriteButton = true,
            showDeleteButton = tabIndex == 2,
            imageHeight = imageHeight,
            usePremiumGlass = usePremiumGlass
        )
    }
}

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // 主题色圆底图标，对齐 ColorOS 空态视觉语言
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = themedTextSecondary(),
                textAlign = TextAlign.Center
            )
            if (subMessage.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 底部加载更多提示 - 持续更新 敬请期待
 */
@Composable
private fun LoadingMoreTip() {
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
            // 装饰线条
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 主文字
            Text(
                text = stringResource(R.string.load_more_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            // 装饰线条
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// FD-10 / v1.6：搜索 + 多维筛选 + 排序 一体化栏
// 位置：HomeTabRow → DeviceRecommendedBanner → SearchFilterBar → HorizontalPager
// Tab 0 专属；Tab 切换时状态保留（不重建 Composable，只是条件渲染）
// ═══════════════════════════════════════════════════════════

@Composable
private fun SearchFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    availableFilters: List<String>,
    selectedFilters: Set<String>,
    onToggleFilter: (String) -> Unit,
    sortOrder: HomeViewModel.SortOrder,
    onSortChange: (HomeViewModel.SortOrder) -> Unit,
    showFavoritesOnly: Boolean,
    onToggleFavoritesOnly: () -> Unit,
    showNewOnly: Boolean,
    onToggleNewOnly: () -> Unit,
    presetCount: Int,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var sortExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDesign.ContentPadding),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = AppDesign.CardShape
    ) {
        Column(modifier = Modifier.padding(AppDesign.ContentPadding)) {

            // ① 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_preset_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = themedTextSecondary()
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear),
                                tint = themedTextSecondary()
                            )
                        }
                    }
                },
                shape = AppDesign.ButtonShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = themedBackground(),
                    unfocusedContainerColor = themedBackground(),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = themedTextSecondary().copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ② 风格 ChipGroup（多选，滚动横排）
            if (availableFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing / 2)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing / 2),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(availableFilters) { label ->
                            val selected = label in selectedFilters
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    haptic.perform(HapticFeedbackType.ToggleOn)
                                    onToggleFilter(label)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    selectedLabelColor = Color.White,
                                    containerColor = themedCardBackground(),
                                    labelColor = themedTextPrimary()
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ③ 排序下拉 + 快捷 Toggle + 计数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 排序小下拉按钮
                    Box {
                        TextButton(
                            onClick = { sortExpanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(R.string.sort_default),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (sortOrder) {
                                    HomeViewModel.SortOrder.DEFAULT -> stringResource(R.string.sort_default)
                                    HomeViewModel.SortOrder.NEW_FIRST -> stringResource(R.string.sort_new_first)
                                    HomeViewModel.SortOrder.FAVORITE_FIRST -> stringResource(R.string.sort_favorite_first)
                                    HomeViewModel.SortOrder.NAME_A_Z -> stringResource(R.string.sort_name_a_z)
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            HomeViewModel.SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (order) {
                                                HomeViewModel.SortOrder.DEFAULT -> stringResource(R.string.sort_default)
                                                HomeViewModel.SortOrder.NEW_FIRST -> stringResource(R.string.sort_new_first)
                                                HomeViewModel.SortOrder.FAVORITE_FIRST -> stringResource(R.string.sort_favorite_first)
                                                HomeViewModel.SortOrder.NAME_A_Z -> stringResource(R.string.sort_name_a_z)
                                            }
                                        )
                                    },
                                    onClick = {
                                        haptic.perform(HapticFeedbackType.SegmentTick)
                                        onSortChange(order)
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 收藏 Toggle（Pill）
                    val favChipColor = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else themedCardBackground()
                    val favTextColor = if (showFavoritesOnly) Color.White else themedTextPrimary()
                    Surface(
                        shape = AppDesign.PillShape,
                        color = favChipColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.clickable {
                            haptic.perform(HapticFeedbackType.ToggleOn)
                            onToggleFavoritesOnly()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.filter_favorites_only),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = favTextColor
                        )
                    }

                    // 新预设 Toggle
                    val newChipColor = if (showNewOnly) MaterialTheme.colorScheme.primary else themedCardBackground()
                    val newTextColor = if (showNewOnly) Color.White else themedTextPrimary()
                    Surface(
                        shape = AppDesign.PillShape,
                        color = newChipColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier.clickable {
                            haptic.perform(HapticFeedbackType.ToggleOn)
                            onToggleNewOnly()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.filter_new_only),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = newTextColor
                        )
                    }
                }

                // 右侧：数量 + 一键清除
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasActiveFilter = searchQuery.isNotBlank() || selectedFilters.isNotEmpty() ||
                            showFavoritesOnly || showNewOnly || sortOrder != HomeViewModel.SortOrder.DEFAULT
                    Text(
                        text = context.getString(R.string.filter_result_count, presetCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary()
                    )
                    if (hasActiveFilter) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { onClearAll() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                stringResource(R.string.clear_all),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
