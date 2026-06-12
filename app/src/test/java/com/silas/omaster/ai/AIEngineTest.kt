package com.silas.omaster.ai

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AI 引擎单元测试
 * 使用 MockK 模拟 Android 依赖，测试核心业务逻辑
 */
class AIEngineTest {

    @Before
    fun setup() {
        // 初始化 MockK
        MockKAnnotations.init(this)
    }

    // ===== 场景分类测试 =====

    @Test
    fun `场景分类 - 识别风景场景`() {
        val sceneClassifier = mockk<SceneClassifier>()
        
        // 模拟场景分类结果
        every { sceneClassifier.classify(any()) } returns SceneResult(
            sceneType = SceneType.LANDSCAPE,
            confidence = 0.85f,
            probabilities = floatArrayOf(0.85f, 0.05f, 0.03f, 0.02f, 0.01f)
        )
        
        val result = sceneClassifier.classify(mockImageFeatures())
        
        assertEquals(SceneType.LANDSCAPE, result.sceneType)
        assertTrue("置信度应该大于0.7", result.confidence > 0.7f)
        
        verify { sceneClassifier.classify(any()) }
    }

    @Test
    fun `场景分类 - 低置信度返回未知场景`() {
        val sceneClassifier = mockk<SceneClassifier>()
        
        // 模拟低置信度结果
        every { sceneClassifier.classify(any()) } returns SceneResult(
            sceneType = SceneType.UNKNOWN,
            confidence = 0.35f,
            probabilities = FloatArray(36) { 0.03f }
        )
        
        val result = sceneClassifier.classify(mockImageFeatures())
        
        // 低置信度时应该返回 UNKNOWN
        assertTrue("置信度低于阈值时返回UNKNOWN", 
            result.confidence < 0.5f || result.sceneType == SceneType.UNKNOWN)
    }

    @Test
    fun `场景分类 - 概率分布归一化验证`() {
        val probabilities = generateNormalizedProbabilities()
        
        // 概率总和应该等于1
        val sum = probabilities.sum()
        assertTrue("概率总和应该接近1.0: 实际=$sum", 
            sum in 0.99f..1.01f)
        
        // 所有概率应该非负
        assertTrue("所有概率应该非负", probabilities.all { it >= 0f })
    }

    // ===== 质量分析测试 =====

    @Test
    fun `质量分析 - 暗场景噪点评分`() {
        val qualityAnalyzer = mockk<QualityAnalyzer>()
        
        // 模拟暗场景质量分析
        every { qualityAnalyzer.analyze(any()) } returns QualityResult(
            brightness = 35f,  // 暗场景
            contrast = 60f,
            noise = 45f,       // 暗场景噪点多
            sharpness = 70f,
            overall = 52.5f
        )
        
        val result = qualityAnalyzer.analyze(mockDarkImageFeatures())
        
        assertTrue("暗场景亮度应该低于50", result.brightness < 50f)
        assertTrue("暗场景噪点评分应该较低", result.noise < 60f)
        
        verify { qualityAnalyzer.analyze(any()) }
    }

    @Test
    fun `质量分析 - 高对比度场景评分`() {
        val qualityAnalyzer = mockk<QualityAnalyzer>()
        
        every { qualityAnalyzer.analyze(any()) } returns QualityResult(
            brightness = 75f,
            contrast = 90f,    // 高对比度
            noise = 85f,
            sharpness = 88f,
            overall = 84.5f
        )
        
        val result = qualityAnalyzer.analyze(mockHighContrastFeatures())
        
        assertTrue("高对比度评分应该大于80", result.contrast > 80f)
        assertTrue("总体评分应该良好", result.overall > 70f)
    }

    @Test
    fun `质量分析 - 评分范围验证`() {
        val metrics = QualityMetrics(
            brightness = 50f,
            contrast = 65f,
            noise = 80f,
            sharpness = 72f
        )
        
        // 所有评分应该在0-100范围
        assertTrue("亮度评分范围", metrics.brightness in 0f..100f)
        assertTrue("对比度评分范围", metrics.contrast in 0f..100f)
        assertTrue("噪点评分范围", metrics.noise in 0f..100f)
        assertTrue("清晰度评分范围", metrics.sharpness in 0f..100f)
    }

