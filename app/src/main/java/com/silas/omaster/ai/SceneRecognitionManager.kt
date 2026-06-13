package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.HasselbladParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 哈苏之眼管理器
 * 支持 35+ 拍摄场景智能识别
 * 
 * 使用真实启发式分析引擎进行场景识别：
 * - 颜色直方图分析
 * - 亮度等级检测
 * - 人脸检测（ML Kit）
 * - 纹理边缘分析
 * - 多特征投票融合
 */
class SceneRecognitionManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    
    // 使用真实启发式分析器（而非模拟检测器）
    private val heuristicAnalyzer = HeuristicSceneAnalyzer.getInstance(context)
    
    // 场景→哈苏参数映射
    private val sceneMapping = SceneToHasselbladMapping

    // 35+ 场景类型定义
    val supportedScenes = listOf(
        SceneType.PORTRAIT,
        SceneType.LANDSCAPE,
        SceneType.NIGHT,
        SceneType.FOOD,
        SceneType.PET,
        SceneType.DOCUMENT,
        SceneType.BUILDING,
        SceneType.FLOWER,
        SceneType.SKY,
        SceneType.SNOW,
        SceneType.BEACH,
        SceneType.SUNSET,
        SceneType.FIREWORKS,
        SceneType.STAGE,
        SceneType.SPORTS,
        SceneType.CHILDREN,
        SceneType.TEXT,
        SceneType.GROUP_PHOTO,
        SceneType.CANDLELIGHT,
        SceneType.AQUARIUM,
        SceneType.WATERFALL,
        SceneType.STARRY_SKY,
        SceneType.TRAFFIC,
        SceneType.RAINBOW,
        SceneType.BACKLIGHT,
        SceneType.MACRO,
        SceneType.PANORAMA,
        SceneType.TIME_LAPSE,
        SceneType.SLOW_MOTION,
        SceneType.PRO_MODE,
        SceneType.STREET,
        SceneType.CAFE,
        SceneType.MUSEUM,
        SceneType.CONCERT,
        SceneType.WEDDING,
        SceneType.PARTY
    )

    /**
     * 识别图片场景（真实启发式分析）
     * 
     * 使用HeuristicSceneAnalyzer进行真实图像分析：
     * 1. 颜色直方图采样
     * 2. 亮度等级计算
     * 3. 人脸检测（ML Kit）
     * 4. 纹理边缘密度分析
     * 5. 多特征投票融合
     * 
     * @param bitmap 待识别图片
     * @return 识别结果
     */
    suspend fun recognizeScene(bitmap: Bitmap): SceneRecognitionResult = withContext(Dispatchers.Default) {
        if (!settingsManager.isAISceneRecognitionEnabled) {
            Log.d(TAG, "AI场景识别已禁用")
            return@withContext SceneRecognitionResult(
                sceneType = SceneType.UNKNOWN,
                confidence = 0f,
                recommendedParams = emptyMap(),
                isEnabled = false
            )
        }

        try {
            Log.d(TAG, "开始真实场景识别分析")
            
            // 使用启发式分析器进行真实图像分析
            val analysisResult = heuristicAnalyzer.analyze(bitmap)
            
            Log.d(TAG, "场景分析完成: 场景=${analysisResult.primaryScene.id}, 置信度=${analysisResult.confidence}")
            
            // 将分析结果映射到SceneType
            val detectedSceneType = mapSceneProfileToSceneType(analysisResult.primaryScene.id)
            
            // 生成推荐参数（基于哈苏参数映射）
            val recommendedParams = generateRecommendedParamsFromAnalysis(
                detectedSceneType,
                analysisResult
            )
            
            SceneRecognitionResult(
                sceneType = detectedSceneType,
                confidence = analysisResult.confidence,
                recommendedParams = recommendedParams,
                isEnabled = true,
                // 扩展信息
                colorProfile = analysisResult.colorProfile,
                brightnessLevel = analysisResult.brightnessLevel,
                faceCount = analysisResult.faceCount,
                edgeDensity = analysisResult.edgeDensity,
                alternativeScenes = analysisResult.alternativeScenes.map { 
                    mapSceneProfileToSceneType(it.id) 
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "场景识别失败: ${e.message}", e)
            // 错误时返回默认结果
            SceneRecognitionResult(
                sceneType = SceneType.UNKNOWN,
                confidence = 0f,
                recommendedParams = emptyMap(),
                isEnabled = true,
                errorMessage = e.message
            )
        }
    }

    /**
     * 将SceneProfile的sceneId映射到SceneType
     */
    private fun mapSceneProfileToSceneType(sceneId: String): SceneType {
        return when (sceneId) {
            // 人像类
            "portrait-standard", "portrait-backlit", "portrait-couple", "portrait-group", "portrait-child" -> SceneType.PORTRAIT
            
            // 风景类
            "landscape-standard", "landscape-forest", "landscape-sky", "landscape-beach", "landscape-sunset", "landscape-snow" -> SceneType.LANDSCAPE
            
            // 夜景类
            "night-city", "night-neon", "night-starry", "night-candle" -> SceneType.NIGHT
            
            // 美食类
            "food-restaurant", "food-dessert" -> SceneType.FOOD
            
            // 街拍类
            "urban-street", "urban-cafe", "urban-architecture" -> SceneType.STREET
            
            // 建筑类
            "urban-architecture" -> SceneType.BUILDING
            
            // 微距类
            "macro-insect", "macro-texture" -> SceneType.MACRO
            
            // 其他场景映射
            "event-party", "event-concert" -> SceneType.PARTY
            "still-product" -> SceneType.DOCUMENT
            
            else -> SceneType.UNKNOWN
        }
    }

    /**
     * 基于真实分析结果生成推荐参数
     */
    private fun generateRecommendedParamsFromAnalysis(
        sceneType: SceneType,
        analysisResult: HeuristicSceneAnalyzer.AnalysisResult
    ): Map<String, String> {
        // 获取哈苏参数映射
        val hasselbladParams = sceneMapping.getParams(analysisResult.primaryScene.id)
        
        // 基础参数（从哈苏参数转换）
        val baseParams = mutableMapOf(
            "saturation" to hasselbladParams.saturation.toString(),
            "contrast" to hasselbladParams.contrast.toString(),
            "brightness" to hasselbladParams.tone.toString(),
            "warmth" to hasselbladParams.colorTemp.toString(),
            "sharpness" to hasselbladParams.sharpness.toString(),
            "vignette" to hasselbladParams.vignette.toString()
        )
        
        // 基于亮度等级调整
        when (analysisResult.brightnessLevel) {
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_DARK -> {
                baseParams["iso"] = "1600"
                baseParams["noise_reduction"] = "35"
                baseParams["shadows"] = "30"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.DARK -> {
                baseParams["iso"] = "800"
                baseParams["noise_reduction"] = "20"
                baseParams["shadows"] = "15"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.NORMAL -> {
                baseParams["iso"] = "200"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.BRIGHT -> {
                baseParams["iso"] = "100"
                baseParams["highlights"] = "-15"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_BRIGHT -> {
                baseParams["iso"] = "50"
                baseParams["highlights"] = "-25"
            }
        }
        
        // 基于人脸数量调整
        if (analysisResult.faceCount > 0) {
            baseParams["focus_mode"] = "人脸优先"
            baseParams["skin_smooth"] = if (analysisResult.colorProfile.skinToneRatio > 0.1f) "25" else "15"
            
            // 人像场景添加额外参数
            if (sceneType == SceneType.PORTRAIT) {
                baseParams["aperture"] = "f/1.8"
                baseParams["shutter"] = "1/125"
                baseParams["white_balance"] = "自动"
            }
        }
        
        // 基于边缘密度调整清晰度
        if (analysisResult.edgeDensity > 0.25f) {
            baseParams["clarity"] = "${(analysisResult.edgeDensity * 30).toInt()}"
            baseParams["detail_enhance"] = "开启"
        }
        
        // 基于颜色画像调整色温
        if (analysisResult.colorProfile.warmthRatio > 0.5f) {
            baseParams["white_balance"] = "暖色调"
        } else if (analysisResult.colorProfile.warmthRatio < 0.2f) {
            baseParams["white_balance"] = "冷色调"
        }
        
        // 场景特定参数
        when (sceneType) {
            SceneType.LANDSCAPE -> {
                baseParams["aperture"] = "f/8"
                baseParams["shutter"] = "1/60"
                baseParams["focus_mode"] = "无穷远"
                baseParams["hdr"] = "开启"
            }
            SceneType.NIGHT -> {
                baseParams["aperture"] = "f/1.6"
                baseParams["shutter"] = "1/15"
                baseParams["stabilization"] = "开启"
                baseParams["long_exposure"] = "开启"
            }
            SceneType.FOOD -> {
                baseParams["aperture"] = "f/2.8"
                baseParams["shutter"] = "1/60"
                baseParams["saturation"] = "${hasselbladParams.saturation + 10}"
                baseParams["warmth"] = "${hasselbladParams.colorTemp + 15}"
            }
            SceneType.STREET -> {
                baseParams["aperture"] = "f/5.6"
                baseParams["shutter"] = "1/250"
                baseParams["focus_mode"] = "连续对焦"
                baseParams["style"] = "胶片"
            }
            SceneType.MACRO -> {
                baseParams["aperture"] = "f/2.8"
                baseParams["focus_mode"] = "手动对焦"
                baseParams["detail_enhance"] = "开启"
            }
            else -> {
                // 默认参数
                baseParams["iso"] = "自动"
                baseParams["shutter"] = "自动"
                baseParams["aperture"] = "自动"
                baseParams["white_balance"] = "自动"
            }
        }
        
        return baseParams
    }

    /**
     * 实时识别流
     * 用于相机预览时持续识别场景
     * 优化：使用 ensureActive() 让协程可被外部取消,避免泄漏
     */
    fun recognizeSceneStream(bitmap: Bitmap): Flow<SceneRecognitionResult> = flow {
        while (currentCoroutineIsActive()) {
            emit(recognizeScene(bitmap))
            kotlinx.coroutines.delay(500) // 每 500ms 识别一次
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 检查当前协程是否仍处于活动状态
     * (避免直接导入 currentCoroutineContext,提升可读性)
     */
    private suspend inline fun currentCoroutineIsActive(): Boolean {
        return kotlin.coroutines.coroutineContext[Job]?.isActive ?: true
    }

    /**
     * 切换哈苏之眼开关
     */
    fun toggleSceneRecognition(enabled: Boolean) {
        settingsManager.isAISceneRecognitionEnabled = enabled
    }

    companion object {
        private const val TAG = "SceneRecognitionManager"
        
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
 * 场景类型枚举
 */
enum class SceneType(val displayName: String, val icon: String) {
    UNKNOWN("未知", "❓"),
    PORTRAIT("人像", "👤"),
    LANDSCAPE("风景", "🏔️"),
    NIGHT("夜景", "🌃"),
    FOOD("美食", "🍜"),
    PET("宠物", "🐕"),
    DOCUMENT("文档", "📄"),
    BUILDING("建筑", "🏢"),
    FLOWER("花卉", "🌸"),
    SKY("天空", "☁️"),
    SNOW("雪景", "❄️"),
    BEACH("海滩", "🏖️"),
    SUNSET("日落", "🌅"),
    FIREWORKS("烟花", "🎆"),
    STAGE("舞台", "🎭"),
    SPORTS("运动", "⚽"),
    CHILDREN("儿童", "👶"),
    TEXT("文字", "📝"),
    GROUP_PHOTO("合影", "👥"),
    CANDLELIGHT("烛光", "🕯️"),
    AQUARIUM("水族馆", "🐠"),
    WATERFALL("瀑布", "💧"),
    STARRY_SKY("星空", "⭐"),
    TRAFFIC("车流", "🚗"),
    RAINBOW("彩虹", "🌈"),
    BACKLIGHT("逆光", "☀️"),
    MACRO("微距", "🔍"),
    PANORAMA("全景", "📷"),
    TIME_LAPSE("延时", "⏱️"),
    SLOW_MOTION("慢动作", "🎬"),
    PRO_MODE("专业", "⚙️"),
    STREET("街拍", "🚶"),
    CAFE("咖啡馆", "☕"),
    MUSEUM("博物馆", "🏛️"),
    CONCERT("演唱会", "🎵"),
    WEDDING("婚礼", "💒"),
    PARTY("派对", "🎉")
}

/**
 * 场景识别结果（扩展版）
 */
data class SceneRecognitionResult(
    val sceneType: SceneType,
    val confidence: Float,
    val recommendedParams: Map<String, String>,
    val isEnabled: Boolean,
    // 扩展信息（真实分析结果）
    val colorProfile: HeuristicSceneAnalyzer.ColorProfile? = null,
    val brightnessLevel: HeuristicSceneAnalyzer.BrightnessLevel? = null,
    val faceCount: Int = 0,
    val edgeDensity: Float = 0f,
    val alternativeScenes: List<SceneType> = emptyList(),
    val errorMessage: String? = null
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
    
    // 获取场景描述
    val sceneDescription: String
        get() = when {
            errorMessage != null -> "识别失败: $errorMessage"
            !isEnabled -> "AI场景识别已禁用"
            else -> "${sceneType.displayName} (${confidencePercent}%置信度)"
        }
    
    // 获取颜色分析描述
    val colorAnalysisDescription: String?
        get() = colorProfile?.let { cp ->
            val warmth = if (cp.warmthRatio > 0.5f) "暖色调主导" 
                         else if (cp.warmthRatio < 0.2f) "冷色调主导" 
                         else "色调平衡"
            val skin = if (cp.skinToneRatio > 0.05f) "，检测到肤色" else ""
            "$warmth$skin"
        }
    
    // 获取亮度分析描述
    val brightnessDescription: String?
        get() = brightnessLevel?.displayName
}
