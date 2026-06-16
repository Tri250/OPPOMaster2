package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.silas.omaster.ai.analyzer.FaceDetectorSingleton
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.util.CrashHandler
import com.silas.omaster.util.HapticSettings
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent

class OMasterApplication : Application() {
    companion object {
        private const val PREFS_NAME = "omaster_prefs"
        private const val KEY_USER_AGREED = "user_agreed_to_policy"

        // 与 build.gradle.kts 中的 OBFUSCATION_KEY 保持一致
        private const val OBFUSCATION_KEY = "Oma5terK3y2024!X"

        @Volatile
        private var instance: OMasterApplication? = null
        private lateinit var prefs: SharedPreferences

        /**
         * 运行时解混淆：Base64 解码 + XOR 还原明文 AppKey
         *
         * 防止 APK 反编译后直接提取明文 AppKey。
         * 此为中间安全方案，XOR 混淆可防止简单字符串提取，
         * 但无法抵御针对性逆向分析。生产环境建议：
         * 1. 将 AppKey 迁移至后端代理，通过 API 动态获取
         * 2. 使用 NDK/C++ 层存储密钥（需添加 native 代码）
         */
        private fun deobfuscateKey(obfuscated: String, key: String): String {
            if (obfuscated.isEmpty()) return ""
            return try {
                val decoded = Base64.decode(obfuscated, Base64.DEFAULT)
                val keyBytes = key.toByteArray(Charsets.UTF_8)
                val result = ByteArray(decoded.size)
                for (i in decoded.indices) {
                    result[i] = (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
                }
                String(result, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e("OMasterApplication", "AppKey 解混淆失败", e)
                ""
            }
        }

        /**
         * 获取 Application 实例
         * 使用双重检查锁定确保线程安全
         * 注意：多进程场景下每个进程有独立的 Application 实例
         */
        fun getInstance(): OMasterApplication {
            return instance ?: throw IllegalStateException(
                "OMasterApplication 尚未初始化，请在 Application.onCreate 之后调用"
            )
        }

        /**
         * 安全获取 Application 实例（可能返回 null）
         * 用于在不确定初始化状态时访问
         */
        fun getInstanceOrNull(): OMasterApplication? = instance

        fun getPrefs(): SharedPreferences = prefs
    }

    override fun onCreate() {
        super.onCreate()

        // 第 1 步: 初始化基础变量（必须在任何访问前）
        // 使用 synchronized 确保多进程场景下的安全
        synchronized(Companion::class.java) {
            if (instance != null) {
                // 多进程场景：每个进程有自己的 Application 实例，这是正常的
                Log.w("OMasterApplication", "Application 实例在多进程中重新创建: ${android.os.Process.myPid()}")
            }
            instance = this
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        // 第 2 步: 安装全局异常处理器（最早安装,确保后续初始化异常能被捕获）
        // 必须传 context,CrashHandler 才能持久化日志
        try {
            CrashHandler.getInstance().install(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "CrashHandler安装失败", e)
        }

        // 第 3 步: 初始化震动设置（fail-safe 模式,使用默认值兜底）
        try {
            HapticSettings.enabled = SettingsManager.getInstance(this).isVibrationEnabled
        } catch (e: Throwable) {
            android.util.Log.w("OMasterApplication", "HapticSettings初始化失败,使用默认值", e)
        }

        // 第 4 步: 友盟初始化推迟到用户同意隐私政策后
        // 不在 onCreate 中预初始化，确保"未同意不采集"的合规承诺
        // 初始化逻辑移至 initUMeng()，在用户同意隐私政策后调用
    }

    /**
     * 初始化友盟统计
     * 必须在用户明确同意隐私政策后才能调用
     * 确保"未同意不采集"的合规承诺
     */
    fun initUMeng() {
        // 安全检查：用户必须已同意隐私政策
        if (!hasUserAgreed()) {
            android.util.Log.w("OMasterApplication", "用户未同意隐私政策，跳过友盟初始化")
            return
        }

        try {
            UMConfigure.setLogEnabled(false)
            // 使用解混淆后的 AppKey（BuildConfig 中存储的是 XOR+Base64 混淆值）
            val appKey = deobfuscateKey(BuildConfig.UMENG_APPKEY, OBFUSCATION_KEY)
            UMConfigure.init(this, appKey, "default", UMConfigure.DEVICE_TYPE_PHONE, null)
            android.util.Log.i("OMasterApplication", "友盟统计初始化成功")
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "友盟初始化失败", e)
        }
    }

    fun hasUserAgreed(): Boolean {
        return prefs.getBoolean(KEY_USER_AGREED, false)
    }

    fun setUserAgreed(agreed: Boolean) {
        prefs.edit().putBoolean(KEY_USER_AGREED, agreed).apply()
    }

    /**
     * 检查统计开关是否开启
     */
    private fun isAnalyticsEnabled(): Boolean {
        return SettingsManager.getInstance(this).isAnalyticsEnabled
    }

    /**
     * 根据当前开关状态重新初始化或禁用友盟统计
     * 在设置页面切换开关后调用
     */
    fun updateAnalyticsState() {
        if (isAnalyticsEnabled() && hasUserAgreed()) {
            // 开启统计，执行初始化
            initUMeng()
        } else {
            // 关闭统计，禁用数据上报
            // 注意：友盟SDK不支持完全停止，但可以通过以下方式减少数据收集
            MobclickAgent.disable()
        }
    }

    /**
     * 应用终止时释放资源
     * 释放 ML Kit FaceDetector 单例资源和 Ktor HttpClient
     */
    override fun onTerminate() {
        super.onTerminate()
        try {
            FaceDetectorSingleton.release()
            android.util.Log.i("OMasterApplication", "FaceDetectorSingleton 已释放")
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "释放 FaceDetectorSingleton 失败", e)
        }

        // 关闭 Ktor HttpClient，释放连接池和线程资源
        try {
            PresetRepository.getInstance(this).close()
            android.util.Log.i("OMasterApplication", "PresetRepository HttpClient 已关闭")
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "关闭 PresetRepository HttpClient 失败", e)
        }
    }
}
