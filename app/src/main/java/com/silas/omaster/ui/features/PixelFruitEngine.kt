package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
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
 *
 * @param context 用于初始化 ML Kit 人脸检测器；传 null 时面部美白退化为肤色检测
 */
class PixelFruitEngine(context: Context? = null) {

    companion object {
        // 人脸检测最大边长，控制 ML Kit 推理耗时
        private const val FACE_DETECT_MAX_DIMENSION = 640
    }

    // 生产级 ML Kit 人脸检测器（高精度轮廓模式，用于生成面部美白蒙版）
    private val faceDetector: FaceDetector? by lazy {
        context?.let {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
            FaceDetection.getClient(options)
        }
    }

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

        // Step 4: 面部美白（优先使用 ML Kit Face Mesh 蒙版，无脸时回退到肤色检测）
        if (params.faceBrightening > 0) {
            checkActive { onProgress("面部美白", 0.7f) }
            val faceMask = detectFaceMask(bitmap)
            applyFaceBrightening(pixels, width, height, params.faceBrightening, params.faceSmoothness, faceMask)
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

            // 5. 色温/色调偏移
            if (params.redTint != 0f || params.blueTint != 0f) {
                r += tempR + tempB * 0.5f
                b += tempB + tempR * 0.25f
            }
            if (params.greenTint != 0f) {
                g += toneG
                r += toneR
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
     * 降噪：双边滤波（Bilateral Filter）
     *
     * 在平滑噪声的同时保留边缘，效果优于 3×3 边缘感知均值滤波。
     * 权重 = 空间高斯 × 颜色差异高斯；为控制耗时使用固定 5×5 窗口。
     *
     * @param spatialSigma 空间距离标准差（像素），越大越平滑
     * @param rangeSigma   颜色差异标准差（0..255），越小越保护边缘
     */
    private fun applyNoiseReduction(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Float,
        detailPreservation: Float,
        spatialSigma: Float = 2.0f,
        rangeSigma: Float = 30f
    ) {
        if (strength <= 0f) return
        val temp = pixels.copyOf()
        val effectiveStrength = (strength / 100f).coerceIn(0f, 1f)
        // 细节保留系数：0..1，越大越保留边缘（rangeSigma 越大）
        val preservation = (detailPreservation / 100f).coerceIn(0.1f, 1f)
        val adjustedRangeSigma = rangeSigma * (1.5f - preservation * 0.5f)

        val radius = 2
        // 预计算空间高斯权重
        val spatialWeights = FloatArray((2 * radius + 1) * (2 * radius + 1))
        val twoSpatialSigma2 = 2f * spatialSigma * spatialSigma
        val twoRangeSigma2 = 2f * adjustedRangeSigma * adjustedRangeSigma
        var wi = 0
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                spatialWeights[wi++] = exp(-(dx * dx + dy * dy) / twoSpatialSigma2)
            }
        }

        for (y in radius until height - radius) {
            for (x in radius until width - radius) {
                val i = y * width + x
                val curR = Color.red(temp[i])
                val curG = Color.green(temp[i])
                val curB = Color.blue(temp[i])

                var weightSumR = 0f
                var weightSumG = 0f
                var weightSumB = 0f
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                wi = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val ni = (y + dy) * width + (x + dx)
                        val nR = Color.red(temp[ni])
                        val nG = Color.green(temp[ni])
                        val nB = Color.blue(temp[ni])

                        val dr = (curR - nR).toFloat()
                        val dg = (curG - nG).toFloat()
                        val db = (curB - nB).toFloat()
                        val colorDistSq = dr * dr + dg * dg + db * db
                        val rangeWeight = exp(-colorDistSq / twoRangeSigma2)
                        val weight = spatialWeights[wi++] * rangeWeight

                        weightSumR += weight
                        weightSumG += weight
                        weightSumB += weight
                        sumR += nR * weight
                        sumG += nG * weight
                        sumB += nB * weight
                    }
                }

                val filteredR = if (weightSumR > 0.001f) sumR / weightSumR else curR.toFloat()
                val filteredG = if (weightSumG > 0.001f) sumG / weightSumG else curG.toFloat()
                val filteredB = if (weightSumB > 0.001f) sumB / weightSumB else curB.toFloat()

                val blend = effectiveStrength
                val r = (curR * (1f - blend) + filteredR * blend).toInt().coerceIn(0, 255)
                val g = (curG * (1f - blend) + filteredG * blend).toInt().coerceIn(0, 255)
                val b = (curB * (1f - blend) + filteredB * blend).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(Color.alpha(temp[i]), r, g, b)
            }
        }
    }

    // ==================== ML Kit 人脸蒙版 ====================

    /**
     * 使用 ML Kit 人脸检测生成面部蒙版。
     * 为控制耗时，对超过 640px 的图像先缩放到 640px 进行检测，再映射回原图。
     *
     * @return 若检测到人脸返回 0..1 蒙版数组；否则返回 null，调用方回退到肤色检测
     */
    private suspend fun detectFaceMask(source: Bitmap): FloatArray? {
        val detector = faceDetector ?: return null
        return try {
            val maxDim = maxOf(source.width, source.height)
            val detectBitmap = if (maxDim <= FACE_DETECT_MAX_DIMENSION) {
                source
            } else {
                val scale = FACE_DETECT_MAX_DIMENSION.toFloat() / maxDim
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt(),
                    (source.height * scale).toInt(),
                    true
                )
            }
            val inputImage = InputImage.fromBitmap(detectBitmap, 0)
            val faces = detector.process(inputImage).await()
            if (faces.isEmpty()) return null

            val srcWidth = source.width
            val srcHeight = source.height
            val scaleX = srcWidth.toFloat() / detectBitmap.width
            val scaleY = srcHeight.toFloat() / detectBitmap.height
            val mask = FloatArray(srcWidth * srcHeight)

            for (face in faces) {
                val bounds = face.boundingBox
                // 映射到原图坐标
                val left = (bounds.left * scaleX).toInt().coerceIn(0, srcWidth - 1)
                val top = (bounds.top * scaleY).toInt().coerceIn(0, srcHeight - 1)
                val right = (bounds.right * scaleX).toInt().coerceIn(0, srcWidth)
                val bottom = (bounds.bottom * scaleY).toInt().coerceIn(0, srcHeight)

                // 在面部矩形内部使用轮廓点生成更紧致的椭圆蒙版
                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val radiusX = (right - left) / 2f
                val radiusY = (bottom - top) / 2f
                val radiusXInv = if (radiusX > 0) 1f / radiusX else 0f
                val radiusYInv = if (radiusY > 0) 1f / radiusY else 0f

                for (y in top until bottom) {
                    for (x in left until right) {
                        val dx = (x - centerX) * radiusXInv
                        val dy = (y - centerY) * radiusYInv
                        val distSq = dx * dx + dy * dy
                        // 椭圆内部为 1，边缘按距离衰减
                        val value = if (distSq <= 1f) 1f - distSq.coerceIn(0f, 1f) * 0.3f else 0f
                        if (value > 0f) {
                            val idx = y * srcWidth + x
                            mask[idx] = maxOf(mask[idx], value)
                        }
                    }
                }
            }

            if (detectBitmap !== source) detectBitmap.recycle()
            mask
        } catch (e: Exception) {
            null
        }
    }

    // ==================== Details.js: applyFaceBrightening ====================

    /**
     * 面部美白
     * 1. 若提供 ML Kit 人脸蒙版则优先使用；否则四重肤色检测生成蒙版
     * 2. 蒙版平滑（均值滤波）
     * 3. 应用美白：亮度提升 + 减红 + 加蓝 + 降饱和
     */
    private fun applyFaceBrightening(
        pixels: IntArray,
        width: Int,
        height: Int,
        brightening: Float,
        smoothness: Float,
        faceMask: FloatArray? = null
    ) {
        if (brightening <= 0f) return
        val strength = brightening / 100f
        val mask = faceMask ?: FloatArray(pixels.size).also { m ->
            // Step 1: 肤色检测（四重检测加权评分）
            for (i in pixels.indices) {
                val r = Color.red(pixels[i])
                val g = Color.green(pixels[i])
                val b = Color.blue(pixels[i])
                m[i] = detectSkinTone(r, g, b)
            }
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
}
