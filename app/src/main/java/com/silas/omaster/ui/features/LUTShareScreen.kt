package com.silas.omaster.ui.features

import android.content.Context
import android.os.Environment
import android.widget.Toast
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
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.data.repository.LUTResourceRepository
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
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
    onDownload: (LUTResource) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = LUTResourceRepository

    // 分类选择 - 对齐 Web 端 LUT_CATEGORIES
    var selectedCategory by remember { mutableStateOf("all") }
    val categories = repo.CATEGORIES

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
    var selectedLUT by remember { mutableStateOf<LUTResource?>(null) }

    // LUT 数据 - 来自真实数据源
    val luts = remember { repo.RESOURCES }

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
        TopAppBar(
            title = {
                Column {
                    Text("LUT 资源库", fontWeight = FontWeight.Bold)
                    Text("视频调色LUT下载", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            actions = {
                Text(
                    text = "${repo.RESOURCES.size} 个LUT",
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("搜索LUT名称、风格...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                cursorColor = HasselbladOrange,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // 分类标签 - 对齐 Web 端
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
                            if (category.key == selectedCategory) HasselbladOrange
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedCategory = category.key
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${category.icon} ${category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (category.key == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                        color = if (category.key == selectedCategory) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
            Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
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
            if (selectedCategory == "all" && searchQuery.isBlank()) {
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
                                    badgeColor = SuccessGreen,
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
                            color = WarningYellow,
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        if (!lut.downloadUrl.startsWith("https://")) {
                                            withContext(Dispatchers.Main) {
                                                downloadingId = null
                                                Toast.makeText(context, "下载地址无效", Toast.LENGTH_SHORT).show()
                                            }
                                            return@launch
                                        }
                                        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
                                        if (!dir.exists()) dir.mkdirs()
                                        val file = File(dir, "${lut.nameEn}.${lut.format}")
                                        URL(lut.downloadUrl).openStream().use { input ->
                                            FileOutputStream(file).use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (!downloadedIds.contains(lut.id)) downloadedIds.add(lut.id)
                                            downloadingId = null
                                            Toast.makeText(context, "LUT 已下载: ${lut.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            downloadingId = null
                                            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("未找到匹配的LUT", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Text("请调整搜索条件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
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
                scope.launch(Dispatchers.IO) {
                    try {
                        if (!lut.downloadUrl.startsWith("https://")) {
                            withContext(Dispatchers.Main) {
                                downloadingId = null
                                Toast.makeText(context, "下载地址无效", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, "${lut.nameEn}.${lut.format}")
                        URL(lut.downloadUrl).openStream().use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (!downloadedIds.contains(lut.id)) downloadedIds.add(lut.id)
                            downloadingId = null
                            Toast.makeText(context, "LUT 已下载: ${lut.name}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            downloadingId = null
                            Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
                color = if (selected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

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
            .width(140.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(HasselbladOrange.copy(alpha = 0.2f))
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
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, color = MaterialTheme.colorScheme.onBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                        "收藏",
                        tint = if (isLiked) ErrorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${LUTResourceRepository.formatDownloads(lut.downloads)}下载",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // 预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(HasselbladOrange.copy(alpha = 0.2f))
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SuccessGreen)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("NEW", color = MaterialTheme.colorScheme.onBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (lut.isHot && !lut.isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HasselbladOrange)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("HOT", color = MaterialTheme.colorScheme.onBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // 格式
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ".${lut.format}",
                        color = MaterialTheme.colorScheme.onBackground,
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
                        "收藏",
                        tint = if (isLiked) ErrorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lut.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
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
                    Icon(Icons.Default.Star, null, tint = WarningYellow, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", lut.rating),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = LUTResourceRepository.formatDownloads(lut.downloads),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${lut.size}x${lut.size}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
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
                        containerColor = if (isDownloaded) SuccessGreen.copy(alpha = 0.2f) else HasselbladOrange.copy(alpha = 0.2f),
                        contentColor = if (isDownloaded) SuccessGreen else HasselbladOrange
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
                            text = "下载 (${LUTResourceRepository.formatFileSize(lut.fileSize)})",
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
    lut: LUTResource,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onLike: () -> Unit,
    onDownload: () -> Unit,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f))
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
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onBackground)
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
                        Text(lut.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(lut.nameEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(HasselbladOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, null, tint = WarningYellow, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", lut.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(lut.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
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
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("#$tag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 信息网格
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailInfoRow("格式", ".${lut.format.uppercase()}")
                        DetailInfoRow("尺寸", "${lut.size}x${lut.size}")
                        DetailInfoRow("文件大小", LUTResourceRepository.formatFileSize(lut.fileSize))
                        DetailInfoRow("下载次数", LUTResourceRepository.formatDownloads(lut.downloads))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 适用场景
                Text("适用场景", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
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
                            contentColor = if (isLiked) ErrorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                                color = MaterialTheme.colorScheme.onBackground
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
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
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
                                    listOf(HasselbladOrange, WarningYellow)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lut.author.firstOrNull()?.uppercase() ?: "O",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
    }
}


