package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌色系
 * 哈苏橙是品牌的标志性颜色 (#FF6B35 对齐 Web 端)
 */
val HasselbladOrange = Color(0xFFFF6B35)
val HasselbladOrangeDark = Color(0xFFE55A25)
val HasselbladOrangeLight = Color(0xFFFF8A50)
val HasselbladGreen = Color(0xFF4CAF50)

/**
 * 品牌主题色
 */
val ZeissBlue = Color(0xFF005A9C)
val LeicaRed = Color(0xFFCC0000)
val RicohGreen = Color(0xFF00A95C)
val FujifilmGreen = Color(0xFF009B3A)
val CanonRed = Color(0xFFCC0000)
val NikonYellow = Color(0xFFFFC20E)
val SonyOrange = Color(0xFFF15A24)
val PhaseOneGrey = Color(0xFF5A5A5A)

/**
 * 纯黑背景系列（对齐 Web 端）
 * 用于深色模式的主背景
 */
val PureBlack = Color(0xFF0A0A0A)  // 对齐 Web 端 #0A0A0A
val NearBlack = Color(0xFF0A0A0A)
val DarkGray = Color(0xFF1A1A1A)  // 卡片背景 rgba(255,255,255,0.05) ≈ #1A1A1A
val MediumGray = Color(0xFF333333)
val LightGray = Color(0xFF999999)
val OffWhite = Color(0xFFF5F5F5)

/**
 * 功能色
 */
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningYellow = Color(0xFFFFB300)

/**
 * UI 扩展色
 */
val CyanAccent = Color(0xFF00BCD4)  // 对齐 Web 端 #00BCD4
val CardBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val CardBorderHighlight = Color(0xFFFF6600).copy(alpha = 0.3f)
val SurfaceElevated = Color(0xFF222222)
val GradientOrangeStart = Color(0xFFFF6600)
val GradientOrangeEnd = Color(0xFFFF8533)

/**
 * 语义化文字颜色（WCAG 4.5:1 对比度合规）
 * 在 #0A0A0A 背景上:
 * - alpha 0.87: 对比度 ~14:1 (主要文字)
 * - alpha 0.70: 对比度 ~9:1 (次要文字)
 * - alpha 0.50: 对比度 ~5.5:1 (辅助文字，刚好达标)
 */
val TextHighEmphasis = Color.White.copy(alpha = 0.87f)
val TextMediumEmphasis = Color.White.copy(alpha = 0.70f)
val TextLowEmphasis = Color.White.copy(alpha = 0.50f)
