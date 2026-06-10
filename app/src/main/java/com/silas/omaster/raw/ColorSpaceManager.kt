package com.silas.omaster.raw

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 色彩空间管理
 *
 * 工作流程：
 * 1. RAW 解码到 ProPhoto RGB / Linear 工作空间
 * 2. 在工作空间中进行所有参数调节
 * 3. 导出时转换到目标色彩空间（sRGB / Adobe RGB / Display P3）
 *
 * 优势：
 * - 避免 sRGB 的色彩裁切
 * - 保留更多高光/阴影细节
 * - 编辑结果可输出到不同色彩空间的目标设备
 */
class ColorSpaceManager {

    /**
     * 色彩空间转换矩阵
     * 用于在不同色彩空间之间转换
     * 矩阵使用标准 sRGB D65 / ProPhoto RGB / Adobe RGB 定义
     */
    object Matrices {
        /** sRGB → ProPhoto RGB (3x3 矩阵，行优先) */
        val sRGB_TO_PROPHOTO = floatArrayOf(
            0.5293f, 0.0989f, 0.0173f,
            0.3304f, 0.8793f, 0.0716f,
            0.1403f, 0.0218f, 0.9111f
        )

        /** ProPhoto RGB → sRGB */
        val PROPHOTO_TO_SRGB = floatArrayOf(
            1.3459f, -0.2556f, -0.0511f,
            -0.5446f, 1.5082f, 0.0205f,
            0.1987f, -0.2526f, 1.0118f
        )

        /** sRGB → Adobe RGB */
        val sRGB_TO_ADOBE = floatArrayOf(
            0.7152f, 0.1663f, 0.0244f,
            0.2848f, 0.8337f, 0.0244f,
            0.0000f, 0.0000f, 0.9512f
        )

        /** Adobe RGB → sRGB */
        val ADOBE_TO_SRGB = floatArrayOf(
            1.3977f, -0.2792f, -0.0358f,
            -0.3977f, 1.2792f, -0.0358f,
            0.0000f, 0.0000f, 1.0716f
        )

        /** XYZ → sRGB (Bradford) */
        val XYZ_TO_SRGB = floatArrayOf(
            3.2406f, -1.5372f, -0.4986f,
            -0.9689f, 1.8758f, 0.0415f,
            0.0557f, -0.2040f, 1.0570f
        )

        /** sRGB → XYZ */
        val SRGB_TO_XYZ = floatArrayOf(
            0.4124f, 0.3576f, 0.1805f,
            0.2126f, 0.7152f, 0.0722f,
            0.0193f, 0.1192f, 0.9505f
        )
    }

