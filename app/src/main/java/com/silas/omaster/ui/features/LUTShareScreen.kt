package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.data.repository.LUTResourceRepository
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * LUT 资源分享页面
 *
 * 功能：
 * - 展示专业 LUT 滤镜库
 * - LUT 分类浏览
 * - LUT 下载和应用
 * - LUT 效果预览
 * - 搜索 / 排序 / 收藏 / 详情
 *
 * 对齐 Web 端 LUTSharePage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit,
    onDownload: (LUTResource) -> Unit = {},
    onApplyLUT: ((LUTResource) -> Unit)? = null,
    onNavigateToStyleGenerator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = LUTResourceRepository
    val lutManager = remember { LUTManager.getInstance(context) }

    // 分类选择 - 对齐 Web 端 LUT_CATEGORIES
    var selectedCategory by remember { mutableStateOf("all") }
    val categories = repo.CATEGORIES

    // 搜索
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    // 排序
    var sortBy by remember { mutableStateOf(SortType.DOWNLOADS) }
    // 喜欢/收藏 — 从 LUTManager 持久化状态读取
    val likedIds by lutManager.likedIds.collectAsState()
    // 已下载 — 从 LUTManager 持久化状态读取
    val downloadedIds by lutManager.downloadedIds.collectAsState()
    // 正在下载
    var downloadingId by remember { mutableStateOf<String?>(null) }
    // 详情弹窗
    var selectedLUT by remember { mutableStateOf<LUTResource?>(null) }
    // LUT 预览 Bitmap
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }
    // 加载状态
    var isLoading by remember { mutableStateOf(true) }

    // 模拟初次加载
    LaunchedEffect(Unit) {
        delay(300)
        isLoading = false
    }

    // LUT 数据 - 来自真实数据源
    val luts = remember { repo.getValidResources() }

    // 过滤 + 排序
    val filteredLuts = remember(searchQuery, selectedCategory, sortBy, luts) {
        var result = if (searchQuery.isNotBlank()) {
            repo.searchResources(searchQuery)
        } else {
            repo.getResources(selectedCategory)
        }
        result = when (sortBy) {
            SortType.DOWNLOADS -> result.sortedByDescending { it.downloads }
            SortType.RATING -> result.sortedByDescending { it.rating }
            SortType.NEWEST -> result.sortedByDescending { it.createdAt }
        }
        result
    }

    // 2026新品 / 热门
    val newLuts = remember(luts) { repo.getNewResources() }
    val hotLuts = remember(luts) { repo.getHotResources().filter { !it.isNew }.take(6) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text("LUT 资源库", fontWeight = FontWeight.Bold)
                    Text(
                        "视频调色LUT下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                // 风格 LUT 生成器入口
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToStyleGenerator()
                }) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        "风格LUT生成器",
                        tint = HasselbladOrange
                    )
                }
                Text(
                    text = "${luts.size} 个LUT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // 搜索框
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            isActive = isSearchActive,
            onActiveChange = { isSearchActive = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 分类标签 - 水平滚动，带视觉指示器
        CategoryFilterRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedCategory = category
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 排序
        SortRow(
            sortBy = sortBy,
            onSortChanged = { sortBy = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 主内容区
        if (isLoading) {
            // 加载状态
            LoadingState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 2026新品 & 热门推荐（仅在"全部"分类且无搜索时显示）
                if (selectedCategory == "all" && searchQuery.isBlank()) {
                    if (newLuts.isNotEmpty()) {
                        item(key = "section_new") {
                            SectionHeader(
                                title = "2026新品",
                                accentColor = SuccessGreen,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                        }
                        item(key = "new_list") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(newLuts, key = { "new_${it.id}" }) { lut ->
                                    LUTPosterCard(
                                        lut = lut,
                                        badge = "NEW",
                                        badgeColor = SuccessGreen,
                                        isLiked = likedIds.contains(lut.id),
                                        onLike = { lutManager.toggleLike(lut.id) },
                                        onClick = { selectedLUT = lut }
                                    )
                                }
                            }
                        }
                    }
                    if (hotLuts.isNotEmpty()) {
                        item(key = "section_hot") {
                            SectionHeader(
                                title = "热门推荐",
                                accentColor = WarningYellow,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                            )
                        }
                        item(key = "hot_list") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(hotLuts, key = { "hot_${it.id}" }) { lut ->
                                    LUTPosterCard(
                                        lut = lut,
                                        badge = "HOT",
                                        badgeColor = HasselbladOrange,
                                        isLiked = likedIds.contains(lut.id),
                                        onLike = { lutManager.toggleLike(lut.id) },
                                        onClick = { selectedLUT = lut }
                                    )
                                }
                            }
                        }
                    }
                }

                // 统计信息
                item(key = "stats") {
                    StatsHeader(
                        totalCount = filteredLuts.size,
                        freeCount = filteredLuts.count { it.isFree },
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }

                // 空状态
                if (filteredLuts.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            isSearchActive = searchQuery.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        )
                    }
                } else {
                    // LUT 网格（2列）
                    items(filteredLuts.chunked(2), key = { "row_${it.firstOrNull()?.id ?: "empty"}" }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { lut ->
                                LUTGridCard(
                                    modifier = Modifier.weight(1f),
                                    lut = lut,
                                    isLiked = likedIds.contains(lut.id),
                                    isDownloaded = downloadedIds.contains(lut.id),
                                    isDownloading = downloadingId == lut.id,
                                    onLike = { lutManager.toggleLike(lut.id) },
                                    onClick = { selectedLUT = lut },
                                    onDownload = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        downloadingId = lut.id
                                        scope.launch {
                                            val file = lutManager.downloadLUT(lut)
                                            withContext(Dispatchers.Main) {
                                                downloadingId = null
                                                if (file != null) {
                                                    Toast.makeText(
                                                        context,
                                                        "LUT 已下载: ${lut.name}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onDownload(lut)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "下载失败，请检查网络连接",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            // 补齐单数
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // 详情弹窗
    selectedLUT?.let { lut ->
        LUTDetailDialog(
            lut = lut,
            isLiked = likedIds.contains(lut.id),
            isDownloaded = downloadedIds.contains(lut.id),
            isDownloading = downloadingId == lut.id,
            previewBitmap = previewBitmap,
            isGeneratingPreview = isGeneratingPreview,
            onLike = { lutManager.toggleLike(lut.id) },
            onDownload = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                downloadingId = lut.id
                scope.launch {
                    val file = lutManager.downloadLUT(lut)
                    withContext(Dispatchers.Main) {
                        downloadingId = null
                        if (file != null) {
                            Toast.makeText(
                                context,
                                "LUT 已下载: ${lut.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onDownload(lut)
                        } else {
                            Toast.makeText(
                                context,
                                "下载失败，请检查网络连接",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            onPreview = {
                if (downloadedIds.contains(lut.id)) {
                    isGeneratingPreview = true
                    scope.launch {
                        try {
                            val imageLoader = coil.ImageLoader.Builder(context).build()
                            val request = ImageRequest.Builder(context)
                                .data(lut.previewImage)
                                .crossfade(true)
                                .build()
                            val sourceBitmap = withContext(Dispatchers.IO) {
                                val result = imageLoader.execute(request)
                                (result as? coil.request.SuccessResult)?.drawable?.toBitmap()
                            }
                            if (sourceBitmap != null) {
                                val result = lutManager.applyLUTToBitmap(sourceBitmap, lut.id)
                                withContext(Dispatchers.Main) {
                                    previewBitmap = result
                                    isGeneratingPreview = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isGeneratingPreview = false
                                    Toast.makeText(
                                        context,
                                        "预览图加载失败，请检查网络后重试",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isGeneratingPreview = false
                                Toast.makeText(
                                    context,
                                    "预览生成失败：${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "请先下载 LUT 后预览", Toast.LENGTH_SHORT).show()
                }
            },
            onApply = onApplyLUT?.let { callback -> { callback(lut) } },
            onDismiss = { selectedLUT = null; previewBitmap = null }
        )
    }
}

// ==================== 排序类型 ====================
private enum class SortType { DOWNLOADS, RATING, NEWEST }

// ==================== 搜索栏 ====================
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                "搜索LUT名称、风格...",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "搜索",
                tint = if (query.isNotBlank()) HasselbladOrange
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        "清除",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = HasselbladOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            cursorColor = HasselbladOrange,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

// ==================== 分类筛选行 ====================
@Composable
private fun CategoryFilterRow(
    categories: List<com.silas.omaster.data.repository.LUTCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.key }) { category ->
            val isSelected = category.key == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category.key) },
                label = {
                    Text(
                        text = "${category.icon}  ${category.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) HasselbladOrange
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    selectedBorderColor = HasselbladOrange,
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ==================== 排序行 ====================
@Composable
private fun SortRow(
    sortBy: SortType,
    onSortChanged: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.FilterList,
            null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        SortChip("最多下载", sortBy == SortType.DOWNLOADS) { onSortChanged(SortType.DOWNLOADS) }
        Spacer(modifier = Modifier.width(6.dp))
        SortChip("最高评分", sortBy == SortType.RATING) { onSortChanged(SortType.RATING) }
        Spacer(modifier = Modifier.width(6.dp))
        SortChip("最新发布", sortBy == SortType.NEWEST) { onSortChanged(SortType.NEWEST) }
    }
}

// ==================== 排序标签 ====================
@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HasselbladOrange.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) HasselbladOrange
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) HasselbladOrange
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// ==================== 区块标题 ====================
@Composable
private fun SectionHeader(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ==================== 统计头 ====================
@Composable
private fun StatsHeader(
    totalCount: Int,
    freeCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$totalCount 个 LUT",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(SuccessGreen.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$freeCount 个免费",
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen
            )
        }
    }
}

// ==================== 加载状态 ====================
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = HasselbladOrange,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "加载LUT资源库...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

// ==================== 空状态 ====================
@Composable
private fun EmptyState(
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (isSearchActive) Icons.Outlined.SearchOff else Icons.Outlined.FolderOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isSearchActive) "未找到匹配的LUT" else "暂无LUT资源",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isSearchActive) "请尝试其他关键词或调整筛选条件" else "请检查网络连接后重试",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}

// ==================== 海报卡片（横向展示用） ====================
@Composable
private fun LUTPosterCard(
    lut: LUTResource,
    badge: String,
    badgeColor: Color,
    isLiked: Boolean,
    onLike: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HasselbladOrange.copy(alpha = 0.15f),
                                HasselbladOrange.copy(alpha = 0.05f)
                            )
                        )
                    )
            ) {
                if (lut.previewImage.isNotBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(lut.previewImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = lut.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = rememberVectorPainter(Icons.Default.Image),
                        fallback = rememberVectorPainter(Icons.Default.Image)
                    )
                } else {
                    Text(
                        text = lut.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // 徽标
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                // 收藏按钮
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "收藏",
                        tint = if (isLiked) ErrorRed
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${LUTResourceRepository.formatDownloads(lut.downloads)}下载",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )
            }
        }
    }
}

// ==================== 网格卡片 ====================
@Composable
private fun LUTGridCard(
    modifier: Modifier = Modifier,
    lut: LUTResource,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onLike: () -> Unit,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 预览图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HasselbladOrange.copy(alpha = 0.12f),
                                HasselbladOrange.copy(alpha = 0.04f)
                            )
                        )
                    )
            ) {
                if (lut.previewImage.isNotBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(lut.previewImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = lut.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = rememberVectorPainter(Icons.Default.Image),
                        fallback = rememberVectorPainter(Icons.Default.Image)
                    )
                } else {
                    Text(
                        text = lut.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // 徽标
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (lut.isNew) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen
                        ) {
                            Text(
                                "NEW",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (lut.isHot && !lut.isNew) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HasselbladOrange
                        ) {
                            Text(
                                "HOT",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // 格式标签
                Surface(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = ".${lut.format}",
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                // 收藏
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "收藏",
                        tint = if (isLiked) ErrorRed
                        else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // 信息
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lut.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                // 标签
                if (lut.suitableFor.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lut.suitableFor.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // 评分/下载
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = WarningYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", lut.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Download,
                        null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = LUTResourceRepository.formatDownloads(lut.downloads),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${lut.size}³",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
                // 下载按钮
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) SuccessGreen.copy(alpha = 0.15f)
                        else HasselbladOrange.copy(alpha = 0.15f),
                        contentColor = if (isDownloaded) SuccessGreen else HasselbladOrange
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 7.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isDownloaded) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("已下载", fontSize = 12.sp)
                    } else if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载中...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "下载 (${LUTResourceRepository.formatFileSize(lut.fileSize)})",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== 详情弹窗 ====================
@Composable
private fun LUTDetailDialog(
    lut: LUTResource,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    onLike: () -> Unit,
    onDownload: () -> Unit,
    onPreview: () -> Unit = {},
    onApply: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    HasselbladOrange.copy(alpha = 0.15f),
                                    HasselbladOrange.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    if (lut.previewImage.isNotBlank()) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(lut.previewImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = lut.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = rememberVectorPainter(Icons.Default.Image),
                            fallback = rememberVectorPainter(Icons.Default.Image)
                        )
                    } else {
                        Text(
                            text = lut.name.firstOrNull()?.toString() ?: "",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "关闭",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            lut.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            lut.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HasselbladOrange.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = WarningYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", lut.rating),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    lut.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lut.suitableFor.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                        ) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 信息网格
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        DetailInfoRow("格式", ".${lut.format.uppercase()}")
                        DetailInfoRow("尺寸", "${lut.size}x${lut.size}")
                        DetailInfoRow("文件大小", LUTResourceRepository.formatFileSize(lut.fileSize))
                        DetailInfoRow("下载次数", LUTResourceRepository.formatDownloads(lut.downloads))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 适用场景
                Text(
                    "适用场景",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lut.suitableFor.forEach { scene ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HasselbladOrange.copy(alpha = 0.1f)
                        ) {
                            Text(
                                scene,
                                style = MaterialTheme.typography.bodySmall,
                                color = HasselbladOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // LUT 效果预览（已下载时显示）
                if (isDownloaded && previewBitmap != null) {
                    Text(
                        "LUT 效果预览",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (lut.previewImage.isNotBlank()) {
                                    val ctx = LocalContext.current
                                    AsyncImage(
                                        model = ImageRequest.Builder(ctx)
                                            .data(lut.previewImage)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "原图",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .align(Alignment.BottomStart),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        "原图",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "LUT效果",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .align(Alignment.BottomStart),
                                    shape = RoundedCornerShape(4.dp),
                                    color = HasselbladOrange.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        "LUT",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (isDownloaded && isGeneratingPreview) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "生成预览中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLike,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isLiked) ErrorRed
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLiked) "已收藏" else "收藏", fontSize = 12.sp)
                    }
                    if (isDownloaded && onApply != null) {
                        Button(
                            onClick = onApply,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("应用LUT", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) SuccessGreen else HasselbladOrange
                        )
                    ) {
                        if (isDownloaded) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("已下载", fontSize = 12.sp)
                        } else if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下载中...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下载LUT", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 作者
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(HasselbladOrange, WarningYellow)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lut.author.firstOrNull()?.uppercase() ?: "O",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lut.author,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = lut.createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

// ==================== 详情信息行 ====================
@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}