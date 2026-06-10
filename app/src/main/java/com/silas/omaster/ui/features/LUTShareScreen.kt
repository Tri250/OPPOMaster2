package com.silas.omaster.ui.features

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.local.LUTLocalDataSource
import com.silas.omaster.data.model.LUTCategory
import com.silas.omaster.data.model.MasterLUT
import com.silas.omaster.data.remote.LUTRemoteDataSource
import com.silas.omaster.data.repository.DownloadProgress
import com.silas.omaster.data.repository.LUTRepository
import com.silas.omaster.data.repository.Resource
import com.silas.omaster.ui.theme.*

/**
 * LUT 资源分享页面（与 Web 端 LUTSharePage.tsx 完全对齐）
 *
 * 数据流：UI → ViewModel → Repository → DataSource
 * - MasterLUT 统一数据模型（双端统一）
 * - 真实数据源（远程 API → CDN → 本地默认）
 * - 缓存优先 + 网络刷新策略
 *
 * 功能：
 * - 展示专业 LUT 滤镜库
 * - LUT 分类浏览（含 HASSELBLAD 哈苏专属）
 * - LUT 下载/收藏/评分
 * - LUT 搜索 + 多种排序
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit,
    onDownload: (MasterLUT) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val viewModel: LUTViewModel = viewModel(
        factory = LUTViewModelFactory.provide(context)
    )

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lutsResource by viewModel.luts.collectAsState()
    val hotLUTs by viewModel.hotLUTs.collectAsState()
    val newLUTs by viewModel.newLUTs.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    // 分类列表 - 使用 MasterLUT 中的 LUTCategory
    val categories = remember { LUTCategory.entries.toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("LUT 资源库", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "刷新", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索 LUT...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", tint = HasselbladOrange) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HasselbladOrange,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = HasselbladOrange
            )
        )

        // 分类标签
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
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
                            haptic.perform(HapticFeedbackType.Select)
                            viewModel.selectCategory(category)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${category.icon} ${category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                        color = if (category == selectedCategory) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 资源状态展示
        when (val resource = lutsResource) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HasselbladOrange)
                }
            }
            is Resource.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", color = Color.White, fontSize = 16.sp)
                        Text(
                            resource.message,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            is Resource.Success -> {
                LUTListContent(
                    luts = resource.data,
                    downloadStates = downloadStates,
                    favoriteIds = favoriteIds,
                    onDownload = { lut ->
                        viewModel.downloadLUT(lut)
                        onDownload(lut)
                    },
                    onToggleFavorite = { lutId ->
                        viewModel.toggleFavorite(lutId)
                    }
                )
            }
        }
    }
}

@Composable
private fun LUTListContent(
    luts: List<MasterLUT>,
    downloadStates: Map<String, DownloadProgress>,
    favoriteIds: Set<String>,
    onDownload: (MasterLUT) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LUT 数量统计
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${luts.size} 个 LUT",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "${luts.count { it.isFree }} 个免费",
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladOrange
                )
            }
        }

        items(luts) { lut ->
            LUTCardReal(
                lut = lut,
                downloadProgress = downloadStates[lut.id],
                isFavorite = lut.id in favoriteIds,
                onDownload = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onDownload(lut)
                },
                onToggleFavorite = { onToggleFavorite(lut.id) }
            )
        }
    }
}

@Composable
private fun LUTCardReal(
    lut: MasterLUT,
    downloadProgress: DownloadProgress?,
    isFavorite: Boolean,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 左侧：信息
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // 预览图占位
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HasselbladOrange.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = lut.nameEn.firstOrNull()?.toString() ?: lut.name.firstOrNull()?.toString() ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = lut.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            if (lut.isFree) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HasselbladOrange)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "免费",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (lut.isHncsCertified) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFB300))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "HNCS",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = lut.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 2
                        )

                        // 评分与下载数
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", lut.rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatCount(lut.downloads),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 右侧：操作按钮
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "收藏",
                            tint = if (isFavorite) Color(0xFFFF6B9D) else Color.White.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDownload) {
                        Icon(
                            Icons.Default.Download,
                            "下载",
                            tint = HasselbladOrange
                        )
                    }
                }
            }

            // 下载进度
            downloadProgress?.let { progress ->
                Spacer(modifier = Modifier.height(8.dp))
                when (progress) {
                    is DownloadProgress.Starting -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = HasselbladOrange,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                    is DownloadProgress.Downloading -> {
                        LinearProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = HasselbladOrange,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                    is DownloadProgress.Completed -> {
                        Text(
                            "✓ 已下载",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp
                        )
                    }
                    is DownloadProgress.Error -> {
                        Text(
                            "✗ ${progress.message}",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 格式化数字（12345 → 1.2万）
 */
private fun formatCount(count: Long): String {
    return when {
        count >= 100_000 -> String.format("%.1fw", count / 10_000.0)
        count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}

/**
 * LUTViewModel Factory - 注入 LUTRepository
 */
class LUTViewModelFactory(private val repository: LUTRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LUTViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LUTViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }

    companion object {
        fun provide(context: Context): LUTViewModelFactory {
            // 构造完整的 Repository
            val localDataSource = LUTLocalDataSource(context.applicationContext)
            val remoteDataSource = LUTRemoteDataSource()
            val repository = LUTRepository(remoteDataSource, localDataSource)
            return LUTViewModelFactory(repository)
        }
    }
}
