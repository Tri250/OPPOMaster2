package com.silas.omaster.ui.featured

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.local.FavoriteManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 作品收集 ViewModel
 * 管理精选预设列表、筛选、搜索和收藏状态
 */
class FeaturedPresetsViewModel(
    private val repository: PresetRepository,
    private val favoriteManager: FavoriteManager
) : ViewModel() {

    // 精选预设列表
    private val _featuredPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val featuredPresets: StateFlow<List<MasterPreset>> = _featuredPresets.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 筛选状态
    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _selectedScene = MutableStateFlow<String?>(null)
    val selectedScene: StateFlow<String?> = _selectedScene.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 收藏ID列表
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // 加载任务
    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    init {
        loadFeaturedPresets()
        observeFavorites()
    }

    /**
     * 加载精选预设
     */
    private fun loadFeaturedPresets() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                // 从仓库获取精选预设（HNCS或高评分预设）
                val presets = repository.getAllPresets()
                    .first()
                    .filter { it.isHncs || (it.rating ?: 0f) >= 4.5f }
                    .sortedByDescending { it.rating ?: 0f }
                _featuredPresets.value = presets
            }
            _isLoading.value = false
        }
    }

    /**
     * 观察收藏状态
     */
    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoriteManager.favoritesFlow.collect { ids ->
                _favoriteIds.value = ids
            }
        }
    }

    /**
     * 获取筛选后的预设
     */
    fun getFilteredPresets(): List<MasterPreset> {
        return _featuredPresets.value.filter { preset ->
            val brandMatch = _selectedBrand.value == null || preset.brand == _selectedBrand.value
            val sceneMatch = _selectedScene.value == null || 
                preset.tags?.contains(_selectedScene.value) == true
            val searchMatch = _searchQuery.value.isEmpty() ||
                preset.name.contains(_searchQuery.value, ignoreCase = true) ||
                preset.author.contains(_searchQuery.value, ignoreCase = true)
            brandMatch && sceneMatch && searchMatch
        }
    }

    /**
     * 设置品牌筛选
     */
    fun setBrand(brand: String?) {
        _selectedBrand.value = brand
    }

    /**
     * 设置场景筛选
     */
    fun setScene(scene: String?) {
        _selectedScene.value = scene
    }

    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 清空所有筛选
     */
    fun clearFilters() {
        _selectedBrand.value = null
        _selectedScene.value = null
        _searchQuery.value = ""
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(presetId)
        }
    }

    /**
     * 检查是否收藏
     */
    fun isFavorite(presetId: String): Boolean {
        return _favoriteIds.value.contains(presetId)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadFeaturedPresets()
    }

    /**
     * 获取所有可选品牌
     */
    fun getAvailableBrands(): List<String> {
        return _featuredPresets.value
            .mapNotNull { it.brand }
            .distinct()
            .sorted()
    }

    /**
     * 获取所有可选场景
     */
    fun getAvailableScenes(): List<String> {
        return _featuredPresets.value
            .flatMap { it.tags ?: emptyList() }
            .distinct()
            .sorted()
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        favoritesJob?.cancel()
    }
}

/**
 * FeaturedPresetsViewModel 工厂
 */
class FeaturedPresetsViewModelFactory(
    private val repository: PresetRepository,
    private val favoriteManager: FavoriteManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeaturedPresetsViewModel::class.java)) {
            return FeaturedPresetsViewModel(repository, favoriteManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}