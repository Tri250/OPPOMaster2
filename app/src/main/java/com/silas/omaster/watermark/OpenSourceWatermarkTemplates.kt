package com.silas.omaster.watermark

import android.graphics.*
import com.silas.omaster.model.WatermarkConfig
import java.text.SimpleDateFormat
import java.util.*

/**
 * 开源水印模板库 - 免费商用
 * 参考: Easy Watermark (MIT), Photix Mark (开源), darktable水印模板
 */
object OpenSourceWatermarkTemplates {

    /**
     * 平铺水印模板 - 防盗用
     */
    fun drawTilePatternWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 36f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val textWidth = paint.measureText(text)
        val textHeight = paint.fontMetrics.let { it.bottom - it.top }
        
        val horizontalSpacing = (textWidth + 100) * config.scale
        val verticalSpacing = (textHeight + 60) * config.scale
        
        val rotationAngle = 30f

        canvas.save()
        canvas.rotate(rotationAngle, width / 2f, height / 2f)
        var y = -height.toFloat()
        while (y < height * 2f) {
            var x = -width.toFloat()
            while (x < width * 2f) {
                canvas.drawText(text, x, y, paint)
                x += horizontalSpacing
            }
            y += verticalSpacing
        }
        canvas.restore()
    }

    /**
     * 对角线文字水印 - 版权保护
     */
    fun drawDiagonalTextWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 48f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val angle = -Math.toDegrees(Math.atan(height.toDouble() / width.toDouble())).toFloat()

        canvas.withRotation(angle, centerX, centerY) {
            val textWidth = paint.measureText(text)
            drawText(text, centerX - textWidth / 2, centerY, paint)
        }
    }

    /**
     * 相机参数水印 - Leica/小米风格
     */
    fun drawCameraInfoWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val params = config.cameraParams ?: return
        val padding = 20f * config.scale
        
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (0.6f * 255).toInt()
            style = Paint.Style.FILL
        }
        
        val textPaint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 24f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val smallTextPaint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.8f * 255).toInt()
            textSize = 18f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val cameraModel = "OPPO Find X8 Ultra"
        val paramLine1 = "${params.aperture}  ${params.shutterSpeed}  ISO ${params.iso}"
        val paramLine2 = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        val maxWidth = maxOf(
            textPaint.measureText(cameraModel),
            textPaint.measureText(paramLine1),
            smallTextPaint.measureText(paramLine2)
        )
        val bgWidth = maxWidth + padding * 2
        val bgHeight = 90f * config.scale

        val left = width - bgWidth - padding
        val top = height - bgHeight - padding
        val rect = RectF(left, top, left + bgWidth, top + bgHeight)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val textStartX = left + padding
        var textY = top + 30f * config.scale
        canvas.drawText(cameraModel, textStartX, textY, textPaint)
        
        textY += 28f * config.scale
        canvas.drawText(paramLine1, textStartX, textY, textPaint)
        
        textY += 24f * config.scale
        canvas.drawText(paramLine2, textStartX, textY, smallTextPaint)
    }

    /**
     * 日期印章水印 - 证件照专用
     */
    fun drawDateStampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        purpose: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF4444")
            alpha = (config.opacity * 255).toInt()
            textSize = 28f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val date = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date())
        val text1 = "本照片仅限【$purpose】审核之用"
        val text2 = "他用无效 · $date"

        val centerX = width / 2f
        val centerY = height / 2f

        val bgPaint = Paint().apply {
            color = Color.WHITE
            alpha = (0.9f * 255).toInt()
            style = Paint.Style.FILL
        }
        val bgRect = RectF(
            centerX - 200 * config.scale,
            centerY - 50 * config.scale,
            centerX + 200 * config.scale,
            centerY + 50 * config.scale
        )
        canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)

        canvas.drawText(text1, centerX, centerY - 10 * config.scale, paint)
        paint.textSize = 22f * config.scale
        canvas.drawText(text2, centerX, centerY + 25 * config.scale, paint)
    }

    /**
     * 版权符号水印 - ©️风格
     */
    fun drawCopyrightSignWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        author: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 32f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val text = "© $year $author"

        val padding = 30f * config.scale
        val x = width - paint.measureText(text) - padding
        val y = height - padding

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 签名水印 - 手写风格
     */
    fun drawSignatureWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        signature: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#333333")
            alpha = (config.opacity * 255).toInt()
            textSize = 40f * config.scale
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val padding = 40f * config.scale
        val x = width - paint.measureText(signature) - padding
        val y = height - padding

        canvas.withRotation(-5f, x + paint.measureText(signature) / 2, y) {
            drawText(signature, x, y, paint)
        }
    }

    /**
     * 拼图九宫格水印
     */
    fun drawCollageGridWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.5f * 255).toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        val thirdWidth = width / 3f
        val thirdHeight = height / 3f

        canvas.drawLine(thirdWidth, 0f, thirdWidth, height.toFloat(), paint)
        canvas.drawLine(thirdWidth * 2, 0f, thirdWidth * 2, height.toFloat(), paint)

        canvas.drawLine(0f, thirdHeight, width.toFloat(), thirdHeight, paint)
        canvas.drawLine(0f, thirdHeight * 2, width.toFloat(), thirdHeight * 2, paint)
    }

    /**
     * 社交媒体水印
     */
    fun drawSocialMediaWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        platform: String,
        username: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 26f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val text = "@$username"
        val padding = 30f * config.scale
        val x = padding
        val y = height - padding

        val icon = when (platform.lowercase()) {
            "instagram" -> "📷"
            "twitter", "x" -> "🐦"
            "weibo" -> "📱"
            "xiaohongshu" -> "📕"
            else -> "🔗"
        }
        
        val iconPaint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 30f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        canvas.drawText(icon, x, y - 5 * config.scale, iconPaint)
        canvas.drawText(text, x + 40 * config.scale, y, paint)
    }

    /**
     * 极简角标水印
     */
    fun drawMinimalCornerWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.6f * 255).toInt()
            textSize = 18f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val padding = 20f * config.scale
        val x = width - paint.measureText(text) - padding
        val y = height - padding

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 邮票邮戳水印 - vivo风格
     */
    fun drawStampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        location: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#8B4513")
            alpha = (config.opacity * 255).toInt()
            textSize = 24f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        val centerX = width / 2f
        val centerY = height / 2f

        val borderPaint = Paint().apply {
            color = Color.parseColor("#8B4513")
            alpha = (config.opacity * 0.5f * 255).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * config.scale
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, 60f * config.scale, borderPaint)

        canvas.drawText(location, centerX, centerY - 10 * config.scale, paint)
        paint.textSize = 16f * config.scale
        canvas.drawText(date, centerX, centerY + 20 * config.scale, paint)
    }

    /**
     * 国风印章水印 - 水墨风格
     */
    fun drawChineseStyleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#C41E3A")
            alpha = (config.opacity * 255).toInt()
            textSize = 32f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val padding = 30f * config.scale
        val x = width - padding - 40 * config.scale
        val y = height - padding

        val borderPaint = Paint().apply {
            color = Color.parseColor("#C41E3A")
            alpha = (config.opacity * 0.7f * 255).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * config.scale
            isAntiAlias = true
        }
        val rect = RectF(x - 30 * config.scale, y - 40 * config.scale, x + 30 * config.scale, y + 10 * config.scale)
        canvas.drawRect(rect, borderPaint)

        canvas.drawText(text, x, y - 10 * config.scale, paint)
    }

    /**
     * 胶片相框水印 - 小米风格
     */
    fun drawFilmFrameWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 14f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val barHeight = 30f * config.scale
        val barPaint = Paint().apply {
            color = Color.BLACK
            alpha = (config.opacity * 0.8f * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - barHeight, width.toFloat(), height.toFloat(), barPaint)

        val params = config.cameraParams
        val text = if (params != null) {
            "f/${params.aperture}  ${params.shutterSpeed}  ISO ${params.iso}"
        } else {
            "f/1.8  1/125  ISO 100"
        }
        canvas.drawText(text, 20f * config.scale, height - 10 * config.scale, paint)
    }

    /**
     * 新春舞狮水印 - 小米非遗
     */
    fun drawNewYearWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        greeting: String,
        config: WatermarkConfig
    ) {
        val barHeight = 50f * config.scale
        val barPaint = Paint().apply {
            color = Color.parseColor("#FF0000")
            alpha = (config.opacity * 0.9f * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - barHeight, width.toFloat(), height.toFloat(), barPaint)

        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 28f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val text = "$greeting · $year"
        canvas.drawText(text, width / 2f, height - 15 * config.scale, paint)
    }
}

/**
 * 扩展Canvas旋转功能
 */
private inline fun Canvas.withRotation(
    degrees: Float,
    pivotX: Float = 0f,
    pivotY: Float = 0f,
    block: Canvas.() -> Unit
) {
    save()
    rotate(degrees, pivotX, pivotY)
    block()
    restore()
}