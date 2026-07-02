package com.silas.omaster.ui.features

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.data.watermark.WatermarkConfig
import com.silas.omaster.data.watermark.WatermarkRenderer
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
        watermarkConfig: WatermarkConfig? = null,
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

                            val presetBitmap = applyPreset(bitmap, params)
                            var processedBitmap = presetBitmap

                            // 阶段 2.5: 应用水印（如果配置了）
                            if (watermarkConfig != null) {
                                processedBitmap = WatermarkRenderer.renderWatermark(processedBitmap, watermarkConfig)
                            }

                            // 阶段 3: 保存图片
                            updateImageProgress(index, ImageProgress(status = ImageStatus.SAVING, progress = 0.8f), onImageProgress)

                            val savedUri = saveProcessedImage(
                                processedBitmap,
                                uri,
                                exportFormat
                            )

                            // 回收 Bitmap：按创建顺序逆序释放，避免中间产物泄漏 (MEM-01 / MEM-05)
                            if (processedBitmap !== presetBitmap) processedBitmap.recycle()
                            if (presetBitmap !== bitmap) presetBitmap.recycle()
                            bitmap.recycle()

                            if (savedUri != null) {
                                results[index] = BatchResult(uri, savedUri, null)
                                updateImageProgress(index, ImageProgress(status = ImageStatus.COMPLETED, progress = 1f), onImageProgress)
                            } else {
                                results[index] = BatchResult(uri, null, "保存失败")
                                updateImageProgress(index, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "保存失败"), onImageProgress)
                            }

                        } catch (e: OutOfMemoryError) {
                            Log.e(TAG, "处理图片[$index] OOM: ${e.message}")
                            System.gc()
                            results[index] = BatchResult(uri, null, "内存不足，已跳过此图")
                            updateImageProgress(index, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "内存不足，已跳过此图"), onImageProgress)
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
     * 检查当前设备是否支持 WebP 格式 (UC-25)
     * Android 10 (API 29) 及以上原生支持 WebP 编码
     */
    fun isWebPSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * 解析导出格式，处理不兼容的回退逻辑 (UC-25)
     * - WebP 在 Android 10 以下回退为 JPEG
     * - HEIF 不支持，回退为 JPEG
     */
    private fun resolveExportFormat(format: HasselbladEyeViewModel.ExportFormat): HasselbladEyeViewModel.ExportFormat {
        return when (format) {
            HasselbladEyeViewModel.ExportFormat.WEBP -> {
                if (!isWebPSupported()) {
                    Log.w(TAG, "WebP 格式在当前设备 (API ${Build.VERSION.SDK_INT}) 不支持，已回退为 JPEG")
                    HasselbladEyeViewModel.ExportFormat.JPEG
                } else {
                    format
                }
            }
            HasselbladEyeViewModel.ExportFormat.HEIF -> {
                Log.w(TAG, "HEIF 格式当前设备不支持，已回退为 JPEG 格式导出")
                HasselbladEyeViewModel.ExportFormat.JPEG
            }
            else -> format
        }
    }

    /**
     * 按目标分辨率缩放 Bitmap (UC-10)
     */
    private fun scaleBitmapToResolution(bitmap: Bitmap, resolution: ExportResolution): Bitmap {
        if (resolution == ExportResolution.ORIGINAL) return bitmap
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val targetMaxWidth = resolution.maxWidth
        val targetMaxHeight = resolution.maxHeight
        if (srcWidth <= targetMaxWidth && srcHeight <= targetMaxHeight) return bitmap
        val scale = minOf(targetMaxWidth.toFloat() / srcWidth, targetMaxHeight.toFloat() / srcHeight)
        val newWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val newHeight = (srcHeight * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * UC-10: 多格式 + 多分辨率批量导出
     * 对每张图片 × 每种格式 × 每种分辨率的组合，分别导出一个文件
     *
     * @param imageUris 待处理的图片 URI 列表
     * @param params 预设参数
     * @param exportFormats 导出格式列表（可多选）
     * @param resolutions 导出分辨率列表（可多选）
     * @param watermarkConfig 水印配置
     * @param onProgress 进度回调（合并后的整体进度）
     * @param onImageProgress 单图进度回调
     * @param onComplete 完成回调
     */
    suspend fun processBatchMultiFormat(
        imageUris: List<Uri>,
        params: HasselbladParams,
        exportFormats: List<HasselbladEyeViewModel.ExportFormat>,
        resolutions: List<ExportResolution>,
        watermarkConfig: WatermarkConfig? = null,
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

        if (exportFormats.isEmpty() || resolutions.isEmpty()) {
            synchronized(lock) {
                _batchState.value = _batchState.value.copy(
                    error = "请至少选择一种导出格式和一种分辨率"
                )
            }
            return
        }

        // UC-25: 预检 WebP 兼容性，不支持的格式回退为 JPEG
        val resolvedFormats = exportFormats.map { resolveExportFormat(it) }.distinct()

        // 展开所有任务组合：图片 × 格式 × 分辨率
        data class ExportTask(
            val imageIndex: Int,
            val imageUri: Uri,
            val format: HasselbladEyeViewModel.ExportFormat,
            val resolution: ExportResolution
        )

        val tasks = mutableListOf<ExportTask>()
        for (i in imageUris.indices) {
            for (fmt in resolvedFormats) {
                for (res in resolutions) {
                    tasks.add(ExportTask(i, imageUris[i], fmt, res))
                }
            }
        }

        val totalCount = tasks.size

        synchronized(lock) {
            isCancelled = false
            _batchState.value = BatchState(
                isProcessing = true,
                totalCount = totalCount,
                currentIndex = 0
            )
        }

        // 初始化每张图片的独立进度
        val initialProgress = imageUris.indices.associateWith { ImageProgress() }
        _imageProgress.value = initialProgress

        val results = arrayOfNulls<BatchResult>(totalCount)
        val completedCount = AtomicInteger(0)

        // 保存已恢复（跳过）的任务索引，用于 UC-18 恢复时跳过已完成的导出
        val restoredCompletedIndices = restoreBatchState()

        withContext(Dispatchers.IO) {
            processingJob = coroutineContext[Job]

            coroutineScope {
                tasks.forEachIndexed { taskIndex, task ->
                    // UC-18: 如果已恢复状态中标记此任务为已完成，则跳过
                    if (taskIndex in restoredCompletedIndices) {
                        results[taskIndex] = BatchResult(
                            task.imageUri, null, null
                        )
                        val done = completedCount.incrementAndGet()
                        updateOverallProgress(done, totalCount, onProgress, imageUris, results)
                        return@forEachIndexed
                    }

                    launch(parallelDispatcher) {
                        if (!isActive || isCancelled) {
                            results[taskIndex] = BatchResult(task.imageUri, null, "已取消")
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.CANCELLED), onImageProgress)
                            val done = completedCount.incrementAndGet()
                            updateOverallProgress(done, totalCount, onProgress, imageUris, results)
                            return@launch
                        }

                        try {
                            // 阶段 1: 加载图片
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.LOADING, progress = 0.2f), onImageProgress)

                            val bitmap = loadBitmap(context, task.imageUri)
                            if (bitmap == null) {
                                results[taskIndex] = BatchResult(task.imageUri, null, "图片加载失败")
                                updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "图片加载失败"), onImageProgress)
                                val done = completedCount.incrementAndGet()
                                updateOverallProgress(done, totalCount, onProgress, imageUris, results)
                                return@launch
                            }

                            // 阶段 2: 应用预设
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.PROCESSING, progress = 0.5f), onImageProgress)

                            var processedBitmap = applyPreset(bitmap, params)

                            // 阶段 2.5: 应用水印
                            if (watermarkConfig != null) {
                                val watermarked = WatermarkRenderer.renderWatermark(processedBitmap, watermarkConfig)
                                if (watermarked !== processedBitmap) processedBitmap.recycle()
                                processedBitmap = watermarked
                            }

                            // 阶段 2.6: 按分辨率缩放 (UC-10)
                            val scaledBitmap = scaleBitmapToResolution(processedBitmap, task.resolution)
                            if (scaledBitmap !== processedBitmap) processedBitmap.recycle()

                            // 阶段 3: 保存图片（含格式+分辨率后缀）
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.SAVING, progress = 0.8f), onImageProgress)

                            val savedUri = saveProcessedImage(
                                scaledBitmap,
                                task.imageUri,
                                task.format,
                                task.resolution
                            )

                            scaledBitmap.recycle()
                            if (bitmap !== scaledBitmap && !bitmap.isRecycled) bitmap.recycle()

                            if (savedUri != null) {
                                results[taskIndex] = BatchResult(task.imageUri, savedUri, null)
                                updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.COMPLETED, progress = 1f), onImageProgress)
                                // UC-18: 每完成一个任务就持久化进度
                                saveBatchState(completedCount.get() + 1, totalCount, setOf(taskIndex))
                            } else {
                                results[taskIndex] = BatchResult(task.imageUri, null, "保存失败")
                                updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "保存失败"), onImageProgress)
                            }

                        } catch (e: OutOfMemoryError) {
                            Log.e(TAG, "处理图片[task=$taskIndex] OOM: ${e.message}")
                            System.gc()
                            results[taskIndex] = BatchResult(task.imageUri, null, "内存不足，已跳过此图")
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = "内存不足，已跳过此图"), onImageProgress)
                        } catch (e: Exception) {
                            Log.e(TAG, "处理图片[task=$taskIndex]失败: ${e.message}", e)
                            results[taskIndex] = BatchResult(task.imageUri, null, e.message ?: "未知错误")
                            updateImageProgress(task.imageIndex, ImageProgress(status = ImageStatus.FAILED, progress = 1f, error = e.message), onImageProgress)
                        }

                        val done = completedCount.incrementAndGet()
                        updateOverallProgress(done, totalCount, onProgress, imageUris, results)
                    }
                }
            }

            // 如果中途取消，填充剩余结果
            if (!isActive || isCancelled) {
                synchronized(lock) {
                    for (i in tasks.indices) {
                        if (results[i] == null) {
                            results[i] = BatchResult(tasks[i].imageUri, null, "已取消")
                            updateImageProgress(tasks[i].imageIndex, ImageProgress(status = ImageStatus.CANCELLED, progress = 1f), onImageProgress)
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
        // 批次完成后清除持久化状态
        clearBatchState()
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
     * 加载图片 - 流式解码避免大图全量读入内存 (MEM-01 OOM 防护)
     * 使用 ParcelFileDescriptor 直接解码文件描述符，配合 inSampleSize 降采样。
     */
    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { descriptor ->
                // 第一次解码：仅获取尺寸信息
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)

                // 计算采样率
                options.inSampleSize = calculateSampleSize(
                    options.outWidth, options.outHeight,
                    EXPORT_MAX_DIMENSION, EXPORT_MAX_DIMENSION
                )
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565

                // 第二次解码：复用同一文件描述符
                // 需要重新 seek 到开头
                descriptor.fileDescriptor.sync()
                BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "加载图片 OOM: ${e.message}")
            System.gc()
            null
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: ${e.message}", e)
            null
        }
    }

    /**
     * 安全浮点值：NaN / Infinite 替换为 [fallback]，否则 coerceIn
     */
    private fun safeFloat(value: Float, min: Float, max: Float, fallback: Float = 0f): Float {
        return if (value.isNaN() || value.isInfinite()) fallback else value.coerceIn(min, max)
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
            val saturation = safeFloat((params.saturation + 30) / 60f * 2f, 0f, 2f, 1f)
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(saturation)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }

        // 对比度
        if (params.contrast != 0) {
            val contrast = safeFloat((params.contrast + 30) / 60f * 2f, 0f, 2f, 1f)
            val cm = android.graphics.ColorMatrix()
            cm.setScale(contrast, contrast, contrast, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
        }

        // 色温
        if (params.colorTemp != 0) {
            val warmth = safeFloat(params.colorTemp / 30f * 0.3f, -0.3f, 0.3f, 0f)
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
            val toneScale = safeFloat(1f + params.tone / 30f * 0.3f, 0.5f, 2f, 1f)
            val cm = android.graphics.ColorMatrix()
            cm.setScale(toneScale, toneScale, toneScale, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
        }

        paint.colorFilter = null
        return result
    }

    /**
     * 保存处理后的图片 (UC-10 多格式/分辨率 + UC-18 原子写入 + UC-25 WebP 兼容)
     *
     * 原子写入策略：
     * - Android 10+: 使用 MediaStore IS_PENDING 标志，写入完成后才对外可见
     * - Android 9-: 先写入临时文件，成功后重命名为最终路径
     * - 任何写入失败时删除临时文件，不留半写文件
     *
     * @param resolution 导出分辨率（UC-10），默认 ORIGINAL
     */
    private fun saveProcessedImage(
        bitmap: Bitmap,
        originalUri: Uri,
        format: HasselbladEyeViewModel.ExportFormat,
        resolution: ExportResolution = ExportResolution.ORIGINAL
    ): Uri? {
        // UC-25: WebP 兼容性检查
        val resolvedFormat = resolveExportFormat(format)

        return try {
            // DB-03: 预检存储空间，避免 ENOSPC 导致部分写出文件残留
            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
            val availBytes = stat.availableBytes
            val estimatedSize = bitmap.byteCount.toLong()
            if (availBytes < estimatedSize + 10 * 1024 * 1024) {
                Log.e(TAG, "存储空间不足: 可用 ${availBytes / 1024 / 1024}MB, 需要约 ${estimatedSize / 1024 / 1024}MB")
                return null
            }

            val (compressFormat, extension) = when (resolvedFormat) {
                HasselbladEyeViewModel.ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG to "jpg"
                HasselbladEyeViewModel.ExportFormat.PNG -> Bitmap.CompressFormat.PNG to "png"
                HasselbladEyeViewModel.ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP to "webp"
                HasselbladEyeViewModel.ExportFormat.HEIF -> {
                    // resolveExportFormat 已将 HEIF 回退为 JPEG，此处不应到达
                    Bitmap.CompressFormat.JPEG to "jpg"
                }
            }
            val mimeType = when (resolvedFormat) {
                HasselbladEyeViewModel.ExportFormat.JPEG -> "image/jpeg"
                HasselbladEyeViewModel.ExportFormat.PNG -> "image/png"
                HasselbladEyeViewModel.ExportFormat.WEBP -> "image/webp"
                HasselbladEyeViewModel.ExportFormat.HEIF -> "image/jpeg"
            }

            // UC-10: 文件名中包含分辨率标识
            val resSuffix = when (resolution) {
                ExportResolution.ORIGINAL -> ""
                ExportResolution.FHD_1080P -> "_1080p"
                ExportResolution.UHD_4K -> "_4k"
            }
            val filename = "OMaster_Batch_${System.currentTimeMillis()}_${originalUri.lastPathSegment ?: "processed"}${resSuffix}.${extension}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // UC-18: Android 10+ 使用 IS_PENDING 实现原子写入
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OMaster/Batch")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null

                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(compressFormat, 95, out)
                        out.flush()
                    } ?: run {
                        // 写入失败，删除条目
                        context.contentResolver.delete(uri, null, null)
                        return null
                    }

                    // 写入成功，取消 IS_PENDING 使文件对外可见
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, updateValues, null, null)
                    uri
                } catch (e: Exception) {
                    // 写入失败，删除半写文件
                    Log.e(TAG, "原子写入失败，删除半写文件: ${e.message}", e)
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) { /* 忽略删除失败 */ }
                    null
                }
            } else {
                // UC-18: Android 9- 使用临时文件 + 重命名实现原子写入
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OMaster/Batch")
                if (!dir.exists()) dir.mkdirs()

                val finalFile = File(dir, filename)
                val tempFile = File(dir, "$filename.tmp")

                try {
                    // 先写入临时文件
                    FileOutputStream(tempFile).use { out ->
                        bitmap.compress(compressFormat, 95, out)
                        out.flush()
                    }

                    // 写入成功后原子重命名
                    if (!tempFile.renameTo(finalFile)) {
                        Log.e(TAG, "临时文件重命名失败: ${tempFile.absolutePath}")
                        tempFile.delete()
                        return null
                    }

                    Uri.fromFile(finalFile)
                } catch (e: Exception) {
                    // 写入失败，删除临时文件，不留半写文件
                    Log.e(TAG, "保存图片失败，删除临时文件: ${e.message}", e)
                    tempFile.delete()
                    null
                }
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "保存图片 IO 错误: ${e.message}", e)
            null
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

    // ---- UC-18: 批次状态持久化 ----
    // 用于 App 被杀死后恢复时跳过已完成的导出任务

    private val batchStateFile: File
        get() = File(context.cacheDir, "batch_export_state.properties")

    /**
     * 保存批次导出进度到本地文件 (UC-18)
     * @param completedCount 已完成的任务数
     * @param totalCount 总任务数
     * @param newCompletedIndices 本次新增已完成的任务索引集合
     */
    fun saveBatchState(completedCount: Int, totalCount: Int, newCompletedIndices: Set<Int>) {
        try {
            val file = batchStateFile
            val existingIndices = if (file.exists()) {
                file.readLines()
                    .filter { it.startsWith("completed.") }
                    .mapNotNull { it.substringAfter("completed.").trim().toIntOrNull() }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }
            existingIndices.addAll(newCompletedIndices)

            file.bufferedWriter().use { writer ->
                writer.write("totalCount=$totalCount\n")
                writer.write("completedCount=$completedCount\n")
                existingIndices.sorted().forEach { idx ->
                    writer.write("completed.$idx=true\n")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "保存批次状态失败: ${e.message}")
        }
    }

    /**
     * 恢复批次导出进度，返回已完成任务的索引集合 (UC-18)
     * 恢复时跳过这些已完成的导出任务，避免重复导出
     */
    fun restoreBatchState(): Set<Int> {
        return try {
            val file = batchStateFile
            if (!file.exists()) return emptySet()

            file.readLines()
                .filter { it.startsWith("completed.") }
                .mapNotNull { it.substringAfter("completed.").substringBefore("=").trim().toIntOrNull() }
                .toSet()
        } catch (e: Exception) {
            Log.w(TAG, "恢复批次状态失败: ${e.message}")
            emptySet()
        }
    }

    /**
     * 清除批次持久化状态（批次完成后调用）
     */
    private fun clearBatchState() {
        try {
            val file = batchStateFile
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "清除批次状态失败: ${e.message}")
        }
    }
}

/**
 * 导出分辨率选项
 */
enum class ExportResolution(val label: String, val maxWidth: Int, val maxHeight: Int) {
    ORIGINAL("原始分辨率", Int.MAX_VALUE, Int.MAX_VALUE),
    FHD_1080P("1080P", 1920, 1080),
    UHD_4K("4K", 3840, 2160)
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