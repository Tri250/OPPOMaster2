package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * OMaster Design System - Typography
 * 基于设计规范的字体系统
 *
 * 字体层级:
 * - Display: 超大展示文字 (Hero标题)
 * - Headline: 标题文字 (页面标题、区块标题)
 * - Title: 卡片/组件标题
 * - Body: 正文内容
 * - Label: 标签/按钮文字
 */

// ============================================
// 字体家族 - 使用系统字体栈
// ============================================
val OMasterFontFamily = FontFamily.Default

// ============================================
// 字体大小规范
// ============================================
private val DisplayLargeSize = 56.sp      // Hero主标题 - 移动端
private val DisplayMediumSize = 40.sp     // Hero主标题 - 小屏
private val HeadlineLargeSize = 32.sp     // 页面主标题
private val HeadlineMediumSize = 28.sp    // 区块标题
private val HeadlineSmallSize = 24.sp     // 小标题
private val TitleLargeSize = 20.sp        // 卡片标题
private val TitleMediumSize = 18.sp       // 次级标题
private val TitleSmallSize = 16.sp        // 小标题
private val BodyLargeSize = 16.sp         // 大正文
private val BodyMediumSize = 14.sp        // 标准正文
private val BodySmallSize = 12.sp         // 小正文
private val LabelLargeSize = 14.sp        // 大标签
private val LabelMediumSize = 12.sp       // 标准标签
private val LabelSmallSize = 11.sp        // 小标签

// ============================================
// 行高规范
// ============================================
private val DisplayLineHeight = 64.sp
private val HeadlineLineHeight = 40.sp
private val TitleLineHeight = 28.sp
private val BodyLineHeight = 24.sp
private val LabelLineHeight = 16.sp

// ============================================
// 字间距规范
// ============================================
private val TightLetterSpacing = (-0.5).sp
private val NormalLetterSpacing = 0.sp
private val WideLetterSpacing = 0.5.sp
private val ExtraWideLetterSpacing = 2.sp  // 用于装饰性文字

// ============================================
// Typography 配置
// ============================================
val Typography = Typography(
    // ========================================
    // Display - 展示文字 (Hero区域)
    // ========================================
    displayLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = DisplayLargeSize,
        lineHeight = DisplayLineHeight,
        letterSpacing = TightLetterSpacing,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = DisplayMediumSize,
        lineHeight = 48.sp,
        letterSpacing = TightLetterSpacing,
        color = TextPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),

    // ========================================
    // Headline - 标题文字
    // ========================================
    headlineLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = HeadlineLargeSize,
        lineHeight = HeadlineLineHeight,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = HeadlineMediumSize,
        lineHeight = 36.sp,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = HeadlineSmallSize,
        lineHeight = 32.sp,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),

    // ========================================
    // Title - 卡片/组件标题
    // ========================================
    titleLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = TitleLargeSize,
        lineHeight = TitleLineHeight,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TitleMediumSize,
        lineHeight = 26.sp,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = TitleSmallSize,
        lineHeight = 24.sp,
        letterSpacing = NormalLetterSpacing,
        color = TextPrimary
    ),

    // ========================================
    // Body - 正文内容
    // ========================================
    bodyLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BodyLargeSize,
        lineHeight = BodyLineHeight,
        letterSpacing = WideLetterSpacing,
        color = TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BodyMediumSize,
        lineHeight = 20.sp,
        letterSpacing = WideLetterSpacing,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = BodySmallSize,
        lineHeight = 18.sp,
        letterSpacing = WideLetterSpacing,
        color = TextTertiary
    ),

    // ========================================
    // Label - 标签/按钮文字
    // ========================================
    labelLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LabelLargeSize,
        lineHeight = LabelLineHeight,
        letterSpacing = WideLetterSpacing,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LabelMediumSize,
        lineHeight = 14.sp,
        letterSpacing = WideLetterSpacing,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = LabelSmallSize,
        lineHeight = 14.sp,
        letterSpacing = WideLetterSpacing,
        color = TextTertiary
    )
)

// ============================================
// 扩展样式 - 用于特定场景
// ============================================

/**
 * Hero标题样式 - 带渐变效果
 */
val HeroTitleStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 48.sp,
    lineHeight = 56.sp,
    letterSpacing = TightLetterSpacing
)

/**
 * 卡片标题样式
 */
val CardTitleStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = NormalLetterSpacing,
    color = TextPrimary
)

/**
 * 标签文字样式
 */
val TagTextStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.3.sp,
    color = Zinc300
)

/**
 * 按钮文字样式
 */
val ButtonTextStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.5.sp,
    color = TextPrimary
)

/**
 * 参数标签样式
 */
val ParameterLabelStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = WideLetterSpacing,
    color = TextTertiary
)

/**
 * 参数值样式
 */
val ParameterValueStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = NormalLetterSpacing,
    color = TextPrimary
)

/**
 * 徽章文字样式
 */
val BadgeTextStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.5.sp,
    color = TextPrimary
)

/**
 * 装饰性大写文字样式
 */
val DecorativeUppercaseStyle = TextStyle(
    fontFamily = OMasterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = ExtraWideLetterSpacing,
    color = TextSecondary
)
