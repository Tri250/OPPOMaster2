# 哈苏之眼 2.0 全面升级方案

## 2026年旗舰级智能摄影系统 · OPPO Find X 用户专属

---

## 一、现状分析

### 1.1 现有架构回顾

| 模块 | 现状 | 评分 |
|------|------|------|
| **场景识别** | HeuristicSceneAnalyzer 启发式分析 + ML Kit 人脸检测 | ⭐⭐⭐ |
| **色彩引擎** | HasselbladColorEngine ColorMatrix 处理管线 | ⭐⭐⭐⭐ |
| **相机集成** | OPPOCameraManager ContentProvider/Settings/Intent/Clipboard 四级降级 | ⭐⭐⭐⭐ |
| **实时预览** | CameraXManager + ImageAnalysis，每3帧处理1帧 | ⭐⭐⭐ |
| **拍摄模式** | 仅有基础场景模式选择，无专业模式 | ⭐⭐ |
| **AI构图** | 基础构图指南，无 AR 引导 | ⭐⭐ |
| **美化引擎** | 预设参数叠加，无实时分层处理 | ⭐⭐ |

### 1.2 核心差距分析

```
┌─────────────────────────────────────────────────────────────┐
│                    2026年旗舰标准                              │
├─────────────────────────────────────────────────────────────┤
│  ✦ 一键场景扫描 + AI构图建议 + AR实时引导                      │
│  ✦ 专业模式：长曝光/光绘/多帧降噪/专业手动                      │
│  ✦ 实时分层美化：美颜/光效/滤镜三链路并行                      │
│  ✦ 计算摄影：单帧HDR/夜景合成/AI超分                           │
│  ✦ 交互标准：手势操作/智能模式切换/上下文感知                   │
└─────────────────────────────────────────────────────────────┘
                              ↓ 差距
┌─────────────────────────────────────────────────────────────┐
│                   现有OMaster能力                              │
├─────────────────────────────────────────────────────────────┤
│  ✗ 无一键扫描分析                                            │
│  ✗ 无专业拍摄模式（长曝光/光绘/专业手动）                       │
│  ✗ 实时预览仅 ColorMatrix 叠加，每3帧1处理                     │
│  ✗ 无分层美颜/实时光效/滤镜实时预览                            │
│  ✗ 无夜景多帧合成/AI超分                                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、2026 旗舰级架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         哈苏之眼 2.0 系统架构                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │  智能拍摄入口层  │    │   场景感知层    │    │   用户交互层     │         │
│  │  SmartCapture   │    │  SceneContext   │    │   UIXLayer      │         │
│  │  EntryPoint     │    │   Manager      │    │                 │         │
│  └────────┬────────┘    └────────┬────────┘    └────────┬────────┘         │
│           │                      │                      │                  │
│           ▼                      ▼                      ▼                  │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │                      AI 感知决策引擎                         │           │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │           │
│  │  │ SceneAI  │  │FaceAI   │  │ GestureAI│  │ LightAI │   │           │
│  │  │ Analyzer │  │ Detector │  │ Recognizer│ │ Analyzer │   │           │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │           │
│  │       └────────────┴────────────┴────────────┘            │           │
│  │                         │                                   │           │
│  │                         ▼                                   │           │
│  │              ┌────────────────────┐                        │           │
│  │              │   CompositionAI    │ ◄── AI自动构图引擎       │           │
│  │              │      Engine       │                        │           │
│  │              └─────────┬─────────┘                        │           │
│  └────────────────────────┼────────────────────────────────────┘           │
│                           │                                              │
│                           ▼                                              │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │                      拍摄模式矩阵                             │           │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │           │
│  │  │ 人像模式 │ │ 夜景模式 │ │光绘模式  │ │专业模式  │          │           │
│  │  │PortraitAI│ │NightSynth│ │LightPain│ │ProManual│          │           │
│  │  │+姿势引导 │ │+多帧降噪 │ │+车流水  │ │+全手动  │          │           │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │           │
│  └─────────────────────────┬───────────────────────────────┘           │
│                            │                                           │
│                            ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐         │
│  │                    实时美化引擎 3.0                           │         │
│  │  ┌─────────┐    ┌─────────┐    ┌─────────┐                 │         │
│  │  │ SkinAI  │ +  │ LightFX │ +  │ LUT     │ = 实时输出       │         │
│  │  │美颜引擎  │    │光效引擎  │    │滤镜引擎  │                 │         │
│  │  └─────────┘    └─────────┘    └─────────┘                 │         │
│  └─────────────────────────────────────────────────────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心模块规格

#### 2.2.1 场景感知引擎 (SceneContextManager)

```kotlin
/**
 * 2026 场景感知上下文
 * 实时融合：相机帧 / 光照传感器 / 陀螺仪 / AI识别
 */
data class SceneContext(
    val timestamp: Long,
    val brightness: Float,              // 0-1，环境亮度
    val colorTemperature: Int,           // K，当前色温
    val sceneType: SceneType,           // 场景大类
    val subSceneType: SubSceneType,     // 场景细分类
    val faceCount: Int,                 // 检测到的人脸数
    val facePositions: List<FaceRect>,  // 人脸位置
    val subjectDistance: Float?,         // 主体距离（TOF/激光对焦）
    val motionLevel: MotionLevel,        // 运动程度
    val goldenHour: Boolean,            // 黄金时刻
    val handheldStability: Float         // 手持稳定性 0-1
)

enum class SceneType {
    PORTRAIT, LANDSCAPE, NIGHT, FOOD,
    URBAN, MACRO, ACTION, DOCUMENT, UNKNOWN
}

