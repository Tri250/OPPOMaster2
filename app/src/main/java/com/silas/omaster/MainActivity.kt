package com.silas.omaster

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.navigation.Screen
import com.silas.omaster.ui.components.PillNavBar
import com.silas.omaster.ui.components.WelcomeDialog
import com.silas.omaster.ui.create.PresetSelectionScreen
import com.silas.omaster.ui.create.UniversalCreatePresetScreen
import com.silas.omaster.ui.create.UniversalCreatePresetViewModel
import com.silas.omaster.ui.create.UniversalCreatePresetViewModelFactory
import com.silas.omaster.ui.discover.DiscoverScreen
import com.silas.omaster.ui.discover.ColorWalkScreen
import com.silas.omaster.ui.frame.PhotoFrameScreen
import com.silas.omaster.ui.detail.ProfileScreen
import com.silas.omaster.ui.detail.DetailScreen
import com.silas.omaster.ui.detail.OpenSourceLicenseScreen
import com.silas.omaster.ui.detail.PrivacyPolicyScreen
import com.silas.omaster.ui.home.HomeScreen
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.OMasterTheme
import com.silas.omaster.util.JsonUtil
import com.silas.omaster.util.LanguageUtil
import com.silas.omaster.util.VersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.network.PresetRemoteManager

import androidx.compose.runtime.collectAsState
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.local.HolidayGreeting
import com.silas.omaster.data.local.HolidayGreetingManager
import com.silas.omaster.ui.components.HolidayGreetingDialog
import com.silas.omaster.ui.settings.SettingsScreen
import com.silas.omaster.util.UpdateChecker
import kotlinx.coroutines.delay


val LocalActivity = compositionLocalOf<Activity> { error("No Activity provided") }

private const val SURVEY_URL = "https://www.wjx.cn/vm/your_survey_id.aspx"

// 【v1.6.12】ColorOS 17 水生动效：低刚度弹簧曲线，回弹细腻不夸张（对齐系统转场手感）
private val fluidSlideSpec = spring<IntOffset>(dampingRatio = 0.92f, stiffness = 380f)
private val fluidFadeSpec = spring<Float>(dampingRatio = 0.92f, stiffness = 380f)

class MainActivity : ComponentActivity() {

    private lateinit var floatingWindowController: FloatingWindowController

