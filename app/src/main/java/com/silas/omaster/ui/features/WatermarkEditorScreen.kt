package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField

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
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.math.*

// ========== 模板分类 ==========
enum class WatermarkCategory(
    val id: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ALL("all", "全部", Icons.Default.Apps),
    BRAND("brand", "品牌", Icons.Default.Star),
    MINIMAL("minimal", "极简", Icons.Default.Minimize),
    TECH("tech", "技术", Icons.Default.Settings),
    INFO("info", "信息", Icons.Default.Info),
    PERSONAL("personal", "个人", Icons.Default.Person),
    SOCIAL("social", "社交", Icons.Default.AlternateEmail),
    LEGAL("legal", "法律", Icons.Default.Copyright),
    BADGE("badge", "徽章", Icons.Default.EmojiEvents),
    PRO("pro", "专业", Icons.Default.AutoAwesome)
}

// ========== 水印模板 ==========
data class WatermarkTemplate(
    val id: String,
    val name: String,
    val category: WatermarkCategory,
    val showBrand: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showDate: Boolean = true,
    val showLocation: Boolean = false,
    val showPhotographer: Boolean = false,
    val brandText: String = "OMaster",
    val defaultPosition: WatermarkPlacement = WatermarkPlacement.BOTTOM_LEFT,
    val defaultFontSize: Float = 14f,
    val defaultLetterSpacing: Float = 0f,
    val isBold: Boolean = false
)

// 模板列表（与Web端同步）
val WATERMARK_TEMPLATES = listOf(
    WatermarkTemplate(
        id = "classic",
        name = "经典相机",
        category = WatermarkCategory.BRAND,
        brandText = "OMaster",
        defaultPosition = WatermarkPlacement.BOTTOM_LEFT
    ),
    WatermarkTemplate(
        id = "hasselblad",
        name = "哈苏大师",
        category = WatermarkCategory.BRAND,
        brandText = "HASSELBLAD",
        defaultPosition = WatermarkPlacement.BOTTOM_CENTER,
        defaultLetterSpacing = 2f,
        isBold = true
    ),
    WatermarkTemplate(
        id = "leica",
        name = "徕卡风格",
        category = WatermarkCategory.BRAND,
        brandText = "Leica",
        defaultPosition = WatermarkPlacement.BOTTOM_RIGHT,
        defaultLetterSpacing = 3f,
        isBold = true
    ),
    WatermarkTemplate(
        id = "minimal",
        name = "极简风格",
        category = WatermarkCategory.MINIMAL,
        showModel = false,
        showParams = false,
        showDate = false,
        brandText = "OM",
        defaultPosition = WatermarkPlacement.BOTTOM_RIGHT,
        defaultFontSize = 20f,
        isBold = true
    ),
    WatermarkTemplate(
        id = "detailed",
        name = "详细参数",
        category = WatermarkCategory.TECH,
        defaultPosition = WatermarkPlacement.BOTTOM_LEFT
    ),
    WatermarkTemplate(
        id = "location",
        name = "地理位置",
        category = WatermarkCategory.INFO,
        showLocation = true,
        defaultPosition = WatermarkPlacement.BOTTOM_LEFT
    ),
    WatermarkTemplate(
        id = "signature",
        name = "摄影师签名",
        category = WatermarkCategory.PERSONAL,
        showPhotographer = true,
        showBrand = false,
        showModel = false,
        showParams = false,
        defaultPosition = WatermarkPlacement.BOTTOM_RIGHT
    ),
    WatermarkTemplate(
        id = "social",
        name = "社交媒体",
        category = WatermarkCategory.SOCIAL,
        showBrand = false,
        showModel = false,
        showParams = false,
        showDate = false,
        brandText = "@omaster",
        defaultPosition = WatermarkPlacement.BOTTOM_CENTER
    ),
    WatermarkTemplate(
        id = "timestamp",
        name = "时间戳",
        category = WatermarkCategory.INFO,
        showBrand = false,
        showModel = false,
        showParams = false,
        defaultPosition = WatermarkPlacement.TOP_RIGHT
    ),
    WatermarkTemplate(
        id = "copyright",
        name = "版权声明",
        category = WatermarkCategory.LEGAL,
        showBrand = false,
        showModel = false,
        showParams = false,
        brandText = "© 2026 OMaster",
        defaultPosition = WatermarkPlacement.BOTTOM_CENTER
    ),
    WatermarkTemplate(
        id = "award",
        name = "获奖作品",
        category = WatermarkCategory.BADGE,
        showBrand = false,
        showModel = false,
        showParams = false,
        showDate = false,
        brandText = "Award Winning",
        defaultPosition = WatermarkPlacement.TOP_LEFT
    ),
    WatermarkTemplate(
        id = "exif",
        name = "EXIF信息",
        category = WatermarkCategory.TECH,
        defaultPosition = WatermarkPlacement.BOTTOM_LEFT,
        defaultFontSize = 11f
    ),
    WatermarkTemplate(
        id = "logo",
        name = "品牌Logo",
        category = WatermarkCategory.BRAND,
        showModel = false,
        showParams = false,
        showDate = false,
        defaultPosition = WatermarkPlacement.BOTTOM_RIGHT
    ),
    WatermarkTemplate(
        id = "watermark_pro",
        name = "专业防伪",
        category = WatermarkCategory.PRO,
        showBrand = false,
        showModel = false,
        showParams = false,
        showDate = false,
        brandText = "PROTECTED",
        defaultPosition = WatermarkPlacement.CENTER,
        defaultLetterSpacing = 4f
    ),
    WatermarkTemplate(
        id = "custom",
        name = "自定义",
        category = WatermarkCategory.ALL,
        defaultPosition = WatermarkPlacement.BOTTOM_LEFT
    )
)

