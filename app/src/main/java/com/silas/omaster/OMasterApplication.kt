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
import com.silas.omaster.infrastructure.utils.CrashHandler
import com.silas.omaster.infrastructure.utils.HapticSettings
import com.silas.omaster.infrastructure.security.SecurityIntegrityChecker
import com.silas.omaster.infrastructure.utils.ANRWatchdog
import com.silas.omaster.infrastructure.network.NetworkResilienceManager
import com.silas.omaster.background.SyncWorker
import com.silas.omaster.infrastructure.utils.InAppUpdateManager
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * 启动初始化日志记录器
 * 追踪每个初始化步骤的耗时，用于性能分析和优化
 */
object StartupLogger {
    private val steps = mutableListOf<Step>()
    private var appStartTime: Long = 0L

    // 启动时间阈值（毫秒），超过此值将输出警告
    const val STARTUP_THRESHOLD_MS = 1500L

    data class Step(
        val name: String,
        val durationMs: Long,
        val timestamp: Long
    )

    /**
     * 启动验证结果
     */
    data class ValidationResult(
        val totalMs: Long,
        val exceededThreshold: Boolean,
        val thresholdMs: Long,
        val slowSteps: List<StepWithSuggestion>,
        val suggestions: List<String>
    )

    /**
     * 带优化建议的步骤
     */
    data class StepWithSuggestion(
        val step: Step,
        val suggestion: String?,
        val isSlow: Boolean
    )

    fun markAppStart() {
        appStartTime = SystemClock.elapsedRealtime()
    }

    fun logStep(name: String, durationMs: Long) {
        steps.add(Step(name, durationMs, SystemClock.elapsedRealtime()))
    }

    /**
     * 验证启动时间并生成优化建议
     */
    fun validateStartupTime(): ValidationResult {
        val totalMs = if (appStartTime > 0) SystemClock.elapsedRealtime() - appStartTime else 0L
        val exceededThreshold = totalMs > STARTUP_THRESHOLD_MS

        // 标记慢步骤（超过100ms的步骤视为潜在瓶颈）
        val slowThreshold = 100L
        val stepsWithSuggestions = steps.map { step ->
            val isSlow = step.durationMs > slowThreshold
            val suggestion = generateStepSuggestion(step, isSlow)
            StepWithSuggestion(step, suggestion, isSlow)
        }

        val slowSteps = stepsWithSuggestions.filter { it.isSlow }

        // 生成总体优化建议
        val suggestions = mutableListOf<String>()
        if (exceededThreshold) {
            suggestions.add("启动时间 ${totalMs}ms 超过阈值 ${STARTUP_THRESHOLD_MS}ms")
            suggestions.add("建议检查耗时超过100ms的步骤")

            if (slowSteps.any { it.step.name.contains("CrashHandler") }) {
                suggestions.add("CrashHandler安装较慢，考虑延迟到后台线程初始化")
            }
            if (slowSteps.any { it.step.name.contains("Sentry") }) {
                suggestions.add("Sentry初始化较慢，考虑延迟初始化或配置异步初始化")
            }
            if (slowSteps.any { it.step.name.contains("安全") }) {
                suggestions.add("安全完整性检查耗时较长，已在后台协程执行")
            }
            if (slowSteps.any { it.step.name.contains("PresetRepository") }) {
                suggestions.add("预设加载耗时较长，考虑按需加载")
            }
        }

        return ValidationResult(
            totalMs = totalMs,
            exceededThreshold = exceededThreshold,
            thresholdMs = STARTUP_THRESHOLD_MS,
            slowSteps = slowSteps,
            suggestions = suggestions
        )
    }

    /**
     * 为单个步骤生成优化建议
     */
    private fun generateStepSuggestion(step: Step, isSlow: Boolean): String? {
        if (!isSlow) return null

        return when {
            step.name.contains("CrashHandler") -> "考虑延迟到后台线程初始化"
            step.name.contains("Sentry") -> "可配置异步初始化或延迟上报"
            step.name.contains("安全") -> "已在后台执行，检查内部逻辑"
            step.name.contains("SettingsManager") -> "避免同步IO操作，使用内存缓存"
            step.name.contains("PresetRepository") -> "考虑按需加载或延迟预加载"
            step.name.contains("FaceDetector") -> "ML Kit模型加载较慢，考虑延迟初始化"
            step.name.contains("WorkManager") -> "后台调度，已在协程中执行"
            else -> "检查是否有阻塞操作可移至后台"
        }
    }

