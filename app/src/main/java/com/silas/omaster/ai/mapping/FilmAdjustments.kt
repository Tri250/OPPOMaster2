package com.silas.omaster.ai.mapping

/**
 * 胶片参数微调配置表
 *
 * 把 calculateFinalParams 中原本硬编码的 3 种胶片调整表抽到独立的配置文件，
 * 让每一种推荐胶片都拥有自己的 adjustments map，避免「其他胶片都拿空 Map」的退化行为。
 *
 * 命名规范：
 * - key：胶片 ID（与 FilmPreset.id 一致）
 * - value：相对当前 HasselbladParams 的微调量（-30 ~ +30，对齐大师模式范围）
 */
object FilmAdjustments {

    /**
     * 获取指定胶片的参数微调表
     * 若胶片未配置则返回仅含 neutral（中性）条目的空表，不再返回空 Map 导致整段逻辑失效。
     */
    fun get(filmId: String): Map<String, Int> = adjustments[filmId] ?: emptyMap()

    /**
     * 已登记的胶片微调表
     *
     * 调整维度：
     * - tone        影调（暗/亮）
     * - saturation  饱和度
     * - contrast    对比度
     * - colorTemp   色温（暖/冷）
     * - sharpness   锐度
     * - vignette    暗角
     * - cyanMagenta 青品偏移
     */
    private val adjustments: Map<String, Map<String, Int>> = mapOf(
        // ─── 负片系列（柔和、肤色） ───
        "portra" to mapOf(
            "saturation" to -5,
            "contrast" to -10,
            "colorTemp" to 5,
            "sharpness" to -5
        ),
        "portra_400" to mapOf(
            "saturation" to -5,
            "contrast" to -10,
            "colorTemp" to 5,
            "sharpness" to -5
        ),
        "portra_800" to mapOf(
            "saturation" to -8,
            "contrast" to -12,
            "colorTemp" to 5,
            "tone" to -5
        ),

        // ─── 正片系列（高饱和、反转片） ───
        "rdp3" to mapOf(
            "saturation" to 15,
            "contrast" to 5,
            "sharpness" to 10,
            "colorTemp" to -3
        ),
        "velvia" to mapOf(
            "saturation" to 20,
            "contrast" to 10,
            "sharpness" to 8,
            "colorTemp" to 0
        ),

        // ─── 黑白系列（去色 + 强对比） ───
        "tx400" to mapOf(
            "saturation" to -30,
            "contrast" to 20,
            "tone" to -10,
            "sharpness" to 12
        ),
        "nh" to mapOf(
            "saturation" to -30,
            "contrast" to 25,
            "tone" to -5,
            "sharpness" to 10
        ),

        // ─── 夜景系列（高 ISO 电影感） ───
        "800t" to mapOf(
            "saturation" to 8,
            "contrast" to 15,
            "tone" to -12,
            "sharpness" to 8,
            "cyanMagenta" to -8
        ),

        // ─── 经典负片 ───
        "cc" to mapOf(
            "saturation" to 0,
            "contrast" to 0,
            "colorTemp" to 0
        ),
        "nc" to mapOf(
            "saturation" to -3,
            "contrast" to -3,
            "colorTemp" to 0,
            "sharpness" to 0
        ),

        // ─── 数字 CCD 风格（暖/冷） ───
        "ccd_warm" to mapOf(
            "saturation" to 8,
            "contrast" to 5,
            "colorTemp" to 10,
            "vignette" to 5,
            "cyanMagenta" to 5
        ),
        "ccd_cool" to mapOf(
            "saturation" to 5,
            "contrast" to 8,
            "colorTemp" to -10,
            "vignette" to 3,
            "cyanMagenta" to -5
        ),

        // ─── 黑白夜景变体 ───
        "bw_night" to mapOf(
            "saturation" to -30,
            "contrast" to 18,
            "tone" to -15,
            "sharpness" to 10
        )
    )

    /**
     * 已配置胶片 ID 集合（用于校验 / 调试）
     */
    val registeredFilmIds: Set<String> = adjustments.keys
}
