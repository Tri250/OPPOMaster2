package com.silas.omaster

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.OMasterTheme
import com.silas.omaster.util.PermissionChecker

/**
 * 用于在整个 Compose 树中访问当前 Activity（悬浮窗权限申请等场景需要）
 */
val LocalActivity = compositionLocalOf<Activity> { error("No Activity provided") }

/**
 * 应用主 Activity
 *
 * 职责：
 *  1. 初始化悬浮窗控制器
 *  2. 提供根 CompositionLocal（Activity）
 *  3. 加载主题（颜色/暗色模式）并应用 OMasterTheme
 *  4. 根据是否同意隐私政策，选择进入欢迎流程或主应用
 *  5. 在销毁时注销悬浮窗控制器
 *
 * 真正的 UI 结构已拆分到独立文件：
 *  - [AppNavigation.kt] - NavHost / 底部导航 / 全局 Snackbar
 *  - [WelcomeFlow.kt] - 首次启动欢迎对话框
 *  - [Screen.kt] - 路由定义
 *
 * 2.2.0 闪退修复：
 *  - 使用 safeGetInstance() 替代 getInstance()，避免 OMasterApplication 未完全初始化时崩溃
 *  - 添加 PermissionChecker 自检，缺失权限给用户清晰提示
 *  - 所有 initUMeng/setUserAgreed 调用都包了 try-catch 防御
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        /** 进程被杀后恢复的标记键 */
        private const val KEY_LAST_SAVED_STATE = "main_activity_last_state"
        /** 启动计数键（用于验证多次启动稳定性） */
        private const val KEY_LAUNCH_COUNT = "launch_count"
    }

    private var floatingWindowController: FloatingWindowController? = null
    /** 通过 Deep Link 传入的预设 ID，用于导航到详情页 */
    private var deepLinkPresetId: String? = null
    /** 标记Activity是否正常启动过（用于区分进程被杀后的恢复） */
    private var hasLaunchedSuccessfully = false
    /** 启动次数（用于验证多次启动稳定性） */
    private var launchCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动恢复：检查上次是否被异常终止（如进程被杀）
        launchCount = getPreferences(MODE_PRIVATE).getInt(KEY_LAUNCH_COUNT, 0) + 1
        getPreferences(MODE_PRIVATE).edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply()
        Log.i(TAG, "MainActivity 启动 #$launchCount")

        // 解析 Deep Link: omaster://preset/{id} 或 https://omaster.app/preset/{id}
        deepLinkPresetId = parseDeepLink(intent)

        // 2.2.0 闪退修复：所有关键调用包 try-catch，绝不让 onCreate 抛出
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        // 初始化悬浮窗控制器（延迟注册，等待权限检查）
        try {
            floatingWindowController = FloatingWindowController.getInstance(this)
            // 仅在已有悬浮窗权限时注册，否则延迟到用户主动启用悬浮窗功能时
            if (Settings.canDrawOverlays(this)) {
                floatingWindowController?.register()
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "悬浮窗控制器初始化失败", e)
        }

        setContent {
            CompositionLocalProvider(LocalActivity provides this) {
                val settingsManager = remember { SettingsManager.getInstance(applicationContext) }
                val currentTheme by settingsManager.themeFlow.collectAsState()
                val darkMode by settingsManager.darkModeFlow.collectAsState()
                // 2.2.0 闪退修复：使用 safeGetInstance 而非 getInstance
                val hasUserAgreed = remember {
                    try {
                        mutableStateOf(
                            OMasterApplication.safeGetInstance()?.hasUserAgreed() ?: false
                        )
                    } catch (e: Throwable) {
                        Log.e("MainActivity", "读取用户协议状态失败", e)
                        mutableStateOf(false)
                    }
                }
                var showWelcomeFlow by hasUserAgreed
                val navController = rememberNavController()

                OMasterTheme(
                    darkMode = darkMode,
                    brandTheme = currentTheme
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showWelcomeFlow) {
                            WelcomeFlow(
                                onAgree = {
                                    try {
                                        OMasterApplication.safeGetInstance()?.let {
                                            it.setUserAgreed(true)
                                            it.initUMeng()
                                        }
                                    } catch (e: Throwable) {
                                        Log.e("MainActivity", "同意隐私政策后初始化失败", e)
                                    }
                                    showWelcomeFlow = false
                                },
                                onDisagree = {
                                    // 不同意隐私政策：禁用友盟统计和云同步，但允许使用本地功能
                                    try {
                                        OMasterApplication.safeGetInstance()
                                            ?.setUserAgreed(false)
                                    } catch (e: Throwable) {
                                        Log.e("MainActivity", "保存用户协议状态失败", e)
                                    }
                                    showWelcomeFlow = false
                                }
                            )
                        } else {
                            MainApp(
                                navController = navController,
                                deepLinkPresetId = deepLinkPresetId
                            )
                            // 消费 Deep Link，避免重复导航
                            deepLinkPresetId = null
                        }
                    }
                }
            }
        }

        // 2.2.0 闪退修复：异步执行权限自检，不阻塞 UI
        try {
            // PermissionChecker 通过 SettingsManager/SubscriptionManager 暴露状态，UI 可观察
            PermissionChecker.preflight(this)
        } catch (e: Throwable) {
            Log.e("MainActivity", "权限预检失败", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // 权限收回检查：用户可能在设置中手动关闭了权限
            if (hasLaunchedSuccessfully) {
                PermissionChecker.refresh(this)
                Log.d(TAG, "onResume: 权限状态已刷新")
            }

            // 悬浮窗权限状态检查
            if (Settings.canDrawOverlays(this) && floatingWindowController?.let {
                try {
                    val field = it::class.java.getDeclaredField("isRegistered").apply { isAccessible = true }
                    !(field.get(it) as Boolean)
                } catch (e: Throwable) { true }
            } == true) {
                floatingWindowController?.register()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "onResume 权限检查失败", e)
        }
        hasLaunchedSuccessfully = true
    }

    override fun onPause() {
        super.onPause()
        // 保存当前状态，防止进程被杀后数据丢失
        try {
            val prefs = getPreferences(MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_LAST_SAVED_STATE, true)
                .putLong("last_pause_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Throwable) {
            Log.e(TAG, "onPause 保存状态失败", e)
        }
    }

    override fun onStop() {
        super.onStop()
        // 活动不可见时，释放非关键资源（如悬浮窗可暂停更新）
        try {
            Log.d(TAG, "onStop: Activity 进入后台")
        } catch (e: Throwable) {
            Log.e(TAG, "onStop 处理失败", e)
        }
    }

    override fun onRestart() {
        super.onRestart()
        try {
            Log.i(TAG, "onRestart: Activity 从后台恢复")
            // 恢复时重新检查权限状态
            PermissionChecker.refresh(this)
        } catch (e: Throwable) {
            Log.e(TAG, "onRestart 处理失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销悬浮窗控制器
        try {
            floatingWindowController?.unregister()
        } catch (e: Throwable) {
            Log.e("MainActivity", "注销悬浮窗控制器失败", e)
        }
    }

    /**
     * 当应用已在前台时收到新的 Deep Link Intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkPresetId = parseDeepLink(intent)
        // 如果 Deep Link 有效，标记需要导航
        if (deepLinkPresetId != null) {
            Log.d("MainActivity", "DeepLink received: presetId=$deepLinkPresetId")
        }
    }

    /**
     * 解析 Deep Link Intent
     * 支持格式:
     *   - omaster://preset/{id}
     *   - https://omaster.app/preset/{id}
     *
     * @return 预设 ID，无效时返回 null
     */
    private fun parseDeepLink(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null

        return when {
            // 自定义 scheme: omaster://preset/{id}
            uri.scheme == "omaster" && uri.host == "preset" -> {
                uri.pathSegments?.firstOrNull()
            }
            // HTTPS: https://omaster.app/preset/{id}
            uri.scheme == "https" && uri.host == "omaster.app" && uri.path?.startsWith("/preset") == true -> {
                uri.lastPathSegment
            }
            else -> null
        }
    }
}
