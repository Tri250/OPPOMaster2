package com.silas.omaster.ai.shooting

import android.graphics.Bitmap
import android.graphics.RectF
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile

/**
 * 智能拍摄系统 - L4 AI 决策层接口契约
 *
 * 设计原则：
 * 1. 所有数据类为不可变值类型，便于在 Flow 中安全传递
 * 2. 接口面向能力，不绑定具体算法实现，便于替换为 TFLite 模型
 * 3. 单帧决策延迟预算：≤55ms（30fps 预览，每帧 33ms 预留 22ms 冗余）
 */

// ==================== L2 HAL 抽象层输入 ====================

/**
 * 一帧预览的完整元数据
 *
 * @param timestampNs       帧时间戳（System.nanoTime，用于运动估计）
 * @param previewBitmap     预览帧 Bitmap（已旋转至正向，所有权归调用方）
 * @param width             Bitmap 宽度
 * @param height            Bitmap 高度
 * @param sensorLux         当前传感器估计环境 lux（来自 Camera2Statistics 或 SENSOR_SENSITIVITY 反推）
 * @param colorTempK        当前估计色温 K（来自白平衡算法）
 * @param focalLength35mm  当前焦段 35mm 等效
 * @param zoomRatio        当前变焦倍率（1.0 = 广角原生）
 * @param gyroX/Y/Z        陀螺仪角速度 rad/s（用于运动模糊判定 + 长曝光对齐）
 * @param gyroTimestampNs  陀螺仪采样时间戳
 * @param isFrontCamera    是否前置摄像头
 */
data class FrameMeta(
    val timestampNs: Long,
    val previewBitmap: Bitmap,
    val width: Int,
    val height: Int,
    val sensorLux: Float,
    val colorTempK: Int,
    val focalLength35mm: Float,
    val zoomRatio: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val gyroTimestampNs: Long,
    val isFrontCamera: Boolean
)

// ==================== 场景理解输出 ====================

enum class ShootingMode {
    SMART,           // 智能拍摄（默认）
    PORTRAIT,        // 人像（带姿势引导 + 背景虚化）
    NIGHT,           // 夜景（多帧合成降噪）
    LONG_EXPOSURE,   // 光绘/长曝光
    PROFESSIONAL     // 专业（全手动参数控制）
}

enum class MotionLevel { STATIC, LOW, MEDIUM, HIGH }

/**
 * 光源估计（不依赖 24 通道丹霞镜头时的降级实现也兼容）
 */
data class LightEstimate(
    val lux: Float,
    val colorTempK: Int,
    /** 光源 2D 投影方向，画面坐标系，原点在中心，归一化 [-1, 1] */
    val lightDirectionX: Float,
    val lightDirectionY: Float,
    val isBacklit: Boolean,
    val dynamicRangeEv: Float,
    val motionLevel: MotionLevel
)

/**
 * 场景识别结果（120 类的粗分类，由 HeuristicSceneAnalyzer 的细分类聚合而成）
 */
data class SceneClassification(
    val sceneProfile: SceneProfile,
    val category: SceneCategory,
    val subSceneName: String,
    val confidence: Float,
    val light: LightEstimate,
    val faceCount: Int,
    val primarySubjectRect: RectF?,  // 归一化坐标 [0,1]
    val recommendedMode: ShootingMode
)

// ==================== 构图引擎输出 ====================

enum class CompositionRule {
    RULE_OF_THIRDS,   // 三分法
    GOLDEN_RATIO,     // 黄金分割
    DIAGONAL,         // 对角线
    SYMMETRY,         // 对称
    FRAME_WITHIN,     // 框架构图
    LEADING_LINE      // 引导线
}

/**
 * 单个构图方案
 */
data class CompositionOption(
    val rule: CompositionRule,
    /** 归一化裁切框 [0,1]，相对当前预览画面 */
    val cropRect: RectF,
    /** 建议变焦倍率（应用到 CameraX ZoomRatio） */
    val zoomFactor: Float,
    /** 美学评分 [0, 100]，由 CompositionEngine 评分网络（或启发式评分器）输出 */
    val score: Float,
    /** 构图理由（用于场景胶囊展示，如"主体落于黄金分割右上线"） */
    val reason: String,
    /** 配套的引导线，用于 AR 渲染 */
    val guideLines: List<GuideLine>
)

data class GuideLine(
    val startX: Float,  // 归一化 [0,1]
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val type: GuideLineType
)

enum class GuideLineType {
    THIRD_HORIZONTAL,   // 三分横线
    THIRD_VERTICAL,     // 三分竖线
    GOLDEN_SPIRAL,      // 黄金螺旋引导
    DIAGONAL_LINE,      // 对角线
    HORIZON_REFERENCE,   // 水平参考线
    SUBJECT_TRACKING     // 主体追踪框
)

