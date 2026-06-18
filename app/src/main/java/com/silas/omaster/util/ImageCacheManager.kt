package com.silas.omaster.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.core.net.toUri
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.silas.omaster.model.MasterPreset
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * 图片下载回调接口
 */
interface ImageDownloadCallback {
    fun onStart(url: String)
    fun onProgress(url: String, bytesDownloaded: Long, totalBytes: Long)
    fun onSuccess(url: String, file: File)
    fun onError(url: String, error: Throwable, retryCount: Int)
    fun onRetry(url: String, attempt: Int)
}

/**
 * 下载状态
 */
sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val exception: Throwable, val retryCount: Int) : DownloadResult()
}

/**
 * 图片缓存管理器
 * 管理网络图片的本地缓存，减少对象存储流量费用
 *
 * 内存管理增强：
 * - LRU 内存缓存（可配置大小限制）
 * - 磁盘缓存大小限制
 * - 基于内存压力的自动缓存清理（ComponentCallbacks2）
 */
class ImageCacheManager private constructor(private val context: Context) : ComponentCallbacks2 {

    companion object {
        private const val TAG = "ImageCacheManager"
        private const val CACHE_DIR = "presets/images"
        private const val TIMEOUT_MS = 30000L
        private const val MAX_RETRIES = 3
        private const val DEFAULT_MAX_DISK_CACHE_BYTES = 50L * 1024 * 1024

        @Volatile
        private var instance: ImageCacheManager? = null

        fun getInstance(context: Context): ImageCacheManager {
            return instance ?: synchronized(this) {
                instance ?: ImageCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // ==================== LRU 内存缓存 ====================

    /** 默认最大内存缓存大小：可用内存的 1/8 */
    private val defaultMemoryCacheSize: Int by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        maxMemory / 8
    }

    /** 可配置的最大内存缓存大小（KB） */
    @Volatile
    var maxMemoryCacheSizeKB: Int = 0
        set(value) {
            field = value
            if (value > 0) {
                resizeMemoryCache(value)
            }
        }

    /** LRU 内存缓存 */
    private var memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(defaultMemoryCacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private fun resizeMemoryCache(sizeKB: Int) {
        synchronized(memoryCache) {
            val newCache = object : LruCache<String, Bitmap>(sizeKB) {
                override fun sizeOf(key: String, bitmap: Bitmap): Int {
                    return bitmap.byteCount / 1024
                }
            }
            memoryCache = newCache
        }
    }

    init {
        if (maxMemoryCacheSizeKB > 0) {
            resizeMemoryCache(maxMemoryCacheSizeKB)
        }
    }

    // ==================== 磁盘缓存大小限制 ====================

    /** 可配置的最大磁盘缓存大小（字节） */
    @Volatile
    var maxDiskCacheBytes: Long = DEFAULT_MAX_DISK_CACHE_BYTES

    // ==================== HTTP 客户端 ====================

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MS
                connectTimeoutMillis = TIMEOUT_MS
                socketTimeoutMillis = TIMEOUT_MS
            }
        }
    }

    // 记录失败的下载，用于后台重试
    // 线程安全：使用 Collections.synchronizedSet 包裹
    private val failedDownloads = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    // ==================== ComponentCallbacks2 ====================

    private var registeredForCallbacks = false

    fun registerMemoryCallbacks() {
        if (!registeredForCallbacks) {
            context.registerComponentCallbacks(this)
            registeredForCallbacks = true
            Log.d(TAG, "已注册 ComponentCallbacks2")
        }
    }

    fun unregisterMemoryCallbacks() {
        if (registeredForCallbacks) {
            context.unregisterComponentCallbacks(this)
            registeredForCallbacks = false
            Log.d(TAG, "已注销 ComponentCallbacks2")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // 无需处理
    }

    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory: 清空内存缓存")
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }

    override fun onTrimMemory(level: Int) {
        Log.w(TAG, "onTrimMemory: level=$level")
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 应用在前台但内存紧张，清空一半缓存
                trimMemoryCache(0.5f)
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // 应用进入后台，清空缓存
                trimMemoryCache(0.0f)
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // 后台且内存紧张，清空全部缓存
                synchronized(memoryCache) {
                    memoryCache.evictAll()
                }
                // 清理磁盘缓存
                trimDiskCache()
            }
        }
    }

    private fun trimMemoryCache(keepFraction: Float) {
        synchronized(memoryCache) {
            val currentSize = memoryCache.size()
            val targetSize = (currentSize * keepFraction).toInt()
            if (targetSize < currentSize) {
                memoryCache.trimToSize(targetSize)
            }
        }
    }

    private fun trimDiskCache() {
        try {
            val cacheDir = File(context.filesDir, CACHE_DIR)
            if (!cacheDir.exists()) return
            val files = cacheDir.listFiles() ?: return
            // 按最后修改时间排序，删除最旧的文件
            files.sortedBy { it.lastModified() }.forEach { it.delete() }
            Log.d(TAG, "已清理磁盘缓存: ${files.size} 个文件")
        } catch (e: Exception) {
            Log.w(TAG, "清理磁盘缓存失败", e)
        }
    }

    // ==================== 内存缓存操作 ====================

    /**
     * 将 Bitmap 放入内存缓存
     */
    fun putBitmap(key: String, bitmap: Bitmap) {
        synchronized(memoryCache) {
            if (memoryCache.get(key) == null) {
                memoryCache.put(key, bitmap)
            }
        }
    }

    /**
     * 从内存缓存获取 Bitmap
     */
    fun getBitmap(key: String): Bitmap? {
        synchronized(memoryCache) {
            return memoryCache.get(key)
        }
    }

    /**
     * 获取内存缓存统计信息
     */
    fun getMemoryCacheStats(): String {
        synchronized(memoryCache) {
            return "内存缓存: ${memoryCache.size()}/${memoryCache.maxSize()}KB, " +
                    "命中=${memoryCache.hitCount()}, 未命中=${memoryCache.missCount()}"
        }
    }

    // ==================== 磁盘缓存管理 ====================

    /**
     * 确保磁盘缓存大小在限制范围内
     */
    private fun enforceDiskCacheLimit() {
        try {
            val cacheDir = File(context.filesDir, CACHE_DIR)
            if (!cacheDir.exists()) return

            var totalSize = 0L
            val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return

            for (file in files) {
                totalSize += file.length()
            }

            // 超出限制时删除最旧的文件
            var currentSize = totalSize
            val iterator = files.iterator()
            while (currentSize > maxDiskCacheBytes && iterator.hasNext()) {
                val file = iterator.next()
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                }
            }

            if (totalSize > maxDiskCacheBytes) {
                Log.d(TAG, "磁盘缓存清理: ${(totalSize - currentSize) / 1024}KB 释放")
            }
        } catch (e: Exception) {
            Log.w(TAG, "磁盘缓存限制检查失败", e)
        }
    }

    // ==================== 公共 API ====================

    /**
     * 获取图片的本地缓存路径
     */
    fun getLocalImagePath(context: Context, url: String): File {
        val fileName = generateFileName(url)
        return File(context.filesDir, "$CACHE_DIR/$fileName")
    }

    /**
     * 检查图片是否已缓存到本地
     */
    fun isImageCached(context: Context, url: String): Boolean {
        if (!url.startsWith("http")) return false
        return getLocalImagePath(context, url).exists()
    }

    /**
     * 获取图片的加载路径（优先本地缓存）
     * @return 本地路径（如果存在），否则返回原始URL
     */
    fun getImageLoadPath(context: Context, url: String): String {
        // 空路径直接返回空字符串
        if (url.isBlank()) {
            Log.w(TAG, "getImageLoadPath: 图片路径为空")
            return ""
        }

        // 非网络图片直接返回
        if (!url.startsWith("http")) {
            val result = when {
                url.startsWith("/") -> File(url).toUri().toString()
                url.startsWith("presets/") -> File(context.filesDir, url).toUri().toString()
                url.startsWith("file://") -> url  // 已经是file协议，直接返回
                url.startsWith("images/") -> "file:///android_asset/$url"
                url.startsWith("assets/") -> "file:///android_asset/$url"
                url.contains("/") -> {
                    // 包含路径分隔符但无已知前缀，尝试作为 assets 路径
                    Log.d(TAG, "getImageLoadPath: 未知路径格式，尝试作为assets路径: $url")
                    "file:///android_asset/$url"
                }
                else -> {
                    // 无路径分隔符，可能是 assets 根目录下的文件
                    Log.d(TAG, "getImageLoadPath: 简单文件名，尝试作为assets路径: $url")
                    "file:///android_asset/$url"
                }
            }
            Log.d(TAG, "getImageLoadPath: 本地路径 '$url' -> '$result'")
            return result
        }

        // 检查本地缓存
        val localFile = getLocalImagePath(context, url)
        return if (localFile.exists()) {
            Log.d(TAG, "getImageLoadPath: 使用缓存 '$url' -> '${localFile.toUri()}'")
            localFile.toUri().toString()
        } else {
            Log.d(TAG, "getImageLoadPath: 使用网络URL: $url")
            url
        }
    }

    /**
     * 下载并缓存图片（带重试机制）
     * @param maxRetries 最大重试次数，默认3次
     * @param callback 下载回调
     * @return 下载结果
     */
    suspend fun downloadAndCacheImage(
        context: Context,
        url: String,
        maxRetries: Int = MAX_RETRIES,
        callback: ImageDownloadCallback? = null
    ): DownloadResult {
        if (!url.startsWith("http")) {
            return DownloadResult.Error(IllegalArgumentException("非网络URL: $url"), 0)
        }

        val localFile = getLocalImagePath(context, url)

        // 已存在则直接返回
        if (localFile.exists()) {
            failedDownloads.remove(url)  // 从失败列表移除
            // 尝试加载到内存缓存
            cacheToMemoryIfNeeded(url, localFile)
            return DownloadResult.Success(localFile)
        }

        callback?.onStart(url)

        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    if (attempt > 0) {
                        callback?.onRetry(url, attempt)
                        Log.d(TAG, "第${attempt + 1}次重试下载: $url")
                        // 指数退避：1s, 2s, 4s
                        delay(1000L * (1 shl (attempt - 1)))
                    }

                    // 创建目录
                    localFile.parentFile?.mkdirs()

                    // 下载图片
                    val bytes = client.get(url).body<ByteArray>()

                    // 使用临时文件写入，成功后重命名（原子操作）
                    val tempFile = File(localFile.absolutePath + ".tmp")
                    tempFile.writeBytes(bytes)
                    tempFile.renameTo(localFile)

                    // 下载成功
                    failedDownloads.remove(url)
                    callback?.onSuccess(url, localFile)
                    Log.d(TAG, "下载成功: $url (${bytes.size / 1024}KB)")

                    // 检查磁盘缓存大小限制
                    enforceDiskCacheLimit()

                    // 加载到内存缓存
                    cacheToMemoryIfNeeded(url, localFile)

                    return@withContext DownloadResult.Success(localFile)

                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "下载失败 (尝试 ${attempt + 1}/$maxRetries): $url - ${e.message}")

                    if (attempt == maxRetries - 1) {
                        // 最终失败
                        failedDownloads.add(url)
                        callback?.onError(url, e, maxRetries)
                        Log.e(TAG, "下载最终失败: $url", e)
                    }
                }
            }

            DownloadResult.Error(lastException ?: Exception("未知错误"), maxRetries)
        }
    }

    /**
     * 将本地文件加载到内存缓存（如果尚未缓存）
     */
    private fun cacheToMemoryIfNeeded(url: String, localFile: File) {
        try {
            val cacheKey = generateFileName(url)
            if (getBitmap(cacheKey) == null) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(localFile.absolutePath, options)
                // 仅缓存小于 4MB 的图片到内存
                val estimatedSizeKB = (options.outWidth * options.outHeight * 4) / 1024
                if (estimatedSizeKB < 4 * 1024) {
                    options.inJustDecodeBounds = false
                    options.inSampleSize = calculateInSampleSize(options, 512, 512)
                    val bitmap = BitmapFactory.decodeFile(localFile.absolutePath, options)
                    if (bitmap != null) {
                        putBitmap(cacheKey, bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            // 静默失败，不影响主流程
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 简化的下载方法（兼容旧代码）
     */
    suspend fun downloadAndCacheImage(context: Context, url: String): File? {
        return when (val result = downloadAndCacheImage(context, url, MAX_RETRIES, null)) {
            is DownloadResult.Success -> result.file
            is DownloadResult.Error -> null
        }
    }

    /**
     * 预下载预设的所有图片（封面 + 图库）
     */
    suspend fun prefetchPresetImages(
        context: Context,
        preset: MasterPreset,
        callback: ImageDownloadCallback? = null
    ) {
        // 下载封面（单张失败不影响整体）
        try {
            downloadAndCacheImage(context, preset.coverPath, callback = callback)
        } catch (e: Exception) {
            Log.w(TAG, "封面下载失败: ${preset.coverPath}", e)
        }

        // 下载图库图片
        preset.galleryImages?.forEach { url ->
            try {
                downloadAndCacheImage(context, url, callback = callback)
            } catch (e: Exception) {
                // 单张图库图片失败不影响其他图片
                Log.w(TAG, "图库图片下载失败: $url", e)
            }
        }
    }

    /**
     * 获取失败的下载列表
     */
    fun getFailedDownloads(): Set<String> {
        return synchronized(failedDownloads) { failedDownloads.toSet() }
    }

    /**
     * 重试所有失败的下载
     */
    suspend fun retryFailedDownloads(
        context: Context,
        callback: ImageDownloadCallback? = null
    ): Int {
        // 复制快照避免 ConcurrentModificationException
        val toRetry = synchronized(failedDownloads) { failedDownloads.toList() }
        var successCount = 0

        toRetry.forEach { url ->
            try {
                val result = downloadAndCacheImage(context, url, callback = callback)
                if (result is DownloadResult.Success) {
                    failedDownloads.remove(url)
                    successCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "重试URL失败: $url", e)
            }
        }

        Log.d(TAG, "重试完成: $successCount/${toRetry.size} 成功")
        return successCount
    }

    /**
     * 清理过期缓存
     * @param maxAgeDays 缓存最大保留天数
     */
    suspend fun cleanOldCache(context: Context, maxAgeDays: Int = 30) {
        withContext(Dispatchers.IO) {
            val cacheDir = File(context.filesDir, CACHE_DIR)
            if (!cacheDir.exists()) return@withContext

            val maxAge = maxAgeDays * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            var cleanedCount = 0

            cacheDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAge) {
                    file.delete()
                    cleanedCount++
                }
            }

            Log.d(TAG, "清理旧缓存: $cleanedCount 个文件")
        }
    }

    /**
     * 获取缓存大小（MB）
     */
    fun getCacheSize(context: Context): Double {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) return 0.0

        val size = cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()

        return size / (1024.0 * 1024.0)
    }

    /**
     * 清除所有缓存
     */
    fun clearCache(context: Context) {
        // 清空内存缓存
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        // 清空磁盘缓存
        File(context.filesDir, CACHE_DIR).deleteRecursively()
        failedDownloads.clear()
        unregisterMemoryCallbacks()
        Log.d(TAG, "缓存已清空")
    }

    /**
     * 释放资源
     */
    fun release() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        unregisterMemoryCallbacks()
    }

    /**
     * 生成缓存文件名（基于URL路径，忽略域名）
     */
    private fun generateFileName(url: String): String {
        return try {
            // 去掉协议和域名，只保留路径部分
            val path = url.replace(Regex("^https?://[^/]+/"), "")
                .replace("/", "_")
                .take(100)

            if (path.contains(".")) {
                path
            } else {
                "$path.webp"
            }
        } catch (e: Exception) {
            val md5 = MessageDigest.getInstance("MD5")
                .digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(8)
            "$md5.webp"
        }
    }

    /**
     * 创建 Coil ImageRequest
     */
    fun createImageRequest(context: Context, url: String): ImageRequest {
        val loadPath = getImageLoadPath(context, url)

        return ImageRequest.Builder(context)
            .data(loadPath)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
