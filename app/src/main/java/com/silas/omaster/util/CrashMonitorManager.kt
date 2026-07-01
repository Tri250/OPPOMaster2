package com.silas.omaster.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃监控与上报管理器
 *
 * 功能：
 * - 本地崩溃日志存储
 * - 崩溃统计分析
 * - 可选的线上崩溃上报
 * - 崩溃报告生成
 *
 * 注意：上报功能需要配置上报URL，默认仅本地存储
 */
object CrashMonitorManager {

    private const val TAG = "CrashMonitor"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 30
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L // 5MB

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 配置
    private var context: Context? = null
    private var uploadUrl: String? = null
    private var isUploadEnabled: Boolean = false
    private var appVersion: String = "unknown"
    private var deviceInfo: String = "unknown"

    // 统计数据
    private val _crashCount = kotlinx.coroutines.flow.MutableStateFlow(0)
    val crashCount: kotlinx.coroutines.flow.StateFlow<Int> = _crashCount.asStateFlow()

    /**
     * 崩溃报告数据类
     */
    data class CrashReport(
        val crashId: String,
        val timestamp: Long,
        val appVersion: String,
        val deviceInfo: String,
        val androidVersion: String,
        val crashType: String,
        val exceptionName: String,
        val message: String,
        val stackTrace: String,
        val threadName: String,
        val memoryUsage: Long,
        val foreground: Boolean
    )

    /**
     * 初始化崩溃监控
     */
    fun init(
        context: Context,
        appVersion: String = "unknown",
        uploadUrl: String? = null,
        enableUpload: Boolean = false
    ) {
        this.context = context.applicationContext
        this.appVersion = appVersion
        this.uploadUrl = uploadUrl
        this.isUploadEnabled = enableUpload

        this.deviceInfo = buildString {
            append("Brand: ").append(android.os.Build.BRAND).append("\n")
            append("Model: ").append(android.os.Build.MODEL).append("\n")
            append("Manufacturer: ").append(android.os.Build.MANUFACTURER).append("\n")
            append("Hardware: ").append(android.os.Build.HARDWARE)
        }

        initCrashHandler()

        // 统计已有的崩溃数
        scope.launch {
            val count = getCrashLogCount()
            _crashCount.value = count
        }

        Log.i(TAG, "崩溃监控初始化完成，已有崩溃日志: ${_crashCount.value}")
    }