enum class SubSceneType {
    // 人像子类
    SINGLE_PORTRAIT, COUPLE_PORTRAIT, GROUP_PORTRAIT,
    BACKLIT_PORTRAIT, NIGHT_PORTRAIT, SELFIE,
    // 夜景子类
    CITY_NIGHT, STAR_NIGHT, NEON_NIGHT, CANDLE_NIGHT,
    // 风光子类
    SUNSET, SUNRISE, BLUE_HOUR, BLUE_SKY, OVERCAST,
    // ... 扩展至50+ 细分类
}

enum class MotionLevel {
    STATIC,     // 静止 < 0.1m/s
    LOW,        // 微动 < 0.5m/s
    MEDIUM,     // 中等 < 1.5m/s
    HIGH        // 剧烈 > 1.5m/s
}
```

#### 2.2.2 AI构图引擎 (CompositionAIEngine)

```kotlin
/**
 * 2026 AI智能构图系统
 * 支持：专业构图规则 + 动态构图 + AR引导叠加
 */
object CompositionAIEngine {

    // 支持的构图法则（可组合）
    enum class CompositionRule {
        RULE_OF_THIRDS,      // 三分法
        GOLDEN_RATIO,       // 黄金分割
        DIAGONAL,            // 对角线
        TRIANGLE,            // 三角形
        LEADING_LINES,      // 引导线
        SYMMETRY,           // 对称
        FRAME_WITHIN_FRAME, // 框架构图
        NEGATIVE_SPACE,      // 留白
        DEPTH_LAYERS,       // 景深分层
        DYNAMIC_DIAGONAL    // 动态对角
    }

    /**
     * 构图分析结果
     */
    data class CompositionResult(
        val primaryRule: CompositionRule,           // 主构图法则
        val confidence: Float,                       // 置信度 0-1
        val suggestedCrop: Rect,                    // 建议裁剪区域（归一化坐标）
        val guideOverlays: List<GuideOverlay>,      // AR引导线
        val tips: List<String>,                      // 拍摄建议
        val score: Float,                            // 整体构图评分 0-100
        val alternatives: List<CompositionRule>      // 备选构图法则
    )

    /**
     * AR引导线
     */
    data class GuideOverlay(
        val type: GuideType,
        val startPoint: PointF,      // 归一化坐标
        val endPoint: PointF,
        val style: GuideStyle,
        val label: String?,           // 可选标签
        val animationHint: String?    // 动画提示
    )

    enum class GuideType {
        GRID,           // 三分网格
        GOLDEN_GRID,   // 黄金网格
        DIAGONAL,      // 对角线
        HORIZON,       // 地平线
        FACE_DETECT,   // 人脸框
        SUBJECT_BOX,   // 主体框
        LEADING_LINE,  // 引导线
        TILT_HINT      // 倾斜提示
    }

    data class GuideStyle(
        val color: Int = 0xFFFFFFFF.toInt(),
        val strokeWidth: Float = 2f,
        val alpha: Float = 0.8f,
        val dashEffect: Boolean = false
    )
}
```

---

## 三、智能拍摄模式矩阵

### 3.1 模式概览

| 模式 | 核心能力 | AI加持 | 目标场景 |
|------|---------|--------|---------|
| **人像模式** | 姿势引导 + 背景虚化 + 分层美颜 | 场景适配 + 眼神光检测 | 单人/合影/逆光 |
| **夜景模式** | 多帧合成 + 降噪 + 手持检测 | 场景判断 + 张数自适应 | 城市/星空/美食夜景 |
| **光绘模式** | 长曝光 + 光轨叠加 + 车流 | 运动轨迹检测 + 时长推荐 | 车流/流水/光绘 |
| **专业模式** | 全手动 + 直方图 + 斑马纹 | 曝光预测 + 安全快门 | 高阶用户 |

### 3.2 人像模式 2.0

```kotlin
/**
 * 人像模式核心功能
 * 2026 差异化：姿势引导 + 眼神光 + 分层美颜
 */
@Composable
fun PortraitModeCapture(
    onPoseGuidance: (PoseGuide) -> Unit,
    onBeautyApplied: (BeautyParams) -> Unit,
    onCapture: (PortraitCaptureResult) -> Unit
) {
    // 1. 实时姿势检测
    val poseDetector = remember { PoseDetector.getInstance() }

    // 2. 虚化引擎
    val bokehEngine = remember { BokehEngine() }

    // 3. 分层美颜
    val beautyEngine = remember { BeautyEngine3D() }

    // 姿势引导状态
    var currentPoseGuide by remember { mutableStateOf<PoseGuide?>(null) }

    // 实时姿势分析
    LaunchedEffect(frameBitmap) {
        val poseResult = poseDetector.analyze(frameBitmap)
        if (poseResult.confidence > 0.7f) {
            // 匹配最佳姿势模板
            val matchedPose = PoseTemplateDatabase.match(poseResult)
            currentPoseGuide = PoseGuide(
                template = matchedPose,
                currentPose = poseResult,
                feedback = generatePoseFeedback(poseResult, matchedPose),
                qualityScore = calculatePoseScore(poseResult, matchedPose)
            )
            onPoseGuidance(currentPoseGuide!!)
        }
    }
}

/**
 * 姿势引导数据
 */
data class PoseGuide(
    val template: PoseTemplate,           // 目标姿势模板
    val currentPose: DetectedPose,        // 当前检测到的姿势
    val feedback: List<PoseFeedback>,     // 实时反馈
    val qualityScore: Float,              // 姿势质量评分 0-100
    val recommendedAngle: Float?,         // 建议调整角度
    val recommendedDistance: Float?       // 建议调整距离
)

/**
 * 姿势模板库（50+预设）
 */
