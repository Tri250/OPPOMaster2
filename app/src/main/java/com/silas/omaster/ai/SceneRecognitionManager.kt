package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * AI 场景识别管理器
 * 支持 35+ 拍摄场景智能识别
 */
class SceneRecognitionManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val sceneDetector = SceneDetector()

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
     * 识别图片场景
     * @param bitmap 待识别图片
     * @return 识别结果
     */
    suspend fun recognizeScene(bitmap: Bitmap): SceneRecognitionResult = withContext(Dispatchers.Default) {
        if (!settingsManager.isAISceneRecognitionEnabled) {
            return@withContext SceneRecognitionResult(
                sceneType = SceneType.UNKNOWN,
                confidence = 0f,
                recommendedParams = emptyMap(),
                isEnabled = false
            )
        }

        // 模拟 AI 识别过程（实际项目中应调用 TensorFlow Lite 模型）
        delay(300) // 模拟处理时间
        
        val detectedScene = sceneDetector.detect(bitmap)
        val recommendedParams = generateRecommendedParams(detectedScene)
        
        SceneRecognitionResult(
            sceneType = detectedScene,
            confidence = (0.75f + Math.random() * 0.24f).toFloat(),
            recommendedParams = recommendedParams,
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
     * 获取场景推荐参数
     */
    private fun generateRecommendedParams(sceneType: SceneType): Map<String, String> {
        return when (sceneType) {
            SceneType.PORTRAIT -> mapOf(
                "iso" to "100",
                "shutter" to "1/125",
                "aperture" to "f/1.8",
                "whiteBalance" to "自动",
                "focus" to "人脸优先",
                "beauty" to "自然"
            )
            SceneType.LANDSCAPE -> mapOf(
                "iso" to "50",
                "shutter" to "1/60",
                "aperture" to "f/8",
                "whiteBalance" to "日光",
                "focus" to "无穷远",
                "hdr" to "开启"
            )
            SceneType.NIGHT -> mapOf(
                "iso" to "1600",
                "shutter" to "1/15",
                "aperture" to "f/1.6",
                "whiteBalance" to "自动",
                "noise" to "降噪开启",
                "stabilization" to "开启"
            )
            SceneType.FOOD -> mapOf(
                "iso" to "200",
                "shutter" to "1/60",
                "aperture" to "f/2.8",
                "whiteBalance" to "暖色调",
                "saturation" to "+10",
                "contrast" to "+5"
            )
            SceneType.STREET -> mapOf(
                "iso" to "400",
                "shutter" to "1/250",
                "aperture" to "f/5.6",
                "whiteBalance" to "自动",
                "focus" to "连续对焦",
                "style" to "胶片"
            )
            else -> mapOf(
                "iso" to "自动",
                "shutter" to "自动",
                "aperture" to "自动",
                "whiteBalance" to "自动"
            )
        }
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
 * 场景识别结果
 */
data class SceneRecognitionResult(
    val sceneType: SceneType,
    val confidence: Float,
    val recommendedParams: Map<String, String>,
    val isEnabled: Boolean
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
}

/**
 * 场景检测器（模拟实现）
 * 实际项目中应使用 TensorFlow Lite 模型
 */
private class SceneDetector {
    fun detect(bitmap: Bitmap): SceneType {
        // 模拟检测逻辑
        // 实际项目中应调用 ML 模型进行推理
        val randomIndex = (Math.random() * 10).toInt()
        return when (randomIndex) {
            0 -> SceneType.PORTRAIT
            1 -> SceneType.LANDSCAPE
            2 -> SceneType.NIGHT
            3 -> SceneType.FOOD
            4 -> SceneType.STREET
            5 -> SceneType.BUILDING
            6 -> SceneType.FLOWER
            7 -> SceneType.SKY
            8 -> SceneType.SUNSET
            else -> SceneType.UNKNOWN
        }
    }
}
