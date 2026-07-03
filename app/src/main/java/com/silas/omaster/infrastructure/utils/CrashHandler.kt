package com.silas.omaster.infrastructure.utils

import android.content.Context
import android.util.Log
import com.silas.omaster.BuildConfig
import com.silas.omaster.infrastructure.security.SecurityCrypto
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

    /**
     * 崩溃回调监听器列表（线程安全）
     * 其他组件（如 CrashMonitorManager）可通过此接口接收崩溃事件，
     * 避免多个组件争抢 Thread.setDefaultUncaughtExceptionHandler 导致覆盖。
     */
    private val crashListeners = java.util.concurrent.CopyOnWriteArrayList<CrashListener>()

    /**
     * 崩溃事件监听器
     */
    interface CrashListener {
        fun onCrash(thread: Thread, throwable: Throwable, exceptionType: String)
    }

    fun addCrashListener(listener: CrashListener) {
        if (!crashListeners.contains(listener)) {
            crashListeners.add(listener)
        }
    }

    fun removeCrashListener(listener: CrashListener) {
        crashListeners.remove(listener)
    }

    fun install(context: Context? = null) {
        // v2.3.6 修复：防止重复安装导致 defaultHandler 指向自身，
        // 进而在 uncaughtException 中无限递归引发 StackOverflowError。
        if (installed) {
            Log.d(TAG, "CrashHandler 已安装，跳过重复安装")
            return
        }
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        appContext = context?.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(this)
        installed = true
        Log.i(TAG, "CrashHandler 已安装")
    }

    fun isInstalled(): Boolean = installed

    /**
     * 记录普通信息到 Logcat（可由 InitializationProvider 等早期组件调用）
     * 不抛出异常，确保启动链路不被打断。
     */
    fun logInfo(tag: String, message: String) {
        try {
            Log.i(tag, message)
        } catch (_: Throwable) {
            // 忽略日志失败
        }
    }

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

            // 5. 通知所有已注册的崩溃监听器（如 CrashMonitorManager）
            // 逐个通知，单个监听器异常不影响其他监听器
            for (listener in crashListeners) {
                try {
                    listener.onCrash(thread, throwable, exceptionType)
                } catch (e: Throwable) {
                    Log.e(TAG, "崩溃监听器 ${listener.javaClass.simpleName} 处理失败", e)
                }
            }

        } catch (e: Throwable) {
            // CrashHandler 本身绝不能再崩
            try {
                Log.e(TAG, "CrashHandler 处理异常时再次失败", e)
            } catch (_: Throwable) {
                // 完全放弃
            }
        }

        // 6. 委托给默认处理器（最终由系统决定是否退出）
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
            val sb = StringBuilder()
            // 手动构建堆栈，避免 printStackTrace 输出到系统日志
            sb.append(throwable.toString()).append('\n')
            val stackTrace = throwable.stackTrace
            val limit = minOf(stackTrace.size, maxDepth)
            for (i in 0 until limit) {
                sb.append("\tat ").append(stackTrace[i]).append('\n')
            }
            if (stackTrace.size > maxDepth) {
                sb.append("\t... (truncated, total ${stackTrace.size} frames)\n")
            }
            // 处理 cause 链（限深避免循环引用）
            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < 3) {
                sb.append("Caused by: ").append(cause.toString()).append('\n')
                val causeTrace = cause.stackTrace
                val causeLimit = minOf(causeTrace.size, maxDepth)
                for (i in 0 until causeLimit) {
                    sb.append("\tat ").append(causeTrace[i]).append('\n')
                }
                if (causeTrace.size > maxDepth) {
                    sb.append("\t... (truncated, total ${causeTrace.size} frames)\n")
                }
                cause = cause.cause
                depth++
            }
            sb.toString()
        } catch (e: OutOfMemoryError) {
            "Stack trace unavailable (OOM while printing): ${throwable.javaClass.name}"
        } catch (e: Throwable) {
            "Stack trace unavailable: ${e.javaClass.name}: ${e.message}"
        }
    }

    /**
     * 持久化崩溃报告到文件
     * 修复 P2-9: 隐私保护 - 不存储可能包含敏感信息的路径/IP
     * 修复 L2: 崩溃日志文件加密存储，防止 root 后泄露业务逻辑
     */
    private fun persistCrashReport(content: String, type: String) {
        val ctx = appContext ?: return
        try {
            val crashDir = File(ctx.filesDir, "crash_logs")
            if (!crashDir.exists()) crashDir.mkdirs()

            // 隐私保护：过滤敏感信息
            val sanitizedContent = sanitizeCrashReport(content)

            // 加密存储：使用 AES/GCM + Android Keystore
            val encryptedContent = try {
                SecurityCrypto.encrypt(sanitizedContent)
            } catch (e: Exception) {
                Log.w(TAG, "日志加密失败，降级为明文存储", e)
                sanitizedContent
            }

            val fileName = "crash_${getCurrentTime().replace(":", "-").replace(" ", "_")}_$type.enc"
            val crashFile = File(crashDir, fileName)
            crashFile.writeText(encryptedContent ?: sanitizedContent)

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
