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
        val context = context ?: return false

        try {
            // 第 1 步：初始化 SharedPreferences（其他组件可能依赖它）
            val prefsStart = SystemClock.elapsedRealtime()
            OMasterApplication.initializePrefs(context)
            Log.d(TAG, "SharedPreferences 初始化耗时: ${SystemClock.elapsedRealtime() - prefsStart}ms")

            // 第 2 步：预初始化全局异常处理器（捕获后续初始化阶段的崩溃）
            val crashStart = SystemClock.elapsedRealtime()
            try {
                com.silas.omaster.infrastructure.utils.CrashHandler.getInstance().install(context)
            } catch (e: Throwable) {
                Log.e(TAG, "CrashHandler 预安装失败", e)
            }
            Log.d(TAG, "CrashHandler 初始化耗时: ${SystemClock.elapsedRealtime() - crashStart}ms")

            earlyInitCompleted = true
            Log.i(TAG, "早期初始化完成，总耗时: ${SystemClock.elapsedRealtime() - startTime}ms")

        } catch (e: Throwable) {
            Log.e(TAG, "早期初始化失败", e)
        }

        // 返回 true 表示 Provider 已成功创建
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