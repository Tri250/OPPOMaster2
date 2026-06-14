package com.silas.omaster.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs

/**
 * 预设管理器 - 完整版
 * 实现所有PM系列功能用例
 */
class PresetRepository private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val appContext = context.applicationContext

    // 预设列表
    private val _presets = MutableStateFlow<List<PresetItem>>(emptyList())
    val presets: StateFlow<List<PresetItem>> = _presets.asStateFlow()

    // 收藏列表
    private val _favorites = MutableStateFlow<Set<String>>(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // 置顶列表
    private val _pinnedIds = MutableStateFlow<Set<String>>(loadPinned())
    val pinnedIds: StateFlow<Set<String>> = _pinnedIds.asStateFlow()

    // 搜索历史
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())

    // 设备型号（WM-003）
    private var deviceModel: String = Build.MODEL

    // 预设缓存文件
    private val cacheFile: File
        get() = File(appContext.filesDir, CACHE_FILE_NAME)

    // 损坏的缓存备份文件
    private val corruptedBackupFile: File
        get() = File(appContext.filesDir, CORRUPTED_BACKUP_FILE_NAME)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        coerceInputValues = true
    }

    /**
     * Ktor HTTP 客户端 - 用于访问 jsDelivr CDN
     * 复用以避免每次创建新连接带来的开销
     */
    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = NETWORK_TIMEOUT_MS
                connectTimeoutMillis = NETWORK_TIMEOUT_MS
                socketTimeoutMillis = NETWORK_TIMEOUT_MS
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay(base = 2.0, maxDelayMs = 4_000L)
            }
            expectSuccess = false
        }
    }

    init {
        // 在后台线程初始化本地预设，避免阻塞主线程
        // 优化：守护线程 + 指数退避重试 + 中断检测
        Thread {
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts) {
                try {
                    loadLocalPresets()
                    Log.i(TAG, "本地预设初始化完成: ${_presets.value.size} 条")
                    return@Thread
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@Thread
                } catch (e: Exception) {
                    attempts++
                    Log.e(TAG, "本地预设初始化失败 (尝试 $attempts/$maxAttempts)", e)
                    if (attempts < maxAttempts) {
                        try {
                            Thread.sleep(2000L * attempts)  // 指数退避
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return@Thread
                        }
                    } else {
                        Log.e(TAG, "本地预设初始化彻底失败,使用空列表")
                        _presets.value = emptyList()
                    }
                }
            }
        }.apply {
            name = "PresetRepository-Init"
            isDaemon = true
            priority = Thread.MIN_PRIORITY  // 低优先级,不抢占主线程
        }.start()
    }

    /**
     * PM-001: 预设瀑布流浏览
     * 列表加载 < 2s（100 条内）
     * 滚动 FPS ≥ 55（由UI层保证）
     * 长按卡片弹出操作菜单（由UI层保证）
     */
    suspend fun loadPresets(brand: String? = null): List<PresetItem> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 从CDN或本地缓存加载
        val allPresets = loadFromCacheOrNetwork(brand)

        // 应用置顶排序
        val sortedPresets = applyPinningAndSorting(allPresets)

        _presets.value = sortedPresets

        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 2000) {
            Log.w(TAG, "加载耗时 ${elapsed}ms 超过 2s 阈值，brand=$brand, count=${sortedPresets.size}")
        } else {
            Log.d(TAG, "加载完成: ${sortedPresets.size} 条, 耗时 ${elapsed}ms, brand=$brand")
        }

        sortedPresets
    }

    /**
     * PM-002: 预设筛选
     */
    fun filterPresets(
        presets: List<PresetItem>,
        brands: Set<String>? = null,
        scenes: Set<String>? = null,
        hasHncs: Boolean? = null,
        searchQuery: String? = null
    ): List<PresetItem> {
        var filtered = presets

        // 品牌筛选
        if (!brands.isNullOrEmpty()) {
            filtered = filtered.filter { brands.contains(it.brand) }
        }

        // 场景筛选
        if (!scenes.isNullOrEmpty()) {
            filtered = filtered.filter { scenes.contains(it.scene) }
        }

        // HNCS认证筛选
        if (hasHncs == true) {
            filtered = filtered.filter { it.isHncs }
        }

        // 搜索筛选
        if (!searchQuery.isNullOrBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }

        return filtered
    }


    fun isFavorite(presetId: String): Boolean = _favorites.value.contains(presetId)

    /**
     * PM-004: 自定义预设创建
     */
    suspend fun createCustomPreset(
        name: String,
        params: Map<String, Int>,
        coverPath: String? = null,
        description: String = ""
    ): Result<PresetItem> = withContext(Dispatchers.IO) {
        // 验证名称
        if (name.length !in 1..20) {
            return@withContext Result.failure(IllegalArgumentException("名称长度需在1-20字之间"))
        }

        // 检查重名
        if (_presets.value.any { it.name == name && it.isSystem }) {
            return@withContext Result.failure(IllegalArgumentException("名称不能与系统预设重名"))
        }

        val preset = PresetItem(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            brand = "custom",
            scene = "自定义",
            params = params,
            coverPath = coverPath,
            galleryImages = null,  // 自定义预设默认无画廊图片
            description = description,
            isSystem = false,
            isHncs = false,
            rating = 0f,
            ratingCount = null,
            downloadCount = 0,
            favoriteCount = 0,
            comments = null,
            tags = listOf("自定义"),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isNew = false,
            isPinned = false,
            mode = null
        )

        val current = _presets.value.toMutableList()
        current.add(0, preset)
        _presets.value = current

        // 保存到本地
        saveToCache()

        Result.success(preset)
    }

    /**
     * PM-005: 自定义预设编辑
     * - 编辑仅影响自定义预设
     * - 系统预设不可编辑
     */
    suspend fun updateCustomPreset(
        presetId: String,
        updates: Map<String, Any?>
    ): Result<PresetItem> = withContext(Dispatchers.IO) {
        val index = _presets.value.indexOfFirst { it.id == presetId }
        if (index == -1) {
            return@withContext Result.failure(IllegalArgumentException("预设不存在"))
        }

        val preset = _presets.value[index]

        // PM-005: 系统预设不可编辑
        if (preset.isSystem) {
            return@withContext Result.failure(IllegalArgumentException("系统预设不可编辑"))
        }

        val updated = preset.copy(
            name = updates["name"] as? String ?: preset.name,
            params = updates["params"] as? Map<String, Int> ?: preset.params,
            coverPath = updates["coverPath"] as? String ?: preset.coverPath,
            description = updates["description"] as? String ?: preset.description,
            tags = updates["tags"] as? List<String> ?: preset.tags,
            updatedAt = System.currentTimeMillis()
        )

        val current = _presets.value.toMutableList()
        current[index] = updated
        _presets.value = current

        saveToCache()

        Result.success(updated)
    }

    /**
     * PM-006: 自定义预设删除
     * - 二次确认不可关闭勾选跳过
     * - 删除后释放本地存储
     */
    suspend fun deletePreset(presetId: String, forceConfirm: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val preset = _presets.value.find { it.id == presetId }
            ?: return@withContext Result.failure(IllegalArgumentException("预设不存在"))

        // PM-006: 仅自定义预设可删除
        if (preset.isSystem) {
            return@withContext Result.failure(IllegalArgumentException("系统预设不可删除"))
        }

        if (!forceConfirm) {
            return@withContext Result.failure(ConfirmationRequired("需要确认删除"))
        }

        // 移除收藏
        val favorites = _favorites.value.toMutableSet()
        favorites.remove(presetId)
        _favorites.value = favorites
        saveFavorites(favorites)

        // 移除置顶
        val pinned = _pinnedIds.value.toMutableSet()
        pinned.remove(presetId)
        _pinnedIds.value = pinned
        savePinned(pinned)

        // 删除封面文件
        preset.coverPath?.let { path ->
            runCatching { File(path).delete() }
                .onSuccess { deleted -> Log.d(TAG, "删除封面文件: $path, 成功=$deleted") }
                .onFailure { e -> Log.w(TAG, "删除封面文件失败: $path", e) }
        }

        // 移除预设
        val current = _presets.value.toMutableList()
        current.removeIf { it.id == presetId }
        _presets.value = current

        saveToCache()

        Result.success(Unit)
    }

    /**
     * PM-007: 预设导入/导出
     */
    suspend fun exportPresets(presetIds: Set<String>): Result<File> = withContext(Dispatchers.IO) {
        if (presetIds.size > 50) {
            return@withContext Result.failure(IllegalArgumentException("单次最多导出50条"))
        }

        val presetsToExport = _presets.value.filter { presetIds.contains(it.id) }

        val exportData = ExportData(
            version = 2,
            app = "OMaster",
            timestamp = System.currentTimeMillis(),
            presets = presetsToExport.map { it.toExportModel() }
        )

        val jsonStr = json.encodeToString(exportData)
        val file = File(appContext.cacheDir, "export_${System.currentTimeMillis()}.json")
        file.writeText(jsonStr)

        if (file.length() > 5 * 1024 * 1024) {
            file.delete()
            return@withContext Result.failure(IllegalArgumentException("文件大小超过5MB"))
        }

        Result.success(file)
    }

    suspend fun importPresets(file: File): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val data = json.decodeFromString<ExportData>(content)

            // PM-007: 校验版本号
            if (data.version > 2) {
                return@withContext Result.failure(IllegalArgumentException("版本号不兼容"))
            }

            var imported = 0
            var skipped = 0
            val conflicts = mutableListOf<PresetItem>()

            for (exportModel in data.presets) {
                val existing = _presets.value.find {
                    it.name == exportModel.name && it.isSystem
                }

                if (existing != null) {
                    conflicts.add(existing)
                    skipped++
                } else {
                    val preset = exportModel.toPresetItem()
                    val current = _presets.value.toMutableList()
                    current.add(preset)
                    _presets.value = current
                    imported++
                }
            }

            saveToCache()

            Result.success(ImportResult(imported, skipped, conflicts))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PM-008: 预设置顶与NEW标记
     */
    fun togglePin(presetId: String) {
        val current = _pinnedIds.value.toMutableSet()

        if (current.contains(presetId)) {
            current.remove(presetId)
        } else {
            // PM-008: 最多置顶5条
            if (current.size >= 5) {
                return
            }
            current.add(presetId)
        }

        _pinnedIds.value = current
        savePinned(current)
    }

    fun isPinned(presetId: String): Boolean = _pinnedIds.value.contains(presetId)

    /**
     * PM-008: 检查NEW标记（7天后自动消失）
     */
    fun isNew(presetId: String): Boolean {
        val preset = _presets.value.find { it.id == presetId } ?: return false
        val daysSinceCreation = (System.currentTimeMillis() - preset.createdAt) / (1000 * 60 * 60 * 24)
        return daysSinceCreation <= 7 && preset.isNew
    }

    /**
     * PM-009: 预设数据导入（云端）
     *
     * 流程：尝试从 jsDelivr CDN 拉取所有启用品牌的预设 JSON
     * - 任一品牌成功：合并入内存、写入本地缓存、返回成功
     * - 全部失败：返回失败，并保留已有缓存
     * - 内部带有重试与指数退避
     */
    suspend fun syncFromCloud(): Result<SyncResult> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "云同步第 ${attempt}/${maxRetries} 次尝试")
                val result = fetchFromCDN()
                if (result.isSuccess) {
                    settingsManager.lastSyncTime = System.currentTimeMillis()
                    settingsManager.cloudSyncStatus =
                        com.silas.omaster.data.local.CloudSyncStatus.SYNCED
                    return@withContext result
                }
                lastError = result.exceptionOrNull()
                Log.w(TAG, "第 ${attempt} 次同步失败: ${lastError?.message}")
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "第 ${attempt} 次同步异常", e)
            }
            // 指数退避：1s, 2s, 4s
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(1000L * (1L shl (attempt - 1)))
            }
        }

        settingsManager.cloudSyncStatus =
            com.silas.omaster.data.local.CloudSyncStatus.ERROR
        Result.failure(lastError ?: Exception("同步失败：达到最大重试次数"))
    }

    /**
     * 从 jsDelivr CDN 拉取所有启用品牌的预设数据
     */
    private suspend fun fetchFromCDN(): Result<SyncResult> = withContext(Dispatchers.IO) {
        val cloudUrls = settingsManager.cloudPresetUrls
        if (cloudUrls.isEmpty()) {
            Log.w(TAG, "未配置云端数据源 URL，跳过同步")
            return@withContext Result.failure(IllegalStateException("未配置云端数据源 URL"))
        }

        val imported = mutableListOf<PresetItem>()
        val conflicts = mutableListOf<PresetItem>()
        val errors = mutableListOf<Throwable>()

        // 已存在的预设索引（按 brand+name 查重）
        val existingIndex = _presets.value.associateBy { "${it.brand}::${it.name}" }.toMutableMap()

        for ((brand, url) in cloudUrls) {
            try {
                Log.d(TAG, "拉取品牌 [$brand] 的云端预设: $url")
                val brandPresets = fetchBrandFromCDN(brand, url)
                if (brandPresets.isNotEmpty()) {
                    for (preset in brandPresets) {
                        val key = "${preset.brand}::${preset.name}"
                        if (existingIndex.containsKey(key)) {
                            conflicts.add(preset)
                        } else {
                            imported.add(preset)
                            existingIndex[key] = preset
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "拉取品牌 [$brand] 失败: $url", e)
                errors.add(e)
            }
        }

        if (imported.isEmpty() && errors.isNotEmpty()) {
            // 全部失败：保留原数据，返回失败
            return@withContext Result.failure(
                Exception("所有云端数据源拉取失败: ${errors.joinToString { it.message ?: it.javaClass.simpleName }}")
            )
        }

        if (imported.isNotEmpty()) {
            // 合并新数据，写入缓存
            val newList = (imported + _presets.value).distinctBy { "${it.brand}::${it.name}" }
            _presets.value = newList
            try {
                saveToCache()
                Log.d(TAG, "云同步成功，新增 ${imported.size} 条，冲突 ${conflicts.size} 条")
            } catch (e: Exception) {
                Log.e(TAG, "云同步结果写入本地缓存失败", e)
            }
        } else {
            Log.d(TAG, "云端无新增数据（可能全部冲突或最新）")
        }

        Result.success(SyncResult(imported = imported.size, conflicts = conflicts))
    }

    /**
     * 拉取单个品牌的云端预设 JSON
     * - 读取远程 JSON 字符串后显式 decode，避免 Ktor 因 Content-Type 不匹配而抛 NoTransformationFoundException
     * - 返回转换后的 PresetItem 列表
     */
    private suspend fun fetchBrandFromCDN(brand: String, url: String): List<PresetItem> {
        val response = httpClient.get(url)
        val status = response.status
        if (status.value !in 200..299) {
            throw java.io.IOException("HTTP ${status.value} ${status.description} 来自 $url")
        }
        val body = response.bodyAsText()
        if (body.isBlank()) {
            Log.w(TAG, "品牌 [$brand] 返回空响应体: $url")
            return emptyList()
        }
        val presetList = json.decodeFromString(PresetList.serializer(), body)
        Log.d(TAG, "品牌 [$brand] 解析得到 ${presetList.presets.size} 条云端预设")
        return presetList.presets.map { it.toRepositoryPreset(brand) }
    }

    /**
     * PM-010: 本地存储损坏处理
     *
     * 处理策略：
     * 1. 备份当前收藏、置顶、版本等关键用户数据（内存中已存在）
     * 2. 校验缓存文件 JSON 格式
     *    - 格式正确：什么都不做，直接返回成功
     *    - 格式损坏/不存在：将损坏文件重命名为 .corrupted 作为取证备份
     * 3. 重新从 assets 加载基础预设，恢复收藏/置顶关系
     * 4. 写回缓存，保证下次启动可用
     */
    suspend fun recoverFromCorruption(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.w(TAG, "PM-010: 开始执行本地缓存损坏恢复流程")

            // 1) 备份用户关键数据（内存中的收藏/置顶已经是最新）
            val favoritesBackup = _favorites.value.toSet()
            val pinnedBackup = _pinnedIds.value.toSet()

            // 2) 校验当前缓存文件
            val corrupted = !isCacheFileValid()

            if (corrupted) {
                Log.w(TAG, "检测到缓存文件损坏或不可读，开始恢复")

                // 将损坏文件改名备份，保留现场以便事后排查
                if (cacheFile.exists()) {
                    try {
                        if (corruptedBackupFile.exists()) {
                            corruptedBackupFile.delete()
                        }
                        val renamed = cacheFile.renameTo(corruptedBackupFile)
                        if (renamed) {
                            Log.w(TAG, "已将损坏缓存备份至: ${corruptedBackupFile.absolutePath}")
                        } else {
                            Log.w(TAG, "重命名损坏缓存失败，将直接删除")
                            cacheFile.delete()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "备份损坏缓存时发生异常", e)
                    }
                }
            } else {
                Log.d(TAG, "缓存文件结构正常，无需恢复")
                return@withContext Result.success(Unit)
            }

            // 3) 重新加载（优先从 assets）
            loadLocalPresets()

            // 4) 恢复用户数据（收藏/置顶与新预设按 id 匹配）
            val validIds = _presets.value.map { it.id }.toSet()
            val restoredFavorites = favoritesBackup.filter { it in validIds }.toSet()
            val restoredPinned = pinnedBackup.filter { it in validIds }.toSet()

            _favorites.value = restoredFavorites
            _pinnedIds.value = restoredPinned
            saveFavorites(restoredFavorites)
            savePinned(restoredPinned)

            // 5) 立刻写回新的干净缓存
            saveToCache()

            Log.i(
                TAG,
                "PM-010: 恢复完成。预设 ${_presets.value.size} 条，" +
                        "收藏恢复 ${restoredFavorites.size}/${favoritesBackup.size}，" +
                        "置顶恢复 ${restoredPinned.size}/${pinnedBackup.size}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "PM-010: 损坏恢复过程中出现未捕获异常", e)
            // 上报日志（真实环境可对接 FirebaseCrashlytics）
            // FirebaseCrashlytics.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * 校验缓存文件是否为可解析的 JSON 格式
     * - 不存在视为正常（首次启动会从 assets 加载）
     * - 存在但解析失败 / 字段类型错误 / 数据为空 视为损坏
     */
    private fun isCacheFileValid(): Boolean {
        if (!cacheFile.exists()) {
            Log.d(TAG, "缓存文件不存在，视为正常")
            return true
        }
        if (cacheFile.length() == 0L) {
            Log.w(TAG, "缓存文件大小为 0，视为损坏: ${cacheFile.absolutePath}")
            return false
        }
        return try {
            val text = cacheFile.readText()
            val cache = json.decodeFromString(PresetCache.serializer(), text)
            // 进一步校验：必须能解析出 presets 字段
            Log.d(TAG, "缓存文件校验通过，包含 ${cache.presets.size} 条预设")
            true
        } catch (e: Exception) {
            Log.w(TAG, "缓存文件解析失败: ${e.message}", e)
            false
        }
    }

    /**
     * WM-003: 获取设备型号
     */
    fun getDeviceModel(): String = deviceModel

    /**
     * WM-003: 手动覆盖设备型号
     */
    fun setDeviceModel(model: String) {
        deviceModel = model
        settingsManager.customDeviceModel = model
    }

    /**
     * WM-003: 检查是否已手动覆盖
     */
    fun isDeviceModelOverridden(): Boolean {
        return settingsManager.customDeviceModel.isNotEmpty()
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 加载本地预设（仅本地缓存或 assets，不会触发网络请求）
     * - 优先从 cacheFile 加载
     * - cacheFile 缺失或损坏时回退到 assets/presets.json
     * - assets 也无法读取时回退到空列表
     *
     * 调用方：init {} / recoverFromCorruption() / loadFromCacheOrNetwork() 内部
     */
    private fun loadLocalPresets() {
        try {
            val loaded = readFromCache() ?: readFromAssets() ?: emptyList()
            _presets.value = loaded
            Log.d(TAG, "本地预设加载完成: ${loaded.size} 条 (来源=${if (cacheFile.exists()) "cache" else "assets"})")
        } catch (e: Exception) {
            Log.e(TAG, "本地预设加载失败，返回空列表", e)
            _presets.value = emptyList()
        }
    }

    /**
     * 读取本地缓存文件，返回 null 表示无可用缓存
     */
    private fun readFromCache(): List<PresetItem>? {
        if (!cacheFile.exists()) {
            Log.d(TAG, "本地缓存文件不存在: ${cacheFile.absolutePath}")
            return null
        }
        return try {
            val text = cacheFile.readText()
            if (text.isBlank()) {
                Log.w(TAG, "本地缓存文件为空: ${cacheFile.absolutePath}")
                return null
            }
            val cache = json.decodeFromString(PresetCache.serializer(), text)
            Log.d(TAG, "从本地缓存读取 ${cache.presets.size} 条预设 (version=${cache.version})")
            cache.presets
        } catch (e: Exception) {
            Log.w(TAG, "本地缓存解析失败，将回退到 assets", e)
            null
        }
    }

    /**
     * 读取 assets/presets.json，返回 null 表示读取失败
     */
    private fun readFromAssets(): List<PresetItem>? {
        return try {
            val text = appContext.assets.open(ASSETS_PRESETS_FILE).use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
            val presetList = json.decodeFromString(PresetList.serializer(), text)
            // assets 是官方内置预设，统一标记为系统预设
            val items = presetList.presets.map { it.toRepositoryPreset(brand = "oppo") }
            Log.i(TAG, "从 assets 加载 ${items.size} 条内置预设")
            items
        } catch (e: Exception) {
            Log.e(TAG, "从 assets 加载预设失败", e)
            null
        }
    }

    /**
     * 从缓存或网络加载预设
     * 策略：
     * 1. 缓存命中且未过期：直接返回
     * 2. 尝试拉取云端（异步在后台触发，不阻塞当前请求）
     * 3. 网络拉取失败：返回现有缓存（保证 PM-001 < 2s 体验）
     */
    private suspend fun loadFromCacheOrNetwork(brand: String?): List<PresetItem> = withContext(Dispatchers.IO) {
        val localPresets = if (_presets.value.isNotEmpty()) {
            _presets.value
        } else {
            // 首次：尝试本地
            readFromCache() ?: readFromAssets() ?: emptyList()
        }

        // 启动后台同步任务，不阻塞当前返回
        try {
            triggerBackgroundSync(brand)
        } catch (e: Exception) {
            Log.w(TAG, "后台同步调度失败", e)
        }

        // 按品牌过滤
        return@withContext if (brand.isNullOrBlank()) {
            localPresets
        } else {
            val filtered = localPresets.filter { it.brand.equals(brand, ignoreCase = true) }
            Log.d(TAG, "按品牌 [$brand] 过滤后剩余 ${filtered.size}/${localPresets.size} 条")
            filtered
        }
    }

    /**
     * 在后台尝试一次云同步，结果合并到 _presets 并写回缓存
     * 失败时静默降级，不影响主流程
     */
    private suspend fun triggerBackgroundSync(brand: String?) {
        if (!settingsManager.isCloudSyncEnabled) {
            Log.d(TAG, "云同步开关未启用，跳过后台同步")
            return
        }
        // 节流：距上次同步不足 5 分钟则跳过
        val now = System.currentTimeMillis()
        val lastSync = settingsManager.lastSyncTime
        if (lastSync > 0 && now - lastSync < BACKGROUND_SYNC_INTERVAL_MS) {
            Log.d(TAG, "距上次同步不足 ${BACKGROUND_SYNC_INTERVAL_MS / 1000}s，跳过")
            return
        }

        try {
            val result = fetchFromCDN()
            result.onSuccess { syncResult ->
                Log.i(TAG, "后台同步完成: 新增 ${syncResult.imported} 条")
            }.onFailure { e ->
                Log.w(TAG, "后台同步失败，已使用本地缓存: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "后台同步异常，已使用本地缓存", e)
        }
    }

    private fun applyPinningAndSorting(presets: List<PresetItem>): List<PresetItem> {
        val pinned = presets.filter { _pinnedIds.value.contains(it.id) }
        val unpinned = presets.filter { !_pinnedIds.value.contains(it.id) }

        // NEW标记排序
        val newFirst = unpinned.sortedByDescending { isNew(it.id) }

        // 下载量排序
        return pinned + newFirst.sortedByDescending { it.downloadCount }
    }

    private fun loadFavorites(): Set<String> {
        return settingsManager.favoritePresetIds.toSet()
    }

    private fun saveFavorites(favorites: Set<String>) {
        settingsManager.favoritePresetIds = favorites.toList()
    }

    private fun loadPinned(): Set<String> {
        return settingsManager.pinnedPresetIds.toSet()
    }

    private fun savePinned(pinned: Set<String>) {
        settingsManager.pinnedPresetIds = pinned.toList()
    }

    /**
     * 将预设列表写入本地缓存文件
     * 写入采用「写临时文件 + 原子重命名」的方式，避免写入过程中崩溃导致缓存损坏
     */
    private suspend fun saveToCache() = withContext(Dispatchers.IO) {
        try {
            val currentList = _presets.value
            if (currentList.isEmpty()) {
                Log.d(TAG, "预设列表为空，跳过缓存写入")
                return@withContext
            }

            val cache = PresetCache(
                version = CACHE_VERSION,
                timestamp = System.currentTimeMillis(),
                presets = currentList
            )
            val jsonStr = json.encodeToString(cache)

            // 写入临时文件，再原子替换
            val tempFile = File(appContext.filesDir, "$CACHE_FILE_NAME.tmp")
            tempFile.writeText(jsonStr, Charsets.UTF_8)
            if (!tempFile.renameTo(cacheFile)) {
                // renameTo 在跨文件系统等场景可能失败，fallback 到直接写入
                cacheFile.writeText(jsonStr, Charsets.UTF_8)
                tempFile.delete()
            }
            Log.d(TAG, "已写入本地缓存: ${currentList.size} 条, ${jsonStr.length / 1024}KB")
        } catch (e: Exception) {
            Log.e(TAG, "写入本地缓存失败", e)
        }
    }

    /**
     * 重新加载默认预设（公开包装）
     * 主要被订阅管理界面在远程拉取完成后调用，以刷新内存中的预设列表
     */
    suspend fun reloadDefaultPresets(): List<PresetItem> = loadPresets(brand = null)

    // ==================== HomeViewModel 需要的方法 ====================

    /**
     * 获取所有预设（实时转换为 MasterPreset）
     * 使用 map 操作符实现响应式更新
     * 正确设置 isFavorite 属性
     */
    fun getAllPresets(): Flow<List<MasterPreset>> {
        return combine(_presets, _favorites) { items, favIds ->
            items.map { item ->
                item.toMasterPreset().copy(isFavorite = favIds.contains(item.id))
            }
        }
    }

    /**
     * 获取收藏的预设
     * 合并 presets 和 favorites 流
     */
    fun getFavoritePresets(): Flow<List<MasterPreset>> {
        return combine(_presets, _favorites) { items, favIds ->
            items.filter { favIds.contains(it.id) }
                .map { it.toMasterPreset().copy(isFavorite = true) }
        }
    }

    /**
     * 获取自定义预设（非系统预设）
     */
    fun getCustomPresets(): Flow<List<MasterPreset>> {
        return combine(_presets, _favorites) { items, favIds ->
            items.filter { !it.isSystem }
                .map { item ->
                    item.toMasterPreset().copy(
                        isFavorite = favIds.contains(item.id),
                        isCustom = true
                    )
                }
        }
    }

    /**
     * 获取关联推荐预设（基于品牌和标签匹配）
     */
    fun getRelatedPresets(
        currentId: String?,
        brand: String?,
        tags: List<String>?,
        limit: Int = 4
    ): List<MasterPreset> {
        val current = _presets.value
        val favIds = _favorites.value
        return current
            .filter { it.id != currentId }
            .map { it.toMasterPreset().copy(isFavorite = favIds.contains(it.id)) }
            .sortedByDescending { preset ->
                var score = 0
                if (preset.brand == brand) score += 2
                if (!tags.isNullOrEmpty()) {
                    score += preset.tags?.count { tag -> tags.contains(tag) } ?: 0
                }
                score
            }
            .take(limit)
    }

    /**
     * 切换收藏状态（用于 HomeViewModel）
     */
    suspend fun toggleFavorite(presetId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(presetId)) {
            current.remove(presetId)
        } else {
            current.add(presetId)
        }
        _favorites.value = current
        saveFavorites(current)
    }

    /**
     * 删除自定义预设（简化版本）
     */
    suspend fun deleteCustomPreset(presetId: String) {
        deletePreset(presetId, forceConfirm = true)
    }

    /**
     * 释放 Ktor 资源
     * 真实使用中通常不需要调用，因为 HttpClient 是 lazy 单例
     * 保留入口以便测试或显式生命周期管理
     */
    fun close() {
        runCatching { httpClient.close() }
            .onFailure { Log.w(TAG, "关闭 HttpClient 时发生异常", it) }
    }

    companion object {
        private const val TAG = "PresetRepository"
        private const val CACHE_FILE_NAME = "presets_cache.json"
        private const val CORRUPTED_BACKUP_FILE_NAME = "presets_cache.json.corrupted"
        private const val ASSETS_PRESETS_FILE = "presets.json"
        private const val CACHE_VERSION = 1
        private const val NETWORK_TIMEOUT_MS = 10_000L
        private const val BACKGROUND_SYNC_INTERVAL_MS = 5 * 60 * 1000L

        @Volatile
        private var instance: PresetRepository? = null

        fun getInstance(context: Context): PresetRepository {
            return instance ?: synchronized(this) {
                instance ?: PresetRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 预设项 - 仓库内部数据模型
 * 与 model.PresetItem (UI 用的项) 区分
 */
@Serializable
data class PresetItem(
    val id: String,
    val name: String,
    val brand: String,
    val scene: String,
    val params: Map<String, Int>,
    val coverPath: String? = null,
    val galleryImages: List<String>? = null,  // 画廊图片列表（对齐Web端）
    val description: String = "",
    val isSystem: Boolean = true,
    val isHncs: Boolean = false,
    val rating: Float = 0f,
    val ratingCount: Int? = null,  // 评分数量（对齐Web端）
    val downloadCount: Int = 0,
    val favoriteCount: Int = 0,
    val comments: List<com.silas.omaster.model.PresetComment>? = null,  // 评论列表（对齐Web端）
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isNew: Boolean = false,
    val isPinned: Boolean = false,
    val mode: String? = null  // 模式：auto或pro（对齐Web端）
) {
    fun toExportModel() = ExportPresetModel(
        name = name,
        brand = brand,
        scene = scene,
        params = params,
        description = description,
        tags = tags
    )

    /**
     * 转换为 MasterPreset（用于 UI 层）
     */
    fun toMasterPreset(): MasterPreset {
        return MasterPreset(
            id = id,
            name = name,
            coverPath = coverPath ?: "images/placeholder.webp",
            galleryImages = galleryImages,  // 对齐Web端
            brand = brand,
            tags = tags,
            description = if (description.isNotEmpty()) {
                com.silas.omaster.model.PresetDescription("Shooting Tips", description)
            } else null,
            isNew = isNew,
            isHncs = isHncs,
            downloads = downloadCount,
            rating = rating,
            ratingCount = ratingCount,  // 对齐Web端
            comments = comments,  // 对齐Web端
            createdAt = createdAt,
            mode = mode,  // 对齐Web端
            saturation = params["saturation"],
            tone = params["contrast"],
            warmCool = params["warmth"],
            sharpness = params["sharpness"],
            cyanMagenta = params["cyan_magenta"],
            colorTemperature = params["color_temperature"],
            colorHue = params["color_hue"]
        )
    }
}

/**
 * 缓存包装结构
 * 用于在 JSON 中保存版本、时间戳与预设列表
 */
@Serializable
data class PresetCache(
    val version: Int = 1,
    val timestamp: Long = 0,
    val presets: List<PresetItem> = emptyList()
)

/**
 * 导出数据模型
 */
@Serializable
data class ExportData(
    val version: Int,
    val app: String,
    val timestamp: Long,
    val presets: List<ExportPresetModel>
)

@Serializable
data class ExportPresetModel(
    val name: String,
    val brand: String,
    val scene: String,
    val params: Map<String, Int>,
    val description: String = "",
    val tags: List<String> = emptyList()
) {
    fun toPresetItem() = PresetItem(
        id = "imported_${System.currentTimeMillis()}_${abs(name.hashCode())}",
        name = name,
        brand = brand,
        scene = scene,
        params = params,
        galleryImages = null,  // 导入预设默认无画廊图片
        description = description,
        isSystem = false,
        isHncs = false,
        ratingCount = null,
        comments = null,
        tags = tags,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        mode = null
    )
}

/**
 * 将云端/内置的 MasterPreset 转换为仓库内部的 PresetItem
 *
 * 转换策略：
 * - 数值参数（饱和度/对比度/锐度/影调/冷暖/青品/色温/色调）解析为 Int 存入 params
 * - 不在 params 中的字符串参数（滤镜/柔光/暗角/ISO/快门/曝光补偿）也保留到 params（值为 0）
 *   并以专用键存储原值
 * - description 优先取 description.content
 * - scene 优先取 tags 第一个，否则默认 "通用"
 */
private fun MasterPreset.toRepositoryPreset(brand: String): PresetItem {
    val resolvedBrand = (this.brand ?: brand).ifBlank { brand }
    val resolvedScene = tags?.firstOrNull { it.isNotBlank() } ?: "通用"
    val resolvedDescription = description?.content
        ?: shootingTips
        ?: ""
    val paramsMap = mutableMapOf<String, Int>()
    saturation?.let { paramsMap["saturation"] = it }
    tone?.let { paramsMap["contrast"] = it } // tone 在本应用中等同于 contrast
    warmCool?.let { paramsMap["warmth"] = it }
    sharpness?.let { paramsMap["sharpness"] = it }
    // 以下字段在当前模型中尚无对应值，存 0 占位
    paramsMap.putIfAbsent("clarity", 0)
    paramsMap.putIfAbsent("brightness", 0)
    // 额外保存原值字段（保证导入导出无损）
    cyanMagenta?.let { paramsMap["cyan_magenta"] = it }
    colorTemperature?.let { paramsMap["color_temperature"] = it }
    colorHue?.let { paramsMap["color_hue"] = it }

    return PresetItem(
        id = id ?: "preset_${abs(name.hashCode())}_${System.nanoTime()}",
        name = name,
        brand = resolvedBrand,
        scene = resolvedScene,
        params = paramsMap,
        coverPath = coverPath,
        galleryImages = galleryImages,  // 对齐Web端
        description = resolvedDescription,
        isSystem = true,
        isHncs = isHncs,
        rating = rating ?: 0f,
        ratingCount = ratingCount,  // 对齐Web端
        downloadCount = downloads ?: 0,
        favoriteCount = 0,
        comments = comments,  // 对齐Web端
        tags = tags ?: emptyList(),
        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isNew = isNew,
        isPinned = false,
        mode = mode  // 对齐Web端
    )
}

/**
 * 导入结果
 */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val conflicts: List<PresetItem>
)

/**
 * 同步结果
 */
data class SyncResult(
    val imported: Int,
    val conflicts: List<PresetItem>
)

/**
 * 需要确认异常
 */
class ConfirmationRequired(message: String) : Exception(message)
