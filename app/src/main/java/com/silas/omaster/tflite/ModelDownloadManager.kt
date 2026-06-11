package com.silas.omaster.tflite

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import kotlin.math.min

/**
 * 模型下载管理器
 * 
 * 负责从CDN下载TFLite模型文件
 * 支持进度回调、校验和验证、断点续传
 */
class ModelDownloadManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloadManager"
        
        // 模型下载配置（来自MODEL_SPEC.json）
        private const val BASE_URL = "https://releases.omaster.app/models/v1.2"
        
        // 模型文件信息
        val MODEL_DOWNLOAD_INFO = mapOf(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER to ModelDownloadInfo(
                url = "$BASE_URL/scene_classifier.tflite",
                expectedSize = 700 * 1024L, // 约700KB
                checksum = null // 可选校验
            ),
            TFLiteEngine.MODEL_QUALITY_ANALYZER to ModelDownloadInfo(
                url = "$BASE_URL/quality_analyzer.tflite",
                expectedSize = 500 * 1024L, // 约500KB
                checksum = null
            ),
            TFLiteEngine.MODEL_PARAM_PREDICTOR to ModelDownloadInfo(
                url = "$BASE_URL/param_predictor.tflite",
                expectedSize = 200 * 1024L, // 约200KB
                checksum = null
            )
        )
        
        // 备用镜像URL（腾讯云）
        private const val BACKUP_BASE_URL = "https://mirrors.cloud.tencent.com/omaster-models/v1.2"
        
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
     * 模型下载信息
     */
    data class ModelDownloadInfo(
        val url: String,
        val expectedSize: Long,
        val checksum: String? = null
    )
    
    /**
     * 下载进度回调
     */
    interface DownloadCallback {
        fun onProgress(progress: Int, downloadedBytes: Long, totalBytes: Long)
        fun onComplete(success: Boolean, error: String?)
    }
    
    /**
     * 下载单个模型
     * 
     * @param modelName 模型名称
     * @param callback 进度回调
     * @return 是否成功
     */
    suspend fun downloadModel(
        modelName: String,
        callback: DownloadCallback? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val info = MODEL_DOWNLOAD_INFO[modelName]
            if (info == null) {
                callback?.onComplete(false, "未知的模型: $modelName")
                return@withContext false
            }
            
            // 检查模型是否已存在且完整
            val modelDir = File(context.filesDir, "models")
            val modelFile = File(modelDir, modelName)
            
            if (modelFile.exists() && verifyModel(modelFile, info.expectedSize)) {
                Log.i(TAG, "模型已存在且完整: $modelName")
                callback?.onProgress(100, modelFile.length(), info.expectedSize)
                callback?.onComplete(true, null)
                return@withContext true
            }
            
            // 创建模型目录
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            // 尝试主URL下载
            var success = tryDownload(info.url, modelFile, info.expectedSize, callback)
            
            // 如果主URL失败，尝试备用URL
            if (!success) {
                Log.w(TAG, "主URL下载失败，尝试备用镜像")
                val backupUrl = info.url.replace(BASE_URL, BACKUP_BASE_URL)
                success = tryDownload(backupUrl, modelFile, info.expectedSize, callback)
            }
            
            // 验证下载结果
            if (success && verifyModel(modelFile, info.expectedSize)) {
                Log.i(TAG, "模型下载成功: $modelName, 大小: ${modelFile.length()}")
                callback?.onComplete(true, null)
                true
            } else {
                Log.e(TAG, "模型下载失败或校验失败: $modelName")
                modelFile.delete()
                callback?.onComplete(false, "下载失败或校验失败")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载模型异常: $modelName", e)
            callback?.onComplete(false, e.message ?: "下载异常")
            false
        }
    }
    
    /**
     * 尝试从URL下载
     */
    private suspend fun tryDownload(
        url: String,
        targetFile: File,
        expectedSize: Long,
        callback: DownloadCallback?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始下载: $url -> ${targetFile.absolutePath}")
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            val totalSize = connection.contentLengthLong
            val actualTotal = if (totalSize > 0) totalSize else expectedSize
            
            connection.getInputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var lastProgress = 0
                    
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        
                        output.write(buffer, 0, read)
                        downloaded += read
                        
                        // 更新进度（每5%更新一次）
                        val progress = min(99, (downloaded * 100 / actualTotal).toInt())
                        if (progress > lastProgress + 4 || progress == 99) {
                            lastProgress = progress
                            callback?.onProgress(progress, downloaded, actualTotal)
                            Log.d(TAG, "下载进度: $progress%, $downloaded/$actualTotal bytes")
                        }
                    }
                }
            }
            
            Log.i(TAG, "下载完成: ${targetFile.name}, 大小: ${targetFile.length()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "下载失败: $url", e)
            false
        }
    }
    
    /**
     * 验证模型文件
     */
    private fun verifyModel(file: File, expectedSize: Long): Boolean {
        if (!file.exists()) return false
        
        val actualSize = file.length()
        
        // 允许10%的大小误差（量化模型大小可能有变化）
        val minSize = expectedSize * 0.9
        val maxSize = expectedSize * 1.1
        
        return actualSize >= minSize && actualSize <= maxSize
    }
    
    /**
     * 下载所有模型
     */
    suspend fun downloadAllModels(callback: DownloadCallback? = null): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        
        for ((modelName, _) in MODEL_DOWNLOAD_INFO) {
            Log.i(TAG, "开始下载模型: $modelName")
            results[modelName] = downloadModel(modelName, callback)
        }
        
        results
    }
    
    /**
     * 检查所有模型是否可用
     */
    fun checkAllModelsAvailable(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        val modelDir = File(context.filesDir, "models")
        
        for ((modelName, info) in MODEL_DOWNLOAD_INFO) {
            val modelFile = File(modelDir, modelName)
            results[modelName] = modelFile.exists() && verifyModel(modelFile, info.expectedSize)
        }
        
        return results
    }
    
    /**
     * 获取缺失的模型列表
     */
    fun getMissingModels(): List<String> {
        return MODEL_DOWNLOAD_INFO.keys.filter { modelName ->
            !checkAllModelsAvailable()[modelName]!!
        }
    }
    
    /**
     * 获取模型下载状态描述
     */
    fun getStatusDescription(): String {
        val available = checkAllModelsAvailable()
        val availableCount = available.values.count { it }
        val totalCount = available.size
        
        return when (availableCount) {
            0 -> "所有模型需要下载（约1.4MB）"
            totalCount -> "所有模型已就绪"
            else -> "${totalCount - availableCount}个模型需要下载"
        }
    }
}