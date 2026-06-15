package com.silas.omaster

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.util.CrashHandler
import com.silas.omaster.util.HapticSettings

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

        // 第 3 步: 初始化震动设置（fail-safe 模式,使用默认值兜底）
        try {
            HapticSettings.enabled = SettingsManager.getInstance(this).isVibrationEnabled
        } catch (e: Throwable) {
            android.util.Log.w("OMasterApplication", "HapticSettings初始化失败,使用默认值", e)
        }
    }

    fun hasUserAgreed(): Boolean {
        return prefs.getBoolean(KEY_USER_AGREED, false)
    }

    fun setUserAgreed(agreed: Boolean) {
        prefs.edit().putBoolean(KEY_USER_AGREED, agreed).apply()
    }
}
