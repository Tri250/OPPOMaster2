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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 2026 哈苏之眼一键扫描场景识别引擎
 *
 * 职责：
 * 1. 实时分析相机取景器帧，识别当前拍摄场景（美食/人像/夜景/风景/宠物/街拍等）
 * 2. 双路融合：轻量 TFLite 图像分类模型 + HeuristicSceneAnalyzer 启发式分析 + FeatureBasedClassifier 特征分类
 * 3. 输出场景画像、置信度、推荐哈苏参数、推荐胶片、AR构图建议
 * 4. 模型不存在时自动降级到启发式+特征分析，保证功能完整可用
 *
 * 性能目标：
 * - 输入 224x224，单次推理 < 30ms（GPU delegate）
 * - 每 5 帧分析一次，不阻塞 30fps 预览
 */
class SceneRecognitionManager private constructor(context: Context) {

    /**
     * 模型加载状态
     */
    enum class ModelStatus {
        NOT_LOADED,
        LOADING,
        LOADED,
        DOWNLOADING,
        ERROR
    }

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

        private val refCount = java.util.concurrent.atomic.AtomicInteger(0)

        fun getInstance(context: Context): SceneRecognitionManager {
            return instance ?: synchronized(this) {
                instance ?: SceneRecognitionManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 获取单例实例并增加引用计数。
         * 调用者必须在生命周期结束时调用 [SceneRecognitionManager.release] 配对释放。
         */
        fun acquire(context: Context): SceneRecognitionManager {
            val manager = getInstance(context)
            val count = refCount.incrementAndGet()
            Log.d(TAG, "acquire refCount=$count")
            return manager
        }
    }

    private val appContext = context.applicationContext
    private val heuristicAnalyzer = HeuristicSceneAnalyzer.getInstance(appContext)
    private val sceneMapping = SceneToHasselbladMapping
    private val inferenceEngine = MasterInferenceEngine.getInstance(appContext)
    private val featureClassifier = FeatureBasedClassifier()
    private val modelManager = SceneModelManager(appContext)

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

    // 模型状态 StateFlow，供 UI 观察
    private val _modelStatus = MutableStateFlow(ModelStatus.NOT_LOADED)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    // 预分配缓冲区
    private val preallocatedOutput = AtomicReference<Array<FloatArray>>(null)
    private var preallocatedTensorImage: TensorImage? = null

    init {
        // 后台异步初始化 TFLite，失败不阻塞主功能
        _modelStatus.value = ModelStatus.LOADING
        Thread { initTFLite() }.start()
    }

    /**
     * 初始化 TFLite 解释器。
     * 优先从 assets 加载；assets 不存在则尝试从 SceneModelManager 的下载缓存加载；
     * 优先 GPU delegate；失败则 CPU；模型不存在则标记为不可用。
     */
    private fun initTFLite() {
        if (isInitializing.getAndSet(true)) return
        try {
            // 1. 尝试从 assets 加载
            val modelBuffer: MappedByteBuffer? = try {
                FileUtil.loadMappedFile(appContext, MODEL_FILE)
            } catch (_: Exception) {
                null
            }

            // 2. assets 不存在时尝试从 SceneModelManager 下载缓存加载
            val finalBuffer = modelBuffer ?: try {
                val downloadedPath = modelManager.getModelPath()
                if (downloadedPath != null) {
                    val file = File(downloadedPath)
                    FileInputStream(file).use { fis ->
                        fis.channel.map(
                            FileChannel.MapMode.READ_ONLY,
                            0,
                            file.length()
                        )
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }

            if (finalBuffer == null) {
                _isModelAvailable.set(false)
                _modelStatus.value = ModelStatus.NOT_LOADED
                Log.w(TAG, "TFLite 模型未找到（assets 和缓存均无），将使用启发式分析器 + 特征分类器")
                return
            }

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
            tfliteInterpreter = Interpreter(finalBuffer, options)
            _isModelAvailable.set(true)
            _modelStatus.value = ModelStatus.LOADED
            Log.d(TAG, "TFLite 场景分类模型加载成功")
        } catch (e: Exception) {
            _isModelAvailable.set(false)
            _modelStatus.value = ModelStatus.ERROR
            Log.w(TAG, "TFLite 模型加载失败，将使用启发式分析器: ${e.message}")
        }
    }

    /**
     * 从远程下载模型。
     * 下载完成后自动重新初始化 TFLite 解释器。
     *
     * @param url 模型下载地址，为空则使用默认地址
     * @param onProgress 下载进度回调 (0.0 ~ 1.0)
     * @return 下载并加载是否成功
     */
    suspend fun downloadModel(
        url: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (_modelStatus.value == ModelStatus.DOWNLOADING) {
            Log.w(TAG, "模型正在下载中，请勿重复调用")
            return@withContext false
        }

        _modelStatus.value = ModelStatus.DOWNLOADING
        val success = modelManager.downloadModel(
            url = url ?: SceneModelManager.DEFAULT_MODEL_URL,
            onProgress = onProgress
        )

        if (success) {
            // 关闭旧解释器
            try {
                tfliteInterpreter?.close()
                tfliteInterpreter = null
                gpuDelegate?.close()
                gpuDelegate = null
            } catch (_: Exception) {}

            // 重新初始化
            isInitializing.set(false)
            initTFLite()
        } else {
            _modelStatus.value = if (_isModelAvailable.get()) ModelStatus.LOADED else ModelStatus.ERROR
        }

        success
    }

    /**
     * 预热：预分配推理所需的缓冲区
     * 在相机预览开始前调用，避免首次推理时的内存分配延迟
     */
    fun warmup() {
        try {
            // 预分配模型输出缓冲区
            if (preallocatedOutput.get() == null) {
                preallocatedOutput.set(Array(1) { FloatArray(MODEL_LABELS.size) })
            }

            // 预分配 TensorImage
            if (preallocatedTensorImage == null) {
                preallocatedTensorImage = TensorImage.fromBitmap(
                    Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
                )
            }

            // 如果模型已加载，执行一次空推理预热 TFLite
            tfliteInterpreter?.let { interpreter ->
                try {
                    val dummyInput = TensorImage.fromBitmap(
                        Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
                    )
                    val processed = imageProcessor.process(dummyInput)
                    val output = Array(1) { FloatArray(MODEL_LABELS.size) }
                    interpreter.run(processed.buffer, output)
                    Log.d(TAG, "TFLite 预热推理完成")
                } catch (e: Exception) {
                    Log.w(TAG, "TFLite 预热推理失败: ${e.message}")
                }
            }

            Log.d(TAG, "预热完成：缓冲区已预分配")
        } catch (e: Exception) {
            Log.w(TAG, "预热失败: ${e.message}")
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

        // 3. 特征分类器推理（始终执行，作为无模型时的增强）
        val featurePredictions = runFeatureClassification(bitmap)

        // 4. 三路融合
        val fusedProfile = fusePredictions(heuristicResult.primaryScene, modelPrediction, featurePredictions)
        val fusedConfidence = fuseConfidence(
            heuristicResult.confidence,
            modelPrediction?.confidence ?: 0f,
            featurePredictions.firstOrNull()?.confidence ?: 0f
        )

        // 5. 构建推荐参数与胶片
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
            source = when {
                modelPrediction != null -> "model+heuristic"
                featurePredictions.isNotEmpty() -> "feature+heuristic"
                else -> "heuristic"
            },
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
            val output = preallocatedOutput.get() ?: Array(1) { FloatArray(MODEL_LABELS.size) }
            output[0].fill(0f)
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
     * 运行特征分类器推理
     */
    private fun runFeatureClassification(bitmap: Bitmap): List<FeatureBasedClassifier.ScenePrediction> {
        return try {
            val features = featureClassifier.extractFeatures(bitmap)
            featureClassifier.classify(features)
        } catch (e: Exception) {
            Log.e(TAG, "特征分类器推理失败", e)
            emptyList()
        }
    }

    /**
     * 将模型输出标签映射到项目内的 SceneProfile。
     *
     * 三路融合策略：
     * - 模型高置信(>0.75)：以模型为主，启发式微调
     * - 模型中置信(0.5-0.75)：模型和启发式加权融合，启发式权重更高
     * - 模型低置信(<0.5) 或无模型：以启发式为主，特征分类器辅助验证
     * - 无模型时：特征分类器和启发式互相验证增强
     */
    private fun fusePredictions(
        heuristicProfile: SceneProfile,
        modelPrediction: ModelPrediction?,
        featurePredictions: List<FeatureBasedClassifier.ScenePrediction>
    ): SceneProfile {
        val topFeaturePrediction = featurePredictions.firstOrNull()
        val featureConfidence = topFeaturePrediction?.confidence ?: 0f
        val featureLabel = topFeaturePrediction?.label

        if (modelPrediction == null) {
            // 无模型路径：启发式为主，特征分类器辅助验证
            return fuseHeuristicAndFeature(heuristicProfile, featureLabel, featureConfidence)
        }

        val modelConfidence = modelPrediction.confidence
        val modelSceneId = mapModelLabelToSceneId(modelPrediction.label)
        val modelCategory = mapModelLabelToCategory(modelPrediction.label)

        return when {
            modelConfidence > 0.75f && modelSceneId != null -> {
                // 高置信模型结果：复用启发式画像但覆盖关键字段
                heuristicProfile.copy(
                    id = modelSceneId,
                    name = mapModelLabelToDisplayName(modelPrediction.label),
                    category = modelCategory ?: heuristicProfile.category,
                    confidence = modelConfidence
                )
            }
            modelConfidence > 0.5f && modelSceneId != null -> {
                // 中置信：模型与启发式加权融合，启发式权重更高
                val featureAgrees = featureLabel != null &&
                    mapFeatureLabelToSceneId(featureLabel) == modelSceneId
                val adjustedWeight = if (featureAgrees) 0.55f else 0.4f

                if (heuristicProfile.id == modelSceneId ||
                    mapFeatureLabelToSceneId(featureLabel ?: "") == modelSceneId) {
                    // 两者一致，增强结果
                    heuristicProfile.copy(
                        id = modelSceneId,
                        name = mapModelLabelToDisplayName(modelPrediction.label),
                        category = modelCategory ?: heuristicProfile.category,
                        confidence = (modelConfidence * adjustedWeight + heuristicProfile.confidence * (1f - adjustedWeight))
                            .coerceIn(0f, 1f)
                    )
                } else {
                    // 不一致，偏向启发式
                    heuristicProfile
                }
            }
            else -> {
                // 低置信或无法映射：以启发式为主，特征分类器辅助
                fuseHeuristicAndFeature(heuristicProfile, featureLabel, featureConfidence)
            }
        }
    }

    /**
     * 启发式与特征分类器融合（无模型或模型低置信时使用）
     */
    private fun fuseHeuristicAndFeature(
        heuristicProfile: SceneProfile,
        featureLabel: String?,
        featureConfidence: Float
    ): SceneProfile {
        if (featureLabel == null || featureConfidence < 0.1f) {
            return heuristicProfile
        }

        val featureSceneId = mapFeatureLabelToSceneId(featureLabel)
        val featureCategory = mapFeatureLabelToCategory(featureLabel)
        val featureDisplayName = mapFeatureLabelToDisplayName(featureLabel)

        if (featureSceneId == null) {
            return heuristicProfile
        }

        // 如果启发式和特征分类器结论一致，增强置信度
        val agrees = heuristicProfile.id == featureSceneId ||
            heuristicProfile.category == featureCategory

        return if (agrees) {
            // 一致：提高置信度
            val boostedConfidence = (heuristicProfile.confidence * 0.7f + featureConfidence * 0.3f)
                .coerceIn(heuristicProfile.confidence, 1f)
            heuristicProfile.copy(confidence = boostedConfidence)
        } else if (featureConfidence > 0.6f) {
            // 不一致但特征分类器高置信：考虑采纳特征分类器结论
            // 但需要额外验证：如果特征分类器的类别和启发式类别差距不大，才采纳
            heuristicProfile.copy(
                id = featureSceneId,
                name = featureDisplayName,
                category = featureCategory ?: heuristicProfile.category,
                confidence = (heuristicProfile.confidence * 0.5f + featureConfidence * 0.5f)
                    .coerceIn(0f, 1f)
            )
        } else {
            // 特征分类器低置信：完全信任启发式
            heuristicProfile
        }
    }

    /**
     * 融合置信度：模型存在时加权平均，否则启发式+特征加权。
     */
    private fun fuseConfidence(heuristic: Float, model: Float, feature: Float): Float {
        return when {
            model > 0.5f -> {
                // 模型高置信：模型为主
                (heuristic * 0.3f + model * 0.5f + feature * 0.2f).coerceIn(0f, 1f)
            }
            model > 0.01f -> {
                // 模型低置信：启发式为主
                (heuristic * 0.5f + model * 0.25f + feature * 0.25f).coerceIn(0f, 1f)
            }
            else -> {
                // 无模型：启发式为主，特征辅助
                if (feature > 0.1f) {
                    (heuristic * 0.7f + feature * 0.3f).coerceIn(0f, 1f)
                } else {
                    heuristic.coerceIn(0f, 1f)
                }
            }
        }
    }

    // ==================== 标签映射 ====================

    /**
     * 模型标签 → 场景 ID 映射。
     */
    private fun mapModelLabelToSceneId(label: String): String? = when (label) {
        "food" -> "food-restaurant"
        "portrait" -> "portrait-standard"
        "portrait_group" -> "portrait-group"
        "pet" -> "pet-cat"
        "landscape" -> "landscape-standard"
        "night_city" -> "night-city"
        "street" -> "urban-street"
        "macro" -> "macro-flower"
        "document" -> "still-document"
        "indoor" -> "indoor-living"
        else -> null
    }

    private fun mapModelLabelToCategory(label: String): SceneCategory? = when (label) {
        "food" -> SceneCategory.FOOD
        "portrait", "portrait_group" -> SceneCategory.PORTRAIT
        "pet" -> SceneCategory.PORTRAIT
        "landscape" -> SceneCategory.LANDSCAPE
        "night_city" -> SceneCategory.NIGHT
        "street" -> SceneCategory.URBAN
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
        "night_city" -> "夜景"
        "street" -> "街拍"
        "macro" -> "微距"
        "document" -> "文档"
        "indoor" -> "室内"
        else -> "日常"
    }

    // ==================== 特征分类器标签映射 ====================

    private fun mapFeatureLabelToSceneId(label: String): String? = when (label) {
        "portrait" -> "portrait-standard"
        "landscape" -> "landscape-standard"
        "food" -> "food-restaurant"
        "night" -> "night-city"
        "architecture" -> "urban-architecture"
        "indoor" -> "indoor-living"
        "macro" -> "macro-flower"
        "beach" -> "landscape-beach"
        "snow" -> "landscape-snow"
        "sunset" -> "landscape-sunset"
        "flower" -> "still-flower"
        "street" -> "urban-street"
        else -> null
    }

    private fun mapFeatureLabelToCategory(label: String): SceneCategory? = when (label) {
        "portrait" -> SceneCategory.PORTRAIT
        "landscape", "beach", "snow", "sunset" -> SceneCategory.LANDSCAPE
        "food" -> SceneCategory.FOOD
        "night" -> SceneCategory.NIGHT
        "architecture", "street" -> SceneCategory.URBAN
        "indoor" -> SceneCategory.STILL_LIFE
        "macro", "flower" -> SceneCategory.MACRO
        else -> null
    }

    private fun mapFeatureLabelToDisplayName(label: String): String = when (label) {
        "portrait" -> "人像"
        "landscape" -> "风景"
        "food" -> "美食"
        "night" -> "夜景"
        "architecture" -> "建筑"
        "indoor" -> "室内"
        "macro" -> "微距"
        "beach" -> "海滩"
        "snow" -> "雪景"
        "sunset" -> "日落"
        "flower" -> "花卉"
        "street" -> "街拍"
        else -> "日常"
    }

    /**
     * 释放 TFLite 资源。
     */
    fun release() {
        val count = refCount.decrementAndGet()
        Log.d(TAG, "release refCount=$count")
        if (count > 0) {
            return
        }
        if (count < 0) {
            Log.w(TAG, "release called more times than acquire, resetting refCount")
            refCount.set(0)
            return
        }
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
            gpuDelegate?.close()
            gpuDelegate = null
            preallocatedOutput.set(null)
            preallocatedTensorImage = null
            synchronized(SceneRecognitionManager::class.java) {
                instance = null
            }
            Log.d(TAG, "SceneRecognitionManager fully released")
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
