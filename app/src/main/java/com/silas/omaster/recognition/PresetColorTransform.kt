package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import kotlin.math.pow

/**
 * 滤镜名 → 色彩调整量（与数值参数同单位：-1..1 增量；lift 为 0..255 亮度偏移）。
 */
data class FilterAdjust(
    val saturation: Float = 0f,
    val warmCool: Float = 0f,
    val cyanMagenta: Float = 0f,
    val tone: Float = 0f,
    val contrastDelta: Float = 0f,
    val lift: Float = 0f,
    val ev: Float = 0f,
    /** 灰度滤镜（黑白/单色）标记；强度比例见 [grayscaleAmount] */
    val grayscale: Boolean = false,
    /**
     * 【v1.8.8 调色盘百分比真实化】灰度强度 0..1（1=完全去色）。
     * "黑白 50%" 此前被硬性渲染成 100% 黑白（grayscale 布尔不随强度缩放），
     * 百分比控制在黑白滤镜上完全失效；现在按强度保留 (1-强度) 比例的色彩，
     * 与彩色滤镜的百分比语义对齐——调色盘滑杆拖到多少，出片就缩放多少。
     */
    val grayscaleAmount: Float = 1f
)

/**
 * 预设 → 色彩变换矩阵（拍后套色核心，纯 JVM 计算，可单测）。
 *
 * 消费 MasterPreset 全部"拍后可套用"参数：
 * - 数值调色：saturation/warmCool/cyanMagenta/tone；
 * - 相机滤镜 filter（黑白/胶片/复古/鲜明/夜景/柔光/琥珀…）→ [filterAdjustment]；
 * - 详情参数：colorTemperature（色温→冷暖偏移）、colorHue（色调→青品偏移）、
 *   exposureCompensation（EV→亮度增益 2^ev）、softLight（柔光→提亮降对比）；
 * - vignette/sharpness 无法用 ColorMatrix 表达，由 [PresetPixelEffects] 像素级套用。
 *
 * 这是"风格模拟"的实现，非哈苏原厂算法（PRD 边界声明）。
 */
object PresetColorTransform {

    val NEUTRAL = FilterAdjust()

    /**
     * 调色盘可选滤镜名（与 [filterAdjustment] 的滤镜表逐字一致）。
     * UI 通过此列表渲染滤镜 chips，保证"选什么名、引擎认什么名"，
     * 避免展示层与引擎层两份名单漂移。
     */
    val PRESET_FILTER_NAMES = listOf(
        "黑白", "单色", "胶片", "复古", "高级灰", "鲜明", "明艳", "浓郁", "夜景", "柔光",
        "童话", "暖调", "冷调", "琥珀", "清新", "通透", "清透", "人文", "美味", "霓虹", "赛博朋克"
    )

