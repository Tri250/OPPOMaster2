package com.silas.omaster.ui.settings

import android.content.Context
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
import com.silas.omaster.data.model.PresetSource
import com.silas.omaster.model.PresetList
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.util.UrlConstants
import com.silas.omaster.util.perform
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSourceManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var sources by remember { mutableStateOf(loadSources(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<PresetSource?>(null) }
    var fetchedPresetCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 加载预设数量
    fun refreshCounts() {
        scope.launch {
            errorMessage = null
            if (!sources.any { it.enabled }) {
                fetchedPresetCount = 0
                return@launch
            }
            isLoading = true
            var count = 0
            val errors = mutableListOf<String>()
            sources.filter { it.enabled }.forEach { source ->
                try {
                    val response = withContext(Dispatchers.IO) {
                        java.net.URL(source.url).openConnection().apply {
                            connectTimeout = 15_000
                            readTimeout = 15_000
                            setRequestProperty("Accept", "application/json")
                        }.getInputStream().bufferedReader().readText()
                    }
                    if (response.isBlank()) {
                        errors.add("${source.name}: 返回空数据")
                        return@forEach
                    }
                    val presetList = Json.decodeFromString<PresetList>(response)
                    count += presetList.presets.size
                } catch (e: Exception) {
                    val msg = e.message ?: "未知错误"
                    errors.add("${source.name}: $msg")
                }
            }
            fetchedPresetCount = count
            if (errors.isNotEmpty() && count == 0) {
                errorMessage = errors.joinToString("\n")
            } else if (errors.isNotEmpty()) {
                errorMessage = "部分源加载失败\n${errors.joinToString("\n")}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(sources) {
        refreshCounts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("预设源管理", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            actions = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    refreshCounts()
                }) {
                    Icon(
                        Icons.Default.Refresh, "刷新",
                        tint = if (isLoading) HasselbladOrange else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        text = "预设源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLoading) "..." else "$fetchedPresetCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "已加载预设",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 错误提示
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "错误",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { errorMessage = null }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                        haptic.perform(HapticFeedbackType.LongPress)
                        sources = sources.map {
                            if (it.id == source.id) it.copy(enabled = enabled) else it
                        }
                        saveSources(context, sources)
                    },
                    onEdit = {
                        editingSource = source
                    },
                    onSave = { name, url ->
                        sources = sources.map {
                            if (it.id == source.id) it.copy(name = name, url = url) else it
                        }
                        editingSource = null
                        saveSources(context, sources)
                    },
                    onCancel = {
                        editingSource = null
                    },
                    onDelete = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        sources = sources.filter { it.id != source.id }
                        saveSources(context, sources)
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
                sources = sources + PresetSource(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    url = url,
                    enabled = true
                )
                saveSources(context, sources)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.outline,
        title = {
            Text("添加预设源", color = MaterialTheme.colorScheme.onBackground)
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
                    onValueChange = { url = it },
                    label = { Text("URL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, url) },
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

private const val PREFS_SOURCES = "preset_source_manager"
private const val KEY_SOURCES = "sources"

private fun getDefaultSources(): List<PresetSource> = listOf(
    PresetSource(
        id = "oppo",
        name = "OPPO 预设库",
        url = UrlConstants.PRESET_OPPO,
        enabled = true
    ),
    PresetSource(
        id = "realme",
        name = "realme 预设库",
        url = UrlConstants.PRESET_REALME,
        enabled = true
    ),
    PresetSource(
        id = "vivo",
        name = "vivo 预设库",
        url = UrlConstants.PRESET_VIVO,
        enabled = true
    ),
    PresetSource(
        id = "honor",
        name = "荣耀 预设库",
        url = UrlConstants.PRESET_HONOR,
        enabled = true
    )
)

private fun loadSources(context: Context): List<PresetSource> {
    return try {
        val prefs = context.getSharedPreferences(PREFS_SOURCES, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SOURCES, null) ?: return getDefaultSources()
        Json.decodeFromString(json)
    } catch (e: Exception) {
        getDefaultSources()
    }
}

private fun saveSources(context: Context, sources: List<PresetSource>) {
    try {
        val prefs = context.getSharedPreferences(PREFS_SOURCES, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SOURCES, Json.encodeToString(sources)).apply()
    } catch (e: Exception) {
        android.util.Log.e("PresetSourceManager", "保存预设源失败", e)
    }
}
