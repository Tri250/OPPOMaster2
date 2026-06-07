package com.silas.omaster.cloud

import android.content.Context
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.UUID

/**
 * 云同步管理器
 * 负责从 CDN 同步预设数据
 */
class CloudSyncManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val presetDao = com.silas.omaster.data.local.PresetDatabase.getInstance(context).presetDao()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(settingsManager.lastSyncTime)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    /**
     * 检查是否需要同步
     * 每24小时自动同步一次
     */
    fun shouldSync(): Boolean {
        if (!settingsManager.isCloudSyncEnabled) return false
        val lastSync = settingsManager.lastSyncTime
        val currentTime = System.currentTimeMillis()
        return currentTime - lastSync > 24 * 60 * 60 * 1000 // 24小时
    }

    /**
     * 执行云同步
     * 从 CDN 拉取最新预设数据
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        if (!settingsManager.isCloudSyncEnabled) {
            return@withContext SyncResult.Disabled
        }

        _syncState.value = SyncState.Syncing
        settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.SYNCING

        try {
            var totalNewPresets = 0
            var totalUpdatedPresets = 0

            // 同步每个品牌的预设
            settingsManager.cloudPresetUrls.forEach { (brand, url) ->
                try {
                    val result = syncBrandPresets(brand, url)
                    totalNewPresets += result.newCount
                    totalUpdatedPresets += result.updatedCount
                } catch (e: Exception) {
                    // 单个品牌同步失败不影响其他品牌
                    e.printStackTrace()
                }
            }

            // 更新同步状态
            val currentTime = System.currentTimeMillis()
            settingsManager.lastSyncTime = currentTime
            _lastSyncTime.value = currentTime
            settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.SYNCED
            _syncState.value = SyncState.Success(totalNewPresets, totalUpdatedPresets)

            SyncResult.Success(totalNewPresets, totalUpdatedPresets)
        } catch (e: Exception) {
            settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.ERROR
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 同步单个品牌的预设
     */
    private suspend fun syncBrandPresets(brand: String, urlString: String): BrandSyncResult = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val jsonString = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        val version = jsonObject.optInt("version", 1)
        val build = jsonObject.optInt("build", 1)
        val presetsArray = jsonObject.getJSONArray("presets")

        var newCount = 0
        var updatedCount = 0

        for (i in 0 until presetsArray.length()) {
            val presetJson = presetsArray.getJSONObject(i)
            val preset = parsePresetJson(presetJson, brand, version, build)

            // 检查是否已存在
            val existingPreset = presetDao.getPresetById(preset.id)
            if (existingPreset == null) {
                presetDao.insertPreset(preset)
                newCount++
            } else if (existingPreset.build < preset.build) {
                // 更新已有预设
                presetDao.updatePreset(preset)
                updatedCount++
            }
        }

        BrandSyncResult(newCount, updatedCount)
    }

    /**
     * 解析 JSON 为 MasterPreset
     */
    private fun parsePresetJson(json: JSONObject, brand: String, version: Int, build: Int): MasterPreset {
        val id = json.optString("name", UUID.randomUUID().toString()).hashCode().toString()
        val name = json.getString("name")
        val author = json.optString("author", "@OMaster")
        val coverPath = json.optString("coverPath", "")
        val galleryImages = json.optJSONArray("galleryImages")?.let { array ->
            List(array.length()) { array.getString(it) }
        } ?: emptyList()
        val tags = json.optJSONArray("tags")?.let { array ->
            List(array.length()) { array.getString(it) }
        } ?: emptyList()
        val isNew = json.optBoolean("isNew", false)

        // 解析参数
        val sections = json.optJSONArray("sections")
        val params = mutableMapOf<String, String>()
        val colorGradingParams = mutableMapOf<String, String>()

        sections?.let { array ->
            for (i in 0 until array.length()) {
                val section = array.getJSONObject(i)
                val sectionTitle = section.optString("title", "")
                val items = section.optJSONArray("items")

                items?.let { itemArray ->
                    for (j in 0 until itemArray.length()) {
                        val item = itemArray.getJSONObject(j)
                        val label = item.optString("label", "")
                        val value = item.optString("value", "")

                        // 根据标题分类存储参数
                        when {
                            sectionTitle.contains("专业") || sectionTitle.contains("Pro") -> {
                                params[label] = value
                            }
                            sectionTitle.contains("色彩") || sectionTitle.contains("Color") -> {
                                colorGradingParams[label] = value
                            }
                            else -> {
                                colorGradingParams[label] = value
                            }
                        }
                    }
                }
            }
        }

        // 解析描述
        val description = json.optJSONObject("description")
        val descriptionText = description?.optString("content", "") ?: ""

        return MasterPreset(
            id = id,
            name = name,
            author = author,
            coverPath = coverPath,
            galleryImages = galleryImages,
            tags = tags,
            isNew = isNew,
            params = params,
            colorGradingParams = colorGradingParams,
            description = descriptionText,
            brand = brand,
            version = version,
            build = build,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * 获取云端预设数量
     */
    suspend fun getCloudPresetCount(): Int = withContext(Dispatchers.IO) {
        settingsManager.cloudPresetUrls.size
    }

    /**
     * 获取云端预设URL列表
     */
    fun getCloudPresetUrls(): Map<String, String> {
        return settingsManager.cloudPresetUrls
    }

    /**
     * 切换云同步开关
     */
    fun toggleCloudSync(enabled: Boolean) {
        settingsManager.isCloudSyncEnabled = enabled
        if (!enabled) {
            settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.DISABLED
        }
    }

    companion object {
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
 * 同步状态
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val newCount: Int, val updatedCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * 同步结果
 */
sealed class SyncResult {
    object Disabled : SyncResult()
    data class Success(val newCount: Int, val updatedCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * 单个品牌同步结果
 */
private data class BrandSyncResult(
    val newCount: Int,
    val updatedCount: Int
)
