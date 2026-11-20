package com.silas.omaster.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * MU-22 增强 EXIF 信息（v1.6）
 * 新增：光圈 / 焦距 / ISO / 总像素估算 / Hasselblad 品牌标记 / 2 亿像素判断。
 */
data class ExifInfo(
    val dateTime: String?,
    val make: String?,
    val model: String?,
    // ═══════════════════════════════════════════════════════════
    // MU-22 新增字段
    // ═══════════════════════════════════════════════════════════
    val aperture: String? = null,           // 光圈，如 "f/1.6"
    val focalLength: String? = null,        // 焦距，如 "23mm"
    val iso: String? = null,
    val sensorTotalPixels: Int = 0,         // 总像素估算（单位：MP，如 200 = 2亿）
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val hasHasselblad: Boolean = false,     // 是否 Hasselblad 系统
    val is200Mp: Boolean = false,           // 是否 2 亿像素

    // ═══════════════════════════════════════════════════════════
    // P1-PHOTO-RECOGNITION：照片参数识别新增字段（FR-01）
    // ═══════════════════════════════════════════════════════════
    val shutterSpeed: String? = null,       // 快门，如 "1/250"
    val whiteBalance: String? = null,       // 白平衡文字，如 "日光"/"自动"
    val exposureBias: String? = null,       // 曝光补偿，如 "+0.7EV"
    val colorTemperature: Int? = null,      // 色温 K（由白平衡或像素估算）
    val flash: String? = null,              // 闪光灯，如 "开"/"关"
    /** 是否含 EXIF 摄影参数（至少一项摄影字段非空） */
    val hasPhotoParams: Boolean = false
)

object ExifReader {

    private const val TAG = "ExifReader"

    // Hasselblad 关联品牌/型号关键词（小写匹配）
    private val HASSELBLAD_KEYWORDS = listOf("hasselblad", "hasselblad camera", "oppox10", "oppox9", "oppox8", "oneplus13", "oneplus12")
    private val HASSELBLAD_MAKE_KEYWORDS = listOf("oppo", "hasselblad")

    suspend fun read(context: Context, uri: Uri): ExifInfo = withContext(Dispatchers.IO) {
        try {
            val exif = try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                if (fd != null) {
                    ExifInterface(fd.fileDescriptor)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream)
                    } ?: return@withContext ExifInfo(null, null, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open ExifInterface", e)
                return@withContext ExifInfo(null, null, null)
            }

            val dateTime = readDateTime(exif)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)

            // ═══════════════════════════════════════════════════════════
            // MU-22：解析光圈 / 焦距 / ISO / 分辨率 / Hasselblad / 2亿像素
            // ═══════════════════════════════════════════════════════════
            val aperture = readAperture(exif)
            val focalLength = readFocalLength(exif)
            val iso = readIso(exif)
            val (width, height) = readImageDimensions(exif)
            val sensorTotalPixels = (width.toLong() * height.toLong() / 1_000_000).toInt()
            val hasHasselblad = detectHasselblad(make, model)
            val is200Mp = sensorTotalPixels >= 200

            // P1-PHOTO-RECOGNITION：新增摄影参数读取
            val shutterSpeed = readShutterSpeed(exif)
            val whiteBalance = readWhiteBalance(exif)
            val exposureBias = readExposureBias(exif)
            val colorTemperature = readColorTemperature(exif)
            val flash = readFlash(exif)
            val hasPhotoParams = listOf(aperture, focalLength, iso, shutterSpeed,
                whiteBalance, exposureBias, colorTemperature?.toString()).any { !it.isNullOrBlank() }

