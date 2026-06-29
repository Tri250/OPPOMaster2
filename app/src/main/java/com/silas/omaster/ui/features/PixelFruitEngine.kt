package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * PixelFruit 图像处理引擎
 *
 * 将 PixelFruit (gitee.com/ji_annn/PixelFruit) 的核心算法
 * 完整移植为 Android Kotlin 原生实现
 *
 * 处理管线顺序（对齐 applyAdjustmentsToCachedData）:
 * 1. 颜色调整 (color.js applyColorAdjustments)
 * 2. 降噪     (Details.js applyNoiseReduction)
 * 3. 锐化     (Details.js applyUnsharpMask)
 * 4. 面部美白 (Details.js applyFaceBrightening)
 * 5. LUT 应用 (LutProcessor.applyLut)
 *
 * 所有算法在 Dispatchers.Default 后台线程执行，
 * 支持协程取消和进度回调。
 */
class PixelFruitEngine {

    /**
     * 主处理入口
     *
     * @param bitmap  输入图像
     * @param params  PixelFruit 14 参数
     * @param lut     可选 LUT（3D 查找表）
     * @param onProgress 进度回调 (stepName, progress)
     * @return 处理后的新 Bitmap（不修改输入）
     */
    suspend fun process(
        bitmap: Bitmap,
        params: PixelFruitParams,
        lut: LutProcessor.Lut3D? = null,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Step 1: 颜色调整（对齐 color.js）
        if (params.changedParamCount() > 0) {
            checkActive { onProgress("颜色调整", 0.1f) }
            applyColorAdjustments(pixels, width, height, params)
        }

        // Step 2: 降噪（对齐 Details.js，边缘感知均值滤波）
        if (params.noiseReduction > 0) {
            checkActive { onProgress("降噪", 0.3f) }
            applyNoiseReduction(pixels, width, height, params.noiseReduction, detailPreservation = 50f)
        }

        // Step 3: 锐化（对齐 Details.js，USM 非锐化蒙版）
        if (params.sharpness > 0) {
            checkActive { onProgress("锐化", 0.5f) }
            applyUnsharpMask(pixels, width, height, params.sharpness)
        }

        // Step 4: 面部美白（对齐 Details.js，四重肤色检测）
        if (params.faceBrightening > 0) {
            checkActive { onProgress("面部美白", 0.7f) }
            applyFaceBrightening(pixels, width, height, params.faceBrightening, params.faceSmoothness)
        }

        // Step 5: 清晰度（局部对比度增强）
        if (params.clarity != 0f) {
            checkActive { onProgress("清晰度", 0.72f) }
            applyClarity(pixels, width, height, params.clarity)
        }

        // Step 6: 自然饱和度（低饱和像素优先增强）
        if (params.vibrance != 100f) {
            checkActive { onProgress("自然饱和度", 0.74f) }
            applyVibrance(pixels, width, height, params.vibrance)
        }

        // Step 7: 黑色阶（黑场提升）
        if (params.blacks != 0f) {
            checkActive { onProgress("黑色阶", 0.76f) }
            applyBlacks(pixels, width, height, params.blacks)
        }

        // Step 8: 晕影效果
        if (params.vignette != 0f) {
            checkActive { onProgress("晕影", 0.78f) }
            applyVignette(pixels, width, height, params.vignette)
        }

        // Step 9: 胶片颗粒
        if (params.grain > 0f) {
            checkActive { onProgress("颗粒感", 0.80f) }
            applyGrain(pixels, width, height, params.grain)
        }

        // Step 5: LUT 应用（对齐 LutProcessor.js，三线性插值）
        if (lut != null && lut.size > 0) {
            checkActive { onProgress("LUT", 0.85f) }
            LutProcessor().applyLut(pixels, lut, 1.0f)
        }

        // 写回 Bitmap
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        checkActive { onProgress("完成", 1.0f) }
        result
    }

    /** 快速预览处理：仅调色，跳过降噪/锐化/美白（用于实时预览） */
    suspend fun processPreview(
        bitmap: Bitmap,
        params: PixelFruitParams
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        applyColorAdjustments(pixels, width, height, params)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }

