package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

// ==================== AlcedoStudio + RapidRAW 完整参数模型 ====================

/**
 * 智能优化主参数 — 完整对齐 AlcedoStudio + RapidRAW 所有功能
 *
 * 参数分组：
 * - 基础：曝光/亮度/对比度/饱和度/鲜艳度
 * - 光影：高光/阴影/白色/黑色/去霾
 * - 色彩：色温/色调/HSL八通道
 * - 色调曲线：RGB/L 参数曲线
 * - 色彩分级：阴影/中间调/高光色轮
 * - 细节：锐化/降噪/纹理/清晰度
 * - 效果：颗粒/暗角/褪色
 * - 光学：畸变校正/色差/透视
 * - 色彩科学：ACES/DRT/色调映射
 * - 校准：阴影色调/红绿蓝原色
 */
@Serializable
data class SmartOptimizeParams(
    // ========== 基础调整 (Basic) ==========
    /** 曝光 -5.0~5.0 EV, 默认 0.0 */
    val exposure: Float = 0f,
    /** 亮度 -100~100, 默认 0 */
    val brightness: Float = 0f,
    /** 对比度 -100~100, 默认 0 */
    val contrast: Float = 0f,
    /** 饱和度 -100~100, 默认 0 */
    val saturation: Float = 0f,
    /** 鲜艳度 -100~100, 默认 0 */
    val vibrance: Float = 0f,

    // ========== 光影调整 (Light) ==========
    /** 高光 -100~100, 默认 0 */
    val highlights: Float = 0f,
    /** 阴影 -100~100, 默认 0 */
    val shadows: Float = 0f,
    /** 白色色阶 -100~100, 默认 0 */
    val whites: Float = 0f,
    /** 黑色色阶 -100~100, 默认 0 */
    val blacks: Float = 0f,
    /** 去霾 0~100, 默认 0 */
    val dehaze: Float = 0f,

    // ========== 色彩调整 (Color) ==========
    /** 色温 2000~50000, 默认 5500 */
    val temperature: Float = 5500f,
    /** 色调 -100~100, 默认 0 */
    val tint: Float = 0f,

    // ========== HSL 八通道 (8 channels) ==========
    /** HSL 八个通道调整 */
    val hslAdjustments: HSLAdjustments = HSLAdjustments(),

    // ========== 色调曲线 (Tone Curve) ==========
    /** 参数曲线 (高光/亮调/暗调/阴影) */
    val parametricCurve: ParametricCurve = ParametricCurve(),
    /** 点曲线控制点列表 */
    val pointCurve: List<CurvePoint> = listOf(
        CurvePoint(0f, 0f),
        CurvePoint(0.25f, 0.25f),
        CurvePoint(0.5f, 0.5f),
        CurvePoint(0.75f, 0.75f),
        CurvePoint(1f, 1f)
    ),
    /** RGB 三通道独立曲线 */
    val redCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val greenCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val blueCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),

    // ========== 色彩分级 (Color Grading - CDL + Wheels) ==========
    /** 阴影色轮 (H: 0-360, S: 0-100, L: -100~100) */
    val shadowWheel: ColorWheel = ColorWheel(),
    /** 中间调色轮 */
    val midtoneWheel: ColorWheel = ColorWheel(),
    /** 高光色轮 */
    val highlightWheel: ColorWheel = ColorWheel(),
    /** 全局色轮 */
    val globalWheel: ColorWheel = ColorWheel(),
    /** 混合 0~100 */
    val gradingBlend: Float = 0f,
    /** 亮度平衡 0~100 */
    val gradingBalance: Float = 0f,

    // ========== 细节处理 (Detail) ==========
    /** 锐化 0~150, 默认 0 */
    val sharpness: Float = 0f,
    /** 锐化半径 0.5~3.0, 默认 1.0 */
    val sharpnessRadius: Float = 1f,
    /** 锐化细节 0~100, 默认 25 */
    val sharpnessDetail: Float = 25f,
    /** 锐化蒙版 0~100, 默认 0 */
    val sharpnessMasking: Float = 0f,
    /** 降噪(亮度) 0~100, 默认 0 */
    val luminanceNoiseReduction: Float = 0f,
    /** 降噪(细节) 0~100, 默认 50 */
    val noiseReductionDetail: Float = 50f,
    /** 降噪(色彩) 0~100, 默认 25 */
    val colorNoiseReduction: Float = 25f,
    /** 降噪(色彩细节) 0~100, 默认 50 */
    val colorNoiseReductionDetail: Float = 50f,
    /** 纹理 -100~100, 默认 0 */
    val texture: Float = 0f,
    /** 清晰度 -100~100, 默认 0 */
    val clarity: Float = 0f,

    // ========== 效果 (Effects) ==========
    /** 颗粒 0~100, 默认 0 */
    val grain: Float = 0f,
    /** 颗粒大小 0~100, 默认 25 */
    val grainSize: Float = 25f,
    /** 颗粒粗糙度 0~100, 默认 50 */
    val grainRoughness: Float = 50f,
    /** 暗角 -100~100, 默认 0 */
    val vignette: Float = 0f,
    /** 暗角中点 0~100, 默认 50 */
    val vignetteMidpoint: Float = 50f,
    /** 暗角羽化 0~100, 默认 50 */
    val vignetteFeather: Float = 50f,
    /** 褪色 0~100, 默认 0 */
    val fade: Float = 0f,

    // ========== 光学校正 (Optics) ==========
    /** 畸变校正 -100~100, 默认 0 */
    val distortion: Float = 0f,
    /** 色差校正(红/青) -100~100, 默认 0 */
    val chromaticAberrationR: Float = 0f,
    /** 色差校正(蓝/黄) -100~100, 默认 0 */
    val chromaticAberrationB: Float = 0f,
    /** 透视校正(X) -100~100, 默认 0 */
    val perspectiveX: Float = 0f,
    /** 透视校正(Y) -100~100, 默认 0 */
    val perspectiveY: Float = 0f,
    /** 旋转角度 -45~45, 默认 0 */
    val rotation: Float = 0f,

    // ========== 裁剪/变换 (Transform) ==========
    /** 裁剪上边界 0~1 */
    val cropTop: Float = 0f,
    /** 裁剪下边界 0~1 */
    val cropBottom: Float = 1f,
    /** 裁剪左边界 0~1 */
    val cropLeft: Float = 0f,
    /** 裁剪右边界 0~1 */
    val cropRight: Float = 1f,
    /** 裁剪锁定比例 */
    val cropLockAspect: Boolean = true,
    /** 裁剪比例 */
    val cropAspectRatio: Float = 1f, // 1:1, 4:3, 16:9, etc.

    // ========== 色彩科学 (Color Science, 对齐 AlcedoStudio) ==========
    /** 色彩科学模式: "ACES_2_0" / "OPEN_DRT" / "STANDARD" */
    val colorScience: String = "STANDARD",
    /** 显示色彩空间: "SRGB" / "DISPLAY_P3" / "REC2020" */
    val displayColorSpace: String = "SRGB",
    /** EOTF: "SRGB" / "GAMMA_2_2" / "GAMMA_2_4" / "PQ" */
    val eotf: String = "SRGB",
    /** 峰值亮度 nits (HDR) */
    val peakLuminance: Float = 100f,
    /** 色调映射强度 0~100 */
    val toneMappingStrength: Float = 0f,
    /** Sigmoid对比度 0~100 */
    val sigmoidContrast: Float = 0f,
    /** 高光过渡 0~100 (对齐 AlcedoStudio highlight transition) */
    val highlightTransition: Float = 0f,

    // ========== 相机校准 (Calibration) ==========
    /** 阴影色调 -100~100 */
    val shadowTint: Float = 0f,
    /** 红色原色(色相) -100~100 */
    val redPrimaryHue: Float = 0f,
    /** 红色原色(饱和度) -100~100 */
    val redPrimarySaturation: Float = 0f,
    /** 绿色原色(色相) -100~100 */
    val greenPrimaryHue: Float = 0f,
    /** 绿色原色(饱和度) -100~100 */
    val greenPrimarySaturation: Float = 0f,
    /** 蓝色原色(色相) -100~100 */
    val bluePrimaryHue: Float = 0f,
    /** 蓝色原色(饱和度) -100~100 */
    val bluePrimarySaturation: Float = 0f,

    // ========== 面部美化 (Face) ==========
    /** 面部美白 0~100 */
    val faceBrightening: Float = 0f,
    /** 面部平滑 0~100 */
    val faceSmoothness: Float = 50f,

    // ========== LUT 应用 ==========
    /** 3D LUT 强度 0~100, 默认 0 */
    val lutIntensity: Float = 0f,
    /** 当前使用的 LUT 名称 */
    val activeLutName: String = "",

    // ========== 黑白转换 (AlcedoStudio B&W) ==========
    /** 黑白混合比 0~100, 默认 0 */
    val blackAndWhite: Float = 0f,
    /** 黑白滤镜色相 0~360, 模拟彩色滤镜 */
    val blackAndWhiteFilterHue: Float = 0f,
    /** 黑白滤镜强度 0~100 */
    val blackAndWhiteFilterStrength: Float = 0f,

    // ========== 光晕效果 (AlcedoStudio Halation) ==========
    /** 光晕强度 0~100, 默认 0 */
    val halation: Float = 0f,
    /** 光晕色温偏移 -50~50 */
    val halationColor: Float = 0f,

    // ========== 负片转换 (RapidRAW Negative) ==========
    /** 负片转换开关 */
    val negativeConversion: Boolean = false,
    /** 负片去色罩强度 0~100 */
    val negativeOrangeMask: Float = 50f,

    // ========== AI 自动增强 (RapidRAW AI) ==========
    /** AI 一键自动增强 */
    val aiAutoEnhance: Boolean = false,
    /** AI 增强强度 0~100 */
    val aiEnhanceStrength: Float = 80f,

    // ========== 遮罩 (RapidRAW Masks) ==========
    /** 遮罩启用 */
    val maskEnabled: Boolean = false,
    /** 遮罩类型: NONE / BRUSH / LUMINANCE / AI_SKY / AI_SUBJECT */
    val maskType: String = "NONE",
    /** 遮罩强度/羽化 0~100 */
    val maskIntensity: Float = 50f,
    /** 亮度遮罩阈值低 0~1 */
    val maskLuminanceLow: Float = 0f,
    /** 亮度遮罩阈值高 0~1 */
    val maskLuminanceHigh: Float = 1f,

    // ========== 直方图/波形/矢量 ==========
    /** 是否显示直方图 */
    val showHistogram: Boolean = true,
    /** 是否显示波形图 */
    val showWaveform: Boolean = false,
    /** 是否显示矢量图 */
    val showVectorscope: Boolean = false,
    /** 直方图模式: "RGB" / "LUMINANCE" / "COLOR" */
    val histogramMode: String = "RGB",

    // ========== 编辑历史 ==========
    /** 编辑历史栈 */
    val editHistory: List<EditHistoryEntry> = emptyList()
) {
    companion object {
        val DEFAULT = SmartOptimizeParams()
    }

    fun changedParamCount(): Int {
        var count = 0
        if (exposure != 0f) count++
        if (brightness != 0f) count++
        if (contrast != 0f) count++
        if (saturation != 0f) count++
        if (vibrance != 0f) count++
        if (highlights != 0f) count++
        if (shadows != 0f) count++
        if (whites != 0f) count++
        if (blacks != 0f) count++
        if (dehaze != 0f) count++
        if (temperature != 5500f) count++
        if (tint != 0f) count++
        if (hslAdjustments.hasChanges()) count++
        if (parametricCurve.hasChanges()) count++
        if (pointCurve != DEFAULT.pointCurve) count++
        if (shadowWheel.hasChanges()) count++
        if (midtoneWheel.hasChanges()) count++
        if (highlightWheel.hasChanges()) count++
        if (globalWheel.hasChanges()) count++
        if (sharpness != 0f) count++
        if (luminanceNoiseReduction != 0f) count++
        if (colorNoiseReduction != 25f) count++
        if (texture != 0f) count++
        if (clarity != 0f) count++
        if (grain != 0f) count++
        if (vignette != 0f) count++
        if (fade != 0f) count++
        if (distortion != 0f) count++
        if (chromaticAberrationR != 0f) count++
        if (chromaticAberrationB != 0f) count++
        if (faceBrightening != 0f) count++
        if (faceSmoothness != 50f) count++
        if (lutIntensity != 0f) count++
        if (shadowTint != 0f) count++
        if (redPrimaryHue != 0f || redPrimarySaturation != 0f) count++
        if (greenPrimaryHue != 0f || greenPrimarySaturation != 0f) count++
        if (bluePrimaryHue != 0f || bluePrimarySaturation != 0f) count++
        if (blackAndWhite != 0f) count++
        if (blackAndWhiteFilterStrength != 0f) count++
        if (halation != 0f) count++
        if (negativeConversion) count++
        if (aiAutoEnhance) count++
        if (maskEnabled) count++
        return count
    }

    fun isDefault(): Boolean = this == DEFAULT
}

