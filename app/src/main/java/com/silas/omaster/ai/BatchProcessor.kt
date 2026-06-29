package com.silas.omaster.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.silas.omaster.renderer.CPURenderer
import com.silas.omaster.renderer.GPURenderManager
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.renderer.RenderQuality
import com.silas.omaster.renderer.RenderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * 批量处理器
 *
 * 支持将同一组 RenderParameters 应用到多张图片
 * 提供进度追踪和错误处理
 */
class BatchProcessor(context: Context) {

    data class BatchProgress(
        val total: Int,
        val completed: Int,
        val failed: Int,
        val currentUri: Uri?,
        val isRunning: Boolean
    )

    companion object {
        private const val TAG = "BatchProcessor"
        private const val MAX_BITMAP_DIMENSION = 4096
    }

    private val appContext = context.applicationContext
    private var gpuRenderManager: GPURenderManager? = null

    private val _progress = MutableStateFlow(BatchProgress(0, 0, 0, null, false))
    val progress: StateFlow<BatchProgress> = _progress

    @Volatile
    private var cancelled = false

    /**
     * 初始化 GPU 渲染管理器
     */
    private suspend fun ensureGpuInitialized() {
        if (gpuRenderManager == null) {
            gpuRenderManager = GPURenderManager.acquire(appContext)
            if (!gpuRenderManager!!.isInitialized.value) {
                gpuRenderManager!!.initialize()
            }
        }
    }

    /**
     * 批量处理图片列表
     *
     * @param imageUris 待处理的图片 URI 列表
     * @param params 要应用的渲染参数
     * @param onImageSaved 每张图片保存后的回调 (uri, success)
     * @return 成功处理的图片数量
     */
    suspend fun processBatch(
        imageUris: List<Uri>,
        params: RenderParameters,
        onImageSaved: ((Uri, Boolean) -> Unit)? = null
    ): Int = withContext(Dispatchers.Default) {
        cancelled = false
        _progress.value = BatchProgress(imageUris.size, 0, 0, null, true)

        var successCount = 0
        var failCount = 0

        // 初始化 GPU
        try {
            ensureGpuInitialized()
        } catch (e: Exception) {
            Log.w(TAG, "GPU init failed, will use CPU fallback", e)
        }

        for (uri in imageUris) {
            if (!isActive || cancelled) break

            _progress.value = _progress.value.copy(currentUri = uri)

            try {
                // 1. 加载 Bitmap
                val sourceBitmap = loadBitmap(uri) ?: run {
                    failCount++
                    _progress.value = _progress.value.copy(
                        failed = failCount,
                        completed = successCount + failCount
                    )
                    onImageSaved?.invoke(uri, false)
                    continue
                }

                // 2. 渲染处理
                val renderedBitmap = renderBitmap(sourceBitmap, params)

                // 回收源 bitmap
                sourceBitmap.recycle()

                if (renderedBitmap == null) {
                    failCount++
                    _progress.value = _progress.value.copy(
                        failed = failCount,
                        completed = successCount + failCount
                    )
                    onImageSaved?.invoke(uri, false)
                    continue
                }

                // 3. 保存到相册
                val savedUri = saveToGallery(renderedBitmap, "OMaster_Batch_${System.currentTimeMillis()}")

                // 回收渲染结果
                renderedBitmap.recycle()

                if (savedUri != null) {
                    successCount++
                } else {
                    failCount++
                }

                _progress.value = _progress.value.copy(
                    completed = successCount + failCount,
                    failed = failCount
                )
                onImageSaved?.invoke(uri, savedUri != null)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image: $uri", e)
                failCount++
                _progress.value = _progress.value.copy(
                    failed = failCount,
                    completed = successCount + failCount
                )
                onImageSaved?.invoke(uri, false)
            }
        }

        _progress.value = _progress.value.copy(isRunning = false, currentUri = null)
        successCount
    }

    /**
     * 取消批量处理
     */
    fun cancel() {
        cancelled = true
    }

    /**
     * 释放资源
     */
    fun release() {
        gpuRenderManager?.release()
        gpuRenderManager = null
    }

    /**
     * 渲染 Bitmap：优先 GPU，回退 CPU
     */
    private suspend fun renderBitmap(source: Bitmap, params: RenderParameters): Bitmap? {
        val gpu = gpuRenderManager
        if (gpu != null && gpu.isInitialized.value && gpu.isGpuAvailable.value) {
            val result = gpu.renderSync(source, params, RenderQuality.HIGH)
            when (result) {
                is RenderResult.Success -> {
                    if (result.outputBitmap != null) return result.outputBitmap
                }
                is RenderResult.FallbackToCPU -> return result.outputBitmap
                is RenderResult.Error -> {
                    Log.w(TAG, "GPU render failed: ${result.message}, falling back to CPU")
                }
            }
        }

        // CPU 回退
        return try {
            val cpuRenderer = CPURenderer()
            cpuRenderer.render(source, params)
        } catch (e: Exception) {
            Log.e(TAG, "CPU render failed", e)
            null
        }
    }

    /**
     * 加载 Bitmap（带降采样）
     */
    private suspend fun loadBitmap(uri: Uri, maxDimension: Int = MAX_BITMAP_DIMENSION): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri) ?: return@withContext null

                // 先读取尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                // 计算降采样倍数
                val input = appContext.contentResolver.openInputStream(uri) ?: return@withContext null
                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxDimension)

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
                input.close()
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap from $uri", e)
                null
            }
        }

    /**
     * 计算降采样倍数
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= maxDimension && height <= maxDimension) return 1

        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension ||
               height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * 保存到系统相册
     */
    private suspend fun saveToGallery(bitmap: Bitmap, tag: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val filename = "${tag}.jpg"
            val mimeType = "image/jpeg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OMaster")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = appContext.contentResolver.insert(collection, values) ?: return@withContext null

            appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // 清除 IS_PENDING 标记
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                appContext.contentResolver.update(uri, values, null, null)
            }

            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to gallery", e)
            null
        }
    }
}
