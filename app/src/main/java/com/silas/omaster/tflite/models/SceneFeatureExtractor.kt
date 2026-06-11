package com.silas.omaster.tflite.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.silas.omaster.util.ReleaseLog

import com.silas.omaster.tflite.TFLiteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * 场景特征提取器
 * 
 * 从图像中提取用于场景分类的特征向量
 * 支持多种特征提取方法：
 * - 颜色特征（颜色直方图、主色调）
 * - 纹理特征（边缘、纹理复杂度）
 * - 亮度特征（亮度分布、对比度）
 * - 空间特征（区域划分、空间分布）
 * 
 * 特征维度：
 * - 颜色特征：12维（RGB各4维）
 * - 亮度特征：8维（亮度分布）
 * - 纹理特征：10维（边缘强度、纹理复杂度）
 * - 空间特征：6维（区域分布）
 * - 总计：36维（与场景分类模型输出对应）
 */
class SceneFeatureExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "SceneFeatureExtractor"
        
        // 特征维度
        const val COLOR_FEATURE_DIM = 12
        const val BRIGHTNESS_FEATURE_DIM = 8
        const val TEXTURE_FEATURE_DIM = 10
        const val SPATIAL_FEATURE_DIM = 6
        const val TOTAL_FEATURE_DIM = COLOR_FEATURE_DIM + BRIGHTNESS_FEATURE_DIM + TEXTURE_FEATURE_DIM + SPATIAL_FEATURE_DIM
        
        // 图像分析尺寸
        const val ANALYSIS_SIZE = 256
        
        // 颜色量化级别
        const val COLOR_QUANTIZATION_LEVELS = 4
        
        // 亮度区域划分
        const val BRIGHTNESS_REGIONS = 8
    }
    
    // TFLite引擎
    private val engine = TFLiteEngine.getInstance(context)
    
    /**
     * 提取场景特征
     * 
     * @param bitmap 输入图像
     * @return 特征向量（36维）
     */
    suspend fun extractFeatures(bitmap: Bitmap): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            
            // 缩放图像以加快分析速度
            val analysisBitmap = Bitmap.createScaledBitmap(
                bitmap,
                ANALYSIS_SIZE,
                ANALYSIS_SIZE,
                true
            )
            
            // 提取各类特征
            val colorFeatures = extractColorFeatures(analysisBitmap)
            val brightnessFeatures = extractBrightnessFeatures(analysisBitmap)
            val textureFeatures = extractTextureFeatures(analysisBitmap)
            val spatialFeatures = extractSpatialFeatures(analysisBitmap)
            
            // 合并特征向量
            val features = FloatArray(TOTAL_FEATURE_DIM)
            
            // 颜色特征（0-11）
            for (i in 0 until COLOR_FEATURE_DIM) {
                features[i] = colorFeatures[i]
            }
            
            // 亮度特征（12-19）
            for (i in 0 until BRIGHTNESS_FEATURE_DIM) {
                features[COLOR_FEATURE_DIM + i] = brightnessFeatures[i]
            }
            
            // 纹理特征（20-29）
            for (i in 0 until TEXTURE_FEATURE_DIM) {
                features[COLOR_FEATURE_DIM + BRIGHTNESS_FEATURE_DIM + i] = textureFeatures[i]
            }
            
            // 空间特征（30-35）
            for (i in 0 until SPATIAL_FEATURE_DIM) {
                features[COLOR_FEATURE_DIM + BRIGHTNESS_FEATURE_DIM + TEXTURE_FEATURE_DIM + i] = spatialFeatures[i]
            }
            
            // 归一化特征向量
            normalizeFeatures(features)
            
            val elapsed = System.currentTimeMillis() - startTime
            ReleaseLog.d(TAG, "特征提取完成: ${features.size}维, 耗时${elapsed}ms")
            
            Result.success(features)
        } catch (e: Exception) {
            ReleaseLog.e(TAG, "特征提取失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 提取颜色特征
     * 
     * RGB各通道的颜色分布统计
     */
    private fun extractColorFeatures(bitmap: Bitmap): FloatArray {
        val features = FloatArray(COLOR_FEATURE_DIM)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // RGB通道统计
        val rBins = IntArray(COLOR_QUANTIZATION_LEVELS)
        val gBins = IntArray(COLOR_QUANTIZATION_LEVELS)
        val bBins = IntArray(COLOR_QUANTIZATION_LEVELS)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // 量化到指定级别
            val rBin = (r * COLOR_QUANTIZATION_LEVELS / 256).coerceIn(0, COLOR_QUANTIZATION_LEVELS - 1)
            val gBin = (g * COLOR_QUANTIZATION_LEVELS / 256).coerceIn(0, COLOR_QUANTIZATION_LEVELS - 1)
            val bBin = (b * COLOR_QUANTIZATION_LEVELS / 256).coerceIn(0, COLOR_QUANTIZATION_LEVELS - 1)
            
            rBins[rBin]++
            gBins[gBin]++
            bBins[bBin]++
        }
        
        // 归一化并填充特征
        val total = pixels.size.toFloat()
        for (i in 0 until COLOR_QUANTIZATION_LEVELS) {
            features[i] = rBins[i] / total
            features[COLOR_QUANTIZATION_LEVELS + i] = gBins[i] / total
            features[COLOR_QUANTIZATION_LEVELS * 2 + i] = bBins[i] / total
        }
        
        return features
    }
    
    /**
     * 提取亮度特征
     * 
     * 亮度分布统计和对比度指标
     */
    private fun extractBrightnessFeatures(bitmap: Bitmap): FloatArray {
        val features = FloatArray(BRIGHTNESS_FEATURE_DIM)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 亮度区域统计
        val brightnessBins = IntArray(BRIGHTNESS_REGIONS)
        var totalBrightness = 0f
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // 计算亮度
            val brightness = 0.2126f * r + 0.7152f * g + 0.0722f * b
            totalBrightness += brightness
            
            // 量化到指定区域
            val bin = (brightness * BRIGHTNESS_REGIONS / 256).toInt().coerceIn(0, BRIGHTNESS_REGIONS - 1)
            brightnessBins[bin]++
        }
        
        // 归一化并填充特征
        val total = pixels.size.toFloat()
        for (i in 0 until BRIGHTNESS_REGIONS) {
            features[i] = brightnessBins[i] / total
        }
        
        return features
    }
    
    /**
     * 提取纹理特征
     * 
     * 边缘强度和纹理复杂度
     */
    private fun extractTextureFeatures(bitmap: Bitmap): FloatArray {
        val features = FloatArray(TEXTURE_FEATURE_DIM)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 计算边缘强度（使用Sobel算子简化版）
        var edgeSum = 0f
        var edgeCount = 0
        
        // 计算纹理复杂度（局部方差）
        var textureSum = 0f
        var textureCount = 0
        
        // 区域纹理分析（10个区域）
        val regionTexture = FloatArray(10)
        val regionSize = (width * height) / 10
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val centerLuma = getLuminance(pixels[y * width + x])
                val rightLuma = getLuminance(pixels[y * width + x + 1])
                val bottomLuma = getLuminance(pixels[(y + 1) * width + x])
                
                // 水平和垂直边缘
                val edgeX = kotlin.math.abs(centerLuma - rightLuma)
                val edgeY = kotlin.math.abs(centerLuma - bottomLuma)
                val edgeStrength = edgeX + edgeY
                
                edgeSum += edgeStrength
                edgeCount++
                
                // 纹理复杂度
                val neighbors = listOf(
                    getLuminance(pixels[(y - 1) * width + x]),
                    getLuminance(pixels[(y + 1) * width + x]),
                    getLuminance(pixels[y * width + (x - 1)]),
                    getLuminance(pixels[y * width + (x + 1)])
                )
                
                val avgNeighbor = neighbors.average()
                val variance = neighbors.map { (it - avgNeighbor) * (it - avgNeighbor) }.average()
                textureSum += variance
                textureCount++
                
                // 区域纹理统计
                val regionIndex = ((y * width + x) / regionSize).coerceIn(0, 9)
                regionTexture[regionIndex] += edgeStrength
            }
        }
        
        // 平均边缘强度
        val avgEdge = if (edgeCount > 0) edgeSum / edgeCount else 0f
        val avgTexture = if (textureCount > 0) textureSum / textureCount else 0f
        
        // 填充特征
        features[0] = avgEdge / 255f
        features[1] = avgTexture / 1000f
        
        // 区域纹理特征（归一化）
        val maxRegionTexture = regionTexture.maxOrNull() ?: 1f
        for (i in 2 until 10) {
            features[i] = regionTexture[i - 2] / maxRegionTexture
        }
        
        return features
    }
    
    /**
     * 提取空间特征
     * 
     * 图像区域分布特征
     */
    private fun extractSpatialFeatures(bitmap: Bitmap): FloatArray {
        val features = FloatArray(SPATIAL_FEATURE_DIM)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 将图像划分为6个区域（左上、右上、左下、右下、中心上、中心下）
        val regions = FloatArray(6)
        val regionWidth = width / 2
        val regionHeight = height / 3
        
        for (region in 0 until 6) {
            val startX = if (region % 2 == 0) 0 else regionWidth
            val startY = (region / 2) * regionHeight
            
            var regionBrightness = 0f
            var count = 0
            
            for (y in startY until startY + regionHeight) {
                for (x in startX until startX + regionWidth) {
                    regionBrightness += getLuminance(pixels[y * width + x])
                    count++
                }
            }
            
            regions[region] = if (count > 0) regionBrightness / count else 0f
        }
        
        // 归一化
        val maxBrightness = regions.maxOrNull() ?: 1f
        for (i in 0 until 6) {
            features[i] = regions[i] / maxBrightness
        }
        
        return features
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
     * 归一化特征向量
     */
    private fun normalizeFeatures(features: FloatArray) {
        // 计算特征向量长度
        var length = 0f
        for (feature in features) {
            length += feature * feature
        }
        length = kotlin.math.sqrt(length)
        
        // 归一化
        if (length > 0) {
            for (i in features.indices) {
                features[i] /= length
            }
        }
    }
    
    /**
     * 提取主色调
     * 
     * @param bitmap 输入图像
     * @param numColors 提取的颜色数量
     * @return 主色调列表（RGB值）
     */
    fun extractDominantColors(bitmap: Bitmap, numColors: Int = 5): List<Int> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 简化的颜色聚类（使用颜色量化）
        val colorMap = mutableMapOf<Int, Int>()
        
        for (pixel in pixels) {
            // 量化颜色（减少颜色数量）
            val r = ((pixel shr 16) and 0xFF) / 32 * 32
            val g = ((pixel shr 8) and 0xFF) / 32 * 32
            val b = (pixel and 0xFF) / 32 * 32
            
            val quantizedColor = (r shl 16) | (g shl 8) | b
            colorMap[quantizedColor] = colorMap.getOrDefault(quantizedColor, 0) + 1
        }
        
        // 按频率排序，取前N个
        return colorMap.entries
            .sortedByDescending { it.value }
            .take(numColors)
            .map { it.key }
    }
    
    /**
     * 计算颜色饱和度分布
     */
    fun calculateSaturationDistribution(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val saturationBins = IntArray(10)
        
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            
            val saturation = if (max > 0) {
                (max - min) / max
            } else 0f
            
            val bin = (saturation * 10).toInt().coerceIn(0, 9)
            saturationBins[bin]++
        }
        
        val total = pixels.size.toFloat()
        return FloatArray(10) { i -> saturationBins[i] / total }
    }
    
    /**
     * 计算图像复杂度评分
     */
    fun calculateComplexityScore(bitmap: Bitmap): Float {
        val featuresResult = extractFeatures(bitmap)
        val features = featuresResult.getOrNull() ?: return 0f
        
        // 基于特征方差计算复杂度
        var variance = 0f
        val mean = features.average()
        
        for (feature in features) {
            variance += (feature - mean) * (feature - mean)
        }
        
        return (variance / features.size).coerceIn(0f, 1f)
    }
}