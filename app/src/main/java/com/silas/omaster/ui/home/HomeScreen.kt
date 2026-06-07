package com.silas.omaster.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.hapticClickable
import com.silas.omaster.util.perform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan

/**
 * HomeScreen - 适配Web端设计
 * 包含：Hero区域、功能展示、预设列表、统计数据
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (MasterPreset) -> Unit,
    onNavigateToCreate: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    onNavigateToAi: () -> Unit = {},
    onNavigateToWatermark: () -> Unit = {},
    onNavigateToHasselbladColor: () -> Unit = {},
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

    val allPresets by viewModel.allPresets.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            viewModel.refresh()
        }
    }

    val settingsManager = remember { SettingsManager.getInstance(context) }
    val defaultStartTab = remember { settingsManager.defaultStartTab }
    val pagerState = rememberPagerState(initialPage = defaultStartTab, pageCount = { 3 })

    LaunchedEffect(Unit) {
        if (selectedTab != defaultStartTab) {
            viewModel.selectTab(defaultStartTab)
        }
    }

    val floatingWindowController = remember { FloatingWindowController.getInstance(context) }

    LaunchedEffect(allPresets, favorites, customPresets, selectedTab) {
        val currentList = when (selectedTab) {
            0 -> allPresets
            1 -> favorites
            2 -> customPresets
            else -> allPresets
        }
        floatingWindowController.setPresetList(currentList)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) {
            viewModel.selectTab(pagerState.currentPage)
        }
    }

    // 下拉刷新状态
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .pullRefresh(pullRefreshState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Hero区域 - 带渐变背景
            HeroSection(
                onNavigateToAi = onNavigateToAi,
                onNavigateToWatermark = onNavigateToWatermark,
                onNavigateToHasselbladColor = onNavigateToHasselbladColor
            )

            // 功能展示区域
            FeaturesSection(
                onNavigateToAi = onNavigateToAi,
                onNavigateToWatermark = onNavigateToWatermark,
                onNavigateToHasselbladColor = onNavigateToHasselbladColor
            )

            // Tab 切换栏
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureBlack,
                contentColor = HasselbladOrange,
                edgePadding = 16.dp,
                modifier = Modifier.height(48.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp),
                        color = HasselbladOrange
                    )
                },
                divider = {}
            ) {
                val tabs = listOf("全部预设", "我的收藏", "自定义")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            haptic.perform(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(index)
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 预设列表
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val presets = when (page) {
                    0 -> allPresets
                    1 -> favorites
                    2 -> customPresets
                    else -> allPresets
                }

                PresetGrid(
                    presets = presets,
                    onNavigateToDetail = onNavigateToDetail,
                    onToggleFavorite = { id ->
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleFavorite(id)
                    },
                    onDeletePreset = { id ->
                        presetToDelete = id
                        showDeleteConfirm = true
                    },
                    tabIndex = page,
                    onScrollStateChanged = onScrollStateChanged
                )
            }
        }

        // 下拉刷新指示器
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = HasselbladOrange,
            backgroundColor = Color(0xFF1A1A1A)
        )

        // 删除确认对话框
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除这个预设吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            presetToDelete?.let { id ->
                                viewModel.deleteCustomPreset(id)
                            }
                            showDeleteConfirm = false
                        }
                    ) {
                        Text("删除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * Hero区域 - 适配Web端设计
 */
