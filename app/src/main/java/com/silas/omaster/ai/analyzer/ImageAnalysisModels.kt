package com.silas.omaster.ai.analyzer

/**
 * 亮度等级
 * 基于图像整体亮度分布划分
 */
enum class BrightnessLevel(val displayName: String, val range: String, val value: Int) {
    VERY_DARK("极暗", "0-25", 0),
    DARK("暗调", "25-50", 1),
    NORMAL("正常", "50-75", 2),
    BRIGHT("明亮", "75-90", 3),
    VERY_BRIGHT("极亮", "90-100", 4);

    companion object {
        fun fromValue(value: Int): BrightnessLevel {
            return when {
                value < 25 -> VERY_DARK
                value < 50 -> DARK
                value < 75 -> NORMAL
                value < 90 -> BRIGHT
                else -> VERY_BRIGHT
            }
        }
    }
}

/**
 * 颜色分析结果
 * 包含 RGB 分布、色调倾向、主导色等
 */
data class ColorProfile(
    // 平均 RGB 值
    val avgRed: Int,
    val avgGreen: Int,
    val avgBlue: Int,

    // 暖色调占比（R > B + 20 且 R > G 的像素比例）
    val warmthRatio: Float,

    // 冷色调占比（B > R + 20 且 B > G 的像素比例）
    val coolRatio: Float,

    // 绿色主导度（绿色通道相对于平均亮度的比值）
    val greenDominance: Float,

    // 蓝色主导度
    val blueDominance: Float,

    // 红色主导度
    val redDominance: Float,

    // 色彩丰富度（色彩变化程度）
    val colorVariance: Float,

    // 主导色调类型
    val dominantTone: DominantTone
) {
    /**
     * 获取平均亮度
     */
    val avgBrightness: Int get() = (avgRed + avgGreen + avgBlue) / 3

    /**
     * 是否暖色调主导
     */
    val isWarmDominant: Boolean get() = warmthRatio > 0.55f

    /**
     * 是否冷色调主导
     */
    val isCoolDominant: Boolean get() = coolRatio > 0.55f

    /**
     * 是否绿色主导
     */
    val isGreenDominant: Boolean get() = greenDominance > 1.25f

    /**
     * 是否蓝色主导
     */
    val isBlueDominant: Boolean get() = blueDominance > 1.2f

    /**
     * 获取色彩描述
     */
    fun getDescription(): String {
        return when {
            isWarmDominant -> "暖色调主导（${(warmthRatio * 100).toInt()}%）"
            isCoolDominant -> "冷色调主导（${(coolRatio * 100).toInt()}%）"
            isGreenDominant -> "绿色主导"
            isBlueDominant -> "蓝色主导"
            else -> "色彩均衡"
        }
    }
}

/**
 * 主导色调类型
 */
enum class DominantTone(val displayName: String) {
    WARM("暖调"),
    COOL("冷调"),
    GREEN("绿色"),
    BLUE("蓝色"),
    NEUTRAL("中性"),
    HIGH_KEY("高调"),
    LOW_KEY("低调")
}

/**
 * 纹理分析结果
 */
data class TextureProfile(
    // 边缘密度（边缘像素占比）
    val edgeDensity: Float,

    // 纹理复杂度
    val complexity: Float,

    // 清晰度评分
    val sharpnessScore: Float,

    // 纹理类型
    val textureType: TextureType
) {
    /**
     * 是否高纹理（细节丰富）
     */
    val isHighTexture: Boolean get() = edgeDensity > 0.15f

    /**
     * 是否低纹理（柔和）
     */
    val isLowTexture: Boolean get() = edgeDensity < 0.08f
}

/**
 * 纹理类型
 */
enum class TextureType(val displayName: String) {
    SMOOTH("柔和"),
    NORMAL("正常"),
    DETAILED("细节丰富"),
    HIGH_CONTRAST("高对比"),
    SOFT_GRADIENT("柔和渐变")
}