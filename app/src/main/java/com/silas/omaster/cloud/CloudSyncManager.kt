package com.silas.omaster.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.silas.omaster.util.SecurityCrypto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * 云端同步管理器（Singleton）
 *
 * 职责：
 * 1. 管理云端服务提供者（WebDAV / Google Drive）配置
 * 2. 预设与设置的自动同步（带防抖）
 * 3. 冲突解决：last-write-wins + 版本追踪
 * 4. 数据加密：同步前加密、下载后解密（SecurityCrypto）
 * 5. 错误重试：指数退避重试（最多3次）
 * 6. 同步状态通过 StateFlow 暴露给 UI
 */
class CloudSyncManager private constructor(private val context: Context) {

    // ==================== 同步状态 ====================

    sealed class SyncStatus {
        data object Idle : SyncStatus()
        data object Syncing : SyncStatus()
        data class Success(val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // ==================== 自动同步配置 ====================

    private val _autoSyncEnabled = MutableStateFlow(false)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    // ==================== 连接状态 ====================

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ==================== 当前提供者 ====================

    private val _currentProvider = MutableStateFlow<CloudProvider?>(null)
    val currentProvider: StateFlow<CloudProvider?> = _currentProvider.asStateFlow()

    // ==================== 上次同步时间 ====================

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // ==================== 内部状态 ====================

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null
    private var syncVersion = 0L

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    init {
        loadConfiguration()
    }

    // ==================== 公开 API ====================

    /**
     * 配置 WebDAV 提供者
     */
    fun configureWebDAV(serverUrl: String, username: String, password: String) {
        val provider = CloudProvider.WebDAV(serverUrl, username, password)
        _currentProvider.value = provider
        saveProviderConfig(provider)
    }

    /**
     * 配置 Google Drive 提供者
     */
    fun configureGoogleDrive(accessToken: String, folderId: String = "root") {
        val provider = CloudProvider.GoogleDrive(accessToken, folderId)
        _currentProvider.value = provider
        saveProviderConfig(provider)
    }

    /**
     * 断开云端连接，清除配置
     */
    fun disconnect() {
        _currentProvider.value = null
        _isConnected.value = false
        _autoSyncEnabled.value = false
        prefs.edit().clear().apply()
    }

    /**
     * 测试当前提供者连接
     */
    suspend fun testConnection(): Boolean {
        val provider = _currentProvider.value ?: return false
        val result = provider.validateConnection()
        _isConnected.value = result
        if (result) {
            saveProviderConfig(provider)
        }
        return result
    }

    /**
     * 设置自动同步开关
     */
    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    /**
     * 触发手动同步
     */
    fun syncNow() {
        syncScope.launch {
            performSync()
        }
    }

    /**
     * 预设变更时触发自动同步（带防抖 3 秒）
     * 由 PresetRepository 等调用方在写入后调用
     */
    fun notifyPresetChanged() {
        if (!_autoSyncEnabled.value) return
        if (_currentProvider.value == null) return

        debounceJob?.cancel()
        debounceJob = syncScope.launch {
            delay(DEBOUNCE_MS)
            performSync()
        }
    }

    // ==================== 同步核心逻辑 ====================

    private suspend fun performSync() {
        val provider = _currentProvider.value
        if (provider == null) {
            _syncStatus.value = SyncStatus.Error("未配置云端服务")
            return
        }

        _syncStatus.value = SyncStatus.Syncing

        val result = retryWithBackoff(maxRetries = 3) {
            when (provider) {
                is CloudProvider.WebDAV -> syncWithWebDAV(provider)
                is CloudProvider.GoogleDrive -> syncWithGoogleDrive(provider)
            }
        }

        when (result) {
            is Result.Success -> {
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
                _syncStatus.value = SyncStatus.Success(now)
                _isConnected.value = true
            }
            is Result.Failure -> {
                _syncStatus.value = SyncStatus.Error(result.message)
                _isConnected.value = false
            }
        }
    }

    /**
     * WebDAV 同步流程
     *
     * 1. 确保远程目录结构存在
     * 2. 上传本地预设数据（加密后）
     * 3. 上传本地设置数据（加密后）
     * 4. 下载远端数据，合并冲突（last-write-wins）
     */
    private suspend fun syncWithWebDAV(provider: CloudProvider.WebDAV) {
        val client = WebDAVClient(provider.serverUrl, provider.username, provider.password)

        // 确保远程目录存在
        client.ensureDirectory(REMOTE_DIR_PRESETS)
        client.ensureDirectory(REMOTE_DIR_SETTINGS)

        // 上传本地预设
        val localPresetsData = collectLocalPresets()
        if (localPresetsData.isNotEmpty()) {
            val encryptedPresets = encryptData(localPresetsData)
            client.put("$REMOTE_DIR_PRESETS/$FILE_PRESETS", encryptedPresets)
        }

        // 上传本地设置
        val localSettingsData = collectLocalSettings()
        val encryptedSettings = encryptData(localSettingsData)
        client.put("$REMOTE_DIR_SETTINGS/$FILE_SETTINGS", encryptedSettings)

        // 上传版本信息
        syncVersion = System.currentTimeMillis()
        val versionData = json.encodeToString(SyncVersion(syncVersion, DEVICE_ID))
        client.put(
            "$REMOTE_DIR_SETTINGS/$FILE_VERSION",
            encryptData(versionData.toByteArray(Charsets.UTF_8))
        )

        // 下载远端数据并合并
        downloadAndMerge(client)
    }

    /**
     * Google Drive 同步流程
     *
     * 使用 GoogleDriveClient 调用 Google Drive REST API v3
     */
    private suspend fun syncWithGoogleDrive(provider: CloudProvider.GoogleDrive) {
        val client = GoogleDriveClient(provider.accessToken, provider.folderId)

        // 上传预设数据
        val localPresetsData = collectLocalPresets()
        if (localPresetsData.isNotEmpty()) {
            val encryptedPresets = encryptData(localPresetsData)
            client.uploadFile("$REMOTE_DIR_PRESETS/$FILE_PRESETS", encryptedPresets)
        }

        // 上传设置数据
        val localSettingsData = collectLocalSettings()
        val encryptedSettings = encryptData(localSettingsData)
        client.uploadFile("$REMOTE_DIR_SETTINGS/$FILE_SETTINGS", encryptedSettings)

        // 上传版本信息
        syncVersion = System.currentTimeMillis()
        val versionData = json.encodeToString(SyncVersion(syncVersion, DEVICE_ID))
        client.uploadFile(
            "$REMOTE_DIR_SETTINGS/$FILE_VERSION",
            encryptData(versionData.toByteArray(Charsets.UTF_8))
        )

        // 下载远端数据并合并
        downloadAndMergeGDrive(provider)
    }

    // ==================== Google Drive HTTP 操作 ====================

    /**
     * 通过 Google Drive REST API v3 上传文件
     * 使用 multipart upload（简单上传模式）
     */
    private suspend fun gdriveUpload(
        provider: CloudProvider.GoogleDrive,
        fileName: String,
        data: ByteArray
    ) = withContext(Dispatchers.IO) {
        // 先查找同名文件是否已存在
        val existingFileId = gdriveFindFile(provider, fileName)

        if (existingFileId != null) {
            // 更新已有文件: PATCH /upload/drive/v3/files/{fileId}
            val url = URL("https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("Authorization", "Bearer ${provider.accessToken}")
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            conn.outputStream.use { it.write(data) }
            val code = conn.responseCode
            conn.disconnect()

            if (code !in 200..299) {
                throw CloudSyncException("Google Drive 上传失败: HTTP $code")
            }
        } else {
            // 创建新文件: POST /upload/drive/v3/files
            val metadata = """{"name":"$fileName","parents":["${provider.folderId}"]}"""
            val boundary = "omaster_boundary_${System.currentTimeMillis()}"

            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer ${provider.accessToken}")
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append("$metadata\r\n")
                append("--$boundary\r\n")
                append("Content-Type: application/octet-stream\r\n\r\n")
            }.toByteArray(Charsets.UTF_8)

            conn.outputStream.use { os ->
                os.write(body)
                os.write(data)
                os.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code !in 200..299) {
                throw CloudSyncException("Google Drive 上传失败: HTTP $code")
            }
        }
    }

    /**
     * 通过 Google Drive REST API v3 查找文件
     */
    private suspend fun gdriveFindFile(
        provider: CloudProvider.GoogleDrive,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        val query = "name='$fileName' and '${provider.folderId}' in parents and trashed=false"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = URL("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name)")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer ${provider.accessToken}")
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000

        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            return@withContext null
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        // 简易 JSON 解析提取 fileId
        val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
        return@withContext idRegex.find(response)?.groupValues?.get(1)
    }

    /**
     * 通过 Google Drive REST API v3 下载文件
     */
    private suspend fun gdriveDownload(
        provider: CloudProvider.GoogleDrive,
        fileName: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        val fileId = gdriveFindFile(provider, fileName) ?: return@withContext null

        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer ${provider.accessToken}")
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000

        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            return@withContext null
        }

        val bytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        bytes
    }

