package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 智能优化参数模型
 *
 * 完整对齐 AlcedoStudio + RapidRAW 全部功能
 * 包括：基础调整、光效、色彩、影调、胶片仿真、特效、几何变换、蒙版、镜头校正等
 */
@Parcelize
data class SmartOptimizeSettings(
    // ========== 基础调整 (Basic) ==========
    var exposure: Float = 0f,          // EV 曝光
    var contrast: Float = 0f,
    var brightness: Float = 0f,
    var highlights: Float = 0f,
    var shadows: Float = 0f,
    var whites: Float = 0f,
    var blacks: Float = 0f,

    // 色调映射器 (RapidRAW: basic / agx)
    var toneMapper: String = "agx",
    var evShift: Float = 0f,

    // ========== 光效 (Light) ==========
    var light: Float = 0f,
    var highlightPreserve: Float = 50f,
    var shadowRecover: Float = 50f,

    // ========== 色彩 (Color) ==========
    var saturation: Float = 0f,
    var vibrance: Float = 0f,
    var temperature: Float = 0f,
    var tint: Float = 0f,
    var hueShift: Float = 0f,
    var hsl: Map<String, Float> = emptyMap(),
    var colorGrading: ColorGradingParams = ColorGradingParams(),

    // ========== 影调 (Tone) ==========
    // 全局点曲线 + 各通道点曲线 (RapidRAW)
    var toneCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    var redCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    var greenCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    var blueCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),

    // 全局参数化曲线 + 各通道参数化曲线 (RapidRAW)
    var parametricCurve: ParametricCurve = ParametricCurve(),
    var redParametricCurve: ParametricCurve = ParametricCurve(),
    var greenParametricCurve: ParametricCurve = ParametricCurve(),
    var blueParametricCurve: ParametricCurve = ParametricCurve(),

    // Hue vs Sat / Hue vs Lum / Lum vs Sat (RapidRAW)
    var hueVsSatCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0.5f), CurvePoint(1f, 0.5f)),
    var hueVsLumCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0.5f), CurvePoint(1f, 0.5f)),
    var lumVsSatCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0.5f), CurvePoint(1f, 0.5f)),

    // 胶片仿真 / 色彩科学 (AlcedoStudio)
    var filmSimulation: String = "none",
    var colorScience: String = "auto",
    var lutPath: String = "",       // LUT 文件路径
    var lutIntensity: Float = 100f,
    var highlightReconstruction: Boolean = false,

    // ========== 特效 (Effects) ==========
    var grainAmount: Float = 0f,
    var grainSize: Float = 25f,
    var grainRoughness: Float = 50f,
    var vignetteAmount: Float = 0f,
    var vignetteMidpoint: Float = 50f,
    var vignetteRoundness: Float = 0f,
    var vignetteFeather: Float = 50f,
    var glowAmount: Float = 0f,
    var halationAmount: Float = 0f,
    var flareAmount: Float = 0f,

    // ========== 细节 (Details) ==========
    var sharpness: Float = 0f,
    var sharpnessThreshold: Float = 15f,
    var clarity: Float = 0f,
    var dehaze: Float = 0f,
    var structure: Float = 0f,
    var centre: Float = 0f,
    var lumaNoiseReduction: Float = 0f,
    var colorNoiseReduction: Float = 0f,
    var chromaticAberrationRedCyan: Float = 0f,
    var chromaticAberrationBlueYellow: Float = 0f,

    // ========== 几何变换 (Geometry) ==========
    var rotation: Float = 0f,
    var orientationSteps: Int = 0,   // 0,1,2,3 = 0,90,180,270
    var flipHorizontal: Boolean = false,
    var flipVertical: Boolean = false,
    var cropData: CropData = CropData(),
    var perspective: PerspectiveParams = PerspectiveParams(),

    // ========== 镜头校正 (Lens Correction) ==========
    var lensProfile: String = "",     // Lensfun 风格镜头描述
    var lensCorrectionStrength: Float = 100f,
    var removeChromaticAberration: Boolean = false,
    var geometryWarp: Float = 0f,     // DNG warp / 几何扭曲

    // ========== 蒙版 / 局部调整 (Masking) ==========
    var masks: List<LocalMask> = emptyList(),

    // ========== 预设强度 (RapidRAW) ==========
    var presetIntensity: Float = 100f,

    // ========== 导出配置 ==========
    var exportFormat: String = "jpeg",  // jpeg / png / tiff / exr
    var exportQuality: Int = 95,
    var exportBitDepth: Int = 8,
    var exportResize: ExportResize = ExportResize(),
    var exportMetadata: Boolean = true,
    var exportColorSpace: String = "sRGB",

    // ========== 复制粘贴支持 ==========
    var clipboardSettings: String = ""
) : Parcelable

@Parcelize
data class CurvePoint(
    var x: Float = 0f,
    var y: Float = 0f
) : Parcelable

