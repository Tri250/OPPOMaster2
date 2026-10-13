package com.silas.omaster.ui.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MU-12② 手势数学单测（与 FloatingWindowService 原行为一致性） */
class FloatingWindowGestureMathTest {

    @Test
    fun `click threshold judgement matches legacy behaviour`() {
        assertTrue(FloatingWindowGestureMath.isBeyondClickThreshold(11f, 0f, 10f))
        assertTrue(FloatingWindowGestureMath.isBeyondClickThreshold(0f, -11f, 10f))
        assertFalse(FloatingWindowGestureMath.isBeyondClickThreshold(10f, 10f, 10f)) // 等于阈值不算拖动
    }

    @Test
    fun `clampY keeps window inside screen`() {
        assertEquals(0, FloatingWindowGestureMath.clampY(-50, 2000, 200))
        assertEquals(1800, FloatingWindowGestureMath.clampY(5000, 2000, 200))
        assertEquals(960, FloatingWindowGestureMath.clampY(960, 2000, 200))
    }

    @Test
    fun `clampY is safe when view taller than screen`() {
        assertEquals(0, FloatingWindowGestureMath.clampY(100, 500, 800))
    }

    @Test
    fun `snap target picks nearer edge`() {
        // 窗口中心偏左 → 贴左
        assertEquals(0, FloatingWindowGestureMath.snapTargetX(100, 1080, 300))
        // 窗口中心偏右 → 贴右（屏宽-窗宽）
        assertEquals(780, FloatingWindowGestureMath.snapTargetX(900, 1080, 300))
        // 含边距
        assertEquals(16, FloatingWindowGestureMath.snapTargetX(100, 1080, 300, margin = 16))
        assertEquals(764, FloatingWindowGestureMath.snapTargetX(900, 1080, 300, margin = 16))
    }
}
