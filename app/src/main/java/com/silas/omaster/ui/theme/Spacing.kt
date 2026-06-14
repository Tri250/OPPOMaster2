package com.silas.omaster.ui.theme

import androidx.compose.ui.unit.dp

/**
 * OMaster Spacing 间距系统
 * 统一管理应用内所有间距值，确保设计一致性
 * 基于 8dp 网格系统
 */
object Spacing {
    // ========== 基础间距 ==========

    /** 极小间距 - 用于紧密元素 */
    val ExtraSmall = 2.dp

    /** 小间距 - 用于元素之间小间距 */
    val Small = 4.dp

    /** 中小间距 - 用于图标与文字间距 */
    val SmallMedium = 8.dp

    /** 中间距 - 用于卡片内边距、元素间距 */
    val Medium = 12.dp

    /** 标准间距 - 用于页面边距、卡片间距 */
    val Standard = 16.dp

    /** 大间距 - 用于章节之间间距 */
    val Large = 24.dp

    /** 极大间距 - 用于页面底部留白 */
    val ExtraLarge = 32.dp

    /** 超大间距 - 用于列表底部留白 */
    val Huge = 100.dp

    // ========== 特殊间距 ==========

    /** 页面水平边距 */
    val PageHorizontal = 16.dp

    /** 页面垂直边距 */
    val PageVertical = 16.dp

    /** 卡片内边距 */
    val CardPadding = 16.dp

    /** 卡片间距 */
    val CardSpacing = 12.dp

    /** 列表项间距 */
    val ListItemSpacing = 12.dp

    /** 按钮内边距 */
    val ButtonPaddingHorizontal = 12.dp
    val ButtonPaddingVertical = 6.dp

    /** 图标与文字间距 */
    val IconTextSpacing = 8.dp

    /** 标签内边距 */
    val BadgePaddingHorizontal = 8.dp
    val BadgePaddingVertical = 4.dp

    /** 搜索栏内边距 */
    val SearchBarPaddingHorizontal = 12.dp
    val SearchBarPaddingVertical = 10.dp

    /** 导航栏高度 */
    val NavBarHeight = 64.dp

    /** 导航栏按钮宽度 */
    val NavButtonWidth = 80.dp

    /** 悬浮按钮尺寸 */
    val FabSize = 64.dp

    /** 图标尺寸 */
    val IconSizeSmall = 12.dp
    val IconSizeMedium = 16.dp
    val IconSizeLarge = 18.dp
    val IconSizeExtraLarge = 20.dp
    val IconSizeHuge = 24.dp
    val IconSizeFab = 32.dp

    /** 卡片圆角 */
    val CardCornerRadius = 16.dp

    /** 搜索栏圆角 */
    val SearchBarCornerRadius = 24.dp

    /** 导航栏圆角 */
    val NavBarCornerRadius = 32.dp

    /** 标签圆角 */
    val BadgeCornerRadius = 8.dp

    /** 按钮圆角 */
    val ButtonCornerRadius = 16.dp

    /** 分割线高度 */
    val DividerHeight = 1.dp

    /** 进度条高度 */
    val ProgressBarHeight = 8.dp

    /** 标签指示器高度 */
    val TabIndicatorHeight = 3.dp
}