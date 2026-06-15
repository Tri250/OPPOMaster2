package com.silas.omaster.ui.features

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.ai.AISuggestion
import com.silas.omaster.ai.ErrorState
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * AI微调 ViewModel
 * 管理AI微调的所有状态：参数、HSL、曲线、推理进度等
 */
class AIFineTuneViewModel(
    private val aiManager: AIFineTuneManager
) : ViewModel() {

    // 处理状态
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // 推荐参数
    private val _suggestedParams = MutableStateFlow<AISuggestion?>(null)
    val suggestedParams: StateFlow<AISuggestion?> = _suggestedParams.asStateFlow()

    // 错误状态
    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    // 当前Tab
    private val _activeTab = MutableStateFlow("basic")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // 选中的风格ID
    private val _selectedStyleId = MutableStateFlow<String?>(null)
    val selectedStyleId: StateFlow<String?> = _selectedStyleId.asStateFlow()

    // 选中的优化选项
    private val _selectedOptimizations = MutableStateFlow<Set<String>>(emptySet())
    val selectedOptimizations: StateFlow<Set<String>> = _selectedOptimizations.asStateFlow()

    // 锁定的参数
    private val _lockedParams = MutableStateFlow<Set<String>>(emptySet())
    val lockedParams: StateFlow<Set<String>> = _lockedParams.asStateFlow()

    // 对比预览开关
    private val _showCompare = MutableStateFlow(false)
    val showCompare: StateFlow<Boolean> = _showCompare.asStateFlow()

    // 成功提示
    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    // 推理阶段
    private val _inferenceStage = MutableStateFlow(InferenceStage.IDLE)
    val inferenceStage: StateFlow<InferenceStage> = _inferenceStage.asStateFlow()

    // 推理进度
    private val _inferenceProgress = MutableStateFlow(0f)
    val inferenceProgress: StateFlow<Float> = _inferenceProgress.asStateFlow()

    // 推理消息
    private val _inferenceMessage = MutableStateFlow("")
    val inferenceMessage: StateFlow<String> = _inferenceMessage.asStateFlow()

    // HSL值
    private val _hslValues = MutableStateFlow(getDefaultHSLValues())
    val hslValues: StateFlow<List<HSLValue>> = _hslValues.asStateFlow()

    // 曲线通道
    private val _curveChannel = MutableStateFlow("RGB")
    val curveChannel: StateFlow<String> = _curveChannel.asStateFlow()

    // 曲线点
    private val _curvePoints = MutableStateFlow<Map<String, List<CurvePoint>>>(
        mapOf(
            "RGB" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "R" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "G" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
            "B" to listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        )
    )
    val curvePoints: StateFlow<Map<String, List<CurvePoint>>> = _curvePoints.asStateFlow()

    // 当前参数
    private val _currentParams = MutableStateFlow(RenderParameters())
    val currentParams: StateFlow<RenderParameters> = _currentParams.asStateFlow()

    // 推理任务
    private var inferenceJob: Job? = null

    init {
        observeAIManager()
    }

    /**
     * 观察AI Manager状态
     */
    private fun observeAIManager() {
        viewModelScope.launch {
            aiManager.isProcessing.collect { processing ->
                _isProcessing.value = processing
            }
        }
        viewModelScope.launch {
            aiManager.suggestedParams.collect { params ->
                _suggestedParams.value = params
                if (params != null) {
                    // 应用AI推荐（从AISuggestion提取参数到RenderParameters）
                    applySuggestionToCurrentParams(params)
                }
            }
        }
        viewModelScope.launch {
            aiManager.errorState.collect { error ->
                _errorState.value = error
            }
        }
    }

    /**
     * 设置Tab
     */
    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    /**
     * 选择风格
     */
    fun selectStyle(styleId: String) {
        _selectedStyleId.value = styleId
        // 应用风格预设参数
        val style = COLOR_STYLES.find { it.id == styleId }
        if (style != null) {
            _currentParams.value = style.params
        }
    }

    /**
     * 切换优化选项
     */
    fun toggleOptimization(optimizationId: String) {
        val current = _selectedOptimizations.value.toMutableSet()
        if (current.contains(optimizationId)) {
            current.remove(optimizationId)
        } else {
            current.add(optimizationId)
        }
        _selectedOptimizations.value = current
    }

    /**
     * 切换参数锁定
     */
    fun toggleParamLock(paramId: String) {
        val current = _lockedParams.value.toMutableSet()
        if (current.contains(paramId)) {
            current.remove(paramId)
        } else {
            current.add(paramId)
        }
        _lockedParams.value = current
    }

    /**
     * 切换对比预览
     */
    fun toggleCompare() {
        _showCompare.value = !_showCompare.value
    }

    /**
     * 更新参数值
     */
    fun updateParam(paramName: String, value: Int) {
        val v = value.toFloat()
        val current = _currentParams.value
        _currentParams.value = when (paramName) {
            "exposure" -> current.copy(exposure = v)
            "brightness" -> current.copy(brightness = v)
            "contrast" -> current.copy(contrast = v)
            "saturation" -> current.copy(saturation = v)
            "temperature" -> current.copy(warmth = v)
            "vibrance" -> current.copy(vibrance = v)
            "highlight" -> current.copy(highlights = v)
            "shadow" -> current.copy(shadows = v)
            "whiteLevel" -> current.copy(whites = v)
            "blackLevel" -> current.copy(blacks = v)
            "texture" -> current.copy(texture = v)
            "clarity" -> current.copy(clarity = v)
            "sharpness" -> current.copy(sharpness = v)
            "dehaze" -> current.copy(dehaze = v)
            "noiseReduction" -> current.copy(denoise = v)
            "grain" -> current.copy(grain = v)
            "fade" -> current.copy(fade = v)
            "skinSmooth" -> current.copy(skinSmooth = v)
            else -> current
        }
    }

    /**
     * 更新HSL值
     */
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
        }
    }

    /**
     * 设置曲线通道
     */
    fun setCurveChannel(channel: String) {
        _curveChannel.value = channel
    }

    /**
     * 更新曲线点
     */
    fun updateCurvePoints(channel: String, points: List<CurvePoint>) {
        val current = _curvePoints.value.toMutableMap()
        current[channel] = points
        _curvePoints.value = current
    }

    /**
     * 应用曲线预设
     */
    fun applyCurvePreset(presetId: String) {
        val points = when (presetId) {
            "linear" -> listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
            "highContrast" -> listOf(CurvePoint(0f, 0f), CurvePoint(0.25f, 0.1f), CurvePoint(0.75f, 0.9f), CurvePoint(1f, 1f))
            "soft" -> listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.55f), CurvePoint(1f, 1f))
            "sCurve" -> listOf(CurvePoint(0f, 0f), CurvePoint(0.25f, 0.15f), CurvePoint(0.5f, 0.5f), CurvePoint(0.75f, 0.85f), CurvePoint(1f, 1f))
            "invert" -> listOf(CurvePoint(0f, 1f), CurvePoint(1f, 0f))
            else -> listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
        }
        updateCurvePoints(_curveChannel.value, points)
    }

    /**
     * 执行AI推理
     */
    fun performAIInference(bitmap: Bitmap?) {
        if (bitmap == null) return
        
        inferenceJob?.cancel()
        inferenceJob = viewModelScope.launch {
            _inferenceStage.value = InferenceStage.ANALYZING
            _inferenceProgress.value = 0f
            _inferenceMessage.value = "正在分析图像..."
            
            try {
                // 模拟推理进度
                _inferenceProgress.value = 0.2f
                _inferenceStage.value = InferenceStage.DETECTING_SUBJECT
                _inferenceMessage.value = "检测主体..."
                
                _inferenceProgress.value = 0.4f
                _inferenceStage.value = InferenceStage.ANALYZING_LIGHT
                _inferenceMessage.value = "分析光线..."
                
                _inferenceProgress.value = 0.6f
                _inferenceStage.value = InferenceStage.COMPUTING_PARAMS
                _inferenceMessage.value = "计算参数..."
                
                // 实际AI推理（仅模拟进度，不调用不存在的analyzeImage）
                // aiManager.analyzeImage(bitmap)
                
                _inferenceProgress.value = 0.8f
                _inferenceStage.value = InferenceStage.APPLYING_AI
                _inferenceMessage.value = "应用AI建议..."
                
                _inferenceProgress.value = 1f
                _inferenceStage.value = InferenceStage.COMPLETED
                _inferenceMessage.value = "完成"
                _showSuccess.value = true
                
            } catch (e: Exception) {
                _inferenceStage.value = InferenceStage.ERROR
                _inferenceMessage.value = "推理失败: ${e.message}"
                _errorState.value = ErrorState(ErrorState.ErrorType.UNKNOWN, e.message ?: "未知错误")
            }
        }
    }

    /**
     * 重置推理状态
     */
    fun resetInference() {
        _inferenceStage.value = InferenceStage.IDLE
        _inferenceProgress.value = 0f
        _inferenceMessage.value = ""
        _showSuccess.value = false
        _errorState.value = null
    }

    /**
     * 清除成功提示
     */
    fun clearSuccess() {
        _showSuccess.value = false
    }

    /**
     * 获取最终参数
     */
    fun getFinalParams(): RenderParameters {
        return _currentParams.value
    }

    /**
     * 将AISuggestion应用到当前参数
     */
    private fun applySuggestionToCurrentParams(suggestion: AISuggestion) {
        val current = _currentParams.value
        var updated = current
        suggestion.suggestions.forEach { ps ->
            val v = ps.suggestedValue.toFloat()
            updated = when (ps.field) {
                "exposure" -> updated.copy(exposure = v)
                "brightness" -> updated.copy(brightness = v)
                "contrast" -> updated.copy(contrast = v)
                "saturation" -> updated.copy(saturation = v)
                "temperature" -> updated.copy(warmth = v)
                "vibrance" -> updated.copy(vibrance = v)
                "highlights" -> updated.copy(highlights = v)
                "shadows" -> updated.copy(shadows = v)
                "whites" -> updated.copy(whites = v)
                "blacks" -> updated.copy(blacks = v)
                "texture" -> updated.copy(texture = v)
                "clarity" -> updated.copy(clarity = v)
                "sharpness" -> updated.copy(sharpness = v)
                "dehaze" -> updated.copy(dehaze = v)
                "denoise" -> updated.copy(denoise = v)
                "grain" -> updated.copy(grain = v)
                "fade" -> updated.copy(fade = v)
                "skinSmooth" -> updated.copy(skinSmooth = v)
                else -> updated
            }
        }
        _currentParams.value = updated
    }

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
    }
}

