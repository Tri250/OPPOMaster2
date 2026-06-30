package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ==================== 选项卡定义 ====================

enum class SmartOptimizeTab(val label: String, val icon: String) {
    BASIC("基础", "tune"),
    LIGHT("光效", "light_mode"),
    COLOR("色彩", "palette"),
    CURVE("曲线", "show_chart"),
    GRADING("分级", "gradient"),
    DETAIL("细节", "grain"),
    EFFECTS("特效", "auto_awesome"),
    OPTICS("光学", "camera"),
    CALIBRATION("校准", "tune_int"),
    LUT("LUT", "filter"),
    MASK("蒙版", "brush"),
    PRESETS("预设", "auto_fix_high"),
    HISTORY("历史", "history"),
    EXPORT("导出", "save")
}

// ==================== 核心参数模型 ====================

/**
 * 智能优化参数
 *
 * 完整对齐 AlcedoStudio + RapidRAW 全部功能
 * 包括：基础调整、光效、色彩、影调曲线、色彩分级、细节、特效、光学、校准、
 *       LUT、蒙版、胶片仿真、几何变换、镜头校正、导出等
 */
@Parcelize
data class SmartOptimizeParams(
    // ========== 基础调整 (Basic) ==========
    var exposure: Float = 0f,
    var brightness: Float = 0f,
    var contrast: Float = 0f,
    var highlights: Float = 0f,
    var shadows: Float = 0f,
    var whites: Float = 0f,
    var blacks: Float = 0f,
    var saturation: Float = 0f,
    var vibrance: Float = 0f,

    // 色调映射器 (RapidRAW: basic / agx)
    var toneMapper: String = "agx",
    var evShift: Float = 0f,
    var toneMappingStrength: Float = 50f,
    var sigmoidContrast: Float = 50f,
    var highlightTransition: Float = 50f,

    // ========== 光效 (Light) ==========
    var light: Float = 0f,
    var highlightPreserve: Float = 50f,
    var shadowRecover: Float = 50f,
    var dehaze: Float = 0f,

    // ========== 色彩 (Color) ==========
    var temperature: Float = 6500f,
    var tint: Float = 0f,
    var hueShift: Float = 0f,
    var hslAdjustments: HSLAdjustments = HSLAdjustments(),
    var colorScience: String = "auto",       // auto / aces2 / opendrt / srgb
    var displayColorSpace: String = "sRGB",   // sRGB / Rec2020 / DCIP3
    var eotf: String = "sRGB",               // sRGB / PQ / HLG / Linear
    var peakLuminance: Float = 100f,         // nits

    // ========== 影调曲线 (Curve) ==========
    var parametricCurve: ParametricCurveData = ParametricCurveData(),
    var pointCurve: List<CurvePoint> = defaultCurve(),
    var redCurve: List<CurvePoint> = defaultCurve(),
    var greenCurve: List<CurvePoint> = defaultCurve(),
    var blueCurve: List<CurvePoint> = defaultCurve(),

    // Hue vs Sat / Hue vs Lum / Lum vs Sat (RapidRAW)
    var hueVsSatCurve: List<CurvePoint> = flatCurve(),
    var hueVsLumCurve: List<CurvePoint> = flatCurve(),
    var lumVsSatCurve: List<CurvePoint> = flatCurve(),

    // ========== 色彩分级 (Grading) - CDL Lift/Gain + 3-Way ==========
    var shadowWheel: ColorWheelValue = ColorWheelValue(),
    var midtoneWheel: ColorWheelValue = ColorWheelValue(),
    var highlightWheel: ColorWheelValue = ColorWheelValue(),
    var globalWheel: ColorWheelValue = ColorWheelValue(),
    var gradingBlend: Float = 100f,
    var gradingBalance: Float = 50f,

    // ========== 细节 (Detail) ==========
    var sharpness: Float = 0f,
    var sharpnessRadius: Float = 1f,
    var sharpnessDetail: Float = 25f,
    var sharpnessMasking: Float = 0f,
    var sharpnessThreshold: Float = 15f,
    var clarity: Float = 0f,
    var structure: Float = 0f,           // RapidRAW
    var centre: Float = 0f,              // RapidRAW
    var texture: Float = 0f,
    var luminanceNoiseReduction: Float = 0f,
    var noiseReductionDetail: Float = 50f,
    var colorNoiseReduction: Float = 0f,
    var colorNoiseReductionDetail: Float = 50f,
    var lumaNoiseReduction: Float = 0f,  // RapidRAW 别名

    // ========== 特效 (Effects) ==========
    var grain: Float = 0f,
    var grainSize: Float = 25f,
    var grainRoughness: Float = 50f,
    var vignette: Float = 0f,
    var vignetteMidpoint: Float = 50f,
    var vignetteRoundness: Float = 0f,
    var vignetteFeather: Float = 50f,
    var fade: Float = 0f,
    // RapidRAW 创意特效
    var glowAmount: Float = 0f,
    var halationAmount: Float = 0f,
    var flareAmount: Float = 0f,

    // ========== 光学 (Optics) ==========
    var distortion: Float = 0f,
    var chromaticAberrationR: Float = 0f,
    var chromaticAberrationB: Float = 0f,
    var lensProfile: String = "",
    var lensCorrectionStrength: Float = 100f,
    var removeChromaticAberration: Boolean = false,
    var geometryWarp: Float = 0f,

    // ========== 透视与裁剪 (Geometry) ==========
    var perspectiveX: Float = 0f,
    var perspectiveY: Float = 0f,
    var rotation: Float = 0f,
    var orientationSteps: Int = 0,
    var flipHorizontal: Boolean = false,
    var flipVertical: Boolean = false,
    var cropLockAspect: Boolean = false,
    var cropTop: Float = 0f,
    var cropLeft: Float = 0f,
    var cropBottom: Float = 0f,
    var cropRight: Float = 0f,

    // ========== 校准 (Calibration) ==========
    var shadowTint: Float = 0f,
    var redPrimaryHue: Float = 0f,
    var redPrimarySaturation: Float = 100f,
    var greenPrimaryHue: Float = 0f,
    var greenPrimarySaturation: Float = 100f,
    var bluePrimaryHue: Float = 0f,
    var bluePrimarySaturation: Float = 100f,

    // ========== LUT ==========
    var activeLutName: String = "",
    var lutPath: String = "",
    var lutIntensity: Float = 100f,
    var highlightReconstruction: Boolean = false,

    // ========== 胶片仿真 (AlcedoStudio) ==========
    var filmSimulation: String = "none",

    // ========== 蒙版 / 局部调整 (RapidRAW) ==========
    var masks: List<LocalMask> = emptyList(),

    // ========== 预设强度 (RapidRAW) ==========
    var presetIntensity: Float = 100f,

    // ========== 显示辅助 ==========
    var showHistogram: Boolean = true,
    var histogramMode: String = "luma",    // luma / rgb / parade / vectorscope

    // ========== 导出配置 ==========
    var exportFormat: String = "jpeg",
    var exportQuality: Int = 95,
    var exportBitDepth: Int = 8,
    var exportResize: ExportResize = ExportResize(),
    var exportMetadata: Boolean = true,
    var exportColorSpace: String = "sRGB"
) : Parcelable {

    companion object {
        val DEFAULT = SmartOptimizeParams()
    }

    /** 计算已修改的参数数量 */
    fun changedParamCount(): Int {
        val d = DEFAULT
        var count = 0
        if (exposure != d.exposure) count++
        if (brightness != d.brightness) count++
        if (contrast != d.contrast) count++
        if (highlights != d.highlights) count++
        if (shadows != d.shadows) count++
        if (whites != d.whites) count++
        if (blacks != d.blacks) count++
        if (saturation != d.saturation) count++
        if (vibrance != d.vibrance) count++
        if (temperature != d.temperature) count++
        if (tint != d.tint) count++
        if (dehaze != d.dehaze) count++
        if (sharpness != d.sharpness) count++
        if (clarity != d.clarity) count++
        if (texture != d.texture) count++
        if (structure != d.structure) count++
        if (grain != d.grain) count++
        if (vignette != d.vignette) count++
        if (fade != d.fade) count++
        if (distortion != d.distortion) count++
        if (chromaticAberrationR != d.chromaticAberrationR) count++
        if (chromaticAberrationB != d.chromaticAberrationB) count++
        if (glowAmount != d.glowAmount) count++
        if (halationAmount != d.halationAmount) count++
        if (flareAmount != d.flareAmount) count++
        if (luminanceNoiseReduction != d.luminanceNoiseReduction) count++
        if (colorNoiseReduction != d.colorNoiseReduction) count++
        if (activeLutName != d.activeLutName) count++
        if (filmSimulation != d.filmSimulation) count++
        if (toneMapper != d.toneMapper) count++
        return count
    }

    /** 重置所有参数到默认值 */
    fun resetAll(): SmartOptimizeParams = DEFAULT.copy()
}

