package com.silas.omaster.tflite

import org.junit.Assert.*
import org.junit.Test

/**
 * TFLiteEngine 单元测试
 */
class TFLiteEngineTest {

    @Test
    fun `InferenceResult 默认值正确`() {
        val result = InferenceResult()
        assertNotNull(result)
    }

    @Test
    fun `QualityMetrics 范围验证`() {
        val metrics = QualityMetrics()
        // 亮度范围 0-100
        assertTrue(metrics.brightness in 0f..100f)
        // 对比度范围 0-100
        assertTrue(metrics.contrast in 0f..100f)
        // 饱和度范围 0-100
        assertTrue(metrics.saturation in 0f..100f)
    }

    @Test
    fun `ModelLoader 路径验证`() {
        // 验证模型路径格式
        val modelPath = "models/scene_classifier.tflite"
        assertTrue(modelPath.endsWith(".tflite"))
        assertTrue(modelPath.startsWith("models/"))
    }
}
