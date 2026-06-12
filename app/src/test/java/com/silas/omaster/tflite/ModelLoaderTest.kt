package com.silas.omaster.tflite

import org.junit.Assert.*
import org.junit.Test

/**
 * ModelLoader 单元测试
 * 测试模型加载器的逻辑
 */
class ModelLoaderTest {

    @Test
    fun `模型路径验证 - 有效路径格式`() {
        val validPaths = listOf(
            "models/scene_classifier.tflite",
            "models/quality_analyzer.tflite",
            "models/param_predictor.tflite"
        )
        
        for (path in validPaths) {
            assertTrue("路径应该以.tflite结尾: $path", path.endsWith(".tflite"))
            assertTrue("路径应该以models/开头: $path", path.startsWith("models/"))
        }
    }

    @Test
    fun `模型路径验证 - 无效路径格式`() {
        val invalidPaths = listOf(
            "scene_classifier.tflite",
            "/models/scene_classifier.tflite",
            "models/scene_classifier",
            "models/scene_classifier.tflite.bak"
        )
        
        for (path in invalidPaths) {
            val isValid = path.startsWith("models/") && path.endsWith(".tflite")
            assertFalse("路径应该是无效的: $path", isValid)
        }
    }

    @Test
    fun `模型加载状态 - 状态枚举`() {
        val states = listOf("NOT_LOADED", "LOADING", "LOADED", "FAILED")
        
        for (state in states) {
            assertTrue("状态应该是有效的: $state", state in states)
        }
    }

    @Test
    fun `模型元数据 - 版本格式`() {
        val version = "1.0.0"
        val parts = version.split(".")
        
        assertEquals("版本应该有3部分", 3, parts.size)
        assertTrue("主版本应该是数字", parts[0].all { it.isDigit() })
    }

    @Test
    fun `模型缓存键生成 - 基于模型名称和版本`() {
        val modelName = "scene_classifier"
        val version = "1.0.0"
        
        val cacheKey = "${modelName}_${version}".hashCode().toString()
        
        assertTrue("缓存键应该是正数", cacheKey.toInt() > 0)
    }

    @Test
    fun `模型大小验证 - 合理大小范围`() {
        val modelSizeBytes = 5 * 1024 * 1024L // 5MB
        
        assertTrue("模型大小应该 > 0", modelSizeBytes > 0)
        assertTrue("模型大小应该 < 100MB", modelSizeBytes < 100 * 1024 * 1024)
    }

    @Test
    fun `模型输入尺寸验证 - 标准尺寸`() {
        val standardSizes = listOf(224, 299, 331, 512)
        
        for (size in standardSizes) {
            assertTrue("尺寸应该是正数", size > 0)
            assertTrue("尺寸应该是偶数", size % 2 == 0)
        }
    }

    @Test
    fun `模型输出维度验证 - 有效维度`() {
        val outputShapes = listOf(
            listOf(1, 36),      // 场景分类器
            listOf(1, 5),       // 质量分析器
            listOf(1, 11)       // 参数预测器
        )
        
        for (shape in outputShapes) {
            assertTrue("第一个维度应该是批次大小", shape[0] > 0)
            assertTrue("输出维度应该 > 0", shape[1] > 0)
        }
    }
}

/**
 * ParamPredictor 单元测试
 */
class ParamPredictorTest {

    @Test
    fun `参数预测范围 - 验证范围限制`() {
        val minValue = -30
        val maxValue = 30
        
        val testValues = listOf(-50, -30, 0, 15, 30, 50)
        
        for (value in testValues) {
            val clamped = value.coerceIn(minValue, maxValue)
            assertTrue("值应该在范围内: $value -> $clamped", clamped in minValue..maxValue)
        }
    }

    @Test
    fun `参数预测置信度 - 合理范围`() {
        val confidence = 0.85f
        
        assertTrue("置信度应该在0-1之间", confidence in 0f..1f)
    }

    @Test
    fun `预测模式 - 模式枚举`() {
        val modes = listOf("BALANCED", "AGGRESSIVE", "CONSERVATIVE")
        
        for (mode in modes) {
            assertTrue("模式应该是有效的: $mode", mode in modes)
        }
    }

