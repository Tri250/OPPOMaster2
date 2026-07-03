package com.silas.omaster.video

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.data.repository.LUTResourceRepository
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 视频滤镜页面
 *
 * 功能：
 * - 从相册选择视频
 * - 选择 LUT 滤镜或哈苏调色参数
 * - 实时预览滤镜效果（首帧）
 * - 导出处理后的视频
 *
 * 对齐 v2.3.0 视频滤镜支持计划
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFilterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val engine = remember { VideoFilterEngine(context) }
    val lutManager = remember { LUTManager.getInstance(context) }

    // 视频选择
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var videoDuration by remember { mutableStateOf("") }

    // 滤镜选择
    var selectedLUT by remember { mutableStateOf<LUTResource?>(null) }
    val downloadedLuts by lutManager.downloadedIds.collectAsState()
    val allLuts = remember { LUTResourceRepository.RESOURCES }
    val availableLuts = remember { allLuts.filter { downloadedLuts.contains(it.id) } }

    // 哈苏参数
    var hdrParams by remember { mutableStateOf(HasselbladParams()) }

    // 处理状态
    var isProcessing by remember { mutableStateOf(false) }
    val progress by engine.progress.collectAsState()
    var processingResult by remember { mutableStateOf<VideoFilterEngine.ProcessResult?>(null) }

    // 视频选择器（Android 16+ Photo Picker）
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedVideoUri = it
            videoName = "视频_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
            // 获取视频时长
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, it)
                val durationMs = retriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                val seconds = durationMs / 1000
                videoDuration = "${seconds / 60}:${String.format("%02d", seconds % 60)}"
                retriever.release()
            } catch (_: Exception) {
                videoDuration = "--:--"
            }
        }
    }

    // 参数调节面板
    var showParamPanel by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text("视频滤镜", fontWeight = FontWeight.Bold)
                    Text(
                        "v2.3.0 · 视频调色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 视频选择区 ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("选择视频", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedVideoUri != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(HasselbladOrange, WarningYellow)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "视频",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    videoName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "时长: $videoDuration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = {
                                selectedVideoUri = null
                                processingResult = null
                            }) {
                                Icon(Icons.Default.Close, "清除", tint = ErrorRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册选择视频")
                        }
                    }
                }
            }

            // === LUT 滤镜选择 ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("LUT 滤镜", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "选择已下载的 LUT 滤镜应用到视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (availableLuts.isEmpty()) {
                        Text(
                            "暂无已下载的 LUT，请先在 LUT 资源库中下载",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 无滤镜选项
                            FilterChip(
                                selected = selectedLUT == null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedLUT = null
                                },
                                label = { Text("无滤镜", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = HasselbladOrange
                                )
                            )
                            availableLuts.take(6).forEach { lut ->
                                FilterChip(
                                    selected = selectedLUT?.id == lut.id,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedLUT = lut
                                    },
                                    label = {
                                        Text(
                                            lut.name,
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
                    }
                }
            }

            // === 哈苏参数调节 ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("哈苏参数", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                "饱和度: ${hdrParams.saturation} | 对比度: ${hdrParams.contrast} | 色温: ${hdrParams.colorTemp}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showParamPanel = !showParamPanel
                        }) {
                            Icon(
                                Icons.Default.Tune,
                                "参数调节",
                                tint = if (showParamPanel) HasselbladOrange
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (showParamPanel) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ParamSlider("饱和度", hdrParams.saturation, -100, 100) { value ->
                            hdrParams = hdrParams.copy(saturation = value)
                        }
                        ParamSlider("对比度", hdrParams.contrast, -100, 100) { value ->
                            hdrParams = hdrParams.copy(contrast = value)
                        }
                        ParamSlider("色温", hdrParams.colorTemp, -100, 100) { value ->
                            hdrParams = hdrParams.copy(colorTemp = value)
                        }
                        ParamSlider("清晰度", hdrParams.clarity, 0, 100) { value ->
                            hdrParams = hdrParams.copy(clarity = value)
                        }
                        ParamSlider("锐化", hdrParams.sharpness, 0, 100) { value ->
                            hdrParams = hdrParams.copy(sharpness = value)
                        }
                    }
                }
            }

            // === 处理按钮 ===
            if (selectedVideoUri != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isProcessing) {
                            engine.cancel()
                            return@Button
                        }
                        isProcessing = true
                        processingResult = null
                        scope.launch {
                            val outputFile = createOutputFile(context)
                            val lutData: LUT3DData? = null
                            val result = engine.processVideo(
                                inputUri = selectedVideoUri!!,
                                outputFile = outputFile,
                                params = if (hdrParams.hasAnyAdjustment()) hdrParams else null,
                                lutData = lutData,
                                onProgress = { /* progress 通过 StateFlow 更新 */ }
                            )
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                processingResult = result
                                if (result.success) {
                                    Toast.makeText(context, "视频处理完成！", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "处理失败: ${result.error ?: "未知错误"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing || true, // 允许取消
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) ErrorRed else SuccessGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "处理中 ${(progress.percentage * 100).toInt()}% · 点击取消",
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(Icons.Default.Movie, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始处理视频", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // === 处理进度 ===
            if (isProcessing && progress.totalFrames > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "处理进度",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress.percentage },
                            modifier = Modifier.fillMaxWidth(),
                            color = HasselbladOrange,
                            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "帧 ${progress.currentFrame} / ${progress.totalFrames}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // === 处理结果 ===
            processingResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.success) SuccessGreen.copy(alpha = 0.1f)
                        else ErrorRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                null,
                                tint = if (result.success) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (result.success) "处理完成" else "处理失败",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (result.success) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "共 ${result.totalFrames} 帧 · 耗时 ${result.durationMs / 1000}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Text(
                                "输出: ${result.outputFile.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        // 分享视频
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            result.outputFile
                                        )
                                        val shareIntent = android.content.Intent(
                                            android.content.Intent.ACTION_SEND
                                        ).apply {
                                            type = "video/*"
                                            putExtra(android.content.Intent.EXTRA_STREAM, shareUri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            android.content.Intent.createChooser(shareIntent, "分享视频")
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("分享", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        // 保存到相册
                                        saveVideoToGallery(context, result.outputFile)
                                        Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("保存到相册", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                result.error ?: "未知错误",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 参数滑块组件
 */
@Composable
private fun ParamSlider(
    label: String,
    value: Int,
    rangeMin: Int,
    rangeMax: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                "$value",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = HasselbladOrange
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = rangeMin.toFloat()..rangeMax.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            )
        )
    }
}

/**
 * 创建输出文件
 */
private fun createOutputFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.cacheDir, "video_filter")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "OMaster_${timestamp}.mp4")
}

/**
 * 保存视频到系统相册
 */
private fun saveVideoToGallery(context: Context, videoFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OMaster")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                videoFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATA, videoFile.absolutePath)
        }
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }
}

/**
 * 扩展函数：检查是否有任何参数调整
 */
private fun HasselbladParams.hasAnyAdjustment(): Boolean {
    return saturation != 0 || contrast != 0 || colorTemp != 0 ||
            clarity != 0 || sharpness != 0 || tone != 0
}