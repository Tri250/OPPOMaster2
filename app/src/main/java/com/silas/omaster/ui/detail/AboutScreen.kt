package com.silas.omaster.ui.detail

import android.app.DownloadManager
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.ui.settings.ThemeSelectionDialog
import com.silas.omaster.ui.settings.DarkModeDialog
import com.silas.omaster.ui.settings.UpdateChannelDialog
// import com.silas.omaster.util.UpdateChecker  // 版本更新功能已暂停
import com.silas.omaster.util.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.silas.omaster.util.perform

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPresetSourceManager: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    currentVersionCode: Int = VersionInfo.VERSION_CODE,
    currentVersionName: String = VersionInfo.VERSION_NAME,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme by settingsManager.themeFlow.collectAsState()
    var darkMode by remember { mutableStateOf(settingsManager.darkMode) }
    var updateChannel by remember { mutableStateOf(settingsManager.updateChannel) }

    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }

    // 滚动方向检测（不在 derivedStateOf 内变更状态，避免触发无限重组）
    var isScrollingUp by remember { mutableStateOf(false) }
    var previousScrollValue by remember { mutableIntStateOf(0) }

    // 滚动到顶/底部震感
    var hasHapticAtTop by remember { mutableStateOf(false) }
    var hasHapticAtBottom by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        val currentValue = scrollState.value
        isScrollingUp = currentValue <= previousScrollValue
        previousScrollValue = currentValue
        onScrollStateChanged(isScrollingUp)

        // 滚动到顶/底部震感
        val maxValue = scrollState.maxValue

        if (currentValue == 0 && !hasHapticAtTop) {
            haptic.perform(HapticFeedbackType.TextHandleMove)
            hasHapticAtTop = true
            hasHapticAtBottom = false
        } else if (maxValue > 0 && currentValue >= maxValue && !hasHapticAtBottom) {
            haptic.perform(HapticFeedbackType.TextHandleMove)
            hasHapticAtBottom = true
            hasHapticAtTop = false
        } else if (currentValue > 0 && currentValue < maxValue) {
            hasHapticAtTop = false
            hasHapticAtBottom = false
        }
    }

    // 版本更新功能已暂停
    // var isChecking by remember { mutableStateOf(false) }
    // var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    // var checkError by remember { mutableStateOf<String?>(null) }
    // var lastCheckTime by remember { mutableStateOf<Long?>(null) }

    // 下载进度相关状态
    // var downloadId by remember { mutableStateOf<Long>(-1L) }
    // var downloadProgress by remember { mutableIntStateOf(0) }
    // var isDownloading by remember { mutableStateOf(false) }

    // val checkFailedText = stringResource(R.string.version_check_failed)

    // 版本更新检查 - 功能已暂停
    // var showUpdateDialog by remember { mutableStateOf(false) }

    // 监听下载进度 - 功能已暂停
    /*
    LaunchedEffect(isDownloading, downloadId) {
        if (isDownloading && downloadId != -1L) {
            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
            while (isActive && isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (bytesTotal > 0) {
                        downloadProgress = (bytesDownloaded * 100 / bytesTotal)
                    }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                    }
                }
                cursor.close()
                delay(500)
            }
        }
    }
    */

    // Dialogs
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                haptic.perform(HapticFeedbackType.LongPress)
                settingsManager.currentTheme = theme
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDarkModeDialog) {
        DarkModeDialog(
            currentMode = darkMode,
            onModeSelected = { mode ->
                haptic.perform(HapticFeedbackType.LongPress)
                settingsManager.darkMode = mode
                darkMode = mode
                showDarkModeDialog = false
            },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    if (showChannelDialog) {
        UpdateChannelDialog(
            currentChannel = updateChannel,
            onChannelSelected = { channel ->
                haptic.perform(HapticFeedbackType.LongPress)
                settingsManager.updateChannel = channel
                updateChannel = channel
                showChannelDialog = false
            },
            onDismiss = { showChannelDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.about_title),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Info Card - Logo + Version (版本更新功能已暂停)
            AppInfoCard(
                currentVersionName = currentVersionName,
                currentTheme = currentTheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Settings List Card
            SettingsListCard(
                currentTheme = currentTheme,
                darkMode = darkMode,
                updateChannel = updateChannel,
                onThemeClick = { showThemeDialog = true },
                onDarkModeClick = { showDarkModeDialog = true },
                onUpdateChannelClick = { showChannelDialog = true },
                onNotificationClick = onNavigateToNotificationSettings,
                onPresetSourceClick = onNavigateToPresetSourceManager,
                onPrivacyClick = onNavigateToPrivacy,
                onTermsClick = onNavigateToTerms
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Info Footer
            DeveloperFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppInfoCard(
    currentVersionName: String,
    currentTheme: BrandTheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            currentTheme.primaryColor.copy(alpha = 0.2f),
                            currentTheme.primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    currentTheme.primaryColor,
                                    currentTheme.primaryColor.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Name
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = currentTheme.primaryColor)) {
                            append("O")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                            append("Master")
                        }
                    },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // App Slogan
                Text(
                    text = stringResource(R.string.app_slogan),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version Badge - 显示当前版本和构建类型
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.version_format, currentVersionName),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primaryColor)
                    )
                    // 根据版本号判断显示"最新版"或"开发版"
                    val isReleaseVersion = !currentVersionName.contains("dev") &&
                                          !currentVersionName.contains("alpha") &&
                                          !currentVersionName.contains("beta")
                    Text(
                        text = if (isReleaseVersion)
                            stringResource(R.string.version_latest_badge)
                        else
                            "开发版",
                        fontSize = 12.sp,
                        color = currentTheme.primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsListCard(
    currentTheme: BrandTheme,
    darkMode: DarkMode,
    updateChannel: UpdateChannel,
    onThemeClick: () -> Unit,
    onDarkModeClick: () -> Unit,
    onUpdateChannelClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onPresetSourceClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    val settingsItems = listOf(
        SettingsItem(
            icon = Icons.Default.Palette,
            label = stringResource(R.string.settings_theme_title),
            value = stringResource(currentTheme.brandNameResId),
            onClick = onThemeClick
        ),
        SettingsItem(
            icon = when (darkMode) {
                DarkMode.LIGHT -> Icons.Default.WbSunny
                DarkMode.DARK -> Icons.Default.DarkMode
                else -> Icons.Default.Brush
            },
            label = stringResource(R.string.dark_mode_title),
            value = when (darkMode) {
                DarkMode.SYSTEM -> stringResource(R.string.dark_mode_system)
                DarkMode.LIGHT -> stringResource(R.string.dark_mode_light)
                DarkMode.DARK -> stringResource(R.string.dark_mode_dark)
            },
            onClick = onDarkModeClick
        ),
        SettingsItem(
            icon = Icons.Default.Language,
            label = stringResource(R.string.update_channel_title),
            value = when (updateChannel) {
                UpdateChannel.GITEE -> stringResource(R.string.update_channel_gitee)
                UpdateChannel.GITHUB -> stringResource(R.string.update_channel_github)
            },
            onClick = onUpdateChannelClick
        ),
        SettingsItem(
            icon = Icons.Default.Notifications,
            label = stringResource(R.string.notification_settings_title),
            value = "",
            onClick = onNotificationClick
        ),
        SettingsItem(
            icon = Icons.Default.Storage,
            label = stringResource(R.string.preset_source_title),
            value = "",
            onClick = onPresetSourceClick
        ),
        SettingsItem(
            icon = Icons.Default.Security,
            label = stringResource(R.string.privacy_policy_title),
            value = "",
            onClick = onPrivacyClick
        ),
        SettingsItem(
            icon = Icons.Default.Description,
            label = stringResource(R.string.user_agreement_title),
            value = "",
            onClick = onTermsClick
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            settingsItems.forEachIndexed { index, item ->
                SettingsListItem(
                    item = item,
                    showDivider = index < settingsItems.size - 1
                )
            }
        }
    }
}

private data class SettingsItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsListItem(
    item: SettingsItem,
    showDivider: Boolean
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.perform(HapticFeedbackType.TextHandleMove)
                item.onClick()
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = item.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.value.isNotEmpty()) {
                Text(
                    text = item.value,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
    
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        )
    }
}

@Composable
private fun DeveloperFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.developer_footer),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        
        Text(
            text = stringResource(R.string.copyright_footer),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
    }
}