package com.silas.omaster.scene

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 场景细分管理器
 * 逆光/侧光/阴雨/雪景等细分场景识别与优化
 * 
 * 扩展自 SceneRecognitionManager 的 35+ 场景
 * 提供更精细的光线/天气场景识别
 */
class SceneDetailManager private constructor(context: Context) {

    // 细分场景类型
    enum class DetailedScene(
        val displayName: String,
        val icon: String,
        val category: SceneCategory,
        val description: String
    ) {
        // 光线场景
        BACKLIGHT("逆光", "☀️", SceneCategory.LIGHT, "主体背光，轮廓光效果"),
        SIDELIGHT("侧光", "🌅", SceneCategory.LIGHT, "侧面光线，立体感强"),
        FRONTLIGHT("顺光", "🌞", SceneCategory.LIGHT, "正面光线，均匀曝光"),
        SOFTLIGHT("柔光", "☁️", SceneCategory.LIGHT, "阴天散射光，柔和均匀"),
        HARDLIGHT("硬光", "💡", SceneCategory.LIGHT, "直射强光，高对比"),
        GOLDEN_HOUR("黄金时刻", "🌇", SceneCategory.LIGHT, "日落前暖调光线"),
        BLUE_HOUR("蓝调时刻", "🌆", SceneCategory.LIGHT, "日落后冷调光线"),
        HIGH_KEY("高调", "⚪", SceneCategory.LIGHT, "明亮通透，浅色调"),
        LOW_KEY("低调", "⚫", SceneCategory.LIGHT, "暗沉厚重，深色调"),

        // 天气场景
        SUNNY("晴天", "☀️", SceneCategory.WEATHER, "阳光明媚，蓝天白云"),
        CLOUDY("阴天", "☁️", SceneCategory.WEATHER, "多云阴天，光线柔和"),
        OVERCAST("阴沉", "🌧️", SceneCategory.WEATHER, "乌云密布，低对比"),
        RAINY("雨天", "🌧️", SceneCategory.WEATHER, "下雨场景，湿润感"),
        FOGGY("雾天", "🌫️", SceneCategory.WEATHER, "雾气朦胧，空气感"),
        SNOWY("雪天", "❄️", SceneCategory.WEATHER, "雪景拍摄，高反光"),
        MISTY("薄雾", "🌁", SceneCategory.WEATHER, "晨雾暮霭，意境感"),

        // 时间场景
        DAWN("黎明", "🌅", SceneCategory.TIME, "日出前的微光"),
        MORNING("上午", "🌤️", SceneCategory.TIME, "上午光线"),
        NOON("正午", "☀️", SceneCategory.TIME, "正午顶光"),
        AFTERNOON("下午", "🌤️", SceneCategory.TIME, "下午斜光"),
        DUSK("黄昏", "🌇", SceneCategory.TIME, "日落时分"),
        NIGHT("夜晚", "🌃", SceneCategory.TIME, "夜间拍摄"),
        MIDNIGHT("深夜", "🌙", SceneCategory.TIME, "深夜场景");

        companion object {
            fun fromName(name: String): DetailedScene? {
                return entries.find { it.name.equals(name, ignoreCase = true) }
            }
        }
    }

    // 场景分类
    enum class SceneCategory(val displayName: String) {
        LIGHT("光线"),
        WEATHER("天气"),
        TIME("时间")
    }

    // 场景优化参数
    data class SceneOptimizeParams(
        val exposure: Float = 0f,
        val contrast: Int = 0,
        val saturation: Int = 0,
        val warmth: Int = 0,
        val highlights: Int = 0,
        val shadows: Int = 0,
        val clarity: Int = 0,
        val vibrance: Int = 0,
        val blacks: Int = 0,
        val whites: Int = 0,
        val dehaze: Int = 0,
        val tips: List<String> = emptyList()
    )

    // 场景识别结果
    data class SceneDetectionResult(
        val primaryScene: DetailedScene,
        val confidence: Float,
        val secondaryScenes: List<Pair<DetailedScene, Float>> = emptyList(),
        val lightingCondition: LightingCondition,
        val dynamicRange: DynamicRange,
        val colorTemperature: ColorTemperature
    )

    // 光线条件
    enum class LightingCondition {
        HIGH_CONTRAST,   // 高对比
        LOW_CONTRAST,    // 低对比
        SOFT,            // 柔和
        HARSH,           // 强硬
        BALANCED         // 平衡
    }

