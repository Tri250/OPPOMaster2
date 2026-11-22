package com.silas.omaster.recognition

import android.os.Build

/**
 * 【v1.8.10 Reno15/16/17 机型适配】Reno 机型档案 — POP 即拍质感调校。
 *
 * 设计原则与 [CameraCapabilityProbe] 一致：设备真实能力一律以 Camera2 探测为准，
 * 机型档案不伪造能力值，只提供两类**已知确定**的校准信息：
 *
 * 1. 机型识别（Reno15 / Reno16 / Reno17，含标准版 / Pro / Pro+ 等变体）：
 *    - 机型名文本直接含 "Reno15/16/17"（全球版营销名 / 部分系统 Build.MODEL）；
 *    - 国行 ColorOS 的 Build.MODEL 多为内部代号（如 "PJD110"），营销名改由
 *      oplus 系统属性提供（反射读取，多属性逐一尝试）；
 *    - 任一来源不可得 → 返回 null，按通用档调校（与旧版行为一致，无副作用）。
 * 2. 机型调校项（探测结果之外的拍摄质感增强）：
 *    - 拍后 unsharp 锐化降权：ColorOS 静态出片在 HAL 内自带锐化管线，
 *      Reno15/16/17 档 App 侧全量叠加会出现硬边光晕 → 统一 ×0.8 降权，
 *      配合既有中间调掩膜保细节（详见 [sharpeningMultiplier]）；
 *    - 慢快门稳拍提示：部分 Reno 档位无 OIS，慢于 1/30s 的手持快门是
 *      "出片率第一杀手"，写入手动快门前给用户明确稳拍预期。
 */
object RenoDeviceProfile {

    enum class Family { RENO15, RENO16, RENO17 }

    /** 手持安全快门线 1/30s（纳秒）：慢于此值提示稳拍 */
    const val STEADY_SHUTTER_THRESHOLD_NS: Long = 33_333_333L

    /** Reno15/16/17 代际文本（容忍空格/大小写；负向环视排除 "Reno150" 类误匹配） */
    private val RENO_GENERATION = Regex("Reno\\s*1([567])(?!\\d)", RegexOption.IGNORE_CASE)

    // oplus 营销名系统属性：不同 ColorOS 版本/分支暴露的键不同，逐一尝试
    private val MARKETING_NAME_KEYS = listOf(
        "ro.vendor.oplus.marketing_mark_name",
        "ro.config.marketing_name",
        "ro.vendor.oplus.marketing_name"
    )

    /**
     * 机型识别（纯 JVM 可测）。
     *
     * @param manufacturer  Build.MANUFACTURER（仅 OPPO 参与识别，一加/realme 不算 Reno）
     * @param model         Build.MODEL（全球版可能直接是营销名；国行为内部代号）
     * @param marketingName oplus 营销名系统属性（国行代号机型的唯一可靠来源；不可得传 null）
     * @return 命中的 Reno 代际；非 OPPO / 非 Reno15-17 / 无可用来源 → null
     */
    fun detectFamily(manufacturer: String?, model: String?, marketingName: String? = null): Family? {
        if (manufacturer.isNullOrBlank() || !manufacturer.contains("oppo", ignoreCase = true)) return null
        // 营销名优先（代际信息可靠），机型名兜底
        return familyFromText(marketingName) ?: familyFromText(model)
    }

    /** 当前设备机型档（识别失败为 null → 通用调校）。进程内缓存，只探测一次。 */
    @Volatile
    private var cachedFamily: Family? = null

    @Volatile
    private var probed = false

    fun currentFamily(): Family? {
        if (probed) return cachedFamily
        synchronized(this) {
            if (probed) return cachedFamily
            val marketingName = MARKETING_NAME_KEYS.firstNotNullOfOrNull { readSystemProperty(it) }
            cachedFamily = detectFamily(Build.MANUFACTURER, Build.MODEL, marketingName)
            probed = true
            return cachedFamily
        }
    }

    /**
     * 拍后 unsharp 锐化强度倍率（机型调校，纯 JVM 可测）。
     *
     * ColorOS 静态出片自带 HAL 锐化（与 App 的 MAXIMIZE_QUALITY 厂商后处理
     * 全链路叠加），Reno15/16/17 档按 ×0.8 降权避免双重锐化的硬边光晕；
     * 其余机型维持 1.0，引擎输出与既有契约逐位一致。
     */
    fun sharpeningMultiplier(family: Family?): Float = when (family) {
        Family.RENO15, Family.RENO16, Family.RENO17 -> 0.8f
        null -> 1.0f
    }

    /**
     * 慢快门稳拍提示（纯 JVM 可测）：写入的手动快门慢于 1/30s 时为 true。
     * null（AE 自动接管）或快门快于安全线 → false，不干扰用户。
     */
    fun needsSteadyHint(shutterNs: Long?): Boolean =
        shutterNs != null && shutterNs > STEADY_SHUTTER_THRESHOLD_NS

    private fun familyFromText(text: String?): Family? {
        if (text.isNullOrBlank()) return null
        val match = RENO_GENERATION.find(text) ?: return null
        return when (match.groupValues[1]) {
            "5" -> Family.RENO15
            "6" -> Family.RENO16
            "7" -> Family.RENO17
            else -> null
        }
    }

    /** 反射读取系统属性（系统 API 不在 SDK 公开，失败返回 null 不抛异常） */
    private fun readSystemProperty(key: String): String? = try {
        @Suppress("PrivateApi")
        val getter = Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
        (getter.invoke(null, key) as? String)?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}