// ==================== HSL 调整模型 ====================

@Serializable
data class HSLAdjustments(
    // 红色通道
    val redHue: Float = 0f,        // -100~100
    val redSaturation: Float = 0f,  // -100~100
    val redLuminance: Float = 0f,   // -100~100
    // 橙色通道
    val orangeHue: Float = 0f,
    val orangeSaturation: Float = 0f,
    val orangeLuminance: Float = 0f,
    // 黄色通道
    val yellowHue: Float = 0f,
    val yellowSaturation: Float = 0f,
    val yellowLuminance: Float = 0f,
    // 绿色通道
    val greenHue: Float = 0f,
    val greenSaturation: Float = 0f,
    val greenLuminance: Float = 0f,
    // 青色通道
    val cyanHue: Float = 0f,
    val cyanSaturation: Float = 0f,
    val cyanLuminance: Float = 0f,
    // 蓝色通道
    val blueHue: Float = 0f,
    val blueSaturation: Float = 0f,
    val blueLuminance: Float = 0f,
    // 紫色通道
    val purpleHue: Float = 0f,
    val purpleSaturation: Float = 0f,
    val purpleLuminance: Float = 0f,
    // 品红通道
    val magentaHue: Float = 0f,
    val magentaSaturation: Float = 0f,
    val magentaLuminance: Float = 0f
) {
    fun hasChanges(): Boolean = this != HSLAdjustments()
}

