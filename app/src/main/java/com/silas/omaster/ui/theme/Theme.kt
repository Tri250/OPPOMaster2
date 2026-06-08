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

/**
 * OMaster 2026 Theme 主题设计
 * Material3标准 + Dynamic Color动态颜色 + 深色模式完善支持
 */

// ========== OMaster品牌颜色定义 ==========

/**
 * 品牌主色 - OPPO哈苏橙
 */
object OMasterColors {
    // 主色（哈苏橙）
    val Primary = Color(0xFFFF6B35)
    val PrimaryLight = Color(0xFFFF8C42)
    val PrimaryDark = Color(0xFFE65100)

    // 次色（哈苏绿）
    val Secondary = Color(0xFF4CAF50)
    val SecondaryLight = Color(0xFF81C784)
    val SecondaryDark = Color(0xFF388E3C)

    // 强调色（哈苏金）
    val Tertiary = Color(0xFFFFC107)
    val TertiaryLight = Color(0xFFFFCA28)
    val TertiaryDark = Color(0xFFFFA000)

    // 背景（深色主题）
    val BackgroundDark = Color(0xFF0A0A0A)
    val SurfaceDark = Color(0xFF1A1A1A)
    val SurfaceVariantDark = Color(0xFF252525)

    // 背景（浅色主题）
    val BackgroundLight = Color(0xFFFFFBF5)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF5F5F5)

    // 文字颜色
    val OnPrimary = Color.White
    val OnSecondary = Color.White
    val OnTertiary = Color(0xFF1A1A1A)
    val OnBackgroundDark = Color.White
    val OnBackgroundLight = Color(0xFF1A1A1A)
    val OnSurfaceDark = Color.White
    val OnSurfaceLight = Color(0xFF1A1A1A)

    // 错误色
    val Error = Color(0xFFBA1A1A)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnError = Color.White
    val OnErrorContainer = Color(0xFF410002)

    // 成功色
    val Success = Color(0xFF4CAF50)
    val SuccessContainer = Color(0xFFE8F5E9)
    val OnSuccess = Color.White
    val OnSuccessContainer = Color(0xFF1B5E20)

    // 警告色
    val Warning = Color(0xFFFF9800)
    val WarningContainer = Color(0xFFFFF3E0)
    val OnWarning = Color(0xFF1A1A1A)
    val OnWarningContainer = Color(0xFFE65100)

    // 信息色
    val Info = Color(0xFF2196F3)
    val InfoContainer = Color(0xFFE3F2FD)
    val OnInfo = Color.White
    val OnInfoContainer = Color(0xFF0D47A1)

    // 场景识别颜色
    val ScenePortrait = Color(0xFFE91E63)
    val SceneLandscape = Color(0xFF4CAF50)
    val SceneNight = Color(0xFF3F51B5)
    val SceneFood = Color(0xFFFF9800)
    val SceneSunset = Color(0xFFFF5722)
    val SceneStreet = Color(0xFFFF5722)
    val SceneFlower = Color(0xFFE91E63)
    val SceneArchitecture = Color(0xFF607D8B)
    val ScenePet = Color(0xFF9C27B0)
    val SceneBeach = Color(0xFF00BCD4)
    val SceneForest = Color(0xFF388E3C)
    val SceneCafe = Color(0xFF795548)
}

// ========== 深色主题配色方案 ==========

private val DarkColorScheme = darkColorScheme(
    primary = OMasterColors.Primary,
    onPrimary = OMasterColors.OnPrimary,
    primaryContainer = OMasterColors.PrimaryDark,
    onPrimaryContainer = OMasterColors.OnPrimary,

    secondary = OMasterColors.Secondary,
    onSecondary = OMasterColors.OnSecondary,
    secondaryContainer = OMasterColors.SecondaryDark,
    onSecondaryContainer = OMasterColors.OnSecondary,

    tertiary = OMasterColors.Tertiary,
    onTertiary = OMasterColors.OnTertiary,
    tertiaryContainer = OMasterColors.TertiaryDark,
    onTertiaryContainer = OMasterColors.OnTertiary,

    background = OMasterColors.BackgroundDark,
    onBackground = OMasterColors.OnBackgroundDark,

    surface = OMasterColors.SurfaceDark,
    onSurface = OMasterColors.OnSurfaceDark,
    surfaceVariant = OMasterColors.SurfaceVariantDark,
    onSurfaceVariant = OMasterColors.OnSurfaceDark,

    error = OMasterColors.Error,
    errorContainer = OMasterColors.ErrorContainer,
    onError = OMasterColors.OnError,
    onErrorContainer = OMasterColors.OnErrorContainer,

    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF444444),

    inverseSurface = OMasterColors.SurfaceLight,
    inverseOnSurface = OMasterColors.OnSurfaceLight,
    inversePrimary = OMasterColors.PrimaryLight
)

