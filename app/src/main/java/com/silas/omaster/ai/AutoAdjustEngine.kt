package com.silas.omaster.ai

import android.graphics.Bitmap
import com.silas.omaster.renderer.RenderParameters
import kotlin.math.sqrt

/**
 * 直方图驱动自动调整引擎
 *
 * 参考 RapidRAW 自动色阶算法，基于图像直方图统计计算最优渲染参数。
 *
 * 处理管线：
 * 1. 采样分析 → 直方图统计
 * 2. 自动色阶 → 黑白点拉伸
 * 3. 自动白平衡 → 色偏检测与校正
 * 4. 自动曝光 → 亮度目标优化
 * 5. 自动对比度 → 动态范围评估
 * 6. 自动饱和度 → 自然饱和度提升
 * 7. 场景感知 → 暗调/高调/高低对比度自适应
 */
class AutoAdjustEngine {

    /**
     * 直方图统计数据
     *
     * 包含亮度/RGB 四通道直方图、均值、标准差、黑白点、裁剪比例等
     */
    data class HistogramStats(
        val luminance: IntArray,
        val red: IntArray,
        val green: IntArray,
        val blue: IntArray,
        val meanLuminance: Float,
        val stdDevLuminance: Float,
        val meanR: Float,
        val meanG: Float,
        val meanB: Float,
        val shadowClipping: Float,    // 亮度 < 50 的像素占比
        val highlightClipping: Float, // 亮度 > 200 的像素占比
        val blackPoint: Int,          // 第 1 百分位
        val whitePoint: Int,          // 第 99 百分位
        val meanSaturation: Float     // 由 RGB 通道平衡推算的平均饱和度
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HistogramStats) return false
            return meanLuminance == other.meanLuminance &&
                    stdDevLuminance == other.stdDevLuminance &&
                    meanR == other.meanR &&
                    meanG == other.meanG &&
                    meanB == other.meanB &&
                    blackPoint == other.blackPoint &&
                    whitePoint == other.whitePoint
        }

        override fun hashCode(): Int {
            var result = meanLuminance.hashCode()
            result = 31 * result + stdDevLuminance.hashCode()
            result = 31 * result + meanR.hashCode()
            result = 31 * result + meanG.hashCode()
            result = 31 * result + meanB.hashCode()
            result = 31 * result + blackPoint
            result = 31 * result + whitePoint
            return result
        }
    }

    /**
     * 分析 Bitmap 直方图，返回统计数据
     *
     * 采样策略：动态步长，大图降采样以减少计算量
     * - 宽/高 > 2000 → 6 倍下采样
     * - 宽/高 > 1000 → 4 倍下采样
     * - 宽/高 > 500  → 2 倍下采样
     * - 其他          → 逐像素
     */
    fun analyzeHistogram(bitmap: Bitmap): HistogramStats {
        val width = bitmap.width
        val height = bitmap.height

        // 动态采样步长
        val step = when {
            width > 2000 || height > 2000 -> 6
            width > 1000 || height > 1000 -> 4
            width > 500 || height > 500 -> 2
            else -> 1
        }

        val luminance = IntArray(256)
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        var totalLuminance = 0L
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var pixelCount = 0
        var shadowCount = 0
        var highlightCount = 0

        // 用于计算饱和度的累积器
        var saturationSum = 0.0

        // 按行批量读取像素，减少 JNI 开销
        val rowPixels = IntArray(width)
        for (y in 0 until height step step) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (x in 0 until width step step) {
                val pixel = rowPixels[x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Rec.709 亮度
                val luma = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt().coerceIn(0, 255)

                luminance[luma]++
                red[r]++
                green[g]++
                blue[b]++

                totalLuminance += luma
                totalR += r
                totalG += g
                totalB += b
                pixelCount++

                if (luma < 50) shadowCount++
                if (luma > 200) highlightCount++

                // 计算像素级饱和度（HSV 简化）
                val maxVal = maxOf(r, g, b)
                val minVal = minOf(r, g, b)
                if (maxVal > 0) {
                    saturationSum += (maxVal - minVal).toFloat() / maxVal
                }
            }
        }

        if (pixelCount == 0) {
            return HistogramStats(
                luminance = luminance,
                red = red,
                green = green,
                blue = blue,
                meanLuminance = 0f,
                stdDevLuminance = 0f,
                meanR = 0f,
                meanG = 0f,
                meanB = 0f,
                shadowClipping = 0f,
                highlightClipping = 0f,
                blackPoint = 0,
                whitePoint = 255,
                meanSaturation = 0f
            )
        }

        val meanLuminance = (totalLuminance / pixelCount).toFloat()
        val meanR = (totalR / pixelCount).toFloat()
        val meanG = (totalG / pixelCount).toFloat()
        val meanB = (totalB / pixelCount).toFloat()
        val meanSaturation = (saturationSum / pixelCount).toFloat()

        // 计算亮度标准差
        var varianceSum = 0.0
        for (i in luminance.indices) {
            val diff = i - meanLuminance
            varianceSum += diff * diff * luminance[i]
        }
        val stdDevLuminance = sqrt(varianceSum / pixelCount).toFloat()

        // 计算黑白点（第 1 和第 99 百分位）
        val blackPoint = findPercentile(luminance, pixelCount, 0.01)
        val whitePoint = findPercentile(luminance, pixelCount, 0.99)

        val shadowClipping = shadowCount.toFloat() / pixelCount
        val highlightClipping = highlightCount.toFloat() / pixelCount

        return HistogramStats(
            luminance = luminance,
            red = red,
            green = green,
            blue = blue,
            meanLuminance = meanLuminance,
            stdDevLuminance = stdDevLuminance,
            meanR = meanR,
            meanG = meanG,
            meanB = meanB,
            shadowClipping = shadowClipping,
            highlightClipping = highlightClipping,
            blackPoint = blackPoint,
            whitePoint = whitePoint,
            meanSaturation = meanSaturation
        )
    }

    /**
     * 在直方图中查找指定百分分位对应的色阶值
     *
     * @param histogram 直方图数据（256 级）
     * @param totalPixelCount 总采样像素数
     * @param percentile 百分位（0.0~1.0），如 0.01 = 第 1 百分位
     * @return 对应的色阶值（0~255）
     */
    private fun findPercentile(histogram: IntArray, totalPixelCount: Int, percentile: Float): Int {
        val targetCount = (totalPixelCount * percentile).toLong()
        var cumulative = 0L
        for (i in histogram.indices) {
            cumulative += histogram[i]
            if (cumulative >= targetCount) return i
        }
        return 255
    }

    /**
     * 根据直方图统计计算自动调整参数
     *
     * 处理顺序：色阶 → 白平衡 → 曝光 → 对比度 → 饱和度 → 场景感知
     */
    fun computeAutoAdjust(stats: HistogramStats): RenderParameters {
        // ---- 1. 自动色阶：黑白点拉伸 ----
        val blacks = computeAutoBlacks(stats)
        val whites = computeAutoWhites(stats)
        val exposure = computeAutoExposure(stats)
        val contrast = computeAutoContrast(stats)
        val shadows = computeAutoShadows(stats)
        val highlights = computeAutoHighlights(stats)

        // ---- 2. 自动白平衡：色偏检测与校正 ----
        val warmth = computeAutoWarmth(stats)

        // ---- 3. 自动饱和度：自然饱和度提升 ----
        val vibrance = computeAutoVibrance(stats)
        val saturation = computeAutoSaturation(stats)

        // ---- 4. 基础锐度（低对比度图像适当锐化） ----
        val sharpness = computeAutoSharpness(stats)
        val clarity = computeAutoClarity(stats)

        // ---- 5. 场景感知叠加调整 ----
        val sceneAdjustments = computeSceneAwareAdjustments(stats)

        // 合并基础调整与场景感知调整
        return RenderParameters(
            saturation = (saturation + sceneAdjustments.saturationDelta).coerceIn(-100f, 100f),
            contrast = (contrast + sceneAdjustments.contrastDelta).coerceIn(-100f, 100f),
            brightness = 0f,
            warmth = (warmth + sceneAdjustments.warmthDelta).coerceIn(-100f, 100f),
            sharpness = (sharpness + sceneAdjustments.sharpnessDelta).coerceIn(0f, 100f),
            clarity = (clarity + sceneAdjustments.clarityDelta).coerceIn(0f, 100f),
            vibrance = (vibrance + sceneAdjustments.vibranceDelta).coerceIn(-100f, 100f),
            highlights = (highlights + sceneAdjustments.highlightsDelta).coerceIn(-100f, 100f),
            shadows = (shadows + sceneAdjustments.shadowsDelta).coerceIn(-100f, 100f),
            whites = whites.coerceIn(-100f, 100f),
            blacks = blacks.coerceIn(-100f, 100f),
            exposure = (exposure + sceneAdjustments.exposureDelta).coerceIn(-100f, 100f),
            grain = 0f,
            fade = sceneAdjustments.fade.coerceIn(0f, 100f),
            dehaze = sceneAdjustments.dehaze.coerceIn(0f, 100f),
            denoise = sceneAdjustments.denoise.coerceIn(0f, 100f),
            skinSmooth = 0f,
            texture = sceneAdjustments.texture.coerceIn(-100f, 100f)
        )
    }

    /**
     * 一键自动调整：分析 + 计算
     */
    fun autoAdjust(bitmap: Bitmap): RenderParameters {
        val stats = analyzeHistogram(bitmap)
        return computeAutoAdjust(stats)
    }

    // ==================== 自动色阶 ====================

    /**
     * 计算黑色色阶（Blacks）
     *
     * 原理：如果黑点高于 0，说明图像偏亮/灰蒙，需要压暗黑点
     * 黑点越高（偏移越大），blacks 调整越强（负值压暗）
     */
    private fun computeAutoBlacks(stats: HistogramStats): Float {
        // 理想黑点为 0；实际黑点越高，需要越强的负值 blacks 来压暗
        val blackPointOffset = stats.blackPoint.toFloat()
        // 线性映射：黑点 0 → blacks 0；黑点 30 → blacks 约 -25
        val blacks = -(blackPointOffset / 30f * 25f)
        return blacks.coerceIn(-60f, 0f)
    }

    /**
     * 计算白色色阶（Whites）
     *
     * 原理：如果白点低于 255，说明图像缺乏高光，需要提升白点
     * 白点越低（偏移越大），whites 调整越强（正值提亮）
     */
    private fun computeAutoWhites(stats: HistogramStats): Float {
        // 理想白点为 255；实际白点越低，需要越强的正值 whites 来提亮
        val whitePointDeficit = 255f - stats.whitePoint.toFloat()
        // 线性映射：白点缺失 0 → whites 0；白点缺失 50 → whites 约 25
        val whites = (whitePointDeficit / 50f * 25f)
        return whites.coerceIn(0f, 60f)
    }

    /**
     * 计算自动曝光
     *
     * 目标亮度：~118（中灰偏暗，保留更丰富的阴影细节）
     * 曝光补偿量 = (目标 - 实际) 的比例映射
     */
    private fun computeAutoExposure(stats: HistogramStats): Float {
        val targetLuminance = 118f
        val diff = targetLuminance - stats.meanLuminance

        if (kotlin.math.abs(diff) < 8f) return 0f // 接近目标，无需调整

        // 曝光调整：差值映射到 [-50, 50] 范围
        // diff = -50（极暗）→ exposure ≈ 40
        // diff = +50（极亮）→ exposure ≈ -40
        val exposure = (diff / 50f * 40f)
        return exposure.coerceIn(-50f, 50f)
    }

    /**
     * 计算自动对比度
     *
     * 原理：标准差低 → 图像缺乏对比 → 增加对比度
     *        标准差高 → 图像已有对比 → 略微降低避免过硬
     * 参考标准差 ~64 为正常对比度（8bit 图像理论中值）
     */
    private fun computeAutoContrast(stats: HistogramStats): Float {
        val referenceStdDev = 64f
        val stdDevRatio = stats.stdDevLuminance / referenceStdDev

        return when {
            stdDevRatio < 0.4f -> {
                // 极低对比度：大幅增强
                35f + (0.4f - stdDevRatio) / 0.4f * 30f
            }
            stdDevRatio < 0.7f -> {
                // 低对比度：中度增强
                15f + (0.7f - stdDevRatio) / 0.3f * 20f
            }
            stdDevRatio < 1.3f -> {
                // 正常对比度：轻微调整
                (1.0f - stdDevRatio) / 0.3f * 8f
            }
            stdDevRatio < 1.8f -> {
                // 较高对比度：轻微降低
                -((stdDevRatio - 1.3f) / 0.5f * 10f)
            }
            else -> {
                // 高对比度：适度降低
                -(10f + (stdDevRatio - 1.8f).coerceAtMost(2f) / 2f * 15f)
            }
        }.coerceIn(-25f, 65f)
    }

    /**
     * 计算自动阴影调整
     *
     * 阴影裁剪比例高 → 提亮阴影恢复细节
     */
    private fun computeAutoShadows(stats: HistogramStats): Float {
        val shadowClip = stats.shadowClipping
        return when {
            shadowClip > 0.5f -> 45f + (shadowClip - 0.5f) / 0.5f * 20f
            shadowClip > 0.25f -> 20f + (shadowClip - 0.25f) / 0.25f * 25f
            shadowClip > 0.1f -> 5f + (shadowClip - 0.1f) / 0.15f * 15f
            shadowClip > 0.03f -> (shadowClip - 0.03f) / 0.07f * 5f
            else -> 0f
        }.coerceIn(0f, 65f)
    }

    /**
     * 计算自动高光调整
     *
     * 高光裁剪比例高 → 压低高光保护细节
     */
    private fun computeAutoHighlights(stats: HistogramStats): Float {
        val highlightClip = stats.highlightClipping
        return when {
            highlightClip > 0.4f -> -(35f + (highlightClip - 0.4f) / 0.6f * 25f)
            highlightClip > 0.2f -> -(15f + (highlightClip - 0.2f) / 0.2f * 20f)
            highlightClip > 0.08f -> -((highlightClip - 0.08f) / 0.12f * 15f)
            highlightClip > 0.02f -> -((highlightClip - 0.02f) / 0.06f * 5f)
            else -> 0f
        }.coerceIn(-60f, 0f)
    }

    // ==================== 自动白平衡 ====================

    /**
     * 计算自动色温（Warmth）
     *
     * 原理：通过比较 R/B 通道均值检测色偏
     * - R > B 较多 → 暖色偏 → 负值 warmth（偏冷校正）
     * - B > R 较多 → 冷色偏 → 正值 warmth（偏暖校正）
     * 同时检测绿色通道偏移（品红/绿偏移）
     */
    private fun computeAutoWarmth(stats: HistogramStats): Float {
        val rDiff = stats.meanR - stats.meanG
        val bDiff = stats.meanB - stats.meanG

        // R/B 相对于 G 的偏差
        val warmBias = rDiff - bDiff // 正值 = 暖偏，负值 = 冷偏

        // 映射：warmBias = ±30 → warmth = ∓15
        val warmth = -(warmBias / 30f * 15f)

        return warmth.coerceIn(-30f, 30f)
    }

    // ==================== 自动饱和度 ====================

    /**
     * 计算自动鲜艳度（Vibrance）
     *
     * 原理：平均饱和度低 → 大幅提升鲜艳度（仅增强低饱和度区域，更自然）
     *       平均饱和度高 → 不调整或轻微降低
     */
    private fun computeAutoVibrance(stats: HistogramStats): Float {
        // meanSaturation 范围约 0~1，正常照片约 0.3~0.5
        val sat = stats.meanSaturation
        return when {
            sat < 0.1f -> 50f + (0.1f - sat) / 0.1f * 30f      // 极低饱和度
            sat < 0.2f -> 30f + (0.2f - sat) / 0.1f * 20f       // 低饱和度
            sat < 0.35f -> 10f + (0.35f - sat) / 0.15f * 20f    // 偏低饱和度
            sat < 0.5f -> (0.5f - sat) / 0.15f * 10f            // 正常偏低
            sat < 0.65f -> 0f                                     // 正常
            sat < 0.8f -> -((sat - 0.65f) / 0.15f * 10f)        // 偏高
            else -> -(10f + (sat - 0.8f).coerceAtMost(0.2f) / 0.2f * 15f) // 过高
        }.coerceIn(-25f, 80f)
    }

    /**
     * 计算自动饱和度（Saturation）
     *
     * 原理：仅作为 vibrance 的辅助，低饱和度图像同时轻度提升整体饱和度
     *       高饱和度图像不额外增加饱和度
     */
    private fun computeAutoSaturation(stats: HistogramStats): Float {
        val sat = stats.meanSaturation
        return when {
            sat < 0.15f -> 15f + (0.15f - sat) / 0.15f * 15f
            sat < 0.3f -> 5f + (0.3f - sat) / 0.15f * 10f
            sat < 0.5f -> (0.5f - sat) / 0.2f * 5f
            else -> 0f
        }.coerceIn(0f, 30f)
    }

    // ==================== 自动锐度与清晰度 ====================

    /**
     * 计算自动锐度
     *
     * 低对比度图像需要更多锐化来恢复边缘感知
     */
    private fun computeAutoSharpness(stats: HistogramStats): Float {
        val stdDevRatio = stats.stdDevLuminance / 64f
        return when {
            stdDevRatio < 0.5f -> 20f + (0.5f - stdDevRatio) / 0.5f * 20f
            stdDevRatio < 0.8f -> 10f + (0.8f - stdDevRatio) / 0.3f * 10f
            stdDevRatio < 1.2f -> 5f
            else -> 0f
        }.coerceIn(0f, 40f)
    }

    /**
     * 计算自动清晰度
     *
     * 低对比度和高对比度图像都可受益于适度清晰度增强
     */
    private fun computeAutoClarity(stats: HistogramStats): Float {
        val stdDevRatio = stats.stdDevLuminance / 64f
        return when {
            stdDevRatio < 0.5f -> 25f + (0.5f - stdDevRatio) / 0.5f * 20f
            stdDevRatio < 0.8f -> 15f + (0.8f - stdDevRatio) / 0.3f * 10f
            stdDevRatio < 1.5f -> 10f
            else -> 5f
        }.coerceIn(0f, 45f)
    }

    // ==================== 场景感知 ====================

    /**
     * 场景感知增量调整
     *
     * 检测高调/低调/高低对比度/冷暖场景，叠加额外修正
     */
    private data class SceneAdjustments(
        val contrastDelta: Float = 0f,
        val exposureDelta: Float = 0f,
        val warmthDelta: Float = 0f,
        val shadowsDelta: Float = 0f,
        val highlightsDelta: Float = 0f,
        val vibranceDelta: Float = 0f,
        val saturationDelta: Float = 0f,
        val sharpnessDelta: Float = 0f,
        val clarityDelta: Float = 0f,
        val texture: Float = 0f,
        val fade: Float = 0f,
        val dehaze: Float = 0f,
        val denoise: Float = 0f
    )

    /**
     * 检测场景特征并计算叠加调整量
     *
     * 场景检测规则：
     * - 高调（High-key）：平均亮度 > 180 → 保护高光，轻微提亮阴影
     * - 低调（Low-key）：平均亮度 < 70 → 提亮阴影，压低高光，添加降噪
     * - 高对比度：标准差/均值比 > 0.7 → 降低对比度，增加清晰度
     * - 低对比度：标准差/均值比 < 0.3 → 增加对比度，增加清晰度
     * - 暖场景：R 均值显著 > B 均值 → 轻微冷调校正
     * - 冷场景：B 均值显著 > R 均值 → 轻微暖调校正
     */
    private fun computeSceneAwareAdjustments(stats: HistogramStats): SceneAdjustments {
        var adj = SceneAdjustments()

        // 高调场景检测
        if (stats.meanLuminance > 180f) {
            adj = adj.copy(
                highlightsDelta = adj.highlightsDelta - 15f, // 保护高光
                shadowsDelta = adj.shadowsDelta + 10f,       // 轻微提亮阴影
                contrastDelta = adj.contrastDelta - 8f,      // 降低对比度避免过硬
                fade = adj.fade + 5f                         // 轻微褪色增加柔和感
            )
        }

        // 低调场景检测
        if (stats.meanLuminance < 70f) {
            adj = adj.copy(
                shadowsDelta = adj.shadowsDelta + 15f,       // 提亮阴影
                highlightsDelta = adj.highlightsDelta - 5f,  // 轻微压低高光
                denoise = adj.denoise + 20f,                 // 暗部降噪
                dehaze = adj.dehaze + 8f,                    // 轻微去霾提升通透感
                vibranceDelta = adj.vibranceDelta + 8f,      // 暗部色彩偏灰，增强鲜艳度
                clarityDelta = adj.clarityDelta + 5f         // 增加清晰度
            )
        }

        // 对比度场景检测（使用变异系数 = 标准差/均值）
        val coefficientOfVariation = if (stats.meanLuminance > 1f) {
            stats.stdDevLuminance / stats.meanLuminance
        } else {
            0f
        }

        if (coefficientOfVariation > 0.7f) {
            // 高对比度场景
            adj = adj.copy(
                contrastDelta = adj.contrastDelta - 8f,      // 降低对比度
                clarityDelta = adj.clarityDelta + 8f,        // 增加清晰度（替代对比度提供中频细节）
                highlightsDelta = adj.highlightsDelta - 5f,  // 保护高光
                shadowsDelta = adj.shadowsDelta + 5f         // 提亮阴影
            )
        } else if (coefficientOfVariation < 0.3f) {
            // 低对比度场景
            adj = adj.copy(
                contrastDelta = adj.contrastDelta + 10f,     // 增加对比度
                clarityDelta = adj.clarityDelta + 10f,       // 增加清晰度
                texture = adj.texture + 8f,                  // 增加纹理感
                dehaze = adj.dehaze + 5f                     // 去霾增加通透感
            )
        }

        // 暖场景检测
        val warmthGap = stats.meanR - stats.meanB
        if (warmthGap > 25f) {
            adj = adj.copy(
                warmthDelta = adj.warmthDelta - 5f           // 轻微冷调校正
            )
        }

        // 冷场景检测
        if (warmthGap < -25f) {
            adj = adj.copy(
                warmthDelta = adj.warmthDelta + 5f           // 轻微暖调校正
            )
        }

        // 绿/品红偏移检测
        val greenBias = stats.meanG - (stats.meanR + stats.meanB) / 2f
        if (greenBias > 15f) {
            // 绿色偏移 → 轻微暖调校正（暖调含品红，可抵消绿偏）
            adj = adj.copy(
                warmthDelta = adj.warmthDelta + 3f
            )
        } else if (greenBias < -15f) {
            // 品红偏移 → 轻微冷调校正
            adj = adj.copy(
                warmthDelta = adj.warmthDelta - 3f
            )
        }

        // 极端暗部裁剪检测（ > 60% 的像素在阴影中）
        if (stats.shadowClipping > 0.6f) {
            adj = adj.copy(
                shadowsDelta = adj.shadowsDelta + 20f,       // 大幅提亮阴影
                denoise = adj.denoise + 15f,                 // 阴影降噪
                exposureDelta = adj.exposureDelta + 10f       // 额外曝光补偿
            )
        }

        // 极端高光裁剪检测（ > 30% 的像素在高光中）
        if (stats.highlightClipping > 0.3f) {
            adj = adj.copy(
                highlightsDelta = adj.highlightsDelta - 20f,  // 大幅压低高光
                exposureDelta = adj.exposureDelta - 8f        // 轻微降低曝光
            )
        }

        // 色阶拉伸后的纹理补偿
        val tonalRange = stats.whitePoint - stats.blackPoint
        if (tonalRange < 128) {
            // 色阶范围窄（灰蒙图像），需要更强的纹理与清晰度补偿
            adj = adj.copy(
                texture = adj.texture + 5f,
                clarityDelta = adj.clarityDelta + 5f,
                dehaze = adj.dehaze + 5f
            )
        }

        return adj
    }
}
