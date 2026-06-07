package com.silas.omaster.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * =====================================================
 * ColorOS 16 专业摄影主题系统
 * =====================================================
 * 设计标准：Aquatic Design 水感设计
 * 目标用户：OPPO Find 系列高端摄影用户
 * 视觉定位：专业、高端、精致、有质感
 */

// ==================== 深色配色方案 ====================
val ColorOS16DarkColorScheme = darkColorScheme(
    // 品牌色
    primary = HasselbladOrange,
    onPrimary = DeepSpaceBlack,
    primaryContainer = HasselbladOrangeDark,
    onPrimaryContainer = TextPrimary,

    // 辅助色
    secondary = OPPOGreen,
    onSecondary = DeepSpaceBlack,
    secondaryContainer = OPPOGreenDark,
    onSecondaryContainer = TextPrimary,

    // 第三色
    tertiary = Color(0xFF8B5CF6),
    onTertiary = DeepSpaceBlack,
    tertiaryContainer = Color(0xFF7C3AED),
    onTertiaryContainer = TextPrimary,

    // 背景色
    background = DeepSpaceBlack,
    onBackground = TextPrimary,

    // 表面色
    surface = CardBackgroundPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundSecondary,
    onSurfaceVariant = TextSecondary,

    // 轮廓色
    outline = BorderPrimary,
    outlineVariant = BorderSecondary,

    // 错误色
    error = ErrorColor,
    onError = TextPrimary,
    errorContainer = ErrorColor.copy(alpha = 0.15f),
    onErrorContainer = TextPrimary,

    // 反色
    inverseSurface = TextPrimary,
    inverseOnSurface = DeepSpaceBlack,
    inversePrimary = HasselbladOrangeLight,

    // 表面色调
    surfaceTint = HasselbladOrange.copy(alpha = 0.1f)
)

// ==================== 浅色配色方案 ====================
val ColorOS16LightColorScheme = lightColorScheme(
    // 品牌色
    primary = HasselbladOrange,
    onPrimary = Color.White,
    primaryContainer = HasselbladOrangeLight,
    onPrimaryContainer = DeepSpaceBlack,

    // 辅助色
    secondary = OPPOGreen,
    onSecondary = Color.White,
    secondaryContainer = OPPOGreenLight,
    onSecondaryContainer = DeepSpaceBlack,

    // 第三色
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA78BFA),
    onTertiaryContainer = DeepSpaceBlack,

    // 背景色
    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF18181B),

    // 表面色
    surface = Color.White,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),

    // 轮廓色
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFF4F4F5),

    // 错误色
    error = ErrorColor,
    onError = Color.White,
    errorContainer = ErrorColor.copy(alpha = 0.1f),
    onErrorContainer = DeepSpaceBlack,

    // 反色
    inverseSurface = Color(0xFF18181B),
    inverseOnSurface = Color.White,
    inversePrimary = HasselbladOrangeDark,

    // 表面色调
    surfaceTint = HasselbladOrange.copy(alpha = 0.05f)
)

/**
 * ColorOS 16 主题入口
 *
 * @param darkTheme 是否使用深色主题
 * @param dynamicColor 是否使用动态颜色（Android 12+）
 * @param theme 品牌主题选择
 * @param content 内容 composable
 */
@Composable
fun OMasterTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    theme: ColorOSTheme = ColorOSTheme.Hasselblad,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // 深色主题
        darkTheme -> ColorOS16DarkColorScheme
        // 浅色主题
        else -> ColorOS16LightColorScheme
    }

    // 根据品牌主题调整主色调
    val brandedColorScheme = remember(theme, colorScheme) {
        colorScheme.copy(
            primary = theme.primaryColor,
            onPrimary = if (darkTheme) DeepSpaceBlack else Color.White,
            primaryContainer = theme.secondaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = if (darkTheme) TextPrimary else DeepSpaceBlack,
            surfaceTint = theme.primaryColor.copy(alpha = 0.1f)
        )
    }

    // 设置状态栏和导航栏
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 状态栏颜色
            window.statusBarColor = brandedColorScheme.background.toArgb()
            // 导航栏颜色
            window.navigationBarColor = brandedColorScheme.background.toArgb()

            // 状态栏图标颜色
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // 导航栏图标颜色
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = brandedColorScheme,
        typography = ColorOS16Typography,
        shapes = ColorOS16Shapes,
        content = content
    )
}

/**
 * 获取当前主题颜色
 */
@Composable
fun rememberThemeColors(): ThemeColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(colorScheme) {
        ThemeColors(
            primary = colorScheme.primary,
            onPrimary = colorScheme.onPrimary,
            background = colorScheme.background,
            onBackground = colorScheme.onBackground,
            surface = colorScheme.surface,
            onSurface = colorScheme.onSurface,
            surfaceVariant = colorScheme.surfaceVariant,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            outline = colorScheme.outline
        )
    }
}

/**
 * 主题颜色数据类
 */
data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color
)

// ==================== 兼容旧版本 ====================
val ColorOSDarkColorScheme = ColorOS16DarkColorScheme
val ColorOSLightColorScheme = ColorOS16LightColorScheme

enum class BrandTheme(
    val displayName: String,
    val primaryColor: Color
) {
    Hasselblad("哈苏", HasselbladOrange),
    OPPO("OPPO", OPPOGreen),
    OnePlus("一加", Color(0xFFF05A28)),
    Realme("realme", Color(0xFFFFD900))
}
