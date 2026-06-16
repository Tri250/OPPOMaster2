package com.silas.omaster.util

import android.content.Context
import android.util.Log
import com.silas.omaster.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器
 *
 * 防止应用因未捕获的异常直接崩溃退出，提升稳定性
 * 行为：
 * - 记录异常到日志文件（持久化，便于事后分析）
 * - 分类记录异常类型
 * - 委托给默认处理器
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null
    private var installed: Boolean = false

    fun install(context: Context? = null) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        appContext = context?.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(this)
        installed = true
        Log.i(TAG, "CrashHandler 已安装")
    }

    fun isInstalled(): Boolean = installed

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. 分类记录异常类型
            val exceptionType = when (throwable) {
                is NullPointerException -> "NPE"
                is IllegalStateException -> "ISE"
                is IllegalArgumentException -> "IAE"
                is IndexOutOfBoundsException -> "IOOBE"
                is ClassCastException -> "CCE"
                is SecurityException -> "SEC"
                is OutOfMemoryError -> "OOM"
                is StackOverflowError -> "SOE"
                else -> "OTHER"
            }

            // 2. 构建详细信息（OOM 时限制堆栈深度避免再次 OOM）
            val detailInfo = buildString {
                append("==== Crash Report ====\n")
                append("时间: ${getCurrentTime()}\n")
                append("线程: ${thread.name} (id=${thread.id})\n")
                append("类型: $exceptionType\n")
                append("消息: ${throwable.message}\n")
                append("==== 堆栈 ====\n")
                // OOM/SOE 限深：避免递归递归
                val maxDepth = if (throwable is OutOfMemoryError || throwable is StackOverflowError) 20 else 100
                append(getStackTraceSafe(throwable, maxDepth))
                append("\n")
            }

            // 3. 写入 Logcat
            Log.e(TAG, "未捕获异常 [${thread.name}] $exceptionType: ${throwable.message}", throwable)

            // 4. 持久化到文件（重要：让下次启动能读取）
            persistCrashReport(detailInfo, exceptionType)

        } catch (e: Throwable) {
            // CrashHandler 本身绝不能再崩
            try {
                Log.e(TAG, "CrashHandler 处理异常时再次失败", e)
            } catch (_: Throwable) {
                // 完全放弃
            }
        }

        // 5. 委托给默认处理器（最终由系统决定是否退出）
        // 注意：必须在 finally 之外，避免前序代码异常时跳过记录
        try {
            defaultHandler?.uncaughtException(thread, throwable)
                ?: run {
                    // 没有默认处理器，手动退出
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(10)
                }
        } catch (e: Throwable) {
            // 默认处理器也失败，强杀进程
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(11)
        }
    }

    /**
     * 安全获取堆栈（限制深度避免OOM）
     */
    private fun getStackTraceSafe(throwable: Throwable, maxDepth: Int): String {
        return try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val full = sw.toString()
            // 限制行数
            val lines = full.split('\n')
            if (lines.size > maxDepth) {
                lines.take(maxDepth).joinToString("\n") + "\n... (truncated, total ${lines.size} lines)"
            } else {
                full
            }
        } catch (e: OutOfMemoryError) {
            "Stack trace unavailable (OOM while printing): ${throwable.javaClass.name}"
        } catch (e: Throwable) {
            "Stack trace unavailable: ${e.javaClass.name}: ${e.message}"
        }
    }

    /**
     * 持久化崩溃报告到文件
     * 修复 P2-9: 隐私保护 - 不存储可能包含敏感信息的路径/IP
     */
    private fun persistCrashReport(content: String, type: String) {
        val ctx = appContext ?: return
        try {
            val crashDir = File(ctx.filesDir, "crash_logs")
            if (!crashDir.exists()) crashDir.mkdirs()

            // 隐私保护：过滤敏感信息
            val sanitizedContent = sanitizeCrashReport(content)

            val fileName = "crash_${getCurrentTime().replace(":", "-").replace(" ", "_")}_$type.log"
            val crashFile = File(crashDir, fileName)
            crashFile.writeText(sanitizedContent)

            // 清理旧日志（只保留最近 10 个）
            cleanupOldCrashLogs(crashDir, keepCount = 10)
        } catch (e: Throwable) {
            Log.w(TAG, "持久化崩溃日志失败", e)
        }
    }

    /**
     * 清理崩溃报告中的敏感信息
     * 移除：文件路径、IP地址、可能的凭证信息
     */
    private fun sanitizeCrashReport(content: String): String {
        return content
            // 移除文件路径（保留文件名）
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            // 移除 IP 地址
            .replace(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"), "[IP_REDACTED]")
            // 移除可能的 token/key（简单启发式）
            .replace(Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
    }

    /**
     * 清理旧崩溃日志
     */
    private fun cleanupOldCrashLogs(dir: File, keepCount: Int) {
        try {
            val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
            if (files.size > keepCount) {
                files.drop(keepCount).forEach { it.delete() }
            }
        } catch (_: Throwable) {
            // 忽略清理失败
        }
    }

    /**
     * 获取当前时间字符串
     */
    private fun getCurrentTime(): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        } catch (_: Throwable) {
            System.currentTimeMillis().toString()
        }
    }

    companion object {
        private const val TAG = "CrashHandler"

        @Volatile
        private var instance: CrashHandler? = null

        fun getInstance(): CrashHandler =
            instance ?: synchronized(this) {
                instance ?: CrashHandler().also { instance = it }
            }
    }
}
