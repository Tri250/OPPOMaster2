package com.silas.omaster.ai.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Layer 2: 大师推理层 - 核心启发式分析器
 * 
 * 混合推理策略（放弃随机，拥抱真实分析）
 * 
 * 优先级 1: 颜色直方图分析（即时，无模型依赖）
 * 优先级 2: EXIF 元数据分析（即时，无模型依赖）
 * 优先级 3: 亮度与纹理分析
 * 优先级 4: 用户上下文推断
 * 未来: TFLite 模型（待模型文件就绪后替换启发式管道）
 */
class HeuristicSceneAnalyzer(private val context: Context) {

    /**
     * 分析结果
     */
    data class AnalysisResult(
        val primaryScene: SceneProfile,
        val confidence: Float,
        val alternativeScenes: List<SceneProfile>,  // Top-3 备选
        val colorProfile: ColorProfile,
        val brightnessLevel: BrightnessLevel,
        val faceCount: Int,
        val edgeDensity: Float,
        val exifData: ExifData? = null,
        val analysisDetails: Map<String, Float> = emptyMap()
    )

    /**
     * 颜色画像 — 增强版：包含饱和度、色彩方差、亮度分布、对比度等多维特征
     */
    data class ColorProfile(
        val avgRed: Int,
        val avgGreen: Int,
        val avgBlue: Int,
        val warmthRatio: Float,        // 暖色调占比 0-1
        val greenDominance: Float,     // 绿色通道主导度
        val blueDominance: Float,      // 蓝色通道主导度
        val redDominance: Float,       // 红色通道主导度
        val skinToneRatio: Float,      // 肤色占比（YCbCr检测）
        val darkPixelRatio: Float,     // 暗部像素占比（luminance < 50）
        val highlightRatio: Float,     // 高光像素占比（luminance > 200）
        val skyBlueRatio: Float = 0f,  // 天空区域蓝色主导度
        val groundWarmthRatio: Float = 0f,  // 地面区域暖色调占比
        // === 新增多维特征（提升场景识别准确率） ===
        val saturationMean: Float,     // 平均饱和度 0-1
        val saturationVariance: Float, // 饱和度方差（高 → 色彩丰富）
        val colorVariance: Float,      // RGB 通道方差均值（高 → 色彩多样，低 → 色调统一）
        val brightnessStdDev: Float,   // 亮度标准差（高 → 明暗对比强，低 → 均匀）
        val contrastRatio: Float,      // 对比度比：高光占比 / (暗部占比 + 0.01)
        val grayDominance: Float       // 中性灰占比（RGB 三通道接近，低饱和度）
    )

    /**
     * 亮度等级
     */
    enum class BrightnessLevel(val displayName: String, val range: String) {
        VERY_DARK("极暗", "0-50"),
        DARK("暗调", "50-100"),
        NORMAL("正常", "100-150"),
        BRIGHT("亮调", "150-200"),
        VERY_BRIGHT("高亮", "200-255")
    }

    /**
     * 场景候选
     */
    data class SceneCandidate(
        val sceneId: String,
        val score: Float,
        val source: String  // 来源：color/brightness/face/exif/texture
    )

    /**
     * 分析图片并返回场景识别结果
     */
    suspend fun analyze(
        bitmap: Bitmap,
        exif: ExifData? = null,
        userContext: UserContext? = null
    ): AnalysisResult = withContext(Dispatchers.Default) {
        // 1. 颜色分析（采样策略：取中心 60% 区域，避免边缘干扰）
        val colorProfile = sampleColorProfile(bitmap, sampleRatio = 0.6f)

        // 2. 亮度分析
        val brightnessLevel = computeBrightnessLevel(bitmap)

        // 3. 人脸检测（使用 ML Kit Face Detection，传入已计算的 colorProfile 用于回退推断）
        val faceCount = detectFaces(bitmap, colorProfile)

        // 4. 纹理分析（边缘密度）
        val edgeDensity = computeEdgeDensity(bitmap)

        // 5. 多特征投票 — 增强版：加入饱和度、对比度、色彩方差维度
        val candidates = mutableListOf<SceneCandidate>()

        // 颜色投票（基于多维颜色特征）
        candidates.addAll(voteByColor(colorProfile))
        // 饱和度投票（新增：高饱和度→美食/风景，低饱和度→城市/夜景）
        candidates.addAll(voteBySaturation(colorProfile))
        // 对比度投票（新增：高对比度→城市建筑，低对比度→室内/柔光人像）
        candidates.addAll(voteByContrast(colorProfile))
        // 亮度投票
        candidates.addAll(voteByBrightness(brightnessLevel, colorProfile))
        // 人脸投票
        if (faceCount > 0) candidates.addAll(voteByFace(faceCount, colorProfile))
        // EXIF 投票
        if (exif != null && hasEnoughExif(exif)) {
            candidates.addAll(voteByExif(exif, brightnessLevel))
        } else {
            candidates.addAll(voteByExifFallback(brightnessLevel, colorProfile))
        }
        // 纹理投票
        candidates.addAll(voteByTexture(edgeDensity, colorProfile))

        // 6. 加权融合
        val fused = fuseVotes(candidates, userContext)

        // 7. 构建分析详情
        val analysisDetails = buildAnalysisDetails(
            colorProfile, brightnessLevel, faceCount, edgeDensity, exif
        )

        AnalysisResult(
            primaryScene = fused.primary,
            confidence = fused.confidence,
            alternativeScenes = fused.alternatives,
            colorProfile = colorProfile,
            brightnessLevel = brightnessLevel,
            faceCount = faceCount,
            edgeDensity = edgeDensity,
            exifData = exif,
            analysisDetails = analysisDetails
        )
    }

