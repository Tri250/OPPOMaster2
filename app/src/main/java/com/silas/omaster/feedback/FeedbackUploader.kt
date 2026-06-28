package com.silas.omaster.feedback

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 反馈上传器
 * 使用 Ktor CIO 引擎将反馈（JSON + 可选截图）上传到后端。
 *
 * 注意：FEEDBACK_API_ENDPOINT 为占位符，实际部署时替换为真实接口地址。
 */
class FeedbackUploader(context: Context) {

    companion object {
        private const val TAG = "FeedbackUploader"
        // TODO: 替换为真实反馈接口地址
        private const val FEEDBACK_API_ENDPOINT = "https://api.omaster.silas/feedback/v1/submit"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay()
            }
        }
    }

    /**
     * 上传单条反馈。
     * @return true 表示上传成功，false 表示失败（可重试）
     */
    suspend fun upload(entry: FeedbackEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadataJson = json.encodeToString(entry)
            val screenshotFile = entry.screenshotPath?.let { File(it) }?.takeIf { it.exists() }

            val response = client.post(FEEDBACK_API_ENDPOINT) {
                header(HttpHeaders.Authorization, "Bearer anonymous") // 匿名上传
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("metadata", metadataJson, Headers.build {
                                append(HttpHeaders.ContentType, "application/json")
                            })
                            if (screenshotFile != null) {
                                append("screenshot", screenshotFile.readBytes(), Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${screenshotFile.name}\"")
                                })
                            }
                        }
                    )
                )
            }

            if (response.status.isSuccess()) {
                Log.i(TAG, "Feedback ${entry.id} uploaded successfully")
                true
            } else {
                Log.w(TAG, "Feedback upload failed: ${response.status} - ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Feedback upload exception for ${entry.id}", e)
            false
        }
    }

    fun release() {
        client.close()
    }
}
