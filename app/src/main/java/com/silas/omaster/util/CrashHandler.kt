package com.silas.omaster.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.silas.omaster.BuildConfig
import com.silas.omaster.MainActivity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器 - 2026年最高稳定性标准
 *
 * 功能：
 * - 捕获所有未处理异常
 * - 记录详细崩溃日志到文件
 * - Release模式下尝试优雅恢复
 * - Debug模式下保持原有崩溃行为
 * - 监控Activity生命周期防止泄漏
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var applicationContext: Context? = null
    private var currentActivity: Activity? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 崩溃计数器（防止无限重启循环）
    private var crashCount = 0
    private var lastCrashTime = 0L
    private const val MAX_CRASH_COUNT = 3
    private const val CRASH_INTERVAL_MS = 60_000L // 1分钟内最多3次崩溃

    fun install(context: Context) {
        applicationContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        // 注册Activity生命周期监控
        if (context is Application) {
            context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        }
        
        Log.i(TAG, "CrashHandler 已安装，稳定性保护已启用")
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
            is RuntimeException -> "RE"
            else -> "OTHER"
        }

        Log.e(TAG, "异常类型: $exceptionType, 消息: ${throwable.message}")
        
        // 记录堆栈跟踪
        val stackTrace = Log.getStackTraceString(throwable)
        Log.e(TAG, "堆栈跟踪:\n$stackTrace")
        
        // 保存崩溃日志到文件
        saveCrashLogToFile(throwable, exceptionType, thread.name)
        
        // 检查崩溃频率（防止无限循环）
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCrashTime < CRASH_INTERVAL_MS) {
            crashCount++
        } else {
            crashCount = 1
        }
        lastCrashTime = currentTime
        
        if (crashCount > MAX_CRASH_COUNT) {
            Log.e(TAG, "崩溃次数过多($crashCount次)，停止自动恢复")
            // 委托给默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        // Release模式：尝试优雅恢复
        if (!BuildConfig.DEBUG) {
            Log.i(TAG, "Release模式：尝试优雅恢复应用")
            tryGracefulRecovery(throwable)
        } else {
            // Debug模式：保持原有崩溃行为
            Log.w(TAG, "Debug模式：保持原有崩溃行为")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 优雅恢复策略
     */
    private fun tryGracefulRecovery(throwable: Throwable) {
        try {
            // 1. 清理当前Activity（防止状态不一致）
            currentActivity?.let { activity ->
                Log.d(TAG, "清理当前Activity: ${activity.localClassName}")
                activity.finish()
            }
            
            // 2. 清理可能的内存泄漏
            clearMemoryLeaks()
            
            // 3. 重启应用（延迟执行）
            mainHandler.postDelayed({
                restartApp()
            }, 500L)
            
        } catch (e: Exception) {
            Log.e(TAG, "优雅恢复失败", e)
            // 最终委托给默认处理器
            defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
        }
    }
    
    /**
     * 重启应用
     */
    private fun restartApp() {
        applicationContext?.let { context ->
            try {
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("crash_recovery", true)
                context.startActivity(intent)
                
                Log.i(TAG, "应用已重启")
                
                // 杀死当前进程（确保干净重启）
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            } catch (e: Exception) {
                Log.e(TAG, "重启应用失败", e)
            }
        }
    }
    
    /**
     * 清理可能的内存泄漏
     */
    private fun clearMemoryLeaks() {
        try {
            // 清理静态引用
            currentActivity = null
            
            // 触发GC
            System.gc()
            
            Log.d(TAG, "内存泄漏清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "清理内存泄漏失败", e)
        }
    }
    
    /**
     * 保存崩溃日志到文件
     */
    private fun saveCrashLogToFile(throwable: Throwable, type: String, threadName: String) {
        try {
            applicationContext?.let { context ->
                val crashDir = File(context.filesDir, "crash_logs")
                if (!crashDir.exists()) crashDir.mkdirs()
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val crashFile = File(crashDir, "crash_$timestamp.log")
                
                FileWriter(crashFile, true).use { writer ->
                    writer.write("=== 崩溃日志 ===\n")
                    writer.write("时间: ${Date()}\n")
                    writer.write("线程: $threadName\n")
                    writer.write("类型: $type\n")
                    writer.write("消息: ${throwable.message}\n")
                    writer.write("堆栈:\n")
                    writer.write(Log.getStackTraceString(throwable))
                    writer.write("\n=== 结束 ===\n\n")
                }
                
                Log.d(TAG, "崩溃日志已保存: ${crashFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
    }
    
    /**
     * Activity生命周期监控
     */
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.d(TAG, "Activity创建: ${activity.localClassName}")
        }
        
        override fun onActivityStarted(activity: Activity) {
            Log.d(TAG, "Activity启动: ${activity.localClassName}")
        }
        
        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
            Log.d(TAG, "Activity恢复: ${activity.localClassName}")
        }
        
        override fun onActivityPaused(activity: Activity) {
            if (currentActivity == activity) currentActivity = null
            Log.d(TAG, "Activity暂停: ${activity.localClassName}")
        }
        
        override fun onActivityStopped(activity: Activity) {
            Log.d(TAG, "Activity停止: ${activity.localClassName}")
        }
        
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity == activity) currentActivity = null
            Log.d(TAG, "Activity销毁: ${activity.localClassName}")
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
