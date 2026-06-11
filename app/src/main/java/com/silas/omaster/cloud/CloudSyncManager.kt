package com.silas.omaster.cloud

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.ReleaseLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.UUID

/**
 * 云同步管理器
 * 负责从 CDN 同步预设数据到本地 SharedPreferences
 */
class CloudSyncManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val appContext = context.applicationContext

    // 使用SharedPreferences存储云端预设
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(settingsManager.lastSyncTime)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // 内存缓存
    private val _cloudPresets = MutableStateFlow<List<MasterPreset>>(loadFromCache())
    val cloudPresets: StateFlow<List<MasterPreset>> = _cloudPresets.asStateFlow()

    /**
     * 从缓存加载预设
     */
    private fun loadFromCache(): List<MasterPreset> {
        val json = prefs.getString(KEY_CLOUD_PRESETS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MasterPreset>>() {}.type
            gson.fromJson<List<MasterPreset>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            ReleaseLog.e("CloudSyncManager", "加载云端预设失败", e)
            emptyList()
        }
    }

    /**
     * 保存预设到缓存
     */
    private fun saveToCache(presets: List<MasterPreset>) {
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_CLOUD_PRESETS, json).apply()
        _cloudPresets.value = presets
    }

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
            val allPresets = mutableListOf<MasterPreset>()

            // 同步每个品牌的预设
            settingsManager.cloudPresetUrls.forEach { (brand, url) ->
                try {
                    val result = syncBrandPresets(brand, url)
                    totalNewPresets += result.newCount
                    totalUpdatedPresets += result.updatedCount
                    // 收集所有预设
                    allPresets.addAll(result.presets)
                } catch (e: Exception) {
                    // 单个品牌同步失败不影响其他品牌
                    ReleaseLog.e("CloudSyncManager", "同步品牌 $brand 失败", e)
                }
            }

            // 保存到缓存
            saveToCache(allPresets)

            // 更新同步状态
            val currentTime = System.currentTimeMillis()
            settingsManager.lastSyncTime = currentTime
            _lastSyncTime.value = currentTime
            settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.SYNCED
            _syncState.value = SyncState.Success(totalNewPresets, totalUpdatedPresets)

            ReleaseLog.d("CloudSyncManager", "同步完成: 新增$totalNewPresets, 更新$totalUpdatedPresets")

            SyncResult.Success(totalNewPresets, totalUpdatedPresets)
        } catch (e: Exception) {
            ReleaseLog.e("CloudSyncManager", "同步失败", e)
            settingsManager.cloudSyncStatus = com.silas.omaster.data.local.CloudSyncStatus.ERROR
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * 同步单个品牌的预设
     */
    private suspend fun syncBrandPresets(brand: String, urlString: String): BrandSyncResult = withContext(Dispatchers.IO) {
        // 验证URL协议
        if (!urlString.startsWith("https://")) {
            throw SecurityException("仅支持 HTTPS 协议: $urlString")
        }
        
        val url = URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "OMaster/${com.silas.omaster.BuildConfig.VERSION_NAME}")
            connectTimeout = 10000
            readTimeout = 15000
            instanceFollowRedirects = false  // 不自动跟随重定向，避免跳转到HTTP
        }
        
        // 验证响应码
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw java.io.IOException("HTTP $responseCode")
        }
        
        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        
        val jsonObject = JSONObject(jsonString)

        val version = jsonObject.optInt("version", 1)
        val build = jsonObject.optInt("build", 1)
        val presetsArray = jsonObject.getJSONArray("presets")

        var newCount = 0
        var updatedCount = 0
        val brandPresets = mutableListOf<MasterPreset>()

        // 获取现有预设用于比较
        val existingPresets = _cloudPresets.value.filter { it.brand == brand }
        val existingMap = existingPresets.associateBy { it.name }

        for (i in 0 until presetsArray.length()) {
            val presetJson = presetsArray.getJSONObject(i)
            val preset = parsePresetJson(presetJson, brand, version, build)

            brandPresets.add(preset)

            // 检查是否已存在
            val existing = existingMap[preset.name]
            if (existing == null) {
                newCount++
            } else if (existing.build < preset.build) {
                updatedCount++
            }
        }

        BrandSyncResult(newCount, updatedCount, brandPresets)
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
     * 获取云端预设列表
     */
    fun getCloudPresets(): List<MasterPreset> {
        return _cloudPresets.value
    }

    /**
     * 按品牌获取预设
     */
    fun getPresetsByBrand(brand: String): List<MasterPreset> {
        return _cloudPresets.value.filter { it.brand == brand }
    }

    /**
     * 获取云端预设数量
     */
    fun getCloudPresetCount(): Int {
        return _cloudPresets.value.size
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

    /**
     * 清除云端缓存
     */
    fun clearCache() {
        prefs.edit().remove(KEY_CLOUD_PRESETS).apply()
        _cloudPresets.value = emptyList()
    }

    companion object {
        private const val PREFS_NAME = "omaster_cloud_sync"
        private const val KEY_CLOUD_PRESETS = "cloud_presets"

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
    val updatedCount: Int,
    val presets: List<MasterPreset>
)
