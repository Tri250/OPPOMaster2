package com.silas.omaster

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.SystemClock
import android.util.Log

/**
 * 基于 ContentProvider 的关键早期组件初始化
 *
 * ContentProvider.onCreate() 在 Application.attachBaseContext() 之后、Application.onCreate() 之前调用，
 * 因此适合初始化那些必须在 Application 完全启动前就绪的关键组件。
 *
 * 注意：此处仅初始化真正"关键"的组件，避免在此执行耗时操作拖慢冷启动。
 */
class InitializationProvider : ContentProvider() {

    companion object {
        private const val TAG = "InitProvider"

        @Volatile
        private var earlyInitCompleted = false

        fun isEarlyInitCompleted(): Boolean = earlyInitCompleted
    }

    override fun onCreate(): Boolean {
        val startTime = SystemClock.elapsedRealtime()
        val context = context ?: run {
            Log.e(TAG, "ContentProvider 上下文为 null，无法完成启动初始化")
            return true // v2.3.6 关键修复：即使 context 为 null 也返回 true，避免 ContentProvider 启动失败
        }

        // v2.3.6 关键修复：每一步独立 try-catch，任何一步异常都不能中断 ContentProvider 启动
        // 否则 Application.onCreate 不会被调用，App 直接黑屏/崩溃

        // 第 1 步：初始化 SharedPreferences（其他组件可能依赖它）
        try {
            val prefsStart = SystemClock.elapsedRealtime()
            OMasterApplication.initializePrefs(context)
            Log.d(TAG, "SharedPreferences 初始化耗时: ${SystemClock.elapsedRealtime() - prefsStart}ms")
        } catch (t: Throwable) {
            Log.e(TAG, "SharedPreferences 初始化失败（已忽略）", t)
        }

        // 第 2 步：预初始化全局异常处理器（捕获后续初始化阶段的崩溃）
        try {
            val crashStart = SystemClock.elapsedRealtime()
            com.silas.omaster.infrastructure.utils.CrashHandler.getInstance().install(context)
            Log.d(TAG, "CrashHandler 初始化耗时: ${SystemClock.elapsedRealtime() - crashStart}ms")
        } catch (t: Throwable) {
            Log.e(TAG, "CrashHandler 安装失败（已忽略）", t)
        }

        // 第 3 步：写入启动日志
        try {
            com.silas.omaster.infrastructure.utils.CrashHandler.getInstance()
                .logInfo("InitializationProvider", "ContentProvider 启动完成, 耗时 ${SystemClock.elapsedRealtime() - startTime}ms")
        } catch (t: Throwable) {
            Log.e(TAG, "写入启动日志失败（已忽略）", t)
        }

        earlyInitCompleted = true
        Log.i(TAG, "早期初始化完成，总耗时: ${SystemClock.elapsedRealtime() - startTime}ms")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}