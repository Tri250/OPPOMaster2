package com.silas.omaster.infrastructure.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * SecurityCrypto 单元测试
 * 验证加密解密功能的正确性
 *
 * 注意：实际的 Android Keystore 测试需要 instrumented test
 * 此测试验证基础加密逻辑和格式兼容性
 */
class SecurityCryptoTest {

    @Test
    fun `encrypt decrypt 往返应返回原始字符串`() {
        val original = "Hello, OMaster! 测试中文 123"
        val encrypted = SecurityCrypto.encrypt(original)
        assertNotNull(encrypted)
        assertTrue(encrypted!!.isNotEmpty())

        val decrypted = SecurityCrypto.decrypt(encrypted)
        assertEquals(original, decrypted)
    }

    @Test
    fun `空字符串加密解密应安全处理`() {
        val encrypted = SecurityCrypto.encrypt("")
        assertNotNull(encrypted)

        val decrypted = SecurityCrypto.decrypt(encrypted!!)
        assertEquals("", decrypted)
    }

    fun `长文本加密解密应正常工作`() {
        val longText = buildString {
            repeat(1000) { append("OMaster 测试 ") }
        }
        val encrypted = SecurityCrypto.encrypt(longText)
        assertNotNull(encrypted)

        val decrypted = SecurityCrypto.decrypt(encrypted!!)
        assertEquals(longText, decrypted)
    }

    @Test
    fun `特殊字符加密解密应正常工作`() {
        val special = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        val encrypted = SecurityCrypto.encrypt(special)
        assertNotNull(encrypted)

        val decrypted = SecurityCrypto.decrypt(encrypted!!)
        assertEquals(special, decrypted)
    }

    @Test
    fun `解密无效Base64应返回null`() {
        assertNull(SecurityCrypto.decrypt(""))
        assertNull(SecurityCrypto.decrypt("invalid_base64!!!"))
    }

    @Test
    fun `GCM_IV_LENGTH 应为12字节`() {
        assertEquals(12, SecurityCrypto.GCM_IV_LENGTH)
    }
}
