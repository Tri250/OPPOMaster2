package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * =====================================================
 * OMaster 设计系统 - OPPOMaster Web风格
 * =====================================================
 * 设计风格：深灰背景 + 橙色主色调
 * 参考：https://github.com/Tri250/OPPOMaster
 */

// ==================== 核心背景色 ====================

/**
 * Zinc色系 - 深灰背景
 * 主背景色，营造专业沉稳氛围
 */
val Zinc900 = Color(0xFF18181B)      // 主背景
val Zinc800 = Color(0xFF27272A)      // 卡片背景
val Zinc700 = Color(0xFF3F3F46)      // 边框/分割
val Zinc600 = Color(0xFF52525B)      // 悬停状态
val Zinc500 = Color(0xFF71717A)      // 禁用文字
val Zinc400 = Color(0xFFA1A1AA)      // 副标题文字
val Zinc300 = Color(0xFFD4D4D8)      // 次级文字
val Zinc200 = Color(0xFFE4E4E7)      // 浅色文字
val Zinc100 = Color(0xFFF4F4F5)      // 最浅文字

// ==================== 橙色主色调 ====================

/**
 * Orange色系 - 品牌主色
 * 代表活力、专业、哈苏认证
 */
val Orange500 = Color(0xFFF97316)    // 主色调
val Orange600 = Color(0xFFEA580C)    // 深色变体
val Orange400 = Color(0xFFFB923C)    // 浅色变体
val Orange700 = Color(0xFFC2410C)    // 更深变体
val Orange300 = Color(0xFFFDBA74)    // 更浅变体

// 橙色透明度变体（用于光晕效果）
val Orange500_20 = Color(0x33F97316) // 20%透明度光晕
val Orange500_10 = Color(0x19F97316) // 10%透明度光晕
val Orange500_30 = Color(0x4DF97316) // 30%透明度边框
val Orange500_25 = Color(0x40F97316) // 25%透明度阴影

// ==================== 文字色彩 ====================

val TextWhite = Color(0xFFFFFFFF)           // 主标题
val TextWhiteSemi = Color(0xB3FFFFFF)       // 次标题 70%
val TextZinc400 = Zinc400                    // 副标题
val TextZinc500 = Zinc500                    // 描述文字
val TextZinc600 = Color(0x8071717A)          // 辅助文字 50%

// ==================== 边框与分割 ====================

val BorderZinc700_50 = Color(0x803F3F46)     // 边框 50%
val BorderZinc800 = Color(0x4027272A)        // 边框 25%
val BorderOrange500_30 = Orange500_30        // 橙色边框

// ==================== 功能色 ====================

val Yellow400 = Color(0xFFFACC15)            // 星星评分
val Yellow500 = Color(0xFFEAB308)            // 星星填充

val Green500 = Color(0xFF22C55E)             // 成功/下载
val Green600 = Color(0xFF16A34A)             // 深绿

val Blue500 = Color(0xFF3B82F6)              // 信息
val Blue600 = Color(0xFF2563EB)              // 深蓝

val Red500 = Color(0xFFEF4444)               // 错误
val Red600 = Color(0xFFDC2626)               // 深红

// ==================== 渐变色组 ====================

/**
 * 橙色渐变 - 用于按钮、强调元素
 */
val GradientOrange = listOf(
    Orange400,
    Orange500,
    Orange600
)

/**
 * Zinc渐变 - 用于背景层次
 */
val GradientZinc = listOf(
    Zinc900,
    Zinc800,
    Zinc900
)

/**
 * 遮罩渐变 - 用于图片遮罩
 */
val GradientOverlay = listOf(
    Color.Transparent,
    Color(0x3318181B),
    Color(0xE018181B)
)

/**
 * 光晕渐变 - 用于装饰效果
 */
val GradientGlow = listOf(
    Orange500_20,
    Orange500_10,
    Color.Transparent
)

// ==================== 兼容旧版本 ====================

val DeepSpaceBlack = Zinc900
val DeepSpaceBlackElevated = Zinc800
val DeepSpaceBlackSurface = Zinc700

val CardBackgroundPrimary = Zinc800
val CardBackgroundSecondary = Zinc700

val AmberGold = Orange500
val AmberGoldLight = Orange400
val AmberGoldDark = Orange600

val TextPrimary = TextWhite
val TextSecondary = TextZinc400
val TextTertiary = TextZinc500

val BorderPrimary = BorderZinc700_50
val BorderAccent = BorderOrange500_30

// ==================== 主题枚举 ====================

enum class OMasterTheme(
    val displayName: String,
    val primaryColor: Color,
    val backgroundColor: Color,
    val gradient: List<Color>
) {
    Orange(
        displayName = "橙色主题",
        primaryColor = Orange500,
        backgroundColor = Zinc900,
        gradient = GradientOrange
    ),
    Zinc(
        displayName = "深灰主题",
        primaryColor = Zinc400,
        backgroundColor = Zinc900,
        gradient = GradientZinc
    )
}