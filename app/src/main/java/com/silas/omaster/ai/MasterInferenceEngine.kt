package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Layer 2: 大师推理 (Master Inference)
 * 图像分析与参数推理引擎
 * 
 * 功能：
 * - 颜色直方图分析
 * - EXIF 元数据提取
 * - 亮度分布分析
 * - 人脸检测
 * - 多策略融合置信度
 * - 场景→哈苏参数映射
 */
class MasterInferenceEngine private constructor(context: Context) {

    /**
     * 分析图片并生成 SceneProfile
     * 端到端的大师工作流入口
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        imagePath: String? = null
    ): SceneProfile = withContext(Dispatchers.Default) {
        // 并行执行各项分析
        val histogramData = analyzeHistogram(bitmap)
        val exifData = imagePath?.let { extractExifData(it) }
        val faceData = detectFaces(bitmap)

        // 场景识别
        val sceneHierarchy = inferSceneHierarchy(bitmap, histogramData, faceData)

        // 哈苏参数映射
        val hasselbladParams = mapToHasselbladParams(sceneHierarchy, histogramData, exifData)

        // 胶片配方推荐
        val filmRecipe = recommendFilmRecipe(sceneHierarchy, histogramData)

        // 计算综合置信度
        val confidence = calculateConfidence(sceneHierarchy, histogramData, faceData)

        SceneProfile(
            id = System.currentTimeMillis().toString(),
            sceneHierarchy = sceneHierarchy,
            hasselbladParams = hasselbladParams,
            filmRecipe = filmRecipe,
            exifData = exifData,
            histogramData = histogramData,
            faceData = faceData,
            confidence = confidence
        )
    }

    /**
     * 直方图分析
     */
    private fun analyzeHistogram(bitmap: Bitmap): HistogramData {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val luminanceHist = IntArray(256)
        val redHist = IntArray(256)
        val greenHist = IntArray(256)
        val blueHist = IntArray(256)

        var totalLuminance = 0L
        var shadowPixels = 0
        var highlightPixels = 0

        for (y in 0 until height step 4) { // 采样优化
            for (x in 0 until width step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // 计算亮度 (Rec. 709)
                val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
                val lumaClamped = luminance.coerceIn(0, 255)

                luminanceHist[lumaClamped]++
                redHist[r]++
                greenHist[g]++
                blueHist[b]++

                totalLuminance += lumaClamped

                // 检测阴影/高光裁剪
                if (lumaClamped < 10) shadowPixels++
                if (lumaClamped > 245) highlightPixels++
            }
        }

        val sampleCount = ((width / 4) * (height / 4)).coerceAtLeast(1)
        val meanLuminance = totalLuminance.toFloat() / sampleCount

        return HistogramData(
            luminance = luminanceHist,
            red = redHist,
            green = greenHist,
            blue = blueHist,
            meanLuminance = meanLuminance,
            shadowClipping = shadowPixels > sampleCount * 0.05,
            highlightClipping = highlightPixels > sampleCount * 0.05
        )
    }

