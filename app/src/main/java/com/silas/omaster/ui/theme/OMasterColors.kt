package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * =====================================================
 * OMaster 设计系统 v2.0
 * =====================================================
 * 设计风格：ColorOS 16 系统级水准
 * 
 * 色彩规范：
 * - 主色调（哈苏橙）：#E65100
 * - 辅助色（OPPO绿）：#00C853
 * - 深色背景：#1A1A1A / #18181B
 * - 卡片背景：#27272A
 * - 边框/分割线：#3F3F46 / #52525B
 * - 文字主色：纯白 #FFFFFF
 * - 文字次色：#A1A1AA / #D4D4D8
 */

// ==================== 主色调 ====================

/**
 * 哈苏橙 - 品牌主色
 * 用于：按钮、强调、认证标识
 */
val HasselbladOrange = Color(0xFFE65100)
val HasselbladOrangeLight = Color(0xFFFF7A1A)
val HasselbladOrangeDark = Color(0xFFBF4000)

// 哈苏橙透明度变体
val HasselbladOrange10 = Color(0x1AE65100)
val HasselbladOrange20 = Color(0x33E65100)
val HasselbladOrange30 = Color(0x4DE65100)
val HasselbladOrange50 = Color(0x80E65100)

/**
 * OPPO绿 - 辅助色
 * 用于：成功状态、下载、积极反馈
 */
val OPPOGreen = Color(0xFF00C853)
val OPPOGreenLight = Color(0xFF00E676)
val OPPOGreenDark = Color(0xFF00A344)

// OPPO绿透明度变体
val OPPOGreen10 = Color(0x1A00C853)
val OPPOGreen20 = Color(0x3300C853)

// ==================== 背景色 ====================

/**
 * 深色背景系统
 * ColorOS 16 深空黑风格
 */
val BackgroundPrimary = Color(0xFF18181B)      // 主背景 #18181B
val BackgroundSecondary = Color(0xFF1A1A1A)    // 次级背景 #1A1A1A
val BackgroundTertiary = Color(0xFF27272A)     // 卡片背景 #27272A

// ==================== 边框与分割 ====================

val BorderPrimary = Color(0xFF3F3F46)          // 主边框 #3F3F46
val BorderSecondary = Color(0xFF52525B)        // 次边框 #52525B
val BorderTertiary = Color(0xFF71717A)         // 三级边框

// 边框透明度
val BorderPrimary50 = Color(0x803F3F46)
val BorderPrimary30 = Color(0x4D3F3F46)

// ==================== 文字色彩 ====================

val TextPrimary = Color(0xFFFFFFFF)            // 主文字 纯白
val TextSecondary = Color(0xFFD4D4D8)          // 次文字 #D4D4D8
val TextTertiary = Color(0xFFA1A1AA)           // 三级文字 #A1A1AA
val TextQuaternary = Color(0xFF71717A)         // 四级文字 #71717A

// 文字透明度
val TextPrimary70 = Color(0xB3FFFFFF)          // 70%白
val TextPrimary50 = Color(0x80FFFFFF)          // 50%白

// ==================== 功能色 ====================

val ErrorRed = Color(0xFFEF4444)               // 错误
val ErrorRedDark = Color(0xFFDC2626)

val WarningYellow = Color(0xFFFACC15)          // 警告/星星
val WarningYellowDark = Color(0xFFEAB308)

val InfoBlue = Color(0xFF3B82F6)               // 信息
val InfoBlueDark = Color(0xFF2563EB)

// ==================== 渐变色 ====================

/**
 * 哈苏橙渐变 - 用于按钮、强调元素
 */
val GradientHasselblad = listOf(
    HasselbladOrangeLight,
    HasselbladOrange,
    HasselbladOrangeDark
)

/**
 * OPPO绿渐变 - 用于成功状态
 */
val GradientOPPOGreen = listOf(
    OPPOGreenLight,
    OPPOGreen,
    OPPOGreenDark
)

/**
 * 背景渐变 - 用于Hero区域
 */
val GradientBackground = listOf(
    BackgroundPrimary,
    BackgroundSecondary,
    BackgroundPrimary
)

/**
 * 卡片渐变 - 用于卡片背景
 */
val GradientCard = listOf(
    BackgroundTertiary,
    Color(0xFF2E2E33),
    BackgroundTertiary
)

// ==================== 阴影 ====================

val ShadowOrange = Color(0x40E65100)           // 橙色阴影
val ShadowGreen = Color(0x4000C853)            // 绿色阴影
val ShadowDark = Color(0x40000000)             // 深色阴影

// ==================== 主题对象 ====================

object OMasterColorScheme {
    val primary = HasselbladOrange
    val secondary = OPPOGreen
    val background = BackgroundPrimary
    val surface = BackgroundTertiary
    val onPrimary = TextPrimary
    val onSecondary = TextPrimary
    val onBackground = TextPrimary
    val onSurface = TextPrimary
}
