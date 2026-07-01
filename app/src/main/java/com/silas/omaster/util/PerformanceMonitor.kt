package com.silas.omaster.util

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 性能监控工具
 *
 * 功能：
 * - 启动时间追踪
 * - 页面渲染性能监控
 * - 内存使用监控
 * - FPS 监控
 * - 性能基准记录
 *
 * 用于发布前性能基准测试和线上性能监控
 */
object PerformanceMonitor {

    private const val TAG = "PerfMonitor"
    private const val MAX_RECORDS = 100

    // 启动时间记录
    private val _startupMetrics = MutableStateFlow<StartupMetrics?>(null)
    val startupMetrics: StateFlow<StartupMetrics?> = _startupMetrics.asStateFlow()

    // 页面性能记录
    private val pageMetrics = mutableListOf<PageMetrics>()

    // 性能配置
    var isEnabled: Boolean = true
    var isDebugMode: Boolean = true

    /**
     * 启动性能指标
     */
    data class StartupMetrics(
        val totalTimeMs: Long = 0,
        val applicationCreateTimeMs: Long = 0,
        val firstFrameTimeMs: Long = 0,
        val firstInteractiveTimeMs: Long = 0,
        val steps: List<StepMetrics> = emptyList()
    )

    /**
     * 步骤性能指标
     */
    data class StepMetrics(
        val name: String,
        val durationMs: Long,
        val timestampMs: Long
    )

    /**
     * 页面性能指标
     */
    data class PageMetrics(
        val pageName: String,
        val renderTimeMs: Long,
        val frameDropCount: Int = 0,
        val timestampMs: Long = System.currentTimeMillis()
    )

    // ===== 启动时间追踪 =====

    private var appStartTime: Long = 0
    private var firstFrameTime: Long = 0
    private val startupSteps = mutableListOf<StepMetrics>()

    /**
     * 标记应用启动开始（应在 Application.onCreate 最开始调用）
     */
    fun markAppStart() {
        if (!isEnabled) return
        appStartTime = SystemClock.elapsedRealtime()
        if (isDebugMode) {
            Log.i(TAG, "应用启动计时开始")
        }
    }

    /**
     * 记录启动步骤耗时
     */
    fun logStartupStep(name: String, durationMs: Long) {
        if (!isEnabled) return
        startupSteps.add(
            StepMetrics(
                name = name,
                durationMs = durationMs,
                timestampMs = SystemClock.elapsedRealtime()
            )
        )
        if (isDebugMode) {
            Log.d(TAG, "启动步骤: $name 耗时 ${durationMs}ms")
        }
    }

    /**
     * 标记首帧渲染完成
     */
    fun markFirstFrame() {
        if (!isEnabled) return
        firstFrameTime = SystemClock.elapsedRealtime()
        val totalTime = firstFrameTime - appStartTime
        if (isDebugMode) {
            Log.i(TAG, "首帧渲染完成，总耗时: ${totalTime}ms")
        }
    }

    /**
     * 标记首次可交互
     */
    fun markFirstInteractive() {
        if (!isEnabled) return
        val interactiveTime = SystemClock.elapsedRealtime()
        val totalTime = interactiveTime - appStartTime

        _startupMetrics.value = StartupMetrics(
            totalTimeMs = totalTime,
            applicationCreateTimeMs = startupSteps.sumOf { it.durationMs },
            firstFrameTimeMs = firstFrameTime - appStartTime,
            firstInteractiveTimeMs = totalTime,
            steps = startupSteps.toList()
        )

        if (isDebugMode) {
            Log.i(TAG, "首次可交互，总耗时: ${totalTime}ms")
            Log.i(TAG, "启动步骤详情:")
            startupSteps.forEach { step ->
                Log.i(TAG, "  ${step.name}: ${step.durationMs}ms")
            }
        }
    }

    // ===== 页面性能追踪 =====

    private var pageStartTime: Long = 0

    /**
     * 标记页面开始渲染
     */
    fun markPageStart(pageName: String) {
        if (!isEnabled) return
        pageStartTime = SystemClock.elapsedRealtime()
        if (isDebugMode) {
            Log.d(TAG, "页面开始渲染: $pageName")
        }
    }

