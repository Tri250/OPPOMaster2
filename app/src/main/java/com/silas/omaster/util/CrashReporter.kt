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
 * 【v1.6 启动闪退诊断】全局未捕获异常捕获器 + 启动里程碑追踪
 *
 * 背景：release 包在冷启动阶段闪退时，真机（非调试）Logcat 极难获取，
 * 且 Android 11+ 文件管理器无法访问 Android/data，历史多次
 * "闪退 → 无日志 → 盲改 ProGuard 规则 → 仍然闪退"即源于此。
 *
 * v1.6.10 升级：
 *   1) 崩溃堆栈落盘（crash.log）——同 v1.6.8；
 *   2) 启动里程碑打点（boot.log）——记录 attach/onCreate/composition 各阶段；
 *   3) 【关键】即使进程"无 Java 异常死亡"（native 崩溃 / 系统杀进程 / ANR /
 *      ContentProvider 阶段崩溃，UncaughtExceptionHandler 捕不到），下次启动
 *      也能依据残留里程碑生成诊断报告，由 App 内弹窗直接展示并可一键分享，
 *      彻底摆脱对文件管理器 / adb 的依赖。
 */
object CrashReporter {

    private const val TAG = "OMasterCrash"
    private const val LOG_DIR = "logs"
    private const val CRASH_FILE = "crash.log"
    private const val BOOT_FILE = "boot.log"

    @Volatile
    private var installed = false

    /** 上一次运行遗留的里程碑轨迹（install 时读取并持有；null = 上次正常或首次安装） */
    @Volatile
    private var previousRunTrail: String? = null

    /**
     * 安装崩溃捕获器。必须在【最早期】调用（Application.attachBaseContext，
     * 早于 ContentProvider 安装与 Application.onCreate，覆盖面最大）。
     */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext

        // 读取并清除上次运行的里程碑轨迹：
        // 若 boot.log 仍存在，说明上次运行未走到 bootSucceeded()（进程中途死亡）
        previousRunTrail = try {
            val boot = File(File(appContext.filesDir, LOG_DIR), BOOT_FILE)
            if (boot.exists()) boot.readText().trim().takeIf { it.isNotEmpty() } else null
        } catch (_: Throwable) {
            null
        }
        try {
            File(File(appContext.filesDir, LOG_DIR), BOOT_FILE).delete()
        } catch (_: Throwable) {
        }

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

    /** 启动里程碑打点（各关键阶段调用；失败静默，绝不影响启动） */
    fun mark(context: Context, stage: String) {
        try {
            val dir = File(context.filesDir, LOG_DIR).apply { if (!exists()) mkdirs() }
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            File(dir, BOOT_FILE).appendText("[$ts] $stage\n")
        } catch (_: Throwable) {
        }
    }

    /** UI 完全可见（首个 Activity.onResume）后调用：标记本次启动成功，清除里程碑 */
    fun bootSucceeded(context: Context) {
        try {
            File(File(context.filesDir, LOG_DIR), BOOT_FILE).delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * 获取待展示的诊断报告（App 内弹窗用，无需文件管理器）：
     *   ① 有 Java 崩溃报告 → 返回完整堆栈（附带上次运行里程碑作上下文）；
     *   ② 无 Java 崩溃但上次运行中途死亡 → 返回里程碑轨迹 + 死亡类型推断；
     *   ③ 无异常 → null。
     */
    fun getPendingDiagnostics(context: Context): String? {
        val crash = getCrashReport(context)
        if (crash != null) {
            val trail = previousRunTrail
            return if (trail != null) "$trail\n\n$crash" else crash
        }
        val trail = previousRunTrail ?: return null
        return buildString {
            append("==== 启动异常终止（未捕获到 Java 异常）====\n")
            append("进程在上次启动中途死亡，且 UncaughtExceptionHandler 未触发。\n")
            append("可能原因：native 崩溃(SIGSEGV/SIGABRT)、ANR、系统杀进程、\n")
            append("或 ContentProvider/静态初始化阶段崩溃（早于异常处理器覆盖）。\n")
            append("---- 死亡前最后里程碑 ----\n")
            append(trail).append('\n')
            append("==========================================\n")
        }
    }

    /** 诊断已确认（用户看完/分享后），清除报告避免重复弹窗 */
    fun clearDiagnostics(context: Context) {
        previousRunTrail = null
        try {
            File(File(context.filesDir, LOG_DIR), CRASH_FILE).delete()
        } catch (_: Throwable) {
        }
        try {
            externalFile(context)?.delete()
        } catch (_: Throwable) {
        }
    }

    /** 读取崩溃报告（不存在返回 null） */
    fun getCrashReport(context: Context): String? {
        val file = File(File(context.filesDir, LOG_DIR), CRASH_FILE)
        return if (file.exists()) runCatching { file.readText() }.getOrNull() else null
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
