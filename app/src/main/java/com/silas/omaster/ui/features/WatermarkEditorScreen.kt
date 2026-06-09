package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*
import com.silas.omaster.util.*
import com.silas.omaster.watermark.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.math.*

/**
 * 水印编辑器独立页面
 * 
 * 功能特性：
 * - 实时预览区（Canvas渲染）
 * - 水印模板分类和选择
 * - 水印元素开关和编辑
 * - 位置网格和样式调节
 * - EXIF自动填充
 * - 智能颜色适配
 * - 手势缩放拖拽
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkEditorScreen(
    imagePath: String? = null,
    onBack: () -> Unit,
    onSave: (WatermarkConfig) -> Unit,
    onExport: (Bitmap, WatermarkConfig) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ========== 状态管理 ==========
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWatermarkEnabled by remember { mutableStateOf(true) }
    var showBeforeAfter by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // 水印配置
    var selectedCategory by remember { mutableStateOf("全部") }
    var selectedTemplateId by remember { mutableStateOf("hasselblad_classic") }
    var watermarkConfig by remember { mutableStateOf(WatermarkConfig()) }

    // 元素开关
    var showBrand by remember { mutableStateOf(true) }
    var showModel by remember { mutableStateOf(true) }
    var showParams by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(true) }
    var showVignette by remember { mutableStateOf(false) }

    // 元素文本
    var brandText by remember { mutableStateOf("HASSELBLAD") }
    var modelText by remember { mutableStateOf("OPPO Find X8 Pro") }
    var paramsText by remember { mutableStateOf("f/1.8 1/125 ISO100") }
    var dateText by remember { mutableStateOf("2026-06-09") }

    // 样式参数
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_LEFT) }
    var selectedFont by remember { mutableStateOf("默认") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var textSize by remember { mutableFloatStateOf(14f) }
    var opacity by remember { mutableFloatStateOf(0.8f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var shadowBlur by remember { mutableFloatStateOf(4f) }

    // 智能颜色推荐
    var recommendedColor by remember { mutableStateOf(Color.White) }

    // 拖拽状态
    var watermarkOffset by remember { mutableStateOf(Offset.Zero) }
    var watermarkScale by remember { mutableFloatStateOf(1f) }

    // ========== 初始化加载图片 ==========
    LaunchedEffect(imagePath) {
        isLoading = true
        try {
            val bitmap = if (imagePath != null) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                // 使用默认示例图片
                null
            }
            originalBitmap = bitmap

            // 从EXIF自动填充
            if (imagePath != null) {
                extractExifData(imagePath).let { exif ->
                    modelText = exif.model ?: modelText
                    paramsText = exif.params ?: paramsText
                    dateText = exif.date ?: dateText
                }
            }

            // 智能颜色适配
            if (bitmap != null) {
                recommendedColor = analyzeDominantColor(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    // ========== 实时预览更新 ==========
    LaunchedEffect(
        isWatermarkEnabled, showBrand, showModel, showParams, showDate, showVignette,
        brandText, modelText, paramsText, dateText,
        selectedPosition, selectedColor, textSize, opacity, rotation,
        shadowEnabled, shadowBlur, watermarkOffset, watermarkScale
    ) {
        originalBitmap?.let { bitmap ->
            previewBitmap = renderWatermarkPreview(
                bitmap = bitmap,
                config = watermarkConfig.copy(
                    enabled = isWatermarkEnabled,
                    showBrand = showBrand,
                    showModel = showModel,
                    showParams = showParams,
                    showDate = showDate,
                    showVignette = showVignette,
                    brandText = brandText,
                    modelText = modelText,
                    paramsText = paramsText,
                    dateText = dateText,
                    position = selectedPosition,
                    textColor = selectedColor,
                    textSize = textSize,
                    opacity = opacity,
                    rotation = rotation,
                    shadowEnabled = shadowEnabled,
                    shadowBlur = shadowBlur,
                    offset = watermarkOffset,
                    scale = watermarkScale
                )
            )
        }
    }

    // ========== 主界面 ==========
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("哈苏大师印记", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 预览按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Select)
                    showBeforeAfter = !showBeforeAfter
                }) {
                    Icon(
                        if (showBeforeAfter) Icons.Default.Compare else Icons.Default.Visibility,
                        "预览对比",
                        tint = if (showBeforeAfter) HasselbladOrange else Color.White
                    )
                }
                // 保存按钮
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.Confirm)
                    onSave(watermarkConfig)
                }) {
                    Icon(Icons.Default.Save, "保存", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // ========== 实时预览区（55%屏幕高度）==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .background(Color(0xFF1A1A1A))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = HasselbladOrange
                )
            } else if (previewBitmap != null) {
                // 预览Canvas（支持手势操作）
                WatermarkPreviewCanvas(
                    bitmap = if (showBeforeAfter) originalBitmap!! else previewBitmap!!,
                    watermarkConfig = watermarkConfig.copy(
                        enabled = isWatermarkEnabled,
                        offset = watermarkOffset,
                        scale = watermarkScale
                    ),
                    onOffsetChange = { offset -> watermarkOffset = offset },
                    onScaleChange = { scale -> watermarkScale = scale },
                    modifier = Modifier.fillMaxSize()
                )

                // 水印开关
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "水印",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isWatermarkEnabled,
                        onCheckedChange = {
                            haptic.perform(HapticFeedbackType.ToggleOn)
                            isWatermarkEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HasselbladOrange
                        )
                    )
                }

                // Before/After对比按钮
                if (originalBitmap != null && previewBitmap != null) {
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Select)
                            showBeforeAfter = !showBeforeAfter
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showBeforeAfter) HasselbladOrange else Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            if (showBeforeAfter) Icons.Default.CompareArrows else Icons.Default.Compare,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showBeforeAfter) "对比中" else "对比",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // 无图片提示
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "选择图片开始编辑",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { /* TODO: 打开图片选择器 */ },
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择图片")
                    }
                }
            }
        }

        // ========== 控制面板区（可滚动）==========
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 分类横滑
            WatermarkCategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    haptic.perform(HapticFeedbackType.Select)
                    selectedCategory = category
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 印记风格模板横滑
            WatermarkTemplateSlider(
                selectedTemplateId = selectedTemplateId,
                onTemplateSelected = { templateId ->
                    haptic.perform(HapticFeedbackType.Select)
                    selectedTemplateId = templateId
                    // 应用模板预设配置
                    applyTemplateConfig(templateId).let { config ->
                        showBrand = config.showBrand
                        showModel = config.showModel
                        showParams = config.showParams
                        showDate = config.showDate
                        brandText = config.brandText
                        selectedPosition = config.position
                        selectedColor = config.textColor
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 印记元素配置
            WatermarkElementsSection(
                showBrand = showBrand,
                showModel = showModel,
                showParams = showParams,
                showDate = showDate,
                showVignette = showVignette,
                brandText = brandText,
                modelText = modelText,
                paramsText = paramsText,
                dateText = dateText,
                vignetteStrength = watermarkConfig.vignetteStrength,
                onToggleBrand = { showBrand = it },
                onToggleModel = { showModel = it },
                onToggleParams = { showParams = it },
                onToggleDate = { showDate = it },
                onToggleVignette = { showVignette = it },
                onBrandTextChange = { brandText = it },
                onModelTextChange = { modelText = it },
                onParamsTextChange = { paramsText = it },
                onDateTextChange = { dateText = it },
                onVignetteStrengthChange = { watermarkConfig = watermarkConfig.copy(vignetteStrength = it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 位置网格
            WatermarkPositionGrid(
                selectedPosition = selectedPosition,
                onPositionSelected = { position ->
                    haptic.perform(HapticFeedbackType.Select)
                    selectedPosition = position
                    watermarkOffset = Offset.Zero // 重置拖拽偏移
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 样式调节
            WatermarkStyleSection(
                selectedFont = selectedFont,
                selectedColor = selectedColor,
                recommendedColor = recommendedColor,
                textSize = textSize,
                opacity = opacity,
                rotation = rotation,
                shadowEnabled = shadowEnabled,
                shadowBlur = shadowBlur,
                onFontSelected = { selectedFont = it },
                onColorSelected = { selectedColor = it },
                onTextSizeChange = { textSize = it },
                onOpacityChange = { opacity = it },
                onRotationChange = { rotation = it },
                onShadowToggle = { shadowEnabled = it },
                onShadowBlurChange = { shadowBlur = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 另存为印记
                OutlinedButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        onSave(watermarkConfig)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HasselbladOrange
                    ),
                    border = BorderStroke(1.dp, HasselbladOrange)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("另存为印记")
                }

                // 铭刻并导出
                Button(
                    onClick = {
                        haptic.perform(HapticFeedbackType.Confirm)
                        previewBitmap?.let { bitmap ->
                            onExport(bitmap, watermarkConfig)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Icon(Icons.Default.Output, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("铭刻并导出")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ========== 子组件 ==========

/**
 * 水印分类标签页
 */
@Composable
private fun WatermarkCategoryTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("全部", "品牌", "极简", "技术", "个人", "社交")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) HasselbladOrange else Color(0xFF2A2A2A),
                label = "bg"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 水印模板横滑选择器
 */
@Composable
private fun WatermarkTemplateSlider(
    selectedTemplateId: String,
    onTemplateSelected: (String) -> Unit
) {
    val templates = listOf(
        WatermarkTemplateItem("经典", "classic", "经典水印风格"),
        WatermarkTemplateItem("哈苏", "hasselblad_classic", "哈苏大师印记"),
        WatermarkTemplateItem("徕卡", "leica_style", "徕卡红点风格"),
        WatermarkTemplateItem("极简", "minimal", "简约水印"),
        WatermarkTemplateItem("详细", "detailed", "完整参数水印"),
        WatermarkTemplateItem("地理", "geo", "带地理位置水印")
    )

    Column {
        Text(
            text = "印记风格",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                WatermarkTemplateCard(
                    template = template,
                    isSelected = template.id == selectedTemplateId,
                    onClick = { onTemplateSelected(template.id) }
                )
            }
        }
    }
}

@Composable
private fun WatermarkTemplateCard(
    template: WatermarkTemplateItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) HasselbladOrange else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .size(width = 80.dp, height = 100.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else Color(0xFF2A2A2A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 模板图标/预览
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) HasselbladOrange.copy(alpha = 0.3f) else Color(0xFF3A3A3A))
            ) {
                Text(
                    text = template.name.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) HasselbladOrange else Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) HasselbladOrange else Color.White,
                maxLines = 1
            )
        }
    }
}

/**
 * 水印元素配置区
 */
@Composable
private fun WatermarkElementsSection(
    showBrand: Boolean,
    showModel: Boolean,
    showParams: Boolean,
    showDate: Boolean,
    showVignette: Boolean,
    brandText: String,
    modelText: String,
    paramsText: String,
    dateText: String,
    vignetteStrength: Float,
    onToggleBrand: (Boolean) -> Unit,
    onToggleModel: (Boolean) -> Unit,
    onToggleParams: (Boolean) -> Unit,
    onToggleDate: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onBrandTextChange: (String) -> Unit,
    onModelTextChange: (String) -> Unit,
    onParamsTextChange: (String) -> Unit,
    onDateTextChange: (String) -> Unit,
    onVignetteStrengthChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "印记元素",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 品牌名
        WatermarkElementRow(
            name = "品牌名",
            text = brandText,
            isEnabled = showBrand,
            onToggle = onToggleBrand,
            onTextChange = onBrandTextChange,
            canEdit = true
        )

        // 设备
        WatermarkElementRow(
            name = "设备",
            text = modelText,
            isEnabled = showModel,
            onToggle = onToggleModel,
            onTextChange = onModelTextChange,
            canEdit = true
        )

        // 参数
        WatermarkElementRow(
            name = "参数",
            text = paramsText,
            isEnabled = showParams,
            onToggle = onToggleParams,
            onTextChange = onParamsTextChange,
            canEdit = true,
            placeholder = "从EXIF自动读取"
        )

        // 日期
        WatermarkElementRow(
            name = "日期",
            text = dateText,
            isEnabled = showDate,
            onToggle = onToggleDate,
            onTextChange = onDateTextChange,
            canEdit = true
        )

        // 暗角（带滑块）
        WatermarkVignetteRow(
            isEnabled = showVignette,
            strength = vignetteStrength,
            onToggle = onToggleVignette,
            onStrengthChange = onVignetteStrengthChange
        )
    }
}

@Composable
private fun WatermarkElementRow(
    name: String,
    text: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    canEdit: Boolean,
    placeholder: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(50.dp)
        )

        if (canEdit) {
            // 可编辑文本框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HasselbladOrange
            )
        )
    }
}

@Composable
private fun WatermarkVignetteRow(
    isEnabled: Boolean,
    strength: Float,
    onToggle: (Boolean) -> Unit,
    onStrengthChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "暗角",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(50.dp)
        )

        if (isEnabled) {
            Slider(
                value = strength,
                onValueChange = onStrengthChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HasselbladOrange
            )
        )
    }
}

