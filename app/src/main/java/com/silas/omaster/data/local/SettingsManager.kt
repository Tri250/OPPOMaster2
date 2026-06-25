package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.util.SecurityCrypto
import com.silas.omaster.util.UrlConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.jvm.JvmName

// DataStore 扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

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
 * API配置数据类
 */
@Serializable
data class ApiConfig(
    val aiApiEndpoint: String = "",
    val presetApiEndpoint: String = "",
    val authApiEndpoint: String = "",
    val apiVersion: String = "v1"
)

/**
 * 设置管理器 - 使用 DataStore 替代 SharedPreferences
 * 
 * 优势：
 * 1. 异步 IO，避免主线程 ANR
 * 2. 类型安全的数据存储
 * 3. 支持 Flow 观察，自动响应变化
 * 4. 数据一致性保证
 * 
 * 向后兼容：
 * - 首次启动时自动迁移 SharedPreferences 数据到 DataStore
 * - 迁移完成后清除旧数据
 */
class SettingsManager private constructor(private val context: Context) {
    
    // 内存缓存层：避免在主线程调用 runBlocking 读取 DataStore 时因磁盘 I/O 导致 ANR
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Any>()
    
    // 旧版 SharedPreferences（仅用于迁移）
    private val legacyPrefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // 迁移标记
    @Volatile
    private var migrationCompleted: Boolean = false

    // 缓存预加载完成标记：用于检测 getDataSync 在缓存未就绪时的回退行为
    @Volatile
    private var cachePreloaded: Boolean = false

    // 销毁标记：防止 shutdown 后继续操作
    @Volatile
    private var isDestroyed: Boolean = false
    
    // 协程作用域用于异步迁移和 DataStore 写入
    private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // ==================== 云端API配置 ====================
    
    // 云端AI推理API端点
    val aiApiEndpoint: String
        get() = getDataSync(KEY_AI_API_ENDPOINT, DEFAULT_AI_API_ENDPOINT)
    
    // 预设同步API端点
    val presetApiEndpoint: String
        get() = getDataSync(KEY_PRESET_API_ENDPOINT, DEFAULT_PRESET_API_ENDPOINT)
    
    // 用户认证API端点
    val authApiEndpoint: String
        get() = getDataSync(KEY_AUTH_API_ENDPOINT, DEFAULT_AUTH_API_ENDPOINT)
    
    // API版本
    val apiVersion: String
        get() = getDataSync(KEY_API_VERSION, "v1")
    
    // 是否已加载API配置
    val isApiConfigLoaded: Boolean
        get() = getDataSync(KEY_API_CONFIG_LOADED, false)

    // 震动开关（使用 Flow）
    private val _isVibrationEnabledFlow = MutableStateFlow(true)
    val isVibrationEnabledFlow: StateFlow<Boolean> = _isVibrationEnabledFlow.asStateFlow()
    
    var isVibrationEnabled: Boolean
        get() = _isVibrationEnabledFlow.value
        set(value) {
            setDataSync(KEY_VIBRATION_ENABLED, value)
            _isVibrationEnabledFlow.value = value
        }

    // 主题设置（使用 Flow）
    private val _themeFlow: MutableStateFlow<BrandTheme>
    val themeFlow: StateFlow<BrandTheme>

    // 深色模式（使用 Flow）
    private val _darkModeFlow: MutableStateFlow<DarkMode>
    val darkModeFlow: StateFlow<DarkMode>

    init {
        // 异步迁移：避免在主线程阻塞导致 ANR
        // 迁移完成后自动预加载缓存，确保后续读取零阻塞
        settingsScope.launch {
            try {
                migrateFromSharedPreferences()
                preloadCache()
                // 启动 DataStore 外部变更监听，解决多进程写入导致缓存过期的问题
                startCacheObserver()
            } catch (e: Exception) {
                android.util.Log.w("SettingsManager", "异步迁移失败", e)
            }
        }
        
        // 初始化 Flow 使用默认值，迁移完成后通过 preloadCache 更新缓存
        val themeId = getDataSync(KEY_THEME_ID, BrandTheme.Hasselblad.id)
        _themeFlow = MutableStateFlow(BrandTheme.fromId(themeId))
        themeFlow = _themeFlow.asStateFlow()

        val darkModeValue = getDataSync(KEY_DARK_MODE, DarkMode.DARK.name)
        _darkModeFlow = MutableStateFlow(safeValueOf(darkModeValue, DarkMode.DARK))
        darkModeFlow = _darkModeFlow.asStateFlow()
        
        // 初始化震动设置
        _isVibrationEnabledFlow.value = getDataSync(KEY_VIBRATION_ENABLED, true)
    }

