package com.silas.omaster.infrastructure.utils

/**
 * 应用全局 URL 常量集中管理
 *
 * 所有外部 URL（CDN、API端点、预设源、隐私政策等）统一在此维护，
 * 避免硬编码分散导致维护困难和安全风险。
 *
 * 修改规则：
 * 1. 所有生产 URL 必须使用 HTTPS
 * 2. 变更 CDN 域名时同步更新 network_security_config.xml 白名单
 * 3. 新增 API 端点需经过安全评审
 */
object UrlConstants {

    // ===== CDN 基础 URL =====
    /**
     * UC-26: jsDelivr CDN 基础路径——可配置
     *
     * 默认值: https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main
     * 可通过 setCdnBaseUrl() 在运行时覆盖，例如切换到自定义镜像或加速节点。
     * 所有依赖此常量的 URL（预设源、LUT、示例图片等）会自动使用新值。
     */
    const val CDN_JSDELIVR_DEFAULT = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main"

    /** 当前生效的 jsDelivr CDN 基础路径（可运行时修改） */
    var cdnBaseUrl: String = CDN_JSDELIVR_DEFAULT
        private set

    /** UC-26: 运行时配置 CDN 基础路径 */
    fun setCdnBaseUrl(url: String) {
        if (url.isNotBlank() && url.lowercase().startsWith("https://")) {
            cdnBaseUrl = url.trimEnd('/')
        }
    }

    /** UC-26: 重置为默认 CDN 基础路径 */
    fun resetCdnBaseUrl() {
        cdnBaseUrl = CDN_JSDELIVR_DEFAULT
    }

    /** jsDelivr CDN - OMaster 社区资源（兼容旧代码，指向可配置值） */
    val CDN_JSDELIVR: String get() = cdnBaseUrl

    /** 模型下载 CDN */
    const val CDN_MODELS = "https://releases.omaster.app/models"

    // ===== API 端点 =====
    /** AI 推理 API 默认端点 */
    const val API_AI_ENDPOINT = "https://api.omaster.app/ai"

    /** 预设同步 API 默认端点 */
    const val API_PRESET_ENDPOINT = "https://api.omaster.app/presets"

    /** 用户认证 API 默认端点 */
    const val API_AUTH_ENDPOINT = "https://api.omaster.app/auth"

    /** AI 场景分析云端 API */
    const val API_CLOUD_SCENE_ANALYZE = "https://api.omaster.ai/v1/scene/analyze"

    /** 内购验证 API */
    const val API_BILLING_VERIFY = "https://api.omaster.app/billing/verify"

    /** 反馈上传 API（默认端点，可通过 local.properties 覆盖） */
    const val API_FEEDBACK_ENDPOINT = "https://api.omaster.app/feedback"

    // ===== 预设源 URL =====
    /** OPPO/一加 大师模式预设 */
    val PRESET_OPPO: String get() = "$CDN_JSDELIVR/presets/v2/oppo.json"

    /** realme GT 大师模式预设 */
    val PRESET_REALME: String get() = "$CDN_JSDELIVR/presets/v2/realme.json"

    /** vivo 蔡司自然色彩预设 */
    val PRESET_VIVO: String get() = "$CDN_JSDELIVR/presets/v2/vivo.json"

    /** 荣耀 Magic 影像预设 */
    val PRESET_HONOR: String get() = "$CDN_JSDELIVR/presets/v2/honor.json"

    /** 预设源 URL 映射（品牌 -> URL） */
    val PRESET_SOURCE_URLS: Map<String, String> get() = mapOf(
        "oppo" to PRESET_OPPO,
        "realme" to PRESET_REALME,
        "vivo" to PRESET_VIVO,
        "honor" to PRESET_HONOR
    )

    // ===== 更新相关 =====
    /** GitHub API - 最新 Release */
    const val GITHUB_API_RELEASES = "https://api.github.com/repos/fengyec2/OMaster-Android/releases/latest"

    /** Gitee API - 最新 Release */
    const val GITEE_API_RELEASES = "https://gitee.com/api/v5/repos/silas/omaster-android/releases/latest"

    // ===== 隐私与政策 =====
    /** 隐私政策页面 */
    const val PRIVACY_POLICY_URL = "https://omaster.app/privacy-policy"

    /** 友盟隐私政策 */
    const val UMENG_PRIVACY_URL = "https://www.umeng.com/page/policy"

    // ===== 预设源信息（用于 UI 展示） =====
    data class PresetSourceInfo(
        val brand: String,
        val displayName: String,
        val description: String,
        val url: String
    )

    val PRESET_SOURCE_INFO_LIST: List<PresetSourceInfo> get() = listOf(
        PresetSourceInfo("OPPO", "一加/OPPO 大师模式官方预设", "OPPO & OnePlus 大师模式预设合集", PRESET_OPPO),
        PresetSourceInfo("realme", "realme GT 大师模式官方预设", "realme GT 系列大师模式预设", PRESET_REALME),
        PresetSourceInfo("vivo", "vivo 蔡司自然色彩官方预设", "vivo ZEISS 自然色彩预设", PRESET_VIVO),
        PresetSourceInfo("honor", "荣耀 Magic 影像官方预设", "荣耀 Magic 系列影像预设", PRESET_HONOR)
    )

    // ===== LUT 资源 CDN 路径 =====
    val LUT_BASE_PATH: String get() = "$CDN_JSDELIVR/luts"

    // ===== 示例图片 CDN 路径 =====
    val SAMPLES_BASE_PATH: String get() = "$CDN_JSDELIVR/samples"

    /**
     * 获取 LUT 下载 URL
     */
    fun getLUTDownloadUrl(category: String, fileName: String): String {
        return "$LUT_BASE_PATH/$category/$fileName"
    }

    /**
     * 获取示例图片 URL
     */
    fun getSampleImageUrl(fileName: String): String {
        return "$SAMPLES_BASE_PATH/$fileName"
    }
}