package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.silas.omaster.renderer.GPURenderManager
import com.silas.omaster.renderer.RenderParameters
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 人像模式处理参数
 *
 * @param blurStrength 背景虚化强度 [0, 1]，默认 0.6
 * @param beauty 五层美颜参数
 */
data class PortraitModeParams(
    val blurStrength: Float = 0.6f,
    val beauty: BeautyLevels = BeautyLevels()
)

/**
 * 五层美颜强度配置
 *
 * @param smooth 磨皮强度 [0, 1]
 * @param whitening 美白强度 [0, 1]
 * @param faceSlim 瘦脸强度 [0, 1]
 * @param eyeEnlarge 大眼强度 [0, 1]
 * @param blush 红润强度 [0, 1]
 * @param enabled 美颜总开关
 */
data class BeautyLevels(
    val smooth: Float = 0.4f,
    val whitening: Float = 0.3f,
    val faceSlim: Float = 0.2f,
    val eyeEnlarge: Float = 0.2f,
    val blush: Float = 0.1f,
    val enabled: Boolean = true
)

/**
 * 人像模式管理器
 *
 * 职责：
 * 1. 实时人像虚化（Bokeh）：基于 ML Kit Selfie Segmentation 前景掩码，对背景做高斯/散景模糊。
 * 2. 姿势引导：基于 MediaPipe Pose Landmarker 检测人体关键点，给出实时引导建议。
 * 3. 五层美颜引擎：磨皮、美白、瘦脸、大眼、红润，每层可独立开关与调节强度。
 * 4. 实时预览处理：优先使用 GPU（GPURenderManager）做最终色彩/肤色增强；GPU 失败或不可用时使用 CPU 降级。
 *
 * 线程安全：
 * - 检测器为单例，在 [PortraitModeManager] 实例间共享。
 * - 可变参数使用 @Volatile，支持运行时动态调节。
 * - 图像处理在协程后台线程执行，不阻塞 UI。
 *
 * Bitmap 所有权：
 * - [processFrame] 的输入 Bitmap 由调用方持有，本类不会回收。
 * - 返回的 [PortraitModeResult.processedFrame] 为新分配 Bitmap，由调用方负责回收。
 */
class PortraitModeManager(context: Context) {

    companion object {
        private const val TAG = "PortraitModeManager"

        /** MediaPipe Pose Landmarker 模型路径（需置于 assets） */
        private const val POSE_LANDMARKER_MODEL = "pose_landmarker_lite.task"

        /** 处理分辨率上限，避免高分辨率预览导致 ANR */
        private const val MAX_PROCESS_DIMENSION = 1280

        /** 分割掩码前景阈值 */
        private const val MASK_THRESHOLD = 0.5f

        /** 多人脸美颜时最多处理的人脸数，平衡效果与性能 */
        private const val MAX_BEAUTY_FACES = 3

        /** 人脸检测器单例 */
        @Volatile
        private var faceDetectorInstance: FaceDetector? = null

        /** Selfie 分割器单例 */
        @Volatile
        private var selfieSegmenterInstance: Segmenter? = null

        /** MediaPipe Pose Landmarker 单例（模型缺失时可能为 null） */
        @Volatile
        private var poseLandmarkerInstance: PoseLandmarker? = null

        /** 是否已尝试初始化 Pose Landmarker，避免重复加载缺失模型 */
        @Volatile
        private var poseInitialized = false

        /**
         * 获取/创建 ML Kit 人脸检测器（单例）
         */
        private fun getFaceDetector(): FaceDetector {
            return faceDetectorInstance ?: synchronized(this) {
                faceDetectorInstance ?: FaceDetection.getClient(
                    FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .setMinFaceSize(0.15f)
                        .build()
                ).also { faceDetectorInstance = it }
            }
        }

        /**
         * 获取/创建 ML Kit Selfie Segmenter（单例）
         */
        private fun getSelfieSegmenter(): Segmenter {
            return selfieSegmenterInstance ?: synchronized(this) {
                selfieSegmenterInstance ?: Segmentation.getClient(
                    SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build()
                ).also { selfieSegmenterInstance = it }
            }
        }

        /**
         * 获取/创建 MediaPipe Pose Landmarker（单例，模型缺失时返回 null）
         */
        private fun getPoseLandmarker(context: Context): PoseLandmarker? {
            if (poseInitialized) return poseLandmarkerInstance
            synchronized(this) {
                if (poseInitialized) return poseLandmarkerInstance
                poseInitialized = true
                return try {
                    val baseOptions = BaseOptions.builder()
                        .setModelAssetPath(POSE_LANDMARKER_MODEL)
                        .build()
                    val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.IMAGE)
                        .setNumPoses(1)
                        .setMinPoseDetectionConfidence(0.5f)
                        .setMinPosePresenceConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .build()
                    PoseLandmarker.createFromOptions(context.applicationContext, options).also {
                        Log.d(TAG, "Pose Landmarker 初始化成功")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Pose Landmarker 初始化失败（模型可能缺失）: ${e.message}")
                    null
                }
            }
        }
    }

    private val appContext = context.applicationContext

    /** GPU 渲染管理器，用于最终色彩/肤色增强通道 */
    private val gpuRenderManager = GPURenderManager.getInstance(appContext)

    /** 虚化强度（运行时可通过 [setBlurStrength] 调节） */
    @Volatile
    private var currentBlurStrength: Float = 0.6f

