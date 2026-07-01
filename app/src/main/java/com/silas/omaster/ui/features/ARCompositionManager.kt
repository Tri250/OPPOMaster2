package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.silas.omaster.model.RectData
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * AR 构图管理器
 *
 * 负责在取景过程中实时完成：
 * 1. 主体检测与追踪（ML Kit 人脸 / 物体 / 自拍分割综合决策）
 * 2. 构图评分（三分法、黄金分割、对角线、对称、居中度、填充度、水平仪）
 * 3. 水平仪横滚角计算（RotationVector / Accelerometer+MagneticField）
 * 4. 生成 AR 引导线（三分线、黄金螺线/黄金分割线、水平线）
 * 5. 输出实时构图提示
 *
 * 所有 ML Kit 检测均通过协程挂起到 [Dispatchers.Default]，不阻塞相机线程。
 * FaceDetector / ObjectDetector / Segmenter 为懒加载单例，避免重复创建。
 */
class ARCompositionManager(context: Context) : SensorEventListener {

    /** 使用 Application Context，避免持有 Activity/Fragment 引用导致内存泄露 */
    private val appContext = context.applicationContext

    /** 内部协程作用域，用于状态流更新与资源生命周期 */
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ===== 时序稳定器：卡尔曼滤波 + 指数平滑双轨 =====
    private val subjectKalman = KalmanFilter2D(
        processNoise = 0.005f,
        measurementNoise = 0.05f
    )
    private val rollEma = EMAFilter(alpha = 0.15f)

    /** 上一帧测得的主体中心与速度，用于遮挡时预测 */
    private var lastSubjectMotion: SubjectMotion? = null

    /** 低光照增强器 */
    private val lowLightEnhancer = LowLightEnhancer()

