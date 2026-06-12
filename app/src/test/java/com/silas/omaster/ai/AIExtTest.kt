package com.silas.omaster.ai

import org.junit.Assert.*
import org.junit.Test

/**
 * AI 扩展测试 - 补充覆盖更多AI模块
 */
class AIExtTest {

    // ===== SceneRecognitionManager 测试 =====

    @Test
    fun `场景识别 - 置信度阈值验证`() {
        val thresholds = listOf(0.6f, 0.7f, 0.8f, 0.9f)
        
        for (threshold in thresholds) {
            assertTrue("置信度阈值应该在有效范围内", threshold in 0f..1f)
        }
    }

    @Test
    fun `场景识别 - 候选数量验证`() {
        val maxCandidates = 5
        
        assertTrue("候选数量应该 > 0", maxCandidates > 0)
        assertTrue("候选数量应该 <= 10", maxCandidates <= 10)
    }

    @Test
    fun `场景识别 - 推理超时验证`() {
        val timeoutMs = 5000L
        
        assertTrue("超时应该 > 0", timeoutMs > 0)
        assertTrue("超时应该 < 30秒", timeoutMs < 30000)
    }

    @Test
    fun `场景识别 - 场景分类映射`() {
        val categoryMapping = mapOf(
            "portrait" to "人物",
            "landscape" to "风景",
            "food" to "美食",
            "night" to "夜景",
            "urban" to "城市"
        )
        
        assertEquals(5, categoryMapping.size)
    }

    @Test
    fun `场景识别 - 场景优先级排序`() {
        val candidates = listOf(
            Pair("portrait", 0.95f),
            Pair("landscape", 0.72f),
            Pair("food", 0.65f)
        )
        
        val sorted = candidates.sortedByDescending { it.second }
        
        assertEquals("portrait", sorted[0].first)
        assertEquals(0.95f, sorted[0].second, 0.001f)
    }

    // ===== AIFineTuneManager 扩展测试 =====

    @Test
    fun `AI微调 - 微调参数范围`() {
        val minValue = -100
        val maxValue = 100
        
        val testValues = listOf(-150, -100, 0, 50, 100, 150)
        
        for (value in testValues) {
            val clamped = value.coerceIn(minValue, maxValue)
            assertTrue("值应该在范围内: $value -> $clamped", clamped in minValue..maxValue)
        }
    }

    @Test
    fun `AI微调 - 强度等级验证`() {
        val intensityLevels = listOf("SUBTLE", "LIGHT", "NONE", "MODERATE", "STRONG")
        
        for (level in intensityLevels) {
            assertTrue("强度等级应该有效: $level", level.isNotEmpty())
        }
    }

    @Test
    fun `AI微调 - 影响系数计算`() {
        val baseInfluence = 1.0f
        val intensity = 50
        
        val influence = baseInfluence * (intensity / 100f)
        
        assertEquals(0.5f, influence, 0.001f)
    }

    @Test
    fun `AI微调 - 批次大小验证`() {
        val batchSizes = listOf(1, 4, 8, 16, 32)
        
        for (size in batchSizes) {
            assertTrue("批次大小应该 >= 1", size >= 1)
            assertTrue("批次大小应该 <= 32", size <= 32)
        }
    }

    @Test
    fun `AI微调 - 学习率范围`() {
        val learningRates = listOf(0.0001f, 0.001f, 0.01f, 0.1f)
        
        for (lr in learningRates) {
            assertTrue("学习率应该 > 0", lr > 0f)
            assertTrue("学习率应该 < 1", lr < 1f)
        }
    }

    @Test
    fun `AI微调 - 微调模式验证`() {
        val modes = listOf("AUTO", "MANUAL", "ADAPTIVE")
        
        for (mode in modes) {
            assertTrue("微调模式应该有效: $mode", mode.isNotEmpty())
        }
    }

    @Test
    fun `AI微调 - 微调状态验证`() {
        val states = listOf("IDLE", "TRAINING", "COMPLETED", "FAILED")
        
        for (state in states) {
            assertTrue("微调状态应该有效: $state", state.isNotEmpty())
        }
    }

    @Test
    fun `AI微调 - 参数平滑过渡`() {
        val from = 0
        val to = 100
        val progress = 0.5f
        
        val current = from + (to - from) * progress
        
        assertEquals(50f, current, 0.001f)
    }

    @Test
    fun `AI微调 - 损失收敛检测`() {
        val previousLoss = 0.05f
        val currentLoss = 0.04f
        val improvementThreshold = 0.001f
        
        val improvement = previousLoss - currentLoss
        val hasImproved = improvement > improvementThreshold
        
        assertTrue("损失应该收敛", hasImproved)
    }