    /** 美颜强度（运行时可通过 [setBeautyLevels] 调节） */
    @Volatile
    private var currentBeautyLevels: BeautyLevels = BeautyLevels()

    /** 是否已释放 */
    @Volatile
    private var isReleased: Boolean = false

    init {
        // 提前触发单例检测器创建，避免首次处理帧时初始化耗时
        getFaceDetector()
        getSelfieSegmenter()
        getPoseLandmarker(appContext)
    }

    /**
     * 设置虚化强度
     *
     * @param strength [0, 1]，0 表示不虚化，1 表示最大虚化
     */
    fun setBlurStrength(strength: Float) {
        currentBlurStrength = strength.coerceIn(0f, 1f)
    }

    /**
     * 设置美颜等级
     */
    fun setBeautyLevels(levels: BeautyLevels) {
        currentBeautyLevels = levels
    }

    /**
     * 处理单帧图像，返回人像模式结果。
     *
     * 所有权契约：调用方将 [bitmap] 所有权移交给本方法；本方法保证在处理完成后回收该
     * Bitmap 及其缩放副本，调用方不应再访问 [bitmap]。
     *
     * 处理流程：
     * 1. 分辨率自适应缩放（保持宽高比，限制最大边）。
     * 2. 并行执行人脸检测、Selfie 分割、Pose 检测。
     * 3. 根据检测结果生成姿势引导。
     * 4. 优先走 GPU 渲染管线；失败时降级 CPU。
     * 5. 返回处理后的 Bitmap 与元信息。
     *
     * @param bitmap 输入帧（所有权转移给本方法）
     * @param params 本次处理参数；为 null 时使用当前 [setBlurStrength]/[setBeautyLevels] 设置
     */
    suspend fun processFrame(
        bitmap: Bitmap,
        params: PortraitModeParams? = null
    ): PortraitModeResult = withContext(Dispatchers.Default) {
        if (isReleased || bitmap.isRecycled) {
            if (!bitmap.isRecycled) bitmap.recycle()
            Log.w(TAG, "PortraitModeManager 已释放或输入无效，跳过处理")
            return@withContext PortraitModeResult(null, 0, null, false)
        }

        val effectiveParams = params ?: PortraitModeParams(
            blurStrength = currentBlurStrength,
            beauty = currentBeautyLevels
        )

        // 限制处理分辨率，保证实时性；并接管原始 Bitmap 生命周期
        val workBitmap = scaleToProcessDimension(bitmap)
        if (workBitmap !== bitmap) {
            bitmap.recycle()
        }

        try {
            // 并行检测：人脸、分割、姿势
            val facesDeferred: Deferred<List<Face>> = async { detectFaces(workBitmap) }
            val maskDeferred: Deferred<SegmentationMask?> = async { detectSegmentationMask(workBitmap) }
            val poseDeferred: Deferred<PoseLandmarkerResult?> = async { detectPose(workBitmap) }

            val faces = facesDeferred.await()
            val mask = maskDeferred.await()
            val poseResult = poseDeferred.await()

            val poseGuide = generatePoseGuide(poseResult, faces)

            val processedBitmap = applyPortraitEffects(workBitmap, mask, faces, effectiveParams)

            PortraitModeResult(
                processedFrame = processedBitmap,
                faceCount = faces.size,
                poseGuide = poseGuide,
                beautyApplied = effectiveParams.beauty.enabled && hasActiveBeauty(effectiveParams.beauty)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "人像模式处理失败", e)
            PortraitModeResult(null, 0, null, false)
        } finally {
            if (!workBitmap.isRecycled) {
                workBitmap.recycle()
            }
        }
    }

