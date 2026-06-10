package com.silas.omaster.data.model

/**
 * 统一水印模板预设
 * 20个专业水印模板体系
 */

// ========== 统一模板列表 ==========
val MASTER_WATERMARK_TEMPLATES = listOf(
    // === 哈苏大师系列 (3个) ===
    MasterWatermarkTemplate(
        id = "hasselblad-master",
        name = "哈苏大师",
        nameEn = "Hasselblad Master",
        category = WatermarkCategory.HASSELBLAD,
        description = "哈苏大师赛官方水印风格",
        isHasselbladSeries = true,
        isPopular = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "HASSELBLAD",
                defaultPosition = WatermarkPosition.BOTTOM_CENTER,
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 14f,
                    fontWeight = 700,
                    letterSpacing = 2f,
                    opacity = 0.9f
                ),
                isRequired = true,
                contentSource = ContentSource.MANUAL,
                sortOrder = 0
            ),
            WatermarkLayerDef(
                id = "subtitle",
                type = WatermarkLayerType.TEXT,
                defaultContent = "Natural Color Solution",
                defaultPosition = WatermarkPosition.BOTTOM_CENTER,
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 10f,
                    opacity = 0.6f
                ),
                sortOrder = 1
            )
        ),
        presetStyle = WatermarkStylePreset(
            primaryColor = "#FFFFFF",
            fontSize = 14f,
            letterSpacing = 2f
        )
    ),
    MasterWatermarkTemplate(
        id = "hasselblad-hncs",
        name = "HNCS认证",
        nameEn = "HNCS Certified",
        category = WatermarkCategory.HASSELBLAD,
        description = "哈苏自然色彩解决方案认证",
        isHasselbladSeries = true,
        isNew = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "HNCS",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 16f,
                    fontWeight = 700,
                    letterSpacing = 4f
                ),
                isRequired = true
            ),
            WatermarkLayerDef(
                id = "cert",
                type = WatermarkLayerType.TEXT,
                defaultContent = "CERTIFIED",
                defaultStyle = WatermarkLayerStyle(fontSize = 8f, opacity = 0.5f),
                sortOrder = 1
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "hasselblad-xpan",
        name = "XPAN宽幅",
        nameEn = "XPAN Format",
        category = WatermarkCategory.HASSELBLAD,
        description = "哈苏XPAN宽幅相机风格",
        isHasselbladSeries = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "XPAN",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 18f,
                    fontWeight = 700,
                    letterSpacing = 6f
                ),
                isRequired = true
            )
        )
    ),
    
    // === 品牌认证系列 (5个) ===
    MasterWatermarkTemplate(
        id = "classic-camera",
        name = "经典相机",
        nameEn = "Classic Camera",
        category = WatermarkCategory.BRAND,
        description = "经典相机水印风格",
        isPopular = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "shot-on",
                type = WatermarkLayerType.TEXT,
                defaultContent = "Shot on",
                defaultStyle = WatermarkLayerStyle(fontSize = 12f, opacity = 0.6f),
                sortOrder = 0
            ),
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "OMaster",
                defaultStyle = WatermarkLayerStyle(fontSize = 16f, fontWeight = 700),
                isRequired = true,
                sortOrder = 1
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "leica-style",
        name = "徕卡风格",
        nameEn = "Leica Style",
        category = WatermarkCategory.BRAND,
        description = "徕卡相机经典风格",
        isPopular = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "Leica",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 18f,
                    fontWeight = 700,
                    letterSpacing = 3f
                ),
                isRequired = true
            ),
            WatermarkLayerDef(
                id = "subtitle",
                type = WatermarkLayerType.TEXT,
                defaultContent = "Camera AG",
                defaultStyle = WatermarkLayerStyle(fontSize = 10f, opacity = 0.5f),
                sortOrder = 1
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "oppo-find",
        name = "Find系列",
        nameEn = "OPPO Find",
        category = WatermarkCategory.BRAND,
        description = "OPPO Find系列手机风格",
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "OPPO Find",
                defaultStyle = WatermarkLayerStyle(fontSize = 14f, fontWeight = 600),
                isRequired = true
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "oneplus-hasselblad",
        name = "一加哈苏",
        nameEn = "OnePlus Hasselblad",
        category = WatermarkCategory.BRAND,
        description = "一加哈苏联合水印",
        isHasselbladSeries = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "OnePlus | HASSELBLAD",
                defaultStyle = WatermarkLayerStyle(fontSize = 12f, fontWeight = 600),
                isRequired = true
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "film-strip",
        name = "胶片条",
        nameEn = "Film Strip",
        category = WatermarkCategory.BRAND,
        description = "胶片边框风格",
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "FILM",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 10f,
                    letterSpacing = 8f,
                    opacity = 0.7f
                ),
                isRequired = true
            )
        )
    ),
    
    // === 极简印记系列 (2个) ===
    MasterWatermarkTemplate(
        id = "minimal-mark",
        name = "极简印记",
        nameEn = "Minimal Mark",
        category = WatermarkCategory.MINIMAL,
        description = "极简风格水印",
        isPopular = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "brand",
                type = WatermarkLayerType.BRAND,
                defaultContent = "OM",
                defaultStyle = WatermarkLayerStyle(fontSize = 20f, fontWeight = 700),
                isRequired = true
            )
        ),
        defaultPosition = WatermarkPosition.BOTTOM_RIGHT
    ),
    MasterWatermarkTemplate(
        id = "brand-logo",
        name = "品牌Logo",
        nameEn = "Brand Logo",
        category = WatermarkCategory.MINIMAL,
        description = "仅显示品牌Logo",
        layers = listOf(
            WatermarkLayerDef(
                id = "logo",
                type = WatermarkLayerType.LOGO,
                defaultPosition = WatermarkPosition.BOTTOM_RIGHT
            )
        )
    ),
    
    // === 技术参数系列 (2个) ===
    MasterWatermarkTemplate(
        id = "detailed-params",
        name = "详细参数",
        nameEn = "Detailed Parameters",
        category = WatermarkCategory.TECH,
        description = "显示完整拍摄参数",
        layers = listOf(
            WatermarkLayerDef(
                id = "device",
                type = WatermarkLayerType.DEVICE,
                contentSource = ContentSource.DEVICE_INFO,
                defaultStyle = WatermarkLayerStyle(fontSize = 12f),
                isEnabled = true
            ),
            WatermarkLayerDef(
                id = "params",
                type = WatermarkLayerType.PARAMS,
                contentSource = ContentSource.EXIF,
                defaultStyle = WatermarkLayerStyle(fontSize = 10f, opacity = 0.7f),
                sortOrder = 1
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "exif-info",
        name = "EXIF信息",
        nameEn = "EXIF Info",
        category = WatermarkCategory.TECH,
        description = "从照片读取EXIF信息",
        layers = listOf(
            WatermarkLayerDef(
                id = "params",
                type = WatermarkLayerType.PARAMS,
                contentSource = ContentSource.EXIF,
                defaultStyle = WatermarkLayerStyle(fontSize = 11f)
            )
        )
    ),
    
    // === 信息记录系列 (2个) ===
    MasterWatermarkTemplate(
        id = "location-tag",
        name = "地理位置",
        nameEn = "Location Tag",
        category = WatermarkCategory.INFO,
        description = "显示拍摄地点",
        layers = listOf(
            WatermarkLayerDef(
                id = "location",
                type = WatermarkLayerType.LOCATION,
                contentSource = ContentSource.GPS,
                defaultStyle = WatermarkLayerStyle(fontSize = 12f)
            ),
            WatermarkLayerDef(
                id = "date",
                type = WatermarkLayerType.TIMESTAMP,
                contentSource = ContentSource.EXIF,
                defaultStyle = WatermarkLayerStyle(fontSize = 10f, opacity = 0.6f),
                sortOrder = 1
            )
        )
    ),
    MasterWatermarkTemplate(
        id = "timestamp",
        name = "时间戳",
        nameEn = "Timestamp",
        category = WatermarkCategory.INFO,
        description = "显示拍摄时间",
        layers = listOf(
            WatermarkLayerDef(
                id = "date",
                type = WatermarkLayerType.TIMESTAMP,
                contentSource = ContentSource.SYSTEM,
                defaultStyle = WatermarkLayerStyle(fontSize = 12f)
            )
        )
    ),
    
    // === 个人签名系列 (2个) ===
    MasterWatermarkTemplate(
        id = "photographer-sign",
        name = "摄影师签名",
        nameEn = "Photographer Signature",
        category = WatermarkCategory.PERSONAL,
        description = "摄影师个人署名",
        isPopular = true,
        layers = listOf(
            WatermarkLayerDef(
                id = "signature",
                type = WatermarkLayerType.TEXT,
                defaultContent = "Photographer",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 14f,
                    fontFamily = "italic"
                )
            )
        ),
        defaultPosition = WatermarkPosition.BOTTOM_RIGHT
    ),
    MasterWatermarkTemplate(
        id = "art-signature",
        name = "艺术签名",
        nameEn = "Art Signature",
        category = WatermarkCategory.PERSONAL,
        description = "艺术风格签名",
        layers = listOf(
            WatermarkLayerDef(
                id = "signature",
                type = WatermarkLayerType.TEXT,
                defaultContent = "Artist",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 16f,
                    fontWeight = 300,
                    letterSpacing = 4f
                )
            )
        )
    ),
    
    // === 社交分享系列 (1个) ===
    MasterWatermarkTemplate(
        id = "social-share",
        name = "社交分享",
        nameEn = "Social Share",
        category = WatermarkCategory.SOCIAL,
        description = "社交媒体账号",
        layers = listOf(
            WatermarkLayerDef(
                id = "social",
                type = WatermarkLayerType.TEXT,
                defaultContent = "@omaster",
                defaultStyle = WatermarkLayerStyle(fontSize = 14f)
            )
        ),
        defaultPosition = WatermarkPosition.BOTTOM_CENTER
    ),
    
    // === 版权保护系列 (1个) ===
    MasterWatermarkTemplate(
        id = "copyright",
        name = "版权声明",
        nameEn = "Copyright",
        category = WatermarkCategory.LEGAL,
        description = "版权保护声明",
        layers = listOf(
            WatermarkLayerDef(
                id = "copyright",
                type = WatermarkLayerType.TEXT,
                defaultContent = "© 2026 OMaster. All rights reserved.",
                defaultStyle = WatermarkLayerStyle(fontSize = 10f, opacity = 0.6f)
            )
        )
    ),
    
    // === 荣誉徽章系列 (1个) ===
    MasterWatermarkTemplate(
        id = "award-badge",
        name = "获奖作品",
        nameEn = "Award Badge",
        category = WatermarkCategory.BADGE,
        description = "获奖作品标识",
        layers = listOf(
            WatermarkLayerDef(
                id = "award",
                type = WatermarkLayerType.TEXT,
                defaultContent = "🏆 Award Winning",
                defaultStyle = WatermarkLayerStyle(fontSize = 12f)
            )
        )
    ),
    
    // === 专业防伪系列 (1个) ===
    MasterWatermarkTemplate(
        id = "pro-protect",
        name = "专业防伪",
        nameEn = "Pro Protection",
        category = WatermarkCategory.PRO,
        description = "满屏半透明防伪水印",
        layers = listOf(
            WatermarkLayerDef(
                id = "protect",
                type = WatermarkLayerType.TEXT,
                defaultContent = "OMASTER",
                defaultStyle = WatermarkLayerStyle(
                    fontSize = 48f,
                    opacity = 0.1f,
                    rotation = -30f
                )
            )
        ),
        defaultPosition = WatermarkPosition.CENTER
    )
)

// ========== 获取模板 ==========
fun getWatermarkTemplateById(id: String): MasterWatermarkTemplate? =
    MASTER_WATERMARK_TEMPLATES.find { it.id == id }

fun getWatermarkTemplatesByCategory(category: WatermarkCategory): List<MasterWatermarkTemplate> =
    if (category == WatermarkCategory.ALL) {
        MASTER_WATERMARK_TEMPLATES
    } else {
        MASTER_WATERMARK_TEMPLATES.filter { it.category == category }
    }

fun getPopularWatermarkTemplates(): List<MasterWatermarkTemplate> =
    MASTER_WATERMARK_TEMPLATES.filter { it.isPopular }

fun getNewWatermarkTemplates(): List<MasterWatermarkTemplate> =
    MASTER_WATERMARK_TEMPLATES.filter { it.isNew }

fun getHasselbladWatermarkTemplates(): List<MasterWatermarkTemplate> =
    MASTER_WATERMARK_TEMPLATES.filter { it.isHasselbladSeries }
