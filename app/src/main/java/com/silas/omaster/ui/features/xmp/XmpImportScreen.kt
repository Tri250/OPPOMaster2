package com.silas.omaster.ui.features.xmp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.data.xmp.XMPParser
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * XMP 导入页面
 * 支持 .xmp 文件导入、解析、预览和保存为自定义预设
 * PR-01 ~ PR-03
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XmpImportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PresetRepository.getInstance(context) }

    var parseResult by remember { mutableStateOf<com.silas.omaster.data.xmp.XMPParseResult?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var xmpContent by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isParsing = true
            errorMessage = null
            parseResult = null
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                }
                xmpContent = content
                val result = withContext(Dispatchers.Default) {
                    XMPParser.parse(content)
                }
                parseResult = result
                if (!result.success) {
                    errorMessage = result.errorMessage ?: "解析失败：未知错误"
                }
            } catch (e: Exception) {
                errorMessage = "读取文件失败: ${e.message}"
            } finally {
                isParsing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("XMP 导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 导入按钮
            if (parseResult == null && !isParsing) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "导入 .xmp 预设文件",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "支持哈苏、富士、索尼、徕卡等品牌",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { filePicker.launch("application/xml") }) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择 .xmp 文件")
                }
            }

            // 解析中
            if (isParsing) {
                CircularProgressIndicator(color = HasselbladOrange)
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在解析...", style = MaterialTheme.typography.bodyMedium)
            }

            // 错误提示（PR-03：非法/损坏 .xmp 兜底）
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = {
                    parseResult = null
                    errorMessage = null
                    xmpContent = null
                }) {
                    Text("重新选择")
                }
            }

            // 解析成功结果展示（PR-01/PR-02）
            parseResult?.let { result ->
                if (result.success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = result.presetName ?: "未命名预设",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "品牌: ${result.brand ?: "未知"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 参数明细
                            if (result.params.isNotEmpty()) {
                                Text("参数明细", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                result.params.forEach { (key, value) ->
                                    Text(
                                        text = "  $key: ${String.format("%.2f", value)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // HSL
                            if (result.hsl.isNotEmpty()) {
                                Text("HSL 调整", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                result.hsl.forEach { (color, values) ->
                                    Text(
                                        text = "  $color: H=${values.hue}, S=${values.saturation}, L=${values.luminance}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 曲线
                            if (result.curve.isNotEmpty()) {
                                Text("曲线点: ${result.curve.size} 个", fontWeight = FontWeight.SemiBold)
                            }

                            // 品牌特有字段提示
                            when (result.brand?.lowercase()) {
                                "fujifilm", "富士" -> {
                                    result.rawFields["CameraProfile"]?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("富士胶片模拟: $it", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                "sony", "索尼" -> {
                                    result.rawFields["DRODynamicRange"]?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("索尼 DRO: $it", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                repo.createCustomPreset(
                                    name = result.presetName ?: "导入预设",
                                    params = result.params.mapValues { it.value.toString() },
                                    brand = result.brand ?: "custom",
                                    description = XMPParser.formatParamsDetail(result)
                                )
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存为自定义预设")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            parseResult = null
                            errorMessage = null
                            xmpContent = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("导入其他文件")
                    }
                }
            }
        }
    }
}
