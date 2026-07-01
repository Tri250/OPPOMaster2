package com.silas.omaster.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.File

/**
 * 性能基准监控工具
 *
 * 用于：
 * - 测量应用启动时间
 * - 监控内存使用情况
 * - 估算 APK 体积
 * - 检测性能回归
 *
 * 注意：此工具仅在 Debug 构建中激活，Release 构建中为 no-op。
 */
object PerformanceMonitor {

    private const val TAG = "PerfMonitor"

    @Volatile
    private var enabled: Boolean = false

    /** 启动性能数据 */
    data class StartupMetrics(
        val coldStartMs: Long,
        val warmStartMs: Long,
        val appInitMs: Long,
        val firstFrameMs: Long
    )

    /** 内存快照 */
    data class MemorySnapshot(
        val heapSizeMB: Long,
        val heapUsedMB: Long,
        val heapFreeMB: Long,
        val nativeHeapMB: Long,
        val pssKB: Long,
        val rssKB: Long,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 启用性能监控（仅 Debug 构建）
     */
    fun enable() {
        enabled = true
        Log.d(TAG, "性能监控已启用")
    }

    /**
     * 获取当前内存快照
     */
    fun getMemorySnapshot(context: Context): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val heapSize = runtime.totalMemory() / (1024 * 1024)
        val heapFree = runtime.freeMemory() / (1024 * 1024)
        val heapUsed = heapSize - heapFree

        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

        val pssKB = getPssKB(context)
        val rssKB = getRssKB()

        return MemorySnapshot(
            heapSizeMB = heapSize,
            heapUsedMB = heapUsed,
            heapFreeMB = heapFree,
            nativeHeapMB = nativeHeap,
            pssKB = pssKB,
            rssKB = rssKB
        )
    }

    /**
     * 检查内存是否超过阈值
     *
     * @param thresholdMB 内存阈值（MB）
     * @return true 表示内存使用超过阈值
     */
    fun isMemoryExceeded(context: Context, thresholdMB: Long = 256): Boolean {
        val snapshot = getMemorySnapshot(context)
        return snapshot.heapUsedMB > thresholdMB
    }

    /**
     * 记录内存使用情况（仅在 Debug 模式）
     */
    fun logMemoryUsage(context: Context, tag: String = TAG) {
        if (!enabled) return
        val snapshot = getMemorySnapshot(context)
        Log.d(tag, "内存: Heap=${snapshot.heapUsedMB}MB/${snapshot.heapSizeMB}MB, " +
                "Native=${snapshot.nativeHeapMB}MB, PSS=${snapshot.pssKB}KB, RSS=${snapshot.rssKB}KB")
    }

    /**
     * 获取 APK 体积信息
     */
    fun getApkSizeInfo(context: Context): ApkSizeInfo {
        return try {
            val sourceDir = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .applicationInfo?.sourceDir ?: return ApkSizeInfo(0, 0, 0)

            val apkFile = File(sourceDir)
            val apkSize = apkFile.length()

            // 计算解压后大小
            val nativeLibDir = File(context.applicationInfo.nativeLibDir)
            val libSize = if (nativeLibDir.exists()) {
                nativeLibDir.walkTopDown().sumOf { it.length() }
            } else 0L

            ApkSizeInfo(
                apkSizeBytes = apkSize,
                apkSizeMB = apkSize / (1024.0 * 1024.0),
                nativeLibSizeMB = libSize / (1024.0 * 1024.0)
            )
        } catch (e: Exception) {
            Log.w(TAG, "获取 APK 体积失败", e)
            ApkSizeInfo(0, 0, 0)
        }
    }

    data class ApkSizeInfo(
        val apkSizeBytes: Long,
        val apkSizeMB: Double,
        val nativeLibSizeMB: Double
    )

    // ===== 私有方法 =====

    private fun getPssKB(context: Context): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = arrayOf(Process.myPid())
            val memInfoArray = am?.getProcessMemoryInfo(memInfo)
            memInfoArray?.firstOrNull()?.totalPss?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getRssKB(): Long {
        return try {
            val statFile = File("/proc/${Process.myPid()}/status")
            if (!statFile.exists()) return 0L
            statFile.readLines().forEach { line ->
                if (line.startsWith("VmRSS:")) {
                    return line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }
}