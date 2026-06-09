package com.silas.omaster.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 参数预测网络
 * 
 * 全连接网络，根据场景特征和质量指标预测18个调校参数
 * 
 * 输入：
 * - 场景特征向量（36维场景概率分布）
 * - 质量指标向量（5维质量评分）
 * 
 * 输出：
 * - 18个哈苏调校参数建议值
 * 
 * 参数类型：
 * - 基础调整：曝光、对比度、高光、阴影、白色色阶、黑色色阶
 * - 颜色调整：清晰度、自然饱和度、饱和度、色温、色调
 * - 效果调整：锐度、降噪、暗角、颗粒感、褪色效果
 * - 分色调：分色调高光、分色调阴影
 */
class ParamPredictor(private val context: Context) {
    
    companion object {
        private const val TAG = "ParamPredictor"
        
        // 输入维度
        private const val SCENE_FEATURE_DIM = 36    // 场景特征维度
        private const val QUALITY_FEATURE_DIM = 5   // 质量特征维度
        private const val TOTAL_INPUT_DIM = SCENE_FEATURE_DIM + QUALITY_FEATURE_DIM
        
        // 输出维度
        private const val PARAM_OUTPUT_DIM = 18     // 参数输出维度
        
        // 参数范围约束
        val PARAM_RANGES = mapOf(
            "exposure" to ParamRange(-2f, 2f),
            "contrast" to ParamRange(0f, 100f),
            "highlights" to ParamRange(-100f, 100f),
            "shadows" to ParamRange(-100f, 100f),
            "whites" to ParamRange(-100f, 100f),
            "blacks" to ParamRange(-100f, 100f),
            "clarity" to ParamRange(-100f, 100f),
            "vibrance" to ParamRange(-100f, 100f),
            "saturation" to ParamRange(-100f, 100f),
            "warmth" to ParamRange(-100f, 100f),
            "tint" to ParamRange(-100f, 100f),
            "sharpness" to ParamRange(0f, 100f),
            "noiseReduction" to ParamRange(0f, 100f),
            "vignette" to ParamRange(0f, 100f),
            "grain" to ParamRange(0f, 100f),
            "fade" to ParamRange(0f, 100f),
            "splitToneHighlights" to ParamRange(0f, 360f),
            "splitToneShadows" to ParamRange(0f, 360f)
        )
        
        // 参数名称列表
        val PARAM_NAMES = listOf(
            "exposure", "contrast", "highlights", "shadows", "whites", "blacks",
            "clarity", "vibrance", "saturation", "warmth", "tint",
            "sharpness", "noiseReduction", "vignette", "grain", "fade",
            "splitToneHighlights", "splitToneShadows"
        )
    }
    
    /**
     * 参数范围
     */
    data class ParamRange(
        val min: Float,
        val max: Float
    ) {
        fun clamp(value: Float): Float = value.coerceIn(min, max)
    }
    
    // TFLite引擎
    private val engine = TFLiteEngine.getInstance(context)
    
    /**
     * 预测调校参数
     * 
     * @param sceneProbabilities 场景概率分布（36维）
     * @param qualityScores 质量评分（5维）
     * @param useCache 是否使用缓存
     * @return 参数预测结果
     */
    suspend fun predict(
        sceneProbabilities: FloatArray,
        qualityScores: FloatArray,
        useCache: Boolean = true
    ): Result<ParamResult> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()
            
            // 验证输入维度
            if (sceneProbabilities.size != SCENE_FEATURE_DIM) {
                return@withContext Result.failure(
                    Exception("场景特征维度不正确: 期望${SCENE_FEATURE_DIM}, 实际${sceneProbabilities.size}")
                )
            }
            
            if (qualityScores.size != QUALITY_FEATURE_DIM) {
                return@withContext Result.failure(
                    Exception("质量特征维度不正确: 期望${QUALITY_FEATURE_DIM}, 实际${qualityScores.size}")
                )
            }
            
            // 构建输入向量
            val inputVector = buildInputVector(sceneProbabilities, qualityScores)
            
            // 生成缓存键
            val cacheKey = if (useCache) {
                generateCacheKey(sceneProbabilities, qualityScores)
            } else null
            
            // 执行推理
            val inferenceResult = engine.runInference<FloatArray>(
                modelName = TFLiteEngine.MODEL_PARAM_PREDICTOR,
                input = inputVector,
                cacheKey = cacheKey
            )
            
