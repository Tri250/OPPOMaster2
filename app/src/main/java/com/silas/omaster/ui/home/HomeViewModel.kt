package com.silas.omaster.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.DeviceProfile
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.network.PresetRemoteManager
import com.silas.omaster.util.DeviceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * 主页 ViewModel
 * 管理预设列表、收藏和 Tab 状态
 *
 * 修复：
 * 1. 使用 Job 管理协程，避免重复收集
 * 2. refresh() 现在会取消旧任务并重新收集
 * 3. refresh() 现在会从远程获取最新预设
 *
 * MU-21 / FD-04 v1.6 新增：
 * 4. deviceRecommendedPresets — 机型感知精选预设（deviceProfile 匹配当前设备）
 * 5. deviceDisplayName — 当前设备展示名，用于 Banner 标题
 */
class HomeViewModel(
    application: Application,
    private val repository: PresetRepository
) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()

    // 所有预设
    private val _allPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val allPresets: StateFlow<List<MasterPreset>> = _allPresets.asStateFlow()

    // 收藏的预设
    private val _favorites = MutableStateFlow<List<MasterPreset>>(emptyList())
    val favorites: StateFlow<List<MasterPreset>> = _favorites.asStateFlow()

    // 自定义预设
    private val _customPresets = MutableStateFlow<List<MasterPreset>>(emptyList())
    val customPresets: StateFlow<List<MasterPreset>> = _customPresets.asStateFlow()

    // ═══════════════════════════════════════════════════════════
    // FD-04：机型感知精选预设（v1.6 新增）
    // 从 allPresets 中筛选 deviceProfile / deviceSupport 匹配当前设备的预设
    // 若设备无匹配预设则发出 emptyList，UI 层据此隐藏 Banner
    // 注意：设备身份在运行时不变，deviceProfile 用 lazy 缓存一次即可
    // ═══════════════════════════════════════════════════════════
    private val currentDeviceProfile: DeviceProfile by lazy {
        DeviceDetector.getCurrentDevice(appContext)
    }
    val deviceDisplayName: String by lazy {
        currentDeviceProfile.displayNameZh.ifBlank { currentDeviceProfile.model }
    }
    val isFindX10Series: Boolean by lazy { DeviceDetector.isFindX10Series(appContext) }

    /**
     * 机型匹配精选预设 —— 仅依赖 allPresets 的变化，deviceProfile 用 lazy 固定
     * 避免 combine 2-arg vs vararg 重载歧义导致的类型推断失败
     */
    val deviceRecommendedPresets: StateFlow<List<MasterPreset>> = allPresets.asStateFlow()
        .map { presets ->
            val dp = currentDeviceProfile
            val key = dp.deviceKey
            val brand = dp.brandTag
            val dp_all = DeviceProfile.ALL
            presets.filter { preset ->
                val presetDp = preset.deviceProfile
                val support = preset.deviceSupport
                val presetIsDeviceSpecific = presetDp != null || !support.isNullOrEmpty()
                if (!presetIsDeviceSpecific) return@filter true
                val keyMatch = presetDp == null || presetDp.deviceKey == dp_all || presetDp.deviceKey == key
                val brandMatch = presetDp == null || presetDp.brandTag == DeviceProfile.BRAND_ALL || presetDp.brandTag.equals(brand, true)
                val supportMatch = support == null || support.isEmpty() || support.contains(key) || support.contains(dp_all)
                keyMatch && brandMatch && supportMatch
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // 当前选中的 Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // ═══════════════════════════════════════════════════════════
    // FD-10 / v1.6：多维筛选 + 搜索 + 排序
    // ═══════════════════════════════════════════════════════════

    /** 搜索关键字（空则不过滤） */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 多选筛选 —— 风格标签（对应 MasterPreset.filter 字段） */
    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()

    /** 排序方式 */
    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    /** 是否只看收藏 */
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    /** 是否只看新预设 */
    private val _showNewOnly = MutableStateFlow(false)
    val showNewOnly: StateFlow<Boolean> = _showNewOnly.asStateFlow()

    /**
     * 可用风格标签枚举 —— 从 allPresets 自动收集 MasterPreset.filter 字段去重
     * 使用 stateIn 直接转换为 StateFlow，避免嵌套 launch/collect anti-pattern
     */
    val availableFilters: StateFlow<List<String>> = allPresets.asStateFlow()
        .map { presets -> presets.mapNotNull { it.filter }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * FD-10 主管线：搜索 AND 风格筛选 AND 收藏/新预设过滤 → 排序 → 最终可见列表
     * 注意：kotlinx.coroutines 的 combine 最多 5 个 flow，7 个需嵌套
     */
    val filteredPresets: StateFlow<List<MasterPreset>> = combine(
        combine(
            allPresets.asStateFlow(),
            favorites,
            _searchQuery,
            _selectedFilters
        ) { all, favs, query, filters ->
            listOf(all, favs, query, filters)
        },
        combine(
            _sortOrder,
            _showFavoritesOnly,
            _showNewOnly
        ) { sort, favOnly, newOnly ->
            Triple(sort, favOnly, newOnly)
        }
    ) { combined, triple ->
        val all = combined[0] as List<MasterPreset>
        val favs = combined[1] as List<MasterPreset>
        val query = combined[2] as String
        val filters = combined[3] as Set<String>
        val (sort, favOnly, newOnly) = triple

        val favIds = favs.map { it.id }.toSet()
        var result = all.asSequence()

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { p ->
                p.name.lowercase().contains(q) ||
                        (p.filter?.lowercase()?.contains(q) == true) ||
                        (p.tags?.any { it.lowercase().contains(q) } == true)
            }
        }

        if (filters.isNotEmpty()) {
            result = result.filter { p ->
                (p.filter != null && p.filter in filters) ||
                        (p.tags?.any { it in filters } == true)
            }
        }

        if (favOnly) result = result.filter { favIds.contains(it.id) }
        if (newOnly) result = result.filter { it.isNew }

        val sorted = when (sort) {
            SortOrder.DEFAULT -> result.toList()
            SortOrder.NEW_FIRST -> result.sortedByDescending { it.isNew }.toList()
            SortOrder.FAVORITE_FIRST -> result.sortedByDescending { favIds.contains(it.id) }.toList()
            SortOrder.NAME_A_Z -> result.sortedBy { it.name }.toList()
        }
        sorted
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ═══════════════════════════════════════════════════════════
    // FD-10 公开动作
    // ═══════════════════════════════════════════════════════════

    fun updateSearchQuery(q: String) { _searchQuery.value = q }

    fun toggleFilter(label: String) {
        val current = _selectedFilters.value
        _selectedFilters.value = if (label in current) current - label else current + label
    }

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun toggleShowFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }

    fun toggleShowNewOnly() { _showNewOnly.value = !_showNewOnly.value }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedFilters.value = emptySet()
        _showFavoritesOnly.value = false
        _showNewOnly.value = false
        _sortOrder.value = SortOrder.DEFAULT
    }

    enum class SortOrder {
        DEFAULT,        // 仓库默认顺序
        NEW_FIRST,      // 新预设置顶
        FAVORITE_FIRST, // 收藏置顶
        NAME_A_Z        // 按名称 A → Z
    }

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
     * 切换 Tab
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
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
     * 新增：下拉刷新会从远程获取最新预设，并返回更新结果
     */
    fun refresh(onComplete: (RefreshResult) -> Unit = {}) {
        viewModelScope.launch {
            // 1. 先尝试从远程更新所有启用的订阅
            val config = ConfigCenter.getInstance(appContext)
            val subscriptions = config.subscriptionsFlow.value
            val enabledSubs = subscriptions.filter { it.isEnabled }

            var successCount = 0
            var upToDateCount = 0
            var failCount = 0

            if (enabledSubs.isNotEmpty()) {
                for (sub in enabledSubs) {
                    try {
                        val result = PresetRemoteManager.fetchAndSave(appContext, sub.url)
                        if (result.isSuccess) {
                            successCount++
                        } else if (result.exceptionOrNull()?.message == "无需更新") {
                            upToDateCount++
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        // 单个订阅失败不影响其他订阅
                        failCount++
                        android.util.Log.e("HomeViewModel", "Failed to update subscription: ${sub.url}", e)
                    }
                }
            }

            // 2. 重新加载本地预设（包括刚下载的更新）
            repository.reloadDefaultPresets()
            loadPresets()
            delay(500) // 给予足够时间让 Flow 发射新值并让 UI 感知

            // 3. 返回更新结果
            val result = when {
                enabledSubs.isEmpty() -> RefreshResult.NoSubscriptions
                successCount > 0 && upToDateCount > 0 -> RefreshResult.PartialUpdate(successCount, upToDateCount)
                successCount > 0 -> RefreshResult.Success(successCount)
                upToDateCount > 0 && failCount == 0 -> RefreshResult.UpToDate(upToDateCount)
                failCount > 0 -> RefreshResult.Failed(failCount)
                else -> RefreshResult.UpToDate(enabledSubs.size)
            }
            onComplete(result)
        }
    }

    /**
     * 刷新结果枚举
     */
    sealed class RefreshResult {
        data class Success(val count: Int) : RefreshResult()
        data class PartialUpdate(val updated: Int, val upToDate: Int) : RefreshResult()
        data class UpToDate(val count: Int) : RefreshResult()
        data class Failed(val count: Int) : RefreshResult()
        object NoSubscriptions : RefreshResult()
    }

    override fun onCleared() {
        super.onCleared()
        // 清理时取消所有任务
        allPresetsJob?.cancel()
        favoritesJob?.cancel()
        customPresetsJob?.cancel()
        // 清理 Repository 的协程作用域，避免内存泄漏
        repository.cleanup()
    }
}

/**
 * HomeViewModel 工厂
 */
class HomeViewModelFactory(
    private val repository: PresetRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
