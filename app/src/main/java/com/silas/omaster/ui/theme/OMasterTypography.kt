package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * =====================================================
 * OMaster 设计系统 - 字体排版
 * =====================================================
 * 字体风格：Inter风格，Bold标题，清晰层次
 * 参考：https://github.com/Tri250/OPPOMaster
 */

// ==================== 核心字体系统 ====================

val OMasterTypography = Typography(
    // Hero大标题 - 用于首页主标题
    // text-5xl md:text-7xl font-bold
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-0.5).sp
    ),
    
    // 大标题 - 用于页面标题
    // text-3xl md:text-4xl font-bold
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.3).sp
    ),
    
    // 标题 - 用于区块标题
    // text-2xl font-bold
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.2).sp
    ),

    // 大标题 - 用于卡片标题
    // text-lg font-bold
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.1).sp
    ),
    
    // 中标题 - 用于卡片名称
    // text-lg font-bold mb-1
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.08).sp
    ),
    
    // 小标题 - 用于副标题
    // text-xl font-semibold
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.05).sp
    ),

    // 标题 - 用于功能名称
    // text-lg font-bold
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // 中标题 - 用于按钮文字
    // font-semibold
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01.sp
    ),
    
    // 小标题 - 用于标签
    // text-sm font-medium
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    ),

    // 正文 - 用于描述文字
    // text-zinc-400 text-lg
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.02.sp
    ),
    
    // 正文 - 用于卡片描述
    // text-zinc-400 text-sm mb-3 line-clamp-2
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    ),
    
    // 小正文 - 用于辅助信息
    // text-zinc-500 text-xs
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.sp
    ),

    // 标签 - 用于按钮文字
    // font-semibold rounded-xl
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    ),
    
    // 小标签 - 用于卡片标签
    // text-xs rounded
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    ),
    
    // 最小标签 - 用于徽章
    // text-xs font-medium
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.sp
    )
)

// ==================== 特殊字体样式 ====================

/**
 * Hero渐变标题 - 用于首页大标题
 * text-transparent bg-clip-text bg-gradient-to-r from-orange-400 to-orange-600
 */
object HeroText {
    val GradientTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.4).sp
    )
    
    val Subtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 统计数字 - 用于数据展示
 * text-2xl font-bold text-white
 */
object StatsText {
    val Value = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    )
    
    val Label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 徽章文字 - 用于认证标识
 * text-xs font-medium
 */
object BadgeText {
    val HNCS = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    )
    
    val Rating = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
    
    val Tag = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.sp
    )
}

/**
 * 卡片文字 - 用于预设卡片
 */
object CardText {
    val Title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.05).sp
    )
    
    val Author = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
    
    val Description = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
    
    val Params = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.sp
    )
    
    val Downloads = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 按钮文字 - 用于CTA按钮
 * px-8 py-4 font-semibold rounded-xl
 */
object ButtonText {
    val Primary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    )
    
    val Secondary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    )
    
    val Small = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.03.sp
    )
}

/**
 * 页脚文字
 * text-zinc-500 text-sm
 */
object FooterText {
    val Copyright = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
    
    val Info = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
}

// ==================== 兼容旧版本 ====================

val AppTypography = OMasterTypography
val ColorOSTypography = OMasterTypography