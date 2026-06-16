package com.silas.omaster.ai.mapping

import com.silas.omaster.model.*
import kotlin.math.abs

/**
 * 5.3 场景→哈苏参数映射表
 * 
 * 提供三个核心映射功能：
 * 1. 场景 → 哈苏大师参数
 * 2. 场景 → 推荐胶片风格
 * 3. 场景 → 大师拍摄建议
 */
object SceneToHasselbladMapping {

    // ==================== 场景 → 哈苏参数映射 ====================

    /**
     * 根据场景ID获取哈苏大师参数
     * 参数范围：-30 ~ +30（对齐 OPPO 大师模式）
     */
    fun getParams(sceneId: String): HasselbladParams = when (sceneId) {
        // ─── 人像系列 ───
        "portrait-standard", "portrait" -> HasselbladParams(
            tone = -3, saturation = 10, contrast = -15,
            colorTemp = -5, sharpness = -15, vignette = 20,
            cyanMagenta = -5, softLight = SoftLightMode.SOFT
        )
        "portrait-backlit" -> HasselbladParams(
            tone = -5, saturation = 12, contrast = -10,
            colorTemp = -10, sharpness = -10, vignette = 25,
            cyanMagenta = -8, softLight = SoftLightMode.DREAMY
        )
        "portrait-studio" -> HasselbladParams(
            tone = 0, saturation = 0, contrast = 15,
            colorTemp = 0, sharpness = 0, vignette = 0,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "portrait-bw" -> HasselbladParams(
            tone = -10, saturation = -30, contrast = 25,
            colorTemp = 0, sharpness = 20, vignette = 15,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "portrait-group" -> HasselbladParams(
            tone = 0, saturation = 8, contrast = -10,
            colorTemp = 0, sharpness = 12, vignette = 0,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "portrait-child" -> HasselbladParams(
            tone = 5, saturation = 12, contrast = -8,
            colorTemp = 5, sharpness = -5, vignette = 10,
            cyanMagenta = 3, softLight = SoftLightMode.DREAMY
        )
        "portrait-couple" -> HasselbladParams(
            tone = 5, saturation = 10, contrast = -5,
            colorTemp = 10, sharpness = -10, vignette = 15,
            cyanMagenta = 5, softLight = SoftLightMode.SOFT
        )
        "portrait-senior" -> HasselbladParams(
            tone = -5, saturation = 5, contrast = 20,
            colorTemp = 0, sharpness = 8, vignette = 10,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )

        // ─── 风景系列 ───
        "landscape-standard", "landscape" -> HasselbladParams(
            tone = 5, saturation = 15, contrast = 12,
            colorTemp = 0, sharpness = 18, vignette = -5,
            cyanMagenta = -3, softLight = SoftLightMode.NONE
        )
        "landscape-sunset" -> HasselbladParams(
            tone = -5, saturation = 25, contrast = 10,
            colorTemp = 20, sharpness = 12, vignette = 0,
            cyanMagenta = 5, softLight = SoftLightMode.NONE
        )
        "landscape-sky" -> HasselbladParams(
            tone = 10, saturation = 10, contrast = 5,
            colorTemp = -15, sharpness = 15, vignette = -10,
            cyanMagenta = -10, softLight = SoftLightMode.NONE
        )
        "landscape-forest" -> HasselbladParams(
            tone = 3, saturation = 20, contrast = 10,
            colorTemp = 5, sharpness = 15, vignette = -8,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "landscape-autumn" -> HasselbladParams(
            tone = 0, saturation = 25, contrast = 10,
            colorTemp = 15, sharpness = 12, vignette = 0,
            cyanMagenta = 8, softLight = SoftLightMode.NONE
        )
        "landscape-snow" -> HasselbladParams(
            tone = 15, saturation = -5, contrast = -10,
            colorTemp = -5, sharpness = 10, vignette = -15,
            cyanMagenta = -8, softLight = SoftLightMode.NONE
        )
        "landscape-beach" -> HasselbladParams(
            tone = 10, saturation = 8, contrast = 5,
            colorTemp = -5, sharpness = 15, vignette = -10,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "landscape-waterfall" -> HasselbladParams(
            tone = 0, saturation = 10, contrast = 5,
            colorTemp = 0, sharpness = 20, vignette = -10,
            cyanMagenta = -3, softLight = SoftLightMode.NONE
        )
        "landscape-mountain" -> HasselbladParams(
            tone = 8, saturation = 12, contrast = 15,
            colorTemp = 0, sharpness = 20, vignette = 5,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "landscape-lake" -> HasselbladParams(
            tone = 5, saturation = 10, contrast = 5,
            colorTemp = 0, sharpness = 12, vignette = -5,
            cyanMagenta = -3, softLight = SoftLightMode.NONE
        )
        "landscape-desert" -> HasselbladParams(
            tone = -5, saturation = 15, contrast = 10,
            colorTemp = 15, sharpness = 18, vignette = 10,
            cyanMagenta = 10, softLight = SoftLightMode.NONE
        )

        // ─── 夜景系列 ───
        "night-city", "night" -> HasselbladParams(
            tone = -15, saturation = -5, contrast = 20,
            colorTemp = -5, sharpness = 15, vignette = 25,
            cyanMagenta = -10, softLight = SoftLightMode.NONE
        )
        "night-neon" -> HasselbladParams(
            tone = -18, saturation = 12, contrast = 25,
            colorTemp = -10, sharpness = 20, vignette = 20,
            cyanMagenta = -15, softLight = SoftLightMode.NONE
        )
        "night-starry" -> HasselbladParams(
            tone = -20, saturation = 10, contrast = 30,
            colorTemp = -15, sharpness = 25, vignette = 30,
            cyanMagenta = -10, softLight = SoftLightMode.NONE
        )
        "night-candle" -> HasselbladParams(
            tone = -10, saturation = 5, contrast = 10,
            colorTemp = 15, sharpness = -5, vignette = 20,
            cyanMagenta = 5, softLight = SoftLightMode.SOFT
        )
        "night-fireworks" -> HasselbladParams(
            tone = -5, saturation = 15, contrast = 20,
            colorTemp = 0, sharpness = 15, vignette = 10,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "night-moon" -> HasselbladParams(
            tone = -15, saturation = -5, contrast = 20,
            colorTemp = -10, sharpness = 20, vignette = 25,
            cyanMagenta = -8, softLight = SoftLightMode.NONE
        )
        "night-bridge" -> HasselbladParams(
            tone = -10, saturation = 5, contrast = 20,
            colorTemp = -5, sharpness = 18, vignette = 15,
            cyanMagenta = -12, softLight = SoftLightMode.NONE
        )

        // ─── 美食系列 ───
        "food-restaurant", "food" -> HasselbladParams(
            tone = -5, saturation = 15, contrast = 8,
            colorTemp = 10, sharpness = 20, vignette = -10,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "food-dessert" -> HasselbladParams(
            tone = 5, saturation = 20, contrast = 5,
            colorTemp = 5, sharpness = 18, vignette = -15,
            cyanMagenta = 5, softLight = SoftLightMode.SOFT
        )
        "food-drink" -> HasselbladParams(
            tone = 0, saturation = 10, contrast = 12,
            colorTemp = 5, sharpness = 15, vignette = -10,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "food-coffee" -> HasselbladParams(
            tone = -5, saturation = 8, contrast = 5,
            colorTemp = 10, sharpness = 12, vignette = -5,
            cyanMagenta = 3, softLight = SoftLightMode.SOFT
        )
        "food-bbq" -> HasselbladParams(
            tone = 0, saturation = 15, contrast = 10,
            colorTemp = 8, sharpness = 18, vignette = 0,
            cyanMagenta = 5, softLight = SoftLightMode.NONE
        )

        // ─── 城市/街拍系列 ───
        "urban-street", "street" -> HasselbladParams(
            tone = -5, saturation = 5, contrast = 18,
            colorTemp = 0, sharpness = 22, vignette = 10,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "urban-architecture", "architecture" -> HasselbladParams(
            tone = 5, saturation = 5, contrast = 25,
            colorTemp = 0, sharpness = 28, vignette = 0,
            cyanMagenta = -10, softLight = SoftLightMode.NONE
        )
        "urban-cafe" -> HasselbladParams(
            tone = -10, saturation = 8, contrast = 5,
            colorTemp = 15, sharpness = 10, vignette = 5,
            cyanMagenta = 8, softLight = SoftLightMode.SOFT
        )
        "urban-museum" -> HasselbladParams(
            tone = -5, saturation = 0, contrast = 15,
            colorTemp = 0, sharpness = 15, vignette = 0,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "urban-market" -> HasselbladParams(
            tone = 0, saturation = 12, contrast = 10,
            colorTemp = 5, sharpness = 18, vignette = 5,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "urban-station" -> HasselbladParams(
            tone = -8, saturation = 5, contrast = 15,
            colorTemp = -5, sharpness = 20, vignette = 10,
            cyanMagenta = -8, softLight = SoftLightMode.NONE
        )
        "urban-park" -> HasselbladParams(
            tone = 5, saturation = 10, contrast = 8,
            colorTemp = 5, sharpness = 15, vignette = -5,
            cyanMagenta = -3, softLight = SoftLightMode.NONE
        )

        // ─── 静物系列 ───
        "still-flower" -> HasselbladParams(
            tone = 5, saturation = 15, contrast = 5,
            colorTemp = 0, sharpness = 10, vignette = -10,
            cyanMagenta = 0, softLight = SoftLightMode.SOFT
        )
        "still-product" -> HasselbladParams(
            tone = 0, saturation = 5, contrast = 10,
            colorTemp = 0, sharpness = 15, vignette = -5,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "still-book" -> HasselbladParams(
            tone = -5, saturation = 5, contrast = 5,
            colorTemp = 5, sharpness = 10, vignette = 0,
            cyanMagenta = 3, softLight = SoftLightMode.SOFT
        )
        "still-art" -> HasselbladParams(
            tone = 0, saturation = 8, contrast = 12,
            colorTemp = 0, sharpness = 12, vignette = 0,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )

        // ─── 微距系列 ───
        "macro-insect" -> HasselbladParams(
            tone = 0, saturation = 10, contrast = 15,
            colorTemp = 0, sharpness = 25, vignette = -5,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "macro-water" -> HasselbladParams(
            tone = 10, saturation = 5, contrast = 10,
            colorTemp = -5, sharpness = 20, vignette = -10,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )
        "macro-texture" -> HasselbladParams(
            tone = 5, saturation = 0, contrast = 25,
            colorTemp = 0, sharpness = 30, vignette = 0,
            cyanMagenta = -8, softLight = SoftLightMode.NONE
        )

        // ─── 活动系列 ───
        "event-wedding" -> HasselbladParams(
            tone = 5, saturation = 10, contrast = -5,
            colorTemp = 10, sharpness = -10, vignette = 15,
            cyanMagenta = 5, softLight = SoftLightMode.SOFT
        )
        "event-party" -> HasselbladParams(
            tone = 0, saturation = 15, contrast = 10,
            colorTemp = 5, sharpness = 15, vignette = 5,
            cyanMagenta = 0, softLight = SoftLightMode.NONE
        )
        "event-concert" -> HasselbladParams(
            tone = -10, saturation = 15, contrast = 20,
            colorTemp = 15, sharpness = 20, vignette = 15,
            cyanMagenta = -10, softLight = SoftLightMode.NONE
        )
        "event-sports" -> HasselbladParams(
            tone = 0, saturation = 10, contrast = 15,
            colorTemp = 0, sharpness = 20, vignette = 0,
            cyanMagenta = -5, softLight = SoftLightMode.NONE
        )

        // 默认中性参数
        else -> HasselbladParams()
    }

    // ==================== 场景 → 胶片推荐 ====================

    /**
     * 根据场景ID获取推荐胶片列表
     * 返回 Top-3 推荐胶片及其匹配度
     */
    fun getRecommendedFilms(sceneId: String): List<FilmPreset> = when {
        // 人像系列：Portra 400 为首选
        sceneId.startsWith("portrait") -> listOf(
            FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.95f, "柔和肤色，人像首选"),
            FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.82f, "经典胶片质感"),
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.70f, "温馨氛围")
        )
        // 黑白人像：TX400 为首选
        sceneId == "portrait-bw" -> listOf(
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.98f, "经典黑白颗粒"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.65f, "高对比黑白")
        )
        // 风景系列：RDP3 为首选
        sceneId.startsWith("landscape") -> listOf(
            FilmPreset("rdp3", "RDP3 正片", FilmSeries.EMOTION, 0.93f, "反转片质感，高饱和"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.88f, "浓郁色彩"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.75f, "自然柔和")
        )
        // 日落/秋景：额外推荐暖色调胶片
        sceneId == "landscape-sunset" || sceneId == "landscape-autumn" -> listOf(
            FilmPreset("rdp3", "RDP3 正片", FilmSeries.EMOTION, 0.95f, "反转片质感"),
            FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.88f, "柔和暖调"),
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.80f, "数字暖调")
        )
        // 雪景：冷色调胶片
        sceneId == "landscape-snow" -> listOf(
            FilmPreset("ccd_cool", "冷 CCD", FilmSeries.DIGITAL, 0.90f, "清冷质感"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.82f, "纯净高对比"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.70f, "自然柔和")
        )
        // 夜景系列：800T 为首选
        sceneId.startsWith("night") -> listOf(
            FilmPreset("800t", "800T 夜景", FilmSeries.STRUCTURE, 0.96f, "夜景电影感"),
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.78f, "黑白夜景"),
            FilmPreset("ccd_cool", "冷 CCD", FilmSeries.DIGITAL, 0.65f, "冷调数字")
        )
        // 星空：黑白首选
        sceneId == "night-starry" -> listOf(
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.92f, "星空黑白"),
            FilmPreset("800t", "800T 夜景", FilmSeries.STRUCTURE, 0.85f, "电影感星空"),
            FilmPreset("ccd_cool", "冷 CCD", FilmSeries.DIGITAL, 0.70f, "冷调星空")
        )
        // 美食系列：暖 CCD 为首选
        sceneId.startsWith("food") -> listOf(
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.90f, "食欲感暖调"),
            FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.85f, "经典质感"),
            FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.72f, "柔和自然")
        )
        // 街拍/建筑：TX400 黑白为首选
        sceneId.startsWith("urban-street") || sceneId.startsWith("urban-architecture") ||
        sceneId == "street" || sceneId == "architecture" -> listOf(
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.92f, "街头黑白"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.80f, "高对比"),
            FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.74f, "经典质感")
        )
        // 咖啡馆/博物馆：柔和风格
        sceneId == "urban-cafe" || sceneId == "urban-museum" -> listOf(
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.88f, "温馨氛围"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.82f, "自然柔和"),
            FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.75f, "柔和质感")
        )
        // 静物系列：自然风格
        sceneId.startsWith("still") -> listOf(
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.88f, "自然质感"),
            FilmPreset("rdp3", "RDP3 正片", FilmSeries.EMOTION, 0.82f, "高饱和"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.75f, "浓郁色彩")
        )
        // 微距系列：高细节风格
        sceneId.startsWith("macro") -> listOf(
            FilmPreset("rdp3", "RDP3 正片", FilmSeries.EMOTION, 0.90f, "细节高饱和"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.85f, "浓郁质感"),
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.78f, "黑白纹理")
        )
        // 活动系列：根据类型推荐
        sceneId.startsWith("event-wedding") -> listOf(
            FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.95f, "婚礼首选"),
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.88f, "温馨氛围"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.80f, "自然记录")
        )
        sceneId.startsWith("event-concert") -> listOf(
            FilmPreset("tx400", "TX400 黑白", FilmSeries.STRUCTURE, 0.90f, "舞台黑白"),
            FilmPreset("800t", "800T 夜景", FilmSeries.STRUCTURE, 0.85f, "灯光效果"),
            FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.78f, "浓郁色彩")
        )
        sceneId.startsWith("event") -> listOf(
            FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.88f, "活动记录"),
            FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.82f, "温馨氛围"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.75f, "自然记录")
        )
        // 默认推荐
        else -> listOf(
            FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.85f, "经典胶片质感"),
            FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.80f, "自然柔和")
        )
    }

    // ==================== 场景 → 大师拍摄建议 ====================

    /**
     * 根据场景ID获取哈苏大师拍摄建议
     *
     * 修复 #13：原 getMasterTips 是 35 个场景全模板的 4 句同一结构拼接
     * 现在拆为 4 个独立方法按场景 ID 真正定制：
     *  - compositionTips：构图建议（按场景定制）
     *  - lightingTips：光线建议（夜景与日落完全不同的内容）
     *  - colorTips：色彩建议（按场景的胶片/色调方向给差异化文案）
     *  - filmTip：胶片建议（与 recommendFilms 联动）
     */
    fun getMasterTips(sceneId: String): List<String> {
        val composition = getCompositionTips(sceneId)
        val lighting = getLightingTips(sceneId)
        val color = getColorTips(sceneId)
        val film = getFilmTip(sceneId)
        return listOf(composition, lighting, color, film)
    }

    /**
     * 构图建议（按场景定制）
     *
     * 修复：顺序很重要——具体场景 ID 必须在泛化匹配（如 startsWith）之前
     */
    fun getCompositionTips(sceneId: String): String = when {
        // 具体场景优先（必须在泛化匹配之前）
        sceneId == "portrait-backlit" -> "🌅 对焦眼睛，让逆光产生柔和的轮廓光效果"
        sceneId == "portrait-bw" -> "📐 简化背景元素，让人物轮廓更加突出"
        sceneId == "portrait-group" || sceneId == "portrait-couple" -> "📐 三角构图或对称布局，让画面平衡稳定"
        sceneId == "landscape-sunset" -> "📐 使用渐变滤镜平衡天空与地面曝光"
        sceneId == "night-starry" -> "📐 借助三脚架，构图时给地面前景留 1/3"
        sceneId == "night-moon" -> "📐 月亮置于黄金分割点，前景用剪影增强戏剧性"
        sceneId == "urban-street" || sceneId == "street" -> "🚶 等待决定性瞬间——一个人、一束光、一个故事"
        sceneId == "urban-cafe" -> "📐 45° 角度拍摄咖啡杯与桌面互动，展现空间层次"
        // 泛化匹配（放在具体场景之后）
        sceneId.startsWith("landscape") -> "📐 利用前景（岩石/树枝/花草）增加画面层次感"
        sceneId.startsWith("night") -> "📐 寻找点光源（路灯/霓虹/橱窗）作为画面视觉锚点"
        sceneId.startsWith("urban-architecture") || sceneId == "architecture" -> "📐 寻找建筑的几何线条和对称结构"
        sceneId.startsWith("food") -> "📐 45° 俯拍或平视 0° 特写，两种角度切换拍"
        sceneId.startsWith("still") -> "📐 简化背景，让主体更加突出"
        sceneId.startsWith("macro") -> "🔍 使用微距镜头或长焦 + 近摄滤镜，景深很浅注意对焦精度"
        sceneId.startsWith("event-wedding") -> "💒 捕捉重要瞬间——仪式、誓言、亲吻"
        sceneId.startsWith("event-concert") -> "🎤 寻找舞台上的瞬间戏剧感"
        sceneId.startsWith("event") -> "🎉 捕捉欢乐瞬间——笑容、互动、氛围"
        sceneId.startsWith("portrait") -> "📐 使用 2× 或 3× 长焦避免广角畸变，让人脸比例更自然"
        else -> "📐 运用三分法构图，把主体放在画面 1/3 处"
    }

    /**
     * 光线建议（夜景 / 日落 / 逆光 / 室内等场景内容完全独立）
     *
     * 修复：具体场景 ID 优先于泛化匹配
     */
    fun getLightingTips(sceneId: String): String = when {
        // 夜景具体场景（必须在 startsWith("night") 之前）
        sceneId == "night-city" -> "💡 ISO 控制在 400 以内，找栏杆/三脚架等支撑点防抖"
        sceneId == "night-neon" -> "💡 借霓虹灯本身做主光源，避免补光破坏氛围"
        sceneId == "night-starry" -> "💡 三脚架 + 高 ISO + 长曝光（15-30秒），让星轨自然形成"
        sceneId == "night-candle" -> "🕯️ 借烛光作为暖色主光，脸部补光用白色卡片反射"
        sceneId == "night-fireworks" -> "💡 用 2-4 秒长曝光捕捉烟花轨迹，三脚架必备"
        sceneId == "night-moon" -> "🌙 利用月光作为弱主光，前景剪影提对比"
        sceneId == "night-bridge" -> "💡 桥体自身灯光即主光，等待行人与车流形成动线"
        // 风景具体场景（必须在 startsWith("landscape") 之前）
        sceneId == "landscape-sunset" -> "☀️ 等待太阳接触地平线的瞬间，色彩最丰富"
        sceneId == "landscape-forest" -> "☀️ 寻找林间透光的丁达尔光束（晨雾/雨后更明显）"
        sceneId == "landscape-snow" -> "☀️ 雪地反射强，曝光补偿 +0.7 ~ +1.3 EV"
        sceneId == "landscape-beach" -> "☀️ 避开正午顶光，黄昏侧光让沙纹与水波立体"
        // 人像具体场景
        sceneId == "portrait-backlit" -> "🌅 逆光拍摄保留高光细节，使用 HDR 或手动提升阴影"
        sceneId == "portrait-bw" -> "☀️ 寻找硬光或侧光，增强黑白对比效果"
        // 街拍具体场景
        sceneId == "urban-street" || sceneId == "street" -> "💡 街灯、橱窗、车灯都是天然主光，弱光下大胆提高 ISO"
        sceneId == "urban-cafe" -> "💡 利用窗户光线，创造柔和温馨的氛围"
        sceneId == "urban-museum" -> "💡 室内混合光复杂，关闭自动白平衡，尝试钨丝灯模式"
        // 泛化匹配（放在具体场景之后）
        sceneId.startsWith("night") -> "💡 ISO 控制在 400 以内，手持拍摄找支撑点"
        sceneId.startsWith("landscape") -> "🌅 黄金时刻（日出后/日落前 30 分钟）出片率最高"
        sceneId.startsWith("urban-architecture") || sceneId == "architecture" -> "☀️ 等待侧光创造阴影，增强建筑立体感"
        sceneId.startsWith("food") -> "💡 寻找自然光或暖光，避免顶光造成难看的阴影"
        sceneId.startsWith("still") -> "💡 使用柔和的侧光或逆光，增强质感"
        sceneId.startsWith("macro") -> "💡 使用环形灯或柔光罩避免金属反光，保留细节层次"
        sceneId.startsWith("event-wedding") -> "💡 室内弱光下 RAW 拍摄 + 高 ISO，闪光灯柔化"
        sceneId.startsWith("event-concert") -> "💡 注意舞台灯光变化，预判高潮时刻"
        sceneId.startsWith("event") -> "💡 室内弱光利用舞台灯/手机补光，避免顶光"
        sceneId.startsWith("portrait") -> "☀️ 寻找柔和的侧光或窗户光，哈苏风格偏爱自然光影"
        else -> "💡 关注光影变化，哈苏风格偏爱自然光"
    }

    /**
     * 色彩建议（按场景的胶片/色调方向给差异化文案）
     *
     * 修复：具体场景 ID 优先于泛化匹配
     */
    fun getColorTips(sceneId: String): String = when {
        // 人像具体场景（必须在 startsWith("portrait") 之前）
        sceneId == "portrait-backlit" -> "✨ 柔光模式让轮廓光更加梦幻，哈苏特色效果"
        sceneId == "portrait-bw" -> "⚫ 关注光影对比，黑白人像的核心是明暗层次"
        sceneId == "portrait-studio" -> "🎨 棚拍保持肤色中性，避免偏色"
        sceneId == "portrait-senior" -> "🎨 保留皮肤纹理细节，避免过度磨皮"
        // 风景具体场景（必须在 startsWith("landscape") 之前）
        sceneId == "landscape-sunset" -> "🎨 色温 +20 + 饱和度 +25，日落完美配方"
        sceneId == "landscape-autumn" -> "🍁 强调暖橙色调，让黄叶更浓郁"
        sceneId == "landscape-snow" -> "❄️ 降低饱和度，保留白雪的纯净高对比"
        sceneId == "landscape-forest" -> "🌲 强化绿色通道，让林间层次更分明"
        sceneId == "landscape-sky" -> "☁️ 提升蓝色饱和度但避免过深，保留云的层次"
        // 街拍 / 建筑具体场景
        sceneId == "urban-street" || sceneId == "street" -> "⚫ TX400 黑白胶片让街头故事感翻倍"
        sceneId == "urban-cafe" -> "🎨 暖 CCD + 柔光模式，咖啡馆完美配方"
        sceneId == "urban-museum" -> "🎨 保留展品原色调，白平衡锁定"
        // 泛化匹配（放在具体场景之后）
        sceneId.startsWith("portrait") -> "🎨 肤色还原是 HNCS 的核心——不过度美白，保留真实肤色"
        sceneId.startsWith("landscape") -> "🖼️ 浓郁胶片风格让蓝天更澄澈、绿植更鲜活"
        sceneId.startsWith("night") -> "🌊 寻找水面拍摄，倒影让夜景层次翻倍"
        sceneId.startsWith("food") -> "🎨 暖 CCD 胶片风格让食物更有食欲"
        sceneId.startsWith("urban-architecture") || sceneId == "architecture" -> "⚫ TX400 黑白风格让建筑更有力量感"
        sceneId.startsWith("still") -> "🎨 NC 自然风格保留静物的真实色彩"
        sceneId.startsWith("macro") -> "🎨 锐度 +25 + 对比度 +15，细节最大化"
        sceneId.startsWith("event-wedding") -> "🎨 Portra 400 + 柔光，婚礼完美配方"
        sceneId.startsWith("event-concert") -> "⚫ TX400 黑白风格让舞台更有戏剧感"
        sceneId.startsWith("event") -> "🎨 CC 经典负片风格，活动记录首选"
        else -> "🎨 试试不同胶片风格，找到你的专属配方"
    }

    /**
     * 胶片建议（与 getRecommendedFilms 联动）
     */
    fun getFilmTip(sceneId: String): String {
        val topFilm = getRecommendedFilms(sceneId).firstOrNull()
        val name = topFilm?.name ?: "CC 经典负片"
        return when {
            sceneId.startsWith("portrait") -> "📷 试试 $name 胶片风格，温柔叙事感拉满"
            sceneId.startsWith("landscape") -> "📷 $name 风格，让自然色彩更通透"
            sceneId.startsWith("night") -> "🎞️ $name 专为夜景优化——不糊不噪"
            sceneId.startsWith("food") -> "📷 对焦在食物的纹理细节上，配 $name 风味更佳"
            sceneId == "urban-street" || sceneId == "street" -> "🎯 预设对焦点在画面 1/3 处，配 $name 抬手即拍"
            sceneId.startsWith("urban-architecture") || sceneId == "architecture" -> "🎯 对比度 +25 + 锐度 +28，配 $name 建筑细节最大化"
            sceneId.startsWith("macro") -> "📷 景深很浅，$name 帮你保留每一处细节"
            sceneId.startsWith("event") -> "📷 多拍人物互动，$name 记录真实情感"
            else -> "📷 哈苏大师模式 + $name 让每一张照片都有故事"
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据场景ID获取完整的场景画像
     */
    fun getSceneProfile(sceneId: String, confidence: Float = 0.85f): SceneProfile {
        val preset = ScenePresets.getSceneById(sceneId)
        return if (preset != null) {
            preset.copy(
                confidence = confidence,
                hasselbladParams = getParams(sceneId),
                recommendedFilm = getRecommendedFilms(sceneId),
                masterTips = getMasterTips(sceneId)
            )
        } else {
            // 创建默认场景画像
            SceneProfile(
                id = sceneId,
                name = sceneId.replace("-", " ").capitalize(),
                category = inferCategory(sceneId),
                description = "通用场景",
                color = 0xFFFF6B35,
                confidence = confidence,
                hasselbladParams = getParams(sceneId),
                recommendedFilm = getRecommendedFilms(sceneId),
                masterTips = getMasterTips(sceneId)
            )
        }
    }

    /**
     * 根据场景ID推断类别
     * 改善：fallback 改为 UNKNOWN + 走"通用胶片"分支，避免未识别场景一律当人像
     */
    private fun inferCategory(sceneId: String): SceneCategory = when {
        sceneId.startsWith("portrait") -> SceneCategory.PORTRAIT
        sceneId.startsWith("landscape") -> SceneCategory.LANDSCAPE
        sceneId.startsWith("night") -> SceneCategory.NIGHT
        sceneId.startsWith("food") -> SceneCategory.FOOD
        sceneId.startsWith("urban") || sceneId == "street" || sceneId == "architecture" -> SceneCategory.URBAN
        sceneId.startsWith("still") -> SceneCategory.STILL_LIFE
        sceneId.startsWith("macro") -> SceneCategory.MACRO
        sceneId.startsWith("event") -> SceneCategory.EVENT
        else -> SceneCategory.UNKNOWN  // 改善：改为 UNKNOWN 而非默认 PORTRAIT
    }

    /**
     * 获取参数调整建议（基于当前参数与推荐参数的差异）
     */
    fun getParamAdjustmentAdvice(
        currentParams: HasselbladParams,
        targetSceneId: String
    ): List<ParamAdjustment> {
        val targetParams = getParams(targetSceneId)
        val adjustments = mutableListOf<ParamAdjustment>()

        // 计算各参数差异
        if (currentParams.tone != targetParams.tone) {
            adjustments.add(ParamAdjustment(
                param = "tone",
                displayName = "影调",
                currentValue = currentParams.tone,
                targetValue = targetParams.tone,
                delta = targetParams.tone - currentParams.tone
            ))
        }
        if (currentParams.saturation != targetParams.saturation) {
            adjustments.add(ParamAdjustment(
                param = "saturation",
                displayName = "饱和度",
                currentValue = currentParams.saturation,
                targetValue = targetParams.saturation,
                delta = targetParams.saturation - currentParams.saturation
            ))
        }
        if (currentParams.contrast != targetParams.contrast) {
            adjustments.add(ParamAdjustment(
                param = "contrast",
                displayName = "对比度",
                currentValue = currentParams.contrast,
                targetValue = targetParams.contrast,
                delta = targetParams.contrast - currentParams.contrast
            ))
        }
        if (currentParams.colorTemp != targetParams.colorTemp) {
            adjustments.add(ParamAdjustment(
                param = "colorTemp",
                displayName = "色温",
                currentValue = currentParams.colorTemp,
                targetValue = targetParams.colorTemp,
                delta = targetParams.colorTemp - currentParams.colorTemp
            ))
        }
        if (currentParams.vignette != targetParams.vignette) {
            adjustments.add(ParamAdjustment(
                param = "vignette",
                displayName = "暗角",
                currentValue = currentParams.vignette,
                targetValue = targetParams.vignette,
                delta = targetParams.vignette - currentParams.vignette
            ))
        }

        return adjustments.sortedByDescending { abs(it.delta) }
    }

    /**
     * 参数调整建议
     */
    data class ParamAdjustment(
        val param: String,
        val displayName: String,
        val currentValue: Int,
        val targetValue: Int,
        val delta: Int
    ) {
        val advice: String = when {
            delta > 0 -> "建议提升 ${displayName} ${abs(delta)} 点"
            delta < 0 -> "建议降低 ${displayName} ${abs(delta)} 点"
            else -> "${displayName} 已达到推荐值"
        }
    }
}