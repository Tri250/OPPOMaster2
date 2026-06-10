package com.silas.omaster.data.model

import kotlinx.serialization.Serializable

/**
 * 哈苏大师色彩配方数据模型
 * 双端统一：Android (Kotlin Serialization) ↔ Web (TypeScript interface)
 */
@Serializable
data class MasterLUT(
    // ===== 基础信息 =====
    val id: String,                    // 唯一标识 (如 "kodak-portra-400")
    val name: String,                  // 中文名称
    val nameEn: String,                // 英文名称
    val description: String,           // 描述文案
    val longDescription: String = "",  // 详细描述（含拍摄建议）

    // ===== 分类与标签 =====
    val category: LUTCategory,         // 主分类
    val subCategory: String = "",      // 子分类（如 "人像/户外"）
    val tags: List<String>,            // 标签列表
    val suitableFor: List<String>,     // 适用场景

    // ===== 技术规格 =====
    val format: LUTFormat,             // 文件格式
    val size: LUTSize,                 // 色彩精度
    val fileSize: Long,                // 文件大小 (bytes)

    // ===== 视觉资源 =====
    val coverImage: String,            // 封面预览图
    val sampleImages: List<String> = emptyList(), // 样片列表（Before/After 对比用）
    val sampleVideo: String = "",      // 视频样片（可选）

    // ===== 下载信息 =====
    val downloadUrl: String,           // 下载直链
    val mirrorUrls: List<String> = emptyList(), // 备用下载链接

    // ===== 作者与来源 =====
    val author: String,                // 作者名
    val authorAvatar: String = "",     // 作者头像
    val authorUrl: String = "",        // 作者主页
    val source: LUTSource = LUTSource.OMASTER, // 来源

    // ===== 哈苏品牌属性 =====
    val isHncsCertified: Boolean = false,  // 是否 HNCS 认证
    val filmPresetMapping: String = "",     // 关联的胶片风格 (如 "CC"/"NC"/"NH")
    val hasselbladCollection: String = "",  // 所属哈苏系列 (如 "大师赛2024"/"胶片经典")

    // ===== 运营属性 =====
    val isFree: Boolean = true,        // 是否免费
    val isHot: Boolean = false,        // 是否热门
    val isNew: Boolean = false,        // 是否新品
    val isFeatured: Boolean = false,   // 是否精选推荐
    val featuredReason: String = "",   // 精选理由

    // ===== 统计 =====
    val downloads: Long = 0,           // 下载次数
    val likes: Long = 0,               // 喜欢次数
    val rating: Float = 0f,            // 评分 (0-5)
    val ratingCount: Long = 0,         // 评分人数

    // ===== 预设关联 =====
    val relatedPresetIds: List<String> = emptyList(), // 关联的预设ID
    val generatedParams: LUTParams? = null, // 从 LUT 反推的参数近似值

    // ===== 元数据 =====
    val version: Int = 1,              // 版本号
    val createdAt: String,             // 创建时间 (ISO 8601)
    val updatedAt: String = "",        // 更新时间
    val minAppVersion: String = "1.0", // 最低支持版本

    // ===== 使用指引 =====
    val usageGuide: String = "",       // 使用说明 (Markdown)
    val compatibleSoftware: List<String> = emptyList() // 兼容软件 (如 ["DaVinci Resolve", "Premiere Pro", "Final Cut Pro"])
)

enum class LUTCategory(val key: String, val displayName: String, val icon: String) {
    ALL("all", "全部", "🎬"),
    FILM("film", "胶片经典", "🎥"),
    CINEMATIC("cinematic", "电影感", "🎞️"),
    VLOG("vlog", "Vlog风格", "📹"),
    COLOR("color", "色彩风格", "🎨"),
    PORTRAIT("portrait", "人像优化", "👤"),
    NIGHT("night", "夜景", "🌃"),
    VINTAGE("vintage", "复古怀旧", "📻"),
    HASSELBLAD("hasselblad", "哈苏大师", "👑");  // 哈苏专属分类

    companion object {
        fun fromKey(key: String): LUTCategory =
            entries.find { it.key == key } ?: ALL
    }
}

enum class LUTFormat(val extension: String, val displayName: String) {
    CUBE("cube", "Cube LUT"),
    L3D("3dl", "3D LUT"),
    MGA("mga", "MGA LUT");
}

enum class LUTSize(val value: Int, val displayName: String) {
    SIZE_33(33, "33×33×33 (标准)"),
    SIZE_64(64, "64×64×64 (高精度)");
}

enum class LUTSource(val displayName: String) {
    OMASTER("OMaster 官方"),
    COMMUNITY("社区上传"),
    HASSELBLAD("哈苏官方"),
    PARTNER("合作摄影师");
}

@Serializable
data class LUTParams(
    val saturation: Float = 0f,      // 饱和度偏移 (-1~1)
    val contrast: Float = 0f,        // 对比度偏移
    val brightness: Float = 0f,      // 亮度偏移
    val colorTemperature: Float = 0f, // 色温偏移
    val tint: Float = 0f,            // 色调偏移
    val highlightRolloff: Float = 0f, // 高光衰减
    val shadowLift: Float = 0f,      // 阴影提升
    val skinProtection: Boolean = true // 肤色保护
)
