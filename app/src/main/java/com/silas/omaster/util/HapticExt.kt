package com.silas.omaster.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 全局震感开关
 * P1-3：改 @Volatile 保证多线程可见性（设置页与悬浮窗服务并发读写）
 */
object HapticSettings {
    @Volatile
    var enabled: Boolean = true
}

/**
 * 执行震感反馈
 */
fun HapticFeedback.perform(type: HapticFeedbackType) {
    if (HapticSettings.enabled) {
        performHapticFeedback(type)
    }
}

/**
 * P1-3：开关专用触感
 * - Android 14+ (API 34+) 使用 ToggleOn / ToggleOff 细粒度类型，符合 Android 16 交互规范
 * - 低版本回退到 LongPress
 */
fun HapticFeedback.performToggle(isOn: Boolean) {
    if (!HapticSettings.enabled) return
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // Android 14+ 才有 ToggleOn/ToggleOff
        if (isOn) toggleOnType() else toggleOffType()
    } else {
        HapticFeedbackType.LongPress
    }
    performHapticFeedback(type)
}

@Suppress("UNUSED_VARIABLE")
private fun toggleOnType(): HapticFeedbackType {
    // 反射兼容取值，避免在 <34 编译期找不到符号
    return try {
        HapticFeedbackType::class.java.getField("ToggleOn").get(null) as HapticFeedbackType
    } catch (e: Exception) {
        HapticFeedbackType.LongPress
    }
}

@Suppress("UNUSED_VARIABLE")
private fun toggleOffType(): HapticFeedbackType {
    return try {
        HapticFeedbackType::class.java.getField("ToggleOff").get(null) as HapticFeedbackType
    } catch (e: Exception) {
        HapticFeedbackType.LongPress
    }
}

/**
 * 带震感反馈的点击
 */
fun Modifier.hapticClickable(
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    val haptic = LocalHapticFeedback.current
    clickable(enabled = enabled) {
        haptic.perform(type)
        onClick()
    }
}

// ==================== P2-6：传统 View（非 Compose）触感扩展 ====================
// 适用于 FloatingWindowService 等基于原生 View 的场景：
// - 复用全局 [HapticSettings.enabled] 开关（已 @Volatile，跨线程可见）
// - 走 Android View 标准 [View.performHapticFeedback]，由系统统一调度，自动尊重用户系统级震感设置
// - 使用 [HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING] 确保即使 View 自身未设置触感属性也能触发

/**
 * 传统 View 触感反馈。默认使用 [HapticFeedbackConstants.CONTEXT_CLICK]（API 23+），
 * 低版本回退到 [HapticFeedbackConstants.VIRTUAL_KEY]，符合 Android 16 轻触反馈规范。
 */
fun View.performHapticCompat() {
    if (!HapticSettings.enabled) return
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        HapticFeedbackConstants.CONTEXT_CLICK
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    performHapticFeedback(
        resolved,
        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
    )
}

/**
 * 开关专用触感（传统 View）。
 * - API 34+（Android 14）使用 ToggleOn / ToggleOff 细粒度类型，符合 Android 16 交互规范
 * - 低版本回退到 CONTEXT_CLICK
 */
fun View.performToggleHapticCompat(isOn: Boolean) {
    if (!HapticSettings.enabled) return
    val resolved = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            // 反射兼容取值，避免在 <34 编译期找不到符号
            val fieldName = if (isOn) "TOGGLE_ON" else "TOGGLE_OFF"
            try {
                HapticFeedbackConstants::class.java.getField(fieldName).getInt(null)
            } catch (_: Exception) {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> HapticFeedbackConstants.CONTEXT_CLICK
        else -> HapticFeedbackConstants.VIRTUAL_KEY
    }
    performHapticFeedback(
        resolved,
        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
    )
}
