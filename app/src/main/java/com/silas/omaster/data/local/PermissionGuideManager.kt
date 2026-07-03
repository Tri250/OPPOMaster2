package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 权限引导管理器
 * 
 * 记录用户是否已看过权限引导流程
 * 与 OnboardingManager 类似，支持版本升级后重新展示
 */
class PermissionGuideManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("permission_guide_prefs", Context.MODE_PRIVATE)

    /**
     * 检查是否需要展示权限引导
     * 首次使用或版本升级后需要展示
     */
    fun shouldShowPermissionGuide(currentVersionCode: Long): Boolean {
        val lastShownVersion = prefs.getLong(KEY_LAST_SHOWN_VERSION, 0L)
        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_PERMISSION_GUIDE, false)
        // 首次使用，或版本升级后展示
        return !hasSeen || (lastShownVersion < currentVersionCode && currentVersionCode >= 20306L)
    }

    /**
     * 标记权限引导已展示
     */
    fun markPermissionGuideShown(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_PERMISSION_GUIDE, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 跳过权限引导（用户主动跳过）
     */
    fun skipPermissionGuide(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_PERMISSION_GUIDE, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 重置引导状态（用于测试）
     */
    fun resetGuideStatus() {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_PERMISSION_GUIDE, false)
            .putLong(KEY_LAST_SHOWN_VERSION, 0L)
            .apply()
    }

    companion object {
        private const val KEY_HAS_SEEN_PERMISSION_GUIDE = "has_seen_permission_guide"
        private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"

        @Volatile
        private var instance: PermissionGuideManager? = null

        fun getInstance(context: Context): PermissionGuideManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionGuideManager(context.applicationContext).also { instance = it }
            }
        }
    }
}