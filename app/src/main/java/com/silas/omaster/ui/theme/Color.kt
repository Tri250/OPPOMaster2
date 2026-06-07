package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OMaster Design System - Color Palette
 * 基于哈苏品牌色系的专业摄影应用色彩规范
 */

// ============================================
// 主品牌色 - 哈苏橙 (Hasselblad Orange)
// ============================================
val HasselbladOrange = Color(0xFFFF6600)           // 主品牌色 #FF6600
val HasselbladOrangeDark = Color(0xFFE65100)       // 深橙色 #E65100 - 用于渐变终点
val HasselbladOrangeLight = Color(0xFFFF8533)      // 浅橙色 #FF8533 - 用于渐变起点
val HasselbladOrangeMuted = Color(0xFFFF6600).copy(alpha = 0.7f)  // 弱化橙色

// 品牌渐变
val GradientOrangeStart = HasselbladOrangeLight
val GradientOrangeEnd = HasselbladOrangeDark

// ============================================
// 深色主题背景色 (Zinc-based Dark Theme)
// ============================================
val PureBlack = Color(0xFF000000)                  // 纯黑 - 最底层背景
val Zinc950 = Color(0xFF09090B)                    // 近黑 - 页面主背景
val Zinc900 = Color(0xFF18181B)                    // 深色 - 页面主背景
val Zinc800 = Color(0xFF27272A)                    // 深灰 - 卡片背景
val Zinc700 = Color(0xFF3F3F46)                    // 中深灰 - 边框、分割线
val Zinc600 = Color(0xFF52525B)                    // 中灰 - 禁用状态
val Zinc500 = Color(0xFF71717A)                    // 灰色 - 次要文字
val Zinc400 = Color(0xFFA1A1AA)                    // 浅灰 - 描述文字
val Zinc300 = Color(0xFFD4D4D8)                    // 更浅灰 - 标签文字
val Zinc200 = Color(0xFFE4E4E7)                    // 近白灰
val Zinc100 = Color(0xFFF4F4F5)                    // 极浅灰
val Zinc50 = Color(0xFFFAFAFA)                     // 几乎白

// 语义化别名 - 便于使用
val BackgroundPrimary = Zinc900                    // 主背景
val BackgroundSecondary = Zinc800                  // 卡片背景
val BackgroundElevated = Color(0xFF222222)         // 提升背景
val SurfaceDark = Zinc800                          // 表面深色
val SurfaceElevated = Color(0xFF2D2D2D)            // 提升表面

// ============================================
// 边框与分割线
// ============================================
val BorderDefault = Zinc700                        // 默认边框
val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.05f)   // 浅色边框
val BorderHighlight = HasselbladOrange.copy(alpha = 0.3f) // 高亮边框
val BorderPressed = HasselbladOrange.copy(alpha = 0.5f)   // 按下状态边框
val DividerDefault = Zinc700                       // 分割线

// ============================================
// 文字颜色
// ============================================
val TextPrimary = Color(0xFFFFFFFF)                // 主文字 - 白色
val TextSecondary = Zinc400                        // 次要文字 - zinc-400
val TextTertiary = Zinc500                         // 第三级文字 - zinc-500
val TextMuted = Zinc500                            // 弱化文字
val TextDisabled = Zinc600                         // 禁用文字

// ============================================
// 功能色
// ============================================
val SuccessGreen = Color(0xFF22C55E)               // 成功 - 绿色
val ErrorRed = Color(0xFFEF4444)                   // 错误 - 红色
val WarningYellow = Color(0xFFF59E0B)              // 警告 - 黄色
val InfoBlue = Color(0xFF3B82F6)                   // 信息 - 蓝色

// ============================================
// 其他品牌色（用于设备标识等）
// ============================================
val ZeissBlue = Color(0xFF005A9C)
val LeicaRed = Color(0xFFCC0000)
val RicohGreen = Color(0xFF00A95C)
val FujifilmGreen = Color(0xFF009B3A)
val CanonRed = Color(0xFFCC0000)
val NikonYellow = Color(0xFFFFC20E)
val SonyOrange = Color(0xFFF15A24)
val PhaseOneGrey = Color(0xFF5A5A5A)

// ============================================
// 装饰色
// ============================================
val GlowOrange = HasselbladOrange.copy(alpha = 0.2f)      // 橙色光晕
val OverlayDark = Color(0xFF000000).copy(alpha = 0.6f)    // 深色遮罩
val OverlayLight = Color(0xFFFFFFFF).copy(alpha = 0.1f)   // 浅色遮罩
val ScrimDefault = Color(0xFF000000).copy(alpha = 0.8f)   // 默认遮罩

// ============================================
// 阴影色
// ============================================
val ShadowOrange = HasselbladOrange.copy(alpha = 0.25f)   // 橙色阴影
val ShadowOrangeHover = HasselbladOrange.copy(alpha = 0.40f) // 悬停橙色阴影
val ShadowDefault = Color(0xFF000000).copy(alpha = 0.3f)  // 默认阴影
