package com.silas.omaster.ai.scene

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.MappedByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 2026 哈苏之眼一键扫描场景识别引擎
 *
 * 职责：
 * 1. 实时分析相机取景器帧，识别当前拍摄场景（美食/人像/夜景/风景/宠物/街拍等）
 * 2. 双路融合：轻量 TFLite 图像分类模型 + HeuristicSceneAnalyzer 启发式分析
 * 3. 输出场景画像、置信度、推荐哈苏参数、推荐胶片、AR构图建议
 * 4. 模型不存在时自动降级到启发式分析，保证功能完整可用
 *
 * 性能目标：
 * - 输入 224x224，单次推理 < 30ms（GPU delegate）
 * - 每 5 帧分析一次，不阻塞 30fps 预览
 */
class SceneRecognitionManager private constructor(context: Context) {

    companion object {
        private const val TAG = "SceneRecognitionManager"

        // assets 中 TFLite 模型文件名；如不存在则自动降级启发式
        private const val MODEL_FILE = "models/scene_classifier.tflite"

        // 模型输入尺寸（MobileNetV3 224x224）
        private const val INPUT_SIZE = 224

        // 模型输出标签，与训练时一致
        private val MODEL_LABELS = listOf(
            "food",
            "portrait",
            "portrait_group",
            "pet",
            "landscape",
            "night_city",
            "street",
            "macro",
            "document",
            "indoor",
            "unknown"
        )

        @Volatile
        private var instance: SceneRecognitionManager? = null

        fun getInstance(context: Context): SceneRecognitionManager {
            return instance ?: synchronized(this) {
                instance ?: SceneRecognitionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val heuristicAnalyzer = HeuristicSceneAnalyzer.getInstance(appContext)
    private val sceneMapping = SceneToHasselbladMapping
    private val inferenceEngine = MasterInferenceEngine.getInstance(appContext)

    // TFLite 解释器，异步初始化
    private var tfliteInterpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private val isInitializing = AtomicBoolean(false)

    // 图像预处理管线（复用）
    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // 归一化到 [0,1]
            .build()
    }

    // 初始化状态
    private val _isModelAvailable = AtomicBoolean(false)
    val isModelAvailable: Boolean get() = _isModelAvailable.get()

    init {
        // 后台异步初始化 TFLite，失败不阻塞主功能
        Thread { initTFLite() }.start()
    }

    /**
     * 初始化 TFLite 解释器。
     * 优先 GPU delegate；失败则 CPU；模型不存在则标记为不可用。
     */
    private fun initTFLite() {
        if (isInitializing.getAndSet(true)) return
        try {
            val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(appContext, MODEL_FILE)
            val options = Interpreter.Options().apply {
                numThreads = 2
                try {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate)
                    Log.d(TAG, "TFLite 使用 GPU delegate")
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate 不可用，回退 CPU", e)
                }
            }
            tfliteInterpreter = Interpreter(modelFile, options)
            _isModelAvailable.set(true)
            Log.d(TAG, "TFLite 场景分类模型加载成功")
        } catch (e: Exception) {
            _isModelAvailable.set(false)
            Log.w(TAG, "TFLite 模型未找到或加载失败，将使用启发式分析器: ${e.message}")
        }
    }

    /**
     * 分析单帧画面，返回实时场景识别结果。
     *
     * @param bitmap 原始取景器帧（ARGB_8888）
     * @return 实时场景识别结果，包含场景画像、推荐参数、置信度
     */
    suspend fun analyzeFrame(bitmap: Bitmap): RealtimeSceneResult = withContext(Dispatchers.Default) {
        // 1. 启发式分析（始终执行，作为基准与 fallback）
        val heuristicResult = heuristicAnalyzer.analyze(
            bitmap = bitmap,
            exif = null,
            userContext = null
        )

        // 2. TFLite 模型推理（如果模型已加载）
        val modelPrediction = tfliteInterpreter?.let { interpreter ->
            runModelInference(bitmap, interpreter)
        }

        // 3. 双路融合
        val fusedProfile = fusePredictions(heuristicResult.primaryScene, modelPrediction)
        val fusedConfidence = fuseConfidence(heuristicResult.confidence, modelPrediction?.confidence ?: 0f)

        // 4. 构建推荐参数与胶片
        val recommendedParams = sceneMapping.getParams(fusedProfile.id)
        val recommendedFilms = sceneMapping.getRecommendedFilms(fusedProfile.id)
        val masterTips = sceneMapping.getMasterTips(fusedProfile.id)

        RealtimeSceneResult(
            sceneProfile = fusedProfile.copy(
                confidence = fusedConfidence,
                hasselbladParams = recommendedParams,
                recommendedFilm = recommendedFilms,
                masterTips = masterTips
            ),
            confidence = fusedConfidence,
            category = fusedProfile.category,
            recommendedParams = recommendedParams,
            recommendedFilm = recommendedFilms.firstOrNull()?.name,
            source = if (modelPrediction != null) "model+heuristic" else "heuristic",
            heuristicConfidence = heuristicResult.confidence,
            modelConfidence = modelPrediction?.confidence ?: 0f
        )
    }

    /**
     * 运行 TFLite 模型推理。
     */
    private fun runModelInference(bitmap: Bitmap, interpreter: Interpreter): ModelPrediction? {
        return try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processed = imageProcessor.process(tensorImage)
            val output = Array(1) { FloatArray(MODEL_LABELS.size) }
            interpreter.run(processed.buffer, output)
            val scores = output[0]
            val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: return null
            val label = MODEL_LABELS.getOrNull(maxIndex) ?: "unknown"
            ModelPrediction(
                label = label,
                confidence = scores[maxIndex].coerceIn(0f, 1f),
                allScores = MODEL_LABELS.zip(scores.toList()).toMap()
            )
        } catch (e: Exception) {
            Log.e(TAG, "TFLite 推理失败", e)
            null
        }
    }

