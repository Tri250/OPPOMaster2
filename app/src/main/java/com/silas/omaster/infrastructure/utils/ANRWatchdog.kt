package com.silas.omaster.infrastructure.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.sentry.Sentry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ANR 看门狗
 *
 * 在主线程上周期性地执行检测任务，当主线程阻塞超过阈值时，
 * 记录 ANR 事件并上报到 Sentry。
 *
 * 使用方式：
 * - 在 Application.onCreate() 中调用 ANRWatchdog.install()
 * - 在 Application.onTerminate() 中调用 ANRWatchdog.uninstall()
 *
 * 注意：此工具仅用于检测和上报，不会阻止系统 ANR 对话框弹出。
 * 与 StrictMode 配合使用效果最佳。
 */
object ANRWatchdog {

    private const val TAG = "ANRWatchdog"
    private const val TICK_INTERVAL_MS = 2000L  // 每 2 秒检测一次
    private const val ANR_THRESHOLD_MS = 5000L  // 主线程阻塞超过 5 秒视为 ANR

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _anrCount = MutableStateFlow(0)
    val anrCount: StateFlow<Int> = _anrCount.asStateFlow()

    private var lastTickTime = 0L
    private var running = false
    private var installTime = 0L

    /**
     * 安装 ANR 看门狗
     */
    fun install() {
        if (running) {
            Log.w(TAG, "ANR看门狗已在运行")
            return
        }
        running = true
        installTime = System.currentTimeMillis()
        lastTickTime = installTime

        // 检测线程：定期检查主线程是否及时响应
        scope.launch {
            while (running) {
                val tickTime = System.currentTimeMillis()
                val expectedTime = tickTime + TICK_INTERVAL_MS

                // 在主线程上投递一个检测任务
                handler.post {
                    lastTickTime = System.currentTimeMillis()
                }

                // 给主线程预留时间处理
                delay(TICK_INTERVAL_MS + ANR_THRESHOLD_MS)

                // 检查主线程是否在预期时间内响应
                if (running && lastTickTime < expectedTime) {
                    val blockDuration = System.currentTimeMillis() - expectedTime
                    onANRDetected(blockDuration)
                }
            }
        }

        Log.i(TAG, "ANR看门狗已安装，检测间隔: ${TICK_INTERVAL_MS}ms，阈值: ${ANR_THRESHOLD_MS}ms")
    }

    /**
     * 卸载 ANR 看门狗
     */
    fun uninstall() {
        running = false
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        Log.d(TAG, "ANR看门狗已卸载，运行时长: ${(System.currentTimeMillis() - installTime) / 1000}s")
    }

    private fun onANRDetected(blockDuration: Long) {
        _anrCount.value++

        val mainThread = Looper.getMainLooper().thread
        val stackTrace = mainThread.stackTrace

        val sb = StringBuilder()
        sb.appendLine("=== ANR 检测 === ")
        sb.appendLine("主线程阻塞时间: ${blockDuration}ms")
        sb.appendLine("当前线程: ${mainThread.name} (id=${mainThread.id})")
        sb.appendLine("堆栈跟踪:")
        for (element in stackTrace) {
            sb.appendLine("  at $element")
        }

        Log.e(TAG, sb.toString())

        // 上报到 Sentry
        try {
            Sentry.captureMessage(
                "ANR detected: main thread blocked for ${blockDuration}ms",
                io.sentry.SentryLevel.ERROR
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Sentry ANR 上报失败", e)
        }
    }
}