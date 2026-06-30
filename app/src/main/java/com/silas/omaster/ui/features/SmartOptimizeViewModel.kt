package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 智能优化模块 ViewModel
 *
 * 职责：
 * 1. 持有并管理所有智能优化状态（原图、预览图、参数、历史、导出配置等）
 * 2. 调度 SmartOptimizeEngine 进行图像处理
 * 3. 提供撤销/重做/预设/导出等完整交互链路
 * 4. 处理参数变化防抖，避免快速拖动滑块导致大量重算
 */
@OptIn(FlowPreview::class)
class SmartOptimizeViewModel : ViewModel() {

    private val engine = SmartOptimizeEngine(null)

    private val _uiState = MutableStateFlow(SmartOptimizeUiState())
    val uiState: StateFlow<SmartOptimizeUiState> = _uiState.asStateFlow()

    /**
     * 参数变化请求流，用于防抖处理。
     * 拖动滑块时可能连续发射多次，debounce 后统一渲染。
     */
    private val paramChangeRequests = MutableSharedFlow<SmartOptimizeParams>(
        extraBufferCapacity = 1
    )

    // 参数稳定后自动记录历史（用户停止拖动约 800ms 后提交）
    private var historyCommitJob: Job? = null

    init {
        paramChangeRequests
            .debounce(80L)
            .distinctUntilChanged()
            .onEach { params ->
                processWithParams(params)
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        engine.cancel()
    }

    // ========== 图片加载 ==========

    fun loadImage(context: Context, uri: Uri) {
        historyCommitJob?.cancel()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, processingStage = "正在加载图片...")

            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            } ?: run {
                _uiState.value = _uiState.value.copy(isProcessing = false, processingStage = "")
                return@launch
            }

