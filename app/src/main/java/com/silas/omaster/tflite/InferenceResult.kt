package com.silas.omaster.tflite

/**
 * 推理结果数据类
 * 
 * 包含场景分类、质量评估和参数预测的完整结果
 * 
 * @property sceneResult 场景分类结果
 * @property qualityResult 图像质量评估结果
 * @property paramResult 参数预测结果
 * @property totalInferenceTimeMs 总推理时间（毫秒）
 * @property timestamp 结果生成时间戳
 */
data class InferenceResult(
    val sceneResult: SceneResult? = null,
    val qualityResult: QualityResult? = null,
    val paramResult: ParamResult? = null,
    val totalInferenceTimeMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 判断推理是否成功
     */
    val isSuccess: Boolean
        get() = sceneResult != null && qualityResult != null && paramResult != null
    
    /**
     * 获取错误信息
     */
    val errorMessage: String?
        get() = when {
            sceneResult == null -> "场景分类失败"
            qualityResult == null -> "质量评估失败"
            paramResult == null -> "参数预测失败"
            else -> null
        }
}

/**
 * 场景分类结果
 * 
 * @property sceneId 场景ID（如 "landscape", "portrait", "night" 等）
 * @property sceneName 场景名称（中文）
 * @property confidence 置信度（0.0-1.0）
 * @property topCandidates 候选场景列表（按置信度降序）
 * @property inferenceTimeMs 推理时间（毫秒）
 */
data class SceneResult(
    val sceneId: String,
    val sceneName: String,
    val confidence: Float,
    val topCandidates: List<SceneCandidate> = emptyList(),
    val inferenceTimeMs: Long = 0
)

/**
 * 场景候选
 * 
 * @property sceneId 场景ID
 * @property sceneName 场景名称
 * @property confidence 置信度
 */
data class SceneCandidate(
    val sceneId: String,
    val sceneName: String,
    val confidence: Float
)

/**
 * 图像质量评估结果
 * 
 * @property brightnessScore 亮度评分（0-100）
 * @property contrastScore 对比度评分（0-100）
 * @property noiseScore 噪点评分（0-100，越高表示噪点越少）
 * @property sharpnessScore 清晰度评分（0-100）
 * @property overallScore 总体质量评分（0-100）
 * @property brightnessDistribution 亮度分布数据
 * @property contrastMetrics 对比度指标
 * @property noiseMetrics 噪点指标
 * @property blurMetrics 模糊指标
 * @property inferenceTimeMs 推理时间（毫秒）
 */
data class QualityResult(
    val brightnessScore: Float,
    val contrastScore: Float,
    val noiseScore: Float,
    val sharpnessScore: Float,
    val overallScore: Float,
    val brightnessDistribution: BrightnessDistribution = BrightnessDistribution(),
    val contrastMetrics: ContrastMetrics = ContrastMetrics(),
    val noiseMetrics: NoiseMetrics = NoiseMetrics(),
    val blurMetrics: BlurMetrics = BlurMetrics(),
    val inferenceTimeMs: Long = 0
)

/**
 * 亮度分布数据
 * 
 * @property shadows 阴影区域占比（0-1）
 * @property midtones 中间调占比（0-1）
 * @property highlights 高光区域占比（0-1）
 * @property meanBrightness 平均亮度（0-255）
 * @property stdDeviation 亮度标准差
 */
data class BrightnessDistribution(
    val shadows: Float = 0f,
    val midtones: Float = 0f,
    val highlights: Float = 0f,
    val meanBrightness: Float = 0f,
    val stdDeviation: Float = 0f
)

/**
 * 对比度指标
 * 
 * @property globalContrast 全局对比度
 * @property localContrast 局部对比度
 * @property dynamicRange 动态范围
 */
data class ContrastMetrics(
    val globalContrast: Float = 0f,
    val localContrast: Float = 0f,
    val dynamicRange: Float = 0f
)

/**
 * 噪点指标
 * 
 * @property estimatedNoise 估计噪点水平
 * @property noiseType 噪点类型（高斯、椒盐等）
 * @property frequency 噪点频率
 */
data class NoiseMetrics(
    val estimatedNoise: Float = 0f,
    val noiseType: String = "unknown",
    val frequency: Float = 0f
)

/**
 * 模糊指标
 * 
 * @property blurScore 模糊评分（越高越模糊）
 * @property isBlurred 是否模糊
 * @property blurType 模糊类型（运动模糊、失焦等）
 */
