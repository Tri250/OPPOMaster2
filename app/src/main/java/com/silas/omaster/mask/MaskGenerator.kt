package com.silas.omaster.mask

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 蒙版生成器
 * - 线性渐变蒙版生成
 * - 径向渐变蒙版生成
 * - 画笔蒙版合成
 * - AI 蒙版骨架（由 AIMaskProvider 提供识别结果后生成）
 *
 * 输出：单通道灰度 Bitmap (ALPHA_8)，白色=完全选中，黑色=未选中
 */
object MaskGenerator {

    /**
     * 生成蒙版 Bitmap
     * @param width 输出宽度
     * @param height 输出高度
     * @param mask 蒙版配置
     * @return 灰度蒙版（ALPHA_8）
     */
    fun generate(width: Int, height: Int, mask: AdjustmentMask): Bitmap {
        return when (mask.type) {
            MaskType.LINEAR_GRADIENT -> generateLinearGradient(width, height, mask.gradientParams)
            MaskType.RADIAL -> generateRadialGradient(width, height, mask.gradientParams)
            MaskType.BRUSH -> generateBrushMask(width, height, mask.brushParams)
            MaskType.AI -> generateAIMaskPlaceholder(width, height, mask.aiParams)
            MaskType.LUMINANCE -> generateLuminancePlaceholder(width, height, mask.luminanceRange)
            MaskType.COLOR -> generateColorPlaceholder(width, height, mask.colorTarget)
        }.also {
            // 应用整体不透明度与反转
            applyOpacityAndInvert(it, mask.opacity, shouldInvert(mask))
        }
    }