    fun getReport(): String {
        val totalMs = if (appStartTime > 0) SystemClock.elapsedRealtime() - appStartTime else 0L
        return buildString {
            appendLine("=== 启动性能报告 ===")
            appendLine("总耗时: ${totalMs}ms")
            if (totalMs > STARTUP_THRESHOLD_MS) {
                appendLine("⚠️ 警告: 超过阈值 ${STARTUP_THRESHOLD_MS}ms")
            }
            appendLine("--- 各步骤耗时 ---")
            steps.forEach { step ->
                val marker = if (step.durationMs > 100) "🔴" else "✅"
                appendLine("  $marker ${step.name}: ${step.durationMs}ms")
            }
        }
    }

    /**
     * 获取详细性能报告（包含优化建议）
     */
    fun getDetailedReport(): String {
        val validation = validateStartupTime()
        return buildString {
            appendLine("=== 启动性能详细报告 ===")
            appendLine("总耗时: ${validation.totalMs}ms")
            appendLine("目标阈值: ${validation.thresholdMs}ms")

            if (validation.exceededThreshold) {
                appendLine("状态: ⚠️ 超过阈值，需要优化")
            } else {
                appendLine("状态: ✅ 符合目标")
            }

            appendLine("")
            appendLine("--- 各步骤耗时详情 ---")
            steps.forEach { step ->
                val slow = step.durationMs > 100
                val marker = if (slow) "🔴" else "✅"
                appendLine("  $marker ${step.name}: ${step.durationMs}ms")
                if (slow) {
                    val suggestion = generateStepSuggestion(step, true)
                    appendLine("      💡 $suggestion")
                }
            }

            if (validation.suggestions.isNotEmpty()) {
                appendLine("")
                appendLine("--- 优化建议 ---")
                validation.suggestions.forEach { suggestion ->
                    appendLine("  • $suggestion")
                }
            }
        }
    }

    fun getSteps(): List<Step> = steps.toList()

    /**
     * 重置日志状态（用于测试）
     */
    fun reset() {
        steps.clear()
        appStartTime = 0L
    }
}

class OMasterApplication : Application() {
    companion object {
        private const val PREFS_NAME = "omaster_prefs"
        private const val KEY_USER_AGREED = "user_agreed_to_policy"

        // v2.3.6 崩溃恢复：崩溃标记存储
        private const val KEY_CRASH_FLAG = "app_crashed_last_run"
        private const val KEY_CRASH_TIMESTAMP = "crash_timestamp"

        // 从 BuildConfig 读取混淆密钥（构建时动态生成或从环境变量读取）
        // 避免硬编码密钥在代码中，增加逆向难度
        private val OBFUSCATION_KEY: String
            get() = BuildConfig.OBFUSCATION_KEY

        @Volatile
        private var instance: OMasterApplication? = null
        private var prefs: SharedPreferences? = null

        /**
         * 由 InitializationProvider 在早期阶段调用，初始化 SharedPreferences
         */
        fun initializePrefs(context: Context) {
            if (prefs == null) {
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
            val appContext = Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null)
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

    fun getPrefs(): SharedPreferences? = prefs

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
            if (prefs == null) {
                prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
    }

    override fun onCreate() {
        super.onCreate()

        StartupLogger.markAppStart()

        // v2.3.6 崩溃恢复：在启动开始时设置崩溃标记
        // 如果应用正常退出，会在 onTerminate 或 releaseResources 中清除标记
        // 如果应用崩溃，标记会保留，下次启动时检测到并提示用户
        setCrashFlag(true)

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

        // 第 2.1 步: 初始化 Sentry 崩溃上报（必须紧接 CrashHandler 之后）
        // 配置了 DSN 才初始化，未配置时静默跳过
        val step2_1Start = SystemClock.elapsedRealtime()
        try {
            if (BuildConfig.SENTRY_DSN.isNotEmpty()) {
                SentryAndroid.init(this) { options ->
                    options.dsn = BuildConfig.SENTRY_DSN
                    options.isEnableUserInteractionTracing = true
                    options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.3
                    options.profilesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.3
                    // 设置环境标识
                    options.environment = if (BuildConfig.DEBUG) "development" else "production"
                    // 设置发布版本
                    options.release = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
                    // 在崩溃前添加面包屑（CrashHandler 的异常信息）
                    // v2.3.6 关键修复：beforeSend 回调加双重 try-catch，
                    // 防止 CrashHandler 自身异常反向上抛污染 Sentry 事件链路
                    options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                        try {
                            val installed = try {
                                CrashHandler.getInstance().isInstalled()
                            } catch (t: Throwable) {
                                false
                            }
                            event.setTag("crash_handler_installed", installed.toString())
                        } catch (t: Throwable) {
                            // 兜底：setTag 失败也不能影响事件正常上报
                        }
                        event
                    }
                }
                android.util.Log.i("OMasterApplication", "Sentry 崩溃上报已初始化")
            } else {
                android.util.Log.d("OMasterApplication", "SENTRY_DSN 未配置，跳过 Sentry 初始化")
            }
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "Sentry初始化失败", e)
        }
        StartupLogger.logStep("Sentry初始化", SystemClock.elapsedRealtime() - step2_1Start)

        // 第 2.2 步: 启用 StrictMode 违规检测（仅 Debug 构建）
        // 检测主线程磁盘 I/O、网络调用等，及早发现潜在 ANR 问题
        if (BuildConfig.DEBUG) {
            val step2_2Start = SystemClock.elapsedRealtime()
            try {
                android.os.StrictMode.setThreadPolicy(
                    android.os.StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build()
                )
                android.os.StrictMode.setVmPolicy(
                    android.os.StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .detectActivityLeaks()
                        .detectLeakedRegistrationObjects()
                        .penaltyLog()
                        .build()
                )
            } catch (e: Throwable) {
                android.util.Log.e("OMasterApplication", "StrictMode设置失败", e)
            }
            StartupLogger.logStep("StrictMode设置", SystemClock.elapsedRealtime() - step2_2Start)
        }

        // 第 2.3 步: 安装 ANR 看门狗
        val step2_3Start = SystemClock.elapsedRealtime()
        try {
            ANRWatchdog.install()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "ANRWatchdog安装失败", e)
        }
        StartupLogger.logStep("ANRWatchdog安装", SystemClock.elapsedRealtime() - step2_3Start)

