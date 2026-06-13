package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 配方历史管理器
 * 
 * 功能：
 * - 记录用户保存的配方（场景识别结果）
 * - 提供统计数据用于分析报告
 * - 支持时间范围筛选
 * 
 * 数据结构：
 * - 每条配方记录包含：场景ID、场景名称、胶片ID、胶片名称、时间戳、置信度
 */
class RecipeHistoryManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _recipesFlow = MutableStateFlow<List<RecipeRecord>>(emptyList())
    val recipesFlow: StateFlow<List<RecipeRecord>> = _recipesFlow.asStateFlow()

    init {
        loadRecipes()
    }

    /**
     * 获取所有配方记录
     */
    fun getAllRecipes(): List<RecipeRecord> = _recipesFlow.value

    /**
     * 添加新的配方记录
     * 性能优化：自动限制最大记录数,防止 SharedPreferences 过载
     */
    fun addRecipe(recipe: RecipeRecord) {
        val recipes = getAllRecipes().toMutableList()
        recipes.add(0, recipe) // 最新的在前面
        // 限制最大记录数,防止 OOM 与持久化文件膨胀
        val trimmed = if (recipes.size > MAX_RECIPES) {
            recipes.take(MAX_RECIPES)
        } else {
            recipes
        }
        saveRecipes(trimmed)
    }

    /**
     * 根据时间范围获取配方记录
     * @param timeRange 时间范围：week, month, year, all
     */
    fun getRecipesByTimeRange(timeRange: String): List<RecipeRecord> {
        val allRecipes = getAllRecipes()
        if (allRecipes.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val rangeMillis = when (timeRange) {
            "week" -> 7 * 24 * 60 * 60 * 1000L
            "month" -> 30 * 24 * 60 * 60 * 1000L
            "year" -> 365 * 24 * 60 * 60 * 1000L
            "all" -> Long.MAX_VALUE
            else -> Long.MAX_VALUE
        }

        return allRecipes.filter { now - it.timestamp <= rangeMillis }
    }

    /**
     * 清除所有配方记录
     */
    fun clearAllRecipes() {
        saveRecipes(emptyList())
    }

    /**
     * 删除指定配方记录
     */
    fun deleteRecipe(recipeId: String) {
        val recipes = getAllRecipes().filter { it.id != recipeId }
        saveRecipes(recipes)
    }

    /**
     * 获取配方总数
     */
    fun getTotalCount(): Int = getAllRecipes().size

    /**
     * 获取最近拍摄日期
     */
    fun getLastShootDate(): String {
        val recipes = getAllRecipes()
        if (recipes.isEmpty()) return "从未"
        
        val lastTimestamp = recipes.maxOfOrNull { it.timestamp } ?: 0L
        return formatDate(lastTimestamp)
    }

    /**
     * 从 SharedPreferences 加载配方数据
     */
    private fun loadRecipes() {
        val json = prefs.getString(KEY_RECIPES, null)
        val recipes = if (json != null) {
            try {
                val type = object : TypeToken<List<RecipeRecord>>() {}.type
                gson.fromJson<List<RecipeRecord>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("RecipeHistoryManager", "加载配方数据失败", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        _recipesFlow.value = recipes
    }

    /**
     * 保存配方数据到 SharedPreferences
     */
    private fun saveRecipes(recipes: List<RecipeRecord>) {
        val json = gson.toJson(recipes)
        prefs.edit().putString(KEY_RECIPES, json).apply()
        _recipesFlow.value = recipes
    }

    /**
     * 格式化日期
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    companion object {
        private const val PREFS_NAME = "omaster_recipe_history"
        private const val KEY_RECIPES = "recipes"
        // 最大记录数：防止 SharedPreferences 与内存膨胀
        private const val MAX_RECIPES = 500

        @Volatile
        private var INSTANCE: RecipeHistoryManager? = null

        fun getInstance(context: Context): RecipeHistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecipeHistoryManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * 配方记录数据类
 */
data class RecipeRecord(
    val id: String,                    // 唯一标识
    val sceneId: String,               // 场景ID（如 portrait-standard）
    val sceneName: String,             // 场景名称（如 标准人像）
    val sceneCategory: String,         // 场景分类（如 PORTRAIT）
    val filmId: String?,               // 推荐胶片ID
    val filmName: String?,             // 推荐胶片名称
    val timestamp: Long,               // 记录时间戳
    val confidence: Float = 0.85f,     // 识别置信度
    val thumbnail: String? = null      // 缩略图路径（可选）
) {
    /**
     * 格式化日期显示
     */
    fun formatDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}