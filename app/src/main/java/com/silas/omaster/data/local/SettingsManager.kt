package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.util.UrlConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    private var migrationCompleted: Boolean = false
    
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
        // 首次启动时迁移旧数据
        runBlocking {
            migrateFromSharedPreferences()
        }
        
        // 初始化 Flow
        val themeId = getDataSync(KEY_THEME_ID, BrandTheme.Hasselblad.id)
        _themeFlow = MutableStateFlow(BrandTheme.fromId(themeId))
        themeFlow = _themeFlow.asStateFlow()

        val darkModeValue = getDataSync(KEY_DARK_MODE, DarkMode.SYSTEM.name)
        _darkModeFlow = MutableStateFlow(safeValueOf(darkModeValue, DarkMode.SYSTEM))
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
        get() = safeValueOf(getDataSync(KEY_DARK_MODE, DarkMode.SYSTEM.name), DarkMode.SYSTEM)
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

    // 云同步开关（默认开启）
    var isCloudSyncEnabled: Boolean
        get() = getDataSync(KEY_CLOUD_SYNC_ENABLED, true)
        set(value) {
            setDataSync(KEY_CLOUD_SYNC_ENABLED, value)
        }

    // 云端预设数据源 URL
    val cloudPresetUrls: Map<String, String>
        get() = UrlConstants.PRESET_SOURCE_URLS

    // 云同步状态
    var cloudSyncStatus: CloudSyncStatus
        get() = safeValueOf(getDataSync(KEY_CLOUD_SYNC_STATUS, CloudSyncStatus.DISABLED.name), CloudSyncStatus.DISABLED)
        set(value) {
            setDataSync(KEY_CLOUD_SYNC_STATUS, value.name)
        }

    // 最后同步时间
    var lastSyncTime: Long
        get() = getDataSync(KEY_LAST_SYNC_TIME, 0L)
        set(value) {
            setDataSync(KEY_LAST_SYNC_TIME, value)
        }

    // 用户ID（用于云同步）
    var userId: String?
        get() = getDataSyncOrNull(KEY_USER_ID)
        set(value) {
            if (value != null) {
                setDataSync(KEY_USER_ID, value)
            } else {
                removeDataSync(KEY_USER_ID)
            }
        }

    // 云端API密钥（用于云端AI推理）
    var cloudApiKey: String?
        get() = getDataSyncOrNull(KEY_CLOUD_API_KEY)
        set(value) {
            if (value != null) {
                setDataSync(KEY_CLOUD_API_KEY, value)
            } else {
                removeDataSync(KEY_CLOUD_API_KEY)
            }
        }

    // 哈苏之眼开关（带 StateFlow 支持）
    private val _isAISceneRecognitionEnabledFlow = MutableStateFlow(getDataSync(KEY_AI_SCENE_ENABLED, true))
    val isAISceneRecognitionEnabledFlow: StateFlow<Boolean> = _isAISceneRecognitionEnabledFlow.asStateFlow()

    var isAISceneRecognitionEnabled: Boolean
        get() = _isAISceneRecognitionEnabledFlow.value
        set(value) {
            setDataSync(KEY_AI_SCENE_ENABLED, value)
            _isAISceneRecognitionEnabledFlow.value = value
        }

    // AI 微调开关
    var isAIFineTuneEnabled: Boolean
        get() = getDataSync(KEY_AI_FINE_TUNE_ENABLED, true)
        set(value) {
            setDataSync(KEY_AI_FINE_TUNE_ENABLED, value)
        }

    // 智能优化开关
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
     * 同步获取 String 数据（带内存缓存，避免主线程 ANR）
     * 优先从缓存读取；缓存未命中时使用 runBlocking 读取 DataStore，
     * 若读取失败则返回默认值，成功后写入缓存。
     */
    private fun getDataSync(key: Preferences.Key<String>, defaultValue: String): String {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as String }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }
    
    private fun getDataSyncOrNull(key: Preferences.Key<String>): String? {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as String? }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] }.first()
            }
        } catch (e: Exception) {
            null
        }
        if (value != null) cache[key.name] = value
        return value
    }
    
    private fun getDataSync(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as Boolean }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }
    
    private fun getDataSync(key: Preferences.Key<Int>, defaultValue: Int): Int {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as Int }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }
    
    private fun getDataSync(key: Preferences.Key<Long>, defaultValue: Long): Long {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as Long }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }

    private fun getDataSync(key: Preferences.Key<Float>, defaultValue: Float): Float {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as Float }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }
    
    private fun getDataSetSync(key: Preferences.Key<Set<String>>, defaultValue: Set<String>): Set<String> {
        @Suppress("UNCHECKED_CAST")
        cache[key.name]?.let { return it as Set<String> }
        val value = try {
            runBlocking {
                context.dataStore.data.map { prefs -> prefs[key] ?: defaultValue }.first()
            }
        } catch (e: Exception) {
            defaultValue
        }
        cache[key.name] = value
        return value
    }
    
    /**
     * 同步设置数据（先更新缓存再写入 DataStore，避免后续读取时阻塞）
     */
    private fun setDataSync(key: Preferences.Key<String>, value: String) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Boolean>, value: Boolean) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Int>, value: Int) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
    
    private fun setDataSync(key: Preferences.Key<Long>, value: Long) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }

    private fun setDataSync(key: Preferences.Key<Float>, value: Float) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
    
    private fun setDataSetSync(key: Preferences.Key<Set<String>>, value: Set<String>) {
        cache[key.name] = value
        runBlocking {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }
    }
    
    /**
     * 同步删除数据（同时清除缓存）
     */
    private fun removeDataSync(key: Preferences.Key<String>) {
        cache.remove(key.name)
        runBlocking {
            context.dataStore.edit { prefs -> prefs.remove(key) }
        }
    }
    
    private fun removeDataSync(key: Preferences.Key<Int>) {
        cache.remove(key.name)
        runBlocking {
            context.dataStore.edit { prefs -> prefs.remove(key) }
        }
    }
    
    private fun removeDataSync(key: Preferences.Key<Boolean>) {
        cache.remove(key.name)
        runBlocking {
            context.dataStore.edit { prefs -> prefs.remove(key) }
        }
    }

    private fun removeDataSync(key: Preferences.Key<Float>) {
        cache.remove(key.name)
        runBlocking {
            context.dataStore.edit { prefs -> prefs.remove(key) }
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
        } catch (e: Exception) {
            android.util.Log.w("SettingsManager", "预加载缓存失败，将在首次访问时逐项加载", e)
        }
    }

    // ==================== SharedPreferences 迁移 ====================
    
    /**
     * 从 SharedPreferences 迁移数据到 DataStore
     * 仅在首次启动时执行一次
     */
    private suspend fun migrateFromSharedPreferences() {
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
                KEY_DARK_MODE, KEY_CLOUD_SYNC_ENABLED, KEY_CLOUD_SYNC_STATUS,
                KEY_LAST_SYNC_TIME, KEY_USER_ID, KEY_CLOUD_API_KEY, KEY_AI_SCENE_ENABLED,
                KEY_LAST_WATERMARK_TEMPLATE, KEY_AI_FINE_TUNE_ENABLED, KEY_SMART_OPTIMIZE_ENABLED,
                KEY_WATERMARK_EDITOR_ENABLED, KEY_HASSELBLAD_COLOR_ENABLED, KEY_CUSTOM_DEVICE_MODEL,
                KEY_PRESET_VERSION_MAP, KEY_FAVORITE_PRESET_IDS, KEY_PINNED_PRESET_IDS,
                KEY_MANUALLY_MODIFIED_PARAMS, KEY_CUSTOM_QUICK_PRESETS, KEY_API_CONFIG_LOADED,
                KEY_AI_API_ENDPOINT, KEY_PRESET_API_ENDPOINT, KEY_AUTH_API_ENDPOINT, KEY_API_VERSION,
                KEY_APPLIED_SATURATION, KEY_APPLIED_CONTRAST, KEY_APPLIED_WARMTH,
                KEY_APPLIED_SHARPNESS, KEY_APPLIED_CLARITY, KEY_APPLIED_BRIGHTNESS,
                KEY_HAS_APPLIED_PRESET, KEY_MIGRATION_HANDLED
            )
            
            context.dataStore.edit { prefs ->
                // 迁移所有数据
                legacyPrefs.all.forEach { (key, value) ->
                    when (value) {
                        is String -> prefs[stringPreferencesKey(key)] = value
                        is Boolean -> prefs[booleanPreferencesKey(key)] = value
                        is Int -> prefs[intPreferencesKey(key)] = value
                        is Long -> prefs[longPreferencesKey(key)] = value
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            prefs[stringSetPreferencesKey(key)] = value as Set<String>
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
        private val KEY_CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        private val KEY_CLOUD_SYNC_STATUS = stringPreferencesKey("cloud_sync_status")
        private val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
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

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}