    // ===== ML Kit 检测器（懒加载，仅在首次使用时创建） =====
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    private val objectDetector: ObjectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    private val segmenter: Segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .build()
        Segmentation.getClient(options)
    }

    // ===== 传感器与水平仪 =====
    private val sensorManager: SensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    /** 保护内部状态（roll 角、稳定后的主体框、丢失帧计数）的锁对象 */
    private val stateLock = Any()

    /** 当前设备横滚角，单位：弧度 */
    private var rollAngle: Float = 0f

    /** 经时序稳定后的主体包围框 */
    private var smoothedSubject: RectData? = null

    /** 连续未检测到主体的帧数 */
    private var lostFrames: Int = 0

    /** 是否已释放，防止 release 后重复操作 */
    @Volatile
    private var isReleased: Boolean = false

    // ===== 暴露给 UI 的状态流 =====
    private val _compositionResult = MutableStateFlow<ARCompositionResult?>(null)
    val compositionResult: StateFlow<ARCompositionResult?> = _compositionResult.asStateFlow()

    init {
        registerSensors()
    }

    /**
     * 注册传感器监听器。
     * 优先使用 TYPE_ROTATION_VECTOR，精度更高且不受磁场短时干扰；
     * 不可用时回退到加速度计 + 磁力计。
     */
    private fun registerSensors() {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            sensorManager.registerListener(
                this,
                rotationVector,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "水平仪使用 ROTATION_VECTOR 传感器")
        } else {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
                sensorManager.registerListener(
                    this,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
                Log.d(TAG, "水平仪使用 ACCELEROMETER + MAGNETIC_FIELD 传感器")
            } else {
                Log.w(TAG, "设备缺少水平仪所需传感器，水平仪功能不可用")
            }
        }
    }

    /**
     * 分析单帧图像并返回 AR 构图结果。
     *
     * 所有权契约：[bitmap] 由调用方移交给本方法，本方法会在内部处理完成后统一回收。
     * 调用方不应再访问或回收该 Bitmap。
     *
     * @param bitmap 输入图像（所有权转移给本方法）
     * @param rotationDegrees 图像顺时针旋转角度（0/90/180/270）
     * @param captureMode 当前拍摄模式，用于动态调整构图权重
     */
    suspend fun analyzeFrame(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        captureMode: CaptureMode = CaptureMode.AI_AUTO
    ): ARCompositionResult = withContext(Dispatchers.Default) {
        ensureActive()

        if (isReleased || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            if (!bitmap.isRecycled) bitmap.recycle()
            Log.w(TAG, "ARCompositionManager 已释放或输入无效，返回空结果")
            return@withContext emptyResult()
        }

        // 1. 缩放图像以降低推理开销；仅在需要时创建新 Bitmap
        val scaledBitmap = createScaledBitmapIfNeeded(bitmap)

        // 2. 低光照增强（仅在暗光下创建新 Bitmap）
        val enhancedBitmap = lowLightEnhancer.enhanceIfNeeded(scaledBitmap)

        try {
            val inputImage = InputImage.fromBitmap(enhancedBitmap, rotationDegrees)
            val imageWidth = inputImage.width.toFloat()
            val imageHeight = inputImage.height.toFloat()

            // 3. 并行执行三类检测，互不阻塞
            val (faces, objects, mask) = coroutineScope {
                val facesDeferred = async {
                    runCatching { faceDetector.process(inputImage).await() }
                        .getOrDefault(emptyList())
                }
                val objectsDeferred = async {
                    runCatching { objectDetector.process(inputImage).await() }
                        .getOrDefault(emptyList())
                }
                val maskDeferred = async {
                    runCatching { segmenter.process(inputImage).await() }
                        .getOrNull()
                }
                Triple(
                    facesDeferred.await(),
                    objectsDeferred.await(),
                    maskDeferred.await()
                )
            }

            // 3. 综合决策出当前帧主体包围框
            val measuredSubject = selectPrimarySubject(
                faces = faces,
                objects = objects,
                mask = mask,
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )

            // 4. 时序稳定（指数平滑）
            val stabilizedSubject = updateSmoothedSubject(measuredSubject)

            // 5. 读取当前水平仪横滚角
            val currentRoll = synchronized(stateLock) { rollAngle }

            // 6. 构图评分
            val score = computeCompositionScore(stabilizedSubject, currentRoll, captureMode)

            // 7. 生成引导线与提示
            val guideLines = buildGuideLines(stabilizedSubject, score, currentRoll)
            val tips = buildTips(stabilizedSubject, score, currentRoll)

            val result = ARCompositionResult(
                guideLines = guideLines,
                subjectBounds = stabilizedSubject,
                compositionScore = score,
                levelIndicator = currentRoll,
                tips = tips
            )

            _compositionResult.value = result
            result
        } catch (e: Exception) {
            Log.e(TAG, "构图分析失败: ${e.message}", e)
            emptyResult()
        } finally {
            // 回收本方法持有的所有中间 Bitmap
            if (enhancedBitmap !== scaledBitmap && !enhancedBitmap.isRecycled) {
                enhancedBitmap.recycle()
            }
            if (scaledBitmap !== bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * 释放所有检测器与传感器资源。
     * 释放后 [analyzeFrame] 将返回空结果。
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "注销传感器监听失败", e)
        }
        try {
            faceDetector.close()
        } catch (e: Exception) {
            Log.w(TAG, "释放 FaceDetector 失败", e)
        }
        try {
            objectDetector.close()
        } catch (e: Exception) {
            Log.w(TAG, "释放 ObjectDetector 失败", e)
        }
        try {
            segmenter.close()
        } catch (e: Exception) {
            Log.w(TAG, "释放 Segmenter 失败", e)
        }
        managerScope.cancel()
        synchronized(stateLock) {
            smoothedSubject = null
            lastSubjectMotion = null
            lostFrames = 0
            rollAngle = 0f
            subjectKalman.reset()
            rollEma.reset()
        }
        _compositionResult.value = null
        Log.d(TAG, "ARCompositionManager 资源已释放")
    }

    // ==================== 传感器回调 ====================

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (isReleased) return
        try {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> updateRollFromRotationVector(event.values)
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                    updateRollFromAccelMag()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                    updateRollFromAccelMag()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "传感器数据处理异常", e)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 不需要处理
    }

    /** 基于 RotationVector 计算横滚角 */
    private fun updateRollFromRotationVector(rotationVector: FloatArray) {
        try {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y,
                remappedRotationMatrix
            )
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
            synchronized(stateLock) {
                rollAngle = rollEma.update(orientationAngles[2])
            }
        } catch (e: Exception) {
            Log.w(TAG, "RotationVector 计算横滚角失败", e)
        }
    }

    /** 基于加速度计 + 磁力计计算横滚角 */
    private fun updateRollFromAccelMag() {
        try {
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )
            if (!success) return
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y,
                remappedRotationMatrix
            )
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
            synchronized(stateLock) {
                rollAngle = rollEma.update(orientationAngles[2])
            }
        } catch (e: Exception) {
            Log.w(TAG, "加速度计/磁力计计算横滚角失败", e)
        }
    }

    // ==================== 图像预处理 ====================

    /**
     * 若图像长边超过 [MAX_INPUT_SIZE]，则创建缩放副本；否则返回原图。
     * 调用方需负责回收返回值（若与原图不同）。
     */
    private fun createScaledBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = max(bitmap.width, bitmap.height)
        if (maxDim <= MAX_INPUT_SIZE) return bitmap

        val scale = MAX_INPUT_SIZE.toFloat() / maxDim
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            ?: bitmap
    }

    // ==================== 主体检测与追踪 ====================

    /**
     * 综合人脸、物体、人体掩码三类结果，选出最主要的主体。
     * 策略：
     * - 人脸框会按头肩比例外扩，保持中心不变；
     * - 物体框直接使用，并根据类别标签（人/宠物）加权；
     * - 自拍分割掩码计算人体前景包围框；
     * - 多主体场景使用综合评分：人脸优先级 > 中心位置 > 面积合适 > 类别匹配。
     */
    private fun selectPrimarySubject(
        faces: List<Face>,
        objects: List<DetectedObject>,
        mask: SegmentationMask?,
        imageWidth: Float,
        imageHeight: Float
    ): RectData? {
        val candidates = mutableListOf<SubjectCandidate>()

        // 人脸候选：优先级最高，按头肩比例外扩
        faces.forEach { face ->
            val normalized = normalizeRect(face.boundingBox, imageWidth, imageHeight)
            val expanded = expandRect(normalized, marginXFactor = 0.25f, marginYFactor = 0.4f)
            candidates.add(
                SubjectCandidate(
                    rect = expanded,
                    isFace = true,
                    isPerson = true,
                    isPet = false,
                    labelConfidence = 1.0f
                )
            )
        }

        // 物体候选：保留分类标签用于加权
        objects.forEach { obj ->
            val normalized = normalizeRect(obj.boundingBox, imageWidth, imageHeight)
            val labels = obj.labels.map { it.text.lowercase() }
            val isPerson = labels.any { it in PERSON_KEYWORDS }
            val isPet = labels.any { it in PET_KEYWORDS }
            val confidence = obj.labels.maxOfOrNull { it.confidence } ?: 0.5f
            candidates.add(
                SubjectCandidate(
                    rect = normalized,
                    isFace = false,
                    isPerson = isPerson,
                    isPet = isPet,
                    labelConfidence = confidence
                )
            )
        }

        // 分割掩码候选：视为人体前景
        mask?.let {
            bodyBoundsFromMask(it)?.let { bodyRect ->
                candidates.add(
                    SubjectCandidate(
                        rect = bodyRect,
                        isFace = false,
                        isPerson = true,
                        isPet = false,
                        labelConfidence = 0.8f
                    )
                )
            }
        }

        return candidates
            .filter { it.rect.area() in 0.005f..0.95f }
            .maxByOrNull { scoreSubjectCandidate(it) }
            ?.rect
    }

    /**
     * 单个候选主体的评分函数。
     * 评分越高越容易被选为当前主要主体。
     */
    private fun scoreSubjectCandidate(candidate: SubjectCandidate): Float {
        val rect = candidate.rect
        val area = rect.area()
        val cx = (rect.left + rect.right) / 2f
        val cy = (rect.top + rect.bottom) / 2f

        // 中心距离（越靠近画面中心越好）
        val centerDistance = hypot(cx - 0.5f, cy - 0.5f)
        val centerScore = exp(-centerDistance / 0.25f)

        // 面积分数（在 0.08 ~ 0.60 区间内最佳）
        val idealArea = 0.25f
        val areaScore = exp(-abs(area - idealArea) / 0.18f)

        // 人脸优先级最高
        val faceBonus = if (candidate.isFace) 2.5f else 1.0f
        // 人物/宠物类别加成
        val categoryBonus = when {
            candidate.isPerson -> 1.6f
            candidate.isPet -> 1.4f
            else -> 1.0f
        }

        // 过小的物体扣分
        val sizePenalty = if (area < 0.02f) 0.5f else 1.0f

        return faceBonus * categoryBonus * sizePenalty *
                (centerScore * 0.45f + areaScore * 0.35f + candidate.labelConfidence * 0.20f)
    }

    /**
     * 候选主体数据结构。
     */
    private data class SubjectCandidate(
        val rect: RectData,
        val isFace: Boolean,
        val isPerson: Boolean,
        val isPet: Boolean,
        val labelConfidence: Float
    )

    companion object {
        private const val TAG = "ARCompositionManager"

        /** 输入图像长边最大分辨率，降低 ML Kit 推理耗时与内存峰值 */
        private const val MAX_INPUT_SIZE = 640

        /** 指数平滑系数：越大响应越快，越小越稳定 */
        private const val SMOOTH_ALPHA = 0.38f

        /** 主体丢失超过该帧数后清空稳定状态 */
        private const val MAX_LOST_FRAMES = 5

        /** 分割掩码前景置信度阈值 */
        private const val MASK_THRESHOLD = 0.5f

        /** 引导线默认颜色（青色 ARGB） */
        private const val GUIDE_COLOR_MAIN: Int = 0xFF33CCFF.toInt()

        /** 主体框引导线颜色（橙色 ARGB） */
        private const val GUIDE_COLOR_SUBJECT: Int = 0xFFFF6B35.toInt()

        /** 三分线交点坐标 */
        private val RULE_OF_THIRDS_POINTS = listOf(
            1f / 3f to 1f / 3f,
            2f / 3f to 1f / 3f,
            1f / 3f to 2f / 3f,
            2f / 3f to 2f / 3f
        )

        /** 黄金分割点坐标（基于 0.618 / 0.382） */
        private val GOLDEN_POINTS = listOf(
            0.381966f to 0.381966f,
            0.618034f to 0.381966f,
            0.381966f to 0.618034f,
            0.618034f to 0.618034f
        )

        /** 人物相关关键词 */
        private val PERSON_KEYWORDS = setOf(
            "person", "people", "man", "woman", "child", "human", "face"
        )

        /** 宠物相关关键词 */
        private val PET_KEYWORDS = setOf(
            "cat", "dog", "animal", "pet", "kitten", "puppy", "feline", "canine",
            "bird", "rabbit"
        )
    }

    /** 将像素级 [Rect] 归一化为 0-1 的 [RectData] */
    private fun normalizeRect(rect: Rect, imageWidth: Float, imageHeight: Float): RectData {
        return RectData(
            left = rect.left / imageWidth,
            top = rect.top / imageHeight,
            right = rect.right / imageWidth,
            bottom = rect.bottom / imageHeight
        )
    }

    /** 按比例外扩包围框，中心点保持不变 */
    private fun expandRect(
        rect: RectData,
        marginXFactor: Float,
        marginYFactor: Float
    ): RectData {
        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        val dx = width * marginXFactor
        val dy = height * marginYFactor
        return RectData(
            left = (rect.left - dx).coerceAtLeast(0f),
            top = (rect.top - dy).coerceAtLeast(0f),
            right = (rect.right + dx).coerceAtMost(1f),
            bottom = (rect.bottom + dy).coerceAtMost(1f)
        )
    }

    /** 从自拍分割掩码中提取人体前景包围框 */
    private fun bodyBoundsFromMask(mask: SegmentationMask): RectData? {
        val width = mask.width
        val height = mask.height
        if (width <= 0 || height <= 0) return null

        val byteBuffer: ByteBuffer = mask.buffer
        byteBuffer.rewind()
        val floatBuffer: FloatBuffer = byteBuffer.asFloatBuffer()
        val confidences = FloatArray(width * height)
        floatBuffer.rewind()
        floatBuffer.get(confidences)

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        // 跳点采样以平衡精度与速度
        for (y in 0 until height step 4) {
            val rowStart = y * width
            for (x in 0 until width step 4) {
                if (confidences[rowStart + x] > MASK_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < 0 || maxY < 0) return null

        val padX = 2f / width
        val padY = 2f / height
        return RectData(
            left = (minX.toFloat() / width - padX).coerceIn(0f, 1f),
            top = (minY.toFloat() / height - padY).coerceIn(0f, 1f),
            right = (maxX.toFloat() / width + padX).coerceIn(0f, 1f),
            bottom = (maxY.toFloat() / height + padY).coerceIn(0f, 1f)
        )
    }

    /**
     * 使用卡尔曼滤波 + 指数平滑双轨稳定主体包围框。
     * - 正常检测到时：卡尔曼滤波位置，指数平滑边框尺寸；
     * - 短暂遮挡（lostFrames <= MAX_LOST_FRAMES）：用卡尔曼预测保持主体位置；
     * - 长期丢失：清空稳定状态，避免错误引导。
     */
    private fun updateSmoothedSubject(measured: RectData?): RectData? = synchronized(stateLock) {
        val nowNs = SystemClock.elapsedRealtimeNanos()
        val prevMotion = lastSubjectMotion

        if (measured != null) {
            lostFrames = 0
            val cx = (measured.left + measured.right) / 2f
            val cy = (measured.top + measured.bottom) / 2f
            val w = measured.right - measured.left
            val h = measured.bottom - measured.top

            // 计算速度，用于卡尔曼滤波和遮挡预测
            val dtSeconds = if (prevMotion != null) {
                (nowNs - prevMotion.timestampNs) / 1_000_000_000f
            } else 0.033f
            val vx = if (prevMotion != null) (cx - prevMotion.cx) / dtSeconds.coerceAtLeast(0.001f) else 0f
            val vy = if (prevMotion != null) (cy - prevMotion.cy) / dtSeconds.coerceAtLeast(0.001f) else 0f

            // 卡尔曼滤波位置
            val filteredRect = subjectKalman.predictAndUpdate(
                measuredCenter = cx to cy,
                measuredSize = w to h,
                dtSeconds = dtSeconds
            )

            // 指数平滑尺寸（保持卡尔曼中心不变）
            val smoothedRect = smoothedSubject?.let { prev ->
                val prevW = prev.right - prev.left
                val prevH = prev.bottom - prev.top
                val filteredW = filteredRect.right - filteredRect.left
                val filteredH = filteredRect.bottom - filteredRect.top
                val newW = lerp(prevW, filteredW, SMOOTH_ALPHA)
                val newH = lerp(prevH, filteredH, SMOOTH_ALPHA)
                val fCx = (filteredRect.left + filteredRect.right) / 2f
                val fCy = (filteredRect.top + filteredRect.bottom) / 2f
                RectData(
                    left = (fCx - newW / 2f).coerceIn(0f, 1f),
                    top = (fCy - newH / 2f).coerceIn(0f, 1f),
                    right = (fCx + newW / 2f).coerceIn(0f, 1f),
                    bottom = (fCy + newH / 2f).coerceIn(0f, 1f)
                )
            } ?: filteredRect

            smoothedSubject = smoothedRect
            lastSubjectMotion = SubjectMotion(cx, cy, vx, vy, nowNs)
        } else {
            lostFrames++
            if (lostFrames <= MAX_LOST_FRAMES && prevMotion != null) {
                // 遮挡期间使用卡尔曼预测保持主体位置
                val dtSeconds = (nowNs - prevMotion.timestampNs) / 1_000_000_000f
                val predicted = subjectKalman.predict(dtSeconds)
                if (predicted != null) {
                    val (pcx, pcy) = predicted
                    val prev = smoothedSubject
                    if (prev != null) {
                        val pw = prev.right - prev.left
                        val ph = prev.bottom - prev.top
                        smoothedSubject = RectData(
                            left = (pcx - pw / 2f).coerceIn(0f, 1f),
                            top = (pcy - ph / 2f).coerceIn(0f, 1f),
                            right = (pcx + pw / 2f).coerceIn(0f, 1f),
                            bottom = (pcy + ph / 2f).coerceIn(0f, 1f)
                        )
                    }
                }
            } else {
                smoothedSubject = null
                lastSubjectMotion = null
                subjectKalman.reset()
            }
        }
        smoothedSubject
    }

    // ==================== 构图评分 ====================

    /**
     * 根据主体位置、尺寸、水平仪状态与拍摄模式计算 0-100 的构图评分。
     */
    private fun computeCompositionScore(
        subject: RectData?,
        roll: Float,
        mode: CaptureMode
    ): Int {
        if (subject == null) {
            // 没有主体时，仅能基于画面中心给出较低的基础分
            val baseScore = (
                scoreRuleOfThirds(0.5f, 0.5f) * 0.3f +
                    scoreCenter(0.5f, 0.5f) * 0.3f +
                    scoreDiagonal(0.5f, 0.5f) * 0.2f +
                    scoreLevel(roll) * 0.2f
                )
            return (baseScore * 0.3f).toInt().coerceIn(0, 100)
        }

        val cx = (subject.left + subject.right) / 2f
        val cy = (subject.top + subject.bottom) / 2f
        val area = subject.area()
        val aspectRatio = subject.width() / subject.height().coerceAtLeast(0.001f)

        val weights = getWeightsForMode(mode)
        val thirdsScore = scoreRuleOfThirds(cx, cy)
        val goldenScore = scoreGoldenRatio(cx, cy)
        val centerScore = scoreCenter(cx, cy)
        val symmetryScore = scoreSymmetry(cx)
        val diagonalScore = scoreDiagonal(cx, cy)
        val fillScore = scoreFill(area, mode)
        val levelScore = scoreLevel(roll)
        val aspectScore = scoreAspectRatio(aspectRatio, mode)

        val weighted =
            thirdsScore * weights.thirds +
                goldenScore * weights.golden +
                centerScore * weights.center +
                symmetryScore * weights.symmetry +
                diagonalScore * weights.diagonal +
                fillScore * weights.fill +
                levelScore * weights.level

        // 纵横比作为 15% 权重的乘性修正，保持总分仍在 0-100 之间
        val aspectWeighted = weighted * (0.85f + 0.15f * aspectScore / 100f)

        return aspectWeighted.toInt().coerceIn(0, 100)
    }

    /** 根据拍摄模式返回各构图规则的权重，权重和为 1 */
    private fun getWeightsForMode(mode: CaptureMode): CompositionWeights {
        return when (mode) {
            CaptureMode.PORTRAIT -> CompositionWeights(
                thirds = 0.25f,
                golden = 0.15f,
                center = 0.10f,
                symmetry = 0.05f,
                diagonal = 0.05f,
                fill = 0.25f,
                level = 0.15f
            )
            CaptureMode.FOOD -> CompositionWeights(
                thirds = 0.15f,
                golden = 0.10f,
                center = 0.25f,
                symmetry = 0.10f,
                diagonal = 0.05f,
                fill = 0.30f,
                level = 0.05f
            )
            CaptureMode.STREET -> CompositionWeights(
                thirds = 0.20f,
                golden = 0.10f,
                center = 0.05f,
                symmetry = 0.05f,
                diagonal = 0.20f,
                fill = 0.20f,
                level = 0.20f
            )
            CaptureMode.PET -> CompositionWeights(
                thirds = 0.20f,
                golden = 0.10f,
                center = 0.20f,
                symmetry = 0.05f,
                diagonal = 0.05f,
                fill = 0.25f,
                level = 0.15f
            )
            // 风景、夜景、AI_AUTO、PRO、光绘等默认权重
            else -> CompositionWeights(
                thirds = 0.30f,
                golden = 0.15f,
                center = 0.05f,
                symmetry = 0.15f,
                diagonal = 0.10f,
                fill = 0.15f,
                level = 0.10f
            )
        }
    }

    /** 三分法：主体中心到最近三分线交点的距离越近得分越高 */
    private fun scoreRuleOfThirds(cx: Float, cy: Float): Float {
        val minDistance = RULE_OF_THIRDS_POINTS.minOf { hypot(cx - it.first, cy - it.second) }
        return 100f * exp(-minDistance / 0.12f)
    }

    /** 黄金分割：主体中心到最近黄金分割点的距离越近得分越高 */
    private fun scoreGoldenRatio(cx: Float, cy: Float): Float {
        val minDistance = GOLDEN_POINTS.minOf { hypot(cx - it.first, cy - it.second) }
        return 100f * exp(-minDistance / 0.14f)
    }

    /** 居中度：主体中心越接近画面中心得分越高 */
    private fun scoreCenter(cx: Float, cy: Float): Float {
        val distance = hypot(cx - 0.5f, cy - 0.5f)
        return 100f * (1f - (distance / 0.5f).coerceAtMost(1f))
    }

    /** 对称：主体中心越靠近水平中线（x=0.5）得分越高 */
    private fun scoreSymmetry(cx: Float): Float {
        return 100f * (1f - (abs(cx - 0.5f) / 0.5f).coerceAtMost(1f))
    }

    /** 对角线：主体中心到两条对角线的最近距离越小得分越高 */
    private fun scoreDiagonal(cx: Float, cy: Float): Float {
        val d1 = abs(cy - cx) / sqrt(2f)
        val d2 = abs(cy - (1f - cx)) / sqrt(2f)
        return 100f * exp(-min(d1, d2) / 0.15f)
    }

    /** 填充度：主体占画面比例越接近当前模式理想区间得分越高 */
    private fun scoreFill(area: Float, mode: CaptureMode): Float {
        val (minIdeal, maxIdeal) = when (mode) {
            CaptureMode.PORTRAIT -> 0.20f to 0.45f
            CaptureMode.FOOD -> 0.25f to 0.55f
            CaptureMode.PET -> 0.20f to 0.50f
            CaptureMode.STREET -> 0.15f to 0.45f
            else -> 0.15f to 0.40f
        }
        if (area in minIdeal..maxIdeal) return 100f
        val mid = (minIdeal + maxIdeal) / 2f
        val scale = (maxIdeal - minIdeal) / 2f + 0.08f
        return 100f * exp(-abs(area - mid) / scale)
    }

    /** 水平仪：横滚角接近 0 得分越高 */
    private fun scoreLevel(roll: Float): Float {
        val threshold = 0.05f // 约 3°
        if (abs(roll) <= threshold) return 100f
        val degrees = abs(roll) * 180f / Math.PI.toFloat()
        return max(0f, 100f - (degrees - 3f) * 2.5f)
    }

    /**
     * 纵横比匹配：主体宽高比越接近当前模式的理想比例得分越高。
     * 例如人像偏好竖向（0.6-0.8），风景偏好横向（1.5-2.0）。
     */
    private fun scoreAspectRatio(ratio: Float, mode: CaptureMode): Float {
        val idealRatio = when (mode) {
            CaptureMode.PORTRAIT -> 0.75f
            CaptureMode.FOOD -> 1.0f
            CaptureMode.PET -> 1.2f
            CaptureMode.STREET -> 1.6f
            else -> 1.0f
        }
        val logRatio = ln(ratio.coerceAtLeast(0.1f))
        val logIdeal = ln(idealRatio)
        return 100f * exp(-abs(logRatio - logIdeal) / 0.35f)
    }

    // ==================== 引导线生成 ====================

    /**
     * 根据当前主体位置、评分与水平仪状态生成 AR 引导线列表。
     * - 三分线始终作为构图基准；
     * - 水平参考线在倾斜大时变为警示色；
     * - 黄金分割线仅在主体存在且需要精调时显示，避免视觉干扰；
     * - 主体包围框随构图质量提升逐渐变细，强化“即将出片”的反馈。
     */
    private fun buildGuideLines(subject: RectData?, score: Int, roll: Float): List<ARGuideLine> {
        val lines = mutableListOf<ARGuideLine>()

        // 构图越佳，引导线越轻，减少视觉干扰
        val baseAlpha = if (score >= 90) 0x66 else 0xFF
        val mainColor = withAlpha(GUIDE_COLOR_MAIN, baseAlpha)

        // 三分线
        lines.add(ARGuideLine(1f / 3f, 0f, 1f / 3f, 1f, mainColor, 2f))
        lines.add(ARGuideLine(2f / 3f, 0f, 2f / 3f, 1f, mainColor, 2f))
        lines.add(ARGuideLine(0f, 1f / 3f, 1f, 1f / 3f, mainColor, 2f))
        lines.add(ARGuideLine(0f, 2f / 3f, 1f, 2f / 3f, mainColor, 2f))

        // 水平参考线：倾斜大时红色警示，否则青色
        val rollDegrees = abs(roll) * 180f / Math.PI.toFloat()
        val levelColor = if (rollDegrees > 5f) 0xFFFF4444.toInt() else mainColor
        lines.add(ARGuideLine(0f, 0.5f, 1f, 0.5f, levelColor, if (rollDegrees > 5f) 3f else 1.5f))

        // 黄金分割线（仅在主体存在且评分未达优秀时作为精调提示）
        if (subject != null && score in 65..<85) {
            val goldenColor = withAlpha(GUIDE_COLOR_SUBJECT, baseAlpha)
            lines.add(ARGuideLine(0.381966f, 0f, 0.381966f, 1f, goldenColor, 1.5f))
            lines.add(ARGuideLine(0.618034f, 0f, 0.618034f, 1f, goldenColor, 1.5f))
            lines.add(ARGuideLine(0f, 0.381966f, 1f, 0.381966f, goldenColor, 1.5f))
            lines.add(ARGuideLine(0f, 0.618034f, 1f, 0.618034f, goldenColor, 1.5f))
        }

        // 主体包围框
        subject?.let {
            val subjectStroke = when {
                score >= 90 -> 2f
                score >= 75 -> 2.5f
                else -> 3.5f
            }
            val subjectColor = withAlpha(GUIDE_COLOR_SUBJECT, baseAlpha)
            lines.add(ARGuideLine(it.left, it.top, it.right, it.top, subjectColor, subjectStroke))
            lines.add(ARGuideLine(it.right, it.top, it.right, it.bottom, subjectColor, subjectStroke))
            lines.add(ARGuideLine(it.right, it.bottom, it.left, it.bottom, subjectColor, subjectStroke))
            lines.add(ARGuideLine(it.left, it.bottom, it.left, it.top, subjectColor, subjectStroke))
        }

        return lines
    }

    /**
     * 调整 ARGB 颜色的 alpha 通道。
     */
    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    }

    // ==================== 实时提示 ====================

    /**
     * 根据评分、水平仪状态与主体位置生成简短提示。
     * 提示遵循 PM 验收链路：先纠水平 -> 再找主体 -> 再精调构图 -> 给出情绪反馈。
     */
    private fun buildTips(subject: RectData?, score: Int, roll: Float): String {
        // 水平仪优先提示
        val rollDegrees = abs(roll) * 180f / Math.PI.toFloat()
        if (rollDegrees > 5f) {
            return when {
                roll > 0 -> "设备向右倾斜 ${rollDegrees.toInt()}°，请向左摆正"
                else -> "设备向左倾斜 ${rollDegrees.toInt()}°，请向右摆正"
            }
        }

        if (subject == null) {
            return "未检测到主体，请将主体置于画面中"
        }

        val cx = (subject.left + subject.right) / 2f
        val cy = (subject.top + subject.bottom) / 2f
        val area = subject.area()

        // 优先提示主体位置偏移，给出明确方向
        val horizontalTip = when {
            cx < 0.30f -> "主体偏左，请向右移动"
            cx > 0.70f -> "主体偏右，请向左移动"
            else -> null
        }
        val verticalTip = when {
            cy < 0.30f -> "主体偏上，请向下移动"
            cy > 0.70f -> "主体偏下，请向上移动"
            else -> null
        }

        if (horizontalTip != null || verticalTip != null) {
            return listOfNotNull(horizontalTip, verticalTip).joinToString("；") +
                    "，使主体靠近三分线交点"
        }

        // 主体大小提示
        if (area < 0.08f) {
            return "主体较小，请靠近或放大画面"
        }
        if (area > 0.70f) {
            return "主体过大，请适当后退"
        }

        return when {
            score >= 90 -> "构图极佳，按下快门"
            score >= 80 -> "构图优美，保持当前"
            score >= 65 -> "构图不错，可轻微微调"
            else -> "尝试将主体放在三分线交点"
        }
    }

    // ==================== 工具方法 ====================

    /** 空结果兜底，用于异常或资源已释放场景 */
    private fun emptyResult(): ARCompositionResult {
        val roll = synchronized(stateLock) { rollAngle }
        return ARCompositionResult(
            guideLines = buildGuideLines(null, 0, roll),
            subjectBounds = null,
            compositionScore = 0,
            levelIndicator = roll,
            tips = "构图分析未就绪"
        )
    }

    /** 线性插值 */
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /** RectData 面积 */
    private fun RectData.area(): Float {
        return max(0f, (right - left) * (bottom - top))
    }

    /** RectData 宽度 */
    private fun RectData.width(): Float = right - left

    /** RectData 高度 */
    private fun RectData.height(): Float = bottom - top

    /** 各构图规则权重数据类 */
    private data class CompositionWeights(
        val thirds: Float,
        val golden: Float,
        val center: Float,
        val symmetry: Float,
        val diagonal: Float,
        val fill: Float,
        val level: Float
    )

    // ==================== 时序稳定与增强工具类 ====================

    /**
     * 主体运动状态（中心位置 + 速度），用于遮挡时预测。
     */
    private data class SubjectMotion(
        val cx: Float,
        val cy: Float,
        val vx: Float,
        val vy: Float,
        val timestampNs: Long
    )

    /**
     * 一维指数移动平均低通滤波器，用于平滑 roll 角。
     */
    private class EMAFilter(private val alpha: Float) {
        private var value: Float? = null
        fun update(newValue: Float): Float {
            val v = value
            val result = if (v == null) newValue else v + alpha * (newValue - v)
            value = result
            return result
        }
        fun reset() { value = null }
    }

    /**
     * 2D 卡尔曼滤波器（恒定速度模型），用于稳定主体中心位置。
     * 状态向量：[cx, cy, vx, vy]
     */
    private class KalmanFilter2D(
        private val processNoise: Float,
        private val measurementNoise: Float
    ) {
        // 状态估计 [cx, cy, vx, vy]
        private var state: FloatArray? = null
        // 误差协方差 P（4x4 对称矩阵，用 16 长度数组存储）
        private var covariance: FloatArray = FloatArray(16) { 0f }

        fun reset() {
            state = null
            covariance.fill(0f)
        }

        /**
         * 预测并更新，返回滤波后的 [cx, cy, w, h]。
         * @param measuredCenter 当前测量到的中心点
         * @param measuredSize 当前测量到的 [width, height]
         * @param dtSeconds 与上一帧的时间差（秒）
         */
        fun predictAndUpdate(
            measuredCenter: Pair<Float, Float>,
            measuredSize: Pair<Float, Float>,
            dtSeconds: Float
        ): RectData {
            val dt = dtSeconds.coerceAtLeast(0.001f)
            val (mx, my) = measuredCenter
            val (mw, mh) = measuredSize

            val s = state
            if (s == null) {
                state = floatArrayOf(mx, my, 0f, 0f)
                covariance[0] = 1f; covariance[5] = 1f
                covariance[10] = 1f; covariance[15] = 1f
                return RectData(
                    left = mx - mw / 2f,
                    top = my - mh / 2f,
                    right = mx + mw / 2f,
                    bottom = my + mh / 2f
                )
            }

            // 预测：x' = x + vx * dt
            val px = s[0] + s[2] * dt
            val py = s[1] + s[3] * dt
            val pvx = s[2]
            val pvy = s[3]

            // 预测协方差简单更新（过程噪声加到位置和速度上）
            val p00 = covariance[0] + processNoise + covariance[2] * dt + covariance[8] * dt
            val p11 = covariance[5] + processNoise + covariance[7] * dt + covariance[13] * dt
            val p22 = covariance[10] + processNoise
            val p33 = covariance[15] + processNoise

            // 更新：测量为位置 (cx, cy)
            val k0 = p00 / (p00 + measurementNoise)
            val k1 = p11 / (p11 + measurementNoise)

            val ux = px + k0 * (mx - px)
            val uy = py + k1 * (my - py)

            val vx = pvx + k0 * ((mx - px) / dt)
            val vy = pvy + k1 * ((my - py) / dt)

            state = floatArrayOf(ux, uy, vx, vy)

            covariance[0] = (1f - k0) * p00
            covariance[5] = (1f - k1) * p11
            covariance[10] = p22
            covariance[15] = p33

            return RectData(
                left = ux - mw / 2f,
                top = uy - mh / 2f,
                right = ux + mw / 2f,
                bottom = uy + mh / 2f
            )
        }

        /**
         * 仅预测，用于遮挡时估计主体位置。
         */
        fun predict(dtSeconds: Float): Pair<Float, Float>? {
            val s = state ?: return null
            val dt = dtSeconds.coerceAtLeast(0.001f)
            return Pair(s[0] + s[2] * dt, s[1] + s[3] * dt)
        }
    }

    /**
     * 低光照增强器。
     * 当检测到画面平均亮度低于阈值时，对输入 Bitmap 做伽马提升 + 轻微对比度拉伸，
     * 提高 ML Kit 在暗光下的人脸/物体检测成功率。
     */
    private class LowLightEnhancer {
        private val bufferLock = Any()
        private val reusableBuffer = IntArray(MAX_INPUT_SIZE * MAX_INPUT_SIZE)

        /**
         * 返回增强后的 Bitmap（可能为原图副本或新分配）。调用方负责回收。
         * 方法内部对共享缓冲区加锁，保证并发安全。
         */
        fun enhanceIfNeeded(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val total = width * height
            if (total > reusableBuffer.size) return bitmap

            synchronized(bufferLock) {
                bitmap.getPixels(reusableBuffer, 0, width, 0, 0, width, height)

                var sum = 0L
                for (i in 0 until total) {
                    val p = reusableBuffer[i]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF
                    sum += (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                }
                val meanLum = sum.toFloat() / total

                // 平均亮度低于 45（约暗光环境）才做增强
                if (meanLum >= 45f) return bitmap

                val gamma = 0.65f
                val gammaTable = FloatArray(256) { i ->
                    ((i / 255f).pow(gamma) * 255f).coerceIn(0f, 255f)
                }

                val output = IntArray(total)
                for (i in 0 until total) {
                    val p = reusableBuffer[i]
                    val a = (p shr 24) and 0xFF
                    val r = gammaTable[(p shr 16) and 0xFF].toInt()
                    val g = gammaTable[(p shr 8) and 0xFF].toInt()
                    val b = gammaTable[p and 0xFF].toInt()
                    output[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }

                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    ?: return bitmap
                result.setPixels(output, 0, width, 0, 0, width, height)
                return result
            }
        }
    }
}
