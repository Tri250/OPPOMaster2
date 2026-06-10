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
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.data.local.LUTDownloadManager
import com.silas.omaster.data.local.LUTDownloadCallback
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LUT 资源分享页面
 * 
 * 功能：
 * - 展示专业 LUT 滤镜库
 * - LUT 分类浏览
 * - LUT 下载和应用（带校验）
 * - LUT 效果预览
 * - 下载状态显示
 * 
 * 对齐 Web 端 LUTSharePage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit,
    onDownload: (LUTResource) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 分类选择
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "电影", "胶片", "风景", "人像", "商业", "创意")
    
    // LUT 数据（使用扩展后的 LUTResource）
    val luts = remember {
        listOf(
            LUTResource("lut_1", "柯达 Portra 400", "经典胶片风格，温暖柔和", "胶片", 
                "https://cdn.example.com/luts/portra400_v2.cube", 1500, 4.8,
                fileSize = 45000, checksum = "a1b2c3d4e5f6...", isFree = true, version = 2),
            LUTResource("lut_2", "富士 Velvia 50", "高饱和度，风景首选", "胶片",
                "https://cdn.example.com/luts/velvia50_v1.cube", 800, 4.5,
                fileSize = 42000, checksum = "b2c3d4e5f6g7...", isFree = false, version = 1),
            LUTResource("lut_3", "好莱坞电影", "电影级调色，专业质感", "电影",
                "https://cdn.example.com/luts/hollywood_v3.cube", 2500, 4.9,
                fileSize = 48000, checksum = "c3d4e5f6g7h8...", isFree = true, version = 3),
            LUTResource("lut_4", "哈苏 HNCS", "自然色彩还原，专业标准", "风景",
                "https://cdn.example.com/luts/hncs_v1.cube", 3000, 5.0,
                fileSize = 50000, checksum = "d4e5f6g7h8i9...", isFree = true, version = 1),
            LUTResource("lut_5", "人像柔光", "柔和肤色，自然美化", "人像",
                "https://cdn.example.com/luts/portrait_soft_v2.cube", 1200, 4.6,
                fileSize = 43000, checksum = "e5f6g7h8i9j0...", isFree = false, version = 2),
            LUTResource("lut_6", "商业广告", "明亮通透，产品展示", "商业",
                "https://cdn.example.com/luts/commercial_v1.cube", 900, 4.4,
                fileSize = 41000, checksum = "f6g7h8i9j0k1...", isFree = true, version = 1),
            LUTResource("lut_7", "赛博朋克", "霓虹色彩，科幻风格", "创意",
                "https://cdn.example.com/luts/cyberpunk_v2.cube", 600, 4.3,
                fileSize = 44000, checksum = "g7h8i9j0k1l2...", isFree = false, version = 2),
            LUTResource("lut_8", "黑白经典", "黑白胶片质感", "胶片",
                "https://cdn.example.com/luts/bw_classic_v1.cube", 1800, 4.7,
                fileSize = 38000, checksum = "h8i9j0k1l2m3...", isFree = true, version = 1),
            LUTResource("lut_9", "日落暖调", "温暖日落氛围", "风景",
                "https://cdn.example.com/luts/sunset_v1.cube", 700, 4.2,
                fileSize = 39000, checksum = "i9j0k1l2m3n4...", isFree = false, version = 1),
            LUTResource("lut_10", "复古怀旧", "复古褪色效果", "创意",
                "https://cdn.example.com/luts/vintage_v2.cube", 1100, 4.5,
                fileSize = 40000, checksum = "j0k1l2m3n4o5...", isFree = true, version = 2)
        )
    }
    
    // 下载状态映射
    val downloadStates = remember { mutableStateMapOf<String, DownloadState>() }
    
    // 过滤后的 LUT
    val filteredLuts = if (selectedCategory == "全部") {
        luts
    } else {
        luts.filter { it.category == selectedCategory }
    }
    
    // 缓存大小
    var cacheSize by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        cacheSize = LUTDownloadManager.getCacheSize(context)
    }
    
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
                // 缓存大小显示
                Text(
                    text = "${String.format("%.1f", cacheSize)} MB",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                // 清理缓存按钮
                IconButton(onClick = {
                    LUTDownloadManager.clearCache(context)
                    cacheSize = 0.0
                }) {
                    Icon(Icons.Default.Delete, "清理缓存", tint = Color.White.copy(alpha = 0.6f))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
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
                            haptic.perform(HapticFeedbackType.Select)
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

        // LUT 数量统计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filteredLuts.size} 个 LUT",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${filteredLuts.filter { it.isFree }.size} 个免费",
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladOrange
            )
        }

        // LUT 列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLuts) { lut ->
                LUTCard(
                    lut = lut,
                    downloadState = downloadStates[lut.id] ?: DownloadState.Idle,
                    isDownloaded = lut.isDownloaded(context),
                    onDownload = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        scope.launch {
                            downloadLUTWithProgress(context, lut, downloadStates)
                        }
                        onDownload(lut)
                    },
                    onApply = {
                        // 应用 LUT（后续实现）
                        haptic.perform(HapticFeedbackType.Confirm)
                    }
                )
            }
        }
    }
}

