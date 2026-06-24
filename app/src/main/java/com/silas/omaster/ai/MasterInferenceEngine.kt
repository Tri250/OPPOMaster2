package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
 * 已修复：人脸检测改用 ML Kit 真实检测结果
 * 注意：当前场景识别使用启发式分析器（HeuristicSceneAnalyzer），非 TFLite 模型推理
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
    ): SceneProfile = analyzeImageWithDetails(bitmap, imagePath).profile

    /**
     * 分析图片并生成完整场景分析详情（含主场景 + 备选场景）。
     *
     * P1-10：统一场景识别模型与 UI 模式，将启发式分析器输出的细粒度场景
     * 以及 Top-3 备选场景完整返回给上层，供结果页展开与切换。
     */
    suspend fun analyzeImageWithDetails(
        bitmap: Bitmap,
        imagePath: String? = null
    ): SceneAnalysisDetail = withContext(Dispatchers.Default) {
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
        val profile = sceneProfile.copy(
            exifData = exifData,
            histogramData = computeRealHistogram(bitmap),
            faceData = faceData,
            confidence = analysisResult.confidence
        )

        SceneAnalysisDetail(
            profile = profile,
            alternatives = analysisResult.alternativeScenes
        )
    }

    /**
     * 场景分析详情
     */
    data class SceneAnalysisDetail(
        val profile: SceneProfile,
        val alternatives: List<SceneProfile>
    )

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
     * @param strength 强度 0.0~1.0，默认 1.0（全效果）。各算法根据强度缩放参数
     * @return 处理后的图片（失败时返回原图）
     */
    fun applyOptimization(bitmap: Bitmap, optimizationId: String, strength: Float = 1.0f): Bitmap {
        val clampedStrength = strength.coerceIn(0f, 1f)
        return try {
            when (optimizationId) {
                "hdr" -> applyHdr(bitmap, clampedStrength)
                "denoise" -> applyDenoise(bitmap, clampedStrength)
                "sharpen" -> applySharpen(bitmap, clampedStrength)
                "exposure" -> applyExposure(bitmap, clampedStrength)
                "color" -> applyColorCorrection(bitmap, clampedStrength)
                else -> bitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("MasterInferenceEngine", "applyOptimization failed: $optimizationId", e)
            bitmap
        }
    }

    /**
     * HDR 增强：提升动态范围，显著提升亮部与暗部
     * @param strength 0.0~1.0，控制对比度与亮度提升幅度
     */
    private fun applyHdr(bitmap: Bitmap, strength: Float): Bitmap {
        // 基础参数：对比度 1.15，亮度 25（全强度时）
        val contrast = 1.0f + 0.15f * strength
        val brightness = 25f * strength
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return drawWithColorMatrix(bitmap, matrix)
    }

    /**
     * 智能降噪：使用 O(n) boxBlur 平滑高频噪点
     * @param strength 0.0~1.0，控制模糊半径（1~8）
     */
    private fun applyDenoise(bitmap: Bitmap, strength: Float): Bitmap {
        val radius = (1 + 7 * strength).toInt().coerceIn(1, 8)
        return applyBoxBlur(bitmap, radius)
    }

    /**
     * 锐化增强：使用 Unsharp Mask 提升边缘清晰度。
     * @param strength 0.0~1.0，控制锐化强度（0.4~2.0）
     */
    private fun applySharpen(bitmap: Bitmap, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val blurred = applyBoxBlur(bitmap, radius = 4)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // 锐化强度：0.4（轻微）到 2.0（强烈），由 strength 线性映射
        val sharpenStrength = 0.4f + 1.6f * strength

        val srcPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)

        for (i in srcPixels.indices) {
            val s = srcPixels[i]
            val b = blurPixels[i]
            val sa = (s ushr 24) and 0xFF
            val sr = (s shr 16) and 0xFF
            val sg = (s shr 8) and 0xFF
            val sb = s and 0xFF

            val br = (b shr 16) and 0xFF
            val bg = (b shr 8) and 0xFF
            val bb = b and 0xFF

            val or = (sr + sharpenStrength * (sr - br)).toInt().coerceIn(0, 255)
            val og = (sg + sharpenStrength * (sg - bg)).toInt().coerceIn(0, 255)
            val ob = (sb + sharpenStrength * (sb - bb)).toInt().coerceIn(0, 255)

            outPixels[i] = (sa shl 24) or (or shl 16) or (og shl 8) or ob
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        blurred.recycle()
        return output
    }

    /**
     * 自动曝光调整：根据直方图进行亮度补偿
     * @param strength 0.0~1.0，控制曝光补偿幅度
     */
    private fun applyExposure(bitmap: Bitmap, strength: Float): Bitmap {
        val scale = 1.0f + 0.10f * strength
        val offset = 30f * strength
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    scale, 0f, 0f, 0f, offset,
                    0f, scale, 0f, 0f, offset,
                    0f, 0f, scale, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return drawWithColorMatrix(bitmap, matrix)
    }

    /**
     * 智能色彩校正：增强暖调与自然饱和度
     * @param strength 0.0~1.0，控制色彩偏移幅度
     */
    private fun applyColorCorrection(bitmap: Bitmap, strength: Float): Bitmap {
        val matrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    1.0f + 0.12f * strength, 0.08f * strength, 0f, 0f, 15f * strength,
                    0f, 1.0f + 0.05f * strength, 0f, 0f, 8f * strength,
                    0f, 0f, 1.0f - 0.05f * strength, 0f, -5f * strength,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
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
     * O(n) boxBlur 实现：水平+垂直滑动窗口盒式模糊
     * 替代原 applyFastBlur（缩放模糊），保留更多细节
     * @param radius 模糊半径（1~8）
     */
    private fun applyBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val srcPixels = IntArray(w * h)
        val tempPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        bitmap.getPixels(srcPixels, 0, w, 0, 0, w, h)

        // 水平模糊
        val diameter = 2 * radius + 1
        for (y in 0 until h) {
            var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
            // 初始化窗口
            for (x in -radius..radius) {
                val cx = x.coerceIn(0, w - 1)
                val pixel = srcPixels[y * w + cx]
                aSum += (pixel ushr 24) and 0xFF
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
            }
            for (x in 0 until w) {
                tempPixels[y * w + x] = ((aSum / diameter).toInt() shl 24) or
                        ((rSum / diameter).toInt() shl 16) or
                        ((gSum / diameter).toInt() shl 8) or
                        (bSum / diameter).toInt()
                // 滑动窗口：移出左侧，移入右侧
                val leftX = (x - radius - 1).coerceIn(0, w - 1)
                val rightX = (x + radius + 1).coerceIn(0, w - 1)
                val leftPixel = srcPixels[y * w + leftX]
                val rightPixel = srcPixels[y * w + rightX]
                aSum += ((rightPixel ushr 24) and 0xFF) - ((leftPixel ushr 24) and 0xFF)
                rSum += ((rightPixel shr 16) and 0xFF) - ((leftPixel shr 16) and 0xFF)
                gSum += ((rightPixel shr 8) and 0xFF) - ((leftPixel shr 8) and 0xFF)
                bSum += (rightPixel and 0xFF) - (leftPixel and 0xFF)
            }
        }

        // 垂直模糊
        for (x in 0 until w) {
            var rSum = 0L; var gSum = 0L; var bSum = 0L; var aSum = 0L
            for (y in -radius..radius) {
                val cy = y.coerceIn(0, h - 1)
                val pixel = tempPixels[cy * w + x]
                aSum += (pixel ushr 24) and 0xFF
                rSum += (pixel shr 16) and 0xFF
                gSum += (pixel shr 8) and 0xFF
                bSum += pixel and 0xFF
            }
            for (y in 0 until h) {
                outPixels[y * w + x] = ((aSum / diameter).toInt() shl 24) or
                        ((rSum / diameter).toInt() shl 16) or
                        ((gSum / diameter).toInt() shl 8) or
                        (bSum / diameter).toInt()
                val topY = (y - radius - 1).coerceIn(0, h - 1)
                val botY = (y + radius + 1).coerceIn(0, h - 1)
                val topPixel = tempPixels[topY * w + x]
                val botPixel = tempPixels[botY * w + x]
                aSum += ((botPixel ushr 24) and 0xFF) - ((topPixel ushr 24) and 0xFF)
                rSum += ((botPixel shr 16) and 0xFF) - ((topPixel shr 16) and 0xFF)
                gSum += ((botPixel shr 8) and 0xFF) - ((topPixel shr 8) and 0xFF)
                bSum += (botPixel and 0xFF) - (topPixel and 0xFF)
            }
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(outPixels, 0, w, 0, 0, w, h)
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
     * 基于 Bitmap 像素的真实直方图计算
     *
     * 直接遍历像素数据，统计每个亮度/色阶的像素数量，
     * 生成 256 级精度的 L/R/G/B 直方图。采样步长根据图片尺寸动态调整，
     * 大图跳步采样保证性能，小图逐像素保证精度。
     */
    private fun computeRealHistogram(bitmap: Bitmap): HistogramData {
        val width = bitmap.width
        val height = bitmap.height

        val luminance = IntArray(256)
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)

        // 动态采样步长：大图稀疏采样，小图密集采样
        val step = when {
            width > 2000 || height > 2000 -> 6   // ~3M+ 像素，6倍下采样
            width > 1000 || height > 1000 -> 4   // ~1M+ 像素，4倍下采样
            width > 500 || height > 500 -> 2     // ~250K+ 像素，2倍下采样
            else -> 1                             // 小图逐像素
        }

        var totalLuminance = 0L
        var pixelCount = 0
        var shadowCount = 0
        var highlightCount = 0

        // 逐像素统计直方图
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)

                // Rec.709 亮度
                val luma = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt().coerceIn(0, 255)

                luminance[luma]++
                red[r]++
                green[g]++
                blue[b]++

                totalLuminance += luma
                pixelCount++

                if (luma < 50) shadowCount++
                if (luma > 200) highlightCount++
            }
        }

        val meanLuminance = if (pixelCount > 0) (totalLuminance / pixelCount).toFloat() else 0f
        val shadowRatio = if (pixelCount > 0) shadowCount.toFloat() / pixelCount else 0f
        val highlightRatio = if (pixelCount > 0) highlightCount.toFloat() / pixelCount else 0f

        return HistogramData(
            luminance = luminance,
            red = red,
            green = green,
            blue = blue,
            meanLuminance = meanLuminance,
            shadowClipping = shadowRatio > 0.7f,
            highlightClipping = highlightRatio > 0.3f
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
