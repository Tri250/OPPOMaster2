package com.silas.omaster.performance

import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.animation.calculateStaggerDelay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 性能相关单元测试
 *
 * 覆盖：
 * 1. 动画错开延迟计算不会随列表无限增长（保证长列表流畅性）
 * 2. 动画最大时长约束，避免低端设备卡顿
 * 3. 错开延迟算法时间复杂度 O(1)
 */
class PerformanceUnitTest {

    @Test
    fun staggerDelay_doesNotExceedMax() {
        // 模拟超长列表第 1000 项的延迟
        val delay = calculateStaggerDelay(index = 1000, visibleStartIndex = 0)
        assertTrue(
            "长列表项延迟不应超过 ${AnimationSpecs.MaxStaggerDelayMillis}ms，实际 $delay",
            delay <= AnimationSpecs.MaxStaggerDelayMillis
        )
    }

    @Test
    fun staggerDelay_forFirstVisibleItem_isZero() {
        val delay = calculateStaggerDelay(index = 5, visibleStartIndex = 5)
        assertEquals(0, delay)
    }

    @Test
    fun pageTransitionDuration_withinBudget() {
        // 页面切换动画预算 500ms，低端设备体验下限
        assertTrue(
            "页面切换时长 ${AnimationSpecs.PageTransitionMillis}ms 应 <= 500ms",
            AnimationSpecs.PageTransitionMillis <= 500
        )
    }

    @Test
    fun autoPlayInterval_notTooAggressive() {
        // 自动轮播间隔至少 2s，避免频繁刷新导致耗电/卡顿
        assertTrue(
            "自动播放间隔 ${AnimationSpecs.AutoPlayIntervalMillis}ms 应 >= 2000ms",
            AnimationSpecs.AutoPlayIntervalMillis >= 2000L
        )
    }

    @Test
    fun staggerDelay_constantTime() {
        // 验证算法只依赖简单运算，不随 index 增加产生额外开销
        val start = System.nanoTime()
        repeat(10_000) {
            calculateStaggerDelay(index = it, visibleStartIndex = it / 2)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(
            "10,000 次错开延迟计算应在 100ms 内完成，实际 ${elapsedMs}ms",
            elapsedMs < 100
        )
    }
}