// ==================== 辅助数据类 ====================

@Parcelize
data class CurvePoint(
    val x: Float = 0f,
    val y: Float = 0f
) : Parcelable

fun defaultCurve() = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
fun flatCurve() = listOf(CurvePoint(0f, 0.5f), CurvePoint(1f, 0.5f))

@Parcelize
data class ParametricCurveData(
    val darks: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val lights: Float = 0f,
    val whiteLevel: Float = 0f,
    val blackLevel: Float = 0f,
    val split1: Float = 25f,
    val split2: Float = 50f,
    val split3: Float = 75f
) : Parcelable

@Parcelize
data class HSLAdjustments(
    val redHue: Float = 0f,
    val redSaturation: Float = 0f,
    val redLuminance: Float = 0f,
    val orangeHue: Float = 0f,
    val orangeSaturation: Float = 0f,
    val orangeLuminance: Float = 0f,
    val yellowHue: Float = 0f,
    val yellowSaturation: Float = 0f,
    val yellowLuminance: Float = 0f,
    val greenHue: Float = 0f,
    val greenSaturation: Float = 0f,
    val greenLuminance: Float = 0f,
    val aquaHue: Float = 0f,
    val aquaSaturation: Float = 0f,
    val aquaLuminance: Float = 0f,
    val blueHue: Float = 0f,
    val blueSaturation: Float = 0f,
    val blueLuminance: Float = 0f,
    val purpleHue: Float = 0f,
    val purpleSaturation: Float = 0f,
    val purpleLuminance: Float = 0f,
    val magentaHue: Float = 0f,
    val magentaSaturation: Float = 0f,
    val magentaLuminance: Float = 0f
) : Parcelable

