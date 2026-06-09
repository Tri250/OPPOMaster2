package com.silas.omaster.ui.camera

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.data.model.MasterLUT
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * 拍摄时LUT叠加层 - 哈苏大师专业设计
 * 在相机预览界面叠加已下载的LUT效果
 */
@Composable
fun CameraLUTOverlay(
    downloadedLUTs: List<MasterLUT>,
    currentLUT: MasterLUT?,
    lutIntensity: Float = 1f, // 0-1
    showLUTPanel: Boolean = false,
    onLUTSelect: (MasterLUT?) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onTogglePanel: () -> Unit,
    onCapture: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // === LUT效果指示器（顶部） ===
        if (currentLUT != null) {
            LUTIndicator(
                lut = currentLUT,
                intensity = lutIntensity,
                onToggle = onTogglePanel,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // === LUT选择面板（底部滑出） ===
        AnimatedVisibility(
            visible = showLUTPanel,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LUTSelectionPanel(
                luts = downloadedLUTs,
                currentLUT = currentLUT,
                lutIntensity = lutIntensity,
                onLUTSelect = onLUTSelect,
                onIntensityChange = onIntensityChange,
                onCapture = onCapture,
                onClose = onTogglePanel
            )
        }
        
        // === 快捷切换按钮（无LUT选中时） ===
        if (currentLUT == null && downloadedLUTs.isNotEmpty()) {
            FloatingActionButton(
                onClick = onTogglePanel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = CircleShape,
                containerColor = HasselbladOrange.copy(alpha = 0.9f),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Palette, "选择LUT")
            }
        }
    }
}

// === 子组件 ===

@Composable
private fun LUTIndicator(
    lut: MasterLUT,
    intensity: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 60.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        HasselbladOrange.copy(alpha = 0.9f),
                        HasselbladOrange.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LUT图标
        AsyncImage(
            model = lut.coverImage,
            contentDescription = lut.name,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        
        Column {
            Text(
                text = lut.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "强度 ${(intensity * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (lut.isHncsCertified) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "HNCS认证",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 切换按钮
        Icon(
            Icons.Default.SwapHoriz,
            contentDescription = "切换",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LUTSelectionPanel(
    luts: List<MasterLUT>,
    currentLUT: MasterLUT?,
    lutIntensity: Float,
    onLUTSelect: (MasterLUT?) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onCapture: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PureBlack.copy(alpha = 0.95f),
                        PureBlack
                    )
                )
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        // === 拖拽手柄 ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
        
        // === 标题栏 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "选择色彩配方",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 关闭按钮
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        // === 强度调节 ===
        if (currentLUT != null) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    "LUT强度",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 0%
                    Text(
                        "0%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    
                    Slider(
                        value = lutIntensity,
                        onValueChange = onIntensityChange,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = HasselbladOrange,
                            activeTrackColor = HasselbladOrange,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    
                    // 100%
                    Text(
                        "100%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // 当前强度显示
                Text(
                    "当前: ${(lutIntensity * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
        }
        
        // === LUT列表 ===
        Text(
            "已下载的LUT",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        LazyRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 无LUT选项
            item {
                NoLUTOption(
                    selected = currentLUT == null,
                    onClick = { onLUTSelect(null) }
                )
            }
            
            items(luts) { lut ->
                LUTThumbnail(
                    lut = lut,
                    selected = currentLUT?.id == lut.id,
                    onClick = { onLUTSelect(lut) }
                )
            }
        }
        
        // === 操作按钮 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 取消LUT
            OutlinedButton(
                onClick = { onLUTSelect(null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Icon(Icons.Outlined.Block, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("取消LUT")
            }
            
            // 拍摄
            Button(
                onClick = {
                    onCapture()
                    onClose()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("拍摄")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NoLUTOption(
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) HasselbladOrange.copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) HasselbladOrange.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Block,
                contentDescription = "无LUT",
                tint = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Text(
            "原始",
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LUTThumbnail(
    lut: MasterLUT,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) HasselbladOrange.copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = lut.coverImage,
                contentDescription = lut.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 选中指示
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HasselbladOrange.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // HNCS认证徽章
            if (lut.isHncsCertified) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "HNCS",
                    tint = HasselbladOrange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                )
            }
        }
        
        // 名称
        Text(
            text = lut.filmPresetMapping.ifEmpty { lut.name },
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// === 预览用的简化版本 ===

/**
 * 简化版LUT选择器 - 用于快速切换
 */
@Composable
fun QuickLUTSelector(
    luts: List<MasterLUT>,
    currentLUT: MasterLUT?,
    onSelect: (MasterLUT?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // 触发按钮
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (currentLUT != null) HasselbladOrange.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.1f),
                    CircleShape
                )
        ) {
            if (currentLUT != null) {
                AsyncImage(
                    model = currentLUT.coverImage,
                    contentDescription = currentLUT.name,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "选择LUT",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(PureBlack.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                .width(200.dp)
        ) {
            // 无LUT选项
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Block, null, tint = Color.White.copy(alpha = 0.5f))
                        Text("原始（无LUT）", color = Color.White)
                    }
                },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            luts.forEach { lut ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = lut.coverImage,
                                contentDescription = lut.name,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(lut.name, color = Color.White, fontSize = 13.sp)
                                if (lut.isHncsCertified) {
                                    Text(
                                        "HNCS认证",
                                        color = HasselbladOrange,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            if (currentLUT?.id == lut.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = HasselbladOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(lut)
                        expanded = false
                    }
                )
            }
        }
    }
}