package com.silas.omaster.mask

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.util.LogUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 蒙版 ViewModel
 * 为 UI 提供状态与操作入口
 */
class MaskViewModel(
    private val maskManager: MaskManager,
    private val aiMaskProvider: AIMaskProvider
) : ViewModel() {

    // 当前所有蒙版
    val masks: StateFlow<List<AdjustmentMask>> = maskManager.masks
    val selectedMaskId: StateFlow<String?> = maskManager.selectedMaskId

    // 编辑模式
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    // 当前选中的蒙版对象
    val selectedMask: AdjustmentMask?
        get() = maskManager.getSelectedMask()

    // 预览图
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    // ===== 操作 =====

    /**
     * 添加预设蒙版
     */
    fun addPreset(preset: AdjustmentMask) {
        maskManager.addFromPreset(preset)
    }

    /**
     * 添加自定义蒙版
     */
    fun addCustomMask(type: MaskType, name: String = "新蒙版") {
        val newMask = AdjustmentMask(
            name = name,
            type = type
        )
        maskManager.addMask(newMask)
    }

    /**
     * 删除蒙版
     */
    fun removeMask(maskId: String) {
        maskManager.removeMask(maskId)
    }

    /**
     * 更新蒙版
     */
    fun updateMask(mask: AdjustmentMask) {
        maskManager.updateMask(mask)
    }

    /**
     * 选中蒙版
     */
    fun selectMask(maskId: String?) {
        maskManager.selectMask(maskId)
    }

    /**
     * 切换启用
     */
    fun toggleEnabled(maskId: String) {
        maskManager.toggleMask(maskId)
    }

    /**
     * 复制蒙版
     */
    fun duplicateMask(maskId: String) {
        maskManager.duplicateMask(maskId)
    }

    /**
     * 重新排序
     */
    fun reorder(fromIndex: Int, toIndex: Int) {
        maskManager.reorderMasks(fromIndex, toIndex)
    }

    // ===== AI 蒙版 =====

    /**
     * 通过 AI 生成蒙版
     */
    fun generateAIMask(subject: AISubject, source: Bitmap? = null, name: String? = null) {
        val bitmap = source ?: _previewBitmap.value ?: return
        viewModelScope.launch {
            try {
                val mask = aiMaskProvider.createAIMask(
                    source = bitmap,
                    subject = subject,
                    name = name ?: "${subject.displayName}蒙版"
                )
                maskManager.addMask(mask)
            } catch (e: Exception) {
                LogUtil.logThrowable("Mask", e, "添加蒙版失败")
            }
        }
    }

    // ===== 渲染 =====

    /**
     * 应用所有蒙版到图像
     */
    fun applyToImage(
        source: Bitmap,
        baseParams: com.silas.omaster.renderer.RenderParameters
    ): Bitmap {
        return maskManager.applyToImage(source, baseParams)
    }

    /**
     * 预览图
     */
    fun setPreviewBitmap(bitmap: Bitmap?) {
        _previewBitmap.value = bitmap
    }

    /**
     * 进入编辑模式
     */
    fun enterEditMode() {
        _isEditing.value = true
    }

    /**
     * 退出编辑模式
     */
    fun exitEditMode() {
        _isEditing.value = false
    }

    /**
     * 清空所有蒙版
     */
    fun clearAll() {
        maskManager.clearAll()
    }

    override fun onCleared() {
        super.onCleared()
        maskManager.invalidateAllCache()
    }

    companion object {
        fun create(context: Context): MaskViewModel {
            return MaskViewModel(
                maskManager = MaskManager.getInstance(context),
                aiMaskProvider = AIMaskProvider.getInstance(context)
            )
        }
    }
}
