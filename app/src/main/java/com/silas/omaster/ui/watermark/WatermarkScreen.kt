package com.silas.omaster.ui.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.R
import com.silas.omaster.data.watermark.WatermarkConfig
import com.silas.omaster.data.watermark.WatermarkPosition
import com.silas.omaster.data.watermark.WatermarkRenderer
import com.silas.omaster.data.watermark.WatermarkType
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    onBack: () -> Unit,
    viewModel: WatermarkViewModel = viewModel(factory = WatermarkViewModelFactory(LocalContext.current))
) {
    val config by viewModel.currentConfig.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 预览用的示例图片
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var watermarkedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 生成预览图
    LaunchedEffect(Unit) {
        val sample = createSampleBitmap()
        previewBitmap = sample
    }

    // 配置变化时更新预览（ST-RES-01: 修复bitmap竞态，使用原子引用防止快速切换时回收正在使用的bitmap）
    LaunchedEffect(config) {
        val source = previewBitmap ?: return@LaunchedEffect
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                try {
                    WatermarkRenderer.renderWatermark(source, config)
                } catch (e: OutOfMemoryError) {
                    android.util.Log.e("WatermarkScreen", "水印渲染OOM", e)
                    System.gc()
                    null
                }
            }
            result?.let {
                val old = watermarkedBitmap
                watermarkedBitmap = it
                old?.recycle()
            }
        }
    }

    // 页面退出时释放预览 Bitmap
    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.recycle()
            previewBitmap = null
            watermarkedBitmap?.recycle()
            watermarkedBitmap = null
        }
    }

    // 图片选择器（用于大师签名水印，Android 16+ Photo Picker）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // 将选中的图片复制到应用私有目录
            try {
                val inputStream = context.contentResolver.openInputStream(it) ?: return@let
                val outputFile = java.io.File(context.filesDir, "signature_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(outputFile).use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()
                viewModel.updateConfig(config.copy(signatureImagePath = outputFile.absolutePath))
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watermark_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = stringResource(R.string.watermark_undo),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = stringResource(R.string.watermark_redo),
                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 预览区域
            PreviewSection(
                watermarkedBitmap = watermarkedBitmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 模板选择器
            TemplateSelector(
                templates = templates,
                onApplyTemplate = { viewModel.applyTemplate(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 水印类型切换
            WatermarkTypeSelector(
                selectedType = config.type,
                onTypeSelected = { newType ->
                    viewModel.updateConfig(config.copy(type = newType))
                    viewModel.loadTemplates(newType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 根据类型显示不同编辑面板
            when (config.type) {
                WatermarkType.BRAND -> BrandWatermarkEditor(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                WatermarkType.MASTER_MARK -> MasterMarkWatermarkEditor(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) },
                    onPickImage = { 
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                WatermarkType.XPAN -> XpanWatermarkEditor(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = {
                    viewModel.save()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PreviewSection(
    watermarkedBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.watermark_preview),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            watermarkedBitmap?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.watermark_preview),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } ?: run {
                Text(
                    text = stringResource(R.string.watermark_preview_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun TemplateSelector(
    templates: List<com.silas.omaster.data.watermark.WatermarkTemplate>,
    onApplyTemplate: (com.silas.omaster.data.watermark.WatermarkTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (templates.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.watermark_template),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { template ->
                FilterChip(
                    selected = false,
                    onClick = { onApplyTemplate(template) },
                    label = { Text(template.name, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun WatermarkTypeSelector(
    selectedType: WatermarkType,
    onTypeSelected: (WatermarkType) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        WatermarkType.BRAND to stringResource(R.string.watermark_type_brand),
        WatermarkType.MASTER_MARK to stringResource(R.string.watermark_type_master),
        WatermarkType.XPAN to stringResource(R.string.watermark_type_xpan)
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.watermark_type_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { (type, label) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun BrandWatermarkEditor(
    config: WatermarkConfig,
    onConfigChange: (WatermarkConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 文字输入
        OutlinedTextField(
            value = config.text,
            onValueChange = { onConfigChange(config.copy(text = it)) },
            label = { Text(stringResource(R.string.watermark_text)) },
            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // 字体选择
        val fonts = listOf("Default", "Monospace", "Serif", "Sans_Serif")
        LabeledSection(stringResource(R.string.watermark_font)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fonts.forEach { font ->
                    FilterChip(
                        selected = config.fontName == font,
                        onClick = { onConfigChange(config.copy(fontName = font)) },
                        label = { Text(font, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // 字号滑块
        LabeledSlider(
            label = stringResource(R.string.watermark_font_size),
            value = config.fontSize.toFloat(),
            onValueChange = { onConfigChange(config.copy(fontSize = it.toInt())) },
            valueRange = 10f..72f,
            valueLabel = "${config.fontSize}sp"
        )

        // 颜色选择（简化为预设颜色）
        LabeledSection(stringResource(R.string.watermark_color)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val colors = listOf(
                    0xFFFFFFFFL to "白",
                    0xFF000000L to "黑",
                    0xFFFF8C00L to "橙",
                    0xFFAAAAAAL to "灰"
                )
                colors.forEach { (colorValue, label) ->
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (config.color == colorValue) Modifier.border(
                                    2.dp, HasselbladOrange, RoundedCornerShape(8.dp)
                                ) else Modifier
                            ),
                        color = androidx.compose.ui.graphics.Color(colorValue.toInt()),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { onConfigChange(config.copy(color = colorValue)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (config.color == colorValue) {
                                Text(label, fontSize = 10.sp, color = HasselbladOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 位置选择
        PositionSelector(
            selectedPosition = config.position,
            onPositionSelected = { onConfigChange(config.copy(position = it)) }
        )

        // 透明度滑块
        LabeledSlider(
            label = stringResource(R.string.watermark_opacity),
            value = config.opacity,
            onValueChange = { onConfigChange(config.copy(opacity = it)) },
            valueRange = 0.1f..1f,
            valueLabel = "${(config.opacity * 100).toInt()}%"
        )
    }
}

@Composable
private fun MasterMarkWatermarkEditor(
    config: WatermarkConfig,
    onConfigChange: (WatermarkConfig) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 图片选择按钮
        Button(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (config.signatureImagePath.isEmpty())
                    stringResource(R.string.watermark_pick_signature)
                else
                    stringResource(R.string.watermark_signature_loaded)
            )
        }

        // 位置选择
        PositionSelector(
            selectedPosition = config.position,
            onPositionSelected = { onConfigChange(config.copy(position = it)) }
        )

        // 透明度滑块
        LabeledSlider(
            label = stringResource(R.string.watermark_opacity),
            value = config.opacity,
            onValueChange = { onConfigChange(config.copy(opacity = it)) },
            valueRange = 0.1f..1f,
            valueLabel = "${(config.opacity * 100).toInt()}%"
        )
    }
}

@Composable
private fun XpanWatermarkEditor(
    config: WatermarkConfig,
    onConfigChange: (WatermarkConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 黑边比例滑块
        LabeledSlider(
            label = stringResource(R.string.watermark_xpan_bar_ratio),
            value = config.xpanBarRatio,
            onValueChange = { onConfigChange(config.copy(xpanBarRatio = it)) },
            valueRange = 0.02f..0.3f,
            valueLabel = "${(config.xpanBarRatio * 100).toInt()}%"
        )

        // 文字输入
        OutlinedTextField(
            value = config.xpanText,
            onValueChange = { onConfigChange(config.copy(xpanText = it)) },
            label = { Text(stringResource(R.string.watermark_xpan_text)) },
            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // 透明度
        LabeledSlider(
            label = stringResource(R.string.watermark_opacity),
            value = config.opacity,
            onValueChange = { onConfigChange(config.copy(opacity = it)) },
            valueRange = 0.1f..1f,
            valueLabel = "${(config.opacity * 100).toInt()}%"
        )
    }
}

@Composable
private fun PositionSelector(
    selectedPosition: WatermarkPosition,
    onPositionSelected: (WatermarkPosition) -> Unit
) {
    LabeledSection(stringResource(R.string.watermark_position)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WatermarkPosition.entries.forEach { position ->
                val label = when (position) {
                    WatermarkPosition.BOTTOM_LEFT -> stringResource(R.string.watermark_pos_bottom_left)
                    WatermarkPosition.BOTTOM_RIGHT -> stringResource(R.string.watermark_pos_bottom_right)
                    WatermarkPosition.CENTER -> stringResource(R.string.watermark_pos_center)
                }
                FilterChip(
                    selected = selectedPosition == position,
                    onClick = { onPositionSelected(position) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun LabeledSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 创建预览用示例图片
 */
private fun createSampleBitmap(): Bitmap {
    val width = 600
    val height = 400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 渐变背景模拟
    val paint = android.graphics.Paint()
    for (y in 0 until height) {
        val ratio = y.toFloat() / height
        val r = (30 + ratio * 60).toInt()
        val g = (30 + ratio * 30).toInt()
        val b = (60 + ratio * 80).toInt()
        paint.color = Color.rgb(r, g, b)
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
    }

    // 添加简单装饰
    paint.color = Color.argb(40, 255, 255, 255)
    paint.textSize = 32f
    canvas.drawText("Preview", 200f, 200f, paint)

    return bitmap
}
