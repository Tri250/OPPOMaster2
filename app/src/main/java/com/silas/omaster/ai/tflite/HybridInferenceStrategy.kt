package com.silas.omaster.ai.tflite

import android.content.Context
import android.graphics.Bitmap
import com.silas.omaster.ai.analyzer.AnalysisResult
import com.silas.omaster.ai.analyzer.ExifData
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ai.model.SceneProfileRepository

/**
 * 混合推理策略
 * TFLite + 启发式分析融合
 * 提升准确率至 80%+
 *
 * 策略：
 * 1. TFLite 模型推理（权重 0.60）- 提供主要场景判断
 * 2. 启发式分析（权重 0.40）- 提供颜色、亮度、纹理补充
 * 3. EXIF 数据辅助 - 提供拍摄环境信息
 * 4. 置信度融合 - 多策略加权合并
 */
class HybridInferenceStrategy(context: Context) {

    private val tfliteClassifier = TFLiteSceneClassifier.getInstance(context)
    private val heuristicAnalyzer = HeuristicSceneAnalyzer()

    // 权重配置
    private val tfliteWeight = 0.60f
    private val heuristicWeight = 0.40f

    /**
     * 混合推理
     *
     * @param bitmap 输入图像
     * @param exif EXIF 数据（可选）
     * @return 综合分析结果
     */
    fun analyze(bitmap: Bitmap, exif: ExifData? = null): AnalysisResult {
        val startTime = System.currentTimeMillis()

        // 1. TFLite 模型推理
        val tfliteResult = runTFLiteInference(bitmap)

        // 2. 启发式分析
        val heuristicResult = heuristicAnalyzer.analyze(bitmap, exif)

        // 3. 融合结果
        val fusedResult = fuseResults(tfliteResult, heuristicResult)

        // 4. 构建最终结果
        val analysisTime = System.currentTimeMillis() - startTime

        return AnalysisResult(
            primaryScene = fusedResult.primaryScene,
            confidence = fusedResult.confidence,
            alternativeScenes = fusedResult.alternatives,
            colorProfile = heuristicResult.colorProfile,
            brightnessLevel = heuristicResult.brightnessLevel,
            faceCount = heuristicResult.faceCount,
            textureProfile = heuristicResult.textureProfile,
            exifData = exif,
            analysisTimeMs = analysisTime,
            analysisDetails = buildAnalysisDetails(tfliteResult, heuristicResult)
        )
    }

    /**
     * 执行 TFLite 推理
     */
    private fun runTFLiteInference(bitmap: Bitmap): TFLiteInferenceResult {
        if (!tfliteClassifier.isModelAvailable()) {
            return TFLiteInferenceResult(
                isSuccess = false,
                topSceneId = null,
                probabilities = emptyMap(),
                confidence = 0f
            )
        }

        val classificationResult = tfliteClassifier.classify(bitmap)

        return TFLiteInferenceResult(
            isSuccess = classificationResult.isSuccess,
            topSceneId = classificationResult.topSceneId,
            probabilities = classificationResult.probabilities,
            confidence = classificationResult.maxConfidence
        )
    }

    /**
     * 融合 TFLite 和启发式结果
     */
    private fun fuseResults(
        tfliteResult: TFLiteInferenceResult,
        heuristicResult: AnalysisResult
    ): FusedInferenceResult {
        // 如果 TFLite 模型不可用，直接使用启发式结果
        if (!tfliteResult.isSuccess) {
            return FusedInferenceResult(
                primaryScene = heuristicResult.primaryScene,
                confidence = heuristicResult.confidence,
                alternatives = heuristicResult.alternativeScenes,
                inferenceSource = InferenceSource.HEURISTIC_ONLY
            )
        }

        // 获取 TFLite 主场景
        val tflitePrimaryId = tfliteResult.topSceneId
        val tfliteConfidence = tfliteResult.confidence

        // 获取启发式主场景
        val heuristicPrimaryId = heuristicResult.primaryScene.id
        val heuristicConfidence = heuristicResult.confidence

        // 融合策略
        val fusedSceneId = when {
            // TFLite 高置信度（> 80%）直接采用
            tfliteConfidence > 0.80f -> tflitePrimaryId

            // 启发式高置信度（> 70%）且与 TFLite 一致
            heuristicConfidence > 0.70f && tflitePrimaryId == heuristicPrimaryId -> tflitePrimaryId

            // TFLite 中等置信度（> 60%）
            tfliteConfidence > 0.60f -> {
                // 检查启发式结果是否支持
                if (heuristicPrimaryId == tflitePrimaryId) {
                    tflitePrimaryId // 一致，采用 TFLite
                } else {
                    // 不一致，加权选择
                    val tfliteScore = tfliteConfidence * tfliteWeight
                    val heuristicScore = heuristicConfidence * heuristicWeight

                    if (tfliteScore > heuristicScore) tflitePrimaryId else heuristicPrimaryId
                }
            }

            // TFLite 低置信度（< 60%），主要依赖启发式
            else -> {
                // 启发式高置信度时采用启发式
                if (heuristicConfidence > 0.60f) heuristicPrimaryId
                else {
                    // 都低置信度，加权选择
                    val tfliteScore = tfliteConfidence * tfliteWeight
                    val heuristicScore = heuristicConfidence * heuristicWeight

                    if (tfliteScore > heuristicScore) tflitePrimaryId else heuristicPrimaryId
                }
            }
        }

        // 计算融合置信度
        val fusedConfidence = calculateFusedConfidence(
            tfliteConfidence, heuristicConfidence,
            tflitePrimaryId == fusedSceneId, heuristicPrimaryId == fusedSceneId
        )

        // 获取主场景配置
        val primaryScene = SceneProfileRepository.getProfileById(fusedSceneId ?: "portrait-standard")
            ?: heuristicResult.primaryScene

        // 获取备选场景
        val alternatives = buildAlternatives(tfliteResult, heuristicResult, fusedSceneId)

        return FusedInferenceResult(
            primaryScene = primaryScene.copy(confidence = fusedConfidence),
            confidence = fusedConfidence,
            alternatives = alternatives,
            inferenceSource = InferenceSource.HYBRID
        )
    }

