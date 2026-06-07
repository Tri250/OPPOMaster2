package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
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

/**
 * AI 微调管理器
 * 色彩风格、参数调整控制
 */
class AIFineTuneManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    
    private val _currentAdjustments = MutableStateFlow(AdjustmentParams())
    val currentAdjustments: StateFlow<AdjustmentParams> = _currentAdjustments.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // 预设色彩风格
    val colorStyles = listOf(
        ColorStyle(
            id = "natural",
            name = "自然",
            description = "还原真实色彩",
            saturation = 0,
            contrast = 0,
            warmth = 0
        ),
        ColorStyle(
            id = "vivid",
            name = "鲜艳",
            description = "增强色彩饱和度",
            saturation = 20,
            contrast = 10,
            warmth = 5
        ),
        ColorStyle(
            id = "film",
            name = "胶片",
            description = "复古胶片质感",
            saturation = -10,
            contrast = 15,
            warmth = 10
        ),
        ColorStyle(
            id = "bw",
            name = "黑白",
            description = "经典黑白影调",
            saturation = -100,
            contrast = 20,
            warmth = 0
        ),
        ColorStyle(
            id = "warm",
            name = "暖调",
            description = "温暖色调风格",
            saturation = 5,
            contrast = 0,
            warmth = 25
        ),
        ColorStyle(
            id = "cool",
            name = "冷调",
            description = "清冷色调风格",
            saturation = 5,
            contrast = 5,
            warmth = -20
        ),
        ColorStyle(
            id = "portrait",
            name = "人像",
            description = "优化肤色表现",
            saturation = 10,
            contrast = -5,
            warmth = 8
        ),
        ColorStyle(
            id = "landscape",
            name = "风景",
            description = "增强自然色彩",
            saturation = 15,
            contrast = 10,
            warmth = 0
        )
    )

    // 智能优化预设
    val smartPresets = listOf(
        SmartPreset(
            id = "auto_optimize",
            name = "智能优化",
            description = "AI 自动分析并优化图片",
            icon = "✨"
        ),
        SmartPreset(
            id = "hdr_enhance",
            name = "HDR 增强",
            description = "提升动态范围",
            icon = "🌅"
        ),
        SmartPreset(
            id = "noise_reduce",
            name = "降噪处理",
            description = "减少画面噪点",
            icon = "🔇"
        ),
        SmartPreset(
            id = "sharpness",
            name = "清晰度",
            description = "增强细节锐度",
            icon = "🔍"
        ),
        SmartPreset(
            id = "skin_smooth",
            name = "肤色优化",
            description = "自然美肤效果",
            icon = "✨"
        ),
        SmartPreset(
            id = "sky_enhance",
            name = "天空增强",
            description = "优化天空色彩",
            icon = "☁️"
        )
    )

    /**
     * 应用色彩风格
     * @param styleId 风格ID
     */
    suspend fun applyColorStyle(styleId: String): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) {
            return@withContext AdjustmentParams()
        }

        _isProcessing.value = true
        delay(200) // 模拟处理

        val style = colorStyles.find { it.id == styleId } ?: colorStyles.first()
        val newParams = AdjustmentParams(
            saturation = style.saturation,
            contrast = style.contrast,
            warmth = style.warmth,
            selectedStyleId = styleId
        )
        
        _currentAdjustments.value = newParams
        _isProcessing.value = false
        newParams
    }

    /**
     * 应用智能优化
     * @param presetId 预设ID
     * @param bitmap 待处理图片
     */
    suspend fun applySmartPreset(presetId: String, bitmap: Bitmap): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) {
            return@withContext AdjustmentParams()
        }

        _isProcessing.value = true
        delay(500) // 模拟 AI 处理时间

        val params = when (presetId) {
            "auto_optimize" -> AdjustmentParams(
                saturation = 10,
                contrast = 8,
                brightness = 5,
                sharpness = 15,
                clarity = 10
            )
            "hdr_enhance" -> AdjustmentParams(
                contrast = 20,
                highlights = -30,
                shadows = 25,
                clarity = 15
            )
            "noise_reduce" -> AdjustmentParams(
                noiseReduction = 40,
                sharpness = -5
            )
            "sharpness" -> AdjustmentParams(
                sharpness = 30,
                clarity = 20,
                detail = 15
            )
            "skin_smooth" -> AdjustmentParams(
                skinSmooth = 25,
                warmth = 5,
                saturation = -5
            )
            "sky_enhance" -> AdjustmentParams(
                saturation = 20,
                contrast = 10,
                highlights = -15,
                clarity = 10
            )
            else -> AdjustmentParams()
        }

        _currentAdjustments.value = params
        _isProcessing.value = false
        params
    }

    /**
     * 手动调整参数
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
     * 实时预览调整效果
     */
    fun previewAdjustment(bitmap: Bitmap): Flow<Bitmap> = flow {
        if (!settingsManager.isAIFineTuneEnabled) {
            emit(bitmap)
            return@flow
        }

        // 模拟实时预览处理
        delay(50)
        emit(bitmap) // 实际项目中应返回处理后的图片
    }.flowOn(Dispatchers.Default)

    /**
     * 重置所有调整
     */
    fun resetAdjustments() {
        _currentAdjustments.value = AdjustmentParams()
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
 * 调整参数
 */
data class AdjustmentParams(
    val saturation: Int = 0,        // 饱和度 -100 ~ +100
    val contrast: Int = 0,          // 对比度 -100 ~ +100
    val brightness: Int = 0,        // 亮度 -100 ~ +100
    val warmth: Int = 0,            // 色温 -100 ~ +100
    val sharpness: Int = 0,         // 锐度 0 ~ 100
    val clarity: Int = 0,           // 清晰度 0 ~ 100
    val highlights: Int = 0,        // 高光 -100 ~ +100
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