    /**
     * 滤镜名 → 调整量。前缀匹配（兼容"黑白 高对比"带后缀写法），未知/原图/标准 → 中性。
     * 支持强度百分比缩放："复古 75%" = 复古滤镜的 75% 强度；"标准 0%"/强度 0 → 完全中性。
     */
    fun filterAdjustment(filter: String?): FilterAdjust {
        val raw = filter?.trim() ?: return NEUTRAL
        val key = raw.split(Regex("\\s+")).firstOrNull() ?: return NEUTRAL
        val base = when (key) {
            "黑白", "单色" -> NEUTRAL.copy(grayscale = true, contrastDelta = 0.06f)
            "胶片" -> NEUTRAL.copy(saturation = -0.14f, lift = 10f, contrastDelta = -0.08f, warmCool = 0.10f, cyanMagenta = 0.04f)
            "复古" -> NEUTRAL.copy(saturation = -0.12f, lift = 8f, contrastDelta = -0.06f, warmCool = 0.08f)
            "高级灰" -> NEUTRAL.copy(saturation = -0.22f, lift = 6f, contrastDelta = -0.04f)
            "鲜明", "明艳" -> NEUTRAL.copy(saturation = 0.18f, contrastDelta = 0.10f)
            "浓郁" -> NEUTRAL.copy(saturation = 0.20f, contrastDelta = 0.08f, tone = -0.05f)
            "夜景" -> NEUTRAL.copy(tone = -0.10f, contrastDelta = 0.10f, warmCool = -0.05f, ev = -0.2f)
            "柔光" -> NEUTRAL.copy(contrastDelta = -0.10f, lift = 8f, saturation = -0.04f)
            "童话" -> NEUTRAL.copy(contrastDelta = -0.10f, lift = 10f, saturation = -0.02f, warmCool = 0.02f)
            "暖调" -> NEUTRAL.copy(warmCool = 0.12f)
            "冷调" -> NEUTRAL.copy(warmCool = -0.12f)
            "琥珀" -> NEUTRAL.copy(warmCool = 0.12f, saturation = 0.06f)
            "清新" -> NEUTRAL.copy(saturation = -0.08f, lift = 5f, warmCool = -0.06f)
            "通透", "清透" -> NEUTRAL.copy(contrastDelta = 0.08f, lift = 4f, saturation = 0.04f)
            "人文" -> NEUTRAL.copy(saturation = -0.06f, warmCool = 0.04f, contrastDelta = 0.05f)
            "美味" -> NEUTRAL.copy(saturation = 0.10f, warmCool = 0.06f)
            "霓虹" -> NEUTRAL.copy(saturation = 0.14f, cyanMagenta = 0.10f, contrastDelta = 0.08f)
            "赛博朋克" -> NEUTRAL.copy(saturation = 0.14f, cyanMagenta = 0.12f, contrastDelta = 0.10f, warmCool = -0.06f)
            else -> NEUTRAL
        }
        // 强度百分比："名称 NN%" → 按比例缩放；无百分比 → 满强度
        val percent = parseFilterPercent(raw) ?: return base
        return when {
            percent <= 0f -> NEUTRAL
            percent >= 1f -> base
            else -> base.scaled(percent)
        }
    }

    /** 滤镜值尾部百分比："复古 75%" → 0.75；无百分比 → null */
    fun parseFilterPercent(filter: String): Float? {
        val match = Regex("(\\d+(?:\\.\\d+)?)\\s*%\\s*$").find(filter.trim()) ?: return null
        return (match.groupValues[1].toFloatOrNull() ?: return null) / 100f
    }

    private fun FilterAdjust.scaled(factor: Float): FilterAdjust = FilterAdjust(
        saturation = saturation * factor,
        warmCool = warmCool * factor,
        cyanMagenta = cyanMagenta * factor,
        tone = tone * factor,
        contrastDelta = contrastDelta * factor,
        lift = lift * factor,
        ev = ev * factor,
        // 黑白/单色：灰度强度随百分比缩放（"黑白 50%" = 保留 50% 色彩的部分灰度），
        // 强度为 0 已由上层返回 NEUTRAL
        grayscale = grayscale,
        grayscaleAmount = grayscaleAmount * factor
    )

    /**
     * "+0.7" / "-1.0" / "1" / 区间 "-0.3~-1.0" / "-1~0"（取两端中值）→ EV，限幅 ±3；
     * 不可解析 → 0
     */
    fun parseExposureEv(value: String?): Float {
        // 【v1.8.10 三轮审计】容忍 "EV" 后缀写法（全局去 EV，区间 "-0.3EV~-1.0EV" 同步支持）：
        // 截图 OCR 与部分订阅源沿用相机界面 "±0.5EV" 原文，此前解析失败静默归零，
        // EV 效果丢失且无任何归因提示——与 parseIso（"ISO 100"）/parseShutterNs（"1/60s"）
        // 的单位后缀宽容度对齐
        val v = value?.trim()?.replace("+", "")?.replace("±", "")
            ?.replace(Regex("ev", RegexOption.IGNORE_CASE), "")?.trim() ?: return 0f
        if (v.isEmpty()) return 0f
        // 区间写法取中值（"-0.3~-1.0" → -0.65）
        val range = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*[~～–]\\s*(-?\\d+(?:\\.\\d+)?)$").find(v)
        if (range != null) {
            val lo = range.groupValues[1].toFloatOrNull() ?: return 0f
            val hi = range.groupValues[2].toFloatOrNull() ?: return 0f
            return ((lo + hi) / 2f).coerceIn(-3f, 3f)
        }
        val num = v.toFloatOrNull() ?: return 0f
        return num.coerceIn(-3f, 3f)
    }

