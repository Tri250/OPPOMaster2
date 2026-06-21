package com.silas.omaster.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 性能优化工具类 - 2026年最高标准
 *
 * 功能：
 * - 内存监控与优化
 * - Bitmap内存管理
 * - 协程作用域管理
 * - 性能统计与分析
 * - ANR预防
 */
object PerformanceHelper {

    private const val TAG = "PerformanceHelper"
    
    // 内存警告阈值（MB）
    private const val MEMORY_WARNING_THRESHOLD_MB = 100L
    private const val MEMORY_CRITICAL_THRESHOLD_MB = 50L
    
    // Bitmap缓存大小限制（MB）
    private const val BITMAP_CACHE_MAX_SIZE_MB = 50L
    
    // 协程作用域管理
    private val scopeMap = ConcurrentHashMap<String, WeakReference<CoroutineScope>>()
    
    // 性能统计
    private val operationTimes = ConcurrentHashMap<String, AtomicLong>()
    private val operationCounts = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 检查内存状态
     * @param context Context
     * @return 内存状态（OK/WARNING/CRITICAL）
     */
    fun checkMemoryStatus(context: Context): MemoryStatus {
        val availableMB = getAvailableMemoryMB(context)
        return when {
            availableMB > MEMORY_WARNING_THRESHOLD_MB -> MemoryStatus.OK
            availableMB > MEMORY_CRITICAL_THRESHOLD_MB -> MemoryStatus.WARNING
            else -> MemoryStatus.CRITICAL
        }
    }

    /**
     * 获取可用内存（MB）
     */
    fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * 获取应用内存使用（MB）
     */
    fun getAppMemoryUsageMB(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.getTotalPss() / 1024L
    }

    /**
     * 获取Native内存使用（MB）
     */
    fun getNativeMemoryUsageMB(): Long {
        return Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
    }

    /**
     * 内存优化建议
     * @param context Context
     * @return 优化建议列表
     */
    fun getMemoryOptimizationSuggestions(context: Context): List<String> {
        val suggestions = mutableListOf<String>()
        val status = checkMemoryStatus(context)
        
        if (status == MemoryStatus.WARNING || status == MemoryStatus.CRITICAL) {
            suggestions.add("内存紧张，建议清理缓存")
            suggestions.add("避免加载大图，使用缩略图")
            suggestions.add("及时释放不再使用的Bitmap")
            suggestions.add("减少后台任务数量")
        }
        
        val appMemory = getAppMemoryUsageMB()
        if (appMemory > 200) {
            suggestions.add("应用内存占用过高($appMemory MB)，检查是否有内存泄漏")
        }
        
        val nativeMemory = getNativeMemoryUsageMB()
        if (nativeMemory > 100) {
            suggestions.add("Native内存占用过高($nativeMemory MB)，检查TFLite/OpenGL资源释放")
        }
        
        return suggestions
    }

