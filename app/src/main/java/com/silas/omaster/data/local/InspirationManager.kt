package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 【v1.6.14 发现 2.0】灵感收藏管理器
 * 保存用户收藏的灵感预设 id（最多 50 条，最新在前），支持订阅刷新后预设被移除时的容错展示。
 */
class InspirationManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _favoritesFlow = MutableStateFlow(loadFavorites())
    val favoritesFlow: StateFlow<List<InspirationFavorite>> = _favoritesFlow.asStateFlow()

    fun isFavorite(presetId: String): Boolean {
        return _favoritesFlow.value.any { it.presetId == presetId }
    }

    /** 收藏 / 取消收藏；返回收藏后的最新状态 */
    fun toggleFavorite(presetId: String): Boolean {
        val current = _favoritesFlow.value.toMutableList()
        val exists = current.removeAll { it.presetId == presetId }
        if (!exists) {
            current.add(0, InspirationFavorite(presetId = presetId, savedAt = System.currentTimeMillis()))
            while (current.size > MAX_FAVORITES) current.removeAt(current.size - 1)
        }
        prefs.edit().putString(KEY_FAVORITES, json.encodeToString(current)).apply()
        _favoritesFlow.value = current
        return !exists
    }

    private fun loadFavorites(): List<InspirationFavorite> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<InspirationFavorite>>(raw) }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "inspiration_favorites_prefs"
        private const val KEY_FAVORITES = "favorites"
        private const val MAX_FAVORITES = 50
    }
}

@Serializable
data class InspirationFavorite(
    val presetId: String,
    val savedAt: Long = 0
)
