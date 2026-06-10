package com.silas.omaster.data.local

import android.content.Context
import android.util.Log
import com.silas.omaster.data.model.LUTResource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * LUT 下载回调接口
 */
interface LUTDownloadCallback {
    fun onStart(lutId: String)
    fun onProgress(lutId: String, bytesDownloaded: Long, totalBytes: Long)
    fun onSuccess(lutId: String, file: File)
    fun onError(lutId: String, error: Throwable, retryCount: Int)
    fun onRetry(lutId: String, attempt: Int)
    fun onVerifyStart(lutId: String)
    fun onVerifySuccess(lutId: String)
    fun onVerifyFailed(lutId: String, reason: String)
}

/**
 * LUT 下载结果
 */
sealed class LUTDownloadResult {
    data class Success(val file: File, val verified: Boolean) : LUTDownloadResult()
    data class Error(val exception: Throwable, val retryCount: Int) : LUTDownloadResult()
}

/**
 * LUT 下载管理器
 * 
 * 功能：
 * - LUT .cube 文件下载（带重试机制）
 * - SHA-256 校验和验证
 * - 原子写入（临时文件 + 重命名）
 * - 损坏文件恢复
 * - 版本管理
 */
object LUTDownloadManager {

    private const val TAG = "LUTDownloadManager"
    private const val TIMEOUT_MS = 60000L  // LUT 文件较大，超时时间更长
    private const val MAX_RETRIES = 3
    private const val LUT_DIR = "luts"

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MS
                connectTimeoutMillis = TIMEOUT_MS
                socketTimeoutMillis = TIMEOUT_MS
            }
        }
    }

    // 失败下载记录（用于后台重试）
    private val failedDownloads = mutableSetOf<String>()

    /**
     * 下载 LUT 文件（带校验）
     * 
     * 流程：
     * 1. 检查本地缓存
     * 2. 下载到临时文件
     * 3. 校验 SHA-256
     * 4. 原子写入（重命名）
     * 
     * @param context 上下文
     * @param lut LUT 资源信息
     * @param maxRetries 最大重试次数
     * @param callback 下载回调
     * @return 下载结果
     */
    suspend fun downloadLUT(
        context: Context,
        lut: LUTResource,
        maxRetries: Int = MAX_RETRIES,
        callback: LUTDownloadCallback? = null
    ): LUTDownloadResult = withContext(Dispatchers.IO) {
        
        val localFile = lut.getLocalPath(context)
        
        // 1. 检查本地缓存是否已存在且完整
        if (lut.isDownloaded(context) && lut.verifyIntegrity(context)) {
            Log.d(TAG, "LUT ${lut.id} 已缓存且校验通过")
            failedDownloads.remove(lut.id)
            return@withContext LUTDownloadResult.Success(localFile, true)
        }
        
        // 2. 检查是否有损坏文件，删除后重新下载
        if (localFile.exists() && !lut.verifyIntegrity(context)) {
            Log.w(TAG, "LUT ${lut.id} 文件损坏，删除后重新下载")
            localFile.delete()
        }
        
        callback?.onStart(lut.id)
        
        var lastException: Exception? = null
        
        // 3. 重试下载
        repeat(maxRetries) { attempt ->
            try {
                if (attempt > 0) {
                    callback?.onRetry(lut.id, attempt)
                    Log.d(TAG, "第 ${attempt + 1} 次重试下载 LUT: ${lut.id}")
                    delay(1000L * attempt)  // 指数退避
                }
                
                // 创建目录
                localFile.parentFile?.mkdirs()
                
                // 下载到临时文件
                val tempFile = File(localFile.absolutePath + ".tmp")
                
                Log.d(TAG, "开始下载 LUT: ${lut.id} from ${lut.downloadUrl}")
                
                val bytes = client.get(lut.downloadUrl).bodyAsBytes()
                
                // 写入临时文件
                tempFile.writeBytes(bytes)
                
                // 4. 校验 SHA-256（如果提供）
                callback?.onVerifyStart(lut.id)
                
                if (lut.checksum != null) {
                    val actualChecksum = calculateSHA256(tempFile)
                    if (actualChecksum != lut.checksum) {
                        Log.w(TAG, "LUT ${lut.id} 校验失败: expected=${lut.checksum}, actual=$actualChecksum")
                        callback?.onVerifyFailed(lut.id, "SHA-256 校验失败")
                        tempFile.delete()
                        throw SecurityException("SHA-256 校验失败")
                    }
                }
                
                // 校验文件大小（如果提供）
                if (lut.fileSize != null && tempFile.length() != lut.fileSize) {
                    Log.w(TAG, "LUT ${lut.id} 文件大小不匹配: expected=${lut.fileSize}, actual=${tempFile.length()}")
                    callback?.onVerifyFailed(lut.id, "文件大小不匹配")
                    tempFile.delete()
                    throw SecurityException("文件大小不匹配")
                }
                
                callback?.onVerifySuccess(lut.id)
                
                // 5. 原子写入（重命名）
                if (!tempFile.renameTo(localFile)) {
                    // 重命名失败，尝试复制
                    tempFile.copyTo(localFile, overwrite = true)
                    tempFile.delete()
                }
                
                Log.d(TAG, "LUT ${lut.id} 下载成功 (${bytes.size / 1024}KB)")
                failedDownloads.remove(lut.id)
                callback?.onSuccess(lut.id, localFile)
                
                return@withContext LUTDownloadResult.Success(localFile, true)
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "下载 LUT ${lut.id} 失败 (尝试 ${attempt + 1}/$maxRetries): ${e.message}")
                
                if (attempt == maxRetries - 1) {
                    failedDownloads.add(lut.id)
                    callback?.onError(lut.id, e, maxRetries)
                    Log.e(TAG, "下载 LUT ${lut.id} 最终失败", e)
                }
            }
        }
        
        LUTDownloadResult.Error(lastException ?: Exception("未知错误"), maxRetries)
    }

    /**
     * 批量下载 LUT
     */
    suspend fun downloadLUTs(
        context: Context,
        luts: List<LUTResource>,
        callback: LUTDownloadCallback? = null
    ): Map<String, LUTDownloadResult> {
        val results = mutableMapOf<String, LUTDownloadResult>()
        
        luts.forEach { lut ->
            results[lut.id] = downloadLUT(context, lut, callback = callback)
        }
        
        return results
    }

    /**
     * 获取失败的下载列表
     */
    fun getFailedDownloads(): Set<String> = failedDownloads.toSet()

    /**
     * 重试所有失败的下载
     */
    suspend fun retryFailedDownloads(
        context: Context,
        luts: List<LUTResource>,
        callback: LUTDownloadCallback? = null
    ): Int {
        val toRetry = luts.filter { failedDownloads.contains(it.id) }
        var successCount = 0
        
        toRetry.forEach { lut ->
            val result = downloadLUT(context, lut, callback = callback)
            if (result is LUTDownloadResult.Success) {
                failedDownloads.remove(lut.id)
                successCount++
            }
        }
        
        Log.d(TAG, "重试完成: $successCount/${toRetry.size} 成功")
        return successCount
    }

    /**
     * 解析 CUBE 文件为 3D LUT 数据
     * 
     * @param file CUBE 文件
     * @return 3D LUT 数据数组（RGB 值）
     */
    fun parseCubeFile(file: File): FloatArray? {
        return try {
            val lines = file.readLines()
            var cubeSize = 33  // 默认尺寸
            var dataStartIndex = 0
            
            // 解析头部信息
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("LUT_3D_SIZE")) {
                    cubeSize = line.split(" ").last().toInt()
                }
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("TITLE") ||
                    line.startsWith("LUT_3D_SIZE") || line.startsWith("DOMAIN_MIN") || 
                    line.startsWith("DOMAIN_MAX")) {
                    continue
                }
                dataStartIndex = i
                break
            }
            
            // 解析 RGB 数据
            val lutData = FloatArray(cubeSize * cubeSize * cubeSize * 3)
            var dataIndex = 0
            
            for (i in dataStartIndex until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                
                val values = line.split(" ").map { it.toFloatOrNull() ?: 0f }
                if (values.size >= 3) {
                    lutData[dataIndex++] = values[0]  // R
                    lutData[dataIndex++] = values[1]  // G
                    lutData[dataIndex++] = values[2]  // B
                }
            }
            
            Log.d(TAG, "解析 CUBE 文件成功: size=$cubeSize, dataPoints=${dataIndex / 3}")
            lutData
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 CUBE 文件失败: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * 清理旧版本 LUT 文件
     */
    suspend fun cleanOldVersions(context: Context, currentLUTs: List<LUTResource>) {
        withContext(Dispatchers.IO) {
            val lutDir = File(context.filesDir, LUT_DIR)
            if (!lutDir.exists()) return@withContext
            
            val currentIds = currentLUTs.map { "${it.id}_${it.version}" }.toSet()
            
            lutDir.listFiles()?.forEach { file ->
                val fileName = file.nameWithoutExtension
                if (!currentIds.contains(fileName) && fileName.endsWith(".cube")) {
                    Log.d(TAG, "删除旧版本 LUT: ${file.name}")
                    file.delete()
                }
            }
        }
    }

    /**
     * 获取缓存大小（MB）
     */
    fun getCacheSize(context: Context): Double {
        val lutDir = File(context.filesDir, LUT_DIR)
        if (!lutDir.exists()) return 0.0
        
        val size = lutDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
        
        return size / (1024.0 * 1024.0)
    }

    /**
     * 清除所有缓存
     */
    fun clearCache(context: Context) {
        File(context.filesDir, LUT_DIR).deleteRecursively()
        failedDownloads.clear()
        Log.d(TAG, "LUT 缓存已清空")
    }

    /**
     * 计算 SHA-256 校验和
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        java.io.FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}