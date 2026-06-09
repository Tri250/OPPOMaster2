package com.silas.omaster.ai.recipe

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.ai.model.FilmPreset
import com.silas.omaster.ai.model.SceneProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 配方管理器
 * 负责配方的保存、加载、分享和导入
 */
class RecipeManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hasselblad_recipes", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    // 配方列表状态流
    private val _recipesFlow = MutableStateFlow<List<RecipeProfile>>(emptyList())
    val recipesFlow: Flow<List<RecipeProfile>> = _recipesFlow.asStateFlow()

    // 当前配方列表
    val recipes: List<RecipeProfile> get() = _recipesFlow.value

    // 收藏的配方
    val favoriteRecipes: List<RecipeProfile> get() = recipes.filter { it.isFavorite }

    // 常用配方（按使用次数排序）
    val frequentlyUsedRecipes: List<RecipeProfile> get() = recipes.sortedByDescending { it.usageCount }.take(10)

    init {
        loadRecipes()
    }

    /**
     * 保存新配方
     */
    suspend fun saveRecipe(
        profile: SceneProfile,
        selectedFilm: FilmPreset?,
        recipeName: String? = null,
        description: String? = null
    ): RecipeProfile = withContext(Dispatchers.IO) {
        val film = selectedFilm ?: profile.recommendedFilm.firstOrNull() ?: FilmPreset.ALL_PRESETS.first()
        val now = System.currentTimeMillis()

        val recipe = RecipeProfile(
            id = "recipe_${profile.id}_${now}",
            name = recipeName ?: "${profile.name} - ${film.displayName}",
            description = description ?: profile.description,
            author = AuthorInfo(name = "用户"),
            scene = SceneInfo(
                id = profile.id,
                displayName = profile.name,
                category = profile.category.displayName,
                icon = profile.category.icon
            ),
            film = FilmInfo(
                id = film.id,
                displayName = film.displayName,
                series = film.series.displayName,
                matchScore = film.matchScore
            ),
            hasselbladParams = profile.hasselbladParams,
            masterTips = profile.masterTips,
            createdAt = now,
            updatedAt = now,
            tags = profile.tags + film.series.displayName
        )

        // 添加到列表
        val updatedRecipes = recipes + recipe
        saveRecipesToPrefs(updatedRecipes)
        _recipesFlow.value = updatedRecipes

        recipe
    }

    /**
     * 更新配方
     */
    suspend fun updateRecipe(recipe: RecipeProfile): Boolean = withContext(Dispatchers.IO) {
        val index = recipes.indexOfFirst { it.id == recipe.id }
        if (index == -1) return false

        val updatedRecipe = recipe.copy(updatedAt = System.currentTimeMillis())
        val updatedRecipes = recipes.toMutableList()
        updatedRecipes[index] = updatedRecipe

        saveRecipesToPrefs(updatedRecipes)
        _recipesFlow.value = updatedRecipes

        true
    }

    /**
     * 删除配方
     */
    suspend fun deleteRecipe(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        val updatedRecipes = recipes.filter { it.id != recipeId }
        if (updatedRecipes.size == recipes.size) return false

        saveRecipesToPrefs(updatedRecipes)
        _recipesFlow.value = updatedRecipes

        true
    }

    /**
     * 收藏/取消收藏配方
     */
    suspend fun toggleFavorite(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        val recipe = recipes.find { it.id == recipeId } ?: return false
        updateRecipe(recipe.copy(isFavorite = !recipe.isFavorite))
    }

    /**
     * 使用配方（增加使用次数）
     */
    suspend fun useRecipe(recipeId: String): Boolean = withContext(Dispatchers.IO) {
        val recipe = recipes.find { it.id == recipeId } ?: return false
        updateRecipe(recipe.copy(usageCount = recipe.usageCount + 1))
    }

    /**
     * 从分享码导入配方
     */
    suspend fun importRecipe(shareCode: String): ImportResult = withContext(Dispatchers.IO) {
        val recipe = RecipeProfile.fromShareCode(shareCode)
        if (recipe == null) {
            return ImportResult.Error("无效的配方代码")
        }

        // 检查是否已存在
        if (recipes.any { it.id == recipe.id }) {
            return ImportResult.Duplicate(recipe)
        }

        // 添加导入标记
        val importedRecipe = recipe.copy(
            id = "${recipe.id}_imported_${System.currentTimeMillis()}",
            author = recipe.author.copy(name = "${recipe.author.name}（导入）")
        )

        val updatedRecipes = recipes + importedRecipe
        saveRecipesToPrefs(updatedRecipes)
        _recipesFlow.value = updatedRecipes

        ImportResult.Success(importedRecipe)
    }

    /**
     * 导出配方为分享码
     */
    fun exportRecipe(recipeId: String): String? {
        val recipe = recipes.find { it.id == recipeId } ?: return null
        return recipe.toShareCode()
    }

    /**
     * 获取配方统计信息
     */
    fun getStats(): RecipeStats {
        val totalRecipes = recipes.size
        val favoriteCount = favoriteRecipes.size
        val totalUsageCount = recipes.sumOf { it.usageCount }
        val mostUsedRecipe = frequentlyUsedRecipes.firstOrNull()
        val mostUsedScene = recipes.groupBy { it.scene.displayName }
            .maxByOrNull { it.value.sumOf { r -> r.usageCount } }?.key
        val mostUsedFilm = recipes.groupBy { it.film.displayName }
            .maxByOrNull { it.value.sumOf { r -> r.usageCount } }?.key
        val averageUsage = if (totalRecipes > 0) totalUsageCount.toFloat() / totalRecipes else 0f

        return RecipeStats(
            totalRecipes = totalRecipes,
            favoriteCount = favoriteCount,
            totalUsageCount = totalUsageCount,
            mostUsedRecipe = mostUsedRecipe,
            mostUsedScene = mostUsedScene,
            mostUsedFilm = mostUsedFilm,
            averageUsagePerRecipe = averageUsage
        )
    }

    /**
     * 搜索配方
     */
    fun searchRecipes(query: String): List<RecipeProfile> {
        val lowerQuery = query.lowercase()
        return recipes.filter { recipe ->
            recipe.name.contains(query, ignoreCase = true) ||
            recipe.scene.displayName.contains(query, ignoreCase = true) ||
            recipe.film.displayName.contains(query, ignoreCase = true) ||
            recipe.tags.any { it.contains(lowerQuery, ignoreCase = true) }
        }
    }

    /**
     * 按场景分类获取配方
     */
    fun getRecipesBySceneCategory(category: String): List<RecipeProfile> {
        return recipes.filter { it.scene.category == category }
    }

    /**
     * 按胶片系列获取配方
     */
    fun getRecipesByFilmSeries(series: String): List<RecipeProfile> {
        return recipes.filter { it.film.series == series }
    }

    /**
     * 加载配方列表
     */
    private fun loadRecipes() {
        val recipesJson = prefs.getString("recipes_list", null)
        if (recipesJson != null) {
            try {
                val recipeList = json.decodeFromString<List<RecipeProfile>>(recipesJson)
                _recipesFlow.value = recipeList
            } catch (e: Exception) {
                _recipesFlow.value = emptyList()
            }
        }
    }

    /**
     * 保存配方列表到 SharedPreferences
     */
    private fun saveRecipesToPrefs(recipes: List<RecipeProfile>) {
        val recipesJson = json.encodeToString(recipes)
        prefs.edit().putString("recipes_list", recipesJson).apply()
    }

    /**
     * 清空所有配方
     */
    suspend fun clearAllRecipes() = withContext(Dispatchers.IO) {
        prefs.edit().remove("recipes_list").apply()
        _recipesFlow.value = emptyList()
    }

    companion object {
        @Volatile
        private var instance: RecipeManager? = null

        fun getInstance(context: Context): RecipeManager {
            return instance ?: synchronized(this) {
                instance ?: RecipeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 导入结果
 */
sealed class ImportResult {
    data class Success(val recipe: RecipeProfile) : ImportResult()
    data class Duplicate(val recipe: RecipeProfile) : ImportResult()
    data class Error(val message: String) : ImportResult()
}