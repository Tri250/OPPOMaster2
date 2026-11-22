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
    /** 展示值：预设原始写法（如 "200-400"、"1/125"）；写入前被设备范围钳制/补偿时
     *  为 "原值→实际值"（如 "50→100"、"1/100→1/200"） */
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
        // 【v1.8.3 审计修复】"Auto/自动" = 交给相机 AE 决定的正常创作意图，
        // 不是"取值无法解析"——内置 5 条预设含 Auto 写法，此前被误标
        // UNSUPPORTED(VALUE_UNPARSEABLE) 虚报错误。Auto 不产生任何芯片。
        val isoAuto = isAutoValue(preset.iso)
        val shutterAuto = isAutoValue(preset.shutterSpeed)
        val canManual = ranges.aeModeOffSupported &&
            ranges.isoRange != null && ranges.exposureTimeRange != null
        val wantManual = canManual && isoValue != null && shutterValue != null

        val captureIso = if (wantManual) isoValue.coerceIn(ranges.isoRange) else null
        // 【v1.8.3 Reno17 适配】设备 ISO 范围可能不含预设值（如 Reno17 主摄下限 ISO 100，
        // 而内置"人文/清新人文"指定 ISO 50）。直接钳制会让曝光量翻倍（+1EV 过曝）；
        // 此处按 ISO 钳制比例等比调整快门，保持 ISO×快门（曝光量）不变后再落设备快门
        // 范围，确保成片亮度符合预设意图（快门变化 1 档以内，不影响构图与主体）。
        val captureShutter = if (wantManual) {
            val requestedShutter = shutterValue
            // 曝光量 ISO×t 守恒：ISO 被抬高（如 50→100）则快门缩短，被压低则快门延长
            val compensation = isoValue.toDouble() / captureIso!!.toDouble()
            Math.round(requestedShutter * compensation)
                .coerceIn(ranges.exposureTimeRange)
        } else null

        val evValue = PresetColorTransform.parseExposureEv(preset.exposureCompensation)
        val steps = evToSteps(evValue, ranges)
        val captureEv = if (!wantManual && steps != null) steps else null

        // ISO（Auto = AE 决定，不产生芯片）
        if (preset.iso != null && !isoAuto) {
            params += when {
                captureIso != null -> ParamStatus(
                    ParamKey.ISO,
                    appliedLabel(preset.iso, captureIso.toString(), captureIso != isoValue),
                    ApplyStatus.WRITTEN
                )
                !canManual -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.MANUAL_EXPOSURE_UNAVAILABLE)
                isoValue == null -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
                else -> ParamStatus(ParamKey.ISO, preset.iso, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.NEED_ISO_SHUTTER_PAIR)
            }
        }

        // 快门（Auto = AE 决定，不产生芯片）
        if (preset.shutterSpeed != null && !shutterAuto) {
            params += when {
                captureShutter != null -> ParamStatus(
                    ParamKey.SHUTTER,
                    appliedLabel(
                        preset.shutterSpeed,
                        formatShutterNs(captureShutter),
                        captureShutter != shutterValue
                    ),
                    ApplyStatus.WRITTEN
                )
                !canManual -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.MANUAL_EXPOSURE_UNAVAILABLE)
                shutterValue == null -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
                else -> ParamStatus(ParamKey.SHUTTER, preset.shutterSpeed, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.NEED_ISO_SHUTTER_PAIR)
            }
        }

        // 曝光补偿：手动曝光已包含曝光控制，AE 开启时按步进写入；EV=0 视为无需套用。
        // 【v1.8.10 三轮审计】纯乱码文本（不含任何数字）如实归因 VALUE_UNPARSEABLE，
        // 与 ISO/快门口径一致，不再静默当作 0EV 丢失无提示；含数字文本经
        // parseExposureEv 宽容解析（"+0.5EV"/"±0.3" 等后缀写法），解析为 0 的
        // （"0"/"0.0"）维持"无需套用、不产生芯片"既有契约
        val evRaw = preset.exposureCompensation
        if (evRaw != null && !isAutoValue(evRaw)) {
            if (!evRaw.any { it.isDigit() }) {
                params += ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                    UnsupportedReason.VALUE_UNPARSEABLE)
            } else if (evValue != 0f) {
                params += when {
                    captureEv != null -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.WRITTEN)
                    // 手动曝光下 EV 由 ISO/快门接管：如实标记"不适用"且归因正确，不再误报设备能力缺失
                    wantManual -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                        UnsupportedReason.MANUAL_EXPOSURE_OVERRIDES)
                    // 设备无步进信息/步进非法（解析失败已前置归因）
                    else -> ParamStatus(ParamKey.EV, evRaw, ApplyStatus.UNSUPPORTED,
                        UnsupportedReason.EV_UNAVAILABLE)
                }
            }
        }

        // 拍后套用项（与设备能力无关，恒可套用）。
        // 【v1.8.0 审计修复】门控口径与 EV（上方 evValue != 0f）及
        // [PresetColorTransform.hasVisibleEffect] 对齐：取值存在但效果中性
        // （饱和度 0 / 色温 5500K / 柔光"无"等）不产生芯片——
        // 否则会显示"0 · 拍后套用"这类"假装已套用"的误导信息，
        // 违背本类"UI 如实展示，不做假装已套用"的设计契约。
        // 【v1.8.3 审计修复】黑白/单色灰度滤镜下，矩阵强制归零饱和度/冷暖/青品
        // （真灰度输出，见 buildColorMatrix），这些参数不得显示"拍后套用"芯片——
        // 否则内置"高对比黑白"（冷暖 +100/青品 -39）会虚报效果。
        // 滤镜本身与影调/柔光/暗角等仍如实 POST。
        // 【v1.8.8 调色盘百分比真实化】灰度强度化后仅满灰度（a≥1）归零；
        // 部分灰度（如"黑白 50%"）下饱和度/冷暖/青品按 (1-a) 比例仍生效，芯片恢复显示。
        val grayAmount = PresetColorTransform.filterAdjustment(preset.filter)
            .let { if (it.grayscale) it.grayscaleAmount else 0f }
        val fullGrayscale = grayAmount >= 1f
        fun post(key: ParamKey, value: String?, effective: Boolean) {
            if (effective && value != null) params += ParamStatus(key, value, ApplyStatus.POST)
        }
        post(ParamKey.COLOR_TEMP, preset.colorTemperature?.let { "${it}K" },
            PresetColorTransform.colorTempWarmDelta(preset.colorTemperature) != 0f)
        post(ParamKey.FILTER, preset.filter, isFilterApplicable(preset.filter))
        post(ParamKey.SATURATION, formatSigned(preset.saturation),
            !fullGrayscale && (preset.saturation ?: 0) != 0)
        post(ParamKey.TONE, formatSigned(preset.tone), (preset.tone ?: 0) != 0)
        post(ParamKey.WARM_COOL, formatSigned(preset.warmCool),
            !fullGrayscale && (preset.warmCool ?: 0) != 0)
        post(ParamKey.CYAN_MAGENTA, formatSigned(preset.cyanMagenta),
            !fullGrayscale && (preset.cyanMagenta ?: 0) != 0)
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

    /** "Auto"（任意大小写）/ "自动" → true：交给相机 AE 决定的正常创作意图 */
    private fun isAutoValue(value: String?): Boolean {
        val v = value?.trim() ?: return false
        return v.lowercase() == "auto" || v == "自动"
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
     * 约定：带斜杠按分数快门（分子/分母，"3/4"=0.75s——【v1.8.10 审计修复】
     * 此前忽略分子把 "3/4" 错解析成 1/4s，曝光量偏差 3 倍）；纯小数按秒；
     * 纯整数优先按秒（如 "2" 表示 2 秒长曝光）。
     */
    fun parseShutterNs(value: String?): Long? {
        val v = value?.trim()?.replace(Regex("s$", RegexOption.IGNORE_CASE), "")
            ?.removeSuffix("秒")?.trim() ?: return null
        if (v.isEmpty()) return null
        if (v.contains('/')) {
            val parts = v.split('/')
            if (parts.size != 2) return null
            // 分子容忍不等号前缀（"<1/100" 表示快门上限描述，取 1/100）
            val num = Regex("[\\d.]+").find(parts[0])?.value?.toDoubleOrNull() ?: return null
            val den = parts[1].trim().toDoubleOrNull() ?: return null
            if (num <= 0.0 || den <= 0.0) return null
            val seconds = num / den
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

    /**
     * 设备范围钳制/补偿后，实际写入值与预设原始写法不同时展示"原值→实际值"，
     * 如实反映真正写进拍摄请求的内容（不做"显示即套用"的虚报）。
     */
    private fun appliedLabel(raw: String, applied: String, changed: Boolean): String =
        if (changed) "$raw→$applied" else raw

    /** 纳秒 → 快门写法（"1/200" / "2s"），用于展示设备钳制后的实际写入值 */
    fun formatShutterNs(ns: Long): String {
        if (ns <= 0L) return "0"
        if (ns >= 1_000_000_000L) {
            val seconds = ns / 1_000_000_000.0
            val rounded = Math.round(seconds)
            return if (Math.abs(seconds - rounded) < 1e-6) "${rounded}s"
            else String.format(java.util.Locale.ROOT, "%.2fs", seconds)
        }
        return "1/${Math.round(1_000_000_000.0 / ns)}"
    }
}
