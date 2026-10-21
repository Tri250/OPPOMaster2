package com.silas.omaster.ui.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val allPresets: StateFlow<List<MasterPreset>> = _allPresets

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
    val deviceRecommendedPresets: StateFlow<List<MasterPreset>> = allPresets
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

    // ═══════════════════════════════════════════════════════════
    // 【v1.7.0】主页 2.0：搜索 2.0（防抖 + 历史 + 全文检索）与场景化分区
    // ═══════════════════════════════════════════════════════════

    /**
     * 防抖后的搜索关键字（150ms）。
     * 输入框直接驱动 [searchQuery] 保证 UI 响应；列表过滤走防抖值，
     * 避免每次击键都触发全量 filter + sortedBy 阻塞（v1.6.15 已下沉 Default，
     * 但列表规模大时仍需防抖削减无效计算）。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _debouncedQuery = MutableStateFlow("")
    val debouncedQuery: StateFlow<String> = _debouncedQuery.asStateFlow()

    /** 搜索历史（SharedPreferences 持久化，最多 [SearchHistoryStore.MAX_HISTORY] 条） */
    private val searchHistoryStore = SearchHistoryStore(appContext)
    private val _searchHistory = MutableStateFlow(searchHistoryStore.load())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    /** 当前选中的场景分类（null 表示不限场景） */
    private val _selectedScene = MutableStateFlow<SceneCategory?>(null)
    val selectedScene: StateFlow<SceneCategory?> = _selectedScene.asStateFlow()

    /**
     * 可用场景分类 —— 从 allPresets 的 tags 派生：
     * 预设有任一 tag 命中某场景关键词（不区分大小写）时该场景出现。
     * 避免改订阅 JSON Schema 即可获得场景化分区（tags 字段已存在）。
     */
    val availableScenes: StateFlow<List<SceneCategory>> = allPresets
        .map { presets ->
            SceneCategory.entries.filter { scene ->
                presets.any { p -> scene.matches(p) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // 搜索防抖：击停 150ms 后才驱动列表过滤
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { q -> _debouncedQuery.value = q }
        }
    }

    /**
     * 可用风格标签枚举 —— 从 allPresets 自动收集 MasterPreset.filter 字段去重
     * 使用 stateIn 直接转换为 StateFlow，避免嵌套 launch/collect anti-pattern
     */
    val availableFilters: StateFlow<List<String>> = allPresets
        .map { presets -> presets.mapNotNull { it.filter }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * FD-10 主管线：搜索 AND 风格筛选 AND 场景分区 AND 收藏/新预设过滤 → 排序 → 最终可见列表
     * 注意：kotlinx.coroutines 的 combine 最多 5 个 flow，更多需嵌套
     */
    val filteredPresets: StateFlow<List<MasterPreset>> = combine(
        combine(
            allPresets,
            favorites,
            _debouncedQuery,
            _selectedFilters,
            _selectedScene
        ) { all, favs, query, filters, scene ->
            listOf(all, favs, query, filters, scene)
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
        val scene = combined[4] as SceneCategory?
        val (sort, favOnly, newOnly) = triple

        val favIds = favs.map { it.id }.toSet()
        var result = all.asSequence()

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { p ->
                // 【v1.7.0】全文检索：名称 / 风格 / 标签 / 作者 / 描述标题与正文
                p.name.lowercase().contains(q) ||
                        (p.filter?.lowercase()?.contains(q) == true) ||
                        (p.tags?.any { it.lowercase().contains(q) } == true) ||
                        (p.author.lowercase().contains(q)) ||
                        (p.description?.title?.lowercase()?.contains(q) == true) ||
                        (p.description?.content?.lowercase()?.contains(q) == true)
            }
        }

        if (filters.isNotEmpty()) {
            result = result.filter { p ->
                (p.filter != null && p.filter in filters) ||
                        (p.tags?.any { it in filters } == true)
            }
        }

        // 【v1.7.0】场景分区过滤：tags 命中场景关键词即视为属于该场景
        if (scene != null) {
            result = result.filter { p -> scene.matches(p) }
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
    // v1.6.15 性能修复：过滤+排序在 Default（CPU）调度器执行。
    // 此前运行在 stateIn 收集上下文（Main），每次击键/收藏切换都全量 filter+sortedBy 阻塞主线程。
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ═══════════════════════════════════════════════════════════
    // FD-10 公开动作
    // ═══════════════════════════════════════════════════════════

    fun updateSearchQuery(q: String) { _searchQuery.value = q }

    /** 【v1.7.0】提交搜索：记录历史（去空白、去重、置顶、限长） */
    fun submitSearch(q: String) {
        val keyword = q.trim()
        if (keyword.isBlank()) return
        val updated = (listOf(keyword) + _searchHistory.value)
            .distinct()
            .take(SearchHistoryStore.MAX_HISTORY)
        _searchHistory.value = updated
        searchHistoryStore.save(updated)
    }

    /** 【v1.7.0】移除单条搜索历史 */
    fun removeSearchHistory(q: String) {
        val updated = _searchHistory.value - q
        _searchHistory.value = updated
        searchHistoryStore.save(updated)
    }

    /** 【v1.7.0】清空搜索历史 */
    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        searchHistoryStore.save(emptyList())
    }

    fun toggleFilter(label: String) {
        val current = _selectedFilters.value
        _selectedFilters.value = if (label in current) current - label else current + label
    }

    /** 【v1.7.0】切换场景分区（再次点击同一场景为取消） */
    fun toggleScene(scene: SceneCategory) {
        _selectedScene.value = if (_selectedScene.value == scene) null else scene
    }

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun toggleShowFavoritesOnly() { _showFavoritesOnly.value = !_showFavoritesOnly.value }

    fun toggleShowNewOnly() { _showNewOnly.value = !_showNewOnly.value }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _debouncedQuery.value = ""
        _selectedFilters.value = emptySet()
        _showFavoritesOnly.value = false
        _showNewOnly.value = false
        _sortOrder.value = SortOrder.DEFAULT
        _selectedScene.value = null
    }

    enum class SortOrder {
        DEFAULT,        // 仓库默认顺序
        NEW_FIRST,      // 新预设置顶
        FAVORITE_FIRST, // 收藏置顶
        NAME_A_Z        // 按名称 A → Z
    }

    /**
     * 【v1.7.0】主页 2.0 场景分类
     * 从预设 tags 派生（无需改订阅 Schema）：预设有任一 tag 命中关键词即归入该场景。
     */
    enum class SceneCategory(
        val labelResName: String,
        val keywords: List<String>
    ) {
        PORTRAIT("scene_portrait", listOf("人像", "portr", "肖像", "写真", "selfie")),
        LANDSCAPE("scene_landscape", listOf("风光", "landscap", "风景", "山", "海", "旅行", "travel")),
        NIGHT("scene_night", listOf("夜景", "night", "夜", "星空", "star", "灯光")),
        FOOD("scene_food", listOf("美食", "food", "吃", "餐", "美味", "delicious")),
        STREET("scene_street", listOf("街拍", "street", "城市", "city", "人文", "urban")),
        PET("scene_pet", listOf("宠物", "pet", "猫", "cat", "狗", "dog", "动物")),
        ARCHITECTURE("scene_architecture", listOf("建筑", "architect", "楼", "几何", "geometry", "极简")),
        PLANT("scene_plant", listOf("花", "flower", "植物", "plant", "绿", "森林", "forest", "春")),
        SUNSET("scene_sunset", listOf("日落", "sunset", "黄昏", "晚霞", "夕阳", "golden")),
        MONO("scene_mono", listOf("黑白", "mono", "bw", "灰度", "胶片", "film", "复古"));

        /** 预设有任一 tag 命中关键词（不区分大小写）即视为属于该场景 */
        fun matches(preset: MasterPreset): Boolean {
            val tags = preset.tags ?: return false
            return tags.any { tag ->
                val t = tag.lowercase()
                keywords.any { kw -> t.contains(kw.lowercase()) }
            }
        }

        companion object {
            /** 按标签名反查场景（UI 层已本地化，不需要） */
            fun fromLabelResName(name: String): SceneCategory? =
                entries.firstOrNull { it.labelResName == name }
        }
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
            // v1.6.15 性能修复：删除链路含全量 JSON 反序列化 + 同步文件删除，
            // 此前在 Main 调度器执行，切换预设时主线程磁盘 IO。整体移入 IO。
            withContext(Dispatchers.IO) {
                repository.deleteCustomPreset(presetId)
            }
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
        // 只取消本 ViewModel 自己的收集任务
        allPresetsJob?.cancel()
        favoritesJob?.cancel()
        customPresetsJob?.cancel()
        // 注意：repository 是进程级单例，其 scope 与 Application 同生命周期，
        // 不可在此取消（否则订阅变更监听会被永久中断）。
    }

    companion object {
        /** 搜索防抖时长（ms）——击停后才驱动列表过滤 */
        const val SEARCH_DEBOUNCE_MS = 150L
    }
}

/**
 * 【v1.7.0】搜索历史持久化（SharedPreferences，JSON 数组字符串）
 * 轻量本地存储，不引入 Room：历史仅 <10 条短文本。
 */
private class SearchHistoryStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            org.json.JSONArray(raw).let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it) }
            }
        }.getOrDefault(emptyList())
    }

    fun save(history: List<String>) {
        val arr = org.json.JSONArray()
        history.take(MAX_HISTORY).forEach { arr.put(it) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        const val MAX_HISTORY = 8
        private const val PREFS_NAME = "home_search_prefs"
        private const val KEY_HISTORY = "search_history"
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
