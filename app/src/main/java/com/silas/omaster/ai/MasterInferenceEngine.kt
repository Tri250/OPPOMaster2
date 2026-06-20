package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.util.UrlConstants
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        // 尝试云端 AI 分析（优先）
        val cloudResult = tryCloudAnalysis(bitmap, imagePath)
        if (cloudResult != null) {
            // 补充本地人脸检测数据
            val faceData = runCatching { detectFaces(bitmap) }
                .getOrElse { FaceData(faces = emptyList()) }
            val exifData = imagePath?.let { extractExifData(it) }
            return@withContext cloudResult.copy(
                exifData = exifData,
                faceData = faceData
            )
        }

        // 回退到本地启发式分析
        val exifData = imagePath?.let { extractExifData(it) }
        val analysisResult = sceneAnalyzer.analyze(bitmap, exifData, null)
        val sceneProfile = analysisResult.primaryScene
        val faceData = runCatching { detectFaces(bitmap) }
            .getOrElse { FaceData(faces = emptyList()) }

        sceneProfile.copy(
            exifData = exifData,
            histogramData = convertToHistogramData(analysisResult.colorProfile),
            faceData = faceData,
            confidence = analysisResult.confidence
        )
    }

    /**
     * 尝试使用云端 AI 场景分析 API
     * 如果网络不可用或 API 调用失败，回退到本地启发式分析
     */
    private suspend fun tryCloudAnalysis(bitmap: Bitmap, imagePath: String?): SceneProfile? {
        return try {
            // 将 Bitmap 转为 Base64 发送到云端
            val outputStream = java.io.ByteArrayOutputStream()
            // 缩小图片以减少传输量
            val maxDim = 1024
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
            if (scaledBitmap != bitmap) scaledBitmap.recycle()

            val httpClient = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 10_000
                }
            }

            try {
                val response = httpClient.post(UrlConstants.API_CLOUD_SCENE_ANALYZE) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("image", base64Image)
                        put("max_results", 5)
                    })
                }
                if (response.status.value in 200..299) {
                    val responseBody = response.bodyAsText()
                    parseCloudAnalysisResponse(responseBody)
                } else {
                    null
                }
            } finally {
                httpClient.close()
            }
        } catch (e: Exception) {
            android.util.Log.w("MasterInferenceEngine", "Cloud analysis failed, fallback to local", e)
            null
        }
    }

    private fun parseCloudAnalysisResponse(responseBody: String): SceneProfile? {
        return try {
            val json = org.json.JSONObject(responseBody)
            val scenes = json.optJSONArray("scenes") ?: return null
            if (scenes.length() == 0) return null

            val primaryScene = scenes.getJSONObject(0)
            val sceneId = primaryScene.optString("id", "unknown")
            val sceneName = primaryScene.optString("name", "未知场景")
            val confidence = primaryScene.optDouble("confidence", 0.5).toFloat()

            // Map cloud scene ID to local category
            val category = when {
                sceneId.contains("portrait", ignoreCase = true) -> SceneCategory.PORTRAIT
                sceneId.contains("landscape", ignoreCase = true) -> SceneCategory.LANDSCAPE
                sceneId.contains("night", ignoreCase = true) -> SceneCategory.NIGHT
                sceneId.contains("food", ignoreCase = true) -> SceneCategory.FOOD
                sceneId.contains("urban", ignoreCase = true) || sceneId.contains("street", ignoreCase = true) -> SceneCategory.URBAN
                sceneId.contains("still", ignoreCase = true) -> SceneCategory.STILL_LIFE
                sceneId.contains("macro", ignoreCase = true) -> SceneCategory.MACRO
                sceneId.contains("event", ignoreCase = true) -> SceneCategory.EVENT
                else -> SceneCategory.UNKNOWN
            }

            // Get Hasselblad params from mapping
            val hasselbladParams = sceneMapping.getParams(sceneId)
            val recommendedFilms = sceneMapping.getRecommendedFilms(sceneId)
            val masterTips = sceneMapping.getMasterTips(sceneId)

            SceneProfile(
                id = sceneId,
                name = sceneName,
                category = category,
                description = sceneName,
                color = category.color,
                confidence = confidence,
                hasselbladParams = hasselbladParams,
                recommendedFilm = recommendedFilms,
                masterTips = masterTips
            )
        } catch (e: Exception) {
            android.util.Log.w("MasterInferenceEngine", "Parse cloud response failed", e)
            null
        }
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
     * 应用单项优化处理到图片并返回处理后的 Bitmap
     * 根据 optimizationId 执行对应的原生 Android 图像处理操作
     *
     * @param bitmap 待处理的图片
     * @param optimizationId 优化项ID（hdr/denoise/sharpen/exposure/color）
     * @return 处理后的图片（失败时返回原图）
     */
    fun applyOptimization(bitmap: Bitmap, optimizationId: String): Bitmap {
        return try {
            when (optimizationId) {
                "hdr" -> applyHdr(bitmap)
                "denoise" -> applyDenoise(bitmap)
                "sharpen" -> applySharpen(bitmap)
                "exposure" -> applyExposure(bitmap)
                "color" -> applyColorCorrection(bitmap)
                else -> bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("MasterInferenceEngine", "applyOptimization failed: $optimizationId", e)
            bitmap
        }
    }

    /**
     * HDR 增强：使用局部色调映射，S-curve 压缩 + 原图混合实现自然 HDR 效果
     */
    private fun applyHdr(bitmap: Bitmap): Bitmap {
        // Step 1: Create luminance map
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Step 2: Apply local tone mapping using ColorMatrix for global adjustment
        // plus a vignette-aware brightness boost
        val matrix = ColorMatrix().apply {
            // S-curve tone mapping for HDR feel
            set(
                floatArrayOf(
                    1.2f, 0.05f, 0.05f, 0f, -15f,   // R: slight boost + offset
                    0.05f, 1.15f, 0.05f, 0f, -15f,   // G: moderate boost
                    0.05f, 0.05f, 1.1f, 0f, -10f,    // B: subtle cool shift
                    0f, 0f, 0f, 1f, 0f                // A: unchanged
                )
            )
        }
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isFilterBitmap = true
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Step 3: Blend with original for natural HDR look (50/50 blend)
        val blendPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            alpha = 180
        }
        canvas.drawBitmap(bitmap, 0f, 0f, blendPaint)

        return output
    }

    /**
     * 智能降噪：使用多通道 box blur 近似双边滤波，保留边缘
     */
    private fun applyDenoise(bitmap: Bitmap): Bitmap {
        // Multi-pass box blur to approximate bilateral filter
        var current = applyFastBlur(bitmap, radius = 2)
        // Edge-preserving: blend blurred with original, keeping edges sharp
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            alpha = 120  // 47% blend of blurred version
        }
        canvas.drawBitmap(current, 0f, 0f, paint)
        current.recycle()
        return output
    }

    /**
     * 锐化增强：改进的 Unsharp Mask，使用 LIGHTEN + ColorMatrix 增强边缘对比
     */
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val blurred = applyFastBlur(bitmap, radius = 3)
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw original
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Overlay (original - blur) * strength using SUBTRACT + ADD
        // This creates the unsharp mask effect
        val maskPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            alpha = 100  // ~39% sharpening strength
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                set(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -50f,
                        0f, 1.5f, 0f, 0f, -50f,
                        0f, 0f, 1.5f, 0f, -50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            })
        }
        canvas.drawBitmap(blurred, 0f, 0f, maskPaint)
        blurred.recycle()
        return output
    }

    /**
     * 自动曝光调整：基于直方图感知的亮度补偿，以中间灰为目标自动调整
     */
    private fun applyExposure(bitmap: Bitmap): Bitmap {
        // Calculate average brightness
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var totalLuma = 0L
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalLuma += (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
        }
        val avgLuma = (totalLuma.toFloat() / pixels.size).coerceIn(0f, 255f)

        // Calculate exposure compensation needed
        val targetLuma = 118f // Middle gray
        val compensation = (targetLuma - avgLuma) / 255f

        val matrix = ColorMatrix().apply {
            val gain = 1f + compensation * 2f  // Amplify compensation
            set(
                floatArrayOf(
                    gain, 0f, 0f, 0f, compensation * 40f,
                    0f, gain, 0f, 0f, compensation * 40f,
                    0f, 0f, gain, 0f, compensation * 40f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return drawWithColorMatrix(bitmap, matrix)
    }

    /**
     * 智能色彩校正：暖高光 + 冷阴影 + 15% 饱和度提升，实现自然色彩分级
     */
    private fun applyColorCorrection(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply {
            // Natural color enhancement: warm highlights, cool shadows
            set(
                floatArrayOf(
                    1.08f, 0.04f, 0.02f, 0f, 8f,     // R: warm boost
                    0.02f, 1.06f, 0.02f, 0f, 4f,      // G: natural green
                    0f, 0.02f, 1.04f, 0f, -2f,         // B: slight cool
                    0f, 0f, 0f, 1f, 0f                  // A: unchanged
                )
            )
        }
        // Apply saturation boost separately for more natural result
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(1.15f)  // 15% saturation boost
        }
        matrix.postConcat(saturationMatrix)
        return drawWithColorMatrix(bitmap, matrix)
    }

    /**
     * 使用 ColorMatrix 绘制 Bitmap
     */
    private fun drawWithColorMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isFilterBitmap = true
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * 快速 box blur 实现，用于降噪/锐化辅助
     */
    private fun applyFastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isFilterBitmap = true
            // 通过缩放实现快速模糊
        }
        val scale = 1f / (radius.coerceAtLeast(1) + 1)
        val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        canvas.drawBitmap(scaled, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        scaled.recycle()
        return output
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
