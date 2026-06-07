package com.silas.omaster.ai

import android.content.Context
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

/**
 * AI 微调管理器
 * 色彩风格、参数调整控制
 *
 * FT-001: 一键AI微调 - 基于基础预设生成建议参数
 * FT-002: 单参数采纳 - 单选参数应用
 * FT-003: AI微调结果二次编辑 - 手动脱离AI推荐状态
 */
class AIFineTuneManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)

    // 当前调整参数
    private val _currentAdjustments = MutableStateFlow(AdjustmentParams())
    val currentAdjustments: StateFlow<AdjustmentParams> = _currentAdjustments.asStateFlow()

    // 处理状态
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // AI建议参数（派生自基础预设）
    private val _suggestedParams = MutableStateFlow<AISuggestion?>(null)
    val suggestedParams: StateFlow<AISuggestion?> = _suggestedParams.asStateFlow()

    // 基础预设（用于派生建议）
    private var basePreset: BasePreset? = null

    // 手动修改的参数（脱离AI推荐状态）
    private val _manuallyModifiedFields = MutableStateFlow<Set<String>>(emptySet())
    val manuallyModifiedFields: StateFlow<Set<String>> = _manuallyModifiedFields.asStateFlow()

    // 预设色彩风格
    val colorStyles = listOf(
        ColorStyle("natural", "自然", "还原真实色彩", 0, 0, 0),
        ColorStyle("vivid", "鲜艳", "增强色彩饱和度", 20, 10, 5),
        ColorStyle("film", "胶片", "复古胶片质感", -10, 15, 10),
        ColorStyle("bw", "黑白", "经典黑白影调", -100, 20, 0),
        ColorStyle("warm", "暖调", "温暖色调风格", 5, 0, 25),
        ColorStyle("cool", "冷调", "清冷色调风格", 5, 5, -20),
        ColorStyle("portrait", "人像", "优化肤色表现", 10, -5, 8),
        ColorStyle("landscape", "风景", "增强自然色彩", 15, 10, 0),
        ColorStyle("fresh_cc", "清新-CC胶片", "清新通透胶片感", 5, 8, 3),
        ColorStyle("rich", "浓郁", "浓郁饱满色彩", 25, 15, 8),
        ColorStyle("retro", "复古", "复古怀旧风格", -5, 10, 15)
    )

    // 智能优化预设
    val smartPresets = listOf(
        SmartPreset("auto_optimize", "智能优化", "AI自动分析并优化图片", "✨"),
        SmartPreset("hdr_enhance", "HDR增强", "提升动态范围", "🌅"),
        SmartPreset("noise_reduce", "降噪处理", "减少画面噪点", "🔇"),
        SmartPreset("sharpness", "清晰度", "增强细节锐度", "🔍"),
        SmartPreset("skin_smooth", "肤色优化", "自然美肤效果", "✨"),
        SmartPreset("sky_enhance", "天空增强", "优化天空色彩", "☁️"),
        SmartPreset("detail_enhance", "细节增强", "提升画面细节", "🔎"),
        SmartPreset("night_optimize", "夜景优化", "优化暗光表现", "🌃")
    )

    // 基础预设库（23+款，每款有独特的参数特征）
    private val basePresets = listOf(
        BasePreset("fresh_cc", "清新-CC胶片", mapOf(
            "saturation" to 5, "contrast" to 8, "brightness" to 3,
            "warmth" to 3, "clarity" to 10
        )),
        BasePreset("film_nc", "富士NC", mapOf(
            "saturation" to 8, "contrast" to 5, "brightness" to 2,
            "warmth" to 5, "clarity" to 8
        )),
        BasePreset("portrait_soft", "人像柔美", mapOf(
            "saturation" to 10, "contrast" to -5, "brightness" to 5,
            "skinSmooth" to 25, "warmth" to 8
        )),
        BasePreset("landscape_vivid", "风景鲜明", mapOf(
            "saturation" to 20, "contrast" to 15, "brightness" to 5,
            "clarity" to 20, "sharpness" to 15
        )),
        BasePreset("night_scene", "夜景氛围", mapOf(
            "contrast" to 20, "highlights" to -20, "shadows" to 30,
            "noiseReduction" to 30, "brightness" to -5
        )),
        BasePreset("food_warm", "美食暖调", mapOf(
            "saturation" to 15, "contrast" to 10, "brightness" to 8,
            "warmth" to 20, "clarity" to 12
        )),
        BasePreset("bw_classic", "经典黑白", mapOf(
            "saturation" to -100, "contrast" to 25, "brightness" to 0,
            "clarity" to 15, "sharpness" to 20
        )),
        BasePreset("cyberpunk", "赛博朋克", mapOf(
            "saturation" to 30, "contrast" to 25, "brightness" to -5,
            "highlights" to 20, "shadows" to -15
        )),
        BasePreset("vintage_film", "复古胶片", mapOf(
            "saturation" to -10, "contrast" to 15, "warmth" to 25,
            "clarity" to 8, "vignette" to 30
        )),
        BasePreset("hasselblad_rich", "哈苏浓郁", mapOf(
            "saturation" to 12, "contrast" to 10, "brightness" to 3,
            "warmth" to 5, "clarity" to 15
        )),
        BasePreset("hasselblad_natural", "哈苏自然", mapOf(
            "saturation" to 5, "contrast" to 8, "brightness" to 0,
            "warmth" to 2, "clarity" to 10
        )),
        BasePreset("find_x8_pro", "Find X8 Pro", mapOf(
            "saturation" to 10, "contrast" to 12, "brightness" to 5,
            "warmth" to 3, "clarity" to 18
        )),
        BasePreset("reno_portrait", "Reno人像", mapOf(
            "saturation" to 8, "contrast" to -3, "brightness" to 8,
            "skinSmooth" to 30, "warmth" to 5
        )),
        BasePreset("street_snapshot", "街拍快照", mapOf(
            "saturation" to 15, "contrast" to 18, "brightness" to 0,
            "clarity" to 22, "sharpness" to 18
        )),
        BasePreset("blue_hour", "蓝调时刻", mapOf(
            "saturation" to 5, "contrast" to 15, "brightness" to -8,
            "warmth" to -25, "highlights" to 10
        )),
        BasePreset("sunset_warm", "日落暖阳", mapOf(
            "saturation" to 25, "contrast" to 12, "brightness" to -3,
            "warmth" to 35, "shadows" to 15
        )),
        BasePreset("cloud_clear", "通透蓝天", mapOf(
            "saturation" to 20, "contrast" to 10, "brightness" to 8,
            "warmth" to -10, "clarity" to 25
        )),
        BasePreset("spring_green", "春日清新", mapOf(
            "saturation" to 18, "contrast" to 8, "brightness" to 10,
            "warmth" to 8, "clarity" to 15
        )),
        BasePreset("night_neon", "霓虹夜景", mapOf(
            "saturation" to 35, "contrast" to 20, "brightness" to -10,
            "highlights" to 25, "shadows" to -20
        )),
        BasePreset("macro_detail", "微距细节", mapOf(
            "saturation" to 8, "contrast" to 15, "brightness" to 5,
            "clarity" to 30, "sharpness" to 25, "detail" to 20
        )),
        BasePreset("pet_soft", "宠物柔光", mapOf(
            "saturation" to 12, "contrast" to 5, "brightness" to 8,
            "clarity" to 10, "skinSmooth" to 15
        )),
        BasePreset("snow_scene", "雪景纯净", mapOf(
            "saturation" to 5, "contrast" to -5, "brightness" to 15,
            "warmth" to 10, "clarity" to 12
        )),
        BasePreset("beach_vacation", "海滩度假", mapOf(
            "saturation" to 22, "contrast" to 12, "brightness" to 12,
            "warmth" to 15, "clarity" to 18
        ))
    )

    /**
     * FT-001: 一键AI微调
     * 基于基础预设生成建议参数，差异 < ±20
     *
     * @param presetId 基础预设ID
     * @return AI建议参数（最多4个参数）
     */
    suspend fun generateAISuggestion(presetId: String): AISuggestion = withContext(Dispatchers.Default) {
        _isProcessing.value = true

        // 模拟AI处理延迟（实际应调用AI模型）
        delay(1500) // 实际项目中 ≤ 3秒

        // 查找基础预设
        val base = basePresets.find { it.id == presetId } ?: basePresets.first()
        basePreset = base

        // 基于基础预设生成建议参数（派生逻辑）
        val current = _currentAdjustments.value
        val suggestions = mutableListOf<ParamSuggestion>()

        // 分析基础预设与当前参数的差异，生成建议
        val paramMap = mapOf(
            "saturation" to Triple(current.saturation, base.params["saturation"] ?: 0, "饱和度"),
            "contrast" to Triple(current.contrast, base.params["contrast"] ?: 0, "对比度"),
            "brightness" to Triple(current.brightness, base.params["brightness"] ?: 0, "亮度"),
            "warmth" to Triple(current.warmth, base.params["warmth"] ?: 0, "冷暖"),
            "clarity" to Triple(current.clarity, base.params["clarity"] ?: 0, "清晰度"),
            "sharpness" to Triple(current.sharpness, base.params["sharpness"] ?: 0, "锐度"),
            "highlights" to Triple(current.highlights, base.params["highlights"] ?: 0, "高光"),
            "shadows" to Triple(current.shadows, base.params["shadows"] ?: 0, "阴影")
        )

        // 找出差异最大的参数（确保建议参数从基础预设派生，差异 < ±20）
        val sortedParams = paramMap.entries.sortedBy {
            abs(it.value.second - it.value.first)
        }

        // 只取差异最大的参数，最多4个
        for (entry in sortedParams.take(4)) {
            val (key, triple) = entry
            val (currentVal, baseVal, displayName) = triple
            val diff = baseVal - currentVal

            // 只建议差异在 ±20 范围内的参数
            if (abs(diff) in 1..20) {
                suggestions.add(
                    ParamSuggestion(
                        field = key,
                        currentValue = currentVal,
                        suggestedValue = baseVal,
                        displayName = displayName,
                        isSelected = true // 默认全选
                    )
                )
            }
        }

        // 如果没有合适的建议，生成一些优化建议
        if (suggestions.size < 2) {
            suggestions.clear()
            suggestions.add(
                ParamSuggestion("saturation", current.saturation, current.saturation + 5, "饱和度", true)
            )
            suggestions.add(
                ParamSuggestion("contrast", current.contrast, current.contrast + 8, "对比度", true)
            )
            suggestions.add(
                ParamSuggestion("clarity", current.clarity, current.clarity + 10, "清晰度", true)
            )
        }

        val suggestion = AISuggestion(
            basePresetId = presetId,
            basePresetName = base.name,
            suggestions = suggestions,
            generatedAt = System.currentTimeMillis()
        )

        _suggestedParams.value = suggestion
        _isProcessing.value = false

        suggestion
    }

    /**
     * FT-001: 获取参数对比表
     */
    fun getParamComparison(): List<ParamComparison> {
        val suggestion = _suggestedParams.value ?: return emptyList()
        return suggestion.suggestions.map { s ->
            ParamComparison(
                field = s.field,
                displayName = s.displayName,
                currentValue = s.currentValue,
                suggestedValue = s.suggestedValue,
                difference = s.suggestedValue - s.currentValue
            )
        }
    }

    /**
     * FT-002: 应用选中的建议参数
     * 仅应用用户勾选的参数
     *
     * @param selectedFields 要应用的参数字段列表
     */
    fun applySelectedSuggestions(selectedFields: Set<String>) {
        val suggestion = _suggestedParams.value ?: return
        val current = _currentAdjustments.value

        var updated = current
        for (s in suggestion.suggestions) {
            if (selectedFields.contains(s.field)) {
                updated = when (s.field) {
                    "saturation" -> updated.copy(saturation = s.suggestedValue)
                    "contrast" -> updated.copy(contrast = s.suggestedValue)
                    "brightness" -> updated.copy(brightness = s.suggestedValue)
                    "warmth" -> updated.copy(warmth = s.suggestedValue)
                    "clarity" -> updated.copy(clarity = s.suggestedValue)
                    "sharpness" -> updated.copy(sharpness = s.suggestedValue)
                    "highlights" -> updated.copy(highlights = s.suggestedValue)
                    "shadows" -> updated.copy(shadows = s.suggestedValue)
                    "noiseReduction" -> updated.copy(noiseReduction = s.suggestedValue)
                    "skinSmooth" -> updated.copy(skinSmooth = s.suggestedValue)
                    "detail" -> updated.copy(detail = s.suggestedValue)
                    else -> updated
                }
            }
        }

        _currentAdjustments.value = updated
        _suggestedParams.value = null // 清除建议
        _manuallyModifiedFields.value = emptySet() // 清除手动修改记录
    }

    /**
     * FT-002: 单参数采纳
     * 仅采纳单个参数的变化
     *
     * @param field 参数字段名
     */
    fun applySingleParam(field: String) {
        applySelectedSuggestions(setOf(field))
    }

    /**
     * FT-003: 手动修改参数（脱离AI推荐状态）
     */
    fun manuallyAdjustParam(param: AdjustmentType, value: Int) {
        val fieldName = param.name.lowercase()
        val currentModified = _manuallyModifiedFields.value.toMutableSet()
        currentModified.add(fieldName)
        _manuallyModifiedFields.value = currentModified

        adjustParam(param, value)
    }

    /**
     * FT-003: 检查参数是否被手动修改
     */
    fun isParamManuallyModified(field: String): Boolean {
        return _manuallyModifiedFields.value.contains(field)
    }

    /**
     * FT-003: 重置为AI推荐状态
     */
    fun resetToAISuggestion() {
        val suggestion = _suggestedParams.value ?: return
        _manuallyModifiedFields.value = emptySet()

        // 重新应用所有建议
        applySelectedSuggestions(suggestion.suggestions.filter { it.isSelected }.map { it.field }.toSet())
    }

    /**
     * FT-003: 清除建议（会话结束）
     */
    fun clearSuggestion() {
        _suggestedParams.value = null
        _manuallyModifiedFields.value = emptySet()
    }

    /**
     * 应用色彩风格
     */
    suspend fun applyColorStyle(styleId: String): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) {
            return@withContext AdjustmentParams()
        }

        _isProcessing.value = true
        delay(200)

        val style = colorStyles.find { it.id == styleId } ?: colorStyles.first()
        val newParams = AdjustmentParams(
            saturation = style.saturation,
            contrast = style.contrast,
            brightness = style.brightness,
            warmth = style.warmth,
            selectedStyleId = styleId
        )

        _currentAdjustments.value = newParams
        clearSuggestion()
        _isProcessing.value = false
        newParams
    }

    /**
     * 应用智能优化预设
     */
    suspend fun applySmartPreset(presetId: String): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) {
            return@withContext AdjustmentParams()
        }

        _isProcessing.value = true
        delay(500)

        val params = when (presetId) {
            "auto_optimize" -> AdjustmentParams(
                saturation = 10, contrast = 8, brightness = 5,
                sharpness = 15, clarity = 10
            )
            "hdr_enhance" -> AdjustmentParams(
                contrast = 20, highlights = -30, shadows = 25, clarity = 15
            )
            "noise_reduce" -> AdjustmentParams(
                noiseReduction = 40, sharpness = -5
            )
            "sharpness" -> AdjustmentParams(
                sharpness = 30, clarity = 20, detail = 15
            )
            "skin_smooth" -> AdjustmentParams(
                skinSmooth = 25, warmth = 5, saturation = -5
            )
            "sky_enhance" -> AdjustmentParams(
                saturation = 20, contrast = 10, highlights = -15, clarity = 10
            )
            "detail_enhance" -> AdjustmentParams(
                sharpness = 25, clarity = 25, detail = 20
            )
            "night_optimize" -> AdjustmentParams(
                contrast = 15, highlights = -15, shadows = 20, noiseReduction = 35
            )
            else -> AdjustmentParams()
        }

        _currentAdjustments.value = params
        clearSuggestion()
        _isProcessing.value = false
        params
    }

    /**
     * 手动调整参数（不脱离AI推荐）
     */
    fun adjustParam(param: AdjustmentType, value: Int) {
        if (!settingsManager.isAIFineTuneEnabled) return

        val current = _currentAdjustments.value
        _currentAdjustments.value = when (param) {
            AdjustmentType.SATURATION -> current.copy(saturation = value)
            AdjustmentType.CONTRAST -> current.copy(contrast = value)
            AdjustmentType.BRIGHTNESS -> current.copy(brightness = value)
            AdjustmentType.WARMTH -> current.copy(warmth = value)
            AdjustmentType.SHARPNESS -> current.copy(sharpness = value)
            AdjustmentType.CLARITY -> current.copy(clarity = value)
            AdjustmentType.HIGHLIGHTS -> current.copy(highlights = value)
            AdjustmentType.SHADOWS -> current.copy(shadows = value)
            AdjustmentType.NOISE_REDUCTION -> current.copy(noiseReduction = value)
            AdjustmentType.SKIN_SMOOTH -> current.copy(skinSmooth = value)
            AdjustmentType.DETAIL -> current.copy(detail = value)
        }
    }

    /**
     * 重置所有调整
     */
    fun resetAdjustments() {
        _currentAdjustments.value = AdjustmentParams()
        clearSuggestion()
    }

    /**
     * 保存当前调整为自定义预设
     */
    fun saveAsPreset(name: String): CustomPreset {
        return CustomPreset(
            id = System.currentTimeMillis().toString(),
            name = name,
            params = _currentAdjustments.value,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * 切换 AI 微调开关
     */
    fun toggleAIFineTune(enabled: Boolean) {
        settingsManager.isAIFineTuneEnabled = enabled
    }

    companion object {
        @Volatile
        private var instance: AIFineTuneManager? = null

        fun getInstance(context: Context): AIFineTuneManager {
            return instance ?: synchronized(this) {
                instance ?: AIFineTuneManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 色彩风格
 */
data class ColorStyle(
    val id: String,
    val name: String,
    val description: String,
    val saturation: Int,
    val contrast: Int,
    val warmth: Int
)

/**
 * 智能优化预设
 */
data class SmartPreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String
)

/**
 * 基础预设（用于AI微调派生）
 */
data class BasePreset(
    val id: String,
    val name: String,
    val params: Map<String, Int>
)

/**
 * AI建议参数
 */
data class AISuggestion(
    val basePresetId: String,
    val basePresetName: String,
    val suggestions: List<ParamSuggestion>,
    val generatedAt: Long
)

/**
 * 单个参数建议
 */
data class ParamSuggestion(
    val field: String,
    val currentValue: Int,
    val suggestedValue: Int,
    val displayName: String,
    val isSelected: Boolean = true
) {
    val difference: Int get() = suggestedValue - currentValue
}

/**
 * 参数对比（用于UI显示）
 */
data class ParamComparison(
    val field: String,
    val displayName: String,
    val currentValue: Int,
    val suggestedValue: Int,
    val difference: Int
)

/**
 * 调整参数
 */
data class AdjustmentParams(
    val saturation: Int = 0,        // 饱和度 -100 ~ +100
    val contrast: Int = 0,          // 对比度 -100 ~ +100
    val brightness: Int = 0,        // 亮度 -100 ~ +100
    val warmth: Int = 0,            // 色温 -100 ~ +100
    val sharpness: Int = 0,         // 锐度 0 ~ 100
    val clarity: Int = 0,            // 清晰度 0 ~ 100
    val highlights: Int = 0,         // 高光 -100 ~ +100
    val shadows: Int = 0,           // 阴影 -100 ~ +100
    val noiseReduction: Int = 0,    // 降噪 0 ~ 100
    val skinSmooth: Int = 0,        // 美肤 0 ~ 100
    val detail: Int = 0,            // 细节 0 ~ 100
    val selectedStyleId: String? = null
)

/**
 * 调整类型
 */
enum class AdjustmentType {
    SATURATION,
    CONTRAST,
    BRIGHTNESS,
    WARMTH,
    SHARPNESS,
    CLARITY,
    HIGHLIGHTS,
    SHADOWS,
    NOISE_REDUCTION,
    SKIN_SMOOTH,
    DETAIL
}

/**
 * 自定义预设
 */
data class CustomPreset(
    val id: String,
    val name: String,
    val params: AdjustmentParams,
    val createdAt: Long
)
