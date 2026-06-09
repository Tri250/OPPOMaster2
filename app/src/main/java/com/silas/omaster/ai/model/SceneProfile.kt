package com.silas.omaster.ai.model

import kotlinx.serialization.Serializable

/**
 * 相机参数
 * 专业模式相机设置
 */
@Serializable
data class CameraParams(
    val iso: String? = null,           // ISO 感光度，如 "100", "Auto"
    val shutterSpeed: String? = null,  // 快门速度，如 "1/125", "Auto"
    val aperture: String? = null,      // 光圈值，如 "f/1.8"
    val exposureCompensation: String? = null, // 曝光补偿，如 "-0.3", "+0.7"
    val whiteBalance: String? = null,  // 白平衡，如 "5200K", "日光"
    val focusMode: String? = null,     // 对焦模式，如 "人脸优先", "连续对焦"
    val hdrEnabled: Boolean = false,   // HDR 开关
    val stabilizationEnabled: Boolean = false // 防抖开关
) {
    /**
     * 格式化显示参数
     */
    fun formatDisplay(): List<Pair<String, String>> {
        val params = mutableListOf<Pair<String, String>>()

        iso?.let { params.add("ISO" to it) }
        shutterSpeed?.let { params.add("快门" to it) }
        aperture?.let { params.add("光圈" to it) }
        exposureCompensation?.let { params.add("曝光" to it) }
        whiteBalance?.let { params.add("白平衡" to it) }
        focusMode?.let { params.add("对焦" to it) }
        if (hdrEnabled) params.add("HDR" to "开启")
        if (stabilizationEnabled) params.add("防抖" to "开启")

        return params
    }

    companion object {
        val AUTO = CameraParams(
            iso = "Auto",
            shutterSpeed = "Auto",
            aperture = "Auto",
            whiteBalance = "Auto"
        )
    }
}

/**
 * 统一场景数据模型
 * 将 Android 和 React 两端的场景定义统一为以下结构
 *
 * @param id 场景唯一标识符，如 "portrait-backlit"
 * @param name 场景名称，如 "逆光人像"
 * @param category 场景大类
 * @param subCategory 二级细分场景，如 "逆光人像"
 * @param description 场景描述
 * @param color 主题色（哈苏橙 0xFFFF6B35）
 * @param confidence 识别置信度（AI识别后填充）
 * @param hasselbladParams 哈苏大师参数（对齐 OPPO 大师模式参数体系）
 * @param recommendedFilm 推荐胶片风格列表（按匹配度排序）
 * @param masterTips 拍摄建议（哈苏大师风格）
 * @param cameraParams 关联的相机参数
 */
@Serializable
data class SceneProfile(
    val id: String,                        // "portrait-backlit"
    val name: String,                      // "逆光人像"
    val category: SceneCategory,           // PORTRAIT
    val subCategory: String,               // "逆光人像"
    val description: String,               // "侧逆光环境下的柔美人像..."
    val color: Long,                       // 主题色 0xFFFF6B35 (哈苏橙)
    val confidence: Float = 0f,            // 识别置信度（AI识别后填充）

    // 🔑 哈苏大师参数（对齐 OPPO 大师模式参数体系）
    val hasselbladParams: HasselbladParams,

    // 🔑 推荐胶片风格（按匹配度排序）
    val recommendedFilm: List<FilmPreset>,

    // 🔑 拍摄建议（哈苏大师风格）
    val masterTips: List<String>,

    // 关联的相机参数
    val cameraParams: CameraParams,

    // 场景特征标签
    val tags: List<String> = emptyList(),

    // 最佳拍摄时间
    val bestTime: String? = null,

    // 环境建议
    val environmentTips: String? = null
) {
    /**
     * 获取置信度百分比
     */
    val confidencePercent: Int get() = (confidence * 100).toInt()

    /**
     * 获取最高匹配胶片
     */
    val bestFilm: FilmPreset? get() = recommendedFilm.maxByOrNull { it.matchScore }

    /**
     * 获取高匹配胶片列表（> 70%）
     */
    val highMatchFilms: List<FilmPreset> get() = recommendedFilm.filter { it.isHighMatch }

    /**
     * 格式化哈苏参数显示
     */
    fun formatHasselbladParams(): String {
        val params = hasselbladParams.formatDisplay()
        return params.joinToString(" · ") { "${it.first}${it.second}" }
    }

    /**
     * 格式化胶片推荐显示
     */
    fun formatFilmRecommendation(): String {
        return recommendedFilm.take(2).joinToString(" / ") { it.displayName }
    }

    /**
     * 获取完整拍摄建议
     */
    fun getFullTips(): String {
        val tips = mutableListOf<String>()

        environmentTips?.let { tips.add("【环境建议】$it") }
        bestTime?.let { tips.add("【最佳时间】$it") }
        tips.add("【推荐胶片】${formatFilmRecommendation()}")
        tips.add("【大师参数】${formatHasselbladParams()}")

        masterTips.forEach { tip ->
            tips.add("【拍摄要点】$tip")
        }

        return tips.joinToString("\n")
    }

    companion object {
        // 哈苏橙主题色
        const val HASSELBLAD_ORANGE = 0xFFFF6B35L
    }
}