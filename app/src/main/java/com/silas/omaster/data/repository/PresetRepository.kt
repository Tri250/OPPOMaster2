package com.silas.omaster.data.repository

import android.content.Context
import android.util.Log
import com.silas.omaster.data.local.CustomPresetManager
import com.silas.omaster.data.local.FavoriteManager
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 预设仓库 - 统一管理所有预设数据
 * 整合内置预设（JsonUtil）和自定义预设（CustomPresetManager）
 * 
 * 功能：
 * - PM-001: 预设瀑布流浏览
 * - PM-002: 预设筛选
 * - PM-003: 预设收藏/取消收藏
 * - PM-004: 自定义预设创建
 * - PM-005: 自定义预设编辑
 * - PM-006: 自定义预设删除
 * - PM-008: 预设置顶与NEW标记
 */
class PresetRepository private constructor(private val context: Context) {
    
    private val customPresetManager = CustomPresetManager.getInstance(context)
    private val favoriteManager = FavoriteManager.getInstance(context)
    private val settingsManager = SettingsManager.getInstance(context)
    
    // 内置预设缓存
    private val _builtInPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    private val builtInPresets: StateFlow<List<MasterPreset>> = _builtInPresets.asStateFlow()
    
    // 所有预设（内置 + 自定义）
    private val _allPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    
    init {
        loadBuiltInPresets()
    }
    
    /**
     * 加载内置预设
     */
    private fun loadBuiltInPresets() {
        val presets = JsonUtil.loadPresets(context)
        // 合并收藏状态
        val favorites = favoriteManager.getFavorites()
        _builtInPresets.value = presets.map { preset ->
            preset.copy(isFavorite = favorites.contains(preset.id))
        }
        updateAllPresets()
    }
    
    /**
     * 更新所有预设列表
     */
    private fun updateAllPresets() {
        val builtIn = _builtInPresets.value
        val custom = customPresetManager.getCustomPresets()
        val favorites = favoriteManager.getFavorites()
        
        // 合并内置和自定义预设，并更新收藏状态
        val all = (builtIn + custom).map { preset ->
            preset.copy(isFavorite = favorites.contains(preset.id))
        }
        _allPresets.value = all
        Log.d("PresetRepository", "Updated all presets: ${all.size} (built-in: ${builtIn.size}, custom: ${custom.size})")
    }
    
    /**
     * 获取所有预设 Flow
     * 用于首页"全部"Tab
     */
    fun getAllPresets(): Flow<List<MasterPreset>> {
        return combine(
            _builtInPresets,
            customPresetManager.customPresetsFlow,
            favoriteManager.favoritesFlow
        ) { builtIn, custom, favorites ->
            (builtIn + custom).map { preset ->
                preset.copy(isFavorite = favorites.contains(preset.id))
            }
        }.flowOn(Dispatchers.IO)
    }
    
    /**
     * 获取收藏预设 Flow
     * 用于首页"收藏"Tab
     */
    fun getFavoritePresets(): Flow<List<MasterPreset>> {
        return combine(
            _builtInPresets,
            customPresetManager.customPresetsFlow,
            favoriteManager.favoritesFlow
        ) { builtIn, custom, favorites ->
            (builtIn + custom)
                .filter { favorites.contains(it.id) }
                .map { preset ->
                    preset.copy(isFavorite = true)
                }
        }.flowOn(Dispatchers.IO)
    }
    
    /**
     * 获取自定义预设 Flow
     * 用于首页"我的"Tab
     */
    fun getCustomPresets(): Flow<List<MasterPreset>> {
        return combine(
            customPresetManager.customPresetsFlow,
            favoriteManager.favoritesFlow
        ) { custom, favorites ->
            custom.map { preset ->
                preset.copy(isFavorite = favorites.contains(preset.id))
            }
        }.flowOn(Dispatchers.IO)
    }
    