object PoseTemplateDatabase {
    val templates = mapOf(
        // 通用人像
        "portrait-basic-01" to PoseTemplate(
            name = "自然站姿",
            description = "自然站立，一只手插兜",
            keyPoints = mapOf(
                PoseKey.LEFT_SHOULDER to PointRule(atPercent(0.35f), atPercent(0.4f)),
                PoseKey.RIGHT_SHOULDER to PointRule(atPercent(0.65f), atPercent(0.4f)),
                PoseKey.HEAD to PointRule(atPercent(0.5f), atPercent(0.25f)),
                // ...
            ),
           适用场景 = listOf(SceneType.PORTRAIT, SceneType.URBAN)
        ),
        // 情侣pose
        "couple-holding-hands" to PoseTemplate(...),
        // 逆光pose
        "backlit-silhouette" to PoseTemplate(...),
        // 更多模板...
    )
}

/**
 * 分层美颜参数（3D LUT + 区域识别）
 */
data class BeautyParams(
    val skinSmoothing: Float = 0.5f,      // 磨皮 0-1
    val skinBrightening: Float = 0.3f,    // 美白 0-1
    val faceSlimming: Float = 0f,         // 瘦脸 -0.5 ~ 0.5
    val eyeEnlarging: Float = 0.3f,       // 大眼 0-1
    val eyeBrightening: Float = 0.4f,     // 眼神光 0-1
    val lipColor: Float = 0f,             // 口红 -1~1
    val cheekBlush: Float = 0f,           // 腮红 0-1
    val noseReshape: Float = 0f,         // 鼻型 0-1
    val jawlineDefine: Float = 0f         // 下颌线 0-1
)
```

### 3.3 夜景模式 2.0

```kotlin
/**
 * 夜景合成引擎
 * 2026 差异化：AI场景判断 + 自适应张数 + 手持检测
 */
class NightModeEngine(private val context: Context) {

    /**
     * 夜景合成配置
     */
    data class NightSynthConfig(
        val mode: NightMode,
        val frameCount: Int,           // 自动计算
        val exposureBrackets: List<Float>,  // 曝光包围
        val useGimbel: Boolean,        // 是否使用云台
        val sceneType: NightSceneType
    )

    enum class NightMode {
        HANDHELD_STANDARD,    // 手持标准（4-8张）
        HANDHELD_LUXURY,      // 手持极致（8-16张）
        TRIPOD_CLASSIC,      // 脚架经典（多帧长曝光）
        TRIPOD_HDR,          // 脚架HDR（包围曝光）
        STAR_TRACK,           // 星轨模式
        LIGHT_TRAIL           // 光轨模式
    )

    enum class NightSceneType {
        CITY_SCAPE,       // 城市夜景
        NEON_LIGHTS,      // 霓虹灯光
        STAR_NIGHT,       // 星空
        BLUE_HOUR,        // 蓝调时刻
        CANDLE_LIGHT,     // 烛光环境
        STREET_NIGHT      // 街拍夜景
    }

    /**
     * 智能帧数计算
     * 根据场景和手持稳定性自动决定合成张数
     */
    fun calculateOptimalFrameCount(
        context: SceneContext,
        deviceStability: Float  // 0-1
    ): NightSynthConfig {
        val brightness = context.brightness
        val motion = context.motionLevel

        return when {
            // 极暗环境 + 高稳定性 → 脚架模式
            brightness < 0.1f && deviceStability > 0.9f -> NightSynthConfig(
                mode = NightMode.TRIPOD_CLASSIC,
                frameCount = 8,
                exposureBrackets = listOf(0.5f, 1f, 2f),
                useGimbel = true,
                sceneType = detectNightSceneType(context)
            )

            // 低亮度 + 低稳定性 → 手持Luxury
            brightness < 0.2f && deviceStability < 0.6f -> NightSynthConfig(
                mode = NightMode.HANDHELD_LUXURY,
                frameCount = 16,
                exposureBrackets = listOf(1f),
                useGimbel = false,
                sceneType = detectNightSceneType(context)
            )

            // 中等亮度 → 手持标准
            else -> NightSynthConfig(
                mode = NightMode.HANDHELD_STANDARD,
                frameCount = calculateFrameByBrightness(brightness),
                exposureBrackets = listOf(1f),
                useGimbel = false,
                sceneType = detectNightSceneType(context)
            )
        }
    }

    /**
     * AI对齐 + 合成管线
     */
    suspend fun synthesize(
        frames: List<Bitmap>,
        config: NightSynthConfig,
        progressCallback: (Float) -> Unit
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: AI对齐（检测并修正轻微位移）
        progressCallback(0.1f)
        val alignedFrames = alignFramesAI(frames)

        // Step 2: 场景检测选择合成策略
        progressCallback(0.2f)
        val strategy = selectSynthesisStrategy(config.sceneType)

        // Step 3: 多帧合成
        progressCallback(0.3f)
        val synthesized = when (strategy) {
            SynthesisStrategy.MTFUSION -> mtfusion(alignedFrames)
            SynthesisStrategy.HDR_MERGE -> hdrMerge(alignedFrames, config.exposureBrackets)
            SynthesisStrategy.MEDIAN -> medianBlend(alignedFrames)
        }

        // Step 4: AI降噪（基于场景的降噪强度）
        progressCallback(0.7f)
        val denoised = aiDenoise(synthesized, config.sceneType)

        // Step 5: 色彩增强（夜景氛围优化）
        progressCallback(0.9f)
        val enhanced = enhanceNightColors(denoised, config.sceneType)

        progressCallback(1.0f)
        enhanced
    }