            // 处理推理结果
            val rawParams = inferenceResult.getOrNull()
                ?: return@withContext Result.failure(Exception("参数预测推理失败"))
            
            // 解析结果
            val result = parseParamResult(rawParams, startTime)
            
            Log.d(TAG, "参数预测完成: 耗时=${result.inferenceTimeMs}ms")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "参数预测失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从场景和质量结果预测参数
     * 
     * 便捷方法，直接使用场景和质量分析结果
     */
    suspend fun predictFromResults(
        sceneResult: SceneResult,
        qualityResult: QualityResult,
        useCache: Boolean = true
    ): Result<ParamResult> {
        // 构建场景概率向量
        val sceneProbabilities = FloatArray(SCENE_FEATURE_DIM)
        
        // 填充场景概率（使用候选场景的概率）
        val classifier = SceneClassifier(context)
        for (candidate in sceneResult.topCandidates) {
            val index = classifier.SCENE_LABELS.entries.find { it.value.id == candidate.sceneId }?.key
            if (index != null && index < SCENE_FEATURE_DIM) {
                sceneProbabilities[index] = candidate.confidence
            }
        }
        
        // 如果主场景概率未填充，确保填充
        val primaryIndex = classifier.SCENE_LABELS.entries.find { it.value.id == sceneResult.sceneId }?.key
        if (primaryIndex != null && primaryIndex < SCENE_FEATURE_DIM && sceneProbabilities[primaryIndex] == 0f) {
            sceneProbabilities[primaryIndex] = sceneResult.confidence
        }
        
        // 构建质量评分向量
        val qualityScores = floatArrayOf(
            qualityResult.brightnessScore,
            qualityResult.contrastScore,
            qualityResult.noiseScore,
            qualityResult.sharpnessScore,
            qualityResult.overallScore
        )
        
        return predict(sceneProbabilities, qualityScores, useCache)
    }
    
    /**
     * 构建输入向量
     */
    private fun buildInputVector(
        sceneProbabilities: FloatArray,
        qualityScores: FloatArray
    ): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(TOTAL_INPUT_DIM * 4)
        buffer.order(ByteOrder.nativeOrder())
        
        // 添加场景特征
        for (prob in sceneProbabilities) {
            buffer.putFloat(prob)
        }
        
        // 添加质量特征（归一化到0-1范围）
        for (score in qualityScores) {
            buffer.putFloat(score / 100f)
        }
        