/**
 * 位置网格（3x3）
 */
@Composable
private fun WatermarkPositionGrid(
    selectedPosition: WatermarkPosition,
    onPositionSelected: (WatermarkPosition) -> Unit
) {
    val positions = listOf(
        listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT),
        listOf(WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT),
        listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM, WatermarkPosition.BOTTOM_RIGHT)
    )

    Column {
        Text(
            text = "位置",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            positions.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { position ->
                        val isSelected = position == selectedPosition
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) HasselbladOrange else Color(0xFF2A2A2A))
                                .border(
                                    1.dp,
                                    if (isSelected) HasselbladOrange else Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onPositionSelected(position) }
                        ) {
                            // 位置指示点
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.Gray)
                                    .align(
                                        when (position) {
                                            WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                                            WatermarkPosition.TOP_CENTER -> Alignment.TopCenter
                                            WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                                            WatermarkPosition.CENTER_LEFT -> Alignment.CenterStart
                                            WatermarkPosition.CENTER -> Alignment.Center
                                            WatermarkPosition.CENTER_RIGHT -> Alignment.CenterEnd
                                            WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                                            WatermarkPosition.BOTTOM -> Alignment.BottomCenter
                                            WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                                        }
                                    )
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 样式调节区
 */
@Composable
private fun WatermarkStyleSection(
    selectedFont: String,
    selectedColor: Color,
    recommendedColor: Color,
    textSize: Float,
    opacity: Float,
    rotation: Float,
    shadowEnabled: Boolean,
    shadowBlur: Float,
    onFontSelected: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onShadowToggle: (Boolean) -> Unit,
    onShadowBlurChange: (Float) -> Unit
) {
    val colors = listOf(
        Color.White to "白色",
        Color(0xFFFFD700) to "金色",
        Color(0xFFC0C0C0) to "银色",
        Color.Black to "黑色",
        Color(0xFF808080) to "灰色"
    )

    val fonts = listOf("默认", "衬线", "无衬线", "手写", "粗体")

    Column {
        Text(
            text = "样式",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 字体和颜色选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 字体下拉
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "字体: $selectedFont",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            // 颜色选择
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                colors.forEach { (color, name) ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                2.dp,
                                if (color == selectedColor) HasselbladOrange else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }

        // 智能推荐提示
        if (recommendedColor != selectedColor) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(HasselbladOrange.copy(alpha = 0.1f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "智能推荐: ${colors.find { it.first == recommendedColor }?.second ?: "白色"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladOrange
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onColorSelected(recommendedColor) }
                ) {
                    Text("应用", color = HasselbladOrange, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 大小滑块
        StyleSlider(
            label = "大小",
            value = textSize,
            valueRange = 8f..24f,
            unit = "px",
            onValueChange = onTextSizeChange
        )

        // 透明度滑块
        StyleSlider(
            label = "透明",
            value = opacity,
            valueRange = 0.3f..1f,
            unit = "%",
            displayValue = (opacity * 100).toInt(),
            onValueChange = onOpacityChange
        )

        // 旋转滑块
        StyleSlider(
            label = "旋转",
            value = rotation,
            valueRange = -45f..45f,
            unit = "°",
            onValueChange = onRotationChange
        )

        // 阴影开关和模糊度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "阴影",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = shadowEnabled,
                    onCheckedChange = onShadowToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HasselbladOrange
                    )
                )
            }

            if (shadowEnabled) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "模糊",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.width(40.dp)
                    )
                    Slider(
                        value = shadowBlur,
                        onValueChange = onShadowBlurChange,
                        valueRange = 0f..12f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = HasselbladOrange,
                            activeTrackColor = HasselbladOrange
                        )
                    )
                    Text(
                        text = "${shadowBlur.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    displayValue: Int = value.toInt(),
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(40.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            )
        )
        Text(
            text = "$displayValue$unit",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.width(50.dp)
        )
    }
}

