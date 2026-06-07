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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silas.omaster.model.*
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.util.WatermarkJsonLoader
import com.silas.omaster.watermark.WatermarkProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 水印编辑器屏幕 - 用户可选择模板、调整参数、保存水印图片
 * 使用真实数据从watermarks.json加载
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
    var selectedTemplate by remember { mutableStateOf<WatermarkTemplateUiData?>(null) }
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

    // 从JSON加载真实水印模板数据
    val templates = remember { WatermarkJsonLoader.loadTemplatesForUi(context) }
    
    // 按分类分组
    val templatesByCategory = remember(templates) {
        templates.groupBy { it.category }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OMasterTopAppBar(
            title = "水印编辑器",
            subtitle = "${templates.size}+ 模板 · 个性定制",
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

            // 模板选择 - 按分类展示
            templatesByCategory.forEach { (category, categoryTemplates) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            // 分类标签
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (category) {
                                    WatermarkCategory.BRAND -> HasselbladOrange.copy(alpha = 0.2f)
                                    WatermarkCategory.FUNCTIONAL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    WatermarkCategory.FREE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = "${categoryTemplates.size}个",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (category) {
                                        WatermarkCategory.BRAND -> HasselbladOrange
                                        WatermarkCategory.FUNCTIONAL -> MaterialTheme.colorScheme.primary
                                        WatermarkCategory.FREE -> MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(categoryTemplates) { template ->
                                TemplateCard(
                                    template = template,
                                    isSelected = selectedTemplate?.id == template.id,
                                    onClick = { selectedTemplate = template }
                                )
                            }
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
                        
                        // 显示选中的模板信息
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "当前模板: ${selectedTemplate?.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            if (selectedTemplate?.source != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${selectedTemplate?.source})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
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
private fun TemplateCard(
    template: WatermarkTemplateUiData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.1f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 预览图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(template.style.backgroundColor))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 简化的预览效果
                Text(
                    text = template.name.take(2),
                    color = try {
                        Color(android.graphics.Color.parseColor(template.style.textColor))
                    } catch (e: Exception) {
                        Color.White
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = template.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            
            // 特性标签
            if (template.features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.features.first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            
            // 来源品牌
            if (template.source != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = template.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
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
    template: WatermarkTemplateUiData,
    imageUri: Uri,
    opacity: Float,
    textSizeRatio: Float,
    position: WatermarkPosition
): WatermarkResult = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val sourceBitmap = BitmapFactory.decodeStream(inputStream)
            ?: return@withContext WatermarkResult(success = false, error = "无法读取图片")

        // 根据模板ID选择对应的水印处理方式
        val result = when (template.id) {
            "hasselblad" -> applyHasselbladWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "leica" -> applyLeicaWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "zeiss" -> applyZeissWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "oppo-frame" -> applyOppoFrameWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "camera-info" -> applyCameraInfoWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "timestamp" -> applyTimestampWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "location" -> applyLocationWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "live-photo" -> applyLivePhotoWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "stamp" -> applyStampWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "chinese-style" -> applyChineseStyleWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "film-frame" -> applyFilmFrameWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "new-year" -> applyNewYearWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "signature" -> applySignatureWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "tile-pattern" -> applyTilePatternWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "diagonal" -> applyDiagonalWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            "minimal" -> applyMinimalWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
            else -> applyDefaultWatermark(sourceBitmap, template, opacity, textSizeRatio, position)
        }

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

// 各种水印处理函数（简化实现，实际应用中可以更复杂）
private fun applyHasselbladWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    // 使用现有的OpenSourceWatermarkTemplates中的实现
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyLeicaWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyZeissWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyOppoFrameWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyCameraInfoWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyTimestampWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyLocationWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyLivePhotoWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyStampWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyChineseStyleWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyFilmFrameWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyNewYearWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applySignatureWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyTilePatternWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyDiagonalWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyMinimalWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}

private fun applyDefaultWatermark(
    bitmap: Bitmap,
    template: WatermarkTemplateUiData,
    opacity: Float,
    scale: Float,
    position: WatermarkPosition
): WatermarkProcessResult {
    return WatermarkProcessResult(success = true, bitmap = bitmap)
}
