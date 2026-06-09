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
 * 未来: TFLite 模型（MediaPipe Image Classifier）
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
        val highlightRatio: Float      // 高光像素占比
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
        // EXIF 投票
        if (exif != null) candidates.addAll(voteByExif(exif, brightnessLevel))
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
     * 采样策略：取中心区域，避免边缘干扰
     */
    private fun sampleColorProfile(bitmap: Bitmap, sampleRatio: Float): ColorProfile {
        val width = bitmap.width
        val height = bitmap.height
        val startX = (width * (1 - sampleRatio) / 2).toInt()
        val startY = (height * (1 - sampleRatio) / 2).toInt()
        val sampleW = (width * sampleRatio).toInt()
        val sampleH = (height * sampleRatio).toInt()

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var warmPixels = 0; var coldPixels = 0
        var skinPixels = 0; var darkPixels = 0; var highlightPixels = 0
        var totalPixels = 0

        // 采样步长：根据图片大小动态调整
        val step = when {
            sampleW > 1000 -> 8
            sampleW > 500 -> 4
            else -> 2
        }

        for (y in startY until startY + sampleH step step) {
            for (x in startX until startX + sampleW step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                totalR += r; totalG += g; totalB += b
                totalPixels++

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

        val avgR = (totalR / totalPixels).toInt()
        val avgG = (totalG / totalPixels).toInt()
        val avgB = (totalB / totalPixels).toInt()
        val avgTotal = (avgR + avgG + avgB) / 3f

        val warmthRatio = warmPixels.toFloat() / totalPixels
        val coldRatio = coldPixels.toFloat() / totalPixels
        val skinToneRatio = skinPixels.toFloat() / totalPixels
        val darkPixelRatio = darkPixels.toFloat() / totalPixels
        val highlightRatio = highlightPixels.toFloat() / totalPixels

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
            highlightRatio = highlightRatio
        )
    }

    /**
     * 肤色检测（YCbCr 色彩空间）
     * 基于肤色在 YCbCr 空间的典型范围
     */
    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        // RGB → YCbCr 转换
        val y = 16 + 0.257 * r + 0.504 * g + 0.098 * b
        val cb = 128 - 0.148 * r - 0.291 * g + 0.439 * b
        val cr = 128 + 0.439 * r - 0.368 * g - 0.071 * b

        // 肤色典型范围（适用于多种肤色）
        return y in 80.0..230.0 &&
               cb in 77.0..127.0 &&
               cr in 133.0..173.0
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
     * 已集成 ML Kit，支持实时人脸检测
     */
    private suspend fun detectFaces(bitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        try {
            // 创建 ML Kit 人脸检测器配置
            val options = com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()

            val faceDetector = com.google.mlkit.vision.face.FaceDetection.getClient(options)
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)

            // 异步检测人脸
            val faces = faceDetector.process(inputImage).await()
            
            // 关闭检测器释放资源
            faceDetector.close()
            
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
     */
    private fun computeEdgeDensity(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height

        // 缩小采样区域以提高效率
        val sampleSize = 100
        val scaledBitmap = if (width > sampleSize || height > sampleSize) {
            Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        } else {
            bitmap
        }

        val w = scaledBitmap.width
        val h = scaledBitmap.height
        var edgeCount = 0
        var totalPixels = 0

        // Sobel 算子
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = sobelX(scaledBitmap, x, y)
                val gy = sobelY(scaledBitmap, x, y)
                val gradient = sqrt(gx * gx + gy * gy)

                // 边缘阈值：梯度 > 50
                if (gradient > 50) edgeCount++
                totalPixels++
            }
        }

        return edgeCount.toFloat() / totalPixels
    }

    /**
     * Sobel X 方向算子
     */
    private fun sobelX(bitmap: Bitmap, x: Int, y: Int): Float {
        val p1 = getLuminance(bitmap.getPixel(x - 1, y - 1))
        val p2 = getLuminance(bitmap.getPixel(x, y - 1))
        val p3 = getLuminance(bitmap.getPixel(x + 1, y - 1))
        val p4 = getLuminance(bitmap.getPixel(x - 1, y))
        val p6 = getLuminance(bitmap.getPixel(x + 1, y))
        val p7 = getLuminance(bitmap.getPixel(x - 1, y + 1))
        val p8 = getLuminance(bitmap.getPixel(x, y + 1))
        val p9 = getLuminance(bitmap.getPixel(x + 1, y + 1))

        return (-p1 + p3 - 2 * p4 + 2 * p6 - p7 + p9).toFloat()
    }

    /**
     * Sobel Y 方向算子
     */
    private fun sobelY(bitmap: Bitmap, x: Int, y: Int): Float {
        val p1 = getLuminance(bitmap.getPixel(x - 1, y - 1))
        val p2 = getLuminance(bitmap.getPixel(x, y - 1))
        val p3 = getLuminance(bitmap.getPixel(x + 1, y - 1))
        val p4 = getLuminance(bitmap.getPixel(x - 1, y))
        val p6 = getLuminance(bitmap.getPixel(x + 1, y))
        val p7 = getLuminance(bitmap.getPixel(x - 1, y + 1))
        val p8 = getLuminance(bitmap.getPixel(x, y + 1))
        val p9 = getLuminance(bitmap.getPixel(x + 1, y + 1))

        return (-p1 - 2 * p2 - p3 + p7 + 2 * p8 + p9).toFloat()
    }

    private fun getLuminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
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

        // 暖色调 > 60% → 日落
        if (cp.warmthRatio > 0.55f) {
            val score = 0.55f + cp.warmthRatio * 0.3f
            votes.add(SceneCandidate("landscape-sunset", score.coerceAtMost(0.95f), "color"))
        }

        // 暖色调 35-55% → 美食
        if (cp.warmthRatio in 0.35f..0.55f) {
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
                if (scoreMap.containsKey(recentScene)) {
                    scoreMap[recentScene] = scoreMap[recentScene]!! + 0.15f
                }
            }
            ctx.preferredCategories.forEach { category ->
                ScenePresets.getScenesByCategory(category).forEach { scene ->
                    if (scoreMap.containsKey(scene.id)) {
                        scoreMap[scene.id] = scoreMap[scene.id]!! + 0.10f
                    }
                }
            }
        }

        // 排序并取Top-4
        val sorted = scoreMap.entries.sortedByDescending { it.value }
        val topScenes = sorted.take(4)

        // 获取场景预设
        val primaryScene = ScenePresets.getSceneById(topScenes.first().key)
            ?: ScenePresets.allScenes.first()

        val alternatives = topScenes.drop(1).mapNotNull { entry ->
            ScenePresets.getSceneById(entry.key)
        }

        // 计算置信度
        val totalScore = sorted.sumOf { it.value }
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
            "has_exif" to (exif != null).toFloat()
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