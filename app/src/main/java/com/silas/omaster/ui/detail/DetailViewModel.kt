package com.silas.omaster.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * 详情页 ViewModel
 * 管理预设详情和收藏状态
 *
 * 修复：
 * 1. 使用 Job 管理加载任务，快速切换时取消旧任务
 * 2. 添加加载状态标识，避免竞态条件
 */
class DetailViewModel(
    private val repository: PresetRepository
) : ViewModel() {

    // 当前预设
    private val _preset = MutableStateFlow<MasterPreset?>(null)
    val preset: StateFlow<MasterPreset?> = _preset.asStateFlow()

    // 收藏状态
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 当前预设 ID
    private var currentPresetId: String? = null

    // 用于管理加载任务的 Job
    private var loadJob: Job? = null

    // ===== 参数微调相关 =====
    // 原始参数（加载预设时记录，用于重置）
    private var originalParams: OriginalParams? = null

    // 微调偏移量（滑块值 = 原始值 + offset）
    private val _exposureOffset = MutableStateFlow(0f)
    val exposureOffset: StateFlow<Float> = _exposureOffset.asStateFlow()

    private val _colorTempOffset = MutableStateFlow(0f)
    val colorTempOffset: StateFlow<Float> = _colorTempOffset.asStateFlow()

    private val _contrastOffset = MutableStateFlow(0f)
    val contrastOffset: StateFlow<Float> = _contrastOffset.asStateFlow()

    private val _saturationOffset = MutableStateFlow(0f)
    val saturationOffset: StateFlow<Float> = _saturationOffset.asStateFlow()

    private val _sharpnessOffset = MutableStateFlow(0f)
    val sharpnessOffset: StateFlow<Float> = _sharpnessOffset.asStateFlow()

    private val _clarityOffset = MutableStateFlow(0f)
    val clarityOffset: StateFlow<Float> = _clarityOffset.asStateFlow()

    // 保存副本结果
    private val _saveCopyResult = MutableStateFlow<SaveCopyResult?>(null)
    val saveCopyResult: StateFlow<SaveCopyResult?> = _saveCopyResult.asStateFlow()

    // 重置完成事件
    private val _resetDone = MutableStateFlow(false)
    val resetDone: StateFlow<Boolean> = _resetDone.asStateFlow()

    /**
     * 加载预设数据
     * 修复：取消之前的加载任务，避免竞态条件
     */
    fun loadPreset(presetId: String) {
        // 如果正在加载同一个预设，跳过
        if (presetId == currentPresetId && _preset.value != null) {
            return
        }

        // 取消之前的加载任务
        loadJob?.cancel()
        currentPresetId = presetId

        android.util.Log.d("DetailViewModel", "Loading preset with id: $presetId")

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val presetData = repository.presets.value.find { it.id == presetId }?.toMasterPreset()
                // 检查是否仍然是当前要加载的预设（可能被取消了）
                if (presetId == currentPresetId) {
                    android.util.Log.d("DetailViewModel", "Loaded preset: ${presetData?.name}, id: ${presetData?.id}")
                    _preset.value = presetData
                    _isFavorite.value = presetData?.isFavorite ?: false
                    // 记录原始参数并重置偏移量
                    presetData?.let { storeOriginalParams(it) }
                    resetOffsets()
                }
            } catch (e: Exception) {
                android.util.Log.e("DetailViewModel", "Error loading preset: $presetId", e)
                if (presetId == currentPresetId) {
                    _preset.value = null
                    _isFavorite.value = false
                }
            } finally {
                if (presetId == currentPresetId) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val id = currentPresetId ?: return
        viewModelScope.launch {
            try {
                repository.toggleFavorite(id)
                val isNowFavorite = repository.isFavorite(id)
                _isFavorite.value = isNowFavorite
                // 更新预设数据
                _preset.value = _preset.value?.copy(isFavorite = isNowFavorite)
            } catch (e: Exception) {
                android.util.Log.e("DetailViewModel", "Error toggling favorite: $id", e)
            }
        }
    }

    // ===== 参数微调方法 =====

    /**
     * 记录预设的原始参数值
     */
    private fun storeOriginalParams(preset: MasterPreset) {
        originalParams = OriginalParams(
            exposure = parseExposureFloat(preset.exposureCompensation),
            colorTemp = preset.colorTemperature?.toFloat() ?: 0f,
            contrast = preset.tone?.toFloat() ?: 0f,
            saturation = preset.saturation?.toFloat() ?: 0f,
            sharpness = preset.sharpness?.toFloat() ?: 0f,
            clarity = 0f
        )
    }

    /**
     * 解析曝光补偿字符串为 Float（如 "-1.0" -> -1.0f, "+0.7" -> 0.7f）
     */
    private fun parseExposureFloat(value: String?): Float {
        if (value.isNullOrBlank()) return 0f
        return value.replace("+", "").toFloatOrNull() ?: 0f
    }

    /**
     * 重置所有偏移量为0
     */
    private fun resetOffsets() {
        _exposureOffset.value = 0f
        _colorTempOffset.value = 0f
        _contrastOffset.value = 0f
        _saturationOffset.value = 0f
        _sharpnessOffset.value = 0f
        _clarityOffset.value = 0f
    }

    fun updateExposure(value: Float) { _exposureOffset.value = value }
    fun updateColorTemp(value: Float) { _colorTempOffset.value = value }
    fun updateContrast(value: Float) { _contrastOffset.value = value }
    fun updateSaturation(value: Float) { _saturationOffset.value = value }
    fun updateSharpness(value: Float) { _sharpnessOffset.value = value }
    fun updateClarity(value: Float) { _clarityOffset.value = value }

    /**
     * PR-05: 重置所有参数到原始预设值
     */
    fun resetParams() {
        resetOffsets()
        _resetDone.value = true
    }

    /**
     * 消费重置完成事件
     */
    fun consumeResetDone() {
        _resetDone.value = false
    }

    /**
     * PR-04: 保存副本 - 创建一个新预设，应用当前微调后的参数
     */
    fun saveAsCopy() {
        val id = currentPresetId ?: return
        val original = _preset.value ?: return
        val orig = originalParams ?: return

        viewModelScope.launch {
            try {
                // 计算微调后的参数值
                val adjustedTone = (orig.contrast + _contrastOffset.value).toInt()
                    .coerceIn(-100, 100)
                val adjustedSaturation = (orig.saturation + _saturationOffset.value).toInt()
                    .coerceIn(-100, 100)
                val adjustedColorTemp = (orig.colorTemp + _colorTempOffset.value).toInt()
                    .coerceIn(2000, 8000)
                val adjustedExposure = orig.exposure + _exposureOffset.value
                val adjustedSharpness = (orig.sharpness + _sharpnessOffset.value).toInt()
                    .coerceIn(0, 100)

                val newPreset = repository.duplicatePresetWithParams(
                    presetId = id,
                    tone = adjustedTone,
                    saturation = adjustedSaturation,
                    colorTemperature = adjustedColorTemp,
                    exposureCompensation = formatExposure(adjustedExposure),
                    sharpness = adjustedSharpness
                )
                if (newPreset != null) {
                    _saveCopyResult.value = SaveCopyResult.Success
                } else {
                    _saveCopyResult.value = SaveCopyResult.Failed("预设不存在")
                }
            } catch (e: Exception) {
                android.util.Log.e("DetailViewModel", "Error saving copy: $id", e)
                _saveCopyResult.value = SaveCopyResult.Failed(e.message ?: "保存失败")
            }
        }
    }

    /**
     * 消费保存副本结果
     */
    fun consumeSaveCopyResult() {
        _saveCopyResult.value = null
    }

    /**
     * 格式化曝光值为字符串
     */
    private fun formatExposure(value: Float): String {
        return if (value >= 0) "+${String.format("%.1f", value)}"
        else String.format("%.1f", value)
    }

    /**
     * 复制预设
     */
    fun duplicatePreset() {
        val id = currentPresetId ?: return
        viewModelScope.launch {
            try {
                repository.duplicatePreset(id)
            } catch (e: Exception) {
                android.util.Log.e("DetailViewModel", "Error duplicating preset: $id", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 清理时取消加载任务
        loadJob?.cancel()
    }
}

/**
 * DetailViewModel 工厂
 */
class DetailViewModelFactory(
    private val repository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            return DetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * 原始参数记录（加载预设时快照）
 */
private data class OriginalParams(
    val exposure: Float,
    val colorTemp: Float,
    val contrast: Float,
    val saturation: Float,
    val sharpness: Float,
    val clarity: Float
)

/**
 * 保存副本结果
 */
sealed class SaveCopyResult {
    object Success : SaveCopyResult()
    data class Failed(val message: String) : SaveCopyResult()
}
