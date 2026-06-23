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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.BuildConfig
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.UrlConstants
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedChannelId by remember { mutableStateOf("stable") }

    // 更新检查状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }
    var lastCheckTime by remember { mutableStateOf<String?>(null) }

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
            .background(MaterialTheme.colorScheme.background)
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
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
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
            CurrentVersionCard(
                lastCheckTime = lastCheckTime,
                isCheckingUpdate = isCheckingUpdate,
                updateCheckResult = updateCheckResult,
                selectedChannelId = selectedChannelId,
                onCheckUpdate = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    scope.launch {
                        isCheckingUpdate = true
                        updateCheckResult = null
                        try {
                            val result = checkForUpdate(context, selectedChannelId)
                            updateCheckResult = result
                            lastCheckTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date())
                        } catch (e: Exception) {
                            updateCheckResult = "检查失败: ${e.message}"
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                }
            )

            // 更新渠道
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "更新渠道",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "更新选项",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
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
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    UpdateOptionRow(
                        title = "仅 Wi-Fi 下下载",
                        checked = wifiOnlyEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            wifiOnlyEnabled = it
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "v3.2.0 更新内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
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
private fun CurrentVersionCard(
    lastCheckTime: String?,
    isCheckingUpdate: Boolean,
    updateCheckResult: String?,
    selectedChannelId: String,
    onCheckUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
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
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "已是最新版本",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "v3.2.0 (20260608)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lastCheckTime != null) "最后检查：$lastCheckTime" else "尚未检查",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCheckUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCheckingUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在检查...", fontWeight = FontWeight.Medium)
                    } else {
                        Text("检查更新", fontWeight = FontWeight.Medium)
                    }
                }

                // 显示检查结果
                updateCheckResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.startsWith("发现新版本"))
                                HasselbladOrange.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                else Color.Transparent
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
                color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = channel.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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

/**
 * 真实检查更新：从 GitHub/Gitee Release API 获取最新版本信息
 *
 * 链路：选择渠道 → 确定 API URL → HTTPS 请求 → JSON 解析 → 版本比较 → 返回结果
 */
private suspend fun checkForUpdate(context: android.content.Context, channel: String): String =
    withContext(Dispatchers.IO) {
        try {
            // 根据渠道选择 API 端点
            val apiUrl = when (channel) {
                "stable" -> UrlConstants.GITHUB_API_RELEASES
                "beta" -> UrlConstants.GITHUB_API_RELEASES
                "dev" -> UrlConstants.GITEE_API_RELEASES
                else -> UrlConstants.GITHUB_API_RELEASES
            }

            val url = URL(apiUrl)
            var conn: HttpURLConnection? = null
            try {
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    return@withContext "检查失败: HTTP $responseCode"
                }

                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val latestVersion = json.optString("tag_name", "").removePrefix("v")
                val releaseName = json.optString("name", "")
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "")

                if (latestVersion.isBlank()) {
                    return@withContext "无法获取版本信息"
                }

                // 版本比较
                val currentVersion = BuildConfig.VERSION_NAME
                val comparison = compareVersions(latestVersion, currentVersion)

                if (comparison > 0) {
                    "发现新版本: v$latestVersion ($releaseName)\n${if (htmlUrl.isNotBlank()) "下载: $htmlUrl" else ""}\n\n${body.take(200)}"
                } else {
                    "当前已是最新版本 (v$currentVersion)"
                }
            } finally {
                try { conn?.disconnect() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            "检查失败: ${e.message}"
        }
    }

/**
 * 语义化版本比较
 * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
 */
private fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(parts1.size, parts2.size)
    for (i in 0 until maxLen) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1 - p2
    }
    return 0
}