data class CompositionProposal(
    val options: List<CompositionOption>,  // 按 score 降序，最多 3 个
    val goldenPoints: List<RectF>,           // 黄金点位标注（4 个，归一化半径 0.02）
    val autoApplyZoom: Float,               // Top1 方案的变焦倍率
    val autoApplyExposureComp: Float         // Top1 方案的曝光补偿（-5..5 EV）
)

// ==================== AR 姿势引导输出 ====================

data class PoseKeypoint(
    val x: Float,           // 归一化 [0,1]
    val y: Float,
    val visibility: Float,  // [0,1]
    val type: KeypointType
)

enum class KeypointType {
    NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR,
    LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST, LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE
}

data class PoseTemplate(
    val id: String,
    val name: String,                  // "回眸侧颜"
    val keypoints: List<PoseKeypoint>, // 标准姿势模板
    val isFullBody: Boolean
)

data class PoseGuide(
    val detected: List<PoseKeypoint>,
    val target: PoseTemplate?,
    /** 贴合度评分 [0, 100]，100 = 完全贴合 */
    val alignmentScore: Float,
    /** AR 渲染连线（成对的 keypoint index） */
    val skeletonEdges: List<Pair<Int, Int>>
)

// ==================== 美颜引擎输出 ====================

data class BeautyParams(
    val skinTextureStrength: Float,    // [0,1] 肌理层（保纹理降噪）
    val skinLightStrength: Float,      // [0,1] 光影层（智能补光）
    val facialFeatureStrength: Float,  // [0,1] 五官层（默认 0，避免假面感）
    val hairEnhanceStrength: Float,    // [0,1] 发丝层
    val skinToneWarmth: Float,         // [-1,1] 肤色暖冷倾向
    val faceLandmarks: List<RectF>     // 检测到的人脸区域（归一化）
) {
    companion object {
        /** 系统默认配置：肌理+光影+发丝开，五官关 */
        val DEFAULT = BeautyParams(
            skinTextureStrength = 0.65f,
            skinLightStrength = 0.55f,
            facialFeatureStrength = 0f,
            hairEnhanceStrength = 0.5f,
            skinToneWarmth = 0.1f,
            faceLandmarks = emptyList()
        )

        /** 全关（用于专业模式 / 风景模式） */
        val DISABLED = BeautyParams(
            skinTextureStrength = 0f,
            skinLightStrength = 0f,
            facialFeatureStrength = 0f,
            hairEnhanceStrength = 0f,
            skinToneWarmth = 0f,
            faceLandmarks = emptyList()
        )
    }
}

// ==================== 参数建议 ====================

data class ParameterAdvice(
    val iso: Int?,                // 推荐 ISO（专业模式）
    val shutterSeconds: Float?,   // 推荐快门秒数
    val evCompensation: Float,    // 曝光补偿
    val whiteBalanceK: Int?,      // 白平衡 K
    val focusDistance: Float?,    // 对焦距离 m（null = 自动）
    val multiFrameCount: Int,     // 多帧合成帧数（夜景/长曝光）
    val applyHasselbladParams: Boolean  // 是否应用哈苏影调
)

// ==================== 决策结果聚合 ====================

data class DecisionResult(
    val scene: SceneClassification,
    val composition: CompositionProposal,
    val poseGuide: PoseGuide?,
    val beautyParams: BeautyParams,
    val paramAdvice: ParameterAdvice,
    val decisionLatencyMs: Long    // 单帧决策总耗时（用于性能监控）
)

// ==================== 引擎接口契约 ====================

interface SceneUnderstandingEngine {
    /**
     * 单帧场景理解。
     * 约束：纯函数（不修改 [frame]），线程安全，端侧执行。
     * 性能预算：≤18ms。
     */
    suspend fun understand(frame: FrameMeta): SceneClassification
}

interface CompositionEngine {
    /**
     * 单帧构图提案。
     * 性能预算：≤12ms。
     */
    suspend fun propose(frame: FrameMeta, scene: SceneClassification): CompositionProposal
}

interface PoseEstimator {
    /**
     * 单帧姿态估计 + 贴合度评分（若 [targetTemplate] 为 null，仅输出检测结果）。
     * 性能预算：≤15ms。
     */
    suspend fun estimate(frame: FrameMeta, targetTemplate: PoseTemplate?): PoseGuide?
}

interface BeautyEngine {
    /**
     * 单帧美颜参数决策（基于人脸检测结果 + 场景光照）。
     * 性能预算：≤10ms。
     */
    suspend fun decideParams(frame: FrameMeta, scene: SceneClassification): BeautyParams

    /**
     * 实际像素级美颜处理（应用于最终输出 Bitmap）。
     * 该步骤仅在按下快门时执行，不在预览路径。
     */
    suspend fun process(source: Bitmap, params: BeautyParams): Bitmap
}

interface BestShotSelector {
    /**
     * 从连拍帧中选择最优帧。
     * 评分维度：清晰度 / 构图 / 表情（人像）/ 闭合眼判别。
     */
    suspend fun select(frames: List<Bitmap>, scene: SceneClassification): Int
}
