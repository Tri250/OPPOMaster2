package com.silas.omaster.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.DividerColor
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.OnSurfaceInverse
import com.silas.omaster.ui.theme.OnSurfacePrimary
import com.silas.omaster.ui.theme.OnSurfaceSecondary
import com.silas.omaster.ui.theme.OnSurfaceTertiary
import com.silas.omaster.ui.theme.OutlineVariant
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SurfaceOverlay
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.perform

/**
 * 更新渠道页面（对齐 Web 端 UpdateChannelPage.tsx）
 * - 当前版本 / 检查更新
 * - 渠道选择：稳定版 / 测试版 / 开发版
 * - 更新选项：自动检查 / 仅Wi-Fi / 夜间自动安装
 * - 发布说明
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateChannelScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedChannelId by remember { mutableStateOf("stable") }

    // 更新选项状态（对齐 Web 端 UPDATE_SETTINGS）
    var autoCheckEnabled by remember { mutableStateOf(true) }
    var wifiOnlyEnabled by remember { mutableStateOf(true) }
    var autoInstallEnabled by remember { mutableStateOf(false) }

    val channels = listOf(
        UpdateChannelInfo("stable", "稳定版", "最稳定的版本，推荐日常使用", Icons.Default.Shield, Color(0xFF10B981)),
        UpdateChannelInfo("beta", "测试版", "提前体验新功能，可能存在小问题", Icons.Default.Bolt, Color(0xFFF59E0B)),
        UpdateChannelInfo("dev", "开发版", "最新功能，适合尝鲜用户", Icons.Default.Autorenew, Color(0xFF3B82F6))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TopAppBar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = HasselbladOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更新设置", fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = OnSurfacePrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = OnSurfacePrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前版本卡片
            CurrentVersionCard()

            // 更新渠道
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "更新渠道",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfacePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    channels.forEach { channel ->
                        ChannelOptionRow(
                            channel = channel,
                            isSelected = selectedChannelId == channel.id,
                            onClick = {
                                haptic.perform(HapticFeedbackType.LongPress)
                                selectedChannelId = channel.id
                            }
                        )
                    }
                }
            }

            // 更新选项
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "更新选项",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfacePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    UpdateOptionRow(
                        title = "自动检查更新",
                        checked = autoCheckEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            autoCheckEnabled = it
                        }
                    )
                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                    UpdateOptionRow(
                        title = "仅 Wi-Fi 下下载",
                        checked = wifiOnlyEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            wifiOnlyEnabled = it
                        }
                    )
                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                    UpdateOptionRow(
                        title = "夜间自动安装",
                        checked = autoInstallEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            autoInstallEnabled = it
                        }
                    )
                }
            }

            // 发布说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "v3.2.0 更新内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfacePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ReleaseNoteItem("全新 LUT 资源下载功能，20+ 专业滤镜")
                    ReleaseNoteItem("哈苏色彩科学升级，HNCS 3.0 自然色彩")
                    ReleaseNoteItem("哈苏之眼增强，支持 50+ 精细场景")
                    ReleaseNoteItem("优化性能，启动速度提升 30%")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CurrentVersionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(HasselbladOrange, WarningYellow)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceOverlay),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = OnSurfacePrimary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "已是最新版本",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfacePrimary
                        )
                        Text(
                            text = "v3.2.0 (20260608)",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfacePrimary.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = OnSurfaceSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "最后检查：刚刚",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /* 检查更新 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceOverlay,
                        contentColor = OnSurfacePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("检查更新", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ChannelOptionRow(
    channel: UpdateChannelInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) HasselbladOrange.copy(alpha = 0.1f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(channel.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(channel.icon, null, tint = channel.color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) HasselbladOrange else OnSurfacePrimary
            )
            Text(
                text = channel.description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceTertiary
            )
        }
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = HasselbladOrange)
        }
    }
}

@Composable
private fun UpdateOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceSecondary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnSurfacePrimary,
                checkedTrackColor = HasselbladOrange
            )
        )
    }
}

@Composable
private fun ReleaseNoteItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = HasselbladOrange,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceSecondary
        )
    }
}

private data class UpdateChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)