    /**
     * 颜色直方图采样
     * 采样策略：上中下分3个采样区（各占30%高度），按"天空优先"和"地面优先"做加权
     * 改善：不再只采中心60%，而是覆盖天空、主体、地面三个区域
     */
    private fun sampleColorProfile(bitmap: Bitmap, sampleRatio: Float): ColorProfile {
        val width = bitmap.width
        val height = bitmap.height

        // 采样步长：根据图片大小动态调整
        val step = when {
            width > 1000 -> 8
            width > 500 -> 4
            else -> 2
        }

        // 上中下三个采样区（各占30%高度，中间有10%重叠）
        val regionHeight = (height * 0.3).toInt()
        val topRegion = 0 to regionHeight                    // 天空区域
        val midRegion = (height * 0.35).toInt() to (height * 0.65).toInt()  // 主体区域
        val botRegion = (height - regionHeight) to height    // 地面区域

        // 分别采样三个区域
        val topProfile = sampleRegion(bitmap, 0, width, topRegion.first, topRegion.second, step, weight = 1.0f)
        val midProfile = sampleRegion(bitmap, 0, width, midRegion.first, midRegion.second, step, weight = 1.5f)
        val botProfile = sampleRegion(bitmap, 0, width, botRegion.first, botRegion.second, step, weight = 1.0f)

        // 加权合并三个区域的采样结果
        val totalWeight = topProfile.pixelCount * 1.0f + midProfile.pixelCount * 1.5f + botProfile.pixelCount * 1.0f

        val avgR = ((topProfile.totalR * 1.0 + midProfile.totalR * 1.5 + botProfile.totalR * 1.0) / totalWeight).toInt()
        val avgG = ((topProfile.totalG * 1.0 + midProfile.totalG * 1.5 + botProfile.totalG * 1.0) / totalWeight).toInt()
        val avgB = ((topProfile.totalB * 1.0 + midProfile.totalB * 1.5 + botProfile.totalB * 1.0) / totalWeight).toInt()

        val totalPixels = topProfile.pixelCount + midProfile.pixelCount + botProfile.pixelCount
        val warmthRatio = (topProfile.warmPixels + midProfile.warmPixels + botProfile.warmPixels).toFloat() / totalPixels
        val coldRatio = (topProfile.coldPixels + midProfile.coldPixels + botProfile.coldPixels).toFloat() / totalPixels
        val skinToneRatio = (topProfile.skinPixels + midProfile.skinPixels + botProfile.skinPixels).toFloat() / totalPixels
        val darkPixelRatio = (topProfile.darkPixels + midProfile.darkPixels + botProfile.darkPixels).toFloat() / totalPixels
        val highlightRatio = (topProfile.highlightPixels + midProfile.highlightPixels + botProfile.highlightPixels).toFloat() / totalPixels

        val avgTotal = (avgR + avgG + avgB) / 3f

        // === 计算新增特征 ===

        // 饱和度均值与方差
        val totalSatSum = topProfile.saturationSum * 1.0 + midProfile.saturationSum * 1.5 + botProfile.saturationSum * 1.0
        val totalSatSqSum = topProfile.saturationSqSum * 1.0 + midProfile.saturationSqSum * 1.5 + botProfile.saturationSqSum * 1.0
        val saturationMean = (totalSatSum / totalWeight).toFloat().coerceIn(0f, 1f)
        val saturationVariance = ((totalSatSqSum / totalWeight) - saturationMean * saturationMean).coerceAtLeast(0.0).toFloat()

        // RGB 通道方差（衡量色彩多样度）
        val rVariance = variance(topProfile.rSqSum, topProfile.totalR, topProfile.pixelCount,
            midProfile.rSqSum, midProfile.totalR, midProfile.pixelCount,
            botProfile.rSqSum, botProfile.totalR, botProfile.pixelCount)
        val gVariance = variance(topProfile.gSqSum, topProfile.totalG, topProfile.pixelCount,
            midProfile.gSqSum, midProfile.totalG, midProfile.pixelCount,
            botProfile.gSqSum, botProfile.totalG, botProfile.pixelCount)
        val bVariance = variance(topProfile.bSqSum, topProfile.totalB, topProfile.pixelCount,
            midProfile.bSqSum, midProfile.totalB, midProfile.pixelCount,
            botProfile.bSqSum, botProfile.totalB, botProfile.pixelCount)
        val colorVariance = ((rVariance + gVariance + bVariance) / 3f).coerceAtLeast(0f)

        // 亮度标准差（高 → 明暗对比强烈）
        val totalLumSum = topProfile.luminanceSum * 1.0 + midProfile.luminanceSum * 1.5 + botProfile.luminanceSum * 1.0
        val totalLumSqSum = topProfile.luminanceSqSum * 1.0 + midProfile.luminanceSqSum * 1.5 + botProfile.luminanceSqSum * 1.0
        val lumMean = totalLumSum / totalWeight
        val brightnessStdDev = sqrt(((totalLumSqSum / totalWeight) - lumMean * lumMean).coerceAtLeast(0.0)).toFloat()

        // 对比度比
        val contrastRatio = highlightRatio / (darkPixelRatio + 0.01f)

        // 中性灰占比
        val totalGrayPixels = topProfile.grayPixels * 1.0f + midProfile.grayPixels * 1.5f + botProfile.grayPixels * 1.0f
        val grayDominance = (totalGrayPixels / totalWeight).coerceIn(0f, 1f)

        return ColorProfile(
            avgRed = avgR,
            avgGreen = avgG,
            avgBlue = avgB,
            warmthRatio = warmthRatio,
            greenDominance = if (avgTotal > 0) avgG / avgTotal else 1f,
            blueDominance = if (avgTotal > 0) avgB / avgTotal else 1f,
            redDominance = if (avgTotal > 0) avgR / avgTotal else 1f,
            skinToneRatio = skinToneRatio,
            darkPixelRatio = darkPixelRatio,
            highlightRatio = highlightRatio,
            skyBlueRatio = topProfile.blueDominance,
            groundWarmthRatio = botProfile.warmthRatio,
            saturationMean = saturationMean,
            saturationVariance = saturationVariance,
            colorVariance = colorVariance,
            brightnessStdDev = brightnessStdDev,
            contrastRatio = contrastRatio,
            grayDominance = grayDominance
        )
    }

    /**
     * 合并三个采样区域的加权方差
     * 公式：Var = E[X²] - E[X]²
     */
    private fun variance(
        sqSum1: Long, sum1: Long, count1: Int,
        sqSum2: Long, sum2: Long, count2: Int,
        sqSum3: Long, sum3: Long, count3: Int
    ): Float {
        val totalSq = sqSum1 + sqSum2 + sqSum3
        val totalSum = sum1 + sum2 + sum3
        val totalCount = count1 + count2 + count3
        if (totalCount <= 0) return 0f
        val mean = totalSum.toDouble() / totalCount
        return ((totalSq.toDouble() / totalCount) - mean * mean).toFloat().coerceAtLeast(0f)
    }

    /**
     * 采样指定区域的颜色特征
     */
    private data class RegionSample(
        val totalR: Long, val totalG: Long, val totalB: Long,
        val warmPixels: Int, val coldPixels: Int,
        val skinPixels: Int, val darkPixels: Int, val highlightPixels: Int,
        val pixelCount: Int,
        val blueDominance: Float, val warmthRatio: Float,
        // === 新增特征 ===
        val rSqSum: Long, val gSqSum: Long, val bSqSum: Long,  // RGB 平方和 → 方差
        val saturationSum: Double, val saturationSqSum: Double, // 饱和度 → 均值/方差
        val luminanceSum: Long, val luminanceSqSum: Long,       // 亮度 → 标准差
        val grayPixels: Int,                                      // 中性灰像素数
        val highContrastPixels: Int, val lowContrastPixels: Int  // 高/低对比度
    )