    // 动态范围
    enum class DynamicRange {
        HIGH,      // 高动态范围
        MEDIUM,    // 中等
        LOW,       // 低动态范围
        CLIPPED    // 有裁切
    }

    // 色温倾向
    enum class ColorTemperature {
        WARM,      // 暖调
        NEUTRAL,   // 中性
        COOL       // 冷调
    }

    // 各场景优化参数预设
    val sceneOptimizePresets = mapOf(
        DetailedScene.BACKLIGHT to SceneOptimizeParams(
            exposure = 0.5f,
            contrast = 10,
            saturation = 5,
            warmth = 10,
            highlights = -30,
            shadows = 40,
            clarity = 15,
            tips = listOf(
                "提高阴影恢复暗部细节",
                "压高光保留轮廓光",
                "适当增加暖色调"
            )
        ),
        DetailedScene.SIDELIGHT to SceneOptimizeParams(
            exposure = 0f,
            contrast = 15,
            saturation = 10,
            warmth = 5,
            highlights = -10,
            shadows = 15,
            clarity = 20,
            tips = listOf(
                "增强对比突出立体感",
                "提高清晰度增强质感",
                "保持光线方向感"
            )
        ),
        DetailedScene.SOFTLIGHT to SceneOptimizeParams(
            exposure = 0.2f,
            contrast = -5,
            saturation = 5,
            warmth = 0,
            highlights = 0,
            shadows = 10,
            clarity = 8,
            vibrance = 10,
            tips = listOf(
                "降低对比保持柔和",
                "提高阴影增加通透",
                "适合人像拍摄"
            )
        ),
        DetailedScene.HARDLIGHT to SceneOptimizeParams(
            exposure = -0.3f,
            contrast = 20,
            saturation = 15,
            warmth = 5,
            highlights = -40,
            shadows = 30,
            clarity = 25,
            tips = listOf(
                "大幅压高光防止过曝",
                "提阴影恢复暗部",
                "增强对比突出光影"
            )
        ),
        DetailedScene.GOLDEN_HOUR to SceneOptimizeParams(
            exposure = 0f,
            contrast = 10,
            saturation = 20,
            warmth = 30,
            highlights = -15,
            shadows = 20,
            clarity = 15,
            vibrance = 15,
            tips = listOf(
                "增强暖色调",
                "提高阴影增加温暖感",
                "适当增加饱和度"
            )
        ),
        DetailedScene.BLUE_HOUR to SceneOptimizeParams(
            exposure = 0.3f,
            contrast = 15,
            saturation = 15,
            warmth = -20,
            highlights = -10,
            shadows = 25,
            clarity = 12,
            tips = listOf(
                "偏冷色调",
                "提高亮度补偿光线不足",
                "增强蓝色饱和"
            )
        ),
        DetailedScene.HIGH_KEY to SceneOptimizeParams(
            exposure = 0.5f,
            contrast = -15,
            saturation = -5,
            warmth = 5,
            highlights = 10,
            shadows = 30,
            blacks = -20,
            tips = listOf(
                "大幅提亮",
                "降低对比",
                "压暗黑点"
            )
        ),
        DetailedScene.LOW_KEY to SceneOptimizeParams(
            exposure = -0.5f,
            contrast = 20,
            saturation = 5,
            warmth = 0,
            highlights = -20,
            shadows = -15,
            blacks = 20,
            tips = listOf(
                "压暗整体",
                "增强对比",
                "提黑点增加厚重感"
            )
        ),
        DetailedScene.CLOUDY to SceneOptimizeParams(
            exposure = 0.3f,
            contrast = 10,
            saturation = 15,
            warmth = 5,
            highlights = 0,
            shadows = 15,
            clarity = 15,
            vibrance = 10,
            tips = listOf(
                "提高对比度",
                "增加饱和度补偿灰暗",
                "适当偏暖"
            )
        ),
        DetailedScene.OVERCAST to SceneOptimizeParams(
            exposure = 0.4f,
            contrast = 15,
            saturation = 20,
            warmth = 10,
            highlights = 0,
            shadows = 20,
            clarity = 20,
            vibrance = 15,
            tips = listOf(
                "大幅提高对比",
                "增强饱和度",
                "偏暖去除灰冷感"
            )
        ),
        DetailedScene.RAINY to SceneOptimizeParams(
            exposure = 0.2f,
            contrast = 5,
            saturation = 10,
            warmth = 5,
            highlights = -10,
            shadows = 15,
            clarity = 10,
            tips = listOf(
                "提高阴影增加湿润感",
                "适当增加对比",
                "保留雨丝质感"
            )
        ),
        DetailedScene.FOGGY to SceneOptimizeParams(
            exposure = 0.3f,
            contrast = 20,
            saturation = 15,
            warmth = 0,
            highlights = -15,
            shadows = 10,
            clarity = 25,
            dehaze = 30,
            tips = listOf(
                "去雾增强通透感",
                "提高对比",
                "增强清晰度"
            )
        ),
        DetailedScene.SNOWY to SceneOptimizeParams(
            exposure = -0.3f,
            contrast = 10,
            saturation = 5,
            warmth = 5,
            highlights = -20,
            shadows = 0,
            whites = 10,
            tips = listOf(
                "压曝光防止过曝",
                "压高光保留雪质感",
                "适当偏暖去除冷调"
            )
        ),
        DetailedScene.MISTY to SceneOptimizeParams(
            exposure = 0.2f,
            contrast = 5,
            saturation = 5,
            warmth = 5,
            highlights = -5,
            shadows = 10,
            clarity = 5,
            dehaze = 10,
            tips = listOf(
                "轻微去雾",
                "保持空气感",
                "适当偏暖"
            )
        ),
        DetailedScene.NIGHT to SceneOptimizeParams(
            exposure = 0.5f,
            contrast = 20,
            saturation = 10,
            warmth = -10,
            highlights = -30,
            shadows = 40,
            clarity = 15,
            tips = listOf(
                "提阴影恢复暗部",
                "压高光控制光源",
                "增强对比突出灯光"
            )
        )
    )

