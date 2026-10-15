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

    companion object {
        const val DEFAULT_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json"
        const val REALME_PRESET_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json"

        private const val PREFS_NAME = "omaster_config_subscriptions"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"

        /** 旧版 SubscriptionManager 使用的存储名（仅用于一次性迁移） */
        private const val LEGACY_PREFS_NAME = "omaster_subscriptions"
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
        android.util.Log.d(
            ConfigLog.TAG,
            "[Subscription] Migrated ${migrated.size} subscriptions from legacy store"
        )
        return true
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
            )
        )
        _subscriptionsFlow.value = defaultSubs
        saveSubscriptions()
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
