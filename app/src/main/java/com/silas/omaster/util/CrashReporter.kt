package com.silas.omaster.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 【v1.6 启动闪退诊断】全局未捕获异常捕获器
 *
 * 背景：release 包在冷启动阶段闪退时，真机（非调试）Logcat 极难获取，
 * 历史多次"闪退 → 无日志 → 盲改 ProGuard 规则 → 仍然闪退"即源于此。
 *
 * 本工具在进程最早阶段安装 Thread.UncaughtExceptionHandler，把完整堆栈落盘：
 *   1) 内部存储 filesDir/logs/crash.log
 *   2) 外部私有存储 Android/data/<pkg>/files/crash.log —— 可用文件管理器直接取走
 * 之后仍转交系统默认处理器，进程终止行为保持不变。
 */
object CrashReporter {

    private const val TAG = "OMasterCrash"
    private const val LOG_DIR = "logs"
    private const val CRASH_FILE = "crash.log"

    @Volatile
    private var installed = false

    /**
     * 安装崩溃捕获器，必须在 Application.onCreate 最早期调用（越早越安全）。
     */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                persist(appContext, thread, throwable)
            } catch (_: Throwable) {
                // 诊断自身失败绝不能影响原崩溃流程
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** 读取上一次崩溃报告（不存在返回 null） */
    fun getCrashReport(context: Context): String? {
        val file = File(File(context.filesDir, LOG_DIR), CRASH_FILE)
        return if (file.exists()) runCatching { file.readText() }.getOrNull() else null
    }

    fun clear(context: Context) {
        File(File(context.filesDir, LOG_DIR), CRASH_FILE).delete()
        externalFile(context)?.delete()
    }

    private fun externalFile(context: Context): File? =
        context.getExternalFilesDir(null)?.let { File(it, CRASH_FILE) }

    private fun persist(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val stack = StringWriter().also { sw -> throwable.printStackTrace(PrintWriter(sw)) }.toString()
        val report = buildString {
            append("==================== OMaster Crash Report ====================\n")
            append("Time: ").append(timestamp).append('\n')
            append("Thread: ").append(thread.name).append('\n')
            append("Version: ").append(VersionInfo.VERSION_NAME)
            append(" (").append(VersionInfo.VERSION_CODE).append(")\n")
            append("Android SDK: ").append(Build.VERSION.SDK_INT).append('\n')
            append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("ABI: ").append(Build.SUPPORTED_ABIS.joinToString(", ")).append('\n')
            append("--- stack trace ---\n")
            append(stack).append('\n')
            append("==============================================================\n")
        }

        // ① 内部存储
        try {
            val dir = File(context.filesDir, LOG_DIR).apply { if (!exists()) mkdirs() }
            File(dir, CRASH_FILE).writeText(report)
        } catch (_: Throwable) {
        }

        // ② 外部私有存储（用户侧可取回）
        externalFile(context)?.let { file ->
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(report)
            }
        }

        android.util.Log.e(TAG, report)
    }
}
