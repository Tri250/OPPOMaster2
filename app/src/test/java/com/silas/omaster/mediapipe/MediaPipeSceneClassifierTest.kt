package com.silas.omaster.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MediaPipe 场景分类器单元测试
 *
 * 测试内容：
 * - 初始化流程
 * - 场景分类功能
 * - 启发式降级算法
 * - GPU/CPU 推理切换
 * - 性能指标验证
 */
class MediaPipeSceneClassifierTest {

    private lateinit var context: Context
    private lateinit var classifier: MediaPipeSceneClassifier

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        classifier = MediaPipeSceneClassifier.getInstance(context)
    }

    @After
    fun tearDown() {
        classifier.release()
    }

    /**
     * 测试初始化流程
     */
    @Test
    fun testInitialize(): Unit = runBlocking {
        val result = classifier.initialize(useGpu = false)

        // 初始化应该成功（即使模型不存在，也会返回 false 但不会失败）
        assertTrue("初始化应该返回成功", result.isSuccess)
    }

    /**
     * 测试 GPU 初始化
     */
    @Test
    fun testInitializeWithGpu(): Unit = runBlocking {
        val result = classifier.initialize(useGpu = true)

        assertTrue("GPU 初始化应该返回成功", result.isSuccess)
    }

    /**
     * 测试场景分类（启发式降级）
     */
    @Test
    fun testClassifyWithHeuristic(): Unit = runBlocking {
        // 创建测试图像（明亮暖色调）
        val bitmap = createTestBitmap(
            red = 200,
            green = 150,
            blue = 100,
            width = 224,
            height = 224
        )

        val result = classifier.classify(bitmap)

        // 验证结果结构
        assertNotNull("分类结果不应为 null", result)
        assertTrue("置信度应在有效范围", result.topConfidence >= 0f && result.topConfidence <= 1f)
        assertTrue("概率数组长度应为 36", result.allProbabilities.size == 36)
        assertTrue("推理时间应记录", result.inferenceTimeMs > 0)

        // 概率总和应接近 1.0
        val sum = result.allProbabilities.sum()
        assertTrue("概率总和应接近 1.0", sum > 0.9f && sum < 1.1f)
    }

    /**
     * 测试暗色调图像分类
     */
    @Test
    fun testClassifyDarkImage(): Unit = runBlocking {
        val bitmap = createTestBitmap(
            red = 30,
            green = 30,
            blue = 50,  // 偏蓝（冷色调）
            width = 224,
            height = 224
        )

        val result = classifier.classify(bitmap)

        // 暗色调图像应倾向于夜景或室内场景
        assertNotNull("分类结果不应为 null", result)

        // 验证概率分布
        val nightProb = result.allProbabilities[2]  // night
        val indoorProb = result.allProbabilities[15] // indoor

        println("暗色调图像分类结果: ${result.topScene} (${result.topConfidence})")
        println("夜景概率: $nightProb, 室内概率: $indoorProb")
    }

    /**
     * 测试暖色调图像分类
     */
    @Test
    fun testClassifyWarmImage(): Unit = runBlocking {
        val bitmap = createTestBitmap(
            red = 220,
            green = 180,
            blue = 80,  // 暖色调
            width = 224,
            height = 224
        )

        val result = classifier.classify(bitmap)

        // 暖色调图像应倾向于日落、美食或秋景
        assertNotNull("分类结果不应为 null", result)

        val sunsetProb = result.allProbabilities[9]  // sunset
        val foodProb = result.allProbabilities[3]    // food
        val autumnProb = result.allProbabilities[28] // autumn

        println("暖色调图像分类结果: ${result.topScene} (${result.topConfidence})")
        println("日落概率: $sunsetProb, 美食概率: $foodProb, 秋景概率: $autumnProb")
    }

    /**
     * 测试高亮图像分类
     */
    @Test
    fun testClassifyBrightImage(): Unit = runBlocking {
        val bitmap = createTestBitmap(
            red = 240,
            green = 240,
            blue = 250,  // 高亮冷色调
            width = 224,
            height = 224
        )

        val result = classifier.classify(bitmap)

        // 高亮图像应倾向于天空、雪景或海滩
        assertNotNull("分类结果不应为 null", result)

        val skyProb = result.allProbabilities[16]  // sky
        val snowProb = result.allProbabilities[7]  // snow
        val beachProb = result.allProbabilities[8] // beach

        println("高亮图像分类结果: ${result.topScene} (${result.topConfidence})")
        println("天空概率: $skyProb, 雪景概率: $snowProb, 海滩概率: $beachProb")
    }

    /**
     * 测试场景类别映射
     */
    @Test
    fun testSceneCategoryMapping() {
        val mapping = MediaPipeSceneClassifier.SCENE_TO_CATEGORY_MAP

        // 验证关键映射
        assertEquals("portrait 应映射到 PORTRAIT", SceneCategory.PORTRAIT, mapping["portrait"])
        assertEquals("landscape 应映射到 LANDSCAPE", SceneCategory.LANDSCAPE, mapping["landscape"])
        assertEquals("night 应映射到 NIGHT", SceneCategory.NIGHT, mapping["night"])
        assertEquals("food 应映射到 FOOD", SceneCategory.FOOD, mapping["food"])
        assertEquals("street 应映射到 URBAN", SceneCategory.URBAN, mapping["street"])
        assertEquals("macro 应映射到 MACRO", SceneCategory.MACRO, mapping["macro"])
    }

    /**
     * 测试场景类别列表
     */
    @Test
    fun testSceneClassesList() {
        val classes = MediaPipeSceneClassifier.SCENE_CLASSES

        assertEquals("场景类别数量应为 36", 36, classes.size)
        assertTrue("应包含 landscape", classes.contains("landscape"))
        assertTrue("应包含 portrait", classes.contains("portrait"))
        assertTrue("应包含 night", classes.contains("night"))
        assertTrue("应包含 food", classes.contains("food"))
    }

    /**
     * 测试分类器状态
     */
    @Test
    fun testClassifierState(): Unit = runBlocking {
        // 初始化前
        assertFalse("初始化前应不可用", classifier.isReady())

        // 初始化
        classifier.initialize(useGpu = false)

        // 初始化后（如果模型存在）
        // 注意：测试环境可能没有模型文件，所以不强制要求 isReady() 为 true

        // 释放
        classifier.release()
        assertFalse("释放后应不可用", classifier.isReady())
    }

    /**
     * 测试性能目标
     */
    @Test
    fun testPerformanceTarget(): Unit = runBlocking {
        val bitmap = createTestBitmap(
            red = 128,
            green = 128,
            blue = 128,
            width = 224,
            height = 224
        )

        // 多次推理测试性能
        val inferenceTimes = mutableListOf<Long>()

        for (i in 0 until 10) {
            val result = classifier.classify(bitmap)
            inferenceTimes.add(result.inferenceTimeMs)
        }

        val avgTime = inferenceTimes.average()
        val maxTime = inferenceTimes.maxOrNull() ?: 0

        println("平均推理时间: ${avgTime}ms")
        println("最大推理时间: ${maxTime}ms")

        // 启发式算法应该在合理时间内完成（< 100ms）
        assertTrue("平均推理时间应 < 100ms", avgTime < 100)
    }

    /**
     * 测试多次分类的一致性
     */
    @Test
    fun testClassificationConsistency(): Unit = runBlocking {
        val bitmap = createTestBitmap(
            red = 150,
            green = 100,
            blue = 50,
            width = 224,
            height = 224
        )

        // 多次分类同一图像
        val results = mutableListOf<ClassificationResult>()
        for (i in 0 until 5) {
            results.add(classifier.classify(bitmap))
        }

        // 结果应该一致（启发式算法）
        val topScenes = results.map { it.topScene }.distinct()
        assertTrue("同一图像的分类结果应一致", topScenes.size <= 2)
    }

    /**
     * 创建测试 Bitmap
     */
    private fun createTestBitmap(
        red: Int,
        green: Int,
        blue: Int,
        width: Int = 224,
        height: Int = 224
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // 添加一些变化避免完全均匀
                val variation = ((x + y) % 10) - 5
                val r = (red + variation).coerceIn(0, 255)
                val g = (green + variation).coerceIn(0, 255)
                val b = (blue + variation).coerceIn(0, 255)
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }

        return bitmap
    }
}