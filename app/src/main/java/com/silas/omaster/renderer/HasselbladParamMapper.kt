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

        // 归一化函数：-30~+30 → -100~+100
        val n30 = { v: Int -> (v / 30f * 100f).coerceIn(-100f, 100f) }
        // 归一化函数：-100~+100 → -100~+100
        val n100 = { key: String ->
            ((colorModeParams[key] ?: 0).toFloat()).coerceIn(-100f, 100f)
        }

        var rp = RenderParameters(
            exposure = n30(hp.tone) + n100("brightness"),
            contrast = n30(hp.contrast) + n100("contrast"),
            saturation = n30(hp.saturation) + n100("saturation"),
            vibrance = (hp.clarity / 30f * 100f).coerceIn(0f, 100f) + n100("vibrance"),
            highlights = n30(hp.highlights) + n100("highlights"),
            shadows = n30(hp.shadows) + n100("shadows"),
            whites = n100("whitePoint"),
            blacks = n100("blackPoint"),
            clarity = ((hp.sharpness + 30) / 60f * 100f).coerceIn(0f, 100f),
            sharpness = ((hp.sharpness + 30) / 60f * 100f).coerceIn(0f, 100f),
            warmth = n30(hp.colorTemp) + n100("temperature"),
            hslRedHue = n100("hslRedHue") + if (hp.cyanMagenta > 0) hp.cyanMagenta / 60f * 180f else 0f,
            hslOrangeHue = n100("hslOrangeHue"),
            hslYellowHue = n100("hslYellowHue"),
            hslGreenHue = n100("hslGreenHue"),
            hslCyanHue = n100("hslCyanHue") + if (hp.cyanMagenta < 0) hp.cyanMagenta / 60f * 180f else 0f,
            hslBlueHue = n100("hslBlueHue"),
            hslPurpleHue = n100("hslPurpleHue"),
            hslMagentaHue = n100("hslMagentaHue"),
            hslRedSaturation = n100("hslRedSat"),
            hslOrangeSaturation = n100("hslOrangeSat"),
            hslYellowSaturation = n100("hslYellowSat"),
            hslGreenSaturation = n100("hslGreenSat"),
            hslCyanSaturation = n100("hslCyanSat"),
            hslBlueSaturation = n100("hslBlueSat"),
            hslPurpleSaturation = n100("hslPurpleSat"),
            hslMagentaSaturation = n100("hslMagentaSat"),
            hslRedLuminance = n100("hslRedLum"),
            hslOrangeLuminance = n100("hslOrangeLum"),
            hslYellowLuminance = n100("hslYellowLum"),
            hslGreenLuminance = n100("hslGreenLum"),
            hslCyanLuminance = n100("hslCyanLum"),
            hslBlueLuminance = n100("hslBlueLum"),
            hslPurpleLuminance = n100("hslPurpleLum"),
            hslMagentaLuminance = n100("hslMagentaLum"),
            lutEnabled = active3DLUTId != null,
            lutStrength = lut3DStrength.coerceIn(0f, 1f) * 100f,
            lutTextureId = 0,
            lutSize = 0
        )

        // 柔光模式映射：SOFT/DREAMY 通过 colorMatrix 模拟柔光效果
        rp = when (hp.softLight) {
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
