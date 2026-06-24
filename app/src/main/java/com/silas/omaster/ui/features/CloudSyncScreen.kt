package com.silas.omaster.ui.features

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.silas.omaster.cloud.CloudSyncManager
import com.silas.omaster.cloud.SyncState
import com.silas.omaster.ui.theme.HasselbladOrange

import com.silas.omaster.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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

    // 云服务提供商连接状态（从 SharedPreferences 持久化加载，应用重启后恢复）
    var cloudProviders by remember {
        mutableStateOf(loadCloudProviders(context))
    }

    // 连接对话框状态
    var showConnectDialog by remember { mutableStateOf(false) }
    var selectedProviderType by remember { mutableStateOf<CloudProviderType?>(null) }

    // 从 CloudSyncManager 获取真实的云服务提供商列表
    // 依赖 cloudPresets State，确保同步完成后 UI 重新计算连接状态
    val presetProviders = remember(cloudPresets) {
        val urls = syncManager.getCloudPresetUrls()
        val brandColors = mapOf(
            "oppo" to 0xFF1E90FF,
            "realme" to 0xFFFFD700,
            "vivo" to 0xFF4169E1,
            "honor" to 0xFF32CD32
        )
        urls.entries.mapIndexed { index, entry ->
            val brand = entry.key
            ProviderInfo(
                name = brand.replaceFirstChar { it.uppercase() },
                color = brandColors[brand.lowercase()] ?: (0xFF1E90FF + index * 0x002020),
                connected = cloudPresets.any { it.brand == brand },
                url = entry.value
            )
        }
    }

    // 同步内容项（基于真实同步状态）
    val syncItems = remember(lastSyncTimestamp, cloudPresets) {
        val presetCount = cloudPresets.size
        listOf(
            SyncItem("预设同步", presetCount > 0, lastSyncTimeText, presetCount),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
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
                        val (statusText, statusColor) = when (syncState) {
                            is SyncState.Syncing -> "正在同步中..." to HasselbladOrange
                            is SyncState.Success -> {
                                val s = syncState as SyncState.Success
                                "同步成功：新增 ${s.newCount} 个，更新 ${s.updatedCount} 个" to SuccessGreen
                            }
                            is SyncState.Error -> {
                                val msg = (syncState as SyncState.Error).message
                                if (msg.contains("网络", ignoreCase = true) || msg.contains("Network", ignoreCase = true) || msg.contains("Connect", ignoreCase = true)) {
                                    "同步失败：网络不可用" to Color(0xFFFFA726)
                                } else {
                                    "同步失败" to Color(0xFFE57373)
                                }
                            }
                            else -> if (lastSyncTimestamp > 0) "自动同步已开启" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                   else "尚未同步" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // 同步成功详情：提示已同步到本地主界面
                        if (syncState is SyncState.Success) {
                            val s = syncState as SyncState.Success
                            val total = s.newCount + s.updatedCount
                            Text(
                                text = "已同步 $total 个预设到本地",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // 错误/离线详情
                        if (syncState is SyncState.Error) {
                            val errorMessage = (syncState as SyncState.Error).message
                            if (errorMessage.isNotBlank()) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            Text(
                                text = "最后同步：$lastSyncTimeText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                    color = MaterialTheme.colorScheme.onBackground,
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
            if (presetProviders.isNotEmpty()) {
                item {
                    Text(
                        text = "云服务提供商",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetProviders.forEach { provider ->
                            ProviderCard(
                                provider = provider,
                                onConnect = {
                                    scope.launch {
                                        syncManager.sync()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 外部云存储服务
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "外部云存储",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cloudProviders.forEach { cp ->
                        CloudProviderCard(
                            provider = cp,
                            onConnect = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedProviderType = cp.type
                                showConnectDialog = true
                            },
                            onDisconnect = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                cloudProviders = cloudProviders.map {
                                    if (it.type == cp.type) it.copy(isConnected = false, apiKey = "")
                                    else it
                                }
                                // 持久化断开状态，清除已保存的 API Key
                                saveCloudProviderConnection(context, cp.type, isConnected = false, apiKey = "")
                            }
                        )
                    }
                }
            }

            // 同步内容
            item {
                Text(
                    text = "同步内容",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
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
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCard(
                        icon = Icons.Default.Shield,
                        iconColor = SuccessGreen,
                        title = "端到端加密",
                        description = "您的数据完全加密，安全可靠",
                        comingSoon = true
                    )
                    FeatureCard(
                        icon = Icons.Default.Wifi,
                        iconColor = Color(0xFF9C27B0),
                        title = "Wi-Fi 自动同步",
                        description = "仅在 Wi-Fi 下自动同步，节省流量",
                        comingSoon = true
                    )
                    FeatureCard(
                        icon = Icons.Default.History,
                        iconColor = HasselbladOrange,
                        title = "历史版本",
                        description = "保留 30 天历史版本，随时回退",
                        comingSoon = true
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 云存储连接对话框
    val selectedType = selectedProviderType
    if (showConnectDialog && selectedType != null) {
        CloudProviderConnectDialog(
            providerType = selectedType,
            provider = cloudProviders.find { it.type == selectedType },
            onDismiss = {
                showConnectDialog = false
                selectedProviderType = null
            },
            onConnect = { apiKey ->
                val provider = selectedType
                cloudProviders = cloudProviders.map {
                    if (it.type == provider) {
                        val updated = it.copy(isConnecting = true, apiKey = apiKey)
                        // 真实 HTTP 连接验证
                        scope.launch {
                            try {
                                // 验证 API Key / WebDAV 地址
                                val isValid = validateProviderConnection(provider, apiKey)
                                if (isValid) {
                                    cloudProviders = cloudProviders.map { cp ->
                                        if (cp.type == provider) cp.copy(
                                            isConnected = true,
                                            isConnecting = false,
                                            apiKey = apiKey
                                        ) else cp
                                    }
                                    // 持久化连接状态，应用重启后恢复
                                    saveCloudProviderConnection(context, provider, isConnected = true, apiKey = apiKey)
                                    // 连接验证通过后触发同步，将云端预设拉取到本地
                                    scope.launch {
                                        try {
                                            syncManager.sync()
                                        } catch (e: Exception) {
                                            android.util.Log.e("CloudSyncScreen", "连接后同步失败", e)
                                        }
                                    }
                                } else {
                                    // 验证失败：保留用户输入的 Key 以便修改后重试
                                    cloudProviders = cloudProviders.map { cp ->
                                        if (cp.type == provider) cp.copy(
                                            isConnected = false,
                                            isConnecting = false,
                                            apiKey = apiKey
                                        ) else cp
                                    }
                                }
                            } catch (e: Exception) {
                                // 异常：同样保留用户输入的 Key
                                cloudProviders = cloudProviders.map { cp ->
                                    if (cp.type == provider) cp.copy(
                                        isConnected = false,
                                        isConnecting = false,
                                        apiKey = apiKey
                                    ) else cp
                                }
                            }
                        }
                        updated
                    } else it
                }
                showConnectDialog = false
                selectedProviderType = null
            }
        )
    }
}

enum class CloudProviderType(val displayName: String, val icon: ImageVector, val color: Long) {
    GOOGLE_DRIVE("Google Drive", Icons.Default.Cloud, 0xFF4285F4),
    DROPBOX("Dropbox", Icons.Default.CloudUpload, 0xFF0061FF),
    WEBDAV("WebDAV", Icons.Default.Storage, 0xFF4CAF50)
}

private data class CloudProviderConnection(
    val type: CloudProviderType,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val apiKey: String = ""
)

private const val CLOUD_PROVIDER_PREFS = "cloud_provider_connections"

/**
 * 从 SharedPreferences 加载外部云存储提供商的连接状态
 * 应用重启后恢复连接状态
 */
private fun loadCloudProviders(context: Context): List<CloudProviderConnection> {
    val prefs = context.getSharedPreferences(CLOUD_PROVIDER_PREFS, Context.MODE_PRIVATE)
    return CloudProviderType.values().map { type ->
        val key = type.name.lowercase()
        CloudProviderConnection(
            type = type,
            isConnected = prefs.getBoolean("${key}_connected", false),
            apiKey = prefs.getString("${key}_api_key", "") ?: ""
        )
    }
}

/**
 * 持久化外部云存储提供商的连接状态到 SharedPreferences
 * 断开连接时清除已保存的 API Key
 */
private fun saveCloudProviderConnection(
    context: Context,
    type: CloudProviderType,
    isConnected: Boolean,
    apiKey: String
) {
    val prefs = context.getSharedPreferences(CLOUD_PROVIDER_PREFS, Context.MODE_PRIVATE)
    val key = type.name.lowercase()
    prefs.edit().apply {
        putBoolean("${key}_connected", isConnected)
        if (isConnected && apiKey.isNotEmpty()) {
            putString("${key}_api_key", apiKey)
        } else if (!isConnected) {
            remove("${key}_api_key")
        }
    }.apply()
}

data class ProviderInfo(
    val name: String,
    val color: Long,
    val connected: Boolean,
    val url: String = ""
)

data class SyncItem(
    val name: String,
    val enabled: Boolean,
    val lastSync: String,
    val count: Int = 0
)

@Composable
private fun ProviderCard(provider: ProviderInfo, onConnect: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${provider.name} Cloud",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (provider.connected) "已连接" else "未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (provider.connected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (provider.url.isNotEmpty()) {
                        Text(
                            text = provider.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            maxLines = 1
                        )
                    }
                }
            }
            if (provider.connected) {
                Icon(Icons.Default.Check, null, tint = SuccessGreen)
            } else {
                TextButton(onClick = onConnect) {
                    Text("同步", color = HasselbladOrange)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (item.count > 0) "最后同步：${item.lastSync} (${item.count}条)" else "最后同步：${item.lastSync}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (item.enabled) SuccessGreen else MaterialTheme.colorScheme.outline),
            )
        }
    }
}

@Composable
private fun CloudProviderCard(
    provider: CloudProviderConnection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        .background(Color(provider.type.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(provider.type.icon, null, tint = Color(provider.type.color))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = provider.type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (provider.isConnected) "已连接" else if (provider.isConnecting) "连接中..." else "未连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            provider.isConnected -> SuccessGreen
                            provider.isConnecting -> HasselbladOrange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            if (provider.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp
                )
            } else if (provider.isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDisconnect) {
                        Text("断开", color = Color(0xFFE57373))
                    }
                }
            } else {
                TextButton(onClick = onConnect) {
                    Text("连接", color = HasselbladOrange)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    comingSoon: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (comingSoon) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HasselbladOrange.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "即将推出",
                                style = MaterialTheme.typography.labelSmall,
                                color = HasselbladOrange
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CloudProviderConnectDialog(
    providerType: CloudProviderType,
    provider: CloudProviderConnection?,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    val isWebDAV = providerType == CloudProviderType.WEBDAV

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    providerType.icon,
                    null,
                    tint = Color(providerType.color),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "连接 ${providerType.displayName}",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        text = {
            Column {
                Text(
                    text = if (isWebDAV) {
                        "请输入 WebDAV 服务器地址和凭据"
                    } else {
                        "请输入 ${providerType.displayName} API 密钥以完成连接"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = {
                        Text(
                            if (isWebDAV) "WebDAV 地址" else "API Key",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    },
                    placeholder = {
                        Text(
                            if (isWebDAV) "https://dav.example.com" else "输入您的 API 密钥...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isWebDAV) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isWebDAV) KeyboardType.Uri else KeyboardType.Password
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = Color(providerType.color),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        cursorColor = Color(providerType.color)
                    )
                )
                if (isWebDAV) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持 HTTPS 协议的 WebDAV 服务器，如 NextCloud、ownCloud 等",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(apiKey) },
                enabled = apiKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(providerType.color))
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
    )
}

/**
 * 验证云服务提供商连接（真实 HTTP 请求验证）
 *
 * 对每种提供商发起真实的网络探测：
 * - Google Drive: 调用 about API 验证 token 有效性
 * - Dropbox: 调用 /2/users/get_current_account 验证 token
 * - WebDAV: 发送 PROPFIND 请求验证地址可达性
 */
private suspend fun validateProviderConnection(
    providerType: CloudProviderType,
    apiKey: String
): Boolean = withContext(Dispatchers.IO) {
    // 基础验证：检查格式
    if (apiKey.isBlank()) return@withContext false

    var conn: HttpURLConnection? = null
    try {
        when (providerType) {
            CloudProviderType.GOOGLE_DRIVE -> {
                // Google Drive: 使用 about API 验证 OAuth token
                if (apiKey.length < 20) return@withContext false
                val url = URL("https://www.googleapis.com/drive/v3/about?fields=user")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                conn.responseCode == 200
            }
            CloudProviderType.DROPBOX -> {
                // Dropbox: 调用 get_current_account 验证 token
                if (apiKey.length < 15) return@withContext false
                val url = URL("https://api.dropboxapi.com/2/users/get_current_account")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                conn.outputStream.use { it.write("{}".toByteArray()) }
                conn.responseCode == 200
            }
            CloudProviderType.WEBDAV -> {
                // WebDAV: 发送 PROPFIND 请求验证地址可达性
                if (!apiKey.lowercase().startsWith("https://")) return@withContext false
                val url = URL(apiKey.trimEnd('/'))
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PROPFIND"
                    setRequestProperty("Depth", "0")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                // 207 Multi-Status 或 200 OK 均表示地址可达
                val code = conn.responseCode
                code == 207 || code == 200
            }
        }
    } catch (e: Exception) {
        // 网络异常视为验证失败（含 SSLHandshakeException、UnknownHostException、ConnectException 等）
        false
    } finally {
        // 确保连接资源被释放，避免 Socket 泄漏
        try { conn?.disconnect() } catch (_: Exception) { /* 忽略关闭异常 */ }
        try { conn?.inputStream?.close() } catch (_: Exception) { /* 忽略 */ }
        try { conn?.errorStream?.close() } catch (_: Exception) { /* 忽略 */ }
    }
}
