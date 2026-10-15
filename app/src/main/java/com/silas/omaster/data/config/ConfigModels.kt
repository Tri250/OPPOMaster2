package com.silas.omaster.data.config

import com.silas.omaster.data.local.AppLanguage
import com.silas.omaster.data.local.FloatingWindowMode
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.ui.theme.BrandTheme

/**
 * 悬浮窗完整配置
 *
 * MU-27 / v1.6 扩展：
 * - mode              STANDARD | COMPACT | FRAMING（取景框避位）
 * - followCameraFraming  是否在相机应用前台时自动切 FRAMING 模式（默认 true，ColorOS 17 / Find X10 推荐开启）
 */
data class FloatingWindowConfig(
    val opacity: Int = 56,
    val mode: FloatingWindowMode = FloatingWindowMode.COMPACT,
    val followCameraFraming: Boolean = true
)

/**
 * UI 配置组合
 */
data class UiConfig(
    val theme: BrandTheme = BrandTheme.Hasselblad,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val darkMode: Boolean = true
)

/**
 * 系统配置组合
 */
data class SystemConfig(
    val updateChannel: UpdateChannel = UpdateChannel.GITEE,
    val defaultPresetUrl: String = SubscriptionConfig.DEFAULT_PRESET_URL,
    val realmePresetUrl: String = SubscriptionConfig.REALME_PRESET_URL
)

/**
 * 用户偏好配置组合
 */
data class UserPreferences(
    val vibrationEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val defaultStartTab: Int = 0
)
