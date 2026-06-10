package com.silas.omaster.mask

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 蒙版管理器
 *
 * 功能：
 * - 维护当前编辑会话的所有蒙版
 * - 蒙版 CRUD（增/删/改/查）
 * - 蒙版导入/导出
 * - 蒙版缓存（生成的 Bitmap 缓存）
 *
 * 与 ParamAdjustmentManager 集成：每个蒙版有独立的 localParams
 */
class MaskManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    // 当前会话的蒙版列表
    private val _masks = MutableStateFlow<List<AdjustmentMask>>(emptyList())
    val masks: StateFlow<List<AdjustmentMask>> = _masks.asStateFlow()

    // 当前选中的蒙版 ID
    private val _selectedMaskId = MutableStateFlow<String?>(null)
    val selectedMaskId: StateFlow<String?> = _selectedMaskId.asStateFlow()

    // 蒙版生成器 & 渲染器
    private val generator = MaskGenerator
    private val renderer = MaskRenderer()

    // Bitmap 缓存：maskId → bitmap
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    // ===== CRUD =====

    /**
     * 添加新蒙版
     */
    fun addMask(mask: AdjustmentMask): String {
        val newList = _masks.value + mask
        _masks.value = newList
        _selectedMaskId.value = mask.id
        invalidateCache(mask.id)
        return mask.id
    }

    /**
     * 删除蒙版
     */
    fun removeMask(maskId: String) {
        val newList = _masks.value.filter { it.id != maskId }
        _masks.value = newList
        if (_selectedMaskId.value == maskId) {
            _selectedMaskId.value = newList.firstOrNull()?.id
        }
        releaseCache(maskId)
    }

    /**
     * 更新蒙版
     */
    fun updateMask(mask: AdjustmentMask) {
        val updated = mask.copy(updatedAt = System.currentTimeMillis())
        val newList = _masks.value.map { if (it.id == mask.id) updated else it }
        _masks.value = newList
        invalidateCache(mask.id)
    }

    /**
     * 选中蒙版
     */
    fun selectMask(maskId: String?) {
        _selectedMaskId.value = maskId
    }

    /**
     * 获取当前选中的蒙版
     */
    fun getSelectedMask(): AdjustmentMask? {
        val id = _selectedMaskId.value ?: return null
        return _masks.value.find { it.id == id }
    }

    /**
     * 切换蒙版启用状态
     */
    fun toggleMask(maskId: String) {
        val mask = _masks.value.find { it.id == maskId } ?: return
        updateMask(mask.copy(enabled = !mask.enabled))
    }

    /**
     * 复制蒙版
     */
    fun duplicateMask(maskId: String): String? {
        val mask = _masks.value.find { it.id == maskId } ?: return null
        val newMask = mask.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${mask.name} 副本",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return addMask(newMask)
    }

    /**
     * 重排蒙版（拖拽改变顺序）
     */
    fun reorderMasks(fromIndex: Int, toIndex: Int) {
        val list = _masks.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _masks.value = list
    }

    // ===== 缓存管理 =====

    /**
     * 获取蒙版 Bitmap（自动生成/缓存）
     * @param width 输出宽度
     * @param height 输出高度
     * @param maskId 蒙版 ID
     */
    fun getMaskBitmap(width: Int, height: Int, maskId: String): Bitmap? {
        val mask = _masks.value.find { it.id == maskId } ?: return null
        if (!mask.enabled) {
            return null
        }
        bitmapCache[maskId]?.let { cached ->
            if (cached.width == width && cached.height == height) {
                return cached
            }
        }
        val bitmap = generator.generate(width, height, mask)
        bitmapCache[maskId] = bitmap
        return bitmap
    }

    /**
     * 失效指定蒙版的缓存
     */
    fun invalidateCache(maskId: String) {
        bitmapCache.remove(maskId)
    }

    /**
     * 失效所有缓存
     */
    fun invalidateAllCache() {
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
    }

    /**
     * 释放指定蒙版的缓存
     */
    private fun releaseCache(maskId: String) {
        bitmapCache.remove(maskId)?.recycle()
    }

    /**
     * 清理资源
     */
    fun release() {
        invalidateAllCache()
        _masks.value = emptyList()
        _selectedMaskId.value = null
    }

    // ===== 导入/导出 =====

    /**
     * 导出为 JSON
     */
    fun exportToJson(): String {
        val masks = _masks.value
        val jsonArray = masks.joinToString(",") { mask ->
            """{"id":"${mask.id}","name":"${mask.name}","type":"${mask.type}","enabled":${mask.enabled},"opacity":${mask.opacity},"blendMode":"${mask.blendMode}","localParams":{}}"""
        }
        return """{"version":1,"masks":[$jsonArray]}"""
    }

    /**
     * 从预设添加
     */
    fun addFromPreset(preset: AdjustmentMask): String {
        val newMask = preset.copy(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return addMask(newMask)
    }

    /**
     * 清空所有蒙版
     */
    fun clearAll() {
        invalidateAllCache()
        _masks.value = emptyList()
        _selectedMaskId.value = null
    }

    /**
     * 应用到图像（CPU 端）
     */
    fun applyToImage(
        source: Bitmap,
        baseParams: com.silas.omaster.renderer.RenderParameters
    ): Bitmap {
        return renderer.applyOnCpu(source, _masks.value, baseParams)
    }

    companion object {
        @Volatile
        private var instance: MaskManager? = null

        fun getInstance(context: Context): MaskManager {
            return instance ?: synchronized(this) {
                instance ?: MaskManager(context.applicationContext).also { instance = it }
            }
        }

        /** 全局默认预设（用于"添加蒙版"快速选择） */
        val QUICK_PRESETS = AdjustmentMask.DEFAULT_PRESETS
    }
}
