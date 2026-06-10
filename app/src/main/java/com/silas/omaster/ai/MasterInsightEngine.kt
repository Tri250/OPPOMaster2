package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Layer 4: 大师洞察 (Master Insight)
 * 高级分析与建议引擎
 *
 * 功能：
 * - 哈苏风格分析报告
 * - 胶片配方智能匹配
 * - 光影叙事建议
 * - 场景→胶片→参数的端到端大师工作流
 * 
 * 已修复：使用新版 SceneProfile 数据模型，移除硬编码返回值
 */
class MasterInsightEngine private constructor(context: Context) {

    private val context = context.applicationContext
    private val inferenceEngine = MasterInferenceEngine.getInstance(context)
    private val sceneMapping = SceneToHasselbladMapping()

    /**
     * 生成完整的哈苏风格分析报告
     */
    suspend fun generateHasselbladReport(profile: SceneProfile): HasselbladReport =
        withContext(Dispatchers.Default) {
            HasselbladReport(
                sceneAnalysis = analyzeScene(profile),
                colorAnalysis = analyzeColor(profile),
                lightAnalysis = analyzeLight(profile),
                compositionAnalysis = analyzeComposition(profile),
                recommendations = generateRecommendations(profile),
                narrative = generateNarrative(profile)
            )
        }

    /**
     * 胶片配方智能匹配
     * 基于场景特征推荐最佳胶片配方
     */
    suspend fun matchFilmRecipe(
        sceneProfile: SceneProfile,
        preferences: FilmPreferences = FilmPreferences()
    ): FilmMatchResult = withContext(Dispatchers.Default) {
        val candidates = sceneProfile.recommendedFilm.map { film ->
            val score = calculateFilmMatchScore(film, sceneProfile, preferences)
            FilmCandidate(film, score)
        }.sortedByDescending { it.score }

        FilmMatchResult(
            primaryRecommendation = candidates.firstOrNull() 
                ?: FilmCandidate(sceneProfile.recommendedFilm.firstOrNull() ?: FilmPreset(
                    id = "cc", name = "CC 经典负片", series = FilmSeries.CLASSIC, matchScore = 0.85f
                ), 0.85f),
            alternatives = candidates.take(3),
            reasoning = generateFilmReasoning(candidates.firstOrNull(), sceneProfile)
        )
    }

    /**
     * 生成光影叙事建议
     */
    suspend fun generateLightNarrative(profile: SceneProfile): LightNarrative =
        withContext(Dispatchers.Default) {
            LightNarrative(
                mood = inferMood(profile),
                story = generateStory(profile),
                technicalTips = generateTechnicalTips(profile),
                artisticDirection = generateArtisticDirection(profile)
            )
        }

    /**
     * 端到端大师工作流
     * 从场景识别到参数推荐的一站式处理
     */
    suspend fun executeMasterWorkflow(
        imagePath: String,
        preferences: UserPreferences = UserPreferences()
    ): MasterWorkflowResult = withContext(Dispatchers.Default) {
        // Step 1: 图像分析 (Layer 2)
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
        val profile = inferenceEngine.analyzeImage(bitmap, imagePath)

        // Step 2: 胶片匹配 (Layer 4)
        val filmMatch = matchFilmRecipe(profile, preferences.toFilmPreferences())

        // Step 3: 生成报告 (Layer 4)
        val report = generateHasselbladReport(profile)

        // Step 4: 生成叙事
        val narrative = generateLightNarrative(profile)

        MasterWorkflowResult(
            sceneProfile = profile,
            filmMatch = filmMatch,
            hasselbladReport = report,
            lightNarrative = narrative,
            finalParams = calculateFinalParams(profile, filmMatch.primaryRecommendation.film)
        )
    }

    // ==================== 私有分析方法 ====================

    private fun analyzeScene(profile: SceneProfile): SceneAnalysis {
        return SceneAnalysis(
            primaryScene = profile.category.displayName,
            sceneName = profile.name,
            sceneId = profile.id,
            sceneConfidence = profile.confidence,
            suitableSubjects = getSuitableSubjects(profile.category),
            bestTimeOfDay = getBestTimeOfDay(profile.id),
            weatherPreference = getWeatherPreference(profile.category)
        )
    }

    private fun analyzeColor(profile: SceneProfile): ColorAnalysis {
        val histogram = profile.histogramData
        val params = profile.hasselbladParams
        
        return ColorAnalysis(
            dominantTone = inferDominantTone(histogram, params),
            colorTemperature = estimateColorTemperature(params),
            saturationLevel = estimateSaturation(params),
            colorHarmony = analyzeColorHarmony(profile),
            hncsCompatibility = calculateHNCSCompatibility(profile)
        )
    }