    private fun sampleRegion(
        bitmap: Bitmap, startX: Int, endX: Int, startY: Int, endY: Int,
        step: Int, weight: Float
    ): RegionSample {
        val sx = startX.coerceAtLeast(0)
        val ex = endX.coerceAtMost(bitmap.width)
        val sy = startY.coerceAtLeast(0)
        val ey = endY.coerceAtMost(bitmap.height)

        val regionWidth = ex - sx
        val regionHeight = ey - sy
        if (regionWidth <= 0 || regionHeight <= 0) {
            return RegionSample(0L, 0L, 0L, 0, 0, 0, 0, 0, 0, 1f, 0f,
                0L, 0L, 0L, 0.0, 0.0, 0L, 0L, 0, 0, 0)
        }

        // 批量读取所有像素
        val pixels = IntArray(regionWidth * regionHeight)
        bitmap.getPixels(pixels, 0, regionWidth, sx, sy, regionWidth, regionHeight)

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var rSq = 0L; var gSq = 0L; var bSq = 0L
        var warmPixels = 0; var coldPixels = 0
        var skinPixels = 0; var darkPixels = 0; var highlightPixels = 0
        var pixelCount = 0
        var blueSum = 0.0
        var saturationTotal = 0.0; var saturationSqTotal = 0.0
        var luminanceTotal = 0L; var luminanceSqTotal = 0L
        var grayPixels = 0
        var highContrastPixels = 0; var lowContrastPixels = 0

        for (y in sy until ey step step) {
            val rowOffset = (y - sy) * regionWidth
            for (x in sx until ex step step) {
                val pixel = pixels[rowOffset + (x - sx)]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                totalR += r; totalG += g; totalB += b
                rSq += r.toLong() * r; gSq += g.toLong() * g; bSq += b.toLong() * b
                pixelCount++

                // 蓝色主导度计算（用于天空检测）
                val avg = (r + g + b) / 3.0
                if (avg > 0) blueSum += b / avg

                // 暖色调判定：R > B + 20 且 R > G
                if (r > b + 20 && r > g) warmPixels++

                // 冷色调判定：B > R + 20 且 B > G
                if (b > r + 20 && b > g) coldPixels++

                // 肤色检测（YCbCr 色彩空间）
                if (isSkinTone(r, g, b)) skinPixels++

                // 饱和度计算（HSV 风格：(max-min)/max）
                val maxVal = maxOf(r, g, b)
                val minVal = minOf(r, g, b)
                val saturation = if (maxVal > 0) (maxVal - minVal).toDouble() / maxVal else 0.0
                saturationTotal += saturation
                saturationSqTotal += saturation * saturation

                // 中性灰检测：RGB 三通道差异 < 30 且饱和度 < 0.15
                if (maxVal - minVal < 30 && saturation < 0.15) grayPixels++

                // 亮度计算
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                luminanceTotal += luminance
                luminanceSqTotal += luminance.toLong() * luminance

                // 暗部判定：亮度 < 50
                if (luminance < 50) darkPixels++

                // 高光判定：亮度 > 200
                if (luminance > 200) highlightPixels++

                // 局部对比度：以当前像素为中心的 3x3 邻域（简化：使用相邻像素差异）
                // 这里简化为判断该像素与周围像素的亮度差异
                if (x > sx && x < ex - 1 && y > sy && y < ey - 1) {
                    val neighborPixel = pixels[rowOffset + (x - sx) + 1] // 右邻像素
                    val nr = (neighborPixel shr 16) and 0xFF
                    val ng = (neighborPixel shr 8) and 0xFF
                    val nb = neighborPixel and 0xFF
                    val neighborLum = (0.299 * nr + 0.587 * ng + 0.114 * nb).toInt()
                    if (abs(luminance - neighborLum) > 60) highContrastPixels++
                    else if (abs(luminance - neighborLum) < 10) lowContrastPixels++
                }
            }
        }

        val avgTotal = ((totalR + totalG + totalB) / 3.0).coerceAtLeast(1.0)
        val blueDominance = if (pixelCount > 0) (blueSum / pixelCount).toFloat() else 1f
        val warmthRatio = if (pixelCount > 0) warmPixels.toFloat() / pixelCount else 0f

        return RegionSample(
            totalR = (totalR * weight).toLong(),
            totalG = (totalG * weight).toLong(),
            totalB = (totalB * weight).toLong(),
            warmPixels = (warmPixels * weight).toInt(),
            coldPixels = (coldPixels * weight).toInt(),
            skinPixels = (skinPixels * weight).toInt(),
            darkPixels = (darkPixels * weight).toInt(),
            highlightPixels = (highlightPixels * weight).toInt(),
            pixelCount = (pixelCount * weight).toInt(),
            blueDominance = blueDominance,
            warmthRatio = warmthRatio,
            rSqSum = (rSq * weight).toLong(),
            gSqSum = (gSq * weight).toLong(),
            bSqSum = (bSq * weight).toLong(),
            saturationSum = saturationTotal * weight,
            saturationSqSum = saturationSqTotal * weight,
            luminanceSum = (luminanceTotal * weight).toLong(),
            luminanceSqSum = (luminanceSqTotal * weight).toLong(),
            grayPixels = (grayPixels * weight).toInt(),
            highContrastPixels = (highContrastPixels * weight).toInt(),
            lowContrastPixels = (lowContrastPixels * weight).toInt()
        )
    }

    /**
     * 肤色检测（YCbCr 色彩空间）
     * 改善：收窄范围并增加饱和度检查，减少误识别
     * - cb: 85-125 (原77-127)
     * - cr: 140-165 (原133-173)
     * - 新增饱和度下限 > 0.15
     */
    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        // RGB → YCbCr 转换
        val y = 16 + 0.257 * r + 0.504 * g + 0.098 * b
        val cb = 128 - 0.148 * r - 0.291 * g + 0.439 * b
        val cr = 128 + 0.439 * r - 0.368 * g - 0.071 * b

        // 计算饱和度（避免低饱和度颜色如奶油色、浅黄色被误判）
        val maxVal = maxOf(r, g, b)
        val minVal = minOf(r, g, b)
        val saturation = if (maxVal > 0) (maxVal - minVal).toFloat() / maxVal else 0f

