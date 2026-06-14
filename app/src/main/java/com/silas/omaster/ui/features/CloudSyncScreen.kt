package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.cloud.CloudSyncManager
import com.silas.omaster.cloud.SyncState
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.LightGray
import com.silas.omaster.ui.theme.MediumGray
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 云同步功能页面
 * 支持多平台云同步
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val syncManager = remember { CloudSyncManager.getInstance(context) }

    // 从 CloudSyncManager 读取真实同步状态
    val syncState by syncManager.syncState.collectAsState()
    val lastSyncTimestamp by syncManager.lastSyncTime.collectAsState()
    val cloudPresets by syncManager.cloudPresets.collectAsState()

    val isSyncing = syncState is SyncState.Syncing

    // 格式化最后同步时间
    val lastSyncTimeText = remember(lastSyncTimestamp) {
        if (lastSyncTimestamp <= 0L) {
            "从未同步"
        } else {
            val diffMs = System.currentTimeMillis() - lastSyncTimestamp
            when {
                diffMs < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
                diffMs < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}分钟前"
                diffMs < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}小时前"
                else -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(lastSyncTimestamp))
                }
            }
        }
    }

    // 从 CloudSyncManager 获取真实的云服务提供商列表
    val providers = remember(syncManager) {
        val urls = syncManager.getCloudPresetUrls()
        val brandColors = mapOf(
            "oppo" to 0xFF1E90FF,
            "realme" to 0xFFFFD700,
            "vivo" to 0xFF4169E1,
            "honor" to 0xFF32CD32
        )
        urls.keys.mapIndexed { index, brand ->
            ProviderInfo(
                name = brand.replaceFirstChar { it.uppercase() },
                color = brandColors[brand.lowercase()] ?: (0xFF1E90FF + index * 0x002020),
                connected = cloudPresets.any { it.brand == brand }
            )
        }
    }

    // 同步内容项（基于真实同步状态）
    val syncItems = remember(lastSyncTimestamp, cloudPresets) {
        val presetCount = cloudPresets.size
        listOf(
            SyncItem("预设同步", presetCount > 0, lastSyncTimeText, presetCount),
            SyncItem("LUT 资源同步", false, "从未同步", 0),
            SyncItem("设置同步", false, "从未同步", 0),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        TopAppBar(
            title = { Text("云同步", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 同步状态卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "同步状态",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                        Text(
                            text = when (syncState) {
                                is SyncState.Syncing -> "正在同步中..."
                                is SyncState.Success -> "同步成功"
                                is SyncState.Error -> "同步失败"
                                else -> if (lastSyncTimestamp > 0) "自动同步已开启" else "尚未同步"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            Text(
                                text = "最后同步：$lastSyncTimeText",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    try {
                                        syncManager.sync()
                                    } catch (e: Exception) {
                                        android.util.Log.e("CloudSyncScreen", "Sync failed", e)
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("立即同步")
                            }
                        }
                    }
                }
            }

            // 云服务提供商
            if (providers.isNotEmpty()) {
                item {
                    Text(
                        text = "云服务提供商",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        providers.forEach { provider ->
                            ProviderCard(provider = provider)
                        }
                    }
                }
            }

            // 同步内容
            item {
                Text(
                    text = "同步内容",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    syncItems.forEach { item ->
                        SyncItemCard(item = item)
                    }
                }
            }

            // 同步特性
            item {
                Text(
                    text = "同步特性",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.Shield,
                        title = "端到端加密",
                        description = "您的数据完全加密，安全可靠"
                    )
                    FeatureCard(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi 自动同步",
                        description = "仅在 Wi-Fi 下自动同步，节省流量"
                    )
                    FeatureCard(
                        icon = Icons.Default.Schedule,
                        title = "历史版本",
                        description = "保留 30 天历史版本，随时回退"
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

data class ProviderInfo(
    val name: String,
    val color: Long,
    val connected: Boolean
)

data class SyncItem(
    val name: String,
    val enabled: Boolean,
    val lastSync: String,
    val count: Int = 0
)

@Composable
private fun ProviderCard(provider: ProviderInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(provider.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Smartphone, null, tint = Color(provider.color))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${provider.name} Cloud",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (provider.connected) "已连接" else "未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provider.connected) SuccessGreen else LightGray
                    )
                }
            }
            if (provider.connected) {
                Icon(Icons.Default.Check, null, tint = SuccessGreen)
            } else {
                TextButton(onClick = {
                    // 触发同步以建立连接
                }) {
                    Text("连接", color = HasselbladOrange)
                }
            }
        }
    }
}

@Composable
private fun SyncItemCard(item: SyncItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Upload, null, tint = HasselbladOrange)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (item.count > 0) "最后同步：${item.lastSync} (${item.count}条)" else "最后同步：${item.lastSync}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (item.enabled) SuccessGreen else MediumGray),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = SuccessGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
