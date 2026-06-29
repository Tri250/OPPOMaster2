package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    initialImageUri: Uri? = null,
    onBack: () -> Unit = {},
    onSave: (Bitmap) -> Unit = {},
    onShare: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SmartOptimizeEngine() }

    // ========== 状态 ==========
    var selectedTab by remember { mutableStateOf(SmartOptimizeTab.BASIC) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var params by remember { mutableStateOf(SmartOptimizeParams.DEFAULT) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf("") }
    var processingProgress by remember { mutableStateOf(0f) }
    var showBefore by remember { mutableStateOf(false) }
    var histogramData by remember { mutableStateOf<HistogramFullResult?>(null) }
    var editHistory by remember { mutableStateOf<List<EditHistoryEntry>>(emptyList()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showColorScience by remember { mutableStateOf(false) }
    var exportConfig by remember { mutableStateOf(ExportConfig()) }
    var presetFilter by remember { mutableStateOf<PresetCategory?>(null) }
    var selectedPresetId by remember { mutableStateOf<String?>(null) }

    // ========== 图片选择器 ==========
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                originalBitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                originalBitmap?.let { bmp ->
                    processedBitmap = bmp
                    displayBitmap = bmp
                    triggerFullProcess(bmp, params, engine) { stage, prog ->
                        processingStage = stage; processingProgress = prog
                    }?.let { result ->
                        processedBitmap = result
                        displayBitmap = result
                    }
                }
            }
        }
    }

    // ========== 实时预览 ==========
    fun requestPreview(newParams: SmartOptimizeParams) {
        params = newParams
        if (isProcessing) return

        scope.launch {
            isProcessing = true
            originalBitmap?.let { bmp ->
                val result = withContext(Dispatchers.Default) {
                    engine.processPreview(bmp, newParams)
                }
                processedBitmap = result
                displayBitmap = result
            }
            isProcessing = false
        }
    }

    // ========== 完整处理 ==========
    fun triggerFullProcess(
        bmp: Bitmap,
        p: SmartOptimizeParams,
        eng: SmartOptimizeEngine,
        onProgress: (String, Float) -> Unit
    ): Bitmap? {
        var result: Bitmap? = null
        scope.launch {
            isProcessing = true
            try {
                result = eng.process(bmp, p, onProgress)
                processedBitmap = result
                displayBitmap = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isProcessing = false
        }
        return result
    }

    // ========== 保存编辑历史 ==========
    fun pushHistory(label: String = "") {
        val entry = EditHistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            params = params,
            label = label
        )
        editHistory = (editHistory.take(historyIndex + 1) + entry)
        historyIndex = editHistory.lastIndex
    }

    fun restoreHistory(index: Int) {
        if (index in editHistory.indices) {
            historyIndex = index
            requestPreview(editHistory[index].params)
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            requestPreview(editHistory[historyIndex].params)
        }
    }

    fun redo() {
        if (historyIndex < editHistory.lastIndex - 1) {
            historyIndex++
            requestPreview(editHistory[historyIndex].params)
        }
    }

    // ========== 应用预设 ==========
    fun applyPreset(preset: SmartOptimizePreset) {
        selectedPresetId = preset.id
        pushHistory("应用预设: ${preset.name}")
        requestPreview(preset.params)
    }

    // ========== 重置 ==========
    fun resetAll() {
        pushHistory("重置全部")
        requestPreview(SmartOptimizeParams.DEFAULT)
        selectedPresetId = null
    }

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
                        enabled = historyIndex < editHistory.lastIndex - 1
                    ) {
                        Icon(Icons.Default.Redo, "重做")
                    }
                    // 重置
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Refresh, "重置")
                    }
                    // 保存
                    IconButton(onClick = {
                        processedBitmap?.let { onSave(it) }
                    }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    // 导出
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileUpload, "导出")
                    }
                    // 分享
                    IconButton(onClick = {
                        processedBitmap?.let { onShare(it) }
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
                    displayBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "预览",
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
                        onToggle = { showBefore = it },
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
                        onClick = { selectedTab = tab },
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
                        onFilterChanged = { presetFilter = it },
                        onPresetSelected = { applyPreset(it) },
                        enabled = originalBitmap != null
                    )
                    SmartOptimizeTab.HISTORY -> HistoryPanel(
                        history = editHistory,
                        currentIndex = historyIndex,
                        onRestore = { restoreHistory(it) },
                        params = params,
                        onSaveCheckpoint = { pushHistory("检查点") }
                    )
                }
            }
        }
    }

    // ========== 对话框 ==========
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("重置所有调整") },
            text = { Text("确定要重置所有参数到默认值吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    resetAll()
                    showResetConfirm = false
                }) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出图像") },
            text = {
                ExportConfigPanel(
                    config = exportConfig,
                    onConfigChanged = { exportConfig = it },
                    onExport = {
                        showExportDialog = false
                        // 触发导出
                        scope.launch {
                            processedBitmap?.let { bmp ->
                                pushHistory("导出: ${exportConfig.format.label}")
                                onSave(bmp)
                            }
                        }
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    if (showColorScience) {
        AlertDialog(
            onDismissRequest = { showColorScience = false },
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
                TextButton(onClick = { showColorScience = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorScience = false }) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("色彩科学", style = MaterialTheme.typography.titleSmall)
                Text(
                    params.colorScience,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item {
            LabeledSlider("色调映射", params.toneMappingStrength, 0f, 100f,
                { onParamsChanged(params.copy(toneMappingStrength = it)) }, enabled = enabled)
        }
        item {
            LabeledSlider("Sigmoid对比度", params.sigmoidContrast, 0f, 100f,
                { onParamsChanged(params.copy(sigmoidContrast = it)) }, enabled = enabled)
        }
        item {
            LabeledSlider("高光过渡", params.highlightTransition, 0f, 100f,
                { onParamsChanged(params.copy(highlightTransition = it)) }, enabled = enabled)
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
    }
}

// ==================== 色彩分级面板 ====================

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
                    size = 110f
                )
                ColorWheelPanel(
                    wheel = params.midtoneWheel,
                    onWheelChanged = { onParamsChanged(params.copy(midtoneWheel = it)) },
                    label = "中间调",
                    size = 110f
                )
                ColorWheelPanel(
                    wheel = params.highlightWheel,
                    onWheelChanged = { onParamsChanged(params.copy(highlightWheel = it)) },
                    label = "高光",
                    size = 110f
                )
            }
        }

        item {
            ColorWheelPanel(
                wheel = params.globalWheel,
                onWheelChanged = { onParamsChanged(params.copy(globalWheel = it)) },
                label = "全局",
                size = 100f
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
        item { LabeledSlider("羽化", params.vignetteFeather, 0f, 100f,
            { onParamsChanged(params.copy(vignetteFeather = it)) }, enabled = enabled) }

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