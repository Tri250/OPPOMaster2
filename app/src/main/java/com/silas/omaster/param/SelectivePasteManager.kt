package com.silas.omaster.param

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 选择性粘贴管理器
 * 支持勾选只粘贴特定参数（如饱和度+对比度），忽略其他参数
 */
class SelectivePasteManager private constructor(context: Context) {
    private val paramAdjustmentManager = ParamAdjustmentManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: SelectivePasteManager? = null

        fun getInstance(context: Context): SelectivePasteManager {
            return instance ?: synchronized(this) {
                instance ?: SelectivePasteManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 可粘贴的参数选项
    val pasteableParams = listOf(
        PasteableParam("saturation", "饱和度", true),
        PasteableParam("contrast", "对比度", true),
        PasteableParam("brightness", "亮度", false),
        PasteableParam("warmth", "冷暖", false),
        PasteableParam("sharpness", "锐度", false),
        PasteableParam("clarity", "清晰度", false),
        PasteableParam("highlights", "高光", false),
        PasteableParam("shadows", "阴影", false),
        PasteableParam("noiseReduction", "降噪", false),
        PasteableParam("skinSmooth", "美肤", false),
        PasteableParam("detail", "细节", false),
        PasteableParam("vignette", "暗角", false),
        PasteableParam("lutIntensity", "LUT强度", false)
    )

    // 当前选中的粘贴参数
    private val _selectedParams = MutableStateFlow<Set<String>>(setOf("saturation", "contrast"))
    val selectedParams: StateFlow<Set<String>> = _selectedParams.asStateFlow()

    // 剪贴板中的参数数据
    private val _clipboard = MutableStateFlow<Map<String, Int>>(emptyMap())
    val clipboard: StateFlow<Map<String, Int>> = _clipboard.asStateFlow()

    // 剪贴板来源信息
    private val _clipboardSource = MutableStateFlow<ClipboardSource?>(null)
    val clipboardSource: StateFlow<ClipboardSource?> = _clipboardSource.asStateFlow()

    /**
     * 切换参数选中状态
     */
    fun toggleParamSelection(paramName: String) {
        val current = _selectedParams.value.toMutableSet()
        if (current.contains(paramName)) {
            current.remove(paramName)
        } else {
            current.add(paramName)
        }
        _selectedParams.value = current
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll(selectAll: Boolean) {
        _selectedParams.value = if (selectAll) {
            pasteableParams.map { it.name }.toSet()
        } else {
            emptySet()
        }
    }

    /**
     * 快速选择预设组合
     */
    fun selectPreset(preset: SelectionPreset) {
        _selectedParams.value = preset.params.toSet()
    }

    /**
     * 复制参数到剪贴板
     */
    fun copyParams(source: ClipboardSource? = null) {
        val allParams = paramAdjustmentManager.getAllParamValues()
        _clipboard.value = allParams
        _clipboardSource.value = source
    }

    /**
     * 复制指定参数到剪贴板
     */
    fun copySpecificParams(params: Map<String, Int>, source: ClipboardSource? = null) {
        _clipboard.value = params
        _clipboardSource.value = source
    }

    /**
     * 选择性粘贴
     * 只粘贴选中的参数，忽略其他参数
     */
    fun pasteSelected(): PasteResult {
        val clipboard = _clipboard.value
        val selected = _selectedParams.value

        if (clipboard.isEmpty()) {
            return PasteResult.Error("剪贴板为空")
        }

        if (selected.isEmpty()) {
            return PasteResult.Error("未选择任何参数")
        }

        val pastedParams = mutableMapOf<String, Int>()
        val skippedParams = mutableMapOf<String, Int>()

        clipboard.forEach { (paramName, value) ->
            if (selected.contains(paramName)) {
                // 粘贴选中的参数
                paramAdjustmentManager.adjustParam(paramName, value.toFloat())
                pastedParams[paramName] = value
            } else {
                // 记录跳过的参数
                skippedParams[paramName] = value
            }
        }

        return PasteResult.Success(
            pastedCount = pastedParams.size,
            skippedCount = skippedParams.size,
            pastedParams = pastedParams,
            skippedParams = skippedParams
        )
    }

    /**
     * 粘贴所有参数（忽略选择）
     */
    fun pasteAll(): PasteResult {
        val clipboard = _clipboard.value

        if (clipboard.isEmpty()) {
            return PasteResult.Error("剪贴板为空")
        }

        clipboard.forEach { (paramName, value) ->
            paramAdjustmentManager.adjustParam(paramName, value.toFloat())
        }

        return PasteResult.Success(
            pastedCount = clipboard.size,
            skippedCount = 0,
            pastedParams = clipboard,
            skippedParams = emptyMap()
        )
    }

    /**
     * 清空剪贴板
     */
    fun clearClipboard() {
        _clipboard.value = emptyMap()
        _clipboardSource.value = null
    }

    /**
     * 获取剪贴板预览
     */
    fun getClipboardPreview(): List<ClipboardPreviewItem> {
        val clipboard = _clipboard.value
        val selected = _selectedParams.value

        return pasteableParams.map { param ->
            ClipboardPreviewItem(
                paramName = param.name,
                displayName = param.displayName,
                value = clipboard[param.name],
                isSelected = selected.contains(param.name),
                willPaste = selected.contains(param.name) && clipboard.containsKey(param.name)
            )
        }
    }

    /**
     * 检查剪贴板是否有数据
     */
    fun hasClipboardData(): Boolean = _clipboard.value.isNotEmpty()

    /**
     * 获取选中的参数数量
     */
    fun getSelectedCount(): Int = _selectedParams.value.size

    /**
     * 获取剪贴板中可粘贴的参数数量
     */
    fun getPasteableCount(): Int {
        val clipboard = _clipboard.value
        val selected = _selectedParams.value
        return clipboard.keys.count { selected.contains(it) }
    }
}

/**
 * 可粘贴参数定义
 */
data class PasteableParam(
    val name: String,
    val displayName: String,
    val defaultSelected: Boolean = false
)

/**
 * 剪贴板来源信息
 */
data class ClipboardSource(
    val presetName: String? = null,
    val deviceName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 选择预设
 */
data class SelectionPreset(
    val name: String,
    val description: String,
    val params: List<String>
) {
    companion object {
        // 预设选择组合
        val PRESETS = listOf(
            SelectionPreset(
                name = "饱和度+对比度",
                description = "只粘贴饱和度和对比度参数",
                params = listOf("saturation", "contrast")
            ),
            SelectionPreset(
                name = "色彩相关",
                description = "饱和度、对比度、冷暖、亮度",
                params = listOf("saturation", "contrast", "brightness", "warmth")
            ),
            SelectionPreset(
                name = "细节相关",
                description = "锐度、清晰度、细节、降噪",
                params = listOf("sharpness", "clarity", "detail", "noiseReduction")
            ),
            SelectionPreset(
                name = "光影相关",
                description = "高光、阴影、亮度、暗角",
                params = listOf("highlights", "shadows", "brightness", "vignette")
            ),
            SelectionPreset(
                name = "人像相关",
                description = "美肤、锐度、清晰度",
                params = listOf("skinSmooth", "sharpness", "clarity")
            )
        )
    }
}

/**
 * 粘贴结果
 */
sealed class PasteResult {
    data class Success(
        val pastedCount: Int,
        val skippedCount: Int,
        val pastedParams: Map<String, Int>,
        val skippedParams: Map<String, Int>
    ) : PasteResult()

    data class Error(val message: String) : PasteResult()
}

/**
 * 剪贴板预览项
 */
data class ClipboardPreviewItem(
    val paramName: String,
    val displayName: String,
    val value: Int?,
    val isSelected: Boolean,
    val willPaste: Boolean
)
