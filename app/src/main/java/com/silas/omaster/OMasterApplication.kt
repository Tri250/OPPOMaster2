package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.util.CrashHandler
import com.silas.omaster.util.HapticSettings
import com.silas.omaster.util.MemoryOptimizer
import com.silas.omaster.util.PerformanceOptimizer
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

        // 第 1 步: 初始化基础变量（必须在任何访问前）
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 第 2 步: 安装全局异常处理器（最早安装,确保后续初始化异常能被捕获）
        // 必须传 context,CrashHandler 才能持久化日志
        try {
            CrashHandler.getInstance().install(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "CrashHandler安装失败", e)
        }

        // 第 3 步: 初始化性能优化（严格模式等）
        try {
            PerformanceOptimizer.initStrictMode(BuildConfig.DEBUG)
            // 注册内存优化回调
            registerComponentCallbacks(MemoryOptimizer(this))
        } catch (e: Throwable) {
            android.util.Log.w("OMasterApplication", "性能优化初始化失败", e)
        }

        // 第 4 步: 初始化震动设置（fail-safe 模式,使用默认值兜底）
        try {
            HapticSettings.enabled = SettingsManager.getInstance(this).isVibrationEnabled
        } catch (e: Throwable) {
            android.util.Log.w("OMasterApplication", "HapticSettings初始化失败,使用默认值", e)
        }

        // 第 5 步: 预初始化友盟（不采集数据,任何异常都不应阻塞启动）
        try {
            preInitUMeng()
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "友盟预初始化失败", e)
        }

        // 第 6 步: 如果用户已同意隐私政策且统计开关开启,则正式初始化
        try {
            if (hasUserAgreed() && isAnalyticsEnabled()) {
                initUMeng()
            }
        } catch (e: Throwable) {
            android.util.Log.e("OMasterApplication", "友盟正式初始化失败", e)
        }
    }

    /**
     * 预初始化友盟
     * 不会采集设备信息，也不会上报数据
     * 必须在 Application.onCreate 中调用
     */
    private fun preInitUMeng() {
        UMConfigure.setLogEnabled(false)
        // 使用 BuildConfig 中的 AppKey（从 gradle.properties 注入，避免硬编码）
        UMConfigure.preInit(this, BuildConfig.UMENG_APPKEY, "default")
    }

    /**
     * 正式初始化友盟
     * 用户同意隐私政策后才能调用
     * 此时才会采集设备信息并上报数据
     */
    fun initUMeng() {
        // 使用 BuildConfig 中的 AppKey（从 gradle.properties 注入，避免硬编码）
        UMConfigure.init(this, BuildConfig.UMENG_APPKEY, "default", UMConfigure.DEVICE_TYPE_PHONE, null)
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
}