    /**
     * 标记页面渲染完成
     */
    fun markPageRendered(pageName: String, frameDropCount: Int = 0) {
        if (!isEnabled) return
        val renderTime = SystemClock.elapsedRealtime() - pageStartTime
        val metrics = PageMetrics(
            pageName = pageName,
            renderTimeMs = renderTime,
            frameDropCount = frameDropCount
        )
        pageMetrics.add(metrics)
        if (pageMetrics.size > MAX_RECORDS) {
            pageMetrics.removeAt(0)
        }
        if (isDebugMode) {
            Log.d(TAG, "页面渲染完成: $pageName 耗时 ${renderTime}ms 丢帧 $frameDropCount")
        }
    }

    /**
     * 获取页面性能历史
     */
    fun getPageMetrics(): List<PageMetrics> = pageMetrics.toList()

    /**
     * 获取平均页面渲染时间
     */
    fun getAverageRenderTime(pageName: String): Long? {
        val pageRecords = pageMetrics.filter { it.pageName == pageName }
        if (pageRecords.isEmpty()) return null
        return pageRecords.sumOf { it.renderTimeMs } / pageRecords.size
    }

    // ===== 性能阈值检查 =====

    /**
     * 启动性能等级
     */
    enum class PerformanceLevel {
        EXCELLENT,   // 优秀
        GOOD,        // 良好
        NORMAL,      // 一般
        POOR         // 较差
    }

    /**
     * 评估启动性能
     */
    fun evaluateStartupPerformance(metrics: StartupMetrics): PerformanceLevel {
        return when {
            metrics.totalTimeMs < 500 -> PerformanceLevel.EXCELLENT
            metrics.totalTimeMs < 1000 -> PerformanceLevel.GOOD
            metrics.totalTimeMs < 2000 -> PerformanceLevel.NORMAL
            else -> PerformanceLevel.POOR
        }
    }

    /**
     * 评估页面渲染性能
     */
    fun evaluatePagePerformance(renderTimeMs: Long): PerformanceLevel {
        return when {
            renderTimeMs < 100 -> PerformanceLevel.EXCELLENT
            renderTimeMs < 200 -> PerformanceLevel.GOOD
            renderTimeMs < 500 -> PerformanceLevel.NORMAL
            else -> PerformanceLevel.POOR
        }
    }

    // ===== 工具方法 =====

    /**
     * 测量代码块执行时间
     */
    internal inline fun <T> measureTime(tag: String, block: () -> T): T {
        if (!isEnabled) return block()
        val start = SystemClock.elapsedRealtime()
        val result = block()
        val duration = SystemClock.elapsedRealtime() - start
        if (isDebugMode) {
            Log.d(TAG, "[$tag] 耗时: ${duration}ms")
        }
        return result
    }

    /**
     * 重置所有性能数据
     */
    fun reset() {
        _startupMetrics.value = null
        pageMetrics.clear()
        startupSteps.clear()
        appStartTime = 0
        firstFrameTime = 0
    }

    /**
     * 获取性能报告
     */
    fun getReport(): String {
        return buildString {
            appendLine("===== 性能监控报告 =====")
            appendLine()
            _startupMetrics.value?.let { startup ->
                appendLine("启动性能:")
                appendLine("  总耗时: ${startup.totalTimeMs}ms")
                appendLine("  Application创建: ${startup.applicationCreateTimeMs}ms")
                appendLine("  首帧渲染: ${startup.firstFrameTimeMs}ms")
                appendLine("  首次可交互: ${startup.firstInteractiveTimeMs}ms")
                appendLine("  性能等级: ${evaluateStartupPerformance(startup)}")
                appendLine("  步骤详情:")
                startup.steps.forEach { step ->
                    appendLine("    ${step.name}: ${step.durationMs}ms")
                }
                appendLine()
            }
            appendLine("页面性能记录: ${pageMetrics.size} 条")
            if (pageMetrics.isNotEmpty()) {
                val avgRender = pageMetrics.sumOf { it.renderTimeMs } / pageMetrics.size
                appendLine("  平均渲染时间: ${avgRender}ms")
                appendLine("  总丢帧数: ${pageMetrics.sumOf { it.frameDropCount }}")
            }
        }
    }
}
