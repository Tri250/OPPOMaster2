package com.silas.omaster.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.param.*
import kotlinx.coroutines.launch

/**
 * 选择性粘贴屏幕
 * 支持勾选只粘贴特定参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectivePasteScreen(
    selectivePasteManager: SelectivePasteManager,
    onDismiss: () -> Unit = {}
) {
    val selectedParams by selectivePasteManager.selectedParams.collectAsState()
    val clipboard by selectivePasteManager.clipboard.collectAsState()
    val clipboardSource by selectivePasteManager.clipboardSource.collectAsState()

    var showPasteResult by remember { mutableStateOf<PasteResult?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择性粘贴") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    // 全选/取消全选
                    IconButton(onClick = {
                        selectivePasteManager.toggleSelectAll(selectedParams.size < selectivePasteManager.pasteableParams.size)
                    }) {
                        Icon(
                            if (selectedParams.size == selectivePasteManager.pasteableParams.size)
                                Icons.Default.CheckBox
                            else
                                Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "全选"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 剪贴板状态卡片
            ClipboardStatusCard(
                clipboard = clipboard,
                source = clipboardSource,
                hasData = selectivePasteManager.hasClipboardData()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 快速选择预设
            SelectionPresetSection(
                presets = SelectionPreset.PRESETS,
                onPresetSelected = { preset ->
                    selectivePasteManager.selectPreset(preset)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 参数选择列表
            Text(
                text = "选择要粘贴的参数",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectivePasteManager.pasteableParams) { param ->
                    ParamSelectionItem(
                        param = param,
                        isSelected = selectedParams.contains(param.name),
                        clipboardValue = clipboard[param.name],
                        onToggle = { selectivePasteManager.toggleParamSelection(param.name) }
                    )
                }
            }

            // 底部操作栏
            BottomActionBar(
                selectedCount = selectedParams.size,
                pasteableCount = selectivePasteManager.getPasteableCount(),
                hasClipboard = selectivePasteManager.hasClipboardData(),
                onPasteSelected = {
                    val result = selectivePasteManager.pasteSelected()
                    showPasteResult = result
                },
                onPasteAll = {
                    val result = selectivePasteManager.pasteAll()
                    showPasteResult = result
                },
                onCopy = {
                    selectivePasteManager.copyParams()
                }
            )
        }
    }

    // 粘贴结果提示
    showPasteResult?.let { result ->
        PasteResultDialog(
            result = result,
            onDismiss = { showPasteResult = null }
        )
    }
}

/**
 * 剪贴板状态卡片
 */
