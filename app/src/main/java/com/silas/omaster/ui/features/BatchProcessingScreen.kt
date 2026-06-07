package com.silas.omaster.ui.features

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.batch.BatchProcessingManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

/**
 * 批量处理页面
 * 同步 Web 设计：多图同款处理
 * 支持批量选择、参数应用、进度追踪
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProcessingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { BatchProcessingManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current

    val selectedItems by manager.selectedItems.collectAsState()
    val processingState by manager.processingState.collectAsState()
    val batchParams by manager.batchParams.collectAsState()

    // 图片选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            manager.addItems(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.batch_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.batch_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            },
            actions = {
                if (selectedItems.isNotEmpty()) {
                    IconButton(onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        manager.clearAll()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.batch_clear), tint = HasselbladOrange)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 选择图片区域
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            imagePicker.launch("image/*")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HasselbladOrange.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = HasselbladOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.batch_select_images),
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.batch_max_hint, manager.maxSelectionCount),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        if (selectedItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.batch_select_count, selectedItems.size),
                                color = HasselbladOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 处理状态进度
            item {
                AnimatedVisibility(visible = processingState !is BatchProcessingManager.BatchState.Idle) {
                    ProcessingStateCard(processingState)
                }
            }

            // 已选图片列表
            if (selectedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "已选图片 (${selectedItems.size})",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(selectedItems) { item ->
                            SelectedImageChip(
                                name = item.name,
                                status = item.status,
                                onRemove = { manager.removeItem(item.id) }
                            )
                        }
                    }
                }
            }

            // 批量处理参数
            item {
                Text(
                    text = stringResource(R.string.batch_select_preset),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                BatchParamsCard(
                    params = batchParams,
                    onParamsChange = { manager.setBatchParams(it) }
                )
            }

            // 输出设置
            item {
                Text(
                    text = stringResource(R.string.batch_output_settings),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                OutputSettingsCard(
                    quality = batchParams.outputQuality,
                    onQualityChange = {
                        manager.setBatchParams(batchParams.copy(outputQuality = it))
                    }
                )
            }

            // 开始处理按钮
            item {
                val isProcessing = processingState is BatchProcessingManager.BatchState.Processing
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                Button(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        if (!isProcessing) {
                            coroutineScope.launch {
                                val outputDir = java.io.File(
                                    context.getExternalFilesDir(null),
                                    "batch_output"
                                )
                                manager.processBatch(outputDir)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) Color(0xFF424242) else HasselbladOrange
                    ),
                    enabled = selectedItems.isNotEmpty() && !isProcessing
                ) {
                    Icon(
                        imageVector = if (isProcessing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessing) stringResource(R.string.batch_cancel)
                                else stringResource(R.string.batch_start),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingStateCard(state: BatchProcessingManager.BatchState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state) {
                is BatchProcessingManager.BatchState.Selecting -> {
                    Text(
                        text = stringResource(R.string.batch_select_count, state.selectedCount),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                is BatchProcessingManager.BatchState.Processing -> {
                    Text(
                        text = stringResource(R.string.batch_progress) + " ${state.processed}/${state.total}",
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (state.processed.toFloat() / state.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = HasselbladOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.batch_processing_item, state.currentItem),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                is BatchProcessingManager.BatchState.Completed -> {
                    Text(
                        text = stringResource(R.string.batch_completed),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.batch_complete_msg, state.successCount, state.failedCount),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.batch_output_path, state.outputPath),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
                is BatchProcessingManager.BatchState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SelectedImageChip(
    name: String,
    status: BatchProcessingManager.ItemStatus,
    onRemove: () -> Unit
) {
    val statusColor = when (status) {
        BatchProcessingManager.ItemStatus.Pending -> Color.White.copy(alpha = 0.5f)
        BatchProcessingManager.ItemStatus.Processing -> HasselbladOrange
        BatchProcessingManager.ItemStatus.Success -> Color(0xFF4CAF50)
        BatchProcessingManager.ItemStatus.Failed -> Color.Red
    }
    val statusIcon = when (status) {
        BatchProcessingManager.ItemStatus.Pending -> Icons.Default.Add
        BatchProcessingManager.ItemStatus.Processing -> Icons.Default.Refresh
        BatchProcessingManager.ItemStatus.Success -> Icons.Default.Check
        BatchProcessingManager.ItemStatus.Failed -> Icons.Default.Close
    }
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = name,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun BatchParamsCard(
    params: BatchProcessingManager.BatchParams,
    onParamsChange: (BatchProcessingManager.BatchParams) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ParamSlider(
                label = stringResource(R.string.param_saturation),
                value = params.saturation.toFloat(),
                onValueChange = { onParamsChange(params.copy(saturation = it.toInt())) }
            )
            ParamSlider(
                label = stringResource(R.string.param_iso) + "对比",
                value = params.contrast.toFloat(),
                onValueChange = { onParamsChange(params.copy(contrast = it.toInt())) }
            )
            ParamSlider(
                label = "亮度",
                value = params.brightness.toFloat(),
                valueRange = -100f..100f,
                onValueChange = { onParamsChange(params.copy(brightness = it.toInt())) }
            )
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = value.toInt().toString(),
                color = HasselbladOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun OutputSettingsCard(
    quality: Int,
    onQualityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${stringResource(R.string.batch_quality)}: $quality%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = quality.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                valueRange = 50f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
