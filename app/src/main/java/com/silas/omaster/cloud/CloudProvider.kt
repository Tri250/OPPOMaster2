package com.silas.omaster.cloud

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL

/**
 * 云端服务提供者定义
 *
 * 使用 sealed class 约束支持的服务类型，
 * 每种提供者携带独立的配置数据类与连接验证方法。
 */
sealed class CloudProvider {

    /** 提供者类型标识 */
    abstract val type: String

    /** 显示名称 */
    abstract val displayName: String

    /** 连接验证：返回 true 表示配置可用 */
    abstract suspend fun validateConnection(): Boolean

    // ==================== WebDAV ====================

    /**
     * WebDAV 服务提供者
     *
     * @param serverUrl  服务器地址，如 https://dav.example.com/omaster/
     * @param username   Basic Auth 用户名
     * @param password   Basic Auth 密码
     */
    data class WebDAV(
        val serverUrl: String,
        val username: String,
        val password: String
    ) : CloudProvider() {

        override val type: String = TYPE_WEBDAV
        override val displayName: String = "WebDAV"

        override suspend fun validateConnection(): Boolean {
            return try {
                val url = URL(serverUrl.trimEnd('/') + "/")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PROPFIND"
                conn.setRequestProperty("Depth", "0")
                conn.setRequestProperty("Authorization", buildBasicAuth())
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                val code = conn.responseCode
                conn.disconnect()
                code in 200..399
            } catch (_: Exception) {
                false
            }
        }

        fun buildBasicAuth(): String {
            val credential = "$username:$password"
            return "Basic ${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"
        }

        companion object {
            const val TYPE_WEBDAV = "webdav"
        }
    }

    // ==================== Google Drive ====================

    /**
     * Google Drive 服务提供者
     *
     * @param accessToken OAuth2 访问令牌
     * @param folderId    同步目标文件夹 ID（Drive API file ID）
     */
    data class GoogleDrive(
        val accessToken: String,
        val folderId: String = "root"
    ) : CloudProvider() {

        override val type: String = TYPE_GDRIVE
        override val displayName: String = "Google Drive"

        override suspend fun validateConnection(): Boolean {
            return try {
                val url = URL("https://www.googleapis.com/drive/v3/files/$folderId?fields=id,name")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                val code = conn.responseCode
                conn.disconnect()
                code == 200
            } catch (_: Exception) {
                false
            }
        }

        companion object {
            const val TYPE_GDRIVE = "gdrive"
        }
    }
}