@Parcelize
data class ColorWheelValue(
    val hue: Float = 0f,       // 0-360
    val saturation: Float = 0f, // 0-100
    val luminance: Float = 0f   // -100 ~ 100
) : Parcelable

@Parcelize
data class ExportResize(
    val enabled: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val longEdge: Int = 0,
    val shortEdge: Int = 0,
    val unit: String = "px"       // px / inch / cm
) : Parcelable

@Parcelize
data class LocalMask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String = "brush",    // brush / linear / radial / subject / sky
    val name: String = "",
    val enabled: Boolean = true,
    val invert: Boolean = false,
    val feather: Float = 50f,
    val density: Float = 100f,
    val maskData: String = "",
    val adjustments: SmartOptimizeParams = SmartOptimizeParams()
) : Parcelable

// ==================== 编辑历史 ====================

@Parcelize
data class EditHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val params: SmartOptimizeParams,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String = "",
    val nodeType: HistoryNodeType = HistoryNodeType.NORMAL,
    val parentId: String? = null
) : Parcelable

enum class HistoryNodeType {
    NORMAL, BRANCH, COLLAPSED, MERGED
}

/**
 * 编辑历史管理器 - Git 风格分支 (AlcedoStudio)
 */
class EditHistoryManager {
    private val history = mutableListOf<EditHistoryEntry>()
    private var currentIndex = -1
    private val branches = mutableMapOf<String, MutableList<EditHistoryEntry>>()

    val canUndo: Boolean get() = currentIndex > 0
    val canRedo: Boolean get() = currentIndex < history.lastIndex

    fun push(params: SmartOptimizeParams, label: String = "") {
        if (currentIndex < history.lastIndex) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(EditHistoryEntry(params = params.copy(), label = label))
        currentIndex = history.lastIndex
    }

    fun undo(): SmartOptimizeParams? {
        if (!canUndo) return null
        currentIndex--
        return history[currentIndex].params
    }

    fun redo(): SmartOptimizeParams? {
        if (!canRedo) return null
        currentIndex++
        return history[currentIndex].params
    }

