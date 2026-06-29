package com.silas.omaster.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * 实时直方图叠加视图
 *
 * 显示图像的四通道直方图（亮度/R/G/B）叠加渲染
 * 支持：
 * - 四通道独立显示或全部叠加
 * - 实时更新（参数变化时重新计算）
 * - 半透明背景确保可见性
 * - 标准摄影直方图样式（左暗右亮）
 */
@Composable
fun HistogramOverlay(
    luminanceHistogram: IntArray,  // 256 bins
    redHistogram: IntArray,        // 256 bins
    greenHistogram: IntArray,      // 256 bins
    blueHistogram: IntArray,       // 256 bins
    modifier: Modifier = Modifier,
    showLuminance: Boolean = true,
    showRed: Boolean = true,
    showGreen: Boolean = true,
    showBlue: Boolean = true,
    showRGBOverlay: Boolean = true  // overlay all channels vs separate
) {
    val histogramHeight = 100.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(histogramHeight)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Semi-transparent dark background
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = androidx.compose.ui.geometry.Offset.Zero,
            size = size
        )

        // Collect enabled channels and find global max for normalization
        val channels = mutableListOf<Pair<IntArray, Color>>()
        if (showLuminance) channels.add(luminanceHistogram to Color.White.copy(alpha = 0.5f))
        if (showRed) channels.add(redHistogram to Color.Red.copy(alpha = 0.5f))
        if (showGreen) channels.add(greenHistogram to Color.Green.copy(alpha = 0.5f))
        if (showBlue) channels.add(blueHistogram to Color.Blue.copy(alpha = 0.5f))

        if (channels.isEmpty()) return@Canvas

        // Find the global peak across all enabled channels for normalization
        // Exclude the extreme bins (0 and 255) from peak calculation to avoid
        // clipping spikes dominating the histogram display
        val globalMax = channels.maxOf { (hist, _) ->
            var max = 0
            for (i in 1 until 255) {
                if (hist[i] > max) max = hist[i]
            }
            max.coerceAtLeast(1)
        }

        val binWidth = canvasWidth / 256f
        val usableHeight = canvasHeight * 0.95f  // leave a tiny top margin

        // 2. Draw each enabled channel as a filled path
        for ((histogram, color) in channels) {
            val path = Path().apply {
                // Start from bottom-left
                moveTo(0f, canvasHeight)

                for (i in 0 until 256) {
                    val x = i * binWidth
                    // Normalize to canvas height, skip extreme bins for peak but still draw them
                    val normalizedValue = (histogram[i].toFloat() / globalMax).coerceIn(0f, 1f)
                    val y = canvasHeight - normalizedValue * usableHeight
                    if (i == 0) {
                        // First point: lineTo from bottom-left
                        lineTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }

                // Close path: line to bottom-right, then back to origin
                lineTo(255 * binWidth, canvasHeight)
                close()
            }

            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }
    }
}
