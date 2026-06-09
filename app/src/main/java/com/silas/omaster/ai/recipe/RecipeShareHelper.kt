package com.silas.omaster.ai.recipe

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 配方分享工具
 * 支持二维码生成、链接分享、文本分享
 */
object RecipeShareHelper {

    /**
     * 生成配方二维码
     *
     * @param shareCode 配方分享码
     * @param size 二维码尺寸（像素）
     * @return 二维码Bitmap
     */
    fun generateQRCode(shareCode: String, size: Int = 300): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(shareCode, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    // 使用哈苏橙作为二维码颜色
                    pixels[y * width + x] = if (bitMatrix[x, y]) {
                        // 哈苏橙 #FF6B35
                        0xFFFF6B35.toInt()
                    } else {
                        // 白色背景
                        Color.WHITE
                    }
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成带品牌水印的二维码
     */
    fun generateBrandedQRCode(shareCode: String, size: Int = 400): Bitmap? {
        val baseQRCode = generateQRCode(shareCode, size) ?: return null

        // 创建带边框的二维码
        val borderSize = 20
        val totalSize = size + borderSize * 2
        val brandedBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)

        // 填充黑色背景
        brandedBitmap.eraseColor(0xFF0A0A0A.toInt())

        // 绘制二维码
        val canvas = android.graphics.Canvas(brandedBitmap)
        canvas.drawBitmap(baseQRCode, borderSize.toFloat(), borderSize.toFloat(), null)

        // 添加品牌水印文字
        val paint = android.graphics.Paint()
        paint.color = 0xFFFF6B35.toInt()
        paint.textSize = 24f
        paint.isAntiAlias = true
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("HNCS · OMaster", totalSize / 2f, totalSize - 10f, paint)

        return brandedBitmap
    }

    /**
     * 生成分享链接
     * 格式: omaster://recipe?code={shareCode}
     */
    fun generateShareLink(shareCode: String): String {
        return "omaster://recipe?code=$shareCode"
    }

    /**
     * 生成分享文本
     */
    fun generateShareText(recipe: RecipeProfile): String {
        val lines = mutableListOf<String>()
        lines.add("【哈苏大师配方】${recipe.name}")
        lines.add("")
        lines.add("场景: ${recipe.scene.icon} ${recipe.scene.displayName}")
        lines.add("胶片: ${recipe.film.displayName} (${recipe.film.matchPercent}%匹配)")
        lines.add("")
        lines.add("哈苏参数:")
        recipe.hasselbladParams.formatDisplay().forEach { (label, value) ->
            lines.add("  $label: $value")
        }
        lines.add("")
        if (recipe.masterTips.isNotEmpty()) {
            lines.add("大师建议:")
            recipe.masterTips.take(3).forEach { tip ->
                lines.add("  💡 $tip")
            }
        }
        lines.add("")
        lines.add("配方代码: ${recipe.toShareCode().take(50)}...")
        lines.add("")
        lines.add("使用 OMaster App 导入此配方")

        return lines.joinToString("\n")
    }

    /**
     * 从分享链接解析配方代码
     */
    fun parseShareLink(link: String): String? {
        if (!link.startsWith("omaster://recipe?code=")) return null
        return link.removePrefix("omaster://recipe?code=")
    }

    /**
     * 验证分享码格式
     */
    fun validateShareCode(code: String): Boolean {
        return try {
            RecipeProfile.fromShareCode(code) != null
        } catch (e: Exception) {
            false
        }
    }
}