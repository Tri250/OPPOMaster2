package com.silas.omaster.tflite.models

import com.silas.omaster.tflite.*

/**
 * 质量指标数据类
 * 
 * 包含图像质量评估的所有详细指标
 * 用于质量分析器内部传递和存储质量评估结果
 * 
 * @property brightnessDistribution 亮度分布数据
 * @property contrastMetrics 对比度指标
 * @property noiseMetrics 噪点指标
 * @property blurMetrics 模糊指标
 */
data class QualityMetrics(
    val brightnessDistribution: BrightnessDistribution = BrightnessDistribution(),
    val contrastMetrics: ContrastMetrics = ContrastMetrics(),
    val noiseMetrics: NoiseMetrics = NoiseMetrics(),
    val blurMetrics: BlurMetrics = BlurMetrics()
) {
    /**
     * 计算总体质量评分
     * 
     * 综合各项指标计算一个总体评分
     */
    fun calculateOverallScore(): Float {
        // 亮度评分（基于分布均衡度）
        val brightnessScore = calculateBrightnessScore()
        
        // 对比度评分
        val contrastScore = calculateContrastScore()
        
        // 噪点评分（反向，噪点越少评分越高）
        val noiseScore = 100f - noiseMetrics.estimatedNoise.coerceIn(0f, 100f)
        
        // 清晰度评分（反向，模糊越少评分越高）
        val sharpnessScore = 100f - blurMetrics.blurScore.coerceIn(0f, 100f)
        
        // 加权平均
        return (brightnessScore * 0.2f + 
                contrastScore * 0.25f + 
                noiseScore * 0.25f + 
                sharpnessScore * 0.3f)
    }
    
    /**
     * 计算亮度评分
     */
    private fun calculateBrightnessScore(): Float {
        // 理想亮度分布：中间调占比约50%，阴影和高光各约25%
        val idealMidtones = 0.5f
        val idealShadows = 0.25f
        val idealHighlights = 0.25f
        
        // 计算与理想分布的偏差
        val midtoneDeviation = kotlin.math.abs(brightnessDistribution.midtones - idealMidtones)
        val shadowDeviation = kotlin.math.abs(brightnessDistribution.shadows - idealShadows)
        val highlightDeviation = kotlin.math.abs(brightnessDistribution.highlights - idealHighlights)
        
        // 总偏差（最大1.0）
        val totalDeviation = (midtoneDeviation + shadowDeviation + highlightDeviation) / 2f
        
        // 评分（偏差越小评分越高）
        return (1f - totalDeviation) * 100f
    }
    
    /**
     * 计算对比度评分
     */
    private fun calculateContrastScore(): Float {
        // 综合全局对比度和局部对比度
        val globalWeight = 0.6f
        val localWeight = 0.4f
        
        // 理想对比度范围：40-60
        val idealGlobalContrast = 50f
        val idealLocalContrast = 40f
        
        // 计算偏差
        val globalDeviation = kotlin.math.abs(contrastMetrics.globalContrast - idealGlobalContrast) / 100f
        val localDeviation = kotlin.math.abs(contrastMetrics.localContrast - idealLocalContrast) / 100f
        
        // 评分
        val globalScore = 1f - globalDeviation.coerceIn(0f, 1f)
        val localScore = 1f - localDeviation.coerceIn(0f, 1f)
        
        return (globalScore * globalWeight + localScore * localWeight) * 100f
    }
    
    /**
     * 获取质量诊断信息
     */
    fun getDiagnosticInfo(): QualityDiagnosticInfo {
        val issues = mutableListOf<QualityIssue>()
        
        // 检查亮度问题
        if (brightnessDistribution.shadows > 0.4f) {
            issues.add(QualityIssue(
                type = QualityIssueType.LOW_BRIGHTNESS,
                severity = IssueSeverity.WARNING,
                message = "图像偏暗，阴影区域占比过高"
            ))
        }
        
        if (brightnessDistribution.highlights > 0.4f) {
            issues.add(QualityIssue(
                type = QualityIssueType.HIGH_BRIGHTNESS,
                severity = IssueSeverity.WARNING,
                message = "图像偏亮，高光区域占比过高"
            ))
        }
        
        if (brightnessDistribution.stdDeviation < 30f) {
            issues.add(QualityIssue(
                type = QualityIssueType.LOW_DYNAMIC_RANGE,
                severity = IssueSeverity.INFO,
                message = "动态范围较小，图像可能显得平淡"
            ))
        }
        
        // 检查对比度问题
        if (contrastMetrics.globalContrast < 30f) {
            issues.add(QualityIssue(
                type = QualityIssueType.LOW_CONTRAST,
                severity = IssueSeverity.WARNING,
                message = "对比度偏低，图像可能缺乏层次感"
            ))
        }
        
        if (contrastMetrics.globalContrast > 80f) {
            issues.add(QualityIssue(
                type = QualityIssueType.HIGH_CONTRAST,
                severity = IssueSeverity.INFO,
                message = "对比度较高，可能丢失细节"
            ))
        }
        
        // 检查噪点问题
        if (noiseMetrics.estimatedNoise > 20f) {
            issues.add(QualityIssue(
                type = QualityIssueType.HIGH_NOISE,
                severity = if (noiseMetrics.estimatedNoise > 40f) IssueSeverity.ERROR else IssueSeverity.WARNING,
                message = "图像噪点较多，建议使用降噪处理"
            ))
        }
        
        // 检查模糊问题
        if (blurMetrics.isBlurred) {
            issues.add(QualityIssue(
                type = QualityIssueType.BLUR,
                severity = if (blurMetrics.blurScore > 70f) IssueSeverity.ERROR else IssueSeverity.WARNING,
                message = "图像存在模糊，可能影响清晰度"
            ))
        }
        
        return QualityDiagnosticInfo(
            overallScore = calculateOverallScore(),
            issues = issues,
            brightnessScore = calculateBrightnessScore(),
            contrastScore = calculateContrastScore(),
            noiseScore = 100f - noiseMetrics.estimatedNoise.coerceIn(0f, 100f),
            sharpnessScore = 100f - blurMetrics.blurScore.coerceIn(0f, 100f)
        )
    }
    
    /**
     * 判断是否需要质量改进
     */
    fun needsImprovement(): Boolean {
        val overallScore = calculateOverallScore()
        return overallScore < 70f
    }
    
    /**
     * 获取改进建议优先级
     */
    fun getImprovementPriority(): List<QualityImprovementAction> {
        val actions = mutableListOf<QualityImprovementAction>()
        
        // 按影响程度排序
        val diagnostic = getDiagnosticInfo()
        
        // 模糊问题优先级最高
        if (blurMetrics.isBlurred && blurMetrics.blurScore > 50f) {
            actions.add(QualityImprovementAction(
                type = ImprovementType.SHARPEN,
                priority = 1,
                suggestedValue = 30f,
                description = "使用锐化处理改善清晰度"
            ))
        }
        
        // 噪点问题
        if (noiseMetrics.estimatedNoise > 15f) {
            actions.add(QualityImprovementAction(
                type = ImprovementType.NOISE_REDUCTION,
                priority = 2,
                suggestedValue = noiseMetrics.estimatedNoise * 1.5f,
                description = "使用降噪处理减少噪点"
            ))
        }
        
        // 对比度问题
        if (contrastMetrics.globalContrast < 40f) {
            actions.add(QualityImprovementAction(
                type = ImprovementType.CONTRAST,
                priority = 3,
                suggestedValue = 50f - contrastMetrics.globalContrast,
                description = "增加对比度改善层次感"
            ))
        }
        
        // 亮度问题
        if (brightnessDistribution.shadows > 0.35f) {
            actions.add(QualityImprovementAction(
                type = ImprovementType.BRIGHTNESS,
                priority = 4,
                suggestedValue = 10f,
                description = "增加曝光改善亮度"
            ))
        }
        
        return actions.sortedBy { it.priority }
    }
}