// ========== 字体选项 ==========
enum class FontOption(
    val id: String,
    val displayName: String,
    val fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    DEFAULT("default", "默认", FontFamily.Default),
    SERIF("serif", "衬线", FontFamily.Serif),
    MONOSPACE("mono", "等宽", FontFamily.Monospace),
    ELEGANT("elegant", "优雅", FontFamily.Serif),
    SANS_SERIF("sans", "现代", FontFamily.SansSerif),
    CURSIVE("cursive", "手写", FontFamily.Cursive)
}

/**
 * 水印编辑器独立页面
 * 
 * 功能特性：
 * - 实时预览区（Canvas渲染）
 * - 水印模板分类和选择（10个分类，15+模板）
 * - 水印元素开关和编辑（品牌/设备/参数/时间/位置/摄影师）
 * - 位置网格选择（7宫格）
 * - 样式调节（字体/字号/颜色/阴影/边距/字间距/背景）
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
    onExport: (Bitmap, WatermarkConfig) -> Unit,
    modifier: Modifier = Modifier
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

    // 元素文本
    var brandText by remember { mutableStateOf("OMaster") }
    var modelText by remember { mutableStateOf("OPPO Find X8 Pro") }
    var paramsText by remember { mutableStateOf("f/1.8 1/125 ISO100") }
    var dateText by remember { mutableStateOf("2026-06-09") }

    // 智能颜色推荐
    var recommendedColor by remember { mutableStateOf(Color.White) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                    originalBitmap = bitmap

                    // 从 URI 读取 EXIF
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val exif = ExifInterface(stream)
                            modelText = exif.getAttribute(ExifInterface.TAG_MODEL) ?: modelText
                            val params = buildParamsString(exif)
                            if (params.isNotBlank()) paramsText = params
                            val date = exif.getAttribute(ExifInterface.TAG_DATETIME)
                                ?.substring(0, 10)
                                ?.replace(':', '-')
                            if (!date.isNullOrBlank()) dateText = date
                        }
                    } catch (e: Exception) {
                        Log.w("WatermarkEditor", "EXIF read failed", e)
                    }

                    if (bitmap != null) {
                        recommendedColor = analyzeDominantColor(bitmap)
                    }
                } catch (e: Exception) {
                    Log.w("WatermarkEditor", "Image load failed", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // ========== 状态管理（续）==========
    // originalBitmap, previewBitmap, isWatermarkEnabled, showBeforeAfter, isLoading,
    // brandText, modelText, paramsText, dateText, recommendedColor 已在上方声明

    // 水印配置
    var selectedCategory by remember { mutableStateOf(WatermarkCategory.ALL) }
    var selectedTemplate by remember { mutableStateOf(WATERMARK_TEMPLATES[0]) }
    var watermarkConfig by remember { mutableStateOf(WatermarkConfig()) }

    // 元素开关
    var showBrand by remember { mutableStateOf(true) }
    var showModel by remember { mutableStateOf(true) }
    var showParams by remember { mutableStateOf(true) }
    var showDate by remember { mutableStateOf(true) }
    var showLocation by remember { mutableStateOf(false) }
    var showPhotographer by remember { mutableStateOf(false) }
    var showVignette by remember { mutableStateOf(false) }

    // 元素文本（续）
    var locationText by remember { mutableStateOf("北京市朝阳区") }
    var photographerText by remember { mutableStateOf("摄影师") }

    // 样式参数
    var selectedPosition by remember { mutableStateOf(WatermarkPlacement.BOTTOM_LEFT) }
    var selectedFont by remember { mutableStateOf(FontOption.DEFAULT) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var textSize by remember { mutableFloatStateOf(14f) }
    var opacity by remember { mutableFloatStateOf(0.8f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var shadowBlur by remember { mutableFloatStateOf(4f) }
    var padding by remember { mutableFloatStateOf(20f) }
    var letterSpacing by remember { mutableFloatStateOf(0f) }
    var fontWeight by remember { mutableStateOf(FontWeight.Normal) }
    var bgOpacity by remember { mutableFloatStateOf(0f) }

    // 拖拽状态
    var watermarkOffset by remember { mutableStateOf(Offset.Zero) }
    var watermarkScale by remember { mutableFloatStateOf(1f) }

    // 搜索
    var searchQuery by remember { mutableStateOf("") }

    // ========== 初始化加载图片 ==========
    LaunchedEffect(imagePath) {
        isLoading = true
        try {
            val bitmap = if (imagePath != null) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                null
            }
            originalBitmap = bitmap

            if (imagePath != null) {
                extractExifData(imagePath).let { exif ->
                    modelText = exif.model ?: modelText
                    paramsText = exif.params ?: paramsText
                    dateText = exif.date ?: dateText
                }
            }

            if (bitmap != null) {
                recommendedColor = analyzeDominantColor(bitmap)
            }
        } catch (e: Exception) {
            Log.w("WatermarkEditor", "Init image load failed", e)
        }
        isLoading = false
    }

    // ========== 实时预览更新 ==========
    LaunchedEffect(
        isWatermarkEnabled, showBrand, showModel, showParams, showDate, showLocation, showPhotographer, showVignette,
        brandText, modelText, paramsText, dateText, locationText, photographerText,
        selectedPosition, selectedColor, textSize, opacity, rotation,
        shadowEnabled, shadowBlur, padding, letterSpacing, fontWeight, bgOpacity,
        watermarkOffset, watermarkScale
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
                    showLocation = showLocation,
                    showPhotographer = showPhotographer,
                    showVignette = showVignette,
                    brandText = brandText,
                    modelText = modelText,
                    paramsText = paramsText,
                    dateText = dateText,
                    locationText = locationText,
                    photographerText = photographerText,
                    position = selectedPosition,
                    textColor = selectedColor,
                    textSize = textSize,
                    opacity = opacity,
                    rotation = rotation,
                    shadowEnabled = shadowEnabled,
                    shadowBlur = shadowBlur,
                    padding = padding,
                    letterSpacing = letterSpacing,
                    fontWeight = fontWeight,
                    bgOpacity = bgOpacity,
                    offset = watermarkOffset,
                    scale = watermarkScale
                )
            )
        }
    }

    // 过滤模板
    val filteredTemplates = remember(selectedCategory, searchQuery) {
        WATERMARK_TEMPLATES.filter { template ->
            val categoryMatch = selectedCategory == WatermarkCategory.ALL || template.category == selectedCategory
            val searchMatch = searchQuery.isBlank() || template.name.contains(searchQuery, ignoreCase = true)
            categoryMatch && searchMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TopAppBar
        TopAppBar(
            title = { 
                Column {
                    Text("水印编辑器", fontWeight = FontWeight.Bold)
                    Text(
                        "专业水印设计 · ${WATERMARK_TEMPLATES.size}+模板",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                // 图层按钮
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showBeforeAfter = !showBeforeAfter
                }) {
                    Icon(
                        Icons.Default.Layers, "图层",
                        tint = if (showBeforeAfter) CyanAccent else Color.White.copy(alpha = 0.5f)
                    )
                }
                // 预览按钮
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showBeforeAfter = !showBeforeAfter
                }) {
                    Icon(
                        if (showBeforeAfter) Icons.Default.Compare else Icons.Default.Visibility,
                        "预览对比",
                        tint = if (showBeforeAfter) CyanAccent else Color.White.copy(alpha = 0.5f)
                    )
                }
                // 导出按钮
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    previewBitmap?.let { onExport(it, watermarkConfig) }
                }) {
                    Icon(Icons.Default.Download, "导出", tint = CyanAccent)
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
                .background(DarkGray)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyanAccent
                )
            } else if (previewBitmap != null) {
                // 预览Canvas（支持手势操作）
                WatermarkPreviewCanvas(
                    bitmap = if (showBeforeAfter && originalBitmap != null) originalBitmap!! else previewBitmap!!,
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
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isWatermarkEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyanAccent
                        )
                    )
                }

                // 对比按钮
                if (originalBitmap != null && previewBitmap != null) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showBeforeAfter = !showBeforeAfter
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showBeforeAfter) CyanAccent else Color(0xFF2A2A2A)
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            imagePickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
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
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedCategory = category
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索框
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 模板网格选择
            WatermarkTemplateGrid(
                templates = filteredTemplates,
                selectedTemplate = selectedTemplate,
                onTemplateSelected = { template ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedTemplate = template
                    // 应用模板配置
                    showBrand = template.showBrand
                    showModel = template.showModel
                    showParams = template.showParams
                    showDate = template.showDate
                    showLocation = template.showLocation
                    showPhotographer = template.showPhotographer
                    brandText = template.brandText
                    selectedPosition = template.defaultPosition
                    textSize = template.defaultFontSize
                    letterSpacing = template.defaultLetterSpacing
                    fontWeight = if (template.isBold) FontWeight.Bold else FontWeight.Normal
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 位置网格（7宫格）
            WatermarkPositionGrid(
                selectedPosition = selectedPosition,
                onPositionSelected = { position ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedPosition = position
                    watermarkOffset = Offset.Zero
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 自定义文字
            CustomTextInput(
                label = "自定义文字",
                value = brandText,
                onValueChange = { brandText = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 印记元素配置
            WatermarkElementsSection(
                showBrand = showBrand,
                showModel = showModel,
                showParams = showParams,
                showDate = showDate,
                showLocation = showLocation,
                showPhotographer = showPhotographer,
                showVignette = showVignette,
                brandText = brandText,
                modelText = modelText,
                paramsText = paramsText,
                dateText = dateText,
                locationText = locationText,
                photographerText = photographerText,
                vignetteStrength = watermarkConfig.vignetteStrength,
                onToggleBrand = { showBrand = it },
                onToggleModel = { showModel = it },
                onToggleParams = { showParams = it },
                onToggleDate = { showDate = it },
                onToggleLocation = { showLocation = it },
                onTogglePhotographer = { showPhotographer = it },
                onToggleVignette = { showVignette = it },
                onBrandTextChange = { brandText = it },
                onModelTextChange = { modelText = it },
                onParamsTextChange = { paramsText = it },
                onDateTextChange = { dateText = it },
                onLocationTextChange = { locationText = it },
                onPhotographerTextChange = { photographerText = it },
                onVignetteStrengthChange = { watermarkConfig = watermarkConfig.copy(vignetteStrength = it) }
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
                padding = padding,
                letterSpacing = letterSpacing,
                fontWeight = fontWeight,
                bgOpacity = bgOpacity,
                onFontSelected = { selectedFont = it },
                onColorSelected = { selectedColor = it },
                onTextSizeChange = { textSize = it },
                onOpacityChange = { opacity = it },
                onRotationChange = { rotation = it },
                onShadowToggle = { shadowEnabled = it },
                onShadowBlurChange = { shadowBlur = it },
                onPaddingChange = { padding = it },
                onLetterSpacingChange = { letterSpacing = it },
                onFontWeightChange = { fontWeight = it },
                onBgOpacityChange = { bgOpacity = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 底部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 重置默认
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        brandText = "Shot on OMaster"
                        selectedPosition = WatermarkPlacement.BOTTOM_LEFT
                        textSize = 14f
                        opacity = 0.8f
                        rotation = 0f
                        selectedColor = Color.White
                        letterSpacing = 0f
                        fontWeight = FontWeight.Normal
                        bgOpacity = 0f
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanAccent
                    ),
                    border = BorderStroke(1.dp, CyanAccent)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置默认", style = MaterialTheme.typography.labelMedium)
                }

                // 批量应用
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // 批量应用功能
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanAccent
                    ),
                    border = BorderStroke(1.dp, CyanAccent)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("批量应用", style = MaterialTheme.typography.labelMedium)
                }

                // 保存图片
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        previewBitmap?.let { onExport(it, watermarkConfig) }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存图片", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ========== 子组件 ==========

/**
 * 水印分类标签页（10个分类）
 */
