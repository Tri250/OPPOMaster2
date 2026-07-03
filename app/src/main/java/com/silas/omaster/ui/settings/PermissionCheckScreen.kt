package com.silas.omaster.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.silas.omaster.R
import com.silas.omaster.data.local.PermissionState
import com.silas.omaster.data.local.PermissionStatus
import com.silas.omaster.util.PermissionChecker
import com.silas.omaster.util.PermissionKey
import kotlinx.coroutines.launch

/**
 * 2.2.0 新增：权限自检页面
 *
 * 显示应用所有运行时权限的当前状态：
 *  - 已授予：绿色 ✓
 *  - 被拒绝：红色 ✗，并提供"去开启"按钮
 *  - 不需要：灰色（低版本系统）
 *
 * 重新进入页面或点击"刷新"按钮会重新检查权限。
 * 从系统设置返回后自动刷新（通过 Lifecycle 监听）。
 */
@Composable
fun PermissionCheckScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState by PermissionChecker.permissionState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 进入页面和从设置返回时刷新
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                PermissionChecker.refresh(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 构造权限项（按重要程度排序）
    val items = remember(permissionState) {
        buildPermissionItems(permissionState)
    }

    val grantedCount = items.count { it.state.isGranted }
    val totalCount = items.count { it.state.status != PermissionStatus.NOT_REQUIRED }
    val progress = if (totalCount > 0) grantedCount.toFloat() / totalCount else 1f
    val allGranted = grantedCount == totalCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "权限自检",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (!isRefreshing) {
                        isRefreshing = true
                        coroutineScope.launch {
                            try {
                                PermissionChecker.refresh(context)
                            } finally {
                                isRefreshing = false
                            }
                        }
                    }
                }
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 概览卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (allGranted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (allGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allGranted) "所有权限已就绪" else "部分权限待开启",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "已授予 $grantedCount / $totalCount 项必要权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
                if (!allGranted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 权限列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = "权限详情",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(items) { index, item ->
                PermissionItemCard(
                    item = item,
                    onRequest = { action -> handlePermissionAction(context, item.key, action) }
                )
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                HelpFooter()
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 权限项数据
 */
private data class PermissionItem(
    val key: PermissionKey,
    val state: PermissionState,
    val icon: ImageVector,
    val description: String,
    val required: Boolean
)

/**
 * 构造权限项（按重要程度排序）
 */
private fun buildPermissionItems(
    state: Map<PermissionKey, PermissionStatus>
): List<PermissionItem> {
    val keys = listOf(
        PermissionKey.SYSTEM_ALERT_WINDOW to (Icons.Default.Window to "用于显示预设参数悬浮窗，可在相机界面实时查看调参效果"),
        PermissionKey.POST_NOTIFICATIONS to (Icons.Default.Notifications to "Android 13+ 必需，用于接收更新提醒和悬浮窗通知"),
        PermissionKey.CAMERA to (Icons.Default.Camera to "实时取景器、AI 场景识别、相机调参助手的核心功能"),
        PermissionKey.RECORD_AUDIO to (Icons.Default.Mic to "CameraX 视频录制模式及语音快门所需"),
        PermissionKey.ACCESS_FINE_LOCATION to (Icons.Default.LocationOn to "地标识别 / GPS 场景增强，可记录拍摄位置"),
        PermissionKey.READ_MEDIA_IMAGES to (Icons.Default.Image to "读取相册图片用于 AI 智能分析和预设套用"),
        PermissionKey.REQUEST_INSTALL_PACKAGES to (Icons.Default.OpenInBrowser to "应用内更新功能，检查更新后安装 APK 所需"),
        PermissionKey.INTERNET to (Icons.Default.Wifi to "网络通信，已默认授予"),
        PermissionKey.ACCESS_NETWORK_STATE to (Icons.Default.Wifi to "检测网络状态，已默认授予")
    )

    return keys.map { (key, info) ->
        val (icon, desc) = info
        val status = state[key] ?: PermissionStatus.NOT_REQUIRED
        PermissionItem(
            key = key,
            state = PermissionState(
                key = key.name,
                displayName = PermissionChecker.getPermissionDisplayName(key),
                status = status,
                isRequired = key != PermissionKey.INTERNET &&
                    key != PermissionKey.ACCESS_NETWORK_STATE,
                description = desc
            ),
            icon = icon,
            description = desc,
            required = key != PermissionKey.INTERNET &&
                key != PermissionKey.ACCESS_NETWORK_STATE
        )
    }
}

/**
 * 权限操作类型
 */
private enum class PermissionAction {
    /** 跳转到系统应用设置 */
    OPEN_SETTINGS,
    /** 跳转到悬浮窗权限设置 */
    OPEN_OVERLAY_SETTINGS,
    /** 跳转到应用安装权限设置 */
    OPEN_INSTALL_SETTINGS
}

/**
 * 处理权限操作（跳转到对应系统设置）
 */
private fun handlePermissionAction(
    context: Context,
    key: PermissionKey,
    action: PermissionAction
) {
    try {
        val intent = when (action) {
            PermissionAction.OPEN_SETTINGS -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            PermissionAction.OPEN_OVERLAY_SETTINGS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            PermissionAction.OPEN_INSTALL_SETTINGS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("PermissionCheckScreen", "跳转到权限设置失败", e)
    }
}

/**
 * 单个权限项卡片
 */
@Composable
private fun PermissionItemCard(
    item: PermissionItem,
    onRequest: (PermissionAction) -> Unit
) {
    val (statusColor, statusText, statusIcon) = when (item.state.status) {
        PermissionStatus.GRANTED -> Triple(
            MaterialTheme.colorScheme.primary,
            "已授予",
            Icons.Default.CheckCircle
        )
        PermissionStatus.DENIED -> Triple(
            MaterialTheme.colorScheme.error,
            "未授予",
            Icons.Default.Error
        )
        PermissionStatus.NOT_REQUIRED -> Triple(
            Color.Gray,
            "不需要",
            Icons.Default.Info
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            // 名称和描述
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.state.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.required) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "必需",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // 状态 + 操作
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.state.status == PermissionStatus.DENIED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val action = when (item.key) {
                                PermissionKey.SYSTEM_ALERT_WINDOW -> PermissionAction.OPEN_OVERLAY_SETTINGS
                                PermissionKey.REQUEST_INSTALL_PACKAGES -> PermissionAction.OPEN_INSTALL_SETTINGS
                                else -> PermissionAction.OPEN_SETTINGS
                            }
                            onRequest(action)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = "去开启",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部帮助说明
 */
@Composable
private fun HelpFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "为什么需要这些权限？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "OMaster 是相机调参工具，权限仅用于实现对应功能，不会上传任何数据。\n" +
                    "若您拒绝授予，仍可使用本地预设管理和导入导出功能。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
