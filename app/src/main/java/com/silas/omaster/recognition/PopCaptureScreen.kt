package com.silas.omaster.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.annotation.SuppressLint
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.ErrorRed
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.NearBlack
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.ImageExporter
import com.silas.omaster.util.PresetI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * POP 预设即拍页（PRD FR-10~14 / 16.2）。
 *
 * 链路：加载预设 → 能力探测分级（后置主摄）→ 生成拍摄计划（[PresetCapturePlan]）→
 * 自研相机管线（CameraX + Camera2 Interop）：
 * ① ImageAnalysis 逐帧套用预设色彩矩阵 → 取景器实时滤镜（所见即所得）；
 * ② ImageCapture 按 [CapturePlan] 把 ISO/快门/曝光补偿真实写入拍摄请求
 * （AE off + STILL_CAPTURE 意图，厂商画质管线保持开启）；
 * ③ 成片经 [PopImageProcessor] 全分辨率画质管线（色彩矩阵 + 高 ISO 保边降噪 +
 * 亮度域 unsharp 锐化 + 暗角）→ 保存相册；无可套用参数时保存原始 JPEG 字节零重编码。
 *
 * 降级路径：无相机权限或设备不支持手动曝光时，可用系统相机拍照 / 相册选图，
 * 拍后仍完整套用调色参数。参数卡按"已写入拍摄 / 取景与成片已套用 / 无法套用（含原因）"
 * 如实分级，不做"假装已套用"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopCaptureScreen(
    preset: MasterPreset,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val accent = HasselbladOrange
    val capability = remember { CameraCapabilityProbe.probe(context) }
    val plan = remember(preset, capability) { PresetCapturePlan.build(preset, capability.manualRanges) }

    var appliedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 无可套用参数时保留的传感器原始 JPEG 字节（保存时零重编码直通，v1.7.8）
    var capturedJpegBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    // 【闭环修复】保存进行中防重复点击（连点会产生重复文件）
    var isSaving by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    // 系统相机临时照片实体文件（读取完成后即删，防 cache 目录无限累积）
    var photoFile by remember { mutableStateOf<File?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val localizedPresetName = PresetI18n.getLocalizedPresetName(context, preset.name)

    // 文案在组合作用域预取（协程/回调复用，避免 lint 拦截）
    val msgApplied = stringResource(R.string.pop_applied_style, localizedPresetName)
    val msgReadFailed = stringResource(R.string.pop_read_failed)
    val msgSaved = stringResource(R.string.pop_saved)
    val msgSaveFailed = stringResource(R.string.pop_save_failed)
    val msgNeedPhoto = stringResource(R.string.pop_need_photo)
    val msgCaptureFailed = stringResource(R.string.pop_capture_failed)
    // 无可套用参数时的如实提示（不宣称"已套用"）
    val msgNoEffect = stringResource(R.string.pop_no_effect, localizedPresetName)

    // 申请清单：CAMERA 必需；API 23-28 补 WRITE_EXTERNAL_STORAGE（写公共相册 Pictures/OMaster），
    // 拒绝时保存自动回退应用专属目录 + MediaScanner 索引（ImageExporter），不影响功能
    val requestPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionRequested = true
        cameraGranted = granted[Manifest.permission.CAMERA] == true
    }

    // ── 自研相机管线：ImageAnalysis 实时滤镜取景 + 按拍摄计划配置 ImageCapture ──
    val imageCapture = remember(plan) { buildImageCapture(plan) }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraBound by remember { mutableStateOf(false) }
    // 【闭环修复】相机绑定失败如实呈现（被占用/无后摄等），与"连接中"占位区分
    var cameraError by remember { mutableStateOf(false) }

    // 实时滤镜取景：与拍后套色同源的色彩矩阵，取景即成片观感
    val colorMatrix = remember(preset) { PresetColorTransform.buildColorMatrix(preset) }
    val vignetteStrength = remember(preset) { PresetColorTransform.vignetteStrength(preset) }
    val filteredFrame = remember { mutableStateOf<Bitmap?>(null) }
    // 守护线程执行器：页面退出时 shutdown，异常场景也不阻塞进程退出
    val analyzerExecutor = remember {
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "pop-filter-preview").apply { isDaemon = true }
        }
    }
    val analyzer = remember(colorMatrix, vignetteStrength) {
        FilteredPreviewAnalyzer(colorMatrix, vignetteStrength) { bmp ->
            filteredFrame.value = bmp
        }
    }
    val imageAnalysis = remember {
        // 【v1.8.0 画质】取景目标 1280x720（默认 640x480 拉伸到预览框观感模糊）；
        // 不可用时就近取更高再退更低，KEEP_ONLY_LATEST + 30fps 节流保证流畅省电
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    // 成片展示时暂停实时分析，重新取景自动恢复
    LaunchedEffect(appliedBitmap) { analyzer.enabled = appliedBitmap == null }
    DisposableEffect(Unit) {
        onDispose {
            analyzer.enabled = false
            analyzer.release()
            imageAnalysis.clearAnalyzer()
            analyzerExecutor.shutdown()
        }
    }

    DisposableEffect(cameraGranted, imageCapture) {
        if (cameraGranted) {
            val listener = Runnable {
                cameraError = false
                try {
                    val provider = providerFuture.get()
                    provider.unbindAll()
                    imageAnalysis.setAnalyzer(analyzerExecutor, analyzer)
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis,
                        imageCapture
                    )
                    boundProvider = provider
                    cameraBound = true
                } catch (e: Exception) {
                    cameraBound = false
                    cameraError = true
                }
            }
            providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching { imageAnalysis.clearAnalyzer() }
                runCatching { boundProvider?.unbindAll() }
                boundProvider = null
                cameraBound = false
            }
        } else {
            onDispose { }
        }
    }

    /**
     * 拍后套色管线（相机成片 / 系统相机 / 相册选图共用）。
     *
     * 【v1.7.8 全分辨率画质】有效果预设走 [PopImageProcessor] 条带管线：
     * 全分辨率输出（不再 2048px 降采样）+ 内存守卫自动兜底 + 高 ISO 保边降噪；
     * 无可套用参数时成片即原图，保留传感器原始 JPEG 字节供保存直通（零重编码）。
     *
     * @param raw 原始位图（调用方交出所有权，内部按需回收）
     * @param jpegBytes 相机原始 JPEG 字节（仅相机路径非空；无效果时保存直通）
     */
    fun stylePhoto(raw: Bitmap, preset: MasterPreset, jpegBytes: ByteArray? = null) {
        scope.launch {
            val hasEffect = PresetColorTransform.hasVisibleEffect(preset)
            val styled = if (hasEffect) {
                withContext(Dispatchers.Default) {
                    PopImageProcessor.process(raw, preset, Runtime.getRuntime().maxMemory())
                }
            } else {
                raw
            }
            appliedBitmap = styled
            capturedJpegBytes = if (!hasEffect) jpegBytes else null
            if (styled !== raw) raw.recycle()
            // 如实提示：确有机器可套用的参数才宣称"已套用"，否则明示按原图拍摄
            toast = if (hasEffect) msgApplied else msgNoEffect
            isCapturing = false
        }
    }

    /** 快门：拍摄 → 拍后套色 → 预览（相机未绑定成功时不触发，防无效点击） */
    fun captureWithPreset() {
        if (isCapturing || !cameraBound) return
        isCapturing = true
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // 原始 JPEG 字节（duplicate 独立 position，不影响随后的 toBitmap）：
                    // 无效果预设保存时零重编码直通，画质等同系统相机（v1.7.8）
                    val jpegBytes = try {
                        image.planes?.takeIf { it.isNotEmpty() }?.get(0)?.buffer?.let { buf ->
                            ByteArray(buf.remaining()).also { buf.duplicate().get(it) }
                        }
                    } catch (e: Exception) {
                        null
                    }
                    val bmp = try {
                        // 【v1.8.0 方向修复】toBitmap() 不应用旋转，成片按 rotationDegrees 转正
                        // （与取景路径同源；无效果直通路径带 EXIF，无需处理）
                        val rotation = runCatching { image.imageInfo.rotationDegrees }.getOrDefault(0)
                        rotateToUpright(image.toBitmap(), rotation)
                    } catch (e: Exception) {
                        null
                    } finally {
                        image.close()
                    }
                    if (bmp == null) {
                        isCapturing = false
                        toast = msgCaptureFailed
                        return
                    }
                    stylePhoto(bmp, preset, jpegBytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    toast = msgCaptureFailed
                }
            }
        )
    }

    // 系统相机拍照：写入 FileProvider cache Uri，返回 true 表示拍摄成功
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        // 临时照片读取完成后即清理（无论成功/失败/取消）
        fun cleanupTempPhoto() {
            photoFile?.delete()
            photoFile = null
        }
        if (success) {
            photoUri?.let { uri ->
                isCapturing = true
                scope.launch {
                    val bmp = loadBitmap(context, uri)
                    cleanupTempPhoto()
                    if (bmp != null) {
                        stylePhoto(bmp, preset)
                    } else {
                        toast = msgReadFailed
                        isCapturing = false
                    }
                }
            } ?: run {
                cleanupTempPhoto()
                isCapturing = false
            }
        } else {
            cleanupTempPhoto()
            isCapturing = false
        }
    }

    // 从相册选图套色（离线路径，无相机权限可用）
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCapturing = true
            scope.launch {
                val bmp = loadBitmap(context, uri)
                if (bmp != null) {
                    stylePhoto(bmp, preset)
                } else {
                    toast = msgReadFailed
                    isCapturing = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pop_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pop_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AppDesign.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDesign.SectionSpacing)
        ) {
            // 设备能力徽章
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (capability.level) {
                        CameraCapabilityProbe.Level.L1 -> SuccessGreen.copy(alpha = 0.12f)
                        CameraCapabilityProbe.Level.L2 -> WarningYellow.copy(alpha = 0.12f)
                        CameraCapabilityProbe.Level.L3 -> ErrorRed.copy(alpha = 0.12f)
                    }
                ),
                shape = AppDesign.CardShape, modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(AppDesign.ContentPadding)) {
                    Text(
                        stringResource(R.string.pop_capability, capability.level.toString(), capability.hardwareLevelString),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(capability.degradationHintRes), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            // 取景 / 成片预览区（四态品牌级过渡：淡入 + 微缩放）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(AppDesign.LargeRadius))
                    .background(NearBlack),
                contentAlignment = Alignment.Center
            ) {
                val bmp = appliedBitmap
                val mode = when {
                    bmp != null -> PreviewMode.Result
                    cameraGranted && cameraBound -> PreviewMode.Live
                    cameraGranted && cameraError -> PreviewMode.Error
                    else -> PreviewMode.Placeholder
                }
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = 0.98f, animationSpec = tween(180))
                            )
                    },
                    label = "popPreviewState",
                    modifier = Modifier.fillMaxSize()
                ) { m ->
                    when (m) {
                        PreviewMode.Result -> {
                            bmp?.let {
                                Image(it.asImageBitmap(), contentDescription = stringResource(R.string.pop_preview),
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            // 重拍：回到取景（有相机权限）或占位
                            TextButton(
                                onClick = {
                                    appliedBitmap = null
                                    capturedJpegBytes = null
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    stringResource(R.string.pop_retake),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        PreviewMode.Live -> {
                            // 实时滤镜取景：ImageAnalysis 逐帧套色后的画面（所见即所得）
                            val frame = filteredFrame.value
                            // isRecycled 防御：页面退出竞态下 analyzer.release() 可能刚回收缓冲
                            if (frame != null && !frame.isRecycled) {
                                Image(
                                    frame.asImageBitmap(),
                                    contentDescription = stringResource(R.string.pop_preview),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                ViewfinderPlaceholder(accent)
                            }
                        }
                        PreviewMode.Error -> {
                            // 相机启动失败如实呈现：明示降级路径仍可用
                            Text(
                                stringResource(R.string.pop_camera_bind_failed),
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(AppDesign.ContentPadding)
                            )
                        }
                        PreviewMode.Placeholder -> ViewfinderPlaceholder(accent)
                    }
                }
            }

            // 参数应用状态卡（已写入拍摄 / 取景与成片已套用 / 无法套用+原因）
            ParamStatusCard(plan, accent)

            // 相机权限引导（未授权时提供授权入口，同时保留降级路径说明）
            if (!cameraGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = AppDesign.CardShape, modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(AppDesign.ContentPadding)) {
                        Text(stringResource(R.string.pop_camera_permission_rationale),
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { permissionLauncher.launch(requestPermissions) }) {
                            Text(stringResource(R.string.pop_camera_permission_button))
                        }
                        if (permissionRequested) {
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.pop_camera_permission_denied),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // 快门（自研管线）/ 系统相机 / 相册选图
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cameraGranted) {
                    // 快门按钮（防重复触发；相机绑定失败时禁用，引导走降级路径）
                    // 按压反馈：拍摄期间轻微收缩，松开回弹（品牌触感）
                    val shutterScale by animateFloatAsState(
                        targetValue = if (isCapturing) 0.9f else 1f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing),
                        label = "popShutterScale"
                    )
                    Button(
                        onClick = { captureWithPreset() },
                        modifier = Modifier.size(72.dp).scale(shutterScale),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = cameraBound && !isCapturing
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.pop_shutter),
                            modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                // 系统相机（降级路径）
                SmallFloatingActionButton(
                    onClick = {
                        if (!isCapturing) {
                            val (file, uri) = createPhotoFile(context)
                            photoFile = file
                            photoUri = uri
                            isCapturing = true
                            takePictureLauncher.launch(uri)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = stringResource(R.string.pop_system_camera))
                }
                Spacer(Modifier.width(16.dp))
                // 相册选图（离线套色）
                SmallFloatingActionButton(
                    onClick = { if (!isCapturing) pickImageLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.pop_pick_from_gallery))
                }
            }

            // 保存到相册（v1.7.8：无可套用参数时优先保存原始 JPEG 字节，零重编码）
            Button(
                onClick = {
                    val bytes = capturedJpegBytes
                    val bmp = appliedBitmap
                    if (bytes == null && bmp == null) {
                        toast = msgNeedPhoto
                    } else if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                when {
                                    bytes != null && bytes.isNotEmpty() ->
                                        ImageExporter.saveJpegBytes(context, bytes)
                                    bmp != null ->
                                        ImageExporter.saveToGallery(context, bmp)
                                    else -> false
                                }
                            }
                            isSaving = false
                            toast = if (ok) msgSaved else msgSaveFailed
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = appliedBitmap != null && !isCapturing && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = AppDesign.ButtonShape
            ) {
                Text(stringResource(R.string.pop_save_to_gallery), fontWeight = FontWeight.Bold)
            }

            // 拍摄/保存进度（展开-收起过渡，不做生硬的插入/移除）
            AnimatedVisibility(
                visible = isCapturing || isSaving,
                enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) + expandVertically(),
                exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) + shrinkVertically()
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = accent)
            }

            Text(
                text = stringResource(R.string.pop_notice, localizedPresetName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
            toast = null
        }
    }
}

/** 按拍摄计划构造 ImageCapture：手动曝光设备关闭 AE 并写入 ISO/快门，其余按步进写 EV */
@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("UnsafeOptInUsageError")
private fun buildImageCapture(plan: CapturePlan): ImageCapture {
    // 【v1.7.8 画质】MAXIMIZE_QUALITY：关闭零快门延迟，换取厂商全链路后处理
    // （多帧合成 / HDR / 降噪）与最高输出分辨率；MINIMIZE_LATENCY 会跳过这些管线，
    // 是 v1.7.7 及以前"POP 即拍画质非常差"的主因之一。
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    if (plan.manualExposure) {
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF
        )
        // 【v1.7.8】意图用 STILL_CAPTURE 而非 MANUAL：MANUAL 意图会让 HAL 关闭
        // 厂商画质增强（ColorOS 的 HDR / 多帧降噪在 MANUAL 下不触发，成片动态
        // 范围与噪点明显劣化）；STILL_CAPTURE + CONTROL_AE_MODE_OFF 是标准手动
        // 模式语义——手动参数全权生效，厂商画质管线保持开启。
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_CAPTURE_INTENT, CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
        )
        plan.iso?.let {
            extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
        }
        plan.shutterNs?.let {
            extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
            extender.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, it)
        }
    } else {
        plan.evSteps?.let { steps ->
            val extender = Camera2Interop.Extender(builder)
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, steps)
        }
    }
    return builder.build()
}

@Composable
private fun ViewfinderPlaceholder(accent: Color) {
    Box(Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // 对焦框四角
            val s = 60f; val m = 40f
            val c = accent
            drawLine(c, Offset(w/2-s, h/2-s), Offset(w/2-s+m, h/2-s), 3f)
            drawLine(c, Offset(w/2-s, h/2-s), Offset(w/2-s, h/2-s+m), 3f)
            drawLine(c, Offset(w/2+s, h/2-s), Offset(w/2+s-m, h/2-s), 3f)
            drawLine(c, Offset(w/2+s, h/2-s), Offset(w/2+s, h/2-s+m), 3f)
            drawLine(c, Offset(w/2+s, h/2+s), Offset(w/2+s+m, h/2+s), 3f)
            drawLine(c, Offset(w/2+s, h/2+s), Offset(w/2+s, h/2+s-m), 3f)
            drawLine(c, Offset(w/2-s, h/2+s), Offset(w/2-s+m, h/2+s), 3f)
            drawLine(c, Offset(w/2-s, h/2+s), Offset(w/2-s, h/2+s-m), 3f)
        }
        Text(stringResource(R.string.pop_viewfinder_hint), color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(AppDesign.ContentPadding),
            style = MaterialTheme.typography.labelMedium)
    }
}

/** 参数应用状态卡：如实分级，避免"展示即套用"的误导 */
@Composable
private fun ParamStatusCard(plan: CapturePlan, accent: Color) {
    val written = plan.params.filter { it.status == ApplyStatus.WRITTEN }
    val post = plan.params.filter { it.status == ApplyStatus.POST }
    val unsupported = plan.params.filter { it.status == ApplyStatus.UNSUPPORTED }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = AppDesign.CardShape, modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(AppDesign.ContentPadding)) {
            Text(stringResource(R.string.pop_applied_params), color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (plan.params.isEmpty()) {
                Text(stringResource(R.string.pop_no_params), style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            if (written.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_written), SuccessGreen)
                ParamChipRow(written)
            }
            if (post.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_post), accent)
                ParamChipRow(post)
            }
            if (unsupported.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_unsupported), ErrorRed)
                ParamChipRow(unsupported)
                Text(
                    stringResource(R.string.pop_unsupported_reasons),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ParamChipRow(params: List<ParamStatus>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        params.forEach { p ->
            // 【v1.7.7 P2 修复】参数卡是只读信息，AssistChip 强制 onClick={} 构成
            // "可点击假象"（点击无任何反应）。改用 Surface 非交互展示，视觉等价。
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = when (p.status) {
                    ApplyStatus.WRITTEN -> SuccessGreen
                    ApplyStatus.POST -> MaterialTheme.colorScheme.onSurface
                    ApplyStatus.UNSUPPORTED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(AppDesign.SmallRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "${paramLabel(p.key)} ${p.value}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun paramLabel(key: ParamKey): String = stringResource(
    when (key) {
        ParamKey.ISO -> R.string.param_iso
        ParamKey.SHUTTER -> R.string.param_shutter
        ParamKey.EV -> R.string.param_exposure
        ParamKey.COLOR_TEMP -> R.string.param_color_temp
        ParamKey.FILTER -> R.string.param_filter
        ParamKey.SATURATION -> R.string.param_saturation
        ParamKey.TONE -> R.string.param_tone_curve
        ParamKey.WARM_COOL -> R.string.param_warm_cool
        ParamKey.CYAN_MAGENTA -> R.string.param_cyan_magenta
        ParamKey.SOFT_LIGHT -> R.string.param_soft_light
        ParamKey.SHARPNESS -> R.string.param_sharpness
        ParamKey.VIGNETTE -> R.string.param_vignette
        ParamKey.COLOR_HUE -> R.string.param_tone
    }
)

// ---------- Android 层工具 ----------

/** 系统相机临时照片：FileProvider Uri + 实体文件（读取完成后即删，防缓存累积） */
private fun createPhotoFile(context: Context): Pair<File, Uri> {
    val dir = File(context.cacheDir, "pop_capture").apply { mkdirs() }
    val file = File(dir, "pop_${System.currentTimeMillis()}.jpg")
    return file to FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        // 先读边界；【v1.7.8】与相机成片同一内存守卫（PresetPixelEffects.chooseStyleMaxSide）：
        // 堆充裕时保留原图全分辨率（相册大图同样受益），堆紧张时按面积等比收缩，
        // 不再固定 2048px 上限——2048px 会让 12MP 原图细节损失 4 倍像素。
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return@withContext null
        val guardMax = PresetPixelEffects.chooseStyleMaxSide(w, h, Runtime.getRuntime().maxMemory())
        var sample = 1
        if (guardMax > 0) {
            while (maxOf(w, h) / sample > guardMax) sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return@withContext null

        // EXIF 方向修正（系统相机/相册图片常见旋转 90°/180°）
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees }
        }.getOrNull() ?: 0
        val oriented = if (degrees == 0) decoded else rotateBitmap(decoded, degrees.toFloat())

        // inSampleSize 为 2 的幂可能略超守卫上限，精确缩放到目标边长
        if (guardMax > 0 && maxOf(oriented.width, oriented.height) > guardMax) {
            val scale = guardMax.toFloat() / maxOf(oriented.width, oriented.height)
            val sw = (oriented.width * scale).toInt().coerceAtLeast(1)
            val sh = (oriented.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(oriented, sw, sh, true).also { oriented.recycle() }
        } else {
            oriented
        }
    } catch (e: Exception) { null }
}

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return src
    val matrix = Matrix().apply { postRotate(degrees) }
    return try {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true).also {
            if (it !== src) src.recycle()
        }
    } catch (e: Exception) { src }
}

/** 取景/成片预览区四态（AnimatedContent 过渡目标，实时帧不参与状态切换） */
private enum class PreviewMode { Result, Live, Error, Placeholder }