    /**
     * 初始化崩溃处理器（作为CrashHandler的补充）
     */
    private fun initCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 处理未捕获异常
     */
    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        scope.launch {
            try {
                val report = createCrashReport(thread, throwable)
                saveCrashReport(report)

                if (isUploadEnabled && uploadUrl != null) {
                    uploadCrashReport(report)
                }

                _crashCount.value++
            } catch (e: Exception) {
                Log.e(TAG, "处理崩溃异常失败", e)
            }
        }
    }

    /**
     * 创建崩溃报告
     */
    private fun createCrashReport(thread: Thread, throwable: Throwable): CrashReport {
        val runtime = Runtime.getRuntime()
        val memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        return CrashReport(
            crashId = "crash_${System.currentTimeMillis()}_${(0..9999).random()}",
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            deviceInfo = deviceInfo,
            androidVersion = android.os.Build.VERSION.RELEASE,
            crashType = getCrashType(throwable),
            exceptionName = throwable.javaClass.simpleName,
            message = throwable.message ?: "No message",
            stackTrace = sw.toString(),
            threadName = thread.name,
            memoryUsage = memoryUsage,
            foreground = isAppForeground()
        )
    }

    /**
     * 获取崩溃类型分类
     */
    private fun getCrashType(throwable: Throwable): String {
        return when (throwable) {
            is OutOfMemoryError -> "OOM"
            is StackOverflowError -> "SOE"
            is NullPointerException -> "NPE"
            is IllegalStateException -> "ISE"
            is IllegalArgumentException -> "IAE"
            is SecurityException -> "SEC"
            is ClassNotFoundException -> "CNFE"
            is NoSuchMethodError -> "NSME"
            else -> "OTHER"
        }
    }

    /**
     * 保存崩溃报告到本地
     */
    private suspend fun saveCrashReport(report: CrashReport) = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val dir = File(ctx.filesDir, CRASH_LOG_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "crash_${dateFormat.format(Date(report.timestamp))}.log"
        val file = File(dir, fileName)

        try {
            FileWriter(file).use { writer ->
                writer.write("===== OMaster Crash Report =====\n")
                writer.write("Crash ID: ${report.crashId}\n")
                writer.write("Timestamp: ${Date(report.timestamp)}\n")
                writer.write("App Version: ${report.appVersion}\n")
                writer.write("Android Version: ${report.androidVersion}\n")
                writer.write("Crash Type: ${report.crashType}\n")
                writer.write("Exception: ${report.exceptionName}\n")
                writer.write("Message: ${report.message}\n")
                writer.write("Thread: ${report.threadName}\n")
                writer.write("Memory: ${report.memoryUsage}MB\n")
                writer.write("Foreground: ${report.foreground}\n")
                writer.write("===== Device Info =====\n")
                writer.write("${report.deviceInfo}\n")
                writer.write("===== Stack Trace =====\n")
                writer.write("${report.stackTrace}\n")
                writer.write("===== End of Report =====\n")
            }
            cleanupOldLogs(dir)
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃报告失败", e)
        }
    }

    /**
     * 清理旧的崩溃日志
     */
    private fun cleanupOldLogs(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size > MAX_LOG_FILES) {
            val filesToDelete = files.size - MAX_LOG_FILES
            for (i in 0 until filesToDelete) {
                files[i].delete()
            }
        }

        var totalSize = 0L
        files.forEach { totalSize += it.length() }
        if (totalSize > MAX_LOG_SIZE) {
            for (file in files) {
                if (totalSize <= MAX_LOG_SIZE) break
                totalSize -= file.length()
                file.delete()
            }
        }
    }

    /**
     * 上传崩溃报告（可选）
     */
    private suspend fun uploadCrashReport(report: CrashReport) = withContext(Dispatchers.IO) {
        val url = uploadUrl ?: return@withContext
        try {
            val json = JSONObject().apply {
                put("crashId", report.crashId)
                put("timestamp", report.timestamp)
                put("appVersion", report.appVersion)
                put("androidVersion", report.androidVersion)
                put("crashType", report.crashType)
                put("exceptionName", report.exceptionName)
                put("message", report.message)
                put("stackTrace", report.stackTrace)
                put("threadName", report.threadName)
                put("memoryUsage", report.memoryUsage)
                put("deviceInfo", report.deviceInfo)
                put("platform", "android")
            }

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray())
                os.flush()
            }

            val responseCode = conn.responseCode
            Log.i(TAG, "崩溃报告上传结果: $responseCode")
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "上传崩溃报告失败", e)
        }
    }

    /**
     * 获取崩溃日志数量
     */
    suspend fun getCrashLogCount(): Int = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext 0
        val dir = File(ctx.filesDir, CRASH_LOG_DIR)
        if (!dir.exists()) return@withContext 0
        dir.listFiles()?.size ?: 0
    }

    /**
     * 获取所有崩溃报告列表
     */
    suspend fun getCrashReports(): List<String> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        val dir = File(ctx.filesDir, CRASH_LOG_DIR)
        if (!dir.exists()) return@withContext emptyList()

        val reports = mutableListOf<String>()
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return@withContext emptyList()

        for (file in files.take(10)) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    val content = reader.readText()
                    reports.add(content)
                }
            } catch (e: Exception) {
                Log.e(TAG, "读取崩溃日志失败: ${file.name}", e)
            }
        }
        reports
    }

    /**
     * 清除所有崩溃日志
     */
    suspend fun clearCrashLogs() = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext
        val dir = File(ctx.filesDir, CRASH_LOG_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
        _crashCount.value = 0
    }

    /**
     * 检查应用是否在前台
     */
    private fun isAppForeground(): Boolean {
        return try {
            val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val appProcesses = am?.runningAppProcesses ?: return false
            val packageName = context?.packageName ?: return false
            for (processInfo in appProcesses) {
                if (processInfo.processName == packageName) {
                    return processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取崩溃统计摘要
     */
    suspend fun getCrashSummary(): CrashSummary = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext CrashSummary(0, emptyMap(), 0)
        val dir = File(ctx.filesDir, CRASH_LOG_DIR)
        if (!dir.exists()) return@withContext CrashSummary(0, emptyMap(), 0)

        val files = dir.listFiles() ?: return@withContext CrashSummary(0, emptyMap(), 0)
        val typeCount = mutableMapOf<String, Int>()
        var totalSize = 0L

        for (file in files) {
            totalSize += file.length()
            try {
                val firstLine = file.useLines { lines ->
                    lines.find { it.startsWith("Crash Type:") }
                }
                val type = firstLine?.substringAfter("Crash Type: ")?.trim() ?: "UNKNOWN"
                typeCount[type] = (typeCount[type] ?: 0) + 1
            } catch (_: Exception) {
            }
        }

        CrashSummary(
            totalCount = files.size,
            typeDistribution = typeCount,
            totalSizeMB = totalSize / (1024 * 1024)
        )
    }

    /**
     * 崩溃统计摘要
     */
    data class CrashSummary(
        val totalCount: Int,
        val typeDistribution: Map<String, Int>,
        val totalSizeMB: Long
    )
}
