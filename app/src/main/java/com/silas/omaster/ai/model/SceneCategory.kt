package com.silas.omaster.ai.model

/**
 * 场景大类枚举
 * 一级场景分类体系
 */
enum class SceneCategory(
    val id: String,
    val displayName: String,
    val icon: String,
    val description: String
) {
    PORTRAIT(
        id = "portrait",
        displayName = "人像",
        icon = "👤",
        description = "人像摄影场景，包括标准人像、逆光人像、棚拍、黑白人像等"
    ),
    LANDSCAPE(
        id = "landscape",
        displayName = "风景",
        icon = "🏔️",
        description = "风景摄影场景，包括标准风景、日落、蓝天白云、森林、秋景等"
    ),
    NIGHT(
        id = "night",
        displayName = "夜景",
        icon = "🌃",
        description = "夜景摄影场景，包括城市夜景、霓虹灯、星空、烛光、烟花等"
    ),
    FOOD(
        id = "food",
        displayName = "美食",
        icon = "🍜",
        description = "美食摄影场景，包括餐厅美食、甜点、饮品等"
    ),
    URBAN(
        id = "urban",
        displayName = "城市",
        icon = "🏙️",
        description = "城市摄影场景，包括街拍、建筑、咖啡馆、博物馆等"
    ),
    PET(
        id = "pet",
        displayName = "宠物",
        icon = "🐕",
        description = "宠物摄影场景，包括猫、狗、鸟类等动物拍摄"
    ),
    MACRO(
        id = "macro",
        displayName = "微距",
        icon = "🔍",
        description = "微距摄影场景，包括花卉、昆虫、细节特写等"
    ),
    SPECIAL(
        id = "special",
        displayName = "特殊",
        icon = "✨",
        description = "特殊场景，包括雪景、海滩、瀑布、烟花、演唱会等"
    );

    companion object {
        fun fromId(id: String): SceneCategory? {
            return entries.find { it.id == id }
        }
    }
}