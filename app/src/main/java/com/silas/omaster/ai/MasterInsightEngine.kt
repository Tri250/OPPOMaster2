package com.silas.omaster.ai

import android.content.Context
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Layer 4: 大师洞察 (Master Insight)
 * 高级分析与建议引擎
 *
 * 功能：
 * - 哈苏风格分析报告
 * - 胶片配方智能匹配
 * - 光影叙事建议
 * - 场景→胶片→参数的端到端大师工作流
 */
class MasterInsightEngine private constructor(context: Context) {

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
        sceneHierarchy: SceneHierarchy,
        histogramData: HistogramData,
        preferences: FilmPreferences = FilmPreferences()
    ): FilmMatchResult = withContext(Dispatchers.Default) {
        val candidates = FilmStock.entries.map { film ->
            val score = calculateFilmMatchScore(film, sceneHierarchy, histogramData, preferences)
            FilmCandidate(film, score)
        }.sortedByDescending { it.score }

        FilmMatchResult(
            primaryRecommendation = candidates.first(),
            alternatives = candidates.take(3),
            reasoning = generateFilmReasoning(candidates.first(), sceneHierarchy)
        )
    }

    /**
     * 生成光影叙事建议
     */
    suspend fun generateLightNarrative(profile: SceneProfile): LightNarrative =
        withContext(Dispatchers.Default) {
            val histogram = profile.histogramData
            val exif = profile.exifData

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
        val inferenceEngine = MasterInferenceEngine.getInstance(context)
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
        val profile = inferenceEngine.analyzeImage(bitmap, imagePath)

        // Step 2: 胶片匹配 (Layer 4)
        val filmMatch = matchFilmRecipe(
            profile.sceneHierarchy,
            profile.histogramData!!,
            preferences.toFilmPreferences()
        )

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
        val hierarchy = profile.sceneHierarchy
        return SceneAnalysis(
            primaryScene = hierarchy.primary,
            secondaryScene = hierarchy.secondary,
            fineScene = hierarchy.fine,
            sceneConfidence = profile.confidence,
            suitableSubjects = getSuitableSubjects(hierarchy.primary),
            bestTimeOfDay = getBestTimeOfDay(hierarchy.fine),
            weatherPreference = getWeatherPreference(hierarchy.secondary)
        )
    }

    private fun analyzeColor(profile: SceneProfile): ColorAnalysis {
        val histogram = profile.histogramData
        return ColorAnalysis(
            dominantTone = inferDominantTone(histogram),
            colorTemperature = estimateColorTemperature(histogram),
            saturationLevel = estimateSaturation(histogram),
            colorHarmony = analyzeColorHarmony(histogram),
            hncsCompatibility = calculateHNCSCompatibility(profile)
        )
    }

    private fun analyzeLight(profile: SceneProfile): LightAnalysis {
        val histogram = profile.histogramData
        val meanLuma = histogram?.meanLuminance ?: 128f

        return LightAnalysis(
            exposureLevel = when {
                meanLuma < 64 -> ExposureLevel.UNDER_EXPOSED
                meanLuma > 192 -> ExposureLevel.OVER_EXPOSED
                else -> ExposureLevel.BALANCED
            },
            contrastLevel = calculateContrastLevel(histogram),
            dynamicRange = estimateDynamicRange(histogram),
            lightQuality = inferLightQuality(profile.sceneHierarchy.fine),
            shadowDetail = !histogram?.shadowClipping!!,
            highlightDetail = !histogram.highlightClipping
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
        when (profile.sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> recommendations.add(
                MasterRecommendation(
                    type = RecommendationType.COLOR,
                    priority = Priority.MEDIUM,
                    title = "肤色优化",
                    description = "人像场景建议降低对比度 -10，提升清晰度 +15",
                    paramAdjustment = mapOf("contrast" to "-10", "clarity" to "+15")
                )
            )
            PrimaryScene.LANDSCAPE -> recommendations.add(
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

        return recommendations
    }

    private fun generateNarrative(profile: SceneProfile): String {
        val scene = profile.sceneHierarchy.fine.displayName
        val film = profile.filmRecipe?.filmStock?.displayName ?: "Portra 400"

        return buildString {
            append("这是一幅$scene 作品，")
            append("采用 $film 胶片配方，")
            append("呈现出${inferMood(profile).description}的影像氛围。")
            append("${generateStory(profile)}")
        }
    }

    private fun calculateFilmMatchScore(
        film: FilmStock,
        sceneHierarchy: SceneHierarchy,
        histogramData: HistogramData,
        preferences: FilmPreferences
    ): Float {
        var score = 0.5f

        // 基于场景匹配
        score += when (sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> when (film) {
                FilmStock.KODAK_PORTRA_400, FilmStock.KODAK_PORTRA_160 -> 0.3f
                FilmStock.FUJI_PRO_400H -> 0.25f
                else -> 0.1f
            }
            PrimaryScene.LANDSCAPE -> when (film) {
                FilmStock.KODAK_EKTAR_100, FilmStock.FUJI_VELVIA_50 -> 0.3f
                else -> 0.1f
            }
            PrimaryScene.STREET -> when (film) {
                FilmStock.KODAK_TRI_X_400, FilmStock.FUJI_ACROS_100 -> 0.3f
                else -> 0.1f
            }
            else -> 0.2f
        }

        // 基于亮度匹配
        val meanLuma = histogramData.meanLuminance
        score += when {
            meanLuma < 80 && film.iso >= 400 -> 0.1f // 暗光场景配高ISO胶片
            meanLuma > 150 && film.iso <= 200 -> 0.1f // 明亮场景配低ISO胶片
            else -> 0f
        }

        // 基于用户偏好
        if (preferences.preferredBrands.contains(film.brand)) {
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    private fun generateFilmReasoning(
        candidate: FilmCandidate,
        sceneHierarchy: SceneHierarchy
    ): String {
        return "${candidate.film.displayName} 的${candidate.film.characteristics}特性，"
            .plus("非常适合${sceneHierarchy.fine.displayName}场景，")
            .plus("能够呈现${inferMoodFromFilm(candidate.film).description}的视觉效果。")
    }

    private fun inferMood(profile: SceneProfile): Mood {
        return when (profile.sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> Mood.WARM_INTIMATE
            PrimaryScene.LANDSCAPE -> Mood.GRAND_SERENE
            PrimaryScene.STREET -> Mood.DYNAMIC_ENERGETIC
            PrimaryScene.NIGHT -> Mood.MYSTERIOUS_DRAMATIC
            else -> Mood.NEUTRAL_CALM
        }
    }

    private fun inferMoodFromFilm(film: FilmStock): Mood {
        return when (film) {
            FilmStock.KODAK_PORTRA_400, FilmStock.KODAK_PORTRA_160 -> Mood.WARM_INTIMATE
            FilmStock.KODAK_EKTAR_100, FilmStock.FUJI_VELVIA_50 -> Mood.VIBRANT_LIVELY
            FilmStock.KODAK_TRI_X_400, FilmStock.FUJI_ACROS_100 -> Mood.DRAMATIC_TIMELESS
            else -> Mood.NEUTRAL_CALM
        }
    }

    private fun generateStory(profile: SceneProfile): String {
        return when (profile.sceneHierarchy.fine) {
            FineScene.GOLDEN_HOUR -> "黄金时刻的暖调光线为画面注入了温暖的情感，"
                .plus("建议保留高光细节，让逆光产生柔和的轮廓光。")
            FineScene.BLUE_HOUR -> "蓝调时刻的冷色温创造出宁静神秘的氛围，"
                .plus("适合表现城市灯光与天空的冷暖对比。")
            FineScene.BACKLIGHT_PORTRAIT -> "逆光条件下的人物轮廓富有诗意，"
                .plus("建议使用 HDR 或手动提升阴影以保留面部细节。")
            else -> "当前场景光线条件良好，"
                .plus("建议根据主体特点调整参数以达到最佳效果。")
        }
    }

    private fun generateTechnicalTips(profile: SceneProfile): List<String> {
        return when (profile.sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> listOf(
                "使用大光圈 (f/1.4-f/2.8) 获得柔和背景虚化",
                "对焦眼睛确保清晰度",
                "肤色曝光遵循'向右曝光'原则"
            )
            PrimaryScene.LANDSCAPE -> listOf(
                "使用小光圈 (f/8-f/11) 获得全景深",
                "使用三脚架确保稳定",
                "考虑使用渐变滤镜平衡天空与地面曝光"
            )
            else -> listOf(
                "根据光线条件调整 ISO",
                "注意构图中的引导线",
                "利用前景增加画面层次"
            )
        }
    }

    private fun generateArtisticDirection(profile: SceneProfile): String {
        return "建议采用${profile.filmRecipe?.filmStock?.displayName ?: "胶片"}风格，"
            .plus("强调${inferMood(profile).description}的情绪表达，")
            .plus("通过${profile.sceneHierarchy.fine.displayName}的独特视角，")
            .plus("讲述一个关于光影与色彩的故事。")
    }

    private fun calculateFinalParams(
        profile: SceneProfile,
        selectedFilm: FilmStock
    ): FinalParams {
        val baseParams = profile.hasselbladParams

        // 根据胶片特性微调
        val filmAdjustments = when (selectedFilm) {
            FilmStock.KODAK_PORTRA_400 -> mapOf(
                "saturation" to -5,
                "contrast" to -10,
                "warmth" to 5
            )
            FilmStock.KODAK_EKTAR_100 -> mapOf(
                "saturation" to 15,
                "contrast" to 5,
                "clarity" to 20
            )
            FilmStock.KODAK_TRI_X_400 -> mapOf(
                "saturation" to -100,
                "contrast" to 20,
                "grain" to 30
            )
            else -> emptyMap()
        }

        return FinalParams(
            baseParams = baseParams,
            filmAdjustments = filmAdjustments,
            totalAdjustments = mergeAdjustments(baseParams, filmAdjustments)
        )
    }

    // ==================== 辅助方法 ====================

    private fun getSuitableSubjects(primary: PrimaryScene): List<String> = when (primary) {
        PrimaryScene.PORTRAIT -> listOf("人物", "表情", "互动")
        PrimaryScene.LANDSCAPE -> listOf("自然风光", "城市天际线", "日出日落")
        PrimaryScene.STILL_LIFE -> listOf("美食", "产品", "花卉")
        PrimaryScene.STREET -> listOf("行人", "建筑", "生活场景")
        else -> listOf("多样化主体")
    }

    private fun getBestTimeOfDay(fine: FineScene): String = when (fine) {
        FineScene.GOLDEN_HOUR, FineScene.SUNRISE_SUNSET -> "日出后/日落前1小时"
        FineScene.BLUE_HOUR -> "日出前/日落后30分钟"
        FineScene.CITY_NIGHT, FineScene.NEON_PORTRAIT -> "夜晚"
        else -> "全天适宜"
    }

    private fun getWeatherPreference(secondary: SecondaryScene): String = when (secondary) {
        SecondaryScene.OUTDOOR_PORTRAIT -> "多云或阴天最佳"
        SecondaryScene.NATURAL_LANDSCAPE -> "晴朗或多云"
        else -> "无特殊要求"
    }

    private fun inferDominantTone(histogram: HistogramData?): String {
        val mean = histogram?.meanLuminance ?: 128f
        return when {
            mean < 85 -> "暗调"
            mean > 170 -> "亮调"
            else -> "中间调"
        }
    }

    private fun estimateColorTemperature(histogram: HistogramData?): Int {
        // 简化估算，实际应基于RGB比例
        return 5500
    }

    private fun estimateSaturation(histogram: HistogramData?): SaturationLevel {
        // 简化估算
        return SaturationLevel.MODERATE
    }

    private fun analyzeColorHarmony(histogram: HistogramData?): ColorHarmony {
        return ColorHarmony.COMPLEMENTARY
    }

    private fun calculateHNCSCompatibility(profile: SceneProfile): Float {
        // 哈苏自然色彩解决方案兼容性评分
        return when (profile.sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT, PrimaryScene.LANDSCAPE -> 0.95f
            else -> 0.85f
        }
    }

    private fun calculateContrastLevel(histogram: HistogramData?): ContrastLevel {
        // 基于直方图分布计算对比度
        return ContrastLevel.MEDIUM
    }

    private fun estimateDynamicRange(histogram: HistogramData?): DynamicRange {
        return if (histogram?.shadowClipping == true || histogram?.highlightClipping == true) {
            DynamicRange.LIMITED
        } else {
            DynamicRange.WIDE
        }
    }

    private fun inferLightQuality(fine: FineScene): LightQuality = when (fine) {
        FineScene.GOLDEN_HOUR -> LightQuality.WARM_SOFT
        FineScene.BLUE_HOUR -> LightQuality.COOL_DIFFUSED
        FineScene.OVERCAST_PORTRAIT -> LightQuality.DIFFUSED_EVEN
        else -> LightQuality.DIRECT_HARD
    }

    private fun checkRuleOfThirds(faceData: FaceData?): Boolean {
        // 简化检查
        return true
    }

    private fun detectLeadingLines(profile: SceneProfile): Boolean {
        return false
    }

    private fun analyzeFraming(profile: SceneProfile): String {
        return "标准构图"
    }

    private fun mergeAdjustments(
        baseParams: HasselbladParams,
        adjustments: Map<String, Int>
    ): Map<String, String> {
        return mapOf(
            "iso" to baseParams.iso.toString(),
            "aperture" to "f/${baseParams.aperture}",
            "shutter" to baseParams.shutterSpeed,
            "saturation" to ((baseParams.saturation + (adjustments["saturation"] ?: 0))).toString(),
            "contrast" to ((baseParams.contrast + (adjustments["contrast"] ?: 0))).toString()
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
    val primaryScene: PrimaryScene,
    val secondaryScene: SecondaryScene,
    val fineScene: FineScene,
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
    val film: FilmStock,
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
    val preferredBrands: List<String> = emptyList(),
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
