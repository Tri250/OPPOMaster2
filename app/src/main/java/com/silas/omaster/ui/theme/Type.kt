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
 * 当前使用系统默认字体族，确保在没有自定义字体资源时也能正常编译和运行。
 * 如需接入品牌字体，请将字体文件放入 app/src/main/res/font/ 后，
 * 在此处改为 FontFamily(Font(R.font.xxx, FontWeight.Normal), ...)。
 */
val OMasterFontFamily: FontFamily = FontFamily.Default

val Typography = Typography(
    // 大标题 - 用于页面主标题
    titleLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    
    // 中标题 - 用于卡片标题、章节标题
    titleMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    
    // 小标题 - 用于列表项标题
    titleSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // 正文大 - 用于主要内容文本
    bodyLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 正文中 - 用于描述文本、辅助信息
    bodyMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    
    // 正文小 - 用于次要信息、提示文本
    bodySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // 标签大 - 用于按钮文字、重要标签
    labelLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // 标签中 - 用于普通标签、徽章
    labelMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标签小 - 用于小标签、角标文字
    labelSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标题显示 - 用于特殊大标题（如Logo）
    displayLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.5.sp
    ),
    
    // 标题显示中 - 用于欢迎页标题
    displayMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp
    ),
    
    // 标题显示小 - 用于副标题
    displaySmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    
    // 标题头 - 用于顶部导航栏标题
    headlineLarge = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头中 - 用于列表头部
    headlineMedium = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    
    // 标题头小 - 用于小节头部
    headlineSmall = TextStyle(
        fontFamily = OMasterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)