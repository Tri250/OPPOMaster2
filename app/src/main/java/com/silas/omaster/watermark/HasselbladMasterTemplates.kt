package com.silas.omaster.watermark

import android.graphics.Color
import com.silas.omaster.watermark.WatermarkLayerSystem.*

/**
 * 哈苏品牌色板定义
 * 
 * 基于 OPPO × 哈苏品牌体系，定义专属色彩系统
 */
object HasselbladColorPalette {

    // ========== 主品牌色 ==========

    /** 哈苏橙 - 主品牌色 */
    const val HASSELBLAD_ORANGE = "#FF6B35"
    
    /** 哈苏橙（浅色变体） */
    const val HASSELBLAD_ORANGE_LIGHT = "#FF8C42"

    /** 哈苏橙（深色变体） */
    const val HASSELBLAD_ORANGE_DARK = "#E55A25"

    // ========== 金色点缀 ==========

    /** 金色 - 哈苏大师系列分割线、徽章边框 */
    const val GOLD_ACCENT = "#D4AF37"

    /** 金色（浅色变体） */
    const val GOLD_LIGHT = "#E5C158"

    /** 金色（深色变体） */
    const val GOLD_DARK = "#B8962E"

    // ========== 文字颜色 ==========

    /** 文字主色 - 默认水印文字色 */
    const val TEXT_PRIMARY = "#FFFFFF"

    /** 文字暗色 - 高调画面自动切换 */
    const val TEXT_DARK = "#1A1A1A"

    /** 文字灰色 - 次级信息 */
    const val TEXT_GRAY = "#808080"

    // ========== 背景颜色 ==========

    /** 半透明背景 - 底部信息栏背景 */
    const val BACKGROUND_SEMI_TRANSPARENT = "#000000"

    /** 半透明背景透明度 */
    const val BACKGROUND_ALPHA = 0.4f

    /** 卡片背景 */
    const val CARD_BACKGROUND = "#1A1A1A"

    // ========== HNCS 认证色 ==========

    /** HNCS 绿 - 认证标识点缀 */
    const val HNCS_GREEN = "#4CAF50"

    /** HNCS 绿（浅色变体） */
    const val HNCS_GREEN_LIGHT = "#66BB6A"

    // ========== 分割线颜色 ==========

    /** 分割线 - 金色分割线 */
    const val DIVIDER_GOLD = "#D4AF37"

    /** 分割线透明度 */
    const val DIVIDER_ALPHA = 0.3f

    /** 分割线（白色） */
    const val DIVIDER_WHITE = "#FFFFFF"

    // ========== 辅助方法 ==========

    /**
     * 获取哈苏橙颜色值
     */
    fun getHasselbladOrange(): Int = Color.parseColor(HASSELBLAD_ORANGE)

    /**
     * 获取金色颜色值
     */
    fun getGoldAccent(): Int = Color.parseColor(GOLD_ACCENT)

    /**
     * 获取HNCS绿色值
     */
    fun getHncsGreen(): Int = Color.parseColor(HNCS_GREEN)

    /**
     * 获取半透明黑色背景
     */
    fun getSemiTransparentBackground(): Int {
        val alpha = (BACKGROUND_ALPHA * 255).toInt()
        return Color.argb(alpha, 0, 0, 0)
    }

    /**
     * 获取分割线颜色（带透明度）
     */
    fun getDividerColor(useGold: Boolean = true): Int {
        val baseColor = if (useGold) GOLD_ACCENT else DIVIDER_WHITE
        val alpha = (DIVIDER_ALPHA * 255).toInt()
        val parsed = Color.parseColor(baseColor)
        return Color.argb(alpha, Color.red(parsed), Color.green(parsed), Color.blue(parsed))
    }

    /**
     * 根据亮度推荐文字颜色
     */
    fun recommendTextColor(luminance: Float): Int {
        return if (luminance > 0.7f) {
            Color.parseColor(TEXT_DARK)
        } else {
            Color.parseColor(TEXT_PRIMARY)
        }
    }

