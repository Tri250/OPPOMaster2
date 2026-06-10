package com.silas.omaster.data.model

import kotlinx.serialization.Serializable

/**
 * 统一水印模板数据模型
 * 双端统一：Android (Kotlin) ↔ Web (TypeScript)
 */

// ========== 分类枚举 ==========
enum class WatermarkCategory(
    val key: String,
    val displayName: String,
    val icon: String
) {
    ALL("all", "全部", "📋"),
    BRAND("brand", "品牌认证", "⭐"),
    MINIMAL("minimal", "极简印记", "✨"),
    TECH("tech", "技术参数", "⚙️"),
    INFO("info", "信息记录", "📅"),
    PERSONAL("personal", "个人签名", "✍️"),
    SOCIAL("social", "社交分享", "@"),
    LEGAL("legal", "版权保护", "©"),
    BADGE("badge", "荣誉徽章", "🏆"),
    PRO("pro", "专业防伪", "🔐"),
    HASSELBLAD("hasselblad", "哈苏大师", "👑");

    companion object {
        fun fromKey(key: String): WatermarkCategory =
            entries.find { it.key == key } ?: ALL
    }
}

// ========== 图层类型 ==========
enum class WatermarkLayerType {
    TEXT,        // 自由文本
    BRAND,       // 品牌名
    DEVICE,      // 设备型号（自动）
    PARAMS,      // 拍摄参数（EXIF）
    TIMESTAMP,   // 时间戳（自动）
    LOCATION,    // GPS 位置（自动）
    LOGO,        // Logo 图片
    SHAPE,       // 形状
    VIGNETTE     // 暗角效果
}

// ========== 内容来源 ==========
enum class ContentSource {
    MANUAL,      // 手动输入
    EXIF,        // 从照片 EXIF 读取
    GPS,         // 从 GPS 坐标反地理编码
    SYSTEM,      // 系统时间
    DEVICE_INFO  // 设备信息
}

// ========== 图层样式 ==========
@Serializable
data class WatermarkLayerStyle(
    val fontSize: Float = 14f,
    val fontFamily: String = "default",
    val fontWeight: Int = 400,
    val color: String = "#FFFFFF",
    val opacity: Float = 0.8f,
    val letterSpacing: Float = 0f,
    val rotation: Float = 0f,
    val shadowEnabled: Boolean = true,
    val shadowBlur: Float = 4f,
    val backgroundColor: String = "transparent",
    val backgroundOpacity: Float = 0f,
    val padding: Float = 8f
)

// ========== 水印位置 ==========
enum class WatermarkPosition(val key: String, val displayName: String) {
    TOP_LEFT("top-left", "左上"),
    TOP_CENTER("top-center", "上中"),
    TOP_RIGHT("top-right", "右上"),
    CENTER_LEFT("center-left", "左中"),
    CENTER("center", "居中"),
    CENTER_RIGHT("center-right", "右中"),
    BOTTOM_LEFT("bottom-left", "左下"),
    BOTTOM_CENTER("bottom-center", "下中"),
    BOTTOM_RIGHT("bottom-right", "右下");

    companion object {
        fun fromKey(key: String): WatermarkPosition =
            entries.find { it.key == key } ?: BOTTOM_LEFT
    }
}

// ========== 图层定义 ==========
@Serializable
data class WatermarkLayerDef(
    val id: String,
    val type: WatermarkLayerType,
    val defaultContent: String = "",
    val defaultPosition: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val defaultStyle: WatermarkLayerStyle = WatermarkLayerStyle(),
    val isRequired: Boolean = false,
    val contentSource: ContentSource = ContentSource.MANUAL,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)

// ========== 预设样式 ==========
@Serializable
data class WatermarkStylePreset(
    val primaryColor: String = "#FFFFFF",
    val secondaryColor: String = "#FFFFFF",
    val fontSize: Float = 14f,
    val opacity: Float = 0.8f,
    val letterSpacing: Float = 0f,
    val shadowEnabled: Boolean = true,
    val shadowBlur: Float = 4f,
    val fontFamily: String = "default"
)

// ========== 统一模板 ==========
@Serializable
data class MasterWatermarkTemplate(
    val id: String,
    val name: String,
    val nameEn: String,
    val category: WatermarkCategory,
    val description: String = "",
    val previewThumb: String = "",
    val isHasselbladSeries: Boolean = false,
    val layers: List<WatermarkLayerDef> = emptyList(),
    val presetStyle: WatermarkStylePreset = WatermarkStylePreset(),
    val defaultPosition: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val isPopular: Boolean = false,
    val isNew: Boolean = false
)

// ========== EXIF 数据 ==========
@Serializable
data class ExifWatermarkData(
    val make: String? = null,
    val model: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val dateTaken: String? = null,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val locationName: String? = null,
    val lensModel: String? = null,
    val flashUsed: Boolean = false,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
) {
    fun getDeviceInfo(): String {
        return if (!make.isNullOrEmpty() && !model.isNullOrEmpty()) {
            "$make $model"
        } else if (!model.isNullOrEmpty()) {
            model
        } else {
            "Unknown Device"
        }
    }
    
    fun getParamsInfo(): String {
        val parts = mutableListOf<String>()
        if (!aperture.isNullOrEmpty()) parts.add(aperture)
        if (!shutterSpeed.isNullOrEmpty()) parts.add(shutterSpeed)
        if (!iso.isNullOrEmpty()) parts.add(iso)
        if (!focalLength.isNullOrEmpty()) parts.add(focalLength)
        return parts.joinToString("  ")
    }
}
