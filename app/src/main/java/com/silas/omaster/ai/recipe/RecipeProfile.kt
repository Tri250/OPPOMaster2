package com.silas.omaster.ai.recipe

import com.silas.omaster.ai.model.FilmPreset
import com.silas.omaster.ai.model.HasselbladParams
import com.silas.omaster.ai.model.SceneProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 配方数据模型
 * 场景 + 胶片 + 参数的完整配方
 */
@Serializable
data class RecipeProfile(
    // 配方唯一ID
    val id: String,

    // 配方名称
    val name: String,

    // 配方描述
    val description: String,

    // 作者信息
    val author: AuthorInfo,

    // 场景信息
    val scene: SceneInfo,

    // 胶片预设
    val film: FilmInfo,

    // 哈苏参数
    val hasselbladParams: HasselbladParams,

    // 大师建议
    val masterTips: List<String>,

    // 创建时间
    val createdAt: Long,

    // 更新时间
    val updatedAt: Long,

    // 使用次数
    val usageCount: Int = 0,

    // 收藏状态
    val isFavorite: Boolean = false,

    // 配方版本
    val version: Int = 1,

    // 标签
    val tags: List<String> = emptyList(),

    // 示例图片URL（可选）
    val sampleImageUrl: String? = null
) {
    /**
     * 生成配方分享码（JSON压缩）
     */
    fun toShareCode(): String {
        val json = Json { 
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
        val jsonString = json.encodeToString(serializer(), this)
        // Base64编码
        return java.util.Base64.getEncoder().encodeToString(jsonString.toByteArray())
    }

    /**
     * 获取配方摘要
     */
    fun getSummary(): String {
        return "${scene.displayName} + ${film.displayName}"
    }

    /**
     * 获取配方完整描述
     */
    fun getFullDescription(): String {
        val parts = mutableListOf<String>()
        parts.add("【场景】${scene.displayName}")
        parts.add("【胶片】${film.displayName} (${film.matchPercent}%匹配)")
        parts.add("【参数】${hasselbladParams.formatDisplay().joinToString("·") { "${it.first}${it.second}" }}")
        if (masterTips.isNotEmpty()) {
            parts.add("【建议】${masterTips.first()}")
        }
        return parts.joinToString("\n")
    }

    companion object {
        /**
         * 从分享码解析配方
         */
        fun fromShareCode(code: String): RecipeProfile? {
            return try {
                val json = Json { ignoreUnknownKeys = true }
                val jsonString = java.util.Base64.getDecoder().decode(code).decodeToString()
                json.decodeFromString(serializer(), jsonString)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 从场景配置创建配方
         */
        fun fromSceneProfile(
            profile: SceneProfile,
            selectedFilm: FilmPreset?,
            authorName: String = "用户"
        ): RecipeProfile {
            val film = selectedFilm ?: profile.recommendedFilm.firstOrNull() ?: FilmPreset.ALL_PRESETS.first()
            val now = System.currentTimeMillis()

            return RecipeProfile(
                id = "recipe_${profile.id}_${now}",
                name = "${profile.name} - ${film.displayName}",
                description = profile.description,
                author = AuthorInfo(
                    name = authorName,
                    avatarUrl = null,
                    userId = null
                ),
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
        }
    }
}

/**
 * 作者信息
 */
@Serializable
data class AuthorInfo(
    val name: String,
    val avatarUrl: String? = null,
    val userId: String? = null
)

/**
 * 场景信息（配方中的简化版）
 */
@Serializable
data class SceneInfo(
    val id: String,
    val displayName: String,
    val category: String,
    val icon: String
)

/**
 * 胶片信息（配方中的简化版）
 */
@Serializable
data class FilmInfo(
    val id: String,
    val displayName: String,
    val series: String,
    val matchScore: Float
) {
    val matchPercent: Int get() = (matchScore * 100).toInt()
}

/**
 * 配方分类
 */
enum class RecipeCategory(val displayName: String) {
    MY_RECIPES("我的配方"),
    FAVORITES("收藏配方"),
    POPULAR("热门配方"),
    RECOMMENDED("推荐配方"),
    IMPORTED("导入配方")
}

/**
 * 配方统计信息
 */
data class RecipeStats(
    val totalRecipes: Int,
    val favoriteCount: Int,
    val totalUsageCount: Int,
    val mostUsedRecipe: RecipeProfile?,
    val mostUsedScene: String?,
    val mostUsedFilm: String?,
    val averageUsagePerRecipe: Float
) {
    fun getSummary(): String {
        return "共 $totalRecipes 个配方，收藏 $favoriteCount 个，累计使用 $totalUsageCount 次"
    }
}