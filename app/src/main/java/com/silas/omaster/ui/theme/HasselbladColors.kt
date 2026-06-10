package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌色板
 * 基于 OPPO × 哈苏品牌体系
 */
object HasselbladColors {
    // ========== 主品牌色 ==========
    val HasselbladOrange = Color(0xFFFF6B35)      // 主品牌色 - 选中态、按钮、强调元素
    val HasselbladOrangeLight = Color(0xFFFF8C42) // 浅哈苏橙
    
    // ========== 金色点缀 ==========
    val Gold = Color(0xFFD4AF37)                   // 哈苏大师系列分割线、徽章边框
    val GoldLight = Color(0xFFE5C76B)              // 浅金色
    val GoldDark = Color(0xFFB8962D)               // 深金色
    
    // ========== 文字色 ==========
    val TextPrimary = Color(0xFFFFFFFF)            // 默认水印文字色
    val TextDark = Color(0xFF1A1A1A)               // 高调画面自动切换
    val TextSecondary = Color(0xB3FFFFFF)          // 70% 透明度白色
    val TextTertiary = Color(0x66FFFFFF)           // 40% 透明度白色
    
    // ========== 背景色 ==========
    val BackgroundSemiTransparent = Color(0x66000000)  // 40% 透明度黑色 - 底部信息栏背景
    val BackgroundLight = Color(0x33000000)            // 20% 透明度黑色
    
    // ========== HNCS 认证色 ==========
    val HncsGreen = Color(0xFF4CAF50)              // HNCS 认证标识点缀
    val HncsGreenLight = Color(0xFF66BB6A)
    
    // ========== 分割线 ==========
    val DividerGold = Color(0x4DD4AF37)            // 金色分割线 30% 透明度
    val DividerWhite = Color(0x33FFFFFF)           // 白色分割线 20% 透明度
    
    // ========== 渐变 ==========
    val OrangeGradient = listOf(HasselbladOrange, HasselbladOrangeLight)
    val GoldGradient = listOf(GoldDark, Gold, GoldLight)
    
    // ========== 辅助方法 ==========
    /**
     * 根据背景亮度选择文字颜色
     * @param backgroundLuminance 背景亮度 0-1
     */
    fun getTextColorForBackground(backgroundLuminance: Float): Color {
        return if (backgroundLuminance > 0.5f) TextDark else TextPrimary
    }
    
    /**
     * 获取半透明背景色
     * @param alpha 透明度 0-1
     */
    fun getSemiTransparentBackground(alpha: Float): Color {
        return Color(0xFF000000).copy(alpha = alpha.coerceIn(0f, 1f))
    }
}

/**
 * 哈苏水印样式预设
 */
object HasselbladWatermarkStyles {
    // 大师印记样式
    val MasterStyle = WatermarkStyleConfig(
        brandFontSize = 14f,
        brandLetterSpacing = 2f,
        brandColor = HasselbladColors.Gold,
        paramsFontSize = 10f,
        paramsFontFamily = "monospace",
        dividerColor = HasselbladColors.DividerGold,
        backgroundColor = HasselbladColors.BackgroundSemiTransparent,
        showBottomBar = true,
        bottomBarHeight = 0.08f,  // 8% 画面高度
    )
    
    // HNCS 认证样式
    val HncsStyle = WatermarkStyleConfig(
        brandFontSize = 16f,
        brandLetterSpacing = 4f,
        brandColor = HasselbladColors.Gold,
        badgeBorderColor = HasselbladColors.Gold,
        badgeBackgroundColor = HasselbladColors.BackgroundSemiTransparent,
        showBadge = true,
    )
    
    // XPAN 宽幅样式
    val XpanStyle = WatermarkStyleConfig(
        brandFontSize = 18f,
        brandLetterSpacing = 6f,
        brandColor = HasselbladColors.TextPrimary,
        paramsFontSize = 12f,
        showAspectRatio = true,
        aspectRatio = "65:24",
    )
}

/**
 * 水印样式配置
 */
data class WatermarkStyleConfig(
    val brandFontSize: Float = 14f,
    val brandLetterSpacing: Float = 0f,
    val brandColor: Color = HasselbladColors.TextPrimary,
    val brandFontWeight: Int = 700,
    
    val paramsFontSize: Float = 10f,
    val paramsFontFamily: String = "default",
    val paramsColor: Color = HasselbladColors.TextSecondary,
    
    val dividerColor: Color = HasselbladColors.DividerWhite,
    val dividerWidth: Float = 1f,
    
    val backgroundColor: Color = HasselbladColors.BackgroundSemiTransparent,
    val backgroundAlpha: Float = 0.4f,
    
    val badgeBorderColor: Color = HasselbladColors.Gold,
    val badgeBackgroundColor: Color = HasselbladColors.BackgroundSemiTransparent,
    
    val showBottomBar: Boolean = false,
    val bottomBarHeight: Float = 0.08f,
    
    val showBadge: Boolean = false,
    val showDivider: Boolean = true,
    val showAspectRatio: Boolean = false,
    val aspectRatio: String = "3:2",
)
