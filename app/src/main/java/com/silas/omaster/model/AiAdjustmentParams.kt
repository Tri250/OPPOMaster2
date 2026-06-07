package com.silas.omaster.model

/**
 * AI 微调参数
 * 用于 AI 样张智能优化
 */
data class AiAdjustmentParams(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val clarity: Float = 0f,
    val vignette: Float = 0f
) {
    companion object {
        val DEFAULT = AiAdjustmentParams()
    }
    
    fun toDisplayMap(): Map<String, Float> = mapOf(
        "亮度" to brightness,
        "对比度" to contrast,
        "饱和度" to saturation,
        "色温" to warmth,
        "色调" to tint,
        "高光" to highlights,
        "阴影" to shadows,
        "清晰度" to clarity,
        "暗角" to vignette
    )
}