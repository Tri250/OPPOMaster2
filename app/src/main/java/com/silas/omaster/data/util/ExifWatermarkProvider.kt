package com.silas.omaster.data.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.silas.omaster.data.model.ExifWatermarkData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * EXIF 数据提取工具
 * 从照片文件读取真实 EXIF 数据用于水印填充
 */
class ExifWatermarkProvider(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 从 Uri 提取 EXIF 数据
     */
    fun extractFromUri(uri: Uri): ExifWatermarkData {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                parseExif(exif)
            } ?: ExifWatermarkData()
        } catch (_: Exception) {
            ExifWatermarkData()
        }
    }

    /**
     * 从文件路径提取 EXIF 数据
     */
    fun extractFromPath(path: String): ExifWatermarkData {
        return try {
            val exif = ExifInterface(path)
            parseExif(exif)
        } catch (_: Exception) {
            ExifWatermarkData()
        }
    }

    /**
     * 解析 EXIF 数据
     */
    private fun parseExif(exif: ExifInterface): ExifWatermarkData {
        return ExifWatermarkData(
            make = exif.getAttribute(ExifInterface.TAG_MAKE)?.cleanExifString(),
            model = exif.getAttribute(ExifInterface.TAG_MODEL)?.cleanExifString(),
            aperture = formatAperture(exif.getAttribute(ExifInterface.TAG_F_NUMBER)),
            shutterSpeed = formatShutterSpeed(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)),
            iso = formatIso(exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)),
            focalLength = formatFocalLength(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)),
            dateTaken = formatDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME)),
            gpsLat = exif.latLong?.get(0),
            gpsLng = exif.latLong?.get(1),
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.cleanExifString(),
            flashUsed = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0) and 1 == 1,
            imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
            imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        )
    }

    /**
     * 格式化光圈值
     * "160/100" → "f/1.6"
     */
    private fun formatAperture(value: String?): String? {
        if (value == null) return null
        return try {
            val f = if (value.contains("/")) {
                val parts = value.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                value.toDouble()
            }
            "f/${"%.1f".format(f)}"
        } catch (_: Exception) {
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
            if (value.contains("/")) {
                val parts = value.split("/")
                val numerator = parts[0].toDouble()
                val denominator = parts[1].toDouble()
                val seconds = numerator / denominator
                
                if (seconds >= 1) {
                    "${seconds.toInt()}s"
                } else {
                    val fracDenom = (1.0 / seconds).roundToInt()
                    "1/${fracDenom}s"
                }
            } else {
                val seconds = value.toDouble()
                if (seconds >= 1) {
                    "${seconds.toInt()}s"
                } else {
                    val fracDenom = (1.0 / seconds).roundToInt()
                    "1/${fracDenom}s"
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 格式化 ISO
     * "100" → "ISO 100"
     */
    private fun formatIso(value: String?): String? {
        if (value.isNullOrEmpty()) return null
        return "ISO $value"
    }

    /**
     * 格式化焦距
     * "230/10" → "23mm"
     */
    private fun formatFocalLength(value: String?): String? {
        if (value == null) return null
        return try {
            val mm = if (value.contains("/")) {
                val parts = value.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            } else {
                value.toDouble()
            }
            "${mm.roundToInt()}mm"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 格式化日期时间
     * "2026:06:09 14:30:00" → "2026-06-09 14:30"
     */
    private fun formatDateTime(value: String?): String? {
        if (value == null) return null
        return try {
            // EXIF 日期格式: "YYYY:MM:DD HH:MM:SS"
            val cleaned = value.replace(":", "-").replaceFirst("-", ":")
            val parts = cleaned.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0].replace("-", ":")
                val timePart = parts[1].substring(0, minOf(5, parts[1].length))
                "$datePart $timePart"
            } else {
                value
            }
        } catch (_: Exception) {
            value
        }
    }

    /**
     * 清理 EXIF 字符串
     */
    private fun String.cleanExifString(): String {
        return this.trim().replace("\u0000", "")
    }

    /**
     * 获取当前系统时间
     */
    fun getCurrentTimestamp(): String {
        return dateFormat.format(Date())
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        val model = android.os.Build.MODEL
        return "$manufacturer $model"
    }
}

/**
 * 扩展函数：根据 EXIF 数据生成水印内容
 */
fun ExifWatermarkData.generateWatermarkContent(
    showDevice: Boolean = true,
    showParams: Boolean = true,
    showDate: Boolean = false
): String {
    val parts = mutableListOf<String>()
    
    if (showDevice) {
        val device = getDeviceInfo()
        if (device.isNotEmpty()) parts.add(device)
    }
    
    if (showParams) {
        val params = getParamsInfo()
        if (params.isNotEmpty()) parts.add(params)
    }
    
    if (showDate && !dateTaken.isNullOrEmpty()) {
        parts.add(dateTaken)
    }
    
    return parts.joinToString("\n")
}
