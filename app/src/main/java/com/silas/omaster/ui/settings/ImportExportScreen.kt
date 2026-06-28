package com.silas.omaster.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.silas.omaster.data.repository.ImportResult
import com.silas.omaster.data.repository.PresetItem
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_EXPORT_PRESETS = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val allPresets by repository.presets.collectAsState()

    // Export state
    var selectedPresetIds by remember { mutableStateOf(setOf<String>()) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var showExportWarning by remember { mutableStateOf(false) }

    // Import state
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showImportResultDialog by remember { mutableStateOf(false) }
    var showVersionErrorDialog by remember { mutableStateOf(false) }

    val selectAll = {
        haptic.perform(HapticFeedbackType.TextHandleMove)
        selectedPresetIds = allPresets.map { it.id }.toSet()
    }
    val deselectAll = {
        haptic.perform(HapticFeedbackType.TextHandleMove)
        selectedPresetIds = emptySet()
    }

    // File picker for import
    val pickJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        isImporting = true
        importProgress = 0f
        importError = null
        importResult = null

        coroutineScope.launch {
            try {
                importProgress = 0.2f
                val tempFile = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        importError = "无法打开文件，请检查文件是否已被删除或权限不足"
                        isImporting = false
                        return@withContext null
                    }
                    val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}.json")
                    inputStream.use { input ->
                        temp.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp
                }
                if (tempFile == null) return@launch

                importProgress = 0.5f

                val result = repository.importPresets(tempFile)

                importProgress = 1f

                withContext(Dispatchers.IO) {
                    tempFile.delete()
                }

                result.onSuccess { importRes ->
                    importResult = importRes
                    showImportResultDialog = true
                    haptic.perform(HapticFeedbackType.LongPress)
                }.onFailure { e ->
                    val message = e.message ?: "导入失败"
                    if (message.contains("版本号不兼容") || message.contains("版本")) {
                        showVersionErrorDialog = true
                    } else {
                        importError = message
                    }
                    haptic.perform(HapticFeedbackType.LongPress)
                }
            } catch (e: Exception) {
                importError = e.message ?: "导入过程中发生错误"
                haptic.perform(HapticFeedbackType.LongPress)
            } finally {
                isImporting = false
                importProgress = 0f
            }
        }
    }

    // Export function
    val doExport = {
        if (selectedPresetIds.size > MAX_EXPORT_PRESETS) {
            showExportWarning = true
        } else {
            isExporting = true
            exportProgress = 0f
            coroutineScope.launch {
                try {
                    exportProgress = 0.3f
                    val result = repository.exportPresets(selectedPresetIds)
                    exportProgress = 0.7f

                    result.onSuccess { file ->
                        exportProgress = 0.9f
                        shareExportFile(context, file)
                        haptic.perform(HapticFeedbackType.LongPress)
                    }.onFailure { e ->
                        Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                        haptic.perform(HapticFeedbackType.LongPress)
                    }
                } finally {
                    exportProgress = 1f
                    isExporting = false
                    exportProgress = 0f
                }
            }
        }
    }

    // Import result dialog
    if (showImportResultDialog && importResult != null) {
        ImportResultDialog(
            result = importResult,
            onDismiss = {
                showImportResultDialog = false
                importResult = null
            }
        )
    }

    // Export warning dialog
    if (showExportWarning) {
        AlertDialog(
            onDismissRequest = { showExportWarning = false },
            title = { Text("导出数量超限") },
            text = {
                Text("单次最多导出 $MAX_EXPORT_PRESETS 条预设，当前已选择 ${selectedPresetIds.size} 条。请减少选择后重试。")
            },
            confirmButton = {
                TextButton(onClick = { showExportWarning = false }) {
                    Text("知道了")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            textContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    // Version incompatibility dialog
    if (showVersionErrorDialog) {
        AlertDialog(
            onDismissRequest = { showVersionErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = HasselbladOrange
                )
            },
            title = { Text("版本不兼容") },
            text = {
                Text("导入文件的版本高于当前应用支持的版本，请更新应用后再试。")
            },
            confirmButton = {
                TextButton(onClick = { showVersionErrorDialog = false }) {
                    Text("确定", color = HasselbladOrange)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            textContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "导入/导出预设",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== Export Section ==========
            item {
                SectionHeader(
                    icon = Icons.Default.FileUpload,
                    title = "导出预设",
                    accentColor = HasselbladOrange
                )
            }

            // Select All / Deselect All + Count
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已选择 ${selectedPresetIds.size} / ${allPresets.size} 条预设",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            if (selectedPresetIds.size > MAX_EXPORT_PRESETS) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = HasselbladOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "超出上限",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HasselbladOrange
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = selectAll,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("全选")
                            }

                            OutlinedButton(
                                onClick = deselectAll,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("取消全选")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic.perform(HapticFeedbackType.TextHandleMove)
                                doExport()
                            },
                            enabled = selectedPresetIds.isNotEmpty() && !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HasselbladOrange,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                disabledContainerColor = HasselbladOrange.copy(alpha = 0.4f),
                                disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("导出中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("导出选中预设")
                            }
                        }

                        AnimatedVisibility(
                            visible = isExporting,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                LinearProgressIndicator(
                                    progress = { exportProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = HasselbladOrange,
                                    trackColor = HasselbladOrange.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }

            // Preset list with checkboxes
            items(items = allPresets, key = { it.id }) { preset ->
                PresetCheckboxItem(
                    preset = preset,
                    isSelected = selectedPresetIds.contains(preset.id),
                    onToggle = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        selectedPresetIds = if (selectedPresetIds.contains(preset.id)) {
                            selectedPresetIds - preset.id
                        } else {
                            selectedPresetIds + preset.id
                        }
                    }
                )
            }

            // ========== Import Section ==========
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    icon = Icons.Default.FileDownload,
                    title = "导入预设",
                    accentColor = SuccessGreen
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "选择一个 JSON 文件以导入预设。导入的预设将与已有预设进行冲突检测，同名系统预设将被跳过。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic.perform(HapticFeedbackType.TextHandleMove)
                                pickJsonLauncher.launch(arrayOf("application/json"))
                            },
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = androidx.compose.ui.graphics.Color.White,
                                disabledContainerColor = SuccessGreen.copy(alpha = 0.4f),
                                disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("导入中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("选择文件导入")
                            }
                        }

                        AnimatedVisibility(
                            visible = isImporting,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                LinearProgressIndicator(
                                    progress = { importProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SuccessGreen,
                                    trackColor = SuccessGreen.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Import error display
                        importError?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorRed,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun PresetCheckboxItem(
    preset: PresetItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                HasselbladOrange.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = HasselbladOrange,
                    uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange.copy(alpha = 0.8f)
                    )
                    if (preset.scene.isNotBlank()) {
                        Text(
                            text = preset.scene,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    if (preset.isSystem) {
                        Text(
                            text = "系统",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("导入完成") },
        text = {
            Column {
                // Imported count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "成功导入 ${result.imported} 条预设",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Skipped count
                if (result.skipped > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "跳过 ${result.skipped} 条（冲突）",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Conflicts list
                if (result.conflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "冲突预设：",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.conflicts.take(10).forEach { conflict ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\u2022",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HasselbladOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${conflict.name}（${conflict.brand}）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (result.conflicts.size > 10) {
                            Text(
                                text = "还有 ${result.conflicts.size - 10} 条冲突预设...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定", color = HasselbladOrange)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

private fun shareExportFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享预设文件"))
}
