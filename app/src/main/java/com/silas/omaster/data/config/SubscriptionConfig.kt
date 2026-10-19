package com.silas.omaster.data.config

import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.model.Subscription
import com.silas.omaster.model.SubscriptionList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 订阅配置管理（ConfigCenter 子模块）
 * 处理订阅列表的持久化和状态管理
 */
internal class SubscriptionConfig(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val json = Json { ignoreUnknownKeys = true }

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    // 【v1.6.14 发现 2.0】订阅更新雷达：每源自上次"标记已读"后的新增预设数
    private val _unseenCountsFlow = MutableStateFlow<Map<String, Int>>(loadUnseenCounts())
    val unseenCountsFlow: StateFlow<Map<String, Int>> = _unseenCountsFlow.asStateFlow()

    companion object {
        const val DEFAULT_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json"
        const val REALME_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json"
        // 【v1.6.13】vivo / HONOR 社区预设源（默认启用，启动自动刷新拉取）
        const val VIVO_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json"
        const val HONOR_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json"

        private const val PREFS_NAME = "omaster_config_subscriptions"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"

        /** 旧版 SubscriptionManager 使用的存储名（仅用于一次性迁移） */
        private const val LEGACY_PREFS_NAME = "omaster_subscriptions"

        /**
         * 【v1.6.13】默认订阅种子版本：
         * 1 = oppo 出厂源；2 = +realme；3 = +vivo / HONOR（本次追加）。
         * 版本落后时幂等补齐一次；用户随后删除不会被再次补回。
         */
        private const val KEY_DEFAULT_SEED_VERSION = "default_seed_version"
        private const val DEFAULT_SEED_VERSION = 3

        // 【v1.6.14 发现 2.0】订阅雷达未读新增计数（url → count）
        private const val KEY_UNSEEN_COUNTS = "unseen_counts"
    }

    init {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        val jsonStr = prefs.getString(KEY_SUBSCRIPTIONS, null)
        if (jsonStr != null) {
            try {
                val list = json.decodeFromString<SubscriptionList>(jsonStr)
                _subscriptionsFlow.value = list.subscriptions
                // 【v1.6.13】存量用户升级后幂等补齐新增默认源（vivo / HONOR）
                seedDefaultSubscriptions()
            } catch (e: Exception) {
                android.util.Log.e(ConfigLog.TAG, "[Subscription] Failed to load subscriptions", e)
                _subscriptionsFlow.value = emptyList()
            }
        } else {
            // 首启：优先迁移 v1.5 及以前的旧订阅存储，避免升级用户的自定义订阅丢失；
            // 仅当确实没有旧数据时才写入出厂默认订阅。
            if (migrateFromLegacy()) {
                saveSubscriptions()
            } else {
                createDefaultSubscriptions()
            }
        }
    }

    /**
     * 迁移旧 SubscriptionManager 的订阅存储（SharedPreferences "omaster_subscriptions"）。
     * 保留用户自定义订阅，并补齐官方 / Realme 默认订阅（与旧版迁移语义一致）。
     * @return true 表示迁移到了旧数据
     */
    private fun migrateFromLegacy(): Boolean {
        val legacyJson = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SUBSCRIPTIONS, null)
            ?: return false

        val legacy = try {
            json.decodeFromString<SubscriptionList>(legacyJson).subscriptions
        } catch (e: Exception) {
            android.util.Log.e(ConfigLog.TAG, "[Subscription] Failed to migrate legacy subscriptions", e)
            return false
        }
        if (legacy.isEmpty()) return false

        // 1) 旧版「官方内置预设」URL 归一化
        var migrated = legacy.map { sub ->
            if (sub.name == "官方内置预设" && sub.url != DEFAULT_PRESET_URL) {
                sub.copy(url = DEFAULT_PRESET_URL)
            } else {
                sub
            }
        }
        // 2) 补齐官方预设（用户可能只订阅了第三方源）
        if (migrated.none { it.url == DEFAULT_PRESET_URL }) {
            migrated = listOf(
                Subscription(
                    url = DEFAULT_PRESET_URL,
                    name = "OPPO / 一加 大师预设",
                    author = "@OMaster",
                    build = 4,
                    isEnabled = true,
                    presetCount = 0,
                    lastUpdateTime = System.currentTimeMillis()
                )
            ) + migrated
        }
        // 3) 补齐 Realme 预设（默认关闭，与旧版一致）
        if (migrated.none { it.url == REALME_PRESET_URL }) {
            migrated = migrated + Subscription(
                url = REALME_PRESET_URL,
                name = "Realme GR预设",
                author = "@OMaster",
                build = 1,
                isEnabled = false,
                presetCount = 0,
                lastUpdateTime = 0
            )
        }

        _subscriptionsFlow.value = migrated
        // 【v1.6.13】迁移后同样补齐新增默认源（vivo / HONOR）
        seedDefaultSubscriptions()
        android.util.Log.d(
            ConfigLog.TAG,
            "[Subscription] Migrated ${_subscriptionsFlow.value.size} subscriptions from legacy store"
        )
        return true
    }

    /**
     * 【v1.6.13】默认订阅种子补齐（幂等）：为存量订阅列表追加 vivo / HONOR 社区预设源。
     * 新源默认启用（isEnabled = true），启动时自动刷新机制（每日一次）会立即拉取；
     * 仅在种子版本落后时执行，写入版本后用户删除不会被再次补回。
     */
    private fun seedDefaultSubscriptions() {
        val currentSeed = prefs.getInt(KEY_DEFAULT_SEED_VERSION, 1)
        if (currentSeed >= DEFAULT_SEED_VERSION) return

        val current = _subscriptionsFlow.value
        var changed = false
        var migrated = current
        if (migrated.none { it.url == VIVO_PRESET_URL }) {
            migrated = migrated + Subscription(
                url = VIVO_PRESET_URL,
                name = "vivo 大师预设",
                author = "@OMaster",
                build = 1,
                isEnabled = true,
                presetCount = 0,
                lastUpdateTime = 0
            )
            changed = true
        }
        if (migrated.none { it.url == HONOR_PRESET_URL }) {
            migrated = migrated + Subscription(
                url = HONOR_PRESET_URL,
                name = "HONOR 大师预设",
                author = "@OMaster",
                build = 1,
                isEnabled = true,
                presetCount = 0,
                lastUpdateTime = 0
            )
            changed = true
        }
        if (changed) {
            _subscriptionsFlow.value = migrated
            saveSubscriptions()
            android.util.Log.d(ConfigLog.TAG, "[Subscription] Seeded vivo/HONOR default subscriptions")
        }
        prefs.edit().putInt(KEY_DEFAULT_SEED_VERSION, DEFAULT_SEED_VERSION).apply()
    }

    private fun createDefaultSubscriptions() {
        val defaultSubs = listOf(
            Subscription(
                url = DEFAULT_PRESET_URL,
                name = "OPPO / 一加 大师预设",
                author = "@OMaster",
                build = 4,
                isEnabled = true,
                presetCount = 0,
                lastUpdateTime = System.currentTimeMillis()
            ),
            Subscription(
                url = REALME_PRESET_URL,
                name = "Realme GR预设",
                author = "@OMaster",
                build = 1,
                isEnabled = false,
                presetCount = 0,
                lastUpdateTime = 0
            ),
            // 【v1.6.13】vivo / HONOR 社区源（默认启用，启动自动刷新拉取）
            Subscription(
                url = VIVO_PRESET_URL,
                name = "vivo 大师预设",
                author = "@OMaster",
                build = 1,
                isEnabled = true,
                presetCount = 0,
                lastUpdateTime = 0
            ),
            Subscription(
                url = HONOR_PRESET_URL,
                name = "HONOR 大师预设",
                author = "@OMaster",
                build = 1,
                isEnabled = true,
                presetCount = 0,
                lastUpdateTime = 0
            )
        )
        _subscriptionsFlow.value = defaultSubs
        saveSubscriptions()
        prefs.edit().putInt(KEY_DEFAULT_SEED_VERSION, DEFAULT_SEED_VERSION).apply()
    }

    fun addSubscription(url: String, name: String = "", author: String = "", build: Int = 1) {
        if (_subscriptionsFlow.value.any { it.url == url }) return
        val newSub = Subscription(url = url, name = name, author = author, build = build)
        _subscriptionsFlow.value = _subscriptionsFlow.value + newSub
        saveSubscriptions()
    }

    fun removeSubscription(url: String) {
        _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { it.url == url }
        saveSubscriptions()
        // 【v1.6.14 发现 2.0】同步清理雷达计数
        removeUnseenCount(url)
        val fileName = getFileNameForUrl(url)
        File(context.filesDir, fileName).delete()
    }

    fun updateSubscriptionUrl(oldUrl: String, newUrl: String) {
        val oldFileName = getFileNameForUrl(oldUrl)
        val newFileName = getFileNameForUrl(newUrl)
        val oldFile = File(context.filesDir, oldFileName)
        val newFile = File(context.filesDir, newFileName)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
        _subscriptionsFlow.value = _subscriptionsFlow.value.map {
            if (it.url == oldUrl) it.copy(url = newUrl) else it
        }
        saveSubscriptions()
    }

    fun toggleSubscription(url: String) {
        _subscriptionsFlow.value = _subscriptionsFlow.value.map {
            if (it.url == url) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveSubscriptions()
    }

    fun updateSubscriptionStatus(
        url: String,
        presetCount: Int,
        lastUpdateTime: Long,
        name: String? = null,
        author: String? = null,
        build: Int? = null
    ) {
        // 【v1.6.14 发现 2.0】刷新雷达：预设数增量计入未读
        // （旧值为 0 且从未标记已读时视为初始基准，不把首次拉取算作"新增"）
        val oldCount = _subscriptionsFlow.value.firstOrNull { it.url == url }?.presetCount ?: 0
        val unseen = _unseenCountsFlow.value
        if (presetCount > oldCount && (oldCount > 0 || unseen.containsKey(url))) {
            val updated = unseen.toMutableMap()
            updated[url] = (updated[url] ?: 0) + (presetCount - oldCount)
            _unseenCountsFlow.value = updated
            saveUnseenCounts(updated)
        }

        _subscriptionsFlow.value = _subscriptionsFlow.value.map {
            if (it.url == url) {
                it.copy(
                    presetCount = presetCount,
                    lastUpdateTime = lastUpdateTime,
                    name = name ?: it.name,
                    author = author ?: it.author,
                    build = build ?: it.build
                )
            } else it
        }
        saveSubscriptions()
    }

    /** 【v1.6.14 发现 2.0】标记某订阅源已读：雷达未读清零（0 占位以区分"从未看过"） */
    fun markSubscriptionSeen(url: String) {
        if (_unseenCountsFlow.value[url] != null) {
            val updated = _unseenCountsFlow.value.toMutableMap()
            updated[url] = 0
            _unseenCountsFlow.value = updated
            saveUnseenCounts(updated)
        }
    }

    /** 移除订阅时同步清理雷达计数 */
    private fun removeUnseenCount(url: String) {
        if (_unseenCountsFlow.value.containsKey(url)) {
            val updated = _unseenCountsFlow.value - url
            _unseenCountsFlow.value = updated
            saveUnseenCounts(updated)
        }
    }

    private fun loadUnseenCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_UNSEEN_COUNTS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    private fun saveUnseenCounts(map: Map<String, Int>) {
        prefs.edit().putString(KEY_UNSEEN_COUNTS, json.encodeToString(map)).apply()
    }

    fun getFileNameForUrl(url: String): String {
        val hash = url.hashCode().toString(16)
        return "sub_$hash.json"
    }

    private fun saveSubscriptions() {
        val list = SubscriptionList(_subscriptionsFlow.value)
        val jsonStr = json.encodeToString(SubscriptionList.serializer(), list)
        prefs.edit().putString(KEY_SUBSCRIPTIONS, jsonStr).apply()
    }
}