    fun branchFromCurrent(branchName: String): String {
        val parentId = history.getOrNull(currentIndex)?.id
        val branchId = java.util.UUID.randomUUID().toString()
        branches[branchId] = mutableListOf(
            EditHistoryEntry(
                params = history[currentIndex].params.copy(),
                nodeType = HistoryNodeType.BRANCH,
                parentId = parentId,
                label = branchName
            )
        )
        return branchId
    }

    fun getHistory(): List<EditHistoryEntry> = history.toList()
    fun getCurrent(): SmartOptimizeParams? = history.getOrNull(currentIndex)?.params
    fun clear() {
        history.clear()
        currentIndex = -1
        branches.clear()
    }
}

// ==================== 预设 ====================

data class SmartOptimizePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String,
    val category: PresetCategory = PresetCategory.GENERAL,
    val params: SmartOptimizeParams = SmartOptimizeParams(),
    val intensity: Float = 100f,
    val isBuiltIn: Boolean = true
)

enum class PresetCategory(val label: String) {
    GENERAL("通用"), RAPIDRAW("RapidRAW"), ALCEDO("Alcedo"), FILM("胶片"),
    MONOCHROME("黑白"), CINEMATIC("电影"), PORTRAIT("人像"), LANDSCAPE("风景"),
    NIGHT("夜景"), MOOD("情绪"), VINTAGE("复古"), HDR("HDR"), CREATIVE("创意")
}

// ==================== 胶片仿真描述 ====================

data class FilmSimulation(
    val id: String,
    val name: String,
    val brand: String,
    val series: String,
    val description: String,
    val colorStyle: String,
    val grainLevel: String,
    val contrastLevel: String,
    val bestFor: String,
    val params: SmartOptimizeParams = SmartOptimizeParams()
)

// ==================== 导出 ====================

@Parcelize
data class ExportConfig(
    val format: String = "jpeg",
    val quality: Int = 95,
    val bitDepth: Int = 8,
    val resize: ExportResize = ExportResize(),
    val colorSpace: String = "sRGB",
    val metadata: Boolean = true
) : Parcelable

data class ExportQueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourcePath: String,
    val params: SmartOptimizeParams,
    val config: ExportConfig = ExportConfig(),
    val status: ExportStatus = ExportStatus.PENDING
)

enum class ExportStatus {
    PENDING, PROCESSING, DONE, ERROR
}

// ==================== 直方图 ====================

data class HistogramFullResult(
    val luma: IntArray = IntArray(256),
    val red: IntArray = IntArray(256),
    val green: IntArray = IntArray(256),
    val blue: IntArray = IntArray(256),
    val waveform: WaveformData? = null,
    val meanLuminance: Float = 0f,
    val dynamicRange: Float = 0f,
    val shadowClipping: Boolean = false,
    val highlightClipping: Boolean = false
)

data class WaveformData(
    val data: FloatArray = FloatArray(0),
    val width: Int = 0,
    val height: Int = 0,
    val scanlines: List<FloatArray> = emptyList(),
    val maxValue: Float = 0f
)

// ==================== 复制粘贴管理器 ====================

object SettingsClipboard {
    private var copiedParams: SmartOptimizeParams? = null
    private var includedAdjustments: Set<String> = emptySet()

    fun copy(params: SmartOptimizeParams, included: Set<String> = emptySet()) {
        copiedParams = params.copy()
        includedAdjustments = included
    }

