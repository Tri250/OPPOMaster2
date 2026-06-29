package com.silas.omaster.ui.features

/**
 * PixelFruit 调色参数体系
 * 对齐 PixelFruit (gitee.com/ji_annn/PixelFruit) 的 14 参数模型
 *
 * 参数范围与 PixelFruit AI调色参数文档 完全一致，
 * 便于 AI 一键调色返回的 JSON 直接反序列化应用
 */
data class PixelFruitParams(
    // ========== 基础调色 ==========
    /** 亮度 0.1~4.0, 默认 1.0 */
    val brightness: Float = 1.0f,
    /** 曝光 -2.0~2.0 EV, 默认 0.0 */
    val exposure: Float = 0.0f,
    /** 饱和度 0~300, 默认 100 */
    val saturation: Float = 100f,
    /** 对比度 -50~50, 默认 0 */
    val contrast: Float = 0f,
    /** 高光 -50~50, 默认 0 */
    val highlights: Float = 0f,
    /** 阴影 -50~50, 默认 0 */
    val shadows: Float = 0f,
    /** 白场 0~200, 默认 100 */
    val whites: Float = 100f,

    // ========== 白平衡/色调 ==========
    /** 红色调 -100~100, 默认 0 */
    val redTint: Float = 0f,
    /** 绿色调 -100~100, 默认 0 */
    val greenTint: Float = 0f,
    /** 蓝色调 -100~100, 默认 0 */
    val blueTint: Float = 0f,

    // ========== 细节处理 ==========
    /** 锐化 0~100, 默认 0 */
    val sharpness: Float = 0f,
    /** 降噪 0~100, 默认 0 */
    val noiseReduction: Float = 0f,

    // ========== 面部美化 ==========
    /** 面部美白 0~100, 默认 0 */
    val faceBrightening: Float = 0f,
    /** 过渡平滑 0~100, 默认 50 */
    val faceSmoothness: Float = 50f,

    // ========== 白平衡 ==========
    /** 色温 -100~100, 默认 0 (暖→冷) */
    val temperature: Float = 0f,
    /** 色调 -100~100, 默认 0 (绿→品) */
    val tint: Float = 0f,

    // ========== 高级光影 ==========
    /** 黑色阶 0~100, 默认 0 (黑场提升) */
    val blacks: Float = 0f,
    /** 清晰度 -100~100, 默认 0 (局部对比度增强) */
    val clarity: Float = 0f,
    /** 自然饱和度 0~200, 默认 100 (低饱和像素优先增强) */
    val vibrance: Float = 100f,

    // ========== 效果 ==========
    /** 晕影 -100~100, 默认 0 (负值=暗角, 正值=亮角) */
    val vignette: Float = 0f,
    /** 颗粒感 0~100, 默认 0 (胶片颗粒) */
    val grain: Float = 0f
) {
    /** 检测参数是否全为默认值 */
    fun isDefault(): Boolean = this == PixelFruitParams()

    /** 获取非默认参数的数量 */
    fun changedParamCount(): Int {
        var count = 0
        if (brightness != 1.0f) count++
        if (exposure != 0.0f) count++
        if (saturation != 100f) count++
        if (contrast != 0f) count++
        if (highlights != 0f) count++
        if (shadows != 0f) count++
        if (whites != 100f) count++
        if (redTint != 0f) count++
        if (greenTint != 0f) count++
        if (blueTint != 0f) count++
        if (sharpness != 0f) count++
        if (noiseReduction != 0f) count++
        if (faceBrightening != 0f) count++
        if (faceSmoothness != 50f) count++
        if (temperature != 0f) count++
        if (tint != 0f) count++
        if (blacks != 0f) count++
        if (clarity != 0f) count++
        if (vibrance != 100f) count++
        if (vignette != 0f) count++
        if (grain != 0f) count++
        return count
    }
}

/**
 * 降噪算法类型（对齐 PixelFruit Details.js）
 */
enum class NoiseReductionType(val label: String) {
    MEAN("均值滤波"),
    MEDIAN("中值滤波"),
    GAUSSIAN("高斯滤波")
}

/**
 * 滤镜预设（对齐 PixelFruit Filter.js 7 个内置预设）
 */
data class FilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val params: PixelFruitParams
)

/**
 * 内置滤镜预设
 */