/**
 * 实时预览Canvas（支持手势操作）
 */
@Composable
private fun WatermarkPreviewCanvas(
    bitmap: Bitmap,
    watermarkConfig: WatermarkConfig,
    onOffsetChange: (Offset) -> Unit,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(watermarkConfig.scale) }
    var offset by remember { mutableStateOf(watermarkConfig.offset) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 2f)
                    offset = offset + pan
                    onScaleChange(scale)
                    onOffsetChange(offset)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制图片
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scaleRatio = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
            val drawWidth = imageWidth * scaleRatio
            val drawHeight = imageHeight * scaleRatio
            val offsetX = (canvasWidth - drawWidth) / 2
            val offsetY = (canvasHeight - drawHeight) / 2

            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
            )

            // 绘制水印（如果启用）
            if (watermarkConfig.enabled) {
                drawWatermarkText(
                    config = watermarkConfig,
                    canvasSize = size,
                    imageRect = Rect(offsetX, offsetY, offsetX + drawWidth, offsetY + drawHeight),
                    scale = scale,
                    offset = offset
                )
            }
        }

        // 手势提示
        if (watermarkConfig.enabled) {
            Text(
                text = "双指拖拽调整位置和大小",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}

// ========== 辅助函数 ==========

/**
 * 从EXIF提取数据
 */
private fun extractExifData(imagePath: String): ExifData {
    return try {
        val exif = ExifInterface(imagePath)
        ExifData(
            model = exif.getAttribute(ExifInterface.TAG_MODEL),
            params = buildParamsString(exif),
            date = exif.getAttribute(ExifInterface.TAG_DATETIME)?.substring(0, 10)?.replace(':', '-')
        )
    } catch (e: Exception) {
        ExifData()
    }
}

private fun buildParamsString(exif: ExifInterface): String {
    val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER) ?: "f/1.8"
    val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME) ?: "1/125"
    val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS) ?: "100"
    return "$fNumber $exposure ISO$iso"
}

