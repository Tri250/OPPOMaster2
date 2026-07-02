package com.silas.omaster.data.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import java.io.File
import kotlin.math.roundToInt

/**
 * 水印渲染引擎
 * 支持品牌水印、大师印记、XPAN 宽幅水印
 */
object WatermarkEngine {

    /**
     * 应用水印到图片
     */
    fun applyWatermark(
        source: Bitmap,
        config: WatermarkConfig,
        signatureBitmap: Bitmap? = null
    ): Bitmap {
        return when (config.type) {
            WatermarkType.BRAND -> applyBrandWatermark(source, config)
            WatermarkType.MASTER_MARK -> applyMasterMarkWatermark(source, config, signatureBitmap)
            WatermarkType.XPAN -> applyXpanWatermark(source, config)
        }
    }

    /**
     * 品牌水印：左下/右下文字水印
     */
    private fun applyBrandWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(config.fontColor)
            textSize = config.fontSize.coerceIn(12f, 200f)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 0f, 1f, Color.BLACK)
        }

        // 计算位置
        val (x, y) = calculatePosition(
            result.width, result.height,
            config.position, config.offsetX, config.offsetY,
            paint, config.text
        )

        // 绘制背景（可选）
        config.backgroundColor?.let { bgColor ->
            val bounds = Rect()
            paint.getTextBounds(config.text, 0, config.text.length, bounds)
            val bgPaint = Paint().apply {
                color = Color.parseColor(bgColor)
                alpha = (config.backgroundAlpha * 255).roundToInt().coerceIn(0, 255)
            }
            val padding = 8
            canvas.drawRect(
                (x - padding).toFloat(),
                (y + bounds.top - padding).toFloat(),
                (x + bounds.width() + padding).toFloat(),
                (y + bounds.bottom + padding).toFloat(),
                bgPaint
            )
        }

        canvas.drawText(config.text, x, y, paint)
        return result
    }

    /**
     * 大师印记水印：叠加签名图
     */
    private fun applyMasterMarkWatermark(
        source: Bitmap,
        config: WatermarkConfig,
        signatureBitmap: Bitmap?
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        if (signatureBitmap == null || signatureBitmap.isRecycled) {
            // 没有签名图时降级为品牌水印
            return applyBrandWatermark(result, config.copy(type = WatermarkType.BRAND))
        }

        val canvas = Canvas(result)

        // 缩放签名图到合适大小（不超过图片短边的 30%）
        val maxSize = (minOf(result.width, result.height) * 0.3f).roundToInt()
        val scale = minOf(
            maxSize.toFloat() / signatureBitmap.width,
            maxSize.toFloat() / signatureBitmap.height
        )
        val newWidth = (signatureBitmap.width * scale).roundToInt()
        val newHeight = (signatureBitmap.height * scale).roundToInt()

        val scaledSignature = Bitmap.createScaledBitmap(signatureBitmap, newWidth, newHeight, true)

        // 计算位置
        val (x, y) = calculatePositionForBitmap(
            result.width, result.height,
            config.position, config.offsetX, config.offsetY,
            newWidth, newHeight
        )

        // 绘制半透明签名
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (config.signatureAlpha * 255).roundToInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(scaledSignature, x.toFloat(), y.toFloat(), paint)

        if (scaledSignature !== signatureBitmap) {
            scaledSignature.recycle()
        }

        return result
    }

    /**
     * XPAN 宽幅水印：上下黑边 + 文字
     */
    private fun applyXpanWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        val topHeight = (source.height * config.xpanTopRatio).roundToInt()
        val bottomHeight = (source.height * config.xpanBottomRatio).roundToInt()
        val newHeight = source.height + topHeight + bottomHeight

        val result = Bitmap.createBitmap(source.width, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 绘制上黑边
        canvas.drawColor(Color.BLACK)

        // 绘制原图（居中）
        canvas.drawBitmap(source, 0f, topHeight.toFloat(), null)

        // 上黑边文字
        if (config.xpanTextTop.isNotEmpty()) {
            val topPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (topHeight * 0.4f).coerceIn(12f, 80f)
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                config.xpanTextTop,
                result.width / 2f,
                topHeight * 0.65f,
                topPaint
            )
        }

        // 下黑边文字
        if (config.xpanTextBottom.isNotEmpty()) {
            val bottomPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (bottomHeight * 0.4f).coerceIn(12f, 80f)
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                config.xpanTextBottom,
                result.width / 2f,
                source.height + topHeight + bottomHeight * 0.65f,
                bottomPaint
            )
        }

        return result
    }

    /**
     * 计算文字水印位置
     */
    private fun calculatePosition(
        imgWidth: Int, imgHeight: Int,
        position: WatermarkPosition,
        offsetX: Float, offsetY: Float,
        paint: Paint, text: String
    ): Pair<Float, Float> {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()

        val marginX = (imgWidth * offsetX).roundToInt()
        val marginY = (imgHeight * offsetY).roundToInt()

        return when (position) {
            WatermarkPosition.BOTTOM_LEFT -> {
                marginX.toFloat() to (imgHeight - marginY).toFloat()
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                (imgWidth - textWidth - marginX).toFloat() to (imgHeight - marginY).toFloat()
            }
            WatermarkPosition.TOP_LEFT -> {
                marginX.toFloat() to (marginY + textHeight).toFloat()
            }
            WatermarkPosition.TOP_RIGHT -> {
                (imgWidth - textWidth - marginX).toFloat() to (marginY + textHeight).toFloat()
            }
            WatermarkPosition.CENTER -> {
                ((imgWidth - textWidth) / 2f) to ((imgHeight + textHeight) / 2f)
            }
        }
    }

    /**
     * 计算图片水印位置
     */
    private fun calculatePositionForBitmap(
        imgWidth: Int, imgHeight: Int,
        position: WatermarkPosition,
        offsetX: Float, offsetY: Float,
        bitmapWidth: Int, bitmapHeight: Int
    ): Pair<Int, Int> {
        val marginX = (imgWidth * offsetX).roundToInt()
        val marginY = (imgHeight * offsetY).roundToInt()

        return when (position) {
            WatermarkPosition.BOTTOM_LEFT -> {
                marginX to (imgHeight - bitmapHeight - marginY)
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                (imgWidth - bitmapWidth - marginX) to (imgHeight - bitmapHeight - marginY)
            }
            WatermarkPosition.TOP_LEFT -> {
                marginX to marginY
            }
            WatermarkPosition.TOP_RIGHT -> {
                (imgWidth - bitmapWidth - marginX) to marginY
            }
            WatermarkPosition.CENTER -> {
                ((imgWidth - bitmapWidth) / 2) to ((imgHeight - bitmapHeight) / 2)
            }
        }
    }
}
