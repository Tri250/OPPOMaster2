package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * OMaster Typography 字体系统
 * 基于 Material3 设计规范 + ColorOS 16 字体标准，统一管理应用内所有字体样式
 *
 * ColorOS 16 字体规范：
 * - 大标题：28sp / 行高 36sp
 * - 标题1：22sp / 行高 30sp
 * - 标题2：18sp / 行高 26sp
 * - 标题3：16sp / 行高 24sp
 * - 正文大：16sp / 行高 24sp
 * - 正文：14sp / 行高 22sp
 * - 辅助文字：12sp / 行高 18sp
 * - 标签：11sp / 行高 16sp
 *
 * 品牌字体配置：
 * - 当前使用系统默认字体族（如需自定义字体，请将字体文件放入 app/src/main/res/font/ 后修改此处）
 * - 字体文件推荐：NotoSans-Regular.ttf, NotoSans-Bold.ttf 等
 */
val OMasterFontFamily = FontFamily.Default

/**
 * 安全获取字体族：当前直接使用系统默认字体
 */
private val safeFontFamily: FontFamily = OMasterFontFamily

val Typography = Typography(
    // ========== Display 系列 - ColorOS 16 大标题 ==========
    
    // 大标题 - 用于页面主标题 / ColorOS 16 大标题 28sp
    displayLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题1 - ColorOS 16 标题1 22sp
    displayMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题2 - ColorOS 16 标题2 18sp
    displaySmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    
    // ========== Headline 系列 - ColorOS 16 标题层级 ==========
    
    // 标题头大 - 用于顶部导航栏标题 / ColorOS 16 标题2
    headlineLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头中 - 用于列表头部 / ColorOS 16 标题3
    headlineMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头小 - 用于小节头部
    headlineSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    
    // ========== Title 系列 - ColorOS 16 卡片标题 ==========
    
    // 大标题 - 用于页面主标题
    titleLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    
    // 中标题 - 用于卡片标题、章节标题 / ColorOS 16 标题3 16sp
    titleMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    
    // 小标题 - 用于列表项标题
    titleSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    
    // ========== Body 系列 - ColorOS 16 正文 ==========
    
    // 正文大 - 用于主要内容文本 / ColorOS 16 正文大 16sp
    bodyLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 正文中 - 用于描述文本、辅助信息 / ColorOS 16 正文 14sp/22sp
    bodyMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    
    // 正文小 - 用于次要信息、提示文本 / ColorOS 16 辅助文字 12sp/18sp
    bodySmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    
    // ========== Label 系列 - ColorOS 16 标签 ==========
    
    // 标签大 - 用于按钮文字、重要标签
    labelLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    
    // 标签中 - 用于普通标签、徽章
    labelMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标签小 - 用于小标签、角标文字 / ColorOS 16 标签 11sp/16sp
    labelSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
