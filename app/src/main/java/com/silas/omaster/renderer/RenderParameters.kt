package com.silas.omaster.renderer

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 渲染参数数据类
 * 包含18个调校参数的全通道渲染配置
 * 
 * 参数值域说明：
 * - 大部分参数范围：[-100, 100]，0表示无调整
 * - 部分参数范围：[0, 100]，0表示无效果
 * 
 * @property saturation 饱和度 - 色彩饱和度调整 [-100, 100]
 * @property contrast 对比度 - 对比度调整 [-100, 100]
 * @property brightness 亮度 - 亮度调整 [-100, 100]
 * @property warmth 色温 - 色温调整（暖/冷）[-100, 100]，负值偏冷，正值偏暖
 * @property sharpness 锐度 - 锐化处理 [0, 100]
 * @property clarity 清晰度 - 清晰度增强 [0, 100]
 * @property vibrance 鲜艳度 - 自然饱和度 [-100, 100]
 * @property highlights 高光 - 高光调整 [-100, 100]
 * @property shadows 阴影 - 阴影调整 [-100, 100]
 * @property whites 白色 - 白色色阶 [-100, 100]
 * @property blacks 黑色 - 黑色色阶 [-100, 100]
 * @property grain 颗粒 - 胶片颗粒效果 [0, 100]
 * @property fade 褪色 - 褪色效果 [0, 100]
 * @property dehaze 去霾 - 去雾/去霾 [0, 100]
 * @property denoise 降噪 - 噪点抑制 [0, 100]
 * @property skinSmooth 肤色平滑 - 人像肤色处理 [0, 100]
 * @property exposure 曝光 - 曝光补偿 [-100, 100]
 * @property texture 纹理 - 纹理增强 [-100, 100]
 */
