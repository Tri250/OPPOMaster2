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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.data.lut.StyleLUTGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 风格 LUT 生成器 ViewModel
 *
 * 完整操作链路：
 * 1. 选择原图（拍照/相册）
 * 2. 选择参考风格图（拍照/相册）
 * 3. 设置色彩空间（Auto/Rec.709/Log）
 * 4. 调整迁移强度
 * 5. 生成 LUT（色彩迁移算法）
 * 6. 预览效果（Before/After 对比）
 * 7. 导出 .cube 文件（保存到 Download 目录）
 * 8. 应用到当前图片（跳转哈苏之眼/AI调色）
 */
class StyleLUTGeneratorViewModel : ViewModel() {

    companion object {
        private const val TAG = "StyleLUTGenVM"
    }

    // 原图
    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    // 参考风格图
    private val _referenceBitmap = MutableStateFlow<Bitmap?>(null)
    val referenceBitmap: StateFlow<Bitmap?> = _referenceBitmap.asStateFlow()

    // 色彩空间
    private val _colorSpace = MutableStateFlow(StyleLUTGenerator.ColorSpace.AUTO)
    val colorSpace: StateFlow<StyleLUTGenerator.ColorSpace> = _colorSpace.asStateFlow()

    // 检测到的色彩空间
    private val _detectedColorSpace = MutableStateFlow(StyleLUTGenerator.ColorSpace.REC709)
    val detectedColorSpace: StateFlow<StyleLUTGenerator.ColorSpace> = _detectedColorSpace.asStateFlow()

    // 迁移强度
    private val _strength = MutableStateFlow(1.0f)
    val strength: StateFlow<Float> = _strength.asStateFlow()

    // 生成状态
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // 生成进度
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // 生成结果
    private val _generationResult = MutableStateFlow<StyleLUTGenerator.GenerationResult?>(null)
    val generationResult: StateFlow<StyleLUTGenerator.GenerationResult?> = _generationResult.asStateFlow()

    // 预览 Bitmap（LUT 应用后的效果）
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 导出状态
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // 导出成功
    private val _exportSuccess = MutableStateFlow(false)
    val exportSuccess: StateFlow<Boolean> = _exportSuccess.asStateFlow()

    /**
     * 加载原图
     */
    fun loadSourceImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(context, uri, 2048)
                }
                _sourceBitmap.value = bitmap
                // 自动检测色彩空间
                if (_colorSpace.value == StyleLUTGenerator.ColorSpace.AUTO && bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                    _detectedColorSpace.value = StyleLUTGenerator.detectColorSpace(scaled)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load source image", e)
                _error.value = "加载原图失败：${e.message}"
            }
        }
    }

    /**
     * 加载参考风格图
     */
    fun loadReferenceImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(context, uri, 2048)
                }
                _referenceBitmap.value = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load reference image", e)
                _error.value = "加载参考图失败：${e.message}"
            }
        }
    }

    /**
     * 设置色彩空间
     */
    fun setColorSpace(colorSpace: StyleLUTGenerator.ColorSpace) {
        _colorSpace.value = colorSpace
    }

    /**
     * 设置迁移强度
     */
    fun setStrength(strength: Float) {
        _strength.value = strength.coerceIn(0f, 1f)
    }

    /**
     * 生成风格 LUT
     */
    fun generate() {
        val source = _sourceBitmap.value
        val reference = _referenceBitmap.value

        if (source == null || reference == null) {
            _error.value = "请先选择原图和参考风格图"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _progress.value = 0f
            _error.value = null

            try {
                _progress.value = 0.2f
                val result = withContext(Dispatchers.Default) {
                    StyleLUTGenerator.generate(
                        sourceBitmap = source,
                        referenceBitmap = reference,
                        sourceColorSpace = _colorSpace.value,
                        strength = _strength.value
                    )
                }

                _progress.value = 0.8f
                _generationResult.value = result
                _previewBitmap.value = result.previewBitmap

                _progress.value = 1f
            } catch (e: Exception) {
                Log.e(TAG, "LUT generation failed", e)
                _error.value = "生成失败：${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * 重新生成（调整强度后）
     */
    fun regenerate() {
        _generationResult.value = null
        _previewBitmap.value = null
        generate()
    }

    /**
     * 导出 .cube 文件到 Download 目录
     */
    fun exportCubeFile(context: Context, title: String = "OMaster_Style_LUT") {
        val result = _generationResult.value ?: return

        viewModelScope.launch {
            _isExporting.value = true
            _exportSuccess.value = false

            try {
                withContext(Dispatchers.IO) {
                    val cubeContent = StyleLUTGenerator.exportToCubeString(result.lutData, title)
                    val fileName = "${title.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]"), "_")}.cube"

                    // 保存到公共 Download 目录
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                            put(
                                MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS + "/OMaster/LUTs"
                            )
                        }
                        val uri = context.contentResolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { out ->
                                out.write(cubeContent.toByteArray())
                            }
                        }
                    } else {
                        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val lutDir = File(downloadDir, "OMaster/LUTs")
                        if (!lutDir.exists()) lutDir.mkdirs()
                        val file = File(lutDir, fileName)
                        FileOutputStream(file).use { out ->
                            out.write(cubeContent.toByteArray())
                        }
                    }

                    // 同时保存到应用私有目录（用于内部快速访问）
                    val privateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "OMaster/LUTs")
                    if (!privateDir.exists()) privateDir.mkdirs()
                    val privateFile = File(privateDir, fileName)
                    FileOutputStream(privateFile).use { out ->
                        out.write(cubeContent.toByteArray())
                    }
                }

                _exportSuccess.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _error.value = "导出失败：${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * 应用生成的 LUT 到哈苏之眼
     */
    fun applyToHasselblad(context: Context) {
        val result = _generationResult.value ?: return
        val lutManager = LUTManager.getInstance(context)
        // 将生成的 LUT 缓存到 LUTManager 并设为激活
        val tempId = "generated_${System.currentTimeMillis()}"
        lutManager.parseAndCache(tempId, result.lutData)
        lutManager.setActiveLUT(tempId)
        lutManager.setLUTStrength(_strength.value)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        _sourceBitmap.value?.recycle()
        _referenceBitmap.value?.recycle()
        _previewBitmap.value?.recycle()
        _sourceBitmap.value = null
        _referenceBitmap.value = null
        _previewBitmap.value = null
        _generationResult.value = null
        _strength.value = 1.0f
        _colorSpace.value = StyleLUTGenerator.ColorSpace.AUTO
        _error.value = null
        _exportSuccess.value = false
    }

    /**
     * 从 Uri 加载 Bitmap
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 先解码尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)

                // 计算采样率
                var sampleSize = 1
                while (options.outWidth / sampleSize > maxDimension ||
                       options.outHeight / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                // 解码图片
                context.contentResolver.openInputStream(uri)?.use { stream2 ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeStream(stream2, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        _sourceBitmap.value?.recycle()
        _referenceBitmap.value?.recycle()
        _previewBitmap.value?.recycle()
    }
}
