package com.silas.omaster.recognition

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 像素风格分析器：从照片像素统计出 [StyleVector]，并匹配内置风格目录。
 *
 * 设计（对应 PRD 第八章 AI 功能专项）：
 * - 纯统计特征 + 规则，不引入重型模型，端侧低耗时（满足 P95 ≤3s）；
 * - 核心统计 [computeVectorFromSamples] 为纯 JVM 函数，可单测；
 * - 超大图先降采样（>50MB / 长边 >1600px），结果标注 compressed 提示；
 * - 输出 Top-N 候选与可解释依据，供确认页展示（Ask first，不静默落库）。
 */
object StyleAnalyzer {

    /** 候选匹配结果 */
    data class Match(
        val preset: StylePreset,
        val confidence: Float,
        /** 结构化依据标签（文案本地化交由 UI 层） */
        val reasonTags: List<FeatureTag>
    )

    /** 分析结果 */
    data class Analysis(
        val vector: StyleVector,
        val sampleCount: Int,
        val compressed: Boolean,
        val matches: List<Match>
    )

    /** 采样上限（性能与精度平衡） */
    private const val MAX_SAMPLE_PIXELS = 48 * 1024
    private const val MAX_EDGE = 1600

    /** 从 Bitmap 分析（IO/CPU 密集，切 Dispatchers.Default） */
    suspend fun analyze(bitmap: Bitmap): Analysis = withContext(Dispatchers.Default) {
        val compressed = bitmap.byteCount > 50 * 1024 * 1024 ||
                bitmap.width > MAX_EDGE || bitmap.height > MAX_EDGE
        val samples = sampleBitmap(bitmap)
        val vector = computeVectorFromSamples(samples)
        val matches = matchCatalog(vector, topN = 3)
        Analysis(vector, samples.size, compressed, matches)
    }

    /** 从 ARGB 像素样本统计风格向量（纯函数，便于 JVM 单测） */
    fun computeVectorFromSamples(samples: List<Int>): StyleVector {
        if (samples.isEmpty()) return StyleVector.NEUTRAL
        var sumB = 0f; var sumS = 0f; var sumW = 0f; var sumF = 0f
        val lumList = ArrayList<Float>(samples.size)

        for (argb in samples) {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            // 亮度（Rec.709）
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            lumList.add(lum)
            // HSV 饱和度
            val max = maxOf(r, g, b).toFloat()
            val min = minOf(r, g, b).toFloat()
            val sat = if (max == 0f) 0f else (max - min) / max
            sumB += lum
            sumS += sat
            // 冷暖：R 相对 B 的归一化差（0.5 中性）
            sumW += ((r - b).toFloat() / 255f + 1f) / 2f
            // 色彩张力：饱和且有一定亮度的像素占比
            if (sat > 0.15f && lum > 0.08f && lum < 0.95f) sumF += 1f
        }

        val n = samples.size.toFloat()
        val avgLum = (sumB / n).coerceIn(0f, 1f)
        val avgSat = (sumS / n).coerceIn(0f, 1f)
        val avgWarm = (sumW / n).coerceIn(0f, 1f)
        val colorfulness = (sumF / n).coerceIn(0f, 1f)

        // 对比度 = 亮度标准差（乘 2.5 放大到 0..1 区间）
        val avgLumD = avgLum.toDouble()
        val variance = (lumList.sumOf { (it.toDouble() - avgLumD) * (it.toDouble() - avgLumD) } / n.toDouble()).toFloat()
        val contrast = (sqrt(variance) * 2.5f).coerceIn(0f, 1f)

        // 动态范围 = 亮度 5%~95% 分位跨度
        val sorted = lumList.sorted()
        val p5 = sorted[(sorted.size * 0.05f).toInt().coerceIn(0, sorted.size - 1)]
        val p95 = sorted[(sorted.size * 0.95f).toInt().coerceIn(0, sorted.size - 1)]
        val dynamicRange = abs(p95 - p5).coerceIn(0f, 1f)

        return StyleVector(avgLum, contrast, avgSat, avgWarm, dynamicRange, colorfulness)
    }

    /** 与风格目录匹配，返回置信度 Top-N，附可解释依据标签 */
    fun matchCatalog(vector: StyleVector, topN: Int = 3): List<Match> {
        return StyleCatalog.PRESETS
            .map { preset ->
                val conf = vector.confidenceAgainst(preset.target)
                Match(preset, conf, buildReasonTags(vector))
            }
            .sortedByDescending { it.confidence }
            .take(topN)
    }

    /** 生成可解释依据标签（结构化，文案本地化交由 UI 层） */
    fun buildReasonTags(v: StyleVector): List<FeatureTag> {
        val tags = mutableListOf<FeatureTag>()
        if (v.brightness > 0.65f) tags.add(FeatureTag.BRIGHT)
        else if (v.brightness < 0.35f) tags.add(FeatureTag.DARK)
        if (v.saturation < 0.35f) tags.add(FeatureTag.LOW_SAT)
        else if (v.saturation > 0.65f) tags.add(FeatureTag.VIVID)
        if (v.warmth > 0.6f) tags.add(FeatureTag.WARM)
        else if (v.warmth < 0.4f) tags.add(FeatureTag.COOL)
        if (v.dynamicRange > 0.7f) tags.add(FeatureTag.WIDE_DR)
        return tags
    }

    /** 均匀网格采样 Bitmap，控制样本数上限 */
    private fun sampleBitmap(bitmap: Bitmap): List<Int> {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return emptyList()
        val total = w * h
        val step = maxOf(1, sqrt(total.toFloat() / MAX_SAMPLE_PIXELS).toInt())
        val out = ArrayList<Int>(MAX_SAMPLE_PIXELS)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                out.add(bitmap.getPixel(x, y))
                x += step
            }
            y += step
        }
        return out
    }
}
