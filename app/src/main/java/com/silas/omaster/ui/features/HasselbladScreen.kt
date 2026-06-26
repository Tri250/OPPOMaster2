package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.model.FilmPreset
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.ui.components.AnalysisStatus
import com.silas.omaster.ui.components.AnalysisStep
import com.silas.omaster.ui.components.ApertureState
import com.silas.omaster.ui.components.defaultAnalysisSteps
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.HasselbladOrangeLight
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.util.formatSigned
import com.silas.omaster.util.hapticClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "HasselbladEye"

enum class HasselbladEyeStage { SETUP, ANALYZING, RESULTS, PREVIEW, DONE }

data class AnalysisResult(
    val sceneProfile: SceneProfile,
    val alternativeScenes: List<SceneProfile>,
    val recommendedFilms: List<FilmPreset>,
    val masterTips: List<String>,
    val suggestedColorMode: String,
    val paramAdjustments: Map<String, Int>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladScreen(
    onBack: () -> Unit,
    onLaunchViewfinder: () -> Unit = {},
    // P2-1 修复：将当前 ViewModel 状态变化通知给上层（用于通过 savedStateHandle 传递到 CameraXViewfinder）
    onARGuideStateChanged: (guideType: String?, isEnabled: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel: HasselbladEyeViewModel = viewModel()
    val inferenceEngine = remember(context) { MasterInferenceEngine.getInstance(context) }

    val stage by viewModel.stage.collectAsState()
    val params by viewModel.params.collectAsState()
    val isParamsLocked by viewModel.isParamsLocked.collectAsState()
    val selectedSceneModeId by viewModel.selectedSceneModeId.collectAsState()
    val selectedColorModeId by viewModel.selectedColorModeId.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val apertureState by viewModel.apertureState.collectAsState()
    val originalBitmap by viewModel.originalBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val thumbnailPreview by viewModel.thumbnailPreview.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val lastSavedUri by viewModel.lastSavedUri.collectAsState()
    val analysisSteps by viewModel.analysisSteps.collectAsState()
    val analysisMessage by viewModel.analysisMessage.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()

    // AI 构图引导状态
    val appliedCompositionGuideId by viewModel.appliedCompositionGuideId.collectAsState()
    val appliedARGuideType by viewModel.appliedARGuideType.collectAsState()
    val isARGuideEnabled by viewModel.isARGuideEnabled.collectAsState()

    // P2-1 修复：将哈苏构图引导线状态同步给上层（AppNavigation），
    // 用于跳转到 CameraXViewfinder 时通过 savedStateHandle 传递
    LaunchedEffect(appliedARGuideType, isARGuideEnabled) {
        val typeName = appliedARGuideType?.name
        onARGuideStateChanged(typeName, isARGuideEnabled)
    }

    // 操作错误状态：保存/分享失败时弹出 Toast 提示
    val operationError by viewModel.operationError.collectAsState()
    LaunchedEffect(operationError) {
        operationError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearOperationError()
        }
    }

    // 应用到 OPPO 大师模式相机状态：成功/失败时弹出 Toast 提示
    val oppoApplyState by viewModel.oppoApplyState.collectAsState()
    LaunchedEffect(oppoApplyState) {
        when (val state = oppoApplyState) {
            is HasselbladEyeViewModel.OPOApplyState.Success -> {
                val methodLabel = when (state.method) {
                    "CONTENT_PROVIDER" -> "ContentProvider"
                    "SYSTEM_SETTINGS" -> "系统设置"
                    "CAMERA_INTENT" -> "相机 Intent"
                    "CLIPBOARD_FALLBACK" -> "剪贴板"
                    else -> state.method
                }
                Toast.makeText(
                    context,
                    "已应用到 OPPO 大师模式（$methodLabel）",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetOPOApplyState()
            }
            is HasselbladEyeViewModel.OPOApplyState.PartialSuccess -> {
                val failedList = state.failedParams.joinToString("、")
                Toast.makeText(context, "部分参数应用成功，以下参数失败：$failedList", Toast.LENGTH_LONG).show()
                viewModel.resetOPOApplyState()
            }
            is HasselbladEyeViewModel.OPOApplyState.Failed -> {
                Toast.makeText(
                    context,
                    "应用失败：${state.reason}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetOPOApplyState()
            }
            else -> Unit
        }
    }

    // 检查 LUTManager 中是否有待应用的活跃 LUT（来自风格 LUT 生成器等入口）
    val lutManager = remember { LUTManager.getInstance(context) }
    LaunchedEffect(Unit) {
        val activeLUTId = lutManager.activeLUTId.value
        if (activeLUTId != null) {
            val strength = lutManager.lutStrength.value
            viewModel.apply3DLUT(context, activeLUTId, strength)
            // 应用后清除活跃 LUT，避免重复应用
            lutManager.setActiveLUT(null)
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    // P3-6 修复：最近拍摄从 SharedPreferences 加载（跨页面重启保留）
    val recentShotsPrefs = remember { context.getSharedPreferences("hasselblad_eye", Context.MODE_PRIVATE) }
    var recentShots by remember {
        mutableStateOf<List<Uri>>(
            recentShotsPrefs.getString("recent_shots", "")
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?.map { Uri.parse(it) }
                ?: emptyList()
        )
    }
    // 权限二次引导对话框状态：首次拒绝后展示说明，勾选"不再询问"后引导跳转设置
    var showPermissionRationale by remember { mutableStateOf(false) }
    var shouldGoToSettings by remember { mutableStateOf(false) }
    // P3-7 修复：构图清空确认对话框状态
    var showClearCompositionDialog by remember { mutableStateOf(false) }
    // 当前 Activity 引用，用于判断 shouldShowRequestPermissionRationale
    val activity = context as? android.app.Activity

    // 跳转系统应用详情设置页（用于权限被永久拒绝场景）
    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 从设置页返回后，若已授权则可直接继续，否则保持当前状态
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            Toast.makeText(context, "相机权限已开启", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // 判断是否应该展示权限说明（用户未勾选"不再询问"）
            val rationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it, android.Manifest.permission.CAMERA
                )
            } ?: false
            if (rationale) {
                // 可再次请求：展示说明对话框
                showPermissionRationale = true
                shouldGoToSettings = false
            } else {
                // 已被永久拒绝：引导跳转设置页
                showPermissionRationale = true
                shouldGoToSettings = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                scope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmapFromUri(context, uri)
                    }
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            // P3-6 修复：持久化最近 3 张
                            val updated = (listOf(uri) + recentShots).take(3)
                            recentShots = updated
                            recentShotsPrefs.edit().putString(
                                "recent_shots",
                                updated.joinToString("|") { it.toString() }
                            ).apply()
                        }
                        // startAnalysis 内部已用 viewModelScope.launch 启动协程，无需外层调度器
                        viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(context, it)
                }
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        // P3-6 修复：持久化最近 3 张
                        val updated = (listOf(it) + recentShots).take(3)
                        recentShots = updated
                        recentShotsPrefs.edit().putString(
                            "recent_shots",
                            updated.joinToString("|") { it.toString() }
                        ).apply()
                    }
                    viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun launchCamera() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val photoUri = createTempImageUri(context)
            if (photoUri != null) {
                cameraImageUri = photoUri
                cameraLauncher.launch(photoUri)
            } else {
                Toast.makeText(context, "无法创建相机临时文件", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun onPickFromGallery() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun onSceneModeSelected(sceneMode: SceneMode) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateSelectedSceneMode(sceneMode.id, sceneMode.params)
        if (isParamsLocked) {
            Toast.makeText(context, "参数已锁定：场景模式仅作参考，未覆盖当前参数", Toast.LENGTH_SHORT).show()
        }
    }

    fun onColorModeSelected(colorMode: ColorMode) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateSelectedColorMode(colorMode.id, colorMode.params)
        if (isParamsLocked) {
            Toast.makeText(context, "参数已锁定：色彩模式仅作参考，未覆盖当前参数", Toast.LENGTH_SHORT).show()
        }
    }

    fun onParamChanged(key: String, value: Int) {
        viewModel.updateParam(key, value)
    }

    fun onPreviewEffect() {
        val source = originalBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.generateFullPreview(source, emptyMap())
    }

    fun onSaveImage() {
        val bitmap = previewBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.saveImage(context, bitmap, exportFormat)
    }

    fun onShareImage() {
        val bitmap = previewBitmap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.shareImage(context, bitmap, exportFormat)
    }

    fun onResetParams() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.resetToRecommended()
    }

    fun onToggleLock() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setParamsLocked(!isParamsLocked)
    }

    fun onRetake() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.clear()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (stage) {
                            HasselbladEyeStage.SETUP -> "哈苏之眼"
                            HasselbladEyeStage.ANALYZING -> "哈苏之眼 · 分析中"
                            HasselbladEyeStage.RESULTS -> "哈苏之眼 · 分析结果"
                            HasselbladEyeStage.PREVIEW -> "哈苏之眼 · 预览"
                            HasselbladEyeStage.DONE -> "哈苏之眼 · 已完成"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (stage) {
                            HasselbladEyeStage.RESULTS,
                            HasselbladEyeStage.PREVIEW,
                            HasselbladEyeStage.DONE -> onRetake()
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)) togetherWith
                    fadeOut(animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f))
            },
            label = "hasselblad_stage",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentStage: HasselbladEyeStage ->
            when (currentStage) {
                HasselbladEyeStage.SETUP -> SetupContent(
                recentShots = recentShots,
                onLaunchCamera = ::launchCamera,
                onPickFromGallery = ::onPickFromGallery,
                onLaunchViewfinder = onLaunchViewfinder,
                onRecentShotClick = { uri ->
                    scope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            loadBitmapFromUri(context, uri)
                        }
                        if (bitmap != null) {
                            viewModel.startAnalysis(bitmap, inferenceEngine, allColorModes)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

                HasselbladEyeStage.ANALYZING -> AnalyzingContent(
                    apertureState = apertureState,
                    progress = analysisProgress,
                    message = analysisMessage,
                    steps = analysisSteps,
                    onCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.cancelAnalysis()
                    }
                )

                HasselbladEyeStage.RESULTS -> ResultsContent(
                    sceneModes = allSceneModes,
                    colorModes = allColorModes,
                    selectedSceneModeId = selectedSceneModeId,
                    selectedColorModeId = selectedColorModeId,
                    params = params,
                    isParamsLocked = isParamsLocked,
                    originalBitmap = originalBitmap,
                    thumbnailPreview = thumbnailPreview,
                    result = analysisResult,
                    appliedGuideId = appliedCompositionGuideId,
                    onSceneModeSelected = ::onSceneModeSelected,
                    onColorModeSelected = ::onColorModeSelected,
                    onParamChanged = ::onParamChanged,
                    onParamsLockedChanged = { onToggleLock() },
                    onPreviewEffect = ::onPreviewEffect,
                    onResetParams = ::onResetParams,
                    onFilmClick = { film ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyFilmPreset(film.id)
                    },
                    onFineGrainedSceneSelected = { sceneProfile ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyFineGrainedScene(sceneProfile)
                        if (isParamsLocked) {
                            Toast.makeText(context, "参数已锁定：仅切换参考模式，未覆盖当前参数", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onGuideClick = { guide ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyCompositionGuide(guide)
                        Toast.makeText(
                            context,
                            "已应用构图：${guide.name}，AR引导线已开启",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClearComposition = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // P3-7 修复：弹出确认对话框
                        showClearCompositionDialog = true
                    },
                    onSaveAsPreset = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val currentColorMode = allColorModes.find { it.id == selectedColorModeId }
                        val currentSceneMode = allSceneModes.find { it.id == selectedSceneModeId }
                        if (currentColorMode != null) {
                            scope.launch {
                                val repository = PresetRepository.getInstance(context)
                                val result = repository.createPresetFromHasselbladMode(
                                    colorMode = currentColorMode,
                                    sceneMode = currentSceneMode
                                )
                                result.onSuccess {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "已保存为预设：${currentColorMode.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "保存失败：${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "请先选择一个色彩模式", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onApplyToOPPO = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // P2-5 修复：应用参数 + 联动悬浮窗服务
                        viewModel.applyToOPPOAndShowFloating(context)
                    }
                )

                HasselbladEyeStage.PREVIEW -> PreviewContent(
                    originalBitmap = originalBitmap,
                    previewBitmap = previewBitmap,
                    params = params,
                    exportFormat = exportFormat,
                    isSaving = isSaving,
                    // P2-4 修复：传递 LUT 状态
                    activeLUTId = viewModel.active3DLUTId.collectAsState().value,
                    lutStrength = viewModel.lut3DStrength.collectAsState().value,
                    onLUTStrengthChange = { viewModel.update3DLUTStrength(it) },
                    onLUTRemove = { viewModel.remove3DLUT() },
                    onExportFormatChanged = { viewModel.setExportFormat(it) },
                    onSave = ::onSaveImage,
                    onShare = ::onShareImage,
                    onRetake = ::onRetake
                )

                HasselbladEyeStage.DONE -> DoneContent(
                    onShare = ::onShareImage,
                    onViewImage = {
                        val uri = lastSavedUri
                        if (uri != null) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "image/*")
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开相册", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "图片未保存", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditAnother = {
                        viewModel.clear()
                    },
                    onRetake = ::onRetake,
                    onBack = onBack
                )
            }
        }
    }

    // 权限二次引导对话框：首次拒绝展示说明，永久拒绝引导跳转设置
    if (showPermissionRationale) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPermissionRationale = false
            },
            title = {
                Text(
                    text = "相机权限说明",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (shouldGoToSettings) {
                        "哈苏之眼需要相机权限才能拍照分析。\n\n您已选择\u201C不再询问\u201D，请前往设置页手动开启相机权限后返回使用。"
                    } else {
                        "哈苏之眼需要相机权限才能拍照并进行场景分析、AI构图引导。\n\n开启后即可使用哈苏大师色彩科学与AR构图引导功能。"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        if (shouldGoToSettings) {
                            // 跳转应用详情设置页
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            appSettingsLauncher.launch(intent)
                        } else {
                            // 重新请求权限
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text(
                        text = if (shouldGoToSettings) "去设置" else "重新授权",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPermissionRationale = false }
                ) {
                    Text("暂不使用")
                }
            }
        )
    }

    // P3-7 修复：构图清空确认对话框
    if (showClearCompositionDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearCompositionDialog = false },
            title = {
                Text(
                    text = "清除当前构图？",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("清除后将丢失当前应用的构图引导方案与AR引导线设置。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCompositionDialog = false
                        viewModel.clearAppliedComposition()
                        Toast.makeText(context, "已清除当前构图", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("确认清除", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearCompositionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 辅助组件：GlassCard / ColorOS 16 动效 ====================

/**
 * P2-14：ColorOS 16 风格的入场动效修饰符
 * 给卡片添加淡入 + 轻微上滑的弹簧入场效果。
 */
@Composable
private fun Modifier.colorOsEntrance(delayMillis: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.EaseOutCubic),
        label = "entrance_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "entrance_offset"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // P2-14：ColorOS 16 液态玻璃近似效果（多层渐变 + 高光描边）
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val highlightBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.02f),
            Color.Transparent
        )
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            0.5.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .background(highlightBrush)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

// ==================== SETUP 阶段 ====================

@Composable
private fun SetupContent(
    recentShots: List<Uri>,
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onLaunchViewfinder: () -> Unit,
    onRecentShotClick: (Uri) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard() }

        item {
            ShutterCard(
                onLaunchCamera = onLaunchCamera,
                onPickFromGallery = onPickFromGallery
            )
        }

        // 实时取景拍摄入口：直接进入 CameraX 取景器进行实时预览与拍摄
        item {
            ViewfinderEntryCard(onLaunchViewfinder = onLaunchViewfinder)
        }

        // AR 取景器模拟：拍照前预览构图引导线
        item {
            ViewfinderSimulatorCard()
        }

        if (recentShots.isNotEmpty()) {
            item {
                SectionTitle(title = "最近拍摄")
            }
            item {
                RecentShotsRow(
                    recentShots = recentShots,
                    onShotClick = onRecentShotClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(HasselbladOrange, HasselbladOrangeLight)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "相机图标",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "哈苏之眼 · HNCS 3.0",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OPPO Find X9 系列 · 哈苏大师影像",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "真实场景识别 · 哈苏大师参数 · HNCS 3.0 自然色彩科学",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * AR 取景器模拟卡片
 * 拍照前预览不同场景的 AR 构图引导线，模拟真实取景器叠加效果
 * 用户可切换场景模式与引导线类型，提前了解构图方案
 */
@Composable
private fun ViewfinderSimulatorCard() {
    var selectedSceneMode by remember { mutableStateOf(CompositionSceneMode.TRAVEL) }
    var selectedGuideType by remember { mutableStateOf(ARGuideType.THIRDS) }
    // 根据场景模式自动推荐引导线
    LaunchedEffect(selectedSceneMode) {
        val recommendedGuideId = selectedSceneMode.recommendedGuides.firstOrNull()
        val guide = compositionGuideLibrary.find { it.id == recommendedGuideId }
        guide?.let { selectedGuideType = it.arGuideType }
    }

    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AR 取景器模拟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "拍照前预览构图引导线，选择场景与引导方式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 场景模式选择
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CompositionSceneMode.entries) { mode ->
                    FilterChip(
                        selected = selectedSceneMode == mode,
                        onClick = { selectedSceneMode = mode },
                        label = {
                            Text(
                                text = "${mode.icon} ${mode.displayName}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                            selectedLabelColor = HasselbladOrange
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 引导线类型选择
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ARGuideType.entries) { guideType ->
                    FilterChip(
                        selected = selectedGuideType == guideType,
                        onClick = { selectedGuideType = guideType },
                        label = {
                            Text(
                                text = guideType.displayName,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                            selectedLabelColor = HasselbladOrange
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // AR 引导线叠加预览（模拟取景器）
            ARGuideOverlay(
                guideType = selectedGuideType,
                sceneMode = selectedSceneMode
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 ${selectedSceneMode.arTip}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneModeChip(
    sceneMode: SceneMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = sceneMode.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sceneMode.name)
                }
                if (sceneMode.confidence > 0f) {
                    Text(
                        text = "${(sceneMode.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = HasselbladOrange,
            enabled = true,
            selected = isSelected
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorModeChip(
    mode: ColorMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(mode.color))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(mode.name)
                }
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) HasselbladOrange.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
            selectedLabelColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = HasselbladOrange,
            enabled = true,
            selected = isSelected
        )
    )
}

@Composable
private fun ParamsPanel(
    params: HasselbladParams,
    isParamsLocked: Boolean,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onResetParams: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isParamsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isParamsLocked) "参数已锁定" else "参数未锁定",
                    tint = if (isParamsLocked) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "锁定当前参数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "开启后切换场景/色彩模式不会覆盖已调参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = isParamsLocked,
                onCheckedChange = onParamsLockedChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HasselbladOrange,
                    checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ParamSlider(label = "影调", value = params.tone, enabled = !isParamsLocked, onValueChange = { onParamChanged("tone", it) })
        ParamSlider(label = "饱和度", value = params.saturation, enabled = !isParamsLocked, onValueChange = { onParamChanged("saturation", it) })
        ParamSlider(label = "对比度", value = params.contrast, enabled = !isParamsLocked, onValueChange = { onParamChanged("contrast", it) })
        ParamSlider(label = "色温", value = params.colorTemp, enabled = !isParamsLocked, onValueChange = { onParamChanged("colorTemp", it) })
        ParamSlider(label = "锐度", value = params.sharpness, enabled = !isParamsLocked, onValueChange = { onParamChanged("sharpness", it) })
        ParamSlider(label = "暗角", value = params.vignette, enabled = !isParamsLocked, onValueChange = { onParamChanged("vignette", it) })
        ParamSlider(label = "青品调", value = params.cyanMagenta, enabled = !isParamsLocked, onValueChange = { onParamChanged("cyanMagenta", it) })
        ParamSlider(label = "高光", value = params.highlights, enabled = !isParamsLocked, onValueChange = { onParamChanged("highlights", it) })
        ParamSlider(label = "阴影", value = params.shadows, enabled = !isParamsLocked, onValueChange = { onParamChanged("shadows", it) })
        ParamSlider(label = "清晰度", value = params.clarity, range = 0f..30f, enabled = !isParamsLocked, onValueChange = { onParamChanged("clarity", it) })

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "柔光模式",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(SoftLightMode.NONE, SoftLightMode.SOFT, SoftLightMode.DREAMY).forEach { softMode ->
                val isSoftSelected = params.softLight == softMode
                val label = when (softMode) {
                    SoftLightMode.NONE -> "无"
                    SoftLightMode.SOFT -> "柔"
                    SoftLightMode.DREAMY -> "梦幻"
                }
                FilterChip(
                    selected = isSoftSelected,
                    onClick = { if (!isParamsLocked) onParamChanged("softLight", softMode.ordinal) },
                    enabled = !isParamsLocked,
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                        selectedLabelColor = HasselbladOrange
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = HasselbladOrange,
                        enabled = !isParamsLocked,
                        selected = isSoftSelected
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onResetParams,
            enabled = !isParamsLocked,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, HasselbladOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "重置参数",
                tint = HasselbladOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "重置为推荐参数", color = HasselbladOrange)
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float> = -30f..30f,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (enabled) HasselbladOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = value.formatSigned(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                thumbColor = HasselbladOrange,
                disabledActiveTrackColor = HasselbladOrange.copy(alpha = 0.3f),
                disabledThumbColor = HasselbladOrange.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ShutterCard(
    onLaunchCamera: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "选择一张照片，让哈苏之眼为你分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onLaunchCamera,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = CircleShape,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { shadowElevation = 20.dp.toPx() },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击拍照",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onPickFromGallery,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "从相册选择",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "从相册选择", color = HasselbladOrange)
            }
        }
    }
}

/**
 * 实时取景拍摄入口卡片
 * 进入 CameraX 取景器进行实时预览、对焦、变焦与拍照（自动应用哈苏色彩）
 */
@Composable
private fun ViewfinderEntryCard(onLaunchViewfinder: () -> Unit) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hapticClickable { onLaunchViewfinder() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "实时取景拍摄",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "实时取景拍摄",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "CameraX 实时预览 · 点击对焦 · 双指缩放 · 哈苏色彩",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f)
            )
        }
    }
}

@Composable
private fun RecentShotsRow(
    recentShots: List<Uri>,
    onShotClick: (Uri) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(recentShots) { uri ->
            RecentShotThumbnail(uri = uri, onClick = { onShotClick(uri) })
        }
    }
}

@Composable
private fun RecentShotThumbnail(
    uri: Uri,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        thumbnail = withContext(Dispatchers.IO) {
            loadBitmapFromUri(context, uri, maxDimension = 200)
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(72.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "最近拍摄",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ==================== ANALYZING 阶段 ====================

@Composable
private fun AnalyzingContent(
    apertureState: ApertureState,
    progress: Float,
    message: String,
    steps: List<AnalysisStep>,
    onCancel: () -> Unit
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val infiniteRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 2000),
        label = "aperture_rotation"
    )

    LaunchedEffect(Unit) {
        while (true) {
            rotationAngle += 360f
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .drawBehind {
                        drawCircle(
                            color = HasselbladOrange.copy(alpha = 0.3f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )

            val bladeCount = 8
            val openingFactor = when (apertureState) {
                ApertureState.OPEN -> 0.3f
                ApertureState.OPENING -> 0.5f
                else -> 1f
            }

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .rotate(infiniteRotation)
            ) {
                for (i in 0 until bladeCount) {
                    val angle = (i * 45f)
                    val alpha = 0.6f - (i * 0.05f)
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .rotate(angle)
                            .drawBehind {
                                val bladeWidth = 20.dp.toPx() * openingFactor
                                val bladeLength = 40.dp.toPx() * openingFactor
                                drawPath(
                                    path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(size.width / 2, size.height / 2)
                                        lineTo(
                                            size.width / 2 + bladeLength,
                                            size.height / 2 - bladeWidth / 2
                                        )
                                        lineTo(
                                            size.width / 2 + bladeLength,
                                            size.height / 2 + bladeWidth / 2
                                        )
                                        close()
                                    },
                                    color = HasselbladOrange.copy(alpha = alpha)
                                )
                            }
                    )
                }
            }

            val centerSize = when (apertureState) {
                ApertureState.OPEN -> 8.dp
                else -> 4.dp
            }
            Box(
                modifier = Modifier
                    .size(centerSize)
                    .clip(CircleShape)
                    .background(HasselbladOrange)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(HasselbladOrange, HasselbladOrangeLight, Color(0xFFFFB366))
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分析进度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Text(
                text = "${progress.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            steps.forEach { step ->
                AnalysisStepRow(step = step)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "取消分析",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "取消分析")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HNCS · HASSELBLAD NATURAL COLOR SOLUTION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun AnalysisStepRow(step: AnalysisStep) {
    val backgroundColor = when (step.status) {
        AnalysisStatus.COMPLETED -> HasselbladOrange.copy(alpha = 0.1f)
        AnalysisStatus.PROCESSING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        AnalysisStatus.PENDING -> Color.Transparent
    }

    val textColor = when (step.status) {
        AnalysisStatus.COMPLETED -> HasselbladOrange
        AnalysisStatus.PROCESSING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        AnalysisStatus.PENDING -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (step.status) {
            AnalysisStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "完成",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
            AnalysisStatus.PROCESSING -> {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(16.dp),
                    color = HasselbladOrange,
                    strokeWidth = 2.dp,
                    trackColor = HasselbladOrange.copy(alpha = 0.3f)
                )
            }
            AnalysisStatus.PENDING -> {
                val pendingRingColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .drawBehind {
                            drawCircle(
                                color = pendingRingColor,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                )
            }
        }
        Text(text = step.name, color = textColor, fontSize = 14.sp)
        if (step.status == AnalysisStatus.COMPLETED) {
            Text(
                text = "完成",
                color = HasselbladOrange.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

// ==================== RESULTS 阶段 ====================

@Composable
private fun ResultsContent(
    sceneModes: List<SceneMode>,
    colorModes: List<ColorMode>,
    selectedSceneModeId: String,
    selectedColorModeId: String,
    params: HasselbladParams,
    isParamsLocked: Boolean,
    originalBitmap: Bitmap?,
    thumbnailPreview: Bitmap?,
    result: AnalysisResult?,
    appliedGuideId: String?,
    onSceneModeSelected: (SceneMode) -> Unit,
    onColorModeSelected: (ColorMode) -> Unit,
    onParamChanged: (String, Int) -> Unit,
    onParamsLockedChanged: (Boolean) -> Unit,
    onPreviewEffect: () -> Unit,
    onResetParams: () -> Unit,
    onFilmClick: (FilmPreset) -> Unit,
    onFineGrainedSceneSelected: (SceneProfile) -> Unit,
    onGuideClick: (CompositionGuide) -> Unit,
    onClearComposition: () -> Unit,
    onSaveAsPreset: () -> Unit = {},
    onApplyToOPPO: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (result != null) {
            item {
                ResultPhotoCard(bitmap = originalBitmap, preview = thumbnailPreview)
            }

            item {
                SceneRecognitionCard(profile = result.sceneProfile)
            }

            // P1-10：展示 Top-3 备选场景，支持一键切换
            if (result.alternativeScenes.isNotEmpty()) {
                item {
                    SectionTitle(title = "备选场景")
                }
                item {
                    AlternativeScenesRow(
                        alternatives = result.alternativeScenes,
                        onSceneSelected = onFineGrainedSceneSelected
                    )
                }
            }

            if (result.masterTips.isNotEmpty()) {
                item {
                    SectionTitle(title = "哈苏大师建议")
                }
                item {
                    MasterTipsCard(tips = result.masterTips)
                }
            }

            // AI构图技巧（参考DOKA算法）
            item {
                SectionTitle(title = "AI构图技巧")
            }
            item {
                CompositionGuideCard(
                    sceneCategory = result.sceneProfile.category,
                    appliedGuideId = appliedGuideId,
                    onGuideClick = onGuideClick,
                    onClearComposition = onClearComposition
                )
            }

            item {
                SectionTitle(title = "推荐胶片")
            }
            item {
                RecommendedFilmsRow(
                    films = result.recommendedFilms,
                    onFilmClick = onFilmClick
                )
            }

            item {
                SectionTitle(title = "场景模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sceneModes) { scene ->
                        SceneModeChip(
                            sceneMode = scene,
                            isSelected = selectedSceneModeId == scene.id,
                            onClick = { onSceneModeSelected(scene) }
                        )
                    }
                }
            }

            // P1-10：当前粗粒度模式展开 2-4 个细分子模式
            item {
                SubSceneModesPanel(
                    selectedSceneModeId = selectedSceneModeId,
                    currentSceneId = result.sceneProfile.id,
                    onSceneSelected = onFineGrainedSceneSelected
                )
            }

            item {
                SectionTitle(title = "色彩模式")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorModes) { mode ->
                        ColorModeChip(
                            mode = mode,
                            isSelected = selectedColorModeId == mode.id,
                            onClick = { onColorModeSelected(mode) }
                        )
                    }
                }
            }

            item {
                SectionTitle(title = "哈苏大师参数")
            }
            item {
                ParamsPanel(
                    params = params,
                    isParamsLocked = isParamsLocked,
                    onParamChanged = onParamChanged,
                    onParamsLockedChanged = onParamsLockedChanged,
                    onResetParams = onResetParams
                )
            }

            item {
                Button(
                    onClick = onPreviewEffect,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "生成预览图",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "生成预览图",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onSaveAsPreset,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HasselbladOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存为预设",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存为预设",
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Button(
                    onClick = onApplyToOPPO,
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "应用到 OPPO 相机",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "应用到 OPPO 相机",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无分析结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ResultPhotoCard(bitmap: Bitmap?, preview: Bitmap?) {
    val displayBitmap = preview ?: bitmap
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (preview != null) "实时预览" else "原图预览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = if (preview != null) "实时预览" else "原图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "暂无图片",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneRecognitionCard(profile: SceneProfile) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HasselbladOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = profile.category.icon, fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "识别置信度 ${(profile.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MasterTipsCard(tips: List<String>) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tips.forEachIndexed { index, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedFilmsRow(
    films: List<FilmPreset>,
    onFilmClick: (FilmPreset) -> Unit
) {
    if (films.isEmpty()) {
        GlassCard {
            Text(
                text = "暂无推荐胶片",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(films) { film ->
            FilmCard(film = film, onClick = { onFilmClick(film) })
        }
    }
}

@Composable
private fun FilmCard(
    film: FilmPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .hapticClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = film.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${(film.matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "色彩风格：${film.colorStyle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = "颗粒感：${film.grainLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = "对比度：${film.contrastLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

// P1-10：备选场景横向列表
@Composable
private fun AlternativeScenesRow(
    alternatives: List<SceneProfile>,
    onSceneSelected: (SceneProfile) -> Unit
) {
    val top3 = alternatives.take(3)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(top3) { scene ->
            AlternativeSceneCard(scene = scene, onClick = { onSceneSelected(scene) })
        }
    }
}

@Composable
private fun AlternativeSceneCard(
    scene: SceneProfile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = scene.category.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scene.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "置信度 ${(scene.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = HasselbladOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scene.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// P1-10：当前粗粒度模式下的细分子模式面板
@Composable
private fun SubSceneModesPanel(
    selectedSceneModeId: String,
    currentSceneId: String,
    onSceneSelected: (SceneProfile) -> Unit
) {
    val subScenes = getSubSceneProfiles(selectedSceneModeId)
    if (subScenes.isEmpty()) return

    // 优先展示 2-4 个细分子模式，当前识别到的排最前
    val sorted = subScenes.sortedByDescending { it.id == currentSceneId }
    val displayScenes = sorted.take(4)

    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "细分子模式",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(displayScenes) { scene ->
                    val isCurrent = scene.id == currentSceneId
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onSceneSelected(scene) },
                        label = {
                            Column {
                                Text(
                                    text = scene.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = scene.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                            selectedLabelColor = HasselbladOrange
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = HasselbladOrange,
                            enabled = true,
                            selected = isCurrent
                        )
                    )
                }
            }
        }
    }
}

// ==================== PREVIEW 阶段 ====================

@Composable
private fun PreviewContent(
    originalBitmap: Bitmap?,
    previewBitmap: Bitmap?,
    params: HasselbladParams,
    exportFormat: HasselbladEyeViewModel.ExportFormat,
    isSaving: Boolean,
    onExportFormatChanged: (HasselbladEyeViewModel.ExportFormat) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRetake: () -> Unit,
    // P2-4 修复：3D LUT 控制回调
    activeLUTId: String? = null,
    lutStrength: Float = 1.0f,
    onLUTStrengthChange: (Float) -> Unit = {},
    onLUTRemove: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BeforeAfterCompareCard(
                original = originalBitmap,
                processed = previewBitmap
            )
        }

        item {
            SectionTitle(title = "参数摘要")
        }
        item {
            ParamsSummaryCard(params = params)
        }

        item {
            SectionTitle(title = "导出格式")
        }
        item {
            ExportFormatSelector(
                currentFormat = exportFormat,
                onFormatChanged = onExportFormatChanged
            )
        }

        // P2-4 修复：3D LUT 强度滑块（仅在有活跃 LUT 时显示）
        if (activeLUTId != null) {
            item {
                SectionTitle(title = "3D LUT 强度")
            }
            item {
                LUTStrengthCard(
                    lutId = activeLUTId,
                    strength = lutStrength,
                    onStrengthChange = onLUTStrengthChange,
                    onRemove = onLUTRemove
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Button(
                onClick = onSave,
                enabled = !isSaving && previewBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "保存中...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存到相册",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "保存到相册",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onShare,
                    enabled = previewBitmap != null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, HasselbladOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "分享",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "分享", color = HasselbladOrange)
                }
                OutlinedButton(
                    onClick = onRetake,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "重新拍摄",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "重新拍摄")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun BeforeAfterCompareCard(original: Bitmap?, processed: Bitmap?) {
    val haptic = LocalHapticFeedback.current
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "前后对比 · 滑动查看差异",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val maxW = constraints.maxWidth
                val maxH = constraints.maxHeight
                var splitFraction by remember { mutableFloatStateOf(0.5f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ) { _, dragAmount ->
                                splitFraction = (splitFraction + dragAmount.x / maxW).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    // 底层：处理后图（右侧）
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (processed != null) {
                            Image(
                                bitmap = processed.asImageBitmap(),
                                contentDescription = "哈苏处理",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 上层裁剪：原图（左侧）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(splitFraction)
                            .fillMaxHeight()
                    ) {
                        if (original != null) {
                            Image(
                                bitmap = original.asImageBitmap(),
                                contentDescription = "原图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .offset(x = with(LocalDensity.current) { (maxW * splitFraction - 1.5f).toDp() })
                            .background(HasselbladOrange)
                    )

                    // 拖动圆点 + 百分比
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { (maxW * splitFraction - 16).toDp() },
                                y = with(LocalDensity.current) { (maxH / 2 - 16).toDp() }
                            )
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(splitFraction * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 标签：左侧原图，右侧哈苏
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "原图 ORIGINAL", color = Color.White, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HasselbladOrange.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "哈苏 HASSELBLAD", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamsSummaryCard(params: HasselbladParams) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val paramPairs = listOf(
                "影调" to params.tone,
                "饱和度" to params.saturation,
                "对比度" to params.contrast,
                "色温" to params.colorTemp,
                "锐度" to params.sharpness,
                "暗角" to params.vignette,
                "青品调" to params.cyanMagenta,
                "高光" to params.highlights,
                "阴影" to params.shadows,
                "清晰度" to params.clarity
            )
            paramPairs.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, value) ->
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = value.formatSigned(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HasselbladOrange
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "柔光模式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Surface(
                    color = HasselbladOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (params.softLight) {
                            SoftLightMode.NONE -> "无"
                            SoftLightMode.SOFT -> "柔"
                            SoftLightMode.DREAMY -> "梦幻"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatSelector(
    currentFormat: HasselbladEyeViewModel.ExportFormat,
    onFormatChanged: (HasselbladEyeViewModel.ExportFormat) -> Unit
) {
    val formats = listOf(
        HasselbladEyeViewModel.ExportFormat.JPEG to "JPEG",
        HasselbladEyeViewModel.ExportFormat.PNG to "PNG",
        HasselbladEyeViewModel.ExportFormat.WEBP to "WEBP"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(formats) { (format, label) ->
            FilterChip(
                selected = currentFormat == format,
                onClick = { onFormatChanged(format) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                    selectedLabelColor = HasselbladOrange
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = HasselbladOrange,
                    enabled = true,
                    selected = currentFormat == format
                )
            )
        }
    }
}

/**
 * P2-4 修复：3D LUT 强度调节卡片
 * 提供滑块 UI，让用户在 PREVIEW 阶段调节已应用的 LUT 强度（0-100%）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LUTStrengthCard(
    lutId: String,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    onRemove: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "当前 LUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = lutId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladOrange
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemove()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "移除 LUT",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "0%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Slider(
                    value = strength,
                    onValueChange = onStrengthChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = HasselbladOrange,
                        activeTrackColor = HasselbladOrange,
                        inactiveTrackColor = HasselbladOrange.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(strength * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==================== DONE 阶段 ====================

@Composable
private fun DoneContent(
    onShare: () -> Unit,
    onViewImage: () -> Unit,
    onEditAnother: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(HasselbladOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "完成",
                tint = HasselbladOrange,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "已保存到相册",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "图片已保存至 Pictures/OMaster/Hasselblad/ 目录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "分享图片",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "分享图片", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewImage,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "查看图片",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "查看图片", color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onEditAnother,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                border = BorderStroke(1.dp, HasselbladOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "再调一张",
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "再调一张", color = HasselbladOrange, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRetake,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "重新拍摄",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "重新拍摄", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text(text = "返回")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HNCS 3.0",
                color = HasselbladOrange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HASSELBLAD NATURAL COLOR SOLUTION",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==================== 文件工具函数 ====================

private suspend fun loadBitmapFromUri(
    context: android.content.Context,
    uri: Uri,
    maxDimension: Int = 2048
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }
        if (maxDimension > 0 && options.outWidth > 0 && options.outHeight > 0) {
            var inSampleSize = 1
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
            options.inSampleSize = inSampleSize
        }
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        null
    }
}

private fun createTempImageUri(context: android.content.Context): Uri? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // 优先使用外部文件目录（已配置 FileProvider 路径 Pictures/Hasselblad/）
        // 若外部目录不可用（getExternalFilesDir 返回 null），回退到缓存目录（已配置 camera/ 路径）
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = if (externalDir != null) {
            val storageDir = File(externalDir, "Hasselblad").apply {
                if (!exists()) mkdirs()
            }
            File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
        } else {
            val cacheDir = File(context.cacheDir, "camera").apply {
                if (!exists()) mkdirs()
            }
            File.createTempFile("IMG_${timeStamp}_", ".jpg", cacheDir)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        android.util.Log.e("HasselbladScreen", "创建相机临时文件失败", e)
        null
    }
}

// ==================== AI构图技巧模块（参考DOKA算法） ====================

/**
 * 构图指南数据模型
 * 参考DOKA软件的构图算法功能点：
 * - 三分法/黄金分割构图
 * - 引导线构图
 * - 对角线构图
 * - 居中对称构图
 * - 框架构图
 * - 黄金螺旋构图
 * - AR引导线叠加
 * - 场景智能推荐
 */
data class CompositionGuide(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val tips: List<String>,
    val applicableCategories: List<SceneCategory>,
    val difficulty: String, // "入门" / "进阶" / "大师"
    val arGuideType: ARGuideType, // AR引导线类型
    val sceneMode: CompositionSceneMode, // 适用场景模式
    val arOverlayDescription: String // AR引导线叠加描述
)

/**
 * AR引导线类型
 * 定义在相机预览画面上叠加的辅助线类型
 */
enum class ARGuideType(
    val displayName: String,
    val description: String
) {
    THIRDS("三分线", "画面横竖各三等分，4个交叉点为最佳主体位置"),
    GOLDEN_RATIO("黄金分割线", "1:1.618比例分割，比三分法更精准"),
    DIAGONAL("对角线", "两条对角线交叉，增强画面动感"),
    CENTER_CROSS("中心十字", "水平+垂直中线，适合居中对称构图"),
    SPIRAL("黄金螺旋", "斐波那契螺旋线，自然界最和谐比例"),
    FRAME("框架线", "内缩矩形框，提示画中画构图区域"),
    HORIZON("水平线", "水平参考线，确保地平线水平"),
    TRIANGLE("三角构图", "三角形顶点连线，稳定且有张力")
}

/**
 * 构图场景模式
 * 对应7大核心应用场景（对标OPPO Find X9哈苏大师）
 */
enum class CompositionSceneMode(
    val displayName: String,
    val icon: String,
    val description: String,
    val recommendedGuides: List<String>, // 推荐构图ID列表
    val arTip: String // AR引导提示
) {
    TRAVEL(
        displayName = "旅行摄影",
        icon = "🏔️",
        description = "山川湖海、城市地标、街头巷尾",
        recommendedGuides = listOf("rule-of-thirds", "leading-lines", "frame-in-frame", "center-symmetry"),
        arTip = "AI实时分析光线与构图，AR引导线提示最佳拍摄角度"
    ),
    PORTRAIT(
        displayName = "人像记录",
        icon = "👤",
        description = "聚会合影、个人写真、儿童成长",
        recommendedGuides = listOf("rule-of-thirds", "golden-ratio", "negative-space", "center-symmetry"),
        arTip = "人像模式自动优化肤色与光影，虚化背景突出主体"
    ),
    FOOD(
        displayName = "美食探店",
        icon = "🍽️",
        description = "餐厅打卡、菜品特写、咖啡拉花",
        recommendedGuides = listOf("diagonal", "rule-of-thirds", "center-symmetry", "golden-ratio"),
        arTip = "AI识别食物主体，自动调整焦段与亮度"
    ),
    PET(
        displayName = "宠物捕捉",
        icon = "🐾",
        description = "猫咪嬉戏、狗狗奔跑、互动瞬间",
        recommendedGuides = listOf("rule-of-thirds", "diagonal", "leading-lines", "negative-space"),
        arTip = "AR引导线稳定追踪动态主体，解决手抖或构图偏差"
    ),
    NIGHT(
        displayName = "夜景星空",
        icon = "🌃",
        description = "城市夜景、星轨银河、车流光轨",
        recommendedGuides = listOf("rule-of-thirds", "center-symmetry", "leading-lines", "frame-in-frame"),
        arTip = "长曝光引导线辅助稳定构图，水平仪确保地平线水平"
    ),
    MACRO(
        displayName = "微距世界",
        icon = "🔍",
        description = "花卉昆虫、水滴纹理、珠宝细节",
        recommendedGuides = listOf("golden-ratio", "golden-spiral", "center-symmetry", "diagonal"),
        arTip = "景深引导框提示对焦区域，黄金螺旋突出微观主体"
    ),
    STREET(
        displayName = "城市街拍",
        icon = "🏢",
        description = "街头人文、建筑几何、城市光影",
        recommendedGuides = listOf("leading-lines", "diagonal", "frame-in-frame", "rule-of-thirds"),
        arTip = "引导线汇聚消失点，框架构图增强纵深感与故事性"
    )
}

/**
 * 基于DOKA算法的构图指南库
 * 根据场景类型推荐最佳构图方式
 */
private val compositionGuideLibrary: List<CompositionGuide> = listOf(
    CompositionGuide(
        id = "rule-of-thirds",
        name = "三分法构图",
        description = "将画面横竖各三等分，主体置于交叉点",
        icon = "⊞",
        tips = listOf(
            "将主体放在四分线交叉点上，视觉最稳定",
            "地平线放在上1/3或下1/3处，避免居中",
            "人像眼睛放在上1/3线上，增强视觉引导"
        ),
        applicableCategories = listOf(SceneCategory.PORTRAIT, SceneCategory.LANDSCAPE, SceneCategory.URBAN, SceneCategory.FOOD),
        difficulty = "入门",
        arGuideType = ARGuideType.THIRDS,
        sceneMode = CompositionSceneMode.TRAVEL,
        arOverlayDescription = "叠加三分线网格，4个交叉点高亮闪烁提示最佳主体位置"
    ),
    CompositionGuide(
        id = "golden-ratio",
        name = "黄金分割",
        description = "1:1.618黄金比例，比三分法更精准的视觉焦点",
        icon = "ϕ",
        tips = listOf(
            "黄金分割点比三分法更靠近中心，画面更紧凑",
            "适合需要突出主体同时保持背景的场景",
            "哈苏大师常用构图法，自然和谐"
        ),
        applicableCategories = listOf(SceneCategory.PORTRAIT, SceneCategory.STILL_LIFE, SceneCategory.LANDSCAPE),
        difficulty = "进阶",
        arGuideType = ARGuideType.GOLDEN_RATIO,
        sceneMode = CompositionSceneMode.PORTRAIT,
        arOverlayDescription = "叠加黄金分割线，4个黄金交叉点以圆圈标注"
    ),
    CompositionGuide(
        id = "leading-lines",
        name = "引导线构图",
        description = "利用线条引导视线至主体或消失点",
        icon = "⟋",
        tips = listOf(
            "道路、河流、栏杆都是天然引导线",
            "引导线从画面边缘指向主体，增强纵深感",
            "多条汇聚线形成消失点，营造强烈空间感"
        ),
        applicableCategories = listOf(SceneCategory.URBAN, SceneCategory.LANDSCAPE, SceneCategory.NIGHT),
        difficulty = "入门",
        arGuideType = ARGuideType.HORIZON,
        sceneMode = CompositionSceneMode.TRAVEL,
        arOverlayDescription = "检测画面中的引导线并高亮标注，箭头指向消失点"
    ),
    CompositionGuide(
        id = "diagonal",
        name = "对角线构图",
        description = "主体沿对角线分布，增加画面动感与张力",
        icon = "⟍",
        tips = listOf(
            "对角线构图比水平构图更有动感",
            "适合拍摄运动、建筑、食物等主题",
            "从左下到右上更符合阅读习惯"
        ),
        applicableCategories = listOf(SceneCategory.FOOD, SceneCategory.URBAN, SceneCategory.EVENT, SceneCategory.MACRO),
        difficulty = "入门",
        arGuideType = ARGuideType.DIAGONAL,
        sceneMode = CompositionSceneMode.FOOD,
        arOverlayDescription = "叠加两条对角线，主体建议放置于对角线1/3处"
    ),
    CompositionGuide(
        id = "center-symmetry",
        name = "居中对称",
        description = "主体居中，左右或上下对称，庄重稳定",
        icon = "◎",
        tips = listOf(
            "建筑、倒影等对称场景最佳选择",
            "确保对称轴完全居中，偏移会破坏效果",
            "可搭配广角镜头增强对称气势"
        ),
        applicableCategories = listOf(SceneCategory.LANDSCAPE, SceneCategory.URBAN, SceneCategory.STILL_LIFE),
        difficulty = "进阶",
        arGuideType = ARGuideType.CENTER_CROSS,
        sceneMode = CompositionSceneMode.TRAVEL,
        arOverlayDescription = "叠加中心十字线与对称轴，偏移时红色警示"
    ),
    CompositionGuide(
        id = "frame-in-frame",
        name = "框架构图",
        description = "利用前景元素形成画中画，增加层次感",
        icon = "⬜",
        tips = listOf(
            "门框、窗户、树枝都是天然框架",
            "框架不必完整，半框也能增强纵深感",
            "框架与主体形成明暗对比，突出主体"
        ),
        applicableCategories = listOf(SceneCategory.LANDSCAPE, SceneCategory.URBAN, SceneCategory.PORTRAIT),
        difficulty = "进阶",
        arGuideType = ARGuideType.FRAME,
        sceneMode = CompositionSceneMode.TRAVEL,
        arOverlayDescription = "叠加内缩矩形框，提示画中画构图区域"
    ),
    CompositionGuide(
        id = "golden-spiral",
        name = "黄金螺旋",
        description = "基于斐波那契螺旋线，自然界最和谐的比例",
        icon = "🌀",
        tips = listOf(
            "螺旋中心放置主体，线条引导视线流动",
            "适合花朵、贝壳等自然螺旋形态",
            "哈苏X系统经典构图法，大师级运用"
        ),
        applicableCategories = listOf(SceneCategory.MACRO, SceneCategory.STILL_LIFE, SceneCategory.LANDSCAPE),
        difficulty = "大师",
        arGuideType = ARGuideType.SPIRAL,
        sceneMode = CompositionSceneMode.PORTRAIT,
        arOverlayDescription = "叠加斐波那契螺旋线，螺旋中心高亮标注"
    ),
    CompositionGuide(
        id = "negative-space",
        name = "留白构图",
        description = "大面积留白衬托小主体，极简意境",
        icon = "◻",
        tips = listOf(
            "主体占画面1/4以下，留白占3/4以上",
            "留白方向给主体留出视线或运动空间",
            "东方美学核心构图法，少即是多"
        ),
        applicableCategories = listOf(SceneCategory.LANDSCAPE, SceneCategory.PORTRAIT, SceneCategory.STILL_LIFE),
        difficulty = "大师",
        arGuideType = ARGuideType.THIRDS,
        sceneMode = CompositionSceneMode.PET,
        arOverlayDescription = "叠加三分线并标注建议留白区域，主体位置高亮"
    ),
    CompositionGuide(
        id = "triangle",
        name = "三角构图",
        description = "三个视觉重点形成三角形，稳定且有张力",
        icon = "△",
        tips = listOf(
            "三个主体形成三角形分布，画面最稳定",
            "正三角稳定庄重，倒三角动感张力",
            "适合多人合影、建筑群、静物组合"
        ),
        applicableCategories = listOf(SceneCategory.PORTRAIT, SceneCategory.STILL_LIFE, SceneCategory.EVENT),
        difficulty = "进阶",
        arGuideType = ARGuideType.TRIANGLE,
        sceneMode = CompositionSceneMode.PORTRAIT,
        arOverlayDescription = "叠加三角形辅助线，三个顶点标注建议主体位置"
    ),
    CompositionGuide(
        id = "pet-tracking",
        name = "动态追踪构图",
        description = "AR引导线追踪运动主体，实时调整构图",
        icon = "🎯",
        tips = listOf(
            "AR引导线自动追踪宠物运动轨迹",
            "主体保持在三分线交叉点附近",
            "高帧率模式配合轻量化滤镜，快速抓拍灵动表情"
        ),
        applicableCategories = listOf(SceneCategory.PORTRAIT, SceneCategory.EVENT),
        difficulty = "进阶",
        arGuideType = ARGuideType.THIRDS,
        sceneMode = CompositionSceneMode.PET,
        arOverlayDescription = "AR动态追踪框锁定主体，三分线实时调整保持构图"
    ),
    CompositionGuide(
        id = "long-exposure",
        name = "长曝光构图",
        description = "稳定水平构图，适合夜景长曝光与光轨",
        icon = "🌌",
        tips = listOf(
            "使用三脚架或稳定支撑，避免长曝光抖动",
            "地平线严格水平，光轨沿引导线延伸",
            "前景留出空间，营造夜景纵深感"
        ),
        applicableCategories = listOf(SceneCategory.NIGHT, SceneCategory.URBAN, SceneCategory.LANDSCAPE),
        difficulty = "大师",
        arGuideType = ARGuideType.HORIZON,
        sceneMode = CompositionSceneMode.NIGHT,
        arOverlayDescription = "叠加水平仪与三分线，水平偏移时红色警示"
    ),
    CompositionGuide(
        id = "depth-of-field",
        name = "景深引导构图",
        description = "对焦区域框定主体，虚化背景突出细节",
        icon = "🔬",
        tips = listOf(
            "对焦框对准微观主体，确保焦点锐利",
            "背景远离主体以获得更柔和虚化",
            "黄金螺旋中心放置最精细的纹理细节"
        ),
        applicableCategories = listOf(SceneCategory.MACRO, SceneCategory.STILL_LIFE),
        difficulty = "大师",
        arGuideType = ARGuideType.SPIRAL,
        sceneMode = CompositionSceneMode.MACRO,
        arOverlayDescription = "对焦区域高亮框 + 黄金螺旋引导微观主体位置"
    ),
    CompositionGuide(
        id = "vanishing-point",
        name = "消失点构图",
        description = "引导线汇聚至消失点，营造强烈空间感",
        icon = "📐",
        tips = listOf(
            "寻找街道、走廊、铁轨等天然汇聚线",
            "消失点放置于三分线交叉点或黄金点",
            "前景人物或物体增加故事性与比例参考"
        ),
        applicableCategories = listOf(SceneCategory.URBAN, SceneCategory.LANDSCAPE, SceneCategory.NIGHT),
        difficulty = "进阶",
        arGuideType = ARGuideType.DIAGONAL,
        sceneMode = CompositionSceneMode.STREET,
        arOverlayDescription = "检测引导线并标注消失点，对角线辅助构图"
    )
)

/**
 * 根据场景类型和拍摄模式获取推荐的构图指南
 * 优先排序：场景模式推荐 > 场景类型匹配 > 通用
 */
private fun getRecommendedGuides(
    category: SceneCategory,
    sceneMode: CompositionSceneMode? = null
): List<CompositionGuide> {
    val sceneModePreferred = if (sceneMode != null) {
        compositionGuideLibrary.filter { it.sceneMode == sceneMode }
    } else emptyList()
    val categoryMatched = compositionGuideLibrary.filter { category in it.applicableCategories && it !in sceneModePreferred }
    val others = compositionGuideLibrary.filter { category !in it.applicableCategories && it !in sceneModePreferred && it !in categoryMatched }
    return sceneModePreferred + categoryMatched + others
}

/**
 * AI构图技巧卡片（参考DOKA算法）
 * 展示基于当前场景的推荐构图方式，支持场景模式切换和AR引导
 *
 * @param sceneCategory 当前 AI 识别出的场景分类
 * @param appliedGuideId 当前已应用的构图 ID（来自 ViewModel），用于显示"已应用"状态
 * @param onGuideClick 点击"应用构图"时回调，会真正写入 ViewModel 状态
 * @param onClearComposition 清除当前已应用的构图
 */
@Composable
private fun CompositionGuideCard(
    sceneCategory: SceneCategory,
    appliedGuideId: String? = null,
    onGuideClick: (CompositionGuide) -> Unit,
    onClearComposition: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var selectedSceneMode by remember { mutableStateOf(CompositionSceneMode.TRAVEL) }
    val guides = remember(sceneCategory, selectedSceneMode) {
        getRecommendedGuides(sceneCategory, selectedSceneMode)
    }
    // 安全边界：若推荐列表为空则显示空状态，避免 guides[0] 崩溃
    if (guides.isEmpty()) {
        GlassCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无推荐构图，请切换场景模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        return@CompositionGuideCard
    }
    var selectedGuideIndex by remember(guides) { mutableIntStateOf(0.coerceIn(0, (guides.size - 1).coerceAtLeast(0))) }
    val currentGuide = guides[selectedGuideIndex]
    // AR预览状态：默认关闭，应用构图后自动开启（与 ViewModel.isARGuideEnabled 保持单向同步）
    var showARPreview by remember(appliedGuideId, selectedGuideIndex) {
        mutableStateOf(appliedGuideId != null && appliedGuideId == currentGuide.id)
    }
    val isCurrentApplied = appliedGuideId == currentGuide.id

    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 场景模式选择标签
            Text(
                text = "拍摄场景",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(CompositionSceneMode.entries.toList()) { mode ->
                    FilterChip(
                        selected = mode == selectedSceneMode,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedSceneMode = mode
                            selectedGuideIndex = 0
                        },
                        label = {
                            Text(
                                text = "${mode.icon} ${mode.displayName}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                            selectedLabelColor = HasselbladOrange
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = HasselbladOrange,
                            enabled = true,
                            selected = mode == selectedSceneMode
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "场景模式：${mode.displayName}，${if (mode == selectedSceneMode) "已选中" else "未选中"}"
                        }
                    )
                }
            }

            // 场景描述
            Text(
                text = selectedSceneMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentGuide.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currentGuide.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentGuide.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // 难度标签
                Surface(
                    color = when (currentGuide.difficulty) {
                        "入门" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                        "进阶" -> HasselbladOrange.copy(alpha = 0.15f)
                        else -> Color(0xFF9C27B0).copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = currentGuide.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (currentGuide.difficulty) {
                            "入门" -> Color(0xFF4CAF50)
                            "进阶" -> HasselbladOrange
                            else -> Color(0xFF9C27B0)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AR引导线说明
            Surface(
                color = HasselbladOrange.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "AR引导",
                        tint = HasselbladOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AR ${currentGuide.arGuideType.displayName}：${currentGuide.arOverlayDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 构图技巧列表
            currentGuide.tips.forEachIndexed { index, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(HasselbladOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladOrange,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                if (index < currentGuide.tips.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 构图方式切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "构图方式列表，当前选择：${currentGuide.name}，难度${currentGuide.difficulty}"
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                guides.take(5).forEachIndexed { index, guide ->
                    FilterChip(
                        selected = index == selectedGuideIndex,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedGuideIndex = index
                        },
                        label = {
                            Text(
                                text = "${guide.icon} ${guide.name}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HasselbladOrange.copy(alpha = 0.15f),
                            selectedLabelColor = HasselbladOrange
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = HasselbladOrange,
                            enabled = true,
                            selected = index == selectedGuideIndex
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "构图方式：${guide.name}，${if (index == selectedGuideIndex) "已选中" else "未选中"}，难度${guide.difficulty}"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AR预览按钮
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showARPreview = !showARPreview
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (showARPreview) HasselbladOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "AR预览",
                        tint = if (showARPreview) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showARPreview) "关闭AR" else "AR引导",
                        color = if (showARPreview) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 应用构图按钮 - 根据 ViewModel 状态显示"应用构图"或"已应用"，颜色有过渡动画
                val buttonColor by animateColorAsState(
                    targetValue = if (isCurrentApplied) Color(0xFF4CAF50) else HasselbladOrange,
                    animationSpec = tween(durationMillis = 300),
                    label = "buttonColor"
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isCurrentApplied) {
                            // 已应用：再次点击则清除
                            onClearComposition()
                        } else {
                            // 未应用：写入 ViewModel 状态
                            onGuideClick(currentGuide)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentApplied) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.AutoAwesome
                        },
                        contentDescription = if (isCurrentApplied) "已应用" else "应用构图",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCurrentApplied) "已应用" else "应用构图",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // AR预览叠加区域
            if (showARPreview) {
                Spacer(modifier = Modifier.height(8.dp))
                ARGuideOverlay(
                    guideType = currentGuide.arGuideType,
                    sceneMode = selectedSceneMode
                )
            }
        }
    }
}

/**
 * AR引导线叠加预览组件
 * 在卡片内显示构图辅助线预览
 */
@Composable
private fun ARGuideOverlay(
    guideType: ARGuideType,
    sceneMode: CompositionSceneMode
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val guideColor = HasselbladOrange.copy(alpha = 0.7f)
            val pointColor = HasselbladOrange

            when (guideType) {
                ARGuideType.THIRDS -> {
                    // 三分线
                    for (i in 1..2) {
                        drawLine(guideColor, Offset(0f, h * i / 3), Offset(w, h * i / 3), strokeWidth = 1.5f)
                        drawLine(guideColor, Offset(w * i / 3, 0f), Offset(w * i / 3, h), strokeWidth = 1.5f)
                    }
                    // 交叉点
                    for (i in 1..2) for (j in 1..2) {
                        drawCircle(pointColor, radius = 5f, center = Offset(w * i / 3, h * j / 3))
                    }
                }
                ARGuideType.GOLDEN_RATIO -> {
                    val ratio = 0.618f
                    drawLine(guideColor, Offset(0f, h * ratio), Offset(w, h * ratio), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(0f, h * (1 - ratio)), Offset(w, h * (1 - ratio)), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(w * ratio, 0f), Offset(w * ratio, h), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(w * (1 - ratio), 0f), Offset(w * (1 - ratio), h), strokeWidth = 1.5f)
                    for (x in listOf(ratio, 1 - ratio)) for (y in listOf(ratio, 1 - ratio)) {
                        drawCircle(pointColor, radius = 5f, center = Offset(w * x, h * y))
                    }
                }
                ARGuideType.DIAGONAL -> {
                    drawLine(guideColor, Offset(0f, 0f), Offset(w, h), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(w, 0f), Offset(0f, h), strokeWidth = 1.5f)
                    drawCircle(pointColor, radius = 5f, center = Offset(w / 3, h / 3))
                    drawCircle(pointColor, radius = 5f, center = Offset(w * 2 / 3, h * 2 / 3))
                }
                ARGuideType.CENTER_CROSS -> {
                    drawLine(guideColor, Offset(w / 2, 0f), Offset(w / 2, h), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1.5f)
                    drawCircle(pointColor, radius = 5f, center = Offset(w / 2, h / 2))
                }
                ARGuideType.SPIRAL -> {
                    // 简化斐波那契螺旋
                    val phi = 1.618f
                    val steps = 60
                    val spiralPoints = mutableListOf<Offset>()
                    var cx = w * 0.618f
                    var cy = h * 0.382f
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps * 3f * Math.PI.toFloat()
                        val r = 8f * Math.pow(phi.toDouble(), (t / (2f * Math.PI.toFloat())).toDouble()).toFloat()
                        val x = cx + r * cos(t)
                        val y = cy + r * sin(t)
                        if (x in 0f..w && y in 0f..h) {
                            spiralPoints.add(Offset(x, y))
                        }
                    }
                    for (i in 0 until spiralPoints.size - 1) {
                        drawLine(guideColor, spiralPoints[i], spiralPoints[i + 1], strokeWidth = 1.5f)
                    }
                    drawCircle(pointColor, radius = 6f, center = Offset(cx, cy))
                }
                ARGuideType.FRAME -> {
                    val inset = 0.2f
                    drawRect(guideColor, topLeft = Offset(w * inset, h * inset), size = Size(w * (1 - 2 * inset), h * (1 - 2 * inset)), style = Stroke(width = 2f))
                    drawRect(guideColor.copy(alpha = 0.3f), style = Stroke(width = 1f))
                }
                ARGuideType.HORIZON -> {
                    drawLine(guideColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 2f)
                    // 水平仪指示
                    drawLine(Color.Green.copy(alpha = 0.5f), Offset(w * 0.3f, h / 2), Offset(w * 0.7f, h / 2), strokeWidth = 3f)
                }
                ARGuideType.TRIANGLE -> {
                    val p1 = Offset(w / 2, h * 0.15f)
                    val p2 = Offset(w * 0.15f, h * 0.85f)
                    val p3 = Offset(w * 0.85f, h * 0.85f)
                    drawLine(guideColor, p1, p2, strokeWidth = 1.5f)
                    drawLine(guideColor, p2, p3, strokeWidth = 1.5f)
                    drawLine(guideColor, p3, p1, strokeWidth = 1.5f)
                    drawCircle(pointColor, radius = 5f, center = p1)
                    drawCircle(pointColor, radius = 5f, center = p2)
                    drawCircle(pointColor, radius = 5f, center = p3)
                }
            }
        }

        // 场景模式标签
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                text = "${sceneMode.icon} ${sceneMode.displayName} · ${guideType.displayName}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // AR提示
        Text(
            text = sceneMode.arTip,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
        )
    }
}
