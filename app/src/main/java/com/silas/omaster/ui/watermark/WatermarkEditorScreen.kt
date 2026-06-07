package com.silas.omaster.ui.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silas.omaster.R
import com.silas.omaster.model.*
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.watermark.OpenSourceWatermarkTemplates
import com.silas.omaster.watermark.WatermarkProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 水印编辑器屏幕 - 用户可选择模板、调整参数、保存水印图片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkEditorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { WatermarkProcessor(context) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTemplate by remember { mutableStateOf<WatermarkTemplate?>(null) }
    var textOpacity by remember { mutableStateOf(0.85f) }
    var textSize by remember { mutableStateOf(0.06f) }
    var position by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var isProcessing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var savedFilePath by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        resultMessage = null
        savedFilePath = null
    }

    val templates = remember { OpenSourceWatermarkTemplates.getAllTemplates() }

    Column(modifier = Modifier.fillMaxSize()) {
        OMasterTopAppBar(
            title = "水印编辑器",
            subtitle = "20+ 模板 · 个性定制",
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图片选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "选择原图",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "已选图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Camera,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击下方按钮选择图片",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("从相册选择图片")
                    }
                }
            }

            // 模板选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选择水印模板",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { info ->
                            TemplateChip(
                                info = info,
                                isSelected = selectedTemplate == info.template,
                                onClick = { selectedTemplate = info.template }
                            )
                        }
                    }
                }
            }

            // 参数调节
            if (selectedTemplate != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "参数调节",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("不透明度: ${(textOpacity * 100).toInt()}%", color = Color.White)
                        Slider(
                            value = textOpacity,
                            onValueChange = { textOpacity = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )

                        Text("文字大小: ${(textSize * 100).toInt()}%", color = Color.White)
                        Slider(
                            value = textSize,
                            onValueChange = { textSize = it },
                            valueRange = 0.02f..0.15f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("水印位置", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        PositionSelector(
                            selected = position,
                            onSelect = { position = it }
                        )
                    }
                }

                // 应用水印按钮
                Button(
                    onClick = {
                        val template = selectedTemplate ?: return@Button
                        val uri = selectedImageUri ?: return@Button
                        isProcessing = true
                        resultMessage = null
                        scope.launch {
                            val result = applyWatermark(
                                context = context,
                                processor = processor,
                                template = template,
                                imageUri = uri,
                                opacity = textOpacity,
                                textSizeRatio = textSize,
                                position = position
                            )
                            resultMessage = if (result.success) {
                                "水印已保存到: ${result.filePath}"
                            } else {
                                "保存失败: ${result.error}"
                            }
                            savedFilePath = result.filePath
                            isProcessing = false
                        }
                    },
                    enabled = selectedImageUri != null && selectedTemplate != null && !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("处理中...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("应用水印并保存")
                    }
                }
            }

            // 结果提示
            resultMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (savedFilePath != null) {
                            HasselbladOrange.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (savedFilePath != null) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = HasselbladOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 重置
            if (selectedImageUri != null || selectedTemplate != null) {
                OutlinedButton(
                    onClick = {
                        selectedImageUri = null
                        selectedTemplate = null
                        resultMessage = null
                        savedFilePath = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重置")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TemplateChip(
    info: com.silas.omaster.watermark.WatermarkTemplateInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.1f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = info.category,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PositionSelector(
    selected: WatermarkPosition,
    onSelect: (WatermarkPosition) -> Unit
) {
    val positions = listOf(
        WatermarkPosition.TOP_LEFT to "左上",
        WatermarkPosition.TOP_RIGHT to "右上",
        WatermarkPosition.CENTER to "居中",
        WatermarkPosition.BOTTOM_LEFT to "左下",
        WatermarkPosition.BOTTOM_RIGHT to "右下"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        positions.forEach { (pos, name) ->
            FilterChip(
                selected = selected == pos,
                onClick = { onSelect(pos) },
                label = { Text(name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HasselbladOrange,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

private data class WatermarkResult(
    val success: Boolean,
    val filePath: String? = null,
    val error: String? = null
)

private suspend fun applyWatermark(
    context: Context,
    processor: WatermarkProcessor,
    template: WatermarkTemplate,
    imageUri: Uri,
    opacity: Float,
    textSizeRatio: Float,
    position: WatermarkPosition
): WatermarkResult = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val sourceBitmap = BitmapFactory.decodeStream(inputStream)
            ?: return@withContext WatermarkResult(success = false, error = "无法读取图片")

        val config = WatermarkConfig(
            template = template,
            customText = template.defaultText,
            opacity = opacity,
            scale = textSizeRatio,
            position = position
        )

        val request = WatermarkProcessRequest(
            sourceBitmap = sourceBitmap,
            config = config
        )

        val result = processor.processWatermark(request)
        if (!result.success || result.bitmap == null) {
            sourceBitmap.recycle()
            return@withContext WatermarkResult(success = false, error = result.error ?: "处理失败")
        }

        // 保存到文件
        val fileName = "OMaster_Watermark_${System.currentTimeMillis()}.jpg"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { out ->
            result.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        sourceBitmap.recycle()
        result.bitmap.recycle()

        WatermarkResult(success = true, filePath = file.absolutePath)
    } catch (e: Exception) {
        WatermarkResult(success = false, error = e.message)
    }
}
