package com.silas.omaster.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.silas.omaster.data.local.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.silas.omaster.R

/**
 * 更新检查工具
 * 支持 GitHub 和 Gitee 双渠道更新检查
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    /** 更新 APK 固定文件名（与 CI 产物名、下载目标目录一致，用于下载完成校验） */
    const val UPDATE_APK_NAME = "app-universal-release.apk"

    // GitHub 配置
    // 必须与本仓库 CI 发布 Release 的仓库一致（.github/workflows/release.yml 将签名 APK
    // 发布到 github.repository）。若指向其他仓库，将下载到签名不匹配的 APK 导致安装失败。
    private const val GITHUB_OWNER = "Tri250"
    private const val GITHUB_REPO = "OPPOMaster2"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // Gitee 配置
    private const val GITEE_OWNER = "qiublog"
    private const val GITEE_REPO = "OMaster"
    private const val GITEE_API_URL = "https://gitee.com/api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases/latest"

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val releaseNotes: String,
        val isNewer: Boolean
    )

    /**
     * 检查更新（根据渠道选择）
     * @param context 上下文
     * @param currentVersionCode 当前版本号
     * @param channel 更新渠道，默认 Gitee
     * @return 更新信息，失败返回 null
     */
    suspend fun checkUpdate(
        context: Context,
        currentVersionCode: Int,
        channel: UpdateChannel = UpdateChannel.GITEE
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        return@withContext when (channel) {
            UpdateChannel.GITEE -> checkGiteeUpdate(context, currentVersionCode)
            UpdateChannel.GITHUB -> checkGithubUpdate(context, currentVersionCode)
        }
    }

    /**
     * Gitee 更新检查
     */
    private suspend fun checkGiteeUpdate(context: Context, currentVersionCode: Int): UpdateInfo? {
        return checkUpdateFromApi(context, currentVersionCode, GITEE_API_URL, isGitee = true)
    }

    /**
     * GitHub 更新检查
     */
    private suspend fun checkGithubUpdate(context: Context, currentVersionCode: Int): UpdateInfo? {
        return checkUpdateFromApi(context, currentVersionCode, GITHUB_API_URL, isGitee = false)
    }

    /**
     * 通用 API 检查逻辑
     */
    private fun checkUpdateFromApi(
        context: Context,
        currentVersionCode: Int,
        apiUrl: String,
        isGitee: Boolean
    ): UpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                // GitHub 需要特殊请求头
                if (!isGitee) {
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }
            }
            connection = conn

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val tagName = json.getString("tag_name")
                val versionName = tagName.removePrefix("v")
                val versionCode = VersionInfo.parseVersionCode(versionName)

                // 获取 app-universal-release.apk 下载链接
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.getString("name")
                    // 两个渠道都使用固定文件名
                    if (assetName == "app-universal-release.apk") {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                val releaseNotes = json.optString("body", context.getString(R.string.no_release_notes))

                UpdateInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = versionCode > currentVersionCode && downloadUrl.isNotEmpty()
                )
            } else {
                Log.e(TAG, "检查更新失败，HTTP 状态码: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新出错 [${if (isGitee) "Gitee" else "GitHub"}]", e)
            null
        } finally {
            // 释放底层连接：频繁自动检查更新时若不 disconnect，会持续占用文件描述符/TCP 连接
            connection?.disconnect()
        }
    }

    /**
     * 是否已获得"安装未知应用"权限。
     *
     * Android 8.0(API 26) 起 REQUEST_INSTALL_PACKAGES 属于需用户在系统设置中手动授予的特殊权限，
     * 未授予时系统安装器不会响应 ACTION_VIEW(apk)，表现为"下载完成后无任何反应、无法安装"。
     */
    fun canRequestInstall(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * 跳转到本应用的"安装未知应用"授权页，引导用户开启安装权限。
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { Log.e(TAG, "打开安装未知应用设置失败", it) }
    }

    /**
     * 使用系统 DownloadManager 下载并安装
     * @return 下载任务 ID，用于查询进度
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, versionName: String): Long {
        // 【v1.6.13 安全加固】仅允许 https 下载源，防止外部传入明文链接导致 APK 被中间人替换
        if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
            Log.e(TAG, "拒绝非 https 下载地址: $downloadUrl")
            return -1L
        }

        val fileName = UPDATE_APK_NAME

        // 安装前置：Android 8.0+ 未授予"安装未知应用"权限时，系统安装器不会响应安装 Intent。
        // 在下载开始前引导用户开启（下载在后台继续，授权完成后即可正常拉起安装界面）。
        if (!canRequestInstall(context)) {
            openInstallPermissionSettings(context)
        }

        // 清理旧文件
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        File(downloadDir, fileName).delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("OMaster 更新")
            setDescription("正在下载 v$versionName...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // 【v1.6.13 兼容性修复】setDestinationInExternalFilesDir 为 API 24+ API，
            // minSdk 23 下直接调用会 NoSuchMethodError 崩溃；API 23 使用等价 setDestinationUri。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            } else {
                setDestinationUri(Uri.fromFile(File(downloadDir, fileName)))
            }
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    /**
     * 查询下载进度
     * @return Pair<下载状态, 进度百分比> 状态：1=等待中, 2=下载中, 4=完成, 8=失败, 16=暂停
     */
    fun queryDownloadProgress(context: Context, downloadId: Long): Pair<Int, Int> {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        var status = -1
        var progress = 0

        if (cursor.moveToFirst()) {
            status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

            when (status) {
                DownloadManager.STATUS_PENDING -> {
                    progress = 0
                }
                DownloadManager.STATUS_RUNNING -> {
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                }
                DownloadManager.STATUS_FAILED -> {
                    progress = -1
                }
                DownloadManager.STATUS_PAUSED -> {
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                }
            }
        }
        cursor.close()
        return Pair(status, progress)
    }

    /**
     * 取消下载
     */
    fun cancelDownload(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }
}

