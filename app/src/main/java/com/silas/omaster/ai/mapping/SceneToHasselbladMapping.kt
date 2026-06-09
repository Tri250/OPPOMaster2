package com.silas.omaster.ai.mapping

import com.silas.omaster.ai.model.FilmPreset
import com.silas.omaster.ai.model.FilmSeries
import com.silas.omaster.ai.model.HasselbladParams
import com.silas.omaster.ai.model.SoftLightMode

/**
 * 场景→哈苏参数映射表
 * 将识别的场景映射到对应的哈苏大师参数和胶片推荐
 */
object SceneToHasselbladMapping {

    /**
     * 获取哈苏大师参数
     * 所有参数范围: -30 ~ +30
     *
     * @param sceneId 场景ID
     * @return 对应的哈苏参数配置
     */
    fun getParams(sceneId: String): HasselbladParams = when (sceneId) {
        // ─── 人像系列 ───
        "portrait", "portrait-standard" -> HasselbladParams(
            tone = -3,
            saturation = 10,
            contrast = -15,
            colorTemp = -5,
            sharpness = -15,
            vignette = 20,
            cyanMagenta = -5,
            softLight = SoftLightMode.SOFT
        )

        "portrait-backlit" -> HasselbladParams(
            tone = -5,
            saturation = 12,
            contrast = -10,
            colorTemp = -10,
            sharpness = -10,
            vignette = 25,
            cyanMagenta = -8,
            softLight = SoftLightMode.DREAMY
        )

        "portrait-studio" -> HasselbladParams(
            tone = 0,
            saturation = 0,
            contrast = 15,
            colorTemp = 0,
            sharpness = 10,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        "portrait-bw" -> HasselbladParams(
            tone = -10,
            saturation = -30,
            contrast = 25,
            colorTemp = 0,
            sharpness = 20,
            vignette = 15,
            cyanMagenta = 0,
            softLight = SoftLightMode.SOFT
        )

        "portrait-group" -> HasselbladParams(
            tone = 0,
            saturation = 8,
            contrast = -10,
            colorTemp = 0,
            sharpness = 12,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        "portrait-children" -> HasselbladParams(
            tone = 5,
            saturation = 12,
            contrast = -8,
            colorTemp = 10,
            sharpness = 0,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.DREAMY
        )

        // ─── 风景系列 ───
        "landscape", "landscape-standard" -> HasselbladParams(
            tone = 5,
            saturation = 15,
            contrast = 12,
            colorTemp = 0,
            sharpness = 18,
            vignette = -5,
            cyanMagenta = -3,
            softLight = SoftLightMode.NONE
        )

        "landscape-sunset" -> HasselbladParams(
            tone = -5,
            saturation = 25,
            contrast = 10,
            colorTemp = 20,
            sharpness = 12,
            vignette = 0,
            cyanMagenta = 5,
            softLight = SoftLightMode.NONE
        )

        "landscape-blue-sky" -> HasselbladParams(
            tone = 10,
            saturation = 10,
            contrast = 8,
            colorTemp = -15,
            sharpness = 12,
            vignette = 0,
            cyanMagenta = -5,
            softLight = SoftLightMode.NONE
        )

        "landscape-forest" -> HasselbladParams(
            tone = 3,
            saturation = 20,
            contrast = 10,
            colorTemp = 5,
            sharpness = 15,
            vignette = -8,
            cyanMagenta = -5,
            softLight = SoftLightMode.NONE
        )

        "landscape-autumn" -> HasselbladParams(
            tone = 0,
            saturation = 25,
            contrast = 10,
            colorTemp = 15,
            sharpness = 12,
            vignette = 5,
            cyanMagenta = 3,
            softLight = SoftLightMode.SOFT
        )

        "landscape-snow" -> HasselbladParams(
            tone = 15,
            saturation = -5,
            contrast = -10,
            colorTemp = -10,
            sharpness = 8,
            vignette = 0,
            cyanMagenta = -5,
            softLight = SoftLightMode.SOFT
        )

        "landscape-beach" -> HasselbladParams(
            tone = 10,
            saturation = 8,
            contrast = 5,
            colorTemp = -5,
            sharpness = 10,
            vignette = 0,
            cyanMagenta = -3,
            softLight = SoftLightMode.NONE
        )

        "landscape-waterfall" -> HasselbladParams(
            tone = 0,
            saturation = 10,
            contrast = 8,
            colorTemp = 0,
            sharpness = 20,
            vignette = -10,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 夜景系列 ───
        "night", "night-city" -> HasselbladParams(
            tone = -18,
            saturation = 12,
            contrast = 25,
            colorTemp = -10,
            sharpness = 20,
            vignette = 20,
            cyanMagenta = -15,
            softLight = SoftLightMode.NONE
        )

        "night-neon" -> HasselbladParams(
            tone = 0,
            saturation = 20,
            contrast = 15,
            colorTemp = -10,
            sharpness = 10,
            vignette = 5,
            cyanMagenta = 3,
            softLight = SoftLightMode.SOFT
        )

        "night-starry" -> HasselbladParams(
            tone = -20,
            saturation = 10,
            contrast = 30,
            colorTemp = -15,
            sharpness = 25,
            vignette = 30,
            cyanMagenta = -10,
            softLight = SoftLightMode.NONE
        )

        "night-candlelight" -> HasselbladParams(
            tone = -10,
            saturation = 5,
            contrast = 5,
            colorTemp = 15,
            sharpness = 0,
            vignette = 10,
            cyanMagenta = -5,
            softLight = SoftLightMode.SOFT
        )

        "night-fireworks" -> HasselbladParams(
            tone = 0,
            saturation = 15,
            contrast = 20,
            colorTemp = 0,
            sharpness = 15,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 美食系列 ───
        "food", "food-restaurant" -> HasselbladParams(
            tone = -5,
            saturation = 15,
            contrast = 8,
            colorTemp = 10,
            sharpness = 20,
            vignette = -10,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        "food-dessert" -> HasselbladParams(
            tone = 5,
            saturation = 20,
            contrast = 5,
            colorTemp = 5,
            sharpness = 18,
            vignette = -15,
            cyanMagenta = 5,
            softLight = SoftLightMode.DREAMY
        )

        "food-drink" -> HasselbladParams(
            tone = 0,
            saturation = 10,
            contrast = 12,
            colorTemp = 5,
            sharpness = 10,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 城市系列 ───
        "urban-street", "street" -> HasselbladParams(
            tone = -5,
            saturation = 5,
            contrast = 18,
            colorTemp = 0,
            sharpness = 22,
            vignette = 10,
            cyanMagenta = -5,
            softLight = SoftLightMode.NONE
        )

        "urban-architecture", "architecture" -> HasselbladParams(
            tone = 5,
            saturation = 5,
            contrast = 25,
            colorTemp = 0,
            sharpness = 28,
            vignette = 0,
            cyanMagenta = -10,
            softLight = SoftLightMode.NONE
        )

        "urban-cafe" -> HasselbladParams(
            tone = -10,
            saturation = 8,
            contrast = 5,
            colorTemp = 15,
            sharpness = 5,
            vignette = 5,
            cyanMagenta = -3,
            softLight = SoftLightMode.SOFT
        )

        "urban-museum" -> HasselbladParams(
            tone = -5,
            saturation = 0,
            contrast = 15,
            colorTemp = 0,
            sharpness = 10,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 宠物系列 ───
        "pet-standard" -> HasselbladParams(
            tone = 5,
            saturation = 12,
            contrast = 5,
            colorTemp = 5,
            sharpness = 10,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.SOFT
        )

        "pet-outdoor" -> HasselbladParams(
            tone = 10,
            saturation = 15,
            contrast = 8,
            colorTemp = 0,
            sharpness = 15,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 微距系列 ───
        "macro-flower" -> HasselbladParams(
            tone = 5,
            saturation = 18,
            contrast = 10,
            colorTemp = 5,
            sharpness = 25,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.SOFT
        )

        "macro-insect" -> HasselbladParams(
            tone = 0,
            saturation = 10,
            contrast = 15,
            colorTemp = 0,
            sharpness = 30,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.NONE
        )

        // ─── 特殊系列 ───
        "special-concert" -> HasselbladParams(
            tone = -10,
            saturation = 15,
            contrast = 20,
            colorTemp = 0,
            sharpness = 10,
            vignette = 10,
            cyanMagenta = 0,
            softLight = SoftLightMode.SOFT
        )

        "special-wedding" -> HasselbladParams(
            tone = 0,
            saturation = 10,
            contrast = 5,
            colorTemp = 10,
            sharpness = 8,
            vignette = 5,
            cyanMagenta = 0,
            softLight = SoftLightMode.DREAMY
        )

        "special-aquarium" -> HasselbladParams(
            tone = 0,
            saturation = 5,
            contrast = 10,
            colorTemp = -15,
            sharpness = 15,
            vignette = 0,
            cyanMagenta = -5,
            softLight = SoftLightMode.NONE
        )

        "special-party" -> HasselbladParams(
            tone = 5,
            saturation = 15,
            contrast = 10,
            colorTemp = 5,
            sharpness = 8,
            vignette = 0,
            cyanMagenta = 0,
            softLight = SoftLightMode.SOFT
        )

        // ─── 默认中性 ───
        else -> HasselbladParams()
    }

    /**
     * 获取推荐胶片列表
     * 根据场景类型推荐匹配度最高的胶片
     *
     * @param sceneId 场景ID
     * @return 推荐胶片列表（按匹配度排序）
     */
    fun getRecommendedFilms(sceneId: String): List<FilmPreset> = when {
        // ─── 人像系列 ───
        sceneId.startsWith("portrait") -> listOf(
            createFilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.95f),
            createFilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.82f),
            createFilmPreset("ccd_warm", "暖调 CCD", FilmSeries.DIGITAL, 0.70f)
        )

        // ─── 风景系列 ───
        sceneId.startsWith("landscape") -> listOf(
            createFilmPreset("rdp3", "RDP3", FilmSeries.EMOTION, 0.93f),
            createFilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.88f),
            createFilmPreset("nc", "富士 NC", FilmSeries.CLASSIC, 0.75f)
        )

        // ─── 夜景系列 ───
        sceneId.startsWith("night") -> listOf(
            createFilmPreset("800t", "800T", FilmSeries.STRUCTURE, 0.96f),
            createFilmPreset("tx400", "TX400", FilmSeries.STRUCTURE, 0.78f),
            createFilmPreset("ccd_cool", "冷调 CCD", FilmSeries.DIGITAL, 0.65f)
        )

        // ─── 美食系列 ───
        sceneId.startsWith("food") -> listOf(
            createFilmPreset("ccd_warm", "暖调 CCD", FilmSeries.DIGITAL, 0.90f),
            createFilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.85f),
            createFilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.72f)
        )

        // ─── 城市系列 ───
        sceneId.startsWith("urban") || sceneId == "street" || sceneId == "architecture" -> listOf(
            createFilmPreset("tx400", "TX400", FilmSeries.STRUCTURE, 0.92f),
            createFilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.80f),
            createFilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.74f)
        )

        // ─── 宠物系列 ───
        sceneId.startsWith("pet") -> listOf(
            createFilmPreset("ccd_warm", "暖调 CCD", FilmSeries.DIGITAL, 0.85f),
            createFilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.80f),
            createFilmPreset("nc", "富士 NC", FilmSeries.CLASSIC, 0.70f)
        )

        // ─── 微距系列 ───
        sceneId.startsWith("macro") -> listOf(
            createFilmPreset("rdp3", "RDP3", FilmSeries.EMOTION, 0.85f),
            createFilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.80f),
            createFilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.70f)
        )

        // ─── 特殊系列 ───
        sceneId.startsWith("special") -> listOf(
            createFilmPreset("800t", "800T", FilmSeries.STRUCTURE, 0.85f),
            createFilmPreset("tx400", "TX400", FilmSeries.STRUCTURE, 0.80f),
            createFilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.70f)
        )

        // ─── 默认推荐 ───
        else -> listOf(
            createFilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.85f),
            createFilmPreset("nc", "富士 NC", FilmSeries.CLASSIC, 0.80f),
            createFilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.75f)
        )
    }

    /**
     * 获取场景拍摄建议
     *
     * @param sceneId 场景ID
     * @return 拍摄建议列表
     */
    fun getMasterTips(sceneId: String): List<String> = when (sceneId) {
        // ─── 人像系列 ───
        "portrait-standard" -> listOf(
            "使用柔光模式营造自然肤色",
            "降低对比度保持皮肤质感",
            "注意眼神光和面部轮廓"
        )

        "portrait-backlit" -> listOf(
            "利用逆光创造光晕效果",
            "梦幻柔光增强氛围感",
            "注意面部曝光补偿"
        )

        "portrait-studio" -> listOf(
            "高对比度突出轮廓",
            "注意光影造型",
            "控制背景简洁"
        )

        "portrait-bw" -> listOf(
            "黑白摄影注重光影对比",
            "寻找有纹理和质感的场景",
            "注意构图简洁有力"
        )

        "portrait-group" -> listOf(
            "确保所有人清晰可见",
            "注意站位和表情",
            "使用小光圈保证景深"
        )

        "portrait-children" -> listOf(
            "梦幻柔光营造童话氛围",
            "捕捉自然表情和动作",
            "注意安全距离"
        )

        // ─── 风景系列 ───
        "landscape-standard" -> listOf(
            "使用HDR增强动态范围",
            "注意构图层次",
            "寻找前景增加纵深感"
        )

        "landscape-sunset" -> listOf(
            "增强暖色调表现",
            "寻找有层次的天空",
            "注意曝光控制"
        )

        "landscape-blue-sky" -> listOf(
            "冷色调增强蓝色表现",
            "注意天空与地面比例",
            "寻找有趣的云层形态"
        )

        "landscape-forest" -> listOf(
            "增强绿色表现力",
            "利用光线穿透树叶",
            "寻找有趣的树木形态"
        )

        "landscape-autumn" -> listOf(
            "增强暖色调表现",
            "寻找色彩丰富的秋叶",
            "注意光影层次"
        )

        "landscape-snow" -> listOf(
            "冷色调增强雪景纯净感",
            "注意曝光补偿防止过曝",
            "寻找有趣的雪景形态"
        )

        "landscape-beach" -> listOf(
            "冷色调增强海水表现",
            "注意天空与海面比例",
            "寻找有趣的海岸线"
        )

        "landscape-waterfall" -> listOf(
            "高锐度增强水流质感",
            "可尝试慢门拍摄",
            "注意安全距离"
        )

        // ─── 夜景系列 ───
        "night-city" -> listOf(
            "高对比度增强光影层次",
            "注意曝光控制",
            "寻找有灯光的建筑"
        )

        "night-neon" -> listOf(
            "增强色彩饱和度",
            "柔光营造梦幻感",
            "寻找有霓虹灯招牌的场景"
        )

        "night-starry" -> listOf(
            "高对比度增强星空层次",
            "使用长曝光",
            "远离城市光污染"
        )

        "night-candlelight" -> listOf(
            "暖色调增强温馨感",
            "柔光营造氛围",
            "注意曝光控制"
        )

        "night-fireworks" -> listOf(
            "高对比度增强烟花层次",
            "使用长曝光捕捉轨迹",
            "寻找开阔视野"
        )

        // ─── 美食系列 ───
        "food-restaurant" -> listOf(
            "暖色调增强食欲感",
            "注意食物摆放角度",
            "寻找最佳光线位置"
        )

        "food-dessert" -> listOf(
            "梦幻柔光营造温馨感",
            "注意甜点摆放角度",
            "寻找简洁背景"
        )

        "food-drink" -> listOf(
            "注意饮品透明度表现",
            "寻找有趣的光线角度",
            "注意冰块和气泡"
        )

        // ─── 城市系列 ───
        "urban-street" -> listOf(
            "高对比度增强街头质感",
            "捕捉真实生活瞬间",
            "注意构图简洁"
        )

        "urban-architecture" -> listOf(
            "高对比度突出几何线条",
            "注意构图对称和透视",
            "寻找有趣的光影角度"
        )

        "urban-cafe" -> listOf(
            "暖色调营造温馨氛围",
            "柔光增加文艺感",
            "寻找有趣的室内元素"
        )

        "urban-museum" -> listOf(
            "低饱和度保持克制感",
            "注意展品光线",
            "遵守拍摄规定"
        )

        // ─── 默认建议 ───
        else -> listOf(
            "注意光线和构图",
            "寻找有趣的拍摄角度",
            "保持画面简洁"
        )
    }

    /**
     * 创建胶片预设实例
     */
    private fun createFilmPreset(
        id: String,
        name: String,
        series: FilmSeries,
        matchScore: Float
    ): FilmPreset {
        return FilmPreset(
            id = id,
            name = id,
            displayName = name,
            series = series,
            matchScore = matchScore,
            description = getFilmDescription(id),
            colorCharacteristics = getFilmColorCharacteristics(id),
            bestFor = getFilmBestFor(id)
        )
    }

    /**
     * 获取胶片描述
     */
    private fun getFilmDescription(id: String): String = when (id) {
        "portra" -> "柯达 Portra 400，柔美人像胶片"
        "cc" -> "经典负片风格，色彩浓郁复古"
        "nc" -> "富士经典负片，柔和自然"
        "nh" -> "浓郁色彩，强烈对比"
        "rdp3" -> "富士 Velvia 风格，风景专用"
        "800t" -> "夜景胶片，霓虹灯专用"
        "tx400" -> "黑白胶片，经典质感"
        "ccd_cool" -> "数码 CCD 冷调风格"
        "ccd_warm" -> "数码 CCD 暖调风格"
        else -> "经典胶片风格"
    }

    /**
     * 获取胶片色彩特征
     */
    private fun getFilmColorCharacteristics(id: String): String = when (id) {
        "portra" -> "柔美肤色，低对比度"
        "cc" -> "浓郁暖调，高对比度"
        "nc" -> "柔和自然，日系风格"
        "nh" -> "浓郁饱和，高对比"
        "rdp3" -> "高饱和，风景专用"
        "800t" -> "夜景专用，霓虹感"
        "tx400" -> "黑白经典，高对比"
        "ccd_cool" -> "冷色调，清透感"
        "ccd_warm" -> "暖色调，温馨感"
        else -> "经典色彩"
    }

    /**
     * 获取胶片最佳适用场景
     */
    private fun getFilmBestFor(id: String): List<String> = when (id) {
        "portra" -> listOf("人像", "逆光", "婚礼", "柔美")
        "cc" -> listOf("街拍", "人像", "风景", "建筑")
        "nc" -> listOf("人像", "日常", "旅行", "日系")
        "nh" -> listOf("风景", "建筑", "棚拍", "艺术")
        "rdp3" -> listOf("风景", "日落", "秋景", "自然")
        "800t" -> listOf("夜景", "霓虹", "城市", "星空")
        "tx400" -> listOf("黑白", "街拍", "建筑", "纪实")
        "ccd_cool" -> listOf("雪景", "天空", "海滩", "冷调")
        "ccd_warm" -> listOf("美食", "咖啡馆", "烛光", "儿童")
        else -> listOf("通用", "日常")
    }
}