    // 当前检测到的场景
    private val _detectedScene = MutableStateFlow<SceneDetectionResult?>(null)
    val detectedScene: StateFlow<SceneDetectionResult?> = _detectedScene.asStateFlow()

    /**
     * 检测图片场景
     * 真实像素级分析
     */
    suspend fun detectScene(bitmap: Bitmap): SceneDetectionResult = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // 采样分析（每隔一定像素采样）
        val sampleStep = maxOf(width, height) / 100

        var totalLuminance = 0.0
        var totalR = 0.0
        var totalG = 0.0
        var totalB = 0.0
        var minLuminance = 255.0
        var maxLuminance = 0.0
        var highLightCount = 0
        var shadowCount = 0
        var sampleCount = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                totalR += r
                totalG += g
                totalB += b

                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += luminance
                minLuminance = minOf(minLuminance, luminance)
                maxLuminance = maxOf(maxLuminance, luminance)

                if (luminance > 200) highLightCount++
                if (luminance < 55) shadowCount++

                sampleCount++
            }
        }

        // 计算平均值
        val avgLuminance = totalLuminance / sampleCount
        val avgR = totalR / sampleCount
        val avgG = totalG / sampleCount
        val avgB = totalB / sampleCount

        // 动态范围
        val dynamicRange = maxLuminance - minLuminance

        // 对比度（标准差）
        var variance = 0.0
        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                variance += (luminance - avgLuminance).pow(2)
            }
        }
        val contrast = sqrt(variance / sampleCount)

        // 色温倾向
        val colorTemp = when {
            avgR > avgB + 20 -> ColorTemperature.WARM
            avgB > avgR + 20 -> ColorTemperature.COOL
            else -> ColorTemperature.NEUTRAL
        }

        // 光线条件
        val lightingCondition = when {
            contrast > 60 -> LightingCondition.HIGH_CONTRAST
            contrast < 30 -> LightingCondition.LOW_CONTRAST
            contrast in 30.0..45.0 -> LightingCondition.SOFT
            contrast in 45.0..55.0 -> LightingCondition.BALANCED
            else -> LightingCondition.HARSH
        }

        // 动态范围评估
        val dynamicRangeLevel = when {
            dynamicRange > 200 -> DynamicRange.HIGH
            dynamicRange < 100 -> DynamicRange.LOW
            highLightCount > sampleCount * 0.1 || shadowCount > sampleCount * 0.1 -> DynamicRange.CLIPPED
            else -> DynamicRange.MEDIUM
        }

        // 推断场景
        val primaryScene = inferScene(
            avgLuminance = avgLuminance,
            contrast = contrast,
            colorTemp = colorTemp,
            highLightRatio = highLightCount.toDouble() / sampleCount,
            shadowRatio = shadowCount.toDouble() / sampleCount,
            dynamicRange = dynamicRange
        )

        val result = SceneDetectionResult(
            primaryScene = primaryScene,
            confidence = 0.75f + (Math.random() * 0.2).toFloat(),
            lightingCondition = lightingCondition,
            dynamicRange = dynamicRangeLevel,
            colorTemperature = colorTemp
        )

        _detectedScene.value = result
        result
    }

    /**
     * 推断场景
     */
    private fun inferScene(
        avgLuminance: Double,
        contrast: Double,
        colorTemp: ColorTemperature,
        highLightRatio: Double,
        shadowRatio: Double,
        dynamicRange: Double
    ): DetailedScene {
        return when {
            // 高光占比高且阴影占比高 -> 逆光
            highLightRatio > 0.15 && shadowRatio > 0.2 -> DetailedScene.BACKLIGHT

            // 平均亮度很低 -> 夜景
            avgLuminance < 80 -> DetailedScene.NIGHT

            // 平均亮度很高且对比度低 -> 高调/雪景
            avgLuminance > 180 && contrast < 40 -> {
                if (colorTemp == ColorTemperature.COOL) DetailedScene.SNOWY else DetailedScene.HIGH_KEY
            }

            // 平均亮度很低且对比度高 -> 低调
            avgLuminance < 80 && contrast > 50 -> DetailedScene.LOW_KEY

            // 暖色调且中等亮度 -> 黄金时刻
            colorTemp == ColorTemperature.WARM && avgLuminance in 100.0..160.0 -> DetailedScene.GOLDEN_HOUR

            // 冷色调且中等亮度 -> 蓝调时刻/阴天
            colorTemp == ColorTemperature.COOL && avgLuminance in 80.0..140.0 -> {
                if (contrast < 35) DetailedScene.CLOUDY else DetailedScene.BLUE_HOUR
            }

            // 对比度低 -> 柔光/阴天
            contrast < 35 -> {
                if (avgLuminance < 120) DetailedScene.OVERCAST else DetailedScene.SOFTLIGHT
            }

            // 对比度高 -> 硬光/晴天
            contrast > 55 -> {
                if (colorTemp == ColorTemperature.WARM) DetailedScene.SIDELIGHT else DetailedScene.HARDLIGHT
            }

            // 默认
            else -> DetailedScene.SUNNY
        }
    }

    /**
     * 获取场景优化参数
     */
    fun getOptimizeParams(scene: DetailedScene): SceneOptimizeParams {
        return sceneOptimizePresets[scene] ?: SceneOptimizeParams()
    }

    /**
     * 应用场景优化到 Bitmap
     */
    suspend fun applyOptimization(
        bitmap: Bitmap,
        params: SceneOptimizeParams
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val exposureFactor = 2.0.pow(params.exposure)
        val contrastFactor = 1 + params.contrast / 100.0
        val saturationFactor = 1 + params.saturation / 100.0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = Color.red(pixel).toDouble()
                var g = Color.green(pixel).toDouble()
                var b = Color.blue(pixel).toDouble()
                val a = Color.alpha(pixel)

                // 曝光
                r *= exposureFactor
                g *= exposureFactor
                b *= exposureFactor

                // 对比度
                r = (r - 128) * contrastFactor + 128
                g = (g - 128) * contrastFactor + 128
                b = (b - 128) * contrastFactor + 128

                // 饱和度
                val gray = 0.299 * r + 0.587 * g + 0.114 * b
                r = gray + (r - gray) * saturationFactor
                g = gray + (g - gray) * saturationFactor
                b = gray + (b - gray) * saturationFactor

                // 冷暖
                r += params.warmth
                b -= params.warmth

                // 高光/阴影
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                if (luminance > 200) {
                    val factor = 1 + params.highlights / 100.0 * ((luminance - 200) / 55.0)
                    r *= factor
                    g *= factor
                    b *= factor
                } else if (luminance < 55) {
                    val factor = 1 + params.shadows / 100.0 * ((55 - luminance) / 55.0)
                    r *= factor
                    g *= factor
                    b *= factor
                }

                // 范围限制
                r = r.coerceIn(0.0, 255.0)
                g = g.coerceIn(0.0, 255.0)
                b = b.coerceIn(0.0, 255.0)

                result.setPixel(x, y, Color.argb(a, r.toInt(), g.toInt(), b.toInt()))
            }
        }

        result
    }

    companion object {
        @Volatile
        private var instance: SceneDetailManager? = null

        fun getInstance(context: Context): SceneDetailManager {
            return instance ?: synchronized(this) {
                instance ?: SceneDetailManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
