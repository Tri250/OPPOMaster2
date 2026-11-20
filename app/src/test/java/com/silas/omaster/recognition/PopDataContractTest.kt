package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * POP 即拍真实数据契约测试（P0 修复的回归防线）。
 *
 * 直接读取仓库里的 assets/presets.json（内置预设唯一数据源），走
 * 「反序列化 → SectionParamsParser.enrich → POP 引擎消费」的完整链路，
 * 断言每个真实预设都对 POP 即拍产生可见效果。
 *
 * 这条测试专门拦截"数据层（sections）与引擎层（typed 字段）脱节"类回归：
 * v1.7.5 时 25/25 内置预设在此断言下会全部失败（矩阵恒为单位矩阵）。
 *
 * 注意：Gradle Test 任务的工作目录为模块目录（app/），故相对路径 src/main/assets 可达。
 */
class PopDataContractTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** 支持手动曝光的设备能力集（与 CameraCapabilityProbeTest 同规格） */
    private val ranges = ManualRanges(
        isoRange = 50..3200,
        exposureTimeRange = 100_000L..32_000_000L,
        evStepRange = -12..12,
        evStepEv = 1f / 6f,
        aeModeOffSupported = true
    )

    private fun loadRealPresets(): List<MasterPreset> {
        val candidates = listOf(
            "src/main/assets/presets.json",
            "app/src/main/assets/presets.json",
            "../src/main/assets/presets.json"
        )
        val file = candidates.map(::File).firstOrNull { it.isFile }
            ?: error("未找到 presets.json（工作目录 ${File("").absolutePath}），候选路径: $candidates")
        val parsed = json.decodeFromString<PresetList>(file.readText())
        // _placeholder_* 为格式占位示例，非真实预设
        return parsed.presets.filterNot { it.name.startsWith("_placeholder") }
    }

    private fun CapturePlan.statusOf(key: ParamKey) = params.firstOrNull { it.key == key }

    @Test
    fun `真实内置预设桥接后 POP 即拍全部产生可见效果`() {
        val presets = loadRealPresets()
        assertTrue("内置预设数量应 >= 25，实际 ${presets.size}", presets.size >= 25)

        val failures = mutableListOf<String>()
        for (preset in presets) {
            val enriched = SectionParamsParser.enrich(preset)
            if (!PresetColorTransform.hasVisibleEffect(enriched)) {
                failures += "${preset.name}（filter=${enriched.filter} sat=${enriched.saturation} vignette=${enriched.vignette}）"
            }
        }
        assertTrue("以下预设经桥接后仍无可见效果: $failures", failures.isEmpty())
    }

    @Test
    fun `真实内置预设桥接后色彩矩阵偏离单位矩阵`() {
        val neutral = PresetColorTransform.buildColorMatrix(
            MasterPreset(coverPath = "", name = "neutral")
        )
        val presets = loadRealPresets().map { SectionParamsParser.enrich(it) }
        val identityLike = presets.filter { p ->
            PresetColorTransform.buildColorMatrix(p).contentEquals(neutral)
        }
        assertTrue("色彩矩阵仍为单位矩阵的预设: ${identityLike.map { it.name }}", identityLike.isEmpty())
    }

    @Test
    fun `真实内置预设桥接后拍摄计划非空且滤镜为拍后套用`() {
        val presets = loadRealPresets().map { SectionParamsParser.enrich(it) }
        for (preset in presets) {
            val plan = PresetCapturePlan.build(preset, ranges)
            assertTrue("${preset.name} 参数列表为空", plan.params.isNotEmpty())
            val filter = plan.statusOf(ParamKey.FILTER)
            assertTrue("${preset.name} 滤镜参数缺失", filter != null)
            assertEquals("${preset.name} 滤镜应为 POST", ApplyStatus.POST, filter?.status)
        }
    }

    @Test
    fun `百分比滤镜强度如实缩放`() {
        // 胶片感的滤镜值确为带百分比的写法（桥接后保留原文）
        val base = loadRealPresets().first { it.name == "胶片感" }
        assertEquals("复古 75%", SectionParamsParser.enrich(base).filter)
        val fullSat = PresetColorTransform.filterAdjustment("复古 100%").saturation
        val partSat = PresetColorTransform.filterAdjustment("复古 75%").saturation
        assertNotEquals(fullSat, partSat, 1e-5f)
        assertTrue("75% 强度应弱于 100%", kotlin.math.abs(partSat) < kotlin.math.abs(fullSat))
    }
}