// ==================== 色调曲线模型 ====================

@Serializable
data class CurvePoint(
    val x: Float,  // 0~1 输入
    val y: Float   // 0~1 输出
)

@Serializable
data class ParametricCurve(
    val highlights: Float = 0f,   // -100~100
    val lights: Float = 0f,       // -100~100
    val darks: Float = 0f,        // -100~100
    val shadows: Float = 0f,      // -100~100
    val highlightSplit: Float = 75f,  // 0~100
    val shadowSplit: Float = 25f      // 0~100
) {
    fun hasChanges(): Boolean =
        highlights != 0f || lights != 0f ||
        darks != 0f || shadows != 0f
}

// ==================== 色彩分级色轮 ====================

@Serializable
data class ColorWheel(
    val hue: Float = 0f,        // 0~360
    val saturation: Float = 0f, // 0~100
    val luminance: Float = 0f   // -100~100
) {
    fun hasChanges(): Boolean =
        hue != 0f || saturation != 0f || luminance != 0f

    companion object {
        val DEFAULT = ColorWheel()
    }
}

// ==================== 波形/直方图模型 ====================

data class WaveformData(
    val scanlines: List<FloatArray>,  // 每列256个亮度值
    val maxValue: Float,
    val isParade: Boolean = false
)

data class HistogramFullResult(
    val luminance: IntArray,
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val meanLuminance: Float,
    val medianLuminance: Float,
    val shadowClipping: Boolean,
    val highlightClipping: Boolean,
    val dynamicRange: Float,
    val exposureBias: Float  // 正=过曝倾向, 负=欠曝倾向
)

