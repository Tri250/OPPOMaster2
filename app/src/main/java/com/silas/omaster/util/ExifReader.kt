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
    val is200Mp: Boolean = false           // 是否 2 亿像素
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
                is200Mp = is200Mp
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
        // TAG_APERTURE 在 androidx.exifinterface < 1.5.0 中不存在（我们用的 1.4.1），
        // 直接用 EXIF 标准标签字符串 "ApertureValue"（所有 ExifInterface 版本都支持字符串）
        val raw = exif.getAttribute("ApertureValue")
        return runCatching {
            raw?.toFloatOrNull()?.let { "f/${String.format(Locale.getDefault(), "%.1f", it)}" }
        }.getOrNull()
    }

    private fun readFocalLength(exif: ExifInterface): String? {
        val raw = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        return raw?.let { if (it.endsWith("mm", true)) it else "${it}mm" }
    }

    private fun readIso(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
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
