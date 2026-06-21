package com.silas.omaster.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * 排序类型枚举（对齐Web端）
 */
enum class SortType {
    NEWEST,     // 最新
    POPULAR,    // 最热
    RATING      // 评分
}

/**
 * 主页 ViewModel
 * 管理预设列表、收藏、Tab状态、品牌筛选、排序和搜索（对齐Web端）
 *
 * 修复：
 * 1. 使用 Job 管理协程，避免重复收集
 * 2. refresh() 现在会取消旧任务并重新收集
 */
class HomeViewModel(
    private val repository: PresetRepository
) : ViewModel() {

    // 所有预设
    private val _allPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val allPresets: StateFlow<List<MasterPreset>> = _allPresets.asStateFlow()

    // 收藏的预设
    private val _favorites = MutableStateFlow<List<MasterPreset>>(emptyList())
    val favorites: StateFlow<List<MasterPreset>> = _favorites.asStateFlow()

    // 自定义预设（保留Android原生功能）
    private val _customPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val customPresets: StateFlow<List<MasterPreset>> = _customPresets.asStateFlow()

    // 当前选中的 Tab（对齐Web端：0=发现, 1=收藏, 2=哈苏, 3=上新）
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // 当前选中的品牌（对齐Web端）
    private val _selectedBrand = MutableStateFlow("all")
    val selectedBrand: StateFlow<String> = _selectedBrand.asStateFlow()

    // 当前排序方式（对齐Web端）
    private val _sortType = MutableStateFlow(SortType.NEWEST)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // 搜索关键词（对齐Web端）
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 用于管理收集任务的 Job
    private var allPresetsJob: Job? = null
    private var favoritesJob: Job? = null
    private var customPresetsJob: Job? = null

    init {
        loadPresets()
    }

    /**
     * 加载所有预设数据
     * 修复：先取消旧任务，再启动新任务，避免重复收集
     */
    private fun loadPresets() {
        // 取消之前的收集任务
        allPresetsJob?.cancel()
        favoritesJob?.cancel()
        customPresetsJob?.cancel()

        // 启动新的收集任务
        allPresetsJob = viewModelScope.launch {
            repository.getAllPresets().collect { presets ->
                _allPresets.value = presets
            }
        }

        favoritesJob = viewModelScope.launch {
            repository.getFavoritePresets().collect { favorites ->
                _favorites.value = favorites
            }
        }

        customPresetsJob = viewModelScope.launch {
            repository.getCustomPresets().collect { custom ->
                _customPresets.value = custom
            }
        }
    }

    /**
     * 切换 Tab（对齐Web端）
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    /**
     * 切换品牌筛选（对齐Web端）
     */
    fun selectBrand(brand: String) {
        _selectedBrand.value = brand
    }

    /**
     * 切换排序方式（对齐Web端）
     */
    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    /**
     * 设置搜索关键词（对齐Web端）
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 获取过滤后的预设列表（对齐Web端）
     * 修复：收藏Tab使用独立的 _favorites 数据源，而非从 _allPresets 中按 isFavorite 过滤
     */
    fun getFilteredPresets(): List<MasterPreset> {
        // 根据Tab选择基础列表
        val baseList: List<MasterPreset> = when (_selectedTab.value) {
            1 -> _favorites.value              // 收藏：使用独立数据源
            2 -> _allPresets.value.filter { it.isHncs }   // 哈苏
            3 -> _allPresets.value.filter { it.isNew }     // 上新
            else -> _allPresets.value          // 发现：全部
        }
        var result = baseList.toList()

        // 品牌过滤
        if (_selectedBrand.value != "all") {
            result = result.filter { it.brand == _selectedBrand.value }
        }

        // 搜索过滤
        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            result = result.filter { preset ->
                preset.name.lowercase().contains(query) ||
                preset.author.lowercase().contains(query) ||
                preset.tags?.any { it.lowercase().contains(query) } == true
            }
        }

        // 排序
        result = when (_sortType.value) {
            SortType.NEWEST -> result.sortedByDescending { it.isNew }
            SortType.POPULAR -> result.sortedByDescending { it.downloads ?: 0 }
            SortType.RATING -> result.sortedByDescending { it.rating ?: 0f }
        }

        return result
    }

    /**
     * 获取Tab计数（对齐Web端）
     * 修复：与 getFilteredPresets() 使用一致的数据源，收藏Tab使用 _favorites
     */
    fun getTabCount(tabIndex: Int): Int {
        return when (tabIndex) {
            0 -> _allPresets.value.size                              // 发现
            1 -> _favorites.value.size                               // 收藏：使用独立数据源
            2 -> _allPresets.value.count { it.isHncs }               // 哈苏
            3 -> _allPresets.value.count { it.isNew }                // 上新
            else -> 0
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(presetId)
        }
    }

    /**
     * 删除自定义预设
     */
    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
        }
    }

    /**
     * 刷新数据
     * 修复：现在会正确取消旧任务并重新收集
     */
    fun refresh(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reloadDefaultPresets()
            loadPresets()
            delay(500) // 给予足够时间让 Flow 发射新值并让 UI 感知
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 清理时取消所有任务
        allPresetsJob?.cancel()
        favoritesJob?.cancel()
        customPresetsJob?.cancel()
    }
}

/**
 * HomeViewModel 工厂
 */
class HomeViewModelFactory(
    private val repository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