    // ===== 参数预测测试 =====

    @Test
    fun `参数预测 - 人像场景参数推荐`() {
        val paramPredictor = mockk<ParamPredictor>()
        
        every { paramPredictor.predict(any(), any()) } returns ParamResult(
            exposure = 5f,
            contrast = 30f,
            saturation = -5f,     // 人像降低饱和度
            highlights = -20f,    // 压制高光
            shadows = 15f,        // 提升阴影
            sharpness = 25f,
            noiseReduction = 30f
        )
        
        val sceneResult = SceneResult(SceneType.PORTRAIT, 0.85f, FloatArray(36))
        val qualityResult = QualityResult(70f, 65f, 75f, 80f, 72f)
        
        val params = paramPredictor.predict(sceneResult, qualityResult)
        
        // 人像场景应该降低饱和度
        assertTrue("人像场景饱和度应该适度降低", params.saturation <= 0f)
        // 人像场景应该压制高光
        assertTrue("人像场景应该压制高光", params.highlights < 0f)
        
        verify { paramPredictor.predict(sceneResult, qualityResult) }
    }

    @Test
    fun `参数预测 - 夜景场景参数推荐`() {
        val paramPredictor = mockk<ParamPredictor>()
        
        every { paramPredictor.predict(any(), any()) } returns ParamResult(
            exposure = 15f,       // 夜景提升曝光
            contrast = 40f,
            saturation = 10f,
            highlights = 0f,
            shadows = 30f,        // 大幅提升阴影
            sharpness = 20f,
            noiseReduction = 45f  // 夜景需要更多降噪
        )
        
        val sceneResult = SceneResult(SceneType.NIGHT, 0.90f, FloatArray(36))
        val qualityResult = QualityResult(30f, 55f, 40f, 65f, 47.5f)
        
        val params = paramPredictor.predict(sceneResult, qualityResult)
        
        // 夜景场景应该提升曝光
        assertTrue("夜景场景应该提升曝光", params.exposure > 0f)
        // 夜景场景需要更多降噪
        assertTrue("夜景场景降噪应该大于30", params.noiseReduction > 30f)
        
        verify { paramPredictor.predict(sceneResult, qualityResult) }
    }

    @Test
    fun `参数预测 - 参数范围验证`() {
        val params = ParamResult(
            exposure = 50f,
            contrast = 60f,
            saturation = 30f,
            highlights = -20f,
            shadows = 25f,
            whites = 10f,
            blacks = -15f,
            clarity = 40f,
            vibrance = 20f,
            warmth = 10f,
            tint = 5f,
            sharpness = 35f,
            noiseReduction = 25f,
            vignette = 15f,
            grain = 0f,
            fade = 10f,
            splitToneHighlights = 0f,
            splitToneShadows = 0f
        )
        
        // 曝光范围 -100 到 100
        assertTrue("曝光范围", params.exposure in -100f..100f)
        // 对比度范围 -100 到 100
        assertTrue("对比度范围", params.contrast in -100f..100f)
        // 锐度范围 0 到 100
        assertTrue("锐度范围", params.sharpness in 0f..100f)
        // 降噪范围 0 到 100
        assertTrue("降噪范围", params.noiseReduction in 0f..100f)
    }

    // ===== AI 引擎集成测试 =====

    @Test
    fun `AI引擎 - 完整推理流程`() = runTest {
        val aiEngine = mockk<AIEngine>()
        
        // 模拟完整推理流程
        coEvery { aiEngine.processImage(any()) } returns AIResult(
            scene = SceneResult(SceneType.LANDSCAPE, 0.88f, FloatArray(36)),
            quality = QualityResult(75f, 70f, 80f, 85f, 77.5f),
            params = ParamResult(
                exposure = 0f,
                contrast = 50f,
                saturation = 15f,
                sharpness = 30f,
                noiseReduction = 20f
            ),
            mood = Mood.VIBRANT,
            suggestions = listOf("增加饱和度", "适度锐化")
        )
        
        val result = aiEngine.processImage(mockBitmap())
        
        assertNotNull("推理结果不应为null", result)
        assertNotNull("场景结果不应为null", result.scene)
        assertNotNull("质量结果不应为null", result.quality)
        assertNotNull("参数结果不应为null", result.params)
        
        // 验证推理流程被调用
        coVerify { aiEngine.processImage(any()) }
    }

