package com.silas.omaster.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * STAB-003: 文件访问并发控制器
 *
 * 解决问题：
 * 1. FileDescriptor 泄露：快速冷启动时多次打开同一文件导致句柄泄漏
 * 2. SQLite 锁定：并发访问同一数据库文件导致死锁
 * 3. 缓存文件竞争：多个进程/线程同时读写缓存文件
 *
 * 使用方式：
 * ```kotlin
 * FileAccessController.withLock("presets_cache", timeoutMs = 5000) {
 *     // 安全的文件操作
 *     cacheFile.writeText(json)
 * }
 * ```
 */
object FileAccessController {

    private const val TAG = "FileAccessController"

    /** 每个文件的最大并发访问数 */
    private const val MAX_CONCURRENT_ACCESS = 1

    /** 全局文件锁映射 */
    private val fileLocks = ConcurrentHashMap<String, Semaphore>()

    /** 打开文件计数器（用于检测FD泄漏） */
    private val openFileCounts = ConcurrentHashMap<String, Int>()

    /**
     * 安全执行文件操作
     *
     * @param fileKey 文件标识（通常是文件名或路径）
     * @param timeoutMs 超时时间（毫秒），超时后返回 null
     * @param block 执行的文件操作
     * @return 操作结果，失败返回 null
     */
    @Synchronized
    fun <T> withLock(fileKey: String, timeoutMs: Long = 5000, block: () -> T?): T? {
        val lock = fileLocks.computeIfAbsent(fileKey) { Semaphore(MAX_CONCURRENT_ACCESS) }

        val startTime = System.currentTimeMillis()
        try {
            // 获取锁，超时保护
            if (!lock.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "获取文件锁超时: $fileKey, 耗时: ${System.currentTimeMillis() - startTime}ms")
                return null
            }

            // 增加打开计数
            openFileCounts[fileKey] = openFileCounts.getOrDefault(fileKey, 0) + 1
            val currentCount = openFileCounts[fileKey] ?: 0
            if (currentCount > 3) {
                Log.w(TAG, "文件打开计数较高: $fileKey = $currentCount")
            }

            // 执行文件操作
            return try {
                block()
            } finally {
                // 减少打开计数
                openFileCounts[fileKey] = (openFileCounts[fileKey] ?: 1) - 1
                lock.release()
            }

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w(TAG, "文件操作被中断: $fileKey", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "文件操作异常: $fileKey", e)
            return null
        }
    }

    /**
     * 安全读取文件内容
     */
    fun safeReadFile(file: File): String? {
        return withLock(file.absolutePath) {
            if (!file.exists()) return@withLock null
            try {
                file.readText()
            } catch (e: Exception) {
                Log.w(TAG, "读取文件失败: ${file.name}", e)
                null
            }
        }
    }

    /**
     * 安全写入文件内容（原子写入，防止写入中断导致文件损坏）
     */
    fun safeWriteFile(file: File, content: String): Boolean {
        return withLock(file.absolutePath) {
            try {
                // 写入临时文件
                val tempFile = File(file.parent, "${file.name}.tmp")
                tempFile.writeText(content)

                // 原子替换
                if (!tempFile.renameTo(file)) {
                    // renameTo 失败时回退直接写入
                    file.writeText(content)
                    tempFile.delete()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "写入文件失败: ${file.name}", e)
                false
            }
        }
    }

    /**
     * 获取当前打开文件统计（用于调试）
     */
    fun getOpenFileStats(): Map<String, Int> {
        return openFileCounts.filter { it.value > 0 }
    }

    /**
     * 重置统计（仅用于测试）
     */
    fun resetStats() {
        openFileCounts.clear()
    }
}