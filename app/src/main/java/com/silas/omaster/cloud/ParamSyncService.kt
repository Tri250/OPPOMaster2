package com.silas.omaster.cloud

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silas.omaster.param.ParamAdjustmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 参数云同步服务
 * 与 CloudSyncManager 集成，实现跨设备参数同步
 */
class ParamSyncService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val paramAdjustmentManager = ParamAdjustmentManager.getInstance(context)
    private val cloudSyncManager = CloudSyncManager.getInstance(context)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "omaster_param_sync"
        private const val KEY_SYNCED_PARAMS = "synced_params"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SYNC_HISTORY = "sync_history"

        @Volatile
        private var instance: ParamSyncService? = null

        fun getInstance(context: Context): ParamSyncService {
            return instance ?: synchronized(this) {
                instance ?: ParamSyncService(context.applicationContext).also { instance = it }
            }
        }
    }

    // 同步状态
    private val _syncState = MutableStateFlow<ParamSyncState>(ParamSyncState.Idle)
    val syncState: StateFlow<ParamSyncState> = _syncState.asStateFlow()

    // 当前设备ID
    private val deviceId: String by lazy {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }

    // 已同步的参数快照
    private val _syncedParams = MutableStateFlow<SyncedParamSnapshot?>(null)
    val syncedParams: StateFlow<SyncedParamSnapshot?> = _syncedParams.asStateFlow()

    // 同步历史
    private val _syncHistory = MutableStateFlow<List<SyncHistoryItem>>(loadSyncHistory())
    val syncHistory: StateFlow<List<SyncHistoryItem>> = _syncHistory.asStateFlow()

    /**
     * 上传当前参数到云端
     */
    suspend fun uploadParams(
        presetName: String? = null,
        tags: List<String> = emptyList()
    ): ParamSyncResult = withContext(Dispatchers.IO) {
        _syncState.value = ParamSyncState.Uploading

        try {
            val params = paramAdjustmentManager.getAllParamValues()
            val snapshot = SyncedParamSnapshot(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                params = params,
                presetName = presetName ?: "未命名预设",
                tags = tags,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // 模拟上传到云端（实际应调用云存储API）
            val success = uploadToCloud(snapshot)

            if (success) {
                _syncedParams.value = snapshot
                addToSyncHistory(SyncHistoryItem(
                    snapshotId = snapshot.id,
                    action = SyncAction.UPLOAD,
                    timestamp = System.currentTimeMillis(),
                    deviceId = deviceId,
                    success = true
                ))
                _syncState.value = ParamSyncState.Synced(snapshot)
                ParamSyncResult.Success("参数已上传到云端")
            } else {
                _syncState.value = ParamSyncState.Error("上传失败")
                ParamSyncResult.Error("上传失败")
            }
        } catch (e: Exception) {
            _syncState.value = ParamSyncState.Error(e.message ?: "Unknown error")
            ParamSyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 从云端下载参数
     */
    suspend fun downloadParams(snapshotId: String? = null): ParamSyncResult = withContext(Dispatchers.IO) {
        _syncState.value = ParamSyncState.Downloading

        try {
            // 模拟从云端下载（实际应调用云存储API）
            val snapshot = downloadFromCloud(snapshotId)

            if (snapshot != null) {
                // 应用下载的参数
                snapshot.params.forEach { (paramName, value) ->
                    paramAdjustmentManager.adjustParam(paramName, value.toFloat())
                }

                _syncedParams.value = snapshot
                addToSyncHistory(SyncHistoryItem(
                    snapshotId = snapshot.id,
                    action = SyncAction.DOWNLOAD,
                    timestamp = System.currentTimeMillis(),
                    deviceId = snapshot.deviceId,
                    success = true
                ))
                _syncState.value = ParamSyncState.Synced(snapshot)
                ParamSyncResult.Success("参数已从云端同步")
            } else {
                _syncState.value = ParamSyncState.Error("未找到同步数据")
                ParamSyncResult.Error("未找到同步数据")
            }
        } catch (e: Exception) {
            _syncState.value = ParamSyncState.Error(e.message ?: "Unknown error")
            ParamSyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 获取云端参数列表
     */
    suspend fun getCloudParamSnapshots(): List<SyncedParamSnapshot> = withContext(Dispatchers.IO) {
        // 模拟获取云端列表（实际应调用云存储API）
        getSnapshotsFromCloud()
    }

    /**
     * 删除云端参数快照
     */
    suspend fun deleteSnapshot(snapshotId: String): ParamSyncResult = withContext(Dispatchers.IO) {
        try {
            val success = deleteFromCloud(snapshotId)
            if (success) {
                addToSyncHistory(SyncHistoryItem(
                    snapshotId = snapshotId,
                    action = SyncAction.DELETE,
                    timestamp = System.currentTimeMillis(),
                    deviceId = deviceId,
                    success = true
                ))
                ParamSyncResult.Success("已删除")
            } else {
                ParamSyncResult.Error("删除失败")
            }
        } catch (e: Exception) {
            ParamSyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 同步参数（双向同步）
     * 比较本地和云端的时间戳，自动选择最新版本
     */
    suspend fun syncParams(): ParamSyncResult = withContext(Dispatchers.IO) {
        _syncState.value = ParamSyncState.Syncing

        try {
            val localParams = paramAdjustmentManager.getAllParamValues()
            val localTimestamp = System.currentTimeMillis()

            // 获取云端最新快照
            val cloudSnapshots = getSnapshotsFromCloud()
            val latestCloud = cloudSnapshots.maxByOrNull { it.updatedAt }

            if (latestCloud == null) {
                // 云端无数据，上传本地
                return@withContext uploadParams()
            }

            // 比较时间戳
            if (localTimestamp > latestCloud.updatedAt) {
                // 本地更新，上传
                uploadParams()
            } else {
                // 云端更新，下载
                downloadParams(latestCloud.id)
            }
        } catch (e: Exception) {
            _syncState.value = ParamSyncState.Error(e.message ?: "Unknown error")
            ParamSyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 导出参数为分享链接
     */
    fun exportAsShareLink(): String {
        val params = paramAdjustmentManager.getAllParamValues()
        val snapshot = SyncedParamSnapshot(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            params = params,
            presetName = "分享预设",
            tags = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return gson.toJson(snapshot)
    }

    /**
     * 从分享链接导入参数
     */
    fun importFromShareLink(json: String): ParamSyncResult {
        return try {
            val type = object : TypeToken<SyncedParamSnapshot>() {}.type
            val snapshot = gson.fromJson<SyncedParamSnapshot>(json, type)

            snapshot.params.forEach { (paramName, value) ->
                paramAdjustmentManager.adjustParam(paramName, value.toFloat())
            }

            _syncedParams.value = snapshot
            ParamSyncResult.Success("导入成功")
        } catch (e: Exception) {
            ParamSyncResult.Error("导入失败: ${e.message}")
        }
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = deviceId,
            deviceName = android.os.Build.MODEL,
            lastSyncTime = _syncHistory.value.firstOrNull()?.timestamp ?: 0L
        )
    }

    // 私有方法

    private fun uploadToCloud(snapshot: SyncedParamSnapshot): Boolean {
        // 模拟上传逻辑
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_SYNCED_PARAMS, null)
        val list = if (existing != null) {
            val type = object : TypeToken<MutableList<SyncedParamSnapshot>>() {}.type
            gson.fromJson<MutableList<SyncedParamSnapshot>>(existing, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }
        list.add(snapshot)
        prefs.edit().putString(KEY_SYNCED_PARAMS, gson.toJson(list)).apply()
        return true
    }

    private fun downloadFromCloud(snapshotId: String?): SyncedParamSnapshot? {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_SYNCED_PARAMS, null) ?: return null
        val type = object : TypeToken<List<SyncedParamSnapshot>>() {}.type
        val list = gson.fromJson<List<SyncedParamSnapshot>>(existing, type) ?: return null

        return if (snapshotId != null) {
            list.find { it.id == snapshotId }
        } else {
            list.maxByOrNull { it.updatedAt }
        }
    }

    private fun getSnapshotsFromCloud(): List<SyncedParamSnapshot> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_SYNCED_PARAMS, null) ?: return emptyList()
        val type = object : TypeToken<List<SyncedParamSnapshot>>() {}.type
        return gson.fromJson<List<SyncedParamSnapshot>>(existing, type) ?: emptyList()
    }

    private fun deleteFromCloud(snapshotId: String): Boolean {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_SYNCED_PARAMS, null) ?: return false
        val type = object : TypeToken<MutableList<SyncedParamSnapshot>>() {}.type
        val list = gson.fromJson<MutableList<SyncedParamSnapshot>>(existing, type) ?: return false
        val removed = list.removeAll { it.id == snapshotId }
        if (removed) {
            prefs.edit().putString(KEY_SYNCED_PARAMS, gson.toJson(list)).apply()
        }
        return removed
    }

    private fun loadSyncHistory(): List<SyncHistoryItem> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SYNC_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<SyncHistoryItem>>() {}.type
        return gson.fromJson<List<SyncHistoryItem>>(json, type) ?: emptyList()
    }

    private fun addToSyncHistory(item: SyncHistoryItem) {
        val history = _syncHistory.value.toMutableList()
        history.add(0, item)
        // 只保留最近100条记录
        if (history.size > 100) {
            history.removeAt(history.size - 1)
        }
        _syncHistory.value = history

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SYNC_HISTORY, gson.toJson(history)).apply()
    }
}

/**
 * 参数同步状态
 */
sealed class ParamSyncState {
    object Idle : ParamSyncState()
    object Uploading : ParamSyncState()
    object Downloading : ParamSyncState()
    object Syncing : ParamSyncState()
    data class Synced(val snapshot: SyncedParamSnapshot) : ParamSyncState()
    data class Error(val message: String) : ParamSyncState()
}

/**
 * 参数同步结果
 */
sealed class ParamSyncResult {
    data class Success(val message: String) : ParamSyncResult()
    data class Error(val message: String) : ParamSyncResult()
}

/**
 * 同步的参数快照
 */
data class SyncedParamSnapshot(
    val id: String,
    val deviceId: String,
    val params: Map<String, Int>,
    val presetName: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 同步历史项
 */
data class SyncHistoryItem(
    val snapshotId: String,
    val action: SyncAction,
    val timestamp: Long,
    val deviceId: String,
    val success: Boolean
)

/**
 * 同步动作
 */
enum class SyncAction {
    UPLOAD, DOWNLOAD, DELETE, SYNC
}

/**
 * 设备信息
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val lastSyncTime: Long
)
