package com.silas.omaster.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.FileProvider
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
 * 链路：加载预设 → 能力探测分级 → 展示已套用参数（能力不足项灰显+原因）→
 * 系统相机拍照或相册选图 → 拍后套色（PresetColorTransform）→ 保存相册。
 *
 * 边界（PRD 2.4）：第三方 App 无法改写 OPPO 原生相机内部参数，故采用
 * "自研套色管线 + 能力分级提示"；L1 设备可完整套用调色参数，L3 降级为
 * 悬浮窗照着调 + 拍后套色。快门按钮防重复触发（状态机 CAPTURING）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopCaptureScreen(
    preset: MasterPreset,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(0xFFEC7223)
    val capability = remember { CameraCapabilityProbe.probe(context) }
    var appliedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val localizedPresetName = PresetI18n.getLocalizedPresetName(context, preset.name)

    // 文案在组合作用域预取（LocalContext 上直接 getString 会被 lint 拦截），供协程/回调复用
    val msgApplied = stringResource(R.string.pop_applied_style, localizedPresetName)
    val msgReadFailed = stringResource(R.string.pop_read_failed)
    val msgSaved = stringResource(R.string.pop_saved)
    val msgSaveFailed = stringResource(R.string.pop_save_failed)
    val msgNeedPhoto = stringResource(R.string.pop_need_photo)

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

            // 取景/成片预览区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = appliedBitmap
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = stringResource(R.string.pop_preview),
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    // 取景框占位 + 对焦框示意
                    ViewfinderPlaceholder(accent)
                }
            }

            // 已套用参数胶囊
            AppliedParamsChips(preset, accent)

            // 快门 + 相册入口
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 相册选图（离线套色）
                SmallFloatingActionButton(
                    onClick = { if (!isCapturing) pickImageLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = stringResource(R.string.pop_pick_from_gallery))
                }
                Spacer(Modifier.width(28.dp))
                // 快门按钮（防重复触发）
                Button(
                    onClick = {
                        if (isCapturing) return@Button
                        val uri = createPhotoUri(context)
                        photoUri = uri
                        isCapturing = true
                        takePictureLauncher.launch(uri)
                    },
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.pop_shutter), modifier = Modifier.size(32.dp))
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
        // 提示托管在 Scaffold snackbarHost，自动获得位置与消失行为
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
            toast = null
        }
    }
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
            drawLine(c, Offset(w/2-s, h/2+s), Offset(w/2-s+m, h/2+s), 3f)
            drawLine(c, Offset(w/2-s, h/2+s), Offset(w/2-s, h/2+s-m), 3f)
            drawLine(c, Offset(w/2+s, h/2+s), Offset(w/2+s-m, h/2+s), 3f)
            drawLine(c, Offset(w/2+s, h/2+s), Offset(w/2+s, h/2+s-m), 3f)
        }
        Text(stringResource(R.string.pop_viewfinder_hint), color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            fontSize = 12.sp)
    }
}

@Composable
private fun AppliedParamsChips(preset: MasterPreset, accent: Color) {
    val params = buildList {
        preset.iso?.let { add("ISO $it" to true) }
        preset.shutterSpeed?.let { add("${it}s" to true) }
        preset.colorTemperature?.let { add("${it}K" to true) }
        preset.exposureCompensation?.let { add(it to true) }
        preset.filter?.let { add(it to true) }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.pop_applied_params), color = accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (params.isEmpty()) {
                Text(stringResource(R.string.pop_no_params), style = MaterialTheme.typography.bodySmall)
            } else {
                // 参数胶囊较多时横向滚动，避免溢出裁切
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    params.forEach { (text, _) ->
                        AssistChip(onClick = {}, label = { Text(text, fontSize = 11.sp) })
                    }
                }
            }
        }
    }
}

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
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    } catch (e: Exception) { null }
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
