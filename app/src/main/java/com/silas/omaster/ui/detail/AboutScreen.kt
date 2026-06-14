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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Globe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.ui.settings.ThemeSelectionDialog
import com.silas.omaster.ui.settings.DarkModeDialog
import com.silas.omaster.ui.settings.UpdateChannelDialog
import com.silas.omaster.util.UpdateChecker
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

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var lastCheckTime by remember { mutableStateOf<Long?>(null) }

    // 下载进度相关状态
    var downloadId by remember { mutableStateOf<Long>(-1L) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    val checkFailedText = stringResource(R.string.version_check_failed)

    LaunchedEffect(Unit) {
        delay(500)
        if (updateInfo == null && checkError == null) {
            isChecking = true
            checkError = null
            try {
                val result = UpdateChecker.checkUpdate(context, currentVersionCode, updateChannel)
                if (result != null) {
                    updateInfo = result
                    lastCheckTime = System.currentTimeMillis()
                } else {
                    checkError = checkFailedText
                }
            } catch (e: Exception) {
                checkError = e.message ?: checkFailedText
            } finally {
                isChecking = false
            }
        }
    }

    val checkForUpdate = {
        scope.launch {
            isChecking = true
            checkError = null
            try {
                val result = UpdateChecker.checkUpdate(context, currentVersionCode, updateChannel)
                if (result != null) {
                    updateInfo = result
                    lastCheckTime = System.currentTimeMillis()
                } else {
                    checkError = checkFailedText
                }
            } catch (e: Exception) {
                checkError = e.message ?: checkFailedText
            } finally {
                isChecking = false
            }
        }
    }

    // 监听下载进度
    LaunchedEffect(isDownloading, downloadId) {
        if (isDownloading && downloadId != -1L) {
            while (isActive) {
                val (status, progress) = UpdateChecker.queryDownloadProgress(context, downloadId)
                downloadProgress = progress
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        isDownloading = false
                        downloadProgress = 100
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        isDownloading = false
                        downloadProgress = -1
                        break
                    }
                }
                
                if (!isDownloading) break
                delay(500)
            }
        }
    }

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
            .background(PureBlack)
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.about_title),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = Color.White
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
            // App Info Card - Logo + Version
            AppInfoCard(
                currentVersionName = currentVersionName,
                currentTheme = currentTheme,
                isChecking = isChecking,
                updateInfo = updateInfo,
                checkError = checkError,
                lastCheckTime = lastCheckTime,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                onCheckClick = { checkForUpdate() },
                onDownloadClick = {
                    updateInfo?.let { info ->
                        downloadId = UpdateChecker.downloadAndInstall(context, info.downloadUrl, info.versionName)
                        isDownloading = true
                        downloadProgress = 0
                    }
                },
                onCancelDownload = {
                    if (downloadId != -1L) {
                        UpdateChecker.cancelDownload(context, downloadId)
                        isDownloading = false
                        downloadProgress = 0
                        downloadId = -1L
                    }
                },
                onRetryClick = {
                    checkError = null
                    checkForUpdate()
                }
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
    currentTheme: BrandTheme,
    isChecking: Boolean,
    updateInfo: UpdateChecker.UpdateInfo?,
    checkError: String?,
    lastCheckTime: Long?,
    isDownloading: Boolean,
    downloadProgress: Int,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryClick: () -> Unit
) {
    val hasUpdate = updateInfo?.isNewer == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGray.copy(alpha = 0.5f)
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
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append("Master")
                        }
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // App Slogan
                Text(
                    text = stringResource(R.string.app_slogan),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.version_format, currentVersionName),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primaryColor)
                    )
                    if (hasUpdate) {
                        Text(
                            text = stringResource(R.string.version_new_available),
                            fontSize = 12.sp,
                            color = currentTheme.primaryColor
                        )
                    } else if (!isChecking && updateInfo != null && !updateInfo.isNewer) {
                        Text(
                            text = stringResource(R.string.version_latest_badge),
                            fontSize = 12.sp,
                            color = currentTheme.primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Update Status Section
                UpdateStatusSection(
                    isChecking = isChecking,
                    updateInfo = updateInfo,
                    checkError = checkError,
                    lastCheckTime = lastCheckTime,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    onCheckClick = onCheckClick,
                    onDownloadClick = onDownloadClick,
                    onCancelDownload = onCancelDownload,
                    onRetryClick = onRetryClick
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusSection(
    isChecking: Boolean,
    updateInfo: UpdateChecker.UpdateInfo?,
    checkError: String?,
    lastCheckTime: Long?,
    isDownloading: Boolean,
    downloadProgress: Int,
    onCheckClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        when {
            isChecking -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.checking),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            updateInfo != null -> {
                if (updateInfo.isNewer) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "v${updateInfo.versionName}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            if (isDownloading) {
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "$downloadProgress%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier.width(120.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        drawStopIndicator = {}
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.cancel),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable { onCancelDownload() }
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onDownloadClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.version_download_btn),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.update_notes_title),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateInfo.releaseNotes,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.version_is_latest),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            checkError != null -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRetryClick() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.version_retry),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckClick() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.version_check),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
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
            icon = Icons.Default.Globe,
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
            containerColor = Color.White.copy(alpha = 0.05f)
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
                color = Color.White
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
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
    
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            color = Color.White.copy(alpha = 0.05f)
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
            color = Color.White.copy(alpha = 0.3f)
        )
        
        Text(
            text = stringResource(R.string.copyright_footer),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.2f)
        )
    }
}