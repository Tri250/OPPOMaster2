package com.silas.omaster.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.silas.omaster.R

/**
 * 编辑操作链路完整性组件
 *
 * 提供标准的「放弃修改」确认对话框、「恢复原始」按钮、
 * 「保存为预设」对话框等操作链完整性保障组件。
 */

/**
 * 「放弃修改」确认对话框
 *
 * 在编辑页面退出时，若用户已修改参数，弹出此对话框确认是否放弃。
 * 防止用户误触返回导致编辑成果丢失。
 *
 * 使用方式：
 * ```kotlin
 * var showDiscardDialog by remember { mutableStateOf(false) }
 * BackHandler(hasChanges) { showDiscardDialog = true }
 * if (showDiscardDialog) {
 *     DiscardChangesDialog(
 *         onConfirm = { navController.popBackStack() },
 *         onDismiss = { showDiscardDialog = false }
 *     )
 * }
 * ```
 */
@Composable
fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "放弃修改？",
    message: String = "你有未保存的修改，退出后将丢失所有更改。确定要放弃吗？",
    confirmText: String = "放弃",
    dismissText: String = "继续编辑"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * 保存失败重试对话框
 *
 * 当保存操作失败时，提供重试选项。
 */
@Composable
fun SaveErrorRetryDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "保存失败",
    retryText: String = "重试",
    dismissText: String = "取消"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(retryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * 「保存为预设」对话框
 *
 * 编辑完成后，将当前参数保存为可复用的预设。
 */
@Composable
fun SaveAsPresetDialog(
    defaultName: String = "",
    onConfirm: (presetName: String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "保存为预设",
    confirmText: String = "保存",
    dismissText: String = "取消"
) {
    var presetName by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(presetName) },
                enabled = presetName.isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}