    /**
     * EXIF 元数据提取
     */
    private fun extractExifData(imagePath: String): ExifData? {
        return try {
            val exif = ExifInterface(imagePath)
            
            ExifData(
                cameraModel = exif.getAttribute(ExifInterface.TAG_MAKE)?.let { 
                    "$it ${exif.getAttribute(ExifInterface.TAG_MODEL)}" 
                },
                lensModel = exif.getAttribute("LensModel"),
                focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                    parseRational(it)
                },
                fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
                    parseRational(it)
                },
                exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.toIntOrNull(),
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME),
                gpsLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let {
                    parseGpsCoordinate(it, exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
                },
                gpsLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let {
                    parseGpsCoordinate(it, exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF))
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 人脸检测 (简化版，实际应使用 ML Kit)
     */
    private fun detectFaces(bitmap: Bitmap): FaceData {
        // 实际项目中应使用 ML Kit Face Detection
        // 这里返回模拟数据
        return FaceData(
            faces = emptyList() // 模拟无检测到人脸
        )
    }

    /**
     * 场景层次推理
     * 基于直方图、人脸检测等特征推理三级场景
     */
    private fun inferSceneHierarchy(
        bitmap: Bitmap,
        histogram: HistogramData,
        faceData: FaceData
    ): SceneHierarchy {
        // 基于亮度分布判断
        val meanLuma = histogram.meanLuminance
        val hasFaces = faceData.hasFace

        // 一级场景判断
        val primary = when {
            hasFaces -> PrimaryScene.PORTRAIT
            meanLuma > 100 && histogram.highlightClipping -> PrimaryScene.LANDSCAPE
            meanLuma < 50 -> PrimaryScene.NIGHT
            else -> PrimaryScene.STREET
        }

        // 二级场景判断
        val secondary = when (primary) {
            PrimaryScene.PORTRAIT -> {
                when {
                    meanLuma < 60 -> SecondaryScene.NIGHT_PORTRAIT
                    meanLuma > 120 -> SecondaryScene.OUTDOOR_PORTRAIT
                    else -> SecondaryScene.INDOOR_PORTRAIT
                }
            }
            PrimaryScene.LANDSCAPE -> {
                when {
                    histogram.highlightClipping -> SecondaryScene.NATURAL_LANDSCAPE
                    else -> SecondaryScene.URBAN_LANDSCAPE
                }
            }
            else -> SecondaryScene.URBAN_STREET
        }

        // 三级场景判断
        val fine = when (secondary) {
            SecondaryScene.OUTDOOR_PORTRAIT -> {
                when {
                    histogram.highlightClipping && meanLuma > 150 -> FineScene.GOLDEN_HOUR
                    histogram.highlightClipping -> FineScene.BACKLIGHT_PORTRAIT
                    else -> FineScene.OVERCAST_PORTRAIT
                }
            }
            SecondaryScene.NATURAL_LANDSCAPE -> {
                when {
                    meanLuma < 80 -> FineScene.BLUE_HOUR
                    histogram.shadowClipping -> FineScene.SUNRISE_SUNSET
                    else -> FineScene.MISTY
                }
            }
            else -> FineScene.CITY_NIGHT
        }

        return SceneHierarchy(primary, secondary, fine)
    }

    /**
     * 场景到哈苏参数映射
     * 基于场景层次和直方图数据生成推荐参数
     */
    private fun mapToHasselbladParams(
        sceneHierarchy: SceneHierarchy,
        histogram: HistogramData,
        exifData: ExifData?
    ): HasselbladParams {
        val baseParams = when (sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> HasselbladParams(
                iso = 100,
                aperture = 1.8f,
                shutterSpeed = "1/125",
                saturation = -5,
                contrast = -10,
                clarity = 15,
                colorProfile = HasselbladColorProfile.PORTRAIT
            )
            PrimaryScene.LANDSCAPE -> HasselbladParams(
                iso = 50,
                aperture = 8.0f,
                shutterSpeed = "1/60",
                saturation = 10,
                contrast = 5,
                clarity = 25,
                colorProfile = HasselbladColorProfile.LANDSCAPE
            )
            PrimaryScene.NIGHT -> HasselbladParams(
                iso = 1600,
                aperture = 1.6f,
                shutterSpeed = "1/15",
                saturation = 5,
                contrast = 10,
                clarity = 20,
                colorProfile = HasselbladColorProfile.HNCS
            )
            else -> HasselbladParams()
        }

        // 根据直方图微调
        return baseParams.copy(
            highlights = if (histogram.highlightClipping) -20 else 0,
            shadows = if (histogram.shadowClipping) 20 else 0,
            brightness = when {
                histogram.meanLuminance < 80 -> 10
                histogram.meanLuminance > 180 -> -10
                else -> 0
            }
        )
    }

    /**
     * 胶片配方推荐
     */
    private fun recommendFilmRecipe(
        sceneHierarchy: SceneHierarchy,
        histogram: HistogramData
    ): FilmRecipe? {
        return when (sceneHierarchy.primary) {
            PrimaryScene.PORTRAIT -> FilmRecipe(
                filmStock = FilmStock.KODAK_PORTRA_400,
                pushPull = 0
            )
            PrimaryScene.LANDSCAPE -> FilmRecipe(
                filmStock = FilmStock.KODAK_EKTAR_100,
                pushPull = 0
            )
            PrimaryScene.STREET -> FilmRecipe(
                filmStock = FilmStock.KODAK_TRI_X_400,
                pushPull = 0
            )
            else -> FilmRecipe(
                filmStock = FilmStock.FUJI_PRO_400H,
                pushPull = 0
            )
        }
    }

    /**
     * 计算综合置信度
     * 多策略融合
     */
    private fun calculateConfidence(
        sceneHierarchy: SceneHierarchy,
        histogram: HistogramData,
        faceData: FaceData
    ): Float {
        var confidence = 0.75f

        // 基于直方图分布质量
        val histogramQuality = calculateHistogramQuality(histogram)
        confidence += histogramQuality * 0.15f

        // 基于人脸检测置信度
        if (faceData.hasFace) {
            val faceConfidence = faceData.faces.map { it.confidence }.average().toFloat()
            confidence += faceConfidence * 0.1f
        }

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * 计算直方图质量分数
     */
    private fun calculateHistogramQuality(histogram: HistogramData): Float {
        // 检查直方图是否分布均匀（避免过曝/欠曝）
        val total = histogram.luminance.sum()
        if (total == 0) return 0f

        // 计算标准差
        val mean = histogram.meanLuminance
        var variance = 0.0
        for (i in histogram.luminance.indices) {
            variance += (i - mean).pow(2) * histogram.luminance[i]
        }
        variance /= total
        val stdDev = sqrt(variance)

        // 标准差在 40-80 之间认为是良好分布
        return when {
            stdDev < 20 -> 0.3f // 对比度过低
            stdDev in 40.0..80.0 -> 1.0f // 理想分布
            stdDev > 100 -> 0.6f // 对比度过高
            else -> 0.8f
        }
    }

    /**
     * 解析有理数 (如 "35/10" -> 3.5)
     */
    private fun parseRational(rational: String): Float? {
        return try {
            val parts = rational.split("/")
            if (parts.size == 2) {
                parts[0].toFloat() / parts[1].toFloat()
            } else {
                rational.toFloatOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析GPS坐标
     */
    private fun parseGpsCoordinate(coordinate: String, ref: String?): Double? {
        return try {
            val parts = coordinate.split(", ")
            if (parts.size == 3) {
                val degrees = parseRational(parts[0]) ?: 0f
                val minutes = parseRational(parts[1]) ?: 0f
                val seconds = parseRational(parts[2]) ?: 0f
                var result = degrees + minutes / 60 + seconds / 3600
                if (ref == "S" || ref == "W") {
                    result = -result
                }
                result.toDouble()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: MasterInferenceEngine? = null

        fun getInstance(context: Context): MasterInferenceEngine {
            return instance ?: synchronized(this) {
                instance ?: MasterInferenceEngine(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}

/**
 * 推理结果回调
 */
interface MasterInferenceCallback {
    fun onAnalysisComplete(profile: SceneProfile)
    fun onAnalysisFailed(error: String)
    fun onProgressUpdate(progress: Float)
}
