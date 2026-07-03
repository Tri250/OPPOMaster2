package com.silas.omaster.infrastructure.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 功能旗标 / A/B 测试系统
 *
 * 基于 SharedPreferences 的简单功能旗标管理：
 * - 默认值内置在代码中
 * - 可通过 SettingsManager API 配置远程覆盖
 * - 支持 Flow 响应式观察
 * - 无需 RemoteConfig 依赖
 *
 * 使用方式：
 * ```kotlin
 * val flags = FeatureFlags.getInstance(context)
 * if (flags.isEnabled(FeatureFlags.FLAG_NEW_SEARCH)) {
 *     // 显示新版搜索
 * }
 * ```
 */
class FeatureFlags private constructor(context: Context) {

    companion object {
        private const val TAG = "FeatureFlags"
        private const val PREFS_NAME = "feature_flags_prefs"

        // ===== 功能旗标定义 =====

        /** 启用新版搜索 */
        const val FLAG_NEW_SEARCH = "enable_new_search"

        /** 启用视频转码 */
        const val FLAG_VIDEO_TRANSCODING = "enable_video_transcoding"

        /** 启用 Google Drive 同步 */
        const val FLAG_GOOGLE_DRIVE = "enable_google_drive"

        /** 显示新版引导页 */
        const val FLAG_NEW_ONBOARDING = "show_new_onboarding"

        // ===== 默认值 =====

        private val DEFAULTS = mapOf(
            FLAG_NEW_SEARCH to false,
            FLAG_VIDEO_TRANSCODING to false,
            FLAG_GOOGLE_DRIVE to false,
            FLAG_NEW_ONBOARDING to false
        )

        @Volatile
        private var instance: FeatureFlags? = null

        fun getInstance(context: Context): FeatureFlags {
            return instance ?: synchronized(this) {
                instance ?: FeatureFlags(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 每个旗标对应一个 MutableStateFlow
    private val flows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    /**
     * 获取功能旗标的值（同步）
     */
    fun isEnabled(flag: String): Boolean {
        // 优先从 Preferences 读取（用户或 API 覆盖）
        if (prefs.contains(flag)) {
            return prefs.getBoolean(flag, DEFAULTS[flag] ?: false)
        }
        return DEFAULTS[flag] ?: false
    }

    /**
     * 获取功能旗标的响应式 Flow
     */
    fun isEnabledFlow(flag: String): Flow<Boolean> {
        return flows.getOrPut(flag) {
            MutableStateFlow(isEnabled(flag))
        }.asStateFlow()
    }

    /**
     * 设置功能旗标（覆盖默认值）
     */
    fun setEnabled(flag: String, enabled: Boolean) {
        prefs.edit().putBoolean(flag, enabled).apply()
        flows[flag]?.value = enabled
        Log.d(TAG, "功能旗标更新: $flag = $enabled")
    }

    /**
     * 从 API 配置同步功能旗标
     * 可被 SettingsManager 的远程配置覆盖
     */
    fun syncFromApiConfig(flagOverrides: Map<String, Boolean>) {
        flagOverrides.forEach { (flag, enabled) ->
            if (DEFAULTS.containsKey(flag)) {
                setEnabled(flag, enabled)
            }
        }
        Log.i(TAG, "API 配置同步完成: ${flagOverrides.size} 项")
    }

    /**
     * 重置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        DEFAULTS.keys.forEach { flag ->
            flows[flag]?.value = DEFAULTS[flag] ?: false
        }
        Log.i(TAG, "功能旗标已重置为默认值")
    }

    /**
     * 获取所有旗标状态
     */
    fun getAllFlags(): Map<String, Boolean> {
        return DEFAULTS.keys.associateWith { isEnabled(it) }
    }

    /**
     * 重置单个旗标为默认值
     */
    fun resetFlag(flag: String) {
        prefs.edit().remove(flag).apply()
        flows[flag]?.value = DEFAULTS[flag] ?: false
        Log.d(TAG, "功能旗标重置: $flag = ${DEFAULTS[flag]}")
    }
}