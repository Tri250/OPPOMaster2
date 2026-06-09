package com.silas.omaster.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.data.model.*
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * 哈苏大师 LUT 详情页 - 全屏 BottomSheet
 * 专业级设计，完整信息展示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTDetailBottomSheet(
    lut: MasterLUT,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    isFavorite: Boolean = false,
    onDownload: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    onApplyParams: ((LUTParams) -> Unit)? = null,
    onRelatedClick: ((MasterLUT) -> Unit)? = null,
    relatedLUTs: List<MasterLUT> = emptyList()
) {
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 直接展开到全屏
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            // 自定义拖拽手柄
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // === 封面预览区 ===
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // 主封面图
                    AsyncImage(
                        model = lut.coverImage,
                        contentDescription = lut.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        PureBlack.copy(alpha = 0.5f),
                                        PureBlack.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                    
                    // 品牌徽章
                    if (lut.isHncsCertified) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(HasselbladOrange, HasselbladOrange.copy(alpha = 0.8f))
                                    ),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "HNCS认证",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                "HNCS认证",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // 状态徽章
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (lut.isNew) {
                            StatusBadge("NEW", Color(0xFF4CAF50))
                        }
                        if (lut.isHot) {
                            StatusBadge("HOT", HasselbladOrange)
                        }
                        if (lut.isFeatured) {
                            StatusBadge("精选", Color(0xFFFFD700))
                        }
                    }
                    
                    // 标题区
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = lut.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (lut.nameEn.isNotEmpty()) {
                            Text(
                                text = lut.nameEn,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        // 系列标签
                        if (lut.hasselbladCollection.isNotEmpty()) {
                            Text(
                                text = "哈苏 ${lut.hasselbladCollection} 系列",
                                fontSize = 12.sp,
                                color = HasselbladOrange,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // === 快捷操作栏 ===
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 评分
                    StatCard(
                        icon = Icons.Default.Star,
                        value = lut.rating.toString(),
                        label = "评分",
                        color = Color(0xFFFFD700)
                    )
                    // 下载量
                    StatCard(
                        icon = Icons.Default.Download,
                        value = formatCount(lut.downloads),
                        label = "下载",
                        color = HasselbladOrange
                    )
                    // 文件大小
                    StatCard(
                        icon = Icons.Default.Storage,
                        value = formatFileSize(lut.fileSize),
                        label = "大小",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    // 尺寸
                    StatCard(
                        icon = Icons.Default.GridOn,
                        value = "${lut.size.value}³",
                        label = "精度",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // === 描述区 ===
            item {
                Text(
                    text = lut.description,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // === 详细描述（拍摄建议） ===
            if (lut.longDescription.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "大师拍摄建议",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HasselbladOrange
                                )
                            }
                            Text(
                                text = lut.longDescription,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
            
            // === 技术规格区 ===
            item {
                SectionTitle("技术规格", Icons.Default.Settings)
                SpecGrid(lut)
            }
            
            // === 适用场景 ===
            if (lut.suitableFor.isNotEmpty()) {
                item {
                    SectionTitle("适用场景", Icons.Default.PhotoCamera)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(lut.suitableFor) { scene ->
                            SceneChip(scene)
                        }
                    }
                }
            }
            
            // === 标签 ===
            if (lut.tags.isNotEmpty()) {
                item {
                    SectionTitle("风格标签", Icons.Default.Tag)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(lut.tags) { tag ->
                            TagChip(tag)
                        }
                    }
                }
            }
            
            // === 兼容软件 ===
            if (lut.compatibleSoftware.isNotEmpty()) {
                item {
                    SectionTitle("兼容软件", Icons.Default.Computer)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(lut.compatibleSoftware) { software ->
                            SoftwareChip(software)
                        }
                    }
                }
            }
            
            // === 作者信息 ===
            item {
                SectionTitle("创作者", Icons.Default.Person)
                AuthorCard(lut)
            }
            
            // === 关联推荐 ===
            if (relatedLUTs.isNotEmpty()) {
                item {
                    SectionTitle("相关推荐", Icons.Default.AutoAwesome)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(relatedLUTs) { related ->
                            RelatedLUTCard(
                                lut = related,
                                onClick = { onRelatedClick?.invoke(related) }
                            )
                        }
                    }
                }
            }
            
            // === 反推参数（如果有） ===
            if (lut.generatedParams != null && onApplyParams != null) {
                item {
                    SectionTitle("哈苏大师参数", Icons.Default.Tune)
                    GeneratedParamsCard(
                        params = lut.generatedParams,
                        onApply = { onApplyParams(lut.generatedParams) }
                    )
                }
            }
            
            // === 样片画廊 ===
            if (lut.sampleImages.isNotEmpty()) {
                item {
                    SectionTitle("效果样片", Icons.Default.Collection)
                    SampleGallery(lut.sampleImages)
                }
            }
            
            // === 使用指南 ===
            if (lut.usageGuide.isNotEmpty()) {
                item {
                    SectionTitle("使用指南", Icons.Default.HelpOutline)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Text(
                            text = lut.usageGuide,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        
        // === 底部操作栏 ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureBlack)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 收藏按钮
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isFavorite) HasselbladOrange.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isFavorite) HasselbladOrange else Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // 分享按钮
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(lut.downloadUrl))
                        onShare()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // 下载/应用按钮
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDownloaded) Color(0xFF4CAF50) else HasselbladOrange
                    )
                ) {
                    if (isDownloading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text("${downloadProgress}%")
                        }
                    } else if (isDownloaded) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null)
                            Text("已下载")
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, null)
                            Text("下载 LUT")
                        }
                    }
                }
            }
        }
    }
}

