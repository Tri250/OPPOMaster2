package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 智能优化处理引擎 — 完整移植 AlcedoStudio + RapidRAW 全部功能
 *
 * 处理管线顺序：
 * 1. 光学校正（畸变/色差/透视）
 * 2. 裁剪/变换
 * 3. 基础调整（曝光/亮度/对比度）
 * 4. 光影调整（高光/阴影/白/黑/去霾）
 * 5. 色彩调整（色温/色调/HSL八通道）
 * 6. 色调曲线（参数曲线/点曲线/RGB通道曲线）
 * 7. 色彩分级（阴影/中间调/高光色轮）
 * 8. 细节处理（锐化/降噪/纹理/清晰度）
 * 9. 效果处理（颗粒/暗角/褪色）
 * 10. 面部美化
 * 11. 相机校准
 * 12. 色彩科学（ACES/DRT/色调映射/Sigmoid）
 * 13. LUT 应用
 */
class SmartOptimizeEngine {

    // LUT 缓存（名称 -> Lut3D）
    private val lutCache = mutableMapOf<String, LutProcessor.Lut3D>()
    private val lutProcessor = LutProcessor()

    /**
     * 加载并缓存 LUT 文件内容
     */
    fun loadLut(name: String, cubeContent: String) {
        if (!lutCache.containsKey(name)) {
            lutCache[name] = lutProcessor.parseCube(cubeContent)
        }
    }

    /**
     * 预置内置 LUT（简化版本，用代码生成）
     */
    fun ensureBuiltInLuts() {
        if (lutCache.isEmpty()) {
            // 生成一个通用电影感 LUT（Teal & Orange 风格）
            lutCache["cine_teal"] = generateTealOrangeLut()
            lutCache["cine_bleach"] = generateBleachBypassLut()
            lutCache["vintage_fade"] = generateVintageFadeLut()
            lutCache["kodak_portra"] = generateKodakPortraLut()
            lutCache["fuji_velvia"] = generateFujiVelviaLut()
            lutCache["cine_2383"] = generateCine2383Lut()
            lutCache["cine_arri"] = generateArriLut()
            lutCache["agfa_vista"] = generateAgfaVistaLut()
            lutCache["ilford_hp5"] = generateIlfordHp5Lut()
            lutCache["cine_16mm"] = generate16mmLut()
        }
    }

    // ==================== 主处理入口 ====================

    suspend fun process(
        bitmap: Bitmap,
        params: SmartOptimizeParams,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var progress = 0f

        // Step 1: 光学校正
        if (params.distortion != 0f || params.chromaticAberrationR != 0f || params.chromaticAberrationB != 0f) {
            checkActive { onProgress("光学校正", progress) }
            applyOpticsCorrection(pixels, width, height, params)
        }
        progress = 0.05f

        // Step 2: 裁剪/透视/旋转
        if (params.cropLeft != 0f || params.cropRight != 1f || params.cropTop != 0f || params.cropBottom != 1f ||
            params.perspectiveX != 0f || params.perspectiveY != 0f || params.rotation != 0f) {
            checkActive { onProgress("几何变换", progress) }
            applyGeometryTransform(pixels, width, height, params)
        }
        progress = 0.10f

        // Step 3: 负片转换 (RapidRAW)
        if (params.negativeConversion) {
            checkActive { onProgress("负片转换", progress) }
            applyNegativeConversion(pixels, params)
        }
        progress = 0.12f

        // Step 4: 基础调整
        if (params.exposure != 0f || params.brightness != 0f || params.contrast != 0f) {
            checkActive { onProgress("基础调整", progress) }
            applyBasicAdjustments(pixels, params)
        }
        progress = 0.18f

        // Step 5: 光影调整
        if (params.highlights != 0f || params.shadows != 0f || params.whites != 0f ||
            params.blacks != 0f || params.dehaze != 0f) {
            checkActive { onProgress("光影调整", progress) }
            applyLightAdjustments(pixels, params)
        }
        progress = 0.28f

        // Step 6: 色彩调整
        if (params.temperature != 5500f || params.tint != 0f ||
            params.saturation != 0f || params.vibrance != 0f ||
            params.hslAdjustments.hasChanges()) {
            checkActive { onProgress("色彩调整", progress) }
            applyColorAdjustments(pixels, params)
        }
        progress = 0.38f

        // Step 7: 色调曲线
        if (params.parametricCurve.hasChanges() || params.pointCurve != SmartOptimizeParams.DEFAULT.pointCurve ||
            params.redCurve.any { it.x != 0f || it.y != 0f } || params.greenCurve.any { it.x != 0f || it.y != 0f } ||
            params.blueCurve.any { it.x != 0f || it.y != 0f }) {
            checkActive { onProgress("色调曲线", progress) }
            applyToneCurve(pixels, params)
        }
        progress = 0.48f

        // Step 8: 色彩分级 (CDL + Wheels)
        if (params.shadowWheel.hasChanges() || params.midtoneWheel.hasChanges() ||
            params.highlightWheel.hasChanges() || params.globalWheel.hasChanges()) {
            checkActive { onProgress("色彩分级", progress) }
            applyColorGrading(pixels, params)
        }
        progress = 0.55f

        // Step 9: 细节处理
        if (params.sharpness != 0f || params.luminanceNoiseReduction != 0f ||
            params.colorNoiseReduction != 25f || params.texture != 0f || params.clarity != 0f) {
            checkActive { onProgress("细节处理", progress) }
            applyDetailProcessing(pixels, width, height, params)
        }
        progress = 0.68f

        // Step 10: 效果处理
        if (params.grain != 0f || params.vignette != 0f || params.fade != 0f) {
            checkActive { onProgress("效果处理", progress) }
            applyEffects(pixels, width, height, params)
        }
        progress = 0.75f

        // Step 11: 黑白转换 (AlcedoStudio)
        if (params.blackAndWhite > 0f) {
            checkActive { onProgress("黑白转换", progress) }
            applyBlackAndWhite(pixels, params)
        }
        progress = 0.80f

        // Step 12: 面部美化
        if (params.faceBrightening != 0f || params.faceSmoothness != 50f) {
            checkActive { onProgress("面部美化", progress) }
            applyFaceBeautification(pixels, width, height, params)
        }
        progress = 0.85f

        // Step 13: 相机校准
        if (params.shadowTint != 0f || params.redPrimaryHue != 0f || params.greenPrimaryHue != 0f || params.bluePrimaryHue != 0f) {
            checkActive { onProgress("相机校准", progress) }
            applyCalibration(pixels, params)
        }
        progress = 0.90f

        // Step 14: 色彩科学 (ACES/DRT/Sigmoid)
        if (params.colorScience != "STANDARD" || params.toneMappingStrength != 0f ||
            params.sigmoidContrast != 0f || params.highlightTransition != 0f) {
            checkActive { onProgress("色彩科学", progress) }
            applyColorScience(pixels, params)
        }
        progress = 0.93f

        // Step 15: 光晕效果 (AlcedoStudio Halation)
        if (params.halation > 0f) {
            checkActive { onProgress("光晕效果", progress) }
            applyHalation(pixels, width, height, params)
        }
        progress = 0.96f

        // Step 16: LUT 应用
        if (params.lutIntensity > 0f && params.activeLutName.isNotEmpty()) {
            checkActive { onProgress("LUT", progress) }
            applyLUT(pixels, params)
        }
        progress = 0.98f

        // Step 17: 遮罩应用 (RapidRAW Masks)
        if (params.maskEnabled && params.maskType != "NONE") {
            checkActive { onProgress("遮罩应用", progress) }
            applyMask(pixels, width, height, params)
        }

        checkActive { onProgress("完成", 1.0f) }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }

    /** 快速预览：仅基础+光影+色彩调整 */
    suspend fun processPreview(
        bitmap: Bitmap,
        params: SmartOptimizeParams
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        applyBasicAdjustments(pixels, params)
        applyLightAdjustments(pixels, params)
        applyColorAdjustments(pixels, params)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }

    // ==================== Step 1: 光学校正 ====================

    private fun applyOpticsCorrection(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val temp = pixels.copyOf()
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = sqrt(cx * cx + cy * cy)

        val distStrength = params.distortion / 100f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / maxRadius
                val dy = (y - cy) / maxRadius
                val r = sqrt(dx * dx + dy * dy)

                // 桶形/枕形畸变校正
                val distortion = 1f + distStrength * r * r
                val srcX = (cx + dx * maxRadius * distortion).toInt().coerceIn(0, width - 1)
                val srcY = (cy + dy * maxRadius * distortion).toInt().coerceIn(0, height - 1)

                val srcIdx = srcY * width + srcX
                val color = temp[srcIdx]

                // 色差校正（红/青通道偏移）
                val caR = params.chromaticAberrationR / 100f
                val caB = params.chromaticAberrationB / 100f

                if (caR != 0f || caB != 0f) {
                    val srcXR = (cx + dx * maxRadius * (distortion + caR)).toInt().coerceIn(0, width - 1)
                    val srcXB = (cx + dx * maxRadius * (distortion + caB)).toInt().coerceIn(0, width - 1)
                    val srcYInt = srcY.toInt().coerceIn(0, height - 1)

                    val r = Color.red(temp[srcYInt * width + srcXR])
                    val g = Color.green(color)
                    val b = Color.blue(temp[srcYInt * width + srcXB])

                    pixels[y * width + x] = Color.argb(Color.alpha(color), r, g, b)
                } else {
                    pixels[y * width + x] = color
                }
            }
        }
    }

    // ==================== Step 2: 几何变换 ====================

    private fun applyGeometryTransform(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val temp = pixels.copyOf()
        val cx = width / 2f
        val cy = height / 2f

        // 裁剪边界（在循环外计算，避免重复）
        val cropLeftPx = (params.cropLeft * width).toInt().coerceIn(0, width)
        val cropRightPx = (params.cropRight * width).toInt().coerceIn(0, width)
        val cropTopPx = (params.cropTop * height).toInt().coerceIn(0, height)
        val cropBottomPx = (params.cropBottom * height).toInt().coerceIn(0, height)

        // 透视
        val px = params.perspectiveX / 100f
        val py = params.perspectiveY / 100f

        // 旋转
        val angle = Math.toRadians(params.rotation.toDouble())
        val cosA = cos(angle).toFloat()
        val sinA = sin(angle).toFloat()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x

                // 从输出坐标反推源坐标（逆映射）
                var srcX = x.toFloat()
                var srcY = y.toFloat()

                // 逆旋转
                if (params.rotation != 0f) {
                    val rdx = srcX - cx
                    val rdy = srcY - cy
                    srcX = cx + rdx * cosA + rdy * sinA
                    srcY = cy - rdx * sinA + rdy * cosA
                }

                // 逆透视
                if (px != 0f || py != 0f) {
                    val dx = (srcX - cx) / cx
                    val dy = (srcY - cy) / cy
                    srcX -= px * dx * dy * cx
                    srcY -= py * dx * dy * cy
                }

                // 检查源坐标是否在裁剪区域内
                if (srcX < cropLeftPx || srcX >= cropRightPx || srcY < cropTopPx || srcY >= cropBottomPx) {
                    pixels[i] = Color.TRANSPARENT
                    continue
                }

                val sx = srcX.toInt().coerceIn(0, width - 1)
                val sy = srcY.toInt().coerceIn(0, height - 1)

                pixels[i] = temp[sy * width + sx]
            }
        }
    }

    // ==================== Step 3: 基础调整 ====================

    private fun applyBasicAdjustments(pixels: IntArray, params: SmartOptimizeParams) {
        val exposureFactor = if (params.exposure != 0f) 2.0.pow(params.exposure.toDouble()).toFloat() else 1f
        val brightnessOff = params.brightness / 100f * 0.5f
        val contrastFactor = if (params.contrast != 0f) 1f + params.contrast / 100f else 1f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // 曝光
            if (params.exposure != 0f) {
                r *= exposureFactor; g *= exposureFactor; b *= exposureFactor
            }

            // 亮度
            if (params.brightness != 0f) {
                r += brightnessOff; g += brightnessOff; b += brightnessOff
            }

            // 对比度
            if (params.contrast != 0f) {
                r = 0.5f + (r - 0.5f) * contrastFactor
                g = 0.5f + (g - 0.5f) * contrastFactor
                b = 0.5f + (b - 0.5f) * contrastFactor
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    // ==================== Step 4: 光影调整 ====================

    private fun applyLightAdjustments(pixels: IntArray, params: SmartOptimizeParams) {
        val hFactor = params.highlights / 100f
        val sFactor = params.shadows / 100f
        val wFactor = params.whites / 100f
        val blFactor = params.blacks / 100f
        val dFactor = params.dehaze / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            // 高光（亮度 > 0.5 区域）
            if (hFactor != 0f) {
                val mask = smoothstep(0.5f, 1.0f, lum)
                r *= (1f + hFactor * mask)
                g *= (1f + hFactor * mask)
                b *= (1f + hFactor * mask)
            }

            // 阴影（亮度 < 0.5 区域）
            if (sFactor != 0f) {
                val mask = smoothstep(0.5f, 0.0f, lum)
                r += sFactor * mask * 0.3f
                g += sFactor * mask * 0.3f
                b += sFactor * mask * 0.3f
            }

            // 白色色阶
            if (wFactor != 0f) {
                val mask = smoothstep(0.7f, 1.0f, lum)
                r = 1f - (1f - r) * (1f - wFactor * mask)
                g = 1f - (1f - g) * (1f - wFactor * mask)
                b = 1f - (1f - b) * (1f - wFactor * mask)
            }

            // 黑色色阶
            if (blFactor != 0f) {
                val mask = smoothstep(0.3f, 0.0f, lum)
                r *= (1f + blFactor * mask)
                g *= (1f + blFactor * mask)
                b *= (1f + blFactor * mask)
            }

            // 去霾（对齐 RapidRAW dehaze）
            if (dFactor != 0f) {
                val hsl = rgb2hsl(r, g, b)
                val fogLevel = hsl[2] * (1f - hsl[1])
                val ds = dFactor * fogLevel
                r = 0.5f + (r - 0.5f) * (1f + ds)
                g = 0.5f + (g - 0.5f) * (1f + ds)
                b = 0.5f + (b - 0.5f) * (1f + ds)
                val hsl2 = rgb2hsl(r, g, b)
                hsl2[1] = (hsl2[1] + ds * 0.5f).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl2[0], hsl2[1], hsl2[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    // ==================== Step 5: 色彩调整 ====================

    private fun applyColorAdjustments(pixels: IntArray, params: SmartOptimizeParams) {
        // 色温转换 (Kelvin → RGB factor)
        val tempFactor = (params.temperature - 5500f) / 10000f
        val tintFactor = params.tint / 100f

        val satFactor = 1f + params.saturation / 100f
        val vibFactor = params.vibrance / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // 色温
            if (tempFactor != 0f) {
                r += tempFactor * 0.15f
                b -= tempFactor * 0.15f
            }

            // 色调
            if (tintFactor != 0f) {
                g += tintFactor * 0.1f
                r -= tintFactor * 0.05f
            }

            // 饱和度
            if (satFactor != 1f) {
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + satFactor * (r - gray)
                g = gray + satFactor * (g - gray)
                b = gray + satFactor * (b - gray)
            }

            // 鲜艳度
            if (vibFactor != 0f) {
                val hsl = rgb2hsl(r, g, b)
                val vibAmount = (1f - hsl[1]) * vibFactor
                hsl[1] = (hsl[1] + vibAmount * 0.5f).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // HSL 八通道调整
            if (params.hslAdjustments.hasChanges()) {
                applyHSLAdjustments(r, g, b, params.hslAdjustments).let { (nr, ng, nb) ->
                    r = nr; g = ng; b = nb
                }
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun applyHSLAdjustments(r: Float, g: Float, b: Float, hsl: HSLAdjustments): Triple<Float, Float, Float> {
        val hslVal = rgb2hsl(r, g, b)
        val hue = hslVal[0] * 360f

        // 根据色相确定受影响的通道
        val channelWeights = getHSLChannelWeights(hue)

        var nr = r; var ng = g; var nb = b

        // 对每个通道应用调整
        data class HSLChannel(val h: Float, val s: Float, val l: Float)

        val channels = listOf(
            HSLChannel(hsl.redHue, hsl.redSaturation, hsl.redLuminance),
            HSLChannel(hsl.orangeHue, hsl.orangeSaturation, hsl.orangeLuminance),
            HSLChannel(hsl.yellowHue, hsl.yellowSaturation, hsl.yellowLuminance),
            HSLChannel(hsl.greenHue, hsl.greenSaturation, hsl.greenLuminance),
            HSLChannel(hsl.cyanHue, hsl.cyanSaturation, hsl.cyanLuminance),
            HSLChannel(hsl.blueHue, hsl.blueSaturation, hsl.blueLuminance),
            HSLChannel(hsl.purpleHue, hsl.purpleSaturation, hsl.purpleLuminance),
            HSLChannel(hsl.magentaHue, hsl.magentaSaturation, hsl.magentaLuminance)
        )

        for ((idx, channel) in channels.withIndex()) {
            val weight = channelWeights[idx]
            if (weight <= 0.01f) continue

            if (channel.h != 0f || channel.s != 0f || channel.l != 0f) {
                val curHSL = rgb2hsl(nr, ng, nb)
                curHSL[0] = ((curHSL[0] * 360f + channel.h * weight / 100f) % 360f) / 360f
                if (curHSL[0] < 0f) curHSL[0] += 1f
                curHSL[1] = (curHSL[1] + channel.s * weight / 100f).coerceIn(0f, 1f)
                curHSL[2] = (curHSL[2] + channel.l * weight / 100f).coerceIn(0f, 1f)
                val rgb = hsl2rgb(curHSL[0], curHSL[1], curHSL[2])
                nr = rgb[0]; ng = rgb[1]; nb = rgb[2]
            }
        }

        return Triple(nr, ng, nb)
    }

    private fun getHSLChannelWeights(hue: Float): List<Float> {
        // 8通道: 红(0/360), 橙(30), 黄(60), 绿(120), 青(180), 蓝(240), 紫(270), 品红(300)
        val centers = listOf(0f, 30f, 60f, 120f, 180f, 240f, 270f, 300f)
        return centers.map { center ->
            val diff = abs(hue - center)
            val diff2 = abs(hue - (center + 360f))
            val minDiff = min(diff, diff2)
            if (minDiff < 30f) (30f - minDiff) / 30f else 0f
        }
    }

    // ==================== Step 6: 色调曲线 ====================

    private fun applyToneCurve(pixels: IntArray, params: SmartOptimizeParams) {
        // 构建点曲线 LUT
        val curveLUT = if (params.pointCurve.isNotEmpty()) {
            buildCurveLUT(params.pointCurve)
        } else {
            FloatArray(256) { it / 255f }
        }

        val rCurveLUT = if (params.redCurve.size > 2) buildCurveLUT(params.redCurve) else null
        val gCurveLUT = if (params.greenCurve.size > 2) buildCurveLUT(params.greenCurve) else null
        val bCurveLUT = if (params.blueCurve.size > 2) buildCurveLUT(params.blueCurve) else null

        val pc = params.parametricCurve

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // 参数曲线
            if (pc.hasChanges()) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val factor = applyParametricCurve(lum, pc)
                r *= factor; g *= factor; b *= factor
            }

            // 点曲线
            r = applyCurveLUT(r, curveLUT)
            g = applyCurveLUT(g, curveLUT)
            b = applyCurveLUT(b, curveLUT)

            // RGB 通道独立曲线
            if (rCurveLUT != null) r = applyCurveLUT(r, rCurveLUT)
            if (gCurveLUT != null) g = applyCurveLUT(g, gCurveLUT)
            if (bCurveLUT != null) b = applyCurveLUT(b, bCurveLUT)

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun buildCurveLUT(points: List<CurvePoint>): FloatArray {
        val lut = FloatArray(256)
        val sorted = points.sortedBy { it.x }
        for (i in 0..255) {
            val x = i / 255f
            lut[i] = interpolateCurve(x, sorted)
        }
        return lut
    }

    private fun interpolateCurve(x: Float, points: List<CurvePoint>): Float {
        if (points.isEmpty()) return x
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y

        for (j in 0 until points.size - 1) {
            if (x >= points[j].x && x <= points[j + 1].x) {
                val t = (x - points[j].x) / (points[j + 1].x - points[j].x)
                return points[j].y + t * (points[j + 1].y - points[j].y)
            }
        }
        return x
    }

    private fun applyCurveLUT(value: Float, lut: FloatArray): Float {
        val idx = (value * 255f).toInt().coerceIn(0, 255)
        return lut[idx]
    }

    private fun applyParametricCurve(lum: Float, pc: ParametricCurve): Float {
        var factor = 1f
        val hSplit = pc.highlightSplit / 100f
        val sSplit = pc.shadowSplit / 100f

        if (lum > hSplit) {
            val t = (lum - hSplit) / (1f - hSplit)
            factor += pc.highlights / 100f * t
        } else if (lum > 0.5f) {
            val t = (lum - 0.5f) / (hSplit - 0.5f)
            factor += pc.lights / 100f * (1f - t)
        } else if (lum > sSplit) {
            val t = (lum - sSplit) / (0.5f - sSplit)
            factor += pc.darks / 100f * t
        } else {
            val t = (sSplit - lum) / sSplit
            factor += pc.shadows / 100f * t
        }

        return factor.coerceIn(0.5f, 1.5f)
    }

    // ==================== Step 7: 色彩分级 (CDL + Color Wheels) ====================

    private fun applyColorGrading(pixels: IntArray, params: SmartOptimizeParams) {
        val blend = params.gradingBlend / 100f
        if (blend <= 0f) return

        val balance = params.gradingBalance / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            // 计算各范围权重
            val shadowWeight = (1f - smoothstep(0.3f, 0.5f + balance * 0.2f, lum)) * blend
            val highlightWeight = smoothstep(0.5f - balance * 0.2f, 0.7f, lum) * blend
            val midtoneWeight = (1f - shadowWeight - highlightWeight).coerceAtLeast(0f) * blend

            // 阴影色轮
            if (params.shadowWheel.hasChanges() && shadowWeight > 0.01f) {
                applyWheel(r, g, b, params.shadowWheel, shadowWeight).let { (nr, ng, nb) ->
                    r = nr; g = ng; b = nb
                }
            }

            // 中间调色轮
            if (params.midtoneWheel.hasChanges() && midtoneWeight > 0.01f) {
                applyWheel(r, g, b, params.midtoneWheel, midtoneWeight).let { (nr, ng, nb) ->
                    r = nr; g = ng; b = nb
                }
            }

            // 高光色轮
            if (params.highlightWheel.hasChanges() && highlightWeight > 0.01f) {
                applyWheel(r, g, b, params.highlightWheel, highlightWeight).let { (nr, ng, nb) ->
                    r = nr; g = ng; b = nb
                }
            }

            // 全局色轮
            if (params.globalWheel.hasChanges()) {
                applyWheel(r, g, b, params.globalWheel, blend * 0.5f).let { (nr, ng, nb) ->
                    r = nr; g = ng; b = nb
                }
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun applyWheel(r: Float, g: Float, b: Float, wheel: ColorWheel, weight: Float): Triple<Float, Float, Float> {
        if (wheel.saturation <= 0.01f) return Triple(r, g, b)

        val hueRad = Math.toRadians(wheel.hue.toDouble()).toFloat()
        val sat = wheel.saturation / 100f * weight
        val lum = wheel.luminance / 100f * weight

        // 从色相/饱和度生成 RGB 偏移
        val offsetR = sat * (cos(hueRad) + lum).coerceIn(-1f, 1f)
        val offsetG = sat * (cos(hueRad - 2.094f) + lum).coerceIn(-1f, 1f) // -120°
        val offsetB = sat * (cos(hueRad + 2.094f) + lum).coerceIn(-1f, 1f) // +120°

        return Triple(
            (r + offsetR * 0.3f).coerceIn(0f, 1f),
            (g + offsetG * 0.3f).coerceIn(0f, 1f),
            (b + offsetB * 0.3f).coerceIn(0f, 1f)
        )
    }

    // ==================== Step 8: 细节处理 ====================

    private fun applyDetailProcessing(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        // 清晰度（逐像素自适应对比度，对齐 AlcedoStudio）
        if (params.clarity != 0f) {
            applyClarity(pixels, width, height, params.clarity / 100f)
        }

        // 纹理（高通滤波，对齐 RapidRAW）
        if (params.texture != 0f) {
            applyTexture(pixels, width, height, params.texture / 100f)
        }

        // 降噪（亮度+色彩）
        if (params.luminanceNoiseReduction != 0f || params.colorNoiseReduction != 25f) {
            applyDenoising(pixels, width, height, params)
        }

        // 锐化
        if (params.sharpness != 0f) {
            applySharpening(pixels, width, height, params)
        }
    }

    private fun applyClarity(pixels: IntArray, width: Int, height: Int, strength: Float) {
        if (strength <= 0.01f) return
        val temp = pixels.copyOf()
        val radius = 3
        val blurred = boxBlur(temp, width, height, radius)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            val br = Color.red(blurred[i]) / 255f
            val bg = Color.green(blurred[i]) / 255f
            val bb = Color.blue(blurred[i]) / 255f

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val adaptiveStrength = strength * (1f - abs(lum - 0.5f) * 0.5f)

            val nr = (r + (r - br) * adaptiveStrength * 2f).coerceIn(0f, 1f)
            val ng = (g + (g - bg) * adaptiveStrength * 2f).coerceIn(0f, 1f)
            val nb = (b + (b - bb) * adaptiveStrength * 2f).coerceIn(0f, 1f)

            pixels[i] = Color.argb(a,
                (nr * 255f).toInt(),
                (ng * 255f).toInt(),
                (nb * 255f).toInt())
        }
    }

    private fun applyTexture(pixels: IntArray, width: Int, height: Int, strength: Float) {
        if (abs(strength) < 0.01f) return
        val temp = pixels.copyOf()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val cr = Color.red(pixels[i]) / 255f
                val cg = Color.green(pixels[i]) / 255f
                val cb = Color.blue(pixels[i]) / 255f
                val a = Color.alpha(pixels[i])

                var sr = 0f; var sg = 0f; var sb = 0f
                var wSum = 0f
                val weights = floatArrayOf(0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f, 0.0625f, 0.125f, 0.0625f)
                var wi = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ni = (y + dy) * width + (x + dx)
                        val w = weights[wi++]
                        sr += Color.red(temp[ni]) / 255f * w
                        sg += Color.green(temp[ni]) / 255f * w
                        sb += Color.blue(temp[ni]) / 255f * w
                        wSum += w
                    }
                }
                sr /= wSum; sg /= wSum; sb /= wSum

                val nr = (cr + (cr - sr) * strength * 2f).coerceIn(0f, 1f)
                val ng = (cg + (cg - sg) * strength * 2f).coerceIn(0f, 1f)
                val nb = (cb + (cb - sb) * strength * 2f).coerceIn(0f, 1f)

                pixels[i] = Color.argb(a,
                    (nr * 255f).toInt(),
                    (ng * 255f).toInt(),
                    (nb * 255f).toInt())
            }
        }
    }

    private fun applyDenoising(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val lumaStrength = params.luminanceNoiseReduction / 100f
        val colorStrength = (params.colorNoiseReduction - 25f) / 100f // 25=默认

        if (lumaStrength <= 0.01f && abs(colorStrength) < 0.01f) return

        val temp = pixels.copyOf()
        val radius = 2
        val blurred = boxBlur(temp, width, height, radius)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            val br = Color.red(blurred[i]) / 255f
            val bg = Color.green(blurred[i]) / 255f
            val bb = Color.blue(blurred[i]) / 255f

            if (lumaStrength > 0.01f) {
                // 亮度降噪：边缘感知混合
                val edgeR = abs(r - br)
                val edgeG = abs(g - bg)
                val edgeB = abs(b - bb)
                val maxEdge = maxOf(edgeR, edgeG, edgeB)
                val preserve = min(1f, maxEdge * 2f)
                val blend = lumaStrength * (1f - preserve)

                val nr = (r * (1f - blend) + br * blend).coerceIn(0f, 1f)
                val ng = (g * (1f - blend) + bg * blend).coerceIn(0f, 1f)
                val nb = (b * (1f - blend) + bb * blend).coerceIn(0f, 1f)

                pixels[i] = Color.argb(a,
                    (nr * 255f).toInt(),
                    (ng * 255f).toInt(),
                    (nb * 255f).toInt())
            }

            if (abs(colorStrength) > 0.01f) {
                // 色彩降噪：在 YCbCr 空间降低色度
                val y = 0.299f * r + 0.587f * g + 0.114f * b
                val cb = 0.564f * (b - y)
                val cr = 0.713f * (r - y)
                val reducedCb = cb * (1f - colorStrength)
                val reducedCr = cr * (1f - colorStrength)
                val nr = (y + 1.402f * reducedCr).coerceIn(0f, 1f)
                val ng = (y - 0.344f * reducedCb - 0.714f * reducedCr).coerceIn(0f, 1f)
                val nb = (y + 1.772f * reducedCb).coerceIn(0f, 1f)

                pixels[i] = Color.argb(a,
                    (nr * 255f).toInt(),
                    (ng * 255f).toInt(),
                    (nb * 255f).toInt())
            }
        }
    }

    private fun applySharpening(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val strength = params.sharpness / 100f * 0.5f
        if (strength <= 0.01f) return
        val temp = pixels.copyOf()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val cr = Color.red(pixels[i]) / 255f
                val cg = Color.green(pixels[i]) / 255f
                val cb = Color.blue(pixels[i]) / 255f
                val a = Color.alpha(pixels[i])

                var sr = 0f; var sg = 0f; var sb = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val ni = (y + dy) * width + (x + dx)
                        sr += Color.red(temp[ni]) / 255f
                        sg += Color.green(temp[ni]) / 255f
                        sb += Color.blue(temp[ni]) / 255f
                    }
                }

                // Laplacian 锐化
                val nr = (cr + strength * (cr * 8f - sr)).coerceIn(0f, 1f)
                val ng = (cg + strength * (cg * 8f - sg)).coerceIn(0f, 1f)
                val nb = (cb + strength * (cb * 8f - sb)).coerceIn(0f, 1f)

                pixels[i] = Color.argb(a,
                    (nr * 255f).toInt(),
                    (ng * 255f).toInt(),
                    (nb * 255f).toInt())
            }
        }
    }

    // ==================== Step 9: 效果处理 ====================

    private fun applyEffects(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        // 颗粒
        if (params.grain > 0f) {
            applyGrain(pixels, width, height, params)
        }

        // 暗角
        if (params.vignette != 0f) {
            applyVignette(pixels, width, height, params)
        }

        // 褪色
        if (params.fade > 0f) {
            applyFade(pixels, params)
        }
    }

    private fun applyGrain(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val intensity = params.grain / 100f
        val size = params.grainSize / 100f
        // 使用确定性伪随机
        var seed = 0L

        for (i in pixels.indices) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) and 0xFFFFFFFF
            val noise = (((seed shr 16) and 0xFFFF).toFloat() / 65535f - 0.5f) * 2f

            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val grainStrength = intensity * (1f + (1f - lum) * 0.5f) // 暗部颗粒更多
            val grainValue = noise * grainStrength * 0.12f

            val nr = overlayBlend(r, r + grainValue)
            val ng = overlayBlend(g, g + grainValue)
            val nb = overlayBlend(b, b + grainValue)

            pixels[i] = Color.argb(a,
                (nr * 255f).toInt().coerceIn(0, 255),
                (ng * 255f).toInt().coerceIn(0, 255),
                (nb * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun applyVignette(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val strength = params.vignette / 100f
        val cx = width / 2f
        val cy = height / 2f
        val maxR = sqrt(cx * cx + cy * cy)
        val midpoint = params.vignetteMidpoint / 100f * maxR
        val feather = params.vignetteFeather / 100f * maxR

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val dx = x - cx
                val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)

                val vignetteFactor = if (dist < midpoint) {
                    1f
                } else {
                    val t = (dist - midpoint) / feather
                    (1f - strength * t.coerceIn(0f, 1f)).coerceIn(0f, 1f)
                }

                if (vignetteFactor < 1f) {
                    val r = Color.red(pixels[i]) / 255f * vignetteFactor
                    val g = Color.green(pixels[i]) / 255f * vignetteFactor
                    val b = Color.blue(pixels[i]) / 255f * vignetteFactor
                    val a = Color.alpha(pixels[i])

                    pixels[i] = Color.argb(a,
                        (r * 255f).toInt().coerceIn(0, 255),
                        (g * 255f).toInt().coerceIn(0, 255),
                        (b * 255f).toInt().coerceIn(0, 255))
                }
            }
        }
    }

    private fun applyFade(pixels: IntArray, params: SmartOptimizeParams) {
        val fadeFactor = params.fade / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // 降低对比度
            r = 0.5f + (r - 0.5f) * (1f - fadeFactor * 0.3f)
            g = 0.5f + (g - 0.5f) * (1f - fadeFactor * 0.3f)
            b = 0.5f + (b - 0.5f) * (1f - fadeFactor * 0.3f)

            // 提亮暗部
            r = mix(r, r + 0.1f * fadeFactor, fadeFactor)
            g = mix(g, g + 0.1f * fadeFactor, fadeFactor)
            b = mix(b, b + 0.1f * fadeFactor, fadeFactor)

            // 降低饱和度
            val hsl = rgb2hsl(r, g, b)
            hsl[1] = hsl[1] * (1f - fadeFactor * 0.2f)
            val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
            r = rgb[0]; g = rgb[1]; b = rgb[2]

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    // ==================== Step 10: 面部美化 ====================

    private fun applyFaceBeautification(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val brightStrength = params.faceBrightening / 100f
        val smoothStrength = params.faceSmoothness / 100f
        if (brightStrength <= 0.01f && smoothStrength <= 0.01f) return

        val mask = FloatArray(pixels.size)

        // 肤色检测
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            mask[i] = detectSkin(r, g, b)
        }

        // 蒙版平滑
        smoothMask(mask, width, height, 2)

        // 预计算皮肤区域模糊图（用于磨皮）
        val blurred = if (smoothStrength > 0.01f) {
            boxBlur(pixels, width, height, (smoothStrength * 3f).toInt().coerceAtLeast(1))
        } else null

        // 应用美白 + 磨皮
        for (i in pixels.indices) {
            val m = mask[i]
            if (m <= 0.01f) continue

            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            var nr = r
            var ng = g
            var nb = b

            // 磨皮：边缘感知混合原图与模糊图
            if (smoothStrength > 0.01f && blurred != null) {
                val br = Color.red(blurred[i]) / 255f
                val bg = Color.green(blurred[i]) / 255f
                val bb = Color.blue(blurred[i]) / 255f
                // 边缘检测：避免模糊边缘
                val edgeR = abs(r - br)
                val edgeG = abs(g - bg)
                val edgeB = abs(b - bb)
                val maxEdge = maxOf(edgeR, edgeG, edgeB)
                val preserve = min(1f, maxEdge * 3f)
                val blend = smoothStrength * m * (1f - preserve)
                nr = (r * (1f - blend) + br * blend).coerceIn(0f, 1f)
                ng = (g * (1f - blend) + bg * blend).coerceIn(0f, 1f)
                nb = (b * (1f - blend) + bb * blend).coerceIn(0f, 1f)
            }

            // 美白
            if (brightStrength > 0.01f) {
                val brightness = 1f + 0.3f * brightStrength * m
                nr = nr * brightness - 5f / 255f * brightStrength * m
                ng = ng * brightness
                nb = nb * brightness + 3f / 255f * brightStrength * m

                // 降饱和
                val gray = 0.299f * nr + 0.587f * ng + 0.114f * nb
                val desat = 1f - 0.2f * brightStrength * m
                nr = gray + desat * (nr - gray)
                ng = gray + desat * (ng - gray)
                nb = gray + desat * (nb - gray)
            }

            pixels[i] = Color.argb(a,
                (nr * 255f).toInt().coerceIn(0, 255),
                (ng * 255f).toInt().coerceIn(0, 255),
                (nb * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun detectSkin(r: Int, g: Int, b: Int): Float {
        // YCbCr 肤色检测
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        val cb = 0.564f * (b - y)
        val cr = 0.713f * (r - y)
        if (y in 46f..242f && cb in -45f..25f && cr in 5f..71f && cr > cb + 4f) {
            return 1f
        }
        return 0f
    }

    private fun smoothMask(mask: FloatArray, width: Int, height: Int, radius: Int) {
        val temp = mask.copyOf()
        for (y in radius until height - radius) {
            for (x in radius until width - radius) {
                val i = y * width + x
                var sum = 0f
                var count = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        sum += temp[(y + dy) * width + (x + dx)]
                        count++
                    }
                }
                mask[i] = sum / count
            }
        }
    }

    // ==================== Step 11: 相机校准 ====================

    private fun applyCalibration(pixels: IntArray, params: SmartOptimizeParams) {
        val shadowT = params.shadowTint / 100f

        // 原色校准矩阵
        val rHue = Math.toRadians(params.redPrimaryHue.toDouble() / 100f * 30f).toFloat()
        val rSat = 1f + params.redPrimarySaturation / 100f
        val gHue = Math.toRadians(params.greenPrimaryHue.toDouble() / 100f * 30f).toFloat()
        val gSat = 1f + params.greenPrimarySaturation / 100f
        val bHue = Math.toRadians(params.bluePrimaryHue.toDouble() / 100f * 30f).toFloat()
        val bSat = 1f + params.bluePrimarySaturation / 100f

        if (shadowT == 0f && rHue == 0f && rSat == 1f && gHue == 0f && gSat == 1f && bHue == 0f && bSat == 1f) return

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            // 阴影色调
            if (shadowT != 0f) {
                val shadowMask = (1f - lum) * (1f - lum)
                r += shadowT * shadowMask * 0.1f
                b -= shadowT * shadowMask * 0.1f
            }

            // 原色校准（简化旋转矩阵）
            if (rHue != 0f || rSat != 1f) {
                r = r * rSat * cos(rHue) - g * rSat * sin(rHue)
            }
            if (gHue != 0f || gSat != 1f) {
                g = g * gSat * cos(gHue) - b * gSat * sin(gHue)
            }
            if (bHue != 0f || bSat != 1f) {
                b = b * bSat * cos(bHue) - r * bSat * sin(bHue)
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    // ==================== Step 12: 色彩科学 (ACES / OpenDRT / Sigmoid) ====================

    private fun applyColorScience(pixels: IntArray, params: SmartOptimizeParams) {
        when (params.colorScience) {
            "ACES_2_0" -> applyACESTonemap(pixels, params)
            "OPEN_DRT" -> applyOpenDRT(pixels, params)
            else -> {
                // STANDARD + Sigmoid 对比度 + 高光过渡
                if (params.sigmoidContrast != 0f || params.highlightTransition != 0f || params.toneMappingStrength != 0f) {
                    applySigmoidAndHighlight(pixels, params)
                }
            }
        }
    }

    private fun applyACESTonemap(pixels: IntArray, params: SmartOptimizeParams) {
        // ACES 2.0 RRT 简化版
        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // ACES RRT: (x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14)
            r = acesTonemap(r)
            g = acesTonemap(g)
            b = acesTonemap(b)

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun acesTonemap(x: Float): Float {
        val a = 2.51f; val b = 0.03f; val c = 2.43f; val d = 0.59f; val e = 0.14f
        return ((x * (a * x + b)) / (x * (c * x + d) + e)).coerceIn(0f, 1f)
    }

    private fun applyOpenDRT(pixels: IntArray, params: SmartOptimizeParams) {
        // OpenDRT 简化版色调映射
        val peakLum = params.peakLuminance / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            val maxC = maxOf(r, g, b)
            if (maxC > 0.01f) {
                val compressed = maxC / (maxC + 1f) * peakLum
                val scale = compressed / maxC
                r *= scale; g *= scale; b *= scale
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun applySigmoidAndHighlight(pixels: IntArray, params: SmartOptimizeParams) {
        val sigmoid = params.sigmoidContrast / 100f
        val highlight = params.highlightTransition / 100f
        val toneMap = params.toneMappingStrength / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]) / 255f
            var g = Color.green(pixels[i]) / 255f
            var b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // Sigmoid 对比度
            if (sigmoid > 0.01f) {
                r = sigmoidCurve(r, sigmoid)
                g = sigmoidCurve(g, sigmoid)
                b = sigmoidCurve(b, sigmoid)
            }

            // 高光过渡（对齐 AlcedoStudio film-like highlight transition）
            if (highlight > 0.01f) {
                r = highlightRolloff(r, highlight)
                g = highlightRolloff(g, highlight)
                b = highlightRolloff(b, highlight)
            }

            // 色调映射
            if (toneMap > 0.01f) {
                val maxC = maxOf(r, g, b)
                if (maxC > 0.5f) {
                    val compressed = 0.5f + (maxC - 0.5f) / (1f + toneMap * (maxC - 0.5f) * 2f)
                    val scale = compressed / maxC
                    r *= scale; g *= scale; b *= scale
                }
            }

            pixels[i] = Color.argb(a,
                (r * 255f).toInt().coerceIn(0, 255),
                (g * 255f).toInt().coerceIn(0, 255),
                (b * 255f).toInt().coerceIn(0, 255))
        }
    }

    private fun sigmoidCurve(x: Float, strength: Float): Float {
        // S 型曲线: 1 / (1 + exp(-strength * 10 * (x - 0.5)))
        return (1f / (1f + exp(-strength * 10f * (x - 0.5f)))).coerceIn(0f, 1f)
    }

    private fun highlightRolloff(x: Float, strength: Float): Float {
        // 高光过渡: 缓和高光压缩
        if (x < 0.7f) return x
        val t = (x - 0.7f) / 0.3f
        return 0.7f + 0.3f * (1f - exp(-t * (1f + strength * 3f)))
    }

    // ==================== Step 3: 负片转换 (RapidRAW) ====================

    private fun applyNegativeConversion(pixels: IntArray, params: SmartOptimizeParams) {
        val orangeMask = params.negativeOrangeMask / 100f
        for (i in pixels.indices) {
            var r = 255 - Color.red(pixels[i])
            var g = 255 - Color.green(pixels[i])
            var b = 255 - Color.blue(pixels[i])
            val a = Color.alpha(pixels[i])

            // 去色罩（模拟橙色色罩去除）
            if (orangeMask > 0.01f) {
                val maskR = (r + orangeMask * 30f).coerceIn(0f, 255f)
                val maskG = (g + orangeMask * 15f).coerceIn(0f, 255f)
                val maskB = (b + orangeMask * 5f).coerceIn(0f, 255f)
                r = maskR.toInt()
                g = maskG.toInt()
                b = maskB.toInt()
            }

            pixels[i] = Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        }
    }

    // ==================== Step 11: 黑白转换 (AlcedoStudio) ====================

    private fun applyBlackAndWhite(pixels: IntArray, params: SmartOptimizeParams) {
        val mixRatio = params.blackAndWhite / 100f
        if (mixRatio <= 0.01f) return
        val filterHue = params.blackAndWhiteFilterHue
        val filterStrength = params.blackAndWhiteFilterStrength / 100f

        for (i in pixels.indices) {
            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])

            // 基础亮度
            var lum = 0.299f * r + 0.587f * g + 0.114f * b

            // 模拟彩色滤镜效果（根据色相调整各通道权重）
            if (filterStrength > 0.01f) {
                val hueRad = Math.toRadians(filterHue.toDouble()).toFloat()
                val filterR = (cos(hueRad) + 1f) / 2f
                val filterG = (cos(hueRad - 2.094f) + 1f) / 2f
                val filterB = (cos(hueRad + 2.094f) + 1f) / 2f
                val filtered = (r * filterR + g * filterG + b * filterB) /
                        (filterR + filterG + filterB).coerceAtLeast(0.01f)
                lum = mix(lum, filtered, filterStrength)
            }

            val nr = mix(r, lum, mixRatio)
            val ng = mix(g, lum, mixRatio)
            val nb = mix(b, lum, mixRatio)

            pixels[i] = Color.argb(a,
                (nr * 255f).toInt().coerceIn(0, 255),
                (ng * 255f).toInt().coerceIn(0, 255),
                (nb * 255f).toInt().coerceIn(0, 255))
        }
    }

    // ==================== Step 15: 光晕效果 (AlcedoStudio Halation) ====================

    private fun applyHalation(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val strength = params.halation / 100f
        if (strength <= 0.01f) return
        val temp = pixels.copyOf()

        // 仅对红色通道高亮区域产生光晕（简化版）
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val r = Color.red(pixels[i]) / 255f
                val g = Color.green(pixels[i]) / 255f
                val b = Color.blue(pixels[i]) / 255f
                val a = Color.alpha(pixels[i])

                // 高光检测
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                if (lum > 0.7f) {
                    // 相邻像素红色扩散
                    var haloR = 0f
                    var count = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val ni = (y + dy) * width + (x + dx)
                            haloR += Color.red(temp[ni]) / 255f
                            count++
                        }
                    }
                    haloR /= count
                    val bloom = (lum - 0.7f) / 0.3f * strength * 0.3f
                    val colorShift = params.halationColor / 100f * 0.1f
                    val nr = (r + bloom * haloR * (1f + colorShift)).coerceIn(0f, 1f)
                    val ng = (g + bloom * haloR * 0.3f).coerceIn(0f, 1f)
                    val nb = (b + bloom * haloR * 0.1f * (1f - colorShift)).coerceIn(0f, 1f)
                    pixels[i] = Color.argb(a,
                        (nr * 255f).toInt(),
                        (ng * 255f).toInt(),
                        (nb * 255f).toInt())
                }
            }
        }
    }

    // ==================== Step 17: 遮罩应用 (RapidRAW Masks) ====================

    private fun applyMask(pixels: IntArray, width: Int, height: Int, params: SmartOptimizeParams) {
        val maskIntensity = params.maskIntensity / 100f
        if (maskIntensity <= 0.01f) return
        val low = params.maskLuminanceLow
        val high = params.maskLuminanceHigh

        for (i in pixels.indices) {
            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val a = Color.alpha(pixels[i])
            val lum = 0.299f * r + 0.587f * g + 0.114f * b

            val maskValue = when {
                lum < low -> 0f
                lum > high -> 0f
                else -> {
                    val center = (low + high) / 2f
                    val dist = abs(lum - center)
                    val maxDist = (high - low) / 2f
                    if (maxDist > 0.001f) 1f - (dist / maxDist) else 1f
                }
            }

            // 羽化边缘
            val feathered = maskValue * maskIntensity
            if (feathered > 0.01f) {
                // 亮度遮罩：提亮选中区域
                val boost = 1f + feathered * 0.2f
                val nr = (r * boost).coerceIn(0f, 1f)
                val ng = (g * boost).coerceIn(0f, 1f)
                val nb = (b * boost).coerceIn(0f, 1f)
                pixels[i] = Color.argb(a,
                    (nr * 255f).toInt().coerceIn(0, 255),
                    (ng * 255f).toInt().coerceIn(0, 255),
                    (nb * 255f).toInt().coerceIn(0, 255))
            }
        }
    }

    // ==================== Step 13: LUT ====================

    private fun applyLUT(pixels: IntArray, params: SmartOptimizeParams) {
        ensureBuiltInLuts()
        val lutName = params.activeLutName
        if (lutName.isNotEmpty()) {
            val lut = lutCache[lutName]
            if (lut != null) {
                lutProcessor.applyLut(pixels, lut, params.lutIntensity / 100f)
            }
        }
    }

    // ==================== 内置 LUT 生成器 ====================

    private fun generateIdentityLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    data[idx++] = r / (size - 1f)
                    data[idx++] = g / (size - 1f)
                    data[idx++] = b / (size - 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateTealOrangeLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    // 阴影偏青蓝，高光偏橙黄
                    val shadowWeight = (1f - lum).coerceIn(0f, 1f)
                    val highlightWeight = lum.coerceIn(0f, 1f)
                    data[idx++] = (rf + highlightWeight * 0.08f - shadowWeight * 0.05f).coerceIn(0f, 1f)
                    data[idx++] = (gf + highlightWeight * 0.03f + shadowWeight * 0.08f).coerceIn(0f, 1f)
                    data[idx++] = (bf - highlightWeight * 0.1f + shadowWeight * 0.12f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateBleachBypassLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    // 高对比度，低饱和度
                    val satScale = 0.6f
                    val contrast = 1.3f
                    val nr = ((rf - lum) * satScale + lum).coerceIn(0f, 1f)
                    val ng = ((gf - lum) * satScale + lum).coerceIn(0f, 1f)
                    val nb = ((bf - lum) * satScale + lum).coerceIn(0f, 1f)
                    data[idx++] = (0.5f + (nr - 0.5f) * contrast).coerceIn(0f, 1f)
                    data[idx++] = (0.5f + (ng - 0.5f) * contrast).coerceIn(0f, 1f)
                    data[idx++] = (0.5f + (nb - 0.5f) * contrast).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateVintageFadeLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    // 降低对比度，提亮暗部，偏暖
                    data[idx++] = (rf * 0.9f + 0.08f).coerceIn(0f, 1f)
                    data[idx++] = (gf * 0.9f + 0.06f).coerceIn(0f, 1f)
                    data[idx++] = (bf * 0.85f + 0.1f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateKodakPortraLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    // 暖调，肤色自然，略降饱和
                    data[idx++] = (rf * 1.02f + 0.02f).coerceIn(0f, 1f)
                    data[idx++] = (gf * 0.98f + 0.01f).coerceIn(0f, 1f)
                    data[idx++] = (bf * 0.95f + 0.03f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateFujiVelviaLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    // 高饱和，鲜艳
                    val satScale = 1.2f
                    val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    data[idx++] = ((rf - lum) * satScale + lum).coerceIn(0f, 1f)
                    data[idx++] = ((gf - lum) * satScale + lum).coerceIn(0f, 1f)
                    data[idx++] = ((bf - lum) * satScale + lum).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateCine2383Lut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    val shadowW = (1f - lum).coerceIn(0f, 1f)
                    val hlW = lum.coerceIn(0f, 1f)
                    data[idx++] = (rf * 0.95f + hlW * 0.05f - shadowW * 0.03f).coerceIn(0f, 1f)
                    data[idx++] = (gf * 0.92f + hlW * 0.02f + shadowW * 0.05f).coerceIn(0f, 1f)
                    data[idx++] = (bf * 0.88f - hlW * 0.05f + shadowW * 0.1f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateArriLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    // 自然肤色，略降对比
                    data[idx++] = (0.5f + (rf - 0.5f) * 0.95f + 0.01f).coerceIn(0f, 1f)
                    data[idx++] = (0.5f + (gf - 0.5f) * 0.95f + 0.005f).coerceIn(0f, 1f)
                    data[idx++] = (0.5f + (bf - 0.5f) * 0.92f + 0.015f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateAgfaVistaLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    // 德系浓郁红色
                    data[idx++] = (rf * 1.08f).coerceIn(0f, 1f)
                    data[idx++] = (gf * 0.98f + 0.01f).coerceIn(0f, 1f)
                    data[idx++] = (bf * 0.95f + 0.02f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generateIlfordHp5Lut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    val gray = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    // 黑白，高对比
                    val v = (gray * 1.1f + 0.02f).coerceIn(0f, 1f)
                    data[idx++] = v
                    data[idx++] = v
                    data[idx++] = v
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    private fun generate16mmLut(size: Int = 33): LutProcessor.Lut3D {
        val data = FloatArray(size * size * size * 3)
        var idx = 0
        for (r in 0 until size) {
            for (g in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)
                    val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                    val shadowW = (1f - lum).coerceIn(0f, 1f)
                    // 复古，略暖，暗部偏青
                    data[idx++] = (rf * 0.95f + shadowW * 0.05f + 0.03f).coerceIn(0f, 1f)
                    data[idx++] = (gf * 0.92f + shadowW * 0.08f + 0.02f).coerceIn(0f, 1f)
                    data[idx++] = (bf * 0.88f + shadowW * 0.1f + 0.04f).coerceIn(0f, 1f)
                }
            }
        }
        return LutProcessor.Lut3D(size, data)
    }

    // ==================== 辅助函数 ====================

    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val output = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sr = 0; var sg = 0; var sb = 0; var sa = 0; var count = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val np = pixels[ny * width + nx]
                        sr += Color.red(np)
                        sg += Color.green(np)
                        sb += Color.blue(np)
                        sa += Color.alpha(np)
                        count++
                    }
                }
                output[y * width + x] = Color.argb(sa / count, sr / count, sg / count, sb / count)
            }
        }
        return output
    }

    private fun overlayBlend(base: Float, blend: Float): Float {
        return if (base < 0.5f) {
            (2f * base * blend).coerceIn(0f, 1f)
        } else {
            (1f - 2f * (1f - base) * (1f - blend)).coerceIn(0f, 1f)
        }
    }

    private fun rgb2hsl(r: Float, g: Float, b: Float): FloatArray {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h = 0f
        var s = 0f
        if (delta > 0.0001f) {
            s = if (l < 0.5f) delta / (maxC + minC) else delta / (2f - maxC - minC)
            h = when {
                r >= maxC -> (g - b) / delta
                g >= maxC -> 2f + (b - r) / delta
                else -> 4f + (r - g) / delta
            }
            h /= 6f
            if (h < 0f) h += 1f
        }
        return floatArrayOf(h, s, l)
    }

    private fun hsl2rgb(h: Float, s: Float, l: Float): FloatArray {
        if (s < 0.0001f) return floatArrayOf(l, l, l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        return floatArrayOf(
            hue2rgb(p, q, h + 1f / 3f),
            hue2rgb(p, q, h),
            hue2rgb(p, q, h - 1f / 3f)
        )
    }

    private fun hue2rgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 1f / 2f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private inline fun checkActive(block: () -> Unit) {
        if (!kotlinx.coroutines.currentCoroutineContext().isActive) return
        block()
    }

    // ==================== 图像分析（直方图/波形/矢量） ====================

    fun computeHistogram(pixels: IntArray): HistogramFullResult {
        val lumHist = IntArray(256)
        val rHist = IntArray(256)
        val gHist = IntArray(256)
        val bHist = IntArray(256)
        var totalLum = 0f
        val lumValues = FloatArray(pixels.size)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

            rHist[r]++
            gHist[g]++
            bHist[b]++
            lumHist[lum]++
            totalLum += lum / 255f
            lumValues[i] = lum / 255f
        }

        val meanLum = totalLum / pixels.size
        val sortedLum = lumValues.sortedArray()
        val medianLum = sortedLum[sortedLum.size / 2]

        val shadowPixels = lumHist.take(16).sum()
        val highlightPixels = lumHist.takeLast(16).sum()
        val shadowClipping = shadowPixels > pixels.size * 0.05f
        val highlightClipping = highlightPixels > pixels.size * 0.05f

        val minLum = sortedLum.indexOfFirst { it > 0.01f }.coerceAtLeast(0) / 255f
        val maxLum = sortedLum.indexOfLast { it < 0.99f }.coerceAtLeast(0) / 255f
        val dynamicRange = if (maxLum > minLum) -ln(maxLum - minLum + 0.001f) * 3f else 0f

        val exposureBias = (meanLum - 0.5f) * 2f

        return HistogramFullResult(
            luminance = lumHist,
            red = rHist,
            green = gHist,
            blue = bHist,
            meanLuminance = meanLum,
            medianLuminance = medianLum,
            shadowClipping = shadowClipping,
            highlightClipping = highlightClipping,
            dynamicRange = dynamicRange.coerceIn(0f, 15f),
            exposureBias = exposureBias
        )
    }

    fun computeWaveform(pixels: IntArray, width: Int, height: Int): WaveformData {
        val scanlines = mutableListOf<FloatArray>()
        var maxValue = 0f
        val columns = width.coerceAtMost(512)
        val stepX = width.toFloat() / columns

        for (col in 0 until columns) {
            val scanline = FloatArray(256)
            val xStart = (col * stepX).toInt()
            val xEnd = ((col + 1) * stepX).toInt().coerceAtMost(width)

            for (y in 0 until height) {
                var colLum = 0f
                var count = 0
                for (x in xStart until xEnd) {
                    val i = y * width + x
                    val r = Color.red(pixels[i])
                    val g = Color.green(pixels[i])
                    val b = Color.blue(pixels[i])
                    colLum += (0.299f * r + 0.587f * g + 0.114f * b)
                    count++
                }
                if (count > 0) {
                    val lum = (colLum / count).toInt().coerceIn(0, 255)
                    scanline[lum] += 1f
                }
            }
            scanlines.add(scanline)
            maxValue = maxOf(maxValue, scanline.maxOrNull() ?: 0f)
        }

        return WaveformData(scanlines, maxValue.coerceAtLeast(1f))
    }

    fun computeCullingScore(pixels: IntArray, width: Int, height: Int): Int {
        // 基于锐度、曝光、色彩、动态范围的启发式评分 (RapidRAW Culling)
        var edgeDiff = 0f
        var totalLum = 0f
        var lumSq = 0f
        var colorVar = 0f
        var highlightClip = 0
        var shadowClip = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val r = Color.red(pixels[i]) / 255f
                val g = Color.green(pixels[i]) / 255f
                val b = Color.blue(pixels[i]) / 255f
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                totalLum += lum
                lumSq += lum * lum

                // 简单边缘检测（拉普拉斯近似）
                val left = Color.red(pixels[i - 1]) / 255f
                val right = Color.red(pixels[i + 1]) / 255f
                val top = Color.red(pixels[i - width]) / 255f
                val bottom = Color.red(pixels[i + width]) / 255f
                edgeDiff += abs(4f * r - left - right - top - bottom)

                // 色彩方差
                colorVar += abs(r - g) + abs(g - b) + abs(b - r)

                if (lum > 0.98f) highlightClip++
                if (lum < 0.02f) shadowClip++
            }
        }

        val count = (width - 2) * (height - 2)
        val meanLum = totalLum / count
        val variance = lumSq / count - meanLum * meanLum
        val sharpness = (edgeDiff / count) * 1000f
        val colorScore = (colorVar / count) * 100f
        val exposureScore = 100f - abs(meanLum - 0.45f) * 200f
        val clipPenalty = (highlightClip + shadowClip).toFloat() / count * 500f
        val drScore = sqrt(variance) * 200f

        val score = (sharpness * 0.25f + exposureScore * 0.25f + colorScore * 0.2f + drScore * 0.3f - clipPenalty)
            .coerceIn(0f, 100f).toInt()
        return score
    }

    fun computeVectorscope(pixels: IntArray): Pair<FloatArray, FloatArray> {
        // 简化矢量图：计算 UV 平面上的颜色分布密度
        val gridSize = 128
        val uGrid = FloatArray(gridSize * gridSize)
        val vGrid = FloatArray(gridSize * gridSize)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i]) / 255f
            val g = Color.green(pixels[i]) / 255f
            val b = Color.blue(pixels[i]) / 255f
            val y = 0.299f * r + 0.587f * g + 0.114f * b
            val cb = 0.564f * (b - y)
            val cr = 0.713f * (r - y)

            val gx = ((cb + 0.5f) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
            val gy = ((cr + 0.5f) * (gridSize - 1)).toInt().coerceIn(0, gridSize - 1)
            val idx = gy * gridSize + gx
            uGrid[idx] += 1f
            vGrid[idx] += 1f
        }

        return Pair(uGrid, vGrid)
    }
}