package com.silas.omaster.data.lut

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * LUT3DParser 单元测试
 * 验证标准 Adobe .cube 文件解析的正确性与容错性
 */
class LUT3DParserTest {

    @Test
    fun `parse - 标准2尺寸cube应正确解析`() {
        val cube = buildCube(size = 2, title = "Test LUT")
        val result = LUT3DParser.parse(ByteArrayInputStream(cube.toByteArray()), "test.cube")

        assertNotNull(result)
        assertEquals("Test LUT", result?.title)
        assertEquals(2, result?.size)
        assertEquals(2 * 2 * 2 * 3, result?.data?.size ?: 0)
    }

    @Test
    fun `parse - 应跳过注释和空行`() {
        val cube = """
            # This is a comment
            TITLE "Commented LUT"

            # Empty line above
            LUT_3D_SIZE 2
            # Data starts
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val result = LUT3DParser.parse(ByteArrayInputStream(cube.toByteArray()), "comment.cube")
        assertNotNull(result)
        assertEquals("Commented LUT", result?.title)
        assertEquals(2, result?.size)
    }

    @Test
    fun `parse - 缺少LUT_3D_SIZE应返回null`() {
        val cube = """
            0.0 0.0 0.0
            1.0 1.0 1.0
        """.trimIndent()

        val result = LUT3DParser.parse(ByteArrayInputStream(cube.toByteArray()), "bad.cube")
        assertNull(result)
    }

    @Test
    fun `parse - 数据点不足应返回null`() {
        val cube = """
            LUT_3D_SIZE 2
            0.0 0.0 0.0
        """.trimIndent()

        val result = LUT3DParser.parse(ByteArrayInputStream(cube.toByteArray()), "incomplete.cube")
        assertNull(result)
    }

    @Test
    fun `parse - 空输入应返回null`() {
        val result = LUT3DParser.parse(ByteArrayInputStream(ByteArray(0)), "empty.cube")
        assertNull(result)
    }

    @Test
    fun `parse - 默认TITLE应使用文件名`() {
        val cube = buildCube(size = 2)
        val result = LUT3DParser.parse(ByteArrayInputStream(cube.toByteArray()), "fallback.cube")
        assertEquals("fallback.cube", result?.title)
    }

    private fun buildCube(size: Int, title: String? = null): String {
        val sb = StringBuilder()
        title?.let { sb.appendLine("TITLE \"$it\"") }
        sb.appendLine("LUT_3D_SIZE $size")
        repeat(size * size * size) { index ->
            val r = ((index shr 0) and 1).toFloat()
            val g = ((index shr 1) and 1).toFloat()
            val b = ((index shr 2) and 1).toFloat()
            sb.appendLine("$r $g $b")
        }
        return sb.toString()
    }
}
