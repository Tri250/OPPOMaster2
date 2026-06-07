package com.silas.omaster.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * =====================================================
 * OMaster 设计系统 v3.0 - 完整规范
 * =====================================================
 * 
 * 参考 OMaster Web 设计规范
 * https://github.com/Tri250/OPPOMaster
 */

object OMasterDesignSystem {
    
    // ==================== 色彩系统 ====================
    
    object Colors {
        // 主品牌色 - 哈苏橙
        val Primary = HasselbladOrange           // #E65100
        val PrimaryLight = HasselbladOrangeLight // #FF7A1A
        val PrimaryDark = HasselbladOrangeDark   // #BF4000
        
        // 辅助色 - OPPO绿
        val Secondary = OPPOGreen                // #00C853
        val SecondaryLight = OPPOGreenLight      // #00E676
        val SecondaryDark = OPPOGreenDark        // #00A344
        
        // 背景色
        val Background = BackgroundPrimary       // #18181B
        val BackgroundElevated = BackgroundTertiary // #27272A
        val Surface = BackgroundTertiary         // #27272A
        
        // 边框
        val Border = BorderPrimary               // #3F3F46
        val BorderLight = BorderSecondary        // #52525B
        
        // 文字
        val TextPrimary = com.silas.omaster.ui.theme.TextPrimary       // #FFFFFF
        val TextSecondary = com.silas.omaster.ui.theme.TextSecondary   // #D4D4D8
        val TextTertiary = com.silas.omaster.ui.theme.TextTertiary     // #A1A1AA
        val TextMuted = TextQuaternary           // #71717A
    }
    
    // ==================== 圆角系统 ====================
    
    object Radius {
        val Small = 6.dp      // rounded-md - 标签/徽章
        val Medium = 12.dp    // rounded-xl - 按钮、图标容器
        val Large = 16.dp     // rounded-2xl - 卡片
        val XLarge = 24.dp    // rounded-3xl - 大卡片
        val Full = 100.dp     // rounded-full - 装饰光晕
    }
    
    val Shapes = androidx.compose.material3.Shapes(
        small = RoundedCornerShape(Radius.Small),
        medium = RoundedCornerShape(Radius.Medium),
        large = RoundedCornerShape(Radius.Large)
    )
    
    // ==================== 间距系统 ====================
    
    object Spacing {
        val XSmall = 4.dp
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 16.dp
        val XLarge = 20.dp
        val XXLarge = 24.dp
        val XXXLarge = 32.dp
        
        // 页面内边距
        val PageHorizontal = 20.dp
        val PageVertical = 24.dp
        
        // 卡片内边距
        val CardPadding = 16.dp
        
        // 网格间距
        val GridGap = 12.dp
    }
    
    // ==================== 阴影系统 ====================
    
    object Elevation {
        val None = 0.dp
        val Small = 4.dp
        val Medium = 8.dp
        val Large = 16.dp
        
        // 按钮阴影
        val ButtonShadow = 8.dp
        val ButtonShadowPressed = 4.dp
        
        // 卡片阴影
        val CardShadow = 0.dp
        val CardShadowHovered = 8.dp
    }
    
    // ==================== 字体大小 ====================
    
    object Typography {
        // Hero主标题
        val Hero = 48.sp       // text-5xl
        val HeroLarge = 72.sp  // text-7xl
        
        // 区块标题
        val H1 = 36.sp         // text-3xl
        val H2 = 32.sp         // text-4xl
        
        // 卡片标题
        val H3 = 20.sp         // text-xl
        
        // 正文
        val BodyLarge = 18.sp  // text-lg
        val Body = 16.sp       // text-base
        val BodySmall = 14.sp  // text-sm
        
        // 标签
        val Caption = 12.sp    // text-xs
    }
    
    // ==================== 动画时长 ====================
    
    object Animation {
        const val Fast = 100        // 按钮点击
        const val Normal = 200      // 边框、颜色变化
        const val Slow = 300        // 卡片悬停
        const val ImageScale = 500  // 图片缩放
        const val PageEnter = 800   // 页面进入
        
        // 延迟间隔 (stagger)
        const val StaggerDelay = 100
    }
    
    // ==================== 组件规范 ====================
    
    object Components {
        
        // 按钮
        object Button {
            val Height = 48.dp
            val PaddingHorizontal = 24.dp
            val PaddingVertical = 12.dp
            val Radius = Radius.Medium  // 12dp
            
            // 主按钮渐变
            val PrimaryGradient = listOf(
                Colors.PrimaryLight,
                Colors.Primary,
                Colors.PrimaryDark
            )
        }
        
        // 卡片
        object Card {
            val Radius = Radius.Large   // 16dp
            val Padding = Spacing.CardPadding
            val BorderWidth = 1.dp
        }
        
        // 标签
        object Tag {
            val Radius = Radius.Small   // 6dp
            val PaddingHorizontal = 10.dp
            val PaddingVertical = 5.dp
        }
        
        // 图标容器
        object IconBox {
            val Size = 56.dp
            val Radius = Radius.Medium  // 12dp
        }
        
        // 底部导航
        object BottomNav {
            val Height = 64.dp
            val Radius = 32.dp
            val PaddingHorizontal = 12.dp
            val ItemPadding = 20.dp
        }
    }
    
    // ==================== 网格系统 ====================
    
    object Grid {
        val Columns = 2           // 移动端默认2列
        val Gap = Spacing.GridGap // 12dp
    }
}
