package com.silas.omaster

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.OMasterTheme

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
 */
class MainActivity : ComponentActivity() {

    private lateinit var floatingWindowController: FloatingWindowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化并注册全局悬浮窗控制器
        floatingWindowController = FloatingWindowController.getInstance(this)
        floatingWindowController.register()

        setContent {
            CompositionLocalProvider(LocalActivity provides this) {
                val settingsManager = remember { SettingsManager.getInstance(applicationContext) }
                val currentTheme by settingsManager.themeFlow.collectAsState()
                val darkMode by settingsManager.darkModeFlow.collectAsState()
                val hasUserAgreed = remember {
                    mutableStateOf(OMasterApplication.getInstance().hasUserAgreed())
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
                                navController = navController,
                                onAgree = {
                                    OMasterApplication.getInstance().setUserAgreed(true)
                                    showWelcomeFlow = false
                                },
                                onDisagree = {
                                    finish()
                                }
                            )
                        } else {
                            MainApp(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销悬浮窗控制器
        floatingWindowController.unregister()
    }
}