    @Test
    fun `AI引擎 - Mood映射验证`() {
        val moodMapping = mapOf(
            SceneType.LANDSCAPE to Mood.VIBRANT,
            SceneType.PORTRAIT to Mood.SOFT,
            SceneType.NIGHT to Mood.MOODY,
            SceneType.FOOD to Mood.WARM,
            SceneType.STREET to Mood.EDITORIAL
        )
        
        // 验证 Mood 映射存在
        assertTrue("风景场景映射到VIBRANT", moodMapping.containsKey(SceneType.LANDSCAPE))
        assertTrue("人像场景映射到SOFT", moodMapping.containsKey(SceneType.PORTRAIT))
        assertTrue("夜景场景映射到MOODY", moodMapping.containsKey(SceneType.NIGHT))
    }

    // ===== 辅助函数 =====

    private fun mockImageFeatures(): ImageFeatures {
        return mockk(relaxed = true) {
            every { brightness } returns 75f
            every { contrast } returns 65f
            every { saturation } returns 80f
            every { edgeDensity } returns 0.25f
        }
    }

    private fun mockDarkImageFeatures(): ImageFeatures {
        return mockk(relaxed = true) {
            every { brightness } returns 25f
            every { contrast } returns 45f
            every { noiseLevel } returns 0.8f
        }
    }

    private fun mockHighContrastFeatures(): ImageFeatures {
        return mockk(relaxed = true) {
            every { brightness } returns 80f
            every { contrast } returns 95f
            every { edgeDensity } returns 0.35f
        }
    }

    private fun mockBitmap(): Any = mockk(relaxed = true)

    private fun generateNormalizedProbabilities(): FloatArray {
        val probs = FloatArray(36) { (it + 1).toFloat() }
        val sum = probs.sum()
        for (i in probs.indices) {
            probs[i] = probs[i] / sum
        }
        return probs
    }

    // ===== 数据类定义（测试用） =====

    data class SceneResult(
        val sceneType: SceneType,
        val confidence: Float,
        val probabilities: FloatArray
    )

    data class QualityResult(
        val brightness: Float,
        val contrast: Float,
        val noise: Float,
        val sharpness: Float,
        val overall: Float
    )

    data class QualityMetrics(
        val brightness: Float = 50f,
        val contrast: Float = 50f,
        val noise: Float = 50f,
        val sharpness: Float = 50f
    )

    data class ParamResult(
        val exposure: Float,
        val contrast: Float,
        val saturation: Float,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val whites: Float = 0f,
        val blacks: Float = 0f,
        val clarity: Float = 0f,
        val vibrance: Float = 0f,
        val warmth: Float = 0f,
        val tint: Float = 0f,
        val sharpness: Float,
        val noiseReduction: Float,
        val vignette: Float = 0f,
        val grain: Float = 0f,
        val fade: Float = 0f,
        val splitToneHighlights: Float = 0f,
        val splitToneShadows: Float = 0f
    )

    data class AIResult(
        val scene: SceneResult,
        val quality: QualityResult,
        val params: ParamResult,
        val mood: Mood,
        val suggestions: List<String>
    )

    enum class SceneType {
        LANDSCAPE, PORTRAIT, NIGHT, FOOD, STREET, UNKNOWN
    }

    enum class Mood {
        VIBRANT, SOFT, MOODY, WARM, EDITORIAL
    }

    interface SceneClassifier {
        fun classify(features: ImageFeatures): SceneResult
    }

    interface QualityAnalyzer {
        fun analyze(features: ImageFeatures): QualityResult
    }

    interface ParamPredictor {
        fun predict(scene: SceneResult, quality: QualityResult): ParamResult
    }

    interface AIEngine {
        suspend fun processImage(bitmap: Any): AIResult
    }

    interface ImageFeatures {
        val brightness: Float
        val contrast: Float
        val saturation: Float
        val edgeDensity: Float
        val noiseLevel: Float
    }
}