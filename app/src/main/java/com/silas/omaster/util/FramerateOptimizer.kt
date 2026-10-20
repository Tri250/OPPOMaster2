package com.silas.omaster.util

import android.content.Context
import android.os.Build
import android.view.Choreographer
import com.silas.omaster.ui.animation.AnimationSpecs

/**
 * MU-23 高刷设备帧率感知 —— 读取 Choreographer.frameDelayNanos，
 * 为 AnimationSpecs 提供帧率自适应系数，确保 144Hz / 120Hz / 60Hz 设备
 * 动画体感时长一致（高刷设备需要更短的 durationMillis 来达到相同"速度感"）。
 *
 * 用法：
 *   val frameOpt = FramerateOptimizer.get(context)
 *   val tweenDuration = frameOpt.adjustDuration(AnimationSpecs.FastTween)
 */
object FramerateOptimizer {

    private const val BASELINE_FPS = 60
    private const val NANOS_PER_SECOND = 1_000_000_000L

    @Volatile
    private var cached: CachedOptimizer? = null

    data class CachedOptimizer(
        val detectedFps: Float,          // 检测到的最大帧率
        val durationMultiplier: Float,   // 动画时长系数（<1 表示需要缩短时长）
        val frameDelayNanos: Long        // frameDelayNanos 原始值
    )

    fun get(context: Context): CachedOptimizer {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val choreographer = Choreographer.getInstance()
            // frameDelayNanos 是 API 33；低版本用 postFrameCallback 估算
            val frameDelay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // v1.6.15 兼容性修复：frameDelayNanos 反射访问属非 SDK 接口，
                // Android 16 进一步收紧黑名单后将静默失效（144Hz 时长系数退化）。
                // 主路径改用公开 API Display.refreshRate；反射仅作兜底。
                val displayFps = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.display?.refreshRate
                    } else {
                        @Suppress("DEPRECATION")
                        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                            .defaultDisplay.refreshRate
                    }
                }.getOrNull()
                if (displayFps != null && displayFps > 0f) {
                    (NANOS_PER_SECOND / displayFps).toLong()
                } else {
                    runCatching {
                        try {
                            // 直接字段访问
                            val field = Choreographer::class.java.getDeclaredField("frameDelayNanos")
                            field.isAccessible = true
                            field.getLong(choreographer)
                        } catch (_: Throwable) {
                            // 反射 getter
                            val method = Choreographer::class.java.getMethod("getFrameDelayNanos")
                            method.invoke(choreographer) as Long
                        }
                    }.getOrDefault(16_666_667L)
                }
            } else {
                16_666_667L  // 60fps 基准
            } // 60fps 基准

            val fps = NANOS_PER_SECOND.toFloat() / frameDelay
            val multiplier = (BASELINE_FPS.toFloat() / fps).coerceIn(0.5f, 1.0f)
            // 144Hz → multiplier ≈ 0.417（动画时长缩短 ~40%）
            // 120Hz → multiplier ≈ 0.5
            // 60Hz → multiplier ≈ 1.0（不变）

            val opt = CachedOptimizer(
                detectedFps = fps,
                durationMultiplier = multiplier,
                frameDelayNanos = frameDelay
            )
            cached = opt
            return opt
        }
    }

    /** 把一个 tween 动画的 duration 乘以 multiplier，高刷下变短 */
    fun CachedOptimizer.adjustDuration(baseMillis: Int): Int {
        return (baseMillis * this.durationMultiplier).toInt().coerceAtLeast(40)
    }

    // ═══════════════════════════════════════════════════════════
    // 【v1.6.14】ColorOS 17 潮汐引擎式"前台流畅"保障
    //
    // 高刷设备（Find X10 144Hz）滑动列表时，交错入场动画 + 毛玻璃模糊是主要的
    // GPU/CPU 开销来源。掉帧连续超阈值时临时降级（暂停交错动画、降低模糊半径），
    // 停止滑动后恢复 —— 对齐潮汐引擎"宁可少一点特效也要保住帧率"的取舍原则。
    // ═══════════════════════════════════════════════════════════

    /** 高刷判定阈值（Hz）—— 达到即启用掉帧降级策略 */
    private const val HIGH_REFRESH_THRESHOLD_HZ = 120f

    /** 是否为高刷设备（>= 120Hz） */
    fun isHighRefreshDevice(context: Context): Boolean {
        return get(context).detectedFps >= HIGH_REFRESH_THRESHOLD_HZ
    }

    /** 创建与当前设备帧率匹配的掉帧监测器 */
    fun createMonitor(context: Context): FrameDropMonitor {
        return FrameDropMonitor(targetFps = get(context).detectedFps)
    }

    /**
     * 掉帧监测器 —— 非 Compose 依赖的纯逻辑，便于单测。
     *
     * @param targetFps 设备目标帧率（来自 detectedFps）
     * @param dropThreshold 连续超时帧数达到该值即判定需要降级
     */
    class FrameDropMonitor(
        private val targetFps: Float,
        private val dropThreshold: Int = 3
    ) {
        /** 单帧预算（纳秒）；低于 30fps 的设备按 30fps 计算预算，避免误判 */
        private val frameBudgetNanos: Long =
            (1_000_000_000L / targetFps.coerceAtLeast(30f)).toLong()

        /** 当前是否建议降级（true = 暂停高开销特效） */
        @Volatile
        var shouldDegrade: Boolean = false
            private set

        private var consecutiveDrops = 0
        private var lastFrameNanos = 0L

        /**
         * 每帧回调（由 Choreographer.postFrameCallback 驱动）。
         * 帧间隔超过预算的 2 倍即视为一次掉帧，连续 dropThreshold 次触发降级。
         */
        fun onFrame(frameTimeNanos: Long) {
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameTimeNanos
                return
            }
            val delta = frameTimeNanos - lastFrameNanos
            lastFrameNanos = frameTimeNanos
            if (delta > frameBudgetNanos * 2) {
                consecutiveDrops++
                if (consecutiveDrops >= dropThreshold) shouldDegrade = true
            } else if (consecutiveDrops > 0) {
                // 帧率恢复：立即解除降级（潮汐引擎"前台优先"）
                consecutiveDrops = 0
                shouldDegrade = false
            }
        }

        /** 滑动/动画结束后重置，避免降级状态长期驻留 */
        fun reset() {
            consecutiveDrops = 0
            lastFrameNanos = 0L
            shouldDegrade = false
        }
    }
}