    /**
     * 计算融合置信度
     */
    private fun calculateFusedConfidence(
        tfliteConf: Float,
        heuristicConf: Float,
        tfliteMatches: Boolean,
        heuristicMatches: Boolean
    ): Float {
        // 基础融合公式
        val baseConfidence = tfliteConf * tfliteWeight + heuristicConf * heuristicWeight

        // 一致性加成
        val consistencyBonus = when {
            tfliteMatches && heuristicMatches -> 0.10f // 双方一致，加成 10%
            tfliteMatches || heuristicMatches -> 0.05f // 单方一致，加成 5%
            else -> 0f
        }

        return (baseConfidence + consistencyBonus).coerceIn(0f, 1f)
    }

    /**
     * 构建备选场景列表
     */
    private fun buildAlternatives(
        tfliteResult: TFLiteInferenceResult,
        heuristicResult: AnalysisResult,
        primaryId: String?
    ): List<SceneProfile> {
        val alternatives = mutableListOf<SceneProfile>()

        // 添加 TFLite Top-3（排除主场景）
        tfliteResult.top3
            .filter { it.first != primaryId }
            .take(2)
            .forEach { (sceneId, confidence) ->
                val profile = SceneProfileRepository.getProfileById(sceneId)
                if (profile != null) {
                    alternatives.add(profile.copy(confidence = confidence * tfliteWeight))
                }
            }

        // 添加启发式备选（排除已添加的）
        heuristicResult.alternativeScenes
            .filter { it.id != primaryId }
            .filter { alt -> alternatives.none { it.id == alt.id } }
            .take(1)
            .forEach { profile ->
                alternatives.add(profile.copy(confidence = profile.confidence * heuristicWeight))
            }

        return alternatives.take(3)
    }

    /**
     * 构建分析详情
     */
    private fun buildAnalysisDetails(
        tfliteResult: TFLiteInferenceResult,
        heuristicResult: AnalysisResult
    ): Map<String, Any> {
        return mapOf(
            "tflite_success" to tfliteResult.isSuccess,
            "tflite_top_scene" to (tfliteResult.topSceneId ?: "unknown"),
            "tflite_confidence" to tfliteResult.confidence,
            "tflite_top3" to tfliteResult.top3.map { "${it.first}:${it.second}" },
            "heuristic_scene" to heuristicResult.primaryScene.id,
            "heuristic_confidence" to heuristicResult.confidence,
            "inference_source" to (if (tfliteResult.isSuccess) "HYBRID" else "HEURISTIC_ONLY"),
            "color_warmth" to heuristicResult.colorProfile.warmthRatio,
            "brightness_level" to heuristicResult.brightnessLevel.value,
            "face_count" to heuristicResult.faceCount
        )
    }

    /**
     * 检查 TFLite 模型是否可用
     */
    fun isTFLiteAvailable(): Boolean {
        return tfliteClassifier.isModelAvailable()
    }

    /**
     * 获取推理策略描述
     */
    fun getStrategyDescription(): String {
        return if (isTFLiteAvailable()) {
            "混合推理：TFLite(60%) + 启发式(40%)"
        } else {
            "启发式推理：颜色+亮度+纹理+EXIF"
        }
    }

    companion object {
        @Volatile
        private var instance: HybridInferenceStrategy? = null

        fun getInstance(context: Context): HybridInferenceStrategy {
            return instance ?: synchronized(this) {
                instance ?: HybridInferenceStrategy(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * TFLite 推理结果
 */
data class TFLiteInferenceResult(
    val isSuccess: Boolean,
    val topSceneId: String?,
    val probabilities: Map<String, Float>,
    val confidence: Float
) {
    val top3: List<Pair<String, Float>> get() = probabilities
        .toList()
        .sortedByDescending { it.second }
        .take(3)
}

/**
 * 融合推理结果
 */
data class FusedInferenceResult(
    val primaryScene: SceneProfile,
    val confidence: Float,
    val alternatives: List<SceneProfile>,
    val inferenceSource: InferenceSource
)

/**
 * 推理来源
 */
enum class InferenceSource(val displayName: String) {
    TFLITE_ONLY("仅TFLite"),
    HEURISTIC_ONLY("仅启发式"),
    HYBRID("混合推理")
}