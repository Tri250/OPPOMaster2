package com.silas.omaster.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * MediaPipe 场景分类器
 *
 * 使用 MediaPipe Tasks Vision ImageClassifier API 进行场景分类
 * 支持 GPU 加速和高效的模型推理
 *
 * 功能：
 * - 加载 TFLite 模型文件
 * - 执行场景分类推理
 * - 返回场景概率分布
 * - 支持 GPU 加速
 *
 * 模型要求：
 * - 输入: [1, 224, 224, 3] RGB 图像，归一化到 [0, 1]
 * - 输出: [1, 36] Softmax 概率分布
 */
class MediaPipeSceneClassifier private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MediaPipeClassifier"

        // 模型文件名
        const val MODEL_NAME = "scene_classifier.tflite"

        // 场景类别映射 (36类)
        val SCENE_CLASSES = listOf(
            "landscape", "portrait", "night", "food", "street", "architecture",
            "pet", "snow", "beach", "sunset", "flower", "forest",
            "mountain", "water", "city", "indoor", "sky", "macro",
            "sports", "document", "product", "wedding", "children", "group",
            "backlight", "cloudy", "sunny", "rainy", "autumn", "spring",
            "summer", "winter", "abstract", "bw", "vintage", "other"
        )

        // 场景类别到 SceneCategory 的映射
        val SCENE_TO_CATEGORY_MAP: Map<String, SceneCategory> = mapOf(
            "portrait" to SceneCategory.PORTRAIT,
            "children" to SceneCategory.PORTRAIT,
            "group" to SceneCategory.PORTRAIT,
            "wedding" to SceneCategory.EVENT,
            "landscape" to SceneCategory.LANDSCAPE,
            "mountain" to SceneCategory.LANDSCAPE,
            "forest" to SceneCategory.LANDSCAPE,
            "water" to SceneCategory.LANDSCAPE,
            "beach" to SceneCategory.LANDSCAPE,
            "sunset" to SceneCategory.LANDSCAPE,
            "snow" to SceneCategory.LANDSCAPE,
            "autumn" to SceneCategory.LANDSCAPE,
            "spring" to SceneCategory.LANDSCAPE,
            "summer" to SceneCategory.LANDSCAPE,
            "winter" to SceneCategory.LANDSCAPE,
            "sky" to SceneCategory.LANDSCAPE,
            "night" to SceneCategory.NIGHT,
            "food" to SceneCategory.FOOD,
            "street" to SceneCategory.URBAN,
            "city" to SceneCategory.URBAN,
            "architecture" to SceneCategory.URBAN,
            "indoor" to SceneCategory.URBAN,
            "macro" to SceneCategory.MACRO,
            "flower" to SceneCategory.STILL_LIFE,
            "product" to SceneCategory.STILL_LIFE,
            "document" to SceneCategory.STILL_LIFE,
            "sports" to SceneCategory.EVENT,
            "pet" to SceneCategory.PORTRAIT,
            "backlight" to SceneCategory.PORTRAIT,
            "cloudy" to SceneCategory.LANDSCAPE,
            "sunny" to SceneCategory.LANDSCAPE,
            "rainy" to SceneCategory.LANDSCAPE,
            "abstract" to SceneCategory.STILL_LIFE,
            "bw" to SceneCategory.PORTRAIT,
            "vintage" to SceneCategory.STILL_LIFE,
            "other" to SceneCategory.PORTRAIT
        )

        @Volatile
        private var instance: MediaPipeSceneClassifier? = null

        fun getInstance(context: Context): MediaPipeSceneClassifier {
            return instance ?: synchronized(this) {
                instance ?: MediaPipeSceneClassifier(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // MediaPipe 图像分类器已移除（依赖不可用），始终使用启发式降级模式
    // private var imageClassifier: ImageClassifier? = null

    // 模型是否已加载
    private var isModelLoaded = false

    // 是否支持 GPU
    private var gpuSupported = false

    /**
     * 初始化分类器
     *
     * @param useGpu 是否使用 GPU 加速
     * @return 初始化是否成功
     */
    suspend fun initialize(useGpu: Boolean = true): Result<Boolean> = withContext(Dispatchers.IO) {
        // MediaPipe 依赖不可用，始终使用启发式降级模式
        Log.i(TAG, "MediaPipe 依赖不可用，将使用启发式降级模式")
        isModelLoaded = false
        gpuSupported = false
        Result.success(false)
    }

    /**
     * 检查 GPU 支持
     */
    private fun checkGpuSupport(): Boolean {
        return try {
            // MediaPipe GPU 委托需要 OpenGL ES 3.1+
            val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
            val configInfo = activityManager?.deviceConfigurationInfo
            val glEsVersion = configInfo?.glEsVersion?.toDoubleOrNull() ?: 0.0
            glEsVersion >= 3.1
        } catch (e: Exception) {
            Log.w(TAG, "检查 GPU 支持失败", e)
            false
        }
    }

    /**
     * 获取模型文件路径
     */
    private fun getModelPath(): File {
        return File(context.filesDir, "models/$MODEL_NAME")
    }

    /**
     * 从 assets 复制模型文件
     */
    private fun copyModelFromAssets(): Boolean {
        return try {
            val modelFile = getModelPath()
            modelFile.parentFile?.mkdirs()

            context.assets.open("models/$MODEL_NAME").use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "模型文件已复制到: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "复制模型文件失败", e)
            false
        }
    }

    /**
     * 执行场景分类
     *
     * @param bitmap 输入图像
     * @return 分类结果
     */
    suspend fun classify(bitmap: Bitmap): ClassificationResult = withContext(Dispatchers.Default) {
        // MediaPipe 依赖不可用，始终使用启发式降级模式
        heuristicClassification(bitmap)
    }

    /**
     * 启发式分类（降级模式）
     * 当模型不可用时使用图像特征分析
     */
    private fun heuristicClassification(bitmap: Bitmap): ClassificationResult {
        Log.d(TAG, "使用启发式算法进行场景分类")

        val startTime = System.currentTimeMillis()

        // 提取图像特征
        val features = extractImageFeatures(bitmap)

        // 基于特征推断场景
        val probabilities = inferSceneFromFeatures(features)

        // 归一化
        val sum = probabilities.sum()
        for (i in probabilities.indices) {
            probabilities[i] /= sum.coerceAtLeast(1f)
        }

        // 找到最高概率场景
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val topScene = SCENE_CLASSES[maxIndex]
        val topConfidence = probabilities[maxIndex]

        val inferenceTime = System.currentTimeMillis() - startTime

        return ClassificationResult(
            topScene = topScene,
            topConfidence = topConfidence,
            allProbabilities = probabilities,
            sceneResults = listOf(SceneProbability(
                sceneId = topScene,
                sceneName = getSceneDisplayName(topScene),
                confidence = topConfidence,
                category = SCENE_TO_CATEGORY_MAP[topScene] ?: SceneCategory.PORTRAIT
            )),
            inferenceTimeMs = inferenceTime,
            usedGpu = false
        )
    }

    /**
     * 提取图像特征
     */
    private fun extractImageFeatures(bitmap: Bitmap): ImageFeatures {
        val width = bitmap.width
        val height = bitmap.height

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var warmPixels = 0; var coldPixels = 0
        var darkPixels = 0; var brightPixels = 0
        var totalPixels = 0

        // 采样步长（大图采样）
        val step = when {
            width > 1000 -> 8
            width > 500 -> 4
            else -> 2
        }

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                totalR += r; totalG += g; totalB += b
                totalPixels++

                // 暖色调（红 > 蓝 + 20）
                if (r > b + 20) warmPixels++

                // 冷色调（蓝 > 红 + 20）
                if (b > r + 20) coldPixels++

                // 亮度计算
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (luminance < 50) darkPixels++
                if (luminance > 200) brightPixels++
            }
        }

        return ImageFeatures(
            avgRed = (totalR / totalPixels).toInt(),
            avgGreen = (totalG / totalPixels).toInt(),
            avgBlue = (totalB / totalPixels).toInt(),
            warmthRatio = warmPixels.toFloat() / totalPixels,
            coldRatio = coldPixels.toFloat() / totalPixels,
            darkRatio = darkPixels.toFloat() / totalPixels,
            brightRatio = brightPixels.toFloat() / totalPixels,
            avgLuminance = (0.299 * (totalR / totalPixels) +
                           0.587 * (totalG / totalPixels) +
                           0.114 * (totalB / totalPixels)).toInt()
        )
    }

    /**
     * 基于特征推断场景概率
     */
    private fun inferSceneFromFeatures(features: ImageFeatures): FloatArray {
        val probabilities = FloatArray(SCENE_CLASSES.size)

        // 基于亮度判断
        when {
            features.avgLuminance < 50 -> {
                // 极暗场景 -> 夜景
                probabilities[2] = 0.4f  // night
                probabilities[24] = 0.2f // backlight
            }
            features.avgLuminance < 100 -> {
                // 暗调场景
                probabilities[2] = 0.25f // night
                probabilities[1] = 0.15f // portrait
            }
            features.avgLuminance > 200 -> {
                // 高亮场景
                probabilities[0] = 0.3f  // landscape
                probabilities[16] = 0.2f // sky
                probabilities[9] = 0.15f // sunset
            }
            else -> {
                // 正常亮度
                probabilities[0] = 0.2f  // landscape
                probabilities[1] = 0.2f  // portrait
                probabilities[3] = 0.1f  // food
            }
        }

        // 基于颜色判断
        if (features.warmthRatio > 0.4f) {
            // 暖色调场景
            probabilities[9] += 0.2f  // sunset
            probabilities[3] += 0.15f // food
            probabilities[28] += 0.1f // autumn
        }

        if (features.coldRatio > 0.4f) {
            // 冷色调场景
            probabilities[16] += 0.2f // sky
            probabilities[7] += 0.1f // snow
            probabilities[8] += 0.1f // beach
        }

        // 基于暗部/高光比例判断
        if (features.darkRatio > 0.5f) {
            probabilities[2] += 0.2f // night
            probabilities[15] += 0.1f // indoor
        }

        if (features.brightRatio > 0.3f) {
            probabilities[16] += 0.15f // sky
            probabilities[0] += 0.1f  // landscape
        }

        // 默认概率填充
        for (i in probabilities.indices) {
            if (probabilities[i] == 0f) {
                probabilities[i] = 0.02f
            }
        }

        return probabilities
    }

    /**
     * 获取场景显示名称
     */
    private fun getSceneDisplayName(sceneId: String): String {
        return when (sceneId) {
            "landscape" -> "风景"
            "portrait" -> "人像"
            "night" -> "夜景"
            "food" -> "美食"
            "street" -> "街拍"
            "architecture" -> "建筑"
            "pet" -> "宠物"
            "snow" -> "雪景"
            "beach" -> "海滩"
            "sunset" -> "日落"
            "flower" -> "花卉"
            "forest" -> "森林"
            "mountain" -> "山脉"
            "water" -> "水景"
            "city" -> "城市"
            "indoor" -> "室内"
            "sky" -> "天空"
            "macro" -> "微距"
            "sports" -> "运动"
            "document" -> "文档"
            "product" -> "产品"
            "wedding" -> "婚礼"
            "children" -> "儿童"
            "group" -> "群体"
            "backlight" -> "逆光"
            "cloudy" -> "阴天"
            "sunny" -> "晴天"
            "rainy" -> "雨天"
            "autumn" -> "秋景"
            "spring" -> "春景"
            "summer" -> "夏景"
            "winter" -> "冬景"
            "abstract" -> "抽象"
            "bw" -> "黑白"
            "vintage" -> "复古"
            else -> "其他"
        }
    }

    /**
     * 检查模型是否已加载
     */
    fun isReady(): Boolean = isModelLoaded

    /**
     * 获取 GPU 支持状态
     */
    fun isGpuEnabled(): Boolean = gpuSupported

    /**
     * 释放资源
     */
    fun release() {
        try {
            // MediaPipe 分类器已移除，无需关闭
            isModelLoaded = false
            Log.i(TAG, "MediaPipe 场景分类器已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败", e)
        }
    }
}

/**
 * 分类结果
 */
data class ClassificationResult(
    val topScene: String,
    val topConfidence: Float,
    val allProbabilities: FloatArray,
    val sceneResults: List<SceneProbability> = emptyList(),
    val inferenceTimeMs: Long,
    val usedGpu: Boolean
)

/**
 * 场景概率
 */
data class SceneProbability(
    val sceneId: String,
    val sceneName: String,
    val confidence: Float,
    val category: SceneCategory
)

/**
 * 图像特征
 */
data class ImageFeatures(
    val avgRed: Int,
    val avgGreen: Int,
    val avgBlue: Int,
    val warmthRatio: Float,
    val coldRatio: Float,
    val darkRatio: Float,
    val brightRatio: Float,
    val avgLuminance: Int
)