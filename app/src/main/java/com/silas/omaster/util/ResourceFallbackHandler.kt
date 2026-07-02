package com.silas.omaster.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * 启动资源容错处理器
 *
 * 覆盖场景：
 * - 5.3 启动资源加载容错：启动图/动画损坏时显示默认占位图
 * - 4.4 WebView环境异常：WebView不可用时降级为原生启动页
 * - 3.3 显示设置变更：确保字体/显示变更后UI不崩溃
 *
 * 设计原则：
 * - 所有资源加载失败不崩溃，降级显示默认占位
 * - 不依赖任何第三方库，避免循环依赖
 */
object ResourceFallbackHandler {

    private const val TAG = "ResourceFallback"
    private const val FALLBACK_BITMAP_SIZE = 256

    /**
     * 安全加载 Bitmap（带降级处理）
     * 启动资源损坏时返回默认占位图
     *
     * @param context 上下文
     * @param resourceId 资源ID（如 R.drawable.xxx）
     * @return Bitmap，加载失败时返回占位图
     */
    fun safeLoadBitmap(context: Context, resourceId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, resourceId)
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM 加载资源 $resourceId，降级为占位图", e)
            createFallbackBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "资源加载失败 $resourceId: ${e.message}，降级为占位图")
            createFallbackBitmap()
        }
    }

    /**
     * 安全加载文件中的 Bitmap
     */
    fun safeLoadBitmapFromFile(file: File): Bitmap? {
        return try {
            if (!file.exists() || !file.canRead()) {
                Log.w(TAG, "文件不存在或不可读: ${file.name}")
                return null
            }
            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(file)
                val opts = BitmapFactory.Options()
                opts.inPreferredConfig = Bitmap.Config.RGB_565 // 降内存占用
                // 低内存时降低采样率
                opts.inSampleSize = 2
                BitmapFactory.decodeStream(inputStream, null, opts)
            } finally {
                inputStream?.close()
            }
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM 加载文件: ${file.name}", e)
            createFallbackBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "文件加载失败: ${file.name}: ${e.message}")
            null
        }
    }

    /**
     * 安全加载 Drawable 资源ID
     * 返回 null 表示资源不存在，UI层应使用默认样式
     */
    fun safeLoadDrawableId(context: Context, resourceId: Int): Int? {
        return try {
            context.resources.getDrawable(resourceId, context.theme)
            resourceId // 加载成功返回原ID
        } catch (e: Exception) {
            Log.w(TAG, "Drawable 资源加载失败: $resourceId")
            null // 加载失败，UI层应检查null并降级
        }
    }

    /**
     * 创建默认占位 Bitmap
     * 白色背景 + 灰色文字提示
     */
    private fun createFallbackBitmap(): Bitmap {
        return try {
            val bitmap = Bitmap.createBitmap(
                FALLBACK_BITMAP_SIZE,
                FALLBACK_BITMAP_SIZE,
                Bitmap.Config.RGB_565
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                color = Color.parseColor("#CCCCCC")
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "?",
                FALLBACK_BITMAP_SIZE / 2f,
                FALLBACK_BITMAP_SIZE / 2f + paint.textSize / 3,
                paint
            )
            bitmap
        } catch (e: Throwable) {
            Log.e(TAG, "创建占位图失败", e)
            Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        }
    }

    /**
     * 检查 WebView 是否可用
     * 不可用时提醒UI层降级为原生显示
     */
    fun isWebViewAvailable(): Boolean {
        return try {
            Class.forName("android.webkit.WebView")
            // 尝试获取 WebView 的 Provider
            try {
                val factory = Class.forName("android.webkit.WebViewFactory")
                val provider = factory.getMethod("getProvider").invoke(null)
                provider != null
            } catch (_: Throwable) {
                // Provider 获取失败，但 WebView 类存在，可能仍可用
                true
            }
        } catch (_: ClassNotFoundException) {
            Log.w(TAG, "WebView 类不存在，降级为原生显示")
            false
        }
    }

    /**
     * 获取安全的字体缩放值
     * 防止用户设置极端字体大小导致UI溢出
     */
    fun getSafeFontScale(context: Context): Float {
        val fontScale = context.resources.configuration.fontScale
        return fontScale.coerceIn(0.8f, 1.5f)
    }

    /**
     * 获取安全的显示密度
     * 防止极端DPI导致布局异常
     */
    fun getSafeDisplayDensity(context: Context): Float {
        return context.resources.displayMetrics.density.coerceIn(0.75f, 4.0f)
    }
}