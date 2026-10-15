package com.silas.omaster.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.network.PresetRemoteManager
import com.silas.omaster.util.DeviceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    // ═══════════════════════════════════════════════════════════
    val deviceDisplayName: String by lazy {
        val dp = DeviceDetector.getCurrentDevice(appContext)
        dp.displayNameZh.ifBlank { dp.model }
    }
    val isFindX10Series: Boolean by lazy { DeviceDetector.isFindX10Series(appContext) }

    val deviceRecommendedPresets: StateFlow<List<MasterPreset>> = allPresets.asStateFlow().combine(
        MutableStateFlow(DeviceDetector.getCurrentDevice(appContext))
    ) { presets, deviceProfile ->
        // 筛选匹配：deviceProfile.deviceKey 匹配 或 brandTag 匹配 或 deviceSupport 包含 deviceKey
        presets.filter { preset ->
            val presetDp = preset.deviceProfile
            val keyMatched = presetDp == null ||
                    presetDp.deviceKey == com.silas.omaster.model.DeviceProfile.ALL ||
                    presetDp.deviceKey == deviceProfile.deviceKey
            val brandMatched = presetDp == null ||
                    presetDp.brandTag == com.silas.omaster.model.DeviceProfile.BRAND_ALL ||
                    presetDp.brandTag.equals(deviceProfile.brandTag, ignoreCase = true)
            val supportMatched = preset.deviceSupport == null ||
                    preset.deviceSupport.isEmpty() ||
                    preset.deviceSupport.contains(deviceProfile.deviceKey) ||
                    preset.deviceSupport.contains(com.silas.omaster.model.DeviceProfile.ALL)
            // 只要预设声明了 deviceProfile 且匹配当前设备 — 精选；没有声明的预设也通过（作为通配）
            val presetIsDeviceSpecific = presetDp != null || !preset.deviceSupport.isNullOrEmpty()
            // 精选只展示有 deviceProfile 且匹配的预设；或所有通配预设（如果设备专属预设为空）
            if (presetIsDeviceSpecific) keyMatched && brandMatched && supportMatched else true
        }
    }.let { flow ->
        MutableStateFlow<List<MasterPreset>>(emptyList()).also { holder ->
            viewModelScope.launch { flow.collect { holder.value = it } }
        }.asStateFlow()
    }

    // 当前选中的 Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

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
