package com.silas.omaster.model

/**
 * 场景类型枚举 - AI场景识别
 * 支持所有测试用例场景
 */
enum class SceneType(
    val displayName: String,
    val description: String,
    val sceneKey: String,
    val priority: Int = 0
) {
    // 基础场景类型
    PORTRAIT("人像", "适合人物摄影，正面、侧面、背面", "portrait", 10),
    LANDSCAPE("风景", "适合户外风景、山川湖海", "landscape", 1),
    NIGHT("夜景", "适合夜间城市、星空", "night", 3),
    SUNSET("日落", "适合日落、黄金时刻", "sunset", 4),
    FOOD("美食", "适合美食拍摄", "food", 5),
    STREET("街头", "适合街头纪实", "street", 6),
    NATURE("自然", "适合自然生态、植物", "nature", 7),
    ARCHITECTURE("建筑", "适合城市建筑、室内空间", "architecture", 8),
    MACRO("微距", "适合特写、微距摄影", "macro", 9),
    
    // 新增场景类型 - 根据测试用例
    NIGHT_PORTRAIT("夜景人像", "夜间环境下的人像拍摄", "night_portrait", 11),
    MIXED_LANDSCAPE("人像风景混合", "人物站在风景前，人物占画面1/3", "mixed_landscape", 12),
    MIXED_FOOD("人像美食混合", "人物手持美食，美食占画面1/2", "mixed_food", 13),
    MOTION("运动", "快速移动的物体（汽车、跑步者）", "motion", 14),
    CITYSCAPE("城市风光", "城市建筑风光", "cityscape", 15),
    RAINY_FOGGY("雨雾天", "雨雾天风景", "rainy_foggy", 16),
    STARRY_NIGHT("星空夜景", "星空夜景", "starry_night", 17),
    DESERT("沙漠风光", "沙漠风光", "desert", 18),
    FLOWER("花卉特写", "花卉特写", "flower", 19),
    INSECT("昆虫特写", "昆虫特写", "insect", 20),
    OBJECT_DETAIL("物品细节", "物品细节", "object_detail", 21),
    FLOWERS_SUNSET("日落花卉", "日落花卉场景", "flowers_sunset", 22),
    STILL_LIFE("静物", "室内灯光下的静物", "still_life", 24),
    
    // 异常场景处理
    TOO_DARK("光线太暗", "提示光线太暗无法识别", "too_dark", 100),
    TOO_BRIGHT("光线太亮", "光线太亮无法识别", "too_bright", 101),
    TOO_BLURRY("画面模糊", "画面模糊无法识别", "too_blurry", 102),
    INDOOR_WARM("室内暖光", "室内暖光场景", "indoor_warm", 103),
    UNKNOWN("未知", "无法识别场景", "unknown", 999);

    companion object {
        fun fromKey(key: String): SceneType {
            return entries.find { it.sceneKey == key } ?: UNKNOWN
        }
        
        /**
         * 判断是否是混合场景
         */
        fun isMixedScene(scene: SceneType): Boolean {
            return scene in listOf(NIGHT_PORTRAIT, MIXED_LANDSCAPE, MIXED_FOOD)
        }
        
        /**
         * 判断是否是异常场景
         */
        fun isErrorScene(scene: SceneType): Boolean {
            return scene in listOf(TOO_DARK, TOO_BRIGHT, TOO_BLURRY)
        }
    }
    
    /**
     * 获取场景对应的哈苏模式参数名称
     */
    fun getHasselbladModeName(): String {
        return when (this) {
            PORTRAIT, NIGHT_PORTRAIT, MIXED_LANDSCAPE, MIXED_FOOD -> "哈苏人像模式"
            LANDSCAPE, CITYSCAPE, RAINY_FOGGY -> "哈苏风景模式"
            NIGHT, STARRY_NIGHT -> "哈苏夜景模式"
            SUNSET, FLOWERS_SUNSET -> "哈苏日落模式"
            FOOD -> "哈苏美食模式"
            STREET -> "哈苏街拍模式"
            NATURE, FLOWER -> "哈苏自然模式"
            ARCHITECTURE -> "哈苏建筑模式"
            MACRO, INSECT, OBJECT_DETAIL -> "哈苏微距模式"
            MOTION -> "哈苏运动模式"
            else -> "哈苏大师模式"
        }
    }
    
    /**
     * 获取场景对应的预设推荐
     */
    fun getRecommendedPresetKeywords(): List<String> {
        return when (this) {
            PORTRAIT -> listOf("人像", "自然", "哈苏人像")
            LANDSCAPE, CITYSCAPE -> listOf("风景", "自然", "哈苏风景")
            NIGHT, STARRY_NIGHT -> listOf("夜景", "哈苏夜景")
            SUNSET, FLOWERS_SUNSET -> listOf("日落", "哈苏日落")
            FOOD -> listOf("美食", "哈苏美食")
            STREET -> listOf("街拍", "纪实")
            NATURE, FLOWER -> listOf("自然", "清新")
            ARCHITECTURE -> listOf("建筑", "城市")
            MACRO, INSECT, OBJECT_DETAIL -> listOf("微距", "特写")
            NIGHT_PORTRAIT -> listOf("夜景人像", "夜景", "哈苏夜景人像")
            MIXED_LANDSCAPE -> listOf("风景人像", "风景人像")
            MIXED_FOOD -> listOf("人像美食", "人像美食")
            MOTION -> listOf("运动", "动感")
            RAINY_FOGGY -> listOf("雨雾天", "风景", "自然")
            else -> listOf("哈苏大师")
        }
    }
}