@Parcelize
@Serializable
data class RenderParameters(
    // 基础调整参数
    val saturation: Float = 0f,      // 饱和度 [-100, 100]
    val contrast: Float = 0f,         // 对比度 [-100, 100]
    val brightness: Float = 0f,      // 亮度 [-100, 100]
    val warmth: Float = 0f,           // 色温 [-100, 100]
    
    // 细节增强参数
    val sharpness: Float = 0f,        // 锐度 [0, 100]
    val clarity: Float = 0f,          // 清晰度 [0, 100]
    val texture: Float = 0f,          // 纹理 [-100, 100]
    
    // 色彩调整参数
    val vibrance: Float = 0f,         // 鲜艳度 [-100, 100]
    
    // 光影调整参数
    val highlights: Float = 0f,       // 高光 [-100, 100]
    val shadows: Float = 0f,          // 阴影 [-100, 100]
    val whites: Float = 0f,           // 白色色阶 [-100, 100]
    val blacks: Float = 0f,           // 黑色色阶 [-100, 100]
    val exposure: Float = 0f,         // 曝光 [-100, 100]
    
    // 效果参数
    val grain: Float = 0f,            // 颗粒 [0, 100]
    val fade: Float = 0f,             // 褪色 [0, 100]
    val dehaze: Float = 0f,           // 去霾 [0, 100]
    
    // 降噪与平滑参数
    val denoise: Float = 0f,          // 降噪 [0, 100]
    val skinSmooth: Float = 0f,       // 肤色平滑 [0, 100]

    // HSL 8 通道调色（每个通道：色相/饱和度/明度）
    val hslRedHue: Float = 0f,        // 红色色相 [-180, 180]
    val hslRedSaturation: Float = 0f, // 红色饱和度 [-100, 100]
    val hslRedLuminance: Float = 0f,  // 红色明度 [-100, 100]
    val hslOrangeHue: Float = 0f,
    val hslOrangeSaturation: Float = 0f,
    val hslOrangeLuminance: Float = 0f,
    val hslYellowHue: Float = 0f,
    val hslYellowSaturation: Float = 0f,
    val hslYellowLuminance: Float = 0f,
    val hslGreenHue: Float = 0f,
    val hslGreenSaturation: Float = 0f,
    val hslGreenLuminance: Float = 0f,
    val hslCyanHue: Float = 0f,
    val hslCyanSaturation: Float = 0f,
    val hslCyanLuminance: Float = 0f,
    val hslBlueHue: Float = 0f,
    val hslBlueSaturation: Float = 0f,
    val hslBlueLuminance: Float = 0f,
    val hslPurpleHue: Float = 0f,
    val hslPurpleSaturation: Float = 0f,
    val hslPurpleLuminance: Float = 0f,
    val hslMagentaHue: Float = 0f,
    val hslMagentaSaturation: Float = 0f,
    val hslMagentaLuminance: Float = 0f,

    // 曲线查找表（4 通道：rgb/r/g/b，每通道 256 个采样点）
    val curveRgbLut: FloatArray = IDENTITY_CURVE.copyOf(),
    val curveRedLut: FloatArray = IDENTITY_CURVE.copyOf(),
    val curveGreenLut: FloatArray = IDENTITY_CURVE.copyOf(),
    val curveBlueLut: FloatArray = IDENTITY_CURVE.copyOf(),

    // LUT 3D 参数（运行时状态，用于 GPU 管线内的 3D LUT 色彩映射）
    val lutTextureId: Int = 0,
    val lutSize: Int = 0,
    val lutStrength: Float = 0f,
    val lutEnabled: Boolean = false
) : Parcelable {
    
    companion object {
        // 参数范围常量
        const val RANGE_MIN = -100f
        const val RANGE_MAX = 100f
        const val RANGE_POSITIVE_MIN = 0f
        const val RANGE_POSITIVE_MAX = 100f
        
        // 默认参数（无调整）
        val DEFAULT = RenderParameters()

        // 恒等曲线查找表（256 采样，y = x）
        val IDENTITY_CURVE = FloatArray(256) { it / 255f }

        // 参数元数据
        val PARAM_METADATA = listOf(
            ParamMetadata("saturation", "饱和度", RANGE_MIN, RANGE_MAX, "色彩饱和度调整"),
            ParamMetadata("contrast", "对比度", RANGE_MIN, RANGE_MAX, "对比度调整"),
            ParamMetadata("brightness", "亮度", RANGE_MIN, RANGE_MAX, "亮度调整"),
            ParamMetadata("warmth", "色温", RANGE_MIN, RANGE_MAX, "色温调整（暖/冷）"),
            ParamMetadata("sharpness", "锐度", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "锐化处理"),
            ParamMetadata("clarity", "清晰度", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "清晰度增强"),
            ParamMetadata("vibrance", "鲜艳度", RANGE_MIN, RANGE_MAX, "自然饱和度"),
            ParamMetadata("highlights", "高光", RANGE_MIN, RANGE_MAX, "高光调整"),
            ParamMetadata("shadows", "阴影", RANGE_MIN, RANGE_MAX, "阴影调整"),
            ParamMetadata("whites", "白色", RANGE_MIN, RANGE_MAX, "白色色阶"),
            ParamMetadata("blacks", "黑色", RANGE_MIN, RANGE_MAX, "黑色色阶"),
            ParamMetadata("grain", "颗粒", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "胶片颗粒效果"),
            ParamMetadata("fade", "褪色", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "褪色效果"),
            ParamMetadata("dehaze", "去霾", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "去雾/去霾"),
            ParamMetadata("denoise", "降噪", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "噪点抑制"),
            ParamMetadata("skinSmooth", "肤色平滑", RANGE_POSITIVE_MIN, RANGE_POSITIVE_MAX, "人像肤色处理"),
            ParamMetadata("exposure", "曝光", RANGE_MIN, RANGE_MAX, "曝光补偿"),
            ParamMetadata("texture", "纹理", RANGE_MIN, RANGE_MAX, "纹理增强")
        )
        
        /**
         * 从Map创建RenderParameters
         */
        fun fromMap(map: Map<String, Float>): RenderParameters {
            return RenderParameters(
                saturation = map["saturation"] ?: 0f,
                contrast = map["contrast"] ?: 0f,
                brightness = map["brightness"] ?: 0f,
                warmth = map["warmth"] ?: 0f,
                sharpness = map["sharpness"] ?: 0f,
                clarity = map["clarity"] ?: 0f,
                vibrance = map["vibrance"] ?: 0f,
                highlights = map["highlights"] ?: 0f,
                shadows = map["shadows"] ?: 0f,
                whites = map["whites"] ?: 0f,
                blacks = map["blacks"] ?: 0f,
                grain = map["grain"] ?: 0f,
                fade = map["fade"] ?: 0f,
                dehaze = map["dehaze"] ?: 0f,
                denoise = map["denoise"] ?: 0f,
                skinSmooth = map["skinSmooth"] ?: 0f,
                exposure = map["exposure"] ?: 0f,
                texture = map["texture"] ?: 0f
            )
        }
        
        /**
         * 从Int Map创建RenderParameters（兼容整数参数）
         */
        fun fromIntMap(map: Map<String, Int>): RenderParameters {
            return fromMap(map.mapValues { it.value.toFloat() })
        }
    }
    
    /**
     * 转换为Map
     */
    fun toMap(): Map<String, Float> {
        return mapOf(
            "saturation" to saturation,
            "contrast" to contrast,
            "brightness" to brightness,
            "warmth" to warmth,
            "sharpness" to sharpness,
            "clarity" to clarity,
            "vibrance" to vibrance,
            "highlights" to highlights,
            "shadows" to shadows,
            "whites" to whites,
            "blacks" to blacks,
            "grain" to grain,
            "fade" to fade,
            "dehaze" to dehaze,
            "denoise" to denoise,
            "skinSmooth" to skinSmooth,
            "exposure" to exposure,
            "texture" to texture
        )
    }
    
    /**
     * 转换为着色器uniform数组
     * 顺序必须与片段着色器中的uniform数组一致
     */
    fun toShaderUniforms(): FloatArray {
        return floatArrayOf(
            saturation / 100f,       // 归一化到 [-1, 1]
            contrast / 100f,
            brightness / 100f,
            warmth / 100f,
            sharpness / 100f,        // 归一化到 [0, 1]
            clarity / 100f,
            vibrance / 100f,
            highlights / 100f,
            shadows / 100f,
            whites / 100f,
            blacks / 100f,
            grain / 100f,
            fade / 100f,
            dehaze / 100f,
            denoise / 100f,
            skinSmooth / 100f,
            exposure / 100f,
            texture / 100f
        )
    }
    
    /**
     * 检查是否有任何非零参数（含 HSL 与曲线）
     */
    fun hasAnyAdjustment(): Boolean {
        if (saturation != 0f || contrast != 0f || brightness != 0f ||
            warmth != 0f || sharpness != 0f || clarity != 0f ||
            vibrance != 0f || highlights != 0f || shadows != 0f ||
            whites != 0f || blacks != 0f || grain != 0f ||
            fade != 0f || dehaze != 0f || denoise != 0f ||
            skinSmooth != 0f || exposure != 0f || texture != 0f
        ) return true

        // HSL
        if (hslRedHue != 0f || hslRedSaturation != 0f || hslRedLuminance != 0f) return true
        if (hslOrangeHue != 0f || hslOrangeSaturation != 0f || hslOrangeLuminance != 0f) return true
        if (hslYellowHue != 0f || hslYellowSaturation != 0f || hslYellowLuminance != 0f) return true
        if (hslGreenHue != 0f || hslGreenSaturation != 0f || hslGreenLuminance != 0f) return true
        if (hslCyanHue != 0f || hslCyanSaturation != 0f || hslCyanLuminance != 0f) return true
        if (hslBlueHue != 0f || hslBlueSaturation != 0f || hslBlueLuminance != 0f) return true
        if (hslPurpleHue != 0f || hslPurpleSaturation != 0f || hslPurpleLuminance != 0f) return true
        if (hslMagentaHue != 0f || hslMagentaSaturation != 0f || hslMagentaLuminance != 0f) return true

        // 曲线（任一 LUT 非恒等映射即视为有调整）
        if (!curveRgbLut.contentEquals(IDENTITY_CURVE)) return true
        if (!curveRedLut.contentEquals(IDENTITY_CURVE)) return true
        if (!curveGreenLut.contentEquals(IDENTITY_CURVE)) return true
        if (!curveBlueLut.contentEquals(IDENTITY_CURVE)) return true

        // 3D LUT
        if (lutEnabled && lutTextureId != 0) return true

        return false
    }
    
    /**
     * 获取非零参数数量
     */
    fun nonZeroCount(): Int {
        return toMap().values.count { it != 0f }
    }
    
    /**
     * 合并两个参数配置（当前参数覆盖默认参数）
     */
    fun merge(other: RenderParameters): RenderParameters {
        return RenderParameters(
            saturation = if (saturation != 0f) saturation else other.saturation,
            contrast = if (contrast != 0f) contrast else other.contrast,
            brightness = if (brightness != 0f) brightness else other.brightness,
            warmth = if (warmth != 0f) warmth else other.warmth,
            sharpness = if (sharpness != 0f) sharpness else other.sharpness,
            clarity = if (clarity != 0f) clarity else other.clarity,
            vibrance = if (vibrance != 0f) vibrance else other.vibrance,
            highlights = if (highlights != 0f) highlights else other.highlights,
            shadows = if (shadows != 0f) shadows else other.shadows,
            whites = if (whites != 0f) whites else other.whites,
            blacks = if (blacks != 0f) blacks else other.blacks,
            grain = if (grain != 0f) grain else other.grain,
            fade = if (fade != 0f) fade else other.fade,
            dehaze = if (dehaze != 0f) dehaze else other.dehaze,
            denoise = if (denoise != 0f) denoise else other.denoise,
            skinSmooth = if (skinSmooth != 0f) skinSmooth else other.skinSmooth,
            exposure = if (exposure != 0f) exposure else other.exposure,
            texture = if (texture != 0f) texture else other.texture,
            hslRedHue = if (hslRedHue != 0f) hslRedHue else other.hslRedHue,
            hslRedSaturation = if (hslRedSaturation != 0f) hslRedSaturation else other.hslRedSaturation,
            hslRedLuminance = if (hslRedLuminance != 0f) hslRedLuminance else other.hslRedLuminance,
            hslOrangeHue = if (hslOrangeHue != 0f) hslOrangeHue else other.hslOrangeHue,
            hslOrangeSaturation = if (hslOrangeSaturation != 0f) hslOrangeSaturation else other.hslOrangeSaturation,
            hslOrangeLuminance = if (hslOrangeLuminance != 0f) hslOrangeLuminance else other.hslOrangeLuminance,
            hslYellowHue = if (hslYellowHue != 0f) hslYellowHue else other.hslYellowHue,
            hslYellowSaturation = if (hslYellowSaturation != 0f) hslYellowSaturation else other.hslYellowSaturation,
            hslYellowLuminance = if (hslYellowLuminance != 0f) hslYellowLuminance else other.hslYellowLuminance,
            hslGreenHue = if (hslGreenHue != 0f) hslGreenHue else other.hslGreenHue,
            hslGreenSaturation = if (hslGreenSaturation != 0f) hslGreenSaturation else other.hslGreenSaturation,
            hslGreenLuminance = if (hslGreenLuminance != 0f) hslGreenLuminance else other.hslGreenLuminance,
            hslCyanHue = if (hslCyanHue != 0f) hslCyanHue else other.hslCyanHue,
            hslCyanSaturation = if (hslCyanSaturation != 0f) hslCyanSaturation else other.hslCyanSaturation,
            hslCyanLuminance = if (hslCyanLuminance != 0f) hslCyanLuminance else other.hslCyanLuminance,
            hslBlueHue = if (hslBlueHue != 0f) hslBlueHue else other.hslBlueHue,
            hslBlueSaturation = if (hslBlueSaturation != 0f) hslBlueSaturation else other.hslBlueSaturation,
            hslBlueLuminance = if (hslBlueLuminance != 0f) hslBlueLuminance else other.hslBlueLuminance,
            hslPurpleHue = if (hslPurpleHue != 0f) hslPurpleHue else other.hslPurpleHue,
            hslPurpleSaturation = if (hslPurpleSaturation != 0f) hslPurpleSaturation else other.hslPurpleSaturation,
            hslPurpleLuminance = if (hslPurpleLuminance != 0f) hslPurpleLuminance else other.hslPurpleLuminance,
            hslMagentaHue = if (hslMagentaHue != 0f) hslMagentaHue else other.hslMagentaHue,
            hslMagentaSaturation = if (hslMagentaSaturation != 0f) hslMagentaSaturation else other.hslMagentaSaturation,
            hslMagentaLuminance = if (hslMagentaLuminance != 0f) hslMagentaLuminance else other.hslMagentaLuminance,
            curveRgbLut = if (!curveRgbLut.contentEquals(IDENTITY_CURVE)) curveRgbLut else other.curveRgbLut,
            curveRedLut = if (!curveRedLut.contentEquals(IDENTITY_CURVE)) curveRedLut else other.curveRedLut,
            curveGreenLut = if (!curveGreenLut.contentEquals(IDENTITY_CURVE)) curveGreenLut else other.curveGreenLut,
            curveBlueLut = if (!curveBlueLut.contentEquals(IDENTITY_CURVE)) curveBlueLut else other.curveBlueLut,
            // LUT 为运行时状态，优先保留当前实例的 LUT 配置
            lutTextureId = if (lutEnabled) lutTextureId else other.lutTextureId,
            lutSize = if (lutEnabled) lutSize else other.lutSize,
            lutStrength = if (lutEnabled) lutStrength else other.lutStrength,
            lutEnabled = lutEnabled || other.lutEnabled
        )
    }
    
    /**
     * 插值到目标参数
     * @param target 目标参数
     * @param t 插值因子 [0, 1]
     */
    fun lerp(target: RenderParameters, t: Float): RenderParameters {
        val clampedT = t.coerceIn(0f, 1f)
        return RenderParameters(
            saturation = saturation + (target.saturation - saturation) * clampedT,
            contrast = contrast + (target.contrast - contrast) * clampedT,
            brightness = brightness + (target.brightness - brightness) * clampedT,
            warmth = warmth + (target.warmth - warmth) * clampedT,
            sharpness = sharpness + (target.sharpness - sharpness) * clampedT,
            clarity = clarity + (target.clarity - clarity) * clampedT,
            vibrance = vibrance + (target.vibrance - vibrance) * clampedT,
            highlights = highlights + (target.highlights - highlights) * clampedT,
            shadows = shadows + (target.shadows - shadows) * clampedT,
            whites = whites + (target.whites - whites) * clampedT,
            blacks = blacks + (target.blacks - blacks) * clampedT,
            grain = grain + (target.grain - grain) * clampedT,
            fade = fade + (target.fade - fade) * clampedT,
            dehaze = dehaze + (target.dehaze - dehaze) * clampedT,
            denoise = denoise + (target.denoise - denoise) * clampedT,
            skinSmooth = skinSmooth + (target.skinSmooth - skinSmooth) * clampedT,
            exposure = exposure + (target.exposure - exposure) * clampedT,
            texture = texture + (target.texture - texture) * clampedT,
            hslRedHue = hslRedHue + (target.hslRedHue - hslRedHue) * clampedT,
            hslRedSaturation = hslRedSaturation + (target.hslRedSaturation - hslRedSaturation) * clampedT,
            hslRedLuminance = hslRedLuminance + (target.hslRedLuminance - hslRedLuminance) * clampedT,
            hslOrangeHue = hslOrangeHue + (target.hslOrangeHue - hslOrangeHue) * clampedT,
            hslOrangeSaturation = hslOrangeSaturation + (target.hslOrangeSaturation - hslOrangeSaturation) * clampedT,
            hslOrangeLuminance = hslOrangeLuminance + (target.hslOrangeLuminance - hslOrangeLuminance) * clampedT,
            hslYellowHue = hslYellowHue + (target.hslYellowHue - hslYellowHue) * clampedT,
            hslYellowSaturation = hslYellowSaturation + (target.hslYellowSaturation - hslYellowSaturation) * clampedT,
            hslYellowLuminance = hslYellowLuminance + (target.hslYellowLuminance - hslYellowLuminance) * clampedT,
            hslGreenHue = hslGreenHue + (target.hslGreenHue - hslGreenHue) * clampedT,
            hslGreenSaturation = hslGreenSaturation + (target.hslGreenSaturation - hslGreenSaturation) * clampedT,
            hslGreenLuminance = hslGreenLuminance + (target.hslGreenLuminance - hslGreenLuminance) * clampedT,
            hslCyanHue = hslCyanHue + (target.hslCyanHue - hslCyanHue) * clampedT,
            hslCyanSaturation = hslCyanSaturation + (target.hslCyanSaturation - hslCyanSaturation) * clampedT,
            hslCyanLuminance = hslCyanLuminance + (target.hslCyanLuminance - hslCyanLuminance) * clampedT,
            hslBlueHue = hslBlueHue + (target.hslBlueHue - hslBlueHue) * clampedT,
            hslBlueSaturation = hslBlueSaturation + (target.hslBlueSaturation - hslBlueSaturation) * clampedT,
            hslBlueLuminance = hslBlueLuminance + (target.hslBlueLuminance - hslBlueLuminance) * clampedT,
            hslPurpleHue = hslPurpleHue + (target.hslPurpleHue - hslPurpleHue) * clampedT,
            hslPurpleSaturation = hslPurpleSaturation + (target.hslPurpleSaturation - hslPurpleSaturation) * clampedT,
            hslPurpleLuminance = hslPurpleLuminance + (target.hslPurpleLuminance - hslPurpleLuminance) * clampedT,
            hslMagentaHue = hslMagentaHue + (target.hslMagentaHue - hslMagentaHue) * clampedT,
            hslMagentaSaturation = hslMagentaSaturation + (target.hslMagentaSaturation - hslMagentaSaturation) * clampedT,
            hslMagentaLuminance = hslMagentaLuminance + (target.hslMagentaLuminance - hslMagentaLuminance) * clampedT,
            curveRgbLut = curveRgbLut,
            curveRedLut = curveRedLut,
            curveGreenLut = curveGreenLut,
            curveBlueLut = curveBlueLut,
            // LUT 纹理无法插值，保留当前实例的纹理与尺寸；强度可插值
            lutTextureId = lutTextureId,
            lutSize = lutSize,
            lutStrength = lutStrength + (target.lutStrength - lutStrength) * clampedT,
            lutEnabled = lutEnabled
        )
    }
}

/**
 * 参数元数据
 */
data class ParamMetadata(
    val key: String,
    val displayName: String,
    val minValue: Float,
    val maxValue: Float,
    val description: String
)

/**
 * 渲染质量级别
 */
enum class RenderQuality {
    PREVIEW,      // 预览质量（快速渲染）
    STANDARD,     // 标准质量
    HIGH,         // 高质量
    ULTRA         // 超高质量
}

/**
 * 渲染结果
 */
sealed class RenderResult {
    data class Success(
        val outputTextureId: Int,
        val processingTimeMs: Long,
        val quality: RenderQuality,
        val outputBitmap: Bitmap? = null
    ) : RenderResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : RenderResult()
    
    data class FallbackToCPU(
        val reason: String,
        val processingTimeMs: Long,
        val outputBitmap: Bitmap? = null
    ) : RenderResult()
}