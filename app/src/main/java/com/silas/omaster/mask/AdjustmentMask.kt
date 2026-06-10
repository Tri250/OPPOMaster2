package com.silas.omaster.mask

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 蒙版类型
 * 2026 标准：与 Lightroom / Snapseed / Darkroom 能力对齐
 */
enum class MaskType(val displayName: String) {
    /** 线性渐变蒙版 - 用户拖拽定义方向，渐变区域应用独立参数 */
    LINEAR_GRADIENT("线性渐变"),
    /** 径向渐变蒙版 - 中心向外衰减，常见于暗角/光晕效果 */
    RADIAL("径向渐变"),
    /** 画笔蒙版 - 手指/触控笔涂抹区域 */
    BRUSH("画笔"),
    /** AI 智能蒙版 - ML Kit 人脸 / TFLite 语义分割 */
    AI("AI 智能"),
    /** 亮度蒙版 - 基于图像亮度自动选区 (2026 趋势) */
    LUMINANCE("亮度范围"),
    /** 颜色蒙版 - 基于色彩相似度选区 */
    COLOR("颜色范围");

    val isAuto: Boolean get() = this == AI || this == LUMINANCE || this == COLOR
}

/**
 * 蒙版混合模式
 */
enum class BlendMode(val displayName: String) {
    /** 叠加 - 在蒙版区域内应用参数，外部区域不应用 */
    OVERLAY("叠加"),
    /** 替换 - 完全替换原图参数 */
    REPLACE("替换"),
    /** 相乘 - 参数效果按蒙版强度相乘 */
    MULTIPLY("相乘"),
    /** 屏幕 - 参数效果与原图屏幕混合 */
    SCREEN("屏幕");

    /** 转换为 GLSL 混合因子 (0=REPLACE, 1=ADD, 2=MULTIPLY, 3=SCREEN) */
    fun toGlslFactor(): Int = when (this) {
        OVERLAY -> 0
        REPLACE -> 1
        MULTIPLY -> 2
        SCREEN -> 3
    }
}

/**
 * 渐变蒙版参数 (LINEAR_GRADIENT / RADIAL)
 * 坐标使用归一化 [0, 1] 系统
 *
 * @property startX 起点 X 坐标（归一化）
 * @property startY 起点 Y 坐标（归一化）
 * @property endX 终点 X 坐标（归一化，LINEAR） / 中心 X（RADIAL）
 * @property endY 终点 Y 坐标（归一化，LINEAR） / 中心 Y（RADIAL）
 * @property feathering 羽化程度 [0, 1]，0=硬边，1=最柔
 * @property rotation 旋转角度（度数，LINEAR）
 * @property aspectRatio 宽高比 (用于径向的椭圆形状，1.0=正圆)
 * @property invert 是否反转
 */
@Parcelize
@Serializable
data class GradientMaskParams(
    val startX: Float = 0.5f,
    val startY: Float = 0.0f,
    val endX: Float = 0.5f,
    val endY: Float = 1.0f,
    val feathering: Float = 0.5f,
    val rotation: Float = 0f,
    val aspectRatio: Float = 1.0f,
    val invert: Boolean = false
) : Parcelable {
    companion object {
        val DEFAULT_LINEAR = GradientMaskParams(
            startX = 0.5f, startY = 0.0f,
            endX = 0.5f, endY = 1.0f,
            feathering = 0.5f
        )
        val DEFAULT_RADIAL = GradientMaskParams(
            startX = 0.5f, startY = 0.5f,
            endX = 0.5f, endY = 0.5f,
            feathering = 0.7f
        )
    }
}

/**
 * 画笔蒙版参数 (BRUSH)
 *
 * @property size 画笔半径（归一化到图像短边，最小 0.005，最大 0.5）
 * @property hardness 硬度 [0, 1]，0=最软，1=最硬
 * @property flow 流量 [0, 1]，每笔涂抹的强度
 * @property spacing 笔触间距 [0, 1]，相对笔刷大小
 * @property strokes 笔触序列 (相对坐标 0..1)
 */
@Parcelize
@Serializable
data class BrushMaskParams(
    val size: Float = 0.08f,
    val hardness: Float = 0.85f,
    val flow: Float = 1.0f,
    val spacing: Float = 0.25f,
    val strokes: List<BrushStroke> = emptyList()
) : Parcelable {
    /**
     * 笔触
     * @property points 笔触经过的点 (相对坐标 0..1)
     * @property strength 该笔触的强度 [0, 1]
     */
    @Parcelize
    @Serializable
    data class BrushStroke(
        val points: List<Pair<Float, Float>>,
        val strength: Float = 1.0f
    ) : Parcelable
}

/**
 * AI 蒙版参数
 *
 * @property subjectType 识别目标
 * @property confidenceThreshold 置信度阈值 [0, 1]
 * @property expand 扩展像素 (相对短边，0..0.1)
 * @property feather 羽化
 */
@Parcelize
@Serializable
data class AIMaskParams(
    val subjectType: AISubject = AISubject.SKY,
    val confidenceThreshold: Float = 0.5f,
    val expand: Float = 0.0f,
    val feather: Float = 0.1f
) : Parcelable

/**
 * AI 识别目标
 */
enum class AISubject(val displayName: String) {
    SKY("天空"),
    PERSON("人物"),
    FACE("人脸"),
    HAIR("头发"),
    SKIN("肤色"),
    FOREGROUND("前景"),
    BACKGROUND("背景"),
    BUILDING("建筑"),
    PLANT("植物"),
    WATER("水体"),
    ANIMAL("动物");
}

