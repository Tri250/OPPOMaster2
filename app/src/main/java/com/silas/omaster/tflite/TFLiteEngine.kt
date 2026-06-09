package com.silas.omaster.tflite

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * TensorFlow Lite 推理引擎
 * 
 * 单例模式，管理模型加载和推理
 * 支持GPU Delegate、NNAPI Delegate和XNNPACK加速
 * 
 * 功能：
 * - 模型生命周期管理
 * - 多硬件加速支持（GPU/NNAPI/XNNPACK）
 * - 异步推理，不阻塞UI线程
 * - 推理结果缓存机制
 * - 性能监控和统计
 */
class TFLiteEngine private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TFLiteEngine"
        
        // 模型文件名
        const val MODEL_SCENE_CLASSIFIER = "scene_classifier.tflite"
        const val MODEL_QUALITY_ANALYZER = "quality_analyzer.tflite"
        const val MODEL_PARAM_PREDICTOR = "param_predictor.tflite"
        
        // 性能目标（毫秒）
        const val TARGET_SCENE_CLASSIFICATION_MS = 50L
        const val TARGET_QUALITY_ANALYSIS_MS = 30L
        const val TARGET_PARAM_PREDICTION_MS = 10L
        const val TARGET_TOTAL_INFERENCE_MS = 100L
        
        @Volatile
        private var instance: TFLiteEngine? = null
        
        fun getInstance(context: Context): TFLiteEngine {
            return instance ?: synchronized(this) {
                instance ?: TFLiteEngine(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    // 推理配置
    private val config = AtomicReference(InferenceConfig())
    
    // 模型解释器
    private val interpreters = ConcurrentHashMap<String, Interpreter>()
    
    // 硬件加速委托
    private var gpuDelegate: GpuDelegate? = null
    private var nnapiDelegate: NnApiDelegate? = null
    
    // 硬件兼容性
    private val gpuCompatibilityList = CompatibilityList()
    
    // 推理状态
    private val state = AtomicReference(InferenceState.IDLE)
    
    // 推理结果缓存
    private val resultCache = ConcurrentHashMap<String, CachedResult>()
    private val cacheMutex = Mutex()
    
    // 性能统计
    private val performanceStats = ConcurrentHashMap<String, PerformanceStats>()
    
    // 推理协程作用域
    private val inferenceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 模型信息
    private val modelInfoMap = ConcurrentHashMap<String, ModelInfo>()
    
    /**
     * 缓存的推理结果
     */
    private data class CachedResult(
        val result: Any,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 性能统计
     */
    private data class PerformanceStats(
        var totalInferences: Long = 0,
        var totalTimeMs: Long = 0,
        var minTimeMs: Long = Long.MAX_VALUE,
        var maxTimeMs: Long = 0,
        var lastInferenceTimeMs: Long = 0
    ) {
        val averageTimeMs: Float
            get() = if (totalInferences > 0) totalTimeMs.toFloat() / totalInferences else 0f
    }
    
    /**
     * 初始化引擎
     * 
     * @param config 推理配置
     * @return 初始化是否成功
     */
    suspend fun initialize(config: InferenceConfig = InferenceConfig()): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            state.set(InferenceState.LOADING)
            this@TFLiteEngine.config.set(config)
            
            // 检查模型文件是否存在
            val modelsAvailable = checkModelsAvailable()
            if (!modelsAvailable) {
                Log.w(TAG, "部分模型文件不存在，将使用模拟推理模式")
            }
            
            // 初始化硬件加速
            initializeDelegates(config)
            
            state.set(InferenceState.READY)
            Log.i(TAG, "TFLite引擎初始化成功 - GPU: ${config.useGpu}, NNAPI: ${config.useNnapi}, XNNPACK: ${config.useXnnpack}")
            Result.success(true)
        } catch (e: Exception) {
            state.set(InferenceState.ERROR)
            Log.e(TAG, "TFLite引擎初始化失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 检查模型文件是否可用
     */
    private fun checkModelsAvailable(): Boolean {
        val models = listOf(MODEL_SCENE_CLASSIFIER, MODEL_QUALITY_ANALYZER, MODEL_PARAM_PREDICTOR)
        var allAvailable = true
        
        for (modelName in models) {
            try {
                val modelFile = File(context.filesDir, "models/$modelName")
                val assetPath = "models/$modelName"
                
                val exists = modelFile.exists() || try {
                    context.assets.list("")?.contains(modelName) == true ||
                    context.assets.list("models")?.contains(modelName) == true
                } catch (e: Exception) {
                    false
                }
                
                if (!exists) {
                    Log.d(TAG, "模型文件不存在: $modelName")
                    allAvailable = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查模型文件失败: $modelName", e)
                allAvailable = false
            }
        }
        
        return allAvailable
    }
    
    /**
     * 初始化硬件加速委托
     */
    private fun initializeDelegates(config: InferenceConfig) {
        // GPU Delegate
        if (config.useGpu && gpuCompatibilityList.isDelegateSupportedOnThisDevice) {
            try {
                val options = GpuDelegate.Options()
                    .setQuantizedModelsAllowed(true)
                    .setForceBackend(GpuDelegate.Options.FORCE_BACKEND_OPENCL)
                
                gpuDelegate = GpuDelegate(options)
                Log.i(TAG, "GPU Delegate 初始化成功")
            } catch (e: Exception) {
                Log.w(TAG, "GPU Delegate 初始化失败，将回退到CPU", e)
            }
        }
        
        // NNAPI Delegate (Android 8.1+)
        if (config.useNnapi && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                nnapiDelegate = NnApiDelegate.Builder()
                    .setUseNnapiCpu(true)
                    .setAllowFp16PrecisionForFp32(true)
                    .build()
                Log.i(TAG, "NNAPI Delegate 初始化成功")
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI Delegate 初始化失败", e)
            }
        }
    }
    
    /**
     * 加载模型
     * 
     * @param modelName 模型名称
     * @return Interpreter实例，失败返回null
     */
    private fun loadModel(modelName: String): Interpreter? {
        // 检查缓存
        interpreters[modelName]?.let { return it }
        
        return try {
            // 尝试从不同位置加载模型
            val modelBuffer = loadModelBuffer(modelName) ?: return null
            
            // 配置解释器选项
            val options = Interpreter.Options().apply {
                val currentConfig = config.get()
                
                // 设置线程数
                setNumThreads(currentConfig.numThreads)
                
                // 添加硬件加速委托
                if (currentConfig.useNnapi && nnapiDelegate != null) {
                    addDelegate(nnapiDelegate!!)
                    Log.d(TAG, "使用 NNAPI Delegate: $modelName")
                } else if (currentConfig.useGpu && gpuDelegate != null) {
                    addDelegate(gpuDelegate!!)
                    Log.d(TAG, "使用 GPU Delegate: $modelName")
                } else if (currentConfig.useXnnpack) {
                    setUseXNNPACK(true)
                    Log.d(TAG, "使用 XNNPACK: $modelName")
                }
            }
            
            val interpreter = Interpreter(modelBuffer, options)
            interpreters[modelName] = interpreter
            
            // 保存模型信息
            modelInfoMap[modelName] = ModelInfo(
                name = modelName,
                sizeBytes = modelBuffer.capacity().toLong(),
                inputShape = interpreter.getInputTensor(0).shape(),
                outputShape = interpreter.getOutputTensor(0).shape(),
                isQuantized = interpreter.getInputTensor(0).dataType() != Interpreter.DataType.FLOAT32
            )
            
            Log.i(TAG, "模型加载成功: $modelName, 输入形状: ${modelInfoMap[modelName]?.inputShape?.contentToString()}")
            interpreter
        } catch (e: Exception) {
            Log.e(TAG, "模型加载失败: $modelName", e)
            null
        }
    }
    
    /**
     * 加载模型缓冲区
     */
    private fun loadModelBuffer(modelName: String): ByteBuffer? {
        return try {
            // 尝试从assets加载
            try {
                val inputStream = context.assets.open("models/$modelName")
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                val buffer = ByteBuffer.allocateDirect(bytes.size)
                buffer.order(ByteOrder.nativeOrder())
                buffer.put(bytes)
                buffer.rewind()
                buffer
            } catch (e: Exception) {
                // 尝试从文件系统加载
                val modelFile = File(context.filesDir, "models/$modelName")
                if (modelFile.exists()) {
                    FileUtil.loadMappedFile(context, "models/$modelName")
                } else {
                    Log.w(TAG, "模型文件不存在: $modelName，将使用模拟推理")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载模型缓冲区失败: $modelName", e)
            null
        }
    }
    
    /**
     * 执行推理
     * 
     * @param modelName 模型名称
     * @param input 输入数据
     * @param cacheKey 缓存键（可选）
     * @return 推理结果
     */
    suspend fun <T> runInference(
        modelName: String,
        input: Any,
        cacheKey: String? = null
    ): Result<T> = withContext(Dispatchers.Default) {
        try {
            // 检查缓存
            if (config.get().enableCache && cacheKey != null) {
                resultCache[cacheKey]?.let { cached ->
                    @Suppress("UNCHECKED_CAST")
                    return@withContext Result.success(cached.result as T)
                }
            }
            
            // 加载模型
            val interpreter = loadModel(modelName)
            
            if (interpreter == null) {
                // 模型不存在，返回模拟结果
                Log.d(TAG, "使用模拟推理: $modelName")
                @Suppress("UNCHECKED_CAST")
                return@withContext Result.success(getSimulatedResult(modelName) as T)
            }
            
            // 执行推理
            state.set(InferenceState.INFERRING)
            val startTime = System.currentTimeMillis()
            
            val output = allocateOutputBuffer(interpreter)
            interpreter.run(input, output)
            
            val inferenceTime = System.currentTimeMillis() - startTime
            state.set(InferenceState.READY)
            
            // 更新性能统计
            updatePerformanceStats(modelName, inferenceTime)
            
            // 缓存结果
            if (config.get().enableCache && cacheKey != null) {
                resultCache[cacheKey] = CachedResult(output)
                cleanupCache()
            }
            
            @Suppress("UNCHECKED_CAST")
            Result.success(output as T)
        } catch (e: Exception) {
            state.set(InferenceState.ERROR)
            Log.e(TAG, "推理失败: $modelName", e)
            Result.failure(e)
        }
    }
    
    /**
     * 分配输出缓冲区
     */
    private fun allocateOutputBuffer(interpreter: Interpreter): ByteBuffer {
        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputSize = outputShape.reduce { acc, i -> acc * i }
        
        val buffer = ByteBuffer.allocateDirect(outputSize * 4) // FLOAT32
        buffer.order(ByteOrder.nativeOrder())
        buffer.rewind()
        
        return buffer
    }
    
    /**
     * 获取模拟推理结果
     * 当模型文件不存在时使用
     */
    private fun getSimulatedResult(modelName: String): Any {
        return when (modelName) {
            MODEL_SCENE_CLASSIFIER -> {
                // 模拟场景分类输出（36个场景的概率分布）
                val probabilities = FloatArray(36) { (0..100).random() / 100f }
                // 归一化
                val sum = probabilities.sum()
                probabilities.map { it / sum }.toFloatArray()
            }
            MODEL_QUALITY_ANALYZER -> {
                // 模拟质量评估输出
                floatArrayOf(
                    75f,  // 亮度
                    68f,  // 对比度
                    82f,  // 噪点
                    70f,  // 清晰度
                    72f   // 总体
                )
            }
            MODEL_PARAM_PREDICTOR -> {
                // 模拟参数预测输出（18个参数）
                floatArrayOf(
                    0f, 50f, 0f, 0f, 0f, 0f,  // 曝光、对比度、高光、阴影、白、黑
                    0f, 0f, 0f, 0f, 0f,       // 清晰度、自然饱和度、饱和度、色温、色调
                    25f, 25f, 0f, 0f, 0f,     // 锐度、降噪、暗角、颗粒、褪色
                    0f, 0f                    // 分色调高光、分色调阴影
                )
            }
            else -> FloatArray(0)
        }
    }
    
    /**
     * 更新性能统计
     */
    private fun updatePerformanceStats(modelName: String, inferenceTime: Long) {
        val stats = performanceStats.getOrPut(modelName) { PerformanceStats() }
        stats.totalInferences++
        stats.totalTimeMs += inferenceTime
        stats.minTimeMs = minOf(stats.minTimeMs, inferenceTime)
        stats.maxTimeMs = maxOf(stats.maxTimeMs, inferenceTime)
        stats.lastInferenceTimeMs = inferenceTime
        
        Log.d(TAG, "$modelName 推理耗时: ${inferenceTime}ms, 平均: ${stats.averageTimeMs}ms")
    }
    
    /**
     * 清理缓存
     */
    private suspend fun cleanupCache() = cacheMutex.withLock {
        val cacheSize = config.get().cacheSize
        if (resultCache.size > cacheSize) {
            // 删除最旧的缓存项
            val sortedKeys = resultCache.entries.sortedBy { it.value.timestamp }.map { it.key }
            val toRemove = sortedKeys.take(resultCache.size - cacheSize)
            toRemove.forEach { resultCache.remove(it) }
        }
    }
    
    /**
     * 获取推理状态
     */
    fun getState(): InferenceState = state.get()
    
    /**
     * 获取模型信息
     */
    fun getModelInfo(modelName: String): ModelInfo? = modelInfoMap[modelName]
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats(modelName: String): PerformanceStats? = performanceStats[modelName]
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        resultCache.clear()
        Log.i(TAG, "推理结果缓存已清除")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        inferenceScope.cancel()
        
        interpreters.values.forEach { it.close() }
        interpreters.clear()
        
        gpuDelegate?.close()
        gpuDelegate = null
        
        nnapiDelegate?.close()
        nnapiDelegate = null
        
        resultCache.clear()
        performanceStats.clear()
        modelInfoMap.clear()
        
        state.set(InferenceState.IDLE)
        Log.i(TAG, "TFLite引擎资源已释放")
    }
    
    /**
     * 预处理Bitmap为模型输入
     * 
     * @param bitmap 输入图像
     * @param targetSize 目标尺寸
     * @param normalize 是否归一化到[0,1]
     * @return ByteBuffer输入
     */
    fun preprocessBitmap(
        bitmap: Bitmap,
        targetSize: Int = 224,
        normalize: Boolean = true
    ): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val batch = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4)
        batch.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(targetSize * targetSize)
        resized.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            batch.putFloat(if (normalize) r / 255f else r.toFloat())
            batch.putFloat(if (normalize) g / 255f else g.toFloat())
            batch.putFloat(if (normalize) b / 255f else b.toFloat())
        }
        
        batch.rewind()
        return batch
    }
    
    /**
     * 检查硬件加速支持情况
     */
    fun getHardwareAccelerationInfo(): Map<String, Boolean> {
        return mapOf(
            "GPU" to gpuCompatibilityList.isDelegateSupportedOnThisDevice,
            "NNAPI" to (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P),
            "XNNPACK" to true
        )
    }
}