    var currentTheme: BrandTheme
        get() = _themeFlow.value
        set(value) {
            setDataSync(KEY_THEME_ID, value.id)
            _themeFlow.value = value
        }

    // 悬浮窗透明度 (30-70%，默认56%)
    var floatingWindowOpacity: Int
        get() = getDataSync(KEY_FLOATING_WINDOW_OPACITY, 56)
        set(value) {
            setDataSync(KEY_FLOATING_WINDOW_OPACITY, value.coerceIn(30, 70))
        }

    // 默认启动 Tab (0=发现, 1=收藏, 2=哈苏, 3=上新，默认0)
    var defaultStartTab: Int
        get() = getDataSync(KEY_DEFAULT_START_TAB, 0)
        set(value) {
            setDataSync(KEY_DEFAULT_START_TAB, value.coerceIn(0, 3))
        }

    // 更新渠道（默认 Gitee）
    var updateChannel: UpdateChannel
        get() = safeValueOf(getDataSync(KEY_UPDATE_CHANNEL, UpdateChannel.GITEE.name), UpdateChannel.GITEE)
        set(value) {
            setDataSync(KEY_UPDATE_CHANNEL, value.name)
        }

    // 友盟统计开关（默认开启，因为用户首次已同意隐私政策）
    var isAnalyticsEnabled: Boolean
        get() = getDataSync(KEY_ANALYTICS_ENABLED, true)
        set(value) {
            setDataSync(KEY_ANALYTICS_ENABLED, value)
        }

    // ==================== 新增功能 ====================

    // 深色模式设置
    var darkMode: DarkMode
        get() = safeValueOf(getDataSync(KEY_DARK_MODE, DarkMode.DARK.name), DarkMode.DARK)
        set(value) {
            setDataSync(KEY_DARK_MODE, value.name)
            _darkModeFlow.value = value
        }

    // 上次使用的水印模板
    var lastWatermarkTemplate: String?
        get() = getDataSyncOrNull(KEY_LAST_WATERMARK_TEMPLATE)
        set(value) {
            if (value != null) {
                setDataSync(KEY_LAST_WATERMARK_TEMPLATE, value)
            } else {
                removeDataSync(KEY_LAST_WATERMARK_TEMPLATE)
            }
        }

    // 用户ID（用于统计与分析上报）
    var userId: String?
        get() = getDataSyncOrNull(KEY_USER_ID)
        set(value) {
            if (value != null) {
                setDataSync(KEY_USER_ID, value)
            } else {
                removeDataSync(KEY_USER_ID)
            }
        }

