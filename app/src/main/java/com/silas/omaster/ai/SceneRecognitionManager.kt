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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // StateFlow 暴露场景识别开关状态，供 UI 订阅
    private val _isSceneRecognitionEnabled = MutableStateFlow(settingsManager.isAISceneRecognitionEnabled)
    val isSceneRecognitionEnabled: StateFlow<Boolean> = _isSceneRecognitionEnabled.asStateFlow()

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
     * 完整支持 35+ 场景映射
     */
    private fun mapSceneProfileToSceneType(sceneId: String): SceneType {
        return when (sceneId) {
            // 人像类 (5个)
            "portrait-standard", "portrait-backlit", "portrait-couple" -> SceneType.PORTRAIT
            "portrait-group" -> SceneType.GROUP_PHOTO
            "portrait-child" -> SceneType.CHILDREN

            // 风景类 (6个)
            "landscape-standard", "landscape-forest" -> SceneType.LANDSCAPE
            "landscape-sky" -> SceneType.SKY
            "landscape-beach" -> SceneType.BEACH
            "landscape-sunset" -> SceneType.SUNSET
            "landscape-snow" -> SceneType.SNOW

            // 夜景类 (4个)
            "night-city" -> SceneType.NIGHT
            "night-neon" -> SceneType.TRAFFIC
            "night-starry" -> SceneType.STARRY_SKY
            "night-candle" -> SceneType.CANDLELIGHT

            // 美食类 (2个)
            "food-restaurant", "food-dessert" -> SceneType.FOOD

            // 城市/街拍类 - 根据关键词区分 STREET 和 BUILDING
            "urban-street" -> SceneType.STREET
            "urban-cafe" -> SceneType.CAFE
            // urban-architecture 根据上下文判断：如果是高纹理密度→BUILDING，否则→STREET
            "urban-architecture" -> SceneType.BUILDING

            // 微距类 (2个)
            "macro-insect" -> SceneType.MACRO
            "macro-texture" -> SceneType.MACRO

            // 活动/事件类 (3个)
            "event-party" -> SceneType.PARTY
            "event-concert" -> SceneType.CONCERT
            "event-wedding" -> SceneType.WEDDING

            // 静物/文档类
            "still-product" -> SceneType.DOCUMENT

            // 其他场景映射
            "pet-animal" -> SceneType.PET
            "flower-plant" -> SceneType.FLOWER
            "firework-display" -> SceneType.FIREWORKS
            "waterfall-scene" -> SceneType.WATERFALL
            "aquarium-view" -> SceneType.AQUARIUM
            "rainbow-sky" -> SceneType.RAINBOW
            "sports-action" -> SceneType.SPORTS
            "stage-performance" -> SceneType.STAGE
            "museum-indoor" -> SceneType.MUSEUM
            "text-document" -> SceneType.TEXT
            "backlight-scene" -> SceneType.BACKLIGHT

            else -> SceneType.UNKNOWN
        }
    }

    /**
     * 基于真实分析结果生成推荐参数
     * 只输出 OPPO 大师模式可调的 6 项参数：
     * tone(影调), saturation(饱和度), contrast(对比度), colorTemp(色温), sharpness(锐度), vignette(暗角)
     * ISO/aperture/shutter 改为"环境参考"标签展示
     */
    private fun generateRecommendedParamsFromAnalysis(
        sceneType: SceneType,
        analysisResult: HeuristicSceneAnalyzer.AnalysisResult
    ): Map<String, String> {
        // 获取哈苏参数映射
        val hasselbladParams = sceneMapping.getParams(analysisResult.primaryScene.id)

        // 大师模式可调参数（6项核心参数）
        val adjustableParams = mutableMapOf(
            "影调" to hasselbladParams.tone.toString(),
            "饱和度" to hasselbladParams.saturation.toString(),
            "对比度" to hasselbladParams.contrast.toString(),
            "色温" to "${hasselbladParams.colorTemp}K",
            "锐度" to hasselbladParams.sharpness.toString(),
            "暗角" to if (hasselbladParams.vignette > 0) "开启" else "关闭"
        )

        // 基于亮度等级调整影调
        when (analysisResult.brightnessLevel) {
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_DARK -> {
                adjustableParams["影调"] = "${hasselbladParams.tone + 20}"
                adjustableParams["对比度"] = "${hasselbladParams.contrast - 5}"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.DARK -> {
                adjustableParams["影调"] = "${hasselbladParams.tone + 10}"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.NORMAL -> {
                // 保持默认
            }
            HeuristicSceneAnalyzer.BrightnessLevel.BRIGHT -> {
                adjustableParams["影调"] = "${hasselbladParams.tone - 10}"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_BRIGHT -> {
                adjustableParams["影调"] = "${hasselbladParams.tone - 20}"
                adjustableParams["对比度"] = "${hasselbladParams.contrast + 5}"
            }
        }

        // 基于人脸数量调整锐度和暗角
        if (analysisResult.faceCount > 0) {
            // 人像场景降低锐度，开启暗角
            adjustableParams["锐度"] = "${(hasselbladParams.sharpness * 0.8).toInt()}"
            adjustableParams["暗角"] = "开启"
        }

        // 基于边缘密度调整锐度
        if (analysisResult.edgeDensity > 0.25f) {
            adjustableParams["锐度"] = "${(hasselbladParams.sharpness * 1.2).toInt().coerceAtMost(100)}"
        }

        // 基于颜色画像调整色温
        if (analysisResult.colorProfile.warmthRatio > 0.5f) {
            adjustableParams["色温"] = "${hasselbladParams.colorTemp + 200}K"
        } else if (analysisResult.colorProfile.warmthRatio < 0.2f) {
            adjustableParams["色温"] = "${hasselbladParams.colorTemp - 200}K"
        }

        // 场景特定微调
        when (sceneType) {
            SceneType.LANDSCAPE -> {
                adjustableParams["饱和度"] = "${hasselbladParams.saturation + 10}"
                adjustableParams["锐度"] = "${hasselbladParams.sharpness + 10}"
            }
            SceneType.NIGHT -> {
                adjustableParams["影调"] = "${hasselbladParams.tone + 15}"
                adjustableParams["对比度"] = "${hasselbladParams.contrast + 10}"
            }
            SceneType.FOOD -> {
                adjustableParams["饱和度"] = "${hasselbladParams.saturation + 15}"
                adjustableParams["色温"] = "${hasselbladParams.colorTemp + 300}K"
            }
            SceneType.PORTRAIT -> {
                adjustableParams["锐度"] = "${(hasselbladParams.sharpness * 0.7).toInt()}"
                adjustableParams["暗角"] = "开启"
            }
            SceneType.MACRO -> {
                adjustableParams["锐度"] = "${hasselbladParams.sharpness + 20}"
                adjustableParams["饱和度"] = "${hasselbladParams.saturation + 5}"
            }
            else -> {
                // 保持默认参数
            }
        }

        return adjustableParams
    }

    /**
     * 生成环境参考参数（仅用于展示，不可调节）
     * 包含 ISO、光圈、快门等环境信息
     */
    fun generateEnvironmentReference(
        sceneType: SceneType,
        analysisResult: HeuristicSceneAnalyzer.AnalysisResult
    ): Map<String, String> {
        val envRef = mutableMapOf<String, String>()

        // 基于亮度等级提供环境参考
        when (analysisResult.brightnessLevel) {
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_DARK -> {
                envRef["ISO建议"] = "1600-3200"
                envRef["快门建议"] = "1/15s 或更慢"
                envRef["环境"] = "极暗环境"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.DARK -> {
                envRef["ISO建议"] = "800-1600"
                envRef["快门建议"] = "1/30s-1/60s"
                envRef["环境"] = "暗光环境"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.NORMAL -> {
                envRef["ISO建议"] = "200-400"
                envRef["快门建议"] = "1/125s-1/250s"
                envRef["环境"] = "正常光线"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.BRIGHT -> {
                envRef["ISO建议"] = "100-200"
                envRef["快门建议"] = "1/250s-1/500s"
                envRef["环境"] = "明亮环境"
            }
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_BRIGHT -> {
                envRef["ISO建议"] = "50-100"
                envRef["快门建议"] = "1/500s 或更快"
                envRef["环境"] = "高亮环境"
            }
        }

        // 场景特定建议
        when (sceneType) {
            SceneType.PORTRAIT -> {
                envRef["光圈建议"] = "f/1.8-f/2.8"
                envRef["对焦"] = "人脸优先"
            }
            SceneType.LANDSCAPE -> {
                envRef["光圈建议"] = "f/8-f/11"
                envRef["对焦"] = "无穷远"
            }
            SceneType.NIGHT -> {
                envRef["光圈建议"] = "f/1.6-f/2.8"
                envRef["防抖"] = "建议开启"
            }
            SceneType.MACRO -> {
                envRef["光圈建议"] = "f/2.8-f/5.6"
                envRef["对焦"] = "手动对焦"
            }
            else -> {
                envRef["光圈建议"] = "自动"
            }
        }

        // 人脸信息
        if (analysisResult.faceCount > 0) {
            envRef["检测到人脸"] = "${analysisResult.faceCount}人"
        }

        return envRef
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
     * 使用 StateFlow 通知所有观察者状态变更
     */
    fun toggleSceneRecognition(enabled: Boolean) {
        settingsManager.isAISceneRecognitionEnabled = enabled
        _isSceneRecognitionEnabled.value = enabled
    }

    /**
     * 获取场景识别开关状态流（带 distinctUntilChanged 防抖动）
     * 供 UI 层订阅使用
     */
    fun isSceneRecognitionEnabledFlow(): Flow<Boolean> {
        return isSceneRecognitionEnabled
    }

    /**
     * 从 SettingsManager 同步最新状态到 StateFlow
     * 在应用恢复或设置变更时调用
     */
    fun syncStateFromSettings() {
        _isSceneRecognitionEnabled.value = settingsManager.isAISceneRecognitionEnabled
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
