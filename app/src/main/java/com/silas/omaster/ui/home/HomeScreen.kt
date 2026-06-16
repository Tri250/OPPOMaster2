package com.silas.omaster.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.R
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.animation.ListItemFadeInSpec
import com.silas.omaster.ui.animation.ListItemPlacementSpec
import com.silas.omaster.ui.animation.calculateStaggerDelay
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.hapticClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform
import kotlinx.coroutines.delay
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

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

    val allPresets by viewModel.allPresets.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // 当 refreshTrigger 变化时刷新数据
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.refresh()
        }
    }

    // 读取默认启动 Tab 设置
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val defaultStartTab = remember { settingsManager.defaultStartTab }

    // 初始化时同步默认 Tab
    LaunchedEffect(Unit) {
        if (selectedTab != defaultStartTab) {
            viewModel.selectTab(defaultStartTab)
        }
    }

    // 全局悬浮窗控制器
    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }

    // 获取过滤后的预设列表
    val filteredPresets = remember(selectedTab, selectedBrand, sortType, searchQuery, allPresets) {
        viewModel.getFilteredPresets()
    }

    // 当预设列表变化时，更新到全局控制器
    LaunchedEffect(filteredPresets, selectedTab) {
        floatingWindowController.setPresetList(filteredPresets)
    }

    // 删除确认对话框状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header（对齐Web端）
            HeaderSection(
                onRefresh = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.refresh()
                }
            )

            // 搜索栏（对齐Web端）
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab 切换栏（对齐Web端：发现、收藏、哈苏、上新）
            TabBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.selectTab(index)
                },
                getTabCount = { tabIndex -> viewModel.getTabCount(tabIndex) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 品牌筛选 + 排序筛选（对齐Web端）
            BrandAndSortFilter(
                selectedBrand = selectedBrand,
                onBrandSelected = { brand ->
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.selectBrand(brand)
                },
                sortType = sortType,
                onSortSelected = { sort ->
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.setSortType(sort)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 预设网格
            PresetGrid(
                presets = filteredPresets,
                selectedTab = selectedTab,
                onNavigateToDetail = onNavigateToDetail,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDeletePreset = {
                    presetToDelete = it
                    showDeleteConfirm = true
                },
                onScrollStateChanged = onScrollStateChanged,
                onRefresh = { onComplete -> viewModel.refresh(onComplete) }
            )
        }

        // 悬浮添加按钮（保留Android原生功能：只在收藏Tab显示）
        if (selectedTab == 1) {
            FloatingActionButton(
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 100.dp)
                    .size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_preset),
                    modifier = Modifier.size(32.dp)
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
                        haptic.perform(HapticFeedbackType.LongPress)
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
                        haptic.perform(HapticFeedbackType.LongPress)
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
 * Header区域（对齐Web端）
 */
@Composable
private fun HeaderSection(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            // 哈苏大师标签（对齐Web端）
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(HasselbladOrange, WarningYellow)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "👑",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.hasselblad_master),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 刷新按钮
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.refresh),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 搜索栏（对齐Web端）
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_hint),
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * Tab栏（对齐Web端：发现、收藏、哈苏、上新）
 */
@Composable
private fun TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    getTabCount: (Int) -> Int,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.tab_discover),  // 发现
        stringResource(R.string.tab_favorites), // 收藏
        stringResource(R.string.tab_hncs),      // 哈苏
        stringResource(R.string.tab_new)        // 上新
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = PureBlack,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 0.dp,
        modifier = modifier,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTab])
                    .height(3.dp)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            val count = getTabCount(index)

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        // 计数徽章（对齐Web端）
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else
                                            Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    else
                                        Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * 品牌筛选 + 排序筛选（对齐Web端）
 */
