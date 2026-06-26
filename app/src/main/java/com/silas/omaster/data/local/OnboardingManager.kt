package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * 首次启动引导管理器
 * 管理 Onboarding 是否已展示，支持版本升级后重新展示
 */
class OnboardingManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    /**
     * 检查是否需要展示引导页
     *
     * 展示条件：
     *  1. 首次安装（has_seen_onboarding=false）；或
     *  2. 上次观看引导页的版本号 < [ONBOARDING_CONTENT_VERSION]（即引导页内容已更新）
     *
     * 注意：[ONBOARDING_CONTENT_VERSION] 是「引导页内容版本」，与应用 [currentVersionCode]
     * 解耦。每次发版不一定要弹引导页；只有当引导页内容确实发生变更（新增页、改文案、
     * 新功能引导等）时，才将 [ONBOARDING_CONTENT_VERSION] 上调到对应的版本号。
     */
    fun shouldShowOnboarding(currentVersionCode: Long): Boolean {
        val lastShownVersion = prefs.getLong(KEY_LAST_SHOWN_VERSION, 0L)
        val hasSeen = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        // P2-7：用 ONBOARDING_CONTENT_VERSION 替换原硬编码 10900 魔法数字。
        // 原表达式 `lastShownVersion < currentVersionCode && currentVersionCode >= 10900`
        // 在 2.x 时代 10900 阈值恒真，退化为「每次发版都重弹引导」——
        // 与「仅在引导内容变更时重弹」的产品意图不符。
        return !hasSeen || lastShownVersion < ONBOARDING_CONTENT_VERSION
    }

    /**
     * 标记引导页已展示
     */
    fun markOnboardingShown(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    /**
     * 跳过引导页（用户主动跳过）
     */
    fun skipOnboarding(versionCode: Long) {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
            .putLong(KEY_LAST_SHOWN_VERSION, versionCode)
            .apply()
    }

    companion object {
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"

        /**
         * 引导页内容版本（对应 versionCode 计算：major*10000 + minor*100 + patch）
         *
         * 当引导页内容发生变更（新增页面、改文案、新功能引导等）时，将此常量上调到
         * 对应的 versionCode，老用户上次观看版本低于此值时会在下次启动时重新看到引导。
         *
         * 历史值：
         *  - 1.9.0 (10900)：原始引导系统引入版本（原硬编码阈值）
         *  - 2.1.0 (20100)：当前版本——为 OPPO Find X9 哈苏大师 + Android 16 体验重新设计引导内容
         */
        private const val ONBOARDING_CONTENT_VERSION = 20100L

        @Volatile
        private var instance: OnboardingManager? = null

        fun getInstance(context: Context): OnboardingManager {
            return instance ?: synchronized(this) {
                instance ?: OnboardingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}