    /**
     * 获取品牌渐变色数组
     */
    fun getBrandGradient(): IntArray {
        return intArrayOf(
            Color.parseColor(HASSELBLAD_ORANGE),
            Color.parseColor(HASSELBLAD_ORANGE_LIGHT)
        )
    }

    /**
     * 获取金色渐变色数组
     */
    fun getGoldGradient(): IntArray {
        return intArrayOf(
            Color.parseColor(GOLD_DARK),
            Color.parseColor(GOLD_ACCENT),
            Color.parseColor(GOLD_LIGHT)
        )
    }
}

/**
 * 哈苏大师印记专属模板
 * 
 * 包含 3 款哈苏专属水印模板：
 * 1. 哈苏大师印记 (hasselblad-master)
 * 2. HNCS 认证标识 (hasselblad-hncs)
 * 3. XPAN 宽幅印记 (hasselblad-xpan)
 */
object HasselbladMasterTemplates {

    /**
     * 获取所有哈苏大师模板
     */
    fun getAll(): List<WatermarkTemplateDef> = listOf(
        hasselbladMasterTemplate(),
        hasselbladHncsTemplate(),
        hasselbladXpanTemplate()
    )

    /**
     * 模板 1：哈苏大师印记 (hasselblad-master)
     * 
     * 设计要素：
     * - 底部信息栏高约 8% 画面高度，半透明黑色背景 (alpha=0.4)
     * - "HASSELBLAD" 使用 bold 字体，letterSpacing=2，金色 (#D4AF37)
     * - "HNCS" 认证标识为小号标签，带金色边框
     * - 参数行使用 MONOSPACE 字体，体现技术感
     * - 分割线 1px，alpha=0.3
     */
    private fun hasselbladMasterTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "hasselblad-master",
            name = "哈苏大师印记",
            description = "OPPO × 哈苏大师系列专属水印，金色分割线 + HNCS认证标识",
            category = "哈苏",
            isSystem = true,
            layers = listOf(
                // 金色分割线
                WatermarkLayerDef(
                    id = "divider_gold",
                    type = WatermarkLayerType.SHAPE,
                    content = "divider_horizontal",
                    position = WatermarkPosition.BOTTOM,
                    style = WatermarkLayerStyle(
                        colorHex = HasselbladColorPalette.GOLD_ACCENT,
                        opacity = 0.3f,
                        padding = 0f
                    ),
                    sortOrder = 10,
                    isRequired = false
                ),

                // 品牌 + HNCS 认证标识
                WatermarkLayerDef(
                    id = "brand_hncs",
                    type = WatermarkLayerType.BRAND,
                    content = "HASSELBLAD  ▎  HNCS",
                    defaultContent = "HASSELBLAD  ▎  HNCS",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 16f,
                        fontWeight = 700,
                        colorHex = HasselbladColorPalette.GOLD_ACCENT,
                        letterSpacing = 2f,
                        opacity = 1f,
                        shadowEnabled = false
                    ),
                    sortOrder = 9,
                    isRequired = true,
                    contentSource = ContentSource.MANUAL
                ),

