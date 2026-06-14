package com.silas.omaster.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.tflite.models.SceneFeatureExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.Locale

/**
 * 场景分类器
 * 
 * 基于 MobileNetV3 变体的场景分类模型
 * 支持 36+ 种场景类型的识别
 * 
 * 场景类型：
 * - 自然风景：风景、山脉、海滩、森林、雪景、沙漠、草原
 * - 城市建筑：建筑、街拍、城市夜景、室内、咖啡馆
 * - 人物肖像：人像、自拍、情侣、家庭、儿童
 * - 动物宠物：宠物、野生动物、鸟类、猫、狗
 * - 美食餐饮：美食、饮品、咖啡、甜点、水果
 * - 特殊场景：夜景、日落、日出、雨天、雾天、星空
 * - 运动活动：运动、户外活动、旅行、徒步
 * - 艺术创意：艺术、黑白、复古、极简、抽象
 */
class SceneClassifier(private val context: Context) {
    
    companion object {
        private const val TAG = "SceneClassifier"
        
        // 模型输入尺寸
        private const val INPUT_SIZE = 224
        
        // 场景标签（36种场景类型）
        val SCENE_LABELS = mapOf(
            0 to SceneLabel("landscape", "风景", "自然风光、山水景色"),
            1 to SceneLabel("mountain", "山脉", "山峰、山峦、山景"),
            2 to SceneLabel("beach", "海滩", "海边、沙滩、海岸"),
            3 to SceneLabel("forest", "森林", "树林、丛林、森林景观"),
            4 to SceneLabel("snow", "雪景", "雪地、雪山、冰雪景观"),
            5 to SceneLabel("desert", "沙漠", "沙漠、戈壁、荒漠"),
            6 to SceneLabel("grassland", "草原", "草原、牧场、草地"),
            7 to SceneLabel("architecture", "建筑", "建筑物、现代建筑、古典建筑"),
            8 to SceneLabel("street", "街拍", "街道、城市街道、街头摄影"),
            9 to SceneLabel("city_night", "城市夜景", "城市夜景、霓虹灯、城市灯光"),
            10 to SceneLabel("interior", "室内", "室内空间、房间、家居"),
            11 to SceneLabel("cafe", "咖啡馆", "咖啡厅、茶室、休闲场所"),
            12 to SceneLabel("portrait", "人像", "人物肖像、单人照片"),
            13 to SceneLabel("selfie", "自拍", "自拍照片、自拍人像"),
            14 to SceneLabel("couple", "情侣", "情侣合照、双人照片"),
            15 to SceneLabel("family", "家庭", "家庭合影、全家福"),
            16 to SceneLabel("children", "儿童", "儿童照片、小孩"),
            17 to SceneLabel("pet", "宠物", "宠物照片、猫狗等"),
            18 to SceneLabel("wildlife", "野生动物", "野生动物、动物园"),
            19 to SceneLabel("bird", "鸟类", "鸟类照片、飞鸟"),
            20 to SceneLabel("cat", "猫", "猫咪照片"),
            21 to SceneLabel("dog", "狗", "狗狗照片"),
            22 to SceneLabel("food", "美食", "美食照片、菜肴"),
            23 to SceneLabel("drink", "饮品", "饮料、酒水"),
            24 to SceneLabel("coffee", "咖啡", "咖啡饮品"),
            25 to SceneLabel("dessert", "甜点", "甜点、蛋糕、甜品"),
            26 to SceneLabel("fruit", "水果", "水果照片"),
            27 to SceneLabel("night", "夜景", "夜景、夜间场景"),
            28 to SceneLabel("sunset", "日落", "日落、黄昏"),
            29 to SceneLabel("sunrise", "日出", "日出、清晨"),
            30 to SceneLabel("rainy", "雨天", "雨天、雨景"),
            31 to SceneLabel("foggy", "雾天", "雾天、雾景"),
            32 to SceneLabel("starry", "星空", "星空、夜空、银河"),
            33 to SceneLabel("sports", "运动", "运动场景、体育活动"),
            34 to SceneLabel("outdoor", "户外", "户外活动、野外"),
            35 to SceneLabel("travel", "旅行", "旅行照片、旅游场景")
        )
        
        // 场景分组
        val SCENE_GROUPS = mapOf(
            "nature" to listOf(0, 1, 2, 3, 4, 5, 6),
            "urban" to listOf(7, 8, 9, 10, 11),
            "portrait" to listOf(12, 13, 14, 15, 16),
            "animal" to listOf(17, 18, 19, 20, 21),
            "food" to listOf(22, 23, 24, 25, 26),
            "special" to listOf(27, 28, 29, 30, 31, 32),
            "activity" to listOf(33, 34, 35)
        )
    }
    
    /**
     * 场景标签
     */
    data class SceneLabel(
        val id: String,
        val name: String,
        val description: String
    )
    
    // TFLite引擎
    private val engine = TFLiteEngine.getInstance(context)
    
    // 特征提取器
    private val featureExtractor = SceneFeatureExtractor(context)
    
    /**
     * 分类场景
     * 
     * @param bitmap 输入图像
     * @param useCache 是否使用缓存
     * @return 场景分类结果
     */
    suspend fun classify(
        bitmap: Bitmap,
        useCache: Boolean = true
    ): Result<SceneResult> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            
            // 生成缓存键
            val cacheKey = if (useCache) {
                generateCacheKey(bitmap)
            } else null
            
