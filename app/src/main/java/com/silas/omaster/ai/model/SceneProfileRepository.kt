package com.silas.omaster.ai.model

/**
 * 场景配置仓库
 * 50+ 场景 × 哈苏参数映射表
 * 三级场景体系（大类→细分→精细）
 */
object SceneProfileRepository {

    // 哈苏橙主题色
    private const val HASSELBLAD_ORANGE = 0xFFFF6B35L

    /**
     * 所有场景配置列表
     */
    val allProfiles: List<SceneProfile> by lazy {
        buildProfiles()
    }

    /**
     * 按大类分组
     */
    val profilesByCategory: Map<SceneCategory, List<SceneProfile>> by lazy {
        allProfiles.groupBy { it.category }
    }

    /**
     * 根据 ID 获取场景配置
     */
    fun getProfileById(id: String): SceneProfile? {
        return allProfiles.find { it.id == id }
    }

    /**
     * 根据大类获取场景列表
     */
    fun getProfilesByCategory(category: SceneCategory): List<SceneProfile> {
        return profilesByCategory[category] ?: emptyList()
    }

    /**
     * 搜索场景（按名称或标签）
     */
    fun searchProfiles(query: String): List<SceneProfile> {
        val lowerQuery = query.lowercase()
        return allProfiles.filter { profile ->
            profile.name.contains(query, ignoreCase = true) ||
            profile.subCategory.contains(query, ignoreCase = true) ||
            profile.tags.any { it.contains(lowerQuery, ignoreCase = true) }
        }
    }

    /**
     * 构建所有场景配置
     */
    private fun buildProfiles(): List<SceneProfile> {
        return mutableListOf<SceneProfile>().apply {
            // 人像场景 (PORTRAIT)
            addAll(buildPortraitProfiles())

            // 风景场景 (LANDSCAPE)
            addAll(buildLandscapeProfiles())

            // 夜景场景 (NIGHT)
            addAll(buildNightProfiles())

            // 美食场景 (FOOD)
            addAll(buildFoodProfiles())

            // 城市场景 (URBAN)
            addAll(buildUrbanProfiles())

            // 宠物场景 (PET)
            addAll(buildPetProfiles())

            // 微距场景 (MACRO)
            addAll(buildMacroProfiles())

            // 特殊场景 (SPECIAL)
            addAll(buildSpecialProfiles())
        }
    }

    // ==================== 人像场景 ====================
    private fun buildPortraitProfiles(): List<SceneProfile> {
        return listOf(
            // 标准人像
            SceneProfile(
                id = "portrait-standard",
                name = "标准人像",
                category = SceneCategory.PORTRAIT,
                subCategory = "标准人像",
                description = "自然光环境下的标准人像拍摄，追求真实自然的肤色表现",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -3,
                    saturation = 10,
                    contrast = -15,
                    colorTemp = 0,
                    sharpness = 0,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.70f)
                ),
                masterTips = listOf(
                    "使用柔光模式营造自然肤色",
                    "降低对比度保持皮肤质感",
                    "注意眼神光和面部轮廓"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/1.8",
                    whiteBalance = "自动",
                    focusMode = "人脸优先"
                ),
                tags = listOf("人像", "自然光", "柔美"),
                bestTime = "上午10点-下午4点",
                environmentTips = "自然光或柔和人工光源，避免直射阳光"
            ),

