package com.silas.omaster.ui.features

import android.graphics.Bitmap
import com.silas.omaster.model.HistogramData
import com.silas.omaster.model.RectData

/**
 * 哈苏之眼拍摄模式枚举
 *
 * 每种模式对应一条完整的用户交互体验链路：
 * - AI_AUTO：一键扫描 + AI 自动构图 + 自动参数
 * - PORTRAIT：人像模式（姿势引导 + 实时虚化 + 5 层美颜）
 * - NIGHT：夜景模式（多帧合成降噪 + 长曝光）
 * - LIGHT_PAINTING：光绘/长曝光模式（手动曝光 + 光轨累积预览）
 * - PRO：专业模式（全手动参数 + 直方图 + 斑马纹）
 * - FOOD / STREET / PET：特化场景模式（0 延迟快门 + 专属参数）
 */
enum class CaptureMode(val displayName: String, val description: String, val icon: String) {
    AI_AUTO("AI 自动", "一键扫描，智能出片", "✨"),
    PORTRAIT("人像", "姿势引导 · 虚化 · 美颜", "👤"),
    NIGHT("夜景", "多帧降噪 · 长曝光", "🌃"),
    LIGHT_PAINTING("光绘", "手动曝光 · 光轨累积", "✨"),
    PRO("专业", "全手动 · 直方图 · 斑马纹", "⚙️"),
    FOOD("美食", "食欲色彩 · 0 延迟快门", "🍜"),
    STREET("街拍", "抓拍瞬间 · 电影质感", "🏢"),
    PET("宠物", "追焦捕捉 · 0 延迟快门", "🐾")
}

/**
 * AR 构图结果
 *
 * @param guideLines 引导线列表（用于 Canvas 绘制）
 * @param subjectBounds 主体包围框（归一化坐标 0-1）
 * @param compositionScore 构图评分 0-100
 * @param levelIndicator 水平仪指示（弧度）
 * @param tips 给用户的实时构图提示
 */
data class ARCompositionResult(
    val guideLines: List<ARGuideLine>,
    val subjectBounds: RectData?,
    val compositionScore: Int,
    val levelIndicator: Float,
    val tips: String
)

/**
 * AR 引导线
 */
data class ARGuideLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Int,
    val strokeWidth: Float = 3f
)

/**
 * 专业模式参数
 */
data class ProModeParams(
    val iso: Int? = null,                // null = 自动 ISO
    val shutterSpeedNs: Long? = null,    // null = 自动快门；单位：纳秒
    val focusDistance: Float? = null,    // null = 自动对焦；0-1 对焦距离
    val whiteBalanceTemperature: Int? = null, // null = 自动白平衡；单位：K
    val exposureCompensation: Int = 0
)

/**
 * 人像模式实时处理结果
 */
data class PortraitModeResult(
    val processedFrame: Bitmap?,
    val faceCount: Int,
    val poseGuide: PoseGuide?,
    val beautyApplied: Boolean
)

/**
 * 姿势引导建议
 */
data class PoseGuide(
    val title: String,
    val description: String,
    val previewImageName: String? = null
)

/**
 * 夜景模式状态
 */
data class NightModeState(
    val isCapturing: Boolean,
    val capturedFrames: Int,
    val totalFrames: Int,
    val estimatedRemainingMs: Long
)

/**
 * 光绘模式状态
 */
data class LightPaintingState(
    val isRecording: Boolean,
    val elapsedMs: Long,
    val accumulatedFrame: Bitmap? = null
)

/**
 * 特化模式配置（美食/街拍/宠物）
 */
data class SpecializedModeConfig(
    val mode: CaptureMode,
    val shutterLagElimination: Boolean,  // 是否启用 0 延迟快门
    val burstEnabled: Boolean,           // 是否支持连拍
    val preferredColorModeId: String,    // 推荐色彩模式 ID
    val sceneParams: Map<String, Int>    // 专属场景参数
)
