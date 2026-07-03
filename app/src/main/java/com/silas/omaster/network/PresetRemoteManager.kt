package com.silas.omaster.network

import android.content.Context
import android.util.Log
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.silas.omaster.util.JsonUtil
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.pow

object PresetRemoteManager {

    /**
     * JSON 解析配置——与 PresetRepository 保持一致
     *
     * 修复：原实现使用 Json 伴生对象默认配置（ignoreUnknownKeys=false, coerceInputValues=false），
     * 导致远程 JSON 包含未知字段或显式 null 时解析失败，而同一段 JSON 写入磁盘后
     * PresetRepository 却能解析成功（因其配置了 ignoreUnknownKeys=true, coerceInputValues=true）。
     * 此处统一配置消除"拉取阶段"与"缓存读取阶段"的行为不一致。
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    // 使用 lazy + Application 生命周期内复用
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }

            // ==================== 连接池配置 ====================
            engine {
                // 连接超时
                requestTimeout = 30_000
                // 全局最大连接数
                maxConnectionsCount = 32
                // 连接池配置
                endpoint.maxConnectionsPerRoute = 8          // 每个路由最大连接数
                endpoint.pipelineMaxSize = 4                  // 流水线最大请求数
                endpoint.keepAliveTime = 30_000               // 保持连接存活时间 (30s)
                endpoint.connectTimeout = 10_000              // 连接建立超时 (10s)
                endpoint.connectAttempts = 2                  // 连接重试次数
                endpoint.socketTimeout = 15_000               // Socket 超时
            }

            // ==================== 超时配置 ====================
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            // ==================== 请求重试配置（指数退避） ====================
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                exponentialDelay()
                // 不对 POST 请求重试（幂等性保证）
                retryIf { _, response ->
                    val status = response.status.value
                    status in 500..599 || status == 429
                }
            }
        }
    }

    /**
     * 关闭 HTTP 客户端（仅在 Application 终止时调用）
     */
    fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            Log.w("PresetRemoteManager", "关闭HttpClient失败", e)
        }
    }

    /**
     * 带指数退避的网络请求重试
     * @param maxRetries 最大重试次数
     * @param baseDelayMs 初始延迟（毫秒）
     * @param block 请求操作
     * @return 请求结果或 null
     */
    private suspend fun <T> withExponentialBackoff(
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000L,
        block: suspend () -> T
    ): T? {
        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    // 指数退避：baseDelay * 2^attempt
                    val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
                    Log.w("PresetRemoteManager", "请求失败，${delayMs}ms后重试 (${attempt + 1}/$maxRetries): ${e.message}")
                    delay(delayMs)
                }
            }
        }

        Log.e("PresetRemoteManager", "请求最终失败，已重试 $maxRetries 次", lastException)
        return null
    }

    /**
     * 构建缓存控制请求头
     * 添加 If-None-Match 和 Cache-Control 头以支持条件请求
     */
    private fun buildCacheHeaders(etag: String? = null): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["Cache-Control"] = "max-age=3600, stale-while-revalidate=86400"
        etag?.let {
            headers["If-None-Match"] = it
        }
        return headers
    }

    /**
     * 订阅源拉取结果——区分「订阅源不可用」与「网络错误」
     * UC-13: 缺失字段时返回 ParseFailed，而非崩溃
     * UC-20: 404/非JSON返回 InvalidSource
     */
    sealed class FetchResult {
        data class Success(val presetList: PresetList) : FetchResult()
        data class InvalidSource(val message: String) : FetchResult()
        data class ParseFailed(val message: String) : FetchResult()
        data class NetworkError(val message: String) : FetchResult()
    }

    suspend fun fetchPresets(url: String): PresetList? {
        val result = fetchPresetsWithResult(url)
        return when (result) {
            is FetchResult.Success -> result.presetList
            else -> null
        }
    }

    /**
     * UC-11/UC-13/UC-20: 带 error type 的拉取接口
     * UC-11: 以 OMaster-Community 格式 (oppo.json / PresetList + sections) 解析
     * UC-13: JSON 缺失 sections 等字段时返回 ParseFailed，不崩溃
     * UC-20: 404/非JSON 响应返回 InvalidSource（"订阅源不可用"）
     */
    suspend fun fetchPresetsWithResult(url: String): FetchResult {
        Log.d("PresetRemoteManager", "Starting fetch from $url")
        // SSRF防护：验证URL
        validateUrl(url)?.let { error ->
            Log.e("PresetRemoteManager", "URL validation failed: $error")
            return FetchResult.InvalidSource(error)
        }

        val response: HttpResponse = try {
            client.get(url) {
                buildCacheHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Network error fetching presets", e)
            return FetchResult.NetworkError(e.message ?: "网络请求失败")
        }

        // UC-20: 检查 HTTP 状态码，404 等非 2xx 视为「订阅源不可用」
        val statusCode = response.status.value
        if (statusCode !in 200..299) {
            val msg = "HTTP $statusCode"
            Log.e("PresetRemoteManager", "Subscription source unavailable: $msg")
            return FetchResult.InvalidSource("订阅源不可用 ($msg)")
        }

        // 读取响应文本
        val text: String = try {
            response.body()
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to read response body", e)
            return FetchResult.NetworkError(e.message ?: "读取响应失败")
        }

        // UC-13: JSON 解析——缺失 sections 等字段时返回 ParseFailed 而不崩溃
        val presetList = try {
            json.decodeFromString(PresetList.serializer(), text)
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "JSON parse failed for $url", e)
            return FetchResult.ParseFailed("解析失败: ${e.message}")
        }

        Log.d("PresetRemoteManager", "Fetched ${presetList.presets.size} presets")
        return FetchResult.Success(presetList)
    }

    /**
     * 验证 URL 安全性（SSRF 防护）
     * - 仅允许 HTTPS 协议（强制加密）
     * - 使用 URI 标准化解析，避免手动字符串分割被绕过
     * - 禁止 @ 绕过（userInfo）
     * - 端口检查：只允许 443
     * - 完整 IPv4/IPv6 私有地址段屏蔽
     * - IPv6 方括号形式屏蔽
     * - 纯数字/IPv4 地址屏蔽
     */
    private fun validateUrl(url: String): String? {
        if (url.isBlank()) return "URL 不能为空"

        // 使用 URI 标准化解析，避免手动字符串分割被绕过
        val uri = try {
            java.net.URI(url.trim())
        } catch (e: Exception) {
            return "URL 格式不合法"
        }

        // 协议检查
        if (uri.scheme?.lowercase() != "https") return "仅支持 HTTPS 协议"

        val host = uri.host ?: return "无法解析主机名"

        // 禁止 @ 绕过（如 https://evil.com@cdn.jsdelivr.net/...）
        val userInfo = uri.userInfo
        if (!userInfo.isNullOrBlank()) return "URL 中不允许包含用户信息"

        // 端口检查：只允许 443，禁止端口 0 和其他非标准端口
        val port = uri.port
        if (port != -1 && port != 443) {
            return "仅允许标准 HTTPS 端口 (443)"
        }

        val lowerHost = host.lowercase()

        // 禁止访问内网和本地地址（含 IPv4 和 IPv6）
        val blockedPrefixes = listOf(
            "localhost", "127.", "0.0.0.0", "::1",
            "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.", "169.254.", "fc00:", "fe80:", "ff00:", "ff02:"
        )

        if (blockedPrefixes.any { lowerHost.startsWith(it) || lowerHost == it.trimEnd('.') }) {
            return "禁止访问内网或本地地址"
        }

        // IPv6 地址检测（含方括号形式如 [::1]）
        if (lowerHost.startsWith("[") && lowerHost.endsWith("]")) {
            return "禁止直接使用 IP 地址"
        }

        // 验证域名格式（防止 IPv4 地址绕过）
        if (lowerHost.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
            return "禁止直接使用 IP 地址"
        }

        // 额外检查：确保 host 不是纯数字（某些 IPv6 缩写或畸形地址）
        if (lowerHost.all { it.isDigit() || it == ':' || it == '.' }) {
            return "禁止直接使用 IP 地址"
        }

        return null
    }

    /**
     * UC-12: 仅 build 号增大时触发更新；pull-to-refresh 不会无限转圈
     * UC-13: 缺失字段时返回 ParseFailed 而不崩溃
     * UC-20: 404/非JSON 返回"订阅源不可用"
     */
    suspend fun fetchAndSave(context: Context, url: String, forceUpdate: Boolean = false): Result<PresetList> {
        // URL 安全验证
        validateUrl(url)?.let { return Result.failure(SecurityException(it)) }

        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            // 使用带 FetchResult 的接口拉取，区分 InvalidSource / ParseFailed / NetworkError
            val fetchResult = withExponentialBackoff(maxRetries = 3) {
                fetchPresetsWithResult(url)
            }

            // 重试耗尽后仍为 null
            if (fetchResult == null) {
                return Result.failure(Exception("网络请求失败，已重试3次"))
            }

            when (fetchResult) {
                is FetchResult.InvalidSource -> {
                    // UC-20: 404/非JSON → "订阅源不可用"
                    return Result.failure(Exception(fetchResult.message))
                }
                is FetchResult.ParseFailed -> {
                    // UC-13: JSON 缺失字段 → "解析失败"
                    return Result.failure(Exception(fetchResult.message))
                }
                is FetchResult.NetworkError -> {
                    return Result.failure(Exception(fetchResult.message))
                }
                is FetchResult.Success -> { /* 正常继续 */ }
            }

            val presetList = fetchResult.presetList

            // 验证必填字段
            val missingFields = mutableListOf<String>()
            if (presetList.name.isNullOrBlank()) missingFields.add("name (订阅名称)")
            if (presetList.author.isNullOrBlank()) missingFields.add("author (作者)")
            if (missingFields.isNotEmpty()) {
                val errorMsg = "缺少必要字段: ${missingFields.joinToString(", ")}"
                return Result.failure(Exception(errorMsg))
            }

            val subManager = com.silas.omaster.data.local.SubscriptionManager.getInstance(context)

            // UC-12: 仅远程 build > 本地 build 时才触发更新
            if (!forceUpdate) {
                val currentSub = subManager.subscriptionsFlow.value.find { it.url == url }
                if (currentSub != null && currentSub.build >= presetList.build) {
                    return Result.failure(Exception("无需更新"))
                }
            }

            // 重新拉取原始文本用于写入磁盘（fetchPresetsWithResult 只返回解析后对象）
            val response = withExponentialBackoff(maxRetries = 2) {
                client.get(url) {
                    buildCacheHeaders().forEach { (key, value) ->
                        header(key, value)
                    }
                }
            } ?: return Result.failure(Exception("网络请求失败"))

            val text: String = response.body()

            withContext(Dispatchers.IO) {
                val fileName = subManager.getFileNameForUrl(url)
                val file = File(context.filesDir, fileName)
                // 防御性：原子写入,先写临时文件,再重命名
                val tempFile = File(context.filesDir, "$fileName.tmp")
                try {
                    tempFile.writeText(text)
                    if (file.exists()) file.delete()
                    if (!tempFile.renameTo(file)) {
                        // renameTo 失败则尝试回退
                        file.writeText(text)
                        tempFile.delete()
                    }
                    Log.d("PresetRemoteManager", "Saved remote presets to ${file.absolutePath}")
                } catch (e: java.io.IOException) {
                    Log.e("PresetRemoteManager", "保存预设文件失败: ${file.absolutePath}", e)
                    throw e
                }

                // Update subscription info
                subManager.updateSubscriptionStatus(
                    url = url,
                    presetCount = presetList.presets.size,
                    lastUpdateTime = System.currentTimeMillis(),
                    name = presetList.name,
                    author = presetList.author,
                    build = presetList.build
                )

                // Invalidate JsonUtil cache so subsequent loads read the new remote file
                try {
                    JsonUtil.invalidateCache()
                } catch (e: Exception) {
                    Log.w("PresetRemoteManager", "Failed to invalidate JsonUtil cache", e)
                }
            }
            Result.success(presetList)
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to save presets", e)
            Result.failure(e)
        }
    }
}
