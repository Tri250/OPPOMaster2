package com.silas.omaster.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.silas.omaster.util.LogUtil
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * EXIF 数据自动填充提供者
 * 
 * 功能：
 * - 从照片文件读取完整 EXIF 信息
 * - 格式化参数显示（光圈、快门、焦距等）
 * - GPS 反地理编码获取位置名称
 * - 智能后备方案（无 EXIF 时的默认值）
 */
class ExifWatermarkProvider(private val context: Context) {

    /**
     * EXIF 水印数据结构
     */
    data class ExifWatermarkData(
        val make: String?,                    // 制造商: "OPPO"
        val model: String?,                   // 型号: "Find X8 Pro"
        val fullDeviceName: String?,          // 完整设备名: "OPPO Find X8 Pro"
        val aperture: String?,                // 光圈: "f/1.6"
        val shutterSpeed: String?,            // 快门: "1/500s"
        val iso: String?,                     // ISO: "ISO 100"
        val focalLength: String?,             // 焦距: "23mm"
        val exposureTime: String?,            // 曝光时间（原始值）
        val dateTaken: String?,               // 拍摄日期: "2026-06-09"
        val timeTaken: String?,               // 拍摄时间: "14:30"
        val fullDateTime: String?,            // 完整时间: "2026-06-09 14:30"
        val gpsLat: Double?,                  // 纬度
        val gpsLng: Double?,                  // 经度
        val locationName: String?,            // 反地理编码: "杭州市西湖区"
        val lensModel: String?,               // 镜头型号
        val flashUsed: Boolean?,              // 是否使用闪光灯
        val imageWidth: Int?,                 // 图片宽度
        val imageHeight: Int?,                // 图片高度
        val orientation: Int?,                // 方向
        val whiteBalance: String?,            // 白平衡
        val focalLength35mm: String?,         // 35mm等效焦距
        val software: String?,                // 软件/APP
        val artist: String?                   // 摄影师
    ) {
        /**
         * 获取格式化的拍摄参数字符串
         */
        fun getFormattedParams(): String {
            val parts = mutableListOf<String>()
            
            aperture?.let { parts.add(it) }
            shutterSpeed?.let { parts.add(it) }
            iso?.let { parts.add(it) }
            focalLength?.let { parts.add(it) }
            
            return parts.joinToString(" ")
        }

        /**
         * 获取格式化的日期字符串
         */
        fun getFormattedDate(): String {
            return dateTaken ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())
        }

