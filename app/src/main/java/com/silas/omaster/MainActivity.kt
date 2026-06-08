package com.silas.omaster

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.PillNavBar
import com.silas.omaster.ui.components.WelcomeDialog
import com.silas.omaster.ui.create.PresetSelectionScreen
import com.silas.omaster.ui.create.UniversalCreatePresetScreen
import com.silas.omaster.ui.create.UniversalCreatePresetViewModel
import com.silas.omaster.ui.detail.AboutScreen
import com.silas.omaster.ui.detail.DetailScreen
import com.silas.omaster.ui.detail.PrivacyPolicyScreen
import com.silas.omaster.ui.home.HomeScreen
import com.silas.omaster.ui.service.FloatingWindowController
import com.silas.omaster.ui.theme.OMasterTheme
import com.silas.omaster.util.JsonUtil
import com.silas.omaster.util.VersionInfo
import kotlinx.serialization.Serializable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.repository.PresetRepository

import androidx.compose.runtime.collectAsState
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.settings.SettingsScreen
import com.silas.omaster.ui.features.CoreFeaturesScreen
import com.silas.omaster.ui.features.AIFineTuneScreen
import com.silas.omaster.ui.features.CameraSceneRecognitionScreen
import com.silas.omaster.ui.featured.FeaturedPresetsScreen


val LocalActivity = compositionLocalOf<Activity> { error("No Activity provided") }

sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data class Detail(val presetId: String) : Screen()

    @Serializable
    data object PresetSelection : Screen()

    @Serializable
    data class CreatePreset(val templateId: String? = null) : Screen()

    @Serializable
    data class EditPreset(val presetId: String) : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object About : Screen()

    @Serializable
    data object Subscription : Screen()

    @Serializable
    data object CoreFeatures : Screen()

    @Serializable
    data object AIFineTune : Screen()

    @Serializable
    data object SceneRecognition : Screen()

    @Serializable
    data object WatermarkEditor : Screen()

    @Serializable
    data object SmartOptimize : Screen()

    @Serializable
    data object ParamAdjustment : Screen()

    @Serializable
    data object PrivacyPolicy : Screen()
}

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
                OMasterTheme(
                    darkMode = darkMode,
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

@Composable
fun WelcomeFlow(
    navController: NavHostController,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    // 处理系统返回键
    androidx.activity.compose.BackHandler(enabled = showPrivacyPolicy) {
        showPrivacyPolicy = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showPrivacyPolicy) {
            PrivacyPolicyScreen(
                onBack = {
                    showPrivacyPolicy = false
                }
            )
        } else {
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

@Composable
fun MainApp(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (JsonUtil.currentPresetsVersion != 2) {
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
                        currentRoute?.contains("About") == true || 
                        currentRoute?.contains("Subscription") == true ||
                        currentRoute?.contains("CoreFeatures") == true

    var isHomeScrollingUp by remember { mutableStateOf(true) }
    
    // 用于触发 HomeScreen 刷新的状态
    var refreshTrigger by remember { mutableStateOf(0) }

    // 底部导航栏页面顺序，用于决定切换动画方向
    val mainRouteList = remember { listOf("Home", "Subscription", "CoreFeatures", "About") }
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
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
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
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
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
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
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
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
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
                    onNavigateToSceneRecognition = { navController.navigate(Screen.SceneRecognition) },
                    onNavigateToAIFineTune = { navController.navigate(Screen.AIFineTune) },
                    onNavigateToWatermarkEditor = { navController.navigate(Screen.WatermarkEditor) },
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Home) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    },
                    refreshTrigger = refreshTrigger
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
                    factory = UniversalCreatePresetViewModelFactory(localContext, repository)
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
                    factory = UniversalCreatePresetViewModelFactory(localContext, repository)
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
                AboutScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings)
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    },
                    currentVersionCode = VersionInfo.VERSION_CODE,
                    currentVersionName = VersionInfo.VERSION_NAME
                )
            }

            composable<Screen.Subscription> {
                FeaturedPresetsScreen(
                    onNavigateToDetail = { preset ->
                        navController.navigate(Screen.Detail(preset.id ?: ""))
                    },
                    onApplyPreset = { preset ->
                        // 应用预设参数到相机
                        // 将预设参数保存到设置中，供相机使用
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyPresetParams(
                            saturation = preset.saturation,
                            contrast = preset.contrast,
                            warmth = preset.warmth,
                            sharpness = preset.sharpness,
                            clarity = preset.clarity,
                            brightness = preset.brightness
                        )
                        // 显示应用成功提示
                        Toast.makeText(context, "预设参数已应用", Toast.LENGTH_SHORT).show()
                    },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    }
                )
            }

            composable<Screen.CoreFeatures> {
                CoreFeaturesScreen(
                    onNavigateToSceneRecognition = { navController.navigate(Screen.SceneRecognition) },
                    onNavigateToAIFineTune = { navController.navigate(Screen.AIFineTune) },
                    onNavigateToWatermarkEditor = { navController.navigate(Screen.WatermarkEditor) },
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToHasselbladColor = { navController.navigate(Screen.Settings) },
                    onScrollStateChanged = { isScrollingUp ->
                        isHomeScrollingUp = isScrollingUp
                    }
                )
            }

            composable<Screen.AIFineTune> {
                AIFineTuneScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // AI场景识别页面 - 真实CameraX相机实时预览
            composable<Screen.SceneRecognition> {
                CameraSceneRecognitionScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 水印编辑器页面（复用AIFineTuneScreen）
            composable<Screen.WatermarkEditor> {
                AIFineTuneScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 智能优化页面（复用AIFineTuneScreen）
            composable<Screen.SmartOptimize> {
                AIFineTuneScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 参数调节页面（复用AIFineTuneScreen）
            composable<Screen.ParamAdjustment> {
                AIFineTuneScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomNav) {
            PillNavBar(
                visible = isHomeScrollingUp,
                currentRoute = when {
                    currentRoute?.contains("Home") == true -> "home"
                    currentRoute?.contains("Subscription") == true -> "subscription"
                    currentRoute?.contains("CoreFeatures") == true -> "features"
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
                        "subscription" -> {
                            if (currentRoute?.contains("Subscription") != true) {
                                navController.navigate(Screen.Subscription) {
                                    popUpTo(Screen.Home) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        "features" -> {
                            if (currentRoute?.contains("CoreFeatures") != true) {
                                navController.navigate(Screen.CoreFeatures) {
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