    /**
     * 根据ID获取预设
     * 用于详情页加载
     */
    suspend fun getPresetById(presetId: String): MasterPreset? = withContext(Dispatchers.IO) {
        // 先从内置预设查找
        val builtIn = _builtInPresets.value.find { it.id == presetId }
        if (builtIn != null) {
            val favorites = favoriteManager.getFavorites()
            return@withContext builtIn.copy(isFavorite = favorites.contains(presetId))
        }
        
        // 再从自定义预设查找
        val custom = customPresetManager.getPresetById(presetId)
        if (custom != null) {
            val favorites = favoriteManager.getFavorites()
            return@withContext custom.copy(isFavorite = favorites.contains(presetId))
        }
        
        return@withContext null
    }
    
    /**
     * PM-003: 切换收藏状态
     * @return 新的收藏状态
     */
    suspend fun toggleFavorite(presetId: String): Boolean = withContext(Dispatchers.IO) {
        val isNowFavorite = favoriteManager.toggleFavorite(presetId)
        
        // 更新内置预设的收藏状态
        val builtIn = _builtInPresets.value
        val index = builtIn.indexOfFirst { it.id == presetId }
        if (index != -1) {
            _builtInPresets.value = builtIn.map { 
                if (it.id == presetId) it.copy(isFavorite = isNowFavorite) else it 
            }
        }
        
        Log.d("PresetRepository", "Toggle favorite: $presetId -> $isNowFavorite")
        isNowFavorite
    }
    
    /**
     * PM-004: 添加自定义预设
     */
    fun addCustomPreset(preset: MasterPreset) {
        customPresetManager.addCustomPreset(preset)
        Log.d("PresetRepository", "Added custom preset: ${preset.name}")
    }
    
    /**
     * PM-005: 更新自定义预设
     */
    fun updateCustomPreset(preset: MasterPreset) {
        customPresetManager.updateCustomPreset(preset)
        Log.d("PresetRepository", "Updated custom preset: ${preset.name}")
    }
    
    /**
     * PM-006: 删除自定义预设
     */
    fun deleteCustomPreset(presetId: String) {
        customPresetManager.deleteCustomPreset(context, presetId)
        // 同时移除收藏
        favoriteManager.removeFavorite(presetId)
        Log.d("PresetRepository", "Deleted custom preset: $presetId")
    }
    
    /**
     * 重新加载默认预设（用于数据迁移后）
     */
    fun reloadDefaultPresets() {
        JsonUtil.invalidateCache()
        loadBuiltInPresets()
        Log.d("PresetRepository", "Reloaded default presets")
    }
    
    /**
     * 检查是否收藏
     */
    fun isFavorite(presetId: String): Boolean {
        return favoriteManager.isFavorite(presetId)
    }
    
    /**
     * 获取预设总数
     */
    fun getTotalCount(): Int {
        return _builtInPresets.value.size + customPresetManager.getCustomPresets().size
    }
    
    /**
     * 获取收藏数
     */
    fun getFavoriteCount(): Int {
        return favoriteManager.getFavorites().size
    }
    
    /**
     * 获取自定义预设数
     */
    fun getCustomCount(): Int {
        return customPresetManager.getCustomPresets().size
    }
    
    /**
     * PM-002: 预设筛选
     */
    fun filterPresets(
        presets: List<MasterPreset>,
        brands: Set<String>? = null,
        scenes: Set<String>? = null,
        hasHncs: Boolean? = null,
        searchQuery: String? = null
    ): List<MasterPreset> {
        var filtered = presets
        
        // 品牌筛选
        if (!brands.isNullOrEmpty()) {
            filtered = filtered.filter { brands.contains(it.brand) }
        }
        
        // 场景筛选（基于tags）
        if (!scenes.isNullOrEmpty()) {
            filtered = filtered.filter { preset ->
                preset.tags?.any { tag -> scenes.contains(tag) } == true
            }
        }
        
        // 搜索筛选
        if (!searchQuery.isNullOrBlank()) {
            filtered = filtered.filter { preset ->
                preset.name.contains(searchQuery, ignoreCase = true) ||
                preset.author.contains(searchQuery, ignoreCase = true) ||
                preset.tags?.any { tag -> tag.contains(searchQuery, ignoreCase = true) } == true
            }
        }
        
        return filtered
    }
    
    companion object {
        @Volatile
        private var instance: PresetRepository? = null
        
        fun getInstance(context: Context): PresetRepository {
            return instance ?: synchronized(this) {
                instance ?: PresetRepository(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}