package com.silas.omaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌色系
 * 哈苏橙是品牌的标志性颜色
 */
val HasselbladOrange = Color(0xFFEC7223)
val HasselbladOrangeDark = Color(0xFFD15F1A)
val HasselbladOrangeLight = Color(0xFFF08A4A)

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
 * 【v1.7.7】OPPO Reno17 系列风格色系（整体默认主题）
 *
 * Reno17 的设计语言：星光蓝主色 + 极光蓝紫渐变 + 高透玻璃背板。
 * 主题色取 Reno17「星光蓝」，渐变 partner 取「极光紫」，
 * 玻璃面取蓝调半透明（在深色底上呈现 Reno17 玻璃后盖的通透感）。
 */
val Reno17Blue = Color(0xFF3D6DF0)          // 星光蓝（主色）
val Reno17BlueDeep = Color(0xFF2A4FC4)      // 星光蓝·深（按压/渐变起点）
val Reno17Violet = Color(0xFF8A5CF6)        // 极光紫（渐变终点）
val Reno17Aqua = Color(0xFF39C6E8)          // 极光青（高光/点缀）
val Reno17GradientStart = Reno17Blue
val Reno17GradientMid = Color(0xFF6A6BF2)
val Reno17GradientEnd = Reno17Violet

/**
 * 纯黑背景系列
 * 用于深色模式的主背景
 */
val PureBlack = Color(0xFF000000)
val NearBlack = Color(0xFF0A0A0A)
val DarkGray = Color(0xFF2D2D2D)  // 从 #1A1A1A 调亮，阳光下更清晰
val MediumGray = Color(0xFF333333)
val LightGray = Color(0xFF999999)
val OffWhite = Color(0xFFEEEEEE)

/**
 * 文字颜色优化 - 提升深色模式对比度
 */
val PrimaryText = Color(0xFFFFFFFF)           // 主文字 - 100% 不透明度
val SecondaryText = Color(0xFFB0B0B0)         // 次要文字 - 70% 不透明度
val TertiaryText = Color(0xFF808080)          // 第三优先级文字 - 50% 不透明度

/**
 * 功能色
 */
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningYellow = Color(0xFFFFB300)

/**
 * UI 扩展色
 * 优化对比度以提升深色模式下的可见性
 */
val CardBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val CardBorderHighlight = Color(0xFFEC7223).copy(alpha = 0.5f)
val SurfaceElevated = Color(0xFF222222)
val GradientOrangeStart = Color(0xFFEC7223)
val GradientOrangeEnd = Color(0xFFF08A4A)

/**
 * 玻璃质感 (Glassmorphism) 配色 - 统一系统
 */
object GlassColors {
    // 基础层 - 深色半透明背景
    val Base = Color(0xFF1A1A1A).copy(alpha = 0.75f)

    // 表面层 - 带微妙蓝紫色调的玻璃表面
    val Surface = Color(0xFF2A2A3A).copy(alpha = 0.60f)

    // 边框系统 - 双层边框结构
    val BorderOuter = Color(0xFFFFFFFF).copy(alpha = 0.18f)      // 外边框 - 更明显
    val BorderInner = Color(0xFFFFFFFF).copy(alpha = 0.06f)      // 内边框 -  subtle
    val BorderHighlight = Color(0xFFEC7223).copy(alpha = 0.7f)   // 高亮状态

    // 高光系统 - 玻璃的关键特征
    val HighlightTop = Color(0xFFFFFFFF).copy(alpha = 0.30f)     // 顶部高光
    val HighlightGlow = Color(0xFFEC7223).copy(alpha = 0.20f)    // 橙色光晕

    // 兼容旧代码的别名
    val Background = Base
    val Border = BorderOuter
}

// 保留旧名称兼容已有代码 (标记为废弃，逐步迁移)
@Deprecated("使用 GlassColors.Base 代替", ReplaceWith("GlassColors.Base"))
val GlassBackground = GlassColors.Base

@Deprecated("使用 GlassColors.BorderOuter 代替", ReplaceWith("GlassColors.BorderOuter"))
val GlassBorder = GlassColors.BorderOuter

@Deprecated("使用 GlassColors.BorderHighlight 代替", ReplaceWith("GlassColors.BorderHighlight"))
val GlassBorderHighlight = GlassColors.BorderHighlight

@Deprecated("使用 GlassColors.Surface 代替", ReplaceWith("GlassColors.Surface"))
val GlassSurface = GlassColors.Surface

/**
 * 主题感知颜色函数
 * 根据当前主题返回对应的颜色值,用于替代硬编码颜色
 */

/**
 * 【v1.7.7 Reno17 玻璃化】主题感知的背景色
 * 深色模式: PureBlack（Reno17 深空底，极光渐变绘制其上）
 * 浅色模式: OffWhite
 */
@Composable
fun themedBackground(): Color =
    if (MaterialTheme.colorScheme.background == PureBlack) PureBlack else OffWhite

/**
 * 【v1.7.7 Reno17 玻璃化】主题感知的卡片背景色
 * 深色模式: Reno17 蓝调玻璃面（半透明，透出底部极光渐变）
 * 浅色模式: 白色磨砂（半透明）
 */
@Composable
fun themedCardBackground(): Color =
    if (MaterialTheme.colorScheme.surfaceVariant == DarkGray) {
        Color(0xFF141A2E).copy(alpha = 0.72f)   // Reno17 深空玻璃
    } else {
        Color.White.copy(alpha = 0.82f)          // 浅色磨砂
    }

/**
 * 获取主题感知的主文字颜色
 * 深色模式: PrimaryText (白色)
 * 浅色模式: PureBlack (黑色)
 */
@Composable
fun themedTextPrimary(): Color =
    if (MaterialTheme.colorScheme.onBackground == OffWhite) PrimaryText else PureBlack

/**
 * 获取主题感知的次要文字颜色
 * 深色模式: SecondaryText
 * 浅色模式: MediumGray
 */
@Composable
fun themedTextSecondary(): Color =
    if (MaterialTheme.colorScheme.onSurfaceVariant == LightGray) SecondaryText else MediumGray

/**
 * 【v1.7.7 Reno17 玻璃化】主题感知的边框颜色
 * 深色模式: Reno17 星光蓝玻璃棱边（蓝调描边）
 * 浅色模式: 深蓝灰磨砂描边
 */
@Composable
fun themedBorderLight(): Color =
    if (MaterialTheme.colorScheme.outline == MediumGray) {
        Reno17Blue.copy(alpha = 0.22f)
    } else {
        Color(0xFF1A1A2E).copy(alpha = 0.10f)
    }

/**
 * 主题感知的自定义颜色
 * 根据当前主题返回对应的深色/浅色颜色值
 */
@Composable
fun themedColor(darkColor: Color, lightColor: Color): Color =
    if (MaterialTheme.colorScheme.background == PureBlack) darkColor else lightColor
