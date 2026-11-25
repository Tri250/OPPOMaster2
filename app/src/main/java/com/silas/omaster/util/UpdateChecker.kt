package com.silas.omaster.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.silas.omaster.data.local.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import com.silas.omaster.R

/**
 * 更新检查工具
 * 支持 GitHub 和 Gitee 双渠道更新检查
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    /** 更新 APK 固定文件名（与 CI 产物名、下载目标目录一致，用于下载完成校验） */
    const val UPDATE_APK_NAME = "app-universal-release.apk"

    // ---- P0-3 更新链路加固：预期 APK 哈希经本地持久化传递给下载完成接收器 ----
    private const val UPDATE_PREFS = "update_checker"
    private const val KEY_PENDING_APK_SHA256 = "pending_apk_sha256"

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
        val isNewer: Boolean,
        /** 提醒热更新标识（【v1.5.6】本地低于基线的存量版本且远端有可用更新）：
         *  弹窗保持可关闭（back/外部点击/稍后均可用），仅正文顶部追加警示文案，
         *  且不受"自动检查更新"开关门控（runAutoUpdateCheck 分流） */
        val forceUpdate: Boolean = false,
        /** 【P0-3 更新链路加固】更新 APK 的 SHA-256（Release 正文 update-sha256 标记下发，
         *  经 [parseUpdateSha256] 解析）。下载开始时写入本地，下载完成后由
         *  DownloadCompleteReceiver 做内容哈希比对，一致才允许安装。
         *  可空：旧 Release 未携带标记时跳过哈希校验（签名一致性校验仍强制执行）。 */
        val sha256: String? = null
    )

    /**
     * 提醒热更新基线（客户端默认兜底）：低于此版本的存量用户必收到提醒（警示文案）。
     * 背景：v1.5.6 的订阅全源强制刷新与 POP 即拍数据管线修复（快门分数解析
     * 曝光 3 倍错算、EV 后缀写法静默丢失等）均为关键修复，旧版本预设数据与
     * 拍摄链路存在已知缺陷且无自行修复渠道。
     *
     * 【服务端下发】基线可由最新 Release 正文动态调整（无需发版）：正文写入
     * `force-update-baseline: 1.5.6`（或 HTML 注释 `<!-- force-update-baseline: 1.5.6 -->`，
     * GitHub 渲染不可见）；无标记时回退本常量。写极低版本号（如 0.0.0）等效关闭提醒，
     * 上调基线则扩大强更覆盖面（如 1.9.0 → v1.8.9 存量用户也纳入提醒）。
     */
    const val FORCE_UPDATE_MIN = "1.5.6"

    /** Release 正文中的基线下发标记（HTML 注释或裸文本，大小写不敏感） */
    private val FORCE_BASELINE_REGEX =
        Regex("""force-update-baseline:\s*v?(\d+\.\d+\.\d+)""", RegexOption.IGNORE_CASE)

    /**
     * 从最新 Release 正文解析服务端下发的提醒基线，无标记或格式异常回退客户端默认。
     */
    fun parseForceUpdateBaseline(releaseNotes: String?): String =
        releaseNotes?.let { FORCE_BASELINE_REGEX.find(it)?.groupValues?.get(1) }
            ?: FORCE_UPDATE_MIN

    /** Release 正文中的 APK SHA-256 下发标记（HTML 注释或裸文本，大小写不敏感） */
    private val UPDATE_SHA256_REGEX =
        Regex("""update-sha256:\s*([a-fA-F0-9]{64})""", RegexOption.IGNORE_CASE)

    /**
     * 从最新 Release 正文解析更新 APK 的 SHA-256（update-sha256 标记），无标记返回 null。
     * 供下载开始时记录预期值、下载完成后做内容哈希校验（P0-3 更新链路加固）。
     */
    fun parseUpdateSha256(releaseNotes: String?): String? =
        releaseNotes?.let { UPDATE_SHA256_REGEX.find(it)?.groupValues?.get(1)?.lowercase() }

    /**
     * 提醒判定（纯函数，供单测锁定）：本地低于基线（v1.5.6 以下存量用户）且远端有可用更新。
     * 【v1.5.6 上架审计语义修正】此前误用"远端 >= 基线"——远端发布 v1.9.0 时会把
     * 已升级到 v1.5.6 的用户也标记为强更提醒，与"基线以下存量用户"定义矛盾；
     * 修正后以本地版本判定存量身份，远端仅要求"有可用更新"。
     * 服务端下发语义随之明确：下调基线（如 0.0.0）等效关闭提醒，上调基线扩大覆盖面
     * （如 1.9.0 → v1.8.9 存量用户也纳入）。基线可经 [parseForceUpdateBaseline] 下发。
     */
    fun shouldForceUpdate(
        remoteVersionCode: Int,
        currentVersionCode: Int,
        baseline: String = FORCE_UPDATE_MIN
    ): Boolean =
        remoteVersionCode > currentVersionCode &&
            currentVersionCode < VersionInfo.parseVersionCode(baseline)

    /**
     * 检查更新（根据渠道选择）
     *
     * 【v1.8.0 热更新修复】双渠道回退：配置渠道查询失败（网络异常）或判定"无更新"时，
     * 自动回退查询另一渠道，以"任一渠道有新版即提醒"为准。
     * 背景：原默认 Gitee 渠道指向的第三方镜像停更在 v1.4.1，版本比较恒判无更新，
     * 存量用户即使保持 Gitee 设置也能经回退收到 GitHub 权威发布（CI 实际发布仓库）的更新。
     *
     * @param context 上下文
     * @param currentVersionCode 当前版本号
     * @param channel 更新渠道，默认 GitHub
     * @return 更新信息，两渠道均失败返回 null
     */
    suspend fun checkUpdate(
        context: Context,
        currentVersionCode: Int,
        channel: UpdateChannel = UpdateChannel.GITHUB
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        val primary = when (channel) {
            UpdateChannel.GITEE -> checkGiteeUpdate(context, currentVersionCode)
            UpdateChannel.GITHUB -> checkGithubUpdate(context, currentVersionCode)
        }
        if (primary != null && primary.isNewer) return@withContext primary

        val secondary = when (channel) {
            UpdateChannel.GITEE -> checkGithubUpdate(context, currentVersionCode)
            UpdateChannel.GITHUB -> checkGiteeUpdate(context, currentVersionCode)
        }
        // 回退渠道有新版 → 用回退结果；否则保留主渠道结果（供 UI 展示"已是最新"），
        // 主渠道失败时退回回退渠道结果，两者都失败返回 null
        when {
            secondary != null && secondary.isNewer -> secondary
            else -> primary ?: secondary
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
                    // 【v1.5.6 修复】flavor 化（github/store 渠道分离）后 CI 产物名变为
                    // app-github-universal-release.apk，此处仍精确匹配旧名导致 downloadUrl
                    // 恒为空、应用内更新链路失效；改为通用后缀匹配，兼容旧命名与渠道变体。
                    if (assetName == UPDATE_APK_NAME || assetName.endsWith("-universal-release.apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                val releaseNotes = json.optString("body", context.getString(R.string.no_release_notes))
                // 【v1.5.6 提醒热更新 · 服务端下发】基线从最新 Release 正文解析
                // （force-update-baseline 标记），无标记回退客户端默认 FORCE_UPDATE_MIN
                val isNewer = versionCode > currentVersionCode && downloadUrl.isNotEmpty()

                UpdateInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    isNewer = isNewer,
                    forceUpdate = isNewer && shouldForceUpdate(
                        versionCode, currentVersionCode, parseForceUpdateBaseline(releaseNotes)
                    ),
                    sha256 = parseUpdateSha256(releaseNotes)
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

    /** 记录本次下载应校验的 APK SHA-256（传 null 清除残留值，防误校验后续下载） */
    internal fun setPendingApkSha256(context: Context, sha256: String?) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PENDING_APK_SHA256, sha256?.lowercase())
            .apply()
    }

    /** 读取并清除待校验哈希（one-shot：仅本次下载完成校验生效） */
    internal fun readAndClearPendingApkSha256(context: Context): String? {
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_PENDING_APK_SHA256, null)
        if (value != null) prefs.edit().remove(KEY_PENDING_APK_SHA256).apply()
        return value
    }

    /**
     * 使用系统 DownloadManager 下载并安装
     * @param sha256 预期 APK 哈希（Release 正文 update-sha256 标记，可空）
     * @return 下载任务 ID，用于查询进度
     */
    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        versionName: String,
        sha256: String? = null
    ): Long {
        // 【v1.6.13 安全加固】仅允许 https 下载源，防止外部传入明文链接导致 APK 被中间人替换
        if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
            Log.e(TAG, "拒绝非 https 下载地址: $downloadUrl")
            return -1L
        }

        // 【P0-3 更新链路加固】记录预期 APK 哈希（null 也写入以清除上次残留，防误校验）
        setPendingApkSha256(context, sha256)

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
     * @return Pair(下载状态, 进度百分比) 状态：1=等待中, 2=下载中, 4=完成, 8=失败, 16=暂停；查询异常返回 (-1, 0)
     */
    fun queryDownloadProgress(context: Context, downloadId: Long): Pair<Int, Int> {
        // 【v1.5.6 审计修复】DownloadManager.query 在部分 ROM 可能返回 null cursor，
        // 且列索引缺失时 getColumnIndexOrThrow 直接抛异常（调用方多为 UI 轮询，
        // 一崩即更新对话框闪退）。整体兜底为 (-1, 0)，由调用方按"查询失败"处理。
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return Pair(-1, 0)
        val cursor = try {
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        } catch (e: Exception) {
            Log.e(TAG, "查询下载进度失败", e)
            null
        } ?: return Pair(-1, 0)

        var status = -1
        var progress = 0
        try {
            if (cursor.moveToFirst()) {
                // 列缺失时 getColumnIndexOrThrow 抛 IllegalArgumentException，
                // 由下方 catch 统一兜底为 (-1, 0)（查询失败语义）
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_PENDING -> progress = 0
                    DownloadManager.STATUS_RUNNING -> {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> progress = 100
                    DownloadManager.STATUS_FAILED -> progress = -1
                    DownloadManager.STATUS_PAUSED -> {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析下载进度 cursor 失败", e)
            return Pair(-1, 0)
        } finally {
            cursor.close()
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

                // 归属校验 + 内容级完整性校验（P0-3）：仅安装包名/版本/签名/哈希（若有）均匹配的更新 APK
                if (apkFile != null && apkFile.exists() &&
                    isOurUpdateApk(context, apkFile) &&
                    verifyApkIntegrity(context, apkFile)
                ) {
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

    /**
     * 【P0-3 更新链路加固】内容级完整性校验（文件名/目录归属之后的第二道防线，fail-closed）：
     * 1) 包名必须与本应用一致（防任意 APK 误装）；
     * 2) versionCode 不得低于当前已装版本（防降级攻击）；
     * 3) 签名证书与已安装应用存在交集（防替换包；签名不一致时系统安装器也会拒绝，
     *    此处提前拦截，避免向用户展示注定失败的安装页）；
     * 4) 若本次下载携带预期 SHA-256（update-sha256 标记经 SP 传递），则对流式哈希比对，
     *    防下载损坏或传输中被替换；校验为一次性（读后即清）。
     * 任一环节失败均拒绝安装。
     */
    private fun verifyApkIntegrity(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager

            // 解析 APK 包元数据（API 28+ 携带签名信息；低版本走 GET_SIGNATURES 回退）
            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            }
            if (archiveInfo == null) {
                Log.e("DownloadReceiver", "更新 APK 元数据解析失败，拒绝安装: ${apkFile.name}")
                return false
            }

            // 1) 包名一致性
            if (archiveInfo.packageName != context.packageName) {
                Log.e("DownloadReceiver", "更新 APK 包名不匹配: ${archiveInfo.packageName}")
                return false
            }

            // 2) 防降级：以当前已装版本为基准（重装同版本允许，低版本拒绝）
            val installedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            if (PackageInfoCompat.getLongVersionCode(archiveInfo) <
                PackageInfoCompat.getLongVersionCode(installedInfo)
            ) {
                Log.e(
                    "DownloadReceiver",
                    "更新 APK 版本(${PackageInfoCompat.getLongVersionCode(archiveInfo)})低于当前已装版本，拒绝降级安装"
                )
                return false
            }

            // 3) 签名一致性：与已安装应用证书集合存在交集（兼容签名轮换场景）
            val installedCerts = signatureDigests(installedInfo)
            if (installedCerts.isNullOrEmpty()) {
                Log.e("DownloadReceiver", "无法读取已安装应用签名，拒绝安装")
                return false
            }
            val archiveCerts = signatureDigests(archiveInfo)
            if (archiveCerts == null || archiveCerts.intersect(installedCerts).isEmpty()) {
                Log.e("DownloadReceiver", "更新 APK 签名与当前应用不匹配，疑似替换包，拒绝安装")
                return false
            }

            // 4) 预期哈希校验（一次性；有值才校验，向后兼容未携带标记的旧发布渠道）
            val expectedSha256 = UpdateChecker.readAndClearPendingApkSha256(context)
            if (expectedSha256 != null) {
                val actual = fileSha256(apkFile)
                if (actual == null || !actual.equals(expectedSha256, ignoreCase = true)) {
                    Log.e("DownloadReceiver", "更新 APK SHA-256 校验失败（预期 $expectedSha256，实际 $actual）")
                    return false
                }
                Log.d("DownloadReceiver", "更新 APK SHA-256 校验通过")
            }

            true
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "更新 APK 完整性校验异常，拒绝安装", e)
            false
        }
    }

    /** 提取 APK 签名证书的 SHA-256 摘要集合；无签名信息返回 null */
    private fun signatureDigests(info: PackageInfo): Set<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { sha256Hex(it.toByteArray()) }?.toSet()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.map { sha256Hex(it.toByteArray()) }?.toSet()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /** 流式计算文件 SHA-256（更新包数十 MB，避免整块载入内存） */
    private fun fileSha256(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("DownloadReceiver", "计算 APK 哈希失败", e)
            null
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