    /**
     * 将模型输出标签映射到项目内的 SceneProfile。
     * 优化：降低模型置信度门槛（0.75→0.55），使模型在更多场景下发挥作用；
     * 同时当模型与启发式结果一致时，即使置信度低于门槛也给予加分
     */
    private fun fusePredictions(heuristicProfile: SceneProfile, modelPrediction: ModelPrediction?): SceneProfile {
        if (modelPrediction == null) return heuristicProfile

        // 模型置信度高时，以模型标签为主；否则以启发式为主
        val modelConfidence = modelPrediction.confidence
        val modelSceneId = mapModelLabelToSceneId(modelPrediction.label)
        val modelCategory = mapModelLabelToCategory(modelPrediction.label)

        // 优化：模型与启发式一致时降低门槛
        val isConsistent = modelSceneId != null && (
            heuristicProfile.id.startsWith(modelSceneId.substringBefore("-")) ||
            heuristicProfile.category == modelCategory
        )
        val confidenceThreshold = if (isConsistent) 0.45f else 0.55f

        return if (modelConfidence > confidenceThreshold && modelSceneId != null) {
            // 高置信模型结果：复用启发式画像但覆盖关键字段
            heuristicProfile.copy(
                id = modelSceneId,
                name = mapModelLabelToDisplayName(modelPrediction.label),
                category = modelCategory ?: heuristicProfile.category,
                confidence = modelConfidence
            )
        } else {
            // 低置信或无法映射：启发式结果微调
            heuristicProfile
        }
    }

    /**
     * 融合置信度：模型存在时加权平均，否则完全信任启发式。
     * 优化：降低模型置信度门槛，增加启发式权重，提高低置信度场景的识别率
     */
    private fun fuseConfidence(heuristic: Float, model: Float): Float {
        return if (model > 0.01f) {
            // 优化：模型低置信度时增加启发式权重，避免模型误判覆盖正确启发式结果
            val modelWeight = if (model > 0.5f) 0.6f else 0.35f
            val heuristicWeight = 1f - modelWeight
            (heuristic * heuristicWeight + model * modelWeight).coerceIn(0f, 1f)
        } else {
            heuristic.coerceIn(0f, 1f)
        }
    }

    /**
     * 模型标签 → 场景 ID 映射。
     * 优化：增加常见场景标签映射，提高模型输出利用率
     */
    private fun mapModelLabelToSceneId(label: String): String? = when (label) {
        "food" -> "food-restaurant"
        "portrait" -> "portrait-standard"
        "portrait_group" -> "portrait-group"
        "pet" -> "pet-cat"
        "landscape" -> "landscape-standard"
        "sky" -> "landscape-sky"
        "beach" -> "landscape-beach"
        "snow" -> "landscape-snow"
        "sunset" -> "landscape-sunset"
        "forest" -> "landscape-forest"
        "night_city" -> "night-city"
        "street" -> "urban-street"
        "architecture" -> "urban-street"
        "macro" -> "macro-flower"
        "document" -> "still-document"
        "indoor" -> "indoor-living"
        "cafe" -> "urban-cafe"
        else -> null
    }

    private fun mapModelLabelToCategory(label: String): SceneCategory? = when (label) {
        "food" -> SceneCategory.FOOD
        "portrait", "portrait_group", "pet" -> SceneCategory.PORTRAIT
        "landscape", "sky", "beach", "snow", "sunset", "forest" -> SceneCategory.LANDSCAPE
        "night_city" -> SceneCategory.NIGHT
        "street", "architecture", "cafe" -> SceneCategory.URBAN
        "macro" -> SceneCategory.MACRO
        "document", "indoor" -> SceneCategory.STILL_LIFE
        else -> null
    }

    private fun mapModelLabelToDisplayName(label: String): String = when (label) {
        "food" -> "美食"
        "portrait" -> "人像"
        "portrait_group" -> "合影"
        "pet" -> "宠物"
        "landscape" -> "风景"
        "sky" -> "天空"
        "beach" -> "海滩"
        "snow" -> "雪景"
        "sunset" -> "日落"
        "forest" -> "森林"
        "night_city" -> "夜景"
        "street" -> "街拍"
        "architecture" -> "建筑街拍"
        "macro" -> "微距"
        "document" -> "文档"
        "indoor" -> "室内"
        "cafe" -> "咖啡馆"
        else -> "日常"
    }

    /**
     * 释放 TFLite 资源。
     */
    fun release() {
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
            gpuDelegate?.close()
            gpuDelegate = null
            Log.d(TAG, "SceneRecognitionManager 已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放失败", e)
        }
    }

    private data class ModelPrediction(
        val label: String,
        val confidence: Float,
        val allScores: Map<String, Float>
    )
}

/**
 * 实时场景识别结果。
 */
data class RealtimeSceneResult(
    val sceneProfile: SceneProfile,
    val confidence: Float,
    val category: SceneCategory,
    val recommendedParams: HasselbladParams,
    val recommendedFilm: String?,
    val source: String,
    val heuristicConfidence: Float,
    val modelConfidence: Float
)
