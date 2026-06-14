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
     * 每个场景提供4条专业建议
     */
    fun getMasterTips(sceneId: String): List<String> = when {
        // 人像系列建议
        sceneId.startsWith("portrait") -> listOf(
            "📐 使用 2× 或 3× 长焦避免广角畸变，让人脸比例更自然",
            "☀️ 寻找柔和的侧光或窗户光，哈苏风格偏爱自然光影",
            "🎨 肤色还原是 HNCS 的核心——不过度美白，保留真实肤色",
            "📷 试试 Portra 400 胶片风格，温柔叙事感拉满"
        )
        // 逆光人像特殊建议
        sceneId == "portrait-backlit" -> listOf(
            "🌅 逆光拍摄保留高光细节，使用 HDR 或手动提升阴影",
            "✨ 柔光模式让轮廓光更加梦幻，哈苏特色效果",
            "📐 对焦眼睛，让逆光产生柔和的轮廓光效果",
            "📷 Portra 400 + 暗角 +20，逆光人像完美配方"
        )
        // 黑白人像特殊建议
        sceneId == "portrait-bw" -> listOf(
            "⚫ 关注光影对比，黑白人像的核心是明暗层次",
            "📐 简化背景元素，让人物轮廓更加突出",
            "☀️ 寻找硬光或侧光，增强黑白对比效果",
            "📷 TX400 胶片风格，经典黑白颗粒质感"
        )
        // 风景系列建议
        sceneId.startsWith("landscape") -> listOf(
            "🌅 黄金时刻（日出后/日落前 30 分钟）出片率最高",
            "📐 利用前景（岩石/树枝/花草）增加画面层次感",
            "🖼️ 试试 XPAN 宽幅模式，电影感构图一步到位",
            "🎨 浓郁胶片风格让蓝天更澄澈、绿植更鲜活"
        )
        // 日落特殊建议
        sceneId == "landscape-sunset" -> listOf(
            "☀️ 等待太阳接触地平线的瞬间，色彩最丰富",
            "📐 使用渐变滤镜平衡天空与地面曝光",
            "🎨 色温 +20 + 饱和度 +25，日落完美配方",
            "📷 RDP3 正片风格，反转片质感让日落更壮观"
        )
        // 夜景系列建议
        sceneId.startsWith("night") -> listOf(
            "📷 ISO 控制在 400 以内，手持拍摄找支撑点",
            "💡 寻找点光源（路灯/霓虹/橱窗）作为画面视觉锚点",
            "🌊 寻找水面拍摄，倒影让夜景层次翻倍",
            "🎞️ 800T 胶片专为夜景优化——不糊不噪"
        )
        // 星空特殊建议
        sceneId == "night-starry" -> listOf(
            "📍 远离城市光污染，寻找暗夜保护区",
            "📷 使用三脚架 + 高 ISO + 长曝光（15-30秒）",
            "🎨 对比度 +30 + 锐度 +25，星空细节最大化",
            "🎞️ TX400 黑白风格，星空摄影的经典选择"
        )
        // 美食系列建议
        sceneId.startsWith("food") -> listOf(
            "📐 45° 俯拍或平视 0° 特写，两种角度切换拍",
            "💡 寻找自然光或暖光，避免顶光造成难看的阴影",
            "🎨 暖 CCD 胶片风格让食物更有食欲",
            "📷 对焦在食物的纹理细节上——肉汁、糖霜、气泡"
        )
        // 街拍/建筑系列建议
        sceneId.startsWith("urban-street") || sceneId == "street" -> listOf(
            "🚶 等待决定性瞬间——一个人、一束光、一个故事",
            "⚫ TX400 黑白胶片让街头故事感翻倍",
            "📐 寻找几何线条和光影对比，哈苏黑白风格强调明暗反差",
            "🎯 预设对焦点在画面 1/3 处，抬手即拍"
        )
        sceneId.startsWith("urban-architecture") || sceneId == "architecture" -> listOf(
            "📐 寻找建筑的几何线条和对称结构",
            "☀️ 等待光线创造阴影，增强建筑立体感",
            "⚫ TX400 黑白风格让建筑更有力量感",
            "🎯 对比度 +25 + 锐度 +28，建筑细节最大化"
        )
        // 咖啡馆/博物馆特殊建议
        sceneId == "urban-cafe" -> listOf(
            "☕ 捕捉咖啡馆的氛围——咖啡杯、书本、光影",
            "💡 利用窗户光线，创造柔和温馨的氛围",
            "🎨 暖 CCD + 柔光模式，咖啡馆完美配方",
            "📷 45° 角度拍摄，展现空间层次"
        )
        // 静物系列建议
        sceneId.startsWith("still") -> listOf(
            "📐 简化背景，让主体更加突出",
            "💡 使用柔和的侧光或逆光，增强质感",
            "🎨 NC 自然风格保留静物的真实色彩",
            "📷 关注细节纹理——花瓣、叶片、质感"
        )
        // 微距系列建议
        sceneId.startsWith("macro") -> listOf(
            "🔍 使用微距镜头或长焦 + 近摄滤镜",
            "💡 柔和光线避免过曝，保留细节层次",
            "🎨 锐度 +25 + 对比度 +15，细节最大化",
            "📷 景深很浅，注意对焦精度"
        )
        // 活动系列建议
        sceneId.startsWith("event-wedding") -> listOf(
            "💒 捕捉重要瞬间——仪式、誓言、亲吻",
            "💡 注意光线柔和，避免硬光",
            "🎨 Portra 400 + 柔光，婚礼完美配方",
            "📷 多拍细节——戒指、花束、表情"
        )
        sceneId.startsWith("event-concert") -> listOf(
            "🎤 使用快速镜头 + 高 ISO，捕捉动态",
            "💡 注意舞台灯光变化，预判高潮时刻",
            "⚫ TX400 黑白风格让舞台更有戏剧感",
            "📷 连拍模式，捕捉最佳瞬间"
        )
        sceneId.startsWith("event") -> listOf(
            "🎉 捕捉欢乐瞬间——笑容、互动、氛围",
            "💡 注意光线变化，随时调整参数",
            "🎨 CC 经典负片风格，活动记录首选",
            "📷 多拍人物互动，记录真实情感"
        )
        // 默认建议
        else -> listOf(
            "📷 哈苏大师模式让每一张照片都有故事",
            "🎨 试试不同胶片风格，找到你的专属配方",
            "💡 关注光影变化，哈苏风格偏爱自然光",
            "📐 注意构图，三分法则永不过时"
        )
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
                name = sceneId.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
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
        else -> SceneCategory.PORTRAIT
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