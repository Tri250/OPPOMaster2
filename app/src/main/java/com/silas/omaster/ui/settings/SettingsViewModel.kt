package com.silas.omaster.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.cloud.CloudSyncManager
import com.silas.omaster.cloud.SyncResult
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.util.ImageCacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置页面 ViewModel
 * 管理所有设置状态：主题、深色模式、振动、云同步等
 */
class SettingsViewModel(
    private val application: Application,
    private val settingsManager: SettingsManager,
    private val presetRepository: PresetRepository
) : ViewModel() {

    // 主题设置
    private val _currentTheme = MutableStateFlow(settingsManager.currentTheme)
    val currentTheme: StateFlow<BrandTheme> = _currentTheme.asStateFlow()

    // 深色模式
    private val _darkMode = MutableStateFlow(settingsManager.darkMode)
    val darkMode: StateFlow<DarkMode> = _darkMode.asStateFlow()

    // 振动开关
    private val _vibrationEnabled = MutableStateFlow(settingsManager.isVibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // 分析开关
    private val _analyticsEnabled = MutableStateFlow(settingsManager.isAnalyticsEnabled)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    // 云同步开关
    private val _cloudSyncEnabled = MutableStateFlow(settingsManager.isCloudSyncEnabled)
    val cloudSyncEnabled: StateFlow<Boolean> = _cloudSyncEnabled.asStateFlow()

    // 最后同步时间
    private val _lastSyncTime = MutableStateFlow(settingsManager.lastSyncTime)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // 同步状态
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 默认启动Tab
    private val _defaultStartTab = MutableStateFlow(settingsManager.defaultStartTab)
    val defaultStartTab: StateFlow<Int> = _defaultStartTab.asStateFlow()

    // 更新渠道
    private val _updateChannel = MutableStateFlow(settingsManager.updateChannel)
    val updateChannel: StateFlow<UpdateChannel> = _updateChannel.asStateFlow()

    // 浮窗透明度
    private val _floatingWindowOpacity = MutableStateFlow(settingsManager.floatingWindowOpacity)
    val floatingWindowOpacity: StateFlow<Int> = _floatingWindowOpacity.asStateFlow()

    // 缓存大小
    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeSettings()
        updateCacheSize()
    }

    /**
     * 观察设置变化
     */
    private fun observeSettings() {
        viewModelScope.launch {
            settingsManager.themeFlow.collect { theme ->
                _currentTheme.value = theme
            }
        }
    }

    /**
     * 设置主题
     */
    fun setTheme(theme: BrandTheme) {
        settingsManager.currentTheme = theme
        _currentTheme.value = theme
    }

    /**
     * 设置深色模式
     */
    fun setDarkMode(mode: DarkMode) {
        settingsManager.darkMode = mode
        _darkMode.value = mode
    }

    /**
     * 设置振动开关
     */
    fun setVibrationEnabled(enabled: Boolean) {
        settingsManager.isVibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }

    /**
     * 设置分析开关
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        settingsManager.isAnalyticsEnabled = enabled
        _analyticsEnabled.value = enabled
    }

    /**
     * 设置云同步开关
     */
    fun setCloudSyncEnabled(enabled: Boolean) {
        settingsManager.isCloudSyncEnabled = enabled
        _cloudSyncEnabled.value = enabled
    }

    /**
     * 执行云同步
     * 先通过 CloudSyncManager 从 CDN 拉取各品牌预设，再刷新本地 PresetRepository。
     */
    fun performCloudSync(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val cloudSyncManager = CloudSyncManager.getInstance(application)
                val result = withContext(Dispatchers.IO) {
                    cloudSyncManager.sync()
                }
                if (result is SyncResult.Success) {
                    // 同步成功后刷新本地预设列表与缓存
                    withContext(Dispatchers.IO) {
                        presetRepository.reloadDefaultPresets()
                    }
                    _lastSyncTime.value = settingsManager.lastSyncTime
                    onComplete(true)
                } else if (result is SyncResult.Disabled) {
                    _errorMessage.value = "云同步未开启"
                    onComplete(false)
                } else {
                    val errorResult = result as? SyncResult.Error
                    _errorMessage.value = "同步失败: ${errorResult?.message ?: "未知错误"}"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = "同步失败: ${e.message}"
                onComplete(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * 格式化最后同步时间
     */
    fun formatLastSyncTime(): String {
        val time = _lastSyncTime.value
        if (time == 0L) return "从未同步"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(Date(time))
    }

    /**
     * 设置默认启动Tab
     */
    fun setDefaultStartTab(tab: Int) {
        settingsManager.defaultStartTab = tab
        _defaultStartTab.value = tab
    }

    /**
     * 设置更新渠道
     */
    fun setUpdateChannel(channel: UpdateChannel) {
        settingsManager.updateChannel = channel
        _updateChannel.value = channel
    }

    /**
     * 设置浮窗透明度
     */
    fun setFloatingWindowOpacity(opacity: Int) {
        settingsManager.floatingWindowOpacity = opacity
        _floatingWindowOpacity.value = opacity
    }

    /**
     * 更新缓存大小
     */
    fun updateCacheSize() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sizeMb = ImageCacheManager.getInstance(application).getCacheSize(application)
                _cacheSize.value = String.format("%.2f MB", sizeMb)
            }
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ImageCacheManager.getInstance(application).clearCache(application)
            }
            _cacheSize.value = "0.00 MB"
            onComplete()
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // 清理资源
    }
}

/**
 * SettingsViewModel 工厂
 */
class SettingsViewModelFactory(
    private val application: Application,
    private val settingsManager: SettingsManager,
    private val presetRepository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(application, settingsManager, presetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}