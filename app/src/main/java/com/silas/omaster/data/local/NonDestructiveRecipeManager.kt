package com.silas.omaster.data.local

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.ui.features.CurvePoint
import com.silas.omaster.ui.features.PixelFruitParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * P0-1: 非破坏性编辑配方管理器
 *
 * 核心理念（对齐 RapidRAW .rrdata Sidecar）：
 * - 原始图片永不修改
 * - 所有编辑参数以 JSON 配方形式存储于应用私有目录
 * - 支持一图多配方（版本分支），随时可回退到任意历史配方
 * - 导出时根据原图 + 配方实时渲染生成新图
 *
 * 存储结构：
 * /data/data/{pkg}/files/recipes/{imageHash}/
 *   ├── recipe_{uuid}.json   (单个配方)
 *   └── index.json           (该图片的配方索引)
 */
class NonDestructiveRecipeManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val recipesDir = File(appContext.filesDir, "recipes")
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    // 内存缓存：当前会话活跃配方
    private val _activeRecipes = MutableStateFlow<Map<String, EditRecipe>>(emptyMap())
    val activeRecipes: StateFlow<Map<String, EditRecipe>> = _activeRecipes.asStateFlow()

    init {
        if (!recipesDir.exists()) recipesDir.mkdirs()
    }

    /**
     * 根据图片 URI 计算稳定哈希，作为配方关联键
     */
    fun computeImageHash(uri: Uri): String {
        val input = uri.toString().toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * 根据 Bitmap 内容计算哈希（用于无 URI 场景，如相机直接传入）
     */
    fun computeBitmapHash(bitmap: android.graphics.Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        // 降采样到 64x64 计算特征哈希，避免大图内存压力
        val sampleSize = (maxOf(width, height) / 64).coerceAtLeast(1)
        val smallW = width / sampleSize
        val smallH = height / sampleSize
        val pixels = IntArray(smallW * smallH)
        bitmap.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH)
        val digest = MessageDigest.getInstance("SHA-256")
        pixels.forEach { digest.update(it.toByte()) }
        return digest.digest().take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * 获取某张图片的所有配方列表（按时间倒序）
     */
    suspend fun getRecipesForImage(imageHash: String): List<EditRecipe> = withContext(Dispatchers.IO) {
        val indexFile = getIndexFile(imageHash)
        if (!indexFile.exists()) return@withContext emptyList()
        try {
            val json = indexFile.readText()
            val type = object : TypeToken<List<EditRecipeIndexEntry>>() {}.type
            val entries = gson.fromJson<List<EditRecipeIndexEntry>>(json, type) ?: emptyList()
            entries.sortedByDescending { it.createdAt }.mapNotNull { entry ->
                val recipeFile = File(getImageRecipeDir(imageHash), entry.fileName)
                if (recipeFile.exists()) {
                    try {
                        gson.fromJson(recipeFile.readText(), EditRecipe::class.java)
                    } catch (_: Exception) { null }
                } else null
            }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * 保存新配方（非破坏性写入）
     * @return 保存后的配方（含生成的 id）
     */
    suspend fun saveRecipe(
        imageHash: String,
        sourceUri: Uri?,
        recipe: EditRecipe
    ): EditRecipe = withContext(Dispatchers.IO) {
        val imageDir = getImageRecipeDir(imageHash)
        if (!imageDir.exists()) imageDir.mkdirs()

        val recipeWithId = recipe.copy(
            id = recipe.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            imageHash = imageHash,
            sourceUri = sourceUri?.toString(),
            createdAt = System.currentTimeMillis()
        )

        // 写入配方文件
        val fileName = "recipe_${recipeWithId.id}.json"
        val recipeFile = File(imageDir, fileName)
        recipeFile.writeText(gson.toJson(recipeWithId))

        // 更新索引
        updateIndex(imageHash) { entries ->
            val mutable = entries.toMutableList()
            mutable.add(0, EditRecipeIndexEntry(
                id = recipeWithId.id,
                fileName = fileName,
                createdAt = recipeWithId.createdAt,
                label = recipeWithId.label,
                isAutoSave = recipeWithId.isAutoSave
            ))
            // 限制单图最大配方数，防止膨胀
            mutable.take(MAX_RECIPES_PER_IMAGE)
        }

        // 更新内存缓存
        _activeRecipes.value = _activeRecipes.value.toMutableMap().apply {
            put(imageHash, recipeWithId)
        }

        recipeWithId
    }

    /**
     * 自动保存：用于编辑过程中定时/退出时的静默保存
     */
    suspend fun autoSave(imageHash: String, sourceUri: Uri?, recipe: EditRecipe): EditRecipe {
        val autoSaveRecipe = recipe.copy(
            id = "",
            label = "自动保存",
            isAutoSave = true
        )
        return saveRecipe(imageHash, sourceUri, autoSaveRecipe)
    }

    /**
     * 加载某张图片的最新配方（含自动保存）
     */
    suspend fun loadLatestRecipe(imageHash: String): EditRecipe? = withContext(Dispatchers.IO) {
        val recipes = getRecipesForImage(imageHash)
        recipes.firstOrNull()
    }

    /**
     * 删除指定配方
     */
    suspend fun deleteRecipe(imageHash: String, recipeId: String) = withContext(Dispatchers.IO) {
        val imageDir = getImageRecipeDir(imageHash)
        val recipeFile = File(imageDir, "recipe_$recipeId.json")
        if (recipeFile.exists()) recipeFile.delete()
        updateIndex(imageHash) { entries ->
            entries.filter { it.id != recipeId }
        }
    }

    /**
     * 清理某张图片的所有自动保存配方，仅保留用户手动保存的
     */
    suspend fun clearAutoSaves(imageHash: String) = withContext(Dispatchers.IO) {
        val imageDir = getImageRecipeDir(imageHash)
        val indexFile = getIndexFile(imageHash)
        if (!indexFile.exists()) return@withContext
        val json = indexFile.readText()
        val type = object : TypeToken<List<EditRecipeIndexEntry>>() {}.type
        val entries = gson.fromJson<List<EditRecipeIndexEntry>>(json, type) ?: return@withContext
        val autoSaveEntries = entries.filter { it.isAutoSave }
        autoSaveEntries.forEach { entry ->
            File(imageDir, entry.fileName).delete()
        }
        updateIndex(imageHash) { list ->
            list.filter { !it.isAutoSave }
        }
    }

    /**
     * 设置当前会话活跃配方
     */
    fun setActiveRecipe(imageHash: String, recipe: EditRecipe?) {
        _activeRecipes.value = _activeRecipes.value.toMutableMap().apply {
            if (recipe != null) put(imageHash, recipe) else remove(imageHash)
        }
    }

    /**
     * 获取当前会话活跃配方
     */
    fun getActiveRecipe(imageHash: String): EditRecipe? = _activeRecipes.value[imageHash]

    // ==================== 私有辅助 ====================

    private fun getImageRecipeDir(imageHash: String): File =
        File(recipesDir, imageHash)

    private fun getIndexFile(imageHash: String): File =
        File(getImageRecipeDir(imageHash), "index.json")

    private fun updateIndex(
        imageHash: String,
        transform: (List<EditRecipeIndexEntry>) -> List<EditRecipeIndexEntry>
    ) {
        val indexFile = getIndexFile(imageHash)
        val current = try {
            if (indexFile.exists()) {
                val type = object : TypeToken<List<EditRecipeIndexEntry>>() {}.type
                gson.fromJson<List<EditRecipeIndexEntry>>(indexFile.readText(), type) ?: emptyList()
            } else emptyList()
        } catch (_: Exception) { emptyList() }
        val updated = transform(current)
        indexFile.writeText(gson.toJson(updated))
    }

    companion object {
        private const val MAX_RECIPES_PER_IMAGE = 20

        @Volatile
        private var INSTANCE: NonDestructiveRecipeManager? = null

        fun getInstance(context: Context): NonDestructiveRecipeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NonDestructiveRecipeManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * 编辑配方数据类（完整参数快照）
 *
 * 对齐 RapidRAW Sidecar：包含所有可调参数的完整状态
 */
data class EditRecipe(
    val id: String = "",
    val imageHash: String = "",
    val sourceUri: String? = null,
    val createdAt: Long = 0L,
    val label: String = "",
    val isAutoSave: Boolean = false,

    // === AIFineTune 参数域 ===
    val renderParams: RenderParameters = RenderParameters(),
    val hslValues: List<HSLRecipeValue> = emptyList(),
    val curvePoints: Map<String, List<CurvePoint>> = emptyMap(),
    val selectedStyleId: String? = null,
    val selectedOptimizations: Set<String> = emptySet(),

    // === SmartOptimize (PixelFruit) 参数域 ===
    val pixelFruitParams: PixelFruitParams = PixelFruitParams(),

    // === LUT 信息 ===
    val lutId: String? = null,
    val lutStrength: Float = 1f,

    // === 元数据 ===
    val version: Int = CURRENT_RECIPE_VERSION
) {
    companion object {
        const val CURRENT_RECIPE_VERSION = 1
    }

    /**
     * 是否有任何实质性调整
     */
    fun hasAdjustments(): Boolean {
        return renderParams.hasAnyAdjustment() ||
            !pixelFruitParams.isDefault() ||
            lutId != null ||
            selectedOptimizations.isNotEmpty()
    }
}

/**
 * HSL 配方值（序列化友好版本）
 */
data class HSLRecipeValue(
    val id: String,
    val name: String,
    val hue: Int = 0,
    val saturation: Int = 0,
    val luminance: Int = 0
)

/**
 * 配方索引条目（轻量，用于快速列表）
 */
data class EditRecipeIndexEntry(
    val id: String,
    val fileName: String,
    val createdAt: Long,
    val label: String,
    val isAutoSave: Boolean
)

/**
 * 配方列表项（UI 展示用）
 */
data class RecipeListItem(
    val recipe: EditRecipe,
    val thumbnailPath: String? = null,
    val isActive: Boolean = false
)
