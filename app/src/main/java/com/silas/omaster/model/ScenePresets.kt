package com.silas.omaster.model

/**
 * 4.2 三级场景体系（50+ 场景 × 哈苏参数映射）
 * 对齐 OPPO 大师模式参数体系
 */

/**
 * 场景预设定义
 * 包含哈苏大师参数配方和推荐胶片
 */
object ScenePresets {

    /**
     * 所有场景预设列表
     */
    val allScenes: List<SceneProfile> = listOf(
        // ==================== PORTRAIT (人像) ====================
        portraitScene("portrait-standard", "标准人像",
            "柔和自然的人像风格，适合日常拍摄",
            HasselbladParams(tone = -3, saturation = 10, contrast = -15),
            listOf("portra", "cc"),
            listOf("使用大光圈获得柔和背景", "对焦眼睛确保清晰度", "肤色曝光遵循向右曝光原则")
        ),
        portraitScene("portrait-backlit", "逆光人像",
            "侧逆光环境下的柔美人像，轮廓光效果",
            HasselbladParams(colorTemp = -10, sharpness = -15, vignette = 20),
            listOf("portra", "nh"),
            listOf("保留高光细节", "提升阴影恢复面部", "使用HDR或手动调节")
        ),
        portraitScene("portrait-studio", "棚拍人像",
            "专业影棚环境，精准控制光线",
            HasselbladParams(tone = 0, saturation = 0, contrast = 15),
            listOf("nh", "cc"),
            listOf("使用专业灯光", "精确控制曝光", "注意背景分离")
        ),
        portraitScene("portrait-bw", "黑白人像",
            "经典黑白人像，戏剧性光影",
            HasselbladParams(saturation = -30, contrast = 25, tone = -10),
            listOf("tx400"),
            listOf("关注光影对比", "简化背景元素", "突出面部轮廓")
        ),
        portraitScene("portrait-group", "合影",
            "多人合影，保持整体协调",
            HasselbladParams(saturation = 8, contrast = -10, sharpness = 12),
            listOf("cc", "nc"),
            listOf("确保所有人清晰", "使用较小光圈", "注意站位布局")
        ),
        portraitScene("portrait-child", "儿童",
            "活泼可爱的儿童人像",
            HasselbladParams(saturation = 12, contrast = -8, softLight = SoftLightMode.DREAMY),
            listOf("ccd_warm", "portra"),
            listOf("捕捉自然表情", "使用柔和光线", "保持互动感")
        ),
        portraitScene("portrait-couple", "情侣",
            "温馨浪漫的情侣人像",
            HasselbladParams(tone = 5, saturation = 8, colorTemp = 10),
            listOf("portra", "ccd_warm"),
            listOf("营造亲密氛围", "使用暖色调", "注意构图平衡")
        ),
        portraitScene("portrait-senior", "老人",
            "沉稳有质感的老人人像",
            HasselbladParams(tone = -5, contrast = 10, sharpness = 8),
            listOf("tx400", "nc"),
            listOf("突出人物气质", "使用自然光线", "关注表情细节")
        ),

        // ==================== LANDSCAPE (风景) ====================
        landscapeScene("landscape-standard", "标准风景",
            "自然通透的风景效果",
            HasselbladParams(tone = 5, saturation = 15, contrast = 12),
            listOf("rdp3", "nh"),
            listOf("使用小光圈获得全景深", "使用三脚架", "注意构图层次")
        ),
        landscapeScene("landscape-sunset", "日落",
            "温暖壮观的日落场景",
            HasselbladParams(tone = -5, saturation = 25, colorTemp = 20),
            listOf("rdp3", "portra"),
            listOf("等待最佳时刻", "使用渐变滤镜", "保留天空细节")
        ),
        landscapeScene("landscape-sky", "蓝天白云",
            "清澈明亮的蓝天白云",
            HasselbladParams(tone = 10, saturation = 10, colorTemp = -15),
            listOf("nh", "nc"),
            listOf("注意天空比例", "避免过曝", "使用偏振镜")
        ),
        landscapeScene("landscape-forest", "森林",
            "郁郁葱葱的森林场景",
            HasselbladParams(saturation = 20, sharpness = 15, colorTemp = 5),
            listOf("nh", "cc"),
            listOf("利用光线穿透", "注意层次感", "使用广角镜头")
        ),
        landscapeScene("landscape-autumn", "秋景",
            "金黄绚丽的秋季风光",
            HasselbladParams(saturation = 25, colorTemp = 15, contrast = 10),
            listOf("rdp3", "nh"),
            listOf("捕捉色彩变化", "注意光线方向", "使用中长焦")
        ),
        landscapeScene("landscape-snow", "雪景",
            "纯净洁白的雪景",
            HasselbladParams(tone = 15, saturation = -5, contrast = -10),
            listOf("ccd_cool", "nh"),
            listOf("注意曝光补偿", "避免过曝", "寻找对比元素")
        ),
        landscapeScene("landscape-beach", "海滩",
            "阳光明媚的海滩场景",
            HasselbladParams(tone = 10, saturation = 8, colorTemp = -5),
            listOf("nc", "nh"),
            listOf("注意水面反射", "使用偏振镜", "捕捉动态元素")
        ),
        landscapeScene("landscape-waterfall", "瀑布",
            "动感柔美的瀑布场景",
            HasselbladParams(saturation = 10, sharpness = 20, vignette = -10),
            listOf("nh", "rdp3"),
            listOf("使用慢门拍摄", "注意水流方向", "使用三脚架")
        ),
        landscapeScene("landscape-mountain", "山景",
            "壮阔雄伟的山景",
            HasselbladParams(tone = 8, saturation = 12, contrast = 15),
            listOf("rdp3", "nh"),
            listOf("选择最佳视角", "注意光线变化", "使用广角镜头")
        ),
        landscapeScene("landscape-lake", "湖泊",
            "宁静优美的湖泊场景",
            HasselbladParams(tone = 5, saturation = 10, contrast = 5),
            listOf("nc", "portra"),
            listOf("利用倒影效果", "注意水面平静", "选择合适时机")
        ),
        landscapeScene("landscape-desert", "沙漠",
            "广袤神秘的沙漠场景",
            HasselbladParams(tone = -5, saturation = 15, colorTemp = 15),
            listOf("rdp3", "ccd_warm"),
            listOf("注意光线方向", "利用阴影效果", "寻找视觉焦点")
        ),

        // ==================== NIGHT (夜景) ====================
        nightScene("night-city", "城市夜景",
            "繁华璀璨的城市夜景",
            HasselbladParams(tone = -15, contrast = 25, colorTemp = -5),
            listOf("800t", "tx400"),
            listOf("使用三脚架", "注意稳定", "选择合适时机")
        ),
        nightScene("night-neon", "霓虹灯",
            "绚丽多彩的霓虹灯场景",
            HasselbladParams(saturation = 20, contrast = 15, colorTemp = -10),
            listOf("800t", "ccd_cool"),
            listOf("注意色彩对比", "使用慢门", "寻找最佳角度")
        ),
        nightScene("night-starry", "星空",
            "浩瀚神秘的星空场景",
            HasselbladParams(tone = -20, contrast = 30, sharpness = 25),
            listOf("tx400", "800t"),
            listOf("远离城市光源", "使用高ISO", "长时间曝光")
        ),
        nightScene("night-candle", "烛光",
            "温暖柔和的烛光场景",
            HasselbladParams(tone = -10, saturation = 5, colorTemp = 15),
            listOf("ccd_warm", "portra"),
            listOf("注意曝光控制", "保持温暖色调", "避免过曝")
        ),
        nightScene("night-fireworks", "烟花",
            "绚烂绽放的烟花场景",
            HasselbladParams(saturation = 15, contrast = 20, sharpness = 15),
            listOf("rdp3", "800t"),
            listOf("使用慢门拍摄", "预判烟花位置", "注意构图")
        ),
        nightScene("night-moon", "月光",
            "静谧神秘的月光场景",
            HasselbladParams(tone = -15, saturation = -5, contrast = 20),
            listOf("tx400", "800t"),
            listOf("注意曝光控制", "使用长焦镜头", "捕捉月亮细节")
        ),
        nightScene("night-bridge", "桥梁夜景",
            "灯火通明的桥梁夜景",
            HasselbladParams(tone = -10, contrast = 20, vignette = 15),
            listOf("800t", "tx400"),
            listOf("选择合适角度", "注意水面倒影", "使用三脚架")
        ),

        // ==================== FOOD (美食) ====================
        foodScene("food-restaurant", "餐厅美食",
            "精致诱人的餐厅美食",
            HasselbladParams(tone = -5, saturation = 15, colorTemp = 10),
            listOf("ccd_warm", "cc"),
            listOf("注意光线方向", "使用自然光", "关注细节质感")
        ),
        foodScene("food-dessert", "甜点",
            "精致可爱的甜点",
            HasselbladParams(tone = 5, saturation = 20, colorTemp = 5),
            listOf("ccd_warm", "nh"),
            listOf("突出质感细节", "使用柔和光线", "注意色彩搭配")
        ),
        foodScene("food-drinks", "饮品",
            "清爽诱人的饮品",
            HasselbladParams(saturation = 10, contrast = 12, colorTemp = 5),
            listOf("nc", "cc"),
            listOf("注意透明感", "使用逆光", "关注色彩表现")
        ),
        foodScene("food-coffee", "咖啡",
            "温暖醇香的咖啡场景",
            HasselbladParams(tone = -5, saturation = 8, colorTemp = 10),
            listOf("ccd_warm", "nc"),
            listOf("营造氛围感", "注意蒸汽效果", "使用暖色调")
        ),
        foodScene("food-bbq", "烧烤",
            "热气腾腾的烧烤场景",
            HasselbladParams(saturation = 15, contrast = 10, colorTemp = 8),
            listOf("ccd_warm", "cc"),
            listOf("捕捉热气效果", "注意色彩饱和", "突出质感")
        ),

        // ==================== URBAN (城市) ====================
        urbanScene("urban-street", "街拍",
            "动感活力的街头场景",
            HasselbladParams(tone = -5, saturation = 5, contrast = 18),
            listOf("tx400", "cc"),
            listOf("捕捉瞬间动态", "注意构图节奏", "使用快速快门")
        ),
        urbanScene("urban-architecture", "建筑",
            "结构感强烈的建筑场景",
            HasselbladParams(tone = 5, saturation = 5, contrast = 25),
            listOf("tx400", "nh"),
            listOf("注意线条构图", "选择合适角度", "利用光影效果")
        ),
        urbanScene("urban-cafe", "咖啡馆",
            "温馨舒适的咖啡馆场景",
            HasselbladParams(tone = -10, saturation = 8, colorTemp = 15),
            listOf("ccd_warm", "nh"),
            listOf("营造氛围感", "注意光线柔和", "捕捉细节质感")
        ),
        urbanScene("urban-museum", "博物馆",
            "庄严肃穆的博物馆场景",
            HasselbladParams(tone = -5, saturation = 0, contrast = 15),
            listOf("nh", "tx400"),
            listOf("注意光线控制", "尊重场所规则", "关注展品细节")
        ),
        urbanScene("urban-market", "市场",
            "热闹繁华的市场场景",
            HasselbladParams(saturation = 12, contrast = 10, tone = 0),
            listOf("cc", "nc"),
            listOf("捕捉生活气息", "注意色彩丰富", "关注人物互动")
        ),
        urbanScene("urban-station", "车站",
            "繁忙有序的车站场景",
            HasselbladParams(tone = -8, contrast = 15, saturation = 5),
            listOf("tx400", "800t"),
            listOf("捕捉人流动态", "注意光线变化", "使用慢门效果")
        ),
        urbanScene("urban-park", "公园",
            "宁静优美的城市公园",
            HasselbladParams(tone = 5, saturation = 10, contrast = 8),
            listOf("nc", "portra"),
            listOf("利用自然光线", "注意季节变化", "捕捉休闲氛围")
        ),

        // ==================== STILL_LIFE (静物) ====================
        stillLifeScene("still-flower", "花卉",
            "娇艳动人的花卉场景",
            HasselbladParams(saturation = 15, tone = 5, sharpness = 10),
            listOf("rdp3", "nc"),
            listOf("注意背景简洁", "使用柔和光线", "关注细节质感")
        ),
        stillLifeScene("still-product", "产品",
            "精致专业的产品展示",
            HasselbladParams(tone = 0, saturation = 5, contrast = 10),
            listOf("nc", "cc"),
            listOf("注意光线均匀", "突出产品特点", "控制背景干净")
        ),
        stillLifeScene("still-book", "书籍",
            "文艺气息的书籍场景",
            HasselbladParams(tone = -5, saturation = 5, colorTemp = 5),
            listOf("nc", "ccd_warm"),
            listOf("营造阅读氛围", "注意光线柔和", "关注细节质感")
        ),
        stillLifeScene("still-art", "艺术品",
            "精致优雅的艺术品展示",
            HasselbladParams(tone = 0, saturation = 8, contrast = 12),
            listOf("nh", "nc"),
            listOf("注意光线控制", "突出艺术特点", "控制背景简洁")
        ),

        // ==================== MACRO (微距) ====================
        macroScene("macro-insect", "昆虫",
            "细致入微的昆虫细节",
            HasselbladParams(sharpness = 25, contrast = 15, saturation = 10),
            listOf("rdp3", "nh"),
            listOf("使用微距镜头", "注意稳定拍摄", "捕捉细节质感")
        ),
        macroScene("macro-water", "水滴",
            "晶莹剔透的水滴效果",
            HasselbladParams(tone = 10, sharpness = 20, contrast = 10),
            listOf("rdp3", "nc"),
            listOf("注意光线折射", "使用微距镜头", "捕捉瞬间效果")
        ),
        macroScene("macro-texture", "纹理",
            "细腻丰富的纹理细节",
            HasselbladParams(sharpness = 25, contrast = 20, tone = 5),
            listOf("tx400", "nh"),
            listOf("突出纹理质感", "注意光线方向", "使用微距镜头")
        ),

        // ==================== EVENT (活动) ====================
        eventScene("event-wedding", "婚礼",
            "温馨浪漫的婚礼场景",
            HasselbladParams(tone = 5, saturation = 10, colorTemp = 10),
            listOf("portra", "ccd_warm"),
            listOf("捕捉情感瞬间", "注意光线变化", "关注人物互动")
        ),
        eventScene("event-party", "派对",
            "欢乐热闹的派对场景",
            HasselbladParams(saturation = 15, contrast = 10, tone = 0),
            listOf("cc", "ccd_warm"),
            listOf("捕捉欢乐氛围", "注意光线变化", "关注人物表情")
        ),
        eventScene("event-concert", "演唱会",
            "激情澎湃的演唱会场景",
            HasselbladParams(tone = -10, contrast = 20, saturation = 15),
            listOf("800t", "tx400"),
            listOf("注意舞台光线", "捕捉表演瞬间", "使用快速快门")
        ),
        eventScene("event-sports", "运动",
            "动感活力的运动场景",
            HasselbladParams(contrast = 15, sharpness = 20, saturation = 10),
            listOf("tx400", "cc"),
            listOf("捕捉运动瞬间", "使用快速快门", "注意构图动态")
        ),
        eventScene("event-travel", "旅行",
            "记录旅途中的精彩瞬间",
            HasselbladParams(tone = 5, saturation = 12, contrast = 8),
            listOf("rdp3", "nh"),
            listOf("把握黄金时刻", "注意环境故事性", "保持轻便灵活")
        ),
        eventScene("event-graduation", "毕业",
            "青春难忘的毕业场景",
            HasselbladParams(tone = 3, saturation = 10, colorTemp = 5),
            listOf("portra", "cc"),
            listOf("捕捉真挚笑容", "注意人群构图", "记录难忘时刻")
        )
    )

