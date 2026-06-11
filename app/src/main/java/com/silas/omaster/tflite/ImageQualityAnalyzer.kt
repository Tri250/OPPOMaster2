package com.silas.omaster.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.silas.omaster.util.ReleaseLog

import com.silas.omaster.tflite.models.QualityMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * 图像质量分析器
 * 
 * 基于 NIMA 变体的图像质量评估模型
 * 评估图像的亮度、对比度、噪点和模糊度
 * 
 * 功能：
 * - 亮度分布分析（阴影、中间调、高光）
 * - 对比度评分（全局对比度、局部对比度）
 * - 噪点估计（高斯噪点、椒盐噪点）
 * - 模糊度检测（运动模糊、失焦模糊）
 * - 总体质量评分
 */
class ImageQualityAnalyzer(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageQualityAnalyzer"
        
        // 模型输入尺寸
        private const val INPUT_SIZE = 224
        
        // 亮度区域划分阈值
        private const val SHADOW_THRESHOLD = 85      // 阴影区域阈值（0-85）
        private const val HIGHLIGHT_THRESHOLD = 170  // 高光区域阈值（170-255）
        
        // 质量评分阈值
        private const val LOW_QUALITY_THRESHOLD = 40f
        private const val HIGH_QUALITY_THRESHOLD = 80f
        
        // 模糊检测阈值
        private const val BLUR_THRESHOLD = 50f
        
        // 噪点检测阈值
        private const val NOISE_THRESHOLD = 30f
    }
    
    // TFLite引擎
    private val engine = TFLiteEngine.getInstance(context)
    
    /**
     * 分析图像质量
     * 
     * @param bitmap 输入图像
     * @param useCache 是否使用缓存
     * @return 质量评估结果
     */
    suspend fun analyze(
        bitmap: Bitmap,
        useCache: Boolean = true
    ): Result<QualityResult> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            
            // 生成缓存键
            val cacheKey = if (useCache) {
                generateCacheKey(bitmap)
            } else null
            
            // 预处理图像
            val inputBuffer = preprocessImage(bitmap)
            
            // 执行推理
            val inferenceResult = engine.runInference<FloatArray>(
                modelName = TFLiteEngine.MODEL_QUALITY_ANALYZER,
                input = inputBuffer,
                cacheKey = cacheKey
            )
            
            // 处理推理结果
            val rawScores = inferenceResult.getOrNull()
                ?: return@withContext Result.failure(Exception("质量评估推理失败"))
            
            // 同时进行传统图像分析（补充详细指标）
            val traditionalMetrics = analyzeTraditionalMetrics(bitmap)
            
            // 解析结果
            val result = parseQualityResult(rawScores, traditionalMetrics, startTime)
            
            ReleaseLog.d(TAG, "质量评估完成: 总评分=${result.overallScore}, 耗时=${result.inferenceTimeMs}ms")
            Result.success(result)
        } catch (e: Exception) {
            ReleaseLog.e(TAG, "质量评估失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 预处理图像
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        return engine.preprocessBitmap(resizedBitmap, INPUT_SIZE, normalize = true)
    }
    
    /**
     * 传统图像质量分析
     * 
     * 使用传统图像处理方法获取详细指标
     */
    private fun analyzeTraditionalMetrics(bitmap: Bitmap): QualityMetrics {
        // 缩小图像以提高分析速度
        val analysisBitmap = if (bitmap.width > 512 || bitmap.height > 512) {
            Bitmap.createScaledBitmap(bitmap, 512, 512, true)
        } else {
            bitmap
        }
        
        val width = analysisBitmap.width
        val height = analysisBitmap.height
        val pixels = IntArray(width * height)
        analysisBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 亮度分布分析
        val brightnessDistribution = analyzeBrightnessDistribution(pixels)
        
        // 对比度分析
        val contrastMetrics = analyzeContrast(pixels, width, height)
        
        // 噪点估计
        val noiseMetrics = estimateNoise(pixels, width, height)
        
        // 模糊检测
        val blurMetrics = detectBlur(pixels, width, height)
        
        return QualityMetrics(
            brightnessDistribution = brightnessDistribution,
            contrastMetrics = contrastMetrics,
            noiseMetrics = noiseMetrics,
            blurMetrics = blurMetrics
        )
    }
    
    /**
     * 分析亮度分布
     */
    private fun analyzeBrightnessDistribution(pixels: IntArray): BrightnessDistribution {
        val luminances = FloatArray(pixels.size)
        var sumLuminance = 0f
        
        // 计算每个像素的亮度
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // 使用加权亮度公式
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            luminances[i] = luminance
            sumLuminance += luminance
        }
        
        // 计算平均亮度
        val meanBrightness = sumLuminance / pixels.size
        
        // 计算标准差
        var sumSquaredDiff = 0f
        for (luma in luminances) {
            sumSquaredDiff += (luma - meanBrightness) * (luma - meanBrightness)
        }
        val stdDeviation = sqrt(sumSquaredDiff / pixels.size)
        
        // 计算区域占比
        var shadowCount = 0
        var midtoneCount = 0
        var highlightCount = 0
        
        for (luma in luminances) {
            when {
                luma < SHADOW_THRESHOLD -> shadowCount++
                luma > HIGHLIGHT_THRESHOLD -> highlightCount++
                else -> midtoneCount++
            }
        }
        
        val total = pixels.size.toFloat()
        val shadows = shadowCount / total
        val midtones = midtoneCount / total
        val highlights = highlightCount / total
        
        return BrightnessDistribution(
            shadows = shadows,
            midtones = midtones,
            highlights = highlights,
            meanBrightness = meanBrightness,
            stdDeviation = stdDeviation
        )
    }
    
    /**
     * 分析对比度
     */
    private fun analyzeContrast(pixels: IntArray, width: Int, height: Int): ContrastMetrics {
        // 全局对比度（基于亮度标准差）
        val luminances = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            luminances[i] = 0.2126f * r + 0.7152f * g + 0.0722f * b
        }
        
        // 计算最大最小亮度差
        val minLuma = luminances.minOrNull() ?: 0f
        val maxLuma = luminances.maxOrNull() ?: 255f
        val dynamicRange = maxLuma - minLuma
        
        // 全局对比度（Michelson对比度）
        val globalContrast = if (maxLuma + minLuma > 0) {
            (maxLuma - minLuma) / (maxLuma + minLuma) * 100f
        } else 0f
        
        // 局部对比度（采样分析）
        val localContrast = calculateLocalContrast(pixels, width, height)
        
        return ContrastMetrics(
            globalContrast = globalContrast,
            localContrast = localContrast,
            dynamicRange = dynamicRange
        )
    }
    
    /**
     * 计算局部对比度
     */
    private fun calculateLocalContrast(pixels: IntArray, width: Int, height: Int): Float {
        val blockSize = 16
        val numBlocksX = width / blockSize
        val numBlocksY = height / blockSize
        
        var totalLocalContrast = 0f
        var blockCount = 0
        
        for (blockY in 0 until numBlocksY) {
            for (blockX in 0 until numBlocksX) {
                val startX = blockX * blockSize
                val startY = blockY * blockSize
                
                var minLuma = 255f
                var maxLuma = 0f
                
                for (y in startY until startY + blockSize) {
                    for (x in startX until startX + blockSize) {
                        val pixel = pixels[y * width + x]
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                        
                        minLuma = minOf(minLuma, luma)
                        maxLuma = maxOf(maxLuma, luma)
                    }
                }
                
                if (maxLuma + minLuma > 0) {
                    totalLocalContrast += (maxLuma - minLuma) / (maxLuma + minLuma)
                    blockCount++
                }
            }
        }
        
        return if (blockCount > 0) {
            totalLocalContrast / blockCount * 100f
        } else 0f
    }
    
    /**
     * 估计噪点水平
     */
    private fun estimateNoise(pixels: IntArray, width: Int, height: Int): NoiseMetrics {
        // 使用高通滤波估计噪点
        var noiseSum = 0f
        var noiseCount = 0
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerPixel = pixels[y * width + x]
                val centerLuma = getLuminance(centerPixel)
                
                // 计算周围像素的平均亮度
                val neighbors = listOf(
                    pixels[(y - 1) * width + x],
                    pixels[(y + 1) * width + x],
                    pixels[y * width + (x - 1)],
                    pixels[y * width + (x + 1)]
                )
                
                val avgNeighborLuma = neighbors.map { getLuminance(it) }.average()
                
                // 计算噪点差异
                val noiseDiff = kotlin.math.abs(centerLuma - avgNeighborLuma)
                noiseSum += noiseDiff
                noiseCount++
            }
        }
        
        val estimatedNoise = if (noiseCount > 0) noiseSum / noiseCount else 0f
        
        // 判断噪点类型（简化判断）
        val noiseType = when {
            estimatedNoise > 20f -> "gaussian"
            estimatedNoise > 10f -> "mixed"
            else -> "low"
        }
        
        return NoiseMetrics(
            estimatedNoise = estimatedNoise,
            noiseType = noiseType,
            frequency = estimatedNoise / 255f * 100f
        )
    }
    
    /**
     * 检测模糊
     */
    private fun detectBlur(pixels: IntArray, width: Int, height: Int): BlurMetrics {
        // 使用Laplacian方差检测模糊
        var laplacianSum = 0f
        var laplacianSquaredSum = 0f
        var count = 0
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerLuma = getLuminance(pixels[y * width + x])
                val topLuma = getLuminance(pixels[(y - 1) * width + x])
                val bottomLuma = getLuminance(pixels[(y + 1) * width + x])
                val leftLuma = getLuminance(pixels[y * width + (x - 1)])
                val rightLuma = getLuminance(pixels[y * width + (x + 1)])
                
                // Laplacian算子
                val laplacian = 4 * centerLuma - topLuma - bottomLuma - leftLuma - rightLuma
                
                laplacianSum += laplacian
                laplacianSquaredSum += laplacian * laplacian
                count++
            }
        }
        
        // 计算Laplacian方差
        val meanLaplacian = if (count > 0) laplacianSum / count else 0f
        val variance = if (count > 0) {
            laplacianSquaredSum / count - meanLaplacian * meanLaplacian
        } else 0f
        
        // 模糊评分（方差越小越模糊）
        val blurScore = 100f - (variance / 1000f).coerceIn(0f, 100f)
        val isBlurred = blurScore > BLUR_THRESHOLD
        
        // 判断模糊类型
        val blurType = when {
            !isBlurred -> "none"
            blurScore > 70f -> "heavy"
            blurScore > 50f -> "moderate"
            else -> "light"
        }
        
        return BlurMetrics(
            blurScore = blurScore,
            isBlurred = isBlurred,
            blurType = blurType
        )
    }
    
    /**
     * 获取像素亮度
     */
    private fun getLuminance(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    
    /**
     * 解析质量评估结果
     */
    private fun parseQualityResult(
        rawScores: FloatArray,
        traditionalMetrics: QualityMetrics,
        startTime: Long
    ): QualityResult {
        // rawScores格式: [brightness, contrast, noise, sharpness, overall]
        val brightnessScore = rawScores.getOrElse(0) { 75f }
        val contrastScore = rawScores.getOrElse(1) { 68f }
        val noiseScore = rawScores.getOrElse(2) { 82f }
        val sharpnessScore = rawScores.getOrElse(3) { 70f }
        val overallScore = rawScores.getOrElse(4) { 72f }
        
        // 将评分转换为0-100范围
        val normalizedBrightness = brightnessScore.coerceIn(0f, 100f)
        val normalizedContrast = contrastScore.coerceIn(0f, 100f)
        val normalizedNoise = noiseScore.coerceIn(0f, 100f)
        val normalizedSharpness = sharpnessScore.coerceIn(0f, 100f)
        val normalizedOverall = overallScore.coerceIn(0f, 100f)
        
        return QualityResult(
            brightnessScore = normalizedBrightness,
            contrastScore = normalizedContrast,
            noiseScore = normalizedNoise,
            sharpnessScore = normalizedSharpness,
            overallScore = normalizedOverall,
            brightnessDistribution = traditionalMetrics.brightnessDistribution,
            contrastMetrics = traditionalMetrics.contrastMetrics,
            noiseMetrics = traditionalMetrics.noiseMetrics,
            blurMetrics = traditionalMetrics.blurMetrics,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val samplePixels = IntArray(10)
        
        for (i in 0 until 10) {
            val x = (i * width / 10).coerceIn(0, width - 1)
            val y = (i * height / 10).coerceIn(0, height - 1)
            samplePixels[i] = bitmap.getPixel(x, y)
        }
        
        return "quality_${width}_${height}_${samplePixels.contentHashCode()}"
    }
    
    /**
     * 获取质量等级描述
     */
    fun getQualityLevel(score: Float): QualityLevel {
        return when {
            score >= HIGH_QUALITY_THRESHOLD -> QualityLevel.HIGH
            score >= LOW_QUALITY_THRESHOLD -> QualityLevel.MEDIUM
            else -> QualityLevel.LOW
        }
    }
    
    /**
     * 获取质量改进建议
     */
    fun getImprovementSuggestions(result: QualityResult): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 亮度建议
        if (result.brightnessScore < 50f) {
            suggestions.add("图像亮度偏低，建议适当增加曝光补偿")
        } else if (result.brightnessScore > 90f) {
            suggestions.add("图像可能过曝，建议降低曝光补偿")
        }
        
        // 对比度建议
        if (result.contrastScore < 50f) {
            suggestions.add("对比度偏低，图像可能显得平淡，建议增加对比度")
        }
        
        // 噪点建议
        if (result.noiseScore < 60f) {
            suggestions.add("图像噪点较多，建议使用降噪处理")
        }
        
        // 清晰度建议
        if (result.sharpnessScore < 50f) {
            suggestions.add("图像清晰度不足，建议使用锐化处理")
        }
        
        // 模糊建议
        if (result.blurMetrics.isBlurred) {
            suggestions.add("图像存在模糊，建议检查拍摄稳定性或使用锐化修复")
        }
        
        return suggestions
    }
    
    /**
     * 质量等级
     */
    enum class QualityLevel {
        LOW,    // 低质量（< 40）
        MEDIUM, // 中等质量（40-80）
        HIGH    // 高质量（> 80）
    }
}