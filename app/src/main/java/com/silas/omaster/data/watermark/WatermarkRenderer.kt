package com.silas.omaster.data.watermark

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File

/**
 * 水印渲染器 - 纯函数式，不修改输入 Bitmap
 *
 * 支持三种水印类型：
 * - BRAND: 品牌文字水印，在指定位置渲染文字
 * - MASTER_MARK: 大师签名水印，渲染签名图片并支持透明度
 * - XPAN: 宽幅水印，在图片上下添加黑边条并叠加文字
 */
object WatermarkRenderer {

    /**
     * 渲染水印到 Bitmap 上
     * @param bitmap 原始图片（不会被修改）
     * @param config 水印配置
     * @return 带水印的新 Bitmap
     */
    fun renderWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
        return when (config.type) {
            WatermarkType.BRAND -> renderBrandWatermark(bitmap, config)
            WatermarkType.MASTER_MARK -> renderMasterMarkWatermark(bitmap, config)
            WatermarkType.XPAN -> renderXpanWatermark(bitmap, config, null)
        }
    }

    /**
     * 渲染组合水印 - 支持同时应用多种水印类型
     * 自动处理 XPAN + BRAND 的组合场景，品牌水印位置基于 XPAN 画布重新计算
     *
     * @param bitmap 原始图片（不会被修改）
     * @param configs 水印配置列表
     * @return 带水印的新 Bitmap
     */
    fun renderCombinedWatermark(bitmap: Bitmap, configs: List<WatermarkConfig>): Bitmap {
        if (configs.isEmpty()) return bitmap

        // 检测是否同时包含 XPAN 和 BRAND
        val xpanConfig = configs.find { it.type == WatermarkType.XPAN }
        val brandConfig = configs.find { it.type == WatermarkType.BRAND }

        var result = bitmap

        // XPAN + BRAND 组合：在 XPAN 画布上重新计算品牌水印位置
        if (xpanConfig != null && brandConfig != null) {
            result = renderXpanWatermark(result, xpanConfig, brandConfig)
            // 处理其余水印（排除已处理的 XPAN 和 BRAND）
            for (config in configs) {
                if (config.type == WatermarkType.MASTER_MARK) {
                    result = renderMasterMarkWatermark(result, config)
                }
            }
        } else {
            // 普通情况：依次叠加
            for (config in configs) {
                result = when (config.type) {
                    WatermarkType.BRAND -> renderBrandWatermark(result, config)
                    WatermarkType.MASTER_MARK -> renderMasterMarkWatermark(result, config)
                    WatermarkType.XPAN -> renderXpanWatermark(result, config, null)
                }
            }
        }

        return result
    }

    /**
     * 渲染品牌文字水印
     */
    private fun renderBrandWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = Paint().apply {
            color = config.color.toInt()
            alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
            textSize = config.fontSize.toFloat()
            typeface = resolveTypeface(config.fontName)
            isAntiAlias = true
            isFilterBitmap = true
        }

        val text = config.text.ifEmpty { return result }
        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val (x, y) = calculatePosition(
            canvasWidth = canvas.width,
            canvasHeight = canvas.height,
            textWidth = textWidth,
            textHeight = textHeight,
            fontMetrics = fontMetrics,
            position = config.position,
            padding = (config.fontSize * 0.8f)
        )

        // 绘制文字阴影以增强可读性
        val shadowPaint = Paint(paint).apply {
            color = Color.BLACK
            alpha = (config.opacity * 128).toInt().coerceIn(0, 255)
        }
        canvas.drawText(text, x + 1f, y + 1f, shadowPaint)
        canvas.drawText(text, x, y, paint)

        return result
    }

    /**
     * 渲染大师签名水印
     */
    private fun renderMasterMarkWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val signaturePath = config.signatureImagePath
        if (signaturePath.isEmpty()) return result

        val signatureFile = File(signaturePath)
        if (!signatureFile.exists()) return result

        val signatureBitmap = BitmapFactory.decodeFile(signaturePath) ?: return result

        // 计算签名大小：不超过原图宽高的 1/4
        val maxWidth = bitmap.width / 4f
        val maxHeight = bitmap.height / 4f
        val scale = minOf(
            maxWidth / signatureBitmap.width,
            maxHeight / signatureBitmap.height
        ).coerceAtMost(1f)

        val scaledWidth = (signatureBitmap.width * scale).toInt()
        val scaledHeight = (signatureBitmap.height * scale).toInt()

        val scaledSignature = Bitmap.createScaledBitmap(signatureBitmap, scaledWidth, scaledHeight, true)

        val paint = Paint().apply {
            alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
            isAntiAlias = true
            isFilterBitmap = true
        }

        val (x, y) = calculateImagePosition(
            canvasWidth = canvas.width,
            canvasHeight = canvas.height,
            imageWidth = scaledWidth,
            imageHeight = scaledHeight,
            position = config.position,
            padding = (bitmap.width * 0.03f)
        )

        canvas.drawBitmap(scaledSignature, x, y, paint)

        if (scaledSignature !== signatureBitmap) scaledSignature.recycle()
        signatureBitmap.recycle()

        return result
    }

    /**
     * 渲染 XPAN 宽幅水印
     * 在图片上下添加黑边条，保持宽幅比例（65:24），并在底部黑边上叠加文字
     * 如果同时启用了品牌水印，则品牌水印位置会基于 XPAN 画布尺寸重新计算
     *
     * @param bitmap 原始图片（不会被修改）
     * @param config XPAN 水印配置
     * @param brandConfig 可选的品牌水印配置，同时启用时在 XPAN 画布上重新计算位置
     */
    private fun renderXpanWatermark(bitmap: Bitmap, config: WatermarkConfig, brandConfig: WatermarkConfig?): Bitmap {
        val barRatio = config.xpanBarRatio.coerceIn(0.02f, 0.3f)
        val barHeight = (bitmap.height * barRatio).toInt()

        // XPAN 宽幅比例目标：65:24 ≈ 2.7083（使用配置中的 xpanRatio）
        val targetRatio = config.xpanRatio
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        // 计算目标宽度（保持高度不变，调整宽度到宽幅比例）
        val totalHeight = bitmap.height + barHeight * 2
        val targetWidth = (totalHeight * targetRatio).toInt()
        val finalWidth = maxOf(targetWidth, bitmap.width)

        val result = Bitmap.createBitmap(finalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 填充黑色背景
        canvas.drawColor(Color.BLACK)

        // 居中绘制原图
        val offsetX = (finalWidth - bitmap.width) / 2f
        canvas.drawBitmap(bitmap, offsetX, barHeight.toFloat(), null)

        // 底部黑边上绘制 XPAN 文字
        val text = config.xpanText.ifEmpty { "XPAN" }
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
            textSize = barHeight * 0.5f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            letterSpacing = 0.15f
        }

        val textWidth = paint.measureText(text)
        val textX = (finalWidth - textWidth) / 2f
        val textY = barHeight + bitmap.height + barHeight / 2f - (paint.descent() + paint.ascent()) / 2f

        canvas.drawText(text, textX, textY, paint)

        // 如果同时存在品牌水印配置，基于 XPAN 画布尺寸重新计算并渲染品牌水印
        if (brandConfig != null) {
            val brandText = brandConfig.text.ifEmpty { return result }
            val brandPaint = Paint().apply {
                color = brandConfig.color.toInt()
                alpha = (brandConfig.opacity * 255).toInt().coerceIn(0, 255)
                textSize = brandConfig.fontSize.toFloat()
                typeface = resolveTypeface(brandConfig.fontName)
                isAntiAlias = true
                isFilterBitmap = true
            }

            val brandTextWidth = brandPaint.measureText(brandText)
            val brandFontMetrics = brandPaint.fontMetrics
            val brandTextHeight = brandFontMetrics.descent - brandFontMetrics.ascent

            // 基于 XPAN 画布尺寸（finalWidth x totalHeight，包含黑边）重新计算品牌水印位置
            val (bx, by) = calculatePosition(
                canvasWidth = finalWidth,
                canvasHeight = totalHeight,
                textWidth = brandTextWidth,
                textHeight = brandTextHeight,
                fontMetrics = brandFontMetrics,
                position = brandConfig.position,
                padding = (brandConfig.fontSize * 0.8f)
            )

            val brandShadowPaint = Paint(brandPaint).apply {
                color = Color.BLACK
                alpha = (brandConfig.opacity * 128).toInt().coerceIn(0, 255)
            }
            canvas.drawText(brandText, bx + 1f, by + 1f, brandShadowPaint)
            canvas.drawText(brandText, bx, by, brandPaint)
        }

        return result
    }

    /**
     * 计算文字水印位置
     */
    private fun calculatePosition(
        canvasWidth: Int,
        canvasHeight: Int,
        textWidth: Float,
        textHeight: Float,
        fontMetrics: Paint.FontMetrics,
        position: WatermarkPosition,
        padding: Float
    ): Pair<Float, Float> {
        return when (position) {
            WatermarkPosition.BOTTOM_LEFT -> {
                padding to (canvasHeight - padding - fontMetrics.descent)
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                (canvasWidth - textWidth - padding) to (canvasHeight - padding - fontMetrics.descent)
            }
            WatermarkPosition.CENTER -> {
                (canvasWidth - textWidth) / 2f to (canvasHeight - (fontMetrics.descent + fontMetrics.ascent)) / 2f - fontMetrics.ascent
            }
        }
    }

    /**
     * 计算图片水印位置
     */
    private fun calculateImagePosition(
        canvasWidth: Int,
        canvasHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        position: WatermarkPosition,
        padding: Float
    ): Pair<Float, Float> {
        return when (position) {
            WatermarkPosition.BOTTOM_LEFT -> {
                padding to (canvasHeight - imageHeight - padding)
            }
            WatermarkPosition.BOTTOM_RIGHT -> {
                (canvasWidth - imageWidth - padding) to (canvasHeight - imageHeight - padding)
            }
            WatermarkPosition.CENTER -> {
                (canvasWidth - imageWidth) / 2f to (canvasHeight - imageHeight) / 2f
            }
        }
    }

    /**
     * 根据字体名称获取 Typeface
     */
    private fun resolveTypeface(fontName: String): Typeface {
        return when (fontName.lowercase()) {
            "monospace" -> Typeface.MONOSPACE
            "serif" -> Typeface.SERIF
            "sans_serif", "default" -> Typeface.DEFAULT
            else -> Typeface.DEFAULT
        }
    }
}
