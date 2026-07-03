package com.silas.omaster.infrastructure.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * UndoRedoManager 单元测试
 * 验证撤销/重做栈的核心行为、边界条件与内存限制
 */
class UndoRedoManagerTest {

    private lateinit var manager: UndoRedoManager<String>

    @Before
    fun setup() {
        manager = UndoRedoManager()
    }

    @Test
    fun `初始状态应不可撤销也不可重做`() {
        assertFalse(manager.canUndo())
        assertFalse(manager.canRedo())
        assertNull(manager.undo("current"))
        assertNull(manager.redo("current"))
    }

    @Test
    fun `pushState后应可撤销`() {
        manager.pushState("state1")
        assertTrue(manager.canUndo())
        assertFalse(manager.canRedo())
    }

    @Test
    fun `undo应返回上一个状态并将当前状态入重做栈`() {
        manager.pushState("state1")
        manager.pushState("state2")

        val result = manager.undo("state3")
        assertEquals("state2", result)
        assertTrue(manager.canRedo())
        assertEquals(1, manager.redoCount())
    }

    @Test
    fun `redo应返回重做栈状态并将当前状态入撤销栈`() {
        manager.pushState("state1")
        manager.pushState("state2")
        manager.undo("state3")

        val result = manager.redo("state3")
        assertEquals("state2", result)
        assertFalse(manager.canRedo())
    }

    @Test
    fun `新操作后应清空重做栈`() {
        manager.pushState("state1")
        manager.pushState("state2")
        manager.undo("state3")
        assertTrue(manager.canRedo())

        manager.pushState("state4")
        assertFalse(manager.canRedo())
        assertTrue(manager.canUndo())
    }

    @Test
    fun `clear应清空所有历史`() {
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
    fun `undo栈应受maxHistory限制`() {
        val limitedManager = UndoRedoManager<String>(maxHistory = 3)
        limitedManager.pushState("s1")
        limitedManager.pushState("s2")
        limitedManager.pushState("s3")
        limitedManager.pushState("s4")

        assertEquals(3, limitedManager.undoCount())
        // 最早的状态 s1 应被移除
        assertEquals("s4", limitedManager.undo("current"))
        assertEquals("s3", limitedManager.undo("current"))
        assertEquals("s2", limitedManager.undo("current"))
        assertNull(limitedManager.undo("current"))
    }

    @Test
    fun `连续undo redo应保持状态一致性`() {
        manager.pushState("A")
        manager.pushState("B")
        manager.pushState("C")

        var current = "D"
        current = manager.undo(current)!! // C
        assertEquals("C", current)
        current = manager.undo(current)!! // B
        assertEquals("B", current)
        current = manager.redo(current)!! // C
        assertEquals("C", current)
        current = manager.redo(current)!! // D
        assertEquals("D", current)
    }

    @Test
    fun `空undo redo应安全返回null`() {
        assertNull(manager.undo("current"))
        assertNull(manager.redo("current"))
    }
}
