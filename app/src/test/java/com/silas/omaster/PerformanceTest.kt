package com.silas.omaster

import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.model.*
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.util.UndoRedoManager
import com.silas.omaster.util.VersionInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * 性能测试
 * 验证核心算法和数据结构在合理时间内完成
 */
class PerformanceTest {

    companion object {
        // 性能阈值（毫秒）
        private const val SCENE_MAPPING_THRESHOLD_MS = 500L
        private const val RENDER_PARAMS_THRESHOLD_MS = 100L
        private const val UNDO_REDO_THRESHOLD_MS = 100L
        private const val FILM_LOOKUP_THRESHOLD_MS = 200L
    }

    @Test
    fun `场景映射 - 全部场景参数计算应在合理时间内完成`() {
        val sceneIds = ScenePresets.allScenes.map { it.id }
        assertTrue("测试需要至少一个场景", sceneIds.isNotEmpty())

        val start = System.nanoTime()
        repeat(100) {
            for (sceneId in sceneIds) {
                SceneToHasselbladMapping.getParams(sceneId)
                SceneToHasselbladMapping.getRecommendedFilms(sceneId)
                SceneToHasselbladMapping.getMasterTips(sceneId)
            }
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("场景映射 100 轮耗时: ${durationMs}ms")
        assertTrue("场景映射性能应满足阈值", durationMs < SCENE_MAPPING_THRESHOLD_MS)
    }

    @Test
    fun `渲染参数 - toShaderUniforms万次调用应在合理时间内完成`() {
        val params = RenderParameters(
            saturation = 50f,
            contrast = -20f,
            brightness = 10f,
            sharpness = 30f,
            grain = 15f
        )

        val start = System.nanoTime()
        repeat(10_000) {
            params.toShaderUniforms()
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("toShaderUniforms 10000 次耗时: ${durationMs}ms")
        assertTrue("渲染参数转换性能应满足阈值", durationMs < RENDER_PARAMS_THRESHOLD_MS)
    }

    @Test
    fun `渲染参数 - merge和lerp万次调用应在合理时间内完成`() {
        val base = RenderParameters(saturation = 10f, contrast = 20f)
        val overlay = RenderParameters(saturation = 30f)
        val target = RenderParameters(saturation = 100f, brightness = 100f)

        val start = System.nanoTime()
        repeat(10_000) {
            overlay.merge(base)
            base.lerp(target, 0.5f)
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("merge+lerp 10000 次耗时: ${durationMs}ms")
        assertTrue("渲染参数合并插值性能应满足阈值", durationMs < RENDER_PARAMS_THRESHOLD_MS)
    }

    @Test
    fun `撤销重做 - 千次push和undo redo应在合理时间内完成`() {
        val manager = UndoRedoManager<Int>()

        val start = System.nanoTime()
        repeat(1000) { manager.pushState(it) }
        var current = 1000
        repeat(500) { current = manager.undo(current) ?: current }
        repeat(300) { current = manager.redo(current) ?: current }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("撤销重做 1000 次操作耗时: ${durationMs}ms")
        assertTrue("撤销重做性能应满足阈值", durationMs < UNDO_REDO_THRESHOLD_MS)
    }

    @Test
    fun `胶片预设 - 万次查找应在合理时间内完成`() {
        val ids = listOf("cc", "nc", "nh", "portra", "rdp3", "800t", "tx400")

        val start = System.nanoTime()
        repeat(10_000) {
            FilmPresets.getFilmById(ids[it % ids.size])
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("胶片预设查找 10000 次耗时: ${durationMs}ms")
        assertTrue("胶片预设查找性能应满足阈值", durationMs < FILM_LOOKUP_THRESHOLD_MS)
    }

    @Test
    fun `场景预设 - 按类别查找应在合理时间内完成`() {
        val start = System.nanoTime()
        repeat(1000) {
            ScenePresets.getScenesByCategory(SceneCategory.PORTRAIT)
            ScenePresets.getScenesByCategory(SceneCategory.LANDSCAPE)
            ScenePresets.getScenesByCategory(SceneCategory.NIGHT)
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("场景按类别查找 1000 轮耗时: ${durationMs}ms")
        assertTrue("场景分类查找性能应满足阈值", durationMs < SCENE_MAPPING_THRESHOLD_MS)
    }

    @Test
    fun `版本解析 - 万次版本比较应在合理时间内完成`() {
        val versions = listOf("1.0.0", "1.9.0", "2.0.0-beta1", "1.10.50", "v1.2.3")

        val start = System.nanoTime()
        repeat(10_000) {
            VersionInfo.parseVersionCode(versions[it % versions.size])
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("版本解析 10000 次耗时: ${durationMs}ms")
        assertTrue("版本解析性能应满足阈值", durationMs < FILM_LOOKUP_THRESHOLD_MS)
    }

    @Test
    fun `哈苏参数 - 批量格式化应在合理时间内完成`() {
        val params = HasselbladParams(tone = 15, saturation = -10, contrast = 20)

        val start = System.nanoTime()
        repeat(10_000) {
            params.formatParamValue(params.tone)
            params.formatParamValue(params.saturation)
            params.formatParamValue(params.contrast)
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("哈苏参数格式化 10000 次耗时: ${durationMs}ms")
        assertTrue("参数格式化性能应满足阈值", durationMs < FILM_LOOKUP_THRESHOLD_MS)
    }

    @Test
    fun `场景画像 - 构建全部画像应在合理时间内完成`() {
        val sceneIds = ScenePresets.allScenes.map { it.id }

        val start = System.nanoTime()
        repeat(10) {
            sceneIds.map { SceneToHasselbladMapping.getSceneProfile(it) }
        }
        val durationMs = (System.nanoTime() - start) / 1_000_000

        println("构建全部场景画像 10 轮耗时: ${durationMs}ms")
        assertTrue("场景画像构建性能应满足阈值", durationMs < SCENE_MAPPING_THRESHOLD_MS)
    }
}
