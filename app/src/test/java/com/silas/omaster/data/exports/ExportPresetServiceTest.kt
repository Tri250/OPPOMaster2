package com.silas.omaster.data.exports

import com.silas.omaster.data.imports.PresetImportValidator
import com.silas.omaster.model.MasterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MU-07 导出服务单测：导出 ↔ 导入互逆（自检 F7「导出即投稿」口径） */
class ExportPresetServiceTest {

    private val preset = MasterPreset(
        name = "测试预设",
        coverPath = "/data/cover.webp",
        iso = "100",
        shutterSpeed = "1/500",
        exposureCompensation = "-0.3",
        colorTemperature = 5200,
        tags = listOf("人像", "夜街")
    )

    @Test
    fun `single preset exports as one json object`() {
        val result = ExportPresetService.toJson(preset)
        assertTrue(result is ExportPresetService.ExportResult.Success)
        val text = (result as ExportPresetService.ExportResult.Success).text
        assertTrue(text.trimStart().startsWith("{"))
        assertTrue(text.contains("\"name\""))
        // explicitNulls=false：未设置的 mode 不应输出
        assertTrue(!text.contains("\"mode\""))
    }

    @Test
    fun `array and package formats export valid structure`() {
        val array = ExportPresetService.toJson(listOf(preset, preset.copy(name = "第二")), ExportPresetService.Format.ARRAY)
        val pack = ExportPresetService.toJson(listOf(preset), ExportPresetService.Format.PACKAGE)
        val arrayText = (array as ExportPresetService.ExportResult.Success).text
        val packText = (pack as ExportPresetService.ExportResult.Success).text
        assertTrue(arrayText.trimStart().startsWith("["))
        assertTrue(packText.contains("\"presets\""))
        assertTrue(packText.contains("\"build\""))
    }

    @Test
    fun `empty list export fails with reason`() {
        val result = ExportPresetService.toJson(emptyList())
        assertTrue(result is ExportPresetService.ExportResult.Failure)
    }

    @Test
    fun `export roundtrips through import validator`() {
        val text = (ExportPresetService.toJson(preset) as ExportPresetService.ExportResult.Success).text
        val result = PresetImportValidator.parse(text)
        assertTrue(result is PresetImportValidator.ImportResult.Success)
        val restored = (result as PresetImportValidator.ImportResult.Success).presets.single()
        assertEquals("测试预设", restored.name)
        assertEquals("100", restored.iso)
        assertEquals("1/500", restored.shutterSpeed)
        assertEquals("-0.3", restored.exposureCompensation)
        assertEquals(5200, restored.colorTemperature)
        assertEquals(listOf("人像", "夜街"), restored.tags)
    }

    @Test
    fun `share text is human readable and non json`() {
        val text = ExportPresetService.toShareText(preset)
        assertTrue(text.contains("【测试预设】"))
        assertTrue(text.contains("ISO 100"))
        assertTrue(text.contains("快门 1/500"))
        assertTrue(text.contains("#人像"))
    }
}
