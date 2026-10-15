package com.silas.omaster.data.config

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.local.UpdateChannel

/**
 * 配置数据迁移工具
 * 从旧版本管理器迁移数据到 ConfigCenter
 */
object ConfigMigration {

    /**
     * 执行数据迁移
     * @return true 表示执行了迁移，false 表示无需迁移
     */
    fun migrate(context: Context, targetPrefs: SharedPreferences): Boolean {
        if (targetPrefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            android.util.Log.d(ConfigLog.TAG, "[Migration] Already done, skipping")
            return false
        }

        android.util.Log.d(ConfigLog.TAG, "[Migration] Starting config migration...")

        val editor = targetPrefs.edit()
        var hasData = false

        // 1. 迁移旧版设置存储（SharedPreferences "app_settings"）
        val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (settingsPrefs.all.isNotEmpty()) {
            android.util.Log.d(ConfigLog.TAG, "[Migration] Migrating legacy app_settings...")

            // 主题
            settingsPrefs.getString("theme_id", null)?.let {
                editor.putString("theme_id", it)
                hasData = true
            }

            // 语言
            settingsPrefs.getString("app_language", null)?.let {
                editor.putString("app_language", it)
                hasData = true
            }

            // 震动
            if (settingsPrefs.contains("vibration_enabled")) {
                editor.putBoolean("vibration_enabled",
                    settingsPrefs.getBoolean("vibration_enabled", true))
                hasData = true
            }

            // 悬浮窗透明度
            if (settingsPrefs.contains("floating_window_opacity")) {
                editor.putInt("floating_window_opacity",
                    settingsPrefs.getInt("floating_window_opacity", 56))
                hasData = true
            }

            // 悬浮窗模式
            settingsPrefs.getString("floating_window_mode", null)?.let {
                editor.putString("floating_window_mode", it)
                hasData = true
            }

            // 默认启动 Tab
            if (settingsPrefs.contains("default_start_tab")) {
                editor.putInt("default_start_tab",
                    settingsPrefs.getInt("default_start_tab", 0))
                hasData = true
            }

            // 更新渠道
            settingsPrefs.getString("update_channel", null)?.let {
                editor.putString("update_channel", it)
                hasData = true
            }

            // 统计开关
            if (settingsPrefs.contains("analytics_enabled")) {
                editor.putBoolean("analytics_enabled",
                    settingsPrefs.getBoolean("analytics_enabled", true))
                hasData = true
            }
        }

        // 2. 迁移旧版预设更新地址（SharedPreferences "omaster_update_prefs"）
        val updatePrefs = context.getSharedPreferences("omaster_update_prefs", Context.MODE_PRIVATE)
        if (updatePrefs.all.isNotEmpty()) {
            android.util.Log.d(ConfigLog.TAG, "[Migration] Migrating legacy update prefs...")

            updatePrefs.getString("preset_update_url", null)?.let { url ->
                // 推断更新渠道
                val channel = when {
                    url.contains("github", ignoreCase = true) -> UpdateChannel.GITHUB
                    else -> UpdateChannel.GITEE
                }
                editor.putString("update_channel", channel.name)
                hasData = true
            }
        }

        // 标记迁移完成
        editor.putBoolean(KEY_MIGRATION_DONE, true)
        editor.apply()

        android.util.Log.d(ConfigLog.TAG, "[Migration] Completed. Has data: $hasData")
        return hasData
    }

    /**
     * 清理旧版数据（在确认迁移成功后调用）
     * 注意：订阅存储 "omaster_subscriptions" 由 SubscriptionConfig 在构造时自行迁移，
     * 此处一并清理，确保旧版残留数据不再占用存储。
     */
    fun cleanupLegacyData(context: Context) {
        android.util.Log.d(ConfigLog.TAG, "[Migration] Cleaning up legacy data...")

        // 删除旧版 SharedPreferences
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("omaster_update_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("omaster_subscriptions", Context.MODE_PRIVATE)
            .edit().clear().apply()

        android.util.Log.d(ConfigLog.TAG, "[Migration] Legacy data cleaned up")
    }

    private const val KEY_MIGRATION_DONE = "config_migration_done"
}
