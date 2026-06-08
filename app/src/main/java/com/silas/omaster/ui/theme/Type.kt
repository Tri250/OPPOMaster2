package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ============================================
 * 字体大小层级
 * ============================================
 */
val FontSizeXs = 11.sp      // 标签
val FontSizeSm = 13.sp      // 辅助文字
val FontSizeBase = 15.sp    // 正文
val FontSizeLg = 17.sp      // 副标题
val FontSizeXl = 20.sp      // 卡片标题
val FontSize2xl = 24.sp     // 页面标题
val FontSize3xl = 28.sp     // 大标题
val FontSize4xl = 32.sp     // 展示标题

/**
 * ============================================
 * 字体粗细
 * ============================================
 */
val FontWeightNormal = FontWeight(400)
val FontWeightMedium = FontWeight(500)
val FontWeightSemibold = FontWeight(600)
val FontWeightBold = FontWeight(700)

/**
 * ============================================
 * 行高
 * ============================================
 */
val LineHeightTight = 1.25f
val LineHeightNormal = 1.5f
val LineHeightRelaxed = 1.75f

/**
 * ============================================
 * 字间距
 * ============================================
 */
val LetterSpacingTight = (-0.02).sp
val LetterSpacingNormal = 0.sp
val LetterSpacingWide = 0.02.sp

/**
 * ============================================
 * Typography 样式
 * ============================================
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = FontSize4xl,
        fontWeight = FontWeightBold,
        lineHeight = (FontSize4xl.value * LineHeightTight).sp,
        letterSpacing = LetterSpacingTight
    ),
    titleLarge = TextStyle(
        fontSize = FontSize2xl,
        fontWeight = FontWeightBold,
        lineHeight = (FontSize2xl.value * LineHeightTight).sp
    ),
    titleMedium = TextStyle(
        fontSize = FontSizeXl,
        fontWeight = FontWeightSemibold,
        lineHeight = (FontSizeXl.value * LineHeightTight).sp
    ),
    subtitle = TextStyle(
        fontSize = FontSizeLg,
        fontWeight = FontWeightMedium,
        lineHeight = (FontSizeLg.value * LineHeightNormal).sp
    ),
    body = TextStyle(
        fontSize = FontSizeBase,
        fontWeight = FontWeightNormal,
        lineHeight = (FontSizeBase.value * LineHeightRelaxed).sp
    ),
    caption = TextStyle(
        fontSize = FontSizeSm,
        fontWeight = FontWeightNormal,
        lineHeight = (FontSizeSm.value * LineHeightNormal).sp,
        color = TextTertiary
    ),
    label = TextStyle(
        fontSize = FontSizeXs,
        fontWeight = FontWeightMedium,
        lineHeight = (FontSizeXs.value * LineHeightNormal).sp
    )
)