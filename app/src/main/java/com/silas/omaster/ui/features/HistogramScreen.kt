package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.histogram.HistogramManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 直方图分析页面
 * 同步 Web 设计：RGB/R/G/B/Luminance 直方图
 * 显示参数调节的参考信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistogramScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { HistogramManager.getInstance() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var histogramData by remember { mutableStateOf<HistogramManager.HistogramData?>(null) }
    var stats by remember { mutableStateOf<HistogramManager.HistogramStats?>(null) }
    var exposureIssues by remember { mutableStateOf<List<HistogramManager.ExposureIssue>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf(HistogramManager.HistogramChannel.RGB) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    bitmap?.let { bmp ->
                        val data = manager.calculate(bmp)
                        histogramData = data
                        stats = manager.calculateStats(data, selectedChannel)
                        exposureIssues = manager.detectExposureIssues(data)
                    }
                } catch (e: Exception) {
                    // 错误处理
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.histogram_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.histogram_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 加载图片按钮
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            imagePicker.launch("image/*")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = HasselbladOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.histogram_load_image),
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 通道选择
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(HistogramManager.HistogramChannel.entries) { channel ->
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                selectedChannel = channel
                                histogramData?.let { data ->
                                    stats = manager.calculateStats(data, channel)
                                }
                            },
                            label = { Text(channel.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                                selectedLabelColor = HasselbladOrange
                            )
                        )
                    }
                }
            }

            // 直方图画布
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (histogramData == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.histogram_load_image),
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            val data = histogramData!!
                            val normalized = data.normalized(100)
                            HistogramCanvas(
                                normalized = normalized,
                                channel = selectedChannel
                            )
                        }
                    }
                }
            }

            // 统计信息
            stats?.let { s ->
                item {
                    StatsCard(s)
                }
            }

            // 曝光问题
            if (exposureIssues.isNotEmpty()) {
                item {
                    Text(
                        text = "曝光评估",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(exposureIssues) { issue ->
                    ExposureIssueCard(issue)
                }
            }
        }
    }
}

@Composable
private fun HistogramCanvas(
    normalized: HistogramManager.NormalizedHistogram,
    channel: HistogramManager.HistogramChannel
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .background(Color.Black)
    ) {
        val w = size.width
        val h = size.height
        val padding = 10f

        // 网格
        for (i in 0..3) {
            val pos = padding + (w - 2 * padding) * i / 3
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(pos, padding),
                end = Offset(pos, h - padding),
                strokeWidth = 1f
            )
        }

        // 直方图数据
        val data: FloatArray = when (channel) {
            HistogramManager.HistogramChannel.RGB -> normalized.rgb
            HistogramManager.HistogramChannel.RED -> normalized.red
            HistogramManager.HistogramChannel.GREEN -> normalized.green
            HistogramManager.HistogramChannel.BLUE -> normalized.blue
            HistogramManager.HistogramChannel.LUMINANCE -> normalized.luminance
        }

        val colors: List<Color> = when (channel) {
            HistogramManager.HistogramChannel.RGB -> listOf(
                Color(0xFFE53935).copy(alpha = 0.7f),
                Color(0xFF43A047).copy(alpha = 0.7f),
                Color(0xFF1E88E5).copy(alpha = 0.7f)
            )
            else -> listOf(
                when (channel) {
                    HistogramManager.HistogramChannel.RED -> Color(0xFFE53935)
                    HistogramManager.HistogramChannel.GREEN -> Color(0xFF43A047)
                    HistogramManager.HistogramChannel.BLUE -> Color(0xFF1E88E5)
                    else -> Color.White.copy(alpha = 0.8f)
                }
            )
        }

        // 绘制 RGB 三色叠加
        if (channel == HistogramManager.HistogramChannel.RGB) {
            listOf(
                Pair(normalized.red, Color(0xFFE53935).copy(alpha = 0.6f)),
                Pair(normalized.green, Color(0xFF43A047).copy(alpha = 0.6f)),
                Pair(normalized.blue, Color(0xFF1E88E5).copy(alpha = 0.6f))
            ).forEach { (arr, col) ->
                val path = Path()
                for (i in arr.indices) {
                    val x = padding + (w - 2 * padding) * i / 255f
                    val y = h - padding - (h - 2 * padding) * (arr[i] / 100f).coerceIn(0f, 1f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = col,
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )
            }
        } else {
            val path = Path()
            for (i in data.indices) {
                val x = padding + (w - 2 * padding) * i / 255f
                val y = h - padding - (h - 2 * padding) * (data[i] / 100f).coerceIn(0f, 1f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = colors.first(),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun StatsCard(stats: HistogramManager.HistogramStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "统计信息",
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            StatRow(stringResource(R.string.histogram_mean, stats.mean))
            StatRow(stringResource(R.string.histogram_median, stats.median))
            StatRow(stringResource(R.string.histogram_std_dev, stats.stdDev))
            StatRow(stringResource(R.string.histogram_dynamic_range, stats.dynamicRange))
        }
    }
}

@Composable
private fun StatRow(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun ExposureIssueCard(issue: HistogramManager.ExposureIssue) {
    val (title, color) = when (issue) {
        is HistogramManager.ExposureIssue.Overexposed -> stringResource(R.string.histogram_overexposed) to Color(0xFFFF9800)
        is HistogramManager.ExposureIssue.Underexposed -> stringResource(R.string.histogram_underexposed) to Color(0xFF2196F3)
        is HistogramManager.ExposureIssue.LowContrast -> "低对比度" to Color(0xFFFFC107)
        is HistogramManager.ExposureIssue.HighContrast -> "高对比度" to Color(0xFF9C27B0)
        is HistogramManager.ExposureIssue.ColorCast -> "色偏" to Color(0xFFE91E63)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
