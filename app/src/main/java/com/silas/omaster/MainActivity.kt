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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.silas.omaster.data.local.FeatureGuideManager
import com.silas.omaster.data.local.PermissionGuideManager
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.navigation.MainApp
import com.silas.omaster.ui.onboarding.FeatureGuideFlow
import com.silas.omaster.ui.onboarding.PermissionGuideFlow
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.components.CrashRecoveryDialog
import com.silas.omaster.ui.theme.OMasterTheme
import com.silas.omaster.infrastructure.utils.PermissionChecker
import com.silas.omaster.infrastructure.utils.VersionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    // Deep Link 状态管理：使用 StateFlow 确保跨线程安全访问和 Compose 响应式更新
    private val _deepLinkPresetId = MutableStateFlow<String?>(null)
    val deepLinkPresetIdFlow: StateFlow<String?> = _deepLinkPresetId

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 SplashScreen，必须在 super.onCreate() 之前调用
        // Android 12+ 使用系统 SplashScreen，低版本使用兼容库实现
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 设置 SplashScreen 退出条件：Compose 渲染完成后退出
        // 使用 setKeepOnScreenCondition 确保内容准备好后再退出
        splashScreen.setKeepOnScreenCondition {
            // 返回 false 表示可以退出 SplashScreen
            // Compose 渲染是异步的，setContent 后第一帧会自动触发退出
            false
        }

        // 解析 Deep Link: omaster://preset/{id} 或 https://omaster.app/preset/{id}
        _deepLinkPresetId.value = parseDeepLink(intent)

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

        // v2.3.6 关键修复：setContent 整体 try-catch，Compose 渲染异常降级到空白 Surface
        try {
            setContent {
                CompositionLocalProvider(LocalActivity provides this) {
                    val settingsManager = remember { SettingsManager.getInstance(applicationContext) }
                    val currentTheme by settingsManager.themeFlow.collectAsState()
                    val darkMode by settingsManager.darkModeFlow.collectAsState()
                    
                    // 引导流程管理器
                    val featureGuideManager = remember { FeatureGuideManager.getInstance(applicationContext) }
                    val permissionGuideManager = remember { PermissionGuideManager.getInstance(applicationContext) }
                    val versionCode = VersionInfo.VERSION_CODE.toLong()
                    
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

                    // v2.3.6 崩溃恢复：检查上次崩溃标记
                    val hadCrashLastRun = remember {
                        try {
                            mutableStateOf(
                                OMasterApplication.safeGetInstance()?.hadCrashLastRun() ?: false
                            )
                        } catch (e: Throwable) {
                            Log.e("MainActivity", "读取崩溃标记失败", e)
                            mutableStateOf(false)
                        }
                    }
                    var showCrashRecovery by hadCrashLastRun

                    // Deep Link 状态：从 StateFlow 收集，确保引导流程完成后正确处理
                    val deepLinkPresetId by deepLinkPresetIdFlow.collectAsState()

                    // 功能引导流程状态
                    val shouldShowFeatureGuide = remember {
                        featureGuideManager.shouldShowFeatureGuide(versionCode)
                    }
                    var showFeatureGuideFlow by remember { mutableStateOf(shouldShowFeatureGuide && !showWelcomeFlow) }

                    // 权限引导流程状态
                    val shouldShowPermissionGuide = remember {
                        permissionGuideManager.shouldShowPermissionGuide(versionCode)
                    }
                    var showPermissionGuideFlow by remember { mutableStateOf(shouldShowPermissionGuide && !showWelcomeFlow && !showFeatureGuideFlow) }

                    val navController = rememberNavController()

                    OMasterTheme(
                        darkMode = darkMode,
                        brandTheme = currentTheme
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // v2.3.6 崩溃恢复：引导流程优先级调整
                            // CrashRecoveryDialog -> WelcomeFlow -> FeatureGuideFlow -> PermissionGuideFlow -> MainApp
                            when {
                                // 0. 首先检查是否需要显示崩溃恢复对话框（最高优先级）
                                showCrashRecovery -> {
                                    CrashRecoveryDialog(
                                        onIgnore = {
                                            showCrashRecovery = false
                                            // 忽略后继续正常引导流程
                                            if (!showWelcomeFlow) {
                                                if (featureGuideManager.shouldShowFeatureGuide(versionCode)) {
                                                    showFeatureGuideFlow = true
                                                } else if (permissionGuideManager.shouldShowPermissionGuide(versionCode)) {
                                                    showPermissionGuideFlow = true
                                                }
                                            }
                                        },
                                        onClearCacheAndRestart = {
                                            // 此选项会在 CrashRecoveryDialog 内部重启应用
                                            // 不需要额外处理
                                        }
                                    )
                                }

                                // 1. 其次显示隐私政策欢迎页
                                showWelcomeFlow -> {
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
                                            // 检查是否需要显示功能引导
                                            if (featureGuideManager.shouldShowFeatureGuide(versionCode)) {
                                                showFeatureGuideFlow = true
                                            } else if (permissionGuideManager.shouldShowPermissionGuide(versionCode)) {
                                                showPermissionGuideFlow = true
                                            }
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
                                            // 不同意也进入引导流程
                                            if (featureGuideManager.shouldShowFeatureGuide(versionCode)) {
                                                showFeatureGuideFlow = true
                                            } else if (permissionGuideManager.shouldShowPermissionGuide(versionCode)) {
                                                showPermissionGuideFlow = true
                                            }
                                        }
                                    )
                                }
                                
                                // 2. 显示功能引导流程（5页）
                                showFeatureGuideFlow -> {
                                    FeatureGuideFlow(
                                        onComplete = {
                                            featureGuideManager.markFeatureGuideShown(versionCode)
                                            showFeatureGuideFlow = false
                                            // 检查是否需要显示权限引导
                                            if (permissionGuideManager.shouldShowPermissionGuide(versionCode)) {
                                                showPermissionGuideFlow = true
                                            }
                                        },
                                        onSkip = {
                                            featureGuideManager.skipFeatureGuide(versionCode)
                                            showFeatureGuideFlow = false
                                            // 检查是否需要显示权限引导
                                            if (permissionGuideManager.shouldShowPermissionGuide(versionCode)) {
                                                showPermissionGuideFlow = true
                                            }
                                        }
                                    )
                                }
                                
                                // 3. 显示权限引导流程（4页）
                                showPermissionGuideFlow -> {
                                    PermissionGuideFlow(
                                        onComplete = {
                                            permissionGuideManager.markPermissionGuideShown(versionCode)
                                            showPermissionGuideFlow = false
                                        },
                                        onSkip = {
                                            permissionGuideManager.skipPermissionGuide(versionCode)
                                            showPermissionGuideFlow = false
                                        }
                                    )
                                }
                                
                                // 4. 进入主应用
                                else -> {
                                    // Deep Link 处理：引导流程完成后才执行导航
                                    MainApp(
                                        navController = navController,
                                        deepLinkPresetId = deepLinkPresetId
                                    )
                                    // 消费 Deep Link，避免重复导航
                                    if (deepLinkPresetId != null) {
                                        _deepLinkPresetId.value = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // v2.3.6 兜底：setContent 失败时降级为空白 Activity，防止 App 闪退
            Log.e("MainActivity", "setContent 失败，使用降级 UI", e)
            try {
                setContent {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {}
                }
            } catch (e2: Throwable) {
                Log.e("MainActivity", "降级 setContent 也失败，App 将保持空白", e2)
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
                    !it.isRegistered()
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

    /**
     * 当应用已在前台时收到新的 Deep Link Intent
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newDeepLinkPresetId = parseDeepLink(intent)
        // 更新 StateFlow，Compose 会自动响应并导航
        _deepLinkPresetId.value = newDeepLinkPresetId
        if (newDeepLinkPresetId != null) {
            Log.d("MainActivity", "DeepLink received in onNewIntent: presetId=$newDeepLinkPresetId")
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