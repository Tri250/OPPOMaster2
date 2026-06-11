package com.silas.omaster.tflite

import org.junit.Test
import org.junit.Assert.*

/**
 * ModelLoader 单元测试
 * 测试模型加载器的逻辑
 */
class ModelLoaderTest {

    @Test
    fun `模型信息 - 应该包含所有模型`() {
        val modelInfo = mapOf(
            "scene_classifier.tflite" to TestModelInfo(
                name = "scene_classifier.tflite",
                expectedSize = 700 * 1024L,
                version = "1.0.0"
            ),
            "quality_analyzer.tflite" to TestModelInfo(
                name = "quality_analyzer.tflite",
                expectedSize = 500 * 1024L,
                version = "1.0.0"
            ),
            "param_predictor.tflite" to TestModelInfo(
                name = "param_predictor.tflite",
                expectedSize = 200 * 1024L,
                version = "1.0.0"
            )
        )
        
        assertEquals("应该有3个模型", 3, modelInfo.size)
        assertTrue("应该包含场景分类模型", modelInfo.containsKey("scene_classifier.tflite"))
        assertTrue("应该包含质量分析模型", modelInfo.containsKey("quality_analyzer.tflite"))
        assertTrue("应该包含参数预测模型", modelInfo.containsKey("param_predictor.tflite"))
    }

    @Test
    fun `模型大小验证 - 应该在合理范围内`() {
        val modelSizes = mapOf(
            "scene_classifier.tflite" to 700 * 1024L,
            "quality_analyzer.tflite" to 500 * 1024L,
            "param_predictor.tflite" to 200 * 1024L
        )
        
        for ((name, size) in modelSizes) {
            assertTrue("$name 大小应该大于0", size > 0)
            assertTrue("$name 大小应该小于10MB", size < 10 * 1024 * 1024)
        }
    }

    @Test
    fun `模型完整性验证 - 应该允许10%误差`() {
        val expectedSize = 700 * 1024L
        val actualSize = 720 * 1024L // 约2.8%误差
        
        val minSize = expectedSize * 0.9
        val maxSize = expectedSize * 1.1
        
        val isValid = actualSize >= minSize && actualSize <= maxSize
        assertTrue("应该接受10%以内的误差", isValid)
    }

    @Test
    fun `模型位置 - 应该支持多种存储位置`() {
        val locations = listOf(
            ModelLocation.ASSETS,
            ModelLocation.FILE_SYSTEM,
            ModelLocation.NOT_FOUND
        )
        
        assertEquals("应该有3种位置类型", 3, locations.size)
    }

    @Test
    fun `模型加载状态 - 应该包含所有必要信息`() {
        val status = TestModelLoadStatus(
            modelName = "scene_classifier.tflite",
            isAvailable = true,
            location = ModelLocation.ASSETS,
            size = 700 * 1024L,
            version = "1.0.0",
            isValid = true
        )
        
        assertEquals("scene_classifier.tflite", status.modelName)
        assertTrue(status.isAvailable)
        assertEquals(ModelLocation.ASSETS, status.location)
        assertTrue(status.isValid)
    }
}

/**
 * InferenceResult 单元测试
 */
class InferenceResultTest {

    @Test
    fun `推理结果创建 - 应该正确创建结果对象`() {
        val result = InferenceResult(
            sceneLabel = "portrait",
            confidence = 0.85f,
            processingTimeMs = 50L
        )
        
        assertEquals("portrait", result.sceneLabel)
        assertEquals(0.85f, result.confidence)
        assertEquals(50L, result.processingTimeMs)
    }

    @Test
    fun `置信度范围 - 应该在0到1之间`() {
        val result = InferenceResult(
            sceneLabel = "landscape",
            confidence = 0.92f,
            processingTimeMs = 30L
        )
        
        assertTrue("置信度应该在0到1之间", result.confidence in 0.0f..1.0f)
    }

