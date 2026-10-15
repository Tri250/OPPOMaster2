package com.silas.omaster.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager

/**
 * MU-27 相机取景框避位策略 —— 给悬浮窗计算一个"不遮挡取景"的目标坐标。
 *
 * 为什么不直接读 CameraCharacteristics？
 *   OMaster 没有 CAMERA 权限，且无法获知用户当前是否真的在 OPPO/一加/Realme 相机，
 *   所以采用**启发式避位**：
 *   1. 避开状态栏和导航栏；
 *   2. 避开 OPPO / 一加 / Realme 相机 UI 常见底部控件区（约 120dp 高度）；
 *   3. 避开顶部状态栏（约 56dp 高度，避免与 AI 闪记 / 通知重叠）；
 *   4. 横向避开左右各 16dp，防止贴边触发系统手势；
 *   5. 优先放在屏幕右下，离取景框最远。
 *
 * 所有计算都是基于屏幕真实像素（DeviceDetector），
 * 适配 Find X10 2768×1272、X9、一加 13 等不同分辨率。
 */
object CameraFramingAvoidance {

    /** 典型相机 UI 底部控件区高度（dp）：包含快门 / 相册 / 变焦条 */
    private const val CAMERA_BOTTOM_UI_HEIGHT_DP = 120
    /** 顶部状态栏预留（dp），避免与系统图标 / AI 闪记入口重叠 */
    private const val STATUS_BAR_RESERVED_DP = 56
    /** 左右安全边距（dp），避开边缘手势区域 */
    private const val SIDE_PADDING_DP = 16

    data class SafeArea(
        val left: Int, val top: Int,
        val right: Int, val bottom: Int
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    /**
     * 计算"悬浮窗安全可放置"的屏幕区域。
     *
     * @param viewWidth   悬浮窗宽度（像素）
     * @param viewHeight  悬浮窗高度（像素）
     * @param screenWidth 屏幕宽度（像素）
     * @param screenHeight 屏幕高度（像素）
     * @param windowInsets （可选）WindowInsets 的 systemBars，精度更高；传 null 走 dp 启发式
     */
    fun computeSafeArea(
        context: Context,
        viewWidth: Int,
        viewHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        systemBarInsets: Rect? = null
    ): SafeArea {
        val dp = context.resources.displayMetrics.density

        val statusBarTop = systemBarInsets?.top ?: (STATUS_BAR_RESERVED_DP * dp).toInt()
        val navBarBottom = systemBarInsets?.bottom ?: 0
        val bottomSafe = systemBarInsets?.let {
            it.bottom + (CAMERA_BOTTOM_UI_HEIGHT_DP * dp).toInt()
        } ?: (CAMERA_BOTTOM_UI_HEIGHT_DP * dp).toInt()
        val side = (SIDE_PADDING_DP * dp).toInt()

        return SafeArea(
            left = side,
            top = statusBarTop,
            right = screenWidth - side,
            bottom = screenHeight - bottomSafe - navBarBottom
        )
    }

    /**
     * 在安全区域内选一个不遮挡取景的"最佳坐标"。
     *
     * 策略：取景框通常覆盖屏幕 10%~85% 高度区间，
     * 所以优先落在安全区的"右下象限"（右上也可，但右下更符合用户手持习惯）。
     *
     * 返回值可直接赋给 WindowManager.LayoutParams.x / .y
     */
    fun pickAnchor(
        safeArea: SafeArea,
        viewWidth: Int,
        viewHeight: Int
    ): Pair<Int, Int> {
        val maxX = (safeArea.right - viewWidth).coerceAtLeast(safeArea.left)
        val maxY = (safeArea.bottom - viewHeight).coerceAtLeast(safeArea.top)

        // 右下优先
        val x = maxX
        val y = maxY
        return x to y
    }

    /**
     * 在避位后，窗口是否完全落在安全区域内（防越界）
     */
    fun clampToSafeArea(
        x: Int, y: Int,
        viewWidth: Int, viewHeight: Int,
        safeArea: SafeArea
    ): Pair<Int, Int> {
        val clampedX = x.coerceIn(safeArea.left, (safeArea.right - viewWidth).coerceAtLeast(safeArea.left))
        val clampedY = y.coerceIn(safeArea.top, (safeArea.bottom - viewHeight).coerceAtLeast(safeArea.top))
        return clampedX to clampedY
    }
}
