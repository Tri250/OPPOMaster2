package com.silas.omaster.ai.analyzer

import android.graphics.Bitmap
import com.silas.omaster.ai.model.SceneCategory
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ai.model.SceneProfileRepository

/**
 * 启发式场景分析器
 * 混合推理策略：颜色直方图 + EXIF + 亮度 + 纹理 + 人脸检测
 * 放弃随机，拥抱真实分析
 */
class HeuristicSceneAnalyzer {

    /**
     * 分析图片场景
     *
     * @param bitmap 待分析图片
     * @param exif EXIF 元数据（可选）
     * @return 综合分析结果
     */
    fun analyze(bitmap: Bitmap, exif: ExifData? = null): AnalysisResult {
        val startTime = System.currentTimeMillis()

        // 1. 颜色分析（采样策略：取中心 60% 区域，避免边缘干扰）
        val colorProfile = sampleColorProfile(bitmap, sampleRatio = 0.6f)

        // 2. 亮度分析
        val brightnessLevel = computeBrightnessLevel(bitmap)

        // 3. 人脸检测（简化实现，实际应使用 ML Kit）
        val faceCount = detectFacesSimple(bitmap)

        // 4. 纹理分析（边缘密度）
        val textureProfile = computeTextureProfile(bitmap)

        // 5. 多特征投票
        val candidates = mutableListOf<SceneCandidate>()

        // 颜色投票
        candidates.addAll(voteByColor(colorProfile))

        // 亮度投票
        candidates.addAll(voteByBrightness(brightnessLevel))

        // 人脸投票
        if (faceCount > 0) {
            candidates.addAll(voteByFace(faceCount))
        }

        // EXIF 投票
        if (exif != null) {
            candidates.addAll(voteByExif(exif, brightnessLevel))
        }

        // 纹理投票
        candidates.addAll(voteByTexture(textureProfile))

        // 6. 加权融合
        val fused = fuseVotes(candidates)

        // 7. 构建结果
        val analysisTime = System.currentTimeMillis() - startTime

        return AnalysisResult(
            primaryScene = fused.primary,
            confidence = fused.confidence,
            alternativeScenes = fused.alternatives,
            colorProfile = colorProfile,
            brightnessLevel = brightnessLevel,
            faceCount = faceCount,
            textureProfile = textureProfile,
            exifData = exif,
            analysisTimeMs = analysisTime,
            analysisDetails = buildAnalysisDetails(
                colorProfile, brightnessLevel, faceCount, textureProfile, candidates
            )
        )
    }

    // ==================== 颜色分析 ====================

    /**
     * 颜色直方图采样
     * 取中心区域避免边缘干扰
     */
    private fun sampleColorProfile(bitmap: Bitmap, sampleRatio: Float): ColorProfile {
        val width = bitmap.width
        val height = bitmap.height
        val startX = (width * (1 - sampleRatio) / 2).toInt()
        val startY = (height * (1 - sampleRatio) / 2).toInt()
        val sampleW = (width * sampleRatio).toInt()
        val sampleH = (height * sampleRatio).toInt()

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var warmPixels = 0
        var coolPixels = 0
        var totalPixels = 0

        // 色彩方差计算
        var varianceR = 0L
        var varianceG = 0L
        var varianceB = 0L

        // 采样步长（优化性能）
        val stepX = maxOf(4, sampleW / 100)
        val stepY = maxOf(4, sampleH / 100)

        for (y in startY until startY + sampleH step stepY) {
            for (x in startX until startX + sampleW step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                totalR += r
                totalG += g
                totalB += b

                // 暖色调判定：R > B + 20 且 R > G
                if (r > b + 20 && r > g) warmPixels++

                // 冷色调判定：B > R + 20 且 B > G
                if (b > r + 20 && b > g) coolPixels++

                totalPixels++
            }
        }

        val avgR = totalR / totalPixels
        val avgG = totalG / totalPixels
        val avgB = totalB / totalPixels
        val avgTotal = (avgR + avgG + avgB) / 3f

        val warmthRatio = warmPixels.toFloat() / totalPixels
        val coolRatio = coolPixels.toFloat() / totalPixels

        val greenDominance = avgG.toFloat() / avgTotal
        val blueDominance = avgB.toFloat() / avgTotal
        val redDominance = avgR.toFloat() / avgTotal

        // 计算色彩方差（简化）
        val colorVariance = kotlin.math.abs(avgR - avgG) + kotlin.math.abs(avgG - avgB) + kotlin.math.abs(avgB - avgR) / 3f

        // 确定主导色调
        val dominantTone = determineDominantTone(
            warmthRatio, coolRatio, greenDominance, blueDominance, avgTotal
        )

        return ColorProfile(
            avgRed = avgR.toInt(),
            avgGreen = avgG.toInt(),
            avgBlue = avgB.toInt(),
            warmthRatio = warmthRatio,
            coolRatio = coolRatio,
            greenDominance = greenDominance,
            blueDominance = blueDominance,
            redDominance = redDominance,
            colorVariance = colorVariance / 255f,
            dominantTone = dominantTone
        )
    }

