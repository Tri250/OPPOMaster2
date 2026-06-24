package com.silas.omaster.data.lut

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.data.repository.LUTResourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * LUT 持久化管理器
 *
 * 职责：
 * 1. 下载 .cube 文件到公共 Download 目录
 * 2. 收藏/下载状态持久化（SharedPreferences）
 * 3. 本地 LUT 文件扫描与缓存
 * 4. 解析 .cube 文件为 LUT3DData 并缓存
 * 5. 提供 LUT 应用能力（CPU 回退）
 */
class LUTManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LUTManager"
        private const val PREFS_NAME = "omaster_lut_prefs"
        private const val KEY_LIKED_IDS = "liked_lut_ids"
        private const val KEY_DOWNLOADED_IDS = "downloaded_lut_ids"
        private const val KEY_LUT_STRENGTH = "lut_strength"
        private const val KEY_ACTIVE_LUT_ID = "active_lut_id"

        @Volatile
        private var instance: LUTManager? = null

        fun getInstance(context: Context): LUTManager {
            return instance ?: synchronized(this) {
                instance ?: LUTManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 收藏的 LUT ID 集合
    private val _likedIds = MutableStateFlow<Set<String>>(loadStringSet(KEY_LIKED_IDS))
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    // 已下载的 LUT ID 集合
    private val _downloadedIds = MutableStateFlow<Set<String>>(loadStringSet(KEY_DOWNLOADED_IDS))
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    // 当前激活的 LUT ID
    private val _activeLUTId = MutableStateFlow(prefs.getString(KEY_ACTIVE_LUT_ID, null))
    val activeLUTId: StateFlow<String?> = _activeLUTId.asStateFlow()

    // LUT 强度
    private val _lutStrength = MutableStateFlow(prefs.getFloat(KEY_LUT_STRENGTH, 1.0f))
    val lutStrength: StateFlow<Float> = _lutStrength.asStateFlow()

    // 解析后的 LUT 数据缓存
    private val lutDataCache = mutableMapOf<String, LUT3DData>()

    // 下载进度
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    init {
        // 启动时扫描本地文件，恢复下载状态
        scanLocalFiles()
    }

    // ========== 收藏管理 ==========

    fun isLiked(lutId: String): Boolean = _likedIds.value.contains(lutId)

    fun toggleLike(lutId: String) {
        val current = _likedIds.value.toMutableSet()
        if (current.contains(lutId)) current.remove(lutId) else current.add(lutId)
        _likedIds.value = current
        saveStringSet(KEY_LIKED_IDS, current)
    }

    // ========== 下载管理 ==========

    fun isDownloaded(lutId: String): Boolean = _downloadedIds.value.contains(lutId)

    /**
     * 下载 LUT 文件到公共 Download 目录
     *
     * 使用 HttpURLConnection 下载，支持进度回调。
     * 下载完成后自动更新下载状态并解析缓存。
     *
     * @param resource LUT 资源
     * @return 下载成功返回本地文件，失败返回 null
     */
    suspend fun downloadLUT(resource: LUTResource): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(resource.downloadUrl)
            if (!url.protocol.startsWith("https")) {
                Log.w(TAG, "Non-HTTPS download URL: ${resource.downloadUrl}")
            }

            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.requestProperty("Accept", "*/*")
            connection.requestProperty("User-Agent", "OMaster/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP $responseCode for ${resource.id}")
                return@withContext null
            }

            val contentLength = connection.contentLengthLong
            val inputStream = connection.inputStream

            // 保存到公共 Download 目录
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val lutDir = File(downloadDir, "OMaster/LUTs")
            if (!lutDir.exists()) lutDir.mkdirs()

            val fileName = "${resource.nameEn.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.${resource.format}"
            val targetFile = File(lutDir, fileName)

            // 同时保存到应用私有目录（用于内部快速访问）
            val privateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
            if (!privateDir.exists()) privateDir.mkdirs()
            val privateFile = File(privateDir, fileName)

            FileOutputStream(targetFile).use { output ->
                FileOutputStream(privateFile).use { privateOutput ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        privateOutput.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt()
                            val current = _downloadProgress.value.toMutableMap()
                            current[resource.id] = progress
                            _downloadProgress.value = current
                        }
                    }
                }
            }

            inputStream.close()
            connection.disconnect()

            // 更新下载状态
            val current = _downloadedIds.value.toMutableSet()
            current.add(resource.id)
            _downloadedIds.value = current
            saveStringSet(KEY_DOWNLOADED_IDS, current)

            // 预解析并缓存
            parseAndCache(resource.id, targetFile)

            // 清除下载进度
            val progress = _downloadProgress.value.toMutableMap()
            progress.remove(resource.id)
            _downloadProgress.value = progress

            Log.d(TAG, "LUT downloaded: ${resource.name} → ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${resource.id}", e)
            val progress = _downloadProgress.value.toMutableMap()
            progress.remove(resource.id)
            _downloadProgress.value = progress
            null
        }
    }

    /**
     * 获取 LUT 本地文件路径
     */
    fun getLocalFile(resource: LUTResource): File? {
        val fileName = "${resource.nameEn.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.${resource.format}"

        // 优先查找私有目录
        val privateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
        val privateFile = File(privateDir, fileName)
        if (privateFile.exists()) return privateFile

        // 其次查找公共 Download 目录
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicFile = File(downloadDir, "OMaster/LUTs/$fileName")
        if (publicFile.exists()) return publicFile

        return null
    }

    // ========== LUT 解析与缓存 ==========

    /**
     * 解析 .cube 文件并缓存
     */
    fun parseAndCache(lutId: String, file: File): LUT3DData? {
        if (lutDataCache.containsKey(lutId)) return lutDataCache[lutId]

        val data = LUT3DParser.parse(file)
        if (data != null) {
            lutDataCache[lutId] = data
        }
        return data
    }

    /**
     * 直接缓存 LUT3DData（用于风格 LUT 生成器等场景）
     */
    fun parseAndCache(lutId: String, lutData: LUT3DData): LUT3DData {
        lutDataCache[lutId] = lutData
        return lutData
    }

    /**
     * 获取缓存的 LUT 数据
     */
    fun getCachedLUTData(lutId: String): LUT3DData? {
        // 如果缓存中没有，尝试从本地文件解析
        if (!lutDataCache.containsKey(lutId)) {
            val resource = LUTResourceRepository.RESOURCES.find { it.id == lutId } ?: return null
            val file = getLocalFile(resource) ?: return null
            parseAndCache(lutId, file)
        }
        return lutDataCache[lutId]
    }

    /**
     * 将 LUT 应用到 Bitmap（CPU 回退）
     */
    suspend fun applyLUTToBitmap(
        bitmap: android.graphics.Bitmap,
        lutId: String,
        strength: Float = 1.0f
    ): android.graphics.Bitmap? {
        val lutData = getCachedLUTData(lutId) ?: return null
        return LUT3DRenderer.applyLUTCPU(bitmap, lutData, strength)
    }

    // ========== 激活 LUT ==========

    fun setActiveLUT(lutId: String?) {
        _activeLUTId.value = lutId
        prefs.edit().putString(KEY_ACTIVE_LUT_ID, lutId).apply()
    }

    fun setLUTStrength(strength: Float) {
        _lutStrength.value = strength
        prefs.edit().putFloat(KEY_LUT_STRENGTH, strength).apply()
    }

    // ========== 本地文件扫描 ==========

    /**
     * 扫描本地 LUT 文件，恢复下载状态
     */
    private fun scanLocalFiles() {
        val downloaded = mutableSetOf<String>()

        // 扫描私有目录
        val privateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
        if (privateDir.exists()) {
            scanDirectory(privateDir, downloaded)
        }

        // 扫描公共 Download 目录
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "OMaster/LUTs"
        )
        if (downloadDir.exists()) {
            scanDirectory(downloadDir, downloaded)
        }

        // 合并已有状态
        val existing = _downloadedIds.value.toMutableSet()
        existing.addAll(downloaded)
        _downloadedIds.value = existing
        saveStringSet(KEY_DOWNLOADED_IDS, existing)
    }

    private fun scanDirectory(dir: File, downloaded: MutableSet<String>) {
        dir.listFiles()?.filter { it.extension == "cube" }?.forEach { file ->
            // 尝试匹配资源 ID
            val matched = LUTResourceRepository.RESOURCES.find { resource ->
                val expectedName = resource.nameEn.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                file.nameWithoutExtension == expectedName
            }
            if (matched != null) {
                downloaded.add(matched.id)
            }
        }
    }

    // ========== SharedPreferences 辅助 ==========

    private fun loadStringSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    private fun saveStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).apply()
    }
}
