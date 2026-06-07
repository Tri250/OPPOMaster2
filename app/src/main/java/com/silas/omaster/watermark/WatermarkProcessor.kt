package com.silas.omaster.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.silas.omaster.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 水印处理器
 */
class WatermarkProcessor(private val context: Context) {

    companion object {
        private const val OPPO_ORANGE = 0xFFD4A574.toInt()
        private const val ONEPLUS_RED = 0xFFF50514.toInt()
        private const val REALME_YELLOW = 0xFFFFE70A.toInt()
        private const val HASSELBLAD_GOLD = 0xFFC9A962.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK_TRANSLUCENT = 0xCC000000.toInt()
    }

    suspend fun processWatermark(request: WatermarkProcessRequest): WatermarkProcessResult =
        withContext(Dispatchers.IO) {
            try {
                val resultBitmap = processWatermarkInternal(request)
                WatermarkProcessResult(success = true, bitmap = resultBitmap)
            } catch (e: Exception) {
                WatermarkProcessResult(success = false, error = e.message)
            }
        }

    suspend fun batchProcessWatermarks(
        requests: List<WatermarkProcessRequest>
    ): List<WatermarkProcessResult> = withContext(Dispatchers.IO) {
        requests.map { processWatermark(it) }
    }

    private fun processWatermarkInternal(request: WatermarkProcessRequest): Bitmap {
        val source = request.sourceBitmap
        val config = request.config
        val width = source.width
        val height = source.height

        val result = source.copy(source.config, true)
        val canvas = Canvas(result)

        when (config.template) {
            WatermarkTemplate.OPPO -> drawOppoWatermark(canvas, width, height, config)
            WatermarkTemplate.ONEPLUS -> drawOneplusWatermark(canvas, width, height, config)
            WatermarkTemplate.REALME -> drawRealmeWatermark(canvas, width, height, config)
            WatermarkTemplate.MINIMAL_PARAMS -> drawMinimalParamsWatermark(canvas, width, height, config)
            WatermarkTemplate.TIMESTAMP -> drawTimestampWatermark(canvas, width, height, config)
            WatermarkTemplate.LOCATION -> drawLocationWatermark(canvas, width, height, config)
            WatermarkTemplate.CUSTOM -> drawCustomWatermark(canvas, width, height, config)
            WatermarkTemplate.HASSELBLAD -> drawHasselbladWatermark(canvas, width, height, config)
            WatermarkTemplate.BRAND_SIMPLE -> drawBrandSimpleWatermark(canvas, width, height, config)
            WatermarkTemplate.FILM_STYLE -> drawFilmStyleWatermark(canvas, width, height, config)
            WatermarkTemplate.TILE_PATTERN -> OpenSourceWatermarkTemplates.drawTilePatternWatermark(
                canvas, width, height, config.customText ?: "SAMPLE", config
            )
            WatermarkTemplate.DIAGONAL_TEXT -> OpenSourceWatermarkTemplates.drawDiagonalTextWatermark(
                canvas, width, height, config.customText ?: "COPYRIGHT", config
            )
            WatermarkTemplate.CAMERA_INFO -> OpenSourceWatermarkTemplates.drawCameraInfoWatermark(
                canvas, width, height, config
            )
            WatermarkTemplate.DATE_STAMP -> OpenSourceWatermarkTemplates.drawDateStampWatermark(
                canvas, width, height, config.customText ?: "身份核验", config
            )
            WatermarkTemplate.COPYRIGHT_SIGN -> OpenSourceWatermarkTemplates.drawCopyrightSignWatermark(
                canvas, width, height, config.customText ?: "Author", config
            )
            WatermarkTemplate.SIGNATURE -> OpenSourceWatermarkTemplates.drawSignatureWatermark(
                canvas, width, height, config.customText ?: "Signature", config
            )
            WatermarkTemplate.COLLAGE_GRID -> OpenSourceWatermarkTemplates.drawCollageGridWatermark(
                canvas, width, height, config
            )
            WatermarkTemplate.SOCIAL_MEDIA -> OpenSourceWatermarkTemplates.drawSocialMediaWatermark(
                canvas, width, height, "instagram", config.customText ?: "username", config
            )
            WatermarkTemplate.MINIMAL_CORNER -> OpenSourceWatermarkTemplates.drawMinimalCornerWatermark(
                canvas, width, height, config.customText ?: "© 2026", config
            )
            WatermarkTemplate.QR_CODE -> drawCustomWatermark(canvas, width, height, config)
            WatermarkTemplate.STAMP -> OpenSourceWatermarkTemplates.drawStampWatermark(
                canvas, width, height, config.customText ?: "北京", config
            )
            WatermarkTemplate.CHINESE_STYLE -> OpenSourceWatermarkTemplates.drawChineseStyleWatermark(
                canvas, width, height, config.customText ?: "摄影", config
            )
            WatermarkTemplate.FILM_FRAME -> OpenSourceWatermarkTemplates.drawFilmFrameWatermark(
                canvas, width, height, config
            )
            WatermarkTemplate.NEW_YEAR -> OpenSourceWatermarkTemplates.drawNewYearWatermark(
                canvas, width, height, config.customText ?: "新春快乐", config
            )
            WatermarkTemplate.LEICA_CLASSIC -> drawLeicaWatermark(canvas, width, height, config)
            WatermarkTemplate.ZEISS_OPTICS -> drawZeissWatermark(canvas, width, height, config)
        }

        return result
    }

