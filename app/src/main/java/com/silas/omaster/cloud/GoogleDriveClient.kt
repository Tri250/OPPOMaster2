package com.silas.omaster.cloud

import android.util.Log
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Google Drive 云端客户端
 *
 * 使用 Google Drive REST API v3 实现文件操作。
 * 与 WebDAVClient 保持一致的接口风格：
 * - listFiles: 列出文件
 * - uploadFile: 上传文件
 * - downloadFile: 下载文件
 * - deleteFile: 删除文件
 *
 * 认证方式：OAuth 2.0 Bearer Token
 * Token 从 SettingsManager 获取，支持自动刷新
 */
class GoogleDriveClient(
    private val accessToken: String,
    private val folderId: String = "root"
) {

    companion object {
        private const val TAG = "GoogleDriveClient"
        private const val BASE_URL = "https://www.googleapis.com"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"
        private const val CONNECT_TIMEOUT = 15_000
        private const val READ_TIMEOUT = 30_000
    }

    // ==================== 文件列表 ====================

    /**
     * 列出指定文件夹下的文件
     *
     * @param remotePath 远程路径（Google Drive 中为文件夹名称或 ID）
     * @return 文件列表
     */
    suspend fun listFiles(remotePath: String? = null): Result<List<GoogleDriveFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFolderId = if (remotePath.isNullOrEmpty()) folderId else resolveFolderId(remotePath)

            val query = "'$targetFolderId' in parents and trashed=false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$BASE_URL/drive/v3/files?q=$encodedQuery&fields=files(id,name,mimeType,size,modifiedTime,trashed)&pageSize=100")

            val conn = openConnection(url, "GET")
            val code = conn.responseCode

            if (code != 200) {
                val errorBody = readErrorBody(conn)
                conn.disconnect()
                throw GoogleDriveException("列出文件失败: HTTP $code - $errorBody")
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            parseFileListResponse(response)
        }
    }

    // ==================== 上传文件 ====================

    /**
     * 上传文件到 Google Drive
     *
     * @param remotePath 远程文件路径（相对于 folderId）
     * @param data 文件内容
     * @param mimeType MIME 类型
     * @return 上传后的文件 ID
     */
    suspend fun uploadFile(
        remotePath: String,
        data: ByteArray,
        mimeType: String = "application/octet-stream"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = remotePath.trim('/').substringAfterLast('/')
            val parentFolderId = resolveParentFolderId(remotePath)

            // 先查找同名文件是否已存在
            val existingFileId = findFileByName(fileName, parentFolderId)

            if (existingFileId != null) {
                // 更新已有文件
                val url = URL("$UPLOAD_URL/files/$existingFileId?uploadType=media")
                val conn = openConnection(url, "PATCH")
                conn.setRequestProperty("Content-Type", mimeType)
                conn.doOutput = true

                conn.outputStream.use { it.write(data) }
                val code = conn.responseCode
                conn.disconnect()

                if (code !in 200..299) {
                    throw GoogleDriveException("更新文件失败: HTTP $code")
                }
                existingFileId
            } else {
                // 创建新文件
                val metadata = """{"name":"$fileName","parents":["$parentFolderId"]}"""
                val boundary = "omaster_boundary_${System.currentTimeMillis()}"

                val url = URL("$UPLOAD_URL/files?uploadType=multipart")
                val conn = openConnection(url, "POST")
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    writeMultipartBody(os, boundary, metadata, mimeType, data)
                }

                val code = conn.responseCode
                val response = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    readErrorBody(conn)
                }
                conn.disconnect()

                if (code !in 200..299) {
                    throw GoogleDriveException("上传文件失败: HTTP $code - $response")
                }

                val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
                idRegex.find(response)?.groupValues?.get(1) ?: throw GoogleDriveException("无法解析文件 ID")
            }
        }
    }

    // ==================== 下载文件 ====================

    /**
     * 下载文件
     *
     * @param remotePath 远程文件路径
     * @return 文件内容
     */
    suspend fun downloadFile(remotePath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = remotePath.trim('/').substringAfterLast('/')
            val parentFolderId = resolveParentFolderId(remotePath)

            val fileId = findFileByName(fileName, parentFolderId)
                ?: throw GoogleDriveException("文件不存在: $fileName")

            val url = URL("$BASE_URL/drive/v3/files/$fileId?alt=media")
            val conn = openConnection(url, "GET")

            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                throw GoogleDriveException("下载文件失败: HTTP $code")
            }

            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            bytes
        }
    }

    // ==================== 删除文件 ====================

    /**
     * 删除文件
     *
     * @param remotePath 远程文件路径
     */
    suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = remotePath.trim('/').substringAfterLast('/')
            val parentFolderId = resolveParentFolderId(remotePath)

            val fileId = findFileByName(fileName, parentFolderId)
                ?: return@runCatching // 文件不存在，视为删除成功

            val url = URL("$BASE_URL/drive/v3/files/$fileId")
            val conn = openConnection(url, "DELETE")

            val code = conn.responseCode
            conn.disconnect()

            if (code != 204 && code != 404) {
                throw GoogleDriveException("删除文件失败: HTTP $code")
            }
        }
    }

    // ==================== 连接验证 ====================

    /**
     * 验证连接是否有效
     */
    suspend fun validateConnection(): Boolean {
        return try {
            val url = URL("$BASE_URL/drive/v3/files/$folderId?fields=id,name")
            val conn = openConnection(url, "GET")
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (_: Exception) {
            false
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 按名称查找文件
     */
    private fun findFileByName(fileName: String, parentFolderId: String): String? {
        return try {
            val query = "name='$fileName' and '$parentFolderId' in parents and trashed=false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$BASE_URL/drive/v3/files?q=$encodedQuery&fields=files(id,name)")

            val conn = openConnection(url, "GET")
            val code = conn.responseCode

            if (code != 200) {
                conn.disconnect()
                return null
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            idRegex.find(response)?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.w(TAG, "查找文件失败", e)
            null
        }
    }

    /**
     * 解析文件夹 ID（支持用文件夹名称查找）
     */
    private fun resolveFolderId(path: String): String {
        if (path.isEmpty()) return folderId
        val folderName = path.trim('/').split("/").lastOrNull() ?: return folderId
        if (folderName == "root") return "root"

        // 尝试按名称查找文件夹
        val query = "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("$BASE_URL/drive/v3/files?q=$encodedQuery&fields=files(id,name)")

        val conn = openConnection(url, "GET")
        return try {
            val code = conn.responseCode
            if (code == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val idRegex = """"id"\s*:\s*"([^"]+)"""".toRegex()
                idRegex.find(response)?.groupValues?.get(1) ?: folderId
            } else {
                folderId
            }
        } catch (_: Exception) {
            folderId
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveParentFolderId(remotePath: String): String {
        val path = remotePath.trim('/')
        val segments = path.split("/")
        if (segments.size <= 1) return folderId
        val parentPath = segments.dropLast(1).joinToString("/")
        return resolveFolderId(parentPath)
    }

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("User-Agent", "OMaster/${com.silas.omaster.BuildConfig.VERSION_NAME}")
        conn.instanceFollowRedirects = true
        return conn
    }

    private fun writeMultipartBody(
        os: OutputStream,
        boundary: String,
        metadata: String,
        mimeType: String,
        data: ByteArray
    ) {
        os.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        os.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray(Charsets.UTF_8))
        os.write("$metadata\r\n".toByteArray(Charsets.UTF_8))
        os.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        os.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
        os.write(data)
        os.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        os.flush()
    }

    private fun readErrorBody(conn: HttpURLConnection): String {
        return try {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseFileListResponse(json: String): List<GoogleDriveFile> {
        val files = mutableListOf<GoogleDriveFile>()
        try {
            val filePattern = Regex("""\{[^}]*"id"\s*:\s*"([^"]+)"[^}]*"name"\s*:\s*"([^"]+)"[^}]*\}""")
            filePattern.findAll(json).forEach { match ->
                val id = match.groupValues[1]
                val name = match.groupValues[2]
                val mimeType = Regex(""""mimeType"\s*:\s*"([^"]+)"""").find(match.value)?.groupValues?.get(1) ?: ""
                val size = Regex(""""size"\s*:\s*"(\d+)"""").find(match.value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val modifiedTime = Regex(""""modifiedTime"\s*:\s*"([^"]+)"""").find(match.value)?.groupValues?.get(1) ?: ""
                val isFolder = mimeType == "application/vnd.google-apps.folder"
                files.add(
                    GoogleDriveFile(
                        id = id,
                        name = name,
                        mimeType = mimeType,
                        size = size,
                        modifiedTime = modifiedTime,
                        isFolder = isFolder
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析文件列表响应失败", e)
        }
        return files
    }
}

/**
 * Google Drive 文件描述
 */
data class GoogleDriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val modifiedTime: String,
    val isFolder: Boolean
)

/**
 * Google Drive 操作异常
 */
class GoogleDriveException(message: String) : Exception(message)