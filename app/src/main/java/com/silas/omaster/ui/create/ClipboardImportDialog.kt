package com.silas.omaster.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.silas.omaster.R
import com.silas.omaster.data.imports.PresetImportValidator
import com.silas.omaster.data.imports.PresetImportValidator.ErrorCode
import com.silas.omaster.data.imports.PresetImportValidator.ImportResult
import com.silas.omaster.data.local.CustomPresetManager

/**
 * FD-01b/01a/01c 粘贴导入对话框：文本 → R-3 校验管道（PresetImportValidator）→
 * 统一经 CustomPresetManager 入库（架构红线：外部内容不绕过校验管道）。
 *
 * @param initialText 预填文本（文件导入 FD-01a / 系统分享 FD-01c 复用同一对话框）
 */
@Composable
fun ClipboardImportDialog(
    initialText: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var result by remember { mutableStateOf<ImportResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.paste_import)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        result = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
                    placeholder = { Text(stringResource(R.string.paste_import_hint)) }
                )
                result?.let { r ->
                    val message = when (r) {
                        is ImportResult.Success ->
                            stringResource(R.string.paste_import_result, r.presets.size)
                        is ImportResult.Failure -> stringResource(r.code.toResId())
                    }
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (r is ImportResult.Success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = PresetImportValidator.parse(text)
                result = parsed
                if (parsed is ImportResult.Success) {
                    val manager = CustomPresetManager.getInstance(context)
                    parsed.presets.forEach { manager.addCustomPreset(it) }
                    onDismiss()
                }
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun ErrorCode.toResId(): Int = when (this) {
    ErrorCode.EMPTY -> R.string.paste_import_error_empty
    ErrorCode.TOO_LARGE -> R.string.paste_import_error_too_large
    ErrorCode.INVALID_JSON -> R.string.paste_import_error_invalid
    ErrorCode.NO_VALID_PRESET -> R.string.paste_import_error_no_valid
    ErrorCode.TOO_MANY_PRESETS -> R.string.paste_import_error_too_many
}
