package com.silas.omaster.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.local.SubscriptionManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.network.PresetRemoteManager
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
    private val repository: PresetRepository,
    private val appContext: Context
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
                    _allPresets.value = presets
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
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

        searchHistoryJob = viewModelScope.launch {
            repository.searchHistory.collect { history ->
                _searchHistory.value = history
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
     */
    fun getFilteredPresets(): List<MasterPreset> {
        val baseList = _allPresets.value
        var result = baseList.toList()

        // Tab 过滤
        when (_selectedTab.value) {
            1 -> result = result.filter { it.isFavorite } // 收藏
            2 -> result = result.filter { it.isHncs }     // 哈苏
            3 -> result = result.filter { it.isNew }      // 上新
            4 -> result = _customPresets.value             // 我的（自定义预设）
        }

        // 品牌过滤
        if (_selectedBrand.value != "all") {
            result = result.filter { it.brand == _selectedBrand.value }
        }

        // 搜索过滤
        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            result = result.filter { preset ->
                preset.name?.lowercase()?.contains(query) == true ||
                preset.author?.lowercase()?.contains(query) == true ||
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
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        repository.clearSearchHistory()
    }

    /**
     * 刷新数据（仅本地，不拉取网络订阅）
     * 用于预设创建/编辑保存后的本地数据刷新
     * 修复：使用 forceReloadFromFiles 强制从文件重新加载，避免内存缓存非空导致数据不更新
     */
    fun refresh(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.forceReloadFromFiles()
            loadPresets()
            delay(500) // 给予足够时间让 Flow 发射新值并让 UI 感知
            onComplete()
        }
    }

    /**
     * 加载更新解析订阅管理链接
     *
     * 拉取所有启用的订阅源最新内容，解析后刷新本地内存预设。
     * 用于首页 Header 刷新按钮和下拉刷新，使用户在首页即可触发订阅链接的加载、更新、解析。
     *
     * @param onComplete 刷新完成回调（无论成功失败均会调用）
     * @param onResult 拉取结果回调 (successCount, upToDateCount, failCount)，供 UI 显示 Toast
     */
    fun refreshWithSubscriptions(
        onComplete: () -> Unit = {},
        onResult: (successCount: Int, upToDateCount: Int, failCount: Int) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            var successCount = 0
            var upToDateCount = 0
            var failCount = 0
            try {
                val subManager = SubscriptionManager.getInstance(appContext)
                val enabledSubs = subManager.subscriptionsFlow.value.filter { it.isEnabled }
                for (sub in enabledSubs) {
                    try {
                        val result = PresetRemoteManager.fetchAndSave(
                            appContext, sub.url, forceUpdate = false
                        )
                        if (result.isSuccess) {
                            successCount++
                        } else if (result.exceptionOrNull()?.message == "无需更新") {
                            upToDateCount++
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        Log.w("HomeViewModel", "订阅拉取失败: ${sub.name}", e)
                        failCount++
                    }
                }
                // 拉取完成后刷新内存预设
                repository.forceReloadFromFiles()
                loadPresets()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "订阅刷新整体失败", e)
                // 整体异常仍尝试刷新本地缓存
                try {
                    repository.forceReloadFromFiles()
                    loadPresets()
                } catch (_: Exception) {
                }
            } finally {
                onResult(successCount, upToDateCount, failCount)
                delay(300) // 给予 UI 感知时间
                onComplete()
            }
        }
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
    private val repository: PresetRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
