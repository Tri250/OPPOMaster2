package com.silas.omaster.renderer

import android.graphics.ColorMatrix
import android.util.Log
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SoftLightMode

/**
 * 将哈苏之眼参数映射到 GPU RenderParameters
 * 统一 GPU 渲染管线入口，消除 HasselbladColorEngine 的 CPU 重复实现
 */
object HasselbladParamMapper {

    private const val TAG = "HasselbladParamMapper"

    /**
     * HasselbladParams (-30..+30) + 色彩模式参数 (-100..+100) → GPU RenderParameters
     */
    fun map(
        hasselbladParams: HasselbladParams,
        colorModeParams: Map<String, Int> = emptyMap(),
        active3DLUTId: String? = null,
        lut3DStrength: Float = 0.0f
    ): RenderParameters {
        val hp = hasselbladParams

        // 辅助函数：-30~+30 → -100~+100
        val map30to100 = { v: Int -> (v * 100f / 30f).coerceIn(-100f, 100f) }
        // 辅助函数：从 colorModeParams 取值（Int → Float）
        val cm = { key: String -> (colorModeParams[key] ?: 0).toFloat() }

        // ── 基础参数：HasselbladParams → RenderParameters ──
        var exposure = map30to100(hp.tone)                // tone -30..30 → exposure -100..100
        var saturation = map30to100(hp.saturation)        // -30..30 → -100..100
        var contrast = map30to100(hp.contrast)            // -30..30 → -100..100
        var warmth = map30to100(hp.colorTemp)             // -30..30 → -100..100
        var sharpness = (hp.sharpness + 30) * 100f / 60f  // -30..30 → 0..100
        var highlights = map30to100(hp.highlights)        // -30..30 → -100..100
        var shadows = map30to100(hp.shadows)              // -30..30 → -100..100
        var clarity = hp.clarity * 100f / 30f             // 0..30 → 0..100
        var brightness = 0f
        var vibrance = 0f
        var texture = 0f
        var grain = 0f
        var fade = 0f
        var dehaze = 0f
        var denoise = 0f
        var skinSmooth = 0f
        var whites = 0f
        var blacks = 0f

        // ── HSL 色相偏移：cyanMagenta 映射 ──
        // cyanMagenta < 0 偏青（电影感）→ 青色色相偏移；> 0 偏品（复古感）→ 红色色相偏移
        var hslRedHue = if (hp.cyanMagenta > 0) hp.cyanMagenta * 3f else 0f    // ±90 hue
        var hslCyanHue = if (hp.cyanMagenta < 0) hp.cyanMagenta * 3f else 0f   // ±90 hue

        // ── colorModeParams 叠加（-100..+100） ──
        saturation += cm("saturation")
        contrast += cm("contrast")
        brightness = cm("brightness")
        warmth += cm("warmth")
        vibrance = cm("vibrance")
        highlights += cm("highlights")
        shadows += cm("shadows")
        clarity = (clarity + cm("clarity")).coerceIn(0f, 100f)
        sharpness = (sharpness + cm("sharpness")).coerceIn(0f, 100f)
        dehaze = cm("dehaze").coerceIn(0f, 100f)
        denoise = cm("denoise").coerceIn(0f, 100f)
        grain = cm("grain").coerceIn(0f, 100f)
        fade = cm("fade").coerceIn(0f, 100f)
        skinSmooth = cm("skinSmooth").coerceIn(0f, 100f)
        texture = cm("texture")

        // ── colorModeParams HSL 叠加 ──
        hslRedHue += cm("hslRedHue")
        hslCyanHue += cm("hslCyanHue")
        val hslOrangeHue = cm("hslOrangeHue")
        val hslYellowHue = cm("hslYellowHue")
        val hslGreenHue = cm("hslGreenHue")
        val hslBlueHue = cm("hslBlueHue")
        val hslPurpleHue = cm("hslPurpleHue")
        val hslMagentaHue = cm("hslMagentaHue")
        val hslRedSaturation = cm("hslRedSaturation")
        val hslOrangeSaturation = cm("hslOrangeSaturation")
        val hslYellowSaturation = cm("hslYellowSaturation")
        val hslGreenSaturation = cm("hslGreenSaturation")
        val hslCyanSaturation = cm("hslCyanSaturation")
        val hslBlueSaturation = cm("hslBlueSaturation")
        val hslPurpleSaturation = cm("hslPurpleSaturation")
        val hslMagentaSaturation = cm("hslMagentaSaturation")
        val hslRedLuminance = cm("hslRedLuminance")
        val hslOrangeLuminance = cm("hslOrangeLuminance")
        val hslYellowLuminance = cm("hslYellowLuminance")
        val hslGreenLuminance = cm("hslGreenLuminance")
        val hslCyanLuminance = cm("hslCyanLuminance")
        val hslBlueLuminance = cm("hslBlueLuminance")
        val hslPurpleLuminance = cm("hslPurpleLuminance")
        val hslMagentaLuminance = cm("hslMagentaLuminance")

        // ── 柔光模式：SOFT/DREAMY 降低对比、饱和、锐度 ──
        when (hp.softLight) {
            SoftLightMode.SOFT -> {
                contrast = (contrast - 10f).coerceIn(-100f, 100f)
                saturation = (saturation - 5f).coerceIn(-100f, 100f)
                sharpness = (sharpness * 0.6f).coerceIn(0f, 100f)
            }
            SoftLightMode.DREAMY -> {
                contrast = (contrast - 20f).coerceIn(-100f, 100f)
                saturation = (saturation - 10f).coerceIn(-100f, 100f)
                warmth = (warmth + 10f).coerceIn(-100f, 100f)
                sharpness = (sharpness * 0.3f).coerceIn(0f, 100f)
            }
            else -> { /* NONE: 无调整 */ }
        }

        // 暗角由 GPU 外处理（CameraX 预览管线 / 后处理叠加）
        // hp.vignette 不映射到 RenderParameters

        val rp = RenderParameters(
            exposure = exposure,
            contrast = contrast,
            brightness = brightness,
            warmth = warmth,
            sharpness = sharpness,
            clarity = clarity,
            vibrance = vibrance,
            highlights = highlights,
            shadows = shadows,
            whites = whites,
            blacks = blacks,
            grain = grain,
            fade = fade,
            dehaze = dehaze,
            denoise = denoise,
            skinSmooth = skinSmooth,
            texture = texture,
            // HSL 8 通道
            hslRedHue = hslRedHue,
            hslRedSaturation = hslRedSaturation,
            hslRedLuminance = hslRedLuminance,
            hslOrangeHue = hslOrangeHue,
            hslOrangeSaturation = hslOrangeSaturation,
            hslOrangeLuminance = hslOrangeLuminance,
            hslYellowHue = hslYellowHue,
            hslYellowSaturation = hslYellowSaturation,
            hslYellowLuminance = hslYellowLuminance,
            hslGreenHue = hslGreenHue,
            hslGreenSaturation = hslGreenSaturation,
            hslGreenLuminance = hslGreenLuminance,
            hslCyanHue = hslCyanHue,
            hslCyanSaturation = hslCyanSaturation,
            hslCyanLuminance = hslCyanLuminance,
            hslBlueHue = hslBlueHue,
            hslBlueSaturation = hslBlueSaturation,
            hslBlueLuminance = hslBlueLuminance,
            hslPurpleHue = hslPurpleHue,
            hslPurpleSaturation = hslPurpleSaturation,
            hslPurpleLuminance = hslPurpleLuminance,
            hslMagentaHue = hslMagentaHue,
            hslMagentaSaturation = hslMagentaSaturation,
            hslMagentaLuminance = hslMagentaLuminance,
            // LUT 3D（lutTextureId / lutSize 由运行时赋值）
            lutEnabled = active3DLUTId != null,
            lutStrength = lut3DStrength.coerceIn(0f, 1f)
        )

        Log.d(TAG, "Mapped HasselbladParams to RenderParameters: exposure=${rp.exposure}, contrast=${rp.contrast}, saturation=${rp.saturation}")
        return rp
    }

    /**
     * 生成 ColorMatrix（用于 CameraX 实时预览 ColorMatrix 路径）
     */
    fun buildColorMatrix(params: HasselbladParams): ColorMatrix {
        val cm = ColorMatrix()
        val hp = params

        // 饱和度
        val sat = 1f + hp.saturation / 30f
        val satMatrix = ColorMatrix().apply { setSaturation(sat.coerceIn(0f, 2f)) }
        cm.postConcat(satMatrix)

        // 对比度
        val contrast = 1f + hp.contrast / 60f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, 128f * (1f - contrast),
            0f, contrast, 0f, 0f, 128f * (1f - contrast),
            0f, 0f, contrast, 0f, 128f * (1f - contrast),
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        // 色温
        val warmth = hp.colorTemp / 30f
        val warmthMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, warmth * 20f,
            0f, 1f, 0f, 0f, warmth * 5f,
            0f, 0f, 1f, 0f, warmth * -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(warmthMatrix)

        return cm
    }
}