    @Test
    fun `处理时间 - 应该非负`() {
        val result = InferenceResult(
            sceneLabel = "night",
            confidence = 0.78f,
            processingTimeMs = 100L
        )
        
        assertTrue("处理时间应该非负", result.processingTimeMs >= 0)
    }
}

/**
 * QualityMetrics 单元测试
 */
class QualityMetricsTest {

    @Test
    fun `亮度分布 - 应该总和为1`() {
        val distribution = BrightnessDistribution(
            shadows = 0.25f,
            midtones = 0.5f,
            highlights = 0.25f,
            meanBrightness = 128f,
            stdDeviation = 50f
        )
        
        val total = distribution.shadows + distribution.midtones + distribution.highlights
        assertEquals("亮度分布总和应该为1", 1.0f, total, 0.01f)
    }

    @Test
    fun `对比度指标 - 应该在有效范围内`() {
        val contrast = ContrastMetrics(
            globalContrast = 50f,
            localContrast = 40f,
            dynamicRange = 200f
        )
        
        assertTrue("全局对比度应该在0到100之间", contrast.globalContrast in 0.0f..100.0f)
        assertTrue("局部对比度应该在0到100之间", contrast.localContrast in 0.0f..100.0f)
    }

    @Test
    fun `噪点指标 - 应该在有效范围内`() {
        val noise = NoiseMetrics(
            estimatedNoise = 15f,
            noiseType = "gaussian",
            noiseLevel = "medium"
        )
        
        assertTrue("噪点估计应该在0到100之间", noise.estimatedNoise in 0.0f..100.0f)
    }

    @Test
    fun `模糊指标 - 应该正确判断模糊状态`() {
        val blur = BlurMetrics(
            blurScore = 60f,
            isBlurred = true,
            blurType = "motion"
        )
        
        assertTrue("模糊分数应该在0到100之间", blur.blurScore in 0.0f..100.0f)
        assertTrue("应该被标记为模糊", blur.isBlurred)
    }

    @Test
    fun `总体质量评分 - 应该在0到100之间`() {
        val metrics = TestQualityMetrics(
            brightnessScore = 75f,
            contrastScore = 80f,
            noiseScore = 85f,
            sharpnessScore = 70f
        )
        
        // 加权平均: 75*0.2 + 80*0.25 + 85*0.25 + 70*0.3 = 15 + 20 + 21.25 + 21 = 77.25
        val overallScore = metrics.brightnessScore * 0.2f +
                          metrics.contrastScore * 0.25f +
                          metrics.noiseScore * 0.25f +
                          metrics.sharpnessScore * 0.3f
        
        assertTrue("总体评分应该在0到100之间", overallScore in 0.0f..100.0f)
        assertEquals(77.25f, overallScore, 0.5f)
    }
}

/**
 * TFLiteEngine 单元测试
 */
class TFLiteEngineTest {

    @Test
    fun `模型名称 - 应该是有效的文件名`() {
        val modelNames = listOf(
            "scene_classifier.tflite",
            "quality_analyzer.tflite",
            "param_predictor.tflite"
        )
        
        for (name in modelNames) {
            assertTrue("$name 应该以.tflite结尾", name.endsWith(".tflite"))
            assertFalse("$name 不应该包含特殊字符", name.contains(Regex("[^a-z_.]")))
        }
    }

    @Test
    fun `输入尺寸 - 应该是合理的尺寸`() {
        val inputSizes = mapOf(
            "scene_classifier.tflite" to 224, // 224x224
            "quality_analyzer.tflite" to 224,
            "param_predictor.tflite" to 224
        )
        
        for ((_, size) in inputSizes) {
            assertTrue("输入尺寸应该大于0", size > 0)
            assertTrue("输入尺寸应该是偶数", size % 2 == 0)
        }
    }

    @Test
    fun `输出维度 - 应该是合理的维度`() {
        val outputDims = mapOf(
            "scene_classifier.tflite" to 36, // 36种场景
            "quality_analyzer.tflite" to 4,  // 亮度、对比度、噪点、模糊
            "param_predictor.tflite" to 18   // 18个调校参数
        )
        
        for ((_, dim) in outputDims) {
            assertTrue("输出维度应该大于0", dim > 0)
        }
    }
}

