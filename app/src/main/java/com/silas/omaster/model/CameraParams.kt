package com.silas.omaster.model

/**
 * 2026年 OPPO Find X8 Ultra 哈苏大师模式影像参数
 * 基于 OPPO HyperTone Camera System
 */

/**
 * 相机模式枚举
 */
enum class CameraMode(val displayName: String) {
    HasselbladMaster("哈苏大师"),
    HasselbladPortrait("哈苏人像"),
    HasselbladLandscape("哈苏风景"),
    HasselbladNight("哈苏夜景"),
    HasselbladStreet("哈苏街拍"),
    HasselbladPro("哈苏专业"),
    AutoMode("智能模式"),
    ManualMode("专业模式")
}

/**
 * 色彩风格枚举
 */
enum class ColorStyle(val displayName: String, val description: String) {
    Natural("自然", "忠实地再现真实色彩"),
    Vivid("鲜明", "增强色彩饱和度和对比度"),
    Cinematic("电影感", "低饱和度高对比度的电影风格"),
    Professional("专业", "哈苏标准色彩科学"),
    Warm("暖调", "温暖柔和的色调"),
    Cool("冷调", "清冷清爽的色调"),
    Classic("经典", "复古胶片风格"),
    BlackWhite("黑白", "纯粹的黑白影像"),
    Portrait("人像", "优化人像肤色"),
    Food("美食", "提升美食色彩饱和度")
}

/**
 * 焦距模式枚举
 */
enum class FocalLengthMode(val displayName: String) {
    UltraWide("超广角"),
    Wide("广角"),
    Standard("标准"),
    Portrait("人像焦"),
    Telephoto("长焦"),
    UltraTelephoto("超长焦"),
    Macro("微距"),
    SuperMacro("超级微距")
}

/**
 * 哈苏大师模式影像参数 - 2026年 OPPO Find X8 Ultra
 */
