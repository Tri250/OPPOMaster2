package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.ui.theme.BrandTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 更新渠道枚举
 */
enum class UpdateChannel {
    GITEE,    // 默认，国内访问快
    GITHUB    // GitHub，国际访问
}

/**
 * 深色模式枚举
 */
enum class DarkMode {
    SYSTEM,   // 跟随系统
    LIGHT,    // 浅色模式
    DARK      // 深色模式
}

/**
 * 云同步状态枚举
 */
enum class CloudSyncStatus {
    DISABLED,     // 未启用
    SYNCING,      // 同步中
    SYNCED,       // 已同步
    ERROR         // 同步出错
}

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
        }

    private val _themeFlow: MutableStateFlow<BrandTheme>
    val themeFlow: StateFlow<BrandTheme>

    init {
        val themeId = prefs.getString(KEY_THEME_ID, BrandTheme.Hasselblad.id) ?: BrandTheme.Hasselblad.id
        _themeFlow = MutableStateFlow(BrandTheme.fromId(themeId))
        themeFlow = _themeFlow.asStateFlow()
    }

    var currentTheme: BrandTheme
        get() = _themeFlow.value
        set(value) {
            prefs.edit().putString(KEY_THEME_ID, value.id).apply()
            _themeFlow.value = value
        }

    // 悬浮窗透明度 (30-70%，默认56%)
    var floatingWindowOpacity: Int
        get() = prefs.getInt(KEY_FLOATING_WINDOW_OPACITY, 56)
        set(value) {
            prefs.edit().putInt(KEY_FLOATING_WINDOW_OPACITY, value.coerceIn(30, 70)).apply()
        }

    // 默认启动 Tab (0=全部, 1=收藏, 2=我的，默认0)
    var defaultStartTab: Int
        get() = prefs.getInt(KEY_DEFAULT_START_TAB, 0)
        set(value) {
            prefs.edit().putInt(KEY_DEFAULT_START_TAB, value.coerceIn(0, 2)).apply()
        }

    // 更新渠道（默认 Gitee）
    var updateChannel: UpdateChannel
        get() {
            val value = prefs.getString(KEY_UPDATE_CHANNEL, UpdateChannel.GITEE.name)
            return try {
                UpdateChannel.valueOf(value ?: UpdateChannel.GITEE.name)
            } catch (e: Exception) {
                UpdateChannel.GITEE
            }
        }
        set(value) {
            prefs.edit().putString(KEY_UPDATE_CHANNEL, value.name).apply()
        }

    // 友盟统计开关（默认开启，因为用户首次已同意隐私政策）
    var isAnalyticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()
        }

    // ==================== 新增功能 ====================

    // 深色模式设置
    var darkMode: DarkMode
        get() {
            val value = prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name)
            return try {
                DarkMode.valueOf(value ?: DarkMode.SYSTEM.name)
            } catch (e: Exception) {
                DarkMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DARK_MODE, value.name).apply()
        }

    // 云同步开关
    var isCloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, value).apply()
        }

    // 云同步状态
    var cloudSyncStatus: CloudSyncStatus
        get() {
            val value = prefs.getString(KEY_CLOUD_SYNC_STATUS, CloudSyncStatus.DISABLED.name)
            return try {
                CloudSyncStatus.valueOf(value ?: CloudSyncStatus.DISABLED.name)
            } catch (e: Exception) {
                CloudSyncStatus.DISABLED
            }
        }
        set(value) {
            prefs.edit().putString(KEY_CLOUD_SYNC_STATUS, value.name).apply()
        }

    // 最后同步时间
    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0)
        set(value) {
            prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()
        }

    // 用户ID（用于云同步）
    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) {
            prefs.edit().putString(KEY_USER_ID, value).apply()
        }

    // AI 场景识别开关
    var isAISceneRecognitionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_SCENE_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AI_SCENE_ENABLED, value).apply()
        }

    // AI 微调开关
    var isAIFineTuneEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_FINE_TUNE_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AI_FINE_TUNE_ENABLED, value).apply()
        }

    // 水印编辑器开关
    var isWatermarkEditorEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATERMARK_EDITOR_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_WATERMARK_EDITOR_ENABLED, value).apply()
        }

    // 哈苏色彩科学开关
    var isHasselbladColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_HASSELBLAD_COLOR_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HASSELBLAD_COLOR_ENABLED, value).apply()
        }

    companion object {
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_FLOATING_WINDOW_OPACITY = "floating_window_opacity"
        private const val KEY_DEFAULT_START_TAB = "default_start_tab"
        private const val KEY_UPDATE_CHANNEL = "update_channel"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"

        // 新增功能 Key
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        private const val KEY_CLOUD_SYNC_STATUS = "cloud_sync_status"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AI_SCENE_ENABLED = "ai_scene_enabled"
        private const val KEY_AI_FINE_TUNE_ENABLED = "ai_fine_tune_enabled"
        private const val KEY_WATERMARK_EDITOR_ENABLED = "watermark_editor_enabled"
        private const val KEY_HASSELBLAD_COLOR_ENABLED = "hasselblad_color_enabled"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
