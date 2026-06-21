package com.silas.omaster.ui.detail

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.util.UpdateChecker
import com.silas.omaster.util.VersionInfo
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

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
    val haptic = LocalHapticFeedback.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme by settingsManager.themeFlow.collectAsState()
    val scope = rememberCoroutineScope()

    // 版本更新检查状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    fun checkForUpdate() {
        if (isCheckingUpdate) return
        haptic.perform(HapticFeedbackType.LongPress)
        isCheckingUpdate = true
        scope.launch {
            val info = UpdateChecker.checkUpdate(context, currentVersionCode, UpdateChannel.GITEE)
            isCheckingUpdate = false
            updateInfo = info
            when {
                info == null -> {
                    Toast.makeText(context, R.string.version_check_failed, Toast.LENGTH_SHORT).show()
                }
                info.isNewer -> {
                    showUpdateDialog = true
                }
                else -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.version_latest),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 滚动方向检测
    var isScrollingUp by remember { mutableStateOf(false) }
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var hasHapticAtTop by remember { mutableStateOf(false) }
    var hasHapticAtBottom by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        val currentValue = scrollState.value
        isScrollingUp = currentValue <= previousScrollValue
        previousScrollValue = currentValue
        onScrollStateChanged(isScrollingUp)

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
            // App Info Card - Logo + Version
            AppInfoCard(
                currentVersionName = currentVersionName,
                currentTheme = currentTheme,
                isCheckingUpdate = isCheckingUpdate,
                onCheckUpdate = ::checkForUpdate
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions Card - 精简版，仅保留关键入口
            QuickActionsCard(
                onNavigateToSettings = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToSettings()
                },
                onNavigateToPrivacy = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToPrivacy()
                },
                onNavigateToTerms = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onNavigateToTerms()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Info Footer
            DeveloperFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 版本更新对话框
    if (showUpdateDialog) {
        val info = updateInfo
        if (info != null) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text(text = stringResource(R.string.version_new_available)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.version_new_found, info.versionName),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_notes_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUpdateDialog = false
                            val downloadId = UpdateChecker.downloadAndInstall(
                                context,
                                info.downloadUrl,
                                info.versionName
                            )
                            if (downloadId != -1L) {
                                Toast.makeText(
                                    context,
                                    "开始下载 v${info.versionName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "下载启动失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.version_download_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun AppInfoCard(
    currentVersionName: String,
    currentTheme: BrandTheme,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit
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
                        contentDescription = "应用图标",
                        tint = MaterialTheme.colorScheme.onBackground,
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

                Spacer(modifier = Modifier.height(16.dp))

                // 检查更新按钮
                TextButton(
                    onClick = onCheckUpdate,
                    enabled = !isCheckingUpdate
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = currentTheme.primaryColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.version_checking),
                            color = currentTheme.primaryColor,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.version_check),
                            color = currentTheme.primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToSettings: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            QuickActionItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.settings_title),
                description = "主题、深色模式、通知、预设源等",
                onClick = onNavigateToSettings
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
            )
            QuickActionItem(
                icon = Icons.Default.Security,
                label = stringResource(R.string.privacy_policy_title),
                description = "了解我们如何保护你的数据",
                onClick = onNavigateToPrivacy
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
            )
            QuickActionItem(
                icon = Icons.Default.Description,
                label = stringResource(R.string.user_agreement_title),
                description = "使用条款与用户协议",
                onClick = onNavigateToTerms
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.perform(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "进入",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
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
            text = "开发者：带娃的小陈工",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

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