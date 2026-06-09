package com.silas.omaster.ui.features

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.data.model.*
import com.silas.omaster.ui.components.LUTDetailBottomSheet
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * LUT资源库功能页面 - 哈苏大师专业设计
 * 提供9款哈苏胶片LUT滤镜，完整信息展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // 状态管理
    var selectedCategory by remember { mutableStateOf(LUTCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(LUTSortBy.DOWNLOADS) }
    var selectedLUT by remember { mutableStateOf<MasterLUT?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var downloadedIds by remember { mutableStateOf(setOf<String>()) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0) }
    
    // 数据源
    val allLUTs = MasterLUTRepository.ALL_LUTS
    val hotLUTs = MasterLUTRepository.getHotLUTs()
    val newLUTs = allLUTs.filter { it.isNew }
    
    // 过滤和排序
    val filteredLUTs = remember(selectedCategory, searchQuery, sortBy) {
        allLUTs.filter { lut ->
            (selectedCategory == LUTCategory.ALL || lut.category == selectedCategory) &&
            (searchQuery.isEmpty() || lut.name.contains(searchQuery, ignoreCase = true) ||
             lut.nameEn.contains(searchQuery, ignoreCase = true) ||
             lut.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }.sortedWith(getComparator(sortBy))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // === 标题栏 ===
        TopAppBar(
            title = {
                Column {
                    Text(
                        "LUT 资源库",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "哈苏大师色彩配方",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 统计
                Text(
                    "${allLUTs.size} 款",
                    fontSize = 12.sp,
                    color = HasselbladOrange,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 搜索栏 ===
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
            
            // === 分类标签 ===
            item {
                CategoryChips(
                    categories = LUTCategory.entries.toList(),
                    selected = selectedCategory,
                    onSelect = { 
                        haptic.perform(HapticFeedbackType.Select)
                        selectedCategory = it 
                    }
                )
            }
            
            // === 排序选择器 ===
            item {
                SortSelector(
                    sortBy = sortBy,
                    onSortChange = { 
                        haptic.perform(HapticFeedbackType.Select)
                        sortBy = it 
                    }
                )
            }
            
            // === 热门推荐横滑区 ===
            if (selectedCategory == LUTCategory.ALL && searchQuery.isEmpty()) {
                item {
                    HorizontalSection(
                        title = "热门推荐",
                        icon = Icons.Default.Whatshot,
                        luts = hotLUTs.take(6),
                        downloadedIds = downloadedIds,
                        favoriteIds = favoriteIds,
                        onItemClick = { 
                            haptic.perform(HapticFeedbackType.Select)
                            selectedLUT = it
                            showDetailSheet = true
                        },
                        onFavorite = { id ->
                            favoriteIds = if (favoriteIds.contains(id)) {
                                favoriteIds - id
                            } else {
                                favoriteIds + id
                            }
                        }
                    )
                }
            }
            
            // === 新品上架横滑区 ===
            if (selectedCategory == LUTCategory.ALL && searchQuery.isEmpty() && newLUTs.isNotEmpty()) {
                item {
                    HorizontalSection(
                        title = "新品上架",
                        icon = Icons.Default.NewReleases,
                        luts = newLUTs,
                        downloadedIds = downloadedIds,
                        favoriteIds = favoriteIds,
                        onItemClick = { 
                            haptic.perform(HapticFeedbackType.Select)
                            selectedLUT = it
                            showDetailSheet = true
                        },
                        onFavorite = { id ->
                            favoriteIds = if (favoriteIds.contains(id)) {
                                favoriteIds - id
                            } else {
                                favoriteIds + id
                            }
                        },
                        showNewBadge = true
                    )
                }
            }
            
            // === LUT列表 ===
            items(filteredLUTs) { lut ->
                LUTCard(
                    lut = lut,
                    isDownloaded = downloadedIds.contains(lut.id),
                    isFavorite = favoriteIds.contains(lut.id),
                    isDownloading = downloadingId == lut.id,
                    downloadProgress = downloadProgress,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Select)
                        selectedLUT = lut
                        showDetailSheet = true
                    },
                    onFavorite = {
                        favoriteIds = if (favoriteIds.contains(lut.id)) {
                            favoriteIds - lut.id
                        } else {
                            favoriteIds + lut.id
                        }
                    },
                    onDownload = {
                        downloadingId = lut.id
                        // 模拟下载进度
                        downloadProgress = 0
                        // 实际项目中应调用ViewModel下载
                    }
                )
            }
            
            // === 底部间距 ===
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // === 详情 BottomSheet ===
    if (showDetailSheet && selectedLUT != null) {
        LUTDetailBottomSheet(
            lut = selectedLUT!!,
            isDownloaded = downloadedIds.contains(selectedLUT!!.id),
            isDownloading = downloadingId == selectedLUT!!.id,
            downloadProgress = downloadProgress,
            isFavorite = favoriteIds.contains(selectedLUT!!.id),
            onDownload = {
                haptic.perform(HapticFeedbackType.Confirm)
                downloadingId = selectedLUT!!.id
                // 模拟下载完成
                downloadedIds = downloadedIds + selectedLUT!!.id
                downloadingId = null
            },
            onFavorite = {
                favoriteIds = if (favoriteIds.contains(selectedLUT!!.id)) {
                    favoriteIds - selectedLUT!!.id
                } else {
                    favoriteIds + selectedLUT!!.id
                }
            },
            onShare = {
                haptic.perform(HapticFeedbackType.Confirm)
            },
            onDismiss = {
                haptic.perform(HapticFeedbackType.ToggleOff)
                showDetailSheet = false
                selectedLUT = null
            },
            relatedLUTs = allLUTs.filter { 
                it.id != selectedLUT!!.id && 
                it.hasselbladCollection == selectedLUT!!.hasselbladCollection 
            }.take(4),
            onRelatedClick = { lut ->
                selectedLUT = lut
            }
        )
    }
}

// === 子组件 ===

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { 
            Text("搜索 LUT 名称、风格...", color = Color.White.copy(alpha = 0.4f))
        },
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.5f))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = HasselbladOrange,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            textColor = Color.White,
            placeholderColor = Color.White.copy(alpha = 0.4f),
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun CategoryChips(
    categories: List<LUTCategory>,
    selected: LUTCategory,
    onSelect: (LUTCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(category.icon, fontSize = 14.sp)
                        Text(category.displayName, fontSize = 13.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.1f),
                    labelColor = Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.White.copy(alpha = 0.2f),
                    selectedBorderColor = HasselbladOrange,
                    enabled = true,
                    selected = selected == category
                )
            )
        }
    }
}

