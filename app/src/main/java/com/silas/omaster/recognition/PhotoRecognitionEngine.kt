package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetDescription
import com.silas.omaster.util.PhotoParamParser
import java.util.Locale

/**
 * 照片参数识别引擎（核心为纯 JVM 逻辑，可单测）。
 *
 * 整合 EXIF 摄影参数 + 像素风格向量 → 生成 [RecognitionResult]：
 * - MasterPreset 草稿：摄影参数（光圈/快门/ISO/色温/EV）来自 EXIF，调色参数来自风格匹配；
 * - 综合置信度：风格匹配置信度为主，EXIF 完整性小幅加权；
 * - 口径冲突处理：EXIF 色温与像素冷暖矛盾时，以像素风格为准，EXIF 作为附注 warning；
 * - 禁止输出 PII：不写入 GPS/设备序列号，sourceLabel 仅保留机型营销名。
 *
 * 对应 PRD FR-01~05、8.3/8.5。
 */
object PhotoRecognitionEngine {

    const val ENGINE_VERSION = "1.0.0"

    /** 从内部输入生成识别结果 */
    fun recognize(input: RecognitionInput): RecognitionResult {
        val candidates = input.styleAnalysis.matches
        val top = candidates.firstOrNull()
            ?: StyleAnalyzer.Match(
                StyleCatalog.byKey("original")!!,
                0.5f,
                "样本特征不足，采用中性风格"
            )

        val warnings = mutableListOf<String>()
        if (input.styleAnalysis.compressed) warnings.add("图片较大，已降采样分析")
        if (!input.hasExif) warnings.add("照片无 EXIF 摄影参数，参数项需手动补充")
        detectConflict(input, top.preset)?.let { warnings.add(it) }

        // 综合置信度：风格匹配为主（0.85），EXIF 完整度加权（0.15）
        val styleConf = top.confidence
        val exifCompleteness = computeExifCompleteness(input)
        val confidence = (styleConf * 0.85f + exifCompleteness * 0.15f).coerceIn(0f, 1f)

        val source = if (input.hasExif) RecognitionSource.PHOTO_EXIF else RecognitionSource.PHOTO_STYLE
        val presetName = buildPresetName(input, top.preset)
        val draft = buildDraft(input, top.preset, presetName, source)

        val explanation = buildString {
            append("风格依据：").append(top.reason).append("。")
            if (input.hasExif) {
                append("摄影参数来自照片 EXIF")
                input.sourceLabel?.let { append("（来源：").append(it).append("）") }
                append("。")
            }
            append("综合置信度 ").append(String.format(Locale.getDefault(), "%.0f%%", confidence * 100))
        }

        return RecognitionResult(
            draft = draft,
            candidates = candidates,
            confidence = confidence,
            warnings = warnings,
            source = source,
            engineVersion = ENGINE_VERSION,
            catalogVersion = StyleCatalog.CATALOG_VERSION,
            explanation = explanation
        )
    }

    /** 构造 MasterPreset 草稿：摄影参数填 EXIF，调色参数填风格模板 */
    private fun buildDraft(
        input: RecognitionInput,
        style: StylePreset,
        name: String,
        source: RecognitionSource
    ): MasterPreset {
        val wbLabel = input.whiteBalance
        val temp = input.colorTemperature
        // 色温：优先 EXIF 数值；无则留空由用户补（像素冷暖作为风格参数体现，不伪造 EXIF 色温）
        return MasterPreset(
            name = name,
            coverPath = "", // 走占位图，识别结果不内嵌原图（隐私）
            author = "@照片识别",
            mode = "auto",
            filter = style.filter,
            whiteBalance = temp?.let { "${it}K" } ?: wbLabel,
            colorTemperature = temp,
            exposureCompensation = input.exposureBias,
            iso = input.iso,
            shutterSpeed = input.shutterSpeed,
            softLight = style.softLight,
            tone = style.tone,
            saturation = style.saturation,
            warmCool = style.warmCool,
            cyanMagenta = style.cyanMagenta,
            sharpness = style.sharpness,
            vignette = style.vignette,
            isCustom = true,
            tags = buildTags(input, style, source),
            description = PresetDescription(
                title = "由照片识别生成",
                content = "参数来源：${if (source == RecognitionSource.PHOTO_EXIF) "照片 EXIF + 风格模拟" else "像素风格模拟"}。该预设为风格模拟，非哈苏原厂算法。"
            )
        )
    }

    /** EXIF 完整度 0..1（7 项摄影参数填充比例） */
    private fun computeExifCompleteness(input: RecognitionInput): Float {
        if (!input.hasExif) return 0f
        val fields = listOf(input.aperture, input.shutterSpeed, input.iso, input.whiteBalance,
            input.exposureBias, input.colorTemperature?.toString(), input.focusMm)
        val filled = fields.count { !it.isNullOrBlank() }
        return filled.toFloat() / fields.size
    }

    /** EXIF 色温(冷暖)与像素冷暖矛盾检测（口径冲突） */
    private fun detectConflict(input: RecognitionInput, top: StylePreset): String? {
        val exifTemp = input.colorTemperature ?: return null
        // 色温越高越暖（K 大偏暖是拍摄常见的"高色温补偿"语义，这里用相对中性 5500K 判断）
        val exifWarm = exifTemp > 6000
        val styleWarm = top.target.warmth > 0.55f
        return if (exifWarm != styleWarm) {
            "EXIF 色温(${exifTemp}K)与画面冷暖倾向不一致，已以画面风格为准，色温仅作参考"
        } else null
    }

    private fun buildPresetName(input: RecognitionInput, style: StylePreset): String {
        val base = input.sourceLabel?.take(20)?.let { "$it · " } ?: ""
        return "${base}${style.displayName}风格".take(40)
    }

    private fun buildTags(input: RecognitionInput, style: StylePreset, source: RecognitionSource): List<String> {
        return buildList {
            add("照片识别")
            add(style.displayName)
            add(if (source == RecognitionSource.PHOTO_EXIF) "含EXIF" else "纯风格")
            if (input.hasHasselbladHint()) add("哈苏样片风格参考")
        }
    }

    private fun RecognitionInput.hasHasselbladHint(): Boolean =
        sourceLabel?.contains("哈苏", ignoreCase = true) == true ||
        sourceLabel?.contains("hasselblad", ignoreCase = true) == true
}
