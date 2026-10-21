package com.silas.omaster.util

import com.silas.omaster.model.MasterPreset
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.cos

/**
 * 【v1.7.0 发现 2.0】AI 场景预设匹配器（端上轻量启发式）
 *
 * 用途：快拍笔记拍照/选图后，用 Palette 主色 + 色板反推"照片气质"，
 * 在预设库中匹配最合适的预设（闭环 SnapNote.matchedPresetId 死字段）。
 *
 * 设计约束：
 * - 全程本地计算，零网络、零上传（隐私合规默认项）；
 * - 不依赖机器学习模型：色温倾向 × 饱和度 × 明度 × 风格关键词 加权评分；
 * - 低于阈值返回 null（宁缺毋滥，避免"乱点鸳鸯谱"伤害产品信任）。
 *
 * 评分维度：
 * 1. 色温一致性：照片平均色相偏暖（橙/黄/红）→ 预设 warmCool 正值优先，反之负值；
 * 2. 饱和度匹配：低饱和照片（黑白/极简倾向）→ 黑白/单色/高级灰/胶片 风格优先；
 * 3. 明度匹配：暗调照片 → 夜景倾向；
 * 4. 关键词匹配：filter/tags 命中色相语义（日落→暖、蓝调→冷、森林→绿）。
 */
object PresetColorMatcher {

    /** 匹配合格阈值：低于该分数视为"没有气质相投的预设" */
    private const val MATCH_THRESHOLD = 28f

    /** 单次参与评分的候选预设上限（按收藏/新预设优先截取，控制计算量） */
    private const val MAX_CANDIDATES = 120

    /**
     * 匹配最佳预设。
     *
     * @param dominantHex 主色 #RRGGBB
     * @param swatchHexes 色板（可空）
     * @param presets 候选预设库
     * @return 匹配的预设 id；无合格候选返回 null
     */
    fun matchPreset(
        dominantHex: String,
        swatchHexes: List<String>,
        presets: List<MasterPreset>
    ): String? {
        if (presets.isEmpty()) return null
        val colors = (listOf(dominantHex) + swatchHexes).mapNotNull { parseHexOrNull(it) }
        if (colors.isEmpty()) return null

        // 照片色彩画像：色温倾向 / 平均饱和度 / 平均明度 / 主色相
        val profile = ColorProfile.from(colors)

        val candidates = presets
            .filter { !it.id.isNullOrEmpty() }
            .take(MAX_CANDIDATES)

        val best = candidates
            .map { preset -> preset to scorePreset(preset, profile) }
            .maxByOrNull { it.second }

        return if (best != null && best.second >= MATCH_THRESHOLD) best.first.id else null
    }

    // ═══════════════════════════════════════════════════════════
    // 内部实现
    // ═══════════════════════════════════════════════════════════

    /** 照片色彩画像（由色板聚合） */
    private data class ColorProfile(
        /** 色温倾向：-100（极冷）~ +100（极暖） */
        val warmth: Float,
        /** 平均饱和度 0~1 */
        val saturation: Float,
        /** 平均明度 0~1 */
        val lightness: Float,
        /** 主色相（0~360，-1 表示灰度图） */
        val dominantHue: Float
    ) {
        companion object {
            fun from(argbColors: List<Int>): ColorProfile {
                // 一次性转 HSV（h / s / v），聚合出照片色彩画像
                val hsvList = argbColors.map { argb ->
                    FloatArray(3).also { Color.colorToHSV(argb, it) }
                }
                val avgSat = hsvList.map { it[1] }.average().toFloat()
                val avgLight = hsvList.map { it[2] }.average().toFloat()
                // 主色取色板第一枚（调用方保证 dominant 在最前）
                val dominantHue = hsvList.firstOrNull()?.get(0) ?: 0f
                // 色温：以主色相为基准加权（暖色相 0~60 / 300~360 为暖，180~260 为冷）
                val warmth = hueWarmth(dominantHue) * 0.7f +
                        hueWarmth(hsvList.map { it[0] }.average().toFloat()) * 0.3f
                return ColorProfile(
                    warmth = warmth.coerceIn(-100f, 100f),
                    saturation = avgSat.coerceIn(0f, 1f),
                    lightness = avgLight.coerceIn(0f, 1f),
                    dominantHue = dominantHue
                )
            }

            /** 色相 → 色温倾向（-100 冷 ~ +100 暖） */
            private fun hueWarmth(hue: Float): Float {
                // 冷端 240（蓝）→ 暖端 30（橙）；用余弦平滑过渡
                val rad = Math.toRadians(hue.toDouble())
                return (cos(rad - Math.toRadians(30.0)) * 100).toFloat()
            }
        }
    }