    /**
     * AI对齐算法（基于特征点匹配）
     */
    private suspend fun alignFramesAI(frames: List<Bitmap>): List<Bitmap> {
        // 使用 ORB 特征点 + RANSAC 匹配
        // 替代传统模板匹配，提升对齐精度
        val reference = frames.first()
        return frames.map { frame ->
            val transform = calculateTransform(reference, frame)
            applyGeometricTransform(frame, transform)
        }
    }
}
```

### 3.4 光绘/长曝光模式

```kotlin
/**
 * 光绘模式引擎
 * 2026 差异化：轨迹检测 + 实时预览 + 智能时长推荐
 */
class LightPaintingEngine {

    data class LightPaintingConfig(
        val mode: LightPaintingMode,
        val duration: Duration,         // 曝光时长
        val maxLightTrails: Int,         // 最大光轨层数
        val autoDetectMotion: Boolean,   // 自动检测运动轨迹
        val previewLive: Boolean         // 实时预览
    )

    enum class LightPaintingMode {
        CAR_LIGHTS,      // 车流
        WATERFALL,       // 瀑布流水
        SPARKLER,        // 烟火/仙女棒
        STARS,           // 星星轨迹
        CUSTOM           // 自定义
    }

    /**
     * 实时轨迹检测
     */
    fun detectLightTrails(
        currentFrame: Bitmap,
        previousFrame: Bitmap
    ): List<LightTrail> {
        // 帧差分 + 形态学处理
        val diff = computeFrameDifference(currentFrame, previousFrame)
        val binary = thresholdAdaptive(diff)
        val morph = morphologicalOperations(binary)
        return extractContours(morph).map { contour ->
            LightTrail(
                points = contour.points,
                intensity = calculateIntensity(contour),
                direction = estimateDirection(contour)
            )
        }
    }

    /**
     * 智能时长推荐
     */
    fun recommendDuration(
        sceneContext: SceneContext,
        detectedMotion: MotionLevel
    ): Duration {
        return when (detectedMotion) {
            MotionLevel.LOW -> when (sceneContext.sceneType) {
                SceneType.LANDSCAPE -> Duration.ofSeconds(5)
                else -> Duration.ofSeconds(2)
            }
            MotionLevel.MEDIUM -> Duration.ofSeconds(3)
            MotionLevel.HIGH -> Duration.ofSeconds(1)
        }
    }
}
```

### 3.5 专业模式

```kotlin
/**
 * 专业模式 ViewModel
 * 2026 差异化：曝光预测 + 安全快门 + 直方图实时
 */
class ProModeViewModel : ViewModel() {

    // 全手动参数
    private val _iso = MutableStateFlow(100)
    val iso: StateFlow<Int> = _iso.asStateFlow()

    private val _shutterSpeed = MutableStateFlow("1/125")
    val shutterSpeed: StateFlow<String> = _shutterSpeed.asStateFlow()

    private val _whiteBalance = MutableStateFlow(5500)
    val whiteBalance: StateFlow<Int> = _whiteBalance.asStateFlow()

    private val _focusDistance = MutableStateFlow(2.0f)
    val focusDistance: StateFlow<Float> = _focusDistance.asStateFlow()

    private val _exposureCompensation = MutableStateFlow(0f)
    val exposureCompensation: StateFlow<Float> = _exposureCompensation.asStateFlow()

    // 直方图数据（实时）
    private val _histogram = MutableStateFlow<HistogramData?>(null)
    val histogram: StateFlow<HistogramData?> = _histogram.asStateFlow()

    // 斑马纹状态
    private val _zebraPattern = MutableStateFlow(false)
    val zebraPattern: StateFlow<Boolean> = _zebraPattern.asStateFlow()

    // 安全快门建议
    val safeShutterSpeed: StateFlow<String> = derivedStateOf {
        val focalLength = currentFocalLength.value
        val minShutter = 1 / (focalLength * 1.5)  // 安全快门 = 1/(焦距×1.5)
        "1/${(minShutter).toInt().coerceAtLeast(500)}"
    }

    // 曝光预测
    val exposurePrediction: StateFlow<ExposurePrediction> = derivedStateOf {
        calculateExposure(
            iso.value,
            parseShutterSpeed(shutterSpeed.value),
            aperture.value,
            sceneContext.brightness
        )
    }

    /**
     * 曝光状态指示
     */
    data class ExposurePrediction(
        val status: ExposureStatus,  // UNDER / OK / OVER
        val stops: Float,            // 与正确曝光的差距（档数）
        val histogram: IntArray,      // 模拟直方图
        val highlightClipping: Boolean,
        val shadowClipping: Boolean
    )

    enum class ExposureStatus { UNDER, OK, OVER }
}

/**
 * 斑马纹渲染
 * 检测过曝区域（亮度 > threshold）并叠加斜线图案
 */
@Composable
fun ZebraPatternOverlay(
    bitmap: Bitmap,
    threshold: Float = 0.9f,
    enabled: Boolean
) {
    if (!enabled) return

    val zebraBitmap = remember(bitmap, threshold) {
        renderZebraPattern(bitmap, threshold)
    }

    Image(
        bitmap = zebraBitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        alpha = 0.6f
    )
}
```

---

## 四、实时美化引擎 3.0

### 4.1 架构设计

```
┌──────────────────────────────────────────────────────────────────────┐
│                    实时美化引擎 3.0 三链路并行架构                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   输入帧 ──┬──► 美颜链路 ──► SkinAI ──► 分层磨皮 ──► 立体美颜          │
│           │                                                           │
│           ├──► 光效链路 ──► 眼神光 ──► 补光 ──► 氛围光效                 │
│           │                                                           │
│           └──► 滤镜链路 ──► LUT 3D ──► 风格化 ──► 叠加输出               │
│                                                                       │
│                           GPU 加速 (RenderScript/OpenGL)               │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 核心实现

