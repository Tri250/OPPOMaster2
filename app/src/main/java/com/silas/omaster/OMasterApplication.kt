package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.util.CrashReporter
import com.silas.omaster.util.HapticSettings
import com.silas.omaster.util.Logger
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent

class OMasterApplication : Application() {
    companion object {
        private const val PREFS_NAME = "omaster_prefs"
        private const val KEY_USER_AGREED = "user_agreed_to_policy"

        private lateinit var instance: OMasterApplication
        private lateinit var prefs: SharedPreferences

        fun getInstance(): OMasterApplication = instance
        fun getPrefs(): SharedPreferences = prefs
    }

    override fun onCreate() {
        super.onCreate()

        // 【v1.6 启动闪退诊断】最先安装全局崩溃捕获器：
        // 其后任意初始化抛出的未捕获异常都会落盘到 files/logs/crash.log 与
        // Android/data/<pkg>/files/crash.log，解决 release 包真机闪退无日志、根因无法定位的问题。
        CrashReporter.install(this)

        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 初始化日志系统（非关键，失败不阻断冷启动）
        try {
            Logger.init(this)
        } catch (e: Throwable) {
            android.util.Log.e("OMaster", "Logger.init failed (non-fatal)", e)
        }

        // MU-24 / v1.6：注册 ColorOS 17 侧边键快捷键（Find X10 额外注册 AI 闪记）
        com.silas.omaster.util.ShortcutRegistrar.refresh(this)

        // 初始化震动设置（ConfigCenter 构造失败时静默降级，绝不连带阻断冷启动）
        try {
            HapticSettings.enabled = ConfigCenter.getInstance(this).isVibrationEnabled
        } catch (e: Throwable) {
            Logger.w("OMaster", "ConfigCenter init failed in Application.onCreate (non-fatal): ${e.message}")
        }

        // 冷启动性能：后台线程预热预设缓存。
        // 解析 assets/presets.json 属磁盘 IO + JSON 反序列化，首个界面（PresetRepository.init）
        // 会在主线程首次读取；此处提前在后台解析并填充缓存，使主线程读取命中缓存、不再阻塞。
        warmUpPresetCache()

        // 每次冷启动都调用预初始化（不采集数据）
        preInitUMeng()

        // 如果用户已同意隐私政策且统计开关开启，则调用正式初始化
        // 【v1.6 启动闪退诊断】统计初始化读取 ConfigCenter，位于冷启动路径，
        // 任何异常都必须自防御，绝不连带阻断冷启动。
        try {
            if (hasUserAgreed() && isAnalyticsEnabled()) {
                initUMeng()
            }
        } catch (e: Throwable) {
            Logger.w("OMaster", "Analytics init check failed (non-fatal): ${e.message}")
        }
    }

    /**
     * 冷启动性能预热：后台解析预设 JSON 并填充 JsonUtil 内存缓存。
     * 失败不影响启动（缓存缺失时会退回主线程解析，仅性能退化）。
     */
    private fun warmUpPresetCache() {
        try {
            Thread {
                runCatching { com.silas.omaster.util.JsonUtil.loadPresets(this) }
                    .onFailure { Logger.w("OMaster", "preset cache warm-up failed: ${it.message}") }
            }.apply {
                isDaemon = true
                name = "preset-cache-warmup"
            }.start()
        } catch (e: Throwable) {
            Logger.w("OMaster", "preset cache warm-up skipped: ${e.message}")
        }
    }

    /**
     * 预初始化友盟
     * 不会采集设备信息，也不会上报数据
     * 必须在 Application.onCreate 中调用
     *
     * 【v1.6.0 启动闪退修复】友盟 SDK 为外部依赖，其 preInit 在极少数 ROM / 混淆边界下
     * 可能抛异常；该调用位于启动路径且不被 UI 依赖，故降级为可失败，绝不连带阻断冷启动。
     */
    private fun preInitUMeng() {
        try {
            UMConfigure.setLogEnabled(false)
            UMConfigure.preInit(this, "698938eb9a7f3764885bbdaa", "default")
        } catch (e: Throwable) {
            Logger.w("OMaster", "UMeng preInit failed (non-fatal): ${e.message}")
        }
    }

    /**
     * 正式初始化友盟
     * 用户同意隐私政策后才能调用
     * 此时才会采集设备信息并上报数据
     *
     * 【v1.6 启动闪退诊断】友盟初始化同时位于冷启动路径与设置页开关回调，
     * 外部 SDK 在极少数 ROM / 混淆边界下可能抛异常，必须自防御。
     */
    fun initUMeng() {
        try {
            UMConfigure.init(this, "698938eb9a7f3764885bbdaa", "default", UMConfigure.DEVICE_TYPE_PHONE, null)
        } catch (e: Throwable) {
            Logger.w("OMaster", "UMeng init failed (non-fatal): ${e.message}")
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
        return ConfigCenter.getInstance(this).isAnalyticsEnabled
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
}
