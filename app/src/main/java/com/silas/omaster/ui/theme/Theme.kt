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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.silas.omaster.data.local.DarkMode

/**
 * 生成深色主题配色方案
 */
private fun generateDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = PureBlack,
    primaryContainer = primaryColor.copy(alpha = 0.8f),
    onPrimaryContainer = OffWhite,
    secondary = LightGray,
    onSecondary = PureBlack,
    secondaryContainer = DarkGray,
    onSecondaryContainer = OffWhite,
    tertiary = primaryColor.copy(alpha = 0.6f),
    onTertiary = PureBlack,
    tertiaryContainer = MediumGray,
    onTertiaryContainer = OffWhite,
    background = PureBlack,
    onBackground = OffWhite,
    surface = NearBlack,
    onSurface = OffWhite,
    surfaceVariant = DarkGray,
    onSurfaceVariant = LightGray,
    error = ErrorRed,
    onError = OffWhite,
    outline = MediumGray,
    outlineVariant = DarkGray,
    scrim = PureBlack.copy(alpha = 0.8f)
)

/**
 * 生成浅色主题配色方案（备用）
 */
private fun generateLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = OffWhite,
    primaryContainer = primaryColor.copy(alpha = 0.6f),
    onPrimaryContainer = PureBlack,
    secondary = DarkGray,
    onSecondary = OffWhite,
    secondaryContainer = LightGray,
    onSecondaryContainer = PureBlack,
    tertiary = primaryColor.copy(alpha = 0.8f),
    onTertiary = OffWhite,
    tertiaryContainer = OffWhite,
    onTertiaryContainer = PureBlack,
    background = OffWhite,
    onBackground = PureBlack,
    surface = Color.White,
    onSurface = PureBlack,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    error = ErrorRed,
    onError = Color.White,
    outline = MediumGray,
    outlineVariant = LightGray,
    scrim = PureBlack.copy(alpha = 0.5f)
)

/**
 * OMaster 主题配置
 *
 * @param darkMode 深色模式设置，默认为跟随系统
 * @param dynamicColor 是否使用动态颜色，默认为 false
 * @param brandTheme 品牌主题，默认为哈苏
 * @param content 主题内容
 */
@Composable
fun OMasterTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    dynamicColor: Boolean = false, // 禁用动态颜色，使用品牌色
    brandTheme: BrandTheme = BrandTheme.Hasselblad,
    customColorOverride: Int? = null, // P0-1：自定义强调色 ARGB，覆盖品牌 primary
    content: @Composable () -> Unit
) {
    // 根据 darkMode 确定是否使用深色主题
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkMode.SYSTEM -> systemInDarkTheme
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }

    // P0-1：customColorOverride 优先于 brandTheme.primaryColor，使"自定义强调色"真实生效
    val effectivePrimary = if (customColorOverride != null) Color(customColorOverride) else brandTheme.primaryColor

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> generateDarkColorScheme(effectivePrimary)
        else -> generateLightColorScheme(effectivePrimary)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val windowInsetsController = WindowInsetsControllerCompat(window, view)

            // 配置状态栏图标颜色（浅色图标用于深色背景）
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            // 配置导航栏图标颜色
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // P2: 限制字体缩放最大为 1.5x，防止极端布局破坏，同时保持无障碍支持
    val currentDensity = LocalDensity.current
    val fontScale = currentDensity.fontScale
    val constrainedDensity = if (fontScale > 1.5f) {
        androidx.compose.ui.unit.Density(currentDensity.density, 1.5f)
    } else {
        currentDensity
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(LocalDensity provides constrainedDensity) {
                content()
            }
        }
    )
}