```kotlin
/**
 * 实时美化引擎
 * 三链路并行处理，GPU加速
 */
class RealTimeBeautyEngine(private val context: Context) {

    // 美颜链路处理器
    private val skinProcessor = SkinAIProcessor()
    private val lightFXProcessor = LightFXProcessor()
    private val lutProcessor = LUTProcessor()

    /**
     * 实时美化处理
     * @param source 输入帧
     * @param beautyParams 美颜参数
     * @param lightFXParams 光效参数
     * @param lutId 当前滤镜ID（null表示无滤镜）
     * @return 处理后帧
     */
    fun processRealTime(
        source: Bitmap,
        beautyParams: BeautyParams,
        lightFXParams: LightFXParams,
        lutId: String?
    ): Bitmap {
        // 1. 人脸关键点检测（用于美颜定位）
        val faceLandmarks = detectFaceLandmarks(source)

        // 2. 三链路并行处理
        val skinResult = skinProcessor.process(
            source, beautyParams, faceLandmarks
        )

        val lightResult = lightFXProcessor.process(
            skinResult, lightFXParams, faceLandmarks
        )

        val lutResult = lutId?.let { lutProcessor.apply(it, lightResult) }
            ?: lightResult

        return lutResult
    }

    /**
     * 分层美颜处理
     * 基于人脸关键点的区域识别，实现精细化美颜
     */
    inner class SkinAIProcessor {
        fun process(
            source: Bitmap,
            params: BeautyParams,
            landmarks: FaceLandmarks
        ): Bitmap {
            val result = source.copy(Bitmap.Config.ARGB_8888, true)

            // 分区域处理
            // 1. 全局磨皮（保边去噪）
            applySkinSmoothing(result, params.skinSmoothing, landmarks.faceRegion)

            // 2. 区域美颜
            // 眼睛区域：大眼 + 眼神光
            applyEyeEnhancement(result, params, landmarks.eyeRegions)

            // 鼻子区域：鼻型重塑
            applyNoseReshape(result, params, landmarks.noseRegion)

            // 下颌区域：瘦脸 + 下颌线
            applyJawEnhancement(result, params, landmarks.jawRegion)

            // 嘴唇区域：口红 + 唇色
            applyLipEnhancement(result, params, landmarks.lipRegion)

            // 腮红（可选）
            if (params.cheekBlush > 0) {
                applyCheekBlush(result, params, landmarks.cheekRegions)
            }

            return result
        }

        /**
         * 保边去噪磨皮
         * 使用双边滤波器 + AI皮肤检测
         */
        private fun applySkinSmoothing(
            bitmap: Bitmap,
            strength: Float,
            faceRegion: Rect
        ) {
            // 双边滤波参数根据强度自适应
            val d = (5 + strength * 15).toInt().coerceIn(5, 20)
            val sigmaColor = (20 + strength * 40).toFloat()
            val sigmaSpace = (30 + strength * 50).toFloat()

            // 仅在皮肤区域应用
            applyBilateralFilter(bitmap, faceRegion, d, sigmaColor, sigmaSpace)
        }

        /**
         * 眼神光增强
         */
        private fun applyEyeEnhancement(
            bitmap: Bitmap,
            params: BeautyParams,
            eyeRegions: List<Rect>
        ) {
            eyeRegions.forEach { region ->
                // 大眼变形
                if (params.eyeEnlarging > 0) {
                    applyFaceWarping(bitmap, region, params.eyeEnlarging * 0.3f)
                }

                // 眼神光（高光点）
                if (params.eyeBrightening > 0) {
                    addEyeHighlight(bitmap, region, params.eyeBrightening)
                }
            }
        }
    }

    /**
     * 光效处理器
     */
    inner class LightFXProcessor {
        fun process(
            source: Bitmap,
            params: LightFXParams,
            landmarks: FaceLandmarks
        ): Bitmap {
            var result = source

            // 眼神光（3D球体高光）
            if (params.eyeLight > 0) {
                result = addEyeCatchLight(result, landmarks.eyeRegions, params.eyeLight)
            }

            // 全局补光
            if (params.globalFillLight > 0) {
                result = applyFillLight(result, params.globalFillLight)
            }

            // 氛围光效
            if (params.ambientEffect != null) {
                result = applyAmbientEffect(result, params.ambientEffect)
            }

            return result
        }

        /**
         * 眼神光（3D球体模拟）
         */
        private fun addEyeCatchLight(
            bitmap: Bitmap,
            eyeRegions: List<Rect>,
            intensity: Float
        ): Bitmap {
            val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)

            eyeRegions.forEach { region ->
                // 3D高光球体模拟
                val gradient = RadialGradient(
                    region.left + region.width() * 0.3f,
                    region.top + region.height() * 0.3f,
                    region.width() * 0.15f,
                    intArrayOf(
                        Color.WHITE,
                        Color.argb((255 * intensity).toInt(), 255, 255, 255),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )

                val paint = Paint().apply {
                    shader = gradient
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                }

                canvas.drawRect(region, paint)
            }

            return result
        }
    }
}

/**
 * 光效参数
 */
data class LightFXParams(
    val eyeLight: Float = 0f,              // 眼神光 0-1
    val globalFillLight: Float = 0f,       // 全局补光 0-1
    val ambientEffect: AmbientEffect? = null
)

enum class AmbientEffect {
    SUNNY,       // 阳光感
    GOLDEN,      // 黄金时刻
    COOL,        // 冷调
    WARM,        // 暖调
    NEON,        // 霓虹
    FILM         // 胶片感
}
```

### 4.3 滤镜实时预览

