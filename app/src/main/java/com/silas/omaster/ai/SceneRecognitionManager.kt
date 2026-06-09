package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import com.silas.omaster.ai.analyzer.AnalysisResult
import com.silas.omaster.ai.analyzer.ExifData
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.model.SceneCategory
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ai.model.SceneProfileRepository
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * AI 场景识别管理器
 * 支持 50+ 拍摄场景智能识别
 * 使用统一 SceneProfile 模型 + 启发式分析器
 */
class SceneRecognitionManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val heuristicAnalyzer = HeuristicSceneAnalyzer()

    /**
     * 获取所有支持的场景配置
     */
    val supportedProfiles: List<SceneProfile> = SceneProfileRepository.allProfiles

    /**
     * 获取按大类分组的场景配置
     */
    val profilesByCategory: Map<SceneCategory, List<SceneProfile>> = SceneProfileRepository.profilesByCategory

    /**
     * 识别图片场景
     * @param bitmap 待识别图片
     * @return 识别结果（包含完整 SceneProfile）
     */
    suspend fun recognizeScene(bitmap: Bitmap): SceneRecognitionResult = withContext(Dispatchers.Default) {
        if (!settingsManager.isAISceneRecognitionEnabled) {
            return@withContext SceneRecognitionResult(
                profile = null,
                confidence = 0f,
                isEnabled = false
            )
        }

        // 使用启发式分析器进行真实分析
        val analysisResult = heuristicAnalyzer.analyze(bitmap)

        SceneRecognitionResult(
            profile = analysisResult.primaryScene,
            confidence = analysisResult.confidence,
            isEnabled = true,
            analysisResult = analysisResult
        )
    }

    /**
     * 详细分析图片场景
     * 包含颜色、亮度、纹理、人脸检测等完整分析结果
     *
     * @param bitmap 待分析图片
     * @param exif EXIF 元数据（可选）
     * @return 综合分析结果
     */
    suspend fun analyzeSceneDetailed(bitmap: Bitmap, exif: ExifData? = null): AnalysisResult = withContext(Dispatchers.Default) {
        if (!settingsManager.isAISceneRecognitionEnabled) {
            // 返回默认结果
            val defaultProfile = SceneProfileRepository.allProfiles.first()
            return@withContext AnalysisResult(
                primaryScene = defaultProfile,
                confidence = 0f,
                alternativeScenes = emptyList(),
                colorProfile = com.silas.omaster.ai.analyzer.ColorProfile(
                    avgRed = 128, avgGreen = 128, avgBlue = 128,
                    warmthRatio = 0f, coolRatio = 0f,
                    greenDominance = 1f, blueDominance = 1f, redDominance = 1f,
                    colorVariance = 0f,
                    dominantTone = com.silas.omaster.ai.analyzer.DominantTone.NEUTRAL
                ),
                brightnessLevel = com.silas.omaster.ai.analyzer.BrightnessLevel.NORMAL,
                faceCount = 0,
                analysisTimeMs = 0
            )
        }

        heuristicAnalyzer.analyze(bitmap, exif)
    }

    /**
     * 实时识别流
     * 用于相机预览时持续识别场景
     */
    fun recognizeSceneStream(bitmap: Bitmap): Flow<SceneRecognitionResult> = flow {
        while (true) {
            emit(recognizeScene(bitmap))
            delay(500) // 每 500ms 识别一次
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 根据场景 ID 获取配置
     */
    fun getProfileById(id: String): SceneProfile? {
        return SceneProfileRepository.getProfileById(id)
    }

    /**
     * 根据大类获取场景列表
     */
    fun getProfilesByCategory(category: SceneCategory): List<SceneProfile> {
        return SceneProfileRepository.getProfilesByCategory(category)
    }

    /**
     * 搜索场景（按名称或标签）
     */
    fun searchProfiles(query: String): List<SceneProfile> {
        return SceneProfileRepository.searchProfiles(query)
    }

    /**
     * 切换 AI 场景识别开关
     */
    fun toggleSceneRecognition(enabled: Boolean) {
        settingsManager.isAISceneRecognitionEnabled = enabled
    }

    companion object {
        @Volatile
        private var instance: SceneRecognitionManager? = null

        fun getInstance(context: Context): SceneRecognitionManager {
            return instance ?: synchronized(this) {
                instance ?: SceneRecognitionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 场景识别结果
 * 包含完整的 SceneProfile 配置
 */
data class SceneRecognitionResult(
    val profile: SceneProfile?,
    val confidence: Float,
    val isEnabled: Boolean,
    val analysisResult: AnalysisResult? = null
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
    
    /**
     * 获取哈苏大师参数
     */
    val hasselbladParams get() = profile?.hasselbladParams
    
    /**
     * 获取推荐胶片
     */
    val recommendedFilm get() = profile?.recommendedFilm
    
    /**
     * 获取拍摄建议
     */
    val masterTips get() = profile?.masterTips
    
    /**
     * 获取颜色分析结果
     */
    val colorProfile get() = analysisResult?.colorProfile
    
    /**
     * 获取亮度等级
     */
    val brightnessLevel get() = analysisResult?.brightnessLevel
    
    /**
     * 获取人脸数量
     */
    val faceCount get() = analysisResult?.faceCount
    
    /**
     * 获取备选场景
     */
    val alternativeScenes get() = analysisResult?.alternativeScenes
}
