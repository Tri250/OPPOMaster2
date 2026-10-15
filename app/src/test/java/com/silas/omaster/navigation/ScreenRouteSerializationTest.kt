package com.silas.omaster.navigation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 路由序列化契约测试。
 *
 * v1.6.0 release 冷启动闪退的根因是：NavHost / composable&lt;T&gt; / toRoute&lt;T&gt;
 * 在导航库内部通过反射解析 Serializer，R8 会把未被直接引用的
 * `Screen$Xxx$$serializer` 判定为"未使用"并移除，导致运行时抛
 * SerializationException。
 *
 * 本测试锁定契约：每一条路由都必须能解析出 Serializer 且可 JSON 往返。
 * 说明：本测试在 JVM 上运行，无法复现 R8 移除场景；
 * 产物侧的保留验证由 CI 的 release 冒烟门禁（release.yml）承担。
 */
class ScreenRouteSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun <T : Screen> assertRoundTrip(ser: KSerializer<T>, value: T) {
        val encoded = json.encodeToString(ser, value)
        val decoded = json.decodeFromString(ser, encoded)
        assertEquals(value, decoded)
        assertTrue("serialName 不应为空", ser.descriptor.serialName.isNotBlank())
    }

    @Test
    fun `无参对象路由可解析序列化器并往返`() {
        assertRoundTrip(serializer<Screen.Home>(), Screen.Home)
        assertRoundTrip(serializer<Screen.PresetSelection>(), Screen.PresetSelection)
        assertRoundTrip(serializer<Screen.Settings>(), Screen.Settings)
        assertRoundTrip(serializer<Screen.Profile>(), Screen.Profile)
        assertRoundTrip(serializer<Screen.About>(), Screen.About)
        assertRoundTrip(serializer<Screen.Discover>(), Screen.Discover)
        assertRoundTrip(serializer<Screen.ColorWalk>(), Screen.ColorWalk)
        assertRoundTrip(serializer<Screen.Subscription>(), Screen.Subscription)
        assertRoundTrip(serializer<Screen.PrivacyPolicy>(), Screen.PrivacyPolicy)
        assertRoundTrip(serializer<Screen.OpenSourceLicense>(), Screen.OpenSourceLicense)
        assertRoundTrip(serializer<Screen.PhotoFrame>(), Screen.PhotoFrame)
    }

    @Test
    fun `带参路由可解析序列化器并保留参数`() {
        assertRoundTrip(serializer<Screen.Detail>(), Screen.Detail("preset-1"))
        assertRoundTrip(serializer<Screen.EditPreset>(), Screen.EditPreset("preset-2"))
        assertRoundTrip(serializer<Screen.CreatePreset>(), Screen.CreatePreset("template-1"))
    }

    @Test
    fun `CreatePreset 的可空模板参数可缺省`() {
        assertRoundTrip(serializer<Screen.CreatePreset>(), Screen.CreatePreset(null))
    }

    @Test
    fun `底部导航三项路由互斥 避免字符串匹配串台`() {
        // MainActivity 用 destination.route.contains("Home"/"Discover"/"About")
        // 判定当前主 Tab 与底部栏显隐；此处锁定三者不可互相包含，防止新增路由破坏该判定。
        val home = serializer<Screen.Home>().descriptor.serialName
        val discover = serializer<Screen.Discover>().descriptor.serialName
        val about = serializer<Screen.About>().descriptor.serialName

        assertTrue("Home 路由应包含 Home：$home", home.contains("Home"))
        assertFalse("Home 路由不应包含 Discover：$home", home.contains("Discover"))
        assertFalse("Home 路由不应包含 About：$home", home.contains("About"))

        assertTrue("Discover 路由应包含 Discover：$discover", discover.contains("Discover"))
        assertFalse("Discover 路由不应包含 Home：$discover", discover.contains("Home"))
        assertFalse("Discover 路由不应包含 About：$discover", discover.contains("About"))

        assertTrue("About 路由应包含 About：$about", about.contains("About"))
        assertFalse("About 路由不应包含 Home：$about", about.contains("Home"))
        assertFalse("About 路由不应包含 Discover：$about", about.contains("Discover"))
    }
}
