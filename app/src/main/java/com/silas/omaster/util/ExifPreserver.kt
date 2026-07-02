package com.silas.omaster.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.silas.omaster.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * EXIF 元数据保留工具
 *
 * 在图像处理（AI 微调、智能优化、哈苏色彩）后回写原始 EXIF 元数据，
 * 确保 GPS 位置、相机型号、光圈、快门、ISO 等摄影参数不丢失。
 *
 * 使用方式：
 * ```kotlin
 * val sourceUri = ... // 原始图片 URI
 * val processedBitmap = ... // 处理后的 Bitmap
 * val savedUri = ExifPreserver.saveWithExif(context, processedBitmap, sourceUri)
 * ```
 */
object ExifPreserver {

    private const val TAG = "ExifPreserver"
    private const val EXPORT_QUALITY = 95

    /**
     * 保存处理后的图片并保留原始 EXIF 元数据
     *
     * @param context 上下文
     * @param bitmap 处理后的 Bitmap
     * @param sourceUri 原始图片 URI（用于提取 EXIF）
     * @param targetFileName 目标文件名，默认使用时间戳
     * @param targetRelativePath 目标相对路径，默认 Pictures/OMaster
     * @return 保存后的 MediaStore URI，失败时返回 null
     */
    fun saveWithExif(
        context: Context,
        bitmap: Bitmap,
        sourceUri: Uri? = null,
        targetFileName: String? = null,
        targetRelativePath: String = Environment.DIRECTORY_PICTURES + "/OMaster",
        customDeviceModel: String? = null
    ): Uri? {
        val filename = targetFileName
            ?: "OMaster_${System.currentTimeMillis()}.jpg"

        // 1. 提取原始 EXIF 数据
        val exifAttributes = sourceUri?.let { extractExif(context, it) } ?: mutableMapOf()

        // 1.5 自定义设备型号覆盖 EXIF Model/Make（UC-09）
        if (!customDeviceModel.isNullOrBlank()) {
            exifAttributes[ExifInterface.TAG_MODEL] = customDeviceModel
            // 从自定义型号提取厂商部分（如 "Hasselblad 907X" → "Hasselblad"）
            val makePart = customDeviceModel.substringBefore(' ').trim()
            if (makePart.isNotEmpty()) {
                exifAttributes[ExifInterface.TAG_MAKE] = makePart
            }
        }

        // 2. 保存 Bitmap 到临时文件
        val tempFile = try {
            saveBitmapToTempFile(context, bitmap, filename)
        } catch (e: IOException) {
            Log.e(TAG, "保存临时文件失败", e)
            return null
        }

        // 3. 回写 EXIF 到临时文件
        if (exifAttributes.isNotEmpty()) {
            writeExif(tempFile, exifAttributes)
        }

        // 4. 写入 MediaStore
        return try {
            insertToMediaStore(context, tempFile, filename, targetRelativePath)
        } catch (e: IOException) {
            Log.e(TAG, "写入 MediaStore 失败", e)
            null
        } finally {
            // 清理临时文件
            tempFile.delete()
        }
    }

    /**
     * 从 URI 提取 EXIF 属性
     */
    private fun extractExif(context: Context, uri: Uri): MutableMap<String, String> {
        val attributes = mutableMapOf<String, String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(it)
                } else {
                    null
                }
            }
            inputStream?.close()

            exif?.let {
                // GPS 信息
                it.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { v ->
                    attributes[ExifInterface.TAG_GPS_LATITUDE] = v
                    attributes[ExifInterface.TAG_GPS_LATITUDE_REF] =
                        it.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) ?: "N"
                }
                it.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { v ->
                    attributes[ExifInterface.TAG_GPS_LONGITUDE] = v
                    attributes[ExifInterface.TAG_GPS_LONGITUDE_REF] =
                        it.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) ?: "E"
                }
                it.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let { v ->
                    attributes[ExifInterface.TAG_GPS_ALTITUDE] = v
                }

                // 相机参数
                it.getAttribute(ExifInterface.TAG_MAKE)?.let { v ->
                    attributes[ExifInterface.TAG_MAKE] = v
                }
                it.getAttribute(ExifInterface.TAG_MODEL)?.let { v ->
                    attributes[ExifInterface.TAG_MODEL] = v
                }
                it.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { v ->
                    attributes[ExifInterface.TAG_F_NUMBER] = v
                }
                it.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { v ->
                    attributes[ExifInterface.TAG_EXPOSURE_TIME] = v
                }
                it.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { v ->
                    attributes[ExifInterface.TAG_ISO_SPEED_RATINGS] = v
                }
                it.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { v ->
                    attributes[ExifInterface.TAG_FOCAL_LENGTH] = v
                }
                it.getAttribute(ExifInterface.TAG_FLASH)?.let { v ->
                    attributes[ExifInterface.TAG_FLASH] = v
                }

                // 时间信息
                it.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { v ->
                    attributes[ExifInterface.TAG_DATETIME_ORIGINAL] = v
                }
                it.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)?.let { v ->
                    attributes[ExifInterface.TAG_DATETIME_DIGITIZED] = v
                }

                // 方向
                it.getAttribute(ExifInterface.TAG_ORIENTATION)?.let { v ->
                    attributes[ExifInterface.TAG_ORIENTATION] = v
                }

                // 图像参数
                it.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let { v ->
                    attributes[ExifInterface.TAG_IMAGE_WIDTH] = v
                }
                it.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let { v ->
                    attributes[ExifInterface.TAG_IMAGE_LENGTH] = v
                }
                it.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let { v ->
                    attributes[ExifInterface.TAG_WHITE_BALANCE] = v
                }

                // 软件信息（标记由 OMaster 处理）
                attributes[ExifInterface.TAG_SOFTWARE] = "OMaster ${BuildConfig.VERSION_NAME}"

                Log.d(TAG, "已提取 ${attributes.size} 个 EXIF 属性")
            }
        } catch (e: IOException) {
            Log.e(TAG, "提取 EXIF 失败", e)
        }

        return attributes
    }

    /**
     * 将 EXIF 属性写入文件
     */
    private fun writeExif(file: File, attributes: Map<String, String>) {
        try {
            val exif = ExifInterface(file.absolutePath)
            for ((tag, value) in attributes) {
                try {
                    exif.setAttribute(tag, value)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "无法设置 EXIF 标签: $tag", e)
                }
            }
            exif.saveAttributes()
            Log.d(TAG, "已回写 ${attributes.size} 个 EXIF 属性")
        } catch (e: IOException) {
            Log.e(TAG, "写入 EXIF 失败", e)
        }
    }

    /**
     * 保存 Bitmap 到临时文件
     */
    private fun saveBitmapToTempFile(context: Context, bitmap: Bitmap, filename: String): File {
        val tempDir = File(context.cacheDir, "exif_temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tempFile = File(tempDir, filename)

        FileOutputStream(tempFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, EXPORT_QUALITY, outputStream)
        }

        return tempFile
    }

    /**
     * 写入 MediaStore
     */
    private fun insertToMediaStore(
        context: Context,
        file: File,
        filename: String,
        relativePath: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 标记写入完成
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Log.d(TAG, "图片已保存到 MediaStore: $uri")
            return uri
        } catch (e: IOException) {
            // 写入失败，删除已创建的记录
            resolver.delete(uri, null, null)
            throw e
        }
    }
}