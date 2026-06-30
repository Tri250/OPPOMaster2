package com.silas.omaster.util

import android.content.Context
import android.util.Log
import com.umeng.analytics.MobclickAgent
import org.json.JSONObject

/**
 * 埋点事件管理工具
 *
 * 功能：
 * - 统一的事件埋点接口
 * - 事件参数标准化
 * - 性能埋点
 * - 页面埋点
 * - 用户行为埋点
 *
 * 基于友盟统计 SDK，提供统一的封装接口
 */
object AnalyticsManager {

    private const val TAG = "Analytics"
    private var isInitialized = false
    private var context: Context? = null
    private var isDebugMode = false

    // ===== 页面事件 =====
    const val EVENT_PAGE_VIEW = "page_view"
    const val EVENT_HOME_VIEW = "home_view"
    const val EVENT_CORE_FEATURES_VIEW = "core_features_view"
    const val EVENT_PRESET_DETAIL_VIEW = "preset_detail_view"
    const val EVENT_SETTINGS_VIEW = "settings_view"
    const val EVENT_LUT_SHARE_VIEW = "lut_share_view"
    const val EVENT_TRAILSNAP_VIEW = "trailsnap_view"

    // ===== 功能事件 =====
    const val EVENT_PRESET_FAVORITE = "preset_favorite"
    const val EVENT_PRESET_UNFAVORITE = "preset_unfavorite"
    const val EVENT_PRESET_CREATE = "preset_create"
    const val EVENT_PRESET_EDIT = "preset_edit"
    const val EVENT_PRESET_DELETE = "preset_delete"
    const val EVENT_PRESET_SHARE = "preset_share"
    const val EVENT_PRESET_SEARCH = "preset_search"
    const val EVENT_PRESET_FILTER = "preset_filter"
    const val EVENT_PRESET_DOWNLOAD = "preset_download"

    // ===== 核心功能事件 =====
    const val EVENT_AI_FINETUNE_USE = "ai_finetune_use"
    const val EVENT_SMART_OPTIMIZE_USE = "smart_optimize_use"
    const val EVENT_HASSELBLAD_USE = "hasselblad_use"
    const val EVENT_CAMERA_X_USE = "camera_x_use"
    const val EVENT_LUT_GENERATE = "lut_generate"
    const val EVENT_LUT_APPLY = "lut_apply"

    // ===== 行影集事件 =====
    const val EVENT_TRAILSNAP_PHOTO_VIEW = "trailsnap_photo_view"
    const val EVENT_TRAILSNAP_ALBUM_CREATE = "trailsnap_album_create"
    const val EVENT_TRAILSNAP_FACE_DETECT = "trailsnap_face_detect"
    const val EVENT_TRAILSNAP_TICKET_SCAN = "trailsnap_ticket_scan"
    const val EVENT_TRAILSNAP_TOOLBOX_USE = "trailsnap_toolbox_use"
    const val EVENT_TRAILSNAP_ANNUAL_REPORT = "trailsnap_annual_report"

    // ===== 设置事件 =====
    const val EVENT_SETTINGS_THEME_CHANGE = "settings_theme_change"
    const val EVENT_SETTINGS_CACHE_CLEAR = "settings_cache_clear"
    const val EVENT_SETTINGS_FLOATING_TOGGLE = "settings_floating_toggle"
    const val EVENT_SETTINGS_DATA_IMPORT = "settings_data_import"
    const val EVENT_SETTINGS_DATA_EXPORT = "settings_data_export"

    // ===== 性能事件 =====
    const val EVENT_APP_STARTUP = "app_startup"
    const val EVENT_PAGE_RENDER = "page_render"
    const val EVENT_CRASH_OCCURRED = "crash_occurred"

    // ===== 参数名 =====
    const val PARAM_PAGE_NAME = "page_name"
    const val PARAM_PRESET_ID = "preset_id"
    const val PARAM_PRESET_NAME = "preset_name"
    const val PARAM_PRESET_BRAND = "preset_brand"
    const val PARAM_THEME_COLOR = "theme_color"
    const val PARAM_DURATION_MS = "duration_ms"
    const val PARAM_RESULT = "result"
    const val PARAM_FROM = "from"
    const val PARAM_QUERY = "query"
    const val PARAM_FILTER = "filter"

    /**
     * 初始化埋点管理
     */
    fun init(context: Context, debugMode: Boolean = false) {
        if (isInitialized) return
        this.context = context.applicationContext
        this.isDebugMode = debugMode
        this.isInitialized = true

        Log.i(TAG, "埋点管理初始化完成，Debug模式: $debugMode")
    }

    /**
     * 页面进入埋点
     */
    fun onPageStart(pageName: String) {
        if (!isInitialized) return
        try {
            MobclickAgent.onPageStart(pageName)
            logEvent("page_start", mapOf(PARAM_PAGE_NAME to pageName))
        } catch (e: Exception) {
            Log.e(TAG, "页面开始埋点失败", e)
        }
    }

