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
import kotlinx.coroutines.ensureActive

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

    // 当前选中的 Tab（对齐Web端：0=发现, 1=收藏, 2=哈苏, 3=上新, 4=我的）
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

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // 搜索历史
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // 用于管理收集任务的 Job
    private var allPresetsJob: Job? = null
    private var favoritesJob: Job? = null
    private var customPresetsJob: Job? = null
    private var searchHistoryJob: Job? = null

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
        searchHistoryJob?.cancel()

        _isLoading.value = true

        // 启动新的收集任务
        allPresetsJob = viewModelScope.launch {
            try {
                repository.getAllPresets().collect { presets ->
                    ensureActive()
                    _allPresets.value = presets
                    _isLoading.value = false
                    _errorState.value = null
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消时不更新状态
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _errorState.value = "加载失败: ${e.message}"
            }
        }

        favoritesJob = viewModelScope.launch {
            try {
                repository.getFavoritePresets().collect { favorites ->
                    ensureActive()
                    _favorites.value = favorites
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = "加载收藏失败: ${e.message}"
            }
        }

        customPresetsJob = viewModelScope.launch {
            try {
                repository.getCustomPresets().collect { custom ->
                    ensureActive()
                    _customPresets.value = custom
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = "加载自定义预设失败: ${e.message}"
            }
        }

        searchHistoryJob = viewModelScope.launch {
            try {
                repository.searchHistory.collect { history ->
                    ensureActive()
                    _searchHistory.value = history
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // 搜索历史加载失败不阻断主流程
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
     * 修复：增加空安全检查，防止预设列表为null时崩溃
     */
    fun getFilteredPresets(): List<MasterPreset> {
        val baseList = _allPresets.value
        var result = baseList.toList()

        // Tab 过滤
        when (_selectedTab.value) {
            1 -> result = result.filter { it.isFavorite } // 收藏
            2 -> result = result.filter { it.isHncs }     // 哈苏
            3 -> result = result.filter { it.isNew }      // 上新
            4 -> result = _customPresets.value // 我的（自定义预设）
        }

        // 品牌过滤
        val currentBrand = _selectedBrand.value
        if (currentBrand != "all") {
            result = result.filter { it.brand == currentBrand }
        }

        // 搜索过滤
        val currentQuery = _searchQuery.value
        if (currentQuery.isNotEmpty()) {
            val query = currentQuery.lowercase()
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
     */
    fun getTabCount(tabIndex: Int): Int {
        return when (tabIndex) {
            0 -> _allPresets.value.size      // 发现
            1 -> _favorites.value.size       // 收藏
            2 -> _allPresets.value.filter { it.isHncs }.size  // 哈苏
            3 -> _allPresets.value.filter { it.isNew }.size   // 上新
            4 -> _customPresets.value.size   // 我的
            else -> 0
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(presetId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = "操作失败: ${e.message}"
            }
        }
    }

    /**
     * 删除自定义预设
     */
    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomPreset(presetId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        try {
            repository.clearSearchHistory()
        } catch (e: Exception) {
            _errorState.value = "清空搜索历史失败: ${e.message}"
        }
    }

    /**
     * 刷新数据
     * 修复：使用 forceReloadFromFiles 强制从文件重新加载，避免内存缓存非空导致数据不更新
     */
    fun refresh(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.forceReloadFromFiles()
                loadPresets()
                _errorState.value = null
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorState.value = "刷新失败: ${e.message}"
                onComplete()
            }
        }
    }

    /**
     * 重试加载
     */
    fun retry() {
        _errorState.value = null
        refresh()
    }

    // ===== 测试辅助方法（仅供单测访问，不参与生产路径） =====
    internal fun setInternalPresets(list: List<MasterPreset>) {
        _allPresets.value = list
    }

    internal fun setInternalFavorites(list: List<MasterPreset>) {
        _favorites.value = list
    }

    internal fun setInternalCustomPresets(list: List<MasterPreset>) {
        _customPresets.value = list
    }

    internal fun setInternalError(message: String?) {
        _errorState.value = message
    }

    override fun onCleared() {
        super.onCleared()
        // 清理时取消所有任务
        allPresetsJob?.cancel()
        favoritesJob?.cancel()
        customPresetsJob?.cancel()
        searchHistoryJob?.cancel()
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
