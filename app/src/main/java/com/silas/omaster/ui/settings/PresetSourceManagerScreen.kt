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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.silas.omaster.data.model.PresetSource
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.DividerColor
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.MediumGray
import com.silas.omaster.ui.theme.OnSurfaceDisabled
import com.silas.omaster.ui.theme.OnSurfaceInverse
import com.silas.omaster.ui.theme.OnSurfacePrimary
import com.silas.omaster.ui.theme.OnSurfaceSecondary
import com.silas.omaster.ui.theme.OnSurfaceTertiary
import com.silas.omaster.ui.theme.OutlineVariant
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.util.perform
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSourceManagerScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var sources by remember { mutableStateOf(getDefaultSources()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<PresetSource?>(null) }
    var fetchedPresetCount by remember { mutableStateOf(0) }
    
    // 加载预设数量
    LaunchedEffect(sources) {
        if (sources.any { it.enabled }) {
            isLoading = true
            var count = 0
            sources.filter { it.enabled }.forEach { source ->
                try {
                    val response = withContext(Dispatchers.IO) {
                        java.net.URL(source.url).openStream().bufferedReader().readText()
                    }
                    val data = Json.decodeFromString<Map<String, Any>>(response)
                    val presets = data["presets"]
                    if (presets is List<*>) {
                        count += presets.size
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            fetchedPresetCount = count
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
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
                    // 刷新
                    scope.launch {
                        isLoading = true
                        delay(1000)
                        isLoading = false
                    }
                }) {
                    Icon(
                        Icons.Default.Refresh, "刷新",
                        tint = if (isLoading) HasselbladOrange else OnSurfacePrimary
                    )
                }
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, "添加", tint = OnSurfacePrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = OnSurfacePrimary
            )
        )
        
        // 统计信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
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
                        color = OnSurfaceSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${sources.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfacePrimary
                    )
                    Text(
                        text = "预设源",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceSecondary
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
                        color = OnSurfaceSecondary
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
                        haptic.perform(HapticFeedbackType.LongPress)
                        sources = sources.map {
                            if (it.id == source.id) it.copy(enabled = enabled) else it
                        }
                    },
                    onEdit = {
                        editingSource = source
                    },
                    onSave = { name, url ->
                        sources = sources.map {
                            if (it.id == source.id) it.copy(name = name, url = url) else it
                        }
                        editingSource = null
                    },
                    onCancel = {
                        editingSource = null
                    },
                    onDelete = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        sources = sources.filter { it.id != source.id }
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
                    label = { Text("名称", color = OnSurfaceSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfacePrimary,
                        unfocusedTextColor = OnSurfacePrimary,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = OnSurfaceTertiary,
                        cursorColor = HasselbladOrange
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text("URL", color = OnSurfaceSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfacePrimary,
                        unfocusedTextColor = OnSurfacePrimary,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = OnSurfaceTertiary,
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
                        Text("取消", color = OnSurfaceSecondary)
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
                            color = OnSurfacePrimary
                        )
                        Text(
                            text = source.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceTertiary,
                            maxLines = 1
                        )
                    }
                    
                    // 开关
                    Switch(
                        checked = source.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnSurfacePrimary,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = OutlineVariant,
                            uncheckedTrackColor = OutlineVariant.copy(alpha = 0.3f)
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
                            tint = OnSurfaceSecondary
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
        containerColor = MediumGray,
        title = {
            Text("添加预设源", color = OnSurfacePrimary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称", color = OnSurfaceSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfacePrimary,
                        unfocusedTextColor = OnSurfacePrimary,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = OnSurfaceTertiary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL", color = OnSurfaceSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurfacePrimary,
                        unfocusedTextColor = OnSurfacePrimary,
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = OnSurfaceTertiary
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
                Text("取消", color = OnSurfaceSecondary)
            }
        }
    )
}

private fun getDefaultSources(): List<PresetSource> = listOf(
    PresetSource(
        id = "oppo",
        name = "OPPO 预设库",
        url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
        enabled = true
    ),
    PresetSource(
        id = "realme",
        name = "realme 预设库",
        url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
        enabled = true
    ),
    PresetSource(
        id = "vivo",
        name = "vivo 预设库",
        url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
        enabled = true
    ),
    PresetSource(
        id = "honor",
        name = "荣耀 预设库",
        url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json",
        enabled = true
    )
)
