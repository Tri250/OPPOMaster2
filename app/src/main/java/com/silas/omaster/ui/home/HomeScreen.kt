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
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.silas.omaster.ui.components.ShimmerPresetGrid
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.hapticClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform
import com.silas.omaster.billing.AppReviewManager
import kotlinx.coroutines.delay
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (MasterPreset) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToAIFineTune: () -> Unit = {},
    onNavigateToSmartOptimize: () -> Unit = {},
    onNavigateToPresetManager: () -> Unit = {},
    onNavigateToParamAdjustment: () -> Unit = {},
    onNavigateToHasselbladEye: () -> Unit = {},
    onNavigateToXingYingJi: () -> Unit = {},
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
    val isLoading by viewModel.isLoading.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedPresetIds by viewModel.selectedPresetIds.collectAsState()

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

    // 全局悬浮窗控制器：注册/注销生命周期管理
    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }
    DisposableEffect(floatingWindowController) {
        floatingWindowController.register()
        onDispose {
            floatingWindowController.unregister()
        }
    }

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header（对齐Web端）
            HeaderSection(
                onRefresh = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.refresh()
                },
                isMultiSelectMode = isMultiSelectMode,
                onToggleMultiSelect = { viewModel.toggleMultiSelectMode() },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() }
            )

            // 权限自检横幅：在关键权限缺失时提示用户
            PermissionCheckBanner()

            // 错误状态提示卡片
            if (errorState != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.error_icon_desc),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorState ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                haptic.perform(HapticFeedbackType.LongPress)
                                viewModel.retry()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.retry),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 首次加载指示器
            if (isLoading && allPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 快捷功能入口（横向滚动卡片）
            QuickFeaturesSection(
                onNavigateToAIFineTune = onNavigateToAIFineTune,
                onNavigateToSmartOptimize = onNavigateToSmartOptimize,
                onNavigateToParamAdjustment = onNavigateToParamAdjustment,
                onNavigateToHasselbladEye = onNavigateToHasselbladEye,
                onNavigateToXingYingJi = onNavigateToXingYingJi,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 搜索栏（对齐Web端）
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                searchHistory = searchHistory,
                onHistoryClick = { viewModel.setSearchQuery(it) },
                onClearHistory = { viewModel.clearSearchHistory() },
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
                isLoading = isLoading && allPresets.isEmpty(),
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToCreate = onNavigateToCreate,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDeletePreset = {
                    presetToDelete = it
                    showDeleteConfirm = true
                },
                onScrollStateChanged = onScrollStateChanged,
                onRefresh = { onComplete -> viewModel.refresh(onComplete) },
                isMultiSelectMode = isMultiSelectMode,
                selectedPresetIds = selectedPresetIds,
                onToggleSelection = { viewModel.toggleSelection(it) }
            )
        }

        // 悬浮添加按钮（保留Android原生功能：在收藏Tab和我的Tab显示）
        if (selectedTab == 1 || selectedTab == 4) {
            FloatingActionButton(
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToCreate()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onBackground,
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

        // P7: 多选批量操作栏
        if (isMultiSelectMode && selectedPresetIds.isNotEmpty()) {
            MultiSelectBottomBar(
                selectedCount = selectedPresetIds.size,
                onDelete = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.batchDelete()
                },
                onExport = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    viewModel.batchExport()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
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
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {},
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
        if (isMultiSelectMode) {
            // 多选模式：显示全选/取消全选
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDeselectAll) {
                    Text("取消", color = MaterialTheme.colorScheme.onBackground)
                }
                TextButton(onClick = onSelectAll) {
                    Text("全选", color = HasselbladOrange)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.hasselblad_master),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 多选按钮
            TextButton(
                onClick = onToggleMultiSelect,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = if (isMultiSelectMode) "完成" else "选择",
                    color = if (isMultiSelectMode) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            // 刷新按钮
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
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
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { }),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        },
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // 搜索历史建议：搜索词为空且搜索栏获得焦点时显示
        if (query.isEmpty() && isFocused && searchHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "搜索历史",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = onClearHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "清除",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 最多显示5条历史
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    searchHistory.take(5).forEach { historyQuery ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                                )
                                .hapticClickable { onHistoryClick(historyQuery) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = historyQuery,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
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
        stringResource(R.string.tab_new),       // 上新
        "我的"                                    // 我的（自定义预设）
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.background,
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
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
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
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
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
        "荣耀" to stringResource(R.string.brand_honor)
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.sort),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                sortOptions.forEach { (type, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sortType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// P7: 多选批量操作栏
@Composable
private fun MultiSelectBottomBar(
    selectedCount: Int,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已选择 $selectedCount 项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", color = Color.White)
                }
                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导出", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PresetGrid(
    presets: List<MasterPreset>,
    selectedTab: Int,
    isLoading: Boolean = false,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onNavigateToCreate: () -> Unit = {},
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {},
    onRefresh: (onComplete: () -> Unit) -> Unit = {},
    isMultiSelectMode: Boolean = false,
    selectedPresetIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {}
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
        when {
            isLoading -> {
                // 骨架屏加载状态
                ShimmerPresetGrid(itemCount = 6)
            }
            presets.isEmpty() -> {
                // 空状态
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        EnhancedEmptyState(
                            tabIndex = selectedTab,
                            onNavigateToCreate = onNavigateToCreate,
                            onRefresh = { onRefresh {} }
                        )
                    }
                }
            }
            else -> {
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
                    key = { index, preset -> preset.id ?: "${preset.name}_$index" }
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
                        isMultiSelectMode = isMultiSelectMode,
                        selectedPresetIds = selectedPresetIds,
                        onToggleSelection = onToggleSelection,
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
    isMultiSelectMode: Boolean = false,
    selectedPresetIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 使用安全的 key，确保每个预设有独立的动画状态
    val animationKey = preset.id ?: preset.name
    val animatedProgress = remember(animationKey) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

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
        Box {
            PresetCard(
                preset = preset,
                onClick = {
                    if (isMultiSelectMode) {
                        onToggleSelection(preset.id ?: return@PresetCard)
                    } else {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onNavigateToDetail(preset)
                        // 触发应用内评分检查
                        try {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                AppReviewManager.getInstance(activity).tryShowReview()
                            }
                        } catch (_: Exception) {}
                    }
                },
                onFavoriteClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    preset.id?.let { onToggleFavorite(it) }
                },
                imageHeight = imageHeight
            )

            // 多选模式复选框
            if (isMultiSelectMode) {
                val isSelected = selectedPresetIds.contains(preset.id)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(
                            if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(2.dp, if (isSelected) HasselbladOrange else Color.Gray.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedEmptyState(
    tabIndex: Int,
    onNavigateToCreate: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val message = when (tabIndex) {
        0 -> stringResource(R.string.empty_no_presets)
        1 -> stringResource(R.string.empty_no_favorites)
        2 -> stringResource(R.string.empty_no_hncs_presets)
        3 -> stringResource(R.string.empty_no_new_presets)
        4 -> stringResource(R.string.empty_no_custom_presets)
        else -> stringResource(R.string.empty_no_data)
    }

    val subMessage = when (tabIndex) {
        0 -> stringResource(R.string.empty_hint_add_presets)
        1 -> stringResource(R.string.empty_hint_favorite)
        2 -> stringResource(R.string.empty_hint_hncs_presets)
        3 -> stringResource(R.string.empty_hint_new_presets)
        4 -> stringResource(R.string.empty_hint_custom_presets)
        else -> ""
    }

    val icon = when (tabIndex) {
        0 -> Icons.Default.Search
        1 -> Icons.Outlined.FavoriteBorder
        2 -> Icons.Default.ColorLens
        3 -> Icons.Default.Refresh
        4 -> Icons.Default.Add
        else -> Icons.Default.Search
    }

    val contentDescription = when (tabIndex) {
        0 -> stringResource(R.string.empty_icon_search_desc)
        1 -> stringResource(R.string.empty_icon_favorite_desc)
        2 -> stringResource(R.string.empty_icon_color_desc)
        3 -> stringResource(R.string.empty_icon_refresh_desc)
        4 -> stringResource(R.string.empty_icon_add_desc)
        else -> stringResource(R.string.empty_icon_default_desc)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            if (subMessage.isNotEmpty()) {
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            // 操作按钮引导
            when (tabIndex) {
                0 -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onRefresh?.invoke() }) {
                        Text(stringResource(R.string.refresh_data))
                    }
                }
                1, 4 -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onNavigateToCreate?.invoke() }) {
                        Text(stringResource(R.string.go_add_preset))
                    }
                }
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

/**
 * 快捷功能入口区域（横向滚动卡片）
 * 将首页未使用的导航回调接入实际入口
 */
@Composable
private fun QuickFeaturesSection(
    onNavigateToAIFineTune: () -> Unit,
    onNavigateToSmartOptimize: () -> Unit,
    onNavigateToParamAdjustment: () -> Unit,
    onNavigateToHasselbladEye: () -> Unit,
    onNavigateToXingYingJi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickFeatureCard(
                name = "AI微调",
                icon = Icons.Default.AutoAwesome,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToAIFineTune()
                }
            )
        }
        item {
            QuickFeatureCard(
                name = "智能优化",
                icon = Icons.Default.Tune,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToSmartOptimize()
                }
            )
        }
        item {
            QuickFeatureCard(
                name = "参数调节",
                icon = Icons.Default.Adjust,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToParamAdjustment()
                }
            )
        }
        item {
            QuickFeatureCard(
                name = "哈苏之眼",
                icon = Icons.Default.ColorLens,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToHasselbladEye()
                }
            )
        }
        item {
            QuickFeatureCard(
                name = "行影集",
                icon = Icons.Default.Collections,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToXingYingJi()
                }
            )
        }
    }
}

@Composable
private fun QuickFeatureCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .hapticClickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 权限自检横幅：在 HomeScreen 入口检查核心权限状态，缺失时提示用户引导授权。
 * 覆盖 2026 正式版自检要求中的权限获取闭环。
 */
@Composable
private fun PermissionCheckBanner() {
    val context = LocalContext.current

    // 核心权限列表（根据系统版本适配）
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var missingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // 每次重组时重新检查权限（用户从设置页返回后会触发）
    LaunchedEffect(Unit) {
        missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    if (missingPermissions.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "部分核心权限未授权，可能影响功能使用",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            val permissionNames = missingPermissions.joinToString(separator = "、") { permission ->
                when (permission) {
                    Manifest.permission.CAMERA -> "相机"
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES -> "存储/相册"
                    Manifest.permission.POST_NOTIFICATIONS -> "通知"
                    else -> permission.substringAfterLast(".")
                }
            }
            Text(
                text = "缺失权限：$permissionNames",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        ).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("去设置授权", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}