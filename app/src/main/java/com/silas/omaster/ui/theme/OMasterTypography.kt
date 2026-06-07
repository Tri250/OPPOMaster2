package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * =====================================================
 * OMaster 设计系统 v2.0 - 字体排版
 * =====================================================
 * 字体规范：
 * - 主字体：Inter + 系统字体栈
 * - 备用字体：-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto
 * - 中文推荐：思源黑体 (Noto Sans SC)
 */

// 字体家族
val OMasterFontFamily = FontFamily.Default

// ==================== Material3 Typography ====================

val OMasterTypography = Typography(
    // Display - 超大标题
    displayLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline - 页面标题
    headlineLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title - 卡片标题
    titleLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01.sp
    ),

    // Body - 正文
    bodyLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.sp
    ),

    // Label - 标签/按钮
    labelLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.03.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    )
)

// ==================== 自定义字体样式 ====================

/**
 * Hero标题 - 用于首页大标题
 */
object HeroTypography {
    val Title = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp
    )
    
    val Subtitle = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 卡片文字
 */
object CardTypography {
    val Title = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    )
    
    val Description = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
    
    val Tag = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.sp
    )
}

/**
 * 按钮文字
 */
object ButtonTypography {
    val Primary = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    )
    
    val Secondary = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 徽章/标签
 */
object BadgeTypography {
    val HNCS = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    )
    
    val Rating = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 统计数字
 */
object StatsTypography {
    val Value = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    )
    
    val Label = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    )
}
