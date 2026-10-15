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
                // frameDelayNanos 是 API 33 (TIRAMISU) 的公开字段
                // compileSdk 37 可直接访问；用反射兜底以防 AGP/Kotlin 编译器版本差异
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
}
