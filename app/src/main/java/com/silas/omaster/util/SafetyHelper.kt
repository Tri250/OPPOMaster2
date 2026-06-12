package com.silas.omaster.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * 安全性辅助工具类 - 2026年最高稳定性标准
 *
 * 功能：
 * - 空指针安全访问
 * - 资源安全释放
 * - 异常安全处理
 * - 线程安全操作
 */
object SafetyHelper {

    const val TAG = "SafetyHelper"

    /**
     * 安全获取值，避免空指针
     * @param value 可能为空的值
     * @param default 默认值
     * @return 非空值
     */
    inline fun <T> safeGet(value: T?, default: T): T = value ?: default

    /**
     * 安全执行操作，捕获所有异常
     * @param block 要执行的操作
     * @param onError 错误回调
     * @return 操作结果，失败返回null
     */
    inline fun <T> safeRun(block: () -> T, noinline onError: ((Exception) -> Unit)? = null): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "safeRun 异常: ${e.message}", e)
            onError?.invoke(e)
            null
        }
    }

    /**
     * 安全执行操作，返回默认值
     * @param block 要执行的操作
     * @param default 默认值
     * @return 操作结果，失败返回默认值
     */
    inline fun <T> safeRunWithDefault(block: () -> T, default: T): T {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "safeRunWithDefault 异常: ${e.message}", e)
            default
        }
    }

    /**
     * 安全释放资源
     * @param closeable 可关闭的资源
     */
    fun safeClose(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Exception) {
            Log.e(TAG, "safeClose 异常: ${e.message}", e)
        }
    }

    /**
     * 安全释放Bitmap
     * @param bitmap Bitmap对象
     */
    fun safeRecycleBitmap(bitmap: Bitmap?) {
        try {
            bitmap?.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "safeRecycleBitmap 异常: ${e.message}", e)
        }
    }

    /**
     * 安全关闭流
     * @param stream 输入/输出流
     */
    fun safeCloseStream(stream: Closeable?) {
        safeClose(stream)
    }

    /**
     * 安全获取Context
     * @param context 可能为空的Context
     * @return 应用Context（applicationContext）
     */
    fun safeContext(context: Context?): Context? {
        return context?.applicationContext
    }

    /**
     * 安全获取StateFlow值
     * @param flow StateFlow
     * @param default 默认值
     * @return 非空值
     */
    inline fun <T> safeFlowValue(flow: StateFlow<T?>, default: T): T = flow.value ?: default

    /**
     * 安全更新MutableStateFlow
     * @param flow MutableStateFlow
     * @param value 新值
     */
    inline fun <T> safeUpdateFlow(flow: MutableStateFlow<T>, value: T) {
        try {
            flow.value = value
        } catch (e: Exception) {
            Log.e(TAG, "safeUpdateFlow 异常: ${e.message}", e)
        }
    }

    /**
     * 安全执行异步操作
     * @param block 异步操作
     * @param onError 错误回调
     */
    inline fun safeAsync(crossinline block: () -> Unit, noinline onError: ((Exception) -> Unit)? = null) {
        try {
            Thread {
                try {
                    block()
                } catch (e: Exception) {
                    Log.e(TAG, "safeAsync 异常: ${e.message}", e)
                    onError?.invoke(e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "safeAsync 启动线程异常: ${e.message}", e)
            onError?.invoke(e)
        }
    }

    /**
     * 安全检查数组边界
     * @param array 数组
     * @param index 索引
     * @return 是否在有效范围内
     */
    inline fun <T> isValidIndex(array: Array<T>, index: Int): Boolean {
        return index >= 0 && index < array.size
    }

    /**
     * 安全检查列表边界
     * @param list 列表
     * @param index 索引
     * @return 是否在有效范围内
     */
    inline fun <T> isValidIndex(list: List<T>, index: Int): Boolean {
        return index >= 0 && index < list.size
    }

    /**
     * 安全获取数组元素
     * @param array 数组
     * @param index 索引
     * @param default 默认值
     * @return 元素值，索引无效返回默认值
     */
    inline fun <T> safeArrayGet(array: Array<T>, index: Int, default: T): T {
        return if (isValidIndex(array, index)) array[index] else default
    }

    /**
     * 安全获取列表元素
     * @param list 列表
     * @param index 索引
     * @param default 默认值
     * @return 元素值，索引无效返回默认值
     */
    inline fun <T> safeListGet(list: List<T>, index: Int, default: T): T {
        return if (isValidIndex(list, index)) list[index] else default
    }

    /**
     * 安全转换字符串
     * @param value 任意值
     * @return 字符串表示，null返回空字符串
     */
    fun safeToString(value: Any?): String {
        return value?.toString() ?: ""
    }

    /**
     * 安全解析整数
     * @param str 字符串
     * @param default 默认值
     * @return 整数值，解析失败返回默认值
     */
    fun safeParseInt(str: String?, default: Int = 0): Int {
        return try {
            str?.toInt() ?: default
        } catch (e: NumberFormatException) {
            Log.e(TAG, "safeParseInt 异常: ${e.message}")
            default
        }
    }

    /**
     * 安全解析浮点数
     * @param str 字符串
     * @param default 默认值
     * @return 浮点数值，解析失败返回默认值
     */
    fun safeParseFloat(str: String?, default: Float = 0f): Float {
        return try {
            str?.toFloat() ?: default
        } catch (e: NumberFormatException) {
            Log.e(TAG, "safeParseFloat 异常: ${e.message}")
            default
        }
    }

    /**
     * 安全解析布尔值
     * @param str 字符串
     * @param default 默认值
     * @return 布尔值，解析失败返回默认值
     */
    fun safeParseBoolean(str: String?, default: Boolean = false): Boolean {
        return when (str?.lowercase()) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off" -> false
            null -> default
            else -> default
        }
    }

    /**
     * 安全执行多次尝试
     * @param maxRetries 最大重试次数
     * @param delayMs 重试间隔（毫秒）
     * @param block 要执行的操作
     * @return 操作结果
     */
    inline fun <T> safeRetry(maxRetries: Int, delayMs: Long, block: (Int) -> T): T? {
        var lastException: Exception? = null
        for (i in 0 until maxRetries) {
            try {
                return block(i)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "safeRetry 第${i + 1}次失败: ${e.message}")
                if (i < maxRetries - 1) {
                    Thread.sleep(delayMs)
                }
            }
        }
        Log.e(TAG, "safeRetry 全部失败", lastException)
        return null
    }

    /**
     * 安全检查内存状态
     * @param context Context
     * @return 是否内存充足
     */
    fun isMemoryAvailable(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return !memoryInfo.lowMemory
    }

    /**
     * 安全获取可用内存
     * @param context Context
     * @return 可用内存（MB）
     */
    fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * 安全触发GC（仅在内存紧张时）
     * @param context Context
     */
    fun safeGC(context: Context) {
        if (!isMemoryAvailable(context)) {
            Log.w(TAG, "内存紧张，触发GC")
            System.gc()
        }
    }
}