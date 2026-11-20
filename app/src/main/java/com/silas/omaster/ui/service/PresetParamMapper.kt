package com.silas.omaster.ui.service

import android.content.Context
import com.silas.omaster.model.PresetSection
import com.silas.omaster.util.IconFont
import com.silas.omaster.util.PresetI18n

/**
 * FD-03① 悬浮窗参数映射（从 FloatingWindowService 1617 行单类中提取的第一步）：
 * 纯逻辑部分——预设 sections → 悬浮窗参数序列 / 图标映射，不含任何 View 代码。
 *
 * 架构红线（规划 4.9）：此层只读展示预设数据，不修改任何参数值。
 */
object PresetParamMapper {

    // param_* 资源名 → 中文规范文本。悬浮窗收到的 sections 已被 PresetI18n.resolveString
    // 本地化（getDisplaySections），en/ja 下中文关键词 contains 匹配失效；
    // 先把本地化显示文本反查为中文规范文本（如 "Contrast (Highlight)" → "对比度（亮部）"），
    // 使 Realme 判定与参数映射在任何 locale 下与 zh 行为一致。
    private val CANONICAL_PARAM_RESOURCES = listOf(
        "param_contrast_highlight" to "对比度（亮部）",
        "param_contrast_shadow" to "对比度（暗部）",
        "param_contrast" to "对比度",
        "param_clarity" to "清晰",
        "param_grain_size" to "颗粒尺寸",
        "param_grain" to "颗粒强度",
        "param_filter" to "滤镜",
        "param_soft_light" to "柔光",
        "param_tone_curve" to "影调",
        "param_saturation" to "饱和度",
        "param_warm_cool" to "冷暖",
        "param_brightness" to "明暗",
        "param_cyan_magenta" to "青品",
        "param_hue" to "色相",
        "param_sharpness" to "锐度",
        "param_vignette" to "暗角",
        "param_tone" to "色调"
    )

    @Volatile
    private var cachedParamDisplayTexts: Pair<String, List<Pair<String, String>>>? = null

