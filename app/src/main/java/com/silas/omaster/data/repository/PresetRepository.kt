package com.silas.omaster.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.SubscriptionManager
import com.silas.omaster.network.PresetRemoteManager
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val subscriptionManager = SubscriptionManager.getInstance(context)
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
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // 重载错误状态（用于向 UI 层反馈 forceReloadFromFiles 的失败信息）
    private val _reloadError = MutableStateFlow<String?>(null)
    val reloadError: StateFlow<String?> = _reloadError.asStateFlow()

    /**
     * forceReloadFromFiles 专用锁，防止多线程并发调用导致数据竞态
     * 解决：两个线程同时调用时，_presets.value = emptyList() 导致互相覆盖的问题
     */
    private val forceReloadLock = Mutex()

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
            engine {
                maxConnectionsCount = 32
                endpoint.maxConnectionsPerRoute = 8
                endpoint.connectTimeout = NETWORK_CONNECT_TIMEOUT_MS
                endpoint.socketTimeout = NETWORK_READ_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = NETWORK_READ_TIMEOUT_MS
                connectTimeoutMillis = NETWORK_CONNECT_TIMEOUT_MS
                socketTimeoutMillis = NETWORK_READ_TIMEOUT_MS
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay(base = 2.0, maxDelayMs = 4_000L)
            }
            expectSuccess = false
        }
    }

    // 协程作用域用于结构化并发
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 从 SettingsManager 初始化搜索历史
        _searchHistory.value = settingsManager.searchHistory

        // 使用协程替代原始 Thread ，实现结构化并发
        // 优势：可取消、异常处理更完善、与生命周期更好绑定
        repositoryScope.launch {
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts) {
                try {
                    loadLocalPresets()
                    Log.i(TAG, "本地预设初始化完成: ${_presets.value.size} 条")
                    break
                } catch (e: Exception) {
                    attempts++
                    Log.e(TAG, "本地预设初始化失败 (尝试 $attempts/$maxAttempts)", e)
                    if (attempts < maxAttempts) {
                        delay(2000L * attempts)  // 指数退避
                    } else {
                        Log.e(TAG, "本地预设初始化彻底失败,使用空列表")
                        _presets.value = emptyList()
                    }
                }
            }

            // 本地预设加载完成
            Log.i(TAG, "PresetRepository 初始化完成: ${_presets.value.size} 条预设")
        }
    }

    /**
     * PM-001: 预设瀑布流浏览
     * 列表加载 < 2s（100 条内）
     * 滚动 FPS ≥ 55（由UI层保证）
     * 长按卡片弹出操作菜单（由UI层保证）
     */
    suspend fun loadPresets(brand: String? = null): List<PresetItem> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val presets = mutableListOf<PresetItem>()

        // 从 assets 加载（内置预设，最可靠的数据源）
        try {
            val assetsPresets = readFromAssets() ?: emptyList()
            presets.addAll(assetsPresets)
        } catch (e: Exception) {
            Log.e(TAG, "加载 assets 预设失败", e)
        }

        // 从本地缓存加载
        try {
            val localPresets = readFromCache() ?: emptyList()
            presets.addAll(localPresets)
        } catch (e: Exception) {
            Log.e(TAG, "加载本地缓存预设失败", e)
        }

        // 从网络加载（如果启用）
        try {
            val networkPresets = loadFromNetwork()
            presets.addAll(networkPresets)
        } catch (e: Exception) {
            Log.e(TAG, "加载网络预设失败", e)
        }

        // 按品牌过滤
        val filtered = if (brand.isNullOrBlank()) presets
        else presets.filter { it.brand.equals(brand, ignoreCase = true) }

        // 应用置顶排序
        val sortedPresets = applyPinningAndSorting(filtered)

        _presets.value = sortedPresets

        // 网络加载成功后保存到本地缓存
        try {
            saveToCache()
        } catch (e: Exception) {
            Log.e(TAG, "加载后保存缓存失败", e)
        }

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
            // 搜索时记录搜索历史
            addSearchQuery(searchQuery)
        }

        return filtered
    }

    /**
     * 添加搜索关键词到历史记录
     * 去重、最多保留20条、新搜索词置顶
     */
    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        // 去重：移除已存在的相同搜索词
        current.remove(query)
        // 新搜索词置顶
        current.add(0, query)
        // 最多保留20条
        val trimmed = current.take(20)
        _searchHistory.value = trimmed
        settingsManager.searchHistory = trimmed
    }

    /**
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        settingsManager.searchHistory = emptyList()
    }

    /**
     * 从哈苏色彩模式创建预设
     * 将 HasselbladScreen 中的 ColorMode/SceneMode 转换为可保存的自定义预设
     */
    suspend fun createPresetFromHasselbladMode(
        colorMode: com.silas.omaster.ui.features.ColorMode,
        sceneMode: com.silas.omaster.ui.features.SceneMode? = null
    ): Result<PresetItem> = withContext(Dispatchers.IO) {
        val tags = mutableListOf("哈苏", "色彩模式")
        val description = buildString {
            append(colorMode.description)
            if (sceneMode != null) {
                append("\n场景：${sceneMode.name} - ${sceneMode.description}")
            }
        }

        val preset = PresetItem(
            id = "hasselblad_${colorMode.id}_${System.currentTimeMillis()}",
            name = colorMode.name,
            brand = "hasselblad",
            scene = sceneMode?.name ?: "色彩模式",
            params = colorMode.params,
            coverPath = null,
            galleryImages = null,
            description = description,
            isSystem = false,
            isHncs = true,
            rating = 0f,
            ratingCount = null,
            downloadCount = 0,
            favoriteCount = 0,
            comments = null,
            tags = tags,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isNew = true,
            isPinned = false,
            mode = null,
            author = "@哈苏色彩科学",
            sections = null,
            filter = null,
            softLight = null,
            vignette = null
        )

        val current = _presets.value.toMutableList()
        current.add(0, preset)
        _presets.value = current

        saveToCache()

        Result.success(preset)
    }


    fun isFavorite(presetId: String): Boolean = _favorites.value.contains(presetId)

    /**
     * PM-004: 自定义预设创建
     */
    suspend fun createCustomPreset(
        name: String,
        params: Map<String, Int>,
        coverPath: String? = null,
        description: String = "",
        sections: List<com.silas.omaster.model.PresetSection>? = null
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
            mode = null,
            sections = sections
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
            sections = updates["sections"] as? List<com.silas.omaster.model.PresetSection> ?: preset.sections,
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

        // 删除封面文件（使用应用私有目录拼接完整路径）
        preset.coverPath?.let { path ->
            runCatching {
                val file = if (File(path).isAbsolute) File(path) else File(appContext.filesDir, path)
                file.delete()
            }
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
            // 文件大小检查（防止恶意大文件导致 OOM）
            if (file.length() > MAX_IMPORT_FILE_SIZE_BYTES) {
                return@withContext Result.failure(IllegalArgumentException("导入文件大小超过5MB限制"))
            }

            val content = file.readText()

            // 再次校验读取后内容大小
            if (content.length > MAX_IMPORT_FILE_SIZE_BYTES) {
                return@withContext Result.failure(IllegalArgumentException("导入文件大小超过5MB限制"))
            }

            val data = json.decodeFromString<ExportData>(content)

            // PM-007: 校验版本号
            if (data.version > 2) {
                return@withContext Result.failure(IllegalArgumentException("版本号不兼容"))
            }

            // 校验预设数量
            if (data.presets.size > 100) {
                return@withContext Result.failure(IllegalArgumentException("单次导入预设不能超过100条"))
            }

            var imported = 0
            var skipped = 0
            val conflicts = mutableListOf<PresetItem>()
            val validationErrors = mutableListOf<String>()

            for ((index, exportModel) in data.presets.withIndex()) {
                // 严格验证：预设名称非空
                if (exportModel.name.isBlank()) {
                    validationErrors.add("第${index + 1}条预设：名称不能为空")
                    skipped++
                    continue
                }

                // 验证名称长度限制
                if (exportModel.name.length > 50) {
                    validationErrors.add("第${index + 1}条预设：名称长度超过50字限制")
                    skipped++
                    continue
                }

                // 验证描述长度限制
                if (exportModel.description.length > 500) {
                    validationErrors.add("第${index + 1}条预设「${exportModel.name}」：描述长度超过500字限制")
                    skipped++
                    continue
                }

                // 验证危险字符（防止注入/XSS）
                val dangerousPattern = Regex("[<>\"'&\\\\]|(script)|(javascript)|(on\\w+=)", RegexOption.IGNORE_CASE)
                if (dangerousPattern.containsMatchIn(exportModel.name)) {
                    validationErrors.add("第${index + 1}条预设「${exportModel.name}」：名称包含非法字符")
                    skipped++
                    continue
                }
                if (dangerousPattern.containsMatchIn(exportModel.description)) {
                    validationErrors.add("第${index + 1}条预设「${exportModel.name}」：描述包含非法字符")
                    skipped++
                    continue
                }

                // 验证 mode 值（如果提供）
                exportModel.mode?.let { mode ->
                    if (mode !in listOf("auto", "pro", null)) {
                        validationErrors.add("第${index + 1}条预设「${exportModel.name}」：模式值无效($mode)")
                        skipped++
                        return@let
                    }
                }

                // 验证 params 非空
                if (exportModel.params.isEmpty()) {
                    validationErrors.add("第${index + 1}条预设「${exportModel.name}」：参数不能为空")
                    skipped++
                    continue
                }

                // 检查与系统预设重名
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

            if (validationErrors.isNotEmpty()) {
                Log.w(TAG, "导入验证错误: ${validationErrors.joinToString("; ")}")
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
     * 从本地存储加载预设（缓存 + assets）
     */
    private fun loadFromLocal(): List<PresetItem> {
        return readFromCache() ?: readFromAssets() ?: emptyList()
    }

    /**
     * 从网络加载预设
     * 遍历 SubscriptionManager 中所有已启用的订阅源，
     * 通过 PresetRemoteManager 拉取远端 JSON 并转换为 PresetItem
     */
    private suspend fun loadFromNetwork(): List<PresetItem> = withContext(Dispatchers.IO) {
        val enabledSubscriptions = subscriptionManager.subscriptionsFlow.value.filter { it.isEnabled }
        if (enabledSubscriptions.isEmpty()) {
            Log.d(TAG, "无已启用的订阅源，跳过网络加载")
            return@withContext emptyList<PresetItem>()
        }

        val remotePresets = mutableListOf<PresetItem>()
        for (subscription in enabledSubscriptions) {
            try {
                val result = PresetRemoteManager.fetchAndSave(appContext, subscription.url)
                if (result.isSuccess) {
                    val presetList = result.getOrNull()
                    if (presetList != null) {
                        val brand = presetList.name?.lowercase() ?: "oppo"
                        val items = presetList.presets.map { it.toRepositoryPreset(brand = brand) }
                        remotePresets.addAll(items)
                        Log.d(TAG, "从网络加载订阅 [${subscription.name}] 成功: ${items.size} 条")
                    }
                } else {
                    Log.w(TAG, "从网络加载订阅 [${subscription.name}] 失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "从网络加载订阅 [${subscription.name}] 异常", e)
            }
        }
        Log.d(TAG, "网络加载完成，共获取 ${remotePresets.size} 条预设")
        remotePresets
    }

    /**
     * 将下载的预设保存到本地缓存
     */
    private suspend fun saveToLocal(presets: List<PresetItem>) {
        val current = _presets.value.toMutableList()
        // 按名称+品牌去重，远程数据覆盖本地同名的
        val remoteKeys = presets.map { "${it.name}_${it.brand}" }.toSet()
        val merged = current.filter { "${it.name}_${it.brand}" !in remoteKeys } + presets
        _presets.value = merged
        saveToCache()
    }

    /**
     * 从缓存或网络加载预设
     * 策略：
     * 1. 非强制刷新时，优先返回本地缓存
     * 2. 尝试从网络拉取（使用 SubscriptionManager 的已启用订阅源）
     * 3. 网络成功则更新本地缓存并返回
     * 4. 网络失败则回退本地缓存
     * 5. 保证 PM-001 < 2s 体验
     */
    private suspend fun loadFromCacheOrNetwork(brand: String?, forceRefresh: Boolean = false): List<PresetItem> = withContext(Dispatchers.IO) {
        // 非强制刷新时，优先返回本地缓存
        if (!forceRefresh) {
            val cached = loadFromLocal()
            if (cached.isNotEmpty()) {
                val result = if (brand.isNullOrBlank()) cached
                else cached.filter { it.brand.equals(brand, ignoreCase = true) }
                Log.d(TAG, "使用本地缓存: ${result.size} 条, brand=$brand")
                return@withContext result
            }
        }

        // 尝试从网络加载
        try {
            val remote = loadFromNetwork()
            if (remote.isNotEmpty()) {
                saveToLocal(remote)
                val result = if (brand.isNullOrBlank()) remote
                else remote.filter { it.brand.equals(brand, ignoreCase = true) }
                Log.d(TAG, "网络加载成功: ${result.size} 条, brand=$brand")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w(TAG, "网络加载失败，回退到本地缓存", e)
        }

        // 回退到本地
        val local = loadFromLocal()
        val result = if (brand.isNullOrBlank()) local
        else local.filter { it.brand.equals(brand, ignoreCase = true) }
        Log.d(TAG, "使用本地回退: ${result.size} 条, brand=$brand")
        result
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
            // 即使列表为空也写入缓存，避免清空操作后重启数据"复活"
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

    /**
     * 强制从文件重新加载预设（清空内存缓存后重新读取）
     * 用于订阅更新后刷新数据，解决 loadFromCacheOrNetwork 因内存缓存非空而跳过文件读取的问题
     *
     * 使用 Mutex 确保多线程并发调用时的数据一致性（避免 A 线程清空后被 B 线程覆盖）
     * 失败时通过 reloadError StateFlow 向 UI 层反馈错误信息
     */
    suspend fun forceReloadFromFiles(): List<PresetItem> = withContext(Dispatchers.IO) {
        forceReloadLock.withLock {
            try {
                _reloadError.value = null
                _presets.value = emptyList()
                loadLocalPresets()
                if (_presets.value.isEmpty()) {
                    val errorMsg = "强制重载后预设列表为空，本地数据可能损坏"
                    Log.e(TAG, errorMsg)
                    _reloadError.value = errorMsg
                }
                _presets.value
            } catch (e: Exception) {
                val errorMsg = "强制重载预设失败: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _reloadError.value = errorMsg
                _presets.value
            }
        }
    }

    /**
     * 清除重载错误状态（UI 层消费后调用）
     */
    fun clearReloadError() {
        _reloadError.value = null
    }

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
     * 切换前验证预设是否存在于本地列表，避免收藏不存在的预设
     */
    suspend fun toggleFavorite(presetId: String) {
        // 验证预设是否存在
        val exists = _presets.value.any { it.id == presetId }
        if (!exists) {
            Log.w(TAG, "toggleFavorite: 预设 $presetId 不存在，跳过收藏操作")
            return
        }

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
     * 释放 Ktor 资源与协程作用域
     * 真实使用中通常不需要调用，因为 HttpClient 是 lazy 单例
     * 保留入口以便测试或显式生命周期管理
     */
    fun close() {
        runCatching { repositoryScope.coroutineContext.cancel() }
            .onFailure { Log.w(TAG, "取消 repositoryScope 时发生异常", it) }
        runCatching { httpClient.close() }
            .onFailure { Log.w(TAG, "关闭 HttpClient 时发生异常", it) }
    }

    companion object {
        private const val TAG = "PresetRepository"
        private const val CACHE_FILE_NAME = "presets_cache.json"
        private const val CORRUPTED_BACKUP_FILE_NAME = "presets_cache.json.corrupted"
        private const val ASSETS_PRESETS_FILE = "presets.json"
        private const val CACHE_VERSION = 1
        private const val NETWORK_CONNECT_TIMEOUT_MS = 10_000L
        private const val NETWORK_READ_TIMEOUT_MS = 30_000L
        private const val MAX_IMPORT_FILE_SIZE_BYTES = 5 * 1024 * 1024L

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
    val mode: String? = null,  // 模式：auto或pro（对齐Web端）
    val author: String = "@OPPO影像",  // 作者
    val sections: List<com.silas.omaster.model.PresetSection>? = null,  // 动态参数分组（保留原始展示数据）
    val filter: String? = null,  // 滤镜
    val softLight: String? = null,  // 柔光
    val vignette: String? = null,  // 暗角
    // 修复数据丢失：以下字符串型专业参数在 params(Map<String,Int>) 中无法存储，
    // 新增专用字段确保 MasterPreset↔PresetItem 往返不丢失
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val exposureCompensation: String? = null,
    val whiteBalance: String? = null,
    val colorTone: String? = null
) {
    fun toExportModel() = ExportPresetModel(
        name = name,
        brand = brand,
        scene = scene,
        params = params,
        description = description,
        tags = tags,
        mode = mode
    )

    /**
     * 转换为 MasterPreset（用于 UI 层）
     */
    fun toMasterPreset(): MasterPreset {
        return MasterPreset(
            id = id,
            name = name,
            coverPath = coverPath ?: "images/placeholder.webp",
            galleryImages = galleryImages,
            author = author,
            brand = brand,
            tags = tags,
            description = if (description.isNotEmpty()) {
                com.silas.omaster.model.PresetDescription("Shooting Tips", description)
            } else null,
            sections = sections,  // 保留原始动态参数分组
            isNew = isNew,
            isHncs = isHncs,
            downloads = downloadCount,
            rating = rating,
            ratingCount = ratingCount,
            comments = comments,
            createdAt = createdAt,
            mode = mode,
            filter = filter,
            softLight = softLight,
            vignette = vignette,
            saturation = params["saturation"],
            tone = params["contrast"],
            warmCool = params["warmth"],
            sharpness = params["sharpness"],
            cyanMagenta = params["cyan_magenta"],
            colorTemperature = params["color_temperature"],
            colorHue = params["color_hue"],
            // 修复数据丢失：从专用字段读取字符串型专业参数，而非 params Map
            exposureCompensation = exposureCompensation,
            iso = iso,
            shutterSpeed = shutterSpeed,
            whiteBalance = whiteBalance,
            colorTone = colorTone
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
    val tags: List<String> = emptyList(),
    val mode: String? = null
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
        mode = mode
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

    // 优先从数值字段提取参数
    saturation?.let { paramsMap["saturation"] = it }
    tone?.let { paramsMap["contrast"] = it }
    warmCool?.let { paramsMap["warmth"] = it }
    sharpness?.let { paramsMap["sharpness"] = it }
    cyanMagenta?.let { paramsMap["cyan_magenta"] = it }
    colorTemperature?.let { paramsMap["color_temperature"] = it }
    colorHue?.let { paramsMap["color_hue"] = it }

    // 当数值字段为空时，从 sections.items 的 value 字符串中解析参数
    // JSON格式如: {"label": "@string/param_saturation", "value": "+19"}
    if (paramsMap.isEmpty() || !paramsMap.containsKey("saturation")) {
        parseParamsFromSections(sections, paramsMap)
    }

    // 默认值
    paramsMap.putIfAbsent("clarity", 0)
    paramsMap.putIfAbsent("brightness", 0)

    return PresetItem(
        id = id ?: "preset_${abs(name.hashCode())}_${System.nanoTime()}",
        name = name,
        brand = resolvedBrand,
        scene = resolvedScene,
        params = paramsMap,
        coverPath = coverPath,
        galleryImages = galleryImages,
        description = resolvedDescription,
        isSystem = true,
        isHncs = isHncs,
        rating = rating ?: 0f,
        ratingCount = ratingCount,
        downloadCount = downloads ?: 0,
        favoriteCount = 0,
        comments = comments,
        tags = tags ?: emptyList(),
        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isNew = isNew,
        isPinned = false,
        mode = mode,
        author = this.author ?: "@OPPO影像",
        sections = sections,  // 保留原始动态参数分组
        filter = filter,
        softLight = softLight,
        vignette = vignette,
        // 修复数据丢失：保存字符串型专业参数
        iso = this.iso,
        shutterSpeed = this.shutterSpeed,
        exposureCompensation = this.exposureCompensation,
        whiteBalance = this.whiteBalance,
        colorTone = this.colorTone
    )
}

/**
 * 从 sections.items 的 value 字符串中解析数值参数
 * JSON格式如: {"label": "@string/param_saturation", "value": "+19"}
 * label 中的 @string/ 前缀会被去除，匹配参数名
 */
private fun parseParamsFromSections(
    sections: List<com.silas.omaster.model.PresetSection>?,
    paramsMap: MutableMap<String, Int>
) {
    if (sections.isNullOrEmpty()) return

    // label后缀到参数键名的映射
    val labelToKey = mapOf(
        "param_saturation" to "saturation",
        "param_tone_curve" to "contrast",
        "param_warm_cool" to "warmth",
        "param_cyan_magenta" to "cyan_magenta",
        "param_sharpness" to "sharpness",
        "param_color_temp" to "color_temperature",
        "param_tone" to "color_hue",
        "param_filter" to "filter",
        "param_soft_light" to "soft_light",
        "param_vignette" to "vignette"
    )

    for (section in sections) {
        for (item in section.items) {
            // 提取label中的参数标识: "@string/param_saturation" -> "param_saturation"
            val labelKey = item.label
                .removePrefix("@string/")
                .trim()

            val paramKey = labelToKey[labelKey] ?: continue

            // 跳过已存在的参数（数值字段优先）
            if (paramsMap.containsKey(paramKey)) continue

            // 解析value中的数值: "+19" -> 19, "-5" -> -5, "15" -> 15
            val numericValue = parseNumericValue(item.value)
            if (numericValue != null) {
                paramsMap[paramKey] = numericValue
            }
        }
    }
}

/**
 * 从字符串中解析数值
 * 支持: "+19" -> 19, "-5" -> -5, "15" -> 15, "0" -> 0
 * 不支持: "复古 100%", "无", "开", "关" 等非纯数值字符串
 */
private fun parseNumericValue(value: String): Int? {
    val trimmed = value.trim()
    // 匹配可选正负号后跟数字的模式
    val match = Regex("^([+-]?)(\\d+)$").find(trimmed)
    return match?.let {
        val sign = if (it.groupValues[1] == "-") -1 else 1
        val num = it.groupValues[2].toIntOrNull() ?: return null
        sign * num
    }
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
 * 需要确认异常
 */
class ConfirmationRequired(message: String) : Exception(message)
