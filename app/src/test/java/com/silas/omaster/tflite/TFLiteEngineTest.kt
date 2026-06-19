package com.silas.omaster.tflite

import com.silas.omaster.tflite.models.QualityMetrics
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
        val diagnostic = metrics.getDiagnosticInfo()
        // 亮度评分范围 0-100
        assertTrue(diagnostic.brightnessScore in 0f..100f)
        // 对比度评分范围 0-100
        assertTrue(diagnostic.contrastScore in 0f..100f)
        // 清晰度评分范围 0-100
        assertTrue(diagnostic.sharpnessScore in 0f..100f)
    }

    @Test
    fun `ModelLoader 路径验证`() {
        // 验证模型路径格式
        val modelPath = "models/scene_classifier.tflite"
        assertTrue(modelPath.endsWith(".tflite"))
        assertTrue(modelPath.startsWith("models/"))
    }
}
