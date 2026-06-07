package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OMaster Web风格色彩系统 ====================
// 融合Web展示页面设计 - 专业、现代、高端

// ========== 核心品牌色 - Web风格哈苏橙 ==========
// 更鲜艳的哈苏橙，与Web端一致
val HasselbladOrange = Color(0xFFFF6B35)       // Web主色调
val HasselbladOrangeLight = Color(0xFFFF8F5C)  // 浅色变体
val HasselbladOrangeDark = Color(0xFFE55A25)   // 深色变体

// OPPO绿色 - Web风格
val OppoGreen = Color(0xFF00D68F)              // Web辅助色
val OppoGreenLight = Color(0xFF00B4D8)         // 浅色变体

// ========== 深色主题 - Web风格深空黑 ==========
// 与Web展示页面一致的深色背景
val PureBlack = Color(0xFF0A0A0F)              // Web深色背景
val DarkBackground = Color(0xFF0A0A0F)         // 主背景
val CardBackground = Color(0xFF12121A)         // 卡片背景
val SurfaceBackground = Color(0xFF1A1A24)      // Surface背景

// ColorOS兼容色名
val ColorOSBlack = Color(0xFF0A0A0F)
val ColorOSBlackElevated = Color(0xFF12121A)
val ColorOSCard = Color(0xFF12121A)
val ColorOSGlass = Color(0xCC0A0A0F)
val ColorOSGlassLight = Color(0x800A0A0F)
val DeepSpace = Color(0xFF050510)

// ========== 文字层级 - Web风格 ==========
val TextPrimary = Color(0xFFFFFFFF)            // 主文字
val TextSecondary = Color(0xB3FFFFFF)          // 次文字 (70%透明)
val TextTertiary = Color(0x66FFFFFF)           // 三级文字 (40%透明)
val TextMuted = Color(0x40FFFFFF)              // 暗淡文字 (25%透明)

// ColorOS兼容文字色名
val ColorOSTextPrimary = Color(0xFFFFFFFF)
val ColorOSTextSecondary = Color(0xB3FFFFFF)
val ColorOSTextTertiary = Color(0x66FFFFFF)
val ColorOSTextQuaternary = Color(0x40FFFFFF)

// ========== 边框系统 - Web风格 ==========
val BorderDefault = Color(0x0DFFFFFF)          // 默认边框 (5%透明)
val BorderLight = Color(0x14FFFFFF)            // 浅边框 (8%透明)
val BorderFocus = Color(0x33FF6B35)            // 聚焦边框

// ColorOS兼容边框色名
val ColorOSBorder = Color(0x0DFFFFFF)
val ColorOSBorderLight = Color(0x14FFFFFF)

// ========== 功能色 - Web风格 ==========
val SuccessPro = Color(0xFF00D68F)             // 成功色（OPPO绿）
val WarningPro = Color(0xFFF59E0B)             // 警告色
val ErrorPro = Color(0xFFEF4444)               // 错误色
val InfoPro = Color(0xFF3B82F6)                // 信息色

// ========== 品牌辅助色 ==========
val LeicaRed = Color(0xFFE60012)               // 徕卡红
val ZeissBlue = Color(0xFF0066CC)              // 蔡司蓝
val StampBrown = Color(0xFF8B4513)             // 邮票棕
val ChineseRed = Color(0xFFC41E3A)             // 国风红
val FilmBlack = Color(0xFF1A1A1A)              // 胶片黑

// ========== 渐变色组 - Web风格 ==========
// 哈苏橙渐变
val GradientHasselblad = listOf(
    Color(0xFFFF6B35),
    Color(0xFFFF8F5C)
)

// OPPO绿渐变
val GradientOppoGreen = listOf(
    Color(0xFF00D68F),
    Color(0xFF00B4D8)
)

// AI紫色渐变
val GradientAI = listOf(
    Color(0xFF667EEA),
    Color(0xFF764BA2)
)

