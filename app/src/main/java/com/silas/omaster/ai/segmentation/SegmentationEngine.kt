package com.silas.omaster.ai.segmentation

import android.graphics.Bitmap
import com.silas.omaster.renderer.GPURenderManager
import com.silas.omaster.renderer.RenderParameters
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 启发式图像分割引擎
 *
 * 基于颜色、边缘、位置特征的实时分割，无需 ML 模型。
 * 支持天空分割和人像分割，生成像素级软蒙版。
 */
class SegmentationEngine {

    data class SegmentationMask(
        val width: Int,
        val height: Int,
        val mask: FloatArray,  // 0.0~1.0, width*height
        val type: MaskType
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SegmentationMask
            if (width != other.width) return false
            if (height != other.height) return false
            if (!mask.contentEquals(other.mask)) return false
            if (type != other.type) return false
            return true
        }

        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + mask.contentHashCode()
            result = 31 * result + type.hashCode()
            return result
        }
    }

    enum class MaskType {
        SKY,
        PERSON,
        SKIN,
        INVERTED
    }

    // ==================== 天空分割 ====================

    /**
     * 天空分割：基于颜色+位置+边缘的多特征融合
     *
     * 三个信号的加权融合：
     * - 颜色信号：HSL 蓝色范围 (hue 0.55~0.72, sat 0.15~0.8, light 0.3~0.9)
     * - 位置信号：越靠上天空概率越高
     * - 边缘信号：Sobel 边缘密度越低越可能是天空（天空通常平滑）
     */
    fun segmentSky(bitmap: Bitmap): SegmentationMask {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height

        // 对大图降采样，避免逐像素计算过慢
        val maxDim = 1024
        val scale = if (maxOf(width, height) > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else 1f
        val sw = (width * scale).toInt().coerceAtLeast(1)
        val sh = (height * scale).toInt().coerceAtLeast(1)

        val workBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        } else {
            bitmap
        }

        val workW = workBitmap.width
        val workH = workBitmap.height
        val workSize = workW * workH

        // 1. 提取像素
        val pixels = IntArray(workSize)
        workBitmap.getPixels(pixels, 0, workW, 0, 0, workW, workH)

        // 2. 计算颜色概率、位置概率、边缘图
        val colorProb = FloatArray(workSize)
        val edgeMag = FloatArray(workSize)

        for (i in 0 until workSize) {
            val pixel = pixels[i]
            val r = ((pixel ushr 16) and 0xFF) / 255f
            val g = ((pixel ushr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            colorProb[i] = skyColorProbability(r, g, b)
        }

        // Sobel 边缘检测
        computeSobelEdge(pixels, workW, workH, edgeMag)

        // 3. 融合信号
        val rawMask = FloatArray(workSize)
        val colorWeight = 0.50f
        val posWeight = 0.25f
        val edgeWeight = 0.25f
        val edgeThreshold = 0.15f

        for (y in 0 until workH) {
            val posProb = 1f - (y.toFloat() / workH.toFloat())
            // 使用平方使位置衰减更快：顶部行高概率，中间行迅速降低
            val posSignal = posProb * posProb

            for (x in 0 until workW) {
                val i = y * workW + x
                val edgeSignal = 1f - (edgeMag[i] / edgeThreshold).coerceIn(0f, 1f)
                rawMask[i] = colorProb[i] * colorWeight +
                        posSignal * posWeight +
                        edgeSignal * edgeWeight
            }
        }

        // 4. 对原始蒙版做高斯模糊，实现软边缘
        val blurredMask = gaussianBlurMask(rawMask, workW, workH, blurRadius = 3)

        // 5. 归一化到 [0, 1]
        var maxVal = 0f
        for (v in blurredMask) { if (v > maxVal) maxVal = v }
        val mask = FloatArray(workSize)
        if (maxVal > 0.001f) {
            for (i in 0 until workSize) {
                mask[i] = (blurredMask[i] / maxVal).coerceIn(0f, 1f)
            }
        }

        // 6. 如有降采样，上采样回原始尺寸
        val finalMask = if (scale < 1f) {
            upsampleMask(mask, workW, workH, width, height)
        } else {
            mask
        }

        // 清理临时 bitmap
        if (workBitmap !== bitmap) {
            workBitmap.recycle()
        }

        return SegmentationMask(width, height, finalMask, MaskType.SKY)
    }

    // ==================== 人像/肤色分割 ====================

    /**
     * 人像/肤色分割：基于YCbCr肤色检测+连通域分析
     *
     * 步骤：
     * 1. YCbCr 肤色检测 (Cb: 77~127, Cr: 133~173)
     * 2. 连通域分析：过滤太小的噪声区域和太大的背景区域
     * 3. 边缘精炼
     * 4. 高斯模糊产生软蒙版
     */
    fun segmentPerson(bitmap: Bitmap): SegmentationMask {
        val width = bitmap.width
        val height = bitmap.height
        val size = width * height

        // 降采样
        val maxDim = 768
        val scale = if (maxOf(width, height) > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else 1f
        val sw = (width * scale).toInt().coerceAtLeast(1)
        val sh = (height * scale).toInt().coerceAtLeast(1)

        val workBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        } else {
            bitmap
        }

        val workW = workBitmap.width
        val workH = workBitmap.height
        val workSize = workW * workH

        val pixels = IntArray(workSize)
        workBitmap.getPixels(pixels, 0, workW, 0, 0, workW, workH)

        // 1. YCbCr 肤色检测 → 二值图
        val skinBinary = BooleanArray(workSize)
        for (i in 0 until workSize) {
            val pixel = pixels[i]
            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF
            skinBinary[i] = isSkinColor(r, g, b)
        }

        // 2. 连通域分析（4-连通 flood fill）
        val componentMap = IntArray(workSize) { -1 }
        val componentSizes = mutableListOf<Int>()
        var componentId = 0

        for (i in 0 until workSize) {
            if (!skinBinary[i] || componentMap[i] >= 0) continue
            // BFS flood fill
            val queue = java.util.ArrayDeque<Int>()
            queue.add(i)
            componentMap[i] = componentId
            var compSize = 0

            while (queue.isNotEmpty()) {
                val idx = queue.poll()
                compSize++
                val cx = idx % workW
                val cy = idx / workW
                // 4-邻域
                val neighbors = intArrayOf(
                    if (cy > 0) idx - workW else -1,
                    if (cy < workH - 1) idx + workW else -1,
                    if (cx > 0) idx - 1 else -1,
                    if (cx < workW - 1) idx + 1 else -1
                )
                for (ni in neighbors) {
                    if (ni >= 0 && skinBinary[ni] && componentMap[ni] < 0) {
                        componentMap[ni] = componentId
                        queue.add(ni)
                    }
                }
            }
            componentSizes.add(compSize)
            componentId++
        }

        // 3. 过滤连通域：太小=噪声，太大=背景
        val totalPixels = workSize.toFloat()
        val minComponentRatio = 0.005f  // 最小 0.5% 图像面积
        val maxComponentRatio = 0.60f   // 最大 60% 图像面积
        val minComponentSize = (totalPixels * minComponentRatio).toInt().coerceAtLeast(50)
        val maxComponentSize = (totalPixels * maxComponentRatio).toInt()

        val validComponents = mutableSetOf<Int>()
        for (cid in componentSizes.indices) {
            val sz = componentSizes[cid]
            if (sz in minComponentSize..maxComponentSize) {
                validComponents.add(cid)
            }
        }

        // 4. 生成软蒙版
        val rawMask = FloatArray(workSize)
        for (i in 0 until workSize) {
            val cid = componentMap[i]
            rawMask[i] = if (cid >= 0 && cid in validComponents) 1f else 0f
        }

        // 5. 边缘精炼：在肤色边界处，根据局部边缘强度做渐变
        val edgeMag = FloatArray(workSize)
        computeSobelEdge(pixels, workW, workH, edgeMag)

        // 对硬蒙版做膨胀-腐蚀形态学操作（先膨胀后腐蚀 = 闭合操作），填充小孔洞
        val closedMask = morphClose(rawMask, workW, workH, radius = 2)

        // 在边缘处根据梯度强度做渐变过渡
        val refinedMask = FloatArray(workSize)
        for (y in 0 until workH) {
            for (x in 0 until workW) {
                val i = y * workW + x
                val maskVal = closedMask[i]
                val edge = edgeMag[i]
                if (maskVal > 0.5f && edge > 0.08f) {
                    // 在边缘处，根据边缘强度降低蒙版值，产生柔和过渡
                    refinedMask[i] = (maskVal - edge * 0.8f).coerceIn(0f, 1f)
                } else {
                    refinedMask[i] = maskVal
                }
            }
        }

        // 6. 高斯模糊平滑蒙版
        val blurredMask = gaussianBlurMask(refinedMask, workW, workH, blurRadius = 4)

        // 7. 上采样回原始尺寸
        val finalMask = if (scale < 1f) {
            upsampleMask(blurredMask, workW, workH, width, height)
        } else {
            blurredMask
        }

        if (workBitmap !== bitmap) {
            workBitmap.recycle()
        }

        return SegmentationMask(width, height, finalMask, MaskType.PERSON)
    }

    // ==================== 蒙版操作 ====================

    /**
     * 反转蒙版
     */
    fun invertMask(mask: SegmentationMask): SegmentationMask {
        val inverted = FloatArray(mask.mask.size) { i ->
            1f - mask.mask[i]
        }
        return SegmentationMask(
            mask.width,
            mask.height,
            inverted,
            MaskType.INVERTED
        )
    }

    /**
     * 羽化蒙版（高斯模糊边缘过渡）
     *
     * @param mask 输入蒙版
     * @param radius 羽化半径（像素），越大边缘过渡越柔和
     */
    fun featherMask(mask: SegmentationMask, radius: Int): SegmentationMask {
        val blurred = gaussianBlurMask(mask.mask, mask.width, mask.height, radius)
        return SegmentationMask(
            mask.width,
            mask.height,
            blurred,
            mask.type
        )
    }

    /**
     * 将蒙版应用到渲染参数：蒙版区域应用masked参数，非蒙版区域保持原参数
     *
     * 实现方式（CPU 逐像素路径）：
     * 1. 先用 globalParams 渲染全图
     * 2. 再用 maskedParams 渲染全图
     * 3. 用蒙版值作为混合因子，在两个渲染结果之间插值
     *
     * @param source 原始图像
     * @param mask 分割蒙版
     * @param globalParams 全局渲染参数（应用于非蒙版区域）
     * @param maskedParams 蒙版区域渲染参数
     * @param gpuRenderManager GPU 渲染管理器（可选，当前使用 CPU 路径）
     * @return 混合后的 Bitmap
     */
    fun applyMaskedEdit(
        source: Bitmap,
        mask: SegmentationMask,
        globalParams: RenderParameters,
        maskedParams: RenderParameters,
        gpuRenderManager: GPURenderManager? = null
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 读取原始像素
        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val output = IntArray(width * height)
        val maskData = mask.mask

        // 归一化参数（与 CPURenderer.processPixels 一致）
        val gExposure = globalParams.exposure / 100f
        val gBrightness = globalParams.brightness / 100f
        val gContrast = globalParams.contrast / 100f
        val gSaturation = globalParams.saturation / 100f
        val gVibrance = globalParams.vibrance / 100f
        val gWarmth = globalParams.warmth / 100f
        val gHighlights = globalParams.highlights / 100f
        val gShadows = globalParams.shadows / 100f
        val gWhites = globalParams.whites / 100f
        val gBlacks = globalParams.blacks / 100f
        val gClarity = globalParams.clarity / 100f
        val gDehaze = globalParams.dehaze / 100f
        val gFade = globalParams.fade / 100f
        val gGrain = globalParams.grain / 100f

        val mExposure = maskedParams.exposure / 100f
        val mBrightness = maskedParams.brightness / 100f
        val mContrast = maskedParams.contrast / 100f
        val mSaturation = maskedParams.saturation / 100f
        val mVibrance = maskedParams.vibrance / 100f
        val mWarmth = maskedParams.warmth / 100f
        val mHighlights = maskedParams.highlights / 100f
        val mShadows = maskedParams.shadows / 100f
        val mWhites = maskedParams.whites / 100f
        val mBlacks = maskedParams.blacks / 100f
        val mClarity = maskedParams.clarity / 100f
        val mDehaze = maskedParams.dehaze / 100f
        val mFade = maskedParams.fade / 100f
        val mGrain = maskedParams.grain / 100f

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]
            val a = (pixel ushr 24) and 0xFF
            val or = ((pixel ushr 16) and 0xFF) / 255f
            val og = ((pixel ushr 8) and 0xFF) / 255f
            val ob = (pixel and 0xFF) / 255f

            // 全局参数渲染结果
            val gr = applyPixelAdjustments(or, og, ob,
                gExposure, gBrightness, gContrast, gSaturation, gVibrance,
                gWarmth, gHighlights, gShadows, gWhites, gBlacks,
                gClarity, gDehaze, gFade, gGrain, i)

            // 蒙版区域参数渲染结果
            val mr = applyPixelAdjustments(or, og, ob,
                mExposure, mBrightness, mContrast, mSaturation, mVibrance,
                mWarmth, mHighlights, mShadows, mWhites, mBlacks,
                mClarity, mDehaze, mFade, mGrain, i)

            // 蒙版混合因子（确保蒙版尺寸与像素数组匹配）
            val m = if (i < maskData.size) maskData[i].coerceIn(0f, 1f) else 0f

            // 插值混合
            val fr = (gr[0] * (1f - m) + mr[0] * m).coerceIn(0f, 1f)
            val fg = (gr[1] * (1f - m) + mr[1] * m).coerceIn(0f, 1f)
            val fb = (gr[2] * (1f - m) + mr[2] * m).coerceIn(0f, 1f)

            output[i] = (a shl 24) or
                    ((fr * 255f).toInt() shl 16) or
                    ((fg * 255f).toInt() shl 8) or
                    (fb * 255f).toInt()
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    // ==================== 私有辅助函数 ====================

    /**
     * 天空颜色概率：HSL 空间分析
     * 蓝色色相范围 0.55~0.72，饱和度 0.15~0.8，明度 0.3~0.9
     */
    private fun skyColorProbability(r: Float, g: Float, b: Float): Float {
        val hsl = rgb2hsl(r, g, b)
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        // 色相：蓝色范围（0.55~0.72）
        val hueScore = if (h in 0.55f..0.72f) {
            1f - abs(h - 0.635f) / 0.085f  // 中心 0.635 处最高
        } else if (h in 0.50f..0.55f) {
            // 青蓝色过渡区
            (h - 0.50f) / 0.05f * 0.5f
        } else if (h in 0.72f..0.78f) {
            // 蓝紫色过渡区
            (0.78f - h) / 0.06f * 0.3f
        } else {
            0f
        }

        // 饱和度：0.15~0.8 之间天空概率高
        val satScore = if (s in 0.15f..0.8f) {
            1f
        } else if (s < 0.15f) {
            // 低饱和度可能是阴天/白色天空
            s / 0.15f * 0.4f
        } else {
            // 过于饱和可能不是天空
            (1f - s).coerceIn(0f, 1f) * 0.3f
        }

        // 明度：0.3~0.9 之间天空概率高
        val lightScore = if (l in 0.3f..0.9f) {
            1f
        } else if (l > 0.9f) {
            // 过曝区域仍可能是天空（白云）
            0.5f
        } else if (l > 0.2f) {
            (l - 0.2f) / 0.1f * 0.5f
        } else {
            0f
        }

        // 加权融合
        return (hueScore * 0.5f + satScore * 0.25f + lightScore * 0.25f).coerceIn(0f, 1f)
    }

    /**
     * YCbCr 肤色检测 (BT.601)
     * Cb: 77~127, Cr: 133~173（8 位量程）
     */
    private fun isSkinColor(r: Int, g: Int, b: Int): Boolean {
        // RGB → YCbCr (BT.601)
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        val cb = 128f - 0.169f * r - 0.331f * g + 0.500f * b
        val cr = 128f + 0.500f * r - 0.419f * g - 0.081f * b
        return cb in 77f..127f && cr in 133f..173f && y in 60f..255f
    }

    /**
     * Sobel 边缘检测
     */
    private fun computeSobelEdge(pixels: IntArray, width: Int, height: Int, outMag: FloatArray) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 获取 3x3 邻域亮度
                val tl = luminance(pixels, width, height, x - 1, y - 1)
                val tc = luminance(pixels, width, height, x, y - 1)
                val tr = luminance(pixels, width, height, x + 1, y - 1)
                val ml = luminance(pixels, width, height, x - 1, y)
                val mr = luminance(pixels, width, height, x + 1, y)
                val bl = luminance(pixels, width, height, x - 1, y + 1)
                val bc = luminance(pixels, width, height, x, y + 1)
                val br = luminance(pixels, width, height, x + 1, y + 1)

                // Sobel 水平/垂直梯度
                val gx = -tl + tr - 2f * ml + 2f * mr - bl + br
                val gy = -tl - 2f * tc - tr + bl + 2f * bc + br
                outMag[y * width + x] = sqrt(gx * gx + gy * gy)
            }
        }
    }

    /**
     * 安全获取像素亮度
     */
    private fun luminance(pixels: IntArray, width: Int, height: Int, x: Int, y: Int): Float {
        val cx = x.coerceIn(0, width - 1)
        val cy = y.coerceIn(0, height - 1)
        val pixel = pixels[cy * width + cx]
        val r = ((pixel ushr 16) and 0xFF) / 255f
        val g = ((pixel ushr 8) and 0xFF) / 255f
        val b = (pixel and 0xFF) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /**
     * 高斯模糊蒙版
     *
     * 使用可分离的两趟 1D 高斯卷积，复杂度 O(n) 而非 O(n*r²)
     */
    private fun gaussianBlurMask(mask: FloatArray, width: Int, height: Int, blurRadius: Int): FloatArray {
        if (blurRadius <= 0) return mask.copyOf()

        // 生成 1D 高斯核
        val kernelSize = blurRadius * 2 + 1
        val kernel = FloatArray(kernelSize)
        val sigma = blurRadius / 2f
        var sum = 0f
        for (i in 0 until kernelSize) {
            val x = i - blurRadius
            kernel[i] = exp(-(x * x) / (2f * sigma * sigma))
            sum += kernel[i]
        }
        // 归一化
        for (i in 0 until kernelSize) {
            kernel[i] /= sum
        }

        // 水平趟
        val temp = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var val_ = 0f
                for (k in 0 until kernelSize) {
                    val nx = (x + k - blurRadius).coerceIn(0, width - 1)
                    val_ += mask[y * width + nx] * kernel[k]
                }
                temp[y * width + x] = val_
            }
        }

        // 垂直趟
        val result = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var val_ = 0f
                for (k in 0 until kernelSize) {
                    val ny = (y + k - blurRadius).coerceIn(0, height - 1)
                    val_ += temp[ny * width + x] * kernel[k]
                }
                result[y * width + x] = val_
            }
        }

        return result
    }

    /**
     * 上采样蒙版（双线性插值）
     */
    private fun upsampleMask(mask: FloatArray, srcW: Int, srcH: Int, dstW: Int, dstH: Int): FloatArray {
        val result = FloatArray(dstW * dstH)
        val xRatio = srcW.toFloat() / dstW.toFloat()
        val yRatio = srcH.toFloat() / dstH.toFloat()

        for (dy in 0 until dstH) {
            val sy = dy * yRatio
            val sy0 = sy.toInt().coerceIn(0, srcH - 2)
            val sy1 = (sy0 + 1).coerceIn(0, srcH - 1)
            val fy = sy - sy0

            for (dx in 0 until dstW) {
                val sx = dx * xRatio
                val sx0 = sx.toInt().coerceIn(0, srcW - 2)
                val sx1 = (sx0 + 1).coerceIn(0, srcW - 1)
                val fx = sx - sx0

                val v00 = mask[sy0 * srcW + sx0]
                val v10 = mask[sy0 * srcW + sx1]
                val v01 = mask[sy1 * srcW + sx0]
                val v11 = mask[sy1 * srcW + sx1]

                val top = v00 * (1f - fx) + v10 * fx
                val bottom = v01 * (1f - fx) + v11 * fx
                result[dy * dstW + dx] = (top * (1f - fy) + bottom * fy).coerceIn(0f, 1f)
            }
        }
        return result
    }

    /**
     * 形态学闭合操作（先膨胀后腐蚀），填充小孔洞
     */
    private fun morphClose(mask: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        // 膨胀：取邻域最大值
        val dilated = FloatArray(mask.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var maxVal = 0f
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val v = mask[ny * width + nx]
                        if (v > maxVal) maxVal = v
                    }
                }
                dilated[y * width + x] = maxVal
            }
        }

        // 腐蚀：取邻域最小值
        val result = FloatArray(mask.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var minVal = 1f
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val v = dilated[ny * width + nx]
                        if (v < minVal) minVal = v
                    }
                }
                result[y * width + x] = minVal
            }
        }

        return result
    }

    /**
     * 逐像素渲染调整（与 CPURenderer.processPixels 逻辑一致）
     * 返回 [r, g, b] 均在 [0, 1]
     */
    private fun applyPixelAdjustments(
        r: Float, g: Float, b: Float,
        exposure: Float, brightness: Float, contrast: Float,
        saturation: Float, vibrance: Float, warmth: Float,
        highlights: Float, shadows: Float, whites: Float, blacks: Float,
        clarity: Float, dehaze: Float, fade: Float, grain: Float,
        pixelIndex: Int
    ): FloatArray {
        var rr = r
        var gg = g
        var bb = b

        // 曝光
        if (abs(exposure) > 0.01f) {
            val factor = 2.0.pow(exposure.toDouble()).toFloat()
            rr *= factor; gg *= factor; bb *= factor
        }

        // 亮度
        if (abs(brightness) > 0.01f) {
            val offset = brightness * 0.5f
            rr += offset; gg += offset; bb += offset
        }

        // 对比度
        if (abs(contrast) > 0.01f) {
            val factor = 1f + contrast
            rr = 0.5f + (rr - 0.5f) * factor
            gg = 0.5f + (gg - 0.5f) * factor
            bb = 0.5f + (bb - 0.5f) * factor
        }

        // 饱和度
        if (abs(saturation) > 0.01f) {
            val hsl = rgb2hsl(rr, gg, bb)
            hsl[1] = (hsl[1] + saturation).coerceIn(0f, 1f)
            val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
            rr = rgb[0]; gg = rgb[1]; bb = rgb[2]
        }

        // 鲜艳度
        if (abs(vibrance) > 0.01f) {
            val hsl = rgb2hsl(rr, gg, bb)
            val vibranceAmount = (1f - hsl[1]) * vibrance
            hsl[1] = (hsl[1] + vibranceAmount * 0.5f).coerceIn(0f, 1f)
            val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
            rr = rgb[0]; gg = rgb[1]; bb = rgb[2]
        }

        // 色温
        if (abs(warmth) > 0.01f) {
            rr += warmth * 0.1f
            bb -= warmth * 0.1f
        }

        // 高光
        if (abs(highlights) > 0.01f) {
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val mask = smoothstep(0.5f, 1.0f, lum)
            val adjR = rr * (1f + highlights * mask)
            val adjG = gg * (1f + highlights * mask)
            val adjB = bb * (1f + highlights * mask)
            rr = mix(rr, adjR, mask)
            gg = mix(gg, adjG, mask)
            bb = mix(bb, adjB, mask)
        }

        // 阴影
        if (abs(shadows) > 0.01f) {
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val mask = smoothstep(0.5f, 0.0f, lum)
            val adjR = rr + shadows * mask * 0.3f
            val adjG = gg + shadows * mask * 0.3f
            val adjB = bb + shadows * mask * 0.3f
            rr = mix(rr, adjR, mask)
            gg = mix(gg, adjG, mask)
            bb = mix(bb, adjB, mask)
        }

        // 白色色阶
        if (abs(whites) > 0.01f) {
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val mask = smoothstep(0.7f, 1.0f, lum)
            val adjR = 1f - (1f - rr) * (1f - whites * mask)
            val adjG = 1f - (1f - gg) * (1f - whites * mask)
            val adjB = 1f - (1f - bb) * (1f - whites * mask)
            rr = mix(rr, adjR, mask)
            gg = mix(gg, adjG, mask)
            bb = mix(bb, adjB, mask)
        }

        // 黑色色阶
        if (abs(blacks) > 0.01f) {
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val mask = smoothstep(0.3f, 0.0f, lum)
            val adjR = rr * (1f + blacks * mask)
            val adjG = gg * (1f + blacks * mask)
            val adjB = bb * (1f + blacks * mask)
            rr = mix(rr, adjR, mask)
            gg = mix(gg, adjG, mask)
            bb = mix(bb, adjB, mask)
        }

        // 清晰度
        if (clarity > 0.01f) {
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val adaptiveStrength = clarity * (1f - abs(lum - 0.5f) * 0.5f)
            val newR = 0.5f + (rr - 0.5f) * (1f + adaptiveStrength * 2f)
            val newG = 0.5f + (gg - 0.5f) * (1f + adaptiveStrength * 2f)
            val newB = 0.5f + (bb - 0.5f) * (1f + adaptiveStrength * 2f)
            rr = mix(rr, newR, clarity)
            gg = mix(gg, newG, clarity)
            bb = mix(bb, newB, clarity)
        }

        // 去霾
        if (dehaze > 0.01f) {
            val hsl = rgb2hsl(rr, gg, bb)
            val fogLevel = hsl[2] * (1f - hsl[1])
            val ds = dehaze * fogLevel
            rr = 0.5f + (rr - 0.5f) * (1f + ds)
            gg = 0.5f + (gg - 0.5f) * (1f + ds)
            bb = 0.5f + (bb - 0.5f) * (1f + ds)
            val hsl2 = rgb2hsl(rr, gg, bb)
            hsl2[1] = (hsl2[1] + ds * 0.5f).coerceIn(0f, 1f)
            val rgb = hsl2rgb(hsl2[0], hsl2[1], hsl2[2])
            rr = rgb[0]; gg = rgb[1]; bb = rgb[2]
        }

        // 褪色
        if (fade > 0.01f) {
            rr = 0.5f + (rr - 0.5f) * (1f - fade * 0.3f)
            gg = 0.5f + (gg - 0.5f) * (1f - fade * 0.3f)
            bb = 0.5f + (bb - 0.5f) * (1f - fade * 0.3f)
            rr = mix(rr, rr + 0.1f * fade, fade)
            gg = mix(gg, gg + 0.1f * fade, fade)
            bb = mix(bb, bb + 0.1f * fade, fade)
            val hsl = rgb2hsl(rr, gg, bb)
            hsl[1] = hsl[1] * (1f - fade * 0.2f)
            val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
            rr = rgb[0]; gg = rgb[1]; bb = rgb[2]
        }

        // 颗粒
        if (grain > 0.01f) {
            val noiseRaw = abs((sin(pixelIndex * 12.9898 + 78.233) * 43758.5453) % 1.0)
            val noise = noiseRaw.toFloat() * 2f - 1f
            val lum = 0.299f * rr + 0.587f * gg + 0.114f * bb
            val gs = grain * (1f + (1f - lum) * 0.5f)
            rr += noise * gs * 0.15f
            gg += noise * gs * 0.15f
            bb += noise * gs * 0.15f
        }

        return floatArrayOf(rr.coerceIn(0f, 1f), gg.coerceIn(0f, 1f), bb.coerceIn(0f, 1f))
    }

    // ==================== 颜色空间转换 ====================

    /** RGB→HSL，返回 [h, s, l] 均在 [0, 1] */
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

    /** HSL→RGB，返回 [r, g, b] 均在 [0, 1] */
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
}
