package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ============================================
 * 小O帮帮 设计系统 - ColorOS 16 规范
 * 2026 企业级设计标准 - 统一哈苏橙风格
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
 * 功能模块统一哈苏橙色系
 * ============================================
 */
val FeaturePrimary = Color(0xFFFF6B35)
val FeaturePrimaryLight = Color(0xFFFF8C5A)
val FeaturePrimaryDark = Color(0xFFE55A25)

/**
 * ============================================
 * 文字颜色系统 - 简化三级
 * ============================================
 */
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.70f)
val TextTertiary = Color(0xFFFFFFFF).copy(alpha = 0.40f)

/**
 * ============================================
 * 边框颜色系统
 * ============================================
 */
val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val BorderMedium = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val BorderStrong = Color(0xFFFFFFFF).copy(alpha = 0.20f)
val CardBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)

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
 * 获取功能模块颜色 - 统一哈苏橙
 */
fun getFeatureColor(featureId: String): Color = HasselbladOrange
