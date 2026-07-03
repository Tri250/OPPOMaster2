package com.silas.omaster.data.local

import android.content.Context
import com.silas.omaster.model.Subscription
import com.silas.omaster.model.SubscriptionList
import com.silas.omaster.util.SecurityCrypto
import com.silas.omaster.util.UpdateConfigManager
import com.silas.omaster.util.UrlConstants
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

    /**
     * 默认订阅源定义（云同步JSON链接迁移而来）
     * - 所有源均启用，由 PresetRemoteManager 在后台动态拉取并原子写入
     * - JsonUtil.loadPresets() 会读取所有启用订阅的本地缓存文件
     */
    private val defaultSubscriptions: List<Subscription> = listOf(
        Subscription(
            url = UpdateConfigManager.DEFAULT_PRESET_URL,
            name = "官方内置预设",
            author = "@OMaster",
            build = 1,
            isEnabled = true
        ),
        Subscription(
            url = UrlConstants.PRESET_REALME,
            name = "realme GT 大师模式预设",
            author = "@OMaster",
            build = 1,
            isEnabled = true
        ),
        Subscription(
            url = UrlConstants.PRESET_VIVO,
            name = "vivo 蔡司自然色彩预设",
            author = "@OMaster",
            build = 1,
            isEnabled = true
        ),
        Subscription(
            url = UrlConstants.PRESET_HONOR,
            name = "荣耀 Magic 影像预设",
            author = "@OMaster",
            build = 1,
            isEnabled = true
        )
    )

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
                }.toMutableList()

                // 迁移逻辑：补齐缺失的默认订阅源（从云同步迁移到订阅管理）
                // 确保所有 4 个品牌预设源都存在，保留用户已有的自定义订阅
                val existingUrls = migratedSubscriptions.map { it.url }.toSet()
                for (defaultSub in defaultSubscriptions) {
                    if (defaultSub.url !in existingUrls) {
                        migratedSubscriptions.add(defaultSub)
                        updated = true
                        android.util.Log.d("SubscriptionManager", "Added missing default subscription: ${defaultSub.name}")
                    }
                }

                _subscriptionsFlow.value = migratedSubscriptions
                if (updated) {
                    saveSubscriptions()
                    android.util.Log.d("SubscriptionManager", "Migrated subscriptions, total: ${migratedSubscriptions.size}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionManager", "Failed to decode subscriptions", e)
                _subscriptionsFlow.value = defaultSubscriptions
                saveSubscriptions()
            }
        } else {
            // 首次使用，添加全部默认订阅（4个品牌预设源，默认启用动态加载）
            _subscriptionsFlow.value = defaultSubscriptions
            saveSubscriptions()
            android.util.Log.i("SubscriptionManager", "First launch, added ${defaultSubscriptions.size} default subscriptions")
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
        // HTTPS 安全校验
        if (!url.lowercase().startsWith("https://")) {
            android.util.Log.w("SubscriptionManager", "非HTTPS URL,拒绝添加: $url")
            return
        }
        if (_subscriptionsFlow.value.any { it.url == url }) return
        val newSub = Subscription(url = url, name = name, author = author, build = build)
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
