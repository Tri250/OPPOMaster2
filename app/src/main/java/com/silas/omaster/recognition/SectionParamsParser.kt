package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetSection

/**
 * 预设 sections（展示层参数）→ MasterPreset typed 字段（引擎消费层）桥接。
 *
 * 【背景 / 修复 P0】
 * 内置预设（assets/presets.json）与应用内自建预设的调色参数只落在 `sections`
 * 展示字段里（如 `label=@string/param_filter, value="复古 75%"`），而 POP 即拍
 * 引擎（[PresetColorTransform] / [PresetCapturePlan] / [PresetPixelEffects]）
 * 只消费 typed 字段（filter/saturation/warmCool/…）。两者之间原本没有任何桥接，
 * 导致 25/25 内置预设进入 POP 即拍后色彩矩阵恒为单位矩阵、暗角/锐度恒 0——
 * 即"拍了个原图"却提示"已套用风格"。
 *
 * 本解析器把 sections 归一化为 typed 字段（仅填充空值，不覆盖已有 typed 字段，
 * 照片识别草稿 / 截图导入预设自带 typed 字段，不受影响）。
 *
 * 纯 JVM 逻辑（不依赖 Android Context），可单测；label 支持三种写法：
 * - 资源引用 "@string/param_filter"（内置预设 / Realme 预设）
 * - 原始中文 "滤镜"（旧版自定义预设 / 截图导入 sections）
 * - 英 / 日文标签（Filter / フィルター 等，与 strings.xml 对齐）
 */
object SectionParamsParser {

    /** label（或资源引用名）→ 规范化参数键 */
    fun labelKey(label: String?): String? {
        val raw = label?.trim()?.removePrefix("@string/") ?: return null
        if (raw.isEmpty()) return null
        // 资源名直配（param_filter → filter）
        KEY_BY_RES_NAME[raw]?.let { return it }
        // 原始文案别名（中 / 英 / 日）
        return KEY_BY_LABEL[raw]
    }

    /**
     * 用 sections 补全预设的 typed 字段。
     * @return 补全后的预设副本（无 sections 或无需补全时返回原对象）
     */
    fun enrich(preset: MasterPreset): MasterPreset {
        val sections = preset.sections ?: return preset
        if (sections.isEmpty()) return preset

        var result = preset
        for (section in sections) {
            for (item in section.items) {
                result = applyItem(result, item.label, item.value)
            }
        }
        return result
    }

    private fun applyItem(preset: MasterPreset, label: String?, value: String?): MasterPreset {
        val key = labelKey(label) ?: return preset
        val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return preset
        return when (key) {
            "filter" -> preset.copy(filter = preset.filter ?: v)
            "soft_light" -> preset.copy(softLight = preset.softLight ?: v)
            "tone_curve" -> preset.copy(tone = preset.tone ?: parseIntOrNullSigned(v))
            "saturation" -> preset.copy(saturation = preset.saturation ?: parseIntOrNullSigned(v))
            "warm_cool" -> preset.copy(warmCool = preset.warmCool ?: parseIntOrNullSigned(v))
            "cyan_magenta" -> preset.copy(cyanMagenta = preset.cyanMagenta ?: parseIntOrNullSigned(v))
            "sharpness" -> preset.copy(sharpness = preset.sharpness ?: parseIntOrNullSigned(v))
            "tone" -> preset.copy(colorHue = preset.colorHue ?: parseIntOrNullSigned(v))
            "vignette" -> preset.copy(vignette = preset.vignette ?: v)
            "iso" -> preset.copy(iso = preset.iso ?: v)
            "shutter" -> preset.copy(shutterSpeed = preset.shutterSpeed ?: v)
            "exposure" -> preset.copy(exposureCompensation = preset.exposureCompensation ?: v)
            "color_temp" -> preset.copy(colorTemperature = preset.colorTemperature ?: parseKelvin(v))
            "white_balance" -> preset.copy(whiteBalance = preset.whiteBalance ?: v)
            // Realme 扩展参数（hue/contrast/brightness/clarity/grain…）暂无引擎字段，跳过
            else -> preset
        }
    }

    /** "+20" / "-5" / "3" / "+0" → Int；不可解析 → null */
    fun parseIntOrNullSigned(value: String?): Int? {
        val v = value?.trim()?.removePrefix("+") ?: return null
        if (v.isEmpty()) return null
        return v.toIntOrNull()
    }

    /** "5200K" / "5200 k" / "5200" → Int；不可解析 → null */
    fun parseKelvin(value: String?): Int? {
        val v = value?.trim()?.removeSuffix("K")?.removeSuffix("k")?.trim() ?: return null
        if (v.isEmpty()) return null
        return v.toIntOrNull()?.takeIf { it > 0 }
    }

    // ── label 映射表（资源名 / 中 / 英 / 日） ──

    private val KEY_BY_RES_NAME = mapOf(
        "param_filter" to "filter",
        "param_soft_light" to "soft_light",
        "param_tone_curve" to "tone_curve",
        "param_saturation" to "saturation",
        "param_warm_cool" to "warm_cool",
        "param_cyan_magenta" to "cyan_magenta",
        "param_sharpness" to "sharpness",
        "param_vignette" to "vignette",
        "param_iso" to "iso",
        "param_shutter" to "shutter",
        "param_exposure" to "exposure",
        "param_color_temp" to "color_temp",
        "param_tone" to "tone",
        "param_white_balance" to "white_balance",
    )

    private val KEY_BY_LABEL: Map<String, String> = buildMap {
        // 中文（旧版自定义预设 / 截图录入）
        mapOf(
            "滤镜" to "filter",
            "柔光" to "soft_light",
            "影调" to "tone_curve",
            "饱和度" to "saturation",
            "冷暖" to "warm_cool",
            "青品" to "cyan_magenta",
            "锐度" to "sharpness",
            "暗角" to "vignette",
            "快门" to "shutter",
            "曝光补偿" to "exposure",
            "曝光" to "exposure",
            "色温" to "color_temp",
            "色调" to "tone",
            "白平衡" to "white_balance",
        ).forEach { (k, v) -> put(k, v) }
        put("ISO", "iso")
        put("iso", "iso")
        // 英文（values-en/strings.xml）
        mapOf(
            "Filter" to "filter",
            "Soft Light" to "soft_light",
            "Tone Curve" to "tone_curve",
            "Saturation" to "saturation",
            "Warm/Cool" to "warm_cool",
            "Cyan/Magenta" to "cyan_magenta",
            "Sharpness" to "sharpness",
            "Vignette" to "vignette",
            "Shutter" to "shutter",
            "Exposure Comp." to "exposure",
            "Exposure Compensation" to "exposure",
            "Color Temp" to "color_temp",
            "Tone" to "tone",
            "White Balance" to "white_balance",
        ).forEach { (k, v) -> put(k, v) }
        // 日文（values-ja/strings.xml）
        mapOf(
            "フィルター" to "filter",
            "ソフトライト" to "soft_light",
            "トーンカーブ" to "tone_curve",
            "彩度" to "saturation",
            "暖色/寒色" to "warm_cool",
            "シアン/マゼンタ" to "cyan_magenta",
            "シャープネス" to "sharpness",
            "ビネット" to "vignette",
            "シャッター" to "shutter",
            "露出補正" to "exposure",
            "色温度" to "color_temp",
            "トーン" to "tone",
            "ホワイトバランス" to "white_balance",
        ).forEach { (k, v) -> put(k, v) }
    }
}
