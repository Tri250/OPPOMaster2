package com.silas.omaster.network

import android.content.Context
import android.util.Log
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.silas.omaster.util.JsonUtil
import kotlinx.serialization.json.Json
import java.io.File

object PresetRemoteManager {

    // 使用 lazy + Application 生命周期内复用
    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            engine {
                requestTimeout = 30_000
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

    suspend fun fetchPresets(url: String): PresetList? {
        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            val response: HttpResponse = client.get(url)
            // Some servers (GitHub raw) may return Content-Type: text/plain; charset=utf-8
            // which prevents Ktor's content-negotiation from selecting the JSON transformer.
            // Read as text and decode explicitly to avoid NoTransformationFoundException.
            val text: String = response.body()
            val presets = Json.decodeFromString(PresetList.serializer(), text)
            Log.d("PresetRemoteManager", "Fetched ${presets.presets.size} presets")
            presets
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to fetch presets", e)
            null
        }
    }

    /**
     * 验证 URL 安全性
     * 仅允许 HTTPS 协议（强制加密），并防止 SSRF
     */
    private fun validateUrl(url: String): String? {
        if (url.isBlank()) return "URL 不能为空"
        if (!url.lowercase().startsWith("https://")) return "仅支持 HTTPS 协议"
        // 防止 SSRF：禁止访问内网地址
        val lower = url.lowercase()
        // 严格匹配主机段,而非子串,避免误判
        val blockedPrefixes = listOf("localhost", "127.", "0.0.0.0", "10.", "192.168.", "172.16.", "169.254.")
        val hostStart = lower.indexOf("https://") + 8
        val hostEnd = lower.indexOf('/', startIndex = hostStart).let { if (it < 0) lower.length else it }
        val host = lower.substring(hostStart, hostEnd)
        if (blockedPrefixes.any { host.startsWith(it) || host == it.trimEnd('.') }) {
            return "禁止访问内网地址"
        }
        return null
    }

    suspend fun fetchAndSave(context: Context, url: String, forceUpdate: Boolean = false): Result<PresetList> {
        // URL 安全验证
        validateUrl(url)?.let { return Result.failure(SecurityException(it)) }

        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            val response: HttpResponse = client.get(url)

            // 验证响应码
            if (response.status.value !in 200..299) {
                return Result.failure(Exception("HTTP ${response.status.value}"))
            }

            val text: String = response.body()

            // 验证 JSON 是否有效
            val presetList = try {
                Json.decodeFromString(PresetList.serializer(), text)
            } catch (e: Exception) {
                Log.e("PresetRemoteManager", "Invalid JSON received", e)
                return Result.failure(Exception("JSON 格式错误"))
            }

            // 验证必填字段
            val missingFields = mutableListOf<String>()
            if (presetList.name.isNullOrBlank()) missingFields.add("name (订阅名称)")
            if (presetList.author.isNullOrBlank()) missingFields.add("author (作者)")
            if (missingFields.isNotEmpty()) {
                val errorMsg = "缺少必要字段: ${missingFields.joinToString(", ")}"
                return Result.failure(Exception(errorMsg))
            }

            val subManager = com.silas.omaster.data.local.SubscriptionManager.getInstance(context)

            // 检查版本号是否相同
            if (!forceUpdate) {
                val currentSub = subManager.subscriptionsFlow.value.find { it.url == url }
                if (currentSub != null && currentSub.build == presetList.build) {
                    return Result.failure(Exception("无需更新"))
                }
            }

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
