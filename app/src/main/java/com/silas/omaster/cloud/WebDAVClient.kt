package com.silas.omaster.cloud

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * WebDAV 协议客户端
 *
 * 基于 HttpURLConnection 实现标准 WebDAV 操作：
 * - PROPFIND：列出目录/获取文件属性
 * - PUT：上传文件
 * - GET：下载文件
 * - DELETE：删除文件/目录
 * - MKCOL：创建目录
 *
 * 认证方式：HTTP Basic Auth
 * 超时配置：连接 15s / 读取 30s
 */
class WebDAVClient(
    private val serverUrl: String,
    private val username: String,
    private val password: String
) {

    companion object {
        private const val TAG = "WebDAVClient"
        private const val CONNECT_TIMEOUT = 15_000
        private const val READ_TIMEOUT = 30_000
    }

    // ==================== PROPFIND ====================

    /**
     * 列出远程目录下的文件与子目录
     *
     * @param remotePath 远程路径（相对 serverUrl），如 "/omaster/presets/"
     * @param depth      "0"=仅当前资源, "1"=当前+子项（默认）, "infinity"=递归
     * @return WebDAV 资源列表
     */
    suspend fun propFind(
        remotePath: String,
        depth: String = "1"
    ): Result<List<WebDAVResource>> = runCatching {
        val url = buildUrl(remotePath)
        val conn = openConnection(url, "PROPFIND")
        conn.setRequestProperty("Depth", depth)
        conn.setRequestProperty("Content-Type", "application/xml; charset=utf-8")

        // PROPFIND 请求体（最小 prop 请求）
        val body = """<?xml version="1.0" encoding="utf-8"?>
            |<d:propfind xmlns:d="DAV:">
            |  <d:prop>
            |    <d:getlastmodified/>
            |    <d:getcontentlength/>
            |    <d:resourcetype/>
            |  </d:prop>
            |</d:propfind>""".trimMargin()
        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            throw WebDAVException("PROPFIND failed: HTTP $code")
        }

        val xml = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        parsePropFindResponse(xml)
    }

    // ==================== PUT ====================

    /**
     * 上传文件到 WebDAV 服务器
     *
     * @param remotePath 远程文件路径
     * @param data       文件内容字节数组
     * @return Result<Unit>
     */
    suspend fun put(
        remotePath: String,
        data: ByteArray
    ): Result<Unit> = runCatching {
        val url = buildUrl(remotePath)
        val conn = openConnection(url, "PUT")
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.doOutput = true

        conn.outputStream.use { os ->
            os.write(data)
            os.flush()
        }

        val code = conn.responseCode
        conn.disconnect()
        if (code !in 200..299 && code != 201) {
            throw WebDAVException("PUT failed: HTTP $code")
        }
    }

    // ==================== GET ====================

    /**
     * 从 WebDAV 服务器下载文件
     *
     * @param remotePath 远程文件路径
     * @return Result<ByteArray>
     */
    suspend fun get(remotePath: String): Result<ByteArray> = runCatching {
        val url = buildUrl(remotePath)
        val conn = openConnection(url, "GET")

        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw WebDAVException("GET failed: HTTP $code")
        }

        val bytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        bytes
    }

    // ==================== DELETE ====================

    /**
     * 删除远程文件或目录
     *
     * @param remotePath 远程路径
     * @return Result<Unit>
     */
    suspend fun delete(remotePath: String): Result<Unit> = runCatching {
        val url = buildUrl(remotePath)
        val conn = openConnection(url, "DELETE")

        val code = conn.responseCode
        conn.disconnect()
        if (code !in 200..299 && code != 404) {
            throw WebDAVException("DELETE failed: HTTP $code")
        }
    }

    // ==================== MKCOL ====================

    /**
     * 创建远程目录
     *
     * @param remotePath 远程目录路径
     * @return Result<Unit>  成功或目录已存在时返回成功
     */
    suspend fun mkCol(remotePath: String): Result<Unit> = runCatching {
        val url = buildUrl(remotePath)
        val conn = openConnection(url, "MKCOL")

        val code = conn.responseCode
        conn.disconnect()
        // 201=创建成功, 405=已存在
        if (code != 201 && code != 405) {
            throw WebDAVException("MKCOL failed: HTTP $code")
        }
    }

    // ==================== 确保目录存在 ====================

    /**
     * 递归确保远程目录路径存在，不存在则逐级创建
     */
    suspend fun ensureDirectory(remotePath: String): Result<Unit> = runCatching {
        val normalized = remotePath.trim('/')
        if (normalized.isEmpty()) return@runCatching

        val segments = normalized.split("/")
        var currentPath = ""
        for (segment in segments) {
            if (segment.isBlank()) continue
            currentPath = "$currentPath/$segment"
            mkCol("$currentPath/").getOrDefault(Unit)
        }
    }

    // ==================== 内部工具方法 ====================

    private fun buildUrl(remotePath: String): URL {
        val base = serverUrl.trimEnd('/')
        val path = remotePath.trimStart('/')
        return URL("$base/$path")
    }

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("Authorization", buildBasicAuth())
        conn.setRequestProperty("User-Agent", "OMaster/1.0")
        conn.instanceFollowRedirects = true

        if (method == "PROPFIND") {
            conn.doOutput = true
        }

        return conn
    }

    private fun buildBasicAuth(): String {
        val credential = "$username:$password"
        return "Basic ${android.util.Base64.encodeToString(credential.toByteArray(), android.util.Base64.NO_WRAP)}"
    }

    /**
     * 解析 PROPFIND 的 XML 多状态响应
     *
     * 简易实现：使用 DocumentBuilder 提取 <d:href>、<d:getlastmodified>、
     * <d:getcontentlength>、<d:resourcetype>（是否包含 <d:collection/>）
     */
    private fun parsePropFindResponse(xml: String): List<WebDAVResource> {
        val resources = mutableListOf<WebDAVResource>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xml.byteInputStream())

            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            for (i in 0 until responses.length) {
                val responseNode = responses.item(i)
                var href = ""
                var lastModified: String? = null
                var contentLength: Long = 0
                var isDirectory = false

                val children = responseNode.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    when (child.localName) {
                        "href" -> href = child.textContent
                        "propstat" -> {
                            val propChildren = child.childNodes
                            for (k in 0 until propChildren.length) {
                                val propNode = propChildren.item(k)
                                if (propNode.localName == "prop") {
                                    val propFields = propNode.childNodes
                                    for (m in 0 until propFields.length) {
                                        val field = propFields.item(m)
                                        when (field.localName) {
                                            "getlastmodified" -> lastModified = field.textContent
                                            "getcontentlength" -> contentLength = field.textContent.toLongOrNull() ?: 0
                                            "resourcetype" -> {
                                                val rtChildren = field.childNodes
                                                for (n in 0 until rtChildren.length) {
                                                    if (rtChildren.item(n).localName == "collection") {
                                                        isDirectory = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (href.isNotEmpty()) {
                    resources.add(
                        WebDAVResource(
                            href = href,
                            lastModified = lastModified,
                            contentLength = contentLength,
                            isDirectory = isDirectory
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析 PROPFIND 响应失败", e)
        }
        return resources
    }
}

/**
 * WebDAV 资源描述
 */
data class WebDAVResource(
    val href: String,
    val lastModified: String?,
    val contentLength: Long,
    val isDirectory: Boolean
)

/**
 * WebDAV 操作异常
 */
class WebDAVException(message: String) : Exception(message)