// === 子组件 ===

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HasselbladOrange,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SpecGrid(lut: MasterLUT) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 格式
        SpecRow("文件格式", ".${lut.format.extension.uppercase()}", Icons.Default.FilePresent)
        // 尺寸
        SpecRow("色彩精度", "${lut.size.value}×${lut.size.value}×${lut.size.value}", Icons.Default.GridOn)
        // 文件大小
        SpecRow("文件大小", formatFileSize(lut.fileSize), Icons.Default.Storage)
        // 版本
        SpecRow("版本", "v${lut.version}", Icons.Default.Update)
        // 更新时间
        if (lut.updatedAt.isNotEmpty()) {
            SpecRow("更新日期", lut.updatedAt.substring(0, 10), Icons.Default.CalendarToday)
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun SceneChip(scene: String) {
    Box(
        modifier = Modifier
            .background(HasselbladOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = scene,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = HasselbladOrange
        )
    }
}

@Composable
private fun TagChip(tag: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "#$tag",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SoftwareChip(software: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = software,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AuthorCard(lut: MasterLUT) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            AsyncImage(
                model = lut.authorAvatar.ifEmpty { 
                    "https://cdn.hasselblad.com/avatar/default.png" 
                },
                contentDescription = lut.author,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange.copy(alpha = 0.2f)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = lut.author,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = lut.source.displayName,
                    fontSize = 12.sp,
                    color = HasselbladOrange,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "发布于 ${lut.createdAt.substring(0, 10)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (lut.authorUrl.isNotEmpty()) {
                IconButton(
                    onClick = { /* 打开作者主页 */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "访问主页",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedLUTCard(
    lut: MasterLUT,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Column {
            AsyncImage(
                model = lut.coverImage,
                contentDescription = lut.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = lut.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = lut.rating.toString(),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedParamsCard(
    params: LUTParams,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "从LUT反推的哈苏大师参数近似值",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // 参数网格
            ParamItem("饱和度", params.saturation, -1f, 1f)
            ParamItem("对比度", params.contrast, -1f, 1f)
            ParamItem("亮度", params.brightness, -1f, 1f)
            ParamItem("色温", params.colorTemperature, -1f, 1f)
            ParamItem("色调", params.tint, -1f, 1f)
            ParamItem("高光衰减", params.highlightRolloff, 0f, 1f)
            ParamItem("阴影提升", params.shadowLift, 0f, 1f)
            
            if (params.skinProtection) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "肤色保护已启用",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            OutlinedButton(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HasselbladOrange
                )
            ) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("应用参数到大师模式")
            }
        }
    }
}

@Composable
private fun ParamItem(label: String, value: Float, min: Float, max: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp)
        )
        
        // 进度条
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
        ) {
            val progress = (value - min) / (max - min)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(HasselbladOrange, RoundedCornerShape(2.dp))
            )
        }
        
        Text(
            text = if (value >= 0) "+${(value * 30).toInt()}" else "${(value * 30).toInt()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier
                .padding(start = 8.dp)
                .width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SampleGallery(images: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(images) { image ->
            AsyncImage(
                model = image,
                contentDescription = "样片",
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// === 工具函数 ===

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}