@Composable
private fun HeroSection(
    onNavigateToAi: () -> Unit,
    onNavigateToWatermark: () -> Unit,
    onNavigateToHasselbladColor: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D)
                    )
                )
            )
            .padding(vertical = 24.dp)
    ) {
        // 装饰光晕
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HasselbladOrange.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 哈苏认证徽章
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HasselbladOrange.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    HasselbladOrange.copy(alpha = 0.3f)
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "哈苏 HNCS 官方认证",
                        style = MaterialTheme.typography.labelMedium,
                        color = HasselbladOrange
                    )
                }
            }

            // 主标题
            Text(
                text = "OMaster",
                style = MaterialTheme.typography.displaySmall.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            HasselbladOrange,
                            HasselbladOrange.copy(alpha = 0.8f)
                        )
                    )
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "专业摄影参数预设",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 副标题
            Text(
                text = "为 OPPO Find 系列打造的专业级摄影工具",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 功能标签
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureTag("哈苏色彩科学")
                FeatureTag("智能场景识别")
                FeatureTag("一键优化")
            }

            // 统计数据
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Filled.Camera,
                    value = "500+",
                    label = "专业预设"
                )
                StatItem(
                    icon = Icons.Filled.Sparkles,
                    value = "35+",
                    label = "场景识别"
                )
                StatItem(
                    icon = Icons.Filled.Download,
                    value = "100万+",
                    label = "用户下载"
                )
            }

            // 快速入口按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessButton(
                    icon = Icons.Filled.AutoAwesome,
                    label = "AI识别",
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        onNavigateToAi()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickAccessButton(
                    icon = Icons.Filled.Brush,
                    label = "水印",
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        onNavigateToWatermark()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickAccessButton(
                    icon = Icons.Filled.Palette,
                    label = "色彩",
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        onNavigateToHasselbladColor()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 功能标签
 */
@Composable
private fun FeatureTag(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HasselbladOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * 快速入口按钮
 */
@Composable
private fun QuickAccessButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = HasselbladOrange.copy(alpha = 0.15f),
        modifier = modifier
            .height(48.dp)
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

/**
 * 功能展示区域 - 适配Web端6个核心功能
 */
@Composable
private fun FeaturesSection(
    onNavigateToAi: () -> Unit,
    onNavigateToWatermark: () -> Unit,
    onNavigateToHasselbladColor: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val features = listOf(
        FeatureItem(
            icon = Icons.Filled.AutoAwesome,
            title = "AI场景识别",
            description = "智能识别35+拍摄场景，自动推荐最佳参数配置",
            features = listOf("实时识别", "参数推荐", "场景优化"),
            onClick = onNavigateToAi
        ),
        FeatureItem(
            icon = Icons.Filled.Palette,
            title = "哈苏色彩科学",
            description = "官方HNCS认证，还原专业级哈苏色彩表现",
            features = listOf("HNCS 3.0", "自然色彩", "大师风格"),
            onClick = onNavigateToHasselbladColor
        ),
        FeatureItem(
            icon = Icons.Filled.Favorite,
            title = "预设管理",
            description = "500+专业预设，支持导入导出与云端同步",
            features = listOf("分类筛选", "收藏管理", "一键应用"),
            onClick = {}
        ),
        FeatureItem(
            icon = Icons.Filled.Camera,
            title = "参数精细调节",
            description = "专业级参数控制，实时预览调节效果",
            features = listOf("ISO/快门", "白平衡", "曝光补偿"),
            onClick = {}
        ),
        FeatureItem(
            icon = Icons.Filled.WaterDrop,
            title = "水印编辑器",
            description = "12+水印模板，支持品牌、功能、开源多种风格",
            features = listOf("品牌水印", "版权保护", "自定义样式"),
            onClick = onNavigateToWatermark
        ),
        FeatureItem(
            icon = Icons.Filled.Sparkles,
            title = "智能优化",
            description = "AI一键优化，根据图片特征智能调整参数",
            features = listOf("自动优化", "风格迁移", "智能蒙版"),
            onClick = onNavigateToAi
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = "核心功能",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Text(
            text = "专业级摄影工具，为创作者打造极致拍摄体验",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 功能网格
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.chunked(2).forEach { rowFeatures ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowFeatures.forEach { feature ->
                        FeatureCard(
                            feature = feature,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowFeatures.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 功能项数据类
 */
private data class FeatureItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val features: List<String>,
    val onClick: () -> Unit
)

/**
 * 功能卡片
 */
@Composable
private fun FeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "card_scale"
    )

    Surface(
        onClick = {
            haptic.perform(HapticFeedbackType.TextHandleMove)
            feature.onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A1A),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HasselbladOrange,
                                HasselbladOrange.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 标题
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // 描述
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 功能标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                feature.features.take(2).forEach { f ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.06f)
                    ) {
                        Text(
                            text = f,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 预设网格
 */
@Composable
private fun PresetGrid(
    presets: List<MasterPreset>,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    tabIndex: Int,
    onScrollStateChanged: (Boolean) -> Unit
) {
    val listState = rememberLazyStaggeredGridState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                val totalItems = presets.size
                onScrollStateChanged(lastVisibleItem < totalItems - 3)
            }
    }

    if (presets.isEmpty()) {
        EmptyState(tabIndex)
    } else {
        LazyVerticalStaggeredGrid(
            state = listState,
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = presets,
                key = { _, preset -> preset.id ?: preset.hashCode() }
            ) { index, preset ->
                AnimatedPresetCard(
                    preset = preset,
                    index = index,
                    onNavigateToDetail = onNavigateToDetail,
                    onToggleFavorite = onToggleFavorite,
                    onDeletePreset = onDeletePreset,
                    tabIndex = tabIndex
                )
            }

            // 底部提示
            item(span = StaggeredGridItemSpan.FullLine) {
                LoadingMoreTip()
            }
        }
    }
}

/**
 * 带动画的预设卡片
 */
@Composable
private fun AnimatedPresetCard(
    preset: MasterPreset,
    index: Int,
    onNavigateToDetail: (MasterPreset) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    tabIndex: Int
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val delayMillis = calculateStaggerDelay(index, 0)
        delay(delayMillis.toLong())
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = ListItemFadeInSpec
        )
    }

    val alpha = animatedProgress.value
    val scale = 0.9f + (0.1f * animatedProgress.value)
    val translationY = (1f - animatedProgress.value) * 30f

    Box(
        modifier = Modifier
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
            onFavoriteClick = { preset.id?.let { onToggleFavorite(it) } },
            onDeleteClick = { preset.id?.let { onDeletePreset(it) } },
            showFavoriteButton = true,
            showDeleteButton = tabIndex == 2,
            imageHeight = 160.dp
        )
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(tabIndex: Int) {
    val message = when (tabIndex) {
        0 -> "暂无预设"
        1 -> "暂无收藏"
        2 -> "暂无自定义预设"
        else -> "暂无数据"
    }

    val subMessage = when (tabIndex) {
        0 -> "下拉刷新或添加订阅源"
        1 -> "点击爱心收藏喜欢的预设"
        2 -> "点击右下角按钮创建预设"
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Camera,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            if (subMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HasselbladOrange.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 底部加载提示
 */
@Composable
private fun LoadingMoreTip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "持续更新 · 敬请期待",
                style = MaterialTheme.typography.bodyMedium,
                color = HasselbladOrange.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

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
