package com.silas.omaster.ui.features

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*
import com.silas.omaster.util.perform

/**
 * LUT 资源分享页面
 * 
 * 功能：
 * - 展示专业 LUT 滤镜库
 * - LUT 分类浏览
 * - LUT 下载和应用
 * - LUT 效果预览
 * 
 * 对齐 Web 端 LUTSharePage.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTShareScreen(
    onBack: () -> Unit,
    onDownload: (LUTItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // 分类选择
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "电影", "胶片", "风景", "人像", "商业", "创意")
    
    // LUT 数据
    val luts = remember {
        listOf(
            LUTItem("lut_1", "柯达 Portra 400", "经典胶片风格，温暖柔和", "胶片", "https://example.com/lut1.cube", true),
            LUTItem("lut_2", "富士 Velvia 50", "高饱和度，风景首选", "胶片", "https://example.com/lut2.cube", false),
            LUTItem("lut_3", "好莱坞电影", "电影级调色，专业质感", "电影", "https://example.com/lut3.cube", true),
            LUTItem("lut_4", "哈苏 HNCS", "自然色彩还原，专业标准", "风景", "https://example.com/lut4.cube", true),
            LUTItem("lut_5", "人像柔光", "柔和肤色，自然美化", "人像", "https://example.com/lut5.cube", false),
            LUTItem("lut_6", "商业广告", "明亮通透，产品展示", "商业", "https://example.com/lut6.cube", true),
            LUTItem("lut_7", "赛博朋克", "霓虹色彩，科幻风格", "创意", "https://example.com/lut7.cube", false),
            LUTItem("lut_8", "黑白经典", "黑白胶片质感", "胶片", "https://example.com/lut8.cube", true),
            LUTItem("lut_9", "日落暖调", "温暖日落氛围", "风景", "https://example.com/lut9.cube", false),
            LUTItem("lut_10", "复古怀旧", "复古褪色效果", "创意", "https://example.com/lut10.cube", true)
        )
    }
    
    // 过滤后的 LUT
    val filteredLuts = if (selectedCategory == "全部") {
        luts
    } else {
        luts.filter { it.category == selectedCategory }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("LUT 资源库", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PureBlack,
                        titleContentColor = Color.White
                    )
                )

                // 分类标签
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (category == selectedCategory) HasselbladOrange
                                    else Color(0xFF2A2A2A)
                                )
                                .clickable {
                                    haptic.perform(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = category
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                                color = if (category == selectedCategory) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

        // LUT 数量统计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filteredLuts.size} 个 LUT",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${filteredLuts.filter { it.isFree }.size} 个免费",
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladOrange
            )
        }

        // LUT 列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredLuts) { lut ->
                LUTCard(
                    lut = lut,
                    onDownload = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onDownload(lut)
                    }
                )
            }
        }
    }
}

@Composable
private fun LUTCard(
    lut: LUTItem,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：预览图和信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 预览图占位
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = lut.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = lut.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        
                        if (lut.isFree) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HasselbladOrange)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "免费",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = lut.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    
                    Text(
                        text = "分类: ${lut.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            // 右侧：下载按钮
            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HasselbladOrange.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.Download,
                    "下载",
                    tint = HasselbladOrange
                )
            }
        }
    }
}

/**
 * LUT 数据项
 */
data class LUTItem(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val url: String,
    val isFree: Boolean = false
)