package com.silas.omaster.ai

import android.graphics.Bitmap
import android.graphics.Color
import com.silas.omaster.renderer.RenderParameters
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 白平衡引擎
 *
 * 功能：
 * 1. 吸管取色白平衡：用户点击"应该为白色/灰色"的区域，自动计算色偏并校正
 * 2. 色温/色调滑块：手动微调色温和色调偏移
 * 3. 自动白平衡：基于灰度世界假设自动校正
 */
class WhiteBalanceEngine {

    data class WhiteBalanceResult(
        val temperatureShift: Float,  // -100..100
        val tintShift: Float,         // -100..100
        val warmth: Float,            // mapped to RenderParameters.warmth
        val greenMagentaShift: Float  // mapped to HSL green adjustments
    )

    companion object {
        private const val TAG = "WhiteBalanceEngine"

        /** 吸管采样半径（5x5 = 半径2） */
        private const val EYEDROPPER_RADIUS = 2

        /** 色温缩放因子：将 RGB 比值差映射到 [-100, 100] 的 warmth */
        private const val TEMP_SCALE_FACTOR = 150f

        /** 色调缩放因子：将 G 偏差映射到 [-100, 100] 的 tint */
        private const val TINT_SCALE_FACTOR = 200f
    }

    /**
     * 吸管白平衡：点击一个"应该为中性灰"的像素区域
     * 采样 5x5 邻域均值，计算 RGB → 灰色的偏移量
     */
    fun eyedropperWhiteBalance(bitmap: Bitmap, x: Int, y: Int): WhiteBalanceResult {
        // 采样 5x5 邻域
        var sumR = 0f
        var sumG = 0f
        var sumB = 0f
        var count = 0

        val xStart = (x - EYEDROPPER_RADIUS).coerceAtLeast(0)
        val xEnd = (x + EYEDROPPER_RADIUS).coerceAtMost(bitmap.width - 1)
        val yStart = (y - EYEDROPPER_RADIUS).coerceAtLeast(0)
        val yEnd = (y + EYEDROPPER_RADIUS).coerceAtMost(bitmap.height - 1)

        for (py in yStart..yEnd) {
            for (px in xStart..xEnd) {
                val pixel = bitmap.getPixel(px, py)
                sumR += Color.red(pixel)
                sumG += Color.green(pixel)
                sumB += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) {
            return WhiteBalanceResult(0f, 0f, 0f, 0f)
        }

        val avgR = sumR / count
        val avgG = sumG / count
        val avgB = sumB / count

        return computeWhiteBalanceFromRGB(avgR, avgG, avgB)
    }

    /**
     * 自动白平衡：灰度世界假设
     * 计算全图平均 R/G/B，以绿色通道为基准校正红蓝
     */
    fun autoWhiteBalance(bitmap: Bitmap): WhiteBalanceResult {
        val width = bitmap.width
        val height = bitmap.height

        // 使用步长采样，避免全图遍历耗时过长
        val stepX = (width / 200).coerceAtLeast(1)
        val stepY = (height / 200).coerceAtLeast(1)

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        for (py in 0 until height step stepY) {
            for (px in 0 until width step stepX) {
                val pixel = bitmap.getPixel(px, py)
                sumR += Color.red(pixel)
                sumG += Color.green(pixel)
                sumB += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) {
            return WhiteBalanceResult(0f, 0f, 0f, 0f)
        }

        val avgR = (sumR / count).toFloat()
        val avgG = (sumG / count).toFloat()
        val avgB = (sumB / count).toFloat()

        return computeWhiteBalanceFromRGB(avgR, avgG, avgB)
    }

    /**
     * 从 RGB 均值计算白平衡偏移
     *
     * 算法：
     * - 以绿色通道为参考基准
     * - R > G 表示暖色偏（需降低 R），R < G 表示冷色偏（需增加 R/warmth）
     * - B > G 表示冷色偏，B < G 表示暖色偏
     * - G 偏离 (R+B)/2 表示绿/品红偏色
     */
    private fun computeWhiteBalanceFromRGB(avgR: Float, avgG: Float, avgB: Float): WhiteBalanceResult {
        // 避免零值
        val safeR = avgR.coerceAtLeast(1f)
        val safeG = avgG.coerceAtLeast(1f)
        val safeB = avgB.coerceAtLeast(1f)

        // 以绿色通道为基准，计算红蓝通道的相对偏差
        // 色温偏移：R/B 比值差异
        // R 相对 G 偏高 → 暖色偏 → 需要负 warmth（降温）
        // B 相对 G 偏高 → 冷色偏 → 需要正 warmth（升温）
        val redRatio = safeR / safeG
        val blueRatio = safeB / safeG

        // 色温偏移：综合红蓝偏差
        // redRatio > 1 且 blueRatio < 1 → 暖色偏 → warmth 为负
        // redRatio < 1 且 blueRatio > 1 → 冷色偏 → warmth 为正
        val tempShift = ((blueRatio - redRatio) * TEMP_SCALE_FACTOR / 2f)
            .coerceIn(-100f, 100f)

        // 色调偏移：G 偏离 (R+B)/2 的程度
        // G > (R+B)/2 → 绿色偏 → 需要品红补偿（负 tintValue → 降绿）
        // G < (R+B)/2 → 品红偏 → 需要绿色补偿（正 tintValue → 增绿）
        val midRB = (safeR + safeB) / 2f
        val greenDeviation = (safeG - midRB) / safeG.coerceAtLeast(1f)
        val tintShift = (-greenDeviation * TINT_SCALE_FACTOR)
            .coerceIn(-100f, 100f)

        // warmth 直接映射到 RenderParameters.warmth
        val warmth = tempShift

        // greenMagentaShift 映射到 HSL 绿色通道调整
        // 正值 → 增加绿色饱和度/明度（补偿品红偏）
        // 负值 → 减少绿色饱和度/明度（补偿绿色偏）
        val greenMagentaShift = tintShift

        return WhiteBalanceResult(
            temperatureShift = tempShift,
            tintShift = tintShift,
            warmth = warmth,
            greenMagentaShift = greenMagentaShift
        )
    }

    /**
     * 将白平衡结果应用到 RenderParameters
     */
    fun applyToRenderParameters(params: RenderParameters, wb: WhiteBalanceResult): RenderParameters {
        // warmth 直接映射
        val newWarmth = (params.warmth + wb.warmth).coerceIn(-100f, 100f)

        // green-magenta shift 通过 HSL 绿色通道饱和度和明度调整
        // 正 tintValue → 增加绿色饱和度（补偿品红偏）
        // 负 tintValue → 降低绿色饱和度（补偿绿色偏）
        val greenSatAdjustment = (wb.greenMagentaShift * 0.5f).coerceIn(-100f, 100f)
        val greenLumAdjustment = (wb.greenMagentaShift * 0.3f).coerceIn(-100f, 100f)

        return params.copy(
            warmth = newWarmth,
            hslGreenSaturation = (params.hslGreenSaturation + greenSatAdjustment).coerceIn(-100f, 100f),
            hslGreenLuminance = (params.hslGreenLuminance + greenLumAdjustment).coerceIn(-100f, 100f)
        )
    }
}
