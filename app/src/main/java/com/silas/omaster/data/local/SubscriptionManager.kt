package com.silas.omaster.data.local

import android.content.Context
import com.silas.omaster.model.Subscription
import com.silas.omaster.model.SubscriptionList
import com.silas.omaster.util.SecurityCrypto
import com.silas.omaster.util.UpdateConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

class SubscriptionManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _subscriptionsFlow = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionsFlow: StateFlow<List<Subscription>> = _subscriptionsFlow.asStateFlow()

    init {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        // 优先尝试解密读取，加密存储后无法迁移则回退明文
        val jsonStr = tryReadSecureSubscriptions() ?: prefs.getString(KEY_SUBSCRIPTIONS, null)
        if (jsonStr != null) {
            try {
                val list = json.decodeFromString<SubscriptionList>(jsonStr)
                var updated = false
                val migratedSubscriptions = list.subscriptions.map { sub ->
                    // 迁移逻辑：如果订阅名称是"官方内置预设"但 URL 不是最新的，则更新它
                    if (sub.name == "官方内置预设" && sub.url != UpdateConfigManager.DEFAULT_PRESET_URL) {
                        updated = true
                        sub.copy(url = UpdateConfigManager.DEFAULT_PRESET_URL)
                    } else {
                        sub
                    }
                }
                _subscriptionsFlow.value = migratedSubscriptions
                if (updated) {
                    saveSubscriptions()
                    android.util.Log.d("SubscriptionManager", "Migrated official subscription to new URL")
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionManager", "Failed to decode subscriptions", e)
                _subscriptionsFlow.value = emptyList()
            }
        } else {
            // 首次使用，添加默认订阅（包含官方预设 + 4个品牌社区预设）
            val defaultSubs = listOf(
                Subscription(
                    url = UpdateConfigManager.DEFAULT_PRESET_URL,
                    name = "官方内置预设",
                    author = "@OMaster",
                    build = 1,
                    isEnabled = true
                ),
                Subscription(
                    url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json",
                    name = "OPPO 社区预设",
                    author = "@OMaster-Community",
                    build = 1,
                    isEnabled = true
                ),
                Subscription(
                    url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json",
                    name = "realme 社区预设",
                    author = "@OMaster-Community",
                    build = 1,
                    isEnabled = true
                ),
                Subscription(
                    url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json",
                    name = "vivo 社区预设",
                    author = "@OMaster-Community",
                    build = 1,
                    isEnabled = true
                ),
                Subscription(
                    url = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json",
                    name = "荣耀社区预设",
                    author = "@OMaster-Community",
                    build = 1,
                    isEnabled = true
                )
            )
            _subscriptionsFlow.value = defaultSubs
            saveSubscriptions()
        }
    }

    private fun tryReadSecureSubscriptions(): String? {
        return try {
            val encrypted = prefs.getString(KEY_SUBSCRIPTIONS_ENC, null)
            if (encrypted != null) {
                SecurityCrypto.decrypt(encrypted)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("SubscriptionManager", "读取加密订阅失败", e)
            null
        }
    }

    private fun saveSubscriptions() {
        val list = SubscriptionList(_subscriptionsFlow.value)
        val jsonStr = json.encodeToString(SubscriptionList.serializer(), list)
        
        // 使用加密存储
        try {
            val encrypted = SecurityCrypto.encrypt(jsonStr)
            if (encrypted != null) {
                prefs.edit()
                    .putString(KEY_SUBSCRIPTIONS_ENC, encrypted)
                    // 清除旧明文（一次性迁移）
                    .remove(KEY_SUBSCRIPTIONS)
                    .apply()
                return
            }
        } catch (e: Exception) {
            android.util.Log.w("SubscriptionManager", "加密存储失败，使用明文", e)
        }
        
        // 回退明文存储
        prefs.edit().putString(KEY_SUBSCRIPTIONS, jsonStr).apply()
    }

    fun addSubscription(url: String, name: String = "", author: String = "", build: Int = 1) {
        // 防御性:验证URL非空
        if (url.isBlank()) {
            android.util.Log.w("SubscriptionManager", "URL为空,拒绝添加订阅")
            return
        }
        // 自动补全协议头
        var normalizedUrl = url.trim()
        if (!normalizedUrl.lowercase().startsWith("http://") && !normalizedUrl.lowercase().startsWith("https://")) {
            normalizedUrl = "https://$normalizedUrl"
        }
        // 允许 HTTP 和 HTTPS 协议（CDN 链接可能使用 HTTP）
        if (!normalizedUrl.lowercase().startsWith("http://") && !normalizedUrl.lowercase().startsWith("https://")) {
            android.util.Log.w("SubscriptionManager", "非法URL协议,拒绝添加: $normalizedUrl")
            return
        }
        if (_subscriptionsFlow.value.any { it.url == normalizedUrl }) return
        val newSub = Subscription(url = normalizedUrl, name = name, author = author, build = build)
        _subscriptionsFlow.value = _subscriptionsFlow.value + newSub
        saveSubscriptions()
    }

    fun removeSubscription(url: String) {
        _subscriptionsFlow.value = _subscriptionsFlow.value.filterNot { it.url == url }
        saveSubscriptions()
        // Delete corresponding file
        val fileName = getFileNameForUrl(url)
        val file = File(appContext.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun toggleSubscription(url: String) {
        _subscriptionsFlow.value = _subscriptionsFlow.value.map {
            if (it.url == url) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveSubscriptions()
    }

    fun updateSubscriptionStatus(url: String, presetCount: Int, lastUpdateTime: Long, name: String? = null, author: String? = null, build: Int? = null) {
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

    fun updateSubscriptionUrl(oldUrl: String, newUrl: String) {
        if (oldUrl == newUrl) return
        _subscriptionsFlow.value = _subscriptionsFlow.value.map {
            if (it.url == oldUrl) it.copy(url = newUrl) else it
        }
        saveSubscriptions()
        
        // 删除旧文件，新文件将在下次更新时创建
        val oldFileName = getFileNameForUrl(oldUrl)
        val oldFile = File(appContext.filesDir, oldFileName)
        if (oldFile.exists()) {
            oldFile.delete()
        }
    }

    fun getFileNameForUrl(url: String): String {
        // Use a hash of the URL to create a unique filename
        val hash = url.hashCode().toString(16)
        return "sub_$hash.json"
    }

    companion object {
        private const val PREFS_NAME = "omaster_subscriptions"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"
        private const val KEY_SUBSCRIPTIONS_ENC = "subscriptions_list_enc"

        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionManager(context).also { INSTANCE = it }
            }
        }
    }
}
