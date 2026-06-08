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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.cloud.CloudSyncManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import kotlinx.coroutines.launch

/**
 * 云同步功能页面
 * 支持多平台云同步
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val syncManager = remember { CloudSyncManager.getInstance() }

    val isSyncing = remember { mutableStateOf(false) }
    val lastSyncTime = remember { mutableStateOf("2分钟前") }

    val providers = listOf(
        ProviderInfo("OPPO", "#1E90FF", true),
        ProviderInfo("realme", "#FFD700", false),
        ProviderInfo("vivo", "#4169E1", false),
        ProviderInfo("荣耀", "#32CD32", false),
    )

    val syncItems = listOf(
        SyncItem("预设同步", true, "2分钟前"),
        SyncItem("LUT 资源同步", true, "5分钟前"),
        SyncItem("设置同步", false, "从未同步"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("云同步", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E90FF).copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "同步状态",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E90FF)
                        )
                        Text(
                            text = "自动同步已开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Clock, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            Text(
                                text = "最后同步：$lastSyncTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                isSyncing.value = true
                                scope.launch {
                                    syncManager.performSync()
                                    kotlinx.coroutines.delay(2000)
                                    isSyncing.value = false
                                    lastSyncTime.value = "刚刚"
                                }
                            },
                            enabled = !isSyncing.value,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF))
                        ) {
                            if (isSyncing.value) {
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
                        icon = Icons.Default.Clock,
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
    val color: String,
    val connected: Boolean
)

data class SyncItem(
    val name: String,
    val enabled: Boolean,
    val lastSync: String
)

@Composable
private fun ProviderCard(provider: ProviderInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
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
                        color = if (provider.connected) Color.Green else Color.Gray
                    )
                }
            }
            if (provider.connected) {
                Icon(Icons.Default.Check, null, tint = Color.Green)
            } else {
                TextButton(onClick = {}) {
                    Text("连接", color = Color(0xFF1E90FF))
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
            .background(Color(0xFF1A1A1A))
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
                        .background(Color(0xFF1E90FF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Upload, null, tint = Color(0xFF1E90FF))
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
                        text = "最后同步：${item.lastSync}",
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
                    .background(if (item.enabled) Color(0xFF10B981) else Color.Gray),
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
    icon: androidx.compose.material.icons.Icon,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF10B981))
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
