package com.silas.omaster.model

import android.os.Build
import kotlinx.serialization.Serializable

/**
 * MU-21 机型档案 — 描述一款机型的相机 / 屏幕 / SoC 等硬件特征。
 *
 * 用途：
 * - 订阅 JSON Schema v3：预设可声明 `deviceProfile` 推荐机型；
 * - HomeScreen 首启时识别当前机型，推荐匹配预设（FD-04）；
 * - FrameRenderer 相框右下角根据 EXIF 匹配到的 DeviceProfile 渲染金标水印（MU-22）；
 * - 悬浮窗读取屏幕尺寸/刷新率，精准贴边（MU-23）。
 */
@Serializable
data class DeviceProfile(
    /** 机型代号（唯一 key），如 "oppo_find_x10"、"oneplus_13"、"realme_gt7" */
    val deviceKey: String,
    /** 品牌 */
    val brand: String,
    /** 官方型号 */
    val model: String,
    /** 品牌短标识，用于预设匹配（"oppo" / "oneplus" / "realme" / "hasselblad" / "all"） */
    val brandTag: String,
    /** 传感器总像素（单位：MP），如 200 表示 2 亿像素；0 表示未知或不重要 */
    val totalPixelsMp: Int = 0,
    /** 传感器尺寸字符串（供相框水印文案使用），如 "1/1.4" */
    val sensorSize: String = "",
    /** 最大光圈（广角主摄），如 "f/1.6" */
    val maxAperture: String = "",
    /** 是否搭载哈苏 / 徕卡 / 蔡司 等专业影像系统 */
    val isProImaging: Boolean = false,
    /** 屏幕宽度（像素），默认 0 由 DeviceDetector 运行时填充 */
    val screenWidthPx: Int = 0,
    /** 屏幕高度（像素），默认 0 由 DeviceDetector 运行时填充 */
    val screenHeightPx: Int = 0,
    /** 最大刷新率（Hz） */
    val maxRefreshRateHz: Int = 60,
    /** ColorOS / OxygenOS / RealmeUI 版本（如 17, 15） */
    val colorOsVersion: Int = 0,
    /** 简短展示名（中文），用于机型匹配徽章 */
    val displayNameZh: String = model,
    /** 简短展示名（英文） */
    val displayNameEn: String = model
) {
    val has200Mp: Boolean get() = totalPixelsMp >= 200

    companion object {
        /** 通配机型：预设声明此值表示"适配所有设备" */
        const val ALL = "all"
        /** 通配品牌标记 */
        const val BRAND_ALL = "all"
    }
}
