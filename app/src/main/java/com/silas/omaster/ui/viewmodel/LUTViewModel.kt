package com.silas.omaster.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.local.LUTLocalDataSource
import com.silas.omaster.data.model.*
import com.silas.omaster.data.remote.LUTRemoteDataSource
import com.silas.omaster.data.repository.LUTRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * LUT ViewModel
 * 管理 LUT 页面的所有状态
 */
class LUTViewModel(
    private val repository: LUTRepository
) : ViewModel() {

    // ===== 状态流 =====

    // LUT列表
    private val _luts = MutableStateFlow<Resource<List<MasterLUT>>>(Resource.Loading())
    val luts: StateFlow<Resource<List<MasterLUT>>> = _luts.asStateFlow()

    // 分类列表
    val categories: StateFlow<List<LUTCategory>> = flow {
        emit(LUTCategory.entries.toList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, LUTCategory.entries.toList())

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 选中的分类
    private val _selectedCategory = MutableStateFlow(LUTCategory.ALL)
    val selectedCategory: StateFlow<LUTCategory> = _selectedCategory.asStateFlow()

    // 排序方式
    private val _sortBy = MutableStateFlow(LUTSortBy.DOWNLOADS)
    val sortBy: StateFlow<LUTSortBy> = _sortBy.asStateFlow()

    // 下载状态
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // 收藏ID集合
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // 热门LUT
    private val _hotLUTs = MutableStateFlow<List<MasterLUT>>(emptyList())
    val hotLUTs: StateFlow<List<MasterLUT>> = _hotLUTs.asStateFlow()

    // 新品LUT
    private val _newLUTs = MutableStateFlow<List<MasterLUT>>(emptyList())
    val newLUTs: StateFlow<List<MasterLUT>> = _newLUTs.asStateFlow()

    // 精选LUT
    private val _featuredLUTs = MutableStateFlow<List<MasterLUT>>(emptyList())
    val featuredLUTs: StateFlow<List<MasterLUT>> = _featuredLUTs.asStateFlow()

    // 已下载LUT
    private val _downloadedLUTs = MutableStateFlow<List<MasterLUT>>(emptyList())
    val downloadedLUTs: StateFlow<List<MasterLUT>> = _downloadedLUTs.asStateFlow()

    // 收藏LUT
    private val _favoriteLUTs = MutableStateFlow<List<MasterLUT>>(emptyList())
    val favoriteLUTs: StateFlow<List<MasterLUT>> = _favoriteLUTs.asStateFlow()

    // 选中的LUT详情
    private val _selectedLUT = MutableStateFlow<MasterLUT?>(null)
    val selectedLUT: StateFlow<MasterLUT?> = _selectedLUT.asStateFlow()

    // 刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ===== 初始化 =====

    init {
        loadLUTs()
        loadHotLUTs()
        loadNewLUTs()
        loadFeaturedLUTs()
        loadDownloadedLUTs()
        loadFavorites()
        observeDownloadStates()
    }

    // ===== 数据加载 =====

    /**
     * 加载LUT列表
     */
    private fun loadLUTs() {
        viewModelScope.launch {
            repository.getLUTs(
                category = _selectedCategory.value,
                query = _searchQuery.value,
                sortBy = _sortBy.value
            ).collect { resource ->
                _luts.value = resource
            }
        }
    }

    /**
     * 加载热门LUT
     */
    private fun loadHotLUTs() {
        viewModelScope.launch {
            repository.getHotLUTs().collect { luts ->
                _hotLUTs.value = luts
            }
        }
    }

    /**
     * 加载新品LUT
     */
    private fun loadNewLUTs() {
        viewModelScope.launch {
            repository.getNewLUTs().collect { luts ->
                _newLUTs.value = luts
            }
        }
    }

    /**
     * 加载精选LUT
     */
    private fun loadFeaturedLUTs() {
        viewModelScope.launch {
            repository.getFeaturedLUTs().collect { luts ->
                _featuredLUTs.value = luts
            }
        }
    }

    /**
     * 加载已下载LUT
     */
    private fun loadDownloadedLUTs() {
        viewModelScope.launch {
            repository.getDownloadedLUTs().collect { luts ->
                _downloadedLUTs.value = luts
            }
        }
    }

    /**
     * 加载收藏
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavorites().collect { luts ->
                _favoriteLUTs.value = luts
                _favoriteIds.value = luts.map { it.id }.toSet()
            }
        }
    }

    /**
     * 观察下载状态
     */
    private fun observeDownloadStates() {
        viewModelScope.launch {
            repository.getAllDownloadStates().collect { states ->
                _downloadStates.value = states
            }
        }
    }

    // ===== 用户操作 =====

    /**
     * 设置搜索查询
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadLUTs()
    }

    /**
     * 设置分类
     */
    fun setCategory(category: LUTCategory) {
        _selectedCategory.value = category
        loadLUTs()
    }

    /**
     * 设置排序
     */
    fun setSortBy(sortBy: LUTSortBy) {
        _sortBy.value = sortBy
        loadLUTs()
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAll()
            loadLUTs()
            loadHotLUTs()
            loadNewLUTs()
            loadFeaturedLUTs()
            _isRefreshing.value = false
        }
    }

    /**
     * 下载LUT
     */
    fun downloadLUT(lut: MasterLUT) {
        viewModelScope.launch {
            repository.downloadLUT(lut).collect { progress ->
                val current = _downloadStates.value.toMutableMap()
                when (progress) {
                    is DownloadProgress.Starting -> {
                        current[lut.id] = DownloadState(lut.id, isDownloading = true)
                    }
                    is DownloadProgress.Downloading -> {
                        current[lut.id] = DownloadState(
                            lut.id,
                            isDownloading = true,
                            progress = progress.progress
                        )
                    }
                    is DownloadProgress.Completed -> {
                        current[lut.id] = DownloadState(
                            lut.id,
                            isCompleted = true,
                            filePath = progress.filePath,
                            downloadedAt = System.currentTimeMillis()
                        )
                        loadDownloadedLUTs()
                    }
                    is DownloadProgress.Error -> {
                        current[lut.id] = DownloadState(
                            lut.id,
                            error = progress.message
                        )
                    }
                }
                _downloadStates.value = current
            }
        }
    }

    /**
     * 删除已下载的LUT
     */
    fun deleteDownloadedLUT(lutId: String) {
        viewModelScope.launch {
            repository.deleteDownloadedLUT(lutId)
            loadDownloadedLUTs()
        }
    }

    /**
     * 切换收藏
     */
    fun toggleFavorite(lutId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(lutId)
            loadFavorites()
        }
    }

    /**
     * 检查是否收藏
     */
    fun isFavorite(lutId: String): Boolean {
        return _favoriteIds.value.contains(lutId)
    }

    /**
     * 检查是否已下载
     */
    fun isDownloaded(lutId: String): Boolean {
        return _downloadStates.value[lutId]?.isCompleted == true
    }

    /**
     * 获取下载进度
     */
    fun getDownloadProgress(lutId: String): Int {
        return _downloadStates.value[lutId]?.progress ?: 0
    }

    /**
     * 是否正在下载
     */
    fun isDownloading(lutId: String): Boolean {
        return _downloadStates.value[lutId]?.isDownloading == true
    }

    /**
     * 提交评分
     */
    fun submitRating(lutId: String, rating: Float) {
        viewModelScope.launch {
            repository.submitRating(lutId, rating)
        }
    }

    /**
     * 选择LUT查看详情
     */
    fun selectLUT(lut: MasterLUT) {
        _selectedLUT.value = lut
    }

    /**
     * 清除选中的LUT
     */
    fun clearSelectedLUT() {
        _selectedLUT.value = null
    }

    /**
     * 搜索LUT
     */
    fun search(query: String) {
        viewModelScope.launch {
            repository.searchLUTs(query).collect { luts ->
                _luts.value = Resource.Success(luts)
            }
        }
    }

    /**
     * 按系列筛选
     */
    fun filterByCollection(collection: String) {
        viewModelScope.launch {
            repository.getLUTsByCollection(collection).collect { luts ->
                _luts.value = Resource.Success(luts)
            }
        }
    }

    /**
     * 筛选HNCS认证
     */
    fun filterHncsCertified() {
        viewModelScope.launch {
            repository.getHncsCertifiedLUTs().collect { luts ->
                _luts.value = Resource.Success(luts)
            }
        }
    }

    // ===== 工厂方法 =====

    companion object {
        fun create(context: android.content.Context): LUTViewModel {
            val localDataSource = LUTLocalDataSource(context)
            val remoteDataSource = LUTRemoteDataSource(
                downloadDir = File(context.filesDir, "luts")
            )
            val repository = LUTRepository(remoteDataSource, localDataSource)
            return LUTViewModel(repository)
        }
    }
}