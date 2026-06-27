package com.silas.omaster.trailsnap.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.ToolboxItem
import com.silas.omaster.trailsnap.model.ToolboxTool
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.CoroutineScope

@Composable
fun ToolboxScreen(
    onBack: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val photos by repository.photos.collectAsState()
    val tools = remember(photos) { repository.getToolboxItems() }

    var activeDialog by remember { mutableStateOf<ToolboxDialog?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TrailSnapTopBar(title = "工具箱", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ToolboxHeader()
            }
            items(tools) { item ->
                ToolboxCard(
                    item = item,
                    onClick = {
                        when (item.tool) {
                            ToolboxTool.DUPLICATE_CLEANUP -> activeDialog = ToolboxDialog.DuplicateCleanup
                            ToolboxTool.SIMILAR_PHOTOS -> activeDialog = ToolboxDialog.SimilarPhotos
                            ToolboxTool.ORGANIZE_BY_DATE -> activeDialog = ToolboxDialog.OrganizeByDate
                            ToolboxTool.RENAME_BATCH -> activeDialog = ToolboxDialog.BatchRename
                            ToolboxTool.TIME_FROM_FILENAME -> {
                                val fixed = repository.fixTimeFromFilename()
                                Toast.makeText(context, "已修复 $fixed 张照片时间", Toast.LENGTH_SHORT).show()
                            }
                            ToolboxTool.RECYCLE_BIN -> onNavigateToRecycleBin()
                        }
                    }
                )
            }
        }
    }

    when (val dialog = activeDialog) {
        is ToolboxDialog.DuplicateCleanup -> DuplicateCleanupDialog(
            repository = repository,
            onDismiss = { activeDialog = null }
        )
        is ToolboxDialog.SimilarPhotos -> SimilarPhotosDialog(
            repository = repository,
            onDismiss = { activeDialog = null }
        )
        is ToolboxDialog.OrganizeByDate -> OrganizeByDateDialog(
            repository = repository,
            onDismiss = { activeDialog = null }
        )
        is ToolboxDialog.BatchRename -> BatchRenameDialog(
            repository = repository,
            onDismiss = { activeDialog = null }
        )
        null -> {}
    }
}

private sealed class ToolboxDialog {
    data object DuplicateCleanup : ToolboxDialog()
    data object SimilarPhotos : ToolboxDialog()
    data object OrganizeByDate : ToolboxDialog()
    data object BatchRename : ToolboxDialog()
}

@Composable
private fun ToolboxHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HasselbladOrange.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "照片整理助手",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "清理、整理、重命名，让图库井井有条",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ToolboxCard(
    item: ToolboxItem,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "toolbox_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(HasselbladOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = toolboxIcon(item.tool),
                contentDescription = item.title,
                tint = HasselbladOrange,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.badgeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HasselbladOrange)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.badgeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DuplicateCleanupDialog(
    repository: TrailSnapRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(DuplicateStep.Confirm) }
    var removed by remember { mutableStateOf(0) }

    when (step) {
        DuplicateStep.Confirm -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("重复照片清理") },
            text = {
                Text("将保留每组重复照片中的第一张，其余照片移入回收站。是否继续？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        removed = repository.cleanupDuplicates()
                        step = DuplicateStep.Result
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("开始清理")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
        DuplicateStep.Result -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("清理完成") },
            text = { Text("已将 $removed 张重复照片移入回收站，可在回收站中恢复或彻底删除。") },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("知道了")
                }
            }
        )
    }
}

private enum class DuplicateStep { Confirm, Result }

@Composable
private fun SimilarPhotosDialog(
    repository: TrailSnapRepository,
    onDismiss: () -> Unit
) {
    val similarGroups = remember { repository.getSimilarPhotoGroups() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("相似照片整理") },
        text = {
            if (similarGroups.isEmpty()) {
                Text("未检测到明显相似照片")
            } else {
                Column {
                    Text("检测到 ${similarGroups.size} 组相似照片，可前往“重复照片清理”合并。")
                    Spacer(modifier = Modifier.height(8.dp))
                    similarGroups.take(5).forEach { photos ->
                        val sample = photos.firstOrNull()?.filename ?: "未知"
                        Text(
                            text = "$sample 等 ${photos.size} 张",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun OrganizeByDateDialog(
    repository: TrailSnapRepository,
    onDismiss: () -> Unit
) {
    val plan = remember { repository.getOrganizeByDatePlan() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("按日期整理方案") },
        text = {
            if (plan.isEmpty()) {
                Text("暂无可整理照片")
            } else {
                Column {
                    Text("将按年月归档到以下文件夹：")
                    Spacer(modifier = Modifier.height(8.dp))
                    plan.entries.sortedByDescending { it.value.size }.take(6).forEach { (month, photos) ->
                        Text(
                            text = "$month · ${photos.size} 张",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun BatchRenameDialog(
    repository: TrailSnapRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preview = remember { repository.getBatchRenamePreview() }
    var step by remember { mutableStateOf(BatchRenameStep.Preview) }
    var renamed by remember { mutableStateOf(0) }

    when (step) {
        BatchRenameStep.Preview -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("批量重命名预览") },
            text = {
                if (preview.isEmpty()) {
                    Text("暂无可重命名照片")
                } else {
                    Column {
                        Text("共 ${preview.size} 张照片将按以下规则重命名：")
                        Spacer(modifier = Modifier.height(8.dp))
                        preview.entries.take(5).forEach { (oldName, newName) ->
                            Text(
                                text = "$oldName → $newName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        if (preview.size > 5) {
                            Text(
                                text = "... 等 ${preview.size - 5} 项",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        renamed = repository.applyBatchRename(preview)
                        step = BatchRenameStep.Result
                    },
                    enabled = preview.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("应用重命名")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
        BatchRenameStep.Result -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("重命名完成") },
            text = { Text("成功重命名 $renamed / ${preview.size} 张照片。受系统权限限制，部分照片可能无法重命名。") },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("知道了")
                }
            }
        )
    }
}

private enum class BatchRenameStep { Preview, Result }

private fun toolboxIcon(tool: ToolboxTool): ImageVector = when (tool) {
    ToolboxTool.DUPLICATE_CLEANUP -> Icons.Default.ContentCopy
    ToolboxTool.SIMILAR_PHOTOS -> Icons.Default.CleaningServices
    ToolboxTool.ORGANIZE_BY_DATE -> Icons.Default.CalendarToday
    ToolboxTool.RENAME_BATCH -> Icons.Default.Edit
    ToolboxTool.TIME_FROM_FILENAME -> Icons.Default.Schedule
    ToolboxTool.RECYCLE_BIN -> Icons.Default.DeleteOutline
}