            // 逆光人像
            SceneProfile(
                id = "portrait-backlit",
                name = "逆光人像",
                category = SceneCategory.PORTRAIT,
                subCategory = "逆光人像",
                description = "侧逆光环境下的柔美人像，营造梦幻光晕效果",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 5,
                    contrast = -10,
                    colorTemp = -10,
                    sharpness = -15,
                    vignette = 20,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.DREAMY
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "利用逆光创造光晕效果",
                    "梦幻柔光增强氛围感",
                    "注意面部曝光补偿"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/2.0",
                    exposureCompensation = "+0.7",
                    whiteBalance = "自动"
                ),
                tags = listOf("人像", "逆光", "梦幻", "光晕"),
                bestTime = "日落前1-2小时",
                environmentTips = "侧逆光或全逆光环境，寻找有遮挡的背景"
            ),

            // 棚拍人像
            SceneProfile(
                id = "portrait-studio",
                name = "棚拍人像",
                category = SceneCategory.PORTRAIT,
                subCategory = "棚拍人像",
                description = "专业摄影棚环境，追求高对比度和浓郁色彩",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 0,
                    contrast = 15,
                    colorTemp = 0,
                    sharpness = 10,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.70f)
                ),
                masterTips = listOf(
                    "高对比度突出轮廓",
                    "注意光影造型",
                    "控制背景简洁"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/200",
                    aperture = "f/5.6",
                    whiteBalance = "5500K"
                ),
                tags = listOf("人像", "棚拍", "专业", "高对比"),
                environmentTips = "专业摄影棚，可控光源环境"
            ),

            // 黑白人像
            SceneProfile(
                id = "portrait-bw",
                name = "黑白人像",
                category = SceneCategory.PORTRAIT,
                subCategory = "黑白人像",
                description = "经典黑白人像，追求光影质感和艺术表达",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -10,
                    saturation = -30,
                    contrast = 25,
                    colorTemp = 0,
                    sharpness = 15,
                    vignette = 15,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.95f)
                ),
                masterTips = listOf(
                    "黑白摄影注重光影对比",
                    "寻找有纹理和质感的场景",
                    "注意构图简洁有力"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("人像", "黑白", "艺术", "光影"),
                bestTime = "强烈光影对比时段",
                environmentTips = "强烈光影对比场景，如阳光直射或聚光灯"
            ),

            // 合影
            SceneProfile(
                id = "portrait-group",
                name = "合影",
                category = SceneCategory.PORTRAIT,
                subCategory = "合影",
                description = "多人合影拍摄，追求整体协调和清晰度",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 8,
                    contrast = -10,
                    colorTemp = 0,
                    sharpness = 12,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.75f),
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.70f)
                ),
                masterTips = listOf(
                    "确保所有人清晰可见",
                    "注意站位和表情",
                    "使用小光圈保证景深"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/4.0",
                    focusMode = "连续对焦"
                ),
                tags = listOf("人像", "合影", "多人"),
                environmentTips = "光线均匀的环境，避免强光阴影"
            ),

            // 儿童
            SceneProfile(
                id = "portrait-children",
                name = "儿童",
                category = SceneCategory.PORTRAIT,
                subCategory = "儿童",
                description = "儿童摄影，追求温馨柔和的色彩表现",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 12,
                    contrast = -8,
                    colorTemp = 10,
                    sharpness = 0,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.DREAMY
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "梦幻柔光营造童话氛围",
                    "捕捉自然表情和动作",
                    "注意安全距离"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "1/250",
                    aperture = "f/2.8",
                    focusMode = "连续对焦"
                ),
                tags = listOf("人像", "儿童", "温馨", "梦幻"),
                bestTime = "清晨或阴天散射光",
                environmentTips = "柔和光线环境，色彩丰富的场景"
            )
        )
    }

    // ==================== 风景场景 ====================
    private fun buildLandscapeProfiles(): List<SceneProfile> {
        return listOf(
            // 标准风景
            SceneProfile(
                id = "landscape-standard",
                name = "标准风景",
                category = SceneCategory.LANDSCAPE,
                subCategory = "标准风景",
                description = "自然风景拍摄，追求色彩饱满和清晰度",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 15,
                    contrast = 12,
                    colorTemp = 0,
                    sharpness = 15,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "使用HDR增强动态范围",
                    "注意构图层次",
                    "寻找前景增加纵深感"
                ),
                cameraParams = CameraParams(
                    iso = "50",
                    shutterSpeed = "1/60",
                    aperture = "f/8",
                    whiteBalance = "日光",
                    hdrEnabled = true
                ),
                tags = listOf("风景", "自然", "HDR"),
                bestTime = "日出后或日落前",
                environmentTips = "光线充足的户外环境"
            ),

            // 日落
            SceneProfile(
                id = "landscape-sunset",
                name = "日落",
                category = SceneCategory.LANDSCAPE,
                subCategory = "日落",
                description = "日落时分，追求暖色调和浓郁色彩",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -5,
                    saturation = 25,
                    contrast = 10,
                    colorTemp = 20,
                    sharpness = 10,
                    vignette = 10,
                    cyanMagenta = 5,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "增强暖色调表现",
                    "寻找有层次的天空",
                    "注意曝光控制"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/5.6",
                    exposureCompensation = "-0.3",
                    whiteBalance = "5600K"
                ),
                tags = listOf("风景", "日落", "暖调", "天空"),
                bestTime = "日落前30分钟",
                environmentTips = "开阔视野，有云层的天空更佳"
            ),

            // 蓝天白云
            SceneProfile(
                id = "landscape-blue-sky",
                name = "蓝天白云",
                category = SceneCategory.LANDSCAPE,
                subCategory = "蓝天白云",
                description = "蓝天白云场景，追求通透清新",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 10,
                    saturation = 10,
                    contrast = 8,
                    colorTemp = -15,
                    sharpness = 12,
                    vignette = 0,
                    cyanMagenta = -5,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "冷色调增强蓝色表现",
                    "注意天空与地面比例",
                    "寻找有趣的云层形态"
                ),
                cameraParams = CameraParams(
                    iso = "50",
                    shutterSpeed = "1/125",
                    aperture = "f/8",
                    whiteBalance = "日光"
                ),
                tags = listOf("风景", "天空", "蓝天", "清新"),
                bestTime = "晴朗天气",
                environmentTips = "晴朗天气，视野开阔"
            ),

            // 森林
            SceneProfile(
                id = "landscape-forest",
                name = "森林",
                category = SceneCategory.LANDSCAPE,
                subCategory = "森林",
                description = "森林场景，追求绿色自然通透",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 20,
                    contrast = 10,
                    colorTemp = 5,
                    sharpness = 15,
                    vignette = 5,
                    cyanMagenta = -8,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.70f)
                ),
                masterTips = listOf(
                    "增强绿色表现力",
                    "利用光线穿透树叶",
                    "寻找有趣的树木形态"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/60",
                    aperture = "f/5.6",
                    whiteBalance = "日光"
                ),
                tags = listOf("风景", "森林", "绿色", "自然"),
                bestTime = "上午或阴天",
                environmentTips = "户外自然光，森林、草地、植物丰富的场景"
            ),

            // 秋景
            SceneProfile(
                id = "landscape-autumn",
                name = "秋景",
                category = SceneCategory.LANDSCAPE,
                subCategory = "秋景",
                description = "秋景拍摄，追求金黄暖色调",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 25,
                    contrast = 10,
                    colorTemp = 15,
                    sharpness = 12,
                    vignette = 5,
                    cyanMagenta = 3,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "增强暖色调表现",
                    "寻找色彩丰富的秋叶",
                    "注意光影层次"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/5.6",
                    whiteBalance = "5600K"
                ),
                tags = listOf("风景", "秋景", "金黄", "暖调"),
                bestTime = "秋季晴天",
                environmentTips = "秋季户外，色彩丰富的落叶场景"
            ),

            // 雪景
            SceneProfile(
                id = "landscape-snow",
                name = "雪景",
                category = SceneCategory.LANDSCAPE,
                subCategory = "雪景",
                description = "雪景拍摄，追求纯净冷色调",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 15,
                    saturation = -5,
                    contrast = -10,
                    colorTemp = -10,
                    sharpness = 8,
                    vignette = 0,
                    cyanMagenta = -5,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_cool")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.70f)
                ),
                masterTips = listOf(
                    "冷色调增强雪景纯净感",
                    "注意曝光补偿防止过曝",
                    "寻找有趣的雪景形态"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/8",
                    exposureCompensation = "+0.7",
                    whiteBalance = "5200K"
                ),
                tags = listOf("风景", "雪景", "纯净", "冷调"),
                bestTime = "雪天或阴天",
                environmentTips = "雪天、阴天或低色温场景"
            ),

            // 海滩
            SceneProfile(
                id = "landscape-beach",
                name = "海滩",
                category = SceneCategory.LANDSCAPE,
                subCategory = "海滩",
                description = "海滩场景，追求清新通透",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 10,
                    saturation = 8,
                    contrast = 5,
                    colorTemp = -5,
                    sharpness = 10,
                    vignette = 0,
                    cyanMagenta = -3,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "冷色调增强海水表现",
                    "注意天空与海面比例",
                    "寻找有趣的海岸线"
                ),
                cameraParams = CameraParams(
                    iso = "50",
                    shutterSpeed = "1/125",
                    aperture = "f/8",
                    whiteBalance = "日光"
                ),
                tags = listOf("风景", "海滩", "海边", "清新"),
                bestTime = "晴朗天气",
                environmentTips = "晴朗天气或明亮的度假场景"
            ),

            // 瀑布
            SceneProfile(
                id = "landscape-waterfall",
                name = "瀑布",
                category = SceneCategory.LANDSCAPE,
                subCategory = "瀑布",
                description = "瀑布场景，追求动态水流质感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 10,
                    contrast = 8,
                    colorTemp = 0,
                    sharpness = 20,
                    vignette = -10,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "高锐度增强水流质感",
                    "可尝试慢门拍摄",
                    "注意安全距离"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/60",
                    aperture = "f/8",
                    stabilizationEnabled = true
                ),
                tags = listOf("风景", "瀑布", "水流", "动态"),
                bestTime = "阴天或散射光",
                environmentTips = "瀑布环境，注意安全"
            )
        )
    }

    // ==================== 夜景场景 ====================
    private fun buildNightProfiles(): List<SceneProfile> {
        return listOf(
            // 城市夜景
            SceneProfile(
                id = "night-city",
                name = "城市夜景",
                category = SceneCategory.NIGHT,
                subCategory = "城市夜景",
                description = "城市夜景拍摄，追求光影层次和氛围感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -15,
                    saturation = 5,
                    contrast = 25,
                    colorTemp = -5,
                    sharpness = 10,
                    vignette = 10,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("800t")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "高对比度增强光影层次",
                    "注意曝光控制",
                    "寻找有灯光的建筑"
                ),
                cameraParams = CameraParams(
                    iso = "1600",
                    shutterSpeed = "1/15",
                    aperture = "f/1.6",
                    whiteBalance = "自动",
                    stabilizationEnabled = true
                ),
                tags = listOf("夜景", "城市", "灯光", "氛围"),
                bestTime = "日落后蓝调时刻",
                environmentTips = "城市夜景，灯光璀璨的场景"
            ),

            // 霓虹灯
            SceneProfile(
                id = "night-neon",
                name = "霓虹灯",
                category = SceneCategory.NIGHT,
                subCategory = "霓虹灯",
                description = "霓虹灯场景，追求色彩饱和和梦幻感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 20,
                    contrast = 15,
                    colorTemp = -10,
                    sharpness = 10,
                    vignette = 5,
                    cyanMagenta = 3,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("800t")!!.copy(matchScore = 0.95f),
                    FilmPreset.fromId("ccd_cool")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "增强色彩饱和度",
                    "柔光营造梦幻感",
                    "寻找有霓虹灯招牌的场景"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "Auto",
                    aperture = "f/1.8",
                    exposureCompensation = "-0.3",
                    whiteBalance = "自动"
                ),
                tags = listOf("夜景", "霓虹", "色彩", "梦幻"),
                bestTime = "夜晚繁华街道",
                environmentTips = "夜晚城市、霓虹灯招牌、繁华街道"
            ),

            // 星空
            SceneProfile(
                id = "night-starry",
                name = "星空",
                category = SceneCategory.NIGHT,
                subCategory = "星空",
                description = "星空拍摄，追求深邃神秘感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -20,
                    saturation = 0,
                    contrast = 30,
                    colorTemp = -5,
                    sharpness = 25,
                    vignette = 15,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("800t")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高对比度增强星空层次",
                    "使用长曝光",
                    "远离城市光污染"
                ),
                cameraParams = CameraParams(
                    iso = "3200",
                    shutterSpeed = "30s",
                    aperture = "f/2.8",
                    whiteBalance = "自动",
                    stabilizationEnabled = true
                ),
                tags = listOf("夜景", "星空", "深邃", "神秘"),
                bestTime = "晴朗夜晚",
                environmentTips = "远离城市光污染的开阔地带"
            ),

            // 烛光
            SceneProfile(
                id = "night-candlelight",
                name = "烛光",
                category = SceneCategory.NIGHT,
                subCategory = "烛光",
                description = "烛光环境，追求温馨暖色调",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -10,
                    saturation = 5,
                    contrast = 5,
                    colorTemp = 15,
                    sharpness = 0,
                    vignette = 10,
                    cyanMagenta = -5,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "暖色调增强温馨感",
                    "柔光营造氛围",
                    "注意曝光控制"
                ),
                cameraParams = CameraParams(
                    iso = "800",
                    shutterSpeed = "1/30",
                    aperture = "f/1.8",
                    whiteBalance = "3200K"
                ),
                tags = listOf("夜景", "烛光", "温馨", "暖调"),
                environmentTips = "烛光环境，温馨室内"
            ),

            // 烟花
            SceneProfile(
                id = "night-fireworks",
                name = "烟花",
                category = SceneCategory.NIGHT,
                subCategory = "烟花",
                description = "烟花拍摄，追求色彩鲜艳和动态感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 15,
                    contrast = 20,
                    colorTemp = 0,
                    sharpness = 15,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("800t")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高对比度增强烟花层次",
                    "使用长曝光捕捉轨迹",
                    "寻找开阔视野"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "2s",
                    aperture = "f/8",
                    stabilizationEnabled = true
                ),
                tags = listOf("夜景", "烟花", "色彩", "动态"),
                bestTime = "节日烟花表演",
                environmentTips = "开阔视野，远离人群"
            )
        )
    }

    // ==================== 美食场景 ====================
    private fun buildFoodProfiles(): List<SceneProfile> {
        return listOf(
            // 餐厅美食
            SceneProfile(
                id = "food-restaurant",
                name = "餐厅美食",
                category = SceneCategory.FOOD,
                subCategory = "餐厅美食",
                description = "餐厅美食拍摄，追求暖色调和质感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -5,
                    saturation = 15,
                    contrast = 10,
                    colorTemp = 10,
                    sharpness = 12,
                    vignette = 5,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "暖色调增强食欲感",
                    "注意食物摆放角度",
                    "寻找最佳光线位置"
                ),
                cameraParams = CameraParams(
                    iso = "200",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "暖色调"
                ),
                tags = listOf("美食", "餐厅", "暖调", "质感"),
                environmentTips = "餐厅、厨房、美食拍摄场景"
            ),

            // 甜点
            SceneProfile(
                id = "food-dessert",
                name = "甜点",
                category = SceneCategory.FOOD,
                subCategory = "甜点",
                description = "甜点拍摄，追求梦幻温馨感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 20,
                    contrast = 5,
                    colorTemp = 5,
                    sharpness = 8,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.DREAMY
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "梦幻柔光营造温馨感",
                    "注意甜点摆放角度",
                    "寻找简洁背景"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "暖色调"
                ),
                tags = listOf("美食", "甜点", "梦幻", "温馨"),
                environmentTips = "温馨室内，简洁背景"
            ),

            // 饮品
            SceneProfile(
                id = "food-drink",
                name = "饮品",
                category = SceneCategory.FOOD,
                subCategory = "饮品",
                description = "饮品拍摄，追求通透质感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 10,
                    contrast = 12,
                    colorTemp = 5,
                    sharpness = 10,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "注意饮品透明度表现",
                    "寻找有趣的光线角度",
                    "注意冰块和气泡"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("美食", "饮品", "通透", "质感"),
                environmentTips = "光线充足的室内或户外"
            )
        )
    }

    // ==================== 城市场景 ====================
    private fun buildUrbanProfiles(): List<SceneProfile> {
        return listOf(
            // 街拍
            SceneProfile(
                id = "urban-street",
                name = "街拍",
                category = SceneCategory.URBAN,
                subCategory = "街拍",
                description = "街头纪实拍摄，追求真实质感和故事性",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -5,
                    saturation = 5,
                    contrast = 18,
                    colorTemp = 0,
                    sharpness = 15,
                    vignette = 5,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高对比度增强街头质感",
                    "捕捉真实生活瞬间",
                    "注意构图简洁"
                ),
                cameraParams = CameraParams(
                    iso = "400",
                    shutterSpeed = "1/250",
                    aperture = "f/5.6",
                    whiteBalance = "自动",
                    focusMode = "连续对焦"
                ),
                tags = listOf("城市", "街拍", "纪实", "质感"),
                bestTime = "日间户外",
                environmentTips = "自然光或柔和人工光源"
            ),

            // 建筑
            SceneProfile(
                id = "urban-architecture",
                name = "建筑",
                category = SceneCategory.URBAN,
                subCategory = "建筑",
                description = "建筑摄影，追求几何线条和光影质感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 5,
                    contrast = 25,
                    colorTemp = 0,
                    sharpness = 20,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高对比度突出几何线条",
                    "注意构图对称和透视",
                    "寻找有趣的光影角度"
                ),
                cameraParams = CameraParams(
                    iso = "50",
                    shutterSpeed = "1/125",
                    aperture = "f/8",
                    whiteBalance = "日光"
                ),
                tags = listOf("城市", "建筑", "几何", "线条"),
                bestTime = "晴朗天气",
                environmentTips = "强烈光影对比场景"
            ),

            // 咖啡馆
            SceneProfile(
                id = "urban-cafe",
                name = "咖啡馆",
                category = SceneCategory.URBAN,
                subCategory = "咖啡馆",
                description = "咖啡馆场景，追求温馨文艺感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -10,
                    saturation = 8,
                    contrast = 5,
                    colorTemp = 15,
                    sharpness = 5,
                    vignette = 5,
                    cyanMagenta = -3,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "暖色调营造温馨氛围",
                    "柔光增加文艺感",
                    "寻找有趣的室内元素"
                ),
                cameraParams = CameraParams(
                    iso = "200",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "暖色调"
                ),
                tags = listOf("城市", "咖啡馆", "温馨", "文艺"),
                environmentTips = "温馨室内，柔和光线"
            ),

            // 博物馆
            SceneProfile(
                id = "urban-museum",
                name = "博物馆",
                category = SceneCategory.URBAN,
                subCategory = "博物馆",
                description = "博物馆场景，追求冷静克制感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -5,
                    saturation = 0,
                    contrast = 15,
                    colorTemp = 0,
                    sharpness = 10,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "低饱和度保持克制感",
                    "注意展品光线",
                    "遵守拍摄规定"
                ),
                cameraParams = CameraParams(
                    iso = "400",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("城市", "博物馆", "冷静", "克制"),
                environmentTips = "室内展厅，注意光线"
            )
        )
    }

    // ==================== 宠物场景 ====================
    private fun buildPetProfiles(): List<SceneProfile> {
        return listOf(
            // 宠物标准
            SceneProfile(
                id = "pet-standard",
                name = "宠物",
                category = SceneCategory.PET,
                subCategory = "宠物",
                description = "宠物拍摄，追求柔美质感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 12,
                    contrast = 5,
                    colorTemp = 5,
                    sharpness = 10,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "柔光营造温馨感",
                    "捕捉自然表情和动作",
                    "注意宠物安全"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "1/250",
                    aperture = "f/2.8",
                    focusMode = "连续对焦"
                ),
                tags = listOf("宠物", "温馨", "柔美"),
                bestTime = "日间户外或室内",
                environmentTips = "光线充足的环境"
            ),

            // 宠物户外
            SceneProfile(
                id = "pet-outdoor",
                name = "宠物户外",
                category = SceneCategory.PET,
                subCategory = "宠物户外",
                description = "户外宠物拍摄，追求自然活力",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 10,
                    saturation = 15,
                    contrast = 8,
                    colorTemp = 0,
                    sharpness = 15,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "高锐度捕捉动态",
                    "注意宠物运动轨迹",
                    "寻找有趣的户外场景"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "1/500",
                    aperture = "f/2.8",
                    focusMode = "连续对焦"
                ),
                tags = listOf("宠物", "户外", "活力", "动态"),
                bestTime = "晴朗天气",
                environmentTips = "户外开阔环境"
            )
        )
    }

    // ==================== 微距场景 ====================
    private fun buildMacroProfiles(): List<SceneProfile> {
        return listOf(
            // 花卉微距
            SceneProfile(
                id = "macro-flower",
                name = "花卉微距",
                category = SceneCategory.MACRO,
                subCategory = "花卉微距",
                description = "花卉微距拍摄，追求色彩鲜艳和细节",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 18,
                    contrast = 10,
                    colorTemp = 5,
                    sharpness = 25,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高锐度增强细节",
                    "注意光线角度",
                    "寻找有趣的纹理"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/125",
                    aperture = "f/2.8",
                    focusMode = "手动对焦"
                ),
                tags = listOf("微距", "花卉", "细节", "色彩"),
                bestTime = "阴天或散射光",
                environmentTips = "柔和光线环境"
            ),

            // 昆虫微距
            SceneProfile(
                id = "macro-insect",
                name = "昆虫微距",
                category = SceneCategory.MACRO,
                subCategory = "昆虫微距",
                description = "昆虫微距拍摄，追求清晰细节",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 10,
                    contrast = 15,
                    colorTemp = 0,
                    sharpness = 30,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("nh")!!.copy(matchScore = 0.80f),
                    FilmPreset.fromId("rdp3")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "高锐度捕捉细节",
                    "注意昆虫运动",
                    "保持安全距离"
                ),
                cameraParams = CameraParams(
                    iso = "100",
                    shutterSpeed = "1/250",
                    aperture = "f/2.8",
                    focusMode = "连续对焦"
                ),
                tags = listOf("微距", "昆虫", "细节", "清晰"),
                bestTime = "清晨或阴天",
                environmentTips = "自然环境，注意安全"
            )
        )
    }

    // ==================== 特殊场景 ====================
    private fun buildSpecialProfiles(): List<SceneProfile> {
        return listOf(
            // 演唱会
            SceneProfile(
                id = "special-concert",
                name = "演唱会",
                category = SceneCategory.SPECIAL,
                subCategory = "演唱会",
                description = "演唱会拍摄，追求舞台光影效果",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = -10,
                    saturation = 15,
                    contrast = 20,
                    colorTemp = 0,
                    sharpness = 10,
                    vignette = 10,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("800t")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("tx400")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "高对比度增强舞台效果",
                    "注意灯光变化",
                    "遵守拍摄规定"
                ),
                cameraParams = CameraParams(
                    iso = "1600",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "自动",
                    stabilizationEnabled = true
                ),
                tags = listOf("特殊", "演唱会", "舞台", "光影"),
                environmentTips = "舞台环境，注意光线变化"
            ),

            // 婚礼
            SceneProfile(
                id = "special-wedding",
                name = "婚礼",
                category = SceneCategory.SPECIAL,
                subCategory = "婚礼",
                description = "婚礼拍摄，追求温馨浪漫感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 10,
                    contrast = 5,
                    colorTemp = 10,
                    sharpness = 8,
                    vignette = 5,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.DREAMY
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("portra")!!.copy(matchScore = 0.90f),
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.85f)
                ),
                masterTips = listOf(
                    "梦幻柔光营造浪漫感",
                    "捕捉情感瞬间",
                    "注意光线变化"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "1/125",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("特殊", "婚礼", "温馨", "浪漫"),
                environmentTips = "婚礼现场，注意光线"
            ),

            // 水族馆
            SceneProfile(
                id = "special-aquarium",
                name = "水族馆",
                category = SceneCategory.SPECIAL,
                subCategory = "水族馆",
                description = "水族馆拍摄，追求通透蓝色调",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 0,
                    saturation = 5,
                    contrast = 10,
                    colorTemp = -15,
                    sharpness = 15,
                    vignette = 0,
                    cyanMagenta = -5,
                    softLight = SoftLightMode.NONE
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_cool")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("nc")!!.copy(matchScore = 0.75f)
                ),
                masterTips = listOf(
                    "冷色调增强水感",
                    "注意玻璃反光",
                    "寻找有趣的海洋生物"
                ),
                cameraParams = CameraParams(
                    iso = "400",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("特殊", "水族馆", "冷调", "通透"),
                environmentTips = "水族馆环境，注意光线"
            ),

            // 派对
            SceneProfile(
                id = "special-party",
                name = "派对",
                category = SceneCategory.SPECIAL,
                subCategory = "派对",
                description = "派对拍摄，追求欢快氛围感",
                color = HASSELBLAD_ORANGE,
                hasselbladParams = HasselbladParams(
                    tone = 5,
                    saturation = 15,
                    contrast = 10,
                    colorTemp = 5,
                    sharpness = 8,
                    vignette = 0,
                    cyanMagenta = 0,
                    softLight = SoftLightMode.SOFT
                ),
                recommendedFilm = listOf(
                    FilmPreset.fromId("ccd_warm")!!.copy(matchScore = 0.85f),
                    FilmPreset.fromId("cc")!!.copy(matchScore = 0.80f)
                ),
                masterTips = listOf(
                    "暖色调营造欢快感",
                    "捕捉互动瞬间",
                    "注意光线变化"
                ),
                cameraParams = CameraParams(
                    iso = "Auto",
                    shutterSpeed = "1/60",
                    aperture = "f/2.8",
                    whiteBalance = "自动"
                ),
                tags = listOf("特殊", "派对", "欢快", "氛围"),
                environmentTips = "派对现场，注意光线"
            )
        )
    }
}