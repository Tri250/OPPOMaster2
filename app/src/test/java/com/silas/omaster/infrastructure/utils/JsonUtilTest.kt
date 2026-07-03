package com.silas.omaster.infrastructure.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * JsonUtil 单元测试
 * 验证JSON序列化和反序列化功能
 */
class JsonUtilTest {

    data class TestUser(
        val name: String,
        val age: Int,
        val email: String
    )

    data class TestConfig(
        val id: String,
        val enabled: Boolean,
        val tags: List<String>,
        val settings: Map<String, Int>
    )

    @Test
    fun `toJson 应正确序列化对象`() {
        val user = TestUser(name = "张三", age = 25, email = "test@example.com")
        val json = JsonUtil.toJson(user)

        assertNotNull(json)
        assertTrue(json.contains("张三"))
        assertTrue(json.contains("25"))
        assertTrue(json.contains("test@example.com"))
    }

    @Test
    fun `fromJson 应正确反序列化对象`() {
        val json = """{"name":"李四","age":30,"email":"lisi@test.com"}"""
        val user = JsonUtil.fromJson(json, TestUser::class.java)

        assertNotNull(user)
        assertEquals("李四", user!!.name)
        assertEquals(30, user.age)
        assertEquals("lisi@test.com", user.email)
    }

    @Test
    fun `复杂对象序列化反序列化应正确`() {
        val config = TestConfig(
            id = "cfg-001",
            enabled = true,
            tags = listOf("tag1", "tag2", "tag3"),
            settings = mapOf("volume" to 80, "brightness" to 50)
        )

        val json = JsonUtil.toJson(config)
        val parsed = JsonUtil.fromJson(json, TestConfig::class.java)

        assertNotNull(parsed)
        assertEquals(config.id, parsed!!.id)
        assertEquals(config.enabled, parsed.enabled)
        assertEquals(config.tags.size, parsed.tags.size)
    }

    @Test
    fun `空对象序列化应正常`() {
        data class EmptyObj(val value: String = "")
        val empty = EmptyObj()
        val json = JsonUtil.toJson(empty)
        assertNotNull(json)
    }

    @Test
    fun `无效JSON应返回null`() {
        val result = JsonUtil.fromJson("invalid json {{{", TestUser::class.java)
        assertNull(result)
    }

    @Test
    fun `空字符串JSON应返回null`() {
        assertNull(JsonUtil.fromJson("", TestUser::class.java))
    }

    @Test
    fun `toPrettyJson 应输出格式化的JSON`() {
        val user = TestUser(name = "王五", age = 28, email = "wangwu@test.com")
        val pretty = JsonUtil.toPrettyJson(user)

        assertNotNull(pretty)
        assertTrue(pretty.contains("\n"))
    }
}
