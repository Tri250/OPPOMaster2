package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset

/** 识别来源 */
enum class RecognitionSource {
    PHOTO_EXIF,   // 有摄影参数 + 风格
    PHOTO_STYLE   // 无 EXIF，纯风格识别
}

/**
 * 画面特征标签（引擎输出的结构化特征，展示文案本地化交由 UI 层）
 */
enum class FeatureTag {
    /** 明亮 */
    BRIGHT,
    /** 偏暗 */
    DARK,
    /** 低饱和 */
    LOW_SAT,
    /** 鲜艳 */
    VIVID,
    /** 偏暖 */
    WARM,
    /** 偏冷 */
    COOL,
    /** 高动态范围 */
    WIDE_DR
}

/**
 * 引擎警告类型（展示文案本地化交由 UI 层）
 */
enum class WarningType {
    /** 图片较大，已降采样分析 */
    COMPRESSED,
    /** 照片无 EXIF 摄影参数 */
    NO_EXIF,
    /** EXIF 色温与画面冷暖倾向不一致（以画面风格为准） */
    TEMP_CONFLICT,
    /** 样本特征不足，采用中性风格 */
    INSUFFICIENT_SAMPLES
}

/** 结构化警告（value：TEMP_CONFLICT 时附带色温 K） */
data class RecognitionWarning(
    val type: WarningType,
    val value: Int? = null
)

/**
 * 识别结果契约（对应 PRD 8.3 输出契约）。
 *
 * 引擎只输出结构化数据与阈值，不含任何展示层文案（本地化交由 UI 层）。
 *
 * @param draft 识别生成的 MasterPreset 草稿（未落库，需用户确认）
 * @param candidates Top-3 候选风格
 * @param confidence 综合置信度 0..1
 * @param warnings 警示信息（口径冲突、无 EXIF、已压缩等）
 * @param source 识别来源（PHOTO_EXIF / PHOTO_STYLE）
 * @param engineVersion 规则引擎版本（可复现）
 * @param catalogVersion 风格目录版本（可复现）
 * @param features 画面特征标签（构成"可解释依据"）
 * @param styleVector 画面风格特征向量（亮度/对比/饱和/冷暖/动态/色彩，供 UI 展示原始数值）
 * @param sourceLabel 机型来源标签（如 "vivo X7 · 哈苏"），构成"摄影参数来源"说明
 * @param aperture EXIF 光圈（如 "f/1.6"）；手机大师模式无光圈参数项，仅作识别依据展示，不落入预设
 * @param focalLength EXIF 焦距（如 "23mm"）；同上，仅作识别依据展示
 */
data class RecognitionResult(
    val draft: MasterPreset,
    val candidates: List<StyleAnalyzer.Match>,
    val confidence: Float,
    val warnings: List<RecognitionWarning>,
    val source: RecognitionSource,
    val engineVersion: String,
    val catalogVersion: String,
    val features: List<FeatureTag> = emptyList(),
    val styleVector: StyleVector? = null,
    val sourceLabel: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null
) {
    /** 是否可直接采用（PRD：confidence ≥0.7 为强匹配） */
    val isStrongMatch: Boolean get() = confidence >= 0.7f

    /** 置信度等级（0=强 / 1=中 / 2=弱；展示文案本地化交由 UI 层） */
    val confidenceLevel: Int
        get() = when {
            confidence >= 0.7f -> 0
            confidence >= 0.4f -> 1
            else -> 2
        }
}

/** 识别引擎内部输入（解耦 Android Uri，便于测试） */
data class RecognitionInput(
    val hasExif: Boolean,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val whiteBalance: String? = null,
    val colorTemperature: Int? = null,
    val exposureBias: String? = null,
    val focusMm: String? = null,
    val sourceLabel: String? = null,     // 机型来源，如 "Find X7 · 哈苏"
    val styleAnalysis: StyleAnalyzer.Analysis
)
