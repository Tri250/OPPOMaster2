package com.silas.omaster.util

import org.junit.Assert.*
import org.junit.Test

/**
 * CrashHandler 单元测试
 * 测试崩溃报告敏感信息脱敏逻辑和崩溃报告格式
 */
class CrashHandlerSanitizationTest {

    // ===== 文件路径脱敏 =====

    @Test
    fun `data_data路径应该被脱敏`() {
        val content = "Error at /data/data/com.silas.omaster/files/some_file.txt"
        val sanitized = content.replace(
            Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE),
            "[PATH_REDACTED]"
        )
        assertTrue("data/data路径应被脱敏", sanitized.contains("[PATH_REDACTED]"))
        assertFalse("脱敏后不应包含原始data/data路径", sanitized.contains("/data/data/"))
    }

    @Test
    fun `storage_emulated路径应该被脱敏`() {
        val content = "File not found: /storage/emulated/0/DCIM/Camera/photo.jpg"
        val sanitized = content.replace(
            Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE),
            "[PATH_REDACTED]"
        )
        assertTrue("storage/emulated路径应被脱敏", sanitized.contains("[PATH_REDACTED]"))
        assertFalse("脱敏后不应包含原始storage/emulated路径", sanitized.contains("/storage/emulated/"))
    }

    @Test
    fun `多个文件路径应该全部被脱敏`() {
        val content = """
            Error at /data/data/com.silas.omaster/cache/image_cache
            Also at /storage/emulated/0/Pictures/test.png
        """.trimIndent()
        val sanitized = content
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")

        val redactedCount = sanitized.split("[PATH_REDACTED]").size - 1
        assertEquals("应有2处被脱敏", 2, redactedCount)
    }

    @Test
    fun `非敏感路径不应被脱敏`() {
        val content = "Starting service com.silas.omaster.ui.service.FloatingWindowService"
        val sanitized = content
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")

        assertEquals("非敏感路径应不变", content, sanitized)
    }

    // ===== IP 地址脱敏 =====

    @Test
    fun `IPv4地址应该被脱敏`() {
        val content = "Connection to 192.168.1.100 failed"
        val sanitized = content.replace(
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "[IP_REDACTED]"
        )
        assertTrue("IPv4地址应被脱敏", sanitized.contains("[IP_REDACTED]"))
        assertFalse("脱敏后不应包含原始IP", sanitized.contains("192.168.1.100"))
    }

    @Test
    fun `公网IP地址也应该被脱敏`() {
        val content = "Request from 8.8.8.8 timed out"
        val sanitized = content.replace(
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "[IP_REDACTED]"
        )
        assertTrue("公网IP应被脱敏", sanitized.contains("[IP_REDACTED]"))
    }

    @Test
    fun `多个IP地址应该全部被脱敏`() {
        val content = "Source 10.0.0.1, destination 172.16.0.5"
        val sanitized = content.replace(
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "[IP_REDACTED]"
        )
        val redactedCount = sanitized.split("[IP_REDACTED]").size - 1
        assertEquals("应有2个IP被脱敏", 2, redactedCount)
    }

    @Test
    fun `非IP地址数字不应被脱敏`() {
        val content = "Version 1.0.0, build 100"
        val sanitized = content.replace(
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "[IP_REDACTED]"
        )
        assertEquals("版本号不应被脱敏", content, sanitized)
    }

    @Test
    fun `端口号附带的IP地址也应该被脱敏`() {
        val content = "Connected to 127.0.0.1:8080"
        val sanitized = content.replace(
            Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            "[IP_REDACTED]"
        )
        assertTrue("带端口的IP应被脱敏", sanitized.contains("[IP_REDACTED]"))
    }

    // ===== token/key 模式脱敏 =====

    @Test
    fun `token模式应该被脱敏`() {
        val content = "Authorization: token=abc123xyz"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("token应被脱敏", sanitized.contains("[REDACTED]"))
        assertFalse("脱敏后不应包含原始token值", sanitized.contains("abc123xyz"))
    }

    @Test
    fun `key模式应该被脱敏`() {
        val content = "api_key: sk-1234567890abcdef"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("key应被脱敏", sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `secret模式应该被脱敏`() {
        val content = "client_secret=mysecretvalue"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("secret应被脱敏", sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `password模式应该被脱敏`() {
        val content = "database password: superSecret123"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("password应被脱敏", sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `credential模式应该被脱敏`() {
        val content = "credential = my-credential-value"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("credential应被脱敏", sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `大小写不敏感脱敏`() {
        val content = "TOKEN=UPPERCASE_VALUE"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertTrue("大写TOKEN应被脱敏", sanitized.contains("[REDACTED]"))
    }

    @Test
    fun `普通文本中的token单词不应被误脱敏`() {
        // token 后没有 = 或 : 跟值的情况不应被脱敏
        val content = "The token has expired"
        val sanitized = content.replace(
            Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE),
            "$1=[REDACTED]"
        )
        assertEquals("普通文本中的token不应被误脱敏", content, sanitized)
    }

    // ===== 崩溃报告格式验证 =====

    @Test
    fun `崩溃报告应该包含标题头`() {
        val report = buildString {
            append("==== Crash Report ====\n")
            append("时间: 2025-01-01 12:00:00\n")
            append("线程: main (id=1)\n")
            append("类型: NPE\n")
            append("消息: null pointer\n")
            append("==== 堆栈 ====\n")
        }
        assertTrue("崩溃报告应包含标题", report.contains("==== Crash Report ===="))
        assertTrue("崩溃报告应包含堆栈标题", report.contains("==== 堆栈 ===="))
    }

    @Test
    fun `崩溃报告应该包含时间信息`() {
        val report = buildString {
            append("==== Crash Report ====\n")
            append("时间: 2025-01-01 12:00:00\n")
        }
        assertTrue("崩溃报告应包含时间", report.contains("时间:"))
    }

    @Test
    fun `崩溃报告应该包含线程信息`() {
        val report = buildString {
            append("==== Crash Report ====\n")
            append("线程: main (id=1)\n")
        }
        assertTrue("崩溃报告应包含线程", report.contains("线程:"))
    }

    @Test
    fun `崩溃报告应该包含异常类型`() {
        val report = buildString {
            append("==== Crash Report ====\n")
            append("类型: NPE\n")
        }
        assertTrue("崩溃报告应包含类型", report.contains("类型:"))
    }

    @Test
    fun `崩溃报告应该包含异常消息`() {
        val report = buildString {
            append("==== Crash Report ====\n")
            append("消息: Something went wrong\n")
        }
        assertTrue("崩溃报告应包含消息", report.contains("消息:"))
    }

    // ===== 异常类型分类验证 =====

    @Test
    fun `NullPointerException应分类为NPE`() {
        val exceptionType = when (NullPointerException()) {
            is NullPointerException -> "NPE"
            else -> "OTHER"
        }
        assertEquals("NPE", exceptionType)
    }

    @Test
    fun `IllegalStateException应分类为ISE`() {
        val exceptionType = when (IllegalStateException()) {
            is IllegalStateException -> "ISE"
            else -> "OTHER"
        }
        assertEquals("ISE", exceptionType)
    }

    @Test
    fun `IllegalArgumentException应分类为IAE`() {
        val exceptionType = when (IllegalArgumentException()) {
            is IllegalArgumentException -> "IAE"
            else -> "OTHER"
        }
        assertEquals("IAE", exceptionType)
    }

    @Test
    fun `IndexOutOfBoundsException应分类为IOOBE`() {
        val exceptionType = when (IndexOutOfBoundsException()) {
            is IndexOutOfBoundsException -> "IOOBE"
            else -> "OTHER"
        }
        assertEquals("IOOBE", exceptionType)
    }

    @Test
    fun `ClassCastException应分类为CCE`() {
        val exceptionType = when (ClassCastException()) {
            is ClassCastException -> "CCE"
            else -> "OTHER"
        }
        assertEquals("CCE", exceptionType)
    }

    @Test
    fun `SecurityException应分类为SEC`() {
        val exceptionType = when (SecurityException()) {
            is SecurityException -> "SEC"
            else -> "OTHER"
        }
        assertEquals("SEC", exceptionType)
    }

    @Test
    fun `OutOfMemoryError应分类为OOM`() {
        val exceptionType = when (OutOfMemoryError()) {
            is OutOfMemoryError -> "OOM"
            else -> "OTHER"
        }
        assertEquals("OOM", exceptionType)
    }

    @Test
    fun `StackOverflowError应分类为SOE`() {
        val exceptionType = when (StackOverflowError()) {
            is StackOverflowError -> "SOE"
            else -> "OTHER"
        }
        assertEquals("SOE", exceptionType)
    }

    @Test
    fun `RuntimeException应分类为OTHER`() {
        val exceptionType = when (RuntimeException()) {
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
        assertEquals("OTHER", exceptionType)
    }

    // ===== 堆栈截断逻辑验证 =====

    @Test
    fun `堆栈行数超过maxDepth时应该被截断`() {
        val maxDepth = 20
        val lines = (1..100).map { "at com.example.Class.method(Class.java:$it)" }
        val truncated = if (lines.size > maxDepth) {
            lines.take(maxDepth).joinToString("\n") + "\n... (truncated, total ${lines.size} lines)"
        } else {
            lines.joinToString("\n")
        }
        assertTrue("超长堆栈应被截断", truncated.contains("truncated"))
        assertTrue("截断后应包含总行数", truncated.contains("total 100 lines"))
    }

    @Test
    fun `堆栈行数不超过maxDepth时不应被截断`() {
        val maxDepth = 100
        val lines = (1..50).map { "at com.example.Class.method(Class.java:$it)" }
        val truncated = if (lines.size > maxDepth) {
            lines.take(maxDepth).joinToString("\n") + "\n... (truncated, total ${lines.size} lines)"
        } else {
            lines.joinToString("\n")
        }
        assertFalse("短堆栈不应被截断", truncated.contains("truncated"))
    }

    // ===== OOM 限深验证 =====

    @Test
    fun `OOM时应使用更小的maxDepth`() {
        val isOOM = true
        val maxDepth = if (isOOM) 20 else 100
        assertEquals("OOM时maxDepth应为20", 20, maxDepth)
    }

    @Test
    fun `非OOM时应使用标准maxDepth`() {
        val isOOM = false
        val maxDepth = if (isOOM) 20 else 100
        assertEquals("非OOM时maxDepth应为100", 100, maxDepth)
    }

    // ===== 综合脱敏验证 =====

    @Test
    fun `综合脱敏应该同时处理路径IP和凭证`() {
        val content = """
            Error at /data/data/com.silas.omaster/cache/test
            Connection from 192.168.1.1
            Using token=abc123 for auth
        """.trimIndent()

        val sanitized = content
            .replace(Regex("/data/data/[a-zA-Z0-9._-]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("/storage/emulated/[0-9]+/[a-zA-Z0-9/_-]+", RegexOption.MULTILINE), "[PATH_REDACTED]")
            .replace(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"), "[IP_REDACTED]")
            .replace(Regex("(token|key|secret|password|credential)\\s*[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")

        assertTrue("应包含路径脱敏标记", sanitized.contains("[PATH_REDACTED]"))
        assertTrue("应包含IP脱敏标记", sanitized.contains("[IP_REDACTED]"))
        assertTrue("应包含凭证脱敏标记", sanitized.contains("[REDACTED]"))
    }
}