    /**
     * 根据类别获取场景列表
     */
    fun getScenesByCategory(category: SceneCategory): List<SceneProfile> {
        return allScenes.filter { it.category == category }
    }

    /**
     * 根据ID获取场景
     */
    fun getSceneById(id: String): SceneProfile? {
        return allScenes.find { it.id == id }
    }

    // ==================== 辅助构建方法 ====================

    private fun portraitScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.PORTRAIT,
        description = description,
        color = SceneCategory.PORTRAIT.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun landscapeScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.LANDSCAPE,
        description = description,
        color = SceneCategory.LANDSCAPE.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun nightScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.NIGHT,
        description = description,
        color = SceneCategory.NIGHT.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun foodScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.FOOD,
        description = description,
        color = SceneCategory.FOOD.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun urbanScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.URBAN,
        description = description,
        color = SceneCategory.URBAN.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun stillLifeScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.STILL_LIFE,
        description = description,
        color = SceneCategory.STILL_LIFE.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun macroScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.MACRO,
        description = description,
        color = SceneCategory.MACRO.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    private fun eventScene(
        id: String,
        name: String,
        description: String,
        params: HasselbladParams,
        films: List<String>,
        tips: List<String>
    ) = SceneProfile(
        id = id,
        name = name,
        category = SceneCategory.EVENT,
        description = description,
        color = SceneCategory.EVENT.color,
        confidence = 0f,
        hasselbladParams = params,
        recommendedFilm = films.map { createFilmPreset(it) },
        masterTips = tips
    )