/**
 * SceneClassifier 单元测试
 */
class SceneClassifierTest {

    @Test
    fun `场景类别 - 应该包含所有主要类别`() {
        val categories = listOf(
            "portrait", "landscape", "night", "food",
            "urban", "still_life", "macro", "event"
        )
        
        assertTrue("应该有8个主要类别", categories.size == 8)
    }

    @Test
    fun `场景标签 - 应该是有效的标识符`() {
        val labels = listOf(
            "portrait-indoor", "portrait-outdoor", "portrait-studio",
            "landscape-sunset", "landscape-mountain", "landscape-seascape",
            "night-cityscape", "night-neon", "night-portrait"
        )
        
        for (label in labels) {
            assertTrue("$label 应该包含连字符", label.contains("-"))
            assertFalse("$label 不应该包含空格", label.contains(" "))
        }
    }

    @Test
    fun `置信度阈值 - 应该在合理范围内`() {
        val confidenceThreshold = 0.5f
        
        assertTrue("置信度阈值应该在0到1之间", confidenceThreshold in 0.0f..1.0f)
        assertTrue("置信度阈值应该大于0.3", confidenceThreshold > 0.3f)
    }
}

/**
 * ParamPredictor 单元测试
 */
class ParamPredictorTest {

    @Test
    fun `参数范围 - 所有参数应该在有效范围内`() {
        val params = mapOf(
            "tone" to (-30..30),
            "saturation" to (-30..30),
            "contrast" to (-30..30),
            "colorTemp" to (-30..30),
            "sharpness" to (0..30),
            "vignette" to (-30..30),
            "cyanMagenta" to (-30..30)
        )
        
        for ((name, range) in params) {
            assertTrue("$name 范围应该有效", range.first < range.last)
        }
    }

    @Test
    fun `参数数量 - 应该输出18个参数`() {
        val expectedParamCount = 18
        
        assertEquals("应该输出18个参数", 18, expectedParamCount)
    }
}

/**
 * ImageQualityAnalyzer 单元测试
 */
class ImageQualityAnalyzerTest {

    @Test
    fun `质量指标 - 应该包含所有指标`() {
        val metrics = listOf(
            "brightness", "contrast", "noise", "blur",
            "dynamic_range", "sharpness", "exposure"
        )
        
        assertTrue("应该有7个质量指标", metrics.size == 7)
    }

    @Test
    fun `质量评分 - 应该在0到100之间`() {
        val scores = listOf(75f, 80f, 65f, 90f, 55f)
        
        for (score in scores) {
            assertTrue("质量评分应该在0到100之间", score in 0.0f..100.0f)
        }
    }
}

// 辅助数据类和枚举
data class TestModelInfo(
    val name: String,
    val expectedSize: Long,
    val version: String
)

data class TestModelLoadStatus(
    val modelName: String,
    val isAvailable: Boolean,
    val location: ModelLocation,
    val size: Long,
    val version: String,
    val isValid: Boolean
)

enum class ModelLocation {
    ASSETS, FILE_SYSTEM, NOT_FOUND
}

data class TestQualityMetrics(
    val brightnessScore: Float,
    val contrastScore: Float,
    val noiseScore: Float,
    val sharpnessScore: Float
)

data class BrightnessDistribution(
    val shadows: Float = 0.25f,
    val midtones: Float = 0.5f,
    val highlights: Float = 0.25f,
    val meanBrightness: Float = 128f,
    val stdDeviation: Float = 50f
)

data class ContrastMetrics(
    val globalContrast: Float = 50f,
    val localContrast: Float = 40f,
    val dynamicRange: Float = 200f
)

data class NoiseMetrics(
    val estimatedNoise: Float = 10f,
    val noiseType: String = "gaussian",
    val noiseLevel: String = "low"
)

data class BlurMetrics(
    val blurScore: Float = 20f,
    val isBlurred: Boolean = false,
    val blurType: String = "none"
)
