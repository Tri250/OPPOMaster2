package com.silas.omaster

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.PillNavBar
import com.silas.omaster.ui.create.PresetSelectionScreen
import com.silas.omaster.ui.create.UniversalCreatePresetScreen
import com.silas.omaster.ui.create.UniversalCreatePresetViewModel
import com.silas.omaster.ui.create.UniversalCreatePresetViewModelFactory
import com.silas.omaster.ui.detail.AboutScreen
import com.silas.omaster.ui.detail.DetailScreen
import com.silas.omaster.ui.featured.FeaturedPresetsScreen
import com.silas.omaster.ui.features.AIFineTuneScreen
import com.silas.omaster.ui.features.AISceneRecognitionScreen
import com.silas.omaster.ui.features.CloudSyncScreen
import com.silas.omaster.ui.features.CoreFeaturesScreen
import com.silas.omaster.ui.features.HasselbladScreen
import com.silas.omaster.ui.features.LUTShareScreen
import com.silas.omaster.ui.features.SmartOptimizeScreen
import com.silas.omaster.ui.features.WatermarkEditorScreen
import com.silas.omaster.ui.home.HomeScreen
import com.silas.omaster.ui.settings.NotificationSettingsScreen
import com.silas.omaster.ui.settings.PresetSourceManagerScreen
import com.silas.omaster.ui.settings.SettingsScreen
import com.silas.omaster.ui.settings.TermsScreen
import com.silas.omaster.ui.settings.UpdateChannelScreen
import com.silas.omaster.util.JsonUtil
import com.silas.omaster.util.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 主应用 NavHost 容器 + 底部导航栏 + 全局 Snackbar 宿主
 *
 * 职责：
 *  1. 应用所有 Screen 路由的 NavHost（带方向感知过渡动画）
 *  2. 底部 PillNavBar（仅在主路由可见时显示）
 *  3. 全局 SnackbarHost（用于跨页面提示，例如"已应用预设"）
 *  4. 启动时的旧版数据迁移对话框
 */
