package com.silas.omaster.ai.analyzer

import com.silas.omaster.ai.model.SceneProfile

/**
 * EXIF 元数据
 * 用于场景推断的辅助信息
 */
data class ExifData(
    // ISO 感光度
    val iso: Int? = null,

    // 快门速度（秒）
    val shutterSpeed: Float? = null,

    // 光圈值
    val aperture: Float? = null,

    // 曝光补偿
    val exposureBias: Float? = null,

    // 焦距（mm）
    val focalLength: Float? = null,

    // 闪光灯是否开启
    val flashEnabled: Boolean = false,

    // 白平衡模式
    val whiteBalance: String? = null,

    // 拍摄时间（Unix 时间戳）
    val captureTime: Long? = null,

    // GPS 纬度
    val latitude: Double? = null,

    // GPS 经度
    val longitude: Double? = null,

    // 设备型号
    val deviceModel: String? = null,

    // 图像宽度
    val imageWidth: Int? = null,

    // 图像高度
    val imageHeight: Int? = null
) {
    /**
     * 是否高 ISO（暗光环境）
     */
    val isHighISO: Boolean get() = iso != null && iso > 800

    /**
     * 是否低 ISO（充足光线）
     */
    val isLowISO: Boolean get() = iso != null && iso < 200

    /**
     * 是否使用闪光灯
     */
    val isFlashUsed: Boolean get() = flashEnabled

    /**
     * 是否广角镜头
     */
    val isWideAngle: Boolean get() = focalLength != null && focalLength < 24f

    /**
     * 是否长焦镜头
     */
    val isTelephoto: Boolean get() = focalLength != null && focalLength > 85f

    /**
     * 是否微距拍摄
     */
    val isMacro: Boolean get() = focalLength != null && focalLength < 5f

    /**
     * 获取拍摄时段
     */
    val captureHour: Int? get() {
        if (captureTime == null) return null
        val hour = ((captureTime / 3600) % 24).toInt()
        return if (hour < 0) hour + 24 else hour
    }

    /**
     * 是否日落时段（17:00-19:00）
     */
    val isSunsetTime: Boolean get() {
        val hour = captureHour ?: return false
        return hour in 17..19
    }

    /**
     * 是否夜景时段（20:00-6:00）
     */
    val isNightTime: Boolean get() {
        val hour = captureHour ?: return false
        return hour >= 20 || hour <= 6
    }

    /**
     * 是否白天时段（7:00-17:00）
     */
    val isDayTime: Boolean get() {
        val hour = captureHour ?: return false
        return hour in 7..17
    }

    /**
     * 获取 EXIF 信息摘要
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        iso?.let { parts.add("ISO $it") }
        shutterSpeed?.let { parts.add("${it}s") }
        aperture?.let { parts.add("f/$it") }
        focalLength?.let { parts.add("${it}mm") }
        if (flashEnabled) parts.add("闪光灯")
        return parts.joinToString(" · ")
    }
}

/**
 * 场景候选
 * 用于多特征投票机制
 */
data class SceneCandidate(
    // 场景 ID
    val sceneId: String,

    // 置信度（0-1）
    val confidence: Float,

    // 投票来源
    val voteSource: VoteSource,

    // 投票权重
    val weight: Float = 1.0f
) {
    /**
     * 加权置信度
     */
    val weightedConfidence: Float get() = confidence * weight
}

/**
 * 投票来源
 * 标识场景推断的特征来源
 */
enum class VoteSource(val displayName: String, val weight: Float) {
    COLOR_ANALYSIS("颜色分析", 0.30f),
    BRIGHTNESS_ANALYSIS("亮度分析", 0.20f),
    FACE_DETECTION("人脸检测", 0.25f),
    EXIF_DATA("EXIF数据", 0.15f),
    TEXTURE_ANALYSIS("纹理分析", 0.10f)
}

/**
 * 综合分析结果
 * 包含主场景、备选场景及详细分析数据
 */
data class AnalysisResult(
    // 主场景
    val primaryScene: SceneProfile,

    // 主场景置信度
    val confidence: Float,

    // Top-3 备选场景
    val alternativeScenes: List<SceneProfile>,

    // 颜色分析结果
    val colorProfile: ColorProfile,

    // 亮度等级
    val brightnessLevel: BrightnessLevel,

    // 检测到的人脸数量
    val faceCount: Int,

    // 纹理分析结果
    val textureProfile: TextureProfile? = null,

    // EXIF 数据
    val exifData: ExifData? = null,

    // 分析耗时（毫秒）
    val analysisTimeMs: Long,

    // 分析详情（用于调试）
    val analysisDetails: Map<String, Any> = emptyMap()
) {
    /**
     * 获取置信度百分比
     */
    val confidencePercent: Int get() = (confidence * 100).toInt()

    /**
     * 是否高置信度
     */
    val isHighConfidence: Boolean get() = confidence >= 0.70f

    /**
     * 是否中等置信度
     */
    val isMediumConfidence: Boolean get() = confidence in 0.50f..0.70f

    /**
     * 是否低置信度
     */
    val isLowConfidence: Boolean get() = confidence < 0.50f

    /**
     * 获取分析摘要
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        parts.add("主场景: ${primaryScene.name} (${confidencePercent}%)")
        parts.add("颜色: ${colorProfile.getDescription()}")
        parts.add("亮度: ${brightnessLevel.displayName}")
        if (faceCount > 0) parts.add("人脸: $faceCount")
        parts.add("耗时: ${analysisTimeMs}ms")
        return parts.joinToString("\n")
    }
}