/**
 * 下载 LUT 并更新进度状态
 */
private suspend fun downloadLUTWithProgress(
    context: android.content.Context,
    lut: LUTResource,
    downloadStates: MutableMap<String, DownloadState>
) {
    val result = LUTDownloadManager.downloadLUT(
        context = context,
        lut = lut,
        callback = object : LUTDownloadCallback {
            override fun onStart(lutId: String) {
                downloadStates[lutId] = DownloadState.Downloading(0f)
            }
            override fun onProgress(lutId: String, bytesDownloaded: Long, totalBytes: Long) {
                val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                downloadStates[lutId] = DownloadState.Downloading(progress)
            }
            override fun onSuccess(lutId: String, file: File) {
                downloadStates[lutId] = DownloadState.Success
            }
            override fun onError(lutId: String, error: Throwable, retryCount: Int) {
                downloadStates[lutId] = DownloadState.Error(error.message ?: "下载失败")
            }
            override fun onRetry(lutId: String, attempt: Int) {
                downloadStates[lutId] = DownloadState.Retrying(attempt)
            }
            override fun onVerifyStart(lutId: String) {
                downloadStates[lutId] = DownloadState.Verifying
            }
            override fun onVerifySuccess(lutId: String) {
                // 校验成功，等待最终成功回调
            }
            override fun onVerifyFailed(lutId: String, reason: String) {
                downloadStates[lutId] = DownloadState.Error("校验失败: $reason")
            }
        }
    )
}

/**
 * 下载状态
 */
private sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Verifying : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
    data class Retrying(val attempt: Int) : DownloadState()
}

@Composable
private fun LUTCard(
    lut: LUTResource,
    downloadState: DownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：预览图和信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 预览图占位
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = lut.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // 已下载标识
                    if (isDownloaded) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "已下载",
                            tint = SuccessGreen,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
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
                        
                        // 版本标识
                        if (lut.version > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "v${lut.version}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    Text(
                        text = lut.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "分类: ${lut.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        
                        // 文件大小
                        lut.fileSize?.let { size ->
                            Text(
                                text = "${size / 1024}KB",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp
                            )
                        }
                        
                        // 评分
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = String.format("%.1f", lut.rating),
                                color = HasselbladOrange,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 右侧：下载/应用按钮
            when (downloadState) {
                is DownloadState.Downloading -> {
                    // 进度指示器
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = downloadState.progress,
                            modifier = Modifier.size(24.dp),
                            color = HasselbladOrange,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "${(downloadState.progress * 100).toInt()}%",
                            color = HasselbladOrange,
                            fontSize = 10.sp
                        )
                    }
                }
                is DownloadState.Verifying -> {
                    // 校验指示器
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HasselbladOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = HasselbladOrange,
                            strokeWidth = 2.dp
                        )
                    }
                }
                is DownloadState.Success, DownloadState.Idle -> {
                    if (isDownloaded) {
                        // 已下载，显示应用按钮
                        IconButton(
                            onClick = onApply,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Check,
                                "应用",
                                tint = SuccessGreen
                            )
                        }
                    } else {
                        // 未下载，显示下载按钮
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HasselbladOrange.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Download,
                                "下载",
                                tint = HasselbladOrange
                            )
                        }
                    }
                }
                is DownloadState.Error -> {
                    // 错误状态，显示重试按钮
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "重试",
                            tint = Color.Red
                        )
                    }
                }
                is DownloadState.Retrying -> {
                    // 重试中
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HasselbladOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "重试 ${downloadState.attempt}",
                            color = HasselbladOrange,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        
        // 错误信息显示
        if (downloadState is DownloadState.Error) {
            Text(
                text = downloadState.message,
                color = Color.Red.copy(alpha = 0.8f),
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

// SuccessGreen 颜色定义（如果未在 theme 中定义）
private val SuccessGreen = Color(0xFF4CAF50)