/**
 * 下载完成广播接收器（静态注册）
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // v1.6.15 稳定性修复：receiver exported=true 接收设备上所有 DOWNLOAD_COMPLETE 广播，
        // cursor 查询/列索引/URI 解析任一环节抛异常即崩溃。整体包 try-catch 兜底。
        try {
            handleDownloadComplete(context, intent)
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "处理下载完成广播失败", e)
        }
    }

    private fun handleDownloadComplete(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        cursor.use {
            if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // 获取本地文件路径
                val localUriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                Log.d("DownloadReceiver", "下载完成，URI: $localUriString")

                val apkFile = if (localUriString != null) {
                    val localUri = Uri.parse(localUriString)
                    if (localUri.scheme == "file") {
                        // 直接是文件路径（path 理论上非空，仍做空安全兜底）
                        localUri.path?.let { File(it) }
                    } else {
                        // content:// URI，尝试通过 ContentResolver 获取真实路径
                        getFileFromContentUri(context, localUri)
                    }
                } else {
                    // 备用方案：直接找已知文件名
                    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    File(downloadDir, UpdateChecker.UPDATE_APK_NAME)
                }

                // 归属校验：仅安装本应用的更新 APK，避免响应其他 App 下载完成广播而误装任意文件
                if (apkFile != null && apkFile.exists() && isOurUpdateApk(context, apkFile)) {
                    installApk(context, apkFile)
                } else {
                    Log.d("DownloadReceiver", "忽略非本应用更新下载: ${apkFile?.absolutePath}")
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                Log.e("DownloadReceiver", "下载失败，错误码: $reason")
            }
            }
        }
    }

    private fun getFileFromContentUri(context: Context, uri: Uri): File? {
        return try {
            // 对于 DownloadManager 下载的文件，通常可以直接从 URI 解析
            if (uri.path?.contains("/Android/data/") == true) {
                // 提取真实路径
                val path = uri.path
                if (path != null) {
                    File(path)
                } else null
            } else {
                // 备用：通过 ContentResolver 查询
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            val displayName = cursor.getString(displayNameIndex)
                            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            File(downloadDir, displayName)
                        } else null
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "解析文件路径失败", e)
            null
        }
    }

    /**
     * 校验下载完成文件是否为本应用的更新 APK：
     * 文件名匹配固定产物名，且位于本应用私有外部存储 Download 目录。
     * 避免静态广播接收器响应其他应用下载完成事件时误装任意 APK。
     */
    private fun isOurUpdateApk(context: Context, file: File): Boolean {
        if (file.name != UpdateChecker.UPDATE_APK_NAME) return false
        val expectedDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return if (expectedDir != null) {
            file.parentFile?.absolutePath == expectedDir.absolutePath
        } else {
            // 无法解析目录时退化为文件名匹配
            true
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        // 未授予"安装未知应用"权限时系统安装器不会响应安装 Intent，直接跳过避免无效操作。
        if (!UpdateChecker.canRequestInstall(context)) {
            Log.w("DownloadReceiver", "缺少安装未知应用权限，跳过自动安装（已在下载前引导用户授权）")
            return
        }
        try {
            Log.d("DownloadReceiver", "准备安装 APK: ${apkFile.absolutePath}, 大小: ${apkFile.length()}")

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            // Android 11(API 30)+ 包可见性会使 resolveActivity 对系统安装器返回 null，
            // 故不再以其作为门禁，直接启动并捕获 ActivityNotFoundException。
            try {
                context.startActivity(intent)
                Log.d("DownloadReceiver", "已启动安装界面")
            } catch (e: android.content.ActivityNotFoundException) {
                Log.e("DownloadReceiver", "没有找到可以处理安装的应用", e)
            }
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "安装失败: ${e.message}", e)
        }
    }
}
