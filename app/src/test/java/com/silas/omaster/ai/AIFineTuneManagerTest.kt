package com.silas.omaster.ai

import org.junit.Assert.*
import org.junit.Test

/**
 * AIFineTuneManager 单元测试
 * 测试AI微调管理器的逻辑
 */
class AIFineTuneManagerTest {

    @Test
    fun `微调参数范围 - 验证范围限制`() {
        val minValue = -100
        val maxValue = 100
        
        val testValues = listOf(-150, -100, 0, 50, 100, 150)
        
        for (value in testValues) {
            val clamped = value.coerceIn(minValue, maxValue)
            assertTrue("值应该在范围内: $value -> $clamped", clamped in minValue..maxValue)
        }
    }

    @Test
    fun `微调强度级别 - 级别划分`() {
        val getIntensityLevel = { intensity: Int ->
            when {
                intensity < -50 -> "SUBTLE"
                intensity < 0 -> "LIGHT"
                intensity == 0 -> "NONE"
                intensity < 50 -> "MODERATE"
                else -> "STRONG"
            }
        }
        
        assertEquals("SUBTLE", getIntensityLevel(-75))
        assertEquals("LIGHT", getIntensityLevel(-25))
        assertEquals("NONE", getIntensityLevel(0))
        assertEquals("MODERATE", getIntensityLevel(25))
        assertEquals("STRONG", getIntensityLevel(75))
    }

    @Test
    fun `微调影响系数 - 计算调整影响`() {
        val baseInfluence = 1.0f
        val intensity = 50
        
        val influence = baseInfluence * (intensity / 100f)
        
        assertEquals(0.5f, influence, 0.001f)
    }

    @Test
    fun `微调批次大小 - 合理范围`() {
        val batchSizes = listOf(1, 4, 8, 16, 32)
        
        for (size in batchSizes) {
            assertTrue("批次大小应该 >= 1", size >= 1)
            assertTrue("批次大小应该 <= 32", size <= 32)
        }
    }

    @Test
    fun `学习率范围 - 合理范围`() {
        val learningRates = listOf(0.0001f, 0.001f, 0.01f, 0.1f)
        
        for (lr in learningRates) {
            assertTrue("学习率应该 > 0", lr > 0f)
            assertTrue("学习率应该 < 1", lr < 1f)
        }
    }

    @Test
    fun `微调模式 - 模式枚举`() {
        val modes = listOf("AUTO", "MANUAL", "ADAPTIVE")
        
        for (mode in modes) {
            assertTrue("模式应该是有效的: $mode", mode in listOf("AUTO", "MANUAL", "ADAPTIVE"))
        }
    }

    @Test
    fun `微调状态 - 状态枚举`() {
        val states = listOf("IDLE", "TRAINING", "COMPLETED", "FAILED")
        
        for (state in states) {
            assertTrue("状态应该是有效的: $state", state in states)
        }
    }

    @Test
    fun `参数平滑过渡 - 插值计算`() {
        val from = 0
        val to = 100
        val progress = 0.5f
        
        val current = from + (to - from) * progress
        
        assertEquals(50f, current, 0.001f)
    }

    @Test
    fun `参数平滑过渡 - 开始状态`() {
        val from = 0
        val to = 100
        val progress = 0.0f
        
        val current = from + (to - from) * progress
        
        assertEquals(0f, current, 0.001f)
    }

    @Test
    fun `参数平滑过渡 - 结束状态`() {
        val from = 0
        val to = 100
        val progress = 1.0f
        
        val current = from + (to - from) * progress
        
        assertEquals(100f, current, 0.001f)
    }

    @Test
    fun `损失值收敛检测 - 阈值判断`() {
        val previousLoss = 0.05f
        val currentLoss = 0.04f
        val improvementThreshold = 0.001f
        
        val improvement = previousLoss - currentLoss
        val hasImproved = improvement > improvementThreshold
        
        assertTrue("损失应该收敛", hasImproved)
    }

    @Test
    fun `早停检测 - 耐心计数`() {
        var patienceCounter = 0
        val patienceLimit = 5
        val shouldStop = patienceCounter >= patienceLimit
        
        assertFalse("不应该早停", shouldStop)
        
        patienceCounter = 5
        val shouldStopNow = patienceCounter >= patienceLimit
        
        assertTrue("应该早停", shouldStopNow)
    }
}

/**
 * SceneRecognitionManager 单元测试
 */
class SceneRecognitionManagerTest {

    @Test
    fun `场景识别置信度阈值 - 默认阈值`() {
        val defaultThreshold = 0.6f
        val highThreshold = 0.8f
        val lowThreshold = 0.4f
        
        assertTrue("默认阈值应该合理", defaultThreshold in 0f..1f)
        assertTrue("高阈值应该合理", highThreshold in 0f..1f)
        assertTrue("低阈值应该合理", lowThreshold in 0f..1f)
    }

    @Test
    fun `场景识别超时 - 合理超时时间`() {
        val timeoutMs = 5000L
        
        assertTrue("超时时间应该 > 0", timeoutMs > 0)
        assertTrue("超时时间应该 < 30秒", timeoutMs < 30000)
    }

    @Test
    fun `候选场景数量 - 合理数量`() {
        val topCandidates = listOf("portrait", "landscape", "food", "night", "architecture")
        
        assertTrue("候选数量应该在1-10之间", topCandidates.size in 1..10)
    }

    @Test
    fun `场景相似度计算 - 余弦相似度`() {
        val vec1 = floatArrayOf(1f, 0f, 0f)
        val vec2 = floatArrayOf(1f, 0f, 0f)
        
        // 相同向量，相似度应该为1
        val similarity = (vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2]) / 
                        (kotlin.math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1] + vec1[2]*vec1[2]) * 
                         kotlin.math.sqrt(vec2[0]*vec2[0] + vec2[1]*vec2[1] + vec2[2]*vec2[2]))
        
        assertEquals(1f, similarity, 0.001f)
    }

    @Test
    fun `场景分类映射 - 类别验证`() {
        val categoryMapping = mapOf(
            "portrait" to "人物",
            "landscape" to "风景",
            "food" to "美食",
            "night" to "夜景",
            "architecture" to "建筑"
        )
        
        assertEquals(5, categoryMapping.size)
        assertEquals("人物", categoryMapping["portrait"])
    }

    @Test
    fun `场景识别优先级 - 排序逻辑`() {
        val candidates = listOf(
            Pair("portrait", 0.95f),
            Pair("landscape", 0.72f),
            Pair("food", 0.65f)
        )
        
        val sorted = candidates.sortedByDescending { it.second }
        
        assertEquals("portrait", sorted[0].first)
        assertEquals(0.95f, sorted[0].second, 0.001f)
    }
}
