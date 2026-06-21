package com.silas.omaster.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.util.HapticSettings
import com.silas.omaster.util.ImageCacheManager
import com.silas.omaster.util.UrlConstants
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun SettingsScreen(
    onNavigateToNotificationSettings: (() -> Unit)? = null,
    onNavigateToTerms: (() -> Unit)? = null,
    onNavigateToPresetSourceManager: (() -> Unit)? = null,
    onNavigateToUpdateChannel: (() -> Unit)? = null,
    onNavigateToApiConfig: (() -> Unit)? = null,
    onNavigateToThemeSettings: (() -> Unit)? = null,
    onNavigateToSceneAnalysisReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var vibrationEnabled by remember { mutableStateOf(settingsManager.isVibrationEnabled) }
    val currentTheme by settingsManager.themeFlow.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTabDialog by remember { mutableStateOf(false) }
    var floatingWindowOpacity by remember { mutableStateOf(settingsManager.floatingWindowOpacity) }
    var defaultStartTab by remember { mutableStateOf(settingsManager.defaultStartTab) }
    var updateChannel by remember { mutableStateOf(settingsManager.updateChannel) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var analyticsEnabled by remember { mutableStateOf(settingsManager.isAnalyticsEnabled) }
    var cacheSize by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        cacheSize = ImageCacheManager.getInstance(context).getCacheSize(context)
    }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // 新增设置项
    var darkMode by remember { mutableStateOf(settingsManager.darkMode) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var cloudSyncEnabled by remember { mutableStateOf(settingsManager.isCloudSyncEnabled) }
    var lastSyncTime by remember { mutableStateOf(settingsManager.lastSyncTime) }
    var showDataSourceDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    val presetRepository = remember { PresetRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

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

    if (showTabDialog) {
        TabSelectionDialog(
            currentTab = defaultStartTab,
            onTabSelected = { tab ->
                haptic.perform(HapticFeedbackType.LongPress)
                defaultStartTab = tab
                settingsManager.defaultStartTab = tab
                showTabDialog = false
            },
            onDismiss = { showTabDialog = false }
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

    if (showDataSourceDialog) {
        DataSourceDetailsDialog(onDismiss = { showDataSourceDialog = false })
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.settings_title),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        // General Section
        SettingsSectionCard {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_general))

            // Vibration Setting
            SettingsSwitchItem(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.vibration_feedback),
                checked = vibrationEnabled,
                onCheckedChange = { enabled ->
                    vibrationEnabled = enabled
                    settingsManager.isVibrationEnabled = enabled
                    HapticSettings.enabled = enabled
                    if (enabled) {
                        haptic.perform(HapticFeedbackType.LongPress)
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            // Default Start Tab Setting
            SettingsClickableItem(
                icon = Icons.Default.DashboardCustomize,
                title = stringResource(R.string.default_start_tab),
                subtitle = getTabName(defaultStartTab),
                onClick = { showTabDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        SettingsSectionCard {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))

            // Dark Mode Setting
            SettingsClickableItem(
                icon = when (darkMode) {
                    DarkMode.LIGHT -> Icons.Default.WbSunny
                    DarkMode.DARK -> Icons.Default.DarkMode
                    else -> Icons.Default.Brush
                },
                title = "深色模式",
                subtitle = when (darkMode) {
                    DarkMode.SYSTEM -> "跟随系统"
                    DarkMode.LIGHT -> "浅色模式"
                    DarkMode.DARK -> "深色模式"
                },
                onClick = { showDarkModeDialog = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            // Theme Setting
            SettingsClickableItem(
                icon = Icons.Default.Brush,
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(currentTheme.brandNameResId),
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(currentTheme.primaryColor)
                    )
                },
                onClick = { onNavigateToThemeSettings?.invoke() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Window Section
        SettingsSectionCard {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_floating_window))

            // Floating Window Opacity Setting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.floating_window_opacity),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$floatingWindowOpacity%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                var previousOpacity by remember { mutableStateOf(floatingWindowOpacity) }
                Slider(
                    value = floatingWindowOpacity.toFloat(),
                    onValueChange = {
                        val newValue = it.toInt()
                        floatingWindowOpacity = newValue
                        settingsManager.floatingWindowOpacity = newValue
                        if (newValue != previousOpacity) {
                            haptic.perform(HapticFeedbackType.TextHandleMove)
                            previousOpacity = newValue
                        }
                    },
                    valueRange = 30f..70f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "30%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "70%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cloud Sync Section
        SettingsSectionCard {
            SettingsSectionTitle(title = "云同步")

            SettingsSwitchItem(
                icon = Icons.Default.Sync,
                title = "启用云同步",
                subtitle = "从CDN同步最新预设数据",
                checked = cloudSyncEnabled,
                onCheckedChange = { enabled ->
                    cloudSyncEnabled = enabled
                    settingsManager.isCloudSyncEnabled = enabled
                    haptic.perform(HapticFeedbackType.LongPress)
                }
            )

            if (cloudSyncEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                SettingsClickableItem(
                    icon = Icons.Default.Cloud,
                    title = "同步数据源",
                    subtitle = "OPPO、realme、vivo、honor",
                    onClick = {
                        // 显示数据源详情对话框
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        showDataSourceDialog = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                val lastSyncText = if (lastSyncTime > 0) {
                    val diff = System.currentTimeMillis() - lastSyncTime
                    val hours = diff / (1000 * 60 * 60)
                    when {
                        hours < 1 -> "刚刚"
                        hours < 24 -> "${hours}小时前"
                        else -> "${hours / 24}天前"
                    }
                } else "从未同步"

                SettingsClickableItem(
                    icon = Icons.Default.Update,
                    title = "上次同步",
                    subtitle = lastSyncText,
                    onClick = {
                        // 手动触发同步
                        if (!isSyncing) {
                            haptic.perform(HapticFeedbackType.LongPress)
                            isSyncing = true
                            coroutineScope.launch {
                                val result = presetRepository.syncFromCloud()
                                isSyncing = false
                                result.onSuccess {
                                    lastSyncTime = System.currentTimeMillis()
                                    settingsManager.lastSyncTime = lastSyncTime
                                    Toast.makeText(context, "同步成功", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "同步失败：${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Update Section
        SettingsSectionCard {
            SettingsSectionTitle(title = "更新设置")

            SettingsClickableItem(
                icon = Icons.Default.Update,
                title = "更新方式",
                subtitle = when (updateChannel) {
                    UpdateChannel.GITEE -> "Gitee（国内推荐）"
                    UpdateChannel.GITHUB -> "GitHub（国际）"
                },
                onClick = { showChannelDialog = true }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            SettingsClickableItem(
                icon = Icons.Default.Download,
                title = "更新渠道",
                subtitle = "稳定版 / 测试版 / 开发版",
                onClick = { onNavigateToUpdateChannel?.invoke() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Section
        SettingsSectionCard {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_privacy))

            SettingsSwitchItem(
                icon = Icons.Default.Analytics,
                title = stringResource(R.string.analytics_enabled),
                subtitle = stringResource(R.string.analytics_restart_hint),
                checked = analyticsEnabled,
                onCheckedChange = { enabled ->
                    analyticsEnabled = enabled
                    settingsManager.isAnalyticsEnabled = enabled
                    haptic.perform(HapticFeedbackType.LongPress)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            SettingsClickableItem(
                icon = Icons.Default.Notifications,
                title = "通知设置",
                subtitle = "管理推送通知和提醒",
                onClick = { onNavigateToNotificationSettings?.invoke() }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            SettingsClickableItem(
                icon = Icons.Default.Storage,
                title = "预设源管理",
                subtitle = "管理云端预设源",
                onClick = { onNavigateToPresetSourceManager?.invoke() }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            SettingsClickableItem(
                icon = Icons.Default.Description,
                title = "用户协议",
                subtitle = "查看服务条款和隐私政策",
                onClick = { onNavigateToTerms?.invoke() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cache Section
        SettingsSectionCard {
            SettingsSectionTitle(title = "存储")

            SettingsClickableItem(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.clear_cache),
                subtitle = if (cacheSize > 0) {
                    stringResource(R.string.cache_size_format, cacheSize)
                } else {
                    stringResource(R.string.clear_cache_desc)
                },
                onClick = { showClearCacheDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced Section
        SettingsSectionCard {
            SettingsSectionTitle(title = stringResource(R.string.settings_section_advanced))

            SettingsClickableItem(
                icon = Icons.Default.SettingsEthernet,
                title = stringResource(R.string.api_config_title),
                subtitle = stringResource(R.string.api_config_desc),
                onClick = { onNavigateToApiConfig?.invoke() }
            )
        }

        // Clear Cache Confirmation Dialog
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text(stringResource(R.string.clear_cache)) },
                text = {
                    Text(
                        if (cacheSize > 0) {
                            "当前缓存大小为 %.2f MB，清理后将释放存储空间，下次浏览时需要重新下载图片。".format(cacheSize)
                        } else {
                            "当前没有缓存图片"
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            ImageCacheManager.getInstance(context).clearCache(context)
                            cacheSize = 0.0
                            showClearCacheDialog = false
                            haptic.perform(HapticFeedbackType.LongPress)
                        },
                        enabled = cacheSize > 0
                    ) {
                        Text("清理")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                textContentColor = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = trailingContent ?: {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

private fun getTabName(tabIndex: Int): String {
    return when (tabIndex) {
        0 -> "发现"
        1 -> "收藏"
        2 -> "哈苏"
        3 -> "上新"
        else -> "发现"
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: BrandTheme,
    onThemeSelected: (BrandTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_theme_dialog_title))
        },
        text = {
            LazyColumn {
                items(BrandTheme.entries) { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = theme.primaryColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(theme.primaryColor)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = stringResource(theme.brandNameResId),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(theme.colorNameResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

/**
 * 同步数据源详情对话框
 */
@Composable
private fun DataSourceDetailsDialog(onDismiss: () -> Unit) {
    val dataSources = listOf(
        DataSourceInfo("OPPO", "一加/OPPO 大师模式官方预设", UrlConstants.PRESET_OPPO),
        DataSourceInfo("realme", "realme GT 大师模式官方预设", UrlConstants.PRESET_REALME),
        DataSourceInfo("vivo", "vivo 蔡司自然色彩官方预设", UrlConstants.PRESET_VIVO),
        DataSourceInfo("honor", "荣耀 Magic 影像官方预设", UrlConstants.PRESET_HONOR)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("同步数据源") },
        text = {
            LazyColumn {
                items(dataSources) { source ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

private data class DataSourceInfo(
    val name: String,
    val description: String,
    val url: String
)

@Composable
fun TabSelectionDialog(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val tabs = listOf("发现" to 0, "收藏" to 1, "哈苏" to 2, "上新" to 3)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择默认启动页面")
        },
        text = {
            LazyColumn {
                items(tabs) { (name, index) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTabSelected(index) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == currentTab),
                            onClick = { onTabSelected(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun UpdateChannelDialog(
    currentChannel: UpdateChannel,
    onChannelSelected: (UpdateChannel) -> Unit,
    onDismiss: () -> Unit
) {
    val channels = listOf(
        UpdateChannel.GITEE to "Gitee（国内推荐）",
        UpdateChannel.GITHUB to "GitHub（国际）"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择更新方式")
        },
        text = {
            LazyColumn {
                items(channels) { (channel, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChannelSelected(channel) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (channel == currentChannel),
                            onClick = { onChannelSelected(channel) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when (channel) {
                                    UpdateChannel.GITEE -> "国内访问速度快"
                                    UpdateChannel.GITHUB -> "国际访问，国内可能需要代理"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun DarkModeDialog(
    currentMode: DarkMode,
    onModeSelected: (DarkMode) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        DarkMode.SYSTEM to "跟随系统" to "根据系统设置自动切换",
        DarkMode.LIGHT to "浅色模式" to "始终使用浅色主题",
        DarkMode.DARK to "深色模式" to "始终使用深色主题"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择深色模式")
        },
        text = {
            LazyColumn {
                items(modes) { (pair, desc) ->
                    val (mode, name) = pair
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = { onModeSelected(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}
