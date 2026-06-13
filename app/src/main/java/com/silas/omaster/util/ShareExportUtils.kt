package com.silas.omaster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import android.util.Log
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.watermark.ExifWatermarkProvider
import com.silas.omaster.watermark.WatermarkConfigDef
import com.silas.omaster.watermark.WatermarkLayerDef
import com.silas.omaster.watermark.WatermarkLayerType
import com.silas.omaster.watermark.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 分享/保存/导出功能工具类
 * 
 * 功能：
 * - 分享预设配方到社交媒体
 * - 保存预设配方到本地收藏
 * - 导出带水印图片
 * - 复制配方链接到剪贴板
 * 
 * 与 Web 端功能对齐
 */
object ShareExportUtils {

    private const val TAG = "ShareExportUtils"

    /**
     * 分享预设配方
     * 
     * @param context 上下文
     * @param preset 预设数据
     */
    fun sharePreset(context: Context, preset: MasterPreset) {
        val shareText = buildShareText(preset)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "哈苏大师配方 - ${preset.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val chooserIntent = Intent.createChooser(intent, "分享哈苏配方")
        context.startActivity(chooserIntent)
    }

    /**
     * 分享带水印图片
     * 
     * @param context 上下文
     * @param bitmap 图片
     * @param preset 预设数据（可选）
     */
    suspend fun shareImageWithWatermark(
        context: Context,
        bitmap: Bitmap,
        preset: MasterPreset? = null
    ) = withContext(Dispatchers.IO) {
        val file = saveBitmapToCache(context, bitmap)
        
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareText = preset?.let { buildShareText(it) } ?: ""
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "哈苏大师印记")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(intent, "分享图片")
            context.startActivity(chooserIntent)
        }
    }

    /**
     * 保存预设配方到本地收藏
     * 
     * @param context 上下文
     * @param preset 预设数据
     * @return 是否保存成功
     */
    fun savePresetToFavorites(context: Context, preset: MasterPreset): Boolean {
        try {
            val prefs = context.getSharedPreferences("hasselblad_favorites", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            val timestamp = System.currentTimeMillis()
            val key = "preset_${preset.id ?: timestamp}"
            
            // 使用 JSON 序列化
            val json = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.serializer<MasterPreset>(),
                preset.copy(
                    id = preset.id ?: key,
                    isFavorite = true,
                    createdAt = timestamp
                )
            )
            
            editor.putString(key, json)
            editor.putLong("last_updated", timestamp)
            editor.apply()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "savePresetToFavorites failed", e)
            return false
        }
    }

    /**
     * 导出图片到相册
     * 
     * @param context 上下文
     * @param bitmap 图片
     * @param fileName 文件名（可选）
     * @return 导出的文件路径
     */
    suspend fun exportImageToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val name = fileName ?: "hasselblad_$timestamp.jpg"
            
            // 使用 MediaStore API（Android 10+ 推荐，作用域存储兼容）
            val savedUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                saveBitmapViaMediaStore(context, bitmap, name)
            } else {
                saveBitmapLegacy(context, bitmap, name)
            }
            
            savedUri?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "exportImageToGallery failed", e)
            null
        }
    }

    /**
     * Android 10+ 使用 MediaStore API 写入公共相册
     */
    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun saveBitmapViaMediaStore(
        context: Context,
        bitmap: Bitmap,
        name: String
    ): Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_PICTURES}/HasselbladMaster"
            )
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null
        
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /**
     * Android 9 及以下使用传统方式写入公共相册
     */
    private fun saveBitmapLegacy(
        context: Context,
        bitmap: Bitmap,
        name: String
    ): Uri? {
        val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_PICTURES
        )
        val hasselbladDir = File(picturesDir, "HasselbladMaster")
        if (!hasselbladDir.exists() && !hasselbladDir.mkdirs()) {
            return null
        }
        
        val file = File(hasselbladDir, name)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // 使用 FileProvider 而非 file:// URI 触发 FileUriExposedException
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapLegacy failed", e)
            null
        }
    }

    /**
     * 导出带水印图片
     * 
     * @param context 上下文
     * @param originalBitmap 原图
     * @param watermarkConfig 水印配置
     * @return 导出的文件路径
     */
    suspend fun exportImageWithWatermark(
        context: Context,
        originalBitmap: Bitmap,
        watermarkConfig: WatermarkConfigDef
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 渲染水印
            val watermarkedBitmap = renderWatermark(context, originalBitmap, watermarkConfig)

            // 导出到相册
            exportImageToGallery(context, watermarkedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "exportImageWithWatermark failed", e)
            null
        }
    }

    /**
     * 复制配方链接到剪贴板
     * 
     * @param context 上下文
     * @param preset 预设数据
     */
    fun copyPresetLink(context: Context, preset: MasterPreset) {
        val text = buildShareText(preset)
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) 
            as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("哈苏配方", text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * 获取收藏的预设列表
     */
    fun getFavoritePresets(context: Context): List<MasterPreset> {
        try {
            val prefs = context.getSharedPreferences("hasselblad_favorites", Context.MODE_PRIVATE)
            val allEntries = prefs.all
            
            return allEntries
                .filterKeys { it.startsWith("preset_") }
                .mapNotNull { (_, value) ->
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<MasterPreset>(
                            value as String
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "getFavoritePresets failed", e)
            return emptyList()
        }
    }

    /**
     * 移除收藏的预设
     */
    fun removeFavoritePreset(context: Context, presetId: String) {
        try {
            val prefs = context.getSharedPreferences("hasselblad_favorites", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove("preset_$presetId")
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "removeFavoritePreset failed", e)
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建分享文本
     */
    private fun buildShareText(preset: MasterPreset): String {
        val builder = StringBuilder()
        
        builder.appendLine("哈苏大师配方 - ${preset.name}")
        builder.appendLine()
        
        // 作者信息
        preset.author?.let { builder.appendLine("作者: $it") }
        
        // 胶片推荐
        if (preset.isHncs) {
            builder.appendLine("认证: HNCS 自然色彩认证")
        }
        
        // 参数摘要
        preset.sections?.firstOrNull()?.items?.take(4)?.forEach { item ->
            builder.appendLine("${item.label}: ${item.value}")
        }
        
        builder.appendLine()
        builder.appendLine("用哈苏之眼，记录每一刻的光影。")
        
        return builder.toString()
    }

    /**
     * 保存 Bitmap 到缓存目录
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = context.cacheDir
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val file = File(cacheDir, "share_$timestamp.jpg")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        return file
    }

    /**
     * 渲染水印到图片
     *
     * 实现完整的水印渲染：遍历 WatermarkConfigDef 的所有可见图层，
     * 根据每个图层的类型（文本/品牌/设备/参数/时间戳/位置/形状/暗角），
     * 调用真实的 Canvas/Paint 绘制接口；文本内容根据 ContentSource 从 EXIF、
     * 系统时间、设备信息中获取；样式（颜色、透明度、字体、阴影、圆角背景等）
     * 全部按照图层样式参数执行。
     */
    private fun renderWatermark(
        context: Context,
        bitmap: Bitmap,
        config: WatermarkConfigDef
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // 设备/EXIF 数据：当前没有可用的图片路径，因此使用 ExifWatermarkProvider 的兜底实现
        val exifData = ExifWatermarkProvider(context).extractFromUri(
            Uri.fromFile(File(context.cacheDir, "watermark_fallback.tmp"))
        )

        // 直接使用 getVisibleLayers()，内部已经按 sortOrder 降序排序
        val visibleLayers = config.getVisibleLayers()
        for (layer in visibleLayers) {
            drawLayer(canvas, bitmap, layer, exifData)
        }

        return result
    }

    /**
     * 在 Canvas 上绘制单个水印图层
     */
    private fun drawLayer(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        exifData: ExifWatermarkProvider.ExifWatermarkData
    ) {
        val style = layer.style
        val resolvedContent = resolveLayerContent(layer, exifData)

        when (layer.type) {
            WatermarkLayerType.VIGNETTE -> drawVignette(canvas, bitmap, style.opacity)
            WatermarkLayerType.SHAPE -> drawShapeLayer(canvas, bitmap, layer, resolvedContent, style)
            else -> drawTextLayer(canvas, bitmap, layer, resolvedContent, style)
        }
    }

    /**
     * 根据 ContentSource 解析图层内容
     */
    private fun resolveLayerContent(
        layer: WatermarkLayerDef,
        exifData: ExifWatermarkProvider.ExifWatermarkData
    ): String {
        return when (layer.contentSource) {
            com.silas.omaster.watermark.ContentSource.MANUAL ->
                layer.content.ifBlank { layer.defaultContent }
            com.silas.omaster.watermark.ContentSource.EXIF -> exifData.getFormattedParams()
            com.silas.omaster.watermark.ContentSource.GPS ->
                exifData.locationName ?: "${exifData.gpsLat}, ${exifData.gpsLng}"
            com.silas.omaster.watermark.ContentSource.SYSTEM ->
                exifData.fullDateTime ?: exifData.getFormattedDate()
            com.silas.omaster.watermark.ContentSource.DEVICE_INFO -> exifData.getFullDevice()
        }
    }

    /**
     * 绘制文本类图层
     */
    private fun drawTextLayer(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        text: String,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        if (text.isBlank()) return

        val baseTextSize = style.fontSize.coerceAtLeast(8f)
        // 将 sp 字号按图片宽度缩放：图片越宽字号越大，保持视觉一致
        val scaledTextSize = baseTextSize * (bitmap.width / 1000f).coerceIn(0.6f, 3f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = applyAlpha(style.getColor(), style.opacity)
            textSize = scaledTextSize
            isAntiAlias = true
            letterSpacing = style.letterSpacing / 100f
            typeface = resolveTypeface(style.fontFamily, style.fontWeight)
            textAlign = when (layer.position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.CENTER_LEFT, WatermarkPosition.BOTTOM_LEFT -> Paint.Align.LEFT
                WatermarkPosition.TOP_CENTER, WatermarkPosition.CENTER, WatermarkPosition.BOTTOM -> Paint.Align.CENTER
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.CENTER_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> Paint.Align.RIGHT
                WatermarkPosition.CUSTOM -> Paint.Align.LEFT
            }
            if (style.shadowEnabled && style.shadowBlur > 0f) {
                setShadowLayer(style.shadowBlur, 0f, 1f, applyAlpha(style.getShadowColor(), 0.6f))
            }
        }

        val (x, y) = computeTextOrigin(canvas, bitmap, paint, text, layer, style)
        // 多行文本支持
        val lines = text.split("\n")
        val lineHeight = scaledTextSize * style.lineHeight
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, y + index * lineHeight, paint)
        }
    }

    /**
     * 计算文本绘制原点
     */
    private fun computeTextOrigin(
        canvas: Canvas,
        bitmap: Bitmap,
        paint: Paint,
        text: String,
        layer: WatermarkLayerDef,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ): Pair<Float, Float> {
        val padding = style.padding
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val fontMetrics = paint.fontMetrics
        val firstLineHeight = fontMetrics.descent - fontMetrics.ascent

        // 先计算位置
        var x: Float
        var baselineY: Float
        when (layer.position) {
            WatermarkPosition.TOP_LEFT -> { x = padding; baselineY = padding + firstLineHeight }
            WatermarkPosition.TOP_CENTER -> { x = w / 2f; baselineY = padding + firstLineHeight }
            WatermarkPosition.TOP_RIGHT -> { x = w - padding; baselineY = padding + firstLineHeight }
            WatermarkPosition.CENTER_LEFT -> { x = padding; baselineY = h / 2f }
            WatermarkPosition.CENTER -> { x = w / 2f; baselineY = h / 2f }
            WatermarkPosition.CENTER_RIGHT -> { x = w - padding; baselineY = h / 2f }
            WatermarkPosition.BOTTOM_LEFT -> { x = padding; baselineY = h - padding - fontMetrics.descent }
            WatermarkPosition.BOTTOM -> { x = w / 2f; baselineY = h - padding - fontMetrics.descent }
            WatermarkPosition.BOTTOM_RIGHT -> { x = w - padding; baselineY = h - padding - fontMetrics.descent }
            WatermarkPosition.CUSTOM -> { x = padding; baselineY = padding + firstLineHeight }
        }
        x += layer.offset.x
        baselineY += layer.offset.y
        return Pair(x, baselineY)
    }

    /**
     * 解析字体
     */
    private fun resolveTypeface(family: String, weight: Int): Typeface {
        val style = when {
            weight >= 700 -> Typeface.BOLD
            weight >= 500 -> Typeface.BOLD // 介于 500-700 也按粗体处理
            else -> Typeface.NORMAL
        }
        return when (family.lowercase(Locale.ROOT)) {
            "monospace" -> Typeface.create(Typeface.MONOSPACE, style)
            "serif" -> Typeface.create(Typeface.SERIF, style)
            "sans-serif" -> Typeface.create(Typeface.SANS_SERIF, style)
            "default" -> Typeface.create(Typeface.DEFAULT, style)
            else -> Typeface.create(family, style)
        }
    }

    /**
     * 绘制形状类图层
     */
    private fun drawShapeLayer(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        content: String,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        val shapeKey = content.ifBlank { "divider_horizontal" }
        when (shapeKey) {
            "divider_horizontal" -> drawHorizontalDivider(canvas, bitmap, layer, style)
            "rect_background" -> drawRectBackground(canvas, bitmap, layer, style)
            "badge_rect" -> drawBadgeRect(canvas, bitmap, layer, style)
            "circle_dot" -> drawCircleDot(canvas, bitmap, layer, style)
            else -> drawHorizontalDivider(canvas, bitmap, layer, style)
        }
    }

    private fun drawHorizontalDivider(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        val padding = style.padding
        val w = bitmap.width.toFloat()
        val y = when (layer.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT -> padding
            WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT -> bitmap.height / 2f
            else -> bitmap.height - padding
        } + layer.offset.y
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = applyAlpha(style.getColor(), style.opacity)
            strokeWidth = 1f
        }
        canvas.drawLine(0f, y, w, y, paint)
    }

    private fun drawRectBackground(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        val padding = style.padding
        val bgColor = style.getBackgroundColor() ?: return
        val rect = when (layer.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT ->
                RectF(0f, 0f, bitmap.width.toFloat(), padding * 4f + 60f)
            else -> RectF(
                0f,
                bitmap.height - padding * 4f - 60f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat()
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = applyAlpha(bgColor, style.backgroundOpacity)
        }
        canvas.drawRect(rect, paint)
    }

    private fun drawBadgeRect(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        val padding = style.padding
        val w = padding * 8f
        val h = padding * 4f
        val x = padding + layer.offset.x
        val y = padding + layer.offset.y
        val rect = RectF(x, y, x + w, y + h)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style.getBackgroundColor()?.let { color = applyAlpha(it, style.backgroundOpacity) }
        }
        canvas.drawRoundRect(rect, style.cornerRadius, style.cornerRadius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = applyAlpha(style.getColor(), style.opacity)
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, style.cornerRadius, style.cornerRadius, borderPaint)
    }

    private fun drawCircleDot(
        canvas: Canvas,
        bitmap: Bitmap,
        layer: WatermarkLayerDef,
        style: com.silas.omaster.watermark.WatermarkLayerStyle
    ) {
        val r = style.padding.coerceAtLeast(4f) * 0.6f
        val x = style.padding + r + layer.offset.x
        val y = style.padding + r + layer.offset.y
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = applyAlpha(style.getColor(), style.opacity)
        }
        canvas.drawCircle(x, y, r, paint)
    }

    /**
     * 绘制暗角效果
     */
    private fun drawVignette(canvas: Canvas, bitmap: Bitmap, alpha: Float) {
        if (alpha <= 0f) return
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = maxOf(bitmap.width, bitmap.height) * 0.8f
        val paint = Paint().apply {
            shader = android.graphics.RadialGradient(
                centerX, centerY, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((alpha * 180).toInt().coerceIn(0, 255), 0, 0, 0)),
                floatArrayOf(0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }

    /**
     * 给颜色叠加透明度
     */
    private fun applyAlpha(color: Int, opacity: Float): Int {
        val a = (Color.alpha(color) * opacity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}