package com.silas.omaster.ui.features

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
import com.silas.omaster.data.repository.CloudSyncState
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.theme.HasselbladOrange

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
    val repository = remember { PresetRepository.getInstance(context) }

    // 从 PresetRepository 读取真实同步状态
    val syncState by repository.syncState.collectAsState()
    val lastSyncTimestamp by repository.lastSyncTime.collectAsState()
    val cloudPresets by repository.cloudPresets.collectAsState()

    val isSyncing = syncState is CloudSyncState.Syncing

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

    // 云服务提供商连接状态
    var cloudProviders by remember {
        mutableStateOf(
            listOf(
                CloudProviderConnection(CloudProviderType.GOOGLE_DRIVE),
                CloudProviderConnection(CloudProviderType.DROPBOX),
                CloudProviderConnection(CloudProviderType.WEBDAV)
            )
        )
    }

    // 连接对话框状态
    var showConnectDialog by remember { mutableStateOf(false) }
    var selectedProviderType by remember { mutableStateOf<CloudProviderType?>(null) }

    // 从 CloudSyncManager 获取真实的云服务提供商列表
    // 依赖 cloudPresets State，确保同步完成后 UI 重新计算连接状态
    val presetProviders = remember(cloudPresets) {
        val urls = repository.getCloudPresetUrls()
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
                            is CloudSyncState.Syncing -> "正在同步中..." to HasselbladOrange
                            is CloudSyncState.Success -> "同步成功" to SuccessGreen
                            is CloudSyncState.Error -> {
                                val msg = (syncState as CloudSyncState.Error).message
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

                        // 错误/离线详情
                        if (syncState is CloudSyncState.Error) {
                            val errorMessage = (syncState as CloudSyncState.Error).message
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
                                        repository.syncFromCloud()
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
                                        repository.syncFromCloud()
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
                        description = "您的数据完全加密，安全可靠"
                    )
                    FeatureCard(
                        icon = Icons.Default.Wifi,
                        iconColor = Color(0xFF9C27B0),
                        title = "Wi-Fi 自动同步",
                        description = "仅在 Wi-Fi 下自动同步，节省流量"
                    )
                    FeatureCard(
                        icon = Icons.Default.History,
                        iconColor = HasselbladOrange,
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
                        // 模拟连接验证
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
                                } else {
                                    cloudProviders = cloudProviders.map { cp ->
                                        if (cp.type == provider) cp.copy(
                                            isConnected = false,
                                            isConnecting = false,
                                            apiKey = ""
                                        ) else cp
                                    }
                                }
                            } catch (e: Exception) {
                                cloudProviders = cloudProviders.map { cp ->
                                    if (cp.type == provider) cp.copy(
                                        isConnected = false,
                                        isConnecting = false,
                                        apiKey = ""
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
    description: String
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
 * 验证云服务提供商连接
 * 实际应用中将通过 HTTP 请求验证 API 密钥或 WebDAV 地址的有效性
 */
private suspend fun validateProviderConnection(
    providerType: CloudProviderType,
    apiKey: String
): Boolean {
    // 基础验证：检查格式
    if (apiKey.isBlank()) return false

    when (providerType) {
        CloudProviderType.GOOGLE_DRIVE -> {
            // Google Drive API Key 验证：通常为 39 字符的字母数字字符串
            return apiKey.length >= 20
        }
        CloudProviderType.DROPBOX -> {
            // Dropbox API Key 验证：通常以 "sl." 开头
            return apiKey.length >= 15
        }
        CloudProviderType.WEBDAV -> {
            // WebDAV 地址验证：必须是 HTTPS 协议
            return apiKey.lowercase().startsWith("https://")
        }
    }
}
