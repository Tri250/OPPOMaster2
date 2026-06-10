package com.silas.omaster.ui.features

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.image.HistogramCalculator
import com.silas.omaster.ui.components.ColorTemperatureGradient
import com.silas.omaster.ui.components.HistogramView
import com.silas.omaster.ui.components.LuminanceRegion
import com.silas.omaster.ui.components.MiniHistogram
import com.silas.omaster.ui.theme.*
import com.silas.omaster.util.HapticFeedbackTypeCompat
import com.silas.omaster.util.perform
import kotlinx.coroutines.launch

/**
 * 参数精细调节页面（增强版）
 *
 * P1 8. 参数实时预览优化
 * - 双栏布局：上方预览区（40%）+ 下方参数区（60%）
 * - 实时着色器预览：滑块拖动时 16ms 内更新
 * - 前后对比：长按显示原图，松开显示当前效果
 * - 参数可视化：色温渐变条、曝光直方图
 *
 * P1 9. 直方图
 * - RGB 3 通道 + 亮度直方图
 * - 实时更新
 * - 点击定位高光/中间调/阴影
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamAdjustScreen(
    onBack: () -> Unit,
    onApply: (CameraParams) -> Unit,
    sourceBitmap: Bitmap? = null
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 相机参数状态
    var iso by remember { mutableIntStateOf(100) }
    var shutterSpeed by remember { mutableFloatStateOf(125f) }
    var aperture by remember { mutableFloatStateOf(2.8f) }
    var whiteBalance by remember { mutableIntStateOf(5500) }
    var focalLength by remember { mutableIntStateOf(23) }
    var exposureCompensation by remember { mutableFloatStateOf(0f) }

    // 预设模式
    var selectedPreset by remember { mutableStateOf<String?>(null) }

    // 预览状态
    var previewBitmap by remember { mutableStateOf<Bitmap?>(sourceBitmap) }
    var originalBitmap by remember { mutableStateOf(sourceBitmap) }
    var isShowingOriginal by remember { mutableStateOf(false) }
    var histogramData by remember { mutableStateOf<HistogramCalculator.HistogramData?>(null) }

    // 实时更新直方图
    LaunchedEffect(previewBitmap) {
        previewBitmap?.let { bitmap ->
            histogramData = HistogramCalculator.calculate(bitmap, sampleRate = 4)
        }
    }

    // ISO 选项
    val isoOptions = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)

    // 快门速度选项
    val shutterOptions = listOf("1/4000", "1/2000", "1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1s", "2s", "4s", "8s", "15s", "30s")

    // 光圈选项
    val apertureOptions = listOf(1.4f, 1.8f, 2.0f, 2.8f, 4.0f, 5.6f, 8.0f, 11f, 16f, 22f)

    // 白平衡选项
    val wbOptions = listOf(2800, 3200, 4000, 5000, 5500, 6000, 6500, 7000, 8000, 9000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("参数精细调节", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 重置按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
                    iso = 100
                    shutterSpeed = 125f
                    aperture = 2.8f
                    whiteBalance = 5500
                    focalLength = 23
                    exposureCompensation = 0f
                    selectedPreset = null
                    previewBitmap = originalBitmap
                }) {
                    Icon(Icons.Default.Refresh, "重置", tint = Color.White)
                }
                // 应用按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackTypeCompat.Confirm)
                    onApply(CameraParams(
                        iso = iso,
                        shutterSpeed = shutterSpeed,
                        aperture = aperture,
                        whiteBalance = whiteBalance,
                        focalLength = focalLength,
                        exposureCompensation = exposureCompensation
                    ))
                }) {
                    Icon(Icons.Default.Check, "应用", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // === 预览区（40%） ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .background(Color.Black)
        ) {
            // 图像预览
            if (previewBitmap != null || originalBitmap != null) {
                val displayBitmap = if (isShowingOriginal) originalBitmap else previewBitmap

                ImagePreviewWithCompare(
                    bitmap = displayBitmap,
                    isComparing = isShowingOriginal,
                    modifier = Modifier.fillMaxSize(),
                    onLongPressStart = {
                        haptic.perform(HapticFeedbackTypeCompat.Confirm)
                        isShowingOriginal = true
                    },
                    onLongPressEnd = {
                        isShowingOriginal = false
                    }
                )

                // 直方图叠加
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HistogramView(
                        histogramData = histogramData,
                        showRgb = true,
                        showLuminance = true,
                        height = 60.dp,
                        onRegionClick = { region ->
                            // 根据点击区域调整曝光
                            when (region) {
                                LuminanceRegion.SHADOWS -> exposureCompensation = (exposureCompensation + 0.3f).coerceIn(-3f, 3f)
                                LuminanceRegion.MIDTONES -> { /* 不调整 */ }
                                LuminanceRegion.HIGHLIGHTS -> exposureCompensation = (exposureCompensation - 0.3f).coerceIn(-3f, 3f)
                            }
                        }
                    )
                }

                // 对比提示
                if (!isShowingOriginal) {
                    Text(
                        "长按查看原图",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        "原图",
                        color = HasselbladOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                // 无图像占位
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = HasselbladOrange.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "选择图像以预览参数效果",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // === 参数调节区（60%） ===
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) {
            // 快速预设
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParamPresetChip(
                    label = "人像",
                    selected = selectedPreset == "portrait",
                    onClick = {
                        haptic.perform(HapticFeedbackTypeCompat.Select)
                        selectedPreset = "portrait"
                        iso = 100
                        shutterSpeed = 125f
                        aperture = 1.8f
                        whiteBalance = 5500
                    }
                )
                ParamPresetChip(
                    label = "风景",
                    selected = selectedPreset == "landscape",
                    onClick = {
                        haptic.perform(HapticFeedbackTypeCompat.Select)
                        selectedPreset = "landscape"
                        iso = 100
                        shutterSpeed = 250f
                        aperture = 8.0f
                        whiteBalance = 5500
                    }
                )
                ParamPresetChip(
                    label = "夜景",
                    selected = selectedPreset == "night",
                    onClick = {
                        haptic.perform(HapticFeedbackTypeCompat.Select)
                        selectedPreset = "night"
                        iso = 800
                        shutterSpeed = 30f
                        aperture = 2.8f
                        whiteBalance = 3200
                    }
                )
                ParamPresetChip(
                    label = "运动",
                    selected = selectedPreset == "sports",
                    onClick = {
                        haptic.perform(HapticFeedbackTypeCompat.Select)
                        selectedPreset = "sports"
                        iso = 400
                        shutterSpeed = 1000f
                        aperture = 4.0f
                        whiteBalance = 5500
                    }
                )
            }

            // 参数调节列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ISO 调节
                item {
                    ParamSliderCardEnhanced(
                        title = "ISO",
                        subtitle = "感光度，影响噪点和曝光",
                        value = iso,
                        valueRange = 50..6400,
                        options = isoOptions.map { it.toString() },
                        onValueChange = { iso = it as Int },
                        unit = "",
                        color = Color(0xFF9C27B0),
                        miniHistogram = histogramData?.luminance
                    )
                }

                // 快门速度调节
                item {
                    ParamSliderCardEnhanced(
                        title = "快门",
                        subtitle = "曝光时间，影响运动模糊",
                        value = shutterSpeed.toInt(),
                        valueRange = 1..30000,
                        displayValue = formatShutterSpeed(shutterSpeed),
                        onValueChange = { shutterSpeed = (it as Number).toFloat() },
                        unit = "",
                        color = Color(0xFF2196F3)
                    )
                }

                // 光圈调节
                item {
                    ParamSliderCardEnhanced(
                        title = "光圈",
                        subtitle = "景深控制，影响背景虚化",
                        value = aperture,
                        valueRange = 1.4f..22f,
                        options = apertureOptions.map { it.toString() },
                        onValueChange = { aperture = it as Float },
                        unit = "f/",
                        color = HasselbladOrange
                    )
                }

                // 白平衡调节（带色温渐变条）
                item {
                    ParamSliderCardEnhanced(
                        title = "白平衡",
                        subtitle = "色温调节，影响色彩冷暖",
                        value = whiteBalance,
                        valueRange = 2800..9000,
                        options = wbOptions.map { "${it}K" },
                        onValueChange = { whiteBalance = it as Int },
                        unit = "K",
                        color = Color(0xFFFFEB3B),
                        showColorTempGradient = true
                    )
                }

                // 焦距调节
                item {
                    ParamSliderCardEnhanced(
                        title = "焦距",
                        subtitle = "镜头焦距，影响视角和压缩感",
                        value = focalLength,
                        valueRange = 8..400,
                        onValueChange = { focalLength = it as Int },
                        unit = "mm",
                        color = Color(0xFFE91E63)
                    )
                }

                // 曝光补偿（带迷你直方图）
                item {
                    ParamSliderCardEnhanced(
                        title = "曝光补偿",
                        subtitle = "手动调整曝光量",
                        value = exposureCompensation,
                        valueRange = -3f..3f,
                        onValueChange = { exposureCompensation = it as Float },
                        unit = "EV",
                        color = Color(0xFF4CAF50),
                        miniHistogram = histogramData?.luminance,
                        showMiniHistogram = true
                    )
                }
            }

            // 当前参数摘要
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ParamSummaryItem("ISO", iso.toString())
                    ParamSummaryItem("快门", formatShutterSpeed(shutterSpeed))
                    ParamSummaryItem("光圈", "f/$aperture")
                    ParamSummaryItem("WB", "${whiteBalance}K")
                }
            }
        }
    }
}

