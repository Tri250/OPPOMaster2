package com.silas.omaster

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.util.Log
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
import com.silas.omaster.ui.detail.CloudSyncScreen
import com.silas.omaster.ui.detail.DetailScreen
import com.silas.omaster.ui.detail.PrivacyPolicyScreen
import com.silas.omaster.ui.features.AIFineTuneScreen
import com.silas.omaster.ui.features.CameraXViewfinderScreen
import com.silas.omaster.ui.features.CoreFeaturesScreen
import com.silas.omaster.ui.features.HasselbladEyeViewModel
import com.silas.omaster.ui.features.HasselbladScreen
import com.silas.omaster.ui.features.LUTShareScreen
import com.silas.omaster.video.VideoFilterScreen
import com.silas.omaster.ui.features.ParamAdjustScreen
import com.silas.omaster.ui.features.SmartOptimizeScreen
import com.silas.omaster.ui.features.StyleLUTGeneratorScreen
import com.silas.omaster.ui.home.HomeScreen
import com.silas.omaster.ui.onboarding.OnboardingScreen
import com.silas.omaster.ui.subscription.SubscriptionScreen
import com.silas.omaster.ui.screens.SceneAnalysisReportScreen
import com.silas.omaster.ui.settings.ApiConfigScreen
import com.silas.omaster.ui.settings.ImportExportScreen
import com.silas.omaster.ui.settings.NotificationSettingsScreen
import com.silas.omaster.ui.settings.PermissionCheckScreen
import com.silas.omaster.ui.settings.SettingsScreen
import com.silas.omaster.ui.settings.TermsScreen
import com.silas.omaster.ui.settings.ThemeSettingsScreen
import com.silas.omaster.ui.settings.UpdateChannelScreen
import com.silas.omaster.trailsnap.ui.AlbumDetailScreen
import com.silas.omaster.trailsnap.ui.AlbumsScreen
import com.silas.omaster.trailsnap.ui.AnnualReportScreen
import com.silas.omaster.trailsnap.ui.FavoritesScreen
import com.silas.omaster.trailsnap.ui.LocationDetailScreen
import com.silas.omaster.trailsnap.ui.LocationsScreen
import com.silas.omaster.trailsnap.ui.PeopleScreen
import com.silas.omaster.trailsnap.ui.PersonDetailScreen
import com.silas.omaster.trailsnap.ui.RecycleBinScreen
import com.silas.omaster.trailsnap.ui.TicketsScreen
import com.silas.omaster.trailsnap.ui.TimelineScreen
import com.silas.omaster.trailsnap.ui.ToolboxScreen
import com.silas.omaster.trailsnap.ui.XingYingJiHomeScreen
import com.silas.omaster.util.JsonUtil
import com.silas.omaster.util.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// P2-1 修复：savedStateHandle 键名常量（哈苏构图引导线状态传递）
private const val KEY_HASSELBLAD_GUIDE_TYPE = "hasselblad_guide_type"
private const val KEY_IS_AR_GUIDE_ENABLED = "hasselblad_ar_guide_enabled"
// P2-4 修复：savedStateHandle 键名常量（配方参数传递给出境器）
private const val KEY_VIEWFINDER_PARAMS = "viewfinder_params"
private const val KEY_VIEWFINDER_RECIPE_NAME = "viewfinder_recipe_name"

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
fun MainApp(
    navController: NavHostController,
    deepLinkPresetId: String? = null
) {
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

    // Deep Link 导航：当通过 Deep Link 打开时，自动导航到预设详情页
    LaunchedEffect(deepLinkPresetId) {
        if (!deepLinkPresetId.isNullOrBlank()) {
            try {
                navController.navigate(Screen.Detail(deepLinkPresetId))
            } catch (e: Exception) {
                Log.e("AppNavigation", "DeepLink navigation failed", e)
            }
        }
    }

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

    // P2 修复：使用类型安全的路由匹配，避免子页面名称巧合包含主路由名导致底部导航栏误显
    val showBottomNav = currentRoute.isRoute("com.silas.omaster.Screen.Home") ||
        currentRoute.isRoute("com.silas.omaster.Screen.About") ||
        currentRoute.isRoute("com.silas.omaster.Screen.Subscription") ||
        currentRoute.isRoute("com.silas.omaster.Screen.CoreFeatures")

    var isHomeScrollingUp by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val mainRouteList = remember {
        listOf(
            "com.silas.omaster.Screen.Home",
            "com.silas.omaster.Screen.Subscription",
            "com.silas.omaster.Screen.CoreFeatures",
            "com.silas.omaster.Screen.About"
        )
    }
    val getNavIndex: (String?) -> Int = { route ->
        mainRouteList.indexOfFirst { route.isRoute(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToHasselbladEye = { navController.navigate(Screen.HasselbladColor) },
                    onNavigateToXingYingJi = { navController.navigate(Screen.XingYingJiHome) },
                    onScrollStateChanged = { isScrollingUp -> isHomeScrollingUp = isScrollingUp },
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
                    onNavigateToImportExport = { navController.navigate(Screen.ImportExport) },
                    onNavigateToPermissionCheck = { navController.navigate(Screen.PermissionCheck) },
                    onNavigateToCloudSync = { navController.navigate(Screen.CloudSync) }
                )
            }

            // 2.2.0 新增：权限自检页面
            composable<Screen.PermissionCheck> {
                PermissionCheckScreen(
                    onBack = { navController.popBackStack() }
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
                    onNavigateToSmartOptimize = { navController.navigate(Screen.SmartOptimize) },
                    onNavigateToPresetManager = { navController.navigate(Screen.Subscription) },
                    onNavigateToParamAdjustment = { navController.navigate(Screen.ParamAdjustment) },
                    onNavigateToLUTShare = { navController.navigate(Screen.LUTShare) },
                    onNavigateToHasselbladColor = { navController.navigate(Screen.HasselbladColor) },
                    onNavigateToSceneAnalysisReport = { navController.navigate(Screen.SceneAnalysisReport) },
                    onNavigateToXingYingJi = { navController.navigate(Screen.XingYingJiHome) },
                    onNavigateToWatermark = { navController.navigate(Screen.Watermark) },
                    onNavigateToXmpImport = { navController.navigate(Screen.XmpImport) },
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

            composable<Screen.SmartOptimize> {
                SmartOptimizeScreen(
                    onBack = { navController.popBackStack() },
                    onApply = { params ->
                        // 将智能优化参数映射到相机预设参数
                        val settingsManager = SettingsManager.getInstance(context)
                        settingsManager.applyPresetParams(
                            saturation = params.saturation.toInt(),
                            contrast = params.contrast.toInt(),
                            warmth = 0,
                            sharpness = params.sharpness.toInt(),
                            clarity = params.clarity.toInt(),
                            brightness = params.brightness.toInt()
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
                    },
                    onSaveCopy = { params ->
                        // PR-04：保存为新预设副本
                        val repo = PresetRepository.getInstance(context)
                        repo.createCustomPreset(
                            name = "参数微调副本 ${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                            params = mapOf(
                                "iso" to params.iso.toString(),
                                "shutterSpeed" to params.shutterSpeed.toString(),
                                "aperture" to params.aperture.toString(),
                                "whiteBalance" to params.whiteBalance.toString(),
                                "focalLength" to params.focalLength.toString(),
                                "exposureCompensation" to params.exposureCompensation.toString()
                            ),
                            brand = "custom",
                            description = "通过参数微调保存的自定义预设"
                        )
                    }
                )
            }

            composable<Screen.Watermark> {
                com.silas.omaster.ui.features.watermark.WatermarkScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XmpImport> {
                com.silas.omaster.ui.features.xmp.XmpImportScreen(
                    onBack = { navController.popBackStack() }
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
                    },
                    onNavigateToVideoFilter = {
                        navController.navigate(Screen.VideoFilter)
                    }
                )
            }

            composable<Screen.VideoFilter> {
                VideoFilterScreen(
                    onBack = { navController.popBackStack() }
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

            composable<Screen.HasselbladColor> { backStackEntry ->
                HasselbladScreen(
                    onBack = { navController.popBackStack() },
                    // P2-4 修复：将当前配方参数保存到 savedStateHandle，CameraXViewfinder 启动时自动应用
                    onLaunchViewfinder = { params, recipeName ->
                        backStackEntry.savedStateHandle[KEY_VIEWFINDER_PARAMS] = params
                        backStackEntry.savedStateHandle[KEY_VIEWFINDER_RECIPE_NAME] = recipeName
                        navController.navigate(Screen.CameraXViewfinder(presetId = null))
                    },
                    // P2-1 修复：同步哈苏构图引导线状态到 savedStateHandle，
                    // CameraXViewfinder 启动时通过 previousBackStackEntry 读取
                    onARGuideStateChanged = { guideType, isEnabled ->
                        backStackEntry.savedStateHandle[KEY_HASSELBLAD_GUIDE_TYPE] = guideType
                        backStackEntry.savedStateHandle[KEY_IS_AR_GUIDE_ENABLED] = isEnabled
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
                val hasselbladViewModel: HasselbladEyeViewModel = viewModel()

                // P2-1 修复：从上一个页面（HasselbladScreen）读取哈苏构图引导线状态
                val hasselbladGuideType = remember {
                    navController.previousBackStackEntry?.savedStateHandle?.get<String>(KEY_HASSELBLAD_GUIDE_TYPE)
                }
                val isARGuideEnabled = remember {
                    navController.previousBackStackEntry?.savedStateHandle?.get<Boolean>(KEY_IS_AR_GUIDE_ENABLED) ?: false
                }

                // P2-4 修复：从上一个页面读取配方参数与名称，自动应用配方到取景器
                val recipeParams = remember {
                    navController.previousBackStackEntry?.savedStateHandle?.get<HasselbladParams>(KEY_VIEWFINDER_PARAMS)
                }
                val recipeNameFromPrev = remember {
                    navController.previousBackStackEntry?.savedStateHandle?.get<String>(KEY_VIEWFINDER_RECIPE_NAME)
                }

                LaunchedEffect(route.presetId, recipeParams, recipeNameFromPrev) {
                    if (recipeParams != null) {
                        presetParams = recipeParams
                        presetName = recipeNameFromPrev ?: ""
                    } else {
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
                }

                CameraXViewfinderScreen(
                    presetParams = presetParams,
                    presetName = presetName,
                    onBack = { navController.popBackStack() },
                    hasselbladGuideType = hasselbladGuideType,
                    isARGuideEnabled = isARGuideEnabled,
                    onPhotoCaptured = { uri ->
                        // 拍照成功，Toast 提示并返回上一页
                        Toast.makeText(context, "照片已保存", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onFixActionRequested = { action ->
                        when (action.actionValue) {
                            "soft_light" -> {
                                // 启用柔光模式（作用于拍摄后色彩处理）
                                hasselbladViewModel.updateParam(
                                    "softLight",
                                    com.silas.omaster.model.SoftLightMode.SOFT.ordinal
                                )
                                Toast.makeText(context, "已启用柔光模式", Toast.LENGTH_SHORT).show()
                            }
                            "ar_guide:diagonal" -> {
                                // 切换 AR 引导线
                                hasselbladViewModel.toggleARGuide()
                                Toast.makeText(context, "已切换 AR 构图引导", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                // 其他模式暂无法直接自动修复，提示用户
                                Toast.makeText(context, "请手动调整：${action.label}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            composable<Screen.CloudSync> {
                CloudSyncScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.ImportExport> {
                ImportExportScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XingYingJiHome> {
                XingYingJiHomeScreen(
                    onNavigateToTimeline = { navController.navigate(Screen.XingYingJiTimeline) },
                    onNavigateToAlbums = { navController.navigate(Screen.XingYingJiAlbums) },
                    onNavigateToFavorites = { navController.navigate(Screen.XingYingJiFavorites) },
                    onNavigateToLocations = { navController.navigate(Screen.XingYingJiLocations) },
                    onNavigateToPeople = { navController.navigate(Screen.XingYingJiPeople) },
                    onNavigateToTickets = { navController.navigate(Screen.XingYingJiTickets) },
                    onNavigateToToolbox = { navController.navigate(Screen.XingYingJiToolbox) },
                    onNavigateToAnnualReport = { navController.navigate(Screen.XingYingJiAnnualReport) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XingYingJiTimeline> {
                TimelineScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.XingYingJiAlbums> {
                AlbumsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAlbumDetail = { albumId ->
                        navController.navigate(Screen.XingYingJiAlbumDetail(albumId))
                    }
                )
            }

            composable<Screen.XingYingJiAlbumDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.XingYingJiAlbumDetail>()
                AlbumDetailScreen(
                    albumId = route.albumId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XingYingJiFavorites> {
                FavoritesScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.XingYingJiLocations> {
                LocationsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLocationDetail = { locationName ->
                        navController.navigate(Screen.XingYingJiLocationDetail(locationName))
                    }
                )
            }

            composable<Screen.XingYingJiLocationDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.XingYingJiLocationDetail>()
                LocationDetailScreen(
                    locationName = route.locationName,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XingYingJiPeople> {
                PeopleScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToPersonDetail = { faceId ->
                        navController.navigate(Screen.XingYingJiPersonDetail(faceId))
                    }
                )
            }

            composable<Screen.XingYingJiPersonDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.XingYingJiPersonDetail>()
                PersonDetailScreen(
                    faceId = route.faceId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Screen.XingYingJiTickets> {
                TicketsScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.XingYingJiToolbox> {
                ToolboxScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToRecycleBin = { navController.navigate(Screen.XingYingJiRecycleBin) }
                )
            }

            composable<Screen.XingYingJiRecycleBin> {
                RecycleBinScreen(onBack = { navController.popBackStack() })
            }

            composable<Screen.XingYingJiAnnualReport> {
                AnnualReportScreen(onBack = { navController.popBackStack() })
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
            if (!currentRoute.isRoute("com.silas.omaster.Screen.Home")) {
                navController.popBackStack(Screen.Home, false)
            }
        }
        "subscription" -> {
            if (!currentRoute.isRoute("com.silas.omaster.Screen.Subscription")) {
                navController.navigate(Screen.Subscription) {
                    popUpTo(Screen.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        "features" -> {
            if (!currentRoute.isRoute("com.silas.omaster.Screen.CoreFeatures")) {
                navController.navigate(Screen.CoreFeatures) {
                    popUpTo(Screen.Home) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        "about" -> {
            if (!currentRoute.isRoute("com.silas.omaster.Screen.About")) {
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

/**
 * 类型安全的路由匹配辅助函数。
 * 使用 startsWith 替代 contains，避免子页面名称巧合包含主路由名。
 */
private fun String?.isRoute(routePrefix: String): Boolean {
    return this?.startsWith(routePrefix) == true
}