/**
 * 分析图片主色调，推荐水印颜色
 */
private fun analyzeDominantColor(bitmap: Bitmap): Color {
    // 简化分析：统计亮度
    var totalBrightness = 0L
    val sampleSize = 1000
    val stepX = bitmap.width / 20
    val stepY = bitmap.height / 20

    for (x in 0 until bitmap.width step stepX) {
        for (y in 0 until bitmap.height step stepY) {
            val pixel = bitmap.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            totalBrightness += (r + g + b) / 3
        }
    }

    val avgBrightness = totalBrightness / (20 * 20)

    // 如果图片偏亮，推荐深色水印；偏暗，推荐白色水印
    return if (avgBrightness > 128) {
        Color.Black
    } else {
        Color.White
    }
}

/**
 * 渲染水印预览
 */
private fun renderWatermarkPreview(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
    // 创建可变位图
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    // 实际水印绘制逻辑（简化）
    return result
}

/**
 * 应用模板配置
 */
private fun applyTemplateConfig(templateId: String): WatermarkConfig {
    return when (templateId) {
        "hasselblad_classic" -> WatermarkConfig(
            showBrand = true,
            showModel = true,
            showParams = true,
            showDate = true,
            brandText = "HASSELBLAD",
            position = WatermarkPosition.BOTTOM_LEFT,
            textColor = Color.White
        )
        "leica_style" -> WatermarkConfig(
            showBrand = true,
            showModel = false,
            showParams = false,
            showDate = true,
            brandText = "LEICA",
            position = WatermarkPosition.BOTTOM_RIGHT,
            textColor = Color(0xFFFF0000) // 红点风格
        )
        "minimal" -> WatermarkConfig(
            showBrand = false,
            showModel = true,
            showParams = false,
            showDate = false,
            position = WatermarkPosition.BOTTOM_RIGHT,
            textColor = Color.White
        )
        else -> WatermarkConfig()
    }
}