                // 设备型号
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 13f,
                        fontWeight = 500,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.9f,
                        shadowEnabled = true,
                        shadowBlur = 4f
                    ),
                    sortOrder = 8,
                    contentSource = ContentSource.DEVICE_INFO
                ),

                // 拍摄参数（MONOSPACE 字体）
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 12f,
                        fontFamily = "monospace",
                        fontWeight = 400,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.85f,
                        letterSpacing = 1f,
                        shadowEnabled = true,
                        shadowBlur = 3f
                    ),
                    sortOrder = 7,
                    contentSource = ContentSource.EXIF
                ),

                // 白色分割线
                WatermarkLayerDef(
                    id = "divider_white",
                    type = WatermarkLayerType.SHAPE,
                    content = "divider_horizontal",
                    position = WatermarkPosition.BOTTOM,
                    style = WatermarkLayerStyle(
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.2f,
                        padding = 0f
                    ),
                    sortOrder = 6,
                    isRequired = false
                ),

                // 日期 + 位置
                WatermarkLayerDef(
                    id = "date_location",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 11f,
                        fontWeight = 400,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.75f,
                        shadowEnabled = true,
                        shadowBlur = 2f
                    ),
                    sortOrder = 5,
                    contentSource = ContentSource.SYSTEM
                ),

                // 底部信息栏背景
                WatermarkLayerDef(
                    id = "info_bar_bg",
                    type = WatermarkLayerType.SHAPE,
                    content = "rect_background",
                    position = WatermarkPosition.BOTTOM,
                    style = WatermarkLayerStyle(
                        backgroundColorHex = HasselbladColorPalette.BACKGROUND_SEMI_TRANSPARENT,
                        backgroundOpacity = HasselbladColorPalette.BACKGROUND_ALPHA,
                        opacity = 1f,
                        padding = 12f,
                        cornerRadius = 0f
                    ),
                    sortOrder = 0,
                    isRequired = false
                ),

                // 底部品牌栏
                WatermarkLayerDef(
                    id = "brand_bar",
                    type = WatermarkLayerType.TEXT,
                    content = "OPPO × Hasselblad  |  Master Edition",
                    defaultContent = "OPPO × Hasselblad  |  Master Edition",
                    position = WatermarkPosition.BOTTOM,
                    style = WatermarkLayerStyle(
                        fontSize = 10f,
                        fontWeight = 400,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.6f,
                        letterSpacing = 0.5f
                    ),
                    sortOrder = 1,
                    contentSource = ContentSource.MANUAL
                )
            )
        )
    }

    /**
     * 模板 2：HNCS 认证标识 (hasselblad-hncs)
     * 
     * 设计要素：
     * - 左上角认证徽章
     * - 圆角矩形 + 金色边框
     * - "👑 HNCS" 标识
     * - "哈苏自然色彩认证" 说明文字
     */
    private fun hasselbladHncsTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "hasselblad-hncs",
            name = "HNCS 认证标识",
            description = "哈苏自然色彩认证徽章，左上角金色边框认证标识",
            category = "哈苏",
            isSystem = true,
            layers = listOf(
                // HNCS 认证徽章背景
                WatermarkLayerDef(
                    id = "badge_bg",
                    type = WatermarkLayerType.SHAPE,
                    content = "badge_rect",
                    position = WatermarkPosition.TOP_LEFT,
                    style = WatermarkLayerStyle(
                        backgroundColorHex = HasselbladColorPalette.BACKGROUND_SEMI_TRANSPARENT,
                        backgroundOpacity = 0.6f,
                        colorHex = HasselbladColorPalette.GOLD_ACCENT, // 金色边框
                        opacity = 1f,
                        padding = 8f,
                        cornerRadius = 8f
                    ),
                    sortOrder = 10,
                    offset = OffsetData(8f, 8f)
                ),

                // 👑 HNCS 标识
                WatermarkLayerDef(
                    id = "hncs_icon",
                    type = WatermarkLayerType.TEXT,
                    content = "👑 HNCS",
                    defaultContent = "👑 HNCS",
                    position = WatermarkPosition.TOP_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 14f,
                        fontWeight = 700,
                        colorHex = HasselbladColorPalette.GOLD_ACCENT,
                        letterSpacing = 1f,
                        opacity = 1f,
                        shadowEnabled = false
                    ),
                    sortOrder = 9,
                    offset = OffsetData(12f, 12f),
                    contentSource = ContentSource.MANUAL
                ),

                // 哈苏自然色彩认证
                WatermarkLayerDef(
                    id = "hncs_desc",
                    type = WatermarkLayerType.TEXT,
                    content = "哈苏自然\n色彩认证",
                    defaultContent = "哈苏自然\n色彩认证",
                    position = WatermarkPosition.TOP_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 11f,
                        fontWeight = 400,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.9f,
                        lineHeight = 1.3f,
                        shadowEnabled = true,
                        shadowBlur = 3f
                    ),
                    sortOrder = 8,
                    offset = OffsetData(12f, 28f),
                    contentSource = ContentSource.MANUAL
                ),

                // HNCS 绿色点缀
                WatermarkLayerDef(
                    id = "hncs_dot",
                    type = WatermarkLayerType.SHAPE,
                    content = "circle_dot",
                    position = WatermarkPosition.TOP_LEFT,
                    style = WatermarkLayerStyle(
                        colorHex = HasselbladColorPalette.HNCS_GREEN,
                        opacity = 1f,
                        padding = 4f
                    ),
                    sortOrder = 7,
                    offset = OffsetData(60f, 12f)
                ),

                // 设备型号（右下角）
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(
                        fontSize = 12f,
                        fontWeight = 500,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.8f,
                        shadowEnabled = true,
                        shadowBlur = 4f
                    ),
                    sortOrder = 5,
                    contentSource = ContentSource.DEVICE_INFO
                )
            )
        )
    }

    /**
     * 模板 3：XPAN 宽幅印记 (hasselblad-xpan)
     * 
     * 设计要素：
     * - 致敬哈苏 XPAN 宽幅相机
     * - 底部信息条
     * - "▎XPAN  65:24" 标识
     * - "HASSELBLAD  ·  OPPO Find X8 Pro"
     */
    private fun hasselbladXpanTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "hasselblad-xpan",
            name = "XPAN 宽幅印记",
            description = "致敬哈苏 XPAN 宽幅相机，65:24 宽幅比例标识",
            category = "哈苏",
            isSystem = true,
            layers = listOf(
                // 底部信息条背景
                WatermarkLayerDef(
                    id = "xpan_bar_bg",
                    type = WatermarkLayerType.SHAPE,
                    content = "rect_background",
                    position = WatermarkPosition.BOTTOM,
                    style = WatermarkLayerStyle(
                        backgroundColorHex = HasselbladColorPalette.BACKGROUND_SEMI_TRANSPARENT,
                        backgroundOpacity = 0.5f,
                        opacity = 1f,
                        padding = 16f,
                        cornerRadius = 0f
                    ),
                    sortOrder = 0
                ),

                // ▎XPAN  65:24
                WatermarkLayerDef(
                    id = "xpan_label",
                    type = WatermarkLayerType.TEXT,
                    content = "▎XPAN  65:24",
                    defaultContent = "▎XPAN  65:24",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 14f,
                        fontWeight = 700,
                        colorHex = HasselbladColorPalette.GOLD_ACCENT,
                        letterSpacing = 2f,
                        opacity = 1f,
                        shadowEnabled = false
                    ),
                    sortOrder = 10,
                    contentSource = ContentSource.MANUAL
                ),

                // HASSELBLAD  ·  OPPO Find X8 Pro
                WatermarkLayerDef(
                    id = "xpan_device",
                    type = WatermarkLayerType.TEXT,
                    content = "HASSELBLAD  ·  ",
                    defaultContent = "HASSELBLAD  ·  ",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 12f,
                        fontWeight = 500,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.9f,
                        letterSpacing = 1f,
                        shadowEnabled = true,
                        shadowBlur = 3f
                    ),
                    sortOrder = 9,
                    contentSource = ContentSource.MANUAL
                ),

                // 设备型号（动态）
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 12f,
                        fontWeight = 500,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.9f,
                        shadowEnabled = true,
                        shadowBlur = 3f
                    ),
                    sortOrder = 8,
                    contentSource = ContentSource.DEVICE_INFO
                ),

                // 拍摄参数
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(
                        fontSize = 11f,
                        fontFamily = "monospace",
                        fontWeight = 400,
                        colorHex = HasselbladColorPalette.TEXT_PRIMARY,
                        opacity = 0.75f,
                        shadowEnabled = true,
                        shadowBlur = 2f
                    ),
                    sortOrder = 7,
                    contentSource = ContentSource.EXIF
                )
            )
        )
    }

    /**
     * 根据ID获取模板
     */
    fun getById(id: String): WatermarkTemplateDef? {
        return getAll().find { it.id == id }
    }

    /**
     * 获取所有哈苏模板 + 系统基础模板
     */
    fun getAllWithBaseTemplates(): List<WatermarkTemplateDef> {
        return getAll() + SystemWatermarkTemplates.getAll()
    }
}

/**
 * 偏移数据（用于自定义位置微调）
 */
data class OffsetData(
    val x: Float = 0f,
    val y: Float = 0f
)