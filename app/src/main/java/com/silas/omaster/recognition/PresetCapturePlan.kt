package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset

/**
 * 预设参数应用状态：
 * - WRITTEN：已真实写入拍摄请求（Camera2 Interop，随照片传感器数据生效）；
 * - POST：拍后套用（色彩矩阵/像素效果，成片生效）；
 * - UNSUPPORTED：设备不支持或取值无法解析，附原因。
 */
enum class ApplyStatus { WRITTEN, POST, UNSUPPORTED }

/** 无法套用的原因（UI 层映射为本地化文案） */
enum class UnsupportedReason {
    /** 设备不支持关闭自动曝光（无手动曝光能力） */
    MANUAL_EXPOSURE_UNAVAILABLE,
    /** 手动曝光需 ISO 与快门同时提供，预设缺其一 */
    NEED_ISO_SHUTTER_PAIR,
    /** 手动曝光激活时 ISO/快门已完全控制曝光，曝光补偿不再适用（非设备能力缺失） */
    MANUAL_EXPOSURE_OVERRIDES,
    /** 设备不支持曝光补偿 */
    EV_UNAVAILABLE,
    /** 预设取值无法解析 */
    VALUE_UNPARSEABLE,
    /** 滤镜名不在引擎滤镜表内（含未录入的新滤镜名），无法套用 */
    FILTER_UNRECOGNIZED
}

/** 预设参数标识（UI 层映射为本地化标签） */
enum class ParamKey {
    ISO, SHUTTER, EV, COLOR_TEMP, FILTER, SATURATION, TONE,
    WARM_COOL, CYAN_MAGENTA, SOFT_LIGHT, SHARPNESS, VIGNETTE, COLOR_HUE
}

data class ParamStatus(
    val key: ParamKey,
    /** 展示值（取预设原始写法，如 "200-400"、"1/125"） */
    val value: String,
    val status: ApplyStatus,
    val reason: UnsupportedReason? = null
)

/**
 * 拍摄计划：随拍摄请求写入的传感器参数（已按设备能力裁剪）。
 * @param manualExposure true 表示已关闭自动曝光，ISO/快门完全由本计划控制
 */
data class CapturePlan(
    val iso: Int? = null,
    val shutterNs: Long? = null,
    val evSteps: Int? = null,
    val params: List<ParamStatus> = emptyList()
) {
    val manualExposure: Boolean get() = iso != null && shutterNs != null
}

/**
 * 设备手动能力范围（由 CameraCharacteristics 提取，纯数据便于单测）。
 */
data class ManualRanges(
    val isoRange: IntRange? = null,
    val exposureTimeRange: LongRange? = null,
    /** 曝光补偿步进索引范围（设备以"步"为单位，非 EV） */
    val evStepRange: IntRange? = null,
    /** 每步代表的 EV 值（如 1/6EV → 0.1666f），用于 EV→步数换算 */
    val evStepEv: Float? = null,
    val aeModeOffSupported: Boolean = false
) {
    companion object {
        val NONE = ManualRanges()
    }
}

/**
 * 预设 → 拍摄计划（纯 JVM，可单测）。
 *
 * 分流策略：
 * - ISO/快门：设备支持关闭自动曝光且预设成对提供时，写入拍摄请求（WRITTEN）；
 * - 曝光补偿：保留自动曝光时按设备步进写入；
 * - 滤镜/饱和度/影调/冷暖/青品/柔光/锐度/暗角/色温/色调：拍后套用（POST）；
 * - 其余情况给出 UNSUPPORTED 与原因，UI 如实展示，不做"假装已套用"。
 */
object PresetCapturePlan {

