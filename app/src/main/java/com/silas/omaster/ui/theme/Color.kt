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
 * 背景色系统 - 四级层级
 * ============================================
 */
val BgPrimary = Color(0xFF050505)      // 最深背景 - 页面主背景
val BgSecondary = Color(0xFF0f0f0f)    // 二级背景 - 卡片背景
val BgTertiary = Color(0xFF1a1a1a)     // 三级背景 - 提升元素
val BgElevated = Color(0xFF252525)     // 提升背景 - 弹出层
val BgOverlay = Color(0x99000000)      // 遮罩背景

// 兼容旧命名
val PureBlack = Color(0xFF000000)
val NearBlack = Color(0xFF0A0A0A)
val DarkGray = Color(0xFF1A1A1A)
val MediumGray = Color(0xFF252525)
val ElevatedGray = Color(0xFF2D2D2D)

/**
 * ============================================
 * 文字颜色系统 - 四级层级
 * ============================================
 */
val TextPrimary = Color(0xFFFFFFFF)                       // 主要文字 - 标题
val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.85f) // 次级文字 - 正文
val TextTertiary = Color(0xFFFFFFFF).copy(alpha = 0.55f)   // 三级文字 - 辅助
val TextMuted = Color(0xFFFFFFFF).copy(alpha = 0.35f)      // 最弱文字 - 提示
val TextAccent = Color(0xFFFF6B35)                        // 强调文字

/**
 * ============================================
 * 边框颜色系统 - 四级层级
 * ============================================
 */
val BorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.04f)  // 最弱边框
val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)   // 轻边框
val BorderMedium = Color(0xFFFFFFFF).copy(alpha = 0.12f)  // 中边框
val BorderStrong = Color(0xFFFFFFFF).copy(alpha = 0.18f)  // 强边框
val BorderAccent = Color(0xFFFF6B35).copy(alpha = 0.25f)   // 强调边框

// 兼容旧命名
val CardBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)

/**
 * ============================================
 * 状态颜色 - 带muted版本
 * ============================================
 */
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFF44336)
val WarningYellow = Color(0xFFFF9800)
val InfoBlue = Color(0xFF2196F3)

// Muted版本
val SuccessMuted = Color(0xFF4CAF50).copy(alpha = 0.12f)
val WarningMuted = Color(0xFFFF9800).copy(alpha = 0.12f)
val ErrorMuted = Color(0xFFF44336).copy(alpha = 0.12f)
val InfoMuted = Color(0xFF2196F3).copy(alpha = 0.12f)

/**
 * ============================================
 * 哈苏橙渐变相关
 * ============================================
 */
val AccentGradientStart = Color(0xFFFF6B35)
val AccentGradientMid = Color(0xFFFF9F6B)
val AccentGradientEnd = Color(0xFFFFD93D)
val AccentPrimarySubtle = Color(0xFFFF6B35).copy(alpha = 0.06f)

// 兼容旧命名
val GradientOrangeStart = Color(0xFFFF6B35)
val GradientOrangeEnd = Color(0xFFFF9800)
val GradientOrangeLight = Color(0xFFFF9F6B)

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
 * 液态玻璃效果颜色
 * ============================================
 */
val GlassBackground = Color(0xFF1A1A1A).copy(alpha = 0.65f)
val GlassBackgroundLight = Color(0xFF2D2D2D).copy(alpha = 0.55f)
val GlassBackgroundHeavy = Color(0xFF0A0A0A).copy(alpha = 0.85f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)

/**
 * 获取功能模块颜色 - 统一哈苏橙
 */
fun getFeatureColor(featureId: String): Color = HasselbladOrange