package com.silas.omaster.ai.scene

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 场景识别模型管理器
 *
 * 负责 TFLite 模型的下载、缓存和版本管理
 *
 * 模型来源：
 * - 优先从本地缓存加载
 * - 缓存不存在时从远程下载
 * - 下载失败时使用启发式分析器替代
 */
class SceneModelManager(context: Context) {

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
        ERROR
    }

    companion object {
        private const val TAG = "SceneModelManager"
        const val MODEL_FILENAME = "scene_classifier.tflite"
        const val MODEL_VERSION = 1
        const val DEFAULT_MODEL_URL = "https://github.com/omaster/models/releases/download/v1/scene_classifier.tflite"
    }

    private val modelDir = File(context.filesDir, "ml_models")
    private val modelFile = File(modelDir, MODEL_FILENAME)
    private val versionFile = File(modelDir, "model_version.txt")

    val isModelAvailable: Boolean get() = modelFile.exists() && modelFile.length() > 0

    /**
     * 获取当前缓存的模型版本号，无缓存返回 0
     */
    val cachedModelVersion: Int
        get() = if (versionFile.exists()) {
            try { versionFile.readText().trim().toInt() } catch (_: Exception) { 0 }
        } else 0

    /**
     * 确保模型可用
     * 如果本地已有则直接返回true
     * 如果没有则尝试下载
     */
    suspend fun ensureModelAvailable(onProgress: ((Float) -> Unit)? = null): Boolean {
        if (isModelAvailable) return true
        return downloadModel(onProgress = onProgress)
    }

    /**
     * 从远程下载模型
     */
    suspend fun downloadModel(
        url: String = DEFAULT_MODEL_URL,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            modelDir.mkdirs()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "模型下载失败：HTTP $responseCode")
                return@withContext false
            }

            val totalSize = connection.contentLength.toLong()
            var downloadedSize = 0L

            val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        if (totalSize > 0) {
                            onProgress?.invoke(downloadedSize.toFloat() / totalSize)
                        }
                    }
                }
            }

            // 校验文件大小（至少 1KB，防止下载到空文件或错误页面）
            if (tempFile.length() < 1024) {
                Log.e(TAG, "下载的模型文件过小（${tempFile.length()} 字节），可能不是有效模型")
                tempFile.delete()
                return@withContext false
            }

            // 删除旧模型并重命名临时文件
            if (modelFile.exists()) modelFile.delete()
            val renamed = tempFile.renameTo(modelFile)
            if (!renamed) {
                Log.e(TAG, "重命名临时模型文件失败")
                tempFile.delete()
                return@withContext false
            }

            // 写入版本号
            versionFile.writeText(MODEL_VERSION.toString())

            Log.d(TAG, "模型下载完成：${modelFile.absolutePath}，大小 ${modelFile.length()} 字节")
            true
        } catch (e: Exception) {
            Log.e(TAG, "模型下载失败: ${e.message}", e)
            File(modelDir, "$MODEL_FILENAME.tmp").delete()
            false
        }
    }

    /**
     * 获取模型文件路径，不存在返回 null
     */
    fun getModelPath(): String? {
        return if (modelFile.exists() && modelFile.length() > 0) modelFile.absolutePath else null
    }

    /**
     * 删除已缓存的模型
     */
    fun deleteModel(): Boolean {
        val deletedModel = if (modelFile.exists()) modelFile.delete() else true
        val deletedVersion = if (versionFile.exists()) versionFile.delete() else true
        val deletedTemp = File(modelDir, "$MODEL_FILENAME.tmp").let { if (it.exists()) it.delete() else true }
        return deletedModel && deletedVersion && deletedTemp
    }

    /**
     * 检查是否需要更新模型（本地版本低于 MODEL_VERSION）
     */
    fun needsUpdate(): Boolean {
        if (!isModelAvailable) return true
        return cachedModelVersion < MODEL_VERSION
    }
}
