package com.silas.omaster.util

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 照片摄影参数解析与格式化（纯 JVM，无 Android 依赖，便于单元测试）。
 *
 * 职责：
 * - 把 EXIF 原始字符串（快门/曝光补偿/曝光值）解析为相机 UI Human-readable 形式；
 * - 提供 APEX 曝光值 EV → 曝光时间秒 的换算（ExifInterface 的 ExposureTime 有时缺失，
 *   可用 ShutterSpeedValue(APEX) 反推）；
 * - 白平衡数值 → 色温区间/文字标签的归一化；
 * - 全部方法对 null / 非法输入安全，返回 null 而非抛异常。
 *
 * 对应 PRD：FR-01（EXIF 参数读取）、8.2 输入契约（缺失处理）。
 */
object PhotoParamParser {

    /** 快门速度："1/250" / "0.004" / "1/250s" → "1/250s"；无法解析返回 null */
    fun formatShutter(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().removeSuffix("s").removeSuffix("S").trim()
        // 分数形式 1/250
        val frac = Regex("""(\d+)\s*/\s*(\d+)""").matchEntire(s)
        if (frac != null) {
            val num = frac.groupValues[1].toIntOrNull() ?: return null
            val den = frac.groupValues[2].toIntOrNull() ?: return null
            if (den == 0 || num == 0) return null
            // 约分：2/250 → 1/125
            val g = gcd(num, den)
            val n = num / g
            val d = den / g
            return if (n == 1) "1/$d" else "${n}/${d}s"
        }
        // 小数形式 0.004 → 1/250
        val decimal = s.toDoubleOrNull() ?: return null
        if (decimal <= 0 || decimal > 3600) return null
        return decimalToShutter(decimal)
    }

    /** 曝光补偿："-0.7" / "+1" / "0" → "-0.7EV"；非法返回 null */
    fun formatExposureBias(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val v = raw.trim().removeSuffix("EV").removeSuffix("ev").toDoubleOrNull() ?: return null
        if (v !in -20.0..20.0) return null
        if (v == 0.0) return "0EV"
        val sign = if (v > 0) "+" else ""
        // 统一保留一位小数（相机 UI 习惯：-1 → -1.0EV，+0.7 → +0.7EV）
        val text = String.format(java.util.Locale.getDefault(), "%.1f", v)
        return "${sign}${text}EV"
    }

    /**
     * APEX 快门速度值（ShutterSpeedValue）→ 曝光时间秒。
     * 公式：t = 1 / 2^EV。例：EV=8 → 1/256s ≈ 1/250s。
     */
    fun apexToShutterSeconds(apex: String?): String? {
        val ev = apex?.toDoubleOrNull() ?: return null
        if (ev !in -20.0..30.0) return null
        val seconds = 1.0 / 2.0.pow(ev)
        return decimalToShutter(seconds)
    }

    /** APEX 光圈值（ApertureValue）→ f 值。公式：N = 2^(EV/2) */
    fun apexToAperture(apex: String?): String? {
        val ev = apex?.toDoubleOrNull() ?: return null
        if (ev !in -10.0..20.0) return null
        val f = 2.0.pow(ev / 2.0)
        return "f/${String.format(java.util.Locale.getDefault(), "%.1f", f)}"
    }

    /** 小数秒 → "1/250" / "1.6s" 形式（取最接近的常见档位） */
    private fun decimalToShutter(seconds: Double): String {
        if (seconds >= 1.0) {
            val rounded = (seconds * 10).roundToInt() / 10.0
            return "${rounded}s"
        }
        // 取倒数并吸附到接近的整数分母（1/250、1/125 等）
        val den = (1.0 / seconds).roundToInt().coerceAtLeast(1)
        return "1/$den"
    }

    /** 白平衡字符串 → 色温 Int（2000~10000），无法解析返回 null */
    fun parseColorTemp(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val num = Regex("""(\d{3,5})""").find(raw)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return if (num in 2000..10000) num else null
    }

    /** 白平衡模式标签归一化：Auto/自动/日光/阴天/白炽灯 等 */
    fun normalizeWhiteBalance(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "auto", "0", "自动" -> "自动"
            "daylight", "1", "日光" -> "日光"
            "cloudy", "2", "阴天" -> "阴天"
            "tungsten", "3", "白炽灯" -> "白炽灯"
            "fluorescent", "4", "荧光灯" -> "荧光灯"
            "flash", "5", "闪光灯" -> "闪光灯"
            else -> raw.trim().take(12)
        }
    }

    /** EV(ISO) → 相对 ISO 数值的曝光档位差，用于风格对比的可解释性展示 */
    fun isoToEv(iso: String?): Double? {
        val v = iso?.toIntOrNull() ?: return null
        if (v <= 0) return null
        return ln(v / 100.0) / ln(2.0)
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = kotlin.math.abs(a)
        var y = kotlin.math.abs(b)
        while (y != 0) { val t = x % y; x = y; y = t }
        return x.coerceAtLeast(1)
    }
}
