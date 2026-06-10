package com.silas.omaster.util

import android.util.Log
import com.silas.omaster.BuildConfig

/**
 * 统一日志工具
 *
 * 行业最严标准：
 * - Release 模式下自动屏蔽调试日志
 * - 统一 TAG 前缀便于过滤
 * - 避免 release 包泄漏敏感调试信息
 * - 替代散落在代码各处的 printStackTrace
 */
object LogUtil {
    private const val GLOBAL_TAG = "OMaster"

    /**
     * Debug 级别日志（仅 Debug 模式输出）
     */
    fun d(tag: String, msg: String) {
        if (BuildConfig.ENABLE_LOG) {
            Log.d("$GLOBAL_TAG/$tag", msg)
        }
    }

    /**
     * Info 级别日志（仅 Debug 模式输出）
     */
    fun i(tag: String, msg: String) {
        if (BuildConfig.ENABLE_LOG) {
            Log.i("$GLOBAL_TAG/$tag", msg)
        }
    }

    /**
     * Warn 级别日志（始终输出）
     */
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.w("$GLOBAL_TAG/$tag", msg, tr)
        } else {
            Log.w("$GLOBAL_TAG/$tag", msg)
        }
    }

    /**
     * Error 级别日志（始终输出）
     * Release 模式下也会记录到崩溃上报
     */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) {
            Log.e("$GLOBAL_TAG/$tag", msg, tr)
            // Release 模式上报到友盟
            if (!BuildConfig.ENABLE_LOG && BuildConfig.ENABLE_CRASH_REPORT) {
                try {
                    // 上报到友盟错误统计
                    com.umeng.analytics.MobclickAgent.reportError(
                        com.silas.omaster.OMasterApplication.getInstance(),
                        "$msg\n${android.util.Log.getStackTraceString(tr)}"
                    )
                } catch (_: Exception) {
                    // 忽略上报失败
                }
            }
        } else {
            Log.e("$GLOBAL_TAG/$tag", msg)
        }
    }

    /**
     * 统一异常处理入口
     * 替代散落的 e.printStackTrace()
     */
    fun logThrowable(tag: String, tr: Throwable, contextMsg: String = "") {
        val msg = if (contextMsg.isBlank()) "异常" else contextMsg
        if (BuildConfig.ENABLE_LOG) {
            Log.e("$GLOBAL_TAG/$tag", msg, tr)
        } else {
            // Release 模式仅记录简略信息
            Log.e("$GLOBAL_TAG/$tag", "$msg: ${tr.javaClass.simpleName}: ${tr.message}")
            if (BuildConfig.ENABLE_CRASH_REPORT) {
                try {
                    com.umeng.analytics.MobclickAgent.reportError(
                        com.silas.omaster.OMasterApplication.getInstance(),
                        "[$tag] $msg\n${android.util.Log.getStackTraceString(tr)}"
                    )
                } catch (_: Exception) {
                    // 忽略上报失败
                }
            }
        }
    }

    /**
     * 网络请求日志（仅 Debug 模式 + 显式开启网络日志时）
     */
    fun net(msg: String) {
        if (BuildConfig.ENABLE_NET_LOG) {
            Log.d("$GLOBAL_TAG/Net", msg)
        }
    }
}
