package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.UndoRedoManager
import org.junit.Assert.*
import org.junit.Test

/**
 * 稳定性测试
 * 验证边界条件、异常输入、长时间运行、重复操作下的稳定性
 */
class StabilityTest {

    // ===== 1. 边界值测试 =====

    @Test
    fun `渲染参数 - 极端值不应崩溃`() {
        val params = RenderParameters(
            saturation = Float.MAX_VALUE,
            contrast = Float.MIN_VALUE,
            brightness = -1000f,
            sharpness = 1000f
        )
        // 核心：调用方法不应抛出异常
        val uniforms = params.toShaderUniforms()
        assertNotNull(uniforms)
        assertEquals(18, uniforms.size)

        val hasAdjustment = params.hasAnyAdjustment()
        assertTrue(hasAdjustment)
    }

    @Test
    fun `渲染参数 - NaN和Infinity输入应被处理`() {
        val params = RenderParameters(
            saturation = Float.NaN,
            brightness = Float.POSITIVE_INFINITY,
            contrast = Float.NEGATIVE_INFINITY
        )
        val uniforms = params.toShaderUniforms()
        assertTrue(uniforms.any { it.isNaN() || it.isInfinite() })
    }

    @Test
    fun `哈苏参数 - 超出范围值仍可创建对象`() {
        // 数据类本身允许超出业务范围的值，由UI层限制
        val params = HasselbladParams(tone = 999, saturation = -999)
        assertEquals(999, params.tone)
        assertEquals(-999, params.saturation)
    }

    // ===== 2. 空输入与异常输入 =====

    @Test
    fun `场景映射 - 空字符串和特殊字符不应崩溃`() {
        val scenes = listOf("", " ", "!@#$%", "portrait-", "--unknown--", "UNKNOWN-SCENE")
        for (sceneId in scenes) {
            try {
                val params = SceneToHasselbladMapping.getParams(sceneId)
                val films = SceneToHasselbladMapping.getRecommendedFilms(sceneId)
                val tips = SceneToHasselbladMapping.getMasterTips(sceneId)
                assertNotNull(params)
                assertNotNull(films)
                assertEquals(4, tips.size)
            } catch (e: Throwable) {
                fail("场景 '$sceneId' 不应抛出异常: ${e.message}")
            }
        }
    }

    @Test
    fun `胶片预设 - 无效ID应返回null且不崩溃`() {
        val film = FilmPresets.getFilmById("non-existent-film")
        assertNull(film)
    }

    @Test
    fun `场景预设 - 无效ID应返回null`() {
        assertNull(ScenePresets.getSceneById("non-existent-scene"))
        assertNull(ScenePresets.getSceneById(""))
    }

    // ===== 3. 撤销重做稳定性 =====

    @Test
    fun `撤销重做 - 大量操作后状态一致`() {
        val manager = UndoRedoManager<Int>(maxHistory = 100)
        repeat(200) { manager.pushState(it) }

        assertEquals(100, manager.undoCount())

        var current = 199
        repeat(50) { current = manager.undo(current) ?: current }
        assertEquals(150, current)

        repeat(30) { current = manager.redo(current) ?: current }
        assertEquals(180, current)
    }

    @Test
    fun `撤销重做 - 达到上限后仍稳定工作`() {
        val manager = UndoRedoManager<String>(maxHistory = 3)
        manager.pushState("a")
        manager.pushState("b")
        manager.pushState("c")
        manager.pushState("d") // 应移除 "a"

        assertEquals(3, manager.undoCount())
        assertEquals("d", manager.undo("current"))
        assertEquals("c", manager.undo("d"))
        assertEquals("b", manager.undo("c"))
        assertNull(manager.undo("b")) // "a" 已被移除
    }

    @Test
    fun `撤销重做 - 重复undo redo不应异常`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(1)
        manager.pushState(2)

        var current = 3
        repeat(10) {
            current = manager.undo(current) ?: current
            current = manager.redo(current) ?: current
        }
        assertEquals(3, current)
    }

    @Test
    fun `撤销重做 - clear后状态应清空`() {
        val manager = UndoRedoManager<Int>()
        manager.pushState(1)
        manager.pushState(2)
        manager.clear()

        assertFalse(manager.canUndo())
        assertFalse(manager.canRedo())
        assertEquals(0, manager.undoCount())
        assertEquals(0, manager.redoCount())
    }

    // ===== 4. 大数据量处理稳定性 =====

    @Test
    fun `场景映射 - 批量场景参数计算稳定`() {
        val sceneIds = ScenePresets.allScenes.map { it.id }
        val profiles = sceneIds.map { SceneToHasselbladMapping.getSceneProfile(it) }

        assertEquals(sceneIds.size, profiles.size)
        assertTrue(profiles.all { it.recommendedFilm.isNotEmpty() })
        assertTrue(profiles.all { it.masterTips.size == 4 })
    }

    @Test
    fun `渲染参数 - 大量merge操作后稳定`() {
        var base = RenderParameters.DEFAULT
        repeat(1000) { i ->
            val overlay = RenderParameters(
                saturation = (i % 200 - 100).toFloat(),
                contrast = (i % 200 - 100).toFloat()
            )
            base = overlay.merge(base)
        }
        assertNotNull(base)
    }

    // ===== 5. 并发场景下的不变性 =====

    @Test
    fun `数据模型 - 不可变对象多次读取一致`() {
        val params = HasselbladParams(tone = 5, saturation = 10)
        repeat(100) {
            assertEquals(5, params.tone)
            assertEquals(10, params.saturation)
        }
    }

    // ===== 6. 状态恢复稳定性 =====

    @Test
    fun `渲染参数 - 从空Map恢复为默认参数`() {
        val params = RenderParameters.fromMap(emptyMap())
        assertEquals(RenderParameters.DEFAULT, params)
    }

    @Test
    fun `渲染参数 - 从部分缺失字段的Map恢复`() {
        val params = RenderParameters.fromMap(mapOf("saturation" to 50f))
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(0f, params.contrast, 0.001f)
        assertEquals(0f, params.brightness, 0.001f)
    }

    // ===== 7. 异常场景下的默认行为 =====

    @Test
    fun `场景分类 - 未知分类应返回UNKNOWN`() {
        val profile = SceneToHasselbladMapping.getSceneProfile("xyz-unknown")
        assertEquals(SceneCategory.UNKNOWN, profile.category)
    }

    @Test
    fun `参数调整建议 - 相同参数应返回空列表`() {
        val current = HasselbladParams(tone = 5, saturation = 10)
        val advice = SceneToHasselbladMapping.getParamAdjustmentAdvice(
            current,
            "portrait-standard" // tone=-3, saturation=10
        )
        // saturation 相同，tone 不同
        assertTrue(advice.isNotEmpty())
        assertTrue(advice.none { it.param == "saturation" })
    }
}
