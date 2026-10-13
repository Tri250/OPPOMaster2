package com.silas.omaster.data.imports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ED-03 测试基线：FD-01b 粘贴导入校验管道（R-3 文本入口分支）。
 */
class PresetImportValidatorTest {

    private val v = PresetImportValidator

    @Test
    fun `空内容返回 EMPTY`() {
        assertEquals(
            PresetImportValidator.ErrorCode.EMPTY,
            (v.parse("   ") as PresetImportValidator.ImportResult.Failure).code
        )
        assertEquals(
            PresetImportValidator.ErrorCode.EMPTY,
            (v.parse(null) as PresetImportValidator.ImportResult.Failure).code
        )
    }

    @Test
    fun `单个预设对象导入成功`() {
        val r = v.parse("""{"name":"胶片","iso":"100","coverPath":"x.jpg"}""")
        assertTrue(r is PresetImportValidator.ImportResult.Success)
        assertEquals(1, (r as PresetImportValidator.ImportResult.Success).presets.size)
        assertEquals("胶片", r.presets[0].name)
    }

    @Test
    fun `缺失 coverPath 自动注入空串导入成功`() {
        val r = v.parse("""{"name":"无封面","iso":"100"}""")
        assertTrue(r is PresetImportValidator.ImportResult.Success)
        val p = (r as PresetImportValidator.ImportResult.Success).presets[0]
        assertEquals("", p.coverPath)
        assertEquals("100", p.iso)
    }

    @Test
    fun `预设数组导入成功`() {
        val r = v.parse("""[{"name":"A"},{"name":"B"}]""")
        assertTrue(r is PresetImportValidator.ImportResult.Success)
        assertEquals(2, (r as PresetImportValidator.ImportResult.Success).presets.size)
    }

    @Test
    fun `完整订阅内容取其 presets 并忽略外层字段`() {
        val r = v.parse(
            """{"name":"订阅名","author":"作者","build":3,
               "presets":[{"name":"P1"},{"name":"P2"},{"name":""}]}"""
        )
        assertTrue(r is PresetImportValidator.ImportResult.Success)
        // name 为空的预设被过滤，订阅外壳不会成为预设
        assertEquals(2, (r as PresetImportValidator.ImportResult.Success).presets.size)
    }

    @Test
    fun `未知字段被忽略`() {
        val r = v.parse("""{"name":"A","hack_field":"whatever"}""")
        assertTrue(r is PresetImportValidator.ImportResult.Success)
    }

    @Test
    fun `非法 JSON 返回 INVALID_JSON`() {
        val r = v.parse("""{"name": "A", """)
        assertEquals(
            PresetImportValidator.ErrorCode.INVALID_JSON,
            (r as PresetImportValidator.ImportResult.Failure).code
        )
    }

    @Test
    fun `全部预设 name 为空返回 NO_VALID_PRESET`() {
        val r = v.parse("""[{"name":""},{"name":"   "}]""")
        assertEquals(
            PresetImportValidator.ErrorCode.NO_VALID_PRESET,
            (r as PresetImportValidator.ImportResult.Failure).code
        )
    }

    @Test
    fun `超过数量上限返回 TOO_MANY_PRESETS`() {
        val many = (1..(PresetImportValidator.MAX_PRESET_COUNT + 1)).joinToString(",") { """{"name":"N$it"}""" }
        val r = v.parse("[$many]")
        assertEquals(
            PresetImportValidator.ErrorCode.TOO_MANY_PRESETS,
            (r as PresetImportValidator.ImportResult.Failure).code
        )
    }

    @Test
    fun `MU-05 未知字段计入告警映射`() {
        val r = v.parse("""{"name":"A","hack_field":"x","future_knob":1}""")
        val s = r as PresetImportValidator.ImportResult.Success
        assertEquals(listOf("hack_field", "future_knob"), s.unknownFields[0])
    }

    @Test
    fun `MU-05 deviceProfile 对象声明不计入告警`() {
        val r = v.parse("""{"name":"A","deviceProfile":{"brand":"OPPO","models":["Find X10"]}}""")
        val s = r as PresetImportValidator.ImportResult.Success
        assertTrue(s.unknownFields.isEmpty())
    }

    @Test
    fun `MU-05 deviceProfile 类型非法计入告警`() {
        val r = v.parse("""{"name":"A","deviceProfile":"OPPO"}""")
        val s = r as PresetImportValidator.ImportResult.Success
        assertEquals(listOf("deviceProfile"), s.unknownFields[0])
    }
}