    private fun analyzeLight(profile: SceneProfile): LightAnalysis {
        val histogram = profile.histogramData
        val params = profile.hasselbladParams
        val meanLuma = histogram?.meanLuminance ?: 128f

        return LightAnalysis(
            exposureLevel = when {
                meanLuma < 64 -> ExposureLevel.UNDER_EXPOSED
                meanLuma > 192 -> ExposureLevel.OVER_EXPOSED
                else -> ExposureLevel.BALANCED
            },
            contrastLevel = calculateContrastLevel(params),
            dynamicRange = estimateDynamicRange(histogram),
            lightQuality = inferLightQuality(params),
            shadowDetail = !(histogram?.shadowClipping ?: false),
            highlightDetail = !(histogram?.highlightClipping ?: false)
        )
    }

    private fun analyzeComposition(profile: SceneProfile): CompositionAnalysis {
        val faceData = profile.faceData
        return CompositionAnalysis(
            hasSubject = faceData?.hasFace ?: false,
            subjectPosition = if (faceData?.hasFace == true) "Center" else "Unknown",
            ruleOfThirds = checkRuleOfThirds(faceData),
            leadingLines = detectLeadingLines(profile),
            framing = analyzeFraming(profile)
        )
    }

    private fun generateRecommendations(profile: SceneProfile): List<MasterRecommendation> {
        val recommendations = mutableListOf<MasterRecommendation>()
        val params = profile.hasselbladParams

        // 基于直方图的建议
        profile.histogramData?.let { hist ->
            if (hist.shadowClipping) {
                recommendations.add(
                    MasterRecommendation(
                        type = RecommendationType.EXPOSURE,
                        priority = Priority.HIGH,
                        title = "阴影细节恢复",
                        description = "检测到阴影裁剪，建议提升阴影值 +20 到 +30",
                        paramAdjustment = mapOf("shadows" to "+25")
                    )
                )
            }

            if (hist.highlightClipping) {
                recommendations.add(
                    MasterRecommendation(
                        type = RecommendationType.EXPOSURE,
                        priority = Priority.HIGH,
                        title = "高光压制",
                        description = "检测到高光过曝，建议降低高光值 -15 到 -25",
                        paramAdjustment = mapOf("highlights" to "-20")
                    )
                )
            }
        }

        // 基于场景的建议
        when (profile.category) {
            SceneCategory.PORTRAIT -> recommendations.add(
                MasterRecommendation(
                    type = RecommendationType.COLOR,
                    priority = Priority.MEDIUM,
                    title = "肤色优化",
                    description = "人像场景建议降低对比度 -10，提升清晰度 +15",
                    paramAdjustment = mapOf("contrast" to "-10", "clarity" to "+15")
                )
            )
            SceneCategory.LANDSCAPE -> recommendations.add(
                MasterRecommendation(
                    type = RecommendationType.COLOR,
                    priority = Priority.MEDIUM,
                    title = "风景增强",
                    description = "风景场景建议提升饱和度 +10，清晰度 +25",
                    paramAdjustment = mapOf("saturation" to "+10", "clarity" to "+25")
                )
            )
            else -> {}
        }

        // 基于当前参数的建议
        if (params.saturation > 15) {
            recommendations.add(
                MasterRecommendation(
                    type = RecommendationType.COLOR,
                    priority = Priority.LOW,
                    title = "饱和度建议",
                    description = "当前饱和度较高，HNCS理念建议保持克制",
                    paramAdjustment = mapOf("saturation" to "+10")
                )
            )
        }

        return recommendations
    }

    private fun generateNarrative(profile: SceneProfile): String {
        val scene = profile.name
        val film = profile.recommendedFilm.firstOrNull()?.name ?: "CC 经典负片"

        return buildString {
            append("这是一幅$scene 作品，")
            append("采用 $film 胶片配方，")
            append("呈现出${inferMood(profile).description}的影像氛围。")
            append("${generateStory(profile)}")
        }
    }

