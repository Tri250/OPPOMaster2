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
    data object NotificationSettings : Screen()

    @Serializable
    data object Terms : Screen()

    @Serializable
    data object PrivacyPolicy : Screen()

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
    data object CloudSync : Screen()

    @Serializable
    data object ImportExport : Screen()

    /** 2.2.0 新增：权限自检页面 */
    @Serializable
    data object PermissionCheck : Screen()

    // 行影集（TrailSnap Android 原生版）
    @Serializable
    data object XingYingJiHome : Screen()

    @Serializable
    data object XingYingJiTimeline : Screen()

    @Serializable
    data object XingYingJiAlbums : Screen()

    @Serializable
    data class XingYingJiAlbumDetail(val albumId: String) : Screen()

    @Serializable
    data object XingYingJiFavorites : Screen()

    @Serializable
    data object XingYingJiLocations : Screen()

    @Serializable
    data class XingYingJiLocationDetail(val locationName: String) : Screen()

    @Serializable
    data object XingYingJiPeople : Screen()

    @Serializable
    data class XingYingJiPersonDetail(val faceId: String) : Screen()

    @Serializable
    data object XingYingJiTickets : Screen()

    @Serializable
    data object XingYingJiToolbox : Screen()

    @Serializable
    data object XingYingJiRecycleBin : Screen()

    @Serializable
    data object XingYingJiAnnualReport : Screen()

    /** 付费墙页面 */
    @Serializable
    data object Paywall : Screen()

    /** 2.3.0 新增：视频滤镜页面 */
    @Serializable
    data object VideoFilter : Screen()

    /** 水印编辑页面 */
    @Serializable
    data object Watermark : Screen()
}
