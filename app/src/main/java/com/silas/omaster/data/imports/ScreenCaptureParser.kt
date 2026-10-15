package com.silas.omaster.data.imports

import com.silas.omaster.model.MasterPreset

/**
 * MU-06① 对照录入 · 截图文本解析器（纯 JVM，ED-03 单测覆盖）。
 *
 * 场景：用户在 ColorOS 17「AI 一键闪记」截图后，把相机参数文本（哈苏大师模式 /
 * 专业模式参数页）复制或分享进应用，解析为预设字段，经 [PresetImportValidator]
 * 同源的语义落入自定义库。UI 层（MU-06②）仅在真机验收，本类保证 CI 可测（自检 F5）。
 *
 * 解析规则：
 * - 逐 token 正则匹配「标签 + 数值」，中英文标签、±号、小数、K 温标均兼容；
 * - 无法识别的行忽略（不报错）——对照录入宁缺毋滥；
 * - 首个非参数行作为预设名（可选）；
 * - 产出 [ParsedCapture]，由 [toPreset] 构造 MasterPreset（coverPath 置空走占位图）。
 */
object ScreenCaptureParser {

    /** 单字段解析结果：字段名与 MasterPreset 属性一一对应 */
    data class ParsedCapture(
        val name: String?,
        val fields: Map<String, String>
    )

    // 每个条目：正则（含标签与捕获组）→ MasterPreset 字段名
    private val tokenRules: List<Pair<Regex, String>> = listOf(
        // ISO：ISO 100 / ISO100 / 感光度 100
        Regex("(?:ISO|感光度)\\s*([0-9]{1,5})", RegexOption.IGNORE_CASE) to "iso",
        // 快门：快门 1/250 / 1/250s / shutter 1/250
        Regex("(?:快门|shutter)\\s*([0-9]+/[0-9]+(?:\\.[0-9]+)?)s?", RegexOption.IGNORE_CASE) to "shutterSpeed",
        // EV：EV -1.7 / EV+2 / 曝光补偿 -1.7
        Regex("(?:EV|曝光补偿)\\s*([+\\-]?[0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE) to "exposureCompensation",
        // 白平衡：WB 5000K / 白平衡 5000K / 色温 5000
        Regex("(?:WB|白平衡|色温)\\s*([0-9]{3,5})\\s*K?", RegexOption.IGNORE_CASE) to "colorTemperature",
        // 色调：色调 +5 / tone -3
        Regex("(?:色调|tone)\\s*([+\\-]?[0-9]{1,3})", RegexOption.IGNORE_CASE) to "tone",
        // 饱和度：饱和度 +10 / saturation 20
        Regex("(?:饱和度|saturation)\\s*([+\\-]?[0-9]{1,3})", RegexOption.IGNORE_CASE) to "saturation",
        // 冷暖：冷暖 -5 / warmCool +8
        Regex("(?:冷暖|暖冷|warmCool)\\s*([+\\-]?[0-9]{1,3})", RegexOption.IGNORE_CASE) to "warmCool",
        // 青品（洋红-青轴）：青品 -2 / 洋红 +3
        Regex("(?:青品|洋红|cyanMagenta)\\s*([+\\-]?[0-9]{1,3})", RegexOption.IGNORE_CASE) to "cyanMagenta",
        // 锐度：锐度 15 / sharpness 20
        Regex("(?:锐度|sharpness)\\s*([0-9]{1,3})", RegexOption.IGNORE_CASE) to "sharpness",
        // 滤镜：滤镜 经典负片 / filter 醇黑
        Regex("(?:滤镜|filter)\\s*([\\u4e00-\\u9fa5A-Za-z0-9·]{1,12})") to "filter",
        // 暗角：暗角 中 / 弱 / 强
        Regex("暗角\\s*([\\u4e00-\\u9fa5]{1,3})") to "vignette",
        // 软光：软光 弱 / 强
        Regex("软光\\s*([\\u4e00-\\u9fa5]{1,3})") to "softLight",
        // 色调风格：色调风格 自然 / colorTone 鲜明（仅在未被「色调」吃掉时）
        Regex("(?:色调风格|colorTone)\\s*([\\u4e00-\\u9fa5A-Za-z]{1,8})") to "colorTone"
    )

    fun parse(text: String?): ParsedCapture? {
        if (text.isNullOrBlank()) return null
        val fields = linkedMapOf<String, String>()
        var name: String? = null

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            var matched = false
            // 逐行匹配所有规则（单行可含多个参数，如「ISO100 快门 1/60 EV+2 WB 5000K」）
            for ((regex, field) in tokenRules) {
                val m = regex.find(line) ?: continue
                // 同字段首见优先（截图可能含重复行）
                fields.putIfAbsent(field, m.groupValues[1])
                matched = true
            }
            if (!matched && name == null) {
                // 首个非参数行作为名称（限长，去装饰符）
                val candidate = line.trimStart('#', '「', '【').trimEnd('」', '】')
                if (candidate.length in 1..24) name = candidate
            }
        }
        if (fields.isEmpty()) return null
        return ParsedCapture(name, fields)
    }
}

/**
 * 解析结果 → MasterPreset 扩展函数（包级，方便跨文件直接调用 capture.toPreset()）
 * 默认值兜底，name 缺省给「对照录入」占位名
 */
fun ScreenCaptureParser.ParsedCapture.toPreset(): MasterPreset {
    val f = this.fields
    return MasterPreset(
        name = this.name?.takeIf { it.isNotBlank() } ?: "对照录入",
        coverPath = "",
        author = "@对照录入",
        filter = f["filter"],
        whiteBalance = f["colorTemperature"]?.let { "${it}K" },
        colorTone = f["colorTone"],
        exposureCompensation = f["exposureCompensation"],
        colorTemperature = f["colorTemperature"]?.toIntOrNull(),
        iso = f["iso"],
        shutterSpeed = f["shutterSpeed"],
        softLight = f["softLight"],
        tone = f["tone"]?.toIntOrNull(),
        saturation = f["saturation"]?.toIntOrNull(),
        warmCool = f["warmCool"]?.toIntOrNull(),
        cyanMagenta = f["cyanMagenta"]?.toIntOrNull(),
        sharpness = f["sharpness"]?.toIntOrNull(),
        vignette = f["vignette"],
        isCustom = true
    )
}
