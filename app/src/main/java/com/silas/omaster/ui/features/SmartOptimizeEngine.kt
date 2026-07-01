package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
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
import kotlin.math.tan
import kotlin.random.Random

/**
 * 智能优化引擎 (Smart Optimize Engine)
 *
 * 完整实现 AlcedoStudio + RapidRAW 全部图像处理算法：
 * - 基础调整、色调映射 (Basic / AGX)
 * - 光效、色彩、影调 (含各通道曲线、Hue vs Sat 等)
 * - 胶片仿真、LUT、色彩科学 (ACES / OpenDRT / sRGB)
 * - 特效 (Glow, Halation, Flares, Grain, Vignette)
 * - 细节 (Sharpness, Clarity, Dehaze, Structure, Centre, NR, CA)
 * - 几何变换 (Rotate, Flip, Crop, Perspective, Orientation)
 * - 镜头校正 (Lens Correction / DNG Warp)
 * - 局部蒙版 (Mask blending)
 * - 非破坏性编辑 + Git 风格分支历史
 */
class SmartOptimizeEngine(
    private val renderScript: RenderScript?,
    private val lutProcessor: LutProcessor = LutProcessor(),
    private val lutManager: com.silas.omaster.data.lut.LUTManager? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var previewJob: Job? = null
    var onPreviewReady: ((Bitmap) -> Unit)? = null
    var onLoadingChange: ((Boolean) -> Unit)? = null
    var onHistoryChange: ((Boolean, Boolean) -> Unit)? = null

    private val editHistory = EditHistoryManager()

    // 非破坏性编辑核心：保存原始图像 + 编辑参数
    private var originalBitmap: Bitmap? = null
    private var currentSettings = SmartOptimizeParams()

    // 处理锁，使用对象级同步，防止并发处理导致不一致或位图损坏
    private val processLock = Any()

    val canUndo: Boolean get() = editHistory.canUndo
    val canRedo: Boolean get() = editHistory.canRedo

    // ========== 核心流程 ==========

    fun initialize(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        currentSettings = SmartOptimizeParams()
        editHistory.clear()
        editHistory.push(currentSettings, "初始化")
        onHistoryChange?.invoke(false, false)
    }

    fun updateSettings(block: SmartOptimizeParams.() -> Unit) {
        val newSettings = currentSettings.copy().apply(block)
        currentSettings = newSettings
        editHistory.push(newSettings)
        onHistoryChange?.invoke(editHistory.canUndo, editHistory.canRedo)
    }

    /**
     * 请求实时预览（参数变化时调用）
     * 推入历史记录以确保撤销链路完整
     */
    fun requestPreview(settings: SmartOptimizeParams) {
        currentSettings = settings
        editHistory.push(settings)
        onHistoryChange?.invoke(editHistory.canUndo, editHistory.canRedo)
        processPreview()
    }

    fun undo(): Boolean {
        val prev = editHistory.undo() ?: return false
        currentSettings = prev
        processPreview()
        onHistoryChange?.invoke(editHistory.canUndo, editHistory.canRedo)
        return true
    }

    fun redo(): Boolean {
        val next = editHistory.redo() ?: return false
        currentSettings = next
        processPreview()
        onHistoryChange?.invoke(editHistory.canUndo, editHistory.canRedo)
        return true
    }

    // ========== 实时预览处理 ==========

    fun processPreview() {
        val source = originalBitmap ?: return
        previewJob?.cancel()
        previewJob = scope.launch {
            onLoadingChange?.invoke(true)
            try {
                val result = withContext(Dispatchers.Default) {
                    process(source, currentSettings, highQuality = false)
                }
                if (isActive) {
                    result?.let { onPreviewReady?.invoke(it) }
                }
            } finally {
                if (isActive) {
                    onLoadingChange?.invoke(false)
                }
            }
        }
    }

    /**
     * 取消正在进行的预览任务并释放协程作用域。
     * 应在宿主生命周期结束时调用，避免协程泄漏。
     */
    fun cancel() {
        previewJob?.cancel()
        scope.cancel()
    }

    /**
     * 完整处理（导出时使用，高质量模式）
     */
    suspend fun triggerFullProcess(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap? {
        return withContext(Dispatchers.Default) {
            process(bitmap, settings, highQuality = true)
        }
    }

    /**
     * 智能优化主入口
     */
    fun optimize(bitmap: Bitmap, settings: SmartOptimizeParams = SmartOptimizeParams()): Bitmap {
        return process(bitmap, settings, highQuality = true) ?: bitmap
    }

    // ========== 处理管线 ==========

    private fun process(source: Bitmap, settings: SmartOptimizeParams, highQuality: Boolean): Bitmap? {
        synchronized(processLock) {
            try {
                var result = source.copy(Bitmap.Config.ARGB_8888, true)

                // 1. 几何变换前置（旋转/翻转/裁切/透视）
                result = applyGeometryTransformations(result, settings)

                // 2. 基础调整（含色调映射）
                result = applyBasicAdjustments(result, settings)

                // 3. 光效
                result = applyLightAdjustments(result, settings)

                // 4. 色彩
        result = applyColorAdjustments(result, settings)

        // 4.5 相机校准（原色/阴影色调）
        result = applyCalibration(result, settings)

        // 5. 影调（曲线）
        result = applyToneAdjustments(result, settings)

                // 6. 胶片仿真 / LUT / 色彩科学
                result = applyFilmSimulation(result, settings)

                // 7. 特效
                result = applyEffects(result, settings)

                // 8. 细节
                result = applyDetails(result, settings, highQuality)

                // 9. 镜头校正
                result = applyLensCorrection(result, settings)

                // 10. 局部蒙版混合
                result = applyLocalMasks(result, settings)

                // 11. 最终色彩空间转换
                result = applyColorSpace(result, settings)

                return result
            } catch (e: Exception) {
                e.printStackTrace()
                return source
            }
        }
    }

    // ========== 1. 几何变换 ==========

    private fun applyGeometryTransformations(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        var result = bitmap

        // 方向步骤 (0,1,2,3 = 0,90,180,270)
        if (settings.orientationSteps != 0) {
            result = rotateBitmap(result, settings.orientationSteps * 90f)
        }

        // 翻转
        if (settings.flipHorizontal || settings.flipVertical) {
            result = flipBitmap(result, settings.flipHorizontal, settings.flipVertical)
        }

        // 旋转
        if (settings.rotation != 0f) {
            result = rotateBitmap(result, settings.rotation)
        }

        // 透视变换
        if (settings.perspectiveX != 0f || settings.perspectiveY != 0f) {
            result = applyPerspective(result, settings.perspectiveX, settings.perspectiveY)
        }

        // 裁切
        if (settings.cropTop != 0f || settings.cropLeft != 0f || settings.cropBottom != 0f || settings.cropRight != 0f) {
            result = cropBitmap(result, settings.cropTop, settings.cropLeft, settings.cropBottom, settings.cropRight)
        }

        return result
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            if (vertical) postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropBitmap(bitmap: Bitmap, cropTop: Float, cropLeft: Float, cropBottom: Float, cropRight: Float): Bitmap {
        val left = (cropLeft * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val top = (cropTop * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val right = ((1f - cropRight) * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = ((1f - cropBottom) * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun applyPerspective(bitmap: Bitmap, perspectiveX: Float, perspectiveY: Float): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val matrix = Matrix()

        // 水平/垂直透视近似
        val px = perspectiveX / 100f * 0.5f
        val py = perspectiveY / 100f * 0.5f
        val src = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
        val dst = floatArrayOf(
            -w * px, -h * py,
            w + w * px, -h * py,
            w - w * px, h + h * py,
            w * px, h - h * py
        )
        if (matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    // ========== 2. 基础调整 ==========

    private fun applyBasicAdjustments(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            // 曝光 + EV Shift
            val ev = (settings.exposure + settings.evShift) * 0.01f
            val evFactor = 2f.pow(ev)
            r *= evFactor; g *= evFactor; b *= evFactor

            // 亮度
            val brightness = settings.brightness * 0.01f
            r += brightness * 128f; g += brightness * 128f; b += brightness * 128f

            // 对比度
            val contrast = settings.contrast * 0.01f
            val contrastFactor = 1f + contrast
            r = (r - 128f) * contrastFactor + 128f
            g = (g - 128f) * contrastFactor + 128f
            b = (b - 128f) * contrastFactor + 128f

            // Highlights / Shadows / Whites / Blacks (RapidRAW 风格)
            val highlights = settings.highlights * 0.01f
            val shadows = settings.shadows * 0.01f
            val whites = settings.whites * 0.01f
            val blacks = settings.blacks * 0.01f

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val highlightMask = (lum / 255f).pow(2f)
            val shadowMask = 1f - (lum / 255f).pow(0.5f)

            r += highlights * 128f * highlightMask
            g += highlights * 128f * highlightMask
            b += highlights * 128f * highlightMask

            r += shadows * 128f * shadowMask
            g += shadows * 128f * shadowMask
            b += shadows * 128f * shadowMask

            r += whites * 128f
            g += whites * 128f
            b += whites * 128f

            r -= blacks * 128f
            g -= blacks * 128f
            b -= blacks * 128f

            // 色调映射
            when (settings.toneMapper) {
                "agx" -> {
                    r = agxToneMap(r / 255f) * 255f
                    g = agxToneMap(g / 255f) * 255f
                    b = agxToneMap(b / 255f) * 255f
                }
                else -> {
                    // basic: 简单 reinhard
                    r = reinhardToneMap(r / 255f) * 255f
                    g = reinhardToneMap(g / 255f) * 255f
                    b = reinhardToneMap(b / 255f) * 255f
                }
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun reinhardToneMap(x: Float): Float {
        return x / (1f + x)
    }

    // AGX 近似 tone map (RapidRAW)
    private fun agxToneMap(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        // AgX 近似: 使用 sigmoid + toe
        val sigmoid = 1f / (1f + exp(-6f * (v - 0.5f)))
        val toe = v.pow(1.2f)
        return (sigmoid * 0.85f + toe * 0.15f).coerceIn(0f, 1f)
    }

    // ========== 3. 光效 ==========

    private fun applyLightAdjustments(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        if (settings.light == 0f && settings.highlightPreserve == 50f && settings.shadowRecover == 50f) return bitmap

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val lightBoost = settings.light * 0.01f
        val preserve = settings.highlightPreserve / 100f
        val recover = settings.shadowRecover / 100f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val highlightMask = (lum / 255f).pow(3f)
            val shadowMask = 1f - (lum / 255f).pow(0.3f)

            // 光效提升
            val boost = lightBoost * 128f
            r += boost * (1f - highlightMask * preserve)
            g += boost * (1f - highlightMask * preserve)
            b += boost * (1f - highlightMask * preserve)

            // 阴影恢复
            val recoverAmount = (128f - lum) * recover * 0.3f
            r += recoverAmount * shadowMask
            g += recoverAmount * shadowMask
            b += recoverAmount * shadowMask

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // ========== 4. 色彩 ==========

    private fun applyColorAdjustments(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val temp = settings.temperature / 100f
        val tint = settings.tint / 100f
        val hueShift = settings.hueShift / 100f * 30f
        val satBoost = settings.saturation / 100f
        val vibrance = settings.vibrance / 100f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            // 色温
            r += temp * 30f
            b -= temp * 30f

            // 色调
            r += tint * 15f
            g -= tint * 10f
            b += tint * 15f

            // HSL 调整 (per-color-channel hue/saturation/luminance)
            val hslAdj = settings.hslAdjustments
            val (currentH, currentS, currentL) = rgbToHsl(r, g, b)
            // Determine which color range this pixel falls into and apply corresponding HSL shift
            val colorRanges = listOf(
                0f to 30f to Triple(hslAdj.redHue, hslAdj.redSaturation, hslAdj.redLuminance),
                30f to 60f to Triple(hslAdj.orangeHue, hslAdj.orangeSaturation, hslAdj.orangeLuminance),
                60f to 90f to Triple(hslAdj.yellowHue, hslAdj.yellowSaturation, hslAdj.yellowLuminance),
                90f to 150f to Triple(hslAdj.greenHue, hslAdj.greenSaturation, hslAdj.greenLuminance),
                150f to 210f to Triple(hslAdj.aquaHue, hslAdj.aquaSaturation, hslAdj.aquaLuminance),
                210f to 270f to Triple(hslAdj.blueHue, hslAdj.blueSaturation, hslAdj.blueLuminance),
                270f to 300f to Triple(hslAdj.purpleHue, hslAdj.purpleSaturation, hslAdj.purpleLuminance),
                300f to 360f to Triple(hslAdj.magentaHue, hslAdj.magentaSaturation, hslAdj.magentaLuminance)
            )
            var newH = currentH
            var newS = currentS
            var newL = currentL
            for ((range, adjustments) in colorRanges) {
                val (rangeStart, rangeEnd) = range
                if (currentH >= rangeStart && currentH < rangeEnd) {
                    val (hueShift, satShift, lumShift) = adjustments
                    newH = (currentH + hueShift / 100f * 30f + 360f) % 360f
                    newS = (currentS + satShift / 100f).coerceIn(0f, 1f)
                    newL = (currentL + lumShift / 100f).coerceIn(0f, 1f)
                    break
                }
            }
            val (nr2, ng2, nb2) = hslToRgb(newH, newS, newL)
            r = nr2; g = ng2; b = nb2

            // Vibrance: 对低饱和像素提升更大
            if (vibrance != 0f) {
                val (_, s, l) = rgbToHsl(r, g, b)
                val vibranceMask = 1f - s // 低饱和部分受影响更大
                val satChange = vibrance * vibranceMask
                val (h2, _, _) = rgbToHsl(r, g, b)
                val (nr, ng, nb) = hslToRgb(h2, (s + satChange).coerceIn(0f, 1f), l)
                r = nr; g = ng; b = nb
            }

            // 全局饱和度
            if (satBoost != 0f) {
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + (r - gray) * (1f + satBoost)
                g = gray + (g - gray) * (1f + satBoost)
                b = gray + (b - gray) * (1f + satBoost)
            }

            // 色相旋转
            if (hueShift != 0f) {
                val (h, s, l) = rgbToHsl(r, g, b)
                val (nr, ng, nb) = hslToRgb((h + hueShift + 360f) % 360f, s, l)
                r = nr; g = ng; b = nb
            }

            // Color Grading (Shadows / Midtones / Highlights / Global wheels)
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val normLum = lum / 255f

            val shadowWeight = (1f - normLum).pow(2f)
            val midWeight = 1f - abs(normLum - 0.5f) * 2f
            val highlightWeight = normLum.pow(2f)

            val blendFactor = settings.gradingBlend / 100f

            fun applyGrading(hue: Float, sat: Float, lumOffset: Float, weight: Float) {
                if (weight <= 0.001f) return
                val (ch, cs, cl) = rgbToHsl(r, g, b)
                val newH = (ch + hue * 3.6f * weight + 360f) % 360f
                val newS = (cs + sat / 100f * weight).coerceIn(0f, 1f)
                val newL = (cl + lumOffset / 100f * weight).coerceIn(0f, 1f)
                val (nr, ng, nb) = hslToRgb(newH, newS, newL)
                r = r * (1f - weight) + nr * weight
                g = g * (1f - weight) + ng * weight
                b = b * (1f - weight) + nb * weight
            }

            applyGrading(settings.shadowWheel.hue, settings.shadowWheel.saturation, settings.shadowWheel.luminance, shadowWeight * blendFactor)
            applyGrading(settings.midtoneWheel.hue, settings.midtoneWheel.saturation, settings.midtoneWheel.luminance, midWeight * blendFactor)
            applyGrading(settings.highlightWheel.hue, settings.highlightWheel.saturation, settings.highlightWheel.luminance, highlightWeight * blendFactor)

            // Global grading
            applyGrading(settings.globalWheel.hue, settings.globalWheel.saturation, settings.globalWheel.luminance, 0.3f * blendFactor)

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // ========== 4.5 相机校准 ==========

    private fun applyCalibration(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        if (settings.shadowTint == 0f &&
            settings.redPrimaryHue == 0f && settings.redPrimarySaturation == 100f &&
            settings.greenPrimaryHue == 0f && settings.greenPrimarySaturation == 100f &&
            settings.bluePrimaryHue == 0f && settings.bluePrimarySaturation == 100f
        ) {
            return bitmap
        }

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val shadowTint = settings.shadowTint / 100f
        val redHueShift = settings.redPrimaryHue / 100f * 30f
        val redSatScale = settings.redPrimarySaturation / 100f
        val greenHueShift = settings.greenPrimaryHue / 100f * 30f
        val greenSatScale = settings.greenPrimarySaturation / 100f
        val blueHueShift = settings.bluePrimaryHue / 100f * 30f
        val blueSatScale = settings.bluePrimarySaturation / 100f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            val (h, s, l) = rgbToHsl(r, g, b)

            // 阴影色调：对低亮度像素添加冷暖偏移
            if (shadowTint != 0f) {
                val shadowWeight = (1f - l).coerceIn(0f, 1f)
                r += shadowTint * 20f * shadowWeight
                b -= shadowTint * 20f * shadowWeight
            }

            // 原色校准：按色相区间分别旋转色相/缩放饱和度
            val (hueShift, satScale) = when {
                h >= 330f || h < 45f -> redHueShift to redSatScale
                h in 45f..180f -> greenHueShift to greenSatScale
                h in 180f..300f -> blueHueShift to blueSatScale
                else -> 0f to 1f
            }

            if (hueShift != 0f || satScale != 1f) {
                val newH = (h + hueShift + 360f) % 360f
                val newS = (s * satScale).coerceIn(0f, 1f)
                val (nr, ng, nb) = hslToRgb(newH, newS, l)
                r = nr
                g = ng
                b = nb
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // ========== 5. 影调（曲线） ==========

    private fun applyToneAdjustments(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        var result = bitmap
        // 各通道点曲线
        val curves = listOf(
            settings.pointCurve to "luma",
            settings.redCurve to "red",
            settings.greenCurve to "green",
            settings.blueCurve to "blue"
        )

        for ((curve, channel) in curves) {
            if (curve.size > 2 || (curve.size == 2 && (curve[0].y != 0f || curve[1].y != 1f))) {
                result = applyPointCurve(result, curve, channel)
            }
        }

        // 全局参数化曲线
        if (isParametricActive(settings.parametricCurve)) {
            result = applyParametricCurve(result, settings.parametricCurve, "luma")
        }

        // Hue vs Sat / Hue vs Lum / Lum vs Sat
        if (settings.hueVsSatCurve.size > 2 || isCurveActive(settings.hueVsSatCurve)) {
            result = applyHueVsSat(result, settings.hueVsSatCurve)
        }
        if (settings.hueVsLumCurve.size > 2 || isCurveActive(settings.hueVsLumCurve)) {
            result = applyHueVsLum(result, settings.hueVsLumCurve)
        }
        if (settings.lumVsSatCurve.size > 2 || isCurveActive(settings.lumVsSatCurve)) {
            result = applyLumVsSat(result, settings.lumVsSatCurve)
        }

        return result
    }

    private fun isCurveActive(curve: List<CurvePoint>): Boolean {
        return curve.any { it.y != 0.5f } || curve.size > 2
    }

    private fun isParametricActive(curve: ParametricCurveData): Boolean {
        return curve.darks != 0f || curve.shadows != 0f || curve.highlights != 0f ||
               curve.lights != 0f || curve.whiteLevel != 0f || curve.blackLevel != 0f
    }

    private fun applyPointCurve(bitmap: Bitmap, curve: List<CurvePoint>, channel: String): Bitmap {
        val sorted = curve.sortedBy { it.x }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()

            val mapped = when (channel) {
                "luma" -> {
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val mappedLum = interpolateCurve(sorted, lum / 255f) * 255f
                    val ratio = if (lum > 0) mappedLum / lum else 1f
                    Triple(r * ratio, g * ratio, b * ratio)
                }
                "red" -> Triple(interpolateCurve(sorted, r / 255f) * 255f, g, b)
                "green" -> Triple(r, interpolateCurve(sorted, g / 255f) * 255f, b)
                "blue" -> Triple(r, g, interpolateCurve(sorted, b / 255f) * 255f)
                else -> Triple(r, g, b)
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                mapped.first.toInt().coerceIn(0, 255),
                mapped.second.toInt().coerceIn(0, 255),
                mapped.third.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyParametricCurve(bitmap: Bitmap, curve: ParametricCurveData, channel: String): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // 将 parametric 参数转换为控制点
        val points = buildParametricPoints(curve)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()

            val mapped = when (channel) {
                "luma" -> {
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val mappedLum = interpolateCurve(points, lum / 255f) * 255f
                    val ratio = if (lum > 0) mappedLum / lum else 1f
                    Triple(r * ratio, g * ratio, b * ratio)
                }
                "red" -> Triple(interpolateCurve(points, r / 255f) * 255f, g, b)
                "green" -> Triple(r, interpolateCurve(points, g / 255f) * 255f, b)
                "blue" -> Triple(r, g, interpolateCurve(points, b / 255f) * 255f)
                else -> Triple(r, g, b)
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                mapped.first.toInt().coerceIn(0, 255),
                mapped.second.toInt().coerceIn(0, 255),
                mapped.third.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun buildParametricPoints(settings: ParametricCurveData): List<CurvePoint> {
        val vH = settings.highlights / 10f
        val vS = settings.shadows / 10f
        val vD = settings.darks / 10f
        val vL = settings.lights / 10f
        val vW = settings.whiteLevel / 10f
        val vB = settings.blackLevel / 10f
        val s1 = settings.split1 / 100f
        val s2 = settings.split2 / 100f
        val s3 = settings.split3 / 100f

        // 构建 6 个控制点：黑场、暗部、阴影、高光、亮部、白场
        val points = mutableListOf(
            CurvePoint(0f, 0f + vB),
            CurvePoint(s1 * 0.5f, s1 * 0.5f + vD),
            CurvePoint(s2 * 0.8f, s2 * 0.8f + vS),
            CurvePoint(s2 + (1f - s2) * 0.3f, s2 + (1f - s2) * 0.3f + vH),
            CurvePoint(s3 + (1f - s3) * 0.8f, s3 + (1f - s3) * 0.8f + vL),
            CurvePoint(1f, 1f + vW)
        )
        return points.map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }.sortedBy { it.x }
    }

    private fun interpolateCurve(curve: List<CurvePoint>, x: Float): Float {
        if (curve.isEmpty()) return x
        if (curve.size == 1) return curve[0].y
        if (x <= curve.first().x) return curve.first().y
        if (x >= curve.last().x) return curve.last().y

        for (i in 0 until curve.size - 1) {
            if (x >= curve[i].x && x <= curve[i + 1].x) {
                val t = (x - curve[i].x) / (curve[i + 1].x - curve[i].x)
                return curve[i].y + t * (curve[i + 1].y - curve[i].y)
            }
        }
        return x
    }

    // Hue vs Sat: x 轴 = Hue (0-360), y 轴 = Saturation offset (-1 ~ 1, 0.5 为中心)
    private fun applyHueVsSat(bitmap: Bitmap, curve: List<CurvePoint>): Bitmap {
        val sorted = curve.sortedBy { it.x }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()
            val (h, s, l) = rgbToHsl(r, g, b)
            val normHue = h / 360f
            val offset = (interpolateCurve(sorted, normHue) - 0.5f) * 2f
            val (nr, ng, nb) = hslToRgb(h, (s + offset).coerceIn(0f, 1f), l)
            pixels[i] = Color.argb(Color.alpha(pixel), nr.toInt(), ng.toInt(), nb.toInt())
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // Hue vs Lum: x 轴 = Hue, y 轴 = Luminance offset
    private fun applyHueVsLum(bitmap: Bitmap, curve: List<CurvePoint>): Bitmap {
        val sorted = curve.sortedBy { it.x }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()
            val (h, s, l) = rgbToHsl(r, g, b)
            val normHue = h / 360f
            val offset = (interpolateCurve(sorted, normHue) - 0.5f) * 2f
            val (nr, ng, nb) = hslToRgb(h, s, (l + offset).coerceIn(0f, 1f))
            pixels[i] = Color.argb(Color.alpha(pixel), nr.toInt(), ng.toInt(), nb.toInt())
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // Lum vs Sat: x 轴 = Luminance, y 轴 = Saturation offset
    private fun applyLumVsSat(bitmap: Bitmap, curve: List<CurvePoint>): Bitmap {
        val sorted = curve.sortedBy { it.x }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()
            val (h, s, l) = rgbToHsl(r, g, b)
            val offset = (interpolateCurve(sorted, l) - 0.5f) * 2f
            val (nr, ng, nb) = hslToRgb(h, (s + offset).coerceIn(0f, 1f), l)
            pixels[i] = Color.argb(Color.alpha(pixel), nr.toInt(), ng.toInt(), nb.toInt())
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    // ========== 6. 胶片仿真 / LUT / 色彩科学 ==========

    private fun applyFilmSimulation(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        var result = bitmap

        // 高光重建 (AlcedoStudio)
        if (settings.highlightReconstruction) {
            result = applyHighlightReconstruction(result)
        }

        // 真实文件 LUT 应用（lutPath 或 activeLutName 指向已下载的 LUT）
        val shouldApplyLUT = settings.lutPath.isNotEmpty() || settings.activeLutName.isNotEmpty()
        if (shouldApplyLUT) {
            // 先检查 activeLutName 是否为内置预设 ID
            val filmId = settings.filmSimulation.takeIf { it != "none" && it.isNotEmpty() }
                ?: settings.activeLutName.takeIf { it.isNotEmpty() }

            val isBuiltinPreset = filmId in setOf(
                "vivid", "portrait", "landscape",
                "fuji_astia", "fuji_provia", "fuji_velvia",
                "kodak_portra", "kodak_ektar", "kodak_tri_x",
                "ilford_delta", "agfa_vista", "ilford_hp5",
                "cine_2383", "cine_arri", "cine_teal", "cine_bleach",
                "cine_16mm", "vintage_fade",
                "kodachrome", "portra400", "ecktachrome", "fujipro400h",
                "agfaapx", "ilfordhp5", "cinestill800t"
            )

            if (!isBuiltinPreset) {
                // 非 内置预设 ID：通过 LUTManager 或文件路径应用真实 .cube LUT
                result = applyLUT(result, settings)
            } else if (settings.lutPath.isNotEmpty()) {
                // 内置预设 ID 但同时有 lutPath：也需要应用文件 LUT
                result = applyLUT(result, settings.copy(activeLutName = ""))
            }
        }

        // 胶片预设 / 内置 LUT：优先使用 filmSimulation，若未设置则回退到 activeLutName
        val filmId = settings.filmSimulation.takeIf { it != "none" && it.isNotEmpty() }
            ?: settings.activeLutName.takeIf { it.isNotEmpty() }

        filmId?.let {
            result = when (it) {
                "vivid", "portrait", "landscape" -> applyFilmCurve(result, it)
                "fuji_astia", "fuji_provia", "fuji_velvia",
                "kodak_portra", "kodak_ektar", "kodak_tri_x",
                "ilford_delta", "agfa_vista" -> applyFilmEmulationLUT(result, it)
                // LUT 面板内置 ID 中 ilford_hp5 映射到 ilford_delta 实现
                "ilford_hp5" -> applyFilmEmulationLUT(result, "ilford_delta")
                "cine_2383", "cine_arri", "cine_teal", "cine_bleach",
                "cine_16mm", "vintage_fade" -> applyCreativeFilmLook(result, it)
                // 效果面板下拉菜单中的胶片风格名称映射到真实实现
                "kodachrome" -> applyFilmEmulationLUT(result, "kodak_ektar")
                "portra400" -> applyFilmEmulationLUT(result, "kodak_portra")
                "ecktachrome" -> applyFilmEmulationLUT(result, "fuji_provia")
                "fujipro400h" -> applyFilmEmulationLUT(result, "kodak_portra")
                "agfaapx" -> applyFilmEmulationLUT(result, "ilford_delta")
                "ilfordhp5" -> applyFilmEmulationLUT(result, "ilford_delta")
                "cinestill800t" -> applyCreativeFilmLook(result, "cine_2383")
                else -> result
            }
        }

        // 色彩科学转换
        result = when (settings.colorScience) {
            "aces", "aces2" -> applyACESTransform(result)
            "opendrt" -> applyOpenDRT(result)
            "srgb" -> applySRGBTransform(result)
            else -> result
        }

        return result
    }

    /**
     * 创意胶片风格实现（LUT 面板中无真实 .cube 文件的内置 LUT）
     */
    private fun applyCreativeFilmLook(bitmap: Bitmap, lookId: String): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            when (lookId) {
                "cine_2383" -> {
                    // 暖调 Kodak 2383 打印胶片：提升暖色、压暗阴影
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * 1.05f + 8f
                    g = gray + (g - gray) * 0.98f + 3f
                    b = gray + (b - gray) * 0.92f - 5f
                }
                "cine_arri" -> {
                    // Arri LogC 风格：略微降低对比、保护高光
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    r += (lum - r) * 0.1f + 5f
                    g += (lum - g) * 0.1f + 5f
                    b += (lum - b) * 0.1f + 5f
                }
                "cine_teal" -> {
                    // Teal & Orange：阴影偏青、高光偏橙
                    val lum = 0.299f * r + 0.587f * g + 0.114f * b
                    val shadowWeight = (1f - lum / 255f).coerceIn(0f, 1f)
                    r += 15f * (1f - shadowWeight)
                    g -= 10f * shadowWeight
                    b -= 15f * shadowWeight
                }
                "cine_bleach" -> {
                    // Bleach Bypass：高对比、低饱和、银盐感
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * 0.6f
                    g = gray + (g - gray) * 0.6f
                    b = gray + (b - gray) * 0.6f
                    r += 10f; g += 5f; b += 5f
                }
                "cine_16mm" -> {
                    // 16mm 胶片：粗颗粒感 + 轻微褪色
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * 1.1f + 5f
                    g = gray + (g - gray) * 1.05f + 3f
                    b = gray + (b - gray) * 1.0f
                }
                "vintage_fade" -> {
                    // 复古褪色：抬升黑场、降低饱和
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * 0.75f + 20f
                    g = gray + (g - gray) * 0.75f + 20f
                    b = gray + (b - gray) * 0.75f + 20f
                }
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyLUT(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val intensity = settings.lutIntensity / 100f
        if (intensity <= 0f) return bitmap

        // 1. 优先使用 LUTManager 缓存（activeLutName 或 lutPath 均可命中）
        val lutId = settings.activeLutName.takeIf { it.isNotEmpty() }
        if (lutId != null && lutManager != null) {
            val cachedData = lutManager.getCachedLUTData(lutId)
            if (cachedData != null) {
                return try {
                    applyLUT3DData(bitmap, cachedData, intensity)
                } catch (e: Exception) {
                    Log.w("SmartOptimizeEngine", "LUTManager cached LUT apply failed for $lutId", e)
                    bitmap
                }
            }
        }

        // 2. 如果 lutPath 指向本地 .cube 文件，尝试从 LUTManager 缓存或文件解析
        if (settings.lutPath.isNotEmpty()) {
            val lutFile = java.io.File(settings.lutPath)
            if (!lutFile.exists()) return bitmap

            // 尝试通过 LUTManager 解析并缓存
            val fileLutData = try {
                lutManager?.parseAndCache(lutFile.name, lutFile)
                    ?: com.silas.omaster.data.lut.LUT3DParser.parse(lutFile)
            } catch (e: Exception) {
                null
            }

            if (fileLutData != null) {
                return try {
                    applyLUT3DData(bitmap, fileLutData, intensity)
                } catch (e: Exception) {
                    bitmap
                }
            }

            // 回退到 LutProcessor 解析（兼容两种数据格式）
            return try {
                val lutData = lutProcessor.parseCube(lutFile.readText())
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                lutProcessor.applyLut(pixels, lutData, intensity)
                bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap
            }
        }

        return bitmap
    }

    /**
     * 使用 LUT3DData 的三线性插值将 LUT 应用到 Bitmap
     * 统一使用 LUT3DData.sampleTrilinear 实现高质量采样
     */
    private fun applyLUT3DData(bitmap: Bitmap, lutData: com.silas.omaster.data.lut.LUT3DData, strength: Float): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val s = strength.coerceIn(0f, 1f)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16 and 0xFF) / 255f
            val g = (pixel shr 8 and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            val mapped = lutData.sampleTrilinear(r, g, b)

            val outR = ((r * (1f - s) + mapped[0] * s).coerceIn(0f, 1f) * 255).toInt()
            val outG = ((g * (1f - s) + mapped[1] * s).coerceIn(0f, 1f) * 255).toInt()
            val outB = ((b * (1f - s) + mapped[2] * s).coerceIn(0f, 1f) * 255).toInt()
            val outA = pixel ushr 24 and 0xFF

            pixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyHighlightReconstruction(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // 简单高光重建：对过曝通道使用邻近通道信息恢复
        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel)
            var g = Color.green(pixel)
            var b = Color.blue(pixel)

            val maxChannel = maxOf(r, g, b)
            if (maxChannel > 250) {
                val avg = (r + g + b) / 3
                val recover = 0.7f
                if (r > 250) r = (r * (1f - recover) + avg * recover).toInt()
                if (g > 250) g = (g * (1f - recover) + avg * recover).toInt()
                if (b > 250) b = (b * (1f - recover) + avg * recover).toInt()
            }

            pixels[i] = Color.argb(Color.alpha(pixel), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyACESTransform(bitmap: Bitmap): Bitmap {
        // ACES 近似：S 曲线 + 轻微去饱和
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f

            // S 曲线
            val nr = (r * (2.51f * r + 0.03f) / (r * (2.43f * r + 0.59f) + 0.14f)).coerceIn(0f, 1f)
            val ng = (g * (2.51f * g + 0.03f) / (g * (2.43f * g + 0.59f) + 0.14f)).coerceIn(0f, 1f)
            val nb = (b * (2.51f * b + 0.03f) / (b * (2.43f * b + 0.59f) + 0.14f)).coerceIn(0f, 1f)

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                (nr * 255f).toInt(),
                (ng * 255f).toInt(),
                (nb * 255f).toInt()
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyOpenDRT(bitmap: Bitmap): Bitmap {
        // OpenDRT 近似：更平缓的 S 曲线 + 色彩补偿
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f

            // 更平缓的 S 曲线
            fun drtCurve(x: Float): Float {
                val y = x / (0.9f * x + 0.1f)
                return y.coerceIn(0f, 1f)
            }

            val nr = drtCurve(r)
            val ng = drtCurve(g)
            val nb = drtCurve(b)

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                (nr * 255f).toInt(),
                (ng * 255f).toInt(),
                (nb * 255f).toInt()
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applySRGBTransform(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f

            fun srgbCurve(x: Float): Float {
                return if (x <= 0.0031308f) x * 12.92f else 1.055f * x.pow(1f / 2.4f) - 0.055f
            }

            val nr = srgbCurve(r).coerceIn(0f, 1f)
            val ng = srgbCurve(g).coerceIn(0f, 1f)
            val nb = srgbCurve(b).coerceIn(0f, 1f)

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                (nr * 255f).toInt(),
                (ng * 255f).toInt(),
                (nb * 255f).toInt()
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyFilmCurve(bitmap: Bitmap, simulation: String): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            when (simulation) {
                "vivid" -> {
                    val sat = 1.15f
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * sat
                    g = gray + (g - gray) * sat
                    b = gray + (b - gray) * sat
                    r += 10f; g += 5f; b -= 5f
                }
                "portrait" -> {
                    val sat = 0.9f
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * sat
                    g = gray + (g - gray) * sat
                    b = gray + (b - gray) * sat
                    r += 8f; g += 5f; b += 12f
                }
                "landscape" -> {
                    val sat = 1.2f
                    val gray = 0.299f * r + 0.587f * g + 0.114f * b
                    r = gray + (r - gray) * sat
                    g = gray + (g - gray) * sat
                    b = gray + (b - gray) * sat
                    r -= 5f; g += 10f; b += 5f
                }
            }

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyFilmEmulationLUT(bitmap: Bitmap, filmType: String): Bitmap {
        // 使用内置的 3x3 色彩矩阵近似胶片效果
        val matrix = when (filmType) {
            "fuji_astia" -> floatArrayOf(
                1.05f, 0.05f, -0.05f, 0f,
                0.02f, 1.08f, -0.02f, 0f,
                0f, 0.02f, 1.05f, 0f,
                0f, 0f, 0f, 1f
            )
            "fuji_provia" -> floatArrayOf(
                1.1f, 0.02f, -0.05f, 0f,
                0f, 1.05f, -0.02f, 0f,
                -0.02f, 0.02f, 1.1f, 0f,
                0f, 0f, 0f, 1f
            )
            "fuji_velvia" -> floatArrayOf(
                1.2f, 0.05f, -0.1f, 0f,
                -0.05f, 1.15f, -0.05f, 0f,
                -0.1f, 0.05f, 1.2f, 0f,
                0f, 0f, 0f, 1f
            )
            "kodak_portra" -> floatArrayOf(
                1.05f, 0.08f, -0.02f, 0f,
                0.02f, 1.02f, 0.02f, 0f,
                0.02f, 0.05f, 1.05f, 0f,
                0f, 0f, 0f, 1f
            )
            "kodak_ektar" -> floatArrayOf(
                1.15f, 0.02f, -0.05f, 0f,
                -0.02f, 1.1f, -0.02f, 0f,
                -0.05f, 0.02f, 1.15f, 0f,
                0f, 0f, 0f, 1f
            )
            "kodak_tri_x" -> {
                // 黑白
                return applyGrayscale(bitmap, 0.299f, 0.587f, 0.114f, filmType)
            }
            "ilford_delta" -> {
                return applyGrayscale(bitmap, 0.25f, 0.6f, 0.15f, filmType)
            }
            "agfa_vista" -> floatArrayOf(
                1.08f, 0.05f, -0.05f, 0f,
                0.02f, 1.05f, 0f, 0f,
                -0.02f, 0.02f, 1.08f, 0f,
                0f, 0f, 0f, 1f
            )
            else -> return bitmap
        }

        val colorMatrix = ColorMatrix(matrix)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyGrayscale(bitmap: Bitmap, rw: Float, gw: Float, bw: Float, filmType: String): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            var gray = (r * rw + g * gw + b * bw).toInt()

            // Tri-X vs Delta 对比度差异
            when (filmType) {
                "kodak_tri_x" -> {
                    gray = ((gray - 128) * 1.3f + 128).toInt()
                }
                "ilford_delta" -> {
                    gray = ((gray - 128) * 1.1f + 128).toInt()
                }
            }

            gray = gray.coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixel), gray, gray, gray)
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyColorSpace(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        // 最终色彩空间输出转换
        return when (settings.exportColorSpace) {
            "sRGB" -> applySRGBTransform(bitmap)
            else -> bitmap
        }
    }

    // ========== 7. 特效 ==========

    private fun applyEffects(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        var result = bitmap

        // Glow (辉光)
        if (settings.glowAmount > 0f) {
            result = applyGlow(result, settings.glowAmount / 100f)
        }

        // Halation (光晕/红晕)
        if (settings.halationAmount > 0f) {
            result = applyHalation(result, settings.halationAmount / 100f)
        }

        // Light Flares (镜头光晕)
        if (settings.flareAmount > 0f) {
            result = applyFlare(result, settings.flareAmount / 100f)
        }

        // Vignette
        if (settings.vignette != 0f) {
            result = applyVignette(result, settings)
        }

        // Fade (褪色)
        if (settings.fade > 0f) {
            result = applyFade(result, settings.fade / 100f)
        }

        // Film grain
        if (settings.grain > 0f) {
            result = applyFilmGrain(result, settings)
        }

        return result
    }

    private fun applyGlow(bitmap: Bitmap, amount: Float): Bitmap {
        val blurred = applyGaussianBlur(bitmap, 15f)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply { alpha = (255 * amount * 0.6f).toInt() }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return result
    }

    private fun applyHalation(bitmap: Bitmap, amount: Float): Bitmap {
        // Halation：在暗部边缘添加红色/橙色光晕
        val blurred = applyGaussianBlur(bitmap, 8f)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            alpha = (255 * amount * 0.4f).toInt()
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1.3f, 0f, 0f, 0f, 0f,
                    0.3f, 0.8f, 0f, 0f, 0f,
                    0.1f, 0f, 0.7f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return result
    }

    private fun applyFlare(bitmap: Bitmap, amount: Float): Bitmap {
        // 镜头光晕：模拟过曝时的光线散射
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 在中心附近添加光晕
        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val maxR = maxOf(cx, cy)
        val flarePaint = Paint().apply {
            isAntiAlias = true
            alpha = (255 * amount * 0.25f).toInt()
            color = Color.argb(255, 255, 245, 220)
        }
        canvas.drawCircle(cx, cy, maxR * 0.8f, flarePaint)
        return result
    }

    private fun applyFade(bitmap: Bitmap, amount: Float): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            // Fade: lift blacks and reduce contrast
            val lift = amount * 40f
            r = (r * (1f - amount * 0.3f) + lift)
            g = (g * (1f - amount * 0.3f) + lift)
            b = (b * (1f - amount * 0.3f) + lift)

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyVignette(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val maxDist = sqrt(cx * cx + cy * cy)

        val amount = settings.vignette / 100f
        val midpoint = settings.vignetteMidpoint / 100f
        val roundness = (settings.vignetteRoundness + 100f) / 200f // -100~100 -> 0~1
        val feather = settings.vignetteFeather / 100f

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val dx = (x - cx) / cx
                val dy = (y - cy) / cy
                val dist = sqrt(dx * dx + dy * dy)

                // 圆度影响
                val adjustedDist = dist * (1f - roundness * 0.5f) + dist.pow(2f) * roundness * 0.5f

                // 中点 + 羽化
                val vignetteStart = midpoint * (1f - feather)
                val vignetteEnd = midpoint
                val factor = when {
                    adjustedDist < vignetteStart -> 1f
                    adjustedDist > vignetteEnd -> 1f - amount
                    else -> 1f - amount * ((adjustedDist - vignetteStart) / (vignetteEnd - vignetteStart))
                }

                val idx = y * bitmap.width + x
                val pixel = pixels[idx]
                val r = (Color.red(pixel) * factor).toInt().coerceIn(0, 255)
                val g = (Color.green(pixel) * factor).toInt().coerceIn(0, 255)
                val b = (Color.blue(pixel) * factor).toInt().coerceIn(0, 255)
                pixels[idx] = Color.argb(Color.alpha(pixel), r, g, b)
            }
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyFilmGrain(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val amount = settings.grain / 100f
        val size = settings.grainSize / 100f
        val roughness = settings.grainRoughness / 100f
        val seed = System.currentTimeMillis()
        val random = Random(seed)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel).toFloat()
            var g = Color.green(pixel).toFloat()
            var b = Color.blue(pixel).toFloat()

            // 粗糙度影响随机幅度
            val noise = (random.nextFloat() - 0.5f) * 255f * amount * (1f + roughness)

            // 颗粒大小影响：通过位置偏移采样实现（简化版）
            val sizeFactor = 1f + size * 2f
            val grainR = noise * sizeFactor * (0.8f + random.nextFloat() * 0.4f)
            val grainG = noise * sizeFactor * (0.9f + random.nextFloat() * 0.2f)
            val grainB = noise * sizeFactor * (1.0f + random.nextFloat() * 0.1f)

            r += grainR
            g += grainG
            b += grainB

            pixels[i] = Color.argb(
                Color.alpha(pixel),
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyGaussianBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val r = radius.coerceIn(0.1f, 25f)
        if (renderScript != null) {
            try {
                val input = Allocation.createFromBitmap(renderScript, bitmap)
                val output = Allocation.createTyped(renderScript, input.type)
                val blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                blur.setRadius(r)
                blur.setInput(input)
                blur.forEach(output)
                val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                output.copyTo(result)
                input.destroy()
                output.destroy()
                blur.destroy()
                return result
            } catch (_: Exception) {
                // RenderScript 失败时回退到 CPU
            }
        }
        return applyBoxBlur(bitmap, r)
    }

    /**
     * CPU 盒式模糊回退实现，保证在没有 RenderScript 时辉光/清晰度/降噪/结构等效果仍然真实生效
     */
    private fun applyBoxBlur(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0.5f) return bitmap
        val r = radius.toInt().coerceAtLeast(1)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val temp = IntArray(pixels.size)

        // 水平模糊
        for (y in 0 until h) {
            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            var count = 0
            for (x in -r..w + r) {
                val idx = y * w + x.coerceIn(0, w - 1)
                if (x < w + r) {
                    val p = pixels[idx]
                    sumR += Color.red(p)
                    sumG += Color.green(p)
                    sumB += Color.blue(p)
                    count++
                }
                if (x >= r) {
                    val outX = x - r
                    if (outX < w) {
                        temp[y * w + outX] = Color.argb(
                            Color.alpha(pixels[y * w + outX]),
                            (sumR / count).toInt().coerceIn(0, 255),
                            (sumG / count).toInt().coerceIn(0, 255),
                            (sumB / count).toInt().coerceIn(0, 255)
                        )
                    }
                    val remIdx = y * w + (outX - r).coerceIn(0, w - 1)
                    val rem = pixels[remIdx]
                    sumR -= Color.red(rem)
                    sumG -= Color.green(rem)
                    sumB -= Color.blue(rem)
                    count--
                }
            }
        }

        val result = IntArray(pixels.size)
        // 垂直模糊
        for (x in 0 until w) {
            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            var count = 0
            for (y in -r..h + r) {
                val idx = y.coerceIn(0, h - 1) * w + x
                if (y < h + r) {
                    val p = temp[idx]
                    sumR += Color.red(p)
                    sumG += Color.green(p)
                    sumB += Color.blue(p)
                    count++
                }
                if (y >= r) {
                    val outY = y - r
                    if (outY < h) {
                        result[outY * w + x] = Color.argb(
                            Color.alpha(temp[outY * w + x]),
                            (sumR / count).toInt().coerceIn(0, 255),
                            (sumG / count).toInt().coerceIn(0, 255),
                            (sumB / count).toInt().coerceIn(0, 255)
                        )
                    }
                    val remY = (outY - r).coerceIn(0, h - 1)
                    val rem = temp[remY * w + x]
                    sumR -= Color.red(rem)
                    sumG -= Color.green(rem)
                    sumB -= Color.blue(rem)
                    count--
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, bitmap.config ?: Bitmap.Config.ARGB_8888)
        out.setPixels(result, 0, w, 0, 0, w, h)
        return out
    }

    // ========== 8. 细节 ==========

    private fun applyDetails(bitmap: Bitmap, settings: SmartOptimizeParams, highQuality: Boolean): Bitmap {
        var result = bitmap

        // 锐化
        if (settings.sharpness != 0f) {
            result = applySharpening(result, settings.sharpness / 100f, settings.sharpnessThreshold / 100f)
        }

        // Clarity
        if (settings.clarity != 0f) {
            result = applyClarity(result, settings.clarity / 100f)
        }

        // Texture (RapidRAW: mid-frequency detail enhancement)
        if (settings.texture != 0f) {
            result = applyTexture(result, settings.texture / 100f)
        }

        // Dehaze
        if (settings.dehaze != 0f) {
            result = applyDehaze(result, settings.dehaze / 100f)
        }

        // Structure (RapidRAW)
        if (settings.structure != 0f) {
            result = applyStructure(result, settings.structure / 100f)
        }

        // Centré (RapidRAW: 中心提亮/压暗)
        if (settings.centre != 0f) {
            result = applyCentre(result, settings.centre / 100f)
        }

        // Noise reduction
        if (settings.luminanceNoiseReduction > 0f || settings.colorNoiseReduction > 0f) {
            result = applyNoiseReduction(result, settings.luminanceNoiseReduction / 100f, settings.colorNoiseReduction / 100f)
        }

        // Chromatic aberration
        if (settings.chromaticAberrationR != 0f || settings.chromaticAberrationB != 0f) {
            result = applyChromaticAberration(result, settings)
        }

        return result
    }

    private fun applySharpening(bitmap: Bitmap, amount: Float, threshold: Float): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val width = bitmap.width
        val height = bitmap.height

        val output = IntArray(pixels.size)
        System.arraycopy(pixels, 0, output, 0, pixels.size)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = pixels[idx]
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]

                val diff = abs(Color.red(center) - Color.red(top)) +
                           abs(Color.red(center) - Color.red(bottom)) +
                           abs(Color.red(center) - Color.red(left)) +
                           abs(Color.red(center) - Color.red(right))

                if (diff < threshold * 255f) continue

                val sharpenFactor = 1f + amount * 1.5f
                val r = (Color.red(center) * sharpenFactor - (Color.red(top) + Color.red(bottom) + Color.red(left) + Color.red(right)) * amount * 0.375f).toInt()
                val g = (Color.green(center) * sharpenFactor - (Color.green(top) + Color.green(bottom) + Color.green(left) + Color.green(right)) * amount * 0.375f).toInt()
                val b = (Color.blue(center) * sharpenFactor - (Color.blue(top) + Color.blue(bottom) + Color.blue(left) + Color.blue(right)) * amount * 0.375f).toInt()

                output[idx] = Color.argb(
                    Color.alpha(center),
                    r.coerceIn(0, 255),
                    g.coerceIn(0, 255),
                    b.coerceIn(0, 255)
                )
            }
        }

        bitmap.setPixels(output, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun applyClarity(bitmap: Bitmap, amount: Float): Bitmap {
        // Clarity = 中频锐化：原图 + (原图 - 模糊) * amount
        val blurred = applyGaussianBlur(bitmap, 8f)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            alpha = (255 * abs(amount) * 0.5f).toInt()
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1 + amount, 0f, 0f, 0f, 0f,
                    0f, 1 + amount, 0f, 0f, 0f,
                    0f, 0f, 1 + amount, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return result
    }

    private fun applyTexture(bitmap: Bitmap, amount: Float): Bitmap {
        // Texture: fine detail enhancement using local contrast
        val blurred = applyGaussianBlur(bitmap, 3f)
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            alpha = (255 * abs(amount) * 0.4f).toInt()
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1f + amount * 0.5f, 0f, 0f, 0f, 0f,
                    0f, 1f + amount * 0.5f, 0f, 0f, 0f,
                    0f, 0f, 1f + amount * 0.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        return result
    }

    private fun applyDehaze(bitmap: Bitmap, amount: Float): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // 估计大气光（取最亮像素的平均值）
        var airlightR = 0f
        var airlightG = 0f
        var airlightB = 0f
        var maxPixels = 0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            if (r > 240 && g > 240 && b > 240) {
                airlightR += r
                airlightG += g
                airlightB += b
                maxPixels++
            }
        }
        if (maxPixels > 0) {
            airlightR /= maxPixels
            airlightG /= maxPixels
            airlightB /= maxPixels
        } else {
            airlightR = 255f; airlightG = 255f; airlightB = 255f
        }

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toFloat()
            val g = Color.green(pixel).toFloat()
            val b = Color.blue(pixel).toFloat()

            // 透射率估计
            val darkChannel = minOf(r, g, b)
            val transmission = 1f - amount * (1f - darkChannel / 255f)
            val t = transmission.coerceIn(0.1f, 1f)

            val nr = ((r - airlightR) / t + airlightR).coerceIn(0f, 255f)
            val ng = ((g - airlightG) / t + airlightG).coerceIn(0f, 255f)
            val nb = ((b - airlightB) / t + airlightB).coerceIn(0f, 255f)

            pixels[i] = Color.argb(Color.alpha(pixel), nr.toInt(), ng.toInt(), nb.toInt())
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyStructure(bitmap: Bitmap, amount: Float): Bitmap {
        // Structure = 边缘增强 + 局部对比度
        val gray = applyGrayscale(bitmap.copy(Bitmap.Config.ARGB_8888, true), 0.299f, 0.587f, 0.114f, "")
        val blurred = applyGaussianBlur(gray, 5f)
        val edges = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(edges)
        val paint = Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(gray, 0f, 0f, null)
        canvas.drawBitmap(blurred, 0f, 0f, paint)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)
        resultCanvas.drawBitmap(bitmap, 0f, 0f, null)

        val overlayPaint = Paint().apply {
            alpha = (255 * abs(amount) * 0.6f).toInt()
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(floatArrayOf(
                    1f + amount, 0f, 0f, 0f, 0f,
                    0f, 1f + amount, 0f, 0f, 0f,
                    0f, 0f, 1f + amount, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            })
        }
        resultCanvas.drawBitmap(edges, 0f, 0f, overlayPaint)
        gray.recycle()
        blurred.recycle()
        edges.recycle()
        return result
    }

    private fun applyCentre(bitmap: Bitmap, amount: Float): Bitmap {
        // 中心调整：正数提亮中心，负数压暗中心
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val maxDist = sqrt(cx * cx + cy * cy)

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val dist = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                val factor = 1f - dist / maxDist // 中心 = 1，边缘 = 0
                val adjustment = amount * 128f * factor

                val idx = y * bitmap.width + x
                val pixel = pixels[idx]
                val r = (Color.red(pixel) + adjustment).toInt().coerceIn(0, 255)
                val g = (Color.green(pixel) + adjustment).toInt().coerceIn(0, 255)
                val b = (Color.blue(pixel) + adjustment).toInt().coerceIn(0, 255)
                pixels[idx] = Color.argb(Color.alpha(pixel), r, g, b)
            }
        }

        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun applyNoiseReduction(bitmap: Bitmap, lumaAmount: Float, colorAmount: Float): Bitmap {
        var result = bitmap

        // 亮度降噪
        if (lumaAmount > 0f) {
            result = applyGaussianBlur(result, 1f + lumaAmount * 3f)
        }

        // 色彩降噪：降低色度通道变化
        if (colorAmount > 0f) {
            val pixels = IntArray(result.width * result.height)
            result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = Color.red(pixel).toFloat()
                val g = Color.green(pixel).toFloat()
                val b = Color.blue(pixel).toFloat()

                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                val factor = colorAmount * 0.5f
                val nr = gray * factor + r * (1f - factor)
                val ng = gray * factor + g * (1f - factor)
                val nb = gray * factor + b * (1f - factor)
                pixels[i] = Color.argb(
                    Color.alpha(pixel),
                    nr.toInt().coerceIn(0, 255),
                    ng.toInt().coerceIn(0, 255),
                    nb.toInt().coerceIn(0, 255)
                )
            }
            result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        }
        return result
    }

    private fun applyChromaticAberration(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val redCyanShift = settings.chromaticAberrationR / 100f
        val blueYellowShift = settings.chromaticAberrationB / 100f
        if (redCyanShift == 0f && blueYellowShift == 0f) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val result = IntArray(w * h)

        val cx = w / 2f
        val cy = h / 2f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = (x - cx) / cx
                val dy = (y - cy) / cy
                val r2 = sqrt(dx * dx + dy * dy)

                // Red channel shift (red/cyan fringe)
                val rShiftX = if (redCyanShift != 0f) (dx * r2 * redCyanShift * 3f).toInt() else 0
                val rShiftY = if (redCyanShift != 0f) (dy * r2 * redCyanShift * 3f).toInt() else 0

                // Blue channel shift (blue/yellow fringe)
                val bShiftX = if (blueYellowShift != 0f) (dx * r2 * blueYellowShift * 3f).toInt() else 0
                val bShiftY = if (blueYellowShift != 0f) (dy * r2 * blueYellowShift * 3f).toInt() else 0

                val srcIdx = y * w + x

                // Sample R from shifted position
                val rSrcX = (x - rShiftX).coerceIn(0, w - 1)
                val rSrcY = (y - rShiftY).coerceIn(0, h - 1)
                val rPixel = pixels[rSrcY * w + rSrcX]

                // Sample B from shifted position
                val bSrcX = (x - bShiftX).coerceIn(0, w - 1)
                val bSrcY = (y - bShiftY).coerceIn(0, h - 1)
                val bPixel = pixels[bSrcY * w + bSrcX]

                val gPixel = pixels[srcIdx]

                result[srcIdx] = Color.argb(
                    Color.alpha(pixels[srcIdx]),
                    Color.red(rPixel),
                    Color.green(gPixel),
                    Color.blue(bPixel)
                )
            }
        }

        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        output.setPixels(result, 0, w, 0, 0, w, h)
        return output
    }

    private fun applyLensCorrection(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val warpAmount = (settings.geometryWarp + settings.distortion) / 100f
        val strength = settings.lensCorrectionStrength / 100f

        if (warpAmount == 0f && strength == 1f) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val result = IntArray(w * h)

        val cx = w / 2f
        val cy = h / 2f
        val maxR = sqrt(cx * cx + cy * cy)

        // Combined distortion: geometryWarp for barrel/pincushion, lensCorrectionStrength scales it
        val k = warpAmount * strength  // positive = barrel, negative = pincushion

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = (x - cx) / maxR
                val dy = (y - cy) / maxR
                val r2 = dx * dx + dy * dy

                // Radial distortion: r_corrected = r * (1 + k * r^2)
                val factor = 1f + k * r2
                val srcX = (dx * factor * maxR + cx).toInt().coerceIn(0, w - 1)
                val srcY = (dy * factor * maxR + cy).toInt().coerceIn(0, h - 1)

                result[y * w + x] = pixels[srcY * w + srcX]
            }
        }

        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        output.setPixels(result, 0, w, 0, 0, w, h)
        return output
    }

    private fun applyLocalMasks(bitmap: Bitmap, settings: SmartOptimizeParams): Bitmap {
        val enabledMasks = settings.masks.filter { it.enabled }
        if (enabledMasks.isEmpty()) return bitmap

        var result = bitmap

        for (mask in enabledMasks) {
            val maskAdjustments = mask.adjustments
            val w = result.width
            val h = result.height

            // Apply mask adjustments to a copy of the image
            var maskedCopy = result.copy(Bitmap.Config.ARGB_8888, true)
            maskedCopy = applyGeometryTransformations(maskedCopy, maskAdjustments)
            maskedCopy = applyBasicAdjustments(maskedCopy, maskAdjustments)
            maskedCopy = applyLightAdjustments(maskedCopy, maskAdjustments)
            maskedCopy = applyColorAdjustments(maskedCopy, maskAdjustments)
            maskedCopy = applyToneAdjustments(maskedCopy, maskAdjustments)
            maskedCopy = applyFilmSimulation(maskedCopy, maskAdjustments)
            maskedCopy = applyEffects(maskedCopy, maskAdjustments)
            maskedCopy = applyDetails(maskedCopy, maskAdjustments, true)
            maskedCopy = applyLensCorrection(maskedCopy, maskAdjustments)

            // Generate mask weights
            val maskWeights = FloatArray(w * h)
            val cx = w / 2f
            val cy = h / 2f
            val density = mask.density / 100f
            val feather = mask.feather / 100f

            when (mask.type) {
                "radial" -> {
                    val maxR = min(cx, cy)
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val dx = x - cx
                            val dy = y - cy
                            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            val norm = dist / maxR
                            // Sharp edge at feather, falloff beyond
                            val weight = if (norm <= (1f - feather)) {
                                1f
                            } else {
                                ((1f - norm) / feather).coerceIn(0f, 1f)
                            }
                            maskWeights[y * w + x] = if (mask.invert) 1f - weight else weight
                        }
                    }
                }
                "linear" -> {
                    for (y in 0 until h) {
                        for (x in 0 until w) {
                            val norm = x.toFloat() / w
                            val edge = 1f - feather
                            val weight = if (norm <= edge) {
                                1f
                            } else {
                                ((1f - norm) / feather).coerceIn(0f, 1f)
                            }
                            maskWeights[y * w + x] = if (mask.invert) 1f - weight else weight
                        }
                    }
                }
                else -> {
                    // brush / subject / sky: uniform density if no maskData
                    for (i in maskWeights.indices) {
                        maskWeights[i] = if (mask.invert) 0f else density
                    }
                }
            }

            // Scale by density
            for (i in maskWeights.indices) {
                maskWeights[i] *= density
            }

            // Blend
            val basePixels = IntArray(w * h)
            result.getPixels(basePixels, 0, w, 0, 0, w, h)
            val maskedPixels = IntArray(w * h)
            maskedCopy.getPixels(maskedPixels, 0, w, 0, 0, w, h)

            for (i in basePixels.indices) {
                val alpha = maskWeights[i]
                if (alpha <= 0f) continue
                val bp = basePixels[i]
                val mp = maskedPixels[i]
                val r = (Color.red(bp) * (1f - alpha) + Color.red(mp) * alpha).toInt().coerceIn(0, 255)
                val g = (Color.green(bp) * (1f - alpha) + Color.green(mp) * alpha).toInt().coerceIn(0, 255)
                val b = (Color.blue(bp) * (1f - alpha) + Color.blue(mp) * alpha).toInt().coerceIn(0, 255)
                basePixels[i] = Color.argb(Color.alpha(bp), r, g, b)
            }

            result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            result.setPixels(basePixels, 0, w, 0, 0, w, h)
            maskedCopy.recycle()
        }

        return result
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val l = (max + min) / 2f

        if (max == min) {
            return Triple(0f, 0f, l)
        }

        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

        val h = when (max) {
            rf -> ((gf - bf) / d + if (gf < bf) 6f else 0f) * 60f
            gf -> ((bf - rf) / d + 2f) * 60f
            else -> ((rf - gf) / d + 4f) * 60f
        }

        return Triple(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
        if (s == 0f) {
            val v = l * 255f
            return Triple(v, v, v)
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        val hk = h / 360f

        fun hueToChannel(t: Float): Float {
            var tt = t
            if (tt < 0f) tt += 1f
            if (tt > 1f) tt -= 1f
            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }

        val r = hueToChannel(hk + 1f / 3f) * 255f
        val g = hueToChannel(hk) * 255f
        val b = hueToChannel(hk - 1f / 3f) * 255f

        return Triple(r, g, b)
    }
}