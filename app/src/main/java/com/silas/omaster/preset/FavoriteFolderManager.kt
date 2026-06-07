package com.silas.omaster.preset

import android.content.Context
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 预设收藏夹管理器
 * 提升管理效率
 * 
 * 支持：
 * - 多收藏夹分类
 * - 自定义收藏夹
 * - 智能分类推荐
 * - 收藏夹排序
 */
class FavoriteFolderManager private constructor(context: Context) {

    // 收藏夹
    data class FavoriteFolder(
        val id: String,
        val name: String,
        val icon: String,
        val color: String,
        val presetIds: Set<String> = emptySet(),
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val isDefault: Boolean = false,
        val sortOrder: Int = 0
    )

    // 预设收藏夹模板
    val defaultFolders = listOf(
        FavoriteFolder(
            id = "folder_all",
            name = "全部收藏",
            icon = "❤️",
            color = "#FF6B35",
            isDefault = true,
            sortOrder = 0
        ),
        FavoriteFolder(
            id = "folder_portrait",
            name = "人像",
            icon = "👤",
            color = "#E91E63",
            sortOrder = 1
        ),
        FavoriteFolder(
            id = "folder_landscape",
            name = "风景",
            icon = "🏔️",
            color = "#4CAF50",
            sortOrder = 2
        ),
        FavoriteFolder(
            id = "folder_food",
            name = "美食",
            icon = "🍜",
            color = "#FF9800",
            sortOrder = 3
        ),
        FavoriteFolder(
            id = "folder_night",
            name = "夜景",
            icon = "🌃",
            color = "#2196F3",
            sortOrder = 4
        ),
        FavoriteFolder(
            id = "folder_street",
            name = "街拍",
            icon = "🚶",
            color = "#9C27B0",
            sortOrder = 5
        ),
        FavoriteFolder(
            id = "folder_film",
            name = "胶片",
            icon = "📷",
            color = "#795548",
            sortOrder = 6
        ),
        FavoriteFolder(
            id = "folder_bw",
            name = "黑白",
            icon = "⚫",
            color = "#607D8B",
            sortOrder = 7
        )
    )

    // 收藏夹列表
    private val _folders = MutableStateFlow<List<FavoriteFolder>>(defaultFolders)
    val folders: StateFlow<List<FavoriteFolder>> = _folders.asStateFlow()

    // 当前选中的收藏夹
    private val _selectedFolder = MutableStateFlow<FavoriteFolder?>(null)
    val selectedFolder: StateFlow<FavoriteFolder?> = _selectedFolder.asStateFlow()

    // 预设收藏状态映射
    private val _presetFavorites = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val presetFavorites: StateFlow<Map<String, Set<String>>> = _presetFavorites.asStateFlow()

    /**
     * 选择收藏夹
     */
    fun selectFolder(folder: FavoriteFolder?) {
        _selectedFolder.value = folder
    }

    /**
     * 创建自定义收藏夹
     */
    fun createFolder(name: String, icon: String, color: String): FavoriteFolder {
        val folder = FavoriteFolder(
            id = "folder_custom_${System.currentTimeMillis()}",
            name = name,
            icon = icon,
            color = color,
            sortOrder = _folders.value.size
        )
        _folders.value = _folders.value + folder
        return folder
    }

    /**
     * 删除收藏夹
     */
    fun deleteFolder(folderId: String) {
        val folder = _folders.value.find { it.id == folderId } ?: return
        if (folder.isDefault) return // 不能删除默认收藏夹

        _folders.value = _folders.value.filter { it.id != folderId }

        // 清除该收藏夹中的预设
        val newFavorites = _presetFavorites.value.toMutableMap()
        newFavorites.keys.forEach { presetId ->
            newFavorites[presetId] = (newFavorites[presetId] ?: emptySet()) - folderId
        }
        _presetFavorites.value = newFavorites
    }

