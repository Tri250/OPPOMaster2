package com.silas.omaster.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
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
 * 对齐 OPPO Find X9 AMOLED 纯黑 + 哈苏橙品牌色
 * 使用 ColorOS16Palette 液态玻璃色值增强毛玻璃效果
 */
private fun generateDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = PureBlack,
    primaryContainer = primaryColor.copy(alpha = 0.8f),
    onPrimaryContainer = OffWhite,
    secondary = ColorOS16Palette.GlassDarkBorder,  // 使用液态玻璃色替换硬编码 LightGray
    onSecondary = PureBlack,
    secondaryContainer = ColorOS16Palette.GlassDarkSurface,  // 液态玻璃深色表面
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
    onSurfaceVariant = ColorOS16Palette.TextSecondary,  // 较亮的次要文字，提升对比度
    error = ErrorRed,
    onError = OffWhite,
    outline = ColorOS16Palette.GlassBorder,  // 液态玻璃边框色
    outlineVariant = DarkGray,
    scrim = PureBlack.copy(alpha = 0.8f)
)

/**
 * 生成浅色主题配色方案（备用）
 * 使用 ColorOS16Palette 液态玻璃色值
 */
private fun generateLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = OffWhite,
    primaryContainer = primaryColor.copy(alpha = 0.6f),
    onPrimaryContainer = PureBlack,
    secondary = ColorOS16Palette.TextSecondary,
    onSecondary = OffWhite,
    secondaryContainer = ColorOS16Palette.BackgroundGray,
    onSecondaryContainer = PureBlack,
    tertiary = primaryColor.copy(alpha = 0.8f),
    onTertiary = OffWhite,
    tertiaryContainer = OffWhite,
    onTertiaryContainer = PureBlack,
    background = OffWhite,
    onBackground = PureBlack,
    surface = Color.White,
    onSurface = PureBlack,
    surfaceVariant = ColorOS16Palette.BackgroundGray,
    onSurfaceVariant = ColorOS16Palette.TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = ColorOS16Palette.Divider,
    outlineVariant = ColorOS16Palette.BackgroundGray,
    scrim = PureBlack.copy(alpha = 0.5f)
)

/**
 * 统一触觉反馈工具 — 提供三级反馈
 * 在任意 Composable 中调用：
 *   hapticLight()   — 轻反馈（导航点击、开关切换）
 *   hapticMedium()  — 中反馈（按钮点击、选择确认）
 *   hapticHeavy()   — 重反馈（长按、删除、重要操作）
 */
object HapticLevels {
    // 这些常量用于引用，实际调用需通过 LocalHapticFeedback
    const val LIGHT = 0
    const val MEDIUM = 1
    const val HEAVY = 2
}

/**
 * OMaster 主题配置
 *
 * @param darkMode 深色模式设置，默认为跟随系统
 * @param dynamicColor 是否使用 Android 12+ 动态颜色，默认 false（使用品牌色）
 * @param brandTheme 品牌主题，默认为哈苏
 * @param onBackPressed 全局返回键回调，用于 Predictive Back 手势（Android 14+）
 * @param content 主题内容
 */
@Composable
fun OMasterTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    dynamicColor: Boolean = false,
    brandTheme: BrandTheme = BrandTheme.Hasselblad,
    onBackPressed: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // 根据 darkMode 确定是否使用深色主题
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkMode.SYSTEM -> systemInDarkTheme
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> generateDarkColorScheme(brandTheme.primaryColor)
        else -> generateLightColorScheme(brandTheme.primaryColor)
    }

    // Android 14+ Predictive Back 手势 — 统一拦截返回键
    onBackPressed?.let { handler ->
        BackHandler(enabled = true, onBack = handler)
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

            // Android 10+ (API 29) 导航栏透明，实现真正的边到边
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    // 限制字体缩放最大为 1.5x，防止极端布局破坏，同时保持无障碍支持
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