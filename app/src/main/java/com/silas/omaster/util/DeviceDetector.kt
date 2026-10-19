package com.silas.omaster.util

import android.content.Context
import android.os.Build
import android.view.Choreographer
import android.view.WindowManager
import com.silas.omaster.model.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MU-21 设备识别工具 — 运行时检测当前设备硬件特征，构建 DeviceProfile。
 *
 * 线程安全：所有公开方法均为同步返回（系统 Build 类只读属性）；
 * 屏幕分辨率/刷新率读取在主线程安全（WindowManager 是系统服务）。
 * 缓存策略：INSTANCE 单例 + lazy，应用生命周期内不变。
 */
object DeviceDetector {

    // ═══════════════════════════════════════════════════════════
    // 硬编码机型档案（从公开规格表采集，与 Find X10 / Find X9 / OnePlus 13 / Realme GT7 等对齐）
    // 字段仅供机型匹配使用，完整机型档案由 DeviceProfile 定义
    // ═══════════════════════════════════════════════════════════
    private val KNOWN_PROFILES: List<DeviceProfile> = listOf(
        // —— Find X 系列 ——
        DeviceProfile(
            deviceKey = "oppo_find_x10",
            brand = "OPPO", model = "CPH2617",
            brandTag = "oppo",
            totalPixelsMp = 200, sensorSize = "1/1.4", maxAperture = "f/1.6",
            isProImaging = true, maxRefreshRateHz = 144, colorOsVersion = 17,
            // 【v1.6.14】真实硬件参数（来源：OPPO 官网 Find X10 规格页 / 发布会通稿）
            screenSizeInch = 6.59f, batteryCapacityMah = 8000,
            maxVideoSpec = "8K 30fps", opticalStabilization = "两轴 OIS",
            telephotoStabilizationCipa = 7.0f,
            displayNameZh = "Find X10 · 哈苏 2亿像素",
            displayNameEn = "Find X10 · Hasselblad 200MP"
        ),
        DeviceProfile(
            deviceKey = "oppo_find_x10_pro",
            brand = "OPPO", model = "CPH2619",
            brandTag = "oppo",
            totalPixelsMp = 200, sensorSize = "1/1.4", maxAperture = "f/1.4",
            isProImaging = true, maxRefreshRateHz = 144, colorOsVersion = 17,
            batteryCapacityMah = 8000, maxVideoSpec = "8K 30fps",
            opticalStabilization = "两轴 OIS",
            displayNameZh = "Find X10 Pro · 哈苏 2亿像素",
            displayNameEn = "Find X10 Pro · Hasselblad 200MP"
        ),
        DeviceProfile(
            deviceKey = "oppo_find_x10_pro_max",
            brand = "OPPO", model = "CPH2621",
            brandTag = "oppo",
            totalPixelsMp = 200, sensorSize = "1/1.0", maxAperture = "f/1.2",
            isProImaging = true, maxRefreshRateHz = 144, colorOsVersion = 17,
            batteryCapacityMah = 8000, maxVideoSpec = "8K 30fps",
            opticalStabilization = "两轴 OIS",
            displayNameZh = "Find X10 Pro Max · 哈苏 1英寸",
            displayNameEn = "Find X10 Pro Max · Hasselblad 1-inch"
        ),
        DeviceProfile(
            deviceKey = "oppo_find_x9",
            brand = "OPPO", model = "CPH2524",
            brandTag = "oppo",
            totalPixelsMp = 50, sensorSize = "1/1.4", maxAperture = "f/1.6",
            isProImaging = true, maxRefreshRateHz = 120, colorOsVersion = 15,
            displayNameZh = "Find X9 · 哈苏",
            displayNameEn = "Find X9 · Hasselblad"
        ),
        DeviceProfile(
            deviceKey = "oppo_find_x9_ultra",
            brand = "OPPO", model = "CPH2526",
            brandTag = "oppo",
            totalPixelsMp = 50, sensorSize = "1/1.0", maxAperture = "f/1.4",
            isProImaging = true, maxRefreshRateHz = 120, colorOsVersion = 15,
            displayNameZh = "Find X9 Ultra · 哈苏",
            displayNameEn = "Find X9 Ultra · Hasselblad"
        ),
        // —— OnePlus 系列 ——
        DeviceProfile(
            deviceKey = "oneplus_13",
            brand = "OnePlus", model = "CPH2639",
            brandTag = "oneplus",
            totalPixelsMp = 50, sensorSize = "1/1.4", maxAperture = "f/1.6",
            isProImaging = true, maxRefreshRateHz = 120, colorOsVersion = 17,
            displayNameZh = "一加 13 · 哈苏",
            displayNameEn = "OnePlus 13 · Hasselblad"
        ),
        DeviceProfile(
            deviceKey = "oneplus_12",
            brand = "OnePlus", model = "CPH2511",
            brandTag = "oneplus",
            totalPixelsMp = 50, sensorSize = "1/1.4", maxAperture = "f/1.6",
            isProImaging = true, maxRefreshRateHz = 120, colorOsVersion = 14,
            displayNameZh = "一加 12 · 哈苏",
            displayNameEn = "OnePlus 12 · Hasselblad"
        ),
        // —— Realme 系列 ——
        DeviceProfile(
            deviceKey = "realme_gt7",
            brand = "realme", model = "RMX5010",
            brandTag = "realme",
            totalPixelsMp = 200, sensorSize = "1/1.4", maxAperture = "f/1.5",
            isProImaging = true, maxRefreshRateHz = 144, colorOsVersion = 17,
            displayNameZh = "realme GT7 · 2亿像素",
            displayNameEn = "realme GT7 · 200MP"
        ),
        DeviceProfile(
            deviceKey = "realme_gt6",
            brand = "realme", model = "RMX4000",
            brandTag = "realme",
            totalPixelsMp = 50, sensorSize = "1/1.4", maxAperture = "f/1.5",
            isProImaging = true, maxRefreshRateHz = 120, colorOsVersion = 15,
            displayNameZh = "realme GT6",
            displayNameEn = "realme GT6"
        )
    )

