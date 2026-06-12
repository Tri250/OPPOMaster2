package com.silas.omaster.tflite

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
 * TFLiteEngine 集成测试
 *
 * 测试内容：
 * - MediaPipe 集成初始化
 * - 场景分类功能
 * - 推理策略切换
 * - 性能验证
 */
class TFLiteEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var engine: TFLiteEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        engine = TFLiteEngine.getInstance(context)
    }

    @After
    fun tearDown() {
        engine.release()
    }

    /**
     * 测试引擎初始化
     */
    @Test
    fun testEngineInitialize(): Unit = runBlocking {
        val config = InferenceConfig(
            useGpu = false,
            useNnapi = false,
            useXnnpack = true,
            enableCache = true
        )

        val result = engine.initialize(config)

        assertTrue("引擎初始化应成功", result.isSuccess)
        assertEquals("引擎状态应为 READY", InferenceState.READY, engine.getState())
    }

    /**
     * 测试场景分类 API
     */
    @Test
    fun testClassifyScene(): Unit = runBlocking {
        // 初始化引擎
        engine.initialize(InferenceConfig(useGpu = false))

        // 创建测试图像
        val bitmap = createTestBitmap(150, 100, 50, 224, 224)

        // 执行场景分类
        val result = engine.classifyScene(bitmap)

        assertTrue("场景分类应成功", result.isSuccess)

        val classification = result.getOrThrow()
        assertNotNull("分类结果不应为 null", classification)
        assertTrue("置信度应在有效范围", classification.topConfidence >= 0f && classification.topConfidence <= 1f)
        assertTrue("概率数组长度应为 36", classification.allProbabilities.size == 36)
        assertTrue("推理时间应记录", classification.inferenceTimeMs >= 0)

        println("场景分类结果: ${classification.topScene} (${(classification.topConfidence * 100).toInt()}%)")
        println("推理时间: ${classification.inferenceTimeMs}ms")
        println("使用 GPU: ${classification.usedGpu}")
    }

    /**
     * 测试缓存功能
     */
    @Test
    fun testCacheFunctionality(): Unit = runBlocking {
        val config = InferenceConfig(
            useGpu = false,
            enableCache = true,
            cacheSize = 10
        )

        engine.initialize(config)

        val bitmap = createTestBitmap(128, 128, 128, 224, 224)
        val cacheKey = "test_cache_key"

        // 第一次分类
        val result1 = engine.classifyScene(bitmap, cacheKey)
        assertTrue("第一次分类应成功", result1.isSuccess)

        // 第二次分类（应使用缓存）
        val result2 = engine.classifyScene(bitmap, cacheKey)
        assertTrue("第二次分类应成功", result2.isSuccess)

        // 清除缓存
        engine.clearCache()

        // 第三次分类（缓存已清除）
        val result3 = engine.classifyScene(bitmap, cacheKey)
        assertTrue("第三次分类应成功", result3.isSuccess)
    }

    /**
     * 测试不同图像类型的分类
     */
    @Test
    fun testDifferentImageTypes(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        // 暗色调图像
        val darkBitmap = createTestBitmap(30, 30, 50, 224, 224)
        val darkResult = engine.classifyScene(darkBitmap)
        assertTrue("暗色调分类应成功", darkResult.isSuccess)
        println("暗色调: ${darkResult.getOrThrow().topScene}")

        // 暖色调图像
        val warmBitmap = createTestBitmap(220, 180, 80, 224, 224)
        val warmResult = engine.classifyScene(warmBitmap)
        assertTrue("暖色调分类应成功", warmResult.isSuccess)
        println("暖色调: ${warmResult.getOrThrow().topScene}")

        // 高亮图像
        val brightBitmap = createTestBitmap(240, 240, 250, 224, 224)
        val brightResult = engine.classifyScene(brightBitmap)
        assertTrue("高亮分类应成功", brightResult.isSuccess)
        println("高亮: ${brightResult.getOrThrow().topScene}")

        // 正常亮度图像
        val normalBitmap = createTestBitmap(128, 128, 128, 224, 224)
        val normalResult = engine.classifyScene(normalBitmap)
        assertTrue("正常亮度分类应成功", normalResult.isSuccess)
        println("正常: ${normalResult.getOrThrow().topScene}")
    }

    /**
     * 测试性能统计
     */
    @Test
    fun testPerformanceStats(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val bitmap = createTestBitmap(128, 128, 128, 224, 224)

        // 执行多次推理
        for (i in 0 until 10) {
            engine.classifyScene(bitmap)
        }

        // 获取性能统计
        val stats = engine.getPerformanceStats(TFLiteEngine.MODEL_SCENE_CLASSIFIER)

        if (stats != null) {
            println("总推理次数: ${stats.totalInferences}")
            println("平均推理时间: ${stats.averageTimeMs}ms")
            println("最小推理时间: ${stats.minTimeMs}ms")
            println("最大推理时间: ${stats.maxTimeMs}ms")

            assertTrue("总推理次数应 >= 10", stats.totalInferences >= 10)
        }
    }

    /**
     * 测试硬件加速信息
     */
    @Test
    fun testHardwareAccelerationInfo() {
        val info = engine.getHardwareAccelerationInfo()

        assertNotNull("硬件加速信息不应为 null", info)
        assertTrue("应包含 GPU 信息", info.containsKey("GPU"))
        assertTrue("应包含 NNAPI 信息", info.containsKey("NNAPI"))
        assertTrue("应包含 XNNPACK 信息", info.containsKey("XNNPACK"))

        println("GPU 支持: ${info["GPU"]}")
        println("NNAPI 支持: ${info["NNAPI"]}")
        println("XNNPACK 支持: ${info["XNNPACK"]}")
    }

    /**
     * 测试引擎状态
     */
    @Test
    fun testEngineState(): Unit = runBlocking {
        // 初始化前
        assertEquals("初始状态应为 IDLE", InferenceState.IDLE, engine.getState())

        // 初始化
        engine.initialize(InferenceConfig(useGpu = false))
        assertEquals("初始化后应为 READY", InferenceState.READY, engine.getState())

        // 释放
        engine.release()
        assertEquals("释放后应为 IDLE", InferenceState.IDLE, engine.getState())
    }

    /**
     * 测试准确率目标
     */
    @Test
    fun testAccuracyTarget(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val bitmap = createTestBitmap(128, 128, 128, 224, 224)
        val result = engine.classifyScene(bitmap)

        if (result.isSuccess) {
            val classification = result.getOrThrow()

            // 验证置信度在合理范围
            assertTrue("置信度应 > 0", classification.topConfidence > 0)

            // 验证概率分布合理
            val maxProb = classification.allProbabilities.maxOrNull() ?: 0f
            assertTrue("最大概率应与 topConfidence 匹配",
                Math.abs(maxProb - classification.topConfidence) < 0.01f)
        }
    }

    /**
     * 测试推理时间目标
     */
    @Test
    fun testInferenceTimeTarget(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val bitmap = createTestBitmap(128, 128, 128, 224, 224)

        // 执行多次推理取平均
        val times = mutableListOf<Long>()
        for (i in 0 until 20) {
            val result = engine.classifyScene(bitmap)
            if (result.isSuccess) {
                times.add(result.getOrThrow().inferenceTimeMs)
            }
        }

        val avgTime = times.average()
        println("平均推理时间: ${avgTime}ms")

        // 目标：场景分类 < 50ms
        assertTrue("平均推理时间应 < 50ms", avgTime < TFLiteEngine.TARGET_SCENE_CLASSIFICATION_MS)
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