        /**
         * 获取格式化的时间字符串
         */
        fun getFormattedTime(): String {
            return timeTaken ?: SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date())
        }

        /**
         * 获取完整设备名称
         */
        fun getFullDevice(): String {
            return fullDeviceName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
        }

        /**
         * 是否有GPS数据
         */
        fun hasGpsData(): Boolean = gpsLat != null && gpsLng != null

        /**
         * 是否有完整EXIF数据
         */
        fun hasFullExif(): Boolean {
            return make != null && model != null && aperture != null && 
                   shutterSpeed != null && iso != null
        }
    }

    /**
     * 从 Uri 提取 EXIF 数据
     */
    fun extractFromUri(uri: Uri): ExifWatermarkData {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                extractFromInputStream(inputStream)
            } ?: getFallbackData()
        } catch (e: Exception) {
            LogUtil.logThrowable("Exif", e, "从 URI 提取 EXIF 失败")
            getFallbackData()
        }
    }

    /**
     * 从文件路径提取 EXIF 数据
     */
    fun extractFromPath(path: String): ExifWatermarkData {
        return try {
            val exif = ExifInterface(path)
            parseExifData(exif)
        } catch (e: Exception) {
            LogUtil.logThrowable("Exif", e, "从路径提取 EXIF 失败: $path")
            getFallbackData()
        }
    }

    /**
     * 从 InputStream 提取 EXIF 数据
     */
    private fun extractFromInputStream(inputStream: InputStream): ExifWatermarkData {
        return try {
            val exif = ExifInterface(inputStream)
            parseExifData(exif)
        } catch (e: Exception) {
            LogUtil.logThrowable("Exif", e, "从 InputStream 提取 EXIF 失败")
            getFallbackData()
        }
    }

    /**
     * 解析 EXIF 数据
     */
    private fun parseExifData(exif: ExifInterface): ExifWatermarkData {
        // 制造商和型号
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
        val fullDeviceName = if (make != null && model != null) {
            "$make $model"
        } else {
            null
        }

        // 光圈
        val aperture = formatAperture(exif.getAttribute(ExifInterface.TAG_F_NUMBER))

        // 快门速度
        val shutterSpeed = formatShutterSpeed(
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        )

        // ISO
        val iso = formatIso(
            exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY) ?: 
            exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        )

        // 焦距
        val focalLength = formatFocalLength(
            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        )

        // 35mm等效焦距
        val focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let {
            "${it}mm"
        }

        // 日期时间
        val dateTimeStr = exif.getAttribute(ExifInterface.TAG_DATETIME)
        val (dateTaken, timeTaken, fullDateTime) = parseDateTime(dateTimeStr)

        // GPS坐标
        val latLong = exif.latLong
        val gpsLat = latLong?.get(0)
        val gpsLng = latLong?.get(1)

        // 反地理编码获取位置名称
        val locationName = if (gpsLat != null && gpsLng != null) {
            reverseGeocode(gpsLat, gpsLng)
        } else {
            null
        }

        // 图片尺寸
        val imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        // 其他信息
        val lensModel = exif.getAttribute("LensModel")
        val flashUsed = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0) != 0
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
        val artist = exif.getAttribute(ExifInterface.TAG_ARTIST)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val whiteBalance = formatWhiteBalance(
            exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, 0)
        )

        return ExifWatermarkData(
            make = make,
            model = model,
            fullDeviceName = fullDeviceName,
            aperture = aperture,
            shutterSpeed = shutterSpeed,
            iso = iso,
            focalLength = focalLength,
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            dateTaken = dateTaken,
            timeTaken = timeTaken,
            fullDateTime = fullDateTime,
            gpsLat = gpsLat,
            gpsLng = gpsLng,
            locationName = locationName,
            lensModel = lensModel,
            flashUsed = flashUsed,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            orientation = orientation,
            whiteBalance = whiteBalance,
            focalLength35mm = focalLength35mm,
            software = software,
            artist = artist
        )
    }

    /**
     * 格式化光圈值
     * "160/100" → "f/1.6"
     */
    private fun formatAperture(value: String?): String? {
        if (value == null) return null
        
        return try {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull() ?: 0.0
                val denominator = parts[1].toDoubleOrNull() ?: 1.0
                if (denominator > 0) {
                    val f = numerator / denominator
                    "f/${formatDecimal(f)}"
                } else {
                    null
                }
            } else {
                val f = value.toDoubleOrNull()
                if (f != null && f > 0) {
                    "f/${formatDecimal(f)}"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 格式化快门速度
     * "1/500" → "1/500s"
     * "0.002" → "1/500s"
     */
    private fun formatShutterSpeed(value: String?): String? {
        if (value == null) return null
        
        return try {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull() ?: 0.0
                val denominator = parts[1].toDoubleOrNull() ?: 1.0
                if (denominator > 0 && numerator > 0) {
                    val seconds = numerator / denominator
                    if (seconds < 1) {
                        // 快门速度小于1秒，显示分数形式
                        "${numerator.toInt()}/${denominator.toInt()}s"
                    } else {
                        // 快门速度大于1秒，显示秒数
                        "${formatDecimal(seconds)}s"
                    }
                } else {
                    null
                }
            } else {
                val seconds = value.toDoubleOrNull()
                if (seconds != null && seconds > 0) {
                    if (seconds < 1) {
                        // 转换为分数形式
                        val denominator = 1.0 / seconds
                        if (denominator.isFinite() && denominator < 1000) {
                            "1/${denominator.toInt()}s"
                        } else {
                            "${formatDecimal(seconds)}s"
                        }
                    } else {
                        "${formatDecimal(seconds)}s"
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 格式化ISO
     * "100" → "ISO 100"
     */
    private fun formatIso(value: String?): String? {
        if (value == null) return null
        val isoValue = value.toIntOrNull()
        return if (isoValue != null && isoValue > 0) {
            "ISO $isoValue"
        } else {
            null
        }
    }

    /**
     * 格式化焦距
     * "23/1" → "23mm"
     */
    private fun formatFocalLength(value: String?): String? {
        if (value == null) return null
        
        return try {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].toDoubleOrNull() ?: 0.0
                val denominator = parts[1].toDoubleOrNull() ?: 1.0
                if (denominator > 0) {
                    val mm = numerator / denominator
                    "${mm.toInt()}mm"
                } else {
                    null
                }
            } else {
                val mm = value.toDoubleOrNull()
                if (mm != null && mm > 0) {
                    "${mm.toInt()}mm"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析日期时间
     * "2026:06:09 14:30:00" → ("2026-06-09", "14:30", "2026-06-09 14:30")
     */
    private fun parseDateTime(value: String?): Triple<String?, String?, String?> {
        if (value == null) return Triple(null, null, null)
        
        return try {
            // EXIF日期格式: "yyyy:MM:dd HH:mm:ss"
            val parts = value.split(" ")
            if (parts.size >= 2) {
                val datePart = parts[0].replace(":", "-")
                val timePart = parts[1].substring(0, 5) // 只取 HH:mm
                Triple(datePart, timePart, "$datePart $timePart")
            } else {
                Triple(null, null, null)
            }
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }

    /**
     * 反地理编码
     * 从GPS坐标获取位置名称
     */
    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用新的 Geocoder API
                val geocoder = Geocoder(context, Locale.getDefault())
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    addresses.firstOrNull()?.let { address ->
                        // 优先返回城市+区名
                        val parts = mutableListOf<String>()
                        address.locality?.let { parts.add(it) }      // 城市
                        address.subLocality?.let { parts.add(it) }   // 区
                        if (parts.isNotEmpty()) {
                            parts.joinToString("")
                        } else {
                            address.getAddressLine(0)
                        }
                    }
                }
            } else {
                // 旧版本 Geocoder
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.let { address ->
                    val parts = mutableListOf<String>()
                    address.locality?.let { parts.add(it) }
                    address.subLocality?.let { parts.add(it) }
                    if (parts.isNotEmpty()) {
                        parts.joinToString("")
                    } else {
                        address.getAddressLine(0)
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.logThrowable("Exif", e, "获取地址失败")
            null
        }
    }

    /**
     * 格式化白平衡
     */
    private fun formatWhiteBalance(value: Int): String? {
        return when (value) {
            ExifInterface.WHITE_BALANCE_AUTO -> "自动"
            ExifInterface.WHITE_BALANCE_MANUAL -> "手动"
            else -> null
        }
    }

    /**
     * 格式化小数（保留1位）
     */
    private fun formatDecimal(value: Double): String {
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            "%.1f".format(value)
        }
    }

    /**
     * 获取后备数据（无EXIF时）
     */
    private fun getFallbackData(): ExifWatermarkData {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return ExifWatermarkData(
            make = Build.MANUFACTURER,
            model = Build.MODEL,
            fullDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            aperture = null,
            shutterSpeed = null,
            iso = null,
            focalLength = null,
            exposureTime = null,
            dateTaken = currentDate,
            timeTaken = currentTime,
            fullDateTime = "$currentDate $currentTime",
            gpsLat = null,
            gpsLng = null,
            locationName = null,
            lensModel = null,
            flashUsed = null,
            imageWidth = null,
            imageHeight = null,
            orientation = null,
            whiteBalance = null,
            focalLength35mm = null,
            software = null,
            artist = null
        )
    }

    /**
     * 根据图层类型填充内容
     */
    fun fillLayerContent(
        layerType: WatermarkLayerType,
        exifData: ExifWatermarkData
    ): String {
        return when (layerType) {
            WatermarkLayerType.BRAND -> "HASSELBLAD"
            WatermarkLayerType.DEVICE -> exifData.getFullDevice()
            WatermarkLayerType.PARAMS -> exifData.getFormattedParams()
            WatermarkLayerType.TIMESTAMP -> exifData.fullDateTime ?: exifData.getFormattedDate()
            WatermarkLayerType.LOCATION -> exifData.locationName ?: "位置信息不可用"
            WatermarkLayerType.TEXT -> ""
            WatermarkLayerType.LOGO -> ""
            WatermarkLayerType.SHAPE -> ""
            WatermarkLayerType.VIGNETTE -> ""
        }
    }
}

/**
 * 智能颜色适配系统
 * 
 * 功能：
 * - 分析水印区域亮度
 * - 推荐最佳文字颜色
 * - 自动调整阴影效果
 */
object SmartWatermarkColor {

    /**
     * 分析预览图水印区域的亮度，推荐最佳文字颜色
     */
    fun recommendColor(bitmap: Bitmap, position: WatermarkPosition): Int {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        return when {
            avgLuminance > 0.7f -> Color.BLACK   // 亮区用深色
            avgLuminance < 0.3f -> Color.WHITE   // 暗区用白色
            else -> Color.WHITE                   // 中间调默认白色 + 阴影
        }
    }

    /**
     * 推荐颜色并返回颜色名称
     */
    fun recommendColorWithName(bitmap: Bitmap, position: WatermarkPosition): Pair<Int, String> {
        val color = recommendColor(bitmap, position)
        val name = if (color == Color.BLACK) "黑色" else "白色"
        return Pair(color, name)
    }

    /**
     * 推荐是否启用阴影
     */
    fun recommendShadow(bitmap: Bitmap, position: WatermarkPosition): Boolean {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        // 中间调和高对比度场景建议启用阴影
        return avgLuminance >= 0.3f && avgLuminance <= 0.7f
    }

    /**
     * 推荐阴影模糊度
     */
    fun recommendShadowBlur(bitmap: Bitmap, position: WatermarkPosition): Float {
        val region = sampleRegion(bitmap, position)
        val avgLuminance = calculateLuminance(region)

        // 亮度越接近中间值，阴影越强
        val distanceFromMiddle = abs(avgLuminance - 0.5f)
        return (6f - distanceFromMiddle * 8f).coerceIn(2f, 8f)
    }

    /**
     * 采样水印所在区域（占画面 20%）
     */
    private fun sampleRegion(bitmap: Bitmap, position: WatermarkPosition): IntArray {
        val w = bitmap.width / 5
        val h = bitmap.height / 5

        val (x, y) = when (position) {
            WatermarkPosition.BOTTOM_LEFT -> Pair(0, bitmap.height - h)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(bitmap.width - w, bitmap.height - h)
            WatermarkPosition.BOTTOM -> Pair((bitmap.width - w) / 2, bitmap.height - h)
            WatermarkPosition.TOP_LEFT -> Pair(0, 0)
            WatermarkPosition.TOP_RIGHT -> Pair(bitmap.width - w, 0)
            WatermarkPosition.TOP -> Pair((bitmap.width - w) / 2, 0)
            WatermarkPosition.CENTER_LEFT -> Pair(0, (bitmap.height - h) / 2)
            WatermarkPosition.CENTER -> Pair((bitmap.width - w) / 2, (bitmap.height - h) / 2)
            WatermarkPosition.CENTER_RIGHT -> Pair(bitmap.width - w, (bitmap.height - h) / 2)
            WatermarkPosition.CUSTOM -> Pair(0, bitmap.height - h) // 默认左下
        }

        // 确保采样区域在图片范围内
        val safeX = x.coerceIn(0, bitmap.width - w)
        val safeY = y.coerceIn(0, bitmap.height - h)
        val safeW = w.coerceAtMost(bitmap.width - safeX)
        val safeH = h.coerceAtMost(bitmap.height - safeY)

        val pixels = IntArray(safeW * safeH)
        bitmap.getPixels(pixels, 0, safeW, safeX, safeY, safeW, safeH)
        return pixels
    }

    /**
     * 计算亮度平均值
     */
    private fun calculateLuminance(pixels: IntArray): Float {
        if (pixels.isEmpty()) return 0.5f

        var totalLum = 0f
        for (pixel in pixels) {
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f
            // 使用标准亮度公式
            totalLum += 0.299f * r + 0.587f * g + 0.114f * b
        }
        return totalLum / pixels.size
    }

    /**
     * 分析图片整体色调
     */
    fun analyzeOverallTone(bitmap: Bitmap): ToneAnalysis {
        val sampleSize = 1000
        val stepX = max(1, bitmap.width / 50)
        val stepY = max(1, bitmap.height / 50)

        var totalBrightness = 0f
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var sampleCount = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                val brightness = 0.299f * r + 0.587f * g + 0.114f * b

                totalBrightness += brightness
                totalR += r
                totalG += g
                totalB += b
                sampleCount++
            }
        }

        val avgBrightness = if (sampleCount > 0) totalBrightness / sampleCount else 0.5f
        val avgR = if (sampleCount > 0) totalR / sampleCount else 0.5f
        val avgG = if (sampleCount > 0) totalG / sampleCount else 0.5f
        val avgB = if (sampleCount > 0) totalB / sampleCount else 0.5f

        // 判断色调
        val isWarm = avgR > avgB + 0.1f
        val isCool = avgB > avgR + 0.1f
        val isNeutral = !isWarm && !isCool

        return ToneAnalysis(
            avgBrightness = avgBrightness,
            avgR = avgR,
            avgG = avgG,
            avgB = avgB,
            isWarm = isWarm,
            isCool = isCool,
            isNeutral = isNeutral,
            recommendedColor = if (avgBrightness > 0.6f) Color.BLACK else Color.WHITE
        )
    }

    /**
     * 色调分析结果
     */
    data class ToneAnalysis(
        val avgBrightness: Float,      // 平均亮度 (0-1)
        val avgR: Float,               // 平均红色分量
        val avgG: Float,               // 平均绿色分量
        val avgB: Float,               // 平均蓝色分量
        val isWarm: Boolean,           // 是否暖色调
        val isCool: Boolean,           // 是否冷色调
        val isNeutral: Boolean,        // 是否中性色调
        val recommendedColor: Int      // 推荐水印颜色
    )
}