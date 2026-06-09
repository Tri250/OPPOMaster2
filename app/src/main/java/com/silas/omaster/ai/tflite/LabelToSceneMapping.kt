package com.silas.omaster.ai.tflite

/**
 * TFLite 标签到场景ID映射
 * MediaPipe Image Classifier 输出标签映射到 50+ 场景
 *
 * 标签索引对应模型输出的类别索引
 * 场景ID对应 SceneProfileRepository 中的场景配置
 */
object LabelToSceneMapping {

    // 标签索引 -> 场景ID 映射表
    // 基于 MediaPipe Image Classifier 常见标签
    private val labelMap: Map<Int, String> = mapOf(
        // ─── 人像系列 (0-10) ───
        0 to "portrait-standard",       // portrait
        1 to "portrait-backlit",        // portrait_backlight
        2 to "portrait-studio",         // portrait_professional
        3 to "portrait-bw",             // portrait_bw
        4 to "portrait-group",          // group_photo
        5 to "portrait-children",       // child
        6 to "portrait-standard",       // face
        7 to "portrait-backlit",        // selfie
        8 to "portrait-standard",       // person
        9 to "portrait-group",          // people
        10 to "portrait-children",      // baby

        // ─── 风景系列 (11-20) ───
        11 to "landscape-standard",     // landscape
        12 to "landscape-sunset",       // sunset
        13 to "landscape-blue-sky",     // sky
        14 to "landscape-forest",       // forest
        15 to "landscape-autumn",       // autumn
        16 to "landscape-snow",         // snow
        17 to "landscape-beach",        // beach
        18 to "landscape-waterfall",    // waterfall
        19 to "landscape-standard",     // mountain
        20 to "landscape-standard",     // nature

        // ─── 夜景系列 (21-25) ───
        21 to "night-city",             // night
        22 to "night-neon",             // neon
        23 to "night-starry",           // starry_sky
        24 to "night-candlelight",      // candlelight
        25 to "night-fireworks",        // fireworks

        // ─── 美食系列 (26-28) ───
        26 to "food-restaurant",        // food
        27 to "food-dessert",           // dessert
        28 to "food-drink",             // drink

        // ─── 城市系列 (29-32) ───
        29 to "urban-street",           // street
        30 to "urban-architecture",     // architecture
        31 to "urban-cafe",             // cafe
        32 to "urban-museum",           // museum

        // ─── 宠物系列 (33-35) ───
        33 to "pet-standard",           // pet
        34 to "pet-standard",           // cat
        35 to "pet-outdoor",            // dog

        // ─── 微距系列 (36-38) ───
        36 to "macro-flower",           // flower
        37 to "macro-insect",           // insect
        38 to "macro-flower",           // plant

        // ─── 特殊系列 (39-45) ───
        39 to "special-concert",        // concert
        40 to "special-wedding",        // wedding
        41 to "special-aquarium",       // aquarium
        42 to "special-party",          // party
        43 to "night-fireworks",        // festival
        44 to "landscape-sunset",       // golden_hour
        45 to "landscape-sunset",       // blue_hour

        // ─── 其他场景 (46-50) ───
        46 to "urban-architecture",     // building
        47 to "landscape-standard",     // outdoor
        48 to "portrait-standard",      // indoor
        49 to "landscape-standard",     // travel
        50 to "landscape-standard"      // scenery
    )

    // 反向映射：场景ID -> 标签索引列表
    private val reverseMap: Map<String, List<Int>> = labelMap.entries
        .groupBy { it.value }
        .mapValues { entry -> entry.value.map { it.key } }

    /**
     * 根据标签索引获取场景ID
     */
    fun getSceneId(labelIndex: Int): String? {
        return labelMap[labelIndex]
    }

    /**
     * 根据场景ID获取标签索引列表
     */
    fun getLabelIndices(sceneId: String): List<Int> {
        return reverseMap[sceneId] ?: emptyList()
    }

    /**
     * 获取所有支持的标签数量
     */
    fun getLabelCount(): Int = labelMap.size

    /**
     * 获取所有场景ID列表
     */
    fun getAllSceneIds(): List<String> = labelMap.values.distinct()

    /**
     * MediaPipe Image Classifier 原始标签名称
     * 用于调试和日志
     */
    fun getRawLabelName(labelIndex: Int): String {
        return when (labelIndex) {
            0 -> "portrait"
            1 -> "portrait_backlight"
            2 -> "portrait_professional"
            3 -> "portrait_bw"
            4 -> "group_photo"
            5 -> "child"
            6 -> "face"
            7 -> "selfie"
            8 -> "person"
            9 -> "people"
            10 -> "baby"
            11 -> "landscape"
            12 -> "sunset"
            13 -> "sky"
            14 -> "forest"
            15 -> "autumn"
            16 -> "snow"
            17 -> "beach"
            18 -> "waterfall"
            19 -> "mountain"
            20 -> "nature"
            21 -> "night"
            22 -> "neon"
            23 -> "starry_sky"
            24 -> "candlelight"
            25 -> "fireworks"
            26 -> "food"
            27 -> "dessert"
            28 -> "drink"
            29 -> "street"
            30 -> "architecture"
            31 -> "cafe"
            32 -> "museum"
            33 -> "pet"
            34 -> "cat"
            35 -> "dog"
            36 -> "flower"
            37 -> "insect"
            38 -> "plant"
            39 -> "concert"
            40 -> "wedding"
            41 -> "aquarium"
            42 -> "party"
            43 -> "festival"
            44 -> "golden_hour"
            45 -> "blue_hour"
            46 -> "building"
            47 -> "outdoor"
            48 -> "indoor"
            49 -> "travel"
            50 -> "scenery"
            else -> "unknown_$labelIndex"
        }
    }

    /**
     * 场景类别分组
     * 用于模型输出的后处理
     */
    fun getSceneCategory(sceneId: String): String {
        return when {
            sceneId.startsWith("portrait") -> "PORTRAIT"
            sceneId.startsWith("landscape") -> "LANDSCAPE"
            sceneId.startsWith("night") -> "NIGHT"
            sceneId.startsWith("food") -> "FOOD"
            sceneId.startsWith("urban") -> "URBAN"
            sceneId.startsWith("pet") -> "PET"
            sceneId.startsWith("macro") -> "MACRO"
            sceneId.startsWith("special") -> "SPECIAL"
            else -> "OTHER"
        }
    }

    /**
     * 合并相似场景的置信度
     * 例如：portrait, face, selfie, person 都属于人像类别
     */
    fun mergeCategoryConfidence(probabilities: Map<String, Float>): Map<String, Float> {
        val categoryScores = mutableMapOf<String, Float>()

        probabilities.forEach { (sceneId, confidence) ->
            val category = getSceneCategory(sceneId)
            val currentScore = categoryScores[category] ?: 0f
            categoryScores[category] = currentScore + confidence
        }

        return categoryScores
    }
}