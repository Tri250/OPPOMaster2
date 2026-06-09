package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * LUT本地数据源
 * 管理缓存、下载文件、收藏、评分等本地数据
 */
class LUTLocalDataSource(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lut_prefs", Context.MODE_PRIVATE)
    private val downloadDir: File = File(context.filesDir, "luts")

    // 内存缓存
    private val cachedLUTs = MutableStateFlow<List<MasterLUT>>(MasterLUTRepository.ALL_LUTS)
    private val favorites = MutableStateFlow<Set<String>>(loadFavorites())
    private val ratings = MutableStateFlow<Map<String, Float>>(loadRatings())
    private val downloadStates = MutableStateFlow<Map<String, DownloadState>>(loadDownloadStates())

    init {
        // 确保下载目录存在
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    /**
     * 获取LUT列表
     */
    fun getLUTs(
        category: LUTCategory,
        query: String,
        sortBy: LUTSortBy
    ): List<MasterLUT> {
        return cachedLUTs.value.filter { lut ->
            (category == LUTCategory.ALL || lut.category == category) &&
            (query.isEmpty() || lut.name.contains(query, ignoreCase = true) ||
             lut.nameEn.contains(query, ignoreCase = true) ||
             lut.tags.any { it.contains(query, ignoreCase = true) })
        }.sortedWith(getComparator(sortBy))
    }

    /**
     * 获取单个LUT
     */
    fun getLUTById(id: String): MasterLUT? {
        return cachedLUTs.value.find { it.id == id }
    }

    /**
     * 缓存LUT列表
     */
    fun cacheLUTs(luts: List<MasterLUT>) {
        cachedLUTs.value = luts
    }

    /**
     * 缓存单个LUT
     */
    fun cacheLUT(lut: MasterLUT) {
        val current = cachedLUTs.value.toMutableList()
        val index = current.indexOfFirst { it.id == lut.id }
        if (index >= 0) {
            current[index] = lut
        } else {
            current.add(lut)
        }
        cachedLUTs.value = current
    }

    /**
     * 获取热门LUT
     */
    fun getHotLUTs(): List<MasterLUT> {
        return cachedLUTs.value.filter { it.isHot }.sortedByDescending { it.downloads }
    }

    /**
     * 获取新品LUT
     */
    fun getNewLUTs(): List<MasterLUT> {
        return cachedLUTs.value.filter { it.isNew }
    }

    /**
     * 获取精选LUT
     */
    fun getFeaturedLUTs(): List<MasterLUT> {
        return cachedLUTs.value.filter { it.isFeatured }
    }

    /**
     * 搜索LUT
     */
    fun searchLUTs(query: String): List<MasterLUT> {
        val q = query.lowercase()
        return cachedLUTs.value.filter { lut ->
            lut.name.lowercase().contains(q) ||
            lut.nameEn.lowercase().contains(q) ||
            lut.description.lowercase().contains(q) ||
            lut.tags.any { it.lowercase().contains(q) }
        }
    }

    /**
     * 按系列获取LUT
     */
    fun getLUTsByCollection(collection: String): List<MasterLUT> {
        return cachedLUTs.value.filter { it.hasselbladCollection == collection }
    }

    /**
     * 获取HNCS认证LUT
     */
    fun getHncsCertifiedLUTs(): List<MasterLUT> {
        return cachedLUTs.value.filter { it.isHncsCertified }
    }

    // ===== 文件管理 =====

    /**
     * 检查是否已下载
     */
    fun hasLUTFile(lutId: String): Boolean {
        val file = getLUTFile(lutId)
        return file.exists()
    }

    /**
     * 获取LUT文件路径
     */
    fun getLUTFilePath(lutId: String): String {
        return getLUTFile(lutId).absolutePath
    }

    /**
     * 获取LUT文件
     */
    private fun getLUTFile(lutId: String): File {
        return File(downloadDir, "$lutId.cube")
    }

    /**
     * 保存LUT文件
     */
    fun saveLUTFile(lutId: String, sourceFilePath: String) {
        val sourceFile = File(sourceFilePath)
        val targetFile = getLUTFile(lutId)
        if (sourceFile.exists() && sourceFile.absolutePath != targetFile.absolutePath) {
            sourceFile.copyTo(targetFile, overwrite = true)
        }
    }

    /**
     * 删除LUT文件
     */
    fun deleteLUTFile(lutId: String) {
        val file = getLUTFile(lutId)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * 记录下载
     */
    fun recordDownload(lutId: String) {
        val state = DownloadState(
            lutId = lutId,
            isCompleted = true,
            downloadedAt = System.currentTimeMillis()
        )
        val current = downloadStates.value.toMutableMap()
        current[lutId] = state
        downloadStates.value = current
        saveDownloadStates(current)
    }

    /**
     * 移除下载记录
     */
    fun removeDownloadRecord(lutId: String) {
        val current = downloadStates.value.toMutableMap()
        current.remove(lutId)
        downloadStates.value = current
        saveDownloadStates(current)
    }

    /**
     * 获取已下载的LUT列表
     */
    fun getDownloadedLUTs(): Flow<List<MasterLUT>> {
        return downloadStates.asStateFlow().map { states ->
            cachedLUTs.value.filter { lut ->
                states[lut.id]?.isCompleted == true || hasLUTFile(lut.id)
            }
        }
    }

    /**
     * 获取下载状态
     */
    fun getDownloadState(lutId: String): Flow<DownloadState?> {
        return downloadStates.asStateFlow().map { it[lutId] }
    }

    /**
     * 获取所有下载状态
     */
    fun getAllDownloadStates(): Flow<Map<String, DownloadState>> {
        return downloadStates.asStateFlow()
    }

    // ===== 收藏管理 =====

    /**
     * 切换收藏
     */
    fun toggleFavorite(lutId: String) {
        val current = favorites.value.toMutableSet()
        if (current.contains(lutId)) {
            current.remove(lutId)
        } else {
            current.add(lutId)
        }
        favorites.value = current
        saveFavorites(current)
    }

    /**
     * 是否收藏
     */
    fun isFavorite(lutId: String): Flow<Boolean> {
        return favorites.asStateFlow().map { it.contains(lutId) }
    }

    /**
     * 获取收藏列表
     */
    fun getFavorites(): Flow<List<MasterLUT>> {
        return favorites.asStateFlow().map { ids ->
            cachedLUTs.value.filter { it.id in ids }
        }
    }

    // ===== 评分管理 =====

    /**
     * 保存评分
     */
    fun saveRating(lutId: String, rating: Float) {
        val current = ratings.value.toMutableMap()
        current[lutId] = rating
        ratings.value = current
        saveRatings(current)
    }

    /**
     * 获取用户评分
     */
    fun getUserRating(lutId: String): Flow<Float?> {
        return ratings.asStateFlow().map { it[lutId] }
    }

    // ===== SharedPreferences 存储 =====

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    private fun saveFavorites(ids: Set<String>) {
        prefs.edit().putStringSet("favorites", ids).apply()
    }

    private fun loadRatings(): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("rating_") && value is Float) {
                map[key.removePrefix("rating_")] = value
            }
        }
        return map
    }

    private fun saveRatings(map: Map<String, Float>) {
        val editor = prefs.edit()
        // 清除旧评分
        prefs.all.keys.filter { it.startsWith("rating_") }.forEach { editor.remove(it) }
        // 保存新评分
        map.forEach { (id, rating) -> editor.putFloat("rating_$id", rating) }
        editor.apply()
    }

    private fun loadDownloadStates(): Map<String, DownloadState> {
        val map = mutableMapOf<String, DownloadState>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("download_") && value is Long) {
                val lutId = key.removePrefix("download_")
                map[lutId] = DownloadState(
                    lutId = lutId,
                    isCompleted = true,
                    downloadedAt = value
                )
            }
        }
        return map
    }

    private fun saveDownloadStates(map: Map<String, DownloadState>) {
        val editor = prefs.edit()
        // 清除旧记录
        prefs.all.keys.filter { it.startsWith("download_") }.forEach { editor.remove(it) }
        // 保存新记录
        map.forEach { (id, state) ->
            if (state.isCompleted && state.downloadedAt != null) {
                editor.putLong("download_$id", state.downloadedAt)
            }
        }
        editor.apply()
    }

    /**
     * 获取排序比较器
     */
    private fun getComparator(sortBy: LUTSortBy): Comparator<MasterLUT> {
        return when (sortBy) {
            LUTSortBy.DOWNLOADS -> compareByDescending { it.downloads }
            LUTSortBy.RATING -> compareByDescending { it.rating }
            LUTSortBy.NEWEST -> compareByDescending { it.createdAt }
            LUTSortBy.NAME -> compareBy { it.name }
        }
    }
}