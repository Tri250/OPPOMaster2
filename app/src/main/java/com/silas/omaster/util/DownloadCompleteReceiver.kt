package com.silas.omaster.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * 下载完成广播接收器
 * 用于处理应用内更新下载完成后的安装提示
 * 
 * 注意：此类在 UpdateChecker.kt 中也有定义，这里提供独立文件版本
 * 以确保 AndroidManifest.xml 中的引用正确
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DownloadCompleteReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return
        
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        cursor.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        // 获取本地文件路径
                        val localUriString = it.getString(
                            it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                        )
                        Log.d(TAG, "下载完成，URI: $localUriString")

                        val apkFile = if (localUriString != null) {
                            val localUri = Uri.parse(localUriString)
                            if (localUri.scheme == "file") {
                                File(localUri.path ?: localUriString.removePrefix("file://"))
                            } else {
                                getFileFromContentUri(context, localUri)
                            }
                        } else {
                            // 备用方案：直接找已知文件名
                            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            File(downloadDir, "app-universal-release.apk")
                        }

                        if (apkFile != null && apkFile.exists()) {
                            installApk(context, apkFile)
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        Log.e(TAG, "下载失败，原因: $reason")
                    }
                }
            }
        }
    }

    private fun getFileFromContentUri(context: Context, uri: Uri): File? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        val displayName = cursor.getString(displayNameIndex)
                        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        File(downloadDir, displayName)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析文件路径失败", e)
            null
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            Log.d(TAG, "准备安装 APK: ${apkFile.absolutePath}, 大小: ${apkFile.length()}")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                } else {
                    Uri.fromFile(apkFile)
                }

                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "已启动安装界面")
            } else {
                Log.e(TAG, "没有找到可以处理安装的应用")
            }
        } catch (e: Exception) {
            Log.e(TAG, "安装失败: ${e.message}", e)
        }
    }
}