object BuiltInPresets {
    val presets = listOf(
        FilterPreset(
            id = "universal",
            name = "万能公式",
            description = "通用提升，适合大部分场景",
            params = PixelFruitParams(brightness = 1.1f, saturation = 130f, highlights = -10f)
        ),
        FilterPreset(
            id = "fuji_color",
            name = "富士色彩",
            description = "模拟富士胶片色调，浓郁鲜活",
            params = PixelFruitParams(saturation = 125f, contrast = 15f, blueTint = 10f)
        ),
        FilterPreset(
            id = "retro_tower",
            name = "复古钟楼",
            description = "暖调复古，时光质感",
            params = PixelFruitParams(brightness = 1.08f, contrast = 12f, blueTint = 8f)
        ),
        FilterPreset(
            id = "retro_film",
            name = "复古胶片",
            description = "低饱和度胶片质感",
            params = PixelFruitParams(brightness = 0.9f, saturation = 110f, blueTint = -10f)
        ),
        FilterPreset(
            id = "bw",
            name = "黑白模式",
            description = "经典黑白，高对比质感",
            params = PixelFruitParams(saturation = 0f, contrast = 15f)
        ),
        FilterPreset(
            id = "cinema",
            name = "球场电影感",
            description = "低曝光高对比，电影氛围",
            params = PixelFruitParams(exposure = -0.2f, saturation = 85f, contrast = 18f, highlights = -12f)
        ),
        FilterPreset(
            id = "portrait",
            name = "人像",
            description = "提亮美白，柔和自然",
            params = PixelFruitParams(brightness = 1.15f, exposure = 0.3f, faceBrightening = 40f, faceSmoothness = 70f)
        ),
        FilterPreset(
            id = "vivid",
            name = "鲜艳模式",
            description = "高饱和度，色彩鲜明生动",
            params = PixelFruitParams(saturation = 150f, vibrance = 140f, contrast = 8f, sharpness = 15f)
        ),
        FilterPreset(
            id = "muted",
            name = "柔和淡雅",
            description = "低饱和度，温柔宁静",
            params = PixelFruitParams(saturation = 75f, vibrance = 80f, contrast = -8f, blacks = 5f)
        ),
        FilterPreset(
            id = "golden_hour",
            name = "黄金时刻",
            description = "暖色调，模拟日出日落",
            params = PixelFruitParams(temperature = 30f, saturation = 120f, vibrance = 130f, highlights = -10f, shadows = 10f, vignette = -20f)
        ),
        FilterPreset(
            id = "cool_blue",
            name = "冷蓝调",
            description = "冷色调，宁静氛围",
            params = PixelFruitParams(temperature = -25f, tint = 10f, saturation = 90f, contrast = 5f, blacks = 5f)
        ),
        FilterPreset(
            id = "film_grain",
            name = "胶片颗粒",
            description = "复古胶片颗粒感",
            params = PixelFruitParams(saturation = 85f, contrast = 10f, grain = 25f, vignette = -15f, blacks = 8f)
        )
    )
}

/**
 * 编辑面板 Tab 页
 */
enum class EditTab(val label: String, val icon: String) {
    COLOR("调色", "palette"),
    DETAIL("细节", "tune"),
    FILTER("滤镜", "filter"),
    AI("AI", "auto_awesome")
}

/**
 * 智能优化输出参数（兼容 AppNavigation 回调）
 * 将 PixelFruit 14 参数映射为旧的 6 维参数体系
 */
data class OptimizeParams(
    val hdrEnabled: Boolean = false,
    val hdrStrength: Float = 0f,
    val noiseReductionEnabled: Boolean = false,
    val noiseReductionStrength: Float = 0f,
    val sharpenEnabled: Boolean = false,
    val sharpenStrength: Float = 0f,
    val exposureAdjustment: Float = 0f,
    val colorCorrectionEnabled: Boolean = false,
    val colorCorrectionStrength: Float = 0f
) {
    /** 非零参数数量 */
    fun nonZeroCount(): Int {
        var count = 0
        if (hdrEnabled) count++
        if (noiseReductionEnabled) count++
        if (sharpenEnabled) count++
        if (exposureAdjustment != 0f) count++
        if (colorCorrectionEnabled) count++
        return count
    }
}

/**
 * 裁剪/旋转信息
 */
data class CropRotateInfo(
    /** 旋转角度 0, 90, 180, 270 */
    val rotation: Int = 0,
    /** 水平翻转 */
    val flipHorizontal: Boolean = false,
    /** 裁剪区域 (left, top, right, bottom) 归一化 0~1 */
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
) {
    fun isCropped(): Boolean = cropLeft != 0f || cropTop != 0f || cropRight != 1f || cropBottom != 1f
    fun isRotated(): Boolean = rotation != 0 || flipHorizontal
    fun isChanged(): Boolean = isCropped() || isRotated()
}
