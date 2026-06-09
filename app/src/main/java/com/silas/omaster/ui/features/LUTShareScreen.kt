package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * LUT资源库功能页面
 * 提供9款哈苏胶片LUT滤镜
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val categories = listOf("全部", "原生经典", "情绪表达", "结构时间", "数字记忆")
    val selectedCategory = remember { mutableStateOf("全部") }
    val searchQuery = remember { mutableStateOf("") }
    var selectedLUT by remember { mutableStateOf<LUTResource?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // 9款哈苏胶片LUT
    val lutResources = remember {
        listOf(
            // 原生经典系列
            LUTResource("classic-chrome", "Classic Chrome (CC)", "经典铬色，低饱和高对比", "原生经典", "https://cdn.hasselblad.com/lut/cc.cube", 12500, 4.9),
            LUTResource("neutral-color", "Neutral Color (NC)", "中性色彩，自然还原", "原生经典", "https://cdn.hasselblad.com/lut/nc.cube", 9800, 4.8),
            LUTResource("natural-hue", "Natural Hue (NH)", "自然色调，肤色优化", "原生经典", "https://cdn.hasselblad.com/lut/nh.cube", 8500, 4.9),
            // 情绪表达系列
            LUTResource("portra-400", "Portra 400", "专业人像胶片，温暖肤色", "情绪表达", "https://cdn.hasselblad.com/lut/portra.cube", 15600, 4.9),
            LUTResource("rdp3-fujifilm", "RDP3 Fujifilm", "富士反转片，鲜艳通透", "情绪表达", "https://cdn.hasselblad.com/lut/rdp3.cube", 7200, 4.7),
            // 结构时间系列
            LUTResource("cinestill-800t", "CineStill 800T", "电影夜景，钨丝灯暖调", "结构时间", "https://cdn.hasselblad.com/lut/800t.cube", 11300, 4.8),
            LUTResource("trix-400", "Tri-X 400 (TX400)", "经典黑白，颗粒质感", "结构时间", "https://cdn.hasselblad.com/lut/tx400.cube", 9500, 4.9),
            // 数字记忆系列
            LUTResource("ccd-warm", "CCD Warm", "数码暖调，复古质感", "数字记忆", "https://cdn.hasselblad.com/lut/ccd-warm.cube", 6800, 4.6),
            LUTResource("ccd-cool", "CCD Cool", "数码冷调，清透风格", "数字记忆", "https://cdn.hasselblad.com/lut/ccd-cool.cube", 5400, 4.7),
        )
    }

    val filteredLUTs = lutResources.filter { lut ->
        val matchCategory = selectedCategory.value == "全部" || lut.category == selectedCategory.value
        val matchSearch = lut.name.contains(searchQuery.value, ignoreCase = true) || 
                         lut.description.contains(searchQuery.value, ignoreCase = true)
        matchCategory && matchSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("LUT 资源库", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 搜索栏
            item {
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    placeholder = { Text("搜索 LUT 滤镜") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = HasselbladOrange,
                        unfocusedBorderColor = Color.Gray,
                        textColor = Color.White,
                        placeholderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 分类标签
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalScrollState = rememberScrollState()
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory.value == category,
                            onClick = {
                                haptic.perform(HapticFeedbackType.Select)
                                selectedCategory.value = category
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HasselbladOrange,
                                selectedLabelColor = Color.White,
                                unselectedLabelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }

            // LUT列表
            items(filteredLUTs) { lut ->
                LUTCard(
                    lut = lut,
                    onClick = {
                        haptic.perform(HapticFeedbackType.Select)
                        selectedLUT = lut
                        showDetailDialog = true
                    }
                )
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // LUT详情弹窗
    if (showDetailDialog && selectedLUT != null) {
        LUTDetailDialog(
            lut = selectedLUT!!,
            onDownload = {
                haptic.perform(HapticFeedbackType.Confirm)
                showDetailDialog = false
            },
            onDismiss = {
                haptic.perform(HapticFeedbackType.ToggleOff)
                showDetailDialog = false
            }
        )
    }
}

@Composable
private fun LUTCard(
    lut: LUTResource,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 预览图占位
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HasselbladOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, null, tint = HasselbladOrange, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = lut.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${lut.rating}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${lut.downloadCount} 下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun LUTDetailDialog(
    lut: LUTResource,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 预览区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, null, tint = HasselbladOrange, modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = lut.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = lut.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 信息标签
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        onClick = {},
                        label = { Text(lut.category) },
                        colors = ChipDefaults.chipColors(containerColor = HasselbladOrange.copy(alpha = 0.2f), labelColor = HasselbladOrange)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${lut.rating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${lut.downloadCount} 下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载")
                    }
                }
            }
        }
    }
}