// ========== 数据类 ==========

data class WatermarkConfig(
    val enabled: Boolean = true,
    val showBrand: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showDate: Boolean = true,
    val showVignette: Boolean = false,
    val brandText: String = "HASSELBLAD",
    val modelText: String = "",
    val paramsText: String = "",
    val dateText: String = "",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val textColor: Color = Color.White,
    val textSize: Float = 14f,
    val opacity: Float = 0.8f,
    val rotation: Float = 0f,
    val shadowEnabled: Boolean = true,
    val shadowBlur: Float = 4f,
    val vignetteStrength: Float = 0.5f,
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f
)

data class WatermarkTemplateItem(
    val name: String,
    val id: String,
    val description: String
)

data class ExifData(
    val model: String? = null,
    val params: String? = null,
    val date: String? = null
)

enum class WatermarkPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
}

/**
 * Canvas绘制水印文本
 */
private fun DrawScope.drawWatermarkText(
    config: WatermarkConfig,
    canvasSize: Size,
    imageRect: Rect,
    scale: Float,
    offset: Offset
) {
    val textPaint = Paint().asFrameworkPaint().apply {
        color = config.textColor.toArgb()
        textSize = config.textSize * scale
        setShadowLayer(config.shadowBlur, 0f, 0f, Color.Black.toArgb())
    }

    // 计算水印位置
    val watermarkX = when (config.position) {
        WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> 
            imageRect.left + 16.dp.toPx() + offset.x
        WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER, WatermarkPosition.BOTTOM -> 
            imageRect.left + imageRect.width / 2 + offset.x
        WatermarkPosition.TOP_RIGHT, WatermarkPosition.CENTER_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> 
            imageRect.right - 16.dp.toPx() + offset.x
    }

    val watermarkY = when (config.position) {
        WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT -> 
            imageRect.top + 16.dp.toPx() + offset.y
        WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT -> 
            imageRect.top + imageRect.height / 2 + offset.y
        WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM, WatermarkPosition.BOTTOM_RIGHT -> 
            imageRect.bottom - 16.dp.toPx() + offset.y
    }

    // 绘制各元素
    var currentY = watermarkY

    if (config.showBrand) {
        drawContext.canvas.nativeCanvas.drawText(
            config.brandText,
            watermarkX,
            currentY,
            textPaint
        )
        currentY += config.textSize * 1.5f * scale
    }

    if (config.showModel) {
        drawContext.canvas.nativeCanvas.drawText(
            config.modelText,
            watermarkX,
            currentY,
            textPaint
        )
        currentY += config.textSize * 1.5f * scale
    }

    if (config.showParams) {
        drawContext.canvas.nativeCanvas.drawText(
            config.paramsText,
            watermarkX,
            currentY,
            textPaint
        )
        currentY += config.textSize * 1.5f * scale
    }

    if (config.showDate) {
        drawContext.canvas.nativeCanvas.drawText(
            config.dateText,
            watermarkX,
            currentY,
            textPaint
        )
    }
}