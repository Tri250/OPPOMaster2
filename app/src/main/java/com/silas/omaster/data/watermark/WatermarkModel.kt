package com.silas.omaster.data.watermark

import kotlinx.serialization.Serializable

/**
 * 水印类型枚举
 */
@Serializable
enum class WatermarkType {
    BRAND,        // 品牌水印
    MASTER_MARK,  // 大师签名水印
    XPAN          // XPAN 宽幅水印
}

/**
 * 水印位置枚举
 */
@Serializable
enum class WatermarkPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

/**
 * 水印配置数据类
 */
@Serializable
data class WatermarkConfig(
    val type: WatermarkType = WatermarkType.BRAND,
    val text: String = "",
    val fontName: String = "Default",
    val fontSize: Int = 24,
    val color: Long = 0xFFFFFFFFL,  // ARGB
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val opacity: Float = 1.0f,
    val signatureImagePath: String = "",
    val xpanBarRatio: Float = 0.1f,
    val xpanText: String = ""
)

/**
 * 水印预设模板
 */
@Serializable
data class WatermarkTemplate(
    val id: String,
    val name: String,
    val config: WatermarkConfig
)

/**
 * 预置水印模板列表
 */
object WatermarkTemplates {

    val brandTemplates = listOf(
        WatermarkTemplate(
            id = "brand_classic",
            name = "经典品牌",
            config = WatermarkConfig(
                type = WatermarkType.BRAND,
                text = "OMaster",
                fontName = "Default",
                fontSize = 24,
                color = 0xFFFFFFFFL,
                position = WatermarkPosition.BOTTOM_LEFT,
                opacity = 0.8f
            )
        ),
        WatermarkTemplate(
            id = "brand_minimal",
            name = "极简签名",
            config = WatermarkConfig(
                type = WatermarkType.BRAND,
                text = "© Photo",
                fontName = "Default",
                fontSize = 18,
                color = 0xFFFFFFFFL,
                position = WatermarkPosition.BOTTOM_RIGHT,
                opacity = 0.6f
            )
        )
    )

    val masterMarkTemplates = listOf(
        WatermarkTemplate(
            id = "master_signature",
            name = "大师签名",
            config = WatermarkConfig(
                type = WatermarkType.MASTER_MARK,
                position = WatermarkPosition.BOTTOM_RIGHT,
                opacity = 0.7f
            )
        )
    )

    val xpanTemplates = listOf(
        WatermarkTemplate(
            id = "xpan_classic",
            name = "经典宽幅",
            config = WatermarkConfig(
                type = WatermarkType.XPAN,
                xpanBarRatio = 0.1f,
                xpanText = "XPAN"
            )
        ),
        WatermarkTemplate(
            id = "xpan_cinematic",
            name = "电影画幅",
            config = WatermarkConfig(
                type = WatermarkType.XPAN,
                xpanBarRatio = 0.15f,
                xpanText = "CINEMATIC"
            )
        )
    )

    fun getTemplatesForType(type: WatermarkType): List<WatermarkTemplate> {
        return when (type) {
            WatermarkType.BRAND -> brandTemplates
            WatermarkType.MASTER_MARK -> masterMarkTemplates
            WatermarkType.XPAN -> xpanTemplates
        }
    }
}
