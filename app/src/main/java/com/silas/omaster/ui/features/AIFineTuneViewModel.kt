package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face2
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.engine.AIFineTuneManager
import com.silas.omaster.ai.AISuggestionResult
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.engine.LUT3DRenderer
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.engine.GPURenderManager
import com.silas.omaster.engine.RenderParameters
import com.silas.omaster.engine.RenderQuality
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * 推理阶段
 */
enum class InferenceStage {
    IDLE, ANALYZING, DETECTING_SUBJECT, ANALYZING_LIGHT,
    COMPUTING_PARAMS, APPLYING_AI, COMPLETED, ERROR
}

/**
 * HSL 调整值（8 通道：红/橙/黄/绿/青/蓝/紫/洋红）
 */
data class HSLValue(
    val id: String,
    val name: String,
    val color: Color,
    var hue: Int = 0,
    var saturation: Int = 0,
    var luminance: Int = 0
)

/**
 * 色彩风格预设
 */
data class ColorStylePreset(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val params: RenderParameters,
    val description: String
)

/**
 * 智能优化选项
 */
data class SmartOptimization(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String,
    val color: Color,
    val isPro: Boolean = false
)

/**
 * AI 微调 ViewModel
 * 统一管理：图片、参数、HSL、曲线、智能优化、历史记录、导出
 */
