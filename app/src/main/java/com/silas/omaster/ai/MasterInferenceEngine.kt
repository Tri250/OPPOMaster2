package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Layer 2: 大师推理 (Master Inference)
 * 图像分析与参数推理引擎
 *
 * 功能：
 * - 颜色直方图分析
 * - EXIF 元数据提取
 * - 亮度分布分析
 * - 人脸检测（基于 Google ML Kit FaceDetection）
 * - 多策略融合置信度
 * - 场景→哈苏参数映射
 *
 * 已修复：使用新版 SceneProfile 数据模型，与 ScenePresets 对齐
 * 已修复：人脸检测改用 ML Kit 真实检测结果，不再使用模拟数据
 */
class MasterInferenceEngine private constructor(context: Context) {

    private val context = context.applicationContext
    private val sceneAnalyzer = HeuristicSceneAnalyzer.getInstance(context)
    private val sceneMapping = SceneToHasselbladMapping

    // ML Kit 人脸检测器（全局单例，使用 ACCURATE 模式以获得人脸详细分类）
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * 分析图片并生成 SceneProfile
     * 端到端的大师工作流入口
     *
     * 修复：使用 HeuristicSceneAnalyzer 获取真实场景识别结果
     * 修复：人脸数据改用 ML Kit FaceDetection 真实检测
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

        // 调用 ML Kit 真实人脸检测
        val faceData = runCatching {
            detectFaces(bitmap)
        }.getOrElse {
            // 真实检测失败时使用空人脸数据，不构造模拟人脸
            FaceData(faces = emptyList())
        }

        // 更新扩展数据
        sceneProfile.copy(
            exifData = exifData,
            histogramData = convertToHistogramData(analysisResult.colorProfile),
            faceData = faceData,
            confidence = analysisResult.confidence
        )
    }

    /**
     * 使用 ML Kit 真实人脸检测
     *
     * 将 Google Play Services Task 桥接到 suspend 函数，
     * 把每个 ML Kit Face 转换为项目内的 FaceInfo。
     */
    private suspend fun detectFaces(bitmap: Bitmap): FaceData =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = faceDetector.process(inputImage)
            task.addOnSuccessListener { faces: List<Face> ->
                if (continuation.isActive) {
                    val faceInfos = faces.map { face -> face.toFaceInfo(bitmap) }
                    continuation.resume(FaceData(faces = faceInfos))
                }
            }
            task.addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
            continuation.invokeOnCancellation {
                // 协程被取消时尝试关闭 task（ML Kit 任务本身不支持取消，仅取消监听）
            }
        }

    /**
     * 将 ML Kit Face 转为项目内的 FaceInfo
     *
     * 坐标归一化：ML Kit 返回的是像素坐标 Rect，除以图片宽高即可。
     */
    private fun Face.toFaceInfo(bitmap: Bitmap): FaceInfo {
        val w = bitmap.width.coerceAtLeast(1).toFloat()
        val h = bitmap.height.coerceAtLeast(1).toFloat()
        val pixelBounds: Rect = boundingBox
        val normalized = RectData(
            left = (pixelBounds.left / w).coerceIn(0f, 1f),
            top = (pixelBounds.top / h).coerceIn(0f, 1f),
            right = (pixelBounds.right / w).coerceIn(0f, 1f),
            bottom = (pixelBounds.bottom / h).coerceIn(0f, 1f)
        )
        // ML Kit 自身对每张人脸都返回 1.0 置信度；
        // 若有右眼开闭概率，使用 0.4 + 0.6*平均开眼概率作为可读性更好的置信度
        val leftProb = leftEyeOpenProbability ?: 0.5f
        val rightProb = rightEyeOpenProbability ?: 0.5f
        val smileProb = smilingProbability ?: 0f
        val confidence = 0.4f + 0.3f * leftProb + 0.3f * rightProb
        return FaceInfo(
            bounds = normalized,
            confidence = confidence,
            hasSmile = smileProb > 0.5f,
            leftEyeOpen = leftProb > 0.5f,
            rightEyeOpen = rightProb > 0.5f
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

    /**
     * 释放 ML Kit 资源
     */
    fun release() {
        try {
            faceDetector.close()
        } catch (_: Exception) {
            // 关闭异常忽略
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
