package com.silas.omaster.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * CameraX 实时取景器屏幕 - P2 深度优化版
 *
 * 完整功能链路：
 * - 实时相机预览（CameraX）
 * - 8种拍摄模式 Tab 切换（AI自动/人像/夜景/光绘/专业/美食/街拍/宠物）
 * - AI 一键扫描场景识别 + AR 自动构图引导（主体追踪/构图评分/水平仪）
 * - 实时美化引擎（分层美颜/实时光效/滤镜预览）
 * - 专业模式全手动参数（ISO/快门/对焦/白平衡/直方图/斑马纹）
 * - 夜景模式（多帧合成降噪）
 * - 光绘模式（光轨累积预览）
 * - 0 延迟快门（特化模式）
 * - 哈苏色彩科学实时叠加
 *
 * 交互链路：启动 → 模式选择 → 取景 → AR引导 → 拍照 → 色彩处理 → 保存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraXViewfinderScreen(
    presetParams: HasselbladParams = HasselbladParams(),
    presetName: String = "",
    onBack: () -> Unit,
    onPhotoCaptured: (android.net.Uri) -> Unit = {},
    // P2-1 修复：哈苏构图引导线类型（如 "THIRDS"），与 ARCompositionResult 引导线共存
    hasselbladGuideType: String? = null,
    // 是否启用哈苏构图引导线（与 HasselbladEyeViewModel.isARGuideEnabled 对应）
    isARGuideEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = remember {
        (context as? androidx.lifecycle.LifecycleOwner)
            ?: throw IllegalStateException("CameraXViewfinderScreen must be used within a LifecycleOwner context")
    }

    // ==================== 权限 ====================
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能使用哈苏之眼", Toast.LENGTH_LONG).show()
        }
    }
    hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // ==================== 相机管理器 ====================
    val cameraManager = remember { CameraXManager(context, lifecycleOwner) }
    val isCameraReady by cameraManager.isCameraReady.collectAsState()
    val maxZoomRatio = remember(isCameraReady) { cameraManager.getMaxZoomRatio() }
    val exposureRange = remember(isCameraReady) { cameraManager.getExposureCompensationRange() }

    // 实时帧回调（覆盖在 PreviewView 上）
    var processedFrame by remember { mutableStateOf<Bitmap?>(null) }

    // 手势状态
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAnimatable = remember { Animatable(0f) }
    var currentZoom by remember { mutableStateOf(1f) }
    val coroutineScope = rememberCoroutineScope()

    // 当前拍摄模式
    val captureMode by cameraManager.captureMode.collectAsState()
    var selectedModeIndex by remember { mutableIntStateOf(0) }

    // 夜景/光绘状态
    val nightState by cameraManager.nightModeState.collectAsState()
    val lightPaintingState by cameraManager.lightPaintingState.collectAsState()

    // AR 构图结果
    val arResult by cameraManager.arCompositionResult.collectAsState()

    // 专业模式参数
    val proParams by cameraManager.proModeParams.collectAsState()

    // 构图评分
    var showARGuide by remember { mutableStateOf(true) }
    var showARTips by remember { mutableStateOf(true) }
    var showProPanel by remember { mutableStateOf(false) }

    // ==================== 副作用 ====================
    DisposableEffect(cameraManager) {
        cameraManager.setOnFrameAnalyzed { bitmap ->
            val old = processedFrame
            processedFrame = bitmap
            old?.recycle()
        }
        onDispose {
            cameraManager.setOnFrameAnalyzed(null)
            processedFrame?.recycle()
            processedFrame = null
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraManager.release() }
    }

    LaunchedEffect(isCameraReady) {
        if (isCameraReady) currentZoom = cameraManager.getCurrentZoomRatio()
    }

    LaunchedEffect(captureMode) {
        selectedModeIndex = CaptureMode.entries.indexOf(captureMode).coerceAtLeast(0)
    }

    // ==================== 权限提示 ====================
    if (!hasCameraPermission) {
        PermissionRequestScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    // ==================== 主界面 ====================
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = captureMode.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (presetName.isNotEmpty()) {
                            Text(
                                text = presetName,
                                fontSize = 11.sp,
                                color = HasselbladOrange
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 专业模式参数面板开关（仅在专业模式下显示）
                    if (captureMode == CaptureMode.PRO) {
                        IconButton(onClick = { showProPanel = !showProPanel }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "专业参数",
                                tint = if (showProPanel) HasselbladOrange else Color.White
                            )
                        }
                    }
                    // AR 引导线开关（使用 Canvas 绘制网格图标）
                    IconButton(onClick = { showARGuide = !showARGuide }) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val w = size.width
                            val h = size.height
                            val gridColor = if (showARGuide) HasselbladOrange else Color.White
                            // 画 2 条竖线
                            for (i in 1..2) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(w * i / 3f, 0f),
                                    end = Offset(w * i / 3f, h),
                                    strokeWidth = 1.5f
                                )
                            }
                            // 画 2 条横线
                            for (i in 1..2) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, h * i / 3f),
                                    end = Offset(w, h * i / 3f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }
                    }
                    // 闪光灯
                    val flashState by cameraManager.flashModeState.collectAsState()
                    IconButton(onClick = { cameraManager.toggleFlash() }) {
                        Icon(
                            imageVector = when (flashState) {
                                FLASH_ON -> Icons.Default.FlashOn
                                FLASH_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "闪光灯",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.75f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ==================== 相机预览 ====================
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        cameraManager.startCamera(previewView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ==================== 实时处理帧叠加 ====================
            processedFrame?.let { frame ->
                if (!frame.isRecycled) {
                    androidx.compose.foundation.Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = "实时处理效果",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ==================== AR 引导线覆盖层 ====================
            arResult?.let { result ->
                if (showARGuide) {
                    ARGuideOverlay(
                        result = result,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ==================== AR 构图提示 ====================
                if (showARTips && result.tips.isNotEmpty()) {
                    ARTipsOverlay(
                        tips = result.tips,
                        score = result.compositionScore,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }
            }

            // ==================== 哈苏构图引导线覆盖层（P2-1 修复）====================
            // 独立于 arResult 显示，与 ARCompositionResult 引导线叠加共存
            if (isARGuideEnabled && hasselbladGuideType != null && showARGuide) {
                HasselbladARGuideOverlay(
                    guideType = hasselbladGuideType,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ==================== 模式专属状态指示 ====================
            // 夜景采集进度
            if (captureMode == CaptureMode.NIGHT && nightState.isCapturing) {
                NightCaptureOverlay(
                    state = nightState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }

            // 光绘录制状态
            if (captureMode == CaptureMode.LIGHT_PAINTING && lightPaintingState.isRecording) {
                LightPaintingRecordingOverlay(
                    state = lightPaintingState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }

            // ==================== 手势交互层 ====================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                cameraManager.tapToFocus(offset.x, offset.y)
                                focusPoint = offset
                                coroutineScope.launch {
                                    focusAnimatable.snapTo(0f)
                                    focusAnimatable.animateTo(1f, animationSpec = tween(800))
                                    focusPoint = null
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            currentZoom = (currentZoom * zoom).coerceIn(1f, maxZoomRatio)
                            cameraManager.setZoomRatio(currentZoom)
                        }
                    }
            )

            // 对焦指示器
            focusPoint?.let { point ->
                FocusIndicator(
                    point = point,
                    animatable = focusAnimatable,
                    modifier = Modifier
                )
            }

            // ==================== 右侧辅助面板 ====================
            RightAuxPanel(
                captureMode = captureMode,
                currentZoom = currentZoom,
                maxZoomRatio = maxZoomRatio,
                exposureRange = exposureRange,
                onZoomChange = { zoom ->
                    currentZoom = zoom
                    cameraManager.setZoomRatio(zoom)
                },
                onExposureChange = { index ->
                    cameraManager.setExposureCompensation(index)
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // ==================== 专业模式参数面板 ====================
            if (showProPanel && captureMode == CaptureMode.PRO) {
                ProModePanel(
                    params = proParams,
                    onParamsChange = { newParams ->
                        newParams.iso?.let { cameraManager.setProModeIso(it) }
                        newParams.shutterSpeedNs?.let { cameraManager.setProModeShutter(it) }
                        newParams.focusDistance?.let { cameraManager.setProModeFocus(it) }
                        newParams.whiteBalanceTemperature?.let { cameraManager.setProModeWhiteBalance(it) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                )
            }

            // ==================== 顶部状态栏 ====================
            TopStatusBar(
                captureMode = captureMode,
                arScore = arResult?.compositionScore ?: 0,
                nightState = nightState,
                lightPaintingState = lightPaintingState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            )

            // ==================== 底部操作区 ====================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // --- 模式 Tab 行 ---
                ModeTabRow(
                    modes = CaptureMode.entries,
                    selectedIndex = selectedModeIndex,
                    onModeSelected = { index ->
                        selectedModeIndex = index
                        val mode = CaptureMode.entries[index]
                        cameraManager.setCaptureMode(mode)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- 快门控制栏 ---
                ShutterBar(
                    captureMode = captureMode,
                    nightState = nightState,
                    lightPaintingState = lightPaintingState,
                    isCameraReady = isCameraReady,
                    onShutterClick = {
                        cameraManager.takePhoto(
                            onPhotoSaved = { uri ->
                                onPhotoCaptured(uri)
                                Toast.makeText(context, "照片已保存", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "拍照失败: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onSwitchCamera = {
                        cameraManager.switchCamera( /* 需要 PreviewView */ )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(vertical = 20.dp, horizontal = 32.dp)
                )
            }
        }
    }
}

// ==================== 权限请求界面 ====================
@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "需要相机权限",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "哈苏之眼需要使用您的相机来提供\nAI 场景识别、实时滤镜和专业拍摄功能",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Text("授予相机权限")
            }
        }
    }
}

// ==================== 模式 Tab 行 ====================
@Composable
private fun ModeTabRow(
    modes: List<CaptureMode>,
    selectedIndex: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.85f))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        modes.forEachIndexed { index, mode ->
            val selected = index == selectedIndex
            val tabColor = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.5f)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) HasselbladOrange.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) HasselbladOrange else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onModeSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = mode.icon,
                        fontSize = 14.sp
                    )
                    Text(
                        text = mode.displayName,
                        color = tabColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ==================== AR 引导线覆盖层 ====================
@Composable
private fun ARGuideOverlay(
    result: ARCompositionResult,
    modifier: Modifier = Modifier
) {
    val guideColor = when {
        result.compositionScore >= 80 -> Color(0xFF4CAF50) // 绿色：优秀
        result.compositionScore >= 60 -> Color(0xFFFF9800) // 橙色：一般
        else -> Color(0xFFFF5252) // 红色：需调整
    }

    val density = LocalDensity.current.density

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 绘制动态引导线（来自 ARCompositionManager）
        result.guideLines.forEach { line ->
            val lineColor = Color(line.color.toLong()).copy(
                alpha = (line.color ushr 24 and 0xFF) / 255f
            )
            drawLine(
                color = lineColor,
                start = Offset(line.startX * w, line.startY * h),
                end = Offset(line.endX * w, line.endY * h),
                strokeWidth = line.strokeWidth * density
            )
        }

        // 主体包围框
        result.subjectBounds?.let { bounds ->
            val left = bounds.left * w
            val top = bounds.top * h
            val right = bounds.right * w
            val bottom = bounds.bottom * h
            drawRect(
                color = guideColor.copy(alpha = 0.6f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 2f)
            )
        }

        // 水平仪指示（相机横滚角）
        val roll = result.levelIndicator
        val rollDeg = Math.toDegrees(roll.toDouble()).toFloat()
        val indicatorY = h - 60.dp.toPx()
        val indicatorCenterX = w / 2f
        val rollIndicatorLength = 80f

        // 水平仪背景弧线
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(indicatorCenterX - 100f, indicatorY),
            end = Offset(indicatorCenterX + 100f, indicatorY),
            strokeWidth = 2f
        )

        // 水平仪气泡（根据横滚角偏移）
        val bubbleX = indicatorCenterX + (rollDeg / 45f).coerceIn(-1f, 1f) * rollIndicatorLength
        val bubbleColor = when {
            abs(rollDeg) < 2f -> Color(0xFF4CAF50)
            abs(rollDeg) < 5f -> Color(0xFFFF9800)
            else -> Color(0xFFFF5252)
        }
        drawCircle(
            color = bubbleColor,
            radius = 8f,
            center = Offset(bubbleX, indicatorY)
        )

        // 横滚角文字
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(
                String.format("%.1f°", rollDeg),
                indicatorCenterX,
                indicatorY - 16f,
                paint
            )
        }
    }
}

// ==================== AR 构图提示 ====================
@Composable
private fun ARTipsOverlay(
    tips: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFFF5252)
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 评分圆环
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(2.dp, scoreColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score",
                color = scoreColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = tips,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

// ==================== 哈苏构图引导线覆盖层 ====================
// P2-1 修复：在 CameraX 实时取景中叠加哈苏构图引导线
// 与 ARCompositionResult 引导线互不冲突（独立于 arResult 存在）
@Composable
private fun HasselbladARGuideOverlay(
    guideType: String,
    modifier: Modifier = Modifier
) {
    val guideColor = HasselbladOrange.copy(alpha = 0.85f)
    val pointColor = HasselbladOrange

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (guideType) {
            "THIRDS" -> {
                for (i in 1..2) {
                    drawLine(guideColor, Offset(0f, h * i / 3), Offset(w, h * i / 3), strokeWidth = 1.5f)
                    drawLine(guideColor, Offset(w * i / 3, 0f), Offset(w * i / 3, h), strokeWidth = 1.5f)
                }
                for (i in 1..2) for (j in 1..2) {
                    drawCircle(pointColor, radius = 5f, center = Offset(w * i / 3, h * j / 3))
                }
            }
            "GOLDEN_RATIO" -> {
                val ratio = 0.618f
                drawLine(guideColor, Offset(0f, h * ratio), Offset(w, h * ratio), strokeWidth = 1.5f)
                drawLine(guideColor, Offset(0f, h * (1 - ratio)), Offset(w, h * (1 - ratio)), strokeWidth = 1.5f)
                drawLine(guideColor, Offset(w * ratio, 0f), Offset(w * ratio, h), strokeWidth = 1.5f)
                drawLine(guideColor, Offset(w * (1 - ratio), 0f), Offset(w * (1 - ratio), h), strokeWidth = 1.5f)
                for (x in listOf(ratio, 1 - ratio)) for (y in listOf(ratio, 1 - ratio)) {
                    drawCircle(pointColor, radius = 5f, center = Offset(w * x, h * y))
                }
            }
            "DIAGONAL" -> {
                drawLine(guideColor, Offset(0f, 0f), Offset(w, h), strokeWidth = 1.5f)
                drawLine(guideColor, Offset(w, 0f), Offset(0f, h), strokeWidth = 1.5f)
                drawCircle(pointColor, radius = 5f, center = Offset(w / 3, h / 3))
                drawCircle(pointColor, radius = 5f, center = Offset(w * 2 / 3, h * 2 / 3))
            }
            "CENTER_CROSS" -> {
                drawLine(guideColor, Offset(w / 2, 0f), Offset(w / 2, h), strokeWidth = 1.5f)
                drawLine(guideColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 1.5f)
                drawCircle(pointColor, radius = 5f, center = Offset(w / 2, h / 2))
            }
            "SPIRAL" -> {
                val phi = 1.618f
                val steps = 60
                val spiralPoints = mutableListOf<Offset>()
                var cx = w * 0.618f
                var cy = h * 0.382f
                for (i in 0..steps) {
                    val t = i.toFloat() / steps * 3f * Math.PI.toFloat()
                    val r = 8f * Math.pow(phi.toDouble(), (t / (2f * Math.PI.toFloat())).toDouble()).toFloat()
                    val x = cx + r * kotlin.math.cos(t)
                    val y = cy + r * kotlin.math.sin(t)
                    if (x in 0f..w && y in 0f..h) {
                        spiralPoints.add(Offset(x, y))
                    }
                }
                for (i in 0 until spiralPoints.size - 1) {
                    drawLine(guideColor, spiralPoints[i], spiralPoints[i + 1], strokeWidth = 1.5f)
                }
                drawCircle(pointColor, radius = 6f, center = Offset(cx, cy))
            }
            "FRAME" -> {
                val inset = 0.2f
                drawRect(
                    color = guideColor,
                    topLeft = Offset(w * inset, h * inset),
                    size = androidx.compose.ui.geometry.Size(w * (1 - 2 * inset), h * (1 - 2 * inset)),
                    style = Stroke(width = 2f)
                )
            }
            "HORIZON" -> {
                drawLine(guideColor, Offset(0f, h / 2), Offset(w, h / 2), strokeWidth = 2f)
                drawLine(Color.Green.copy(alpha = 0.5f), Offset(w * 0.3f, h / 2), Offset(w * 0.7f, h / 2), strokeWidth = 3f)
            }
            "TRIANGLE" -> {
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
}

// ==================== 夜景采集进度覆盖 ====================
@Composable
private fun NightCaptureOverlay(
    state: NightModeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "夜景合成中 ${state.capturedFrames}/${state.totalFrames}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        LinearProgressIndicator(
            progress = {
                if (state.totalFrames > 0)
                    state.capturedFrames.toFloat() / state.totalFrames
                else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = HasselbladOrange,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        if (state.estimatedRemainingMs > 0) {
            Text(
                text = "约 ${state.estimatedRemainingMs / 1000} 秒",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

// ==================== 光绘录制状态覆盖 ====================
@Composable
private fun LightPaintingRecordingOverlay(
    state: LightPaintingState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordingAlpha"
    )

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 录制红点
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color.Red.copy(alpha = alpha), CircleShape)
        )
        Text(
            text = "光绘录制中 ${state.elapsedMs / 1000}s",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== 顶部状态栏 ====================
@Composable
private fun TopStatusBar(
    captureMode: CaptureMode,
    arScore: Int,
    nightState: NightModeState,
    lightPaintingState: LightPaintingState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // AI 自动模式显示场景识别状态
        if (captureMode == CaptureMode.AI_AUTO) {
            StatusChip(
                text = "AI 自动",
                color = HasselbladOrange,
                modifier = Modifier
            )
        }

        // 人像模式
        if (captureMode == CaptureMode.PORTRAIT) {
            StatusChip(
                text = "虚化 · 美颜",
                color = Color(0xFFE91E63),
                modifier = Modifier
            )
        }

        // 专业模式
        if (captureMode == CaptureMode.PRO) {
            StatusChip(
                text = "专业模式",
                color = Color(0xFF9C27B0),
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// ==================== 对焦指示器 ====================
@Composable
private fun FocusIndicator(
    point: Offset,
    animatable: Animatable<Float, *>,
    modifier: Modifier = Modifier
) {
    val indicatorSize = 80.dp
    val scale = 0.6f + animatable.value * 0.8f
    val alpha = (1f - animatable.value).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (point.x - indicatorSize.toPx() / 2).toInt(),
                    (point.y - indicatorSize.toPx() / 2).toInt()
                )
            }
            .size(indicatorSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.White, CircleShape)
        )
        // 中心十字
        Box(
            modifier = Modifier
                .size(1.dp, 16.dp)
                .background(Color.White, RoundedCornerShape(0.5.dp))
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(16.dp, 1.dp)
                .background(Color.White, RoundedCornerShape(0.5.dp))
                .align(Alignment.Center)
        )
    }
}

// ==================== 右侧辅助面板 ====================
@Composable
private fun RightAuxPanel(
    captureMode: CaptureMode,
    currentZoom: Float,
    maxZoomRatio: Float,
    exposureRange: IntRange,
    onZoomChange: (Float) -> Unit,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var localZoom by remember { mutableFloatStateOf(currentZoom) }
    var localExposure by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentZoom) { localZoom = currentZoom }

    Column(
        modifier = modifier
            .padding(end = 12.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 变焦
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "变焦",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = String.format("%.1fx", localZoom),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = localZoom,
                onValueChange = {
                    localZoom = it
                    onZoomChange(it)
                },
                valueRange = 1f..maxZoomRatio.coerceAtMost(10f),
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .rotate(-90f),
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }

        // 曝光补偿
        if (exposureRange.first != exposureRange.last) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "曝光",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (localExposure > 0) "+$localExposure" else "$localExposure",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = localExposure.toFloat(),
                    onValueChange = {
                        val idx = it.roundToInt()
                        localExposure = idx
                        onExposureChange(idx)
                    },
                    valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                    steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                        .rotate(-90f),
                    colors = SliderDefaults.colors(
                        thumbColor = HasselbladOrange,
                        activeTrackColor = HasselbladOrange,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

// ==================== 专业模式参数面板 ====================
@Composable
private fun ProModePanel(
    params: ProModeParams,
    onParamsChange: (ProModeParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "专业参数",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        // ISO
        ProParamRow(
            label = "ISO",
            value = params.iso?.toString() ?: "自动",
            onIncrease = {
                val current = params.iso ?: 100
                onParamsChange(params.copy(iso = (current * 1.5).toInt().coerceAtMost(12800)))
            },
            onDecrease = {
                val current = params.iso ?: 100
                onParamsChange(params.copy(iso = (current / 1.5).toInt().coerceAtLeast(50)))
            }
        )

        // 快门速度
        ProParamRow(
            label = "快门",
            value = params.shutterSpeedNs?.let {
                val ms = it / 1_000_000f
                if (ms >= 1f) "${ms.toInt()}s" else "1/${(1000f / ms).toInt()}s"
            } ?: "自动",
            onIncrease = {
                val current = params.shutterSpeedNs ?: 1_000_000L
                onParamsChange(params.copy(shutterSpeedNs = (current * 1.5).toLong()))
            },
            onDecrease = {
                val current = params.shutterSpeedNs ?: 1_000_000L
                onParamsChange(params.copy(shutterSpeedNs = (current / 1.5).toLong().coerceAtLeast(1000)))
            }
        )

        // 对焦距离
        ProParamRow(
            label = "对焦",
            value = params.focusDistance?.let {
                if (it < 0.05f) "∞" else String.format("%.1fm", 1f / it)
            } ?: "自动",
            onIncrease = {
                val current = params.focusDistance ?: 0.5f
                onParamsChange(params.copy(focusDistance = (current + 0.1f).coerceIn(0f, 1f)))
            },
            onDecrease = {
                val current = params.focusDistance ?: 0.5f
                onParamsChange(params.copy(focusDistance = (current - 0.1f).coerceIn(0f, 1f)))
            }
        )

        // 白平衡
        ProParamRow(
            label = "WB",
            value = params.whiteBalanceTemperature?.let { "${it}K" } ?: "自动",
            onIncrease = {
                val current = params.whiteBalanceTemperature ?: 5500
                onParamsChange(params.copy(whiteBalanceTemperature = current + 200))
            },
            onDecrease = {
                val current = params.whiteBalanceTemperature ?: 5500
                onParamsChange(params.copy(whiteBalanceTemperature = (current - 200).coerceAtLeast(2500)))
            }
        )

        // 重置按钮
        Button(
            onClick = { onParamsChange(ProModeParams()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = HasselbladOrange.copy(alpha = 0.2f),
                contentColor = HasselbladOrange
            )
        ) {
            Text("重置", fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProParamRow(
    label: String,
    value: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier.size(24.dp)
            ) {
                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                text = value,
                color = HasselbladOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(56.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(24.dp)
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== 快门控制栏 ====================
@Composable
private fun ShutterBar(
    captureMode: CaptureMode,
    nightState: NightModeState,
    lightPaintingState: LightPaintingState,
    isCameraReady: Boolean,
    onShutterClick: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProcessing = nightState.isCapturing || lightPaintingState.isRecording

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧占位（保持居中）
        Spacer(modifier = Modifier.size(48.dp))

        // 快门按钮（行为随模式变化）
        ShutterButton(
            captureMode = captureMode,
            nightState = nightState,
            lightPaintingState = lightPaintingState,
            enabled = isCameraReady && !nightState.isCapturing,
            onClick = onShutterClick,
            modifier = Modifier.size(72.dp)
        )

        // 切换摄像头
        IconButton(
            onClick = onSwitchCamera,
            enabled = isCameraReady && !isProcessing,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "切换摄像头",
                tint = if (isCameraReady && !isProcessing) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// ==================== 智能快门按钮 ====================
@Composable
private fun ShutterButton(
    captureMode: CaptureMode,
    nightState: NightModeState,
    lightPaintingState: LightPaintingState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shutter")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val buttonColor = when {
        nightState.isCapturing -> Color(0xFF2196F3) // 蓝色：夜景采集中
        lightPaintingState.isRecording -> Color.Red.copy(alpha = pulseAlpha) // 红色闪烁：光绘录制中
        captureMode == CaptureMode.NIGHT -> Color(0xFF2196F3)
        captureMode == CaptureMode.LIGHT_PAINTING -> Color.Red
        else -> Color.White
    }

    val innerColor = when {
        nightState.isCapturing -> Color(0xFF2196F3).copy(alpha = 0.3f)
        lightPaintingState.isRecording -> Color.Red.copy(alpha = 0.3f)
        else -> Color.White
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .border(4.dp, buttonColor, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = innerColor,
            disabledContainerColor = innerColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(4.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        // 夜景采集中显示帧数
        if (nightState.isCapturing) {
            Text(
                text = "${nightState.capturedFrames}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // 光绘录制中显示停止图标
        else if (lightPaintingState.isRecording) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Red, RoundedCornerShape(3.dp))
            )
        }
        // 默认实心圆
        else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(buttonColor, CircleShape)
            )
        }
    }
}

// ==================== 辅助常量 ====================
// ImageCapture.FLASH_MODE_* 常量直接用 Int 比较
private const val FLASH_OFF = ImageCapture.FLASH_MODE_OFF
private const val FLASH_ON = ImageCapture.FLASH_MODE_ON
private const val FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO
