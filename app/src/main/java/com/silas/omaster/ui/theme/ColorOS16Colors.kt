package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * =====================================================
 * ColorOS 16 专业摄影设计规范 - 色彩系统
 * =====================================================
 * 设计标准：Aquatic Design 水感设计
 * 目标用户：OPPO Find 系列高端摄影用户
 * 视觉定位：专业、高端、精致、有质感
 */

// ==================== 品牌核心色 ====================
/**
 * 哈苏橙 - 品牌主色
 * 源自哈苏HNCS色彩科学，代表专业摄影
 * 使用场景：主按钮、重点强调、HNCS认证标识
 */
val HasselbladOrange = Color(0xFFFF6B35)
val HasselbladOrangeLight = Color(0xFFFF8A64)
val HasselbladOrangeDark = Color(0xFFE55A28)
val HasselbladOrange50 = Color(0x1AFF6B35)  // 10%透明度
val HasselbladOrange100 = Color(0x33FF6B35) // 20%透明度
val HasselbladOrange200 = Color(0x66FF6B35) // 40%透明度

/**
 * OPPO绿 - 系统辅助色
 * ColorOS系统品牌色，代表活力与创新
 * 使用场景：成功状态、开关开启、正向反馈
 */
val OPPOGreen = Color(0xFF00D68F)
val OPPOGreenLight = Color(0xFF33E0A8)
val OPPOGreenDark = Color(0xFF00B87A)
val OPPOGreen50 = Color(0x1A00D68F)
val OPPOGreen100 = Color(0x3300D68F)

// ==================== 深色主题 - 深空黑系列 ====================
/**
 * 深空黑 - 主背景色
 * 模拟深空摄影的纯净黑色，减少眼部疲劳
 */
val DeepSpaceBlack = Color(0xFF0A0A0F)
val DeepSpaceBlackElevated = Color(0xFF12121A)
val DeepSpaceBlackSurface = Color(0xFF1A1A24)

/**
 * 卡片层级背景
 * 通过微妙的变化区分层级，保持整体沉浸感
 */
val CardBackgroundPrimary = Color(0xFF15151D)
val CardBackgroundSecondary = Color(0xFF1C1C26)
val CardBackgroundTertiary = Color(0xFF23232F)

/**
 * 表面层级
 * 用于对话框、底部面板等浮层
 */
val SurfacePrimary = Color(0xFF1E1E28)
val SurfaceSecondary = Color(0xFF252532)
val SurfaceTertiary = Color(0xFF2C2C3C)

// ==================== 文字色彩系统 ====================
/**
 * 文字层级 - 基于透明度
 * 遵循WCAG 2.1 AA级对比度标准
 */
val TextPrimary = Color(0xFFFFFFFF)              // 主文字 100%
val TextSecondary = Color(0xB3FFFFFF)            // 次文字 70%
val TextTertiary = Color(0x80FFFFFF)             // 辅助文字 50%
val TextQuaternary = Color(0x4DFFFFFF)           // 占位文字 30%
val TextDisabled = Color(0x33FFFFFF)             // 禁用文字 20%

/**
 * 特殊文字色
 */
val TextAccent = HasselbladOrange                // 强调文字
val TextSuccess = OPPOGreen                      // 成功文字
val TextWarning = Color(0xFFFFB800)              // 警告文字
val TextError = Color(0xFFFF4D4F)                // 错误文字

// ==================== 边框与分割线 ====================
/**
 * 边框系统 - 极细且微妙
 * 使用低透明度白色，保持界面整洁
 */
val BorderPrimary = Color(0x1AFFFFFF)            // 主边框 10%
val BorderSecondary = Color(0x0DFFFFFF)          // 次边框 5%
val BorderAccent = HasselbladOrange50            // 强调边框
val BorderFocus = HasselbladOrange100            // 聚焦边框

/**
 * 分割线
 */
val DividerPrimary = Color(0x14FFFFFF)           // 主分割线 8%
val DividerSecondary = Color(0x0AFFFFFF)         // 次分割线 4%

// ==================== 功能色 ====================
/**
 * 状态色 - 符合ColorOS 16规范
 */
val SuccessColor = OPPOGreen
val SuccessColorLight = Color(0xFF33E0A8)
val SuccessColorDark = Color(0xFF00B87A)

val WarningColor = Color(0xFFFFB800)
val WarningColorLight = Color(0xFFFFCC33)
val WarningColorDark = Color(0xFFE6A600)

val ErrorColor = Color(0xFFFF4D4F)
val ErrorColorLight = Color(0xFFFF7879)
val ErrorColorDark = Color(0xFFE03C3E)

val InfoColor = Color(0xFF1890FF)
val InfoColorLight = Color(0xFF4CA3FF)
val InfoColorDark = Color(0xFF0E7AE6)

// ==================== 渐变色组 ====================
/**
 * 哈苏橙渐变 - 用于重点元素
 */
val GradientHasselbladPrimary = listOf(
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
 * 深空渐变 - 用于背景
 */
val GradientDeepSpace = listOf(
    DeepSpaceBlack,
    DeepSpaceBlackElevated,
    DeepSpaceBlackSurface
)

/**
 * 卡片悬停光效 - 微妙的橙色光晕
 */
val GradientCardHover = listOf(
    HasselbladOrange50,
    Color.Transparent
)

/**
 * 封面图遮罩 - 从上到下渐变
 */
val GradientCoverMask = listOf(
    Color.Transparent,
    DeepSpaceBlack.copy(alpha = 0.4f),
    DeepSpaceBlack.copy(alpha = 0.8f)
)

/**
 * Hero区域背景渐变
 */
val GradientHeroBackground = listOf(
    DeepSpaceBlack,
    DeepSpaceBlackElevated,
    Color(0xFF1A1A2E)
)

// ==================== 品牌辅助色 ====================
/**
 * 联名品牌色 - 用于水印和品牌展示
 */
val LeicaRed = Color(0xFFE60012)
val ZeissBlue = Color(0xFF0066CC)
val OnePlusRed = Color(0xFFF50514)
val RealmeYellow = Color(0xFFFFD400)

/**
 * 国风色彩 - 用于国风水印
 */
val ChineseRed = Color(0xFFC41E3A)
val ChineseGold = Color(0xFFFFD700)
val InkBlack = Color(0xFF1A1A1A)

// ==================== 相机参数专用色 ====================
/**
 * 参数显示色彩编码
 * 便于快速识别不同类型的参数
 */
val ColorISO = Color(0xFFFF6B35)        // 哈苏橙 - ISO
val ColorShutter = Color(0xFF00D68F)    // OPPO绿 - 快门
val ColorAperture = Color(0xFF1890FF)   // 蓝色 - 光圈
val ColorEV = Color(0xFFFFB800)         // 黄色 - 曝光补偿
val ColorWB = Color(0xFF8B5CF6)         // 紫色 - 白平衡

// ==================== 透明度常量 ====================
const val Alpha100 = 1.0f
const val Alpha90 = 0.9f
const val Alpha80 = 0.8f
const val Alpha70 = 0.7f
const val Alpha60 = 0.6f
const val Alpha50 = 0.5f
const val Alpha40 = 0.4f
const val Alpha30 = 0.3f
const val Alpha20 = 0.2f
const val Alpha10 = 0.1f
const val Alpha5 = 0.05f

// ==================== 兼容旧版本色名 ====================
val ColorOSBlack = DeepSpaceBlack
val ColorOSBlackElevated = DeepSpaceBlackElevated
val ColorOSCard = CardBackgroundPrimary
val ColorOSGlass = DeepSpaceBlack.copy(alpha = 0.8f)
val ColorOSGlassLight = DeepSpaceBlack.copy(alpha = 0.5f)

val ColorOSTextPrimary = TextPrimary
val ColorOSTextSecondary = TextSecondary
val ColorOSTextTertiary = TextTertiary
val ColorOSTextQuaternary = TextQuaternary

val ColorOSBorder = BorderPrimary
val ColorOSBorderLight = BorderSecondary

val OppoGold = Color(0xFFD4A857)
val DeepOceanBlue = Color(0xFF0066CC)
val AuroraGreen = Color(0xFF22C55E)
val SunsetRed = ErrorColor
val CosmicPurple = Color(0xFF8B5CF6)

// ==================== 主题枚举 ====================
enum class ColorOSTheme(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    Hasselblad(
        displayName = "哈苏专业",
        primaryColor = HasselbladOrange,
        secondaryColor = HasselbladOrangeLight,
        accentColor = HasselbladOrangeDark
    ),
    OPPO(
        displayName = "OPPO经典",
        primaryColor = OPPOGreen,
        secondaryColor = OPPOGreenLight,
        accentColor = OPPOGreenDark
    ),
    OnePlus(
        displayName = "一加极客",
        primaryColor = OnePlusRed,
        secondaryColor = Color(0xFFFF3333),
        accentColor = Color(0xFFCC0000)
    ),
    Realme(
        displayName = "真我潮流",
        primaryColor = RealmeYellow,
        secondaryColor = Color(0xFFFFE066),
        accentColor = Color(0xFFCCAA00)
    )
}
