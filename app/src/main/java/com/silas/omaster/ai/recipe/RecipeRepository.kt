package com.silas.omaster.ai.recipe

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 摄影配方仓库
 * 加载 assets/recipes/ 下的 JSON 配方文件，提供关键词检索与索引服务
 */
class RecipeRepository(private val context: Context) {

    companion object {
        private const val TAG = "RecipeRepository"
        private const val INDEX_FILE = "recipes/recipe_index.json"
    }

    private val gson = Gson()
    private val recipes = ConcurrentHashMap<String, PhotographyRecipe>()
    private val intentIndex = ConcurrentHashMap<String, MutableList<String>>()
    private var categories: List<RecipeCategory> = emptyList()
    private var isLoaded = false

    /**
     * 异步加载所有配方文件
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        
        try {
            // 加载索引
            val indexJson = context.assets.open(INDEX_FILE).bufferedReader().use { it.readText() }
            val index = gson.fromJson(indexJson, RecipeIndex::class.java)
            categories = index.categories

            // 加载每个配方文件
            for (fileName in index.files) {
                val json = context.assets.open("recipes/$fileName").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<PhotographyRecipe>>() {}.type
                val list: List<PhotographyRecipe> = gson.fromJson(json, type)
                
                for (recipe in list) {
                    recipes[recipe.id] = recipe
                    // 建立意图关键词倒排索引
                    recipe.intentKeywords.forEach { keyword ->
                        val normalized = keyword.lowercase().trim()
                        intentIndex.getOrPut(normalized) { mutableListOf() }.add(recipe.id)
                    }
                }
                Log.d(TAG, "Loaded ${list.size} recipes from $fileName")
            }
            
            isLoaded = true
            Log.i(TAG, "Total recipes loaded: ${recipes.size}, index size: ${intentIndex.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recipes", e)
        }
    }

    /**
     * 根据意图关键词匹配 Top-N 配方
     */
    fun matchByIntent(query: String, limit: Int = 3): List<RecipeMatchResult> {
        if (!isLoaded || query.isBlank()) return emptyList()
        
        val tokens = query.lowercase().split(Regex("[\\s,，]+")).filter { it.length >= 2 }
        val scores = mutableMapOf<String, Int>()
        val reasons = mutableMapOf<String, MutableList<String>>()

        for (token in tokens) {
            for ((keyword, ids) in intentIndex) {
                // 完全匹配 + 3 分，前缀匹配 + 2 分，包含匹配 + 1 分
                val score = when {
                    keyword == token -> 3
                    keyword.startsWith(token) -> 2
                    keyword.contains(token) || token.contains(keyword) -> 1
                    else -> 0
                }
                if (score > 0) {
                    ids.forEach { id ->
                        scores[id] = (scores[id] ?: 0) + score
                        reasons.getOrPut(id) { mutableListOf() }.add(keyword)
                    }
                }
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .mapNotNull { entry ->
                recipes[entry.key]?.let { recipe ->
                    RecipeMatchResult(
                        recipe = recipe,
                        matchScore = entry.value,
                        matchReason = reasons[entry.key]?.distinct()?.take(3)?.joinToString(", ") ?: ""
                    )
                }
            }
    }

    /**
     * 根据场景 ID 查找最佳匹配的配方
     */
    fun findBySceneId(sceneId: String): List<RecipeMatchResult> {
        if (!isLoaded) return emptyList()
        
        return recipes.values.mapNotNull { recipe ->
            val score = calculateSceneMatchScore(sceneId, recipe)
            if (score > 0) {
                RecipeMatchResult(recipe, score, "场景匹配")
            } else null
        }.sortedByDescending { it.matchScore }.take(3)
    }

    /**
     * 场景与配方的匹配评分
     */
    private fun calculateSceneMatchScore(sceneId: String, recipe: PhotographyRecipe): Int {
        return when {
            sceneId.startsWith("portrait") && recipe.category == "portrait" -> 2
            sceneId.startsWith("landscape") && recipe.category == "environment" -> 2
            sceneId.startsWith("night") && recipe.category == "environment" -> 1
            sceneId.startsWith("urban") && recipe.category == "environment" -> 1
            sceneId.startsWith("still") && recipe.category == "environment" -> 1
            sceneId.startsWith("macro") && recipe.category == "special_optic" -> 2
            sceneId.startsWith("event") && recipe.category == "portrait" -> 1
            else -> 0
        }
    }

    fun getById(id: String): PhotographyRecipe? = recipes[id]
    fun getAllCategories(): List<RecipeCategory> = categories
    fun getRecipesByCategory(categoryId: String): List<PhotographyRecipe> {
        return recipes.values.filter { it.category == categoryId }
    }
    fun getRecipeCount(): Int = recipes.size
}
