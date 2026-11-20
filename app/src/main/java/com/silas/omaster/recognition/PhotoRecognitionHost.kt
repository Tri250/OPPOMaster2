package com.silas.omaster.recognition

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.silas.omaster.data.local.CustomPresetManager
import kotlinx.coroutines.launch

/**
 * 照片识别宿主：状态机 + 选图 + 识别 + 确认 + 落库 + 跳转 POP。
 *
 * 状态：IDLE（选图）→ ANALYZING → RESULT（确认页）→ 保存后跳 POP / 返回。
 * 对应 PRD 10.1 流程与 10.2 状态机。
 */
@Composable
fun PhotoRecognitionHost(
    initialImageUri: String?,
    onBack: () -> Unit,
    onSavedToPop: (String) -> Unit   // 落库后的 preset id → 跳 POP
) {
    val context = LocalContext.current
    val accent = Color(0xFFEC7223)
    var state by remember { mutableStateOf(RecogState.IDLE) }
    var result by remember { mutableStateOf<RecognitionResult?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) startRecognition(uri, context, { state = it }, { result = it }, { bitmap = it }, { error = it }) }

    // 有外部图片 URI 时自动开始识别（仅一次）
    var consumedInitial by remember { mutableStateOf(false) }
    LaunchedEffect(initialImageUri) {
        if (!consumedInitial && initialImageUri != null) {
            consumedInitial = true
            startRecognition(Uri.parse(initialImageUri), context, { state = it },
                { result = it }, { bitmap = it }, { error = it })
        }
    }

    when (state) {
        RecogState.IDLE, RecogState.ERROR -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null,
                        modifier = Modifier.size(72.dp), tint = accent)
                    Spacer(Modifier.height(20.dp))
                    Text("导入喜欢的照片", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "选择一张 Find 哈苏样片或任意风景/人像照片，OMaster 会读取照片参数并分析画面风格，生成可套用的预设。所有处理均在本地完成。",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("⚠ $error", color = Color(0xFFE53935), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(28.dp))
                    Button(onClick = { error = null; pickLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                        Text("从相册选择照片")
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        RecogState.ANALYZING -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = accent)
                    Spacer(Modifier.height(20.dp))
                    Text("正在分析照片参数与风格…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        RecogState.RESULT -> {
            val r = result
            if (r != null) {
                PhotoRecognitionScreen(
                    bitmap = bitmap,
                    result = r,
                    onBack = { state = RecogState.IDLE },
                    onConfirm = { preset ->
                        // 写库（统一经 CustomPresetManager，含校验后的草稿）
                        val manager = CustomPresetManager.getInstance(context)
                        manager.addCustomPreset(preset)
                        val savedId = manager.getCustomPresets().firstOrNull { it.name == preset.name }?.id
                        if (savedId != null) onSavedToPop(savedId) else onBack()
                    }
                )
            }
        }
    }
}

private enum class RecogState { IDLE, ANALYZING, RESULT, ERROR }

/** 启动识别（协程）——用回调更新 Compose 状态，规避在 Composable 中直接挂起 */
private fun startRecognition(
    uri: Uri,
    context: android.content.Context,
    setState: (RecogState) -> Unit,
    setResult: (RecognitionResult) -> Unit,
    setBitmap: (Bitmap) -> Unit,
    setError: (String) -> Unit
) {
    setState(RecogState.ANALYZING)
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        when (val res = PhotoRecognitionService.recognize(context, uri)) {
            is PhotoRecognitionService.Result.Success -> {
                setResult(res.result); setBitmap(res.bitmap); setState(RecogState.RESULT)
            }
            is PhotoRecognitionService.Result.Failure -> {
                setError(res.error.hint); setState(RecogState.ERROR)
            }
        }
    }
}
