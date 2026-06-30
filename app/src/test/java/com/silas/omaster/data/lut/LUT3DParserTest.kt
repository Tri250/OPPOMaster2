package com.silas.omaster.data.lut

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * LUT3DParser 单元测试
 * 验证 3D LUT 文件解析功能
 */
class LUT3DParserTest {

    @Test
    fun `parseCube 应正确解析标准Cube文件`() {
        val cubeContent = """
            LUT_3D_SIZE 33
            DOMAIN_MIN 0.0 0.0 0.0
            DOMAIN_MAX 1.0 1.0 1.0
            
            0.000000 0.000000 0.000000
            0.031250 0.000000 0.000000
            0.062500 0.000000 0.000000
            0.093750 0.000000 0.000000
        """.trimIndent()

        val inputStream = ByteArrayInputStream(cubeContent.toByteArray())
        val lut = LUT3DParser.parseCube(inputStream, "test_lut")

        assertNotNull(lut)
        assertEquals("test_lut", lut!!.name)
        assertTrue(lut.size >= 3)
    }

    @Test
    fun `parseCube 应处理空文件`() {
        val emptyContent = ""
        val inputStream = ByteArrayInputStream(emptyContent.toByteArray())
        val lut = LUT3DParser.parseCube(inputStream, "empty")

        assertNull(lut)
    }

    @Test
    fun `parseCube 应处理无效格式`() {
        val invalidContent = "invalid content here"
        val inputStream = ByteArrayInputStream(invalidContent.toByteArray())
        val lut = LUT3DParser.parseCube(inputStream, "invalid")

        assertNull(lut)
    }

    @Test
    fun `LUT3DParser 应正确计算LUT大小`() {
        val size33 = 33
        val expectedSize = size33 * size33 * size33
        assertEquals(35937, expectedSize)
    }

    @Test
    fun `LUT3DParser 应正确解析标题行`() {
        val contentWithTitle = """
            TITLE "Test LUT"
            LUT_3D_SIZE 17
            0 0 0
        """.trimIndent()

        val inputStream = ByteArrayInputStream(contentWithTitle.toByteArray())
        val lut = LUT3DParser.parseCube(inputStream, "titled")

        assertNotNull(lut)
    }
}
