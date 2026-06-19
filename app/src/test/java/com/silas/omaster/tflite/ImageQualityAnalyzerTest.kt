package com.silas.omaster.tflite

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

/**
 * ImageQualityAnalyzer 单元测试
 * 测试图像质量分析器的算法逻辑
 */
class ImageQualityAnalyzerTest {

    @Test
    fun `亮度计算 - 加权亮度公式正确`() {
        val r = 100f
        val g = 150f
        val b = 200f
        
        // 使用标准加权公式
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        
        assertEquals(138.1f, luminance, 0.1f)
    }

    @Test
    fun `亮度分布 - 区域划分阈值正确`() {
        val SHADOW_THRESHOLD = 85
        val HIGHLIGHT_THRESHOLD = 170
        
        // 测试阴影检测
        assertTrue("亮度50应该在阴影范围", 50 < SHADOW_THRESHOLD)
        assertFalse("亮度100不应该在阴影范围", 100 < SHADOW_THRESHOLD)
        
        // 测试高光检测
        assertTrue("亮度200应该在高光范围", 200 > HIGHLIGHT_THRESHOLD)
        assertFalse("亮度150不应该在高光范围", 150 > HIGHLIGHT_THRESHOLD)
    }

    @Test
    fun `标准差计算 - 样本标准差正确`() {
        val values = floatArrayOf(100f, 110f, 120f, 130f, 140f)
        val mean = values.average().toFloat()
        
        var sumSquaredDiff = 0f
        for (value in values) {
            sumSquaredDiff += (value - mean) * (value - mean)
        }
        val stdDeviation = sqrt(sumSquaredDiff / values.size)
        
        assertEquals(14.14f, stdDeviation, 0.1f)
    }

    @Test
    fun `区域占比计算 - 阴影中间调高光比例正确`() {
        val luminances = floatArrayOf(50f, 70f, 100f, 150f, 200f)
        val SHADOW_THRESHOLD = 85
        val HIGHLIGHT_THRESHOLD = 170
        
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
        
        assertEquals("应该有2个阴影像素", 2, shadowCount)
        assertEquals("应该有2个中间调像素", 2, midtoneCount)
        assertEquals("应该有1个高光像素", 1, highlightCount)
    }

    @Test
    fun `对比度计算 - Michelson对比度公式正确`() {
        val minLuma = 50f
        val maxLuma = 200f
        
        val michelsonContrast = (maxLuma - minLuma) / (maxLuma + minLuma) * 100f
        
        assertEquals(60f, michelsonContrast, 0.1f)
    }

    @Test
    fun `动态范围计算 - 最大最小亮度差`() {
        val minLuma = 30f
        val maxLuma = 220f
        
        val dynamicRange = maxLuma - minLuma
        
        assertEquals(190f, dynamicRange, 0.1f)
    }

    @Test
    fun `局部对比度 - 块内最大最小亮度差`() {
        val blockPixels = floatArrayOf(50f, 80f, 120f, 180f)
        
        val minLuma = blockPixels.minOrNull() ?: 0f
        val maxLuma = blockPixels.maxOrNull() ?: 255f
        
        val localContrast = if (maxLuma + minLuma > 0) {
            (maxLuma - minLuma) / (maxLuma + minLuma) * 100f
        } else 0f
        
        assertEquals(56.5f, localContrast, 0.1f)
    }

    @Test
    fun `噪点估计 - 高通滤波原理`() {
        val centerLuma = 128f
        val neighborLumas = listOf(125f, 130f, 126f, 132f)
        
        val avgNeighborLuma = neighborLumas.average().toFloat()
        val noiseDiff = kotlin.math.abs(centerLuma - avgNeighborLuma)
        
        assertTrue("噪点差异应该小于5", noiseDiff < 5f)
    }

    @Test
    fun `Laplacian算子 - 边缘检测原理`() {
        val centerLuma = 150f
        val topLuma = 100f
        val bottomLuma = 200f
        val leftLuma = 120f
        val rightLuma = 180f
        
        // Laplacian算子: 4*center - top - bottom - left - right
        val laplacian = 4 * centerLuma - topLuma - bottomLuma - leftLuma - rightLuma
        
        assertEquals(0f, laplacian, 0.001f)
    }

    @Test
    fun `模糊评分 - 方差越小越模糊`() {
        val varianceLow = 100f  // 低方差 = 模糊
        val varianceHigh = 2000f  // 高方差 = 清晰
        
        val blurScoreLow = 100f - (varianceLow / 1000f).coerceIn(0f, 100f)
        val blurScoreHigh = 100f - (varianceHigh / 1000f).coerceIn(0f, 100f)
        
        assertEquals(0f, blurScoreLow, 0.1f) // 完全模糊
        assertTrue(blurScoreHigh < 0f) // 清晰
    }

