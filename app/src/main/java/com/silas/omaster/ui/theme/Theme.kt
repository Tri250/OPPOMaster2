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

// ==================== ColorOS 16 专业摄影深色配色方案 ====================
val ColorOSDarkColorScheme = darkColorScheme(
    primary = HasselbladOrange,
    onPrimary = ColorOSBlack,
    primaryContainer = HasselbladOrangeDark,
    onPrimaryContainer = ColorOSTextPrimary,
    secondary = DeepOceanBlue,
    onSecondary = Color.White,
    secondaryContainer = DeepOceanBlueDark,
    onSecondaryContainer = ColorOSTextPrimary,
    tertiary = OppoGold,
    onTertiary = ColorOSBlack,
    tertiaryContainer = OppoGoldDark,
    onTertiaryContainer = ColorOSTextPrimary,
    background = ColorOSBlack,
    onBackground = ColorOSTextPrimary,
    surface = ColorOSCard,
    onSurface = ColorOSTextPrimary,
    surfaceVariant = ColorOSBlackElevated,
    onSurfaceVariant = ColorOSTextSecondary,
    outline = ColorOSBorder,
    outlineVariant = ColorOSBorderLight,
    error = ErrorPro,
    onError = Color.White,
    errorContainer = ErrorPro.copy(alpha = 0.15f),
    onErrorContainer = ColorOSTextPrimary
)

// ==================== ColorOS 16 专业摄影浅色配色方案 ====================
val ColorOSLightColorScheme = lightColorScheme(
    primary = HasselbladOrange,
    onPrimary = Color.White,
    primaryContainer = HasselbladOrangeLight,
    onPrimaryContainer = ColorOSLightTextPrimary,
    secondary = DeepOceanBlue,
    onSecondary = Color.White,
    secondaryContainer = DeepOceanBlueLight,
    onSecondaryContainer = ColorOSLightTextPrimary,
    tertiary = OppoGold,
    onTertiary = Color.White,
    tertiaryContainer = OppoGoldLight,
    onTertiaryContainer = ColorOSLightTextPrimary,
    background = ColorOSLightBackground,
    onBackground = ColorOSLightTextPrimary,
    surface = ColorOSLightSurface,
    onSurface = ColorOSLightTextPrimary,
    surfaceVariant = ColorOSLightCard,
    onSurfaceVariant = ColorOSLightTextSecondary,
    outline = ColorOSLightBorder,
    outlineVariant = ColorOSLightBorderLight,
    error = ErrorPro,
    onError = Color.White,
    errorContainer = ErrorPro.copy(alpha = 0.15f),
    onErrorContainer = ColorOSLightTextPrimary
)

@Composable
fun OMasterTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    brandTheme: BrandTheme = BrandTheme.Hasselblad,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ColorOSDarkColorScheme
        else -> ColorOSLightColorScheme
    }
    
    // 根据品牌主题调整主色调
    val brandedColorScheme = remember(brandTheme, colorScheme) {
        colorScheme.copy(
            primary = brandTheme.primaryColor,
            onPrimary = if (darkTheme) ColorOSBlack else Color.White,
            primaryContainer = brandTheme.primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = if (darkTheme) ColorOSTextPrimary else ColorOSLightTextPrimary
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = brandedColorScheme.background.toArgb()
            window.navigationBarColor = brandedColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = brandedColorScheme,
        typography = ColorOSTypography,
        shapes = ColorOSShapes,
        content = content
    )
}
