package com.silas.omaster.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * ED-03 测试基线：预设 JSON 解析与校验（R-3 分支）。
 * 覆盖：合法解析、未知字段容忍、缺字段降级、非法 JSON 拒绝、build 缺省。
 * 说明：与 PresetRemoteManager 一致使用 ignoreUnknownKeys 配置。
 */
class PresetListJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `合法订阅 JSON 解析成功`() {
        val text = """
            {"name":"官方预设","author":"OMaster","build":5,
             "presets":[{"id":"p1","name":"胶片","coverPath":"a.jpg","iso":"100"}]}
        """.trimIndent()
        val list = json.decodeFromString(PresetList.serializer(), text)
        assertEquals("官方预设", list.name)
        assertEquals("OMaster", list.author)
        assertEquals(5, list.build)
        assertEquals(1, list.presets.size)
        assertEquals("100", list.presets[0].iso)
    }

    @Test
    fun `未知字段被忽略不阻断加载`() {
        val text = """{"name":"n","author":"a","extra_field":123,"presets":[]}"""
        val list = json.decodeFromString(PresetList.serializer(), text)
        assertEquals(0, list.presets.size)
    }

    @Test
    fun `缺失 name 与 author 时为 null 便于校验管道拦截`() {
        val text = """{"presets":[]}"""
        val list = json.decodeFromString(PresetList.serializer(), text)
        assertNull(list.name)
        assertNull(list.author)
    }

    @Test
    fun `build 缺省时为 1`() {
        val text = """{"name":"n","author":"a","presets":[]}"""
        val list = json.decodeFromString(PresetList.serializer(), text)
        assertEquals(1, list.build)
    }

    @Test
    fun `非法 JSON 解析抛出异常`() {
        val text = """{"name": "n", "presets": ["""
        assertThrows(Exception::class.java) {
            json.decodeFromString(PresetList.serializer(), text)
        }
    }
}
