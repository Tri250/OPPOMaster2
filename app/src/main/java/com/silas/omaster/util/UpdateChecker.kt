package com.silas.omaster.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.silas.omaster.BuildConfig
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.util.UrlConstants
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import com.silas.omaster.R

/**
 * 更新检查工具
 * 支持 GitHub 和 Gitee 双渠道更新检查
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    // GitHub 配置
    private const val GITHUB_API_URL = UrlConstants.GITHUB_API_RELEASES

    // Gitee 配置
    private const val GITEE_API_URL = UrlConstants.GITEE_API_RELEASES

    // Ktor HTTP 客户端（复用，与项目其他网络请求一致）
    private val ktorClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay(base = 2.0, maxDelayMs = 4_000L)
            }
            expectSuccess = false
        }
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

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
     * 通用 API 检查逻辑（使用 Ktor 替代 HttpURLConnection）
     */
    private suspend fun checkUpdateFromApi(
        context: Context,
        currentVersionCode: Int,
        apiUrl: String,
        isGitee: Boolean
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val response = ktorClient.get(apiUrl) {
                if (!isGitee) {
                    header("Accept", "application/vnd.github.v3+json")
                }
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val json = jsonParser.parseToJsonElement(body).jsonObject

                val tagName = json["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
                val versionName = tagName.removePrefix("v")
                val versionCode = VersionInfo.parseVersionCode(versionName)

                // 获取 app-universal-release.apk 下载链接
                val assets = json["assets"]?.jsonArray ?: return@withContext null
                var downloadUrl = ""
                for (asset in assets) {
                    val assetObj = asset.jsonObject
                    val assetName = assetObj["name"]?.jsonPrimitive?.content ?: continue
                    if (assetName == "app-universal-release.apk") {
                        downloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content ?: ""
                        break
                    }
                }

                val releaseNotes = json["body"]?.jsonPrimitive?.content
                    ?: context.getString(R.string.no_release_notes)

                return@withContext UpdateInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = versionCode > currentVersionCode && downloadUrl.isNotEmpty()
                )
            } else {
                Log.e(TAG, "检查更新失败，HTTP 状态码: ${response.status}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新出错 [${if (isGitee) "Gitee" else "GitHub"}]", e)
            return@withContext null
        }
    }

    /**
     * 使用系统 DownloadManager 下载并安装
     * @return 下载任务 ID，用于查询进度
     */
    fun downloadAndInstall(context: Context, downloadUrl: String, versionName: String): Long {
        val fileName = "app-universal-release.apk"

        // 清理旧文件
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        File(downloadDir, fileName).delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("OMaster 更新")
            setDescription("正在下载 v$versionName...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
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

        cursor.use {
            if (it.moveToFirst()) {
                status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                when (status) {
                    DownloadManager.STATUS_PENDING -> {
                        progress = 0
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 100
                    }
                    DownloadManager.STATUS_FAILED -> {
                        progress = -1
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    }
                }
            }
        }
        return Pair(status, progress)
    }

    /**
     * 取消下载
     */
    fun cancelDownload(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }

    // SHA-256 计算缓冲区大小（提取为常量）
    private const val SHA_BUFFER_SIZE = 8192

    /**
     * 计算文件的 SHA-256 哈希值
     * @param file 目标文件
     * @return SHA-256 哈希字符串（小写十六进制）
     */
    fun calculateFileSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(SHA_BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "计算文件 SHA-256 失败", e)
            null
        }
    }

    /**
     * 获取当前安装 APK 的签名证书 SHA-256 指纹
     * @param context 上下文
     * @return 签名指纹字符串，失败返回 null
     */
    fun getCurrentAppSignature(context: Context): String? {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures
            }

            signatures?.firstOrNull()?.let { signature ->
                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(
                    java.io.ByteArrayInputStream(signature.toByteArray())
                ) as X509Certificate

                val digest = MessageDigest.getInstance("SHA-256")
                val certHash = digest.digest(cert.encoded)
                certHash.joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取应用签名失败", e)
            null
        }
    }

    /**
     * 验证 APK 文件的签名是否与当前应用一致
     * @param context 上下文
     * @param apkFile 待验证的 APK 文件
     * @return 验证结果：true=签名一致，false=签名不一致或验证失败
     */
    fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            // 获取当前应用的签名
            val currentSignature = getCurrentAppSignature(context)
            if (currentSignature == null) {
                Log.e(TAG, "无法获取当前应用签名")
                return false
            }

            // 获取 APK 文件的签名
            val packageManager = context.packageManager
            val packageArchiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                )
            }

            val apkSignatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageArchiveInfo?.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageArchiveInfo?.signatures
            }

            val apkSignature = apkSignatures?.firstOrNull()?.let { signature ->
                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(
                    java.io.ByteArrayInputStream(signature.toByteArray())
                ) as X509Certificate

                val digest = MessageDigest.getInstance("SHA-256")
                val certHash = digest.digest(cert.encoded)
                certHash.joinToString("") { "%02x".format(it) }
            }

            if (apkSignature == null) {
                Log.e(TAG, "无法获取 APK 文件签名")
                return false
            }

            // 比较签名
            val isValid = currentSignature.equals(apkSignature, ignoreCase = true)
            if (isValid) {
                Log.i(TAG, "APK 签名验证通过")
            } else {
                Log.e(TAG, "APK 签名验证失败：签名不匹配")
                // 安全日志：不输出具体签名值，防止信息泄露
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "当前应用签名: $currentSignature")
                    Log.d(TAG, "APK 文件签名: $apkSignature")
                }
            }
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "验证 APK 签名时出错", e)
            false
        }
    }

    /**
     * 安全安装 APK（带签名验证）
     * @param context 上下文
     * @param apkFile APK 文件
     * @param skipSignatureVerify 是否跳过签名验证（仅用于调试）
     * @return 是否成功启动安装
     */
    fun installApkSecurely(
        context: Context,
        apkFile: File,
        skipSignatureVerify: Boolean = false
    ): Boolean {
        // 验证签名
        if (!skipSignatureVerify) {
            if (!verifyApkSignature(context, apkFile)) {
                Log.e(TAG, "APK 签名验证失败，拒绝安装")
                // 删除不安全的文件
                apkFile.delete()
                return false
            }
        } else {
            Log.w(TAG, "跳过签名验证（调试模式）")
        }

        // 计算并记录文件哈希
        val fileHash = calculateFileSha256(apkFile)
        Log.i(TAG, "APK 文件 SHA-256: $fileHash")

        return try {
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
                Log.i(TAG, "已启动安装界面")
                true
            } else {
                Log.e(TAG, "没有找到可以处理安装的应用")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "安装失败: ${e.message}", e)
            false
        }
    }
}

/**
 * 下载完成广播接收器（静态注册）
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        cursor.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // 获取本地文件路径
                    val localUriString = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    Log.d("DownloadReceiver", "下载完成，URI: $localUriString")

                    val apkFile = if (localUriString != null) {
                        val localUri = Uri.parse(localUriString)
                        if (localUri.scheme == "file") {
                            // 直接是文件路径
                            File(localUri.path ?: localUriString.removePrefix("file://"))
                        } else {
                            // content:// URI，尝试通过 ContentResolver 获取真实路径
                            getFileFromContentUri(context, localUri)
                        }
                    } else {
                        // 备用方案：直接找已知文件名
                        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        File(downloadDir, "app-universal-release.apk")
                    }

                    if (apkFile != null && apkFile.exists()) {
                        // 使用带签名验证的安全安装
                        val installSuccess = UpdateChecker.installApkSecurely(context, apkFile)
                        if (!installSuccess) {
                            Log.e("DownloadReceiver", "APK 安装失败：签名验证未通过或安装出错")
                        }
                    } else {
                        Log.e("DownloadReceiver", "APK 文件不存在")
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
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

}
