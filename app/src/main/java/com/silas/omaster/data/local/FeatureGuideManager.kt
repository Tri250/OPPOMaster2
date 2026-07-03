package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 功能引导管理器
 * 
 * 记录用户是否已看过功能引导流程
 * 与 OnboardingManager 类似，支持版本升级后重新展示
 */
class FeatureGuideManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("feature_guide_prefs", Context.MODE_PRIVATE)

    /**
     * 检查是否需要展示功能引导
     * 首次使用或版本升级后需要展示
     */
    fun shouldShowFeatureGuide(currentVersionCode: Long): Boolean {
        val lastShownVersion = prefs.getLong(KEY_LAST_SHOWN_VERSION, 0L)
        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_FEATURE_GUIDE, false)
        // 首次使用，或版本升级后展示
        return !hasSeen || (lastShownVersion < currentVersionCode && currentVersionCode >= 20306L)
    }

    /**
     * 标记功能引导已展示
     */
    fun markFeatureGuideShown(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_FEATURE_GUIDE, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 跳过功能引导（用户主动跳过）
     */
    fun skipFeatureGuide(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_FEATURE_GUIDE, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 重置引导状态（用于测试）
     */
    fun resetGuideStatus() {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_FEATURE_GUIDE, false)
            .putLong(KEY_LAST_SHOWN_VERSION, 0L)
            .apply()
    }

    companion object {
        private const val KEY_HAS_SEEN_FEATURE_GUIDE = "has_seen_feature_guide"
        private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"

        @Volatile
        private var instance: FeatureGuideManager? = null

        fun getInstance(context: Context): FeatureGuideManager {
            return instance ?: synchronized(this) {
                instance ?: FeatureGuideManager(context.applicationContext).also { instance = it }
            }
        }
    }
}