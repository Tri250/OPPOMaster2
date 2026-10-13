package com.silas.omaster.data.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MU-06① 对照录入解析器单测（自检 F5：纯逻辑层 CI 可测） */
class ScreenCaptureParserTest {

    @Test
    fun `parse full hasselblad style capture text`() {
        val text = """
            哈苏大师·海岸黄昏
            ISO 64
            快门 1/250s
            EV -0.7
            WB 5600K
            滤镜 经典负片
            色调 +5
            饱和度 -10
            冷暖 -8
            锐度 15
            暗角 中
        """.trimIndent()

        val capture = ScreenCaptureParser.parse(text)
        assertNotNull(capture)
        assertEquals("哈苏大师·海岸黄昏", capture!!.name)
        assertEquals("64", capture.fields["iso"])
        assertEquals("1/250", capture.fields["shutterSpeed"])
        assertEquals("-0.7", capture.fields["exposureCompensation"])
        assertEquals("5600", capture.fields["colorTemperature"])
        assertEquals("经典负片", capture.fields["filter"])
        assertEquals("+5", capture.fields["tone"])
        assertEquals("-10", capture.fields["saturation"])
        assertEquals("-8", capture.fields["warmCool"])
        assertEquals("15", capture.fields["sharpness"])
        assertEquals("中", capture.fields["vignette"])
    }

    @Test
    fun `parse english labels and compact forms`() {
        val capture = ScreenCaptureParser.parse("ISO100 shutter 1/60 EV+2 WB 5000K")
        assertNotNull(capture)
        assertEquals("100", capture!!.fields["iso"])
        assertEquals("1/60", capture.fields["shutterSpeed"])
        assertEquals("+2", capture.fields["exposureCompensation"])
        assertEquals("5000", capture.fields["colorTemperature"])
        assertNull(capture.name) // 全部是参数行，无名称行
    }

    @Test
    fun `parse ignores unknown lines and deduplicates first-wins`() {
        val capture = ScreenCaptureParser.parse("ISO 100\n随便一行废话\nISO 200")
        assertNotNull(capture)
        assertEquals("100", capture!!.fields["iso"])
        assertEquals(1, capture.fields.size)
    }

    @Test
    fun `parse blank or paramless text returns null`() {
        assertNull(ScreenCaptureParser.parse(null))
        assertNull(ScreenCaptureParser.parse("  "))
        assertNull(ScreenCaptureParser.parse("今天天气不错，出去扫街！"))
    }

    @Test
    fun `toPreset builds custom preset with placeholder cover and name`() {
        val capture = ScreenCaptureParser.parse("ISO 64\n快门 1/120\n白平衡 4800K\n色调风格 自然")!!
        val preset = ScreenCaptureParser.toPreset(capture)
        assertTrue(preset.isCustom)
        assertEquals("", preset.coverPath)
        assertEquals("对照录入", preset.name) // 无名称行 → 占位名
        assertEquals("64", preset.iso)
        assertEquals("1/120", preset.shutterSpeed)
        assertEquals(4800, preset.colorTemperature)
        assertEquals("4800K", preset.whiteBalance)
        assertEquals("自然", preset.colorTone)
    }

    @Test
    fun `toPreset output roundtrips through import validator`() {
        // 导出/解析产物必须能通过统一校验管道（R-3 全链路一致性）
        val capture = ScreenCaptureParser.parse("ISO 50\nEV -1.0\n滤镜 醇黑")!!
        val preset = ScreenCaptureParser.toPreset(capture)
        val json = com.silas.omaster.data.exports.ExportPresetService.toJson(preset)
        assertTrue(json is com.silas.omaster.data.exports.ExportPresetService.ExportResult.Success)
        val result = PresetImportValidator.parse((json as com.silas.omaster.data.exports.ExportPresetService.ExportResult.Success).text)
        assertTrue(result is PresetImportValidator.ImportResult.Success)
        val roundtrip = (result as PresetImportValidator.ImportResult.Success).presets.single()
        assertEquals("对照录入", roundtrip.name)
        assertEquals("50", roundtrip.iso)
        assertEquals("-1.0", roundtrip.exposureCompensation)
        assertEquals("醇黑", roundtrip.filter)
    }
}
