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
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.silas.omaster.ui.components.AuroraRefreshIndicator
import com.silas.omaster.ui.components.FluidFab
import com.silas.omaster.ui.components.FluidFabAction
import com.silas.omaster.ui.components.rememberFrameDegrade
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
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
import com.silas.omaster.ui.animation.fluidPressScale
import com.silas.omaster.ui.components.HomeTabRow
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.util.DeviceDetector
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.hapticClickable
import com.silas.omaster.util.perform
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.FavoriteBorder
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
    usePremiumGlass: Boolean = true,
    // 【v1.6.14】悬浮按钮次级动作：剪贴板导入（由宿主传入；未传入时 FAB 无次级菜单）
    onNavigateToClipboardImport: (() -> Unit)? = null
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

    // 【v1.6.14】滚动方向状态：驱动流体 FAB 显隐（下滑隐藏 / 上滑显示）
    var isScrollingUp by remember { mutableStateOf(true) }
    val handleScrollStateChanged: (Boolean) -> Unit = { up ->
        isScrollingUp = up
        onScrollStateChanged(up)
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
                            onScrollStateChanged = handleScrollStateChanged,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            usePremiumGlass = usePremiumGlass,
                            // 【v1.6.13】搜索/筛选无结果 → 一键清除筛选
                            onClearFilters = if (searchQuery.isNotBlank() || selectedFilters.isNotEmpty() ||
                                showFavoritesOnly || showNewOnly
                            ) {
                                { viewModel.clearAllFilters() }
                            } else {
                                null
                            }
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
                            onScrollStateChanged = handleScrollStateChanged,
                            showLoadingTip = false,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            usePremiumGlass = usePremiumGlass,
                            // 【v1.6.13】空状态一键跳转全部预设
                            onEmptyBrowseAll = {
                                scope.launch { pagerState.scrollToPage(0) }
                                viewModel.selectTab(0)
                            }
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
                            onScrollStateChanged = handleScrollStateChanged,
                            onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                            usePremiumGlass = usePremiumGlass,
                            // 【v1.6.13】空状态直达创建入口
                            onEmptyCreate = onNavigateToCreate
                        )
                    }
                }
            }
        }

        // 【v1.6.14】流体展开悬浮按钮
        // 变更：① 全部 Tab 可见（v1.6.13 仅"我的"Tab），下滑隐藏 / 上滑显示；
        //      ② 按压时圆形液态延展为胶囊并显出文案（ColorOS 17 灵动胶囊语言）；
        //      ③ 长按展开次级动作（剪贴板导入），贴合系统长按范式。
        FluidFab(
            onClick = {
                haptic.perform(HapticFeedbackType.Confirm)
                onNavigateToCreate()
            },
            label = stringResource(R.string.create_preset),
            visible = isScrollingUp,
            actions = listOfNotNull(
                onNavigateToClipboardImport?.let { import ->
                    FluidFabAction(
                        label = stringResource(R.string.paste_import),
                        onClick = import
                    )
                }
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = AppDesign.ScreenPadding, bottom = 100.dp)
        )
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
// 【v1.6.13】ColorOS 17 重构：
//   1) 标题/标签全面多语言化（修复原硬编码中文）
//   2) 特色标签随 DeviceProfile 能力动态生成（哈苏专业影像 / 2亿像素）
//   3) 缩略图从主色方块占位升级为真实预设样片（复用 PresetImage 缓存管线）
//   4) Chip 按压液态缩放反馈（fluidPressScale）
// ═══════════════════════════════════════════════════════════

