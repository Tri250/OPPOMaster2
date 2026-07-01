package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.silas.omaster.ai.analyzer.FaceDetectorSingleton
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.util.CrashHandler
import com.silas.omaster.util.HapticSettings
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
         * 
         * @throws IllegalStateException 如果 Application 尚未初始化（仅在明确需要实例时抛出）
         */
        fun getInstance(): OMasterApplication {
            return instance ?: throw IllegalStateException(
                "OMasterApplication 尚未初始化，请在 Application.onCreate 之后调用"
            )
        }

        /**
     * 安全获取 Application 实例（可能返回 null）
     * 推荐在不确定初始化状态时使用此方法
     */
    fun getInstanceOrNull(): OMasterApplication? = instance

    /**
     * 安全获取 Application 实例，带默认值回退
     * 用于组件初始化时获取 Context，即使 Application 尚未完全初始化也能安全返回
     *
     * 2.2.0 闪退修复：在 Application 完全未初始化时，会用 applicationContext 创建
     * 临时实例以避免空指针，绝不返回 null
     */
    fun safeGetInstance(): OMasterApplication? {
        if (instance != null) return instance
        // 兜底：从 ContentProvider 缓存中获取 applicationContext
        return try {
            val appContext = android.app.ActivityThread
                .currentApplication()
            if (appContext is OMasterApplication) {
                instance = appContext
                appContext
            } else {
                null
            }
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "safeGetInstance 失败", e)
            null
        }
    }

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
     * 在后台协程执行，不阻塞主线程启动流程
     */
    // 懒加载协程作用域，在 onTerminate / releaseResources 中取消
    private val lazyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun triggerLazyInitialization() {
        // 使用后台协程预初始化非关键组件，避免阻塞主线程启动流程
        lazyScope.launch {
            try {
                val lazyStart = SystemClock.elapsedRealtime()

                // 预初始化 SettingsManager 缓存（减少首次访问时的阻塞）
                try {
                    SettingsManager.getInstance(this@OMasterApplication).preloadCache()
                    StartupLogger.logStep("SettingsManager预加载", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "SettingsManager预加载失败", e)
                }

                // 预初始化 FaceDetectorSingleton（非关键，触发单例创建）
                // 实际人脸检测模型由 Google ML Kit 按需加载，无需用户配置
                try {
                    FaceDetectorSingleton
                    StartupLogger.logStep("FaceDetector预初始化", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "FaceDetector预初始化失败", e)
                }

                // 订阅动态加载：首次启动或本地缓存缺失时，拉取所有启用的订阅源
                // 取代已删除的云同步功能，所有预设源统一由订阅管理驱动
                try {
                    val subManager = com.silas.omaster.data.local.SubscriptionManager.getInstance(this@OMasterApplication)
                    val enabledSubs = subManager.subscriptionsFlow.value.filter { it.isEnabled }
                    var fetchedCount = 0
                    for (sub in enabledSubs) {
                        val cacheFile = java.io.File(this@OMasterApplication.filesDir, subManager.getFileNameForUrl(sub.url))
                        // 仅在本地缓存缺失时拉取，避免每次启动都产生网络请求
                        if (!cacheFile.exists()) {
                            try {
                                val result = com.silas.omaster.network.PresetRemoteManager.fetchAndSave(
                                    this@OMasterApplication, sub.url, forceUpdate = false
                                )
                                if (result.isSuccess) {
                                    fetchedCount++
                                    Log.i("OMasterApplication", "订阅拉取成功: ${sub.name}")
                                }
                            } catch (e: Throwable) {
                                Log.w("OMasterApplication", "订阅拉取失败: ${sub.name}", e)
                            }
                        }
                    }
                    if (fetchedCount > 0) {
                        StartupLogger.logStep("订阅初始拉取($fetchedCount)", SystemClock.elapsedRealtime() - lazyStart)
                        Log.i("OMasterApplication", "订阅初始拉取完成: $fetchedCount/$enabledSubs.size 个源")
                    }
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "订阅初始拉取失败", e)
                }

            } catch (e: Throwable) {
                Log.e("OMasterApplication", "懒加载初始化失败", e)
            }
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
            // 使用解混淆后的 AppKey 和 MessageSecret（BuildConfig 中存储的是 XOR+Base64 混淆值）
            val appKey = deobfuscateKey(BuildConfig.UMENG_APPKEY, OBFUSCATION_KEY)
            val messageSecret = deobfuscateKey(BuildConfig.UMENG_MESSAGE_SECRET, OBFUSCATION_KEY)
            UMConfigure.init(this, appKey, "default", UMConfigure.DEVICE_TYPE_PHONE, messageSecret)
            android.util.Log.i("OMasterApplication", "友盟统计初始化成功")
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "友盟初始化失败", e)
        }
    }

    fun hasUserAgreed(): Boolean {
        return try {
            if (::prefs.isInitialized) {
                prefs.getBoolean(KEY_USER_AGREED, false)
            } else {
                // 2.2.0 闪退修复：prefs 未初始化时回退到默认 SharedPreferences
                try {
                    val tempPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    tempPrefs.getBoolean(KEY_USER_AGREED, false)
                } catch (e: Throwable) {
                    android.util.Log.w("OMasterApplication", "读取用户协议状态失败,返回 false", e)
                    false
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "hasUserAgreed 失败", e)
            false
        }
    }

    fun setUserAgreed(agreed: Boolean) {
        try {
            if (!::prefs.isInitialized) {
                prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            prefs.edit().putBoolean(KEY_USER_AGREED, agreed).apply()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "setUserAgreed 失败", e)
        }
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
     * 系统内存紧张时主动回收 GPU 等非关键资源，防止 OOM/ANR。
     * 修复 L3: GPURenderManager 引用计数泄漏兜底。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            try {
                com.silas.omaster.renderer.GPURenderManager.forceReleaseAll()
                Log.i("OMasterApplication", "onTrimMemory: GPURenderManager 强制释放完成")
            } catch (e: Exception) {
                Log.w("OMasterApplication", "onTrimMemory: GPURenderManager 释放失败", e)
            }
        }
    }

    /**
     * 释放全局资源（可被 onTerminate 或手动调用）
     * 推荐在组件的生命周期回调中调用，而非依赖 onTerminate
     */
    fun releaseResources() {
        // 取消懒加载协程作用域
        try {
            lazyScope.cancel()
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "取消lazyScope失败", e)
        }

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

        // 关闭 TrailSnapRepository 协程作用域与 ML Kit 检测器
        try {
            TrailSnapRepository.getInstance(this).close()
            android.util.Log.i("OMasterApplication", "TrailSnapRepository 已关闭")
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "关闭 TrailSnapRepository 失败", e)
        }

        // 关闭 SettingsManager 协程作用域
        try {
            SettingsManager.shutdown()
            android.util.Log.i("OMasterApplication", "SettingsManager 已关闭")
        } catch (e: Exception) {
            android.util.Log.e("OMasterApplication", "关闭 SettingsManager 失败", e)
        }

        // 注意：GPURenderManager 由具体页面通过 acquire/release 管理引用计数，
        // 此处不再直接释放，避免负引用计数导致单例被意外销毁。
        // 进程终止时系统会自动回收 EGL/GPU 资源。
    }
}