    /**
     * 页面结束埋点
     */
    fun onPageEnd(pageName: String) {
        if (!isInitialized) return
        try {
            MobclickAgent.onPageEnd(pageName)
        } catch (e: Exception) {
            Log.e(TAG, "页面结束埋点失败", e)
        }
    }

    /**
     * 自定义事件埋点
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!isInitialized) return

        if (isDebugMode) {
            Log.d(TAG, "事件: $eventName, 参数: ${params?.let { JSONObject(it).toString() } ?: "无"}")
        }

        try {
            if (params != null) {
                val jsonParams = JSONObject()
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> jsonParams.put(key, value)
                        is Int -> jsonParams.put(key, value)
                        is Long -> jsonParams.put(key, value)
                        is Float -> jsonParams.put(key, value)
                        is Double -> jsonParams.put(key, value)
                        is Boolean -> jsonParams.put(key, value)
                        else -> jsonParams.put(key, value.toString())
                    }
                }
                MobclickAgent.onEvent(context, eventName, jsonParams)
            } else {
                MobclickAgent.onEvent(context, eventName)
            }
        } catch (e: Exception) {
            if (isDebugMode) {
                Log.e(TAG, "事件埋点失败: $eventName", e)
            }
        }
    }

    /**
     * 预设收藏埋点
     */
    fun logPresetFavorite(presetId: String, presetName: String, brand: String) {
        logEvent(
            EVENT_PRESET_FAVORITE,
            mapOf(
                PARAM_PRESET_ID to presetId,
                PARAM_PRESET_NAME to presetName,
                PARAM_PRESET_BRAND to brand
            )
        )
    }

    /**
     * 预设取消收藏埋点
     */
    fun logPresetUnfavorite(presetId: String, presetName: String) {
        logEvent(
            EVENT_PRESET_UNFAVORITE,
            mapOf(
                PARAM_PRESET_ID to presetId,
                PARAM_PRESET_NAME to presetName
            )
        )
    }

    /**
     * 预设搜索埋点
     */
    fun logPresetSearch(query: String, resultCount: Int) {
        logEvent(
            EVENT_PRESET_SEARCH,
            mapOf(
                PARAM_QUERY to query,
                "result_count" to resultCount
            )
        )
    }

    /**
     * 主题切换埋点
     */
    fun logThemeChange(themeColor: String, from: String) {
        logEvent(
            EVENT_SETTINGS_THEME_CHANGE,
            mapOf(
                PARAM_THEME_COLOR to themeColor,
                PARAM_FROM to from
            )
        )
    }

    /**
     * 核心功能使用埋点
     */
    fun logCoreFeatureUse(featureName: String, durationMs: Long = 0, result: String = "success") {
        logEvent(
            "core_feature_use",
            mapOf(
                "feature_name" to featureName,
                PARAM_DURATION_MS to durationMs,
                PARAM_RESULT to result
            )
        )
    }

    /**
     * 启动性能埋点
     */
    fun logAppStartup(durationMs: Long, step: String = "total") {
        logEvent(
            EVENT_APP_STARTUP,
            mapOf(
                "step" to step,
                PARAM_DURATION_MS to durationMs
            )
        )
    }

    /**
     * 页面渲染性能埋点
     */
    fun logPageRender(pageName: String, renderTimeMs: Long, frameDropCount: Int = 0) {
        logEvent(
            EVENT_PAGE_RENDER,
            mapOf(
                PARAM_PAGE_NAME to pageName,
                "render_time_ms" to renderTimeMs,
                "frame_drop_count" to frameDropCount
            )
        )
    }

    /**
     * 行影集功能使用埋点
     */
    fun logTrailsnapUse(action: String, detail: String = "") {
        logEvent(
            "trailsnap_use",
            mapOf(
                "action" to action,
                "detail" to detail
            )
        )
    }

    /**
     * LUT功能使用埋点
     */
    fun logLutUse(action: String, lutId: String = "", durationMs: Long = 0) {
        logEvent(
            "lut_use",
            mapOf(
                "action" to action,
                "lut_id" to lutId,
                PARAM_DURATION_MS to durationMs
            )
        )
    }

    /**
     * 设置操作埋点
     */
    fun logSettingsAction(action: String, value: String = "") {
        logEvent(
            "settings_action",
            mapOf(
                "action" to action,
                "value" to value
            )
        )
    }

    /**
     * 用户属性设置
     */
    fun setUserProfile(key: String, value: String) {
        if (!isInitialized) return
        try {
            MobclickAgent.onProfileSignIn(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "设置用户属性失败", e)
        }
    }

    /**
     * 计算事件数量统计
     */
    fun getEventCount(): Int {
        return -1
    }
}
