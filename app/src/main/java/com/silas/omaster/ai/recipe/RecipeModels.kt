package com.silas.omaster.ai.recipe

import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SoftLightMode

/**
 * 摄影配方数据模型
 * 将 photodesign-skill 的 53 套案例配方映射为手机实拍可用的参数配置
 */
data class PhotographyRecipe(
    val id: String,
    val name: String,
    val category: String,
    val intentKeywords: List<String>,
    val equivalentEquipment: EquivalentEquipment,
    val phoneShootingGuide: PhoneShootingGuide,
    val hasselbladPreset: HasselbladPresetJson,
    val lutRecommendation: LUTRecommendation?,
    val arGuideLine: String,
    val compositionTip: String,
    val lightingTip: String,
    val avoidTips: List<String>
) {
    /**
     * 转换为实际的 HasselbladParams 供系统使用
     */
    fun toHasselbladParams(): HasselbladParams = HasselbladParams(
        tone = hasselbladPreset.tone,
        saturation = hasselbladPreset.saturation,
        contrast = hasselbladPreset.contrast,
        colorTemp = hasselbladPreset.colorTemp,
        sharpness = hasselbladPreset.sharpness,
        vignette = hasselbladPreset.vignette,
        cyanMagenta = hasselbladPreset.cyanMagenta,
        softLight = hasselbladPreset.softLight,
        highlights = hasselbladPreset.highlights,
        shadows = hasselbladPreset.shadows,
        clarity = hasselbladPreset.clarity
    )
}

data class EquivalentEquipment(
    val camera: String,
    val lens: String,
    val focalLength: String,
    val aperture: String
)

data class PhoneShootingGuide(
    val zoomRatio: Double,
    val iso: String,
    val shutter: String,
    val whiteBalance: String,
    val exposureCompensation: Double
)

/**
 * JSON 中的预设参数，与 HasselbladParams 字段对齐
 */
data class HasselbladPresetJson(
    val tone: Int,
    val saturation: Int,
    val contrast: Int,
    val colorTemp: Int,
    val sharpness: Int,
    val vignette: Int,
    val cyanMagenta: Int,
    val softLight: SoftLightMode,
    val highlights: Int,
    val shadows: Int,
    val clarity: Int
)

data class LUTRecommendation(
    val id: String,
    val strength: Double
)

/**
 * 配方索引配置
 */
data class RecipeIndex(
    val version: Int,
    val files: List<String>,
    val categories: List<RecipeCategory>
)

data class RecipeCategory(
    val id: String,
    val name: String,
    val file: String,
    val count: Int,
    val icon: String
)

/**
 * 意图匹配结果
 */
data class RecipeMatchResult(
    val recipe: PhotographyRecipe,
    val matchScore: Int,
    val matchReason: String
)
