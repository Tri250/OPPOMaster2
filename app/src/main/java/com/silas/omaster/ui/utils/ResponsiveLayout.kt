package com.silas.omaster.ui.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass

/**
 * ============================================
 * 多设备分辨率适配系统
 * 支持: PC、平板、手机、折叠屏
 * ============================================
 */

/**
 * 设备类型枚举
 */
enum class DeviceType {
    /** 手机 - 小屏幕 (< 600dp) */
    MOBILE,
    
    /** 大屏手机/小平板 (600dp - 840dp) */
    MOBILE_LARGE,
    
    /** 平板 - 中等屏幕 (840dp - 1200dp) */
    TABLET,
    
    /** 大平板 (1200dp - 1600dp) */
    TABLET_LARGE,
    
    /** PC/折叠屏展开 - 大屏幕 (> 1600dp) */
    DESKTOP,
    
    /** 折叠屏折叠状态 */
    FOLDABLE_CLOSED,
    
    /** 折叠屏展开状态 */
    FOLDABLE_OPEN
}

/**
 * 屏幕尺寸信息
 */
data class ScreenSizeInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val deviceType: DeviceType,
    val isLandscape: Boolean,
    val columns: Int,
    val cardWidth: Dp,
    val spacing: Dp,
    val padding: Dp
)

/**
 * 获取当前屏幕尺寸信息
 */
@Composable
fun rememberScreenSizeInfo(): ScreenSizeInfo {
    val configuration = LocalConfiguration.current
    
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isLandscape = widthDp > heightDp
    
    val deviceType = when {
        widthDp < 360.dp -> DeviceType.MOBILE
        widthDp < 600.dp -> DeviceType.MOBILE
        widthDp < 840.dp -> DeviceType.MOBILE_LARGE
        widthDp < 1200.dp -> DeviceType.TABLET
        widthDp < 1600.dp -> DeviceType.TABLET_LARGE
        else -> DeviceType.DESKTOP
    }
    
    val columns = when (deviceType) {
        DeviceType.MOBILE -> if (isLandscape) 3 else 2
        DeviceType.MOBILE_LARGE -> if (isLandscape) 4 else 2
        DeviceType.TABLET -> if (isLandscape) 5 else 3
        DeviceType.TABLET_LARGE -> if (isLandscape) 6 else 4
        DeviceType.DESKTOP -> if (isLandscape) 8 else 6
        else -> 2
    }
    
    val cardWidth = when (deviceType) {
        DeviceType.MOBILE -> (widthDp - 32.dp - 8.dp) / 2
        DeviceType.MOBILE_LARGE -> (widthDp - 32.dp - 8.dp) / 2
        DeviceType.TABLET -> (widthDp - 48.dp - 16.dp) / 3
        DeviceType.TABLET_LARGE -> (widthDp - 48.dp - 24.dp) / 4
        DeviceType.DESKTOP -> 280.dp
        else -> (widthDp - 32.dp) / 2
    }
    
    val spacing = when (deviceType) {
        DeviceType.MOBILE -> 8.dp
        DeviceType.MOBILE_LARGE -> 12.dp
        DeviceType.TABLET -> 16.dp
        DeviceType.TABLET_LARGE -> 20.dp
        DeviceType.DESKTOP -> 24.dp
        else -> 8.dp
    }
    
    val padding = when (deviceType) {
        DeviceType.MOBILE -> 16.dp
        DeviceType.MOBILE_LARGE -> 20.dp
        DeviceType.TABLET -> 24.dp
        DeviceType.TABLET_LARGE -> 32.dp
        DeviceType.DESKTOP -> 48.dp
        else -> 16.dp
    }
    
    return remember(widthDp, heightDp) {
        ScreenSizeInfo(
            widthDp = widthDp,
            heightDp = heightDp,
            deviceType = deviceType,
            isLandscape = isLandscape,
            columns = columns,
            cardWidth = cardWidth,
            spacing = spacing,
            padding = padding
        )
    }
}

/**
 * ============================================
 * 响应式尺寸参数
 * ============================================
 */

object ResponsiveDimensions {
    /** 最小卡片宽度 */
    val MinCardWidth = 140.dp
    
    /** 最大卡片宽度 */
    val MaxCardWidth = 320.dp
    
    /** 手机端卡片高度 */
    val MobileCardHeight = 180.dp
    
    /** 平板端卡片高度 */
    val TabletCardHeight = 220.dp
    
    /** PC端卡片高度 */
    val DesktopCardHeight = 260.dp
    
    /** 手机端图标大小 */
    val MobileIconSize = 20.dp
    
    /** 平板端图标大小 */
    val TabletIconSize = 24.dp
    
    /** PC端图标大小 */
    val DesktopIconSize = 28.dp
    
    /** 手机端标题字号 */
    val MobileTitleSize = 16.dp
    
    /** 平板端标题字号 */
    val TabletTitleSize = 18.dp
    
    /** PC端标题字号 */
    val DesktopTitleSize = 20.dp
    
    /** 手机端内边距 */
    val MobilePadding = 16.dp
    
    /** 平板端内边距 */
    val TabletPadding = 24.dp
    
    /** PC端内边距 */
    val DesktopPadding = 32.dp
    
    /** 手机端间距 */
    val MobileSpacing = 8.dp
    
    /** 平板端间距 */
    val TabletSpacing = 16.dp
    
    /** PC端间距 */
    val DesktopSpacing = 24.dp
    
    /** 手机端圆角 */
    val MobileCornerRadius = 16.dp
    
    /** 平板端圆角 */
    val TabletCornerRadius = 20.dp
    
