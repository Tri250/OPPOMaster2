package com.silas.omaster.ai.model

import kotlinx.serialization.Serializable

/**
 * 柔光模式
 * 对应 OPPO 大师模式柔光效果
 */
enum class SoftLightMode(val displayName: String, val intensity: Int) {
    NONE("无", 0),
    SOFT("柔美", 50),
    DREAMY("梦幻", 100)
}

/**
 * 哈苏大师参数
 * 对齐 OPPO 大师模式真实参数范围
 * 所有参数范围: -30 ~ +30
 */
@Serializable
data class HasselbladParams(
    // 影调：控制整体明暗对比，负值偏暗，正值偏亮
    val tone: Int = 0,           // -30 ~ +30

    // 饱和度：控制色彩鲜艳程度
    val saturation: Int = 0,     // -30 ~ +30

    // 对比度：控制画面明暗对比强度
    val contrast: Int = 0,       // -30 ~ +30

    // 色温：负值偏冷（蓝调），正值偏暖（黄调）
    val colorTemp: Int = 0,      // -30 ~ +30

    // 锐度：控制画面清晰度和细节
    val sharpness: Int = 0,      // -30 ~ +30

    // 暗角：负值减少暗角，正值增加暗角
    val vignette: Int = 0,       // -30 ~ +30

    // 青品调：负值偏青，正值偏品红
    val cyanMagenta: Int = 0,    // -30 ~ +30

    // 柔光模式：无/柔美/梦幻
    val softLight: SoftLightMode = SoftLightMode.NONE
) {
    /**
     * 验证参数范围
     */
    fun validate(): Boolean {
        return tone in -30..30 &&
                saturation in -30..30 &&
                contrast in -30..30 &&
                colorTemp in -30..30 &&
                sharpness in -30..30 &&
                vignette in -30..30 &&
                cyanMagenta in -30..30
    }

    /**
     * 转换为 MasterPreset 可用的参数格式
     */
    fun toMasterPresetParams(): Map<String, String> {
        return mapOf(
            "tone" to tone.toString(),
            "saturation" to saturation.toString(),
            "contrast" to contrast.toString(),
            "colorTemp" to colorTemp.toString(),
            "sharpness" to sharpness.toString(),
            "vignette" to if (vignette > 0) "开" else "关",
            "cyanMagenta" to cyanMagenta.toString(),
            "softLight" to softLight.displayName
        )
    }

    /**
     * 格式化显示参数
     */
    fun formatDisplay(): List<Pair<String, String>> {
        val params = mutableListOf<Pair<String, String>>()

        if (tone != 0) params.add("影调" to formatSigned(tone))
        if (saturation != 0) params.add("饱和度" to formatSigned(saturation))
        if (contrast != 0) params.add("对比度" to formatSigned(contrast))
        if (colorTemp != 0) params.add("色温" to formatSigned(colorTemp))
        if (sharpness != 0) params.add("锐度" to "$sharpness")
        if (vignette != 0) params.add("暗角" to if (vignette > 0) "开" else "关")
        if (cyanMagenta != 0) params.add("青品调" to formatSigned(cyanMagenta))
        if (softLight != SoftLightMode.NONE) params.add("柔光" to softLight.displayName)

        return params
    }

    private fun formatSigned(value: Int): String {
        return if (value >= 0) "+$value" else "$value"
    }

    companion object {
        /**
         * 默认参数
         */
        val DEFAULT = HasselbladParams()

        /**
         * 从参数映射创建
         */
        fun fromMap(params: Map<String, Int>, softLight: SoftLightMode = SoftLightMode.NONE): HasselbladParams {
            return HasselbladParams(
                tone = params["tone"] ?: 0,
                saturation = params["saturation"] ?: 0,
                contrast = params["contrast"] ?: 0,
                colorTemp = params["colorTemp"] ?: 0,
                sharpness = params["sharpness"] ?: 0,
                vignette = params["vignette"] ?: 0,
                cyanMagenta = params["cyanMagenta"] ?: 0,
                softLight = softLight
            )
        }
    }
}