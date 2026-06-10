package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 【新预设标记管理器 - 简化版】
 *
 * 【使用方式】
 * 1. 在 presets.json 中手动标记新增预设为 "isNew": true
 * 2. 发版前将旧版本的 "isNew" 改为 false（或删除该字段）
 *
 * 【工作原理】
 * - 完全依赖 JSON 中的 isNew 字段
 * - 不需要版本号判断
 * - 手动控制哪些预设显示 NEW 标签
 */
class NewPresetManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // 存储用户已查看的预设ID，避免重复显示NEW标签
    private val _viewedPresets = MutableStateFlow<Set<String>>(loadViewedPresets())
    val viewedPresets: StateFlow<Set<String>> = _viewedPresets.asStateFlow()

    /**
     * 从SharedPreferences加载已查看的预设ID集合
     */
    private fun loadViewedPresets(): Set<String> {
        return prefs.getStringSet(KEY_VIEWED_PRESETS, emptySet()) ?: emptySet()
    }

    /**
     * 保存已查看的预设ID集合到SharedPreferences
     */
    private fun saveViewedPresets(presetIds: Set<String>) {
        prefs.edit().putStringSet(KEY_VIEWED_PRESETS, presetIds).apply()
    }

    /**
     * 标记预设为已查看
     * @param presetId 预设ID
     */
    fun markAsViewed(presetId: String) {
        val current = _viewedPresets.value.toMutableSet()
        current.add(presetId)
        _viewedPresets.value = current
        saveViewedPresets(current)
    }

    /**
     * 检查预设是否为新预设（未被查看）
     * @param presetId 预设ID
     * @param isNewFromJson JSON中的isNew字段值
     * @return 是否应该显示NEW标签
     */
    fun shouldShowNewBadge(presetId: String, isNewFromJson: Boolean): Boolean {
        // 只有JSON标记为新预设且用户未查看过才显示NEW标签
        return isNewFromJson && !_viewedPresets.value.contains(presetId)
    }

    /**
     * 清除所有已查看记录（用于测试或重置）
     */
    fun clearViewedPresets() {
        _viewedPresets.value = emptySet()
        prefs.edit().remove(KEY_VIEWED_PRESETS).apply()
    }

    /**
     * 获取新预设数量
     * @param allPresets 所有预设列表，包含isNew字段
     * @return 未被查看的新预设数量
     */
    fun getNewPresetCount(allPresets: List<Pair<String, Boolean>>): Int {
        return allPresets.count { (id, isNew) ->
            shouldShowNewBadge(id, isNew)
        }
    }

    companion object {
        private const val PREFS_NAME = "omaster_new_presets"
        private const val KEY_VIEWED_PRESETS = "viewed_presets"

        @Volatile
        private var INSTANCE: NewPresetManager? = null

        fun getInstance(context: Context): NewPresetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NewPresetManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
