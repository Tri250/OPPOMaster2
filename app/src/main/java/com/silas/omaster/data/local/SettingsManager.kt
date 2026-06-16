package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.ui.theme.BrandTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 工具函数：安全的枚举反序列化（性能优化：基于 enumConstants 缓存）
 * @param name 枚举名称
 * @param default 解析失败时的默认值
 * @return 解析结果或默认值
 */
private inline fun <reified E : Enum<E>> safeValueOf(name: String?, default: E): E {
    if (name.isNullOrEmpty()) return default
    return try {
        enumValueOf<E>(name)
    } catch (_: IllegalArgumentException) {
        default
    } catch (_: NullPointerException) {
        default
    }
}

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

/**
 * API配置数据类
 */
@Serializable
data class ApiConfig(
    val aiApiEndpoint: String = "",
    val presetApiEndpoint: String = "",
    val authApiEndpoint: String = "",
    val apiVersion: String = "v1"
)

class SettingsManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // ==================== 云端API配置 ====================
    
    // 云端AI推理API端点
    val aiApiEndpoint: String
        get() = prefs.getString(KEY_AI_API_ENDPOINT, DEFAULT_AI_API_ENDPOINT) ?: DEFAULT_AI_API_ENDPOINT
    
    // 预设同步API端点
    val presetApiEndpoint: String
        get() = prefs.getString(KEY_PRESET_API_ENDPOINT, DEFAULT_PRESET_API_ENDPOINT) ?: DEFAULT_PRESET_API_ENDPOINT
    
    // 用户认证API端点
    val authApiEndpoint: String
        get() = prefs.getString(KEY_AUTH_API_ENDPOINT, DEFAULT_AUTH_API_ENDPOINT) ?: DEFAULT_AUTH_API_ENDPOINT
    
    // API版本
    val apiVersion: String
        get() = prefs.getString(KEY_API_VERSION, "v1") ?: "v1"
    
    // 是否已加载API配置
    val isApiConfigLoaded: Boolean
        get() = prefs.getBoolean(KEY_API_CONFIG_LOADED, false)

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
        }

    private val _themeFlow: MutableStateFlow<BrandTheme>
    val themeFlow: StateFlow<BrandTheme>

    private val _darkModeFlow: MutableStateFlow<DarkMode>
    val darkModeFlow: StateFlow<DarkMode>

    init {
        val themeId = prefs.getString(KEY_THEME_ID, BrandTheme.Hasselblad.id) ?: BrandTheme.Hasselblad.id
        _themeFlow = MutableStateFlow(BrandTheme.fromId(themeId))
        themeFlow = _themeFlow.asStateFlow()

        _darkModeFlow = MutableStateFlow(
            safeValueOf(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name), DarkMode.SYSTEM)
        )
        darkModeFlow = _darkModeFlow.asStateFlow()
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

    // 默认启动 Tab (0=发现, 1=收藏, 2=哈苏, 3=上新，默认0)
    var defaultStartTab: Int
        get() = prefs.getInt(KEY_DEFAULT_START_TAB, 0)
        set(value) {
            prefs.edit().putInt(KEY_DEFAULT_START_TAB, value.coerceIn(0, 3)).apply()
        }

    // 更新渠道（默认 Gitee）
    var updateChannel: UpdateChannel
        get() = safeValueOf(prefs.getString(KEY_UPDATE_CHANNEL, UpdateChannel.GITEE.name), UpdateChannel.GITEE)
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
        get() = safeValueOf(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.name), DarkMode.SYSTEM)
        set(value) {
            prefs.edit().putString(KEY_DARK_MODE, value.name).apply()
            _darkModeFlow.value = value
        }

    // 上次使用的水印模板
    var lastWatermarkTemplate: String?
        get() = prefs.getString(KEY_LAST_WATERMARK_TEMPLATE, null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_LAST_WATERMARK_TEMPLATE, value).apply()
            } else {
                prefs.edit().remove(KEY_LAST_WATERMARK_TEMPLATE).apply()
            }
        }

    // 云同步开关（默认开启）
    var isCloudSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, value).apply()
        }

    // 云端预设数据源 URL
    val cloudPresetUrls: Map<String, String>
        get() = mapOf(
            "oppo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
            "realme" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
            "vivo" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
            "honor" to "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json"
        )

    // 云同步状态
    var cloudSyncStatus: CloudSyncStatus
        get() = safeValueOf(prefs.getString(KEY_CLOUD_SYNC_STATUS, CloudSyncStatus.DISABLED.name), CloudSyncStatus.DISABLED)
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

    // 云端API密钥（用于云端AI推理）
    var cloudApiKey: String?
        get() = prefs.getString(KEY_CLOUD_API_KEY, null)
        set(value) {
            prefs.edit().putString(KEY_CLOUD_API_KEY, value).apply()
        }

    // 哈苏之眼开关（带 StateFlow 支持）
    private val _isAISceneRecognitionEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_AI_SCENE_ENABLED, true))
    val isAISceneRecognitionEnabledFlow: StateFlow<Boolean> = _isAISceneRecognitionEnabledFlow.asStateFlow()

    var isAISceneRecognitionEnabled: Boolean
        get() = _isAISceneRecognitionEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_AI_SCENE_ENABLED, value).apply()
            _isAISceneRecognitionEnabledFlow.value = value
        }

    // AI 微调开关
    var isAIFineTuneEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_FINE_TUNE_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AI_FINE_TUNE_ENABLED, value).apply()
        }

    // 智能优化开关
    var isSmartOptimizeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_OPTIMIZE_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SMART_OPTIMIZE_ENABLED, value).apply()
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

    // 自定义设备型号（WM-003）
    var customDeviceModel: String
        get() = prefs.getString(KEY_CUSTOM_DEVICE_MODEL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_DEVICE_MODEL, value).apply()
        }

    // 预设版本映射（JSON 字符串），用于云端增量更新对比
    var presetVersionMapJson: String
        get() = prefs.getString(KEY_PRESET_VERSION_MAP, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_PRESET_VERSION_MAP, value).apply()
        }

    // 收藏的预设ID列表（PM-003）
    var favoritePresetIds: List<String>
        get() = prefs.getStringSet(KEY_FAVORITE_PRESET_IDS, emptySet())?.toList() ?: emptyList()
        set(value) {
            prefs.edit().putStringSet(KEY_FAVORITE_PRESET_IDS, value.toSet()).apply()
        }

    // 置顶的预设ID列表（PM-008）
    var pinnedPresetIds: List<String>
        get() = prefs.getStringSet(KEY_PINNED_PRESET_IDS, emptySet())?.toList() ?: emptyList()
        set(value) {
            prefs.edit().putStringSet(KEY_PINNED_PRESET_IDS, value.toSet()).apply()
        }

    // 手动修改的参数（PP-005）
    var manuallyModifiedParams: List<String>
        get() = prefs.getStringSet(KEY_MANUALLY_MODIFIED_PARAMS, emptySet())?.toList() ?: emptyList()
        set(value) {
            prefs.edit().putStringSet(KEY_MANUALLY_MODIFIED_PARAMS, value.toSet()).apply()
        }

    // 自定义快捷档位（PP-004）
    // 注意：需要JSON序列化存储
    var customQuickPresets: Map<String, Map<String, Int>>
        get() {
            val jsonStr = prefs.getString(KEY_CUSTOM_QUICK_PRESETS, null) ?: return emptyMap()
            return try {
                kotlinx.serialization.json.Json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val jsonStr = kotlinx.serialization.json.Json.encodeToString(value)
            prefs.edit().putString(KEY_CUSTOM_QUICK_PRESETS, jsonStr).apply()
        }

    // 应用预设参数（精选推荐功能）
    fun applyPresetParams(
        saturation: Int = 0,
        contrast: Int = 0,
        warmth: Int = 0,
        sharpness: Int = 0,
        clarity: Int = 0,
        brightness: Int = 0
    ) {
        prefs.edit().apply {
            putInt(KEY_APPLIED_SATURATION, saturation)
            putInt(KEY_APPLIED_CONTRAST, contrast)
            putInt(KEY_APPLIED_WARMTH, warmth)
            putInt(KEY_APPLIED_SHARPNESS, sharpness)
            putInt(KEY_APPLIED_CLARITY, clarity)
            putInt(KEY_APPLIED_BRIGHTNESS, brightness)
            putBoolean(KEY_HAS_APPLIED_PRESET, true)
        }.apply()
    }

    // 获取已应用的预设参数
    fun getAppliedPresetParams(): Map<String, Int> {
        return mapOf(
            "saturation" to prefs.getInt(KEY_APPLIED_SATURATION, 0),
            "contrast" to prefs.getInt(KEY_APPLIED_CONTRAST, 0),
            "warmth" to prefs.getInt(KEY_APPLIED_WARMTH, 0),
            "sharpness" to prefs.getInt(KEY_APPLIED_SHARPNESS, 0),
            "clarity" to prefs.getInt(KEY_APPLIED_CLARITY, 0),
            "brightness" to prefs.getInt(KEY_APPLIED_BRIGHTNESS, 0)
        )
    }

    // 是否已应用预设
    fun hasAppliedPreset(): Boolean {
        return prefs.getBoolean(KEY_HAS_APPLIED_PRESET, false)
    }

    // 清除已应用的预设
    fun clearAppliedPreset() {
        prefs.edit().apply {
            remove(KEY_APPLIED_SATURATION)
            remove(KEY_APPLIED_CONTRAST)
            remove(KEY_APPLIED_WARMTH)
            remove(KEY_APPLIED_SHARPNESS)
            remove(KEY_APPLIED_CLARITY)
            remove(KEY_APPLIED_BRIGHTNESS)
            putBoolean(KEY_HAS_APPLIED_PRESET, false)
        }.apply()
    }

    // 迁移对话框处理状态
    fun getMigrationHandled(): Boolean {
        return prefs.getBoolean(KEY_MIGRATION_HANDLED, false)
    }

    fun setMigrationHandled(handled: Boolean) {
        prefs.edit().putBoolean(KEY_MIGRATION_HANDLED, handled).apply()
    }
    
    // ==================== 云端API配置方法 ====================
    
    /**
     * 从 assets 加载 API 配置
     * @return ApiConfig 配置对象
     */
    fun loadApiConfig(): ApiConfig {
        return try {
            val jsonStr = context.assets.open("api_config.json").bufferedReader().use { it.readText() }
            val config = Json { ignoreUnknownKeys = true }.decodeFromString<ApiConfig>(jsonStr)
            
            // 保存到 SharedPreferences
            prefs.edit().apply {
                putString(KEY_AI_API_ENDPOINT, config.aiApiEndpoint)
                putString(KEY_PRESET_API_ENDPOINT, config.presetApiEndpoint)
                putString(KEY_AUTH_API_ENDPOINT, config.authApiEndpoint)
                putString(KEY_API_VERSION, config.apiVersion)
                putBoolean(KEY_API_CONFIG_LOADED, true)
            }.apply()
            
            config
        } catch (e: Exception) {
            // 返回默认配置
            ApiConfig(
                aiApiEndpoint = DEFAULT_AI_API_ENDPOINT,
                presetApiEndpoint = DEFAULT_PRESET_API_ENDPOINT,
                authApiEndpoint = DEFAULT_AUTH_API_ENDPOINT,
                apiVersion = "v1"
            )
        }
    }
    
    /**
     * 验证 API 密钥是否有效
     * @return Boolean 密钥是否有效
     */
    fun validateApiKey(): Boolean {
        val apiKey = cloudApiKey ?: return false
        // 基本验证：密钥非空且长度合理
        return apiKey.isNotBlank() && apiKey.length >= 16
    }
    
    /**
     * 验证 API 密钥格式
     * @param key 要验证的密钥
     * @return Boolean 密钥格式是否有效
     */
    fun validateApiKeyFormat(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        // 基本格式验证：长度至少16字符，只包含字母、数字、连字符和下划线
        return key.length >= 16 && key.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }
    
    /**
     * 获取完整的 API URL
     * @param endpoint 端点路径
     * @param type API类型 (ai, preset, auth)
     * @return 完整的URL
     */
    fun getFullApiUrl(endpoint: String, type: String = "ai"): String {
        val baseUrl = when (type) {
            "preset" -> presetApiEndpoint
            "auth" -> authApiEndpoint
            else -> aiApiEndpoint
        }
        return "${baseUrl.trimEnd('/')}/${apiVersion}/${endpoint.trimStart('/')}"
    }
    
    /**
     * 设置自定义 API 端点
     * @param aiEndpoint AI API端点
     * @param presetEndpoint 预设API端点
     * @param authEndpoint 认证API端点
     */
    fun setCustomApiEndpoints(
        aiEndpoint: String? = null,
        presetEndpoint: String? = null,
        authEndpoint: String? = null
    ) {
        prefs.edit().apply {
            aiEndpoint?.let { putString(KEY_AI_API_ENDPOINT, it) }
            presetEndpoint?.let { putString(KEY_PRESET_API_ENDPOINT, it) }
            authEndpoint?.let { putString(KEY_AUTH_API_ENDPOINT, it) }
        }.apply()
    }
    
    /**
     * 重置 API 端点为默认值
     */
    fun resetApiEndpoints() {
        prefs.edit().apply {
            putString(KEY_AI_API_ENDPOINT, DEFAULT_AI_API_ENDPOINT)
            putString(KEY_PRESET_API_ENDPOINT, DEFAULT_PRESET_API_ENDPOINT)
            putString(KEY_AUTH_API_ENDPOINT, DEFAULT_AUTH_API_ENDPOINT)
        }.apply()
    }

    companion object {
        // 云端API默认端点
        private const val DEFAULT_AI_API_ENDPOINT = "https://api.omaster.app/ai"
        private const val DEFAULT_PRESET_API_ENDPOINT = "https://api.omaster.app/presets"
        private const val DEFAULT_AUTH_API_ENDPOINT = "https://api.omaster.app/auth"
        
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
        private const val KEY_CLOUD_API_KEY = "cloud_api_key"
        private const val KEY_AI_SCENE_ENABLED = "ai_scene_enabled"
        private const val KEY_LAST_WATERMARK_TEMPLATE = "last_watermark_template"
        private const val KEY_AI_FINE_TUNE_ENABLED = "ai_fine_tune_enabled"
        private const val KEY_SMART_OPTIMIZE_ENABLED = "smart_optimize_enabled"
        private const val KEY_WATERMARK_EDITOR_ENABLED = "watermark_editor_enabled"
        private const val KEY_HASSELBLAD_COLOR_ENABLED = "hasselblad_color_enabled"
        private const val KEY_CUSTOM_DEVICE_MODEL = "custom_device_model"
        private const val KEY_PRESET_VERSION_MAP = "preset_version_map"
        private const val KEY_FAVORITE_PRESET_IDS = "favorite_preset_ids"
        private const val KEY_PINNED_PRESET_IDS = "pinned_preset_ids"
        private const val KEY_MANUALLY_MODIFIED_PARAMS = "manually_modified_params"
        private const val KEY_CUSTOM_QUICK_PRESETS = "custom_quick_presets"
        
        // 云端API配置 Key
        private const val KEY_API_CONFIG_LOADED = "api_config_loaded"
        private const val KEY_AI_API_ENDPOINT = "ai_api_endpoint"
        private const val KEY_PRESET_API_ENDPOINT = "preset_api_endpoint"
        private const val KEY_AUTH_API_ENDPOINT = "auth_api_endpoint"
        private const val KEY_API_VERSION = "api_version"

        // 应用预设参数 Key
        private const val KEY_APPLIED_SATURATION = "applied_saturation"
        private const val KEY_APPLIED_CONTRAST = "applied_contrast"
        private const val KEY_APPLIED_WARMTH = "applied_warmth"
        private const val KEY_APPLIED_SHARPNESS = "applied_sharpness"
        private const val KEY_APPLIED_CLARITY = "applied_clarity"
        private const val KEY_APPLIED_BRIGHTNESS = "applied_brightness"
        private const val KEY_HAS_APPLIED_PRESET = "has_applied_preset"
        private const val KEY_MIGRATION_HANDLED = "migration_handled"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