    /** softLight：弱/柔美/中/强/梦幻/无 或数字 0-100 → 0..1 强度 */
    fun parseSoftLight(value: String?): Float = when (val v = value?.trim()) {
        null, "" -> 0f
        "弱", "柔" -> 0.3f
        "柔美", "中" -> 0.5f
        "强", "梦幻" -> 0.8f
        "无", "关" -> 0f
        else -> (v.toFloatOrNull() ?: 0f).coerceIn(0f, 100f) / 100f
    }

    /** vignette：开/弱/中/强 或数字 → 0..1 强度；关/null → 0 */
    fun parseVignette(value: String?): Float = when (val v = value?.trim()) {
        null, "", "关" -> 0f
        "开", "中" -> 0.5f
        "弱" -> 0.3f
        "强" -> 0.8f
        else -> (v.toFloatOrNull() ?: 0f).coerceIn(0f, 100f) / 100f
    }

    fun sharpnessAmount(preset: MasterPreset): Float =
        (preset.sharpness ?: 0).coerceIn(0, 100) / 100f

    fun vignetteStrength(preset: MasterPreset): Float = parseVignette(preset.vignette)

    /** 色温（2000-8000K）→ 冷暖增量：低色温偏暖、高色温偏冷 */
    fun colorTempWarmDelta(kelvin: Int?): Float =
        if (kelvin == null) 0f else ((5500f - kelvin) / 3500f * 0.35f).coerceIn(-0.45f, 0.45f)

    /** colorHue（-150..150）→ 青品增量 */
    fun colorHueCyanMagentaDelta(hue: Int?): Float =
        if (hue == null) 0f else (hue / 150f * 0.3f).coerceIn(-0.3f, 0.3f)

