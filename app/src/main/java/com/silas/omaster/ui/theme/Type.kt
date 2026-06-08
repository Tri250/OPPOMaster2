package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.silas.omaster.R

/**
 * OMaster 2026 Typography 字体设计
 * 基于 Material3 Typography 规范，符合行业最高水平
 */

// 自定义字体家族（如果需要）
// val OMasterFontFamily = FontFamily(
//     Font(R.font.omaster_regular, FontWeight.Normal),
//     Font(R.font.omaster_medium, FontWeight.Medium),
//     Font(R.font.omaster_bold, FontWeight.Bold),
// )

// 使用系统默认字体（Material3推荐）
val OMasterFontFamily = FontFamily.Default

/**
 * OMaster Typography 配置
 * 完整的Material3字体层级，2026年行业最高标准
 */
val OMasterTypography = Typography(
    // ========== 显示层级 (Display) - 用于大标题 ==========
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

    // ========== 标题层级 (Headline) - 用于页面标题 ==========
    headlineLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
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

    // ========== 标题层级 (Title) - 用于组件标题 ==========
    titleLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ========== 正文层级 (Body) - 用于内容文本 ==========
    bodyLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ========== 标签层级 (Label) - 用于小标签 ==========
    labelLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// 保持向后兼容
val Typography = OMasterTypography

/**
 * 字体扩展样式 - 用于特殊场景
 */
object OMasterTextStyles {
    // 数字显示（用于参数值）
    val numberDisplay = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    // 参数标签（用于参数名称）
    val paramLabel = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )

    // 提示文本（用于拍摄技巧）
    val tipsText = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )

    // 场景名称（用于场景识别）
    val sceneName = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    // 置信度显示
    val confidenceText = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    // 水印文字
    val watermarkText = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )

    // 品牌名称
    val brandName = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
}