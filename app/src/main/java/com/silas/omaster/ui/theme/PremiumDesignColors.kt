package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * =====================================================
 * OMaster 精致高端设计色彩系统
 * =====================================================
 * 设计理念：高端精致、行业顶尖、摄影美学
 * 目标用户：OPPO Find 系列高端摄影用户
 * 视觉定位：奢华、精致、专业、有深度
 */

// ==================== 品牌核心色 ====================

/**
 * 琥珀金 - 高端主色
 * 源自高端相机金属质感，代表尊贵与专业
 */
val AmberGold = Color(0xFFD4A574)
val AmberGoldLight = Color(0xFFE8C9A8)
val AmberGoldDark = Color(0xFFB88858)
val AmberGold50 = Color(0x1AD4A574)
val AmberGold100 = Color(0x33D4A574)

/**
 * 哈苏橙 - 品牌灵魂
 * 经典哈苏相机配色，代表专业摄影
 */
val HasselbladOrange = Color(0xFFE07B39)
val HasselbladOrangeLight = Color(0xFFFF9B5C)
val HasselbladOrangeDark = Color(0xFFC66A2E)
val HasselbladOrange50 = Color(0x1AE07B39)
val HasselbladOrange100 = Color(0x33E07B39)

/**
 * 深邃蓝 - 科技感
 * OPPO Find系列屏幕色彩，代表前沿科技
 */
val DeepBlue = Color(0xFF1E3A5F)
val DeepBlueLight = Color(0xFF2D5A8A)
val DeepBlueDark = Color(0xFF0F1D30)
val DeepBlue50 = Color(0x1A1E3A5F)

/**
 * 翡翠绿 - 生命力
 * OPPO Find影像色彩，代表鲜活与真实
 */
val JadeGreen = Color(0xFF2ECC71)
val JadeGreenLight = Color(0xFF58D68D)
val JadeGreenDark = Color(0xFF27AE60)

// ==================== 精致中性色 ====================

/**
 * 碳黑 - 主背景
 * 模拟高端相机机身的深邃黑色
 */
val CarbonBlack = Color(0xFF0D0D0F)
val CarbonBlackElevated = Color(0xFF151518)
val CarbonBlackSurface = Color(0xFF1D1D21)

/**
 * 钛灰 - 卡片背景
 * 模拟高端相机金属质感
 */
val TitaniumGray = Color(0xFF252528)
val TitaniumGrayLight = Color(0xFF2F2F33)
val TitaniumGrayDark = Color(0xFF1A1A1D)

/**
 * 银灰 - 边框与分割
 * 模拟高端金属边框反光
 */
val SilverGray = Color(0xFF3A3A40)
val SilverGrayLight = Color(0xFF4A4A52)
val SilverGrayDark = Color(0xFF2A2A30)

// ==================== 文字色彩系统 ====================

/**
 * 文字层级 - 精致细腻
 * 通过微妙变化体现层次感
 */
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xB3F5F5F7)
val TextTertiary = Color(0x80F5F5F7)
val TextQuaternary = Color(0x59F5F5F7)
val TextDisabled = Color(0x33F5F5F7)

/**
 * 文字强调色
 */
val TextAccent = AmberGold
val TextHighlight = HasselbladOrange

// ==================== 边框与分割线 ====================

/**
 * 边框 - 精致细腻
 * 使用极低透明度，保持界面整洁
 */
val BorderSubtle = Color(0x14FFFFFF)
val BorderLight = Color(0x0AFFFFFF)
val BorderAccent = AmberGold50
val BorderFocus = AmberGold100

/**
 * 分割线 - 极细极淡
 */
val DividerLight = Color(0x0CFFFFFF)
val DividerSubtle = Color(0x06FFFFFF)

// ==================== 功能色 ====================

/**
 * 状态色 - 精致和谐
 */
val SuccessGold = JadeGreen
val SuccessGoldLight = JadeGreenLight
val SuccessGoldDark = JadeGreenDark

val WarningGold = Color(0xFFF4B942)
val WarningGoldLight = Color(0xFFFFD47A)
val WarningGoldDark = Color(0xFFE6A830)

val ErrorRose = Color(0xFFEF5350)
val ErrorRoseLight = Color(0xFFFF7F7F)
val ErrorRoseDark = Color(0xFFE03C3E)

val InfoSapphire = Color(0xFF42A5F5)
val InfoSapphireLight = Color(0xFF64B5F6)
val InfoSapphireDark = Color(0xFF1E88E5)

// ==================== 渐变色组 ====================

/**
 * 琥珀金渐变 - 用于高端元素
 */
val GradientAmberGold = listOf(
    AmberGoldLight,
    AmberGold,
    AmberGoldDark
)

/**
 * 哈苏橙渐变 - 用于重点强调
 */
val GradientHasselblad = listOf(
    HasselbladOrangeLight,
    HasselbladOrange,
    HasselbladOrangeDark
)

/**
 * 深邃蓝渐变 - 用于背景层次
 */
val GradientDeepBlue = listOf(
    DeepBlueDark,
    DeepBlue,
    DeepBlueLight
)

/**
 * 碳黑渐变 - 用于主背景
 */
val GradientCarbon = listOf(
    CarbonBlack,
    CarbonBlackElevated,
    CarbonBlackSurface
)

/**
 * 卡片悬停光效 - 精致的金色光晕
 */
val GradientCardGlow = listOf(
    AmberGold50,
    Color.Transparent
)

/**
 * 封面图渐变 - 从上到下柔和过渡
 */
val GradientCoverFade = listOf(
    Color.Transparent,
    CarbonBlack.copy(alpha = 0.3f),
    CarbonBlack.copy(alpha = 0.9f)
)

/**
 * 琥珀光效 - 用于焦点提示
 */
val GradientAmberGlow = listOf(
    AmberGold.copy(alpha = 0.3f),
    AmberGold.copy(alpha = 0.1f),
    Color.Transparent
)

// ==================== 联名品牌色 ====================

/**
 * 徕卡红 - 经典与传承
 */
val LeicaRed = Color(0xFFD6001F)
val LeicaRedLight = Color(0xFFE6394D)
val LeicaRedDark = Color(0xFFB30019)

/**
 * 蔡司蓝 - 光学传奇
 */
val ZeissBlue = Color(0xFF0057B8)
val ZeissBlueLight = Color(0xFF1E7FE0)
val ZeissBlueDark = Color(0xFF004080)

/**
 * 富士绿 - 复古情怀
 */
val FujiGreen = Color(0xFF00AA55)
val FujiGreenLight = Color(0xFF22CC77)
val FujiGreenDark = Color(0xFF008844)

// ==================== 相机参数色 ====================

/**
 * 参数色 - 精致编码
 */
val ColorISO = AmberGold
val ColorShutter = JadeGreen
val ColorAperture = InfoSapphire
val ColorEV = WarningGold
val ColorWB = Color(0xFF9B59B6)

// ==================== 透明度常量 ====================

const val AlphaOpaque = 1.0f
const val AlphaHeavy = 0.85f
const val AlphaMedium = 0.7f
const val AlphaLight = 0.5f
const val AlphaSubtle = 0.3f
const val AlphaFaint = 0.15f
const val AlphaTrace = 0.08f

// ==================== 兼容旧版本 ====================

val DeepSpaceBlack = CarbonBlack
val DeepSpaceBlackElevated = CarbonBlackElevated
val DeepSpaceBlackSurface = CarbonBlackSurface

val CardBackgroundPrimary = TitaniumGray
val CardBackgroundSecondary = TitaniumGrayLight
val CardBackgroundTertiary = TitaniumGrayDark

val SurfacePrimary = CarbonBlackElevated
val SurfaceSecondary = CarbonBlackSurface
val SurfaceTertiary = TitaniumGray

val TextAccentColor = AmberGold
val TextPrimaryColor = TextPrimary
val TextSecondaryColor = TextSecondary

val BorderPrimary = BorderSubtle
val BorderSecondary = BorderLight

// ==================== 主题枚举 ====================

enum class PremiumTheme(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val gradient: List<Color>
) {
    AmberGold(
        displayName = "琥珀金",
        primaryColor = AmberGold,
        secondaryColor = AmberGoldLight,
        gradient = GradientAmberGold
    ),
    Hasselblad(
        displayName = "哈苏",
        primaryColor = HasselbladOrange,
        secondaryColor = HasselbladOrangeLight,
        gradient = GradientHasselblad
    ),
    DeepBlue(
        displayName = "深邃蓝",
        primaryColor = DeepBlue,
        secondaryColor = DeepBlueLight,
        gradient = GradientDeepBlue
    )
}