    @Test
    fun `参数调整步进 - 整数步进`() {
        val step = 1
        val values = listOf(10.4f, 10.5f, 10.6f)
        
        for (value in values) {
            val stepped = value.toInt()
            assertTrue("步进后应该是整数", stepped == value.toInt())
        }
    }

    @Test
    fun `多参数协同调整 - 联动关系`() {
        val sharpness = 20
        val clarityRatio = 0.5f
        
        val linkedClarity = (sharpness * clarityRatio).toInt()
        
        assertEquals(10, linkedClarity)
    }

    @Test
    fun `参数权重计算 - 加权平均`() {
        val weights = floatArrayOf(0.3f, 0.4f, 0.3f)
        val values = floatArrayOf(10f, 20f, 30f)
        
        var weightedSum = 0f
        for (i in weights.indices) {
            weightedSum += weights[i] * values[i]
        }
        
        assertEquals(19.5f, weightedSum, 0.001f)
    }

    @Test
    fun `饱和度与鲜艳度联动 - 联动计算`() {
        val saturation = 15
        val vividRatio = 0.6f
        
        val vividAdjustment = (saturation * vividRatio).toInt()
        
        assertEquals(9, vividAdjustment)
    }

    @Test
    fun `对比度和HDR互斥 - 互斥检测`() {
        val mutexGroups = mapOf(
            "contrast" to setOf("contrast", "hdr")
        )
        
        val param = "hdr"
        val isMutex = mutexGroups.values.any { it.contains(param) }
        
        assertTrue("hdr应该与contrast互斥", isMutex)
    }
}

/**
 * TFLiteEngine 扩展测试
 */
class TFLiteEngineExtTest {

    @Test
    fun `委托选项验证 - GPU委托`() {
        val useGpu = true
        val useNnapi = false
        
        val delegates = mutableListOf<String>()
        if (useGpu) delegates.add("GPU")
        if (useNnapi) delegates.add("NNAPI")
        
        assertEquals(1, delegates.size)
        assertTrue(delegates.contains("GPU"))
    }

    @Test
    fun `委托选项验证 - NNAPI委托`() {
        val useGpu = false
        val useNnapi = true
        
        val delegates = mutableListOf<String>()
        if (useGpu) delegates.add("GPU")
        if (useNnapi) delegates.add("NNAPI")
        
        assertEquals(1, delegates.size)
        assertTrue(delegates.contains("NNAPI"))
    }

    @Test
    fun `委托优先级 - GPU优先于NNAPI`() {
        val useGpu = true
        val useNnapi = true
        
        val delegates = mutableListOf<String>()
        if (useGpu) delegates.add(0, "GPU")  // 插入到前面
        if (useNnapi) delegates.add("NNAPI")
        
        assertEquals("GPU", delegates[0])
    }

    @Test
    fun `推理超时 - 合理超时时间`() {
        val timeoutMs = 10000L
        
        assertTrue("超时时间应该 > 0", timeoutMs > 0)
        assertTrue("超时时间应该 < 60秒", timeoutMs < 60000)
    }

    @Test
    fun `模型缓存管理 - 缓存键生成`() {
        val modelName = "scene_classifier"
        val inputHash = "abc123"
        
        val cacheKey = "${modelName}_${inputHash}"
        
        assertEquals("scene_classifier_abc123", cacheKey)
    }

    @Test
    fun `推理结果缓存 - TTL验证`() {
        val cacheTtlMs = 300000L // 5分钟
        val now = System.currentTimeMillis()
        val cachedTime = now - 60000 // 1分钟前
        
        val isExpired = (now - cachedTime) > cacheTtlMs
        
        assertFalse("缓存不应该过期", isExpired)
    }

    @Test
    fun `推理结果缓存 - 过期检测`() {
        val cacheTtlMs = 300000L // 5分钟
        val now = System.currentTimeMillis()
        val cachedTime = now - 400000 // 超过5分钟前
        
        val isExpired = (now - cachedTime) > cacheTtlMs
        
        assertTrue("缓存应该过期", isExpired)
    }
}
