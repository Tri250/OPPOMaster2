package com.silas.omaster.ui.watermark

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.watermark.WatermarkConfig
import com.silas.omaster.data.watermark.WatermarkTemplate
import com.silas.omaster.data.watermark.WatermarkTemplates
import com.silas.omaster.data.watermark.WatermarkType
import com.silas.omaster.infrastructure.utils.UndoRedoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 水印编辑 ViewModel
 *
 * 职责：
 * - 管理水印编辑状态（WatermarkConfig）
 * - 使用 UndoRedoManager 支持撤销/重做
 * - 持久化水印配置到 SettingsManager
 * - 提供模板加载
 */
class WatermarkViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val undoRedoManager = UndoRedoManager<WatermarkConfig>(maxHistory = 30)

    private val _currentConfig = MutableStateFlow(WatermarkConfig())
    val currentConfig: StateFlow<WatermarkConfig> = _currentConfig.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _templates = MutableStateFlow<List<WatermarkTemplate>>(emptyList())
    val templates: StateFlow<List<WatermarkTemplate>> = _templates.asStateFlow()

    init {
        // 加载已保存的水印配置
        loadSavedConfig()
        // 加载当前类型的模板
        loadTemplates(_currentConfig.value.type)
    }

    /**
     * 更新水印配置（推送新状态到撤销栈）
     */
    fun updateConfig(newConfig: WatermarkConfig) {
        undoRedoManager.pushState(_currentConfig.value)
        _currentConfig.value = newConfig
        updateUndoRedoState()
    }

    /**
     * 撤销
     */
    fun undo() {
        val previous = undoRedoManager.undo(_currentConfig.value) ?: return
        _currentConfig.value = previous
        updateUndoRedoState()
    }

    /**
     * 重做
     */
    fun redo() {
        val next = undoRedoManager.redo(_currentConfig.value) ?: return
        _currentConfig.value = next
        updateUndoRedoState()
    }

    /**
     * 保存水印配置
     */
    fun save() {
        val configs = settingsManager.loadWatermarkConfigs().toMutableList()
        // 替换同类型配置或追加
        val existingIndex = configs.indexOfFirst { it.type == _currentConfig.value.type }
        if (existingIndex >= 0) {
            configs[existingIndex] = _currentConfig.value
        } else {
            configs.add(_currentConfig.value)
        }
        settingsManager.saveWatermarkConfigs(configs)
    }

    /**
     * 加载模板列表
     */
    fun loadTemplates(type: WatermarkType) {
        _templates.value = WatermarkTemplates.getTemplatesForType(type)
    }

    /**
     * 应用模板
     */
    fun applyTemplate(template: WatermarkTemplate) {
        updateConfig(template.config)
    }

    /**
     * 加载已保存的配置
     */
    private fun loadSavedConfig() {
        val configs = settingsManager.loadWatermarkConfigs()
        val savedBrandConfig = configs.find { it.type == WatermarkType.BRAND }
        if (savedBrandConfig != null) {
            _currentConfig.value = savedBrandConfig
        }
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoRedoManager.canUndo()
        _canRedo.value = undoRedoManager.canRedo()
    }
}

class WatermarkViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val settingsManager = SettingsManager.getInstance(context)
        return WatermarkViewModel(settingsManager) as T
    }
}
