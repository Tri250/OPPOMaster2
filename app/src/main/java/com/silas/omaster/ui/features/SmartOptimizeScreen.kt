package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.data.repository.LUTResourceRepository
import kotlinx.coroutines.launch

/**
 * 智能优化主界面 — 完整移植 AlcedoStudio + RapidRAW 全部功能
 *
 * 功能模块（12个Tab）：
 * - BASIC: 基础调整（曝光/亮度/对比度/饱和度/鲜艳度）
 * - LIGHT: 光影调整（高光/阴影/白/黑/去霾）
 * - COLOR: 色彩调整（色温/色调/HSL八通道）
 * - CURVE: 色调曲线（参数曲线/点曲线/RGB通道）
 * - GRADING: 色彩分级（阴影/中间调/高光/全局色轮）
 * - DETAIL: 细节处理（锐化/降噪/纹理/清晰度）
 * - EFFECTS: 效果处理（颗粒/暗角/褪色）
 * - OPTICS: 光学校正（畸变/色差/透视/旋转/裁剪）
 * - CALIBRATION: 相机校准（阴影色调/原色校准）
 * - LUT: 3D LUT滤镜
 * - PRESETS: 预设库（100+胶片模拟/场景/AI推荐）
 * - HISTORY: 编辑历史（Git-like分支、撤销/重做）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOptimizeScreen(
    viewModel: SmartOptimizeViewModel = viewModel(),
    initialImageUri: Uri? = null,
    onBack: () -> Unit = {},
    onSave: (Bitmap) -> Unit = {},
    onShare: (Bitmap) -> Unit = {},
    onApply: (SmartOptimizeParams) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 注入 LUTManager 到 ViewModel，使 Engine 可使用 LUT 缓存链路
    LaunchedEffect(Unit) {
        viewModel.initLUTManager(context)
    }

    val uiState by viewModel.uiState.collectAsState()

    val params = uiState.params
    val originalBitmap = uiState.originalBitmap
    val processedBitmap = uiState.processedBitmap
    val displayBitmap = uiState.displayBitmap
    val isProcessing = uiState.isProcessing
    val processingStage = uiState.processingStage
    val processingProgress = uiState.processingProgress
    val histogramData = uiState.histogramData
    val editHistory = uiState.editHistory
    val historyIndex = uiState.historyIndex
    val exportConfig = uiState.exportConfig
    val presetFilter = uiState.presetFilter
    val selectedPresetId = uiState.selectedPresetId
    val selectedTab = uiState.selectedTab
    val showBefore = uiState.showBefore
    val showExportDialog = uiState.showExportDialog
    val showResetConfirm = uiState.showResetConfirm
    val showColorScience = uiState.showColorScience

    // ========== 图片选择器 ==========
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.loadImage(context, it) }
    }

    // 初始 URI 加载
    LaunchedEffect(initialImageUri) {
        initialImageUri?.let { viewModel.loadImage(context, it) }
    }

    // ========== 实时预览请求封装 ==========
    fun requestPreview(newParams: SmartOptimizeParams, recordHistory: Boolean = false) {
        viewModel.requestPreview(newParams, recordHistory)
    }

    // ========== 历史操作 ==========
    fun restoreHistory(index: Int) {
        if (index in editHistory.indices) {
            viewModel.applyParamsImmediately(editHistory[index].params.copy(), recordHistory = false)
        }
    }

    fun undo() = viewModel.undo()
    fun redo() = viewModel.redo()

    // ========== 应用预设 ==========
    fun applyPreset(preset: SmartOptimizePreset) = viewModel.applyPreset(preset)

    // ========== 重置 ==========
    fun resetAll() = viewModel.resetAll()

    // ========== UI ==========
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能优化", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 撤销/重做
                    IconButton(
                        onClick = { undo() },
                        enabled = historyIndex > 0
                    ) {
                        Icon(Icons.Default.Undo, "撤销")
                    }
                    IconButton(
                        onClick = { redo() },
                        enabled = historyIndex < editHistory.lastIndex
                    ) {
                        Icon(Icons.Default.Redo, "重做")
                    }
                    // 重置
                    IconButton(onClick = { viewModel.setShowResetConfirm(true) }) {
                        Icon(Icons.Default.Refresh, "重置")
                    }
                    // 应用参数到相机
                    IconButton(onClick = { onApply(params) }) {
                        Icon(Icons.Default.Check, "应用")
                    }
                    // 保存
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.exportCurrentImage()?.let { onSave(it) }
                        }
                    }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    // 导出
                    IconButton(onClick = { viewModel.setShowExportDialog(true) }) {
                        Icon(Icons.Default.FileUpload, "导出")
                    }
                    // 分享
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.exportCurrentImage()?.let { onShare(it) }
                        }
                    }) {
                        Icon(Icons.Default.Share, "分享")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ========== 图片预览区域 ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color(0xFF111122)),
                contentAlignment = Alignment.Center
            ) {
                if (originalBitmap == null) {
                    // 空白状态 - 引导选择图片
                    ImportPlaceholder(
                        onImport = { imagePicker.launch("image/*") }
                    )
                } else {
                    val currentDisplayBitmap = if (showBefore) originalBitmap else processedBitmap
                    currentDisplayBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = if (showBefore) "原图" else "预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 处理中遮罩
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(processingStage, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // 处理进度
                    if (processingProgress > 0f && processingProgress < 1f) {
                        ProcessingProgressBar(
                            stage = processingStage,
                            progress = processingProgress,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // 参数变更计数
                    val changedCount = params.changedParamCount()
                    if (changedCount > 0 && !isProcessing) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        ) {
                            Text(
                                "$changedCount 参数已调整",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    // 前后比较切换
                    BeforeAfterToggle(
                        showBefore = showBefore,
                        onToggle = { viewModel.setShowBefore(it) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )

                    // 直方图叠加
                    if (params.showHistogram && histogramData != null) {
                        HistogramView(
                            histogram = histogramData,
                            mode = params.histogramMode,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(0.4f)
                                .padding(8.dp)
                        )
                    }
                }
            }

            // ========== Tab 栏 ==========
            ScrollableTabRow(
                selectedTabIndex = SmartOptimizeTab.entries.indexOf(selectedTab),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 8.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SmartOptimizeTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(tab) },
                        modifier = Modifier.height(44.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            // ========== Tab 内容面板 ==========
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                when (selectedTab) {
                    SmartOptimizeTab.BASIC -> BasicAdjustPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.LIGHT -> LightAdjustPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.COLOR -> ColorAdjustPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.CURVE -> CurvePanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.GRADING -> GradingPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.DETAIL -> DetailPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.EFFECTS -> EffectsPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.OPTICS -> OpticsPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.CALIBRATION -> CalibrationPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.LUT -> LUTPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.PRESETS -> PresetsPanel(
                        params = params,
                        selectedPresetId = selectedPresetId,
                        filterCategory = presetFilter,
                        onFilterChanged = { viewModel.setPresetFilter(it) },
                        onPresetSelected = { applyPreset(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.MASK -> MaskPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.HISTORY -> HistoryPanel(
                        history = editHistory,
                        currentIndex = historyIndex,
                        onRestore = { restoreHistory(it) },
                        params = params,
                        onSaveCheckpoint = { viewModel.saveCheckpoint() }
                    )
                    SmartOptimizeTab.EXPORT -> ExportTabPanel(
                        params = params,
                        onParamsChanged = { requestPreview(it) },
                        config = exportConfig,
                        onConfigChanged = { viewModel.setExportConfig(it) },
                        onExport = {
                            viewModel.setShowExportDialog(false)
                            scope.launch {
                                viewModel.exportCurrentImage()?.let { onSave(it) }
                            }
                        },
                        enabled = originalBitmap != null
                    )
                }
            }
        }
    }

    // ========== 对话框 ==========
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowResetConfirm(false) },
            title = { Text("重置所有调整") },
            text = { Text("确定要重置所有参数到默认值吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    resetAll()
                    viewModel.setShowResetConfirm(false)
                }) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowResetConfirm(false) }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowExportDialog(false) },
            title = { Text("导出图像") },
            text = {
                ExportConfigPanel(
                    config = exportConfig,
                    onConfigChanged = { viewModel.setExportConfig(it) },
                    onExport = {
                        viewModel.setShowExportDialog(false)
                        // 触发导出：使用 exportCurrentImage 应用导出配置
                        scope.launch {
                            viewModel.exportCurrentImage()?.let { bmp ->
                                viewModel.saveCheckpoint("导出: ${exportConfig.format.uppercase()}")
                                onSave(bmp)
                            }
                        }
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.setShowExportDialog(false) }) {
                    Text("关闭")
                }
            }
        )
    }

    if (showColorScience) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowColorScience(false) },
            title = { Text("色彩科学设置") },
            text = {
                ColorScienceSelector(
                    currentMode = params.colorScience,
                    displayColorSpace = params.displayColorSpace,
                    eotf = params.eotf,
                    peakLuminance = params.peakLuminance,
                    onColorScienceChanged = {
                        requestPreview(params.copy(colorScience = it))
                    },
                    onDisplayColorSpaceChanged = {
                        requestPreview(params.copy(displayColorSpace = it))
                    },
                    onEOTFChanged = {
                        requestPreview(params.copy(eotf = it))
                    },
                    onPeakLuminanceChanged = {
                        requestPreview(params.copy(peakLuminance = it))
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowColorScience(false) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowColorScience(false) }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 导入占位图 ====================

@Composable
private fun ImportPlaceholder(onImport: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF444466)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "选择图片开始智能优化",
            color = Color(0xFF666688),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "支持 JPEG / PNG / WebP / HEIC",
            color = Color(0xFF444466),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onImport) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("选择图片")
        }
    }
}

// ==================== 基础调整面板 ====================

@Composable
private fun BasicAdjustPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("基础调整", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("曝光", params.exposure, -5f, 5f,
            { onParamsChanged(params.copy(exposure = it)) },
            { "%.2f EV".format(it) }, enabled = enabled) }
        item { LabeledSlider("亮度", params.brightness, -100f, 100f,
            { onParamsChanged(params.copy(brightness = it)) },
            enabled = enabled) }
        item { LabeledSlider("对比度", params.contrast, -100f, 100f,
            { onParamsChanged(params.copy(contrast = it)) },
            enabled = enabled) }
        item { LabeledSlider("饱和度", params.saturation, -100f, 100f,
            { onParamsChanged(params.copy(saturation = it)) },
            enabled = enabled) }
        item { LabeledSlider("鲜艳度", params.vibrance, -100f, 100f,
            { onParamsChanged(params.copy(vibrance = it)) },
            enabled = enabled) }

        // 色调映射器 (RapidRAW: basic / agx)
        item {
            Spacer(Modifier.height(8.dp))
            Text("色调映射", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("basic" to "基础", "agx" to "AgX").forEach { (id, label) ->
                    FilterChip(
                        selected = params.toneMapper == id,
                        onClick = { onParamsChanged(params.copy(toneMapper = id)) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }
        item { LabeledSlider("EV偏移", params.evShift, -5f, 5f,
            { onParamsChanged(params.copy(evShift = it)) },
            { "%.2f EV".format(it) }, enabled = enabled) }
        item { LabeledSlider("色调映射强度", params.toneMappingStrength, 0f, 100f,
            { onParamsChanged(params.copy(toneMappingStrength = it)) }, enabled = enabled) }
        item { LabeledSlider("Sigmoid对比度", params.sigmoidContrast, 0f, 100f,
            { onParamsChanged(params.copy(sigmoidContrast = it)) }, enabled = enabled) }
        item { LabeledSlider("高光过渡", params.highlightTransition, 0f, 100f,
            { onParamsChanged(params.copy(highlightTransition = it)) }, enabled = enabled) }
    }
}

// ==================== 光影调整面板 ====================

@Composable
private fun LightAdjustPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("光影调整", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("光效", params.light, -100f, 100f,
            { onParamsChanged(params.copy(light = it)) }, enabled = enabled) }
        item { LabeledSlider("高光保留", params.highlightPreserve, 0f, 100f,
            { onParamsChanged(params.copy(highlightPreserve = it)) }, enabled = enabled) }
        item { LabeledSlider("阴影恢复", params.shadowRecover, 0f, 100f,
            { onParamsChanged(params.copy(shadowRecover = it)) }, enabled = enabled) }
        item { LabeledSlider("高光", params.highlights, -100f, 100f,
            { onParamsChanged(params.copy(highlights = it)) }, enabled = enabled) }
        item { LabeledSlider("阴影", params.shadows, -100f, 100f,
            { onParamsChanged(params.copy(shadows = it)) }, enabled = enabled) }
        item { LabeledSlider("白色色阶", params.whites, -100f, 100f,
            { onParamsChanged(params.copy(whites = it)) }, enabled = enabled) }
        item { LabeledSlider("黑色色阶", params.blacks, -100f, 100f,
            { onParamsChanged(params.copy(blacks = it)) }, enabled = enabled) }
        item { LabeledSlider("去霾", params.dehaze, 0f, 100f,
            { onParamsChanged(params.copy(dehaze = it)) }, enabled = enabled) }
    }
}

// ==================== 色彩调整面板 ====================

@Composable
private fun ColorAdjustPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("色彩调整", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("色温", params.temperature, 2000f, 50000f,
            { onParamsChanged(params.copy(temperature = it)) },
            { "${it.toInt()}K" }, enabled = enabled) }
        item { LabeledSlider("色调", params.tint, -100f, 100f,
            { onParamsChanged(params.copy(tint = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("HSL 调色", style = MaterialTheme.typography.titleSmall)
        }
        item {
            HSLPanel(
                hsl = params.hslAdjustments,
                onHSLChanged = { hsl ->
                    onParamsChanged(params.copy(hslAdjustments = hsl))
                }
            )
        }

        // 色彩科学快捷入口
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { viewModel.setShowColorScience(true) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("色彩科学", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        params.colorScience,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==================== 曲线面板 ====================

@Composable
private fun CurvePanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // 参数曲线
            ParametricCurvePanel(
                curve = params.parametricCurve,
                onCurveChanged = { onParamsChanged(params.copy(parametricCurve = it)) }
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            // 点曲线
            ToneCurveEditor(
                points = params.pointCurve,
                onPointsChanged = { onParamsChanged(params.copy(pointCurve = it)) },
                channelLabel = "RGB",
                channelColor = Color.White
            )
        }

        item {
            // RGB 通道曲线
            var channelIndex by remember { mutableIntStateOf(0) }
            val channels = listOf("R", "G", "B")
            val colors = listOf(Color.Red, Color.Green, Color.Blue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                channels.forEachIndexed { idx, name ->
                    FilterChip(
                        selected = idx == channelIndex,
                        onClick = { channelIndex = idx },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors[idx].copy(alpha = 0.3f)
                        )
                    )
                }
            }

            when (channelIndex) {
                0 -> ToneCurveEditor(
                    points = params.redCurve,
                    onPointsChanged = { onParamsChanged(params.copy(redCurve = it)) },
                    channelLabel = "R 通道",
                    channelColor = Color.Red
                )
                1 -> ToneCurveEditor(
                    points = params.greenCurve,
                    onPointsChanged = { onParamsChanged(params.copy(greenCurve = it)) },
                    channelLabel = "G 通道",
                    channelColor = Color.Green
                )
                2 -> ToneCurveEditor(
                    points = params.blueCurve,
                    onPointsChanged = { onParamsChanged(params.copy(blueCurve = it)) },
                    channelLabel = "B 通道",
                    channelColor = Color.Blue
                )
            }
        }

        // Hue vs Sat / Hue vs Lum / Lum vs Sat (RapidRAW)
        item {
            Spacer(Modifier.height(8.dp))
            Text("色相曲线", style = MaterialTheme.typography.titleSmall)
        }
        item {
            var hueCurveIndex by remember { mutableIntStateOf(0) }
            val hueCurveNames = listOf("H-S 色相/饱和度", "H-L 色相/亮度", "L-S 亮度/饱和度")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hueCurveNames.forEachIndexed { idx, name ->
                    FilterChip(
                        selected = idx == hueCurveIndex,
                        onClick = { hueCurveIndex = idx },
                        label = { Text(name, fontSize = 10.sp) }
                    )
                }
            }

            when (hueCurveIndex) {
                0 -> ToneCurveEditor(
                    points = params.hueVsSatCurve,
                    onPointsChanged = { onParamsChanged(params.copy(hueVsSatCurve = it)) },
                    channelLabel = "色相 → 饱和度",
                    channelColor = Color(0xFFFF6600)
                )
                1 -> ToneCurveEditor(
                    points = params.hueVsLumCurve,
                    onPointsChanged = { onParamsChanged(params.copy(hueVsLumCurve = it)) },
                    channelLabel = "色相 → 亮度",
                    channelColor = Color(0xFFFFCC00)
                )
                2 -> ToneCurveEditor(
                    points = params.lumVsSatCurve,
                    onPointsChanged = { onParamsChanged(params.copy(lumVsSatCurve = it)) },
                    channelLabel = "亮度 → 饱和度",
                    channelColor = Color(0xFF00CC88)
                )
            }
        }
    }
}

@Composable
private fun GradingPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("色彩分级", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }

        // 色轮区域
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorWheelPanel(
                    wheel = params.shadowWheel,
                    onWheelChanged = { onParamsChanged(params.copy(shadowWheel = it)) },
                    label = "阴影",
                    wheelSize = 110f
                )
                ColorWheelPanel(
                    wheel = params.midtoneWheel,
                    onWheelChanged = { onParamsChanged(params.copy(midtoneWheel = it)) },
                    label = "中间调",
                    wheelSize = 110f
                )
                ColorWheelPanel(
                    wheel = params.highlightWheel,
                    onWheelChanged = { onParamsChanged(params.copy(highlightWheel = it)) },
                    label = "高光",
                    wheelSize = 110f
                )
            }
        }

        item {
            ColorWheelPanel(
                wheel = params.globalWheel,
                onWheelChanged = { onParamsChanged(params.copy(globalWheel = it)) },
                label = "全局",
                wheelSize = 100f
            )
        }

        item {
            LabeledSlider("混合", params.gradingBlend, 0f, 100f,
                { onParamsChanged(params.copy(gradingBlend = it)) }, enabled = enabled)
        }
        item {
            LabeledSlider("亮度平衡", params.gradingBalance, 0f, 100f,
                { onParamsChanged(params.copy(gradingBalance = it)) }, enabled = enabled)
        }
    }
}

// ==================== 细节面板 ====================

@Composable
private fun DetailPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("锐化", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("锐化", params.sharpness, 0f, 150f,
            { onParamsChanged(params.copy(sharpness = it)) }, enabled = enabled) }
        item { LabeledSlider("半径", params.sharpnessRadius, 0.5f, 3f,
            { onParamsChanged(params.copy(sharpnessRadius = it)) },
            { "%.1f".format(it) }, enabled = enabled) }
        item { LabeledSlider("细节", params.sharpnessDetail, 0f, 100f,
            { onParamsChanged(params.copy(sharpnessDetail = it)) }, enabled = enabled) }
        item { LabeledSlider("蒙版", params.sharpnessMasking, 0f, 100f,
            { onParamsChanged(params.copy(sharpnessMasking = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("降噪", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("亮度降噪", params.luminanceNoiseReduction, 0f, 100f,
            { onParamsChanged(params.copy(luminanceNoiseReduction = it)) }, enabled = enabled) }
        item { LabeledSlider("亮度降噪细节", params.noiseReductionDetail, 0f, 100f,
            { onParamsChanged(params.copy(noiseReductionDetail = it)) }, enabled = enabled) }
        item { LabeledSlider("色彩降噪", params.colorNoiseReduction, 0f, 100f,
            { onParamsChanged(params.copy(colorNoiseReduction = it)) }, enabled = enabled) }
        item { LabeledSlider("色彩降噪细节", params.colorNoiseReductionDetail, 0f, 100f,
            { onParamsChanged(params.copy(colorNoiseReductionDetail = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("增强", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("纹理", params.texture, -100f, 100f,
            { onParamsChanged(params.copy(texture = it)) }, enabled = enabled) }
        item { LabeledSlider("清晰度", params.clarity, -100f, 100f,
            { onParamsChanged(params.copy(clarity = it)) }, enabled = enabled) }
        item { LabeledSlider("结构", params.structure, -100f, 100f,
            { onParamsChanged(params.copy(structure = it)) }, enabled = enabled) }
        item { LabeledSlider("中心偏移", params.centre, -100f, 100f,
            { onParamsChanged(params.copy(centre = it)) }, enabled = enabled) }
    }
}

// ==================== 效果面板 ====================

@Composable
private fun EffectsPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("胶片颗粒", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("颗粒", params.grain, 0f, 100f,
            { onParamsChanged(params.copy(grain = it)) }, enabled = enabled) }
        item { LabeledSlider("大小", params.grainSize, 0f, 100f,
            { onParamsChanged(params.copy(grainSize = it)) }, enabled = enabled) }
        item { LabeledSlider("粗糙度", params.grainRoughness, 0f, 100f,
            { onParamsChanged(params.copy(grainRoughness = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("暗角", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("暗角", params.vignette, -100f, 100f,
            { onParamsChanged(params.copy(vignette = it)) }, enabled = enabled) }
        item { LabeledSlider("中点", params.vignetteMidpoint, 0f, 100f,
            { onParamsChanged(params.copy(vignetteMidpoint = it)) }, enabled = enabled) }
        item { LabeledSlider("圆度", params.vignetteRoundness, -100f, 100f,
            { onParamsChanged(params.copy(vignetteRoundness = it)) }, enabled = enabled) }
        item { LabeledSlider("羽化", params.vignetteFeather, 0f, 100f,
            { onParamsChanged(params.copy(vignetteFeather = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("创意光效", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("发光", params.glowAmount, 0f, 100f,
            { onParamsChanged(params.copy(glowAmount = it)) }, enabled = enabled) }
        item { LabeledSlider("光晕", params.halationAmount, 0f, 100f,
            { onParamsChanged(params.copy(halationAmount = it)) }, enabled = enabled) }
        item { LabeledSlider("光斑", params.flareAmount, 0f, 100f,
            { onParamsChanged(params.copy(flareAmount = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("胶片仿真", style = MaterialTheme.typography.titleSmall)
        }
        item {
            val filmPresets = listOf("none" to "无", "kodachrome" to "Kodachrome",
                "portra400" to "Portra 400", "ecktachrome" to "Ektachrome",
                "fujipro400h" to "Fuji Pro 400H", "agfaapx" to "Agfa APX",
                "ilfordhp5" to "Ilford HP5", "cinestill800t" to "CineStill 800T")
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = filmPresets.find { it.first == params.filmSimulation }?.second ?: "无",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("胶片风格") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filmPresets.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onParamsChanged(params.copy(filmSimulation = id))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        item { LabeledSlider("高光重建", if (params.highlightReconstruction) 100f else 0f, 0f, 100f,
            { onParamsChanged(params.copy(highlightReconstruction = it > 50f)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("其他", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("褪色", params.fade, 0f, 100f,
            { onParamsChanged(params.copy(fade = it)) }, enabled = enabled) }
    }
}

// ==================== 光学面板 ====================

@Composable
private fun OpticsPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("光学校正", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("畸变校正", params.distortion, -100f, 100f,
            { onParamsChanged(params.copy(distortion = it)) }, enabled = enabled) }
        item { LabeledSlider("色差(红/青)", params.chromaticAberrationR, -100f, 100f,
            { onParamsChanged(params.copy(chromaticAberrationR = it)) }, enabled = enabled) }
        item { LabeledSlider("色差(蓝/黄)", params.chromaticAberrationB, -100f, 100f,
            { onParamsChanged(params.copy(chromaticAberrationB = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("方向", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onParamsChanged(params.copy(orientationSteps = (params.orientationSteps + 1) % 4)) },
                    enabled = enabled) {
                    Icon(Icons.Default.RotateRight, "旋转90°", modifier = Modifier.size(20.dp))
                }
                Text("旋转 ${params.orientationSteps * 90}°", fontSize = 12.sp,
                    modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = params.flipHorizontal,
                        onCheckedChange = { onParamsChanged(params.copy(flipHorizontal = it)) },
                        enabled = enabled
                    )
                    Text("水平翻转", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = params.flipVertical,
                        onCheckedChange = { onParamsChanged(params.copy(flipVertical = it)) },
                        enabled = enabled
                    )
                    Text("垂直翻转", fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Text("透视校正", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("水平透视", params.perspectiveX, -100f, 100f,
            { onParamsChanged(params.copy(perspectiveX = it)) }, enabled = enabled) }
        item { LabeledSlider("垂直透视", params.perspectiveY, -100f, 100f,
            { onParamsChanged(params.copy(perspectiveY = it)) }, enabled = enabled) }
        item { LabeledSlider("旋转", params.rotation, -45f, 45f,
            { onParamsChanged(params.copy(rotation = it)) },
            { "%.1f°".format(it) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("镜头校正", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("几何畸变", params.geometryWarp, -100f, 100f,
            { onParamsChanged(params.copy(geometryWarp = it)) }, enabled = enabled) }
        item { LabeledSlider("校正强度", params.lensCorrectionStrength, 0f, 100f,
            { onParamsChanged(params.copy(lensCorrectionStrength = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("裁剪", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = params.cropLockAspect,
                    onCheckedChange = { onParamsChanged(params.copy(cropLockAspect = it)) }
                )
                Text("锁定比例", fontSize = 12.sp)
            }
        }
        item { LabeledSlider("裁剪上", params.cropTop, 0f, 0.5f,
            { onParamsChanged(params.copy(cropTop = it)) },
            { "%.0f%%".format(it * 100) }, enabled = enabled) }
        item { LabeledSlider("裁剪左", params.cropLeft, 0f, 0.5f,
            { onParamsChanged(params.copy(cropLeft = it)) },
            { "%.0f%%".format(it * 100) }, enabled = enabled) }
    }
}

// ==================== 校准面板 ====================

@Composable
private fun CalibrationPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Text("相机校准", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item { LabeledSlider("阴影色调", params.shadowTint, -100f, 100f,
            { onParamsChanged(params.copy(shadowTint = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(12.dp))
            Text("红色原色", style = MaterialTheme.typography.labelMedium)
        }
        item { LabeledSlider("色相", params.redPrimaryHue, -100f, 100f,
            { onParamsChanged(params.copy(redPrimaryHue = it)) }, enabled = enabled) }
        item { LabeledSlider("饱和度", params.redPrimarySaturation, -100f, 100f,
            { onParamsChanged(params.copy(redPrimarySaturation = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(8.dp))
            Text("绿色原色", style = MaterialTheme.typography.labelMedium)
        }
        item { LabeledSlider("色相", params.greenPrimaryHue, -100f, 100f,
            { onParamsChanged(params.copy(greenPrimaryHue = it)) }, enabled = enabled) }
        item { LabeledSlider("饱和度", params.greenPrimarySaturation, -100f, 100f,
            { onParamsChanged(params.copy(greenPrimarySaturation = it)) }, enabled = enabled) }

        item {
            Spacer(Modifier.height(8.dp))
            Text("蓝色原色", style = MaterialTheme.typography.labelMedium)
        }
        item { LabeledSlider("色相", params.bluePrimaryHue, -100f, 100f,
            { onParamsChanged(params.copy(bluePrimaryHue = it)) }, enabled = enabled) }
        item { LabeledSlider("饱和度", params.bluePrimarySaturation, -100f, 100f,
            { onParamsChanged(params.copy(bluePrimarySaturation = it)) }, enabled = enabled) }
    }
}

// ==================== LUT 面板 ====================

@Composable
private fun LUTPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    val context = LocalContext.current
    val lutManager = remember { LUTManager.getInstance(context) }
    val downloadedIds by lutManager.downloadedIds.collectAsState()
    val downloadedLuts = remember(downloadedIds) {
        LUTResourceRepository.RESOURCES.filter { downloadedIds.contains(it.id) }
    }

    val builtInLUTs = listOf(
        "Kodak Portra 400" to "kodak_portra",
        "Fuji Velvia 50" to "fuji_velvia",
        "Kodak 2383" to "cine_2383",
        "Arri Alexa" to "cine_arri",
        "Teal & Orange" to "cine_teal",
        "Bleach Bypass" to "cine_bleach",
        "Agfa Vista" to "agfa_vista",
        "Ilford HP5" to "ilford_hp5",
        "16mm Film" to "cine_16mm",
        "Vintage Fade" to "vintage_fade"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("3D LUT 滤镜", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        item {
            LabeledSlider("LUT 强度", params.lutIntensity, 0f, 100f,
                { onParamsChanged(params.copy(lutIntensity = it)) }, enabled = enabled)
        }
        item {
            Text("内置 LUT", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(builtInLUTs) { (name, id) ->
            val isActive = params.activeLutName == id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) {
                        onParamsChanged(params.copy(
                            activeLutName = if (isActive) "" else id,
                            lutPath = if (isActive) "" else "",
                            lutIntensity = if (isActive) 0f else 80f
                        ))
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    "portra" in id -> Color(0xFFFF6B35)
                                    "velvia" in id -> Color(0xFF4CAF50)
                                    "cine" in id -> Color(0xFF4A90D9)
                                    "ilford" in id -> Color(0xFF888888)
                                    else -> Color(0xFF607D8B)
                                }
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (isActive) {
                        Icon(Icons.Default.Check, "已选",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 已下载 LUT（来自 LUTShare / LUTManager）
        if (downloadedLuts.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("已下载 LUT", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(downloadedLuts) { resource ->
                val localFile = remember(resource.id) { lutManager.getLocalFile(resource) }
                val isActive = params.lutPath == localFile?.absolutePath
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled && localFile != null) {
                            if (localFile != null) {
                                onParamsChanged(params.copy(
                                    activeLutName = if (isActive) "" else resource.id,
                                    lutPath = if (isActive) "" else localFile.absolutePath,
                                    lutIntensity = if (isActive) 0f else 80f
                                ))
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(resource.name, fontSize = 13.sp)
                            Text(
                                localFile?.let { "已下载" } ?: "未找到本地文件",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isActive) {
                            Icon(Icons.Default.Check, "已选",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== 预设面板 ====================

@Composable
private fun PresetsPanel(
    params: SmartOptimizeParams,
    selectedPresetId: String?,
    filterCategory: PresetCategory?,
    onFilterChanged: (PresetCategory?) -> Unit,
    onPresetSelected: (SmartOptimizePreset) -> Unit,
    enabled: Boolean
) {
    val allPresets = remember { SmartOptimizePresets.allPresets() }
    val filteredPresets = remember(filterCategory) {
        if (filterCategory != null) allPresets.filter { it.category == filterCategory }
        else allPresets
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("预设库", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp))
        }

        // 分类筛选
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { onFilterChanged(null) },
                        label = { Text("全部", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                items(PresetCategory.entries) { cat ->
                    FilterChip(
                        selected = filterCategory == cat,
                        onClick = { onFilterChanged(cat) },
                        label = { Text(cat.label, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // 预设网格
        item {
            val rows = filteredPresets.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { preset ->
                            PresetCard(
                                preset = preset,
                                isSelected = preset.id == selectedPresetId,
                                onClick = { onPresetSelected(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // 填充空白
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ==================== 历史面板 ====================

@Composable
private fun HistoryPanel(
    history: List<EditHistoryEntry>,
    currentIndex: Int,
    onRestore: (Int) -> Unit,
    params: SmartOptimizeParams,
    onSaveCheckpoint: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("编辑历史 (${history.size})",
                    style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onSaveCheckpoint) {
                    Icon(Icons.Default.Bookmark, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存检查点", fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Text(
                    "暂无编辑历史。调整参数后会自动记录。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(history.reversed()) { entry ->
                val idx = history.indexOf(entry)
                EditHistoryItem(
                    entry = entry,
                    isCurrent = idx == currentIndex,
                    onRestore = { onRestore(idx) }
                )
            }
        }
    }
}

// ==================== 蒙版面板 (RapidRAW) ====================

@Composable
private fun MaskPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    enabled: Boolean
) {
    var showCreateMask by remember { mutableStateOf(false) }
    var selectedMaskType by remember { mutableStateOf("brush") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("局部调整", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { showCreateMask = true },
                    enabled = enabled
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加蒙版")
                }
            }
        }

        // 现有蒙版列表
        if (params.masks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Brush,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "添加蒙版以进行局部调整",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "支持画笔、线性、径向、主体和天空蒙版",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(params.masks.size) { index ->
                val mask = params.masks[index]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (mask.type) {
                                "brush" -> Icons.Default.Brush
                                "linear" -> Icons.Default.LinearScale
                                "radial" -> Icons.Default.RadioButtonChecked
                                "subject" -> Icons.Default.Person
                                "sky" -> Icons.Default.Cloud
                                else -> Icons.Default.Brush
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mask.name.ifEmpty { "蒙版 ${index + 1}" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                when (mask.type) {
                                    "brush" -> "画笔"
                                    "linear" -> "线性"
                                    "radial" -> "径向"
                                    "subject" -> "主体"
                                    "sky" -> "天空"
                                    else -> mask.type
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mask.enabled,
                            onCheckedChange = {
                                val newMasks = params.masks.toMutableList()
                                newMasks[index] = mask.copy(enabled = it)
                                onParamsChanged(params.copy(masks = newMasks))
                            },
                            modifier = Modifier.height(24.dp)
                        )
                        IconButton(onClick = {
                            val newMasks = params.masks.toMutableList()
                            newMasks.removeAt(index)
                            onParamsChanged(params.copy(masks = newMasks))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    // 蒙版参数
                    if (mask.enabled) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            LabeledSlider("羽化", mask.feather, 0f, 100f,
                                {
                                    val newMasks = params.masks.toMutableList()
                                    newMasks[index] = mask.copy(feather = it)
                                    onParamsChanged(params.copy(masks = newMasks))
                                }, enabled = enabled)
                            LabeledSlider("密度", mask.density, 0f, 100f,
                                {
                                    val newMasks = params.masks.toMutableList()
                                    newMasks[index] = mask.copy(density = it)
                                    onParamsChanged(params.copy(masks = newMasks))
                                }, enabled = enabled)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("反转", fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = mask.invert,
                                    onCheckedChange = {
                                        val newMasks = params.masks.toMutableList()
                                        newMasks[index] = mask.copy(invert = it)
                                        onParamsChanged(params.copy(masks = newMasks))
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 复制粘贴
        item {
            Spacer(Modifier.height(12.dp))
            Text("复制/粘贴设置", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { SettingsClipboard.copy(params) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("复制", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (SettingsClipboard.hasData()) {
                            onParamsChanged(SettingsClipboard.paste(params))
                        }
                    },
                    enabled = enabled && SettingsClipboard.hasData(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("粘贴", fontSize = 12.sp)
                }
            }
        }
    }

    // 创建蒙版对话框
    if (showCreateMask) {
        AlertDialog(
            onDismissRequest = { showCreateMask = false },
            title = { Text("添加蒙版") },
            text = {
                Column {
                    val maskTypes = listOf(
                        Triple("brush", "画笔", Icons.Default.Brush),
                        Triple("linear", "线性渐变", Icons.Default.LinearScale),
                        Triple("radial", "径向渐变", Icons.Default.RadioButtonChecked),
                        Triple("subject", "主体(AI)", Icons.Default.Person),
                        Triple("sky", "天空(AI)", Icons.Default.Cloud)
                    )
                    maskTypes.forEach { (id, label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (selectedMaskType == id) Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer
                                    ) else Modifier
                                )
                                .clickable { selectedMaskType = id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newMask = LocalMask(
                        type = selectedMaskType,
                        name = when (selectedMaskType) {
                            "brush" -> "画笔蒙版"
                            "linear" -> "线性蒙版"
                            "radial" -> "径向蒙版"
                            "subject" -> "主体蒙版"
                            "sky" -> "天空蒙版"
                            else -> "蒙版"
                        }
                    )
                    onParamsChanged(params.copy(masks = params.masks + newMask))
                    showCreateMask = false
                }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMask = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// ==================== 导出面板 (AlcedoStudio 风格) ====================

@Composable
private fun ExportTabPanel(
    params: SmartOptimizeParams,
    onParamsChanged: (SmartOptimizeParams) -> Unit,
    config: ExportConfig,
    onConfigChanged: (ExportConfig) -> Unit,
    onExport: () -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("导出设置", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp))
        }

        // 格式选择
        item { Text("格式", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("jpeg", "png", "tiff").forEach { fmt ->
                    FilterChip(
                        selected = config.format == fmt,
                        onClick = { onConfigChanged(config.copy(format = fmt)) },
                        label = { Text(fmt.uppercase(), fontSize = 12.sp) }
                    )
                }
            }
        }

        // 质量
        if (config.format == "jpeg") {
            item { LabeledSlider("质量", config.quality.toFloat(), 1f, 100f,
                { onConfigChanged(config.copy(quality = it.toInt())) }, enabled = enabled) }
        }

        // 色彩空间
        item { Text("色彩空间", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("sRGB", "Rec2020", "DCIP3", "ACEScg").forEach { cs ->
                    FilterChip(
                        selected = config.colorSpace == cs,
                        onClick = { onConfigChanged(config.copy(colorSpace = cs)) },
                        label = { Text(cs, fontSize = 10.sp) }
                    )
                }
            }
        }

        // 位深度
        item { Text("位深度", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8, 16).forEach { bd ->
                    FilterChip(
                        selected = config.bitDepth == bd,
                        onClick = { onConfigChanged(config.copy(bitDepth = bd)) },
                        label = { Text("${bd}bit", fontSize = 12.sp) }
                    )
                }
            }
        }

        // 调整大小
        item { Text("调整大小", style = MaterialTheme.typography.titleSmall) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = config.resize.enabled,
                    onCheckedChange = { onConfigChanged(config.copy(resize = config.resize.copy(enabled = it))) },
                    modifier = Modifier.height(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("启用调整大小", fontSize = 13.sp)
            }
        }
        if (config.resize.enabled) {
            item { LabeledSlider("长边", config.resize.longEdge.toFloat(), 100f, 10000f,
                { onConfigChanged(config.copy(resize = config.resize.copy(longEdge = it.toInt()))) },
                { "${it.toInt()}px" }, enabled = enabled) }
        }

        // 元数据
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = config.metadata,
                    onCheckedChange = { onConfigChanged(config.copy(metadata = it)) },
                    modifier = Modifier.height(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("保留元数据 (EXIF)", fontSize = 13.sp)
            }
        }

        // 预设强度 (RapidRAW)
        item {
            Spacer(Modifier.height(12.dp))
            Text("预设强度", style = MaterialTheme.typography.titleSmall)
        }
        item { LabeledSlider("强度", params.presetIntensity, 0f, 100f,
            { onParamsChanged(params.copy(presetIntensity = it)) }, enabled = enabled) }

        // 导出按钮
        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onExport,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("导出图像", fontSize = 16.sp)
            }
        }
    }
}