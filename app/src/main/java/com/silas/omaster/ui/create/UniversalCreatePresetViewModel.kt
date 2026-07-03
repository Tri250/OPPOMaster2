package com.silas.omaster.ui.create

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.data.xmp.XmpParser
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetSection
import com.silas.omaster.infrastructure.utils.UndoRedoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 通用预设编辑器 ViewModel
 * 支持基于 sections 的灵活配置
 */
class UniversalCreatePresetViewModel(
    private val context: Context,
    private val repository: PresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversalPresetUiState())
    val uiState: StateFlow<UniversalPresetUiState> = _uiState.asStateFlow()
    
    private var isLoaded = false
    private var editingPresetId: String? = null

    // 撤销/重做管理器
    private val undoRedoManager = UndoRedoManager<UniversalPresetUiState>(maxHistory = 30)
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun updateStateWithUndo(newState: UniversalPresetUiState) {
        undoRedoManager.pushState(_uiState.value)
        _uiState.value = newState
        _canUndo.value = undoRedoManager.canUndo()
        _canRedo.value = undoRedoManager.canRedo()
    }

    fun undo() {
        val currentState = _uiState.value
        undoRedoManager.undo(currentState)?.let { prevState ->
            _uiState.value = prevState
            _canUndo.value = undoRedoManager.canUndo()
            _canRedo.value = undoRedoManager.canRedo()
        }
    }

    fun redo() {
        val currentState = _uiState.value
        undoRedoManager.redo(currentState)?.let { nextState ->
            _uiState.value = nextState
            _canUndo.value = undoRedoManager.canUndo()
            _canRedo.value = undoRedoManager.canRedo()
        }
    }

    fun clearUndoHistory() {
        undoRedoManager.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    // 加载模版或者现有预设
    fun loadTemplate(presetId: String?) {
        if (isLoaded) return
        isLoaded = true
        editingPresetId = null // Ensure not in edit mode
        
        if (presetId == null) {
            // 从零开始
            _uiState.value = UniversalPresetUiState()
            clearUndoHistory()
            return
        }

        viewModelScope.launch {
            val preset = repository.presets.value.find { it.id == presetId }?.toMasterPreset()
            if (preset != null) {
                // 如果是旧数据结构，转换为新结构
                val sections = if (preset.sections.isNullOrEmpty()) {
                    convertOldPresetToSections(preset)
                } else {
                    preset.sections
                }

                _uiState.value = UniversalPresetUiState(
                    name = if (preset.isCustom) preset.name else "${preset.name} (Copy)",
                    sections = sections,
                    // Template mode: require new image
                    imageUri = null,
                    isEditMode = false
                )
            }
        }
    }

    // Load preset for editing
    fun loadPresetForEdit(presetId: String) {
        if (isLoaded) return
        isLoaded = true
        editingPresetId = presetId
        clearUndoHistory()
        
        viewModelScope.launch {
            val preset = repository.presets.value.find { it.id == presetId }?.toMasterPreset()
            if (preset != null) {
                val sections = if (preset.sections.isNullOrEmpty()) {
                    convertOldPresetToSections(preset)
                } else {
                    preset.sections
                }

                _uiState.value = UniversalPresetUiState(
                    name = preset.name,
                    sections = sections,
                    imageUri = null, // Will use originalCoverPath
                    originalCoverPath = preset.coverPath,
                    isEditMode = true
                )
            }
        }
    }

    fun updateName(name: String) {
        updateStateWithUndo(_uiState.value.copy(name = name))
    }

    fun updateImageUri(uri: Uri?) {
        updateStateWithUndo(_uiState.value.copy(imageUri = uri))
    }

    fun addSection(title: String) {
        val newSection = PresetSection(title = title, items = emptyList())
        val currentSections = _uiState.value.sections.toMutableList()
        currentSections.add(newSection)
        updateStateWithUndo(_uiState.value.copy(sections = currentSections))
    }

    fun removeSection(index: Int) {
        val currentSections = _uiState.value.sections.toMutableList()
        if (index in currentSections.indices) {
            currentSections.removeAt(index)
            updateStateWithUndo(_uiState.value.copy(sections = currentSections))
        }
    }

    fun addItemToSection(sectionIndex: Int, item: PresetItem) {
        val currentSections = _uiState.value.sections.toMutableList()
        if (sectionIndex in currentSections.indices) {
            val section = currentSections[sectionIndex]
            val newItems = section.items.toMutableList()
            newItems.add(item)
            currentSections[sectionIndex] = section.copy(items = newItems)
            updateStateWithUndo(_uiState.value.copy(sections = currentSections))
        }
    }

    fun removeItemFromSection(sectionIndex: Int, itemIndex: Int) {
        val currentSections = _uiState.value.sections.toMutableList()
        if (sectionIndex in currentSections.indices) {
            val section = currentSections[sectionIndex]
            val newItems = section.items.toMutableList()
            if (itemIndex in newItems.indices) {
                newItems.removeAt(itemIndex)
                currentSections[sectionIndex] = section.copy(items = newItems)
                updateStateWithUndo(_uiState.value.copy(sections = currentSections))
            }
        }
    }
    
    fun updateItemInSection(sectionIndex: Int, itemIndex: Int, newItem: PresetItem) {
        val currentSections = _uiState.value.sections.toMutableList()
        if (sectionIndex in currentSections.indices) {
            val section = currentSections[sectionIndex]
            val newItems = section.items.toMutableList()
            if (itemIndex in newItems.indices) {
                newItems[itemIndex] = newItem
                currentSections[sectionIndex] = section.copy(items = newItems)
                updateStateWithUndo(_uiState.value.copy(sections = currentSections))
            }
        }
    }

    suspend fun savePreset(): Boolean {
        val state = _uiState.value
        if (state.name.isBlank()) return false

        // Validation:
        // - Create mode: must have imageUri
        // - Edit mode: must have imageUri OR originalCoverPath
        if (state.imageUri == null && state.originalCoverPath == null) return false

        return try {
            val coverPath = if (state.imageUri != null) {
                saveImageToInternalStorage(state.imageUri)
            } else {
                state.originalCoverPath ?: return false
            }

            val presetId = editingPresetId
            if (presetId != null) {
                repository.updateCustomPreset(
                    presetId,
                    mapOf(
                        "name" to state.name,
                        "coverPath" to coverPath,
                        "params" to sectionsToParams(state.sections),
                        "sections" to state.sections
                    )
                )
            } else {
                repository.createCustomPreset(
                    name = state.name,
                    params = sectionsToParams(state.sections),
                    coverPath = coverPath,
                    sections = state.sections
                )
            }
            true
        } catch (e: Exception) {
            Log.e("CreatePresetVM", "savePreset failed", e)
            false
        }
    }

    /**
     * 双向映射：资源 ID 后缀 / 中文标签 / 英文标签 -> 参数键名
     * 优先匹配 @string/ 前缀去除后的资源 ID（如 param_saturation），
     * 再匹配本地化标签（中文/英文），确保多语言环境下均可正确映射。
     */
    private val labelToParamKey: Map<String, String> = mapOf(
        // 资源 ID 后缀（@string/param_xxx 去除前缀后）
        "param_saturation" to "saturation",
        "param_tone_curve" to "contrast",
        "param_warm_cool" to "warmth",
        "param_sharpness" to "sharpness",
        "param_cyan_magenta" to "cyan_magenta",
        "param_color_temp" to "color_temperature",
        "param_tone" to "color_hue",
        "param_exposure" to "exposure_compensation",
        "param_iso" to "iso",
        "param_shutter" to "shutter_speed",
        "param_aperture" to "aperture",
        "param_white_balance" to "white_balance",
        // 中文标签
        "饱和度" to "saturation",
        "影调" to "contrast",
        "冷暖" to "warmth",
        "锐度" to "sharpness",
        "青品" to "cyan_magenta",
        "色温" to "color_temperature",
        "色调" to "color_hue",
        "曝光补偿" to "exposure_compensation",
        "快门" to "shutter_speed",
        "光圈" to "aperture",
        "白平衡" to "white_balance",
        // 英文标签
        "saturation" to "saturation",
        "contrast" to "contrast",
        "warmth" to "warmth",
        "sharpness" to "sharpness",
        "cyan_magenta" to "cyan_magenta",
        "color_temperature" to "color_temperature",
        "color_hue" to "color_hue",
        "exposure_compensation" to "exposure_compensation",
        "iso" to "iso",
        "shutter_speed" to "shutter_speed",
        "aperture" to "aperture",
        "white_balance" to "white_balance"
    )

    private fun sectionsToParams(sections: List<PresetSection>): Map<String, Int> {
        val params = mutableMapOf<String, Int>()
        for (section in sections) {
            for (item in section.items) {
                // 优先尝试去除 @string/ 前缀后的资源 ID 匹配
                val labelKey = item.label.removePrefix("@string/").trim()
                val key = labelToParamKey[labelKey] ?: labelKey
                item.value.toIntOrNull()?.let { params[key] = it }
            }
        }
        return params
    }

    private fun convertOldPresetToSections(preset: MasterPreset): List<PresetSection> {
        val items = mutableListOf<PresetItem>()
        
        // 尝试从旧字段提取数据
        preset.filter?.let { items.add(PresetItem("滤镜", it, 2)) }
        preset.softLight?.let { items.add(PresetItem("柔光", it, 1)) }
        preset.tone?.let { items.add(PresetItem("影调", it.toString(), 1)) }
        preset.saturation?.let { items.add(PresetItem("饱和度", it.toString(), 1)) }
        preset.warmCool?.let { items.add(PresetItem("冷暖", it.toString(), 1)) }
        preset.cyanMagenta?.let { items.add(PresetItem("青品", it.toString(), 1)) }
        preset.sharpness?.let { items.add(PresetItem("锐度", it.toString(), 1)) }
        preset.vignette?.let { items.add(PresetItem("暗角", it, 2)) }
        
        // Pro 模式参数
        preset.exposureCompensation?.let { items.add(PresetItem("曝光补偿", it, 1)) }
        preset.colorTemperature?.let { items.add(PresetItem("色温", it.toString(), 1)) }
        preset.colorHue?.let { items.add(PresetItem("色调", it.toString(), 1)) }
        
        return listOf(PresetSection("参数配置", items))
    }

    @Throws(IOException::class)
    private fun saveImageToInternalStorage(uri: Uri): String {
        val fileName = "custom_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, "presets/$fileName")
        file.parentFile?.mkdirs()
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("无法打开图片文件")
        
        return "presets/$fileName"
    }

    /**
     * 从 XMP 文件导入参数
     *
     * @param uri XMP 文件的 Uri
     * @return true 表示导入成功，false 表示导入失败
     */
    fun importFromXmp(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("无法打开 XMP 文件")

            val result = inputStream.use { XmpParser.parse(it) }

            when (result) {
                is XmpParser.XmpParseResult.Success -> {
                    val currentSections = _uiState.value.sections.toMutableList()
                    currentSections.addAll(result.sections)

                    // 如果名称为空，尝试用相机型号命名
                    val name = _uiState.value.name.ifBlank {
                        result.cameraModel ?: "XMP 导入预设"
                    }

                    updateStateWithUndo(_uiState.value.copy(
                        name = name,
                        sections = currentSections,
                        xmpImportError = null,
                        xmpImportSuccess = true
                    ))
                    // 自动清除成功标记
                    true
                }
                is XmpParser.XmpParseResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        xmpImportError = result.errorMessage,
                        xmpImportSuccess = false
                    )
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("CreatePresetVM", "importFromXmp failed", e)
            _uiState.value = _uiState.value.copy(
                xmpImportError = e.message ?: "解析失败",
                xmpImportSuccess = false
            )
            false
        }
    }

    /**
     * 清除 XMP 导入状态
     */
    fun clearXmpImportState() {
        _uiState.value = _uiState.value.copy(
            xmpImportError = null,
            xmpImportSuccess = false
        )
    }
}

data class UniversalPresetUiState(
    val name: String = "",
    val imageUri: Uri? = null,
    val sections: List<PresetSection> = emptyList(),
    val originalCoverPath: String? = null,
    val isEditMode: Boolean = false,
    val xmpImportError: String? = null,
    val xmpImportSuccess: Boolean = false
)

class UniversalCreatePresetViewModelFactory(
    private val context: Context,
    private val repository: PresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UniversalCreatePresetViewModel::class.java)) {
            return UniversalCreatePresetViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
