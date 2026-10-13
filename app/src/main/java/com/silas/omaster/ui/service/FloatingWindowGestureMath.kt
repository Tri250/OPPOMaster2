package com.silas.omaster.ui.service

import kotlin.math.abs

/**
 * MU-12② 悬浮窗手势数学（纯 JVM，ED-03 单测覆盖）。
 * 从 FloatingWindowService.onTouch 提取的判定与贴边决策，行为保持一致：
 * - 超过点击阈值即视为拖动（isClick 置否）；
 * - 垂直拖动限制在屏幕范围内（防出屏）；
 * - 松手贴边：以窗口中心靠哪侧半屏决定贴左或贴右。
 */
object FloatingWindowGestureMath {

    fun isBeyondClickThreshold(dx: Float, dy: Float, threshold: Float): Boolean =
        abs(dx) > threshold || abs(dy) > threshold

    /** 垂直方向限制，防止拖出屏幕（y ∈ [0, screenH - viewH]） */
    fun clampY(y: Int, screenH: Int, viewH: Int): Int =
        y.coerceIn(0, (screenH - viewH).coerceAtLeast(0))

    /** 贴边目标 X：窗口中心在左半屏 → 贴左（含边距），否则贴右 */
    fun snapTargetX(currentX: Int, screenW: Int, viewW: Int, margin: Int = 0): Int =
        if (currentX + viewW / 2 < screenW / 2) margin else screenW - viewW - margin
}
