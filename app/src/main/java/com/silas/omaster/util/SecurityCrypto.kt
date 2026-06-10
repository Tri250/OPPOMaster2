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
 */
object SecurityCrypto {

    private const val TAG = "SecurityCrypto"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "omaster_secure_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /**
     * 加密字符串
     * @return Base64 编码的 [IV + 密文 + Tag]，失败返回 null
     */
    fun encrypt(plainText: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // 组合 IV + 密文
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "加密失败", e)
            null
        }
    }

    /**
     * 解密字符串
     */
    fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null

            // 提取 IV 和密文
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
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
