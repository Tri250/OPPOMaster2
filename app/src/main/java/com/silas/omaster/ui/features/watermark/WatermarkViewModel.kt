package com.silas.omaster.ui.features.watermark

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.watermark.WatermarkRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 水印模块 ViewModel
 */
class WatermarkViewModel(
    private val repository: WatermarkRepository
) : ViewModel() {

    val watermarks: StateFlow<List<com.silas.omaster.data.watermark.WatermarkConfig>> = repository.watermarks
    val selectedWatermarkId: StateFlow<String?> = repository.selectedWatermarkId

    fun createBrandWatermark(name: String, text: String, color: String) {
        repository.createBrandWatermark(name, text, color)
    }

    fun createMasterMarkWatermark(name: String, signatureUri: Uri) {
        repository.createMasterMarkWatermark(name, signatureUri)
    }

    fun createXpanWatermark(
        name: String,
        topRatio: Float,
        bottomRatio: Float,
        topText: String,
        bottomText: String
    ) {
        repository.createXpanWatermark(name, topRatio, bottomRatio, topText, bottomText)
    }

    fun selectWatermark(id: String) {
        repository.selectWatermark(id)
    }

    fun deleteWatermark(id: String) {
        repository.deleteWatermark(id)
    }

    companion object {
        fun createFactory(context: android.content.Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WatermarkViewModel(WatermarkRepository.getInstance(context)) as T
                }
            }
        }
    }
}
