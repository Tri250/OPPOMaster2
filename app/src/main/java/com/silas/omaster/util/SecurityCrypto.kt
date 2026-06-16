package com.silas.omaster.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 安全加密工具类
 *
 * 使用 Android Keystore 加密敏感数据（如订阅凭证）
 * 加密方式：AES/GCM/NoPadding
 * 密钥存储：Android Keystore (硬件级别隔离)
 *
 * 密文格式（版本化，支持未来迁移）：
 * [版本(1字节) + IV长度(1字节) + IV(变长) + 算法标识(1字节) + 密文+Tag(变长)]
 */
object SecurityCrypto {

    private const val TAG = "SecurityCrypto"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "omaster_secure_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // 密文格式版本（用于未来算法升级时兼容旧数据）
    private const val CIPHER_VERSION = 1.toByte()

    // 算法标识
    private const val ALGORITHM_AES_GCM_128 = 1.toByte()
    private const val ALGORITHM_AES_GCM_256 = 2.toByte()
    // 预留：private const val ALGORITHM_CHACHA20_POLY1305 = 3.toByte()

    /**
     * 加密字符串
     * @return Base64 编码的 [版本 + IV长度 + IV + 算法标识 + 密文+Tag]，失败返回 null
     */
    fun encrypt(plainText: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 版本化密文格式：[版本(1) + IV长度(1) + IV(变长) + 算法标识(1) + 密文(变长)]
            val combined = ByteArray(1 + 1 + iv.size + 1 + cipherBytes.size)
            var offset = 0

            // 版本号
            combined[offset++] = CIPHER_VERSION
            // IV 长度
            combined[offset++] = iv.size.toByte()
            // IV
            System.arraycopy(iv, 0, combined, offset, iv.size)
            offset += iv.size
            // 算法标识（当前使用 AES-GCM-256）
            combined[offset++] = ALGORITHM_AES_GCM_256
            // 密文 + Tag
            System.arraycopy(cipherBytes, 0, combined, offset, cipherBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "加密失败", e)
            null
        }
    }

    /**
     * 解密字符串
     * 支持版本化密文格式，可处理旧版无版本头的数据（向后兼容）
     */
    fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            // 检查是否为新版版本化格式
            if (combined.isEmpty()) return null

            val version = combined[0].toInt()

            if (version == CIPHER_VERSION.toInt() && combined.size >= 3) {
                // 新版格式：[版本(1) + IV长度(1) + IV(变长) + 算法标识(1) + 密文(变长)]
                var offset = 1
                val ivLength = combined[offset++].toInt()

                if (combined.size < 1 + 1 + ivLength + 1) return null

                val iv = combined.copyOfRange(offset, offset + ivLength)
                offset += ivLength

                val algorithmId = combined[offset++]
                val cipherBytes = combined.copyOfRange(offset, combined.size)

                // 根据算法标识选择参数（当前仅支持 AES-GCM）
                val tagLength = when (algorithmId) {
                    ALGORITHM_AES_GCM_128, ALGORITHM_AES_GCM_256 -> GCM_TAG_LENGTH
                    else -> GCM_TAG_LENGTH // 默认
                }

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(tagLength, iv)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            } else {
                // 旧版格式（向后兼容）：[IV(12) + 密文+Tag]
                if (combined.size < GCM_IV_LENGTH) return null

                val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
                val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解密失败", e)
            null
        }
    }

    /**
     * 获取或创建 Keystore 中的密钥
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        
        // 尝试获取已存在的密钥
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }
        
        // 不存在则创建
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // 关键：要求用户认证才能使用（可选）
            // .setUserAuthenticationRequired(true)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 安全保存字符串到 SharedPreferences
     * 自动加密敏感字段
     */
    fun saveSecure(context: Context, prefsName: String, key: String, value: String) {
        try {
            val encrypted = encrypt(value) ?: return
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(key, encrypted)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存安全数据失败", e)
        }
    }

    /**
     * 从 SharedPreferences 读取并解密字符串
     */
    fun readSecure(context: Context, prefsName: String, key: String): String? {
        return try {
            val encrypted = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .getString(key, null) ?: return null
            decrypt(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "读取安全数据失败", e)
            null
        }
    }
}
