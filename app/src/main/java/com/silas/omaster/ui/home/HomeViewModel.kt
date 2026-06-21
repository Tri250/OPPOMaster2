package com.silas.omaster.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

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
     * 过滤后的预设列表 - 使用 StateFlow + combine 实现真正的响应式
     * 避免每次重组时重新计算，提升性能
     */
    val filteredPresets: StateFlow<List<MasterPreset>> = combine(
        _allPresets,
        _favorites,
        _selectedTab,
        _selectedBrand,
        _sortType,
        _searchQuery
    ) { allPresets, favorites, selectedTab, selectedBrand, sortType, searchQuery ->
        var result = allPresets.toList()

        // Tab 过滤
        when (selectedTab) {
            1 -> result = result.filter { it.isFavorite } // 收藏
            2 -> result = result.filter { it.isHncs }     // 哈苏
            3 -> result = result.filter { it.isNew }      // 上新
        }

        // 品牌过滤
        if (selectedBrand != "all") {
            result = result.filter { it.brand == selectedBrand }
        }

        // 搜索过滤
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            result = result.filter { preset ->
                preset.name.lowercase().contains(query) ||
                preset.author.lowercase().contains(query) ||
                preset.tags?.any { it.lowercase().contains(query) } == true
            }
        }

        // 排序：NEWEST 按 createdAt 降序，而非 isNew Boolean
        result = when (sortType) {
            SortType.NEWEST -> result.sortedByDescending { it.createdAt }
            SortType.POPULAR -> result.sortedByDescending { it.downloads ?: 0 }
            SortType.RATING -> result.sortedByDescending { it.rating ?: 0f }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * 获取Tab计数（对齐Web端）
     */
    fun getTabCount(tabIndex: Int): Int {
        return when (tabIndex) {
            0 -> _allPresets.value.size      // 发现
            1 -> _favorites.value.size       // 收藏
            2 -> _allPresets.value.filter { it.isHncs }.size  // 哈苏
            3 -> _allPresets.value.filter { it.isNew }.size   // 上新
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
     * 使用 Flow 的 collect 等待数据加载完成，而非固定 delay
     */
    fun refresh(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reloadDefaultPresets()
            loadPresets()
            // 等待 Flow 发射至少一次数据，而非固定 delay
            kotlinx.coroutines.withTimeoutOrNull(3000) {
                allPresets.first { it.isNotEmpty() || _allPresets.value.isEmpty() }
            }
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
