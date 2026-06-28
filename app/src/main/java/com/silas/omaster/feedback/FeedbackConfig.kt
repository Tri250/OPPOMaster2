package com.silas.omaster.feedback

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * 反馈模块配置
 *
 * 从项目根目录的 local.properties 读取 `feedback.api.endpoint`，
 * 允许在不同环境（开发/测试/生产）指向不同接口，避免在代码中硬编码真实域名。
 *
 * 如果 local.properties 未配置，则使用默认生产域名。
 */
object FeedbackConfig {

    private const val TAG = "FeedbackConfig"
    private const val DEFAULT_ENDPOINT = "https://api.omaster.silas/feedback/v1/submit"
    private const val LOCAL_PROPERTIES = "local.properties"
    private const val KEY_ENDPOINT = "feedback.api.endpoint"

    @Volatile
    private var cachedEndpoint: String? = null

    /**
     * 获取反馈上传接口地址。
     * 优先从 local.properties 读取，未配置则返回默认生产地址。
     */
    fun getEndpoint(context: Context): String {
        cachedEndpoint?.let { return it }

        val endpoint = readFromLocalProperties(context) ?: DEFAULT_ENDPOINT
        cachedEndpoint = endpoint
        Log.i(TAG, "Feedback endpoint: $endpoint")
        return endpoint
    }

    /**
     * 允许运行时覆盖端点（例如从远程配置加载后）。
     */
    fun setEndpoint(endpoint: String) {
        cachedEndpoint = endpoint
    }

    private fun readFromLocalProperties(context: Context): String? {
        return try {
            val properties = Properties()
            // 优先从项目根目录读取（与 Android Studio 行为一致）
            val rootFile = File(LOCAL_PROPERTIES)
            if (rootFile.exists()) {
                FileInputStream(rootFile).use { properties.load(it) }
            } else {
                // 兜底：从 assets 读取
                context.assets.open(LOCAL_PROPERTIES).use { properties.load(it) }
            }
            properties.getProperty(KEY_ENDPOINT)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "读取 local.properties 失败，使用默认 endpoint", e)
            null
        }
    }
}
