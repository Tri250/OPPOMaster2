package com.silas.omaster.util

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
    /** jsDelivr CDN - OMaster 社区资源 */
    const val CDN_JSDELIVR = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main"

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

    // ===== 预设源 URL =====
    /** OPPO/一加 大师模式预设 */
    const val PRESET_OPPO = "$CDN_JSDELIVR/presets/v2/oppo.json"

    /** realme GT 大师模式预设 */
    const val PRESET_REALME = "$CDN_JSDELIVR/presets/v2/realme.json"

    /** vivo 蔡司自然色彩预设 */
    const val PRESET_VIVO = "$CDN_JSDELIVR/presets/v2/vivo.json"

    /** 荣耀 Magic 影像预设 */
    const val PRESET_HONOR = "$CDN_JSDELIVR/presets/v2/honor.json"

    /** 预设源 URL 映射（品牌 -> URL） */
    val PRESET_SOURCE_URLS: Map<String, String> = mapOf(
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

    val PRESET_SOURCE_INFO_LIST: List<PresetSourceInfo> = listOf(
        PresetSourceInfo("OPPO", "一加/OPPO 大师模式官方预设", "OPPO & OnePlus 大师模式预设合集", PRESET_OPPO),
        PresetSourceInfo("realme", "realme GT 大师模式官方预设", "realme GT 系列大师模式预设", PRESET_REALME),
        PresetSourceInfo("vivo", "vivo 蔡司自然色彩官方预设", "vivo ZEISS 自然色彩预设", PRESET_VIVO),
        PresetSourceInfo("honor", "荣耀 Magic 影像官方预设", "荣耀 Magic 系列影像预设", PRESET_HONOR)
    )

    // ===== LUT 资源 CDN 路径 =====
    const val LUT_BASE_PATH = "$CDN_JSDELIVR/luts"

    // ===== 示例图片 CDN 路径 =====
    const val SAMPLES_BASE_PATH = "$CDN_JSDELIVR/samples"

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