    // ==================== color.js: applyColorAdjustments ====================

    /**
     * 10 步调色管道（逐像素，对齐 WebGLRenderer.js uniform 逻辑）
     */
    private fun applyColorAdjustments(
        pixels: IntArray,
        width: Int,
        height: Int,
        params: PixelFruitParams
    ) {
        // 预计算不变因子
        val contrastFactor = if (params.contrast != 0f) {
            (259f * (params.contrast + 255f)) / (255f * (259f - params.contrast))
        } else 1f

        val exposureFactor = if (params.exposure != 0f) {
            2.0.pow(params.exposure.toDouble()).toFloat()
        } else 1f

        val saturationFactor = params.saturation / 100f
        val redMul = 1f + params.redTint / 100f
        val greenMul = 1f + params.greenTint / 100f
        val blueMul = 1f + params.blueTint / 100f
        val whiteFactor = params.whites / 100f

        // 色调偏移：红色调影响 R/G，蓝色调影响 R/B（对齐 color.js 色温/色调逻辑）
        val tempR = params.redTint * 0.5f
        val tempB = -params.blueTint * 0.5f
        val toneG = params.greenTint * 0.3f
        val toneR = -params.greenTint * 0.15f

        val shadowBoost = params.shadows / 100f
        val highlightFactor = params.highlights / 100f

        for (i in pixels.indices) {
            var r = Color.red(pixels[i]).toFloat()
            var g = Color.green(pixels[i]).toFloat()
            var b = Color.blue(pixels[i]).toFloat()
            val a = Color.alpha(pixels[i])

            // 1. 白平衡系数
            r *= redMul; g *= greenMul; b *= blueMul

            // 2. 亮度
            if (params.brightness != 1.0f) {
                r *= params.brightness
                g *= params.brightness
                b *= params.brightness
            }

            // 3. 对比度（以128为中心缩放）
            if (params.contrast != 0f) {
                r = contrastFactor * (r - 128f) + 128f
                g = contrastFactor * (g - 128f) + 128f
                b = contrastFactor * (b - 128f) + 128f
            }

            // 4. 饱和度（灰度混合）
            if (params.saturation != 100f) {
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + saturationFactor * (r - gray)
                g = gray + saturationFactor * (g - gray)
                b = gray + saturationFactor * (b - gray)
            }

            // 5. 色温/色调偏移（合并原RGB色调与新色温/色调参数）
            if (params.redTint != 0f || params.blueTint != 0f) {
                r += tempR + tempB * 0.5f
                b += tempB + tempR * 0.25f
            }
            if (params.greenTint != 0f) {
                g += toneG
                r += toneR
            }
            // 新增色温参数：暖→冷，正值暖（加红减蓝），负值冷（减红加蓝）
            if (params.temperature != 0f) {
                val tempShift = params.temperature / 100f
                r += tempShift * 15f
                b -= tempShift * 15f
                g += tempShift * 3f
            }
            // 新增色调参数：绿→品，正值偏品（加红蓝），负值偏绿
            if (params.tint != 0f) {
                val tintShift = params.tint / 100f
                r += tintShift * 5f
                b += tintShift * 5f
                g -= tintShift * 10f
            }

            // 6. 曝光（2的幂）
            if (params.exposure != 0f) {
                r *= exposureFactor
                g *= exposureFactor
                b *= exposureFactor
            }

            // 7. 阴影（阈值64以下提亮）
            if (params.shadows != 0f) {
                if (r < 64f) r += (64f - r) * shadowBoost
                if (g < 64f) g += (64f - g) * shadowBoost
                if (b < 64f) b += (64f - b) * shadowBoost
            }

            // 8. 高光（阈值192以上压暗/提亮）
            if (params.highlights != 0f) {
                if (r > 192f) r += (r - 192f) * highlightFactor
                if (g > 192f) g += (g - 192f) * highlightFactor
                if (b > 192f) b += (b - 192f) * highlightFactor
            }

            // 9. 白场（阈值220以上缩放）
            if (params.whites != 100f) {
                val avg = (r + g + b) / 3f
                if (avg > 220f) {
                    r *= whiteFactor
                    g *= whiteFactor
                    b *= whiteFactor
                }
            }

            pixels[i] = Color.argb(
                a,
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        }
    }

    // ==================== Details.js: applyUnsharpMask ====================

    /**
     * 非锐化蒙版（USM）
     * 新像素 = 当前像素 + (当前像素 - 邻域均值) × strength
     */
    private fun applyUnsharpMask(
        pixels: IntArray,
        width: Int,
        height: Int,
        sharpness: Float
    ) {
        if (sharpness <= 0f) return
        val strength = sharpness / 100f
        val temp = pixels.copyOf()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                var sumR = 0; var sumG = 0; var sumB = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val ni = (y + dy) * width + (x + dx)
                        sumR += Color.red(temp[ni])
                        sumG += Color.green(temp[ni])
                        sumB += Color.blue(temp[ni])
                    }
                }
                val avgR = sumR / 8f
                val avgG = sumG / 8f
                val avgB = sumB / 8f
                val r = (Color.red(temp[i]) + (Color.red(temp[i]) - avgR) * strength)
                    .toInt().coerceIn(0, 255)
                val g = (Color.green(temp[i]) + (Color.green(temp[i]) - avgG) * strength)
                    .toInt().coerceIn(0, 255)
                val b = (Color.blue(temp[i]) + (Color.blue(temp[i]) - avgB) * strength)
                    .toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(temp[i]), r, g, b)
            }
        }
    }

    // ==================== Details.js: applyNoiseReduction ====================

    /**
     * 降噪：均值滤波 + 边缘感知细节保留
     *
     * edgeFactor = |当前 - 均值| / 255
     * detailFactor = min(1, edgeFactor × (detailPreservation / 50))
     * effectiveStrength = (strength/100) × (1 - detailFactor)
     */
    private fun applyNoiseReduction(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Float,
        detailPreservation: Float
    ) {
        if (strength <= 0f) return
        val temp = pixels.copyOf()
        val effectiveStrength = strength / 100f
        val detailFactor = detailPreservation / 50f

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val curR = Color.red(temp[i])
                val curG = Color.green(temp[i])
                val curB = Color.blue(temp[i])

                var sumR = 0; var sumG = 0; var sumB = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val ni = (y + dy) * width + (x + dx)
                        sumR += Color.red(temp[ni])
                        sumG += Color.green(temp[ni])
                        sumB += Color.blue(temp[ni])
                    }
                }
                val meanR = sumR / 9f
                val meanG = sumG / 9f
                val meanB = sumB / 9f

                val edgeR = abs(curR - meanR) / 255f
                val edgeG = abs(curG - meanG) / 255f
                val edgeB = abs(curB - meanB) / 255f
                val maxEdge = maxOf(edgeR, edgeG, edgeB)
                val preserve = min(1f, maxEdge * detailFactor)
                val blend = effectiveStrength * (1f - preserve)

                val r = (curR * (1f - blend) + meanR * blend).toInt().coerceIn(0, 255)
                val g = (curG * (1f - blend) + meanG * blend).toInt().coerceIn(0, 255)
                val b = (curB * (1f - blend) + meanB * blend).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(temp[i]), r, g, b)
            }
        }
    }

    // ==================== Details.js: applyFaceBrightening ====================

    /**
     * 面部美白
     * 1. 四重肤色检测 → 生成蒙版
     * 2. 蒙版平滑（均值滤波）
     * 3. 应用美白：亮度提升 + 减红 + 加蓝 + 降饱和
     */
    private fun applyFaceBrightening(
        pixels: IntArray,
        width: Int,
        height: Int,
        brightening: Float,
        smoothness: Float
    ) {
        if (brightening <= 0f) return
        val strength = brightening / 100f
        val mask = FloatArray(pixels.size)

        // Step 1: 肤色检测（四重检测加权评分）
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            mask[i] = detectSkinTone(r, g, b)
        }

        // Step 2: 蒙版平滑
        if (smoothness > 0) {
            val radius = (smoothness / 20f).toInt().coerceAtLeast(1).coerceAtMost(5)
            smoothMask(mask, width, height, radius)
        }

        // Step 3: 应用美白
        for (i in pixels.indices) {
            val m = mask[i]
            if (m <= 0.01f) continue
            val r = Color.red(pixels[i]).toFloat()
            val g = Color.green(pixels[i]).toFloat()
            val b = Color.blue(pixels[i]).toFloat()
            val a = Color.alpha(pixels[i])

            val brightness = 1f + 0.3f * strength * m
            var nr = r * brightness - 5f * strength * m
            var ng = g * brightness
            var nb = b * brightness + 3f * strength * m

            // 降饱和
            val gray = 0.299f * nr + 0.587f * ng + 0.114f * nb
            val desat = 1f - 0.2f * strength * m
            nr = gray + desat * (nr - gray)
            ng = gray + desat * (ng - gray)
            nb = gray + desat * (nb - gray)

            pixels[i] = Color.argb(
                a,
                nr.toInt().coerceIn(0, 255),
                ng.toInt().coerceIn(0, 255),
                nb.toInt().coerceIn(0, 255)
            )
        }
    }

    /**
     * 四重肤色检测（对齐 PixelFruit Details.js）
     * 加权评分：RGB范围 0.3 + 归一化RGB 0.25 + 简单肤色 0.25 + 宽松检测 0.2
     */
    private fun detectSkinTone(r: Int, g: Int, b: Int): Float {
        // 1. RGB范围检测
        val cond1 = if (r > g && g > b && r > 80 && g > 30 && b > 15) 0.3f else 0f
        // 2. 归一化RGB检测
        val sum = r + g + b + 1f
        val nr = r / sum
        val ng = g / sum
        val nb = b / sum
        val cond2 = if (nr in 0.20f..0.50f && ng in 0.25f..0.45f && nb in 0.20f..0.45f) 0.25f else 0f
        // 3. 简单肤色范围
        val cond3 = if (r > g && g > b && r - b > 20 && r > 100) 0.25f else 0f
        // 4. 宽松检测
        val cond4 = if (r > 60 && g in 30..180 && b in 15..120 && r > g && g >= b) 0.2f else 0f
        return min(1f, cond1 + cond2 + cond3 + cond4)
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

    private inline fun checkActive(block: () -> Unit) {
        if (!kotlinx.coroutines.coroutineContext.isActive) return
        block()
    }

    // ==================== 清晰度: 局部对比度增强 ====================

    /**
     * 清晰度 = 局部对比度增强 (Clarity)
     * 对中间调区域应用局部对比度提升，使图像更有质感
     */
    private fun applyClarity(pixels: IntArray, width: Int, height: Int, clarity: Float) {
        if (clarity == 0f) return
        val strength = clarity / 100f
        val temp = pixels.copyOf()

        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                val i = y * width + x
                val r = Color.red(temp[i]).toFloat()
                val g = Color.green(temp[i]).toFloat()
                val b = Color.blue(temp[i]).toFloat()
                val lum = 0.299f * r + 0.587f * g + 0.114f * b

                // 5x5 邻域均值
                var sumR = 0f; var sumG = 0f; var sumB = 0f
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val ni = (y + dy) * width + (x + dx)
                        sumR += Color.red(temp[ni])
                        sumG += Color.green(temp[ni])
                        sumB += Color.blue(temp[ni])
                    }
                }
                val meanR = sumR / 25f
                val meanG = sumG / 25f
                val meanB = sumB / 25f
                val meanLum = 0.299f * meanR + 0.587f * meanG + 0.114f * meanB

                // 中间调权重：亮度在64-192之间权重最高
                val midtoneWeight = when {
                    lum < 64f -> lum / 64f
                    lum > 192f -> (255f - lum) / 63f
                    else -> 1f
                }

                val factor = strength * midtoneWeight
                val nr = (r + (r - meanR) * factor).toInt().coerceIn(0, 255)
                val ng = (g + (g - meanG) * factor).toInt().coerceIn(0, 255)
                val nb = (b + (b - meanB) * factor).toInt().coerceIn(0, 255)

                pixels[i] = Color.argb(Color.alpha(temp[i]), nr, ng, nb)
            }
        }
    }

    // ==================== 自然饱和度 ====================

    /**
     * 自然饱和度 (Vibrance)
     * 低饱和像素获得更大增益，高饱和像素保持不变
     */
    private fun applyVibrance(pixels: IntArray, width: Int, height: Int, vibrance: Float) {
        val factor = vibrance / 100f
        for (i in pixels.indices) {
            val r = Color.red(pixels[i]).toFloat()
            val g = Color.green(pixels[i]).toFloat()
            val b = Color.blue(pixels[i]).toFloat()
            val a = Color.alpha(pixels[i])

            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            val currentSat = if (maxC > 0f) (maxC - minC) / maxC else 0f

            // 低饱和像素获得更大增益
            val satBoost = (1f - currentSat) * (factor - 1f)
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            val boost = 1f + satBoost
            val nr = (gray + boost * (r - gray)).toInt().coerceIn(0, 255)
            val ng = (gray + boost * (g - gray)).toInt().coerceIn(0, 255)
            val nb = (gray + boost * (b - gray)).toInt().coerceIn(0, 255)

            pixels[i] = Color.argb(a, nr, ng, nb)
        }
    }

    // ==================== 黑色阶 ====================

    /**
     * 黑色阶 (Blacks)
     * 控制黑场水平，正值提亮暗部，负值加深暗部
     */
    private fun applyBlacks(pixels: IntArray, width: Int, height: Int, blacks: Float) {
        val strength = blacks / 100f
        for (i in pixels.indices) {
            val r = Color.red(pixels[i]).toFloat()
            val g = Color.green(pixels[i]).toFloat()
            val b = Color.blue(pixels[i]).toFloat()
            val a = Color.alpha(pixels[i])

            // 仅影响暗部区域（阈值50以下）
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            if (lum < 50f) {
                val weight = (50f - lum) / 50f
                val shift = strength * 30f * weight
                val nr = (r + shift).toInt().coerceIn(0, 255)
                val ng = (g + shift).toInt().coerceIn(0, 255)
                val nb = (b + shift).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(a, nr, ng, nb)
            }
        }
    }

    // ==================== 晕影效果 ====================

    /**
     * 晕影 (Vignette)
     * 负值=暗角（边缘变暗），正值=亮角（边缘变亮）
     */
    private fun applyVignette(pixels: IntArray, width: Int, height: Int, vignette: Float) {
        val strength = vignette / 100f
        val cx = width / 2f
        val cy = height / 2f
        val maxDist = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val dx = x - cx
                val dy = y - cy
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val normalizedDist = (dist / maxDist).coerceIn(0f, 1f)
                // 边缘影响更大
                val edgeFactor = normalizedDist * normalizedDist
                val adjust = 1f + strength * edgeFactor
                val r = (Color.red(pixels[i]) * adjust).toInt().coerceIn(0, 255)
                val g = (Color.green(pixels[i]) * adjust).toInt().coerceIn(0, 255)
                val b = (Color.blue(pixels[i]) * adjust).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
            }
        }
    }

    // ==================== 胶片颗粒 ====================

    /**
     * 胶片颗粒 (Grain)
     * 添加随机噪点模拟胶片颗粒感，颗粒强度与亮度相关
     */
    private fun applyGrain(pixels: IntArray, width: Int, height: Int, grain: Float) {
        val strength = grain / 100f * 30f // 最大±30的偏移
        val random = java.util.Random()
        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            // 颗粒在中间调最明显
            val grainWeight = 1f - Math.abs(lum - 0.5f) * 1.5f
            val noise = (random.nextGaussian() * strength * grainWeight.coerceIn(0.2f, 1f)).toInt()

            val nr = (r + noise).coerceIn(0, 255)
            val ng = (g + noise).coerceIn(0, 255)
            val nb = (b + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(pixels[i]), nr, ng, nb)
        }
    }
}