@Parcelize
data class ParametricCurve(
    var darks: Float = 0f,
    var shadows: Float = 0f,
    var highlights: Float = 0f,
    var lights: Float = 0f,
    var whiteLevel: Float = 0f,
    var blackLevel: Float = 0f,
    var split1: Float = 25f,
    var split2: Float = 50f,
    var split3: Float = 75f
) : Parcelable

@Parcelize
data class ColorGradingParams(
    var globalHue: Float = 0f,
    var globalSaturation: Float = 0f,
    var globalLuminance: Float = 0f,
    var shadowsHue: Float = 0f,
    var shadowsSaturation: Float = 0f,
    var shadowsLuminance: Float = 0f,
    var midtonesHue: Float = 0f,
    var midtonesSaturation: Float = 0f,
    var midtonesLuminance: Float = 0f,
    var highlightsHue: Float = 0f,
    var highlightsSaturation: Float = 0f,
    var highlightsLuminance: Float = 0f,
    var blending: Float = 100f,
    var balance: Float = 0f
) : Parcelable

@Parcelize
data class CropData(
    var enabled: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 1f,
    var height: Float = 1f,
    var aspectRatio: String = "original"   // original / 1:1 / 4:3 / 16:9 / 3:2 / 5:4 / free
) : Parcelable

@Parcelize
data class PerspectiveParams(
    var vertical: Float = 0f,      // -100 ~ 100
    var horizontal: Float = 0f,    // -100 ~ 100
    var rotation: Float = 0f,      // -15 ~ 15
    var scale: Float = 100f        // 50 ~ 150
) : Parcelable

@Parcelize
data class ExportResize(
    var enabled: Boolean = false,
    var width: Int = 0,
    var height: Int = 0,
    var longEdge: Int = 0,
    var shortEdge: Int = 0,
    var unit: String = "px"       // px / inch / cm
) : Parcelable

/**
 * 局部蒙版定义 (RapidRAW 风格)
 */
@Parcelize
data class LocalMask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "brush",    // brush / linear / radial / subject / sky
    val name: String = "",
    val enabled: Boolean = true,
    val invert: Boolean = false,
    val feather: Float = 50f,
    val density: Float = 100f,
    val maskData: String = "",     // base64 位图数据或路径
    val adjustments: SmartOptimizeSettings = SmartOptimizeSettings()
) : Parcelable

/**
 * 编辑历史节点
 */
@Parcelize
data class EditHistoryNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val settings: SmartOptimizeSettings,
    val timestamp: Long = System.currentTimeMillis(),
    val nodeType: HistoryNodeType = HistoryNodeType.NORMAL,
    val parentId: String? = null,   // Git 风格分支父节点
    val label: String = ""
) : Parcelable

enum class HistoryNodeType {
    NORMAL, BRANCH, COLLAPSED, MERGED
}

/**
 * 优化结果
 */
sealed class OptimizeResult {
    data class Success(
        val bitmap: Bitmap,
        val processingTimeMs: Long
    ) : OptimizeResult()

    data class Error(val message: String) : OptimizeResult()
    object Loading : OptimizeResult()
}

/**
 * 编辑历史管理 (Git 风格分支)
 */
class EditHistoryManager {
    private val history = mutableListOf<EditHistoryNode>()
    private var currentIndex = -1
    private val branches = mutableMapOf<String, MutableList<EditHistoryNode>>()

    val canUndo: Boolean get() = currentIndex > 0
    val canRedo: Boolean get() = currentIndex < history.lastIndex

    fun push(settings: SmartOptimizeSettings, label: String = "") {
        // 如果在中间状态，截断后续历史
        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(EditHistoryNode(
            settings = settings.copy(),
            label = label
        ))
        currentIndex = history.lastIndex
    }

    fun undo(): SmartOptimizeSettings? {
        if (!canUndo) return null
        currentIndex--
        return history[currentIndex].settings
    }

    fun redo(): SmartOptimizeSettings? {
        if (!canRedo) return null
        currentIndex++
        return history[currentIndex].settings
    }

    fun branchFromCurrent(branchName: String): String {
        val parentId = history.getOrNull(currentIndex)?.id
        val branchId = java.util.UUID.randomUUID().toString()
        branches[branchId] = mutableListOf(
            EditHistoryNode(
                settings = history[currentIndex].settings.copy(),
                nodeType = HistoryNodeType.BRANCH,
                parentId = parentId,
                label = branchName
            )
        )
        return branchId
    }

    fun getHistory(): List<EditHistoryNode> = history.toList()
    fun getCurrent(): SmartOptimizeSettings? = history.getOrNull(currentIndex)?.settings
    fun clear() {
        history.clear()
        currentIndex = -1
        branches.clear()
    }
}

/**
 * 预设包装 (支持强度调节)
 */
data class OptimizerPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String,
    val category: PresetCategory = PresetCategory.GENERAL,
    val settings: SmartOptimizeSettings = SmartOptimizeSettings(),
    val intensity: Float = 100f,
    val isBuiltIn: Boolean = true
)

