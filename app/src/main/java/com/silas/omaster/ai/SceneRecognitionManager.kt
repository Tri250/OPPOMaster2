package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
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
 * 使用统一 SceneProfile 模型
 */
class SceneRecognitionManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val sceneDetector = SceneDetector()

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

        // 模拟 AI 识别过程（实际项目中应调用 TensorFlow Lite 模型）
        delay(300) // 模拟处理时间
        
        val detectedProfile = sceneDetector.detect(bitmap)
        
        SceneRecognitionResult(
            profile = detectedProfile,
            confidence = detectedProfile?.confidence ?: (0.75f + Math.random() * 0.24f).toFloat(),
            isEnabled = true
        )
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
    val isEnabled: Boolean
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
}

/**
 * 场景检测器（模拟实现）
 * 实际项目中应使用 TensorFlow Lite 模型
 */
private class SceneDetector {
    fun detect(bitmap: Bitmap): SceneProfile? {
        // 模拟检测逻辑
        // 实际项目中应调用 ML 模型进行推理
        val profiles = SceneProfileRepository.allProfiles
        val randomIndex = (Math.random() * profiles.size).toInt()
        val profile = profiles[randomIndex]
        
        // 模拟填充置信度
        return profile.copy(confidence = (0.75f + Math.random() * 0.24f).toFloat())
    }
}