    private fun drawOppoWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = OPPO_ORANGE
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("OPPO", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawOneplusWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = ONEPLUS_RED
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("OnePlus", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawRealmeWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.4f * config.scale
        val boxHeight = height * 0.15f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = REALME_YELLOW
        paint.textSize = boxHeight * 0.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("realme", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawHasselbladWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.12f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = HASSELBLAD_GOLD
        paint.textSize = boxHeight * 0.45f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("HASSELBLAD", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawLeicaWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.12f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        // 绘制徕卡红标
        val redDotPaint = Paint().apply {
            color = Color.parseColor("#FF0000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(boxRect.left + 15 * config.scale, boxRect.centerY(), 6 * config.scale, redDotPaint)
        
        // 绘制LEICA文字
        paint.color = Color.WHITE
        paint.textSize = boxHeight * 0.45f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("LEICA", boxRect.left + 30 * config.scale, textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawZeissWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.12f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = Color.parseColor("#0077BE")
        paint.textSize = boxHeight * 0.35f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText("ZEISS T*", boxRect.centerX(), textY, paint)

        if (config.showTimestamp) {
            drawTimestamp(canvas, boxRect, paint, config.timestampFormat)
        }
    }

    private fun drawBrandSimpleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        paint.color = WHITE
        paint.alpha = (255 * config.opacity).toInt()
        paint.textSize = height * 0.04f * config.scale
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        
        val margin = width * 0.05f
        val y = height - margin
        
        canvas.drawText("OMaster", width / 2f, y, paint)
    }

    private fun drawFilmStyleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.3f * config.scale
        val boxHeight = height * 0.18f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 8f)
        
        paint.color = 0xFFE0E0E0.toInt()
        paint.textSize = boxHeight * 0.22f
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.LEFT
        
        val params = config.cameraParams ?: CameraParamsForWatermark()
        
        val paramsList = listOf(
            "ISO ${params.iso}",
            params.aperture,
            params.shutterSpeed,
            "EV ${params.ev}"
        )
        
        paramsList.forEachIndexed { index, param ->
            val y = boxRect.top + boxHeight * 0.2f + index * boxHeight * 0.18f
            canvas.drawText(param, boxRect.left + 12f, y, paint)
        }

        if (config.showTimestamp) {
            paint.textSize = boxHeight * 0.18f
            paint.typeface = Typeface.DEFAULT
            val dateFormat = SimpleDateFormat(config.timestampFormat, Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            canvas.drawText(timestamp, boxRect.left + 12f, boxRect.bottom - 8f, paint)
        }
    }

    private fun drawMinimalParamsWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.28f * config.scale
        val boxHeight = height * 0.16f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 10f)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.22f
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.LEFT
        
        val params = config.cameraParams ?: CameraParamsForWatermark()
        
        val paramsList = listOf(
            params.shutterSpeed,
            params.aperture,
            "ISO ${params.iso}"
        )
        
        paramsList.forEachIndexed { index, param ->
            val y = boxRect.top + boxHeight * 0.25f + index * boxHeight * 0.22f
            canvas.drawText(param, boxRect.left + 10f, y, paint)
        }
    }

    private fun drawTimestampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.35f * config.scale
        val boxHeight = height * 0.08f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity, cornerRadius = 6f)
        
        val dateFormat = SimpleDateFormat(config.timestampFormat, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.55f
        paint.textAlign = Paint.Align.CENTER
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(timestamp, boxRect.centerX(), textY, paint)
    }

    private fun drawLocationWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val boxWidth = width * 0.3f * config.scale
        val boxHeight = height * 0.1f * config.scale
        val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
        
        drawRoundedBackground(canvas, boxRect, config.opacity)
        
        paint.color = WHITE
        paint.textSize = boxHeight * 0.45f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        
        val text = config.customText ?: "Unknown Location"
        val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
        canvas.drawText(text, boxRect.centerX(), textY, paint)
    }

