package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
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
 * 
 * 已修复：使用新版 SceneProfile 数据模型，与 ScenePresets 对齐
 */
class MasterInferenceEngine private constructor(context: Context) {

    private val context = context.applicationContext
    private val sceneAnalyzer = HeuristicSceneAnalyzer.getInstance(context)
    private val sceneMapping = SceneToHasselbladMapping()

    /**
     * 分析图片并生成 SceneProfile
     * 端到端的大师工作流入口
     * 
     * 修复：使用 HeuristicSceneAnalyzer 获取真实场景识别结果
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        imagePath: String? = null
    ): SceneProfile = withContext(Dispatchers.Default) {
        // 提取EXIF数据
        val exifData = imagePath?.let { extractExifData(it) }

        // 使用启发式分析器进行场景识别（核心修复）
        val analysisResult = sceneAnalyzer.analyze(
            bitmap = bitmap,
            exif = exifData,
            userContext = null
        )

        // 获取主场景的完整画像
        val sceneProfile = analysisResult.primaryScene

        // 更新扩展数据
        sceneProfile.copy(
            exifData = exifData,
            histogramData = convertToHistogramData(analysisResult.colorProfile),
            faceData = FaceData(
                faces = if (analysisResult.faceCount > 0) {
                    // 创建模拟人脸数据（实际应使用ML Kit检测结果）
                    List(analysisResult.faceCount) { index ->
                        FaceInfo(
                            bounds = RectData(0.3f, 0.3f, 0.7f, 0.7f),
                            confidence = 0.85f,
                            hasSmile = false,
                            leftEyeOpen = true,
                            rightEyeOpen = true
                        )
                    }
                } else emptyList()
            ),
            confidence = analysisResult.confidence
        )
    }

    /**
     * 快速分析 - 仅返回场景ID和置信度
     */
    suspend fun quickAnalyze(bitmap: Bitmap): Pair<String, Float> = withContext(Dispatchers.Default) {
        val result = sceneAnalyzer.analyze(bitmap)
        Pair(result.primaryScene.id, result.confidence)
    }

    /**
     * 获取推荐胶片列表
     */
    fun getRecommendedFilms(sceneId: String): List<FilmPreset> {
        return sceneMapping.getRecommendedFilms(sceneId)
    }

    /**
     * 获取哈苏大师参数
     */
    fun getHasselbladParams(sceneId: String): HasselbladParams {
        return sceneMapping.getParams(sceneId)
    }

    /**
     * 获取大师拍摄建议
     */
    fun getMasterTips(sceneId: String): List<String> {
        return sceneMapping.getMasterTips(sceneId)
    }

    /**
     * 获取参数调整建议
     */
    fun getParamAdjustmentAdvice(
        currentParams: HasselbladParams,
        targetSceneId: String
    ): List<SceneToHasselbladMapping.ParamAdjustment> {
        return sceneMapping.getParamAdjustmentAdvice(currentParams, targetSceneId)
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
     * 将 ColorProfile 转换为 HistogramData
     */
    private fun convertToHistogramData(colorProfile: HeuristicSceneAnalyzer.ColorProfile): HistogramData {
        // 基于颜色画像生成简化的直方图数据
        val luminance = IntArray(256)
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        // 根据平均颜色值填充直方图中心区域
        val avgLuma = ((0.2126 * colorProfile.avgRed + 
                       0.7152 * colorProfile.avgGreen + 
                       0.0722 * colorProfile.avgBlue)).toInt().coerceIn(0, 255)
        
        luminance[avgLuma] = 1000
        red[colorProfile.avgRed] = 1000
        green[colorProfile.avgGreen] = 1000
        blue[colorProfile.avgBlue] = 1000

        return HistogramData(
            luminance = luminance,
            red = red,
            green = green,
            blue = blue,
            meanLuminance = avgLuma.toFloat(),
            shadowClipping = colorProfile.darkPixelRatio > 0.7f,
            highlightClipping = colorProfile.highlightRatio > 0.3f
        )
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
