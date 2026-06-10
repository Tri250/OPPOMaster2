package com.silas.omaster.util

import com.silas.omaster.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LogUtil 单元测试
 *
 * 验证 Release 模式下日志行为正确：
 * - 调试日志不影响功能
 * - 不抛出异常
 */
class LogUtilTest {

    @Test
    fun `debug log does not throw`() {
        // 不管是 Debug 还是 Release 模式，调用都不应抛出异常
        LogUtil.d("Test", "test debug message")
        LogUtil.i("Test", "test info message")
        LogUtil.w("Test", "test warning message")
        LogUtil.e("Test", "test error message")
        LogUtil.net("test net log")
    }

    @Test
    fun `logThrowable does not throw with various exception types`() {
        LogUtil.logThrowable("Test", RuntimeException("test"), "context")
        LogUtil.logThrowable("Test", IllegalStateException("test"))
        LogUtil.logThrowable("Test", NullPointerException("test"), "with context")
    }

    @Test
    fun `BuildConfig fields are properly set`() {
        // 验证 BuildConfig 字段已正确生成
        assertNotNull(BuildConfig.UMENG_APPKEY)
        assertNotNull(BuildConfig.UMENG_CHANNEL)
        assertNotNull(BuildConfig.API_BASE_URL)
        assertTrue(BuildConfig.API_BASE_URL.startsWith("https://"))
    }

    @Test
    fun `API base url does not use cleartext protocol`() {
        // 行业最严标准：API 必须使用 HTTPS
        assertTrue(
            "API_BASE_URL must use HTTPS, got: ${BuildConfig.API_BASE_URL}",
            BuildConfig.API_BASE_URL.startsWith("https://")
        )
    }
}