            // 预处理图像
            val inputBuffer = preprocessImage(bitmap)
            
            // 执行推理
            val inferenceResult = engine.runInference<FloatArray>(
                modelName = TFLiteEngine.MODEL_SCENE_CLASSIFIER,
                input = inputBuffer,
                cacheKey = cacheKey
            )
            
            // 处理推理结果
            val probabilities = inferenceResult.getOrNull()
                ?: return@withContext Result.failure(Exception("场景分类推理失败"))
            
            // 解析结果
            val result = parseClassificationResult(probabilities, startTime)
            
            Log.d(TAG, "场景分类完成: ${result.sceneName}, 置信度: ${result.confidence}, 耗时: ${result.inferenceTimeMs}ms")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "场景分类失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 预处理图像
     * 
     * 将Bitmap转换为模型输入格式
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // 缩放图像到模型输入尺寸
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // 使用引擎的预处理方法
        return engine.preprocessBitmap(resizedBitmap, INPUT_SIZE, normalize = true)
    }
    
    /**
     * 解析分类结果
     */
    private fun parseClassificationResult(
        probabilities: FloatArray,
        startTime: Long
    ): SceneResult {
        // 找到概率最高的场景
        var maxIndex = 0
        var maxProb = probabilities[0]
        
        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }
        
        // 获取场景标签
        val sceneLabel = SCENE_LABELS[maxIndex]
            ?: SceneLabel("unknown", "未知", "无法识别的场景")
        
        // 获取候选场景（前5个）
        val topCandidates = getTopCandidates(probabilities, 5)
        
        return SceneResult(
            sceneId = sceneLabel.id,
            sceneName = sceneLabel.name,
            confidence = maxProb,
            topCandidates = topCandidates,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * 获取候选场景列表
     */
    private fun getTopCandidates(probabilities: FloatArray, topN: Int): List<SceneCandidate> {
        // 边界检查：空数组或无效topN
        if (probabilities.isEmpty() || topN <= 0) {
            return emptyList()
        }

        // 创建索引-概率对
        val indexedProbs = probabilities.indices.map { Pair(it, probabilities[it]) }

        // 按概率降序排序，取前N个（不超过数组长度）
        val effectiveTopN = minOf(topN, probabilities.size)
        val sorted = indexedProbs.sortedByDescending { it.second }.take(effectiveTopN)

        // 转换为候选列表
        return sorted.map { (index, prob) ->
            val label = SCENE_LABELS[index] ?: SceneLabel("unknown", "未知", "")
            SceneCandidate(
                sceneId = label.id,
                sceneName = label.name,
                confidence = prob
            )
        }
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(bitmap: Bitmap): String {
        // 使用图像尺寸和部分像素值生成唯一键
        val width = bitmap.width
        val height = bitmap.height
        val samplePixels = IntArray(10)
        
        for (i in 0 until 10) {
            val x = (i * width / 10).coerceIn(0, width - 1)
            val y = (i * height / 10).coerceIn(0, height - 1)
            samplePixels[i] = bitmap.getPixel(x, y)
        }
        
        return "scene_${width}_${height}_${samplePixels.contentHashCode()}"
    }
    
    /**
     * 获取场景分组
     */
    fun getSceneGroup(sceneId: String): String? {
        // 找到场景索引
        val index = SCENE_LABELS.entries.find { it.value.id == sceneId }?.key
        if (index == null) return null
        
        // 查找所属分组
        return SCENE_GROUPS.entries.find { it.value.contains(index) }?.key
    }
    
    /**
     * 获取同组场景
     */
    fun getSimilarScenes(sceneId: String): List<SceneLabel> {
        val group = getSceneGroup(sceneId)
        if (group == null) return emptyList()
        
        val indices = SCENE_GROUPS[group] ?: emptyList()
        return indices.mapNotNull { SCENE_LABELS[it] }
    }
    
    /**
     * 获取场景描述
     */
    fun getSceneDescription(sceneId: String): String {
        val label = SCENE_LABELS.values.find { it.id == sceneId }
        return label?.description ?: "未知场景类型"
    }
    
    /**
     * 判断是否为人物类场景
     */
    fun isPortraitScene(sceneId: String): Boolean {
        return getSceneGroup(sceneId) == "portrait"
    }
    
    /**
     * 判断是否为风景类场景
     */
    fun isNatureScene(sceneId: String): Boolean {
        return getSceneGroup(sceneId) == "nature"
    }
    
    /**
     * 判断是否为夜景类场景
     */
    fun isNightScene(sceneId: String): Boolean {
        return sceneId in listOf("night", "city_night", "starry")
    }
    
    /**
     * 判断是否为美食类场景
     */
    fun isFoodScene(sceneId: String): Boolean {
        return getSceneGroup(sceneId) == "food"
    }
    
    /**
     * 获取所有场景标签
     */
    fun getAllSceneLabels(): List<SceneLabel> {
        return SCENE_LABELS.values.toList()
    }
    
    /**
     * 获取场景数量
     */
    fun getSceneCount(): Int = SCENE_LABELS.size
    
    /**
     * 根据场景ID获取场景标签
     */
    fun getSceneLabel(sceneId: String): SceneLabel? {
        return SCENE_LABELS.values.find { it.id == sceneId }
    }
}