package com.silas.omaster

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.silas.omaster.ui.animation.AnimationSpecs
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.OnboardingManager
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.PillNavBar
import com.silas.omaster.ui.create.PresetSelectionScreen
import com.silas.omaster.ui.create.UniversalCreatePresetScreen
import com.silas.omaster.ui.create.UniversalCreatePresetViewModel
import com.silas.omaster.ui.create.UniversalCreatePresetViewModelFactory
import com.silas.omaster.ui.detail.AboutScreen
import com.silas.omaster.ui.detail.DetailScreen
import com.silas.omaster.ui.detail.PrivacyPolicyScreen
import com.silas.omaster.ui.features.AIFineTuneScreen
import com.silas.omaster.ui.features.CameraXViewfinderScreen
import com.silas.omaster.ui.features.CoreFeaturesScreen
import com.silas.omaster.ui.features.HasselbladScreen
import com.silas.omaster.ui.features.LUTShareScreen
import com.silas.omaster.ui.features.ParamAdjustScreen
import com.silas.omaster.ui.features.SmartOptimizeScreen
import com.silas.omaster.ui.features.StyleLUTGeneratorScreen
import com.silas.omaster.ui.features.WatermarkEditorScreen
import com.silas.omaster.ui.home.HomeScreen
import com.silas.omaster.ui.onboarding.OnboardingScreen
import com.silas.omaster.ui.subscription.SubscriptionScreen
import com.silas.omaster.ui.screens.SceneAnalysisReportScreen
import com.silas.omaster.ui.settings.ApiConfigScreen
import com.silas.omaster.ui.settings.ImportExportScreen
import com.silas.omaster.ui.settings.NotificationSettingsScreen
import com.silas.omaster.ui.settings.SettingsScreen
import com.silas.omaster.ui.settings.TermsScreen
import com.silas.omaster.ui.settings.ThemeSettingsScreen
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
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var showMigrationDialog by remember { mutableStateOf(false) }

    // 首次启动引导页检测
    val onboardingManager = remember { OnboardingManager.getInstance(context) }
    val initialShowOnboarding = remember {
        onboardingManager.shouldShowOnboarding(VersionInfo.VERSION_CODE.toLong())
    }
    var showOnboarding by remember { mutableStateOf(initialShowOnboarding) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 迁移对话框：仅在首次检测到旧版本数据时显示一次
    LaunchedEffect(Unit) {
        val hasMigrationBeenHandled = settingsManager.getMigrationHandled()
        if (!hasMigrationBeenHandled && JsonUtil.currentPresetsVersion != 2) {
            showMigrationDialog = true
        }
    }

    if (showMigrationDialog) {
        MigrationDialog(
            onMigrate = {
                JsonUtil.deleteRemotePresets(context)
                // 迁移后强制从文件重新加载预设，避免使用旧内存缓存
                coroutineScope.launch { repository.forceReloadFromFiles() }
                settingsManager.setMigrationHandled(true)
                showMigrationDialog = false
            },
            onPostpone = {
                settingsManager.setMigrationHandled(true)
                showMigrationDialog = false
            },
            onDismiss = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "建议尽快迁移数据以避免功能异常",
                        duration = SnackbarDuration.Short
                    )
                }
                settingsManager.setMigrationHandled(true)
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

    Box(modifier = Modifier
        .fillMaxSize()
        .semantics { contentDescription = "OMaster 主应用" }) {
        NavHost(
            navController = navController,
            startDestination = if (initialShowOnboarding) Screen.Onboarding else Screen.Home,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomNav) 80.dp else 0.dp),
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
                    onNavigateToAIFineTune = { navController.navigate(Screen.AIFineTune) },
                    onNavigateToWatermarkEditor = { navController.navigate(Screen.WatermarkEditor()) },
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToHasselbladEye = { navController.navigate(Screen.HasselbladColor) },
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
                    onNavigateToPresetSourceManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToUpdateChannel = { navController.navigate(Screen.UpdateChannel) },
                    onNavigateToApiConfig = { navController.navigate(Screen.ApiConfig) },
                    onNavigateToThemeSettings = { navController.navigate(Screen.ThemeSettings) },
                    onNavigateToSceneAnalysisReport = { navController.navigate(Screen.SceneAnalysisReport) },
                    onNavigateToImportExport = { navController.navigate(Screen.ImportExport) }
                )
            }

            composable<Screen.About> {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings) },
                    onNavigateToNotificationSettings = { navController.navigate(Screen.NotificationSettings) },
                    onNavigateToPresetSourceManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy) },
                    onNavigateToTerms = { navController.navigate(Screen.Terms) },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp },
                    currentVersionCode = VersionInfo.VERSION_CODE,
                    currentVersionName = VersionInfo.VERSION_NAME
                )
            }

            composable<Screen.Subscription> {
                SubscriptionScreen(
                    onBack = { navController.popBackStack() },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp }
                )
            }

            composable<Screen.CoreFeatures> {
                CoreFeaturesScreen(
                    onNavigateToAIFineTune = { navController.navigate(Screen.AIFineTune) },
                    onNavigateToWatermarkEditor = { navController.navigate(Screen.WatermarkEditor()) },
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToLUTShare = { navController.navigate(Screen.LUTShare) },
                    onNavigateToHasselbladColor = { navController.navigate(Screen.HasselbladColor) },
                    onNavigateToSceneAnalysisReport = { navController.navigate(Screen.SceneAnalysisReport) },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp }
                )
            }

            composable<Screen.AIFineTune> {
                AIFineTuneScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "已应用 ${params.nonZeroCount()} 项调整",
                                duration = SnackbarDuration.Short
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.WatermarkEditor> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.WatermarkEditor>()
                WatermarkEditorScreen(
                    imagePath = route.imagePath,
                    onBack = { navController.popBackStack() },
                    onSave = { config ->
                        // 保存水印配置到本地，供批量应用使用
                        try {
                            val prefs = context.getSharedPreferences("watermark_config", 0)
                            prefs.edit().apply {
                                putString("last_config", config.toString())
                                putLong("last_save_time", System.currentTimeMillis())
                                apply()
                            }
                            Toast.makeText(context, "水印配置已保存，可批量应用", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "水印配置保存失败：${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                        navController.popBackStack()
                    },
                    onExport = { bitmap, _ ->
                        // 保存水印图片到相册
                        val saved = saveWatermarkToGallery(context, bitmap)
                        if (saved) {
                            Toast.makeText(context, "水印图片已保存到相册（PNG无损）", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "保存失败，请检查权限", Toast.LENGTH_SHORT).show()
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.SmartOptimize> {
                SmartOptimizeScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        // 应用智能优化参数到设置
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyPresetParams(
                            saturation = params.colorCorrectionStrength.toInt(),
                            contrast = params.hdrStrength.toInt(),
                            warmth = 0,
                            sharpness = params.sharpenStrength.toInt(),
                            clarity = params.noiseReductionStrength.toInt(),
                            brightness = params.exposureAdjustment.toInt()
                        )
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.ParamAdjustment> {
                ParamAdjustScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyCameraParams(
                            iso = params.iso,
                            shutterSpeed = params.shutterSpeed,
                            aperture = params.aperture,
                            whiteBalance = params.whiteBalance,
                            focalLength = params.focalLength,
                            exposureCompensation = params.exposureCompensation
                        )
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.LUTShare> {
                LUTShareScreen(
                    onBack = { navController.popBackStack() },
                    onDownload = { lutResource ->
                        // 标记当前激活的 LUT，便于哈苏之眼引用
                        com.silas.omaster.data.lut.LUTManager
                            .getInstance(context.applicationContext)
                            .setActiveLUT(lutResource.id)
                    },
                    onApplyLUT = { lutResource ->
                        // 应用 LUT 前先激活到 LUTManager，确保哈苏之眼能读取
                        com.silas.omaster.data.lut.LUTManager
                            .getInstance(context.applicationContext)
                            .setActiveLUT(lutResource.id)
                        navController.navigate(Screen.HasselbladColor)
                    },
                    onNavigateToStyleGenerator = {
                        navController.navigate(Screen.StyleLUTGenerator)
                    }
                )
            }

            composable<Screen.StyleLUTGenerator> {
                StyleLUTGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onApplyLUT = {
                        // 应用生成的 LUT 后导航到哈苏之眼进行调色
                        navController.navigate(Screen.HasselbladColor)
                    }
                )
            }

            composable<Screen.HasselbladColor> {
                HasselbladScreen(
                    onBack = { navController.popBackStack() },
                    onLaunchViewfinder = {
                        navController.navigate(Screen.CameraXViewfinder(presetId = null))
                    }
                )
            }

            composable<Screen.NotificationSettings> {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.Terms> {
                TermsScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.PrivacyPolicy> {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.UpdateChannel> {
                UpdateChannelScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.ApiConfig> {
                ApiConfigScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.ThemeSettings> {
                ThemeSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { settings ->
                        // 应用主题设置并持久化
                        val manager = SettingsManager.getInstance(context)
                        manager.currentTheme = BrandTheme.fromId(settings.theme)
                        manager.darkMode = when (settings.darkMode) {
                            "light" -> DarkMode.LIGHT
                            "dark" -> DarkMode.DARK
                            else -> DarkMode.SYSTEM
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.SceneAnalysisReport> {
                SceneAnalysisReportScreen(
                    onBack = { navController.popBackStack() },
                    onViewDetails = {
                        navController.popBackStack(Screen.Home, false)
                    }
                )
            }

            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        onboardingManager.markOnboardingShown(VersionInfo.VERSION_CODE.toLong())
                        showOnboarding = false
                        // 若 Onboarding 为起始页，回退栈为空，需导航到 Home 作为新根
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home) {
                                popUpTo(Screen.Onboarding) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        onboardingManager.skipOnboarding(VersionInfo.VERSION_CODE.toLong())
                        showOnboarding = false
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home) {
                                popUpTo(Screen.Onboarding) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<Screen.CameraXViewfinder> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.CameraXViewfinder>()
                var presetParams by remember { mutableStateOf(HasselbladParams()) }
                var presetName by remember { mutableStateOf("") }

                LaunchedEffect(route.presetId) {
                    val pid = route.presetId ?: return@LaunchedEffect
                    val presetItem = repository.presets.value.find { it.id == pid }
                        ?: repository.loadPresets().find { it.id == pid }
                    val preset = presetItem?.toMasterPreset()
                    if (preset != null) {
                        presetName = preset.name
                        presetParams = HasselbladParams(
                            tone = preset.tone ?: 0,
                            saturation = preset.saturation ?: 0,
                            colorTemp = preset.warmCool ?: 0,
                            sharpness = preset.sharpness ?: 0
                        )
                    }
                }

                CameraXViewfinderScreen(
                    presetParams = presetParams,
                    presetName = presetName,
                    onBack = { navController.popBackStack() },
                    onPhotoCaptured = { uri ->
                        // 拍照后返回上一页
                        navController.popBackStack()
                    }
                )
            }

            composable<Screen.ImportExport> {
                ImportExportScreen(
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
    return slideIntoContainer(towards = direction, animationSpec = AnimationSpecs.ColorOS16PageTransition) +
        fadeIn(animationSpec = AnimationSpecs.ColorOS16ContentEnter)
}

/**
 * 计算 NavHost 退出动画方向
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(
    getNavIndex: (String?) -> Int,
    forward: Boolean
): ExitTransition {
    val direction = computeSlideDirection(getNavIndex, forward)
    return slideOutOfContainer(towards = direction, animationSpec = AnimationSpecs.ColorOS16PageTransition) +
        fadeOut(animationSpec = AnimationSpecs.ColorOS16ContentExit)
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

private fun saveWatermarkToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        val filename = "OMaster_Watermark_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OMaster")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            true
        } else false
    } catch (e: Exception) {
        false
    }
}