@Composable
private fun DeviceRecommendedBanner(
    deviceDisplayName: String,
    is200Mp: Boolean,
    hasProImaging: Boolean,
    presets: List<MasterPreset>,
    onPresetClick: (MasterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipTexts = buildList {
        if (hasProImaging) add(stringResource(R.string.banner_chip_pro_imaging))
        if (is200Mp) add(stringResource(R.string.banner_chip_200mp))
        add(stringResource(R.string.banner_chip_curated))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题行（i18n：为 Find X10 精选 / Curated for Find X10）
            Text(
                text = stringResource(R.string.banner_recommended_for, deviceDisplayName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 特色标签（随设备能力动态生成）
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chipTexts.take(3).forEach { chip ->
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

            // 横滑预设列表（真实样片）
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
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier.fluidPressScale(interactionSource, pressedScale = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .width(96.dp)
                .padding(8.dp)
        ) {
            // 【v1.6.13】真实样片（assets / 本地缓存 / 网络下载统一走 PresetImage 管线）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier.fillMaxSize(),
                    showDownloadIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = PresetI18n.getLocalizedPresetName(preset.name),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface
            )
            preset.filter?.let { filter ->
                Text(
                    text = PresetI18n.getLocalizedFilter(context, filter),
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
    usePremiumGlass: Boolean = true,
    onEmptyBrowseAll: (() -> Unit)? = null,
    onEmptyCreate: (() -> Unit)? = null,
    // 【v1.6.13】搜索/筛选无结果时非空：空状态切换为"未找到匹配"并给出一键清除筛选
    onClearFilters: (() -> Unit)? = null
) {
    val listState = rememberLazyStaggeredGridState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Pull-to-refresh state
    var refreshing by remember { mutableStateOf(false) }
    // 【v1.6.14】刷新完成态：用于追光指示器展示对勾（800ms 后自动复位）
    var refreshFinished by remember { mutableStateOf(false) }
    LaunchedEffect(refreshFinished) {
        if (refreshFinished) {
            delay(800)
            refreshFinished = false
        }
    }
    // 【v1.6.13 lint 修复】组合内通过 LocalResources 访问资源，回调内直接使用
    // (LocalContextGetResourceValueCall: 禁止经 LocalContext.current 查询资源)
    val resources = LocalResources.current
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        onRefresh { result ->
            refreshing = false
            refreshFinished = true
            // 【v1.6.13】刷新结果 Toast 多语言化（修复硬编码中文）
            val message = when (result) {
                is HomeViewModel.RefreshResult.Success ->
                    resources.getString(R.string.refresh_success, result.count)
                is HomeViewModel.RefreshResult.PartialUpdate ->
                    resources.getString(R.string.refresh_partial, result.updated, result.upToDate)
                is HomeViewModel.RefreshResult.UpToDate ->
                    resources.getString(R.string.refresh_up_to_date)
                is HomeViewModel.RefreshResult.Failed ->
                    resources.getString(R.string.refresh_failed)
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
                    // 【v1.6.13】可操作空状态：收藏页一键跳全部预设 / 我的页直达创建 / 搜索无结果一键清除筛选
                    EmptyState(
                        tabIndex = tabIndex,
                        onBrowseAll = onEmptyBrowseAll,
                        onCreatePreset = onEmptyCreate,
                        onClearFilters = onClearFilters
                    )
                }
            }
        } else {
            // 【v1.6.14】高刷掉帧降级：连续掉帧时暂停交错入场动画，保住滚动帧率
            val frameDegrade = rememberFrameDegrade(context = context, active = true)
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
                    // v1.6.15 性能修复：key 去掉 index 后缀。含 index 时搜索/排序后
                    // index 位移导致所有 item key 改变 → 状态丢失、复用与 animateItem 失效。
                    // id 由 processPresets 保证非空且唯一（缺失时回退 name 保证稳定）。
                    key = { _, preset -> preset.id ?: preset.name }
                ) { index, preset ->
                    // 【v1.6.14】确定性高度：以预设 id 为种子，同一预设在筛选/排序前后高度不变。
                    // （v1.6.13 用 index % 4，筛选后 index 位移会导致卡片高度集体跳动）
                    val imageHeight = remember(preset.id) {
                        val raw = preset.id?.hashCode() ?: index
                        when ((if (raw < 0) -raw else raw) % 4) {
                            0 -> 200  // 标准
                            1 -> 170  // 稍矮
                            2 -> 230  // 稍高
                            else -> 190  // 接近标准
                        }
                    }

                    if (preset.id != null) {
                        // 使用缓存的 visibleStartIndex 计算延迟
                        // 优化：只有在列表顶部时才应用交错延迟，滚动时立即显示，避免卡顿感
                        // 【v1.6.14】高刷设备掉帧时自动关闭交错动画（潮汐引擎式"前台流畅"）
                        val delayMillis = if (visibleStartIndex == 0 && !frameDegrade) {
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

        // 【v1.6.14】凝光追光刷新指示器（替代系统默认 PullRefreshIndicator）
        AuroraRefreshIndicator(
            progress = pullRefreshState.progress,
            refreshing = refreshing,
            finished = refreshFinished,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
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

    // 【v1.6.14】长按菜单「应用至悬浮窗」所需的控制器与资源句柄
    val cardContext = LocalContext.current
    val cardResources = LocalResources.current
    val floatingController = remember { FloatingWindowController.getInstance(cardContext) }

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
            // v1.6.15 修复：远程订阅/导入预设可能缺 id 字段，!! 断言会 NPE 崩溃。
            // id 为 null 时安全跳过（processPresets 已保证展示数据 id 非空，此处为纵深防御）。
            onFavoriteClick = { preset.id?.let { onToggleFavorite(it) } },
            onDeleteClick = { preset.id?.let { onDeletePreset(it) } },
            showFavoriteButton = true,
            showDeleteButton = tabIndex == 2,
            imageHeight = imageHeight,
            usePremiumGlass = usePremiumGlass,
            // 【v1.6.14】一步直达悬浮窗（无需先进详情）：失败时提示，不静默吞掉
            onApplyToFloat = {
                runCatching {
                    floatingController.showFloatingWindow(preset)
                }.onFailure {
                    Toast.makeText(
                        cardContext,
                        cardResources.getString(R.string.preset_apply_float_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

/**
 * 【v1.6.13】空状态可操作化：
 * - 图标 + 主/副文案 + 动作按钮（收藏页 → 跳全部预设；我的页 → 直达创建）
 * - 全部预设页给下拉刷新提示（对齐 ColorOS 17 "给用户下一步" 的引导原则）
 * - 搜索/筛选无结果：切换为"未找到匹配"文案 + 一键清除筛选（修复误导性的"暂无预设"）
 */
@Composable
private fun EmptyState(
    tabIndex: Int,
    onBrowseAll: (() -> Unit)? = null,
    onCreatePreset: (() -> Unit)? = null,
    onClearFilters: (() -> Unit)? = null
) {
    // 搜索/筛选无结果态（优先级高于"暂无预设"空态）
    val isNoMatch = tabIndex == 0 && onClearFilters != null

    val message = when {
        isNoMatch -> stringResource(R.string.home_no_match_title)
        tabIndex == 0 -> stringResource(R.string.empty_no_presets)
        tabIndex == 1 -> stringResource(R.string.empty_no_favorites)
        tabIndex == 2 -> stringResource(R.string.empty_no_custom)
        else -> stringResource(R.string.empty_no_data)
    }

    val subMessage = when {
        isNoMatch -> stringResource(R.string.home_no_match_desc)
        tabIndex == 0 -> stringResource(R.string.empty_hint_add_presets)
        tabIndex == 1 -> stringResource(R.string.empty_hint_favorite)
        tabIndex == 2 -> stringResource(R.string.empty_hint_create)
        else -> ""
    }

    val icon = when {
        isNoMatch -> Icons.Default.Search
        tabIndex == 1 -> Icons.Outlined.FavoriteBorder
        tabIndex == 2 -> Icons.Default.Add
        else -> Icons.Default.CloudOff
    }

    val onAction = when {
        isNoMatch -> onClearFilters
        tabIndex == 1 -> onBrowseAll
        tabIndex == 2 -> onCreatePreset
        else -> null
    }
    val actionText = when {
        isNoMatch -> stringResource(R.string.home_no_match_action)
        tabIndex == 1 -> onBrowseAll?.let { stringResource(R.string.home_empty_action_browse) }
        tabIndex == 2 -> onCreatePreset?.let { stringResource(R.string.home_empty_action_create) }
        else -> null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.size(48.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
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
            if (actionText != null && onAction != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = AppDesign.ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = actionText, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (tabIndex == 0 && !isNoMatch) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_empty_hint_pull_refresh),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f)
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
                            // 【v1.6.13】ColorOS 17 液态按压反馈
                            val chipInteraction = remember { MutableInteractionSource() }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    haptic.perform(HapticFeedbackType.ToggleOn)
                                    onToggleFilter(label)
                                },
                                modifier = Modifier.fluidPressScale(chipInteraction, pressedScale = 0.92f),
                                interactionSource = chipInteraction,
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

            // ③ 排序下拉 + 快捷 Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                // 【v1.6.13】ColorOS 17 液态按压反馈
                val favInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.perform(HapticFeedbackType.ToggleOn)
                        onToggleFavoritesOnly()
                    },
                    interactionSource = favInteraction,
                    shape = AppDesign.PillShape,
                    color = favChipColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fluidPressScale(favInteraction, pressedScale = 0.92f)
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
                val newInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.perform(HapticFeedbackType.ToggleOn)
                        onToggleNewOnly()
                    },
                    interactionSource = newInteraction,
                    shape = AppDesign.PillShape,
                    color = newChipColor,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fluidPressScale(newInteraction, pressedScale = 0.92f)
                ) {
                    Text(
                        text = stringResource(R.string.filter_new_only),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = newTextColor
                    )
                }
            }

            // ④ 【v1.6.13】计数 + 一键清除独立成行（修复小屏与排序/Toggle 挤压溢出）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasActiveFilter = searchQuery.isNotBlank() || selectedFilters.isNotEmpty() ||
                        showFavoritesOnly || showNewOnly || sortOrder != HomeViewModel.SortOrder.DEFAULT
                Text(
                    text = stringResource(R.string.filter_result_count, presetCount),
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
