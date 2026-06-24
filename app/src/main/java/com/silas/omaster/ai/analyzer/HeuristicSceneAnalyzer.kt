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
     * 颜色画像
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
        val darkPixelRatio: Float,     // 暗部像素占比
        val highlightRatio: Float,     // 高光像素占比
        val skyBlueRatio: Float = 0f,  // 天空区域蓝色主导度
        val groundWarmthRatio: Float = 0f  // 地面区域暖色调占比
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

        // 3. 人脸检测（使用 ML Kit Face Detection）
        val faceCount = detectFaces(bitmap)

        // 4. 纹理分析（边缘密度）
        val edgeDensity = computeEdgeDensity(bitmap)

        // 5. 多特征投票
        val candidates = mutableListOf<SceneCandidate>()

        // 颜色投票
        candidates.addAll(voteByColor(colorProfile))
        // 亮度投票
        candidates.addAll(voteByBrightness(brightnessLevel, colorProfile))
        // 人脸投票
        if (faceCount > 0) candidates.addAll(voteByFace(faceCount, colorProfile))
        // EXIF 投票（修复 #16：EXIF 缺失时走简化流程，避免整段白跑）
        if (exif != null && hasEnoughExif(exif)) {
            candidates.addAll(voteByExif(exif, brightnessLevel))
        } else {
            // 修复 #16：EXIF 缺失/字段不全时 fallback 到「场景已确定 + 亮度判断」二步简化流程
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
            // 新增：天空和地面特征
            skyBlueRatio = topProfile.blueDominance,
            groundWarmthRatio = botProfile.warmthRatio
        )
    }

    /**
     * 采样指定区域的颜色特征
     */
    private data class RegionSample(
        val totalR: Long, val totalG: Long, val totalB: Long,
        val warmPixels: Int, val coldPixels: Int,
        val skinPixels: Int, val darkPixels: Int, val highlightPixels: Int,
        val pixelCount: Int,
        val blueDominance: Float, val warmthRatio: Float
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
            return RegionSample(0L, 0L, 0L, 0, 0, 0, 0, 0, 0, 1f, 0f)
        }

        // 批量读取所有像素
        val pixels = IntArray(regionWidth * regionHeight)
        bitmap.getPixels(pixels, 0, regionWidth, sx, sy, regionWidth, regionHeight)

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var warmPixels = 0; var coldPixels = 0
        var skinPixels = 0; var darkPixels = 0; var highlightPixels = 0
        var pixelCount = 0
        var blueSum = 0.0

        for (y in sy until ey step step) {
            val rowOffset = (y - sy) * regionWidth
            for (x in sx until ex step step) {
                val pixel = pixels[rowOffset + (x - sx)]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                totalR += r; totalG += g; totalB += b
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

                // 暗部判定：亮度 < 50
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (luminance < 50) darkPixels++

                // 高光判定：亮度 > 200
                if (luminance > 200) highlightPixels++
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
            warmthRatio = warmthRatio
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
     */
    private fun computeBrightnessLevel(bitmap: Bitmap): BrightnessLevel {
        val width = bitmap.width
        val height = bitmap.height
        var totalLuminance = 0L
        var pixelCount = 0

        val step = when {
            width > 1000 -> 8
            width > 500 -> 4
            else -> 2
        }

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Rec. 709 亮度公式
                val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                totalLuminance += luminance
                pixelCount++
            }
        }

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
     */
    private suspend fun detectFaces(bitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        try {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            // 使用单例 FaceDetector，避免每次创建销毁（节省 200-500ms）
            val faces = FaceDetectorSingleton.detect(inputImage)
            faces.size
        } catch (e: Exception) {
            // 检测失败时回退到肤色推断
            android.util.Log.w("HeuristicSceneAnalyzer", "ML Kit人脸检测失败，回退到肤色推断: ${e.message}")
            inferFaceCountBySkinTone(bitmap)
        }
    }

    /**
     * 基于肤色占比推断人脸数量（备用方案）
     */
    private fun inferFaceCountBySkinTone(bitmap: Bitmap): Int {
        val colorProfile = sampleColorProfile(bitmap, sampleRatio = 0.6f)
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

    // ==================== 投票机制 ====================

    /**
     * 颜色→场景投票
     * 
     * 规则：
     * ├── 暖色调占比 > 60% + 高亮度 → 日落/金色时刻
     * ├── 绿色通道占比 > 35% → 森林/自然
     * ├── 蓝色通道占比 > 40% → 天空/海滩
     * ├── 暗部占比 > 70% → 夜景
     * └── 肤色检测 → 人像
     */
    private fun voteByColor(cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // 绿色主导 → 森林/自然
        if (cp.greenDominance > 1.25f) {
            val score = 0.70f * cp.greenDominance.coerceAtMost(1.5f)
            votes.add(SceneCandidate("landscape-forest", score, "color"))
            votes.add(SceneCandidate("landscape-standard", score * 0.8f, "color"))
        }

        // 蓝色主导 → 天空/海滩
        if (cp.blueDominance > 1.20f) {
            val score = 0.65f * cp.blueDominance.coerceAtMost(1.5f)
            votes.add(SceneCandidate("landscape-sky", score, "color"))
            votes.add(SceneCandidate("landscape-beach", score * 0.85f, "color"))
        }

        // 暖色调 >= 55% → 日落（修复：与下方 35%-55% 区间互斥，>= 55% 归日落）
        if (cp.warmthRatio >= 0.55f) {
            val score = 0.55f + cp.warmthRatio * 0.3f
            votes.add(SceneCandidate("landscape-sunset", score.coerceAtMost(0.95f), "color"))
        }

        // 暖色调 35%-55%（不含55%）→ 美食（修复：与上方区间互斥，避免重叠）
        if (cp.warmthRatio >= 0.35f && cp.warmthRatio < 0.55f) {
            val score = 0.50f + cp.warmthRatio * 0.2f
            votes.add(SceneCandidate("food-restaurant", score, "color"))
            votes.add(SceneCandidate("food-dessert", score * 0.9f, "color"))
        }

        // 暗部占比 > 70% → 夜景
        if (cp.darkPixelRatio > 0.70f) {
            val score = 0.60f + cp.darkPixelRatio * 0.25f
            votes.add(SceneCandidate("night-city", score.coerceAtMost(0.90f), "color"))
            votes.add(SceneCandidate("night-neon", score * 0.85f, "color"))
        }

        // 肤色检测 → 人像
        if (cp.skinToneRatio > 0.05f) {
            val score = 0.65f + cp.skinToneRatio * 0.3f
            votes.add(SceneCandidate("portrait-standard", score.coerceAtMost(0.95f), "color"))
            votes.add(SceneCandidate("portrait-backlit", score * 0.8f, "color"))
        }

        // 高光占比高 → 可能是逆光场景
        if (cp.highlightRatio > 0.15f && cp.warmthRatio > 0.3f) {
            votes.add(SceneCandidate("portrait-backlit", 0.55f + cp.highlightRatio * 0.2f, "color"))
        }

        return votes
    }

    /**
     * 亮度→场景投票
     */
    private fun voteByBrightness(level: BrightnessLevel, cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (level) {
            BrightnessLevel.VERY_DARK -> {
                votes.add(SceneCandidate("night-city", 0.75f, "brightness"))
                votes.add(SceneCandidate("night-starry", 0.70f, "brightness"))
                votes.add(SceneCandidate("night-neon", 0.65f, "brightness"))
            }
            BrightnessLevel.DARK -> {
                votes.add(SceneCandidate("night-candle", 0.60f, "brightness"))
                votes.add(SceneCandidate("urban-cafe", 0.55f, "brightness"))
                if (cp.warmthRatio > 0.4f) {
                    votes.add(SceneCandidate("night-candle", 0.70f, "brightness"))
                }
            }
            BrightnessLevel.NORMAL -> {
                // 正常亮度，不添加特定投票
            }
            BrightnessLevel.BRIGHT -> {
                if (cp.warmthRatio > 0.5f) {
                    votes.add(SceneCandidate("landscape-sunset", 0.65f, "brightness"))
                }
                if (cp.blueDominance > 1.2f) {
                    votes.add(SceneCandidate("landscape-sky", 0.60f, "brightness"))
                }
            }
            BrightnessLevel.VERY_BRIGHT -> {
                votes.add(SceneCandidate("landscape-beach", 0.65f, "brightness"))
                votes.add(SceneCandidate("landscape-snow", 0.60f, "brightness"))
                if (cp.warmthRatio > 0.6f) {
                    votes.add(SceneCandidate("landscape-sunset", 0.75f, "brightness"))
                }
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
     * 纹理→场景投票
     * 
     * 规则：
     * ├── 高对比 + 清晰边缘 → 建筑/街拍
     * ├── 低对比 + 柔和 → 人像/柔光
     * └── 高纹理密度 → 细节特写/微距
     */
    private fun voteByTexture(edgeDensity: Float, cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // 高边缘密度 → 建筑/街拍/微距
        if (edgeDensity > 0.30f) {
            votes.add(SceneCandidate("urban-architecture", 0.65f + edgeDensity * 0.2f, "texture"))
            votes.add(SceneCandidate("urban-street", 0.60f + edgeDensity * 0.15f, "texture"))
            votes.add(SceneCandidate("macro-texture", 0.55f + edgeDensity * 0.25f, "texture"))
        }

        // 中等边缘密度 → 正常场景
        if (edgeDensity in 0.15f..0.30f) {
            votes.add(SceneCandidate("landscape-standard", 0.55f, "texture"))
            votes.add(SceneCandidate("still-product", 0.50f, "texture"))
        }

        // 低边缘密度 → 柔光场景
        if (edgeDensity < 0.15f) {
            votes.add(SceneCandidate("portrait-standard", 0.60f, "texture"))
            votes.add(SceneCandidate("portrait-child", 0.55f, "texture"))
            if (cp.warmthRatio > 0.3f) {
                votes.add(SceneCandidate("food-dessert", 0.50f, "texture"))
            }
        }

        return votes
    }

    /**
     * 加权融合投票结果
     */
    private fun fuseVotes(
        candidates: List<SceneCandidate>,
        userContext: UserContext?
    ): FusedResult {
        // 按场景ID分组并累加分数
        val scoreMap = mutableMapOf<String, Float>()
        val sourceMap = mutableMapOf<String, MutableList<String>>()

        // 权重配置
        val weights = mapOf(
            "color" to 1.0f,
            "brightness" to 0.8f,
            "face" to 1.2f,      // 人脸检测权重最高
            "exif" to 0.9f,
            "texture" to 0.7f
        )

        for (candidate in candidates) {
            val weight = weights[candidate.source] ?: 1.0f
            val weightedScore = candidate.score * weight

            scoreMap[candidate.sceneId] = scoreMap.getOrDefault(candidate.sceneId, 0f) + weightedScore
            sourceMap.getOrPut(candidate.sceneId) { mutableListOf() }.add(candidate.source)
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

        // 计算置信度
        val totalScore = sorted.sumOf { it.value.toDouble() }.toFloat()
        val primaryScore = topScenes.first().value
        val confidence = (primaryScore / totalScore.coerceAtLeast(1f)).coerceIn(0f, 1f)

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
            "has_exif" to if (exif != null) 1f else 0f
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