@Composable
private fun SortSelector(
    sortBy: LUTSortBy,
    onSortChange: (LUTSortBy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Sort,
            contentDescription = "排序",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
        
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    sortBy.displayName,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .width(140.dp)
            ) {
                LUTSortBy.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (sortBy == sort) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = HasselbladOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    sort.displayName,
                                    color = if (sortBy == sort) HasselbladOrange else Color.White
                                )
                            }
                        },
                        onClick = {
                            onSortChange(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "${filteredLUTs.size} 结果",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun HorizontalSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    luts: List<MasterLUT>,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    onItemClick: (MasterLUT) -> Unit,
    onFavorite: (String) -> Unit,
    showNewBadge: Boolean = false
) {
    Column {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = HasselbladOrange,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${luts.size} 款",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        // 横滑列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(luts) { lut ->
                HorizontalLUTCard(
                    lut = lut,
                    isDownloaded = downloadedIds.contains(lut.id),
                    isFavorite = favoriteIds.contains(lut.id),
                    showNewBadge = showNewBadge || lut.isNew,
                    onClick = { onItemClick(lut) },
                    onFavorite = { onFavorite(lut.id) }
                )
            }
        }
    }
}

@Composable
private fun HorizontalLUTCard(
    lut: MasterLUT,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    showNewBadge: Boolean = false,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column {
            // 封面图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = lut.coverImage,
                    contentDescription = lut.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // 渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    PureBlack.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                
                // NEW徽章
                if (showNewBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "NEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                // HOT徽章
                if (lut.isHot && !showNewBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(HasselbladOrange, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "HOT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                // HNCS认证徽章
                if (lut.isHncsCertified) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "HNCS认证",
                        tint = HasselbladOrange,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                
                // 收藏按钮
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(
                            if (isFavorite) HasselbladOrange.copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorite) HasselbladOrange else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // 已下载标记
                if (isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已下载",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
            }
            
            // 信息
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = lut.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        lut.rating.toString(),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        "·",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    
                    Text(
                        formatCount(lut.downloads),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LUTCard(
    lut: MasterLUT,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图（真实图片）
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = lut.coverImage,
                    contentDescription = lut.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // 渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    PureBlack.copy(alpha = 0.4f)
                                )
                            )
                        )
                )
                
                // 状态徽章
                if (lut.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (lut.isHot) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(HasselbladOrange, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("HOT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                
                // HNCS认证
                if (lut.isHncsCertified) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lut.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = lut.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // 标签
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 系列
                    if (lut.hasselbladCollection.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(HasselbladOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                lut.hasselbladCollection,
                                fontSize = 10.sp,
                                color = HasselbladOrange
                            )
                        }
                    }
                    
                    // 格式
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            ".${lut.format.extension}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    // 尺寸
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${lut.size.value}³",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // 统计
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Text(lut.rating.toString(), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Text(formatCount(lut.downloads), fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                    
                    Text(
                        formatFileSize(lut.fileSize),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // 操作按钮
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 收藏
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorite) HasselbladOrange else Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // 下载按钮
                if (isDownloading) {
                    // 进度指示器
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(HasselbladOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress / 100f },
                            color = HasselbladOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "${downloadProgress}",
                            fontSize = 10.sp,
                            color = HasselbladOrange
                        )
                    }
                } else if (isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已下载",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(36.dp)
                            .background(HasselbladOrange, CircleShape)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

// === 工具函数 ===

private fun getComparator(sortBy: LUTSortBy): Comparator<MasterLUT> {
    return when (sortBy) {
        LUTSortBy.DOWNLOADS -> compareByDescending { it.downloads }
        LUTSortBy.RATING -> compareByDescending { it.rating }
        LUTSortBy.NEWEST -> compareByDescending { it.createdAt }
        LUTSortBy.NAME -> compareBy { it.name }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}