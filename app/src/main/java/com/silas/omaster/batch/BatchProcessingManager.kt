package com.silas.omaster.batch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 批量处理管理器
 * 旅行用户多图同款场景
 * 
 * 功能：
 * - 批量选择图片
 * - 应用相同预设/参数到所有图片
 * - 批量导出
 * - 处理进度追踪
 */
class BatchProcessingManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    // 批量处理任务状态
    sealed class BatchState {
        object Idle : BatchState()
        data class Selecting(val selectedCount: Int) : BatchState()
        data class Processing(
            val total: Int,
            val processed: Int,
            val currentItem: String
        ) : BatchState()
        data class Completed(
            val successCount: Int,
            val failedCount: Int,
            val outputPath: String
        ) : BatchState()
        data class Error(val message: String) : BatchState()
    }

    // 批量处理项
    data class BatchItem(
        val id: String,
        val uri: Uri,
        val name: String,
        val status: ItemStatus = ItemStatus.Pending,
        val outputPath: String? = null
    )

    enum class ItemStatus {
        Pending, Processing, Success, Failed
    }

    // 批量处理参数
    data class BatchParams(
        val saturation: Int = 0,
        val contrast: Int = 0,
        val brightness: Int = 0,
        val warmth: Int = 0,
        val sharpness: Int = 0,
        val clarity: Int = 0,
        val highlights: Int = 0,
        val shadows: Int = 0,
        val presetId: String? = null,
        val outputQuality: Int = 90,
        val outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    )

    // 选中的图片列表
    private val _selectedItems = MutableStateFlow<List<BatchItem>>(emptyList())
    val selectedItems: StateFlow<List<BatchItem>> = _selectedItems.asStateFlow()

    // 处理状态
    private val _processingState = MutableStateFlow<BatchState>(BatchState.Idle)
    val processingState: StateFlow<BatchState> = _processingState.asStateFlow()

    // 处理参数
    private val _batchParams = MutableStateFlow(BatchParams())
    val batchParams: StateFlow<BatchParams> = _batchParams.asStateFlow()

    // 最大选择数量
    val maxSelectionCount = 50

    /**
     * 添加图片到批量处理列表
     */
    fun addItems(uris: List<Uri>) {
        val current = _selectedItems.value.toMutableList()
        val newItems = uris
            .take(maxSelectionCount - current.size)
            .mapIndexed { index, uri ->
                BatchItem(
                    id = "${System.currentTimeMillis()}_$index",
                    uri = uri,
                    name = getFileName(uri)
                )
            }
        _selectedItems.value = current + newItems
        _processingState.value = BatchState.Selecting(_selectedItems.value.size)
    }

    /**
     * 移除单个图片
     */
    fun removeItem(itemId: String) {
        _selectedItems.value = _selectedItems.value.filter { it.id != itemId }
        _processingState.value = if (_selectedItems.value.isEmpty()) {
            BatchState.Idle
        } else {
            BatchState.Selecting(_selectedItems.value.size)
        }
    }

    /**
     * 清空所有选中图片
     */
    fun clearAll() {
        _selectedItems.value = emptyList()
        _processingState.value = BatchState.Idle
    }

    /**
     * 设置批量处理参数
     */
    fun setBatchParams(params: BatchParams) {
        _batchParams.value = params
    }

    /**
     * 应用预设到批量参数
     */
    fun applyPreset(presetId: String, params: Map<String, Int>) {
        _batchParams.value = BatchParams(
            saturation = params["saturation"] ?: 0,
            contrast = params["contrast"] ?: 0,
            brightness = params["brightness"] ?: 0,
            warmth = params["warmth"] ?: 0,
            sharpness = params["sharpness"] ?: 0,
            clarity = params["clarity"] ?: 0,
            highlights = params["highlights"] ?: 0,
            shadows = params["shadows"] ?: 0,
            presetId = presetId
        )
    }

    /**
     * 执行批量处理
     * 真实像素级处理
     */
    suspend fun processBatch(outputDir: File): BatchState = withContext(Dispatchers.Default) {
        val items = _selectedItems.value
        val params = _batchParams.value

        if (items.isEmpty()) {
            _processingState.value = BatchState.Error("未选择图片")
            return@withContext _processingState.value
        }

        // 确保输出目录存在
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        var successCount = 0
        var failedCount = 0

        items.forEachIndexed { index, item ->
            // 更新处理状态
            _processingState.value = BatchState.Processing(
                total = items.size,
                processed = index,
                currentItem = item.name
            )

            // 更新单项状态
            updateItemStatus(item.id, ItemStatus.Processing)

            try {
                // 加载图片
                val bitmap = loadBitmap(item.uri)
                if (bitmap == null) {
                    updateItemStatus(item.id, ItemStatus.Failed)
                    failedCount++
                    return@forEachIndexed
                }

                // 应用参数处理
                val processedBitmap = applyParams(bitmap, params)

                // 保存输出
                val outputFile = File(outputDir, "batch_${item.name}")
                saveBitmap(processedBitmap, outputFile, params.outputQuality, params.outputFormat)

                // 更新成功状态
                updateItemStatus(item.id, ItemStatus.Success, outputFile.absolutePath)
                successCount++

                // 回收 Bitmap
                if (bitmap != processedBitmap) {
                    bitmap.recycle()
                }
                processedBitmap.recycle()

            } catch (e: Exception) {
                updateItemStatus(item.id, ItemStatus.Failed)
                failedCount++
            }
        }

        // 完成状态
        val finalState = BatchState.Completed(
            successCount = successCount,
            failedCount = failedCount,
            outputPath = outputDir.absolutePath
        )
        _processingState.value = finalState
        finalState
    }

    /**
     * 加载 Bitmap
     */
    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream?.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 应用参数到 Bitmap
     * 真实像素级处理
     */
    private fun applyParams(bitmap: Bitmap, params: BatchParams): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // 预计算调整系数
        val saturationFactor = 1f + params.saturation / 100f
        val contrastFactor = 1f + params.contrast / 100f
        val brightnessOffset = params.brightness * 2.55f
        val warmthShift = params.warmth * 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = android.graphics.Color.red(pixel).toFloat()
                var g = android.graphics.Color.green(pixel).toFloat()
                var b = android.graphics.Color.blue(pixel).toFloat()
                val a = android.graphics.Color.alpha(pixel)

                // 亮度
                r += brightnessOffset
                g += brightnessOffset
                b += brightnessOffset

                // 对比度
                r = (r - 128) * contrastFactor + 128
                g = (g - 128) * contrastFactor + 128
                b = (b - 128) * contrastFactor + 128

                // 饱和度
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + (r - gray) * saturationFactor
                g = gray + (g - gray) * saturationFactor
                b = gray + (b - gray) * saturationFactor

                // 冷暖
                r += warmthShift
                b -= warmthShift

                // 高光/阴影
                val luminance = (r + g + b) / 3f
                if (luminance > 128) {
                    // 高光区域
                    val highlightFactor = 1f + params.highlights / 100f * ((luminance - 128) / 127f)
                    r *= highlightFactor
                    g *= highlightFactor
                    b *= highlightFactor
                } else {
                    // 阴影区域
                    val shadowFactor = 1f + params.shadows / 100f * ((128 - luminance) / 128f)
                    r *= shadowFactor
                    g *= shadowFactor
                    b *= shadowFactor
                }

                // 范围限制
                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                result.setPixel(x, y, android.graphics.Color.argb(a, r.toInt(), g.toInt(), b.toInt()))
            }
        }

        return result
    }

    /**
     * 保存 Bitmap 到文件
     */
    private fun saveBitmap(
        bitmap: Bitmap,
        file: File,
        quality: Int,
        format: Bitmap.CompressFormat
    ) {
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality, out)
        }
    }

    /**
     * 更新单项状态
     */
    private fun updateItemStatus(itemId: String, status: ItemStatus, outputPath: String? = null) {
        _selectedItems.value = _selectedItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(status = status, outputPath = outputPath)
            } else {
                item
            }
        }
    }

    /**
     * 获取文件名
     */
    private fun getFileName(uri: Uri): String {
        var name = "image_${System.currentTimeMillis()}"
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    /**
     * 获取选中数量
     */
    fun getSelectedCount(): Int = _selectedItems.value.size

    /**
     * 检查是否可以添加更多
     */
    fun canAddMore(): Boolean = _selectedItems.value.size < maxSelectionCount

    companion object {
        @Volatile
        private var instance: BatchProcessingManager? = null

        fun getInstance(context: Context): BatchProcessingManager {
            return instance ?: synchronized(this) {
                instance ?: BatchProcessingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
