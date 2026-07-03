package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 首次启动引导管理器
 * 管理 Onboarding 是否已展示，支持版本升级后重新展示
 */
class OnboardingManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    /**
     * 检查是否需要展示引导页
     * 首次安装或版本升级后需要展示
     */
    fun shouldShowOnboarding(currentVersionCode: Long): Boolean {
        val lastShownVersion = prefs.getLong(KEY_LAST_SHOWN_VERSION, 0L)
        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        // 首次使用，或版本升级后展示
        return !hasSeen || (lastShownVersion < currentVersionCode && currentVersionCode >= 10900)
    }

    /**
     * 标记引导页已展示
     */
    fun markOnboardingShown(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 跳过引导页（用户主动跳过）
     */
    fun skipOnboarding(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    companion object {
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"

        @Volatile
        private var instance: OnboardingManager? = null

        fun getInstance(context: Context): OnboardingManager {
            return instance ?: synchronized(this) {
                instance ?: OnboardingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}