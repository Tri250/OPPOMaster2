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
 * 参考 iCurrer/OMaster 的简洁设计原则：
 * - 仅初始化真正"关键"的组件
 * - 每一步独立 try-catch，任何失败都不阻断启动
 * - 避免耗时操作，确保冷启动速度
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
            Log.e(TAG, "ContentProvider 上下文为 null，跳过早期初始化")
            return true
        }

        try {
            OMasterApplication.initializePrefs(context)
        } catch (t: Throwable) {
            Log.e(TAG, "SharedPreferences 初始化失败（已忽略）", t)
        }

        try {
            com.silas.omaster.infrastructure.utils.CrashHandler.getInstance().install(context)
        } catch (t: Throwable) {
            Log.e(TAG, "CrashHandler 安装失败（已忽略）", t)
        }

        try {
            com.silas.omaster.infrastructure.utils.CrashHandler.getInstance()
                .logInfo("InitializationProvider", "早期初始化完成, 耗时 ${SystemClock.elapsedRealtime() - startTime}ms")
        } catch (_: Throwable) {}

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