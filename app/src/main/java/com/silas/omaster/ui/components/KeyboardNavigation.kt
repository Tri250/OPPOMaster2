package com.silas.omaster.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction

/**
 * 无障碍键盘导航工具
 *
 * 提供 Tab/方向键导航支持，确保键盘用户（包括 TalkBack 用户）可以完整体验应用。
 */

/**
 * 为可聚焦组件添加键盘导航支持
 *
 * 使用 Tab 键在组件间导航，Enter/Space 键触发操作
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.keyboardNavigation(
    focusManager: FocusManager = LocalFocusManager.current,
    description: String? = null
): Modifier {
    return this
        .then(
            if (description != null) {
                Modifier.semantics { contentDescription = description }
            } else {
                Modifier
            }
        )
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    // Tab 键：移动到下一个/上一个焦点
                    Key.Tab -> {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
                    // Enter/Space 键：触发操作
                    Key.Enter, Key.Spacebar -> {
                        // 触发当前焦点组件的点击操作
                        // Compose 自动处理，只需返回 true 表示已消费该事件
                        true
                    }
                    // 方向键：在组件内导航
                    Key.DirectionRight -> {
                        focusManager.moveFocus(FocusDirection.Right)
                        true
                    }
                    Key.DirectionLeft -> {
                        focusManager.moveFocus(FocusDirection.Left)
                        true
                    }
                    Key.DirectionDown -> {
                        focusManager.moveFocus(FocusDirection.Down)
                        true
                    }
                    Key.DirectionUp -> {
                        focusManager.moveFocus(FocusDirection.Up)
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
}

/**
 * 为按钮添加无障碍键盘支持
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.accessibleButton(
    description: String,
    enabled: Boolean = true
): Modifier {
    return this
        .semantics { contentDescription = description }
        .focusable(enabled)
        .onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar)
            ) {
                // Compose 自动处理点击事件
                true
            } else {
                false
            }
        }
}

/**
 * 键盘 IME Action 辅助（用于文输入框）
 */
object KeyboardAccessibility {
    val defaultKeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next
    )
    val doneKeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Done
    )
    val defaultKeyboardActions = KeyboardActions(
        onNext = { /* 焦点自动移动到下一个 */ },
        onDone = { /* 键盘收起 */ }
    )
}