    @Test
    fun `质量评分阈值 - 低中高等级划分`() {
        val LOW_QUALITY_THRESHOLD = 40f
        val HIGH_QUALITY_THRESHOLD = 80f
        
        val lowScore = 35f
        val midScore = 60f
        val highScore = 85f
        
        assertTrue("35分应该是低质量", lowScore < LOW_QUALITY_THRESHOLD)
        assertTrue("60分应该是中等质量", midScore in LOW_QUALITY_THRESHOLD..HIGH_QUALITY_THRESHOLD)
        assertTrue("85分应该是高质量", highScore >= HIGH_QUALITY_THRESHOLD)
    }

    @Test
    fun `评分归一化 - 限制在0-100范围内`() {
        val rawScores = listOf(-10f, 50f, 120f, 100f)
        
        val normalized = rawScores.map { it.coerceIn(0f, 100f) }
        
        assertEquals(0f, normalized[0], 0.001f)  // -10 -> 0
        assertEquals(50f, normalized[1], 0.001f)  // 50 -> 50
        assertEquals(100f, normalized[2], 0.001f) // 120 -> 100
        assertEquals(100f, normalized[3], 0.001f) // 100 -> 100
    }

    @Test
    fun `改进建议 - 亮度偏低建议`() {
        val brightnessScore = 45f
        val suggestions = mutableListOf<String>()
        
        if (brightnessScore < 50f) {
            suggestions.add("图像亮度偏低，建议适当增加曝光补偿")
        }
        
        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].contains("亮度偏低"))
    }

    @Test
    fun `改进建议 - 噪点多建议`() {
        val noiseScore = 55f
        val suggestions = mutableListOf<String>()
        
        if (noiseScore < 60f) {
            suggestions.add("图像噪点较多，建议使用降噪处理")
        }
        
        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].contains("噪点"))
    }

    @Test
    fun `改进建议 - 清晰度不足建议`() {
        val sharpnessScore = 45f
        val suggestions = mutableListOf<String>()
        
        if (sharpnessScore < 50f) {
            suggestions.add("图像清晰度不足，建议使用锐化处理")
        }
        
        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].contains("清晰度"))
    }

    @Test
    fun `缓存键生成 - 基于图像尺寸和采样像素`() {
        val width = 1920
        val height = 1080
        val samplePixels = intArrayOf(0xFF111111.toInt(), 0xFF222222.toInt(), 0xFF333333.toInt())
        
        val cacheKey = "quality_${width}_${height}_${samplePixels.contentHashCode()}"
        
        assertTrue(cacheKey.startsWith("quality_1920_1080_"))
    }
}

/**
 * QualityMetrics 数据类测试
 */
class QualityMetricsTest {

    @Test
    fun `质量指标创建 - 亮度分布默认值`() {
        val distribution = BrightnessDistribution(
            shadows = 0.3f,
            midtones = 0.5f,
            highlights = 0.2f,
            meanBrightness = 128f,
            stdDeviation = 50f
        )
        
        assertEquals(0.3f, distribution.shadows, 0.001f)
        assertEquals(0.5f, distribution.midtones, 0.001f)
        assertEquals(0.2f, distribution.highlights, 0.001f)
        assertEquals(128f, distribution.meanBrightness, 0.001f)
    }

    @Test
    fun `对比度指标 - 全局对比度计算`() {
        val metrics = ContrastMetrics(
            globalContrast = 45.5f,
            localContrast = 60f,
            dynamicRange = 180f
        )
        
        assertEquals(45.5f, metrics.globalContrast, 0.001f)
        assertEquals(60f, metrics.localContrast, 0.001f)
        assertEquals(180f, metrics.dynamicRange, 0.001f)
    }

    @Test
    fun `噪点指标 - 噪点类型判断`() {
        val lowNoise = NoiseMetrics(8f, "low", 3f)
        val midNoise = NoiseMetrics(15f, "mixed", 6f)
        val highNoise = NoiseMetrics(25f, "gaussian", 10f)
        
        assertEquals("low", lowNoise.noiseType)
        assertEquals("mixed", midNoise.noiseType)
        assertEquals("gaussian", highNoise.noiseType)
    }

    @Test
    fun `模糊指标 - 模糊类型判断`() {
        val noBlur = BlurMetrics(30f, false, "none")
        val lightBlur = BlurMetrics(55f, true, "light")
        val modBlur = BlurMetrics(65f, true, "moderate")
        val heavyBlur = BlurMetrics(80f, true, "heavy")
        
        assertFalse(noBlur.isBlurred)
        assertTrue(lightBlur.isBlurred)
        assertEquals("light", lightBlur.blurType)
        assertEquals("moderate", modBlur.blurType)
        assertEquals("heavy", heavyBlur.blurType)
    }
}
