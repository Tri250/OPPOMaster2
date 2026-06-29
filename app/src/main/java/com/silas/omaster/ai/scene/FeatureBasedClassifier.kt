package com.silas.omaster.ai.scene

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 基于特征的轻量级场景分类器
 *
 * 不依赖 TFLite 模型，使用提取的图像特征进行场景分类。
 * 作为模型不可用时的替代方案。
 *
 * 分类逻辑：
 * - 提取多维特征（颜色分布、亮度、边缘密度、肤色比例、天空区域等）
 * - 使用加权评分系统对每个候选场景打分
 * - 选择最高分场景作为结果
 */
class FeatureBasedClassifier {

    data class ImageFeatures(
        val avgBrightness: Float,
        val brightnessStdDev: Float,
        val avgSaturation: Float,
        val edgeDensity: Float,
        val skinToneRatio: Float,
        val skyRatio: Float,
        val warmRatio: Float,
        val greenRatio: Float,
        val darkPixelRatio: Float,
        val highlightRatio: Float,
        val topBrightness: Float,
        val bottomBrightness: Float,
        val blueHueRatio: Float,
        val lowSaturationRatio: Float
    )

    data class ScenePrediction(
        val label: String,
        val confidence: Float
    )

    /**
     * 从Bitmap提取特征
     * 使用步进采样提高大图效率
     */
    fun extractFeatures(bitmap: Bitmap): ImageFeatures {
        val width = bitmap.width
        val height = bitmap.height

        val step = when {
            width > 1000 || height > 1000 -> 8
            width > 500 || height > 500 -> 4
            else -> 2
        }

        val topThirdEnd = height / 3
        val bottomThirdStart = (height * 2) / 3

        var totalBrightness = 0f
        var totalSaturation = 0f
        var warmPixels = 0
        var greenDominantPixels = 0
        var skinPixels = 0
        var darkPixels = 0
        var highlightPixels = 0
        var skyPixels = 0
        var blueHuePixels = 0
        var lowSaturationPixels = 0
        var topBrightness = 0f
        var topPixelCount = 0
        var bottomBrightness = 0f
        var bottomPixelCount = 0
        var totalPixelCount = 0

        val brightnessValues = mutableListOf<Float>()

        val rowPixels = IntArray(width)
        for (y in 0 until height step step) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (x in 0 until width step step) {
                val pixel = rowPixels[x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val brightness = 0.2126f * r + 0.7152f * g + 0.0722f * b
                totalBrightness += brightness

                val maxVal = maxOf(r, g, b)
                val minVal = minOf(r, g, b)
                val saturation = if (maxVal > 0) (maxVal - minVal).toFloat() / maxVal else 0f
                totalSaturation += saturation

                brightnessValues.add(brightness)

                // 暖色判定：R > B + 20 且 R > G
                if (r > b + 20 && r > g) warmPixels++

                // 绿色主导：G > R + 15 且 G > B + 15
                if (g > r + 15 && g > b + 15) greenDominantPixels++

                // 肤色检测（YCbCr）
                val yCbCr_y = 16 + 0.257 * r + 0.504 * g + 0.098 * b
                val cb = 128 - 0.148 * r - 0.291 * g + 0.439 * b
                val cr = 128 + 0.439 * r - 0.368 * g - 0.071 * b
                if (yCbCr_y in 80.0..230.0 && cb in 85.0..125.0 && cr in 140.0..165.0 && saturation > 0.15f) {
                    skinPixels++
                }

                // 暗部
                if (brightness < 50) darkPixels++

                // 高光
                if (brightness > 200) highlightPixels++

                // 天空区域（上部1/3）明亮蓝色像素
                if (y < topThirdEnd) {
                    topBrightness += brightness
                    topPixelCount++
                    if (brightness > 120 && b > r + 30 && b > g + 10 && saturation > 0.15f) {
                        skyPixels++
                    }
                }

                // 底部1/3
                if (y >= bottomThirdStart) {
                    bottomBrightness += brightness
                    bottomPixelCount++
                }

                // 蓝色色调
                if (b > r + 25 && b > g + 15 && saturation > 0.2f) blueHuePixels++

                // 低饱和度
                if (saturation < 0.15f) lowSaturationPixels++

                totalPixelCount++
            }
        }

        if (totalPixelCount == 0) {
            return ImageFeatures(
                avgBrightness = 128f, brightnessStdDev = 0f, avgSaturation = 0.5f,
                edgeDensity = 0f, skinToneRatio = 0f, skyRatio = 0f, warmRatio = 0f,
                greenRatio = 0f, darkPixelRatio = 0f, highlightRatio = 0f,
                topBrightness = 128f, bottomBrightness = 128f, blueHueRatio = 0f,
                lowSaturationRatio = 0f
            )
        }

        val avgBrightness = totalBrightness / totalPixelCount
        val avgSaturation = totalSaturation / totalPixelCount
        val mean = brightnessValues.average().toFloat()
        val stdDev = sqrt(brightnessValues.map { (it - mean) * (it - mean) }.average()).toFloat()

        // 计算边缘密度
        val edgeDensity = computeEdgeDensity(bitmap)

        return ImageFeatures(
            avgBrightness = avgBrightness,
            brightnessStdDev = stdDev,
            avgSaturation = avgSaturation,
            edgeDensity = edgeDensity,
            skinToneRatio = skinPixels.toFloat() / totalPixelCount,
            skyRatio = if (topPixelCount > 0) skyPixels.toFloat() / topPixelCount else 0f,
            warmRatio = warmPixels.toFloat() / totalPixelCount,
            greenRatio = greenDominantPixels.toFloat() / totalPixelCount,
            darkPixelRatio = darkPixels.toFloat() / totalPixelCount,
            highlightRatio = highlightPixels.toFloat() / totalPixelCount,
            topBrightness = if (topPixelCount > 0) topBrightness / topPixelCount else avgBrightness,
            bottomBrightness = if (bottomPixelCount > 0) bottomBrightness / bottomPixelCount else avgBrightness,
            blueHueRatio = blueHuePixels.toFloat() / totalPixelCount,
            lowSaturationRatio = lowSaturationPixels.toFloat() / totalPixelCount
        )
    }