    /**
     * 释放所有资源。
     *
     * 注意：检测器单例（Face/Selfie/Pose）在应用生命周期内保持常驻，
     * 避免 CameraX 生命周期切换时反复创建。此处仅释放本管理器的协程作用域与状态。
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        // 检测器单例在应用生命周期内保持常驻，避免 CameraX 生命周期切换时反复创建
        Log.d(TAG, "PortraitModeManager 已释放")
    }

    // ==================== 检测层 ====================

    /**
     * 人脸检测（协程 + ML Kit Task await）
     */
    private suspend fun detectFaces(bitmap: Bitmap): List<Face> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            getFaceDetector().process(inputImage).await()
        } catch (e: Exception) {
            Log.w(TAG, "人脸检测失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * Selfie 分割（协程 + ML Kit Task await）
     */
    private suspend fun detectSegmentationMask(bitmap: Bitmap): SegmentationMask? = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            getSelfieSegmenter().process(inputImage).await()
        } catch (e: Exception) {
            Log.w(TAG, "Selfie 分割失败: ${e.message}")
            null
        }
    }

    /**
     * 姿势检测（协程 + MediaPipe）
     */
    private suspend fun detectPose(bitmap: Bitmap): PoseLandmarkerResult? = withContext(Dispatchers.Default) {
        val landmarker = getPoseLandmarker(appContext) ?: return@withContext null
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            landmarker.detect(mpImage)
        } catch (e: Exception) {
            Log.w(TAG, "姿势检测失败: ${e.message}")
            null
        }
    }

    // ==================== 姿势引导 ====================

    /**
     * 基于检测到的姿势/人脸生成引导建议。
     *
     * 规则（优先级从高到低）：
     * - 未检测到人体或人脸：提示“请对准人物”。
     * - 侧身角度过大：提示“侧脸 45°”。
     * - 手臂位置不自然：提示“抬手自然摆放”。
     * - 低头/仰头明显：提示“保持头部平视”。
     * - 人物太小：提示“靠近镜头”。
     * - 姿态良好：提示“保持当前姿势”。
     */
    private fun generatePoseGuide(poseResult: PoseLandmarkerResult?, faces: List<Face>): PoseGuide? {
        if (faces.isEmpty() && poseResult?.landmarks().isNullOrEmpty()) {
            return PoseGuide("请对准人物", "将人物置于画面中央，保持稳定")
        }

        val landmarks = poseResult?.landmarks()?.firstOrNull()
        if (landmarks != null && landmarks.size >= 33) {
            // 肩部、髋部用于判断侧身角度
            val leftShoulder = landmarks[11]
            val rightShoulder = landmarks[12]
            val leftHip = landmarks[23]
            val rightHip = landmarks[24]

            val shoulderDx = abs(leftShoulder.x() - rightShoulder.x())
            val hipDx = abs(leftHip.x() - rightHip.x())
            val shoulderDy = abs(leftShoulder.y() - rightShoulder.y())
            val hipDy = abs(leftHip.y() - rightHip.y())

            // 侧身判定：肩部/髋部在画面中的水平投影较短，且存在明显高度差
            if (shoulderDx < 0.12f && hipDx < 0.12f && (shoulderDy > 0.08f || hipDy > 0.08f)) {
                return PoseGuide("侧脸 45°", "稍微转回正面，或保持优雅侧脸 45° 角")
            }

            // 手臂姿态：手腕高于肩膀且距离身体较远
            val leftWrist = landmarks[15]
            val rightWrist = landmarks[16]
            val wristRaised = leftWrist.y() < leftShoulder.y() - 0.08f ||
                    rightWrist.y() < rightShoulder.y() - 0.08f
            val wristWide = leftWrist.x() < leftShoulder.x() - 0.15f ||
                    rightWrist.x() > rightShoulder.x() + 0.15f
            if (wristRaised && wristWide) {
                return PoseGuide("抬手自然摆放", "手臂略微放松，避免僵硬举高")
            }

            // 头部俯仰：鼻尖与耳根连线的角度
            val nose = landmarks[0]
            val leftEar = landmarks[7]
            val rightEar = landmarks[8]
            val earCenterY = (leftEar.y() + rightEar.y()) / 2f
            val headTilt = nose.y() - earCenterY
            when {
                headTilt > 0.12f -> return PoseGuide("轻微抬头", "下巴微微抬起，展现颈部线条")
                headTilt < -0.12f -> return PoseGuide("轻微低头", "头部回正，保持自然平视")
            }

            // 肩部水平：双肩高度差过大时提示
            val shoulderDyDiff = abs(leftShoulder.y() - rightShoulder.y())
            if (shoulderDyDiff > 0.08f) {
                return PoseGuide("双肩放平", "保持双肩水平，姿态更端正")
            }

            // 人物占比：根据全身/半身/特写给出不同提示
            val poseMinX = landmarks.minOf { it.x() }
            val poseMaxX = landmarks.maxOf { it.x() }
            val poseMinY = landmarks.minOf { it.y() }
            val poseMaxY = landmarks.maxOf { it.y() }
            val bodyWidth = poseMaxX - poseMinX
            val bodyHeight = poseMaxY - poseMinY
            if (bodyWidth < 0.20f || bodyHeight < 0.35f) {
                return PoseGuide("靠近镜头", "人物占比较小，请适当靠近")
            }
            if (bodyHeight > 0.85f) {
                return PoseGuide("特写构图", "人物几乎充满画面，适合特写情绪表达")
            }
            if (bodyHeight in 0.55f..0.85f) {
                return PoseGuide("半身构图", "半身人像构图良好，注意头肩比例")
            }

            // 手部姿态：复用已有的手腕坐标，检测手部是否出现在画面中
            val leftElbow = landmarks[13]
            val rightElbow = landmarks[14]
            val handInFrame = (leftWrist.y() in 0f..1f) || (rightWrist.y() in 0f..1f)
            if (handInFrame) {
                val handNearFace = (abs(leftWrist.x() - nose.x()) < 0.18f && abs(leftWrist.y() - nose.y()) < 0.18f) ||
                        (abs(rightWrist.x() - nose.x()) < 0.18f && abs(rightWrist.y() - nose.y()) < 0.18f)
                val handOnHip = (abs(leftWrist.x() - leftHip.x()) < 0.12f && abs(leftWrist.y() - leftHip.y()) < 0.12f) ||
                        (abs(rightWrist.x() - rightHip.x()) < 0.12f && abs(rightWrist.y() - rightHip.y()) < 0.12f)
                if (handNearFace) {
                    return PoseGuide("手部自然", "手部靠近面部时保持自然，避免遮挡五官")
                }
                if (handOnHip) {
                    return PoseGuide("手部自然", "叉腰姿态优雅，注意手肘不要过度外扩")
                }
            }

            // 站姿 / 坐姿：根据髋部高度与身体占比判断
            val hipCenterY = (leftHip.y() + rightHip.y()) / 2f
            val shoulderCenterY = (leftShoulder.y() + rightShoulder.y()) / 2f
            if (hipCenterY > 0.70f && bodyHeight in 0.35f..0.75f && shoulderCenterY < hipCenterY) {
                return PoseGuide("坐姿优雅", "坐姿人像可稍微侧身，双腿自然摆放")
            }

            // 身体倾斜：肩中点与髋中点水平偏移过大
            val shoulderCenterX = (leftShoulder.x() + rightShoulder.x()) / 2f
            val hipCenterX = (leftHip.x() + rightHip.x()) / 2f
            if (abs(shoulderCenterX - hipCenterX) > 0.10f) {
                return PoseGuide("身体回正", "肩部和髋部保持垂直，姿态更挺拔")
            }
        }

        // 基于人脸的额外引导
        if (faces.isNotEmpty()) {
            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: faces[0]
            val rotY = face.headEulerAngleY  // 左右转头
            val rotX = face.headEulerAngleX  // 上下点头
            when {
                rotY > 35f -> return PoseGuide("侧脸 45°", "头部稍微回正，或保持 45° 侧脸")
                rotY < -35f -> return PoseGuide("侧脸 45°", "头部稍微回正，或保持 45° 侧脸")
                rotX > 20f -> return PoseGuide("轻微抬头", "头部回正，保持平视")
                rotX < -20f -> return PoseGuide("轻微低头", "头部回正，保持平视")
            }
        }

        return PoseGuide("保持当前姿势", "构图与姿态良好，请保持")
    }

    // ==================== 渲染管线 ====================

    /**
     * 人像效果主入口：优先 GPU，失败降级 CPU。
     *
     * GPU 路径说明：
     * - 现有 GPURenderManager 主要负责通用色彩调整（亮度、饱和度、肤色平滑），
     *   不包含 Bokeh 与几何形变管线。
     * - 因此 GPU 路径先使用 CPU 完成 Bokeh、瘦脸、大眼等像素/几何处理，
     *   再交由 GPURenderManager 执行最终肤色平滑、美白等色彩通道增强。
     * - 若 GPU 色彩通道失败，直接返回 CPU 结果，保证预览不中断。
     */
    private suspend fun applyPortraitEffects(
        bitmap: Bitmap,
        mask: SegmentationMask?,
        faces: List<Face>,
        params: PortraitModeParams
    ): Bitmap? {
        return try {
            if (gpuRenderManager.isGPUAvailable()) {
                applyPortraitEffectsGPU(bitmap, mask, faces, params)
            } else {
                applyPortraitEffectsCPU(bitmap, mask, faces, params)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU 人像处理失败，降级 CPU", e)
            applyPortraitEffectsCPU(bitmap, mask, faces, params)
        }
    }

    /**
     * GPU 路径：CPU 完成 Bokeh/几何美颜，GPU 完成最终色彩增强。
     */
    private suspend fun applyPortraitEffectsGPU(
        bitmap: Bitmap,
        mask: SegmentationMask?,
        faces: List<Face>,
        params: PortraitModeParams
    ): Bitmap {
        // CPU 阶段完成 Bokeh、瘦脸、大眼；暂时关闭色彩类美颜，留给 GPU
        val cpuParams = params.copy(
            beauty = params.beauty.copy(smooth = 0f, whitening = 0f, blush = 0f)
        )
        val cpuProcessed = applyPortraitEffectsCPU(bitmap, mask, faces, cpuParams)

        // GPU 最终色彩增强；成功时回收 CPU 中间结果，避免内存泄漏
        val gpuResult = applyGPUColorPass(cpuProcessed, params)
        return if (gpuResult != null && gpuResult !== cpuProcessed) {
            if (!cpuProcessed.isRecycled) cpuProcessed.recycle()
            gpuResult
        } else {
            gpuResult ?: cpuProcessed
        }
    }

    /**
     * 使用 GPURenderManager 做最终色彩/肤色增强。
     */
    private suspend fun applyGPUColorPass(bitmap: Bitmap, params: PortraitModeParams): Bitmap? {
        if (!params.beauty.enabled) return null
        val beauty = params.beauty
        val renderParams = RenderParameters(
            brightness = beauty.whitening * 25f,
            saturation = -beauty.whitening * 15f,
            skinSmooth = beauty.smooth * 80f,
            warmth = beauty.blush * 8f,
            vibrance = beauty.blush * 5f
        )
        return try {
            gpuRenderManager.renderPreview(bitmap, renderParams)
        } catch (e: Exception) {
            Log.w(TAG, "GPU 色彩增强失败", e)
            null
        }
    }

    /**
     * CPU 完整人像处理管线。
     */
    private fun applyPortraitEffectsCPU(
        bitmap: Bitmap,
        mask: SegmentationMask?,
        faces: List<Face>,
        params: PortraitModeParams
    ): Bitmap {
        var current = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // 1. 实时人像虚化
        if (params.blurStrength > 0.005f && mask != null) {
            val bokeh = applyBokeh(current, mask, params.blurStrength)
            if (bokeh !== current && !current.isRecycled) current.recycle()
            current = bokeh
        }

        // 2. 五层美颜
        if (params.beauty.enabled) {
            val beauty = params.beauty

            // 磨皮：对肤色区域做平滑
            if (beauty.smooth > 0.005f) {
                applySkinSmooth(current, faces, beauty.smooth)
            }

            // 美白：提亮 + 轻微降饱和
            if (beauty.whitening > 0.005f) {
                applyWhitening(current, faces, beauty.whitening)
            }

            // 瘦脸：基于人脸轮廓的局部形变，按面积处理前 3 个人脸
            if (beauty.faceSlim > 0.005f && faces.isNotEmpty()) {
                val sortedFaces = faces.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
                    .take(MAX_BEAUTY_FACES)
                for (face in sortedFaces) {
                    val slimmed = applyFaceSlim(current, face, beauty.faceSlim)
                    if (slimmed !== current && !current.isRecycled) current.recycle()
                    current = slimmed
                }
            }

            // 大眼：基于眼睛轮廓的局部放大，按面积处理前 3 个人脸
            if (beauty.eyeEnlarge > 0.005f && faces.isNotEmpty()) {
                val sortedFaces = faces.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
                    .take(MAX_BEAUTY_FACES)
                for (face in sortedFaces) {
                    val enlarged = applyEyeEnlarge(current, face, beauty.eyeEnlarge)
                    if (enlarged !== current && !current.isRecycled) current.recycle()
                    current = enlarged
                }
            }

            // 红润：提升肤色区域红色通道，按面积处理前 3 个人脸
            if (beauty.blush > 0.005f) {
                val sortedFaces = faces.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
                    .take(MAX_BEAUTY_FACES)
                for (face in sortedFaces) {
                    applyBlush(current, face, beauty.blush)
                }
            }
        }

        return current
    }

    // ==================== 实时人像虚化（Bokeh）====================

    /**
     * 应用 Bokeh 虚化：背景高斯模糊 + 前景保留清晰。
     *
     * 实现：
     * 1. 对原图做快速近似高斯模糊（多趟盒式模糊）。
     * 2. 将 Selfie Segmentation 掩码上采样到图像尺寸，并对边缘做 3x3 平滑。
     * 3. 使用更宽的 smoothstep 羽化过渡，减少前景锯齿与背景残留。
     */
    private fun applyBokeh(bitmap: Bitmap, mask: SegmentationMask, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 模糊半径随强度变化：预览场景下控制在 5~21 之间
        val blurRadius = (5 + strength * 16f).toInt().coerceIn(5, 21)

        // 先获取原图像素
        val srcPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // 生成模糊图
        val blurredPixels = boxBlur(srcPixels, width, height, blurRadius)

        // 准备并平滑掩码边缘
        val smoothedMask = buildSmoothedForegroundMask(mask, width, height)

        val output = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val foreground = smoothedMask[idx]
                val original = srcPixels[idx]
                val blurred = blurredPixels[idx]

                output[idx] = mixPixel(original, blurred, 1f - foreground)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 将分割掩码上采样到图像尺寸，并对边缘做 3x3 盒式平滑。
     * 返回每个像素的“前景置信度”FloatArray（0~1）。
     */
    private fun buildSmoothedForegroundMask(mask: SegmentationMask, width: Int, height: Int): FloatArray {
        val maskBuffer: FloatBuffer = mask.buffer.asFloatBuffer()
        val maskWidth = mask.width
        val maskHeight = mask.height
        val maskArray = FloatArray(maskWidth * maskHeight)
        maskBuffer.rewind()
        maskBuffer.get(maskArray)

        // 上采样到图像尺寸
        val raw = FloatArray(width * height)
        for (y in 0 until height) {
            val maskY = (y * maskHeight / height).coerceIn(0, maskHeight - 1)
            val rowOffset = y * width
            for (x in 0 until width) {
                val maskX = (x * maskWidth / width).coerceIn(0, maskWidth - 1)
                raw[rowOffset + x] = maskArray[maskY * maskWidth + maskX]
            }
        }

        // 3x3 盒式平滑，减少边缘锯齿
        val smoothed = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                for (dy in -1..1) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    for (dx in -1..1) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        sum += raw[ny * width + nx]
                        count++
                    }
                }
                val avg = sum / count
                // 更宽的羽化带，过渡自然
                smoothed[y * width + x] = smoothstep(0.35f, 0.65f, avg)
            }
        }
        return smoothed
    }

    // ==================== 五层美颜引擎 ====================

    /**
     * 磨皮：对肤色区域做盒式模糊并混合，同时保护五官（眼、嘴）边缘不被过度模糊。
     *
     * 保护策略：
     * - 构建眼睛/嘴巴的“保护距离场”，距离五官越近，磨皮强度越低；
     * - 仅在肤色像素上生效；
     * - 通过原图与模糊图混合控制最终强度。
     */
    private fun applySkinSmooth(bitmap: Bitmap, faces: List<Face>, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 半径 2~5 的盒式模糊
        val radius = (2 + strength * 3f).toInt().coerceIn(2, 5)
        val blurred = boxBlur(pixels, width, height, radius)

        // 构建五官保护掩码（0 = 完全保护，1 = 完全磨皮）
        val protectionMask = buildFeatureProtectionMask(width, height, faces)
        val baseMixStrength = strength * 0.55f

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p ushr 16) and 0xFF) / 255f
            val g = ((p ushr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            if (isSkinColor(r, g, b)) {
                val protectedStrength = baseMixStrength * protectionMask[i]
                if (protectedStrength > 0.005f) {
                    pixels[i] = mixPixel(p, blurred[i], protectedStrength)
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 构建五官保护掩码，保护眼睛和嘴巴区域不被磨皮过度模糊。
     * 返回 FloatArray，取值 0（完全保护）~ 1（不保护）。
     */
    private fun buildFeatureProtectionMask(width: Int, height: Int, faces: List<Face>): FloatArray {
        val mask = FloatArray(width * height) { 1f }
        if (faces.isEmpty()) return mask

        // 保护半径：约为眼睛/嘴轮廓外接圆半径的 1.2 倍
        val protectRadiusScale = 1.2f

        faces.forEach { face ->
            val leftEyePos = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEyePos = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
            val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
            val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
            val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position

            val eyeFeatures = listOfNotNull(leftEyePos, rightEyePos)
            val mouthFeatures = listOfNotNull(mouthLeft, mouthRight, mouthBottom, noseBase)

            val eyeRadiusPx = maxOf(
                eyeRadius(face.getContour(FaceContour.LEFT_EYE)?.points ?: emptyList()),
                eyeRadius(face.getContour(FaceContour.RIGHT_EYE)?.points ?: emptyList()),
                8f
            ) * protectRadiusScale
            val mouthRadiusPx = maxOf(
                eyeRadius(face.getContour(FaceContour.UPPER_LIP_TOP)?.points ?: emptyList()),
                eyeRadius(face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points ?: emptyList()),
                10f
            ) * protectRadiusScale

            eyeFeatures.forEach { center ->
                applyRadialProtectionMask(mask, width, height, center, eyeRadiusPx)
            }
            mouthFeatures.forEach { center ->
                applyRadialProtectionMask(mask, width, height, center, mouthRadiusPx)
            }
        }

        return mask
    }

    /**
     * 对以 [center] 为圆心、[radius] 为半径的圆形区域应用径向保护掩码。
     * 圆心处 mask = 0（完全保护），边缘 mask = 1（不保护）。
     */
    private fun applyRadialProtectionMask(
        mask: FloatArray,
        width: Int,
        height: Int,
        center: PointF,
        radius: Float
    ) {
        val cx = center.x
        val cy = center.y
        val minX = max(0, (cx - radius).toInt())
        val maxX = min(width - 1, (cx + radius).toInt())
        val minY = max(0, (cy - radius).toInt())
        val maxY = min(height - 1, (cy + radius).toInt())

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dist = distance(x.toFloat(), y.toFloat(), cx, cy)
                val protectFactor = (dist / radius).coerceIn(0f, 1f)
                val idx = y * width + x
                mask[idx] = min(mask[idx], protectFactor)
            }
        }
    }

    /**
     * 美白：在肤色区域提升明度并轻微降低饱和度。
     */
    private fun applyWhitening(bitmap: Bitmap, faces: List<Face>, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val brightnessBoost = strength * 0.18f
        val saturationReduce = strength * 0.22f

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p ushr 16) and 0xFF) / 255f
            val g = ((p ushr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            if (isSkinColor(r, g, b)) {
                val hsl = rgbToHsl(r, g, b)
                hsl[2] = (hsl[2] + brightnessBoost * (1f - hsl[2])).coerceIn(0f, 1f)
                hsl[1] = (hsl[1] * (1f - saturationReduce)).coerceIn(0f, 1f)
                val rgb = hslToRgb(hsl[0], hsl[1], hsl[2])
                pixels[i] = rebuildPixel(p, rgb[0], rgb[1], rgb[2])
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 瘦脸：基础版本。
     *
     * 思路：取脸颊区域轮廓点，向面部中轴线方向做径向位移。
     * 对位移场覆盖范围内的像素进行反向映射采样，实现局部收缩。
     */
    private fun applyFaceSlim(bitmap: Bitmap, face: Face, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val leftCheek = face.getContour(FaceContour.LEFT_CHEEK)?.points
        val rightCheek = face.getContour(FaceContour.RIGHT_CHEEK)?.points

        if (leftCheek.isNullOrEmpty() || rightCheek.isNullOrEmpty()) {
            return bitmap
        }

        // 取脸颊中心点
        val leftCenter = averagePoint(leftCheek)
        val rightCenter = averagePoint(rightCheek)

        // 面部中心轴（鼻梁/下巴中点近似）
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val faceAxisX = nose?.x ?: ((leftCenter.x + rightCenter.x) / 2f)

        val srcPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val dstPixels = IntArray(width * height)

        // 影响半径：脸颊到中心轴距离的 1.4 倍
        val influenceRadius = maxOf(
            abs(leftCenter.x - faceAxisX),
            abs(rightCenter.x - faceAxisX)
        ) * 1.4f

        val maxOffset = strength * 18f  // 最大像素位移

        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = x.toFloat()
                val py = y.toFloat()

                // 计算该像素受左右脸颊影响的总位移
                var offsetX = 0f

                val leftDist = distance(px, py, leftCenter.x, leftCenter.y)
                if (leftDist < influenceRadius && px < faceAxisX) {
                    val w = falloff(leftDist / influenceRadius)
                    val move = maxOffset * w * (1f - (faceAxisX - px) / influenceRadius)
                    offsetX += move
                }

                val rightDist = distance(px, py, rightCenter.x, rightCenter.y)
                if (rightDist < influenceRadius && px > faceAxisX) {
                    val w = falloff(rightDist / influenceRadius)
                    val move = -maxOffset * w * (1f - (px - faceAxisX) / influenceRadius)
                    offsetX += move
                }

                val srcX = (px - offsetX).coerceIn(0f, width - 1f)
                val srcY = py

                dstPixels[y * width + x] = sampleBilinear(srcPixels, width, height, srcX, srcY)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 大眼：基础版本。
     *
     * 思路：以眼睛轮廓中心为圆心，对眼睛周围像素做径向放大。
     */
    private fun applyEyeEnlarge(bitmap: Bitmap, face: Face, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points
        val rightEye = face.getContour(FaceContour.RIGHT_EYE)?.points
        if (leftEye.isNullOrEmpty() || rightEye.isNullOrEmpty()) {
            return bitmap
        }

        val leftCenter = averagePoint(leftEye)
        val rightCenter = averagePoint(rightEye)

        // 影响半径约为眼宽
        val leftRadius = eyeRadius(leftEye)
        val rightRadius = eyeRadius(rightEye)

        val srcPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val dstPixels = IntArray(width * height)

        val scaleFactor = 1f + strength * 0.35f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = x.toFloat()
                val py = y.toFloat()

                var srcX = px
                var srcY = py

                // 左眼
                val ld = distance(px, py, leftCenter.x, leftCenter.y)
                if (ld < leftRadius) {
                    val localScale = 1f + (scaleFactor - 1f) * falloff(ld / leftRadius)
                    srcX = leftCenter.x + (px - leftCenter.x) / localScale
                    srcY = leftCenter.y + (py - leftCenter.y) / localScale
                }

                // 右眼
                val rd = distance(px, py, rightCenter.x, rightCenter.y)
                if (rd < rightRadius) {
                    val localScale = 1f + (scaleFactor - 1f) * falloff(rd / rightRadius)
                    srcX = rightCenter.x + (px - rightCenter.x) / localScale
                    srcY = rightCenter.y + (py - rightCenter.y) / localScale
                }

                srcX = srcX.coerceIn(0f, width - 1f)
                srcY = srcY.coerceIn(0f, height - 1f)

                dstPixels[y * width + x] = sampleBilinear(srcPixels, width, height, srcX, srcY)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * 红润：在脸颊区域提升红色通道与暖色调。
     */
    private fun applyBlush(bitmap: Bitmap, face: Face, strength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val leftCheek = face.getContour(FaceContour.LEFT_CHEEK)?.points
        val rightCheek = face.getContour(FaceContour.RIGHT_CHEEK)?.points
        if (leftCheek.isNullOrEmpty() || rightCheek.isNullOrEmpty()) return

        val leftCenter = averagePoint(leftCheek)
        val rightCenter = averagePoint(rightCheek)
        val radius = eyeRadius(leftCheek) * 1.6f

        val redBoost = strength * 0.18f
        val warmBoost = strength * 0.08f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val ld = distance(x.toFloat(), y.toFloat(), leftCenter.x, leftCenter.y)
                val rd = distance(x.toFloat(), y.toFloat(), rightCenter.x, rightCenter.y)
                val influence = maxOf(falloff(ld / radius), falloff(rd / radius))
                if (influence <= 0.001f) continue

                val idx = y * width + x
                val p = pixels[idx]
                var r = ((p ushr 16) and 0xFF) / 255f
                var g = ((p ushr 8) and 0xFF) / 255f
                var b = (p and 0xFF) / 255f

                if (isSkinColor(r, g, b)) {
                    r = (r + redBoost * influence).coerceIn(0f, 1f)
                    g = (g + warmBoost * influence * 0.3f).coerceIn(0f, 1f)
                    b = (b - warmBoost * influence * 0.2f).coerceIn(0f, 1f)
                    pixels[idx] = rebuildPixel(p, r, g, b)
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    // ==================== 图像处理工具 ====================

    /**
     * 按最大边限制缩放，保持宽高比。
     * 若输入尺寸已小于限制，直接返回原 Bitmap（不创建副本）。
     */
    private fun scaleToProcessDimension(bitmap: Bitmap): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= MAX_PROCESS_DIMENSION) return bitmap

        val scale = MAX_PROCESS_DIMENSION.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 快速可分离盒式模糊（水平 + 垂直两趟），近似高斯模糊。
     *
     * 复杂度 O(width * height * radius)，边界做 clamp 处理。
     */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val temp = IntArray(pixels.size)
        val output = IntArray(pixels.size)

        // 水平方向
        for (y in 0 until height) {
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var aSum = 0L
            var count = 0

            // 初始化 x = 0 处的窗口 [-radius, radius]
            for (dx in -radius..radius) {
                val px = dx.coerceIn(0, width - 1)
                val p = pixels[y * width + px]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
                count++
            }

            for (x in 0 until width) {
                temp[y * width + x] = ((aSum / count).toInt() shl 24) or
                        ((rSum / count).toInt() shl 16) or
                        ((gSum / count).toInt() shl 8) or
                        (bSum / count).toInt()

                // 窗口左移：仅当左边缘离开图像时才真正移除
                if (x - radius >= 0) {
                    val leftP = pixels[y * width + (x - radius)]
                    aSum -= (leftP ushr 24) and 0xFF
                    rSum -= (leftP ushr 16) and 0xFF
                    gSum -= (leftP ushr 8) and 0xFF
                    bSum -= leftP and 0xFF
                    count--
                }
                // 窗口右扩：仅当右边缘进入图像时才真正添加
                if (x + radius + 1 < width) {
                    val rightP = pixels[y * width + (x + radius + 1)]
                    aSum += (rightP ushr 24) and 0xFF
                    rSum += (rightP ushr 16) and 0xFF
                    gSum += (rightP ushr 8) and 0xFF
                    bSum += rightP and 0xFF
                    count++
                }
            }
        }

        // 垂直方向
        for (x in 0 until width) {
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var aSum = 0L
            var count = 0

            for (dy in -radius..radius) {
                val py = dy.coerceIn(0, height - 1)
                val p = temp[py * width + x]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
                count++
            }

            for (y in 0 until height) {
                output[y * width + x] = ((aSum / count).toInt() shl 24) or
                        ((rSum / count).toInt() shl 16) or
                        ((gSum / count).toInt() shl 8) or
                        (bSum / count).toInt()

                if (y - radius >= 0) {
                    val topP = temp[(y - radius) * width + x]
                    aSum -= (topP ushr 24) and 0xFF
                    rSum -= (topP ushr 16) and 0xFF
                    gSum -= (topP ushr 8) and 0xFF
                    bSum -= topP and 0xFF
                    count--
                }
                if (y + radius + 1 < height) {
                    val bottomP = temp[(y + radius + 1) * width + x]
                    aSum += (bottomP ushr 24) and 0xFF
                    rSum += (bottomP ushr 16) and 0xFF
                    gSum += (bottomP ushr 8) and 0xFF
                    bSum += bottomP and 0xFF
                    count++
                }
            }
        }

        return output
    }

    /**
     * 双线性采样。
     */
    private fun sampleBilinear(pixels: IntArray, width: Int, height: Int, x: Float, y: Float): Int {
        val x0 = x.toInt().coerceIn(0, width - 1)
        val y0 = y.toInt().coerceIn(0, height - 1)
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)

        val fx = x - x0
        val fy = y - y0

        val p00 = pixels[y0 * width + x0]
        val p10 = pixels[y0 * width + x1]
        val p01 = pixels[y1 * width + x0]
        val p11 = pixels[y1 * width + x1]

        return mixPixel(mixPixel(p00, p10, fx), mixPixel(p01, p11, fx), fy)
    }

    /**
     * 按 alpha 通道混合两个像素。
     */
    private fun mixPixel(a: Int, b: Int, t: Float): Int {
        val ta = ((a ushr 24) and 0xFF)
        val tb = ((b ushr 24) and 0xFF)
        val tr = mixChannel((a ushr 16) and 0xFF, (b ushr 16) and 0xFF, t)
        val tg = mixChannel((a ushr 8) and 0xFF, (b ushr 8) and 0xFF, t)
        val tbb = mixChannel(a and 0xFF, b and 0xFF, t)
        val alpha = mixChannel(ta, tb, t)
        return (alpha shl 24) or (tr shl 16) or (tg shl 8) or tbb
    }

    private fun mixChannel(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }

    /**
     * 简化肤色检测（与 CPURenderer 保持一致，基于 RGB 范围）。
     */
    private fun isSkinColor(r: Float, g: Float, b: Float): Boolean {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        return lum > 0.22f && lum < 0.95f &&
                r > 0.28f && r > g && g > b &&
                (maxC - minC) > 0.05f
    }

    /**
     * 使用新 RGB 重建像素（保留原 alpha）。
     */
    private fun rebuildPixel(original: Int, r: Float, g: Float, b: Float): Int {
        val a = (original ushr 24) and 0xFF
        return (a shl 24) or
                ((r.coerceIn(0f, 1f) * 255f).toInt() shl 16) or
                ((g.coerceIn(0f, 1f) * 255f).toInt() shl 8) or
                (b.coerceIn(0f, 1f) * 255f).toInt()
    }

    /**
     * RGB 转 HSL，返回 [h, s, l]。
     */
    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
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

    /**
     * HSL 转 RGB，返回 [r, g, b]。
     */
    private fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
        if (s < 0.0001f) return floatArrayOf(l, l, l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        return floatArrayOf(
            hueToRgb(p, q, h + 1f / 3f),
            hueToRgb(p, q, h),
            hueToRgb(p, q, h - 1f / 3f)
        )
    }

    private fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 0.5f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }

    /**
     * 平滑阶跃函数，用于掩码羽化。
     */
    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun averagePoint(points: List<PointF>): PointF {
        var sx = 0f
        var sy = 0f
        for (p in points) {
            sx += p.x
            sy += p.y
        }
        return PointF(sx / points.size, sy / points.size)
    }

    private fun eyeRadius(points: List<PointF>): Float {
        val center = averagePoint(points)
        var maxDist = 0f
        for (p in points) {
            maxDist = maxOf(maxDist, distance(p.x, p.y, center.x, center.y))
        }
        return maxDist * 2.2f
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return hypot(x2 - x1, y2 - y1)
    }

    /**
     * 径向影响衰减：余弦缓降，中心最强，边缘为 0。
     */
    private fun falloff(t: Float): Float {
        val s = (1f - t.coerceIn(0f, 1f))
        return s * s * (3f - 2f * s)
    }

    /**
     * 判断是否有任意美颜层处于激活状态。
     */
    private fun hasActiveBeauty(beauty: BeautyLevels): Boolean {
        return beauty.smooth > 0.005f ||
                beauty.whitening > 0.005f ||
                beauty.faceSlim > 0.005f ||
                beauty.eyeEnlarge > 0.005f ||
                beauty.blush > 0.005f
    }
}