    fun paste(target: SmartOptimizeParams): SmartOptimizeParams {
        val source = copiedParams ?: return target
        if (includedAdjustments.isEmpty()) {
            return source.copy(
                masks = target.masks,
                cropTop = target.cropTop,
                cropLeft = target.cropLeft,
                lensProfile = target.lensProfile
            )
        }
        var result = target
        for (field in includedAdjustments) {
            result = when (field) {
                "basic" -> result.copy(
                    exposure = source.exposure, contrast = source.contrast,
                    brightness = source.brightness, highlights = source.highlights,
                    shadows = source.shadows, whites = source.whites, blacks = source.blacks,
                    saturation = source.saturation, vibrance = source.vibrance,
                    toneMapper = source.toneMapper, evShift = source.evShift,
                    toneMappingStrength = source.toneMappingStrength,
                    sigmoidContrast = source.sigmoidContrast,
                    highlightTransition = source.highlightTransition
                )
                "light" -> result.copy(
                    light = source.light, highlightPreserve = source.highlightPreserve,
                    shadowRecover = source.shadowRecover, dehaze = source.dehaze
                )
                "color" -> result.copy(
                    temperature = source.temperature, tint = source.tint,
                    hueShift = source.hueShift, hslAdjustments = source.hslAdjustments,
                    colorScience = source.colorScience,
                    displayColorSpace = source.displayColorSpace,
                    eotf = source.eotf, peakLuminance = source.peakLuminance
                )
                "tone" -> result.copy(
                    parametricCurve = source.parametricCurve,
                    pointCurve = source.pointCurve,
                    redCurve = source.redCurve, greenCurve = source.greenCurve,
                    blueCurve = source.blueCurve,
                    hueVsSatCurve = source.hueVsSatCurve,
                    hueVsLumCurve = source.hueVsLumCurve,
                    lumVsSatCurve = source.lumVsSatCurve
                )
                "grading" -> result.copy(
                    shadowWheel = source.shadowWheel,
                    midtoneWheel = source.midtoneWheel,
                    highlightWheel = source.highlightWheel,
                    globalWheel = source.globalWheel,
                    gradingBlend = source.gradingBlend,
                    gradingBalance = source.gradingBalance
                )
                "detail" -> result.copy(
                    sharpness = source.sharpness, sharpnessRadius = source.sharpnessRadius,
                    sharpnessDetail = source.sharpnessDetail,
                    sharpnessMasking = source.sharpnessMasking,
                    clarity = source.clarity, structure = source.structure,
                    centre = source.centre, texture = source.texture,
                    luminanceNoiseReduction = source.luminanceNoiseReduction,
                    colorNoiseReduction = source.colorNoiseReduction
                )
                "effects" -> result.copy(
                    grain = source.grain, grainSize = source.grainSize,
                    grainRoughness = source.grainRoughness,
                    vignette = source.vignette, vignetteMidpoint = source.vignetteMidpoint,
                    vignetteFeather = source.vignetteFeather, fade = source.fade,
                    glowAmount = source.glowAmount, halationAmount = source.halationAmount,
                    flareAmount = source.flareAmount
                )
                "optics" -> result.copy(
                    distortion = source.distortion,
                    chromaticAberrationR = source.chromaticAberrationR,
                    chromaticAberrationB = source.chromaticAberrationB
                )
                "calibration" -> result.copy(
                    shadowTint = source.shadowTint,
                    redPrimaryHue = source.redPrimaryHue,
                    redPrimarySaturation = source.redPrimarySaturation,
                    greenPrimaryHue = source.greenPrimaryHue,
                    greenPrimarySaturation = source.greenPrimarySaturation,
                    bluePrimaryHue = source.bluePrimaryHue,
                    bluePrimarySaturation = source.bluePrimarySaturation
                )
                "lut" -> result.copy(
                    activeLutName = source.activeLutName,
                    lutPath = source.lutPath, lutIntensity = source.lutIntensity
                )
                "film" -> result.copy(filmSimulation = source.filmSimulation)
                "masks" -> result.copy(masks = source.masks)
                "geometry" -> result.copy(
                    perspectiveX = source.perspectiveX,
                    perspectiveY = source.perspectiveY,
                    rotation = source.rotation
                )
                else -> result
            }
        }
        return result
    }

    fun hasData(): Boolean = copiedParams != null
    fun clear() { copiedParams = null; includedAdjustments = emptySet() }
}

// ==================== 优化结果 ====================

sealed class OptimizeResult {
    data class Success(val bitmap: Bitmap, val processingTimeMs: Long) : OptimizeResult()
    data class Error(val message: String) : OptimizeResult()
    object Loading : OptimizeResult()
}

// ==================== 色彩科学枚举 ====================

enum class ColorScienceMode(val label: String) {
    auto("自动"), aces2("ACES 2"), opendrt("OpenDRT"), srgb("sRGB")
}

enum class DisplayColorSpace(val label: String) {
    sRGB("sRGB"), Rec2020("Rec. 2020"), DCIP3("DCI-P3")
}

enum class EOTF(val label: String) {
    sRGB("sRGB"), PQ("PQ"), HLG("HLG"), Linear("Linear")
}

enum class ExportFormat(val label: String, val value: String) {
    JPEG("JPEG", "jpeg"), PNG("PNG", "png"), TIFF("TIFF", "tiff")
}