enum class PresetCategory {
    GENERAL, RAPIDRAW, ALCEDO, FILM, PORTRAIT, LANDSCAPE, NIGHT, CREATIVE
}

/**
 * 导出队列项 (AlcedoStudio 风格)
 */
data class ExportQueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourcePath: String,
    val settings: SmartOptimizeSettings,
    val format: String = "jpeg",
    val quality: Int = 95,
    val bitDepth: Int = 8,
    val resize: ExportResize = ExportResize(),
    val colorSpace: String = "sRGB",
    val metadata: Boolean = true,
    val status: ExportStatus = ExportStatus.PENDING
)

enum class ExportStatus {
    PENDING, PROCESSING, DONE, ERROR
}

/**
 * 复制粘贴管理器
 */
object SettingsClipboard {
    private var copiedSettings: SmartOptimizeSettings? = null
    private var includedAdjustments: Set<String> = emptySet()

    fun copy(settings: SmartOptimizeSettings, included: Set<String> = emptySet()) {
        copiedSettings = settings.copy()
        includedAdjustments = included
    }

    fun paste(target: SmartOptimizeSettings): SmartOptimizeSettings {
        val source = copiedSettings ?: return target
        if (includedAdjustments.isEmpty()) {
            return source.copy(
                masks = target.masks,
                cropData = target.cropData,
                lensProfile = target.lensProfile
            )
        }
        // 合并指定字段
        var result = target
        for (field in includedAdjustments) {
            result = when (field) {
                "basic" -> result.copy(
                    exposure = source.exposure, contrast = source.contrast,
                    brightness = source.brightness, highlights = source.highlights,
                    shadows = source.shadows, whites = source.whites, blacks = source.blacks,
                    toneMapper = source.toneMapper, evShift = source.evShift
                )
                "light" -> result.copy(
                    light = source.light, highlightPreserve = source.highlightPreserve,
                    shadowRecover = source.shadowRecover
                )
                "color" -> result.copy(
                    saturation = source.saturation, vibrance = source.vibrance,
                    temperature = source.temperature, tint = source.tint,
                    hueShift = source.hueShift, hsl = source.hsl,
                    colorGrading = source.colorGrading
                )
                "tone" -> result.copy(
                    toneCurve = source.toneCurve, redCurve = source.redCurve,
                    greenCurve = source.greenCurve, blueCurve = source.blueCurve,
                    parametricCurve = source.parametricCurve,
                    redParametricCurve = source.redParametricCurve,
                    greenParametricCurve = source.greenParametricCurve,
                    blueParametricCurve = source.blueParametricCurve,
                    hueVsSatCurve = source.hueVsSatCurve,
                    hueVsLumCurve = source.hueVsLumCurve,
                    lumVsSatCurve = source.lumVsSatCurve
                )
                "film" -> result.copy(
                    filmSimulation = source.filmSimulation, colorScience = source.colorScience,
                    lutPath = source.lutPath, lutIntensity = source.lutIntensity,
                    highlightReconstruction = source.highlightReconstruction
                )
                "effects" -> result.copy(
                    grainAmount = source.grainAmount, grainSize = source.grainSize,
                    grainRoughness = source.grainRoughness, vignetteAmount = source.vignetteAmount,
                    vignetteMidpoint = source.vignetteMidpoint,
                    vignetteRoundness = source.vignetteRoundness,
                    vignetteFeather = source.vignetteFeather,
                    glowAmount = source.glowAmount, halationAmount = source.halationAmount,
                    flareAmount = source.flareAmount
                )
                "details" -> result.copy(
                    sharpness = source.sharpness, sharpnessThreshold = source.sharpnessThreshold,
                    clarity = source.clarity, dehaze = source.dehaze,
                    structure = source.structure, centre = source.centre,
                    lumaNoiseReduction = source.lumaNoiseReduction,
                    colorNoiseReduction = source.colorNoiseReduction,
                    chromaticAberrationRedCyan = source.chromaticAberrationRedCyan,
                    chromaticAberrationBlueYellow = source.chromaticAberrationBlueYellow
                )
                "geometry" -> result.copy(
                    rotation = source.rotation, orientationSteps = source.orientationSteps,
                    flipHorizontal = source.flipHorizontal, flipVertical = source.flipVertical,
                    cropData = source.cropData, perspective = source.perspective
                )
                "lens" -> result.copy(
                    lensProfile = source.lensProfile,
                    lensCorrectionStrength = source.lensCorrectionStrength,
                    removeChromaticAberration = source.removeChromaticAberration,
                    geometryWarp = source.geometryWarp
                )
                "masks" -> result.copy(masks = source.masks)
                else -> result
            }
        }
        return result
    }

    fun hasData(): Boolean = copiedSettings != null
    fun clear() { copiedSettings = null; includedAdjustments = emptySet() }
}
