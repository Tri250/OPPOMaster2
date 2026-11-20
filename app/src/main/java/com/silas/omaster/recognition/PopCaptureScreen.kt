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
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.ImageExporter
import com.silas.omaster.util.PresetI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * POP 预设即拍页（PRD FR-10~14 / 16.2）。
 *
 * 链路：加载预设 → 能力探测分级 → 生成拍摄计划（[PresetCapturePlan]）→
 * 自研相机管线（CameraX + Camera2 Interop）把 ISO/快门/曝光补偿真实写入拍摄请求 →
 * 成片经拍后套色管线（色彩矩阵 + 锐化 + 暗角）→ 保存相册。
 *
 * 降级路径：无相机权限或设备不支持手动曝光时，可用系统相机拍照 / 相册选图，
 * 拍后仍完整套用调色参数。参数卡按"已写入拍摄 / 拍后套用 / 无法套用（含原因）"
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
    val accent = Color(0xFFEC7223)
    val capability = remember { CameraCapabilityProbe.probe(context) }
    val plan = remember(preset, capability) { PresetCapturePlan.build(preset, capability.manualRanges) }

    var appliedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequested = true
        cameraGranted = granted
    }

    // ── 自研相机管线：按拍摄计划配置 ImageCapture（Camera2 Interop 写入请求键） ──
    val previewView = remember { PreviewView(context) }
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember(plan) { buildImageCapture(plan) }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraBound by remember { mutableStateOf(false) }

    DisposableEffect(cameraGranted, imageCapture) {
        if (cameraGranted) {
            val listener = Runnable {
                try {
                    val provider = providerFuture.get()
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    boundProvider = provider
                    cameraBound = true
                } catch (e: Exception) {
                    cameraBound = false
                }
            }
            providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching { boundProvider?.unbindAll() }
                boundProvider = null
                cameraBound = false
            }
        } else {
            onDispose { }
        }
    }

    /** 快门：拍摄 → 拍后套色 → 预览 */
    fun captureWithPreset() {
        if (isCapturing) return
        isCapturing = true
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = try {
                        image.toBitmap()
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
                    scope.launch {
                        appliedBitmap = applyStyle(bmp, preset)
                        toast = msgApplied
                        isCapturing = false
                    }
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
        if (success) {
            photoUri?.let { uri ->
                isCapturing = true
                scope.launch {
                    val bmp = loadBitmap(context, uri)
                    if (bmp != null) {
                        appliedBitmap = applyStyle(bmp, preset)
                        toast = msgApplied
                    } else {
                        toast = msgReadFailed
                    }
                    isCapturing = false
                }
            }
        } else {
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
                    appliedBitmap = applyStyle(bmp, preset)
                    toast = msgApplied
                } else {
                    toast = msgReadFailed
                }
                isCapturing = false
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 设备能力徽章
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (capability.level) {
                        CameraCapabilityProbe.Level.L1 -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                        CameraCapabilityProbe.Level.L2 -> Color(0xFFFFB300).copy(alpha = 0.12f)
                        CameraCapabilityProbe.Level.L3 -> Color(0xFFE53935).copy(alpha = 0.12f)
                    }
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.pop_capability, capability.level.toString(), capability.hardwareLevelString),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(capability.degradationHintRes), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            // 取景 / 成片预览区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = appliedBitmap
                when {
                    bmp != null -> {
                        Image(bmp.asImageBitmap(), contentDescription = stringResource(R.string.pop_preview),
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // 重拍：回到取景（有相机权限）或占位
                        TextButton(
                            onClick = { appliedBitmap = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(stringResource(R.string.pop_retake), color = Color.White, fontSize = 12.sp)
                        }
                    }
                    cameraGranted && cameraBound -> {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    }
                    else -> ViewfinderPlaceholder(accent)
                }
            }

            // 参数应用状态卡（已写入拍摄 / 拍后套用 / 无法套用+原因）
            ParamStatusCard(plan, accent)

            // 相机权限引导（未授权时提供授权入口，同时保留降级路径说明）
            if (!cameraGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.pop_camera_permission_rationale),
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
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
                    // 快门按钮（防重复触发）
                    Button(
                        onClick = { captureWithPreset() },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isCapturing
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
                            val uri = createPhotoUri(context)
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

            // 保存到相册
            Button(
                onClick = {
                    appliedBitmap?.let { bmp ->
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                ImageExporter.saveToGallery(context, bmp)
                            }
                            toast = if (ok) msgSaved else msgSaveFailed
                        }
                    } ?: run { toast = msgNeedPhoto }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = appliedBitmap != null && !isCapturing,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.pop_save_to_gallery), fontWeight = FontWeight.Bold)
            }

            if (isCapturing) {
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
    val builder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    if (plan.manualExposure) {
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF
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
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            fontSize = 12.sp)
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
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.pop_applied_params), color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (plan.params.isEmpty()) {
                Text(stringResource(R.string.pop_no_params), style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            if (written.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_written), Color(0xFF4CAF50))
                ParamChipRow(written)
            }
            if (post.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_post), accent)
                ParamChipRow(post)
            }
            if (unsupported.isNotEmpty()) {
                SectionLabel(stringResource(R.string.pop_params_unsupported), Color(0xFFE53935))
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
    Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
            AssistChip(
                onClick = {},
                label = { Text("${paramLabel(p.key)} ${p.value}", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = when (p.status) {
                        ApplyStatus.WRITTEN -> Color(0xFF2E7D32)
                        ApplyStatus.POST -> MaterialTheme.colorScheme.onSurface
                        ApplyStatus.UNSUPPORTED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            )
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

private fun createPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "pop_capture").apply { mkdirs() }
    val file = File(dir, "pop_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        // 先读边界再按 2048px 上限降采样，避免大图全尺寸解码 OOM
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide <= 0) return@withContext null
        var sample = 1
        while (maxSide / sample > 2048) sample *= 2
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
        if (degrees == 0) decoded else rotateBitmap(decoded, degrees.toFloat())
    } catch (e: Exception) { null }
}

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return src
    val matrix = Matrix().apply { postRotate(degrees) }
    return try {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    } catch (e: Exception) { src }
}

private suspend fun applyStyle(bitmap: Bitmap, preset: MasterPreset): Bitmap = withContext(Dispatchers.Default) {
    try {
        val matrix = PresetColorTransform.buildColorMatrix(preset)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(matrix))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        // 矩阵无法表达的预设参数：锐度（unsharp）与暗角（径向渐变）像素级套用
        val sharpened = PresetPixelEffects.applySharpen(result, PresetColorTransform.sharpnessAmount(preset))
        PresetPixelEffects.applyVignette(sharpened, PresetColorTransform.vignetteStrength(preset))
    } catch (e: Exception) { bitmap }
}
