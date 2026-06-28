package com.silas.omaster.renderer

import android.graphics.ColorMatrix
import android.util.Log
import com.silas.omaster.ai.mapping.HasselbladParams
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
            exposure = n30(hp.tone) + n100("brightness"),
            contrast = n30(hp.contrast) + n100("contrast"),
            saturation = n30(hp.saturation) + n100("saturation"),
            vibrance = (hp.clarity / 30f).coerceIn(0f, 1f) + n100("vibrance"),
            highlights = n30(hp.highlights) + n100("highlights"),
            shadows = n30(hp.shadows) + n100("shadows"),
            whitePoint = n100("whitePoint"),
            blackPoint = n100("blackPoint"),
            clarity = (hp.sharpness + 30) / 60f,
            sharpenAmount = ((hp.sharpness + 30) / 60f) * 0.8f,
            sharpenRadius = 1.0f,
            temperature = n30(hp.colorTemp) + n100("temperature"),
            tint = n100("tint"),
            hueShift = n100("hueShift"),
            hslHueRed = n100("hslRedHue") + if (hp.cyanMagenta > 0) hp.cyanMagenta / 60f else 0f,
            hslHueOrange = n100("hslOrangeHue"),
            hslHueYellow = n100("hslYellowHue"),
            hslHueGreen = n100("hslGreenHue"),
            hslHueCyan = n100("hslCyanHue") + if (hp.cyanMagenta < 0) hp.cyanMagenta / 60f else 0f,
            hslHueBlue = n100("hslBlueHue"),
            hslHuePurple = n100("hslPurpleHue"),
            hslHueMagenta = n100("hslMagentaHue"),
            hslSatRed = n100("hslRedSat"),
            hslSatOrange = n100("hslOrangeSat"),
            hslSatYellow = n100("hslYellowSat"),
            hslSatGreen = n100("hslGreenSat"),
            hslSatCyan = n100("hslCyanSat"),
            hslSatBlue = n100("hslBlueSat"),
            hslSatPurple = n100("hslPurpleSat"),
            hslSatMagenta = n100("hslMagentaSat"),
            hslLumRed = n100("hslRedLum"),
            hslLumOrange = n100("hslOrangeLum"),
            hslLumYellow = n100("hslYellowLum"),
            hslLumGreen = n100("hslGreenLum"),
            hslLumCyan = n100("hslCyanLum"),
            hslLumBlue = n100("hslBlueLum"),
            hslLumPurple = n100("hslPurpleLum"),
            hslLumMagenta = n100("hslMagentaLum"),
            curveMaster = n100("curveMaster"),
            curveRed = n100("curveRed"),
            curveGreen = n100("curveGreen"),
            curveBlue = n100("curveBlue"),
            lut3DEnabled = active3DLUTId != null,
            lut3DStrength = lut3DStrength.coerceIn(0f, 1f),
            lut3DId = active3DLUTId ?: "",
            inputColorSpace = "sRGB",
            outputColorSpace = "sRGB",
            flipHorizontal = false,
            flipVertical = false,
            rotation = 0f
        )

        // 柔光模式映射：SOFT/DREAMY 通过 colorMatrix 模拟柔光效果
        when (hp.softLight) {
            SoftLightMode.SOFT -> {
                // SOFT: 轻微柔化，降低对比，轻微暖调
                rp.copy(
                    contrast = (rp.contrast - 0.1f).coerceIn(-1f, 1f),
                    saturation = (rp.saturation - 0.05f).coerceIn(-1f, 1f),
                    sharpenAmount = (rp.sharpenAmount * 0.6f).coerceIn(0f, 1f)
                )
            }
            SoftLightMode.DREAMY -> {
                // DREAMY: 更强柔化，褪色感，暖调偏移
                rp.copy(
                    contrast = (rp.contrast - 0.2f).coerceIn(-1f, 1f),
                    saturation = (rp.saturation - 0.1f).coerceIn(-1f, 1f),
                    temperature = (rp.temperature + 0.1f).coerceIn(-1f, 1f),
                    sharpenAmount = (rp.sharpenAmount * 0.3f).coerceIn(0f, 1f)
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