            ExifInfo(
                dateTime = dateTime,
                make = make,
                model = model,
                aperture = aperture,
                focalLength = focalLength,
                iso = iso,
                sensorTotalPixels = sensorTotalPixels,
                imageWidth = width,
                imageHeight = height,
                hasHasselblad = hasHasselblad,
                is200Mp = is200Mp,
                shutterSpeed = shutterSpeed,
                whiteBalance = whiteBalance,
                exposureBias = exposureBias,
                colorTemperature = colorTemperature,
                flash = flash,
                hasPhotoParams = hasPhotoParams
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF", e)
            ExifInfo(null, null, null)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MU-22 辅助解析方法
    // ═══════════════════════════════════════════════════════════

    private fun readAperture(exif: ExifInterface): String? {
        // 优先标准 FNumber 标签（相机直接写入的 f 值，如 "1.8"）；
        // 用字符串字面量而非 ExifInterface.TAG_F_NUMBER，兼容各 exifinterface 版本的常量差异。
        val fNumber = exif.getAttribute("FNumber")?.toFloatOrNull()
        if (fNumber != null && fNumber in 0.7f..64f) {
            return "f/${String.format(Locale.getDefault(), "%.1f", fNumber)}"
        }
        // 回退：APEX ApertureValue（EV 制）按 N = 2^(EV/2) 换算为 f 值。
        // 历史缺陷：曾把 APEX 原始值（f/1.6 ≈ EV 1.36）直接当 f 值展示为 "f/1.4"。
        return PhotoParamParser.apexToAperture(exif.getAttribute("ApertureValue"))
    }

    private fun readFocalLength(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        return raw?.let { if (it.endsWith("mm", true)) it else "${it}mm" }
    }

    private fun readIso(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
    }

    // ═══════════════════════════════════════════════════════════
    // P1-PHOTO-RECOGNITION：新增摄影参数辅助方法
    // 注意：只读取摄影相关 tag，绝不读取 TAG_GPS_* / TAG_SERIAL_NUMBER 等 PII。
    // ═══════════════════════════════════════════════════════════

    private fun readShutterSpeed(exif: ExifInterface): String? {
        // 优先标准曝光时间，缺失时用 APEX 快门值反推
        val direct = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        val formatted = PhotoParamParser.formatShutter(direct)
        if (formatted != null) return formatted
        val apex = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
        return PhotoParamParser.apexToShutterSeconds(apex)
    }

    private fun readWhiteBalance(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
            ?: exif.getAttribute(ExifInterface.TAG_LIGHT_SOURCE)
        return PhotoParamParser.normalizeWhiteBalance(raw)
    }

    private fun readExposureBias(exif: ExifInterface): String? {
        return PhotoParamParser.formatExposureBias(exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE))
    }

    private fun readColorTemperature(exif: ExifInterface): Int? {
        // OPPO/哈苏样片常把色温写在 Make/Model 之外；尝试 APEX 无直接色温 tag，
        // 这里读部分厂商写入的自定义字段（大小写兼容），缺失返回 null（后续由像素风格引擎估算）。
        val candidates = listOf(
            exif.getAttribute("ColorTemperature"),
            exif.getAttribute("WhiteBalanceTemperature")
        )
        return candidates.firstNotNullOfOrNull { PhotoParamParser.parseColorTemp(it) }
    }

    private fun readFlash(exif: ExifInterface): String? {
        val raw = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
        return when (raw) {
            0x0 -> "关"
            0x1, 0x5, 0x7, 0x9, 0xD, 0xF -> "开"
            -1 -> null
            else -> null
        }
    }

    private fun readImageDimensions(exif: ExifInterface): Pair<Int, Int> {
        val w = runCatching { exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.toIntOrNull() }.getOrNull() ?: 0
        val h = runCatching { exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.toIntOrNull() }.getOrNull() ?: 0
        return w to h
    }

    private fun detectHasselblad(make: String?, model: String?): Boolean {
        val lowerMake = (make ?: "").lowercase()
        val lowerModel = (model ?: "").lowercase()
        if (HASSELBLAD_MAKE_KEYWORDS.any { lowerMake.contains(it) }) {
            if (lowerMake.contains("hasselblad")) return true
            return HASSELBLAD_KEYWORDS.any { lowerModel.contains(it) } ||
                    lowerModel.contains("find x10") ||
                    lowerModel.contains("find x9") ||
                    lowerModel.contains("find x8") ||
                    lowerModel.contains("oneplus 13") ||
                    lowerModel.contains("oneplus13")
        }
        return false
    }

    private fun readDateTime(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            ?: return null

        return try {
            val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(raw)
            val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            raw.replace(":", ".").take(16)
        }
    }
}
