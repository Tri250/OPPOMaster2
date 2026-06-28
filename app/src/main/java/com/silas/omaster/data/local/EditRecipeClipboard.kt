package com.silas.omaster.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P1-1: 跨图复制粘贴全局剪贴板
 *
 * 单例对象，应用生命周期内保持复制的编辑参数。
 * 支持监听粘贴可用状态，便于 UI 动态显示/隐藏粘贴按钮。
 */
object EditRecipeClipboard {

    private val _clipboardRecipe = MutableStateFlow<EditRecipe?>(null)
    val clipboardRecipe: StateFlow<EditRecipe?> = _clipboardRecipe.asStateFlow()

    private val _hasClipboard = MutableStateFlow(false)
    val hasClipboard: StateFlow<Boolean> = _hasClipboard.asStateFlow()

    /**
     * 复制配方到全局剪贴板
     */
    fun copy(recipe: EditRecipe) {
        _clipboardRecipe.value = recipe
        _hasClipboard.value = true
    }

    /**
     * 从全局剪贴板读取配方
     */
    fun paste(): EditRecipe? = _clipboardRecipe.value

    /**
     * 清空剪贴板
     */
    fun clear() {
        _clipboardRecipe.value = null
        _hasClipboard.value = false
    }
}
