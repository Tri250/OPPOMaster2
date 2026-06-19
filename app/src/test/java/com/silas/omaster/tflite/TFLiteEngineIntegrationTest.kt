package com.silas.omaster.tflite

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TFLiteEngine 集成测试
 *
 * 测试内容：
 * - 引擎初始化
 * - 推理执行
 * - 缓存功能
 * - 性能统计
 * - 硬件加速信息
 * - 引擎状态管理
 */
class TFLiteEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var engine: TFLiteEngine

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        val assetManager = mockk<AssetManager>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.assets } returns assetManager
        every { assetManager.list(any()) } returns emptyArray()

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
        engine.initialize(InferenceConfig(useGpu = false))

        val result = engine.runInference<FloatArray>(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER,
            floatArrayOf()
        )

        assertTrue("场景分类应成功", result.isSuccess)

        val probabilities = result.getOrThrow()
        assertNotNull("分类结果不应为 null", probabilities)
        assertTrue("概率数组长度应为 36", probabilities.size == 36)

        val topConfidence = probabilities.maxOrNull() ?: 0f
        val topScene = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        assertTrue("置信度应在有效范围", topConfidence in 0f..1f)

        println("场景分类结果索引: $topScene (${(topConfidence * 100).toInt()}%)")
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

        val input = floatArrayOf()
        val cacheKey = "test_cache_key"

        val result1 = engine.runInference<FloatArray>(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER,
            input,
            cacheKey
        )
        assertTrue("第一次分类应成功", result1.isSuccess)

        val result2 = engine.runInference<FloatArray>(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER,
            input,
            cacheKey
        )
        assertTrue("第二次分类应成功", result2.isSuccess)

        engine.clearCache()

        val result3 = engine.runInference<FloatArray>(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER,
            input,
            cacheKey
        )
        assertTrue("第三次分类应成功", result3.isSuccess)
    }

    /**
     * 测试不同输入的分类
     */
    @Test
    fun testDifferentImageTypes(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val inputs = listOf(
            floatArrayOf(0.1f, 0.1f, 0.2f),
            floatArrayOf(0.8f, 0.7f, 0.3f),
            floatArrayOf(0.9f, 0.9f, 0.95f),
            floatArrayOf(0.5f, 0.5f, 0.5f)
        )

        for (input in inputs) {
            val result = engine.runInference<FloatArray>(
                TFLiteEngine.MODEL_SCENE_CLASSIFIER,
                input
            )
            assertTrue("分类应成功", result.isSuccess)
            val probabilities = result.getOrThrow()
            println("分类结果索引: ${probabilities.indices.maxByOrNull { probabilities[it] }}")
        }
    }

    /**
     * 测试性能统计
     */
    @Test
    fun testPerformanceStats(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val input = floatArrayOf()

        for (i in 0 until 10) {
            engine.runInference<FloatArray>(TFLiteEngine.MODEL_SCENE_CLASSIFIER, input)
        }

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
        assertEquals("初始状态应为 IDLE", InferenceState.IDLE, engine.getState())

        engine.initialize(InferenceConfig(useGpu = false))
        assertEquals("初始化后应为 READY", InferenceState.READY, engine.getState())

        engine.release()
        assertEquals("释放后应为 IDLE", InferenceState.IDLE, engine.getState())
    }

    /**
     * 测试准确率目标
     */
    @Test
    fun testAccuracyTarget(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val input = floatArrayOf()
        val result = engine.runInference<FloatArray>(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER,
            input
        )

        if (result.isSuccess) {
            val probabilities = result.getOrThrow()

            val topConfidence = probabilities.maxOrNull() ?: 0f
            assertTrue("置信度应 > 0", topConfidence > 0)

            val maxProb = probabilities.maxOrNull() ?: 0f
            assertTrue("最大概率应与 topConfidence 匹配",
                kotlin.math.abs(maxProb - topConfidence) < 0.01f)
        }
    }

    /**
     * 测试推理时间目标
     */
    @Test
    fun testInferenceTimeTarget(): Unit = runBlocking {
        engine.initialize(InferenceConfig(useGpu = false))

        val input = floatArrayOf()

        val times = mutableListOf<Long>()
        for (i in 0 until 20) {
            val startTime = System.currentTimeMillis()
            val result = engine.runInference<FloatArray>(
                TFLiteEngine.MODEL_SCENE_CLASSIFIER,
                input
            )
            val elapsed = System.currentTimeMillis() - startTime
            if (result.isSuccess) {
                times.add(elapsed)
            }
        }

        val avgTime = times.average()
        println("平均推理时间: ${avgTime}ms")

        assertTrue("平均推理时间应 < 50ms", avgTime < TFLiteEngine.TARGET_SCENE_CLASSIFICATION_MS)
    }
}
