package com.silas.omaster.data.watermark

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

/**
 * 水印类型
 */
enum class WatermarkType {
    BRAND,      // 品牌水印
    MASTER_MARK, // 大师印记（签名图）
    XPAN        // XPAN 宽幅水印
}

/**
 * 水印位置
 */
enum class WatermarkPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    CENTER
}

/**
 * 水印配置
 */
@Serializable
data class WatermarkConfig(
    val id: String,
    val name: String,
    val type: WatermarkType,
    val text: String = "",
    val fontFamily: String = "default",
    val fontColor: String = "#FFFFFF",
    val fontSize: Float = 48f,
    val backgroundColor: String? = null,
    val backgroundAlpha: Float = 0.6f,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val offsetX: Float = 0.05f,  // 相对图片宽度的偏移比例
    val offsetY: Float = 0.05f,  // 相对图片高度的偏移比例
    val signatureBitmapPath: String? = null, // 大师印记的签名图路径
    val signatureAlpha: Float = 0.8f,
    val xpanTopRatio: Float = 0.15f,  // XPAN 上黑边比例
    val xpanBottomRatio: Float = 0.15f, // XPAN 下黑边比例
    val xpanTextTop: String = "",
    val xpanTextBottom: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 水印应用结果
 */
data class WatermarkApplyResult(
    val bitmap: Bitmap,
    val config: WatermarkConfig
)
