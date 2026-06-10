package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.model.*
import com.silas.omaster.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * LUT 本地数据源
 * 管理 LUT 缓存、下载文件、收藏等本地数据
 */
class LUTLocalDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    
    // SharedPreferences 存储
    private val prefs: SharedPreferences = context.getSharedPreferences("lut_cache", Context.MODE_PRIVATE)
    private val favoritesPrefs: SharedPreferences = context.getSharedPreferences("lut_favorites", Context.MODE_PRIVATE)
    private val downloadsPrefs: SharedPreferences = context.getSharedPreferences("lut_downloads", Context.MODE_PRIVATE)
    
    // LUT 文件存储目录
    private val lutDir: File = File(context.filesDir, "luts").apply { mkdirs() }
    
    // 内存缓存
    private val lutCache = MutableStateFlow<List<MasterLUT>>(emptyList())

    /**
     * 获取 LUT 列表
     */
    fun getLUTs(
        category: LUTCategory,
        query: String,
        sortBy: LUTSortBy
    ): List<MasterLUT> {
        var result = lutCache.value
        
        // 分类过滤
        if (category != LUTCategory.ALL) {
            result = result.filter { it.category == category }
        }
        
        // 搜索过滤
        if (query.isNotEmpty()) {
            val q = query.lowercase()
            result = result.filter { 
                it.name.lowercase().contains(q) || 
                it.nameEn.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
            }
        }
        
        // 排序
        result = when (sortBy) {
            LUTSortBy.DOWNLOADS -> result.sortedByDescending { it.downloads }
            LUTSortBy.RATING -> result.sortedByDescending { it.rating }
            LUTSortBy.NEWEST -> result.sortedByDescending { it.createdAt }
            LUTSortBy.NAME -> result.sortedBy { it.name }
        }
        
        return result
    }

    /**
     * 根据 ID 获取 LUT
     */
    fun getLUTById(id: String): Flow<MasterLUT?> = lutCache.map { luts ->
        luts.find { it.id == id }
    }

    /**
     * 获取热门 LUT
     */
    fun getHotLUTs(): Flow<List<MasterLUT>> = lutCache.map { luts ->
        luts.filter { it.isHot }.sortedByDescending { it.downloads }.take(10)
    }

    /**
     * 获取新品 LUT
     */
    fun getNewLUTs(): Flow<List<MasterLUT>> = lutCache.map { luts ->
        luts.filter { it.isNew }.sortedByDescending { it.createdAt }.take(10)
    }

    /**
     * 缓存 LUT 列表
     */
    suspend fun cacheLUTs(luts: List<MasterLUT>) = withContext(Dispatchers.IO) {
        lutCache.value = luts
        // 持久化缓存
        prefs.edit().putString("cached_luts", json.encodeToString(luts)).apply()
    }

    /**
     * 从持久化缓存加载
     */
    suspend fun loadCachedLUTs(): List<MasterLUT> = withContext(Dispatchers.IO) {
        val cached = prefs.getString("cached_luts", null)
        if (cached != null) {
            try {
                val luts = json.decodeFromString<List<MasterLUT>>(cached)
                lutCache.value = luts
                luts
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 检查是否已下载
     */
    fun hasLUTFile(id: String): Boolean = File(lutDir, "$id.cube").exists()

    /**
     * 获取 LUT 文件路径
     */
    fun getLUTFilePath(id: String): String = File(lutDir, "$id.cube").absolutePath

    /**
     * 保存 LUT 文件
     */
    suspend fun saveLUTFile(id: String, sourcePath: String) = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        val targetFile = File(lutDir, "$id.cube")
        sourceFile.copyTo(targetFile, overwrite = true)
    }

    /**
     * 记录下载
     */
    fun recordDownload(id: String) {
        downloadsPrefs.edit().putLong(id, System.currentTimeMillis()).apply()
    }

    /**
     * 获取已下载的 LUT 列表
     */
    fun getDownloadedLUTs(): Flow<List<MasterLUT>> = lutCache.map { luts ->
        luts.filter { hasLUTFile(it.id) }
    }

    /**
     * 删除已下载的 LUT
     */
    suspend fun deleteDownloadedLUT(id: String) = withContext(Dispatchers.IO) {
        File(lutDir, "$id.cube").delete()
        downloadsPrefs.edit().remove(id).apply()
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(id: String) {
        val isFavorite = favoritesPrefs.getBoolean(id, false)
        favoritesPrefs.edit().putBoolean(id, !isFavorite).apply()
    }

    /**
     * 获取收藏列表
     */
    fun getFavorites(): Flow<List<MasterLUT>> = lutCache.map { luts ->
        luts.filter { favoritesPrefs.getBoolean(it.id, false) }
    }

    /**
     * 更新评分
     */
    fun updateRating(id: String, rating: Float) {
        prefs.edit().putFloat("rating_$id", rating).apply()
    }

    /**
     * 获取评分
     */
    fun getRating(id: String): Float = prefs.getFloat("rating_$id", 0f)
}
