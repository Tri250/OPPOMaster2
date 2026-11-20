package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset

/**
 * 识别结果契约（对应 PRD 8.3 输出契约）。
 *
 * @param draft 识别生成的 MasterPreset 草稿（未落库，需用户确认）
 * @param candidates Top-3 候选风格
 * @param confidence 综合置信度 0..1
 * @param warnings 警示信息（口径冲突、无 EXIF、已压缩等）
 * @param source 识别来源（PHOTO_EXIF / PHOTO_STYLE）
 * @param engineVersion 规则引擎版本（可复现）
 * @param catalogVersion 风格目录版本（可复现）
 */
data class RecognitionResult(
    val draft: MasterPreset,
    val candidates: List<StyleAnalyzer.Match>,
    val confidence: Float,
    val warnings: List<String>,
    val source: RecognitionSource,
    val engineVersion: String,
    val catalogVersion: String,
    /** 综合可解释依据 */
    val explanation: String
) {
    /** 是否可直接采用（PRD：confidence ≥0.7 为强匹配） */
    val isStrongMatch: Boolean get() = confidence >= 0.7f

    /** 置信度等级文案 */
    val confidenceLabel: String
        get() = when {
            confidence >= 0.7f -> "可直接采用"
            confidence >= 0.4f -> "建议微调后采用"
            else -> "仅供参考"
        }
}

enum class RecognitionSource {
    PHOTO_EXIF,   // 有摄影参数 + 风格
    PHOTO_STYLE   // 无 EXIF，纯风格识别
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