@Composable
private fun ClipboardStatusCard(
    clipboard: Map<String, Int>,
    source: ClipboardSource?,
    hasData: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasData)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasData) Icons.Default.ContentPaste else Icons.Default.ContentPasteOff,
                    contentDescription = null,
                    tint = if (hasData)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasData) "剪贴板有数据" else "剪贴板为空",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (hasData && source != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    source.presetName?.let { name ->
                        Text(
                            text = "来源: $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    source.deviceName?.let { device ->
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = device,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (hasData) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "共 ${clipboard.size} 个参数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 快速选择预设区域
 */
@Composable
private fun SelectionPresetSection(
    presets: List<SelectionPreset>,
    onPresetSelected: (SelectionPreset) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "快速选择",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.name) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * 参数选择项
 */
@Composable
private fun ParamSelectionItem(
    param: PasteableParam,
    isSelected: Boolean,
    clipboardValue: Int?,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 参数名称
            Text(
                text = param.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // 剪贴板值
            if (clipboardValue != null) {
                Surface(
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (clipboardValue >= 0) "+$clipboardValue" else clipboardValue.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 底部操作栏
 */
@Composable
private fun BottomActionBar(
    selectedCount: Int,
    pasteableCount: Int,
    hasClipboard: Boolean,
    onPasteSelected: () -> Unit,
    onPasteAll: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 复制当前参数
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("复制")
            }

            // 粘贴选中
            Button(
                onClick = onPasteSelected,
                enabled = hasClipboard && selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("粘贴($selectedCount)")
            }

            // 粘贴全部
            FilledTonalButton(
                onClick = onPasteAll,
                enabled = hasClipboard,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SelectAll, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("全部")
            }
        }
    }
}

/**
 * 粘贴结果对话框
 */
@Composable
private fun PasteResultDialog(
    result: PasteResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (result) {
                    is PasteResult.Success -> "粘贴成功"
                    is PasteResult.Error -> "粘贴失败"
                }
            )
        },
        text = {
            when (result) {
                is PasteResult.Success -> {
                    Column {
                        Text("已粘贴 ${result.pastedCount} 个参数")
                        if (result.skippedCount > 0) {
                            Text("跳过 ${result.skippedCount} 个参数")
                        }
                    }
                }
                is PasteResult.Error -> {
                    Text(result.message)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 参数同步屏幕
 * 与 CloudSyncManager 集成，实现跨设备参数同步
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamSyncScreen(
    paramSyncService: com.silas.omaster.cloud.ParamSyncService,
    onDismiss: () -> Unit = {}
) {
    val syncState by paramSyncService.syncState.collectAsState()
    val syncedParams by paramSyncService.syncedParams.collectAsState()
    val syncHistory by paramSyncService.syncHistory.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var shareLink by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("跨设备同步") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        shareLink = paramSyncService.exportAsShareLink()
                        showShareDialog = true
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 设备信息卡片
            DeviceInfoCard(
                deviceInfo = paramSyncService.getDeviceInfo()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 同步状态卡片
            SyncStatusCard(
                syncState = syncState,
                syncedParams = syncedParams
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 同步操作按钮
            SyncActionButtons(
                syncState = syncState,
                onUpload = {
                    scope.launch {
                        paramSyncService.uploadParams()
                    }
                },
                onDownload = {
                    scope.launch {
                        paramSyncService.downloadParams()
                    }
                },
                onSync = {
                    scope.launch {
                        paramSyncService.syncParams()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 同步历史
            Text(
                text = "同步历史",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(syncHistory) { item ->
                    SyncHistoryItem(item = item)
                }
            }
        }
    }

    // 分享对话框
    if (showShareDialog) {
        ShareLinkDialog(
            link = shareLink,
            onDismiss = { showShareDialog = false }
        )
    }
}

/**
 * 设备信息卡片
 */
@Composable
private fun DeviceInfoCard(
    deviceInfo: com.silas.omaster.cloud.DeviceInfo
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = deviceInfo.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "设备ID: ${deviceInfo.deviceId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 同步状态卡片
 */
@Composable
private fun SyncStatusCard(
    syncState: com.silas.omaster.cloud.ParamSyncState,
    syncedParams: com.silas.omaster.cloud.SyncedParamSnapshot?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (syncState) {
                is com.silas.omaster.cloud.ParamSyncState.Synced -> MaterialTheme.colorScheme.primaryContainer
                is com.silas.omaster.cloud.ParamSyncState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (syncState) {
                    is com.silas.omaster.cloud.ParamSyncState.Idle -> {
                        Icon(Icons.Default.CloudOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("未同步")
                    }
                    is com.silas.omaster.cloud.ParamSyncState.Uploading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在上传...")
                    }
                    is com.silas.omaster.cloud.ParamSyncState.Downloading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在下载...")
                    }
                    is com.silas.omaster.cloud.ParamSyncState.Syncing -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在同步...")
                    }
                    is com.silas.omaster.cloud.ParamSyncState.Synced -> {
                        Icon(Icons.Default.CloudDone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("已同步")
                    }
                    is com.silas.omaster.cloud.ParamSyncState.Error -> {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步失败")
                    }
                }
            }

            if (syncedParams != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "预设: ${syncedParams.presetName}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "参数数量: ${syncedParams.params.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 同步操作按钮
 */
@Composable
private fun SyncActionButtons(
    syncState: com.silas.omaster.cloud.ParamSyncState,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onSync: () -> Unit
) {
    val isLoading = syncState is com.silas.omaster.cloud.ParamSyncState.Uploading ||
            syncState is com.silas.omaster.cloud.ParamSyncState.Downloading ||
            syncState is com.silas.omaster.cloud.ParamSyncState.Syncing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 上传
        OutlinedButton(
            onClick = onUpload,
            enabled = !isLoading,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("上传")
        }

        // 下载
        OutlinedButton(
            onClick = onDownload,
            enabled = !isLoading,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("下载")
        }

        // 同步
        Button(
            onClick = onSync,
            enabled = !isLoading,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("同步")
        }
    }
}

/**
 * 同步历史项
 */
@Composable
private fun SyncHistoryItem(
    item: com.silas.omaster.cloud.SyncHistoryItem
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = when (item.action) {
                    com.silas.omaster.cloud.SyncAction.UPLOAD -> Icons.Default.CloudUpload
                    com.silas.omaster.cloud.SyncAction.DOWNLOAD -> Icons.Default.CloudDownload
                    com.silas.omaster.cloud.SyncAction.DELETE -> Icons.Default.Delete
                    com.silas.omaster.cloud.SyncAction.SYNC -> Icons.Default.Sync
                },
                contentDescription = null,
                tint = if (item.success)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (item.action) {
                        com.silas.omaster.cloud.SyncAction.UPLOAD -> "上传"
                        com.silas.omaster.cloud.SyncAction.DOWNLOAD -> "下载"
                        com.silas.omaster.cloud.SyncAction.DELETE -> "删除"
                        com.silas.omaster.cloud.SyncAction.SYNC -> "同步"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                        .format(java.util.Date(item.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态指示
            Surface(
                color = if (item.success)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (item.success) "成功" else "失败",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 分享链接对话框
 */
@Composable
private fun ShareLinkDialog(
    link: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享参数") },
        text = {
            Column {
                Text("复制以下链接分享给其他设备：")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = link.take(100) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 参数调节卡片组件
 * 用于展示和调节单个参数
 */
@Composable
fun ParamAdjustmentCard(
    name: String,
    displayName: String,
    value: Int,
    minValue: Int = -100,
    maxValue: Int = 100,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 数值显示
                    Text(
                        text = if (value >= 0) "+$value" else value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 重置按钮
                    IconButton(
                        onClick = onReset,
                        enabled = value != 0
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = if (value != 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 滑块
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = minValue.toFloat()..maxValue.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            // 快捷按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-50, -25, 0, 25, 50).forEach { preset ->
                    FilterChip(
                        selected = value == preset,
                        onClick = { onValueChange(preset) },
                        label = {
                            Text(
                                text = if (preset >= 0) "+$preset" else preset.toString(),
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 功能卡片组件
 * 用于展示功能入口
 */
@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    badge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 箭头
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 参数组卡片
 * 用于展示一组相关参数
 */
@Composable
fun ParamGroupCard(
    title: String,
    icon: ImageVector,
    params: List<ParamItem>,
    onParamChange: (String, Int) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onExpandChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // 重置全部按钮
                TextButton(onClick = onResetAll) {
                    Text("重置")
                }

                // 展开/收起图标
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // 参数列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    params.forEach { param ->
                        ParamSliderItem(
                            param = param,
                            onValueChange = { onParamChange(param.name, it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 参数滑块项
 */
@Composable
private fun ParamSliderItem(
    param: ParamItem,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = param.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )

        Slider(
            value = param.value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = param.minValue.toFloat()..param.maxValue.toFloat(),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (param.value >= 0) "+${param.value}" else param.value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
            color = if (param.value != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 参数项数据类
 */
data class ParamItem(
    val name: String,
    val displayName: String,
    val value: Int,
    val minValue: Int = -100,
    val maxValue: Int = 100
)
