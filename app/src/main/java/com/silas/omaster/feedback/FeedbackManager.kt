package com.silas.omaster.feedback

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silas.omaster.model.HasselbladParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 反馈管理器
 *
 * 职责：
 * 1. 收集并序列化用户反馈到本地磁盘（pending 队列）
 * 2. 调度上传：网络可用时批量上传，失败自动重试
 * 3. 暴露上传状态供 UI 层观察
 */
class FeedbackManager(context: Context) {

    companion object {
        private const val TAG = "FeedbackManager"
        private const val PENDING_DIR = "feedback/pending"
        private const val UPLOADED_DIR = "feedback/uploaded"
        private const val SCREENSHOT_DIR = "feedback/screenshots"
        private const val RETRY_DELAY_MS = 30_000L
        private const val MAX_RETRY_COUNT = 3
        private const val MAX_PENDING_COUNT = 100
        private const val MAX_SCREENSHOT_DIMENSION = 1080
    }

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val uploader = FeedbackUploader(appContext)

    private val pendingDir = File(appContext.filesDir, PENDING_DIR).apply { mkdirs() }
    private val uploadedDir = File(appContext.filesDir, UPLOADED_DIR).apply { mkdirs() }
    private val screenshotDir = File(appContext.filesDir, SCREENSHOT_DIR).apply { mkdirs() }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 上传状态
    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    // 待上传数量
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    // 防止并发重复上传：正在上传的反馈 ID 集合
    private val uploadingIds = java.util.Collections.newSetFromMap<String>(java.util.concurrent.ConcurrentHashMap())

    init {
        refreshPendingCount()
        startUploadWorker()
    }

    /**
     * 提交一条新反馈。
     * 截图会被保存到本地，反馈 JSON 写入 pending 队列，然后触发上传尝试。
     * 当 pending 队列超过 [MAX_PENDING_COUNT] 时，自动删除最旧的条目。
     */
    fun submitFeedback(
        rating: Int,
        tags: List<String>,
        comment: String,
        screenshot: Bitmap? = null,
        sceneId: String? = null,
        recipeId: String? = null,
        params: HasselbladParams? = null
    ) {
        managerScope.launch {
            enforcePendingLimit()

            val id = UUID.randomUUID().toString()
            val screenshotPath = screenshot?.let { saveScreenshot(it, id) }

            val entry = FeedbackEntry(
                id = id,
                rating = rating,
                tags = tags,
                comment = comment,
                screenshotPath = screenshotPath,
                sceneId = sceneId,
                recipeId = recipeId,
                params = params,
                deviceInfo = buildDeviceInfo()
            )

            val file = File(pendingDir, "$id.json")
            file.writeText(gson.toJson(entry))
            Log.i(TAG, "Feedback saved to pending: $id")

            refreshPendingCount()
            attemptUpload(entry)
        }
    }

    /**
     * 限制 pending 队列大小，超出时删除最旧的条目。
     */
    private fun enforcePendingLimit() {
        val files = pendingDir.listFiles { _, name -> name.endsWith(".json") } ?: return
        if (files.size >= MAX_PENDING_COUNT) {
            files.sortBy { it.lastModified() }
            val toDelete = files.take(files.size - MAX_PENDING_COUNT + 1)
            toDelete.forEach { file ->
                val id = file.nameWithoutExtension
                File(screenshotDir, "feedback_$id.jpg").delete()
                file.delete()
                Log.w(TAG, "Pending limit reached, removed oldest feedback: $id")
            }
        }
    }

    /**
     * 手动触发重试所有待上传反馈。
     */
    fun retryAll() {
        managerScope.launch {
            val files = pendingDir.listFiles { _, name -> name.endsWith(".json") } ?: return@launch
            files.mapNotNull { readEntry(it) }.forEach { attemptUpload(it) }
        }
    }

