package com.silas.omaster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.util.PermissionChecker

/**
 * 闪屏页 - 处理应用冷启动流程
 *
 * 职责：
 * 1. 显示品牌启动画面，提升用户体验
 * 2. 提前预加载必要资源（不阻塞主线程）
 * 3. 处理启动超时保护：防止无限加载导致ANR
 * 4. 处理低存储空间检查
 * 5. 权限预处理：提前告知用户需要哪些权限
 * 6. 启动超时自动跳转到主界面，不会卡住
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val MIN_SPLASH_DISPLAY_MS = 800L // 最小显示时间，保证品牌曝光
        private const val MAX_STARTUP_TIMEOUT_MS = 10000L // 最大启动超时，防止卡住
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isFinished = false

    // 权限请求结果回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限请求完成后，刷新权限状态并继续启动
        try {
            PermissionChecker.refresh(this)
            val deniedCount = permissions.values.count { !it }
            if (deniedCount > 0) {
                Log.i(TAG, "$deniedCount 项权限被拒绝，继续启动")
            } else {
                Log.i(TAG, "所有请求的权限已授予")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "权限处理失败", e)
        }
        // 无论权限是否授予，都继续启动
        completeSplash()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 使用闪屏主题设置窗口背景，避免启动白屏
        setTheme(R.style.Theme_OMaster_Splash)
        super.onCreate(savedInstanceState)

        // 设置闪屏布局
        try {
            setContentView(R.layout.activity_splash)
        } catch (e: Throwable) {
            Log.e(TAG, "加载闪屏布局失败，降级为白屏", e)
            // 降级：使用空的 content view，不阻塞启动
        }

        // 设置启动超时保护：如果10秒内还没完成，强制跳转防止ANR
        mainHandler.postDelayed({
            if (!isFinished) {
                Log.w(TAG, "启动超时，强制跳转到主界面")
                completeSplash()
            }
        }, MAX_STARTUP_TIMEOUT_MS)

        // 检查存储空间
        if (!checkStorageSpace()) {
            Log.w(TAG, "剩余存储空间不足")
        }

        // 请求必要的运行时权限（Android 13+）
        requestRequiredPermissions()

        // 预加载关键数据（在后台进行）
        preloadCriticalData()

        // 保证最小显示时间后再跳转
        mainHandler.postDelayed({
            if (!isFinished) {
                completeSplash()
            }
        }, MIN_SPLASH_DISPLAY_MS)
    }

    /**
     * 检查剩余存储空间
     * @return true 空间足够，false 空间不足
     */
    private fun checkStorageSpace(): Boolean {
        return try {
            val freeBytes = filesDir.freeSpace
            val freeMB = freeBytes / (1024 * 1024)
            Log.i(TAG, "剩余内部存储空间: ${freeMB}MB")
            // 要求至少 500MB 空闲空间
            freeMB >= 500
        } catch (e: Throwable) {
            Log.e(TAG, "检查存储空间失败", e)
            true // 检查失败时默认允许继续
        }
    }

    /**
     * 请求应用必需的运行时权限
     * 根据Android版本动态请求所需权限
     */
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 13+ 需要媒体读取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        // 相机权限（核心功能必需，但可以在使用时再申请，这里只提前请求）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // 录音权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // 如果有需要请求的权限，发起请求
        if (permissionsToRequest.isNotEmpty() && !isFinished) {
            Log.i(TAG, "请求 ${permissionsToRequest.size} 项权限")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // 没有需要请求的权限，直接继续
            if (!isFinished) {
                mainHandler.postDelayed({
                    if (!isFinished) {
                        completeSplash()
                    }
                }, MIN_SPLASH_DISPLAY_MS)
            }
        }
    }

    /**
     * 预加载关键数据
     * 在后台线程进行，不阻塞闪屏显示
     */
    private fun preloadCriticalData() {
        Thread {
            try {
                // 预加载 SharedPreferences（已经在 InitializationProvider 初始化过）
                OMasterApplication.safeGetInstance()?.let { app ->
                    // 触发 SettingsManager 缓存预热
                    try {
                        SettingsManager.getInstance(app).preloadCache()
                        Log.d(TAG, "SettingsManager 预加载完成")
                    } catch (e: Throwable) {
                        Log.w(TAG, "SettingsManager 预加载失败", e)
                    }
                }
            } catch (e: Throwable) {
                // 预加载失败不影响启动，只是性能稍微差一点
                Log.w(TAG, "预加载失败", e)
            }
        }.start()
    }

    /**
     * 完成闪屏流程，跳转到主Activity
     */
    private fun completeSplash() {
        if (isFinished) return
        isFinished = true

        try {
            val intent = Intent(this, MainActivity::class.java)
            // 将原始Intent的数据（如DeepLink）传递过去
            intent.replaceExtras(intent)
            startActivity(intent)
            // 使用淡入淡出过渡动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (e: Throwable) {
            Log.e(TAG, "跳转到MainActivity失败", e)
            // 极端情况：启动MainActivity失败，尝试再次启动
            try {
                val retryIntent = Intent(this, MainActivity::class.java)
                startActivity(retryIntent)
                finish()
            } catch (e2: Throwable) {
                Log.e(TAG, "重试启动MainActivity失败，应用将退出", e2)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除所有待处理的回调，防止内存泄漏
        mainHandler.removeCallbacksAndMessages(null)
    }
}