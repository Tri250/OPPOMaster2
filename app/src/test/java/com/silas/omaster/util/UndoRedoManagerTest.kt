package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * UndoRedoManager 单元测试
 * 覆盖撤销、重做、历史限制、清空等核心行为
 */
class UndoRedoManagerTest {

    @Test
    fun `pushState后 - 应能undo恢复到之前状态`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state1")
        manager.pushState("state2")

        val undone = manager.undo("state3")
        assertEquals("state2", undone)
        assertTrue(manager.canRedo())
    }

    @Test
    fun `undo后 - 应能redo恢复到undo前状态`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state1")
        manager.pushState("state2")
        manager.undo("state3")

        val redoResult = manager.redo("state2_again")
        assertEquals("state3", redoResult)
        assertFalse(manager.canRedo())
    }

    @Test
    fun `空历史时undo - 应返回null`() {
        val manager = UndoRedoManager<String>()
        assertNull(manager.undo("current"))
        assertFalse(manager.canUndo())
    }

    @Test
    fun `空redo栈时redo - 应返回null`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state1")
        assertNull(manager.redo("current"))
        assertFalse(manager.canRedo())
    }

    @Test
    fun `pushState后 - 应清空redo栈`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state1")
        manager.pushState("state2")
        manager.undo("state3")
        assertTrue(manager.canRedo())

        manager.pushState("state4")
        assertFalse(manager.canRedo())
    }

    @Test
    fun `历史记录超过最大值 - 应移除最旧记录`() {
        val manager = UndoRedoManager<Int>(maxHistory = 3)
        manager.pushState(1)
        manager.pushState(2)
        manager.pushState(3)
        manager.pushState(4)

        assertEquals(3, manager.undoCount())
        // 最旧的1应被移除，undo时先返回最近保存的状态4
        assertEquals(4, manager.undo(4))
    }

    @Test
    fun `clear - 应清空所有历史`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state1")
        manager.pushState("state2")
        manager.undo("state3")

        manager.clear()
        assertFalse(manager.canUndo())
        assertFalse(manager.canRedo())
        assertEquals(0, manager.undoCount())
        assertEquals(0, manager.redoCount())
    }

    @Test
    fun `多次undo和redo - 应保持状态一致性`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(1)
        manager.pushState(2)
        manager.pushState(3)

        var current = 4
        current = manager.undo(current) ?: current
        assertEquals(3, current)

        current = manager.undo(current) ?: current
        assertEquals(2, current)

        current = manager.redo(current) ?: current
        assertEquals(3, current)

        current = manager.redo(current) ?: current
        assertEquals(4, current)
    }
}