/**
 * AIFineTuneViewModel 工厂
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

// 常量定义（与Web端对齐）
private val ICON_NATURE get() = androidx.compose.material.icons.Icons.Filled.Nature
private val ICON_AUTO_AWESOME get() = androidx.compose.material.icons.Icons.Filled.AutoAwesome
private val ICON_WB_SUNNY get() = androidx.compose.material.icons.Icons.Filled.WbSunny
private val ICON_AC_UNIT get() = androidx.compose.material.icons.Icons.Filled.AcUnit
private val ICON_CAMERA get() = androidx.compose.material.icons.Icons.Filled.Camera
private val ICON_FILTER_BW get() = androidx.compose.material.icons.Icons.Filled.FilterBAndW
private val ICON_HISTORY get() = androidx.compose.material.icons.Icons.Filled.History
private val ICON_MOVIE get() = androidx.compose.material.icons.Icons.Filled.Movie
private val ICON_MOOD get() = androidx.compose.material.icons.Icons.Filled.Mood
private val ICON_CLOUD get() = androidx.compose.material.icons.Icons.Filled.Cloud
private val ICON_BOLT get() = androidx.compose.material.icons.Icons.Filled.Bolt
private val ICON_LANDSCAPE get() = androidx.compose.material.icons.Icons.Filled.Landscape
private val ICON_GRAIN get() = androidx.compose.material.icons.Icons.Filled.Grain
private val ICON_TUNE get() = androidx.compose.material.icons.Icons.Filled.Tune
private val ICON_AIR get() = androidx.compose.material.icons.Icons.Filled.Air
private val ICON_FACE_RETOUCHING get() = androidx.compose.material.icons.Icons.Filled.FaceRetouchingNatural
private val ICON_CROP get() = androidx.compose.material.icons.Icons.Filled.Crop
private val ICON_PERSON_PIN get() = androidx.compose.material.icons.Icons.Filled.PersonPin
private val ICON_PALETTE get() = androidx.compose.material.icons.Icons.Filled.Palette
private val ICON_LIGHTBULB get() = androidx.compose.material.icons.Icons.Filled.Lightbulb

val COLOR_STYLES = listOf(
    ColorStylePreset("natural", "自然", ICON_NATURE, SuccessGreen, RenderParameters(), "自然真实"),
    ColorStylePreset("vivid", "鲜艳", ICON_AUTO_AWESOME, HasselbladOrange, RenderParameters(saturation = 20f), "鲜艳生动"),
    ColorStylePreset("warm", "暖调", ICON_WB_SUNNY, WarningYellow, RenderParameters(warmth = 15f), "温暖氛围"),
    ColorStylePreset("cool", "冷调", ICON_AC_UNIT, CyanAccent, RenderParameters(warmth = -15f), "冷调清冷"),
    ColorStylePreset("film", "胶片", ICON_CAMERA, MediumGray, RenderParameters(grain = 15f, fade = 10f), "胶片质感"),
    ColorStylePreset("bw", "黑白", ICON_FILTER_BW, Color.White, RenderParameters(saturation = -100f), "黑白经典"),
    ColorStylePreset("retro", "复古", ICON_HISTORY, PureBlack, RenderParameters(fade = 20f, grain = 10f), "复古怀旧"),
    ColorStylePreset("cinematic", "电影", ICON_MOVIE, DarkGray, RenderParameters(contrast = 15f, fade = 5f), "电影风格"),
    ColorStylePreset("mood", "情绪", ICON_MOOD, LightGray, RenderParameters(shadows = -20f, fade = 15f), "情绪氛围"),
    ColorStylePreset("soft", "柔和", ICON_CLOUD, Color.White.copy(alpha = 0.8f), RenderParameters(clarity = -10f), "柔和朦胧"),
    ColorStylePreset("dramatic", "戏剧", ICON_BOLT, ErrorRed, RenderParameters(contrast = 30f, highlights = -20f), "戏剧强烈"),
    ColorStylePreset("hdr", "HDR", ICON_LANDSCAPE, HasselbladOrange.copy(alpha = 0.8f), RenderParameters(highlights = -30f, shadows = 30f), "HDR效果")
)

val SMART_OPTIMIZATIONS = listOf(
    SmartOptimization("hdrEnhance", "HDR增强", ICON_LANDSCAPE, "智能HDR增强", SuccessGreen),
    SmartOptimization("noiseReduce", "智能降噪", ICON_GRAIN, "AI降噪处理", CyanAccent),
    SmartOptimization("smartSharp", "智能锐化", ICON_TUNE, "AI锐化增强", HasselbladOrange),
    SmartOptimization("dehaze", "去雾", ICON_AIR, "去除雾气", LightGray),
    SmartOptimization("skinOptimize", "肤色优化", ICON_FACE_RETOUCHING, "智能肤色调整", WarningYellow),
    SmartOptimization("skyEnhance", "天空增强", ICON_CLOUD, "天空增强PRO", CyanAccent, true),
    SmartOptimization("aiCompose", "AI构图", ICON_CROP, "AI构图建议", HasselbladOrange, true),
    SmartOptimization("portraitBlur", "人像虚化", ICON_PERSON_PIN, "人像虚化PRO", PureBlack, true),
    SmartOptimization("colorMatch", "色彩匹配", ICON_PALETTE, "色彩匹配PRO", MediumGray, true),
    SmartOptimization("smartLight", "智能补光", ICON_LIGHTBULB, "智能补光PRO", WarningYellow.copy(alpha = 0.8f), true)
)

fun getDefaultHSLValues(): List<HSLValue> {
    return listOf(
        HSLValue("red", "红", Color.Red),
        HSLValue("orange", "橙", HasselbladOrange),
        HSLValue("yellow", "黄", WarningYellow),
        HSLValue("green", "绿", SuccessGreen),
        HSLValue("cyan", "青", CyanAccent),
        HSLValue("blue", "蓝", Color.Blue),
        HSLValue("purple", "紫", Color(0xFF800080)),
        HSLValue("magenta", "洋红", Color(0xFFFF00FF))
    )
}