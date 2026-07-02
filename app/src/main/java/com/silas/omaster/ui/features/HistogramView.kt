package com.silas.omaster.ui.features

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.HasselbladOrange

/**
 * 直方图绘制组件
 *
 * 对齐 PixelFruit HistogramManager.js 的绘制逻辑
 * 支持亮度模式和 RGB 叠加模式
 */
@Composable
fun HistogramView(
    histogram: HistogramAnalyzer.HistogramResult?,
    modifier: Modifier = Modifier,
    mode: HistogramMode = HistogramMode.RGB
) {
    if (histogram == null) return

    val maxCount = histogram.luminance.maxOrNull()?.coerceAtLeast(1) ?: 1

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            when (mode) {
                HistogramMode.LUMINANCE -> drawLuminance(histogram.luminance, maxCount)
                HistogramMode.RGB -> drawRGB(histogram, maxCount)
            }
        }
    }
}

private fun DrawScope.drawLuminance(luminance: IntArray, maxCount: Int) {
    val barWidth = size.width / 256f
    luminance.forEachIndexed { i, count ->
        val height = (count.toFloat() / maxCount) * size.height
        drawRect(
            color = HasselbladOrange.copy(alpha = 0.7f),
            topLeft = Offset(i * barWidth, size.height - height),
            size = Size(barWidth.coerceAtLeast(1f), height)
        )
    }
}

private fun DrawScope.drawRGB(histogram: HistogramAnalyzer.HistogramResult, maxCount: Int) {
    val barWidth = size.width / 256f
    val rMax = histogram.red.maxOrNull()?.coerceAtLeast(1) ?: 1
    val gMax = histogram.green.maxOrNull()?.coerceAtLeast(1) ?: 1
    val bMax = histogram.blue.maxOrNull()?.coerceAtLeast(1) ?: 1
    val globalMax = maxOf(rMax, gMax, bMax, maxCount)

    for (i in 0 until 256) {
        val rh = (histogram.red[i].toFloat() / globalMax) * size.height
        val gh = (histogram.green[i].toFloat() / globalMax) * size.height
        val bh = (histogram.blue[i].toFloat() / globalMax) * size.height

        // 叠加绘制：红 → 绿 → 蓝（半透明）
        val x = i * barWidth
        val w = barWidth.coerceAtLeast(1f)

        drawRect(
            color = Color(0xFFFF4444).copy(alpha = 0.5f),
            topLeft = Offset(x, size.height - rh),
            size = Size(w, rh)
        )
        drawRect(
            color = Color(0xFF44FF44).copy(alpha = 0.4f),
            topLeft = Offset(x, size.height - gh),
            size = Size(w, gh)
        )
        drawRect(
            color = Color(0xFF4444FF).copy(alpha = 0.3f),
            topLeft = Offset(x, size.height - bh),
            size = Size(w, bh)
        )
    }
}

enum class HistogramMode {
    LUMINANCE,
    RGB
}