    /** PC端圆角 */
    val DesktopCornerRadius = 24.dp
}

/**
 * 获取响应式尺寸
 */
@Composable
fun getResponsiveDimensions(): ResponsiveDimensions {
    val screenSizeInfo = rememberScreenSizeInfo()
    
    return when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE, DeviceType.MOBILE_LARGE -> ResponsiveDimensions
        DeviceType.TABLET, DeviceType.TABLET_LARGE -> ResponsiveDimensions
        DeviceType.DESKTOP -> ResponsiveDimensions
        else -> ResponsiveDimensions
    }
}

/**
 * ============================================
 * 响应式布局组件
 * ============================================
 */

/**
 * 响应式网格布局
 * 根据屏幕尺寸自动调整列数
 */
@Composable
fun ResponsiveGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val screenSizeInfo = rememberScreenSizeInfo()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(screenSizeInfo.columns),
        modifier = modifier,
        contentPadding = PaddingValues(screenSizeInfo.padding),
        horizontalArrangement = Arrangement.spacedBy(screenSizeInfo.spacing),
        verticalArrangement = Arrangement.spacedBy(screenSizeInfo.spacing)
    ) {
        content()
    }
}

/**
 * 响应式卡片
 * 根据屏幕尺寸调整尺寸和样式
 */
@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val screenSizeInfo = rememberScreenSizeInfo()
    val cornerRadius = when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 16.dp
        DeviceType.TABLET -> 20.dp
        DeviceType.DESKTOP -> 24.dp
        else -> 16.dp
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .widthIn(min = ResponsiveDimensions.MinCardWidth, max = ResponsiveDimensions.MaxCardWidth),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A1A)
        )
    ) {
        Column {
            content()
        }
    }
}

/**
 * 响应式内边距
 */
@Composable
fun responsivePadding(): Dp {
    val screenSizeInfo = rememberScreenSizeInfo()
    return screenSizeInfo.padding
}

/**
 * 响应式间距
 */
@Composable
fun responsiveSpacing(): Dp {
    val screenSizeInfo = rememberScreenSizeInfo()
    return screenSizeInfo.spacing
}

/**
 * 响应式图标大小
 */
@Composable
fun responsiveIconSize(): Dp {
    val screenSizeInfo = rememberScreenSizeInfo()
    return when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 20.dp
        DeviceType.TABLET -> 24.dp
        DeviceType.DESKTOP -> 28.dp
        else -> 20.dp
    }
}

/**
 * 响应式圆角
 */
@Composable
fun responsiveCornerRadius(): Dp {
    val screenSizeInfo = rememberScreenSizeInfo()
    return when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> 16.dp
        DeviceType.TABLET -> 20.dp
        DeviceType.DESKTOP -> 24.dp
        else -> 16.dp
    }
}

/**
 * ============================================
 * 折叠屏适配
 * ============================================
 */

/**
 * 折叠屏状态检测
 */
@Composable
fun rememberFoldableState(): FoldableState {
    // 这里需要使用 WindowManager API 来检测折叠状态
    // 目前使用简化版本
    val screenSizeInfo = rememberScreenSizeInfo()
    
    return remember(screenSizeInfo) {
        when {
            screenSizeInfo.widthDp > 1600.dp && screenSizeInfo.heightDp < 900.dp -> 
                FoldableState.OPEN
            screenSizeInfo.widthDp < 600.dp && screenSizeInfo.heightDp > 800.dp -> 
                FoldableState.CLOSED
            else -> FoldableState.NORMAL
        }
    }
}

enum class FoldableState {
    CLOSED,  // 折叠状态
    OPEN,    // 展开状态
    NORMAL   // 普通设备
}

/**
 * 折叠屏自适应布局
 */
@Composable
fun FoldableAdaptiveLayout(
    modifier: Modifier = Modifier,
    closedContent: @Composable () -> Unit,
    openContent: @Composable () -> Unit,
    normalContent: @Composable () -> Unit
) {
    val foldableState = rememberFoldableState()
    
    when (foldableState) {
        FoldableState.CLOSED -> closedContent()
        FoldableState.OPEN -> openContent()
        FoldableState.NORMAL -> normalContent()
    }
}

/**
 * ============================================
 * 横竖屏适配
 * ============================================
 */

/**
 * 横竖屏自适应布局
 */
@Composable
fun OrientationAdaptiveLayout(
    modifier: Modifier = Modifier,
    portraitContent: @Composable () -> Unit,
    landscapeContent: @Composable () -> Unit
) {
    val screenSizeInfo = rememberScreenSizeInfo()
    
    if (screenSizeInfo.isLandscape) {
        landscapeContent()
    } else {
        portraitContent()
    }
}

/**
 * ============================================
 * 字体大小适配
 * ============================================
 */

@Composable
fun responsiveTextStyle(): androidx.compose.ui.text.TextStyle {
    val screenSizeInfo = rememberScreenSizeInfo()
    
    return when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> MaterialTheme.typography.bodyMedium
        DeviceType.TABLET -> MaterialTheme.typography.bodyLarge
        DeviceType.DESKTOP -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.bodyMedium
    }
}

@Composable
fun responsiveTitleStyle(): androidx.compose.ui.text.TextStyle {
    val screenSizeInfo = rememberScreenSizeInfo()
    
    return when (screenSizeInfo.deviceType) {
        DeviceType.MOBILE -> MaterialTheme.typography.titleMedium
        DeviceType.TABLET -> MaterialTheme.typography.titleLarge
        DeviceType.DESKTOP -> MaterialTheme.typography.headlineMedium
        else -> MaterialTheme.typography.titleMedium
    }
}