package com.silas.omaster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.watermark.WatermarkConfigDef
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
            e.printStackTrace()
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
            
            // 保存到公共相册目录
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val hasselbladDir = File(picturesDir, "HasselbladMaster")
            if (!hasselbladDir.exists()) {
                hasselbladDir.mkdirs()
            }
            
            val file = File(hasselbladDir, name)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // 通知媒体库更新
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            context.sendBroadcast(intent)
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
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
            val watermarkedBitmap = renderWatermark(originalBitmap, watermarkConfig)
            
            // 导出到相册
            exportImageToGallery(context, watermarkedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
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
            e.printStackTrace()
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
     */
    private fun renderWatermark(bitmap: Bitmap, config: WatermarkConfigDef): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // TODO: 实现完整的水印渲染逻辑
        // 当前为简化版本
        
        return result
    }
}