/**
 * 蒙版数据类
 * 是 P0 局部调整的核心数据结构
 *
 * @property id 唯一 ID
 * @property name 显示名（用户可编辑）
 * @property type 蒙版类型
 * @property enabled 是否启用
 * @property opacity 整体不透明度 [0, 1]
 * @property blendMode 混合模式
 * @property gradientParams 渐变参数（LINEAR/RADIAL）
 * @property brushParams 画笔参数（BRUSH）
 * @property aiParams AI 参数（AI）
 * @property luminanceRange 亮度范围（LUMINANCE）
 * @property colorTarget 颜色目标（COLOR）
 * @property localParams 局部应用的参数（复用现有 RenderParameters）
 * @property cachedBitmap 缓存的灰度蒙版 (运行时使用)
 * @property createdAt 创建时间戳
 * @property updatedAt 更新时间戳
 */
@Parcelize
@Serializable
data class AdjustmentMask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "蒙版",
    val type: MaskType = MaskType.LINEAR_GRADIENT,
    val enabled: Boolean = true,
    val opacity: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.OVERLAY,
    val gradientParams: GradientMaskParams = GradientMaskParams.DEFAULT_LINEAR,
    val brushParams: BrushMaskParams = BrushMaskParams(),
    val aiParams: AIMaskParams = AIMaskParams(),
    val luminanceRange: LuminanceRange = LuminanceRange(),
    val colorTarget: ColorTarget = ColorTarget(),
    val localParams: com.silas.omaster.renderer.RenderParameters = com.silas.omaster.renderer.RenderParameters(),
    @Transient
    val cachedBitmap: Bitmap? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /** 检查蒙版是否对图像产生任何影响 */
    fun isEffective(): Boolean {
        if (!enabled) return false
        if (opacity <= 0f) return false
        return localParams.hasAnyAdjustment()
    }

    companion object {
        /** 默认预设：天空增强 */
        val SKY_ENHANCEMENT = AdjustmentMask(
            name = "天空增强",
            type = MaskType.LINEAR_GRADIENT,
            gradientParams = GradientMaskParams(
                startX = 0.5f, startY = 0.0f,
                endX = 0.5f, endY = 0.5f,
                feathering = 0.6f
            ),
            localParams = com.silas.omaster.renderer.RenderParameters(
                saturation = 20f,
                vibrance = 15f,
                contrast = 8f,
                clarity = 10f,
                dehaze = 10f
            )
        )

        /** 默认预设：前景压暗 */
        val FOREGROUND_DARKEN = AdjustmentMask(
            name = "前景压暗",
            type = MaskType.LINEAR_GRADIENT,
            gradientParams = GradientMaskParams(
                startX = 0.5f, startY = 0.7f,
                endX = 0.5f, endY = 1.0f,
                feathering = 0.5f
            ),
            localParams = com.silas.omaster.renderer.RenderParameters(
                exposure = -25f,
                shadows = -10f,
                blacks = -15f
            )
        )

        /** 默认预设：人像提亮 */
        val PORTRAIT_BRIGHTEN = AdjustmentMask(
            name = "人像提亮",
            type = MaskType.AI,
            aiParams = AIMaskParams(subjectType = AISubject.PERSON),
            localParams = com.silas.omaster.renderer.RenderParameters(
                exposure = 15f,
                shadows = 20f,
                skinSmooth = 30f,
                clarity = -5f
            )
        )

        /** 默认预设：径向暗角 */
        val RADIAL_VIGNETTE = AdjustmentMask(
            name = "径向暗角",
            type = MaskType.RADIAL,
            gradientParams = GradientMaskParams.DEFAULT_RADIAL.copy(feathering = 0.7f),
            localParams = com.silas.omaster.renderer.RenderParameters(
                exposure = -30f,
                blacks = -20f
            )
        )

        /** 所有默认预设 */
        val DEFAULT_PRESETS = listOf(SKY_ENHANCEMENT, FOREGROUND_DARKEN, PORTRAIT_BRIGHTEN, RADIAL_VIGNETTE)
    }
}

/**
 * 亮度范围（用于 LUMINANCE 类型蒙版）
 * 选区为亮度值在 [lowerBound, upperBound] 范围内的像素
 *
 * @property lowerBound 亮度下限 [0, 1]
 * @property upperBound 亮度上限 [0, 1]
 * @property smoothness 平滑度（边缘羽化）
 */
@Parcelize
@Serializable
data class LuminanceRange(
    val lowerBound: Float = 0.0f,
    val upperBound: Float = 1.0f,
    val smoothness: Float = 0.1f
) : Parcelable

/**
 * 颜色目标（用于 COLOR 类型蒙版）
 *
 * @property targetColor 目标颜色 (0xAARRGGBB)
 * @property tolerance 容差 [0, 1]
 * @property invertSelection 是否反转
 */
@Parcelize
@Serializable
data class ColorTarget(
    val targetColor: Int = 0xFFFF6B35.toInt(),
    val tolerance: Float = 0.2f,
    val invertSelection: Boolean = false
) : Parcelable

/**
 * 蒙版编辑事件 - 用于 UI 交互回调
 */
sealed class MaskEditEvent {
    object Move : MaskEditEvent()
    object Resize : MaskEditEvent()
    object Rotate : MaskEditEvent()
    object Invert : MaskEditEvent()
    data class BrushPaint(val x: Float, val y: Float) : MaskEditEvent()
    data class StrokeEnd(val points: List<Pair<Float, Float>>) : MaskEditEvent()
    object Undo : MaskEditEvent()
    object Redo : MaskEditEvent()
    object Clear : MaskEditEvent()
}