    /**
     * 创建胶片预设
     */
    private fun createFilmPreset(id: String): FilmPreset {
        return when (id) {
            "portra" -> FilmPreset(
                id = "portra",
                name = "Portra 400",
                series = FilmSeries.EMOTION,
                matchScore = 0.9f,
                description = "柔和肤色，自然色彩"
            )
            "cc" -> FilmPreset(
                id = "cc",
                name = "CC 经典负片",
                series = FilmSeries.CLASSIC,
                matchScore = 0.85f,
                description = "经典胶片质感，复古风格"
            )
            "nc" -> FilmPreset(
                id = "nc",
                name = "NC 自然",
                series = FilmSeries.CLASSIC,
                matchScore = 0.8f,
                description = "自然柔和，日常记录"
            )
            "nh" -> FilmPreset(
                id = "nh",
                name = "NH 浓郁",
                series = FilmSeries.CLASSIC,
                matchScore = 0.85f,
                description = "浓郁色彩，戏剧效果"
            )
            "rdp3" -> FilmPreset(
                id = "rdp3",
                name = "RDP3",
                series = FilmSeries.EMOTION,
                matchScore = 0.9f,
                description = "极高饱和，反转片质感"
            )
            "800t" -> FilmPreset(
                id = "800t",
                name = "800T",
                series = FilmSeries.STRUCTURE,
                matchScore = 0.85f,
                description = "夜景专用，电影感色调"
            )
            "tx400" -> FilmPreset(
                id = "tx400",
                name = "TX400",
                series = FilmSeries.STRUCTURE,
                matchScore = 0.9f,
                description = "经典黑白，颗粒粗犷"
            )
            "ccd_cool" -> FilmPreset(
                id = "ccd_cool",
                name = "冷 CCD",
                series = FilmSeries.DIGITAL,
                matchScore = 0.75f,
                description = "冷色调数字记忆"
            )
            "ccd_warm" -> FilmPreset(
                id = "ccd_warm",
                name = "暖 CCD",
                series = FilmSeries.DIGITAL,
                matchScore = 0.8f,
                description = "暖色调数字记忆"
            )
            else -> FilmPreset(
                id = id,
                name = id,
                series = FilmSeries.CLASSIC,
                matchScore = 0.5f,
                description = ""
            )
        }
    }
}