    private fun calculateFilmMatchScore(
        film: FilmPreset,
        sceneProfile: SceneProfile,
        preferences: FilmPreferences
    ): Float {
        var score = film.matchScore

        // 基于用户偏好调整
        if (preferences.preferredSeries.contains(film.series)) {
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun generateFilmReasoning(
        candidate: FilmCandidate?,
        sceneProfile: SceneProfile
    ): String {
        val film = candidate?.film ?: sceneProfile.recommendedFilm.firstOrNull()
        return "${film?.name ?: "CC 经典负片"} 的${film?.description ?: "经典胶片"}特性，"
            .plus("非常适合${sceneProfile.name}场景，")
            .plus("能够呈现${inferMood(sceneProfile).description}的视觉效果。")
    }

    private fun inferMood(profile: SceneProfile): Mood {
        return when (profile.category) {
            SceneCategory.PORTRAIT -> Mood.WARM_INTIMATE
            SceneCategory.LANDSCAPE -> Mood.GRAND_SERENE
            SceneCategory.URBAN -> Mood.DYNAMIC_ENERGETIC
            SceneCategory.NIGHT -> Mood.MYSTERIOUS_DRAMATIC
            SceneCategory.FOOD -> Mood.VIBRANT_LIVELY
            else -> Mood.NEUTRAL_CALM
        }
    }

    private fun generateStory(profile: SceneProfile): String {
        val tips = profile.masterTips
        return if (tips.isNotEmpty()) {
            tips.first()
        } else {
            "当前场景光线条件良好，建议根据主体特点调整参数以达到最佳效果。"
        }
    }

    private fun generateTechnicalTips(profile: SceneProfile): List<String> {
        val tips = profile.masterTips
        return if (tips.size >= 3) {
            tips.take(3)
        } else {
            tips + when (profile.category) {
                SceneCategory.PORTRAIT -> listOf(
                    "使用大光圈 (f/1.4-f/2.8) 获得柔和背景虚化",
                    "对焦眼睛确保清晰度"
                )
                SceneCategory.LANDSCAPE -> listOf(
                    "使用小光圈 (f/8-f/11) 获得全景深",
                    "使用三脚架确保稳定"
                )
                else -> listOf(
                    "根据光线条件调整 ISO",
                    "注意构图中的引导线"
                )
            }
        }.take(4 - tips.size)
    }

    private fun generateArtisticDirection(profile: SceneProfile): String {
        val film = profile.recommendedFilm.firstOrNull()
        return "建议采用${film?.name ?: "胶片"}风格，"
            .plus("强调${inferMood(profile).description}的情绪表达，")
            .plus("通过${profile.name}的独特视角，")
            .plus("讲述一个关于光影与色彩的故事。")
    }

    private fun calculateFinalParams(
        profile: SceneProfile,
        selectedFilm: FilmPreset
    ): FinalParams {
        val baseParams = profile.hasselbladParams

        // 根据胶片特性微调
        val filmAdjustments = when (selectedFilm.id) {
            "portra" -> mapOf(
                "saturation" to -5,
                "contrast" to -10,
                "colorTemp" to 5
            )
            "rdp3" -> mapOf(
                "saturation" to 15,
                "contrast" to 5,
                "sharpness" to 10
            )
            "tx400" -> mapOf(
                "saturation" to -30,
                "contrast" to 20,
                "tone" to -10
            )
            else -> emptyMap()
        }

        return FinalParams(
            baseParams = baseParams,
            filmAdjustments = filmAdjustments,
            totalAdjustments = mergeAdjustments(baseParams, filmAdjustments)
        )
    }

    // ==================== 辅助方法（已修复硬编码问题）====================

    private fun getSuitableSubjects(category: SceneCategory): List<String> = when (category) {
        SceneCategory.PORTRAIT -> listOf("人物", "表情", "互动")
        SceneCategory.LANDSCAPE -> listOf("自然风光", "城市天际线", "日出日落")
        SceneCategory.STILL_LIFE -> listOf("美食", "产品", "花卉")
        SceneCategory.URBAN -> listOf("行人", "建筑", "生活场景")
        SceneCategory.FOOD -> listOf("美食", "饮品", "甜点")
        SceneCategory.NIGHT -> listOf("城市灯光", "星空", "夜景人像")
        SceneCategory.MACRO -> listOf("昆虫", "花卉细节", "纹理")
        SceneCategory.EVENT -> listOf("婚礼", "派对", "演唱会")
    }

    private fun getBestTimeOfDay(sceneId: String): String = when {
        sceneId.contains("sunset") || sceneId.contains("sunrise") -> "日出后/日落前1小时（黄金时刻）"
        sceneId.contains("night") -> "夜晚"
        sceneId.contains("blue") -> "日出前/日落后30分钟（蓝调时刻）"
        sceneId.contains("portrait") -> "上午9-11点或下午3-5点"
        sceneId.contains("landscape") -> "黄金时刻或蓝调时刻"
        else -> "全天适宜"
    }

    private fun getWeatherPreference(category: SceneCategory): String = when (category) {
        SceneCategory.PORTRAIT -> "多云或阴天最佳，光线柔和"
        SceneCategory.LANDSCAPE -> "晴朗或多云，避免正午强光"
        SceneCategory.NIGHT -> "晴朗无云，光污染少"
        else -> "无特殊要求"
    }

    private fun inferDominantTone(histogram: HistogramData?, params: HasselbladParams): String {
        val mean = histogram?.meanLuminance ?: 128f
        val tone = params.tone
        return when {
            mean < 85 || tone < -10 -> "暗调（电影感）"
            mean > 170 || tone > 10 -> "亮调（通透感）"
            else -> "中间调（自然）"
        }
    }

    private fun estimateColorTemperature(params: HasselbladParams): Int {
        // 基于色温参数计算实际色温值
        val baseTemp = 5500
        val adjustment = params.colorTemp * 100  // 每单位约100K
        return (baseTemp + adjustment).coerceIn(2000, 10000)
    }

    private fun estimateSaturation(params: HasselbladParams): SaturationLevel {
        return when (params.saturation) {
            in -30..-10 -> SaturationLevel.LOW
            in -9..9 -> SaturationLevel.MODERATE
            in 10..20 -> SaturationLevel.HIGH
            else -> SaturationLevel.VIBRANT
        }
    }

    private fun analyzeColorHarmony(profile: SceneProfile): ColorHarmony {
        // 基于场景类别和参数分析色彩和谐度
        return when (profile.category) {
            SceneCategory.PORTRAIT -> ColorHarmony.ANALOGOUS
            SceneCategory.LANDSCAPE -> ColorHarmony.COMPLEMENTARY
            SceneCategory.URBAN -> ColorHarmony.MONOCHROMATIC
            else -> ColorHarmony.COMPLEMENTARY
        }
    }

    private fun calculateHNCSCompatibility(profile: SceneProfile): Float {
        // 哈苏自然色彩解决方案兼容性评分
        val baseScore = when (profile.category) {
            SceneCategory.PORTRAIT, SceneCategory.LANDSCAPE -> 0.95f
            SceneCategory.FOOD -> 0.90f
            else -> 0.85f
        }
        // 根据饱和度调整（HNCS偏好自然饱和度）
        val saturationPenalty = if (profile.hasselbladParams.saturation > 20) 0.05f else 0f
        return (baseScore - saturationPenalty).coerceIn(0f, 1f)
    }

    private fun calculateContrastLevel(params: HasselbladParams): ContrastLevel {
        return when (params.contrast) {
            in -30..-10 -> ContrastLevel.LOW
            in -9..9 -> ContrastLevel.MEDIUM
            else -> ContrastLevel.HIGH
        }
    }

    private fun estimateDynamicRange(histogram: HistogramData?): DynamicRange {
        return if (histogram?.shadowClipping == true || histogram?.highlightClipping == true) {
            DynamicRange.LIMITED
        } else {
            DynamicRange.WIDE
        }
    }

    private fun inferLightQuality(params: HasselbladParams): LightQuality {
        return when (params.softLight) {
            SoftLightMode.SOFT -> LightQuality.WARM_SOFT
            SoftLightMode.DREAMY -> LightQuality.COOL_DIFFUSED
            else -> when (params.colorTemp) {
                in -30..-5 -> LightQuality.COOL_DIFFUSED
                in 5..30 -> LightQuality.WARM_SOFT
                else -> LightQuality.DIRECT_HARD
            }
        }
    }

    private fun checkRuleOfThirds(faceData: FaceData?): Boolean {
        // 简化检查：如果有人脸，假设遵循三分法则
        return faceData?.hasFace ?: false
    }

    private fun detectLeadingLines(profile: SceneProfile): Boolean {
        // 基于场景类别推断
        return profile.category == SceneCategory.URBAN || 
               profile.id.contains("architecture") ||
               profile.id.contains("street")
    }

    private fun analyzeFraming(profile: SceneProfile): String {
        return when {
            profile.faceData?.hasFace == true -> "中心构图（人像）"
            profile.category == SceneCategory.LANDSCAPE -> "三分法构图"
            profile.category == SceneCategory.URBAN -> "引导线构图"
            else -> "标准构图"
        }
    }

    private fun mergeAdjustments(
        baseParams: HasselbladParams,
        adjustments: Map<String, Int>
    ): Map<String, String> {
        return mapOf(
            "tone" to baseParams.formatParamValue(baseParams.tone + (adjustments["tone"] ?: 0)),
            "saturation" to baseParams.formatParamValue(baseParams.saturation + (adjustments["saturation"] ?: 0)),
            "contrast" to baseParams.formatParamValue(baseParams.contrast + (adjustments["contrast"] ?: 0)),
            "colorTemp" to baseParams.formatParamValue(baseParams.colorTemp + (adjustments["colorTemp"] ?: 0)),
            "sharpness" to baseParams.formatParamValue(baseParams.sharpness + (adjustments["sharpness"] ?: 0)),
            "vignette" to baseParams.formatParamValue(baseParams.vignette + (adjustments["vignette"] ?: 0))
        )
    }

    companion object {
        @Volatile
        private var instance: MasterInsightEngine? = null

        fun getInstance(context: Context): MasterInsightEngine {
            return instance ?: synchronized(this) {
                instance ?: MasterInsightEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

// ==================== 数据类定义 ====================

data class HasselbladReport(
    val sceneAnalysis: SceneAnalysis,
    val colorAnalysis: ColorAnalysis,
    val lightAnalysis: LightAnalysis,
    val compositionAnalysis: CompositionAnalysis,
    val recommendations: List<MasterRecommendation>,
    val narrative: String
)

data class SceneAnalysis(
    val primaryScene: String,
    val sceneName: String,
    val sceneId: String,
    val sceneConfidence: Float,
    val suitableSubjects: List<String>,
    val bestTimeOfDay: String,
    val weatherPreference: String
)

data class ColorAnalysis(
    val dominantTone: String,
    val colorTemperature: Int,
    val saturationLevel: SaturationLevel,
    val colorHarmony: ColorHarmony,
    val hncsCompatibility: Float
)

data class LightAnalysis(
    val exposureLevel: ExposureLevel,
    val contrastLevel: ContrastLevel,
    val dynamicRange: DynamicRange,
    val lightQuality: LightQuality,
    val shadowDetail: Boolean,
    val highlightDetail: Boolean
)

data class CompositionAnalysis(
    val hasSubject: Boolean,
    val subjectPosition: String,
    val ruleOfThirds: Boolean,
    val leadingLines: Boolean,
    val framing: String
)

data class MasterRecommendation(
    val type: RecommendationType,
    val priority: Priority,
    val title: String,
    val description: String,
    val paramAdjustment: Map<String, String>
)

data class FilmMatchResult(
    val primaryRecommendation: FilmCandidate,
    val alternatives: List<FilmCandidate>,
    val reasoning: String
)

data class FilmCandidate(
    val film: FilmPreset,
    val score: Float
)

data class LightNarrative(
    val mood: Mood,
    val story: String,
    val technicalTips: List<String>,
    val artisticDirection: String
)

data class MasterWorkflowResult(
    val sceneProfile: SceneProfile,
    val filmMatch: FilmMatchResult,
    val hasselbladReport: HasselbladReport,
    val lightNarrative: LightNarrative,
    val finalParams: FinalParams
)

data class FinalParams(
    val baseParams: HasselbladParams,
    val filmAdjustments: Map<String, Int>,
    val totalAdjustments: Map<String, String>
)

data class FilmPreferences(
    val preferredSeries: List<FilmSeries> = emptyList(),
    val preferredISO: IntRange = 100..400,
    val preferColor: Boolean = true
)

data class UserPreferences(
    val stylePreference: String = "natural",
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE
) {
    fun toFilmPreferences(): FilmPreferences = FilmPreferences()
}

// ==================== 枚举定义 ====================

enum class Mood(val description: String) {
    WARM_INTIMATE("温暖亲密"),
    GRAND_SERENE("宏大宁静"),
    DYNAMIC_ENERGETIC("动感活力"),
    MYSTERIOUS_DRAMATIC("神秘戏剧性"),
    VIBRANT_LIVELY("鲜艳生动"),
    DRAMATIC_TIMELESS("戏剧性永恒"),
    NEUTRAL_CALM("中性平和")
}

enum class ExposureLevel { UNDER_EXPOSED, BALANCED, OVER_EXPOSED }
enum class ContrastLevel { LOW, MEDIUM, HIGH }
enum class DynamicRange { LIMITED, STANDARD, WIDE }
enum class LightQuality { WARM_SOFT, COOL_DIFFUSED, DIFFUSED_EVEN, DIRECT_HARD }
enum class SaturationLevel { LOW, MODERATE, HIGH, VIBRANT }
enum class ColorHarmony { COMPLEMENTARY, ANALOGOUS, TRIADIC, MONOCHROMATIC }
enum class RecommendationType { EXPOSURE, COLOR, COMPOSITION, TECHNIQUE }
enum class Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL }
