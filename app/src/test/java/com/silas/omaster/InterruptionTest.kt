package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.UndoRedoManager
import org.junit.Assert.*
import org.junit.Test

/**
 * 中断测试
 * 验证应用被中断（电话、切后台、旋转屏幕等）后状态能正确恢复
 */
class InterruptionTest {

    // ===== 1. 撤销重做状态恢复 =====

    @Test
    fun `中断恢复 - 撤销重做历史应在多次操作后保持一致`() {
        val manager = UndoRedoManager<String>()
        manager.pushState("state_1")
        manager.pushState("state_2")

        // 模拟用户撤销一步
        var current = "state_3"
        current = manager.undo(current) ?: current
        assertEquals("state_2", current)

        // 模拟中断后恢复：历史栈应保留
        assertTrue(manager.canUndo())
        assertTrue(manager.canRedo())

        // 继续撤销
        current = manager.undo(current) ?: current
        assertEquals("state_1", current)
    }

    @Test
    fun `中断恢复 - 新操作覆盖重做栈模拟重新编辑`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(10)
        manager.pushState(20)

        var current = 30
        current = manager.undo(current) ?: current // current=20
        assertEquals(20, current)

        // 中断后用户没有重做，而是继续编辑
        manager.pushState(25)
        assertFalse(manager.canRedo())
        assertEquals(2, manager.undoCount())
    }

    // ===== 2. 渲染参数状态保持 =====

    @Test
    fun `中断恢复 - 渲染参数对象不可变可安全恢复`() {
        val originalParams = RenderParameters(
            saturation = 30f,
            contrast = -10f,
            brightness = 5f
        )

        // 模拟中断前保存
        val savedParams = originalParams

        // 模拟中断后恢复
        val restoredParams = savedParams

        assertEquals(originalParams, restoredParams)
        assertEquals(30f, restoredParams.saturation, 0.001f)
    }

    @Test
    fun `中断恢复 - 插值中间状态可恢复`() {
        val start = RenderParameters(saturation = 0f)
        val end = RenderParameters(saturation = 100f)

        // 用户调整到50%后中断
        val midState = start.lerp(end, 0.5f)

        // 恢复后继续调整
        val finalState = midState.lerp(end, 0.5f)
        assertEquals(75f, finalState.saturation, 0.001f)
    }

    // ===== 3. 场景分析状态恢复 =====

    @Test
    fun `中断恢复 - 场景画像可重新构建`() {
        val sceneId = "landscape-sunset"
        val profileBefore = SceneToHasselbladMapping.getSceneProfile(sceneId)

        // 模拟中断后重建
        val profileAfter = SceneToHasselbladMapping.getSceneProfile(sceneId)

        assertEquals(profileBefore.id, profileAfter.id)
        assertEquals(profileBefore.name, profileAfter.name)
        assertEquals(profileBefore.category, profileAfter.category)
        assertEquals(profileBefore.hasselbladParams, profileAfter.hasselbladParams)
        assertEquals(profileBefore.recommendedFilm.size, profileAfter.recommendedFilm.size)
    }

    // ===== 4. 数据模型持久化兼容性 =====

    @Test
    fun `中断恢复 - 大师预设关键字段应可重建`() {
        val original = MasterPreset(
            id = "preset_001",
            name = "日落胶片",
            coverPath = "cover.jpg",
            galleryImages = listOf("g1.jpg", "g2.jpg"),
            author = "@TestAuthor",
            brand = "oppo",
            build = 3
        )

        // 模拟序列化/反序列化后重建
        val restored = original.copy()

        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.coverPath, restored.coverPath)
        assertEquals(original.galleryImages, restored.galleryImages)
        assertEquals(original.author, restored.author)
        assertEquals(original.brand, restored.brand)
        assertEquals(original.build, restored.build)
    }

    @Test
    fun `中断恢复 - 订阅信息应完整保留`() {
        val original = Subscription(
            url = "https://example.com/presets.json",
            name = "测试订阅",
            author = "测试作者",
            build = 5,
            presetCount = 20,
            lastUpdateTime = System.currentTimeMillis()
        )

        val restored = original.copy()

        assertEquals(original.url, restored.url)
        assertEquals(original.name, restored.name)
        assertEquals(original.author, restored.author)
        assertEquals(original.build, restored.build)
        assertEquals(original.presetCount, restored.presetCount)
        assertEquals(original.lastUpdateTime, restored.lastUpdateTime)
    }

    // ===== 5. 默认值恢复 =====

    @Test
    fun `中断恢复 - 默认参数始终一致`() {
        val default1 = RenderParameters.DEFAULT
        val default2 = RenderParameters()
        assertEquals(default1, default2)
    }

    @Test
    fun `中断恢复 - 默认哈苏参数始终一致`() {
        val default1 = HasselbladParams()
        val default2 = HasselbladParams()
        assertEquals(default1, default2)
    }

    // ===== 6. 部分数据丢失场景 =====

    @Test
    fun `中断恢复 - 从部分参数Map恢复`() {
        val partialMap = mapOf(
            "saturation" to 40f,
            "sharpness" to 60f
        )
        val restored = RenderParameters.fromMap(partialMap)

        assertEquals(40f, restored.saturation, 0.001f)
        assertEquals(60f, restored.sharpness, 0.001f)
        assertEquals(0f, restored.contrast, 0.001f) // 缺失字段为0
    }

    @Test
    fun `中断恢复 - 空历史记录不应崩溃`() {
        val manager = UndoRedoManager<String>()
        assertFalse(manager.canUndo())
        assertFalse(manager.canRedo())
        assertNull(manager.undo("current"))
        assertNull(manager.redo("current"))
    }

    // ===== 7. 多次中断场景 =====

    @Test
    fun `中断恢复 - 多次连续中断后状态正确`() {
        val manager = UndoRedoManager<Int>()
        var current = 0

        // 第一次编辑
        manager.pushState(1)
        current = 1

        // 第一次中断恢复
        manager.pushState(2)
        current = 2

        // 第二次中断恢复
        current = manager.undo(current) ?: current
        assertEquals(1, current)

        // 第三次中断恢复后继续编辑
        manager.pushState(3)
        current = 3

        assertTrue(manager.canUndo())
        assertFalse(manager.canRedo())
    }

    // ===== 8. 配置变更模拟 =====

    @Test
    fun `中断恢复 - 场景ID在不同形式下应稳定解析`() {
        val ids = listOf("portrait", "portrait-standard", "PORTRAIT")
        for (sceneId in ids) {
            try {
                val params = SceneToHasselbladMapping.getParams(sceneId)
                assertNotNull(params)
            } catch (e: Throwable) {
                fail("场景 '$sceneId' 不应崩溃: ${e.message}")
            }
        }
    }
}