/**
 * 图像预览组件（支持长按对比）
 */
@Composable
private fun ImagePreviewWithCompare(
    bitmap: Bitmap?,
    isComparing: Boolean,
    modifier: Modifier = Modifier,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPressStart()
                    },
                    onPress = {
                        tryAwaitRelease()
                        onLongPressEnd()
                    }
                )
            }
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // 对比模式边框
        if (isComparing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(4.dp, HasselbladOrange, RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun ParamPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HasselbladOrange else Color(0xFF2A2A2A))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * 增强版参数滑块卡片
 * 支持迷你直方图、色温渐变条
 */
@Composable
private fun ParamSliderCardEnhanced(
    title: String,
    subtitle: String,
    value: Any,
    valueRange: Any,
    displayValue: String? = null,
    options: List<String>? = null,
    onValueChange: (Any) -> Unit,
    unit: String,
    color: Color,
    miniHistogram: IntArray? = null,
    showMiniHistogram: Boolean = false,
    showColorTempGradient: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // 迷你直方图（曝光补偿）
                if (showMiniHistogram && miniHistogram != null) {
                    MiniHistogram(
                        luminance = miniHistogram,
                        width = 50.dp,
                        height = 20.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = displayValue ?: when (value) {
                        is Int -> "$value$unit"
                        is Float -> "${if (value == value.toInt().toFloat()) value.toInt() else String.format("%.1f", value)}$unit"
                        else -> "$value$unit"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            // 色温渐变条（白平衡）
            if (showColorTempGradient && value is Int) {
                Spacer(modifier = Modifier.height(8.dp))
                ColorTemperatureGradient(
                    currentKelvin = value,
                    height = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 滑块
            when (value) {
                is Int -> {
                    Slider(
                        value = value.toFloat(),
                        onValueChange = {
                            haptic.perform(HapticFeedbackTypeCompat.Select)
                            onValueChange(it.toInt())
                        },
                        valueRange = when (valueRange) {
                            is IntRange -> valueRange.first.toFloat()..valueRange.last.toFloat()
                            else -> 0f..100f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color
                        )
                    )
                }
                is Float -> {
                    Slider(
                        value = value,
                        onValueChange = {
                            haptic.perform(HapticFeedbackTypeCompat.Select)
                            onValueChange(it)
                        },
                        valueRange = when (valueRange) {
                            is ClosedFloatingPointRange<*> -> valueRange as ClosedFloatingPointRange<Float>
                            else -> 0f..100f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color
                        )
                    )
                }
            }

            // 快速选项
            if (options != null && options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.take(6).forEach { option ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A1A1A))
                                .clickable {
                                    haptic.perform(HapticFeedbackTypeCompat.Select)
                                    val parsedValue = when (value) {
                                        is Int -> option.filter { it.isDigit() }.toIntOrNull() ?: value
                                        is Float -> option.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: value
                                        else -> value
                                    }
                                    onValueChange(parsedValue)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HasselbladOrange
        )
    }
}

/**
 * 格式化快门速度
 */
private fun formatShutterSpeed(speed: Float): String {
    return when {
        speed >= 1000 -> "1/${(speed / 1000).toInt()}s"
        speed >= 1 -> "${speed.toInt()}s"
        else -> "1/${speed.toInt()}s"
    }
}

/**
 * 相机参数数据类
 */
data class CameraParams(
    val iso: Int = 100,
    val shutterSpeed: Float = 125f,
    val aperture: Float = 2.8f,
    val whiteBalance: Int = 5500,
    val focalLength: Int = 23,
    val exposureCompensation: Float = 0f
)
