package com.silas.omaster.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 内存优化管理器
 *
 * 功能：
 * 1. 监控内存使用情况
 * 2. 内存压力时自动释放缓存
 * 3. Bitmap 复用池管理
 * 4. 大对象加载策略建议
 */
object MemoryOptimizer {

    private const val TAG = "MemoryOptimizer"

    // 内存压力阈值（可用内存低于此值时触发清理）
    private const val LOW_MEMORY_THRESHOLD_MB = 50

    // Bitmap 复用池配置
    private val bitmapPool = mutableMapOf<String, MutableList<Bitmap>>()
    private const val MAX_POOL_SIZE_PER_KEY = 3

    /**
     * 获取内存信息
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemMB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            memoryInfo.totalMem / (1024 * 1024)
        } else {
            // 低版本使用 Runtime 估算
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        }

        val availMemMB = memoryInfo.availMem / (1024 * 1024)
        val usedMemMB = totalMemMB - availMemMB
        val usedPercent = (usedMemMB.toFloat() / totalMemMB * 100).toInt()

        return MemoryInfo(
            totalMemoryMB = totalMemMB,
            availableMemoryMB = availMemMB,
            usedMemoryMB = usedMemMB,
            usedPercent = usedPercent,
            isLowMemory = memoryInfo.lowMemory,
            thresholdMB = memoryInfo.threshold / (1024 * 1024)
        )
    }

    /**
     * 检查是否需要释放内存
     */
    fun shouldReleaseMemory(context: Context): Boolean {
        val info = getMemoryInfo(context)
        return info.isLowMemory || info.availableMemoryMB < LOW_MEMORY_THRESHOLD_MB
    }

    /**
     * 执行内存清理
     */
    suspend fun performMemoryCleanup(context: Context): CleanupResult = withContext(Dispatchers.IO) {
        val beforeInfo = getMemoryInfo(context)
        val cleanedItems = mutableListOf<String>()

        // 1. 清理图片缓存
        try {
            CoilConfig.clearCache(context)
            cleanedItems.add("图片缓存")
        } catch (e: Exception) {
            ReleaseLog.e(TAG, "清理图片缓存失败", e)
        }

        // 2. 清理 Bitmap 池
        val bitmapCount = bitmapPool.values.sumOf { it.size }
        bitmapPool.values.forEach { list ->
            list.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
        bitmapPool.clear()
        if (bitmapCount > 0) {
            cleanedItems.add("Bitmap池($bitmapCount)")
        }

        // 3. 触发 GC
        System.gc()

        val afterInfo = getMemoryInfo(context)
        val freedMB = afterInfo.availableMemoryMB - beforeInfo.availableMemoryMB

        ReleaseLog.d(TAG, "内存清理完成: 释放 ${freedMB}MB, 清理项: ${cleanedItems.joinToString()}")

        CleanupResult(
            beforeMemoryMB = beforeInfo.usedMemoryMB,
            afterMemoryMB = afterInfo.usedMemoryMB,
            freedMemoryMB = freedMB,
            cleanedItems = cleanedItems
        )
    }

    /**
     * 从复用池获取 Bitmap
     */
    fun getBitmapFromPool(key: String, width: Int, height: Int): Bitmap? {
        val pool = bitmapPool[key] ?: return null
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next()
            if (bitmap.width == width && bitmap.height == height && !bitmap.isRecycled) {
                iterator.remove()
                return bitmap
            }
        }
        return null
    }

    /**
     * 将 Bitmap 放入复用池
     */
    fun putBitmapToPool(key: String, bitmap: Bitmap) {
        val pool = bitmapPool.getOrPut(key) { mutableListOf() }
        if (pool.size < MAX_POOL_SIZE_PER_KEY && !bitmap.isRecycled) {
            pool.add(bitmap)
        } else if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    /**
     * 计算合适的 Bitmap 采样率
     * 用于加载大图时避免 OOM
     */
    fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (width, height) = options.outWidth to options.outHeight
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 获取推荐的 Bitmap 配置
     * 根据内存情况选择 ARGB_8888 或 RGB_565
     */
    fun getRecommendedBitmapConfig(context: Context): Bitmap.Config {
        val info = getMemoryInfo(context)
        // 内存充足时使用高质量配置，否则使用节省内存的配置
        return if (info.usedPercent < 70) {
            Bitmap.Config.ARGB_8888
        } else {
            Bitmap.Config.RGB_565
        }
    }

    // 数据类
    data class MemoryInfo(
        val totalMemoryMB: Long,
        val availableMemoryMB: Long,
        val usedMemoryMB: Long,
        val usedPercent: Int,
        val isLowMemory: Boolean,
        val thresholdMB: Long
    )

    data class CleanupResult(
        val beforeMemoryMB: Long,
        val afterMemoryMB: Long,
        val freedMemoryMB: Long,
        val cleanedItems: List<String>
    )
}
