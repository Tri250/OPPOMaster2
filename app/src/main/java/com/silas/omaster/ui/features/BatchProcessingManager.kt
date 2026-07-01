package com.silas.omaster.ui.features

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.silas.omaster.model.HasselbladParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 批量处理管理器 - P2 深度优化 + F2-17 多核并行
 *
 * 功能：
 * - 多选图片批量应用预设
 * - 进度追踪与回调
 * - 支持取消操作
 * - 导出格式选择
 * - 内存安全的大图批量处理
 * - F2-17: 多核并行处理（根据 CPU 核心数自动分配线程池）
 * - F2-17: 独立图片进度追踪与错误隔离
 */
class BatchProcessingManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "BatchProcessing"
        private const val MAX_BATCH_SIZE = 20
        private const val EXPORT_MAX_DIMENSION = 2048
    }

    // F2-17: 检测 CPU 核心数并创建固定线程池
    private val cpuCores: Int = Runtime.getRuntime().availableProcessors()
    // 最多使用 core-1 个线程，保留一个核心给 UI 和其他任务
    private val parallelism: Int = maxOf(2, cpuCores - 1).coerceAtMost(4)
    private val threadPool = Executors.newFixedThreadPool(parallelism)
    private val parallelDispatcher = threadPool.asCoroutineDispatcher()

    // 批次状态
    private val _batchState = MutableStateFlow(BatchState())
    val batchState: StateFlow<BatchState> = _batchState.asStateFlow()

    // F2-17: 每张图片的独立进度追踪
    private val _imageProgress = MutableStateFlow<Map<Int, ImageProgress>>(emptyMap())
    val imageProgress: StateFlow<Map<Int, ImageProgress>> = _imageProgress.asStateFlow()

    // 取消标志
    @Volatile
    private var isCancelled = false

    // 协程 Job，用于取消正在执行的协程
    private var processingJob: Job? = null

    // 线程同步锁
    private val lock = Any()

    /**
     * 批量处理图片 - F2-17: 多核并行处理
     * @param imageUris 待处理的图片 URI 列表
     * @param params 预设参数
     * @param exportFormat 导出格式
     * @param onProgress 进度回调（合并后的整体进度）
     * @param onImageProgress F2-17: 单图进度回调（独立追踪每张图）
     * @param onComplete 完成回调
     */
    suspend fun processBatch(
        imageUris: List<Uri>,
        params: HasselbladParams,
        exportFormat: HasselbladEyeViewModel.ExportFormat = HasselbladEyeViewModel.ExportFormat.JPEG,
        onProgress: (current: Int, total: Int, uri: Uri) -> Unit = { _, _, _ -> },
        onImageProgress: ((index: Int, progress: ImageProgress) -> Unit)? = null,
        onComplete: (results: List<BatchResult>) -> Unit = {}
    ) {
        if (imageUris.size > MAX_BATCH_SIZE) {
            synchronized(lock) {
                _batchState.value = _batchState.value.copy(
                    error = "批量处理最多支持 $MAX_BATCH_SIZE 张图片"
                )
            }
            return
        }

        synchronized(lock) {
            isCancelled = false
            _batchState.value = BatchState(
                isProcessing = true,
                totalCount = imageUris.size,
                currentIndex = 0
            )
        }

        // F2-17: 初始化每张图片的独立进度
        val initialProgress = imageUris.indices.associateWith { ImageProgress() }
        _imageProgress.value = initialProgress

        val results = arrayOfNulls<BatchResult>(imageUris.size)
        val completedCount = AtomicInteger(0)

        withContext(Dispatchers.IO) {
            // 捕获当前协程的 Job 以便外部取消
            processingJob = coroutineContext[Job]

            // F2-17: 使用并行调度器并发处理多张图片
            // 使用 coroutineScope 确保所有子协程完成后再继续
            coroutineScope {
                imageUris.forEachIndexed { index, uri ->
                    // 在并行调度器上启动独立的处理协程
                    launch(parallelDispatcher) {
                        // 检查取消
                        if (!isActive || isCancelled) {
                            results[index] = BatchResult(uri, null, "已取消")
                            updateImageProgress(index, ImageProgress(status = ImageStatus.CANCELLED), onImageProgress)
                            val done = completedCount.incrementAndGet()
                            updateOverallProgress(done, imageUris.size, onProgress, imageUris, results)
                            return@launch
                        }

                        try {
                            // 阶段 1: 加载图片
                            updateImageProgress(index, ImageProgress(status = ImageStatus.LOADING, progress = 0.2f), onImageProgress)

                            val bitmap = loadBitmap(context, uri)
                            if (bitmap == null) {
                                results[index] = BatchResult(uri, null, "图片加载失败")
                                updateImageProgress(index, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "图片加载失败"), onImageProgress)
                                val done = completedCount.incrementAndGet()
                                updateOverallProgress(done, imageUris.size, onProgress, imageUris, results)
                                return@launch
                            }

                            // 阶段 2: 应用预设
                            updateImageProgress(index, ImageProgress(status = ImageStatus.PROCESSING, progress = 0.5f), onImageProgress)

                            val processedBitmap = applyPreset(bitmap, params)

                            // 阶段 3: 保存图片
                            updateImageProgress(index, ImageProgress(status = ImageStatus.SAVING, progress = 0.8f), onImageProgress)

                            val savedUri = saveProcessedImage(
                                processedBitmap,
                                uri,
                                exportFormat
                            )

                            // 回收 Bitmap
                            if (processedBitmap !== bitmap) processedBitmap.recycle()
                            bitmap.recycle()

                            if (savedUri != null) {
                                results[index] = BatchResult(uri, savedUri, null)
                                updateImageProgress(index, ImageProgress(status = ImageStatus.COMPLETED, progress = 1f), onImageProgress)
                            } else {
                                results[index] = BatchResult(uri, null, "保存失败")
                                updateImageProgress(index, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "保存失败"), onImageProgress)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "处理图片[$index]失败: ${e.message}", e)
                            // F2-17: 单图错误不影响其他图片，继续处理
                            results[index] = BatchResult(uri, null, e.message ?: "未知错误")
                            updateImageProgress(index, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = e.message), onImageProgress)
                        }

                        // 更新整体进度
                        val done = completedCount.incrementAndGet()
                        updateOverallProgress(done, imageUris.size, onProgress, imageUris, results)
                    }
                }
            }

            // 如果中途取消，填充剩余结果
            if (!isActive || isCancelled) {
                synchronized(lock) {
                    for (i in imageUris.indices) {
                        if (results[i] == null) {
                            results[i] = BatchResult(imageUris[i], null, "已取消")
                            updateImageProgress(i, ImageProgress(status = ImageStatus.CANCELLED, progress = 1f), onImageProgress)
                        }
                    }
                }
            }
        }

        synchronized(lock) {
            _batchState.value = _batchState.value.copy(
                isProcessing = false,
                isComplete = true
            )
        }
        onComplete(results.filterNotNull())
    }

    /**
     * F2-17: 更新单图进度
     */
    private fun updateImageProgress(
        index: Int,
        progress: ImageProgress,
        callback: ((index: Int, progress: ImageProgress) -> Unit)?
    ) {
        val current = _imageProgress.value.toMutableMap()
        current[index] = progress
        _imageProgress.value = current
        callback?.invoke(index, progress)
    }

    /**
     * F2-17: 更新整体进度（合并所有图片进度）
     */
    private fun updateOverallProgress(
        completedCount: Int,
        totalCount: Int,
        onProgress: (current: Int, total: Int, uri: Uri) -> Unit,
        imageUris: List<Uri>,
        results: Array<BatchResult?>
    ) {
        synchronized(lock) {
            _batchState.value = _batchState.value.copy(currentIndex = completedCount)
        }
        // 找到最后完成的图片的 URI 用于回调
        val lastCompletedIndex = results.indexOfLast { it != null }
        val lastUri = if (lastCompletedIndex >= 0) imageUris[lastCompletedIndex] else imageUris.first()
        onProgress(completedCount, totalCount, lastUri)
    }

    /**
     * 取消批量处理
     */
    fun cancel() {
        synchronized(lock) {
            isCancelled = true
            _batchState.value = _batchState.value.copy(isCancelled = true)
        }
        // 同时取消协程，确保 isActive 检查能立即生效
        processingJob?.cancel()
    }

    /**
     * 重置状态
     */
    fun reset() {
        synchronized(lock) {
            isCancelled = false
            _batchState.value = BatchState()
        }
        _imageProgress.value = emptyMap()
        processingJob = null
    }

    /**
     * 加载图片 - 使用单一字节数组流避免重复打开资源
     */
    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            // 一次性读取全部字节，避免两次打开 InputStream
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return null

            // 第一次解码：仅获取尺寸信息
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // 计算采样率
            options.inSampleSize = calculateSampleSize(
                options.outWidth, options.outHeight,
                EXPORT_MAX_DIMENSION, EXPORT_MAX_DIMENSION
            )
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            // 第二次解码：使用同一字节数组实际解码
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: ${e.message}", e)
            null
        }
    }

    /**
     * 应用预设参数
     */
    private fun applyPreset(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        // 如果源已经是 ARGB_8888 且可变，直接使用；否则才拷贝
        val result = if (bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.isMutable) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint()

        // 饱和度
        if (params.saturation != 0) {
            val saturation = (params.saturation + 30) / 60f * 2f
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(saturation)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // 对比度
        if (params.contrast != 0) {
            val contrast = (params.contrast + 30) / 60f * 2f
            val cm = android.graphics.ColorMatrix()
            cm.setScale(contrast, contrast, contrast, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
        }

        // 色温
        if (params.colorTemp != 0) {
            val warmth = params.colorTemp / 30f * 0.3f
            val cm = android.graphics.ColorMatrix()
            cm.set(floatArrayOf(
                1f + warmth, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - warmth, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
        }

        // 影调
        if (params.tone != 0) {
            val toneScale = 1f + params.tone / 30f * 0.3f
            val cm = android.graphics.ColorMatrix()
            cm.setScale(toneScale, toneScale, toneScale, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
        }

        paint.colorFilter = null
        return result
    }

    /**
     * 保存处理后的图片
     */
    private fun saveProcessedImage(
        bitmap: Bitmap,
        originalUri: Uri,
        format: HasselbladEyeViewModel.ExportFormat
    ): Uri? {
        return try {
            val (compressFormat, extension) = when (format) {
                HasselbladEyeViewModel.ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG to "jpg"
                HasselbladEyeViewModel.ExportFormat.PNG -> Bitmap.CompressFormat.PNG to "png"
                HasselbladEyeViewModel.ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP to "webp"
                HasselbladEyeViewModel.ExportFormat.HEIF -> {
                    Log.w(TAG, "HEIF 格式当前设备不支持，已回退为 JPEG 格式导出")
                    Bitmap.CompressFormat.JPEG to "heic"
                }
            }
            val mimeType = when (format) {
                HasselbladEyeViewModel.ExportFormat.JPEG -> "image/jpeg"
                HasselbladEyeViewModel.ExportFormat.PNG -> "image/png"
                HasselbladEyeViewModel.ExportFormat.WEBP -> "image/webp"
                HasselbladEyeViewModel.ExportFormat.HEIF -> "image/heif"
            }

            val filename = "OMaster_Batch_${System.currentTimeMillis()}_${originalUri.lastPathSegment ?: "processed"}.$extension"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OMaster/Batch")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.also {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(compressFormat, 95, out)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存图片失败: ${e.message}", e)
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxWidth || height / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }
}

/**
 * 批量处理状态
 */
data class BatchState(
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val isCancelled: Boolean = false,
    val totalCount: Int = 0,
    val currentIndex: Int = 0,
    val error: String? = null
)

/**
 * 批量处理结果
 */
data class BatchResult(
    val originalUri: Uri,
    val savedUri: Uri?,
    val error: String?
)

/**
 * F2-17: 单图处理进度状态
 * 用于独立追踪每张图片的处理进度
 */
enum class ImageStatus {
    PENDING,        // 等待处理
    LOADING,        // 正在加载图片
    PROCESSING,     // 正在应用预设
    SAVING,         // 正在保存
    COMPLETED,      // 已完成
    FAILED,         // 处理失败（不影响其他图片）
    CANCELLED       // 已取消
}

data class ImageProgress(
    val status: ImageStatus = ImageStatus.PENDING,
    val progress: Float = 0f,   // 0f~1f 单图内部进度
    val error: String? = null   // 失败时的错误信息
)