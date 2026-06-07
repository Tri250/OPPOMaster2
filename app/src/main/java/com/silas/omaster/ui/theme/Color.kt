package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== OMaster Web风格色彩系统 ====================
// 融合Web端framer-motion设计 - 专业、现代、高端
// 参考：https://github.com/Tri250/OPPOMaster Web端组件

// ========== 核心品牌色 - Web风格哈苏橙 ==========
// 与Web端Hero/Features/PresetCard组件一致
val HasselbladOrange = Color(0xFFF97316)       // Web orange-500
val HasselbladOrangeLight = Color(0xFFFF8F5C)  // Web orange-400
val HasselbladOrangeDark = Color(0xFFEA580C)   // Web orange-600

// OPPO绿色 - Web风格
val OppoGreen = Color(0xFF00D68F)              // Web辅助色
val OppoGreenLight = Color(0xFF00B4D8)         // 浅色变体

// ========== 深色主题 - Web风格Zinc色系 ==========
// 与Web端 zinc-900/zinc-800/zinc-700 一致
val Zinc900 = Color(0xFF18181B)                // Web zinc-900 主背景
val Zinc800 = Color(0xFF27272A)                // Web zinc-800 卡片背景
val Zinc700 = Color(0xFF3F3F46)                // Web zinc-700 边框
val Zinc600 = Color(0xFF52525B)                // Web zinc-600
val Zinc500 = Color(0xFF71717A)                // Web zinc-500 次文字
val Zinc400 = Color(0xFFA1A1AA)                // Web zinc-400
val Zinc300 = Color(0xFFD4D4D8)                // Web zinc-300
val Zinc200 = Color(0xFFE4E4E7)                // Web zinc-200

// 深色背景 - Web风格
val DarkBackground = Color(0xFF18181B)         // zinc-900
val CardBackground = Color(0xFF27272A)         // zinc-800
val SurfaceBackground = Color(0xFF3F3F46)      // zinc-700

// ColorOS兼容色名
val ColorOSBlack = Color(0xFF18181B)           // zinc-900
val ColorOSBlackElevated = Color(0xFF27272A)   // zinc-800
val ColorOSCard = Color(0xFF27272A)            // zinc-800
val ColorOSGlass = Color(0xCC18181B)           // 80%透明
val ColorOSGlassLight = Color(0x8018181B)      // 50%透明
val DeepSpace = Color(0xFF0A0A0F)              // 更深的背景

// ========== 文字层级 - Web风格 ==========
val TextPrimary = Color(0xFFFFFFFF)            // 主文字 white
val TextSecondary = Color(0xFFA1A1AA)          // 次文字 zinc-400
val TextTertiary = Color(0xFF71717A)           // 三级文字 zinc-500
val TextMuted = Color(0xFF52525B)              // 暗淡文字 zinc-600

// ColorOS兼容文字色名
val ColorOSTextPrimary = Color(0xFFFFFFFF)
val ColorOSTextSecondary = Color(0xFFA1A1AA)
val ColorOSTextTertiary = Color(0xFF71717A)
val ColorOSTextQuaternary = Color(0xFF52525B)

// ========== 边框系统 - Web风格 ==========
val BorderDefault = Color(0x803F3F46)          // zinc-700 50%透明
val BorderLight = Color(0x403F3F46)            // zinc-700 25%透明
val BorderFocus = Color(0x30F97316)            // orange-500 20%透明

// ColorOS兼容边框色名
val ColorOSBorder = Color(0x803F3F46)
val ColorOSBorderLight = Color(0x403F3F46)

// ========== 功能色 - Web风格 ==========
val SuccessPro = Color(0xFF00D68F)             // 成功色（OPPO绿）
val WarningPro = Color(0xFFF59E0B)             // 警告色 amber-500
val ErrorPro = Color(0xFFEF4444)               // 错误色 red-500
val InfoPro = Color(0xFF3B82F6)                // 信息色 blue-500

// ========== 品牌辅助色 ==========
val LeicaRed = Color(0xFFE60012)               // 徕卡红
val ZeissBlue = Color(0xFF0066CC)              // 蔡司蓝
val StampBrown = Color(0xFF8B4513)             // 邮票棕
val ChineseRed = Color(0xFFC41E3A)             // 国风红
val FilmBlack = Color(0xFF1A1A1A)              // 胶片黑

// ========== 渐变色组 - Web风格 ==========
// Hero区域主标题渐变
val GradientHeroTitle = listOf(
    Color(0xFFFF8F5C),     // orange-400
    Color(0xFFEA580C)      // orange-600
)

// Features图标渐变
val GradientFeatureIcon = listOf(
    Color(0xFFF97316),     // orange-500
    Color(0xFFEA580C)      // orange-600
)

// 哈苏橙渐变
val GradientHasselblad = listOf(
    Color(0xFFF97316),
    Color(0xFFEA580C)
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
    Color(0xFF18181B),     // zinc-900
    Color(0xFF27272A),     // zinc-800
    Color(0xFF18181B)      // zinc-900
)

// 卡片悬停光效渐变
val GradientCardHover = listOf(
    Color(0x0DF97316),     // orange-500 5%透明
    Color(0x00000000)      // transparent
)

// 封面图遮罩渐变
val GradientCoverMask = listOf(
    Color(0xFF18181B),     // zinc-900
    Color(0x0018181B),     // transparent
    Color(0x0018181B)      // transparent
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

// 兼容旧组件颜色
val DarkGray = Color(0xFF27272A)                    // zinc-800
val CardBorderLight = Color(0x403F3F46)             // zinc-700 25%透明
val CardBorderHighlight = Color(0x30F97316)         // orange-500 20%透明

val HasselbladOrangeSubtle = Color(0xFF806030)
val HasselbladOrangeLight_Old = Color(0xFFFFD190)
val HasselbladOrangeDark_Old = Color(0xFFE59427)