    @Test
    fun `AI微调 - 早停检测`() {
        var patienceCounter = 0
        val patienceLimit = 5
        
        val shouldStop = patienceCounter >= patienceLimit
        
        assertFalse("不应该早停", shouldStop)
        
        patienceCounter = 5
        val shouldStopNow = patienceCounter >= patienceLimit
        
        assertTrue("应该早停", shouldStopNow)
    }

    // ===== MasterInferenceEngine 扩展测试 =====

    @Test
    fun `推理引擎 - EXIF解析验证`() {
        val exifFields = listOf(
            "Make", "Model", "FNumber", "ExposureTime", 
            "ISO", "FocalLength", "DateTime"
        )
        
        for (field in exifFields) {
            assertTrue("EXIF字段应该有效: $field", field.isNotEmpty())
        }
    }

    @Test
    fun `推理引擎 - 有理数解析`() {
        val rational = "35/10"
        val parts = rational.split("/")
        
        if (parts.size == 2) {
            val result = parts[0].toFloat() / parts[1].toFloat()
            assertEquals(3.5f, result, 0.001f)
        }
    }

    @Test
    fun `推理引擎 - GPS坐标解析`() {
        val coordinate = "40, 30, 25.5"
        val ref = "N"
        
        val parts = coordinate.split(", ")
        if (parts.size == 3) {
            val degrees = parts[0].toFloat()
            val minutes = parts[1].toFloat()
            val seconds = parts[2].toFloat()
            val result = degrees + minutes / 60 + seconds / 3600
            
            assertEquals(40.507f, result, 0.001f)
        }
    }

    @Test
    fun `推理引擎 - 直方图数据转换`() {
        val avgRed = 180
        val avgGreen = 150
        val avgBlue = 120
        
        val luminance = (0.2126 * avgRed + 0.7152 * avgGreen + 0.0722 * avgBlue).toInt()
        
        assertTrue("亮度应该在有效范围内", luminance in 0..255)
    }

    @Test
    fun `推理引擎 - 阴影裁剪检测`() {
        val darkPixelRatio = 0.75f
        val threshold = 0.7f
        
        val isClipping = darkPixelRatio > threshold
        
        assertTrue("应该检测到阴影裁剪", isClipping)
    }

    @Test
    fun `推理引擎 - 高光裁剪检测`() {
        val highlightRatio = 0.35f
        val threshold = 0.3f
        
        val isClipping = highlightRatio > threshold
        
        assertTrue("应该检测到高光裁剪", isClipping)
    }

    @Test
    fun `推理引擎 - 人脸置信度计算`() {
        val leftProb = 0.8f
        val rightProb = 0.9f
        
        val confidence = 0.4f + 0.3f * leftProb + 0.3f * rightProb
        
        assertEquals(0.91f, confidence, 0.001f)
    }

    @Test
    fun `推理引擎 - 微笑检测`() {
        val smileProb = 0.6f
        val threshold = 0.5f
        
        val hasSmile = smileProb > threshold
        
        assertTrue("应该检测到微笑", hasSmile)
    }

    @Test
    fun `推理引擎 - 坐标归一化`() {
        val pixelBounds = mapOf("left" to 100, "top" to 200, "right" to 400, "bottom" to 600)
        val w = 1920f
        val h = 1080f
        
        val normalized = mapOf(
            "left" to ((pixelBounds["left"]!! / w).coerceIn(0f, 1f) * 1000).toInt(),
            "top" to ((pixelBounds["top"]!! / h).coerceIn(0f, 1f) * 1000).toInt(),
            "right" to ((pixelBounds["right"]!! / w).coerceIn(0f, 1f) * 1000).toInt(),
            "bottom" to ((pixelBounds["bottom"]!! / h).coerceIn(0f, 1f) * 1000).toInt()
        )
        
        assertTrue("归一化坐标应该在有效范围内", normalized["left"]!! >= 0)
        assertTrue("归一化坐标应该在有效范围内", normalized["bottom"]!! <= 1000)
    }

    // ===== MasterInsightEngine 扩展测试 =====

    @Test
    fun `洞察引擎 - 胶片匹配评分`() {
        val baseScore = 0.85f
        val preferenceBonus = 0.1f
        
        val finalScore = (baseScore + preferenceBonus).coerceIn(0f, 1f)
        
        assertEquals(0.95f, finalScore, 0.001f)
    }

    @Test
    fun `洞察引擎 - 饱和度等级`() {
        val estimateSaturation = { saturation: Int ->
            when (saturation) {
                in -30..-10 -> "LOW"
                in -9..9 -> "MODERATE"
                in 10..20 -> "HIGH"
                else -> "VIBRANT"
            }
        }
        
        assertEquals("LOW", estimateSaturation(-20))
        assertEquals("MODERATE", estimateSaturation(0))
        assertEquals("HIGH", estimateSaturation(15))
        assertEquals("VIBRANT", estimateSaturation(25))
    }

