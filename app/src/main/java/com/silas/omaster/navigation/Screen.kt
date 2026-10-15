package com.silas.omaster.navigation

import kotlinx.serialization.Serializable

/**
 * 类型安全导航路由契约（单一事实来源）。
 *
 * ⚠️ release 混淆敏感点：
 * NavHost / composable<T> / toRoute<T> 在导航库内部通过反射解析 Serializer，
 * 无法被 kotlinx 编译插件的 intrinsic 内联为直接引用。
 * 必须保证 proguard-rules.pro 的 kotlinx.serialization keep 规则覆盖本包
 * （见 `com.silas.omaster.**$$serializer` 与 `Companion.serializer()` 规则），
 * 否则路由序列化器会被 R8 移除，导致冷启动时 NavHost 建图抛异常而闪退。
 */
sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data class Detail(val presetId: String) : Screen()

    @Serializable
    data object PresetSelection : Screen()

    @Serializable
    data class CreatePreset(val templateId: String? = null) : Screen()

    @Serializable
    data class EditPreset(val presetId: String) : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object Profile : Screen()

    @Serializable
    data object About : Screen()

    @Serializable
    data object Discover : Screen()

    @Serializable
    data object ColorWalk : Screen()

    @Serializable
    data object Subscription : Screen()

    @Serializable
    data object PrivacyPolicy : Screen()

    @Serializable
    data object OpenSourceLicense : Screen()

    @Serializable
    data object PhotoFrame : Screen()
}
