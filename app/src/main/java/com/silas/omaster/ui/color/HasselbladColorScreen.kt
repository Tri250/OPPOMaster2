package com.silas.omaster.ui.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 哈苏色彩科学屏幕 - 用户可选择不同的哈苏色彩风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladColorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme by settingsManager.themeFlow.collectAsState(initial = BrandTheme.Hasselblad)

    val colorProfiles = remember {
        listOf(
            ColorProfile("HNCS 自然色", "哈苏自然色彩解决方案，真实还原场景色彩", HasselbladOrange, listOf(HasselbladOrange, HasselbladOrangeDark)),
            ColorProfile("鲜艳模式", "高饱和度色彩表现，适合风景和花卉", Color(0xFFFF6B35), listOf(Color(0xFFFF6B35), Color(0xFFE55A00))),
            ColorProfile("电影感", "电影级调色，浓郁影调", Color(0xFF8B5CF6), listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
            ColorProfile("黑白经典", "高对比度黑白，纪实摄影首选", Color(0xFF71717A), listOf(Color(0xFF27272A), Color(0xFF52525B))),
            ColorProfile("人文影调", "柔和自然，日常记录", OppoGold, listOf(OppoGold, OppoGoldDark)),
            ColorProfile("夜景模式", "低噪点高动态范围夜景风格", DeepOceanBlue, listOf(DeepOceanBlue, DeepOceanBlueDark)),
            ColorProfile("日出日落", "暖色调突出黄金时刻氛围", SunsetRed, listOf(SunsetRed, SunsetRedDark)),
            ColorProfile("清新自然", "明亮通透，绿色系突出", AuroraGreen, listOf(AuroraGreen, AuroraGreenDark)),
            ColorProfile("高端金色", "奢华金色调，艺术感", OppoSunriseGold, listOf(OppoSunriseGold, OppoGold)),
            ColorProfile("宇宙紫调", "神秘紫色，创意摄影", CosmicPurple, listOf(CosmicPurple, CosmicPurpleDark))
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OMasterTopAppBar(
            title = "哈苏色彩科学",
            subtitle = "HNCS 3.0 · 10 种风格",
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部介绍卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "哈苏自然色彩解决方案 3.0",
                        style = MaterialTheme.typography.titleLarge,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hasselblad Natural Color Solution (HNCS) 是哈苏百年来色彩科学的结晶，与 OPPO Find X 系列深度合作，为您提供专业级影像色彩表现。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // 色彩风格网格
            Text(
                text = "选择色彩风格",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            colorProfiles.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { profile ->
                        ColorProfileCard(
                            profile = profile,
                            isSelected = currentTheme.primaryColor == profile.primaryColor,
                            onClick = {
                                scope.launch {
                                    val newTheme = when (profile.name) {
                                        "HNCS 自然色" -> BrandTheme.Hasselblad
                                        "鲜艳模式" -> BrandTheme.Sony
                                        "电影感" -> BrandTheme.Zeiss
                                        "黑白经典" -> BrandTheme.PhaseOne
                                        "人文影调" -> BrandTheme.Nikon
                                        "夜景模式" -> BrandTheme.Zeiss
                                        "日出日落" -> BrandTheme.Canon
                                        "清新自然" -> BrandTheme.Fujifilm
                                        "高端金色" -> BrandTheme.Nikon
                                        "宇宙紫调" -> BrandTheme.Leica
                                        else -> BrandTheme.Hasselblad
                                    }
                                    settingsManager.setTheme(newTheme.id)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 当前选择详情
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "当前主题",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(currentTheme.primaryColor, currentTheme.primaryColor.copy(alpha = 0.6f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(currentTheme.brandNameResId),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(currentTheme.colorNameResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private data class ColorProfile(
    val name: String,
    val description: String,
    val primaryColor: Color,
    val gradient: List<Color>
)

@Composable
private fun ColorProfileCard(
    profile: ColorProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) profile.primaryColor.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, profile.primaryColor) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(profile.gradient))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "已选择",
                        tint = profile.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2
            )
        }
    }
}