// ========== 浅色主题配色方案 ==========

private val LightColorScheme = lightColorScheme(
    primary = OMasterColors.Primary,
    onPrimary = OMasterColors.OnPrimary,
    primaryContainer = OMasterColors.PrimaryLight,
    onPrimaryContainer = OMasterColors.OnPrimary,

    secondary = OMasterColors.Secondary,
    onSecondary = OMasterColors.OnSecondary,
    secondaryContainer = OMasterColors.SecondaryLight,
    onSecondaryContainer = OMasterColors.OnSecondary,

    tertiary = OMasterColors.Tertiary,
    onTertiary = OMasterColors.OnTertiary,
    tertiaryContainer = OMasterColors.TertiaryLight,
    onTertiaryContainer = OMasterColors.OnTertiary,

    background = OMasterColors.BackgroundLight,
    onBackground = OMasterColors.OnBackgroundLight,

    surface = OMasterColors.SurfaceLight,
    onSurface = OMasterColors.OnSurfaceLight,
    surfaceVariant = OMasterColors.SurfaceVariantLight,
    onSurfaceVariant = OMasterColors.OnSurfaceLight,

    error = OMasterColors.Error,
    errorContainer = OMasterColors.ErrorContainer,
    onError = OMasterColors.OnError,
    onErrorContainer = OMasterColors.OnErrorContainer,

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFBDBDBD),

    inverseSurface = OMasterColors.SurfaceDark,
    inverseOnSurface = OMasterColors.OnSurfaceDark,
    inversePrimary = OMasterColors.PrimaryDark
)

// ========== OMaster主题 ==========

/**
 * OMaster主题 - 2026年行业最高标准
 *
 * @param darkTheme 是否使用深色主题（默认跟随系统）
 * @param dynamicColor 是否使用动态颜色（Android 12+支持）
 * @param content 主题内容
 */
@Composable
fun OMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 默认启用动态颜色
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // 颜色方案选择逻辑
    val colorScheme = when {
        // Android 12+ 动态颜色支持
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // 深色主题
        darkTheme -> DarkColorScheme
        // 浅色主题
        else -> LightColorScheme
    }

    // 状态栏颜色适配
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // 应用Material3主题
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OMasterTypography,
        content = content
    )
}

// ========== 便捷访问扩展 ==========

/**
 * 获取当前主题的场景颜色
 */
@Composable
fun getSceneColor(sceneType: String): Color {
    return when (sceneType) {
        "portrait" -> OMasterColors.ScenePortrait
        "landscape" -> OMasterColors.SceneLandscape
        "night" -> OMasterColors.SceneNight
        "food" -> OMasterColors.SceneFood
        "sunset" -> OMasterColors.SceneSunset
        "street" -> OMasterColors.SceneStreet
        "flower" -> OMasterColors.SceneFlower
        "architecture" -> OMasterColors.SceneArchitecture
        "pet" -> OMasterColors.ScenePet
        "beach" -> OMasterColors.SceneBeach
        "forest" -> OMasterColors.SceneForest
        "cafe" -> OMasterColors.SceneCafe
        else -> OMasterColors.Primary
    }
}

/**
 * 获取当前主题状态颜色
 */
@Composable
fun getStatusColor(status: String): Color {
    return when (status) {
        "success" -> OMasterColors.Success
        "warning" -> OMasterColors.Warning
        "error" -> OMasterColors.Error
        "info" -> OMasterColors.Info
        else -> OMasterColors.Primary
    }
}

// 保持向后兼容
@Composable
fun OMasterAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    OMasterTheme(darkTheme = darkTheme, content = content)
}