@Composable
private fun WatermarkCategoryTabs(
    selectedCategory: WatermarkCategory,
    onCategorySelected: (WatermarkCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WatermarkCategory.entries) { category ->
            val isSelected = category == selectedCategory
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) CyanAccent else Color(0xFF2A2A2A),
                label = "bg"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    category.icon,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 搜索框
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGray)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            Icons.Default.Search,
            null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "搜索水印模板...",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * 水印模板网格选择（4列）
 */
@Composable
private fun WatermarkTemplateGrid(
    templates: List<WatermarkTemplate>,
    selectedTemplate: WatermarkTemplate,
    onTemplateSelected: (WatermarkTemplate) -> Unit
) {
    Column {
        Text(
            text = "水印模板 (${templates.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 使用普通网格布局避免嵌套滚动问题
        val rows = templates.chunked(4)
        rows.forEach { rowTemplates ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTemplates.forEach { template ->
                    WatermarkTemplateCard(
                        template = template,
                        isSelected = template.id == selectedTemplate.id,
                        onClick = { onTemplateSelected(template) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 填充空位
                repeat(4 - rowTemplates.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WatermarkTemplateCard(
    template: WatermarkTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CyanAccent else Color.Transparent,
        label = "border"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanAccent.copy(alpha = 0.15f) else Color(0xFF2A2A2A))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 模板图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) CyanAccent.copy(alpha = 0.3f) else Color(0xFF3A3A3A))
        ) {
            Icon(
                template.category.icon,
                null,
                tint = if (isSelected) CyanAccent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = template.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CyanAccent else Color.White.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

/**
 * 位置网格（7宫格，与Web端对齐）
 */
@Composable
private fun WatermarkPositionGrid(
    selectedPosition: WatermarkPlacement,
    onPositionSelected: (WatermarkPlacement) -> Unit
) {
    // Web端7位置布局
    val positions = listOf(
        listOf(WatermarkPlacement.TOP_LEFT, WatermarkPlacement.TOP_CENTER, WatermarkPlacement.TOP_RIGHT),
        listOf(null, WatermarkPlacement.CENTER, null),
        listOf(WatermarkPlacement.BOTTOM_LEFT, WatermarkPlacement.BOTTOM_CENTER, WatermarkPlacement.BOTTOM_RIGHT)
    )

    Column {
        Text(
            text = "水印位置",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.5f)
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
                        if (position != null) {
                            val isSelected = position == selectedPosition
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) CyanAccent else Color(0xFF2A2A2A))
                                    .border(
                                        1.dp,
                                        if (isSelected) CyanAccent else Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onPositionSelected(position) }
                            ) {
                                // 位置指示点
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.Gray)
                                        .align(
                                            when (position) {
                                                WatermarkPlacement.TOP_LEFT -> Alignment.TopStart
                                                WatermarkPlacement.TOP_CENTER -> Alignment.TopCenter
                                                WatermarkPlacement.TOP_RIGHT -> Alignment.TopEnd
                                                WatermarkPlacement.CENTER -> Alignment.Center
                                                WatermarkPlacement.BOTTOM_LEFT -> Alignment.BottomStart
                                                WatermarkPlacement.BOTTOM_CENTER -> Alignment.BottomCenter
                                                WatermarkPlacement.BOTTOM_RIGHT -> Alignment.BottomEnd
                                                else -> Alignment.Center
                                            }
                                        )
                                        .padding(4.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 自定义文字输入
 */
@Composable
private fun CustomTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkGray)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
    showLocation: Boolean,
    showPhotographer: Boolean,
    showVignette: Boolean,
    brandText: String,
    modelText: String,
    paramsText: String,
    dateText: String,
    locationText: String,
    photographerText: String,
    vignetteStrength: Float,
    onToggleBrand: (Boolean) -> Unit,
    onToggleModel: (Boolean) -> Unit,
    onToggleParams: (Boolean) -> Unit,
    onToggleDate: (Boolean) -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onTogglePhotographer: (Boolean) -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onBrandTextChange: (String) -> Unit,
    onModelTextChange: (String) -> Unit,
    onParamsTextChange: (String) -> Unit,
    onDateTextChange: (String) -> Unit,
    onLocationTextChange: (String) -> Unit,
    onPhotographerTextChange: (String) -> Unit,
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
            name = "品牌",
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
            name = "时间",
            text = dateText,
            isEnabled = showDate,
            onToggle = onToggleDate,
            onTextChange = onDateTextChange,
            canEdit = true
        )

        // 位置（新增）
        WatermarkElementRow(
            name = "位置",
            text = locationText,
            isEnabled = showLocation,
            onToggle = onToggleLocation,
            onTextChange = onLocationTextChange,
            canEdit = true
        )

        // 摄影师签名（新增）
        WatermarkElementRow(
            name = "签名",
            text = photographerText,
            isEnabled = showPhotographer,
            onToggle = onTogglePhotographer,
            onTextChange = onPhotographerTextChange,
            canEdit = true
        )

        // 暗角
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkGray)
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
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
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
                checkedTrackColor = CyanAccent
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
                    thumbColor = CyanAccent,
                    activeTrackColor = CyanAccent
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
                checkedTrackColor = CyanAccent
            )
        )
    }
}

/**
 * 样式调节区
 */
@Composable
private fun WatermarkStyleSection(
    selectedFont: FontOption,
    selectedColor: Color,
    recommendedColor: Color,
    textSize: Float,
    opacity: Float,
    rotation: Float,
    shadowEnabled: Boolean,
    shadowBlur: Float,
    padding: Float,
    letterSpacing: Float,
    fontWeight: FontWeight,
    bgOpacity: Float,
    onFontSelected: (FontOption) -> Unit,
    onColorSelected: (Color) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onShadowToggle: (Boolean) -> Unit,
    onShadowBlurChange: (Float) -> Unit,
    onPaddingChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onFontWeightChange: (FontWeight) -> Unit,
    onBgOpacityChange: (Float) -> Unit
) {
    // 8种预设颜色（与Web端同步）
    val colors = listOf(
        Color.White to "白色",
        Color.Black to "黑色",
        Color(0xFFFFD700) to "金色",
        Color(0xFFFF6B35) to "橙色",
        CyanAccent to "青色",
        Color(0xFFFF6B9D) to "粉色",
        SuccessGreen to "绿色",
        Color(0xFF9C27B0) to "紫色"
    )

    Column {
        Text(
            text = "样式",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 字体选择
        Text(
            text = "字体样式",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FontOption.entries) { font ->
                val isSelected = font == selectedFont
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color(0xFF2A2A2A))
                        .border(
                            1.dp,
                            if (isSelected) CyanAccent else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onFontSelected(font) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = font.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) CyanAccent else Color.White,
                        fontFamily = font.fontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 字体粗细
        Text(
            text = "字体粗细",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(FontWeight.Normal to "常规", FontWeight.Bold to "粗体").forEach { (weight, name) ->
                val isSelected = fontWeight == weight
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent.copy(alpha = 0.2f) else Color(0xFF2A2A2A))
                        .border(
                            1.dp,
                            if (isSelected) CyanAccent else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onFontWeightChange(weight) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) CyanAccent else Color.White,
                        fontWeight = weight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 颜色选择（8种）
        Text(
            text = "文字颜色",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { (color, _) ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (color == selectedColor) CyanAccent else Color.Transparent,
                            CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }

        // 智能推荐提示
        if (recommendedColor != selectedColor) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanAccent.copy(alpha = 0.1f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "智能推荐: ${colors.find { it.first == recommendedColor }?.second ?: "白色"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onColorSelected(recommendedColor) }
                ) {
                    Text("应用", color = CyanAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 大小滑块
        StyleSlider(
            label = "字体大小",
            value = textSize,
            valueRange = 8f..48f,
            unit = "px",
            onValueChange = onTextSizeChange
        )

        // 透明度滑块
        StyleSlider(
            label = "透明度",
            value = opacity,
            valueRange = 0f..1f,
            unit = "%",
            displayValue = (opacity * 100).toInt(),
            onValueChange = onOpacityChange
        )

        // 旋转滑块
        StyleSlider(
            label = "旋转角度",
            value = rotation,
            valueRange = -45f..45f,
            unit = "°",
            onValueChange = onRotationChange
        )

        // 边距滑块（新增）
        StyleSlider(
            label = "边距",
            value = padding,
            valueRange = 8f..60f,
            unit = "px",
            onValueChange = onPaddingChange
        )

        // 字间距滑块（新增）
        StyleSlider(
            label = "字间距",
            value = letterSpacing,
            valueRange = 0f..10f,
            unit = "px",
            onValueChange = onLetterSpacingChange
        )

        // 背景透明度滑块（新增）
        StyleSlider(
            label = "背景透明度",
            value = bgOpacity,
            valueRange = 0f..0.8f,
            unit = "%",
            displayValue = (bgOpacity * 100).toInt(),
            onValueChange = onBgOpacityChange
        )

        // 阴影开关和模糊度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "文字阴影",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = shadowEnabled,
                    onCheckedChange = onShadowToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CyanAccent
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
                        valueRange = 0f..15f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAccent,
                            activeTrackColor = CyanAccent
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
            modifier = Modifier.width(70.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent
            )
        )
        Text(
            text = "$displayValue$unit",
            style = MaterialTheme.typography.bodySmall,
            color = CyanAccent,
            fontWeight = FontWeight.Bold,
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

            // 绘制水印
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

private fun analyzeDominantColor(bitmap: Bitmap): Color {
    var totalBrightness = 0L
    val stepX = bitmap.width / 20
    val stepY = bitmap.height / 20

    for (x in 0 until bitmap.width step stepX) {
        for (y in 0 until bitmap.height step stepY) {
            val pixel = bitmap.getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            totalBrightness += (r + g + b) / 3
        }
    }

    val avgBrightness = totalBrightness / (20 * 20)
    return if (avgBrightness > 128) Color.Black else Color.White
}

private fun renderWatermarkPreview(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    if (!config.enabled) return result

    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.textSize * result.width / 400f
        color = config.textColor.copy(alpha = config.opacity).toArgb()
        letterSpacing = config.letterSpacing
        typeface = if (config.fontWeight >= FontWeight.Bold) {
            android.graphics.Typeface.DEFAULT_BOLD
        } else {
            android.graphics.Typeface.DEFAULT
        }
    }

    val shadowPaint = if (config.shadowEnabled) Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = config.textSize * result.width / 400f
        color = android.graphics.Color.argb((config.opacity * 0.5f).coerceIn(0f, 1f), 0, 0, 0)
        letterSpacing = config.letterSpacing
        maskFilter = BlurMaskFilter(config.shadowBlur * result.width / 400f, BlurMaskFilter.Blur.NORMAL)
        typeface = if (config.fontWeight >= FontWeight.Bold) {
            android.graphics.Typeface.DEFAULT_BOLD
        } else {
            android.graphics.Typeface.DEFAULT
        }
    } else null

    // 构建水印文字行
    val lines = mutableListOf<String>()
    if (config.showBrand && config.brandText.isNotBlank()) lines.add(config.brandText)
    if (config.showModel && config.modelText.isNotBlank()) lines.add(config.modelText)
    if (config.showParams && config.paramsText.isNotBlank()) lines.add(config.paramsText)
    if (config.showDate && config.dateText.isNotBlank()) lines.add(config.dateText)
    if (config.showLocation && config.locationText.isNotBlank()) lines.add("📍 ${config.locationText}")
    if (config.showPhotographer && config.photographerText.isNotBlank()) lines.add("📸 ${config.photographerText}")

    if (lines.isEmpty()) return result

    val paddingPx = config.padding * result.width / 400f
    val lineHeight = paint.textSize * 1.4f

    // 计算起始位置
    val totalHeight = lineHeight * lines.size
    val startY = when (config.position) {
        WatermarkPlacement.TOP_LEFT, WatermarkPlacement.TOP_CENTER, WatermarkPlacement.TOP_RIGHT ->
            paddingPx + lineHeight
        WatermarkPlacement.CENTER ->
            (result.height - totalHeight) / 2f + lineHeight
        else ->
            result.height - paddingPx - totalHeight + lineHeight
    }

    val startX = when (config.position) {
        WatermarkPlacement.TOP_LEFT, WatermarkPlacement.BOTTOM_LEFT -> paddingPx
        WatermarkPlacement.TOP_CENTER, WatermarkPlacement.CENTER, WatermarkPlacement.BOTTOM_CENTER ->
            (result.width - paint.measureText(lines.maxByOrNull { it.length } ?: "")) / 2f
        else -> result.width - paddingPx
    }

    // 绘制暗角
    if (config.showVignette && config.vignetteStrength > 0) {
        val vignettePaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                result.width / 2f, result.height / 2f,
                maxOf(result.width, result.height) * 0.5f,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb((config.vignetteStrength * 180).toInt(), 0, 0, 0),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), vignettePaint)
    }

    // 绘制背景
    if (config.bgOpacity > 0) {
        val bgPaint = Paint().apply {
            color = android.graphics.Color.argb((config.bgOpacity * 255).toInt(), 0, 0, 0)
        }
        val bgRect = android.graphics.RectF(
            startX - paddingPx / 2,
            startY - lineHeight,
            startX + paint.measureText(lines.maxByOrNull { it.length } ?: "") + paddingPx / 2,
            startY + (lines.size - 1) * lineHeight + paddingPx / 2
        )
        canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)
    }

    // 绘制文字
    lines.forEachIndexed { index, line ->
        val y = startY + index * lineHeight
        val x = when (config.position) {
            WatermarkPlacement.TOP_RIGHT, WatermarkPlacement.BOTTOM_RIGHT ->
                result.width - paddingPx - paint.measureText(line)
            else -> startX
        }
        shadowPaint?.let { canvas.drawText(line, x + 1f, y + 1f, it) }
        canvas.drawText(line, x, y, paint)
    }

    return result
}

// ========== 数据类 ==========

data class WatermarkConfig(
    val enabled: Boolean = true,
    val showBrand: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showDate: Boolean = true,
    val showLocation: Boolean = false,
    val showPhotographer: Boolean = false,
    val showVignette: Boolean = false,
    val brandText: String = "OMaster",
    val modelText: String = "",
    val paramsText: String = "",
    val dateText: String = "",
    val locationText: String = "",
    val photographerText: String = "",
    val position: WatermarkPlacement = WatermarkPlacement.BOTTOM_LEFT,
    val textColor: Color = Color.White,
    val textSize: Float = 14f,
    val opacity: Float = 0.8f,
    val rotation: Float = 0f,
    val shadowEnabled: Boolean = true,
    val shadowBlur: Float = 4f,
    val padding: Float = 20f,
    val letterSpacing: Float = 0f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val bgOpacity: Float = 0f,
    val vignetteStrength: Float = 0.5f,
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f
)

data class ExifData(
    val model: String? = null,
    val params: String? = null,
    val date: String? = null
)

enum class WatermarkPlacement {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
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

    val paddingPx = config.padding.dp.toPx()
    val watermarkX = when (config.position) {
        WatermarkPlacement.TOP_LEFT, WatermarkPlacement.CENTER, WatermarkPlacement.BOTTOM_LEFT -> 
            imageRect.left + paddingPx + offset.x
        WatermarkPlacement.TOP_CENTER, WatermarkPlacement.BOTTOM_CENTER -> 
            imageRect.left + imageRect.width / 2 + offset.x
        WatermarkPlacement.TOP_RIGHT, WatermarkPlacement.BOTTOM_RIGHT -> 
            imageRect.right - paddingPx + offset.x
    }

    val watermarkY = when (config.position) {
        WatermarkPlacement.TOP_LEFT, WatermarkPlacement.TOP_CENTER, WatermarkPlacement.TOP_RIGHT -> 
            imageRect.top + paddingPx + offset.y
        WatermarkPlacement.CENTER -> 
            imageRect.top + imageRect.height / 2 + offset.y
        WatermarkPlacement.BOTTOM_LEFT, WatermarkPlacement.BOTTOM_CENTER, WatermarkPlacement.BOTTOM_RIGHT -> 
            imageRect.bottom - paddingPx + offset.y
    }

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
        currentY += config.textSize * 1.5f * scale
    }

    if (config.showLocation) {
        drawContext.canvas.nativeCanvas.drawText(
            config.locationText,
            watermarkX,
            currentY,
            textPaint
        )
    }
}