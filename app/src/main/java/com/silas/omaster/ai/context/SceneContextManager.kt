package com.silas.omaster.ai.context

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.model.FaceInfo
import com.silas.omaster.model.RectData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 2026 场景感知上下文管理器
 *
 * 实时融合多源数据：
 * - CameraX 预览帧（场景识别）
 * - 光照传感器（环境亮度/色温）
 * - 陀螺仪/加速度计（运动检测/稳定性）
 * - ML Kit 人脸检测（人脸位置/数量）
 *
 * 为所有拍摄模式提供统一的上下文感知能力
 */
class SceneContextManager(
    private val context: Context
) : SensorEventListener {

    // ==================== 数据模型 ====================

    /**
     * 2026 场景感知上下文
     * 融合相机帧 + 传感器 + AI识别
     */
    data class SceneContext(
        val timestamp: Long = System.currentTimeMillis(),
        val brightness: Float = 0.5f,              // 0-1，环境亮度
        val colorTemperature: Int = 5500,          // K，当前色温
        val sceneType: SceneType = SceneType.UNKNOWN,
        val subSceneType: SubSceneType = SubSceneType.UNKNOWN,
        val faceCount: Int = 0,                   // 检测到的人脸数
        val facePositions: List<FacePosition> = emptyList(),
        val subjectDistance: Float? = null,         // 主体距离（米）
        val motionLevel: MotionLevel = MotionLevel.STATIC,
        val goldenHour: Boolean = false,          // 黄金时刻
        val blueHour: Boolean = false,            // 蓝调时刻
        val handheldStability: Float = 1.0f,      // 手持稳定性 0-1
        val ambientLux: Float = 0f,               // 环境照度 (lux)
        val confidence: Float = 0f                // 识别置信度
    )

    /**
     * 人脸位置信息
     */
    data class FacePosition(
        val bounds: RectData,           // 归一化坐标 0-1
        val confidence: Float,         // 检测置信度
        val smilingProbability: Float,   // 微笑概率
        val leftEyeOpen: Boolean,
        val rightEyeOpen: Boolean,
        val headEulerAngleX: Float = 0f,  // 抬头角度
        val headEulerAngleY: Float = 0f,  // 左右转头角度
        val headEulerAngleZ: Float = 0f   // 平面旋转角度
    )

    /**
     * 场景大类
     */
    enum class SceneType {
        PORTRAIT,    // 人像
        LANDSCAPE,   // 风景
        NIGHT,       // 夜景
        FOOD,        // 美食
        URBAN,       // 城市
        MACRO,       // 微距
        ACTION,      // 运动
        DOCUMENT,    // 文档
        GROUP,       // 合影
        UNKNOWN      // 未知
    }

    /**
     * 场景细分类（50+细分类）
     */
    enum class SubSceneType {
        // 人像子类
        SINGLE_PORTRAIT, COUPLE_PORTRAIT, GROUP_PORTRAIT,
        BACKLIT_PORTRAIT, NIGHT_PORTRAIT, SELFIE,
        SPORTS_PORTRAIT, ART_PORTRAIT,
        // 夜景子类
        CITY_NIGHT, STAR_NIGHT, NEON_NIGHT, CANDLE_NIGHT,
        BLUE_HOUR_NIGHT, FIREWORKS_NIGHT,
        // 风光子类
        SUNSET, SUNRISE, BLUE_HOUR, BLUE_SKY, OVERCAST,
        FOREST, BEACH, MOUNTAIN, STREET,
        // 美食子类
        FOOD_CLOSE, FOOD_TABLE, RESTAURANT,
        // 更多...
        UNKNOWN
    }

    /**
     * 运动程度
     */
    enum class MotionLevel {
        STATIC,    // 静止 < 0.1m/s²
        LOW,       // 微动 < 0.5m/s²
        MEDIUM,    // 中等 < 1.5m/s²
        HIGH,      // 剧烈 > 1.5m/s²
        UNKNOWN
    }

    // ==================== 状态 ====================

    private val _sceneContext = MutableStateFlow(SceneContext())
    val sceneContext: StateFlow<SceneContext> = _sceneContext.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // ==================== 内部组件 ====================

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var analysisJob: Job? = null

    // 场景分析器
    private val sceneAnalyzer = HeuristicSceneAnalyzer.getInstance(context)

    // 人脸检测器（高精度模式）
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    // 传感器
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val lightSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    private val accelerometer: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val gyroscope: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    // 传感器数据
    private var currentLux: Float = 0f
    private var lastAcceleration = FloatArray(3) { 0f }
    private var lastAngularVelocity = FloatArray(3) { 0f }
    private var stabilitySamples = mutableListOf<Float>()
    private val stabilitySampleWindow = 30 // 30个样本约1秒

    // ==================== 初始化 ====================

    fun start() {
        // 注册传感器监听
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        analysisJob?.cancel()
        scope.cancel()
    }

    // ==================== 帧分析 ====================

    /**
     * 分析相机帧，更新场景上下文
     * 调用频率：建议 5-10 FPS（每 100-200ms 分析一次）
     *
     * @param image InputImage from CameraX
     */
    fun analyzeFrame(image: InputImage) {
        if (_isAnalyzing.value) return

        analysisJob?.cancel()
        analysisJob = scope.launch {
            _isAnalyzing.value = true

            try {
                // 并行执行场景分析和人脸检测
                val sceneDeferred = async { analyzeScene(image) }
                val faceDeferred = async { detectFaces(image) }

                val sceneResult = sceneDeferred.await()
                val faceResult = faceDeferred.await()

                // 计算时间因子（黄金/蓝调时刻）
                val goldenBlueHour = calculateGoldenBlueHour()

                // 计算运动等级
                val motionLevel = calculateMotionLevel()

                // 计算手持稳定性
                val stability = calculateStability()

                // 合并结果
                val newContext = SceneContext(
                    timestamp = System.currentTimeMillis(),
                    brightness = calculateBrightness(currentLux),
                    colorTemperature = estimateColorTemperature(currentLux),
                    sceneType = mapToSceneType(sceneResult),
                    subSceneType = mapToSubSceneType(sceneResult, faceResult),
                    faceCount = faceResult.size,
                    facePositions = faceResult,
                    motionLevel = motionLevel,
                    goldenHour = goldenBlueHour.first,
                    blueHour = goldenBlueHour.second,
                    handheldStability = stability,
                    ambientLux = currentLux,
                    confidence = sceneResult.confidence
                )

                _sceneContext.value = newContext

            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Frame analysis failed", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * 分析场景类型
     */
    private suspend fun analyzeScene(image: InputImage): SceneAnalysisResult {
        // 使用现有的启发式分析器获取场景信息
        // 未来可替换为更强大的模型
        return try {
            val bitmap = image.bitmapInternal
            if (bitmap != null) {
                val result = sceneAnalyzer.analyze(bitmap)
                SceneAnalysisResult(
                    primaryCategory = result.primaryScene.category.name,
                    subCategory = result.primaryScene.id,
                    confidence = result.confidence,
                    brightnessHint = result.brightnessLevel.ordinal / 4f
                )
            } else {
                SceneAnalysisResult("UNKNOWN", "unknown", 0f, 0.5f)
            }
        } catch (e: Exception) {
            SceneAnalysisResult("UNKNOWN", "unknown", 0f, 0.5f)
        }
    }

    data class SceneAnalysisResult(
        val primaryCategory: String,
        val subCategory: String,
        val confidence: Float,
        val brightnessHint: Float
    )

    /**
     * 检测人脸
     */
    private suspend fun detectFaces(image: InputImage): List<FacePosition> {
        return try {
            val faces = faceDetector.process(image).await()
            faces.map { face ->
                val bounds = face.boundingBox
                FacePosition(
                    bounds = RectData(
                        left = bounds.left.toFloat() / image.width,
                        top = bounds.top.toFloat() / image.height,
                        right = bounds.right.toFloat() / image.width,
                        bottom = bounds.bottom.toFloat() / image.height
                    ),
                    confidence = 1f,
                    smilingProbability = face.smilingProbability ?: 0f,
                    leftEyeOpen = (face.leftEyeOpenProbability ?: 0.5f) > 0.4f,
                    rightEyeOpen = (face.rightEyeOpenProbability ?: 0.5f) > 0.4f,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== 传感器数据处理 ====================

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                currentLux = event.values[0]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values)
            }
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscope(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * 处理加速度计数据（用于稳定性检测）
     */
    private fun processAccelerometer(values: FloatArray) {
        val deltaX = values[0] - lastAcceleration[0]
        val deltaY = values[1] - lastAcceleration[1]
        val deltaZ = values[2] - lastAcceleration[2]

        // 计算加速度变化量（排除重力影响）
        val motion = kotlin.math.sqrt(
            deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
        )

        stabilitySamples.add(motion)
        if (stabilitySamples.size > stabilitySampleWindow) {
            stabilitySamples.removeAt(0)
        }

        lastAcceleration = values.clone()
    }

    /**
     * 处理陀螺仪数据（用于运动检测）
     */
    private fun processGyroscope(values: FloatArray) {
        lastAngularVelocity = values.clone()
    }

    // ==================== 计算方法 ====================

    /**
     * 计算亮度等级（0-1）
     * 基于环境照度估算
     */
    private fun calculateBrightness(lux: Float): Float {
        // lux 范围映射到 0-1
        // 0 lux = 0, 1000 lux = 0.5, 10000+ lux = 1.0
        return when {
            lux <= 0 -> 0f
            lux < 100 -> 0.1f
            lux < 500 -> 0.2f
            lux < 1000 -> 0.35f
            lux < 3000 -> 0.5f
            lux < 5000 -> 0.65f
            lux < 10000 -> 0.8f
            else -> 1f
        }
    }

    /**
     * 估算色温（基于亮度近似）
     * 实际应使用色温传感器
     */
    private fun estimateColorTemperature(lux: Float): Int {
        // 简化估算：暗光偏暖，自然光偏冷
        return when {
            lux < 50 -> 3000    // 暖黄
            lux < 200 -> 4000  // 暖白
            lux < 500 -> 5000  // 中性
            lux < 2000 -> 5500 // 日光
            lux < 5000 -> 6500 // 冷白
            else -> 8000       // 偏蓝
        }
    }

    /**
     * 计算黄金时刻/蓝调时刻
     */
    private fun calculateGoldenBlueHour(): Pair<Boolean, Boolean> {
        // 获取当前时间（简化版，实际应结合 GPS 和日期计算太阳位置）
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        // 黄金时刻：日出后1小时 / 日落前1小时（约6:00-7:00 / 17:00-18:00）
        val isGoldenHour = (timeInMinutes in 340..420) || (timeInMinutes in 1020..1080)

        // 蓝调时刻：黄金时刻前后30分钟
        val isBlueHour = (timeInMinutes in 310..340) || (timeInMinutes in 1080..1110)

        return Pair(isGoldenHour, isBlueHour)
    }

    /**
     * 计算运动等级
     */
    private fun calculateMotionLevel(): MotionLevel {
        // 基于陀螺仪角速度计算
        val rotationMagnitude = kotlin.math.sqrt(
            lastAngularVelocity[0] * lastAngularVelocity[0] +
            lastAngularVelocity[1] * lastAngularVelocity[1] +
            lastAngularVelocity[2] * lastAngularVelocity[2]
        )

        return when {
            rotationMagnitude < 0.1f -> MotionLevel.STATIC
            rotationMagnitude < 0.5f -> MotionLevel.LOW
            rotationMagnitude < 1.5f -> MotionLevel.MEDIUM
            else -> MotionLevel.HIGH
        }
    }

    /**
     * 计算手持稳定性（0-1）
     */
    private fun calculateStability(): Float {
        if (stabilitySamples.isEmpty()) return 1f

        // 计算样本方差
        val avg = stabilitySamples.average()
        val variance = stabilitySamples.map { (it - avg) * (it - avg) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        // 方差越小越稳定
        // 归一化：0方差=1.0，>0.5方差=0
        return (1f - (stdDev / 0.5f).coerceAtMost(1f))
    }

    /**
     * 映射到场景大类
     */
    private fun mapToSceneType(result: SceneAnalysisResult): SceneType {
        return when (result.primaryCategory.uppercase()) {
            "PORTRAIT" -> {
                if (result.confidence > 0.7f) SceneType.PORTRAIT
                else SceneType.UNKNOWN
            }
            "LANDSCAPE" -> SceneType.LANDSCAPE
            "NIGHT" -> SceneType.NIGHT
            "FOOD" -> SceneType.FOOD
            "URBAN" -> SceneType.URBAN
            "MACRO" -> SceneType.MACRO
            "EVENT" -> SceneType.ACTION
            else -> SceneType.UNKNOWN
        }
    }

    /**
     * 映射到场景细分类
     */
    private fun mapToSubSceneType(
        sceneResult: SceneAnalysisResult,
        faces: List<FacePosition>
    ): SubSceneType {
        // 人像相关
        if (sceneResult.primaryCategory == "PORTRAIT") {
            return when {
                faces.size == 1 -> SubSceneType.SINGLE_PORTRAIT
                faces.size == 2 -> SubSceneType.COUPLE_PORTRAIT
                faces.size > 2 -> SubSceneType.GROUP_PORTRAIT
                sceneResult.brightnessHint < 0.2f -> SubSceneType.NIGHT_PORTRAIT
                else -> SubSceneType.SINGLE_PORTRAIT
            }
        }

        // 夜景相关
        if (sceneResult.primaryCategory == "NIGHT" || sceneResult.brightnessHint < 0.2f) {
            return when {
                sceneResult.subCategory.contains("star") -> SubSceneType.STAR_NIGHT
                sceneResult.subCategory.contains("neon") -> SubSceneType.NEON_NIGHT
                else -> SubSceneType.CITY_NIGHT
            }
        }

        // 风光相关
        if (sceneResult.primaryCategory == "LANDSCAPE") {
            return when {
                sceneResult.subCategory.contains("sunset") -> SubSceneType.SUNSET
                sceneResult.subCategory.contains("sunrise") -> SubSceneType.SUNRISE
                sceneResult.subCategory.contains("sky") -> SubSceneType.BLUE_SKY
                sceneResult.subCategory.contains("forest") -> SubSceneType.FOREST
                else -> SubSceneType.STREET
            }
        }

        return SubSceneType.UNKNOWN
    }

    companion object {
        private const val TAG = "SceneContextManager"

        @Volatile
        private var instance: SceneContextManager? = null

        fun getInstance(context: Context): SceneContextManager {
            return instance ?: synchronized(this) {
                instance ?: SceneContextManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
