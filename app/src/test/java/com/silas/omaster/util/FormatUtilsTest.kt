package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * FormatUtils 单元测试
 * 验证格式化工具类的各种格式化功能
 */
class FormatUtilsTest {

    @Test
    fun `formatFileSize 应正确格式化字节大小`() {
        assertEquals("0 B", FormatUtils.formatFileSize(0))
        assertEquals("512 B", FormatUtils.formatFileSize(512))
        assertEquals("1.00 KB", FormatUtils.formatFileSize(1024))
        assertEquals("1.50 KB", FormatUtils.formatFileSize(1536))
        assertEquals("1.00 MB", FormatUtils.formatFileSize(1024 * 1024))
        assertEquals("1.50 GB", FormatUtils.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formatDuration 应正确格式化毫秒时长`() {
        assertEquals("00:00", FormatUtils.formatDuration(0))
        assertEquals("00:01", FormatUtils.formatDuration(1000))
        assertEquals("01:00", FormatUtils.formatDuration(60000))
        assertEquals("01:30", FormatUtils.formatDuration(90000))
        assertEquals("01:00:00", FormatUtils.formatDuration(3600000))
        assertEquals("01:01:01", FormatUtils.formatDuration(3661000))
    }

    @Test
    fun `formatNumber 应正确格式化数字`() {
        assertEquals("0", FormatUtils.formatNumber(0))
        assertEquals("999", FormatUtils.formatNumber(999))
        assertEquals("1.0K", FormatUtils.formatNumber(1000))
        assertEquals("1.5K", FormatUtils.formatNumber(1500))
        assertEquals("10.0K", FormatUtils.formatNumber(10000))
        assertEquals("1.0M", FormatUtils.formatNumber(1000000))
        assertEquals("1.5M", FormatUtils.formatNumber(1500000))
    }

    @Test
    fun `truncateString 应正确截断字符串`() {
        assertEquals("", FormatUtils.truncateString("", 10))
        assertEquals("short", FormatUtils.truncateString("short", 10))
        assertEquals("exact ten.", FormatUtils.truncateString("exact ten.", 10))
        assertEquals("long str...", FormatUtils.truncateString("long string test", 10))
        assertEquals("你好世界...", FormatUtils.truncateString("你好世界测试", 5))
    }

    @Test
    fun `capitalizeFirst 应正确首字母大写`() {
        assertEquals("", FormatUtils.capitalizeFirst(""))
        assertEquals("Hello", FormatUtils.capitalizeFirst("hello"))
        assertEquals("Hello world", FormatUtils.capitalizeFirst("hello world"))
        assertEquals("Already", FormatUtils.capitalizeFirst("Already"))
        assertEquals("123abc", FormatUtils.capitalizeFirst("123abc"))
    }

    @Test
    fun `isValidEmail 应正确验证邮箱格式`() {
        assertTrue(FormatUtils.isValidEmail("test@example.com"))
        assertTrue(FormatUtils.isValidEmail("user.name+tag@domain.co.uk"))
        assertFalse(FormatUtils.isValidEmail(""))
        assertFalse(FormatUtils.isValidEmail("notanemail"))
        assertFalse(FormatUtils.isValidEmail("@nodomain.com"))
        assertFalse(FormatUtils.isValidEmail("user@"))
    }

    @Test
    fun `safeGet 应安全获取列表元素`() {
        val list = listOf("a", "b", "c")
        assertEquals("a", FormatUtils.safeGet(list, 0, "default"))
        assertEquals("c", FormatUtils.safeGet(list, 2, "default"))
        assertEquals("default", FormatUtils.safeGet(list, -1, "default"))
        assertEquals("default", FormatUtils.safeGet(list, 10, "default"))
        assertEquals("default", FormatUtils.safeGet(emptyList(), 0, "default"))
    }
}