@Composable
private fun BrandAndSortFilter(
    selectedBrand: String,
    onBrandSelected: (String) -> Unit,
    sortType: SortType,
    onSortSelected: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val brands = listOf(
        "all" to stringResource(R.string.brand_all),
        "OPPO" to stringResource(R.string.brand_oppo),
        "realme" to stringResource(R.string.brand_realme),
        "vivo" to stringResource(R.string.brand_vivo),
        "荣耀" to stringResource(R.string.brand_honor),
        "小米" to stringResource(R.string.brand_xiaomi)
    )

    val sortOptions = listOf(
        SortType.NEWEST to stringResource(R.string.sort_newest),
        SortType.POPULAR to stringResource(R.string.sort_popular),
        SortType.RATING to stringResource(R.string.sort_rating)
    )

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 品牌筛选按钮（水平滚动，全宽）
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(brands) { (key, label) ->
                BrandFilterButton(
                    label = label,
                    isSelected = selectedBrand == key,
                    onClick = { onBrandSelected(key) }
                )
            }
        }

        // 排序下拉菜单
        Box(modifier = Modifier.align(Alignment.End)) {
            Row(
                modifier = Modifier
                    .hapticClickable { showSortMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sortOptions.find { it.first == sortType }?.second ?: stringResource(R.string.sort_newest),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "排序",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.background(DarkGray)
            ) {
                sortOptions.forEach { (type, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sortType == type) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)
                            )
                        },
                        onClick = {
                            onSortSelected(type)
                            showSortMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 品牌筛选按钮
 */
@Composable
private fun BrandFilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .hapticClickable { onClick() }
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PresetGrid(
    presets: List<MasterPreset>,
    selectedTab: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {},
    onRefresh: (onComplete: () -> Unit) -> Unit = {}
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
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var hasHapticAtTop by remember { mutableStateOf(false) }
    var hasHapticAtBottom by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (currentIndex, currentOffset) ->
            val isUp = currentIndex < previousIndex ||
                       (currentIndex == previousIndex && currentOffset <= previousScrollOffset)
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
            onScrollStateChanged(isUp)

            // 滚动到顶部或底部时触发震感
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            if (currentIndex == 0 && !hasHapticAtTop) {
                haptic.perform(HapticFeedbackType.LongPress)
                hasHapticAtTop = true
                hasHapticAtBottom = false
            } else if (lastVisibleItem >= totalItems - 1 && totalItems > 0 && !hasHapticAtBottom) {
                haptic.perform(HapticFeedbackType.LongPress)
                hasHapticAtBottom = true
                hasHapticAtTop = false
            } else if (currentIndex > 0 && lastVisibleItem < totalItems - 1) {
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
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    EmptyState(selectedTab)
                }
            }
        } else {
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
                    top = 8.dp,
                    bottom = 100.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
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

                    val delayMillis = if (visibleStartIndex == 0) {
                        calculateStaggerDelay(index, visibleStartIndex)
                    } else {
                        0
                    }

                    PresetCardItem(
                        preset = preset,
                        index = index,
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

                // 底部提示（对齐Web端）
                item(span = StaggeredGridItemSpan.FullLine) {
                    LoadingMoreTip()
                }
            }
        }

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
    imageHeight: Int,
    delayMillis: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用安全的 key，确保每个预设有独立的动画状态
    val animationKey = preset.id ?: preset.name
    val animatedProgress = remember(animationKey) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(animationKey) {
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
            onClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                onNavigateToDetail(preset)
            },
            onFavoriteClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                preset.id?.let { onToggleFavorite(it) }
            },
            imageHeight = imageHeight
        )
    }
}

@Composable
private fun EmptyState(tabIndex: Int) {
    val message = when (tabIndex) {
        0 -> stringResource(R.string.empty_no_presets)
        1 -> stringResource(R.string.empty_no_favorites)
        2 -> stringResource(R.string.empty_no_presets) // 哈苏
        3 -> stringResource(R.string.empty_no_presets) // 上新
        else -> stringResource(R.string.empty_no_data)
    }

    val subMessage = when (tabIndex) {
        0 -> stringResource(R.string.empty_hint_add_presets)
        1 -> stringResource(R.string.empty_hint_favorite)
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
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            if (subMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
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
 * 底部加载更多提示（对齐Web端）
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
                        brush = Brush.horizontalGradient(
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
                        brush = Brush.horizontalGradient(
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