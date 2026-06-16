package com.silas.omaster.ui.featured

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.data.local.FavoriteManager
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.LightGray
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.OnSurfacePrimary
import com.silas.omaster.ui.theme.OnSurfaceSecondary
import com.silas.omaster.ui.theme.OnSurfaceTertiary
import com.silas.omaster.ui.theme.OnSurfaceDisabled
import com.silas.omaster.ui.theme.DividerColor
import com.silas.omaster.ui.theme.OnSurfaceInverse
import com.silas.omaster.ui.theme.OutlineVariant
import com.silas.omaster.util.ImageCacheManager
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 精选推荐页面
 * 展示精选预设样张，支持点击查看参数和应用功能
 */
@Composable
fun FeaturedPresetsScreen(
    onNavigateToDetail: (MasterPreset) -> Unit,
    onApplyPreset: (MasterPreset) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val favoriteManager = remember { FavoriteManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    // 精选预设数据
    var featuredPresets by remember { mutableStateOf<List<MasterPreset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 筛选状态
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedScene by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // 收藏状态
    val favoriteIds by favoriteManager.favoritesFlow.collectAsState()

    // 加载精选预设
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 从本地加载精选预设（示例数据）
            featuredPresets = loadFeaturedPresets()
            isLoading = false
        }
    }

    // 筛选后的预设
    val filteredPresets = remember(featuredPresets, selectedBrand, selectedScene, searchQuery) {
        featuredPresets.filter { preset ->
            val brandMatch = selectedBrand == null || preset.brand == selectedBrand
            val sceneMatch = selectedScene == null || preset.tags?.contains(selectedScene) == true
            val searchMatch = searchQuery.isEmpty() ||
                    preset.name.contains(searchQuery, ignoreCase = true) ||
                    preset.author.contains(searchQuery, ignoreCase = true)
            brandMatch && sceneMatch && searchMatch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏（对齐Web端）
        FeaturedHeader(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onFilterClick = { haptic.perform(HapticFeedbackType.LongPress) }
        )

        // 搜索栏（对齐Web端：可见搜索栏）
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 品牌筛选
        BrandFilterRow(
            selectedBrand = selectedBrand,
            onBrandSelected = { brand ->
                haptic.perform(HapticFeedbackType.LongPress)
                selectedBrand = brand
            }
        )

        // 场景筛选
        SceneFilterRow(
            selectedScene = selectedScene,
            onSceneSelected = { scene ->
                haptic.perform(HapticFeedbackType.LongPress)
                selectedScene = scene
            }
        )

        // 预设数量提示
        if (selectedBrand != null || selectedScene != null || searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_result_count, filteredPresets.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    selectedBrand = null
                    selectedScene = null
                    searchQuery = ""
                }) {
                    Text(
                        text = stringResource(R.string.clear_filter),
                        style = MaterialTheme.typography.labelMedium,
                        color = OutlineVariant
                    )
                }
            }
        }

        // 预设网格 - 适配移动端分辨率，小屏单列大屏双列
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = HasselbladOrange
                )
            }
        } else if (filteredPresets.isEmpty()) {
            EmptyState(
                onRetry = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    selectedBrand = null
                    selectedScene = null
                    searchQuery = ""
                }
            )
        } else {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp
            // 小屏手机(<360dp)用单列，普通手机双列，平板大屏3列
            val columnCount = when {
                screenWidthDp >= 600 -> 3
                screenWidthDp >= 360 -> 2
                else -> 1
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPresets) { preset ->
                    FeaturedPresetCard(
                        preset = preset,
                        isFavorite = favoriteIds.contains(preset.id),
                        onClick = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            onNavigateToDetail(preset)
                        },
                        onFavoriteClick = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            preset.id?.let { favoriteManager.toggleFavorite(it) }
                        },
                        onApplyClick = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            onApplyPreset(preset)
                        }
                    )
                }
                // 底部提示（对齐Web端）
                item {
                    LoadingMoreTip()
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "精选推荐",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfacePrimary
            )
            Text(
                text = "大师级影像参数库",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceTertiary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 搜索按钮
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.Search, "搜索", tint = OnSurfacePrimary)
        }

        // 筛选按钮
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, "筛选", tint = OnSurfacePrimary)
        }
    }
}

@Composable
private fun BrandFilterRow(
    selectedBrand: String?,
    onBrandSelected: (String?) -> Unit
) {
    val brands = listOf("OPPO", "realme", "vivo", "荣耀", "小米")

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedBrand == null,
                onClick = { onBrandSelected(null) },
                label = { Text("全部品牌") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = OnSurfacePrimary
                )
            )
        }

        items(brands.size) { index ->
            FilterChip(
                selected = selectedBrand == brands[index],
                onClick = { onBrandSelected(brands[index]) },
                label = { Text(brands[index]) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = OnSurfacePrimary
                )
            )
        }
    }
}

