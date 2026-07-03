package com.silas.omaster.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.silas.omaster.R
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.infrastructure.utils.perform
import kotlinx.coroutines.launch

/**
 * 权限引导页数据
 */
data class PermissionGuidePage(
    val icon: ImageVector,
    val titleResId: Int,
    val descriptionResId: Int,
    val permissionKey: String,
    val isOptional: Boolean = false
)

/**
 * 权限引导流程
 * 
 * FeatureGuideFlow完成后显示，引导用户授予必要权限
 * 使用Pager动画切换页面
 * 
 * 权限项：
 * 1. 相机权限（必需）
 * 2. 存储权限（必需）
 * 3. 通知权限（可选，Android 13+）
 * 4. 悬浮窗权限（可选）
 */
@Composable
fun PermissionGuideFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 权限状态
    var cameraGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.CAMERA)) }
    var storageGranted by remember { mutableStateOf(checkStoragePermission(context)) }
    var notificationGranted by remember { mutableStateOf(checkNotificationPermission(context)) }
    var overlayGranted by remember { mutableStateOf(checkOverlayPermission(context)) }

    // 从设置返回时刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                cameraGranted = checkPermission(context, Manifest.permission.CAMERA)
                storageGranted = checkStoragePermission(context)
                notificationGranted = checkNotificationPermission(context)
                overlayGranted = checkOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pages = listOf(
        // 页面1: 相机权限
        PermissionGuidePage(
            icon = Icons.Default.CameraAlt,
            titleResId = R.string.permission_camera_title,
            descriptionResId = R.string.permission_camera_desc,
            permissionKey = Manifest.permission.CAMERA,
            isOptional = false
        ),
        // 页面2: 存储权限
        PermissionGuidePage(
            icon = Icons.Default.Image,
            titleResId = R.string.permission_storage_title,
            descriptionResId = R.string.permission_storage_desc,
            permissionKey = "storage",
            isOptional = false
        ),
        // 页面3: 通知权限
        PermissionGuidePage(
            icon = Icons.Default.Notifications,
            titleResId = R.string.permission_notification_title,
            descriptionResId = R.string.permission_notification_desc,
            permissionKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                Manifest.permission.POST_NOTIFICATIONS else "notification",
            isOptional = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ),
        // 页面4: 悬浮窗权限
        PermissionGuidePage(
            icon = Icons.Default.Window,
            titleResId = R.string.permission_overlay_title,
            descriptionResId = R.string.permission_overlay_desc,
            permissionKey = "overlay",
            isOptional = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // 权限请求 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 刷新权限状态
        cameraGranted = checkPermission(context, Manifest.permission.CAMERA)
        storageGranted = checkStoragePermission(context)
        notificationGranted = checkNotificationPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 跳过按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.permission_guide_skip),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val permissionPage = pages[page]
            val isGranted = when (page) {
                0 -> cameraGranted
                1 -> storageGranted
                2 -> notificationGranted
                3 -> overlayGranted
                else -> false
            }

            PermissionGuidePageContent(
                page = permissionPage,
                isGranted = isGranted,
                onRequestPermission = {
                    haptic.perform(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    when (page) {
                        0 -> permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        1 -> requestStoragePermission(context, permissionLauncher)
                        2 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                        }
                        3 -> openOverlaySettings(context)
                    }
                },
                onOpenSettings = {
                    haptic.perform(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    openAppSettings(context)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部指示器 + 按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) HasselbladOrange else Color.Gray.copy(alpha = 0.3f),
                        animationSpec = tween(300),
                        label = "indicator_color"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 下一步/完成按钮
            Button(
                onClick = {
                    haptic.perform(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) 
                        stringResource(R.string.permission_guide_next) 
                    else 
                        stringResource(R.string.permission_guide_complete),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 权限引导页内容
 */
@Composable
private fun PermissionGuidePageContent(
    page: PermissionGuidePage,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) HasselbladOrange.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = if (isGranted) HasselbladOrange 
                       else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 标题
        Text(
            text = stringResource(page.titleResId),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = stringResource(page.descriptionResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 状态标签
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (page.isOptional) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(R.string.permission_optional),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HasselbladOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.permission_required),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = HasselbladOrange
                    )
                }
            }
            
            // 授权状态
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGranted) HasselbladOrange.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isGranted) HasselbladOrange 
                               else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (isGranted) stringResource(R.string.permission_granted)
                               else stringResource(R.string.permission_denied),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isGranted) HasselbladOrange 
                               else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 操作按钮
        if (!isGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text(
                        text = stringResource(R.string.permission_request),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.permission_settings),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 检查权限是否已授予
 */
private fun checkPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * 检查存储权限
 */
private fun checkStoragePermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 检查通知权限
 */
private fun checkNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Android 13 以下通知权限默认授予
    }
}

/**
 * 检查悬浮窗权限
 */
private fun checkOverlayPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

/**
 * 请求存储权限
 */
private fun requestStoragePermission(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
    } else {
        launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }
}

/**
 * 打开悬浮窗权限设置
 */
private fun openOverlaySettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * 打开应用设置页面
 */
private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}