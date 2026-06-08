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
 * LUT资源分享功能页面
 * 提供20+专业LUT滤镜下载
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val categories = listOf("全部", "电影色调", "胶片风格", "日系清新", "欧美复古", "自然风景", "人像肤色", "创意特效")
    val selectedCategory = remember { mutableStateOf("全部") }
    val searchQuery = remember { mutableStateOf("") }
    var selectedLUT by remember { mutableStateOf<LUTResource?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val lutResources = remember {
        listOf(
            LUTResource("oxygen-2026", "氧气感2026", "清新通透，适合人像", "日系清新", "https://example.com/lut/oxygen.cube", 5000, 4.9),
            LUTResource("morandi-2026", "莫兰迪2026", "温柔低饱和", "电影色调", "https://example.com/lut/morandi.cube", 4200, 4.8),
            LUTResource("hasselblad-nature", "哈苏自然色彩", "HNCS自然色彩还原", "自然风景", "https://example.com/lut/hasselblad.cube", 8500, 4.9),
            LUTResource("cinematic-cold", "电影冷调", "好莱坞大片风格", "电影色调", "https://example.com/lut/cinematic.cube", 6800, 4.7),
            LUTResource("classic-film", "经典胶片", "复古胶片质感", "胶片风格", "https://example.com/lut/film.cube", 5600, 4.8),
            LUTResource("warm-sunset", "温暖日落", "金色时刻氛围", "自然风景", "https://example.com/lut/sunset.cube", 4100, 4.6),
            LUTResource("soft-cream", "奶油肌", "人像肤色优化", "人像肤色", "https://example.com/lut/cream.cube", 7200, 4.9),
            LUTResource("dramatic-contrast", "戏剧性对比", "高反差艺术", "创意特效", "https://example.com/lut/dramatic.cube", 3200, 4.5),
            LUTResource("vintage-america", "美式复古", "怀旧美式风格", "欧美复古", "https://example.com/lut/vintage.cube", 4900, 4.7),
            LUTResource("japanese-mood", "日系情绪", "清新日系风", "日系清新", "https://example.com/lut/japanese.cube", 5800, 4.8),
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
            title = { Text("LUT 资源分享", fontWeight = FontWeight.Bold) },
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
