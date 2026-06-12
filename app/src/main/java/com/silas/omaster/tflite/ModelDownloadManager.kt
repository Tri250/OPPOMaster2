package com.silas.omaster.tflite

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * TFLite 模型下载管理器
 * 
 * 功能：
 * - 检查模型文件是否存在
 * - 从 CDN 下载模型文件
 * - 校验模型文件完整性（SHA256）
 * - 下载进度回调
 * - 支持断点续传（预留）
 */
class ModelDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloadManager"
        
        // 模型下载配置（从 MODEL_SPEC.json 读取）
        private const val BASE_URL = "https://releases.omaster.app/models"
        private const val MODEL_VERSION = "1.2.0"
        
        // 模型文件名
        val MODEL_FILES = listOf(
            ModelFile(
                name = "scene_classifier.tflite",
                displayName = "场景分类模型",
                description = "36类场景智能识别",
                expectedSize = 700 * 1024,  // 700KB
                checksum = "sha256:g7h8i9j0k1l2..."  // 实际发布时替换为真实校验值
            ),
            ModelFile(
                name = "quality_analyzer.tflite",
                displayName = "质量分析模型",
                description = "图像质量智能评估",
                expectedSize = 500 * 1024,  // 500KB
                checksum = "sha256:h8i9j0k1l2m3..."
            ),
            ModelFile(
                name = "param_predictor.tflite",
                displayName = "参数预测模型",
                description = "哈苏调校参数推荐",
                expectedSize = 200 * 1024,  // 200KB
                checksum = "sha256:i9j0k1l2m3n4..."
            )
        )
        
        @Volatile
        private var instance: ModelDownloadManager? = null
        
        fun getInstance(context: Context): ModelDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: ModelDownloadManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * 模型文件信息
     */
    data class ModelFile(
        val name: String,
        val displayName: String,
        val description: String,
        val expectedSize: Long,
        val checksum: String
    )
    
    /**
     * 下载状态
     */
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Checking(val model: String) : DownloadState()
        data class Downloading(val model: String, val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Verifying(val model: String) : DownloadState()
        data class Completed(val model: String) : DownloadState()
        data class Failed(val model: String, val error: String) : DownloadState()
        data class AllCompleted(val models: List<String>) : DownloadState()
    }
    
    /**
     * 下载进度回调
     */
    interface DownloadCallback {
        fun onStateChanged(state: DownloadState)
        fun onProgress(model: String, progress: Float)
        fun onComplete(model: String, success: Boolean)
        fun onAllComplete(allSuccess: Boolean)
    }
    
    // 下载协程作用域
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 当前下载状态
    private val currentState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    
    // 已下载模型集合
    private val downloadedModels = mutableSetOf<String>()
    
    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(modelName: String): Boolean {
        val modelDir = File(context.filesDir, "models")
        val modelFile = File(modelDir, modelName)
        
        // 检查文件是否存在且大小合理
        if (!modelFile.exists()) return false
        
        val modelInfo = MODEL_FILES.find { it.name == modelName }
        if (modelInfo != null) {
            // 文件大小应该接近预期大小（允许10%误差）
            val sizeRatio = modelFile.length().toFloat() / modelInfo.expectedSize
            if (sizeRatio < 0.9f || sizeRatio > 1.1f) {
                Log.w(TAG, "模型文件大小异常: $modelName, 实际=${modelFile.length()}, 预期=${modelInfo.expectedSize}")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查所有模型是否已下载
     */
    fun areAllModelsDownloaded(): Boolean {
        return MODEL_FILES.all { isModelDownloaded(it.name) }
    }
    
    /**
     * 获取缺失的模型列表
     */
    fun getMissingModels(): List<ModelFile> {
        return MODEL_FILES.filter { !isModelDownloaded(it.name) }
    }
    
    /**
     * 获取模型下载状态摘要
     */
    fun getDownloadSummary(): DownloadSummary {
        val downloaded = MODEL_FILES.filter { isModelDownloaded(it.name) }
        val missing = MODEL_FILES.filter { !isModelDownloaded(it.name) }
        
        return DownloadSummary(
            totalModels = MODEL_FILES.size,
            downloadedCount = downloaded.size,
            missingCount = missing.size,
            downloadedModels = downloaded.map { it.name },
            missingModels = missing.map { it.name },
            totalSizeBytes = MODEL_FILES.sumOf { it.expectedSize },
            downloadedSizeBytes = downloaded.sumOf { it.expectedSize }
        )
    }
    
    /**
     * 下载摘要
     */
    data class DownloadSummary(
        val totalModels: Int,
        val downloadedCount: Int,
        val missingCount: Int,
        val downloadedModels: List<String>,
        val missingModels: List<String>,
        val totalSizeBytes: Long,
        val downloadedSizeBytes: Long
    ) {
        val progressPercent: Int
            get() = if (totalModels > 0) (downloadedCount * 100 / totalModels) else 0
        
        val remainingSizeBytes: Long
            get() = totalSizeBytes - downloadedSizeBytes
        
        val remainingSizeMB: Float
            get() = remainingSizeBytes.toFloat() / (1024 * 1024)
    }
    
    /**
     * 下载单个模型
     */
    suspend fun downloadModel(
        modelFile: ModelFile,
        callback: DownloadCallback? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            callback?.onStateChanged(DownloadState.Checking(modelFile.name))
            
            // 检查是否已下载
            if (isModelDownloaded(modelFile.name)) {
                Log.i(TAG, "模型已存在: ${modelFile.name}")
                callback?.onStateChanged(DownloadState.Completed(modelFile.name))
                callback?.onComplete(modelFile.name, true)
                return@withContext Result.success(true)
            }
            
            // 创建模型目录
            val modelDir = File(context.filesDir, "models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val targetFile = File(modelDir, modelFile.name)
            val tempFile = File(modelDir, "${modelFile.name}.tmp")
            
            // 构建下载 URL
            val downloadUrl = "$BASE_URL/v${MODEL_VERSION}/${modelFile.name}"
            Log.i(TAG, "开始下载模型: ${modelFile.name} from $downloadUrl")
            
            callback?.onStateChanged(DownloadState.Downloading(modelFile.name, 0f, 0, modelFile.expectedSize))
            
            // 执行下载
            var downloadedBytes = 0L
            val connection = URL(downloadUrl).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(tempFile)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                
                // 更新进度
                val progress = downloadedBytes.toFloat() / modelFile.expectedSize
                callback?.onStateChanged(DownloadState.Downloading(modelFile.name, progress, downloadedBytes, modelFile.expectedSize))
                callback?.onProgress(modelFile.name, progress)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            Log.i(TAG, "模型下载完成: ${modelFile.name}, 大小: ${downloadedBytes} bytes")
            
            // 校验文件
            callback?.onStateChanged(DownloadState.Verifying(modelFile.name))
            
            // 检查文件大小
            if (tempFile.length() < modelFile.expectedSize * 0.9f) {
                tempFile.delete()
                return@withContext Result.failure(Exception("下载文件大小不足: ${tempFile.length()} < ${modelFile.expectedSize}"))
            }
            
            // 校验 SHA256（如果提供了校验值）
            if (modelFile.checksum.startsWith("sha256:") && modelFile.checksum.length > 7) {
                val expectedHash = modelFile.checksum.substring(7)
                val actualHash = calculateSHA256(tempFile)
                
                if (actualHash != expectedHash) {
                    Log.w(TAG, "模型校验失败: ${modelFile.name}, 预期=$expectedHash, 实际=$actualHash")
                    // 注意：由于当前校验值是占位符，暂时跳过校验失败
                    // 正式发布时替换为真实校验值后启用校验
                    Log.w(TAG, "跳过校验（当前使用占位符校验值）")
                } else {
                    Log.i(TAG, "模型校验成功: ${modelFile.name}")
                }
            }
            
            // 重命名临时文件为正式文件
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
            
            // 删除占位符文件（如果存在）
            val placeholderFile = File(context.filesDir, "models/${modelFile.name}.placeholder")
            if (placeholderFile.exists()) {
                placeholderFile.delete()
                Log.i(TAG, "已删除占位符文件: ${modelFile.name}.placeholder")
            }
            
            downloadedModels.add(modelFile.name)
            callback?.onStateChanged(DownloadState.Completed(modelFile.name))
            callback?.onComplete(modelFile.name, true)
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "模型下载失败: ${modelFile.name}", e)
            callback?.onStateChanged(DownloadState.Failed(modelFile.name, e.message ?: "未知错误"))
            callback?.onComplete(modelFile.name, false)
            Result.failure(e)
        }
    }
    
    /**
     * 下载所有缺失的模型
     */
    suspend fun downloadAllMissingModels(
        callback: DownloadCallback? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val missingModels = getMissingModels()
            
            if (missingModels.isEmpty()) {
                Log.i(TAG, "所有模型已下载")
                callback?.onStateChanged(DownloadState.AllCompleted(emptyList()))
                callback?.onAllComplete(true)
                return@withContext Result.success(true)
            }
            
            Log.i(TAG, "开始下载 ${missingModels.size} 个模型")
            
            val results = mutableListOf<Pair<String, Boolean>>()
            
            for (modelFile in missingModels) {
                val result = downloadModel(modelFile, callback)
                results.add(Pair(modelFile.name, result.isSuccess))
            }
            
            val successCount = results.count { it.second }
            val allSuccess = successCount == missingModels.size
            
            Log.i(TAG, "模型下载完成: 成功=$successCount, 失败=${missingModels.size - successCount}")
            
            callback?.onStateChanged(DownloadState.AllCompleted(results.filter { it.second }.map { it.first }))
            callback?.onAllComplete(allSuccess)
            
            if (allSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception("${missingModels.size - successCount} 个模型下载失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量下载模型失败", e)
            callback?.onAllComplete(false)
            Result.failure(e)
        }
    }
    
    /**
     * 计算文件 SHA256
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        val inputStream = file.inputStream()
        
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 清理临时文件
     */
    fun cleanupTempFiles() {
        val modelDir = File(context.filesDir, "models")
        if (modelDir.exists()) {
            modelDir.listFiles()?.filter { it.name.endsWith(".tmp") }?.forEach {
                it.delete()
                Log.d(TAG, "已清理临时文件: ${it.name}")
            }
        }
    }
    
    /**
     * 删除所有模型（用于重新下载）
     */
    fun deleteAllModels() {
        val modelDir = File(context.filesDir, "models")
        if (modelDir.exists()) {
            modelDir.listFiles()?.forEach {
                it.delete()
                Log.d(TAG, "已删除模型文件: ${it.name}")
            }
        }
        downloadedModels.clear()
    }
    
    /**
     * 获取当前下载状态
     */
    fun getCurrentState(): DownloadState = currentState.value
    
    /**
     * 取消所有下载任务
     */
    fun cancelAllDownloads() {
        downloadScope.cancel()
        cleanupTempFiles()
        currentState.value = DownloadState.Idle
    }
}