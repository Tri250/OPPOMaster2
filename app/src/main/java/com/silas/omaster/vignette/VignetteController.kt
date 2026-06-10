package com.silas.omaster.vignette

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 暗角控制器
 *
 * P2 14. 暗角与畸变控制
 * - 暗角强度 (0-100)
 * - 暗角形状选择：圆形/椭圆/方形
 * - 暗角中心点可拖拽定位
 * - 畸变校正：桶形/枕形畸变滑块
 *
 * 与 AIFineTuneManager.AdjustmentParams 的 vignette 字段集成
 */
class VignetteController {

    /**
     * 暗角形状
     */
    enum class VignetteShape(val displayName: String) {
        CIRCLE("圆形"),
        ELLIPSE("椭圆"),
        SQUARE("方形"),
        RECTANGLE("矩形");
    }

    /**
     * 畸变类型
     */
    enum class DistortionType(val displayName: String) {
        NONE("无"),
        BARREL("桶形畸变"),
        PINCUSHION("枕形畸变");
    }

    /**
     * 暗角参数
     * @property intensity 暗角强度 [0, 100]
     * @property shape 暗角形状
     * @property centerX 中心点 X（归一化 [0, 1]）
     * @property centerY 中心点 Y（归一化 [0, 1]）
     * @property radius 暗角半径（归一化 [0, 1]）
     * @property feather 羽化程度 [0, 1]
     * @property roundness 圆角（方形时有效）[0, 1]
     * @property aspectRatio 宽高比（椭圆时有效）
     */
    data class VignetteParams(
        val intensity: Int = 30,
        val shape: VignetteShape = VignetteShape.CIRCLE,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
        val radius: Float = 0.7f,
        val feather: Float = 0.5f,
        val roundness: Float = 0.5f,
        val aspectRatio: Float = 1.0f
    ) {
        companion object {
            val DEFAULT = VignetteParams()
            val NONE = VignetteParams(intensity = 0)
        }
    }

    /**
     * 畸变参数
     * @property type 畸变类型
     * @property strength 畸变强度 [-100, 100]
     * @property centerX 畸变中心 X（归一化）
     * @property centerY 畸变中心 Y（归一化）
     */
    data class DistortionParams(
        val type: DistortionType = DistortionType.NONE,
        val strength: Int = 0,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f
    ) {
        companion object {
            val DEFAULT = DistortionParams()
        }
    }

    /**
     * 应用暗角到图像
     * @param source 原图
     * @param params 暗角参数
     * @return 应用暗角后的图像
     */
    fun applyVignette(
        source: Bitmap,
        params: VignetteParams
    ): Bitmap {
        if (params.intensity == 0) return source

        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // 创建暗角蒙版
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        vignettePaint.shader = createVignetteShader(width, height, params)
        vignettePaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_OVER)

        // 绘制暗角
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

        return output
    }

    /**
     * 创建暗角渐变着色器
     */
    private fun createVignetteShader(
        width: Int,
        height: Int,
        params: VignetteParams
    ): Shader {
        val cx = params.centerX * width
        val cy = params.centerY * height
        val maxRadius = when (params.shape) {
            VignetteShape.CIRCLE -> min(width, height) / 2f * params.radius * 1.5f
            VignetteShape.ELLIPSE -> {
                val aspect = params.aspectRatio.coerceAtLeast(0.5f)
                sqrt((width / 2f).pow(2) + (height / 2f / aspect).pow(2)) * params.radius
            }
            VignetteShape.SQUARE -> min(width, height) / 2f * params.radius * 1.2f
            VignetteShape.RECTANGLE -> sqrt((width / 2f).pow(2) + (height / 2f).pow(2)) * params.radius
        }

        val innerRadius = maxRadius * (1f - params.feather)
        val intensity = params.intensity / 100f

        // 暗角颜色：中心透明，边缘黑色
        val centerColor = Color.argb(0, 0, 0, 0)
        val edgeColor = Color.argb((255 * intensity).toInt(), 0, 0, 0)

        return RadialGradient(
            cx, cy, maxRadius,
            intArrayOf(centerColor, centerColor, edgeColor),
            floatArrayOf(0f, innerRadius / maxRadius, 1f),
            Shader.TileMode.CLAMP
        )
    }

    /**
     * 应用畸变校正到图像
     * @param source 原图
     * @param params 畸变参数
     * @return 校正后的图像
     */
    fun applyDistortionCorrection(
        source: Bitmap,
        params: DistortionParams
    ): Bitmap {
        if (params.type == DistortionType.NONE || params.strength == 0) return source

        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height) { Color.TRANSPARENT }

        val cx = params.centerX * width
        val cy = params.centerY * height
        val maxR = sqrt(cx * cx + cy * cy)

        // 畸变系数
        val k = params.strength / 1000f * if (params.type == DistortionType.BARREL) -1f else 1f

        for (y in 0 until height) {
            for (x in 0 until width) {
                // 归一化坐标
                val dx = x - cx
                val dy = y - cy
                val r = sqrt(dx * dx + dy * dy) / maxR

                // 畸变校正公式：r' = r * (1 + k * r^2)
                val rPrime = r * (1f + k * r * r)

                // 源坐标
                val srcX = (cx + dx * rPrime / r.coerceAtLeast(0.001f)).toInt().coerceIn(0, width - 1)
                val srcY = (cy + dy * rPrime / r.coerceAtLeast(0.001f)).toInt().coerceIn(0, height - 1)

                outPixels[y * width + x] = srcPixels[srcY * width + srcX]
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * 同时应用暗角和畸变校正
     */
    fun applyVignetteAndDistortion(
        source: Bitmap,
        vignetteParams: VignetteParams,
        distortionParams: DistortionParams
    ): Bitmap {
        var result = source

        // 先应用畸变校正
        if (distortionParams.type != DistortionType.NONE && distortionParams.strength != 0) {
            result = applyDistortionCorrection(result, distortionParams)
        }

        // 再应用暗角
        if (vignetteParams.intensity != 0) {
            result = applyVignette(result, vignetteParams)
        }

        return result
    }

    /**
     * 预览暗角效果（用于 UI 预览）
     * @param width 预览宽度
     * @param height 预览高度
     * @param params 暗角参数
     * @return 暗角预览 Bitmap
     */
    fun createVignettePreview(
        width: Int,
        height: Int,
        params: VignetteParams
    ): Bitmap {
        val preview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(preview)

        // 填充白色背景
        canvas.drawColor(Color.WHITE)

        // 绘制暗角
        if (params.intensity > 0) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = createVignetteShader(width, height, params)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        // 绘制中心点标记
        val cx = params.centerX * width
        val cy = params.centerY * height
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        markerPaint.color = Color.RED
        markerPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, 8f, markerPaint)

        return preview
    }

    companion object {
        @Volatile
        private var instance: VignetteController? = null

        fun getInstance(): VignetteController {
            return instance ?: synchronized(this) {
                instance ?: VignetteController().also { instance = it }
            }
        }
    }
}
