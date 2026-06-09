package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌主题色
 * 对应 OPPO 哈苏联名官方色
 */
object HasselbladTheme {
    // ─── 核心品牌色 ───

    /**
     * 哈苏橙 - 主强调色
     * OPPO 哈苏联名官方色 #FF6B35
     */
    val HasselbladOrange = Color(0xFFFF6B35)

    /**
     * 哈苏橙渐变起始色
     */
    val HasselbladOrangeLight = Color(0xFFFF8B55)

    /**
     * 哈苏橙渐变结束色
     */
    val HasselbladOrangeDark = Color(0xFFFF5525)

    // ─── 背景色 ───

    /**
     * 纯黑背景
     * OPPO 大师模式深色主题 #0A0A0A
     */
    val PureBlack = Color(0xFF0A0A0A)

    /**
     * 卡片背景
     * rgba(255,255,255,0.05) 半透明白
     */
    val CardBackground = Color(0x0DFFFFFF)

    /**
     * 卡片背景高亮
     */
    val CardBackgroundHighlight = Color(0x14FFFFFF)

    /**
     * 分割线颜色
     */
    val DividerColor = Color(0x1AFFFFFF)

    // ─── 文字色 ───

    /**
     * 主文字 - 纯白
     */
    val TextPrimary = Color(0xFFFFFFFF)

    /**
     * 次文字 - 80%白
     */
    val TextSecondary = Color(0xCCFFFFFF)

    /**
     * 辅助文字 - 60%白
     */
    val TextTertiary = Color(0x99FFFFFF)

    /**
     * 禁用文字 - 40%白
     */
    val TextDisabled = Color(0x66FFFFFF)

    // ─── 功能色 ───

    /**
     * 成功色
     */
    val Success = Color(0xFF4CAF50)

    /**
     * 警告色
     */
    val Warning = Color(0xFFFFC107)

    /**
     * 错误色
     */
    val Error = Color(0xFFF44336)

    // ─── 胶片系列色 ───

    /**
     * 原生经典系列色
     */
    val FilmClassic = Color(0xFFFFB800)

    /**
     * 情绪与表达系列色
     */
    val FilmEmotion = Color(0xFFFF6B9B)

    /**
     * 结构与时间系列色
     */
    val FilmStructure = Color(0xFF6B7FFF)

    /**
     * 数字记忆系列色
     */
    val FilmDigital = Color(0xFF00D4AA)

    // ─── 参数滑块色 ───

    /**
     * 滑块轨道色
     */
    val SliderTrack = Color(0x33FFFFFF)

    /**
     * 滑块激活轨道色（哈苏橙）
     */
    val SliderActiveTrack = HasselbladOrange

    /**
     * 滑块手柄色
     */
    val SliderThumb = Color(0xFFFFFFFF)

    /**
     * 滑块手柄边框色
     */
    val SliderThumbBorder = HasselbladOrange

    // ─── 置信度条色 ───

    /**
     * 置信度条背景
     */
    val ConfidenceBarBackground = Color(0x1AFF6B35)

    /**
     * 置信度条填充（哈苏橙渐变）
     */
    val ConfidenceBarFill = HasselbladOrange

    // ─── 设计规范常量 ───

    /**
     * 卡片圆角
     */
    const val CardCornerRadius = 16

    /**
     * 小圆角
     */
    const val SmallCornerRadius = 8

    /**
     * 置信度条圆角
     */
    const val ConfidenceBarCornerRadius = 4

    /**
     * 滑块手柄大小
     */
    const val SliderThumbSize = 24

    /**
     * 胶片卡片宽度
     */
    const val FilmCardWidth = 100

    /**
     * 胶片卡片高度
     */
    const val FilmCardHeight = 120

    /**
     * 参数滑块高度
     */
    const val ParamSliderHeight = 40

    /**
     * 水印文字大小
     */
    const val WatermarkFontSize = 10

    /**
     * 水印内容
     */
    const val WatermarkText = "HNCS · OMaster"
}