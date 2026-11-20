package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 应用字体排版体系
 *
 * 【v1.7.7 Reno17 精致化】中文字体无衬线家族下：
 * - 行高统一 ≥1.45× 字号（CJK 字形饱满，避免拥挤/切顶）
 * - 大标题字距微收（-0.2~-0.5sp）显精致，正文保持 0（中文不需要额外字距）
 * - 字重阶梯明确：Light(300)/Normal(400)/Medium(500)/SemiBold(600)
 * - 与字号阶梯 11/12/13/14/16/18/20/22/24/28/34 对齐，避免"半号"字号造成参差
 */

/** 应用专属字体家族（走系统默认无衬线，OEM 机自动匹配厂商字体，最稳） */
val AppFontFamily = FontFamily.SansSerif

/**
 * 字号阶梯（屏幕内可直接引用，保证全站字号克制统一）
 */
object AppTypeScale {
    val Caption = 11.sp
    val Label = 12.sp
    val BodySmall = 13.sp
    val Body = 14.sp
    val BodyLarge = 16.sp
    val TitleSmall = 18.sp
    val Title = 20.sp
    val TitleLarge = 22.sp
    val Headline = 24.sp
    val HeadlineLarge = 28.sp
    val Display = 34.sp
}

/** 排版预设：标题/正文/辅助，含 CJK 调优行高 */
object AppTextStyles {
    val DisplayTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = AppTypeScale.Display,
        lineHeight = 42.sp,
        letterSpacing = (-0.2).sp
    )
    val HeadlineTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTypeScale.HeadlineLarge,
        lineHeight = 36.sp,
        letterSpacing = (-0.1).sp
    )
    val SectionTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTypeScale.TitleLarge,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    )
    val CardTitle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = AppTypeScale.TitleSmall,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )
    val BodyStrong = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = AppTypeScale.Body,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    val BodyRegular = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = AppTypeScale.Body,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    )
    val BodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = AppTypeScale.BodySmall,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    )
    val Caption = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = AppTypeScale.Label,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )
    val Overline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = AppTypeScale.Caption,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp
    )
}

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    )
)