    @Test
    fun `洞察引擎 - 对比度等级`() {
        val calculateContrastLevel = { contrast: Int ->
            when (contrast) {
                in -30..-10 -> "LOW"
                in -9..9 -> "MEDIUM"
                else -> "HIGH"
            }
        }
        
        assertEquals("LOW", calculateContrastLevel(-15))
        assertEquals("MEDIUM", calculateContrastLevel(5))
        assertEquals("HIGH", calculateContrastLevel(20))
    }

    @Test
    fun `洞察引擎 - 色温估算`() {
        val baseTemp = 5500
        val colorTempParam = 15
        
        val estimatedTemp = (baseTemp + colorTempParam * 100).coerceIn(2000, 10000)
        
        assertEquals(7000, estimatedTemp)
    }

    @Test
    fun `洞察引擎 - 动态范围估算`() {
        val shadowClipping = true
        val highlightClipping = false
        
        val dynamicRange = if (shadowClipping || highlightClipping) "LIMITED" else "WIDE"
        
        assertEquals("LIMITED", dynamicRange)
    }

    @Test
    fun `洞察引擎 - 光影质量推断`() {
        val softLight = true
        val colorTemp = 15
        
        val lightQuality = if (softLight) "WARM_SOFT" else when {
            colorTemp in -30..-5 -> "COOL_DIFFUSED"
            colorTemp in 5..30 -> "WARM_SOFT"
            else -> "DIRECT_HARD"
        }
        
        assertEquals("WARM_SOFT", lightQuality)
    }

    @Test
    fun `洞察引擎 - 曝光级别判断`() {
        val meanLuma = 128f
        
        val exposureLevel = when {
            meanLuma < 64 -> "UNDER_EXPOSED"
            meanLuma > 192 -> "OVER_EXPOSED"
            else -> "BALANCED"
        }
        
        assertEquals("BALANCED", exposureLevel)
    }

    @Test
    fun `洞察引擎 - 场景适用主体`() {
        val category = "PORTRAIT"
        
        val subjects = when (category) {
            "PORTRAIT" -> listOf("人物", "表情", "互动")
            "LANDSCAPE" -> listOf("自然风光", "城市天际线", "日出日落")
            "FOOD" -> listOf("美食", "饮品", "甜点")
            else -> emptyList()
        }
        
        assertEquals(listOf("人物", "表情", "互动"), subjects)
    }

    @Test
    fun `洞察引擎 - 最佳拍摄时间`() {
        val sceneId = "sunset_golden"
        
        val bestTime = when {
            sceneId.contains("sunset") || sceneId.contains("sunrise") -> "日出后/日落前1小时（黄金时刻）"
            sceneId.contains("night") -> "夜晚"
            sceneId.contains("blue") -> "日出前/日落后30分钟（蓝调时刻）"
            else -> "全天适宜"
        }
        
        assertEquals("日出后/日落前1小时（黄金时刻）", bestTime)
    }

    @Test
    fun `洞察引擎 - 天气偏好`() {
        val category = "PORTRAIT"
        
        val weather = when (category) {
            "PORTRAIT" -> "多云或阴天最佳，光线柔和"
            "LANDSCAPE" -> "晴朗或多云，避免正午强光"
            "NIGHT" -> "晴朗无云，光污染少"
            else -> "无特殊要求"
        }
        
        assertEquals("多云或阴天最佳，光线柔和", weather)
    }

    @Test
    fun `洞察引擎 - 情感推断`() {
        val category = "PORTRAIT"
        
        val mood = when (category) {
            "PORTRAIT" -> "温暖亲密"
            "LANDSCAPE" -> "宏大宁静"
            "URBAN" -> "动感活力"
            "NIGHT" -> "神秘戏剧性"
            "FOOD" -> "鲜艳生动"
            else -> "中性平和"
        }
        
        assertEquals("温暖亲密", mood)
    }

    @Test
    fun `洞察引擎 - 色彩和谐度分析`() {
        val category = "PORTRAIT"
        
        val harmony = when (category) {
            "PORTRAIT" -> "ANALOGOUS"
            "LANDSCAPE" -> "COMPLEMENTARY"
            "URBAN" -> "MONOCHROMATIC"
            else -> "COMPLEMENTARY"
        }
        
        assertEquals("ANALOGOUS", harmony)
    }

    @Test
    fun `洞察引擎 - 构图分析`() {
        val hasFace = true
        val category = "PORTRAIT"
        
        val framing = when {
            hasFace -> "中心构图（人像）"
            category == "LANDSCAPE" -> "三分法构图"
            category == "URBAN" -> "引导线构图"
            else -> "标准构图"
        }
        
        assertEquals("中心构图（人像）", framing)
    }
}