package com.silas.omaster.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

object ImageExporter {

    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "omaster_${System.currentTimeMillis()}.jpg"
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/OMaster"
                    )
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                            return false
                        }
                    } ?: return false
                }
                uri != null
            } else {
                // API 23-28（Q 以下）：无 MediaStore RELATIVE_PATH。写公共 Pictures 目录需要
                // WRITE_EXTERNAL_STORAGE 运行时权限；未授权（或未声明）时改写应用专属外部目录
                // 并触发 MediaScanner 索引——保存必然成功，且对相册/Google Photos 可见。
                @Suppress("DEPRECATION")
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val hasPublicPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                // getExternalFilesDir 在外部存储不可用时可能为 null，逐级回退保证不崩
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.getExternalFilesDir(null)
                    ?: context.cacheDir
                val dir = File(if (hasPublicPermission) publicDir else appDir, "OMaster")
                    .apply { mkdirs() }
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                        return false
                    }
                }
                if (!hasPublicPermission) {
                    // 应用专属目录不会被系统自动扫描，手动索引进媒体库
                    MediaScannerConnection.scanFile(
                        context.applicationContext,
                        arrayOf(file.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                }
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageExporter", "Failed to save image", e)
            false
        }
    }
}