data class CameraParams(
    // 基础参数
    val mode: String = CameraMode.HasselbladMaster.displayName,
    val filter: String = "",
    
    // 核心影像参数
    val iso: Int = 100,
    val shutter: String = "1/200",
    val ev: String = "+0.0",
    val wb: String = "5500K",
    
    // 焦距与光圈
    val focalLength: String = "24mm",
    val focalLengthMode: String = FocalLengthMode.Wide.displayName,
    val aperture: String = "f/1.8",
    
    // 拍摄模式开关
    val hdr: Boolean = false,
    val nightMode: Boolean = false,
    val portraitMode: Boolean = false,
    val aiOptimization: Boolean = true,
    val autoFocus: Boolean = true,
    val opticalStabilization: Boolean = true,
    val rawCapture: Boolean = false,
    val proMode: Boolean = true,
    
    // 哈苏认证与风格
    val hasselblad_hncs: Boolean = true,
    val hasselbladNaturalColor: Boolean = true,
    val hasselbladMasterStyle: String = "",
    val hasselbladProMode: Boolean = true,
    val hasselbladColorScience: String = "HNCS 3.0",
    
    // 色彩与风格
    val colorProfile: String = ColorStyle.Natural.displayName,
    val colorStyle: String = ColorStyle.Natural.name,
    val colorTemperature: Int = 5500,
    val tint: Int = 0,
    
    // 图像质量调整
    val sharpness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50,
    val highlight: Int = 0,
    val shadow: Int = 0,
    val exposureCompensation: Int = 0,
    
    // 高级参数
    val meteringMode: String = "Evaluative",
    val focusMode: String = "Continuous AF",
    val whiteBalancePreset: String = "Auto",
    val noiseReduction: Int = 50,
    val detailEnhancement: Int = 50,
    
    // 镜头信息
    val lensId: String = "",
    val lensAperture: String = "f/1.8",
    val opticalZoom: Int = 1,
    val digitalZoom: Int = 1,
    val sensorSize: String = "1英寸",
    
    // 元数据
    val version: String = "3.0",
    val lastModified: Long = System.currentTimeMillis()
) {
    /**
     * 格式化参数为标准展示格式
     */
    fun formatParamsForDisplay(): String {
        return buildString {
            append("ISO $iso")
            append(" · $shutter")
            if (ev != "+0.0" && ev != "0") append(" · EV $ev")
            append(" · $wb")
            if (hasselblad_hncs) append(" · HNCS")
        }
    }
    
    /**
     * 格式化完整参数展示
     */
    fun formatFullParams(): Map<String, String> {
        return mapOf(
            "拍摄模式" to mode,
            "ISO 感光度" to iso.toString(),
            "快门速度" to shutter,
            "曝光补偿" to ev,
            "白平衡" to wb,
            "焦距" to focalLength,
            "光圈" to aperture,
            "色彩风格" to colorStyle,
            "HDR" to if (hdr) "开启" else "关闭",
            "夜景模式" to if (nightMode) "开启" else "关闭",
            "人像模式" to if (portraitMode) "开启" else "关闭",
            "AI 影像引擎" to if (aiOptimization) "开启" else "关闭",
            "哈苏 HNCS" to if (hasselblad_hncs) "认证" else "未认证",
            "哈苏大师风格" to hasselbladMasterStyle.ifEmpty { "默认" },
            "色彩科学" to hasselbladColorScience,
            "清晰度" to "$sharpness%",
            "对比度" to "$contrast%",
            "饱和度" to "$saturation%"
        )
    }
    
    /**
     * 转换为 JSON 格式用于数据同步
     */
    fun toJsonMap(): Map<String, Any> {
        return mapOf(
            "mode" to mode,
            "filter" to filter,
            "iso" to iso,
            "shutter" to shutter,
            "ev" to ev,
            "wb" to wb,
            "focalLength" to focalLength,
            "aperture" to aperture,
            "hdr" to hdr,
            "nightMode" to nightMode,
            "portraitMode" to portraitMode,
            "aiOptimization" to aiOptimization,
            "hasselblad_hncs" to hasselblad_hncs,
            "hasselbladNaturalColor" to hasselbladNaturalColor,
            "hasselbladMasterStyle" to hasselbladMasterStyle,
            "hasselbladColorScience" to hasselbladColorScience,
            "colorProfile" to colorProfile,
            "colorStyle" to colorStyle,
            "colorTemperature" to colorTemperature,
            "sharpness" to sharpness,
            "contrast" to contrast,
            "saturation" to saturation,
            "version" to version,
            "lastModified" to lastModified
        )
    }
    
    companion object {
        /**
         * 创建默认的哈苏大师模式参数
         */
        fun defaultHasselbladMaster(): CameraParams {
            return CameraParams(
                mode = CameraMode.HasselbladMaster.displayName,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladProMode = true,
                hasselbladColorScience = "HNCS 3.0",
                colorStyle = ColorStyle.Natural.name,
                colorProfile = ColorStyle.Natural.displayName,
                aiOptimization = true,
                proMode = true
            )
        }
        
        /**
         * 创建哈苏人像预设参数
         */
        fun createPortraitPreset(): CameraParams {
            return CameraParams(
                mode = CameraMode.HasselbladPortrait.displayName,
                iso = 100,
                shutter = "1/200",
                ev = "+0.0",
                wb = "5200K",
                focalLength = "50mm",
                aperture = "f/1.8",
                portraitMode = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladMasterStyle = "Portrait",
                hasselbladColorScience = "HNCS 3.0",
                colorStyle = ColorStyle.Portrait.name,
                colorProfile = ColorStyle.Portrait.displayName,
                colorTemperature = 5200,
                sharpness = 45,
                contrast = 48,
                saturation = 52
            )
        }
        
        /**
         * 创建哈苏风景预设参数
         */
        fun createLandscapePreset(): CameraParams {
            return CameraParams(
                mode = CameraMode.HasselbladLandscape.displayName,
                iso = 100,
                shutter = "1/125",
                ev = "-0.3",
                wb = "5600K",
                focalLength = "24mm",
                aperture = "f/8",
                hdr = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladMasterStyle = "Landscape",
                hasselbladColorScience = "HNCS 3.0",
                colorStyle = ColorStyle.Natural.name,
                colorProfile = ColorStyle.Natural.displayName,
                colorTemperature = 5600,
                sharpness = 55,
                contrast = 52,
                saturation = 50
            )
        }
    }
}