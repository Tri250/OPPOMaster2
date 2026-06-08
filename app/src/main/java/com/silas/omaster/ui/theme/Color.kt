package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ============================================
 * 小O帮帮 设计系统 - ColorOS 16 规范
 * 2026 企业级设计标准
 * ============================================
 */

/**
 * 主强调色 - 哈苏橙
 */
val HasselbladOrange = Color(0xFFFF6B35)
val HasselbladOrangeDark = Color(0xFFE55A25)
val HasselbladOrangeLight = Color(0xFFFF8C5A)
val HasselbladOrangeMuted = Color(0xFFFF6B35).copy(alpha = 0.15f)

/**
 * 品牌主题色
 */
val ZeissBlue = Color(0xFF2196F3)
val LeicaRed = Color(0xFFCC0000)
val RicohGreen = Color(0xFF00A95C)
val FujifilmGreen = Color(0xFF009B3A)
val CanonRed = Color(0xFFCC0000)
val NikonYellow = Color(0xFFFFC20E)
val SonyOrange = Color(0xFFF15A24)
val PhaseOneGrey = Color(0xFF5A5A5A)

/**
 * ============================================
 * 背景色系统
 * ============================================
 */
val PureBlack = Color(0xFF000000)
val NearBlack = Color(0xFF0A0A0A)           // 主背景色
val DarkGray = Color(0xFF1A1A1A)            // 卡片背景色
val MediumGray = Color(0xFF252525)          // 三级背景
val ElevatedGray = Color(0xFF2D2D2D)        // 提升背景

/**
 * ============================================
 * 功能模块特色色
 * ============================================
 */
val FeatureAIPurple = Color(0xFF9C27B0)           // AI智能 - 紫色
val FeatureWatermarkCyan = Color(0xFF00BCD4)      // 水印编辑 - 青色
val FeatureSceneGreen = Color(0xFF4CAF50)         // 场景识别 - 绿色
val FeatureSyncBlue = Color(0xFF2196F3)           // 云同步 - 蓝色
val FeaturePresetOrange = Color(0xFFFF9800)       // 预设管理 - 橙色
val FeatureThemePink = Color(0xFFE91E63)          // 主题设置 - 粉色

/**
 * ============================================
 * 文字颜色系统
 * ============================================
 */
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.85f)
val TextTertiary = Color(0xFFFFFFFF).copy(alpha = 0.60f)
val TextMuted = Color(0xFFFFFFFF).copy(alpha = 0.40f)
val TextDisabled = Color(0xFFFFFFFF).copy(alpha = 0.25f)

/**
 * ============================================
 * 边框颜色系统
 * ============================================
 */
val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val BorderMedium = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val BorderStrong = Color(0xFFFFFFFF).copy(alpha = 0.20f)

/**
 * ============================================
 * 状态颜色
 * ============================================
 */
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFF44336)
val WarningYellow = Color(0xFFFF9800)
val InfoBlue = Color(0xFF2196F3)

/**
 * ============================================
 * 液态玻璃效果颜色
 * ============================================
 */
val GlassBackground = Color(0xFF1A1A1A).copy(alpha = 0.65f)
val GlassBackgroundLight = Color(0xFF2D2D2D).copy(alpha = 0.55f)
val GlassBackgroundHeavy = Color(0xFF0A0A0A).copy(alpha = 0.85f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)

/**
 * ============================================
 * 渐变颜色
 * ============================================
 */
val GradientOrangeStart = Color(0xFFFF6B35)
val GradientOrangeEnd = Color(0xFFFF9800)
val GradientOrangeLight = Color(0xFFFF9F6B)

/**
 * 获取功能模块颜色
 */
fun getFeatureColor(featureId: String): Color {
    return when (featureId) {
        "ai-scene", "ai-fine-tune" -> FeatureAIPurple
        "watermark" -> FeatureWatermarkCyan
        "smart-optimize" -> FeatureSceneGreen
        "cloud-sync" -> FeatureSyncBlue
        "preset-manager" -> FeaturePresetOrange
        "theme-settings" -> FeatureThemePink
        "hasselblad" -> HasselbladOrange
        else -> HasselbladOrange
    }
}
