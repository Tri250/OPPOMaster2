package com.silas.omaster.model

import com.google.gson.annotations.SerializedName

/**
 * 水印模板列表（JSON根对象）
 */
data class WatermarkTemplateList(
    val version: Int = 1,
    val name: String = "",
    val author: String = "",
    val build: Int = 1,
    val templates: List<WatermarkTemplateData> = emptyList()
)

/**
 * 水印模板数据类 - 从JSON加载
 */
data class WatermarkTemplateData(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    @SerializedName("previewPath")
    val previewPath: String = "",
    val category: String = "free",
    val features: List<String> = emptyList(),
    val source: String? = null,
    val style: WatermarkStyle = WatermarkStyle()
)

/**
 * 水印样式配置
 */
data class WatermarkStyle(
    val backgroundColor: String = "#FFFFFF",
    val textColor: String = "#000000",
    val accentColor: String? = null,
    val borderColor: String? = null,
    val borderWidth: Int = 0,
    val borderStyle: String? = null,
    val borderRadius: Int = 0,
    val padding: Int = 16,
    val backgroundAlpha: Float = 1.0f,
    val textAlpha: Float = 1.0f,
    val fontSize: Int = 16,
    val fontWeight: String? = null,
    val fontFamily: String? = null,
    val textShadow: Boolean = false,
    val showLogo: Boolean = true,
    val showParams: Boolean = true,
    val paramLayout: String? = null,
    val showTimestamp: Boolean = false,
    val showLocation: Boolean = false,
    val iconStyle: String? = null,
    val animated: Boolean = false,
    val stampStyle: Boolean = false,
    val sealStyle: Boolean = false,
    val filmStyle: Boolean = false,
    val handwritten: Boolean = false,
    val tiled: Boolean = false,
    val diagonal: Boolean = false,
    val minimal: Boolean = false,
    val rotation: Int = 0,
    val festivalTheme: String? = null
)

/**
 * 水印分类枚举
 */
enum class WatermarkCategory(val displayName: String) {
    BRAND("品牌"),
    FUNCTIONAL("功能"),
    FREE("免费");

    companion object {
        fun fromString(value: String): WatermarkCategory {
            return when (value.lowercase()) {
                "brand" -> BRAND
                "functional" -> FUNCTIONAL
                "free" -> FREE
                else -> FREE
            }
        }
    }
}

/**
 * 水印模板UI展示数据
 */
data class WatermarkTemplateUiData(
    val id: String,
    val name: String,
    val description: String,
    val previewPath: String,
    val category: WatermarkCategory,
    val features: List<String>,
    val source: String?,
    val style: WatermarkStyle
) {
    companion object {
        fun fromData(data: WatermarkTemplateData): WatermarkTemplateUiData {
            return WatermarkTemplateUiData(
                id = data.id,
                name = data.name,
                description = data.description,
                previewPath = data.previewPath,
                category = WatermarkCategory.fromString(data.category),
                features = data.features,
                source = data.source,
                style = data.style
            )
        }
    }
}
