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

        // 归一化函数：-30~+30 → -1.0~+1.0
        val n30 = { v: Int -> (v / 30f).coerceIn(-1f, 1f) }
        // 归一化函数：-100~+100 → -1.0~+1.0
        val n100 = { key: String ->
            ((colorModeParams[key] ?: 0) / 100f).coerceIn(-1f, 1f)
        }

        val rp = RenderParameters(
            exposure = ((n30(hp.tone) + n100("brightness")) * 100f).coerceIn(-100f, 100f),
            contrast = ((n30(hp.contrast) + n100("contrast")) * 100f).coerceIn(-100f, 100f),
            saturation = ((n30(hp.saturation) + n100("saturation")) * 100f).coerceIn(-100f, 100f),
            brightness = 0f,
            vibrance = (((hp.clarity / 30f).coerceIn(0f, 1f) + n100("vibrance")) * 100f).coerceIn(-100f, 100f),
            highlights = ((n30(hp.highlights) + n100("highlights")) * 100f).coerceIn(-100f, 100f),
            shadows = ((n30(hp.shadows) + n100("shadows")) * 100f).coerceIn(-100f, 100f),
            whites = (n100("whitePoint") * 100f).coerceIn(-100f, 100f),
            blacks = (n100("blackPoint") * 100f).coerceIn(-100f, 100f),
            clarity = ((hp.sharpness + 30) / 60f * 100f).coerceIn(0f, 100f),
            sharpness = (((hp.sharpness + 30) / 60f) * 0.8f * 100f).coerceIn(0f, 100f),
            warmth = ((n30(hp.colorTemp) + n100("temperature")) * 100f).coerceIn(-100f, 100f),
            hslRedHue = (n100("hslRedHue") * 180f + if (hp.cyanMagenta > 0) hp.cyanMagenta / 60f * 180f else 0f).coerceIn(-180f, 180f),
            hslOrangeHue = (n100("hslOrangeHue") * 180f).coerceIn(-180f, 180f),
            hslYellowHue = (n100("hslYellowHue") * 180f).coerceIn(-180f, 180f),
            hslGreenHue = (n100("hslGreenHue") * 180f).coerceIn(-180f, 180f),
            hslCyanHue = (n100("hslCyanHue") * 180f + if (hp.cyanMagenta < 0) hp.cyanMagenta / 60f * 180f else 0f).coerceIn(-180f, 180f),
            hslBlueHue = (n100("hslBlueHue") * 180f).coerceIn(-180f, 180f),
            hslPurpleHue = (n100("hslPurpleHue") * 180f).coerceIn(-180f, 180f),
            hslMagentaHue = (n100("hslMagentaHue") * 180f).coerceIn(-180f, 180f),
            hslRedSaturation = (n100("hslRedSat") * 100f).coerceIn(-100f, 100f),
            hslOrangeSaturation = (n100("hslOrangeSat") * 100f).coerceIn(-100f, 100f),
            hslYellowSaturation = (n100("hslYellowSat") * 100f).coerceIn(-100f, 100f),
            hslGreenSaturation = (n100("hslGreenSat") * 100f).coerceIn(-100f, 100f),
            hslCyanSaturation = (n100("hslCyanSat") * 100f).coerceIn(-100f, 100f),
            hslBlueSaturation = (n100("hslBlueSat") * 100f).coerceIn(-100f, 100f),
            hslPurpleSaturation = (n100("hslPurpleSat") * 100f).coerceIn(-100f, 100f),
            hslMagentaSaturation = (n100("hslMagentaSat") * 100f).coerceIn(-100f, 100f),
            hslRedLuminance = (n100("hslRedLum") * 100f).coerceIn(-100f, 100f),
            hslOrangeLuminance = (n100("hslOrangeLum") * 100f).coerceIn(-100f, 100f),
            hslYellowLuminance = (n100("hslYellowLum") * 100f).coerceIn(-100f, 100f),
            hslGreenLuminance = (n100("hslGreenLum") * 100f).coerceIn(-100f, 100f),
            hslCyanLuminance = (n100("hslCyanLum") * 100f).coerceIn(-100f, 100f),
            hslBlueLuminance = (n100("hslBlueLum") * 100f).coerceIn(-100f, 100f),
            hslPurpleLuminance = (n100("hslPurpleLum") * 100f).coerceIn(-100f, 100f),
            hslMagentaLuminance = (n100("hslMagentaLum") * 100f).coerceIn(-100f, 100f),
            lutEnabled = active3DLUTId != null,
            lutStrength = (lut3DStrength * 100f).coerceIn(0f, 100f)
        )

        // 柔光模式映射：SOFT/DREAMY 通过调整渲染参数模拟柔光效果
        val adjustedRp = when (hp.softLight) {
            SoftLightMode.SOFT -> {
                // SOFT: 轻微柔化，降低对比，轻微暖调
                rp.copy(
                    contrast = (rp.contrast - 10f).coerceIn(-100f, 100f),
                    saturation = (rp.saturation - 5f).coerceIn(-100f, 100f),
                    sharpness = (rp.sharpness * 0.6f).coerceIn(0f, 100f)
                )
            }
            SoftLightMode.DREAMY -> {
                // DREAMY: 更强柔化，褪色感，暖调偏移
                rp.copy(
                    contrast = (rp.contrast - 20f).coerceIn(-100f, 100f),
                    saturation = (rp.saturation - 10f).coerceIn(-100f, 100f),
                    warmth = (rp.warmth + 10f).coerceIn(-100f, 100f),
                    sharpness = (rp.sharpness * 0.3f).coerceIn(0f, 100f)
                )
            }
            else -> rp
        }

        Log.d(TAG, "Mapped HasselbladParams to RenderParameters: exposure=${adjustedRp.exposure}, contrast=${adjustedRp.contrast}, saturation=${adjustedRp.saturation}")
        return adjustedRp
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
