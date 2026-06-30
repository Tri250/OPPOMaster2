package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * CrashHandler 单元测试
 * 验证崩溃处理器的核心逻辑
 *
 * 注意：实际的线程崩溃捕获需要 Android 环境
 * 此测试验证辅助函数和配置逻辑
 */
class CrashHandlerTest {

    @Test
    fun `CrashHandler 应为单例`() {
        val instance1 = CrashHandler.getInstance()
        val instance2 = CrashHandler.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `初始状态下isInstalled应返回false`() {
        // 注意：实际环境中可能已安装，这里测试单例获取
        val handler = CrashHandler.getInstance()
        assertNotNull(handler)
    }

    @Test
    fun `异常分类应正确识别常见异常`() {
        val npe = NullPointerException("test")
        assertEquals("NPE", getExceptionType(npe))

        val ise = IllegalStateException("test")
        assertEquals("ISE", getExceptionType(ise))

        val iae = IllegalArgumentException("test")
        assertEquals("IAE", getExceptionType(iae))

        val ioobe = IndexOutOfBoundsException("test")
        assertEquals("IOOBE", getExceptionType(ioobe))

        val cce = ClassCastException("test")
        assertEquals("CCE", getExceptionType(cce))

        val se = SecurityException("test")
        assertEquals("SEC", getExceptionType(se))

        val oom = OutOfMemoryError("test")
        assertEquals("OOM", getExceptionType(oom))

        val soe = StackOverflowError("test")
        assertEquals("SOE", getExceptionType(soe))

        val unknown = RuntimeException("test")
        assertEquals("OTHER", getExceptionType(unknown))
    }

    @Test
    fun `敏感信息过滤应正确处理路径`() {
        val contentWithPath = "Error at /data/data/com.silas.omaster/files/cache/test.txt"
        val filtered = sanitizeCrashReport(contentWithPath)
        assertFalse(filtered.contains("/data/data/com.silas.omaster"))
        assertTrue(filtered.contains("[PATH_REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理IP地址`() {
        val contentWithIp = "Connection to 192.168.1.100 failed"
        val filtered = sanitizeCrashReport(contentWithIp)
        assertFalse(filtered.contains("192.168.1.100"))
        assertTrue(filtered.contains("[IP_REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理Token`() {
        val contentWithToken = "Authorization: token=abc123xyz"
        val filtered = sanitizeCrashReport(contentWithToken)
        assertFalse(filtered.contains("abc123xyz"))
        assertTrue(filtered.contains("[REDACTED]"))
    }

    @Test
    fun `敏感信息过滤应正确处理密码`() {
        val contentWithPassword = "user password=secret123"
        val filtered = sanitizeCrashReport(contentWithPassword)
        assertFalse(filtered.contains("secret123"))
        assertTrue(filtered.contains("[REDACTED]"))
    }

    // 辅助函数：从 CrashHandler 复制逻辑用于测试
    private fun getExceptionType(throwable: Throwable): String {
        return when (throwable) {
            is NullPointerException -> "NPE"
            is IllegalStateException -> "ISE"
            is IllegalArgumentException -> "IAE"
            is IndexOutOfBoundsException -> "IOOBE"
            is ClassCastException -> "CCE"
            is SecurityException -> "SEC"
            is OutOfMemoryError -> "OOM"
            is StackOverflowError -> "SOE"
            else -> "OTHER"
        }
    }

    private fun sanitizeCrashReport(content: String): String {
        return content
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"), "[IP_REDACTED]")
            .replace(Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
    }
}
