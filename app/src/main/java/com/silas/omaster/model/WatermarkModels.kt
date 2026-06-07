package com.silas.omaster.model

/**
 * 水印模板枚举
 */
enum class WatermarkTemplate {
    OPPO,
    ONEPLUS,
    REALME,
    MINIMAL_PARAMS,
    TIMESTAMP,
    LOCATION,
    CUSTOM,
    HASSELBLAD,
    BRAND_SIMPLE,
    FILM_STYLE,
    // 免费水印模板 - 参考2026年国内手机水印趋势
    TILE_PATTERN,        // 平铺水印 - 防盗用
    DIAGONAL_TEXT,       // 对角线文字 - 版权保护
    CAMERA_INFO,         // 相机参数水印 - Leica风格
    DATE_STAMP,          // 日期印章 - 证件照专用
    COPYRIGHT_SIGN,      // 版权符号 - ©️风格
    QR_CODE,             // 二维码水印
    SIGNATURE,           // 签名水印
    COLLAGE_GRID,        // 拼图九宫格
    SOCIAL_MEDIA,        // 社交媒体水印
    MINIMAL_CORNER,      // 极简角标
    // 新增2026年国内手机水印趋势
    STAMP,               // 邮票邮戳 - vivo风格
    CHINESE_STYLE,       // 国风印章 - 水墨风格
    FILM_FRAME,          // 胶片相框 - 小米风格
    NEW_YEAR,            // 新春舞狮 - 小米非遗
    LEICA_CLASSIC,       // 徕卡经典 - 小米联名
    ZEISS_OPTICS         // 蔡司光学 - vivo联名
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