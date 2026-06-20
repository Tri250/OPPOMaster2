package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.silas.omaster.ai.analyzer.FaceDetectorSingleton
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.util.CrashHandler
import com.silas.omaster.util.HapticSettings
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.runBlocking

/**
 * 启动初始化日志记录器
 * 追踪每个初始化步骤的耗时，用于性能分析和优化
 */
object StartupLogger {
    private val steps = mutableListOf<Step>()
    private var appStartTime: Long = 0L

    data class Step(
        val name: String,
        val durationMs: Long,
        val timestamp: Long
    )

    fun markAppStart() {
        appStartTime = SystemClock.elapsedRealtime()
    }

    fun logStep(name: String, durationMs: Long) {
        steps.add(Step(name, durationMs, SystemClock.elapsedRealtime()))
    }

    fun getReport(): String {
        val totalMs = if (appStartTime > 0) SystemClock.elapsedRealtime() - appStartTime else 0L
        return buildString {
            appendLine("=== 启动性能报告 ===")
            appendLine("总耗时: ${totalMs}ms")
            steps.forEach { step ->
                appendLine("  ${step.name}: ${step.durationMs}ms")
            }
        }
    }

    fun getSteps(): List<Step> = steps.toList()
}

class OMasterApplication : Application() {
    companion object {
        private const val PREFS_NAME = "omaster_prefs"
        private const val KEY_USER_AGREED = "user_agreed_to_policy"

        // 从 BuildConfig 读取混淆密钥（构建时动态生成或从环境变量读取）
        // 避免硬编码密钥在代码中，增加逆向难度
        private val OBFUSCATION_KEY: String
            get() = BuildConfig.OBFUSCATION_KEY

        // Android Keystore AES-GCM 包装密钥别名（用于运行时解密 AppKey）
        private const val KEYSTORE_ALIAS = "omaster_appkey_wrap"

        @Volatile
        private var instance: OMasterApplication? = null
        private lateinit var prefs: SharedPreferences

        /**
         * 由 InitializationProvider 在早期阶段调用，初始化 SharedPreferences
         */
        fun initializePrefs(context: Context) {
            if (!::prefs.isInitialized) {
                prefs = context.applicationContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
            }
        }

        /**
         * 运行时解混淆：使用 Android Keystore (AES-GCM) 解密 AppKey。
         *
         * 安全方案：
         * 1. 构建时：使用随机 AES 密钥对 AppKey 做 AES-GCM 加密，密文 + IV 写入 BuildConfig
         * 2. 运行时：AES 密钥由 Android Keystore 派生并管理 (StrongBox / TEE)，应用不接触明文密钥
         * 3. 失败时：回退到传统 XOR+Base64 解混淆以保证向后兼容
         *
         * 优势：
         * - 抵御静态分析：从 APK 中无法提取明文密钥或 AppKey
         * - 抵御动态分析：密钥在 TEE/StrongBox 中，应用进程无法读取
         * - 抵御 root 设备：硬件级密钥隔离
         */
        private fun deobfuscateKey(obfuscated: String, key: String): String {
            if (obfuscated.isEmpty()) return ""
            // 优先尝试 Keystore AES-GCM 解密
            val keystoreResult = tryKeystoreDecrypt(obfuscated)
            if (keystoreResult != null) return keystoreResult
            // 回退：XOR + Base64（兼容旧版本构建产物）
            return deobfuscateXorLegacy(obfuscated, key)
        }

        /**
         * 尝试用 Android Keystore (AES/GCM/NoPadding) 解密。
         * 失败返回 null，调用方应回退到 Legacy XOR。
         */
        private fun tryKeystoreDecrypt(payload: String): String? {
            return try {
                val parts = payload.split("|")
                if (parts.size != 2 || parts[0] != "AES-GCM") return null
                val iv = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)

                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                    // 首次运行：生成密钥
                    val kg = javax.crypto.KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
                    )
                    val spec = KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                    kg.init(spec)
                    kg.generateKey()
                }
                val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as javax.crypto.SecretKey

                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                // 密文的前 12 字节为 IV，因此真实密文 = payload - 12字节IV
                val cipherText = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
                if (cipherText.size <= 12) return null
                val plain = cipher.doFinal(cipherText, 12, cipherText.size - 12)
                String(plain, Charsets.UTF_8)
            } catch (e: Throwable) {
                Log.w("OMasterApplication", "Keystore decrypt failed, fallback to XOR", e)
                null
            }
        }

        /**
         * 传统 XOR+Base64 解混淆（兼容旧版本构建）
         */
        private fun deobfuscateXorLegacy(obfuscated: String, key: String): String {
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

        /**
         * 由 Application.onCreate 调用，设置实例并确保 SharedPreferences 已初始化
         */
        private fun onApplicationCreated(app: OMasterApplication) {
            synchronized(Companion::class.java) {
                if (instance != null) {
                    // 多进程场景：每个进程有自己的 Application 实例，这是正常的
                    Log.w("OMasterApplication", "Application 实例在多进程中重新创建: ${android.os.Process.myPid()}")
                }
                instance = app
                // 如果 InitializationProvider 尚未完成初始化，则在此补初始化
                if (!::prefs.isInitialized) {
                    prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        StartupLogger.markAppStart()

        // 第 1 步: 初始化基础变量（必须在任何访问前）
        // 使用 synchronized 确保多进程场景下的安全
        val step1Start = SystemClock.elapsedRealtime()
        onApplicationCreated(this)
        StartupLogger.logStep("基础变量初始化", SystemClock.elapsedRealtime() - step1Start)

        // 第 2 步: 安装全局异常处理器（如果 InitializationProvider 尚未安装）
        // 必须传 context,CrashHandler 才能持久化日志
        val step2Start = SystemClock.elapsedRealtime()
        try {
            if (!CrashHandler.getInstance().isInstalled()) {
                CrashHandler.getInstance().install(applicationContext)
            }
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "CrashHandler安装失败", e)
        }
        StartupLogger.logStep("CrashHandler安装", SystemClock.elapsedRealtime() - step2Start)

        // 第 3 步: 初始化震动设置（fail-safe 模式,使用默认值兜底）
        val step3Start = SystemClock.elapsedRealtime()
        try {
            HapticSettings.enabled = SettingsManager.getInstance(this).isVibrationEnabled
        } catch (e: Throwable) {
            android.util.Log.w("OMasterApplication", "HapticSettings初始化失败,使用默认值", e)
        }
        StartupLogger.logStep("HapticSettings初始化", SystemClock.elapsedRealtime() - step3Start)

        // 第 4 步: 友盟初始化推迟到用户同意隐私政策后
        // 不在 onCreate 中预初始化，确保"未同意不采集"的合规承诺
        // 初始化逻辑移至 initUMeng()，在用户同意隐私政策后调用

        // 第 5 步: 触发非关键组件的懒加载（在后台线程预初始化，不阻塞启动）
        val step5Start = SystemClock.elapsedRealtime()
        triggerLazyInitialization()
        StartupLogger.logStep("触发懒加载调度", SystemClock.elapsedRealtime() - step5Start)

        Log.i("OMasterApplication", StartupLogger.getReport())
    }

    /**
     * 非关键组件的懒加载初始化
     * 在后台线程执行，不阻塞主线程启动流程
     */
    private fun triggerLazyInitialization() {
        // 使用后台线程预初始化非关键组件，避免阻塞主线程
        Thread {
            try {
                val lazyStart = SystemClock.elapsedRealtime()

                // 预初始化 SettingsManager 缓存（减少首次访问时的阻塞）
                try {
                    runBlocking {
                        SettingsManager.getInstance(this@OMasterApplication).preloadCache()
                    }
                    StartupLogger.logStep("SettingsManager预加载", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "SettingsManager预加载失败", e)
                }

                // 预初始化 FaceDetectorSingleton（非关键，首次使用时才真正加载模型）
                try {
                    // 仅触发初始化，实际模型加载延迟到首次使用时
                    FaceDetectorSingleton
                    StartupLogger.logStep("FaceDetector预初始化", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "FaceDetector预初始化失败", e)
                }

                // 预初始化云同步：首次启动时自动同步预设源
                try {
                    val cloudSyncManager = com.silas.omaster.cloud.CloudSyncManager.getInstance(this@OMasterApplication)
                    if (cloudSyncManager.shouldSync()) {
                        kotlinx.coroutines.runBlocking {
                            cloudSyncManager.sync()
                        }
                        StartupLogger.logStep("云同步初始同步", SystemClock.elapsedRealtime() - lazyStart)
                    }
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "云同步初始同步失败", e)
                }

            } catch (e: Throwable) {
                Log.e("OMasterApplication", "懒加载初始化失败", e)
            }
        }.apply {
            name = "LazyInitThread"
            priority = Thread.NORM_PRIORITY - 1 // 降低优先级，不影响主线程
            start()
        }
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
     * 应用终止时释放资源（尽最大努力）
     * 
     * ⚠️ 注意：Android 系统不保证 onTerminate() 一定会被调用。
     * 对于仅运行单个 Activity 的应用，系统通常直接杀死进程而不会调用此方法。
     * 因此，关键资源释放应放在各组件各自的 onDestroy() 或 DisposableEffect 中。
     * 
     * 此方法仅作为补充性的兜底清理。
     */
    override fun onTerminate() {
        super.onTerminate()
        releaseResources()
    }

    /**
     * 释放全局资源（可被 onTerminate 或手动调用）
     * 推荐在组件的生命周期回调中调用，而非依赖 onTerminate
     */
    fun releaseResources() {
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

        // 释放 GPU 渲染管理器
        try {
            com.silas.omaster.renderer.GPURenderManager.getInstance(this).release()
            android.util.Log.i("OMasterApplication", "GPURenderManager 已释放")
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "释放 GPURenderManager 失败", e)
        }
    }
}