/**
 * 质量诊断信息
 */
data class QualityDiagnosticInfo(
    val overallScore: Float,
    val issues: List<QualityIssue>,
    val brightnessScore: Float,
    val contrastScore: Float,
    val noiseScore: Float,
    val sharpnessScore: Float
)

/**
 * 质量问题
 */
data class QualityIssue(
    val type: QualityIssueType,
    val severity: IssueSeverity,
    val message: String
)

/**
 * 质量问题类型
 */
enum class QualityIssueType {
    LOW_BRIGHTNESS,      // 亮度偏低
    HIGH_BRIGHTNESS,     // 亮度偏高
    LOW_DYNAMIC_RANGE,   // 动态范围小
    LOW_CONTRAST,        // 对比度低
    HIGH_CONTRAST,       // 对比度高
    HIGH_NOISE,          // 噪点多
    BLUR,                // 模糊
    OVEREXPOSED,         // 过曝
    UNDEREXPOSED         // 曝光不足
}

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    INFO,     // 信息提示
    WARNING,  // 警告
    ERROR     // 错误（严重影响）
}

/**
 * 质量改进动作
 */
data class QualityImprovementAction(
    val type: ImprovementType,
    val priority: Int,
    val suggestedValue: Float,
    val description: String
)

/**
 * 改进类型
 */
enum class ImprovementType {
    BRIGHTNESS,         // 亮度调整
    CONTRAST,           // 对比度调整
    SHARPEN,            // 锐化
    NOISE_REDUCTION,    // 降噪
    EXPOSURE,           // 曝光调整
    SHADOWS,            // 阴影调整
    HIGHLIGHTS          // 高光调整
}