data class BlurMetrics(
    val blurScore: Float = 0f,
    val isBlurred: Boolean = false,
    val blurType: String = "none"
)

/**
 * 参数预测结果
 * 
 * @property parameters 18个调校参数建议值
 * @property confidence 预测置信度
 * @property inferenceTimeMs 推理时间（毫秒）
 */
data class ParamResult(
    val parameters: HasselbladParameters,
    val confidence: Float = 0f,
    val inferenceTimeMs: Long = 0
)

/**
 * 哈苏调校参数
 * 
 * 包含18个核心调校参数，模拟哈苏大师风格
 * 
 * @property exposure 曝光补偿（-2.0 到 +2.0）
 * @property contrast 对比度（0-100）
 * @property highlights 高光（-100 到 +100）
 * @property shadows 阴影（-100 到 +100）
 * @property whites 白色色阶（-100 到 +100）
 * @property blacks 黑色色阶（-100 到 +100）
 * @property clarity 清晰度（-100 到 +100）
 * @property vibrance 自然饱和度（-100 到 +100）
 * @property saturation 饱和度（-100 到 +100）
 * @property warmth 色温（-100 到 +100，负值偏冷，正值偏暖）
 * @property tint 色调（-100 到 +100，负值偏绿，正值偏洋红）
 * @property sharpness 锐度（0-100）
 * @property noiseReduction 降噪（0-100）
 * @property vignette 暗角（0-100）
 * @property grain 颗粒感（0-100）
 * @property fade 褪色效果（0-100）
 * @property splitToneHighlights 分色调高光（0-360色相）
 * @property splitToneShadows 分色调阴影（0-360色相）
 */
data class HasselbladParameters(
    val exposure: Float = 0f,
    val contrast: Float = 50f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val clarity: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val sharpness: Float = 25f,
    val noiseReduction: Float = 25f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val fade: Float = 0f,
    val splitToneHighlights: Float = 0f,
    val splitToneShadows: Float = 0f
) {
    /**
     * 转换为数组（用于模型输入/输出）
     */
    fun toArray(): FloatArray = floatArrayOf(
        exposure, contrast, highlights, shadows, whites, blacks,
        clarity, vibrance, saturation, warmth, tint,
        sharpness, noiseReduction, vignette, grain, fade,
        splitToneHighlights, splitToneShadows
    )

    companion object {
        /**
         * 从数组创建参数对象
         */
        fun fromArray(array: FloatArray): HasselbladParameters {
            require(array.size == 18) { "参数数组必须包含18个元素" }
            return HasselbladParameters(
                exposure = array[0],
                contrast = array[1],
                highlights = array[2],
                shadows = array[3],
                whites = array[4],
                blacks = array[5],
                clarity = array[6],
                vibrance = array[7],
                saturation = array[8],
                warmth = array[9],
                tint = array[10],
                sharpness = array[11],
                noiseReduction = array[12],
                vignette = array[13],
                grain = array[14],
                fade = array[15],
                splitToneHighlights = array[16],
                splitToneShadows = array[17]
            )
        }

        /**
         * 默认参数
         */
        val DEFAULT = HasselbladParameters()
    }
}

/**
 * 推理配置
 * 
 * @property useGpu 是否使用GPU加速
 * @property useNnapi 是否使用NNAPI加速
 * @property useXnnpack 是否使用XNNPACK优化
 * @property numThreads CPU线程数
 * @property enableCache 是否启用结果缓存
 * @property cacheSize 缓存大小
 */
data class InferenceConfig(
    val useGpu: Boolean = true,
    val useNnapi: Boolean = false,
    val useXnnpack: Boolean = true,
    val numThreads: Int = 4,
    val enableCache: Boolean = true,
    val cacheSize: Int = 50
)

/**
 * 推理状态
 */
enum class InferenceState {
    IDLE,           // 空闲
    LOADING,        // 模型加载中
    READY,          // 就绪
    INFERRING,      // 推理中
    ERROR           // 错误
}

/**
 * 模型信息
 * 
 * @property name 模型名称
 * @property version 模型版本
 * @property sizeBytes 模型大小（字节）
 * @property inputShape 输入形状
 * @property outputShape 输出形状
 * @property isQuantized 是否量化模型
 */
data class ModelInfo(
    val name: String,
    val version: String = "1.0.0",
    val sizeBytes: Long = 0,
    val inputShape: IntArray = intArrayOf(),
    val outputShape: IntArray = intArrayOf(),
    val isQuantized: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelInfo
        return name == other.name && version == other.version
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}