package com.silas.omaster.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.model.Subscription
import com.silas.omaster.model.SubscriptionList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 配置迁移契约测试（Robolectric，无需真机）。
 *
 * 锁定 v1.6 配置中心落地时的迁移边界：
 * 1. 旧设置存储 "app_settings" → ConfigCenter
 * 2. 旧订阅存储 "omaster_subscriptions" → SubscriptionConfig（保留用户自定义源）
 * 3. 迁移幂等 + 旧存储清理
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ConfigMigrationTest {

    private lateinit var context: Context

    private val json = Json { ignoreUnknownKeys = true }

    private val allPrefs = listOf(
        "app_settings",
        "omaster_update_prefs",
        "omaster_subscriptions",
        "omaster_config_center",
        "omaster_config_subscriptions"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        allPrefs.forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    private fun prefs(name: String) =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test
    fun `旧设置存储迁移到配置中心`() {
        prefs("app_settings").edit()
            .putString("theme_id", "leica")
            .putString("app_language", "ENGLISH")
            .putBoolean("vibration_enabled", false)
            .commit()

        val configPrefs = prefs("omaster_config_center")
        assertTrue(ConfigMigration.migrate(context, configPrefs))

        assertEquals("leica", configPrefs.getString("theme_id", null))
        assertEquals("ENGLISH", configPrefs.getString("app_language", null))
        assertFalse(configPrefs.getBoolean("vibration_enabled", true))
    }

    @Test
    fun `迁移幂等 二次调用不再迁移`() {
        prefs("app_settings").edit().putString("theme_id", "zeiss").commit()
        val configPrefs = prefs("omaster_config_center")

        assertTrue(ConfigMigration.migrate(context, configPrefs))
        assertFalse("已完成迁移后应直接跳过", ConfigMigration.migrate(context, configPrefs))
    }

    @Test
    fun `清理旧版存储后不再残留`() {
        prefs("app_settings").edit().putString("theme_id", "zeiss").commit()
        prefs("omaster_update_prefs").edit().putString("preset_update_url", "x").commit()

        ConfigMigration.cleanupLegacyData(context)

        assertTrue(prefs("app_settings").all.isEmpty())
        assertTrue(prefs("omaster_update_prefs").all.isEmpty())
    }

    @Test
    fun `无旧数据时写入出厂默认订阅`() {
        val config = SubscriptionConfig(context)

        val subs = config.subscriptionsFlow.value
        assertEquals(2, subs.size)
        assertTrue(subs.any { it.url == SubscriptionConfig.DEFAULT_PRESET_URL })
        assertTrue(subs.any { it.url == SubscriptionConfig.REALME_PRESET_URL })
    }

    @Test
    fun `旧订阅存储迁移进订阅配置并保留自定义源`() {
        val legacy = SubscriptionList(
            listOf(
                Subscription(url = "https://example.com/custom.json", name = "我的源", isEnabled = true),
                Subscription(url = "https://example.com/old-official.json", name = "官方内置预设", isEnabled = true)
            )
        )
        prefs("omaster_subscriptions").edit()
            .putString("subscriptions_list", json.encodeToString(SubscriptionList.serializer(), legacy))
            .commit()

        val config = SubscriptionConfig(context)
        val subs = config.subscriptionsFlow.value

        // 用户自定义源必须保留，这是 v1.6 升级最容易丢失的数据
        assertTrue(subs.any { it.url == "https://example.com/custom.json" })
        // 旧「官方内置预设」URL 归一化到当前官方地址
        assertTrue(subs.any { it.url == SubscriptionConfig.DEFAULT_PRESET_URL })
        assertFalse(subs.any { it.url == "https://example.com/old-official.json" })
        // Realme 源补齐（默认关闭，与旧版语义一致）
        assertTrue(
            subs.any {
                it.url == SubscriptionConfig.REALME_PRESET_URL && !it.isEnabled
            }
        )
        // 迁移结果落盘，重启后仍可读回
        assertNotNull(prefs("omaster_config_subscriptions").getString("subscriptions_list", null))
    }

    @Test
    fun `迁移结果落盘后可回读`() {
        prefs("omaster_subscriptions").edit()
            .putString(
                "subscriptions_list",
                json.encodeToString(
                    SubscriptionList.serializer(),
                    SubscriptionList(listOf(Subscription(url = "https://example.com/a.json", name = "A")))
                )
            )
            .commit()

        SubscriptionConfig(context)
        val reloaded = SubscriptionConfig(context)

        assertTrue(reloaded.subscriptionsFlow.value.any { it.url == "https://example.com/a.json" })
    }

    @Test
    fun `旧订阅存储损坏时不阻塞启动并回退默认订阅`() {
        prefs("omaster_subscriptions").edit()
            .putString("subscriptions_list", "{ 这不是合法 JSON")
            .commit()

        val config = SubscriptionConfig(context)

        // 解析失败必须安全回退到出厂默认订阅，绝不能抛异常阻塞冷启动
        assertTrue(config.subscriptionsFlow.value.isNotEmpty())
        assertTrue(config.subscriptionsFlow.value.any { it.url == SubscriptionConfig.DEFAULT_PRESET_URL })
    }
}