    /**
     * 线性渐变蒙版
     * 在 [start → end] 方向上从白到黑渐变
     * feathering 控制羽化程度
     */
    fun generateLinearGradient(
        width: Int,
        height: Int,
        params: GradientMaskParams
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val pixels = IntArray(width * height)

        val startX = params.startX * width
        val startY = params.startY * height
        val endX = params.endX * width
        val endY = params.endY * height

        // 渐变方向向量
        val dx = endX - startX
        val dy = endY - startY
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val unitDx = dx / length
        val unitDy = dy / length

        // 法线方向（垂直）
        val normalX = -unitDy
        val normalY = unitDx

        // 羽化宽度（像素）
        val featherPx = (params.feathering * length).coerceAtLeast(1f)
        // 硬边宽度（中心亮带）
        val hardWidth = (length * (1f - params.feathering) * 0.5f).coerceAtLeast(0f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // 像素到起点的向量
                val vx = x - startX
                val vy = y - startY
                // 在渐变方向上的投影
                val proj = vx * unitDx + vy * unitDy
                // 在法线方向上的距离
                val dist = abs(vx * normalX + vy * normalY)

                // 渐变值：起点处=1, 终点处=0
                val gradientValue = (1f - proj / length).coerceIn(0f, 1f)

                // 羽化：考虑法线方向的距离
                val featherFactor = 1f - (dist / featherPx).coerceIn(0f, 1f)
                val featheredValue = gradientValue * featherFactor

                // 硬边
                val hardValue = if (dist < hardWidth) gradientValue else featheredValue

                val alpha = (hardValue * 255).toInt().coerceIn(0, 255)
                pixels[y * width + x] = alpha
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 径向渐变蒙版
     * 中心白，向外渐变到黑
     * 椭圆形状由 aspectRatio 控制
     */
    fun generateRadialGradient(
        width: Int,
        height: Int,
        params: GradientMaskParams
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val pixels = IntArray(width * height)

        val cx = params.endX * width
        val cy = params.endY * height
        val aspect = params.aspectRatio.coerceAtLeast(0.1f)

        // 取对角线作为最大半径参考
        val maxR = sqrt((width * width + height * height).toFloat())
        val featherR = (params.feathering * maxR * 0.5f).coerceAtLeast(1f)
        val hardR = (maxR * 0.5f * (1f - params.feathering) * 0.5f).coerceAtLeast(0f)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx)
                val dy = (y - cy) * aspect
                val r = sqrt(dx * dx + dy * dy)

                // 径向衰减：高斯型
                val gaussianValue = exp(-(r * r) / (2f * featherR * featherR))

                // 中心硬边
                val hardValue = if (r < hardR) 1f else gaussianValue

                val alpha = (hardValue * 255).toInt().coerceIn(0, 255)
                pixels[y * width + x] = alpha
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 画笔蒙版
     * 沿笔触路径合成画笔笔触
     *
     * 流程：
     * 1. 创建空白蒙版
     * 2. 对每条笔触，按间距在两点之间插值生成笔触点
     * 3. 在每个笔触点绘制圆形笔刷（带硬度/流量/羽化）
     */
    fun generateBrushMask(
        width: Int,
        height: Int,
        params: BrushMaskParams
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val pixels = IntArray(width * height)

        val brushRadius = (params.size * min(width, height) * 0.5f).coerceAtLeast(1f)
        val hardness = params.hardness.coerceIn(0f, 1f)
        val flow = params.flow.coerceIn(0f, 1f)
        val spacing = params.spacing.coerceAtLeast(0.05f) * brushRadius

        for (stroke in params.strokes) {
            // 沿笔触路径插值
            val interpolatedPoints = interpolateStroke(stroke.points, spacing, width, height)
            for ((nx, ny) in interpolatedPoints) {
                val px = (nx * width).toInt()
                val py = (ny * height).toInt()
                applyBrushStamp(pixels, width, height, px, py, brushRadius, hardness, flow * stroke.strength)
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 笔触点之间插值，按间距生成连续点
     */
    private fun interpolateStroke(
        points: List<Pair<Float, Float>>,
        spacing: Float,
        width: Int,
        height: Int
    ): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return points

        val result = mutableListOf<Pair<Float, Float>>()
        result.add(points.first())

        for (i in 1 until points.size) {
            val (x1, y1) = points[i - 1]
            val (x2, y2) = points[i]
            val dx = x2 - x1
            val dy = y2 - y1
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.001f) continue
            val steps = max(1, (dist / (spacing / min(width, height).toFloat())).toInt())

            for (s in 1..steps) {
                val t = s.toFloat() / steps
                result.add(x1 + dx * t to y1 + dy * t)
            }
        }
        return result
    }

    /**
     * 在蒙版上应用一个笔刷点
     * 中心硬边（由 hardness 控制）+ 软边羽化
     * 按 alpha 累加（additive）
     */
    private fun applyBrushStamp(
        pixels: IntArray,
        width: Int,
        height: Int,
        cx: Int,
        cy: Int,
        radius: Float,
        hardness: Float,
        strength: Float
    ) {
        val r = radius.toInt().coerceAtLeast(1)
        val innerR = radius * hardness
        val maxAlpha = (255f * strength).toInt().coerceIn(0, 255)

        for (dy in -r..r) {
            val y = cy + dy
            if (y < 0 || y >= height) continue
            for (dx in -r..r) {
                val x = cx + dx
                if (x < 0 || x >= width) continue

                val dist = sqrt((dx * dx + dy * dy).toFloat())
                if (dist > radius) continue

                // 笔刷 alpha 衰减
                val brushAlpha = when {
                    dist <= innerR -> 1f
                    else -> {
                        val t = (radius - dist) / (radius - innerR).coerceAtLeast(0.001f)
                        smoothstep(0f, 1f, t)
                    }
                }

                val addAlpha = (maxAlpha * brushAlpha).toInt().coerceIn(0, 255)
                val idx = y * width + x
                pixels[idx] = min(255, pixels[idx] + addAlpha)
            }
        }
    }

    /**
     * AI 蒙版占位实现
     * 实际效果由 AIMaskProvider 注入识别结果后调用 generateLinearGradient/Radial
     * 这里返回一个全选中的占位蒙版，由具体 AI provider 替换
     */
    fun generateAIMaskPlaceholder(
        width: Int,
        height: Int,
        params: AIMaskParams
    ): Bitmap {
        // 默认全黑（未选中），具体 AI provider 替换为识别结果
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        return bitmap
    }

    /**
     * 亮度蒙版占位
     * 实际生成需先计算原图亮度直方图
     */
    fun generateLuminancePlaceholder(
        width: Int,
        height: Int,
        range: LuminanceRange
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        return bitmap
    }

    /**
     * 颜色蒙版占位
     */
    fun generateColorPlaceholder(
        width: Int,
        height: Int,
        target: ColorTarget
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        return bitmap
    }

    /**
     * 应用整体不透明度与反转
     */
    private fun applyOpacityAndInvert(bitmap: Bitmap, opacity: Float, invert: Boolean) {
        if (opacity >= 1f && !invert) return
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            var v = (pixels[i] * opacity).toInt().coerceIn(0, 255)
            if (invert) v = 255 - v
            pixels[i] = v
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun shouldInvert(mask: AdjustmentMask): Boolean {
        return when (mask.type) {
            MaskType.LINEAR_GRADIENT -> mask.gradientParams.invert
            MaskType.RADIAL -> mask.gradientParams.invert
            MaskType.COLOR -> mask.colorTarget.invertSelection
            else -> false
        }
    }

    private fun abs(v: Float): Float = if (v < 0) -v else v

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