class AIFineTuneViewModel(
    private val aiManager: AIFineTuneManager
) : ViewModel() {

    // ==================== UI 状态 ====================
    private val _activeTab = MutableStateFlow("basic")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedStyleId = MutableStateFlow<String?>(null)
    val selectedStyleId: StateFlow<String?> = _selectedStyleId.asStateFlow()

    // AI 功能可用性状态
    private val _isAIModelAvailable = MutableStateFlow(true)
    val isAIModelAvailable: StateFlow<Boolean> = _isAIModelAvailable.asStateFlow()

    // AI 不可用提示
    private val _aiUnavailableMessage = MutableStateFlow<String?>(null)
    val aiUnavailableMessage: StateFlow<String?> = _aiUnavailableMessage.asStateFlow()

    private val _selectedOptimizations = MutableStateFlow<Set<String>>(emptySet())
    val selectedOptimizations: StateFlow<Set<String>> = _selectedOptimizations.asStateFlow()

    private val _lockedParams = MutableStateFlow<Set<String>>(emptySet())
    val lockedParams: StateFlow<Set<String>> = _lockedParams.asStateFlow()

    private val _showCompare = MutableStateFlow(false)
    val showCompare: StateFlow<Boolean> = _showCompare.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    // ==================== 图片状态 ====================
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isLoadingImage = MutableStateFlow(false)
    val isLoadingImage: StateFlow<Boolean> = _isLoadingImage.asStateFlow()

    private val _imageLoadError = MutableStateFlow<String?>(null)
    val imageLoadError: StateFlow<String?> = _imageLoadError.asStateFlow()

    // ==================== 参数状态 ====================
    private val _currentParams = MutableStateFlow(RenderParameters())
    val currentParams: StateFlow<RenderParameters> = _currentParams.asStateFlow()

    private val _hslValues = MutableStateFlow(getDefaultHSLValues())
    val hslValues: StateFlow<List<HSLValue>> = _hslValues.asStateFlow()

    private val _selectedHslId = MutableStateFlow("red")
    val selectedHslId: StateFlow<String> = _selectedHslId.asStateFlow()

    private val _curveChannel = MutableStateFlow("rgb")
    val curveChannel: StateFlow<String> = _curveChannel.asStateFlow()

    private val _curvePoints = MutableStateFlow<Map<String, List<CurvePoint>>>(
        mapOf(
            "rgb" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "red" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "green" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "blue" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        )
    )
    val curvePoints: StateFlow<Map<String, List<CurvePoint>>> = _curvePoints.asStateFlow()

    // ==================== AI 推理状态 ====================
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isOfflineResult = MutableStateFlow(false)
    val isOfflineResult: StateFlow<Boolean> = _isOfflineResult.asStateFlow()

    private val _inferenceStage = MutableStateFlow(InferenceStage.IDLE)
    val inferenceStage: StateFlow<InferenceStage> = _inferenceStage.asStateFlow()

    private val _inferenceProgress = MutableStateFlow(0f)
    val inferenceProgress: StateFlow<Float> = _inferenceProgress.asStateFlow()

    private val _inferenceMessage = MutableStateFlow("")
    val inferenceMessage: StateFlow<String> = _inferenceMessage.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // 保存错误状态
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    // ==================== 历史记录（撤销/重做） ====================
    private val history = ArrayDeque<RenderParameters>()
    private var historyIndex = -1
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // 是否有修改（相对于原始默认值）
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    // ==================== GPU 渲染器 ====================
    private var gpuRenderManager: GPURenderManager? = null
    private var gpuInitialized = false

    private var inferenceJob: kotlinx.coroutines.Job? = null

    init {
        pushHistory()
    }

    // ==================== 图片加载 ====================

    /**
     * 加载图片（带降采样与尺寸限制，防止 OOM）
     */
    fun loadImage(context: Context, uri: Uri, maxDimension: Int = 2048) {
        viewModelScope.launch {
            _isLoadingImage.value = true
            _imageLoadError.value = null
            try {
                val (bitmap, sampleSize) = decodeSampledBitmap(context, uri, maxDimension)
                if (bitmap != null) {
                    _selectedImageUri.value = uri
                    _sourceBitmap.value?.recycle()
                    _sourceBitmap.value = bitmap
                    _previewBitmap.value?.recycle()
                    _previewBitmap.value = null
                    renderPreviewAsync(context)
                    pushHistory(force = true)
                } else {
                    _imageLoadError.value = "无法解码图片"
                }
            } catch (e: Exception) {
                Log.e("AIFineTuneVM", "图片加载失败", e)
                _imageLoadError.value = e.message
            } finally {
                _isLoadingImage.value = false
            }
        }
    }

    /**
     * 设置直接传入的 Bitmap（例如从其他功能跳转带入）
     */
    fun setBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _sourceBitmap.value?.recycle()
            _sourceBitmap.value = bitmap
            _previewBitmap.value?.recycle()
            _previewBitmap.value = null
            _selectedImageUri.value = null
            renderPreviewAsync(context)
            pushHistory(force = true)
        }
    }

    private suspend fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Pair<Bitmap?, Int> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return@withContext Pair(null, 1)

            var sampleSize = 1
            while (max(width, height) / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            context.contentResolver.openInputStream(uri)?.use { decodeInput ->
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeStream(decodeInput, null, decodeOptions)
                // 如果仍超过限制，进行二次缩放
                if (bitmap != null && max(bitmap.width, bitmap.height) > maxDimension) {
                    val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
                    val scaled = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                    bitmap.recycle()
                    return@withContext Pair(scaled, sampleSize)
                }
                return@withContext Pair(bitmap, sampleSize)
            }
        }
        Pair(null, 1)
    }

    // ==================== GPU 实时预览 ====================

    private suspend fun ensureGPUInitialized(context: Context) {
        if (!gpuInitialized) {
            gpuRenderManager = GPURenderManager.acquire(context)
            gpuInitialized = gpuRenderManager?.initialize() == true
        }
    }

    private suspend fun renderPreviewAsync(context: Context) {
        val bitmap = _sourceBitmap.value ?: return
        ensureGPUInitialized(context)
        val renderer = gpuRenderManager ?: return
        // 防护：GPU 渲染器可能已初始化但 GPU 实际不可用（竞态窗口），强制 CPU 渲染
        val result = if (renderer.isGPUAvailable()) {
            renderer.renderPreview(bitmap, buildEffectiveParams())
        } else {
            renderer.renderPreview(bitmap, buildEffectiveParams()) // CPU fallback 已在内部处理
        }
        if (result != null) {
            _previewBitmap.value?.recycle()
            _previewBitmap.value = result
        }
    }

    /**
     * 刷新预览（参数变化时调用）
     */
    fun refreshPreview(context: Context) {
        viewModelScope.launch {
            renderPreviewAsync(context)
        }
    }

    // ==================== 参数操作 ====================

    fun updateParam(paramName: String, value: Float) {
        val current = _currentParams.value
        val next = when (paramName) {
            "exposure" -> current.copy(exposure = value)
            "brightness" -> current.copy(brightness = value)
            "contrast" -> current.copy(contrast = value)
            "saturation" -> current.copy(saturation = value)
            "warmth" -> current.copy(warmth = value)
            "vibrance" -> current.copy(vibrance = value)
            "highlights" -> current.copy(highlights = value)
            "shadows" -> current.copy(shadows = value)
            "whites" -> current.copy(whites = value)
            "blacks" -> current.copy(blacks = value)
            "texture" -> current.copy(texture = value)
            "clarity" -> current.copy(clarity = value)
            "sharpness" -> current.copy(sharpness = value)
            "dehaze" -> current.copy(dehaze = value)
            "denoise" -> current.copy(denoise = value)
            "grain" -> current.copy(grain = value)
            "fade" -> current.copy(fade = value)
            "skinSmooth" -> current.copy(skinSmooth = value)
            else -> current
        }
        if (next != current) {
            _currentParams.value = next
            pushHistory()
        }
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectHsl(id: String) {
        _selectedHslId.value = id
    }

    fun updateHSL(hslId: String, type: String, value: Int) {
        val current = _hslValues.value.toMutableList()
        val index = current.indexOfFirst { it.id == hslId }
        if (index >= 0) {
            val hsl = current[index]
            when (type) {
                "hue" -> hsl.hue = value
                "saturation" -> hsl.saturation = value
                "luminance" -> hsl.luminance = value
            }
            current[index] = hsl
            _hslValues.value = current
            syncHSLToRenderParameters()
        }
    }

    private fun syncHSLToRenderParameters() {
        val map = _hslValues.value.associateBy { it.id }
        val current = _currentParams.value
        _currentParams.value = current.copy(
            hslRedHue = map["red"]?.hue?.toFloat() ?: 0f,
            hslRedSaturation = map["red"]?.saturation?.toFloat() ?: 0f,
            hslRedLuminance = map["red"]?.luminance?.toFloat() ?: 0f,
            hslOrangeHue = map["orange"]?.hue?.toFloat() ?: 0f,
            hslOrangeSaturation = map["orange"]?.saturation?.toFloat() ?: 0f,
            hslOrangeLuminance = map["orange"]?.luminance?.toFloat() ?: 0f,
            hslYellowHue = map["yellow"]?.hue?.toFloat() ?: 0f,
            hslYellowSaturation = map["yellow"]?.saturation?.toFloat() ?: 0f,
            hslYellowLuminance = map["yellow"]?.luminance?.toFloat() ?: 0f,
            hslGreenHue = map["green"]?.hue?.toFloat() ?: 0f,
            hslGreenSaturation = map["green"]?.saturation?.toFloat() ?: 0f,
            hslGreenLuminance = map["green"]?.luminance?.toFloat() ?: 0f,
            hslCyanHue = map["cyan"]?.hue?.toFloat() ?: 0f,
            hslCyanSaturation = map["cyan"]?.saturation?.toFloat() ?: 0f,
            hslCyanLuminance = map["cyan"]?.luminance?.toFloat() ?: 0f,
            hslBlueHue = map["blue"]?.hue?.toFloat() ?: 0f,
            hslBlueSaturation = map["blue"]?.saturation?.toFloat() ?: 0f,
            hslBlueLuminance = map["blue"]?.luminance?.toFloat() ?: 0f,
            hslPurpleHue = map["purple"]?.hue?.toFloat() ?: 0f,
            hslPurpleSaturation = map["purple"]?.saturation?.toFloat() ?: 0f,
            hslPurpleLuminance = map["purple"]?.luminance?.toFloat() ?: 0f,
            hslMagentaHue = map["magenta"]?.hue?.toFloat() ?: 0f,
            hslMagentaSaturation = map["magenta"]?.saturation?.toFloat() ?: 0f,
            hslMagentaLuminance = map["magenta"]?.luminance?.toFloat() ?: 0f
        )
        pushHistory()
    }

    // ==================== 曲线操作 ====================

    fun setCurveChannel(channel: String) {
        _curveChannel.value = channel
    }

    fun updateCurvePoints(channel: String, points: List<CurvePoint>) {
        val current = _curvePoints.value.toMutableMap()
        current[channel] = points.sortedBy { it.x }
        _curvePoints.value = current
        syncCurveToRenderParameters()
    }

    fun applyCurvePreset(presetId: String) {
        val points = when (presetId) {
            "linear" -> listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
            "highContrast" -> listOf(
                CurvePoint(0f, 0f), CurvePoint(0.25f, 0.1f),
                CurvePoint(0.75f, 0.9f), CurvePoint(1f, 1f)
            )
            "soft" -> listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.55f), CurvePoint(1f, 1f))
            "sCurve" -> listOf(
                CurvePoint(0f, 0f), CurvePoint(0.25f, 0.15f),
                CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.85f), CurvePoint(1f, 1f)
            )
            "invert" -> listOf(CurvePoint(0f, 1f), CurvePoint(1f, 0f))
            else -> listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        }
        updateCurvePoints(_curveChannel.value, points)
    }

    private fun syncCurveToRenderParameters() {
        val current = _currentParams.value
        _currentParams.value = current.copy(
            curveRgbLut = generateCurveLut(_curvePoints.value["rgb"]),
            curveRedLut = generateCurveLut(_curvePoints.value["red"]),
            curveGreenLut = generateCurveLut(_curvePoints.value["green"]),
            curveBlueLut = generateCurveLut(_curvePoints.value["blue"])
        )
        pushHistory()
    }

    private fun generateCurveLut(points: List<CurvePoint>?): FloatArray {
        val sorted = points?.sortedBy { it.x } ?: return RenderParameters.IDENTITY_CURVE.copyOf()
        val lut = FloatArray(256)
        for (i in 0..255) {
            val x = i / 255f
            lut[i] = interpolateCurve(sorted, x)
        }
        return lut
    }

    private fun interpolateCurve(points: List<CurvePoint>, x: Float): Float {
        if (points.isEmpty()) return x
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            if (x in p0.x..p1.x) {
                val t = if (p1.x != p0.x) (x - p0.x) / (p1.x - p0.x) else 0f
                return p0.y + (p1.y - p0.y) * t
            }
        }
        return x
    }

    // ==================== 风格与智能优化 ====================

    fun selectStyle(styleId: String) {
        _selectedStyleId.value = styleId
        val style = COLOR_STYLES.find { it.id == styleId }
        if (style != null) {
            _currentParams.value = style.params
            syncRenderParamsToHSL(style.params)
            syncRenderParamsToCurve(style.params)
            pushHistory(force = true)
        }
    }

    private fun syncRenderParamsToHSL(params: RenderParameters) {
        val map = mapOf(
            "red" to Triple(params.hslRedHue, params.hslRedSaturation, params.hslRedLuminance),
            "orange" to Triple(params.hslOrangeHue, params.hslOrangeSaturation, params.hslOrangeLuminance),
            "yellow" to Triple(params.hslYellowHue, params.hslYellowSaturation, params.hslYellowLuminance),
            "green" to Triple(params.hslGreenHue, params.hslGreenSaturation, params.hslGreenLuminance),
            "cyan" to Triple(params.hslCyanHue, params.hslCyanSaturation, params.hslCyanLuminance),
            "blue" to Triple(params.hslBlueHue, params.hslBlueSaturation, params.hslBlueLuminance),
            "purple" to Triple(params.hslPurpleHue, params.hslPurpleSaturation, params.hslPurpleLuminance),
            "magenta" to Triple(params.hslMagentaHue, params.hslMagentaSaturation, params.hslMagentaLuminance)
        )
        _hslValues.value = _hslValues.value.map { hsl ->
            val (h, s, l) = map[hsl.id] ?: Triple(0f, 0f, 0f)
            hsl.copy(hue = h.toInt(), saturation = s.toInt(), luminance = l.toInt())
        }
    }

    private fun syncRenderParamsToCurve(params: RenderParameters) {
        // 从 LUT 反推控制点较复杂，这里仅保持当前曲线状态；
        // 若所有曲线 LUT 均为恒等映射则重置为线性。
        if (params.curveRgbLut.contentEquals(RenderParameters.IDENTITY_CURVE) &&
            params.curveRedLut.contentEquals(RenderParameters.IDENTITY_CURVE) &&
            params.curveGreenLut.contentEquals(RenderParameters.IDENTITY_CURVE) &&
            params.curveBlueLut.contentEquals(RenderParameters.IDENTITY_CURVE)) {
            _curvePoints.value = mapOf(
                "rgb" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
                "red" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
                "green" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
                "blue" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
            )
        }
    }

    fun toggleOptimization(optimizationId: String) {
        val current = _selectedOptimizations.value.toMutableSet()
        if (current.contains(optimizationId)) {
            current.remove(optimizationId)
        } else {
            current.add(optimizationId)
        }
        _selectedOptimizations.value = current
        applySmartOptimizations()
    }

    private fun applySmartOptimizations() {
        var params = RenderParameters()
        _selectedOptimizations.value.forEach { id ->
            params = params.merge(SMART_OPTIMIZATION_PARAMS[id] ?: RenderParameters())
        }
        // 合并到当前参数（保留用户已手动调整的参数）
        _currentParams.value = _currentParams.value.merge(params)
        pushHistory(force = true)
    }

    fun toggleParamLock(paramId: String) {
        val current = _lockedParams.value.toMutableSet()
        if (current.contains(paramId)) current.remove(paramId)
        else current.add(paramId)
        _lockedParams.value = current
    }

    // ==================== AI 推理 ====================

    /**
     * 取消正在进行的 AI 推理协程，防止屏幕销毁后仍更新已销毁的 UI 状态导致闪退
     */
    fun cancelInference() {
        inferenceJob?.cancel()
        _isProcessing.value = false
    }

    fun performAIInference(bitmap: Bitmap?) {
        if (bitmap == null) return

        // 检查 AI 可用性
        val inferenceEngine = aiManager.getInferenceEngine()
        if (inferenceEngine != null && !inferenceEngine.isModelLoaded()) {
            _isAIModelAvailable.value = false
            _aiUnavailableMessage.value = "AI 功能暂不可用"
            _inferenceStage.value = InferenceStage.ERROR
            _inferenceMessage.value = "AI 功能暂不可用，模型未加载"
            _errorState.value = "AI 功能暂不可用"
            return
        }

        // 先取消旧的，防止竞态：cancel 和下面的 launch 之间 inferenceJob 为 null
        inferenceJob?.cancel()
        inferenceJob = viewModelScope.launch {
            _inferenceStage.value = InferenceStage.ANALYZING
            _inferenceProgress.value = 0f
            _inferenceMessage.value = "正在分析图像..."
            _isOfflineResult.value = false
            _showSuccess.value = false
            _errorState.value = null
            _isAIModelAvailable.value = true
            _aiUnavailableMessage.value = null

            try {
                _inferenceProgress.value = 0.3f
                _inferenceStage.value = InferenceStage.DETECTING_SUBJECT
                _inferenceMessage.value = "检测主体..."
                _inferenceProgress.value = 0.5f
                _inferenceStage.value = InferenceStage.COMPUTING_PARAMS
                _inferenceMessage.value = "计算参数..."

                val currentParamMap = _currentParams.value.toMap().mapValues { it.value.toInt() }
                when (val result = aiManager.generateAISuggestion(bitmap, currentParamMap)) {
                    is AISuggestionResult.Success -> {
                        _isOfflineResult.value = result.isOfflineMode
                        _inferenceProgress.value = 0.8f
                        _inferenceStage.value = InferenceStage.APPLYING_AI
                        _inferenceMessage.value = if (result.isOfflineMode) "应用本地AI建议..." else "应用云端AI建议..."

                        result.suggestion.suggestions.forEach { suggestion ->
                            if (!_lockedParams.value.contains(suggestion.field)) {
                                updateParam(suggestion.field, suggestion.suggestedValue.toFloat())
                            }
                        }
                        _inferenceProgress.value = 1f
                        _inferenceStage.value = InferenceStage.COMPLETED
                        _inferenceMessage.value = "完成"
                        _showSuccess.value = true
                    }
                    is AISuggestionResult.Error -> {
                        _inferenceStage.value = InferenceStage.ERROR
                        _inferenceMessage.value = "推理失败: ${result.error.message}"
                        _errorState.value = result.error.message
                    }
                }
            } catch (e: Exception) {
                _inferenceStage.value = InferenceStage.ERROR
                _inferenceMessage.value = "推理失败: ${e.message}"
                _errorState.value = e.message
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * 应用 AI 微调参数到当前预设。
     * AI 不可用时返回 false 并设置错误状态。
     */
    suspend fun applyFineTune(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false

        val inferenceEngine = aiManager.getInferenceEngine()
        if (inferenceEngine != null && !inferenceEngine.isModelLoaded()) {
            _isAIModelAvailable.value = false
            _aiUnavailableMessage.value = "AI 功能暂不可用"
            _errorState.value = "AI 功能暂不可用，无法应用微调"
            return false
        }

        return try {
            val currentParamMap = _currentParams.value.toMap().mapValues { it.value.toInt() }
            when (val result = aiManager.generateAISuggestion(bitmap, currentParamMap)) {
                is AISuggestionResult.Success -> {
                    result.suggestion.suggestions.forEach { suggestion ->
                        if (!_lockedParams.value.contains(suggestion.field)) {
                            updateParam(suggestion.field, suggestion.suggestedValue.toFloat())
                        }
                    }
                    _showSuccess.value = true
                    true
                }
                is AISuggestionResult.Error -> {
                    _errorState.value = result.error.message
                    false
                }
            }
        } catch (e: Exception) {
            _errorState.value = e.message ?: "应用微调失败"
            false
        }
    }

    // ==================== 历史记录（撤销/重做） ====================

    private fun pushHistory(force: Boolean = false) {
        val params = _currentParams.value
        // 避免重复入栈
        if (!force && historyIndex >= 0 && history[historyIndex] == params) return
        // 删除当前索引之后的历史
        while (history.size > historyIndex + 1) {
            history.removeLast()
        }
        if (history.size >= MAX_HISTORY_SIZE) {
            history.removeFirst()
            historyIndex--
        }
        history.addLast(params)
        historyIndex++
        if (!force) {
            _hasChanges.value = true
        }
        updateHistoryState()
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            _currentParams.value = history[historyIndex]
            syncRenderParamsToHSL(_currentParams.value)
            syncRenderParamsToCurve(_currentParams.value)
            updateHistoryState()
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            _currentParams.value = history[historyIndex]
            syncRenderParamsToHSL(_currentParams.value)
            syncRenderParamsToCurve(_currentParams.value)
            updateHistoryState()
        }
    }

    private fun updateHistoryState() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    // ==================== 对比与重置 ====================

    fun toggleCompare() {
        _showCompare.value = !_showCompare.value
    }

    fun reset() {
        _currentParams.value = RenderParameters()
        _hslValues.value = getDefaultHSLValues()
        _curvePoints.value = mapOf(
            "rgb" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "red" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "green" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "blue" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        )
        _selectedStyleId.value = null
        _selectedOptimizations.value = emptySet()
        _lockedParams.value = emptySet()
        _hasChanges.value = false
        syncCurveToRenderParameters()
        pushHistory(force = true)
    }

    /**
     * 恢复到原始状态（清除所有修改，包括历史记录）
     */
    fun resetToOriginal() {
        _currentParams.value = RenderParameters()
        _hslValues.value = getDefaultHSLValues()
        _curvePoints.value = mapOf(
            "rgb" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "red" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "green" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "blue" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        )
        _selectedStyleId.value = null
        _selectedOptimizations.value = emptySet()
        _lockedParams.value = emptySet()
        _hasChanges.value = false
        history.clear()
        historyIndex = -1
        _canUndo.value = false
        _canRedo.value = false
        syncCurveToRenderParameters()
    }

    fun clearSuccess() {
        _showSuccess.value = false
    }

    // ==================== 导出 ====================

    /**
     * 使用 GPU 渲染管线导出最终图片，确保与预览完全一致
     */
    suspend fun exportImage(context: Context): Boolean = withContext(Dispatchers.IO) {
        val source = _sourceBitmap.value ?: run {
            _saveError.value = "没有可保存的图片"
            return@withContext false
        }
        val manager = gpuRenderManager ?: GPURenderManager.acquire(context).also {
            gpuRenderManager = it
            gpuInitialized = it.initialize() == true
        }
        try {
            // 使用 isGPUAvailable() 而非 gpuInitialized 避免竞态窗口
            val result = if (manager.isGPUAvailable()) {
                manager.renderSync(source, buildEffectiveParams(), RenderQuality.HIGH)
            } else {
                manager.renderSync(source, buildEffectiveParams(), RenderQuality.HIGH) // CPU fallback 已在内部处理
            }
            val output = when (result) {
                is com.silas.omaster.engine.RenderResult.Success -> result.outputBitmap
                is com.silas.omaster.engine.RenderResult.FallbackToCPU -> result.outputBitmap
                else -> null
            }
            if (output == null) {
                _saveError.value = "渲染失败"
                return@withContext false
            }

            val saved = saveBitmapToGallery(context, output)
            if (!saved) {
                _saveError.value = "保存到相册失败"
            }
            saved
        } catch (e: Exception) {
            Log.e("AIFineTuneVM", "导出失败", e)
            _saveError.value = e.message ?: "导出失败"
            false
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "omaster_ai_${System.currentTimeMillis()}.jpg"
        return try {
            val customModel = com.silas.omaster.data.local.SettingsManager
                .getInstance(context).customDeviceModel
                .ifBlank { null }
            val sourceUri = _selectedImageUri.value
            val savedUri = com.silas.omaster.infrastructure.utils.ExifPreserver.saveWithExif(
                context = context,
                bitmap = bitmap,
                sourceUri = sourceUri,
                targetFileName = filename,
                targetRelativePath = Environment.DIRECTORY_PICTURES + "/OMaster",
                customDeviceModel = customModel
            )
            savedUri != null
        } catch (e: Exception) {
            Log.e("AIFineTuneVM", "保存到相册失败", e)
            false
        }
    }

    /**
     * 获取最终参数（含 HSL/曲线）
     */
    fun getFinalParams(): RenderParameters = buildEffectiveParams()

    private fun buildEffectiveParams(): RenderParameters {
        // 智能优化已经合并到 _currentParams，这里直接返回
        val params = _currentParams.value
        // 注入当前 3D LUT 状态，使预览与导出共享 GPU 管线内的 LUT 色彩映射
        return if (_active3DLUTId.value != null && _lut3DTextureId != 0) {
            params.copy(
                lutEnabled = true,
                lutTextureId = _lut3DTextureId,
                lutSize = _lut3DSize,
                lutStrength = _lut3DStrength.value
            )
        } else {
            params.copy(lutEnabled = false, lutTextureId = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        _sourceBitmap.value?.recycle()
        _previewBitmap.value?.recycle()
        gpuRenderManager?.release()
    }

    // ================== 3D LUT 集成 ==================

    private val _active3DLUTId = MutableStateFlow<String?>(null)
    val active3DLUTId: StateFlow<String?> = _active3DLUTId.asStateFlow()

    private val _lut3DStrength = MutableStateFlow(1.0f)
    val lut3DStrength: StateFlow<Float> = _lut3DStrength.asStateFlow()

    // 已上传到 GPU 的 3D LUT 纹理 ID 与尺寸（0 表示未上传）
    private var _lut3DTextureId: Int = 0
    private var _lut3DSize: Int = 0

    /**
     * 应用 3D LUT 到当前图片
     *
     * GPU 可用时：上传 LUT 为 GL 纹理，由主渲染管线内的 image_adjust.frag 着色器
     * 在曲线之后、光影调整之前应用（uLUT3DEnabled=1）。
     * GPU 不可用时：回退到 CPU 三线性插值（LUT3DRenderer.applyLUTCPU）。
     */
    fun apply3DLUT(context: Context, lutId: String, strength: Float = 1.0f) {
        val lutManager = LUTManager.getInstance(context)
        val lutData = lutManager.getCachedLUTData(lutId) ?: return

        _active3DLUTId.value = lutId
        _lut3DStrength.value = strength

        viewModelScope.launch {
            ensureGPUInitialized(context)
            val renderer = gpuRenderManager
            if (renderer != null && gpuInitialized) {
                // GPU 路径：上传 LUT 纹理，预览渲染时由着色器应用
                val textureId = renderer.uploadLUT3DTexture(lutData)
                _lut3DTextureId = textureId
                _lut3DSize = if (textureId != 0) lutData.size else 0
                renderPreviewAsync(context)
            } else {
                // GPU 不可用，回退到 CPU LUT
                val currentPreview = _previewBitmap.value ?: _sourceBitmap.value ?: return@launch
                val result = withContext(Dispatchers.Default) {
                    LUT3DRenderer.applyLUTCPU(currentPreview, lutData, strength)
                }
                _previewBitmap.value = result
            }
        }
    }

    /**
     * 移除 3D LUT 效果，恢复原始渲染结果
     */
    fun remove3DLUT(context: Context) {
        _active3DLUTId.value = null
        _lut3DStrength.value = 1.0f
        viewModelScope.launch {
            // 释放 GPU LUT 纹理
            gpuRenderManager?.releaseLUT3DTexture()
            _lut3DTextureId = 0
            _lut3DSize = 0
            renderPreviewAsync(context)
        }
    }

    /**
     * 调整 3D LUT 强度
     */
    fun update3DLUTStrength(context: Context, strength: Float) {
        _lut3DStrength.value = strength.coerceIn(0f, 1f)
        viewModelScope.launch {
            renderPreviewAsync(context)
        }
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 30
    }
}

/**
 * ViewModel 工厂
 */
class AIFineTuneViewModelFactory(
    private val aiManager: AIFineTuneManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIFineTuneViewModel::class.java)) {
            return AIFineTuneViewModel(aiManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==================== 常量定义（与 Web 端对齐） ====================

val COLOR_STYLES = listOf(
    ColorStylePreset("natural", "自然", Icons.Default.Park, SuccessGreen, RenderParameters(), "自然真实"),
    ColorStylePreset("vivid", "鲜艳", Icons.Default.AutoAwesome, HasselbladOrange, RenderParameters(saturation = 20f), "鲜艳生动"),
    ColorStylePreset("warm", "暖调", Icons.Default.WbSunny, WarningYellow, RenderParameters(warmth = 15f), "温暖氛围"),
    ColorStylePreset("cool", "冷调", Icons.Default.AcUnit, CyanAccent, RenderParameters(warmth = -15f), "冷调清冷"),
    ColorStylePreset("film", "胶片", Icons.Default.CameraAlt, MediumGray, RenderParameters(saturation = -10f, contrast = 15f, warmth = 5f, grain = 15f, fade = 10f), "胶片质感"),
    ColorStylePreset("bw", "黑白", Icons.Default.FilterBAndW, Color.White, RenderParameters(saturation = -100f, contrast = 20f, clarity = 15f), "黑白经典"),
    ColorStylePreset("retro", "复古", Icons.Default.History, PureBlack, RenderParameters(saturation = -15f, contrast = 5f, warmth = 15f, fade = 20f, grain = 10f), "复古怀旧"),
    ColorStylePreset("cinematic", "电影", Icons.Default.Movie, DarkGray, RenderParameters(saturation = 5f, contrast = 25f, warmth = 10f), "电影风格"),
    ColorStylePreset("mood", "情绪", Icons.Default.Mood, LightGray, RenderParameters(saturation = -5f, contrast = 30f, warmth = -10f, shadows = 20f, highlights = -15f), "情绪氛围"),
    ColorStylePreset("soft", "柔和", Icons.Default.Cloud, Color.White.copy(alpha = 0.8f), RenderParameters(saturation = -10f, contrast = -10f, warmth = 5f, brightness = 10f, fade = 15f), "柔和朦胧"),
    ColorStylePreset("dramatic", "戏剧", Icons.Default.Bolt, ErrorRed, RenderParameters(saturation = 15f, contrast = 35f, warmth = 5f, clarity = 20f, highlights = -20f), "戏剧强烈"),
    ColorStylePreset("hdr", "HDR", Icons.Default.Landscape, HasselbladOrange.copy(alpha = 0.8f), RenderParameters(saturation = 10f, contrast = 20f, highlights = -30f, shadows = 30f, clarity = 25f), "HDR效果")
)

val SMART_OPTIMIZATIONS = listOf(
    SmartOptimization("hdrEnhance", "HDR增强", Icons.Default.Landscape, "智能HDR增强", SuccessGreen),
    SmartOptimization("noiseReduce", "智能降噪", Icons.Default.Grain, "AI降噪处理", CyanAccent),
    SmartOptimization("smartSharp", "智能锐化", Icons.Default.Tune, "AI锐化增强", HasselbladOrange),
    SmartOptimization("dehaze", "去雾", Icons.Default.Air, "去除雾气", LightGray),
    SmartOptimization("skinOptimize", "肤色优化", Icons.Default.Face2, "智能肤色调整", WarningYellow),
    SmartOptimization("skyEnhance", "天空增强", Icons.Default.Cloud, "天空增强PRO", CyanAccent, true),
    SmartOptimization("aiCompose", "AI构图", Icons.Default.Crop, "AI构图建议", HasselbladOrange, true),
    SmartOptimization("portraitBlur", "人像虚化", Icons.Default.Person2, "人像虚化PRO", PureBlack, true),
    SmartOptimization("colorMatch", "色彩匹配", Icons.Default.Palette, "色彩匹配PRO", MediumGray, true),
    SmartOptimization("smartLight", "智能补光", Icons.Default.Lightbulb, "智能补光PRO", WarningYellow.copy(alpha = 0.8f), true)
)

/**
 * 智能优化选项到 RenderParameters 的映射
 */
val SMART_OPTIMIZATION_PARAMS = mapOf(
    "hdrEnhance" to RenderParameters(highlights = -25f, shadows = 25f, clarity = 20f, contrast = 10f),
    "noiseReduce" to RenderParameters(denoise = 35f, sharpness = -10f, clarity = -5f),
    "smartSharp" to RenderParameters(sharpness = 30f, clarity = 15f, texture = 10f),
    "dehaze" to RenderParameters(dehaze = 40f, contrast = 10f, saturation = 10f, clarity = 10f),
    "skinOptimize" to RenderParameters(skinSmooth = 25f, clarity = -5f, contrast = -5f, warmth = 5f),
    "skyEnhance" to RenderParameters(saturation = 15f, clarity = 15f, dehaze = 20f, contrast = 5f),
    "aiCompose" to RenderParameters(clarity = 10f, contrast = 5f),
    "portraitBlur" to RenderParameters(skinSmooth = 20f, sharpness = -10f),
    "colorMatch" to RenderParameters(saturation = 10f, vibrance = 10f),
    "smartLight" to RenderParameters(shadows = 20f, exposure = 10f, brightness = 5f)
)

fun getDefaultHSLValues(): List<HSLValue> {
    return listOf(
        HSLValue("red", "红", Color(0xFFFF0000.toInt())),
        HSLValue("orange", "橙", HasselbladOrange),
        HSLValue("yellow", "黄", WarningYellow),
        HSLValue("green", "绿", SuccessGreen),
        HSLValue("cyan", "青", CyanAccent),
        HSLValue("blue", "蓝", Color(0xFF0000FF.toInt())),
        HSLValue("purple", "紫", Color(0xFF800080.toInt())),
        HSLValue("magenta", "洋红", Color(0xFFFF00FF.toInt()))
    )
}
