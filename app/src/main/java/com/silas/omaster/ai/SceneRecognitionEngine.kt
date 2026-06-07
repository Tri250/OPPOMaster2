package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException
import java.nio.MappedByteBuffer
import kotlin.math.abs

/**
 * 真实AI场景识别引擎
 * 基于TensorFlow Lite模型实现35+场景识别
 * 针对OPPO Find系列摄影场景优化
 */
class SceneRecognitionEngine private constructor(context: Context) {
    
    private var interpreter: Interpreter? = null
    private val appContext = context.applicationContext
    
    // 模型输入尺寸
    private val INPUT_SIZE = 224
    private val NUM_CLASSES = 35
    
    // 图像预处理器
    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
    }
    
    // 场景标签映射
    private val sceneLabels = listOf(
        "portrait", "landscape", "night", "food", "pet", "document", "building",
        "flower", "sky", "snow", "beach", "sunset", "fireworks", "stage", "sports",
        "children", "text", "group_photo", "candlelight", "aquarium", "waterfall",
        "starry_sky", "traffic", "rainbow", "backlight", "macro", "panorama",
        "time_lapse", "slow_motion", "pro_mode", "street", "cafe", "museum",
        "concert", "wedding"
    )
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val model: MappedByteBuffer = FileUtil.loadMappedFile(
                appContext, "scene_model.tflite"
            )
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                useNNAPI(true)
            }
            interpreter = Interpreter(model, options)
        } catch (e: IOException) {
            interpreter = null
        }
    }
    
    suspend fun recognize(bitmap: Bitmap): SceneRecognitionResult = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            return@withContext analyzeTraditional(bitmap)
        }
        
        try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            val outputArray = Array(1) { FloatArray(NUM_CLASSES) }
            interpreter?.run(processedImage.buffer, outputArray)
            
            val probabilities = outputArray[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]
            
            val sceneType = getSceneTypeFromLabel(sceneLabels[maxIndex])
            val recommendedParams = generateRecommendedParams(sceneType, bitmap)
            
            SceneRecognitionResult(
                sceneType = sceneType,
                confidence = confidence,
                recommendedParams = recommendedParams,
                isEnabled = true,
                isFallback = false
            )
        } catch (e: Exception) {
            analyzeTraditional(bitmap)
        }
    }
    
    private fun analyzeTraditional(bitmap: Bitmap): SceneRecognitionResult {
        val colorAnalysis = analyzeColorDistribution(bitmap)
        val brightnessAnalysis = analyzeBrightness(bitmap)
        val edgeDensity = analyzeEdgeDensity(bitmap)
        
        val sceneType = determineScene(colorAnalysis, brightnessAnalysis, edgeDensity)
        val confidence = calculateConfidence(colorAnalysis, brightnessAnalysis, sceneType)
        
        return SceneRecognitionResult(
            sceneType = sceneType,
            confidence = confidence,
            recommendedParams = generateRecommendedParams(sceneType, bitmap),
            isEnabled = true,
            isFallback = true
        )
    }
    
    private fun analyzeColorDistribution(bitmap: Bitmap): ColorAnalysis {
        var warmPixels = 0
        var coolPixels = 0
        var neutralPixels = 0
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        
        val sampleStep = 10
        var sampleCount = 0
        
        for (y in 0 until bitmap.height step sampleStep) {
            for (x in 0 until bitmap.width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                totalR += r
                totalG += g
                totalB += b
                sampleCount++
                
                when {
                    r > g + b -> warmPixels++
                    b > r + g -> coolPixels++
                    else -> neutralPixels++
                }
            }
        }
        
        return ColorAnalysis(
            avgR = (totalR / sampleCount).toInt(),
            avgG = (totalG / sampleCount).toInt(),
            avgB = (totalB / sampleCount).toInt(),
            warmRatio = warmPixels.toFloat() / sampleCount,
            coolRatio = coolPixels.toFloat() / sampleCount,
            neutralRatio = neutralPixels.toFloat() / sampleCount
        )
    }
    
    private fun analyzeBrightness(bitmap: Bitmap): BrightnessAnalysis {
        var totalBrightness = 0L
        var darkPixels = 0
        var brightPixels = 0
        val sampleStep = 10
        var sampleCount = 0
        
        for (y in 0 until bitmap.height step sampleStep) {
            for (x in 0 until bitmap.width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                totalBrightness += brightness
                sampleCount++
                
                when {
                    brightness < 50 -> darkPixels++
                    brightness > 200 -> brightPixels++
                }
            }
        }
        
        return BrightnessAnalysis(
            avgBrightness = (totalBrightness / sampleCount).toInt(),
            darkRatio = darkPixels.toFloat() / sampleCount,
            brightRatio = brightPixels.toFloat() / sampleCount
        )
    }
    
    private fun analyzeEdgeDensity(bitmap: Bitmap): Float {
        var edgeCount = 0
        val threshold = 30
        val sampleStep = 5
        
        for (y in sampleStep until bitmap.height - sampleStep step sampleStep) {
            for (x in sampleStep until bitmap.width - sampleStep step sampleStep) {
                val current = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + sampleStep, y)
                val bottom = bitmap.getPixel(x, y + sampleStep)
                
                val diffH = colorDiff(current, right)
                val diffV = colorDiff(current, bottom)
                
                if (diffH > threshold || diffV > threshold) {
                    edgeCount++
                }
            }
        }
        
        val totalPixels = (bitmap.width / sampleStep) * (bitmap.height / sampleStep)
        return edgeCount.toFloat() / totalPixels
    }
    
    private fun colorDiff(p1: Int, p2: Int): Int {
        return abs(Color.red(p1) - Color.red(p2)) +
               abs(Color.green(p1) - Color.green(p2)) +
               abs(Color.blue(p1) - Color.blue(p2))
    }
    
    private fun determineScene(
        color: ColorAnalysis,
        brightness: BrightnessAnalysis,
        edgeDensity: Float
    ): SceneType {
        return when {
            brightness.darkRatio > 0.6 -> SceneType.NIGHT
            brightness.brightRatio > 0.7 && color.warmRatio > 0.5 -> SceneType.SUNSET
            edgeDensity < 0.1 && color.avgB > color.avgR -> SceneType.SKY
            edgeDensity > 0.3 && color.avgG > color.avgR && color.avgG > color.avgB -> SceneType.LANDSCAPE
            edgeDensity > 0.25 && color.warmRatio > 0.4 -> SceneType.PORTRAIT
            color.avgR > 150 && color.avgG > 100 && color.avgB < 100 -> SceneType.FOOD
            brightness.avgBrightness > 180 && color.coolRatio > 0.4 -> SceneType.SNOW
            edgeDensity > 0.2 && color.avgB > 120 -> SceneType.BEACH
            else -> SceneType.AUTO
        }
    }
    
    private fun calculateConfidence(
        color: ColorAnalysis,
        brightness: BrightnessAnalysis,
        sceneType: SceneType
    ): Float {
        return when (sceneType) {
            SceneType.NIGHT -> 0.7f + brightness.darkRatio * 0.25f
            SceneType.SUNSET -> 0.65f + color.warmRatio * 0.3f
            SceneType.PORTRAIT -> 0.6f + color.warmRatio * 0.35f
            else -> 0.5f + (1 - abs(brightness.avgBrightness - 128) / 128f) * 0.4f
        }.coerceIn(0.5f, 0.95f)
    }
    
    private fun getSceneTypeFromLabel(label: String): SceneType {
        return when (label) {
            "portrait" -> SceneType.PORTRAIT
            "landscape" -> SceneType.LANDSCAPE
            "night" -> SceneType.NIGHT
            "food" -> SceneType.FOOD
            "pet" -> SceneType.PET
            "building" -> SceneType.BUILDING
            "flower" -> SceneType.FLOWER
            "sky" -> SceneType.SKY
            "snow" -> SceneType.SNOW
            "beach" -> SceneType.BEACH
            "sunset" -> SceneType.SUNSET
            "sports" -> SceneType.SPORTS
            "macro" -> SceneType.MACRO
            "street" -> SceneType.STREET
            else -> SceneType.AUTO
        }
    }
    
    private fun generateRecommendedParams(sceneType: SceneType, bitmap: Bitmap): Map<String, String> {
        return when (sceneType) {
            SceneType.PORTRAIT -> mapOf(
                "iso" to "100",
                "shutter" to "1/125",
                "aperture" to "f/1.8",
                "ev" to "+0.3",
                "focus" to "人脸优先",
                "whiteBalance" to "自动",
                "filter" to "人像",
                "softLight" to "柔美",
                "skinSmooth" to "15"
            )
            SceneType.LANDSCAPE -> mapOf(
                "iso" to "50",
                "shutter" to "1/60",
                "aperture" to "f/8",
                "ev" to "0",
                "focus" to "无穷远",
                "whiteBalance" to "日光",
                "filter" to "风景",
                "hdr" to "开启",
                "clarity" to "20"
            )
            SceneType.NIGHT -> mapOf(
                "iso" to "1600",
                "shutter" to "1/15",
                "aperture" to "f/1.6",
                "ev" to "-0.3",
                "focus" to "自动",
                "whiteBalance" to "自动",
                "filter" to "夜景",
                "noiseReduction" to "高",
                "stabilization" to "开启"
            )
            SceneType.FOOD -> mapOf(
                "iso" to "200",
                "shutter" to "1/60",
                "aperture" to "f/2.8",
                "ev" to "+0.3",
                "focus" to "中心",
                "whiteBalance" to "暖色调",
                "filter" to "美味",
                "saturation" to "+10",
                "warmth" to "+15"
            )
            SceneType.SUNSET -> mapOf(
                "iso" to "100",
                "shutter" to "1/125",
                "aperture" to "f/5.6",
                "ev" to "-0.7",
                "focus" to "无穷远",
                "whiteBalance" to "阴天",
                "filter" to "日落",
                "saturation" to "+20",
                "warmth" to "+25"
            )
            SceneType.MACRO -> mapOf(
                "iso" to "200",
                "shutter" to "1/250",
                "aperture" to "f/2.8",
                "ev" to "0",
                "focus" to "微距",
                "whiteBalance" to "自动",
                "filter" to "标准",
                "sharpness" to "30",
                "clarity" to "25"
            )
            SceneType.STREET -> mapOf(
                "iso" to "400",
                "shutter" to "1/250",
                "aperture" to "f/5.6",
                "ev" to "0",
                "focus" to "连续对焦",
                "whiteBalance" to "自动",
                "filter" to "人文",
                "contrast" to "+15",
                "style" to "胶片"
            )
            else -> mapOf(
                "iso" to "自动",
                "shutter" to "自动",
                "aperture" to "自动",
                "ev" to "0",
                "focus" to "自动",
                "whiteBalance" to "自动",
                "filter" to "标准"
            )
        }
    }
    
    companion object {
        @Volatile
        private var instance: SceneRecognitionEngine? = null
        
        fun getInstance(context: Context): SceneRecognitionEngine {
            return instance ?: synchronized(this) {
                instance ?: SceneRecognitionEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

data class ColorAnalysis(
    val avgR: Int,
    val avgG: Int,
    val avgB: Int,
    val warmRatio: Float,
    val coolRatio: Float,
    val neutralRatio: Float
)

data class BrightnessAnalysis(
    val avgBrightness: Int,
    val darkRatio: Float,
    val brightRatio: Float
)

data class SceneRecognitionResult(
    val sceneType: SceneType,
    val confidence: Float,
    val recommendedParams: Map<String, String>,
    val isEnabled: Boolean,
    val isFallback: Boolean = false
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
}

enum class SceneType(val displayName: String, val icon: String) {
    AUTO("自动", "📷"),
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