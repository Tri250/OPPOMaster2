package com.silas.omaster.ui.features

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * LUT下载管理页面 - 哈苏大师专业设计
 * 管理已下载、下载中、收藏的LUT资源
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    downloadedLUTs: List<MasterLUT> = emptyList(),
    downloadingLUTs: Map<String, DownloadState> = emptyMap(),
    favoriteLUTs: List<MasterLUT> = emptyList(),
    onDelete: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onApply: (MasterLUT) -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(DownloadTab.DOWNLOADED) }
    
    // 统计
    val downloadedCount = downloadedLUTs.size
    val downloadingCount = downloadingLUTs.size
    val favoriteCount = favoriteLUTs.size
    
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
                        "下载管理",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "管理您的LUT资源",
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )
        
        // === Tab栏 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DownloadTab.values().forEach { tab ->
                DownloadTabButton(
                    tab = tab,
                    count = when (tab) {
                        DownloadTab.DOWNLOADED -> downloadedCount
                        DownloadTab.DOWNLOADING -> downloadingCount
                        DownloadTab.FAVORITES -> favoriteCount
                    },
                    selected = selectedTab == tab,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Select)
                        selectedTab = tab
                    }
                )
            }
        }
        
        // === 内容区 ===
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with
                fadeOut(animationSpec = tween(300))
            },
            label = "TabContent"
        ) { tab ->
            when (tab) {
                DownloadTab.DOWNLOADED -> {
                    if (downloadedLUTs.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.FolderOpen,
                            title = "暂无已下载",
                            subtitle = "去LUT资源库下载您喜欢的色彩配方"
                        )
                    } else {
                        DownloadedLUTList(
                            luts = downloadedLUTs,
                            onDelete = onDelete,
                            onApply = onApply
                        )
                    }
                }
                DownloadTab.DOWNLOADING -> {
                    if (downloadingLUTs.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.CloudDownload,
                            title = "暂无下载任务",
                            subtitle = "正在下载的LUT会在这里显示"
                        )
                    } else {
                        DownloadingLUTList(
                            lutStates = downloadingLUTs,
                            allLUTs = downloadedLUTs + downloadingLUTs.keys.mapNotNull { id ->
                                MasterLUTRepository.ALL_LUTS.find { it.id == id }
                            }
                        )
                    }
                }
                DownloadTab.FAVORITES -> {
                    if (favoriteLUTs.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = "暂无收藏",
                            subtitle = "收藏您喜欢的LUT，方便快速访问"
                        )
                    } else {
                        FavoriteLUTList(
                            luts = favoriteLUTs,
                            onFavorite = onFavorite,
                            onApply = onApply
                        )
                    }
                }
            }
        }
    }
}

// === 子组件 ===

@Composable
private fun DownloadTabButton(
    tab: DownloadTab,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) HasselbladOrange
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.title,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            tab.title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (selected) Color.White.copy(alpha = 0.2f)
                        else HasselbladOrange.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else HasselbladOrange
                )
            }
        }
    }
}

@Composable
private fun DownloadedLUTList(
    luts: List<MasterLUT>,
    onDelete: (String) -> Unit,
    onApply: (MasterLUT) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 总容量统计
        item {
            val totalSize = luts.sumOf { it.fileSize }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HasselbladOrange.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "已下载 ${luts.size} 款LUT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "占用空间 ${formatFileSize(totalSize)}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        items(luts) { lut ->
            DownloadedLUTCard(
                lut = lut,
                onDelete = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onDelete(lut.id)
                },
                onApply = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onApply(lut)
                }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DownloadedLUTCard(
    lut: MasterLUT,
    onDelete: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图
            AsyncImage(
                model = lut.coverImage,
                contentDescription = lut.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lut.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        ".${lut.format.extension}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        "${lut.size.value}³",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        formatFileSize(lut.fileSize),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                if (lut.hasselbladCollection.isNotEmpty()) {
                    Text(
                        "哈苏 ${lut.hasselbladCollection}",
                        fontSize = 12.sp,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 应用按钮
                IconButton(
                    onClick = onApply,
                    modifier = Modifier
                        .size(40.dp)
                        .background(HasselbladOrange, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "应用",
                        tint = Color.White
                    )
                }
                
                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingLUTList(
    lutStates: Map<String, DownloadState>,
    allLUTs: List<MasterLUT>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(lutStates.entries.toList()) { (lutId, state) ->
            val lut = allLUTs.find { it.id == lutId }
            if (lut != null) {
                DownloadingLUTCard(
                    lut = lut,
                    state = state
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DownloadingLUTCard(
    lut: MasterLUT,
    state: DownloadState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = lut.coverImage,
                    contentDescription = lut.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = lut.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (state.isDownloading) "正在下载..."
                               else if (state.error != null) "下载失败"
                               else "等待下载",
                        fontSize = 12.sp,
                        color = when {
                            state.error != null -> Color.Red
                            state.isDownloading -> HasselbladOrange
                            else -> Color.White.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 进度条
            if (state.isDownloading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = HasselbladOrange,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                    Text(
                        "${state.progress}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
            
            // 错误信息
            if (state.error != null) {
                Text(
                    text = state.error,
                    fontSize = 12.sp,
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoriteLUTList(
    luts: List<MasterLUT>,
    onFavorite: (String) -> Unit,
    onApply: (MasterLUT) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(luts) { lut ->
            FavoriteLUTCard(
                lut = lut,
                onFavorite = {
                    haptic.perform(HapticFeedbackType.Select)
                    onFavorite(lut.id)
                },
                onApply = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onApply(lut)
                }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun FavoriteLUTCard(
    lut: MasterLUT,
    onFavorite: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = lut.coverImage,
                contentDescription = lut.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lut.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        lut.rating.toString(),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        formatCount(lut.downloads),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                if (lut.hasselbladCollection.isNotEmpty()) {
                    Text(
                        "哈苏 ${lut.hasselbladCollection}",
                        fontSize = 12.sp,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 取消收藏
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "取消收藏",
                        tint = HasselbladOrange
                    )
                }
                
                // 应用
                IconButton(
                    onClick = onApply,
                    modifier = Modifier
                        .size(40.dp)
                        .background(HasselbladOrange, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "应用",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            subtitle,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// === 枚举和工具 ===

enum class DownloadTab(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    DOWNLOADED("已下载", Icons.Default.Folder),
    DOWNLOADING("下载中", Icons.Default.CloudDownload),
    FAVORITES("收藏", Icons.Default.Favorite)
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