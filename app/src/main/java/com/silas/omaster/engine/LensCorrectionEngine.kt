package com.silas.omaster.engine

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 镜头校正引擎
 *
 * 参照 AlcedoStudio 和 RapidRAW 的镜头校正功能：
 * - AlcedoStudio: lensfun 集成（色差校正、暗角校正、畸变校正）
 * - RapidRAW: 色差校正 + 暗角校正
 *
 * 实现：
 * 1. 色差校正（Chromatic Aberration）：修正 R/B 通道偏移
 * 2. 暗角校正（Lens Vignetting）：径向亮度补偿
 * 3. 畸变校正（Barrel/Pincushion Distortion）：桶形/枕形畸变矫正
 *
 * 操作链路：
 * 1. 用户选择镜头校正类型
 * 2. 手动调节参数或选择预设镜头
 * 3. 引擎对图像执行几何/色彩校正
 * 4. 返回校正后 Bitmap
 */
class LensCorrectionEngine {

    /** 校正参数 */
    data class CorrectionParams(
        val caRedOffset: Float = 0f,        // R通道水平偏移 [像素]
        val caRedOffsetV: Float = 0f,       // R通道垂直偏移
        val caBlueOffset: Float = 0f,       // B通道水平偏移
        val caBlueOffsetV: Float = 0f,      // B通道垂直偏移
        val vignetteAmount: Float = 0f,     // 暗角校正量 [0, 100]
        val vignetteRadius: Float = 50f,    // 暗角半径 [0, 100]
        val distortion: Float = 0f,         // 畸变系数（负=桶形，正=枕形）
        val distortionCenterX: Float = 0.5f,// 畸变中心X
        val distortionCenterY: Float = 0.5f // 畸变中心Y
    )

    /** 预设镜头数据 */
    data class LensProfile(
        val name: String,
        val manufacturer: String,
        val focalLength: String,
        val caRed: Float,
        val caBlue: Float,
        val vignetteAmount: Float,
        val distortion: Float
    )

    /** 内置镜头预设 */
    val lensProfiles = listOf(
        LensProfile("标准 50mm", "通用", "50mm", 0.3f, -0.3f, 20f, 0f),
        LensProfile("广角 24mm", "通用", "24mm", 1.2f, -1.0f, 45f, -0.02f),
        LensProfile("长焦 200mm", "通用", "200mm", 0.1f, -0.1f, 10f, 0.005f),
        LensProfile("手机主摄", "通用", "等效26mm", 0.8f, -0.6f, 35f, -0.01f),
        LensProfile("手机超广角", "通用", "等效13mm", 2.5f, -2.0f, 60f, -0.04f),
        LensProfile("微距 100mm", "通用", "100mm", 0.05f, -0.05f, 5f, 0f)
    )

    /**
     * 应用全部镜头校正
     * 顺序：畸变校正 → 色差校正 → 暗角校正
     */
    fun applyCorrections(bitmap: Bitmap, params: CorrectionParams): Bitmap {
        var result = bitmap

        // 1. 畸变校正（最耗资源，先做）
        if (params.distortion != 0f) {
            result = correctDistortion(result, params)
        }

        // 2. 色差校正
        if (params.caRedOffset != 0f || params.caBlueOffset != 0f ||
            params.caRedOffsetV != 0f || params.caBlueOffsetV != 0f
        ) {
            result = correctChromaticAberration(result, params)
        }

        // 3. 暗角校正
        if (params.vignetteAmount > 0f) {
            result = correctVignette(result, params)
        }

        return result
    }

    /**
     * 色差校正（Chromatic Aberration）
     * 修正红蓝通道相对绿通道的偏移
     * 通过对 R/B 通道进行子像素位移实现
     */
    fun correctChromaticAberration(bitmap: Bitmap, params: CorrectionParams): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        val rOffX = params.caRedOffset
        val rOffY = params.caRedOffsetV
        val bOffX = params.caBlueOffset
        val bOffY = params.caBlueOffsetV

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x

                // 绿色通道保持不变
                val g = (src[idx] shr 8) and 0xFF
                val a = (src[idx] ushr 24) and 0xFF

                // 红色通道：按偏移量采样
                val rSrcX = x - rOffX
                val rSrcY = y - rOffY
                val r = bilinearSample(src, w, h, rSrcX, rSrcY, Channel.RED)

                // 蓝色通道：按偏移量采样
                val bSrcX = x - bOffX
                val bSrcY = y - bOffY
                val b = bilinearSample(src, w, h, bSrcX, bSrcY, Channel.BLUE)