// 深空渐变
val GradientDeepSpace = listOf(
    Color(0xFF1A1A2E),
    Color(0xFF16213E),
    Color(0xFF0F3460)
)

// 品牌主题枚举
enum class BrandTheme(
    val displayName: String,
    val primaryColor: Color
) {
    Hasselblad("哈苏", HasselbladOrange),
    OPPO("OPPO", OppoGreen),
    OnePlus("一加", Color(0xFFF05A28)),
    Realme("realme", Color(0xFFFFD900))
}

// ========== 浅色主题 - 晨曦白 ==========
val ColorOSLightBackground = Color(0xFFFDFDFD)
val ColorOSLightSurface = Color(0xFFFFFFFF)
val ColorOSLightCard = Color(0xFFFAFAFA)
val ColorOSLightGlass = Color(0xCCFFFFFF)

val ColorOSLightTextPrimary = Color(0xFF18181B)
val ColorOSLightTextSecondary = Color(0xFF71717A)
val ColorOSLightTextTertiary = Color(0xFFA1A1AA)
val ColorOSLightTextQuaternary = Color(0xFFD4D4D8)

val ColorOSLightBorder = Color(0xFFE4E4E7)
val ColorOSLightBorderLight = Color(0xFFF4F4F5)

// ========== 兼容旧色名 ==========
val OppoGold = Color(0xFFD4A857)
val OppoGoldLight = Color(0xFFE8C99A)
val OppoGoldDark = Color(0xFFB88838)

val DeepOceanBlue = Color(0xFF0066CC)
val DeepOceanBlueLight = Color(0xFF3399FF)
val DeepOceanBlueDark = Color(0xFF004C99)

val AuroraGreen = Color(0xFF22C55E)
val AuroraGreenLight = Color(0xFF4ADE80)
val AuroraGreenDark = Color(0xFF16A34A)

val SunsetRed = Color(0xFFEF4444)
val SunsetRedLight = Color(0xFFF87171)
val SunsetRedDark = Color(0xFFDC2626)

val CosmicPurple = Color(0xFF8B5CF6)
val CosmicPurpleLight = Color(0xFFA78BFA)
val CosmicPurpleDark = Color(0xFF7C3AED)

val AccentPrimary = HasselbladOrange
val HasselbladOrangePro = Color(0xFFFF8C42)
val OppoSunriseGold = Color(0xFFFFB300)

val GradientHasselbladMaster = GradientHasselblad
val GradientFindXPro = listOf(Color(0xFFFFC266), Color(0xFFD48838))
val GradientDeepOcean = listOf(Color(0xFF0066CC), Color(0xFF004488))
val GradientAuroraMaster = listOf(Color(0xFF22C55E), Color(0xFF059669))
val GradientPortraitPro = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
val GradientCosmicArt = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
val GradientLuxuryGold = listOf(Color(0xFFD4A857), Color(0xFFB88838))

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

const val AlphaOpaque = 1.0f
const val AlphaHeavy = 0.8f
const val AlphaMedium = 0.6f
const val AlphaLight = 0.4f
const val AlphaSubtle = 0.2f
const val AlphaUltraLight = 0.1f

val ColorISO = Color(0xFFEF4444)
val ColorShutter = Color(0xFF22C55E)
val ColorEV = Color(0xFF3B82F6)
val ColorWB = Color(0xFFF59E0B)

val HasselbladOrangeSubtle = Color(0xFF806030)
val HasselbladOrangeLight_Old = Color(0xFFFFD190)
val HasselbladOrangeDark_Old = Color(0xFFE59427)

// ========== 兼容旧组件颜色 ==========
val DarkGray = Color(0xFF1A1A1A)                    // 旧卡片背景色
val CardBorderLight = Color(0x14FFFFFF)             // 旧边框色
val CardBorderHighlight = Color(0x33FF6B35)         // 旧高亮边框色