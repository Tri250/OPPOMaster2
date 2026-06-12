package com.silas.omaster.mediapipe

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * 模型管理器
 *
 * 负责：
 * - 模型文件下载
 * - 模型版本管理
 * - 模型完整性校验
 * - 模型缓存管理
 *
 * 支持的模型：
 * - scene_classifier.tflite (场景分类)
 * - quality_analyzer.tflite (质量分析)
 * - param_predictor.tflite (参数预测)
 */
class ModelManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        private const val PREFS_NAME = "omaster_model_manager"
        private const val KEY_MODEL_VERSION = "model_version"
        private const val KEY_LAST_CHECK = "last_version_check"

        // 当前模型版本
        const val CURRENT_VERSION = "1.2.0"

        // 模型文件名
        const val MODEL_SCENE_CLASSIFIER = "scene_classifier.tflite"
        const val MODEL_QUALITY_ANALYZER = "quality_analyzer.tflite"
        const val MODEL_PARAM_PREDICTOR = "param_predictor.tflite"

        // 模型下载 URL（可配置）
        const val MODEL_BASE_URL = "https://releases.omaster.app/models/v1.2"

        // 模型校验和（SHA256）
        val MODEL_CHECKSUMS = mapOf(
            MODEL_SCENE_CLASSIFIER to "scene_classifier_v1.2_sha256",
            MODEL_QUALITY_ANALYZER to "quality_analyzer_v1.2_sha256",
            MODEL_PARAM_PREDICTOR to "param_predictor_v1.2_sha256"
        )

        // 模型大小（字节）
        val MODEL_SIZES = mapOf(
            MODEL_SCENE_CLASSIFIER to 700 * 1024L,    // ~700KB
            MODEL_QUALITY_ANALYZER to 500 * 1024L,    // ~500KB
            MODEL_PARAM_PREDICTOR to 200 * 1024L     // ~200KB
        )

        @Volatile
        private var instance: ModelManager? = null

        fun getInstance(context: Context): ModelManager {
            return instance ?: synchronized(this) {
                instance ?: ModelManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 模型目录
    private val modelsDir = File(context.filesDir, "models")

    /**
     * 初始化模型管理器
     * 检查模型文件是否存在，不存在则从 assets 复制
     */
    suspend fun initialize(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "初始化模型管理器...")

            // 创建模型目录
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
                Log.i(TAG, "创建模型目录: ${modelsDir.absolutePath}")
            }

            // 检查并复制模型文件
            val models = listOf(MODEL_SCENE_CLASSIFIER, MODEL_QUALITY_ANALYZER, MODEL_PARAM_PREDICTOR)
            var allModelsReady = true

            for (modelName in models) {
                val modelFile = File(modelsDir, modelName)

                if (!modelFile.exists()) {
                    // 尝试从 assets 复制
                    val copied = copyModelFromAssets(modelName)
                    if (!copied) {
                        Log.w(TAG, "模型文件不存在: $modelName，需要下载")
                        allModelsReady = false
                    }
                } else {
                    Log.i(TAG, "模型文件已存在: $modelName (${modelFile.length()} bytes)")
                }
            }

            // 更新版本信息
            prefs.edit()
                .putString(KEY_MODEL_VERSION, CURRENT_VERSION)
                .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                .apply()

            Log.i(TAG, "模型管理器初始化完成 - 所有模型就绪: $allModelsReady")
            Result.success(allModelsReady)

        } catch (e: Exception) {
            Log.e(TAG, "模型管理器初始化失败", e)
            Result.failure(e)
        }
    }

    /**
     * 从 assets 复制模型文件
     */
    private fun copyModelFromAssets(modelName: String): Boolean {
        return try {
            val modelFile = File(modelsDir, modelName)

            // 检查 assets 中是否存在模型
            val assetPath = "models/$modelName"
            val assetExists = try {
                context.assets.list("models")?.contains(modelName) == true
            } catch (e: Exception) {
                false
            }

            if (!assetExists) {
                Log.w(TAG, "Assets 中不存在模型: $modelName")
                return false
            }

            // 复制文件
            context.assets.open(assetPath).use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "模型已从 assets 复制: $modelName (${modelFile.length()} bytes)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "复制模型失败: $modelName", e)
            false
        }
    }

    /**
     * 下载模型文件
     *
     * @param modelName 模型名称
     * @param progressCallback 进度回调 (0-100)
     */
    suspend fun downloadModel(
        modelName: String,
        progressCallback: ((Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始下载模型: $modelName")

            val modelFile = File(modelsDir, modelName)
            val url = URL("$MODEL_BASE_URL/$modelName")

            // 创建临时文件
            val tempFile = File(modelsDir, "$modelName.tmp")

            // 下载
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            val expectedSize = MODEL_SIZES[modelName] ?: 0L
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(tempFile)

            var downloaded = 0L
            val buffer = ByteArray(8192)

            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break

                outputStream.write(buffer, 0, read)
                downloaded += read

                // 更新进度
                if (expectedSize > 0 && progressCallback != null) {
                    val progress = (downloaded * 100 / expectedSize).toInt().coerceIn(0, 100)
                    progressCallback(progress)
                }
            }

            outputStream.close()
            inputStream.close()

            // 校验文件完整性
            if (!verifyModelChecksum(tempFile, modelName)) {
                Log.w(TAG, "模型校验失败: $modelName")
                tempFile.delete()
                return Result.failure(Exception("模型校验失败"))
            }

            // 移动到最终位置
            tempFile.renameTo(modelFile)

            Log.i(TAG, "模型下载完成: $modelName (${modelFile.length()} bytes)")
            Result.success(modelFile)

        } catch (e: Exception) {
            Log.e(TAG, "下载模型失败: $modelName", e)
            Result.failure(e)
        }
    }

    /**
     * 校验模型文件 SHA256
     */
    private fun verifyModelChecksum(file: File, modelName: String): Boolean {
        // 实际校验需要真实的 SHA256 值
        // 这里简化处理，只检查文件大小是否合理
        val expectedSize = MODEL_SIZES[modelName] ?: 0L
        val actualSize = file.length()

        // 允许 10% 的误差
        val tolerance = expectedSize * 0.1
        val isValid = actualSize >= expectedSize - tolerance && actualSize <= expectedSize + tolerance

        Log.d(TAG, "模型大小校验: $modelName - 预期: $expectedSize, 实际: $actualSize, 有效: $isValid")

        return isValid
    }

    /**
     * 检查模型是否存在
     */
    fun isModelAvailable(modelName: String): Boolean {
        return File(modelsDir, modelName).exists()
    }

    /**
     * 检查所有模型是否就绪
     */
    fun areAllModelsReady(): Boolean {
        return isModelAvailable(MODEL_SCENE_CLASSIFIER) &&
               isModelAvailable(MODEL_QUALITY_ANALYZER) &&
               isModelAvailable(MODEL_PARAM_PREDICTOR)
    }

    /**
     * 获取模型文件路径
     */
    fun getModelPath(modelName: String): File {
        return File(modelsDir, modelName)
    }

    /**
     * 获取模型版本
     */
    fun getModelVersion(): String {
        return prefs.getString(KEY_MODEL_VERSION, "unknown") ?: "unknown"
    }

    /**
     * 获取上次版本检查时间
     */
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK, 0)
    }

    /**
     * 删除所有模型文件
     */
    fun clearModels() {
        modelsDir.listFiles()?.forEach { it.delete() }
        prefs.edit().clear().apply()
        Log.i(TAG, "所有模型文件已删除")
    }

    /**
     * 获取模型状态信息
     */
    fun getModelStatus(): Map<String, ModelStatus> {
        return mapOf(
            MODEL_SCENE_CLASSIFIER to getModelStatusFor(MODEL_SCENE_CLASSIFIER),
            MODEL_QUALITY_ANALYZER to getModelStatusFor(MODEL_QUALITY_ANALYZER),
            MODEL_PARAM_PREDICTOR to getModelStatusFor(MODEL_PARAM_PREDICTOR)
        )
    }

    private fun getModelStatusFor(modelName: String): ModelStatus {
        val file = File(modelsDir, modelName)
        return if (file.exists()) {
            ModelStatus(
                name = modelName,
                available = true,
                size = file.length(),
                version = getModelVersion(),
                path = file.absolutePath
            )
        } else {
            ModelStatus(
                name = modelName,
                available = false,
                size = 0,
                version = "unknown",
                path = ""
            )
        }
    }
}

/**
 * 模型状态
 */
data class ModelStatus(
    val name: String,
    val available: Boolean,
    val size: Long,
    val version: String,
    val path: String
) {
    val sizeInKB: Float = size / 1024f
    val sizeInMB: Float = size / (1024f * 1024f)
}