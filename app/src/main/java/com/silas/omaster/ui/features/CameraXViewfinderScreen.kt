package com.silas.omaster.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * CameraX 实时取景器屏幕 - P2 深度优化
 *
 * 功能：
 * - 实时相机预览
 * - 预设参数实时叠加到预览画面
 * - 前后摄像头切换
 * - 闪光灯控制
 * - 拍照保存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraXViewfinderScreen(
    presetParams: HasselbladParams = HasselbladParams(),
    presetName: String = "",
    onBack: () -> Unit,
    onPhotoCaptured: (android.net.Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = remember {
        (context as? androidx.lifecycle.LifecycleOwner)
            ?: throw IllegalStateException("CameraXViewfinderScreen must be used within a LifecycleOwner context")
    }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var showParamsOverlay by remember { mutableStateOf(true) }

    // 持有 PreviewView 引用，用于切换摄像头
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // 实时处理后的帧（用于在 PreviewView 之上叠加显示预设效果）
    var processedFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 手势交互状态：点击对焦指示器、变焦、曝光补偿
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusAnimatable = remember { Animatable(0f) }
    var currentZoom by remember { mutableStateOf(1f) }
    var exposureIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // 相机权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能使用实时取景器", Toast.LENGTH_LONG).show()
        }
    }

    // 检查权限
    hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // 相机管理器
    val cameraManager = remember {
        CameraXManager(context, lifecycleOwner)
    }

    // 相机就绪状态：用于在相机绑定后刷新曝光/变焦范围
    val isCameraReady by cameraManager.isCameraReady.collectAsState()
    val exposureRange = remember(isCameraReady) { cameraManager.getExposureCompensationRange() }
    val maxZoomRatio = remember(isCameraReady) { cameraManager.getMaxZoomRatio() }

    // 相机就绪后同步当前变焦倍数
    LaunchedEffect(isCameraReady) {
        if (isCameraReady) {
            currentZoom = cameraManager.getCurrentZoomRatio()
        }
    }

    // 更新预设参数
    DisposableEffect(presetParams) {
        cameraManager.updatePresetParams(presetParams)
        onDispose { }
    }

    // 订阅 CameraXManager 的实时处理结果，在 PreviewView 之上叠加显示
    DisposableEffect(cameraManager) {
        cameraManager.setOnFrameAnalyzed { bitmap ->
            // 回收旧帧，避免 Bitmap 累积导致内存泄漏
            val oldFrame = processedFrame
            processedFrame = bitmap
            oldFrame?.recycle()
        }
        onDispose {
            cameraManager.setOnFrameAnalyzed(null)
            processedFrame?.recycle()
            processedFrame = null
        }
    }

    // 释放相机资源
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "实时取景器",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (presetName.isNotEmpty()) {
                            Text(
                                text = presetName,
                                fontSize = 12.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
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
            // 相机预览
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewViewRef = previewView
                            cameraManager.startCamera(previewView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 实时叠加预设效果帧
                processedFrame?.let { frame ->
                    if (!frame.isRecycled) {
                        androidx.compose.foundation.Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "实时预设效果",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 预设参数叠加标签
                if (showParamsOverlay) {
                    PresetParamsOverlay(
                        params = presetParams,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }

                // 手势交互层：点击对焦 + 双指缩放
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

                // 对焦指示器（点击位置圆圈，约 1 秒后消失）
                focusPoint?.let { point ->
                    val indicatorSize = 80.dp
                    val indicatorScale = 0.6f + focusAnimatable.value * 0.8f
                    val indicatorAlpha = (1f - focusAnimatable.value).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (point.x - indicatorSize.toPx() / 2).toInt(),
                                    (point.y - indicatorSize.toPx() / 2).toInt()
                                )
                            }
                            .size(indicatorSize)
                            .graphicsLayer {
                                scaleX = indicatorScale
                                scaleY = indicatorScale
                                alpha = indicatorAlpha
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }

                // 变焦倍数指示器
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format("%.1fx", currentZoom),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 曝光补偿滑块（垂直，仅当设备支持时显示）
                if (exposureRange.first != exposureRange.last) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(width = 200.dp, height = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = exposureIndex.toFloat(),
                            onValueChange = { newValue ->
                                val newIndex = newValue.roundToInt()
                                if (newIndex != exposureIndex) {
                                    exposureIndex = newIndex
                                    cameraManager.setExposureCompensation(newIndex)
                                }
                            },
                            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                            modifier = Modifier.rotate(-90f)
                        )
                    }
                }
            } else {
                // 无权限提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "需要相机权限",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    androidx.compose.material3.Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange
                        )
                    ) {
                        Text("授予权限")
                    }
                }
            }

            // 底部控制栏
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(vertical = 24.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 闪光灯按钮
                IconButton(
                    onClick = {
                        flashMode = cameraManager.toggleFlash()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            0 -> Icons.Default.FlashOff
                            1 -> Icons.Default.FlashOn
                            else -> Icons.Default.FlashAuto
                        },
                        contentDescription = "闪光灯",
                        tint = Color.White
                    )
                }

                // 拍照按钮
                androidx.compose.material3.Button(
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            cameraManager.takePhoto(
                                onPhotoSaved = { uri ->
                                    isCapturing = false
                                    onPhotoCaptured(uri)
                                    Toast.makeText(context, "照片已保存", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    isCapturing = false
                                    Toast.makeText(context, "拍照失败: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .border(4.dp, Color.White, CircleShape),
                    shape = CircleShape,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.3f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    enabled = !isCapturing
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                // 切换摄像头
                IconButton(
                    onClick = {
                        previewViewRef?.let { pv ->
                            cameraManager.switchCamera(pv)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "切换摄像头",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 预设参数叠加标签
 */
@Composable
private fun PresetParamsOverlay(
    params: HasselbladParams,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "实时预设",
            color = HasselbladOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        if (params.saturation != 0) {
            ParamLabel("饱和度", params.saturation)
        }
        if (params.contrast != 0) {
            ParamLabel("对比度", params.contrast)
        }
        if (params.colorTemp != 0) {
            ParamLabel("色温", params.colorTemp)
        }
        if (params.tone != 0) {
            ParamLabel("影调", params.tone)
        }
        if (params.vignette != 0) {
            ParamLabel("暗角", params.vignette)
        }
        if (params.sharpness != 0) {
            ParamLabel("锐度", params.sharpness)
        }
    }
}

@Composable
private fun ParamLabel(name: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Text(
            text = if (value > 0) "+$value" else "$value",
            color = if (value > 0) Color(0xFF4CAF50) else Color(0xFFFF9800),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}