@Composable
fun MainApp(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (JsonUtil.currentPresetsVersion != 2) {
            showMigrationDialog = true
        }
    }

    if (showMigrationDialog) {
        MigrationDialog(
            onMigrate = {
                JsonUtil.deleteRemotePresets(context)
                coroutineScope.launch { repository.reloadDefaultPresets() }
                showMigrationDialog = false
            },
            onPostpone = { showMigrationDialog = false },
            onDismiss = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "建议尽快迁移数据以避免功能异常",
                        duration = SnackbarDuration.Short
                    )
                }
                showMigrationDialog = false
            }
        )
    }

    val showBottomNav = currentRoute?.contains("Home") == true ||
        currentRoute?.contains("About") == true ||
        currentRoute?.contains("Subscription") == true ||
        currentRoute?.contains("CoreFeatures") == true

    var isHomeScrollingUp by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val mainRouteList = remember { listOf("Home", "Subscription", "CoreFeatures", "About") }
    val getNavIndex: (String?) -> Int = { route ->
        mainRouteList.indexOfFirst { route?.contains(it) == true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { navEnterTransition(getNavIndex, forward = true) },
            exitTransition = { navExitTransition(getNavIndex, forward = true) },
            popEnterTransition = { navEnterTransition(getNavIndex, forward = false) },
            popExitTransition = { navExitTransition(getNavIndex, forward = false) }
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
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Detail> { backStackEntry ->
                val detail = backStackEntry.toRoute<Screen.Detail>()
                DetailScreen(
                    presetId = detail.presetId,
                    onBack = { navController.popBackStack() },
                    onEdit = { presetId ->
                        navController.navigate(Screen.EditPreset(presetId))
                    },
                    refreshTrigger = refreshTrigger
                )
            }

            composable<Screen.CreatePreset> { backStackEntry ->
                val createPreset = backStackEntry.toRoute<Screen.CreatePreset>()
                val localContext = LocalContext.current
                val localRepository = PresetRepository.getInstance(localContext)

                val viewModel: UniversalCreatePresetViewModel = viewModel(
                    factory = UniversalCreatePresetViewModelFactory(localContext, localRepository)
                )

                LaunchedEffect(createPreset.templateId) {
                    viewModel.loadTemplate(createPreset.templateId)
                }

                UniversalCreatePresetScreen(
                    onSave = {
                        refreshTrigger++
                        navController.popBackStack(Screen.Home, false)
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }

            composable<Screen.EditPreset> { backStackEntry ->
                val editPreset = backStackEntry.toRoute<Screen.EditPreset>()
                val localContext = LocalContext.current
                val localRepository = PresetRepository.getInstance(localContext)

                val viewModel: UniversalCreatePresetViewModel = viewModel(
                    factory = UniversalCreatePresetViewModelFactory(localContext, localRepository)
                )

                LaunchedEffect(editPreset.presetId) {
                    viewModel.loadPresetForEdit(editPreset.presetId)
                }

                UniversalCreatePresetScreen(
                    onSave = {
                        refreshTrigger++
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings) },
                    onNavigateToTerms = { navController.navigate(Screen.Terms) },
                    onNavigateToPresetSourceManager = { navController.navigate(Screen.PresetSourceManager) },
                    onNavigateToUpdateChannel = { navController.navigate(Screen.UpdateChannel) }
                )
            }

            composable<Screen.About> {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings) },
                    onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings) },
                    onNavigateToPresetSourceManager = { navController.navigate(Screen.PresetSourceManager) },
                    onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy) },
                    onNavigateToTerms = { navController.navigate(Screen.Terms) },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp },
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
                        applyPresetAndToast(context, snackbarHostState, coroutineScope, preset)
                    },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp }
                )
            }

            composable<Screen.CoreFeatures> {
                CoreFeaturesScreen(
                    onNavigateToSceneRecognition = { navController.navigate(Screen.SceneRecognition) },
                    onNavigateToAIFineTune = { navController.navigate(Screen.AIFineTune) },
                    onNavigateToWatermarkEditor = { navController.navigate(Screen.WatermarkEditor) },
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Home) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToLUTShare = { navController.navigate(Screen.LUTShare) },
                    onNavigateToHasselbladColor = { navController.navigate(Screen.HasselbladColor) },
                    onNavigateToCloudSync = { navController.navigate(Screen.CloudSync) },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp }
                )
            }

            composable<Screen.AIFineTune> {
                AIFineTuneScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.SceneRecognition> {
                AISceneRecognitionScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.WatermarkEditor> {
                WatermarkEditorScreen(
                    imagePath = null,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
                    onExport = { _, _ -> navController.popBackStack() }
                )
            }

            composable<Screen.SmartOptimize> {
                SmartOptimizeScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        // 应用优化参数到设置
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyPresetParams(
                            saturation = params.colorCorrectionStrength.toInt(),
                            contrast = params.sharpenStrength.toInt(),
                            warmth = 0,
                            sharpness = params.sharpenStrength.toInt(),
                            clarity = params.hdrStrength.toInt(),
                            brightness = params.exposureAdjustment.toInt()
                        )
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.ParamAdjustment> {
                SmartOptimizeScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyPresetParams(
                            saturation = params.colorCorrectionStrength.toInt(),
                            contrast = params.sharpenStrength.toInt(),
                            warmth = 0,
                            sharpness = params.sharpenStrength.toInt(),
                            clarity = params.hdrStrength.toInt(),
                            brightness = params.exposureAdjustment.toInt()
                        )
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.LUTShare> {
                LUTShareScreen(
                    onBack = { navController.popBackStack() },
                    onDownload = { /* LUT download handled internally */ }
                )
            }

            composable<Screen.HasselbladColor> {
                HasselbladScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.CloudSync> {
                CloudSyncScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.NotificationSettings> {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.Terms> {
                TermsScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.PresetSourceManager> {
                PresetSourceManagerScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.UpdateChannel> {
                UpdateChannelScreen(onBack = { navController.popBackStack() })
            }
        }

        if (showBottomNav) {
            PillNavBar(
                visible = true,
                currentRoute = when {
                    currentRoute?.contains("Home") == true -> "home"
                    currentRoute?.contains("Subscription") == true -> "subscription"
                    currentRoute?.contains("CoreFeatures") == true -> "features"
                    currentRoute?.contains("About") == true -> "about"
                    else -> "home"
                },
                onNavigate = { route ->
                    handleBottomNav(navController, route, currentRoute)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 旧版预设数据迁移提示对话框
 */
@Composable
private fun MigrationDialog(
    onMigrate: () -> Unit,
    onPostpone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据结构更新") },
        text = {
            Text(
                "检测到预设数据版本过旧，需要迁移数据以支持新功能。\n\n" +
                    "点击\"迁移数据\"将重置内置预设（您的自定义预设和收藏不会丢失）。"
            )
        },
        confirmButton = {
            TextButton(onClick = onMigrate) { Text("迁移数据") }
        },
        dismissButton = {
            TextButton(onClick = onPostpone) { Text("稍后处理") }
        }
    )
}

/**
 * 应用预设参数到相机设置，并通过 Snackbar 反馈结果
 */
private fun applyPresetAndToast(
    context: android.content.Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    preset: MasterPreset
) {
    val settingsManager = SettingsManager.getInstance(context)
    settingsManager.applyPresetParams(
        saturation = preset.saturation ?: 0,
        contrast = preset.tone ?: 0,
        warmth = preset.warmCool ?: 0,
        sharpness = preset.sharpness ?: 0,
        clarity = 0,
        brightness = 0
    )
    coroutineScope.launch {
        snackbarHostState.showSnackbar(
            message = "已应用预设：${preset.name}",
            actionLabel = "确定",
            duration = SnackbarDuration.Short
        )
    }
}

/**
 * 底部导航栏点击处理：切换到对应主路由（保持 Home 为根）
 */
private fun handleBottomNav(
    navController: NavHostController,
    route: String,
    currentRoute: String?
) {
    when (route) {
        "home" -> {
            if (currentRoute?.contains("Home") != true) {
                navController.popBackStack(Screen.Home, false)
            }
        }
        "subscription" -> {
            if (currentRoute?.contains("Subscription") != true) {
                navController.navigate(Screen.Subscription) {
                    popUpTo(Screen.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        "features" -> {
            if (currentRoute?.contains("CoreFeatures") != true) {
                navController.navigate(Screen.CoreFeatures) {
                    popUpTo(Screen.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        "about" -> {
            if (currentRoute?.contains("About") != true) {
                navController.navigate(Screen.About) {
                    popUpTo(Screen.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}

/**
 * 计算 NavHost 进入动画方向（底部导航栏之间用左右滑动，其他用默认方向）
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(
    getNavIndex: (String?) -> Int,
    forward: Boolean
): EnterTransition {
    val direction = computeSlideDirection(getNavIndex, forward)
    return slideIntoContainer(towards = direction, animationSpec = tween(300)) +
        fadeIn(animationSpec = tween(300))
}

/**
 * 计算 NavHost 退出动画方向
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(
    getNavIndex: (String?) -> Int,
    forward: Boolean
): ExitTransition {
    val direction = computeSlideDirection(getNavIndex, forward)
    return slideOutOfContainer(towards = direction, animationSpec = tween(300)) +
        fadeOut(animationSpec = tween(300))
}

/**
 * 根据当前路由和目标路由决定滑动方向
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.computeSlideDirection(
    getNavIndex: (String?) -> Int,
    forward: Boolean
): AnimatedContentTransitionScope.SlideDirection {
    val initialIndex = getNavIndex(initialState.destination.route)
    val targetIndex = getNavIndex(targetState.destination.route)
    return when {
        initialIndex != -1 && targetIndex != -1 -> {
            if (targetIndex > initialIndex) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
        }
        forward -> AnimatedContentTransitionScope.SlideDirection.Left
        else -> AnimatedContentTransitionScope.SlideDirection.Right
    }
}
