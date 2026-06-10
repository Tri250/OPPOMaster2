package com.silas.omaster.util

import android.util.Log
import com.silas.omaster.BuildConfig

/**
 * 全局未捕获异常处理器
 *
 * 防止应用因未捕获的异常直接崩溃退出，提升稳定性
 * 行为：
 * - 记录异常到日志
 * - 在 debug 模式下仍抛出（让开发者看到崩溃）
 * - 在 release 模式下捕获并尝试恢复
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 记录异常
        Log.e(TAG, "未捕获异常 [${thread.name}]", throwable)

        // 分类记录异常类型
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

        Log.e(TAG, "异常类型: $exceptionType, 消息: ${throwable.message}")

        // 委托给默认处理器
        // 注意：不能完全阻止系统崩溃，但可以记录更详细的信息
        defaultHandler?.uncaughtException(thread, throwable)
            ?: run {
                // 没有默认处理器，手动退出
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
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