                dst[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    private enum class Channel { RED, GREEN, BLUE }

    private fun bilinearSample(
        pixels: IntArray, w: Int, h: Int,
        fx: Float, fy: Float, channel: Channel
    ): Int {
        val x0 = fx.toInt().coerceIn(0, w - 2)
        val y0 = fy.toInt().coerceIn(0, h - 2)
        val x1 = (x0 + 1).coerceIn(0, w - 1)
        val y1 = (y0 + 1).coerceIn(0, h - 1)
        val tx = (fx - x0).coerceIn(0f, 1f)
        val ty = (fy - y0).coerceIn(0f, 1f)

        val shift = when (channel) {
            Channel.RED -> 16
            Channel.GREEN -> 8
            Channel.BLUE -> 0
        }

        val v00 = (pixels[y0 * w + x0] shr shift) and 0xFF
        val v10 = (pixels[y0 * w + x1] shr shift) and 0xFF
        val v01 = (pixels[y1 * w + x0] shr shift) and 0xFF
        val v11 = (pixels[y1 * w + x1] shr shift) and 0xFF

        val top = v00 * (1 - tx) + v10 * tx
        val bottom = v01 * (1 - tx) + v11 * tx
        return (top * (1 - ty) + bottom * ty).toInt().coerceIn(0, 255)
    }

    /**
     * 暗角校正（Lens Vignetting）
     * 基于径向距离的亮度补偿
     * 公式：gain = 1 + amount * (1 - (r / radius)^2)
     */
    fun correctVignette(bitmap: Bitmap, params: CorrectionParams): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val cx = w / 2f
        val cy = h / 2f
        val maxR = sqrt(cx * cx + cy * cy)
        val radius = (params.vignetteRadius / 100f) * maxR
        val amount = params.vignetteAmount / 100f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = x - cx
                val dy = y - cy
                val r = sqrt(dx * dx + dy * dy)
                val normalizedR = (r / radius).coerceAtMost(1.5f)

                // 径向增益：越靠边缘增益越大
                val gain = 1f + amount * (1f - normalizedR * normalizedR).coerceAtLeast(0f)

                val idx = y * w + x
                val p = pixels[idx]
                val rv = ((p shr 16) and 0xFF) * gain
                val gv = ((p shr 8) and 0xFF) * gain
                val bv = (p and 0xFF) * gain

                pixels[idx] = ((p ushr 24) shl 24) or
                        (rv.toInt().coerceIn(0, 255) shl 16) or
                        (gv.toInt().coerceIn(0, 255) shl 8) or
                        (bv.toInt().coerceIn(0, 255))
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * 畸变校正（Barrel/Pincushion Distortion）
     * 使用 Brown-Conrad 模型
     * x_corrected = x * (1 + k1*r^2 + k2*r^4)
     * k < 0: 桶形畸变, k > 0: 枕形畸变
     */
    fun correctDistortion(bitmap: Bitmap, params: CorrectionParams): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        val cx = params.distortionCenterX * w
        val cy = params.distortionCenterY * h
        val k1 = params.distortion
        val maxR = sqrt(cx * cx + cy * cy)
        val normFactor = 1f / maxR  // 归一化到 [0, 1]

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = x - cx
                val dy = y - cy
                val r2 = (dx * dx + dy * dy) * normFactor * normFactor
                val correction = 1f + k1 * r2

                val srcX = (cx + dx * correction).toInt().coerceIn(0, w - 1)
                val srcY = (cy + dy * correction).toInt().coerceIn(0, h - 1)

                dst[y * w + x] = src[srcY * w + srcX]
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(dst, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * 从预设镜头加载参数
     */
    fun paramsFromProfile(profile: LensProfile): CorrectionParams {
        return CorrectionParams(
            caRedOffset = profile.caRed,
            caBlueOffset = profile.caBlue,
            vignetteAmount = profile.vignetteAmount,
            distortion = profile.distortion
        )
    }

    /**
     * 自动检测并应用镜头校正
     * 基于 EXIF 焦距信息自动选择预设
     */
    fun autoCorrect(bitmap: Bitmap, focalLength35mm: Float? = null): Bitmap {
        val params = if (focalLength35mm != null) {
            when {
                focalLength35mm < 20 -> lensProfiles[4].let { paramsFromProfile(it) }  // 超广角
                focalLength35mm < 35 -> lensProfiles[1].let { paramsFromProfile(it) }  // 广角
                focalLength35mm > 100 -> lensProfiles[2].let { paramsFromProfile(it) } // 长焦
                else -> lensProfiles[0].let { paramsFromProfile(it) }                   // 标准
            }
        } else {
            // 无焦距信息时使用手机主摄预设
            paramsFromProfile(lensProfiles[3])
        }

        return applyCorrections(bitmap, params)
    }
}
