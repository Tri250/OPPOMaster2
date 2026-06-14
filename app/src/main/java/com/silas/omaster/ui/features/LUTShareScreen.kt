package com.silas.omaster.ui.features

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*
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
    onDownload: (LUTItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // 分类选择
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "电影", "胶片", "风景", "人像", "商业", "创意")

    // 搜索
    var searchQuery by remember { mutableStateOf("") }
    // 排序
    var sortBy by remember { mutableStateOf(SortType.DOWNLOADS) }
    // 喜欢/收藏
    val likedIds = remember { mutableStateListOf<String>() }
    // 已下载
    val downloadedIds = remember { mutableStateListOf<String>() }
    // 正在下载
    var downloadingId by remember { mutableStateOf<String?>(null) }
    // 详情弹窗
    var selectedLUT by remember { mutableStateOf<LUTItem?>(null) }

    // LUT 数据
    val luts = remember {
        listOf(
            LUTItem("lut_1", "柯达 Portra 400", "Kodak Portra 400", "经典胶片风格，温暖柔和", "胶片", "https://example.com/lut1.cube", 1024, 8, true, true, false, 4.8f, 12580, "https://omaster.app/authors/kodak", listOf("电影", "人像"), "2026-05-12"),
            LUTItem("lut_2", "富士 Velvia 50", "Fuji Velvia 50", "高饱和度，风景首选", "胶片", "https://example.com/lut2.cube", 2048, 12, false, true, true, 4.6f, 8932, "https://omaster.app/authors/fuji", listOf("风景", "自然"), "2026-05-18"),
            LUTItem("lut_3", "好莱坞电影", "Hollywood Cinema", "电影级调色，专业质感", "电影", "https://example.com/lut3.cube", 1024, 6, true, false, false, 4.9f, 23411, "https://omaster.app/authors/hollywood", listOf("电影", "广告"), "2026-04-20"),
            LUTItem("lut_4", "哈苏 HNCS", "Hasselblad HNCS", "自然色彩还原，专业标准", "风景", "https://example.com/lut4.cube", 2048, 10, true, true, true, 4.7f, 15802, "https://omaster.app/authors/hasselblad", listOf("风景", "人像"), "2026-05-25"),
            LUTItem("lut_5", "人像柔光", "Portrait Soft", "柔和肤色，自然美化", "人像", "https://example.com/lut5.cube", 1024, 4, false, false, false, 4.5f, 9320, "https://omaster.app/authors/portrait", listOf("人像", "美妆"), "2026-03-10"),
            LUTItem("lut_6", "商业广告", "Commercial Ad", "明亮通透，产品展示", "商业", "https://example.com/lut6.cube", 2048, 14, true, false, false, 4.4f, 6210, "https://omaster.app/authors/ad", listOf("商业", "产品"), "2026-02-08"),
            LUTItem("lut_7", "赛博朋克", "Cyberpunk", "霓虹色彩，科幻风格", "创意", "https://example.com/lut7.cube", 1024, 5, false, true, true, 4.7f, 18230, "https://omaster.app/authors/cyber", listOf("创意", "夜景"), "2026-05-30"),
            LUTItem("lut_8", "黑白经典", "Classic B&W", "黑白胶片质感", "胶片", "https://example.com/lut8.cube", 512, 2, true, false, false, 4.3f, 4521, "https://omaster.app/authors/bw", listOf("人像", "街拍"), "2026-01-22"),
            LUTItem("lut_9", "日落暖调", "Sunset Warm", "温暖日落氛围", "风景", "https://example.com/lut9.cube", 1024, 7, false, false, true, 4.6f, 11230, "https://omaster.app/authors/sunset", listOf("风景", "旅行"), "2026-04-30"),
            LUTItem("lut_10", "复古怀旧", "Retro Vintage", "复古褪色效果", "创意", "https://example.com/lut10.cube", 1024, 6, true, false, false, 4.5f, 7890, "https://omaster.app/authors/retro", listOf("创意", "胶片"), "2026-03-15")
        )
    }

    // 过滤 + 排序
    val filteredLuts = remember(searchQuery, selectedCategory, sortBy, luts) {
        var result = if (searchQuery.isNotBlank()) {
            luts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        } else if (selectedCategory == "全部") {
            luts
        } else {
            luts.filter { it.category == selectedCategory }
        }
        result = when (sortBy) {
            SortType.DOWNLOADS -> result.sortedByDescending { it.downloads }
            SortType.RATING -> result.sortedByDescending { it.rating }
            SortType.NEWEST -> result.sortedByDescending { it.createdAt }
        }
        result
    }

    // 2026新品 / 热门
    val newLuts = remember(luts) { luts.filter { it.isNew } }
    val hotLuts = remember(luts) { luts.filter { it.isHot && !it.isNew }.take(6) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = {
                Column {
                    Text("LUT 资源库", fontWeight = FontWeight.Bold)
                    Text("视频调色LUT下载", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                Text(
                    text = "${luts.size} 个LUT",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("搜索LUT名称、风格...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = HasselbladOrange,
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF1A1A1A)
            )
        )

        // 分类标签
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (category == selectedCategory) HasselbladOrange
                            else Color(0xFF2A2A2A)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedCategory = category
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                        color = if (category == selectedCategory) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 排序
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            SortChip("最多下载", sortBy == SortType.DOWNLOADS) { sortBy = SortType.DOWNLOADS }
            Spacer(modifier = Modifier.width(6.dp))
            SortChip("最高评分", sortBy == SortType.RATING) { sortBy = SortType.RATING }
            Spacer(modifier = Modifier.width(6.dp))
            SortChip("最新发布", sortBy == SortType.NEWEST) { sortBy = SortType.NEWEST }
        }

        // 列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 2026新品 & 热门
            if (selectedCategory == "全部" && searchQuery.isBlank()) {
                if (newLuts.isNotEmpty()) {
                    item {
                        Text(
                            text = "2026新品",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = HasselbladOrange,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(newLuts) { lut ->
                                LUTPosterCard(
                                    lut = lut,
                                    badge = "NEW",
                                    badgeColor = Color(0xFF4CAF50),
                                    isLiked = likedIds.contains(lut.id),
                                    onLike = { likedIds.toggle(lut.id) },
                                    onClick = { selectedLUT = lut }
                                )
                            }
                        }
                    }
                }
                if (hotLuts.isNotEmpty()) {
                    item {
                        Text(
                            text = "热门推荐",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFC107),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(hotLuts) { lut ->
                                LUTPosterCard(
                                    lut = lut,
                                    badge = "HOT",
                                    badgeColor = HasselbladOrange,
                                    isLiked = likedIds.contains(lut.id),
                                    onLike = { likedIds.toggle(lut.id) },
                                    onClick = { selectedLUT = lut }
                                )
                            }
                        }
                    }
                }
            }

            // LUT 数量统计
            item {
                Text(
                    text = "${filteredLuts.size} 个 LUT  ·  ${filteredLuts.count { it.isFree }} 个免费",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // LUT 网格列表
            items(filteredLuts.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { lut ->
                        LUTGridCard(
                            modifier = Modifier.weight(1f),
                            lut = lut,
                            isLiked = likedIds.contains(lut.id),
                            isDownloaded = downloadedIds.contains(lut.id),
                            isDownloading = downloadingId == lut.id,
                            onLike = { likedIds.toggle(lut.id) },
                            onClick = { selectedLUT = lut },
                            onDownload = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                downloadingId = lut.id
                                onDownload(lut)
                                if (!downloadedIds.contains(lut.id)) downloadedIds.add(lut.id)
                                downloadingId = null
                            }
                        )
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (filteredLuts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("未找到匹配的LUT", color = Color.White.copy(alpha = 0.5f))
                        Text("请调整搜索条件", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.3f))
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
            onLike = { likedIds.toggle(lut.id) },
            onDownload = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                downloadingId = lut.id
                onDownload(lut)
                if (!downloadedIds.contains(lut.id)) downloadedIds.add(lut.id)
                downloadingId = null
            },
            onDismiss = { selectedLUT = null }
        )
    }
}