    fun build(preset: MasterPreset, ranges: ManualRanges): CapturePlan {
        val params = mutableListOf<ParamStatus>()

        val isoValue = parseIso(preset.iso)
        val shutterValue = parseShutterNs(preset.shutterSpeed)
        val canManual = ranges.aeModeOffSupported &&
            ranges.isoRange != null && ranges.exposureTimeRange != null
        val wantManual = canManual && isoValue != null && shutterValue != null

        val captureIso = if (wantManual) isoValue!!.coerceIn(ranges.isoRange!!) else null
        val captureShutter = if (wantManual) shutterValue!!.coerceIn(ranges.exposureTimeRange!!) else null

        val evValue = PresetColorTransform.parseExposureEv(preset.exposureCompensation)
        val steps = evToSteps(evValue, ranges)
        val captureEv = if (!wantManual && steps != null) steps else null

        // ISO
        if (preset.iso != null) {
            params += when {
                captureIso != null -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.WRITTEN)
                !canManual -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.MANUAL_EXPOSURE_UNAVAILABLE)
                isoValue == null -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
                else -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.NEED_ISO_SHUTTER_PAIR)
            }
        }

        // 快门
        if (preset.shutterSpeed != null) {
            params += when {
                captureShutter != null -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.WRITTEN)
                !canManual -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.MANUAL_EXPOSURE_UNAVAILABLE)
                shutterValue == null -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
                else -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.NEED_ISO_SHUTTER_PAIR)
            }
        }

        // 曝光补偿：手动曝光已包含曝光控制，AE 开启时按步进写入；EV=0 视为无需套用
        val evRaw = preset.exposureCompensation
        if (evRaw != null && evValue != 0f) {
            params += when {
                captureEv != null -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.WRITTEN)
                // 手动曝光下 EV 由 ISO/快门接管：如实标记"不适用"且归因正确，不再误报设备能力缺失
                wantManual -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.MANUAL_EXPOSURE_OVERRIDES)
                steps == null -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.EV_UNAVAILABLE)
                else -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
            }
        }

        // 拍后套用项（与设备能力无关，恒可套用）。
        // 【v1.8.0 审计修复】门控口径与 EV（上方 evValue != 0f）及
        // [PresetColorTransform.hasVisibleEffect] 对齐：取值存在但效果中性
        // （饱和度 0 / 色温 5500K / 柔光"无"等）不产生芯片——
        // 否则会显示"0 · 拍后套用"这类"假装已套用"的误导信息，
        // 违背本类"UI 如实展示，不做假装已套用"的设计契约。
        fun post(key: ParamKey, value: String?, effective: Boolean) {
            if (effective && value != null) params += ParamStatus(key, value, ApplyStatus.POST)
        }
        post(ParamKey.COLOR_TEMP, preset.colorTemperature?.let { "${it}K" },
            PresetColorTransform.colorTempWarmDelta(preset.colorTemperature) != 0f)
        post(ParamKey.FILTER, preset.filter, isFilterApplicable(preset.filter))
        post(ParamKey.SATURATION, formatSigned(preset.saturation), (preset.saturation ?: 0) != 0)
        post(ParamKey.TONE, formatSigned(preset.tone), (preset.tone ?: 0) != 0)
        post(ParamKey.WARM_COOL, formatSigned(preset.warmCool), (preset.warmCool ?: 0) != 0)
        post(ParamKey.CYAN_MAGENTA, formatSigned(preset.cyanMagenta), (preset.cyanMagenta ?: 0) != 0)
        post(ParamKey.SOFT_LIGHT, preset.softLight,
            PresetColorTransform.parseSoftLight(preset.softLight) > 0f)
        post(ParamKey.SHARPNESS, preset.sharpness?.toString(),
            PresetColorTransform.sharpnessAmount(preset) > 0f)
        post(ParamKey.VIGNETTE, preset.vignette,
            PresetColorTransform.vignetteStrength(preset) > 0f)
        post(ParamKey.COLOR_HUE, formatSigned(preset.colorHue),
            PresetColorTransform.colorHueCyanMagentaDelta(preset.colorHue) != 0f)

        // 滤镜名未识别（引擎滤镜表外）：如实标记，绝不"假装 POST"
        val filterValue = preset.filter
        if (filterValue != null && !isFilterApplicable(filterValue)) {
            params += ParamStatus(
                ParamKey.FILTER, filterValue, ApplyStatus.UNSUPPORTED,
                reason = UnsupportedReason.FILTER_UNRECOGNIZED
            )
        }

        return CapturePlan(
            iso = captureIso,
            shutterNs = captureShutter,
            evSteps = captureEv,
            params = params
        )
    }

    /** 显式中性滤镜名（用户主动选择"不加滤镜"，视为可套用） */
    private val NEUTRAL_FILTER_NAMES = setOf("原图", "标准", "默认")

    /** 滤镜强度百分比解析："黑白 0%" → 0f；无强度后缀 → null。
     *  委托 [PresetColorTransform.parseFilterPercent]（0~1 分数语义，测试锁定），避免双实现分歧 */
    private fun parseFilterPercent(filter: String): Float? =
        PresetColorTransform.parseFilterPercent(filter)

    /**
     * 滤镜是否可套用：引擎滤镜表内的滤镜名、或显式"原图/标准/默认" → true；
     * 滤镜表外的滤镜名（新机型独占滤镜等） → false，由 UI 如实标记 FILTER_UNRECOGNIZED。
     */
    fun isFilterApplicable(filter: String?): Boolean {
        if (filter.isNullOrBlank()) return false
        val key = filter.trim().split(Regex("\\s+")).firstOrNull() ?: return false
        if (key in NEUTRAL_FILTER_NAMES) return true
        // 显式 0% 强度（如"黑白 0%"）= 用户主动不加滤镜，与"原图/标准"同义，不得误标 FILTER_UNRECOGNIZED
        val percent = parseFilterPercent(filter)
        if (percent != null && percent <= 0f) return true
        return PresetColorTransform.filterAdjustment(filter) != PresetColorTransform.NEUTRAL
    }

    /** "100" / "200-400" / "Auto" / null → ISO；不可解析 → null */
    fun parseIso(value: String?): Int? {
        val v = value?.trim() ?: return null
        if (v.isEmpty() || v.lowercase() == "auto" || v == "自动") return null
        val range = Regex("(\\d+)\\s*[-~]\\s*(\\d+)").find(v)
        if (range != null) {
            val lo = range.groupValues[1].toIntOrNull() ?: return null
            val hi = range.groupValues[2].toIntOrNull() ?: return null
            return (lo + hi) / 2
        }
        return v.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }
    }

    /**
     * "1/125" / "1/125s" / "2s" / "0.5" / "125" → 纳秒。
     * 约定：带斜杠按分数快门；纯小数按秒；纯整数优先按秒（如 "2" 表示 2 秒长曝光）。
     */
    fun parseShutterNs(value: String?): Long? {
        val v = value?.trim()?.replace(Regex("s$", RegexOption.IGNORE_CASE), "")
            ?.removeSuffix("秒")?.trim() ?: return null
        if (v.isEmpty()) return null
        if (v.contains('/')) {
            val parts = v.split('/')
            if (parts.size != 2) return null
            val den = parts[1].trim().toDoubleOrNull() ?: return null
            if (den <= 0.0) return null
            val seconds = 1.0 / den
            return (seconds * 1_000_000_000).toLong().takeIf { it > 0 }
        }
        val seconds = v.toDoubleOrNull() ?: return null
        if (seconds <= 0.0) return null
        return (seconds * 1_000_000_000).toLong()
    }

    /** EV → 设备补偿步数（按步进 EV 换算并限幅）；无设备步进信息或 EV 为 0 → null */
    fun evToSteps(ev: Float, ranges: ManualRanges): Int? {
        if (ev == 0f) return null
        val stepEv = ranges.evStepEv ?: return null
        if (stepEv <= 0f) return null
        val range = ranges.evStepRange ?: return null
        return Math.round(ev / stepEv).coerceIn(range.first, range.last)
    }

    private fun formatSigned(value: Int?): String = value?.let { (if (it >= 0) "+" else "") + it } ?: ""
}