    /**
     * 确定主导色调
     */
    private fun determineDominantTone(
        warmthRatio: Float,
        coolRatio: Float,
        greenDominance: Float,
        blueDominance: Float,
        avgBrightness: Float
    ): DominantTone {
        // 高调（极亮）
        if (avgBrightness > 200) return DominantTone.HIGH_KEY

        // 低调（极暗）
        if (avgBrightness < 50) return DominantTone.LOW_KEY

        // 暖色调主导
        if (warmthRatio > 0.55f) return DominantTone.WARM

        // 冷色调主导
        if (coolRatio > 0.55f) return DominantTone.COOL

        // 绿色主导
        if (greenDominance > 1.25f) return DominantTone.GREEN

        // 蓝色主导
        if (blueDominance > 1.2f) return DominantTone.BLUE

        return DominantTone.NEUTRAL
    }

    // ==================== 亮度分析 ====================

    /**
     * 计算亮度等级
     */
    private fun computeBrightnessLevel(bitmap: Bitmap): BrightnessLevel {
        val width = bitmap.width
        val height = bitmap.height

        var totalBrightness = 0L
        var totalPixels = 0

        // 采样步长
        val stepX = maxOf(4, width / 50)
        val stepY = maxOf(4, height / 50)

        // 暗部像素统计（亮度 < 50）
        var darkPixels = 0

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val brightness = (r + g + b) / 3
                totalBrightness += brightness
                totalPixels++

                if (brightness < 50) darkPixels++
            }
        }

        val avgBrightness = totalBrightness / totalPixels
        val darkRatio = darkPixels.toFloat() / totalPixels

        // 暗部占比 > 70% 判定为夜景
        if (darkRatio > 0.70f) {
            return BrightnessLevel.VERY_DARK
        }

        return BrightnessLevel.fromValue(avgBrightness.toInt())
    }

    // ==================== 人脸检测 ====================

    /**
     * 简化人脸检测（实际应使用 ML Kit）
     * 基于肤色检测的简化实现
     */
    private fun detectFacesSimple(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height

        // 采样区域（中心区域）
        val startX = width / 4
        val startY = height / 4
        val sampleW = width / 2
        val sampleH = height / 2

        var skinPixels = 0
        var totalPixels = 0

        val stepX = maxOf(4, sampleW / 50)
        val stepY = maxOf(4, sampleH / 50)

        for (y in startY until startY + sampleH step stepY) {
            for (x in startX until startX + sampleW step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // YCbCr 色彩空间肤色检测（简化）
                // 肤色范围：R > 95, G > 40, B > 20, R > G, R > B, |R-G| < 15
                if (isSkinColor(r, g, b)) {
                    skinPixels++
                }
                totalPixels++
            }
        }

        val skinRatio = skinPixels.toFloat() / totalPixels

        // 肤色占比 > 15% 判定有人脸
        return if (skinRatio > 0.15f) {
            // 简化：根据肤色占比估算人脸数量
            when {
                skinRatio > 0.40f -> 2 // 多人
                skinRatio > 0.25f -> 1 // 单人
                else -> 1 // 至少一人
            }
        } else {
            0
        }
    }

    /**
     * 肤色判定（YCbCr 色彩空间简化）
     */
    private fun isSkinColor(r: Int, g: Int, b: Int): Boolean {
        // 基本肤色范围
        if (r < 95 || g < 40 || b < 20) return false
        if (r <= g || r <= b) return false
        if (kotlin.math.abs(r - g) > 15) return false

        // 扩展肤色范围（适应不同肤色）
        val brightness = r + g + b
        if (brightness < 200 || brightness > 600) return false

        return true
    }

    // ==================== 纹理分析 ====================

    /**
     * 计算纹理特征
     */
    private fun computeTextureProfile(bitmap: Bitmap): TextureProfile {
        val width = bitmap.width
        val height = bitmap.height

        var edgePixels = 0
        var totalPixels = 0
        var sharpnessScore = 0f

        val stepX = maxOf(2, width / 100)
        val stepY = maxOf(2, height / 100)

        for (y in 1 until height - 1 step stepY) {
            for (x in 1 until width - 1 step stepX) {
                val current = getBrightness(bitmap, x, y)
                val right = getBrightness(bitmap, x + 1, y)
                val below = getBrightness(bitmap, x, y + 1)

                // 边缘检测（简化 Sobel）
                val edgeX = kotlin.math.abs(current - right)
                val edgeY = kotlin.math.abs(current - below)
                val edgeStrength = edgeX + edgeY

                if (edgeStrength > 30) {
                    edgePixels++
                    sharpnessScore += edgeStrength
                }
                totalPixels++
            }
        }

        val edgeDensity = edgePixels.toFloat() / totalPixels
        val avgSharpness = sharpnessScore / edgePixels.coerceAtLeast(1)

        // 纹理复杂度
        val complexity = edgeDensity * avgSharpness / 100f

        // 纹理类型判定
        val textureType = when {
            edgeDensity < 0.08f -> TextureType.SMOOTH
            edgeDensity < 0.12f -> TextureType.SOFT_GRADIENT
            edgeDensity < 0.18f -> TextureType.NORMAL
            edgeDensity < 0.25f -> TextureType.DETAILED
            else -> TextureType.HIGH_CONTRAST
        }

        return TextureProfile(
            edgeDensity = edgeDensity,
            complexity = complexity,
            sharpnessScore = avgSharpness / 255f,
            textureType = textureType
        )
    }

    /**
     * 获取像素亮度
     */
    private fun getBrightness(bitmap: Bitmap, x: Int, y: Int): Int {
        val pixel = bitmap.getPixel(x, y)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3
    }

    // ==================== 多特征投票 ====================

    /**
     * 颜色→场景投票
     */
    private fun voteByColor(cp: ColorProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // 绿色主导 → 森林/自然
        if (cp.greenDominance > 1.25f) {
            val confidence = 0.70f * cp.greenDominance.coerceAtMost(1.5f)
            votes.add(SceneCandidate("landscape-forest", confidence, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("macro-flower", confidence * 0.6f, VoteSource.COLOR_ANALYSIS))
        }

        // 蓝色主导 → 天空/海滩
        if (cp.blueDominance > 1.2f) {
            val confidence = 0.65f * cp.blueDominance.coerceAtMost(1.5f)
            votes.add(SceneCandidate("landscape-blue-sky", confidence, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("landscape-beach", confidence * 0.7f, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("special-aquarium", confidence * 0.5f, VoteSource.COLOR_ANALYSIS))
        }

        // 暖色调 > 55% → 日落/美食
        if (cp.warmthRatio > 0.55f) {
            val confidence = 0.55f + cp.warmthRatio * 0.3f
            votes.add(SceneCandidate("landscape-sunset", confidence, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("landscape-autumn", confidence * 0.7f, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("food-restaurant", confidence * 0.6f, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("urban-cafe", confidence * 0.5f, VoteSource.COLOR_ANALYSIS))
        }

        // 冷色调 > 55% → 雪景/夜景
        if (cp.coolRatio > 0.55f) {
            val confidence = 0.55f + cp.coolRatio * 0.3f
            votes.add(SceneCandidate("landscape-snow", confidence, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("night-city", confidence * 0.6f, VoteSource.COLOR_ANALYSIS))
        }

        // 高调（极亮） → 风景/海滩
        if (cp.dominantTone == DominantTone.HIGH_KEY) {
            votes.add(SceneCandidate("landscape-standard", 0.60f, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("landscape-beach", 0.55f, VoteSource.COLOR_ANALYSIS))
        }

        // 低调（极暗） → 夜景/烛光
        if (cp.dominantTone == DominantTone.LOW_KEY) {
            votes.add(SceneCandidate("night-city", 0.65f, VoteSource.COLOR_ANALYSIS))
            votes.add(SceneCandidate("night-candlelight", 0.55f, VoteSource.COLOR_ANALYSIS))
        }

        return votes
    }

    /**
     * 亮度→场景投票
     */
    private fun voteByBrightness(level: BrightnessLevel): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (level) {
            BrightnessLevel.VERY_DARK -> {
                votes.add(SceneCandidate("night-city", 0.75f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("night-starry", 0.60f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("night-neon", 0.55f, VoteSource.BRIGHTNESS_ANALYSIS))
            }
            BrightnessLevel.DARK -> {
                votes.add(SceneCandidate("night-city", 0.50f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("portrait-bw", 0.45f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("urban-museum", 0.40f, VoteSource.BRIGHTNESS_ANALYSIS))
            }
            BrightnessLevel.NORMAL -> {
                // 正常亮度不提供强投票
                votes.add(SceneCandidate("portrait-standard", 0.30f, VoteSource.BRIGHTNESS_ANALYSIS))
            }
            BrightnessLevel.BRIGHT -> {
                votes.add(SceneCandidate("landscape-standard", 0.50f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("portrait-standard", 0.45f, VoteSource.BRIGHTNESS_ANALYSIS))
            }
            BrightnessLevel.VERY_BRIGHT -> {
                votes.add(SceneCandidate("landscape-blue-sky", 0.60f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("landscape-beach", 0.55f, VoteSource.BRIGHTNESS_ANALYSIS))
                votes.add(SceneCandidate("landscape-snow", 0.50f, VoteSource.BRIGHTNESS_ANALYSIS))
            }
        }

        return votes
    }

    /**
     * 人脸→场景投票
     */
    private fun voteByFace(faceCount: Int): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (faceCount) {
            1 -> {
                votes.add(SceneCandidate("portrait-standard", 0.80f, VoteSource.FACE_DETECTION))
                votes.add(SceneCandidate("portrait-backlit", 0.60f, VoteSource.FACE_DETECTION))
                votes.add(SceneCandidate("portrait-children", 0.50f, VoteSource.FACE_DETECTION))
            }
            2 -> {
                votes.add(SceneCandidate("portrait-group", 0.75f, VoteSource.FACE_DETECTION))
                votes.add(SceneCandidate("special-wedding", 0.55f, VoteSource.FACE_DETECTION))
            }
            else -> {
                votes.add(SceneCandidate("portrait-group", 0.70f, VoteSource.FACE_DETECTION))
                votes.add(SceneCandidate("special-party", 0.50f, VoteSource.FACE_DETECTION))
            }
        }

        return votes
    }

    /**
     * EXIF→场景投票
     */
    private fun voteByExif(exif: ExifData, brightness: BrightnessLevel): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        // 高 ISO → 暗光/夜景
        if (exif.isHighISO) {
            votes.add(SceneCandidate("night-city", 0.70f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("night-candlelight", 0.55f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("special-concert", 0.50f, VoteSource.EXIF_DATA))
        }

        // 闪光灯开启 → 室内/夜景人像
        if (exif.isFlashUsed) {
            votes.add(SceneCandidate("portrait-standard", 0.60f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("night-city", 0.50f, VoteSource.EXIF_DATA))
        }

        // 微距拍摄
        if (exif.isMacro) {
            votes.add(SceneCandidate("macro-flower", 0.85f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("macro-insect", 0.70f, VoteSource.EXIF_DATA))
        }

        // 日落时段
        if (exif.isSunsetTime) {
            votes.add(SceneCandidate("landscape-sunset", 0.80f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("portrait-backlit", 0.60f, VoteSource.EXIF_DATA))
        }

        // 夜景时段
        if (exif.isNightTime) {
            votes.add(SceneCandidate("night-city", 0.75f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("night-neon", 0.60f, VoteSource.EXIF_DATA))
        }

        // 长焦镜头 → 人像/建筑
        if (exif.isTelephoto) {
            votes.add(SceneCandidate("portrait-standard", 0.65f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("urban-architecture", 0.55f, VoteSource.EXIF_DATA))
        }

        // 广角镜头 → 风景/建筑
        if (exif.isWideAngle) {
            votes.add(SceneCandidate("landscape-standard", 0.60f, VoteSource.EXIF_DATA))
            votes.add(SceneCandidate("urban-architecture", 0.55f, VoteSource.EXIF_DATA))
        }

        return votes
    }

    /**
     * 纹理→场景投票
     */
    private fun voteByTexture(texture: TextureProfile): List<SceneCandidate> {
        val votes = mutableListOf<SceneCandidate>()

        when (texture.textureType) {
            TextureType.HIGH_CONTRAST -> {
                votes.add(SceneCandidate("urban-architecture", 0.65f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("urban-street", 0.60f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("portrait-bw", 0.55f, VoteSource.TEXTURE_ANALYSIS))
            }
            TextureType.DETAILED -> {
                votes.add(SceneCandidate("macro-flower", 0.60f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("macro-insect", 0.55f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("landscape-standard", 0.45f, VoteSource.TEXTURE_ANALYSIS))
            }
            TextureType.SOFT_GRADIENT -> {
                votes.add(SceneCandidate("portrait-backlit", 0.55f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("landscape-sunset", 0.50f, VoteSource.TEXTURE_ANALYSIS))
            }
            TextureType.SMOOTH -> {
                votes.add(SceneCandidate("portrait-standard", 0.50f, VoteSource.TEXTURE_ANALYSIS))
                votes.add(SceneCandidate("food-dessert", 0.45f, VoteSource.TEXTURE_ANALYSIS))
            }
            TextureType.NORMAL -> {
                // 正常纹理不提供强投票
            }
        }

        return votes
    }

    // ==================== 加权融合 ====================

    /**
     * 加权融合投票结果
     */
    private fun fuseVotes(candidates: List<SceneCandidate>): FusedResult {
        // 按场景 ID 分组并加权求和
        val sceneScores = candidates
            .groupBy { it.sceneId }
            .mapValues { group ->
                group.value.sumOf { it.weightedConfidence.toDouble() }.toFloat()
            }

        // 按分数排序
        val sortedScenes = sceneScores.entries.sortedByDescending { it.value }

        // 获取主场景和备选场景
        val primaryId = sortedScenes.firstOrNull()?.key ?: "portrait-standard"
        val primaryScore = sortedScenes.firstOrNull()?.value ?: 0.5f

        val primaryProfile = SceneProfileRepository.getProfileById(primaryId)
            ?: SceneProfileRepository.allProfiles.first()

        val alternativeProfiles = sortedScenes
            .drop(1)
            .take(3)
            .mapNotNull { entry ->
                SceneProfileRepository.getProfileById(entry.key)
            }

        // 置信度归一化（限制在 0-1）
        val normalizedConfidence = primaryScore.coerceIn(0f, 1f)

        return FusedResult(
            primary = primaryProfile.copy(confidence = normalizedConfidence),
            confidence = normalizedConfidence,
            alternatives = alternativeProfiles.map { it.copy(confidence = 0.5f) }
        )
    }

    /**
     * 构建分析详情
     */
    private fun buildAnalysisDetails(
        colorProfile: ColorProfile,
        brightnessLevel: BrightnessLevel,
        faceCount: Int,
        textureProfile: TextureProfile?,
        candidates: List<SceneCandidate>
    ): Map<String, Any> {
        return mapOf(
            "avgRed" to colorProfile.avgRed,
            "avgGreen" to colorProfile.avgGreen,
            "avgBlue" to colorProfile.avgBlue,
            "warmthRatio" to colorProfile.warmthRatio,
            "coolRatio" to colorProfile.coolRatio,
            "greenDominance" to colorProfile.greenDominance,
            "blueDominance" to colorProfile.blueDominance,
            "brightnessLevel" to brightnessLevel.value,
            "faceCount" to faceCount,
            "edgeDensity" to (textureProfile?.edgeDensity ?: 0f),
            "textureType" to (textureProfile?.textureType?.displayName ?: "未知"),
            "candidateCount" to candidates.size,
            "topCandidates" to candidates.sortedByDescending { it.weightedConfidence }.take(5)
                .map { "${it.sceneId}:${it.weightedConfidence}" }
        )
    }

    /**
     * 融合结果
     */
    private data class FusedResult(
        val primary: SceneProfile,
        val confidence: Float,
        val alternatives: List<SceneProfile>
    )
}