private enum class SortType { DOWNLOADS, RATING, NEWEST }

private fun MutableList<String>.toggle(id: String) {
    if (contains(id)) remove(id) else add(id)
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HasselbladOrange.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun LUTPosterCard(
    lut: LUTItem,
    badge: String,
    badgeColor: Color,
    isLiked: Boolean,
    onLike: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(HasselbladOrange.copy(alpha = 0.2f))
            ) {
                Text(
                    text = lut.name.firstOrNull()?.toString() ?: "",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange,
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isLiked) Color(0xFFE53935) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDownloads(lut.downloads)}下载",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LUTGridCard(
    modifier: Modifier = Modifier,
    lut: LUTItem,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column {
            // 预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(HasselbladOrange.copy(alpha = 0.2f))
            ) {
                Text(
                    text = lut.name.firstOrNull()?.toString() ?: "",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange,
                    modifier = Modifier.align(Alignment.Center)
                )
                // 徽标
                Row(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (lut.isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (lut.isHot && !lut.isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HasselbladOrange)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("HOT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // 格式
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ".${lut.format}",
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
                // 收藏
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isLiked) Color(0xFFE53935) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // 信息
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lut.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lut.suitableFor.take(2).forEach { tag ->
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 评分/下载/尺寸
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", lut.rating),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Download, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatDownloads(lut.downloads),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${lut.size}x${lut.size}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                // 下载按钮
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) Color(0xFF4CAF50).copy(alpha = 0.2f) else HasselbladOrange.copy(alpha = 0.2f),
                        contentColor = if (isDownloaded) Color(0xFF4CAF50) else HasselbladOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    if (isDownloaded) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("已下载", fontSize = 11.sp)
                    } else if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载中...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "下载 (${formatFileSize(lut.fileSize)})",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LUTDetailDialog(
    lut: LUTItem,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onLike: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = lut.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close, "关闭", tint = Color.White)
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
                        Text(lut.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(lut.nameEn, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HasselbladOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", lut.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(lut.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                // 标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lut.suitableFor.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("#$tag", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 信息网格
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailInfoRow("格式", ".${lut.format.uppercase()}")
                        DetailInfoRow("尺寸", "${lut.size}x${lut.size}")
                        DetailInfoRow("文件大小", formatFileSize(lut.fileSize))
                        DetailInfoRow("下载次数", formatDownloads(lut.downloads))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 适用场景
                Text("适用场景", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lut.suitableFor.forEach { scene ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HasselbladOrange.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(scene, style = MaterialTheme.typography.bodySmall, color = HasselbladOrange)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLike,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isLiked) Color(0xFFE53935) else Color.White.copy(alpha = 0.7f)
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
                    Button(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDownloaded) Color(0xFF4CAF50) else HasselbladOrange
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
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(HasselbladOrange, Color(0xFFFF9800))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lut.author.firstOrNull()?.uppercase() ?: "O",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lut.author,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = lut.createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

private fun formatFileSize(sizeInKb: Int): String = when {
    sizeInKb >= 1024 -> String.format(Locale.US, "%.1f MB", sizeInKb / 1024f)
    else -> "$sizeInKb KB"
}

private fun formatDownloads(count: Int): String = when {
    count >= 10000 -> String.format(Locale.US, "%.1f万", count / 10000f)
    else -> count.toString()
}

/**
 * LUT 数据项
 */
data class LUTItem(
    val id: String,
    val name: String,
    val nameEn: String,
    val description: String,
    val category: String,
    val url: String,
    val size: Int = 1024,
    val fileSize: Int = 8,    // KB
    val isFree: Boolean = false,
    val isNew: Boolean = false,
    val isHot: Boolean = false,
    val rating: Float = 4.5f,
    val downloads: Int = 1000,
    val authorUrl: String = "",
    val suitableFor: List<String> = emptyList(),
    val createdAt: String = "2026-01-01",
    val author: String = "OMaster官方"
)
