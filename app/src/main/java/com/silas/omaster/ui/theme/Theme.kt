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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 品牌主题枚举
 * 支持多种专业摄影品牌主题
 */
enum class BrandTheme(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color? = null
) {
    Hasselblad("Hasselblad", HasselbladOrange, HasselbladOrangeLight),
    Zeiss("Zeiss", ZeissBlue),
    Leica("Leica", LeicaRed),
    Ricoh("Ricoh", RicohGreen),
    Fujifilm("Fujifilm", FujifilmGreen),
    Canon("Canon", CanonRed),
    Nikon("Nikon", NikonYellow),
    Sony("Sony", SonyOrange),
    PhaseOne("Phase One", PhaseOneGrey)
}

/**
 * 生成深色主题配色方案
 * 基于 Zinc 色系的深色主题
 */
private fun generateDarkColorScheme(primaryColor: Color) = darkColorScheme(
    // 主色
    primary = primaryColor,
    onPrimary = PureBlack,
    primaryContainer = primaryColor.copy(alpha = 0.15f),
    onPrimaryContainer = TextPrimary,

    // 次色
    secondary = Zinc400,
    onSecondary = PureBlack,
    secondaryContainer = Zinc800,
    onSecondaryContainer = TextPrimary,

    // 第三色
    tertiary = primaryColor.copy(alpha = 0.7f),
    onTertiary = PureBlack,
    tertiaryContainer = Zinc700,
    onTertiaryContainer = TextPrimary,

    // 背景
    background = Zinc900,
    onBackground = TextPrimary,

    // 表面
    surface = Zinc800,
    onSurface = TextPrimary,
    surfaceVariant = Zinc700,
    onSurfaceVariant = Zinc400,

    // 错误
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorRed.copy(alpha = 0.15f),
    onErrorContainer = ErrorRed,

    // 轮廓
    outline = Zinc600,
    outlineVariant = Zinc700,

    // 遮罩
    scrim = PureBlack.copy(alpha = 0.8f),

    // 表面容器 - 用于卡片等
    surfaceContainer = Zinc800,
    surfaceContainerHigh = Zinc700,
    surfaceContainerLow = Zinc800.copy(alpha = 0.8f),
    surfaceContainerLowest = Zinc900,

    // 反色
    inverseSurface = Zinc200,
    inverseOnSurface = Zinc900,
    inversePrimary = primaryColor
)

/**
 * 生成浅色主题配色方案（备用）
 */
private fun generateLightColorScheme(primaryColor: Color) = lightColorScheme(
    // 主色
    primary = primaryColor,
    onPrimary = Color.White,
    primaryContainer = primaryColor.copy(alpha = 0.1f),
    onPrimaryContainer = primaryColor,

    // 次色
    secondary = Zinc600,
    onSecondary = Color.White,
    secondaryContainer = Zinc100,
    onSecondaryContainer = Zinc900,

    // 第三色
    tertiary = primaryColor.copy(alpha = 0.8f),
    onTertiary = Color.White,
    tertiaryContainer = Zinc100,
    onTertiaryContainer = Zinc900,

    // 背景
    background = Zinc50,
    onBackground = Zinc900,

    // 表面
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc600,

    // 错误
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,

    // 轮廓
    outline = Zinc300,
    outlineVariant = Zinc200,

    // 遮罩
    scrim = PureBlack.copy(alpha = 0.5f),

    // 表面容器
    surfaceContainer = Zinc100,
    surfaceContainerHigh = Zinc200,
    surfaceContainerLow = Zinc50,
    surfaceContainerLowest = Color.White,

    // 反色
    inverseSurface = Zinc800,
    inverseOnSurface = Zinc50,
    inversePrimary = primaryColor
)

/**
 * OMaster 主题配置
 *
 * 设计规范:
 * - 强制深色模式，符合专业摄影应用调性
 * - 使用 Zinc 色系作为基础
 * - 支持品牌主题切换
 * - 禁用动态颜色，保持品牌一致性
 *
 * @param darkTheme 是否使用深色主题，默认为 true
 * @param dynamicColor 是否使用动态颜色，默认为 false
 * @param brandTheme 品牌主题，默认为哈苏
 * @param content 主题内容
 */
@Composable
fun OMasterTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    brandTheme: BrandTheme = BrandTheme.Hasselblad,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        // Android 12+ 动态颜色（可选）
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // 深色主题（默认）
        darkTheme -> generateDarkColorScheme(brandTheme.primaryColor)
        // 浅色主题（备用）
        else -> generateLightColorScheme(brandTheme.primaryColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect

            // 配置窗口
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 状态栏和导航栏配置
            val windowInsetsController = WindowInsetsControllerCompat(window, view)

            // 深色主题使用浅色状态栏图标
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme

            // 设置状态栏颜色
            window.statusBarColor = if (darkTheme) Zinc900.toArgb() else Color.White.toArgb()
            window.navigationBarColor = if (darkTheme) Zinc900.toArgb() else Color.White.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * 获取当前是否为深色主题
 */
@Composable
fun isOMasterDarkTheme(): Boolean = true // 强制返回 true，因为 OMaster 强制深色主题

/**
 * 获取当前品牌主题的主色
 */
@Composable
fun getBrandPrimaryColor(brandTheme: BrandTheme = BrandTheme.Hasselblad): Color {
    return brandTheme.primaryColor
}

/**
 * 获取品牌渐变色
 */
fun getBrandGradientColors(brandTheme: BrandTheme = BrandTheme.Hasselblad): Pair<Color, Color> {
    return when (brandTheme) {
        BrandTheme.Hasselblad -> Pair(HasselbladOrangeLight, HasselbladOrangeDark)
        else -> Pair(brandTheme.primaryColor.copy(alpha = 0.8f), brandTheme.primaryColor)
    }
}