    /**
     * 重命名收藏夹
     */
    fun renameFolder(folderId: String, newName: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId && !folder.isDefault) {
                folder.copy(name = newName, updatedAt = System.currentTimeMillis())
            } else {
                folder
            }
        }
    }

    /**
     * 添加预设到收藏夹
     */
    fun addToFolder(presetId: String, folderId: String) {
        val currentFavorites = _presetFavorites.value.toMutableMap()
        val currentFolders = currentFavorites[presetId] ?: emptySet()
        currentFavorites[presetId] = currentFolders + folderId
        _presetFavorites.value = currentFavorites

        // 更新收藏夹
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    presetIds = folder.presetIds + presetId,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                folder
            }
        }
    }

    /**
     * 从收藏夹移除预设
     */
    fun removeFromFolder(presetId: String, folderId: String) {
        val currentFavorites = _presetFavorites.value.toMutableMap()
        val currentFolders = currentFavorites[presetId] ?: emptySet()
        currentFavorites[presetId] = currentFolders - folderId
        _presetFavorites.value = currentFavorites

        // 更新收藏夹
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    presetIds = folder.presetIds - presetId,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                folder
            }
        }
    }

    /**
     * 切换预设收藏状态
     */
    fun toggleFavorite(presetId: String, folderId: String = "folder_all") {
        val isFavorite = isFavorite(presetId, folderId)
        if (isFavorite) {
            removeFromFolder(presetId, folderId)
        } else {
            addToFolder(presetId, folderId)
        }
    }

    /**
     * 检查预设是否在收藏夹中
     */
    fun isFavorite(presetId: String, folderId: String = "folder_all"): Boolean {
        return _presetFavorites.value[presetId]?.contains(folderId) == true
    }

    /**
     * 获取预设所在的收藏夹列表
     */
    fun getPresetFolders(presetId: String): List<FavoriteFolder> {
        val folderIds = _presetFavorites.value[presetId] ?: emptySet()
        return _folders.value.filter { folderIds.contains(it.id) }
    }

    /**
     * 获取收藏夹中的预设列表
     */
    fun getFolderPresets(folderId: String, allPresets: List<MasterPreset>): List<MasterPreset> {
        val folder = _folders.value.find { it.id == folderId } ?: return emptyList()
        return allPresets.filter { folder.presetIds.contains(it.id) }
    }

    /**
     * 获取收藏夹预设数量
     */
    fun getFolderCount(folderId: String): Int {
        return _folders.value.find { it.id == folderId }?.presetIds?.size ?: 0
    }

    /**
     * 智能推荐收藏夹
     * 根据预设标签自动推荐
     */
    fun recommendFolder(preset: MasterPreset): FavoriteFolder? {
        val tags = preset.tags?.lowercase() ?: return null

        return when {
            tags.contains("人像") || tags.contains("portrait") -> 
                _folders.value.find { it.id == "folder_portrait" }
            tags.contains("风景") || tags.contains("landscape") -> 
                _folders.value.find { it.id == "folder_landscape" }
            tags.contains("美食") || tags.contains("food") -> 
                _folders.value.find { it.id == "folder_food" }
            tags.contains("夜景") || tags.contains("night") -> 
                _folders.value.find { it.id == "folder_night" }
            tags.contains("街拍") || tags.contains("street") -> 
                _folders.value.find { it.id == "folder_street" }
            tags.contains("胶片") || tags.contains("film") -> 
                _folders.value.find { it.id == "folder_film" }
            tags.contains("黑白") || tags.contains("bw") -> 
                _folders.value.find { it.id == "folder_bw" }
            else -> null
        }
    }

    /**
     * 排序收藏夹
     */
    fun sortFolders(folderIds: List<String>) {
        val folderMap = _folders.value.associateBy { it.id }
        _folders.value = folderIds.mapIndexed { index, id ->
            folderMap[id]?.copy(sortOrder = index) ?: return@mapIndexed null
        }.filterNotNull() + _folders.value.filter { !folderIds.contains(it.id) }
    }

    /**
     * 搜索收藏夹
     */
    fun searchFolders(query: String): List<FavoriteFolder> {
        if (query.isBlank()) return _folders.value
        val lowerQuery = query.lowercase()
        return _folders.value.filter { 
            it.name.lowercase().contains(lowerQuery) 
        }
    }

    companion object {
        @Volatile
        private var instance: FavoriteFolderManager? = null

        fun getInstance(context: Context): FavoriteFolderManager {
            return instance ?: synchronized(this) {
                instance ?: FavoriteFolderManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
