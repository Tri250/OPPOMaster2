package com.silas.omaster.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 全局震感开关
 * 日后可在设置中修改
 */
object HapticSettings {
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

@Composable
fun rememberScrollHaptics(scrollState: ScrollState) {
    val haptic = LocalHapticFeedback.current
    var hasHapticAtTop by remember { mutableStateOf(false) }
    var hasHapticAtBottom by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.value) {
        val currentValue = scrollState.value
        val maxValue = scrollState.maxValue

        if (currentValue == 0 && !hasHapticAtTop) {
            haptic.perform(HapticFeedbackType.TextHandleMove)
            hasHapticAtTop = true
            hasHapticAtBottom = false
        } else if (maxValue > 0 && currentValue >= maxValue && !hasHapticAtBottom) {
            haptic.perform(HapticFeedbackType.TextHandleMove)
            hasHapticAtBottom = true
            hasHapticAtTop = false
        } else if (currentValue > 0 && currentValue < maxValue) {
            hasHapticAtTop = false
            hasHapticAtBottom = false
        }
    }
}

/**
 * 【v1.6.12】ColorOS 17 场景化线性马达触感（O-Haptics 风格）
 *
 * API 30+ 使用语义化 HapticFeedbackConstants（CONFIRM 成功 / REJECT 失败），
 * 由系统（含 ColorOS 17）按场景映射精细马达波形；低版本自动降级 VIRTUAL_KEY。
 * 受全局震感开关 HapticSettings 约束，与设置页"震动反馈"一致。
 */
object OsHaptics {

    /** 操作成功（保存预设成功、收藏成功等） */
    fun confirm(view: android.view.View) =
        perform(view, android.view.HapticFeedbackConstants.CONFIRM)

    /** 操作失败（校验不通过、保存失败等） */
    fun reject(view: android.view.View) =
        perform(view, android.view.HapticFeedbackConstants.REJECT)

    /** 普通点按 */
    fun click(view: android.view.View) =
        perform(view, android.view.HapticFeedbackConstants.VIRTUAL_KEY)

    private fun perform(view: android.view.View, api30Constant: Int) {
        if (!HapticSettings.enabled) return
        val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            api30Constant
        } else {
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        }
        runCatching { view.performHapticFeedback(constant) }
    }
}
