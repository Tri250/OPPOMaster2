package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌色系
 * 哈苏橙是品牌的标志性颜色 (#FF6B35 对齐 Web 端)
 */
val HasselbladOrange = Color(0xFFFF6B35)
val HasselbladOrangeDark = Color(0xFFE55A25)
val HasselbladOrangeLight = Color(0xFFFF8A50)
val HasselbladGreen = Color(0xFF4CAF50)

/**
 * 品牌主题色
 */
val ZeissBlue = Color(0xFF005A9C)
val LeicaRed = Color(0xFFCC0000)
val RicohGreen = Color(0xFF00A95C)
val FujifilmGreen = Color(0xFF009B3A)
val CanonRed = Color(0xFFCC0000)
val NikonYellow = Color(0xFFFFC20E)
val SonyOrange = Color(0xFFF15A24)
val PhaseOneGrey = Color(0xFF5A5A5A)

/**
 * 纯黑背景系列（对齐 Web 端）
 * 用于深色模式的主背景
 */
val PureBlack = Color(0xFF0A0A0A)  // 对齐 Web 端 #0A0A0A
val NearBlack = Color(0xFF0A0A0A)
val DarkGray = Color(0xFF1A1A1A)  // 卡片背景 rgba(255,255,255,0.05) ≈ #1A1A1A
val MediumGray = Color(0xFF333333)
val LightGray = Color(0xFF999999)
val OffWhite = Color(0xFFF5F5F5)

/**
 * 功能色
 */
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningYellow = Color(0xFFFFB300)

/**
 * UI 扩展色
 */
val CyanAccent = Color(0xFF00BCD4)  // 对齐 Web 端 #00BCD4
val CardBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val CardBorderHighlight = Color(0xFFFF6600).copy(alpha = 0.3f)
val SurfaceElevated = Color(0xFF222222)
val GradientOrangeStart = Color(0xFFFF6600)
val GradientOrangeEnd = Color(0xFFFF8533)

// ==================== ColorOS 16 色彩系统 ====================

/**
 * ColorOS 16 调色板
 * 基于 ColorOS 16 设计规范，定义系统级色彩
 *
 * 特点：
 * - 柔和渐变色调
 * - 高对比度文字
 * - 液态玻璃效果配色
 */
object ColorOS16Palette {
    // ========== 主色 ==========
    /** 主色 - 柔和蓝紫 */
    val Primary = Color(0xFF4F6EF7)
    /** 主色变体 - 浅 */
    val PrimaryLight = Color(0xFF7B93FA)
    /** 主色变体 - 深 */
    val PrimaryDark = Color(0xFF3A54D4)

    // ========== 液态玻璃色 ==========
    /** 玻璃白 - 液态玻璃基础色 */
    val GlassWhite = Color(0xB3FFFFFF)  // 70% 白色
    /** 玻璃黑 - 深色液态玻璃 */
    val GlassBlack = Color(0xB30A0A0A)  // 70% 黑色
    /** 玻璃边框 - 半透明白色边框 */
    val GlassBorder = Color(0x33FFFFFF)  // 20% 白色
    /** 玻璃高光 - 顶部高光 */
    val GlassHighlight = Color(0x1AFFFFFF) // 10% 白色
    /** 玻璃阴影 */
    val GlassShadow = Color(0x1A000000)   // 10% 黑色

    // ========== 语义色 ==========
    /** 信息蓝 */
    val Info = Color(0xFF2196F3)
    /** 成功绿 */
    val Success = Color(0xFF34C759)
    /** 警告橙 */
    val Warning = Color(0xFFFF9500)
    /** 错误红 */
    val Error = Color(0xFFFF3B30)

    // ========== 中性色 ==========
    /** 文字主色 */
    val TextPrimary = Color(0xFF1A1A1A)
    /** 文字次级 */
    val TextSecondary = Color(0xFF666666)
    /** 文字辅助 */
    val TextTertiary = Color(0xFF999999)
    /** 分割线 */
    val Divider = Color(0xFFE5E5E5)
    /** 背景灰 */
    val BackgroundGray = Color(0xFFF5F5F5)

    // ========== 深色模式液态玻璃 ==========
    /** 深色玻璃 - 深色模式液态玻璃基础色 */
    val GlassDarkSurface = Color(0x991A1A1A)  // 60% 深灰
    /** 深色玻璃边框 */
    val GlassDarkBorder = Color(0x1AFFFFFF)    // 10% 白色
    /** 深色玻璃高光 */
    val GlassDarkHighlight = Color(0x0DFFFFFF) // 5% 白色
}

/**
 * 液态玻璃效果配置
 * 定义液态玻璃的模糊半径、透明度等参数
 * 优化：增强玻璃质感，提升视觉效果
 */
object LiquidGlassConfig {
    /** 模糊半径 (dp) - 增大模糊半径提升玻璃质感 */
    const val BlurRadius = 25f
    /** 背景透明度 - 微调提升通透感 */
    const val BackgroundAlpha = 0.65f
    /** 边框透明度 - 增强边缘高光 */
    const val BorderAlpha = 0.25f
    /** 高光透明度 - 增强顶部高光效果 */
    const val HighlightAlpha = 0.15f
    /** 边框宽度 (dp) */
    const val BorderWidth = 0.5f
    /** 圆角半径 (dp) - 统一为 20dp，符合 ColorOS 16 规范 */
    const val CornerRadius = 20f
    /** 卡片圆角 (dp) - 统一为 16dp */
    const val CardCornerRadius = 16f
    /** 小圆角 (dp) - 标签、按钮等 */
    const val SmallCornerRadius = 12f
    /** 极小圆角 (dp) - 徽标、芯片、小标签等 */
    const val TinyCornerRadius = 8f
}
