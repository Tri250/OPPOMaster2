package com.silas.omaster.ui.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.model.*
import com.silas.omaster.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * LUT 资源分享 ViewModel
 * 管理 LUT 列表、下载、收藏等状态
 */
class LUTViewModel(
    private val repository: LUTRepository
) : ViewModel() {

    // === 列表状态 ===
    private val _selectedCategory = MutableStateFlow(LUTCategory.ALL)
    val selectedCategory: StateFlow<LUTCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(LUTSortBy.DOWNLOADS)
    val sortBy: StateFlow<LUTSortBy> = _sortBy.asStateFlow()

    // LUT 列表（自动响应筛选条件变化）
    val luts: StateFlow<Resource<List<MasterLUT>>> = combine(
        _selectedCategory, _searchQuery, _sortBy
    ) { category, query, sort ->
        Triple(category, query, sort)
    }.flatMapLatest { (category, query, sort) ->
        repository.getLUTs(category, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    // === 热门与新品 ===
    val hotLUTs: StateFlow<List<MasterLUT>> = repository.getHotLUTs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newLUTs: StateFlow<List<MasterLUT>> = repository.getNewLUTs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === 下载状态 ===
    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val _downloadedIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    // 下载任务映射（用于取消）
    private val downloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // === 收藏 ===
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // === 分类列表 ===
    val categories: StateFlow<List<LUTCategory>> = flow {
        emit(LUTCategory.entries.toList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.getDownloadedLUTs().collect { luts ->
                _downloadedIds.value = luts.map { it.id }.toSet()
            }
        }
        viewModelScope.launch {
            repository.getFavorites().collect { luts ->
                _favoriteIds.value = luts.map { it.id }.toSet()
            }
        }
    }

    // === 操作方法 ===

    /**
     * 选择分类
     */
    fun selectCategory(category: LUTCategory) {
        _selectedCategory.value = category
    }

    /**
     * 更新搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 更新排序方式
     */
    fun updateSortBy(sort: LUTSortBy) {
        _sortBy.value = sort
    }

    /**
     * 下载 LUT
     */
    fun downloadLUT(lut: MasterLUT) {
        viewModelScope.launch {
            repository.downloadLUT(lut).collect { progress ->
                _downloadStates.value = _downloadStates.value + (lut.id to progress)
                if (progress is DownloadProgress.Completed) {
                    _downloadedIds.value = _downloadedIds.value + lut.id
                }
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload(lutId: String) {
        // 取消正在进行的下载任务
        downloadJobs[lutId]?.cancel()
        downloadJobs.remove(lutId)
        _downloadStates.value = _downloadStates.value - lutId
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(lutId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(lutId)
            _favoriteIds.value = if (lutId in _favoriteIds.value)
                _favoriteIds.value - lutId
            else
                _favoriteIds.value + lutId
        }
    }

    /**
     * 提交评分
     */
    fun submitRating(lutId: String, rating: Float) {
        viewModelScope.launch { repository.submitRating(lutId, rating) }
    }

    /**
     * 删除已下载的 LUT
     */
    fun deleteDownloaded(lutId: String) {
        viewModelScope.launch {
            repository.deleteDownloadedLUT(lutId)
            _downloadedIds.value = _downloadedIds.value - lutId
        }
    }

    /**
     * 判断是否已下载
     */
    fun isDownloaded(lutId: String): Boolean = lutId in _downloadedIds.value

    /**
     * 判断是否已收藏
     */
    fun isFavorite(lutId: String): Boolean = lutId in _favoriteIds.value

    /**
     * 获取下载进度
     */
    fun getDownloadProgress(lutId: String): DownloadProgress? = _downloadStates.value[lutId]

    /**
     * 刷新列表
     */
    fun refresh() {
        // 触发重新加载
        viewModelScope.launch {
            _searchQuery.value = _searchQuery.value
        }
    }
}