// ==================== 编辑历史 ====================

@Serializable
data class EditHistoryEntry(
    val id: String,
    val timestamp: Long,
    val params: SmartOptimizeParams,
    val label: String = "",
    val isCheckpoint: Boolean = false
)

// ==================== 编辑面板 Tab ====================

enum class SmartOptimizeTab(
    val label: String,
    val icon: String,
    val description: String
) {
    BASIC("基础", "tune", "曝光/对比度/饱和度/AI"),
    LIGHT("光影", "light_mode", "高光/阴影/去霾"),
    COLOR("色彩", "palette", "色温/色调/HSL"),
    CURVE("曲线", "show_chart", "色调曲线/参数曲线"),
    GRADING("分级", "color_lens", "色彩分级色轮"),
    DETAIL("细节", "grain", "锐化/降噪/纹理"),
    EFFECTS("效果", "blur_on", "颗粒/暗角/褪色/B&W/光晕"),
    OPTICS("光学", "camera", "畸变/色差/透视/负片"),
    CALIBRATION("校准", "settings", "相机校准"),
    LUT("LUT", "filter", "3D LUT 滤镜"),
    MASK("遮罩", "masks", "亮度/AI 遮罩"),
    PRESETS("预设", "auto_awesome", "AI 预设/胶片模拟"),
    HISTORY("历史", "history", "编辑历史记录")
}