    /** 本地化显示文本 → 中文规范文本（长文本在前，最长优先替换）；按 locale 缓存 */
    private fun paramDisplayTexts(context: Context): List<Pair<String, String>> {
        // ConfigurationCompat 兼容 API 23（Configuration#getLocales 是 API 24+）
        val locales = androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)
        val locale = if (locales.isEmpty) "zh" else locales[0].toString()
        cachedParamDisplayTexts?.let { (l, list) -> if (l == locale) return list }
        val list = CANONICAL_PARAM_RESOURCES.mapNotNull { (resName, canonical) ->
            val resId = context.resources.getIdentifier(resName, "string", context.packageName)
            if (resId != 0) {
                val display = context.getString(resId)
                // zh 下显示文本即规范文本，无需替换
                if (display.isNotEmpty() && display != canonical) display to canonical else null
            } else null
        }.sortedByDescending { it.first.length }
        cachedParamDisplayTexts = locale to list
        return list
    }

    private fun canonicalizeLabel(context: Context, label: String): String {
        // 原始 "@string/..." 引用保持原样（映射表中的资源名关键词可直接命中）
        if (label.startsWith("@string/")) return label
        var result = label
        for ((display, canonical) in paramDisplayTexts(context)) {
            if (display in result) result = result.replace(display, canonical)
        }
        return result
    }

    private fun containsRealmeParams(sections: List<PresetSection>): Boolean {
        sections.forEach { section ->
            section.items.forEach { item ->
                if (item.label.contains("对比度（亮部）") || item.label.contains("对比度（暗部）")) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 检测是否为 Realme 预设（历史数据直存中文标签的兼容入口）
     * Realme 预设特有参数：对比度（亮部）、对比度（暗部）
     */
    fun isRealmePreset(sections: List<PresetSection>): Boolean {
        return containsRealmeParams(sections)
    }

    /**
     * 检测是否为 Realme 预设（locale 感知版本）
     * sections 已本地化时（en/ja），先反查为中文规范文本再判定
     */
    fun isRealmePreset(context: Context, sections: List<PresetSection>): Boolean {
        return containsRealmeParams(
            sections.map { s -> s.copy(items = s.items.map { it.copy(label = canonicalizeLabel(context, it.label)) }) }
        )
    }

    /**
     * 从 sections 提取动态参数序列：List<(value, icon/text)>
     * 根据预设类型（Realme GR / Realme 老版本 / OPPO·一加）自动选择映射表
     */
    fun extractDynamicParams(context: Context, sections: List<PresetSection>): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()

        // en/ja 本地化标签 → 中文规范文本，保证下方关键词匹配跨 locale 生效
        val canonicalSections = sections.map { s ->
            s.copy(items = s.items.map { it.copy(label = canonicalizeLabel(context, it.label)) })
        }

        // 检查是否有Realme特有参数
        var hasRealmeParams = false
        var hasRealmeExtendedParams = false
        canonicalSections.forEach { section ->
            section.items.forEach { item ->
                if (item.label.contains("对比度（亮部）") || item.label.contains("对比度（暗部）")) {
                    hasRealmeParams = true
                }
                // Realme 扩展参数检测（不包含"对比度"，因为OPPO预设也可能有对比度）
                if (item.label.contains("清晰") || item.label.contains("褪色") ||
                    item.label.contains("颗粒") || item.label.contains("颗粒强度")) {
                    hasRealmeExtendedParams = true
                }
            }
        }

        // 根据预设类型选择参数映射
        val labelToIcon = if (hasRealmeParams) {
            // Realme GR预设：12个参数，全部使用文字
            // 参数顺序：滤镜、饱和度、色相、影调、对比度、对比度(亮)、对比度(暗)、锐度、明暗、清晰、颗粒强度、颗粒尺寸
            listOf(
                listOf("滤镜", "Filter") to IconFont.FILTER_TEXT,
                listOf("饱和", "Saturation") to IconFont.SATURATION_TEXT,
                listOf("色相", "Hue") to IconFont.HUE_TEXT,
                listOf("影调", "Tone") to IconFont.TONE_TEXT,
                listOf("对比度", "Contrast") to IconFont.CONTRAST,
                listOf("对比度（亮部）", "对比度（亮）", "contrast_highlight") to IconFont.CONTRAST_HIGHLIGHT_TEXT,
                listOf("对比度（暗部）", "对比度（暗）", "contrast_shadow") to IconFont.CONTRAST_SHADOW_TEXT,
                listOf("锐度", "Sharpness") to IconFont.SHARPNESS_TEXT,
                listOf("明暗", "Brightness") to IconFont.BRIGHTNESS_TEXT,
                listOf("清晰", "Clarity") to IconFont.CLARITY,
                listOf("颗粒", "颗粒强度", "Grain", "grain_intensity") to IconFont.GRAIN_INTENSITY_TEXT,
                listOf("颗粒尺寸", "grain_size") to IconFont.GRAIN_SIZE_TEXT
            )
        } else if (hasRealmeExtendedParams) {
            // Realme预设（老版本）：全部使用文字
            listOf(
                // 基础8参数 - 文字
                listOf("滤镜", "Filter") to IconFont.FILTER_TEXT,
                listOf("柔光", "Soft") to IconFont.SOFT_LIGHT_TEXT,
                listOf("影调", "Tone") to IconFont.TONE_TEXT,
                listOf("饱和", "Saturation") to IconFont.SATURATION_TEXT,
                listOf("冷暖", "Warm") to IconFont.WARM_COOL_TEXT,
                listOf("青品", "Cyan") to IconFont.CYAN_TEXT,
                listOf("锐度", "Sharpness") to IconFont.SHARPNESS_TEXT,
                listOf("暗角", "Vignette") to IconFont.VIGNETTE_TEXT,
                // 扩展3参数 - 文字
                listOf("清晰", "Clarity") to IconFont.CLARITY,
                listOf("对比度", "褪色", "Contrast", "Fade") to IconFont.CONTRAST,
                listOf("颗粒", "颗粒强度", "Grain") to IconFont.GRAIN
            )
        } else {
            // OPPO/一加预设：使用图标
            listOf(
                // 基础8参数 - 图标
                listOf("滤镜", "Filter") to IconFont.FILTER,
                listOf("柔光", "Soft") to IconFont.SOFT_LIGHT,
                listOf("影调", "Tone") to IconFont.TONE,
                listOf("饱和", "Saturation") to IconFont.SATURATION,
                listOf("冷暖", "Warm") to IconFont.WARM_COOL,
                listOf("青品", "Cyan") to IconFont.CYAN,
                listOf("锐度", "Sharpness") to IconFont.SHARPNESS,
                listOf("暗角", "Vignette") to IconFont.VIGNETTE,
                // 扩展3参数 - 文字
                listOf("清晰", "Clarity") to IconFont.CLARITY,
                listOf("对比度", "褪色", "Contrast", "Fade") to IconFont.CONTRAST,
                listOf("颗粒", "颗粒强度", "Grain") to IconFont.GRAIN
            )
        }

        // 按顺序检查每个参数是否存在
        labelToIcon.forEach { (labels, icon) ->
            var found = false
            var value = "-"

            canonicalSections.forEach { section ->
                section.items.forEach { item ->
                    labels.forEach { label ->
                        if (!found && item.label.contains(label)) {
                            value = PresetI18n.resolveValue(context, item.value)
                            found = true
                        }
                    }
                }
            }

            // 只添加实际存在的参数（值不为"-"）
            if (found && value != "-") {
                result.add(value to icon)
            }
        }

        return result
    }

    fun getIconForLabel(label: String): String {
        return when {
            label.contains("滤镜") || label.contains("Filter") -> IconFont.FILTER
            label.contains("柔光") || label.contains("Soft") -> IconFont.SOFT_LIGHT
            label.contains("影调") || label.contains("Tone") -> IconFont.TONE
            label.contains("饱和") || label.contains("Saturation") -> IconFont.SATURATION
            label.contains("冷暖") || label.contains("Warm") -> IconFont.WARM_COOL
            label.contains("青品") || label.contains("Cyan") -> IconFont.CYAN
            label.contains("锐度") || label.contains("Sharpness") -> IconFont.SHARPNESS
            label.contains("暗角") || label.contains("Vignette") -> IconFont.VIGNETTE
            label.contains("白平衡") || label.contains("WB") -> "🌡️"
            label.contains("曝光") || label.contains("EV") -> "☀️"
            label.contains("ISO") -> "📸"
            label.contains("快门") || label.contains("Shutter") -> "⏱️"
            label.contains("建议") || label.contains("Tips") -> "💡"
            else -> "⚙️"
        }
    }
}