```kotlin
/**
 * 实时滤镜预览
 * 支持 LUT 3D + 基础调整叠加
 */
@Composable
fun FilterPreviewStrip(
    currentBitmap: Bitmap,
    availableFilters: List<FilterItem>,
    selectedFilterId: String?,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 原图选项
        FilterThumbnail(
            name = "原图",
            bitmap = currentBitmap,
            isSelected = selectedFilterId == null,
            onClick = { onFilterSelected("") }
        )

        // LUT 滤镜预览（异步生成缩略图）
        availableFilters.forEach { filter ->
            val previewBitmap = remember(filter.id, currentBitmap) {
                produceState<Bitmap?>(null, filter.id, currentBitmap) {
                    value = withContext(Dispatchers.Default) {
                        LUTProcessor.applyToBitmap(currentBitmap, filter.id, 0.8f)
                    }
                }
            }

            FilterThumbnail(
                name = filter.name,
                bitmap = previewBitmap.value ?: currentBitmap,
                isSelected = selectedFilter.id == filter.id,
                onClick = { onFilterSelected(filter.id) }
            )
        }
    }
}

/**
 * 滤镜数据
 */
data class FilterItem(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val lutId: String?,          // LUT 3D ID（可选）
    val adjustments: Adjustments // 基础调整（可选）
)

data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val vignette: Float = 0f
)
```

---

## 五、AI 场景扫描 + 自动构图

### 5.1 一键扫描流程

```
┌────────────────────────────────────────────────────────────────────────┐
│                         一键 AI 场景扫描流程                             │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   用户点击「扫描」 ──► 相机预览帧捕获 ──► 并行AI分析                       │
│                                         │                              │
│                    ┌────────────────────┼────────────────────┐          │
│                    ▼                    ▼                    ▼          │
│              ┌─────────┐          ┌─────────┐          ┌─────────┐   │
│              │场景识别  │          │构图分析  │          │光照分析  │   │
│              │SceneAI  │          │CompAI   │          │LightAI  │   │
│              └────┬────┘          └────┬────┘          └────┬────┘   │
│                   │                    │                    │          │
│                   └────────────────────┼────────────────────┘          │
│                                        ▼                                │
│                            ┌─────────────────────┐                      │
│                            │   智能推荐生成器    │                       │
│                            │ RecommendationHub  │                       │
│                            └──────────┬──────────┘                      │
│                                       │                                 │
│                                       ▼                                 │
│   ┌──────────────────────────────────────────────────────────────────┐  │
│   │                         扫描结果展示                               │  │
│   │  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐ │  │
│   │  │ 📷 场景    │  │ 🎯 构图    │  │ 💡 光照    │  │ ⚙️ 参数    │ │  │
│   │  │ 城市夜景   │  │ 三分法+引导 │  │ 蓝调时刻   │  │ ISO400     │ │  │
│   │  │ 置信度95% │  │ 评分 88    │  │ 建议+0.3EV │  │ 1/60s      │ │  │
│   │  └────────────┘  └────────────┘  └────────────┘  └────────────┘ │  │
│   │                                                                  │  │
│   │  ┌────────────────────────────────────────────────────────────┐ │  │
│   │  │              AR 取景框叠加预览                               │ │  │
│   │  │     ┌─────────────────────────────────────────┐            │ │  │
│   │  │     │  ╲          三分网格           ╱        │            │ │  │
│   │  │     │   ╲                          ╱         │            │ │  │
│   │  │     │    ──────────────────────────          │            │ │  │
│   │  │     │   ╱  [人物位置]            ╲          │            │ │  │
│   │  │     │  ╱                           ╲         │            │ │  │
│   │  │     └─────────────────────────────────────────┘            │ │  │
│   │  └────────────────────────────────────────────────────────────┘ │  │
│   │                                                                  │  │
│   │  [ 一键应用 ]  [ 切换构图 ]  [ 调整参数 ]  [ 了解更多 ]          │  │
│   └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 AI构图分析器

```kotlin
/**
 * AI构图分析引擎
 * 2026：深度学习构图评分 + AR引导生成
 */
class AICompositionAnalyzer {

    /**
     * 完整构图分析
     */
    suspend fun analyzeComposition(
        frame: Bitmap,
        sceneContext: SceneContext
    ): CompositionResult = withContext(Dispatchers.Default) {
        // 1. 主体检测
        val subjects = detectMainSubjects(frame)

        // 2. 构图规则匹配
        val matchedRules = matchCompositionRules(frame, subjects)

        // 3. 裁剪建议计算
        val cropRegions = calculateCropSuggestions(frame, matchedRules)

        // 4. AR引导线生成
        val guideOverlays = generateARGuides(matchedRules, subjects)

        // 5. 整体评分
        val score = calculateOverallScore(frame, matchedRules, subjects)

        CompositionResult(
            primaryRule = matchedRules.first(),
            confidence = matchedRules.first().confidence,
            suggestedCrop = cropRegions.first(),
            guideOverlays = guideOverlays,
            tips = generateTips(matchedRules.first(), sceneContext),
            score = score,
            alternatives = matchedRules.drop(1)
        )
    }

    /**
     * 主体检测（使用 ML Kit 或自定义模型）
     */
    private fun detectMainSubjects(frame: Bitmap): List<Subject> {
        // 人脸优先
        val faces = faceDetector.detect(frame)
        val faceSubjects = faces.map { face ->
            Subject(
                type = SubjectType.FACE,
                bounds = face.boundingBox,
                importance = 1.0f,
                landmarks = face.landmarks
            )
        }

        // 其他主体（使用分割模型）
        val segments = segmentationModel.predict(frame)
        val mainSegment = segments.maxByOrNull { it.confidence }

        return if (faceSubjects.isNotEmpty()) {
            faceSubjects
        } else if (mainSegment != null) {
            listOf(Subject(type = SubjectType.OBJECT, bounds = mainSegment.bounds))
        } else {
            // 无主体，默认风景构图
            listOf(Subject(type = SubjectType.SCENE, bounds = fullFrame))
        }
    }