    /** 20 项 4x5 颜色矩阵（行主序），null 参数按中性处理 */
    fun buildColorMatrix(preset: MasterPreset): FloatArray {
        val adj = filterAdjustment(preset.filter)
        val fade = parseSoftLight(preset.softLight)
        val ev = (parseExposureEv(preset.exposureCompensation) + adj.ev).coerceIn(-3f, 3f)

        val saturation = ((preset.saturation ?: 0).coerceIn(-100, 100) / 100f + adj.saturation)
            .coerceIn(-1f, 1f)
        var warmCool = ((preset.warmCool ?: 0).coerceIn(-100, 100) / 100f +
            adj.warmCool + colorTempWarmDelta(preset.colorTemperature)).coerceIn(-1f, 1f)
        var cyanMagenta = ((preset.cyanMagenta ?: 0).coerceIn(-100, 100) / 100f +
            adj.cyanMagenta + colorHueCyanMagentaDelta(preset.colorHue)).coerceIn(-1f, 1f)
        val tone = ((preset.tone ?: 0).coerceIn(-100, 100) / 100f + adj.tone).coerceIn(-1f, 1f)

        // 【v1.8.8 调色盘百分比真实化】灰度强度化：a=1 完全去色（与旧布尔行为逐位一致），
        // a=0 无灰度滤镜，0<a<1 部分灰度——统一公式 (1-a)×(1+sat×0.8) 在两端点
        // 精确退化为旧路径（a=1 → 0；a=0 → 1+sat×0.8），内置预设 0%/100% 契约不变。
        // 部分灰度时冷暖/青品按 (1-a) 保留（满灰度仍强制归零，不受冷暖/青品影响）。
        val grayAmount = if (adj.grayscale) adj.grayscaleAmount else 0f
        val colorKeep = 1f - grayAmount
        val satFactor = colorKeep * (1f + saturation * 0.8f)
        warmCool *= colorKeep
        cyanMagenta *= colorKeep

        // 【v1.8.3 审计修复】灰度（黑白/单色）下"影调"切换为暗房语义：
        // 负影调 = 低调摄影（压黑位 + 增强反差），正影调 = 高调摄影（提黑位 + 柔化反差）。
        // 彩色路径的"负影调 → 降对比柔化"语义不适用于纯灰度输出——否则内置
        // "高对比黑白"（影调 -61）会渲染成 c≈0.68 的灰蒙黑白，与预期效果相反。
        // 暗房语义仅在满灰度（a≥1）启用；部分灰度画面仍以彩色为主，沿用彩色语义。
        val toneContrast = if (grayAmount >= 1f) -tone * 0.5f else tone * 0.5f
        var contrast = 1f + toneContrast + adj.contrastDelta - fade * 0.15f
        contrast = (contrast * 2f.pow(ev)).coerceAtLeast(0.05f)
        val lift = tone * 18f + adj.lift + fade * 12f

        val m = FloatArray(20)
        // 灰度亮度权重（Rec.709）
        val lr = 0.2126f; val lg = 0.7152f; val lb = 0.0722f

        // R 行
        m[0] = contrast * (lr + satFactor * (1 - lr)) + warmCool * 0.12f
        m[1] = contrast * (lg - satFactor * lg)
        m[2] = contrast * (lb - satFactor * lb) - warmCool * 0.12f
        m[3] = 0f; m[4] = lift + warmCool * 10f

        // G 行
        m[5] = contrast * (lr - satFactor * lr)
        // 【v1.8.0 审计修复】青品轴必须同时作用于 G 行：正值（偏品红）压低绿增益、
        // 负值（偏绿）抬升绿增益。此前仅调制 B 行交叉项，绿色内容完全不受色调影响。
        m[6] = contrast * (lg + satFactor * (1 - lg)) - cyanMagenta * 0.10f
        m[7] = contrast * (lb - satFactor * lb)
        m[8] = 0f; m[9] = lift

        // B 行
        m[10] = contrast * (lr - satFactor * lr) - warmCool * 0.10f + cyanMagenta * 0.08f
        m[11] = contrast * (lg - satFactor * lg) - cyanMagenta * 0.05f
        m[12] = contrast * (lb + satFactor * (1 - lb)) + warmCool * 0.10f - cyanMagenta * 0.08f
        m[13] = 0f; m[14] = lift - warmCool * 10f

        // A 行
        m[15] = 0f; m[16] = 0f; m[17] = 0f; m[18] = 1f; m[19] = 0f
        return m
    }

    /** 该预设是否会产生可见效果（色彩矩阵、像素效果任一非中性） */
    fun hasVisibleEffect(preset: MasterPreset): Boolean {
        if (listOf(preset.saturation, preset.warmCool, preset.cyanMagenta, preset.tone)
                .any { it != null && it != 0 }) return true
        if (filterAdjustment(preset.filter) != NEUTRAL) return true
        // 【v1.8.0 审计修复】中性取值的详情参数（5500K 色温 / 0 色调）不产生任何
        // 矩阵增量，按"无可见效果"处理，与 POST 芯片门控口径一致
        if (colorTempWarmDelta(preset.colorTemperature) != 0f) return true
        if (colorHueCyanMagentaDelta(preset.colorHue) != 0f) return true
        if (parseExposureEv(preset.exposureCompensation) != 0f) return true
        if (parseSoftLight(preset.softLight) > 0f) return true
        if (vignetteStrength(preset) > 0f || sharpnessAmount(preset) > 0f) return true
        return false
    }
}
