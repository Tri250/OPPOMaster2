package com.silas.omaster.ui.features.watermark

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.watermark.WatermarkConfig
import com.silas.omaster.data.watermark.WatermarkPosition
import com.silas.omaster.data.watermark.WatermarkType
import com.silas.omaster.ui.theme.HasselbladOrange

/**
 * 水印模块主界面
 * 支持品牌水印新建、大师印记水印、XPAN 宽幅水印、应用到导出
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    onBack: () -> Unit,
    viewModel: WatermarkViewModel = viewModel(factory = WatermarkViewModel.Factory)
) {
    val context = LocalContext.current
    val watermarks by viewModel.watermarks.collectAsState()
    val selectedId by viewModel.selectedWatermarkId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水印") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "新建水印")
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
            if (watermarks.isEmpty()) {
                EmptyWatermarkState(onCreate = { showCreateDialog = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watermarks) { watermark ->
                        WatermarkCard(
                            config = watermark,
                            isSelected = watermark.id == selectedId,
                            onSelect = { viewModel.selectWatermark(watermark.id) },
                            onDelete = { showDeleteConfirm = watermark.id }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWatermarkDialog(
            onDismiss = { showCreateDialog = false },
            onCreateBrand = { name, text, color ->
                viewModel.createBrandWatermark(name, text, color)
                showCreateDialog = false
            },
            onCreateXpan = { name, topRatio, bottomRatio, topText, bottomText ->
                viewModel.createXpanWatermark(name, topRatio, bottomRatio, topText, bottomText)
                showCreateDialog = false
            }
        )
    }

    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除水印") },
            text = { Text("确定要删除这个水印吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWatermark(id)
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun WatermarkCard(
    config: WatermarkConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, HasselbladOrange)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (config.type) {
                        WatermarkType.BRAND -> Icons.Outlined.TextFields
                        WatermarkType.MASTER_MARK -> Icons.Outlined.Brush
                        WatermarkType.XPAN -> Icons.Outlined.AspectRatio
                    },
                    contentDescription = null,
                    tint = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (config.type) {
                            WatermarkType.BRAND -> "品牌水印: ${config.text}"
                            WatermarkType.MASTER_MARK -> "大师印记"
                            WatermarkType.XPAN -> "XPAN 宽幅"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "已选中",
                        tint = HasselbladOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmptyWatermarkState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有水印",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右上角 + 创建你的第一个水印",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建水印")
        }
    }
}

@Composable
private fun CreateWatermarkDialog(
    onDismiss: () -> Unit,
    onCreateBrand: (String, String, String) -> Unit,
    onCreateXpan: (String, Float, Float, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(WatermarkType.BRAND) }
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#FFFFFF") }
    var topRatio by remember { mutableStateOf("0.15") }
    var bottomRatio by remember { mutableStateOf("0.15") }
    var topText by remember { mutableStateOf("") }
    var bottomText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建水印") },
        text = {
            Column {
                // 类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WatermarkTypeChip(
                        label = "品牌",
                        selected = selectedType == WatermarkType.BRAND,
                        onClick = { selectedType = WatermarkType.BRAND }
                    )
                    WatermarkTypeChip(
                        label = "XPAN",
                        selected = selectedType == WatermarkType.XPAN,
                        onClick = { selectedType = WatermarkType.XPAN }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("水印名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when (selectedType) {
                    WatermarkType.BRAND -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("水印文字") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    WatermarkType.XPAN -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = topText,
                            onValueChange = { topText = it },
                            label = { Text("上黑边文字") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bottomText,
                            onValueChange = { bottomText = it },
                            label = { Text("下黑边文字") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedType) {
                        WatermarkType.BRAND -> onCreateBrand(name, text, color)
                        WatermarkType.XPAN -> onCreateXpan(
                            name,
                            topRatio.toFloatOrNull() ?: 0.15f,
                            bottomRatio.toFloatOrNull() ?: 0.15f,
                            topText,
                            bottomText
                        )
                        else -> {}
                    }
                },
                enabled = name.isNotBlank() && (selectedType != WatermarkType.BRAND || text.isNotBlank())
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun WatermarkTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) HasselbladOrange.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) HasselbladOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) HasselbladOrange else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