    private fun drawCustomWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        config.customText?.let { text ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            val boxWidth = width * 0.35f * config.scale
            val boxHeight = height * 0.1f * config.scale
            val boxRect = getPositionRect(width.toFloat(), height.toFloat(), boxWidth, boxHeight, config.position)
            
            drawRoundedBackground(canvas, boxRect, config.opacity)
            
            paint.color = WHITE
            paint.textSize = boxHeight * 0.5f
            paint.textAlign = Paint.Align.CENTER
            val textY = boxRect.centerY() + paint.textSize / 2 - paint.descent()
            canvas.drawText(text, boxRect.centerX(), textY, paint)
        }
    }

    private fun drawRoundedBackground(
        canvas: Canvas,
        rect: RectF,
        opacity: Float,
        cornerRadius: Float = 12f
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = BLACK_TRANSLUCENT
        bgPaint.alpha = (255 * opacity).toInt()
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    }

    private fun drawTimestamp(
        canvas: Canvas,
        rect: RectF,
        paint: Paint,
        format: String
    ) {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        
        val originalSize = paint.textSize
        paint.textSize *= 0.55f
        paint.alpha = 200
        paint.typeface = Typeface.DEFAULT
        val y = rect.bottom - 6f
        canvas.drawText(timestamp, rect.centerX(), y, paint)
        
        paint.textSize = originalSize
        paint.alpha = 255
    }

    private fun getPositionRect(
        width: Float,
        height: Float,
        boxWidth: Float,
        boxHeight: Float,
        position: WatermarkPosition
    ): RectF {
        val margin = width * 0.03f
        
        val left = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.BOTTOM_LEFT -> margin
            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.CENTER,
            WatermarkPosition.BOTTOM_CENTER -> (width - boxWidth) / 2f
            WatermarkPosition.TOP_RIGHT,
            WatermarkPosition.BOTTOM_RIGHT -> width - boxWidth - margin
        }
        
        val top = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.TOP_RIGHT -> margin
            WatermarkPosition.CENTER -> (height - boxHeight) / 2f
            WatermarkPosition.BOTTOM_LEFT,
            WatermarkPosition.BOTTOM_CENTER,
            WatermarkPosition.BOTTOM_RIGHT -> height - boxHeight - margin
        }
        
        return RectF(left, top, left + boxWidth, top + boxHeight)
    }
}