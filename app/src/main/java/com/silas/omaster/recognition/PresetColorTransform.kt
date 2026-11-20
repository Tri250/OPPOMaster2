package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset

/**
 * 预设 → 色彩变换矩阵（拍后套色核心，纯 JVM 计算，可单测）。
 *
 * 把 MasterPreset 的调色参数（饱和度/冷暖/青品/影调/对比）转换为 4x5 ColorMatrix，
 * 用于拍后套色与实时预览。这是"风格模拟"的实现，非哈苏原厂算法（PRD 边界声明）。
 */
object PresetColorTransform {

    /** 20 项 4x5 颜色矩阵（行主序），null 参数按中性处理 */
    fun buildColorMatrix(preset: MasterPreset): FloatArray {
        val saturation = (preset.saturation ?: 0).coerceIn(-100, 100) / 100f
        val warmCool = (preset.warmCool ?: 0).coerceIn(-100, 100) / 100f
        val cyanMagenta = (preset.cyanMagenta ?: 0).coerceIn(-100, 100) / 100f
        val tone = (preset.tone ?: 0).coerceIn(-100, 100) / 100f
        val contrast = 1f + tone * 0.5f

        // 基础：对比度 * (1 + 饱和度影响)
        val satFactor = 1f + saturation * 0.8f
        // 亮度偏移（tone 正 → 略提亮）
        val lift = tone * 18f

        val m = FloatArray(20)
        // 灰度亮度权重（Rec.709）
        val lr = 0.2126f; val lg = 0.7152f; val lb = 0.0722f

        // R 行
        m[0] = contrast * (lr + satFactor * (1 - lr)) + warmCool * 0.12f
        m[1] = contrast * (lg - satFactor * lg)
        m[2] = contrast * (lb - satFactor * lb) - warmCool * 0.12f
        m[3] = 0f; m[4] = lift + warmCool * 10f

        // G 行
        m[5] = contrast * (lr - satFactor * lr)
        m[6] = contrast * (lg + satFactor * (1 - lg))
        m[7] = contrast * (lb - satFactor * lb)
        m[8] = 0f; m[9] = lift

        // B 行
        m[10] = contrast * (lr - satFactor * lr) - warmCool * 0.10f + cyanMagenta * 0.08f
        m[11] = contrast * (lg - satFactor * lg) - cyanMagenta * 0.05f
        m[12] = contrast * (lb + satFactor * (1 - lb)) + warmCool * 0.10f - cyanMagenta * 0.08f
        m[13] = 0f; m[14] = lift - warmCool * 10f

        // A 行
        m[15] = 0f; m[16] = 0f; m[17] = 0f; m[18] = 1f; m[19] = 0f
        return m
    }

    /** 该预设是否会产生可见的色彩变化（用于提示"滤镜已套用"） */
    fun hasVisibleEffect(preset: MasterPreset): Boolean {
        return listOf(preset.saturation, preset.warmCool, preset.cyanMagenta, preset.tone)
            .any { it != null && it != 0 }
    }
}