// ==================== 预设定义 ====================

@Serializable
data class SmartOptimizePreset(
    val id: String,
    val name: String,
    val description: String,
    val category: PresetCategory,
    val params: SmartOptimizeParams,
    val isBuiltIn: Boolean = true,
    val thumbnailUrl: String? = null
)

enum class PresetCategory(val label: String) {
    FILM_SIMULATION("胶片模拟"),
    COLOR_GRADING("色彩分级"),
    MOOD("情绪氛围"),
    CINEMATIC("电影感"),
    VINTAGE("复古"),
    LANDSCAPE("风景优化"),
    PORTRAIT("人像优化"),
    NIGHT("夜景增强"),
    MONOCHROME("黑白"),
    HDR("HDR效果")
}

// ==================== 胶片模拟预设 (对齐 AlcedoStudio 100+ 胶片) ====================

@Serializable
data class FilmSimulation(
    val id: String,
    val name: String,
    val brand: String,       // Kodak, Fuji, Agfa, etc.
    val series: String,      // Portra, Ektar, Velvia, etc.
    val description: String,
    val colorStyle: String,
    val grainLevel: String,
    val contrastLevel: String,
    val bestFor: String,
    val params: SmartOptimizeParams
)

// ==================== 渲染请求 ====================

data class OptimizeRenderRequest(
    val id: String,
    val inputBitmap: Bitmap,
    val params: SmartOptimizeParams,
    val isPreview: Boolean = false,
    val quality: OptimizeQuality = OptimizeQuality.STANDARD
)

enum class OptimizeQuality(val maxDimension: Int, val label: String) {
    ULTRA(4096, "超高质量"),
    HIGH(2048, "高质量"),
    STANDARD(1024, "标准"),
    PREVIEW(512, "快速预览")
}

// ==================== 渲染结果 ====================

sealed class OptimizeRenderResult {
    data class Success(
        val bitmap: Bitmap,
        val processingTimeMs: Long,
        val quality: OptimizeQuality,
        val histogram: HistogramFullResult? = null
    ) : OptimizeRenderResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : OptimizeRenderResult()
}

// ==================== 色彩科学模式 ====================

enum class ColorScienceMode(val label: String, val description: String) {
    STANDARD("标准", "sRGB 标准色彩管线"),
    ACES_2_0("ACES 2.0", "学院色彩编码系统 2.0"),
    OPEN_DRT("OpenDRT", "开放显示渲染变换"),
    HNCS("HNCS 3.0", "哈苏自然色彩方案")
}

enum class DisplayColorSpace(val label: String) {
    SRGB("sRGB"),
    DISPLAY_P3("Display P3"),
    REC2020("Rec.2020"),
    ADOBE_RGB("Adobe RGB")
}

enum class EOTF(val label: String) {
    SRGB("sRGB"),
    GAMMA_2_2("Gamma 2.2"),
    GAMMA_2_4("Gamma 2.4"),
    PQ("PQ (ST.2084)"),
    HLG("HLG")
}

// ==================== 镜头校正数据 ====================

data class LensProfile(
    val make: String,
    val model: String,
    val distortionParams: FloatArray = FloatArray(5), // k1,k2,k3,p1,p2
    val vignetteParams: FloatArray = FloatArray(3),   // falloff, midpoint, aspect
    val chromaticParams: FloatArray = FloatArray(4)    // red_scale, blue_scale, red_center, blue_center
)

// ==================== 导出配置 ====================

data class ExportConfig(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 95,
    val colorSpace: DisplayColorSpace = DisplayColorSpace.SRGB,
    val maxDimension: Int = 0, // 0 = 原始尺寸
    val sharpening: Float = 0f,
    val includeMetadata: Boolean = true,
    val watermarkEnabled: Boolean = false,
    val watermarkText: String = "HNCS 3.0"
)

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    TIFF("TIFF", "tiff", "image/tiff"),
    WEBP("WebP", "webp", "image/webp")
}