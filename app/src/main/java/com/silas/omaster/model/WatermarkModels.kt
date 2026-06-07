package com.silas.omaster.model

/**
 * 水印模板枚举
 */
enum class WatermarkTemplate(
    val displayName: String,
    val category: String,
    val defaultText: String
) {
    OPPO("OPPO", "品牌", "OPPO"),
    ONEPLUS("OnePlus", "品牌", "OnePlus"),
    REALME("realme", "品牌", "realme"),
    MINIMAL_PARAMS("极简参数", "极简", ""),
    TIMESTAMP("时间戳", "实用", "2026.01.01"),
    LOCATION("地理位置", "实用", "Unknown Location"),
    CUSTOM("自定义", "自定义", ""),
    HASSELBLAD("HASSELBLAD", "品牌", "HASSELBLAD"),
    BRAND_SIMPLE("OMaster", "极简", "OMaster"),
    FILM_STYLE("胶片参数", "胶片", ""),
    // 免费水印模板 - 参考2026年国内手机水印趋势
    TILE_PATTERN("平铺水印", "防盗用", "SAMPLE"),
    DIAGONAL_TEXT("对角线文字", "防盗用", "COPYRIGHT"),
    CAMERA_INFO("相机参数", "参数", ""),
    DATE_STAMP("日期印章", "证件", "身份核验"),
    COPYRIGHT_SIGN("版权符号", "版权", "Author"),
    QR_CODE("二维码", "实用", "QR"),
    SIGNATURE("签名", "艺术", "Signature"),
    COLLAGE_GRID("拼图九宫格", "实用", ""),
    SOCIAL_MEDIA("社交媒体", "实用", "username"),
    MINIMAL_CORNER("极简角标", "极简", "© 2026"),
    // 新增2026年国内手机水印趋势
    STAMP("邮票邮戳", "国风", "北京"),
    CHINESE_STYLE("国风印章", "国风", "摄影"),
    FILM_FRAME("胶片相框", "胶片", ""),
    NEW_YEAR("新春舞狮", "节日", "新春快乐"),
    LEICA_CLASSIC("徕卡经典", "品牌", "LEICA"),
    ZEISS_OPTICS("蔡司光学", "品牌", "ZEISS T*")
}

/**
 * 水印位置枚举
 */
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

/**
 * 输出格式枚举
 */
enum class OutputFormat {
    JPEG,
    PNG,
    TIFF
}

/**
 * 水印相机参数
 */
data class CameraParamsForWatermark(
    val iso: String = "100",
    val shutterSpeed: String = "1/1000s",
    val aperture: String = "f/1.7",
    val ev: String = "0"
)

/**
 * 水印配置
 */
data class WatermarkConfig(
    val template: WatermarkTemplate,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.8f,
    val scale: Float = 1.0f,
    val customText: String? = null,
    val showTimestamp: Boolean = true,
    val showDevice: Boolean = true,
    val timestampFormat: String = "yyyy-MM-dd HH:mm",
    val preserveOriginal: Boolean = true,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val quality: Int = 95,
    val cameraParams: CameraParamsForWatermark? = null
)

/**
 * 水印处理请求
 */
data class WatermarkProcessRequest(
    val sourceBitmap: android.graphics.Bitmap,
    val config: WatermarkConfig,
    val outputPath: String? = null
)

/**
 * 水印处理结果
 */
data class WatermarkProcessResult(
    val success: Boolean,
    val bitmap: android.graphics.Bitmap? = null,
    val error: String? = null
)