    // 云端API密钥（用于云端AI推理）- 使用 Android Keystore AES/GCM 加密存储
    var cloudApiKey: String?
        get() {
            val encrypted = getDataSyncOrNull(KEY_CLOUD_API_KEY) ?: return null
            // 解密：如果解密失败，尝试作为明文兼容旧版本数据，并自动迁移为加密存储
            return SecurityCrypto.decrypt(encrypted) ?: run {
                // 向后兼容：旧版本可能存储了明文 API Key
                if (encrypted.isNotBlank() && encrypted.length >= 16) {
                    android.util.Log.w("SettingsManager", "检测到明文 API Key，正在自动迁移为加密存储")
                    val plainKey = encrypted
                    // 异步加密存储
                    settingsScope.launch {
                        SecurityCrypto.encrypt(plainKey)?.let { encryptedKey ->
                            try {
                                val ctx = context
                                ctx.dataStore.edit { prefs ->
                                    prefs[KEY_CLOUD_API_KEY] = encryptedKey
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("SettingsManager", "API Key 加密迁移失败", e)
                            }
                        }
                    }
                    plainKey
                } else {
                    null
                }
            }
        }
        set(value) {
            if (value != null) {
                val encrypted = SecurityCrypto.encrypt(value)
                if (encrypted != null) {
                    setDataSync(KEY_CLOUD_API_KEY, encrypted)
                } else {
                    android.util.Log.w("SettingsManager", "API Key 加密失败，回退到明文存储")
                    setDataSync(KEY_CLOUD_API_KEY, value)
                }
            } else {
                removeDataSync(KEY_CLOUD_API_KEY)
            }
        }

    // AI 微调开关
    // v1.6.0 默认开启：所有核心功能默认启用
    var isAIFineTuneEnabled: Boolean
        get() = getDataSync(KEY_AI_FINE_TUNE_ENABLED, true)
        set(value) {
            setDataSync(KEY_AI_FINE_TUNE_ENABLED, value)
        }

    // 智能优化开关
    // v1.6.0 默认开启：所有核心功能默认启用
    var isSmartOptimizeEnabled: Boolean
        get() = getDataSync(KEY_SMART_OPTIMIZE_ENABLED, true)
        set(value) {
            setDataSync(KEY_SMART_OPTIMIZE_ENABLED, value)
        }

    // 水印编辑器开关
    var isWatermarkEditorEnabled: Boolean
        get() = getDataSync(KEY_WATERMARK_EDITOR_ENABLED, true)
        set(value) {
            setDataSync(KEY_WATERMARK_EDITOR_ENABLED, value)
        }

    // 哈苏色彩科学开关
    // v1.6.0 默认开启：所有核心功能默认启用
    var isHasselbladColorEnabled: Boolean
        get() = getDataSync(KEY_HASSELBLAD_COLOR_ENABLED, true)
        set(value) {
            setDataSync(KEY_HASSELBLAD_COLOR_ENABLED, value)
        }

    // 自定义设备型号（WM-003）
    var customDeviceModel: String
        get() = getDataSync(KEY_CUSTOM_DEVICE_MODEL, "")
        set(value) {
            setDataSync(KEY_CUSTOM_DEVICE_MODEL, value)
        }

    // 预设版本映射（JSON 字符串），用于云端增量更新对比
    var presetVersionMapJson: String
        get() = getDataSync(KEY_PRESET_VERSION_MAP, "")
        set(value) {
            setDataSync(KEY_PRESET_VERSION_MAP, value)
        }

    // 预设源配置（JSON 字符串），用于持久化用户自定义的预设源列表
    var presetSourcesJson: String
        get() = getDataSync(KEY_PRESET_SOURCES_JSON, "")
        set(value) {
            setDataSync(KEY_PRESET_SOURCES_JSON, value)
        }

    // 收藏的预设ID列表（PM-003）
    var favoritePresetIds: List<String>
        get() = getDataSetSync(KEY_FAVORITE_PRESET_IDS, emptySet()).toList()
        set(value) {
            setDataSetSync(KEY_FAVORITE_PRESET_IDS, value.toSet())
        }

    // 置顶的预设ID列表（PM-008）
    var pinnedPresetIds: List<String>
        get() = getDataSetSync(KEY_PINNED_PRESET_IDS, emptySet()).toList()
        set(value) {
            setDataSetSync(KEY_PINNED_PRESET_IDS, value.toSet())
        }

    // 搜索历史（最多20条）
    var searchHistory: List<String>
        get() = getDataSetSync(KEY_SEARCH_HISTORY, emptySet()).toList().take(20)
        set(value) {
            setDataSetSync(KEY_SEARCH_HISTORY, value.take(20).toSet())
        }

    // 手动修改的参数（PP-005）
    var manuallyModifiedParams: List<String>
        get() = getDataSetSync(KEY_MANUALLY_MODIFIED_PARAMS, emptySet()).toList()
        set(value) {
            setDataSetSync(KEY_MANUALLY_MODIFIED_PARAMS, value.toSet())
        }

    // 自定义快捷档位（PP-004）
    // 注意：需要JSON序列化存储
    var customQuickPresets: Map<String, Map<String, Int>>
        get() {
            val jsonStr = getDataSyncOrNull(KEY_CUSTOM_QUICK_PRESETS) ?: return emptyMap()
            return try {
                Json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) {
            val jsonStr = Json.encodeToString(value)
            setDataSync(KEY_CUSTOM_QUICK_PRESETS, jsonStr)
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
        setDataSync(KEY_APPLIED_SATURATION, saturation)
        setDataSync(KEY_APPLIED_CONTRAST, contrast)
        setDataSync(KEY_APPLIED_WARMTH, warmth)
        setDataSync(KEY_APPLIED_SHARPNESS, sharpness)
        setDataSync(KEY_APPLIED_CLARITY, clarity)
        setDataSync(KEY_APPLIED_BRIGHTNESS, brightness)
        setDataSync(KEY_HAS_APPLIED_PRESET, true)
    }

    // 获取已应用的预设参数
    fun getAppliedPresetParams(): Map<String, Int> {
        return mapOf(
            "saturation" to getDataSync(KEY_APPLIED_SATURATION, 0),
            "contrast" to getDataSync(KEY_APPLIED_CONTRAST, 0),
            "warmth" to getDataSync(KEY_APPLIED_WARMTH, 0),
            "sharpness" to getDataSync(KEY_APPLIED_SHARPNESS, 0),
            "clarity" to getDataSync(KEY_APPLIED_CLARITY, 0),
            "brightness" to getDataSync(KEY_APPLIED_BRIGHTNESS, 0)
        )
    }

    // 是否已应用预设
    fun hasAppliedPreset(): Boolean {
        return getDataSync(KEY_HAS_APPLIED_PRESET, false)
    }

    // 清除已应用的预设
    fun clearAppliedPreset() {
        removeDataSync(KEY_APPLIED_SATURATION)
        removeDataSync(KEY_APPLIED_CONTRAST)
        removeDataSync(KEY_APPLIED_WARMTH)
        removeDataSync(KEY_APPLIED_SHARPNESS)
        removeDataSync(KEY_APPLIED_CLARITY)
        removeDataSync(KEY_APPLIED_BRIGHTNESS)
        setDataSync(KEY_HAS_APPLIED_PRESET, false)
    }

    // 应用相机参数（参数精细调节功能）
    fun applyCameraParams(
        iso: Int = 100,
        shutterSpeed: Float = 125f,
        aperture: Float = 2.8f,
        whiteBalance: Int = 5500,
        focalLength: Int = 23,
        exposureCompensation: Float = 0f
    ) {
        setDataSync(KEY_APPLIED_ISO, iso)
        setDataSync(KEY_APPLIED_SHUTTER_SPEED, shutterSpeed)
        setDataSync(KEY_APPLIED_APERTURE, aperture)
        setDataSync(KEY_APPLIED_WHITE_BALANCE, whiteBalance)
        setDataSync(KEY_APPLIED_FOCAL_LENGTH, focalLength)
        setDataSync(KEY_APPLIED_EXPOSURE_COMPENSATION, exposureCompensation)
        setDataSync(KEY_HAS_APPLIED_CAMERA_PARAMS, true)
    }

    // 获取已应用的相机参数
    fun getAppliedCameraParams(): Map<String, Any> {
        return mapOf(
            "iso" to getDataSync(KEY_APPLIED_ISO, 100),
            "shutterSpeed" to getDataSync(KEY_APPLIED_SHUTTER_SPEED, 125f),
            "aperture" to getDataSync(KEY_APPLIED_APERTURE, 2.8f),
            "whiteBalance" to getDataSync(KEY_APPLIED_WHITE_BALANCE, 5500),
            "focalLength" to getDataSync(KEY_APPLIED_FOCAL_LENGTH, 23),
            "exposureCompensation" to getDataSync(KEY_APPLIED_EXPOSURE_COMPENSATION, 0f)
        )
    }

    // 是否已应用相机参数
    fun hasAppliedCameraParams(): Boolean {
        return getDataSync(KEY_HAS_APPLIED_CAMERA_PARAMS, false)
    }

    // 清除已应用的相机参数
    fun clearAppliedCameraParams() {
        removeDataSync(KEY_APPLIED_ISO)
        removeDataSync(KEY_APPLIED_SHUTTER_SPEED)
        removeDataSync(KEY_APPLIED_APERTURE)
        removeDataSync(KEY_APPLIED_WHITE_BALANCE)
        removeDataSync(KEY_APPLIED_FOCAL_LENGTH)
        removeDataSync(KEY_APPLIED_EXPOSURE_COMPENSATION)
        setDataSync(KEY_HAS_APPLIED_CAMERA_PARAMS, false)
    }

    // 迁移对话框处理状态
    fun getMigrationHandled(): Boolean {
        return getDataSync(KEY_MIGRATION_HANDLED, false)
    }

    fun setMigrationHandled(handled: Boolean) {
        setDataSync(KEY_MIGRATION_HANDLED, handled)
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
            
            // 保存到 DataStore
            setDataSync(KEY_AI_API_ENDPOINT, config.aiApiEndpoint)
            setDataSync(KEY_PRESET_API_ENDPOINT, config.presetApiEndpoint)
            setDataSync(KEY_AUTH_API_ENDPOINT, config.authApiEndpoint)
            setDataSync(KEY_API_VERSION, config.apiVersion)
            setDataSync(KEY_API_CONFIG_LOADED, true)
            
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
        return validateApiKeyFormat(cloudApiKey)
    }

    /**
     * 验证 API 密钥格式
     * @param key 要验证的密钥
     * @return Boolean 密钥格式是否有效
     */
    fun validateApiKeyFormat(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        // 基本格式验证：长度至少16字符，只包含字母、数字、连字符和下划线
        if (key.length < 16 || !key.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            return false
        }
        // 拒绝占位符/demo密钥，避免误用默认密钥发起无效请求
        val lowerKey = key.lowercase()
        return lowerKey !in setOf("demo_key", "your_api_key_here", "your_api_key", "api_key", "test_key")
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
        aiEndpoint?.let { setDataSync(KEY_AI_API_ENDPOINT, it) }
        presetEndpoint?.let { setDataSync(KEY_PRESET_API_ENDPOINT, it) }
        authEndpoint?.let { setDataSync(KEY_AUTH_API_ENDPOINT, it) }
    }
    
    /**
     * 重置 API 端点为默认值
     */
    fun resetApiEndpoints() {
        setDataSync(KEY_AI_API_ENDPOINT, DEFAULT_AI_API_ENDPOINT)
        setDataSync(KEY_PRESET_API_ENDPOINT, DEFAULT_PRESET_API_ENDPOINT)
        setDataSync(KEY_AUTH_API_ENDPOINT, DEFAULT_AUTH_API_ENDPOINT)
    }

    // ==================== DataStore 操作方法 ====================

    /**
     * 同步获取 String 数据（纯内存缓存读取，零阻塞）
     *
     * 实现说明：
     * - 优先从内存缓存读取；命中则直接返回
     * - 缓存未命中时返回 defaultValue，避免任何主线程阻塞
     * - 真实值由 init{} / preloadCache() / setDataSync() 异步填充到缓存
     */
    private fun getDataSync(key: Preferences.Key<String>, defaultValue: String): String {
        cache[key.name]?.let { return it as String }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }

    private fun getDataSyncOrNull(key: Preferences.Key<String>): String? {
        cache[key.name]?.let { return it as String? }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回 null: ${key.name}")
        }
        return null
    }

    private fun getDataSync(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
        cache[key.name]?.let { return it as Boolean }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }

    private fun getDataSync(key: Preferences.Key<Int>, defaultValue: Int): Int {
        cache[key.name]?.let { return it as Int }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }

    private fun getDataSync(key: Preferences.Key<Long>, defaultValue: Long): Long {
        cache[key.name]?.let { return it as Long }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }

    private fun getDataSync(key: Preferences.Key<Float>, defaultValue: Float): Float {
        cache[key.name]?.let { return it as Float }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }

    private fun getDataSetSync(key: Preferences.Key<Set<String>>, defaultValue: Set<String>): Set<String> {
        cache[key.name]?.let { cachedValue ->
            // 类型安全检查：确保缓存的集合元素都是 String 类型
            if (cachedValue is Set<*>) {
                val safeSet = cachedValue.filterIsInstance<String>().toSet()
                return safeSet
            }
        }
        if (!cachePreloaded) {
            android.util.Log.w("SettingsManager", "缓存未就绪，返回默认值: ${key.name}=$defaultValue")
        }
        return defaultValue
    }
    
    /**
     * 同步设置数据（先更新缓存再异步写入 DataStore，避免后续读取时阻塞）
     * 写入操作使用协程异步执行，不阻塞调用线程
     */
    private fun setDataSync(key: Preferences.Key<String>, value: String) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Boolean>, value: Boolean) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Int>, value: Int) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Long>, value: Long) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }

    private fun setDataSync(key: Preferences.Key<Float>, value: Float) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }
    
    private fun setDataSetSync(key: Preferences.Key<Set<String>>, value: Set<String>) {
        if (isDestroyed) return
        cache[key.name] = value
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs[key] = value }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "写入设置超时/失败: ${key.name}", e)
            }
        }
    }
    
    /**
     * 同步删除数据（清除缓存 + 异步删除 DataStore 条目）
     */
    @JvmName("removeDataSyncString")
    private fun removeDataSync(key: Preferences.Key<String>) {
        if (isDestroyed) return
        cache.remove(key.name)
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs.remove(key) }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "删除设置超时/失败: ${key.name}", e)
            }
        }
    }

    @JvmName("removeDataSyncInt")
    private fun removeDataSync(key: Preferences.Key<Int>) {
        if (isDestroyed) return
        cache.remove(key.name)
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs.remove(key) }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "删除设置超时/失败: ${key.name}", e)
            }
        }
    }

    @JvmName("removeDataSyncBoolean")
    private fun removeDataSync(key: Preferences.Key<Boolean>) {
        if (isDestroyed) return
        cache.remove(key.name)
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs.remove(key) }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "删除设置超时/失败: ${key.name}", e)
            }
        }
    }

    @JvmName("removeDataSyncFloat")
    private fun removeDataSync(key: Preferences.Key<Float>) {
        if (isDestroyed) return
        cache.remove(key.name)
        val ctx = context
        settingsScope.launch {
            try {
                withTimeout(DATASTORE_TIMEOUT_MS) {
                    ctx.dataStore.edit { prefs -> prefs.remove(key) }
                }
            } catch (e: Throwable) {
                android.util.Log.w("SettingsManager", "删除设置超时/失败: ${key.name}", e)
            }
        }
    }

    /**
     * 预加载缓存：在应用启动时（如 Application.onCreate 的协程中）调用，
     * 将所有设置项从 DataStore 一次性读入内存，避免后续主线程读取时阻塞。
     */
    suspend fun preloadCache() {
        try {
            context.dataStore.data.first().let { prefs ->
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            }
            cachePreloaded = true
        } catch (e: Exception) {
            android.util.Log.w("SettingsManager", "预加载缓存失败，将在首次访问时逐项加载", e)
        }
    }

    /**
     * 启动 DataStore 外部变更监听器
     * 当其他进程（如 app upgrade 后的新进程）修改 DataStore 时，
     * 自动同步缓存，避免缓存过期导致读取到旧值。
     */
    private fun startCacheObserver() {
        val ctx = context
        settingsScope.launch {
            try {
                ctx.dataStore.data.collect { prefs ->
                    prefs.asMap().forEach { (key, value) ->
                        cache[key.name] = value
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SettingsManager", "DataStore 变更监听异常", e)
            }
        }
    }

    /**
     * 销毁 SettingsManager，取消所有协程并清理资源。
     * 应在 Application.onTerminate() 或进程退出前调用。
     */
    fun destroy() {
        isDestroyed = true
        settingsScope.cancel()
        android.util.Log.i("SettingsManager", "SettingsManager 已销毁")
    }

    // ==================== SharedPreferences 迁移 ====================
    
    /**
     * 从 SharedPreferences 迁移数据到 DataStore
     * 仅在首次启动时执行一次
     */
    private suspend fun migrateFromSharedPreferences() {
        // 检查 SharedPreferences 文件是否存在（首次安装时无此文件，无需迁移）
        val prefsFile = File(context.applicationContext.filesDir.parent + "/shared_prefs/app_settings.xml")
        if (!prefsFile.exists()) {
            android.util.Log.i("SettingsManager", "SharedPreferences 文件不存在，跳过迁移（首次安装）")
            val ctx = context
            ctx.dataStore.edit { prefs ->
                prefs[KEY_MIGRATION_COMPLETED] = true
            }
            migrationCompleted = true
            return
        }

        // 检查是否已迁移
        val alreadyMigrated = context.dataStore.data.map { prefs ->
            prefs[KEY_MIGRATION_COMPLETED] ?: false
        }.first()
        
        if (alreadyMigrated) {
            migrationCompleted = true
            return
        }
        
        // 执行迁移
        try {
            val legacyKeys = listOf(
                KEY_VIBRATION_ENABLED, KEY_THEME_ID, KEY_FLOATING_WINDOW_OPACITY,
                KEY_DEFAULT_START_TAB, KEY_UPDATE_CHANNEL, KEY_ANALYTICS_ENABLED,
                KEY_DARK_MODE, KEY_USER_ID, KEY_CLOUD_API_KEY, KEY_AI_SCENE_ENABLED,
                KEY_LAST_WATERMARK_TEMPLATE, KEY_AI_FINE_TUNE_ENABLED, KEY_SMART_OPTIMIZE_ENABLED,
                KEY_WATERMARK_EDITOR_ENABLED, KEY_HASSELBLAD_COLOR_ENABLED, KEY_CUSTOM_DEVICE_MODEL,
                KEY_PRESET_VERSION_MAP, KEY_FAVORITE_PRESET_IDS, KEY_PINNED_PRESET_IDS,
                KEY_MANUALLY_MODIFIED_PARAMS, KEY_CUSTOM_QUICK_PRESETS, KEY_API_CONFIG_LOADED,
                KEY_AI_API_ENDPOINT, KEY_PRESET_API_ENDPOINT, KEY_AUTH_API_ENDPOINT, KEY_API_VERSION,
                KEY_APPLIED_SATURATION, KEY_APPLIED_CONTRAST, KEY_APPLIED_WARMTH,
                KEY_APPLIED_SHARPNESS, KEY_APPLIED_CLARITY, KEY_APPLIED_BRIGHTNESS,
                KEY_HAS_APPLIED_PRESET, KEY_MIGRATION_HANDLED, KEY_SEARCH_HISTORY, KEY_PRESET_SOURCES_JSON
            )
            
            val ctx = context
            ctx.dataStore.edit { prefs ->
                // 迁移所有数据
                legacyPrefs.all.forEach { (key, value) ->
                    when (value) {
                        is String -> prefs[stringPreferencesKey(key)] = value
                        is Boolean -> prefs[booleanPreferencesKey(key)] = value
                        is Int -> prefs[intPreferencesKey(key)] = value
                        is Long -> prefs[longPreferencesKey(key)] = value
                        is Set<*> -> {
                            // 类型安全检查：确保 Set 元素都是 String 类型
                            val safeSet = value.filterIsInstance<String>().toSet()
                            prefs[stringSetPreferencesKey(key)] = safeSet
                        }
                    }
                }
                
                // 标记迁移完成
                prefs[KEY_MIGRATION_COMPLETED] = true
            }
            
            // 清除旧 SharedPreferences（可选，保留作为备份）
            // legacyPrefs.edit().clear().apply()
            
            migrationCompleted = true
            android.util.Log.i("SettingsManager", "SharedPreferences → DataStore 迁移完成")
        } catch (e: Exception) {
            android.util.Log.e("SettingsManager", "迁移失败，继续使用默认值", e)
        }
    }

    companion object {
        // DataStore 读写超时时间（毫秒），防止磁盘 I/O 异常导致 ANR
        private const val DATASTORE_TIMEOUT_MS = 500L

        // 云端API默认端点
        private val DEFAULT_AI_API_ENDPOINT = UrlConstants.API_AI_ENDPOINT
        private val DEFAULT_PRESET_API_ENDPOINT = UrlConstants.API_PRESET_ENDPOINT
        private val DEFAULT_AUTH_API_ENDPOINT = UrlConstants.API_AUTH_ENDPOINT
        
        // DataStore Preferences Keys
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_THEME_ID = stringPreferencesKey("theme_id")
        private val KEY_FLOATING_WINDOW_OPACITY = intPreferencesKey("floating_window_opacity")
        private val KEY_DEFAULT_START_TAB = intPreferencesKey("default_start_tab")
        private val KEY_UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")

        // 新增功能 Key
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        private val KEY_AI_SCENE_ENABLED = booleanPreferencesKey("ai_scene_enabled")
        private val KEY_LAST_WATERMARK_TEMPLATE = stringPreferencesKey("last_watermark_template")
        private val KEY_AI_FINE_TUNE_ENABLED = booleanPreferencesKey("ai_fine_tune_enabled")
        private val KEY_SMART_OPTIMIZE_ENABLED = booleanPreferencesKey("smart_optimize_enabled")
        private val KEY_WATERMARK_EDITOR_ENABLED = booleanPreferencesKey("watermark_editor_enabled")
        private val KEY_HASSELBLAD_COLOR_ENABLED = booleanPreferencesKey("hasselblad_color_enabled")
        private val KEY_CUSTOM_DEVICE_MODEL = stringPreferencesKey("custom_device_model")
        private val KEY_PRESET_VERSION_MAP = stringPreferencesKey("preset_version_map")
        private val KEY_FAVORITE_PRESET_IDS = stringSetPreferencesKey("favorite_preset_ids")
        private val KEY_PINNED_PRESET_IDS = stringSetPreferencesKey("pinned_preset_ids")
        private val KEY_MANUALLY_MODIFIED_PARAMS = stringSetPreferencesKey("manually_modified_params")
        private val KEY_CUSTOM_QUICK_PRESETS = stringPreferencesKey("custom_quick_presets")
        
        // 云端API配置 Key
        private val KEY_API_CONFIG_LOADED = booleanPreferencesKey("api_config_loaded")
        private val KEY_AI_API_ENDPOINT = stringPreferencesKey("ai_api_endpoint")
        private val KEY_PRESET_API_ENDPOINT = stringPreferencesKey("preset_api_endpoint")
        private val KEY_AUTH_API_ENDPOINT = stringPreferencesKey("auth_api_endpoint")
        private val KEY_API_VERSION = stringPreferencesKey("api_version")

        // 应用预设参数 Key
        private val KEY_APPLIED_SATURATION = intPreferencesKey("applied_saturation")
        private val KEY_APPLIED_CONTRAST = intPreferencesKey("applied_contrast")
        private val KEY_APPLIED_WARMTH = intPreferencesKey("applied_warmth")
        private val KEY_APPLIED_SHARPNESS = intPreferencesKey("applied_sharpness")
        private val KEY_APPLIED_CLARITY = intPreferencesKey("applied_clarity")
        private val KEY_APPLIED_BRIGHTNESS = intPreferencesKey("applied_brightness")
        private val KEY_HAS_APPLIED_PRESET = booleanPreferencesKey("has_applied_preset")

        // 应用相机参数 Key
        private val KEY_APPLIED_ISO = intPreferencesKey("applied_iso")
        private val KEY_APPLIED_SHUTTER_SPEED = floatPreferencesKey("applied_shutter_speed")
        private val KEY_APPLIED_APERTURE = floatPreferencesKey("applied_aperture")
        private val KEY_APPLIED_WHITE_BALANCE = intPreferencesKey("applied_white_balance")
        private val KEY_APPLIED_FOCAL_LENGTH = intPreferencesKey("applied_focal_length")
        private val KEY_APPLIED_EXPOSURE_COMPENSATION = floatPreferencesKey("applied_exposure_compensation")
        private val KEY_HAS_APPLIED_CAMERA_PARAMS = booleanPreferencesKey("has_applied_camera_params")

        private val KEY_MIGRATION_HANDLED = booleanPreferencesKey("migration_handled")

        // 搜索历史 Key
        private val KEY_SEARCH_HISTORY = stringSetPreferencesKey("search_history")

        // 预设源配置 JSON
        private val KEY_PRESET_SOURCES_JSON = stringPreferencesKey("preset_sources_json")

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 关闭 SettingsManager 实例，取消所有协程并清理资源。
         * 应在 Application.onTerminate() 或进程退出前调用。
         */
        fun shutdown() {
            synchronized(this) {
                instance?.destroy()
                instance = null
            }
        }
    }
}