package com.silas.omaster.data.util

import android.content.Context
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import com.silas.omaster.data.model.*
import com.silas.omaster.ui.theme.HasselbladColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 水印渲染器
 * 负责将水印模板渲染到图片上
 */
class WatermarkRenderer(private val context: Context) {

    private val exifProvider = ExifWatermarkProvider(context)
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

    /**
     * 导出带水印的图片
     */
    suspend fun exportWatermarkedImage(
        sourceUri: Uri,
        template: MasterWatermarkTemplate,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 95
    ): File = withContext(Dispatchers.IO) {
        // 1. 加载原图
        val sourceBitmap = loadBitmap(sourceUri)
        
        // 2. 提取 EXIF
        val exifData = exifProvider.extractFromUri(sourceUri)
        
        // 3. 应用水印
        val watermarked = render(sourceBitmap, template, exifData)
        
        // 4. 写入文件
        val outputDir = File(context.cacheDir, "watermarked")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "OMaster_${System.currentTimeMillis()}.${format.extension}")
        
        outputFile.outputStream().use { stream ->
            watermarked.compress(format.compressFormat, quality, stream)
        }
        
        // 5. 通知相册刷新
        MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)
        
        // 6. 回收 Bitmap
        if (watermarked != sourceBitmap) {
            watermarked.recycle()
        }
        sourceBitmap.recycle()
        
        outputFile
    }

    /**
     * 渲染水印
     */
    fun render(
        sourceBitmap: Bitmap,
        template: MasterWatermarkTemplate,
        exifData: ExifWatermarkData
    ): Bitmap {
        // 创建可变 Bitmap
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // 根据模板 ID 选择渲染方式
        when (template.id) {
            "hasselblad-master" -> renderHasselbladMaster(canvas, result, exifData)
            "hasselblad-hncs" -> renderHasselbladHncs(canvas, result, exifData)
            "hasselblad-xpan" -> renderHasselbladXpan(canvas, result, exifData)
            else -> renderGeneric(canvas, result, template, exifData)
        }
        
        return result
    }

    /**
     * 渲染哈苏大师印记
     */
    private fun renderHasselbladMaster(
        canvas: Canvas,
        bitmap: Bitmap,
        exifData: ExifWatermarkData
    ) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        
        // 底部信息栏高度 (8% 画面高度)
        val barHeight = height * 0.08f
        val barTop = height - barHeight
        
        // 绘制半透明背景
        val bgPaint = Paint().apply {
            color = HasselbladColors.BackgroundSemiTransparent.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, barTop, width, height, bgPaint)
        
        // 文字画笔
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        // 金色分割线
        val dividerPaint = Paint().apply {
            color = HasselbladColors.Gold.toArgb()
            strokeWidth = 1f
            alpha = 77 // 30%
        }
        val dividerY = barTop + barHeight * 0.15f
        canvas.drawLine(width * 0.2f, dividerY, width * 0.8f, dividerY, dividerPaint)
        
        // HASSELBLAD 品牌
        textPaint.apply {
            textSize = barHeight * 0.25f
            color = HasselbladColors.Gold.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        val brandY = barTop + barHeight * 0.4f
        canvas.drawText("HASSELBLAD", width / 2, brandY, textPaint)
        
        // HNCS 认证标识
        textPaint.apply {
            textSize = barHeight * 0.12f
            color = HasselbladColors.TextSecondary.toArgb()
            letterSpacing = 0.05f
        }
        canvas.drawText("HNCS", width / 2, brandY + barHeight * 0.2f, textPaint)
        
        // 设备型号
        textPaint.apply {
            textSize = barHeight * 0.15f
            color = HasselbladColors.TextSecondary.toArgb()
            typeface = Typeface.DEFAULT
            letterSpacing = 0f
        }
        val deviceInfo = exifData.getDeviceInfo()
        canvas.drawText(deviceInfo, width / 2, brandY + barHeight * 0.4f, textPaint)
        
        // 拍摄参数
        textPaint.apply {
            textSize = barHeight * 0.12f
            typeface = Typeface.MONOSPACE
            color = HasselbladColors.TextTertiary.toArgb()
        }
        val paramsInfo = exifData.getParamsInfo()
        canvas.drawText(paramsInfo, width / 2, brandY + barHeight * 0.6f, textPaint)
        
        // 日期和位置
        val dateStr = exifData.dateTaken ?: dateFormat.format(Date())
        textPaint.apply {
            textSize = barHeight * 0.1f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(dateStr, width / 2, barTop + barHeight * 0.85f, textPaint)
        
        // 底部品牌联合标识
        textPaint.apply {
            textSize = barHeight * 0.08f
            color = HasselbladColors.TextTertiary.toArgb()
        }
        canvas.drawText("OPPO × Hasselblad | Master Edition", width / 2, barTop + barHeight * 0.95f, textPaint)
    }

    /**
     * 渲染 HNCS 认证标识
     */
    private fun renderHasselbladHncs(
        canvas: Canvas,
        bitmap: Bitmap,
        exifData: ExifWatermarkData
    ) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        
        // 徽章位置 (左上角)
        val badgeWidth = width * 0.15f
        val badgeHeight = height * 0.08f
        val badgeLeft = width * 0.05f
        val badgeTop = height * 0.05f
        
        // 绘制徽章背景
        val bgPaint = Paint().apply {
            color = HasselbladColors.BackgroundSemiTransparent.toArgb()
            style = Paint.Style.FILL
        }
        val cornerRadius = badgeWidth * 0.1f
        val rectF = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
        
        // 绘制金色边框
        val borderPaint = Paint().apply {
            color = HasselbladColors.Gold.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
        
        // 绘制文字
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = HasselbladColors.Gold.toArgb()
            textSize = badgeHeight * 0.3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // 👑 HNCS
        canvas.drawText("HNCS", badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.4f, textPaint)
        
        // 哈苏自然色彩认证
        textPaint.apply {
            textSize = badgeHeight * 0.2f
            color = HasselbladColors.TextSecondary.toArgb()
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("哈苏自然", badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.65f, textPaint)
        canvas.drawText("色彩认证", badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.85f, textPaint)
    }

    /**
     * 渲染 XPAN 宽幅印记
     */
    private fun renderHasselbladXpan(
        canvas: Canvas,
        bitmap: Bitmap,
        exifData: ExifWatermarkData
    ) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        
        // 底部信息条高度
        val barHeight = height * 0.05f
        val barTop = height - barHeight
        
        // 绘制背景
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 180
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, barTop, width, height, bgPaint)
        
        // 文字画笔
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        
        // XPAN 65:24
        textPaint.apply {
            textSize = barHeight * 0.5f
            color = HasselbladColors.TextPrimary.toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        val leftMargin = width * 0.05f
        canvas.drawText("XPAN  65:24", leftMargin, barTop + barHeight * 0.7f, textPaint)
        
        // HASSELBLAD · 设备型号
        textPaint.apply {
            textAlign = Paint.Align.RIGHT
            textSize = barHeight * 0.35f
            color = HasselbladColors.TextSecondary.toArgb()
            typeface = Typeface.DEFAULT
            letterSpacing = 0f
        }
        val rightMargin = width * 0.95f
        val deviceInfo = "HASSELBLAD · ${exifData.getDeviceInfo()}"
        canvas.drawText(deviceInfo, rightMargin, barTop + barHeight * 0.65f, textPaint)
    }

    /**
     * 渲染通用水印
     */
    private fun renderGeneric(
        canvas: Canvas,
        bitmap: Bitmap,
        template: MasterWatermarkTemplate,
        exifData: ExifWatermarkData
    ) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        
        // 获取模板图层
        val layers = template.layers.sortedBy { it.sortOrder }
        
        // 计算位置
        val position = template.defaultPosition
        val (x, y, align) = getPositionCoords(position, width, height)
        
        // 文字画笔
        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = align
        }
        
        // 绘制每个图层
        var currentY = y
        layers.forEach { layer ->
            if (!layer.isEnabled) return@forEach
            
            val content = getLayerContent(layer, exifData)
            if (content.isEmpty()) return@forEach
            
            textPaint.apply {
                textSize = layer.defaultStyle.fontSize * (height / 1000f)
                color = Color.parseColor(layer.defaultStyle.color)
                alpha = (layer.defaultStyle.opacity * 255).toInt()
                letterSpacing = layer.defaultStyle.letterSpacing / 100f
                if (layer.defaultStyle.fontWeight >= 700) {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    typeface = Typeface.DEFAULT
                }
            }
            
            canvas.drawText(content, x, currentY, textPaint)
            currentY += textPaint.textSize * 1.5f
        }
    }

    /**
     * 获取图层内容
     */
    private fun getLayerContent(layer: WatermarkLayerDef, exifData: ExifWatermarkData): String {
        return when (layer.contentSource) {
            ContentSource.MANUAL -> layer.defaultContent
            ContentSource.EXIF -> when (layer.type) {
                WatermarkLayerType.PARAMS -> exifData.getParamsInfo()
                WatermarkLayerType.TIMESTAMP -> exifData.dateTaken ?: ""
                else -> layer.defaultContent
            }
            ContentSource.DEVICE_INFO -> exifData.getDeviceInfo()
            ContentSource.SYSTEM -> dateFormat.format(Date())
            ContentSource.GPS -> exifData.locationName ?: ""
        }
    }

    /**
     * 获取位置坐标
     */
    private fun getPositionCoords(
        position: WatermarkPosition,
        width: Float,
        height: Float
    ): Triple<Float, Float, Paint.Align> {
        val padding = width * 0.05f
        return when (position) {
            WatermarkPosition.TOP_LEFT -> Triple(padding, padding * 2, Paint.Align.LEFT)
            WatermarkPosition.TOP_CENTER -> Triple(width / 2, padding * 2, Paint.Align.CENTER)
            WatermarkPosition.TOP_RIGHT -> Triple(width - padding, padding * 2, Paint.Align.RIGHT)
            WatermarkPosition.CENTER_LEFT -> Triple(padding, height / 2, Paint.Align.LEFT)
            WatermarkPosition.CENTER -> Triple(width / 2, height / 2, Paint.Align.CENTER)
            WatermarkPosition.CENTER_RIGHT -> Triple(width - padding, height / 2, Paint.Align.RIGHT)
            WatermarkPosition.BOTTOM_LEFT -> Triple(padding, height - padding, Paint.Align.LEFT)
            WatermarkPosition.BOTTOM_CENTER -> Triple(width / 2, height - padding, Paint.Align.CENTER)
            WatermarkPosition.BOTTOM_RIGHT -> Triple(width - padding, height - padding, Paint.Align.RIGHT)
        }
    }

    /**
     * 加载 Bitmap
     */
    private fun loadBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open URI: $uri")
        return BitmapFactory.decodeStream(inputStream).also {
            inputStream.close()
        }
    }
}

/**
 * 导出格式
 */
enum class ExportFormat(val extension: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("jpg", Bitmap.CompressFormat.JPEG),
    PNG("png", Bitmap.CompressFormat.PNG),
    WEBP("webp", Bitmap.CompressFormat.WEBP)
}
