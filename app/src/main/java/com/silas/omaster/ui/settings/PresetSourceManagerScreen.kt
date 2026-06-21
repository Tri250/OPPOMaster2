package com.silas.omaster.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silas.omaster.data.local.SubscriptionManager
import com.silas.omaster.data.model.PresetSource
import com.silas.omaster.model.Subscription
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSourceManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val subscriptionManager = remember { SubscriptionManager.getInstance(context) }

    val subscriptions by subscriptionManager.subscriptionsFlow.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<PresetSource?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    // 将 Subscription 转换为 PresetSource 用于 UI 展示
    val sources = remember(subscriptions) {
        subscriptions.map { sub ->
            PresetSource(
                id = sub.url,
                name = sub.name,
                url = sub.url,
                enabled = sub.isEnabled,
                lastUpdated = sub.lastUpdateTime
            )
        }
    }

    val fetchedPresetCount = remember(subscriptions) {
        subscriptions.filter { it.isEnabled }.sumOf { it.presetCount.toInt() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("订阅管理", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // 刷新：重新加载订阅状态
                    isLoading = true
                    syncError = null
                    scope.launch {
                        try {
                            // 触发订阅更新逻辑（如果有网络拉取需求可在此扩展）
                            isLoading = false
                        } catch (e: Exception) {
                            syncError = e.message ?: "刷新失败"
                            isLoading = false
                        }
                    }
                }) {
                    Icon(
                        Icons.Default.Refresh, "刷新",
                        tint = if (isLoading) HasselbladOrange else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, "添加", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // 统计信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${sources.count { it.enabled }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange
                        )
                        Text(
                            text = "已启用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${sources.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "订阅源",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isLoading) "..." else "$fetchedPresetCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (syncError != null) ErrorRed else SuccessGreen
                        )
                        Text(
                            text = if (syncError != null) "加载失败" else "已加载预设",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncError != null) ErrorRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                if (syncError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 预设源列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sources, key = { it.id }) { source ->
                PresetSourceCard(
                    source = source,
                    isEditing = editingSource?.id == source.id,
                    onToggle = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        subscriptionManager.toggleSubscription(source.url)
                    },
                    onEdit = {
                        editingSource = source
                    },
                    onSave = { name, url ->
                        // 如果 URL 改变，先移除旧订阅再添加新订阅
                        val oldUrl = editingSource?.url ?: ""
                        if (oldUrl.isNotBlank() && oldUrl != url) {
                            subscriptionManager.removeSubscription(oldUrl)
                            subscriptionManager.addSubscription(url, name)
                        } else if (oldUrl.isNotBlank()) {
                            // 仅名称改变：通过更新状态实现（SubscriptionManager 暂无直接改名 API，采用 remove+add 模拟）
                            subscriptionManager.removeSubscription(oldUrl)
                            subscriptionManager.addSubscription(url, name)
                        }
                        editingSource = null
                    },
                    onCancel = {
                        editingSource = null
                    },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        subscriptionManager.removeSubscription(source.url)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, url ->
                subscriptionManager.addSubscription(url, name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PresetSourceCard(
    source: PresetSource,
    isEditing: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var editName by remember(source) { mutableStateOf(source.name) }
    var editUrl by remember(source) { mutableStateOf(source.url) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                // 编辑模式
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("名称", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        cursorColor = HasselbladOrange
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text("URL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        cursorColor = HasselbladOrange
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("取消", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(editName, editUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Text("保存")
                    }
                }
            } else {
                // 显示模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = source.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }

                    // 开关
                    Switch(
                        checked = source.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit, "编辑",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete, "删除",
                            tint = ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.outline,
        title = {
            Text("添加订阅源", color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    label = { Text("URL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(error!!, color = ErrorRed)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!url.lowercase().startsWith("https://")) {
                        error = "仅支持 HTTPS 链接"
                        return@Button
                    }
                    onAdd(name, url)
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
    )
}