    /**
     * 构图规则匹配
     */
    private fun matchCompositionRules(
        frame: Bitmap,
        subjects: List<Subject>
    ): List<MatchedRule> {
        val rules = mutableListOf<MatchedRule>()

        // 三分法评分
        rules.add(evaluateRuleOfThirds(frame, subjects))

        // 黄金分割评分
        rules.add(evaluateGoldenRatio(frame, subjects))

        // 对角线评分
        if (hasDiagonalLines(frame)) {
            rules.add(evaluateDiagonal(frame, subjects))
        }

        // 对称评分
        if (isSymmetrical(frame)) {
            rules.add(MatchedRule(CompositionRule.SYMMETRY, 0.9f))
        }

        return rules.sortedByDescending { it.confidence }
    }

    /**
     * 生成 AR 引导线
     */
    private fun generateARGuides(
        matchedRules: List<MatchedRule>,
        subjects: List<Subject>
    ): List<GuideOverlay> {
        val guides = mutableListOf<GuideOverlay>()

        // 添加三分网格
        guides.add(createThirdsGrid())

        // 添加主体位置引导
        subjects.forEach { subject ->
            guides.add(createSubjectGuide(subject))
        }

        // 添加当前最佳规则的引导
        when (matchedRules.first().rule) {
            CompositionRule.GOLDEN_RATIO -> {
                guides.add(createGoldenSpiral())
            }
            CompositionRule.DIAGONAL -> {
                guides.add(createDiagonalGuides())
            }
            CompositionRule.LEADING_LINES -> {
                guides.addAll(detectAndDrawLeadingLines())
            }
            else -> {}
        }

        return guides
    }

    /**
     * 计算构图评分
     */
    private fun calculateOverallScore(
        frame: Bitmap,
        matchedRules: List<MatchedRule>,
        subjects: List<Subject>
    ): Float {
        // 主体位置得分
        val subjectScore = subjects.firstOrNull()?.let { subject ->
            matchedRules.first().let { rule ->
                evaluateSubjectPlacement(subject, rule.rule)
            }
        } ?: 50f

        // 画面平衡得分
        val balanceScore = evaluateBalance(frame, subjects)

        // 引导线得分
        val leadingScore = evaluateLeadingLines(frame, subjects)

        return (subjectScore * 0.5f + balanceScore * 0.3f + leadingScore * 0.2f)
            .coerceIn(0f, 100f)
    }
}

/**
 * AR 引导线渲染组件
 */
@Composable
fun ARGuideOverlay(
    guides: List<GuideOverlay>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        guides.forEach { guide ->
            when (guide.type) {
                GuideType.GRID -> drawThirdsGrid(guide.style)
                GuideType.GOLDEN_GRID -> drawGoldenGrid(guide.style)
                GuideType.DIAGONAL -> drawDiagonal(guide.style)
                GuideType.FACE_DETECT -> drawFaceBox(guide, guide.style)
                GuideType.SUBJECT_BOX -> drawSubjectBox(guide, guide.style)
                GuideType.LEADING_LINE -> drawLeadingLine(guide, guide.style)
                GuideType.TILT_HINT -> drawTiltIndicator(guide, guide.style)
            }
        }
    }
}
```

---

## 六、2026 交互体验标准

### 6.1 手势交互

```kotlin
/**
 * 2026 手势交互标准
 * OMaster专属：大触控区域 + 智能防误触
 */
object GestureStandards2026 {

    // 触控热区配置
    data class TouchHotspot(
        val region: RectRegion,
        val action: GestureAction,
        val threshold: Float = 0.8f  // 触发阈值
    )

    enum class GestureAction {
        SWITCH_MODE,      // 切换模式
        ADJUST_PARAMS,    // 调节参数
        TRIGGER_CAPTURE,  // 触发拍摄
        TOGGLE_AI,       // 开关AI
        ZOOM_CAMERA      // 相机缩放
    }

    // 拍摄界面手势分布
    val captureGestures = listOf(
        // 上滑：切换模式轮盘
        TouchHotspot(
            region = RectRegion(0.1f, 0f, 0.9f, 0.3f),
            action = GestureAction.SWITCH_MODE
        ),
        // 右滑：参数调节
        TouchHotspot(
            region = RectRegion(0.7f, 0.3f, 1f, 0.7f),
            action = GestureAction.ADJUST_PARAMS
        ),
        // 中心点击：AI场景扫描
        TouchHotspot(
            region = RectRegion(0.3f, 0.3f, 0.7f, 0.7f),
            action = GestureAction.TOGGLE_AI
        ),
        // 底部中央：快门
        TouchHotspot(
            region = RectRegion(0.35f, 0.8f, 0.65f, 1f),
            action = GestureAction.TRIGGER_CAPTURE
        )
    )

    /**
     * 智能防误触
     * 边缘抑制 + 速度检测 + 区域优先级
     */
    fun isIntentionalTouch(
        touch: TouchEvent,
        history: List<TouchEvent>
    ): Boolean {
        // 1. 边缘抑制（10%边缘区域降低灵敏度）
        if (isInEdgeZone(touch.position, threshold = 0.1f)) {
            if (touch.velocity < VELOCITY_THRESHOLD * 1.5f) {
                return false
            }
        }

        // 2. 速度检测（太慢可能是误触）
        if (touch.velocity < VELOCITY_THRESHOLD) {
            return false
        }

        // 3. 历史轨迹分析（连续误触模式检测）
        if (isAccidentalPattern(history)) {
            return false
        }

        return true
    }
}

/**
 * 模式切换轮盘
 * 2026：3D旋转菜单 + Haptic反馈
 */
