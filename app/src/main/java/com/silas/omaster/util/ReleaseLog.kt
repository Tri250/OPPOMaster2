package com.silas.omaster.util

import android.util.Log
import com.silas.omaster.BuildConfig

/**
 * Release 安全的日志工具类
 *
 * 在 Debug 构建中正常输出所有日志
 * 在 Release 构建中禁用 DEBUG、INFO、VERBOSE 日志，仅保留 WARN 和 ERROR
 *
 * 使用方式：将 `android.util.Log.xxx()` 替换为 `ReleaseLog.xxx()`
 */
object ReleaseLog {

    /**
     * 输出 DEBUG 级别日志
     * Release 构建中会被禁用
     */
    fun d(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        } else {
            0
        }
    }

    /**
     * 输出 DEBUG 级别日志（带异常）
     * Release 构建中会被禁用
     */
    fun d(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) {
            Log.d(tag, msg, tr)
        } else {
            0
        }
    }

    /**
     * 输出 INFO 级别日志
     * Release 构建中会被禁用
     */
    fun i(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) {
            Log.i(tag, msg)
        } else {
            0
        }
    }

    /**
     * 输出 INFO 级别日志（带异常）
     * Release 构建中会被禁用
     */
    fun i(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) {
            Log.i(tag, msg, tr)
        } else {
            0
        }
    }

    /**
     * 输出 VERBOSE 级别日志
     * Release 构建中会被禁用
     */
    fun v(tag: String, msg: String): Int {
        return if (BuildConfig.DEBUG) {
            Log.v(tag, msg)
        } else {
            0
        }
    }

    /**
     * 输出 VERBOSE 级别日志（带异常）
     * Release 构建中会被禁用
     */
    fun v(tag: String, msg: String, tr: Throwable): Int {
        return if (BuildConfig.DEBUG) {
            Log.v(tag, msg, tr)
        } else {
            0
        }
    }

    /**
     * 输出 WARN 级别日志
     * Release 构建中保留
     */
    fun w(tag: String, msg: String): Int {
        return Log.w(tag, msg)
    }

    /**
     * 输出 WARN 级别日志（带异常）
     * Release 构建中保留
     */
    fun w(tag: String, msg: String, tr: Throwable): Int {
        return Log.w(tag, msg, tr)
    }

    /**
     * 输出 ERROR 级别日志
     * Release 构建中保留
     */
    fun e(tag: String, msg: String): Int {
        return Log.e(tag, msg)
    }

    /**
     * 输出 ERROR 级别日志（带异常）
     * Release 构建中保留
     */
    fun e(tag: String, msg: String, tr: Throwable): Int {
        return Log.e(tag, msg, tr)
    }

    /**
     * 输出 WTF (What a Terrible Failure) 级别日志
     * Release 构建中保留（严重错误）
     */
    fun wtf(tag: String, msg: String): Int {
        return Log.wtf(tag, msg)
    }

    /**
     * 输出 WTF (What a Terrible Failure) 级别日志（带异常）
     * Release 构建中保留（严重错误）
     */
    fun wtf(tag: String, msg: String, tr: Throwable): Int {
        return Log.wtf(tag, msg, tr)
    }

    /**
     * 判断是否为 Debug 构建
     * 可用于条件性执行调试代码
     */
    fun isDebug(): Boolean = BuildConfig.DEBUG
}