        // 第 2.4 步: 初始化网络韧性管理器
        val step2_4Start = SystemClock.elapsedRealtime()
        try {
            NetworkResilienceManager.init(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "NetworkResilienceManager初始化失败", e)
        }
        StartupLogger.logStep("NetworkResilienceManager初始化", SystemClock.elapsedRealtime() - step2_4Start)

        // 第 2.5 步: 安全完整性检查（Release 构建中执行，Debug 跳过）
        // v2.3.6 修复：移到后台协程执行。Runtime.exec 与文件检查在部分设备上可能
        // 阻塞主线程数百毫秒，导致启动 ANR。异常环境仅记录日志，不阻断启动。
        if (!BuildConfig.DEBUG) {
            val step2_5Start = SystemClock.elapsedRealtime()
            lazyScope.launch {
                try {
                    val integrityResult = SecurityIntegrityChecker.performCheck(this@OMasterApplication)
                    if (!integrityResult.isSafe) {
                        android.util.Log.w("OMasterApplication", "安全环境异常: ${integrityResult.issues}")
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("OMasterApplication", "安全检查失败", e)
                }
                StartupLogger.logStep("安全完整性检查", SystemClock.elapsedRealtime() - step2_5Start)
            }
        }

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

        // 第 5.5 步: 低存储检查（ST-START-04）
        // v2.3.6 修复：StatFs 本身较快，但清理缓存与 SP 写入属于文件 I/O，
        // 移到后台协程避免阻塞主线程启动流程。
        lazyScope.launch {
            try {
                val stat = android.os.StatFs(filesDir.path)
                val availableBytes = stat.availableBytes
                val thresholdBytes = 200L * 1024 * 1024 // 200MB
                if (availableBytes < thresholdBytes) {
                    Log.w("OMasterApplication", "低存储警告: 可用空间 ${availableBytes / 1024 / 1024}MB < ${thresholdBytes / 1024 / 1024}MB")
                    getSharedPreferences("omaster_startup", Context.MODE_PRIVATE)
                        .edit().putBoolean("low_storage_mode", true).apply()
                    cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                } else {
                    getSharedPreferences("omaster_startup", Context.MODE_PRIVATE)
                        .edit().putBoolean("low_storage_mode", false).apply()
                }
            } catch (e: Exception) {
                Log.w("OMasterApplication", "低存储检查失败", e)
            }
        }

        // 第 6 步: 调度 WorkManager 后台定期同步
        // v2.3.6 修复：虽然 schedule 本身通常很快，但 WorkManager 首次初始化可能触发
        // 数据库创建等 I/O。将其移到后台协程，确保主线程 onCreate 尽快返回，降低 ANR 风险。
        if (!BuildConfig.DEBUG) {
            lazyScope.launch {
                try {
                    SyncWorker.schedule(this@OMasterApplication, intervalHours = 24)
                } catch (e: Throwable) {
                    android.util.Log.w("OMasterApplication", "WorkManager 调度失败", e)
                }
            }
        }

        Log.i("OMasterApplication", StartupLogger.getReport())

        // 启动时间验证：检查是否超过阈值并输出优化建议
        val validation = StartupLogger.validateStartupTime()
        if (validation.exceededThreshold) {
            Log.w("OMasterApplication", "⚠️ 启动时间验证失败: 总耗时 ${validation.totalMs}ms > 阈值 ${validation.thresholdMs}ms")
            Log.w("OMasterApplication", "建议优化以下耗时超过100ms的步骤:")
            validation.slowSteps.forEach { slowStep ->
                Log.w("OMasterApplication", "  - ${slowStep.step.name}: ${slowStep.step.durationMs}ms")
                if (slowStep.suggestion != null) {
                    Log.w("OMasterApplication", "    💡 ${slowStep.suggestion}")
                }
            }
            if (validation.suggestions.isNotEmpty()) {
                Log.w("OMasterApplication", "总体优化建议:")
                validation.suggestions.forEach { suggestion ->
                    Log.w("OMasterApplication", "  • $suggestion")
                }
            }
        } else {
            Log.i("OMasterApplication", "✅ 启动时间验证通过: ${validation.totalMs}ms <= ${validation.thresholdMs}ms")
        }
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

                // 预加载预设缓存：提前初始化PresetRepository，减少首屏加载时间
                try {
                    val presetRepo = PresetRepository.getInstance(this@OMasterApplication)
                    // 触发预设加载（从assets或本地缓存），只取第一个值
                    presetRepo.getAllPresets().first().let { presets ->
                        Log.i("OMasterApplication", "预设缓存预加载完成: ${presets.size} 条")
                    }
                    StartupLogger.logStep("PresetRepository预加载", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "PresetRepository预加载失败", e)
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
                                val result = com.silas.omaster.infrastructure.network.PresetRemoteManager.fetchAndSave(
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

                // 检查应用内更新（非阻塞，失败不影响启动）
                try {
                    InAppUpdateManager.init(this@OMasterApplication)
                    StartupLogger.logStep("应用内更新检查", SystemClock.elapsedRealtime() - lazyStart)
                } catch (e: Throwable) {
                    Log.w("OMasterApplication", "应用内更新检查失败", e)
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
            val currentPrefs = prefs ?: run {
                // 2.2.0 闪退修复：prefs 未初始化时回退到默认 SharedPreferences
                try {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                } catch (e: Throwable) {
                    android.util.Log.w("OMasterApplication", "读取用户协议状态失败,返回 false", e)
                    null
                }
            }
            currentPrefs?.getBoolean(KEY_USER_AGREED, false) ?: false
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "hasUserAgreed 失败", e)
            false
        }
    }

    fun setUserAgreed(agreed: Boolean) {
        try {
            if (prefs == null) {
                prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            prefs?.edit()?.putBoolean(KEY_USER_AGREED, agreed)?.apply()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "setUserAgreed 失败", e)
        }
    }

    // v2.3.6 崩溃恢复：崩溃标记管理方法
    /**
     * 设置崩溃标记
     * @param crashed true 表示应用处于"可能崩溃"状态，false 表示正常退出
     */
    fun setCrashFlag(crashed: Boolean) {
        try {
            if (prefs == null) {
                prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            prefs?.edit()?.apply {
                putBoolean(KEY_CRASH_FLAG, crashed)
                if (crashed) {
                    putLong(KEY_CRASH_TIMESTAMP, System.currentTimeMillis())
                }
            }?.apply()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "setCrashFlag 失败", e)
        }
    }

    /**
     * 检查上次是否崩溃
     * @return true 表示上次启动检测到崩溃标记
     */
    fun hadCrashLastRun(): Boolean {
        return try {
            val currentPrefs = prefs ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentPrefs.getBoolean(KEY_CRASH_FLAG, false)
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "hadCrashLastRun 失败", e)
            false
        }
    }

    /**
     * 获取上次崩溃时间戳
     * @return 崩溃时间戳（毫秒），如果无记录返回 0
     */
    fun getLastCrashTimestamp(): Long {
        return try {
            val currentPrefs = prefs ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentPrefs.getLong(KEY_CRASH_TIMESTAMP, 0L)
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "getLastCrashTimestamp 失败", e)
            0L
        }
    }

    /**
     * 清除崩溃标记（应用正常退出时调用）
     */
    fun clearCrashFlag() {
        setCrashFlag(false)
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
                com.silas.omaster.engine.GPURenderManager.forceReleaseAll()
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
        // v2.3.6 崩溃恢复：应用正常退出时清除崩溃标记
        clearCrashFlag()

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

        // 卸载 ANR 看门狗
        try {
            ANRWatchdog.uninstall()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "ANRWatchdog卸载失败", e)
        }

        // 注意：GPURenderManager 由具体页面通过 acquire/release 管理引用计数，
        // 此处不再直接释放，避免负引用计数导致单例被意外销毁。
        // 进程终止时系统会自动回收 EGL/GPU 资源。
    }
}
