package com.silas.omaster.util

/**
 * 撤销/重做管理器
 * 用于预设编辑时支持 Undo/Redo 操作
 */
class UndoRedoManager<T>(private val maxHistory: Int = 30) {
    
    private val undoStack = ArrayDeque<T>(maxHistory)
    private val redoStack = ArrayDeque<T>(maxHistory)
    
    /**
     * 记录当前状态到撤销栈
     */
    fun pushState(state: T) {
        undoStack.addLast(state)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        // 新操作后清空重做栈
        redoStack.clear()
    }
    
    /**
     * 撤销：返回上一个状态
     */
    fun undo(currentState: T): T? {
        if (undoStack.isEmpty()) return null
        // 保存当前状态到重做栈
        redoStack.addLast(currentState)
        return undoStack.removeLast()
    }
    
    /**
     * 重做：返回下一个状态
     */
    fun redo(currentState: T): T? {
        if (redoStack.isEmpty()) return null
        // 保存当前状态到撤销栈
        undoStack.addLast(currentState)
        return redoStack.removeLast()
    }
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * 清空历史
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
    
    /**
     * 历史记录数量
     */
    fun undoCount(): Int = undoStack.size
    fun redoCount(): Int = redoStack.size
}