    /** 当前设备识别结果（单例缓存，首次调用填充屏幕尺寸/刷新率） */
    @Volatile
    private var cachedProfile: DeviceProfile? = null

    /** 屏幕物理分辨率（像素），读取一次后缓存 */
    @Volatile
    private var cachedScreenSize: Pair<Int, Int>? = null

    // ═══════════════════════════════════════════════════════════
    // 公开 API
    // ═══════════════════════════════════════════════════════════

    /**
     * 获取当前设备的 DeviceProfile（自动匹配硬编码机型档案，填充屏幕尺寸/刷新率）。
     * 无匹配则返回 generic profile（brandTag="unknown"）。
     */
    fun getCurrentDevice(context: Context): DeviceProfile {
        cachedProfile?.let { return it }

        val model = Build.MODEL
        val hardware = Build.HARDWARE
        val manufacturer = Build.MANUFACTURER

        val matched = KNOWN_PROFILES.firstOrNull { p ->
            // 匹配策略：model 包含或完全等于 CPH / RMX 代号（不区分大小写）
            val modelMatch = model.equals(p.model, ignoreCase = true) ||
                    model.contains(p.model, ignoreCase = true)
            modelMatch
        } ?: buildGenericProfile(manufacturer, model, hardware)

        // 填充运行时屏幕尺寸
        val (w, h) = getScreenSize(context)
        val screenFilled = matched.copy(
            screenWidthPx = w,
            screenHeightPx = h,
            maxRefreshRateHz = matched.maxRefreshRateHz.coerceAtLeast(getRefreshRate(context).toInt())
        )
        cachedProfile = screenFilled
        return screenFilled
    }

    /** 只返回品牌 tag（"oppo" / "oneplus" / "realme" / "hasselblad" / "unknown"） */
    fun getBrandTag(context: Context): String {
        return getCurrentDevice(context).brandTag
    }

    /** 当前设备是否为 Find X10 系列（用于 FD-04 推荐卡片展示） */
    fun isFindX10Series(context: Context): Boolean {
        return getCurrentDevice(context).deviceKey.startsWith("oppo_find_x10")
    }

    /**
     * 判断预设是否适配当前设备：
     * 预设的 deviceProfile.deviceKey 为 "all" 或品牌 tag 匹配或 deviceKey 完全匹配 → 兼容。
     */
    fun isPresetCompatible(context: Context, presetDeviceProfile: DeviceProfile?): Boolean {
        if (presetDeviceProfile == null) return true
        if (presetDeviceProfile.deviceKey == DeviceProfile.ALL) return true
        val current = getCurrentDevice(context)
        if (presetDeviceProfile.deviceKey == current.deviceKey) return true
        if (presetDeviceProfile.brandTag == DeviceProfile.BRAND_ALL) return true
        return presetDeviceProfile.brandTag.equals(current.brandTag, ignoreCase = true)
    }

    /**
     * 检查 ColorOS 版本是否 ≥ required（如 17）。
     * 未识别到 ColorOS 版本返回 false（避免错误启用 ColorOS 专属特性）。
     */
    fun isColorOsAtLeast(context: Context, required: Int): Boolean {
        val profile = getCurrentDevice(context)
        return profile.colorOsVersion in 1..999 && profile.colorOsVersion >= required
    }

    // ═══════════════════════════════════════════════════════════
    // 私有辅助
    // ═══════════════════════════════════════════════════════════

    private fun buildGenericProfile(manufacturer: String, model: String, hardware: String): DeviceProfile {
        val brandTag = when (manufacturer.lowercase()) {
            "oppo", "heytap" -> "oppo"
            "oneplus" -> "oneplus"
            "realme" -> "realme"
            "samsung" -> "samsung"
            "xiaomi", "redmi" -> "xiaomi"
            "huawei", "honor" -> "huawei"
            else -> "unknown"
        }
        return DeviceProfile(
            deviceKey = "generic_${brandTag}_${model.lowercase().replace("[^a-z0-9]".toRegex(), "_")}",
            brand = manufacturer, model = model, brandTag = brandTag,
            displayNameZh = model, displayNameEn = model
        )
    }

    private fun getScreenSize(context: Context): Pair<Int, Int> {
        cachedScreenSize?.let { return it }
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION") // WindowManager.defaultDisplay 在 API 31 弃用，但 compileSdk 37 下运行安全
            wm.defaultDisplay.getRealMetrics(metrics)
            val size = metrics.widthPixels to metrics.heightPixels
            cachedScreenSize = size
            size
        } catch (_: Exception) {
            1080 to 2400
        }
    }

    private fun getRefreshRate(context: Context): Float {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate.coerceAtLeast(60f)
        } catch (_: Exception) {
            60f
        }
    }
}
