package com.silas.omaster.recognition

import kotlin.math.abs

/**
 * 图像风格向量（6 维，各维度归一化到 0f..1f）。
 *
 * 由 [StyleAnalyzer] 从照片像素统计得到；用于与内置风格目录做余弦/加权距离匹配。
 * 设计为纯数据类（无 Android 依赖），便于 JVM 单测与序列化。
 *
 * @param brightness 平均亮度（0=纯黑, 1=纯白）
 * @param contrast   对比度（亮度标准差归一化）
 * @param saturation 平均饱和度（HSV 的 S）
 * @param warmth     冷暖倾向（0=极冷/偏蓝, 0.5=中性, 1=极暖/偏橙）
 * @param dynamicRange 动态范围（高光与阴影分布跨度）
 * @param colorfulness 色彩丰富度（色彩张力的综合度量）
 */
data class StyleVector(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val dynamicRange: Float,
    val colorfulness: Float
) {
    init {
        require(brightness in 0f..1f && contrast in 0f..1f && saturation in 0f..1f &&
                warmth in 0f..1f && dynamicRange in 0f..1f && colorfulness in 0f..1f) {
            "StyleVector 各维度必须在 0f..1f： $this"
        }
    }

    /** 加权距离（0=完全一致，越大越不同）。权重体现人眼对色彩/明暗的敏感度。 */
    fun distance(other: StyleVector): Float {
        val wb = 1.2f  // 亮度
        val wc = 1.0f  // 对比度
        val ws = 1.3f  // 饱和度
        val ww = 1.0f  // 冷暖
        val wd = 0.7f  // 动态范围
        val wf = 0.8f  // 色彩丰富度
        val sum = wb * abs(brightness - other.brightness) +
                  wc * abs(contrast - other.contrast) +
                  ws * abs(saturation - other.saturation) +
                  ww * abs(warmth - other.warmth) +
                  wd * abs(dynamicRange - other.dynamicRange) +
                  wf * abs(colorfulness - other.colorfulness)
        val totalW = wb + wc + ws + ww + wd + wf
        return (sum / totalW).coerceIn(0f, 1f)
    }

    /** 距离 → 置信度（0..1），距离越小置信度越高 */
    fun confidenceAgainst(other: StyleVector): Float = (1f - distance(other)).coerceIn(0f, 1f)

    companion object {
        val NEUTRAL = StyleVector(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
    }
}

/**
 * 内置风格目录条目：一个风格目标向量 + 对应的 MasterPreset 参数模板。
 *
 * 说明：风格映射为"风格模拟"，不宣称哈苏原厂算法。Reno17 实况调色盘的
 * 清透/琥珀 两款原生质感被纳入目录（对应 PRD 的 Reno17 视觉锚定）。
 */
data class StylePreset(
    val key: String,
    val displayName: String,
    val description: String,
    val target: StyleVector,
    /** 该风格对应的调色参数（填入 MasterPreset） */
    val filter: String,
    val tone: Int,
    val saturation: Int,
    val warmCool: Int,
    val cyanMagenta: Int,
    val sharpness: Int,
    val softLight: String?,
    val vignette: String?
)

/**
 * 内置风格目录（风格画像样本）。真实、可解释的调色模板。
 *
 * 每个 target 向量由对该风格典型样片的统计归纳得出，作为匹配基准。
 * 目录版本用于识别结果的可复现追溯（engineVersion + catalogVersion）。
 */
object StyleCatalog {
    const val CATALOG_VERSION = "1.0.0"

    val PRESETS: List<StylePreset> = listOf(
        StylePreset(
            key = "clear", displayName = "清透", description = "高明度、低饱和、冷调通透，Reno17 原生质感",
            target = StyleVector(0.72f, 0.42f, 0.32f, 0.42f, 0.60f, 0.38f),
            filter = "清透", tone = 12, saturation = -8, warmCool = -6, cyanMagenta = 2,
            sharpness = 20, softLight = "弱", vignette = null
        ),
        StylePreset(
            key = "amber", displayName = "琥珀", description = "暖调、中等饱和、柔和影调，Reno17 原生质感",
            target = StyleVector(0.55f, 0.45f, 0.52f, 0.70f, 0.50f, 0.52f),
            filter = "琥珀", tone = 5, saturation = 10, warmCool = 14, cyanMagenta = -4,
            sharpness = 15, softLight = "中", vignette = "弱"
        ),
        StylePreset(
            key = "film", displayName = "胶片", description = "中低饱和、偏高对比、暖调怀旧颗粒感",
            target = StyleVector(0.48f, 0.62f, 0.40f, 0.62f, 0.66f, 0.42f),
            filter = "胶片", tone = -6, saturation = -14, warmCool = 10, cyanMagenta = 6,
            sharpness = 10, softLight = "弱", vignette = "中"
        ),
        StylePreset(
            key = "bw", displayName = "黑白", description = "零饱和、高对比、强调影调层次",
            target = StyleVector(0.50f, 0.75f, 0.02f, 0.50f, 0.80f, 0.05f),
            filter = "黑白", tone = 8, saturation = -100, warmCool = 0, cyanMagenta = 0,
            sharpness = 30, softLight = null, vignette = "中"
        ),
        StylePreset(
            key = "vivid", displayName = "鲜明", description = "高饱和、高色彩丰富度、通透鲜艳",
            target = StyleVector(0.55f, 0.58f, 0.80f, 0.52f, 0.70f, 0.85f),
            filter = "鲜明", tone = 10, saturation = 25, warmCool = 4, cyanMagenta = 0,
            sharpness = 25, softLight = null, vignette = null
        ),
        StylePreset(
            key = "night", displayName = "夜景", description = "低亮度、高对比、高动态范围",
            target = StyleVector(0.25f, 0.70f, 0.45f, 0.55f, 0.88f, 0.40f),
            filter = "夜景", tone = -10, saturation = 5, warmCool = 6, cyanMagenta = 0,
            sharpness = 20, softLight = null, vignette = "强"
        ),
        StylePreset(
            key = "soft", displayName = "柔光", description = "高明度、低对比、朦胧柔和",
            target = StyleVector(0.70f, 0.28f, 0.38f, 0.55f, 0.35f, 0.32f),
            filter = "柔光", tone = 6, saturation = -6, warmCool = 2, cyanMagenta = 0,
            sharpness = 5, softLight = "强", vignette = "弱"
        ),
        StylePreset(
            key = "original", displayName = "原图", description = "中性无风格化，作为兜底候选",
            target = StyleVector.NEUTRAL,
            filter = "原图", tone = 0, saturation = 0, warmCool = 0, cyanMagenta = 0,
            sharpness = 0, softLight = null, vignette = null
        )
    )

    fun byKey(key: String): StylePreset? = PRESETS.find { it.key == key }
}
