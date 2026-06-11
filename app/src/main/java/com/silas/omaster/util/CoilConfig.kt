package com.silas.omaster.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.silas.omaster.BuildConfig
import okio.Path.Companion.toOkioPath

/**
 * Coil 图片加载优化配置
 *
 * 优化策略：
 * 1. 内存缓存：启用并设置合理大小（可用内存的 1/4）
 * 2. 磁盘缓存：启用并设置 100MB 上限
 * 3. 图片解码：使用 RGB_565 减少内存占用（非透明图片）
 * 4. 请求优化：启用交叉淡入、禁用硬件位图（部分场景）
 */
object CoilConfig {

    /**
     * 创建优化的 ImageLoader
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // 内存缓存配置
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 使用可用内存的 25%
                    .build()
            }
            // 磁盘缓存配置
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB
                    .build()
            }
            // 网络缓存策略
            .networkCachePolicy(CachePolicy.ENABLED)
            // 交叉淡入动画
            .crossfade(true)
            .crossfade(300) // 300ms 淡入
            // 错误处理
            .error(android.R.drawable.ic_menu_report_image)
            .placeholder(android.R.drawable.ic_menu_gallery)
            // 调试日志（仅 Debug 模式）
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    /**
     * 获取缓存大小信息
     */
    fun getCacheInfo(context: Context): CacheInfo {
        val cacheDir = context.cacheDir.resolve("image_cache")
        val diskCacheSize = if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L

        return CacheInfo(
            diskCacheBytes = diskCacheSize,
            diskCacheMB = diskCacheSize / (1024.0 * 1024.0)
        )
    }

    /**
     * 清除图片缓存
     */
    fun clearCache(context: Context) {
        // 清除磁盘缓存
        context.cacheDir.resolve("image_cache").deleteRecursively()
        ReleaseLog.d("CoilConfig", "Image cache cleared")
    }

    data class CacheInfo(
        val diskCacheBytes: Long,
        val diskCacheMB: Double
    )
}
