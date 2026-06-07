package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OPPO Find X8 Pro 哈苏影像色彩系统 ====================
// 基于ColorOS 16 Aquatic Design - 专业摄影、简约大气

// ========== 品牌主色调 - 哈苏专业影像系列 ==========
// 哈苏经典橙 - 专业、高端
val HasselbladOrange = Color(0xFFFFB347)
val HasselbladOrangeLight = Color(0xFFFFD190)
val HasselbladOrangeDark = Color(0xFFE59427)
val HasselbladOrangeSubtle = Color(0xFF806030)

// OPPO 金 - Find系列
val OppoGold = Color(0xFFD4A857)
val OppoGoldLight = Color(0xFFE8C99A)
val OppoGoldDark = Color(0xFFB88838)

// ========== 专业摄影辅助色 ==========
// 深海蓝 - 夜景专业
val DeepOceanBlue = Color(0xFF0066CC)
val DeepOceanBlueLight = Color(0xFF3399FF)
val DeepOceanBlueDark = Color(0xFF004C99)

// 极光绿 - 风景自然
val AuroraGreen = Color(0xFF22C55E)
val AuroraGreenLight = Color(0xFF4ADE80)
val AuroraGreenDark = Color(0xFF16A34A)

// 日落红 - 人像暖调
val SunsetRed = Color(0xFFEF4444)
val SunsetRedLight = Color(0xFFF87171)
val SunsetRedDark = Color(0xFFDC2626)

// 宇宙紫 - 艺术氛围
val CosmicPurple = Color(0xFF8B5CF6)
val CosmicPurpleLight = Color(0xFFA78BFA)
val CosmicPurpleDark = Color(0xFF7C3AED)

// ========== 功能色 - ColorOS 16 规范 ==========
val SuccessPro = Color(0xFF10B981)
val WarningPro = Color(0xFFF59E0B)
val ErrorPro = Color(0xFFEF4444)
val InfoPro = Color(0xFF3B82F6)

// ========== 深色主题 - 深空黑 Pro ==========
// ColorOS 16 专业摄影深色模式
val ColorOSBlack = Color(0xFF080808)
val ColorOSBlackElevated = Color(0xFF121212)
val ColorOSCard = Color(0xFF1A1A1A)
val ColorOSGlass = Color(0xCC080808)
val ColorOSGlassLight = Color(0x80080808)
val DeepSpace = Color(0xFF050510)  // 深空背景

// 专业文字层级
val ColorOSTextPrimary = Color(0xFFFAFAFA)
val ColorOSTextSecondary = Color(0xFFA1A1AA)
val ColorOSTextTertiary = Color(0xFF71717A)
val ColorOSTextQuaternary = Color(0xFF52525B)

// 专业边框
val ColorOSBorder = Color(0xFF27272A)
val ColorOSBorderLight = Color(0xFF3F3F46)

// ========== 浅色主题 - 晨曦白 Pro ==========
val ColorOSLightBackground = Color(0xFFFDFDFD)
val ColorOSLightSurface = Color(0xFFFFFFFF)
val ColorOSLightCard = Color(0xFFFAFAFA)
val ColorOSLightGlass = Color(0xCCFFFFFF)

// 浅色文字
val ColorOSLightTextPrimary = Color(0xFF18181B)
val ColorOSLightTextSecondary = Color(0xFF71717A)
val ColorOSLightTextTertiary = Color(0xFFA1A1AA)
val ColorOSLightTextQuaternary = Color(0xFFD4D4D8)

// 浅色边框
val ColorOSLightBorder = Color(0xFFE4E4E7)
val ColorOSLightBorderLight = Color(0xFFF4F4F5)

val AccentPrimary = HasselbladOrange

// ========== 哈苏 Pro 系列辅助色 ==========
val HasselbladOrangePro = Color(0xFFFF8C42)  // 哈苏专业橙
val OppoSunriseGold = Color(0xFFFFB300)      // OPPO 晨曦金

// ========== 专业摄影渐变色 ==========
// 哈苏大师渐变
val GradientHasselbladMaster = listOf(
    Color(0xFFFFD166),
    Color(0xFFE89427)
)

// Find X Pro 专业渐变
val GradientFindXPro = listOf(
    Color(0xFFFFC266),
    Color(0xFFD48838)
)

// 深海夜景渐变
val GradientDeepOcean = listOf(
    Color(0xFF0066CC),
    Color(0xFF004488)
)

// 极光风景渐变
val GradientAuroraMaster = listOf(
    Color(0xFF22C55E),
    Color(0xFF059669)
)

// 专业人像渐变
val GradientPortraitPro = listOf(
    Color(0xFFEF4444),
    Color(0xFFDC2626)
)

// 宇宙艺术渐变
val GradientCosmicArt = listOf(
    Color(0xFF8B5CF6),
    Color(0xFF7C3AED)
)

// 金色高端渐变
val GradientLuxuryGold = listOf(
    Color(0xFFD4A857),
    Color(0xFFB88838)
)

// ========== 专业灰阶系统 - ColorOS 16 ==========
val ColorOSGrey50 = Color(0xFFFAFAFA)
val ColorOSGrey100 = Color(0xFFF4F4F5)
val ColorOSGrey200 = Color(0xFFE4E4E7)
val ColorOSGrey300 = Color(0xFFD4D4D8)
val ColorOSGrey400 = Color(0xFFA1A1AA)
val ColorOSGrey500 = Color(0xFF71717A)
val ColorOSGrey600 = Color(0xFF52525B)
val ColorOSGrey700 = Color(0xFF3F3F46)
val ColorOSGrey800 = Color(0xFF27272A)
val ColorOSGrey900 = Color(0xFF18181B)

// ========== 专业透明度规范 - ColorOS 16 ==========
const val AlphaOpaque = 1.0f         // 完全不透明
const val AlphaHeavy = 0.8f         // 重度
const val AlphaMedium = 0.6f        // 中度
const val AlphaLight = 0.4f         // 轻度
const val AlphaSubtle = 0.2f        // 微妙
const val AlphaUltraLight = 0.1f    // 极轻

// ========== 相机参数专用色 ==========
val ColorISO = Color(0xFFEF4444)          // ISO - 红色警示
val ColorShutter = Color(0xFF22C55E)     // 快门 - 绿色专业
val ColorEV = Color(0xFF3B82F6)          // EV - 蓝色科技
val ColorWB = Color(0xFFF59E0B)          // 白平衡 - 橙色温度