    /**
     * 尝试上传单条反馈，失败则保留在 pending 目录。
     * 使用 [uploadingIds] 防止并发重复上传同一条目。
     */
    private suspend fun attemptUpload(entry: FeedbackEntry) {
        if (!uploadingIds.add(entry.id)) {
            // 已有其他协程正在上传该条目，跳过
            return
        }
        try {
            _uploadStatus.value = UploadStatus.Uploading(entry.id)

            val success = uploader.upload(entry)

            if (success) {
                moveToUploaded(entry.id)
                _uploadStatus.value = UploadStatus.Success(entry.id)
            } else {
                val retryFile = File(pendingDir, "${entry.id}.retry")
                val currentRetry = try { retryFile.readText().toInt() } catch (_: Exception) { 0 }
                if (currentRetry < MAX_RETRY_COUNT) {
                    retryFile.writeText((currentRetry + 1).toString())
                    Log.w(TAG, "Feedback ${entry.id} upload failed, retry ${currentRetry + 1}/$MAX_RETRY_COUNT")
                    _uploadStatus.value = UploadStatus.RetryScheduled(entry.id, currentRetry + 1)
                } else {
                    Log.e(TAG, "Feedback ${entry.id} reached max retries, kept in pending")
                    _uploadStatus.value = UploadStatus.Failed(entry.id)
                }
            }
            refreshPendingCount()
        } finally {
            uploadingIds.remove(entry.id)
        }
    }

    /**
     * 后台上传工作器：定期扫描 pending 队列并上传。
     * 仅在网络可用时尝试上传，避免无网络环境下持续耗电。
     */
    private fun startUploadWorker() {
        managerScope.launch {
            while (true) {
                delay(RETRY_DELAY_MS)
                if (!isNetworkAvailable()) {
                    Log.d(TAG, "Network unavailable, skipping upload check")
                    continue
                }
                val files = pendingDir.listFiles { _, name -> name.endsWith(".json") } ?: continue
                files.mapNotNull { readEntry(it) }.forEach { attemptUpload(it) }
            }
        }
    }

    /**
     * 检查当前设备是否有可用的网络连接（且网络已验证）。
     */
    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun moveToUploaded(id: String) {
        val pendingFile = File(pendingDir, "$id.json")
        val retryFile = File(pendingDir, "$id.retry")
        if (pendingFile.exists()) {
            pendingFile.renameTo(File(uploadedDir, "$id.json"))
        }
        retryFile.delete()
    }

    private fun readEntry(file: File): FeedbackEntry? {
        return try {
            gson.fromJson(file.readText(), object : TypeToken<FeedbackEntry>() {}.type)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse feedback entry: ${file.name}", e)
            null
        }
    }

    private fun refreshPendingCount() {
        _pendingCount.value = pendingDir.listFiles { _, name -> name.endsWith(".json") }?.size ?: 0
    }

    private fun saveScreenshot(bitmap: Bitmap, id: String): String {
        val scaled = scaleBitmapIfNeeded(bitmap)
        val file = File(screenshotDir, "feedback_$id.jpg")
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }

    /**
     * 若截图任一维度超过 [MAX_SCREENSHOT_DIMENSION]，按比例缩放至限制内。
     */
    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = MAX_SCREENSHOT_DIMENSION
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun buildDeviceInfo(): DeviceInfo {
        val pm = appContext.packageManager
        val pkgName = appContext.packageName
        val appVersion = try {
            pm.getPackageInfo(pkgName, 0).versionName ?: "unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        }
        return DeviceInfo(
            model = Build.MODEL,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = appVersion
        )
    }

    fun release() {
        managerScope.cancel()
        uploader.release()
    }

    sealed class UploadStatus {
        object Idle : UploadStatus()
        data class Uploading(val id: String) : UploadStatus()
        data class Success(val id: String) : UploadStatus()
        data class RetryScheduled(val id: String, val retryCount: Int) : UploadStatus()
        data class Failed(val id: String) : UploadStatus()
    }
}