    /**
     * 【v1.6.9 兼容性】Android 13+ 通知权限请求器。
     * 悬浮窗前台服务的常驻通知依赖 POST_NOTIFICATIONS；未授权时系统静默丢弃通知
     * （前台服务仍可运行，但用户看不到悬浮窗控制入口）。在用户同意隐私政策后请求一次。
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { /* 结果不改变业务逻辑：未授权时前台服务优雅降级 */ }

    // FD-01c：系统分享接入 —— 外部分享的预设文本，导入完成后置空
    private var pendingSharedText by androidx.compose.runtime.mutableStateOf<String?>(null)

    // 【v1.6.12】ColorOS 17 侧边键快捷方式路由 —— 导航完成后置空（消费一次）
    internal var pendingShortcutAction by androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context?) {
        // 在 Activity 创建前应用语言设置
        // 【v1.6 启动闪退诊断】语言资源重建在极少数 ROM / 混淆边界下可能抛异常，
        // 该点位于 UI 出现前，一旦抛出即冷启动闪退；失败时回退原始 Context，保证启动不中断。
        val context = try {
            newBase?.let { LanguageUtil.applyLanguage(it) }
        } catch (e: Throwable) {
            android.util.Log.e("OMaster", "applyLanguage failed, fallback to base context", e)
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.silas.omaster.util.CrashReporter.mark(this, "M4_ACTIVITY_ONCREATE")

        // FD-01c：接收系统分享的预设 JSON 文本
        if (intent?.action == android.content.Intent.ACTION_SEND) {
            pendingSharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
        }

        // 【v1.6.12】接收侧边键快捷方式路由（Find X10 长按图标：新建预设 / 发现广场）
        if (pendingShortcutAction == null) {
            intent?.getStringExtra(com.silas.omaster.util.ShortcutRegistrar.EXTRA_SHORTCUT_ACTION)?.let {
                pendingShortcutAction = it
            }
        }

        // 初始化并注册全局悬浮窗控制器
        floatingWindowController = FloatingWindowController.getInstance(this)
        floatingWindowController.register()

        setContent {
            CompositionLocalProvider(LocalActivity provides this) {
                val config = remember { ConfigCenter.getInstance(applicationContext) }
                val currentTheme by config.themeFlow.collectAsState()
                val darkMode by config.darkModeFlow.collectAsState()

                // 【v1.6.10】启动诊断：读取上次运行遗留的崩溃报告 / 死亡轨迹，
                // 以弹窗形式在 App 内直接展示并支持一键复制/分享，无需文件管理器或 adb。
                var pendingDiagnostics by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    com.silas.omaster.util.CrashReporter.mark(applicationContext, "M5_COMPOSITION_START")
                    runCatching {
                        pendingDiagnostics = com.silas.omaster.util.CrashReporter.getPendingDiagnostics(applicationContext)
                    }
                }

                // 根据深色模式设置计算实际主题
                val isDarkTheme = when (darkMode) {
                    com.silas.omaster.data.local.DarkMode.SYSTEM -> {
                        val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                    com.silas.omaster.data.local.DarkMode.LIGHT -> false
                    com.silas.omaster.data.local.DarkMode.DARK -> true
                }

                OMasterTheme(
                    darkTheme = isDarkTheme,
                    brandTheme = currentTheme
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        var showWelcomeFlow by remember { mutableStateOf(!OMasterApplication.getInstance().hasUserAgreed()) }

                        if (showWelcomeFlow) {
                            WelcomeFlow(
                                navController = navController,
                                onAgree = {
                                    OMasterApplication.getInstance().setUserAgreed(true)
                                    OMasterApplication.getInstance().initUMeng()
                                    requestNotificationPermissionIfNeeded()
                                    showWelcomeFlow = false
                                },
                                onDisagree = {
                                    finish()
                                }
                            )
                        } else {
                            MainApp(
                                navController = navController,
                                config = config
                            )

                            // FD-01c：外部分享的预设文本直接进入统一导入对话框
                            pendingSharedText?.let { shared ->
                                com.silas.omaster.ui.create.ClipboardImportDialog(
                                    initialText = shared,
                                    onDismiss = { pendingSharedText = null }
                                )
                            }
                        }
                    }
                }

                // 【v1.6.10】启动诊断弹窗（置于主题之外，任何界面状态下都可见）
                pendingDiagnostics?.let { report ->
                    StartupDiagnosticsDialog(
                        report = report,
                        onDismiss = {
                            com.silas.omaster.util.CrashReporter.clearDiagnostics(applicationContext)
                            pendingDiagnostics = null
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 【v1.6.10】UI 已完全可见 → 本次启动成功，清除启动里程碑。
        // 注意：不在此处清 crash.log——诊断弹窗确认后才清除，避免报告丢失。
        com.silas.omaster.util.CrashReporter.mark(this, "M6_UI_RESUMED")
        com.silas.omaster.util.CrashReporter.bootSucceeded(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销悬浮窗控制器
        floatingWindowController.unregister()
    }

    /**
     * 【v1.6.9 兼容性】Android 13+ 请求通知权限（悬浮窗前台服务通知依赖）。
     * 仅在用户同意隐私政策后调用；低版本或无需要时直接跳过，失败静默（前台服务可降级运行）。
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(this, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) return
        runCatching { notificationPermissionLauncher.launch(perm) }
    }
}

/**
 * 【v1.6.10】启动诊断弹窗：展示上次运行崩溃堆栈 / 死亡轨迹。
 * 提供复制与系统分享两个出口，用户无需文件管理器即可把日志发给开发者。
 */
@Composable
fun StartupDiagnosticsDialog(report: String, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("启动诊断报告") },
        text = {
            Column {
                Text(
                    "检测到上次启动异常。以下为诊断详情，请点击「分享日志」发给开发者，以便精确定位问题。",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = report,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    runCatching {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "OMaster 启动诊断报告")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(send, "分享诊断日志"))
                    }
                }
            ) { Text("分享日志") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        runCatching {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("OMaster 诊断", report))
                            android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("复制") }
                TextButton(onClick = onDismiss) { Text("知道了") }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun WelcomeFlow(
    navController: NavHostController,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showOpenSourceLicense by remember { mutableStateOf(false) }

    // 处理系统返回键
    androidx.activity.compose.BackHandler(enabled = showPrivacyPolicy || showOpenSourceLicense) {
        showPrivacyPolicy = false
        showOpenSourceLicense = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showPrivacyPolicy -> {
                PrivacyPolicyScreen(
                    onBack = {
                        showPrivacyPolicy = false
                    }
                )
            }
            showOpenSourceLicense -> {
                OpenSourceLicenseScreen(
                    onBack = {
                        showOpenSourceLicense = false
                    }
                )
            }
            else -> {
                WelcomeDialog(
                    onAgree = onAgree,
                    onDisagree = onDisagree,
                    onViewPrivacyPolicy = {
                        showPrivacyPolicy = true
                    }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    navController: NavHostController,
    config: ConfigCenter
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    // 【v1.6.12】消费侧边键快捷方式路由（Find X10 + ColorOS 17 长按图标直达）
    // 【v1.6.13 编译修复】LocalActivity.current 是 @Composable 调用，必须在组合上下文中读取；
    // v1.6.12 因将其写入 LaunchedEffect lambda 导致 :app:compileDebugKotlin FAILED
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
        val mainActivity = activity as? MainActivity ?: return@LaunchedEffect
        when (mainActivity.pendingShortcutAction) {
            com.silas.omaster.util.ShortcutRegistrar.ACTION_CREATE -> {
                navController.navigate(Screen.PresetSelection)
                mainActivity.pendingShortcutAction = null
            }
            com.silas.omaster.util.ShortcutRegistrar.ACTION_DISCOVER -> {
                navController.navigate(Screen.Discover)
                mainActivity.pendingShortcutAction = null
            }
        }
    }

    // 检查是否需要数据迁移
    LaunchedEffect(Unit) {
        // 先触发 loadPresets 来正确检测版本
        // 使用 IO 线程避免阻塞 UI
        withContext(Dispatchers.IO) {
            JsonUtil.loadPresets(context)
        }
        
        // 现在 currentPresetsVersion 已经被正确设置
        // 支持 version 2 和 3，只有旧版本 (version 1) 才需要迁移
        if (JsonUtil.currentPresetsVersion == 1) {
            showMigrationDialog = true
        }
    }

    if (showMigrationDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to decide */ },
            title = { Text("数据结构更新") },
            text = { Text("检测到预设数据版本过旧，需要迁移数据以支持新功能。\n\n点击“迁移数据”将重置内置预设（您的自定义预设和收藏不会丢失）。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        JsonUtil.deleteRemotePresets(context)
                        repository.reloadDefaultPresets()
                        showMigrationDialog = false
                    }
                ) {
                    Text("迁移数据")
                }
            },
            dismissButton = {
                // Optional: Allow user to cancel and exit app?
                // Or maybe just hide dialog and let them use potentially broken app?
                // Given the request "check if version field exists and value is 2, otherwise pop up prompt",
                // usually implies mandatory action.
                // But for safety/UX, maybe allow cancel?
                // If cancel, showMigrationDialog = false, but app might crash later if structure mismatch.
                // Let's stick to mandatory for now or just allow dismiss.
                // I'll leave dismissButton empty to force "Migrate" or back button (which onDismissRequest handles if we implemented logic).
                // Actually, onDismissRequest handles back button.
            }
        )
    }

    val showBottomNav = currentRoute?.contains("Home") == true || 
                        currentRoute?.contains("Discover") == true || 
                        currentRoute?.contains("About") == true

    var isHomeScrollingUp by remember { mutableStateOf(true) }
    
    // 用于触发 HomeScreen 刷新的状态
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 读取高级 Glass 质感配置
    val usePremiumGlass by config.premiumGlassFlow.collectAsState()

    // 自动检查更新相关状态
    val autoCheckUpdate by config.autoCheckUpdateFlow.collectAsState()
    val updateChannel by config.updateChannelFlow.collectAsState()
    var showUpdateDialog by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    // 节假日问候弹窗状态
    var holidayGreeting by remember { mutableStateOf<HolidayGreeting?>(null) }

    // 冷启动时自动检查更新
    LaunchedEffect(Unit) {
        if (autoCheckUpdate) {
            delay(2000)
            try {
                val result = UpdateChecker.checkUpdate(context, VersionInfo.VERSION_CODE, updateChannel)
                if (result != null && result.isNewer) {
                    showUpdateDialog = result
                }
            } catch (e: Exception) {
            }
        }
    }

    // 自动检查订阅更新（每日一次）
    LaunchedEffect(Unit) {
        if (config.shouldAutoUpdateSubscriptions()) {
            delay(4000)
            try {
                val subs = config.subscriptionsFlow.value.filter { it.isEnabled }
                if (subs.isNotEmpty()) {
                    for (sub in subs) {
                        PresetRemoteManager.fetchAndSave(context, sub.url)
                    }
                    PresetRepository.getInstance(context).reloadDefaultPresets()
                    config.markSubscriptionsUpdated()
                }
            } catch (_: Exception) {
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(600)
        val manager = HolidayGreetingManager.getInstance(context)
        val today = manager.getTodayHoliday()
        if (today != null) {
            holidayGreeting = today
        }
    }

    // 底部导航栏页面顺序，用于决定切换动画方向
    val mainRouteList = remember { listOf("Home", "Discover", "About") }
    fun getNavIndex(route: String?): Int {
        return mainRouteList.indexOfFirst { route?.contains(it) == true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val initialIndex = getNavIndex(initialState.destination.route)
                val targetIndex = getNavIndex(targetState.destination.route)
                
                val direction = if (initialIndex != -1 && targetIndex != -1) {
                    // 底部导航栏页面之间的切换
                    if (targetIndex > initialIndex) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                } else {
                    // 默认的前进导航（如 Home -> Detail）
                    AnimatedContentTransitionScope.SlideDirection.Left
                }
                
                slideIntoContainer(
                    towards = direction,
                    animationSpec = fluidSlideSpec
                ) + fadeIn(animationSpec = fluidFadeSpec)
            },
            exitTransition = {
                val initialIndex = getNavIndex(initialState.destination.route)
                val targetIndex = getNavIndex(targetState.destination.route)
                
                val direction = if (initialIndex != -1 && targetIndex != -1) {
                    // 底部导航栏页面之间的切换
                    if (targetIndex > initialIndex) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                } else {
                    // 默认的前进导航（如 Home -> Detail）
                    AnimatedContentTransitionScope.SlideDirection.Left
                }

                slideOutOfContainer(
                    towards = direction,
                    animationSpec = fluidSlideSpec
                ) + fadeOut(animationSpec = fluidFadeSpec)
            },
            popEnterTransition = {
                val initialIndex = getNavIndex(initialState.destination.route)
                val targetIndex = getNavIndex(targetState.destination.route)
                
                val direction = if (initialIndex != -1 && targetIndex != -1) {
                    // 底部导航栏页面之间的切换（通过 popBackStack 触发，如回到 Home）
                    if (targetIndex > initialIndex) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                } else {
                    // 默认的回退导航（如 Detail -> Home）
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideIntoContainer(
                    towards = direction,
                    animationSpec = fluidSlideSpec
                ) + fadeIn(animationSpec = fluidFadeSpec)
            },
            popExitTransition = {
                val initialIndex = getNavIndex(initialState.destination.route)
                val targetIndex = getNavIndex(targetState.destination.route)
                
                val direction = if (initialIndex != -1 && targetIndex != -1) {
                    // 底部导航栏页面之间的切换（通过 popBackStack 触发，如从 Subscription 回到 Home）
                    if (targetIndex > initialIndex) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                } else {
                    // 默认的回退导航（如 Detail -> Home）
                    AnimatedContentTransitionScope.SlideDirection.Right
                }

                slideOutOfContainer(
                    towards = direction,
                    animationSpec = fluidSlideSpec
                ) + fadeOut(animationSpec = fluidFadeSpec)
            }
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    onNavigateToDetail = { preset: MasterPreset ->
                        preset.id?.let { id ->
                            navController.navigate(Screen.Detail(id))
                        }
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.PresetSelection)
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    },
                    refreshTrigger = refreshTrigger,
                    usePremiumGlass = usePremiumGlass
                )
            }

            composable<Screen.PresetSelection> {
                PresetSelectionScreen(
                    onPresetSelected = { templateId ->
                        navController.navigate(Screen.CreatePreset(templateId))
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.Detail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.Detail>()
                val localContext = androidx.compose.ui.platform.LocalContext.current
                val repository = PresetRepository.getInstance(localContext)
                DetailScreen(
                    presetId = detail.presetId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onEdit = { presetId ->
                        navController.navigate(Screen.EditPreset(presetId))
                    },
                    refreshTrigger = refreshTrigger
                )
            }

            composable<Screen.CreatePreset> { backStackEntry ->
                val createPreset = backStackEntry.toRoute<Screen.CreatePreset>()
                val localContext = androidx.compose.ui.platform.LocalContext.current
                val repository = PresetRepository.getInstance(localContext)
                
                val viewModel: UniversalCreatePresetViewModel = viewModel(
                    factory = UniversalCreatePresetViewModelFactory(localContext.applicationContext as Application, repository)
                )
                
                // Load template if not already loaded (to avoid reloading on recomposition)
                // However, viewModel survives configuration changes, but if we navigate back and forth, 
                // we might want to ensure we don't overwrite if user is editing.
                // For simplicity, we can load it once. 
                // But since we create a new screen instance on navigation, 
                // the viewModel store owner is the backStackEntry, so it's a new ViewModel instance.
                LaunchedEffect(createPreset.templateId) {
                    viewModel.loadTemplate(createPreset.templateId)
                }

                UniversalCreatePresetScreen(
                    onSave = {
                        refreshTrigger++ // 触发刷新
                        // Navigate back to Home, popping the selection screen as well
                        navController.popBackStack(Screen.Home, false)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            composable<Screen.EditPreset> { backStackEntry ->
                val editPreset = backStackEntry.toRoute<Screen.EditPreset>()
                val localContext = androidx.compose.ui.platform.LocalContext.current
                val repository = PresetRepository.getInstance(localContext)
                
                val viewModel: UniversalCreatePresetViewModel = viewModel(
                    factory = UniversalCreatePresetViewModelFactory(localContext.applicationContext as Application, repository)
                )

                LaunchedEffect(editPreset.presetId) {
                    viewModel.loadPresetForEdit(editPreset.presetId)
                }

                UniversalCreatePresetScreen(
                    onSave = {
                        refreshTrigger++ // 触发刷新
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            composable<Screen.Settings> {
                SettingsScreen()
            }

            composable<Screen.About> {
                val localContext = androidx.compose.ui.platform.LocalContext.current
                com.silas.omaster.ui.detail.ProfileScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings)
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription)
                    },
                    onNavigateToSurvey = {
                        localContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SURVEY_URL)))
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(Screen.PrivacyPolicy)
                    },
                    onNavigateToOpenSourceLicense = {
                        navController.navigate(Screen.OpenSourceLicense)
                    }
                )
            }

            composable<Screen.Discover> {
                val context = LocalContext.current
                DiscoverScreen(
                    onNavigateToColorWalk = {
                        navController.navigate(Screen.ColorWalk)
                    },
                    onNavigateToPhotoFrame = {
                        navController.navigate(Screen.PhotoFrame)
                    },
                    onNavigateToSurvey = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SURVEY_URL)))
                    },
                    // 【v1.6.13】今日灵感卡 → 预设参数详情
                    onNavigateToPresetDetail = { presetId ->
                        navController.navigate(Screen.Detail(presetId))
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    }
                )
            }

            composable<Screen.ColorWalk> {
                ColorWalkScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.PhotoFrame> {
                PhotoFrameScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.Subscription> {
                com.silas.omaster.ui.subscription.SubscriptionScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    }
                )
            }

            composable<Screen.PrivacyPolicy> {
                PrivacyPolicyScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.OpenSourceLicense> {
                OpenSourceLicenseScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        if (showBottomNav) {
            PillNavBar(
                visible = isHomeScrollingUp,
                currentRoute = when {
                    currentRoute?.contains("Home") == true -> "home"
                    currentRoute?.contains("Discover") == true -> "discover"
                    currentRoute?.contains("About") == true -> "about"
                    else -> "home"
                },
                onNavigate = { route ->
                    when (route) {
                        "home" -> {
                            if (currentRoute?.contains("Home") != true) {
                                navController.popBackStack(Screen.Home, false)
                            }
                        }
                        "discover" -> {
                            if (currentRoute?.contains("Discover") != true) {
                                navController.navigate(Screen.Discover) {
                                    popUpTo(Screen.Home) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        "about" -> {
                            if (currentRoute?.contains("About") != true) {
                                navController.navigate(Screen.About) {
                                    popUpTo(Screen.Home) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                usePremiumGlass = usePremiumGlass
            )
        }

        // 自动检查更新弹窗
        showUpdateDialog?.let { updateInfo ->
            AlertDialog(
                onDismissRequest = { showUpdateDialog = null },
                title = { 
                    Text(
                        text = "发现新版本",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column {
                        // 版本信息卡片
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "当前版本",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${VersionInfo.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "最新版本",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${updateInfo.versionName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // 更新内容
                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "更新内容",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(max = 150.dp)
                            ) {
                                items(updateInfo.releaseNotes.lines().filter { it.isNotBlank() }) { line ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = line.trim().removePrefix("-").trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            UpdateChecker.downloadAndInstall(context, updateInfo.downloadUrl, updateInfo.versionName)
                            showUpdateDialog = null
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    ) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showUpdateDialog = null }
                    ) {
                        Text(
                            "稍后",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 节假日问候弹窗
        holidayGreeting?.let { holiday ->
            HolidayGreetingDialog(
                holiday = holiday,
                onDismiss = {
                    HolidayGreetingManager.getInstance(context).markShown(holiday.holidayKey)
                    holidayGreeting = null
                }
            )
        }
    }
}