@Composable
private fun SceneFilterRow(
    selectedScene: String?,
    onSceneSelected: (String?) -> Unit
) {
    val scenes = listOf("人像", "风景", "夜景", "美食", "街拍", "建筑")

    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedScene == null,
                onClick = { onSceneSelected(null) },
                label = { Text("全部场景") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange.copy(alpha = 0.7f),
                    selectedLabelColor = OnSurfacePrimary
                )
            )
        }

        items(scenes.size) { index ->
            FilterChip(
                selected = selectedScene == scenes[index],
                onClick = { onSceneSelected(scenes[index]) },
                label = { Text(scenes[index]) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange.copy(alpha = 0.7f),
                    selectedLabelColor = OnSurfacePrimary
                )
            )
        }
    }
}

@Composable
private fun FeaturedPresetCard(
    preset: MasterPreset,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
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
        colors = CardDefaults.cardColors(containerColor = DarkGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 预设图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showDownloadIndicator = true
                )

                // NEW标签（对齐Web端：绿色）
                if (preset.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SuccessGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfacePrimary
                        )
                    }
                }

                // 收藏按钮
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OnSurfaceInverse)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorite) HasselbladOrange else OnSurfacePrimary
                    )
                }
            }

            // 预设信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfacePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = LightGray
                    )

                    if (preset.isHncs == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "HNCS",
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange
                        )
                    }
                }

                // 应用哈苏配方按钮
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrange.copy(alpha = 0.2f),
                        contentColor = HasselbladOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = HasselbladOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "应用哈苏配方",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = OutlineVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_no_featured),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfacePrimary
        )
        Text(
            text = stringResource(R.string.empty_hint_filter),
            style = MaterialTheme.typography.bodySmall,
            color = OutlineVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.retry), color = HasselbladOrange)
        }
    }
}

/**
 * 加载精选预设数据
 */
private fun loadFeaturedPresets(): List<MasterPreset> {
    // 示例精选预设数据
    return listOf(
        MasterPreset(
            id = "featured_1",
            name = "清新人像",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/portrait_fresh.jpg",
            author = "@OPPO影像",
            brand = "OPPO",
            tags = listOf("人像", "清新"),
            isNew = true,
            isHncs = true,
            saturation = 10,
            tone = 5,
            warmCool = 8,
            sharpness = 15
        ),
        MasterPreset(
            id = "featured_2",
            name = "夜景霓虹",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/night_neon.jpg",
            author = "@Find摄影",
            brand = "OPPO",
            tags = listOf("夜景", "霓虹"),
            isNew = false,
            isHncs = true,
            saturation = 35,
            tone = 20,
            warmCool = -10,
            sharpness = 25
        ),
        MasterPreset(
            id = "featured_3",
            name = "美食暖调",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/food_warm.jpg",
            author = "@美食摄影师",
            brand = "realme",
            tags = listOf("美食", "暖调"),
            isNew = true,
            saturation = 15,
            tone = 10,
            warmCool = 20,
            sharpness = 12
        ),
        MasterPreset(
            id = "featured_4",
            name = "街拍黑白",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/street_bw.jpg",
            author = "@街拍大师",
            brand = "vivo",
            tags = listOf("街拍", "黑白"),
            isNew = false,
            saturation = -100,
            tone = 25,
            sharpness = 20
        ),
        MasterPreset(
            id = "featured_5",
            name = "风景通透",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/landscape_clear.jpg",
            author = "@风光摄影",
            brand = "荣耀",
            tags = listOf("风景", "通透"),
            isNew = true,
            isHncs = true,
            saturation = 20,
            tone = 10,
            warmCool = -10,
            sharpness = 25
        ),
        MasterPreset(
            id = "featured_6",
            name = "建筑几何",
            coverPath = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/samples/architecture.jpg",
            author = "@建筑摄影",
            brand = "小米",
            tags = listOf("建筑", "几何"),
            isNew = false,
            saturation = 8,
            tone = 15,
            sharpness = 30
        )
    )
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
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = OnSurfaceDisabled,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = DividerColor,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = OnSurfaceTertiary,
                modifier = Modifier.size(16.dp)
            )
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    color = OnSurfacePrimary
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(HasselbladOrange),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索精选预设...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceTertiary
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * 底部提示（对齐Web端）
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
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                HasselbladOrange.copy(alpha = 0.5f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
            )
            Text(
                text = "持续更新 敬请期待",
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
                                androidx.compose.ui.graphics.Color.Transparent,
                                HasselbladOrange.copy(alpha = 0.5f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}