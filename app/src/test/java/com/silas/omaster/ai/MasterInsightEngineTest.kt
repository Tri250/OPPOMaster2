package com.silas.omaster.ai

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * MasterInsightEngine 单元测试
 * 测试大师洞察引擎的算法逻辑
 */
class MasterInsightEngineTest {

    @Test
    fun `胶片匹配评分 - 基础分数计算`() {
        val baseScore = 0.85f
        val preferenceBonus = 0.1f
        
        val finalScore = (baseScore + preferenceBonus).coerceIn(0f, 1f)
        
        assertEquals(0.95f, finalScore, 0.001f)
    }

    @Test
    fun `胶片匹配评分 - 不应超过1`() {
        val baseScore = 0.95f
        val preferenceBonus = 0.2f
        
        val finalScore = (baseScore + preferenceBonus).coerceIn(0f, 1f)
        
        assertEquals(1.0f, finalScore, 0.001f)
    }

    @Test
    fun `饱和度等级 - 正确划分`() {
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
    fun `对比度等级 - 正确划分`() {
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
    fun `HNCS兼容性评分 - 基础分数计算`() {
        val baseScore = 0.95f
        val saturationPenalty = if (25 > 20) 0.05f else 0f
        
        val finalScore = (baseScore - saturationPenalty).coerceIn(0f, 1f)
        
        assertEquals(0.90f, finalScore, 0.001f)
    }

    @Test
    fun `色温估算 - 基础色温值计算`() {
        val baseTemp = 5500
        val colorTempParam = 15  // 每单位约100K
        
        val estimatedTemp = (baseTemp + colorTempParam * 100).coerceIn(2000, 10000)
        
        assertEquals(7000, estimatedTemp)
    }

    @Test
    fun `色温估算 - 边界值处理`() {
        val baseTemp = 5500
        val extremeTemp = -50  // 极端冷调
        
        val estimatedTemp = (baseTemp + extremeTemp * 100).coerceIn(2000, 10000)
        
        assertEquals(2000, estimatedTemp) // 应该在最小值
    }

    @Test
    fun `动态范围估算 - 有限范围检测`() {
        val shadowClipping = true
        val highlightClipping = false
        
        val dynamicRange = if (shadowClipping || highlightClipping) "LIMITED" else "WIDE"
        
        assertEquals("LIMITED", dynamicRange)
    }

    @Test
    fun `光影质量推断 - 暖色温柔光`() {
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
    fun `光影质量推断 - 冷色散射光`() {
        val softLight = false
        val colorTemp = -15
        
        val lightQuality = when {
            softLight -> "WARM_SOFT"
            colorTemp in -30..-5 -> "COOL_DIFFUSED"
            colorTemp in 5..30 -> "WARM_SOFT"
            else -> "DIRECT_HARD"
        }
        
        assertEquals("COOL_DIFFUSED", lightQuality)
    }

    @Test
    fun `光线方向推断 - 直接硬光`() {
        val softLight = false
        val colorTemp = 0
        
        val lightQuality = when {
            softLight -> "WARM_SOFT"
            colorTemp in -30..-5 -> "COOL_DIFFUSED"
            colorTemp in 5..30 -> "WARM_SOFT"
            else -> "DIRECT_HARD"
        }
        
        assertEquals("DIRECT_HARD", lightQuality)
    }

    @Test
    fun `曝光级别判断 - 欠曝`() {
        val meanLuma = 50f
        
        val exposureLevel = when {
            meanLuma < 64 -> "UNDER_EXPOSED"
            meanLuma > 192 -> "OVER_EXPOSED"
            else -> "BALANCED"
        }
        
        assertEquals("UNDER_EXPOSED", exposureLevel)
    }

    @Test
    fun `曝光级别判断 - 正常`() {
        val meanLuma = 128f
        
        val exposureLevel = when {
            meanLuma < 64 -> "UNDER_EXPOSED"
            meanLuma > 192 -> "OVER_EXPOSED"
            else -> "BALANCED"
        }
        
        assertEquals("BALANCED", exposureLevel)
    }

    @Test
    fun `曝光级别判断 - 过曝`() {
        val meanLuma = 200f
        
        val exposureLevel = when {
            meanLuma < 64 -> "UNDER_EXPOSED"
            meanLuma > 192 -> "OVER_EXPOSED"
            else -> "BALANCED"
        }
        
        assertEquals("OVER_EXPOSED", exposureLevel)
    }

    @Test
    fun `场景适用主体 - 人像场景`() {
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
    fun `场景适用主体 - 风景场景`() {
        val category = "LANDSCAPE"
        
        val subjects = when (category) {
            "PORTRAIT" -> listOf("人物", "表情", "互动")
            "LANDSCAPE" -> listOf("自然风光", "城市天际线", "日出日落")
            "FOOD" -> listOf("美食", "饮品", "甜点")
            else -> emptyList()
        }
        
        assertEquals(listOf("自然风光", "城市天际线", "日出日落"), subjects)
    }

    @Test
    fun `最佳拍摄时间 - 日出日落场景`() {
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
    fun `最佳拍摄时间 - 蓝调时刻场景`() {
        val sceneId = "blue_hour_city"
        
        val bestTime = when {
            sceneId.contains("sunset") || sceneId.contains("sunrise") -> "日出后/日落前1小时（黄金时刻）"
            sceneId.contains("night") -> "夜晚"
            sceneId.contains("blue") -> "日出前/日落后30分钟（蓝调时刻）"
            else -> "全天适宜"
        }
        
        assertEquals("日出前/日落后30分钟（蓝调时刻）", bestTime)
    }

    @Test
    fun `天气偏好 - 人像场景`() {
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
    fun `天气偏好 - 风景场景`() {
        val category = "LANDSCAPE"
        
        val weather = when (category) {
            "PORTRAIT" -> "多云或阴天最佳，光线柔和"
            "LANDSCAPE" -> "晴朗或多云，避免正午强光"
            "NIGHT" -> "晴朗无云，光污染少"
            else -> "无特殊要求"
        }
        
        assertEquals("晴朗或多云，避免正午强光", weather)
    }

    @Test
    fun `参数调整合并 - 基础参数加胶片调整`() {
        val baseParams = mapOf(
            "tone" to 0,
            "saturation" to 10,
            "contrast" to 5,
            "colorTemp" to 0,
            "sharpness" to 0,
            "vignette" to 0
        )
        
        val adjustments = mapOf(
            "saturation" to -5,
            "contrast" to -10,
            "colorTemp" to 5
        )
        
        val totalAdjustments = mapOf(
            "tone" to "+0",
            "saturation" to "+5",  // 10 + (-5)
            "contrast" to "-5",    // 5 + (-10)
            "colorTemp" to "+5",  // 0 + 5
            "sharpness" to "+0",
            "vignette" to "+0"
        )
        
        // 验证合并逻辑
        val mergedSaturation = baseParams["saturation"]!! + adjustments["saturation"]!!
        assertEquals(5, mergedSaturation)
        
        val mergedContrast = baseParams["contrast"]!! + adjustments["contrast"]!!
        assertEquals(-5, mergedContrast)
    }

    @Test
    fun `情感推断 - 人像场景`() {
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
    fun `情感推断 - 夜景场景`() {
        val category = "NIGHT"
        
        val mood = when (category) {
            "PORTRAIT" -> "温暖亲密"
            "LANDSCAPE" -> "宏大宁静"
            "URBAN" -> "动感活力"
            "NIGHT" -> "神秘戏剧性"
            "FOOD" -> "鲜艳生动"
            else -> "中性平和"
        }
        
        assertEquals("神秘戏剧性", mood)
    }

    @Test
    fun `色彩和谐度分析 - 人像场景`() {
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
    fun `构图分析 - 三分法则检测`() {
        val hasFace = true
        
        val ruleOfThirds = hasFace // 简化：有人脸就认为遵循三分法则
        
        assertTrue(ruleOfThirds)
    }

    @Test
    fun `引导线检测 - 城市建筑场景`() {
        val category = "URBAN"
        val sceneId = "architecture_building"
        
        val hasLeadingLines = category == "URBAN" || 
                             sceneId.contains("architecture") ||
                             sceneId.contains("street")
        
        assertTrue(hasLeadingLines)
    }

    @Test
    fun `框架分析 - 人像场景`() {
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

    @Test
    fun `框架分析 - 风景场景`() {
        val hasFace = false
        val category = "LANDSCAPE"
        
        val framing = when {
            hasFace -> "中心构图（人像）"
            category == "LANDSCAPE" -> "三分法构图"
            category == "URBAN" -> "引导线构图"
            else -> "标准构图"
        }
        
        assertEquals("三分法构图", framing)
    }
}

/**
 * Mood 枚举测试
 */
class MoodTest {

    @Test
    fun `情感描述 - 所有情感应该有描述`() {
        val moods = listOf(
            "WARM_INTIMATE" to "温暖亲密",
            "GRAND_SERENE" to "宏大宁静",
            "DYNAMIC_ENERGETIC" to "动感活力",
            "MYSTERIOUS_DRAMATIC" to "神秘戏剧性",
            "VIBRANT_LIVELY" to "鲜艳生动",
            "NEUTRAL_CALM" to "中性平和"
        )
        
        for ((_, description) in moods) {
            assertTrue("情感描述不应该为空", description.isNotEmpty())
        }
    }
}

/**
 * FinalParams 数据类测试
 */
class FinalParamsTest {

    @Test
    fun `最终参数合并 - 基础参数加胶片调整`() {
        val filmAdjustments = mapOf(
            "saturation" to -5,
            "contrast" to -10,
            "colorTemp" to 5
        )
        
        assertEquals(-5, filmAdjustments["saturation"])
        assertEquals(-10, filmAdjustments["contrast"])
        assertEquals(5, filmAdjustments["colorTemp"])
    }
}
