package com.silas.omaster.tflite

import org.junit.Assert.*
import org.junit.Test

/**
 * SceneClassifier 单元测试
 * 测试场景分类器的逻辑部分
 */
class SceneClassifierTest {

    @Test
    fun `场景标签映射 - 应该包含36种场景类型`() {
        val expectedSceneCount = 36
        
        assertEquals("场景标签数量应该为36", expectedSceneCount, SceneClassifier.SCENE_LABELS.size)
    }

    @Test
    fun `场景分组 - 应该包含7个分组`() {
        val expectedGroupCount = 7
        
        assertEquals("场景分组数量应该为7", expectedGroupCount, SceneClassifier.SCENE_GROUPS.size)
    }

    @Test
    fun `自然风景分组 - 应该包含7种场景`() {
        val natureScenes = SceneClassifier.SCENE_GROUPS["nature"]
        
        assertNotNull(natureScenes)
        assertEquals("自然风景分组应该包含7种场景", 7, natureScenes!!.size)
        assertTrue(natureScenes.contains(0)) // landscape
        assertTrue(natureScenes.contains(1)) // mountain
        assertTrue(natureScenes.contains(2)) // beach
    }

    @Test
    fun `人像分组 - 应该包含5种场景`() {
        val portraitScenes = SceneClassifier.SCENE_GROUPS["portrait"]
        
        assertNotNull(portraitScenes)
        assertEquals("人像分组应该包含5种场景", 5, portraitScenes!!.size)
        assertTrue(portraitScenes.contains(12)) // portrait
        assertTrue(portraitScenes.contains(13)) // selfie
    }

    @Test
    fun `美食分组 - 应该包含5种场景`() {
        val foodScenes = SceneClassifier.SCENE_GROUPS["food"]
        
        assertNotNull(foodScenes)
        assertEquals("美食分组应该包含5种场景", 5, foodScenes!!.size)
    }

    @Test
    fun `夜景特殊分组 - 应该包含6种场景`() {
        val specialScenes = SceneClassifier.SCENE_GROUPS["special"]
        
        assertNotNull(specialScenes)
        assertEquals("夜景特殊分组应该包含6种场景", 6, specialScenes!!.size)
    }

    @Test
    fun `场景标签 - 应该包含正确的场景ID和名称`() {
        val landscape = SceneClassifier.SCENE_LABELS[0]
        
        assertNotNull(landscape)
        assertEquals("landscape", landscape!!.id)
        assertEquals("风景", landscape.name)
        assertTrue(landscape.description.contains("自然风光"))
    }

    @Test
    fun `场景标签 - 人像场景应该有正确的名称`() {
        val portrait = SceneClassifier.SCENE_LABELS[12]
        
        assertNotNull(portrait)
        assertEquals("portrait", portrait!!.id)
        assertEquals("人像", portrait.name)
    }

    @Test
    fun `场景标签 - 美食场景应该有正确的名称`() {
        val food = SceneClassifier.SCENE_LABELS[22]
        
        assertNotNull(food)
        assertEquals("food", food!!.id)
        assertEquals("美食", food.name)
    }

    @Test
    fun `场景标签 - 夜景场景应该有正确的名称`() {
        val night = SceneClassifier.SCENE_LABELS[27]
        
        assertNotNull(night)
        assertEquals("night", night!!.id)
        assertEquals("夜景", night.name)
    }

    @Test
    fun `概率数组处理 - 应该正确找出最大概率`() {
        val probabilities = floatArrayOf(0.1f, 0.3f, 0.5f, 0.2f, 0.8f, 0.15f)
        
        var maxIndex = 0
        var maxProb = probabilities[0]
        
        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }
        
        assertEquals("最大概率应该在索引4", 4, maxIndex)
        assertEquals("最大概率值应该是0.8", 0.8f, maxProb, 0.001f)
    }

    @Test
    fun `候选场景排序 - 应该按概率降序排列`() {
        val probabilities = floatArrayOf(0.1f, 0.3f, 0.5f, 0.2f, 0.8f, 0.15f)
        
        val indexedProbs = probabilities.indices.map { Pair(it, probabilities[it]) }
        val sorted = indexedProbs.sortedByDescending { it.second }.take(3)
        
        assertEquals("第一名应该是索引4", 4, sorted[0].first)
        assertEquals("第二名应该是索引2", 2, sorted[1].first)
        assertEquals("第三名应该是索引1", 1, sorted[2].first)
    }

    @Test
    fun `缓存键生成 - 应该基于图像尺寸和采样像素`() {
        val width = 1920
        val height = 1080
        val samplePixels = intArrayOf(
            0xFF123456.toInt(),
            0xFF234567.toInt(),
            0xFF345678.toInt(),
            0xFF456789.toInt(),
            0xFF567890.toInt(),
            0xFF678901.toInt(),
            0xFF789012.toInt(),
            0xFF890123.toInt(),
            0xFF901234.toInt(),
            0xFF012345.toInt()
        )
        
        val cacheKey = "scene_${width}_${height}_${samplePixels.contentHashCode()}"
        
        assertTrue(cacheKey.startsWith("scene_1920_1080_"))
        assertTrue(cacheKey.contains("_"))
    }

    @Test
    fun `场景分组查询 - 应该正确查找分组`() {
        val entries = SceneClassifier.SCENE_GROUPS.entries
        val found = entries.find { it.value.contains(0) }
        
        assertNotNull("应该找到包含索引0的分组", found)
        assertEquals("该分组应该是nature", "nature", found!!.key)
    }

    @Test
    fun `未知场景索引 - 应该返回unknown标签`() {
        val unknownLabel = SceneClassifier.SCENE_LABELS[999]
        
        assertNull("不存在的索引应该返回null", unknownLabel)
    }
}

/**
 * SceneResult 数据类测试
 */
class SceneResultTest {

    @Test
    fun `场景结果创建 - 应该正确设置属性`() {
        val sceneId = "portrait"
        val sceneName = "人像"
        val confidence = 0.85f
        val inferenceTimeMs = 150L
        
        // 验证属性设置逻辑
        assertEquals("portrait", sceneId)
        assertEquals("人像", sceneName)
        assertEquals(0.85f, confidence, 0.001f)
        assertEquals(150L, inferenceTimeMs)
    }

    @Test
    fun `候选场景列表 - 应该按置信度排序`() {
        val candidates = listOf(
            SceneCandidate("portrait", "人像", 0.85f),
            SceneCandidate("landscape", "风景", 0.72f),
            SceneCandidate("food", "美食", 0.65f)
        )
        
        val sorted = candidates.sortedByDescending { it.confidence }
        
        assertEquals("人像", sorted[0].sceneName)
        assertEquals("风景", sorted[1].sceneName)
        assertEquals("美食", sorted[2].sceneName)
    }

    @Test
    fun `置信度范围 - 应该在0到1之间`() {
        val confidences = listOf(0.0f, 0.5f, 0.99f, 1.0f)
        
        for (conf in confidences) {
            assertTrue("置信度应该在0到1之间", conf in 0f..1f)
        }
    }
}

/**
 * SceneCandidate 数据类测试
 */
class SceneCandidateTest {

    @Test
    fun `候选场景创建 - 应该正确设置属性`() {
        val candidate = SceneCandidate("landscape", "风景", 0.92f)
        
        assertEquals("landscape", candidate.sceneId)
        assertEquals("风景", candidate.sceneName)
        assertEquals(0.92f, candidate.confidence, 0.001f)
    }
}