@Composable
fun ModeSwitchWheel(
    modes: List<ShootingMode>,
    selectedMode: ShootingMode,
    onModeSelected: (ShootingMode) -> Unit
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableIntStateOf(modes.indexOf(selectedMode)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    // 更新旋转角度
                    this.rotation += rotation
                    // 计算当前选中项
                    val anglePerItem = 360f / modes.size
                    val normalizedRotation = (this.rotation % 360f + 360f) % 360f
                    selectedIndex = ((normalizedRotation / anglePerItem).toInt()) % modes.size

                    // 触发Haptic
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    ) {
        // 3D 旋转效果
        modes.forEachIndexed { index, mode ->
            val angle = (index * (360f / modes.size) + rotation) % 360f
            val isSelected = index == selectedIndex

            ModeWheelItem(
                mode = mode,
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        rotationZ = angle
                        scaleX = if (isSelected) 1.2f else 0.8f
                        scaleY = if (isSelected) 1.2f else 0.8f
                        alpha = if (isSelected) 1f else 0.5f
                    }
            )
        }
    }
}
```

### 6.2 上下文感知 UI

```kotlin
/**
 * 上下文感知 UI 状态机
 * 2026：根据场景和用户行为智能调整界面
 */
class ContextualUIController {

    enum class UIContext {
        DEFAULT,          // 默认
        PORTRAIT,         // 人像模式
        NIGHT,            // 夜景模式
        PROFESSIONAL,     // 专业模式
        LOW_LIGHT,        // 暗光环境
        HIGH_MOTION,      // 运动场景
        GROUP             // 合影场景
    }

    data class UIAdaptation(
        val showPoseGuide: Boolean,      // 显示姿势引导
        val showHistogram: Boolean,      // 显示直方图
        val showZebraPattern: Boolean,   // 显示斑马纹
        val showGrid: Boolean,           // 显示网格
        val autoEnableTimer: Boolean,    // 自动启用倒计时
        val suggestedMode: ShootingMode, // 建议模式
        val hints: List<String>          // 提示信息
    )

    /**
     * 根据上下文计算 UI 适配
     */
    fun calculateAdaptation(
        sceneContext: SceneContext,
        userBehavior: UserBehaviorHistory
    ): UIAdaptation {
        return when {
            // 人像模式上下文
            sceneContext.sceneType == SceneType.PORTRAIT &&
            sceneContext.faceCount > 0 -> UIAdaptation(
                showPoseGuide = true,
                showHistogram = false,
                showZebraPattern = false,
                showGrid = true,
                autoEnableTimer = false,
                suggestedMode = ShootingMode.PORTRAIT,
                hints = generatePortraitHints(sceneContext)
            )

            // 夜景上下文
            sceneContext.brightness < 0.2f -> UIAdaptation(
                showPoseGuide = false,
                showHistogram = true,
                showZebraPattern = sceneContext.sceneType == SceneType.NIGHT,
                showGrid = true,
                autoEnableTimer = userBehavior.isSteadyHands,
                suggestedMode = ShootingMode.NIGHT,
                hints = generateNightHints(sceneContext)
            )

            // 专业模式上下文
            userBehavior.prefersProMode -> UIAdaptation(
                showPoseGuide = false,
                showHistogram = true,
                showZebraPattern = true,
                showGrid = true,
                autoEnableTimer = false,
                suggestedMode = ShootingMode.PRO,
                hints = emptyList()
            )

            else -> defaultAdaptation
        }
    }

    /**
     * 生成人像提示
     */
    private fun generatePortraitHints(context: SceneContext): List<String> {
        val hints = mutableListOf<String>()

        if (context.faceCount == 1) {
            hints.add("💡 尝试侧光或逆光拍摄，更有氛围感")
            hints.add("👤 保持人物在画面三分线交点位置")
        } else if (context.faceCount >= 2) {
            hints.add("👥 合影建议使用连拍模式")
            hints.add("📐 保持所有人都在同一焦平面")
        }

        if (context.backlit) {
            hints.add("🌅 检测到逆光，建议开启补光")
        }

        return hints
    }
}
```

---

## 七、实施路线图

### 7.1 Phase 1：核心架构（V2.0）
- [ ] SceneContextManager 场景感知引擎
- [ ] AICompositionEngine 构图分析
- [ ] AR Guide Overlay 渲染
- [ ] 新一代 CameraXManager（支持所有模式）

### 7.2 Phase 2：拍摄模式（V2.1）
- [ ] 人像模式 + 姿势引导
- [ ] 夜景模式 + 多帧合成
- [ ] 光绘模式 + 轨迹检测
- [ ] 专业模式 + 直方图/斑马纹

### 7.3 Phase 3：美化引擎（V2.2）
- [ ] RealTimeBeautyEngine 3链路并行
- [ ] 分层美颜（皮肤/眼睛/轮廓）
- [ ] 眼神光 + 氛围光效
- [ ] LUT 实时预览

### 7.4 Phase 4：交互体验（V2.3）
- [ ] 模式切换轮盘 3D UI
- [ ] 手势交互增强
- [ ] 上下文感知 UI
- [ ] Haptic 反馈系统

---

## 八、技术指标

| 指标 | 2026 目标 | 当前水平 |
|------|---------|---------|
| 场景识别准确率 | >92% | ~85% |
| 构图评分准确率 | >88% | N/A |
| 实时预览帧率 | 30fps | 10fps |
| 美颜处理延迟 | <16ms | ~50ms |
| 夜景合成张数 | 4-16张自适应 | 固定8张 |
| 模式切换响应 | <100ms | ~300ms |

---

*文档版本：V2.0.0 | 更新日期：2026-06-26 | 作者：OMaster 产品团队*
