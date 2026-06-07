package com.silas.omaster.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =====================================================
 * ColorOS 16 专业摄影设计规范 - 字体系统
 * =====================================================
 * 字体标准：OPPO Sans Pro / Roboto
 * 设计理念：专业、清晰、高端、易读
 * 适用场景：OPPO Find 系列高端摄影用户
 */

// ==================== 形状系统 ====================
/**
 * ColorOS 16 圆角规范
 * 基于Aquatic Design的柔和曲线
 */
val ColorOS16Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),   // 标签、小按钮
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),        // 输入框、小卡片
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),      // 卡片、按钮
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),       // 大卡片、对话框
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)   // 底部面板
)

// ==================== 字体系统 ====================
/**
 * ColorOS 16 字体层级
 * 基于Material Design 3 + ColorOS优化
 */
val ColorOS16Typography = Typography(
    // ==================== Display - 展示级 ====================
    // 用于：启动页、Hero区域、超大标题
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.2).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.15).sp
    ),

    // ==================== Headline - 标题级 ====================
    // 用于：页面标题、分区标题
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.05).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ==================== Title - 卡片标题级 ====================
    // 用于：卡片标题、列表项标题
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01.sp
    ),

    // ==================== Body - 正文级 ====================
    // 用于：正文内容、描述文字
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.03.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    ),

    // ==================== Label - 标签级 ====================
    // 用于：按钮、标签、参数
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.05.sp
    )
)

// ==================== 专业摄影专用字体 ====================
/**
 * 相机参数显示专用字体
 * 等宽数字，便于对齐和快速阅读
 */
object CameraTypography {
    /**
     * ISO数值 - 大号加粗
     */
    val ISOValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )

    /**
     * 快门速度 - 中等大小
     */
    val ShutterValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )

    /**
     * 光圈值
     */
    val ApertureValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    /**
     * 曝光补偿
     */
    val EVValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    /**
     * 参数标签
     */
    val ParamLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.08.sp
    )

    /**
     * 参数单位
     */
    val ParamUnit = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 数据统计专用字体
 */
object StatsTypography {
    /**
     * 大数字 - 下载量、评分等
     */
    val LargeNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    )

    /**
     * 中等数字
     */
    val MediumNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    )

    /**
     * 小数字
     */
    val SmallNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    /**
     * 统计标签
     */
    val StatsLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 徽章标签专用字体
 */
object BadgeTypography {
    /**
     * HNCS认证徽章
     */
    val HNCSBadge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.05.sp
    )

    /**
     * 评分徽章
     */
    val RatingBadge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )

    /**
     * 标签文字
     */
    val TagLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.02.sp
    )

    /**
     * NEW标签
     */
    val NewBadge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.05.sp
    )
}

/**
 * 按钮文字专用字体
 */
object ButtonTypography {
    /**
     * 主按钮
     */
    val PrimaryButton = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    )

    /**
     * 次按钮
     */
    val SecondaryButton = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.sp
    )

    /**
     * 小按钮
     */
    val SmallButton = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )
}

/**
 * 导航文字专用字体
 */
object NavigationTypography {
    /**
     * 底部导航选中
     */
    val BottomNavSelected = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )

    /**
     * 底部导航未选中
     */
    val BottomNavUnselected = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.sp
    )

    /**
     * 顶部标题
     */
    val TopBarTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
}

// ==================== 兼容旧版本 ====================
val ColorOSShapes = ColorOS16Shapes
val ColorOSTypography = ColorOS16Typography
