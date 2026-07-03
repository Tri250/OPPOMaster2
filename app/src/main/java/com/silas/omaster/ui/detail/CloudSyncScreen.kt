package com.silas.omaster.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.cloud.CloudProvider
import com.silas.omaster.cloud.CloudSyncManager
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 云端同步设置页面
 *
 * 功能：
 * - 服务提供者选择（WebDAV / Google Drive）
 * - 连接配置表单
 * - 连接测试
 * - 同步状态显示
 * - 手动同步触发
 * - 自动同步开关
 *
 * 风格：PureBlack #0A0A0A 背景 + HasselbladOrange 强调色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncManager = remember { CloudSyncManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val syncStatus by syncManager.syncStatus.collectAsState()
    val isConnected by syncManager.isConnected.collectAsState()
    val autoSyncEnabled by syncManager.autoSyncEnabled.collectAsState()
    val currentProvider by syncManager.currentProvider.collectAsState()
    val lastSyncTime by syncManager.lastSyncTime.collectAsState()

    // 表单状态
    var selectedProviderType by remember {
        mutableStateOf(
            when (currentProvider) {
                is CloudProvider.GoogleDrive -> CloudProvider.GoogleDrive.TYPE_GDRIVE
                else -> CloudProvider.WebDAV.TYPE_WEBDAV
            }
        )
    }

    // WebDAV 表单
    var serverUrl by remember {
        mutableStateOf(
            (currentProvider as? CloudProvider.WebDAV)?.serverUrl ?: ""
        )
    }
    var username by remember {
        mutableStateOf(
            (currentProvider as? CloudProvider.WebDAV)?.username ?: ""
        )
    }
    var password by remember {
        mutableStateOf(
            (currentProvider as? CloudProvider.WebDAV)?.password ?: ""
        )
    }

    // Google Drive 表单
    var accessToken by remember {
        mutableStateOf(
            (currentProvider as? CloudProvider.GoogleDrive)?.accessToken ?: ""
        )
    }
    var folderId by remember {
        mutableStateOf(
            (currentProvider as? CloudProvider.GoogleDrive)?.folderId ?: "root"
        )
    }

    // 连接测试状态
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Boolean?>(null) }

    // 切换提供者时同步 selectedProviderType
    LaunchedEffect(currentProvider) {
        if (currentProvider != null) {
            selectedProviderType = when (currentProvider) {
                is CloudProvider.GoogleDrive -> CloudProvider.GoogleDrive.TYPE_GDRIVE
                else -> CloudProvider.WebDAV.TYPE_WEBDAV
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.cloud_sync_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack
            ),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==================== 同步状态卡片 ====================
            SyncStatusCard(
                syncStatus = syncStatus,
                isConnected = isConnected,
                lastSyncTime = lastSyncTime
            )

            // ==================== 提供者选择 ====================
            ProviderSelectionSection(
                selectedType = selectedProviderType,
                onTypeSelected = { selectedProviderType = it }
            )

            // ==================== 配置表单 ====================
            AnimatedVisibility(
                visible = selectedProviderType == CloudProvider.WebDAV.TYPE_WEBDAV,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WebDAVConfigForm(
                    serverUrl = serverUrl,
                    onServerUrlChange = { serverUrl = it },
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it }
                )
            }

            AnimatedVisibility(
                visible = selectedProviderType == CloudProvider.GoogleDrive.TYPE_GDRIVE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GoogleDriveConfigForm(
                    accessToken = accessToken,
                    onAccessTokenChange = { accessToken = it },
                    folderId = folderId,
                    onFolderIdChange = { folderId = it }
                )
            }

            // ==================== 操作按钮 ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 连接 / 断开
                if (isConnected) {
                    Button(
                        onClick = {
                            syncManager.disconnect()
                            connectionTestResult = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f),
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cloud_disconnect))
                    }
                } else {
                    Button(
                        onClick = {
                            when (selectedProviderType) {
                                CloudProvider.WebDAV.TYPE_WEBDAV -> {
                                    if (serverUrl.isNotBlank() && username.isNotBlank()) {
                                        syncManager.configureWebDAV(serverUrl, username, password)
                                    }
                                }
                                CloudProvider.GoogleDrive.TYPE_GDRIVE -> {
                                    if (accessToken.isNotBlank()) {
                                        syncManager.configureGoogleDrive(accessToken, folderId)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = when (selectedProviderType) {
                            CloudProvider.WebDAV.TYPE_WEBDAV -> serverUrl.isNotBlank() && username.isNotBlank()
                            CloudProvider.GoogleDrive.TYPE_GDRIVE -> accessToken.isNotBlank()
                            else -> false
                        }
                    ) {
                        Text(stringResource(R.string.cloud_connect))
                    }
                }

                // 测试连接
                OutlinedTestButton(
                    isTesting = isTestingConnection,
                    testResult = connectionTestResult,
                    enabled = isConnected || (selectedProviderType == CloudProvider.WebDAV.TYPE_WEBDAV && serverUrl.isNotBlank()),
                    onClick = {
                        coroutineScope.launch {
                            isTestingConnection = true
                            connectionTestResult = null
                            // 先配置再测试
                            when (selectedProviderType) {
                                CloudProvider.WebDAV.TYPE_WEBDAV -> {
                                    if (serverUrl.isNotBlank() && username.isNotBlank()) {
                                        syncManager.configureWebDAV(serverUrl, username, password)
                                    }
                                }
                                CloudProvider.GoogleDrive.TYPE_GDRIVE -> {
                                    if (accessToken.isNotBlank()) {
                                        syncManager.configureGoogleDrive(accessToken, folderId)
                                    }
                                }
                            }
                            connectionTestResult = syncManager.testConnection()
                            isTestingConnection = false
                        }
                    }
                )
            }

            // ==================== 自动同步开关 ====================
            AutoSyncSection(
                enabled = autoSyncEnabled,
                isConnected = isConnected,
                onToggle = { syncManager.setAutoSync(it) }
            )

            // ==================== 手动同步按钮 ====================
            AnimatedVisibility(visible = isConnected) {
                Button(
                    onClick = { syncManager.syncNow() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HasselbladOrange.copy(alpha = 0.15f),
                        contentColor = HasselbladOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = syncStatus !is CloudSyncManager.SyncStatus.Syncing
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (syncStatus is CloudSyncManager.SyncStatus.Syncing)
                            stringResource(R.string.cloud_syncing)
                        else
                            stringResource(R.string.cloud_sync_title)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== 子组件 ====================

@Composable
private fun SyncStatusCard(
    syncStatus: CloudSyncManager.SyncStatus,
    isConnected: Boolean,
    lastSyncTime: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态图标
            when (syncStatus) {
                is CloudSyncManager.SyncStatus.Syncing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = HasselbladOrange,
                        strokeWidth = 3.dp
                    )
                }
                is CloudSyncManager.SyncStatus.Success -> {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(32.dp)
                    )
                }
                is CloudSyncManager.SyncStatus.Error -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (syncStatus) {
                        is CloudSyncManager.SyncStatus.Idle ->
                            if (isConnected) stringResource(R.string.cloud_connected)
                            else stringResource(R.string.cloud_not_configured)
                        is CloudSyncManager.SyncStatus.Syncing -> stringResource(R.string.cloud_syncing)
                        is CloudSyncManager.SyncStatus.Success -> stringResource(R.string.cloud_sync_success)
                        is CloudSyncManager.SyncStatus.Error -> stringResource(R.string.cloud_sync_failed)
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                if (lastSyncTime > 0) {
                    val formattedTime = remember(lastSyncTime) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(lastSyncTime))
                    }
                    Text(
                        text = stringResource(R.string.cloud_last_sync) + ": $formattedTime",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                if (syncStatus is CloudSyncManager.SyncStatus.Error) {
                    Text(
                        text = syncStatus.message,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSelectionSection(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Text(
        text = "服务提供者",
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = selectedType == CloudProvider.WebDAV.TYPE_WEBDAV,
            onClick = { onTypeSelected(CloudProvider.WebDAV.TYPE_WEBDAV) },
            label = { Text(stringResource(R.string.cloud_provider_webdav)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                selectedLabelColor = HasselbladOrange
            ),
            modifier = Modifier.weight(1f)
        )

        FilterChip(
            selected = selectedType == CloudProvider.GoogleDrive.TYPE_GDRIVE,
            onClick = { onTypeSelected(CloudProvider.GoogleDrive.TYPE_GDRIVE) },
            label = { Text(stringResource(R.string.cloud_provider_gdrive)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                selectedLabelColor = HasselbladOrange
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WebDAVConfigForm(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "WebDAV 配置",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                label = { Text(stringResource(R.string.cloud_server_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.cloud_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.cloud_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun GoogleDriveConfigForm(
    accessToken: String,
    onAccessTokenChange: (String) -> Unit,
    folderId: String,
    onFolderIdChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Google Drive 配置",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )

            OutlinedTextField(
                value = accessToken,
                onValueChange = onAccessTokenChange,
                label = { Text("Access Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = folderId,
                onValueChange = onFolderIdChange,
                label = { Text("Folder ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = HasselbladOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun OutlinedTestButton(
    isTesting: Boolean,
    testResult: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                testResult == true -> Color.Green.copy(alpha = 0.15f)
                testResult == false -> Color.Red.copy(alpha = 0.15f)
                else -> Color.White.copy(alpha = 0.08f)
            },
            contentColor = when {
                testResult == true -> Color.Green
                testResult == false -> Color.Red
                else -> Color.White
            },
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled && !isTesting
    ) {
        if (isTesting) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (testResult == true) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (testResult == false) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(stringResource(R.string.cloud_test_connection))
    }
}

@Composable
private fun AutoSyncSection(
    enabled: Boolean,
    isConnected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.cloud_auto_sync),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = "预设变更后自动同步到云端",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = HasselbladOrange,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f)
                ),
                enabled = isConnected
            )
        }
    }
}
