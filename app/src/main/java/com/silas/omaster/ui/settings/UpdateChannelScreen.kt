package com.silas.omaster.ui.settings

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.core.content.FileProvider
import com.silas.omaster.BuildConfig
import androidx.compose.ui.res.stringResource
import com.silas.omaster.R
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.UrlConstants
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新渠道页面（对齐 Web 端 UpdateChannelPage.tsx）
 * - 当前版本 / 检查更新
 * - 渠道选择：稳定版 / 测试版 / 开发版
 * - 更新选项：自动检查 / 仅Wi-Fi / 夜间自动安装
 * - 发布说明
 * - 下载并安装更新
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
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var lastCheckTime by remember { mutableStateOf<String?>(null) }

    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadComplete by remember { mutableStateOf(false) }
    var downloadFailed by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }

    // 更新选项状态（对齐 Web 端 UPDATE_SETTINGS）
    var autoCheckEnabled by remember { mutableStateOf(true) }
    var wifiOnlyEnabled by remember { mutableStateOf(true) }
    var autoInstallEnabled by remember { mutableStateOf(false) }

    // 下载完成广播接收器
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    isDownloading = false
                    downloadComplete = true
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    // 下载进度轮询
    LaunchedEffect(isDownloading, downloadId) {
        while (isDownloading && downloadId >= 0) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (bytesTotal > 0) {
                    downloadProgress = bytesDownloaded.toFloat() / bytesTotal
                }
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_FAILED) {
                    isDownloading = false
                    downloadFailed = true
                }
            }
            cursor.close()
            delay(500)
        }
    }

    val channels = listOf(
        UpdateChannelInfo("stable", stringResource(R.string.update_channel_stable), stringResource(R.string.update_channel_stable_desc), Icons.Default.Shield, Color(0xFF10B981)),
        UpdateChannelInfo("beta", stringResource(R.string.update_channel_beta), stringResource(R.string.update_channel_beta_desc), Icons.Default.Bolt, Color(0xFFF59E0B)),
        UpdateChannelInfo("dev", stringResource(R.string.update_channel_dev), stringResource(R.string.update_channel_dev_desc), Icons.Default.Autorenew, Color(0xFF3B82F6))
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
                    Text(stringResource(R.string.update_settings_title), fontWeight = FontWeight.Bold)
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
                updateInfo = updateInfo,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                downloadComplete = downloadComplete,
                downloadFailed = downloadFailed,
                onCheckForUpdate = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    scope.launch {
                        isCheckingUpdate = true
                        updateInfo = null
                        downloadComplete = false
                        downloadFailed = false
                        try {
                            val result = checkForUpdate(context, selectedChannelId)
                            updateInfo = result
                            lastCheckTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date())
                        } catch (e: Exception) {
                            updateInfo = UpdateInfo(isNewVersion = false, latestVersion = "", releaseName = "", downloadUrl = "", htmlUrl = "", releaseNotes = "", message = "检查失败: ${e.message}")
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                },
                onDownload = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    val info = updateInfo ?: return@CurrentVersionCard
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val fileName = "OMaster-v${info.latestVersion}.apk"
                    val request = DownloadManager.Request(Uri.parse(info.downloadUrl)).apply {
                        setTitle("OMaster v${info.latestVersion}")
                        setDescription(context.getString(R.string.update_downloading))
                        setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setMimeType("application/vnd.android.package-archive")
                    }
                    downloadId = dm.enqueue(request)
                    isDownloading = true
                    downloadProgress = 0f
                    downloadComplete = false
                    downloadFailed = false
                },
                onInstall = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    val info = updateInfo ?: return@CurrentVersionCard
                    val fileName = "OMaster-v${info.latestVersion}.apk"
                    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (apkFile.exists()) {
                        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(installIntent)
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
                        text = stringResource(R.string.update_channel_label),
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
                        text = stringResource(R.string.update_options_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    UpdateOptionRow(
                        title = stringResource(R.string.update_option_auto_check),
                        checked = autoCheckEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            autoCheckEnabled = it
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    UpdateOptionRow(
                        title = stringResource(R.string.update_option_wifi_only),
                        checked = wifiOnlyEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            wifiOnlyEnabled = it
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    UpdateOptionRow(
                        title = stringResource(R.string.update_option_auto_install),
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
    updateInfo: UpdateInfo?,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadComplete: Boolean,
    downloadFailed: Boolean,
    onCheckForUpdate: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
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
                            text = if (updateInfo?.isNewVersion == true) "发现新版本" else stringResource(R.string.update_already_latest),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
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
                        text = if (lastCheckTime != null) stringResource(R.string.update_last_check, lastCheckTime) else stringResource(R.string.update_not_checked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCheckForUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCheckingUpdate && !isDownloading,
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
                        Text(stringResource(R.string.update_checking), fontWeight = FontWeight.Medium)
                    } else {
                        Text(stringResource(R.string.update_check_button), fontWeight = FontWeight.Medium)
                    }
                }

                // 显示检查结果
                updateInfo?.let { info ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (info.isNewVersion)
                                HasselbladOrange.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (info.isNewVersion)
                                    "发现新版本: v${info.latestVersion} (${info.releaseName})"
                                else info.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (info.isNewVersion && info.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = info.releaseNotes.take(200),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            // 下载进度条
                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.update_download_progress, (downloadProgress * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }

                            // 下载失败提示
                            if (downloadFailed) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.update_download_failed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF5722)
                                )
                            }
                        }
                    }

                    // 下载/安装按钮
                    if (info.isNewVersion && info.downloadUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (downloadComplete) {
                            // 安装按钮
                            Button(
                                onClick = onInstall,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.update_install_button), fontWeight = FontWeight.Medium)
                            }
                        } else if (!isDownloading) {
                            // 下载按钮
                            Button(
                                onClick = onDownload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.update_download_button), fontWeight = FontWeight.Medium)
                            }
                        }
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
 * 更新检查结果数据类
 */
