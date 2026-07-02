package com.silas.omaster.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 应用内更新管理器
 *
 * 使用 Google Play In-App Update API 检查并提示用户更新：
 * - 在 OMasterApplication 启动时异步检查更新
 * - 支持 IMMEDIATE（立即更新）和 FLEXIBLE（灵活更新）两种模式
 * - 处理 InstallException 优雅降级
 * - 更新检查失败不阻塞用户使用
 *
 * 使用方式：
 * 1. 在 Application.onCreate 中调用 InAppUpdateManager.init(this)
 * 2. 在需要检查更新的 Activity 中调用 checkForUpdate(activity)
 */
class InAppUpdateManager private constructor(context: Context) {

    companion object {
        private const val TAG = "InAppUpdateManager"
        const val REQUEST_CODE_UPDATE = 10001

        @Volatile
        private var instance: InAppUpdateManager? = null

        fun getInstance(context: Context): InAppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: InAppUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 在 Application 中初始化并开始检查更新
         */
        fun init(context: Context) {
            val manager = getInstance(context)
            manager.startUpdateCheck()
        }

        /**
         * 处理 onActivityResult 中的更新结果
         */
        fun handleActivityResult(requestCode: Int, resultCode: Int): Boolean {
            if (requestCode != REQUEST_CODE_UPDATE) return false
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.i(TAG, "用户同意更新")
                }
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "用户取消更新")
                }
                else -> {
                    Log.w(TAG, "更新结果未知: $resultCode")
                }
            }
            return true
        }
    }

    private val appContext = context.applicationContext
    // v2.2.6 闪退修复：AppUpdateManagerFactory.create() 在非 GMS 设备（无 Google Play 服务）上
    // 可能抛出异常。使用 try-catch 包裹，失败时置 null，后续方法通过 null 检查安全跳过。
    private val appUpdateManager = try {
        AppUpdateManagerFactory.create(appContext)
    } catch (e: Throwable) {
        Log.e(TAG, "AppUpdateManagerFactory.create 失败（可能是非 GMS 设备）", e)
        null
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var updateAvailable = false
    private var updateType = AppUpdateType.FLEXIBLE
    private var availableVersionCode = 0

    private val installStateListener: InstallStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val progress = (state.bytesDownloaded().toFloat() / state.totalBytesToDownload().toFloat() * 100).toInt()
                Log.d(TAG, "更新下载中: $progress%")
            }
            InstallStatus.DOWNLOADED -> {
                Log.i(TAG, "更新已下载，等待安装")
                // FLEXIBLE 模式：下载完成后，下次启动时自动安装
            }
            InstallStatus.INSTALLING -> {
                Log.i(TAG, "正在安装更新")
            }
            InstallStatus.INSTALLED -> {
                Log.i(TAG, "更新已安装")
                appUpdateManager?.unregisterListener(installStateListener)
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "更新下载失败: errorCode=${state.installErrorCode()}")
                appUpdateManager?.unregisterListener(installStateListener)
            }
            InstallStatus.CANCELED -> {
                Log.i(TAG, "更新已取消")
                appUpdateManager?.unregisterListener(installStateListener)
            }
            else -> { /* PENDING, REQUIRES_UI_CONFIRMATION 等 */ }
        }
    }

    /**
     * 开始检查更新（在后台进行，不阻塞启动）
     */
    fun startUpdateCheck() {
        scope.launch {
            try {
                val mgr = appUpdateManager ?: return@launch
                val appUpdateInfo = mgr.appUpdateInfo.await()

                updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (!updateAvailable) {
                    Log.d(TAG, "没有可用更新")
                    return@launch
                }

                availableVersionCode = appUpdateInfo.availableVersionCode()
                val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                Log.i(TAG, "发现更新: versionCode=$availableVersionCode, " +
                    "immediateAllowed=$isImmediateAllowed, flexibleAllowed=$isFlexibleAllowed")

                updateType = when {
                    isImmediateAllowed -> AppUpdateType.IMMEDIATE
                    isFlexibleAllowed -> AppUpdateType.FLEXIBLE
                    else -> {
                        Log.w(TAG, "无可用更新类型")
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败", e)
            }
        }
    }

    /**
     * 在 Activity 中触发更新流程
     *
     * @param activity 当前 Activity
     * @param type 更新类型（默认使用检查到的类型）
     * @return true 表示已触发更新流程
     */
    fun checkForUpdate(activity: Activity, type: Int? = null): Boolean {
        val updateType = type ?: this.updateType

        if (!updateAvailable) {
            Log.d(TAG, "没有可用更新，跳过")
            return false
        }

        scope.launch {
            try {
                val mgr = appUpdateManager ?: return@launch
                val appUpdateInfo = mgr.appUpdateInfo.await()

                if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                    Log.d(TAG, "更新已不可用")
                    return@launch
                }

                if (!appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                    Log.w(TAG, "更新类型 $updateType 不允许")
                    return@launch
                }

                // 注册安装状态监听器（FLEXIBLE 模式需要）
                if (updateType == AppUpdateType.FLEXIBLE) {
                    mgr.registerListener(installStateListener)
                }

                mgr.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateType,
                    activity,
                    REQUEST_CODE_UPDATE
                )

                Log.i(TAG, "更新流程已启动: type=$updateType")
            } catch (e: com.google.android.play.core.install.InstallException) {
                Log.e(TAG, "启动更新失败: errorCode=${e.errorCode}", e)
            } catch (e: Exception) {
                Log.e(TAG, "启动更新异常", e)
            }
        }

        return true
    }

    /**
     * 触发 FLEXIBLE 更新（后台下载，下次启动安装）
     */
    fun startFlexibleUpdate(activity: Activity): Boolean {
        return checkForUpdate(activity, AppUpdateType.FLEXIBLE)
    }

    /**
     * 触发 IMMEDIATE 更新（强制用户立即更新）
     */
    fun startImmediateUpdate(activity: Activity): Boolean {
        return checkForUpdate(activity, AppUpdateType.IMMEDIATE)
    }

    /**
     * 检查是否有待安装的更新（FLEXIBLE 模式下载完成后）
     */
    fun completeFlexibleUpdate() {
        scope.launch {
            try {
                val mgr = appUpdateManager ?: return@launch
                val appUpdateInfo = mgr.appUpdateInfo.await()
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    Log.i(TAG, "完成灵活更新安装")
                    mgr.completeUpdate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "完成灵活更新失败", e)
            }
        }
    }

    /**
     * 是否检测到可用更新
     */
    fun isUpdateAvailable(): Boolean = updateAvailable

    /**
     * 获取可用更新的版本号
     */
    fun getAvailableVersionCode(): Int = availableVersionCode

    /**
     * 取消安装状态监听器
     */
    fun unregisterListener() {
        try {
            appUpdateManager?.unregisterListener(installStateListener)
        } catch (_: Exception) {}
    }
}