    /**
     * 优化Bitmap内存（带OOM防护）
     * @param bitmap Bitmap对象
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 优化后的Bitmap
     */
    fun optimizeBitmap(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (bitmap == null || bitmap.isRecycled) return null

        val width = bitmap.width
        val height = bitmap.height

        // 如果尺寸合适，直接返回
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // 计算缩放比例
        val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        // OOM 防护：预估内存并检查
        val estimatedBytes = newWidth * newHeight * 4L
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        if (usedMemory + estimatedBytes > maxMemory * 0.9) {
            Log.e(TAG, "Bitmap缩放内存不足，跳过: ${estimatedBytes / 1024 / 1024}MB")
            return null
        }

        return try {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            // 回收原始Bitmap（如果不是同一个对象）
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }
            scaledBitmap
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Bitmap缩放OOM", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap缩放失败", e)
            null
        }
    }

    /**
     * 计算Bitmap内存大小（MB）
     */
    fun calculateBitmapMemoryMB(bitmap: Bitmap?): Double {
        if (bitmap == null) return 0.0
        val bytesPerPixel = when (bitmap.config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 4
        }
        return (bitmap.width * bitmap.height * bytesPerPixel).toDouble() / (1024 * 1024)
    }

    /**
     * 安全加载大图
     * @param context Context
     * @param imagePath 图片路径
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return Bitmap对象
     */
    fun safeLoadLargeBitmap(context: Context, imagePath: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            // 先检查内存状态
            if (checkMemoryStatus(context) == MemoryStatus.CRITICAL) {
                Log.w(TAG, "内存严重不足，拒绝加载大图")
                return null
            }
            
            // 获取图片尺寸
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeFile(imagePath, options)
            
            // 计算采样率
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)
            
            // 加载图片
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 减少内存占用
            
            android.graphics.BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "加载大图失败: ${e.message}", e)
            null
        }
    }

    /**
     * 计算采样率
     */
    private fun calculateSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * 创建安全的协程作用域
     * @param name 作用域名称
     * @return CoroutineScope
     */
    fun createSafeScope(name: String): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scopeMap[name] = WeakReference(scope)
        Log.d(TAG, "创建协程作用域: $name")
        return scope
    }

    /**
     * 取消并清理协程作用域（带空安全检查）
     * @param name 作用域名称
     */
    fun cancelScope(name: String) {
        val ref = scopeMap[name]
        if (ref == null) {
            Log.w(TAG, "cancelScope: 作用域 $name 不存在")
            return
        }
        val scope = ref.get()
        if (scope == null) {
            Log.w(TAG, "cancelScope: 作用域 $name 已被GC回收")
            scopeMap.remove(name)
            return
        }
        scope.cancel()
        Log.d(TAG, "取消协程作用域: $name")
        scopeMap.remove(name)
    }

    /**
     * 清理所有协程作用域
     */
    fun clearAllScopes() {
        scopeMap.forEach { (name, ref) ->
            ref.get()?.cancel()
            Log.d(TAG, "清理协程作用域: $name")
        }
        scopeMap.clear()
    }

    /**
     * 记录操作时间
     * @param operation 操作名称
     * @param startTime 开始时间（毫秒）
     */
    fun recordOperationTime(operation: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        operationTimes.getOrPut(operation) { AtomicLong(0) }.addAndGet(duration)
        operationCounts.getOrPut(operation) { AtomicLong(0) }.incrementAndGet()
        
        // 如果操作时间过长，记录警告
        if (duration > 500) {
            Log.w(TAG, "操作耗时过长: $operation ($duration ms)")
        }
    }

    /**
     * 获取平均操作时间
     * @param operation 操作名称
     * @return 平均时间（毫秒）
     */
    fun getAverageOperationTime(operation: String): Long {
        val totalTime = operationTimes[operation]?.get() ?: 0L
        val count = operationCounts[operation]?.get() ?: 0L
        return if (count > 0) totalTime / count else 0L
    }

    /**
     * 获取性能报告
     * @return 性能报告字符串
     */
    fun getPerformanceReport(): String {
        val report = StringBuilder()
        report.append("=== 性能报告 ===\n")
        report.append("应用内存: ${getAppMemoryUsageMB()} MB\n")
        report.append("Native内存: ${getNativeMemoryUsageMB()} MB\n")
        report.append("活跃协程作用域: ${scopeMap.size}\n")
        
        if (operationCounts.isNotEmpty()) {
            report.append("\n操作统计:\n")
            operationCounts.forEach { (op, count) ->
                val avgTime = getAverageOperationTime(op)
                report.append("  $op: ${count.get()}次, 平均${avgTime}ms\n")
            }
        }
        
        report.append("=== 结束 ===\n")
        return report.toString()
    }

    /**
     * ANR预防：检查主线程阻塞
     * @param context Context
     * @return 是否可能发生ANR
     */
    fun checkANRRisk(context: Context): Boolean {
        // 检查内存状态
        if (checkMemoryStatus(context) == MemoryStatus.CRITICAL) {
            Log.w(TAG, "ANR风险：内存严重不足")
            return true
        }
        
        // 检查是否有长时间运行的操作
        operationCounts.forEach { (op, count) ->
            val avgTime = getAverageOperationTime(op)
            if (avgTime > 5000) {
                Log.w(TAG, "ANR风险：操作 $op 平均耗时 $avgTime ms")
                return true
            }
        }
        
        return false
    }

    /**
     * 触发内存优化
     * @param context Context
     */
    fun optimizeMemory(context: Context) {
        Log.i(TAG, "开始内存优化")
        
        // 清理无效的协程作用域引用
        scopeMap.entries.removeAll { it.value.get() == null }
        
        // 触发GC
        System.gc()
        
        // 记录优化后的内存状态
        val availableMB = getAvailableMemoryMB(context)
        val appMemoryMB = getAppMemoryUsageMB()
        Log.i(TAG, "内存优化完成: 可用${availableMB}MB, 应用占用${appMemoryMB}MB")
    }

    /**
     * 内存状态枚举
     */
    enum class MemoryStatus {
        OK,       // 内存充足
        WARNING,  // 内存警告
        CRITICAL  // 内存严重不足
    }
}