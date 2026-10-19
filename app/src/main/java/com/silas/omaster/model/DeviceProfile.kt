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
    /**
     * 【v1.6.14】屏幕尺寸（英寸）—— Find X10 为 6.59"。
     * 0 表示未知（UI 层据此隐藏该行，避免展示占位 0）。
     */
    val screenSizeInch: Float = 0f,
    /** 【v1.6.14】电池容量（mAh）—— Find X10 全系 8000mAh 冰川电池 */
    val batteryCapacityMah: Int = 0,
    /** 【v1.6.14】最高视频规格文案，如 "8K 30fps" */
    val maxVideoSpec: String = "",
    /** 【v1.6.14】光学防抖规格文案，如 "两轴 OIS" */
    val opticalStabilization: String = "",
    /** 【v1.6.14】长焦防抖等级（CIPA）—— 0 表示未采集 */
    val telephotoStabilizationCipa: Float = 0f,
    /** 简短展示名（中文），用于机型匹配徽章 */
    val displayNameZh: String = model,
    /** 简短展示名（英文） */
    val displayNameEn: String = model
) {
    val has200Mp: Boolean get() = totalPixelsMp >= 200

    /** 【v1.6.14】是否采集到可用的硬件规格（用于关于页"硬件档案"展开区的显隐） */
    val hasHardwareSpecs: Boolean
        get() = screenSizeInch > 0f || batteryCapacityMah > 0 ||
                maxVideoSpec.isNotBlank() || opticalStabilization.isNotBlank()

    /**
     * 【v1.6.14】能力雷达四维评分（0 ~ 100）—— 关于页设备卡片雷达图数据源。
     * 影像：像素 + 光圈 + 防抖加权；性能：刷新率；屏幕：尺寸 + 刷新率；续航：电池容量。
     * 仅作相对强弱的可视化，不代表实验室跑分。
     */
    val capabilityScores: List<Pair<String, Float>>
        get() {
            val imaging = (
                (totalPixelsMp.coerceAtMost(200) / 200f) * 55f +
                (if (maxAperture.isNotBlank()) {
                    val f = maxAperture.removePrefix("f/").toFloatOrNull() ?: 2.0f
                    ((2.4f - f).coerceIn(0f, 1.2f) / 1.2f) * 25f
                } else 0f) +
                (if (isProImaging) 20f else 0f)
            ).coerceIn(0f, 100f)
            val performance = ((maxRefreshRateHz.coerceAtMost(144) - 60).coerceAtLeast(0) / 84f) * 100f
            val display = (
                (if (screenSizeInch > 0f) 60f else 30f) +
                ((maxRefreshRateHz.coerceAtMost(144) - 60).coerceAtLeast(0) / 84f) * 40f
            ).coerceIn(0f, 100f)
            val battery = (batteryCapacityMah.coerceAtMost(8000) / 8000f) * 100f
            return listOf("影像" to imaging, "性能" to performance, "屏幕" to display, "续航" to battery)
        }

    companion object {
        /** 通配机型：预设声明此值表示"适配所有设备" */
        const val ALL = "all"
        /** 通配品牌标记 */
        const val BRAND_ALL = "all"
    }
}
