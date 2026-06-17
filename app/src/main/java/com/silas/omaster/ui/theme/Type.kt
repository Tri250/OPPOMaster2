package com.silas.omaster.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * OMaster Typography 字体系统
 * 基于 Material3 设计规范，统一管理应用内所有字体样式
 *
 * 品牌字体配置：
 * - 使用 Noto Sans 作为品牌字体族（将通过 res/font 目录加载）
 * - 如未添加字体文件，将自动回退至系统默认字体
 * - 字体文件放置路径：app/src/main/res/font/
 * - 推荐字体：NotoSans-Regular.ttf, NotoSans-Bold.ttf 等
 */
// 字体资源尚未添加，使用系统默认字体
val OMasterFontFamily = FontFamily.Default

/**
 * 安全获取字体族：如果自定义字体文件不存在，回退至系统默认字体
 */
private val safeFontFamily: FontFamily
    get() = try {
        OMasterFontFamily
    } catch (e: Exception) {
        FontFamily.Default
    }

val Typography = Typography(
    // 大标题 - 用于页面主标题
    titleLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // 中标题 - 用于卡片标题、章节标题
    titleMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    
    // 小标题 - 用于列表项标题
    titleSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // 正文大 - 用于主要内容文本
    bodyLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 正文中 - 用于描述文本、辅助信息
    bodyMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    
    // 正文小 - 用于次要信息、提示文本
    bodySmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // 标签大 - 用于按钮文字、重要标签
    labelLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // 标签中 - 用于普通标签、徽章
    labelMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标签小 - 用于小标签、角标文字
    labelSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标题显示 - 用于特殊大标题（如Logo）
    displayLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.5.sp
    ),
    
    // 标题显示中 - 用于欢迎页标题
    displayMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp
    ),
    
    // 标题显示小 - 用于副标题
    displaySmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标题头 - 用于顶部导航栏标题
    headlineLarge = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头中 - 用于列表头部
    headlineMedium = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头小 - 用于小节头部
    headlineSmall = TextStyle(
        fontFamily = safeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)