    /**
     * 计算边缘密度（简化 Sobel）
     */
    private fun computeEdgeDensity(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        // 缩放到小尺寸进行快速计算
        val targetSize = 80
        val smallBitmap = if (width > targetSize || height > targetSize) {
            val scale = minOf(targetSize.toFloat() / width, targetSize.toFloat() / height)
            val sw = (width * scale).toInt().coerceAtLeast(3)
            val sh = (height * scale).toInt().coerceAtLeast(3)
            Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        } else {
            bitmap
        }

        val w = smallBitmap.width
        val h = smallBitmap.height
        if (w < 3 || h < 3) return 0f

        val pixels = IntArray(w * h)
        smallBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val luminance = FloatArray(w * h)
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            luminance[i] = 0.2126f * r + 0.7152f * g + 0.0722f * b
        }

        var edgeCount = 0
        var totalPixels = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val gx = -luminance[idx - w - 1] + luminance[idx - w + 1] -
                        2f * luminance[idx - 1] + 2f * luminance[idx + 1] -
                        luminance[idx + w - 1] + luminance[idx + w + 1]
                val gy = -luminance[idx - w - 1] - 2f * luminance[idx - w] - luminance[idx - w + 1] +
                        luminance[idx + w - 1] + 2f * luminance[idx + w] + luminance[idx + w + 1]
                val gradient = sqrt(gx * gx + gy * gy)
                if (gradient > 40f) edgeCount++
                totalPixels++
            }
        }

        if (smallBitmap !== bitmap) smallBitmap.recycle()

        return if (totalPixels > 0) edgeCount.toFloat() / totalPixels else 0f
    }

    /**
     * 基于特征进行场景分类
     */
    fun classify(features: ImageFeatures): List<ScenePrediction> {
        val scores = mutableMapOf<String, Float>()

        scores["portrait"] = scorePortrait(features)
        scores["landscape"] = scoreLandscape(features)
        scores["food"] = scoreFood(features)
        scores["night"] = scoreNight(features)
        scores["architecture"] = scoreArchitecture(features)
        scores["indoor"] = scoreIndoor(features)
        scores["macro"] = scoreMacro(features)
        scores["beach"] = scoreBeach(features)
        scores["snow"] = scoreSnow(features)
        scores["sunset"] = scoreSunset(features)
        scores["flower"] = scoreFlower(features)
        scores["street"] = scoreStreet(features)

        return scores.map { (label, score) ->
            ScenePrediction(label, score.coerceIn(0f, 1f))
        }.sortedByDescending { it.confidence }
    }

    // ==================== Sigmoid 辅助 ====================

    /**
     * Sigmoid 函数：x 越大于 threshold，结果越接近 1
     * k 控制斜率陡峭程度
     */
    private fun sigmoid(x: Float, threshold: Float, k: Float = 10f): Float {
        return (1f / (1f + exp(-(x - threshold) * k))).coerceIn(0f, 1f)
    }

    /**
     * 反向 Sigmoid：x 越小于 threshold，结果越接近 1
     */
    private fun invSigmoid(x: Float, threshold: Float, k: Float = 10f): Float {
        return (1f / (1f + exp((x - threshold) * k))).coerceIn(0f, 1f)
    }

    /**
     * 区间评分：x 在 [low, high] 范围内得高分
     */
    private fun rangeScore(x: Float, low: Float, high: Float, k: Float = 8f): Float {
        val above = sigmoid(x, low, k)
        val below = invSigmoid(x, high, k)
        return above * below
    }

    // ==================== 各场景评分方法 ====================

    /**
     * 人像：高肤色比例，中等亮度，低边缘密度
     */
    private fun scorePortrait(f: ImageFeatures): Float {
        var score = 0f
        // 肤色比例是最强信号
        score += sigmoid(f.skinToneRatio, 0.05f, 20f) * 0.35f
        score += sigmoid(f.skinToneRatio, 0.10f, 15f) * 0.15f
        // 中等亮度（80-180）
        score += rangeScore(f.avgBrightness, 80f, 180f, 0.03f) * 0.15f
        // 低边缘密度（柔和）
        score += invSigmoid(f.edgeDensity, 0.25f, 8f) * 0.10f
        // 非高天空比例
        score += invSigmoid(f.skyRatio, 0.4f, 8f) * 0.10f
        // 中等饱和度
        score += rangeScore(f.avgSaturation, 0.15f, 0.5f, 5f) * 0.10f
        // 低暗部比例
        score += invSigmoid(f.darkPixelRatio, 0.3f, 6f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 风景：高天空比例，绿色主导，上亮下暗
     */
    private fun scoreLandscape(f: ImageFeatures): Float {
        var score = 0f
        // 天空区域（上部亮，蓝色）
        score += sigmoid(f.skyRatio, 0.25f, 8f) * 0.25f
        // 绿色主导
        score += sigmoid(f.greenRatio, 0.08f, 10f) * 0.20f
        // 上亮下暗的梯度
        val gradient = f.topBrightness - f.bottomBrightness
        score += sigmoid(gradient, 10f, 0.03f) * 0.15f
        // 中等边缘密度
        score += rangeScore(f.edgeDensity, 0.05f, 0.30f, 6f) * 0.10f
        // 低肤色比例
        score += invSigmoid(f.skinToneRatio, 0.05f, 15f) * 0.10f
        // 较高亮度
        score += sigmoid(f.avgBrightness, 100f, 0.02f) * 0.10f
        // 中等饱和度
        score += rangeScore(f.avgSaturation, 0.2f, 0.6f, 4f) * 0.10f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 美食：暖色调，高饱和，中等亮度
     */
    private fun scoreFood(f: ImageFeatures): Float {
        var score = 0f
        // 暖色调
        score += sigmoid(f.warmRatio, 0.25f, 8f) * 0.30f
        // 高饱和度
        score += sigmoid(f.avgSaturation, 0.3f, 5f) * 0.25f
        // 中等亮度（不太暗不太亮）
        score += rangeScore(f.avgBrightness, 100f, 200f, 0.02f) * 0.20f
        // 低天空比例（室内拍摄居多）
        score += invSigmoid(f.skyRatio, 0.2f, 8f) * 0.10f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.05f, 12f) * 0.10f
        // 中低边缘密度（食物纹理不太锐利）
        score += rangeScore(f.edgeDensity, 0.05f, 0.25f, 6f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 夜景：低亮度，低饱和度，高暗部比例
     */
    private fun scoreNight(f: ImageFeatures): Float {
        var score = 0f
        // 低亮度
        score += invSigmoid(f.avgBrightness, 80f, 0.04f) * 0.30f
        // 高暗部比例
        score += sigmoid(f.darkPixelRatio, 0.5f, 5f) * 0.25f
        // 低饱和度
        score += invSigmoid(f.avgSaturation, 0.3f, 5f) * 0.15f
        // 高亮度标准差（灯光点缀）
        score += sigmoid(f.brightnessStdDev, 50f, 0.02f) * 0.15f
        // 低天空比例（与白天蓝天不同）
        score += invSigmoid(f.skyRatio, 0.3f, 6f) * 0.10f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.04f, 10f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 建筑：高边缘密度，低天空比例（特写），低肤色
     */
    private fun scoreArchitecture(f: ImageFeatures): Float {
        var score = 0f
        // 高边缘密度
        score += sigmoid(f.edgeDensity, 0.20f, 10f) * 0.30f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 80f, 200f, 0.02f) * 0.15f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.04f, 12f) * 0.15f
        // 中等饱和度（不太鲜艳）
        score += rangeScore(f.avgSaturation, 0.1f, 0.4f, 5f) * 0.15f
        // 低绿色
        score += invSigmoid(f.greenRatio, 0.10f, 10f) * 0.10f
        // 有一定高光（玻璃反射等）
        score += sigmoid(f.highlightRatio, 0.05f, 10f) * 0.10f
        // 较高对比（亮度标准差大）
        score += sigmoid(f.brightnessStdDev, 45f, 0.02f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 室内：低天空比例，中等亮度，低边缘密度
     */
    private fun scoreIndoor(f: ImageFeatures): Float {
        var score = 0f
        // 低天空比例
        score += invSigmoid(f.skyRatio, 0.15f, 10f) * 0.25f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 70f, 180f, 0.02f) * 0.20f
        // 低边缘密度
        score += invSigmoid(f.edgeDensity, 0.25f, 8f) * 0.15f
        // 低绿色
        score += invSigmoid(f.greenRatio, 0.10f, 10f) * 0.10f
        // 低蓝色色调
        score += invSigmoid(f.blueHueRatio, 0.10f, 10f) * 0.10f
        // 中等偏低饱和度
        score += invSigmoid(f.avgSaturation, 0.45f, 5f) * 0.10f
        // 暖色偏多（室内灯光）
        score += sigmoid(f.warmRatio, 0.20f, 6f) * 0.10f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 微距：非常高边缘密度，高饱和度
     */
    private fun scoreMacro(f: ImageFeatures): Float {
        var score = 0f
        // 高边缘密度
        score += sigmoid(f.edgeDensity, 0.30f, 12f) * 0.35f
        // 高饱和度
        score += sigmoid(f.avgSaturation, 0.35f, 5f) * 0.20f
        // 低天空
        score += invSigmoid(f.skyRatio, 0.15f, 10f) * 0.15f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.03f, 15f) * 0.10f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 90f, 200f, 0.02f) * 0.10f
        // 低暗部比例（微距通常光线充足）
        score += invSigmoid(f.darkPixelRatio, 0.25f, 6f) * 0.10f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 海滩：高天空，高亮度，蓝色色调
     */
    private fun scoreBeach(f: ImageFeatures): Float {
        var score = 0f
        // 高天空比例
        score += sigmoid(f.skyRatio, 0.35f, 8f) * 0.25f
        // 高亮度
        score += sigmoid(f.avgBrightness, 150f, 0.02f) * 0.20f
        // 蓝色色调
        score += sigmoid(f.blueHueRatio, 0.15f, 10f) * 0.20f
        // 低绿色
        score += invSigmoid(f.greenRatio, 0.12f, 8f) * 0.10f
        // 中等饱和度
        score += rangeScore(f.avgSaturation, 0.2f, 0.55f, 4f) * 0.10f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.06f, 10f) * 0.05f
        // 高光
        score += sigmoid(f.highlightRatio, 0.10f, 8f) * 0.05f
        // 低边缘密度
        score += invSigmoid(f.edgeDensity, 0.20f, 8f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 雪景：非常高亮度，低饱和度，高天空
     */
    private fun scoreSnow(f: ImageFeatures): Float {
        var score = 0f
        // 非常高亮度
        score += sigmoid(f.avgBrightness, 180f, 0.03f) * 0.30f
        // 低饱和度
        score += invSigmoid(f.avgSaturation, 0.25f, 6f) * 0.25f
        // 高光占比高
        score += sigmoid(f.highlightRatio, 0.15f, 8f) * 0.20f
        // 有天空
        score += sigmoid(f.skyRatio, 0.20f, 6f) * 0.10f
        // 高低饱和度像素比例
        score += sigmoid(f.lowSaturationRatio, 0.4f, 5f) * 0.10f
        // 低暗部
        score += invSigmoid(f.darkPixelRatio, 0.10f, 8f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 日落：高暖色调比例，中等亮度，中高饱和度
     */
    private fun scoreSunset(f: ImageFeatures): Float {
        var score = 0f
        // 高暖色调
        score += sigmoid(f.warmRatio, 0.40f, 8f) * 0.30f
        // 有天空
        score += sigmoid(f.skyRatio, 0.20f, 6f) * 0.20f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 100f, 200f, 0.02f) * 0.15f
        // 中高饱和度
        score += sigmoid(f.avgSaturation, 0.3f, 5f) * 0.15f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.05f, 10f) * 0.10f
        // 低绿色
        score += invSigmoid(f.greenRatio, 0.08f, 10f) * 0.05f
        // 低蓝色色调
        score += invSigmoid(f.blueHueRatio, 0.10f, 8f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 花卉：高饱和度，暖色调，中等边缘密度
     */
    private fun scoreFlower(f: ImageFeatures): Float {
        var score = 0f
        // 高饱和度
        score += sigmoid(f.avgSaturation, 0.35f, 5f) * 0.30f
        // 暖色
        score += sigmoid(f.warmRatio, 0.20f, 6f) * 0.20f
        // 中等边缘密度
        score += rangeScore(f.edgeDensity, 0.10f, 0.35f, 6f) * 0.15f
        // 低肤色
        score += invSigmoid(f.skinToneRatio, 0.04f, 12f) * 0.10f
        // 低天空
        score += invSigmoid(f.skyRatio, 0.25f, 6f) * 0.10f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 100f, 200f, 0.02f) * 0.10f
        // 也有绿色（茎叶）
        score += sigmoid(f.greenRatio, 0.05f, 8f) * 0.05f
        return score.coerceIn(0f, 1f)
    }

    /**
     * 街拍：中等各项，有一定边缘密度
     */
    private fun scoreStreet(f: ImageFeatures): Float {
        var score = 0f
        // 中等边缘密度
        score += rangeScore(f.edgeDensity, 0.10f, 0.30f, 6f) * 0.20f
        // 中等亮度
        score += rangeScore(f.avgBrightness, 80f, 180f, 0.015f) * 0.15f
        // 中等饱和度
        score += rangeScore(f.avgSaturation, 0.15f, 0.45f, 4f) * 0.15f
        // 低天空（城市环境遮挡）
        score += invSigmoid(f.skyRatio, 0.35f, 6f) * 0.10f
        // 有少量肤色（路人）
        score += rangeScore(f.skinToneRatio, 0.01f, 0.08f, 20f) * 0.10f
        // 中等暖色
        score += rangeScore(f.warmRatio, 0.15f, 0.45f, 5f) * 0.10f
        // 有一定对比度
        score += sigmoid(f.brightnessStdDev, 40f, 0.02f) * 0.10f
        // 低绿色
        score += invSigmoid(f.greenRatio, 0.12f, 8f) * 0.10f
        return score.coerceIn(0f, 1f)
    }
}
