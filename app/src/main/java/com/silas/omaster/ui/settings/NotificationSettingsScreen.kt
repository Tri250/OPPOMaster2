package com.silas.omaster.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.perform
import com.silas.omaster.util.performToggle

/**
 * 通知设置页面
 */
private const val CHANNEL_GENERAL = "omaster_general"
private const val CHANNEL_RECOMMENDATION = "omaster_recommendation"
private const val CHANNEL_SYNC = "omaster_sync"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val notificationManager = remember {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val notificationManagerCompat = remember { NotificationManagerCompat.from(context) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Android 13+ 通知权限
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != null) {
            ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
        } else true
    }

    fun createOrUpdateChannel(channelId: String, name: String, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (enabled) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_NONE
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = name
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    var masterEnabled by remember { mutableStateOf(notificationManagerCompat.areNotificationsEnabled()) }

    var generalEnabled by remember { mutableStateOf(isChannelEnabled(CHANNEL_GENERAL)) }
    var recommendationEnabled by remember { mutableStateOf(isChannelEnabled(CHANNEL_RECOMMENDATION)) }
    var syncEnabled by remember { mutableStateOf(isChannelEnabled(CHANNEL_SYNC)) }
    var systemAnnounceEnabled by remember { mutableStateOf(isChannelEnabled(CHANNEL_GENERAL)) }
    var dailyTipEnabled by remember { mutableStateOf(isChannelEnabled(CHANNEL_RECOMMENDATION)) }

    // P2-5：dndEnabled 提升到顶层并持久化到 SettingsManager，
    // 避免之前在 item {} 内局部声明导致退出页面即丢失
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var dndEnabled by remember { mutableStateOf(settingsManager.isDndEnabled) }

    // 首次进入：若未授权，请求权限
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != null) {
            if (!hasNotificationPermission()) {
                // 不在 compose 内部直接请求，权限请求由用户点击总开关触发
            }
        }
        // 确保各渠道已创建（默认开启）
        createOrUpdateChannel(CHANNEL_GENERAL, "通用通知", generalEnabled)
        createOrUpdateChannel(CHANNEL_RECOMMENDATION, "推荐与提示", recommendationEnabled)
        createOrUpdateChannel(CHANNEL_SYNC, "云同步提醒", syncEnabled)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        masterEnabled = granted
        if (!granted) {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            })
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("通知设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 通知总开关
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = HasselbladOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "接收推送通知",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "开启后接收重要通知",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Switch(
                            checked = masterEnabled,
                            onCheckedChange = { enabled ->
                                haptic.performToggle(enabled)
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermission != null) {
                                        if (hasNotificationPermission()) {
                                            masterEnabled = true
                                        } else {
                                            permissionLauncher.launch(notificationPermission)
                                        }
                                    } else {
                                        masterEnabled = true
                                    }
                                } else {
                                    openNotificationSettings()
                                    masterEnabled = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                checkedTrackColor = HasselbladOrange,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // 通知类型列表
            item {
                Text(
                    text = "通知类型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val enabled = generalEnabled
                NotificationSettingCard(
                    title = "功能更新通知",
                    description = "接收新功能和更新提醒",
                    isEnabled = enabled,
                    onToggle = { newValue ->
                        haptic.performToggle(newValue)
                        if (!masterEnabled) {
                            openNotificationSettings()
                        } else {
                            generalEnabled = newValue
                            systemAnnounceEnabled = newValue
                            createOrUpdateChannel(CHANNEL_GENERAL, "通用通知", newValue)
                        }
                    }
                )
            }

            item {
                NotificationSettingCard(
                    title = "预设推荐",
                    description = "接收个性化预设推荐",
                    isEnabled = recommendationEnabled,
                    onToggle = { newValue ->
                        haptic.performToggle(newValue)
                        if (!masterEnabled) {
                            openNotificationSettings()
                        } else {
                            recommendationEnabled = newValue
                            dailyTipEnabled = newValue
                            createOrUpdateChannel(CHANNEL_RECOMMENDATION, "推荐与提示", newValue)
                        }
                    }
                )
            }

            item {
                NotificationSettingCard(
                    title = "云同步提醒",
                    description = "同步状态变更通知",
                    isEnabled = syncEnabled,
                    onToggle = { newValue ->
                        haptic.performToggle(newValue)
                        if (!masterEnabled) {
                            openNotificationSettings()
                        } else {
                            syncEnabled = newValue
                            createOrUpdateChannel(CHANNEL_SYNC, "云同步提醒", newValue)
                        }
                    }
                )
            }

            item {
                NotificationSettingCard(
                    title = "系统公告",
                    description = "重要系统公告通知",
                    isEnabled = systemAnnounceEnabled,
                    onToggle = { newValue ->
                        haptic.performToggle(newValue)
                        if (!masterEnabled) {
                            openNotificationSettings()
                        } else {
                            systemAnnounceEnabled = newValue
                            generalEnabled = newValue
                            createOrUpdateChannel(CHANNEL_GENERAL, "通用通知", newValue)
                        }
                    }
                )
            }

            item {
                NotificationSettingCard(
                    title = "每日提示",
                    description = "摄影技巧每日提示",
                    isEnabled = dailyTipEnabled,
                    onToggle = { newValue ->
                        haptic.performToggle(newValue)
                        if (!masterEnabled) {
                            openNotificationSettings()
                        } else {
                            dailyTipEnabled = newValue
                            recommendationEnabled = newValue
                            createOrUpdateChannel(CHANNEL_RECOMMENDATION, "推荐与提示", newValue)
                        }
                    }
                )
            }

            // 免打扰设置（P2-5：dndEnabled 状态已提升至顶层并持久化到 SettingsManager）
            item {
                Text(
                    text = "免打扰设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    // P2-5：原硬编码 Color(0xFF1A1A1A) 在浅色模式下造成深底+深文字低对比，
                    // 改为 surfaceVariant，与 UpdateChannelScreen 等其他设置卡片保持一致
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DoNotDisturb,
                                    null,
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "夜间免打扰",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Switch(
                                checked = dndEnabled,
                                onCheckedChange = {
                                    haptic.performToggle(it)
                                    dndEnabled = it
                                    settingsManager.isDndEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                    checkedTrackColor = Color(0xFF9C27B0),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "免打扰时段",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "22:00 - 08:00",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun NotificationSettingCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        // P2-5：原硬编码 Color(0xFF1A1A1A) 改为 surfaceVariant 适配深/浅模式
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                    checkedTrackColor = HasselbladOrange,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
    }
}
