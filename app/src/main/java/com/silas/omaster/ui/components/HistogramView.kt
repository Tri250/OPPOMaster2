package com.silas.omaster.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silas.omaster.image.HistogramCalculator
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlin.math.max

/**
 * 直方图视图组件
 *
 * 功能：
 * - RGB 3 通道直方图叠加显示
 * - 亮度直方图
 * - 实时更新
 * - 点击定位高光/中间调/阴影
 *
 * 用于：
 * - 参数调节预览区叠加
 * - 曝光分布可视化
 */
@Composable
fun HistogramView(
    histogramData: HistogramCalculator.HistogramData?,
    modifier: Modifier = Modifier,
    showRgb: Boolean = true,
    showLuminance: Boolean = true,
    height: Dp = 80.dp,
    onRegionClick: ((region: LuminanceRegion) -> Unit)? = null
) {
    val normalized = remember(histogramData) {
        histogramData?.normalized() ?: HistogramCalculator.NormalizedHistogram(
            red = FloatArray(256),
            green = FloatArray(256),
            blue = FloatArray(256),
            luminance = FloatArray(256),
            totalPixels = 0,
            meanBrightness = 0f,
            stdDevBrightness = 0f
        )
    }

    var tapX by remember { mutableFloatStateOf(-1f) }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(onRegionClick) {
                if (onRegionClick != null) {
                    detectTapGestures { offset ->
                        tapX = offset.x
                        // 根据点击位置判断区域
                        val region = when {
                            offset.x < size.width * 0.33f -> LuminanceRegion.SHADOWS
                            offset.x < size.width * 0.67f -> LuminanceRegion.MIDTONES
                            else -> LuminanceRegion.HIGHLIGHTS
                        }
                        onRegionClick(region)
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val width = size.width
            val canvasHeight = size.height
            val bucketWidth = width / 256f

            // 绘制亮度直方图（底层，半透明白色）
            if (showLuminance) {
                val path = Path()
                path.moveTo(0f, canvasHeight)
                for (i in normalized.luminance.indices) {
                    val x = i * bucketWidth
                    val y = canvasHeight * (1f - normalized.luminance[i])
                    path.lineTo(x, y)
                }
                path.lineTo(width, canvasHeight)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
            }

            // 绘制 RGB 直方图（叠加）
            if (showRgb) {
                // 红色通道
                drawHistogramChannel(
                    values = normalized.red,
                    color = Color.Red.copy(alpha = 0.5f),
                    bucketWidth = bucketWidth,
                    height = canvasHeight
                )

                // 绿色通道
                drawHistogramChannel(
                    values = normalized.green,
                    color = Color.Green.copy(alpha = 0.5f),
                    bucketWidth = bucketWidth,
                    height = canvasHeight
                )

                // 蓝色通道
                drawHistogramChannel(
                    values = normalized.blue,
                    color = Color.Blue.copy(alpha = 0.5f),
                    bucketWidth = bucketWidth,
                    height = canvasHeight
                )
            }

            // 绘制平均亮度指示线
            if (normalized.meanBrightness > 0) {
                val meanX = normalized.meanBrightness / 255f * width
                drawLine(
                    color = HasselbladOrange,
                    start = Offset(meanX, 0f),
                    end = Offset(meanX, canvasHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 点击位置指示
            if (tapX >= 0) {
                drawLine(
                    color = Color.White,
                    start = Offset(tapX, 0f),
                    end = Offset(tapX, canvasHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // 曝光分析标签
        if (histogramData != null) {
            val analysis = histogramData.analyzeExposure()
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (analysis.underexposedRatio > 0.1f) {
                    Text(
                        "欠曝 ${(analysis.underexposedRatio * 100).toInt()}%",
                        color = Color(0xFF2196F3),
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        style = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    )
                }
                if (analysis.overexposedRatio > 0.1f) {
                    Text(
                        "过曝 ${(analysis.overexposedRatio * 100).toInt()}%",
                        color = Color(0xFFFF5722),
                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                        style = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHistogramChannel(
    values: FloatArray,
    color: Color,
    bucketWidth: Float,
    height: Float
) {
    val path = Path()
    path.moveTo(0f, height)
    for (i in values.indices) {
        val x = i * bucketWidth
        val y = height * (1f - values[i])
        path.lineTo(x, y)
    }
    path.lineTo(values.size * bucketWidth, height)
    path.close()

    drawPath(
        path = path,
        color = color
    )
}

/**
 * 亮度区域
 */
enum class LuminanceRegion(val displayName: String, val range: IntRange) {
    SHADOWS("阴影", 0..85),
    MIDTONES("中间调", 86..170),
    HIGHLIGHTS("高光", 171..255)
}

/**
 * 迷你直方图（用于参数滑块旁）
 */
@Composable
fun MiniHistogram(
    luminance: IntArray?,
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
    height: Dp = 24.dp
) {
    if (luminance == null) {
        Box(
            modifier = modifier
                .size(width, height)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
        return
    }

    val maxVal = max(1, luminance.maxOrNull() ?: 1)

    Canvas(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
        val bucketWidth = size.width / luminance.size
        val canvasHeight = size.height

        val path = Path()
        path.moveTo(0f, canvasHeight)
        for (i in luminance.indices) {
            val x = i * bucketWidth
            val y = canvasHeight * (1f - luminance[i].toFloat() / maxVal)
            path.lineTo(x, y)
        }
        path.lineTo(size.width, canvasHeight)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    HasselbladOrange.copy(alpha = 0.8f),
                    HasselbladOrange.copy(alpha = 0.3f)
                )
            )
        )
    }
}

/**
 * 色温渐变条
 */
@Composable
fun ColorTemperatureGradient(
    modifier: Modifier = Modifier,
    currentKelvin: Int = 5500,
    height: Dp = 8.dp,
    onValueChange: ((Int) -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(4.dp))
    ) {
        val width = size.width
        val canvasHeight = size.height

        // 色温渐变：2000K (橙红) → 10000K (蓝白)
        val colors = listOf(
            Color(0xFFFF6B35),  // 2000K - 橙红
            Color(0xFFFFA500),  // 3000K - 橙
            Color(0xFFFFD700),  // 4000K - 金黄
            Color(0xFFFFFFFF),  // 5500K - 白
            Color(0xFF87CEEB),  // 7000K - 浅蓝
            Color(0xFF4169E1),  // 10000K - 蓝
            Color(0xFF9370DB)   // 20000K - 紫蓝
        )

        drawRect(
            brush = Brush.horizontalGradient(colors),
            size = size
        )

        // 当前色温指示
        val normalizedPos = when {
            currentKelvin <= 2000 -> 0f
            currentKelvin >= 20000 -> 1f
            else -> {
                // 对数映射（人眼对色温感知是对数的）
                val logMin = kotlin.math.log(2000f, 10f)
                val logMax = kotlin.math.log(20000f, 10f)
                val logCurrent = kotlin.math.log(currentKelvin.toFloat(), 10f)
                (logCurrent - logMin) / (logMax - logMin)
            }
        }
        val indicatorX = normalizedPos * width

        drawCircle(
            color = Color.White,
            radius = canvasHeight * 0.8f,
            center = Offset(indicatorX, canvasHeight / 2f)
        )
        drawCircle(
            color = Color.Black,
            radius = canvasHeight * 0.4f,
            center = Offset(indicatorX, canvasHeight / 2f)
        )
    }
}