/**
 * 9款原生胶片预设完整定义
 */
object FilmPresets {
    val allFilms: List<FilmPreset> = listOf(
        // 原生经典系列
        FilmPreset("cc", "CC 经典负片", FilmSeries.CLASSIC, 0.85f, "经典胶片质感，复古风格"),
        FilmPreset("nc", "NC 自然", FilmSeries.CLASSIC, 0.8f, "自然柔和，日常记录"),
        FilmPreset("nh", "NH 浓郁", FilmSeries.CLASSIC, 0.85f, "浓郁色彩，戏剧效果"),
        
        // 情绪与表达系列
        FilmPreset("portra", "Portra 400", FilmSeries.EMOTION, 0.9f, "柔和肤色，自然色彩"),
        FilmPreset("rdp3", "RDP3", FilmSeries.EMOTION, 0.9f, "极高饱和，反转片质感"),
        
        // 结构与时间系列
        FilmPreset("800t", "800T", FilmSeries.STRUCTURE, 0.85f, "夜景专用，电影感色调"),
        FilmPreset("tx400", "TX400", FilmSeries.STRUCTURE, 0.9f, "经典黑白，颗粒粗犷"),
        
        // 数字记忆系列
        FilmPreset("ccd_cool", "冷 CCD", FilmSeries.DIGITAL, 0.75f, "冷色调数字记忆"),
        FilmPreset("ccd_warm", "暖 CCD", FilmSeries.DIGITAL, 0.8f, "暖色调数字记忆")
    )

    fun getFilmById(id: String): FilmPreset? = allFilms.find { it.id == id }
    fun getFilmsBySeries(series: FilmSeries): List<FilmPreset> = allFilms.filter { it.series == series }
    
    /**
     * 获取所有场景列表
     */
    fun getAllScenes(): List<SceneProfile> = ScenePresets.allScenes
    
    /**
     * 根据场景ID列表获取场景
     */
    fun getScenesByIds(ids: List<String>): List<SceneProfile> {
        return ids.mapNotNull { ScenePresets.getSceneById(it) }
    }
}