        buffer.rewind()
        return buffer
    }
    
    /**
     * 解析参数预测结果
     */
    private fun parseParamResult(
        rawParams: FloatArray,
        startTime: Long
    ): ParamResult {
        // 确保输出维度正确
        val params = if (rawParams.size >= PARAM_OUTPUT_DIM) {
            rawParams.copyOf(PARAM_OUTPUT_DIM)
        } else {
            // 如果维度不足，用默认值填充
            FloatArray(PARAM_OUTPUT_DIM) { i ->
                rawParams.getOrElse(i) { getDefaultParamValue(i) }
            }
        }
        
        // 应用参数范围约束
        val constrainedParams = FloatArray(PARAM_OUTPUT_DIM) { i ->
            val paramName = PARAM_NAMES[i]
            val range = PARAM_RANGES[paramName]
            range?.clamp(params[i]) ?: params[i]
        }
        
        // 创建参数对象
        val hasselbladParams = HasselbladParameters.fromArray(constrainedParams)
        
        // 计算置信度（基于参数合理性）
        val confidence = calculateConfidence(constrainedParams)
        
        return ParamResult(
            parameters = hasselbladParams,
            confidence = confidence,
            inferenceTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * 获取默认参数值
     */
    private fun getDefaultParamValue(index: Int): Float {
        return when (index) {
            0 -> 0f    // exposure
            1 -> 50f   // contrast
            2 -> 0f    // highlights
            3 -> 0f    // shadows
            4 -> 0f    // whites
            5 -> 0f    // blacks
            6 -> 0f    // clarity
            7 -> 0f    // vibrance
            8 -> 0f    // saturation
            9 -> 0f    // warmth
            10 -> 0f   // tint
            11 -> 25f  // sharpness
            12 -> 25f  // noiseReduction
            13 -> 0f   // vignette
            14 -> 0f   // grain
            15 -> 0f   // fade
            16 -> 0f   // splitToneHighlights
            17 -> 0f   // splitToneShadows
            else -> 0f
        }
    }
    
    /**
     * 计算预测置信度
     * 
     * 基于参数的合理性评估
     */
    private fun calculateConfidence(params: FloatArray): Float {
        var totalScore = 0f
        
        // 检查参数是否在合理范围内
        for (i in params.indices) {
            val paramName = PARAM_NAMES[i]
            val range = PARAM_RANGES[paramName]
            
            if (range != null) {
                val paramValue = params[i]
                val normalizedValue = (paramValue - range.min) / (range.max - range.min)
                
                // 参数在中间范围时置信度较高
                val distanceFromCenter = kotlin.math.abs(normalizedValue - 0.5f)
                val score = 1f - distanceFromCenter * 0.5f
                totalScore += score
            }
        }
        
        return (totalScore / params.size).coerceIn(0f, 1f)
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(
        sceneProbabilities: FloatArray,
        qualityScores: FloatArray
    ): String {
        val sceneHash = sceneProbabilities.contentHashCode()
        val qualityHash = qualityScores.contentHashCode()
        return "param_${sceneHash}_${qualityHash}"
    }
    
    /**
     * 获取参数名称
     */
    fun getParamName(index: Int): String {
        return PARAM_NAMES.getOrElse(index) { "unknown" }
    }
    
    /**
     * 获取参数范围
     */
    fun getParamRange(paramName: String): ParamRange? {
        return PARAM_RANGES[paramName]
    }
    
    /**
     * 获取参数描述
     */
    fun getParamDescription(paramName: String): String {
        return when (paramName) {
            "exposure" -> "曝光补偿（-2.0 到 +2.0）"
            "contrast" -> "对比度（0-100）"
            "highlights" -> "高光调整（-100 到 +100）"
            "shadows" -> "阴影调整（-100 到 +100）"
            "whites" -> "白色色阶（-100 到 +100）"
            "blacks" -> "黑色色阶（-100 到 +100）"
            "clarity" -> "清晰度（-100 到 +100）"
            "vibrance" -> "自然饱和度（-100 到 +100）"
            "saturation" -> "饱和度（-100 到 +100）"
            "warmth" -> "色温（负值偏冷，正值偏暖）"
            "tint" -> "色调（负值偏绿，正值偏洋红）"
            "sharpness" -> "锐度（0-100）"
            "noiseReduction" -> "降噪强度（0-100）"
            "vignette" -> "暗角效果（0-100）"
            "grain" -> "颗粒感（0-100）"
            "fade" -> "褪色效果（0-100）"
            "splitToneHighlights" -> "分色调高光色相（0-360）"
            "splitToneShadows" -> "分色调阴影色相（0-360）"
            else -> "未知参数"
        }
    }
    
    /**
     * 合并参数（加权平均）
     * 
     * 用于多场景融合或用户偏好调整
     */
    fun mergeParameters(
        params1: HasselbladParameters,
        params2: HasselbladParameters,
        weight1: Float = 0.5f,
        weight2: Float = 0.5f
    ): HasselbladParameters {
        val array1 = params1.toArray()
        val array2 = params2.toArray()
        
        val merged = FloatArray(PARAM_OUTPUT_DIM) { i ->
            array1[i] * weight1 + array2[i] * weight2
        }
        
        // 应用范围约束
        for (i in merged.indices) {
            val paramName = PARAM_NAMES[i]
            val range = PARAM_RANGES[paramName]
            if (range != null) {
                merged[i] = range.clamp(merged[i])
            }
        }
        
        return HasselbladParameters.fromArray(merged)
    }
    
    /**
     * 调整参数（增量调整）
     */
    fun adjustParameters(
        baseParams: HasselbladParameters,
        adjustments: Map<String, Float>
    ): HasselbladParameters {
        val baseArray = baseParams.toArray()
        val adjusted = baseArray.copyOf()
        
        for ((paramName, delta) in adjustments) {
            val index = PARAM_NAMES.indexOf(paramName)
            if (index >= 0 && index < adjusted.size) {
                val range = PARAM_RANGES[paramName]
                if (range != null) {
                    adjusted[index] = range.clamp(baseArray[index] + delta)
                }
            }
        }
        
        return HasselbladParameters.fromArray(adjusted)
    }
    
    /**
     * 获取场景特定参数预设
     * 
     * 当模型不可用时使用启发式预设
     */
    fun getSceneDefaultParams(sceneId: String): HasselbladParameters {
        return when (sceneId) {
            // 自然风景
            "landscape", "mountain", "grassland" -> HasselbladParameters(
                contrast = 55f,
                clarity = 15f,
                vibrance = 10f,
                sharpness = 30f,
                vignette = 10f
            )
            "beach" -> HasselbladParameters(
                contrast = 50f,
                highlights = -10f,
                shadows = 5f,
                vibrance = 15f,
                warmth = 10f
            )
            "forest" -> HasselbladParameters(
                contrast = 45f,
                shadows = 10f,
                vibrance = 20f,
                warmth = 5f,
                vignette = 15f
            )
            "snow" -> HasselbladParameters(
                contrast = 60f,
                highlights = -15f,
                whites = -10f,
                clarity = 10f,
                vibrance = 5f
            )
            "desert" -> HasselbladParameters(
                contrast = 55f,
                warmth = 20f,
                saturation = 10f,
                sharpness = 35f
            )
            
            // 城市建筑
            "architecture" -> HasselbladParameters(
                contrast = 55f,
                clarity = 20f,
                sharpness = 40f,
                vignette = 5f
            )
            "street" -> HasselbladParameters(
                contrast = 50f,
                clarity = 10f,
                sharpness = 30f,
                grain = 5f
            )
            "city_night" -> HasselbladParameters(
                contrast = 60f,
                highlights = -20f,
                shadows = 15f,
                clarity = 15f,
                noiseReduction = 40f
            )
            "interior" -> HasselbladParameters(
                contrast = 40f,
                shadows = 10f,
                warmth = 10f,
                noiseReduction = 30f
            )
            "cafe" -> HasselbladParameters(
                contrast = 45f,
                warmth = 15f,
                fade = 10f,
                vignette = 20f
            )
            
            // 人物肖像
            "portrait", "selfie" -> HasselbladParameters(
                contrast = 40f,
                clarity = -10f,
                warmth = 5f,
                sharpness = 20f,
                noiseReduction = 35f
            )
            "couple", "family" -> HasselbladParameters(
                contrast = 45f,
                vibrance = 10f,
                warmth = 8f,
                vignette = 10f
            )
            "children" -> HasselbladParameters(
                contrast = 35f,
                saturation = 10f,
                warmth = 10f,
                fade = 5f
            )
            
            // 动物宠物
            "pet", "cat", "dog" -> HasselbladParameters(
                contrast = 45f,
                clarity = 10f,
                sharpness = 30f,
                noiseReduction = 30f
            )
            "wildlife", "bird" -> HasselbladParameters(
                contrast = 55f,
                clarity = 15f,
                sharpness = 35f,
                vignette = 5f
            )
            
            // 美食餐饮
            "food" -> HasselbladParameters(
                contrast = 50f,
                saturation = 15f,
                warmth = 10f,
                sharpness = 25f,
                vignette = 15f
            )
            "coffee", "drink" -> HasselbladParameters(
                contrast = 45f,
                warmth = 15f,
                fade = 10f,
                vignette = 20f
            )
            "dessert" -> HasselbladParameters(
                contrast = 50f,
                saturation = 20f,
                warmth = 12f,
                vignette = 10f
            )
            "fruit" -> HasselbladParameters(
                contrast = 50f,
                vibrance = 25f,
                saturation = 15f,
                sharpness = 30f
            )
            
            // 特殊场景
            "night", "starry" -> HasselbladParameters(
                contrast = 65f,
                highlights = -25f,
                shadows = 20f,
                clarity = 20f,
                noiseReduction = 50f
            )
            "sunset", "sunrise" -> HasselbladParameters(
                contrast = 55f,
                warmth = 25f,
                saturation = 20f,
                vignette = 15f,
                splitToneHighlights = 30f
            )
            "rainy" -> HasselbladParameters(
                contrast = 45f,
                clarity = 5f,
                fade = 15f,
                vignette = 20f
            )
            "foggy" -> HasselbladParameters(
                contrast = 35f,
                clarity = -5f,
                fade = 20f,
                vignette = 25f
            )
            
            // 运动活动
            "sports" -> HasselbladParameters(
                contrast = 55f,
                clarity = 15f,
                sharpness = 40f,
                saturation = 10f
            )
            "outdoor", "travel" -> HasselbladParameters(
                contrast = 50f,
                vibrance = 15f,
                sharpness = 30f,
                vignette = 10f
            )
            
            // 默认
            else -> HasselbladParameters.DEFAULT
        }
    }
}