    /** 预设评分（0~100+，越大越匹配） */
    private fun scorePreset(preset: MasterPreset, profile: ColorProfile): Float {
        var score = 0f

        // 1. 色温一致性（权重最高 45）
        preset.warmCool?.let { wc ->
            val presetWarmth = (wc * 100f / 100f).coerceIn(-100f, 100f)
            val diff = abs(presetWarmth - profile.warmth)
            score += (1f - diff / 200f) * 45f
        }

        // 2. 饱和度匹配（权重 25）
        val filter = preset.filter?.lowercase().orEmpty()
        val tags = preset.tags?.map { it.lowercase() } ?: emptyList()
        val isMonoStyle = listOf("黑白", "mono", "bw", "单色", "高级灰", "灰度")
            .any { filter.contains(it) || tags.any { t -> t.contains(it) } }
        if (isMonoStyle) {
            // 低饱和照片 → 黑白风格高分；高饱和照片 → 黑白低分
            score += (1f - profile.saturation) * 25f
        } else {
            // 彩色照片对非黑白风格有基础分
            score += profile.saturation * 12f
            // 高饱和照片对"明艳/浓郁/鲜艳"风格加分
            val vividStyle = listOf("明艳", "vivid", "浓郁", "rich", "鲜艳", "赛博", "cyber")
                .any { filter.contains(it) || tags.any { t -> t.contains(it) } }
            if (vividStyle && profile.saturation > 0.45f) score += 10f
        }

        // 3. 明度 / 暗调匹配（权重 15）
        val nightStyle = listOf("夜景", "night", "夜", "霓虹", "neon")
            .any { filter.contains(it) || tags.any { t -> t.contains(it) } }
        if (nightStyle && profile.lightness < 0.4f) score += 15f
        if (!nightStyle && profile.lightness > 0.4f) score += 6f

        // 4. 色相语义关键词（权重 15）
        score += hueSemanticScore(profile.dominantHue, preset)

        return score
    }

    /** 色相语义匹配：暖色相↔日落/人像/美食，冷色相↔海/天空/蓝调，绿↔森林/植物 */
    private fun hueSemanticScore(hue: Float, preset: MasterPreset): Float {
        val text = (
                listOfNotNull(preset.filter) + (preset.tags ?: emptyList())
                ).joinToString(" ").lowercase()
        if (text.isBlank()) return 0f

        var score = 0f
        // 暖（<60 或 >330）
        if (hue < 60f || hue > 330f) {
            val warmWords = listOf("日落", "sunset", "夕阳", "晚霞", "暖", "warm", "人像", "portr", "美食", "food", "秋", "autumn")
            if (warmWords.any { text.contains(it) }) score += 15f
        }
        // 冷（180~260）
        if (hue in 180f..260f) {
            val coolWords = listOf("海", "sea", "天空", "sky", "蓝", "blue", "冷", "cool", "夜", "night", "雪", "snow")
            if (coolWords.any { text.contains(it) }) score += 15f
        }
        // 绿（80~160）
        if (hue in 80f..160f) {
            val greenWords = listOf("森林", "forest", "绿", "green", "植物", "plant", "自然", "nature", "春", "spring")
            if (greenWords.any { text.contains(it) }) score += 15f
        }
        return score
    }

    private fun parseHexOrNull(hex: String): Int? {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        return runCatching { Color.parseColor("#$cleaned") }.getOrNull()
    }
}
