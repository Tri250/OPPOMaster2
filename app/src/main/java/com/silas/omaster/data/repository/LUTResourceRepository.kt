package com.silas.omaster.data.repository

import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.util.UrlConstants

/**
 * LUT资源仓库
 * 对齐 Web 端 lutResourceService.ts
 * 提供真实的LUT资源数据，符合2026年视频创作者需求
 */
object LUTResourceRepository {

    // LUT分类 - 对齐 Web 端 LUT_CATEGORIES
    val CATEGORIES = listOf(
        LUTCategory("all", "全部", "🎬"),
        LUTCategory("film", "胶片电影", "🎥"),
        LUTCategory("cinematic", "电影感", "🎞️"),
        LUTCategory("vlog", "Vlog风格", "📹"),
        LUTCategory("color", "色彩风格", "🎨"),
        LUTCategory("portrait", "人像美颜", "👤"),
        LUTCategory("night", "夜景", "🌃"),
        LUTCategory("vintage", "复古怀旧", "📻"),
    )

    // 2026年流行LUT资源库 - 对齐 Web 端 LUT_RESOURCES
    val RESOURCES: List<LUTResource> = listOf(
        // === 胶片电影类 ===
        LUTResource(
            id = "kodak-portra-400",
            name = "柯达Portra 400",
            nameEn = "Kodak Portra 400",
            description = "经典人像胶片色彩，温暖肤色还原，适合户外人像和婚礼拍摄",
            category = "film",
            tags = listOf("人像", "温暖", "胶片", "婚礼"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/kodak_portra_400_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("film_kodak_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 12,
            author = "OMaster Team",
            authorUrl = "https://github.com/fengyec2",
            downloads = 125600,
            likes = 8920,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("人像", "婚礼", "户外", "街拍"),
            createdAt = "2024-06-15"
        ),
        LUTResource(
            id = "fuji-400h",
            name = "富士400H",
            nameEn = "Fuji 400H",
            description = "日系清新胶片风格，柔和的高光过渡，适合小清新风格视频",
            category = "film",
            tags = listOf("日系", "清新", "柔和", "小清新"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/fuji_400h_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("film_fuji_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 11,
            author = "OMaster Team",
            downloads = 98500,
            likes = 7650,
            rating = 4.8f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("Vlog", "人像", "旅行", "日常"),
            createdAt = "2024-07-20"
        ),
        LUTResource(
            id = "cinestill-800t",
            name = "CineStill 800T",
            nameEn = "CineStill 800T",
            description = "电影灯光片风格，钨丝灯平衡，适合夜景和室内低光拍摄",
            category = "film",
            tags = listOf("夜景", "电影", "室内", "灯光"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/cinestill_800t_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("film_cinestill_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 13,
            author = "OMaster Team",
            downloads = 76200,
            likes = 5430,
            rating = 4.7f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("夜景", "室内", "城市", "电影"),
            createdAt = "2024-08-10"
        ),

        // === 电影感类 ===
        LUTResource(
            id = "arri-alexa",
            name = "ARRI Alexa风格",
            nameEn = "ARRI Alexa Look",
            description = "好莱坞电影机色彩科学，自然肤色，宽广动态范围",
            category = "cinematic",
            tags = listOf("好莱坞", "电影机", "专业", "肤色"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/arri_alexa_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("cinematic_arri_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 15,
            author = "OMaster Team",
            downloads = 156800,
            likes = 12300,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("电影", "短片", "广告", "纪录片"),
            createdAt = "2024-05-01"
        ),
        LUTResource(
            id = "red-dragon",
            name = "RED Dragon色彩",
            nameEn = "RED Dragon Color",
            description = "RED电影机色彩风格，高对比度，鲜艳色彩，适合商业视频",
            category = "cinematic",
            tags = listOf("商业", "鲜艳", "专业", "高对比"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/red_dragon_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("cinematic_red_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 14,
            author = "OMaster Team",
            downloads = 89500,
            likes = 6780,
            rating = 4.8f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("商业", "广告", "MV", "宣传片"),
            createdAt = "2024-06-20"
        ),
        LUTResource(
            id = "sony-slog3",
            name = "Sony S-Log3转Rec709",
            nameEn = "Sony S-Log3 to Rec709",
            description = "索尼Log转标准色彩空间，还原自然色彩，适合索尼相机用户",
            category = "cinematic",
            tags = listOf("索尼", "Log", "还原", "专业"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/sony_slog3_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("cinematic_sony_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 10,
            author = "OMaster Team",
            downloads = 112300,
            likes = 8900,
            rating = 4.7f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("索尼相机", "Log素材", "后期调色"),
            createdAt = "2024-07-15"
        ),

        // === Vlog风格类 ===
        LUTResource(
            id = "vlog-warm",
            name = "Vlog暖调日常",
            nameEn = "Vlog Warm Daily",
            description = "温暖舒适的日常Vlog风格，适合生活记录和美食视频",
            category = "vlog",
            tags = listOf("日常", "温暖", "美食", "生活"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_warm_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("vlog_warm_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 8,
            author = "OMaster Team",
            downloads = 198500,
            likes = 15600,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("Vlog", "美食", "日常", "旅行"),
            createdAt = "2024-08-01"
        ),
        LUTResource(
            id = "vlog-cool",
            name = "Vlog清冷风格",
            nameEn = "Vlog Cool Tone",
            description = "清冷高级感Vlog风格，适合城市探索和科技内容",
            category = "vlog",
            tags = listOf("城市", "清冷", "高级", "科技"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_cool_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("vlog_cool_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 8,
            author = "OMaster Team",
            downloads = 145200,
            likes = 11200,
            rating = 4.8f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("Vlog", "城市", "科技", "开箱"),
            createdAt = "2024-08-15"
        ),
        LUTResource(
            id = "vlog-bright",
            name = "Vlog明亮通透",
            nameEn = "Vlog Bright Clear",
            description = "明亮通透的Vlog风格，提升画面通透感，适合室内和阴天",
            category = "vlog",
            tags = listOf("明亮", "通透", "室内", "阴天"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_bright_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("vlog_bright_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 9,
            author = "OMaster Team",
            downloads = 167800,
            likes = 13400,
            rating = 4.8f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("Vlog", "室内", "教程", "开箱"),
            createdAt = "2024-09-01"
        ),

        // === 色彩风格类 ===
        LUTResource(
            id = "teal-orange",
            name = "青橙电影色调",
            nameEn = "Teal & Orange",
            description = "经典好莱坞青橙配色，强烈的视觉冲击力，适合动作片风格",
            category = "color",
            tags = listOf("青橙", "好莱坞", "动作", "强烈"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/teal_orange_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("color_teal_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 11,
            author = "OMaster Team",
            downloads = 234500,
            likes = 18900,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("电影", "动作", "MV", "短片"),
            createdAt = "2024-04-15"
        ),
        LUTResource(
            id = "pastel-soft",
            name = "柔和马卡龙",
            nameEn = "Pastel Macaron",
            description = "柔和的马卡龙色系，适合美妆和时尚内容",
            category = "color",
            tags = listOf("马卡龙", "柔和", "美妆", "时尚"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/pastel_soft_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("color_pastel_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 7,
            author = "OMaster Team",
            downloads = 87600,
            likes = 7200,
            rating = 4.7f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("美妆", "时尚", "人像", "产品"),
            createdAt = "2024-09-10"
        ),
        LUTResource(
            id = "cyberpunk-neon",
            name = "赛博朋克霓虹",
            nameEn = "Cyberpunk Neon",
            description = "赛博朋克霓虹风格，强烈的蓝紫色调，适合科技和游戏内容",
            category = "color",
            tags = listOf("赛博朋克", "霓虹", "科技", "游戏"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/cyberpunk_neon_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("color_cyberpunk_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 12,
            author = "OMaster Team",
            downloads = 156200,
            likes = 12800,
            rating = 4.8f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("游戏", "科技", "夜景", "MV"),
            createdAt = "2024-07-25"
        ),

        // === 人像美颜类 ===
        LUTResource(
            id = "skin-natural",
            name = "自然肤色",
            nameEn = "Natural Skin",
            description = "自然肤色优化，保持真实感的同时美化肌肤",
            category = "portrait",
            tags = listOf("肤色", "自然", "美颜", "人像"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/portrait/skin_natural_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("portrait_natural_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 8,
            author = "OMaster Team",
            downloads = 178900,
            likes = 14500,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("人像", "婚礼", "写真", "Vlog"),
            createdAt = "2024-06-01"
        ),
        LUTResource(
            id = "skin-pale",
            name = "白皙肤色",
            nameEn = "Pale Skin",
            description = "日系白皙肤色风格，适合日系和韩系人像",
            category = "portrait",
            tags = listOf("白皙", "日系", "韩系", "人像"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/portrait/skin_pale_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("portrait_pale_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 8,
            author = "OMaster Team",
            downloads = 134500,
            likes = 10800,
            rating = 4.8f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("人像", "日系", "韩系", "写真"),
            createdAt = "2024-07-10"
        ),

        // === 夜景类 ===
        LUTResource(
            id = "night-city",
            name = "城市夜景",
            nameEn = "Night City",
            description = "城市夜景优化，增强霓虹灯光效果，适合城市夜景视频",
            category = "night",
            tags = listOf("夜景", "城市", "霓虹", "灯光"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/night/night_city_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("night_city_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 10,
            author = "OMaster Team",
            downloads = 145600,
            likes = 11200,
            rating = 4.8f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("夜景", "城市", "旅行", "延时"),
            createdAt = "2024-08-20"
        ),
        LUTResource(
            id = "night-street",
            name = "街头夜景",
            nameEn = "Street Night",
            description = "街头夜景风格，胶片感的夜晚街拍效果",
            category = "night",
            tags = listOf("夜景", "街头", "胶片", "街拍"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/night/night_street_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("night_street_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 9,
            author = "OMaster Team",
            downloads = 98700,
            likes = 7600,
            rating = 4.7f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("街拍", "夜景", "城市", "人文"),
            createdAt = "2024-09-05"
        ),

        // === 复古怀旧类 ===
        LUTResource(
            id = "vintage-80s",
            name = "80年代复古",
            nameEn = "Vintage 80s",
            description = "80年代复古风格，温暖的怀旧色调，适合复古主题视频",
            category = "vintage",
            tags = listOf("复古", "80年代", "怀旧", "温暖"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vintage/vintage_80s_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("vintage_80s_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 11,
            author = "OMaster Team",
            downloads = 167200,
            likes = 13400,
            rating = 4.8f,
            isFree = true,
            isHot = true,
            isNew = false,
            suitableFor = listOf("复古", "MV", "短片", "广告"),
            createdAt = "2024-05-20"
        ),
        LUTResource(
            id = "vintage-90s",
            name = "90年代胶片",
            nameEn = "Vintage 90s",
            description = "90年代胶片风格，带有轻微褪色感，适合怀旧内容",
            category = "vintage",
            tags = listOf("复古", "90年代", "胶片", "褪色"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vintage/vintage_90s_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("vintage_90s_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 10,
            author = "OMaster Team",
            downloads = 123400,
            likes = 9800,
            rating = 4.7f,
            isFree = true,
            isHot = false,
            isNew = false,
            suitableFor = listOf("复古", "Vlog", "短片", "纪录片"),
            createdAt = "2024-06-25"
        ),

        // === 2026年新品 ===
        LUTResource(
            id = "2026-oxygen",
            name = "氧气感2026",
            nameEn = "Oxygen 2026",
            description = "2026年流行氧气感风格，清新通透，适合春夏季节拍摄",
            category = "color",
            tags = listOf("氧气感", "清新", "2026", "春夏"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/2026/oxygen_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("2026_oxygen_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 9,
            author = "OMaster Team",
            downloads = 45600,
            likes = 4200,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = true,
            suitableFor = listOf("Vlog", "人像", "旅行", "春夏"),
            createdAt = "2026-01-15"
        ),
        LUTResource(
            id = "2026-morandi",
            name = "莫兰迪2026",
            nameEn = "Morandi 2026",
            description = "2026年流行莫兰迪色调，高级灰调色彩，适合艺术感视频",
            category = "color",
            tags = listOf("莫兰迪", "高级", "艺术", "2026"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/2026/morandi_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("2026_morandi_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 10,
            author = "OMaster Team",
            downloads = 38900,
            likes = 3600,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = true,
            suitableFor = listOf("艺术", "人像", "静物", "产品"),
            createdAt = "2026-02-01"
        ),
        LUTResource(
            id = "2026-hasselblad",
            name = "哈苏自然色彩",
            nameEn = "Hasselblad Natural",
            description = "哈苏自然色彩解决方案，专业级色彩还原，适合专业视频创作",
            category = "cinematic",
            tags = listOf("哈苏", "专业", "自然", "HNCS"),
            downloadUrl = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/2026/hasselblad_hncs_33.cube",
            previewImage = UrlConstants.getSampleImageUrl("2026_hasselblad_sample.jpg"),
            format = "cube",
            size = "33",
            fileSize = 14,
            author = "OMaster Team",
            downloads = 67800,
            likes = 5900,
            rating = 4.9f,
            isFree = true,
            isHot = true,
            isNew = true,
            suitableFor = listOf("专业", "风景", "人像", "商业"),
            createdAt = "2026-03-01"
        ),
    )

    fun getResources(category: String? = null): List<LUTResource> {
        if (category == null || category == "all") return RESOURCES
        return RESOURCES.filter { it.category == category }
    }

    fun searchResources(query: String): List<LUTResource> {
        val q = query.lowercase()
        return RESOURCES.filter {
            it.name.lowercase().contains(q) ||
            it.nameEn.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    fun getHotResources(): List<LUTResource> =
        RESOURCES.filter { it.isHot }.sortedByDescending { it.downloads }

    fun getNewResources(): List<LUTResource> =
        RESOURCES.filter { it.isNew }

    fun formatFileSize(kb: Int): String = when {
        kb >= 1024 -> String.format(java.util.Locale.US, "%.1f MB", kb / 1024f)
        else -> "$kb KB"
    }

    fun formatDownloads(count: Int): String = when {
        count >= 100000000 -> String.format(java.util.Locale.US, "%.1fB", count / 100000000f)
        count >= 1000000 -> String.format(java.util.Locale.US, "%.1fM", count / 1000000f)
        count >= 10000 -> String.format(java.util.Locale.US, "%.1fK", count / 1000f)
        else -> count.toString()
    }
}

data class LUTCategory(
    val key: String,
    val label: String,
    val icon: String
)
