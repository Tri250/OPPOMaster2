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

    private var floatingWindowController: FloatingWindowController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            MainApp(navController = navController)
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
        // 2.2.0 闪退修复：每次回到前台检查悬浮窗权限状态
        try {
            if (Settings.canDrawOverlays(this) && floatingWindowController?.let {
                try {
                    val field = it::class.java.getDeclaredField("isRegistered").apply { isAccessible = true }
                    !(field.get(it) as Boolean)
                } catch (e: Throwable) { true }
            } == true) {
                floatingWindowController?.register()
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "onResume 权限检查失败", e)
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
}
