package com.silas.omaster.infrastructure.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.silas.omaster.data.local.PermissionState
import com.silas.omaster.data.local.PermissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 2.2.0 新增：权限自检工具
 *
 * 集中管理应用所有运行时权限的检查、申请引导和状态报告。
 * 用于：
 *  1. 启动时预检（不阻塞 UI）
 *  2. 用户进入特定功能时按需检查
 *  3. 设置页面集中展示权限状态
 *  4. 修复安装后首次启动可能因权限缺失引发的闪退
 *
 * 设计原则：
 *  - 权限申请和检查分离，避免循环等待
 *  - 所有检查方法永不抛出异常，返回安全状态
 *  - 缺失权限时不阻塞主流程，仅记录到全局状态
 */
object PermissionChecker {

    private const val TAG = "PermissionChecker"

    // 全局权限状态（用于 UI 观察）
    private val _permissionState = MutableStateFlow<Map<PermissionKey, PermissionStatus>>(emptyMap())
    val permissionState: StateFlow<Map<PermissionKey, PermissionStatus>> = _permissionState.asStateFlow()

    // 是否有 overlay 权限（用于 UI 快捷判断）
    val hasOverlayPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 仅在初始化后才有效
            _cachedOverlay ?: false
        } else true
    private var _cachedOverlay: Boolean? = null

    /**
     * 应用启动时预检（不阻塞 UI）
     * 调用时机：MainActivity.onCreate 之后
     */
    fun preflight(context: Context) {
        try {
            val ctx = context.applicationContext
            val result = mutableMapOf<PermissionKey, PermissionStatus>()

            // 1. 基础权限
            checkBasicPermissions(ctx).forEach { (k, v) -> result[k] = v }

            // 2. Android 13+ 通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result[PermissionKey.POST_NOTIFICATIONS] =
                    if (isGranted(ctx, Manifest.permission.POST_NOTIFICATIONS))
                        PermissionStatus.GRANTED
                    else
                        PermissionStatus.DENIED
            } else {
                result[PermissionKey.POST_NOTIFICATIONS] = PermissionStatus.NOT_REQUIRED
            }

            // 3. 悬浮窗权限
            val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(ctx)
            } else true
            _cachedOverlay = overlayGranted
            result[PermissionKey.SYSTEM_ALERT_WINDOW] =
                if (overlayGranted) PermissionStatus.GRANTED
                else PermissionStatus.DENIED

            // 4. 安装权限（Android 8+ 必需）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                result[PermissionKey.REQUEST_INSTALL_PACKAGES] =
                    if (ctx.packageManager.canRequestPackageInstalls())
                        PermissionStatus.GRANTED
                    else PermissionStatus.DENIED
            } else {
                result[PermissionKey.REQUEST_INSTALL_PACKAGES] = PermissionStatus.NOT_REQUIRED
            }

            // 5. 媒体权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 细粒度媒体权限
                result[PermissionKey.READ_MEDIA_IMAGES] =
                    if (isGranted(ctx, Manifest.permission.READ_MEDIA_IMAGES))
                        PermissionStatus.GRANTED
                    else PermissionStatus.DENIED
            } else {
                result[PermissionKey.READ_MEDIA_IMAGES] = PermissionStatus.NOT_REQUIRED
            }

            // 6. 相机/麦克风/位置（核心相机功能）
            result[PermissionKey.CAMERA] =
                if (isGranted(ctx, Manifest.permission.CAMERA))
                    PermissionStatus.GRANTED
                else PermissionStatus.DENIED
            result[PermissionKey.RECORD_AUDIO] =
                if (isGranted(ctx, Manifest.permission.RECORD_AUDIO))
                    PermissionStatus.GRANTED
                else PermissionStatus.DENIED
            result[PermissionKey.ACCESS_FINE_LOCATION] =
                if (isGranted(ctx, Manifest.permission.ACCESS_FINE_LOCATION))
                    PermissionStatus.GRANTED
                else PermissionStatus.DENIED

            _permissionState.value = result

            val denied = result.filter { it.value == PermissionStatus.DENIED }
            if (denied.isNotEmpty()) {
                Log.i(TAG, "预检完成，${denied.size} 项权限缺失: ${denied.keys.joinToString()}")
            } else {
                Log.i(TAG, "预检完成，所有权限已授予")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "权限预检失败", e)
        }
    }

    /**
     * 启动前台服务（悬浮窗）前的前置权限检查
     * @return true 允许启动；false 权限不足
     */
    fun canStartFloatingService(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Log.w(TAG, "悬浮窗权限缺失，无法启动服务")
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !isGranted(context, Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
            ) {
                Log.w(TAG, "前台服务特殊用途权限缺失")
                // 部分 OEM 系统即便未声明也允许
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "检查悬浮窗权限失败", e)
            false
        }
    }

    /**
     * 启动相机前的前置权限检查
     */
    fun canStartCamera(context: Context): Boolean {
        return try {
            isGranted(context, Manifest.permission.CAMERA)
        } catch (e: Throwable) {
            Log.e(TAG, "检查相机权限失败", e)
            false
        }
    }

    /**
     * 读取媒体前的前置权限检查
     */
    fun canReadMedia(context: Context): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ||
                        isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                else -> true // 旧版本由 READ_EXTERNAL_STORAGE 处理
            }
        } catch (e: Throwable) {
            Log.e(TAG, "检查媒体权限失败", e)
            false
        }
    }

    /**
     * 检查基础权限（已安装即可使用）
     */
    private fun checkBasicPermissions(context: Context): Map<PermissionKey, PermissionStatus> {
        val result = mutableMapOf<PermissionKey, PermissionStatus>()
        // INTERNET、ACCESS_NETWORK_STATE 是普通权限，安装即获得，标记为 NOT_REQUIRED
        result[PermissionKey.INTERNET] = PermissionStatus.NOT_REQUIRED
        result[PermissionKey.ACCESS_NETWORK_STATE] = PermissionStatus.NOT_REQUIRED
        return result
    }

    private fun isGranted(context: Context, permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            Log.e(TAG, "检查权限 $permission 失败", e)
            false
        }
    }

    /**
     * 重新刷新权限状态（用户在系统设置中修改权限后调用）
     */
    fun refresh(context: Context) {
        preflight(context)
    }

    /**
     * 获取缺失权限的友好名称（用于 UI 提示）
     */
    fun getPermissionDisplayName(key: PermissionKey): String = when (key) {
        PermissionKey.POST_NOTIFICATIONS -> "通知权限"
        PermissionKey.SYSTEM_ALERT_WINDOW -> "悬浮窗权限"
        PermissionKey.REQUEST_INSTALL_PACKAGES -> "应用安装权限"
        PermissionKey.READ_MEDIA_IMAGES -> "媒体读取权限"
        PermissionKey.CAMERA -> "相机权限"
        PermissionKey.RECORD_AUDIO -> "麦克风权限"
        PermissionKey.ACCESS_FINE_LOCATION -> "位置权限"
        PermissionKey.INTERNET -> "网络权限"
        PermissionKey.ACCESS_NETWORK_STATE -> "网络状态权限"
    }
}

/**
 * 权限标识（用于状态映射）
 */
enum class PermissionKey {
    INTERNET,
    ACCESS_NETWORK_STATE,
    POST_NOTIFICATIONS,
    SYSTEM_ALERT_WINDOW,
    REQUEST_INSTALL_PACKAGES,
    READ_MEDIA_IMAGES,
    CAMERA,
    RECORD_AUDIO,
    ACCESS_FINE_LOCATION
}
