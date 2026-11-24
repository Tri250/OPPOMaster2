package com.silas.omaster.ui.frame

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.util.ColorExtractor
import com.silas.omaster.util.DominantColorResult
import com.silas.omaster.util.ExifReader
import com.silas.omaster.util.FrameRenderer
import com.silas.omaster.util.OutputRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FrameViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()

    private val _state = MutableStateFlow(FrameState())
    val state: StateFlow<FrameState> = _state.asStateFlow()

    private var extractJob: Job? = null
    private var renderJob: Job? = null

    fun loadImage(uri: Uri) {
        extractJob?.cancel()
        _state.value = _state.value.copy(
            imageUri = uri, isLoading = true, error = null,
            selectedColorIndex = 0
        )

        extractJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeSampledBitmap(uri) }
                val exifInfo = ExifReader.read(appContext, uri)
                val colors = ColorExtractor.extract(bitmap)

                _state.value = _state.value.copy(
                    sourceBitmap = bitmap,
                    colors = colors,
                    dateTime = exifInfo.dateTime,
                    isLoading = false
                )
                renderInternal()
            } catch (e: Exception) {
                android.util.Log.e("FrameViewModel", "Failed to load image", e)
                _state.value = _state.value.copy(isLoading = false, error = "图片加载失败，请重试")
            }
        }
    }

    fun updateTitle(title: String) {
        _state.value = _state.value.copy(dateTime = title)
        renderInternal()
    }

    fun toggleRoundedCorners(enabled: Boolean) {
        _state.value = _state.value.copy(useRoundedCorners = enabled)
        renderInternal()
    }

    fun selectRatio(ratio: OutputRatio) {
        _state.value = _state.value.copy(outputRatio = ratio)
        renderInternal()
    }

    fun selectColorIndex(index: Int) {
        val s = _state.value
        if (index < 0 || index >= (s.colors?.palette?.size ?: 1)) return
        _state.value = s.copy(selectedColorIndex = index)
        renderInternal()
    }

    private fun renderInternal() {
        val s = _state.value
        if (s.sourceBitmap == null || s.colors == null) return

        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val selectedColor = s.colors.palette.getOrElse(s.selectedColorIndex) { s.colors.dominant }
                val result = FrameRenderer.render(
                    FrameRenderer.Params(
                        source = s.sourceBitmap,
                        dominantColor = selectedColor,
                        textColor = s.colors.textColor,
                        title = s.dateTime ?: "",
                        useRoundedCorners = s.useRoundedCorners,
                        ratio = s.outputRatio
                    )
                )
                // 【v1.5.6 审计修复】旧位图回收与 Compose 绘制竞态：
                // _state 更新前 Compose 可能仍在绘制上一帧 renderedBitmap，
                // 此时 recycle 会触发 "Canvas: trying to use a recycled bitmap" 崩溃。
                // Android 8.0(API 26) 起 Bitmap 像素内存由 GC 统一管理，显式 recycle
                // 非必需；仅在 API 23-25（native 内存需手动释放）保留显式回收。
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    s.renderedBitmap?.recycle()
                }
                _state.value = _state.value.copy(renderedBitmap = result)
            } catch (e: Exception) {
                android.util.Log.e("FrameViewModel", "Failed to render", e)
                _state.value = _state.value.copy(error = "渲染失败，请重试")
            }
        }
    }

    private suspend fun decodeSampledBitmap(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { s -> BitmapFactory.decodeStream(s, null, options) }
        options.apply {
            inSampleSize = calculateInSampleSize(options, 1920, 1920)
            inJustDecodeBounds = false
        }
        appContext.contentResolver.openInputStream(uri)?.use { s ->
            BitmapFactory.decodeStream(s, null, options)
        } ?: throw IllegalStateException("无法读取图片")
    }

    private fun calculateInSampleSize(o: BitmapFactory.Options, rw: Int, rh: Int): Int {
        var s = 1
        while (o.outHeight / s > rh || o.outWidth / s > rw) s *= 2
        return s.coerceAtLeast(1)
    }

    override fun onCleared() {
        super.onCleared()
        extractJob?.cancel()
        renderJob?.cancel()
        _state.value.sourceBitmap?.recycle()
        _state.value.renderedBitmap?.recycle()
    }
}

data class FrameState(
    val imageUri: Uri? = null,
    val sourceBitmap: Bitmap? = null,
    val colors: DominantColorResult? = null,
    val renderedBitmap: Bitmap? = null,
    val dateTime: String? = null,
    val useRoundedCorners: Boolean = true,
    val outputRatio: OutputRatio = OutputRatio.FULL,
    val selectedColorIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class FrameViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FrameViewModel(application) as T
}