        // 收窄后的肤色范围 + 饱和度检查
        return y in 80.0..230.0 &&
               cb in 85.0..125.0 &&
               cr in 140.0..165.0 &&
               saturation > 0.15f  // 新增：饱和度下限过滤
    }

    /**
     * 计算亮度等级
     *
     * 优化：使用 getPixels 批量读取整行像素，替代逐像素 getPixel 调用，
     * 减少 JNI 开销。
     */
    private fun computeBrightnessLevel(bitmap: Bitmap): BrightnessLevel {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return BrightnessLevel.NORMAL
        var totalLuminance = 0L
        var pixelCount = 0

        val step = when {
            width > 1000 -> 8
            width > 500 -> 4
            else -> 2
        }

        // 按行批量读取像素
        val rowPixels = IntArray(width)
        for (y in 0 until height step step) {
            bitmap.getPixels(rowPixels, 0, width, 0, y, width, 1)
            for (x in 0 until width step step) {
                val pixel = rowPixels[x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Rec. 709 亮度公式
                val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                totalLuminance += luminance
                pixelCount++
            }
        }

        if (pixelCount == 0) return BrightnessLevel.NORMAL
        val avgLuminance = (totalLuminance / pixelCount).toInt()

        return when {
            avgLuminance < 50 -> BrightnessLevel.VERY_DARK
            avgLuminance < 100 -> BrightnessLevel.DARK
            avgLuminance < 150 -> BrightnessLevel.NORMAL
            avgLuminance < 200 -> BrightnessLevel.BRIGHT
            else -> BrightnessLevel.VERY_BRIGHT
        }
    }

    /**
     * 人脸检测（使用 ML Kit Face Detection）
     * 使用单例 FaceDetector 避免反复创建销毁（性能优化）
     *
     * 优化：传入已计算的 colorProfile，避免回退时重复调用 sampleColorProfile
     */
    private suspend fun detectFaces(bitmap: Bitmap, colorProfile: ColorProfile? = null): Int = withContext(Dispatchers.Default) {
        try {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            // 使用单例 FaceDetector，避免每次创建销毁（节省 200-500ms）
            val faces = FaceDetectorSingleton.detect(inputImage)
            faces.size
        } catch (e: Exception) {
            // 检测失败时回退到肤色推断
            android.util.Log.w("HeuristicSceneAnalyzer", "ML Kit人脸检测失败，回退到肤色推断: ${e.message}")
            inferFaceCountBySkinTone(colorProfile ?: sampleColorProfile(bitmap, sampleRatio = 0.6f))
        }
    }

    /**
     * 基于肤色占比推断人脸数量（备用方案）
     */
    private fun inferFaceCountBySkinTone(colorProfile: ColorProfile): Int {
        return when {
            colorProfile.skinToneRatio > 0.15 -> 2
            colorProfile.skinToneRatio > 0.08 -> 1
            colorProfile.skinToneRatio > 0.03 -> 1
            else -> 0
        }
    }

    /**
     * 计算边缘密度（纹理分析）
     * 使用 Sobel 算子检测边缘
     * 改善：分两级采样 — 中心 200×200 跑精扫，外部 100×100 跑粗扫
     */
    private fun computeEdgeDensity(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        // 计算中心区域（占图像中心40%）
        val centerWidth = (width * 0.4).toInt()
        val centerHeight = (height * 0.4).toInt()
        val centerStartX = (width - centerWidth) / 2
        val centerStartY = (height - centerHeight) / 2

        // 追踪需要回收的临时 Bitmap，避免误回收原始 bitmap
        val bitmapsToRecycle = mutableListOf<Bitmap>()

        // 提取中心区域并缩放到 200×200 进行精扫
        val centerBitmap = Bitmap.createBitmap(bitmap, centerStartX, centerStartY, centerWidth, centerHeight)
        bitmapsToRecycle.add(centerBitmap)

        val fineBitmap = if (centerWidth > 200 || centerHeight > 200) {
            Bitmap.createScaledBitmap(centerBitmap, 200, 200, true).also { bitmapsToRecycle.add(it) }
        } else {
            centerBitmap
        }

        // 整体缩放到 100×100 进行粗扫
        val coarseBitmap = if (width > 100 || height > 100) {
            Bitmap.createScaledBitmap(bitmap, 100, 100, true).also { bitmapsToRecycle.add(it) }
        } else {
            bitmap
        }

        // 精扫中心区域（权重 2.0）
        val fineEdgeDensity = computeSobelEdgeDensity(fineBitmap, threshold = 40)

        // 粗扫整体（权重 1.0）
        val coarseEdgeDensity = computeSobelEdgeDensity(coarseBitmap, threshold = 50)

        // 回收临时 bitmap（不会误回收原始 bitmap）
        bitmapsToRecycle.forEach { it.recycle() }

        // 加权合并：精扫结果权重更高
        return (fineEdgeDensity * 2.0f + coarseEdgeDensity * 1.0f) / 3.0f
    }

    /**
     * 对指定 bitmap 执行 Sobel 边缘检测
     */
    private fun computeSobelEdgeDensity(bitmap: Bitmap, threshold: Int): Float {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3) return 0f

        // 批量读取所有像素
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // 预计算亮度数组，避免重复 getPixel 调用
        val luminance = FloatArray(w * h)
        for (i in pixels.indices) {
            luminance[i] = getLuminance(pixels[i])
        }

        var edgeCount = 0
        var totalPixels = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val gx = sobelX(luminance, w, idx)
                val gy = sobelY(luminance, w, idx)
                val gradient = sqrt(gx * gx + gy * gy)

                if (gradient > threshold) edgeCount++
                totalPixels++
            }
        }

        return if (totalPixels > 0) edgeCount.toFloat() / totalPixels else 0f
    }

    /**
     * Sobel X 方向算子
     */
    private fun sobelX(luminance: FloatArray, w: Int, idx: Int): Float {
        val p1 = luminance[idx - w - 1]
        val p2 = luminance[idx - w]
        val p3 = luminance[idx - w + 1]
        val p4 = luminance[idx - 1]
        val p6 = luminance[idx + 1]
        val p7 = luminance[idx + w - 1]
        val p8 = luminance[idx + w]
        val p9 = luminance[idx + w + 1]

        return (-p1 + p3 - 2 * p4 + 2 * p6 - p7 + p9)
    }

    /**
     * Sobel Y 方向算子
     */
    private fun sobelY(luminance: FloatArray, w: Int, idx: Int): Float {
        val p1 = luminance[idx - w - 1]
        val p2 = luminance[idx - w]
        val p3 = luminance[idx - w + 1]
        val p4 = luminance[idx - 1]
        val p6 = luminance[idx + 1]
        val p7 = luminance[idx + w - 1]
        val p8 = luminance[idx + w]
        val p9 = luminance[idx + w + 1]

        return (-p1 - 2 * p2 - p3 + p7 + 2 * p8 + p9)
    }

    private fun getLuminance(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    // ==================== 投票机制（增强版） ====================

    /**
     * 颜色→场景投票（增强版：多维特征匹配）
     *
     * 每个场景类型有独特的颜色签名：
     * - 美食：暖色调(0.3-0.55) + 高红主导 + 中高饱和度 + 低色彩方差
     * - 人像：肤色检测 + 中暖色调 + 中饱和度 + 中亮度
     * - 风景·森林：高绿主导 + 高饱和度 + 高色彩方差
     * - 风景·天空/海滩：高蓝主导 + 高亮度 + 高饱和度
     * - 风景·日落：极高暖色调 + 高饱和度 + 高亮度
     * - 风景·雪景：极高亮度 + 低饱和度 + 高蓝主导 + 低色彩方差
     * - 夜景：高暗部占比 + 低饱和度 + 高斯度的亮度标准差
     * - 城市/街拍：高灰占比 + 低饱和度 + 中高等边缘密度
     * - 微距：低色彩方差 + 高饱和度(主体) + 极高边缘密度
     * - 室内/静物：低对比度 + 中暖色调 + 低亮度标准差
     */
    private fun voteByColor(cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // ─── 美食：暖色调 + 红主导 + 中高饱和度 + 低色彩方差 ───
        if (cp.warmthRatio >= 0.25f && cp.warmthRatio < 0.55f
            && cp.redDominance > 1.05f
            && cp.saturationMean > 0.15f
        ) {
            val foodScore = (0.55f
                + cp.warmthRatio * 0.25f
                + cp.redDominance * 0.1f
                + cp.saturationMean * 0.3f
                - cp.colorVariance / 10000f * 0.1f  // 低色彩方差加分
            ).coerceIn(0.4f, 0.95f)
            votes.add(SceneCandidate("food-restaurant", foodScore, "color"))
            votes.add(SceneCandidate("food-dessert", foodScore * 0.88f, "color"))
            if (cp.warmthRatio > 0.35f) {
                votes.add(SceneCandidate("food-bbq", foodScore * 0.85f, "color"))
            }
        }

        // ─── 风景·森林：高绿主导 + 高饱和度 + 高色彩方差 ───
        if (cp.greenDominance > 1.20f) {
            val forestScore = (0.60f
                + (cp.greenDominance - 1.0f) * 0.3f
                + cp.saturationMean * 0.25f
                + (cp.colorVariance / 10000f).coerceAtMost(0.15f)
            ).coerceIn(0.4f, 0.95f)
            votes.add(SceneCandidate("landscape-forest", forestScore, "color"))
            votes.add(SceneCandidate("landscape-standard", forestScore * 0.82f, "color"))
            if (cp.warmthRatio > 0.3f && cp.greenDominance > 1.1f) {
                votes.add(SceneCandidate("landscape-autumn", forestScore * 0.8f, "color"))
            }
        }

        // ─── 风景·天空/海滩：高蓝主导 + 高亮度 ───
        if (cp.blueDominance > 1.15f && cp.highlightRatio > 0.1f) {
            val skyScore = (0.55f
                + (cp.blueDominance - 1.0f) * 0.35f
                + cp.highlightRatio * 0.2f
                + cp.saturationMean * 0.15f
            ).coerceIn(0.4f, 0.95f)
            votes.add(SceneCandidate("landscape-sky", skyScore, "color"))
            // 区分海滩：天空区域蓝 + 地面区域暖
            if (cp.skyBlueRatio > 1.2f && cp.groundWarmthRatio > 0.25f) {
                votes.add(SceneCandidate("landscape-beach", skyScore * 0.9f, "color"))
            } else {
                votes.add(SceneCandidate("landscape-beach", skyScore * 0.75f, "color"))
            }
        }

        // ─── 风景·日落：极高暖色调 + 高饱和度 + 高亮度 ───
        if (cp.warmthRatio >= 0.55f) {
            val sunsetScore = (0.60f
                + cp.warmthRatio * 0.3f
                + cp.saturationMean * 0.2f
                + cp.highlightRatio * 0.15f
            ).coerceIn(0.45f, 0.98f)
            votes.add(SceneCandidate("landscape-sunset", sunsetScore, "color"))
            // 极暖 + 高红 → 可能是沙漠
            if (cp.redDominance > 1.15f && cp.greenDominance < 0.95f) {
                votes.add(SceneCandidate("landscape-desert", sunsetScore * 0.82f, "color"))
            }
        }

        // ─── 风景·雪景：极高亮度 + 低饱和度 + 高蓝/灰 + 低色彩方差 ───
        if (cp.highlightRatio > 0.35f && cp.saturationMean < 0.18f && cp.grayDominance > 0.4f) {
            val snowScore = (0.55f
                + cp.highlightRatio * 0.3f
                + cp.grayDominance * 0.2f
                - cp.saturationMean * 0.3f
            ).coerceIn(0.4f, 0.95f)
            votes.add(SceneCandidate("landscape-snow", snowScore, "color"))
        }

        // ─── 夜景：高暗部占比 + 低饱和度 + 高亮度标准差(有光源) ───
        if (cp.darkPixelRatio > 0.55f) {
            val nightScore = (0.55f
                + cp.darkPixelRatio * 0.3f
                - cp.saturationMean * 0.2f
                + (cp.brightnessStdDev / 80f).coerceAtMost(0.15f)
            ).coerceIn(0.4f, 0.95f)
            votes.add(SceneCandidate("night-city", nightScore, "color"))
            if (cp.brightnessStdDev > 60f) {
                // 有光源 → 霓虹灯/城市夜景
                votes.add(SceneCandidate("night-neon", nightScore * 0.92f, "color"))
            }
            if (cp.darkPixelRatio > 0.80f) {
                votes.add(SceneCandidate("night-starry", nightScore * 0.8f, "color"))
            }
        }

        // ─── 城市/街拍：高灰占比 + 低饱和度 + 中高亮度标准差 ───
        if (cp.grayDominance > 0.30f && cp.saturationMean < 0.25f) {
            val urbanScore = (0.50f
                + cp.grayDominance * 0.3f
                - cp.saturationMean * 0.25f
                + (cp.brightnessStdDev / 100f).coerceAtMost(0.1f)
            ).coerceIn(0.35f, 0.85f)
            votes.add(SceneCandidate("urban-architecture", urbanScore, "color"))
            votes.add(SceneCandidate("urban-street", urbanScore * 0.9f, "color"))
            if (cp.warmthRatio > 0.2f && cp.brightnessStdDev < 50f) {
                votes.add(SceneCandidate("urban-cafe", urbanScore * 0.85f, "color"))
            }
        }

        // ─── 肤色检测 → 人像 ───
        if (cp.skinToneRatio > 0.04f) {
            val portraitScore = (0.65f
                + cp.skinToneRatio * 0.3f
                - (cp.darkPixelRatio * 0.15f).coerceAtMost(0.1f)
            ).coerceIn(0.5f, 0.98f)
            votes.add(SceneCandidate("portrait-standard", portraitScore, "color"))
            if (cp.highlightRatio > 0.15f && cp.warmthRatio > 0.3f) {
                votes.add(SceneCandidate("portrait-backlit", portraitScore * 0.85f, "color"))
            }
            if (cp.skinToneRatio > 0.12f) {
                votes.add(SceneCandidate("portrait-group", portraitScore * 0.8f, "color"))
            }
        }

        // ─── 微距/静物：低色彩方差 + 高饱和度(主体突出) ───
        if (cp.colorVariance < 3000f && cp.saturationMean > 0.2f && cp.skinToneRatio < 0.03f) {
            val macroScore = (0.45f
                - cp.colorVariance / 30000f
                + cp.saturationMean * 0.3f
            ).coerceIn(0.35f, 0.80f)
            votes.add(SceneCandidate("macro-texture", macroScore, "color"))
            votes.add(SceneCandidate("still-flower", macroScore * 0.9f, "color"))
        }

        // ─── 室内/静物：低对比度 + 中暖色调 + 低亮度标准差 ───
        if (cp.brightnessStdDev < 45f && cp.warmthRatio > 0.2f
            && cp.darkPixelRatio < 0.5f && cp.highlightRatio < 0.3f
        ) {
            val indoorScore = (0.45f
                - cp.brightnessStdDev / 200f
                + cp.warmthRatio * 0.15f
            ).coerceIn(0.3f, 0.75f)
            votes.add(SceneCandidate("still-product", indoorScore, "color"))
            votes.add(SceneCandidate("urban-cafe", indoorScore * 0.88f, "color"))
        }

        return votes
    }

    /**
     * 饱和度→场景投票（新增）
     *
     * 饱和度是场景区分的关键特征：
     * - 高饱和度(>0.3)：美食（高饱和暖色）、风景（高饱和自然色）、日落
     * - 中饱和度(0.15-0.3)：人像、街拍、微距
     * - 低饱和度(<0.15)：夜景、城市建筑、雪景、室内
     */
    private fun voteBySaturation(cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        if (cp.saturationMean > 0.30f) {
            // 高饱和度 → 美食/风景/日落
            if (cp.warmthRatio > 0.30f) {
                votes.add(SceneCandidate("food-restaurant", 0.55f + cp.saturationMean * 0.3f, "saturation"))
                votes.add(SceneCandidate("landscape-sunset", 0.50f + cp.saturationMean * 0.25f, "saturation"))
            }
            if (cp.greenDominance > 1.15f) {
                votes.add(SceneCandidate("landscape-forest", 0.55f + cp.saturationMean * 0.3f, "saturation"))
            }
            votes.add(SceneCandidate("landscape-standard", 0.50f + cp.saturationMean * 0.2f, "saturation"))
        } else if (cp.saturationMean < 0.12f) {
            // 低饱和度 → 夜景/城市/雪景/室内
            if (cp.darkPixelRatio > 0.50f) {
                votes.add(SceneCandidate("night-city", 0.55f - cp.saturationMean * 0.5f, "saturation"))
            }
            if (cp.grayDominance > 0.25f) {
                votes.add(SceneCandidate("urban-architecture", 0.50f - cp.saturationMean * 0.5f, "saturation"))
                votes.add(SceneCandidate("urban-street", 0.45f - cp.saturationMean * 0.5f, "saturation"))
            }
            if (cp.highlightRatio > 0.30f) {
                votes.add(SceneCandidate("landscape-snow", 0.50f - cp.saturationMean * 0.5f, "saturation"))
            }
            votes.add(SceneCandidate("still-product", 0.40f - cp.saturationMean * 0.5f, "saturation"))
        } else {
            // 中饱和度 → 人像/微距/静物
            if (cp.skinToneRatio > 0.03f) {
                votes.add(SceneCandidate("portrait-standard", 0.50f, "saturation"))
            }
            votes.add(SceneCandidate("macro-texture", 0.45f, "saturation"))
            votes.add(SceneCandidate("still-flower", 0.42f, "saturation"))
        }

        return votes
    }

    /**
     * 对比度→场景投票（新增）
     *
     * 对比度是区分城市/室内/夜景的关键特征：
     * - 高对比度(contrastRatio>2)：城市建筑（明暗分明）、街拍
     * - 中对比度(0.5-2)：人像、风景、美食
     * - 低对比度(<0.5)：室内、柔光人像、夜景(整体暗)
     */
    private fun voteByContrast(cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        if (cp.contrastRatio > 2.5f) {
            // 高对比度 → 城市/建筑
            votes.add(SceneCandidate("urban-architecture", 0.55f + cp.contrastRatio.coerceAtMost(5f) * 0.06f, "contrast"))
            votes.add(SceneCandidate("urban-street", 0.48f + cp.contrastRatio.coerceAtMost(5f) * 0.05f, "contrast"))
            if (cp.grayDominance > 0.3f) {
                votes.add(SceneCandidate("urban-museum", 0.42f, "contrast"))
            }
        } else if (cp.contrastRatio < 0.4f) {
            // 低对比度 → 室内/柔光人像/夜景(整体暗)
            if (cp.darkPixelRatio > 0.50f) {
                votes.add(SceneCandidate("night-city", 0.50f, "contrast"))
            } else if (cp.warmthRatio > 0.25f) {
                votes.add(SceneCandidate("still-product", 0.48f, "contrast"))
                votes.add(SceneCandidate("urban-cafe", 0.45f, "contrast"))
            }
            if (cp.skinToneRatio > 0.03f) {
                votes.add(SceneCandidate("portrait-standard", 0.50f, "contrast"))
            }
        } else {
            // 中对比度 → 风景/美食/人像
            votes.add(SceneCandidate("landscape-standard", 0.45f, "contrast"))
            if (cp.warmthRatio > 0.25f) {
                votes.add(SceneCandidate("food-restaurant", 0.42f, "contrast"))
            }
        }

        return votes
    }

    /**
     * 亮度→场景投票（增强版）
     */
    private fun voteByBrightness(level: BrightnessLevel, cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (level) {
            BrightnessLevel.VERY_DARK -> {
                votes.add(SceneCandidate("night-city", 0.78f, "brightness"))
                if (cp.brightnessStdDev > 50f) {
                    // 有光源 → 霓虹灯/城市夜景
                    votes.add(SceneCandidate("night-neon", 0.72f, "brightness"))
                }
                if (cp.darkPixelRatio > 0.85f) {
                    votes.add(SceneCandidate("night-starry", 0.68f, "brightness"))
                }
                if (cp.warmthRatio > 0.35f) {
                    votes.add(SceneCandidate("night-candle", 0.62f, "brightness"))
                }
            }
            BrightnessLevel.DARK -> {
                votes.add(SceneCandidate("night-city", 0.55f, "brightness"))
                if (cp.warmthRatio > 0.35f) {
                    votes.add(SceneCandidate("night-candle", 0.65f, "brightness"))
                    votes.add(SceneCandidate("urban-cafe", 0.55f, "brightness"))
                }
                if (cp.grayDominance > 0.3f) {
                    votes.add(SceneCandidate("urban-museum", 0.50f, "brightness"))
                }
            }
            BrightnessLevel.NORMAL -> {
                // 正常亮度，不添加特定投票
            }
            BrightnessLevel.BRIGHT -> {
                if (cp.warmthRatio > 0.5f) {
                    votes.add(SceneCandidate("landscape-sunset", 0.68f, "brightness"))
                }
                if (cp.blueDominance > 1.15f) {
                    votes.add(SceneCandidate("landscape-sky", 0.62f, "brightness"))
                }
                if (cp.greenDominance > 1.15f) {
                    votes.add(SceneCandidate("landscape-forest", 0.58f, "brightness"))
                }
                votes.add(SceneCandidate("landscape-standard", 0.52f, "brightness"))
            }
            BrightnessLevel.VERY_BRIGHT -> {
                if (cp.saturationMean < 0.18f && cp.grayDominance > 0.35f) {
                    votes.add(SceneCandidate("landscape-snow", 0.72f, "brightness"))
                }
                if (cp.blueDominance > 1.15f && cp.groundWarmthRatio > 0.2f) {
                    votes.add(SceneCandidate("landscape-beach", 0.68f, "brightness"))
                }
                if (cp.warmthRatio > 0.55f) {
                    votes.add(SceneCandidate("landscape-sunset", 0.72f, "brightness"))
                }
                votes.add(SceneCandidate("landscape-beach", 0.55f, "brightness"))
                votes.add(SceneCandidate("landscape-snow", 0.52f, "brightness"))
            }
        }

        return votes
    }

    /**
     * 人脸→场景投票
     */
    private fun voteByFace(faceCount: Int, cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (faceCount) {
            1 -> {
                // 单人 → 人像
                votes.add(SceneCandidate("portrait-standard", 0.85f, "face"))
                if (cp.warmthRatio > 0.4f) {
                    votes.add(SceneCandidate("portrait-backlit", 0.70f, "face"))
                }
            }
            2 -> {
                // 双人 → 情侣
                votes.add(SceneCandidate("portrait-couple", 0.80f, "face"))
                votes.add(SceneCandidate("portrait-standard", 0.75f, "face"))
            }
            in 3..5 -> {
                // 多人 → 合影
                votes.add(SceneCandidate("portrait-group", 0.75f, "face"))
            }
            else -> {
                // 大量人群 → 街拍/活动
                votes.add(SceneCandidate("urban-street", 0.65f, "face"))
                votes.add(SceneCandidate("event-party", 0.60f, "face"))
            }
        }

        return votes
    }

    /**
     * EXIF→场景投票
     *
     * 规则：
     * ├── ISO > 800 → 暗光/夜景
     * ├── 闪光灯=开 → 室内/夜景人像
     * ├── GPS + 时间 → 日落时段判断
     * └── 焦距 < 5mm → 微距
     */
    private fun voteByExif(exif: ExifData, brightness: BrightnessLevel): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // ISO 分析
        exif.iso?.let { iso ->
            if (iso > 800) {
                votes.add(SceneCandidate("night-city", 0.70f, "exif"))
                votes.add(SceneCandidate("night-neon", 0.65f, "exif"))
                if (iso > 1600) {
                    votes.add(SceneCandidate("night-starry", 0.60f, "exif"))
                }
            }
        }

        // 焦距分析
        exif.focalLength?.let { focal ->
            if (focal < 5f) {
                votes.add(SceneCandidate("macro-insect", 0.70f, "exif"))
                votes.add(SceneCandidate("macro-texture", 0.65f, "exif"))
            }
            if (focal > 85f) {
                votes.add(SceneCandidate("portrait-standard", 0.65f, "exif"))
            }
            if (focal < 24f) {
                votes.add(SceneCandidate("landscape-standard", 0.60f, "exif"))
                votes.add(SceneCandidate("urban-architecture", 0.55f, "exif"))
            }
        }

        // 光圈分析
        exif.fNumber?.let { aperture ->
            if (aperture < 2.8f) {
                votes.add(SceneCandidate("portrait-standard", 0.55f, "exif"))
            }
            if (aperture > 8f) {
                votes.add(SceneCandidate("landscape-standard", 0.55f, "exif"))
            }
        }

        return votes
    }

    /**
     * 修复 #16：判定 EXIF 是否提供足够信息以跑完整 EXIF 投票
     * 当 ISO / 焦距 / 光圈 全部为 null 时视为 EXIF 缺失（典型的大师模式自动档场景），
     * 此时调用方走 voteByExifFallback 简化流程，避免整段代码空跑
     */
    private fun hasEnoughExif(exif: ExifData): Boolean {
        return exif.iso != null || exif.focalLength != null || exif.fNumber != null
    }

    /**
     * 修复 #16：EXIF 缺失时的简化投票流程
     * 思路：场景已通过颜色直方图/亮度分析初步确定，这里只补"亮度 → 场景" 的二次强化
     * 比直接跳过 EXIF 投票更激进，但比跑无字段的 voteByExif 更有效
     */
    private fun voteByExifFallback(
        brightness: BrightnessLevel,
        colorProfile: ColorProfile
    ): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()
        // 用亮度做二段强化
        when (brightness) {
            BrightnessLevel.VERY_DARK -> {
                votes.add(SceneCandidate("night-city", 0.55f, "exif-fallback"))
                votes.add(SceneCandidate("night-neon", 0.50f, "exif-fallback"))
            }
            BrightnessLevel.DARK -> {
                votes.add(SceneCandidate("night-candle", 0.45f, "exif-fallback"))
            }
            BrightnessLevel.VERY_BRIGHT -> {
                votes.add(SceneCandidate("landscape-snow", 0.40f, "exif-fallback"))
                votes.add(SceneCandidate("landscape-beach", 0.40f, "exif-fallback"))
            }
            else -> { /* 中间亮度不补充 */ }
        }
        // 用色调倾向做轻量补偿
        if (colorProfile.warmthRatio > 0.55f) {
            votes.add(SceneCandidate("landscape-sunset", 0.35f, "exif-fallback"))
        }
        return votes
    }

    /**
     * 纹理→场景投票（增强版）
     *
     * 边缘密度区分场景：
     * - 极高(>0.35)：微距昆虫/纹理（细节丰富）
     * - 高(0.25-0.35)：城市建筑（结构化边缘）、街拍
     * - 中(0.12-0.25)：风景（自然纹理）、美食
     * - 低(<0.12)：人像（柔焦/大光圈）、夜景（暗部边缘少）、室内
     */
    private fun voteByTexture(edgeDensity: Float, cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        if (edgeDensity > 0.35f) {
            // 极高边缘密度 → 微距/纹理
            votes.add(SceneCandidate("macro-texture", 0.65f + edgeDensity * 0.2f, "texture"))
            votes.add(SceneCandidate("macro-insect", 0.60f + edgeDensity * 0.15f, "texture"))
            if (cp.grayDominance > 0.25f) {
                votes.add(SceneCandidate("urban-architecture", 0.58f + edgeDensity * 0.12f, "texture"))
            }
        } else if (edgeDensity > 0.25f) {
            // 高边缘密度 → 城市建筑/街拍
            votes.add(SceneCandidate("urban-architecture", 0.62f + edgeDensity * 0.15f, "texture"))
            votes.add(SceneCandidate("urban-street", 0.58f + edgeDensity * 0.12f, "texture"))
            if (cp.greenDominance > 1.1f) {
                votes.add(SceneCandidate("landscape-forest", 0.52f, "texture"))
            }
        } else if (edgeDensity > 0.12f) {
            // 中边缘密度 → 风景/美食/街拍
            votes.add(SceneCandidate("landscape-standard", 0.55f, "texture"))
            votes.add(SceneCandidate("urban-street", 0.50f, "texture"))
            if (cp.warmthRatio > 0.25f) {
                votes.add(SceneCandidate("food-restaurant", 0.48f, "texture"))
            }
            votes.add(SceneCandidate("still-product", 0.45f, "texture"))
        } else {
            // 低边缘密度 → 人像/柔光/夜景/室内
            if (cp.skinToneRatio > 0.03f) {
                votes.add(SceneCandidate("portrait-standard", 0.62f, "texture"))
                votes.add(SceneCandidate("portrait-child", 0.55f, "texture"))
            }
            if (cp.darkPixelRatio > 0.50f) {
                votes.add(SceneCandidate("night-city", 0.52f, "texture"))
            }
            if (cp.warmthRatio > 0.25f && cp.skinToneRatio < 0.03f) {
                votes.add(SceneCandidate("food-dessert", 0.50f, "texture"))
            }
            votes.add(SceneCandidate("still-product", 0.45f, "texture"))
        }

        return votes
    }

    /**
     * 加权融合投票结果（增强版）
     *
     * 置信度计算改进：
     * - 旧方案：primaryScore / totalScore → 候选越多置信度越低（不合理）
     * - 新方案：基于第1名与第2名的分数差距 + 多维度一致性 + 绝对分数
     * 
     * 公式：confidence = 0.4 * gapRatio + 0.3 * dimensionConsistency + 0.3 * absoluteScore
     *   - gapRatio: 1 - (score2/score1)，差距越大置信度越高
     *   - dimensionConsistency: 支持该场景的维度数 / 总维度数
     *   - absoluteScore: 归一化后的绝对分数
     */
    private fun fuseVotes(
        candidates: List<SceneCandidate>,
        userContext: UserContext?
    ): FusedResult {
        // 按场景ID分组并累加分数
        val scoreMap = mutableMapOf<String, Float>()
        val sourceMap = mutableMapOf<String, MutableSet<String>>()

        // 权重配置（新增 saturation 和 contrast 维度）
        val weights = mapOf(
            "color" to 1.0f,
            "saturation" to 0.85f,
            "contrast" to 0.80f,
            "brightness" to 0.75f,
            "face" to 1.3f,       // 人脸检测权重最高
            "exif" to 0.9f,
            "exif-fallback" to 0.5f,
            "texture" to 0.7f
        )

        for (candidate in candidates) {
            val weight = weights[candidate.source] ?: 1.0f
            val weightedScore = candidate.score * weight

            scoreMap[candidate.sceneId] = scoreMap.getOrDefault(candidate.sceneId, 0f) + weightedScore
            sourceMap.getOrPut(candidate.sceneId) { mutableSetOf() }.add(candidate.source)
        }

        // 用户上下文加成
        userContext?.let { ctx ->
            ctx.recentScenes.forEach { recentScene ->
                scoreMap[recentScene]?.let { currentScore ->
                    scoreMap[recentScene] = currentScore + 0.15f
                }
            }
            ctx.preferredCategories.forEach { category ->
                ScenePresets.getScenesByCategory(category).forEach { scene ->
                    scoreMap[scene.id]?.let { currentScore ->
                        scoreMap[scene.id] = currentScore + 0.10f
                    }
                }
            }
        }

        // 排序并取Top-4
        val sorted = scoreMap.entries.sortedByDescending { it.value }
        val topScenes = sorted.take(4)

        // 获取场景预设（安全处理空列表）
        if (topScenes.isEmpty()) {
            val defaultScene = ScenePresets.allScenes.firstOrNull()
                ?: SceneProfile(
                    id = "unknown",
                    name = "Unknown",
                    category = SceneCategory.PORTRAIT,
                    description = "",
                    color = 0xFFFF6B35,
                    confidence = 0f,
                    hasselbladParams = HasselbladParams(),
                    recommendedFilm = emptyList(),
                    masterTips = emptyList()
                )
            return FusedResult(
                primary = defaultScene,
                confidence = 0f,
                alternatives = emptyList()
            )
        }

        val primaryScene = ScenePresets.getSceneById(topScenes.first().key)
            ?: ScenePresets.allScenes.first()

        val alternatives = topScenes.drop(1).mapNotNull { entry ->
            ScenePresets.getSceneById(entry.key)
        }

        // ─── 增强的置信度计算 ───
        val primaryScore = topScenes.first().value
        val secondScore = topScenes.getOrNull(1)?.value ?: 0f

        // 1. 分数差距比：第1名领先第2名越多，置信度越高
        val gapRatio = if (secondScore > 0f) {
            (1f - secondScore / primaryScore.coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        } else {
            1f  // 没有第二名，完全置信
        }

        // 2. 多维度一致性：支持该场景的维度越多，置信度越高
        val primarySources = sourceMap[topScenes.first().key] ?: emptySet()
        val totalSourceCount = sourceMap.values.flatten().toSet().size
        val dimensionConsistency = if (totalSourceCount > 0) {
            primarySources.size.toFloat() / totalSourceCount.coerceAtLeast(1)
        } else 0f

        // 3. 绝对分数归一化：取前4名的总分归一化
        val maxPossibleScore = weights.size * 1.3f * 0.98f  // 理论最大分
        val absoluteScore = (primaryScore / maxPossibleScore).coerceIn(0f, 1f)

        // 综合置信度
        val confidence = (0.40f * gapRatio + 0.30f * dimensionConsistency + 0.30f * absoluteScore)
            .coerceIn(0.15f, 0.98f)

        return FusedResult(
            primary = primaryScene.copy(confidence = confidence),
            confidence = confidence,
            alternatives = alternatives
        )
    }

    /**
     * 构建分析详情
     */
    private fun buildAnalysisDetails(
        cp: ColorProfile,
        brightness: BrightnessLevel,
        faceCount: Int,
        edgeDensity: Float,
        exif: ExifData?
    ): Map<String, Float> {
        return mapOf(
            "warmth_ratio" to cp.warmthRatio,
            "green_dominance" to cp.greenDominance,
            "blue_dominance" to cp.blueDominance,
            "red_dominance" to cp.redDominance,
            "skin_tone_ratio" to cp.skinToneRatio,
            "dark_pixel_ratio" to cp.darkPixelRatio,
            "highlight_ratio" to cp.highlightRatio,
            "brightness_level" to when (brightness) {
                BrightnessLevel.VERY_DARK -> 0f
                BrightnessLevel.DARK -> 1f
                BrightnessLevel.NORMAL -> 2f
                BrightnessLevel.BRIGHT -> 3f
                BrightnessLevel.VERY_BRIGHT -> 4f
            },
            "face_count" to faceCount.toFloat(),
            "edge_density" to edgeDensity,
            "has_exif" to if (exif != null) 1f else 0f,
            // === 新增特征 ===
            "saturation_mean" to cp.saturationMean,
            "saturation_variance" to cp.saturationVariance,
            "color_variance" to cp.colorVariance,
            "brightness_stddev" to cp.brightnessStdDev,
            "contrast_ratio" to cp.contrastRatio,
            "gray_dominance" to cp.grayDominance
        )
    }

    data class FusedResult(
        val primary: SceneProfile,
        val confidence: Float,
        val alternatives: List<SceneProfile>
    )

    /**
     * 用户上下文
     */
    data class UserContext(
        val recentScenes: List<String> = emptyList(),      // 最近拍摄的场景
        val preferredCategories: List<SceneCategory> = emptyList(),  // 偏好类别
        val shootingHistory: List<ShootingRecord> = emptyList()
    )

    data class ShootingRecord(
        val sceneId: String,
        val timestamp: Long,
        val rating: Float? = null
    )

    companion object {
        @Volatile
        private var instance: HeuristicSceneAnalyzer? = null

        fun getInstance(context: Context): HeuristicSceneAnalyzer {
            return instance ?: synchronized(this) {
                instance ?: HeuristicSceneAnalyzer(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * ML Kit FaceDetector 单例
 * 避免每次分析都创建销毁检测器（节省 200-500ms 初始化时间）
 */
object FaceDetectorSingleton {
    private var faceDetector: com.google.mlkit.vision.face.FaceDetector? = null

    private fun getDetector(): com.google.mlkit.vision.face.FaceDetector {
        if (faceDetector == null) {
            try {
                val options = com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                    .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build()
                faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(options)
            } catch (e: Exception) {
                android.util.Log.w("FaceDetectorSingleton", "ML Kit 初始化失败: ${e.message}")
                throw e
            }
        }
        return faceDetector ?: throw IllegalStateException("FaceDetector 初始化失败，请检查 ML Kit 依赖")
    }

    suspend fun detect(inputImage: com.google.mlkit.vision.common.InputImage): List<com.google.mlkit.vision.face.Face> {
        return try {
            getDetector().process(inputImage).await()
        } catch (e: Exception) {
            android.util.Log.w("FaceDetectorSingleton", "Face detection 执行失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 释放 FaceDetector 资源
     * 应在 OMasterApplication.onTerminate 或适当生命周期调用
     */
    fun release() {
        faceDetector?.close()
        faceDetector = null
    }
}