package com.silas.omaster.cloud

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 云端预设仓库
 * 实现基于CDN的预设同步管理
 * 针对OPPO Find摄影用户的真实云同步需求
 */
class CloudPresetRepository private constructor(context: Context) {
    
    private val appContext = context.applicationContext
    private val settingsManager = SettingsManager.getInstance(context)
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    // 状态流
    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()
    
    private val _cloudPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val cloudPresets: StateFlow<List<MasterPreset>> = _cloudPresets.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    // 品牌预设URL配置
    private val brandPresetUrls = mapOf(
        "oppo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
        "realme" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
        "vivo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
        "honor" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json"
    )
    
    init {
        // 从本地缓存加载
        loadFromCache()
    }
    
    /**
     * 执行云同步
     * 从CDN拉取最新预设数据
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        if (!settingsManager.isCloudSyncEnabled) {
            return@withContext SyncResult.Disabled
        }
        
        _syncState.value = CloudSyncState.Syncing
        
        try {
            var totalNew = 0
            var totalUpdated = 0
            val allPresets = mutableListOf<MasterPreset>()
            
            // 同步每个品牌的预设
            brandPresetUrls.forEach { (brand, url) ->
                try {
                    val result = syncBrandPresets(brand, url)
                    totalNew += result.newCount
                    totalUpdated += result.updatedCount
                    allPresets.addAll(result.presets)
                } catch (e: Exception) {
                    // 单个品牌同步失败不影响其他
                }
            }
            
            // 保存到缓存
            saveToCache(allPresets)
            
            // 更新状态
            val currentTime = System.currentTimeMillis()
            settingsManager.lastSyncTime = currentTime
            _lastSyncTime.value = currentTime
            _syncState.value = CloudSyncState.Success(totalNew, totalUpdated)
            
            SyncResult.Success(totalNew, totalUpdated)
            
        } catch (e: Exception) {
            _syncState.value = CloudSyncState.Error(e.message ?: "同步失败")
            SyncResult.Error(e.message ?: "同步失败")
        }
    }
    
    /**
     * 同步单个品牌的预设
     */
    private suspend fun syncBrandPresets(brand: String, url: String): BrandSyncResult = 
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            
            val jsonString = response.body?.string() ?: throw IOException("Empty response")
            
            // 解析JSON
            val presetList = gson.fromJson(jsonString, com.silas.omaster.model.PresetList::class.java)
            
            // 获取现有预设用于比较
            val existingPresets = _cloudPresets.value.filter { it.brand == brand }
            val existingMap = existingPresets.associateBy { it.name }
            
            var newCount = 0
            var updatedCount = 0
            val brandPresets = presetList.presets.map { preset ->
                // 添加品牌标识
                val presetWithBrand = preset.copy(brand = brand)
                
                // 检查是否已存在
                val existing = existingMap[preset.name]
                when {
                    existing == null -> newCount++
                    existing.build < preset.build -> updatedCount++
                }
                
                presetWithBrand
            }
            
            BrandSyncResult(newCount, updatedCount, brandPresets)
        }
    
    /**
     * 按品牌获取预设
     */
    fun getPresetsByBrand(brand: String): List<MasterPreset> {
        return _cloudPresets.value.filter { it.brand == brand }
    }
    
    /**
     * 获取所有云端预设
     */
    fun getAllCloudPresets(): List<MasterPreset> {
        return _cloudPresets.value
    }
    
    /**
     * 搜索预设
     */
    fun searchPresets(query: String): List<MasterPreset> {
        val lowerQuery = query.lowercase()
        return _cloudPresets.value.filter { preset ->
            preset.name.lowercase().contains(lowerQuery) ||
            preset.author.lowercase().contains(lowerQuery) ||
            preset.tags?.any { it.lowercase().contains(lowerQuery) } == true
        }
    }
    
    /**
     * 按标签筛选
     */
    fun filterByTag(tag: String): List<MasterPreset> {
        return _cloudPresets.value.filter { preset ->
            preset.tags?.contains(tag) == true
        }
    }
    
    /**
     * 获取最新预设（按创建时间）
     */
    fun getLatestPresets(limit: Int = 10): List<MasterPreset> {
        return _cloudPresets.value
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
    
    /**
     * 获取热门预设（模拟下载量排序）
     */
    fun getPopularPresets(limit: Int = 10): List<MasterPreset> {
        // 实际应用中应该根据真实下载量排序
        return _cloudPresets.value
            .shuffled() // 临时随机排序
            .take(limit)
    }
    
    /**
     * 检查是否需要同步
     */
    fun shouldSync(): Boolean {
        if (!settingsManager.isCloudSyncEnabled) return false
        
        val lastSync = settingsManager.lastSyncTime
        val currentTime = System.currentTimeMillis()
        
        // 24小时自动同步一次
        return currentTime - lastSync > 24 * 60 * 60 * 1000
    }
    
    /**
     * 从缓存加载
     */
    private fun loadFromCache() {
        val json = prefs.getString(KEY_CLOUD_PRESETS, null)
        val presets = if (json != null) {
            try {
                val type = object : TypeToken<List<MasterPreset>>() {}.type
                gson.fromJson<List<MasterPreset>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        _cloudPresets.value = presets
        _lastSyncTime.value = settingsManager.lastSyncTime
    }
    
    /**
     * 保存到缓存
     */
    private fun saveToCache(presets: List<MasterPreset>) {
        val json = gson.toJson(presets)
        prefs.edit().putString(KEY_CLOUD_PRESETS, json).apply()
        _cloudPresets.value = presets
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        prefs.edit().remove(KEY_CLOUD_PRESETS).apply()
        _cloudPresets.value = emptyList()
    }
    
    companion object {
        private const val PREFS_NAME = "cloud_preset_repository"
        private const val KEY_CLOUD_PRESETS = "cloud_presets"
        
        @Volatile
        private var instance: CloudPresetRepository? = null
        
        fun getInstance(context: Context): CloudPresetRepository {
            return instance ?: synchronized(this) {
                instance ?: CloudPresetRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * 云同步状态
 */
sealed class CloudSyncState {
    object Idle : CloudSyncState()
    object Syncing : CloudSyncState()
    data class Success(val newCount: Int, val updatedCount: Int) : CloudSyncState()
    data class Error(val message: String) : CloudSyncState()
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
 * 品牌同步结果
 */
private data class BrandSyncResult(
    val newCount: Int,
    val updatedCount: Int,
    val presets: List<MasterPreset>
)