    /**
     * Gamma 校正
     * sRGB: gamma 2.2
     * ProPhoto RGB: gamma 1.8
     */
    fun srgbToLinear(value: Float): Float {
        return if (value <= 0.04045f) {
            value / 12.92f
        } else {
            ((value + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    fun linearToSrgb(value: Float): Float {
        return if (value <= 0.0031308f) {
            value * 12.92f
        } else {
            1.055f * value.pow(1f / 2.4f) - 0.055f
        }
    }

    fun prophotoToLinear(value: Float): Float {
        // ProPhoto RGB gamma 1.8
        return if (value < 0.03928f) value / 16f else value.pow(1.8f)
    }

    fun linearToProphoto(value: Float): Float {
        return if (value < 0.001533f) value * 16f else value.pow(1f / 1.8f)
    }

    /**
     * 在两个色彩空间之间转换 Bitmap
     * @param source 原图
     * @param matrix 转换矩阵
     * @param sourceToLinear 源 gamma 反转函数
     * @param linearToTarget 目标 gamma 应用函数
     */
    fun convert(
        source: Bitmap,
        matrix: FloatArray,
        sourceToLinear: (Float) -> Float,
        linearToTarget: (Float) -> Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        for (i in srcPixels.indices) {
            val color = srcPixels[i]
            val r = Color.red(color) / 255f
            val g = Color.green(color) / 255f
            val b = Color.blue(color) / 255f

            // 1. gamma 反转 → linear
            val rLinear = sourceToLinear(r)
            val gLinear = sourceToLinear(g)
            val bLinear = sourceToLinear(b)

            // 2. 矩阵转换
            val rNew = rLinear * matrix[0] + gLinear * matrix[1] + bLinear * matrix[2]
            val gNew = rLinear * matrix[3] + gLinear * matrix[4] + bLinear * matrix[5]
            val bNew = rLinear * matrix[6] + gLinear * matrix[7] + bLinear * matrix[8]

            // 3. gamma 应用 → target
            val rOut = (linearToTarget(rNew.coerceIn(0f, 1f)) * 255f).toInt().coerceIn(0, 255)
            val gOut = (linearToTarget(gNew.coerceIn(0f, 1f)) * 255f).toInt().coerceIn(0, 255)
            val bOut = (linearToTarget(bNew.coerceIn(0f, 1f)) * 255f).toInt().coerceIn(0, 255)

            outPixels[i] = Color.argb(Color.alpha(color), rOut, gOut, bOut)
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * sRGB → ProPhoto RGB
     */
    fun sRGBToProPhoto(source: Bitmap): Bitmap {
        return convert(source, Matrices.sRGB_TO_PROPHOTO, ::srgbToLinear, ::linearToProphoto)
    }

    /**
     * ProPhoto RGB → sRGB
     */
    fun proPhotoToSRGB(source: Bitmap): Bitmap {
        return convert(source, Matrices.PROPHOTO_TO_SRGB, ::prophotoToLinear, ::linearToSrgb)
    }

    /**
     * 在工作色彩空间（ProPhoto Linear）中应用参数调节
     * @param source 输入（应为 ProPhoto Linear）
     * @param params 调节参数
     * @return 调节后的 Bitmap（仍在 ProPhoto Linear）
     */
    fun applyInWorkingSpace(source: Bitmap, params: RawParameters): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val matrix = ColorMatrix()
        buildColorMatrix(matrix, params)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * 构建 ColorMatrix（基于扩展参数）
     * 注意：在 ProPhoto Linear 空间中，所有操作都在更大的动态范围内进行
     */
    private fun buildColorMatrix(matrix: ColorMatrix, params: RawParameters) {
        // 曝光（±5EV，对应 ±500%）
        val exposureFactor = 2.0.pow(params.exposureEV.toDouble()).toFloat()
        // 在线性空间中，曝光 = 乘以系数
        val e = exposureFactor
        matrix.setScale(e, e, e, 1f)

        // 曝光偏移
        if (params.exposureCompensation != 0f) {
            val offset = params.exposureCompensation / 100f * 0.5f
            matrix.postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, offset,
                0f, 1f, 0f, 0f, offset,
                0f, 0f, 1f, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        // 对比度
        val c = 1f + params.contrast / 100f
        val t = 0.5f * (1f - c)
        matrix.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )))

        // 饱和度
        val s = 1f + params.saturation / 100f
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(s)
        matrix.postConcat(satMatrix)

        // 色温 (K → RGB 增益)
        val (rGain, gGain, bGain) = colorTempToRgbGains(params.colorTempKelvin, params.tint)
        matrix.postConcat(ColorMatrix(floatArrayOf(
            rGain, 0f, 0f, 0f, 0f,
            0f, gGain, 0f, 0f, 0f,
            0f, 0f, bGain, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    /**
     * 色温 → RGB 增益
     * 基于 Tanner Helland 近似
     * @param kelvin 色温 (2000K-50000K)
     * @param tint 色调偏移 -100~100
     * @return (R gain, G gain, B gain)
     */
    fun colorTempToRgbGains(kelvin: Int, tint: Int = 0): Triple<Float, Float, Float> {
        val temp = kelvin.coerceIn(2000, 50000) / 100f
        var r: Float
        var g: Float
        var b: Float

        if (temp <= 66) {
            r = 255f
        } else {
            val t = temp - 60f
            r = 329.698727446f * t.pow(-0.1332047592f).toFloat()
        }

        if (temp <= 66) {
            g = 99.4708025861f * temp.ln().toFloat()
        } else {
            val t = temp - 60f
            g = 288.1221695283f * t.pow(-0.0755148492f).toFloat()
        }

        if (temp >= 66) {
            b = 255f
        } else if (temp <= 19) {
            b = 0f
        } else {
            val t = temp - 10f
            b = 138.5177312231f * t.ln().toFloat()
        }

        // 归一化到 [0, 1] 然后应用色调
        r = (r / 255f).coerceIn(0f, 2f)
        g = (g / 255f).coerceIn(0f, 2f)
        b = (b / 255f).coerceIn(0f, 2f)

        // 色调偏移：负值偏绿，正值偏品红
        val tintFactor = tint / 100f * 0.3f
        g -= tintFactor
        r += tintFactor
        b += tintFactor

        return Triple(r, g, b)
    }

    companion object {
        @Volatile
        private var instance: ColorSpaceManager? = null

        fun getInstance(): ColorSpaceManager {
            return instance ?: synchronized(this) {
                instance ?: ColorSpaceManager().also { instance = it }
            }
        }
    }
}

/**
 * RAW 扩展参数
 * 区别于 RenderParameters：
 * - 曝光范围扩展到 ±5EV
 * - 色温 2000K-50000K
 * - 镜头校正参数
 * - 高光/阴影恢复
 * - HSL 颜色调整
 */
data class RawParameters(
    // 基础调节
    val exposure: Float = 0f,                // 基础曝光 [-100, 100]
    val exposureEV: Float = 0f,              // 曝光值（EV） [-5, 5]
    val exposureCompensation: Float = 0f,    // 曝光补偿 [-100, 100]
    val contrast: Float = 0f,                // 对比度 [-100, 100]
    val brightness: Float = 0f,              // 亮度 [-100, 100]
    val saturation: Float = 0f,              // 饱和度 [-100, 100]
    val vibrance: Float = 0f,                // 鲜艳度 [-100, 100]

    // 白平衡
    val colorTempKelvin: Int = 5500,         // 色温 [2000, 50000]
    val tint: Int = 0,                        // 色调 [-100, 100] (绿-品红)

    // 高级调节
    val highlights: Float = 0f,              // 高光 [-100, 100]
    val shadows: Float = 0f,                 // 阴影 [-100, 100]
    val whites: Float = 0f,                  // 白色色阶 [-100, 100]
    val blacks: Float = 0f,                  // 黑色色阶 [-100, 100]
    val clarity: Float = 0f,                 // 清晰度 [-100, 100]
    val dehaze: Float = 0f,                  // 去霾 [-100, 100]
    val texture: Float = 0f,                 // 纹理 [-100, 100]
    val vibranceNew: Float = 0f,             // 新鲜艳度 [-100, 100]
    val sharpness: Float = 0f,               // 锐化 [0, 100]
    val denoise: Float = 0f,                 // 降噪 [0, 100]
    val colorNoise: Float = 0f,              // 颜色降噪 [0, 100]
    val luminanceNoise: Float = 0f,          // 亮度降噪 [0, 100]

    // HSL 颜色调整（按 8 个色相独立调节）
    val hslHue: FloatArray = FloatArray(8),         // 色相 [-100, 100] × 8 色
    val hslSaturation: FloatArray = FloatArray(8),  // 饱和度 [-100, 100] × 8 色
    val hslLuminance: FloatArray = FloatArray(8),   // 亮度 [-100, 100] × 8 色

    // 镜头校正
    val lensCorrectionEnabled: Boolean = false,
    val distortionCorrection: Float = 0f,    // 畸变校正 [-100, 100]
    val chromaticAberrationRemoval: Float = 0f, // 色差去除 [0, 100]
    val vignettingCorrection: Float = 0f,    // 暗角校正 [-100, 100]
    val purpleFringingRemoval: Float = 0f,  // 紫边去除 [0, 100]
    val perspectiveCorrection: Float = 0f,  // 透视校正 [-100, 100]

    // 色调曲线
    val toneCurve: FloatArray? = null,        // 自定义色调曲线

    // 颜色分级（高光/中间调/阴影独立调色）
    val highlightHue: Int = 0,               // 高调色相 [0, 360]
    val highlightSat: Float = 0f,            // 高调饱和度 [0, 100]
    val midtoneHue: Int = 0,                 // 中间调色相
    val midtoneSat: Float = 0f,              // 中间调饱和度
    val shadowHue: Int = 0,                  // 阴影色相
    val shadowSat: Float = 0f,               // 阴影饱和度

    // 效果
    val grain: Float = 0f,                   // 颗粒 [0, 100]
    val grainSize: Float = 0.5f,             // 颗粒大小 [0, 1]
    val fade: Float = 0f,                    // 褪色 [0, 100]
    val toneMappingType: ToneMapping = ToneMapping.STANDARD
) {
    fun hasAnyAdjustment(): Boolean {
        if (exposure != 0f || exposureEV != 0f || exposureCompensation != 0f) return true
        if (contrast != 0f || brightness != 0f || saturation != 0f || vibrance != 0f) return true
        if (colorTempKelvin != 5500 || tint != 0) return true
        if (highlights != 0f || shadows != 0f || whites != 0f || blacks != 0f) return true
        if (clarity != 0f || dehaze != 0f || texture != 0f) return true
        if (sharpness != 0f || denoise != 0f || colorNoise != 0f || luminanceNoise != 0f) return true
        if (hslHue.any { it != 0f }) return true
        if (hslSaturation.any { it != 0f }) return true
        if (hslLuminance.any { it != 0f }) return true
        if (lensCorrectionEnabled) return true
        if (highlightHue != 0 || highlightSat != 0f) return true
        if (midtoneHue != 0 || midtoneSat != 0f) return true
        if (shadowHue != 0 || shadowSat != 0f) return true
        if (grain != 0f || fade != 0f) return true
        return false
    }

    companion object {
        val DEFAULT = RawParameters()

        // 8 色 HSL 索引
        const val HSL_RED = 0
        const val HSL_ORANGE = 1
        const val HSL_YELLOW = 2
        const val HSL_GREEN = 3
        const val HSL_AQUA = 4
        const val HSL_BLUE = 5
        const val HSL_PURPLE = 6
        const val HSL_MAGENTA = 7

        // 色温范围
        const val MIN_COLOR_TEMP = 2000
        const val MAX_COLOR_TEMP = 50000
    }
}

enum class ToneMapping(val displayName: String) {
    STANDARD("标准"),
    FILMIC("电影曲线"),
    ACES("ACES 电影"),
    HDR("HDR"),
    REINHARD("Reinhard")
}