    // ==================== 数据收集与合并 ====================

    /**
     * 收集本地预设数据（从 PresetRepository 的 JSON 文件读取）
     */
    private fun collectLocalPresets(): ByteArray {
        return try {
            val presetsDir = context.filesDir.resolve("presets")
            if (!presetsDir.exists()) return ByteArray(0)

            val allJson = mutableMapOf<String, String>()
            presetsDir.listFiles()?.forEach { file ->
                if (file.extension == "json") {
                    allJson[file.name] = file.readText()
                }
            }
            json.encodeToString(allJson).toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "收集本地预设数据失败", e)
            ByteArray(0)
        }
    }

    /**
     * 收集本地设置数据
     */
    private fun collectLocalSettings(): ByteArray {
        return try {
            val settingsManager = com.silas.omaster.data.local.SettingsManager.getInstance(context)
            val settings = mapOf(
                "theme" to settingsManager.currentTheme.id,
                "darkMode" to settingsManager.darkMode.name,
                "vibration" to settingsManager.isVibrationEnabled.toString(),
                "floatingWindowOpacity" to settingsManager.floatingWindowOpacity.toString(),
                "defaultStartTab" to settingsManager.defaultStartTab.toString()
            )
            json.encodeToString(settings).toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "收集本地设置数据失败", e)
            ByteArray(0)
        }
    }

    /**
     * 从 WebDAV 下载远端数据并合并（last-write-wins）
     */
    private suspend fun downloadAndMerge(client: WebDAVClient) {
        // 下载远端版本信息
        val remoteVersionBytes = client.get("$REMOTE_DIR_SETTINGS/$FILE_VERSION").getOrNull()
        val remoteVersion = remoteVersionBytes?.let {
            val decrypted = decryptData(it)
            if (decrypted != null) {
                try {
                    json.decodeFromString<SyncVersion>(String(decrypted, Charsets.UTF_8))
                } catch (_: Exception) {
                    null
                }
            } else null
        }

        // last-write-wins：如果远端版本更新，下载并覆盖本地
        if (remoteVersion != null && remoteVersion.timestamp > syncVersion && remoteVersion.deviceId != DEVICE_ID) {
            Log.i(TAG, "远端版本更新 (v${remoteVersion.timestamp}), 执行下载合并")

            // 下载预设
            val remotePresets = client.get("$REMOTE_DIR_PRESETS/$FILE_PRESETS").getOrNull()
            if (remotePresets != null) {
                applyRemotePresets(remotePresets)
            }

            // 下载设置
            val remoteSettings = client.get("$REMOTE_DIR_SETTINGS/$FILE_SETTINGS").getOrNull()
            if (remoteSettings != null) {
                applyRemoteSettings(remoteSettings)
            }

            syncVersion = remoteVersion.timestamp
        }
    }

    /**
     * 从 Google Drive 下载远端数据并合并
     */
    private suspend fun downloadAndMergeGDrive(provider: CloudProvider.GoogleDrive) {
        val client = GoogleDriveClient(provider.accessToken, provider.folderId)

        // 下载远端版本信息
        val remoteVersionBytes = client.downloadFile("$REMOTE_DIR_SETTINGS/$FILE_VERSION").getOrNull()
        val remoteVersion = remoteVersionBytes?.let {
            val decrypted = decryptData(it)
            if (decrypted != null) {
                try {
                    json.decodeFromString<SyncVersion>(String(decrypted, Charsets.UTF_8))
                } catch (_: Exception) {
                    null
                }
            } else null
        }

        if (remoteVersion != null && remoteVersion.timestamp > syncVersion && remoteVersion.deviceId != DEVICE_ID) {
            Log.i(TAG, "远端版本更新 (v${remoteVersion.timestamp}), 执行下载合并")

            val remotePresets = client.downloadFile("$REMOTE_DIR_PRESETS/$FILE_PRESETS").getOrNull()
            if (remotePresets != null) {
                applyRemotePresets(remotePresets)
            }

            val remoteSettings = client.downloadFile("$REMOTE_DIR_SETTINGS/$FILE_SETTINGS").getOrNull()
            if (remoteSettings != null) {
                applyRemoteSettings(remoteSettings)
            }

            syncVersion = remoteVersion.timestamp
        }
    }

    /**
     * 将远端预设数据应用到本地
     */
    private fun applyRemotePresets(encryptedData: ByteArray) {
        try {
            val decrypted = decryptData(encryptedData) ?: return
            @Suppress("UNCHECKED_CAST")
            val presetsMap = json.decodeFromString<Map<String, String>>(
                String(decrypted, Charsets.UTF_8)
            )
            val presetsDir = context.filesDir.resolve("presets")
            if (!presetsDir.exists()) presetsDir.mkdirs()

            presetsMap.forEach { (fileName, content) ->
                presetsDir.resolve(fileName).writeText(content)
            }
            Log.i(TAG, "已应用远端预设: ${presetsMap.size} 个文件")
        } catch (e: Exception) {
            Log.w(TAG, "应用远端预设失败", e)
        }
    }

    /**
     * 将远端设置数据应用到本地
     */
    private fun applyRemoteSettings(encryptedData: ByteArray) {
        try {
            val decrypted = decryptData(encryptedData) ?: return
            @Suppress("UNCHECKED_CAST")
            val settings = json.decodeFromString<Map<String, String>>(
                String(decrypted, Charsets.UTF_8)
            )
            val settingsManager = com.silas.omaster.data.local.SettingsManager.getInstance(context)
            settings["theme"]?.let { settingsManager.currentTheme = com.silas.omaster.ui.theme.BrandTheme.fromId(it) }
            settings["darkMode"]?.let {
                settingsManager.darkMode = try {
                    com.silas.omaster.data.local.DarkMode.valueOf(it)
                } catch (_: Exception) {
                    com.silas.omaster.data.local.DarkMode.DARK
                }
            }
            settings["vibration"]?.toBooleanOrNull()?.let { settingsManager.isVibrationEnabled = it }
            settings["floatingWindowOpacity"]?.toIntOrNull()?.let { settingsManager.floatingWindowOpacity = it }
            settings["defaultStartTab"]?.toIntOrNull()?.let { settingsManager.defaultStartTab = it }
            Log.i(TAG, "已应用远端设置")
        } catch (e: Exception) {
            Log.w(TAG, "应用远端设置失败", e)
        }
    }

    // ==================== 加密 / 解密 ====================

    private fun encryptData(data: ByteArray): ByteArray {
        val plainText = String(data, Charsets.UTF_8)
        val encrypted = SecurityCrypto.encrypt(plainText)
        return (encrypted ?: plainText).toByteArray(Charsets.UTF_8)
    }

    private fun decryptData(encryptedData: ByteArray): ByteArray? {
        val encryptedText = String(encryptedData, Charsets.UTF_8)
        val decrypted = SecurityCrypto.decrypt(encryptedText)
        return decrypted?.toByteArray(Charsets.UTF_8)
    }

    // ==================== 重试逻辑 ====================

    /**
     * 指数退避重试
     *
     * @param maxRetries 最大重试次数
     * @param block      需要重试的挂起函数
     */
    private suspend fun retryWithBackoff(maxRetries: Int, block: suspend () -> Unit): Result {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                block()
                return Result.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "同步失败 (第${attempt + 1}次)", e)
                if (attempt < maxRetries - 1) {
                    val delayMs = (INITIAL_RETRY_DELAY_MS * (1L shl attempt)).coerceAtMost(MAX_RETRY_DELAY_MS)
                    delay(delayMs)
                }
            }
        }
        return Result.Failure(lastException?.message ?: "同步失败")
    }

    // ==================== 配置持久化 ====================

    private fun saveProviderConfig(provider: CloudProvider) {
        prefs.edit().apply {
            when (provider) {
                is CloudProvider.WebDAV -> {
                    putString(KEY_PROVIDER_TYPE, CloudProvider.WebDAV.TYPE_WEBDAV)
                    putString(KEY_SERVER_URL, provider.serverUrl)
                    // 安全存储凭证
                    SecurityCrypto.encrypt(provider.username)?.let { putString(KEY_USERNAME, it) }
                    SecurityCrypto.encrypt(provider.password)?.let { putString(KEY_PASSWORD, it) }
                }
                is CloudProvider.GoogleDrive -> {
                    putString(KEY_PROVIDER_TYPE, CloudProvider.GoogleDrive.TYPE_GDRIVE)
                    SecurityCrypto.encrypt(provider.accessToken)?.let { putString(KEY_ACCESS_TOKEN, it) }
                    putString(KEY_FOLDER_ID, provider.folderId)
                }
            }
            apply()
        }
    }

    private fun loadConfiguration() {
        val providerType = prefs.getString(KEY_PROVIDER_TYPE, null)
        when (providerType) {
            CloudProvider.WebDAV.TYPE_WEBDAV -> {
                val serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: ""
                val username = SecurityCrypto.decrypt(prefs.getString(KEY_USERNAME, "") ?: "") ?: ""
                val password = SecurityCrypto.decrypt(prefs.getString(KEY_PASSWORD, "") ?: "") ?: ""
                if (serverUrl.isNotEmpty() && username.isNotEmpty()) {
                    _currentProvider.value = CloudProvider.WebDAV(serverUrl, username, password)
                    _isConnected.value = true
                }
            }
            CloudProvider.GoogleDrive.TYPE_GDRIVE -> {
                val accessToken = SecurityCrypto.decrypt(prefs.getString(KEY_ACCESS_TOKEN, "") ?: "") ?: ""
                val folderId = prefs.getString(KEY_FOLDER_ID, "root") ?: "root"
                if (accessToken.isNotEmpty()) {
                    _currentProvider.value = CloudProvider.GoogleDrive(accessToken, folderId)
                    _isConnected.value = true
                }
            }
        }
        _autoSyncEnabled.value = prefs.getBoolean(KEY_AUTO_SYNC, false)
        _lastSyncTime.value = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        syncVersion = _lastSyncTime.value
    }

    // ==================== 内部类型 ====================

    private sealed class Result {
        data object Success : Result()
        data class Failure(val message: String) : Result()
    }

    @Serializable
    private data class SyncVersion(
        val timestamp: Long,
        val deviceId: String
    )

    companion object {
        private const val TAG = "CloudSyncManager"
        private const val PREFS_NAME = "cloud_sync_prefs"
        private const val KEY_PROVIDER_TYPE = "provider_type"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_FOLDER_ID = "folder_id"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"

        private const val REMOTE_DIR_PRESETS = "omaster/presets"
        private const val REMOTE_DIR_SETTINGS = "omaster/settings"
        private const val FILE_PRESETS = "presets.dat"
        private const val FILE_SETTINGS = "settings.dat"
        private const val FILE_VERSION = "version.dat"

        private const val DEBOUNCE_MS = 3000L
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30_000L

        private val DEVICE_ID = android.provider.Settings.Secure.ANDROID_ID

        @Volatile
        private var instance: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager {
            return instance ?: synchronized(this) {
                instance ?: CloudSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 云端同步异常
 */
class CloudSyncException(message: String) : Exception(message)
