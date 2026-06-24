package com.silas.omaster

import kotlinx.serialization.Serializable

/**
 * 应用导航路由定义
 * 集中管理所有页面路由，避免散落在 MainActivity 中
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
    data object About : Screen()

    @Serializable
    data object Subscription : Screen()

    @Serializable
    data object CoreFeatures : Screen()

    @Serializable
    data object AIFineTune : Screen()

    @Serializable
    data class WatermarkEditor(val imagePath: String? = null) : Screen()

    @Serializable
    data object SmartOptimize : Screen()

    @Serializable
    data object ParamAdjustment : Screen()

    @Serializable
    data object LUTShare : Screen()

    @Serializable
    data object StyleLUTGenerator : Screen()

    @Serializable
    data object HasselbladColor : Screen()

    @Serializable
    data object CloudSync : Screen()

    @Serializable
    data object NotificationSettings : Screen()

    @Serializable
    data object Terms : Screen()

    @Serializable
    data object PrivacyPolicy : Screen()

    @Serializable
    data object PresetSourceManager : Screen()

    @Serializable
    data object UpdateChannel : Screen()

    @Serializable
    data object ApiConfig : Screen()

    @Serializable
    data object ThemeSettings : Screen()

    @Serializable
    data object SceneAnalysisReport : Screen()

    @Serializable
    data object Onboarding : Screen()

    @Serializable
    data class CameraXViewfinder(val presetId: String? = null) : Screen()

    @Serializable
    data object ImportExport : Screen()
}