            engine.initialize(bitmap)
            val initialParams = SmartOptimizeParams.DEFAULT

            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                processedBitmap = bitmap,
                displayBitmap = bitmap,
                params = initialParams,
                histogramData = computeHistogram(bitmap),
                editHistory = emptyList(),
                historyIndex = -1,
                isProcessing = true,
                processingStage = "正在初始化预览..."
            )

            processWithParams(initialParams)
        }
    }

    fun loadImage(bitmap: Bitmap) {
        historyCommitJob?.cancel()
        viewModelScope.launch {
            engine.initialize(bitmap)
            val initialParams = SmartOptimizeParams.DEFAULT

            _uiState.value = _uiState.value.copy(
                originalBitmap = bitmap,
                processedBitmap = bitmap,
                displayBitmap = bitmap,
                params = initialParams,
                histogramData = computeHistogram(bitmap),
                editHistory = emptyList(),
                historyIndex = -1,
                isProcessing = true,
                processingStage = "正在初始化预览..."
            )

            processWithParams(initialParams)
        }
    }

    // ========== 参数更新 ==========

    /**
     * 更新参数并请求预览。
     * 通过 SharedFlow 防抖，连续调用时只会触发一次实际处理。
     *
     * @param params 新参数
     * @param recordHistory 是否记录历史。UI 拖动结束时设为 true，实时预览设为 false。
     */
    fun requestPreview(params: SmartOptimizeParams, recordHistory: Boolean = false) {
        if (recordHistory) {
            pushHistory()
        } else {
            // 自动防抖记录历史：用户停止操作 800ms 后将最终参数提交为历史节点
            scheduleHistoryCommit()
        }
        _uiState.value = _uiState.value.copy(params = params)
        paramChangeRequests.tryEmit(params)
    }

    private fun scheduleHistoryCommit() {
        historyCommitJob?.cancel()
        historyCommitJob = viewModelScope.launch {
            delay(800L)
            pushHistory()
        }
    }

    /**
     * 同步更新参数并立即处理（用于预设应用、撤销重做等需要立即生效的场景）
     */
    fun applyParamsImmediately(params: SmartOptimizeParams, recordHistory: Boolean = true) {
        historyCommitJob?.cancel()
        if (recordHistory) {
            pushHistory()
        }
        _uiState.value = _uiState.value.copy(params = params)
        viewModelScope.launch {
            processWithParams(params)
        }
    }

    // ========== 历史管理 ==========

    private fun pushHistory(label: String = "") {
        val current = _uiState.value
        val entry = EditHistoryEntry(
            id = UUID.randomUUID().toString(),
            params = current.params.copy(),
            timestamp = System.currentTimeMillis(),
            label = label
        )

        val trimmedHistory = current.editHistory.take(current.historyIndex + 1)
        val newHistory = trimmedHistory + entry

        _uiState.value = current.copy(
            editHistory = newHistory,
            historyIndex = newHistory.lastIndex
        )
    }

    fun undo() {
        val current = _uiState.value
        if (current.historyIndex > 0) {
            val newIndex = current.historyIndex - 1
            val params = current.editHistory[newIndex].params
            _uiState.value = current.copy(historyIndex = newIndex)
            applyParamsImmediately(params, recordHistory = false)
        }
    }

    fun redo() {
        val current = _uiState.value
        if (current.historyIndex < current.editHistory.lastIndex) {
            val newIndex = current.historyIndex + 1
            val params = current.editHistory[newIndex].params
            _uiState.value = current.copy(historyIndex = newIndex)
            applyParamsImmediately(params, recordHistory = false)
        }
    }

    fun saveCheckpoint(label: String = "检查点") {
        pushHistory(label)
    }

    // ========== 预设与重置 ==========

    fun applyPreset(preset: SmartOptimizePreset) {
        pushHistory("应用预设: ${preset.name}")
        _uiState.value = _uiState.value.copy(selectedPresetId = preset.id)
        applyParamsImmediately(preset.params.copy(), recordHistory = false)
    }

    fun resetAll() {
        pushHistory("重置全部")
        _uiState.value = _uiState.value.copy(selectedPresetId = null)
        applyParamsImmediately(SmartOptimizeParams.DEFAULT.copy(), recordHistory = false)
    }

    // ========== 处理核心 ==========

    private suspend fun processWithParams(params: SmartOptimizeParams) {
        val original = _uiState.value.originalBitmap ?: return

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            processingStage = "正在应用智能优化...",
            processingProgress = 0f
        )

        val result = withContext(Dispatchers.Default) {
            runCatching {
                engine.optimize(original, params)
            }.getOrElse { original }
        }

        val showBefore = _uiState.value.showBefore
        _uiState.value = _uiState.value.copy(
            processedBitmap = result,
            displayBitmap = if (showBefore) original else result,
            histogramData = computeHistogram(result),
            isProcessing = false,
            processingStage = "",
            processingProgress = 1f
        )
    }

    /**
     * 完整高质量处理（导出时使用）
     */
    suspend fun processForExport(params: SmartOptimizeParams): Bitmap? {
        val original = _uiState.value.originalBitmap ?: return null
        return withContext(Dispatchers.Default) {
            runCatching {
                engine.triggerFullProcess(original, params)
            }.getOrNull()
        }
    }

    // ========== 直方图 ==========

    private fun computeHistogram(bitmap: Bitmap): HistogramFullResult {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val luma = IntArray(256)
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        for (pixel in pixels) {
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            luma[lum]++
            red[r]++
            green[g]++
            blue[b]++
        }

        var totalLum = 0f
        for (i in luma.indices) totalLum += i * luma[i]
        val meanLum = if (pixels.isNotEmpty()) totalLum / pixels.size else 0f

        return HistogramFullResult(
            luma = luma,
            red = red,
            green = green,
            blue = blue,
            meanLuminance = meanLum
        )
    }

    // ========== UI 状态便捷方法 ==========

    fun setSelectedTab(tab: SmartOptimizeTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setShowBefore(showBefore: Boolean) {
        val current = _uiState.value
        _uiState.value = current.copy(
            showBefore = showBefore,
            displayBitmap = if (showBefore) current.originalBitmap else current.processedBitmap
        )
    }

    fun setPresetFilter(category: PresetCategory?) {
        _uiState.value = _uiState.value.copy(presetFilter = category)
    }

    fun setExportConfig(config: ExportConfig) {
        _uiState.value = _uiState.value.copy(exportConfig = config)
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = show)
    }

    fun setShowResetConfirm(show: Boolean) {
        _uiState.value = _uiState.value.copy(showResetConfirm = show)
    }

    fun setShowColorScience(show: Boolean) {
        _uiState.value = _uiState.value.copy(showColorScience = show)
    }

    /**
     * 根据当前导出配置执行导出，返回处理后的 Bitmap。
     */
    suspend fun exportCurrentImage(): Bitmap? {
        val current = _uiState.value
        val params = current.params.copy(
            exportFormat = current.exportConfig.format,
            exportQuality = current.exportConfig.quality,
            exportBitDepth = current.exportConfig.bitDepth,
            exportColorSpace = current.exportConfig.colorSpace,
            exportMetadata = current.exportConfig.metadata
        )
        return processForExport(params)
    }
}

/**
 * 智能优化 UI 状态单一真相源
 */
data class SmartOptimizeUiState(
    val selectedTab: SmartOptimizeTab = SmartOptimizeTab.BASIC,
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val displayBitmap: Bitmap? = null,
    val params: SmartOptimizeParams = SmartOptimizeParams.DEFAULT,
    val isProcessing: Boolean = false,
    val processingStage: String = "",
    val processingProgress: Float = 0f,
    val showBefore: Boolean = false,
    val histogramData: HistogramFullResult? = null,
    val editHistory: List<EditHistoryEntry> = emptyList(),
    val historyIndex: Int = -1,
    val exportConfig: ExportConfig = ExportConfig(),
    val presetFilter: PresetCategory? = null,
    val selectedPresetId: String? = null,
    val showExportDialog: Boolean = false,
    val showResetConfirm: Boolean = false,
    val showColorScience: Boolean = false
) {
    val canUndo: Boolean get() = historyIndex > 0
    val canRedo: Boolean get() = historyIndex < editHistory.lastIndex
    val changedParamCount: Int get() = params.changedParamCount()
}