private data class UpdateInfo(
    val isNewVersion: Boolean,
    val latestVersion: String,
    val releaseName: String,
    val downloadUrl: String,
    val htmlUrl: String,
    val releaseNotes: String,
    val message: String
)

/**
 * 真实检查更新：从 GitHub/Gitee Release API 获取最新版本信息
 *
 * 链路：选择渠道 → 确定 API URL → HTTPS 请求 → JSON 解析 → 版本比较 → 返回结构化结果
 */
private suspend fun checkForUpdate(context: android.content.Context, channel: String): UpdateInfo =
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
                    return@withContext UpdateInfo(
                        isNewVersion = false, latestVersion = "", releaseName = "",
                        downloadUrl = "", htmlUrl = "", releaseNotes = "",
                        message = "检查失败: HTTP $responseCode"
                    )
                }

                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                val latestVersion = json.optString("tag_name", "").removePrefix("v")
                val releaseName = json.optString("name", "")
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "")

                if (latestVersion.isBlank()) {
                    return@withContext UpdateInfo(
                        isNewVersion = false, latestVersion = "", releaseName = "",
                        downloadUrl = "", htmlUrl = "", releaseNotes = "",
                        message = "无法获取版本信息"
                    )
                }

                // 提取 APK 下载链接
                val downloadUrl = extractApkDownloadUrl(json)

                // 版本比较
                val currentVersion = BuildConfig.VERSION_NAME
                val comparison = compareVersions(latestVersion, currentVersion)

                if (comparison > 0) {
                    UpdateInfo(
                        isNewVersion = true,
                        latestVersion = latestVersion,
                        releaseName = releaseName,
                        downloadUrl = downloadUrl,
                        htmlUrl = htmlUrl,
                        releaseNotes = body,
                        message = "发现新版本: v$latestVersion ($releaseName)"
                    )
                } else {
                    UpdateInfo(
                        isNewVersion = false,
                        latestVersion = latestVersion,
                        releaseName = releaseName,
                        downloadUrl = downloadUrl,
                        htmlUrl = htmlUrl,
                        releaseNotes = body,
                        message = "当前已是最新版本 (v$currentVersion)"
                    )
                }
            } finally {
                try { conn?.disconnect() } catch (e: Exception) {
                    android.util.Log.w("UpdateChannel", "Failed to disconnect HTTP connection", e)
                }
            }
        } catch (e: Exception) {
            UpdateInfo(
                isNewVersion = false, latestVersion = "", releaseName = "",
                downloadUrl = "", htmlUrl = "", releaseNotes = "",
                message = "检查失败: ${e.message}"
            )
        }
    }

/**
 * 从 Release JSON 中提取 APK 下载链接
 * 优先选择 arm64-v8a 架构的 APK，其次选第一个 APK
 */
private fun extractApkDownloadUrl(json: JSONObject): String {
    // 尝试从 assets 数组中找 APK
    val assets = json.optJSONArray("assets") ?: return ""
    for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        val name = asset.optString("name", "")
        val downloadUrl = asset.optString("browser_download_url", "")
        if (name.endsWith(".apk") && (name.contains("arm64") || name.contains("universal"))) {
            return downloadUrl
        }
    }
    // 没找到 arm64/universal，返回第一个 APK
    for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        val name = asset.optString("name", "")
        val downloadUrl = asset.optString("browser_download_url", "")
        if (name.endsWith(".apk")) {
            return downloadUrl
        }
    }
    return ""
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
